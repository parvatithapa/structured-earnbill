package com.sapienter.jbilling.server.integration.common.utility;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.sapienter.jbilling.server.integration.common.service.vo.OrderLineTierInfo;

public class TierRowMapper implements RowMapper {

    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

        return OrderLineTierInfo.builder()
        .orderLineid(rs.getInt("order_line_id"))
        .tierNumber(rs.getInt("tier_number"))
        .quantity(rs.getBigDecimal("quantity"))
        .price(rs.getBigDecimal("price"))
        .amount(rs.getBigDecimal("amount"))
        .tierFrom(rs.getBigDecimal("tier_from"))
        .tierTo(rs.getBigDecimal("tier_to")).build();

    }
}
