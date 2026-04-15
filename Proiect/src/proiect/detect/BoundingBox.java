package proiect.detect;

/**
 * Un patrat de detectie ("patrat cap") din imagine.
 *
 * Folosit atat pe timpul detectiei (sliding window produce multi candidati)
 * cat si la desenare peste imaginea finala. Stocheaza scorul clasificatorului
 * pentru a permite Non-Maximum Suppression si filtrare dupa prag.
 */
public class BoundingBox {

    /** Coltul stanga-sus, coordonata x (in pixeli). */
    public final int x;

    /** Coltul stanga-sus, coordonata y. */
    public final int y;

    /** Latura patratului. */
    public final int latura;

    /** Scorul clasificatorului (valoarea functiei de decizie). */
    public final double scor;

    public BoundingBox(int x, int y, int latura, double scor) {
        this.x = x;
        this.y = y;
        this.latura = latura;
        this.scor = scor;
    }

    /** Aria = latura^2 (cerinta 2: alegem patratul cu aria maxima). */
    public int aria() {
        return latura * latura;
    }

    /**
     * Aria intersectiei cu un alt patrat, sau 0 daca nu se suprapun.
     * Folosit la calculul IoU.
     */
    public int ariaIntersectie(BoundingBox b) {
        int x1 = Math.max(this.x, b.x);
        int y1 = Math.max(this.y, b.y);
        int x2 = Math.min(this.x + this.latura, b.x + b.latura);
        int y2 = Math.min(this.y + this.latura, b.y + b.latura);
        if (x2 <= x1 || y2 <= y1) return 0;
        return (x2 - x1) * (y2 - y1);
    }

    /**
     * Intersection over Union — raport folosit la NMS.
     * Valori aproape de 1 = patratele sunt aproape identice.
     */
    public double iou(BoundingBox b) {
        int inter = ariaIntersectie(b);
        int uniune = this.aria() + b.aria() - inter;
        return uniune == 0 ? 0.0 : (double) inter / uniune;
    }
}
