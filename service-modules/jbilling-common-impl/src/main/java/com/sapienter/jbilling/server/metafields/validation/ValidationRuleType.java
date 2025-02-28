package com.sapienter.jbilling.server.metafields.validation;

/**
 * Package of available validation rule types
 *
 *  @author Panche Isajeski
 */
public enum ValidationRuleType {

    EMAIL (new EmailValidationRuleModel()),
    RANGE (new RangeValidationRuleModel()),
    REGEX (new RegExValidationRuleModel()),
    SCRIPT (new ScriptValidationRuleModel()),
    PAYMENT_CARD (new PaymentCardValidationRuleModel()),
    PAYMENT_CARD_OBSCURED (new PaymentCardObscuredValidationRuleModel()),
    COUNTRY_CODE(new CountryCodeValidationRuleModel()),
    UNIQUE_VALUE(new UniqueValueValidationRuleModel());
    

    // CUSTOM (new CustomValidationRuleModel());;

    private final ValidationRuleModel validationRuleModel;

    ValidationRuleType(ValidationRuleModel validationRuleModel) {
        this.validationRuleModel = validationRuleModel;
    }

    public ValidationRuleModel getValidationRuleModel() {
        return validationRuleModel;
    }


}