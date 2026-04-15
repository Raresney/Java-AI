package proiect.tools;

import java.io.File;
import java.io.IOException;

import proiect.alg.HOG;
import proiect.alg.Kernel;
import proiect.alg.KernelLinear;
import proiect.alg.KernelSigmoid;
import proiect.alg.SMO;
import proiect.alg.SVMClassifier;
import proiect.io.DatasetLoader;
import proiect.io.ModelStore;

/**
 * Antrenare SVM pentru detectorul de cap (cerinta 1) si pentru clasificatoarele
 * de recunoastere per persoana (cerinta 6).
 *
 * Clasa nu are GUI — doar logica. E apelata de MainWindow la click pe
 * butoanele corespunzatoare.
 */
public final class Antrenare {

    /** Directorul unde salvam toate modelele serializate. */
    public static final File DIR_MODELE = new File("clasificatoare");

    /** Directorul imaginilor pentru detectorul de cap: pozitive/, negative/. */
    public static final File DIR_INVATARE_CAP = new File("invatare_cap");

    /** Directorul imaginilor pentru recunoastere, cu un subdirector per persoana. */
    public static final File DIR_INVATARE_FETE = new File("invatare_fete");

    /** Directorul vectorilor HOG salvati (cerinta 7 livrare). */
    public static final File DIR_VECTORI = new File("vectori_hog");

    private Antrenare() {}

    /**
     * Antreneaza detectorul de cap pe imagini 64x64.
     *
     * Salveaza modelul in clasificatoare/detector_cap.ser si vectorii HOG
     * (X, y) in vectori_hog/detector_cap_vectori.ser.
     */
    public static SVMClassifier antreneazaDetectorCap() throws IOException {
        HOG hog = new HOG(); // parametri standard
        File pozitive = new File(DIR_INVATARE_CAP, "pozitive");
        File negative = new File(DIR_INVATARE_CAP, "negative");

        System.out.println("Incarc imaginile si extrag HOG...");
        DatasetLoader.Set set = DatasetLoader.incarcaPozNeg(pozitive, negative, 64, hog);
        System.out.println("Exemple totale: " + set.X.length
                + " (dimensiune vector: " + set.X[0].length + ")");

        // Salvam vectorii pentru livrare (cerinta 7).
        DIR_VECTORI.mkdirs();
        ModelStore.salveaza(set, new File(DIR_VECTORI, "detector_cap_vectori.ser"));

        // Detectorul de cap foloseste kernel Linear: la detectie live precalculam
        // w si decizia devine un singur dot product (~650x mai rapid decat Sigmoid
        // cu 650 SV). Cerinta 7 (Sigmoid) ramane satisfacuta de recunoastere.
        Kernel k = new KernelLinear();
        SMO smo = new SMO(1.0, 1e-3, 5, 42);
        System.out.println("Antrenez SMO cu " + k.name() + "...");
        long t0 = System.currentTimeMillis();
        SVMClassifier svm = smo.antreneaza(set.X, set.y, k);
        System.out.println("Timp antrenare: " + (System.currentTimeMillis() - t0) + " ms");
        System.out.println("Vectori suport: " + svm.numSupportVectors());

        DIR_MODELE.mkdirs();
        ModelStore.salveaza(svm, new File(DIR_MODELE, "detector_cap.ser"));
        return svm;
    }

    /**
     * Antreneaza un clasificator de recunoastere pentru o persoana.
     * Imaginile persoanei -> +1, restul persoanelor -> -1 (one-vs-rest).
     *
     * Salveaza in clasificatoare/recunoastere_<persoana>.ser.
     */
    public static SVMClassifier antreneazaRecunoastere(String persoana) throws IOException {
        HOG hog = new HOG();
        System.out.println("Incarc imagini pentru " + persoana + "...");
        DatasetLoader.Set set = DatasetLoader.incarcaPersoana(
                DIR_INVATARE_FETE, persoana, 128, hog);
        System.out.println("Exemple totale: " + set.X.length);

        // Contorizam balantarea claselor — util pentru debug.
        int poz = 0, neg = 0;
        for (int y : set.y) if (y > 0) poz++; else neg++;
        System.out.println("  pozitive (+1): " + poz + ", negative (-1): " + neg);

        Kernel k = new KernelSigmoid(1.0 / set.X[0].length, 0.0);
        SMO smo = new SMO(1.0, 1e-3, 5, 42);
        System.out.println("Antrenez SMO...");
        SVMClassifier svm = smo.antreneaza(set.X, set.y, k);
        System.out.println("Vectori suport: " + svm.numSupportVectors());

        DIR_MODELE.mkdirs();
        ModelStore.salveaza(svm, new File(DIR_MODELE, "recunoastere_" + persoana + ".ser"));
        return svm;
    }
}
