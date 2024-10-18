package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.common.Util.ExecutionResult;

class SPCFileSplitter implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Integer LINES_TO_WRITE_INTO_FILE = 20000;
    static final String PARTITIONED_DIRECTORY_PARAM_KEY = "partitionedDirectoryPath";
    static final String FILE_TO_READ_PARAM_KEY = "fileToRead";

    @Value("#{jobParameters['filePath']}")
    private String resource;
    @Value("#{jobExecution}")
    private JobExecution jobExecution;
    @Autowired
    private JobRepository jobRepository;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> result = new HashMap<>();
        int partitionNumber = 0;
        int linesPerFile = LINES_TO_WRITE_INTO_FILE;
        List<String> files;
        if(!resource.contains(MediationServiceType.AAPT_INTERNET_USAGE.getFileNamePrefix())) {
            if(Util.isUnix()) {
                String fileName = FilenameUtils.getName(resource);
                // read line count from file.
                ExecutionResult executionResult = Util.executeCommand(new String[] {"wc", "-l", resource}, null);
                logger.debug("execution result {} for file {}", executionResult, fileName);
                int lines = Integer.parseInt(executionResult.getOutput()
                        .replaceAll(resource, StringUtils.EMPTY)
                        .replaceAll("[^0-9]", StringUtils.EMPTY));
                logger.debug("file {} has {} lines", fileName, lines);
                int size = lines - 1;
                linesPerFile = size / gridSize + 1;
                logger.debug("creating partitioned file with {} lines for cdr file {}", linesPerFile, fileName);
            }
            try {
                files = splitFile(Paths.get(resource).toFile(), linesPerFile);
            } catch (IOException e) {
                logger.atError().setCause(e.getCause()).log("Error while splitting file {}", resource);
                throw new SessionInternalError("file creation failed!");
            }
        } else {
            logger.debug("no partition required for aapt file");
            files = Collections.singletonList(resource);
        }
        for(String file : files) {
            String partitionName = "fileToRead-"+(++ partitionNumber);
            Map<String, Object> params = Collections.singletonMap(FILE_TO_READ_PARAM_KEY, file);
            result.put(partitionName, new ExecutionContext(params));
            logger.debug("partition {} created with {} params", partitionName, params);
        }
        logger.debug("file {} partitioned into {}", resource, files);
        return result;
    }

    /**
     * Creates file for given directory with name.
     * @param directory
     * @param fileName
     * @param partitionNumber
     * @return
     * @throws IOException
     */
    private File createFile(File directory, String fileName, Integer partitionNumber) throws IOException {
        File file = new File(directory, FilenameUtils.removeExtension(fileName)+ "-" + partitionNumber);
        if(!file.createNewFile()) {
            throw new SessionInternalError("file creation failed!");
        }
        return file;
    }

    /**
     * Creates new directory from given file.
     * @param sourceFile
     * @return
     */
    private File createDirectory(File sourceFile) {
        File directory = new File(sourceFile.getAbsoluteFile().getParent() +
                File.separator + "partitioned-files-"+System.currentTimeMillis());
        try {
            FileUtils.forceMkdir(directory);
            // update partitioned directory path in job execution.
            jobExecution.getExecutionContext().put(PARTITIONED_DIRECTORY_PARAM_KEY, directory.getAbsolutePath());
            jobRepository.updateExecutionContext(jobExecution);
            logger.debug("directory {} saved", directory.getAbsolutePath());
            return directory;
        } catch (IOException ex) {
            throw new SessionInternalError("directory creation failed!", ex);
        } catch (Exception ex) {
            throw new SessionInternalError("jobExecution context update failed!", ex);
        }
    }

    /**
     * Splits one file into multiple files based on given numberOfLines using java.
     * @param source
     * @param numberOfLines
     * @return
     */
    private List<String> splitFileByJava(File source, int numberOfLines) throws IOException {
        LineIterator lineIterator = null;
        try {
            File directory = createDirectory(source);
            lineIterator = FileUtils.lineIterator(source);
            int currentLineCount = 0;
            int partitionNum = 0;
            File partitionedFile = null;
            List<String> fileNames = new ArrayList<>();
            while(lineIterator.hasNext()) {
                if(currentLineCount == 0 || currentLineCount % numberOfLines == 0) {
                    // create new File
                    partitionedFile = createFile(directory, source.getName(), ++partitionNum);
                    fileNames.add(partitionedFile.getAbsolutePath());

                }
                currentLineCount++;
                String line = lineIterator.next().concat(System.lineSeparator());
                Files.write(partitionedFile.toPath(), line.getBytes(), StandardOpenOption.APPEND);
            }
            return fileNames;
        } catch(IOException ex) {
            throw new SessionInternalError("partition file creation failed!", ex);
        } finally {
            if(null!= lineIterator) {
                lineIterator.close();
            }

        }
    }

    private List<String> splitFile(File source, int numberOfLines) throws IOException {
        if(Util.isUnix()) {
            logger.debug("spliting file {} using linux", source.getName());
            return splitFileByLinux(source, numberOfLines);
        }
        logger.debug("spliting file {} using java", source.getName());
        return splitFileByJava(source, numberOfLines);
    }

    /**
     * Splits files for given numberOfLines by linux os.
     * @param cdrFile
     * @param numberOfLines
     * @return
     */
    private List<String> splitFileByLinux(File cdrFile, Integer numberOfLines) {
        File directory = createDirectory(cdrFile);
        String[] command = new String[] {
                "split",
                "-l",
                numberOfLines.toString(),
                cdrFile.getAbsolutePath(),
                cdrFile.getName().replace('.', '_') + '_'
        };
        logger.debug("executing command {} with working directory {}", command, directory.getAbsolutePath());
        Util.executeCommand(command, directory);
        File[] partitionedFiles = directory.listFiles();
        if(ArrayUtils.isEmpty(partitionedFiles)) {
            throw new SessionInternalError("partitioning of file failed");
        }
        return Arrays.stream(partitionedFiles)
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());
    }
}
