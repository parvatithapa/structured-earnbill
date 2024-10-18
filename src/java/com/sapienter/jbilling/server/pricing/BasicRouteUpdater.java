package com.sapienter.jbilling.server.pricing;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import org.apache.log4j.Logger;
import com.sapienter.jbilling.server.util.search.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ITableUpdater information for a route table.
 *
 * @author Gerhard
 * @since 09/12/13
 */
public class BasicRouteUpdater implements ITableUpdater {
    
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BasicRouteUpdater.class));

    private JdbcTemplate jdbcTemplate = null;
    private TransactionTemplate transactionTemplate = null;
    private ITableDescriptor tableDescriptor = null;

    private static final String ID = "id";

    public int create(Map<String, String> row) {
        int id = 0;
        if(!row.containsKey(ID)) {
            id = maxId() + 1;
            row.put(ID, Integer.toString(id));
        } else {
            id = Integer.parseInt(row.get(ID));
            if(idExists(id)) {
                throw new SessionInternalError("ID already exists for the record", new String[] {"route.record.id.exists"}) ;
            }
        }
        final String sql = prepareCreateSql(row);

        if(transactionTemplate != null) {
            transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus ts) {
                    return jdbcTemplate.update(sql);
                }
            });
        } else {
            jdbcTemplate.update(sql);
        }

        return id;
    }

    public void update(Map<String, String> row) {
        final String sql = prepareUpdateSql(row);
        if(transactionTemplate != null) {
            transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus ts) {
                return jdbcTemplate.update(sql);
            }
        });
        } else {
            jdbcTemplate.update(sql);
        }
    }

    public void delete(final int rowId) {
        StringBuilder sqlBld = new StringBuilder("DELETE FROM ");
        sqlBld.append(tableDescriptor.getTableName());
        sqlBld.append(" WHERE ").append(ID).append('=').append(rowId);

        final String sql = sqlBld.toString();

        if(transactionTemplate != null) {
            transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus ts) {
                    return jdbcTemplate.update(sql);
                }
            });
        } else {
            jdbcTemplate.update(sql);
        }
    }

    public List<Map<String, String>> list() {
        StringBuilder sqlBld = new StringBuilder("SELECT * FROM ");
        sqlBld.append(tableDescriptor.getTableName());

        final String sql = sqlBld.toString();
        final RowMapper<Map<String, String>> rowMapper = new RowMapper<Map<String, String>>() {
            @Override
            public Map<String, String> mapRow(ResultSet resultSet, int i) throws SQLException {
                Map<String, String> row = new HashMap<String, String>();
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                while(columnCount > 0) {
                    row.put(metaData.getColumnName(columnCount), resultSet.getString(columnCount));
                    columnCount--;
                }
                return row;
            }
        };

        if(transactionTemplate != null) {
            return (List<Map<String, String>>)transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus ts) {
                    return jdbcTemplate.query(sql, rowMapper);
                }
            });
        } else {
            return jdbcTemplate.query(sql, rowMapper);
        }
    }

    public SearchResultString search(SearchCriteria criteria) {
        LOG.debug(criteria);
        SearchResultString result = new SearchResultString();
        FilterSqlHelper.search(criteria, new StringRowMapper(), result, tableDescriptor.getTableName(), jdbcTemplate);
        return result;
    }

    /**
     * Create SQL for INSERT statement.
     *
     * @param row
     * @return
     */
    private String prepareCreateSql(Map<String, String> row) {
        StringBuilder insert = new StringBuilder("INSERT INTO ").append(tableDescriptor.getTableName()).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");

        //add the id column
        insert.append(ID).append(',');
        values.append(row.remove(ID)).append(',');

        //add the rest of the columns
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
        LOG.debug("Create Sql: %s", sql);
        return sql;
    }

    /**
     * Create the SQL for table updates.
     *
     * @param row
     * @return
     */
    private String prepareUpdateSql(Map<String, String> row) {
        StringBuilder update = new StringBuilder("UPDATE ").append(tableDescriptor.getTableName()).append(" SET ");
        StringBuilder where = new StringBuilder(" WHERE ");

        //add the id column
        String id = row.remove(ID);
        where.append(ID).append('=').append(id);

        //add the rest of the columns
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

    /**
     * Return the maximum 'id' in the table.
     *
     * @return
     */
    private int maxId() {
        if(transactionTemplate != null) {
            return (Integer)transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus ts) {
                    Integer maxId = jdbcTemplate.queryForObject("SELECT max(id) FROM " + tableDescriptor.getTableName(), Integer.class);
                    return (maxId == null) ? 0 : maxId;
                }
            });
        } else {
            Integer maxId = jdbcTemplate.queryForObject("SELECT max(id) FROM " + tableDescriptor.getTableName(), Integer.class);
            return (maxId == null) ? 0 : maxId;
        }
    }

    /**
     * Returns true if the table already contains the id
     * @param id
     * @return
     */
    private boolean idExists(final int id) {
        if(transactionTemplate != null) {
            return (Boolean)transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction(TransactionStatus ts) {
                    return !jdbcTemplate.queryForList("SELECT 1 FROM "+tableDescriptor.getTableName()+" WHERE id="+id).isEmpty();
                }
            });
        } else {
            return !jdbcTemplate.queryForList("SELECT 1 FROM "+tableDescriptor.getTableName()+" WHERE id="+id).isEmpty();
        }
    }

    /**
     * Escape string to be SQL safe.
     *
     * @param value
     * @return
     */
    private static String escapeSpecialChars(String value) {
        if (value != null && value.indexOf("'") > -1) {
            value = value.replaceAll("'", "''");
        }
        return value;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void setTransactionTemplate(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void setTableDescriptor(ITableDescriptor tableDescriptor) {
        this.tableDescriptor = tableDescriptor;
    }
}
