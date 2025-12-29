package data.parser;

import model.Card;
import util.CardParser;
import java.util.List;
import java.util.ArrayList;

/**
 * Парсер для текстовых файлов с колодами ( .txt)
 * <p>
 * Отвечает за разбиение текста файла на логические блоки,
 * где каждый блок соответствует одной карточке
 * </p>
 * <p>
 * Формат файла:
 * Карточки разделяются разделителем {@code ===}.
 * Парсинг содержимого самого блока делегируется утилите {@link CardParser}
 * </p>
 */
public class TxtDeckParser {

    /**
     * Преобразует список строк (из .txt файла) в список объектов {@link Card}
     * <p>
     * Метод проходит по строкам, накапливает их в буфер (текущий блок) и, встречая разделитель,
     * отправляет блок на распознавание
     * </p>
     * @param lines список строк, прочитанных из файла
     * @param fileName имя файла (используется для заполнения метаданных карточки {@code sourceFile},
     *                 чтобы знать, из какой категории/файла пришла карта)
     * @return корректно распознанные объекты Card
     */
    public List<Card> parse(List<String> lines, String fileName) {
        List<Card> cards = new ArrayList<>();

        // буфер для накопления строк текущей карточки
        List<String> currentBlock = new ArrayList<>();

        for (String rawLine : lines) {
            // удаляем BOM (Byte Order Mark) - спецсимвол \uFEFF, который может быть в начале файла UTF-8,
            // делаем trim() для надежной проверки на равенство разделителю
            String line = rawLine.replace("\uFEFF", "").trim();

            // строка разделитель?
            if (line.equals("===")) {
                // разделитель и буфер не пуст - закончилась предыдущая карточка
                if (!currentBlock.isEmpty()) {
                    processBlock(currentBlock, fileName, cards);
                }
                // чистим буфер для следующей карточки
                currentBlock.clear();
            } else {
                // если нет то, добавляем оригинальную строку (rawLine) в буфер
                // rawLine оригинальный, чтобы вопрос не поплыл
                currentBlock.add(rawLine);
            }
        }

        // если конес не === , то всё равно сохраняем
        if (!currentBlock.isEmpty()) {
            processBlock(currentBlock, fileName, cards);
        }

        return cards;
    }

    /**
     * Обработка одного блока текста
     */
    private void processBlock(List<String> block, String fileName, List<Card> resultList) {
        // CardParser.parseSingleBlock возвращает null, если блок невалиден (нет вопроса/ответа)
        Card card = CardParser.parseSingleBlock(block, fileName);
        if (card != null) {
            resultList.add(card);
        }
    }
}