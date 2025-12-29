package util;

import model.Card;
import java.util.UUID;

/**
 * Фабрика для создания объектов карточек
 * <p>
 * Изолирует логику генерации идентификаторов и начальных состояний
 * </p>
 */
public class CardFactory {

    /**
     * Создает абсолютно новую карточку
     * <p>
     * Генерирует новый UUID, устанавливает начальный уровень 0 и флаг {@code isNew = true}
     * </p>
     *
     * @param category категория (тема)
     * @param question текст вопроса
     * @param answer текст ответа
     * @param sourceFile имя файла, откуда пришла карта (для группировки)
     * @return готовый объект {@link Card}
     */
    public static Card createNew(String category, String question, String answer, String sourceFile) {
        return new Card(
                UUID.randomUUID().toString(), // уникальный ID
                category,
                question,
                answer,
                sourceFile,
                0,      // начальный уровень (Box 0)
                true    // флаг новизны
        );
    }

    /**
     * Восстанавливает карточку из хранилища (ID уже известен)
     * <p>
     * Если переданный ID пуст (поврежденный файл), генерирует новый, чтобы не потерять данные
     * </p>
     *
     * @param id сохраненный UUID
     * @param category категория
     * @param question вопрос
     * @param answer ответ
     * @param sourceFile имя файла
     * @return восстановленный объект {@link Card}
     */
    public static Card restore(String id, String category, String question, String answer, String sourceFile) {
        // если в файле ID стерся, создаем новый
        String validId = (id == null || id.trim().isEmpty())
                ? UUID.randomUUID().toString()
                : id;

        return new Card(
                validId,
                category,
                question,
                answer,
                sourceFile,
                0, // уровень будет перезаписан SessionManager'ом
                true  // статус тоже
        );
    }

    /** Приватный конструктор */
    private CardFactory() {}
}