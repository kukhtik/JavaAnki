package util;

import model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CardParser {

    /**
     * Пытается распарсить один блок текста в карту.
     * Используется в ImportService и TxtDeckParser.
     * @param block Список строк, представляющих один блок карты (например, между "===").
     * @param sourceFile Имя файла, откуда пришел блок.
     * @return Card объект или null, если блок невалиден.
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

        // Проверяем, что все обязательные поля есть
        if (qBuf.length() > 0 && aBuf.length() > 0 && currentCat != null && !currentCat.isEmpty()) {
            String qStr = qBuf.toString().trim();
            String aStr = aBuf.toString().trim();

            // Если ID не был найден в блоке (старый формат), генерируем новый.
            if (currentId == null || currentId.isEmpty()) {
                return new Card(currentCat, qStr, aStr, sourceFile);
            } else {
                return new Card(currentId, currentCat, qStr, aStr, sourceFile);
            }
        }
        return null; // Блок невалиден
    }
}