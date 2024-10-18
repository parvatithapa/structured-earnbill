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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Resolves description based on input pricing fields
 */
public class DescriptionFromFieldResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger logger = new FormatLogger(Logger.getLogger(DescriptionFromFieldResolutionStep.class));
    
    private String[] fieldNames = null;

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String desc = Arrays.asList(fieldNames).stream()
                    .map(fieldName -> String.valueOf(context.getPricingField(fieldName).getValue()))
                    .filter(fieldValue -> !fieldValue.trim().isEmpty())
                    .collect(Collectors.joining(","));

            result.setDescription(desc);
            return  true;

        } catch (Exception e) {
            result.addError("ERR-DESCRIPTION-NOT-FOUND");
            return false;
        }
    }

    public void setFieldNames(String[] fieldNames) {
        this.fieldNames = fieldNames;
    }

}
