package com.sapienter.jbilling.server.mediation.converter;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ConversionResult;

import java.util.List;

/**
 * Created by marcolin on 06/10/15.
 */
public interface CdrConverter {

    /**
     * This method will convert the call data records and create the jBillingMediationRecords
     * @param processorName
     * @param records
     * @return
     */
    List<ConversionResult> convertCdrsToJmrs(String processorName, List<CallDataRecord> records);
}
