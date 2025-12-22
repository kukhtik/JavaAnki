import javax.swing.*;

public class JavaAnkiFinal {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
            new MainFrame().setVisible(true);
        });
    }
}