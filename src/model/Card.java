package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Карточка для запоминания
 * <p>
 * Содержит как контент (вопрос/ответ), так и метаданные прогресса (уровень, статус)
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Serializable {

    /** Уникальный идентификатор (UUID). Связь карточки со статистикой в {@code anki_stats.txt} */
    private String id;

    /** Название категории */
    private String category;

    /** Текст вопроса */
    private String question;

    /** Текст ответа */
    private String answer;

    /**
     * Файл-источника
     */
    private String sourceFile;

    /**
     * Уровень карточки.
     * <br>0 - новая
     * <br>1..4 - в процессе (Learning)
     * <br>5+ - выучено (Master)
     */
    private int level;

    /**
     * Флаг новой карточки
     */
    private boolean isNew;
}