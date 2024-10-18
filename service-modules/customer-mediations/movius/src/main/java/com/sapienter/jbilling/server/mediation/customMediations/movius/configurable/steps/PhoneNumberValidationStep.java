package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;
import com.sapienter.jbilling.server.util.Context;

public class PhoneNumberValidationStep implements IMediationStepValidation {

	private static final Logger LOG = LoggerFactory.getLogger(PhoneNumberValidationStep.class);
    private static final String PLUS = "+";
    private final String phoneNumberField;
    
    public PhoneNumberValidationStep(String phoneNumberField) {
        this.phoneNumberField = phoneNumberField;
    }
    
    private boolean addValidationMessage(MediationStepResult result, String errorMessage) {
        result.addError(errorMessage);
        return false;
    }

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {
        try {
            PricingField numberField = PricingField.find(record.getFields(), phoneNumberField);
            if(null == numberField || StringUtils.isEmpty(numberField.getStrValue())) {
                return addValidationMessage(result, "INVALID-DESTINATION-NUMBER");
            }
            PhoneNumberUtil phoneUtil = Context.getBean(MoviusUtil.PHONE_UTIL);
            String rawNumber = numberField.getStrValue();
            if(!rawNumber.startsWith(PLUS)) {
                rawNumber = PLUS.concat(rawNumber);
            }
            
            PhoneNumber number = phoneUtil.parse(rawNumber, "");
            if(!phoneUtil.isValidNumber(number)) {
                return addValidationMessage(result, "INVALID-DESTINATION-NUMBER");
            }
            return true;
        } catch(NumberParseException ex) {
            LOG.error(ex.getMessage(), ex);
            return addValidationMessage(result, "ERROR-VALIDATING-DESTINATION-NUMBER");
        }
    }

}
