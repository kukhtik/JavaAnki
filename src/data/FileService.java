package data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Низкоуровневых операций ввода-вывода (I/O) с файловой системой
 */
public abstract class FileService {
    private static final Logger LOGGER = Logger.getLogger(FileService.class.getName());

    /**
     * Читает все строки текстового файла в кодировке UTF-8
     *
     * @param path путь к файлу.
     * @return список строк файла. Если файл не существует или произошла ошибка чтения,
     *         возвращается пустой неизменяемый список ({@code Collections.emptyList()})
     */
    public List<String> readAllLines(Path path){
        if (!Files.exists(path)){
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка чтения файла: " + path, e);
            return Collections.emptyList();
        }
    }

    /**
     * Дописывает одну строку в конец указанного файла (UTF-8)
     * <p>
     * Использует опции:
     * <ul>
     *     <li>{@code CREATE} - создает файл, если его нет.</li>
     *     <li>{@code APPEND} - дописывает в конец, не затирая старые данные.</li>
     * </ul>
     * Добавляет системный разделитель строки ({@code System.lineSeparator()}) после текста
     * </p>
     *
     * @param path путь к целевому файлу
     * @param line строка для записи (без финального переноса строки)
     */
    public void appendLine(Path path, String line){
        try {
            Files.writeString(
                    path,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e){
            LOGGER.log(Level.SEVERE, "Ошибка записи в файл: " + path, e);
        }
    }

    /**
     * Гарантирует существование указанной директории
     * <p>
     * Проверяет наличие папки по указанному пути. Если её нет - пытается создать
     * (включая все несуществующие родительские директории, аналог {@code mkdir -p}).
     * </p>
     *
     * @param dir путь к директории
     */
    public void ensureDirectory(Path dir){
        if (!Files.exists(dir)){
            try {
                Files.createDirectories(dir);
            } catch (IOException e){
                LOGGER.log(Level.SEVERE, "Не удалось создать папку: " + dir, e);
            }
        }
    }
    public void overwrite(Path path, List<String> lines) {
        try {
            if (lines.isEmpty()) {
                Files.write(path, new byte[0], java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.CREATE);
            } else {
                Files.write(path, lines, java.nio.charset.StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (java.io.IOException e) {
            // Логирование
        }
    }
    // Переопределяем метод записи из ImportService для совместимости
    public abstract void write(Path path, Iterable<? extends CharSequence> lines, java.nio.file.OpenOption... options);
}