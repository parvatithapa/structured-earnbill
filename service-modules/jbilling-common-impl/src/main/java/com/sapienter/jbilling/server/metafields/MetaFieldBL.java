/*
 jBilling - The Enterprise Open Source Billing System
 Copyright (C) 2003-2011 Enterprise jBilling Software Ltd. and Emiliano Conde

 This file is part of jbilling.

 jbilling is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 jbilling is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with jbilling.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sapienter.jbilling.server.metafields;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.format.DateTimeFormat;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.metafields.db.ValidationRule;
import com.sapienter.jbilling.server.metafields.db.ValidationRuleDAS;
import com.sapienter.jbilling.server.metafields.db.value.BooleanMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.CharMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.DateMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.DecimalMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.JsonMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.ListMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.metafields.validation.MetaFieldAttributeDefinition;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleModel;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.DescriptionBL;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.ParseHelper;
import com.sapienter.jbilling.server.util.db.EnumerationDAS;
import com.sapienter.jbilling.server.util.db.EnumerationDTO;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDAS;
import com.sapienter.jbilling.server.util.db.JbillingTable;
import com.sapienter.jbilling.server.util.db.JbillingTableDAS;
import com.sapienter.jbilling.server.util.db.LanguageDAS;
import com.sapienter.jbilling.server.util.db.LanguageDTO;


/**
 * Business Logic for meta-fields.
 *
 * @author Brian Cowdery
 * @since 03-Oct-2011
 */
public class MetaFieldBL {

    public static MetaField getFieldByName(Integer entityId, EntityType[] entityType, String name) {
        return new MetaFieldDAS().getFieldByName(entityId, entityType, name);
    }

    /**
     * Returns a map of MetaField's for the given entity type keyed by the field
     * name's plain text name. Basically a list of name-value-pair names with the original
     * MetaField object to be used when building new fields.
     *
     * @param entityType entity type to query
     * @return map with available fields
     */
    public static Map<String, MetaField> getAvailableFields(Integer entityId, EntityType[] entityType) {
        List<MetaField> entityFields = new MetaFieldDAS().getAvailableFields(entityId, entityType, true);
        Map<String, MetaField> result = new LinkedHashMap<>();
        if (entityFields != null) {
            for (MetaField field : entityFields) {
                result.put(field.getName(), field);
            }
        }
        return result;
    }

    public static List<MetaField> getAvailableFieldsList(Integer entityId, EntityType[] entityType) {
        return new MetaFieldDAS().getAvailableFields(entityId, entityType, true);
    }

    public static List<MetaField> getAllAvailableFieldsList(Integer entityId, EntityType[] entityType) {
        return new MetaFieldDAS().getAvailableFields(entityId, entityType, null);
    }


    public static void validateMetaFields(Integer entityId, Integer languageId, EntityType type, MetaFieldValueWS[] metaFields) {
        MetaFieldBL.validateMetaFields(entityId, languageId, new EntityType[]{type}, metaFields);
    }

    public static void validateMetaFields(Integer entityId, Integer languageId, EntityType[] type, MetaFieldValueWS[] metaFields) {
        Collection<MetaField> metaFieldsCollection =  new MetaFieldDAS().getAvailableFields(entityId,type, true);
        validateMetaFields(languageId, metaFieldsCollection, metaFields);
    }

    public static void validateMetaFields(Integer languageId, Collection<MetaField> metaFieldsCollection, MetaFieldValueWS[] metaFields) {
        for (MetaField field : metaFieldsCollection) {
            MetaFieldValue value = field.createValue();
            if (metaFields != null) {
                for (MetaFieldValueWS valueWS : metaFields) {
                    if (field.getName().equals(valueWS.getFieldName())) {
                        value.setValue(valueWS.getValue());
                        break;
                    }
                }
            }
            // TODO (pai) validate metafields with source context !
            validateMetaField(languageId, field, value, null);
        }
    }

    /**
     * Validates all meta fields, configured for entity
     *
     * @param customizedEntity entity with meta fields for validation
     */
    public static synchronized void validateMetaFields(Integer entityId, Integer languageId, MetaContent customizedEntity) {
        List<MetaField> availableMetaFields = getAvailableFieldsList(entityId, customizedEntity.getCustomizedEntityType());
        for (MetaField field : availableMetaFields) {
            MetaFieldValue value = customizedEntity.getMetaField(field.getName());
            MetaFieldBL.validateMetaField(languageId, field, value, customizedEntity);
        }
    }

