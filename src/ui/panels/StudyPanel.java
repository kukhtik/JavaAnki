package ui.panels;

import model.Card;
import service.StudyService;
import ui.Theme;
import ui.components.UIFactory;

import javax.swing.*;
import java.awt.*;

/**
 * Панель обучения, отвечающая за отображение вопроса,
 * ввод ответа пользователем, показ правильного ответа и визуальную
 * обратную связь
 * <ul>
 *     <li>запрашивает следующую карточку у сервиса</li>
 *     <li>отображает вопрос</li>
 *     <li>принимает ответ пользователя</li>
 *     <li>показывает правильный ответ и результат проверки</li>
 *     <li>позволяет подтвердить результат (верно/ошибка)</li>
 * </ul>
 */
public class StudyPanel extends JPanel {
    private final StudyService service;

    private JTextArea questionArea;
    private JTextArea answerArea;
    private JTextArea inputArea;
    private JLabel infoLabel;
    private JLabel feedbackLabel;
    private JPanel buttonPanel;
    private JButton checkBtn;
    private JButton yesBtn;
    private JButton noBtn;

    /** Режим выбора карточек: случайный или умный */
    private boolean isShuffleMode = false;

    /**
     * Создаёт панель обучения и сразу загружает первую карточку
     *
     * @param service сервис обучения
     */
    public StudyPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        initUI();
        loadNextCard();
    }

    /**
     * Инициализирует все UI-компоненты панели:
     * <ul>
     *     <li>область вопроса</li>
     *     <li>поле ввода ответа</li>
     *     <li>область правильного ответа</li>
     *     <li>кнопки управления</li>
     *     <li>строку состояния</li>
     * </ul>
     */
    private void initUI() {
        // ЦЕНТР: вопрос, ввод, ответ
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // вопрос
        questionArea = UIFactory.createArea(false);
        centerPanel.add(new JScrollPane(questionArea));

        // ввод ответа
        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.add(UIFactory.createLabel("Ваш ответ:"), BorderLayout.NORTH);
        inputArea = UIFactory.createArea(true);
        inputContainer.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        centerPanel.add(inputContainer);

        // ответ (скрыт)
        JPanel answerContainer = new JPanel(new BorderLayout());
        feedbackLabel = UIFactory.createLabel(" ");
        answerContainer.add(feedbackLabel, BorderLayout.NORTH);

        answerArea = UIFactory.createArea(false);
        answerArea.setVisible(false);
        answerContainer.add(new JScrollPane(answerArea), BorderLayout.CENTER);

        centerPanel.add(answerContainer);
        add(centerPanel, BorderLayout.CENTER);

        // ВЕРХ: информация
        infoLabel = new JLabel("Загрузка...", SwingConstants.CENTER);
        infoLabel.setOpaque(true);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(infoLabel, BorderLayout.NORTH);

        // НИЗ: кнопки
        buttonPanel = new JPanel(new FlowLayout());
        checkBtn = UIFactory.createButton("Проверить [Enter]", e -> checkAnswer());
        yesBtn = UIFactory.createButton("ВЕРНО [->]", e -> submit(true));
        noBtn = UIFactory.createButton("ОШИБКА [<-]", e -> submit(false));

        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Загружает следующую карточку из сервиса и обновляет UI.
     * Сбрасывает состояние полей, скрывает правильный ответ и
     * обновляет строку состояния
     */
    private void loadNextCard() {
        Card card = service.nextCard(isShuffleMode);

        // Сброс UI
        buttonPanel.removeAll();
        buttonPanel.add(checkBtn);
        inputArea.setText("");
        inputArea.setEditable(true);
        inputArea.setBackground(Theme.getInputBg());
        answerArea.setVisible(false);
        feedbackLabel.setText(" ");

        if (card == null) {
            questionArea.setText("Нет карт в этой категории.");
            checkBtn.setEnabled(false);
            return;
        }

        checkBtn.setEnabled(true);
        questionArea.setText(card.getQuestion());

        // Обновление строки состояния
        String status = card.isNew() ? "НОВОЕ" : "УРОВЕНЬ " + card.getLevel();
        infoLabel.setText(String.format("[%s] %s (%s)",
                card.getCategory(), status, isShuffleMode ? "Shuffle" : "Smart"));

        Theme.apply(this);
        buttonPanel.revalidate();
        buttonPanel.repaint();
        inputArea.requestFocusInWindow();
    }

    /**
     * Проверяет ответ пользователя, показывает правильный ответ,
     * визуальную обратную связь и переключает кнопки на
     * «Верно» / «Ошибка»
     */
    private void checkAnswer() {
        String userText = inputArea.getText();
        var result = service.checkAnswer(userText);
        Card card = service.getCurrentCard();

        answerArea.setText(card.getAnswer());
        answerArea.setVisible(true);
        inputArea.setEditable(false);

        feedbackLabel.setText(String.format("%s (%.0f%%)",
                result.isPassed() ? "Отлично!" : "Нужно повторить",
                result.getScore()));

        Color resultColor = result.isPassed()
                ? new Color(200, 255, 200)
                : new Color(255, 200, 200);

        if (Theme.isDark()) {
            resultColor = result.isPassed()
                    ? new Color(40, 80, 40)
                    : new Color(80, 40, 40);
        }

        inputArea.setBackground(resultColor);

        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        buttonPanel.revalidate();
        buttonPanel.repaint();

        (result.isPassed() ? yesBtn : noBtn).requestFocusInWindow();
    }

    /**
     * Передаёт результат ответа сервису и загружает следующую карточку.
     *
     * @param correct true, если пользователь подтвердил правильность ответа
     */
    private void submit(boolean correct) {
        service.submitResult(inputArea.getText(), correct);
        loadNextCard();
    }

    /**
     * Переключает режим выбора карточек (Shuffle/Smart)
     * Используется контроллером горячих клавиш.
     */
    public void toggleMode() {
        isShuffleMode = !isShuffleMode;
        loadNextCard();
    }
}
