package ui;

import config.ThemeColors;
import ui.components.DarkScrollBarUI;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ThemeManager {
    private static boolean isDark = true;

    public static void toggle() { isDark = !isDark; }
    public static boolean isDark() { return isDark; }

    public static Color getBackground() { return isDark ? ThemeColors.DARK_BG : ThemeColors.LIGHT_BG; }
    public static Color getPanel() { return isDark ? ThemeColors.DARK_PANEL : ThemeColors.LIGHT_PANEL; }
    public static Color getText() { return isDark ? ThemeColors.DARK_TEXT : ThemeColors.LIGHT_TEXT; }
    public static Color getInputBg() { return isDark ? ThemeColors.DARK_INPUT_BG : ThemeColors.LIGHT_INPUT_BG; }
    public static Color getBorder() { return isDark ? ThemeColors.DARK_BORDER : ThemeColors.LIGHT_BORDER; }
    public static Color getButton() { return isDark ? ThemeColors.DARK_BUTTON : ThemeColors.LIGHT_BUTTON; }
    public static Color getButtonHover() { return isDark ? ThemeColors.DARK_BUTTON_HOVER : ThemeColors.LIGHT_BUTTON_HOVER; }

    public static void setupGlobal() {
        Color pnl = getPanel();
        Color txt = getText();
        Color btn = getButton();
        Color input = getInputBg();

        UIManager.put("ComboBox.background", btn);
        UIManager.put("ComboBox.foreground", txt);
        UIManager.put("ComboBox.selectionBackground", ThemeColors.STATUS_NEW);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        UIManager.put("TableHeader.background", pnl);
        UIManager.put("TableHeader.foreground", txt);

        UIManager.put("List.background", input);
        UIManager.put("List.foreground", txt);
        UIManager.put("MenuItem.background", pnl);
        UIManager.put("MenuItem.foreground", txt);
    }

    public static void apply(Container container) {
        setupGlobal();

        Color pnl = getPanel();
        Color txt = getText();
        Color inputBg = getInputBg();
        Color border = getBorder();
        Color btnBg = getButton();

        if (container instanceof JFrame) {
            ((JFrame) container).getContentPane().setBackground(pnl);
            SwingUtilities.updateComponentTreeUI(container);
        }
        if (container instanceof JPanel) container.setBackground(pnl);

        if (container instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) container;
            scroll.setBackground(pnl);
            scroll.getViewport().setBackground(inputBg);
            scroll.setBorder(BorderFactory.createLineBorder(border));
            if (isDark) {
                scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
                scroll.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
            } else {
                scroll.getVerticalScrollBar().setUI(null);
            }
        }

        if (container instanceof JTable) {
            JTable t = (JTable) container;
            t.setBackground(inputBg);
            t.setForeground(txt);
            t.setGridColor(border);

            JTableHeader header = t.getTableHeader();
            if (header != null) {
                header.setBackground(pnl);
                header.setForeground(txt);
                header.setDefaultRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        c.setBackground(ThemeManager.getPanel());
                        c.setForeground(ThemeManager.getText());
                        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, ThemeManager.getBorder()));
                        return c;
                    }
                });
                header.repaint();
            }
        }

        if (container instanceof JComboBox) {
            JComboBox<?> box = (JComboBox<?>) container;
            box.setOpaque(true);
            box.setBackground(btnBg);
            box.setForeground(txt);
            if (isDark) {
                box.setUI(new BasicComboBoxUI() {
                    @Override protected JButton createArrowButton() {
                        JButton b = super.createArrowButton();
                        b.setBackground(btnBg);
                        b.setBorder(new LineBorder(border));
                        return b;
                    }
                });
            }
        }

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
                        contentBorderInsets = new Insets(0,0,0,0);
                    }
                });
            } else { tabs.updateUI(); }
        }

        for (Component c : container.getComponents()) {
            if (c instanceof Container) apply((Container) c);

            if (c instanceof JTextComponent) {
                c.setBackground(inputBg);
                c.setForeground(txt);
                c.setFont(new Font("Arial", Font.PLAIN, 16));
                ((JTextComponent) c).setCaretColor(txt);
            }
            if (c instanceof JLabel) c.setForeground(txt);

            if (c instanceof JButton && !(c.getParent() instanceof ui.components.CustomTitleBar)) {
                c.setBackground(btnBg);
                c.setForeground(txt);
                ((JButton) c).setFocusPainted(false);
            }
        }
    }
}