package ui.controller;

import model.Card;
import service.StudyService;
import ui.panels.StudyPanel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Ручной интеграционный тест для {@link StudyController}
 * <p>
 * Проверяет взаимодействие контроллера с View и Service, используя.
 * Контроллер правильно оркестрирует процесс:
 * "Загрузить карту" - > "Проверить ответ" - > "Отправить результат"
 * </p>
 */
public class ManualControllerTest {

    public static void main() {
        System.out.println("=== TEST: StudyController (Manual Integration) ===");
        try {
            testStart_LoadsCard();
            testCheckAction_CallsService();
            testSubmit_CallsService();
            System.out.println(">>> ВСЕ ТЕСТЫ КОНТРОЛЛЕРА ПРОШЛИ УСПЕШНО");
        } catch (Exception e) {
            System.err.println("!!! ТЕСТ УПАЛ !!!");
        }
    }

    /**
     * Тест: При старте контроллер должен запросить карту у сервиса и отобразить её во View
     */
    private static void testStart_LoadsCard() {
        FakeService service = new FakeService();
        FakeStudyView view = new FakeStudyView(service);
        StudyController controller = new StudyController(service, view);

        controller.start();

        check(view.wasReset, "Интерфейс должен быть сброшен (resetUI) перед показом карты");
        check("Test Question".equals(view.lastDisplayedQuestion),
                "Вопрос на экране должен совпадать с тем, что вернул сервис");

        System.out.println("testStart: OK");
    }

    /**
     * Тест: Нажатие кнопки 'Проверить' должно вызвать логику проверки в сервисе и показать ответ
     */
    private static void testCheckAction_CallsService() {
        FakeService service = new FakeService();
        FakeStudyView view = new FakeStudyView(service);
        StudyController controller = new StudyController(service, view);

        controller.start(); // загружаем карту
        controller.onCheckAction(); // "Проверить"

        check(service.checkAnswerCalled, "Метод сервиса checkAnswer() должен быть вызван");
        check("Test Answer".equals(view.lastShownAnswer), "Правильный ответ должен появиться в View");

        System.out.println("testCheckAction: OK");
    }

    /**
     * Тест: Нажатие оценки (Верно/Ошибка) должно отправить результат в сервис
     */
    private static void testSubmit_CallsService() {
        FakeService service = new FakeService();
        FakeStudyView view = new FakeStudyView(service);
        StudyController controller = new StudyController(service, view);

        controller.start();
        controller.onCheckAction();

        controller.onSubmitCorrect(); // "ВЕРНО"

        check(service.submitResultCalled, "Метод сервиса submitResult() должен быть вызван");

        System.out.println("testSubmit: OK");
    }

    private static void check(boolean condition, String msg) {
        if (!condition) throw new RuntimeException("FAIL: " + msg);
    }

    /**
     * Фейковый сервис.
     * Вместо реальной работы с БД просто возвращает заранее заданные данные
     * и запоминает, какие методы были вызваны
     */
    static class FakeService extends StudyService {
        boolean checkAnswerCalled = false;
        boolean submitResultCalled = false;

        public FakeService() {
            super(null); // SessionManager null, так как мы переопределяем все методы, где он нужен
        }

        // переопределяем методы, чтобы разорвать зависимость от SessionManager
        @Override public void reloadSession() {}
        @Override public List<String> getAvailableCategories() { return new ArrayList<>(); }

        @Override
        public Card nextCard(boolean isShuffle) {
            Card c = new Card();
            c.setQuestion("Test Question");
            c.setAnswer("Test Answer");
            c.setId("test-id");
            c.setCategory("TestCat");
            c.setLevel(0);
            return c;
        }

        @Override
        public Card getCurrentCard() {
            return nextCard(false);
        }

        @Override
        public GradingResult checkAnswer(String answer) {
            checkAnswerCalled = true;
            return new GradingResult(100.0, true);
        }

        @Override
        public void submitResult(String ans, boolean correct) {
            submitResultCalled = true;
        }
    }

    /**
     * Фейковая панель.
     * Не рисует окна, а просто запоминает, что контроллер попросил отобразить
     */
    static class FakeStudyView extends StudyPanel {
        String lastDisplayedQuestion;
        String lastShownAnswer;
        boolean wasReset = false;

        public FakeStudyView(StudyService service) {
            super(service);
        }

        @Override public void displayCard(Card card) { this.lastDisplayedQuestion = card.getQuestion(); }
        @Override public void showAnswer(String answer) { this.lastShownAnswer = answer; }
        @Override public void setFeedback(boolean passed, Color color, String msg) { }
        @Override public void resetUI() { this.wasReset = true; }
        @Override public String getUserInput() { return "user answer"; }
        @Override public void showEmptyState() {}
        @Override public void updateModeButton(String text) {}
    }
}