package data.repository;

import data.FileService;
import model.Card;
import util.TextUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsRepository {
    private static final String STATS_FILE = "anki_stats.txt";
    private final FileService fileService = new FileService();
    private final Path path = Paths.get(STATS_FILE);

    /**
     * Загрузка статистики в map
     */
    public Map<String, Integer> loadStats() {
        Map<String, Integer> stats = new HashMap<>();
        List<String> lines = fileService.readAllLines(path);

        for (String line : lines) {
            try {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    stats.put(parts[0], Integer.parseInt(parts[1]));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return stats;
    }

    /**
     * Сохраняет уровень карт
     */
    public void saveStats(List<Card> cards) {
        // строки HASHCODE|LEVEL
        String content = cards.stream()
                .filter(c -> !c.isNew()) // только то, что уже попадалось
                .map(c -> {
                    String id = generateId(c.getQuestion());
                    return id + "|" + c.getLevel();
                })
                .collect(Collectors.joining(System.lineSeparator()));
        try {
            java.nio.file.Files.writeString(path, content);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
    public String generateId(String question){
        return String.valueOf(TextUtil.normalizedForId(question).hashCode());
    }
}
