package ui.components;

import config.ThemeColors;
import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * Кастомный стиль для полосы прокрутки (не знаю почему мне от этого немного смешно)
 * <p>
 * Заменяет стандартный вид скроллбара (который яркий!) на плоский и черный
 * <ul>
 *     <li>Скрыты кнопки-стрелки (вверх/вниз)</li>
 *     <li>Ползунок (Thumb) имеет скругленные углы</li>
 *     <li>Цвета берутся из палитры {@link ThemeColors}</li>
 * </ul>
 * </p>
 */
public class DarkScrollBarUI extends BasicScrollBarUI {

    /**
     * Настройка базовых цветов UI.
     * Вызывается автоматически при установке UI компоненту
     */
    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = ThemeColors.DARK_SCROLL_THUMB;
        this.trackColor = ThemeColors.DARK_SCROLL_TRACK;
    }

    /**
     * Переопределение создания кнопки "Вверх/Влево".
     * Возвращаем кнопку нулевого размера, чтобы фактически скрыть её
     */
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }

    /**
     * Переопределение создания кнопки "Вниз/Вправо".
     * Возвращаем кнопку нулевого размера, чтобы фактически скрыть её
     */
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }

    /**
     * Вспомогательный метод для создания невидимой кнопки
     */
    private JButton createZeroButton() {
        JButton j = new JButton();
        // все размеры в 0, чтобы кнопка не занимала место на экране
        j.setPreferredSize(new Dimension(0, 0));
        j.setMinimumSize(new Dimension(0, 0));
        j.setMaximumSize(new Dimension(0, 0));
        return j;
    }

    /**
     * Отрисовка ползунка, скругленный
     */
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        // сглаживание, чтобы скругленные края были плавными
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(thumbColor);
        // прямоугольник со скруглением радиусом 6px
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 6, 6);

        g2.dispose();
    }

    /**
     * Отрисовка фона скроллбара.
     * Просто заливаем прямоугольник цветом фона
     */
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        g.setColor(trackColor);
        g.fillRect(r.x, r.y, r.width, r.height);
    }
}