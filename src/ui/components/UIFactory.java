package ui.components;

import ui.Theme;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;

public class UIFactory {

    public static final Font FONT_MAIN = new Font("Arial", Font.PLAIN, 16);
    public static final Font FONT_BOLD = new Font("Arial", Font.BOLD, 14);

    public static JButton createButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        btn.setPreferredSize(new Dimension(200, 45));

        // Базовый стиль (цвета обновятся через Theme.apply, но структуру задаем тут)
        btn.setBorder(new CompoundBorder(
                new LineBorder(Theme.getBorder(), 1),
                new EmptyBorder(5, 15, 5, 15)
        ));
        return btn;
    }

    public static JTextArea createArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setFont(FONT_MAIN);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(editable);

        // Добавляем отступы внутри текста
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return area;
    }

    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        return lbl;
    }
}