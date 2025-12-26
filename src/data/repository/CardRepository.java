package data.repository;

import model.Card;
import java.util.List;

public interface CardRepository {
    List<Card> loadAllCards();
    void saveDeck(String fileName, List<Card> cards);
}