    /**
     * Validate the meta fields values as specified by {@code availableMetaFields} of {@code customizedEntity}.
     *
     * @param availableMetaFields
     * @param customizedEntity
     */
    public static void validateMetaFields(Integer languageId, Collection<MetaField> availableMetaFields, MetaContent customizedEntity) {
        for (MetaField field : availableMetaFields) {
            MetaFieldValue value = customizedEntity.getMetaField(field.getName());
            MetaFieldBL.validateMetaField(languageId, field, value, customizedEntity);
        }
    }

    public static void validateMetaField(Integer languageId, MetaField field, MetaFieldValue value, MetaContent source) {

        if (field.isDisabled()) {
            return;
        }
        String fieldName = field.getName();
        if (field.isMandatory() && value == null) {
            String error = "MetaFieldValue,value,value.cannot.be.null," + fieldName;
            throw new SessionInternalError("Field value failed validation.", new String[]{ error });
        }

        if (field.isMandatory() && value != null && value.isEmpty()) {
            throw new SessionInternalError("Metafield value must be specified",
                    new String[] {"MetaFieldValue,value,metafield.validation.value.unspecified," + fieldName});
        }
        Integer entityId = field.getEntityId();
        // enumeration validation check.
        if(field.getDataType().equals(DataType.ENUMERATION) && null!= value && !value.isEmpty()) {
            EnumerationDTO enumerationDTO = new EnumerationDAS().getEnumerationByName(fieldName, entityId);
            if(null == enumerationDTO) {
                throw new SessionInternalError("Invalid enumeration field",
                        new String[] {fieldName + " enumeration not found on entity "+ entityId});
            }
            Object metaFieldValue = value.getValue();
            if(null!= metaFieldValue) {
                if(!(metaFieldValue instanceof String)) {
                    throw new SessionInternalError("Invalid enumeration field data type",
                            new String[] { "enumeration field data type must be String " });
                }
                if(!enumerationDTO.isValuePresent((String) metaFieldValue)) {
                    throw new SessionInternalError("Invalid enumeration value passed",
                            new String[] { "enumeration value "+ metaFieldValue + " not defined in enumeration " + fieldName });
                }
            }

        }
        String result = field.getFieldUsage() != null && value != null && value.getValue() != null ?
                field.getFieldUsage().validate(value.getValue().toString()) : null;
                if(result != null){
                    throw new SessionInternalError("MetafieldType failed validation",
                            new String[] {"MetaFieldValue,value,"+String.format(result,fieldName)});
                }

                if (value != null) {
                    value.validate(languageId, source);
                }
    }

    /**
     * Convert only the MetaFields which belongs to the entity. Missing or empty MetaFields will not be added.
     *
     * @param entity
     * @return
     */
    public static MetaFieldValueWS[] convertMetaFieldsToWS(MetaContent entity) {
        List<MetaFieldValue> metaFieldValues = entity.getMetaFields();
        MetaFieldValueWS[] result = new MetaFieldValueWS[metaFieldValues.size()];

        int idx = 0;
        for(MetaFieldValue metaFieldValue : metaFieldValues) {
            MetaFieldValueWS metaFieldValueWS = getWS(metaFieldValue);
            result[idx++] = metaFieldValueWS;
        }

        return result;
    }

    public static MetaFieldValueWS[] convertMetaFieldsToWS(Integer entityId, MetaContent entity) {
        return MetaFieldBL.convertMetaFieldsToWS(entityId, entity, false);
    }

    public static MetaFieldValueWS[] convertMetaFieldsToWS(Integer entityId, MetaContent entity, boolean allMetaFields) {
        // If this is a customer we retrieve only meta fields of type CUSTOMER to avoid duplicates from all the available
        // Account Types. Redmine Issue #6458
        EntityType[] types;
        if (entity.getClass().getName().indexOf("CustomerDTO") >= 0) {
            types = new EntityType[]{EntityType.CUSTOMER};
        } else {
            types = entity.getCustomizedEntityType();
        }

        List<MetaField> availableMetaFields = new MetaFieldDAS().getAvailableFields(
                entityId, types, allMetaFields ? null : true);

        MetaFieldValueWS[] result = new MetaFieldValueWS[]{};
        if (availableMetaFields != null && !availableMetaFields.isEmpty()) {
            result = new MetaFieldValueWS[availableMetaFields.size()];
            int i = 0;
            for (MetaField field : availableMetaFields) {
                Integer groupId = getGroupId(field);
                MetaFieldValue value = entity.getMetaField(field.getName(), groupId, entityId);
                if (value == null) {
                    value = field.createValue();
                }
                MetaFieldValueWS metaFieldValueWS = getWS(value, groupId);
                result[i++] = metaFieldValueWS;
            }
        }
        return result;
    }


    private static Integer getGroupId(MetaField field) {
        Set<MetaFieldGroup> groups = field.getMetaFieldGroups();
        if (null != groups) {
            for (MetaFieldGroup group : groups) {
                if (group.getEntityType().equals(EntityType.ACCOUNT_TYPE)) {
                    return group.getId();
                }
            }
        }
        return null;
    }

