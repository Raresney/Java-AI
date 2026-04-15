package proiect.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Panel pentru cerinta 4: vizualizarea imaginilor capturate si stergerea
 * celor "necorespunzatoare" (neclare, incadrate rau).
 *
 * Interfata:
 *  - un JComboBox pentru selectarea persoanei (subdirector din invatare_fete/)
 *  - o grila de miniaturi
 *  - click dreapta pe o miniatura = sterge fisierul de pe disc
 *  - buton "Reincarca" pentru refresh
 */
public class BrowserPanel extends JPanel {

    private final JComboBox<String> cmbPersoane = new JComboBox<>();
    private final JPanel grila = new JPanel(new GridLayout(0, 6, 4, 4));

    public BrowserPanel() {
        setLayout(new BorderLayout());

        JPanel sus = new JPanel();
        JButton bReload = new JButton("Reincarca");
        bReload.addActionListener(e -> { refreshListaPersoane(); afiseazaPersoana(); });
        sus.add(new JLabel("Persoana:"));
        sus.add(cmbPersoane);
        sus.add(bReload);
        add(sus, BorderLayout.NORTH);

        cmbPersoane.addActionListener(e -> afiseazaPersoana());
        add(new JScrollPane(grila), BorderLayout.CENTER);

        refreshListaPersoane();
    }

    /** Actualizeaza combo-ul cu subdirectoarele din invatare_fete/. */
    private void refreshListaPersoane() {
        cmbPersoane.removeAllItems();
        File dir = new File("invatare_fete");
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return;
        for (File s : subs) cmbPersoane.addItem(s.getName());
    }

    /** Afiseaza toate imaginile persoanei selectate ca miniaturi. */
    private void afiseazaPersoana() {
        grila.removeAll();
        String p = (String) cmbPersoane.getSelectedItem();
        if (p == null) { grila.revalidate(); grila.repaint(); return; }

        File dir = new File("invatare_fete/" + p);
        File[] imgs = dir.listFiles(f -> f.isFile() && f.getName().toLowerCase().endsWith(".png"));
        if (imgs == null) return;

        for (File img : imgs) {
            grila.add(construiesteMiniatura(img));
        }
        grila.revalidate();
        grila.repaint();
    }

    /** Creeaza o miniatura cu meniu contextual "Sterge". */
    private JLabel construiesteMiniatura(File img) {
        JLabel lbl = new JLabel();
        try {
            BufferedImage bi = ImageIO.read(img);
            // Miniatura 96x96.
            Image scaled = bi.getScaledInstance(96, 96, Image.SCALE_SMOOTH);
            lbl.setIcon(new ImageIcon(scaled));
        } catch (Exception ex) {
            lbl.setText("err");
        }
        lbl.setToolTipText(img.getName());
        lbl.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Confirmare inainte de stergere.
                    int r = JOptionPane.showConfirmDialog(BrowserPanel.this,
                            "Sterg " + img.getName() + "?", "Confirma",
                            JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.YES_OPTION) {
                        if (img.delete()) {
                            Container parent = lbl.getParent();
                            parent.remove(lbl);
                            parent.revalidate();
                            parent.repaint();
                        }
                    }
                }
            }
        });
        return lbl;
    }
}
