import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder; // Добавлено
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class MainFrame extends JFrame {
    private DeckManager deckManager;
    private HistoryManager historyManager;
    private KeyController keyController;

    private Card currentCard;
    private boolean isShuffleMode = false;
    private boolean isAnswerRevealed = false;

    // --- UI Элементы ---
    private CustomTitleBar titleBar; // Ссылка на наш новый бар
    private JTextArea qArea, aArea, inputArea;
    private JLabel infoLabel, feedbackLabel;
    private JButton checkBtn, yesBtn, noBtn, modeBtn, themeBtn;
    private JComboBox<String> topicSelector;
    private JTabbedPane tabs;

    // Панели
    private JPanel studyPanel, statsPanel, historyPanel, buttonPanel;

    // Таблицы и Поиск
    private JTable globalStatsTable, categoryStatsTable, historyTable;
    private JTextField historySearchField;

    public MainFrame() {
        AppLogger.info("Запуск GUI MainFrame...");

        // Настраиваем системные цвета
        ThemeManager.updateUIManager();

        // Инициализация логики
        deckManager = new DeckManager();
        historyManager = new HistoryManager();

        // Построение интерфейса
        initUI();

        // Подключение клавиатуры
        keyController = new KeyController(this);
        keyController.setupKeyBindings();

        // Первичная покраска и загрузка
        applyTheme();
        loadNextCard();
    }

    public boolean isAnswerRevealed() { return isAnswerRevealed; }

    private void initUI() {
        // --- ИЗМЕНЕНИЯ ЗДЕСЬ ---
        // 1. Убираем стандартную рамку
        setUndecorated(true);

        setTitle("Java Anki: Master Edition");
        setSize(1100, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Используем BorderLayout для главного окна
        setLayout(new BorderLayout());

        // 2. Добавляем кастомный заголовок наверх
        titleBar = new CustomTitleBar(this);
        add(titleBar, BorderLayout.NORTH);

        tabs = new JTabbedPane();
        studyPanel = createStudyPanel();
        statsPanel = createStatsPanel();
        historyPanel = createHistoryPanel();

        tabs.addTab("Обучение", studyPanel);
        tabs.addTab("Статистика", statsPanel);
        tabs.addTab("История", historyPanel);

        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            if (idx == 1) updateStats();
            if (idx == 2) updateHistory("");
        });

        // 3. Табы добавляем в ЦЕНТР (под заголовком)
        add(tabs, BorderLayout.CENTER);
    }

    // ==========================================
    // 1. ПАНЕЛЬ ОБУЧЕНИЯ (STUDY PANEL)
    // ==========================================
    private JPanel createStudyPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // --- ВЕРХНЯЯ ПАНЕЛЬ ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        themeBtn = new JButton("☀/☾");
        themeBtn.setToolTipText("Сменить тему оформления");
        themeBtn.addActionListener(e -> toggleTheme());

        topicSelector = new JComboBox<>();
        setupTopicSelector();
        topicSelector.addActionListener(e -> {
            String selected = (String) topicSelector.getSelectedItem();
            if (selected != null) {
                deckManager.setCategoryFilter(selected);
                loadNextCard();
                this.requestFocusInWindow();
            }
        });

        modeBtn = new JButton("Режим: УМНЫЙ (M)");
        modeBtn.addActionListener(e -> toggleMode());

        topPanel.add(themeBtn);
        topPanel.add(new JLabel(" Тема: "));
        topPanel.add(topicSelector);
        topPanel.add(modeBtn);

        infoLabel = new JLabel("Загрузка...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoLabel.setBorder(new EmptyBorder(5,5,5,5));
        infoLabel.setOpaque(true);

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(infoLabel, BorderLayout.SOUTH);

        mainPanel.add(northContainer, BorderLayout.NORTH);

        // --- ЦЕНТРАЛЬНАЯ ЧАСТЬ ---
        JPanel centerGrid = new JPanel(new GridLayout(3,1,5,5));
        centerGrid.setBorder(new EmptyBorder(10,10,10,10));

        qArea = createArea(18, false);
        centerGrid.add(new JScrollPane(qArea));

        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.add(new JLabel("Ваш ответ:"), BorderLayout.NORTH);
        inputArea = createArea(16, true);
        inputContainer.add(new JScrollPane(inputArea));
        centerGrid.add(inputContainer);

        JPanel answerContainer = new JPanel(new BorderLayout());
        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(new Font("Arial", Font.BOLD, 14));
        answerContainer.add(feedbackLabel, BorderLayout.NORTH);
        aArea = createArea(14, false);
        aArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        answerContainer.add(new JScrollPane(aArea));
        centerGrid.add(answerContainer);

        mainPanel.add(centerGrid, BorderLayout.CENTER);

        // --- НИЖНЯЯ ПАНЕЛЬ ---
        buttonPanel = new JPanel();
        checkBtn = createButton("Проверить [Enter]", e -> checkAnswer());
        noBtn = createButton("ОШИБКА [←]", e -> submitResult(false));
        yesBtn = createButton("ВЕРНО [→]", e -> submitResult(true));

        buttonPanel.add(checkBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    private void setupTopicSelector() {
        String currentSelection = (String) topicSelector.getSelectedItem();
        Vector<String> options = deckManager.getFilterOptions();
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(options);
        topicSelector.setModel(model);
        if (currentSelection != null && options.contains(currentSelection)) {
            topicSelector.setSelectedItem(currentSelection);
        } else {
            topicSelector.setSelectedIndex(0);
        }
    }

    // ==========================================
    // 2. ПАНЕЛЬ СТАТИСТИКИ
    // ==========================================
    private JPanel createStatsPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1, 10, 10));
        p.setBorder(new EmptyBorder(10,10,10,10));

        globalStatsTable = new JTable(new DefaultTableModel(new String[]{"Статус", "Описание", "Кол-во"}, 0));
        globalStatsTable.setRowHeight(25); globalStatsTable.setFont(new Font("Arial", Font.PLAIN, 14));

        categoryStatsTable = new JTable(new DefaultTableModel(new String[]{"Категория", "Всего", "Новые", "Ошибки", "В процессе", "Мастер"}, 0));
        categoryStatsTable.setRowHeight(25); categoryStatsTable.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel p1 = new JPanel(new BorderLayout());
        p1.add(new JScrollPane(globalStatsTable));
        p1.setBorder(BorderFactory.createTitledBorder("Общая сводка"));

        JPanel p2 = new JPanel(new BorderLayout());
        p2.add(new JScrollPane(categoryStatsTable));
        p2.setBorder(BorderFactory.createTitledBorder("Детализация по темам"));

        p.add(p1); p.add(p2);

        JPanel container = new JPanel(new BorderLayout());
        container.add(p, BorderLayout.CENTER);

        JPanel bottomBtnPanel = new JPanel();
        JButton refreshBtn = new JButton("Обновить статистику");
        refreshBtn.addActionListener(e -> updateStats());
        JButton importBtn = new JButton("Импорт из import.txt");
        importBtn.addActionListener(e -> performImport());

        bottomBtnPanel.add(refreshBtn);
        bottomBtnPanel.add(importBtn);
        container.add(bottomBtnPanel, BorderLayout.SOUTH);

        return container;
    }

    // ==========================================
    // 3. ПАНЕЛЬ ИСТОРИИ
    // ==========================================
    private JPanel createHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10,10,10,10));

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.add(new JLabel("Поиск: "), BorderLayout.WEST);

        historySearchField = new JTextField();
        historySearchField.addActionListener(e -> updateHistory(historySearchField.getText()));
        searchPanel.add(historySearchField, BorderLayout.CENTER);

        JButton searchBtn = new JButton("Найти");
        searchBtn.addActionListener(e -> updateHistory(historySearchField.getText()));
        searchPanel.add(searchBtn, BorderLayout.EAST);

        p.add(searchPanel, BorderLayout.NORTH);

        String[] columns = {"Дата", "Вопрос", "Ваш Ответ", "Результат"};
        historyTable = new JTable(new DefaultTableModel(columns, 0));
        historyTable.setRowHeight(25);
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(130);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(3).setMaxWidth(80);

        p.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JButton resetBtn = new JButton("Сбросить фильтр / Обновить");
        resetBtn.addActionListener(e -> {
            historySearchField.setText("");
            updateHistory("");
        });
        p.add(resetBtn, BorderLayout.SOUTH);

        return p;
    }

    // ==========================================
    // ЛОГИКА
    // ==========================================

    private void toggleTheme() {
        ThemeManager.toggle();
        ThemeManager.updateUIManager();
        applyTheme();
    }

    private void applyTheme() {
        // 1. Рекурсивно красим все компоненты
        ThemeManager.applyRecursive(this);

        // 2. Дополнительно обновляем TitleBar (чтобы кнопки перекрасились)
        if (titleBar != null) {
            titleBar.updateThemeColors();
        }

        // 3. Рисуем рамку вокруг всего окна (т.к. системной рамки нет)
        // Если тема темная - рамка темно-серая, если светлая - серая
        this.getRootPane().setBorder(new LineBorder(ThemeManager.getBorderColor(), 1));

        // 4. Специфичная настройка текстовых полей
        Color txt = ThemeManager.getText();
        Color inputBg = ThemeManager.getInputBackground();

        if (qArea != null) {
            qArea.setBackground(ThemeManager.isDark() ? new Color(50, 50, 50) : new Color(245, 250, 255));
            qArea.setForeground(txt);
        }
        if (aArea != null) {
            aArea.setBackground(ThemeManager.isDark() ? new Color(50, 50, 50) : new Color(230, 255, 230));
            aArea.setForeground(txt);
        }
        if (inputArea != null) {
            inputArea.setBackground(inputBg);
            inputArea.setForeground(txt);
            inputArea.setCaretColor(txt);
        }
        if (infoLabel != null) infoLabel.setForeground(Color.BLACK);

        //SwingUtilities.updateComponentTreeUI(this);
    }

    private void performImport() {
        File importFile = new File("import.txt");
        if(importFile.exists()) {
            Set<String> existing = deckManager.getExistingNormalizedQuestions();
            new DeckDistributor().distribute(importFile, existing);
            JDialog dialog = new JDialog(this, "Сообщение", true);
            dialog.setUndecorated(true); // Убираем белый заголовок системы

            JPanel content = new JPanel(new BorderLayout());
            content.setBorder(new LineBorder(ThemeManager.getBorderColor(), 1));

            JLabel msg = new JLabel("Импорт завершен! Дубликаты пропущены.", SwingConstants.CENTER);
            msg.setBorder(new EmptyBorder(20, 20, 20, 20));

            JButton okBtn = new JButton("OK");
            okBtn.addActionListener(e -> dialog.dispose());
            JPanel btnPanel = new JPanel();
            btnPanel.add(okBtn);
            btnPanel.setBorder(new EmptyBorder(0,0,10,0));

            content.add(msg, BorderLayout.CENTER);
            content.add(btnPanel, BorderLayout.SOUTH);

            dialog.add(content);
            dialog.pack();
            dialog.setLocationRelativeTo(this);

            // Красим наш диалог
            ThemeManager.applyRecursive(dialog);

            dialog.setVisible(true);
            deckManager = new DeckManager();
            setupTopicSelector();
            loadNextCard();
            updateStats();
            applyTheme();
        } else {
            JOptionPane.showMessageDialog(this, "Файл import.txt не найден.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void toggleMode() {
        isShuffleMode = !isShuffleMode;
        modeBtn.setText(isShuffleMode ? "Режим: SHUFFLE (M)" : "Режим: УМНЫЙ (M)");
        loadNextCard();
    }

    private void loadNextCard() {
        currentCard = deckManager.getNext(isShuffleMode);
        isAnswerRevealed = false;

        buttonPanel.removeAll();
        buttonPanel.add(checkBtn);
        ThemeManager.applyRecursive(buttonPanel);
        inputArea.setText("");
        inputArea.setEditable(true);
        inputArea.setBackground(ThemeManager.getInputBackground());

        aArea.setText("");
        aArea.setVisible(false);
        feedbackLabel.setText(" ");

        if(currentCard == null) {
            infoLabel.setText("В этой теме нет вопросов или они закончились.");
            infoLabel.setBackground(Color.GRAY);
            qArea.setText("");
            checkBtn.setEnabled(false);
        } else {
            checkBtn.setEnabled(true);
            Color c;
            String statusText;

            if (currentCard.isNew) {
                c = new Color(24, 98, 181); statusText = "НОВОЕ";
            } else if (currentCard.level == 0) {
                c = new Color(166, 3, 3); statusText = "ОШИБКА";
            } else if (currentCard.level < 5) {
                c = new Color(218, 147, 13); statusText = "УЧУ (" + currentCard.level + ")";
            } else {
                c = new Color(7, 117, 7); statusText = "МАСТЕР (" + currentCard.level + ")";
            }

            String prefix = isShuffleMode ? "[SHUFFLE] " : "[SMART] ";
            infoLabel.setText(String.format("%s [%s] %s", prefix, currentCard.category, statusText));
            infoLabel.setBackground(c);
            infoLabel.setForeground(Color.BLACK);

            qArea.setText(currentCard.question);
            inputArea.requestFocusInWindow();
        }
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    public void checkAnswer() {
        if(currentCard == null) return;
        isAnswerRevealed = true;

        String userAns = inputArea.getText();
        double similarity = SmartChecker.check(userAns, currentCard.answer);
        boolean passed = similarity > 65.0;

        aArea.setText(currentCard.answer);
        aArea.setVisible(true);
        inputArea.setEditable(false);

        feedbackLabel.setText((passed ? "Отлично!" : "Неточно") + " (" + (int)similarity + "%)");
        feedbackLabel.setForeground(passed ? ThemeManager.getSuccessColor() : ThemeManager.getErrorColor());

        Color successBg = ThemeManager.isDark() ? new Color(40, 80, 40) : new Color(220, 255, 220);
        Color errorBg = ThemeManager.isDark() ? new Color(80, 40, 40) : new Color(255, 220, 220);
        inputArea.setBackground(passed ? successBg : errorBg);

        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        ThemeManager.applyRecursive(buttonPanel);
        (passed ? yesBtn : noBtn).requestFocusInWindow();
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    public void submitResult(boolean correct) {
        historyManager.saveEntry(currentCard.question, inputArea.getText(), correct);
        deckManager.processAnswer(currentCard, correct);
        loadNextCard();
    }

    private void updateStats() {
        DefaultTableModel gm = (DefaultTableModel) globalStatsTable.getModel();
        gm.setRowCount(0);

        List<Card> all = deckManager.getAllCards();
        if (all == null) return;

        long cntNew = all.stream().filter(c -> c.isNew).count();
        long cntErr = all.stream().filter(c -> !c.isNew && c.level == 0).count();
        long cntMid = all.stream().filter(c -> !c.isNew && c.level >= 1 && c.level <= 7).count();
        long cntHigh = all.stream().filter(c -> !c.isNew && c.level >= 8).count();

        gm.addRow(new Object[]{"НОВЫЕ", "Ни разу не отвечали", cntNew});
        gm.addRow(new Object[]{"ОШИБКИ", "Нужно повторить (Level 0)", cntErr});
        gm.addRow(new Object[]{"В ПРОЦЕССЕ", "Учу / Закрепляю (Level 1-7)", cntMid});
        gm.addRow(new Object[]{"МАСТЕР", "Отлично знаю (Level 8+)", cntHigh});
        gm.addRow(new Object[]{"ИТОГО", "Всего карточек", all.size()});

        DefaultTableModel cm = (DefaultTableModel) categoryStatsTable.getModel();
        cm.setRowCount(0);
        Map<String, List<Card>> catStats = deckManager.getCardsByCategory();

        for(String cat : catStats.keySet()) {
            List<Card> list = catStats.get(cat);
            long t = list.size();
            long n = list.stream().filter(c -> c.isNew).count();
            long e = list.stream().filter(c -> !c.isNew && c.level == 0).count();
            long m = list.stream().filter(c -> !c.isNew && c.level >= 1 && c.level <= 7).count();
            long h = list.stream().filter(c -> !c.isNew && c.level >= 8).count();
            cm.addRow(new Object[]{cat, t, n, e, m, h});
        }
        applyTheme();
    }

    private void updateHistory(String filter) {
        DefaultTableModel model = (DefaultTableModel) historyTable.getModel();
        model.setRowCount(0);
        List<HistoryManager.HistoryEntry> entries = historyManager.loadHistory();
        String searchLower = filter.toLowerCase().trim();

        for (HistoryManager.HistoryEntry entry : entries) {
            boolean matches = searchLower.isEmpty() ||
                    entry.question.toLowerCase().contains(searchLower) ||
                    entry.userAnswer.toLowerCase().contains(searchLower) ||
                    entry.result.toLowerCase().contains(searchLower) ||
                    entry.date.contains(searchLower);
            if (matches) {
                model.addRow(new Object[]{entry.date, entry.question, entry.userAnswer, entry.result});
            }
        }
        applyTheme();
    }

    private JTextArea createArea(int size, boolean editable) {
        JTextArea t = new JTextArea();
        t.setFont(new Font("Arial", Font.PLAIN, size));
        t.setEditable(editable);
        t.setLineWrap(true);
        t.setWrapStyleWord(true);
        return t;
    }

    private JButton createButton(String txt, java.awt.event.ActionListener l) {
        JButton b = new JButton(txt);
        b.addActionListener(l);
        b.setPreferredSize(new Dimension(220,45));
        return b;
    }
}