package com.sapienter.jbilling.server.metafields;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CompanyInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CompanyInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CompanyInformationTypeDTO;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.CustomerDTO;

/**
 * Created by marcolin on 29/10/15.
 */
public class MetaFieldExternalHelper {

    /**
     * Remove metafields from the entity with a value of null or ''
     *
     * @param customer
     */
    public static void removeEmptyAitMetaFields(CustomerDTO customer) {
        List<CustomerAccountInfoTypeMetaField> valuesToRemove = new ArrayList<>();

        Set<CustomerAccountInfoTypeMetaField> metaFieldsSet = customer.getCustomerAccountInfoTypeMetaFields();

        for(CustomerAccountInfoTypeMetaField metaField : metaFieldsSet) {
            MetaFieldValue value = metaField.getMetaFieldValue();

            if(value.getValue() == null ||
                    value.getValue().toString().trim().isEmpty()) {
                valuesToRemove.add(metaField);
            }
        }
        metaFieldsSet.removeAll(valuesToRemove);
    }

    /**
     * Remove metafields from the entity with a value of null or ''
     *
     * @param companyDTO
     */
    public static void removeEmptyCitMetaFields(CompanyDTO companyDTO) {
        List<CompanyInfoTypeMetaField> valuesToRemove = new ArrayList<CompanyInfoTypeMetaField>();

        Set<CompanyInfoTypeMetaField> metaFieldsSet = companyDTO.getCompanyInfoTypeMetaFields();

        for(CompanyInfoTypeMetaField metaField : metaFieldsSet) {
            MetaFieldValue value = metaField.getMetaFieldValue();

            if(value.getValue() == null ||
                    value.getValue().toString().trim().isEmpty()) {
                valuesToRemove.add(metaField);
            }
        }
        metaFieldsSet.removeAll(valuesToRemove);
    }

    public static void setAitMetaField(Integer entityId, CustomerDTO entity, Integer groupId, String name, Object value){
        setAitMetaField(entityId, groupId, entity, name, value);
    }

    public static void setCitMetaField(Integer entityId, CompanyDTO entity, Integer groupId, String name, Object value){
        setCitMetaField(entityId, groupId, entity, name, value);
    }

    /**
     * Sets the value of an ait meta field in a map.
     *
     * @param entity	:	 customer entity for search/set fields
     * @param name	:	field name
     * @param value	:	field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    public static void setAitMetaField(Integer entityId, Integer groupId, CustomerDTO entity, String name, Object value) throws IllegalArgumentException {
        EntityType[] types = entity.getCustomizedEntityType();
        if (types == null) {
            throw new IllegalArgumentException("Meta Fields could not be specified for current entity");
        }
        MetaField fieldName = null;
        if(null != groupId){
            fieldName = new MetaFieldDAS().getFieldByNameTypeAndGroup(entityId, types, name, groupId);
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
        entity.setAitMetaField(field, groupId);
    }

    /**
     * Sets the value of an ait meta field in a map.
     *
     * @param entity	:	 company entity for search/set fields
     * @param name	:	field name
     * @param value	:	field value
     * @throws IllegalArgumentException thrown if field name does not exist, or if value is of an incorrect type.
     */
    public static void setCitMetaField(Integer entityId, Integer groupId, CompanyDTO entity, String name, Object value) throws IllegalArgumentException {
        EntityType[] types = entity.getCustomizedEntityType();
        if (types == null) {
            throw new IllegalArgumentException("Meta Fields could not be specified for current entity");
        }
        MetaField fieldName = null;
        if(null != groupId){
            fieldName = new MetaFieldDAS().getFieldByNameTypeAndGroupForCompany(entityId, types, name, groupId);
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
        entity.setCitMetaField(field, groupId);
    }

    public static MetaField findPaymentMethodMetaField(String fieldName, Integer paymentMethodTypeId) {

        for (MetaField field : getPaymentMethodMetaFields(paymentMethodTypeId)) {
            if(field.getName().equals(fieldName)){
                return field;
            }
        }
        return null;
    }

    /**
     * finds payment method level {@link MetaField} by {@link MetaFieldType}
     * @param usage
     * @param paymentMethodTypeId
     * @return
     */
    public static MetaField findPaymentMethodMetaFieldByFieldUsage(MetaFieldType fieldUsage, Integer paymentMethodTypeId) {
        for (MetaField field : getPaymentMethodMetaFields(paymentMethodTypeId)) {
            if(fieldUsage.equals(field.getFieldUsage())){
                return field;
            }
        }
        return null;
    }

    public static Set<MetaField> getPaymentMethodMetaFields(Integer paymentMetohdTypeId) {
        return new PaymentMethodTypeDAS().findNow(paymentMetohdTypeId).getMetaFields();
    }


    public static void updateCustomerMetaFieldsWithValidation(Integer languageId, Integer entityId, CustomerDTO entity, MetaContent dto) {
        EntityType[] types = Arrays.stream(dto.getCustomizedEntityType())
                .filter(type -> !type.equals(EntityType.ACCOUNT_TYPE))
                .toArray(EntityType[]::new);

        List<MetaField> metaFields = new MetaFieldDAS().getAvailableFields(entityId, types, null);
        metaFields.forEach(metaField -> {
            String fieldName = metaField.getName();
            MetaFieldValue newValue = dto.getMetaFieldByEntityType(fieldName, metaField.getEntityType(), entityId);
            if (newValue == null) {
                newValue = dto.getMetaField(metaField.getId());
            }

            //allow to update AIT meta field with empty value.
            entity.setCustomerMetaField(entityId, metaField.getEntityType(), fieldName, newValue != null ? newValue.getValue() : null);
        });

        List<String> errors = new ArrayList<>();
        errors.addAll(validateMetaField(entity.getMetaFields(), languageId, entity));
        MetaFieldHelper.removeEmptyMetaFields(entity);

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Metafield value error", errors.toArray(new String[errors.size()]));
        }
    }

