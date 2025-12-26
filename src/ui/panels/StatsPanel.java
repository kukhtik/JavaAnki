package ui.panels;

import model.dto.StatsRow;
import service.StudyService;
import ui.ThemeManager;
import ui.components.UIFactory;
import ui.dialogs.DialogFactory;
import util.EventBus;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class StatsPanel extends JPanel {
    private final StudyService service;
    private final DefaultTableModel model;
    private final JTable table;

    public StatsPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        String[] columns = {"Категория", "Всего", "Новые", "Учу (1-7)", "Мастер (8+)"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setRowHeight(25);
        table.setFillsViewportHeight(true);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();

        var refreshBtn = UIFactory.createButton("Обновить", e -> service.reloadSession());
        var importBtn = UIFactory.createButton("Импорт", e -> {
            service.ImportService importer = new service.ImportService(service);
            String result = importer.performImport();
            DialogFactory.showInfo(this, result);
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(importBtn);
        add(btnPanel, BorderLayout.SOUTH);

        EventBus.subscribe(EventBus.Topic.DATA_UPDATED, () -> SwingUtilities.invokeLater(this::refreshData));
        refreshData();
    }

    public void refreshData() {
        model.setRowCount(0);
        for (model.dto.StatsRow row : service.getStatistics()) {
            model.addRow(new Object[]{
                    row.getCategory(), row.getTotal(), row.getNewCards(), row.getLearning(), row.getMaster()
            });
        }
        ThemeManager.apply(this);
        table.repaint();
    }
}