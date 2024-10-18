package com.sapienter.jbilling.server.metafields.validation;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Email validation rule model
 * </p>
 * Validates that a value is an email address
 *
 *  @author Panche Isajeski
 */
public class EmailValidationRuleModel extends AbstractValidationRuleModel {

    public final static String COMMA = ",";
    public final static String SEMI_COLON = ";";

    private Pattern pattern;
    private Matcher matcher;

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-'-\\+]+(\\.[_A-Za-z0-9-'-\\+]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z']{2,})$";

    public EmailValidationRuleModel() {
        pattern = getMatchingPattern();
    }

    @Override
    public ValidationReport doValidation(MetaContent source, Object object, ValidationRule validationRule, Integer languageId) {

        if (!verifyValidationParameters(object, validationRule, languageId)) {
            return null;
        }

        String errorMessage = validationRule.getErrorMessage(languageId);
        ValidationReport report = new ValidationReport();

        String emailString = object.toString();

        if( emailString.contains(EmailValidationRuleModel.COMMA) && emailString.contains(EmailValidationRuleModel.SEMI_COLON) ) {
            report.addError("MetaFieldValue,value," + errorMessage);
        }
        else {
            List<String> emailList = getEmailList(emailString);
            for(String email : emailList) {
                matcher = pattern.matcher(email);
                if (!matcher.matches()) {
                    report.addError("MetaFieldValue,value," + errorMessage);
                }
            }
        }

        return report;
    }

    public static Pattern getMatchingPattern() {
        return Pattern.compile(EMAIL_PATTERN);
    }

    private List<String> getEmailList(String emailString) {
        List<String> emailList = new ArrayList<String>();
        boolean isComma = emailString.contains(EmailValidationRuleModel.COMMA);
        boolean isSemiColon = emailString.contains(EmailValidationRuleModel.SEMI_COLON);
        String character = "";
        if( isComma || isSemiColon ) {
            character = isComma ? EmailValidationRuleModel.COMMA : EmailValidationRuleModel.SEMI_COLON;
        }

        if(!character.isEmpty()) {
            for(String string : emailString.split(character)) {
                if(!string.isEmpty()) {
                    emailList.add(string.trim());
                }
            }
        } else {
            emailList.add(emailString.trim());
        }

        return emailList;
    }
}
