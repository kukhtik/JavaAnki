package app;

import service.StudyService;
import ui.MainFrame;
import ui.Theme;

import javax.swing.*;
import java.util.logging.Logger;

public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Сначала системный стиль, чтобы рамки окон были родные
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // А ПОВЕРХ накатываем наши цвета для компонентов
                Theme.setupGlobal();
            } catch (Exception ignored) {}

            LOGGER.info("<<<< ЗАПУСК ПРИЛОЖЕНИЯ >>>>");
            StudyService studyService = new StudyService();

            int count = studyService.getAllCards().size();
            LOGGER.info("Система инициализирована. Всего карт в базе: " + count);

            new MainFrame(studyService).setVisible(true);
        });
    }
}