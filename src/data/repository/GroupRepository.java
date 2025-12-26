package data.repository;

import data.FileService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

public class GroupRepository {
    private static final Logger LOGGER = Logger.getLogger(GroupRepository.class.getName());
    private static final String STRUCTURE_FILE = "decks/structure.txt";
    private final FileService fileService = new FileService();

    // Map: Имя группы -> Список файлов (в нижнем регистре)
    private final Map<String, List<String>> groups = new HashMap<>();

    public GroupRepository() {
        loadStructure();
    }

    public void loadStructure() {
        groups.clear();
        Path path = Paths.get(STRUCTURE_FILE);
        List<String> lines = fileService.readAllLines(path);

        if (lines.isEmpty()) {
            LOGGER.warning("Файл structure.txt не найден или пуст. Группировка отключена.");
            return;
        }

        String currentGroup = null;
        for (String line : lines) {
            String tr = line.trim();
            if (tr.isEmpty()) continue;

            if (tr.startsWith("GROUP:")) {
                currentGroup = tr.substring(6).trim();
                groups.putIfAbsent(currentGroup, new ArrayList<>());
            } else if (tr.startsWith("FILES:") && currentGroup != null) {
                String filesRaw = tr.substring(6).trim();
                String[] files = filesRaw.split(",");
                for (String f : files) {
                    groups.get(currentGroup).add(f.trim().toLowerCase());
                }
            }
        }
        LOGGER.info("Загружено групп тем: " + groups.size() + " (" + groups.keySet() + ")");
    }

    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    public boolean isCardInGroup(String sourceFile, String groupName) {
        if (!groups.containsKey(groupName) || sourceFile == null) return false;
        String fileName = sourceFile.toLowerCase().trim();
        return groups.get(groupName).contains(fileName);
    }
}