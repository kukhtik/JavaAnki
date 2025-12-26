package ui;

import config.ThemeColors;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class Theme {
    private static boolean isDark = true;

    public static void toggle() { isDark = !isDark; }
    public static boolean isDark() { return isDark; }

    // Ссылаемся на переменные из ThemeColors.java
    public static Color getBackground() { return isDark ? ThemeColors.DARK_BG : ThemeColors.LIGHT_BG; }
    public static Color getPanel() { return isDark ? ThemeColors.DARK_PANEL : ThemeColors.LIGHT_PANEL; }
    public static Color getText() { return isDark ? ThemeColors.DARK_TEXT : ThemeColors.LIGHT_TEXT; }
    public static Color getInputBg() { return isDark ? ThemeColors.DARK_INPUT_BG : ThemeColors.LIGHT_INPUT_BG; }
    public static Color getBorder() { return isDark ? ThemeColors.DARK_BORDER : ThemeColors.LIGHT_BORDER; }

    public static void apply(Container container) {
        Color bg = getBackground();
        Color pnl = getPanel();
        Color txt = getText();
        Color inputBg = getInputBg();
        Color border = getBorder();

        // 1. Основные контейнеры
        if (container instanceof JPanel || container instanceof JRootPane) {
            container.setBackground(pnl);
        }
        if (container instanceof JFrame) {
            ((JFrame) container).getContentPane().setBackground(pnl);
        }

        // 2. Скроллы
        if (container instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) container;
            scroll.setBackground(pnl);
            scroll.getViewport().setBackground(inputBg);
            scroll.setBorder(BorderFactory.createLineBorder(border));
        }

        // 3. Таблицы
        if (container instanceof JTable) {
            JTable t = (JTable) container;
            t.setBackground(inputBg);
            t.setForeground(txt);
            t.setGridColor(border);
            JTableHeader header = t.getTableHeader();
            if (header != null) {
                header.setBackground(pnl);
                header.setForeground(txt);
            }
        }

        // 4. Вкладки
        if (container instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) container;
            tabs.setForeground(txt);
            tabs.setBackground(pnl);

            if (isDark) {
                tabs.setUI(new BasicTabbedPaneUI() {
                    @Override protected void installDefaults() {
                        super.installDefaults();
                        shadow = ThemeColors.DARK_BORDER;
                        darkShadow = ThemeColors.DARK_BG;
                        lightHighlight = ThemeColors.DARK_PANEL;
                        highlight = ThemeColors.DARK_PANEL;
                    }
                });
            } else {
                tabs.updateUI();
            }
        }

        // Рекурсия и компоненты
        for (Component c : container.getComponents()) {
            if (c instanceof Container) apply((Container) c);

            if (c instanceof JTextComponent) {
                c.setBackground(inputBg);
                c.setForeground(txt);
                c.setFont(new Font("Arial", Font.PLAIN, 16));
                ((JTextComponent) c).setCaretColor(txt);
            }

            if (c instanceof JLabel) {
                c.setForeground(txt);
            }

            if (c instanceof JButton && c.getParent() instanceof ui.components.CustomTitleBar) {
                c.setForeground(txt);
            }
        }
    }
}