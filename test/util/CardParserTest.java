package util;

import model.Card;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для парсера текстовых блоков.
 * <p>
 * Проверяет корректность преобразования сырого списка строк (из файла)
 * в объект {@link Card}
 * </p>
 */
@DisplayName("Тестирование CardParser")
class CardParserTest {

    @Test
    @DisplayName("Валидный блок (без ID): Должен создать новую карту с авто-ID")
    void parseSingleBlock_ValidInput_ShouldReturnCard() {
        List<String> block = Arrays.asList(
                "CATEGORY: Java Core",
                "QUESTION:",
                "What is unit test?",
                "ANSWER:",
                "Testing small parts of code"
        );

        Card card = CardParser.parseSingleBlock(block, "test_file.txt");

        assertNotNull(card);
        assertEquals("Java Core", card.getCategory());
        assertEquals("What is unit test?", card.getQuestion());
        assertEquals("Testing small parts of code", card.getAnswer());

        // CardFactory сработала и присвоила UUID
        assertNotNull(card.getId(), "ID должен быть сгенерирован автоматически");
        assertTrue(card.isNew(), "Карта без ID должна быть помечена как новая");
    }

    @Test
    @DisplayName("Блок с существующим ID: Должен сохранить ID")
    void parseSingleBlock_WithExistingID_ShouldPreserveID() {
        String existingId = "550e8400-e29b-41d4-a716-446655440000";
        List<String> block = Arrays.asList(
                "ID: " + existingId,
                "CATEGORY: Test",
                "QUESTION:",
                "Q",
                "ANSWER:",
                "A"
        );

        Card card = CardParser.parseSingleBlock(block, "file.txt");

        assertNotNull(card);
        assertEquals(existingId, card.getId(), "Парсер не должен менять существующий ID");
    }

    @Test
    @DisplayName("Многострочный текст: Должен сохранить переносы строк")
    void parseSingleBlock_MultiLineText_ShouldPreserveFormatting() {
        List<String> block = Arrays.asList(
                "CATEGORY: Code",
                "QUESTION:",
                "public static void main(String[] args) {",
                "    System.out.println(\"Hello\");",
                "}",
                "ANSWER:",
                "Entry point"
        );

        Card card = CardParser.parseSingleBlock(block, "code.txt");

        assertNotNull(card);
        // переносы строк сохранились (CardParser добавляет \n между строками)
        String expectedQuestion = "public static void main(String[] args) {\n    System.out.println(\"Hello\");\n}";

        // normalizeNewlines - вспомогательный метод (или просто trim(), если парсер делает trim)
        assertEquals(expectedQuestion, card.getQuestion());
    }

    @Test
    @DisplayName("Невалидный блок (нет вопроса): Должен вернуть null")
    void parseSingleBlock_MissingQuestion_ShouldReturnNull() {
        List<String> block = Arrays.asList(
                "CATEGORY: Test",
                // "QUESTION:" пропущен
                "ANSWER:",
                "A"
        );

        Card card = CardParser.parseSingleBlock(block, "file.txt");

        assertNull(card, "Парсер должен вернуть null, если обязательные поля отсутствуют");
    }

    @Test
    @DisplayName("Невалидный блок (нет категории): Должен вернуть null")
    void parseSingleBlock_MissingCategory_ShouldReturnNull() {
        List<String> block = Arrays.asList(
                "QUESTION:", "Q",
                "ANSWER:", "A"
        );

        Card card = CardParser.parseSingleBlock(block, "file.txt");
        assertNull(card);
    }
}