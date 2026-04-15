package proiect.gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import javax.swing.*;

import proiect.alg.HOG;
import proiect.alg.SVMClassifier;
import proiect.detect.HeadDetector;
import proiect.io.ImageUtils;
import proiect.webcam.CameraCapture;
import proiect.webcam.FrameDrawer;

/**
 * Panel pentru cerinta 3: capturare N imagini cu detectia capului, salvate
 * cu prefixul pseudonimului si timestamp.
 *
 * Fluxul:
 *  - utilizatorul introduce pseudonimul si numarul de imagini dorite
 *  - apasa "Start": se deschide camera, iar intr-un thread separat se
 *    preia cadru dupa cadru, se detecteaza capul maxim si se salveaza
 *  - afisajul live e actualizat tot pe un thread separat (dar UI-ul se
 *    face cu invokeLater pentru ca Swing nu e thread-safe)
 */
public class CapturePanel extends JPanel {

    /** Sursa clasificatorului de detectie cap (partajat cu alte tab-uri). */
    private final Supplier<SVMClassifier> sursaDetector;

    /** Camera; se deschide la start si se inchide la stop. */
    private CameraCapture cam;

    /** Flag care comanda oprirea thread-ului de captura. */
    private volatile boolean ruleaza;

    /** Zona unde desenam live imaginea. */
    private final JLabel eticheta = new JLabel();

    /** Campurile de input. */
    private final JTextField txtPseudonim = new JTextField("rares", 10);
    private final JSpinner spnNumar = new JSpinner(new SpinnerNumberModel(500, 1, 10000, 50));
    private final JLabel lblStatus = new JLabel("gata");
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");

    /** Format pentru numele fisierelor: pseudonim_yyyyMMdd_HHmmss_SSS.png */
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    public CapturePanel(Supplier<SVMClassifier> sursaDetector) {
        this.sursaDetector = sursaDetector;
        setLayout(new BorderLayout());

        JPanel sus = new JPanel();
        sus.add(new JLabel("Pseudonim:"));
        sus.add(txtPseudonim);
        sus.add(new JLabel("Nr. imagini:"));
        sus.add(spnNumar);
        sus.add(btnStart);
        sus.add(btnStop);
        sus.add(lblStatus);
        add(sus, BorderLayout.NORTH);

        eticheta.setHorizontalAlignment(SwingConstants.CENTER);
        add(eticheta, BorderLayout.CENTER);

        btnStop.setEnabled(false);
        btnStart.addActionListener(e -> porneste());
        btnStop.addActionListener(e -> opreste());
    }

    private void porneste() {
        SVMClassifier det = sursaDetector.get();
        if (det == null) {
            JOptionPane.showMessageDialog(this,
                    "Detectorul de cap nu e antrenat. Antreneaza-l intai din meniu.");
            return;
        }
        final String pseudonim = txtPseudonim.getText().trim();
        if (pseudonim.isEmpty()) { lblStatus.setText("pseudonim gol"); return; }
        final int nTarget = (Integer) spnNumar.getValue();

        cam = new CameraCapture(0);
        if (!cam.esteDeschisa()) {
            JOptionPane.showMessageDialog(this, "Nu pot deschide camera.");
            return;
        }

        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        ruleaza = true;

        // Thread-ul de captura — nu blocam EDT-ul Swing.
        new Thread(() -> buclaCaptura(det, pseudonim, nTarget), "captura").start();
    }

    private void opreste() {
        ruleaza = false;
    }

    /** Bucla principala de captura, pe thread separat. */
    private void buclaCaptura(SVMClassifier det, String pseudonim, int nTarget) {
        HeadDetector hd = new HeadDetector(det, new HOG());
        File dir = new File("invatare_fete/" + pseudonim);
        dir.mkdirs();

        int nSalvate = 0;
        while (ruleaza && nSalvate < nTarget) {
            int[][] gray = cam.cadruGray();
            if (gray == null) continue;

            int[][] cap = hd.decupeazaCapMaxim(gray);
            if (cap != null) {
                // Nume fisier conform cerintei 3.
                String nume = pseudonim + "_" + LocalDateTime.now().format(FMT) + ".png";
                try {
                    ImageUtils.salveazaPNG(cap, new File(dir, nume));
                    nSalvate++;
                } catch (Exception ex) {
                    System.err.println("Eroare salvare: " + ex);
                }
            }

            // Afisare live: desenam cadrul BGR in zona UI. Actualizam pe EDT.
            BufferedImage bimg = FrameDrawer.matLaBufferedImage(cam.cadruBGR());
            final int n = nSalvate;
            SwingUtilities.invokeLater(() -> {
                eticheta.setIcon(new ImageIcon(bimg));
                lblStatus.setText("salvate: " + n + "/" + nTarget);
            });
        }

        cam.inchide();
        cam = null;
        ruleaza = false;
        SwingUtilities.invokeLater(() -> {
            btnStart.setEnabled(true);
            btnStop.setEnabled(false);
        });
    }
}
