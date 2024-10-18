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

package com.sapienter.jbilling.server.pricing;


import java.util.List;

/**
 * Describes a database table.
 *
 * @author Gerhard Maree
 */
public interface ITableDescriptor {

    public String getTableName();

    /**
     * Column names of primary keys.
     * @return
     */
    public List<String> getPrimaryKeyColumns();

    /**
     * All column names, including the primary keys.
     * @return
     */
    public List<String> getColumnsNames();

}
