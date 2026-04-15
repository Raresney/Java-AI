package proiect.tools;

import java.io.File;

import proiect.alg.HOG;
import proiect.alg.SVMClassifier;
import proiect.io.ImageUtils;
import proiect.io.ModelStore;

/**
 * Curatator automat pentru setul de imagini capturate de la webcam
 * (invatare_fete/&lt;persoana&gt;/). Sterge:
 *
 *  1. Imaginile cu varianta de luminozitate mica — tipic perete uniform,
 *     sau zone fara textura => HOG inutil.
 *  2. Imaginile pe care detectorul de cap curent NU le recunoaste ca fata
 *     (scor SVM sub prag) — inseamna ca au fost capturate dintr-o eroare
 *     veche si nu mai sunt reprezentative.
 *
 * Utilizare: java proiect.tools.CurataImagini &lt;persoana&gt; [pragVar] [pragScor]
 * Implicit: pragVar=600, pragScor=0.3.
 */
public class CurataImagini {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: CurataImagini <persoana> [pragVar] [pragScor]");
            return;
        }
        String persoana = args[0];
        double pragVar   = args.length > 1 ? Double.parseDouble(args[1]) : 600.0;
        double pragScor  = args.length > 2 ? Double.parseDouble(args[2]) : 0.3;

        File dir = new File("invatare_fete/" + persoana);
        File[] imgs = dir.listFiles(f ->
                f.isFile() && f.getName().toLowerCase().endsWith(".png"));
        if (imgs == null || imgs.length == 0) {
            System.err.println("Niciuna imagine in " + dir);
            return;
        }
        System.out.println("Total initial: " + imgs.length);

        SVMClassifier det = (SVMClassifier) ModelStore.incarca(
                new File("clasificatoare/detector_cap.ser"));
        HOG hog = new HOG();

        int sterseVar = 0, sterseDet = 0, pastrate = 0;
        double scorMin = Double.POSITIVE_INFINITY, scorMax = Double.NEGATIVE_INFINITY;
        double sumScor = 0;

        for (File f : imgs) {
            int[][] gray = ImageUtils.citesteGray(f);

            // Filtru 1: varianta.
            double var = varianta(gray);
            if (var < pragVar) {
                f.delete();
                sterseVar++;
                continue;
            }

            // Filtru 2: detectorul mai spune "e cap"?
            // Imaginile sunt 128x128, detectorul a fost antrenat pe 64x64.
            int[][] small = ImageUtils.redimensioneaza(gray, 64, 64);
            double scor = det.decision(hog.extrage(small));
            sumScor += scor;
            if (scor < scorMin) scorMin = scor;
            if (scor > scorMax) scorMax = scor;

            if (scor < pragScor) {
                f.delete();
                sterseDet++;
                continue;
            }
            pastrate++;
        }

        System.out.println("Sterse (varianta mica):  " + sterseVar);
        System.out.println("Sterse (scor SVM sub prag): " + sterseDet);
        System.out.println("Pastrate: " + pastrate);
        if (pastrate > 0) {
            System.out.printf("Scor SVM (pastrate): min=%.2f max=%.2f medie=%.2f%n",
                    scorMin, scorMax, sumScor / (pastrate + sterseDet));
        }
    }

    /** Varianta pixelilor. Pentru 128x128, valoare tipica:
     *   perete uniform ~50-300, fata ~800-3000. */
    private static double varianta(int[][] g) {
        long sum = 0, sumSq = 0;
        int n = 0;
        for (int[] row : g) {
            for (int v : row) {
                sum += v;
                sumSq += (long) v * v;
                n++;
            }
        }
        double mean = (double) sum / n;
        return (double) sumSq / n - mean * mean;
    }
}
