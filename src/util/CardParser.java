package util;

import model.Card;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CardParser {

    /**
     * Парсит список строк (например, содержимое файла) в список карт.
     */
    public static List<Card> parseAll(List<String> lines, String defaultFileName) {
        List<Card> cards = new ArrayList<>();
        // Если имя файла пришло, используем его как дефолтную категорию
        String currentCat = defaultFileName != null ? defaultFileName.replace(".txt", "") : "General";

        StringBuilder qBuf = new StringBuilder();
        StringBuilder aBuf = new StringBuilder();
        String currentId = null;
        int state = 0; // 0=Meta, 1=Question, 2=Answer

        for (String rawLine : lines) {
            String line = rawLine.replace("\uFEFF", "").trim();

            if (line.equals("===")) {
                createCard(cards, currentId, currentCat, qBuf, aBuf, defaultFileName);
                qBuf.setLength(0); aBuf.setLength(0); currentId = null; state = 0;
                continue;
            }

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
        // Хвост
        createCard(cards, currentId, currentCat, qBuf, aBuf, defaultFileName);

        return cards;
    }

    /**
     * Пытается распарсить один блок текста в карту.
     * Используется в ImportService.
     * @return Card или null, если блок невалиден.
     */
    public static Card parseSingleBlock(List<String> block, String sourceFile) {
        List<Card> result = parseAll(block, sourceFile);
        return result.isEmpty() ? null : result.get(0);
    }

    private static void createCard(List<Card> list, String id, String cat, StringBuilder q, StringBuilder a, String src) {
        if (q.length() > 0 && a.length() > 0) {
            // Если ID нет (старый формат), генерируем новый. В модели есть конструктор для этого.
            String qStr = q.toString().trim();
            String aStr = a.toString().trim();

            if (id == null || id.isEmpty()) {
                list.add(new Card(cat, qStr, aStr, src)); // Генерирует новый UUID
            } else {
                list.add(new Card(id, cat, qStr, aStr, src)); // Использует существующий
            }
        }
    }
}