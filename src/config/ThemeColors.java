package config;

import java.awt.*;

/**
 * Набор предопределённых цветовых констант для оформления интерфейса.
 * <p>
 * Содержит:
 * <ul>
 *     <li>цвета тёмной темы</li>
 *     <li>цвета светлой темы</li>
 *     <li>функциональные цвета для отображения статусов</li>
 *     <li>цвета для отображения результатов (успех/ошибка)</li>
 * </ul>
 * <p>
 * Используется для унификации визуального стиля приложения и
 * централизованного управления цветовыми схемами
 */
public class ThemeColors {
    // Темная тема
    /**
     * Фон темной темы
     */
    public static final Color Dark_BG = new Color(43,43,43);
    /**
     * Панели и контейнеры темной темы
     */
    public static final Color Dark_PANEL = new Color(60, 63, 65);
    /**
     * Текст темной темы
     */
    public static final Color Dark_TEXT = new Color(187, 187, 187);
    /**
     * Фон текстовых полей темной темы
     */
    public static final Color Dark_INPUT_BG = new Color(69, 73, 74);
    /**
     * Рамки и границы темной темы
     */
    public static final Color Dark_BORDER = new Color(85, 85, 85);
    /**
     * Кнопки темной темы
     */
    public static final Color Dark_BUTTON = new Color(50, 52, 54);
    /**
     * Скроллбар темной темы
     */
    public static final Color Dark_SCROLL_TRACK = new Color(43,43,43);
    /**
     * Ползунок скроллбара темной темы
     */
    public static final Color Dark_SCROLL_THUMB = new Color(80,80,80);
    // светлая тема
    /** Фон светлой темы */
    public static final Color LIGHT_BG = new Color(240, 240, 240);
    /** Панели и контейнера в светлой темы */
    public static final Color LIGHT_PANEL = Color.WHITE;
    /** Текст в светлой темы */
    public static final Color LIGHT_TEXT = Color.BLACK;
    /** Текстовые поля в светлой теме */
    public static final Color LIGHT_INPUT_BG = Color.WHITE;
    /** Рамоки и границы в светлой теме */
    public static final Color LIGHT_BORDER = new Color(200, 200, 200);
    /** Кнопки в светлой теме */
    public static final Color LIGHT_BUTTON = new Color(225, 225, 225);
    // Функциональные цвета
    /** Новый элемент / новая задача */
    public static final Color STATUS_NEW = new Color(24, 98, 181);
    /** Ошибка или критическое состояние */
    public static final Color STATUS_ERROR = new Color(166, 3, 3);
    /** Обучение, обработка, промежуточный этап. */
    public static final Color STATUS_LEARNING = new Color(218, 147, 13);
    /** Завершено, мастер, высокий уровень */
    public static final Color STATUS_MASTER = new Color(7, 117, 7);
    // Ответы
    public static final Color SUCCESS_TEXT = new Color(0, 150, 0);
    public static final Color ERROR_TEXT = new Color(200, 50, 50);
}
