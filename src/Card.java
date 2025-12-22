public class Card {
    String category;
    String question;
    String answer;
    String sourceFile; // <-- Новое поле: имя файла

    int level = 0;
    boolean isNew = true;

    // Обновили конструктор, теперь он требует имя файла
    public Card(String c, String q, String a, String src) {
        this.category = c;
        this.question = q;
        this.answer = a;
        this.sourceFile = src;
    }

    public double getWeight() {
        if (!isNew && level == 0) return 150.0;
        if (isNew) return 100.0;
        return 100.0 * Math.pow(0.7, level);
    }
}