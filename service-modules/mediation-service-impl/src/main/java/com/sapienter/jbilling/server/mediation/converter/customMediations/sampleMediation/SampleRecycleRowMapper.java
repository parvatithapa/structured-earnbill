package com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by andres on 28/10/15.
 */
public class SampleRecycleRowMapper implements RowMapper<CallDataRecord> {

    @Override
    public CallDataRecord mapRow(ResultSet resultSet, int i) throws SQLException {
        CallDataRecord callDataRecord = new CallDataRecord();
        String encodedPricingFields = resultSet.getString("pricing_fields");
        PricingField[] pricingFields = PricingField.getPricingFieldsValue(encodedPricingFields);
        callDataRecord.appendKey(resultSet.getString("record_key"));
        callDataRecord.setFields(new ArrayList<>(Arrays.asList(pricingFields)));
        return callDataRecord;
    }
}
