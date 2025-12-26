package util;

import model.Card;
import java.util.List;

public class CardParser {

    /**
     * Пытается распарсить один блок текста в карту.
     */
    public static Card parseSingleBlock(List<String> block, String sourceFile) {
        String currentCat = null;
        StringBuilder qBuf = new StringBuilder();
        StringBuilder aBuf = new StringBuilder();
        String currentId = null;
        int state = 0; // 0=Meta, 1=Question, 2=Answer

        for (String rawLine : block) {
            String line = rawLine.replace("\uFEFF", "").trim();

            if (line.startsWith("ID:")) {
                currentId = line.substring(3).trim();
            } else if (line.startsWith("CATEGORY:")) {
                currentCat = line.substring(9).trim();
                state = 0;
            } else if (line.startsWith("QUESTION:")) {
                state = 1;
            } else if (line.startsWith("ANSWER:")) {
                state = 2;
            } else {
                if (state == 1) qBuf.append(rawLine).append("\n");
                else if (state == 2) aBuf.append(rawLine).append("\n");
            }
        }

        // Валидация полей
        if (qBuf.length() > 0 && aBuf.length() > 0 && currentCat != null && !currentCat.isEmpty()) {
            String qStr = qBuf.toString().trim();
            String aStr = aBuf.toString().trim();

            // ИСПОЛЬЗУЕМ ФАБРИКУ
            if (currentId == null || currentId.isEmpty()) {
                // Это старая карта без ID -> Фабрика сгенерирует новый
                return CardFactory.createNew(currentCat, qStr, aStr, sourceFile);
            } else {
                // Это карта с ID -> Фабрика восстановит её
                return CardFactory.restore(currentId, currentCat, qStr, aStr, sourceFile);
            }
        }
        return null; // Блок невалиден
    }

    // Метод parseAll больше не нужен, так как FileDeckRepository теперь использует TxtDeckParser,
    // который может вызывать этот метод parseSingleBlock в цикле.
    // Но для совместимости с TxtDeckParser из предыдущего шага, можно оставить делегацию:
    public static List<Card> parseAll(List<String> lines, String defaultFileName) {
        // TxtDeckParser теперь реализует эту логику, этот метод можно удалить,
        // если обновить TxtDeckParser, чтобы он использовал parseSingleBlock напрямую.
        // Оставим пока пустым или удалим, если вы обновили TxtDeckParser в прошлом шаге.
        return new java.util.ArrayList<>();
    }
}