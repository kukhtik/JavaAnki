package ui.components;

import ui.ThemeManager;

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
        var titleLabel = new JLabel("  Java Anki: Master Edition");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(titleLabel, BorderLayout.WEST);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);

        // Кнопка Темы (Сделали шире: 80px)
        var themeBtn = createBarButton("Theme", e -> {
            ThemeManager.toggle();
            ThemeManager.apply(parent);
            parent.repaint();
            LOGGER.info("Тема переключена: " + (ThemeManager.isDark() ? "Dark" : "Light"));
        });
        themeBtn.setPreferredSize(new Dimension(80, 35));

        var minBtn = createBarButton("_", e -> parent.setState(Frame.ICONIFIED));

        var closeBtn = createBarButton("X", e -> {
            LOGGER.info("Приложение закрыто через CustomTitleBar");
            System.exit(0);
        });

        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(232, 17, 35));
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setOpaque(true);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(ThemeManager.getPanel());
                closeBtn.setForeground(ThemeManager.getText());
                closeBtn.setOpaque(false);
            }
        });

        buttonPanel.add(themeBtn);
        buttonPanel.add(minBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.EAST);

        setBackground(ThemeManager.getPanel());
    }

    private JButton createBarButton(String text, java.awt.event.ActionListener action) {
        var btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 35));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(ThemeManager.getText());
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