    public static Integer getGroupIdByCompanyInfo(MetaField field) {
        Set<MetaFieldGroup> groups = field.getMetaFieldGroups();
        if (null != groups) {
            for (MetaFieldGroup group : groups) {
                if (group.getEntityType().equals(EntityType.COMPANY_INFO)) {
                    return group.getId();
                }
            }
        }
        return null;
    }

    /**
     * Convert the list of metafields (specified by {@code availableMetaFields} ) of {@code entity} to
     * MetaFieldsWS objects.
     *
     * @param availableMetaFields   meta fields to extract.
     * @param entity
     * @return
     */
    public static MetaFieldValueWS[] convertMetaFieldsToWS(Collection<MetaField> availableMetaFields, MetaContent entity) {

        MetaFieldValueWS[] result = new MetaFieldValueWS[]{};
        if (availableMetaFields != null && !availableMetaFields.isEmpty()) {
            result = new MetaFieldValueWS[availableMetaFields.size()];
            int i = 0;
            for (MetaField field : availableMetaFields) {
                MetaFieldValue value = entity.getMetaField(field.getName());
                if (value == null) {
                    value = field.createValue();
                }
                result[i++] = getWS(value);
            }
        }
        return result;
    }

    public static final MetaFieldWS getWS(MetaField dto) {

        MetaFieldWS ws = new MetaFieldWS();
        ws.setDataType(dto.getDataType());
        if(dto.getDefaultValue()!=null){
            ws.setDefaultValue(getWS(dto.getDefaultValue()));
        }
        ws.setDisabled(dto.isDisabled());
        ws.setDisplayOrder(dto.getDisplayOrder());
        ws.setEntityId(dto.getEntityId());
        ws.setEntityType(dto.getEntityType());
        ws.setFieldUsage(dto.getFieldUsage());
        ws.setId(dto.getId());
        ws.setMandatory(dto.isMandatory());
        ws.setName(dto.getName());
        if (dto.getPrimary()!=null) {
            ws.setPrimary(dto.getPrimary());
        }
        if (dto.getValidationRule()!=null) {
            ws.setValidationRule(MetaFieldBL.getValidationRuleWS(dto.getValidationRule()));
        }
        ws.setFilename(dto.getFilename());
        ws.setDataTableId(dto.getDataTableId());
        ws.setHelpContentURL(dto.getHelpContentURL());
        ws.setHelpDescription(dto.getHelpDescription());

        List<Integer> dependentMetaFieldIds = new ArrayList<>();
        for (MetaField dependencyMetaField : dto.getDependentMetaFields()) {
            dependentMetaFieldIds.add(dependencyMetaField.getId());
        }
        ws.setDependentMetaFields(dependentMetaFieldIds.toArray(new Integer[dependentMetaFieldIds.size()]));

        return ws;
    }

    public static final MetaField getDTO(MetaFieldWS ws,Integer entityId) {
        MetaField dto = new MetaField();
        dto.setDataType(ws.getDataType());
        dto.setId(ws.getId());

        if(ws.getDefaultValue()!=null){
            MetaFieldValue mfValue=dto.createValue();
            mfValue.setValue(ws.getDefaultValue().getValue());
            dto.setDefaultValue(mfValue);

        }
        dto.setDisabled(ws.isDisabled());
        dto.setDisplayOrder(ws.getDisplayOrder());
        dto.setEntityType(ws.getEntityType());
        dto.setEntityId(entityId);

        if (ws.getFieldUsage() != null) {
            dto.setFieldUsage( ws.getFieldUsage() );
        }

        dto.setMandatory(ws.isMandatory());
        dto.setName(ws.getName());
        dto.setPrimary(ws.isPrimary());
        dto.setValidationRule(ws.getValidationRule() == null ? null : MetaFieldBL.getValidationRuleDTO(ws.getValidationRule()));
        dto.setFilename(ws.getFilename());
        dto.setDataTableId(ws.getDataTableId());
        dto.setHelpContentURL(ws.getHelpContentURL());
        dto.setHelpDescription(ws.getHelpDescription());

        return dto;
    }

    public static final  MetaFieldValueWS createValue(MetaFieldWS ws,String value) {

        MetaFieldValue metaFieldValue = createValueFromDataType(null, value,ws.getDataType());
        if (metaFieldValue != null) {
            return getWS(metaFieldValue);
        }

        return null;
    }


