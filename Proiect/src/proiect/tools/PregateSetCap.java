package proiect.tools;

import java.io.*;
import java.util.Random;

import proiect.io.ImageUtils;

/**
 * Pregateste setul de invatare pentru detectorul de cap (cerinta 1):
 *  - converteste imaginile ORL (92x112 PGM grayscale) la 64x64 PNG in pozitive/
 *  - genereaza 400 imagini 64x64 sintetice non-face in negative/
 *     (zgomot uniform, gradienti, dungi, tabla de sah, texturi)
 *  - ruleaza antrenarea detectorului de cap
 *
 * ORL = AT&T / ORL Database of Faces: 40 subiecti x 10 poze = 400 fete,
 * descarcat de pe un mirror public (cam-orl.co.uk e offline, mirror GitHub).
 * Aceasta colectie e acceptata pentru cerinta 1 ("imagini preluate din Internet").
 */
public class PregateSetCap {

    /** Directorul cu subfoldere s1..s40, fiecare cu 10 PGM-uri. */
    private static final File DIR_ORL =
            new File("downloads/orl-repo/att_faces");

    private static final File DIR_POZITIVE =
            new File("invatare_cap/pozitive");

    private static final File DIR_NEGATIVE =
            new File("invatare_cap/negative");

    public static void main(String[] args) throws Exception {
        DIR_POZITIVE.mkdirs();
        DIR_NEGATIVE.mkdirs();

        int nPoz = proceseazaORL();
        System.out.println("Pozitive create: " + nPoz);

        int nNeg = genereazaNegative(nPoz); // acelasi numar ca pozitivele
        System.out.println("Negative create: " + nNeg);

        System.out.println("\n=== Antrenare detector cap ===");
        Antrenare.antreneazaDetectorCap();
    }

    /**
     * Citeste toate PGM-urile din ORL si le salveaza ca PNG 64x64 grayscale
     * in invatare_cap/pozitive/.
     */
    private static int proceseazaORL() throws IOException {
        int n = 0;
        File[] subiecti = DIR_ORL.listFiles(File::isDirectory);
        if (subiecti == null) {
            throw new IOException("Nu gasesc dataset-ul la " + DIR_ORL.getAbsolutePath());
        }
        for (File sub : subiecti) {
            File[] imgs = sub.listFiles((d, name) -> name.toLowerCase().endsWith(".pgm"));
            if (imgs == null) continue;
            for (File pgm : imgs) {
                int[][] gray = citestePGM(pgm);
                // ORL: 92 x 112 -> decupam central la 92x92 (patrat), apoi 64x64
                int y0 = (gray.length - 92) / 2;
                int[][] patrat = ImageUtils.decupeaza(gray, 0, y0, 92, 92);
                int[][] r64 = ImageUtils.redimensioneaza(patrat, 64, 64);
                String nume = "p_" + sub.getName() + "_" + pgm.getName().replace(".pgm", ".png");
                ImageUtils.salveazaPNG(r64, new File(DIR_POZITIVE, nume));
                n++;
            }
        }
        return n;
    }

