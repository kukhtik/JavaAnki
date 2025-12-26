package ui.controller;

import model.Card;
import service.StudyService;
import ui.ThemeManager;
import ui.panels.StudyPanel;

import java.awt.Color;

public class StudyController {
    private final StudyService service;
    private final StudyPanel view;

    private boolean isShuffleMode = false;
    private boolean isAnswerRevealed = false;

    public StudyController(StudyService service, StudyPanel view) {
        this.service = service;
        this.view = view;
    }

    public void start() {
        loadNextCard();
    }

    public void onCheckAction() {
        if (!isAnswerRevealed) {
            checkAnswer();
        }
    }

    public void onSubmitCorrect() {
        if (isAnswerRevealed) submit(true);
    }

    public void onSubmitError() {
        if (isAnswerRevealed) submit(false);
    }

    public void onToggleMode() {
        isShuffleMode = !isShuffleMode;
        view.updateModeButton(isShuffleMode ? "Mode: SHUFFLE" : "Mode: SMART");
        loadNextCard();
    }

    public void onCategorySelected(String category) {
        if (category != null && !category.equals("ВСЕ ТЕМЫ")) {
            service.setFilter(category);
        } else {
            service.resetFilter();
        }
        loadNextCard();
    }

    private void loadNextCard() {
        isAnswerRevealed = false;
        Card card = service.nextCard(isShuffleMode);

        view.resetUI();

        if (card == null) {
            view.showEmptyState();
            return;
        }

        view.displayCard(card);
    }

    private void checkAnswer() {
        if (service.getCurrentCard() == null) return;
        isAnswerRevealed = true;

        String userAns = view.getUserInput();
        var result = service.checkAnswer(userAns);

        view.showAnswer(service.getCurrentCard().getAnswer());

        Color bgCol;
        if (ThemeManager.isDark()) {
            bgCol = result.isPassed() ? new Color(40, 80, 40) : new Color(80, 40, 40);
        } else {
            bgCol = result.isPassed() ? new Color(200, 255, 200) : new Color(255, 200, 200);
        }

        // Исправленный вызов:
        String message = result.isPassed() ? "OK" : "ОШИБКА";
        view.setFeedback(result.isPassed(), bgCol, message);
    }

    private void submit(boolean correct) {
        service.submitResult(view.getUserInput(), correct);
        loadNextCard();
    }
}