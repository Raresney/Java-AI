package proiect.alg;

/**
 * Histogram of Oriented Gradients (Dalal & Triggs, CVPR 2005).
 *
 * Implementare pura, fara biblioteci. Pasii:
 *  1) Calculam gradientii Gx, Gy cu diferente centrate ([-1, 0, +1]).
 *     Nu folosim Sobel mai mare — originalul din Dalal & Triggs foloseste
 *     explicit kernelul [-1, 0, +1] si argumenteaza ca merge cel mai bine.
 *  2) Magnitudinea: mag = sqrt(Gx^2 + Gy^2); unghiul: atan2(Gy, Gx) in [0, 180) deg
 *     (gradient "unsigned" — cel mai bun pentru detectie de persoane).
 *  3) Impartim imaginea in celule cellSize x cellSize (default 8x8).
 *     Fiecare celula da o histograma cu nbins (default 9) binuri peste [0, 180)
 *     cu interpolare liniara intre binuri vecine (votarea e ponderata cu
 *     magnitudinea gradientului).
 *  4) Grupam celulele in blocuri blockSize x blockSize (default 2x2), cu pas 1 celula,
 *     concatenam histogramele si normalizam L2-Hys: normalizare L2, taiem valorile
 *     > 0.2, renormalizam L2. Scopul: invarianta la iluminare si contrast.
 *  5) Concatenam toate blocurile => vectorul final.
 *
 * Pentru o fereastra 64x64 cu cellSize=8, blockSize=2, nbins=9:
 *   celule: 8x8, blocuri: 7x7, lungime = 7*7*2*2*9 = 1764.
 * Pentru 128x128: 15*15*2*2*9 = 8100.
 */
public class HOG {

    /** Dimensiunea laturii unei celule in pixeli. */
    private final int cellSize;

    /** Dimensiunea laturii unui bloc in celule. */
    private final int blockSize;

    /** Numarul de binuri pentru histograma de orientare. */
    private final int nbins;

    /** Pasul blocurilor, in celule (default 1 = blocuri suprapuse). */
    private final int blockStride;

    /** Constructor cu valorile recomandate de Dalal & Triggs. */
    public HOG() {
        this(8, 2, 9, 1);
    }

    public HOG(int cellSize, int blockSize, int nbins, int blockStride) {
        this.cellSize = cellSize;
        this.blockSize = blockSize;
        this.nbins = nbins;
        this.blockStride = blockStride;
    }

    /**
     * Extrage vectorul HOG dintr-o imagine grayscale.
     *
     * @param gray matrice [H][W] cu valori in [0, 255]
     * @return vector de trasaturi; lungimea depinde de parametri si dimensiunile imaginii
     */
    public double[] extrage(int[][] gray) {
        final int H = gray.length;
        final int W = gray[0].length;

        // --- PASUL 1: gradientii Gx, Gy cu diferente centrate. ---
        // La margini replicam pixelul (extindem imaginea) ca sa evitam out-of-bounds.
        double[][] mag = new double[H][W];
        double[][] ang = new double[H][W]; // in grade, [0, 180)

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int xm = x > 0 ? x - 1 : x;
                int xp = x < W - 1 ? x + 1 : x;
                int ym = y > 0 ? y - 1 : y;
                int yp = y < H - 1 ? y + 1 : y;

                double gx = gray[y][xp] - gray[y][xm];
                double gy = gray[yp][x] - gray[ym][x];

                mag[y][x] = Math.sqrt(gx * gx + gy * gy);

                // atan2 returneaza in (-PI, PI]. Vrem "unsigned" in [0, 180),
                // deci luam valoarea absoluta a atan2 si o convertim in grade.
                double deg = Math.toDegrees(Math.atan2(gy, gx));
                if (deg < 0) deg += 180;
                if (deg >= 180) deg -= 180;
                ang[y][x] = deg;
            }
        }

        // --- PASUL 2 si 3: histograme per celula. ---
        final int cellsY = H / cellSize;
        final int cellsX = W / cellSize;
        // hist[cy][cx][bin] — histograma celulei (cy, cx).
        double[][][] hist = new double[cellsY][cellsX][nbins];

        // Latimea unui bin in grade.
        final double binWidth = 180.0 / nbins;

        for (int y = 0; y < cellsY * cellSize; y++) {
            for (int x = 0; x < cellsX * cellSize; x++) {
                double m = mag[y][x];
                if (m == 0) continue; // optimizare: gradient nul nu contribuie

                double a = ang[y][x];
                // Binul "stang" pe care il acopera unghiul a — centrele sunt
                // la (bin + 0.5) * binWidth; cautam binul cu centru <= a.
                double pos = a / binWidth - 0.5; // pozitie in binuri fata de centru
                int lo = (int) Math.floor(pos);
                double frac = pos - lo;
                int hi = lo + 1;
                // Wrap-around: [0, 180) e ciclic pentru gradient "unsigned",
                // deci binul -1 inseamna ultimul bin, iar binul nbins inseamna 0.
                if (lo < 0) lo += nbins;
                if (hi >= nbins) hi -= nbins;

                int cy = y / cellSize;
                int cx = x / cellSize;
                // Voteaza in ambele binuri, proportional cu (1 - frac) si frac.
                hist[cy][cx][lo] += m * (1.0 - frac);
                hist[cy][cx][hi] += m * frac;
            }
        }

        // --- PASUL 4: blocuri suprapuse + normalizare L2-Hys. ---
        final int blocksY = (cellsY - blockSize) / blockStride + 1;
        final int blocksX = (cellsX - blockSize) / blockStride + 1;
        final int blockLen = blockSize * blockSize * nbins;
        double[] feat = new double[blocksY * blocksX * blockLen];
        int outIdx = 0;

        for (int by = 0; by < blocksY; by++) {
            for (int bx = 0; bx < blocksX; bx++) {
                // Concatenam histogramele celor blockSize x blockSize celule.
                double[] v = new double[blockLen];
                int p = 0;
                for (int dy = 0; dy < blockSize; dy++) {
                    for (int dx = 0; dx < blockSize; dx++) {
                        int cy = by * blockStride + dy;
                        int cx = bx * blockStride + dx;
                        for (int bi = 0; bi < nbins; bi++) {
                            v[p++] = hist[cy][cx][bi];
                        }
                    }
                }

                // L2-Hys: L2, clip la 0.2, L2 din nou.
                normalizareL2(v);
                for (int i = 0; i < v.length; i++) {
                    if (v[i] > 0.2) v[i] = 0.2;
                }
                normalizareL2(v);

                // Copiem blocul normalizat in vectorul final.
                System.arraycopy(v, 0, feat, outIdx, blockLen);
                outIdx += blockLen;
            }
        }
        return feat;
    }

    /** Normalizare L2 in-place; daca norma e zero, vectorul ramane zero. */
    private static void normalizareL2(double[] v) {
        double s = 0.0;
        for (double x : v) s += x * x;
        double norm = Math.sqrt(s + 1e-10); // epsilon ca sa evitam diviziunea la 0
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) v[i] /= norm;
        }
    }

    /** Lungimea vectorului de trasaturi pentru o imagine H x W (util la dimensionare). */
    public int lungimeVector(int H, int W) {
        int cellsY = H / cellSize;
        int cellsX = W / cellSize;
        int blocksY = (cellsY - blockSize) / blockStride + 1;
        int blocksX = (cellsX - blockSize) / blockStride + 1;
        return blocksY * blocksX * blockSize * blockSize * nbins;
    }
}
