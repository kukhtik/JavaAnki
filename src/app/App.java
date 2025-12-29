package app;

import data.repository.*;
import service.StudyService;
import service.session.SessionManager;
import ui.MainFrame;
import ui.ThemeManager;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * Корень композиции
 * Здесь происходит инициализация всех слоев архитектуры:
 * <ol>
 *     <li>Data Access Layer (Репозитории)</li>
 *     <li>Business Logic Layer (Менеджеры и Сервисы)</li>
 *     <li>Presentation Layer (UI)</li>
 * </ol>
 * Также здесь настраивается внешний вид (Look and Feel) Swing
 * </p>
 */
public class App {
    /** Логгер для записи основных событий жизненного цикла приложения */
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    /**
     * Запускает инициализацию интерфейса в потоке обработки событий
     */
    @SuppressWarnings("unused")
    static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // нативный стиль отображения (для ОС)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                // глобальная настройка шрифтов и цветов
                ThemeManager.setupGlobal();
            } catch (Exception ignored) {
                LOGGER.warning("Не удалось установить системный LookAndFeel");
            }

            LOGGER.info("<<<<<< ЗАПУСК ПРИЛОЖЕНИЯ >>>>>>");
            StudyService studyService = getStudyService();

            // слой представления
            // создание главного окна, внедрение в него сервиса и отображение.
            MainFrame mainFrame = new MainFrame(studyService);
            mainFrame.setVisible(true);

            LOGGER.info("Главное окно успешно отображено");
        });
    }

    private static StudyService getStudyService() {
        CardRepository cardRepo = new FileDeckRepository();
        StatsRepository statsRepo = new FileStatsRepository();
        HistoryRepository historyRepo = new HistoryRepository();
        GroupRepository groupRepo = new GroupRepository();

        // слой бизнес-логики
        // SessionManager управляет состоянием текущей сессии обучения
        SessionManager sessionManager = new SessionManager(cardRepo, statsRepo, historyRepo, groupRepo);

        // сервисный слой
        // StudyService выступает фасадом для UI, скрывая сложность SessionManager'а
        // и предоставляя упрощенный интерфейс для контроллеров или форм
        return new StudyService(sessionManager);
    }
}