package com.sapienter.jbilling.server.deutscheTelecom;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wajeeha on 05/11/18.
 */
public class CSVFileHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void write(String filePath, String content)
    {
        FileWriter fileWriter= null;
        try
        {
            // creates file writer obj
            fileWriter = new FileWriter(filePath);

            // adds content to file
            fileWriter.append(content);

            logger.info("file wrote {}",filePath);

        }
        catch (IOException exception)
        {
            logger.debug("Exception occurred while writing file :{}",exception);
            exception.printStackTrace();
        }
        finally
        {
            try {
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<String> readLines(String filePath){
        BufferedReader reader = null;
        List<String> lines = new ArrayList<>();
        try {
            reader = new BufferedReader(new FileReader(filePath));
            reader.lines().forEach(lines::add);
        } catch (IOException exception){
            logger.debug("Exception:"+exception);
            exception.printStackTrace();
        } finally {
            if (null != reader){
                try {
                    reader.close();
                } catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return lines;
    }
}
