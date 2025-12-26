package util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TextUtil {
    // Стоп-слова для умного поиска
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "и","в","на","с","для","а","но","to","the","of","is","are","что","это","как"
    ));

    public static String basicClean(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase();
    }

    /**
     * Возвращаем алгоритм старой версии приложения.
     * Удаляет только пробельные символы, но оставляет пунктуацию.
     * Это важно для совпадения хэшей с файлом anki_stats.txt
     */
    public static String normalizeForId(String text) {
        if (text == null) return "";
        // Старая логика: s.trim().toLowerCase().replaceAll("\\s+", "")
        return basicClean(text).replaceAll("\\s+", "");
    }

    public static List<String> tokenize(String text) {
        return Arrays.stream(basicClean(text).replaceAll("[^a-zа-я0-9]", " ").split("\\s+"))
                .filter(w -> w.length() > 1 && !STOP_WORDS.contains(w))
                .collect(Collectors.toList());
    }
}