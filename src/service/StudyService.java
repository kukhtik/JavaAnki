package service;

// Убрал ошибочный импорт data.repository.GradingService
import data.repository.HistoryRepository;
import lombok.Getter;
import lombok.Value;
import model.Card;
import model.HistoryRecord;
import model.dto.StatsRow;
import service.algorithm.SpacedRepetitionAlgorithm;
import service.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StudyService {
    private static final Logger LOGGER = Logger.getLogger(StudyService.class.getName());

    private final SessionManager sessionManager;
    private final SpacedRepetitionAlgorithm algorithm;
    private final GradingService gradingService; // Теперь класс виден, так как он в том же пакете

    private List<Card> activeDeck = new ArrayList<>();

    @Getter
    private Card currentCard;

    public StudyService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.algorithm = new SpacedRepetitionAlgorithm();
        this.gradingService = new GradingService();

        reloadSession();
    }

    public void reloadSession() {
        sessionManager.reload();
        resetFilter();
    }

    public List<Card> getAllCards() {
        return sessionManager.getAllCards();
    }

    // --- ИСТОРИЯ ---
    public List<HistoryRecord> getHistory() {
        return new HistoryRepository().loadHistory();
    }

    // --- СТАТИСТИКА ---
    public List<StatsRow> getStatistics() {
        List<Card> all = sessionManager.getAllCards();
        List<StatsRow> rows = new ArrayList<>();

        Map<String, List<Card>> grouped = all.stream()
                .collect(Collectors.groupingBy(Card::getCategory));

        for (var entry : grouped.entrySet()) {
            rows.add(calculateRow(entry.getKey(), entry.getValue()));
        }

        rows.add(calculateRow("=== ИТОГО ===", all));
        return rows;
    }

    private StatsRow calculateRow(String name, List<Card> list) {
        long total = list.size();
        long cntNew = list.stream().filter(Card::isNew).count();
        long cntMaster = list.stream().filter(c -> !c.isNew() && c.getLevel() >= 8).count();
        long cntLearn = total - cntNew - cntMaster;
        return new StatsRow(name, total, cntNew, cntLearn, cntMaster);
    }

    // --- ФИЛЬТРАЦИЯ ---
    public void setFilter(String filter) {
        if (filter == null || filter.equals("ВСЕ ТЕМЫ")) {
            resetFilter();
        } else {
            activeDeck = sessionManager.getAllCards().stream()
                    .filter(c -> sessionManager.isCardInGroup(c, filter))
                    .collect(Collectors.toList());
            LOGGER.info("Фильтр [" + filter + "]: " + activeDeck.size());
        }
    }

    public void resetFilter() {
        this.activeDeck = new ArrayList<>(sessionManager.getAllCards());
    }

    public List<String> getAvailableCategories() {
        return sessionManager.getAllCategories();
    }

    // --- ОБУЧЕНИЕ ---
    public Card nextCard(boolean isShuffle) {
        currentCard = algorithm.selectNextCard(activeDeck, currentCard, isShuffle);
        return currentCard;
    }

    public GradingResult checkAnswer(String userAnswer) {
        if (currentCard == null) return new GradingResult(0, false);
        double score = gradingService.calculateSimilarity(userAnswer, currentCard.getAnswer());
        return new GradingResult(score, score > 65.0);
    }

    public void submitResult(String userAnswer, boolean correct) {
        if (currentCard == null) return;

        algorithm.updateCardProgress(currentCard, correct);
        sessionManager.saveProgress(currentCard, userAnswer, correct);
    }

    @Value
    public static class GradingResult {
        double score;
        boolean passed;
    }
}