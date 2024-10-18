/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.sourcereader;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.FtpSession;

import java.io.*;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Created by marcomanzi on 1/28/14.
 */
public abstract class AbstractRemoteFileRetriever<T extends SessionFactory> implements RemoteFileRetriever<T> {

    private FormatLogger LOG = new FormatLogger(Logger.getLogger(this.getClass()));
    private String remotePath;
    private String remoteMoveToPath;//if this is set then after download remote file is moved into this folder
    private SessionFactory sessionFactory;
    private String localPath;
    private String suffix;
    private String tempFolder;
    private String fileNameRegex;
    private Pattern fileNamePattern;
    private boolean recursive;
    private boolean uncompress;
    private boolean deleteRemote;
    private boolean overwrite;

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public String getRemoteMoveToPath() {
        return remoteMoveToPath;
    }

    public void setRemoteMoveToPath(String remoteMoveToPath) {
        this.remoteMoveToPath = remoteMoveToPath;
    }

    protected boolean moveRemoteFiles() {
        return remoteMoveToPath != null && remoteMoveToPath.trim().length() > 0;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public boolean isUncompress() {
        return uncompress;
    }

    public void setUncompress(boolean uncompress) {
        this.uncompress = uncompress;
    }

    @Override
    public boolean copyToLocalFileSystemFrom(String username, String keyFile, String keyFilePassphrase, String server, int port) {
        return false;
    }

    /**
     * Copy the files from the remote directory path set, to the local path set.
     * Replace the suffix of the file with the suffix set (or add it if files has no suffix)
     * @return true if no exception occurred during the copy
     */
    public boolean copyToLocalFileSystem() {
        boolean success = true;
        //first validate that a pattern can and was built from the defined regex string
        if (getFileNameRegex() != null
                && !getFileNameRegex().trim().isEmpty()
                && null == getFileNamePattern()) {
            LOG.error("File name regex is defined but pattern was not build. Bad Regex.");
            return false;
        }
        //Check whether temp folder exist. If not then create it.
        if (!createTempDirectory()) {
            LOG.error("Unable to create Temp Folder at local Machine.");
            return false;
        }

        Session session = sessionFactory.getSession();
        try {
            success = copyToLocal(session);
        } finally {
            session.close();
        }
        return success;
    }

    /**
     * Checks if there is a remote move to folder defined. If there is no
     * such folder defined then no action is performed. If there is such
     * folder defined then we will check if folder exists, if the folder
     * exists then we return true if the folder does not exists then this
     * method tries to create that folder.
     */
    protected boolean checkRemoteMoveToPath(Session session, String remoteMoveToPath) {
        try {
            if (null != remoteMoveToPath && !remoteMoveToPath.isEmpty()) {
                if (!session.exists(remoteMoveToPath)) {
                    LOG.info("Remote Move To Folder Does Not Exist. Trying to Create. Folder: " + remoteMoveToPath);
                    session.mkdir(remoteMoveToPath);
                } else {
                    LOG.debug("Remote Move To Folder Exists. Remote Folder:" + remoteMoveToPath);
                }
            }
        } catch (IOException e) {
            LOG.error("Problem in creation of  \"Remote Move To Path folder\", exception:", e);
            return false;
        }
        return true;
    }

    /**
     * Does the actual copy (download) from remote folder to a local folder.
     */
    private boolean copyToLocal(Session session) {
        try {
            Map<String, Long> fileNamesSize = getFileNamesFromRemoteDir(session);
            LOG.info("Downloading %s files", fileNamesSize.size());
            for (String fileName : fileNamesSize.keySet()) {
                boolean copied = writeFileToFileSystem(fileName, fileNamesSize.get(fileName), session);
                if (copied) {
                    if (moveRemoteFiles()) {
                        //this action will move the previously downloaded file
                        //into a remote folder that keeps downloaded files
                        session.rename(getRemotePath() + fileName, getAbsolutePathForDownloadedFile(fileName));
                    } else if(isDeleteRemote()) {
                        session.remove(getRemotePath() + fileName);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Problem while copying files from remote, exception:", e);
            return false;
        }
        return true;
    }

    private String getAbsolutePathForDownloadedFile(String fileName) {
        if (fileName.contains(File.separator) && recursive) {
            int lastFileSeparatorIndex = fileName.lastIndexOf(File.separator) + 1;
            String relativePath = fileName.substring(0, lastFileSeparatorIndex);
            String actualFileName = fileName.substring(lastFileSeparatorIndex);
            return getRemotePath() + relativePath + getRemoteMoveToPath() + File.separator + actualFileName;
        } else {
            return getRemotePath() + getRemoteMoveToPath() + File.separator + fileName;
        }
    }

    private boolean writeFileToFileSystem(String fileName, Long fileSize, Session session) throws IOException {
        File tempFile = new File(evaluateTemporaryPath(fileName));
        File finalFile = new File(evaluateNewFilePath(fileName));

        if(overwrite && finalFile.exists()) {
            finalFile.delete();
        }

        if (!finalFile.exists()) {
            String remoteFile = getRemotePath() + fileName;
            LOG.info("Copying remote file: %s, to a temp file: %s", remoteFile, tempFile);
            tempFile.createNewFile();
            FileOutputStream output = new FileOutputStream(tempFile);
            session.read(remoteFile, output);
            output.close();
            if (isUncompress() && isZipFile(tempFile)) {
                moveZipFileToLocal(tempFile);
            } else {
                LOG.info("Rename temp file: %s, to a file: %s", tempFile, finalFile);
                tempFile.renameTo(finalFile);
            }
            return true;
        } else if (finalFile.length() != fileSize) {
            LOG.info("File:" + fileName + " not copied on the filesystem. Exist already a file with the same name but different size");
            return false;
        } else {
            LOG.info("File:" + fileName + " looks like it is already downloaded");
            return true;
        }
    }

    private void moveZipFileToLocal(File tempFile) throws IOException{
        //Unzip Contents in sub folder : unzip-time.
        String unZipContentPath = localPath;
        unZipContentPath += tempFolder + File.separator + "unzip-" + System.currentTimeMillis() + File.separator;
        LOG.info("File is an archive : %s, unzip at subfolder: %s", tempFile, unZipContentPath);
        try {
            // Creating sub folder where archive will unzip.
            File unZipContentSubFolder = new File(unZipContentPath);
            unZipContentSubFolder.mkdirs();
            extractZipFile(tempFile, unZipContentPath);
            LOG.info("Extraction of archive file is completed : %s", tempFile);
        } finally {
            LOG.info("Deleting the zip file from temp folder : %s", tempFile);
            if (tempFile.exists()) {
                FileUtils.forceDelete(tempFile);
            }
        }
    }

    private boolean isZipFile(File file) throws IOException {
        if (file.isDirectory()) {
            return false;
        }
        if (file.length() < 4) {
            return false;
        }
        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        int test = in.readInt();
        in.close();
        return test == 0x504b0304;
    }

    public void extractZipFile(File tempFile, String outputFolder) throws IOException {
        ZipFile zipFile = new ZipFile(tempFile);
        Enumeration<ZipEntry> enumeration = (Enumeration<ZipEntry>) zipFile.entries();
        boolean status = true;
        try {
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                InputStream inputStream = zipFile.getInputStream(entry);
                if (entry.isDirectory()) {
                    new File(outputFolder + entry.getName()).mkdirs();
                } else {
                    File newFile = new File(outputFolder + entry.getName());
                    FileUtils.copyInputStreamToFile(inputStream, newFile);
                    LOG.info("File is extracted from archive : %s. Ready to copy at local folder.", newFile);
                    status = writeExtractedFileToFileSystem(newFile, newFile.length()) && status ? true : false;
                }
            }
        } finally {
            // Delete the temp's subfolder where archive was unzip.
            File unZipContentSubFolder = new File(outputFolder);
            if (unZipContentSubFolder.exists() && status) {
                LOG.info("Deleting the temp sub Folder where archive was unzip.");
                FileUtils.deleteDirectory(unZipContentSubFolder);
            } else {
                LOG.info("Note : Temp sub Folder %s contain files which are not copied to local system.", outputFolder);
            }
        }
    }

    private boolean writeExtractedFileToFileSystem(File file, Long fileSize) throws IOException {

        File finalFile = new File(evaluateNewFilePath(file.getAbsolutePath()));
        if (!finalFile.exists()) {
            LOG.info("Rename temp file: %s, to a file: %s", file, finalFile);
            file.renameTo(finalFile);
            return true;
        } else if (finalFile.length() != fileSize) {
            LOG.info("File:" + file + " not copied on the filesystem. Exist already a file with the same name but different size");
            return false;
        } else {
            LOG.info("File:" + file + " looks like it is already downloaded");
            return false;
        }
    }


    private boolean createTempDirectory() {
        File temp = tempDirectory();
        if (temp != null) {
            if (!temp.exists()) {
                return temp.mkdirs();
            } else {
                return true;
            }
        }
        return false;
    }

    private String evaluateTemporaryPath(String fileName) {
        String tempFilePath = localPath;
        tempFilePath += tempFolder + File.separator;
        String actualFileName = getFileName(fileName);
        return tempFilePath + actualFileName + ".temp";
    }

    private File tempDirectory() {
        if (tempFolder != null && !tempFolder.isEmpty()) {
            String tempFilePath = localPath;
            tempFilePath += tempFolder + File.separator;
            return new File(tempFilePath);
        }
        return null;
    }

    private String evaluateNewFilePath(String fileName) {
        String actualFileName = getFileName(fileName);
        String newFilePath = localPath + actualFileName;
        if (suffix != null && !suffix.isEmpty()) {
            newFilePath += "." + suffix;
        }
        return newFilePath;
    }

    private String getFileName(String path) {
        File file = new File(path);
        return file.getName();
    }

    public String getTempFolder() {
        return tempFolder;
    }

    public void setTempFolder(String tempFolder) {
        this.tempFolder = tempFolder;
    }

    public String getFileNameRegex() {
        return fileNameRegex;
    }

    public Pattern getFileNamePattern() {
        return fileNamePattern;
    }

    public boolean isDeleteRemote() {
        return deleteRemote;
    }

    public void setDeleteRemote(boolean deleteRemote) {
        this.deleteRemote = deleteRemote;
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public void setFileNameRegex(String fileNameRegex) {
        if (null != fileNameRegex && !fileNameRegex.isEmpty()) {
            this.fileNameRegex = fileNameRegex;
            try {
                this.fileNamePattern = Pattern.compile(this.fileNameRegex);
            } catch (PatternSyntaxException pse) {
                this.fileNamePattern = null;
                LOG.error("Defined file name regex has errors. Message: %s, for pattern: %s",
                        pse.getMessage(), pse.getPattern());
            }
        }
    }

    protected abstract Map<String, Long> getFileNamesFromRemoteDir(Session session) throws IOException;
}