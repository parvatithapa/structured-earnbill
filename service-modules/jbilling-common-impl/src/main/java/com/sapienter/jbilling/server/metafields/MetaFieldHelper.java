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

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;

/**
 * Helper class for working with custom fields. It is needed because some classes
 * cann't extends CustomizedEntity directly. Instead they can implement MetaContent interface
 * and use this helper to do work.
 *
 * @author Alexander Aksenov
 * @since 11.10.11
 */
public class MetaFieldHelper {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String MF_TYPE_MISMATCH_ERROR = "Invalid value [%s] for MetaField [%s] Passed In Request.";

    private MetaFieldHelper() {
        throw new IllegalStateException("Non instantiable class");
    }

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param customizedEntity entity for searching fields
     * @param name             meta field name
     * @return field if found, null if not set.
     */
    public static MetaFieldValue getMetaField(MetaContent customizedEntity, String name) {
        return MetaFieldHelper.getMetaField(customizedEntity, name, null);
    }

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param customizedEntity entity for searching fields
     * @param name             meta field name
     * @param groupId          group id
     * @return field if found, null if not set.
     */
    public static MetaFieldValue getMetaField(MetaContent customizedEntity, String name, Integer groupId) {
        return MetaFieldHelper.getMetaField(customizedEntity, name, groupId, null);
    }

    /**
     * Returns the meta field by name if it's been defined for this object.
     *
     * @param customizedEntity entity for searching fields
     * @param name             meta field name
     * @param groupId          group id
     * @param companyId        Company id to which meta field belongs
     * @return field if found, null if not set.
     */
    public static MetaFieldValue getMetaField(MetaContent customizedEntity, String name, Integer groupId, Integer companyId) {
        for (MetaFieldValue value : customizedEntity.getMetaFields()) {
            if (value.getField() != null && value.getField().getName().equals(name)) {
                if (companyId != null && !value.getField().getEntityId().equals(companyId)) {
                    continue;
                }
                if (null != groupId) {
                    if(null != value.getField().getMetaFieldGroups()){
                        for (MetaFieldGroup group : value.getField().getMetaFieldGroups()) {
                            if (group.getId() == groupId) {
                                return value;
                            }
                        }
                    }
                } else {
                    return value;
                }
            }
        }
        return null;
    }

