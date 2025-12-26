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
    private final DefaultTableModel model;
    private final JTable table;

    public StatsPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        // Таблица
        String[] columns = {"Категория", "Всего", "Новые", "Учу (1-7)", "Мастер (8+)"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setRowHeight(25);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // Кнопки (Создаем ОДИН РАЗ)
        JPanel btnPanel = new JPanel();

        var refreshBtn = UIFactory.createButton("Обновить", e -> refreshData());
        refreshBtn.setPreferredSize(new Dimension(150, 40));

        var importBtn = UIFactory.createButton("Импорт из txt", e -> {
            service.ImportService importer = new service.ImportService(service);
            String result = importer.performImport();
            JOptionPane.showMessageDialog(this, result);
            refreshData();
        });
        importBtn.setPreferredSize(new Dimension(150, 40));

        btnPanel.add(refreshBtn);
        btnPanel.add(importBtn);

        add(btnPanel, BorderLayout.SOUTH);
    }

    public void refreshData() {
        model.setRowCount(0);
        List<Card> allCards = service.getAllCards();

        // Группировка
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

        // Итого
        long totalAll = allCards.size();
        long newAll = allCards.stream().filter(Card::isNew).count();
        model.addRow(new Object[]{"=== ИТОГО ===", totalAll, newAll, "---", "---"});

        Theme.apply(this);
    }
}