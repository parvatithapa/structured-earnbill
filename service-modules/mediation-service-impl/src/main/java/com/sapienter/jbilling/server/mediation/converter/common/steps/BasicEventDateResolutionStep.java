/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.mediation.converter.common.steps;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;

/**
 * Resolves event dates based on input pricing fields
 */
public class BasicEventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger logger = new FormatLogger(Logger.getLogger(BasicEventDateResolutionStep.class));

    private String dateFieldName = null;
    private DateTimeFormatter formatter;

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String dateField = context.getPricingField(dateFieldName).getStrValue().trim();

            if (dateField != null) {
                Date parsedEventDate = null;

                try {
                    parsedEventDate = formatter.parseLocalDateTime(dateField).toDate();
                } catch (Exception e) {
                    logger.error("Exception occurred while parsing event date :: ", e);
                    result.addError("ERR-INVALID-EVENT-DATE");
                    return false;
                }

                result.setEventDate(parsedEventDate);
            }

            return  true;
        } catch (Exception e) {
            result.addError("ERR-EVENT-DATE-NOT-FOUND");
            return false;
        }
    }

    public void setDateFieldName(String dateFieldName) {
        this.dateFieldName = dateFieldName;
    }

    public void setDateFormat(String dateFormat) {
        formatter = DateTimeFormat.forPattern(dateFormat);
    }
}
