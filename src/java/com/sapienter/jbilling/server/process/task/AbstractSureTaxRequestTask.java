package com.sapienter.jbilling.server.process.task;

import com.sapienter.jbilling.client.suretax.SureTaxBL;
import com.sapienter.jbilling.client.suretax.request.LineItem;
import com.sapienter.jbilling.client.suretax.request.SuretaxRequest;
import com.sapienter.jbilling.client.suretax.response.IResponseHeader;
import com.sapienter.jbilling.client.suretax.response.ItemMessage;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDAS;
import com.sapienter.jbilling.server.invoice.db.SuretaxTransactionLogDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.user.db.CustomerAccountInfoTypeMetaField;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Base class implementing common parameters and functionality to make a SureTax request.
 */
public abstract class AbstractSureTaxRequestTask extends PluggableTask {

    private static final FormatLogger LOG = new FormatLogger(
            Logger.getLogger(AbstractSureTaxRequestTask.class));

    public static final String LINE_ITEM_DISCOUNT_AS_NEW_LINE = "Send line item discount as separate line";
    public static final String SURETAX_REQUEST_URL = "Suretax Request Url";
    public static final String CLIENT_NUMBER = "Client Number";
    public static final String LINE_ITEM_TAX = "Line Item Tax";
    public static final String VALIDATION_KEY = "Validation Key";
    public static final String DATA_YEAR = "Data Year";
    public static final String DATA_MONTH = "Data Month";
    public static final String RESPONSE_GROUP = "Response Group";
    public static final String RESPONSE_TYPE = "Response Type";
    public static final String NUMBER_OF_DECIMAL = "Number of Decimals";
    public static final String ROLLBACK_INVOICE_ON_ERROR = "Rollback Invoice on Error";
    public static final String SURETAX_TRANS_ID_META_FIELD_NAME = "Suretax Response Trans Id";
    public static final String SECONDARY_ZIP_CODE_EXTN_FIELDNAME = "Secondary Zip Code Extension Field Name";
    public static final String SECONDARY_ZIP_CODE_FIELDNAME = "Secondary Zip Code Field Name";
    public static final String BILLING_ZIP_CODE_FIELDNAME = "Billing Zip Code Field Name";
    public static final String BILLING_ZIP_CODE_Extension_FIELDNAME = "Billing Zip Code Extension Field Name";
    public static final String REGULATORY_CODE_FIELDNAME = "Regulatory Code Field Name";
    public static final String SALES_TYPE_CODE_FIELDNAME = "Sales Type Code Field Name";
    public static final String TAX_EXEMPTION_CODE_FIELDNAME = "Tax Exemption Code Field Name";
    public static final String TRANSACTION_TYPE_CODE_FIELDNAME = "Transaction Type Code Field Name";
    public static final String UNIT_TYPE_CODE_FIELDNAME = "UnitType Field Name";

    protected static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    protected SuretaxTransactionLogDAS suretaxTransactionLogDAS = null;
    protected String secondaryZipCodeExtensionFieldname;
    protected String secondaryZipCodeFieldname;
    protected String billingZipCodeFieldname;
    protected String billingZipCodeExtensionFieldname;
    protected String regulatoryCodeFieldname;
    protected String salesTypeCodeFieldname;
    protected String taxExemptionCodeFieldname;
    protected String transactionTypeCodeFieldname;
    protected MetaFieldValue<String> transTypeCode = null;
    protected String unitTypeFieldname;
    protected String p2pPlus4;
    protected String p2pZipcode;
    protected String plus4;
    protected String salesTypeCode;
    protected String billingZipCodeValue;
    protected String customerId;
    protected String transactionDate;
    protected MetaFieldValue<String> taxExemptionCode;
    protected ObjectMapper mapper = null;
    protected boolean calculateLineItemTaxes = false;
    protected String uniqueTrackingCode;
    protected boolean rollbackInvoiceOnSuretaxError = true;
    protected boolean sendLineItemDiscountAsNewLine = false;

    private OrderDTO currentOrder = null;

