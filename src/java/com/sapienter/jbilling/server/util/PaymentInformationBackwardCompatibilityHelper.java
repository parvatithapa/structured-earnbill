package com.sapienter.jbilling.server.util;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;

/**
 * Created by taimoor on 1/25/17.
 *
 * Handles Backwards Conversion for payment information
 * This was required after the change was made to shift payment related Meta-Fields to CHAR from STRING
 */
public class PaymentInformationBackwardCompatibilityHelper {

    /**
     * Contains the Meta-Field types which are shifted to CHAR from STRING
     */
    private static List<MetaFieldType> paymentSecuredFieldTypes = Arrays.asList(
            MetaFieldType.TITLE,
            MetaFieldType.PAYMENT_CARD_NUMBER,
            MetaFieldType.DATE,
            MetaFieldType.GATEWAY_KEY,
            MetaFieldType.BANK_ROUTING_NUMBER,
            MetaFieldType.BANK_ACCOUNT_NUMBER);

    /**
     * Converts the payment information's meta-field from string to char for Credit Card and ACH
     *
     * @param paymentInformationWSList
     */
    public static void convertStringMetaFieldsToChar(List<PaymentInformationWS> paymentInformationWSList) {

        if (paymentInformationWSList == null)
            return;

        for (PaymentInformationWS paymentInformationWS : paymentInformationWSList) {

            if (paymentInformationWS.getMetaFields() == null)
                continue;

            for (MetaFieldValueWS metaFieldValueWS : paymentInformationWS.getMetaFields()) {

                if (StringUtils.isBlank(metaFieldValueWS.getStringValue())) {
                    continue;
                }
                MetaField metaField = MetaFieldExternalHelper.findPaymentMethodMetaField(metaFieldValueWS.getFieldName(),
                        paymentInformationWS.getPaymentMethodTypeId());

                if (isConversionRequired(metaField, metaFieldValueWS)) {
                    metaFieldValueWS.getMetaField().setDataType(DataType.CHAR);
                    metaFieldValueWS.setValue(metaFieldValueWS.getStringValue().toCharArray());
                }
            }
        }
    }

    /**
     * Checks if the string to char conversion required or not based on incoming meta-field value data type
     * and meta-field's fieldUsage
     *
     * @param metaField
     * @param metaFieldValueWS
     * @return
     */
    private static boolean isConversionRequired(MetaField metaField, MetaFieldValueWS metaFieldValueWS) {

        if (DataType.CHAR.equals(metaField.getDataType())
                && paymentSecuredFieldTypes.contains(metaField.getFieldUsage()))
            return true;

        return false;
    }
}
