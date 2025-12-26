package service;

import lombok.Getter;
import model.Card;
import service.algorithm.SpacedRepetitionAlgorithm;
import service.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StudyService {
    private static final Logger LOGGER = Logger.getLogger(StudyService.class.getName());

    private final SessionManager sessionManager;
    private final SpacedRepetitionAlgorithm algorithm;
    private final GradingService gradingService;

    // Состояние UI
    private List<Card> activeDeck = new ArrayList<>();
    @Getter private Card currentCard;

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

        // 1. Алгоритм обновляет состояние объекта карты
        algorithm.updateCardProgress(currentCard, correct);

        // 2. Менеджер сохраняет изменения на диск
        sessionManager.saveProgress(currentCard, userAnswer, correct);
    }

    public List<model.HistoryRecord> getHistory() {
        // Тут можно было бы тоже делегировать SessionManager, но пока так
        return new data.repository.HistoryRepository().loadHistory();
    }

    @lombok.Value
    public static class GradingResult {
        double score;
        boolean passed;
    }
}