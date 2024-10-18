package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.common.CommonConstants.BIGDECIMAL_QUANTITY_SCALE;
import static com.sapienter.jbilling.common.CommonConstants.BIGDECIMAL_ROUND;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CONNECT_TIME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.RELAESE_TIME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.SECONDS;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.MediationMRIMService;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.util.Context;

public class QuantityResolutionStep  extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField releaseTime = context.getPricingField(RELAESE_TIME);
            PricingField connectTime = context.getPricingField(CONNECT_TIME);

            long release = Long.parseLong(releaseTime.getStrValue());
            long connect = Long.parseLong(connectTime.getStrValue());

            Long diffSeconds = TimeUnit.MILLISECONDS.toSeconds(release - connect);
            logger.debug("Resolved Quantity in seconds {}", diffSeconds);
            BigDecimal originalQuantity = new BigDecimal(diffSeconds).divide(SECONDS, BIGDECIMAL_QUANTITY_SCALE, BIGDECIMAL_ROUND);
            logger.debug("Resolved Original quantity {}", originalQuantity);
            MediationMRIMService mrimService = Context.getBean("mediationMRIMServiceImpl");
            BigDecimal quantity = mrimService.getQuantity(result.getMediationCfgId(), result.getjBillingCompanyId(), diffSeconds.intValue());
            result.setQuantity(quantity);
            result.setOriginalQuantity(originalQuantity);
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-QUANTITY-RESOLUTION");
            logger.error("Quantity resolution failed!", ex);
            return false;
        }
    }

}
