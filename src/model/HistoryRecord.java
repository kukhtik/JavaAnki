package model;

import lombok.Value;
/**
 * Запись истории ответа пользователя
 */
@Value
public class HistoryRecord {
    String date;
    String question;
    String userAnswer;
    boolean isCorrect;
    /**
     * Метка результата
     */
    public String getResultLabel(){
        return isCorrect?"ВЕРНО":"ОШИБКА";
    }
}
