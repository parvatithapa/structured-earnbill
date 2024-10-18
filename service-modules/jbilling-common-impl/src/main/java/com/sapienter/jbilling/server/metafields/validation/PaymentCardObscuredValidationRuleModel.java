package com.sapienter.jbilling.server.metafields.validation;

import java.util.regex.Pattern;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;
import com.sapienter.jbilling.server.util.Constants;

/**
 * Payment card Obscured validation rule model
 * Verifies that a obscured payment card number is valid
 * 
 * @author Ashish Srivastava
 *
 */
public class PaymentCardObscuredValidationRuleModel extends PaymentCardValidationRuleModel{
    
	
    @Override
    public boolean isPaymentCardObscure(char[] cardNumber) {
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
        if((cardNumber!= null)){
            String ccNumber= new String(cardNumber);
                if(Pattern.matches("[0-9]{6}...[0-9]{3}", ccNumber)){
                    return true;
                }
        }
        return result;
    }
}
