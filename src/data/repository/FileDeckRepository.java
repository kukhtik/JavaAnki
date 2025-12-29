package data.repository;

import data.FileService;
import data.parser.TxtDeckParser;
import model.Card;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Реализация репозитория для хранения колод в файловой системе
 * <p>
 * Работает с текстовыми файлами (.txt) в папке {@code decks}
 * Этот класс отвечает за I/O операции (поиск файлов, чтение, запись)
 * </p>
 */
public class FileDeckRepository implements CardRepository {
    private static final Logger LOGGER = Logger.getLogger(FileDeckRepository.class.getName());

    /** Папка, в которой хранятся файлы колод */
    private static final String DECKS_DIR = "decks";

    /** Низкоуровневые операции с файлами (чтение строк, проверка путей) */
    private final FileService fileService;

    /** Парсер, преобразующий текст в объекты Card */
    private final TxtDeckParser parser;

    /**
     * Конструктор репозитория.
     * <p>
     * При создании инициализирует зависимости и гарантирует,
     * что папка {@code decks} существует (создает, если нет)
     * </p>
     */
    public FileDeckRepository() {
        this.fileService = new FileService() {
            @Override
            public void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {

            }
        };
        this.fileService.ensureDirectory(Paths.get(DECKS_DIR));
        this.parser = new TxtDeckParser();
    }

    /**
     * Сканирует папку {@code decks} и загружает все карточки из всех найденных .txt файлов
     *
     * @return общий список карточек. Если возникла ошибка или папка пуста — возвращает пустой список
     */
    @Override
    public List<Card> loadAllCards() {
        LOGGER.info(">>>>>> НАЧАЛО ЗАГРУЗКИ КОЛОД <<<<<<");
        List<Card> allCards = new ArrayList<>();

        // Files.walk для рекурсивного обхода (если вдруг будут подпапки)
        try (Stream<Path> paths = Files.walk(Paths.get(DECKS_DIR))) {
            List<Path> fileList = paths
                    .filter(Files::isRegularFile)
                    // ахтунг! поменять если другой формат файла
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList();

            for (Path path : fileList) {
                // строки из файла через FileService
                List<String> lines = fileService.readAllLines(path);
                String fileName = path.getFileName().toString();

                List<Card> cardsFromFile = parser.parse(lines, fileName);
                if (cardsFromFile.isEmpty()) {
                    LOGGER.warning("Файл пуст или имеет неверный формат: " + fileName);
                } else {
                    // Level.INFO, чтобы не захламлять лог, если много файлов
                    LOGGER.info("Файл загружен: " + fileName + " (Найдено карт: " + cardsFromFile.size() + ")");
                }
                allCards.addAll(cardsFromFile);
            }
            return allCards;
        } catch (Exception e) {
            LOGGER.severe("Критическая ошибка чтения папки decks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Список карточек обратно в текстовый файл
     * <p>
     * Метод формирует текстовое представление карточек в формате:
     * <pre>
     * ID: блабла
     * CATEGORY: блабла
     * QUESTION: блабла
     * ANSWER: блабла
     * ===
     * </pre>
     * И перезаписывает файл целиком.
     * </p>
     *
     * @param fileName имя файла (например, "OPP.txt")
     * @param cards список карточек для записи
     */
    @Override
    public void saveDeck(String fileName, List<Card> cards) {
        Path path = Paths.get(DECKS_DIR, fileName);
        StringBuilder sb = new StringBuilder();

        // формирование текстового содержимого файла
        for (Card c : cards) {
            sb.append("ID: ").append(c.getId()).append(System.lineSeparator());
            sb.append("CATEGORY: ").append(c.getCategory()).append(System.lineSeparator());
            // переносы строк вокруг многострочных полей
            sb.append("QUESTION:").append(System.lineSeparator()).append(c.getQuestion()).append(System.lineSeparator());
            sb.append("ANSWER:").append(System.lineSeparator()).append(c.getAnswer()).append(System.lineSeparator());
            // разделитель карточек
            sb.append("===").append(System.lineSeparator());
        }

        try {
            // запись в файл
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.severe("Ошибка записи файла " + fileName + ": " + e.getMessage());
        }
    }
}