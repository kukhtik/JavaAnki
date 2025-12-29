package service;

/**
 * Ручной тест для сервиса проверки ответов
 * <p>
 * Проверяет математику нечеткого сравнения строк
 * Тест гарантирует, что алгоритм:
 * <ul>
 *     <li>Прощает мелкие опечатки (typo tolerance) благодаря расстоянию Левенштейна</li>
 *     <li>Корректно обрабатывает мусорные данные (null, empty)</li>
 * </ul>
 * </p>
 */
public class GradingTest {

    public static void main() {
        System.out.println("=== TEST SUITE: GradingService (Manual) ===");
        try {
            testExactMatch();
            testTypoTolerance();
            testCompleteMismatch();
            testEmptyInput();
            System.out.println(">>> ALL GRADING TESTS PASSED");
        } catch (Exception e) {
            System.err.println("!!! TEST FAILED !!!");
        }
    }

    /**
     * Тест: Идеальное совпадение должно давать 100%
     */
    private static void testExactMatch() {
        GradingService service = new GradingService();
        double score = service.calculateSimilarity("Hello World", "Hello World");

        // мизерная погрешность double
        check(score > 99.0, "Exact match should be 100%, got " + score);
        System.out.println("testExactMatch: OK");
    }

    /**
     * Тест: Устойчивость к опечаткам
     * <br>Сценарий: Пользователь пропустил букву 's' в слове "System"
     * <br>Ожидание: Алгоритм должен засчитать это слово как верное, так как дистанция Левенштейна = 1
     */
    private static void testTypoTolerance() {
        GradingService service = new GradingService();
        double score = service.calculateSimilarity("System out print", "System out print");

        check(score > 80.0, "Typo should be tolerated (score > 80%), actual: " + score);
        System.out.println("testTypoTolerance: OK");
    }

    /**
     * Тест: Совершенно разные слова должны давать низкий балл
     */
    private static void testCompleteMismatch() {
        GradingService service = new GradingService();
        double score = service.calculateSimilarity("Apple", "Banana");

        // Ожидаем 0%, так как ни одно слово не совпало
        check(score < 10.0, "Mismatch should be low score, got " + score);
        System.out.println("testCompleteMismatch: OK");
    }

    /**
     * Тест: Граничные условия (null, пустые строки).
     * Сервис не должен падать с NullPointerException
     */
    private static void testEmptyInput() {
        GradingService service = new GradingService();

        double score = service.calculateSimilarity("", "Answer");
        check(score == 0.0, "Empty input should be 0%");

        score = service.calculateSimilarity(null, "Answer");
        check(score == 0.0, "Null input should be 0%");

        System.out.println("testEmptyInput: OK");
    }

    /**
     * Простейший аналог Assert.assertTrue
     */
    private static void check(boolean condition, String msg) {
        if (!condition) throw new RuntimeException("ASSERTION FAILED: " + msg);
    }
}