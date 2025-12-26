package service;

import data.FileService;
import util.TextUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ImportService {
    private static final String IMPORT_FILE = "import.txt";
    private static final String DECKS_DIR = "decks";

    private final FileService fileService = new FileService();
    private final StudyService studyService; // Нужно, чтобы проверить дубликаты

    public ImportService(StudyService studyService) {
        this.studyService = studyService;
    }

    public String performImport() {
        Path importPath = Paths.get(IMPORT_FILE);
        List<String> lines = fileService.readAllLines(importPath);

        if (lines.isEmpty()) return "Файл import.txt не найден или пуст.";

        Set<String> existingQuestions = new HashSet<>();
        // Загружаем хэши существующих вопросов, чтобы не создавать дубли
        studyService.getAllCards().forEach(c ->
                existingQuestions.add(TextUtil.normalizedForId(c.getQuestion()))
        );

        List<String> buffer = new ArrayList<>();
        List<String> failedBlocks = new ArrayList<>();

        int added = 0;
        int skipped = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.equals("===")) {
                if (!buffer.isEmpty()) {
                    if (processBlock(buffer, existingQuestions)) added++;
                    else skipped++; // Либо дубль, либо ошибка
                }
                buffer.clear();
            } else {
                buffer.add(line); // Сохраняем оригинальные отступы
            }
        }
        // Хвост файла
        if (!buffer.isEmpty()) {
            if (processBlock(buffer, existingQuestions)) added++;
            else skipped++;
        }

        // Очищаем файл import.txt (или оставляем ошибки, но для простоты очистим, если успех)
        fileService.ensureDirectory(Paths.get(DECKS_DIR));

        // В реальном проекте тут лучше переписать import.txt, оставив только ошибочные блоки.
        // Здесь мы просто очистим файл, считая, что пользователь поправит исходник.
        try {
            java.nio.file.Files.write(importPath, new byte[0]);
        } catch (Exception e) { return "Ошибка очистки import.txt"; }

        // Обновляем данные в приложении
        studyService.reloadSession();

        return String.format("Импорт завершен.\nДобавлено: %d\nПропущено (дубликаты): %d", added, skipped);
    }

    private boolean processBlock(List<String> buffer, Set<String> existing) {
        String cat = null, q = null, a = null;
        StringBuilder qB = new StringBuilder();
        StringBuilder aB = new StringBuilder();
        int state = 0;

        for (String line : buffer) {
            String tr = line.trim();
            if (tr.startsWith("CATEGORY:")) { cat = tr.substring(9).trim(); state = 0; }
            else if (tr.startsWith("QUESTION:")) state = 1;
            else if (tr.startsWith("ANSWER:")) state = 2;
            else {
                if (state == 1) qB.append(line).append("\n");
                if (state == 2) aB.append(line).append("\n");
            }
        }
        q = qB.toString().trim();
        a = aB.toString().trim();

        if (cat == null || q.isEmpty() || a.isEmpty()) return false;

        // Проверка на дубликат
        String id = TextUtil.normalizedForId(q);
        if (existing.contains(id)) return false;

        // Запись в файл колоды
        String safeName = cat.replaceAll("[^a-zA-Z0-9а-яА-Я ._-]", "");
        Path deckPath = Paths.get(DECKS_DIR, safeName + ".txt");

        String entry = String.format("CATEGORY: %s%nQUESTION:%n%s%nANSWER:%n%s%n===%n", cat, q, a);
        fileService.appendLine(deckPath, entry);

        existing.add(id); // Добавляем в локальный кэш, чтобы не дублировать внутри одного импорта
        return true;
    }
}