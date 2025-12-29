package service;

import data.FileService;
import model.Card;
import util.CardParser;
import util.TextUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Сервис для массового импорта карточек из единого текстового файла.
 * <p>
 * Позволяет пользователю добавить много карточек сразу, заполнив файл {@code import.txt}.
 * Разберет их, проверит на дубликаты и разложит по нужным файлам
 * в папке {@code decks/}, основываясь на поле {@code CATEGORY}
 * </p>
 */
public record ImportService(StudyService studyService, FileService fileService) {
    private static final Logger LOGGER = Logger.getLogger(ImportService.class.getName());

    /**
     * Файл-источник для импорта
     */
    private static final String IMPORT_FILE = "import.txt";

    /**
     * Целевая директория, куда будут разложены новые карточки
     */
    private static final String DECKS_DIR = "decks";

    /**
     * Конструктор с внедрением зависимостей
     *
     * @param studyService перезагрузка сессии после импорта и проверки дубликатов
     * @param fileService работа с файловой системой
     */
    public ImportService {
    }

    /**
     * Выполняет процедуру импорта.
     * <p>
     * Алгоритм работы:
     * <ol>
     *     <li>Считывает все карточки из {@code import.txt}</li>
     *     <li>Загружает текущие карточки приложения для проверки на дубликаты</li>
     *     <li>Проходит по блокам из файла импорта:
     *         <ul>
     *             <li>Если блок некорректен (ошибка парсинга) - оставляет его в списке {@code linesToKeep}</li>
     *             <li>Если это дубликат - пропускает (считает как {@code skipped})</li>
     *             <li>Если карточка валидна и уникальна - записывает её (append)
     *             в соответствующий файл {@code decks/[CATEGORY].txt}</li>
     *         </ul>
     *     </li>
     *     <li>Перезаписывает файл {@code import.txt}, оставляя в нем только те блоки,
     *     которые НЕ удалось импортировать (ошибки). Успешные удаляются</li>
     *     <li>Перезагружает сессию {@code studyService}, чтобы новые карты появились в интерфейсе</li>
     * </ol>
     * </p>
     *
     * @return текстовый отчет о результатах (сколько добавлено, пропущено, ошибок)
     */
    public String performImport() {
        // актуализируем данные перед проверкой дубликатов
        studyService.reloadSession();

        Path importPath = Paths.get(IMPORT_FILE);
        if (!Files.exists(importPath)) return "Файл import.txt не найден";

        List<String> lines = fileService.readAllLines(importPath);
        if (lines.isEmpty()) return "Файл import.txt пуст";

        // хеш-сет существующих вопросов для быстрой O(1) проверки на дубликаты
        Set<String> existingContentHashes = new HashSet<>();
        studyService.getAllCards().forEach(c ->
                existingContentHashes.add(TextUtil.normalizeForId(c.getQuestion()))
        );

        List<String> linesToKeep = new ArrayList<>(); // блоки с ошибками, чтобы вернуть их в файл
        List<String> currentBlock = new ArrayList<>(); // буфер текущей карточки

        int added = 0;
        int skipped = 0;
        int errors = 0;

        // парсинг файла построчно
        for (String line : lines) {
            if (line.trim().equals("===")) {
                // конец блока
                if (!currentBlock.isEmpty()) {
                    int res = processBlock(currentBlock, existingContentHashes);
                    if (res == 1) {
                        added++; // успех, блок не сохраняем в linesToKeep (удаляем из файла)
                    } else if (res == 2) {
                        skipped++; // дубликат, тоже удаляем из файла ( дубликат в импорте не нужон)
                    } else {
                        // ошибка парсинга (0). Сохраняем блок обратно в файл, для исправления
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

        // последний блока (если файл кончился не на "===")
        if (!currentBlock.isEmpty()) {
            if (currentBlock.stream().anyMatch(s -> !s.trim().isEmpty())) {
                int res = processBlock(currentBlock, existingContentHashes);
                if (res == 1) added++;
                else if (res == 2) skipped++;
                else {
                    linesToKeep.addAll(currentBlock);
                    errors++;
                }
            }
        }

        fileService.ensureDirectory(Paths.get(DECKS_DIR));

        // обновление файла import.txt (удаляем обработанное)
        try {
            if (linesToKeep.isEmpty()) {
                // очищаем файл
                fileService.overwrite(importPath, new ArrayList<>());
            } else {
                // перезаписываем файл, оставляя только ошибочные блоки
                fileService.overwrite(importPath, linesToKeep);
            }
        } catch (Exception e) {
            LOGGER.severe("Ошибка обновления import.txt: " + e.getMessage());
        }

        // финальная перезагрузка, чтобы подтянуть новые файлы
        studyService.reloadSession();

        return String.format("Импорт завершен.\nДобавлено: %d\nПропущено (дубликаты): %d\nОшибок (остались в файле): %d", added, skipped, errors);
    }

    /**
     * Обрабатывает один блок текста (потенциальную карточку).
     *
     * @param buffer список строк блока
     * @param existingHashes множество хешей уже существующих вопросов
     * @return код результата:
     * <ul>
     *     <li>0 - Ошибка парсинга (невалидный блок)</li>
     *     <li>1 - Успешно добавлено</li>
     *     <li>2 - Пропущено как дубликат</li>
     * </ul>
     */
    private int processBlock(List<String> buffer, Set<String> existingHashes) {
        // парсим блок
        Card tempCard = CardParser.parseSingleBlock(buffer, "import_temp");

        if (tempCard == null) {// не удалось распарсить
            // вывод первых строк блока, чтобы понять, что там сломалось
            String preview = buffer.stream().limit(3).collect(Collectors.joining(" | "));
            LOGGER.warning("Ошибка парсинга блока в импорте. Содержимое: " + preview);
            return 0;
        }

        // дубликат?
        String contentHash = TextUtil.normalizeForId(tempCard.getQuestion());
        if (existingHashes.contains(contentHash)) return 2;

        // целевой файл на основе категории
        String cat = tempCard.getCategory();
        // очистка имени файла от недопустимых символов
        String safeName = cat.replaceAll("[^a-zA-Z0-9а-яА-Я ._-]", "");
        Path deckPath = Paths.get(DECKS_DIR, safeName + ".txt");

        // формирование текста для записи в целевой файл
        String entry = String.format("ID: %s%nCATEGORY: %s%nQUESTION:%n%s%nANSWER:%n%s%n===%n",
                tempCard.getId(),
                tempCard.getCategory(),
                tempCard.getQuestion(),
                tempCard.getAnswer());

        fileService.appendLine(deckPath, entry);

        // хеш в локальный сет, чтобы внутри одного импорта тоже ловить дубликаты
        existingHashes.add(contentHash);

        return 1;
    }
}