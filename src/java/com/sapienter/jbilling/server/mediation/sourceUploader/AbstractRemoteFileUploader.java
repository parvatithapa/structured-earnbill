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

package com.sapienter.jbilling.server.mediation.sourceUploader;

import com.sapienter.jbilling.common.FormatLogger;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;

import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class AbstractRemoteFileUploader<T extends SessionFactory> implements RemoteFileUploader<T> {

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

    /**
     * Copy the files from the remote directory path set, to the local path set.
     * Replace the suffix of the file with the suffix set (or add it if files has no suffix)
     * @return true if no exception occurred during the copy
     */
    public boolean copyToRemoteFileSystem() {
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
            success = copyToRemote(session);
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

    private void renameFile(String fileName, String append){
        File oldFile=new File(getLocalPath()+fileName);
        File newFile=new File(getLocalPath()+fileName+append);
        oldFile.renameTo(newFile);
    }
    /**
     * Does the actual copy (download) from remote folder to a local folder.
     */
    private boolean copyToRemote(Session session) {
        try {
            Map<String, Long> fileNamesSize = getFileNamesFromLocalDir();

            LOG.info("Uploading %s files", fileNamesSize.size());
            for (String fileName : fileNamesSize.keySet()) {
                try{
                    boolean upload = writeFileToRemoteFileSystem(fileName, fileNamesSize.get(fileName), session);
                    if (!upload) {
                        renameFile(fileName, ".error");
                        return false;
                    }else{
                        File file=new File(getLocalPath()+fileName);
                        file.delete();
                    }
                }catch (Exception e){
                    LOG.error("Exception occur on uploading file");
                    renameFile(fileName, ".error");
                    return false;
                }
            }
        } catch (IOException e) {
            LOG.error("Problem while copying files to remote, exception:", e);
            return false;
        }
        return true;
    }

    private boolean writeFileToRemoteFileSystem(String fileName, Long fileSize, Session session) throws IOException {
        FTPFile[] ftpFiles = (FTPFile[]) session.list(getRemotePath());
        FTPFile existFile=null;
        String remoteCopyFileName=evaluateRemoteFilePath(fileName);

        for(FTPFile ftpFile:ftpFiles){
            if(ftpFile.getName().equals(remoteCopyFileName)){
                existFile=ftpFile;
                break ;
            }
        }
        if (existFile==null) {
            File copyFile = new File(getLocalPath()+fileName);
            FileInputStream inputStream = new FileInputStream(copyFile);
            String remoteCopyPath=getRemotePath()+fileName;

            LOG.info("Copying local file: %s, to a remote dir: %s", copyFile, getRemotePath());
            session.write(inputStream, remoteCopyPath);
            inputStream.close();
            return true;
        } else if (existFile.getSize() != fileSize) {
            LOG.info("File:" + fileName + " not copied on the remote filesystem. Exist already a file with the same name but different size");
            return false;
        } else {
            LOG.info("File:" + fileName + " looks like it is already Uploaded");
            return true;
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



    private File tempDirectory() {
        if (tempFolder != null && !tempFolder.isEmpty()) {
            String tempFilePath = localPath;
            tempFilePath += tempFolder + File.separator;
            return new File(tempFilePath);
        }
        return null;
    }

    private String evaluateRemoteFilePath(String fileName) {
        String actualFileName = getFileName(fileName);
        String newFilePath = remotePath + actualFileName;
        if (suffix != null && !suffix.isEmpty()) {
            newFilePath += "." + suffix;
        }
        return newFilePath;
    }

    private String getFileName(String path) {
        File file = new File(path);
        return file.getName();
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

    protected abstract Map<String, Long> getFileNamesFromLocalDir() throws IOException;
}