    public static final MetaFieldValue createValueFromDataType(MetaField metaField, Object value,DataType dataType) {

        try {

            switch (dataType) {
            case STRING:
            case STATIC_TEXT:
            case TEXT_AREA:
            case ENUMERATION:
            case SCRIPT:
                StringMetaFieldValue stringMetaFieldValue = new StringMetaFieldValue(metaField);
                if (value != null) {
                    stringMetaFieldValue.setValue(value.toString());
                }
                return stringMetaFieldValue;

            case INTEGER:
                IntegerMetaFieldValue integerMetaFieldValue = new IntegerMetaFieldValue(metaField);
                if (value != null) {
                    integerMetaFieldValue.setValue(Integer.valueOf(value.toString()));
                }
                return integerMetaFieldValue;

            case DECIMAL:
                DecimalMetaFieldValue decimalMetaFieldValue = new DecimalMetaFieldValue(metaField);
                if (value != null) {
                    decimalMetaFieldValue.setValue(new BigDecimal(value.toString()));
                }
                return decimalMetaFieldValue;

            case BOOLEAN:
                BooleanMetaFieldValue booleanMetaFieldValue = new BooleanMetaFieldValue(metaField);
                if (value != null) {
                    booleanMetaFieldValue.setValue(Boolean.valueOf(value.toString()));
                }
                return booleanMetaFieldValue;

            case DATE:
                DateMetaFieldValue dateMetaFieldValue = new DateMetaFieldValue(metaField);
                if (value != null) {
                    dateMetaFieldValue.setValue(DateTimeFormat.fullDate().parseDateTime(value.toString()).toDate());
                }
                return dateMetaFieldValue;

            case JSON_OBJECT:
                JsonMetaFieldValue jsonMetaFieldValue = new JsonMetaFieldValue(metaField);
                if (value != null) {
                    jsonMetaFieldValue.setValue(value.toString());
                }
                return jsonMetaFieldValue;

            case LIST:
                ListMetaFieldValue listMetaFieldValue = new ListMetaFieldValue(metaField);
                if (value != null) {
                    listMetaFieldValue.setValue(Arrays.asList(new String[] {value.toString()}));
                }
                return listMetaFieldValue;

            case CHAR:
                CharMetaFieldValue charMetaFieldValue = new CharMetaFieldValue(metaField);
                if (value != null) {
                    charMetaFieldValue.setValue((char[]) value);
                }
                return charMetaFieldValue;
            }

        } catch (Exception e) {
            // cant create a value//
            return null;
        }
        return null;
    }

    public static MetaFieldValueWS getWS(@SuppressWarnings("rawtypes") MetaFieldValue metaFieldValue) {
        MetaFieldValueWS ws= new MetaFieldValueWS();
        if (metaFieldValue.getField() != null) {
            ws.setFieldName( metaFieldValue.getField().getName() );
            ws.getMetaField().setDisabled ( metaFieldValue.getField().isDisabled() );
            ws.getMetaField().setMandatory(metaFieldValue.getField().isMandatory() );
            ws.getMetaField().setDataType( metaFieldValue.getField().getDataType() );
            ws.getMetaField().setDisplayOrder( metaFieldValue.getField().getDisplayOrder() );
            ws.getMetaField().setFieldUsage( metaFieldValue.getField().getFieldUsage() );
            ws.setDefaultValue(metaFieldValue.getField().getDefaultValue() != null ? metaFieldValue.getField().getDefaultValue().getValue() : null);
            ws.getMetaField().setEntityId(metaFieldValue.getField().getEntityId());
        }

        if (null!=metaFieldValue.getField() && null != metaFieldValue.getField().getMetaFieldGroups()) {
            for (MetaFieldGroup group : metaFieldValue.getField().getMetaFieldGroups()) {
                if (group.getEntityType().equals(EntityType.COMPANY_INFO)) {
                    ws.setGroupId(group.getId());
                }
            }
        }

        ws.setId(metaFieldValue.getId());
        ws.setValue(metaFieldValue.getValue());
        return ws;
    }

    public static MetaFieldValueWS getWS(MetaFieldValue metaFieldValue, Integer groupId){
        MetaFieldValueWS ws = getWS(metaFieldValue);
        ws.setGroupId(groupId);
        return ws;
    }

    public static synchronized void fillMetaFieldsFromWS(Integer entityId, MetaContent entity, MetaFieldValueWS[] metaFields) {
        if (metaFields != null) {
            for (MetaFieldValueWS fieldValue : metaFields) {
                entity.setMetaField(entityId, fieldValue.getGroupId(), fieldValue.getFieldName(), fieldValue.getValue());
            }
        }
    }

