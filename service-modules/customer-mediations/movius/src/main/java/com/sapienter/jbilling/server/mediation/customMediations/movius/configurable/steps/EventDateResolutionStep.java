package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import java.text.DateFormat;
import java.text.ParseException;

import org.springframework.util.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class EventDateResolutionStep extends AbstractMediationStep<MediationStepResult> {

	private static final Logger LOG = LoggerFactory.getLogger(EventDateResolutionStep.class);
      
      private DateFormat dateFormat;
      private String timeField;
      
      public EventDateResolutionStep(DateFormat format, String timeField) {
        this.dateFormat = format;
        this.timeField = timeField;
      }   
      
      @Override
      public boolean executeStep(MediationStepContext context) {

          PricingField timeStampField = PricingField.find(context.getPricingFields(), timeField);
          MediationStepResult result = context.getResult();

          if(null == timeStampField) {
              result.addError("ERR-EVENT-DATE-NOT-FOUND");
              return false;
          }
          try {
              result.setEventDate(dateFormat.parse(timeStampField.getStrValue()));
              return true;
          } catch (ParseException e) {
              LOG.error("Exception occurred while parsing event date :: ", e);
              result.addError("ERR-INVALID-EVENT-DATE");
              return false;
          }
      }

      public DateFormat getDateFormat() {
          return dateFormat;
      }

      public void setDateFormat(DateFormat dateFormat) {
          Assert.notNull(dateFormat, "DateFormat Property can not be Null!");
          this.dateFormat = dateFormat;
      }
      
      

  }
