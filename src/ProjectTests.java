// Цвета для консоли
public static final String ANSI_RESET = "\u001B[0m";
public static final String ANSI_GREEN = "\u001B[32m";
public static final String ANSI_RED = "\u001B[31m";

void main() {
    IO.println("=== ЗАПУСК ТЕСТОВ PROJЕCT ===\n");

    int passed = 0;
    int failed = 0;

    // 1. Тесты SmartChecker
    if (testSmartCheckerExactMatch()) passed++;
    else failed++;
    if (testSmartCheckerTypo()) passed++;
    else failed++;
    if (testSmartCheckerCompleteMismatch()) passed++;
    else failed++;

    // 2. Тесты Card
    if (testCardWeights()) passed++;
    else failed++;

    // 3. Тесты Импорта (самые сложные, с файлами)
    if (testImportProcess()) passed++;
    else failed++;
    if (testImportDuplicateLogic()) passed++;
    else failed++;

    IO.println("\n=============================");
    IO.println("ИТОГ: " + (failed == 0 ? ANSI_GREEN + "ВСЕ ТЕСТЫ ПРОЙДЕНЫ" : ANSI_RED + "ЕСТЬ ОШИБКИ") + ANSI_RESET);
    IO.println("Пройдено: " + passed);
    IO.println("Провалено: " + failed);
    IO.println("=============================");
}

// --- Утилиты для тестов ---
private static void logRes(String name, boolean result) {
    IO.print(String.format("%-40s", name));
    if (result) IO.println("[" + ANSI_GREEN + "OK" + ANSI_RESET + "]");
    else IO.println("[" + ANSI_RED + "FAIL" + ANSI_RESET + "]");
}

// --- ТЕСТЫ SMART CHECKER ---
private static boolean testSmartCheckerExactMatch() {
    double score = SmartChecker.check("Hello World", "Hello World");
    boolean res = score > 99.0;
    logRes("SmartChecker: Точное совпадение", res);
    return res;
}

private static boolean testSmartCheckerTypo() {
    // "Jvava" vs "Java" - должно быть высокое сходство
    double score = SmartChecker.check("Jvava", "Java");
    boolean res = score > 60.0;
    logRes("SmartChecker: Опечатки", res);
    return res;
}

private static boolean testSmartCheckerCompleteMismatch() {
    double score = SmartChecker.check("Apple", "Banana");
    boolean res = score < 20.0;
    logRes("SmartChecker: Несовпадение", res);
    return res;
}

// --- ТЕСТЫ CARD ---
private static boolean testCardWeights() {
    Card newCard = new Card("C", "Q", "A", "f");
    Card errorCard = new Card("C", "Q", "A", "f");
    errorCard.isNew = false;
    errorCard.level = 0;
    Card masterCard = new Card("C", "Q", "A", "f");
    masterCard.isNew = false;
    masterCard.level = 10;

    boolean res = newCard.getWeight() == 100.0;
    if (errorCard.getWeight() != 150.0) res = false; // Ошибки важнее новых
    if (masterCard.getWeight() > 10.0) res = false;  // Выученные должны иметь малый вес

    logRes("Card: Расчет весов (приоритетов)", res);
    return res;
}

// --- ТЕСТЫ ИМПОРТА (DeckDistributor) ---
private static boolean testImportProcess() {
    String testFileName = "test_import_temp.txt";
    String testCategory = "TestCat_Gen_123"; // Уникальное имя
    File importFile = new File(testFileName);

    // Создаем тестовый файл импорта
    String content = "===\nCATEGORY: " + testCategory + "\nQUESTION:\nQ1\nANSWER:\nA1\n===\n";
    try {
        Files.write(Paths.get(testFileName), content.getBytes());
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }

    // Запускаем дистрибьютор
    return isRes(importFile, testCategory);
}

private static boolean isRes(File importFile, String testCategory) {
    DeckDistributor distributor = new DeckDistributor();
    Set<String> emptySet = new HashSet<>();
    distributor.distribute(importFile, emptySet);

    // Проверяем:
    // 1. Файл колоды создался
    File deckFile = new File("decks/" + testCategory + ".txt");
    boolean deckCreated = deckFile.exists();

    // 2. Файл импорта очистился
    boolean importCleared = importFile.length() == 0;

    // Очистка мусора
    if (deckFile.exists()) deckFile.delete();
    if (importFile.exists()) importFile.delete();

    boolean res = deckCreated && importCleared;
    logRes("Import: Создание файла и очистка", res);
    return res;
}

private static boolean testImportDuplicateLogic() {
    String testFileName = "test_import_dup.txt";
    File importFile = new File(testFileName);

    // Вопрос "Q_DUP" уже якобы существует в базе
    String questionText = "Q_DUP";
    String content = "===\nCATEGORY: TestCat_Dup\nQUESTION:\n" + questionText + "\nANSWER:\nA\n===\n";

    try {
        Files.write(Paths.get(testFileName), content.getBytes());
    } catch (IOException e) {
        e.printStackTrace();
        return false;
    }

    // Симулируем, что вопрос уже есть
    return isRes(questionText, importFile);
}

private static boolean isRes(String questionText, File importFile) {
    Set<String> existing = new HashSet<>();
    existing.add(questionText.toLowerCase().trim());

    DeckDistributor distributor = new DeckDistributor();
    distributor.distribute(importFile, existing);

    // Проверяем:
    // 1. Файл колоды НЕ должен создаться (так как дубликат)
    File deckFile = new File("decks/TestCat_Dup.txt");
    boolean deckNotCreated = !deckFile.exists();

    // 2. Файл импорта ДОЛЖЕН очиститься (так как дубликат удаляется)
    boolean importCleared = importFile.length() == 0;

    // Очистка
    if (deckFile.exists()) deckFile.delete();
    if (importFile.exists()) importFile.delete();

    boolean res = deckNotCreated && importCleared;
    logRes("Import: Фильтрация дубликатов", res);
    return res;
}