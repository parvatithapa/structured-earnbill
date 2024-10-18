package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;

public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String dateFormat;

    public EventDateResolutionStep(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField connectTime = context.getPricingField(SapphireMediationConstants.CONNECT_TIME);
            SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
            String dateText = dateFormater.format(new Date(Long.parseLong(connectTime.getStrValue())));
            result.setEventDate(dateFormater.parse(dateText));
            logger.debug("Resolved Event date {}", result.getEventDate());
            return true;
        } catch(Exception ex) {
            result.addError("ERROR-EVENT-DATE-NOT-RESOLVED");
            logger.error("Event Resolution Failed!", ex);
            return false;

        }
    }

}
