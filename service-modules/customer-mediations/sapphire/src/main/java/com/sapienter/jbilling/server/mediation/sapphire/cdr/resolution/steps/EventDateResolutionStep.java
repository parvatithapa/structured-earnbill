package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import java.lang.invoke.MethodHandles;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;

public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String dateFormat;
    private SapphireMediationHelperService sapphireMediationHelperService;

    public EventDateResolutionStep(String dateFormat, SapphireMediationHelperService sapphireMediationHelperService) {
        this.dateFormat = dateFormat;
        this.sapphireMediationHelperService = sapphireMediationHelperService;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField connectTime = context.getPricingField(SapphireMediationConstants.CONNECT_TIME);
            String companyTimeZone = sapphireMediationHelperService.getCompanyTimeZone(context.getEntityId());
            String formatedDate = DateFormatUtils.format(Long.parseLong(connectTime.getStrValue()),
                    dateFormat, TimeZone.getTimeZone(companyTimeZone));
            logger.debug("parsed text date {}", formatedDate);
            result.setEventDate(DateUtils.parseDate(formatedDate, dateFormat));
            logger.debug("Resolved Event date {}", result.getEventDate());
            return true;
        } catch(Exception ex) {
            result.addError("ERROR-EVENT-DATE-NOT-RESOLVED");
            logger.error("Event Resolution Failed!", ex);
            return false;

        }
    }

}