    public static final ValidationRuleWS getValidationRuleWS(ValidationRule dto){
        ValidationRuleWS ws =new ValidationRuleWS();
        if(null != dto){
            ws.setId(dto.getId());
            ws.setRuleType(dto.getRuleType().name());
            ws.setRuleAttributes(new TreeMap<String, String>(dto.getRuleAttributes()));
            ws.setEnabled(dto.isEnabled());

            List<LanguageDTO> languages = new LanguageDAS().findAll();
            List<InternationalDescriptionWS> errors = new ArrayList<>(1);

            for (LanguageDTO language : languages) {
                try {

                    if (dto.getDescriptionDTO(language.getId(), ValidationRule.ERROR_MSG_LABEL) != null) {
                        errors.add(DescriptionBL.getInternationalDescriptionWS(dto.getDescriptionDTO(language.getId(),
                                ValidationRule.ERROR_MSG_LABEL)));
                    }
                } catch (Exception e) {
                    throw new SessionInternalError("Validation error message cannot be resolved for language: " + language);
                }
            }
            ws.setErrorMessages(errors);
        }
        return ws;
    }

    public static final ValidationRule getValidationRuleDTO(ValidationRuleWS ws){
        if (null != ws) {

            ValidationRule rule = new ValidationRule();
            rule.setId(ws.getId());
            rule.setRuleType(ValidationRuleType.valueOf(ws.getRuleType()));
            rule.setRuleAttributes(ws.getRuleAttributes());
            if (ws.getErrorMessages() != null) {
                for (InternationalDescriptionWS desc : ws.getErrorMessages()) {
                    rule.addError(desc.getLanguageId(), desc.getContent());
                }
            }
            return rule;
        }
        return null;
    }

    /**
     * Convert a collection of MetaField objects to MetaFieldWS[]
     *
     * @param metaFieldsCollection
     * @return
     */
    public static MetaFieldWS[] convertMetaFieldsToWS(Collection<MetaField> metaFieldsCollection) {
        MetaFieldWS[] metaFields = new MetaFieldWS[metaFieldsCollection.size()];
        int idx = 0;
        for(MetaField metaField : metaFieldsCollection) {
            metaFields[idx++] = MetaFieldBL.getWS(metaField);
        }
        return metaFields;
    }

    /**
     * Convert a Collection of MetaFieldWS objects into a Set of MetaField objects.
     *
     * @param metaFields
     * @return
     */
    public static Set<MetaField> convertMetaFieldsToDTO(Collection<MetaFieldWS> metaFields, Integer entityId) {
        Set<MetaField> dtoList = new HashSet<MetaField>(metaFields.size() * 2);

        if (metaFields != null) {
            for (MetaFieldWS metaField : metaFields) {
                MetaField mf = getDTO(metaField,entityId);
                dtoList.add(mf);
            }
        }
        return dtoList;
    }

    /**
     * Check if there are any MetaFieldValue objects linked to the MetaField for the given EntityType
     *
     * @param entityType
     * @param metaFieldId
     * @return
     */
    public static boolean isMetaFieldUsed(EntityType entityType, Integer metaFieldId) {
        return new MetaFieldDAS().countMetaFieldValuesForEntity(entityType, metaFieldId) != 0;
    }

    public MetaField create(MetaField dto) {

        MetaField metaField = new MetaField();
        metaField.setEntityId(dto.getEntityId());
        metaField.setEntityType(dto.getEntityType());
        metaField.setDataType(dto.getDataType());
        metaField.setName(dto.getName());
        metaField.setDisplayOrder(dto.getDisplayOrder());
        metaField.setMandatory(dto.isMandatory());
        metaField.setDisabled(dto.isDisabled());
        metaField.setPrimary(dto.getPrimary());
        metaField.setFieldUsage(dto.getFieldUsage());
        metaField.setFilename(dto.getFilename());
        metaField.setDataTableId(dto.getDataTableId());
        metaField.setHelpContentURL(dto.getHelpContentURL());
        metaField.setHelpDescription(dto.getHelpDescription());
        // validate enum meta field
        if(DataType.ENUMERATION.equals(dto.getDataType())) {
            EnumerationDTO enumeration = new EnumerationDAS().getEnumerationByName(metaField.getName(), dto.getEntityId());
            if(null == enumeration) {
                throw new SessionInternalError("Invalid enumeration field",
                        new String[] {metaField.getName() + " enumeration not found on entity "+ dto.getEntityId()});
            }
        }
        ValidationRule validationRule;
        if(dto.getValidationRule()!=null){
            if (dto.getValidationRule().getId() != 0){
                validationRule = new ValidationRuleDAS().find(dto.getValidationRule().getId());
            } else {
                validationRule = dto.getValidationRule();
            }
            validateAttributes(new ArrayList<>(Arrays.asList(validationRule)));
            metaField.setValidationRule(validationRule);
        }

        List<LanguageDTO> languages = new LanguageDAS().findAll();
        for (LanguageDTO language : languages) {

            if(dto.getDescription(language.getId())!=null){
                metaField.setDescription( dto.getDescription(language.getId()), language.getId());
            }
        }
        MetaFieldDAS das = new MetaFieldDAS();
        metaField = das.save(metaField);

        // save validation rule error messages after the meta field saving
        if (dto.getValidationRule() != null) {
            for (Map.Entry<Integer, String> entry : dto.getValidationRule().getErrors().entrySet()) {
                metaField.getValidationRule().setErrorMessage(entry.getKey(), entry.getValue());
            }
        }

        if (dto.getDefaultValue() != null) {
            MetaFieldValueDAS valueDAS = new MetaFieldValueDAS();
            MetaFieldValue value = dto.getDefaultValue();
            value.setField(metaField);
            valueDAS.save(value);//does not cascade towards MetaField

            metaField.setDefaultValue(value);
            das.save(metaField);//fire an update
        }

        return metaField;
    }

