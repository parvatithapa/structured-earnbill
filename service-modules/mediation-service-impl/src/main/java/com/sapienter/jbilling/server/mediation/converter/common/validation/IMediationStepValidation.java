package com.sapienter.jbilling.server.mediation.converter.common.validation;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Generic validation step
 *
 * @author Panche Isajeski
 * @since 12/17/12
 */
public interface IMediationStepValidation {

    public boolean isValid(ICallDataRecord record, MediationStepResult result);
}
