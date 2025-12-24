import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;

public class ThemeManager {
    private static boolean isDark = true; // По умолчанию темная, раз вы просили

    // Цвета (Dark / Light)
    private static final Color BG_DARK = new Color(43, 43, 43);
    private static final Color BG_LIGHT = new Color(240, 240, 240);

    private static final Color PANEL_DARK = new Color(60, 63, 65);
    private static final Color PANEL_LIGHT = Color.WHITE;

    private static final Color TEXT_DARK = new Color(187, 187, 187);
    private static final Color TEXT_LIGHT = Color.BLACK;

    private static final Color INPUT_BG_DARK = new Color(69, 73, 74);
    private static final Color INPUT_BG_LIGHT = Color.WHITE;

    // Цвета для выделения успеха/ошибки
    private static final Color SUCCESS_DARK = new Color(70, 150, 70);
    private static final Color ERROR_DARK = new Color(150, 70, 70);

    public static void toggle() { isDark = !isDark; }
    public static boolean isDark() { return isDark; }

    public static Color getBackground() { return isDark ? BG_DARK : BG_LIGHT; }
    public static Color getPanelBackground() { return isDark ? PANEL_DARK : PANEL_LIGHT; }
    public static Color getText() { return isDark ? TEXT_DARK : TEXT_LIGHT; }
    public static Color getInputBackground() { return isDark ? INPUT_BG_DARK : INPUT_BG_LIGHT; }
    public static Color getSuccessColor() { return isDark ? new Color(120, 220, 120) : new Color(0, 120, 0); }
    public static Color getErrorColor() { return isDark ? new Color(255, 100, 100) : Color.RED; }

    /**
     * Рекурсивно красит компонент и всех его детей.
     * Решает проблему "белых полос" между элементами.
     */
    public static void applyRecursive(Component c) {
        // Красим панели и корневые элементы
        if (c instanceof JPanel || c instanceof JLayeredPane || c instanceof JRootPane) {
            c.setBackground(getPanelBackground());
        }
        // Красим вкладки
        if (c instanceof JTabbedPane) {
            c.setBackground(getBackground());
            c.setForeground(getText());
        }
        // Красим скроллы
        if (c instanceof JViewport) {
            c.setBackground(getPanelBackground());
        }
        // Красим таблицы
        if (c instanceof JTable) {
            c.setBackground(getInputBackground());
            c.setForeground(getText());
            ((JTable) c).getTableHeader().setBackground(getBackground());
            ((JTable) c).getTableHeader().setForeground(getText());
        }
        // Красим текстовые поля (кроме тех, что мы красим отдельно логикой валидации)
        if (c instanceof JTextArea || c instanceof JTextField) {
            // Оставляем это на откуп MainFrame, чтобы не сбить цвет валидации
            c.setForeground(getText());
            c.setCaretColor(getText());
        }
        // Красим лейблы и чекбоксы
        if (c instanceof JLabel || c instanceof JCheckBox || c instanceof JRadioButton) {
            c.setForeground(getText());
        }
        // Кнопки
        if (c instanceof JButton) {
            c.setBackground(isDark ? new Color(70, 75, 80) : new Color(220, 220, 220));
            c.setForeground(getText());
        }

        // Рекурсия внутрь
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                applyRecursive(child);
            }
        }
    }

    // Настройка UIManager для глобальных элементов (заголовки табов, скроллбары)
    public static void updateUIManager() {
        Color bg = getBackground();
        Color pnl = getPanelBackground();
        Color txt = getText();

        UIManager.put("Panel.background", pnl);
        UIManager.put("Label.foreground", txt);
        UIManager.put("TabbedPane.background", bg);
        UIManager.put("TabbedPane.foreground", txt);
        UIManager.put("TabbedPane.contentAreaColor", pnl);
        UIManager.put("TabbedPane.selected", pnl);
        UIManager.put("TableHeader.background", bg);
        UIManager.put("TableHeader.foreground", txt);
        UIManager.put("Button.background", isDark ? new Color(70, 75, 80) : new Color(230, 230, 230));
        UIManager.put("Button.foreground", txt);
    }
}