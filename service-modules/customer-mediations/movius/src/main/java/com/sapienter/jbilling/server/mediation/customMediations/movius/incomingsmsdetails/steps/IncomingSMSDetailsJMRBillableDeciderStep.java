package com.sapienter.jbilling.server.mediation.customMediations.movius.incomingsmsdetails.steps;

import java.math.BigDecimal;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class IncomingSMSDetailsJMRBillableDeciderStep extends AbstractMediationStep<MediationStepResult> {

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        result.setOriginalQuantity(BigDecimal.ONE);
        result.setQuantity(BigDecimal.ZERO);
        return true;
    }

}
