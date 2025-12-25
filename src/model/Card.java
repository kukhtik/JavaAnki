package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Учебная карточка (flashcard), содержащей вопрос, ответ, категорию и служебную информацию.
 * <p>
 * Карточка используется в системе обучения, хранит статические данные
 * (категория, вопрос, ответ, источник) и динамическое состояние:
 * уровень освоения и флаг "новая"
 * <p>
 * Lombok-аннотации:
 * <ul>
 *     <li>{@code @Data} - генерирует геттеры, сеттеры, equals/hashCode и toString</li>
 *     <li>{@code @NoArgsConstructor} - создаёт пустой конструктор</li>
 *     <li>{@code @AllArgsConstructor} - создаёт конструктор со всеми полями</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Serializable {
    private String category;
    private String question;
    private String answer;
    private String sourceFile;
    /**
     * Уровень освоения карточки.
     * Используется алгоритмом повторений для определения частоты показа.
     * Значение по умолчанию - 0
     */
    private int level = 0;

    /**
     * Флаг, указывающий, что карточка новая и ещё не использовалась в обучении.
     * Значение по умолчанию - true
     */
    private boolean isNew = true;

    /**
     * Конструктор, используемый при парсинге карточек из файлов.
     * <p>
     * В этом случае состояние обучения (level, isNew) ещё неизвестно
     * и остаётся по умолчанию
     *
     * @param category категория карточки
     * @param question текст вопроса
     * @param answer текст ответа
     * @param sourceFile имя файла-источника
     */
    public Card(String category, String question, String answer, String sourceFile) {
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.sourceFile = sourceFile;
    }
}
