package ui;

import config.ThemeColors;

import javax.swing.*;
import java.awt.*;
/**
 * Класс, отвечающий за текущее состояние темы (тёмная/светлая)
 * и предоставляющий методы для получения цветов
 * <p>
 * Также содержит утилиту {@link #apply(Container)}, которая рекурсивно
 * применяет цвета темы
 * <p>
 * Потом надо раздедить:
 * <ul>
 *     <li>ThemeState — хранение текущей темы</li>
 *     <li>ThemeApplicator — рекурсивная покраска компонентов</li>
 *     <li>ThemeColors — палитра цветов</li>
 * </ul>
 */
public class Theme {
    private static boolean isDark = true;

    public static void toggle() {
        isDark = !isDark;
    }

    public static boolean isIsDark() {
        return isDark;
    }

    public static Color getBackground() {
        return isDark ? ThemeColors.Dark_BG : ThemeColors.LIGHT_BG;
    }
    public static Color getPanel(){
        return isDark? ThemeColors.Dark_PANEL : ThemeColors.LIGHT_PANEL;
    }
    public static Color getText(){
        return isDark? ThemeColors.Dark_TEXT : ThemeColors.LIGHT_TEXT;
    }
    public static Color getInputBg(){
        return isDark? ThemeColors.Dark_INPUT_BG : ThemeColors.LIGHT_INPUT_BG;
    }
    public static Color getBorder(){
        return isDark? ThemeColors.Dark_BORDER: ThemeColors.LIGHT_BORDER;
    }
    /**
     * Рекурсивно применяет цвета темы ко всем компонентам контейнера
     * <ul>
     *     <li>JPanel — фон</li>
     *     <li>JFrame — фон contentPane</li>
     *     <li>JTextArea — фон, текст, цвет каретки</li>
     *     <li>JLabel — цвет текста</li>
     *     <li>JCheckBox — фон и текст</li>
     * </ul>
     */
    public static void apply(Container container){
        Color bg = getBackground();
        Color pnl = getPanel();
        Color txt = getText();
        if (container instanceof JPanel) container.setBackground(pnl);
        if (container instanceof JFrame) ((JFrame)container).getContentPane().setBackground(pnl);
        for (Component c: container.getComponents()){
            if (c instanceof JPanel) apply ((Container) c);
            if (c instanceof JTextArea){
                c.setBackground(getInputBg());
                c.setForeground(txt);
                ((JTextArea)c).setCaretColor(txt);
            }
            if (c instanceof JLabel) c.setForeground(txt);
            if (c instanceof JCheckBox){
                c.setBackground(pnl);
                c.setForeground(txt);
            }
        }
    }
}
