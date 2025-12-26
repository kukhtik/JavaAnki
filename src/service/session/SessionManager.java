package service.session;

import data.repository.*;
import lombok.Getter;
import model.Card;
import util.EventBus;
import util.TextUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private final CardRepository cardRepo;
    private final StatsRepository statsRepo;
    private final HistoryRepository historyRepo;
    private final GroupRepository groupRepo;

    @Getter
    private List<Card> allCards = new ArrayList<>();

    public SessionManager(CardRepository cardRepo, StatsRepository statsRepo,
                          HistoryRepository historyRepo, GroupRepository groupRepo) {
        this.cardRepo = cardRepo;
        this.statsRepo = statsRepo;
        this.historyRepo = historyRepo;
        this.groupRepo = groupRepo;
    }

    public void reload() {
        LOGGER.info(">>> SESSION MANAGER: RELOAD...");
        List<Card> rawCards = cardRepo.loadAllCards();
        Map<String, Integer> stats = statsRepo.loadStats();
        groupRepo.loadStructure();

        this.allCards = processCards(rawCards, stats);

        // Перезапись файлов для закрепления UUID (Миграция)
        Map<String, List<Card>> byFile = allCards.stream()
                .collect(Collectors.groupingBy(Card::getSourceFile));
        for (var entry : byFile.entrySet()) {
            cardRepo.saveDeck(entry.getKey(), entry.getValue());
        }

        statsRepo.saveStats(allCards);

        EventBus.publish(EventBus.Topic.DATA_UPDATED);
    }

    public void saveProgress(Card card, String answer, boolean correct) {
        historyRepo.saveEntry(card.getQuestion(), answer, correct);
        statsRepo.saveStats(allCards);
    }

    public List<String> getAllCategories() {
        List<String> options = new ArrayList<>(groupRepo.getGroupNames());
        Collections.sort(options);
        List<String> cats = allCards.stream().map(Card::getCategory).distinct().sorted().toList();
        options.addAll(cats);
        return options;
    }

    public boolean isCardInGroup(Card c, String groupOrCategory) {
        if (groupRepo.getGroupNames().contains(groupOrCategory)) {
            return groupRepo.isCardInGroup(c.getSourceFile(), groupOrCategory);
        }
        return c.getCategory().equals(groupOrCategory);
    }

    // Логика миграции и дедупликации (вынесена в приватный метод)
    private List<Card> processCards(List<Card> raw, Map<String, Integer> stats) {
        Set<String> seenHashes = new HashSet<>();
        List<Card> result = new ArrayList<>();

        for (Card c : raw) {
            String h = TextUtil.normalizeForId(c.getQuestion());
            if (seenHashes.contains(h)) continue;
            seenHashes.add(h);

            boolean found = false;
            // 1. По UUID
            if (c.getId() != null && stats.containsKey(c.getId())) {
                c.setLevel(stats.get(c.getId()));
                c.setNew(false);
                found = true;
            }
            // 2. По тексту (Legacy)
            else {
                String legacyId = String.valueOf(h.hashCode());
                if (stats.containsKey(legacyId)) {
                    c.setLevel(stats.get(legacyId));
                    c.setNew(false);
                    found = true;
                }
            }

            if (!found) { c.setLevel(0); c.setNew(true); }
            result.add(c);
        }
        return result;
    }
}