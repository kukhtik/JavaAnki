package service;

import data.FileService;
import model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестирование сервиса импорта")
class ImportServiceTest {

    private MemoryFileService fileService;
    private StubStudyService studyService;
    private ImportService importService;

    @BeforeEach
    void setUp() {
        fileService = new MemoryFileService();
        studyService = new StubStudyService();
        importService = new ImportService(studyService, fileService);
    }

    @Test
    @DisplayName("Импорт новой карты: должен создать файл и очистить import.txt")
    void testImportNewCard() {
        // Arrange
        List<String> importContent = Arrays.asList(
                "CATEGORY: Test Category",
                "QUESTION:", "Q1",
                "ANSWER:", "A1",
                "==="
        );
        fileService.files.put("import.txt", new ArrayList<>(importContent));

        // Act
        String result = importService.performImport();

        // Assert
        assertTrue(result.contains("Добавлено: 1"), "Отчет должен содержать 'Добавлено: 1'");

        // Проверяем, что файл в памяти очистился (благодаря вызову fileService.overwrite)
        assertTrue(fileService.files.get("import.txt").isEmpty(),
                "Файл import.txt должен быть пуст после успеха. (Проверьте, что ImportService вызывает fileService.overwrite!)");

        // Проверяем создание нового файла
        Path expectedPath = Paths.get("decks", "Test Category.txt");
        assertTrue(fileService.files.containsKey(expectedPath.toString()),
                "Файл decks/Test Category.txt должен быть создан");
    }

    @Test
    @DisplayName("Импорт дубликата: должен пропустить и очистить import.txt")
    void testImportDuplicateCard() {
        // Arrange
        Card existingCard = new Card();
        existingCard.setQuestion("Existing Question");
        studyService.existingCards.add(existingCard);

        fileService.files.put("import.txt", new ArrayList<>(Arrays.asList(
                "CATEGORY: Duplicates",
                "QUESTION:", "Existing Question",
                "ANSWER:", "A",
                "==="
        )));

        // Act
        String result = importService.performImport();

        // Assert
        assertTrue(result.contains("Пропущено (дубликаты): 1"));
        assertTrue(fileService.files.get("import.txt").isEmpty(), "Дубликат должен удаляться из import.txt");
    }

    @Test
    @DisplayName("Импорт невалидного блока: должен оставить его в файле")
    void testImportInvalidBlock() {
        // Arrange: Блок без поля QUESTION
        List<String> invalidContent = Arrays.asList("CATEGORY: Invalid", "ANSWER: A", "===");
        fileService.files.put("import.txt", new ArrayList<>(invalidContent));

        // Act
        String result = importService.performImport();

        // Assert
        // ИСПРАВЛЕНО: ищем актуальную строку из вашего ImportService
        assertTrue(result.contains("Ошибок (остались в файле): 1") || result.contains("Ошибок: 1"),
                "Сообщение об ошибке не найдено. Результат: " + result);

        assertEquals(invalidContent, fileService.files.get("import.txt"),
                "Невалидный блок должен остаться в памяти");
    }

    // =============================================================================================
    // --- ЗАГЛУШКИ ---
    // =============================================================================================

    static class MemoryFileService extends FileService {
        public Map<String, List<String>> files = new HashMap<>();

        @Override
        public List<String> readAllLines(Path path) {
            return files.getOrDefault(path.toString(), new ArrayList<>());
        }

        @Override
        public void appendLine(Path path, String line) {
            files.computeIfAbsent(path.toString(), _ -> new ArrayList<>()).add(line);
        }

        @Override
        public void ensureDirectory(Path dir) { }

        // ВАЖНО: Этот метод должен быть в базовом классе FileService!
        @Override
        public void overwrite(Path path, List<String> lines) {
            // В тесте мы просто обновляем HashMap, а не пишем на диск
            files.put(path.toString(), new ArrayList<>(lines));
        }

        @Override
        public void write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) {

        }
    }

    static class StubStudyService extends StudyService {
        public List<Card> existingCards = new ArrayList<>();
        public StubStudyService() { super(null); }
        @Override public void reloadSession() {}
        @Override public List<Card> getAllCards() { return existingCards; }
    }
}