package ui.panels;

import model.Card;
import service.StudyService;
import ui.Theme;
import ui.components.UIFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsPanel extends JPanel {
    private final StudyService service;
    private final JTable table;
    private final DefaultTableModel model;

    public StatsPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        // Настройка таблицы
        String[] columns = {"Категория", "Всего", "Новые", "Учу (1-7)", "Мастер (8+)"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(UIFactory.FONT_MAIN);
        table.getTableHeader().setFont(UIFactory.FONT_BOLD);

        // Обертка в скролл
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Кнопка обновления
        var refreshBtn = UIFactory.createButton("Обновить данные", e -> refreshData());
        add(refreshBtn, BorderLayout.SOUTH);
    }

    public void refreshData() {
        model.setRowCount(0);
        List<Card> allCards = service.getAllCards();

        // Группировка карт по категориям
        Map<String, List<Card>> grouped = allCards.stream()
                .collect(Collectors.groupingBy(Card::getCategory));

        // Добавляем строки
        for (var entry : grouped.entrySet()) {
            String category = entry.getKey();
            List<Card> list = entry.getValue();

            long total = list.size();
            long countNew = list.stream().filter(Card::isNew).count();
            long countMaster = list.stream().filter(c -> !c.isNew() && c.getLevel() >= 8).count();
            long countLearning = total - countNew - countMaster;

            model.addRow(new Object[]{category, total, countNew, countLearning, countMaster});
        }

        // Строка ИТОГО
        long totalAll = allCards.size();
        long newAll = allCards.stream().filter(Card::isNew).count();
        model.addRow(new Object[]{"=== ИТОГО ===", totalAll, newAll, "---", "---"});

        Theme.apply(this); // Обновить цвета таблицы

        // Внутри StatsPanel.java, в конструкторе, где добавляются кнопки:

        var importBtn = UIFactory.createButton("Импорт из import.txt", e -> {
            service.ImportService importer = new service.ImportService(service);
            String result = importer.performImport();
            JOptionPane.showMessageDialog(this, result);
            refreshData(); // Обновить таблицу
        });

        JPanel btnPanel = new JPanel();
        btnPanel.add(UIFactory.createButton("Обновить данные", e -> refreshData()));
        btnPanel.add(importBtn); // <-- Добавили
        add(btnPanel, BorderLayout.SOUTH);
    }
}