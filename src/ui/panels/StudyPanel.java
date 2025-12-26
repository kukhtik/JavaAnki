package ui.panels;

import model.Card;
import service.StudyService;
import ui.Theme;
import ui.components.UIFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StudyPanel extends JPanel {
    private final StudyService service;

    private JComboBox<String> categoryBox; // NEW: Выбор темы
    private JTextArea questionArea, answerArea, inputArea;
    private JLabel infoLabel, feedbackLabel;
    private JPanel buttonPanel;
    private JButton checkBtn, yesBtn, noBtn;

    private boolean isShuffleMode = false;
    private boolean isAnswerRevealed = false; // Нужно для логики клавиш

    public StudyPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());
        initUI();
        setupKeys(); // NEW: Горячие клавиши
        loadNextCard();
    }

    private void initUI() {
        // --- ВЕРХ: Настройки ---
        JPanel topContainer = new JPanel(new BorderLayout());

        // Панель управления (Тема, Режим)
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Селектор категорий
        categoryBox = new JComboBox<>();
        categoryBox.addActionListener(e -> {
            String cat = (String) categoryBox.getSelectedItem();
            if (cat != null) {
                service.setFilter(cat);
                loadNextCard();
                inputArea.requestFocusInWindow();
            }
        });
        updateCategories(); // Заполнить список

        JButton modeBtn = UIFactory.createButton("Mode: SMART", null); // Упрощенно
        modeBtn.setPreferredSize(new Dimension(120, 30));
        modeBtn.addActionListener(e -> {
            isShuffleMode = !isShuffleMode;
            modeBtn.setText(isShuffleMode ? "Mode: SHUFFLE" : "Mode: SMART");
            loadNextCard();
        });

        controls.add(new JLabel("Тема: "));
        controls.add(categoryBox);
        controls.add(modeBtn);

        infoLabel = new JLabel("...", SwingConstants.CENTER);
        infoLabel.setOpaque(true);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

        topContainer.add(controls, BorderLayout.NORTH);
        topContainer.add(infoLabel, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // --- ЦЕНТР (тот же код, что был раньше) ---
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

        // --- НИЗ ---
        buttonPanel = new JPanel(new FlowLayout());
        checkBtn = UIFactory.createButton("Проверить [Enter]", e -> checkAnswer());
        yesBtn = UIFactory.createButton("ВЕРНО [→]", e -> submit(true));
        noBtn = UIFactory.createButton("ОШИБКА [←]", e -> submit(false));
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void updateCategories() {
        categoryBox.removeAllItems();
        categoryBox.addItem("ВСЕ ТЕМЫ");
        for (String cat : service.getAvailableCategories()) {
            categoryBox.addItem(cat);
        }
    }

    private void setupKeys() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("ENTER"), "enter");
        am.put("enter", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!isAnswerRevealed) checkAnswer();
            }
        });

        im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isAnswerRevealed) submit(true);
            }
        });

        im.put(KeyStroke.getKeyStroke("LEFT"), "left");
        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (isAnswerRevealed) submit(false);
            }
        });
    }

    // Методы loadNextCard, checkAnswer, submit те же, но добавляем обновление флага
    private void loadNextCard() {
        isAnswerRevealed = false;
        // ... (остальной код из предыдущего ответа)
        Card card = service.nextCard(isShuffleMode);
        buttonPanel.removeAll();
        buttonPanel.add(checkBtn);
        inputArea.setText("");
        inputArea.setEditable(true);
        inputArea.setBackground(Theme.getInputBg());
        answerArea.setVisible(false);
        feedbackLabel.setText(" ");

        if (card == null) {
            questionArea.setText("Нет карт.");
            checkBtn.setEnabled(false);
            return;
        }
        checkBtn.setEnabled(true);
        questionArea.setText(card.getQuestion());

        String status = card.isNew() ? "НОВОЕ" : "LVL " + card.getLevel();
        infoLabel.setText(String.format("[%s] %s", card.getCategory(), status));

        // Красим плашку статуса
        if (card.isNew()) infoLabel.setBackground(config.ThemeColors.STATUS_NEW);
        else if (card.getLevel() == 0) infoLabel.setBackground(config.ThemeColors.STATUS_ERROR);
        else if (card.getLevel() < 8) infoLabel.setBackground(config.ThemeColors.STATUS_LEARNING);
        else infoLabel.setBackground(config.ThemeColors.STATUS_MASTER);
        infoLabel.setForeground(Color.WHITE);

        Theme.apply(this);
        // Важно перекрасить InfoLabel обратно, так как apply сбросит его в дефолт
        buttonPanel.revalidate(); buttonPanel.repaint();
        inputArea.requestFocusInWindow();
    }

    private void checkAnswer() {
        if (service.getCurrentCard() == null) return;
        isAnswerRevealed = true;

        String user = inputArea.getText();
        var res = service.checkAnswer(user);

        answerArea.setText(service.getCurrentCard().getAnswer());
        answerArea.setVisible(true);
        inputArea.setEditable(false);

        // Исправленный блок с цветами
        if (Theme.isDark()) {
            inputArea.setBackground(res.isPassed() ? new Color(40, 80, 40) : new Color(80, 40, 40));
        } else {
            inputArea.setBackground(res.isPassed() ? new Color(200, 255, 200) : new Color(255, 200, 200));
        }

        feedbackLabel.setText(res.isPassed() ? "OK" : "TYPO");

        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        buttonPanel.revalidate(); buttonPanel.repaint();
        (res.isPassed() ? yesBtn : noBtn).requestFocusInWindow();
    }

    private void submit(boolean result) {
        service.submitResult(inputArea.getText(), result);
        loadNextCard();
    }
}