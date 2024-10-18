package com.sapienter.jbilling.server.util;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Context.Name;

public interface DbConnectionUtil {

    static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    
    public static boolean isEntityPersisted(String tableName, Serializable id) {
        if(!isTablePresent(tableName)) {
            logger.debug("Table {} not found", tableName);
            throw new IllegalArgumentException("Table Name " + tableName + "Not found!");
        }
        DataSource dataSource = Context.getBean(Name.DATA_SOURCE);
        try(Connection connection = dataSource.getConnection()) {
            try (PreparedStatement stmt = connection.prepareStatement(String.format("SELECT id FROM %s Where id = %s", tableName, id))) {
                try(ResultSet result = stmt.executeQuery()) {
                    boolean found = result.next();
                    logger.debug("Looking in side {} for id {} and it is {}", tableName, id, found ? "Found!" : "Not found!");
                    return found;
                }
            }
        } catch (SQLException e) {
            throw new SessionInternalError(e);
        }
    }

    public static boolean isTablePresent(String tableName) {
        DataSource dataSource = Context.getBean(Name.DATA_SOURCE);
        try (Connection connection = dataSource.getConnection()) {
            try (ResultSet rs = connection.getMetaData().getTables(null, null, tableName, null)) {
                return rs.next();
            }
        } catch (SQLException sqlException) {
            throw new SessionInternalError(sqlException);
        }
    }
}
