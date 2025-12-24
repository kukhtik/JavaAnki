import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class GroupManager {
    private final Map<String, List<String>> groups = new HashMap<>();

    public GroupManager() {
        loadStructure();
    }

    private void loadStructure() {
        File file = new File("decks/structure.txt");
        if (!file.exists()) return;

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            String currentGroup = null;

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("GROUP:")) {
                    currentGroup = line.substring("GROUP:".length()).trim();
                    groups.putIfAbsent(currentGroup, new ArrayList<>());
                } else if (line.startsWith("FILES:")) {
                    if (currentGroup != null) {
                        String filesRaw = line.substring("FILES:".length()).trim();
                        String[] files = filesRaw.split(",");
                        for (String f : files) {
                            // Сохраняем имя файла в нижнем регистре и без пробелов для надежного сравнения
                            groups.get(currentGroup).add(f.trim().toLowerCase());
                        }
                    }
                }
            }
            AppLogger.info("Загружено групп тем: " + groups.size());
        } catch (Exception e) {
            AppLogger.error("Ошибка чтения structure.txt", e);
        }
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    /**
     * Проверяет, входит ли карта (по имени файла) в указанную группу.
     */
    public boolean isCardInGroup(Card card, String groupName) {
        if (!groups.containsKey(groupName)) return false;
        if (card.sourceFile == null) return false;

        String src = card.sourceFile.trim().toLowerCase();
        for (String fileInGroup : groups.get(groupName)) {
            if (src.equals(fileInGroup)) return true;
        }
        return false;
    }
}