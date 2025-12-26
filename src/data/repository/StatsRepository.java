package data.repository;

import data.FileService;
import model.Card;
import util.TextUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class StatsRepository {
    private static final Logger LOGGER = Logger.getLogger(StatsRepository.class.getName());
    private static final String STATS_FILE = "anki_stats.txt";
    private final FileService fileService = new FileService();
    private final Path path = Paths.get(STATS_FILE);

    public Map<String, Integer> loadStats() {
        Map<String, Integer> stats = new HashMap<>();
        List<String> lines = fileService.readAllLines(path);
        for (String line : lines) {
            try {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    stats.put(parts[0], Integer.parseInt(parts[1]));
                }
            } catch (NumberFormatException ignored) {}
        }
        return stats;
    }

    public void saveStats(List<Card> cards) {
        // Теперь сохраняем UUID вместо хэша
        String content = cards.stream()
                .filter(c -> !c.isNew())
                .map(c -> c.getId() + "|" + c.getLevel())
                .collect(Collectors.joining(System.lineSeparator()));

        try {
            java.nio.file.Files.writeString(path, content);
        } catch (java.io.IOException e) {
            LOGGER.severe("Ошибка сохранения статистики: " + e.getMessage());
        }
    }

    // Старый метод нужен только для миграции
    public String generateLegacyId(String question) {
        return String.valueOf(TextUtil.normalizeForId(question).hashCode());
    }
}