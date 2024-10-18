package com.sapienter.jbilling.server.distributel;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.sapienter.jbilling.common.SessionInternalError;


public class DistributelPriceRowMapper implements RowMapper<DistributelPriceUpdateRequest> {

    @Override
    public DistributelPriceUpdateRequest mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        boolean isPricePresent = hasColumn(resultSet, "new_order_line_price");
        String price = null;
        if(isPricePresent) {
            price = resultSet.getString("new_order_line_price");
            if(price.contains("$")) {
                price = price.substring(1);
            }
        }
        return DistributelPriceUpdateRequest
                .builder()
                .id(resultSet.getInt("id"))
                .scheduledDateForAdjustment(hasColumn(resultSet, "scheduled_date_for_adjustment") ? resultSet.getString("scheduled_date_for_adjustment") : "")
                .scheduledDateForReversal(hasColumn(resultSet, "scheduled_date_for_reversal") ? resultSet.getString("scheduled_date_for_reversal") : "")
                .orderId(Integer.valueOf(resultSet.getString("order_id")))
                .customerId(Integer.valueOf(resultSet.getString("customer_id")))
                .productId(Integer.valueOf(resultSet.getString("product_id")))
                .newOrderLinePrice(isPricePresent ? new BigDecimal(price) : BigDecimal.ZERO)
                .status(resultSet.getString("status"))
                .invoiceNote(hasColumn(resultSet, "invoice_note") ? resultSet.getString("invoice_note") : "")
                .build()
                .validate();

    }

    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columns = rsmd.getColumnCount();
            for (int x = 1; x <= columns; x++) {
                if (columnName.equals(rsmd.getColumnName(x))) {
                    return true;
                }
            }
            return false;
        } catch(SQLException sqlException) {
            throw new SessionInternalError("erorr in hasColumn", sqlException);
        }
    }

}
