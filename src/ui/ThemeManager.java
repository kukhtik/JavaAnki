package ui;

import config.ThemeColors;
import lombok.Getter;
import ui.components.DarkScrollBarUI;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Менеджер тем оформления приложения
 * <p>
 * Отвечает за:
 * <ol>
 *     <li>Хранение текущего состояния (Светлая/Темная тема)</li>
 *     <li>Предоставление цветовых констант в зависимости от состояния</li>
 *     <li><b>Применение стилей:</b> Рекурсивный обход дерева компонентов Swing и принудительная
 *     установка цветов фона, текста, границ и скроллбаров</li>
 * </ol>
 * </p>
 */
public class ThemeManager {
    /**
     * Флаг текущего режима (по умолчанию - темная тема)
     */
    @Getter
    private static boolean isDark = true;

    /** Переключить тему (Dark / Light) */
    public static void toggle() { isDark = !isDark; }

    public static Color getPanel() { return isDark ? ThemeColors.DARK_PANEL : ThemeColors.LIGHT_PANEL; }
    public static Color getText() { return isDark ? ThemeColors.DARK_TEXT : ThemeColors.LIGHT_TEXT; }
    public static Color getInputBg() { return isDark ? ThemeColors.DARK_INPUT_BG : ThemeColors.LIGHT_INPUT_BG; }
    public static Color getBorder() { return isDark ? ThemeColors.DARK_BORDER : ThemeColors.LIGHT_BORDER; }
    public static Color getButton() { return isDark ? ThemeColors.DARK_BUTTON : ThemeColors.LIGHT_BUTTON; }
    public static Color getButtonHover() { return isDark ? ThemeColors.DARK_BUTTON_HOVER : ThemeColors.LIGHT_BUTTON_HOVER; }

    /**
     * Настройка глобальных параметров UIManager, вызывается при старте и смене темы
     */
    public static void setupGlobal() {
        Color pnl = getPanel();
        Color txt = getText();
        Color btn = getButton();
        Color input = getInputBg();

        // цвета выпадающих списков
        UIManager.put("ComboBox.background", btn);
        UIManager.put("ComboBox.foreground", txt);
        UIManager.put("ComboBox.selectionBackground", ThemeColors.STATUS_NEW);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);

        // цвета заголовков таблиц
        UIManager.put("TableHeader.background", pnl);
        UIManager.put("TableHeader.foreground", txt);

        // цвета списков и меню
        UIManager.put("List.background", input);
        UIManager.put("List.foreground", txt);
        UIManager.put("MenuItem.background", pnl);
        UIManager.put("MenuItem.foreground", txt);
    }

    /**
     * "Магический" метод применения темы к контейнеру и всем его детям.
     * <p>
     * Работает рекурсивно. Определяет тип компонента и устанавливает ему соответствующие цвета.
     * Также заменяет стандартные UI-делегаты (ScrollUI, ComboBoxUI) на кастомные
     * </p>
     *
     * @param container контейнер, который нужно перекрасить.
     */
    public static void apply(Container container) {
        setupGlobal();

        Color pnl = getPanel();
        Color txt = getText();
        Color inputBg = getInputBg();
        Color border = getBorder();
        Color btnBg = getButton();

        // окраска окна
        if (container instanceof JFrame) {
            ((JFrame) container).getContentPane().setBackground(pnl);
            // обновление L&F дерева
            SwingUtilities.updateComponentTreeUI(container);
        }

        // окраска панелей
        if (container instanceof JPanel) {
            container.setBackground(pnl);
        }

        // окраска скролл-панелей и замена скроллбаров
        if (container instanceof JScrollPane scroll) {
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

        // окраска таблиц
        if (container instanceof JTable t) {
            t.setBackground(inputBg);
            t.setForeground(txt);
            t.setGridColor(border);

            // кастомный рендерер заголовка таблицы
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

        // окраска выпадающих списков (Самая сложная часть)
        if (container instanceof JComboBox<?> box) {
            box.setOpaque(true);
            box.setBackground(btnBg);
            box.setForeground(txt);

            // если темная тема, нужно полностью заменить отрисовку попап,
            // чтобы скроллбар внутри списка тоже стал поменялся
            if (isDark) {
                box.setUI(new BasicComboBoxUI() {
                    @Override
                    protected JButton createArrowButton() {
                        JButton b = super.createArrowButton();
                        b.setBackground(btnBg);
                        b.setBorder(new LineBorder(border));
                        return b;
                    }

                    @Override
                    protected ComboPopup createPopup() {
                        return new BasicComboPopup(comboBox) {
                            @Override
                            protected JScrollPane createScroller() {
                                JScrollPane scroller = super.createScroller();
                                // темные скроллбары внутрь выпадающего списка
                                if (scroller.getVerticalScrollBar() != null) {
                                    scroller.getVerticalScrollBar().setUI(new DarkScrollBarUI());
                                }
                                if (scroller.getHorizontalScrollBar() != null) {
                                    scroller.getHorizontalScrollBar().setUI(new DarkScrollBarUI());
                                }
                                scroller.setBorder(new LineBorder(ThemeManager.getBorder()));
                                return scroller;
                            }
                        };
                    }
                });
            }
        }

        // окраска вкладок
        if (container instanceof JTabbedPane tabs) {
            tabs.setForeground(txt);
            tabs.setBackground(pnl);
            if (isDark) {
                // убираем стандартные объемные эффекты
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
            } else {
                tabs.updateUI(); // сброс на стандартный вид
            }
        }
        // проходим по всем вложенным компонентам
        for (Component c : container.getComponents()) {
            if (c instanceof Container) apply((Container) c);

            // окраска полей ввода
            if (c instanceof JTextComponent) {
                c.setBackground(inputBg);
                c.setForeground(txt);
                c.setFont(new Font("Arial", Font.PLAIN, 16));
                ((JTextComponent) c).setCaretColor(txt); // Цвет курсора ввода
            }

            // окраска простого текста
            if (c instanceof JLabel) c.setForeground(txt);

            // окраска кнопок (кроме кнопок в заголовке окна, у них свой стиль)
            if (c instanceof JButton && !(c.getParent() instanceof ui.components.CustomTitleBar)) {
                c.setBackground(btnBg);
                c.setForeground(txt);
                ((JButton) c).setFocusPainted(false);
            }
        }
    }
}