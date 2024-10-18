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

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.util.MediationUtil;

/**
 * Resolves description based on input pricing fields
 *
 * @author Maryam Rehman
 * @since 5th Aug,2014
 */
public class DescriptionResolutionStep extends AbstractMediationStep<MediationStepResult> {
	private String callIdField = null;
    private String assetField = null;

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(DescriptionResolutionStep.class));
    
    @Override
    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {
		try {
			String Direction = PricingField.find(fields, "Direction").getStrValue();
			String assetField = PricingField.find(fields, getAssetField()).getStrValue();
        
			if(MediationUtil.isDirectionInbound(Direction,entityId) || MediationUtil.isDirectionIVR(Direction,entityId) ){
				String callIdField = PricingField.find(fields, "Caller_ID").getStrValue();
				String description = "Call from " + callIdField + " To " + assetField;
				result.setDescription(description);
			}else if(Direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.CHAT_CALL_TYPE.getMetaFieldName(), entityId))){
				String description = "Chat Account " + assetField;
				result.setDescription(description);
			}else if(Direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.ACTIVE_RESPONSE_CALL_TYPE.getMetaFieldName(), entityId))){
				String description = "ActiveResponse Account  " + assetField;
				result.setDescription(description);
			}
			return  true;
		} catch (Exception e) {
			result.addError("ERR-DESCRIPTION-NOT-FOUND");
		    return false;
		}
    }

    public String getCallIdField() {
		return this.callIdField;
	}

	public void setCallIdField(String callIdField) {
		this.callIdField = callIdField;
	}

    public String getAssetField() {
        return assetField;
    }

    public void setAssetField(String assetField) {
        this.assetField = assetField;
    }

	@Override
	public boolean executeStep(MediationStepContext context) {
		return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
	}

}
