package proiect.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import proiect.alg.HOG;

/**
 * Utilitar pentru incarcarea unui dataset de imagini si extragerea vectorilor HOG.
 *
 * Un dataset este organizat sub forma unui director care contine doua
 * subdirectoare: "pozitive" (exemple +1) si "negative" (exemple -1). Fiecare
 * subdirector contine imagini .png / .jpg.
 *
 * Pentru recunoastere (cerinta 6), putem apela {@link #incarcaPersoana}:
 * toate imaginile persoanei curente sunt +1, restul sunt -1.
 */
public final class DatasetLoader {

    private DatasetLoader() {}

    /**
     * Rezultatul incarcarii: vectori X, etichete y.
     * Implementeaza Serializable pentru cerinta 6/7 (livrarea vectorilor HOG).
     */
    public static class Set implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final double[][] X;
        public final int[] y;
        public Set(double[][] X, int[] y) { this.X = X; this.y = y; }
    }

    /**
     * Incarca un dataset pozitive/negative cu HOG.
     *
     * @param dirPozitive   director cu imagini clasa +1
     * @param dirNegative   director cu imagini clasa -1
     * @param dimensiune    toate imaginile se redimensioneaza la dim x dim inainte de HOG
     * @param hog           extractor HOG
     */
    public static Set incarcaPozNeg(File dirPozitive, File dirNegative,
                                    int dimensiune, HOG hog) throws IOException {
        List<double[]> X = new ArrayList<>();
        List<Integer> y = new ArrayList<>();

        adaugaDirector(dirPozitive, +1, dimensiune, hog, X, y);
        adaugaDirector(dirNegative, -1, dimensiune, hog, X, y);

        return asambleaza(X, y);
    }

    /**
     * Incarca dataset pentru cerinta 6: imaginile din subdirectorul
     * persoanei curente sunt +1, restul (toate celelalte persoane) sunt -1.
     *
     * @param dirPersoane  folderul "invatare_fete"
     * @param persoana     numele subdirectorului persoanei curente (ex. "rares")
     * @param dimensiune   toate imaginile sunt deja 128x128 dupa cerinta 3, dar
     *                     redimensionam pentru siguranta
     * @param hog          extractor HOG
     */
    public static Set incarcaPersoana(File dirPersoane, String persoana,
                                      int dimensiune, HOG hog) throws IOException {
        List<double[]> X = new ArrayList<>();
        List<Integer> y = new ArrayList<>();

        File[] subdirs = dirPersoane.listFiles(File::isDirectory);
        if (subdirs == null) throw new IOException("Director gol: " + dirPersoane);

        for (File sub : subdirs) {
            int eticheta = sub.getName().equals(persoana) ? +1 : -1;
            adaugaDirector(sub, eticheta, dimensiune, hog, X, y);
        }
        return asambleaza(X, y);
    }

    /** Adauga toate imaginile dintr-un director la X / y. */
    private static void adaugaDirector(File dir, int eticheta, int dim, HOG hog,
                                       List<double[]> X, List<Integer> y) throws IOException {
        File[] imgs = dir.listFiles(f ->
                f.isFile() && (f.getName().toLowerCase().endsWith(".png")
                            || f.getName().toLowerCase().endsWith(".jpg")
                            || f.getName().toLowerCase().endsWith(".jpeg")));
        if (imgs == null) return;
        for (File img : imgs) {
            int[][] gray = ImageUtils.citesteGray(img);
            // Daca imaginea nu e exact la dim x dim, o redimensionam ca sa aiba
            // toti vectorii aceeasi lungime.
            if (gray.length != dim || gray[0].length != dim) {
                gray = ImageUtils.redimensioneaza(gray, dim, dim);
            }
            X.add(hog.extrage(gray));
            y.add(eticheta);
        }
    }

    /** Converteste List<double[]>, List<Integer> la tablouri primitive. */
    private static Set asambleaza(List<double[]> X, List<Integer> y) {
        double[][] arrX = X.toArray(new double[0][]);
        int[] arrY = new int[y.size()];
        for (int i = 0; i < y.size(); i++) arrY[i] = y.get(i);
        return new Set(arrX, arrY);
    }
}
