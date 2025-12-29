package service.session;

import data.repository.*;
import lombok.Getter;
import model.Card;
import util.EventBus;
import util.TextUtil;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Менеджер сессии и жизненного цикла данных
 * <p>
 * Выступает "Единым источником истины" (Single Source of Truth) для приложения.
 * Он загружает данные из всех репозиториев, объединяет их, кеширует в памяти
 * и управляет сохранением изменений
 * </p>
 * <p>
 * <b>Ключевые обязанности:</b>
 * <ul>
 *     <li>Загрузка и слияние данных (карточки + статистика)</li>
 *     <li>Миграция данных (присвоение UUID старым карточкам)</li>
 *     <li>Фильтрация (определение, какие карты учить)</li>
 *     <li>Персистентность (сохранение прогресса на диск)</li>
 * </ul>
 * </p>
 */
public class SessionManager {
    private static final Logger LOGGER = Logger.getLogger(SessionManager.class.getName());

    private final CardRepository cardRepo;
    private final StatsRepository statsRepo;
    private final HistoryRepository historyRepo;
    private final GroupRepository groupRepo;

    /**
     * Кеш всех загруженных карточек в памяти, с этим списком работает приложение
     */
    @Getter
    private List<Card> allCards = new ArrayList<>();

    /**
     * Конструктор с зависимостями
     */
    public SessionManager(CardRepository cardRepo, StatsRepository statsRepo,
                          HistoryRepository historyRepo, GroupRepository groupRepo) {
        this.cardRepo = cardRepo;
        this.statsRepo = statsRepo;
        this.historyRepo = historyRepo;
        this.groupRepo = groupRepo;
    }

    /**
     * Полная перезагрузка состояния приложения.
     * <p>
     * Выполняет последовательность действий:
     * <ol>
     *     <li>Загружает карточки из файлов</li>
     *     <li>Загружает статистику (ID - > Level)</li>
     *     <li>Объединяет их в методе {@link #processCards}, исключая дубликаты</li>
     *     <li>Перезаписывает файлы карточек. Необходимо для того,
     *     чтобы если у карточки появился новый UUID (сгенерированный в памяти),
     *     он сохранился в файл и зафиксировался</li>
     *     <li>Оповещает UI через EventBus, что данные готовы</li>
     * </ol>
     * </p>
     */
    public void reload() {
        LOGGER.info(">>>>>> SESSION MANAGER: RELOAD (Загрузка данных)...");

        // загрузка из источников
        List<Card> rawCards = cardRepo.loadAllCards();
        Map<String, Integer> stats = statsRepo.loadStats();
        groupRepo.loadStructure();

        // слияние и дедупликация
        this.allCards = processCards(rawCards, stats);

        // МИГРАЦИЯ / ФИКСАЦИЯ UUID
        // группируем карты обратно по файлам-источникам и перезаписываем их
        // UUID, присвоенные в processCards, останутся навсегда.
        Map<String, List<Card>> byFile = allCards.stream()
                .collect(Collectors.groupingBy(Card::getSourceFile));

        for (var entry : byFile.entrySet()) {
            cardRepo.saveDeck(entry.getKey(), entry.getValue());
        }

        // сохранение актуальной статистики (на случай удаления карт)
        statsRepo.saveStats(allCards);

        long total = allCards.size();
        long newCards = allCards.stream().filter(Card::isNew).count();
        long learned = allCards.stream().filter(c -> c.getLevel() >= 1).count();

        LOGGER.info(String.format(
                "Данные обновлены. Всего карт: %d (Новых: %d, В изучении: %d). Группировка по файлам выполнена",
                total, newCards, learned
        ));
        // уведомление интерфейса
        EventBus.publish(EventBus.Topic.DATA_UPDATED);
    }

    /**
     * Сохраняет результат взаимодействия с карточкой.
     * <p>
     * Метод вызывается после того, как алгоритм SRS уже обновил поле {@code level} у объекта карточки.
     * Здесь происходит только запись факта в историю и сброс состояния статистики на диск
     * </p>
     *
     * @param card карточка (уже с обновленным уровнем)
     * @param answer ответ пользователя
     * @param correct был ли ответ верным
     */
    public void saveProgress(Card card, String answer, boolean correct) {
        historyRepo.saveEntry(card.getQuestion(), answer, correct);
        statsRepo.saveStats(allCards);
    }

    /**
     * Возвращает полный список доступных категорий для выбора в UI.
     * <p>
     * Список гибридный:
     * <ul>
     *     <li>Сначала идут группы (из structure.txt)</li>
     *     <li>Затем категории</li>
     * </ul>
     * </p>
     */
    public List<String> getAllCategories() {
        // группы
        List<String> options = new ArrayList<>(groupRepo.getGroupNames());
        Collections.sort(options); // Сортировка групп

        // категории из карточек (distinct - уникальные)
        List<String> cats = allCards.stream()
                .map(Card::getCategory)
                .distinct()
                .sorted()
                .toList();

        options.addAll(cats);
        return options;
    }

    /**
     * Подходит ли карточка под выбранный фильтр?
     *
     * @param c карточка для проверки
     * @param groupOrCategory строка фильтра (название группы ИЛИ название категории)
     * @return true, если карточку нужно включить в урок
     */
    public boolean isCardInGroup(Card c, String groupOrCategory) {
        // фильтр названиее группы (папки файлов)?
        if (groupRepo.getGroupNames().contains(groupOrCategory)) {
            return groupRepo.isCardInGroup(c.getSourceFile(), groupOrCategory);
        }
        // нет, простая категория (тег внутри файла)
        return c.getCategory().equals(groupOrCategory);
    }

    /**
     * Основная логика слияния загруженных карт с сохраненной статистикой.
     * Также дедупликация и восстановление ID
     */
    private List<Card> processCards(List<Card> raw, Map<String, Integer> stats) {
        Set<String> seenHashes = new HashSet<>();
        List<Card> result = new ArrayList<>();

        for (Card c : raw) {
            // ДЕДУПЛИКАЦИЯ
            // удаляем пробелы, лишние символы
            String h = TextUtil.normalizeForId(c.getQuestion());

            // вопрос уже был? пропускаем (защита от копипасты в файлах)
            if (seenHashes.contains(h)) continue;
            seenHashes.add(h);

            boolean found = false;

            // ВОССТАНОВЛЕНИЕ ПРОГРЕССА (MAPPING)

            // у карточки уже есть UUID (из файла) и он есть в статистике
            if (c.getId() != null && stats.containsKey(c.getId())) {
                c.setLevel(stats.get(c.getId()));
                c.setNew(false);
                found = true;
            }
            // Legacy Support (обратная совместимость)
            // UUID нет? ищем по старому методу (хеш-код текста)
            else {
                String legacyId = String.valueOf(h.hashCode());
                if (stats.containsKey(legacyId)) {
                    // нашли по хешу, то восстанавливаем прогресс
                    c.setLevel(stats.get(legacyId));
                    c.setNew(false);
                    found = true;
                    // при следующем сохранении (saveDeck) карточке присвоится новый UUID,
                    // прогресс сохранится.
                }
            }

            // статистики нет? новая карта
            if (!found) {
                c.setLevel(0);
                c.setNew(true);
            }

            result.add(c);
        }
        return result;
    }
}