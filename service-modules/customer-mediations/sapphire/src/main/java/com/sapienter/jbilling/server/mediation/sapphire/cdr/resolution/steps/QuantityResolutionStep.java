package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CONNECT_TIME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.RELAESE_TIME;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.MediationMRIMService;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class QuantityResolutionStep  extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final MediationMRIMService mrimService;

    public QuantityResolutionStep(MediationMRIMService mrimService) {
        this.mrimService = mrimService;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField releaseTime = context.getPricingField(RELAESE_TIME);
            PricingField connectTime = context.getPricingField(CONNECT_TIME);

            long release = Long.parseLong(releaseTime.getStrValue());
            long connect = Long.parseLong(connectTime.getStrValue());

            Long diffSeconds = roundAndConvertToSecond(release - connect);
            logger.debug("Resolved Quantity in seconds {}", diffSeconds);
            BigDecimal quantity = mrimService.getQuantity(result.getMediationCfgId(), result.getjBillingCompanyId(), diffSeconds.intValue());
            result.setQuantity(quantity);
            result.setOriginalQuantity(new BigDecimal(diffSeconds));
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-QUANTITY-RESOLUTION");
            logger.error("Quantity resolution failed!", ex);
            return false;
        }
    }

    /**
     * Rounds milliseconds by a second and convert it to seconds.
     * @param timeDiff
     * @return
     */
    private long roundAndConvertToSecond(long timeDiff) {
        long unit = ChronoUnit.SECONDS.getDuration().toMillis();
        long reminder = timeDiff % unit;
        if(Long.compare(reminder, 0) > 0) {
            timeDiff =  timeDiff + unit;
        }
        return TimeUnit.MILLISECONDS.toSeconds(timeDiff);
    }

}
