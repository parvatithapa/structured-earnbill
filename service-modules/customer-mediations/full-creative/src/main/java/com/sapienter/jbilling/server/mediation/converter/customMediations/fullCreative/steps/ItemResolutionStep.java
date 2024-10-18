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

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.steps;

import java.util.List;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.util.MediationUtil;

/**
 * Resolves item based on CDR file uploaded
 *
 * @author Maryam Rehman
 * @since 21/07/14
 */
public class ItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ItemResolutionStep.class));
    
    private MediationHelperService mediationHelperService;


    @Override
    public boolean executeStep(MediationStepContext context) {
		try {
			Integer configId = context.getResult().getMediationCfgId();
			MediationStepResult result = context.getResult();
			Integer entityId = context.getEntityId();
			List<PricingField> fields = context.getPricingFields();

			String mediationConfig = getMediationHelperService().getMediationJobLauncherByConfigId(configId);
			String direction = PricingField.find(fields, "Direction").getStrValue();

	        if (MediationUtil.isInbound(mediationConfig, direction, entityId)) {
	            result.setItemId(Integer.valueOf(MediationCacheManager.getMetaFieldValue(MetaFieldName.INBOUND_CALL_ITEM.getMetaFieldName(), entityId)));
	            PricingField callTypeField = new PricingField("Call_Type", 1);
	            context.getRecord().addField(callTypeField, false);
	        } else if (mediationConfig.equals(FullCreativeConstants.ACTIVE_RESPONSE_MEDIATION_CONFIGURATION) &&
	                direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.ACTIVE_RESPONSE_CALL_TYPE.getMetaFieldName(), entityId))) {
	            result.setItemId(Integer.valueOf(MediationCacheManager.getMetaFieldValue(MetaFieldName.ACTIVE_RESPONSE_ITEM.getMetaFieldName(), entityId)));
	            PricingField callTypeField = new PricingField("Call_Type", 2);
	            context.getRecord().addField(callTypeField, false);
	        } else if (mediationConfig.equals(FullCreativeConstants.CHAT_MEDIATION_CONFIGURATION) &&
	                direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.CHAT_CALL_TYPE.getMetaFieldName(), entityId))) {
	            result.setItemId(Integer.valueOf(MediationCacheManager.getMetaFieldValue(MetaFieldName.CHAT_ITEM.getMetaFieldName(), entityId)));
	            PricingField callTypeField = new PricingField("Call_Type", 3);
	            context.getRecord().addField(callTypeField, false);
	        } else if (MediationUtil.isIVR(mediationConfig, direction, entityId)) {
	            result.setItemId(Integer.valueOf(MediationCacheManager.getMetaFieldValue(MetaFieldName.IVR_ITEM.getMetaFieldName(), entityId)));
	            PricingField callTypeField = new PricingField("Call_Type", 4);
	            context.getRecord().addField(callTypeField, false);
	        } else {
	            if (MediationUtil.isDirectionInbound(direction, entityId) ||
	                    direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.ACTIVE_RESPONSE_CALL_TYPE.getMetaFieldName(), entityId)) ||
	                    direction.equalsIgnoreCase(MediationCacheManager.getMetaFieldValue(MetaFieldName.CHAT_CALL_TYPE.getMetaFieldName(), entityId)) ||
	                    MediationUtil.isDirectionIVR(direction, entityId)) {
	                result.setDone(true);
	                result.addError("ERR-MISMATCH-OF-MEDIATION-CONFIG-AND-CALL-TYPE");
	                LOG.error("Call type is correct but apparently there is a mismatch of mediation configuraton and call type");
	                return false;
	            } else {
	                result.setDone(true);
	                result.addError("ERR-UNKNOWN-CALL-TYPE");
	                LOG.error("Unknown Call Type");
	                return false;
	            }
	        }

	        return true;
		} catch (Exception e) {
			context.getResult().addError("ERR-ITEM-NOT-FOUND");
		    return false;
		}
    }

    public MediationHelperService getMediationHelperService() {
	    return mediationHelperService;
	}
	
	public void setMediationHelperService(MediationHelperService mediationHelperService) {
	    this.mediationHelperService = mediationHelperService;
	}
}