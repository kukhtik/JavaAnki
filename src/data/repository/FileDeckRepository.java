package data.repository;

import data.FileService;
import data.parser.TxtDeckParser; // Новый импорт
import model.Card;

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
    private final TxtDeckParser parser; // Внедряем парсер

    public FileDeckRepository() {
        this.fileService = new FileService();
        this.fileService.ensureDirectory(Paths.get(DECKS_DIR));
        this.parser = new TxtDeckParser(); // Создаем экземпляр парсера
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
                // 1. Читаем файл
                List<String> lines = fileService.readAllLines(path);
                String fileName = path.getFileName().toString();

                // 2. ДЕЛЕГИРУЕМ ПАРСИНГ
                List<Card> cardsFromFile = parser.parse(lines, fileName); // Используем TxtDeckParser
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