package com.sapienter.jbilling.server.mediation.converter.common.validation;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Created by coredevelopment on 25/02/16.
 */
public class DuplicateRecordValidationStep implements com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation {
    public static String BEAN_NAME = "DuplicateRecordValidationStep";

    @Autowired
    private JMRRepository jmrRepository;

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {

        Integer recordKeyCount;
        try {
            recordKeyCount = jmrRepository.getRecordKeyCount(record.getKey());

        } catch (DataIntegrityViolationException e) {
            result.addError("JB-DATA-INTEGRITY-NON-TRANSIENT");
            return false;
        }

        if (recordKeyCount == null || recordKeyCount == 0) {
            return true;
        }
        result.addError("JB-DUPLICATE");
        return false;
    }

    public JMRRepository getJmrRepository() {
        return jmrRepository;
    }
}