    public void update(MetaField dto) {
        Integer unUsedValidationRuleId = 0;
        Map<Integer, String> unUsedValidationErrorIds = new HashMap<Integer, String>();
        boolean removeUnused = false;

        MetaFieldDAS das = new MetaFieldDAS();
        MetaField metaField = das.find(dto.getId());
        metaField.setName(dto.getName());
        metaField.setDisplayOrder(dto.getDisplayOrder());
        metaField.setMandatory(dto.isMandatory());
        metaField.setDisabled(dto.isDisabled());
        metaField.setPrimary(dto.getPrimary());
        metaField.setFieldUsage(dto.getFieldUsage());
        metaField.setFilename(dto.getFilename());
        metaField.setDataType(dto.getDataType());
        metaField.setDataTableId(dto.getDataTableId());
        metaField.setHelpContentURL(dto.getHelpContentURL());
        metaField.setHelpDescription(dto.getHelpDescription());

        if (metaField.getDefaultValue() != null && dto.getDefaultValue() == null) {
            metaField.getDefaultValue().setValue(null);
        } else if (dto.getDefaultValue() != null && metaField.getDefaultValue() == null) {
            MetaFieldValue value = metaField.createValue();
            value.setValue(dto.getDefaultValue().getValue());
            metaField.setDefaultValue(value);
        } else if (metaField.getDefaultValue() != null) {
            metaField.getDefaultValue().setValue(dto.getDefaultValue().getValue());
        }

        if (dto.getValidationRule() != null) {
            validateAttributes(new ArrayList<ValidationRule>(Arrays.asList(dto.getValidationRule())));
            if (metaField.getValidationRule() == null) {
                metaField.setValidationRule(dto.getValidationRule());
            } else {
                metaField.getValidationRule().setRuleType(dto.getValidationRule().getRuleType());
                metaField.getValidationRule().setRuleAttributes(dto.getValidationRule().getRuleAttributes());
            }
        }

        List<LanguageDTO> languages = new LanguageDAS().findAll();
        for (LanguageDTO language : languages) {

            if(dto.getDescription(language.getId())!=null){
                metaField.setDescription( dto.getDescription(language.getId()), language.getId());
            }
        }

        das.save(metaField);

        // update validation rule error messages after the meta field update
        if (dto.getValidationRule() != null) {
            for (Map.Entry<Integer, String> entry : dto.getValidationRule().getErrors().entrySet()) {
                metaField.getValidationRule().setErrorMessage(entry.getKey(), entry.getValue());
            }
        }

        // check if the rule has been removed
        if (metaField.getValidationRule() != null) {
            unUsedValidationRuleId = metaField.getValidationRule().getId();
            for (LanguageDTO language : languages) {
                if (metaField.getValidationRule().getErrorMessage(language.getId()) != null) {
                    if (dto.getValidationRule() == null) {
                        unUsedValidationErrorIds.put(language.getId(),
                                metaField.getValidationRule().getErrorMessage(language.getId()));
                    } else if (dto.getValidationRule().getErrors().get(language.getId()) == null) {
                        unUsedValidationErrorIds.put(language.getId(),
                                metaField.getValidationRule().getErrorMessage(language.getId()));
                    }
                }
            }
            if(dto.getValidationRule() == null) {
                ValidationRule rule = metaField.getValidationRule();
                rule.getErrors().clear();
                metaField.setValidationRule(null);
            }
            removeUnused = true;
        }


        //delete un-used links
        if(removeUnused && unUsedValidationRuleId!=0){
            ValidationRuleDAS vrDas = new ValidationRuleDAS();
            ValidationRule unUsedVr = vrDas.find(unUsedValidationRuleId);
            if(unUsedVr!=null && dto.getValidationRule() == null) {
                vrDas.delete(unUsedVr);
            }

            InternationalDescriptionDAS idDas = InternationalDescriptionDAS.getInstance();
            JbillingTableDAS tableDas = Context.getBean(Context.Name.JBILLING_TABLE_DAS);
            JbillingTable table = tableDas.findByName(Constants.TABLE_VALIDATION_RULE);

            for (Map.Entry<Integer, String> entry1 : unUsedValidationErrorIds.entrySet()) {
                idDas.delete(table.getId(),unUsedValidationRuleId,ValidationRule.ERROR_MSG_LABEL,entry1.getKey());
            }
        }
    }

