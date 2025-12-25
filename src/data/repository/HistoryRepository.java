package data.repository;

import data.FileService;
import model.HistoryRecord;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для хранения и загрузки истории ответов пользователя
 * <p>
 * Попытка ответа уходит в текстовый файл {@code history_log.txt}
 * Формат строки в файле:{@code yyyy-MM-dd HH:mm:ss | QUESTION | USER_ANSWER | ВЕРНО/ОШИБКА}
 */
public class HistoryRepository {
    private static final String HISTORY_FILE = "history_log.txt";
    private static final DateTimeFormatter DNF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final FileService fileService;
    private final Path filePath;

    /**
     * Создаёт репозиторий истории и инициализирует путь к файлу
     */
    public HistoryRepository() {
        this.fileService = new FileService();
        this.filePath = Paths.get(HISTORY_FILE);
    }

    /**
     * Сохраняет одну запись истории в файл
     */
    public void saveEntry(String question, String userAnswer, boolean isCorrect) {
        var date = LocalDateTime.now().format(DNF);
        var result = isCorrect ? "ВЕРНО" : "ОШИБКА";

        var safeQ = question.replace("\n", " ").trim();
        var safeA = userAnswer.replace("\n", " ").trim();

        var line = String.format("%s | %s | %s | %s", date, safeQ, safeA, result);

        fileService.appendLine(filePath, line);
    }

    /**
     * Загружает всю историю ответов из файла
     */
    public List<HistoryRecord> loadHistory() {
        List<String> lines = fileService.readAllLines(filePath);
        List<HistoryRecord> records = new ArrayList<>();

        for (var line : lines) {
            var parts = line.split("\\s\\|\\s");
            if (parts.length >= 4) {
                records.add(0, new HistoryRecord(
                        parts[0], // date
                        parts[1], // question
                        parts[2], // userAnswer
                        parts[3].equals("ВЕРНО") // isCorrect
                ));
            }
        }
        return records;
    }
}
