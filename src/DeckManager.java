import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class DeckManager {
    private List<Card> allCards = new ArrayList<>();
    private List<Card> activeDeck = new ArrayList<>();
    private GroupManager groupManager; // Подключили менеджер групп

    private Card lastCard = null;
    private final File saveFile = new File("anki_stats.txt");
    private final String decksDir = "decks";
    private final Random random = new Random();

    public DeckManager() {
        AppLogger.info("Инициализация DeckManager...");
        groupManager = new GroupManager(); // Грузим структуру

        ensureDecksExist();
        loadAll();
        resetFilter();
    }

    /**
     * Умная фильтрация:
     * 1. Если "ВСЕ ТЕМЫ" -> сброс.
     * 2. Если это ГРУППА (из structure.txt) -> фильтруем по именам файлов.
     * 3. Если это просто КАТЕГОРИЯ -> фильтруем по полю category карты.
     */
    public void setCategoryFilter(String selectedItem) {
        if (selectedItem == null || selectedItem.equals("ВСЕ ТЕМЫ")) {
            resetFilter();
            return;
        }

        // Проверяем, является ли выбор ГРУППОЙ файлов
        if (groupManager.getGroupNames().contains(selectedItem)) {
            activeDeck = allCards.stream()
                    .filter(c -> groupManager.isCardInGroup(c, selectedItem))
                    .collect(Collectors.toList());
            AppLogger.info("Выбрана группа файлов: " + selectedItem + " (" + activeDeck.size() + " карт)");
        }
        // Иначе фильтруем по старинке (по названию категории внутри файла)
        else {
            activeDeck = allCards.stream()
                    .filter(c -> c.category.equals(selectedItem))
                    .collect(Collectors.toList());
            AppLogger.info("Выбрана категория: " + selectedItem + " (" + activeDeck.size() + " карт)");
        }

        lastCard = null;
    }

    public void resetFilter() {
        activeDeck = new ArrayList<>(allCards);
        AppLogger.info("Фильтр сброшен. Учим все карты (" + activeDeck.size() + ").");
    }

    /**
     * Возвращает список для выпадающего меню:
     * 1. Группы (из structure.txt)
     * 2. Отдельные категории (из файлов)
     */
    public Vector<String> getFilterOptions() {
        Vector<String> options = new Vector<>();
        options.add("ВСЕ ТЕМЫ");

        // Сначала добавляем группы
        List<String> groups = new ArrayList<>(groupManager.getGroupNames());
        Collections.sort(groups);
        options.addAll(groups);

        // Потом добавляем одиночные категории
        List<String> cats = allCards.stream()
                .map(c -> c.category)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // (Опционально) Можно добавить разделитель, но JComboBox их плохо переваривает, просто добавим
        options.addAll(cats);

        return options;
    }

    // --- ДАЛЕЕ СТАНДАРТНЫЙ КОД (getNext, loadAll и т.д.) ---

    public Card getNext(boolean isShuffle) {
        if (activeDeck.isEmpty()) return null;
        List<Card> pool = new ArrayList<>(activeDeck);
        if (pool.size() > 1 && lastCard != null) pool.remove(lastCard);
        if (pool.isEmpty()) return null;
        if (isShuffle) return pool.get(random.nextInt(pool.size()));
        else return getWeightedRandomCard(pool);
    }

    public void processAnswer(Card card, boolean isCorrect) {
        lastCard = card;
        card.isNew = false;
        if (isCorrect) { if (card.level < 10) card.level++; }
        else { if (card.level > 5) card.level = 2; else card.level = 0; }
        saveProgress();
    }

    private void loadAll() {
        List<Card> rawCards = new ArrayList<>();
        File d = new File(decksDir);
        if (d.exists() && d.listFiles() != null) {
            for (File f : d.listFiles((dir, n) -> n.endsWith(".txt"))) {
                rawCards.addAll(parseFileRobust(f));
            }
        }

        Map<String, Integer> statsMap = new HashMap<>();
        if (saveFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(saveFile), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 2) statsMap.put(parts[0], Integer.parseInt(parts[1]));
                }
            } catch (Exception e) {}
        }

        this.allCards.clear();
        Map<String, String> seenOrigins = new HashMap<>();
        for (Card c : rawCards) {
            String normQ = normalize(c.question);
            if (seenOrigins.containsKey(normQ)) continue;
            seenOrigins.put(normQ, c.sourceFile);

            String id = String.valueOf(normQ.hashCode());
            if (statsMap.containsKey(id)) {
                c.level = Math.max(0, Math.min(10, statsMap.get(id)));
                c.isNew = false;
            } else {
                c.isNew = true;
                c.level = 0;
            }
            this.allCards.add(c);
        }
    }

    private void saveProgress() {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(saveFile), StandardCharsets.UTF_8))) {
            for (Card c : allCards) {
                if (!c.isNew) {
                    String id = String.valueOf(normalize(c.question).hashCode());
                    pw.println(id + "|" + c.level);
                }
            }
        } catch (Exception e) { AppLogger.error("Ошибка сохранения", e); }
    }

    public List<Card> getAllCards() { return allCards; }
    public Map<String, List<Card>> getCardsByCategory() { return allCards.stream().collect(Collectors.groupingBy(c -> c.category)); }
    public Set<String> getExistingNormalizedQuestions() { return allCards.stream().map(c -> normalize(c.question)).collect(Collectors.toSet()); }

    private Card getWeightedRandomCard(List<Card> pool) {
        double totalWeight = 0.0;
        for (Card c : pool) totalWeight += c.getWeight();
        double value = random.nextDouble() * totalWeight;
        for (Card c : pool) { value -= c.getWeight(); if (value <= 0) return c; }
        return pool.get(pool.size() - 1);
    }
    private String normalize(String s) { return s.trim().toLowerCase().replaceAll("\\s+", ""); }
    private void ensureDecksExist() { new File(decksDir).mkdir(); }

    private List<Card> parseFileRobust(File f) {
        List<Card> list = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(f.toPath(), StandardCharsets.UTF_8);
            String currentCat = "General";
            StringBuilder qBuffer = new StringBuilder();
            StringBuilder aBuffer = new StringBuilder();
            int state = 0;
            for (String rawLine : lines) {
                String line = rawLine.replace("\uFEFF", "").trim();
                if (line.equals("===")) {
                    if (qBuffer.length() > 0 && aBuffer.length() > 0) list.add(new Card(currentCat, qBuffer.toString().trim(), aBuffer.toString().trim(), f.getName()));
                    qBuffer.setLength(0); aBuffer.setLength(0); state = 0; continue;
                }
                if (line.startsWith("CATEGORY:")) { currentCat = line.substring("CATEGORY:".length()).trim(); state = 0; }
                else if (line.startsWith("QUESTION:")) state = 1; else if (line.startsWith("ANSWER:")) state = 2;
                else { if (state == 1) qBuffer.append(rawLine).append("\n"); else if (state == 2) aBuffer.append(rawLine).append("\n"); }
            }
            if (qBuffer.length() > 0 && aBuffer.length() > 0) list.add(new Card(currentCat, qBuffer.toString().trim(), aBuffer.toString().trim(), f.getName()));
        } catch (IOException e) { AppLogger.error("Ошибка чтения " + f.getName(), e); }
        return list;
    }
}