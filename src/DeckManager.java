import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DeckManager {
    private List<Card> cards = new ArrayList<>();
    private Card lastCard = null;
    private final File saveFile = new File("anki_stats.txt");
    private final String decksDir = "decks";
    private final Random random = new Random();

    public DeckManager() {
        AppLogger.info("Инициализация DeckManager...");
        ensureDecksExist();
        loadAll();
    }

    private String normalize(String s) {
        return s.trim().toLowerCase().replaceAll("\\s+", "");
    }

    private void loadAll() {
        List<Card> rawCards = new ArrayList<>();
        File d = new File(decksDir);

        // 1. Читаем все файлы
        if (d.exists() && d.listFiles() != null) {
            for (File f : d.listFiles((dir, n) -> n.endsWith(".txt"))) {
                AppLogger.info("Чтение файла колоды: " + f.getName());
                rawCards.addAll(parseFileRobust(f));
            }
        } else {
            AppLogger.error("Папка decks пуста или не существует!");
        }

        // 2. Читаем статистику
        Map<String, Integer> statsMap = new HashMap<>();
        if (saveFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(saveFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) {
                        statsMap.put(parts[0], Integer.parseInt(parts[1]));
                    }
                }
            } catch (Exception e) {
                AppLogger.error("Ошибка чтения статистики", e);
            }
        }

        // 3. Фильтруем дубликаты
        this.cards.clear();

        // Map<НормализованныйВопрос, ИмяФайлаГдеВстретилсяВпервые>
        Map<String, String> seenOrigins = new HashMap<>();
        int duplicatesCount = 0;

        for (Card c : rawCards) {
            String normQ = normalize(c.question);

            // --- ПРОВЕРКА НА ДУБЛИКАТ ---
            if (seenOrigins.containsKey(normQ)) {
                duplicatesCount++;
                String originalFile = seenOrigins.get(normQ);

                // Делаем красивое превью вопроса для лога
                String preview = c.question.replace("\n", " ").trim();
                if (preview.length() > 30) preview = preview.substring(0, 30) + "...";

                // ВЫВОДИМ ТОЧНУЮ ИНФОРМАЦИЮ О ДУБЛИКАТЕ
                AppLogger.error(String.format(
                        "ДУБЛИКАТ: Файл '%s' содержит вопрос '%s', который уже есть в '%s'. Карточка пропущена.",
                        c.sourceFile, preview, originalFile
                ));

                continue; // Пропускаем
            }

            // Если не дубликат - запоминаем, где видели
            seenOrigins.put(normQ, c.sourceFile);
            // ----------------------------

            // Восстанавливаем уровень
            String id = String.valueOf(normQ.hashCode());
            if (statsMap.containsKey(id)) {
                c.level = Math.max(0, Math.min(10, statsMap.get(id)));
                c.isNew = false;
            } else {
                c.isNew = true;
                c.level = 0;
            }
            this.cards.add(c);
        }

        if (duplicatesCount > 0) {
            AppLogger.info(">>> Итого пропущено дубликатов: " + duplicatesCount);
        }
        AppLogger.info("Всего уникальных карт загружено: " + cards.size());
    }

    public void processAnswer(Card card, boolean isCorrect) {
        lastCard = card;
        card.isNew = false;
        if (isCorrect) {
            if (card.level < 10) card.level++;
        } else {
            if (card.level > 5) card.level = 2;
            else card.level = 0;
        }
        saveProgress();
    }

    private void saveProgress() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(saveFile), StandardCharsets.UTF_8))) {
            for (Card c : cards) {
                if (!c.isNew) {
                    String id = String.valueOf(normalize(c.question).hashCode());
                    pw.println(id + "|" + c.level);
                }
            }
        } catch (Exception e) {
            AppLogger.error("Не удалось сохранить прогресс", e);
        }
    }

    public Card getNext(boolean isShuffle) {
        if (cards.isEmpty()) return null;
        List<Card> pool = new ArrayList<>(cards);
        if (pool.size() > 1 && lastCard != null) pool.remove(lastCard);
        if (pool.isEmpty()) return null;

        if (isShuffle) return pool.get(random.nextInt(pool.size()));
        else return getWeightedRandomCard(pool);
    }

    private Card getWeightedRandomCard(List<Card> pool) {
        double totalWeight = 0.0;
        for (Card c : pool) totalWeight += c.getWeight();
        double value = random.nextDouble() * totalWeight;
        for (Card c : pool) {
            value -= c.getWeight();
            if (value <= 0) return c;
        }
        return pool.get(pool.size() - 1);
    }

    public Map<String, List<Card>> getCardsByCategory() {
        return cards.stream().collect(Collectors.groupingBy(c -> c.category));
    }

    public List<Card> getAllCards() { return cards; }

    private void ensureDecksExist() {
        File dir = new File(decksDir);
        if (!dir.exists()) {
            if (dir.mkdir()) AppLogger.info("Создана папка decks");
            else AppLogger.error("Не удалось создать папку decks");
        }
        if (dir.listFiles() == null || dir.listFiles().length == 0) createDefaultFile();
    }

    private List<Card> parseFileRobust(File f) {
        List<Card> list = new ArrayList<>();
        int cardCount = 0;
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            String currentCat = "General";
            StringBuilder qBuffer = new StringBuilder();
            StringBuilder aBuffer = new StringBuilder();
            int state = 0;
            int lineNum = 0;

            for (String rawLine : lines) {
                lineNum++;
                String line = rawLine.replace("\uFEFF", "").trim();

                if (line.equals("===")) {
                    if (qBuffer.length() > 0 && aBuffer.length() > 0) {
                        // ПЕРЕДАЕМ ИМЯ ФАЙЛА f.getName() В КОНСТРУКТОР
                        list.add(new Card(currentCat, qBuffer.toString().trim(), aBuffer.toString().trim(), f.getName()));
                        cardCount++;
                    } else {
                        AppLogger.error("Файл " + f.getName() + ": пустой вопрос/ответ около строки " + lineNum);
                    }
                    qBuffer.setLength(0); aBuffer.setLength(0); state = 0;
                    continue;
                }

                if (line.startsWith("CATEGORY:")) {
                    currentCat = line.substring("CATEGORY:".length()).trim(); state = 0;
                } else if (line.startsWith("QUESTION:")) { state = 1; }
                else if (line.startsWith("ANSWER:")) { state = 2; }
                else {
                    if (state == 1) qBuffer.append(rawLine).append("\n");
                    else if (state == 2) aBuffer.append(rawLine).append("\n");
                    else if (!line.isEmpty()) {
                        AppLogger.error("Файл " + f.getName() + " строка " + lineNum + ": Игнорируется: " + line);
                    }
                }
            }
            if (qBuffer.length() > 0 && aBuffer.length() > 0) {
                // И ЗДЕСЬ ТОЖЕ ПЕРЕДАЕМ ИМЯ ФАЙЛА
                list.add(new Card(currentCat, qBuffer.toString().trim(), aBuffer.toString().trim(), f.getName()));
                cardCount++;
            }

        } catch (IOException e) {
            AppLogger.error("Ошибка чтения файла " + f.getName(), e);
        }
        AppLogger.info("   -> Извлечено карточек: " + cardCount);
        return list;
    }

    private void createDefaultFile() {
        AppLogger.info("Создание файла java_init.txt...");
        String data = "CATEGORY: Java Core\nQUESTION:\nПринципы ООП?\nANSWER:\nИнкапсуляция, Наследование, Полиморфизм, Абстракция.\n===\n";
        try { Files.write(Paths.get(decksDir, "java_init.txt"), data.getBytes(StandardCharsets.UTF_8)); } catch (IOException e) {
            AppLogger.error("Ошибка создания init файла", e);
        }
    }
    /**
     * Возвращает набор нормализованных вопросов (хешей или строк),
     * которые уже загружены в программу.
     */
    public Set<String> getExistingNormalizedQuestions() {
        return cards.stream()
                .map(c -> normalize(c.question))
                .collect(Collectors.toSet());
    }
}