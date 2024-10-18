package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.jdbc.core.RowMapper;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;

/**
 * @author Neelabh
 * @since Dec 18, 2018
 */
public class SPCRecycleRowMapper implements RowMapper<CallDataRecord> {

    @Override
    public CallDataRecord mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
        CallDataRecord callDataRecord = new CallDataRecord();
        String encodedPricingFields = resultSet.getString("pricing_fields");
        PricingField[] pricingFields = PricingField.getPricingFieldsValue(encodedPricingFields);
        callDataRecord.appendKey(resultSet.getString("record_key"));
        callDataRecord.setFields(new ArrayList<>(Arrays.asList(pricingFields)));
        return callDataRecord;
    }
}
