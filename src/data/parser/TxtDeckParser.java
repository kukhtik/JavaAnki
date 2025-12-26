package data.parser;

import model.Card;
import util.CardParser; // Используем CardParser для обработки блоков
import java.util.List;
import java.util.ArrayList;

public class TxtDeckParser {

    /**
     * Преобразует список строк (содержимое .txt файла) в список объектов Card.
     * @param lines Список строк из файла.
     * @param fileName Имя файла, из которого были прочитаны строки (для поля sourceFile в Card).
     * @return Список объектов Card.
     */
    public List<Card> parse(List<String> lines, String fileName) {
        List<Card> cards = new ArrayList<>();

        // Используем CardParser, который умеет парсить отдельные блоки
        // Нам нужно разбить файл на блоки по "==="
        List<String> currentBlock = new ArrayList<>();

        for (String rawLine : lines) {
            String line = rawLine.replace("\uFEFF", "").trim(); // Удаляем BOM

            if (line.equals("===")) {
                if (!currentBlock.isEmpty()) {
                    Card card = CardParser.parseSingleBlock(currentBlock, fileName);
                    if (card != null) {
                        cards.add(card);
                    }
                }
                currentBlock.clear();
            } else {
                currentBlock.add(rawLine); // Сохраняем оригинальную строку для парсера
            }
        }
        // Обработка последнего блока, если он не завершен "==="
        if (!currentBlock.isEmpty()) {
            Card card = CardParser.parseSingleBlock(currentBlock, fileName);
            if (card != null) {
                cards.add(card);
            }
        }

        return cards;
    }
}