    public AbstractSureTaxRequestTask() {
        suretaxTransactionLogDAS = new SuretaxTransactionLogDAS();
        descriptions.add(new ParameterDescription(SURETAX_REQUEST_URL, true, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(CLIENT_NUMBER, true, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(LINE_ITEM_TAX, false, ParameterDescription.Type.BOOLEAN));
        descriptions.add(new ParameterDescription(VALIDATION_KEY, true, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(DATA_YEAR, false, ParameterDescription.Type.INT));
        descriptions.add(new ParameterDescription(DATA_MONTH, false, ParameterDescription.Type.INT));
        descriptions.add(new ParameterDescription(RESPONSE_GROUP, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(RESPONSE_TYPE, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(NUMBER_OF_DECIMAL, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(ROLLBACK_INVOICE_ON_ERROR, false, ParameterDescription.Type.BOOLEAN));
        descriptions.add(new ParameterDescription(SECONDARY_ZIP_CODE_EXTN_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(SECONDARY_ZIP_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(BILLING_ZIP_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(BILLING_ZIP_CODE_Extension_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(REGULATORY_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(SALES_TYPE_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(TAX_EXEMPTION_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(TRANSACTION_TYPE_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(UNIT_TYPE_CODE_FIELDNAME, false, ParameterDescription.Type.STR));
        descriptions.add(new ParameterDescription(LINE_ITEM_DISCOUNT_AS_NEW_LINE, false, ParameterDescription.Type.BOOLEAN));
        mapper = new ObjectMapper();
    }


    protected void performRequest(SuretaxRequest suretaxRequest, SureTaxBL sureTaxBL) throws TaskException {
        //if no lines return immediately
        if(suretaxRequest.itemList == null || suretaxRequest.itemList.isEmpty()) {
            return;
        }
        IResponseHeader response = null;
        boolean errorOccurred = false;
        try{
            // Save the suretax json request in the sure_tax_txn_log table

            String jsonRequestString = mapper
                    .writeValueAsString(suretaxRequest);

            suretaxTransactionLogDAS.save(new SuretaxTransactionLogDTO(
                    suretaxRequest.clientTracking, "REQUEST",
                    jsonRequestString,
                    new Timestamp(System.currentTimeMillis()), null, "TAX"));

            if(calculateLineItemTaxes) {
                response = sureTaxBL.calculateLineItemTax(suretaxRequest);
            } else {
                response = sureTaxBL.calculateAggregateTax(suretaxRequest);
            }
        } catch (Exception e) {
            if (rollbackInvoiceOnSuretaxError)
                throw new TaskException(e);
            else
                errorOccurred = true;
        }

        // If the control has come here but an error had occurred then
        // the error was meant to be ignored. So, if an error had occurred
        // then just ignore the next block of code. That is exit gracefully
        if (!errorOccurred) {
            // Save the suretax json response in the sure_tax_txn_log table
            String transId = response.getTransId();
            suretaxTransactionLogDAS.save(new SuretaxTransactionLogDTO(
                    suretaxRequest.clientTracking, "RESPONSE",
                    response.getJsonString(), new Timestamp(System
                    .currentTimeMillis()), transId, "TAX"));
            if (response != null && !response.getSuccessful().equals("Y")) {
                if (rollbackInvoiceOnSuretaxError) {
                    throw new TaskException(
                            "Error while obtaining the tax lines for this invoice:"
                                    + response.getResponseCode() + ":"
                                    + response.getHeaderMessage());
                }

            } else if (response != null
                    && response.getSuccessful().equals("Y")
                    && response.getHeaderMessage()
                    .contains("Success with Item errors")) {
                StringBuffer errorMessages = new StringBuffer(
                        "Error messages:[");
                int count = 0;
                for (ItemMessage itemMessage : response.getItemMessages()) {
                    if (count == 0) {
                        count++;
                    } else {
                        errorMessages.append(",");
                    }
                    errorMessages.append(itemMessage.message);
                }
                errorMessages.append("]");
                if (rollbackInvoiceOnSuretaxError) {
                    throw new TaskException(
                            "Error while obtaining the tax lines for this invoice:"
                                    + errorMessages.toString());
                }
            } else {
                LOG.debug("Response Code: " + response.getResponseCode()
                        + ", Header Message:" + response.getHeaderMessage()
                        + ", Client Tracking: " + response.getClientTracking()
                        + ", Total tax:" + response.getTotalTax() + ", Trans Id: "
                        + response.getTransId());


                handleResponse(suretaxRequest, response);
            }
        }
    }

    protected abstract void handleResponse(SuretaxRequest request, IResponseHeader response) ;

    protected void extractPluginParameters(Integer userId) {
        // Get the meta field names
        secondaryZipCodeExtensionFieldname = getParameter(
                SECONDARY_ZIP_CODE_EXTN_FIELDNAME,
                "Secondary Zip code extension");
        secondaryZipCodeFieldname = getParameter(
                SECONDARY_ZIP_CODE_FIELDNAME, "Secondary Zip code");
        billingZipCodeFieldname = getParameter(
                BILLING_ZIP_CODE_FIELDNAME, "Billing Zip code extension");
        billingZipCodeExtensionFieldname = getParameter(
                BILLING_ZIP_CODE_Extension_FIELDNAME, "Zip code");
        regulatoryCodeFieldname = getParameter(
                REGULATORY_CODE_FIELDNAME, "Regulatory Code");
        salesTypeCodeFieldname = getParameter(SALES_TYPE_CODE_FIELDNAME,
                "Sales Type Code");
        taxExemptionCodeFieldname = getParameter(
                TAX_EXEMPTION_CODE_FIELDNAME, "Tax exemption code");
        transactionTypeCodeFieldname = getParameter(
                TRANSACTION_TYPE_CODE_FIELDNAME, "Transaction Type Code");
        unitTypeFieldname = getParameter(
                UNIT_TYPE_CODE_FIELDNAME, "Unit Type");

        try {
            calculateLineItemTaxes = getParameter(AbstractSureTaxRequestTask.LINE_ITEM_TAX, 1) == 1;
        } catch (PluggableTaskException e) {
            LOG.debug(AbstractSureTaxRequestTask.LINE_ITEM_TAX + " not defined", e);
        }

        try {
            rollbackInvoiceOnSuretaxError = getParameter(AbstractSureTaxRequestTask.ROLLBACK_INVOICE_ON_ERROR, 1) == 1;
        } catch (PluggableTaskException e) {
            LOG.debug(AbstractSureTaxRequestTask.ROLLBACK_INVOICE_ON_ERROR + " not defined", e);
        }

        try {
            sendLineItemDiscountAsNewLine = getParameter(AbstractSureTaxRequestTask.LINE_ITEM_DISCOUNT_AS_NEW_LINE, 1) == 1;
        } catch (PluggableTaskException e) {
            LOG.debug(AbstractSureTaxRequestTask.LINE_ITEM_DISCOUNT_AS_NEW_LINE + " not defined", e);
        }

        UserDTO invoiceToUser = new UserDAS().find(userId);
        customerId = Integer.toString(invoiceToUser.getCustomer().getId());

        //Fetch data from customer
        List<String> metaFieldNames = new LinkedList<String>();
        metaFieldNames.add(secondaryZipCodeExtensionFieldname);
        metaFieldNames.add(secondaryZipCodeFieldname);
        metaFieldNames.add(billingZipCodeFieldname);
        metaFieldNames.add(billingZipCodeExtensionFieldname);
        metaFieldNames.add(taxExemptionCodeFieldname);
        Map<String, MetaFieldValue> customerMetaFieldValues = getMetaFieldValues(invoiceToUser, metaFieldNames);

        MetaFieldValue<String> p2PPlus4 = customerMetaFieldValues
                .get(secondaryZipCodeExtensionFieldname);
        if (p2PPlus4 != null) {
            p2pPlus4 = p2PPlus4.getValue().toString();
        } else {
            p2pPlus4 = "";
        }

        MetaFieldValue<String> p2PZipcode = customerMetaFieldValues
                .get(secondaryZipCodeFieldname);
        if (p2PZipcode != null) {
            p2pZipcode = p2PZipcode.getValue().toString();
        } else {
            p2pZipcode = "";
        }

        MetaFieldValue<String> plus4Mf = customerMetaFieldValues
                .get(billingZipCodeExtensionFieldname);
        if (plus4Mf != null) {
            plus4 = plus4Mf.getValue().toString();
        } else {
            plus4 = "";
        }

        MetaFieldValue billingZipCodeValueMf = customerMetaFieldValues
                .get(billingZipCodeFieldname);
        if (billingZipCodeValueMf == null || billingZipCodeValueMf.getValue() == null) {
            billingZipCodeValue = "";
        } else {
            billingZipCodeValue = billingZipCodeValueMf.getValue().toString();
        }

        taxExemptionCode = customerMetaFieldValues.get(taxExemptionCodeFieldname);
    }

    protected void extractOrderParameters(OrderDTO orderDTO) {
        if(currentOrder == orderDTO) {
            return;
        }

        MetaFieldValue<String> salesTypeCodeMf = orderDTO
                .getMetaField(salesTypeCodeFieldname);
        if (salesTypeCodeMf == null || salesTypeCodeMf.getValue() == null
                || salesTypeCodeMf.getValue().isEmpty()) {
            salesTypeCode = "R";
        } else {
            salesTypeCode = salesTypeCodeMf.getValue();
        }
        currentOrder = orderDTO;
    }

    protected Map<String, MetaFieldValue> getMetaFieldValues(UserDTO user, List<String> metaFieldNames) {
        Map<String, MetaFieldValue> metaFieldValues = new HashMap<String, MetaFieldValue>();
        Set<CustomerAccountInfoTypeMetaField> list = user.getCustomer().getCustomerAccountInfoTypeMetaFields();
        for (CustomerAccountInfoTypeMetaField customerAccountInfoTypeMetaField : list) {
            MetaFieldValue metaField = customerAccountInfoTypeMetaField.getMetaFieldValue();
            if (metaFieldNames.contains(metaField.getField().getName())) {

                if (metaFieldNames.remove(metaField.getField().getName()))
                    metaFieldValues.put(metaField.getField().getName(), metaField);
            }
        }
        return metaFieldValues;
    }

    protected SuretaxRequest buildBaseSuretaxRequest() {
        // Construct a suretax request to get the tax lines.
        SuretaxRequest suretaxRequest = new SuretaxRequest();
        uniqueTrackingCode = SureTaxBL.nextTransactionId();

        // Get the pluggable task parameters here.
        String clientNumber = getParameter(AbstractSureTaxRequestTask.CLIENT_NUMBER, "");
        String validationKey = getParameter(AbstractSureTaxRequestTask.VALIDATION_KEY, "");
        String responseGroup = getParameter(AbstractSureTaxRequestTask.RESPONSE_GROUP, "03");
        /* SureTax takes this as a Quote and doesn’t store anything on their side. */
        String responseType = getParameter(AbstractSureTaxRequestTask.RESPONSE_TYPE, "D");
        String numberOfDecimals = getParameter(AbstractSureTaxRequestTask.NUMBER_OF_DECIMAL, "2");
        Integer dataYear = null;
        Integer dataMonth = null;
        try {
            dataYear = getParameter(AbstractSureTaxRequestTask.DATA_YEAR, Calendar.getInstance().get(Calendar.YEAR));
            dataMonth = getParameter(AbstractSureTaxRequestTask.DATA_MONTH, Calendar.getInstance().get(Calendar.MONTH) + 1);
        } catch (PluggableTaskException e) {
            LOG.debug("Exception while retrieving Data Year or Data Month");
        }

        suretaxRequest.setClientNumber(clientNumber);
        suretaxRequest.setValidationKey(validationKey);
        suretaxRequest.setClientTracking(uniqueTrackingCode);
        suretaxRequest.setDataMonth(dataMonth.toString());
        suretaxRequest.setDataYear(dataYear.toString());
        suretaxRequest.setIndustryExemption("");
        suretaxRequest.setBusinessUnit("");
        suretaxRequest.setResponseGroup(responseGroup);
        suretaxRequest.setResponseType(responseType + numberOfDecimals);
        return suretaxRequest;
    }

    protected LineItem constructBaseLineItem(String uniqueTrackingCode, PlanDTO plan, ItemDTO item, String lineNr) {
        LineItem lineItem = new LineItem();
        lineItem.setBillToNumber(""); // TODO: need to be addressed ?
        lineItem.setCustomerNumber(customerId);
        lineItem.setInvoiceNumber("JB" + uniqueTrackingCode);
        lineItem.setLineNumber(lineNr);
        lineItem.setOrigNumber(""); // TODO: need to be addressed ?

        lineItem.setP2PPlus4(p2pPlus4);
        lineItem.setP2PZipcode(p2pZipcode);
        lineItem.setPlus4(plus4);

        LOG.debug("Meta fields: p2PPlus4: %s, p2PZipcode: %s, plus4: %s", p2pPlus4, p2pZipcode, plus4);

        MetaFieldValue<String> regulatoryCode = null;
        if (plan != null) {
            regulatoryCode = plan.getMetaField(regulatoryCodeFieldname);
        } else {
            regulatoryCode = item.getMetaField(regulatoryCodeFieldname);
        }
        if (regulatoryCode == null || regulatoryCode.getValue() == null
                || regulatoryCode.getValue().isEmpty()) {
            lineItem.setRegulatoryCode("00");
        } else {
            lineItem.setRegulatoryCode(regulatoryCode.getValue());
        }

        List<String> taxExemptionCodeList = new ArrayList<String>();

        MetaFieldValue<String> taxExemptionCode = this.taxExemptionCode;
        LOG.debug("Tax exemption code from customer: " + taxExemptionCode);
        if (!(taxExemptionCode != null && !taxExemptionCode.isEmpty())) {
            if (plan != null) {
                taxExemptionCode = plan.getMetaField(taxExemptionCodeFieldname);
            } else {
                // If that was null/empty then get it from the product
                taxExemptionCode = item.getMetaField(taxExemptionCodeFieldname);
            }
            LOG.debug("Tax exemption code from product: " + taxExemptionCode);
        }
        if (taxExemptionCode == null) {
            LOG.debug("Setting tax exemption code to be 00");
            taxExemptionCodeList.add("00");
        } else {
            taxExemptionCodeList.add(taxExemptionCode.getValue());
        }
        LOG.debug("Meta fields: regulatoryCode: %s, salesTypeCode: %s, taxExemptionCode: %s"
                , regulatoryCode, salesTypeCode, taxExemptionCode);
        lineItem.setTaxExemptionCodeList(taxExemptionCodeList);
        lineItem.setTaxIncludedCode("0");

        lineItem.setTermNumber("");

        // TODO: Need to check if trans date will be current date or based on data year and data month ?
//        lineItem.setTransDate("07-10-2012");
        lineItem.setTransDate(transactionDate);

        lineItem.setTransTypeCode(transTypeCode.getValue());

        //getting Unit Type
        MetaFieldValue<String> unitType = null;
        if (plan != null) {
            unitType = plan.getMetaField(unitTypeFieldname);
        } else {
            unitType = item.getMetaField(unitTypeFieldname);
        }

        LOG.debug(" Unit Type " + unitType);
        if (unitType == null || unitType.getValue() == null
                || unitType.getValue().isEmpty()) {
            lineItem.setUnitType("00");
        }else{
            lineItem.setUnitType(unitType.getValue());
        }

        String zipCode = null;
        if (billingZipCodeValue != null) zipCode = billingZipCodeValue;

        String plus4Value = null;
        if (plus4 != null) plus4Value = plus4;

        if ((plus4Value == null || plus4.isEmpty()) && zipCode != null && zipCode.length() == 9) {
            plus4Value = zipCode.substring(5);
            zipCode = zipCode.substring(0, 5);
        }
        if (plus4Value != null) {
            lineItem.setPlus4(plus4Value);
        } else {
            lineItem.setPlus4("");
        }
        if (zipCode != null && !zipCode.isEmpty() && plus4Value != null
                && !plus4Value.isEmpty()) {
            lineItem.setZipcode(zipCode);
            lineItem.setTaxSitusRule("05");
        } else if (zipCode != null && !zipCode.isEmpty()
                && (plus4Value == null || plus4Value.isEmpty())) {
            lineItem.setZipcode(zipCode);
            lineItem.setPlus4("0000");
            lineItem.setTaxSitusRule("05");
        }
        lineItem.setSalesTypeCode(salesTypeCode);
        return lineItem;
    }


    /**
     * This method is used to adjust LineItem object's revenue as per discount amount.
     * Discount can be order or order line level.
     * @param lineItems - items which should receive discount
     * @param amount - Amount on which discount is applied. Either order's total or order line's amount.
     * @param discount - discount amount to apply (should be negative)
     */
    protected void subtractDiscountFromLines(List<LineItem> lineItems, BigDecimal amount, BigDecimal discount) {
        if(lineItems == null) {
            return;
        }
        BigDecimal discountFraction = discount.divide(amount);
        for(LineItem lineItem: lineItems) {
            BigDecimal lineAmount = BigDecimal.valueOf(lineItem.getRevenue());
            BigDecimal appliedDiscount = discount.min(lineAmount.multiply(discountFraction));
            discount = discount.subtract(appliedDiscount);

            lineItem.setRevenue(lineAmount.add(appliedDiscount).floatValue());
        }
    }
}
