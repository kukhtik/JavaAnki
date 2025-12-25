package util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Класс для нормализации и обработки текста
 * <p>
 * Предоставляет три основных типа операций:
 * <ul>
 *     <li>Базовая очистка текста (обрезка пробелов, приведение к нижнему регистру)</li>
 *     <li>Жёсткая нормализация для сравнения идентификаторов</li>
 *     <li>Токенизация текста для нечеткого поиска</li>
 * </ul>
 * Класс предназначен для использования в поисковых механизмах, дедупликации,
 * индексировании текста и любых задачах, где требуется стандартизированная обработка строк.
 */
public class TextUtil {
    /**
     * Нормализация для получения чистого индификатора строки
     */
    private static final Pattern STRICT_PATTERN = Pattern.compile("[a-zа-я0-9]");
    /**
     * Слова, исключаемые из поиска
     */
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList( "и","в","на","с","для","а","но","to","the","of","is","are","что","это","как" ));

    /**
     * Чистка текста
     * <ul>
     *     <li>обрезает пробелы по краям</li>
     *     <li>приводит строку к нижнему регистру</li>
     * </ul>
     */
    public static String basicClean(String text){
        if (text == null) return "";
        return text.trim().toLowerCase();
    }

    /**
     * Нормадизация, удаление всего, кроме букв, цифр
     */
    public static String normalizedForId(String text){
        if (text==null) return "";
        return STRICT_PATTERN.matcher(basicClean(text)).replaceAll("");
    }

    /**
     * Разбивает текст на токены (слова), удаляя:
     * <ul>
     *     <li>все неалфавитно-цифровые символы</li>
     *     <li>стоп-слова</li>
     *     <li>слишком короткие слова (длиной 1 символ)</li>
     * </ul>
     * <p>
     * Используется в нечетком поиске, анализе текста и индексировании
     */
    public static List<String> tokenize(String text){
        return Arrays.stream(basicClean(text).replaceAll("a-zа-я0-9"," ").split("\\s+"))
                .filter(w->w.length()>1&&!STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }
}
