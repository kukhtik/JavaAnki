import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AppLogger {
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void info(String msg) {
        System.out.println(String.format("[%s] [INFO] %s", LocalTime.now().format(TIME_FMT), msg));
    }

    public static void input(String key, String action) {
        System.out.println(String.format("[%s] [INPUT] Нажата клавиша: %s -> %s", LocalTime.now().format(TIME_FMT), key, action));
    }

    public static void error(String msg) {
        System.err.println(String.format("[%s] [ERROR] %s", LocalTime.now().format(TIME_FMT), msg));
    }

    public static void error(String msg, Exception e) {
        System.err.println(String.format("[%s] [ERROR] %s: %s", LocalTime.now().format(TIME_FMT), msg, e.getMessage()));
        // e.printStackTrace(); // Раскомментируйте для полного стека
    }
}