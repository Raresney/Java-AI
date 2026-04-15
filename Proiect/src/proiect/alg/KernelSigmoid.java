package proiect.alg;

/**
 * Nucleul Sigmoid: K(x, y) = tanh(gamma * (x . y) + r).
 *
 * Cerinta 7 din enunt cere explicit acest nucleu impreuna cu SMO.
 * NU este nucleu Mercer pentru orice (gamma, r), deci SMO poate sa
 * nu conveargă perfect — vom valida empiric.
 *
 * Parametri tipici: gamma = 1/numarTrasaturi, r = 0.
 */
public class KernelSigmoid implements Kernel {

    /** Versiunea pentru serializare; o fixam ca sa nu se rupa la modificari minore. */
    private static final long serialVersionUID = 1L;

    /** Coeficient de scalare a produsului scalar (analog cu pasul invatarii). */
    private final double gamma;

    /** Termen liber adunat in argumentul tangentei hiperbolice. */
    private final double r;

    /**
     * @param gamma coeficient pozitiv (de obicei 1.0 / numarTrasaturi)
     * @param r    termen liber (frecvent 0)
     */
    public KernelSigmoid(double gamma, double r) {
        this.gamma = gamma;
        this.r = r;
    }

    @Override
    public double k(double[] x, double[] y) {
        // Produsul scalar standard intre cei doi vectori.
        double dot = 0.0;
        for (int i = 0; i < x.length; i++) {
            dot += x[i] * y[i];
        }
        // Aplicam tangenta hiperbolica peste dot scalat si translatat.
        return Math.tanh(gamma * dot + r);
    }

    @Override
    public String name() {
        return "Sigmoid(gamma=" + gamma + ", r=" + r + ")";
    }
}
