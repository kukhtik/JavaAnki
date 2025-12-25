package data.repository;

import data.FileService;
import model.Card;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
/**
 * Репозиторий для загрузки карточек
 * <p>
 * Сканирует папку {@code decks/}, находит все файлы с расширением {@code .txt}
 * и преобразует их в объекты {@link Card}
 * <p>Формат файла поддерживает следующую разметку:
 * <pre>
 * ===
 * CATEGORY: категория
 * QUESTION:
 * вопрос
 * ANSWER:
 * ответ
 * ===
 * </pre>
 *
 * Каждый блок, разделённый {@code ===}, превращается в одну карточку
 */
public class DeckRepository {
    private static final Logger LOGGER = Logger.getLogger(DeckRepository.class.getName());
    private static final String DECKS_DIR = "decks";

    private final FileService fileService;
    /**
     * Создаёт репозиторий и гарантирует существование папки {@code decks/}
     */
    public DeckRepository() {
        this.fileService = new FileService();
        this.fileService.ensureDirectory(Paths.get(DECKS_DIR));
    }

    /**
     * Загружает все карточки из всех файлов {@code .txt} в папке {@code decks/}
     *
     * @return список всех найденных карточек; пустой список при ошибке
     */
    public List<Card> loadAllCards() {
        try (Stream<Path> paths = Files.walk(Paths.get(DECKS_DIR))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .flatMap(this::parseFile) // Преобразуем файл в поток карт
                    .toList(); // Java 16+: вместо .collect(Collectors.toList())
        } catch (Exception e) {
            LOGGER.severe("Ошибка сканирования папки decks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** * Парсит один файл колоды и преобразует его в поток карточек
     * <p>
     *     Используется простая state-machine:
     *     <ul>
     *         <li>0 - ожидание метаданных или начала вопроса</li>
     *         <li>1 - сбор текста вопроса</li>
     *         <li>2 - сбор текста ответа</li>
     *     </ul>
     *     @param path путь к файлу * @return поток карточек, найденных в файле
     *     */
    private Stream<Card> parseFile(Path path) {
        List<String> lines = fileService.readAllLines(path);
        List<Card> cards = new ArrayList<>();

        String fileName = path.getFileName().toString();
        String currentCat = "General";
        StringBuilder qBuf = new StringBuilder();
        StringBuilder aBuf = new StringBuilder();

        // 0=meta, 1=question, 2=answer
        int state = 0;

        for (String rawLine : lines) {
            // Удаляем BOM и пробелы
            var line = rawLine.replace("\uFEFF", "").trim();

            if (line.equals("===")) {
                if (qBuf.length() > 0 && aBuf.length() > 0) {
                    cards.add(new Card(
                            currentCat,
                            qBuf.toString().trim(),
                            aBuf.toString().trim(),
                            fileName
                    ));
                }
                qBuf.setLength(0);
                aBuf.setLength(0);
                state = 0;
                continue;
            }

            if (line.startsWith("CATEGORY:")) {
                currentCat = line.substring("CATEGORY:".length()).trim();
                state = 0;
            } else if (line.startsWith("QUESTION:")) {
                state = 1;
            } else if (line.startsWith("ANSWER:")) {
                state = 2;
            } else {
                if (state == 1) qBuf.append(rawLine).append("\n");
                else if (state == 2) aBuf.append(rawLine).append("\n");
            }
        }

        // "хвост" файла, если не было закрывающего ===
        if (qBuf.length() > 0 && aBuf.length() > 0) {
            cards.add(new Card(currentCat, qBuf.toString().trim(), aBuf.toString().trim(), fileName));
        }

        return cards.stream();
    }
}