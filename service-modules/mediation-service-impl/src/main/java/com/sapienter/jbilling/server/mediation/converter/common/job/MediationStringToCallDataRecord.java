package com.sapienter.jbilling.server.mediation.converter.common.job;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.reader.MediationRecordLineConverter;
import org.springframework.batch.item.*;

/**
 * Created by marcolin on 07/10/15.
 */
public class MediationStringToCallDataRecord implements ItemProcessor<String, ICallDataRecord> {

    private MediationRecordLineConverter converter;

    public void setConverter(MediationRecordLineConverter converter) {
        this.converter = converter;
    }

    @Override
    public ICallDataRecord process(String line) throws Exception {
        return converter.convertLineToRecord(line);
    }
}
