package proiect.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.*;

import proiect.alg.HOG;
import proiect.alg.SVMClassifier;
import proiect.detect.BoundingBox;
import proiect.detect.HeadDetector;
import proiect.io.ImageUtils;
import proiect.io.ModelStore;
import proiect.tools.Antrenare;
import proiect.webcam.CameraCapture;
import proiect.webcam.FrameDrawer;

/**
 * Panel pentru cerinta 8: test live la ~10 FPS.
 *
 * La Start:
 *  - se deschide camera si se incarca clasificatoarele de recunoastere
 *  - pe un thread separat:
 *      -> citim cadru
 *      -> HeadDetector -> lista de BoundingBox
 *      -> pentru fiecare bbox: decupam, scalam la 128x128, HOG, intrebam
 *         fiecare clasificator persoana; daca >= 1 zice +1, alegem cel mai
 *         confident si scriem pseudonimul deasupra patratului
 *      -> desenam peste cadrul BGR cu FrameDrawer si afisam in panel
 */
public class LiveTestPanel extends JPanel {

    private final Supplier<SVMClassifier> sursaDetector;
    private CameraCapture cam;
    private volatile boolean ruleaza;

    private final JLabel eticheta = new JLabel();
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");

    /** Clasificatoare de persoana: nume -> model. */
    private final List<String> numePersoane = new ArrayList<>();
    private final List<SVMClassifier> clasPersoane = new ArrayList<>();

    /** FPS tinta conform cerintei (10 FPS = 100 ms / cadru). */
    private static final long MS_PER_FRAME = 100;

    public LiveTestPanel(Supplier<SVMClassifier> sursaDetector) {
        this.sursaDetector = sursaDetector;
        setLayout(new BorderLayout());
        JPanel sus = new JPanel();
        sus.add(btnStart);
        sus.add(btnStop);
        add(sus, BorderLayout.NORTH);

        eticheta.setHorizontalAlignment(SwingConstants.CENTER);
        add(eticheta, BorderLayout.CENTER);

        btnStop.setEnabled(false);
        btnStart.addActionListener(e -> porneste());
        btnStop.addActionListener(e -> ruleaza = false);

        reincarcaClasificatoare();
    }

    /** Incarca toate fisierele clasificatoare/recunoastere_*.ser din disc. */
    public void reincarcaClasificatoare() {
        numePersoane.clear();
        clasPersoane.clear();
        File[] fis = Antrenare.DIR_MODELE.listFiles(
                (d, n) -> n.startsWith("recunoastere_") && n.endsWith(".ser"));
        if (fis == null) return;
        for (File f : fis) {
            // Nume persoana = partea intre "recunoastere_" si ".ser".
            String nume = f.getName().substring("recunoastere_".length(),
                    f.getName().length() - ".ser".length());
            try {
                SVMClassifier svm = (SVMClassifier) ModelStore.incarca(f);
                numePersoane.add(nume);
                clasPersoane.add(svm);
            } catch (Exception ex) {
                System.err.println("Nu pot incarca " + f + ": " + ex);
            }
        }
        System.out.println("Clasificatoare persoana incarcate: " + numePersoane);
    }

    private void porneste() {
        SVMClassifier det = sursaDetector.get();
        if (det == null) {
            JOptionPane.showMessageDialog(this, "Detectorul de cap nu e antrenat.");
            return;
        }
        cam = new CameraCapture(0);
        if (!cam.esteDeschisa()) {
            JOptionPane.showMessageDialog(this, "Nu pot deschide camera.");
            return;
        }
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        ruleaza = true;
        new Thread(() -> bucla(det), "live-test").start();
    }

    private void bucla(SVMClassifier det) {
        HeadDetector hd = new HeadDetector(det, new HOG());
        HOG hog = new HOG();

        while (ruleaza) {
            long t0 = System.currentTimeMillis();

            int[][] gray = cam.cadruGray();
            if (gray == null) continue;

            // Detectie capete.
            List<BoundingBox> boxes = hd.detecteaza(gray);
            List<String> etichete = new ArrayList<>();

            // Pentru fiecare cap, decidem cine e.
            for (BoundingBox b : boxes) {
                int[][] crop = ImageUtils.decupeaza(gray, b.x, b.y, b.latura, b.latura);
                int[][] crop128 = ImageUtils.redimensioneaza(crop, 128, 128);
                double[] feat = hog.extrage(crop128);

                // Luam clasificatorul cu cel mai mare scor pozitiv (one-vs-rest).
                String bestNume = null;
                double bestScor = 0; // daca niciun clasificator nu iese +1, ramane null
                for (int i = 0; i < clasPersoane.size(); i++) {
                    double s = clasPersoane.get(i).decision(feat);
                    if (s > 0 && s > bestScor) {
                        bestScor = s;
                        bestNume = numePersoane.get(i);
                    }
                }
                etichete.add(bestNume); // poate fi null => doar patrat verde, fara text
            }

            // Desenare peste cadru si afisare.
            FrameDrawer.deseneazaCuEticheta(cam.cadruBGR(), boxes, etichete);
            BufferedImage bimg = FrameDrawer.matLaBufferedImage(cam.cadruBGR());
            SwingUtilities.invokeLater(() -> eticheta.setIcon(new ImageIcon(bimg)));

            // Reglare FPS: dormim doar atat cat trebuie pana la 10 FPS.
            long dt = System.currentTimeMillis() - t0;
            long somn = MS_PER_FRAME - dt;
            if (somn > 0) {
                try { Thread.sleep(somn); } catch (InterruptedException ex) { break; }
            }
        }

        cam.inchide();
        cam = null;
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });
    }
}
