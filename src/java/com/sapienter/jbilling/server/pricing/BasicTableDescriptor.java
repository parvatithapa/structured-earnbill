package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.sql.JDBCUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Basic implementation of ITableDescriptor.
 *
 * @author Gerhard
 * @since 09/12/13
 */
public class BasicTableDescriptor implements ITableDescriptor {

    private JdbcTemplate jdbcTemplate = null;

    private String tableName;
    private List<String> columnNames;
    private List<String> primaryKeyColumns;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getPrimaryKeyColumns() {
        return primaryKeyColumns;
    }

    public List<String> getColumnsNames() {
        return columnNames;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Init method. Will load the table definition.
     */
    public void init() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        Connection connection = DataSourceUtils.getConnection(dataSource);

        try {
            columnNames = JDBCUtils.getAllColumnNames(connection, getTableName());
            primaryKeyColumns = JDBCUtils.getPrimaryKeyColumnNames(connection, getTableName());

        } catch (SQLException e) {

            throw new SessionInternalError("Could not read columns from route table.", e,
                    new String[] { "RouteWS,routes,route.cannot.read.rating.table" });

        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }
}
