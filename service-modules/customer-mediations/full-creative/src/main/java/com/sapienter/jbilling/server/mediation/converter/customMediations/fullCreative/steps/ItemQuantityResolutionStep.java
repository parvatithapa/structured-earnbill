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

import java.math.BigDecimal;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;
import com.sapienter.jbilling.server.mediation.mrim.RatingSchemeDAS;
import com.sapienter.jbilling.server.util.ParseHelper;
import com.sapienter.jbilling.server.util.Constants;
/**
 * Resolves quantity/duration based on input pricing fields
 *
 * @author Maryam Rehman
 * @since  04/08/2014
 */
public class ItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ItemQuantityResolutionStep.class));
	private static final Integer MINUTE_IN_SECONDS = 60;
	
	private String quantityField = null;
    private RatingSchemeDAS ratingSchemeDAS = null;
    private MediationHelperService mediationHelperService = null;

	@Override
	public boolean executeStep(MediationStepContext context) {
		try {
	        MediationStepResult result = (MediationStepResult) context.getResult();

			String quantityField = PricingField.find(context.getPricingFields(), getQuantityField()).getStrValue();
			Integer intQuantity =  ParseHelper.parseInteger(quantityField);
			BigDecimal quantity = resolveQuantityByRatingScheme(intQuantity, context.getResult().getMediationCfgId(), result.getUserId());
			result.setQuantity(quantity);

			if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
				BigDecimal originalQuantity = BigDecimal.valueOf(intQuantity).divide(BigDecimal.valueOf(60), Constants.BIGDECIMAL_QUANTITY_SCALE, Constants.BIGDECIMAL_ROUND);
				PricingField oQPricingField = new PricingField("Original_Quantity", originalQuantity);
	            context.getRecord().addField(oQPricingField, false);
			}

			return true;
		} catch (Exception e) {
			context.getResult().addError("ERR-ITEM-QUANTITY-NOT-FOUND");
	        return false;
	    }
	}

	private BigDecimal resolveQuantityByRatingScheme(Integer quantity, Integer mediationCfgId, Integer userId) {
		Integer company = getMediationHelperService().getUserCompanyByUserId(userId);
        Integer ratingSchemeId = getRatingSchemeIdForMediation(mediationCfgId, company);
        return getQuantity(ratingSchemeId, quantity);
    }
    
    private Integer getRatingSchemeIdForMediation(Integer mediationCfgId, Integer entity) {
    	Integer ratingScheme = getRatingSchemeDAS().getRatingSchemeByMediationAndEntity(mediationCfgId, entity);
        if(ratingScheme == null) {
            //Look for global ratingScheme
            ratingScheme = getRatingSchemeDAS().getGlobalRatingSchemeByEntity(entity);
        }
        return ratingScheme;
    }
    
    private BigDecimal getQuantity(Integer ratingSchemeId, Integer callDuration ) {

        BigDecimal quantity = BigDecimal.valueOf(callDuration);
        Map<String, Integer> ratingSchemeMap = getRatingSchemeDAS().getMediationRatingSchemeById(ratingSchemeId);

        if(ratingSchemeMap != null) {
            Integer initialIncrement = ratingSchemeMap.get("initial_increment");
            Integer initialRoundingMode = ratingSchemeMap.get("initial_rounding_mode");
            Integer mainIncrement = ratingSchemeMap.get("main_increment");
            Integer mainRoundingMode = ratingSchemeMap.get("main_rounding_mode");

            if (callDuration <= initialIncrement) {
                quantity = BigDecimal.valueOf(callDuration)
                        .divide(BigDecimal.valueOf(initialIncrement), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND)
                        .setScale(0, initialRoundingMode).multiply(BigDecimal.valueOf(initialIncrement));
            } else {
                quantity = BigDecimal.valueOf(initialIncrement)
                        .add((BigDecimal.valueOf(callDuration - initialIncrement)
                                .divide(BigDecimal.valueOf(mainIncrement), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND))
                                .setScale(0, mainRoundingMode).multiply(BigDecimal.valueOf(mainIncrement)));
            }

            return quantity.divide(BigDecimal.valueOf(MINUTE_IN_SECONDS), 1, Constants.BIGDECIMAL_ROUND);
        }

        return quantity;
    }
    
	public String getQuantityField() {
		return this.quantityField;
	}

	public void setQuantityField(String quantityField){
		this.quantityField = quantityField;
	}

	public RatingSchemeDAS getRatingSchemeDAS() {
        return ratingSchemeDAS;
    }

    public void setRatingSchemeDAS(RatingSchemeDAS ratingSchemeDAS) {
        this.ratingSchemeDAS = ratingSchemeDAS;
    }

	public MediationHelperService getMediationHelperService() {
		return mediationHelperService;
	}

	public void setMediationHelperService(MediationHelperService mediationHelperService) {
		this.mediationHelperService = mediationHelperService;
	}
    
}
