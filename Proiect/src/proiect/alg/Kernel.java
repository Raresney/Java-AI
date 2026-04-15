package proiect.alg;

import java.io.Serializable;

/**
 * Interfata pentru un nucleu (kernel) folosit de SVM.
 *
 * Un nucleu calculeaza un produs scalar intr-un spatiu de trasaturi
 * (eventual de dimensiune infinita) fara a transforma explicit vectorii.
 * Astfel, SVM-ul poate gasi separatori neliniari in spatiul original.
 *
 * Implementarile trebuie sa fie {@link Serializable} pentru ca un
 * {@link SVMClassifier} antrenat (care contine kernelul) sa poata fi
 * salvat pe disc cu {@link java.io.ObjectOutputStream}.
 */
public interface Kernel extends Serializable {

    /**
     * Calculeaza valoarea nucleului K(x, y).
     *
     * @param x primul vector
     * @param y al doilea vector (aceeasi dimensiune ca x)
     * @return K(x, y)
     */
    double k(double[] x, double[] y);

    /**
     * Numele nucleului — folosit doar pentru log/debug.
     */
    String name();
}
