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

package com.sapienter.jbilling.server.util.sql;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Simple table generator that takes a list of columns and produces SQL to create
 * and manipulate data in a database table.
 *
 * @author Brian Cowdery
 * @since 15-Feb-2012
 */
public class TableGenerator {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(TableGenerator.class));
    private static final String DOUBLE_QUOTES = "\"";

    /**
     * Columns of the table schema.
     */
    public static class Column {
        private final String name;
        private final String dataType;
        private final boolean nullable;
        private final boolean primaryKey;

        public Column(String name, String dataType, boolean nullable) {
            this.name = name.trim().toLowerCase();
            this.dataType = dataType;
            this.nullable = nullable;
            this.primaryKey = false;
        }

        public Column(String name, String dataType, boolean nullable, boolean primaryKey) {
            this.name = name.trim().toLowerCase();
            this.dataType = dataType;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        public String getName() {
            return name;
        }

        public String getDataType() {
            return dataType;
        }

        public boolean isNullable() {
            return nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }

        @Override
        public String toString() {
            StringBuilder sql = new StringBuilder();
            /*
             * The double quotes are necessary, because if the name of the column has a '-'
             * without this, postgresql throws a exception
             */

            sql.append(DOUBLE_QUOTES)
               .append(name)
               .append(DOUBLE_QUOTES)
               .append(' ')
               .append(dataType);

            return sql.toString();
        }
    }



    private List<Column> columns = new ArrayList<Column>();
    private String tableName;

    public TableGenerator() {
    }

    public TableGenerator(String tableName, List<Column> columns) {
        this.tableName = tableName.replaceAll("'", "''");
        this.columns = new ArrayList<Column>(columns); // safety first kids!
    }

    public void addColumn(Column column) {
        this.columns.add(column);
    }

    public void addColumns(List<Column> columns) {
        this.columns.addAll(columns);
    }

    public List<Column> getColumns() {
        return columns;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName.replaceAll("'", "''");
    }


    /**
     * Produces the DDL statements to create a table with the set columns.
     *
     * @return table creation DDL statement
     */
    public String buildCreateTableSQL() {
        StringBuilder ddl = new StringBuilder();
        ddl.append("create table ").append(tableName).append(" (");

        StringBuilder pk = new StringBuilder();
        pk.append("primary key (");

        int primaryKeys = 0;

        for (Iterator<Column> it = columns.iterator(); it.hasNext();) {
            Column column = it.next();
            ddl.append(column);

            if (!column.isNullable())
                ddl.append(" not null");

            if (column.isPrimaryKey()) {
                if (primaryKeys > 0)
                    pk.append(", ");

                pk.append(column.name);
                primaryKeys++;
            }

            if (it.hasNext())
                ddl.append(", ");
        }

        if (primaryKeys > 0) {
            pk.append(')');
            ddl.append(", ").append(pk);
        }

        ddl.append(");");

        LOG.debug("Generated create table SQL [" + ddl + "]");
        return ddl.toString();
    }

    /**
     * Produces the DDL statement to drop a table from the schema.
     *
     * @return drop table DDL statement
     */
    public String buildDropTableSQL() {
        String drop = "drop table if exists " + tableName + ";";
        LOG.debug("Generated drop table SQL [" + drop + "]");

        return drop;
    }

    /**
     * Produces the DDL statement to rename a a table;.
     *
     * @param newTableName new table name
     * @return alter table DDL statement
     */
    public String buildRenameTableSQL(String newTableName) {
        String alter = "alter table " + tableName + " rename to " + newTableName + ";";
        LOG.debug("Generated rename table SQL [" + alter + "]");

        return alter;
    }

    /**
     * Produces a prepared statement SQL template for the set columns.
     *
     * Example:
     * <code>
     *     insert into table_name (col1, col2) values (?, ?);
     * </code>
     *
     * @return prepared statement SQL template
     */
    public String buildInsertPreparedStatementSQL() {
        StringBuilder insert = new StringBuilder();
        insert.append("insert into ").append(tableName).append(" (");

        StringBuilder values = new StringBuilder();
        values.append("values (");

        for (Iterator<Column> it = columns.iterator(); it.hasNext();) {
            Column column = it.next();

            /*
             * The double quotes are necessary, because if the name of the column has a '-'
             * without this, postgresql throws a exception
             */

            insert.append(DOUBLE_QUOTES);
            insert.append(column.getName());
            insert.append(DOUBLE_QUOTES);
            values.append('?');

            if (it.hasNext()) {
                insert.append(", ");
                values.append(", ");
            }
        }

        insert.append(") ");
        values.append(");");

        insert.append(values);

        LOG.debug("Generated insert statement [" + insert + "]");
        return insert.toString();
    }

}
