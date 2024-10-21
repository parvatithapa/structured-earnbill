package com.sapienter.jbilling.server.pluggableTask;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDAS;
import com.sapienter.jbilling.server.invoiceSummary.db.InvoiceSummaryDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.CustomInvoiceFieldsEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.Util;

public class TelcoInvoiceParametersExTask extends TelcoInvoiceParametersTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(TelcoInvoiceParametersExTask.class));
    private static final String ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID = "Account Charges Product Category Id";
    private static final String OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID = "Other Charges And Credits Product Category Id";
    private static final String PRODUCT_CATEGORY_ID_OF_INTERNET_USAGE_ITEMS = "Product Category Id Of Internet Usage Items";
    private static final String BILLING_ADDRESS = "Billing Address Info Group Name";
    private static final String CREDIT_PRODUCTS_CATEGORY_ID = "Credit Products Category Id";
    private static final String DEBIT_PRODUCTS_CATEGORY_ID = "Debit Products Category Id";
    private static final String EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY_ID = "Exclude from Call Itemisation Category Id";
    private static final String BPAY_BILLER_CODE = "BPay Biller Code";
    private static final MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
    private static final String MAILING_ADDRESS = "Mailing Contact Info Group Name";

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        CustomInvoiceFieldsEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException {

        if (!(event instanceof CustomInvoiceFieldsEvent)) {
            throw new PluggableTaskException("Cannot process event " + event);
        }

        LOG.debug("Processing " + event);
        super.process(event);
        CustomInvoiceFieldsEvent customInvoiceFieldsEvent = (CustomInvoiceFieldsEvent) event;

        try {
            Integer userId = customInvoiceFieldsEvent.getUserId();
            Integer entityId = customInvoiceFieldsEvent.getEntityId();
            Map<String, Object> parameters = customInvoiceFieldsEvent.getParameters();
            InvoiceDTO invoice = customInvoiceFieldsEvent.getInvoice();
            generatePaperInvoiceReport(entityId, userId, parameters, invoice);
        } catch (Exception e){
            LOG.error("Exception while setting the parameters", e);
            throw new PluggableTaskException("Message or parameters may be null");
        }
    }

    private static void generatePaperInvoiceReport(Integer entityId, Integer userId, Map<String, Object> parameters, InvoiceDTO invoice) {
        Date effectiveDate = TimezoneHelper.companyCurrentDate(entityId);
        UserDTO user = new UserDAS().find(userId);
        InvoiceSummaryDTO invoiceSummary = new InvoiceSummaryDAS().findInvoiceSummaryByInvoice(invoice.getId());
        try{
            String accountChargesProductCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(accountChargesProductCategoryId)) {
                parameters.put("account_charges_product_category_id", Integer.valueOf(accountChargesProductCategoryId));
            }
            String otherChargesAndCreditsProductCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(otherChargesAndCreditsProductCategoryId)) {
                parameters.put("other_charges_and_credits_product_category_id", Integer.valueOf(otherChargesAndCreditsProductCategoryId));
            }
            String productCategoryIdOfInternetUsageItems = metaFieldDAS.getComapanyLevelMetaFieldValue(PRODUCT_CATEGORY_ID_OF_INTERNET_USAGE_ITEMS, entityId);
            if (StringUtils.isNotEmpty(productCategoryIdOfInternetUsageItems)) {
                parameters.put("product_category_id_of_internet_usage_items", Integer.valueOf(productCategoryIdOfInternetUsageItems));
            }
            String creditProductsCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(CREDIT_PRODUCTS_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(creditProductsCategoryId)) {
                parameters.put("credit_products_category_id", Integer.valueOf(creditProductsCategoryId));
            }
            String debitProductsCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(DEBIT_PRODUCTS_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(debitProductsCategoryId)) {
                parameters.put("debit_products_category_id", Integer.valueOf(debitProductsCategoryId));
            }
            String excludeFromCallItemisationCategoryId = metaFieldDAS.getComapanyLevelMetaFieldValue(EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY_ID, entityId);
            if (StringUtils.isNotEmpty(excludeFromCallItemisationCategoryId)) {
                parameters.put("exclude_from_call_itemisation_category_id", Integer.valueOf(excludeFromCallItemisationCategoryId));
            }
            Boolean usePaymentCreateDate = false;
            if(PreferenceBL.getPreferenceValueAsInteger(invoice.getBaseUser().getEntity().getId(),
                    CommonConstants.PREFERENCE_SWITCH_BETWEEN_PAYMENT_OR_CR_NOTE_DATE_AND_CREATION_DATE_FOR_PAYMENT_RECEIVED_AND_TOTAL_ADJ_AMOUNT_IN_INVOICE_SUMMARY) == 1) {
                usePaymentCreateDate = true;
            }
            parameters.put("use_payment_create_date", usePaymentCreateDate);
            String invoiceNumber = null;
            for (MetaFieldValue invoiceMetafield : invoice.getMetaFields()) {
                if(null != invoiceMetafield
                        && null != invoiceMetafield.getValue()
                                && null != invoiceMetafield.getField()
                                        && invoiceMetafield.getField().getName().equalsIgnoreCase("crmInvoiceID")) {
                    invoiceNumber = invoiceMetafield.getValue().toString();
                    break;
                }
            }
            parameters.put("invoiceNumber", null != invoiceNumber ? invoiceNumber : invoice.getPublicNumber());

            String accountNumber = null;
            for (MetaFieldValue customerMetafield : user.getCustomer().getMetaFields()) {
                if(null != customerMetafield
                        && null != customerMetafield.getValue()
                                && null != customerMetafield.getField()
                                        && customerMetafield.getField().getName().equalsIgnoreCase("crmAccountNumber")) {
                    accountNumber = customerMetafield.getValue().toString();
                    break;
                }
            }
            parameters.put("accountNumber", StringUtils.isNotEmpty(accountNumber) ? accountNumber : "");
            parameters.put("invoiceDate", Util.formatDate(invoice.getCreateDatetime(), userId));
            parameters.put("barcode", getBarcodeString(accountNumber, (null != invoiceSummary ? invoiceSummary.getTotalDue() : BigDecimal.ZERO)));
            String refNumber = null;
            Integer billerCode = null;
            String bpayBillerCode = metaFieldDAS.getComapanyLevelMetaFieldValue(BPAY_BILLER_CODE, entityId);
            for(PaymentInformationDTO instrument : user.getPaymentInstruments()) {
                if(null != instrument &&
                        instrument.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName().equals("BPAY")) {

                    try (PaymentInformationBL piBl = new PaymentInformationBL()) {
                        refNumber = piBl.getStringMetaFieldByType(instrument, MetaFieldType.BPAY_REF);
                        billerCode = piBl.getIntegerMetaFieldByType(instrument, MetaFieldType.BPAY_BILLIER_CODE);
                    }
                    if (StringUtils.isNumeric(bpayBillerCode) &&
                        null != billerCode &&
                        Integer.parseInt(bpayBillerCode) == Integer.valueOf(billerCode)) {
                        parameters.put("refNumber", StringUtils.isNotBlank(refNumber) ? refNumber : StringUtils.EMPTY);
                        parameters.put("billerCode", billerCode);
                        break;
                    } else {
                        if (StringUtils.isNotBlank(refNumber)) {
                            parameters.put("refNumber", refNumber);
                        }
                        parameters.put("billerCode", billerCode);
                    }
                }
            }
            setBillingAddressInfo(entityId, parameters, effectiveDate, user);
            Date billingPeriodStartDate = invoice.getCreateDatetime();
            Date billingPeriodEndDate = invoice.getCreateDatetime();
            Calendar cal = Calendar.getInstance();
            for (InvoiceLineDTO line: invoice.getInvoiceLines()) {
                if (null != line && null != line.getOrder()) {
                    for (OrderProcessDTO orderProcess : line.getOrder().getOrderProcesses()) {
                        if (null != orderProcess &&
                                null != orderProcess.getPeriodStart() &&
                                        null != orderProcess.getPeriodEnd() &&
                                                orderProcess.getInvoice().getId() == invoice.getId()) {
                            billingPeriodStartDate = orderProcess.getPeriodStart();
                            cal.setTime(orderProcess.getPeriodEnd());
                            cal.add(Calendar.DATE, -1);
                            billingPeriodEndDate = cal.getTime();
                            break;
                        }
                    }
                }
            }
            parameters.put("core.param.billingPeriodStartDate", billingPeriodStartDate);
            parameters.put("core.param.billingPeriodEndDate", billingPeriodEndDate);
            setMailingAddressInfo(entityId, parameters, effectiveDate, user);

        } catch (Exception e) {
            LOG.error("Exception while setting the parameters", e);
            throw new SessionInternalError("Error getting invoice", e);
        }
    }

    private static void setBillingAddressInfo(Integer entityId, Map<String, Object> parameters, Date effectiveDate, UserDTO user) {

        AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();

        String billingAdressGroupNameValue = metaFieldDAS.getComapanyLevelMetaFieldValue(BILLING_ADDRESS, entityId);
        MetaFieldGroup billingAdressGroup = accountInformationTypeDAS.getGroupByNameAndEntityId(entityId, EntityType.ACCOUNT_TYPE, billingAdressGroupNameValue, user.getCustomer().getAccountType().getId());

        if(null != billingAdressGroup) {
            Map<String, String> aitMetaFieldsBYUsage = metaFieldDAS.getCustomerAITMetaFieldValueMapByMetaFieldType(user.getCustomer().getId(), billingAdressGroup.getId(), effectiveDate);

            StringBuilder billingAddress1 = new StringBuilder();
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.SUB_PREMISES.toString())) {
                billingAddress1.append(aitMetaFieldsBYUsage.get(MetaFieldType.SUB_PREMISES.toString()));
                billingAddress1.append(" ");
            }
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.STREET_NUMBER.toString())) {
                billingAddress1.append(aitMetaFieldsBYUsage.get(MetaFieldType.STREET_NUMBER.toString()));
                billingAddress1.append(" ");
            }
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.STREET_NAME.toString())) {
                billingAddress1.append(aitMetaFieldsBYUsage.get(MetaFieldType.STREET_NAME.toString()));
                billingAddress1.append(" ");
            }
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.STREET_TYPE.toString())) {
                billingAddress1.append(aitMetaFieldsBYUsage.get(MetaFieldType.STREET_TYPE.toString()));
            }
            parameters.put("billingAddress1", billingAddress1);

            StringBuilder billingAddress2 = new StringBuilder();
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.CITY.toString())) {
                billingAddress2.append(aitMetaFieldsBYUsage.get(MetaFieldType.CITY.toString()));
                billingAddress2.append(" ");
            }
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.STATE_PROVINCE.toString())) {
                billingAddress2.append(aitMetaFieldsBYUsage.get(MetaFieldType.STATE_PROVINCE.toString()));
                billingAddress2.append(" ");
            }
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.POSTAL_CODE.toString())) {
                billingAddress2.append(aitMetaFieldsBYUsage.get(MetaFieldType.POSTAL_CODE.toString()));
                billingAddress2.append(" ");
            }
            parameters.put("billingAddress2", billingAddress2);
        }
    }

    /*
     * returns the barcode string formed
     */
    private static String getBarcodeString(String accountNumber, BigDecimal totalDue) {
        accountNumber = StringUtils.isNotEmpty(accountNumber) && accountNumber.length() > 3 ?
                accountNumber.substring(3, accountNumber.length()): "";
        String barcode =
            "*2005" + accountNumber +
            Integer.toString(getCheckDigit(accountNumber)) +
            StringUtils.leftPad(totalDue.setScale(2, BigDecimal.ROUND_HALF_UP).toString().replace(".", ""),7,"0");
        return barcode;
    }

    /*
     * returns check digit
     */
    public static int getCheckDigit(String accountNumber) {
        //1 1 3 4 9 5
        //1 2 1 2 1 2
        // multiplication result
        //1 2 3 8 9 10
        //1 2 3 8 9 1 = 24 mod 10 = 4 - 10 = 6 check digit
        char [] accountNumberCharArray = accountNumber.toCharArray();
        int checkDigitArray1[] = new int[accountNumberCharArray.length];
        int checkDigitArray2[] = new int[accountNumberCharArray.length];
        for (int i = 0; i < accountNumberCharArray.length; i++) {
            checkDigitArray1[i] = Character.getNumericValue(accountNumberCharArray[i]);
            if (i % 2 == 0) {
                checkDigitArray2[i] = 1 * checkDigitArray1[i];
                } else if (i % 2 == 1) {
                    checkDigitArray2[i] = 2 * checkDigitArray1[i];
                    }
        }
        int checkDigit = 0;
        for (int i = 0; i < checkDigitArray2.length; i++) {
            if (checkDigitArray2[i] > 9) {
                checkDigit+=sumDigits(checkDigitArray2[i]);
                } else {
                    checkDigit+=checkDigitArray2[i];
                    }
        }
        checkDigit = checkDigit % 10;
        checkDigit = 10 - checkDigit;
        return checkDigit == 10 ? 0 : checkDigit;
        }

        static int sumDigits(int no)
        {
            return no == 0 ? 0 : no%10 + sumDigits(no/10) ;
        }
        
    private static void setMailingAddressInfo(Integer entityId, Map<String, Object> parameters, Date effectiveDate, UserDTO user) {
        AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
        String mailingAddressGroupNameValue = metaFieldDAS.getComapanyLevelMetaFieldValue(MAILING_ADDRESS, entityId);
        MetaFieldGroup mailingAddressGroup = accountInformationTypeDAS.getGroupByNameAndEntityId(entityId, EntityType.ACCOUNT_TYPE, mailingAddressGroupNameValue, user.getCustomer().getAccountType().getId());
        if (null != mailingAddressGroup) {
            Map<String, String> aitMetaFieldsBYUsage = metaFieldDAS.getCustomerAITMetaFieldValueMapByMetaFieldType(user.getCustomer().getId(), mailingAddressGroup.getId(), effectiveDate);
            StringBuilder mailingAddressInfo = new StringBuilder();
            if (null != aitMetaFieldsBYUsage.get(MetaFieldType.BUSINESS_NAME.toString())) {
            	mailingAddressInfo.append(aitMetaFieldsBYUsage.get(MetaFieldType.BUSINESS_NAME.toString()));
            }
            parameters.put("businessName", mailingAddressInfo);
        }
    }
}