    /**
     * Parser minimal pentru PGM binar (P5): header text + date pixel raw.
     * Formatul: "P5\n<width> <height>\n<maxval>\n<binary>"
     */
    private static int[][] citestePGM(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            String magic = citesteLinie(in);
            if (!"P5".equals(magic)) throw new IOException("Nu e PGM binar: " + f);
            String dims = citesteLinie(in);
            while (dims.startsWith("#")) dims = citesteLinie(in); // comentarii
            String[] wh = dims.trim().split("\\s+");
            int W = Integer.parseInt(wh[0]);
            int H = Integer.parseInt(wh[1]);
            int maxVal = Integer.parseInt(citesteLinie(in).trim());
            if (maxVal > 255) throw new IOException("PGM 16-bit nesuportat.");
            int[][] out = new int[H][W];
            for (int y = 0; y < H; y++) {
                for (int x = 0; x < W; x++) {
                    out[y][x] = in.readUnsignedByte();
                }
            }
            return out;
        }
    }

    /** Citeste o linie ASCII terminata cu \n din stream binar. */
    private static String citesteLinie(DataInputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1 && c != '\n') {
            if (c != '\r') sb.append((char) c);
        }
        return sb.toString();
    }

    /**
     * Genereaza imagini sintetice 64x64 care NU sunt fete. Folosim 5 clase
     * de patternuri, in parti egale, ca sa avem diversitate:
     *  1. Zgomot uniform
     *  2. Gradient diagonal cu zgomot mic
     *  3. Dungi orizontale / verticale
     *  4. Tabla de sah cu celule variabile
     *  5. Pete mari (blob-uri gaussiene)
     * Acestea produc semnaturi HOG diferite de fete si constituie negative
     * utile pentru SVM sa invete granita "fata vs ne-fata".
     */
    private static int genereazaNegative(int cati) throws IOException {
        Random rng = new Random(12345); // seed fix pentru reproductibilitate
        int n = 0;
        for (int i = 0; i < cati; i++) {
            int tip = i % 5;
            int[][] img;
            switch (tip) {
                case 0: img = zgomot(rng); break;
                case 1: img = gradient(rng); break;
                case 2: img = dungi(rng); break;
                case 3: img = sah(rng); break;
                default: img = pete(rng); break;
            }
            ImageUtils.salveazaPNG(img, new File(DIR_NEGATIVE, String.format("n_%04d.png", i)));
            n++;
        }
        return n;
    }

    private static int[][] zgomot(Random rng) {
        int[][] img = new int[64][64];
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++)
                img[y][x] = rng.nextInt(256);
        return img;
    }

    private static int[][] gradient(Random rng) {
        // Gradient in directie aleatoare + zgomot mic pentru a avea edge-uri reale.
        double teta = rng.nextDouble() * 2 * Math.PI;
        double cx = Math.cos(teta), cy = Math.sin(teta);
        int[][] img = new int[64][64];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                double v = (x * cx + y * cy) * (255.0 / 90) + rng.nextInt(20);
                img[y][x] = Math.max(0, Math.min(255, (int) v));
            }
        }
        return img;
    }

    private static int[][] dungi(Random rng) {
        // Dungi paralele cu latime 2..8 pixeli, orizontale sau verticale.
        int latime = 2 + rng.nextInt(7);
        boolean orizontal = rng.nextBoolean();
        int[][] img = new int[64][64];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int idx = orizontal ? y : x;
                img[y][x] = (idx / latime) % 2 == 0 ? 40 : 210;
            }
        }
        return img;
    }

    private static int[][] sah(Random rng) {
        int cell = 4 + rng.nextInt(13); // 4..16 pixeli
        int[][] img = new int[64][64];
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                boolean alb = ((x / cell) + (y / cell)) % 2 == 0;
                img[y][x] = alb ? 220 : 35;
            }
        }
        return img;
    }

    private static int[][] pete(Random rng) {
        // Mai multe pete gaussiene plasate aleator.
        int[][] img = new int[64][64];
        // Fond mediu.
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++)
                img[y][x] = 128;
        int nPete = 3 + rng.nextInt(4);
        for (int p = 0; p < nPete; p++) {
            int cx = rng.nextInt(64);
            int cy = rng.nextInt(64);
            double sig2 = 20 + rng.nextDouble() * 60;
            double amp = -80 + rng.nextDouble() * 160; // fie intuneric, fie lumina
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    double d2 = (x-cx)*(x-cx) + (y-cy)*(y-cy);
                    img[y][x] += (int) (amp * Math.exp(-d2 / sig2));
                }
            }
        }
        // Clamp la [0, 255].
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++)
                img[y][x] = Math.max(0, Math.min(255, img[y][x]));
        return img;
    }
}
