package proiect.alg;

import java.util.Random;

/**
 * Sequential Minimal Optimization (Platt, 1998).
 *
 * Rezolva problema duala a SVM-ului cu marja slaba:
 *   maximizeaza  W(alpha) = sum alpha_i - 1/2 sum_i sum_j alpha_i alpha_j y_i y_j K(x_i, x_j)
 *   sub          0 <= alpha_i <= C  si  sum alpha_i y_i = 0
 *
 * Idee SMO: la fiecare pas alegem 2 alpha-uri (i, j) si optimizam analitic
 * problema redusa in 2 variabile, mentinand celelalte fixate. Acest lucru
 * e posibil pentru ca restrictia de egalitate face ca alpha_j sa fie
 * determinat o data ce alpha_i e ales.
 *
 * Aceasta implementare urmeaza varianta simplificata din Andrew Ng
 * (CS229 notes), care alege j aleator daca i violeaza KKT. Este mai
 * lenta decat varianta "full" cu euristica pentru j, dar e mai usor de
 * inteles si suficienta pentru seturi de antrenament moderate (sute–mii
 * de exemple).
 */
public class SMO {

    /** Parametrul de regularizare (cat de mult penalizam erorile de margine). */
    private final double C;

    /** Toleranta pentru testul KKT. */
    private final double tol;

    /** Numarul maxim de treceri complete fara nicio modificare inainte de stop. */
    private final int maxPasses;

    /** Generator pentru alegerea aleatoare a celui de-al doilea alpha. */
    private final Random rng;

    public SMO(double C, double tol, int maxPasses, long seed) {
        this.C = C;
        this.tol = tol;
        this.maxPasses = maxPasses;
        this.rng = new Random(seed);
    }

    /**
     * Antreneaza un SVM binar pe (X, y) cu nucleul dat.
     *
     * @param X      matrice [n][d] de exemple
     * @param y      etichete +/-1, lungime n
     * @param kernel nucleul folosit
     * @return clasificator antrenat (contine doar vectorii suport)
     */
    public SVMClassifier antreneaza(double[][] X, int[] y, Kernel kernel) {
        final int n = X.length;
        // alpha[i] sunt variabilele duale; pornim cu zero (toate exemplele "in marja").
        double[] alpha = new double[n];
        // bias-ul hiperplanului.
        double b = 0.0;

        // Cache pentru valorile kernelului — evitam recalculul pentru
        // perechile vizitate des. K[i][j] = kernel.k(X[i], X[j]).
        // Pentru n mare (>~3000) cache-ul plin nu mai incape; aici asumam n <= cateva mii.
        double[][] K = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                double v = kernel.k(X[i], X[j]);
                K[i][j] = v;
                K[j][i] = v; // simetric
            }
        }

        int passes = 0;
        // Cat timp facem treceri fara nicio modificare, ne apropiem de optim.
        while (passes < maxPasses) {
            int numChanged = 0;

            // Iteram peste fiecare exemplu i si verificam KKT.
            for (int i = 0; i < n; i++) {
                // Calculam eroarea de predictie pentru exemplul i.
                double Ei = decizie(alpha, y, K, b, i) - y[i];

                // Conditia KKT incalcata: y_i*E_i < -tol cu alpha_i < C, sau
                // y_i*E_i > tol cu alpha_i > 0.
                if ((y[i] * Ei < -tol && alpha[i] < C) ||
                    (y[i] * Ei >  tol && alpha[i] > 0)) {

                    // Alegem j != i aleator (varianta simplificata).
                    int j = i;
                    while (j == i) {
                        j = rng.nextInt(n);
                    }
                    double Ej = decizie(alpha, y, K, b, j) - y[j];

                    // Salvam vechile valori (avem nevoie de ele in update-ul lui b).
                    double alphaIold = alpha[i];
                    double alphaJold = alpha[j];

                    // Calculam capetele segmentului in care alpha_j poate sa varieze
                    // mentinand restrictia de egalitate si 0 <= alpha <= C.
                    double L, H;
                    if (y[i] != y[j]) {
                        L = Math.max(0.0, alpha[j] - alpha[i]);
                        H = Math.min(C,   C + alpha[j] - alpha[i]);
                    } else {
                        L = Math.max(0.0, alpha[i] + alpha[j] - C);
                        H = Math.min(C,   alpha[i] + alpha[j]);
                    }
                    // Daca segmentul e degenerat, sarim peste perechea asta.
                    if (L == H) continue;

                    // eta = 2*K_ij - K_ii - K_jj este derivata a doua a obiectivului
                    // in raport cu alpha_j, cu semn schimbat. Daca eta >= 0, problema
                    // redusa nu e strict concava (poate aparea la kernel Sigmoid
                    // care nu e Mercer) — sarim ca sa evitam pasi rai.
                    double eta = 2.0 * K[i][j] - K[i][i] - K[j][j];
                    if (eta >= 0) continue;

                    // Update analitic pentru alpha_j si proiectie pe [L, H].
                    alpha[j] = alphaJold - (y[j] * (Ei - Ej)) / eta;
                    if (alpha[j] > H) alpha[j] = H;
                    else if (alpha[j] < L) alpha[j] = L;

                    // Daca modificarea e neglijabila, nu consideram pasul ca valid.
                    if (Math.abs(alpha[j] - alphaJold) < 1e-5) continue;

                    // Update pentru alpha_i pastrand restrictia de egalitate.
                    alpha[i] = alphaIold + y[i] * y[j] * (alphaJold - alpha[j]);

                    // Calculam doi candidati pentru noul bias si alegem dupa
                    // care alpha e in interiorul (0, C).
                    double b1 = b - Ei
                              - y[i] * (alpha[i] - alphaIold) * K[i][i]
                              - y[j] * (alpha[j] - alphaJold) * K[i][j];
                    double b2 = b - Ej
                              - y[i] * (alpha[i] - alphaIold) * K[i][j]
                              - y[j] * (alpha[j] - alphaJold) * K[j][j];
                    if (alpha[i] > 0 && alpha[i] < C)      b = b1;
                    else if (alpha[j] > 0 && alpha[j] < C) b = b2;
                    else                                    b = (b1 + b2) / 2.0;

                    numChanged++;
                }
            }

            // Daca o trecere completa nu a modificat nimic, o numaram; cand atingem
            // maxPasses treceri "linistite" consecutive, oprim antrenarea.
            if (numChanged == 0) passes++;
            else                  passes = 0;
        }

        // Construim modelul final pastrand doar vectorii suport (alpha_i > 0).
        int nSV = 0;
        for (int i = 0; i < n; i++) if (alpha[i] > 1e-8) nSV++;

        double[][] sv      = new double[nSV][];
        int[]      svY     = new int[nSV];
        double[]   svAlpha = new double[nSV];
        int idx = 0;
        for (int i = 0; i < n; i++) {
            if (alpha[i] > 1e-8) {
                sv[idx]      = X[i];
                svY[idx]     = y[i];
                svAlpha[idx] = alpha[i];
                idx++;
            }
        }
        return new SVMClassifier(sv, svY, svAlpha, b, kernel);
    }

    /**
     * Functia de decizie f(x_idx) = sum alpha_k y_k K(x_k, x_idx) + b,
     * folosind cache-ul de kernel. Doar pe parcursul antrenarii.
     */
    private double decizie(double[] alpha, int[] y, double[][] K, double b, int idx) {
        double s = 0.0;
        for (int k = 0; k < alpha.length; k++) {
            if (alpha[k] != 0.0) {
                s += alpha[k] * y[k] * K[k][idx];
            }
        }
        return s + b;
    }
}
