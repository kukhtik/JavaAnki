package service;

import util.TextUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис проверки ответов
 * <p>
 * Отвечает за сравнение ответа пользователя с эталоном.
 * Нестрогое сравнение, который:
 * <ul>
 *     <li>Игнорирует регистр и знаки препинания (через {@link TextUtil})</li>
 *     <li>Игнорирует порядок слов (подход "Bag of Words")</li>
 *     <li>Прощает мелкие опечатки (используя расстояние Левенштейна)</li>
 * </ul>
 * </p>
 */
public class GradingService {

    /**
     * Вычисляет процент сходства между ответом пользователя и эталоном.
     *
     * @param userRaw ответ пользователя
     * @param correctRaw правильный ответ из карточки
     * @return число от 0.0 до 100.0 (процент совпадения)
     */
    public double calculateSimilarity(String userRaw, String correctRaw){
        if (correctRaw == null || userRaw == null) return 0.0;

        // токенизация: разбиваем на слова, чистим от мусора, приводим к нижнему регистру
        List<String> userTokens = TextUtil.tokenize(userRaw);
        List<String> correctTokens = TextUtil.tokenize(correctRaw);

        if (correctTokens.isEmpty()) return 0.0;

        int matches = 0;
        // создаем изменяемый пул слов пользователя, чтобы одно слово не засчитывалось дважды
        List<String> userPool = new ArrayList<>(userTokens);

        // жадный поиск совпадений
        for (String correctWord : correctTokens){
            for (int i = 0; i < userPool.size(); i++){
                String userWord = userPool.get(i);

                // сравниваем слова с учетом допустимых опечаток
                if (isSimilar(correctWord, userWord)){
                    matches++;
                    // удаляем использованное слово, уникальность
                    userPool.remove(i);
                    break;
                }
            }
        }

        // расчет процента: (совпавшие / всего) * 100
        return (double) matches / correctTokens.size() * 100.0;
    }

    /**
     * Проверяет, являются ли два слова "похожими".
     * Использует эвристику на основе длины слова
     */
    private boolean isSimilar(String a, String b){
        // полное совпадение
        if (a.equals(b)) return true;

        // оптимизация: если разница в длине > 2, то слова точно разные
        if (Math.abs(a.length() - b.length()) > 2) return false;

        // адаптивный порог ошибок:
        // для длинных слов (> 6 букв) допускаем 2 ошибки / опечатки
        // для коротких слов - только 1 ошибку
        int limit = a.length() > 6 ? 2 : 1;

        return levenstein(a, b) <= limit;
    }

    /**
     * Алгоритм Расстояния Левенштейна (Levenshtein Distance)
     * <p>
     * Вычисляет минимальное количество операций (вставка, удаление, замена символа),
     * необходимых для превращения одной строки в другую
     * Реализовано через Динамическое Программирование (матрица {@code dp})
     * </p>
     *
     * @return Количество правок (int)
     */
    private int levenstein(String a, String b){
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        // первоя строка и первого столбца
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        // заполнение матрицы
        for (int i = 1; i <= a.length(); i++){
            for (int j = 1; j <= b.length(); j++){
                // буквы равны, стоимость замены = 0, иначе = 1
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, // удаление
                                dp[i][j - 1] + 1), // вставка
                        dp[i - 1][j - 1] + cost // замена
                );
            }
        }
        return dp[a.length()][b.length()];
    }
}