package com.sapienter.jbilling.server.spc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.util.Context;

public class SPCMediationReconciliationFileUtils {
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private int bufferSize;
    private int retryCount;
    SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);

    public SPCMediationReconciliationFileUtils() {
    }

    public SPCMediationReconciliationFileUtils(int bufferSize, int retryCount) {
        this.bufferSize = bufferSize;
        this.retryCount = retryCount;
    }

    public void unzipFile(String zipFilePath, String destDirectory, Map<String, File> fileMap, Map<String, String> fileProcessedMap)
            throws IOException {
        File zipFile = new File(zipFilePath);
        String zipFileName = StringUtils.EMPTY;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            byte[] buffer = new byte[bufferSize];
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                zipFileName = zipEntry.getName();
                String filePath = destDirectory + File.separator + zipEntry.getName();
                if (!zipEntry.isDirectory()) {
                    File parent = new File(destDirectory);
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                fileMap.put(zipFileName, zipFile);
                zipEntry = zis.getNextEntry();
            }
        } catch (IOException e) {
            String errorFileName = StringUtils.isNotBlank(zipFileName) ? zipFileName : zipFile.getName();
            fileProcessedMap.put(errorFileName, e.getMessage());
            logger.error("Error while decompressing ZIP file {},{}", zipFilePath, e.getMessage());
        }
    }

    public String extractGzipFile(File sourceFile, String destDirectory) throws IOException {
        try (FileInputStream fis = new FileInputStream(sourceFile);
                GZIPInputStream gzis = new GZIPInputStream(fis);
                FileOutputStream fos = new FileOutputStream(destDirectory)) {
            byte[] buffer = new byte[bufferSize];
            int length;

            while ((length = gzis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            logger.error("Error while decompressing GZIP file {},{}", sourceFile, e.getMessage());
            return e.getMessage();
        }
        return StringUtils.EMPTY;
    }

    public Map<String, String> extractTarFile(File sourceFile, String destDirectory, Map<String, File> parentFileMap,
            Map<String, String> fileProcessedMap) throws IOException {

        try (TarArchiveInputStream fin = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(
                sourceFile.getAbsolutePath())))) {
            TarArchiveEntry entry;
            while ((entry = fin.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                File curfile = new File(sourceFile.getParent(), entry.getName());
                if (curfile.getName().contains("Reseller42_UDR_")) {
                    File parent = curfile.getParentFile();
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try (FileOutputStream fis = new FileOutputStream(curfile)) {
                        IOUtils.copy(fin, fis);
                    }
                    String fileNameWithoutExt = getFileNameWithoutExtension(curfile.getName());
                    String fileStatus = extractGzipFile(curfile, destDirectory + File.separator + fileNameWithoutExt);

                    if (StringUtils.isBlank(fileStatus)) {
                        parentFileMap.put(fileNameWithoutExt, sourceFile);
                    } else {
                        fileProcessedMap.put(fileNameWithoutExt, fileStatus);
                    }

                    Files.delete(Paths.get(curfile.getAbsolutePath()));
                }

            }
        } catch (IOException e) {
            fileProcessedMap.put(sourceFile.getName(), e.getMessage());
            logger.error("Error while decompressing ZIP file {},{}", sourceFile, e.getMessage());
        }
        return fileProcessedMap;
    }

    public Map<String, String> extractAllTelstraMobileFiles(String sourcePath, String destDirectory, Map<String, File> fileMap)
            throws IOException {
        File root = new File(sourcePath);
        File dest = new File(destDirectory);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File[] list = root.listFiles();

        Map<String, String> fileProcessedMap = new HashMap<>();
        for (File file : list) {
            if (!file.isDirectory()) {
                if (isVerified(file.getName())) {
                    extractTarFile(file, dest.getAbsolutePath(), fileMap, fileProcessedMap);
                }
            }
        }
        return fileProcessedMap;
    }

    public Map<String, String> extractAllOptusMurFiles(String sourcePath, String destDirectory, Map<String, File> fileMap)
            throws IOException {
        File root = new File(sourcePath);
        File dest = new File(destDirectory);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File[] list = root.listFiles();

        Map<String, String> fileProcessedMap = new HashMap<>();
        for (File f : list) {
            if (!f.isDirectory()) {
                if (isVerified(f.getName())) {
                    String fileName = getFileNameWithoutExtension(f.getName());
                    String fileStatus = extractGzipFile(f, dest.getAbsolutePath() + File.separator + fileName);
                    if (StringUtils.isBlank(fileStatus)) {
                        fileMap.put(fileName, f);
                    } else {
                        fileProcessedMap.put(fileName, fileStatus);
                    }
                }
            }
        }
        return fileProcessedMap;
    }

    public Map<String, String> extractAllOptusMobileFiles(String sourcePath, String destDirectory, Map<String, File> fileMap)
            throws IOException {
        File root = new File(sourcePath);
        File dest = new File(destDirectory);
        if (!dest.exists()) {
            dest.mkdirs();
        }
        File[] list = root.listFiles();

        Map<String, String> fileProcessedMap = new HashMap<>();
        for (File f : list) {
            if (!f.isDirectory()) {
                if (isVerified(f.getName())) {
                    unzipFile(f.getAbsolutePath(), dest.getAbsolutePath(), fileMap, fileProcessedMap);
                }
            }
        }
        return fileProcessedMap;
    }

    public static void deleteDirectoriesFromDir(String sourcePath) throws IOException {

        File root = new File(sourcePath);
        File[] list = root.listFiles();
        if (list == null)
            return;

        for (File f : list) {
            if (f.isDirectory()) {
                FileUtils.deleteDirectory(f);
            }
        }
    }

    public void getAllFiles(List<String> filePathList, String path) {

        File root = new File(path);
        File[] list = root.listFiles();
        if (list == null)
            return;

        for (File f : list) {
            if (!f.isDirectory()) {
                filePathList.add(f.getAbsolutePath());
            }
        }
    }

    public void copyFiles(Set<File> sourceFiles, String destinationFile) {

        if (CollectionUtils.isNotEmpty(sourceFiles)) {
            for (File sourceFile : sourceFiles) {
                Path source = Paths.get(sourceFile.getAbsolutePath());
                Path destination = Paths.get(destinationFile + source.getFileName());

                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    logger.error("Error while moving file {} to {},{}", sourceFile, destinationFile, e.getMessage());
                }
            }
        }
    }

    public List<String> getRecordsFromFile(String fileName) {
        List<String> list = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            while ((line = br.readLine()) != null) {
                list.add(line);
            }
        } catch (FileNotFoundException e) {
            logger.error("File with name {} not found {}", fileName, e.getMessage());
        } catch (IOException e) {
            logger.error("Error reading file {},{}", fileName, e.getMessage());
        }
        return list;
    }

    public List<String[]> getExcludedRecordsSetFromFile(String fileName) {
        List<String[]> list = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            while ((line = br.readLine()) != null) {
                list.add(line.split(","));
            }
        } catch (FileNotFoundException e) {
            logger.error("File with name {} not found {}", fileName, e.getMessage());
        } catch (IOException e) {
            logger.error("Error reading file {},{}", fileName, e.getMessage());
        }
        return list;
    }

    public static String getFileNameWithoutExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    public static Set<File> getFilesToCopy(Set<File> fileSet, Map<String, File> filesMap) {
        Set<File> files = new HashSet<>();
        for (File f : fileSet) {
            String fileName = f.getName();
            if (filesMap.containsKey(fileName)) {
                files.add(filesMap.get(fileName));
            }
        }
        return files;
    }

    /**
     * @param recordMap
     * @param currentDate
     * @return
     * @throws IOException
     */
    public static File writeCsvFile(Map<File, MediationReconciliationRecord> recordMap, String currentDate, String path) throws IOException {
        File file = new File(path + "mediation-reconciliation-report-" + currentDate + ".csv");

        try (FileWriter fileWriter = new FileWriter(file.getAbsolutePath());) {

            fileWriter.append(MediationReconciliationRecord.getHeaders());
            fileWriter.append("\n");

            for (MediationReconciliationRecord reconciliationRecord : recordMap.values()) {
                if (reconciliationRecord.getDifference() != 0) {
                    fileWriter.append(reconciliationRecord.toString());
                    fileWriter.append("\n");
                }
            }

            fileWriter.flush();
        }
        return file;
    }

    public Set<MediationReconciliationHistory> getReconciledFilesMap(Map<File, MediationReconciliationRecord> recordMap,
            Map<String, File> processedFiles) {
        Set<MediationReconciliationHistory> historySet = new HashSet<>();
        for (MediationReconciliationRecord reconcliliationRecord : recordMap.values()) {
            String fileName = reconcliliationRecord.getFileName();
            if (processedFiles.containsKey(fileName)) {
                MediationReconciliationHistory reconciliationHistory = new MediationReconciliationHistory(processedFiles.get(fileName)
                        .getParentFile().getName(), processedFiles.get(fileName).getName(), fileName,
                        reconcliliationRecord.getDifference() == 0 , 0);
                historySet.add(reconciliationHistory);
            }
        }
        return historySet;
    }

    private boolean isVerified(String fileName) {
        MediationReconciliationHistory reconciliationHistory = spcHelperService.getMediationReconciliationHistory(fileName);
        return null == reconciliationHistory || (!reconciliationHistory.isVerified() && reconciliationHistory.getRetryCount() < retryCount);
    }
}