    public static void setCustomerMetaField(Integer entityId, CustomerDTO entity, EntityType entityType, String name, Object value) {
        if (entity == null) {
            throw new IllegalArgumentException("Meta Fields could not be specified for current entity");
        }

        MetaField fieldName = new MetaFieldDAS().getFieldByName(entityId, new EntityType[]{ entityType }, name);
        if (fieldName == null) {
            throw new IllegalArgumentException("Meta Field with name " + name + " was not defined for current entity");
        }

        MetaFieldValue field = fieldName.createValue();
        try {
            field.setValue(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Incorrect type for meta field with name " + name, ex);
        }

        entity.setMetaField(field, null);
    }

    /**
     * Usefull method for updating ait meta fields with validation before entity saving
     *
     * @param entity    target entity
     * @param dto       dto with new data
     */
    public static void updateAitMetaFieldsWithValidation(Integer languageId, Integer entityId, Integer accountTypeId, CustomerDTO entity, MetaContent dto) {
        List<String> errors = new ArrayList<>();
        if (null != accountTypeId) {
            Map<Integer, List<MetaField>> groupMetaFields =
                    getAvailableAccountTypeFieldsMap(accountTypeId);

            for (Map.Entry<Integer, List<MetaField>> entry : groupMetaFields.entrySet()) {
                Integer groupId = entry.getKey();
                List<MetaField> fields = entry.getValue();

                for (MetaField field : fields) {
                    String fieldName = field.getName();
                    MetaFieldValue newValue = dto.getMetaField(fieldName, groupId);
                    CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField = entity.getCustomerAccountInfoTypeMetaField(fieldName, groupId);
                    MetaFieldValue prevValue = null != customerAccountInfoTypeMetaField ? customerAccountInfoTypeMetaField.getMetaFieldValue() : null;
                    if (newValue == null) {
                        newValue = dto.getMetaField(field.getId());
                    }
                    if (prevValue == null) {
                        customerAccountInfoTypeMetaField = entity.getCustomerAccountInfoTypeMetaField(fieldName);
                        prevValue = null != customerAccountInfoTypeMetaField ? customerAccountInfoTypeMetaField.getMetaFieldValue() : null;
                    }
                    if (null != newValue && MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED.equals(newValue.getField().getFieldUsage()) &&
                            DataType.CHAR.equals(newValue.getField().getDataType()) && hasSpecialCharacters(new String((char[])newValue.getValue()))) {
                        if(null != prevValue){
                            entity.setAitMetaField(entityId, groupId, fieldName, prevValue.getValue());
                        } else {
                            errors.add("Meta Field with Name " + fieldName +" is Invalid");
                        }
                    } else {
                        //allow to update AIT meta field with empty value.
                        entity.setAitMetaField(entityId, groupId, fieldName, newValue != null ? newValue.getValue() : null);
                    }

                }
            }
        }

        for (Map.Entry<Integer, List<MetaFieldValue>> entry : entity.getAitMetaFieldMap().entrySet()) {
            errors.addAll(validateMetaField(entry.getValue(), languageId, entity));
        }

        removeEmptyAitMetaFields(entity);
        if (!errors.isEmpty()) {
            throw new SessionInternalError("Metafield value error", errors.toArray(new String[errors.size()]));
        }
    }

    /**
     * Usefull method for updating ait meta fields with validation before entity saving
     *
     * @param entity    target entity
     * @param dto       dto with new data
     */
    public static void updateCitMetaFieldsWithValidation(Integer languageId, Integer entityId, Integer companyId, CompanyDTO entity, MetaContent dto) {
        if (null != companyId) {
            Map<Integer, List<MetaField>> groupMetaFields =
                    getAvailableCompanyFieldsMap(companyId);

            for (Map.Entry<Integer, List<MetaField>> entry : groupMetaFields.entrySet()) {
                Integer groupId = entry.getKey();
                List<MetaField> fields = entry.getValue();

                for (MetaField field : fields) {
                    String fieldName = field.getName();
                    MetaFieldValue newValue = dto.getMetaField(fieldName, groupId);
                    if (newValue == null) {
                        newValue = dto.getMetaField(field.getId());
                    }
                    //allow to update AIT meta field with empty value.
                    entity.setCitMetaField(entityId, groupId, fieldName, newValue != null ? newValue.getValue() : null);

                }
            }
        }

        List<String> errors = new ArrayList<>();
        for (Map.Entry<Integer, List<MetaFieldValue>> entry : entity.getCitMetaFieldMap().entrySet()) {
            errors.addAll(validateMetaField(entry.getValue(), languageId, entity));
        }

        removeEmptyCitMetaFields(entity);
        if (!errors.isEmpty()) {
            throw new SessionInternalError("Metafield value error", errors.toArray(new String[errors.size()]));
        }
    }


    public static Map<Integer, List<MetaField>> getAvailableAccountTypeFieldsMap(Integer accountTypeId){
        Map<Integer, List<MetaField>> metaFields = new HashMap<>(1);
        AccountInformationTypeDAS aitDAS = new AccountInformationTypeDAS();
        for(AccountInformationTypeDTO ait : aitDAS.getInformationTypesForAccountType(accountTypeId)){
            metaFields.put(ait.getId(), new LinkedList(ait.getMetaFields()));
        }
        return metaFields;
    }

    public static Map<Integer, List<MetaField>> getAvailableCompanyFieldsMap(Integer companyId){
        Map<Integer, List<MetaField>> metaFields = new HashMap<Integer, List<MetaField>>(1);
        CompanyInformationTypeDAS citDAS = new CompanyInformationTypeDAS();
        for(CompanyInformationTypeDTO cit : citDAS.getInformationTypesForCompany(companyId)){
            metaFields.put(cit.getId(), new LinkedList(cit.getMetaFields()));
        }
        return metaFields;
    }

    /**
     * Usefull method for updating meta fields with validation before entity saving
     * @param entity    target entity
     * @param dto       dto with new data
     */
    public static void updatePaymentMethodMetaFieldsWithValidation(Integer languageId, Integer entityId, Integer paymentMethodTypeId,
            PaymentInformationDTO entity, MetaContent dto) {
        List<String> errors = new ArrayList<>();
        for (MetaField field : getPaymentMethodMetaFields(paymentMethodTypeId)) {
            String fieldName = field.getName();
            MetaFieldValue newValue = dto.getMetaField(fieldName, null);
            MetaFieldValue prevValue = entity.getMetaField(fieldName, null);
            if (newValue == null) {
                newValue = dto.getMetaField(field.getId());
            }
            if(newValue == null){
                newValue = entity.getMetaField(field.getId());
            }
            if(prevValue == null){
                prevValue = entity.getMetaField(field.getId());
            }
            if (null != newValue && MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED.equals(newValue.getField().getFieldUsage()) &&
                    DataType.CHAR.equals(newValue.getField().getDataType()) && hasSpecialCharacters(new String((char[])newValue.getValue()))) {
                if(null!=prevValue && hasSpecialCharacters(new String((char[])prevValue.getValue()))){
                    PaymentInformationBL paymentInformationBL = new PaymentInformationBL(entity.getId());
                    PaymentInformationDTO paymentInformationDTO = paymentInformationBL.get();
                    prevValue = null != paymentInformationDTO ? paymentInformationDTO.getMetaField(field.getId()) : null;
                }
                if(null != prevValue){
                    entity.setMetaField(entityId, null, fieldName, prevValue.getValue());
                } else {
                    errors.add("Payment Method Meta Field with Name " + fieldName +" is Invalid");
                }
            } else {
                entity.setMetaField(entityId, null, fieldName,
                        newValue != null ? newValue.getValue() : null);
            }
        }

        // Updating and validating of ait meta fields is done in a separate method
        errors = validateMetaField(entity.getMetaFields(), languageId, entity);
        if (!errors.isEmpty()) {
            throw new SessionInternalError("Metafield value error", errors.toArray(new String[errors.size()]));
        }
    }

    private static List<String> validateMetaField(List<MetaFieldValue> metaFieldValues, Integer languageId, MetaContent entity){
        List<String> errors = new ArrayList<>();
        for (MetaFieldValue value : metaFieldValues) {
            try {
                MetaFieldBL.validateMetaField(languageId, value.getField(), value, entity);
            } catch (SessionInternalError sie){
                errors.add(sie.getErrorMessages()[0]);
            } catch (Exception e) {
                throw new SessionInternalError("Unexpected error occurred validating the metafield");
            }
        }

        return errors;
    }

    public static boolean hasSpecialCharacters(String value){
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(value);
        return m.find();
    }
}
