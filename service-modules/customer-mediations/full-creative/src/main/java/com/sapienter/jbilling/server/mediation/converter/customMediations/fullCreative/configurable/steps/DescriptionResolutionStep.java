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

import java.util.List;
import org.springframework.util.Assert;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Resolves description based on input pricing fields
 *
 * @author Krunal Bhavsar
 * @since May, 2018
 */
public class DescriptionResolutionStep extends AbstractMediationStep<MediationStepResult> {

	private final String sourceFieldName;
	private final String destinationFieldName;
	private final String directionFieldName;


    public DescriptionResolutionStep(String sourceFieldName,
            String destinationFieldName, String directionFieldName) {
        Assert.hasLength(destinationFieldName, "destinationFieldName can not be null and empty!");
        Assert.hasLength(sourceFieldName, "sourceFieldName can not be null and empty!");
        Assert.hasLength(directionFieldName, "directionFieldName can not be null and empty!");
        this.sourceFieldName = sourceFieldName;
        this.destinationFieldName = destinationFieldName;
        this.directionFieldName = directionFieldName;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
		try {
		    List<PricingField> fields = context.getPricingFields();
			String direction = PricingField.find(fields, directionFieldName).getStrValue();
			String destination = PricingField.find(fields, destinationFieldName).getStrValue();
			PricingField sourcePricingField = PricingField.find(fields, sourceFieldName);
			String source = "N/A";
			if( null!=sourcePricingField ) {
			    source = sourcePricingField.getStrValue();
			}
			result.setDescription(direction + " Source Number: "+ source +" Destination Number: " + destination);
			result.setSource(source);
			result.setDestination(destination);
			return  true;
		} catch (Exception e) {
			result.addError("ERR-DESCRIPTION-NOT-FOUND");
		    return false;
		}
    }

}
