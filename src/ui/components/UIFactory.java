package ui.components;

import ui.ThemeManager;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Фабрика UI-компонентов
 * <p>
 * Централизованное место для создания стандартных элементов интерфейса (кнопок, полей ввода, меток).
 * Дает:
 * <ul>
 *     <li>Единый визуальный стиль во всем приложении</li>
 *     <li>Отсутствие дублирования кода настройки</li>
 *     <li>Применение цветов из {@link ThemeManager} при создании</li>
 * </ul>
 * </p>
 */
public class UIFactory {

    /** Основной шрифт для текстовых полей */
    public static final Font FONT_MAIN = new Font("Arial", Font.PLAIN, 16);

    /** Жирный шрифт для кнопок и заголовков */
    public static final Font FONT_BOLD = new Font("Arial", Font.BOLD, 14);

    /**
     * Создает стилизованную кнопку с эффектом наведения (Hover)
     *
     * @param text текст на кнопке
     * @param action действие при нажатии (ActionListener/Lambda)
     * @return готовая к добавлению на панель кнопка
     */
    public static JButton createButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);

        // убираем стандартные эффекты фокуса и заливки Swing
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true); // ручная отрисовка фона

        // устанавливаем начальные цвета из текущей темы
        btn.setBackground(ThemeManager.getButton());
        btn.setForeground(ThemeManager.getText());

        btn.addActionListener(action);
        btn.setPreferredSize(new Dimension(200, 45)); // фиксированный размер для единообразия

        // сложная граница: тонкая линия снаружи + отступ внутри
        btn.setBorder(new CompoundBorder(
                new LineBorder(ThemeManager.getBorder(), 1),
                new EmptyBorder(5, 15, 5, 15)
        ));

        // HOVER EFFECT
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // мышь! - меняем цвет на более светлый и курсор на руку
                btn.setBackground(ThemeManager.getButtonHover());
                btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // мышь нет - возвращаем базовый цвет
                btn.setBackground(ThemeManager.getButton());
                btn.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        return btn;
    }

    /**
     * Создает многострочное текстовое поле (TextArea).
     * <p>
     * Настроено на перенос слов и имеет внутренние отступы
     * </p>
     *
     * @param editable {@code true} - для полей ввода, {@code false} - для отображения текста (readonly)
     * @return настроенный JTextArea
     */
    public static JTextArea createArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setFont(FONT_MAIN);

        // перенос слов, чтобы текст не уезжал за горизонт
        area.setLineWrap(true);
        area.setWrapStyleWord(true);

        area.setEditable(editable);

        // внутренний отступ, чтобы текст не прилипал к границам
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        return area;
    }

    /**
     * Создает простую метку с жирным шрифтом
     */
    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        return lbl;
    }

    /**
     * Приватный конструктор.
     * Предотвращает создание экземпляров утилитного класса
     */
    private UIFactory() {
        throw new UnsupportedOperationException("Utility class");
    }
}