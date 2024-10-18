package com.sapienter.jbilling.server.mediation.customMediations.movius.outgoingcall.steps;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class OutgoingCallJMRBillableDeciderStep extends AbstractMediationStep<MediationStepResult> {

	private static final Logger LOG = LoggerFactory.getLogger(OutgoingCallJMRBillableDeciderStep.class);
    private static final String CALLED_ORG_ID = "Called Org Id";
    private static final String CALLED_BILLING_ID = "Called Billing Id";
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String calledOrgId = (String) PricingField.find(context.getPricingFields(), CALLED_ORG_ID).getValue();
            String calledBillingId = (String) PricingField.find(context.getPricingFields(), CALLED_BILLING_ID).getValue();
            
            if(StringUtils.isEmpty(calledBillingId) || 
                    StringUtils.isEmpty(calledOrgId)) {
                result.addError("INVALID-JMR-FORMAT");
                return false;
            }
            
            boolean isJMRBillable = false;
            if(calledBillingId.equals("0") && 
                    calledOrgId.equals("0")) {
                isJMRBillable = true;
            }
            
            if(!isJMRBillable) {
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
