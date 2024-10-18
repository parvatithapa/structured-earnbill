package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationUtil;

@Component("optusMurUserResolutionStep")
class UserResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String BILLABLE_NUMBER_FIELD_NAME = "Billable Number";
    private static final String BILLABLE_NUMBER_ERROR_FIELD_VALUE = "61";

    @Autowired
    private SPCMediationHelperService service;

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField billableNumberField = PricingField.find(context.getPricingFields(), BILLABLE_NUMBER_FIELD_NAME);

            if (null == billableNumberField) {
                result.addError("BILLABLE-NUMBER-NOT-FOUND");
                return false;
            }

            String billableNumber = billableNumberField.getStrValue();
            String tempBillableNumber = StringUtils.EMPTY;
            if(StringUtils.isEmpty(billableNumber)) {
                result.addError("BILLABLE-NUMBER-NOT-FOUND");
                return false;
            }
            boolean isAssetPresent = false;
            if (billableNumber.startsWith(BILLABLE_NUMBER_ERROR_FIELD_VALUE)) {
        		tempBillableNumber = convertValidAssetId(billableNumber);
        		isAssetPresent = service.isIdentifierPresent(tempBillableNumber);
        	}           
            if(isAssetPresent) {
            	billableNumber = tempBillableNumber;
            } else {            	
            	isAssetPresent = service.isIdentifierPresent(billableNumber);            	
            	if(!isAssetPresent) {
            		result.addError("ERR-ASSET-NOT-ASSIGNED-TO-ANY-CUSTOMER");
            		logger.error("Asset {} found but is not assigned to any customer", billableNumber);
            		return isAssetPresent;
            	}
            }

            Map<String, Integer> userCurrencyMap = service.getUserIdForAssetIdentifier(billableNumber, result.getEventDate());
            if (SPCMediationUtil.isEmpty(userCurrencyMap)) {
                result.addError("USER-NOT-FOUND");
                return false;
            }
            logger.debug("user resolved {} for entity {}", userCurrencyMap, context.getEntityId());
            result.setUserId(userCurrencyMap.get(MediationStepResult.USER_ID));
            result.setCurrencyId(userCurrencyMap.get(MediationStepResult.CURRENCY_ID));
            result.setSource(billableNumber);
            Optional<Integer> orderId = service.findSubscriptionOrderByUserAssetEventDate(result.getUserId(), billableNumber, result.getEventDate());
            context.getRecord().addField(new PricingField(SPCConstants.PURCHASE_ORDER_ID, orderId.isPresent() ? orderId.get() : null), false);

            if (!orderId.isPresent()) {
                result.addError("ERR-SUBSCRIPTION-ORDER-NOT-FOUND");
                return false;
            } else {
                //Validating event date with order's active since/until dates
                Map<String, Date> orderDatesMap = service.getActiveSinceAndActiveUntilDates(orderId.get());
                if (SPCMediationUtil.isEmpty(orderDatesMap)) {
                    result.addError("ERR-SUBSCRIPTION-ORDER-NOT-FOUND");
                    return false;
                }

                LocalDate localEventDate = getLocalDateOf(result.getEventDate());
                LocalDate localActiveSinceDate = getLocalDateOf(orderDatesMap.get(SPCConstants.ACTIVE_SINCE_DATE));
                LocalDate localActiveUntilDate = getLocalDateOf(orderDatesMap.get(SPCConstants.ACTIVE_UNTIL_DATE));
                if(localEventDate != null && localActiveSinceDate != null && localEventDate.isBefore(localActiveSinceDate)) {
                    result.setEventDate(null);
                    result.addError("ERR-EVENT-DATE-IS-BEFORE-ACTIVE-SINCE-DATE");
                    return false;
                } else if(localEventDate != null && localActiveUntilDate != null && localEventDate.isAfter(localActiveUntilDate)) {
                    result.setEventDate(null);
                    result.addError("ERR-EVENT-DATE-IS-AFTER-ACTIVE-UNTIL-DATE");
                    return false;
                }
            }

            return true;

        } catch (Exception ex) {
            result.addError("USER-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

	private String convertValidAssetId(String billableNumber) {
		String substring = billableNumber.substring(2,billableNumber.length());
		return "0".concat(substring);
	}

    private LocalDate getLocalDateOf(Date sourceDate) {
        if (sourceDate != null) {
            return sourceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }
}
