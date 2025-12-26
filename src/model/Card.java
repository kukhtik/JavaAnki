package model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card implements Serializable {
    private String id;
    private String category;
    private String question;
    private String answer;
    private String sourceFile;

    private int level;
    private boolean isNew;
}