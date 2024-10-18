package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;

/**
 * @author Harshad
 * @since Dec 19, 2018
 */
public class ItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private SPCMediationHelperService service;

    public ItemResolutionStep(SPCMediationHelperService service) {
        this.service= service;
    }

    public ItemResolutionStep() {
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            logger.debug("ItemResolutionStep.executeStep() : result {} : ", result);
            String productCode = null;
            String tariffCode = null;
            PricingField assetIdentifier = null;
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            if(MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_FIXED_LINE)) {
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER);
            } else if (MediationServiceType.fromServiceName(serviceType.getStrValue()).equals(MediationServiceType.TELSTRA_MOBILE_4G)) {
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.P1_S_P_NUMBER_EC_ADDRESS);
            } else {
                assetIdentifier = PricingField.find(context.getPricingFields(), SPCConstants.FROM_NUMBER);
            }

            if(null == assetIdentifier ||
                    StringUtils.isEmpty(assetIdentifier.getStrValue())) {
                result.addError("ITEM-NOT-FOUND");
                logger.debug("Asset Number not found from pricing fields ");
                return false;
            }

            // Adding asset number for resolving quantity
            context.getRecord().addField(new PricingField(SPCConstants.ASSET_NUMBER, assetIdentifier.getStrValue()), false);
            PricingField codeString = PricingField.find(context.getPricingFields(), SPCConstants.CODE_STRING);
            if (codeString != null && StringUtils.isNotEmpty(codeString.getStrValue())) {
                Map<String, String> tariffInfo =
                        service.getProductCodeFromRouteRateCard(result.getUserId(),
                                assetIdentifier.getStrValue(), codeString.getStrValue());

                productCode = tariffInfo.get(SPCConstants.PRODUCT_CODE);
                tariffCode = tariffInfo.get(SPCConstants.TARIFF_CODE);

                //Adding TARIFF_CODE to pricing fields
                context.getRecord().addField(new PricingField(SPCConstants.TARIFF_CODE, tariffCode), false);
                logger.debug("Resolved PRODUCT_CODE {} and TARIFF_CODE {} for CODE_STRING {}", productCode, tariffCode, codeString);
            }

            Optional<Integer> itemId = service.getItemIdByProductCode(productCode);
            if(!itemId.isPresent()) {
                logger.debug("Item not found for user {}", result.getUserId());
                result.addError("ITEM-NOT-FOUND");
                return false;
            }
            result.setItemId(itemId.get());
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
