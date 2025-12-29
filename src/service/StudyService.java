package service;

import data.repository.HistoryRepository;
import lombok.Getter;
import model.Card;
import model.HistoryRecord;
import model.dto.StatsRow;
import service.algorithm.SpacedRepetitionAlgorithm;
import service.session.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Основной сервис приложения (Business Logic Layer)
 * <p>
 * Предоставляет интерфейсу упрощенный API для управления обучением.
 * Оркестрирует работу трех компонентов:
 * <ol>
 *     <li>{@link SessionManager} - управление данными (CRUD, сохранение)</li>
 *     <li>{@link SpacedRepetitionAlgorithm} - математика выбора карт (SRS)</li>
 *     <li>{@link GradingService} - проверка ответов (Fuzzy Matching)</li>
 * </ol>
 * Класс хранит состояние текущей сессии (активная колода, текущая карта)
 * </p>
 */
public class StudyService {
    private static final Logger LOGGER = Logger.getLogger(StudyService.class.getName());

    private final SessionManager sessionManager;
    private final SpacedRepetitionAlgorithm algorithm;
    private final GradingService gradingService;

    /**
     * Текущая активная колода.
     * Может содержать все карты или только отфильтрованные по теме
     */
    private List<Card> activeDeck = new ArrayList<>();

    /**
     * Карточка, которая сейчас отображается на экране
     */
    @Getter
    private Card currentCard;

    /**
     * Конструктор сервиса
     *
     * @param sessionManager менеджер сессии
     */
    public StudyService(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.algorithm = new SpacedRepetitionAlgorithm();
        this.gradingService = new GradingService();

        reloadSession();
    }

    /**
     * Перезагружает данные из файлов и сбрасывает состояние обучения.
     * Вызывается при старте или после импорта новых карт
     */
    public void reloadSession() {
        sessionManager.reload();
        resetFilter();
    }

    /**
     * Получить полный список всех карточек (без фильтров)
     */
    public List<Card> getAllCards() {
        return sessionManager.getAllCards();
    }
    /**
     * Загружает журнал истории ответов.
     * Создает экземпляр репозитория "на лету", так как это редкая операция
     */
    public List<HistoryRecord> getHistory() {
        return new HistoryRepository().loadHistory();
    }

    /**
     * Вычисляет сводную статистику по категориям.
     * <p>
     * Группирует карты по категориям и считает для каждой:
     * Всего / Новые / В процессе / Выученные.
     * </p>
     *
     * @return DTO {@link StatsRow} для отображения в таблице
     */
    public List<StatsRow> getStatistics() {
        List<Card> all = sessionManager.getAllCards();
        List<StatsRow> rows = new ArrayList<>();

        // группировка по полю Category
        Map<String, List<Card>> grouped = all.stream()
                .collect(Collectors.groupingBy(Card::getCategory));

        // расчет статистики для каждой группы
        for (var entry : grouped.entrySet()) {
            rows.add(calculateRow(entry.getKey(), entry.getValue()));
        }

        // расчет итоговой строки
        rows.add(calculateRow("<<<<<< ИТОГО >>>>>>", all));
        return rows;
    }

    /**
     * Вспомогательный метод подсчета статистики для списка карт
     */
    private StatsRow calculateRow(String name, List<Card> list) {
        long total = list.size();
        long cntNew = list.stream().filter(Card::isNew).count();
        // карта "Выученная" (Master), если ее уровень >= 8 (максимум 10)
        long cntMaster = list.stream().filter(c -> !c.isNew() && c.getLevel() >= 8).count();
        // остальные - в процессе (Learning)
        long cntLearn = total - cntNew - cntMaster;

        return new StatsRow(name, total, cntNew, cntLearn, cntMaster);
    }
    /**
     * Устанавливает активную колоду (Active Deck) на основе выбора пользователя
     *
     * @param filter название категории или группы тем. Если {@code null} или "ВСЕ ТЕМЫ", фильтр сбрасывается
     */
    public void setFilter(String filter) {
        if (filter == null || filter.equals("ВСЕ ТЕМЫ")) {
            resetFilter();
        } else {
            // проверка вхождения
            activeDeck = sessionManager.getAllCards().stream()
                    .filter(c -> sessionManager.isCardInGroup(c, filter))
                    .collect(Collectors.toList());
            LOGGER.info("Фильтр установлен [" + filter + "]. Карт отобрано: " + activeDeck.size());
        }
    }

    /**
     * Сбрасывает фильтр, делая доступными для изучения абсолютно все карты
     */
    public void resetFilter() {
        this.activeDeck = new ArrayList<>(sessionManager.getAllCards());
    }

    /**
     * Получает список доступных фильтров (Группы + Категории) для комбобокса
     */
    public List<String> getAvailableCategories() {
        return sessionManager.getAllCategories();
    }

    /**
     * Запрашивает у алгоритма следующую карточку
     *
     * @param isShuffle выбора: {@code true} - случайный, {@code false} - умный (SRS)
     * @return объект Card или null, если колода пуста
     */
    public Card nextCard(boolean isShuffle) {
        currentCard = algorithm.selectNextCard(activeDeck, currentCard, isShuffle);
        return currentCard;
    }

    /**
     * Проверяет ответ пользователя БЕЗ сохранения результата.
     * Используется для предпросмотра оценки (показать пользователю процент совпадения)
     *
     * @param userAnswer текст, введенный пользователем.
     * @return результат проверки (score + passed/failed).
     */
    public GradingResult checkAnswer(String userAnswer) {
        if (currentCard == null) return new GradingResult(0, false);

        // процент сходства (0..100)
        double score = gradingService.calculateSimilarity(userAnswer, currentCard.getAnswer());

        // порог прохождения: 65% сходства
        return new GradingResult(score, score > 65.0);
    }

    /**
     * Фиксирует результат ответа.
     * <ol>
     *     <li>Обновляет математическую модель (уровень карты)</li>
     *     <li>Сохраняет данные на диск (история + статистика)</li>
     * </ol>
     *
     * @param userAnswer ответ пользователя (для лога)
     * @param correct вердикт (прошел/не прошел), принятый пользователем или системой
     */
    public void submitResult(String userAnswer, boolean correct) {
        if (currentCard == null) return;

        int oldLevel = currentCard.getLevel(); // старый уровень
        algorithm.updateCardProgress(currentCard, correct);
        int newLevel = currentCard.getLevel(); // новый уровень

        algorithm.updateCardProgress(currentCard, correct);
        LOGGER.info(String.format(
                "SRS ОБНОВЛЕН | idКАРТЫ: %s | ВЕРДИКТ: %s | УРОВЕНЬ: %d -> %d | КАТЕГОРИЯ: %s",
                currentCard.getId(),
                correct,
                oldLevel,
                newLevel,
                currentCard.getCategory()
        ));

        sessionManager.saveProgress(currentCard, userAnswer, correct);
    }

    /**
     * Простой Value Object для возврата результата проверки
     *
     * @param score процент совпадения (0.0 - 100.0)
     * @param passed флаг зачета (сдал / не сдал)
     */
        public record GradingResult(double score, boolean passed) {
    }
}