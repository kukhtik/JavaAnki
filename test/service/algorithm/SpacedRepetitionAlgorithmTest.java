package service.algorithm;

import model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование алгоритма SRS")
class SpacedRepetitionAlgorithmTest {

    private SpacedRepetitionAlgorithm algorithm;

    @BeforeEach
    void setUp() {
        algorithm = new SpacedRepetitionAlgorithm();
    }

    @Test
    @DisplayName("Верный ответ: Уровень повышается (0 -> 1)")
    void testUpdateProgress_CorrectAnswer_ShouldIncreaseLevel() {
        Card card = new Card();
        card.setId("test-id");
        card.setLevel(0);
        card.setNew(true);

        algorithm.updateCardProgress(card, true);

        assertEquals(1, card.getLevel(), "Уровень должен увеличиться на 1");
        assertFalse(card.isNew(), "Флаг 'Новая' должен быть снят");
    }

    @Test
    @DisplayName("Ошибка на низком уровне (<=5): Сброс в 0")
    void testUpdateProgress_WrongAnswer_ShouldResetLevel() {
        Card card = new Card();
        card.setLevel(3);
        card.setNew(false);

        algorithm.updateCardProgress(card, false);

        assertEquals(0, card.getLevel(), "Уровень 3 должен сброситься в 0");
    }

    @Test
    @DisplayName("Ошибка на высоком уровне (>5): Штраф до 2")
    void testUpdateProgress_WrongAnswer_HighLevelPenalty() {
        Card card = new Card();
        card.setLevel(8);

        algorithm.updateCardProgress(card, false);

        assertEquals(2, card.getLevel(), "Уровень 8 должен упасть до 2 (мягкий сброс)");
    }

    @Test
    @DisplayName("Выборка: Пустая колода возвращает null")
    void testSelectNextCard_EmptyDeck() {
        Card result = algorithm.selectNextCard(new ArrayList<>(), null, false);
        assertNull(result, "Метод должен вернуть null для пустого списка");
    }
}