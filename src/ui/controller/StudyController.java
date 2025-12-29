package ui.controller;

import model.Card;
import service.StudyService;
import ui.ThemeManager;
import ui.panels.StudyPanel;

import java.awt.Color;

/**
 * Контроллер вкладки обучения.
 * <p>
 * Реализует паттерн <b>MVC (Model-View-Controller)</b>.
 * Служит связующим звеном между пользовательским интерфейсом ({@link StudyPanel})
 * и бизнес-логикой ({@link StudyService})
 * </p>
 * <p>
 * <b>Обязанности:</b>
 * <ul>
 *     <li>Обработка нажатий кнопок (Check, Correct, Error, Mode)</li>
 *     <li>Управление состоянием экрана (Вопрос - > Ответ - > Оценка)</li>
 *     <li>Определение цветов обратной связи (зеленый/красный) в зависимости от темы</li>
 * </ul>
 * </p>
 */
public class StudyController {
    private final StudyService service;
    private final StudyPanel view;

    /** Режим перемешивания: true = Random, false = Smart SRS */
    private boolean isShuffleMode = false;

    /**
     * Флаг состояния текущего цикла вопроса.
     * <br>false = пользователь видит вопрос и вводит ответ.
     * <br>true = пользователь видит правильный ответ и должен выбрать оценку
     */
    private boolean isAnswerRevealed = false;

    /**
     * Конструктор контроллера
     *
     * @param service сервис бизнес-логики
     * @param view панель отображения
     */
    public StudyController(StudyService service, StudyPanel view) {
        this.service = service;
        this.view = view;
    }

    /**
     * Запускает процесс обучения (загружает первую карту)
     */
    public void start() {
        loadNextCard();
    }

    // ОБРАБОТЧИКИ СОБЫТИЙ UI

    /**
     * Нажатие на кнопку "Проверить" (или Enter)
     */
    public void onCheckAction() {
        // Защита: нельзя проверить ответ дважды на одной карте
        if (!isAnswerRevealed) {
            checkAnswer();
        }
    }

    /**
     * Нажатие на кнопку "Верно / Знаю"
     */
    public void onSubmitCorrect() {
        // Защита: нельзя оценить карту, пока не увидел правильный ответ
        if (isAnswerRevealed) submit(true);
    }

    /**
     * Нажатие на кнопку "Ошибка / Забыл"
     */
    public void onSubmitError() {
        if (isAnswerRevealed) submit(false);
    }

    /**
     * Переключение режима алгоритма (Shuffle / Smart)
     */
    public void onToggleMode() {
        isShuffleMode = !isShuffleMode;
        view.updateModeButton(isShuffleMode ? "РЕЖИМ: SHUFFLE" : "РЕЖИМ: SMART");
        // сразу загружаем новую карту по новому алгоритму
        loadNextCard();
    }

    /**
     * Выбор категории в выпадающем списке
     */
    public void onCategorySelected(String category) {
        if (category != null && !category.equals("ВСЕ ТЕМЫ")) {
            service.setFilter(category);
        } else {
            service.resetFilter();
        }
        // сбрасываем текущую карту и берем новую из выбранной категории
        loadNextCard();
    }

    // ЛОГИКА УПРАВЛЕНИЯ

    /**
     * Загружает следующую карточку из сервиса и обновляет экран
     */
    private void loadNextCard() {
        // сбрасываем флаг состояния
        isAnswerRevealed = false;

        // данные из модели
        Card card = service.nextCard(isShuffleMode);

        // обновляем вид
        view.resetUI();

        if (card == null) {
            view.showEmptyState(); // заглушка "Карт нет"
            return;
        }

        view.displayCard(card);
    }

    /**
     * Логика проверки ответа пользователя.
     * Вычисляет результат, подбирает цвет плашки и показывает правильный ответ
     */
    private void checkAnswer() {
        if (service.getCurrentCard() == null) return;

        // блокируем повторную проверку и разрешаем оценку
        isAnswerRevealed = true;

        String userAns = view.getUserInput();
        // нечетное сравнение строк
        var result = service.checkAnswer(userAns);

        // скрытая панель с правильным ответом
        view.showAnswer(service.getCurrentCard().getAnswer());

        // цвет фона результата
        // логика цвета тут, в контроллере, т.к. она зависит от темы и результата одновременно
        Color bgCol;
        if (ThemeManager.isDark()) {
            bgCol = result.passed() ? new Color(40, 80, 40) : new Color(80, 40, 40);
        } else {
            bgCol = result.passed() ? new Color(200, 255, 200) : new Color(255, 200, 200);
        }

        String message = result.passed() ? "OK" : "ОШИБКА";
        view.setFeedback(result.passed(), bgCol, message);
    }

    /**
     * Финальная фиксация результата и переход к следующей карте
     */
    private void submit(boolean correct) {
        // прогресс в БД/файл
        service.submitResult(view.getUserInput(), correct);
        // рекурсивный (логически) вызов следующего шага
        loadNextCard();
    }
}