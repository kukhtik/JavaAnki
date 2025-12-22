import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String HISTORY_FILE = "history_log.txt";
    private static final DateTimeFormatter DNF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Класс для строки истории
    public static class HistoryEntry {
        String date;
        String question;
        String userAnswer;
        String result;

        public HistoryEntry(String date, String question, String userAnswer, String result) {
            this.date = date;
            this.question = question;
            this.userAnswer = userAnswer;
            this.result = result;
        }
    }

    public void saveEntry(String question, String userAnswer, boolean isCorrect) {
        String date = LocalDateTime.now().format(DNF);
        String result = isCorrect ? "ВЕРНО" : "ОШИБКА";

        // Заменяем переносы строк на пробелы, чтобы сохранить в одну строку
        String safeQ = question.replace("\n", " ").trim();
        String safeA = userAnswer.replace("\n", " ").trim();

        // Формат: DATE | QUESTION | ANSWER | RESULT
        String line = String.format("%s | %s | %s | %s", date, safeQ, safeA, result);

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(HISTORY_FILE, true), StandardCharsets.UTF_8))) {
            pw.println(line);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<HistoryEntry> loadHistory() {
        List<HistoryEntry> list = new ArrayList<>();
        File file = new File(HISTORY_FILE);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\s\\|\\s");
                if (parts.length >= 4) {
                    list.add(0, new HistoryEntry(parts[0], parts[1], parts[2], parts[3])); // Добавляем в начало (новые сверху)
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}