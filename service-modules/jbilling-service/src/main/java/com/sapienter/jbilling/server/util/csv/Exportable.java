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

package com.sapienter.jbilling.server.util.csv;
import java.util.Date;
import org.joda.time.format.DateTimeFormat;

/**
 * Marks a class as being exportable in a simple format (such as CSV).
 *
 * @author Brian Cowdery
 * @since 03/03/11
 */
public interface Exportable {

    public String[] getFieldNames();
    public Object[][] getFieldValues();

}
