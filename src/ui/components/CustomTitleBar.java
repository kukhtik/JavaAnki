package ui.components;

import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Logger;

public class CustomTitleBar extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CustomTitleBar.class.getName());
    private final JFrame parent;
    private Point initialClick;

    public CustomTitleBar(JFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(parent.getWidth(), 35));

        initUI();
        initDragListeners();
    }

    private void initUI() {
        // Заголовок
        var titleLabel = new JLabel("  Java Anki");
        titleLabel.setFont(UIFactory.FONT_BOLD);
        add(titleLabel, BorderLayout.WEST);

        // Панель кнопок (Справа)
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        // Кнопка Темы
        var themeBtn = createBarButton("☀/☾", e -> {
            Theme.toggle();
            Theme.apply(parent); // Перекрашиваем все окно
            parent.repaint();
            LOGGER.info("Тема переключена пользователем");
        });

        // Кнопка Свернуть
        var minBtn = createBarButton("—", e -> {
            parent.setState(Frame.ICONIFIED);
            LOGGER.info("Окно свернуто");
        });

        // Кнопка Закрыть (Красная)
        var closeBtn = createBarButton("✕", e -> {
            LOGGER.info("Приложение закрыто пользователем");
            System.exit(0);
        });

        // Ховер эффект для закрытия (красный)
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(232, 17, 35));
                closeBtn.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(Theme.getBackground());
                closeBtn.setForeground(Theme.getText());
            }
        });

        buttonPanel.add(themeBtn);
        buttonPanel.add(minBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.EAST);
    }

    private JButton createBarButton(String text, java.awt.event.ActionListener action) {
        var btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 35));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false); // Прозрачный фон
        btn.setOpaque(true);             // Чтобы работал setBackground
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.addActionListener(action);
        return btn;
    }

    private void initDragListeners() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Вычисляем новую позицию окна
                int thisX = parent.getLocation().x;
                int thisY = parent.getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                parent.setLocation(thisX + xMoved, thisY + yMoved);
            }
        });
    }
}