package util;

import model.Card;
import java.util.UUID;

public class CardFactory {

    /**
     * Создает совершенно новую карту (генерирует UUID, уровень 0).
     */
    public static Card createNew(String category, String question, String answer, String sourceFile) {
        return new Card(
                UUID.randomUUID().toString(), // Генерация ID вынесена сюда
                category,
                question,
                answer,
                sourceFile,
                0,      // Начальный уровень
                true    // Флаг новизны
        );
    }

    /**
     * Восстанавливает карту из файла (когда ID уже есть).
     */
    public static Card restore(String id, String category, String question, String answer, String sourceFile) {
        // Если вдруг в файле ID пустой, генерируем (защитная логика)
        String validId = (id == null || id.trim().isEmpty())
                ? UUID.randomUUID().toString()
                : id;

        return new Card(
                validId,
                category,
                question,
                answer,
                sourceFile,
                0,    // Уровень и статус будут переопределены позже из StatsRepository
                true  // По умолчанию считаем новой, пока не наложим статистику
        );
    }
}