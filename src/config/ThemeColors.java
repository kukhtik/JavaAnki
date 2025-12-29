package config;

import java.awt.Color;

/**
 * Палитра цветов приложения
 * <p>
 * Содержит статические константы для стилизации интерфейса.
 * Разделена на три секции:
 * <ul>
 *     <li><b>Тёмная тема</b> (Dark Mode) - для ночного режима или контрастного восприятия</li>
 *     <li><b>Светлая тема</b> (Light Mode) - стандартный вид</li>
 *     <li><b>Семантические цвета</b> - для отображения статусов обучения (цветовое кодирование)</li>
 * </ul>
 * Используется классом {@code ThemeManager} и кастомными UI-компонентами
 * </p>
 */
public class ThemeColors {
    /** Основной цвет фона окна */
    public static final Color DARK_BG = new Color(43, 43, 43);

    /** Цвет фона вторичных панелей */
    public static final Color DARK_PANEL = new Color(60, 63, 65);

    /** Основной цвет текста */
    public static final Color DARK_TEXT = new Color(187, 187, 187);

    /** Фон полей ввода */
    public static final Color DARK_INPUT_BG = new Color(69, 73, 74);

    /** Цвет границ компонентов и разделителей */
    public static final Color DARK_BORDER = new Color(85, 85, 85);

    /** Стандартный цвет кнопки в спокойном состоянии */
    public static final Color DARK_BUTTON = new Color(50, 52, 54);

    /** Цвет кнопки при наведении курсора (Hover state) */
    public static final Color DARK_BUTTON_HOVER = new Color(75, 78, 80);

    /** Цвет дорожки скроллбара */
    public static final Color DARK_SCROLL_TRACK = new Color(43, 43, 43);
    /** Цвет ползунка скроллбара */
    public static final Color DARK_SCROLL_THUMB = new Color(80, 80, 80);

    /** Основной цвет фона окна */
    public static final Color LIGHT_BG = new Color(240, 240, 240);

    /** Цвет фона панелей */
    public static final Color LIGHT_PANEL = Color.WHITE;

    /** Основной цвет текста */
    public static final Color LIGHT_TEXT = new Color(43,43,43);

    /** Фон полей ввода */
    public static final Color LIGHT_INPUT_BG = Color.WHITE;

    /** Цвет границ компонентов */
    public static final Color LIGHT_BORDER = new Color(200, 200, 200);

    /** Стандартный цвет кнопки */
    public static final Color LIGHT_BUTTON = new Color(225, 225, 225);

    /** Цвет кнопки при наведении курсора (чуть светлее/ярче) */
    public static final Color LIGHT_BUTTON_HOVER = new Color(240, 240, 245);

    /**
     * Статус:NEW
     */
    public static final Color STATUS_NEW = new Color(24, 98, 181);

    /**
     * Статус:AGAIN/HARD
     */
    public static final Color STATUS_ERROR = new Color(166, 3, 3);

    /**
     * Статус:IN PROGRESS
     */
    public static final Color STATUS_LEARNING = new Color(218, 147, 13);

    /**
     * Статус:MASTERED/EASY</b>
     */
    public static final Color STATUS_MASTER = new Color(7, 117, 7);
}