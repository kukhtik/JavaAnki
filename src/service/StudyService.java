package service;

import data.repository.DeckRepository;
import data.repository.GroupRepository;
import data.repository.HistoryRepository;
import data.repository.StatsRepository;
import lombok.Getter;
import model.Card;
import util.TextUtil;

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

    /**
     * Основной метод загрузки.
     * Выполняет миграцию на UUID и дедупликацию.
     */
    public void reloadSession() {
        LOGGER.info(">>> ЗАПУСК СЕССИИ (UUID SYSTEM)...");

        List<Card> rawCards = deckRepo.loadAllCards();
        Map<String, Integer> stats = statsRepo.loadStats();
        groupRepo.loadStructure();

        // Сет для отслеживания текстовых дубликатов
        Set<String> seenContentHashes = new HashSet<>();
        this.allCards = new ArrayList<>();

        int statsRestored = 0;
        int duplicatesSkipped = 0;

        for (Card c : rawCards) {
            // 1. Дедупликация по тексту вопроса
            String contentHash = TextUtil.normalizeForId(c.getQuestion());

            if (seenContentHashes.contains(contentHash)) {
                // Если мы уже загрузили такой вопрос в этой сессии - пропускаем
                duplicatesSkipped++;
                continue;
            }
            seenContentHashes.add(contentHash);

            // 2. Поиск статистики (Миграция)
            boolean foundStat = false;

            // А) Сначала ищем по UUID (если карта уже обновлена)
            if (c.getId() != null && stats.containsKey(c.getId())) {
                c.setLevel(stats.get(c.getId()));
                c.setNew(false);
                foundStat = true;
            }
            // Б) Если по UUID не нашли, ищем по старому хэшу текста (для старых карт)
            else {
                String legacyId = statsRepo.generateLegacyId(c.getQuestion());
                if (stats.containsKey(legacyId)) {
                    c.setLevel(stats.get(legacyId));
                    c.setNew(false);
                    foundStat = true;
                }
            }

            // Если статистики нет нигде - карта новая
            if (!foundStat) {
                c.setLevel(0);
                c.setNew(true);
            }

            if (foundStat) statsRestored++;
            this.allCards.add(c);
        }

        // 3. ПЕРЕЗАПИСЬ ФАЙЛОВ (Закрепление UUID)
        // Мы группируем карты обратно по файлам и перезаписываем их.
        // Это гарантирует, что если у карты не было ID в файле, он там появится.
        Map<String, List<Card>> cardsByFile = allCards.stream()
                .collect(Collectors.groupingBy(Card::getSourceFile));

        for (Map.Entry<String, List<Card>> entry : cardsByFile.entrySet()) {
            deckRepo.saveDeck(entry.getKey(), entry.getValue());
        }

        // 4. Сохраняем статистику в новом формате (UUID -> Level)
        statsRepo.saveStats(allCards);

        LOGGER.info("=== ИТОГ ЗАГРУЗКИ ===");
        LOGGER.info(String.format("Загружено карт: %d", allCards.size()));
        LOGGER.info(String.format("Дубликатов пропущено: %d", duplicatesSkipped));
        LOGGER.info(String.format("Статистики подтянуто: %d", statsRestored));
        LOGGER.info("Все файлы колод обновлены (UUID закреплены).");

        resetFilter();
    }

    // --- ФИЛЬТРАЦИЯ ---

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

    // --- SRS ЛОГИКА ---

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

        // Сохраняем статистику по UUID
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