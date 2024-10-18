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

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String dateFieldName;
    private String timeFieldName;
    private DateFormat dateFormat;

    public EventDateResolutionStep(String dateFieldName, String timeFieldName, DateFormat dateFormat) {
        Assert.hasLength(dateFieldName, "dateFieldName can not be null and empty!");
        Assert.hasLength(timeFieldName, "timeFieldName can not be null and empty!");
        Assert.notNull(dateFormat, "DateFormat can not be null!");
        this.dateFieldName = dateFieldName;
        this.timeFieldName = timeFieldName;
        this.dateFormat = dateFormat;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            List<PricingField> fields = context.getPricingFields();
            String date = PricingField.find(fields, dateFieldName).getStrValue();
            String time = PricingField.find(fields, timeFieldName).getStrValue();
            if (StringUtils.isNotEmpty(date) && StringUtils.isNotEmpty(time)) {
                String dateTimeToBeParsed = date + " " + time;
                result.setEventDate(dateFormat.parse(dateTimeToBeParsed));
                return true;
            }
            return false;
        } catch (ParseException e) {
            logger.error("Exception occurred while parsing event date :: ", e);
            result.addError("ERR-INVALID-EVENT-DATE");
            return false;
        } catch (Exception e) {
            result.addError("ERR-EVENT-DATE-NOT-FOUND");
            return false;
        }
    }
}
