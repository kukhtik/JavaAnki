import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainFrame extends JFrame {
    private DeckManager deckManager;
    private HistoryManager historyManager;
    private KeyController keyController;

    private Card currentCard;
    private boolean isShuffleMode = false;
    private boolean isAnswerRevealed = false;
    private String lastUserAnswer = "";

    // Элементы UI
    private JTextArea qArea, aArea, inputArea;
    private JLabel infoLabel, feedbackLabel;
    private JButton checkBtn, yesBtn, noBtn, modeBtn;
    private JPanel buttonPanel;

    private JTable globalStatsTable;
    private JTable categoryStatsTable;
    private JTable historyTable;
    private JTextField historySearchField; // Поле поиска

    public MainFrame() {
        AppLogger.info("Запуск GUI MainFrame...");

        deckManager = new DeckManager();
        historyManager = new HistoryManager();

        initUI();

        keyController = new KeyController(this);
        keyController.setupKeyBindings();

        loadNextCard();
    }

    public boolean isAnswerRevealed() { return isAnswerRevealed; }

    private void initUI() {
        setTitle("Java Anki: Master Edition");
        setSize(1100, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Обучение", createStudyPanel());
        tabs.addTab("Статистика", createStatsPanel());
        tabs.addTab("История", createHistoryPanel());

        tabs.addChangeListener(e -> {
            int idx = tabs.getSelectedIndex();
            if (idx == 1) updateStats();
            if (idx == 2) updateHistory(""); // При входе в историю сбрасываем фильтр (или можно оставить)
        });

        add(tabs);
    }

    // --- 1. ПАНЕЛЬ ОБУЧЕНИЯ ---
    private JPanel createStudyPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        infoLabel = new JLabel("Загрузка...", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.BOLD, 14));
        infoLabel.setBorder(new EmptyBorder(10,10,10,10));
        infoLabel.setOpaque(true);

        modeBtn = new JButton("Режим: УМНЫЙ (M)");
        modeBtn.setBackground(new Color(200, 230, 255));
        modeBtn.addActionListener(e -> toggleMode());

        topPanel.add(infoLabel, BorderLayout.CENTER);
        topPanel.add(modeBtn, BorderLayout.EAST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        JPanel centerGrid = new JPanel(new GridLayout(3,1,5,5));
        centerGrid.setBorder(new EmptyBorder(10,10,10,10));

        qArea = createArea(18, new Color(240,248,255), false);
        centerGrid.add(new JScrollPane(qArea));

        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.add(new JLabel("Ваш ответ:"), BorderLayout.NORTH);
        inputArea = createArea(16, Color.WHITE, true);
        inputContainer.add(new JScrollPane(inputArea));
        centerGrid.add(inputContainer);

        JPanel answerContainer = new JPanel(new BorderLayout());
        feedbackLabel = new JLabel(" ");
        feedbackLabel.setFont(new Font("Arial", Font.BOLD, 14));
        answerContainer.add(feedbackLabel, BorderLayout.NORTH);
        aArea = createArea(14, new Color(230,255,230), false);
        aArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        answerContainer.add(new JScrollPane(aArea));
        centerGrid.add(answerContainer);

        mainPanel.add(centerGrid, BorderLayout.CENTER);

        buttonPanel = new JPanel();
        checkBtn = createButton("Проверить [Enter]", e -> {
            AppLogger.input("UI BUTTON", "Нажата кнопка Проверить");
            checkAnswer();
        });
        noBtn = createButton("ОШИБКА [←]", e -> {
            AppLogger.input("UI BUTTON", "Нажата кнопка Ошибка");
            submitResult(false);
        });
        noBtn.setBackground(new Color(255,100,100));
        yesBtn = createButton("ВЕРНО [→]", e -> {
            AppLogger.input("UI BUTTON", "Нажата кнопка Верно");
            submitResult(true);
        });
        yesBtn.setBackground(new Color(100,200,100));

        buttonPanel.add(checkBtn);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        return mainPanel;
    }

    // --- 2. ПАНЕЛЬ СТАТИСТИКИ ---
    private JPanel createStatsPanel() {
        JPanel p = new JPanel(new GridLayout(2, 1, 10, 10));
        p.setBorder(new EmptyBorder(10,10,10,10));

        JPanel globalP = new JPanel(new BorderLayout());
        globalP.setBorder(BorderFactory.createTitledBorder("Общая сводка"));
        globalStatsTable = new JTable(new DefaultTableModel(new String[]{"Статус", "Описание", "Кол-во"}, 0));
        globalStatsTable.setRowHeight(25); globalStatsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        globalP.add(new JScrollPane(globalStatsTable), BorderLayout.CENTER);

        JPanel catP = new JPanel(new BorderLayout());
        catP.setBorder(BorderFactory.createTitledBorder("Детализация по темам"));
        categoryStatsTable = new JTable(new DefaultTableModel(new String[]{"Категория", "Всего", "Новые", "Ошибки", "В процессе", "Мастер"}, 0));
        categoryStatsTable.setRowHeight(25); categoryStatsTable.setFont(new Font("Arial", Font.PLAIN, 14));
        catP.add(new JScrollPane(categoryStatsTable), BorderLayout.CENTER);

        p.add(globalP); p.add(catP);

        JPanel container = new JPanel(new BorderLayout());
        container.add(p, BorderLayout.CENTER);

        JPanel bottomBtnPanel = new JPanel();
        JButton refreshBtn = new JButton("Обновить статистику");
        refreshBtn.addActionListener(e -> updateStats());

        JButton importBtn = new JButton("Импорт из import.txt");
        importBtn.addActionListener(e -> {
            AppLogger.input("UI BUTTON", "Запущен импорт");
            File importFile = new File("import.txt");
            if(importFile.exists()) {
                // 1. Получаем список текущих вопросов, чтобы не создавать дубликаты
                java.util.Set<String> existingQuestions = deckManager.getExistingNormalizedQuestions();

                // 2. Запускаем импорт с передачей этого списка
                new DeckDistributor().distribute(importFile, existingQuestions);

                JOptionPane.showMessageDialog(this,
                        "Процесс импорта завершен.\nПодробности (добавлено/пропущено) смотрите в консоли.",
                        "Импорт", JOptionPane.INFORMATION_MESSAGE);

                // 3. Перезагружаем программу
                deckManager = new DeckManager();
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

    // --- 3. ПАНЕЛЬ ИСТОРИИ (С ПОИСКОМ) ---
    private JPanel createHistoryPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(10,10,10,10));

        // Панель поиска сверху
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        searchPanel.add(new JLabel("Поиск: "), BorderLayout.WEST);
        historySearchField = new JTextField();
        searchPanel.add(historySearchField, BorderLayout.CENTER);

        JButton searchBtn = new JButton("Найти");
        searchBtn.addActionListener(e -> updateHistory(historySearchField.getText()));
        searchPanel.add(searchBtn, BorderLayout.EAST);

        // Поиск по Enter
        historySearchField.addActionListener(e -> updateHistory(historySearchField.getText()));

        p.add(searchPanel, BorderLayout.NORTH);

        // Таблица
        String[] columns = {"Дата", "Вопрос", "Ваш Ответ", "Результат"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);
        historyTable = new JTable(model);
        historyTable.setRowHeight(25);
        historyTable.setFont(new Font("Arial", Font.PLAIN, 12));

        historyTable.getColumnModel().getColumn(0).setPreferredWidth(130);
        historyTable.getColumnModel().getColumn(0).setMaxWidth(130);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        historyTable.getColumnModel().getColumn(3).setMaxWidth(80);

        p.add(new JScrollPane(historyTable), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Сбросить / Обновить");
        refreshBtn.addActionListener(e -> {
            historySearchField.setText("");
            updateHistory("");
        });
        p.add(refreshBtn, BorderLayout.SOUTH);

        return p;
    }

    // --- ЛОГИКА ---

    public void toggleMode() {
        isShuffleMode = !isShuffleMode;
        if (isShuffleMode) {
            modeBtn.setText("Режим: SHUFFLE (M)");
            modeBtn.setBackground(new Color(255, 200, 100));
        } else {
            modeBtn.setText("Режим: УМНЫЙ (M)");
            modeBtn.setBackground(new Color(200, 230, 255));
        }
        loadNextCard();
    }

    private void loadNextCard() {
        currentCard = deckManager.getNext(isShuffleMode);
        isAnswerRevealed = false;

        buttonPanel.removeAll();
        buttonPanel.add(checkBtn);

        inputArea.setText(""); inputArea.setEditable(true); inputArea.setBackground(Color.WHITE);
        aArea.setText(""); aArea.setVisible(false); feedbackLabel.setText(" ");
        lastUserAnswer = "";

        if(currentCard == null) {
            infoLabel.setText("Нет вопросов! Проверьте папку decks.");
            infoLabel.setBackground(Color.LIGHT_GRAY);
            qArea.setText("");
            checkBtn.setEnabled(false);
        } else {
            checkBtn.setEnabled(true);
            Color c;
            String statusText;

            if (currentCard.isNew) {
                c = new Color(200, 220, 255); statusText = "НОВОЕ";
            } else if (currentCard.level == 0) {
                c = new Color(255, 200, 200); statusText = "ОШИБКА / ЗАБЫЛ";
            } else if (currentCard.level < 5) {
                c = new Color(255, 240, 200); statusText = "УЧУ (" + currentCard.level + ")";
            } else {
                c = new Color(220, 255, 220); statusText = "ЗАКРЕПЛЯЮ (" + currentCard.level + ")";
            }

            String prefix = isShuffleMode ? "[SHUFFLE] " : "[SMART] ";
            infoLabel.setText(String.format("%s [%s] (%s) %s", prefix, currentCard.category, currentCard.sourceFile, statusText));
            infoLabel.setBackground(c);
            qArea.setText(currentCard.question);
            inputArea.requestFocusInWindow();
        }
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    public void checkAnswer() {
        if(currentCard == null) return;
        isAnswerRevealed = true;
        lastUserAnswer = inputArea.getText();

        AppLogger.info("Ответ: " + lastUserAnswer);
        double similarity = SmartChecker.check(lastUserAnswer, currentCard.answer);
        boolean passed = similarity > 65.0;

        aArea.setText(currentCard.answer); aArea.setVisible(true); inputArea.setEditable(false);
        feedbackLabel.setText((passed ? "Отлично!" : "Неточно") + " (Сходство: " + (int)similarity + "%)");
        feedbackLabel.setForeground(passed ? new Color(0,120,0) : Color.RED);
        inputArea.setBackground(passed ? new Color(220,255,220) : new Color(255,220,220));

        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        (passed ? yesBtn : noBtn).requestFocusInWindow();
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    public void submitResult(boolean correct) {
        historyManager.saveEntry(currentCard.question, lastUserAnswer, correct);
        deckManager.processAnswer(currentCard, correct);
        loadNextCard();
    }

    // --- ОБНОВЛЕНИЕ ТАБЛИЦ ---

    private void updateStats() {
        DefaultTableModel gm = (DefaultTableModel) globalStatsTable.getModel(); gm.setRowCount(0);
        if (deckManager == null) return;

        List<Card> all = deckManager.getAllCards();
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

        if (historyManager == null) return;

        List<HistoryManager.HistoryEntry> entries = historyManager.loadHistory();
        String searchLower = filter.toLowerCase().trim();

        for (HistoryManager.HistoryEntry entry : entries) {
            // Если фильтр пустой - показываем все
            // Иначе ищем совпадение в любом из полей
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

    private JTextArea createArea(int size, Color bg, boolean editable) {
        JTextArea t = new JTextArea();
        t.setFont(new Font("Arial", Font.PLAIN, size));
        t.setBackground(bg); t.setEditable(editable); t.setLineWrap(true); t.setWrapStyleWord(true);
        return t;
    }

    private JButton createButton(String txt, java.awt.event.ActionListener l) {
        JButton b = new JButton(txt); b.addActionListener(l); b.setPreferredSize(new Dimension(220,45)); return b;
    }
}