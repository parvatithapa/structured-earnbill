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
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcomanzi on 1/28/14.
 */
public class FtpFileRetriever extends AbstractRemoteFileRetriever<DefaultFtpSessionFactory> {

    private FormatLogger LOG = new FormatLogger(Logger.getLogger(this.getClass()));

    @Override
    public Map<String, Long> getFileNamesFromRemoteDir(Session session) throws IOException {
        Map<String, Long> fileNamesFromRemoteDir = new HashMap<String, Long>();
        getFilesToCopy(session, fileNamesFromRemoteDir, "");
        return fileNamesFromRemoteDir;
    }

    /*
    * Check files and directory at the relativePath recursively.
    * Here relativePath represent the sub directory at remote relativePath where files going to be searched.
    * */
    protected void getFilesToCopy(Session session, Map<String, Long> fileNamesFromRemoteDir, String relativePath) throws IOException {
        FTPFile[] ftpFiles = (FTPFile[]) session.list(getRemotePath() + relativePath);
        LOG.debug("Found %s files in remote folder: %s", ftpFiles.length, getRemotePath() + relativePath);
        for (FTPFile file : ftpFiles) {
            boolean validFile = false;
            if (!file.isDirectory()) {
                String fileName = file.getName();
                Pattern fileNamePattern = getFileNamePattern();
                if (null != fileNamePattern) {
                    Matcher matcher = fileNamePattern.matcher(fileName);
                    if (matcher.matches()) {
                        validFile = true;
                    }
                } else {
                    validFile = true;
                }
            } else if (isRecursive()) {
                String newPath = relativePath + file.getName() + File.separator;
                //check sub directory is not equal to remote_move_to_path.
                if (!moveRemoteFiles() || !(getRemotePath() + newPath).equals(getRemotePath() + relativePath + getRemoteMoveToPath() + File.separator)) {
                    getFilesToCopy(session, fileNamesFromRemoteDir, newPath);
                }
            }
            if (validFile) {
                if (!moveRemoteFiles() || checkRemoteMoveToPath(session, getRemotePath().trim() + relativePath + getRemoteMoveToPath().trim())) {
                    fileNamesFromRemoteDir.put(relativePath + file.getName(), file.getSize());
                } else {
                    LOG.error("File Downloading aborted : \"" + file.getName() + "\".");
                }
            }
        }
    }

    public boolean copyToLocalFileSystemFrom(DefaultFtpSessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
        return copyToLocalFileSystem();
    }

    public boolean copyToLocalFileSystemFrom(String username, String password, String host, int port) {
        ((AbstractFtpSessionFactory)getSessionFactory()).setHost(host);
        ((AbstractFtpSessionFactory)getSessionFactory()).setPort(port);
        ((AbstractFtpSessionFactory)getSessionFactory()).setUsername(username);
        ((AbstractFtpSessionFactory)getSessionFactory()).setPassword(password);
        return copyToLocalFileSystem();
    }

}