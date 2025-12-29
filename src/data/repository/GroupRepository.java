package data.repository;

import data.FileService;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;

/**
 * Репозиторий для управления логической группировкой колод
 * <p>
 * Позволяет объединять несколько физических файлов ( .txt) в одну группу
 * Это удобно, если хочется учить сразу группу, вопросы которой разбросаны, и если темы вопросов маленкие
 * </p>
 * <p>
 * <b>Конфигурация:</b><br>
 * Данные берутся из файла {@code decks/structure.txt}.<br>
 * <b>Формат файла:</b>
 * <pre>
 * GROUP: Название группы
 * FILES: файл1.txt, файл2.txt, файл3.txt
 * </pre>
 * </p>
 */
public class GroupRepository {
    private static final Logger LOGGER = Logger.getLogger(GroupRepository.class.getName());

    /** Путь к файлу конфигурации структуры */
    private static final String STRUCTURE_FILE = "decks/structure.txt";

    private final FileService fileService = new FileService() {
        @Override
        public void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {

        }
    };

    /**
     * Хранилище структуры групп в памяти
     */
    private final Map<String, List<String>> groups = new HashMap<>();

    /**
     * Создает репозиторий и пытается загрузить конфигурацию
     */
    public GroupRepository() {
        loadStructure();
    }

    /**
     * Читает файл конфигурации и заполняет карту групп
     * <p>
     * Парсер работает как простейший конечный автомат:
     * 1. Находит строку начинающуюся с {@code GROUP:}. Запоминает имя группы
     * 2. Находит строку {@code FILES:} под ней. Добавляет перечисленные файлы к текущей группе
     * </p>
     */
    public void loadStructure() {
        groups.clear();
        Path path = Paths.get(STRUCTURE_FILE);

        // FileService вернет пустой список, если файла нет, не выбрасывая исключение
        List<String> lines = fileService.readAllLines(path);

        if (lines.isEmpty()) {
            LOGGER.warning("Файл structure.txt не найден или пуст. Группировка колод отключена");
            return;
        }

        String currentGroup = null;
        for (String line : lines) {
            String tr = line.trim();
            if (tr.isEmpty()) continue; // пропуск пустых строк

            if (tr.startsWith("GROUP:")) {
                // определение новой группы
                currentGroup = tr.substring(6).trim();
                // список для этой группы, если его еще нет
                groups.putIfAbsent(currentGroup, new ArrayList<>());

            } else if (tr.startsWith("FILES:") && currentGroup != null) {
                // список файлов для текущей группы
                String filesRaw = tr.substring(6).trim();
                String[] files = filesRaw.split(",");

                for (String f : files) {
                    groups.get(currentGroup).add(f.trim().toLowerCase());
                }
            }
        }
        LOGGER.info("Загружено групп тем: " + groups.size() + " (" + groups.keySet() + ")");
    }

    /**
     * Возвращает список доступных названий групп.
     * Используется для заполнения выпадающего списка (ComboBox) в UI
     *
     * @return Набор имен групп.
     */
    public Set<String> getGroupNames() {
        return groups.keySet();
    }

    /**
     * Проверяет, принадлежит ли карточка из указанного файла к указанной группе
     *
     * @param sourceFile файл, из которого пришла карточка (например, "OOP.txt")
     * @param groupName группа, которую выбрал пользователь (например, "Java Core")
     * @return {@code true}, если файл входит в список файлов этой группы
     */
    public boolean isCardInGroup(String sourceFile, String groupName) {
        // такой группы нет или у карточки нет файла-источника? false
        if (!groups.containsKey(groupName) || sourceFile == null) return false;
        String fileName = sourceFile.toLowerCase().trim();

        return groups.get(groupName).contains(fileName);
    }
}