package proiect.webcam;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Wrapper peste OpenCV VideoCapture — SINGURUL loc unde folosim OpenCV
 * in mod semnificativ (conform cerintei 3 din enunt: "pot fi utilizate
 * clase si pachete din java-openCV, dar numai pentru preluarea imaginilor
 * de la camera si pentru desenarea patratelor pe imagine").
 *
 * Expunem imaginile ca int[H][W] grayscale, astfel incat restul codului
 * sa nu depinda deloc de OpenCV.
 */
public class CameraCapture {

    /** Incarcam biblioteca nativa o singura data, la primul acces al clasei. */
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /** Handler-ul OpenCV catre camera. */
    private final VideoCapture cap;

    /** Matricea nativa reutilizata la fiecare captura (evitam alocari). */
    private final Mat frame = new Mat();

    /** Index-ul camerei (0 = prima camera detectata de OS). */
    public CameraCapture(int index) {
        this.cap = new VideoCapture(index);
        // Optional: setam o rezolutie standard ca sa avem ceva de lucru.
        cap.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
        cap.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
    }

    public boolean esteDeschisa() {
        return cap.isOpened();
    }

    /**
     * Preia urmatorul cadru si il returneaza ca int[H][W] grayscale.
     * @return matrice sau null daca cadrul nu a putut fi citit
     */
    public int[][] cadruGray() {
        if (!cap.read(frame) || frame.empty()) return null;

        int H = frame.rows();
        int W = frame.cols();
        int[][] out = new int[H][W];

        // OpenCV returneaza BGR cu 3 canale, 1 byte per canal.
        byte[] buf = new byte[W * 3];
        for (int y = 0; y < H; y++) {
            frame.get(y, 0, buf); // citim un rand
            for (int x = 0; x < W; x++) {
                int b = buf[x * 3    ] & 0xFF;
                int g = buf[x * 3 + 1] & 0xFF;
                int r = buf[x * 3 + 2] & 0xFF;
                // Aceeasi formula luminanta ca in ImageUtils.
                out[y][x] = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
            }
        }
        return out;
    }

    /**
     * Returneaza si cadrul BGR brut pentru desenare peste el ({@link FrameDrawer}).
     * Cadrul e proprietatea acestui obiect — se schimba la urmatorul apel de
     * {@link #cadruGray()}, deci apelantul trebuie sa-l foloseasca imediat.
     */
    public Mat cadruBGR() {
        return frame;
    }

    public void inchide() {
        cap.release();
    }
}
