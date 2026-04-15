package proiect;

import javax.swing.SwingUtilities;

import proiect.gui.MainWindow;

/**
 * Punct de intrare al aplicatiei.
 *
 * Verifica incarcarea bibliotecii OpenCV si lanseaza {@link MainWindow}.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("Proiect SVM - detectie si recunoastere fete");

        // Incarcam OpenCV devreme, sa prindem eventualele erori de DLL la pornire
        // (mai bine decat sa se sparga cand utilizatorul apasa Start pe LiveTest).
        try {
            System.loadLibrary(org.opencv.core.Core.NATIVE_LIBRARY_NAME);
            System.out.println("OpenCV " + org.opencv.core.Core.VERSION + " incarcat.");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("EROARE: nu pot incarca OpenCV. Verifica -Djava.library.path.");
            System.err.println(e.getMessage());
            return;
        }

        // Toata manipularea Swing trebuie sa se faca pe Event Dispatch Thread.
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}
