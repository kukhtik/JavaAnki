import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class ThemeManager {
    private static boolean isDark = true;

    // --- ЦВЕТА ---
    public static final Color BG_DARK = new Color(43, 43, 43);
    public static final Color PANEL_DARK = new Color(60, 63, 65);
    public static final Color TEXT_DARK = new Color(187, 187, 187);
    public static final Color INPUT_BG_DARK = new Color(69, 73, 74);
    public static final Color BORDER_DARK = new Color(85, 85, 85);
    public static final Color BUTTON_DARK = new Color(50, 52, 54); // Темно-серые кнопки

    public static final Color SCROLL_TRACK_DARK = new Color(43, 43, 43);
    public static final Color SCROLL_THUMB_DARK = new Color(80, 80, 80);

    // Светлая тема
    public static final Color BG_LIGHT = new Color(240, 240, 240);
    public static final Color PANEL_LIGHT = Color.WHITE;
    public static final Color TEXT_LIGHT = Color.BLACK;
    public static final Color INPUT_BG_LIGHT = Color.WHITE;
    public static final Color BORDER_LIGHT = new Color(200, 200, 200);
    public static final Color BUTTON_LIGHT = new Color(225, 225, 225);

    // --- ЛОГИКА ---
    public static void toggle() { isDark = !isDark; }
    public static boolean isDark() { return isDark; }

    public static Color getBackground() { return isDark ? BG_DARK : BG_LIGHT; }
    public static Color getPanelBackground() { return isDark ? PANEL_DARK : PANEL_LIGHT; }
    public static Color getText() { return isDark ? TEXT_DARK : TEXT_LIGHT; }
    public static Color getInputBackground() { return isDark ? INPUT_BG_DARK : INPUT_BG_LIGHT; }
    public static Color getBorderColor() { return isDark ? BORDER_DARK : BORDER_LIGHT; }
    public static Color getSuccessColor() { return isDark ? new Color(100, 180, 100) : new Color(0, 120, 0); }
    public static Color getErrorColor() { return isDark ? new Color(200, 80, 80) : Color.RED; }

    /**
     * Глобальная рекурсивная покраска
     */
    public static void applyRecursive(Component c) {
        Color bg = getBackground();
        Color pnl = getPanelBackground();
        Color txt = getText();
        Color border = getBorderColor();
        Color btnBg = isDark ? BUTTON_DARK : BUTTON_LIGHT;

        // 1. Панели и Окна
        if (c instanceof JPanel || c instanceof JLayeredPane || c instanceof JRootPane) {
            c.setBackground(pnl);
        }
        if (c instanceof JFrame) {
            ((JFrame) c).getContentPane().setBackground(pnl);
        }

        // 2. Вкладки (JTabbedPane) - ПРИМЕНЯЕМ КАСТОМНЫЙ UI
        if (c instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) c;
            tabs.setBackground(bg);
            tabs.setForeground(txt);
            if (isDark) {
                tabs.setUI(new DarkTabbedPaneUI()); // Наш класс для отрисовки
            } else {
                tabs.setUI(new BasicTabbedPaneUI());
            }
        }

        // 3. Скроллы
        if (c instanceof JScrollPane) {
            JScrollPane scroll = (JScrollPane) c;
            scroll.setBackground(pnl);
            scroll.getViewport().setBackground(pnl);
            scroll.setBorder(new LineBorder(border, 1));
            if (isDark) {
                scroll.getVerticalScrollBar().setUI(new DarkScrollBarUI());
                scroll.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
                JPanel corner = new JPanel(); corner.setBackground(pnl);
                scroll.setCorner(JScrollPane.LOWER_RIGHT_CORNER, corner);
            } else {
                scroll.getVerticalScrollBar().setUI(null);
                scroll.getHorizontalScrollBar().setUI(null);
            }
        }

        // 4. Текстовые поля
        if (c instanceof JTextComponent) {
            c.setBackground(isDark ? INPUT_BG_DARK : INPUT_BG_LIGHT);
            c.setForeground(txt);
            ((JTextComponent) c).setCaretColor(txt);
            if (c instanceof JTextField || c instanceof JTextArea) {
                ((JComponent)c).setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(border, 1),
                        BorderFactory.createEmptyBorder(4, 6, 4, 6)));
            }
        }

        // 5. Кнопки (JButton)
        if (c instanceof JButton) {
            JButton btn = (JButton) c;
            // Исключаем кнопки стрелок внутри ComboBox и скроллов, если они там есть
            if (btn.getParent() instanceof JComboBox) {
                // Стрелку комбобокса не трогаем тут, ее красит UI комбобокса
            } else {
                btn.setUI(new BasicButtonUI()); // Сброс стиля Windows
                btn.setBackground(btnBg);
                btn.setForeground(txt);
                btn.setFocusPainted(false);
                btn.setBorder(new CompoundBorder(
                        new LineBorder(border, 1),
                        new EmptyBorder(6, 15, 6, 15)
                ));
            }
        }

        // 6. Выпадающие списки (JComboBox)
        if (c instanceof JComboBox) {
            JComboBox<?> box = (JComboBox<?>) c;
            box.setBackground(btnBg);
            box.setForeground(txt);

            // Красим выпадающую часть
            box.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (isSelected) {
                        setBackground(new Color(75, 110, 175));
                        setForeground(Color.WHITE);
                    } else {
                        setBackground(btnBg);
                        setForeground(txt);
                    }
                    setBorder(new EmptyBorder(3, 5, 3, 5));
                    return this;
                }
            });

            // Устанавливаем UI для самой кнопки списка
            if (isDark) {
                box.setUI(new BasicComboBoxUI() {
                    @Override
                    protected JButton createArrowButton() {
                        JButton btn = super.createArrowButton();
                        btn.setBackground(BUTTON_DARK);
                        btn.setBorder(new LineBorder(BORDER_DARK));
                        return btn;
                    }
                });
            } else {
                box.setUI(new BasicComboBoxUI());
            }
        }

        // 7. Таблицы
        if (c instanceof JTable) {
            JTable t = (JTable) c;
            t.setBackground(getInputBackground());
            t.setForeground(txt);
            t.setGridColor(border);
            t.setSelectionBackground(new Color(75, 110, 175));
            t.setSelectionForeground(Color.WHITE);

            JTableHeader header = t.getTableHeader();
            if (header != null) {
                header.setDefaultRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        comp.setBackground(pnl);
                        comp.setForeground(txt);
                        ((JComponent)comp).setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(border), new EmptyBorder(4, 4, 4, 4)));
                        setFont(new Font("Arial", Font.BOLD, 12));
                        return comp;
                    }
                });
                header.repaint();
            }
        }

        if (c instanceof JLabel) c.setForeground(txt);

        // Диалоговые окна (JOptionPane)
        if (c instanceof JComponent && c.getClass().getName().contains("OptionPane")) {
            c.setBackground(pnl);
        }

        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                applyRecursive(child);
            }
        }
    }

    public static void updateUIManager() {
        Color pnl = getPanelBackground();
        Color txt = getText();
        Color btnBg = isDark ? BUTTON_DARK : BUTTON_LIGHT;

        // Общие
        UIManager.put("Panel.background", pnl);
        UIManager.put("Label.foreground", txt);

        // Всплывающие окна (JOptionPane)
        UIManager.put("OptionPane.background", pnl);
        UIManager.put("OptionPane.messageForeground", txt);
        UIManager.put("OptionPane.foreground", txt);

        // Кнопки в диалогах
        UIManager.put("Button.background", btnBg);
        UIManager.put("Button.foreground", txt);
        UIManager.put("Button.select", btnBg.darker());

        // Комбобоксы
        UIManager.put("ComboBox.background", btnBg);
        UIManager.put("ComboBox.foreground", txt);
        UIManager.put("ComboBox.selectionBackground", new Color(75, 110, 175));
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        // Вкладки
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(0,0,0,0));
    }

    // --- КЛАСС ДЛЯ ТЕМНЫХ ВКЛАДОК ---
    static class DarkTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets.right = 0; // Убираем отступы
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            // Не рисуем стандартную белую рамку вокруг контента
            g.setColor(BORDER_DARK);
            g.drawRect(0, 0, tabPane.getWidth()-1, tabPane.getHeight()-1);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g;
            if (isSelected) {
                g2.setColor(new Color(60, 63, 65)); // Цвет активной вкладки (как панель)
            } else {
                g2.setColor(new Color(35, 35, 35)); // Цвет неактивной вкладки (темнее)
            }
            g2.fillRect(x, y, w, h);
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
            g.setColor(BORDER_DARK);
            g.drawRect(x, y, w, h);
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            g.setColor(TEXT_DARK);
            super.paintText(g, tabPlacement, font, metrics, tabIndex, title, textRect, isSelected);
        }
    }

    // --- КЛАСС ДЛЯ СКРОЛЛБАРА ---
    static class DarkScrollBarUI extends BasicScrollBarUI {
        @Override protected void configureScrollBarColors() { thumbColor = SCROLL_THUMB_DARK; trackColor = SCROLL_TRACK_DARK; }
        @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }
        private JButton createZeroButton() { JButton j = new JButton(); j.setPreferredSize(new Dimension(0,0)); return j; }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            g.setColor(SCROLL_THUMB_DARK); g.fillRoundRect(r.x+1, r.y+1, r.width-2, r.height-2, 6, 6);
        }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(SCROLL_TRACK_DARK); g.fillRect(r.x, r.y, r.width, r.height);
        }
    }
}