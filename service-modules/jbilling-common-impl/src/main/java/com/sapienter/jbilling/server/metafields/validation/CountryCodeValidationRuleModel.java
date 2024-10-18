package com.sapienter.jbilling.server.metafields.validation;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;

/**
 * 
 * @author krunal bhavsar
 *
 */
public class CountryCodeValidationRuleModel extends AbstractValidationRuleModel<String> {

	private static List<String> countryCode = Arrays.asList(Locale.getISOCountries());
	
	public ValidationReport doValidation(MetaContent source, String strvalue,
			ValidationRule validationRule, Integer languageId) {
		
		  if (!verifyValidationParameters(strvalue, validationRule, languageId)) {
	            return null;
		  }
		  ValidationReport report = new ValidationReport();
		  if(!countryCode.contains(strvalue)) {
			  report.addError(validationRule.getErrorMessage(languageId));
			  return report;
		  }
		return null;
	}
	
	public static String validateCoutryCode(String value) {
		return (value.isEmpty() || countryCode.contains(value)) ? null : "validation.error.contact.countryCode.pattern";
	}

}
