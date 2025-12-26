package app;

import data.repository.*;
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
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                Theme.setupGlobal();
            } catch (Exception ignored) {}

            LOGGER.info("=== ЗАПУСК ПРИЛОЖЕНИЯ (Сборка зависимостей) ===");

            // 1. Создаем конкретные реализации репозиториев (Работа с файлами)
            CardRepository cardRepo = new FileDeckRepository();
            StatsRepository statsRepo = new FileStatsRepository();
            HistoryRepository historyRepo = new HistoryRepository();
            GroupRepository groupRepo = new GroupRepository();

            // 2. Внедряем зависимости в Сервис (Dependency Injection)
            // Сервис не знает, что мы используем файлы. Ему дали интерфейсы.
            StudyService studyService = new StudyService(cardRepo, statsRepo, historyRepo, groupRepo);

            LOGGER.info("Сервис собран. Карт: " + studyService.getAllCards().size());

            // 3. Запускаем UI
            new MainFrame(studyService).setVisible(true);
        });
    }
}