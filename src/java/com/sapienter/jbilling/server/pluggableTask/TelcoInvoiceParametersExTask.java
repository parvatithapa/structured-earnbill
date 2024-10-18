package com.sapienter.jbilling.server.pluggableTask;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.CustomInvoiceFieldsEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Util;

public class TelcoInvoiceParametersExTask extends TelcoInvoiceParametersTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(TelcoInvoiceParametersExTask.class));
    private static final String ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID = "Account Charges Product Category Id";
    private static final String OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID = "Other Charges And Credits Product Category Id";
    private static final String PRODUCT_CATEGORY_ID_OF_INTERNET_USAGE_ITEMS = "Product Category Id Of Internet Usage Items";
    private static final String BILLING_ADDRESS = "Billing Address Info Group Name";
    private static final MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

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
            parameters.put("accountNumber", null != accountNumber ? accountNumber : "");
            parameters.put("invoiceDate", Util.formatDate(invoice.getCreateDatetime(), userId));
            setBillingAddressInfo(entityId, parameters, effectiveDate, user);

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
}
