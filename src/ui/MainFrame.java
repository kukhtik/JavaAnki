package ui;

import service.StudyService;
import ui.components.CustomTitleBar;
import ui.panels.HistoryPanel;
import ui.panels.StatsPanel;
import ui.panels.StudyPanel;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Главное окно
 * <p>
 * Собирает воедино все визуальные компоненты:
 * <ul>
 *     <li><b>CustomTitleBar</b> - кастомный заголовок окна</li>
 *     <li><b>JTabbedPane</b> - навигация между экранами (Обучение, Статистика, История)</li>
 * </ul>
 * Окно создается в режиме {@code undecorated}, чтобы рисовать свою
 * </p>
 */
public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());

    /**
     * Конструктор главного окна.
     * Здесь происходит сборка интерфейса и внедрение зависимостей в дочерние панели
     *
     * @param service экземпляр {@link StudyService}, который будет передан во все вкладки
     */
    public MainFrame(StudyService service) {
        LOGGER.info("Инициализация MainFrame...");

        // базовая настройка окна
        // убираем системную рамку (Windows/macOS title bar), чтобы нарисовать свою
        setUndecorated(true);
        setTitle("Java Anki");
        setSize(1000, 700);
        setLocationRelativeTo(null); // центрирование на экране
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // основной контейнер с BorderLayout
        var root = new JPanel(new BorderLayout());
        setContentPane(root);
        root.setBorder(new LineBorder(ThemeManager.getBorder(), 1));

        // сборка компонентов

        // кастомный заголовок
        add(new CustomTitleBar(this), BorderLayout.NORTH);

        // вкладки
        var tabs = getJTabbedPane(service);

        add(tabs, BorderLayout.CENTER);

        // финализация + применяется текущая тема к окнам
        ThemeManager.apply(this);
    }

    private static JTabbedPane getJTabbedPane(StudyService service) {
        var tabs = new JTabbedPane();

        var studyPanel = new StudyPanel(service);
        var statsPanel = new StatsPanel(service);
        var historyPanel = new HistoryPanel(service);

        tabs.addTab("Обучение", studyPanel);
        tabs.addTab("Статистика", statsPanel);
        tabs.addTab("История", historyPanel);

        // переключение
        // слушатель событий, чтобы обновлять данные в таблицах только тогда,
        // когда пользователь реально открывает соответствующую вкладку
        tabs.addChangeListener(_ -> {
            int index = tabs.getSelectedIndex();
            LOGGER.info("Переключение вкладки: " + index);

            // открыли Статистику (индекс 1) - > обновить цифры
            if (index == 1) statsPanel.refreshData();

            // открыли Историю (индекс 2) - > обновить таблицу
            if (index == 2) historyPanel.updateTable("");
        });
        return tabs;
    }
}