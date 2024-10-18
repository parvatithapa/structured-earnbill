/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.nges.export.batch.processor;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.nges.export.row.ExportRow;
import com.sapienter.jbilling.server.user.UserWS;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Created by hitesh on 11/8/16.
 */
public abstract class AbstractNGESExportProcessor implements ItemProcessor<Integer, ExportRow>, StepExecutionListener {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AbstractNGESExportProcessor.class));

    private static final String DATE_FORMATE = "YYYYMMdd";
    private static final String SIMPLE_DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSSSSSSSS";

    @Override
    public void beforeStep(StepExecution stepExecution) {
        LOG.debug("beforeStep");
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        LOG.debug("afterStep");
        return null;
    }

    /**
     * This method used for search metaFieldValue in given list wit or without groupId.
     *
     * @param metaFieldName
     * @param groupId
     * @param metaFields
     * @return Object
     */
    public Object getMetaFieldValue(String metaFieldName, Integer groupId, MetaFieldValueWS[] metaFields) {
        for (MetaFieldValueWS metaFieldValueWS : metaFields) {
            if (metaFieldValueWS.getFieldName().equals(metaFieldName) && (groupId != null ? metaFieldValueWS.getGroupId().equals(groupId) : true)) {
                return metaFieldValueWS.getValue();
            }
        }
        return null;
    }

    /**
     * This method used for return the string value of object with validation.
     *
     * @param fieldName
     * @param value
     * @param mandatoryCheck
     * @return String
     * @throws SessionInternalError
     */
    public String validate(String fieldName, Object value, boolean mandatoryCheck) {
        if (value instanceof String ? StringUtils.isEmpty(value.toString()) : value == null) {
            if (mandatoryCheck)
                throw new SessionInternalError("The attribute " + fieldName + " value must be present and non-empty");
            else return "";
        } else {
            return escapeCsv(value.toString().trim());
        }
    }

    /**
     * This method used for escape comma and double quote at same time for CSV file.
     *
     * @param str
     * @return String
     */
    public String escapeCsv(String str) {
        return StringEscapeUtils.escapeCsv(str);
    }

    /**
     * @param userWS
     * @return String
     */
    public String getCommodity(UserWS userWS) {
        String commodity = validate(FileConstants.COMMODITY, getMetaFieldValue(FileConstants.COMMODITY, null, userWS.getMetaFields()), true);
        return commodity.equals(FileConstants.COMMODITY_ELECTRICITY) ? "E" : commodity.equals(FileConstants.COMMODITY_GAS) ? "G" : null;
    }

    /**
     * @param value
     * @return String
     */
    public String getFormattedDate(String value) {
        SimpleDateFormat sdf = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
        String strDate = "";
        DateFormat df = new SimpleDateFormat(DATE_FORMATE);
        try {
            strDate = df.format(sdf.parse(value));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return strDate;
    }

    public static class FieldName {
        public static final String COMPANY_NAME = "Company Name";
        public static final String USER_STATUS = "User Status";
        public static final String ACCOUNT_NAME = "Account Name";
        public static final String COMMUNICATION_MODE = "Communication Mode";
        public static final String ORDER_CREATION_DATE = "Order Creation Date";
        public static final String USER_CREATION_DATE = "User Creation Date";
        public static final String PAYMENT_DATE = "Payment Date";
        public static final String PAYMENT_AMOUNT = "Payment Amount";
        public static final String PAYMENT_NOTES = "Notes";
        public static final String INVOICE_ID = "Invoice Id";
        public static final String INVOICE_LINE_TYPE_ID = "Invoice Line Type Id";
        public static final String INVOICE_CREATION_DATE = "Invoice Creation Date";
        public static final String ORDER_ACTIVE_SINCE_DATE = "Order Active Since Date";
        public static final String ORDER_ACTIVE_UNTIL_DATE = "Order Active Until Date";
        public static final String ACCOUNT_TYPE = "Order Active Until Date";
        public static final String INVOICE_LINE_DESCRIPTION = "Invoice Line Description";
        public static final String INVOICE_LINE_QUANTITY = "Invoice Line Quantity";
        public static final String INVOICE_LINE_PRICE = "Invoice Line Price";
        public static final String INVOICE_LINE_AMOUNT = "Invoice Line Amount";
        public static final String INVOICE_DUE_DATE = "Invoice Due Date";
        public static final String FIXED_PRICE = "Fixed Price";
        public static final String ADDER_FEE = "Adder Fee";
        public static final String PAYMENT_METHOD = "Payment Method";
        public static final String BILL_TOTAL = "Bill Total";
        public static final String AGENT_NAME = "Agent Name";
        public static final String PRODUCT_ID = "Product Id";
        public static final String ENROLLMENT_STATUS = "Enrollment Status";
        public static final String TAX_EXEMPTION_CODE = "Tax Exemption Code";
        public static final String TAX_DISCOUNT_PERCENTAGE = "Tax Discount Percentage";
    }

}
