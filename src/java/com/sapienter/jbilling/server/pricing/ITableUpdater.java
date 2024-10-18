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


import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResult;
import com.sapienter.jbilling.server.util.search.SearchResultString;

import java.util.List;
import java.util.Map;

/**
 * Interrogates the content of a table. Provides methods to update/create/delete and list content.
 *
 * @author Gerhard Maree
 */
public interface ITableUpdater {

    /**
     * Create a row in a table
     * @param row
     * @return
     */
    public int create(Map<String,String> row);

    /**
     * Update a row in a table
     * @param row
     */
    public void update(Map<String,String> row);

    /**
     * Delete a row in a table.
     * @param rowId
     */
    public void delete(int rowId);

    /**
     * Returns the entire content of a table. Each map returned represents a row with the key, the column name.
     * @return
     */
    public List<Map<String, String>> list();

    /**
     * Search the table, only returning matching rows.
     *
     * @param criteria
     *
     */
    public SearchResultString search(SearchCriteria criteria);
}
