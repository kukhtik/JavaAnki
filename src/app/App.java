package app;

import data.repository.*;
import service.StudyService;
import service.session.SessionManager; // NEW
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

            LOGGER.info("=== ЗАПУСК ПРИЛОЖЕНИЯ ===");

            // 1. Repositories
            CardRepository cardRepo = new FileDeckRepository();
            StatsRepository statsRepo = new FileStatsRepository();
            HistoryRepository historyRepo = new HistoryRepository();
            GroupRepository groupRepo = new GroupRepository();

            // 2. Session Manager (Core Logic)
            SessionManager sessionManager = new SessionManager(cardRepo, statsRepo, historyRepo, groupRepo);

            // 3. Facade Service
            StudyService studyService = new StudyService(sessionManager);

            new MainFrame(studyService).setVisible(true);
        });
    }
}