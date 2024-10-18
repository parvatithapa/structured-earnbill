package com.sapienter.jbilling.server.metafields.validation;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.CharBuffer;

/**
 *  Validation using regular expression provided as a parameter
 *
 *  @author Panche Isajeski
 */
public class RegExValidationRuleModel extends AbstractValidationRuleModel {

    public static String VALIDATION_REG_EX_FIELD = "regularExpression";

    private Pattern pattern;
    private Matcher matcher;

    public RegExValidationRuleModel() {

        setAttributeDefinitions(
                new MetaFieldAttributeDefinition(VALIDATION_REG_EX_FIELD, MetaFieldAttributeDefinition.Type.STRING, true)
        );
    }

    @Override
    public ValidationReport doValidation(MetaContent source, Object object, ValidationRule validationRule, Integer languageId) {

        if (!verifyValidationParameters(object, validationRule, languageId)) {
            return null;
        }

        String errorMessage = validationRule.getErrorMessage(languageId);
        ValidationReport report = new ValidationReport();

        pattern = Pattern.compile(getValidationRegExField(validationRule));

        if(object instanceof char[]) {
            matcher = pattern.matcher(CharBuffer.wrap((char[])object));
        }
        else {
            matcher = pattern.matcher(object.toString());
        }

        if (!matcher.matches()) {
            report.addError("MetaFieldValue,value," + errorMessage);
        }

        return report;

    }

    public String getValidationRegExField(ValidationRule validationRule) {
        return validationRule.getRuleAttributes().get(VALIDATION_REG_EX_FIELD);
    }
}
