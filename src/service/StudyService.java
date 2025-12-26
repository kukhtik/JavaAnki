package service;

import data.repository.*;
import lombok.Getter;
import model.Card;
import util.TextUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StudyService {
    private static final Logger LOGGER = Logger.getLogger(StudyService.class.getName());

    // Используем ИНТЕРФЕЙСЫ, а не конкретные классы
    private final CardRepository deckRepo;
    private final StatsRepository statsRepo;

    // Эти пока оставим конкретными, чтобы не усложнять всё сразу,
    // но по-хорошему их тоже надо через интерфейсы
    private final HistoryRepository historyRepo;
    private final GroupRepository groupRepo;

    private final GradingService gradingService;
    private final Random random = new Random();

    @Getter private List<Card> allCards = new ArrayList<>();
    private List<Card> activeDeck = new ArrayList<>();
    @Getter private Card currentCard;

    // КОНСТРУКТОР С ВНЕДРЕНИЕМ ЗАВИСИМОСТЕЙ (DI)
    public StudyService(CardRepository deckRepo,
                        StatsRepository statsRepo,
                        HistoryRepository historyRepo,
                        GroupRepository groupRepo) {
        this.deckRepo = deckRepo;
        this.statsRepo = statsRepo;
        this.historyRepo = historyRepo;
        this.groupRepo = groupRepo;
        this.gradingService = new GradingService();

        reloadSession();
    }

    public void reloadSession() {
        LOGGER.info(">>> ЗАПУСК СЕССИИ (DI ENABLED)...");

        List<Card> rawCards = deckRepo.loadAllCards(); // Вызов через интерфейс
        Map<String, Integer> stats = statsRepo.loadStats(); // Вызов через интерфейс
        groupRepo.loadStructure();

        Set<String> seenContentHashes = new HashSet<>();
        this.allCards = new ArrayList<>();

        int statsRestored = 0;
        int duplicatesSkipped = 0;

        for (Card c : rawCards) {
            String contentHash = TextUtil.normalizeForId(c.getQuestion());

            if (seenContentHashes.contains(contentHash)) {
                duplicatesSkipped++;
                continue;
            }
            seenContentHashes.add(contentHash);

            boolean foundStat = false;

            if (c.getId() != null && stats.containsKey(c.getId())) {
                c.setLevel(stats.get(c.getId()));
                c.setNew(false);
                foundStat = true;
            } else {
                // Логика генерации Legacy ID переехала сюда (или в TextUtil),
                // так как Репозиторий должен быть "глупым".
                String legacyId = String.valueOf(TextUtil.normalizeForId(c.getQuestion()).hashCode());
                if (stats.containsKey(legacyId)) {
                    c.setLevel(stats.get(legacyId));
                    c.setNew(false);
                    foundStat = true;
                }
            }

            if (!foundStat) {
                c.setLevel(0);
                c.setNew(true);
            }

            if (foundStat) statsRestored++;
            this.allCards.add(c);
        }

        // Перезапись файлов через интерфейс
        Map<String, List<Card>> cardsByFile = allCards.stream()
                .collect(Collectors.groupingBy(Card::getSourceFile));

        for (Map.Entry<String, List<Card>> entry : cardsByFile.entrySet()) {
            deckRepo.saveDeck(entry.getKey(), entry.getValue());
        }

        statsRepo.saveStats(allCards);

        LOGGER.info("=== ИТОГ ЗАГРУЗКИ ===");
        LOGGER.info("Загружено карт: " + allCards.size());
        LOGGER.info("Дубликатов: " + duplicatesSkipped);
        LOGGER.info("Восстановлено статистики: " + statsRestored);

        resetFilter();
    }

    // --- ОСТАЛЬНЫЕ МЕТОДЫ (Без изменений) ---

    public void setFilter(String selectedItem) {
        if (selectedItem == null || selectedItem.equals("ВСЕ ТЕМЫ")) {
            resetFilter();
        } else {
            if (groupRepo.getGroupNames().contains(selectedItem)) {
                activeDeck = allCards.stream()
                        .filter(c -> groupRepo.isCardInGroup(c.getSourceFile(), selectedItem))
                        .collect(Collectors.toList());
            } else {
                activeDeck = allCards.stream()
                        .filter(c -> c.getCategory().equals(selectedItem))
                        .collect(Collectors.toList());
            }
        }
    }

    public void resetFilter() { this.activeDeck = new ArrayList<>(allCards); }

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