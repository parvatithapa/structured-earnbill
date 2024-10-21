package com.sapienter.jbilling.server.distributel;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;


public class DistributelPriceRowMapper implements RowMapper<DistributelPriceUpdateRequest> {

    @Override
    public DistributelPriceUpdateRequest mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        String price = resultSet.getString("new_order_line_price");
        if(price.contains("$")) {
            price = price.substring(1);
        }
        return DistributelPriceUpdateRequest.builder()
                .id(resultSet.getInt("id"))
                .scheduledDateForAdjustment(resultSet.getString("scheduled_date_for_adjustment"))
                .orderId(Integer.valueOf(resultSet.getString("order_id")))
                .customerId(Integer.valueOf(resultSet.getString("customer_id")))
                .productId(Integer.valueOf(resultSet.getString("product_id")))
                .newOrderLinePrice(new BigDecimal(price))
                .status(resultSet.getString("status"))
                .invoiceNote(resultSet.getString("invoice_note"))
                .build()
                .validate();

    }

}
