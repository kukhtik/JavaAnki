package service.algorithm;

import model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Модульные тесты для алгоритма интервального повторения (SRS).
 * Проверяет математическую модель изменения уровней карточек и логику выборки
 */
@DisplayName("Тестирование SRS Алгоритма")
public class AlgorithmTest {

    private SpacedRepetitionAlgorithm algorithm;
    private Card testCard;

    /**
     * Выполняется перед каждым тестом.
     * Сбрасывает состояние, создавая чистый экземпляр алгоритма и тестовой карты
     */
    @BeforeEach
    void setUp() {
        algorithm = new SpacedRepetitionAlgorithm();
        // новая карта: ID="1", Level=0, isNew=true
        testCard = new Card("1", "TestCat", "Q", "A", "file.txt", 0, true);
    }

    @Test
    @DisplayName("Верный ответ: Уровень повышается, флаг New снимается")
    void testProgress_CorrectAnswer() {
        // исходное состояние: Level 0
        algorithm.updateCardProgress(testCard, true);

        assertAll("Проверка успешного ответа",
                () -> assertEquals(1, testCard.getLevel(), "Уровень должен подняться с 0 до 1"),
                () -> assertFalse(testCard.isNew(), "Карта больше не должна быть новой")
        );
    }

    @Test
    @DisplayName("Верный ответ: Уровень не превышает максимум (10)")
    void testProgress_MaxLevelCap() {
        testCard.setLevel(10);
        algorithm.updateCardProgress(testCard, true);

        assertEquals(10, testCard.getLevel(), "Уровень не должен превышать 10");
    }

    @Test
    @DisplayName("Ошибка на низком уровне (<= 5): Полный сброс в 0")
    void testProgress_WrongAnswer_LowLevel() {
        testCard.setLevel(3);
        testCard.setNew(false);

        algorithm.updateCardProgress(testCard, false);

        assertEquals(0, testCard.getLevel(), "При ошибке на уровне 3 должен быть сброс в 0");
    }

    @Test
    @DisplayName("Ошибка на высоком уровне (> 5): Мягкий сброс в 2")
    void testProgress_WrongAnswer_HighLevel() {
        testCard.setLevel(8);
        testCard.setNew(false);

        algorithm.updateCardProgress(testCard, false);

        assertEquals(2, testCard.getLevel(), "При ошибке на уровне 8 уровень должен упасть до 2 (штраф)");
    }
    @Test
    @DisplayName("Выборка: Возвращает null для пустой колоды")
    void testSelection_EmptyDeck() {
        Card result = algorithm.selectNextCard(new ArrayList<>(), null, false);
        assertNull(result, "Для пустой колоды должен возвращаться null");
    }
    @Test
    @DisplayName("Выборка: Не повторять текущую карту, если есть выбор")
    void testSelection_AvoidCurrentCard() {
        Card c1 = new Card("1", "C", "Q1", "A", "f", 0, false);
        Card c2 = new Card("2", "C", "Q2", "A", "f", 0, false);
        List<Card> deck = List.of(c1, c2);

        // текущая c1, алгоритм обязан вернуть c2 (так как пул > 1)
        Card next = algorithm.selectNextCard(deck, c1, false);

        assertNotNull(next);
        assertEquals(c2, next, "Алгоритм должен выбрать другую карту, чтобы не спамить одной и той же");
    }

    @Test
    @DisplayName("Выборка: Если карта всего одна, возвращать её же")
    void testSelection_SingleCardDeck() {
        List<Card> deck = List.of(testCard);

        Card next = algorithm.selectNextCard(deck, testCard, false);

        assertEquals(testCard, next, "Если в колоде 1 карта, она должна возвращаться даже если была текущей");
    }
}