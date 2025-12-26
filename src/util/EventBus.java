package util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EventBus {

    // Типы событий
    public enum Topic {
        DATA_UPDATED,  // Карты обновились (импорт, перезагрузка)
        THEME_CHANGED  // Сменилась тема (можно использовать для перерисовки)
    }

    private static final List<Subscriber> subscribers = new ArrayList<>();

    // Интерфейс подписчика (просто обертка над Consumer)
    private record Subscriber(Topic topic, Consumer<Object> action) {}

    /**
     * Подписаться на событие.
     * @param topic Тема события
     * @param action Лямбда, которая выполнится при событии
     */
    public static void subscribe(Topic topic, Runnable action) {
        subscribers.add(new Subscriber(topic, (o) -> action.run()));
    }

    /**
     * Опубликовать событие. Все подписчики получат уведомление.
     */
    public static void publish(Topic topic) {
        for (Subscriber s : subscribers) {
            if (s.topic == topic) {
                try {
                    s.action.accept(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}