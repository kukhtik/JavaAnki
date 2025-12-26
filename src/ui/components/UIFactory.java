package ui.components;

import ui.Theme;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UIFactory {
    public static final Font FONT_MAIN = new Font("Arial", Font.PLAIN, 16);
    public static final Font FONT_BOLD = new Font("Arial", Font.BOLD, 14);

    public static JButton createButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);

        // Устанавливаем начальный цвет
        btn.setBackground(Theme.getButton());
        btn.setForeground(Theme.getText());

        btn.addActionListener(action);
        btn.setPreferredSize(new Dimension(200, 45));

        btn.setBorder(new CompoundBorder(
                new LineBorder(Theme.getBorder(), 1),
                new EmptyBorder(5, 15, 5, 15)
        ));

        // ЭФФЕКТ ПОДСВЕТКИ (HOVER)
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // При наведении делаем чуть светлее
                btn.setBackground(Theme.getButtonHover());
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Возвращаем базовый цвет
                btn.setBackground(Theme.getButton());
                btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return btn;
    }

    public static JTextArea createArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setFont(FONT_MAIN);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(editable);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return area;
    }

    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        return lbl;
    }
}