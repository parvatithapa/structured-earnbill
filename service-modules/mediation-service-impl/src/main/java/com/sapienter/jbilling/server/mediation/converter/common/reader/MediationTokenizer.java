/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.reader;

import com.sapienter.jbilling.server.mediation.converter.common.Format;

/**
 * Created with IntelliJ IDEA.
 *
 * @author Panche Isajeski
 * @since 12/18/12
 */
public interface MediationTokenizer {

    String[] tokenize(String line, Format format);
}
