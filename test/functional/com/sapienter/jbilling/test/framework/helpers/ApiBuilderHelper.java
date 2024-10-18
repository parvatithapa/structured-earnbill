package com.sapienter.jbilling.test.framework.helpers;

import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.user.CancellationRequestWS;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Here we place various helper methods
 * used in tests.
 * This class is not designed to be instantiated
 * or sub-classed.
 *
 * @author Vojislav Stanojevikj
 * @since 10-JUN-2016.
 */
public final class ApiBuilderHelper {

    private ApiBuilderHelper(){}


    /**
     * Creates simple {@link MetaFieldValueWS} object
     * with <code>fieldName</code> and <code>value</code>.
     *
     * @param fieldName the name of the meta-field.
     * @param value the value of the meta-field.
     * @return {@link MetaFieldValueWS} object representation.
     */
    public static MetaFieldValueWS getMetaFieldValueWS(String fieldName, Object value) {
        MetaFieldValueWS metaField11 = new MetaFieldValueWS();
        metaField11.setFieldName(fieldName);
        metaField11.setValue(value);
        metaField11.getMetaField().setDataType(getMetaFieldDataType(value));
        return metaField11;
    }

    /**
     * Creates a {@link MetaFieldWS} object
     * with <code>fieldName</code> of <code>type</code>,
     * for <code>entityType</code> and <code>entityId</code>.
     *
     * @param fieldName the name of the field.
     * @param type the type of the field.
     * @param entityType the entity type of the field.
     * @param entityId the owning entity.
     * @return {@link MetaFieldWS} object representation.
     * @see DataType
     * @see EntityType
     */
    public static MetaFieldWS getMetaFieldWS(String fieldName, DataType type,
                                             EntityType entityType, Integer entityId) {
        MetaFieldWS metaField11 = new MetaFieldWS();
        metaField11.setName(fieldName);
        metaField11.setDataType(type);
        metaField11.setEntityId(entityId);
        metaField11.setEntityType(entityType);
        return metaField11;
    }

    public static MetaFieldWS getMetaFieldWithValidationRule(String fieldName, DataType type,
                                             EntityType entityType, Integer entityId,
                                             MetaFieldType metaFieldType, ValidationRuleWS validationRule) {
        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setName(fieldName);
        metaField.setDataType(type);
        metaField.setEntityId(entityId);
        metaField.setEntityType(entityType);
        metaField.setFieldUsage(metaFieldType);
        metaField.setValidationRule(validationRule);
        return metaField;
    }


    private static DataType getMetaFieldDataType(Object value){
        if (value instanceof String) {
            return DataType.STRING;
        } else if (value instanceof Date) {
            return DataType.DATE;
        } else if (value instanceof Boolean) {
            return DataType.BOOLEAN;
        } else if (value instanceof BigDecimal) {
            return DataType.DECIMAL;
        } else if (value instanceof Integer) {
            return DataType.INTEGER;
        } else if (value instanceof List || value instanceof String[]) {
            // store List<String> as String[] for WS-compatible mode, perform manual convertion
            return DataType.LIST;
        } else if (value instanceof char[]) {
            return DataType.CHAR;
        }

        return DataType.STRING;
    }

    public static CancellationRequestWS constructCancellationRequestWS(Date cancellationDate, Integer customerId, String reasonText) {
        CancellationRequestWS cancellationRequestWS = new CancellationRequestWS();
        cancellationRequestWS.setCancellationDate(cancellationDate);
        cancellationRequestWS.setCreateTimestamp(new Date());
        cancellationRequestWS.setCustomerId(customerId);
        cancellationRequestWS.setReasonText(reasonText);
        cancellationRequestWS.setStatus(CancellationRequestStatus.APPLIED);
        return cancellationRequestWS;
    }

    public static Date addDays(Date inputDate, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        cal.add(Calendar.DATE, days);
        return cal.getTime();
    }
}
