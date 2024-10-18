package com.sapienter.jbilling.server.ediFile.ldc;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.ediTransaction.IEDITransactionBean;
import com.sapienter.jbilling.server.ediTransaction.TransactionType;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by aman on 29/8/16.
 */

/**
 * This class deals with all files and folders which are exposed to client which are INBOUND, OUTBOUND and EXPORT.
 * These folders are always in parallel under a dir whose path is defined at company level meta field.
 * It displays files by type. Get any particular file. Delete it from folder(We do not have any backup of any these files)
 */
public class OrphanLDCFiles {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrphanLDCFiles.class));
    private static IEDITransactionBean ediTransactionBean;

    static {
        ediTransactionBean = Context.getBean(Context.Name.EDI_TRANSACTION_SESSION);
    }

    /**
     * Get path for dir and fetch files from file system based on type.
     *
     * @param entityId
     * @param type     INBOUND, OUTBOUND and EXPORT
     * @return List of files with name and creation date.
     */
    public static List<OrphanEDIFile> getOrphanEDIFiles(Integer entityId, TransactionType type) {
        String path = getPath(entityId, type);
        return getFiles(path);
    }

    private static String getPath(Integer entityId, TransactionType type) {
        String path = ediTransactionBean.getEDICommunicationPath(entityId, type);
        LOG.debug("Path to the folder : %s", path);
        return path;
    }

    private static List<OrphanEDIFile> getFiles(String folder) {
        Path path = Paths.get(folder);
        Stream<Path> files = null;
        try {
            files = Files.list(path);
        } catch (IOException ex) {
            LOG.debug(ex);
            return null;
        }
        return files.map(p -> getOrphanEDIFile(p))
                .filter(o -> o != null)
                .collect(Collectors.toList());
    }

    /**
     * @param entityId
     * @param type
     * @param fileName
     * @return File object for file with name @fileName
     */
    public static File getOrphanEDIFile(Integer entityId, TransactionType type, String fileName) {
        String path = getPath(entityId, type);
        File file = new File(path + File.separator + fileName);
        if (!file.exists()) {
            new SessionInternalError("No file exist with name : " + fileName);
        }
        return file;
    }

    private static OrphanEDIFile getOrphanEDIFile(Path p) {
        try {
            Object date = Files.getAttribute(p, "creationTime");
            if (date == null) return null;
            LocalDateTime localDateTime = LocalDateTime.ofInstant(((FileTime) date).toInstant(), ZoneId.systemDefault());
            String formattedDate = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").format(localDateTime);
            return new OrphanEDIFile(p.getFileName().toString(), formattedDate);
        } catch (IOException ex) {
            LOG.debug("Error while fetching file : %s", p.getFileName());
        }
        return null;
    }

    /**
     * This method will delete the files provided as List of names. Type will help to get the dir path
     *
     * @param entityId
     * @param type
     * @param fileNames
     */
    public static void delete(Integer entityId, TransactionType type, List<String> fileNames) {
        String path = getPath(entityId, type);
        fileNames.stream().forEach(fileName -> {
            File file = new File(path + File.separator + fileName);
            if (file.exists()) {
                if (!file.delete()) {
                    throw new SessionInternalError("Error while deleting file : " + fileName);
                }
            } else {
                throw new SessionInternalError("File  not exist: " + fileName);
            }
        });
    }
}


