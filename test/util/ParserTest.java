package util;

import model.Card;
import java.util.Arrays;
import java.util.List;

/**
 * Ручной тест парсера
 * <p>
 * Используется для быстрой проверки логики парсинга без запуска всего приложения
 * и без подключения библиотек тестирования.
 * Запускается как обычный метод {@code main}
 * </p>
 */
public class ParserTest {

    public static void main() {
        System.out.println("=== TEST SUITE: CardParser (Manual) ===");
        try {
            testParseValidBlock();
            testParseWithID();
            testParseInvalid();
            System.out.println(">>> ALL PARSER TESTS PASSED SUCCESSFULLY");
        } catch (RuntimeException e) {
            System.err.println("!!! TEST FAILED !!!");
            System.err.println(e.getMessage());
        }
    }

    /**
     * Тест: Корректный блок текста без ID должен превращаться в валидную карту с новым ID
     */
    private static void testParseValidBlock() {
        List<String> block = Arrays.asList(
                "CATEGORY: Test",
                "QUESTION:",
                "What is Java?",
                "ANSWER:",
                "A language"
        );

        Card c = CardParser.parseSingleBlock(block, "test.txt");

        check(c != null, "Парсер вернул null для валидного блока");
        check("Test".equals(c.getCategory()), "Неверная категория: " + c.getCategory());
        check("What is Java?".equals(c.getQuestion()), "Неверный вопрос");
        check("A language".equals(c.getAnswer()), "Неверный ответ");

        // работа CardFactory внутри парсера
        check(c.getId() != null && !c.getId().isEmpty(), "ID должен быть сгенерирован автоматически");
        check(c.isNew(), "Новая карта должна иметь статус isNew=true");

        System.out.println("testParseValidBlock: OK");
    }

    /**
     * Тест: Блок с полем ID: ... должен сохранять этот ID (восстановление)
     */
    private static void testParseWithID() {
        String expectedId = "123-abc-uuid";
        List<String> block = Arrays.asList(
                "ID: " + expectedId,
                "CATEGORY: Test",
                "QUESTION:", "Q",
                "ANSWER:", "A"
        );

        Card c = CardParser.parseSingleBlock(block, "test.txt");

        check(c != null, "Парсер вернул null");
        check(expectedId.equals(c.getId()), "ID не совпадает. Ожидался: " + expectedId + ", получен: " + c.getId());

        System.out.println("testParseWithID: OK");
    }

    /**
     * Тест: Неполный блок (без вопроса) должен быть отклонен
     */
    private static void testParseInvalid() {
        // без поля QUESTION
        List<String> block = Arrays.asList(
                "CATEGORY: Test",
                "ANSWER: A"
        );

        Card c = CardParser.parseSingleBlock(block, "test.txt");

        check(c == null, "Парсер должен вернуть null для невалидного блока (без вопроса)");

        System.out.println("testParseInvalid: OK");
    }

    /**
     * Вспомогательный метод для проверки условий
     */
    private static void check(boolean condition, String msg) {
        if (!condition) {
            throw new RuntimeException("ASSERTION FAILED: " + msg);
        }
    }
}