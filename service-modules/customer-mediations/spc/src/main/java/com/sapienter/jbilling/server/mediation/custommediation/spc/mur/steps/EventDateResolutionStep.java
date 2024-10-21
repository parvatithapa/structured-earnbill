package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;

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

@Component("optusMurEventDateResolutionStep")
public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private SPCMediationHelperService service;

    @Override
    public boolean executeStep(MediationStepContext context) {

        PricingField timeStampField = PricingField.find(context.getPricingFields(), SPCConstants.DATA_EVENT_DATE_FIELD_NAME);
        MediationStepResult result = context.getResult();

        if(null == timeStampField) {
            result.addError("ERR-EVENT-DATE-NOT-FOUND");
            return false;
        }
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(SPCConstants.DATA_EVENT_DATE_FORMAT);
            Date eventDate = dateFormat.parse(timeStampField.getStrValue());
            result.setEventDate(eventDate);
            return true;
        } catch (Exception e) {
            logger.error("Exception occurred while parsing event date :: ", e);
            result.addError("ERR-INVALID-EVENT-DATE");
            return false;
        }
    }

    private LocalDate getLocalDateOf(Date sourceDate) {
        if (sourceDate != null) {
            return sourceDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        return null;
    }
}
