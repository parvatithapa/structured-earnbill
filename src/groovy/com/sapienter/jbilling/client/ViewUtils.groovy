/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.client

import org.springframework.context.NoSuchMessageException

import org.hibernate.StaleObjectStateException;
import org.codehaus.groovy.grails.context.support.PluginAwareResourceBundleMessageSource
import com.sapienter.jbilling.common.SessionInternalError;

class ViewUtils {
    // thanks Groovy for adding the setters and getters for me
    PluginAwareResourceBundleMessageSource messageSource;

    /**
     * Trims all string parameters.
     *
     * @param params
     * @return
     */
    def trimParameters(params) {
        params.each{
            if(it.value instanceof String) {
                it.value = it.value.trim()
            }
        }
    }

    /**
     * Will add to flash.errorMessages a list of string with each error message, if any.
     *
     * NOTE: If an error message is to contain commas (","), then they should be replaced by its ASCII character
     * representation, "&#44;" in order to properly work.
     *
     * @param flash
     * @param locale
     * @param exception
     * @return
     * true if there are validation errors, otherwise false
     */
    boolean resolveException(flash, Locale locale, Exception exception,List <String> emptyFields=null) {
        List<String> messages = new ArrayList<String>();
        if (exception instanceof SessionInternalError && exception.getErrorMessages()?.length > 0) {
            int i=0
            List <String> errorLabelList = [];
            for (String message : exception.getErrorMessages()) {
                List<String> fields = message.split(",");
                if (fields.size() <= 2) {
                    fields[1] = fields[1]?.replaceAll("&#44;", ",")
                    List restOfFields = null;
                    if (fields.size() >= 2) {
                        restOfFields = fields[1..fields.size()-1];
                    }
                    String errorMessage
                    try {
                        errorMessage = messageSource.getMessage(fields[0], restOfFields as Object[] , locale);
                    } catch (NoSuchMessageException e) {
                        errorMessage = fields[0]
                    }
                    messages.add errorMessage
                } else {
                    String type = messageSource.getMessage("bean." + fields[0], null, locale);
                    String propertyCode = "bean." + fields[0] + "." + fields[1];
                    String property
                    try {
                        property = messageSource.getMessage(propertyCode, null, locale);
                    } catch (NoSuchMessageException e) {
                        if (propertyCode.startsWith("bean.OrderWS.")) {
                            propertyCode = propertyCode.replaceAll(".parentOrder.", ".")
                            propertyCode = propertyCode.replace(".childOrders.", ".")
                            property = messageSource.getMessage(propertyCode, null, locale);
                        } else {
                            throw e
                        }
                    }
                    List restOfFields = null;
                    if (fields.size() >= 4) {
                        restOfFields = fields[3..fields.size()-1];
                    }
                    String errorMessage
                    fields[2] = fields[2]?.replaceAll("&#44;", ",")
                    try {
                        errorMessage = messageSource.getMessage(fields[2], restOfFields as Object[] , locale);
                    } catch (NoSuchMessageException e) {
                        errorMessage = fields[2]
                    }
                    String finalMessage
                    if (emptyFields){
                        if (emptyFields.getAt(i)){
                            errorLabelList.add(messageSource.getMessage("validation.error.email.preference.${emptyFields.getAt(i)}",
                                    [type, property, errorMessage] as Object[], locale))
                        }
                    }else{
                        try {
                            errorMessage = messageSource.getMessage(fields[2], restOfFields as Object[] , locale);
                        } catch (NoSuchMessageException e) {
                            errorMessage = fields[2]
                        }
                        finalMessage = messageSource.getMessage("validation.message",
                                [type, property, errorMessage] as Object[], locale);
                        finalMessage = type.equals("Meta Field") ? errorMessage : finalMessage
                        messages.add finalMessage;

                        if(fields[0].equals('OrderWS') && fields[1].equals('hierarchy')){
                            restOfFields.each{
                                try {
                                    messages.add(messageSource.getMessage(it, null, locale));
                                } catch (NoSuchMessageException e) {
                                    messages.add(it);
                                }
                            }
                        }
                    }
                }
                i++
            }
            if (emptyFields){
                errorLabelList.sort();
                errorLabelList.each {messages.add(it)}
            }
            flash.errorMessages = messages;
            return true;
        } else if (exception.getCause() instanceof StaleObjectStateException) {
            // this is two people trying to update the same data
            StaleObjectStateException ex = exception.getCause();
            flash.error = messageSource.getMessage("error.dobule_update", null, locale);
        } else {
            // generic error
            flash.error = messageSource.getMessage("error.exception", [exception?.getMessage()] as Object[], locale);
        }

        return false;
    }

	/**
	 * This method was added to address issue #7346.
	 * Earlier, while trying to delete a refunded payment the error message being shown to user was - 'The payment has an error in the payment Id
	 * field: This payment cannot be deleted since it has been refunded'.
	 * The error above is not related to payment id or any particular payment field but to a payment as a whole.
	 * This method takes care of the problem and would return the error message as 'This payment cannot be deleted since it has been refunded'    
	 * @param flash
	 * @param locale
	 * @param exception
	 * @return
	 * true if there are validation errors, otherwise false
	 */
	boolean resolveExceptionMessage(flash, Locale locale, Exception exception) {
		List<String> messages = new ArrayList<String>();
		if (exception instanceof SessionInternalError && exception.getErrorMessages()?.length > 0) {
			int i=0
			for (String message : exception.getErrorMessages()) {
				List<String> fields = message.split(",");
				List restOfFields = null;
				if (fields.size() >= 2) {
					restOfFields = fields[1..fields.size()-1];
				}
				String errorMessage = messageSource.getMessage(fields[0], restOfFields as Object[] , locale);
				messages.add errorMessage;
			}
			i++
		flash.errorMessages = messages;
		return true;
	} else if (exception.getCause() instanceof StaleObjectStateException) {
		// this is two people trying to update the same data
		StaleObjectStateException ex = exception.getCause();
		flash.error = messageSource.getMessage("error.dobule_update", null, locale);
	} else {
		// generic error
		flash.error = messageSource.getMessage("error.exception", [exception.getCause().getMessage()] as Object[], locale);
	}

	return false;
}
    /*
    * When exception throw from plugin then it will wrapped into another object of exception like PluggableTaskException/TaskException
    * This method will loop over the exception and find the root object of exception
    * */

    public static Throwable getRootCause(Exception exception) {
        //if error is object of sessionInternalError and has error fields return to controller
        if (exception instanceof SessionInternalError && exception.getErrorMessages()) {
            return exception
        }
        if (exception.getCause() != null)
            return getRootCause(exception.getCause())

        return null;
    }

}