    public void deleteIfNotParticipant(int metaFieldId) {
        MetaFieldDAS das = new MetaFieldDAS();
        MetaField metaField = das.find(metaFieldId);
        if (metaField.getMetaFieldGroups() != null && metaField.getMetaFieldGroups().size() > 0) {
            String error = "MetaFieldValue,value,metafield.validation.inuse," + metaField.getId();
            throw new SessionInternalError(String.format("MetaField is in use in groups: %s .",
                    Arrays.toString(metaField.getMetaFieldGroups().toArray())), new String[]{ error });

        }
        delete(metaFieldId);
    }

    public void delete(int metaFieldId) {
        MetaFieldDAS das = new MetaFieldDAS();
        MetaField metaField = das.find(metaFieldId);
        if (metaField.getDefaultValue() != null) {
            metaField.setDefaultValue(null);
            das.save(metaField);
            das.flush();
        }
        das.deleteMetaFieldValuesForEntity(metaField.getEntityType(), metaFieldId);
        das.flush();
        das.clear();

        das.delete(metaField);

        das.flush();
        das.clear();
    }


    public static MetaField getMetaField(Integer metafieldId) {
        MetaFieldDAS metaFieldDas = new MetaFieldDAS();
        return  metaFieldDas.find(metafieldId);

    }

    public static void validateAttributes(Collection<ValidationRule> models) throws SessionInternalError {
        List<String> errors = new ArrayList<>();

        for (ValidationRule model : models) {
            try {
                validateRuleAttributes(model.getRuleAttributes(), model.getRuleType().getValidationRuleModel());
            } catch (SessionInternalError e) {
                errors.addAll(Arrays.asList(e.getErrorMessages()));
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Validation rule attributes failed validation.",
                    errors.toArray(new String[errors.size()]));
        }
    }

    /**
     * Validates that all the required attributes of the given validation rule that are present and of the
     * correct type.
     *
     * @param attributes attribute map
     * @param strategy   strategy to validate against
     * @throws SessionInternalError if attributes are missing or of an incorrect type
     */
    public static void validateRuleAttributes(Map<String, String> attributes, ValidationRuleModel strategy)
            throws SessionInternalError {
        String strategyName = strategy.getClass().getSimpleName();
        List<String> errors = new ArrayList<String>();

        Iterator iterator = strategy.getAttributeDefinitions().iterator();
        MetaFieldAttributeDefinition definition;
        String name, value;
        while (iterator.hasNext()){
            definition = (MetaFieldAttributeDefinition) iterator.next();
            name = definition.getName();
            value = attributes.get(name);

            // validate required attributes
            if (definition.isRequired() && (value == null || value.trim().equals(""))) {
                errors.add(strategyName + "," + name + ",validation.error.is.required");
            } else {
                // validate attribute types
                try {
                    switch (definition.getType()) {
                    case STRING:
                        // a string is a string...
                        break;
                    case TIME:
                        ParseHelper.parseTime(value);
                        break;
                    case INTEGER:
                        ParseHelper.parseInteger(value);
                        break;
                    case DECIMAL:
                        ParseHelper.parseDecimal(value);
                        break;
                    }
                } catch (SessionInternalError validationException) {
                    errors.add(strategyName + "," + name + "," + validationException.getErrorMessages()[0]);
                }
            }
        }

        // throw new validation exception with complete error list
        if (!errors.isEmpty()) {
            throw new SessionInternalError(strategyName + " attributes failed validation.",
                    errors.toArray(new String[errors.size()]));
        }
    }

