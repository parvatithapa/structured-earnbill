package com.sapienter.jbilling.api.automation.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vojislav Stanojevikj
 * @since 17-Jun-2016.
 */
public final class FileHelper {

    private FileHelper() {
    }

    private static final Logger logger = LoggerFactory.getLogger(FileHelper.class);

    public static void write(String filePath, String content) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            // adds content to file
            fileWriter.append(content);

            logger.debug("file wrote {}", filePath);
        } catch (IOException e) {
            logger.error("Exception thrown during write!", e);
        }
    }

    public static void write(String filePath, String... lines) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            for (String line : lines) {
                fileWriter.append(line).append("\n");
            }

            logger.debug("file wrote " + filePath);

        } catch (IOException e) {
            logger.error("Exception thrown during write!", e);
        }
    }

    public static List<String> readLines(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            reader.lines().forEach(lines::add);
        } catch (IOException e) {
            logger.error("Exception thrown during read!", e);
        }
        return lines;
    }

    public static void deleteFile(String filePath) {

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            String path = file.getPath();
            boolean delete = file.delete();

            logger.debug("File {} delete {}!", path, delete ? "success" : "failed");
        }
    }
}
