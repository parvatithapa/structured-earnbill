package com.sapienter.jbilling.server.distributel;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

public abstract class DistributelHelperUtil {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String UPDATE_SQL = "UPDATE %s SET status = ? WHERE id = ?";

    public static void updateRequestStatus(DistributelPriceUpdateRequest request, String status, String tableName) {
        DataSource dataSource = Context.getBean(Name.DATA_SOURCE);
        try(Connection connection = dataSource.getConnection()) {
            String sql = String.format(UPDATE_SQL, tableName);
            try ( PreparedStatement preparedStmt = connection.prepareStatement(sql) ) {
                preparedStmt.setString(1, status);
                preparedStmt.setInt(2, request.getId());
                preparedStmt.executeUpdate();
            }
            logger.debug("Price {} updated on order {} for product {}",
                    request.getNewOrderLinePrice(), request.getOrderId(), request.getProductId());
        } catch (SQLException ex) {
            logger.error("Error during update status ", ex);
        }
    }
}
