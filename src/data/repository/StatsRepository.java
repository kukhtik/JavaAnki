package data.repository;

import model.Card;

import java.util.List;
import java.util.Map;

public interface StatsRepository {
    Map<String, Integer> loadStats();
    void saveStats(List<Card> cards);
}