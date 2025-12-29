package ui.panels;

import model.dto.StatsRow;
import service.StudyService;
import service.ImportService;
import ui.ThemeManager;
import ui.components.UIFactory;
import ui.dialogs.DialogFactory;
import util.EventBus;
import data.FileService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.OpenOption;
import java.nio.file.Path;

/**
 * Панель статистики и управления данными
 * <p>
 * Отображает сводную таблицу прогресса по категориям.
 * Также содержит кнопки для глобальных действий с данными:
 * <ul>
 *     <li>Обновить: перезагрузка файлов</li>
 *     <li>Импорт: запуск процедуры добавления карт из import.txt</li>
 * </ul>
 * </p>
 */
public class StatsPanel extends JPanel {
    private final StudyService service;
    private final DefaultTableModel model;
    private final JTable table;

    /**
     * Создает панель статистики
     *
     * @param service сервис для получения агрегированных данных
     */
    public StatsPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        String[] columns = {"Категория", "Всего", "Новые", "Учу (1-7)", "Мастер (8+)"};

        // модель таблицы (данные)
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // запрещаем редактировать ячейки
            }
        };

        table = new JTable(model);
        table.setRowHeight(25);
        table.setFillsViewportHeight(true); // таблица занимает всю высоту, даже если строк мало
        table.setFont(UIFactory.FONT_MAIN);

        add(new JScrollPane(table), BorderLayout.CENTER);

        // кнопки
        JPanel btnPanel = new JPanel();

        // обновить
        var refreshBtn = UIFactory.createButton("Обновить", _ -> {
            service.reloadSession();
            // после reload сработает EventBus, и таблица обновится сама
        });

        // импорт
        var importBtn = UIFactory.createButton("Импорт из файла", _ -> {
            FileService fs = new FileService() {
                @Override
                public void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {

                }
            };
            ImportService importer = new ImportService(service, fs);
            String result = importer.performImport();
            DialogFactory.showInfo(this, result);
        });

        btnPanel.add(refreshBtn);
        btnPanel.add(importBtn);
        add(btnPanel, BorderLayout.SOUTH);
        EventBus.subscribe(EventBus.Topic.DATA_UPDATED, () ->
                SwingUtilities.invokeLater(this::refreshData)
        );

        // первичное заполнение
        refreshData();
    }

    /**
     * Перерисовывает таблицу актуальными данными
     */
    public void refreshData() {
        model.setRowCount(0);

        // запрашиваем свежие DTO из сервиса
        for (StatsRow row : service.getStatistics()) {
            model.addRow(new Object[]{
                    row.getCategory(),
                    row.getTotal(),
                    row.getNewCards(),
                    row.getLearning(),
                    row.getMaster()
            });
        }

        // применяем тему (цвета фона, текста) и перерисовываем
        ThemeManager.apply(this);
        table.repaint();
    }
}