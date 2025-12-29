package service.session;

import data.repository.*;
import model.Card;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.TextUtil;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тестирование ядра бизнес-логики (SessionManager)
 * <p>
 * Проверяет сложные сценарии загрузки данных:
 * <ul>
 *     <li><b>Дедупликация:</b> Исключение одинаковых вопросов</li>
 *     <li><b>Миграция:</b> Восстановление прогресса по старому алгоритму хеширования (для обратной совместимости)</li>
 *     <li><b>Связывание:</b> Корректное восстановление прогресса по UUID</li>
 * </ul>
 * </p>
 */
@DisplayName("Тестирование SessionManager")
class SessionManagerTest {

    private StubCardRepo cardRepo;
    private StubStatsRepo statsRepo;
    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        cardRepo = new StubCardRepo();
        statsRepo = new StubStatsRepo();
        DummyHistoryRepo historyRepo = new DummyHistoryRepo();
        DummyGroupRepo groupRepo = new DummyGroupRepo();

        sessionManager = new SessionManager(cardRepo, statsRepo, historyRepo, groupRepo);
    }

    @Test
    @DisplayName("Дедупликация: Одинаковые вопросы должны объединяться в одну карту")
    void testDeduplication() {
        // две карты с одинаковым текстом вопроса, но разными ID и файлами
        Card c1 = new Card("id-1", "Cat", "Duplicate Question", "A", "file1.txt", 0, true);
        Card c2 = new Card("id-2", "Cat", "Duplicate Question", "A", "file2.txt", 0, true);

        cardRepo.cardsToReturn.add(c1);
        cardRepo.cardsToReturn.add(c2);

        // загружаем сессию
        sessionManager.reload();
        List<Card> result = sessionManager.getAllCards();

        // в памяти должна остаться только одна
        assertEquals(1, result.size(), "Дубликаты должны быть удалены, ожидался размер списка 1");
        assertEquals("Duplicate Question", result.getFirst().getQuestion());
    }

    @Test
    @DisplayName("Миграция: Прогресс должен восстанавливаться по тексту вопроса (Legacy Hash)")
    void testMigrationByLegacyHash() {
        String questionText = "Legacy Question";
        // карта новая, у нее есть UUID, но в статистике его еще нет
        Card c = new Card("new-random-uuid", "Cat", questionText, "A", "file.txt", 0, true);
        cardRepo.cardsToReturn.add(c);

        // симулируем старую статистику: там лежит запись не по UUID, а по хешу текста
        String legacyHash = String.valueOf(TextUtil.normalizeForId(questionText).hashCode());
        statsRepo.statsToReturn.put(legacyHash, 5); // Уровень 5

        sessionManager.reload();
        Card loadedCard = sessionManager.getAllCards().getFirst();

        assertEquals(5, loadedCard.getLevel(), "Уровень должен восстановиться по Legacy Hash");
        assertFalse(loadedCard.isNew(), "Карта должна перестать быть 'новой'");
    }

    @Test
    @DisplayName("Нормальная работа: Прогресс восстанавливается по UUID")
    void testLoadingByUUID() {
        String fixedUUID = "fixed-uuid-123";
        Card c = new Card(fixedUUID, "Cat", "Normal Q", "A", "file.txt", 0, true);
        cardRepo.cardsToReturn.add(c);

        // в статистике есть запись именно с этим UUID
        statsRepo.statsToReturn.put(fixedUUID, 8); // Уровень 8 (Master)

        sessionManager.reload();
        Card loadedCard = sessionManager.getAllCards().getFirst();

        assertEquals(8, loadedCard.getLevel(), "Уровень должен восстановиться по UUID");
    }

    static class StubCardRepo implements CardRepository {
        public List<Card> cardsToReturn = new ArrayList<>();

        @Override public List<Card> loadAllCards() { return cardsToReturn; }
        @Override public void saveDeck(String fileName, List<Card> cards) { /* Имитация сохранения */ }
    }

    static class StubStatsRepo implements StatsRepository {
        public Map<String, Integer> statsToReturn = new HashMap<>();

        @Override public Map<String, Integer> loadStats() { return statsToReturn; }
        @Override public void saveStats(List<Card> cards) { /* Имитация сохранения */ }
    }

    // просто заглушки, чтобы код компилировался
    static class DummyHistoryRepo extends HistoryRepository {
        public DummyHistoryRepo() { super(); }
        @Override public void saveEntry(String q, String a, boolean c) {}
    }

    static class DummyGroupRepo extends GroupRepository {
        public DummyGroupRepo() { super(); }
        @Override public void loadStructure() {}
        @Override public Set<String> getGroupNames() { return new HashSet<>(); }
        @Override public boolean isCardInGroup(String f, String g) { return false; }
    }
}