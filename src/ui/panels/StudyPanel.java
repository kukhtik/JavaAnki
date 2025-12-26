package ui.panels;

import model.Card;
import service.StudyService;
import ui.ThemeManager;
import ui.components.UIFactory;
import ui.controller.StudyController; // Убедитесь, что этот класс создан

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StudyPanel extends JPanel {
    private StudyController controller;

    // UI Элементы
    private JComboBox<String> categoryBox;
    private JTextArea questionArea, answerArea, inputArea;
    private JLabel infoLabel, feedbackLabel;
    private JPanel buttonPanel;
    private JButton checkBtn, yesBtn, noBtn;
    private JButton modeBtn;

    public StudyPanel(StudyService service) {
        setLayout(new BorderLayout());

        // 1. Создаем UI компоненты
        initUI(service);

        // 2. Создаем контроллер (связываем View и Service)
        this.controller = new StudyController(service, this);

        // 3. Навешиваем слушатели, которые делегируют контроллеру
        setupListeners();
        setupKeys();

        // 4. Запускаем первый цикл
        controller.start();
    }

    // --- ПУБЛИЧНЫЕ МЕТОДЫ (API для Контроллера) ---

    public String getUserInput() {
        return inputArea.getText();
    }

    public void updateModeButton(String text) {
        modeBtn.setText(text);
    }

    public void resetUI() {
        buttonPanel.removeAll();
        buttonPanel.add(checkBtn);

        inputArea.setText("");
        inputArea.setEditable(true);
        inputArea.setBackground(ThemeManager.getInputBg());

        answerArea.setVisible(false);
        feedbackLabel.setText(" ");

        ThemeManager.apply(this);
        buttonPanel.revalidate(); buttonPanel.repaint();
        inputArea.requestFocusInWindow();
    }

    public void showEmptyState() {
        questionArea.setText("Нет карт в этой категории.");
        checkBtn.setEnabled(false);
        infoLabel.setText("Пусто");
        infoLabel.setBackground(Color.GRAY);
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    public void displayCard(Card card) {
        checkBtn.setEnabled(true);
        questionArea.setText(card.getQuestion());

        String status = card.isNew() ? "НОВОЕ" : "LVL " + card.getLevel();
        infoLabel.setText(String.format("[%s] %s", card.getCategory(), status));

        // Покраска плашки статуса
        infoLabel.setOpaque(true);
        if (card.isNew()) infoLabel.setBackground(config.ThemeColors.STATUS_NEW);
        else if (card.getLevel() == 0) infoLabel.setBackground(config.ThemeColors.STATUS_ERROR);
        else if (card.getLevel() < 8) infoLabel.setBackground(config.ThemeColors.STATUS_LEARNING);
        else infoLabel.setBackground(config.ThemeColors.STATUS_MASTER);
        infoLabel.setForeground(Color.WHITE);

        ThemeManager.apply(this); // Обновляем цвета фона
    }

    public void showAnswer(String answer) {
        answerArea.setText(answer);
        answerArea.setVisible(true);
        inputArea.setEditable(false);
    }

    public void setFeedback(boolean passed, Color inputBgColor, String message) {
        inputArea.setBackground(inputBgColor);
        feedbackLabel.setText(message);

        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        buttonPanel.revalidate(); buttonPanel.repaint();

        (passed ? yesBtn : noBtn).requestFocusInWindow();
    }

    // --- ПРИВАТНЫЕ МЕТОДЫ ПОСТРОЕНИЯ ---

    private void setupListeners() {
        checkBtn.addActionListener(e -> controller.onCheckAction());
        yesBtn.addActionListener(e -> controller.onSubmitCorrect());
        noBtn.addActionListener(e -> controller.onSubmitError());
        modeBtn.addActionListener(e -> controller.onToggleMode());

        categoryBox.addActionListener(e ->
                controller.onCategorySelected((String) categoryBox.getSelectedItem())
        );
    }

    private void setupKeys() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "enter");
        am.put("enter", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.onCheckAction(); } });

        im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
        am.put("right", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.onSubmitCorrect(); } });

        im.put(KeyStroke.getKeyStroke("LEFT"), "left");
        am.put("left", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.onSubmitError(); } });
    }

    private void initUI(StudyService service) {
        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        categoryBox = new JComboBox<>();
        categoryBox.setPreferredSize(new Dimension(250, 30));
        categoryBox.addItem("ВСЕ ТЕМЫ");
        for (String c : service.getAvailableCategories()) categoryBox.addItem(c);

        modeBtn = UIFactory.createButton("Mode: SMART", null);
        modeBtn.setPreferredSize(new Dimension(160, 30));

        controls.add(new JLabel("Тема: "));
        controls.add(categoryBox);
        controls.add(modeBtn);

        infoLabel = new JLabel("...", SwingConstants.CENTER);
        infoLabel.setOpaque(true);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        topContainer.add(controls, BorderLayout.NORTH);
        topContainer.add(infoLabel, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        questionArea = UIFactory.createArea(false);
        centerPanel.add(new JScrollPane(questionArea));

        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.add(UIFactory.createLabel("Ваш ответ:"), BorderLayout.NORTH);
        inputArea = UIFactory.createArea(true);
        inputContainer.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        centerPanel.add(inputContainer);

        JPanel answerContainer = new JPanel(new BorderLayout());
        feedbackLabel = UIFactory.createLabel(" ");
        answerContainer.add(feedbackLabel, BorderLayout.NORTH);
        answerArea = UIFactory.createArea(false);
        answerArea.setVisible(false);
        answerContainer.add(new JScrollPane(answerArea), BorderLayout.CENTER);
        centerPanel.add(answerContainer);
        add(centerPanel, BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout());
        checkBtn = UIFactory.createButton("Проверить [Enter]", null);
        yesBtn = UIFactory.createButton("ВЕРНО [→]", null);
        noBtn = UIFactory.createButton("ОШИБКА [←]", null);
        buttonPanel.add(checkBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}