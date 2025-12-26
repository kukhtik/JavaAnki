package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Serializable {
    private String id; // NEW: Уникальный идентификатор
    private String category;
    private String question;
    private String answer;
    private String sourceFile;

    private int level = 0;
    private boolean isNew = true;

    // Конструктор для новых карт (генерирует UUID сразу)
    public Card(String category, String question, String answer, String sourceFile) {
        this.id = UUID.randomUUID().toString();
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.sourceFile = sourceFile;
    }

    // Конструктор для загрузки существующей карты (если ID уже был в файле)
    public Card(String id, String category, String question, String answer, String sourceFile) {
        this.id = (id == null || id.isEmpty()) ? UUID.randomUUID().toString() : id;
        this.category = category;
        this.question = question;
        this.answer = answer;
        this.sourceFile = sourceFile;
    }
}