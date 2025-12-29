package ui.panels;

import model.Card;
import service.StudyService;
import ui.ThemeManager;
import ui.components.UIFactory;
import ui.controller.StudyController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Основная панель обучения
 * <p>
 * Отрисовка интерфейса и перехват событий пользователя:
 * клики, ввод текста, горячие клавиши
 * </p>
 */
public class StudyPanel extends JPanel {

    /** Ссылка на контроллер, который управляет этой панелью */
    private final StudyController controller;

    // UI КОМПОНЕНТЫ
    private JComboBox<String> categoryBox;
    private JTextArea questionArea, answerArea, inputArea;
    private JLabel infoLabel, feedbackLabel;

    /** Панель кнопок (динамически меняется: Проверить - > Верно/Ошибка) */
    private JPanel buttonPanel;

    private JButton checkBtn, yesBtn, noBtn;
    private JButton modeBtn;

    /**
     * Создает панель обучения
     */
    public StudyPanel(StudyService service) {
        setLayout(new BorderLayout());

        // статический интерфейс
        initUI(service);

        // инициализация контроллера
        this.controller = new StudyController(service, this);

        // обработчик событий
        setupListeners();

        // горяче клавиши (Enter, стрелки)
        setupKeys();

        controller.start();
    }

    /** Возвращает текст, введенный пользователем в поле ответа */
    public String getUserInput() {
        return inputArea.getText();
    }

    /** Обновляет текст на кнопке режима (например, "Mode: SHUFFLE") */
    public void updateModeButton(String text) {
        modeBtn.setText(text);
    }

    /**
     * Сбрасывает интерфейс к состоянию "Вопрос".
     * Очищает поля, скрывает ответ, возвращает кнопку "Проверить"
     */
    public void resetUI() {
        buttonPanel.removeAll();
        buttonPanel.add(checkBtn);

        inputArea.setText("");
        inputArea.setEditable(true);
        inputArea.setBackground(ThemeManager.getInputBg());

        answerArea.setVisible(false);
        feedbackLabel.setText(" ");

        // обновление цветов (если тема сменилась)
        ThemeManager.apply(this);

        buttonPanel.revalidate();
        buttonPanel.repaint();

        // фокус на поле ввода, чтобы сразу печатать
        inputArea.requestFocusInWindow();
    }

    /** Отображает состояние "Колода пуста" */
    public void showEmptyState() {
        questionArea.setText("Нет карт в этой категории или все выучено");
        checkBtn.setEnabled(false);
        infoLabel.setText("Пусто");
        infoLabel.setBackground(Color.GRAY);
        buttonPanel.revalidate(); buttonPanel.repaint();
    }

    /**
     * Отображает данные карточки (вопрос, категорию, уровень)
     */
    public void displayCard(Card card) {
        checkBtn.setEnabled(true);
        questionArea.setText(card.getQuestion());

        String status = card.isNew() ? "НОВОЕ" : "LVL " + card.getLevel();
        infoLabel.setText(String.format("[%s] %s", card.getCategory(), status));

        // цветовое кодирование статуса
        infoLabel.setOpaque(true);
        if (card.isNew()) infoLabel.setBackground(config.ThemeColors.STATUS_NEW);
        else if (card.getLevel() == 0) infoLabel.setBackground(config.ThemeColors.STATUS_ERROR);
        else if (card.getLevel() < 8) infoLabel.setBackground(config.ThemeColors.STATUS_LEARNING);
        else infoLabel.setBackground(config.ThemeColors.STATUS_MASTER);

        infoLabel.setForeground(Color.WHITE);

        ThemeManager.apply(this);
    }

    /**
     * Показывает поле с правильным ответом (вызывается после проверки)
     */
    public void showAnswer(String answer) {
        answerArea.setText(answer);
        answerArea.setVisible(true);
        inputArea.setEditable(false); // Блокируем изменение ответа пользователя
    }

    /**
     * Отображает визуальный фидбек (верно/неверно) и кнопки оценки
     *
     * @param passed верно
     * @param inputBgColor цвет фона для поля ввода (зеленый/красный)
     * @param message текст сообщения ("OK" / "ОШИБКА")
     */
    public void setFeedback(boolean passed, Color inputBgColor, String message) {
        inputArea.setBackground(inputBgColor);
        feedbackLabel.setText(message);

        // замена кнопки "Проверить" на кнопки оценки "ВЕРНО / ОШИБКА"
        buttonPanel.removeAll();
        buttonPanel.add(noBtn);
        buttonPanel.add(yesBtn);
        buttonPanel.revalidate();
        buttonPanel.repaint();

        // фокус на логическую кнопку, чтобы можно было нажать Enter/Space
        (passed ? yesBtn : noBtn).requestFocusInWindow();
    }
    /** Подключение слушателей кнопок и комбобокса */
    private void setupListeners() {
        checkBtn.addActionListener(_ -> controller.onCheckAction());
        yesBtn.addActionListener(_ -> controller.onSubmitCorrect());
        noBtn.addActionListener(_ -> controller.onSubmitError());
        modeBtn.addActionListener(_ -> controller.onToggleMode());

        categoryBox.addActionListener(_ ->
                controller.onCategorySelected((String) categoryBox.getSelectedItem())
        );
    }

    /**
     * Настройка глобальных горячих клавиш (Key Bindings)
     */
    private void setupKeys() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // ENTER - > проверить ответ
        im.put(KeyStroke.getKeyStroke("ENTER"), "enter");
        am.put("enter", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.onCheckAction(); } });

        // стрелка ВПРАВО - > верно
        im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
        am.put("right", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.onSubmitCorrect(); } });

        // стрелка ВЛЕВО - > ошибка
        im.put(KeyStroke.getKeyStroke("LEFT"), "left");
        am.put("left", new AbstractAction() { public void actionPerformed(ActionEvent e) { controller.onSubmitError(); } });
    }

    /** Создание и компоновка всех визуальных элементов */
    private void initUI(StudyService service) {
        JPanel topContainer = new JPanel(new BorderLayout());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // фильтр категорий
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

        // центральная область (Вопрос, Ввод, Ответ)
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
        answerArea.setVisible(false); // Скрыто по умолчанию
        answerContainer.add(new JScrollPane(answerArea), BorderLayout.CENTER);
        centerPanel.add(answerContainer);
        add(centerPanel, BorderLayout.CENTER);

        // нижняя панель кнопок
        buttonPanel = new JPanel(new FlowLayout());
        checkBtn = UIFactory.createButton("Проверить [Enter]", null);
        yesBtn = UIFactory.createButton("ВЕРНО [->]", null);
        noBtn = UIFactory.createButton("ОШИБКА [<-]", null);
        buttonPanel.add(checkBtn);
        add(buttonPanel, BorderLayout.SOUTH);
    }
}