package util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Простая шина событий
 * <p>
 * Реализует паттерн Observer (Publish-Subscribe).
 * Позволяет компонентам (например, {@code StatsPanel}) узнавать об изменениях в системе
 * (например, завершение импорта) без жесткой связности с источником изменений
 * </p>
 */
public class EventBus {
    private static final Logger LOGGER = Logger.getLogger(EventBus.class.getName());

    /** Темы событий */
    public enum Topic {
        /** Данные (карты, статистика) обновились. Нужно перерисовать UI */
        DATA_UPDATED
    }

    private static final List<Subscriber> subscribers = new ArrayList<>();

    // структура для хранения подписки
    private record Subscriber(Topic topic, Consumer<Object> action) {}

    /**
     * Подписаться на событие определенного типа
     *
     * @param topic тип события
     * @param action действие, которое нужно выполнить (обычно обновление UI)
     */
    public static void subscribe(Topic topic, Runnable action) {
        // Runnable в Consumer, так как пока не передаем данные (payload)
        subscribers.add(new Subscriber(topic, (_) -> action.run()));
    }

    /**
     * Опубликовать событие.
     * Все подписчики этой темы получат уведомление синхронно
     *
     * @param topic тип события
     */
    public static void publish(Topic topic) {
        for (Subscriber s : subscribers) {
            if (s.topic == topic) {
                try {
                    s.action.accept(null);
                } catch (Exception e) {
                    // ловим исключения подписчиков, чтобы не поломать основной поток
                    LOGGER.log(Level.SEVERE, "Ошибка при обработке события [" + topic + "]", e);
                }
            }
        }
    }

    private EventBus() {}
}