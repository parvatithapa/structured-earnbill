/*
 JBILLING CONFIDENTIAL
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

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.steps;

import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractUserResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;

/**
 * Resolves the user from the CDR based on 'asset Identifier' field for AnswerConnect
 * <p/>
 *
 * @author Maryam Rehman
 * @since  11th July, 2014.
 */
public class UserItemFromAssetResolutionStep extends AbstractUserResolutionStep<MediationStepResult> {

	private String assetField = null;
	private MediationHelperService mediationHelperService;

	@Override
    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {
		try {
	        PricingField assetField = PricingField.find(fields, getAssetField());
	        String agentLogin = PricingField.find(fields, "Agent_Login").getStrValue();
	        Integer talkTime = PricingField.find(fields, "Talk Time(s)").getIntValue();
	        if (assetField != null) {
	            if ( agentLogin == null || agentLogin.trim().length()==0 || "null".equalsIgnoreCase(agentLogin.trim())) {
					result.setDone(true);
	                result.addError("ERR-NO-AGENT-SPECIFIED-CALL-NOT-ANSWERED");
	                LOG.error("No agent specified, call not answered");
	                return false;
	            }
	            else if(talkTime <= 0){
					result.setDone(true);
	                result.addError("ERR-TALK-TIME-LESS-OR-EQUAL-TO-ZERO-CALL-NOT-ANSWERED");
	                LOG.error("Talk Time less than or equal to zero, call not answered");
	                return false;
	            }
	            else {
	                Map<String, Object> userDTOMap = MediationCacheManager.resolveUserByAssetField(entityId, assetField.getStrValue());
	                if (userDTOMap.isEmpty()) {
						boolean isAssetPresent = getMediationHelperService().doesAssetIdentifierExist(assetField.getStrValue());
	                    if (isAssetPresent) {
	                        result.setDone(true);
							result.addError("ERR-ASSET-NOT-ASSIGNED-TO-ANY-CUSTOMER");
							LOG.error("Asset " + assetField + " found but is not assigned to any customer");
							return false;
	                    } else {
							result.setDone(true);
	                        result.addError("ERR-ASSET-NOT-FOUND");
	                        LOG.error("Asset " + assetField + " not found");
	                        return false;
	                    }
	                }
	                return setUserOnResult(result, userDTOMap);
	            }
	        }
	        return false;
		} catch (Exception e) {
			result.addError("ERR-USER-ITEM-NOT-FOUND");
	        return false;
	    }
	}

	public String getAssetField() {
	    return assetField;
	}

	public void setAssetField(String assetField) {
	    this.assetField = assetField;
	}

	public MediationHelperService getMediationHelperService() {
	    return mediationHelperService;
	}

	public void setMediationHelperService(MediationHelperService mediationHelperService) {
	    this.mediationHelperService = mediationHelperService;
	}

	@Override
	public boolean executeStep(MediationStepContext context) {
	    return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
	}
}