    public static MetaFieldValue getMetaField(MetaContent customizedEntity, String name, EntityType entityType, Integer companyId) {
        return customizedEntity.getMetaFields()
                .stream()
                .filter(value -> value.getField() != null &&
                value.getField().getName().equals(name) &&
                value.getField().getEntityId().equals(companyId) &&
                value.getField().getEntityType().equals(entityType))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a meta field to this object. If there is already a field associated with
     * this object then the existing value should be updated.
     *
     * @param customizedEntity entity for searching fields
     * @param field            field to update.
     */
    public static void setMetaField(MetaContent customizedEntity, MetaFieldValue field, Integer groupId) {
        MetaFieldValue oldValue = customizedEntity.getMetaField(field.getField().getName(), groupId, field.getField().getEntityId());
        if (oldValue != null) {
            customizedEntity.getMetaFields().remove(oldValue);
        }

        customizedEntity.getMetaFields().add(field);
    }

    public static void setMetaField(Integer entityId, MetaContent customizedEntity, String name, Object value){
        MetaFieldHelper.setMetaField(entityId, null, customizedEntity, name, value);
    }

    public static void setMetaField(Integer entityId, MetaContent customizedEntity, Integer groupId, String name, Object value){
        MetaFieldHelper.setMetaField(entityId, groupId, customizedEntity, name, value);
    }


    /**
     * Sets the value of an ait meta field that is already associated with this object for a given date. If
     * the field does not already exist, or if the value class is of an incorrect type
     * then an IllegalArgumentException will be thrown.
     *
     * @param customizedEntity entity for search/set fields
     * @param name field name
     * @param value	field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    public static void setMetaField(Integer entityId, Integer groupId, MetaContent customizedEntity, String name, Object value) throws IllegalArgumentException {
        MetaFieldValue fieldValue = customizedEntity.getMetaField(name, groupId, entityId);
        if (fieldValue != null) { // common case during editing
            try {
                fieldValue.setValue(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Incorrect type for meta field with name " + name, ex);
            }
        } else {
            EntityType[] types = customizedEntity.getCustomizedEntityType();
            if (types == null) {
                throw new IllegalArgumentException("Meta Fields could not be specified for current entity");
            }
            MetaField fieldName = null;
            if(null != groupId){
                fieldName = new MetaFieldDAS().getFieldByNameTypeAndGroup(entityId, types, name, groupId);
            } else if (ArrayUtils.contains(types, EntityType.PAYMENT_METHOD_TYPE)) {
                //TODO MODULARIZATION: UNDERSTAND HERE IF WE CAN AVOID USE THE PaymentInformationDTO
                fieldName = customizedEntity.fieldNameRetrievalFunction(customizedEntity, name);
            } else {
                fieldName = new MetaFieldDAS().getFieldByName(entityId, types, name);
            }
            if (fieldName == null) {
                throw new IllegalArgumentException("Meta Field with name " + name + " was not defined for current entity");
            }
            MetaFieldValue field = fieldName.createValue();
            try {
                field.setValue(value);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Incorrect type for meta field with name " + name, ex);
            }
            customizedEntity.setMetaField(field, groupId);
        }
    }



    /**
     * Usefull method for updating meta fields with validation before entity saving
     * @param entity    target entity
     * @param dto       dto with new data
     */
    public static void updateMetaFieldsWithValidation(Integer languageId, Integer entityId, Integer accountTypeId, MetaContent entity, MetaContent dto, Boolean global) {
        List<EntityType> entityTypes = new LinkedList(Arrays.asList(entity.getCustomizedEntityType()));
        if(entityTypes.contains(EntityType.ACCOUNT_TYPE)){
            entityTypes.remove(EntityType.ACCOUNT_TYPE);
        }

        Map<String, MetaField> availableMetaFields =
                MetaFieldBL.getAvailableFields(entityId, entityTypes.toArray(new EntityType[entityTypes.size()]));

        for (String fieldName : availableMetaFields.keySet()) {
            MetaFieldValue newValue = dto.getMetaField(fieldName, null, entityId);
            MetaFieldValue prevValue = entity.getMetaField(fieldName, null, entityId);
            if (newValue == null) { // try to search by id, may be temp fix
                MetaField metaFieldName = availableMetaFields.get(fieldName);
                newValue = dto.getMetaField(metaFieldName.getId());
            }
            // TODO: (VCA) - we want the null values for the validation
            // if ( null != newValue && null != newValue.getValue() ) {

            if(newValue != null){
                entity.setMetaField(entityId, null, fieldName, newValue.getValue());
            }else if((global != null) && global.equals(Boolean.TRUE) && (prevValue != null)){
                /*
                 * if user edits a global category and retains its global scope
                 * then don't filter out null meta-fields and retain previous values
                 * */
                entity.setMetaField(entityId, null, fieldName, prevValue.getValue());
            }else{
                /*
                 * if user edits a global category and marks it as non-global only then filter out null meta-fields
                 * */
                entity.setMetaField(entityId, null, fieldName, null);
            }
            // } //else {
            //no point creating null/empty-value records in db
            //}
        }

        // Updating and validating of ait meta fields is done in a separate method
        List<String> errors = new ArrayList<>();
        for (MetaFieldValue value : entity.getMetaFields()) {
            try{
                MetaFieldBL.validateMetaField(languageId, value.getField(), value, entity);
            } catch (SessionInternalError sie){
                errors.add(sie.getErrorMessages()[0]);
            } catch (Exception e) {
                throw new SessionInternalError("Unexpected error occurred validating the metafield");
            }
        }

        removeEmptyMetaFields(entity);
        if (!errors.isEmpty()) {
            throw new SessionInternalError("Metafield value error", errors.toArray(new String[errors.size()]));
        }
    }

    public static void updateMetaFieldsWithValidation(Integer languageId, Integer entityId, Integer accountTypeId, MetaContent entity, MetaContent dto) {
        updateMetaFieldsWithValidation(languageId, entityId, accountTypeId, entity, dto, null);
    }

    /**
     * Update MetaFieldValues in entity with the values in dto. Only values of MetaFields in {@code metaFieldCollection}
     * will be updated
     *
     * @param metaFieldCollection   meta fields that will be updated
     * @param entity                destination object
     * @param dto                   source object
     */
    public static void updateMetaFieldsWithValidation(Integer languageId, Collection<MetaField> metaFieldCollection, MetaContent entity, MetaContent dto) {
        Map<String, MetaField> metaFields = new LinkedHashMap<String, MetaField>();
        for (MetaField field : metaFieldCollection) {
            metaFields.put(field.getName(), field);
        }

        //loop through all the meta fields
        for (String fieldName : metaFields.keySet()) {
            //get the new value
            MetaFieldValue newValue = dto.getMetaField(fieldName);
            if (newValue == null) { // try to search by id, may be temp fix
                MetaField metaFieldName = metaFields.get(fieldName);
                newValue = dto.getMetaField(metaFieldName.getId());
            }

            //create a new value and set it to the default if it exists
            if(newValue == null) {
                MetaField metaField = metaFields.get(fieldName);
                newValue = metaField.createValue();
                if(metaField.getDefaultValue() != null) {
                    newValue.setValue(metaField.getDefaultValue().getValue());
                }
            }

            if(newValue != null) {
                entity.setMetaField(newValue, null);
            }
        }

        //do validation
        for (MetaFieldValue value : entity.getMetaFields()) {
            MetaFieldBL.validateMetaField(languageId, value.getField(), value, entity);
        }

        removeEmptyMetaFields(entity);
    }

    /**
     * Remove metafields from the entity with a value of null or ''
     *
     * @param entity
     */
    public static void removeEmptyMetaFields(MetaContent entity) {
        List<MetaFieldValue> metaFields = entity.getMetaFields();
        List<MetaFieldValue> valuesToRemove = new ArrayList<MetaFieldValue>(metaFields.size());

        for(MetaFieldValue mfValue : metaFields) {
            Object value = mfValue.getValue();
            if(value == null ||
                    value.toString().trim().isEmpty()) {
                valuesToRemove.add(mfValue);
            }
        }

        metaFields.removeAll(valuesToRemove);
    }



    /**
     * Create missing MetaFieldValues in entity from the values in {@code metaFieldCollection}.
     * Then do validation.
     *
     * @param metaFieldCollection   meta fields that will be updated
     * @param entity                destination object
     */
    public static void updateMetaFieldDefaultValuesWithValidation(Integer languageId, Collection<MetaField> metaFieldCollection, MetaContent entity) {
        Map<String, MetaField> metaFields = new LinkedHashMap<String, MetaField>();
        for (MetaField field : metaFieldCollection) {
            metaFields.put(field.getName(), field);
        }

        //loop through all the meta fields
        for (String fieldName : metaFields.keySet()) {
            //get the value
            MetaFieldValue value = entity.getMetaField(fieldName);
            if (value == null) { // try to search by id, may be temp fix
                MetaField metaFieldName = metaFields.get(fieldName);
                value = entity.getMetaField(metaFieldName.getId());
            }

            //create a new value and set it to the default if it exists
            if(value == null) {
                MetaField metaField = metaFields.get(fieldName);
                value = metaField.createValue();
                if(metaField.getDefaultValue() != null) {
                    value.setValue(metaField.getDefaultValue().getValue());
                }
                entity.setMetaField(value, null);
            }
        }

        //do validation
        for (MetaFieldValue value : entity.getMetaFields()) {
            MetaFieldBL.validateMetaField(languageId, value.getField(), value, entity);
        }
        removeEmptyMetaFields(entity);
    }

    public static MetaFieldValue getMetaField(MetaContent customizedEntity, Integer metaFieldNameId) {
        for (MetaFieldValue value : customizedEntity.getMetaFields()) {
            if (value.getField() != null && value.getField().getId()==metaFieldNameId){
                return value;
            }
        }
        return null;
    }

    /**
     * Creates a copy of {@code source}. If {@code clearId} is true the MetaFieldValueWS.id field will
     * be set to 0.
     *
     * @param source
     * @param clearId
     * @return
     */
    public static MetaFieldValueWS[] copy(MetaFieldValueWS[] source, boolean clearId) {
        if(source == null) {
            return new MetaFieldValueWS[0];
        }

        MetaFieldValueWS[] copy = Arrays.copyOf(source, source.length);
        if(clearId) {
            for(MetaFieldValueWS ws: copy) {
                ws.setId(0);
            }
        }
        return copy;
    }

    /**
     * Convert a collection of MetaFieldValues to MetaFieldValueWS[]
     *
     * @param metaFieldValues
     * @return
     */
    public static MetaFieldValueWS[] toWSArray(Collection<MetaFieldValue> metaFieldValues) {
        if(metaFieldValues == null) {
            return new MetaFieldValueWS[0];
        }

        MetaFieldValueWS[] result = new MetaFieldValueWS[metaFieldValues.size()];
        int idx = 0;
        for(MetaFieldValue mf : metaFieldValues) {
            result[idx++] = MetaFieldBL.getWS(mf);
        }
        return result;
    }

    /**
     * Comparator for sorting meta field values after retrieving from DB
     */
    public final static class MetaFieldValuesOrderComparator implements Comparator<MetaFieldValue> {
        @Override
        public int compare(MetaFieldValue o1, MetaFieldValue o2) {
            if (o1.getField().getDisplayOrder() == null && o2.getField().getDisplayOrder() == null) {
                return 0;
            }
            if (o1.getField().getDisplayOrder() != null) {
                return o1.getField().getDisplayOrder().compareTo(o2.getField().getDisplayOrder());
            } else {
                return -1 * o2.getField().getDisplayOrder().compareTo(o1.getField().getDisplayOrder());
            }
        }
    }

    /**
     * Set the values of meta fields (as specified by {@code metaFieldNames}) on {@code entity} with values
     * found in {@code metaFields}.
     *
     * @param metaFieldNames    These MetaFields will get their values set
     * @param entity
     * @param metaFields        New values for MetaFields
     */
    public static void fillMetaFieldsFromWS(Set<MetaField> metaFieldNames, CustomizedEntity entity, MetaFieldValueWS[] metaFields) {
        Map<String, MetaField> metaFieldMap = new HashMap<String, MetaField>(metaFieldNames.size() * 2);
        if (null != metaFieldNames && !metaFieldNames.isEmpty()) {
            for(MetaField metaField : metaFieldNames) {
                Integer groupId = MetaFieldBL.getGroupIdByCompanyInfo(metaField);
                if (null != groupId) {
                    metaFieldMap.put(metaField.getName().concat("_").concat(groupId.toString()), metaField);
                } else {
                    metaFieldMap.put(metaField.getName(), metaField);
                }
            }

            if (metaFields != null && metaFields.length > 0) {
                for (MetaFieldValueWS fieldValue : metaFields) {
                    MetaField metaField;
                    if (null != fieldValue.getGroupId()) {
                        metaField = metaFieldMap.get(fieldValue.getFieldName().concat("_").concat(fieldValue.getGroupId().toString()));
                    } else {
                        metaField = metaFieldMap.get(fieldValue.getFieldName());
                    }
                    if(metaField == null) {
                        throw new SessionInternalError("MetaField ["+fieldValue.getFieldName()+"] does not exist for entity "+entity);
                    }
                    if (null != fieldValue.getGroupId()) {
                        entity.setMetaField(metaField, fieldValue.getValue(), fieldValue.getGroupId());
                    } else {
                        entity.setMetaField(metaField, fieldValue.getValue());
                    }
                }
            }
        }
    }

    /**
     * Checks if the given value is of the given type
     * @param value
     * @param requiredType
     * @return
     */
    public static boolean isValueOfType(MetaFieldValue value, MetaFieldType requiredType){
        if(value.getField()!=null && value.getField().getFieldUsage()!=null){
            if(value.getField().getFieldUsage().equals(requiredType)){
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given value is of the given type
     * @param value
     * @param requiredType
     * @return
     */
    public static boolean isValueOfType(Integer id, MetaFieldType requiredType){
        MetaField field = MetaFieldBL.getMetaField(id);
        if(null!=field && field.getFieldUsage()!=null){
            return field.getFieldUsage().equals(requiredType);
        }
        return false;
    }

    public static Optional<MetaFieldValue<?>> getMetaFieldByType(MetaContent customizedEntity, MetaFieldType type) {
        MetaFieldValue<?> value = null;
        for(MetaFieldValue<?> metaFieldValue : customizedEntity.getMetaFields()) {
            if(metaFieldValue.getField().getFieldUsage().equals(type)) {
                value = metaFieldValue;
                break;
            }
        }
        return Optional.ofNullable(value);
    }

    public static Map<String, Object> convertMetaFieldValuesToMap(Integer entityId, Integer groupId, EntityType type, MetaContent source) {
        Map<String, Object> metaFieldNameAndValueMap = new HashMap<>();
        for(Entry<String, MetaField> metaFieldEntry : MetaFieldBL.getAvailableFields(entityId, new EntityType[] {type}).entrySet()) {
            MetaFieldValue<?> value = source.getMetaField(metaFieldEntry.getKey(), groupId);
            metaFieldNameAndValueMap.put(metaFieldEntry.getKey(), value!=null ? value.getValue(): null);
        }
        return metaFieldNameAndValueMap;
    }

    /**
     * Converts String value to Targeted {@link DataType} of {@link MetaField}
     * @param metaField
     * @param value
     * @param errors
     * @return
     */
    private static Object convertToStringValueMetaFieldDataType(MetaField metaField, String value, List<String> errors) {
        Object obj = null;
        if(StringUtils.isEmpty(value)) {
            return null;
        }
        try {
            if(metaField.getDataType().equals(DataType.INTEGER)) {
                obj = Integer.parseInt(value);
                logger.debug("converted value {} to integer data type", value);
            } else if(metaField.getDataType().equals(DataType.CHAR)) {
                obj = value.toCharArray();
                logger.debug("converted value {} to char data type", value);
            } else if(metaField.getDataType().equals(DataType.BOOLEAN)) {
                value = value.toLowerCase();
                if(value.equals("true") || value.endsWith("t")) {
                    obj = Boolean.TRUE;
                    logger.debug("converted value {} to boolean data type", value);
                } else if(value.equals("false") || value.endsWith("f")) {
                    obj = Boolean.FALSE;
                    logger.debug("converted value {} to boolean data type", value);
                } else {
                    errors.add(String.format(MF_TYPE_MISMATCH_ERROR, value, metaField.getName()));
                }
            } else if(metaField.getDataType().equals(DataType.DECIMAL)) {
                obj = new BigDecimal(value);
                logger.debug("converted value {} to Decimal data type", value);
            } else if(metaField.getDataType().equals(DataType.DATE)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                obj = dateFormat.parse(value);
                logger.debug("converted value {} to date data type", value);
            } else if(metaField.getDataType().equals(DataType.STRING) ||
                    metaField.getDataType().equals(DataType.ENUMERATION) ||
                    metaField.getDataType().equals(DataType.JSON_OBJECT)) {
                obj = value;
                logger.debug("converted value {} to string data type", value);
            } else {
                errors.add("DataType "+ metaField.getDataType().name() + " not supported by api.");
            }
            return obj;
        } catch(Exception ex) {
            logger.error("Value conversion failed", ex);
            errors.add(String.format(MF_TYPE_MISMATCH_ERROR, value, metaField.getName()));
            return obj;
        }
    }

    /**
     * Converts given MetaField name and value map to {@link MetaFieldValueWS}
     * @param entityId
     * @param type
     * @param metaFieldNameAndValueMap
     * @return
     */
    public static MetaFieldValueWS[] convertAndValidateMFNameAndValueMapToMetaFieldValueWS(Integer entityId, EntityType type,
            Map<String, String> metaFieldNameAndValueMap) {
        Assert.notNull(entityId, "Plesase Enter entityId");
        Assert.notNull(type, "Plesase Enter type");
        Assert.notNull(metaFieldNameAndValueMap, "Plesase Enter metaFieldNameAndValueMap");
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        Map<String, MetaField> metaFieldMap = MetaFieldBL.getAvailableFields(entityId, new EntityType[] { type });
        validateMetaFields(metaFieldMap, metaFieldNameAndValueMap.keySet(), type);
        List<String> errors = new ArrayList<>();
        for(Entry<String, String> metaFieldNameAndValue : metaFieldNameAndValueMap.entrySet()) {
            MetaField metaField = metaFieldMap.get(metaFieldNameAndValue.getKey());
            Object value = convertToStringValueMetaFieldDataType(metaField, metaFieldNameAndValue.getValue(), errors);
            metaFieldValues.add(new MetaFieldValueWS(metaField.getName(), null, metaField.getDataType(), metaField.isMandatory(), value));
        }
        if(!errors.isEmpty()) {
            logger.error("Invalid metafields passed {}", errors);
            throw new SessionInternalError("Invalid MetaFieldValues Passed ", errors.toArray(new String[0]), HttpStatus.SC_BAD_REQUEST);
        }
        return metaFieldValues.toArray(new MetaFieldValueWS[0]);
    }


    /**
     * validates given meta field values
     * @param metaFieldMap
     * @param metaFieldNames
     */
    private static void validateMetaFields(Map<String, MetaField> metaFieldMap, Set<String> metaFieldNames, EntityType type) {
        List<String> errors = new ArrayList<>();
        for(String metaFieldName : metaFieldNames) {
            if(!metaFieldMap.containsKey(metaFieldName)) {
                errors.add(metaFieldName + " not found on " + type.name().toLowerCase() +" level metafield");
            }
        }
        if(!errors.isEmpty()) {
            logger.error("invalid metafields passed {}", errors);
            throw new SessionInternalError("Invalid MetaFieldValues passed ", errors.toArray(new String[0]), HttpStatus.SC_BAD_REQUEST);
        }
    }

    /**
     * Validates given meta field values
     * @param entityType
     * @param metaFieldValues
     * @param languageId
     * @param entityId
     * @return
     */
    public static Set<MetaField> validateAndGetMetaFields(EntityType entityType, MetaFieldValueWS[] metaFieldValues, Integer languageId, Integer entityId) {
        List<String> errors = new ArrayList<>();
        Map<String, MetaField> metaFieldNameMap = MetaFieldBL.getAvailableFields(entityId, new EntityType[] { entityType });
        logger.debug("available {} meta fields on {} level for entity {}", metaFieldNameMap, entityType, entityId);
        Set<MetaField> metaFields = new HashSet<>();
        for(MetaFieldValueWS metaFieldValueWS : metaFieldValues) {
            if(!metaFieldNameMap.containsKey(metaFieldValueWS.getFieldName())) {
                errors.add(metaFieldValueWS.getFieldName() + " not found on " + entityType.name() +  " level metafield");
            }
            metaFields.add(metaFieldNameMap.get(metaFieldValueWS.getFieldName()));
        }
        if(!errors.isEmpty()) {
            logger.error("invalid metafields passed {}", errors);
            throw new SessionInternalError("Invalid metaFieldValues Passed! ", errors.toArray(new String[0]), HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
        MetaFieldBL.validateMetaFields(languageId, metaFields, metaFieldValues);
        return metaFields;
    }
}
