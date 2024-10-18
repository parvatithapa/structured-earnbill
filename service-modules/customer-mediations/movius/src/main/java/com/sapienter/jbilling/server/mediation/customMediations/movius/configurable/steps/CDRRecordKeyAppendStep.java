package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.common.validation.DuplicateRecordValidationStep;
import com.sapienter.jbilling.server.util.Context;

public class CDRRecordKeyAppendStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger LOG = LoggerFactory.getLogger(CDRRecordKeyAppendStep.class);
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            ICallDataRecord record = context.getRecord();
            DuplicateRecordValidationStep duplicateValidation = Context.getBean(DuplicateRecordValidationStep.BEAN_NAME);
            Integer recordKeyCount = duplicateValidation.getJmrRepository().getRecordKeyCount(record.getKey());
            if(null == recordKeyCount || recordKeyCount == 0) {
                return true;
            }
            String key = UUID.randomUUID().toString();
            record.appendKey(key.substring(0, key.indexOf('-')));
            LOG.debug("New CDR Key is {}", record.getKey());
            result.setCdrRecordKey(record.getKey());
            return true;
        } catch(Exception ex) {
            LOG.error(ex.getMessage(), ex);
            result.addError("ERROR-IN-CDR-RECORD-KEY-APPEND-STEP");
            return false;
        }
    }

}
