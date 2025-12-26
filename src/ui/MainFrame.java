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

public class MainFrame extends JFrame {
    private static final Logger LOGGER = Logger.getLogger(MainFrame.class.getName());

    public MainFrame(StudyService service) {
        LOGGER.info("Инициализация MainFrame...");

        // Убираем системную рамку
        setUndecorated(true);
        setTitle("Java Anki");
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Основной контейнер
        var root = new JPanel(new BorderLayout());
        setContentPane(root);

        // Рамка вокруг окна (так как системной нет)
        root.setBorder(new LineBorder(ThemeManager.getBorder(), 1));

        // 1. Заголовок
        add(new CustomTitleBar(this), BorderLayout.NORTH);

        // 2. Вкладки
        var tabs = new JTabbedPane();

        var studyPanel = new StudyPanel(service);
        var statsPanel = new StatsPanel(service);
        var historyPanel = new HistoryPanel(service);

        tabs.addTab("Обучение", studyPanel);
        tabs.addTab("Статистика", statsPanel);
        tabs.addTab("История", historyPanel);

        // Логика обновления данных при смене вкладки
        tabs.addChangeListener(e -> {
            int index = tabs.getSelectedIndex();
            LOGGER.info("Переключение вкладки: " + index);
            if (index == 1) statsPanel.refreshData();
            if (index == 2) historyPanel.updateTable("");
        });

        add(tabs, BorderLayout.CENTER);

        // Первичная покраска
        ThemeManager.apply(this);
    }
}