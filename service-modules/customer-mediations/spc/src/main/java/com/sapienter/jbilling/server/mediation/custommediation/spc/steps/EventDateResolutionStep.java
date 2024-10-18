package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.datetime.standard.DateTimeFormatterFactory;
import org.springframework.util.Assert;

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

    private SimpleDateFormat dateFormat;
    private String timeField;
    private SPCMediationHelperService service;

    public EventDateResolutionStep(SimpleDateFormat format, String timeField, SPCMediationHelperService service) {
        this.dateFormat = format;
        this.timeField = timeField;
        this.service = service;
        dateFormat.setLenient(false);
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
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
            } else {
                PricingField timeStampField = PricingField.find(context.getPricingFields(), timeField);
                if (null == timeStampField) {
                    result.addError("ERR-EVENT-DATE-NOT-FOUND");
                    return false;
                }
                strDateTime = timeStampField.getStrValue().trim();
            }

            logger.debug("Date time field is {}", strDateTime);
            String companyTimeZone = service.getCompanyLevelTimeZone(context.getEntityId());
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(companyTimeZone));
            Date currentDate = calendar.getTime();
            Date eventDate = null;

            if (mediationType.equals(MediationServiceType.OPTUS_FIXED_LINE) ||
                    mediationType.equals(MediationServiceType.OPTUS_MOBILE) ||
                    mediationType.equals(MediationServiceType.TELSTRA_MOBILE_4G) ||
                    mediationType.equals(MediationServiceType.AAPT_VOIP_CTOP)) {
                String offset = service.getCompanyLevelTimeZoneOffSet(context.getEntityId());
                Date eventDateFromCDR = formatDateUsingZonedDateTime(strDateTime, offset);
                String dateString = dateFormat.format(eventDateFromCDR);
                eventDate = dateFormat.parse(dateString);
            } else {
                dateFormat.setTimeZone(TimeZone.getTimeZone(companyTimeZone));
                eventDate = dateFormat.parse(strDateTime);
            }

            if (currentDate.compareTo(eventDate) < 0) {
                result.addError("ERR-EVENT-DATE-IS-IN-FUTURE");
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

    public DateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        Assert.notNull(dateFormat, "DateFormat Property can not be Null!");
        this.dateFormat = dateFormat;
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
}
