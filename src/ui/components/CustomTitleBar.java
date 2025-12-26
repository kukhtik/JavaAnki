package ui.components;

import ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class CustomTitleBar extends JPanel {
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
        var titleLabel = new JLabel("  Java Anki: Master Edition");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(titleLabel, BorderLayout.WEST);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        // Кнопка Темы
        var themeBtn = createBarButton("Theme", e -> {
            Theme.toggle();
            Theme.apply(parent);
            parent.repaint();
        });
        themeBtn.setPreferredSize(new Dimension(60, 35));

        // Кнопка Свернуть (используем нижнее подчеркивание)
        var minBtn = createBarButton("_", e -> parent.setState(Frame.ICONIFIED));

        // Кнопка Закрыть (обычный X)
        var closeBtn = createBarButton("X", e -> System.exit(0));

        // Красный цвет при наведении на закрытие
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(232, 17, 35));
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setOpaque(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(Theme.getPanel());
                closeBtn.setForeground(Theme.getText());
                closeBtn.setOpaque(false);
            }
        });

        buttonPanel.add(themeBtn);
        buttonPanel.add(minBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.EAST);

        // Чтобы фон панели соответствовал теме при старте
        setBackground(Theme.getPanel());
    }

    private JButton createBarButton(String text, java.awt.event.ActionListener action) {
        var btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 35));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false); // Прозрачный по умолчанию
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(Theme.getText());
        btn.addActionListener(action);
        return btn;
    }

    private void initDragListeners() {
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { initialClick = e.getPoint(); }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int thisX = parent.getLocation().x;
                int thisY = parent.getLocation().y;
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;
                parent.setLocation(thisX + xMoved, thisY + yMoved);
            }
        });
    }
}