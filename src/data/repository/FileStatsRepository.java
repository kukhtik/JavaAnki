package data.repository;

import data.FileService;
import model.Card;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Репозиторий для сохранения прогресса обучения в файл
 * <p>
 * Хранит связь между уникальным идентификатором карточки (UUID) и её текущим уровнем освоения (Box/Level).
 * Данные сохраняются в простой текстовый файл {@code anki_stats.txt}
 * </p>
 */
public class FileStatsRepository implements StatsRepository {
    private static final Logger LOGGER = Logger.getLogger(FileStatsRepository.class.getName());

    /** Имя файла для статистики */
    private static final String STATS_FILE = "anki_stats.txt";

    private final FileService fileService = new FileService() {
        @Override
        public void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {

        }
    };
    private final Path path = Paths.get(STATS_FILE);

    /**
     * Загружает статистику из файла в память.
     * <p>
     * Считывает файл построчно, разбивает строку по символу {@code |} и формирует карту.
     * Если строка повреждена (не число, нет разделителя), она игнорируется
     * </p>
     *
     * @return Map, где ключ = ID карточки (String), значение = уровень обучения (Integer)
     */
    @Override
    public Map<String, Integer> loadStats() {
        Map<String, Integer> stats = new HashMap<>();
        List<String> lines = fileService.readAllLines(path);

        for (String line : lines) {
            try {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    String cardId = parts[0];
                    int level = Integer.parseInt(parts[1]);
                    stats.put(cardId, level);
                }
            } catch (NumberFormatException ignored) {
                // строки с некорректным форматом числа игнорируем, чтобы не крашится
                LOGGER.warning("Пропущена некорректная строка статистики: " + line);
            }
        }
        return stats;
    }

    /**
     * Сохраняет текущее состояние прогресса для переданного списка карточек
     * <p>
     * Метод фильтрует только те карточки, которые уже находятся в процессе изучения (не {@code isNew()}).
     * Новые карточки (уровень 0) не сохраняются, чтобы экономить место, так как 0 - это значение по умолчанию
     * </p>
     *
     * @param cards список карточек приложения (с актуальными уровнями)
     */
    @Override
    public void saveStats(List<Card> cards) {
        // список карточек в строку формата CSV (pipe-separated)
        String content = cards.stream()
                // сохраняем только карточки, прогресс по которым отличается от начального
                .filter(c -> !c.isNew())
                .map(c -> c.getId() + "|" + c.getLevel())
                .collect(Collectors.joining(System.lineSeparator()));

        try {
            // перезапись файла статистики
            java.nio.file.Files.writeString(path, content);
        } catch (java.io.IOException e) {
            LOGGER.severe("Критическая ошибка сохранения статистики: " + e.getMessage());
        }
    }
}