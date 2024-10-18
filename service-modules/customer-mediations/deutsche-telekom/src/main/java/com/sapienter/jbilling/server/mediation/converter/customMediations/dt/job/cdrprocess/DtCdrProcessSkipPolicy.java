package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.beans.factory.annotation.Autowired;

public class DtCdrProcessSkipPolicy implements SkipPolicy {

    @Autowired
    private JMErrorRepository errorRepository;

    @Override
    public boolean shouldSkip(Throwable t, int skipCount) throws SkipLimitExceededException {

        if (t.getClass().isAssignableFrom(DtCDRConversionDataException.class)) {
            DtCDRConversionDataException e = (DtCDRConversionDataException) t;
            JbillingMediationRecord record = e.getRecord();
            errorRepository.save(DaoConverter.getMediationErrorRecordDao(e.getErrorCodes(), record));
        }
        return true;
    }
}
