package app;

import service.StudyService;
import ui.MainFrame;

import javax.swing.*;

/**
 * Вход
 * <ul>
 *     <li>установка системного LookAndFeel</li>
 *     <li>запуск Swing-приложения в UI-потоке</li>
 *     <li>инициализация сервисного слоя ({@link StudyService})</li>
 *     <li>создание главного окна ({@link MainFrame})</li>
 * </ul>
 */
public class App {

    /**
     * Главный метод запуска приложения
     */
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // запуск UI в Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {

            // сервис обучения (бизнес-логика)
            StudyService studyService = new StudyService();

            // главное окно и передаём ему сервис
            new MainFrame(studyService).setVisible(true);
        });
    }
}
