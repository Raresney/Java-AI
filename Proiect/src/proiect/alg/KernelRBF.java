package proiect.alg;

/**
 * Nucleul Radial Basis Function (Gaussian): K(x, y) = exp(-gamma * ||x-y||^2).
 *
 * Inclus pentru testare comparativa (cerinta 7 permite si alte nuclee).
 * gamma controleaza "latimea" — valori mari = decizii mai locale.
 */
public class KernelRBF implements Kernel {

    private static final long serialVersionUID = 1L;

    private final double gamma;

    public KernelRBF(double gamma) {
        this.gamma = gamma;
    }

    @Override
    public double k(double[] x, double[] y) {
        // Calculam patratul distantei euclidiene fara a extrage radical.
        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            double d = x[i] - y[i];
            sum += d * d;
        }
        return Math.exp(-gamma * sum);
    }

    @Override
    public String name() {
        return "RBF(gamma=" + gamma + ")";
    }
}
