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
import org.apache.log4j.Logger;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FtpFileUploader extends AbstractRemoteFileUploader<DefaultFtpSessionFactory> {

    private FormatLogger LOG = new FormatLogger(Logger.getLogger(this.getClass()));

    private final String errorPrefix=".error";


    @Override
    public Map<String, Long> getFileNamesFromLocalDir() throws IOException {
        Map<String, Long> fileNamesFromLocalDir = new HashMap<String, Long>();
        getLocalFilesToCopy(fileNamesFromLocalDir, "");
        return fileNamesFromLocalDir;
    }



    protected void getLocalFilesToCopy(Map<String, Long> fileNamesFromLocalDir, String relativePath) throws IOException {

        File directory = new File(getLocalPath());
        File[] files=directory.listFiles();

        LOG.debug("Found %s files in local folder: %s", files.length,  getRemotePath() + relativePath);

        for (File file : files) {
            if(!file.getName().contains(errorPrefix)){
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
                    if (!(getLocalPath() + newPath).equals(getLocalPath() + relativePath + getRemoteMoveToPath() + File.separator)) {
                        getLocalFilesToCopy(fileNamesFromLocalDir, newPath);
                    }
                }
                if (validFile) {
                    fileNamesFromLocalDir.put(relativePath + file.getName(), file.length());
                }
            }
        }
    }

    public boolean copyToRemoteFileSystemFrom(DefaultFtpSessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
        return copyToRemoteFileSystem();
    }

    public boolean copyToRemoteFileSystemFrom(String username, String password, String host, int port) {
        ((AbstractFtpSessionFactory)getSessionFactory()).setHost(host);
        ((AbstractFtpSessionFactory)getSessionFactory()).setPort(port);
        ((AbstractFtpSessionFactory)getSessionFactory()).setUsername(username);
        ((AbstractFtpSessionFactory)getSessionFactory()).setPassword(password);
        return copyToRemoteFileSystem();
    }

}