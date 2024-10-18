package com.sapienter.jbilling.server.mediation.customMediations.movius.incomingcall.steps;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class IncomingCallJMRBillableDeciderStep extends AbstractMediationStep<MediationStepResult> {

	private static final Logger LOG = LoggerFactory.getLogger(IncomingCallJMRBillableDeciderStep.class);
    private static final String OUT_GOING_MODE = "Outgoing Mode";
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String outGoingMode = (String) PricingField.find(context.getPricingFields(), OUT_GOING_MODE).getValue();
            
            if(StringUtils.isEmpty(outGoingMode)) {
                result.addError("INVALID-JMR-FORMAT");
                return false;
            }
            
            if(!outGoingMode.equalsIgnoreCase("TDM")) {
            	result.setOriginalQuantity(new BigDecimal(result.getQuantity()));
            	result.setQuantity(BigDecimal.ZERO);
            }
            
            return true;
        } catch(Exception ex) {
            result.addError("INVALID-JMR-FORMAT");
            LOG.error(ex.getMessage(), ex);
            return false;
        }
    }

}
