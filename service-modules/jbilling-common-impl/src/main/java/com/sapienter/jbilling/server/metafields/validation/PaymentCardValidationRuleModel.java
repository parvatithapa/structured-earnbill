package com.sapienter.jbilling.server.metafields.validation;

import java.util.regex.Pattern;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;
import com.sapienter.jbilling.server.util.Constants;

/**
 * Payment card validation rule model
 * Verifies that a payment card number is valid
 * 
 * @author khobab
 *
 */
public class PaymentCardValidationRuleModel extends AbstractValidationRuleModel{
    
	@Override
	public ValidationReport doValidation(MetaContent source, Object object,
			ValidationRule validationRule, Integer languageId) {
		if (!verifyValidationParameters(object, validationRule, languageId)) {
            return null;
        }

        String errorMessage = validationRule.getErrorMessage(languageId);
        ValidationReport report = new ValidationReport();

        if (!isPaymentCardValid((char[])object)) {
            report.addError("MetaFieldValue,value," + errorMessage);
        }

        return report;
	}
	
	/**
	 * Verifies if card number is valid using Luhn's Algorithm
	 * 
	 * @param cardNumber	card number to be verified
	 * @return	true if card number is valid
	 */
	public boolean isPaymentCardValid(char[] cardNumber) {

        if(isPaymentCardObscure(cardNumber))
            return true;

        Integer sum = 0;
	    Integer digit = 0;
	    Integer addend = 0;
	    Boolean timesTwo = false;

	    for (int i = cardNumber.length - 1; i >= 0; i--) {
	        
	    	try {
	            //digit = Integer.parseInt(cardNumber.substring(i, i + 1));
	            digit = Character.getNumericValue(cardNumber[i]);
	        } catch (Exception e) {
	            return false;    
	        }
	        
	        if (timesTwo) {
	            addend = digit * 2;
	            if (addend > 9) {
	                addend -= 9;
	            }
	        } else {
	            addend = digit;
	        }
	        
	        sum += addend;
	        timesTwo = !timesTwo;
	    }

	    Integer modulus = sum % 10;
	    
	    return modulus == 0;
	}

    protected boolean isPaymentCardObscure(char[] cardNumber) {

        boolean result = false;
        String obscuredFormat = Constants.OBSCURED_NUMBER_FORMAT;
        if(cardNumber != null){
            for(int i =0; i< obscuredFormat.length() && i < cardNumber.length; i++){
                if(cardNumber[i] == obscuredFormat.charAt(i)){
                    result = true;
                }
                else{
                    result = false;
                    break;
                }
            }
        }
        return result;
    }
}
