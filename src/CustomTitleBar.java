import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class CustomTitleBar extends JPanel {
    private final JFrame parentFrame;
    private Point initialClick;
    private JLabel titleLabel;
    private JButton closeBtn;
    private JButton minimizeBtn;

    public CustomTitleBar(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new BorderLayout());
        setBackground(ThemeManager.getPanelBackground());
        setPreferredSize(new Dimension(frame.getWidth(), 35));

        // --- Заголовок и иконка ---
        titleLabel = new JLabel("  Java Anki: Master Edition");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(ThemeManager.getText());
        add(titleLabel, BorderLayout.WEST);

        // --- Кнопки управления (справа) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        minimizeBtn = createTitleButton("—");
        minimizeBtn.addActionListener(e -> frame.setState(Frame.ICONIFIED));

        closeBtn = createTitleButton("✕");
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(232, 17, 35)); // Красный при наведении
                closeBtn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                updateThemeColors(); // Возврат к теме
            }
        });
        closeBtn.addActionListener(e -> System.exit(0));

        buttonPanel.add(minimizeBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.EAST);

        // --- Логика перетаскивания окна ---
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Получаем позицию окна
                int thisX = parentFrame.getLocation().x;
                int thisY = parentFrame.getLocation().y;

                // Определяем смещение
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                // Перемещаем окно
                int X = thisX + xMoved;
                int Y = thisY + yMoved;
                parentFrame.setLocation(X, Y);
            }
        });

        updateThemeColors();
    }

    private JButton createTitleButton(String text) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 35));
        btn.setFocusPainted(false);
        btn.setBorder(null);
        btn.setFont(new Font("Arial", Font.PLAIN, 14));
        return btn;
    }

    // Метод для обновления цветов при смене темы
    public void updateThemeColors() {
        Color bg = ThemeManager.getPanelBackground();
        Color txt = ThemeManager.getText();
        Color btnHover = ThemeManager.isDark() ? new Color(75, 75, 75) : new Color(220, 220, 220);

        this.setBackground(bg);
        titleLabel.setForeground(txt);

        minimizeBtn.setBackground(bg);
        minimizeBtn.setForeground(txt);

        closeBtn.setBackground(bg);
        closeBtn.setForeground(txt);
    }
}