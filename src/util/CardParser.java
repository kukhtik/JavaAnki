package util;

import model.Card;
import java.util.List;

/**
 * Утилита для парсинга текстовых блоков в объекты карточек
 * <p>
 * Работает с форматом:
 * <pre>
 * ID: ...
 * CATEGORY: ...
 * QUESTION: ...
 * ANSWER: ...
 * ===
 * </pre>
 * </p>
 */
public class CardParser {

    /**
     * Разбирает один блок строк (между разделителями {@code ===})
     * <p>
     * Использует конечный автомат (State Machine) для определения, к какому полю
     * относится текущая строка (Вопрос или Ответ)
     * </p>
     *
     * @param block список строк, относящихся к одной карточке
     * @param sourceFile имя файла-источника (передается в Фабрику)
     * @return объект {@link Card} или {@code null}, если блок не содержит обязательных полей
     */
    public static Card parseSingleBlock(List<String> block, String sourceFile) {
        String currentCat = null;
        StringBuilder qBuf = new StringBuilder();
        StringBuilder aBuf = new StringBuilder();
        String currentId = null;

        // состояние парсера: 0=Meta (заголовки), 1=Question body, 2=Answer body
        int state = 0;

        for (String rawLine : block) {
            // удаляем BOM и лишние пробелы по краям
            String line = rawLine.replace("\uFEFF", "").trim();

            if (line.startsWith("ID:")) {
                currentId = line.substring(3).trim();
            } else if (line.startsWith("CATEGORY:")) {
                currentCat = line.substring(9).trim();
                state = 0; // сброс состояния
            } else if (line.startsWith("QUESTION:")) {
                state = 1; // чтения многострочного вопроса
            } else if (line.startsWith("ANSWER:")) {
                state = 2; // чтения многострочного ответа
            } else {
                // \n чтобы сохранить форматирование внутри вопроса/ответа
                if (state == 1) qBuf.append(rawLine).append("\n");
                else if (state == 2) aBuf.append(rawLine).append("\n");
            }
        }

        // карточка обязана иметь категорию, вопрос и ответ
        if (!qBuf.isEmpty() && !aBuf.isEmpty() && currentCat != null && !currentCat.isEmpty()) {
            String qStr = qBuf.toString().trim();
            String aStr = aBuf.toString().trim();

            // делегируем создание объекта Фабрике
            if (currentId == null || currentId.isEmpty()) {
                // миграция: если ID нет, создаем новую
                return CardFactory.createNew(currentCat, qStr, aStr, sourceFile);
            } else {
                // восстановление
                return CardFactory.restore(currentId, currentCat, qStr, aStr, sourceFile);
            }
        }
        return null; // блок невалиден
    }

    private CardParser() {}
}