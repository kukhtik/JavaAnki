package ui.dialogs;

import ui.ThemeManager;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Фабрика кастомных диалоговых окон.
 * <p>
 * Класс заменяет стандартный {@link JOptionPane},
 * который невозможно перекрасить в тёмную тему
 * <br>
 * Этот класс создает полностью кастомные (undecorated) окна, которые:
 * <ul>
 *     <li>Поддерживают текущую цветовую схему (через {@link ThemeManager})</li>
 *     <li>Имеют кастомный заголовок с кнопкой закрытия</li>
 *     <li>Поддерживают перетаскивание за заголовок (так как системного нет)</li>
 * </ul>
 * </p>
 */
public class DialogFactory {

    /**
     * Показывает простое модальное информационное окно (аналог info)
     *
     * @param parent компонент, относительно которого центрируется окно
     * @param message текст сообщения
     */
    public static void showInfo(Component parent, String message) {
        // родительское окно для правильной привязки модальности
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);

        JDialog dialog = new JDialog(parentWindow, "Сообщение", Dialog.ModalityType.APPLICATION_MODAL);

        // отключаем системное оформление окна
        dialog.setUndecorated(true);

        // корневая панель с рамкой (иногда сливалось с другими окнами)
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new LineBorder(ThemeManager.getBorder(), 1));
        root.setBackground(ThemeManager.getPanel());

        // кастомный заголовок
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(ThemeManager.getPanel());
        titleBar.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel titleLbl = new JLabel("Информация");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        titleLbl.setForeground(ThemeManager.getText());

        JButton closeBtn = new JButton("X");
        styleCloseButton(closeBtn, dialog);

        titleBar.add(titleLbl, BorderLayout.WEST);
        titleBar.add(closeBtn, BorderLayout.EAST);

        // логика перетаскивания (так как системной рамки нет)
        makeDraggable(dialog, titleBar);

        // контент
        JTextArea area = new JTextArea(message);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Arial", Font.PLAIN, 14));
        area.setBackground(ThemeManager.getPanel());
        area.setForeground(ThemeManager.getText());
        area.setBorder(new EmptyBorder(20, 20, 20, 20));

        // кнопки
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(ThemeManager.getPanel());
        btnPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JButton okBtn = new JButton("OK");
        styleOkButton(okBtn, dialog);
        btnPanel.add(okBtn);

        // сборка
        root.add(titleBar, BorderLayout.NORTH);
        root.add(area, BorderLayout.CENTER);
        root.add(btnPanel, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(400, 250);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true); // блокирует поток, пока окно не закроется
    }
    /**
     * Стилизация кнопки закрытия ("крестик"), эффект красной подсветки при наведении
     */
    private static void styleCloseButton(JButton btn, JDialog dialog) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(ThemeManager.getPanel());
        btn.setForeground(ThemeManager.getText());
        btn.setPreferredSize(new Dimension(30, 20));

        btn.addActionListener(e -> dialog.dispose());

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(232, 17, 35));
                btn.setForeground(Color.WHITE);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(ThemeManager.getPanel());
                btn.setForeground(ThemeManager.getText());
            }
        });
    }

    /**
     * Стилизация кнопки OK
     */
    private static void styleOkButton(JButton btn, JDialog dialog) {
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setForeground(ThemeManager.getText());
        btn.setBackground(ThemeManager.getButton());
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorder(new CompoundBorder(new LineBorder(ThemeManager.getBorder()), new EmptyBorder(8, 30, 8, 30)));

        btn.addActionListener(e -> dialog.dispose());

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(ThemeManager.getButtonHover()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(ThemeManager.getButton()); }
        });
    }

    /**
     * Реализация перетаскивания окна за произвольный компонент (заголовок)
     */
    private static void makeDraggable(JDialog dialog, JComponent title) {
        MouseAdapter ma = new MouseAdapter() {
            int pX, pY;

            // точка, где схватили мышкой
            public void mousePressed(MouseEvent e) {
                pX = e.getX();
                pY = e.getY();
            }

            // двигаем окно на разницу координат
            public void mouseDragged(MouseEvent e) {
                dialog.setLocation(
                        dialog.getLocation().x + e.getX() - pX,
                        dialog.getLocation().y + e.getY() - pY
                );
            }
        };
        title.addMouseListener(ma);
        title.addMouseMotionListener(ma);
    }

    /** Приватный конструктор (Utility Class) */
    private DialogFactory() { }
}