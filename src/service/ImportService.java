package service;

import data.FileService;
import model.Card;
import service.session.SessionManager; // Используем SessionManager для доступа к данным
import util.CardParser;
import util.CardFactory;
import util.TextUtil;

import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public class ImportService {
    private static final Logger LOGGER = Logger.getLogger(ImportService.class.getName());
    private static final String IMPORT_FILE = "import.txt";
    private static final String DECKS_DIR = "decks";

    private final FileService fileService = new FileService();
    // Зависим от SessionManager, чтобы получить текущие данные,
    // и от StudyService, чтобы триггернуть обновление UI
    private final StudyService studyService;

    public ImportService(StudyService studyService) {
        this.studyService = studyService;
    }

    public String performImport() {
        studyService.reloadSession(); // Обновляем состояние через фасад

        Path importPath = Paths.get(IMPORT_FILE);
        if (!Files.exists(importPath)) return "Файл import.txt не найден.";

        List<String> lines = fileService.readAllLines(importPath);
        if (lines.isEmpty()) return "Файл import.txt пуст.";

        // Получаем данные через фасад
        Set<String> existingHashes = new HashSet<>();
        studyService.getAllCards().forEach(c ->
                existingHashes.add(TextUtil.normalizeForId(c.getQuestion()))
        );

        List<String> linesToKeep = new ArrayList<>();
        List<String> currentBlock = new ArrayList<>();

        int added = 0;
        int skipped = 0;
        int errors = 0;

        for (String line : lines) {
            if (line.trim().equals("===")) {
                if (!currentBlock.isEmpty()) {
                    int res = processBlock(currentBlock, existingHashes);
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
        if (!currentBlock.isEmpty()) {
            if (currentBlock.stream().anyMatch(s -> !s.trim().isEmpty())) {
                int res = processBlock(currentBlock, existingHashes);
                if (res == 1) added++; else if (res == 2) skipped++; else {
                    linesToKeep.addAll(currentBlock);
                    errors++;
                }
            }
        }

        fileService.ensureDirectory(Paths.get(DECKS_DIR));

        try {
            if (linesToKeep.isEmpty()) {
                Files.write(importPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.write(importPath, linesToKeep, java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка записи import.txt: " + e.getMessage());
        }

        studyService.reloadSession();

        return String.format("Импорт завершен.\nДобавлено: %d\nПропущено: %d\nОшибок: %d", added, skipped, errors);
    }

    private int processBlock(List<String> buffer, Set<String> existingHashes) {
        // ДЕЛЕГИРУЕМ ПАРСИНГ УТИЛИТЕ
        Card tempCard = CardParser.parseSingleBlock(buffer, "import_temp");
        if (tempCard == null) return 0;

        String contentHash = TextUtil.normalizeForId(tempCard.getQuestion());
        if (existingHashes.contains(contentHash)) return 2;

        String cat = tempCard.getCategory();
        String safeName = cat.replaceAll("[^a-zA-Z0-9а-яА-Я ._-]", "");
        Path deckPath = Paths.get(DECKS_DIR, safeName + ".txt");

        String entry = String.format("ID: %s%nCATEGORY: %s%nQUESTION:%n%s%nANSWER:%n%s%n===%n",
                tempCard.getId(), tempCard.getCategory(), tempCard.getQuestion(), tempCard.getAnswer());

        fileService.appendLine(deckPath, entry);
        existingHashes.add(contentHash);
        return 1;
    }
}