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


import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import org.apache.log4j.Logger;


/**
 * Created with IntelliJ IDEA.
 *
 * @author Panche Isajeski
 * @since 12/18/12
 */
public class JMREventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(JMREventDateResolutionStep.class));

    @Override
    public boolean executeStep(MediationStepContext context) {
        PricingField start = PricingField.find(context.getPricingFields(), "start");
        if (start != null) {
            context.getResult().setEventDate(start.getDateValue());
        }

        PricingField startTime = PricingField.find(context.getPricingFields(), "start_time");
        if (startTime != null) {
            context.getResult().setEventDate(startTime.getDateValue());
        }


        if (context.getResult().getEventDate() == null) {
            LOG.debug("Event date cannot be found in the pricing fields " + context.getPricingFields());
            return false;
        }

        LOG.debug("Set result date " + context.getResult().getEventDate());
        return true;

    }
}
