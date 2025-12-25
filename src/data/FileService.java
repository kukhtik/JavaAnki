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
 * Операции ввода вывода
 */
public class FileService {
    private static final Logger LOGGER = Logger.getLogger(FileService.class.getName());
    /**
     * Читает все строки файла в кодировке UTF-8.
     * Если файл не существует или произошла ошибка чтения,
     * метод возвращает пустой список и ошибку в лог
     */
    public List<String> readAllLines(Path path){
        if (!Files.exists(path)){
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Ошибка чтения файла: " + path,e);
            return Collections.emptyList();
        }
    }
    /**
     * Дописывает строку в конец файла в кодировке UTF-8.
     * Если файл отсутствует, то будет создан автоматически
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
            LOGGER.log(Level.SEVERE, "Ошибка записи в файл: " + path,e);
        }
    }
    /**
     * Гарантирует существование директории.
     * Если директория отсутствует, она будет создана вместе со всеми вложенными путями
     */
    public void ensureDirectory(Path dir){
        if (!Files.exists(dir)){
            try {
                Files.createDirectories(dir);
            } catch (IOException e){
                LOGGER.log(Level.SEVERE, "Не удалось создать папку: "+dir,e);
            }
        }
    }
}
