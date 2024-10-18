package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SimpleDateFormat dateFormat;
    private String timeField;

    public EventDateResolutionStep(SimpleDateFormat format, String timeField) {
        this.dateFormat = format;
        this.timeField = timeField;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {

        PricingField timeStampField = PricingField.find(context.getPricingFields(), timeField);
        MediationStepResult result = context.getResult();

        if(null == timeStampField) {
            result.addError("ERR-EVENT-DATE-NOT-FOUND");
            return false;
        }
        try {
            result.setEventDate(dateFormat.parse(timeStampField.getStrValue()));
            return true;
        } catch (Exception e) {
            logger.error("Exception occurred while parsing event date :: ", e);
            result.addError("ERR-INVALID-EVENT-DATE");
            return false;
        }
    }
}
