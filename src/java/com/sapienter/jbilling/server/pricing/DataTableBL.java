/*
 JBILLING CONFIDENTIAL
 _____________________
 [2003] - [2021] Enterprise jBilling Software Ltd.
 All Rights Reserved.
 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing;




import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.common.FormatLogger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains the method to perform the data table operations like create, update, delete on the data table
 */
public class DataTableBL {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
    private JdbcTemplate jdbcTemplate;
    private static final String ID = "id";

    public DataTableBL() {
        this.jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
    }
    public Integer createDataTableRecord(String dataTableName, Map<String, String> columns) {
        String sql = prepareCreateSql(dataTableName, columns);
        logger.debug("Create Data table record query : {} ",sql);
        return jdbcTemplate.update(sql);
    }
    public Integer updateDataTableRecord(String dataTableName, Map<String, String> columns) {
        String sql = prepareUpdateSql(dataTableName, columns);
        logger.debug("Update Data table record query : {} ",sql);
        return jdbcTemplate.update(sql);
    }
    public Integer deleteDataTableRecord(String dataTableName, final int rowId) {
        StringBuilder sqlBld = new StringBuilder("DELETE FROM ");
        sqlBld.append(dataTableName);
        sqlBld.append(" WHERE ").append(ID).append('=').append(rowId);
        String sql = sqlBld.toString();
        logger.debug("Delete Data table record query : {} ",sql);
        return jdbcTemplate.update(sql);  
    }
	/**
	 * * This the insert SQL for table updates
	 * @param tableName
	 * @param row
	 * @return
	 */
    private String prepareCreateSql(String tableName, Map<String, String> row) {
        StringBuilder insert = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
		
        insert.append(ID).append(',');
        values.append("(SELECT COALESCE(max(id),0)+1 FROM "+tableName+")").append(',');

        int count = row.size()-1;
        for (String key: row.keySet()) {
            String value = row.get(key);
            insert.append(key);
            values.append('\'')
            .append(escapeSpecialChars(value))
            .append('\'');

            if(count-- > 0) {
                insert.append(',');
                values.append(',');
            }
        }
        insert.append(')');
        values.append(')');

        String sql = insert.append(values).toString();
        return sql;
	}
    /**
     * Create the update SQL for table updates.
     *
     * @param row
     * @return
     */
    private String prepareUpdateSql(String tableName, Map<String, String> row) {
        StringBuilder update = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        StringBuilder where = new StringBuilder(" WHERE ");

        String id = row.remove(ID);
        where.append(ID).append('=').append(id);
        int count = row.size()-1;
        for (String key: row.keySet()) {
            String value = row.get(key);
            update.append(key).append("='").append(escapeSpecialChars(value)).append('\'');
            if(count-- > 0) {
                update.append(',');
            }
        }
        row.put(ID, id);
        return update.append(where).toString();
    }
    private static String escapeSpecialChars(String value) {
        if (value != null && value.indexOf("'") > -1) {
            value = value.replaceAll("'", "''");
        }
        return value;
    }
}