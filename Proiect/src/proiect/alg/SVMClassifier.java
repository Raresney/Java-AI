package proiect.alg;

import java.io.Serializable;

/**
 * Modelul antrenat al unui SVM binar cu marja slaba (soft-margin).
 *
 * Decizia este: f(x) = sum_i ( alpha_i * y_i * K(SV_i, x) ) + b.
 * Eticheta returnata: +1 daca f(x) >= 0, altfel -1.
 *
 * Doar vectorii suport (acei x_i pentru care alpha_i > 0) sunt pastrati,
 * pentru ca restul nu contribuie la decizie.
 *
 * Clasa este serializabila pentru cerinta 6 (salvarea modelelor pe disc).
 */
public class SVMClassifier implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Vectorii suport: SV[i] este un vector de trasaturi (HOG). */
    private final double[][] sv;

    /** Etichetele vectorilor suport, +1 sau -1, paralele cu sv. */
    private final int[] svY;

    /** Coeficientii alpha ai vectorilor suport, in (0, C]. */
    private final double[] svAlpha;

    /** Termenul liber (bias) al hiperplanului. */
    private final double b;

    /** Nucleul folosit la antrenare; trebuie folosit identic la predictie. */
    private final Kernel kernel;

    /**
     * Cache-ul vectorului de greutati w pentru kernel Linear.
     * Pentru Linear: f(x) = w.x + b, unde w = sum_i (alpha_i * y_i * SV_i).
     * Daca il precalculam o data, decizia devine un singur dot product in loc
     * de nSV produse scalare — critic pentru detectie live (sute de ferestre/cadru).
     * transient = nu se serializeaza (se recalculeaza la primul decision() dupa load).
     */
    private transient double[] wLinear;

    /**
     * @param sv      matrice de vectori suport
     * @param svY     etichete +/-1 pentru fiecare vector suport
     * @param svAlpha coeficienti alpha pentru fiecare vector suport
     * @param b       bias
     * @param kernel  nucleu folosit la antrenare
     */
    public SVMClassifier(double[][] sv, int[] svY, double[] svAlpha,
                         double b, Kernel kernel) {
        this.sv = sv;
        this.svY = svY;
        this.svAlpha = svAlpha;
        this.b = b;
        this.kernel = kernel;
    }

    /**
     * Valoarea functiei de decizie f(x). Util pentru ranking (NMS dupa scor).
     *
     * Pentru kernel Linear folosim fast-path: w.x + b in loc de a suma peste
     * toti vectorii suport. La detectie live cu ~400 ferestre/cadru si 650 SV,
     * asta inseamna ~650x mai rapid.
     */
    public double decision(double[] x) {
        if (kernel instanceof KernelLinear) {
            if (wLinear == null) construiesteW();
            double s = 0.0;
            for (int i = 0; i < wLinear.length; i++) s += wLinear[i] * x[i];
            return s + b;
        }
        double s = 0.0;
        // Suma peste toti vectorii suport (kernel non-liniar).
        for (int i = 0; i < sv.length; i++) {
            s += svAlpha[i] * svY[i] * kernel.k(sv[i], x);
        }
        return s + b;
    }

    /** Construieste w = sum_i (alpha_i * y_i * SV_i) o singura data. */
    private void construiesteW() {
        int d = sv[0].length;
        double[] w = new double[d];
        for (int i = 0; i < sv.length; i++) {
            double coef = svAlpha[i] * svY[i];
            double[] s = sv[i];
            for (int j = 0; j < d; j++) w[j] += coef * s[j];
        }
        this.wLinear = w;
    }

    /**
     * Eticheta prezisa: +1 sau -1.
     */
    public int predict(double[] x) {
        return decision(x) >= 0.0 ? +1 : -1;
    }

    /** Numarul de vectori suport — util pentru log si depanare. */
    public int numSupportVectors() {
        return sv.length;
    }

    public Kernel getKernel() {
        return kernel;
    }
}
