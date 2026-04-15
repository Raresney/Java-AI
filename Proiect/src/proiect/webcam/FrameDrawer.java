package proiect.webcam;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import proiect.detect.BoundingBox;

/**
 * Deseneaza patrate verzi si etichete peste un Mat BGR (cerinta 8).
 *
 * OpenCV e folosit aici doar pentru primitiva grafica (rectangle, putText).
 * Nu se face nicio "logica" — doar randare.
 */
public final class FrameDrawer {

    /** Culoarea verde in BGR (nu RGB — OpenCV lucreaza in BGR). */
    private static final Scalar VERDE = new Scalar(0, 255, 0);

    private FrameDrawer() {}

    /** Deseneaza un patrat peste frame. */
    public static void deseneazaPatrat(Mat frame, BoundingBox b) {
        Imgproc.rectangle(frame,
                new Point(b.x, b.y),
                new Point(b.x + b.latura, b.y + b.latura),
                VERDE, 2);
    }

    /** Deseneaza toate patratele + eticheta (daca nu e null) deasupra fiecaruia. */
    public static void deseneazaCuEticheta(Mat frame, List<BoundingBox> boxes, List<String> etichete) {
        for (int i = 0; i < boxes.size(); i++) {
            BoundingBox b = boxes.get(i);
            deseneazaPatrat(frame, b);
            String text = (etichete != null && i < etichete.size()) ? etichete.get(i) : null;
            if (text != null) {
                // Plasam textul imediat deasupra patratului.
                Imgproc.putText(frame, text,
                        new Point(b.x, Math.max(0, b.y - 5)),
                        Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, VERDE, 2);
            }
        }
    }

    /**
     * Converteste un Mat BGR la BufferedImage pentru afisare in Swing.
     * Nu foloseste OpenCV pentru "algoritm" — doar pentru a muta pixelii
     * intre doua reprezentari.
     */
    public static BufferedImage matLaBufferedImage(Mat mat) {
        int W = mat.cols();
        int H = mat.rows();
        int canale = mat.channels();

        BufferedImage img;
        if (canale == 1) {
            img = new BufferedImage(W, H, BufferedImage.TYPE_BYTE_GRAY);
        } else {
            img = new BufferedImage(W, H, BufferedImage.TYPE_3BYTE_BGR);
        }
        byte[] buf = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        mat.get(0, 0, buf);
        return img;
    }
}
