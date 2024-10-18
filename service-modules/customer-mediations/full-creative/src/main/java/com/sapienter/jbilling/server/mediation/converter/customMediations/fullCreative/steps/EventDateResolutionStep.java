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

package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.steps;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

/**
 * Resolves event dates based on input pricing fields
 *
 * @author Harshad Pathan
 */
public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final FormatLogger LOG = new FormatLogger(EventDateResolutionStep.class);
    
    private String date = null;
    private String time = null;
    private DateFormat dateFormat;

    @Override
    public boolean executeStep(Integer entityId, MediationStepResult result, List<PricingField> fields) {
		try {
	        String date = PricingField.find(fields, getDate()).getStrValue();
	        String time = PricingField.find(fields, getTime()).getStrValue();

	        if (date != null && time != null) {
				Date parsedEventDate = null;
	            String dateTimeToBeParsed = date + " " + time;

				try {
					parsedEventDate = dateFormat.parse(dateTimeToBeParsed);
				} catch (ParseException e) {
					 LOG.error("Exception occurred while parsing event date :: ", e);
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

    public String getTime() {
		return this.time;
	}

	public String getDate() {
		return this.date;
	}
	
	public void setTime(String time){
		this.time = time;
	}
	
	public void setDate(String date){
		this.date = date;
	}
    
    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

	@Override
	public boolean executeStep(MediationStepContext context) {
		return executeStep(context.getEntityId(), context.getResult(), context.getPricingFields());
	}
}
