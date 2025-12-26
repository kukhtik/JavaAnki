package service;

import data.FileService;
import model.Card;
import util.CardParser;
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
        // 1. Перезагрузка состояния с диска (защита от ручного удаления файлов)
        studyService.reloadSession();

        Path importPath = Paths.get(IMPORT_FILE);
        if (!Files.exists(importPath)) return "Файл import.txt не найден.";

        List<String> lines = fileService.readAllLines(importPath);
        if (lines.isEmpty()) return "Файл import.txt пуст.";

        // Загружаем хэши текстов существующих вопросов
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
            // Ищем разделитель блоков
            if (line.trim().equals("===")) {
                if (!currentBlock.isEmpty()) {
                    int res = processBlock(currentBlock, existingHashes);
                    if (res == 1) added++;
                    else if (res == 2) skipped++;
                    else {
                        // Ошибка (например, нет поля QUESTION), оставляем блок в файле
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

        // Обработка "хвоста" файла (если забыли === в конце)
        if (!currentBlock.isEmpty()) {
            if (currentBlock.stream().anyMatch(s -> !s.trim().isEmpty())) {
                // Пытаемся обработать последний блок тоже
                int res = processBlock(currentBlock, existingHashes);
                if (res == 1) added++;
                else if (res == 2) skipped++;
                else {
                    linesToKeep.addAll(currentBlock);
                    errors++;
                }
            }
        }

        fileService.ensureDirectory(Paths.get(DECKS_DIR));

        // Перезаписываем import.txt (удаляем успешные, оставляем ошибки)
        try {
            if (linesToKeep.isEmpty()) {
                Files.write(importPath, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                Files.write(importPath, linesToKeep, java.nio.charset.StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка обновления import.txt: " + e.getMessage());
        }

        // Обновляем сессию еще раз, чтобы новые карты появились в обучении
        studyService.reloadSession();

        return String.format("Импорт завершен.\nДобавлено: %d\nПропущено (дубликаты): %d\nОшибок: %d", added, skipped, errors);
    }

    /**
     * Обрабатывает один блок строк.
     * @return 1=Успех, 2=Дубликат, 0=Ошибка формата
     */
    private int processBlock(List<String> buffer, Set<String> existingHashes) {
        // 1. Используем CardParser + CardFactory
        // Парсер вернет объект Card с уже сгенерированным UUID (так как в import.txt нет поля ID)
        Card tempCard = CardParser.parseSingleBlock(buffer, "import_temp");

        if (tempCard == null) return 0; // Невалидный блок (нет вопроса или ответа)

        // 2. Проверка на дубликат по ТЕКСТУ
        String contentHash = TextUtil.normalizeForId(tempCard.getQuestion());
        if (existingHashes.contains(contentHash)) return 2;

        // 3. Определяем путь к файлу
        String cat = tempCard.getCategory();
        String safeName = cat.replaceAll("[^a-zA-Z0-9а-яА-Я ._-]", "");
        Path deckPath = Paths.get(DECKS_DIR, safeName + ".txt");

        // 4. Формируем запись. Берем UUID, который сгенерировала Фабрика внутри Парсера.
        String entry = String.format("ID: %s%nCATEGORY: %s%nQUESTION:%n%s%nANSWER:%n%s%n===%n",
                tempCard.getId(),
                tempCard.getCategory(),
                tempCard.getQuestion(),
                tempCard.getAnswer());

        // 5. Записываем
        fileService.appendLine(deckPath, entry);

        // Добавляем хэш в локальный сет, чтобы не добавить дубликат внутри того же импорта
        existingHashes.add(contentHash);
        return 1;
    }
}