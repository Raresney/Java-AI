package proiect.gui;

import java.awt.BorderLayout;
import java.io.File;
import javax.swing.*;

import proiect.alg.SVMClassifier;
import proiect.io.ModelStore;
import proiect.tools.Antrenare;

/**
 * Fereastra principala a aplicatiei.
 *
 * Organizata cu {@link JTabbedPane} — un tab pentru fiecare functie majora
 * a proiectului:
 *   - Adnoteaza imagini (pentru detectorul de cap)
 *   - Captureaza fete (cerinta 3)
 *   - Vizualizare / stergere (cerinta 4)
 *   - Test live (cerinta 8)
 *
 * Meniul "Antrenare" contine butoanele pentru cerinta 1 si cerinta 6.
 */
public class MainWindow extends JFrame {

    /** Panelurile le tinem ca atribute ca sa le putem actualiza dupa antrenari. */
    private final CapturePanel tabCaptureaza;
    private final LiveTestPanel tabLive;

    /** Detectorul de cap partajat intre tab-uri (null pana la antrenare). */
    private SVMClassifier detectorCap;

    public MainWindow() {
        super("Proiect SVM - detectie si recunoastere");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 700);

        // Incarcam detectorul de cap de pe disc, daca exista.
        detectorCap = incarcaDetector();

        // Construim tab-urile.
        JTabbedPane tabs = new JTabbedPane();
        AnnotatorPanel tabAdnot = new AnnotatorPanel();
        tabCaptureaza = new CapturePanel(() -> detectorCap);
        BrowserPanel tabBrowse = new BrowserPanel();
        tabLive = new LiveTestPanel(() -> detectorCap);

        tabs.addTab("Adnoteaza (cerinta 1)", tabAdnot);
        tabs.addTab("Captureaza fete (cerinta 3)", tabCaptureaza);
        tabs.addTab("Vizualizare / stergere (cerinta 4)", tabBrowse);
        tabs.addTab("Test live (cerinta 8)", tabLive);

        add(tabs, BorderLayout.CENTER);
        setJMenuBar(construiesteMeniu());
    }

    /** Construieste bara de meniu cu operatiile de antrenare. */
    private JMenuBar construiesteMeniu() {
        JMenuBar mb = new JMenuBar();

        JMenu mAntrenare = new JMenu("Antrenare");

        JMenuItem miCap = new JMenuItem("Antreneaza detector cap (cerinta 1)");
        miCap.addActionListener(e -> antreneazaDetector());
        mAntrenare.add(miCap);

        JMenuItem miRecunoastere = new JMenuItem("Antreneaza recunoastere...");
        miRecunoastere.addActionListener(e -> antreneazaRecunoastereInteractiv());
        mAntrenare.add(miRecunoastere);

        mb.add(mAntrenare);
        return mb;
    }

    /** Incarca detector_cap.ser daca exista; altfel returneaza null. */
    private SVMClassifier incarcaDetector() {
        File f = new File(Antrenare.DIR_MODELE, "detector_cap.ser");
        if (!f.exists()) return null;
        try {
            return (SVMClassifier) ModelStore.incarca(f);
        } catch (Exception e) {
            System.err.println("Nu pot incarca " + f + ": " + e);
            return null;
        }
    }

    /** Ruleaza antrenarea detectorului de cap pe un thread separat. */
    private void antreneazaDetector() {
        new Thread(() -> {
            try {
                SVMClassifier svm = Antrenare.antreneazaDetectorCap();
                detectorCap = svm;
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this,
                            "Detector cap antrenat: " + svm.numSupportVectors() + " SV."));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Eroare: " + ex,
                                "Antrenare detector", JOptionPane.ERROR_MESSAGE));
            }
        }, "antrenare-cap").start();
    }

    /** Cere numele persoanei si antreneaza clasificatorul ei. */
    private void antreneazaRecunoastereInteractiv() {
        String nume = JOptionPane.showInputDialog(this,
                "Numele persoanei (subdirector in invatare_fete):");
        if (nume == null || nume.trim().isEmpty()) return;
        final String persoana = nume.trim();
        new Thread(() -> {
            try {
                SVMClassifier svm = Antrenare.antreneazaRecunoastere(persoana);
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "Clasificator " + persoana + ": " + svm.numSupportVectors() + " SV.");
                    tabLive.reincarcaClasificatoare();
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(this, "Eroare: " + ex,
                                "Antrenare recunoastere", JOptionPane.ERROR_MESSAGE));
            }
        }, "antrenare-recunoastere").start();
    }
}
