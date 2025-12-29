package ui.panels;

import model.HistoryRecord;
import service.StudyService;
import ui.components.UIFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * Панель просмотра истории ответов
 * <p>
 * Отображает таблицу со списком вопросов, ответов и вердикта, даты
 * </p>
 */
public class HistoryPanel extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(HistoryPanel.class.getName());

    private final StudyService service;

    /** Модель данных для таблицы (строки и столбцы) */
    private final DefaultTableModel model;

    /** Поле ввода для фильтрации */
    private final JTextField searchField;

    /**
     * Создает панель истории
     *
     * @param service сервис для получения данных из репозитория
     */
    public HistoryPanel(StudyService service) {
        this.service = service;
        setLayout(new BorderLayout());

        // верхняя панель
        var topPanel = new JPanel(new BorderLayout());
        // отступы для красоты
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        topPanel.add(UIFactory.createLabel("Поиск: "), BorderLayout.WEST);

        searchField = new JTextField();
        searchField.setFont(UIFactory.FONT_MAIN);
        // поиск по нажатию Enter
        searchField.addActionListener(_ -> updateTable(searchField.getText()));
        topPanel.add(searchField, BorderLayout.CENTER);

        var searchBtn = UIFactory.createButton("Найти", _ -> updateTable(searchField.getText()));
        searchBtn.setPreferredSize(new Dimension(100, 30));
        topPanel.add(searchBtn, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        String[] cols = {"Дата", "Вопрос", "Ответ", "Результат"};

        // модель, которая запрещает редактирование ячеек
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        var table = new JTable(model);
        table.setRowHeight(25);
        table.setFont(UIFactory.FONT_MAIN);

        // таблицу в ScrollPane, чтобы появлялась прокрутка
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    /**
     * Обновляет содержимое таблицы, загружая данные из сервиса и применяя фильтр
     *
     * @param filter текст для поиска. Если пустой - показываются все записи
     */
    public void updateTable(String filter) {
        LOGGER.info("Обновление таблицы истории. Фильтр: [" + filter + "]");

        // очистка текущих строк
        model.setRowCount(0);

        // загрузка данных через сервис
        List<HistoryRecord> records = service.getHistory();
        String lowerFilter = filter.toLowerCase();

        // фильтрация и добавление в UI
        for (var rec : records) {
            // проверка на вхождение подстроки
            boolean matches = filter.isEmpty() ||
                    rec.question().toLowerCase().contains(lowerFilter) ||
                    rec.userAnswer().toLowerCase().contains(lowerFilter);

            if (matches) {
                model.addRow(new Object[]{
                        rec.date(),
                        rec.question(),
                        rec.userAnswer(),
                        rec.getResultLabel() // ВЕРНО или ОШИБКА
                });
            }
        }
    }
}