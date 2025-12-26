package service;

import data.repository.DeckRepository;
import data.repository.GroupRepository;
import data.repository.HistoryRepository;
import data.repository.StatsRepository;
import lombok.Getter;
import model.Card;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StudyService {
    private static final Logger LOGGER = Logger.getLogger(StudyService.class.getName());

    private final DeckRepository deckRepo = new DeckRepository();
    private final StatsRepository statsRepo = new StatsRepository();
    private final HistoryRepository historyRepo = new HistoryRepository();
    private final GroupRepository groupRepo = new GroupRepository();

    private final GradingService gradingService = new GradingService();
    private final Random random = new Random();

    @Getter private List<Card> allCards = new ArrayList<>();
    private List<Card> activeDeck = new ArrayList<>();

    @Getter private Card currentCard;

    public StudyService() {
        reloadSession();
    }

    public void reloadSession() {
        LOGGER.info(">>> ЗАПУСК СЕССИИ: ОБРАБОТКА ДАННЫХ...");

        List<Card> rawCards = deckRepo.loadAllCards();
        Map<String, Integer> stats = statsRepo.loadStats();
        groupRepo.loadStructure();

        // Карта для отслеживания дубликатов: ID -> Имя файла первого появления
        Map<String, Card> uniqueMap = new HashMap<>();
        this.allCards = new ArrayList<>();

        int statsApplied = 0;
        int duplicatesFound = 0;

        for (Card c : rawCards) {
            String id = statsRepo.generateId(c.getQuestion());

            // ПРОВЕРКА НА ДУБЛИКАТЫ С ЛОГИРОВАНИЕМ
            if (uniqueMap.containsKey(id)) {
                Card existing = uniqueMap.get(id);
                // Логируем только если файлы разные (если внутри одного файла дубль - это менее важно)
                if (!existing.getSourceFile().equals(c.getSourceFile())) {
                    LOGGER.warning(String.format("ДУБЛИКАТ: Вопрос '%s' из [%s] уже загружен из [%s]. Пропуск.",
                            trimLog(c.getQuestion()), c.getSourceFile(), existing.getSourceFile()));
                }
                duplicatesFound++;
                continue;
            }

            // Если карта уникальная
            uniqueMap.put(id, c);

            // Применяем статистику
            if (stats.containsKey(id)) {
                c.setLevel(stats.get(id));
                c.setNew(false);
                statsApplied++;
            } else {
                c.setLevel(0);
                c.setNew(true);
            }
            this.allCards.add(c);
        }

        LOGGER.info(String.format("=== ИТОГ ЗАГРУЗКИ ==="));
        LOGGER.info(String.format("Всего найдено карт в файлах: %d", rawCards.size()));
        LOGGER.info(String.format("Отброшено дубликатов: %d", duplicatesFound));
        LOGGER.info(String.format("Загружено в память: %d", allCards.size()));
        LOGGER.info(String.format("Восстановлена статистика для: %d карт (из %d записей в файле)", statsApplied, stats.size()));
        LOGGER.info("=========================");

        resetFilter();
    }

    // Вспомогательный метод для красивого лога
    private String trimLog(String s) {
        return s.replace("\n", " ").trim().substring(0, Math.min(30, s.length())) + "...";
    }

    // --- ОСТАЛЬНОЙ КОД БЕЗ ИЗМЕНЕНИЙ ---
    public void setFilter(String selectedItem) {
        if (selectedItem == null || selectedItem.equals("ВСЕ ТЕМЫ")) {
            resetFilter();
            LOGGER.info("Фильтр: ВСЕ ТЕМЫ (Карт: " + activeDeck.size() + ")");
        } else {
            if (groupRepo.getGroupNames().contains(selectedItem)) {
                activeDeck = allCards.stream()
                        .filter(c -> groupRepo.isCardInGroup(c.getSourceFile(), selectedItem))
                        .collect(Collectors.toList());
                LOGGER.info("Фильтр ГРУППА [" + selectedItem + "]: " + activeDeck.size());
            } else {
                activeDeck = allCards.stream()
                        .filter(c -> c.getCategory().equals(selectedItem))
                        .collect(Collectors.toList());
                LOGGER.info("Фильтр КАТЕГОРИЯ [" + selectedItem + "]: " + activeDeck.size());
            }
        }
    }

    public void resetFilter() {
        this.activeDeck = new ArrayList<>(allCards);
    }

    public List<String> getAvailableCategories() {
        List<String> options = new ArrayList<>(groupRepo.getGroupNames());
        Collections.sort(options);
        List<String> cats = allCards.stream().map(Card::getCategory).distinct().sorted().toList();
        options.addAll(cats);
        return options;
    }

    public Card nextCard(boolean isShuffle) {
        if (activeDeck.isEmpty()) return null;
        if (activeDeck.size() == 1) {
            currentCard = activeDeck.get(0);
            return currentCard;
        }
        List<Card> pool = new ArrayList<>(activeDeck);
        if (currentCard != null) pool.remove(currentCard);
        if (pool.isEmpty()) pool = activeDeck;

        if (isShuffle) currentCard = pool.get(random.nextInt(pool.size()));
        else currentCard = getWeightedRandomCard(pool);
        return currentCard;
    }

    public GradingResult checkAnswer(String userAnswer) {
        if (currentCard == null) return new GradingResult(0, false);
        double score = gradingService.calculateSimilarity(userAnswer, currentCard.getAnswer());
        return new GradingResult(score, score > 65.0);
    }

    public void submitResult(String userAnswer, boolean correct) {
        if (currentCard == null) return;
        currentCard.setNew(false);
        if (correct) {
            if (currentCard.getLevel() < 10) currentCard.setLevel(currentCard.getLevel() + 1);
        } else {
            if (currentCard.getLevel() > 5) currentCard.setLevel(2);
            else currentCard.setLevel(0);
        }
        historyRepo.saveEntry(currentCard.getQuestion(), userAnswer, correct);
        statsRepo.saveStats(allCards);
    }

    private double calculateWeight(Card c) {
        if (!c.isNew() && c.getLevel() == 0) return 150.0;
        if (c.isNew()) return 100.0;
        return 100.0 * Math.pow(0.7, c.getLevel());
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

    public List<model.HistoryRecord> getHistory() { return historyRepo.loadHistory(); }

    @lombok.Value
    public static class GradingResult {
        double score;
        boolean passed;
    }
}