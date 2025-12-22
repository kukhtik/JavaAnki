import javax.swing.*;
import java.awt.event.ActionEvent;

public class KeyController {
    private MainFrame frame;

    public KeyController(MainFrame frame) {
        this.frame = frame;
    }

    public void setupKeyBindings() {
        JRootPane root = frame.getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // 1. ENTER -> Проверить
        im.put(KeyStroke.getKeyStroke("ENTER"), "check");
        am.put("check", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Если ответ еще не показан -> проверяем
                if (!frame.isAnswerRevealed()) {
                    AppLogger.input("ENTER", "Проверка ответа");
                    frame.checkAnswer();
                }
            }
        });

        // 2. LEFT ARROW -> Ошибка
        im.put(KeyStroke.getKeyStroke("LEFT"), "wrong");
        am.put("wrong", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (frame.isAnswerRevealed()) {
                    AppLogger.input("LEFT ARROW", "Результат: Ошибка");
                    frame.submitResult(false);
                }
            }
        });

        // 3. RIGHT ARROW -> Верно
        im.put(KeyStroke.getKeyStroke("RIGHT"), "correct");
        am.put("correct", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (frame.isAnswerRevealed()) {
                    AppLogger.input("RIGHT ARROW", "Результат: Верно");
                    frame.submitResult(true);
                }
            }
        });

        // 4. 'M' -> Смена режима
        im.put(KeyStroke.getKeyStroke('m'), "mode");
        am.put("mode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AppLogger.input("M", "Смена режима");
                frame.toggleMode();
            }
        });

        AppLogger.info("Клавиатурное управление инициализировано (KeyController)");
    }
}