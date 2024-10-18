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

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps;

import static com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.UNDER_INITIAL_INCREMENT_THRESHOLD_QUANTITY_FIELD_NAME;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;
import com.sapienter.jbilling.server.mediation.mrim.RatingSchemeDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.ParseHelper;

public class ItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final BigDecimal MINUTE_IN_SECONDS = new BigDecimal("60");
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String quantityFieldName = null;
    private RatingSchemeDAS ratingSchemeDAS = null;
    private MediationHelperService mediationHelperService = null;

    public ItemQuantityResolutionStep(String quantityFieldName, RatingSchemeDAS ratingSchemeDAS,
            MediationHelperService mediationHelperService) {
        Assert.hasLength(quantityFieldName, "quantityField can not be null and empty!");
        Assert.notNull(ratingSchemeDAS, "ratingSchemeDAS can not be null!");
        Assert.notNull(mediationHelperService, "mediationHelperService can not be null!");
        this.quantityFieldName = quantityFieldName;
        this.ratingSchemeDAS = ratingSchemeDAS;
        this.mediationHelperService = mediationHelperService;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        try {
            MediationStepResult result = context.getResult();
            String quantityField = PricingField.find(context.getPricingFields(), quantityFieldName).getStrValue();
            Integer intQuantity =  ParseHelper.parseInteger(quantityField);
            BigDecimal quantity = resolveQuantity(intQuantity, context);
            result.setQuantity(quantity);
            return true;
        } catch (Exception e) {
            logger.error("Exception in ItemQuantityResolutionStep!", e);
            context.getResult().addError("ERR-ITEM-QUANTITY-NOT-FOUND");
            return false;
        }
    }

    private BigDecimal resolveQuantity(Integer callDuration, MediationStepContext mediationStepContext) {
        MediationStepResult mediationStepResult = mediationStepContext.getResult();
        Integer company = mediationHelperService.getUserCompanyByUserId(mediationStepResult.getUserId());
        Integer ratingSchemeId = getRatingSchemeIdForMediation(mediationStepResult.getMediationCfgId(), company);
        Map<String, Integer> ratingSchemeMap = ratingSchemeDAS.getMediationRatingSchemeById(ratingSchemeId);
        BigDecimal resolvedQuantity = convertQuantityByRatingScheme(callDuration, ratingSchemeMap);
        if(resolvedQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal originalQuantity = BigDecimal.valueOf(callDuration)
                    .divide(MINUTE_IN_SECONDS, Constants.BIGDECIMAL_QUANTITY_SCALE, Constants.BIGDECIMAL_ROUND);
            mediationStepResult.setOriginalQuantity(originalQuantity);
            if(null!= ratingSchemeMap) {
                Integer initialIncrement = ratingSchemeMap.get("initial_increment");
                BigDecimal underInitialQuantity = BigDecimal.valueOf(callDuration)
                        .divide(BigDecimal.valueOf(initialIncrement), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND)
                        .setScale(0, BigDecimal.ROUND_UP).multiply(BigDecimal.valueOf(initialIncrement))
                        .divide(MINUTE_IN_SECONDS, 1, Constants.BIGDECIMAL_ROUND);
                PricingField underInitialQuantityPricingField = new PricingField(UNDER_INITIAL_INCREMENT_THRESHOLD_QUANTITY_FIELD_NAME, underInitialQuantity);
                mediationStepContext.getPricingFields().add(underInitialQuantityPricingField);
            }
        }
        return resolvedQuantity;
    }

    private Integer getRatingSchemeIdForMediation(Integer mediationCfgId, Integer entity) {
        Integer ratingScheme = ratingSchemeDAS.getRatingSchemeByMediationAndEntity(mediationCfgId, entity);
        if(ratingScheme == null) {
            //Look for global ratingScheme
            ratingScheme = ratingSchemeDAS.getGlobalRatingSchemeByEntity(entity);
        }
        return ratingScheme;
    }

    private BigDecimal convertQuantityByRatingScheme(Integer callDuration, Map<String, Integer> ratingSchemeMap) {
        BigDecimal quantity = BigDecimal.valueOf(callDuration);
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
            return quantity.divide(MINUTE_IN_SECONDS, 1, Constants.BIGDECIMAL_ROUND);
        }
        return quantity;
    }

}
