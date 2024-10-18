package com.sapienter.jbilling.server.batch.mediation;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.RowMapper;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;

public class DefaultRecycleReader implements RowMapper<CallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public CallDataRecord mapRow(ResultSet resultSet, int i) throws SQLException {
        CallDataRecord callDataRecord = new CallDataRecord();
        String encodedPricingFields = resultSet.getString("pricing_fields");
        PricingField[] pricingFields = PricingField.getPricingFieldsValue(encodedPricingFields);
        callDataRecord.appendKey(resultSet.getString("record_key"));
        callDataRecord.setFields(new ArrayList<>(Arrays.asList(pricingFields)));
        logger.debug("Created cdr {}", callDataRecord);
        return callDataRecord;
    }

}
