package service.algorithm;

import model.Card;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Реализация алгоритма интервального повторения (Spaced Repetition System вроде))
 * <p>
 * Класс отвечает за два ключевых аспекта обучения:
 * <ol>
 *     <li>Выбор следующей карточки: Определение того, какую карту показать пользователю,
 *     основываясь на её текущем уровне</li>
 *     <li>Прогрессия: Изменение уровня карточки (Box) в зависимости от правильности ответа</li>
 * </ol>
 * </p>
 */
public class SpacedRepetitionAlgorithm {
    private final Random random = new Random();
    private static final Logger LOGGER = Logger.getLogger(SpacedRepetitionAlgorithm.class.getName());

    /**
     * Выбирает следующую карту из колоды
     * <p>
     * Одна и та же карта не должна выпасть два раза подряд
     * </p>
     *
     * @param deck текущая колода
     * @param currentCard карточка
     * @param isShuffle режим работы:
     *                    <ul>
     *                      <li>{@code true} (Shuffle) рандом, равновероятный выбор</li>
     *                      <li>{@code false} (Smart) взвешенный рандом (сложные чаще)</li>
     *                    </ul>
     * @return объект следующей {@link Card} или {@code null}, если колода пуста
     */
    public Card selectNextCard(List<Card> deck, Card currentCard, boolean isShuffle) {
        if (deck.isEmpty()) return null;
        if (deck.size() == 1) return deck.getFirst();

        // временный пул кандидатов, исключая текущую карточку
        // не получаем один и тот же вопрос сразу же после ответа
        List<Card> pool = new java.util.ArrayList<>(deck);
        if (currentCard != null) pool.remove(currentCard);

        // после удаления текущей карточки пул пуст (в колоде всего 1 карта)? используем исходную
        if (pool.isEmpty()) pool = deck;

        // Если выбран режим Smart (не Shuffle)
        if (!isShuffle) {
            Card selected = getWeightedRandomCard(pool);
            List<Card> finalPool = pool;
            LOGGER.fine(() -> String.format(
                    "ВЫБОР АЛГОРИТМА: Selected ID=%s (Level=%d) from pool of size %d",
                    selected.getId(), selected.getLevel(), finalPool.size()));
            return selected;
        } else {
            // алгоритм на основе весов
            return getWeightedRandomCard(pool);
        }
    }

    /**
     * Обновляет уровень (Level/Box) карточки на основе результата ответа
     * <p>
     * <b>Логика изменения уровня:</b>
     * <ul>
     *     <li><b>Верно:</b> Уровень повышается на +1 (максимум до 10). Выше уровень - > реже будет выпадать</li>
     *     <li><b>Ошибка (High Level > 5):</b> мягкий сброс. Если карта хорошо выучена, но забыта,
     *     она падает не в 0, а на уровень 2, чтобы вспомнить её</li>
     *     <li><b>Ошибка (Low Level <= 5):</b> полный сброс. Карта падает на уровень 0</li>
     * </ul>
     * </p>
     *
     * @param card карточка, на которую был дан ответ
     * @param isCorrect ответ верный или нет
     */
    public void updateCardProgress(Card card, boolean isCorrect) {
        // ответили на карту, она теряет флаг New
        card.setNew(false);

        if (isCorrect) {
            // лимит уровня = 10
            if (card.getLevel() < 10) card.setLevel(card.getLevel() + 1);
        } else {
            // штраф за ошибку
            if (card.getLevel() > 5) {
                card.setLevel(2); // мягкое падение
            } else {
                card.setLevel(0); // жесткое падение в начало
            }
        }
    }

    /**
     * Реализация алгоритма "Колесо фортуны" (Roulette Wheel Selection)
     * <p>
     * 1. Считаем сумму весов всех карт<br>
     * 2. Выбираем случайное число от 0 до Sum<br>
     * 3. Идем по списку, вычитая вес текущей карты. Где остановились — ту и берем
     * </p>
     */
    private Card getWeightedRandomCard(List<Card> pool) {
        double totalWeight = 0.0;
        for (Card c : pool) totalWeight += calculateWeight(c);

        double value = random.nextDouble() * totalWeight;
        for (Card c : pool) {
            value -= calculateWeight(c);
            if (value <= 0) return c;
        }
        return pool.getLast();
    }

    /**
     * Рассчитывает вероятностный вес карточки.
     * Выше вес - > чаще будет появляться
     *
     * @param c карточка
     * @return вес (double)
     */
    private double calculateWeight(Card c) {
        // самый высокий приоритет: карты, которые мы учили, но забыли (ошиблись, и уровень сбросился в 0)
        if (!c.isNew() && c.getLevel() == 0) return 150.0;

        // высокий приоритет: абсолютно новые карты
        if (c.isNew()) return 100.0;

        // остальные: Экспоненциальное затухание частоты, чё-то такое
        // Level 1: 100 * 0.7^1 = 70
        // Level 2: 100 * 0.7^2 = 49
        // Level 5: 100 * 0.7^5 = ~16
        // Level 10: ~2.8
        return 100.0 * Math.pow(0.7, c.getLevel());
    }
}