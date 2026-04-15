package proiect.io;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Utilitare pentru lucrul cu imagini grayscale reprezentate ca int[H][W].
 *
 * Folosim javax.imageio (din JDK) pentru citire/scriere pe disc. OpenCV e
 * rezervat exclusiv pentru preluarea cadrelor webcam si desenare — cerinta 3
 * din enuntul temei nu ne permite sa folosim biblioteci pentru algoritmi.
 */
public final class ImageUtils {

    private ImageUtils() {} // clasa utilitara, nu se instantiaza

    /**
     * Citeste o imagine de pe disc si o converteste la grayscale.
     *
     * Formula luminanta ITU-R BT.601: Y = 0.299 R + 0.587 G + 0.114 B.
     * Am ales-o pentru ca e standardul clasic; corespunde si cu COLOR_BGR2GRAY din OpenCV.
     */
    public static int[][] citesteGray(File f) throws IOException {
        BufferedImage img = ImageIO.read(f);
        if (img == null) throw new IOException("Nu am putut citi " + f);
        return bufferedImageToGray(img);
    }

    /** Converteste un BufferedImage la int[H][W] grayscale. */
    public static int[][] bufferedImageToGray(BufferedImage img) {
        int H = img.getHeight();
        int W = img.getWidth();
        int[][] out = new int[H][W];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                out[y][x] = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return out;
    }

    /** Converteste o matrice grayscale la BufferedImage (pentru salvare / GUI). */
    public static BufferedImage grayToBufferedImage(int[][] gray) {
        int H = gray.length;
        int W = gray[0].length;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int v = Math.max(0, Math.min(255, gray[y][x]));
                // Intr-o imagine TYPE_BYTE_GRAY, R=G=B=v si alpha=255.
                int rgb = (0xFF << 24) | (v << 16) | (v << 8) | v;
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    /** Salveaza o matrice grayscale pe disc ca PNG. */
    public static void salveazaPNG(int[][] gray, File f) throws IOException {
        ImageIO.write(grayToBufferedImage(gray), "png", f);
    }

    /**
     * Decupeaza o regiune dintr-o imagine grayscale. Daca regiunea iese
     * din imagine, valorile de in afara se clampeaza la marginile imaginii
     * (comportament "clamp to edge"). Asta simplifica sliding window-ul.
     */
    public static int[][] decupeaza(int[][] src, int x, int y, int w, int h) {
        int H = src.length;
        int W = src[0].length;
        int[][] out = new int[h][w];
        for (int j = 0; j < h; j++) {
            int sy = Math.max(0, Math.min(H - 1, y + j));
            for (int i = 0; i < w; i++) {
                int sx = Math.max(0, Math.min(W - 1, x + i));
                out[j][i] = src[sy][sx];
            }
        }
        return out;
    }

    /**
     * Redimensionare biliniara la noua dimensiune (noiH x noiW).
     *
     * Pentru fiecare pixel tinta, calculam pozitia corespunzatoare in sursa
     * si interpolam cei 4 pixeli vecini. Mai bun decat nearest neighbor pt
     * scalari mari; destul de rapid pentru piramida detectiei.
     */
    public static int[][] redimensioneaza(int[][] src, int noiH, int noiW) {
        int H = src.length;
        int W = src[0].length;
        int[][] out = new int[noiH][noiW];

        // Raporturile de scalare intre sursa si tinta.
        double scaleY = (double) H / noiH;
        double scaleX = (double) W / noiW;

        for (int y = 0; y < noiH; y++) {
            double sy = (y + 0.5) * scaleY - 0.5; // centre de pixel
            int y0 = (int) Math.floor(sy);
            int y1 = y0 + 1;
            double fy = sy - y0;
            if (y0 < 0) { y0 = 0; fy = 0; }
            if (y1 >= H) y1 = H - 1;

            for (int x = 0; x < noiW; x++) {
                double sx = (x + 0.5) * scaleX - 0.5;
                int x0 = (int) Math.floor(sx);
                int x1 = x0 + 1;
                double fx = sx - x0;
                if (x0 < 0) { x0 = 0; fx = 0; }
                if (x1 >= W) x1 = W - 1;

                // Cei 4 pixeli din jurul pozitiei (sx, sy).
                double v00 = src[y0][x0];
                double v01 = src[y0][x1];
                double v10 = src[y1][x0];
                double v11 = src[y1][x1];

                // Interpolare liniara pe X, apoi pe Y.
                double v0 = v00 * (1 - fx) + v01 * fx;
                double v1 = v10 * (1 - fx) + v11 * fx;
                double v  = v0  * (1 - fy) + v1  * fy;

                out[y][x] = (int) Math.round(v);
            }
        }
        return out;
    }
}
