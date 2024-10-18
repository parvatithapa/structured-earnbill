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

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

/**
 * Created by marcomanzi on 1/28/14.
 */
public interface RemoteFileRetriever<T extends SessionFactory> {
    boolean copyToLocalFileSystem();
    boolean copyToLocalFileSystemFrom(T sessionFactory);
    boolean copyToLocalFileSystemFrom(String username, String password, String server, int port);
    boolean copyToLocalFileSystemFrom(String username, String keyFile, String keyFilePassphrase, String server, int port) ;
}
