package util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Утилиты для обработки текста
 * <p>
 * Содержит методы для нормализации строк, токенизации и очистки от шума
 * Используется для:
 * <ul>
 *     <li>Генерации хешей ID (строгая нормализация)</li>
 *     <li>Сравнения ответов пользователя (умная токенизация)</li>
 * </ul>
 * </p>
 */
public class TextUtil {

    /** Список стоп-слов, которые игнорируются при проверке ответов (предлоги, артикли) */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "и","в","на","с","для","а","но","to","the","of","is","are","что","это","как"
    ));

    /**
     * Базовая очистка: trim + lowercase
     */
    public static String basicClean(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase();
    }

    /**
     * Строгая нормализация для генерации Legacy-ID
     *
     * @param text исходный текст вопроса
     * @return строка без пробелов в нижнем регистре
     */
    public static String normalizeForId(String text) {
        if (text == null) return "";
        // удаляем все пробельные символы (\s+)
        return basicClean(text).replaceAll("\\s+", "");
    }

    /**
     * Токенизация для нечеткого поиска и проверки ответов
     * <p>
     * 1. Приводит к нижнему регистру.<br>
     * 2. Заменяет все не-буквы и не-цифры на пробелы.<br>
     * 3. Разбивает по пробелам.<br>
     * 4. Удаляет стоп-слова и слова короче 2 символов
     * </p>
     *
     * @param text текст ответа
     * @return список токенов
     */
    public static List<String> tokenize(String text) {
        return Arrays.stream(basicClean(text).replaceAll("[^a-zа-я0-9]", " ").split("\\s+"))
                .filter(w -> w.length() > 1 && !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }

    private TextUtil() {}
}