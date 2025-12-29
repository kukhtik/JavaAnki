package ui.components;

import ui.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Logger;

/**
 * Кастомная панель заголовка окна
 * <p>
 * Используется вместо стандартной системной рамки окна, чтобы не было всетлок рамки по верх черной
 * Умеем:
 * <ul>
 *     <li>Перетаскивать окно мышью</li>
 *     <li>Кнопки управления: закрыть, свернуть, переключить тему</li>
 *     <li>Отображать название приложения</li>
 * </ul>
 * Визуально интегрируется с текущей цветовой схемой {@link ThemeManager}
 * </p>
 */
public class CustomTitleBar extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(CustomTitleBar.class.getName());

    /** Ссылка на родительское окно, которым нужно управлять (перемещать, закрывать) */
    private final JFrame parent;

    /** Точка нажатия мыши для расчета смещения при перетаскивании */
    private Point initialClick;

    /**
     * Панель заголовка
     */
    public CustomTitleBar(JFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        // высота заголовка фиксирована = 35px
        setPreferredSize(new Dimension(parent.getWidth(), 35));

        initUI();
        initDragListeners();
    }

    /**
     * Визуальные компоненты
     */
    private void initUI() {
        // лого/название слева
        var titleLabel = new JLabel("  JavaAnki");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        add(titleLabel, BorderLayout.WEST);

        // панель кнопок справа
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false); // прозрачный фон, чтобы видеть цвет заголовка

        // кнопка смены темы
        var themeBtn = createBarButton("Theme", _ -> {
            ThemeManager.toggle(); // переключаем флаг темы
            ThemeManager.apply(parent); // обновляем UIManager и цвета компонентов
            parent.repaint(); // перерисовываем окно целиком
            LOGGER.info("Тема переключена: " + (ThemeManager.isDark() ? "Dark" : "Light"));
        });
        themeBtn.setPreferredSize(new Dimension(80, 35));

        // кнопка "свернуть" (_)
        var minBtn = createBarButton("_", _ -> parent.setState(Frame.ICONIFIED));

        // кнопка "закрыть" (X)
        var closeBtn = createBarButton("X", _ -> {
            LOGGER.info("Приложение закрыто пользователем через CustomTitleBar");
            System.exit(0);
        });

        // эффект для кнопки закрытия: краснеет при наведении (как в винде)
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(232, 17, 35));
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setOpaque(true); // отрисовка фона
            }
            @Override
            public void mouseExited(MouseEvent e) {
                // возвращаем цвета темы
                closeBtn.setBackground(ThemeManager.getPanel());
                closeBtn.setForeground(ThemeManager.getText());
                closeBtn.setOpaque(false);
            }
        });

        buttonPanel.add(themeBtn);
        buttonPanel.add(minBtn);
        buttonPanel.add(closeBtn);
        add(buttonPanel, BorderLayout.EAST);

        // установка начального фона панели
        setBackground(ThemeManager.getPanel());
    }

    /**
     * Фабрика для создания стилизованных кнопок заголовка.
     * Кнопки по умолчанию прозрачные, без границ
     */
    private JButton createBarButton(String text, java.awt.event.ActionListener action) {
        var btn = new JButton(text);
        btn.setPreferredSize(new Dimension(45, 35));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false); // убирает стандартный фон Swing-кнопки
        btn.setOpaque(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(ThemeManager.getText());
        btn.addActionListener(action);
        return btn;
    }

    /**
     * Добавляет слушатели мыши для реализации перетаскивания окна.
     * <p>
     * Логика:
     * 1. При нажатии (mousePressed) запоминаем координаты курсора внутри компонента
     * 2. При перемещении с зажатой кнопкой (mouseDragged) вычисляем дельту
     *    и сдвигаем окно (parent) на эту дельту
     * </p>
     */
    private void initDragListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // текущие координаты окна на экране
                int thisX = parent.getLocation().x;
                int thisY = parent.getLocation().y;

                // сдвиг мыши относительно точки нажатия
                int xMoved = e.getX() - initialClick.x;
                int yMoved = e.getY() - initialClick.y;

                // новая позиция окна
                int X = thisX + xMoved;
                int Y = thisY + yMoved;

                parent.setLocation(X, Y);
            }
        });
    }
}