    public static void validateMetaFieldsChanges(Collection<MetaField> newMetaFields, Collection<MetaField> currentMetaFields) throws SessionInternalError {
        Map<Integer, MetaField> currentMetaFieldMap = new HashMap<>();
        Set<String> names = new HashSet<String>();

        //collect the current meta fields
        for(MetaField dto : currentMetaFields) {
            currentMetaFieldMap.put(dto.getId(), dto);
        }

        //loop through the new metaFields
        for(MetaField metaField : newMetaFields) {
            if(names.contains(metaField.getName())) {
                String [] errors = new String[] {"MetaFieldWS,name,metaField.validation.name.unique," + metaField.getName()};
                throw new SessionInternalError("Meta field names must be unique ["+metaField.getName()+"]", errors);
            }
            names.add(metaField.getName());

            //if it is already in the DB validate the changes
            if(metaField.getId() > 0) {
                MetaField currentMetaField = currentMetaFieldMap.get(metaField.getId());

                //if the type change we have to make sure it is not already used
                boolean checkUsage = !currentMetaField.getDataType().equals(metaField.getDataType());
                if(checkUsage && MetaFieldBL.isMetaFieldUsed(EntityType.ORDER_LINE, metaField.getId())) {
                    String [] errors = new String[] {"MetaFieldWS,dataType,metaField.validation.type.change.not.allowed"};
                    throw new SessionInternalError("Data Type may not be changes is meta field is used ["+metaField.getName()+"]", errors);
                }
            }
        }
    }

    /**
     * Save new metafields, update existed meta fields, return ID of meta fields to remove
     * @param newMetaFields collection of entered metafileds
     * @param currentMetaFields collection of current metafileds, will be updated after return
     * @return collection of IDs of metafields to be removed
     */
    public static Collection<Integer> updateMetaFieldsCollection(Collection<MetaField> newMetaFields, Collection<MetaField> currentMetaFields) {
        Map<Integer, MetaField> currentMetaFieldMap = new HashMap<>();

        //collect the current meta fields
        for(MetaField dto : currentMetaFields) {
            currentMetaFieldMap.put(dto.getId(), dto);
        }
        // clear current collection for filling later
        currentMetaFields.clear();

        MetaFieldBL metaFieldBL = new MetaFieldBL();
        //loop through the new metaFields
        for(MetaField metaField : newMetaFields) {
            //if it is a saved status update the current object
            if(metaField.getId() > 0) {
                MetaField persistedMetaField = currentMetaFieldMap.remove(metaField.getId());
                mergeBasicProperties(persistedMetaField, metaField);

                metaFieldBL.update(persistedMetaField);
                currentMetaFields.add(persistedMetaField);
            } else {
                //else it is a new meta field and we must create it
                currentMetaFields.add(metaFieldBL.create(metaField));
            }
        }
        return currentMetaFieldMap.keySet();
    }

    /**
     * Merge properties from dto metafield to persisted one
     * @param destination persisted metafield
     * @param source dto metafield with updated properties
     */
    private static void mergeBasicProperties(MetaField destination, MetaField source) {
        destination.setName(source.getName());
        destination.setPrimary(source.getPrimary());
        destination.setValidationRule(source.getValidationRule());
        destination.setDataType(source.getDataType());
        destination.setDefaultValue(source.getDefaultValue());
        destination.setDisabled(source.isDisabled());
        destination.setMandatory(source.isMandatory());
        destination.setDisplayOrder(source.getDisplayOrder());
        destination.setFieldUsage(source.getFieldUsage());
    }


    public static List<MetaField> getMetaFields(Collection<Integer> entityIds, EntityType type){
        List<MetaField> availableFields = new ArrayList<>();

        for(Integer entityId : entityIds) {
            if(type != null){
                availableFields.addAll(MetaFieldBL.getAvailableFieldsList(entityId, new EntityType[]{type}));
            }else{
                availableFields.addAll(MetaFieldBL.getAvailableFieldsList(entityId, new EntityType[]{EntityType.PRODUCT}));
            }
        }

        return availableFields;
    }

    public static Object getMetaFieldValueNullSafety(MetaFieldValue mf) {
        return mf == null ? null : mf.getValue();
    }

    public static Integer getMetaFieldIntegerValueNullSafety(MetaFieldValue mf) {
        return mf == null ? null : Integer.valueOf((String) mf.getValue());
    }

    public static Map<String, String> getMetaFieldsMap(MetaFieldValueWS[] metaFieldValueWSs) {
        Map<String, String> metaFieldMap = new HashMap<>();
        if(ArrayUtils.isNotEmpty(metaFieldValueWSs)){
            for(MetaFieldValueWS metafieldWS : metaFieldValueWSs) {
                metaFieldMap.put(metafieldWS.getFieldName(), String.valueOf(metafieldWS.getValue()));
            }
        }
        return metaFieldMap;
    }

    public static Map<String, Object> getFieldsMap(MetaFieldValueWS[] metaFieldValueWSs) {
        Map<String, Object> metaFieldMap = new HashMap<>();
        if(ArrayUtils.isNotEmpty(metaFieldValueWSs)){
            for(MetaFieldValueWS metafieldWS : metaFieldValueWSs) {
                metaFieldMap.put(metafieldWS.getFieldName(), metafieldWS.getValue());
            }
        }
        return metaFieldMap;
    }
}
