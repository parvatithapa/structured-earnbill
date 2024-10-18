package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;


public class UserResolutionStep extends AbstractMediationStep<MediationStepResult>  {

	private static final Logger LOG = LoggerFactory.getLogger(UserResolutionStep.class);
    
    private String orgIdFieldName;
    
    public UserResolutionStep(String orgIdFieldName) {
        this.orgIdFieldName = orgIdFieldName;
    }
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField orgIdField = PricingField.find(context.getPricingFields(), orgIdFieldName);
            if(null == orgIdField) {
                result.addError("ERR-ORG-ID-NOT-FOUND");
                return false;
            }

            Map<Integer, Integer> userCurrencyMap = MoviusUtil.getUserIdByOrgIdMetaField(context.getEntityId(), orgIdField.getStrValue());
            if(MoviusUtil.isEmpty(userCurrencyMap)) {
                result.addError("USER-NOT-FOUND");
                return false;
            }
            
            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            return true;
            
        } catch(Exception ex) {
            result.addError("USER-NOT-RESOLVED");
            LOG.error(ex.getMessage(), ex);
            return false;
        }
        
    }

}
