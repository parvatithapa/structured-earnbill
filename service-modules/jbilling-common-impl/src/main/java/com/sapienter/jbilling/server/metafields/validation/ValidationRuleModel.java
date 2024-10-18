package com.sapienter.jbilling.server.metafields.validation;

import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;

import java.util.List;

import org.springframework.util.Assert;

/**
 *  Defines models for validation rules;
 *  Performs validation on entities
 *
 *  @author Panche Isajeski
 */
public interface ValidationRuleModel<T> {

    List<MetaFieldAttributeDefinition> getAttributeDefinitions();

    default ValidationReport doValidation(MetaContent source, T object, ValidationRule validationRule, Integer languageId) {
        return null;
    }

    default ValidationReport doValidation(MetaContent source, T object, Integer languageId, MetaField metaField) {
        Assert.notNull(metaField, "MetaField Paramter can not be null!");
        return this.doValidation(source, object, metaField.getValidationRule(), languageId);
    }

}
