package proiect.detect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import proiect.alg.HOG;
import proiect.alg.SVMClassifier;
import proiect.io.ImageUtils;

/**
 * Detector de "patrat cap" (cerintele 1 si 2).
 *
 * Algoritm:
 *  1) Sliding window pe o piramida de scale — imaginea e redimensionata
 *     cu factor 1/scaleStep la fiecare nivel, iar fereastra ramane la
 *     dimensiunea fixa 64x64 (la care a fost antrenat SVM-ul).
 *  2) Pentru fiecare pozitie a ferestrei extragem HOG si intrebam SVM-ul.
 *     Daca scorul > 0, consideram ca e "cap" si salvam BoundingBox-ul
 *     (rescalat la coordonatele imaginii originale).
 *  3) Dupa ce am scanat toata piramida, aplicam Non-Maximum Suppression
 *     ca sa eliminam suprapunerile si pastram doar detectia cea mai buna
 *     pe fiecare zona.
 */
public class HeadDetector {

    /** Dimensiunea laturii ferestrei (cea la care a fost antrenat detectorul). */
    public static final int WINDOW = 64;

    /** SVM-ul antrenat la cerinta 1. */
    private final SVMClassifier svm;

    /** Extractor HOG (aceiasi parametri la antrenare si la detectie!). */
    private final HOG hog;

    /** Pasul cu care gliseaza fereastra, in pixeli. */
    private final int stride;

    /** Factorul cu care micsoram imaginea la fiecare nivel al piramidei. */
    private final double scaleStep;

    /** Pragul IoU peste care doua detectii sunt considerate duplicate. */
    private final double nmsIoU;

    public HeadDetector(SVMClassifier svm, HOG hog) {
        this(svm, hog, 8, 1.2, 0.3);
    }

    public HeadDetector(SVMClassifier svm, HOG hog,
                        int stride, double scaleStep, double nmsIoU) {
        this.svm = svm;
        this.hog = hog;
        this.stride = stride;
        this.scaleStep = scaleStep;
        this.nmsIoU = nmsIoU;
    }

    /**
     * Detecteaza toate patratele cap din imagine, returneaza lista filtrata NMS.
     *
     * @param gray imagine grayscale [H][W] cu valori in [0, 255]
     * @return lista de BoundingBox cu coordonate in sistemul imaginii originale
     */
    public List<BoundingBox> detecteaza(int[][] gray) {
        List<BoundingBox> candidati = new ArrayList<>();

        int[][] nivelCurent = gray;
        double factor = 1.0; // cat de mic e nivelul curent fata de original

        // Coboram in piramida cat timp imaginea e mai mare decat fereastra.
        while (nivelCurent.length >= WINDOW && nivelCurent[0].length >= WINDOW) {
            scaneazaNivel(nivelCurent, factor, candidati);

            // Scalam pentru nivelul urmator.
            int nouH = (int) (nivelCurent.length / scaleStep);
            int nouW = (int) (nivelCurent[0].length / scaleStep);
            if (nouH < WINDOW || nouW < WINDOW) break;
            nivelCurent = ImageUtils.redimensioneaza(nivelCurent, nouH, nouW);
            factor *= scaleStep;
        }

        return nms(candidati);
    }

    /**
     * Varianta ceruta la punctul 2: selecteaza patratul cu aria maxima si
     * returneaza decupajul lui scalat la 128x128.
     *
     * @return matrice 128x128 grayscale, sau null daca nu s-a gasit niciun cap
     */
    public int[][] decupeazaCapMaxim(int[][] gray) {
        List<BoundingBox> boxes = detecteaza(gray);
        if (boxes.isEmpty()) return null;
        BoundingBox best = boxes.get(0);
        for (BoundingBox b : boxes) {
            if (b.aria() > best.aria()) best = b;
        }
        // Extragem portiunea de imagine si o scalam la 128x128.
        int[][] crop = ImageUtils.decupeaza(gray, best.x, best.y, best.latura, best.latura);
        return ImageUtils.redimensioneaza(crop, 128, 128);
    }

    /** Aplica sliding window pe un singur nivel al piramidei. */
    private void scaneazaNivel(int[][] img, double factor, List<BoundingBox> out) {
        final int H = img.length;
        final int W = img[0].length;

        for (int y = 0; y + WINDOW <= H; y += stride) {
            for (int x = 0; x + WINDOW <= W; x += stride) {
                // Decupam fereastra si calculam HOG.
                int[][] fereastra = ImageUtils.decupeaza(img, x, y, WINDOW, WINDOW);
                double[] feat = hog.extrage(fereastra);
                double scor = svm.decision(feat);

                // Daca SVM-ul zice "cap" (scor > 0), convertim coordonatele
                // inapoi la sistemul imaginii originale.
                if (scor > 0) {
                    int xOrig = (int) (x * factor);
                    int yOrig = (int) (y * factor);
                    int latOrig = (int) (WINDOW * factor);
                    out.add(new BoundingBox(xOrig, yOrig, latOrig, scor));
                }
            }
        }
    }

    /**
     * Non-Maximum Suppression: sorteaza descrescator dupa scor, pastreaza
     * fiecare patrat care nu se suprapune prea mult cu unul deja pastrat.
     */
    private List<BoundingBox> nms(List<BoundingBox> in) {
        // Sortare descrescatoare dupa scor.
        Collections.sort(in, (a, b) -> Double.compare(b.scor, a.scor));
        List<BoundingBox> pastrate = new ArrayList<>();
        for (BoundingBox candidat : in) {
            boolean suprapus = false;
            for (BoundingBox p : pastrate) {
                if (candidat.iou(p) > nmsIoU) {
                    suprapus = true;
                    break;
                }
            }
            if (!suprapus) pastrate.add(candidat);
        }
        return pastrate;
    }
}
