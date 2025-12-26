package ui.panels;

import model.Card;
import service.StudyService;
import ui.Theme;
import ui.components.UIFactory;
import util.EventBus; // Импорт EventBus

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StatsPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(StatsPanel.class.getName());
    private final StudyService service;
    private final DefaultTableModel model;
    private final JTable table;

    public StatsPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        // --- Таблица ---
        String[] columns = {"Категория", "Всего", "Новые", "Учу (1-7)", "Мастер (8+)"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setRowHeight(25);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // --- Панель кнопок ---
        JPanel btnPanel = new JPanel();

        // Кнопка "Обновить"
        var refreshBtn = UIFactory.createButton("Обновить", e -> {
            LOGGER.info("Ручное обновление: Запрос перезагрузки сервиса...");
            // Мы просто просим сервис обновиться.
            // Когда он закончит, он кинет событие, и мы (как подписчик) обновим таблицу.
            service.reloadSession();
        });
        refreshBtn.setPreferredSize(new Dimension(150, 40));

        // Кнопка "Импорт"
        var importBtn = UIFactory.createButton("Импорт из txt", e -> {
            LOGGER.info("Запуск импорта...");
            // Сервис импорта требует StudyService для перезагрузки
            service.ImportService importer = new service.ImportService(service);
            String result = importer.performImport();

            Theme.showInfoDialog(this, result);
            LOGGER.info("Импорт завершен.");
            // Импорт сам вызывает service.reloadSession(), так что событие прилетит автоматически
        });
        importBtn.setPreferredSize(new Dimension(150, 40));

        btnPanel.add(refreshBtn);
        btnPanel.add(importBtn);
        add(btnPanel, BorderLayout.SOUTH);

        // --- ПОДПИСКА НА СОБЫТИЯ ---
        // Как только данные в сервисе меняются (при старте, импорте или кнопке Обновить),
        // этот код выполнится автоматически.
        EventBus.subscribe(EventBus.Topic.DATA_UPDATED, () -> {
            SwingUtilities.invokeLater(() -> {
                LOGGER.info("StatsPanel получил событие DATA_UPDATED. Перерисовка таблицы.");
                refreshData();
            });
        });

        // Первичная отрисовка (на случай, если данные уже загружены до создания панели)
        refreshData();
    }

    // Метод только перерисовывает UI на основе данных сервиса
    public void refreshData() {
        model.setRowCount(0);
        List<Card> allCards = service.getAllCards();

        Map<String, List<Card>> grouped = allCards.stream()
                .collect(Collectors.groupingBy(Card::getCategory));

        for (var entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<Card> list = entry.getValue();

            long total = list.size();
            long countNew = list.stream().filter(Card::isNew).count();
            long countMaster = list.stream().filter(c -> !c.isNew() && c.getLevel() >= 8).count();
            long countLearning = total - countNew - countMaster;

            model.addRow(new Object[]{category, total, countNew, countLearning, countMaster});
        }

        long totalAll = allCards.size();
        long newAll = allCards.stream().filter(Card::isNew).count();
        model.addRow(new Object[]{"=== ИТОГО ===", totalAll, newAll, "---", "---"});

        Theme.apply(this);
        table.repaint();
    }
}