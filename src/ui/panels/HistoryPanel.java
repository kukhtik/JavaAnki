package ui.panels;

import model.HistoryRecord;
import service.StudyService;
import ui.components.UIFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

public class HistoryPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(HistoryPanel.class.getName());
    private final StudyService service;
    private final DefaultTableModel model;
    private final JTextField searchField;

    public HistoryPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        // Панель поиска
        var topPanel = new JPanel(new BorderLayout());
        topPanel.add(UIFactory.createLabel("Поиск: "), BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(UIFactory.FONT_MAIN);
        searchField.addActionListener(e -> updateTable(searchField.getText()));
        topPanel.add(searchField, BorderLayout.CENTER);

        var searchBtn = UIFactory.createButton("Найти", e -> updateTable(searchField.getText()));
        searchBtn.setPreferredSize(new Dimension(100, 30));
        topPanel.add(searchBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // Таблица
        String[] cols = {"Дата", "Вопрос", "Ответ", "Результат"};
        model = new DefaultTableModel(cols, 0);
        var table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(UIFactory.FONT_MAIN);

        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void updateTable(String filter) {
        LOGGER.info("Запрос поиска истории: " + filter);
        model.setRowCount(0);

        // В StudyService нужно добавить метод: public List<HistoryRecord> getHistory() { return historyRepo.loadHistory(); }
        List<HistoryRecord> records = service.getHistory();
        String lowerFilter = filter.toLowerCase();

        for (var rec : records) {
            if (filter.isEmpty() ||
                    rec.getQuestion().toLowerCase().contains(lowerFilter) ||
                    rec.getUserAnswer().toLowerCase().contains(lowerFilter)) {

                model.addRow(new Object[]{
                        rec.getDate(),
                        rec.getQuestion(),
                        rec.getUserAnswer(),
                        rec.getResultLabel()
                });
            }
        }
    }
}