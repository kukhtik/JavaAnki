package ui.components;

import config.ThemeColors;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class DarkScrollBarUI extends BasicScrollBarUI {
    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = ThemeColors.DARK_SCROLL_THUMB;
        this.trackColor = ThemeColors.DARK_SCROLL_TRACK;
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    private JButton createZeroButton() {
        JButton j = new JButton();
        j.setPreferredSize(new Dimension(0, 0));
        j.setMinimumSize(new Dimension(0, 0));
        j.setMaximumSize(new Dimension(0, 0));
        return j;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(thumbColor);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);
        g2.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(trackColor);
        g.fillRect(r.x, r.y, r.width, r.height);
    }
}