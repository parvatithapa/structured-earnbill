package com.sapienter.jbilling.server.metafields.validation;

import com.sapienter.jbilling.server.metafields.db.ValidationRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *  Abstraction of the validation rule model
 *  </p>
 *  Provides attributes to be used by the validation rule models
 *
 *  @author Panche Isajeski
 */
public abstract class AbstractValidationRuleModel<T> implements ValidationRuleModel<T> {

    private List<MetaFieldAttributeDefinition> attributeDefinitions = Collections.emptyList();

    @Override
    public List<MetaFieldAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    public void setAttributeDefinitions(MetaFieldAttributeDefinition ...attributeDefinitions) {
        this.attributeDefinitions = Collections.unmodifiableList(Arrays.asList(attributeDefinitions));
    }

    public boolean verifyValidationParameters(Object object, ValidationRule validationRule, Integer languageId) {

        if (null == object) {
            return false;
        } else if( object instanceof String && ((String) object).trim().isEmpty()){
            return false;
        } else if( object instanceof char[] && ((char[]) object).length == 0){
            return  false;
        }
        if (null == validationRule) {
            return false;
        }

        String errorMessage = validationRule.getErrorMessage(languageId);

        if (null == errorMessage || errorMessage.isEmpty()) {
            return false;
        }

        return true;
    }

}
