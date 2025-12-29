package data.repository;

import data.FileService;
import model.HistoryRecord;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для ведения лога истории ответов пользователя
 * <p>
 * История сохраняется в текстовый файл {@code history_log.txt} в корне приложения
 * </p>
 * <p>
 * Формат строки файла:
 * <br>
 * {@code yyyy-MM-dd HH:mm:ss | QUESTION | USER_ANSWER | ВЕРНО/ОШИБКА}
 * </p>
 */
public class HistoryRepository {

    /** Имя файла журнала */
    private static final String HISTORY_FILE = "history_log.txt";

    /** Формат даты для записи в лог (например, "2025-12-25 14:30:00") */
    private static final DateTimeFormatter DNF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final FileService fileService;
    private final Path filePath;

    /**
     * Создаёт репозиторий и инициализирует путь к файлу лога
     */
    public HistoryRepository() {
        this.fileService = new FileService() {
            @Override
            public void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {

            }
        };
        this.filePath = Paths.get(HISTORY_FILE);
    }

    /**
     * Сохраняет одну запись о прохождении карточки в конец файла
     *
     * @param question текст вопроса карточки
     * @param userAnswer текст, который ввел пользователь
     * @param isCorrect результат проверки
     */
    public void saveEntry(String question, String userAnswer, boolean isCorrect) {
        var date = LocalDateTime.now().format(DNF);
        var result = isCorrect ? "ВЕРНО" : "ОШИБКА";

        // переносы строк на пробелы, чтобы не ломать структуру CSV/Log файла
        var safeQ = question.replace("\n", " ").trim();
        var safeA = userAnswer.replace("\n", " ").trim();

        // формирование строки с разделителями " | "
        var line = String.format("%s | %s | %s | %s", date, safeQ, safeA, result);

        fileService.appendLine(filePath, line);
    }

    /**
     * Загружает и парсит всю историю ответов из файла.
     * <p>
     * Метод читает файл построчно и преобразует строки в объекты {@link HistoryRecord}.
     * </p>
     * <p>
     * <b>Порядок сортировки:</b><br>
     * Новые записи добавляются в начало списка (index 0).
     * Таким образом, возвращаемый список отсортирован <b>от новых к старым</b> (Newest First),
     * что удобно для отображения в таблице истории.
     * </p>
     *
     * @return Список записей истории (самые свежие в начале списка).
     */
    public List<HistoryRecord> loadHistory() {
        List<String> lines = fileService.readAllLines(filePath);
        List<HistoryRecord> records = new ArrayList<>();

        for (var line : lines) {
            // разбиваем строку по разделителю " | " (экранируем pipe, так как это спецсимвол regex)
            var parts = line.split("\\s\\|\\s");

            // строка содержит все 4 необходимых поля?
            if (parts.length >= 4) {
                HistoryRecord record = new HistoryRecord(
                        parts[0],
                        parts[1],
                        parts[2],
                        parts[3].equals("ВЕРНО") // строка статуса в boolean
                );

                // вставка в начало списка (0) обеспечивает обратный хронологический порядок
                records.addFirst(record);
            }
        }
        return records;
    }
}