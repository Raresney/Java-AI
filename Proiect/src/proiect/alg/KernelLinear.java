package proiect.alg;

/**
 * Nucleul liniar: K(x, y) = x . y.
 *
 * Util pentru testare comparativa cu Sigmoid si pentru baseline rapid:
 * cand trasaturile sunt deja de dimensiune mare (HOG ~3780), nucleul
 * liniar lucreaza adesea la fel de bine si se antreneaza mai repede.
 */
public class KernelLinear implements Kernel {

    private static final long serialVersionUID = 1L;

    @Override
    public double k(double[] x, double[] y) {
        double dot = 0.0;
        for (int i = 0; i < x.length; i++) {
            dot += x[i] * y[i];
        }
        return dot;
    }

    @Override
    public String name() {
        return "Linear";
    }
}
