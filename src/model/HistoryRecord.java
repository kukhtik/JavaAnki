package model;

/**
 * Запись истории ответа пользователя
 */
public record HistoryRecord(String date, String question, String userAnswer, boolean isCorrect) {
    /**
     * Метка результата
     */
    public String getResultLabel() {
        return isCorrect ? "ВЕРНО" : "ОШИБКА";
    }
}
