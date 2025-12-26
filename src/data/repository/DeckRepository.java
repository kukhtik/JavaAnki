package data.repository;

import data.FileService;
import model.Card;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
        LOGGER.info(">>> НАЧАЛО ЗАГРУЗКИ КОЛОД...");
        List<Card> allCards = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(DECKS_DIR))) {
            List<Path> fileList = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList();

            for (Path path : fileList) {
                allCards.addAll(parseFile(path));
            }
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
        // Используем имя файла как категорию по умолчанию, если не указана
        String currentCat = fileName.replace(".txt", "");

        StringBuilder qBuf = new StringBuilder();
        StringBuilder aBuf = new StringBuilder();
        String currentId = null; // Буфер для ID
        int state = 0; // 0-Meta, 1-Quest, 2-Ans

        for (String rawLine : lines) {
            String line = rawLine.replace("\uFEFF", "").trim();
            if (line.equals("===")) {
                if (qBuf.length() > 0 && aBuf.length() > 0) {
                    cards.add(new Card(currentId, currentCat, qBuf.toString().trim(), aBuf.toString().trim(), fileName));
                }
                qBuf.setLength(0); aBuf.setLength(0); currentId = null; state = 0; continue;
            }

            if (line.startsWith("ID:")) {
                currentId = line.substring(3).trim();
            } else if (line.startsWith("CATEGORY:")) {
                currentCat = line.substring(9).trim();
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
        // Хвост файла
        if (qBuf.length() > 0 && aBuf.length() > 0) {
            cards.add(new Card(currentId, currentCat, qBuf.toString().trim(), aBuf.toString().trim(), fileName));
        }

        return cards;
    }

    /**
     * Перезаписывает файл колоды полностью.
     * Используется для сохранения сгенерированных UUID (миграция) или при изменении карт.
     */
    public void saveDeck(String fileName, List<Card> cards) {
        Path path = Paths.get(DECKS_DIR, fileName);
        StringBuilder sb = new StringBuilder();

        for (Card c : cards) {
            sb.append("ID: ").append(c.getId()).append(System.lineSeparator());
            sb.append("CATEGORY: ").append(c.getCategory()).append(System.lineSeparator());
            sb.append("QUESTION:").append(System.lineSeparator()).append(c.getQuestion()).append(System.lineSeparator());
            sb.append("ANSWER:").append(System.lineSeparator()).append(c.getAnswer()).append(System.lineSeparator());
            sb.append("===").append(System.lineSeparator());
        }

        try {
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            LOGGER.info("Файл обновлен (UUID сохранены): " + fileName);
        } catch (IOException e) {
            LOGGER.severe("Ошибка записи файла " + fileName + ": " + e.getMessage());
        }
    }
}