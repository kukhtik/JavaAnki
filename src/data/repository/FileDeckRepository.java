package data.repository;

import data.FileService;
import model.Card;
import util.CardParser; // Используем наш парсер

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FileDeckRepository implements CardRepository {
    private static final Logger LOGGER = Logger.getLogger(FileDeckRepository.class.getName());
    private static final String DECKS_DIR = "decks";
    private final FileService fileService;

    public FileDeckRepository() {
        this.fileService = new FileService();
        this.fileService.ensureDirectory(Paths.get(DECKS_DIR));
    }

    @Override
    public List<Card> loadAllCards() {
        LOGGER.info(">>> НАЧАЛО ЗАГРУЗКИ КОЛОД...");
        List<Card> allCards = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(Paths.get(DECKS_DIR))) {
            List<Path> fileList = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .toList();

            for (Path path : fileList) {
                // ДЕЛЕГИРУЕМ ПАРСИНГ
                List<String> lines = fileService.readAllLines(path);
                String fileName = path.getFileName().toString();

                List<Card> cardsFromFile = CardParser.parseAll(lines, fileName);
                allCards.addAll(cardsFromFile);
            }
            return allCards;
        } catch (Exception e) {
            LOGGER.severe("Ошибка чтения decks: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
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
        } catch (IOException e) {
            LOGGER.severe("Ошибка записи " + fileName + ": " + e.getMessage());
        }
    }
}