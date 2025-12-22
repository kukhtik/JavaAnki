import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class DeckDistributor {

    private static final String OUTPUT_DIR = "decks";

    /**
     * Читает файл, проверяет дубликаты и распределяет карточки.
     * @param inputFile файл источника
     * @param existingQuestions набор уже существующих вопросов (чтобы не создавать дубли)
     */
    public void distribute(File inputFile, Set<String> existingQuestions) {
        if (!inputFile.exists()) {
            AppLogger.error("Файл для импорта не найден: " + inputFile.getAbsolutePath());
            return;
        }

        AppLogger.info("Начинаем импорт из: " + inputFile.getName());
        ensureDecksDir();

        List<String> linesToKeep = new ArrayList<>();

        // Локальный сет, чтобы отслеживать дубликаты ВНУТРИ самого файла импорта
        Set<String> currentBatchQuestions = new HashSet<>();

        try {
            List<String> lines = Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8);
            List<String> cardBuffer = new ArrayList<>();
            int lineCount = 0;

            // Счетчики
            int processed = 0; // Записано в файлы
            int skipped = 0;   // Дубликаты (удалены из файла, но не записаны)
            int failed = 0;    // Ошибки разметки (остались в файле)

            for (String rawLine : lines) {
                lineCount++;
                String line = rawLine.trim();

                if (line.equals("===")) {
                    if (!cardBuffer.isEmpty()) {
                        // === ВЫЗОВ ОБРАБОТКИ ===
                        int result = processCard(cardBuffer, lineCount, existingQuestions, currentBatchQuestions);

                        if (result == 1) {
                            processed++; // Успешно записан
                        } else if (result == 2) {
                            skipped++;   // Дубликат (успешно обработан удалением)
                        } else {
                            failed++;    // Ошибка, возвращаем в файл
                            linesToKeep.addAll(cardBuffer);
                            linesToKeep.add("===");
                        }
                    }
                    cardBuffer.clear();
                } else {
                    cardBuffer.add(rawLine);
                }
            }

            // Хвост файла
            if (!cardBuffer.isEmpty()) {
                if (cardBuffer.stream().anyMatch(s -> !s.trim().isEmpty())) {
                    AppLogger.error("Файл закончился без '==='. Блок оставлен в файле.");
                    linesToKeep.addAll(cardBuffer);
                    failed++;
                }
            }

            rewriteImportFile(inputFile, linesToKeep);

            AppLogger.info("=== ИТОГИ ИМПОРТА ===");
            AppLogger.info(String.format("Добавлено новых: %d", processed));
            AppLogger.info(String.format("Пропущено дубликатов: %d (удалены из import.txt)", skipped));
            AppLogger.info(String.format("Ошибок разметки: %d (остались в import.txt)", failed));

        } catch (IOException e) {
            AppLogger.error("Критическая ошибка импорта", e);
        }
    }

    /**
     * Возвращает код результата:
     * 0 - Ошибка (оставить в файле)
     * 1 - Успех (записано)
     * 2 - Дубликат (пропустить, но удалить из файла)
     */
    private int processCard(List<String> buffer, int lineNum, Set<String> globalExist, Set<String> localExist) {
        if (buffer.stream().allMatch(String::isEmpty)) return 1; // Пустой блок считаем обработанным

        String category = null;
        StringBuilder question = new StringBuilder();
        StringBuilder answer = new StringBuilder();
        int state = 0;

        for (String line : buffer) {
            String trimmed = line.trim();
            if (trimmed.startsWith("CATEGORY:")) {
                category = trimmed.substring("CATEGORY:".length()).trim();
                state = 0;
            } else if (trimmed.startsWith("QUESTION:")) { state = 1; }
            else if (trimmed.startsWith("ANSWER:")) { state = 2; }
            else {
                if (state == 1) question.append(line).append("\n");
                else if (state == 2) answer.append(line).append("\n");
            }
        }

        // 1. Валидация полей
        String qStr = question.toString().trim();
        String aStr = answer.toString().trim();

        if (category == null || category.isEmpty()) {
            AppLogger.error("Ошибка импорта: Нет CATEGORY (стр ~" + lineNum + ")");
            return 0;
        }
        if (qStr.isEmpty() || aStr.isEmpty()) {
            AppLogger.error("Ошибка импорта: Пустой вопрос/ответ (стр ~" + lineNum + ")");
            return 0;
        }

        // 2. Проверка на дубликаты
        String normalizedQ = qStr.toLowerCase().replaceAll("\\s+", "");

        if (globalExist.contains(normalizedQ)) {
            AppLogger.info("Пропущен дубликат (уже есть в колодах): " + qStr.replace("\n", " ").substring(0, Math.min(30, qStr.length())) + "...");
            return 2; // Код 2: Дубликат глобальный
        }

        if (localExist.contains(normalizedQ)) {
            AppLogger.info("Пропущен дубликат (повтор внутри import.txt): " + qStr.replace("\n", " ").substring(0, Math.min(30, qStr.length())) + "...");
            return 2; // Код 2: Дубликат локальный
        }

        // 3. Сохранение
        saveToFile(category, qStr, aStr);

        // Добавляем в локальный и глобальный сеты, чтобы следующие такие же вопросы тоже считались дублями
        localExist.add(normalizedQ);
        globalExist.add(normalizedQ);

        return 1; // Код 1: Успех
    }

    private void saveToFile(String category, String question, String answer) {
        String safeName = category.replaceAll("[^a-zA-Z0-9а-яА-Я ._-]", "").trim();
        if (safeName.isEmpty()) safeName = "Uncategorized";
        Path path = Paths.get(OUTPUT_DIR, safeName + ".txt");

        StringBuilder sb = new StringBuilder();
        sb.append("CATEGORY: ").append(category).append(System.lineSeparator());
        sb.append("QUESTION:").append(System.lineSeparator()).append(question).append(System.lineSeparator());
        sb.append("ANSWER:").append(System.lineSeparator()).append(answer).append(System.lineSeparator());
        sb.append("===").append(System.lineSeparator());

        try {
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            AppLogger.error("Ошибка записи файла: " + safeName, e);
        }
    }

    private void rewriteImportFile(File file, List<String> content) {
        try {
            if (content.isEmpty()) {
                Files.write(file.toPath(), new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.write(file.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (IOException e) { AppLogger.error("Ошибка обновления import.txt", e); }
    }

    private void ensureDecksDir() {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdir();
    }
}