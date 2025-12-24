import javax.swing.*;
import javax.swing.border.EmptyBorder;
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

    // UI Components
    private JTextArea qArea, aArea, inputArea;
    private JLabel infoLabel, feedbackLabel;
    private JButton checkBtn, yesBtn, noBtn, modeBtn, themeBtn;
    private JComboBox<String> topicSelector; // Выбор темы
    private JTabbedPane tabs;

    // Панели вкладок
    private JPanel studyPanel, statsPanel, historyPanel;
    private JPanel buttonPanel; // Нижняя панель с кнопками ответа

    // Таблицы и Поиск
    private JTable globalStatsTable, categoryStatsTable, historyTable;
    private JTextField historySearchField;

    public MainFrame() {
        AppLogger.info("Запуск GUI MainFrame...");

        deckManager = new DeckManager();
        historyManager = new HistoryManager();

        initUI();

        keyController = new KeyController(this);
        keyController.setupKeyBindings();

        // Применяем тему при старте
        applyTheme();

        loadNextCard();
    }

    public boolean isAnswerRevealed() { return isAnswerRevealed; }

    private void initUI() {
        setTitle("Java Anki: Master Edition");
        setSize(1100, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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

        add(tabs);
    }

    // --- 1. ПАНЕЛЬ ОБУЧЕНИЯ ---
    private JPanel createStudyPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // TOP PANEL: Тема + Фильтр + Режим
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        themeBtn = new JButton("☀/☾");
        themeBtn.setToolTipText("Сменить тему (Светлая/Темная)");
        themeBtn.addActionListener(e -> toggleTheme());

        // Выбор темы
        topicSelector = new JComboBox<>();
        setupTopicSelector(); // Заполнение данными
        topicSelector.addActionListener(e -> {
            String selected = (String) topicSelector.getSelectedItem();
            if (selected != null) {
                deckManager.setCategoryFilter(selected);
                loadNextCard();
                this.requestFocusInWindow(); // Вернуть фокус для клавиш
            }
        });

        modeBtn = new JButton("Режим: УМНЫЙ (M)");
        modeBtn.addActionListener(e -> toggleMode());

        topPanel.add(themeBtn);
        topPanel.add(new JLabel(" Тема: "));
        topPanel.add(topicSelector);
        topPanel.add(modeBtn);

        // Info Label
        infoLabel = new JLabel("Загрузка...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoLabel.setBorder(new EmptyBorder(5,5,5,5));
        infoLabel.setOpaque(true);

        JPanel northContainer = new JPanel(new BorderLayout());
        northContainer.add(topPanel, BorderLayout.NORTH);
        northContainer.add(infoLabel, BorderLayout.SOUTH);

        mainPanel.add(northContainer, BorderLayout.NORTH);

        // CENTER: Вопрос / Ответ
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

        // BOTTOM: Кнопки
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

        Vector<String> categories = new Vector<>();
        categories.add("ВСЕ ТЕМЫ");
        categories.addAll(deckManager.getCategories());

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(categories);
        topicSelector.setModel(model);

        if (currentSelection != null && categories.contains(currentSelection)) {
            topicSelector.setSelectedItem(currentSelection);
        } else {
            topicSelector.setSelectedIndex(0);
        }
    }

    // --- 2. ПАНЕЛЬ СТАТИСТИКИ ---
    private JPanel createStatsPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1, 10, 10));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(10,10,10,10));

        globalStatsTable = new JTable(new DefaultTableModel(new String[]{"Статус", "Описание", "Кол-во"}, 0));
        globalStatsTable.setRowHeight(25); globalStatsTable.setFont(new Font("Arial", Font.PLAIN, 14));

        categoryStatsTable = new JTable(new DefaultTableModel(new String[]{"Категория", "Всего", "Новые", "Ошибки", "В процессе", "Мастер"}, 0));
        categoryStatsTable.setRowHeight(25); categoryStatsTable.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel p1 = new JPanel(new BorderLayout()); p1.add(new JScrollPane(globalStatsTable));
        JPanel p2 = new JPanel(new BorderLayout()); p2.add(new JScrollPane(categoryStatsTable));

        // Заголовки для таблиц
        p1.setBorder(BorderFactory.createTitledBorder("Общая сводка"));
        p2.setBorder(BorderFactory.createTitledBorder("Детализация по темам"));

        p.add(p1); p.add(p2);

        JPanel container = new JPanel(new BorderLayout());
        container.add(p, BorderLayout.CENTER);

        // Кнопки управления
        JPanel bottomBtnPanel = new JPanel();

        JButton refreshBtn = new JButton("Обновить статистику");
        refreshBtn.addActionListener(e -> updateStats());

        JButton importBtn = new JButton("Импорт из import.txt");
        importBtn.addActionListener(e -> {
            File importFile = new File("import.txt");
            if(importFile.exists()) {
                Set<String> existing = deckManager.getExistingNormalizedQuestions();
                new DeckDistributor().distribute(importFile, existing);

                JOptionPane.showMessageDialog(this, "Импорт завершен!");

                // Перезагрузка всего
                deckManager = new DeckManager();
                setupTopicSelector(); // Обновляем список тем
                loadNextCard();
                updateStats();
            } else {
                JOptionPane.showMessageDialog(this, "Файл import.txt не найден.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomBtnPanel.add(refreshBtn);
        bottomBtnPanel.add(importBtn);

        container.add(bottomBtnPanel, BorderLayout.SOUTH);

        return container;
    }

    // --- 3. ПАНЕЛЬ ИСТОРИИ ---
    private JPanel createHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10,10,10,10));

        // Поиск
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.add(new JLabel("Поиск: "), BorderLayout.WEST);
        historySearchField = new JTextField();
        historySearchField.addActionListener(e -> updateHistory(historySearchField.getText()));
        searchPanel.add(historySearchField, BorderLayout.CENTER);
        JButton searchBtn = new JButton("Найти");
        searchBtn.addActionListener(e -> updateHistory(historySearchField.getText()));
        searchPanel.add(searchBtn, BorderLayout.EAST);

        p.add(searchPanel, BorderLayout.NORTH);

        // Таблица
        String[] columns = {"Дата", "Вопрос", "Ваш Ответ", "Результат"};
        historyTable = new JTable(new DefaultTableModel(columns, 0));
        historyTable.setRowHeight(25);
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(130);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(3).setMaxWidth(80);

        p.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Сбросить фильтр");
        refreshBtn.addActionListener(e -> {
            historySearchField.setText("");
            updateHistory("");
        });
        p.add(refreshBtn, BorderLayout.SOUTH);

        return p;
    }

    // --- ТЕМЫ (ThemeManager) ---

    private void toggleTheme() {
        ThemeManager.toggle();
        applyTheme();
    }

    private void applyTheme() {
        Color bg = ThemeManager.getBackground();
        Color panel = ThemeManager.getPanelBackground();
        Color text = ThemeManager.getText();
        Color input = ThemeManager.getInputBackground();

        getContentPane().setBackground(bg);

        // Вкладки
        studyPanel.setBackground(panel);
        statsPanel.setBackground(panel);
        historyPanel.setBackground(panel);
        buttonPanel.setBackground(panel);

        // Компоненты
        qArea.setBackground(ThemeManager.getAreaBackground());
        qArea.setForeground(text);

        aArea.setBackground(ThemeManager.getAreaBackground());
        aArea.setForeground(text);

        inputArea.setBackground(input);
        inputArea.setForeground(text);
        inputArea.setCaretColor(text);

        // Таблицы
        updateTableTheme(globalStatsTable);
        updateTableTheme(categoryStatsTable);
        updateTableTheme(historyTable);

        // Info Label
        infoLabel.setForeground(Color.BLACK); // Всегда черный, так как фон цветной

        SwingUtilities.updateComponentTreeUI(this);
    }

    private void updateTableTheme(JTable table) {
        if(table == null) return;
        table.setBackground(ThemeManager.getInputBackground());
        table.setForeground(ThemeManager.getText());
        table.getTableHeader().setBackground(ThemeManager.getPanelBackground());
        table.getTableHeader().setForeground(ThemeManager.getText());
        if(table.getParent() instanceof JViewport) {
            ((JViewport)table.getParent()).setBackground(ThemeManager.getPanelBackground());
        }
    }

    // --- ЛОГИКА ---

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

        inputArea.setText(""); inputArea.setEditable(true);
        inputArea.setBackground(ThemeManager.getInputBackground());

        aArea.setText(""); aArea.setVisible(false); feedbackLabel.setText(" ");

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
                c = new Color(150, 200, 255); statusText = "НОВОЕ";
            } else if (currentCard.level == 0) {
                c = new Color(255, 150, 150); statusText = "ОШИБКА";
            } else if (currentCard.level < 5) {
                c = new Color(255, 220, 150); statusText = "УЧУ (" + currentCard.level + ")";
            } else {
                c = new Color(150, 255, 150); statusText = "МАСТЕР (" + currentCard.level + ")";
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

        double similarity = SmartChecker.check(inputArea.getText(), currentCard.answer);
        boolean passed = similarity > 65.0;

        aArea.setText(currentCard.answer); aArea.setVisible(true); inputArea.setEditable(false);
        feedbackLabel.setText((passed ? "Отлично!" : "Неточно") + " (" + (int)similarity + "%)");
        feedbackLabel.setForeground(passed ? ThemeManager.getSuccessColor() : ThemeManager.getErrorColor());

        // Подсветка
        Color successBg = ThemeManager.isDark() ? new Color(30, 60, 30) : new Color(220, 255, 220);
        Color errorBg = ThemeManager.isDark() ? new Color(60, 30, 30) : new Color(255, 220, 220);
        inputArea.setBackground(passed ? successBg : errorBg);

        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        (passed ? yesBtn : noBtn).requestFocusInWindow();
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    public void submitResult(boolean correct) {
        historyManager.saveEntry(currentCard.question, inputArea.getText(), correct);
        deckManager.processAnswer(currentCard, correct);
        loadNextCard();
    }

    // --- ОБНОВЛЕНИЕ ТАБЛИЦ ---

    private void updateStats() {
        DefaultTableModel gm = (DefaultTableModel) globalStatsTable.getModel(); gm.setRowCount(0);
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
        gm.addRow(new Object[]{"ИТОГО", "", all.size()});

        DefaultTableModel cm = (DefaultTableModel) categoryStatsTable.getModel(); cm.setRowCount(0);
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
    }

    private JTextArea createArea(int size, boolean editable) {
        JTextArea t = new JTextArea();
        t.setFont(new Font("Arial", Font.PLAIN, size));
        t.setEditable(editable); t.setLineWrap(true); t.setWrapStyleWord(true);
        return t;
    }

    private JButton createButton(String txt, java.awt.event.ActionListener l) {
        JButton b = new JButton(txt); b.addActionListener(l); b.setPreferredSize(new Dimension(220,45)); return b;
    }
}