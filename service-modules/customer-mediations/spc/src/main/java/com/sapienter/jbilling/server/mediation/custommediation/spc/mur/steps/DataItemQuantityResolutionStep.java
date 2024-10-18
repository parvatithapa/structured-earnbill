package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

@Component("optusMurDataItemQuantityResolutionStep")
class DataItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String VOLUMN_DOWNLINK_FILED_NAME = "Volume Downlink";
    private static final String VOLUMN_UPLINK_FILED_NAME = "Volume Uplink";

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            List<PricingField> prcingFields = context.getPricingFields();
            PricingField upLinkField = PricingField.find(prcingFields, VOLUMN_UPLINK_FILED_NAME);
            PricingField downLinkField = PricingField.find(prcingFields, VOLUMN_DOWNLINK_FILED_NAME);
            if(null == upLinkField || StringUtils.isEmpty(upLinkField.getStrValue())) {
                result.addError("ITEM-QUANTITY-NOT-RESOLVED");
                logger.debug("{} field not found in cdr", VOLUMN_UPLINK_FILED_NAME);
                return false;
            }

            if(null == downLinkField || StringUtils.isEmpty(downLinkField.getStrValue())) {
                result.addError("ITEM-QUANTITY-NOT-RESOLVED");
                logger.debug("{} field not found in cdr", VOLUMN_DOWNLINK_FILED_NAME);
                return false;
            }

            BigDecimal uplinkVolume = new BigDecimal(upLinkField.getStrValue());
            BigDecimal downlinkVolume = new BigDecimal(downLinkField.getStrValue());
            logger.debug("{} is {} and {}  is {}", VOLUMN_UPLINK_FILED_NAME, uplinkVolume, VOLUMN_DOWNLINK_FILED_NAME, downLinkField);
            BigDecimal duration  = uplinkVolume.add(downlinkVolume);
            result.setQuantity(duration);
            result.setOriginalQuantity(duration);
            logger.debug("resolved duration {}", duration);
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-QUANTITY-RESOLUTION");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
