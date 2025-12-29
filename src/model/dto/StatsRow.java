package model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Объект передачи данных (DTO) для отображения строки статистики.
 * <p>
 * Этот класс не сущность базы данных. Это snapshot агрегированных данных,
 * вычисленных сервисом {@code StatsCalculator}, для отрисовки таблицы в {@code StatsPanel}
 * </p>
 */
@Data
@AllArgsConstructor
public class StatsRow {

    /** Название категории или имя файла */
    private String category;

    /** Общее количество карточек в этой категории */
    private long total;

    /** Количество новых карточек */
    private long newCards;

    /** Количество карточек в процессе изучения (Level 1-4) */
    private long learning;

    /** Количество выученных карточек (Level 5+) */
    private long master;
}