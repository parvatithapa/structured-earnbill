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

package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

import com.sapienter.jbilling.server.customer.CustomerService;
import com.sapienter.jbilling.server.customer.User;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractUserResolutionStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation.DistributelMediationConstant;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.lang.Override;

/**
 * Created by igutierrez on 25/01/17.
 */
public class DistributelDateResolutionStep extends AbstractUserResolutionStep<MediationStepResult> {
    @Override
    public boolean executeStep(MediationStepContext context) {
        Date date = null;

        try {
            String startStamp = PricingField.find(context.getPricingFields(), DistributelMediationConstant.INVOICE_DATE).getStrValue();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DistributelMediationConstant.DATE_FORMAT);
            LocalDate localDate = LocalDate.parse(startStamp, formatter);
            date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            context.getResult().setEventDate(date);
            return true;
        } catch (Exception e) {
            if(date == null){
                context.getResult().addError("Format date error");
            }else {
                context.getResult().addError("Event Not Found");
            }
            return false;
        }
    }
}