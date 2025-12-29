import service.GradingTest;
import ui.controller.ManualControllerTest;
import util.ParserTest;

import java.awt.*;

/**
 * Test Runner
 * <p>
 * Объединяет все отдельные консольные тесты в один общий прогон (Test Suite).
 * Полезен для быстрой проверки работоспособности всей системы, если в среде разработки
 * не настроен JUnit
 * </p>
 */
void main() {
    IO.println("=========================================");
    IO.println("       STARTING ALL TESTS (SUITE)        ");
    IO.println("=========================================");

    // запуск теста парсера
    run("Parser Tests", ParserTest::main);

    // запуск теста алгоритма оценки строк
    run("Grading Tests", GradingTest::main);

    // запуск теста контроллера UI
    // проверка на headless режим (если запускаем на сервере без дисплея, UI тест упадет)
    if (!GraphicsEnvironment.isHeadless()) {
        run("Controller Integration Tests", ManualControllerTest::main);
    } else {
        IO.println("[SKIP] Controller Tests skipped (Headless mode detected)");
    }

    IO.println("=========================================");
    IO.println("           ALL TESTS FINISHED            ");
    IO.println("=========================================");
}

/**
 * Обертка для безопасного запуска теста
 *
 * @param name название блока тестов для логов
 * @param test лямбда с вызовом main метода теста
 */
private static void run(String name, Runnable test) {
    IO.println(">>> RUNNING: " + name + "...");
    long start = System.currentTimeMillis();

    try {
        test.run();
        long time = System.currentTimeMillis() - start;
        IO.println("[PASS] " + name + " completed in " + time + "ms");
    } catch (Exception e) {
        System.err.println("!!! [FAIL] " + name + " FAILED !!!");
    }
    IO.println("-----------------------------------------");
}