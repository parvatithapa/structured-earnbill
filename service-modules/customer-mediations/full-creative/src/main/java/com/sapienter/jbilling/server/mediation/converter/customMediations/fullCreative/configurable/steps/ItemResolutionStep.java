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

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps;

import java.lang.invoke.MethodHandles;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import static com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName.*;

public class ItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final String directionFieldName;

    private static final EnumSet<MetaFieldName> LIVE_ANSWER_PRODUCT_DIRECTION = EnumSet.of(INBOUND_CALL_TYPE,
            ACTIVE_RESPONSE_CALL_TYPE, CHAT_CALL_TYPE, SPANISH_CALL_TYPE, LIVE_RECEPTION_CALL_TYPE, CALL_RELAY_CALL_TYPE, SUPERVISOR_CALL_TYPE,
            WEB_FORM_CALL_TYPE);

    public ItemResolutionStep(String directionFieldName) {
        Assert.hasLength(directionFieldName, "directionFieldName can not be null or empty!");
        this.directionFieldName = directionFieldName;
    }

    private Integer resolveProductbyDirection(String direction, Integer entityId) {
        for(MetaFieldName directionName : LIVE_ANSWER_PRODUCT_DIRECTION) {
            if(direction.equals(MediationCacheManager.getMetaFieldValue(directionName.getMetaFieldName(), entityId))){
                return Integer.valueOf(MediationCacheManager.getMetaFieldValue(MetaFieldName.LIVE_ANSWER_ITEM.getMetaFieldName(), entityId));
            }
        }
        return Integer.valueOf(MediationCacheManager.getMetaFieldValue(MetaFieldName.IVR_ITEM.getMetaFieldName(), entityId));
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
		try {
			MediationStepResult result = context.getResult();
			Integer entityId = context.getEntityId();
			List<PricingField> fields = context.getPricingFields();
			String direction = PricingField.find(fields, directionFieldName).getStrValue();
			Integer itemId = resolveProductbyDirection(direction, entityId);
			if( itemId == null ) {
			    context.getResult().addError("ERR-ITEM-NOT-FOUND");
	            return false;
			}
			result.setItemId(itemId);
	        return true;
		} catch (Exception e) {
		    logger.error("ItemResolutionStep in error", e);
			context.getResult().addError("ERR-ITEM-NOT-RESOLVED");
		    return false;
		}
    }
}