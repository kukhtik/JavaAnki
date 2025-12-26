package service;

import data.FileService;
import util.TextUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public class ImportService {
    private static final Logger LOGGER = Logger.getLogger(ImportService.class.getName());
    private static final String IMPORT_FILE = "import.txt";
    private static final String DECKS_DIR = "decks";

    private final FileService fileService = new FileService();
    private final StudyService studyService;

    public ImportService(StudyService studyService) {
        this.studyService = studyService;
    }

    public String performImport() {
        // 1. Принудительная перезагрузка перед импортом
        // Это гарантирует, что мы знаем актуальное состояние файлов на диске (вдруг пользователь удалил файл)
        // И также гарантирует, что у всех карт в памяти уже есть UUID
        studyService.reloadSession();

        Path importPath = Paths.get(IMPORT_FILE);
        if (!Files.exists(importPath)) return "Файл import.txt не найден.";

        List<String> lines = fileService.readAllLines(importPath);
        if (lines.isEmpty()) return "Файл import.txt пуст.";

        // Загружаем существующие хэши текстов (чтобы не добавить дубликат по смыслу)
        Set<String> existingContentHashes = new HashSet<>();
        studyService.getAllCards().forEach(c ->
                existingContentHashes.add(TextUtil.normalizeForId(c.getQuestion()))
        );

        List<String> linesToKeep = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();

        int added = 0;
        int skipped = 0;
        int errors = 0;

        for (String line : lines) {
            String tr = line.trim();
            if (tr.equals("===")) {
                if (!currentBlock.isEmpty()) {
                    int res = processBlock(currentBlock, existingContentHashes);
                    if (res == 1) added++;
                    else if (res == 2) skipped++;
                    else {
                        linesToKeep.addAll(currentBlock);
                        linesToKeep.add("===");
                        errors++;
                    }
                }
                currentBlock.clear();
            } else {
                currentBlock.add(line);
            }
        }
        // Обработка последнего блока, если нет === в конце
        if (!currentBlock.isEmpty()) {
            if (currentBlock.stream().anyMatch(s -> !s.trim().isEmpty())) {
                linesToKeep.addAll(currentBlock);
                errors++;
            }
        }

        fileService.ensureDirectory(Paths.get(DECKS_DIR));

        // Перезаписываем import.txt (удаляем успешно импортированные)
        try {
            if (linesToKeep.isEmpty()) {
                Files.write(importPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.write(importPath, linesToKeep, java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка обновления import.txt: " + e.getMessage());
        }

        // Обновляем сессию, чтобы новые карты появились
        studyService.reloadSession();

        return String.format("Импорт завершен.\nДобавлено: %d\nПропущено (дубликаты): %d\nОшибок (осталось в файле): %d", added, skipped, errors);
    }

    /**
     * @return 1=Success, 2=Duplicate, 0=Error
     */
    private int processBlock(List<String> buffer, Set<String> existingHashes) {
        String cat = null, q = null, a = null;
        StringBuilder qB = new StringBuilder();
        StringBuilder aB = new StringBuilder();
        int state = 0;

        for (String line : buffer) {
            String tr = line.trim();
            if (tr.startsWith("CATEGORY:")) { cat = tr.substring(9).trim(); state = 0; }
            else if (tr.startsWith("QUESTION:")) state = 1;
            else if (tr.startsWith("ANSWER:")) state = 2;
            else {
                if (state == 1) qB.append(line).append("\n");
                if (state == 2) aB.append(line).append("\n");
            }
        }
        if (qB.length() > 0) q = qB.toString().trim();
        if (aB.length() > 0) a = aB.toString().trim();

        if (cat == null || q == null || q.isEmpty() || a == null || a.isEmpty()) return 0;

        // Проверяем дубликат по ТЕКСТУ вопроса
        String contentHash = TextUtil.normalizeForId(q);
        if (existingHashes.contains(contentHash)) return 2;

        String safeName = cat.replaceAll("[^a-zA-Z0-9а-яА-Я ._-]", "");
        Path deckPath = Paths.get(DECKS_DIR, safeName + ".txt");

        // ГЕНЕРИРУЕМ НОВЫЙ UUID
        String newId = UUID.randomUUID().toString();

        // Записываем карту с полем ID
        String entry = String.format("ID: %s%nCATEGORY: %s%nQUESTION:%n%s%nANSWER:%n%s%n===%n",
                newId, cat, q, a);

        fileService.appendLine(deckPath, entry);

        existingHashes.add(contentHash);
        return 1;
    }
}