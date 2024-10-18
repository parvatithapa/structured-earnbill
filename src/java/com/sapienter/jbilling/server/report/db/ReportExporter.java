package com.sapienter.jbilling.server.report.db;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.ArrayUtils;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

public class ReportExporter {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    public void readAndExportDataToFile(String sql, String directory, String splittedFilesDirectory, String fileName, Integer filesSplitLimit) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            CopyManager copyManager = new CopyManager(connection.unwrap(BaseConnection.class));
            String fullPath = directory.concat("/").concat(fileName);
            try(FileOutputStream fileOutputStream = new FileOutputStream(fullPath)) {
                copyManager.copyOut("COPY (" + sql + ") TO STDOUT WITH (FORMAT CSV, HEADER)", fileOutputStream);
                splitFile(splittedFilesDirectory, new File(fullPath), filesSplitLimit);
            }
        } catch (SQLException | IOException e) {
            throw new SessionInternalError("exception occurred while reading or exporting the report to the file!", e);
        }
    }
    private void splitFile(String splittedFilesDirectory, File reportFile, Integer numberOfLines) throws IOException {
        if(Util.isUnix()) {
            logger.debug("splitting file {} using linux", reportFile.getName());
            splitFileByLinux(splittedFilesDirectory, reportFile, numberOfLines);
        } else {
            logger.debug("splitting file {} using java", reportFile.getName());
            splitFileByJava(splittedFilesDirectory, reportFile, numberOfLines);
        }
    }
    private void splitFileByLinux(String splittedFilesDirectory, File reportFile, Integer numberOfLines) {
        File directory = new File(splittedFilesDirectory);
        String[] command = new String[] {
                "split",
                "-l",
                numberOfLines.toString(),
                reportFile.getAbsolutePath(),
                reportFile.getName().replace('.', '_') + '_'
        };
        Util.executeCommand(command, directory);
        File[] partitionedFiles = directory.listFiles();
        if(ArrayUtils.isEmpty(partitionedFiles)) {
            logger.error("splitting of file failed");
        }
    }
    private List<String> splitFileByJava(String splittedFilesDirectory, File reportFile, Integer numberOfLines) throws IOException {
        LineIterator lineIterator = null;
        try {
            File directory = new File(splittedFilesDirectory);
            lineIterator = FileUtils.lineIterator(reportFile);
            int currentLineCount = 0;
            int partitionNum = 0;
            File partitionedFile = null;
            List<String> fileNames = new ArrayList<>();
            while(lineIterator.hasNext()) {
                if(currentLineCount == 0 || currentLineCount % numberOfLines == 0) {
                    // create new File
                    partitionedFile = createFile(directory, reportFile.getName(), ++partitionNum);
                    fileNames.add(partitionedFile.getAbsolutePath());

                }
                currentLineCount++;
                String line = lineIterator.next().concat(System.lineSeparator());
                Files.write(partitionedFile.toPath(), line.getBytes(), StandardOpenOption.APPEND);
            }
            return fileNames;
        } catch(IOException ex) {
            throw new SessionInternalError(ex.getMessage(), ex);
        } finally {
            if(null!= lineIterator) {
                lineIterator.close();
            }

        }
    }
    private File createFile(File directory, String fileName, Integer partitionNumber) throws IOException {
        File file = new File(directory, FilenameUtils.removeExtension(fileName)+ "-" + partitionNumber);
        if(!file.createNewFile()) {
            throw new SessionInternalError("file creation failed!");
        }
        return file;
    }
}
