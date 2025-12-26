package service;

import data.repository.DeckRepository;
import data.repository.HistoryRepository;
import data.repository.StatsRepository;
import lombok.Getter;
import model.Card;

import java.util.*;
import java.util.stream.Collectors;
/**
 * Основной сервис обучающей системы
 * <p>
 * Отвечает за загрузку карточек, применение статистики, выбор следующей карточки,
 * проверку ответов и фиксацию результатов. Является центральным элементом
 */
public class StudyService {
    private final DeckRepository deckRepo = new DeckRepository();
    private final StatsRepository statsRepo = new StatsRepository();
    private final HistoryRepository historyRepo = new HistoryRepository();
    private final GradingService gradingService = new GradingService();
    private final Random random = new Random();
    // Добавить getter для всех карт (для StatsPanel)
    @Getter private List<Card> allCards = new ArrayList<>();
    private List<Card> activeDeck = new ArrayList<>();

    @Getter private Card currentCard;

    public StudyService() {
        reloadSession();
    }

    /**
     * Полная перезагрузка: чтение файлов и наложение статистики
     */
    public void reloadSession() {
        List<Card> rawCards = deckRepo.loadAllCards();
        Map<String, Integer> stats = statsRepo.loadStats();
        Map<String, String> seenQuestions = new HashMap<>();
        this.allCards = new ArrayList<>();

        for (Card c : rawCards) {
            String id = statsRepo.generateId(c.getQuestion());

            // пропуск полных дубликатов вопросов
            if (seenQuestions.containsKey(id)) continue;
            seenQuestions.put(id, c.getSourceFile());

            // уровень
            if (stats.containsKey(id)) {
                c.setLevel(stats.get(id));
                c.setNew(false);
            } else {
                c.setLevel(0);
                c.setNew(true);
            }
            this.allCards.add(c);
        }

        // активны все карты по умолчанию
        resetFilter();
    }

    public void setFilter(String category) {
        if (category == null || category.equals("ВСЕ ТЕМЫ")) {
            resetFilter();
        } else {
            activeDeck = allCards.stream()
                    .filter(c -> c.getCategory().equals(category))
                    .collect(Collectors.toList());
        }
    }

    public void resetFilter() {
        this.activeDeck = new ArrayList<>(allCards);
    }

    public List<String> getAvailableCategories() {
        return allCards.stream()
                .map(Card::getCategory)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Выбор следующей карты
     */
    public Card nextCard(boolean isShuffle) {
        if (activeDeck.isEmpty()) return null;

        // карта всего одна, возвращаем её
        if (activeDeck.size() == 1) {
            currentCard = activeDeck.get(0);
            return currentCard;
        }

        // исключаем текущую карту из выборки, чтобы не выпадала дважды подряд
        List<Card> pool = new ArrayList<>(activeDeck);
        if (currentCard != null) pool.remove(currentCard);

        if (pool.isEmpty()) pool = activeDeck;

        if (isShuffle) {
            currentCard = pool.get(random.nextInt(pool.size()));
        } else {
            currentCard = getWeightedRandomCard(pool);
        }
        return currentCard;
    }

    /**
     * Обработка ответа пользователя
     */
    public GradingResult checkAnswer(String userAnswer) {
        if (currentCard == null) return new GradingResult(0, false);

        double score = gradingService.calculateSimilarity(userAnswer, currentCard.getAnswer());
        boolean passed = score > 65.0; // порог прохождения

        return new GradingResult(score, passed);
    }

    /**
     * Фиксация результата (нажал "Верно" или "Ошибка")
     */
    public void submitResult(String userAnswer, boolean correct) {
        if (currentCard == null) return;
        updateCardLevel(currentCard, correct);
        historyRepo.saveEntry(currentCard.getQuestion(), userAnswer, correct);
        statsRepo.saveStats(allCards);
    }

    private void updateCardLevel(Card card, boolean correct) {
        card.setNew(false);
        if (correct) {
            if (card.getLevel() < 10) card.setLevel(card.getLevel() + 1);
        } else {
            // ошибка? сильный штраф
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
        if (!c.isNew() && c.getLevel() == 0) return 150.0; // Ошибки - приоритет
        if (c.isNew()) return 100.0; // Новые
        return 100.0 * Math.pow(0.7, c.getLevel()); // выше уровень, реже
    }

    // DTO для возврата результата проверки
    @lombok.Value
    public static class GradingResult {
        double score;
        boolean passed;
    }
    // Добавить в StudyService.java
    public List<model.HistoryRecord> getHistory() {
        return historyRepo.loadHistory();
    }

}