package com.sapienter.jbilling.server.metafields.validation;

import java.util.Arrays;
import java.util.List;

import static com.sapienter.jbilling.server.metafields.DataType.STRING;
import static com.sapienter.jbilling.server.metafields.DataType.CHAR;
import static com.sapienter.jbilling.server.metafields.DataType.LIST;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaContent;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;

public class UniqueValueValidationRuleModel extends AbstractValidationRuleModel {

    private static final List<DataType> TARGETED_DATA_TYPES = Arrays.asList(STRING, CHAR, LIST);
    
    @Override
    public ValidationReport doValidation(MetaContent source, Object value, Integer languageId, MetaField metaField) {
        
        ValidationRule validationRule = metaField.getValidationRule();
        if (!verifyValidationParameters(value, validationRule, languageId)) {
            return null;
        }
        
        MetaFieldValueDAS valueDAS = new MetaFieldValueDAS();
        MetaFieldValue<?> metaFieldValue = source.getMetaField(metaField.getId());
        DataType dataType = metaFieldValue.getField().getDataType();
        
        if(!TARGETED_DATA_TYPES.contains(dataType)) {
            return null;
        }
        
        if(valueDAS.checkMetaFieldValueExists(metaField.getId(), metaFieldValue)) {
            ValidationReport report = new ValidationReport();
            report.addError(validationRule.getErrorMessage(languageId));
            return report;
        }
        
        return null;
    }

}
