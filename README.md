# JavaAnki

**JavaAnki** - это десктопное приложение для интервального повторения (SRS), написанное на чистой Java (Swing)

## **В JavaAnki.zip лежит .exe**

## **Работа с приложением**

* В корне лежат файлы:
     * anki_stats.txt - информация о вопросах, их расположение в базе.
     * history_log.txt - история ответов, в том числе полный ответ, каким бы он ни был.
     * import.txt - файл для импорта вопросов в базу.
     * каталог decks - вопросы, отсортированные по категориям.
     * decks\structure.txt - структура тем, для упорядочивания маленьких тем, опциональна.
##  Ключевые особенности

*   **Архитектура MVC & Passive View:** Четкое разделение логики (Controller), отображения (View) и данных (Model).
*   **Session Management:** Централизованный менеджер сессии (`SessionManager`), управляющий жизненным циклом данных, дедупликацией и миграцией.
*   **Умный алгоритм (SRS):**
    *   Система уровней (Box 0-10).
    *   "Мягкий сброс" прогресса при ошибках на высоких уровнях.
    *   Взвешенный случайный выбор (Weighted Random) карт.
*   **Data Integrity (UUID):** Каждая карточка имеет уникальный ID, что позволяет редактировать текст вопроса без потери прогресса обучения.
*   **Fuzzy Matching:** Проверка ответов с толерантностью к опечаткам (алгоритм Левенштейна + токенизация).
*   **Event-Driven UI:** Компоненты общаются через шину событий (`EventBus`), обновляясь автоматически при изменении данных.
*   **Deep Theming:** Темная/Светлая тема с рекурсивной перекраской всех компонентов, включая ScrollBars и ComboBoxes.

---

## Технологический стек

*   **Язык:** Java 17+
*   **UI:** Swing (Custom Look & Feel, No 3rd party libs).
*   **Patterns:** Repository, Facade, MVC, Observer, Factory Method, Singleton.
*   **Testing:** JUnit 5 (Unit Tests) + Custom Integration Runners.
*   **Tools:** Lombok, Java Util Logging.

---

## Установка и Запуск

### Требования
1.  **JDK 17** или выше.
2.  **Lombok** (подключен в IDE или в classpath при компиляции).

### Запуск из IDE
1.  Откройте проект в IntelliJ IDEA.
2.  Убедитесь, что включена обработка аннотаций (Annotation Processing).
3.  Запустите класс `app.App`.

### Запуск тестов
В проекте реализована гибридная система тестирования:
*   **Unit-тесты (JUnit 5):** Запускайте папку `src` через встроенный раннер IDE.
*   **Ручные интеграционные тесты:** Запустите класс `TestRunner` (проверяет парсеры, алгоритмы и UI-контроллеры без поднятия тяжелого GUI).

---

## Руководство пользователя

### 1. Импорт вопросов
Создайте файл `import.txt` в корне программы. Формат (разделитель `===`):

```text
CATEGORY: Java Basics
QUESTION:
Что такое JVM?
ANSWER:
Java Virtual Machine - виртуальная машина, исполняющая байт-код.
===
CATEGORY: SQL
QUESTION:
Типы JOIN?
ANSWER:
INNER, LEFT, RIGHT, FULL, CROSS.
===
```

1.  Перейдите на вкладку **"Статистика"**.
2.  Нажмите **"Импорт из файла"**.
3.  Система обработает файл:
    *   Успешные карты добавятся в файлы `decks/CategoryName.txt`.
    *   Успешные блоки удалятся из `import.txt`.
    *   Ошибки и дубликаты останутся в `import.txt` для исправления.

### 2. Организация групп
Файл `decks/structure.txt` позволяет объединять разрозненные файлы в темы:

```text
GROUP: Java Full Stack
FILES: core.txt, collections.txt, spring.txt, sql.txt
```

### 3. Горячие клавиши (Обучение)
*   `ENTER` - Проверить ответ.
*   `->` (Вправо) - Ответ верный (Повысить уровень).
*   `<-` (Влево) - Ответ неверный (Сбросить уровень).

---

## Файловая структура данных

*   `decks/*.txt` - Файлы с вопросами. Формируются автоматически или вручную.
*   `anki_stats.txt` - Хранит прогресс: `UUID | Level`. Позволяет менять текст вопросов без сброса уровня.
*   `history_log.txt` - Полный лог ответов пользователя.
*   `import.txt` - Буфер для добавления новых карт.

---

## Структура исходного кода

Проект разбит на пакеты согласно ответственности (Layered Architecture):

```text
src/
├── app/
│   └── App.java                 # Точка входа (Composition Root)
├── config/
│   └── ThemeColors.java         # Цветовая палитра (Dark/Light)
├── data/
│   ├── parser/
│   │   └── TxtDeckParser.java   # Логика разбора файлов колод
│   ├── repository/              # Слой доступа к данным
│   │   ├── CardRepository.java  # Интерфейс репозитория карт
│   │   ├── FileDeckRepository.java
│   │   ├── FileStatsRepository.java
│   │   ├── GroupRepository.java
│   │   ├── HistoryRepository.java
│   │   └── StatsRepository.java # Интерфейс репозитория статистики
│   └── FileService.java         # Низкоуровневый I/O (NIO.2)
├── model/
│   ├── dto/
│   │   └── StatsRow.java        # DTO для таблицы статистики
│   ├── Card.java                # Основная сущность (Entity)
│   └── HistoryRecord.java       # Неизменяемая запись лога (Record)
├── service/
│   ├── algorithm/
│   │   └── SpacedRepetitionAlgorithm.java # Математика SRS
│   ├── session/
│   │   └── SessionManager.java  # Управление состоянием приложения
│   ├── GradingService.java      # Проверка ответов (Fuzzy Logic)
│   ├── ImportService.java       # Логика массового импорта
│   └── StudyService.java        # Фасад (Facade) для UI
├── ui/
│   ├── components/              # Кастомные UI компоненты
│   │   ├── CustomTitleBar.java
│   │   ├── DarkScrollBarUI.java
│   │   └── UIFactory.java
│   ├── controller/              # MVC Контроллеры
│   │   └── StudyController.java
│   ├── dialogs/
│   │   └── DialogFactory.java   # Кастомные модальные окна
│   ├── panels/                  # Основные экраны (Views)
│   │   ├── HistoryPanel.java
│   │   ├── StatsPanel.java
│   │   └── StudyPanel.java
│   ├── MainFrame.java           # Главное окно (Root Container)
│   └── ThemeManager.java        # Движок темизации и покраски
└── util/
    ├── CardFactory.java         # Генерация ID и создание карт
    ├── CardParser.java          # Утилита парсинга блоков
    ├── EventBus.java            # Шина событий (Observer)
    └── TextUtil.java            # Нормализация текста
```

---

## Тестирование

Проект покрыт тестами для критических узлов:

1.  **Алгоритм SRS:** Проверка математики повышения/понижения уровней и вероятностей выпадения.
2.  **Парсеры:** Проверка устойчивости к битым файлам и сохранение форматирования.
3.  **Импорт:** Интеграционные тесты на `MemoryFileService` (виртуальная ФС).
4.  **Сессия:** Тесты дедупликации и миграции Legacy-данных.

5.  **Контроллеры:** Тестирование потока UI (Start -> Check -> Submit) на заглушках (Fakes).
