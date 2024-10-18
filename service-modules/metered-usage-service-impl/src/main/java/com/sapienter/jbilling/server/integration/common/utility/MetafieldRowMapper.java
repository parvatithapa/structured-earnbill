package com.sapienter.jbilling.server.integration.common.utility;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import org.springframework.jdbc.core.RowMapper;


public class MetafieldRowMapper implements RowMapper {

    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {

        MetaFieldValueWS metafieldValue = new MetaFieldValueWS();
        metafieldValue.setId(rs.getInt("id"));
        metafieldValue.setBooleanValue(rs.getBoolean("boolean_value"));
        metafieldValue.setDateValue(rs.getDate("date_value"));
        metafieldValue.setDecimalValue(rs.getString("decimal_value"));
        metafieldValue.setIntegerValue(rs.getInt("integer_value"));
        metafieldValue.setStringValue(rs.getString("string_value"));

        return metafieldValue;
    }
}
