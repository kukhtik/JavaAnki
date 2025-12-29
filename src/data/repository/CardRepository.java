package data.repository;

import model.Card;
import java.util.List;

/**
 * Интерфейс репозитория для управления данными карточек,
 * определяет контракт (API) для операций чтения и записи колод
 */
public interface CardRepository {

    /**
     * Загружает все доступные карточки из хранилища
     *
     * @return общий список объектов {@link Card} из всех источников.
     */
    List<Card> loadAllCards();

    /**
     * Сохраняет список карточек в конкретный файл (или таблицу)
     *
     * @param fileName имя файла (идентификатор колоды), куда нужно сохранить данные
     * @param cards список карточек для сохранения
     */
    void saveDeck(String fileName, List<Card> cards);
}