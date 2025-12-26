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

public class DeckRepository {
    private static final Logger LOGGER = Logger.getLogger(DeckRepository.class.getName());
    private static final String DECKS_DIR = "decks";
    private final FileService fileService;

    public DeckRepository() {
        this.fileService = new FileService();
        this.fileService.ensureDirectory(Paths.get(DECKS_DIR));
    }

    public List<Card> loadAllCards() {
        LOGGER.info(">>> НАЧАЛО ЗАГРУЗКИ КОЛОД (папка " + DECKS_DIR + ")");
        List<Card> allCards = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(DECKS_DIR))) {
            List<Path> fileList = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList(); // Собираем список файлов

            for (Path path : fileList) {
                List<Card> fromFile = parseFile(path);
                allCards.addAll(fromFile);
            }

            LOGGER.info(">>> ЗАГРУЗКА ЗАВЕРШЕНА. Найдено сырых карт: " + allCards.size());
            return allCards;
        } catch (Exception e) {
            LOGGER.severe("Критическая ошибка чтения decks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Card> parseFile(Path path) {
        List<String> lines = fileService.readAllLines(path);
        List<Card> cards = new ArrayList<>();

        String fileName = path.getFileName().toString();
        String currentCat = fileName.replace(".txt", "");
        StringBuilder qBuf = new StringBuilder();
        StringBuilder aBuf = new StringBuilder();
        int state = 0;

        for (String rawLine : lines) {
            String line = rawLine.replace("\uFEFF", "").trim();
            if (line.equals("===")) {
                if (qBuf.length() > 0 && aBuf.length() > 0) {
                    cards.add(new Card(currentCat, qBuf.toString().trim(), aBuf.toString().trim(), fileName));
                }
                qBuf.setLength(0); aBuf.setLength(0); state = 0; continue;
            }
            if (line.startsWith("CATEGORY:")) { currentCat = line.substring(9).trim(); state = 0; }
            else if (line.startsWith("QUESTION:")) state = 1;
            else if (line.startsWith("ANSWER:")) state = 2;
            else {
                if (state == 1) qBuf.append(rawLine).append("\n");
                else if (state == 2) aBuf.append(rawLine).append("\n");
            }
        }
        if (qBuf.length() > 0 && aBuf.length() > 0) {
            cards.add(new Card(currentCat, qBuf.toString().trim(), aBuf.toString().trim(), fileName));
        }

        // ЛОГИРОВАНИЕ ПО ФАЙЛУ
        LOGGER.info(String.format("Файл: %-25s | Найдено карт: %d", fileName, cards.size()));
        return cards;
    }
}