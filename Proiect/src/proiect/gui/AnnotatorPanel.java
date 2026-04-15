package proiect.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.*;

import proiect.io.ImageUtils;

/**
 * Panel pentru adnotarea manuala a imaginilor descarcate din Internet.
 *
 * Utilizatorul:
 *  1) alege un director cu imagini raw (.jpg/.png)
 *  2) selecteaza o imagine din lista
 *  3) deseneaza patrate peste fiecare fata, prin clic si drag cu mouse-ul
 *  4) apasa "Salveaza" — patratele devin exemple pozitive (64x64 grayscale
 *     in invatare_cap/pozitive/), iar din aceeasi imagine se extrag cateva
 *     patrate random care nu se suprapun, salvate ca exemple negative.
 *
 * Nota: cerinta 5 din enunt interzice "modele neantrenate" (haarcascades);
 * aici NU folosim nicio detectie automata — adnotarea e 100% manuala.
 */
public class AnnotatorPanel extends JPanel {

    /** Directorul curent cu imagini raw. */
    private File dirRaw;

    /** Lista de imagini din dirRaw, afisata in JList. */
    private final DefaultListModel<String> modelLista = new DefaultListModel<>();
    private final JList<String> lista = new JList<>(modelLista);

    /** Imaginea curent afisata. */
    private BufferedImage imaginea;

    /** Patratele desenate peste imaginea curenta (coordonate ale imaginii). */
    private final List<Rectangle> patrate = new ArrayList<>();

    /** Patratul in curs de desenare (in timpul drag-ului). */
    private Rectangle curent;

    /** Coltul initial al drag-ului. */
    private Point start;

    /** Panoul de desen al imaginii. */
    private final JPanel canvas = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (imaginea != null) {
                g.drawImage(imaginea, 0, 0, null);
                g.setColor(Color.GREEN);
                ((Graphics2D) g).setStroke(new BasicStroke(2));
                for (Rectangle r : patrate) g.drawRect(r.x, r.y, r.width, r.height);
                if (curent != null) g.drawRect(curent.x, curent.y, curent.width, curent.height);
            }
        }
        @Override
        public Dimension getPreferredSize() {
            return imaginea == null ? new Dimension(600, 400)
                    : new Dimension(imaginea.getWidth(), imaginea.getHeight());
        }
    };

    public AnnotatorPanel() {
        setLayout(new BorderLayout());

        // Bara de sus: butoane.
        JPanel sus = new JPanel();
        JButton bAlege = new JButton("Alege director imagini...");
        bAlege.addActionListener(e -> alegeDirector());
        JButton bUndo = new JButton("Sterge ultimul patrat");
        bUndo.addActionListener(e -> { if (!patrate.isEmpty()) { patrate.remove(patrate.size()-1); canvas.repaint(); } });
        JButton bSalveaza = new JButton("Salveaza patrate (pozitive + negative)");
        bSalveaza.addActionListener(e -> salveaza());
        sus.add(bAlege);
        sus.add(bUndo);
        sus.add(bSalveaza);
        add(sus, BorderLayout.NORTH);

        // Stanga: lista de imagini.
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && lista.getSelectedValue() != null) {
                incarcaImagine(new File(dirRaw, lista.getSelectedValue()));
            }
        });
        add(new JScrollPane(lista), BorderLayout.WEST);

        // Centru: canvas-ul cu imaginea, in scroll pane.
        add(new JScrollPane(canvas), BorderLayout.CENTER);

        // Mouse handler pentru desenare.
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (imaginea == null) return;
                start = e.getPoint();
                curent = new Rectangle(start.x, start.y, 0, 0);
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (start == null) return;
                // Constrangem desenul la un PATRAT (latura egala): folosim min(dx, dy).
                int dx = e.getX() - start.x;
                int dy = e.getY() - start.y;
                int lat = Math.min(Math.abs(dx), Math.abs(dy));
                int x = dx < 0 ? start.x - lat : start.x;
                int y = dy < 0 ? start.y - lat : start.y;
                curent = new Rectangle(x, y, lat, lat);
                canvas.repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (curent != null && curent.width > 8) patrate.add(curent);
                curent = null;
                start = null;
                canvas.repaint();
            }
        };
        canvas.addMouseListener(ma);
        canvas.addMouseMotionListener(ma);
    }

    private void alegeDirector() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        dirRaw = fc.getSelectedFile();
        modelLista.clear();
        File[] fis = dirRaw.listFiles(f -> {
            String n = f.getName().toLowerCase();
            return f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"));
        });
        if (fis == null) return;
        for (File f : fis) modelLista.addElement(f.getName());
    }

    private void incarcaImagine(File f) {
        try {
            imaginea = ImageIO.read(f);
            patrate.clear();
            canvas.revalidate();
            canvas.repaint();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Nu pot citi imaginea: " + ex);
        }
    }

    /**
     * Pentru imaginea curenta, salveaza fiecare patrat desenat ca pozitiv
     * (decupaj grayscale 64x64 in invatare_cap/pozitive) si 3 patrate random
     * care nu se suprapun cu pozitivele ca negative.
     */
    private void salveaza() {
        if (imaginea == null || patrate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nicio adnotare de salvat.");
            return;
        }
        try {
            int[][] gray = ImageUtils.bufferedImageToGray(imaginea);
            File dirPoz = new File("invatare_cap/pozitive");
            File dirNeg = new File("invatare_cap/negative");
            dirPoz.mkdirs();
            dirNeg.mkdirs();

            long stamp = System.currentTimeMillis();

            // Pozitive: fiecare patrat scalat la 64x64.
            for (int i = 0; i < patrate.size(); i++) {
                Rectangle r = patrate.get(i);
                int[][] crop = ImageUtils.decupeaza(gray, r.x, r.y, r.width, r.height);
                int[][] crop64 = ImageUtils.redimensioneaza(crop, 64, 64);
                ImageUtils.salveazaPNG(crop64, new File(dirPoz, "p_" + stamp + "_" + i + ".png"));
            }

            // Negative: pana la 3 patrate random care nu se suprapun cu pozitivele.
            Random rng = new Random();
            int nNeg = 0;
            int incercari = 0;
            while (nNeg < 3 && incercari < 50) {
                incercari++;
                int lat = 64 + rng.nextInt(Math.max(1, Math.min(gray[0].length, gray.length) - 64));
                int x = rng.nextInt(Math.max(1, gray[0].length - lat));
                int y = rng.nextInt(Math.max(1, gray.length - lat));
                Rectangle cand = new Rectangle(x, y, lat, lat);
                boolean ok = true;
                for (Rectangle p : patrate) {
                    if (cand.intersects(p)) { ok = false; break; }
                }
                if (!ok) continue;
                int[][] crop = ImageUtils.decupeaza(gray, x, y, lat, lat);
                int[][] crop64 = ImageUtils.redimensioneaza(crop, 64, 64);
                ImageUtils.salveazaPNG(crop64, new File(dirNeg, "n_" + stamp + "_" + nNeg + ".png"));
                nNeg++;
            }
            JOptionPane.showMessageDialog(this,
                    patrate.size() + " pozitive + " + nNeg + " negative salvate.");
            patrate.clear();
            canvas.repaint();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Eroare la salvare: " + ex);
        }
    }
}
