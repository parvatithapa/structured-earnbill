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

import com.jcraft.jsch.ChannelSftp;
import org.springframework.core.io.FileSystemResource;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.ftp.session.AbstractFtpSessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by marcomanzi on 1/28/14.
 */
public class SftpFileRetriever extends AbstractRemoteFileRetriever<DefaultSftpSessionFactory> {

    private String privateKey;
    private String privateKeyPassphrase;

    @Override
    public Map<String, Long> getFileNamesFromRemoteDir(Session session) throws IOException {
        Map<String, Long> fileNamesFromRemoteDir = new HashMap<String, Long>();
        ChannelSftp.LsEntry[] lsEntries = (ChannelSftp.LsEntry[]) session.list(getRemotePath());
        for (ChannelSftp.LsEntry lsEntry: lsEntries) {
            if (!(lsEntry.getFilename().equals("..") ||
                  lsEntry.getFilename().equals(".")  ||
                  lsEntry.getAttrs().isDir())) {
                String fileName = lsEntry.getFilename();
                Pattern fileNamePattern = getFileNamePattern();
                if (null != fileNamePattern) {
                    Matcher matcher = fileNamePattern.matcher(fileName);
                    if (matcher.matches()) {
                        fileNamesFromRemoteDir.put(fileName, lsEntry.getAttrs().getSize());
                    }
                } else {
                    fileNamesFromRemoteDir.put(fileName, lsEntry.getAttrs().getSize());
                }
            }

        }
        return fileNamesFromRemoteDir;
    }

    public boolean copyToLocalFileSystemFrom(DefaultSftpSessionFactory sessionFactory) {
        setSessionFactory(sessionFactory);
        return copyToLocalFileSystem();
    }

    @Override
    public boolean copyToLocalFileSystemFrom(String username, String keyFile, String keyFilePassphrase, String host, int port) {
        DefaultSftpSessionFactory sftpSessionFactory = (DefaultSftpSessionFactory)getSessionFactory();

        sftpSessionFactory.setHost(host);
        sftpSessionFactory.setPort(port);
        sftpSessionFactory.setUser(username);
        sftpSessionFactory.setAllowUnknownKeys(true);
        if(keyFile != null && keyFile.length() > 0) {
            sftpSessionFactory.setPrivateKey(new FileSystemResource(keyFile));
        }
        if(keyFilePassphrase != null && keyFilePassphrase.length() > 0) {
            sftpSessionFactory.setPrivateKeyPassphrase(keyFilePassphrase);
        }
        return copyToLocalFileSystem();
    }

    public boolean copyToLocalFileSystemFrom(String username, String password, String host, int port) {
        DefaultSftpSessionFactory sftpSessionFactory = (DefaultSftpSessionFactory)getSessionFactory();

        sftpSessionFactory.setHost(host);
        sftpSessionFactory.setPort(port);
        sftpSessionFactory.setUser(username);
        sftpSessionFactory.setAllowUnknownKeys(true);
        if(password != null && password.length() > 0) {
            sftpSessionFactory.setPassword(password);
        }
        return copyToLocalFileSystem();
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getPrivateKeyPassphrase() {
        return privateKeyPassphrase;
    }

    public void setPrivateKeyPassphrase(String privateKeyPassphrase) {
        this.privateKeyPassphrase = privateKeyPassphrase;
    }
}