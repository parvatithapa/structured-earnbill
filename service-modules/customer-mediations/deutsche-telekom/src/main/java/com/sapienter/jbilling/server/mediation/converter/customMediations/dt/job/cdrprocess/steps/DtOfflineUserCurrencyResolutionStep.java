/*
` JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.cdrprocess.steps;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper.MediationHelperService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves quantity based on rating scheme linked to the product
 */
public class DtOfflineUserCurrencyResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private MediationHelperService mediationHelperService;
    private String userReferenceField = "BSSParams";

    @Override
    public boolean executeStep(MediationStepContext context) {
		try {
            String userReference = context.getPricingField(userReferenceField).getStrValue().trim();
            Map<String, Object> userCurrencyMap = mediationHelperService.resolveCustomerByExternalAccountIdentifier( context.getEntityId(), userReference);
            if(userCurrencyMap.isEmpty()) {
                logger.info("User not found {}", context.getPricingFields());
                context.getResult().addError("JB-USER-NOT-RESOLVED");
                return false;
            }
            context.getResult().setCurrencyId((Integer)userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            context.getResult().setUserId((Integer)userCurrencyMap.get(MediationStepResult.USER_ID));
            return true;
		} catch (Exception e) {
            logger.info("User not found {}", context.getPricingFields());
			context.getResult().addError("JB-USER-NOT-RESOLVED");
		    return false;
		}
    }

    public void setUserReferenceField(String userReferenceField) {
        this.userReferenceField = userReferenceField;
    }

    public void setMediationHelperService(MediationHelperService mediationHelperService) {
	    this.mediationHelperService = mediationHelperService;
	}
}
