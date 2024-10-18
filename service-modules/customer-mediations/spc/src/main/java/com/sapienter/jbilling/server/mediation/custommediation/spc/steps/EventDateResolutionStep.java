package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;

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
public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String OIGINATING_DATE = "Originating Date";
    private static final String OIGINATING_TIME = "Originating Time";

    private String datePattern;
    private String timeField;
    private SPCMediationHelperService service;

    public EventDateResolutionStep(String datePattern, String timeField, SPCMediationHelperService service) {
        this.datePattern = datePattern;
        this.timeField = timeField;
        this.service = service;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);
            String strDateTime = null;
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            MediationServiceType mediationType = MediationServiceType.fromServiceName(serviceType.getStrValue());
            if(mediationType.equals(MediationServiceType.TELSTRA_FIXED_LINE)) {
                PricingField originatingDateField = PricingField.find(context.getPricingFields(), OIGINATING_DATE);
                PricingField originatingTimeField = PricingField.find(context.getPricingFields(), OIGINATING_TIME);
                if( null == originatingDateField || null == originatingTimeField) {
                    result.addError("ERR-EVENT-DATE-NOT-FOUND");
                    return false;
                }
                strDateTime = originatingDateField.getStrValue() + originatingTimeField.getStrValue();
            } else if (mediationType.equals(MediationServiceType.TELSTRA_FIXED_LINE_MONTHLY)){
                PricingField originatingDateField = PricingField.find(context.getPricingFields(), SPCConstants.START_DATE);
                if( null == originatingDateField ) {
                    result.addError("ERR-EVENT-DATE-NOT-FOUND");
                    return false;
                }
                strDateTime = originatingDateField.getStrValue().trim() + "000000";
            } else {
                PricingField timeStampField = PricingField.find(context.getPricingFields(), timeField);
                if (null == timeStampField) {
                    result.addError("ERR-EVENT-DATE-NOT-FOUND");
                    return false;
                }
                strDateTime = timeStampField.getStrValue().trim();
            }

            logger.debug("Date time field is {}", strDateTime);
            Date currentDate = service.getCompanyCurrentDate(context.getEntityId());

            Date eventDate = null;

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            if (mediationType.equals(MediationServiceType.OPTUS_FIXED_LINE) ||
                    mediationType.equals(MediationServiceType.OPTUS_MOBILE) ||
                    mediationType.equals(MediationServiceType.AAPT_VOIP_CTOP) ||
                    mediationType.equals(MediationServiceType.TELSTRA_FIXED_LINE_MONTHLY)) {
                eventDate = formatter.parse(strDateTime);
            } else if (mediationType.equals(MediationServiceType.TELSTRA_MOBILE_4G)) {
                PricingField originatingDateField = PricingField.find(context.getPricingFields(), SPCConstants.P1_INITIAL_START_TIME_EC_TIME_OFFSET);
                String telstraOffset = originatingDateField.getStrValue();
                if (NumberUtils.isCreatable(telstraOffset) && 14 == strDateTime.length()) {
                    eventDate = new Date(formatter.parse(strDateTime).getTime() + Long.parseLong(telstraOffset) * 1000);
                }
            } else {
                eventDate = dateFormat.parse(strDateTime);
            }

            if (currentDate.compareTo(eventDate) < 0) {
                result.addError("ERR-EVENT-DATE-IS-IN-FUTURE");
                return false;
            }

            Integer entityId = context.getEntityId();
            Map<String, String> companyLevelMetaFieldMap = service.getMetaFieldsForEntity(entityId);
            String numberOfDaysToBackDatedEventsStr = companyLevelMetaFieldMap.get(SPCConstants.NUMBER_OF_DAYS_TO_BACK_DATED_EVENTS);
            boolean agedFlag = false;
            long daysBetween = daysBetween(eventDate, currentDate);
            if (StringUtils.isNotBlank(numberOfDaysToBackDatedEventsStr) && StringUtils.isNumeric(numberOfDaysToBackDatedEventsStr)) {
                agedFlag = daysBetween > Integer.parseInt(numberOfDaysToBackDatedEventsStr);
            }
            if(agedFlag) {
                logger.debug("event date is older than {} days", daysBetween);
                result.addError("ERR-EVENT-DATE-AGED-REJECTED");
                return false;
            }

            result.setEventDate(eventDate);
            return true;
        } catch (Exception e) {
            logger.error("Exception occurred while parsing event date :: ", e);
            result.addError("ERR-INVALID-EVENT-DATE");
            return false;
        }
    }

    private Date formatDateUsingZonedDateTime(String dateInString, String offset) {
        int length = dateInString.length();
        if (14 == length) {
            dateInString = dateInString + offset;
            DateTimeFormatter formatter = new DateTimeFormatterFactory("yyyyMMddHHmmssZ").createDateTimeFormatter();
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateInString, formatter);
            return Date.from(zonedDateTime.toInstant());
        }
        return null;
    }

    private long daysBetween(Date one, Date two) {
        return Math.abs(Days.daysBetween(new LocalDate(one.getTime()), new LocalDate(two.getTime())).getDays());
    }

    private <T> boolean checkForNull(T t) {
        return Optional.ofNullable(t).isPresent();
    }
}
