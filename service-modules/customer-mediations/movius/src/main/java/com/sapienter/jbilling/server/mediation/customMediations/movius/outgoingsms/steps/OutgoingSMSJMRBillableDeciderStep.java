package com.sapienter.jbilling.server.mediation.customMediations.movius.outgoingsms.steps;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class OutgoingSMSJMRBillableDeciderStep extends AbstractMediationStep<MediationStepResult> {

	private static final Logger LOG = LoggerFactory.getLogger(OutgoingSMSJMRBillableDeciderStep.class);
    
    private static final String APPLICATION_NAME = "application name";
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String applicationName = (String) PricingField.find(context.getPricingFields(), APPLICATION_NAME).getValue();
            
            if(StringUtils.isEmpty(applicationName)) {
                result.addError("INVALID-JMR-FORMAT");
                return false;
            }
            
            if(!applicationName.equals("sms-receiver")) {
				result.setOriginalQuantity(new BigDecimal(result.getQuantity()));
                result.setQuantity(BigDecimal.ZERO);
            }
            return true;
        } catch(Exception ex) {
            LOG.error(ex.getMessage(), ex);
            result.addError("INVALID-JMR-FORMAT");
            return false;
        }
    }

}
