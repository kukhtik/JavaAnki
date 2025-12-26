package service.algorithm;

import model.Card;
import java.util.List;
import java.util.Random;

public class SpacedRepetitionAlgorithm {
    private final Random random = new Random();

    /**
     * Выбирает следующую карту из списка на основе весов.
     */
    public Card selectNextCard(List<Card> deck, Card currentCard, boolean isShuffle) {
        if (deck.isEmpty()) return null;
        if (deck.size() == 1) return deck.get(0);

        // Исключаем текущую, чтобы не повторялась подряд
        List<Card> pool = new java.util.ArrayList<>(deck);
        if (currentCard != null) pool.remove(currentCard);
        if (pool.isEmpty()) pool = deck;

        if (isShuffle) {
            return pool.get(random.nextInt(pool.size()));
        } else {
            return getWeightedRandomCard(pool);
        }
    }

    /**
     * Обновляет уровень карты на основе результата.
     */
    public void updateCardProgress(Card card, boolean isCorrect) {
        card.setNew(false);
        if (isCorrect) {
            if (card.getLevel() < 10) card.setLevel(card.getLevel() + 1);
        } else {
            if (card.getLevel() > 5) card.setLevel(2);
            else card.setLevel(0);
        }
    }

    private Card getWeightedRandomCard(List<Card> pool) {
        double totalWeight = 0.0;
        for (Card c : pool) totalWeight += calculateWeight(c);

        double value = random.nextDouble() * totalWeight;
        for (Card c : pool) {
            value -= calculateWeight(c);
            if (value <= 0) return c;
        }
        return pool.get(pool.size() - 1);
    }

    private double calculateWeight(Card c) {
        if (!c.isNew() && c.getLevel() == 0) return 150.0; // Приоритет ошибкам
        if (c.isNew()) return 100.0;
        return 100.0 * Math.pow(0.7, c.getLevel()); // Экспоненциальное падение частоты
    }
}