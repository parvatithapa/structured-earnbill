package com.sapienter.jbilling.server.pluggableTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customer.CustomerBL;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants.MetaFieldName;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldGroup;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValueDAS;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskBL;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDAS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskParameterDTO;
import com.sapienter.jbilling.server.process.event.CustomInvoiceFieldsEvent;
import com.sapienter.jbilling.server.user.ContactDTOEx;
import com.sapienter.jbilling.server.user.EntityBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.AccountInformationTypeDAS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDAS;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.item.db.ItemDTO;
/**
 * This plugin handles the CustomInvoiceFieldsEvent
 * to pass Full Creative custom invoice specific parameters
 * This task subscribes to the {@link CustomInvoiceFieldsEvent}
 *  
 * @author Mahesh Shivarkar
 * @since  24-06-2016
 */

public class FullCreativeCustomInvoiceFieldsTokenTask extends PluggableTask
implements IInternalEventsTask {

    public static final ParameterDescription PARAMETER_NEW_INVOICE_CUT_OVER_DATE = 
            new ParameterDescription("new_invoice_cut_over_date", true, ParameterDescription.Type.DATE);

    //initializer for pluggable params
    { 
        descriptions.add(PARAMETER_NEW_INVOICE_CUT_OVER_DATE);
    }

    private static CustomerDAS customerDAS = new CustomerDAS();
    private static InvoiceDAS invoiceDAS = new InvoiceDAS();
    private static PaymentInvoiceMapDAS paymentInvoiceMapDAS = new PaymentInvoiceMapDAS();
    private static ContactDAS contactDas = new ContactDAS();
    private static PluggableTaskParameterDAS pluggableTaskParameterDAS = new PluggableTaskParameterDAS();
    private static ItemDAS itemDAS = new ItemDAS();
    private static PaymentDAS paymentDAS = new PaymentDAS();
    private static AccountInformationTypeDAS accountInformationTypeDAS = new AccountInformationTypeDAS();
    private static CompanyDAS companyDAS = new CompanyDAS();
    private static final String PAPER_INVOICE_EXCEPTION = "Exception generating paper invoice";
    private static final String LAST_PAYMENT_DATE = "lastPaymentDate";
    private static final String LAST_PAYMENT_AMOUNT = "lastPaymentAmount";
    private static final String PREV_INVOICE_TOTAL = "prevInvoiceTotalWithOutCurrencySymbol";
    private static final String FEES_PRODUCT_CATEGORY_ID = "Fees Product Category ID";
    private static final String ADJUSTMENTS_PRODUCT_CATEGORY_ID = "Adjustments Product Category ID";
    private static final String ANSWERCONNECT_UK = "AnswerConnect UK";
    private static final String ANSWERCONNECT_UK_DESIGN = "invoice_design_ac_uk";
    private static final String INVOICE_DESIGN = "design";

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(FullCreativeCustomInvoiceFieldsTokenTask.class));

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        CustomInvoiceFieldsEvent.class
    };

    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public void process(Event event) throws PluggableTaskException {

        if (!(event instanceof CustomInvoiceFieldsEvent)) {
            throw new PluggableTaskException("Cannot process event " + event);
        }

        LOG.debug("Processing " + event);

        CustomInvoiceFieldsEvent customInvoiceFieldsEvent = (CustomInvoiceFieldsEvent) event;
        try {
            Date newInvoiceCutOverDate = getParameter(PARAMETER_NEW_INVOICE_CUT_OVER_DATE.getName());
            if (customInvoiceFieldsEvent.getInvoice().getCreateDatetime().after(newInvoiceCutOverDate)) {
                LOG.debug("This plugin cannot process when Invoice date is after new invoice cut over date.");
                return;
            }
            LOG.debug("Populate old Invoice template parameters:" + newInvoiceCutOverDate);
            String design = null;
            PluggableTaskBL<?> pluggableTaskBL = new PluggableTaskBL<>();
            pluggableTaskBL.set(customInvoiceFieldsEvent.getEntityId(), Constants.PLUGGABLE_TASK_T_PAPER_INVOICE);
            List<PluggableTaskParameterDTO> paperInvoicePluginParameters = new PluggableTaskParameterDAS().findAllByTask(pluggableTaskBL.getDTO());

            for (PluggableTaskParameterDTO paperInvoicePluginParameter : paperInvoicePluginParameters) {
                if (null != paperInvoicePluginParameter &&
                        INVOICE_DESIGN.equals(paperInvoicePluginParameter.getName())) {
                    design = paperInvoicePluginParameter.getStrValue();
                    LOG.debug("Invoice Design: "+design);
                }
            }

            Map<String, Object> parameters = customInvoiceFieldsEvent.getParameters();

            CompanyDTO companyDTO = companyDAS.find(customInvoiceFieldsEvent.getEntityId());
            if(null != companyDTO &&
                    ANSWERCONNECT_UK.equals(companyDTO.getDescription())	&&
                    ANSWERCONNECT_UK_DESIGN.equals(design)) {
                // Invoice parameters specific for AnswerConnect UK company will be sent from this method 
                generatePaperInvoiceNew(parameters, customInvoiceFieldsEvent.getInvoice(),
                        customInvoiceFieldsEvent.getTo(), customInvoiceFieldsEvent.getEntityId());
            }
            else {

                generatePaperInvoiceDefault(parameters, customInvoiceFieldsEvent.getInvoice(), 
                        customInvoiceFieldsEvent.getTo(), customInvoiceFieldsEvent.getEntityId(),design);
            }     
        } catch (Exception e){
            LOG.error(PAPER_INVOICE_EXCEPTION, e);
            throw new PluggableTaskException("Message or parameters may be null");
        }
    }

    @SuppressWarnings("deprecation")
    private static void generatePaperInvoiceDefault(Map<String, Object> parameters, InvoiceDTO invoice, ContactDTOEx to, Integer entityId, String design) throws FileNotFoundException {
        try {
            parameters.put("dateDue", Util.formatDate(invoice.getDueDate(), invoice.getUserId()));
            if (null != to) {
                CustomerDTO customer = new UserDAS().find(invoice.getUserId()).getCustomer();
                Integer accountTypeId = customer.getAccountType().getId();

                String mailingGroupNameValue = new MetaFieldDAS().getComapanyLevelMetaFieldValue(MetaFieldName.NOTIFICATION_MAILING_ADDRESS.getMetaFieldName(), entityId);

                MetaFieldGroup metaFieldGroup = accountInformationTypeDAS.getGroupByNameAndEntityId(entityId
                        , EntityType.ACCOUNT_TYPE, mailingGroupNameValue, accountTypeId);

                if (null != metaFieldGroup) {
                    MetaFieldDAS metaFieldDAS = new MetaFieldDAS();
                    MetaFieldValueDAS metaFieldValueDAS = new MetaFieldValueDAS();

                    Date effectiveDate = TimezoneHelper.companyCurrentDate(entityId);

                    List<MetaFieldValue<?>> organizationFieldValues = new ArrayList<>();

                    for(Integer valueFieldId : metaFieldDAS.getCustomerFieldValues(customer.getId(), MetaFieldType.ORGANIZATION, metaFieldGroup.getId(), effectiveDate)) {
                        organizationFieldValues.add(metaFieldValueDAS.find(valueFieldId));
                    }

                    Collections.sort(organizationFieldValues, (v1 , v2) -> v1.getField().getDisplayOrder().compareTo(v2.getField().getDisplayOrder()));

                    StringBuilder firstNameLastName = new StringBuilder();

                    for(MetaFieldValue<?> value: organizationFieldValues) {
                        Object strValue = value.getValue();
                        firstNameLastName.append(Objects.nonNull(strValue) ? strValue.toString() + " " : "");
                    }

                    Map<String, String> aitMetaFieldsBYUsage = metaFieldDAS
                            .getCustomerAITMetaFieldValueMapByMetaFieldType(customer.getId(), metaFieldGroup.getId(), effectiveDate);

                    LOG.debug(" %s meta fields fetched for customer %s for effective date %s ", aitMetaFieldsBYUsage, customer.getId(), effectiveDate);

                    parameters.put("customerName", firstNameLastName.toString());
                    parameters.put("customerAddress",  aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.ADDRESS1.toString(), ""));
                    parameters.put("customerAddress2", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.ADDRESS2.toString(), ""));
                    parameters.put("customerCity", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.CITY.toString(), ""));
                    parameters.put("customerProvince", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.STATE_PROVINCE.toString(), ""));
                    parameters.put("customerPostalCode", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.POSTAL_CODE.toString(), ""));
                }
                parameters.put("customerOrganization", printable(to.getOrganizationName()));

            }

            // symbol of the currency
            String symbol = evaluateCurrencySymbol(invoice);
            parameters.put("currency_symbol",symbol);

            parameters.put("userId", invoice.getUserId());
            // now some info about payments
            long invoiceCount = invoiceDAS.getInvoiceCountByUserId(invoice.getUserId(), invoice.getCreateDatetime()).longValue();
            LOG.debug("Invoice count: "+invoiceCount);
            parameters.put("invoiceCount", invoiceCount);

            BigDecimal totalPayment = BigDecimal.ZERO;
            // Gathering payment data for brand new customers
            List<PaymentDTO> newCustomerPayments = paymentDAS.findSuccessfulOrEnteredPaymentsByUser(invoice.getUserId());
            List<PaymentDTO> unlinkedPayments = null;
            List<PaymentInvoiceMapDTO> existingCustomerPayments = new ArrayList<PaymentInvoiceMapDTO>();
            LOG.debug("Current Invoice Id "+invoice.getId());
            BigDecimal prevInvoiceTotalWithOutCurrencySymbol = BigDecimal.ZERO;
            InvoiceBL invoiceBL = new InvoiceBL(invoice.getId());

            try {
                // find the previous invoice and its payment for extra info
                invoiceBL.setPreviousByInvoiceDate();
                parameters.put("prevInvoiceId", invoiceBL.getEntity().getId());
                LOG.debug("Previous Invoice Id "+invoiceBL.getEntity().getId());

                if (invoiceCount >= 2) {
                    // Get payments linked to previous invoice.
                    existingCustomerPayments = paymentInvoiceMapDAS.getLinkedPaymentsByInvoiceId(invoiceBL.getEntity().getId());
                }
                // Get payments with balance done after previous invoice
                unlinkedPayments = paymentDAS.getPaymentsWithBalanceDoneAfterPreviousInvoiceDate(invoice.getUserId(), invoiceBL.getEntity().getId(),invoice.getId());

                parameters.put("paid", Util.formatMoney(invoiceBL
                        .getTotalPaid(), invoice.getUserId(), invoice
                        .getCurrency().getId(), false));

                parameters.put("prevInvoiceTotal", Util.formatMoney(
                        invoiceBL.getEntity().getTotal(), invoice
                        .getUserId(), invoice.getCurrency().getId(),
                        false));
                // Current invoice carried balance
                BigDecimal carriedBalanceWithoutCurrencySymbol = (null != invoice.getCarriedBalance() ?
                        invoice.getCarriedBalance().setScale(2, RoundingMode.HALF_UP):new BigDecimal(0));
                prevInvoiceTotalWithOutCurrencySymbol = invoiceBL.getEntity().getTotal();
                // Previous invoice carried balance
                BigDecimal previousInvoiceCarriedBalance = invoiceBL.getEntity().getCarriedBalance();


                Integer carriedInvoiceCount = invoiceDAS.getCarriedInvoicesCountByUserIdAndInvoiceDate(invoice.getUserId(), invoice.getCreateDatetime());

                LOG.debug("invoiceBL.getEntity().getInvoiceStatus().getId(): "+ invoiceBL.getEntity().getInvoiceStatus().getId());
                LOG.debug("carriedInvoiceCount "+carriedInvoiceCount);
                LOG.debug("invoice.getInvoiceStatus().getId() != 3 && carriedInvoiceCount >= 2: "+ (invoice.getInvoiceStatus().getId() != 3 && carriedInvoiceCount >= 2));

                if (design.equals("invoice_design_fc")) {
                    parameters.put(PREV_INVOICE_TOTAL,invoice.getCarriedBalance().setScale(2, RoundingMode.HALF_UP));
                } else {
                    // If current invoice is not carried invoice then set it's carried balance as amount of last statement
                    // else set previous invoice amount.
                    if ((invoice.getInvoiceStatus().getId() == CommonConstants.INVOICE_STATUS_UNPAID_AND_CARRIED
                            || invoice.getInvoiceStatus().getId() == CommonConstants.INVOICE_STATUS_UNPAID)
                            && carriedInvoiceCount >= 1
                            && CollectionUtils.isEmpty(existingCustomerPayments)) {

                        // Carried invoice with no payment
                        prevInvoiceTotalWithOutCurrencySymbol = carriedBalanceWithoutCurrencySymbol;
                        parameters.put(PREV_INVOICE_TOTAL,carriedBalanceWithoutCurrencySymbol.setScale(2, RoundingMode.HALF_UP));
                    } else if (invoice.getInvoiceStatus().getId() != CommonConstants.INVOICE_STATUS_UNPAID_AND_CARRIED
                            && carriedInvoiceCount >= 1
                            && CollectionUtils.isNotEmpty(existingCustomerPayments)) {

                        // Amount of last statement = Sum of balance of all carried invoices + payments linked to previous invoice
                        BigDecimal invoiceBalance = invoiceDAS.getUnpaidAndCarriedInvoiceBalanceByUser(invoice.getUserId(),invoice.getCreateDatetime());
                        BigDecimal sumOfPaymentsLinkedToPreviousInvoice = BigDecimal.ZERO;
                        List<PaymentInvoiceMapDTO> paymentsLinkedToPreviousInvoice = paymentInvoiceMapDAS.getLinkedPaymentsByInvoiceId(invoiceBL.getEntity().getId());

                        if (null != paymentsLinkedToPreviousInvoice) {
                            for (PaymentInvoiceMapDTO paymentInvoiceMap : paymentsLinkedToPreviousInvoice) {
                                sumOfPaymentsLinkedToPreviousInvoice = sumOfPaymentsLinkedToPreviousInvoice.add(paymentInvoiceMap.getAmount());
                            }
                        }
                        prevInvoiceTotalWithOutCurrencySymbol = invoiceBalance.add(sumOfPaymentsLinkedToPreviousInvoice);

                        // Carried invoice with partial payment
                        parameters.put(PREV_INVOICE_TOTAL,prevInvoiceTotalWithOutCurrencySymbol.setScale(2, RoundingMode.HALF_UP));
                    } else {

                        prevInvoiceTotalWithOutCurrencySymbol = prevInvoiceTotalWithOutCurrencySymbol.subtract(previousInvoiceCarriedBalance);
                        parameters.put(PREV_INVOICE_TOTAL,prevInvoiceTotalWithOutCurrencySymbol.setScale(2, RoundingMode.HALF_UP));
                    }
                }

                parameters.put("prevInvoicePaid", Util.formatMoney(invoiceBL.getTotalPaid(), invoice
                        .getUserId(), invoice.getCurrency().getId(),
                        false));

            } catch (EmptyResultDataAccessException e1) {
                parameters.put("prevInvoiceId", ("invoice_design_fc".equals(design) ? null : invoice.getId()));
                // If no previous invoice found get payments linked to current invoice
                existingCustomerPayments = paymentInvoiceMapDAS.getLinkedPaymentsByInvoiceId(invoice.getId());
                LOG.error(PAPER_INVOICE_EXCEPTION, e1);
            } catch (Exception e) {
                LOG.error(PAPER_INVOICE_EXCEPTION, e);
            }

            List<PaymentInvoiceMapDTO> paymentsLinkedToCurrentInvoiceMap = paymentInvoiceMapDAS.getLinkedPaymentsByInvoiceId(invoice.getId());
            BigDecimal sumOfpaymentsLinkedToCurrentInvoice = BigDecimal.ZERO;
            if (null != paymentsLinkedToCurrentInvoiceMap) {
                for (PaymentInvoiceMapDTO paymentInvoiceMapDTO : paymentsLinkedToCurrentInvoiceMap) {
                    if (!newCustomerPayments.contains(paymentInvoiceMapDTO.getPayment()) || invoiceCount >= 2) {

                        // Balance payment received for existing customer
                        if (paymentInvoiceMapDTO.getPayment().getIsRefund() == 0) {
                            sumOfpaymentsLinkedToCurrentInvoice = sumOfpaymentsLinkedToCurrentInvoice.add(paymentInvoiceMapDTO.getAmount());
                        } else {
                            sumOfpaymentsLinkedToCurrentInvoice = sumOfpaymentsLinkedToCurrentInvoice.subtract(paymentInvoiceMapDTO.getAmount());
                        }
                    }
                }
            }

            parameters.put("sumOfpaymentsLinkedToCurrentInvoice",sumOfpaymentsLinkedToCurrentInvoice.setScale(2, RoundingMode.HALF_UP));

            if(CollectionUtils.isNotEmpty(existingCustomerPayments) && invoiceCount >= 2 ) { // if invoice count is greater than 2 means it is old customer.

                for (PaymentInvoiceMapDTO paymentInvoiceMapDTO : existingCustomerPayments) {
                    Integer isRefund = paymentDAS.find(paymentInvoiceMapDTO.getPayment().getId()).getIsRefund();
                    if (isRefund == 0) {
                        totalPayment = totalPayment.add(paymentInvoiceMapDTO.getAmount());
                    } else {
                        // If payment is refund then subtract it.
                        totalPayment = totalPayment.subtract(paymentInvoiceMapDTO.getAmount());
                    }
                }
            } else if (invoiceCount < 2 && !newCustomerPayments.isEmpty()) { // if invoice count is less than 2 means it is new customer.
                for (PaymentDTO value : newCustomerPayments) {
                    if (value.getIsRefund() == 0) {
                        totalPayment = totalPayment.add(value.getAmount());
                    } else {
                        // If payment is refund then subtract it.
                        totalPayment = totalPayment.subtract(value.getAmount());
                    }
                }
            }
            parameters.put("totalPaymentsReceived", null != totalPayment ? totalPayment.setScale(2, RoundingMode.HALF_UP):"0.00");
            // If multiple payments found it will appear in detailed section of summary of account history invoice template.
            if (invoiceCount >= 2) {
                parameters.put("paymentCount",CollectionUtils.isNotEmpty(existingCustomerPayments) ?	Integer.valueOf(existingCustomerPayments.size() + (null != unlinkedPayments ? unlinkedPayments.size():0)):0);
            } else {
                parameters.put("paymentCount",!newCustomerPayments.isEmpty() ? Integer.valueOf(newCustomerPayments.size()):0);
            }
            LOG.debug("totalPaymentsReceived.size(): "+(CollectionUtils.isNotEmpty(existingCustomerPayments) ? existingCustomerPayments.size() : 0));
            if (CollectionUtils.isNotEmpty(existingCustomerPayments) && existingCustomerPayments.size() == 1) {
                // If old customer's invoice have only single payment then set last payment date.
                parameters.put(LAST_PAYMENT_DATE,existingCustomerPayments.get(0).getPayment().getPaymentDate());
                parameters.put(LAST_PAYMENT_AMOUNT,existingCustomerPayments.get(0).getPayment().getAmount().setScale(2, RoundingMode.HALF_UP));
            } else if ( null != newCustomerPayments && !newCustomerPayments.isEmpty() && newCustomerPayments.size() == 1) {
                // If new customer's invoice have only single payment then set last payment date.
                parameters.put(LAST_PAYMENT_DATE,newCustomerPayments.get(0).getPaymentDate());
                parameters.put(LAST_PAYMENT_AMOUNT,newCustomerPayments.get(0).getAmount().setScale(2, RoundingMode.HALF_UP));
            } else {
                parameters.put(LAST_PAYMENT_DATE, null);
                parameters.put(LAST_PAYMENT_AMOUNT, null);
            }

            // the company logo is a file

            File companyLogo = getLogoFile(com.sapienter.jbilling.common.Util
                    .getSysProp("base_dir") + "logos/fc-logos/" , "entity-"+entityId);


            if (null == companyLogo || !companyLogo.exists()) {
                companyLogo = getLogoFile(com.sapienter.jbilling.common.Util
                        .getSysProp("base_dir")+ "logos/", "entity-1");
            }

            parameters.put("companyLogo", companyLogo);
            parameters.put("entityId", entityId);
            parameters.put("companyName", new EntityBL(entityId).getEntity().getDescription());

            ContactDTO companyDetails = contactDas.findEntityContact(entityId);
            parameters.put("companyAddress1", companyDetails.getAddress1());
            parameters.put("companyAddress2", companyDetails.getAddress2());
            parameters.put("companyCity", companyDetails.getCity());
            parameters.put("companyPostalCode", companyDetails.getPostalCode());
            parameters.put("companyStateCode", companyDetails.getStateProvince());

            String serviceNumber = "";
            ContactDTO adminDetails = contactDas.findByEntityAndUserName("admin", entityId);
            if (null != adminDetails &&
                    null != adminDetails.getPhoneCountryCode() &&
                    null != adminDetails.getPhoneAreaCode() &&
                    !adminDetails.getPhoneNumber().isEmpty()) {
                serviceNumber =	adminDetails.getPhoneCountryCode()
                        +"-"+adminDetails.getPhoneAreaCode()
                        +"-"+adminDetails.getPhoneNumber();
            }
            parameters.put("serviceNumber", serviceNumber);

            InvoiceDTO invoiceDTO = invoiceDAS.find(invoice.getId());

            // Retrieve Penalty Item Id from OverdueInvoicePenaltyTask
            String penaltyItemId = pluggableTaskParameterDAS.getPenaltyItemId(entityId);
            LOG.debug("Penalty Item Id "+penaltyItemId);
            BigDecimal lateFeePercentage = BigDecimal.ZERO;
            if (null != penaltyItemId) {
                // Retrieve late fee percentage
                ItemDTO item = itemDAS.find(Integer.valueOf(penaltyItemId));
                lateFeePercentage = item.getPrice(new Date(),entityId).getRate();
                lateFeePercentage = null != lateFeePercentage ? lateFeePercentage : BigDecimal.ZERO;
            }
            parameters.put("lateFeePercentage", lateFeePercentage);
            LOG.debug("Late Fee Percentage "+lateFeePercentage);

            BigDecimal balanceForward = BigDecimal.ZERO;
            balanceForward = balanceForward.add(prevInvoiceTotalWithOutCurrencySymbol);
            balanceForward = balanceForward.subtract(totalPayment);

            parameters.put("balanceForward",null != balanceForward ? balanceForward.setScale(2, RoundingMode.HALF_UP) : null);
            Integer feesProductCategoryId = null;
            Integer adjustmentsProductCategoryId = null;
            try {
                feesProductCategoryId = Integer.valueOf(new MetaFieldDAS().getComapanyLevelMetaFieldValue(
                        FEES_PRODUCT_CATEGORY_ID, entityId));
                adjustmentsProductCategoryId = Integer.valueOf(new MetaFieldDAS().getComapanyLevelMetaFieldValue(
                        ADJUSTMENTS_PRODUCT_CATEGORY_ID, entityId));
            } catch(NumberFormatException e) {
                LOG.error(PAPER_INVOICE_EXCEPTION, e);
            }

            parameters.put("feesProductCategoryId",feesProductCategoryId);
            LOG.debug("Fees Product Category Id: "+feesProductCategoryId);
            parameters.put("adjustmentsProductCategoryId",adjustmentsProductCategoryId);
            LOG.debug("Adjustments Product Category Id: "+adjustmentsProductCategoryId);

            List<ItemDTO> feesProducts = itemDAS.findAllByItemType(feesProductCategoryId);
            List<ItemDTO> adjustmentsProducts = itemDAS.findAllByItemType(adjustmentsProductCategoryId);

            BigDecimal planOverageCharges = BigDecimal.ZERO;
            BigDecimal otherCharges = BigDecimal.ZERO;

            if (invoiceDTO.getDeleted() == 0 && !invoiceDTO.isReviewInvoice()) {
                for (InvoiceLineDTO invoiceLine : invoiceDTO.getInvoiceLines()) {
                    if (null != invoiceLine.getCallIdentifier() && !invoiceLine.isReviewInvoiceLine()) {
                        planOverageCharges = planOverageCharges.add(invoiceLine.getAmount());
                    }
                    if (null == invoiceLine.getCallIdentifier() && (!invoiceLine.getDescription().contains("Carried Invoice")
                            && null != invoiceLine.getItem()) &&
                            !feesProducts.contains(invoiceLine.getItem()) &&
                            !adjustmentsProducts.contains(invoiceLine.getItem()) &&
                            invoiceLine.getInvoiceLineType().getId() !=
                            Constants.INVOICE_LINE_TYPE_TAX) {
                        otherCharges = otherCharges.add(invoiceLine.getAmount());
                    }
                }
            }

            parameters.put("planOverageCharges",planOverageCharges.setScale(2, RoundingMode.HALF_UP));

            LOG.debug("Other Charges "+otherCharges.setScale(2, RoundingMode.HALF_UP));
            parameters.put("otherCharges",otherCharges.setScale(2, RoundingMode.HALF_UP));

            String primaryAccountNumber = getPrimaryAccountNumberByCustomerIdAndEntityId(invoice.getUserId(), entityId);
            if (null != primaryAccountNumber && !primaryAccountNumber.isEmpty())
                parameters.put("accountNumber", primaryAccountNumber);

            parameters.put("billing_cycle",(null != new CustomerBL(customerDAS.getCustomerId(invoice.getUserId())) ? 
                    new CustomerBL(customerDAS.getCustomerId(invoice.getUserId())).getEntity().getMainSubscription().getSubscriptionPeriod().getDescription().toString() :null));

        } catch (Exception e) {
            LOG.error(PAPER_INVOICE_EXCEPTION, e);
        }
    }

    private static File getLogoFile(String filePath, String fileName) {
        Path path = Paths.get(filePath);
        if(Files.exists(path)) {
            File [] logos = path.toFile()
                                .listFiles((file, name) ->  name.startsWith(fileName));
            if(null!= logos && logos.length > 0 ) {
                return logos [logos.length -1];
            }
        }
        return null;
    }

    private static void generatePaperInvoiceNew(Map<String, Object> parameters,
            InvoiceDTO invoice, ContactDTOEx to, Integer entityId) throws FileNotFoundException,
            SessionInternalError {
        try{

            parameters.put("companyName", new EntityBL(entityId).getEntity().getDescription());

            ContactDTO companyDetails = contactDas.findEntityContact(entityId);
            parameters.put("companyAddress1", companyDetails.getAddress1());
            parameters.put("companyAddress2", companyDetails.getAddress2());
            parameters.put("companyCity", companyDetails.getCity());
            parameters.put("companyPostalCode", companyDetails.getPostalCode());
            parameters.put("companyStateCode", companyDetails.getStateProvince());

            if (null != to) {
                CustomerDTO customer = new UserDAS().find(invoice.getUserId()).getCustomer();

                parameters.put("customerFirstName",to.getFirstName());
                parameters.put("customerLastName",to.getLastName());

                String mailingGroupNameValue = new MetaFieldDAS().getComapanyLevelMetaFieldValue(MetaFieldName.NOTIFICATION_MAILING_ADDRESS.getMetaFieldName(), entityId);
                MetaFieldGroup metaFieldGroup = accountInformationTypeDAS.getGroupByNameAndEntityId(entityId, EntityType.ACCOUNT_TYPE, mailingGroupNameValue, customer.getAccountType().getId());

                if (null != metaFieldGroup) {

                    MetaFieldDAS metaFieldDAS = new MetaFieldDAS();

                    Date effectiveDate = TimezoneHelper.companyCurrentDate(entityId);

                    Map<String, String> aitMetaFieldsBYUsage = metaFieldDAS
                            .getCustomerAITMetaFieldValueMapByMetaFieldType(customer.getId(), metaFieldGroup.getId(), effectiveDate);

                    List<String> orgNames = Arrays.asList(MetaFieldType.ORGANIZATION.toString(), MetaFieldType.FIRST_NAME.toString());

                    for(String name : orgNames) {
                        if(aitMetaFieldsBYUsage.get(name)!= null) {
                            parameters.put("customerOrganizationName", aitMetaFieldsBYUsage.get(name));
                            break;
                        }
                    }

                    parameters.put("customerAddress", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.ADDRESS1.toString(), ""));
                    parameters.put("customerAddress2", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.ADDRESS2.toString(), ""));
                    parameters.put("customerCity", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.CITY.toString(), ""));
                    parameters.put("customerState", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.STATE_PROVINCE.toString(), ""));
                    parameters.put("customerCountry", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.COUNTRY_CODE.toString(), ""));
                    parameters.put("customerPostalCode", aitMetaFieldsBYUsage.getOrDefault(MetaFieldType.POSTAL_CODE.toString(), ""));
                    parameters.put("customerOrganization", printable(to.getOrganizationName()));
                    parameters.put("customerName",printable(to.getFirstName(), to.getLastName()));
                }

                // tax calculated
                BigDecimal taxTotal = new BigDecimal(0);
                String taxPrice = "";
                BigDecimal taxAmount = BigDecimal.ZERO;
                String productCode;
                List<InvoiceLineDTO> lines = new ArrayList<InvoiceLineDTO>(invoice.getInvoiceLines());
                // Temp change: sort is leading to NPE

                BigDecimal lineAmountSum = BigDecimal.ZERO;
                for (InvoiceLineDTO line: lines) {
                    // process the tax, if this line is one
                    if (line.getInvoiceLineType() != null && // for headers/footers
                            line.getInvoiceLineType().getId() ==
                            Constants.INVOICE_LINE_TYPE_TAX) {
                        // update the total tax variable
                        taxTotal = taxTotal.add(null != line.getAmount() ?
                                line.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP):BigDecimal.ZERO);
                        productCode = line.getItem() != null ? line.getItem().getInternalNumber() : line.getDescription();
                        taxPrice += productCode+" "+line.getPrice().setScale(2, BigDecimal.ROUND_HALF_UP).toString()+" %\n";
                        taxAmount = taxAmount.add(null != line.getAmount() ?
                                line.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP):BigDecimal.ZERO);
                        lineAmountSum = lineAmountSum.add(line.getAmount());
                    }
                }
                // JBFC-389
                BigDecimal creditPaymentAmount;
                creditPaymentAmount = paymentDAS.getCreditPaymentAmount(invoice.getId());
                LOG.debug("Credit Payment : "+creditPaymentAmount);

                BigDecimal vatAmountOfCreditPayment =
                        creditPaymentAmount != new BigDecimal(0) ?
                                creditPaymentAmount.subtract(creditPaymentAmount.divide(BigDecimal.valueOf(1.2), RoundingMode.HALF_UP)):BigDecimal.ZERO;

                                LOG.debug("Vat amount of credit payment: "+vatAmountOfCreditPayment);

                                taxAmount = lineAmountSum.subtract(vatAmountOfCreditPayment).setScale(2, BigDecimal.ROUND_HALF_UP);
                                taxPrice = "".equals(taxPrice)?"0.00 %":taxPrice.substring(0,taxPrice.lastIndexOf('\n'));

                                parameters.put("sales_tax",taxTotal);
                                parameters.put("tax_price", printable(taxPrice));
                                parameters.put("tax_amount", taxAmount);
            }
        } catch (Exception e) {
            LOG.error(PAPER_INVOICE_EXCEPTION, e);
        }
    }

    private static String getPrimaryAccountNumberByCustomerIdAndEntityId(Integer userId, Integer entityId) {
        if(null == userId || null == entityId) {
            return null;
        }
        return customerDAS.getPrimaryAccountNumberByUserAndEntityId(userId, entityId);
    }

    public static String printable(String str) {
        if (str == null) {
            return "";
        }
        return str;
    }

    /**
     * Safely concatenates 2 strings together with a blank space (" "). Null strings
     * are handled safely, and no extra concatenated character will be added if one
     * string is null.
     *
     * @param str
     * @param str2
     * @return concatenated, printable string
     */
    private static String printable(String str, String str2) {
        StringBuilder builder = new StringBuilder();

        if (str != null) builder.append(printable(str)).append(' ');
        if (str2 != null) builder.append(printable(str2));

        return builder.toString();
    }

    private static String evaluateCurrencySymbol(InvoiceDTO invoice) {
        CurrencyBL currency = new CurrencyBL(invoice.getCurrency().getId());
        String symbol = currency.getEntity().getSymbol();
        if (symbol.length() >= 4 && symbol.charAt(0) == '&' &&
                symbol.charAt(1) == '#') {
            // this is an html symbol
            // remove the first two digits
            symbol = symbol.substring(2);
            // remove the last digit (;)
            symbol = symbol.substring(0, symbol.length() - 1);
            // convert to a single char
            Character ch = new Character((char)
                    Integer.valueOf(symbol).intValue());
            symbol = ch.toString();
        }
        return symbol;
    }

    public Date getParameter(String key) throws ParseException {
        String value = (String) parameters.get(key);
        if (value == null || value.trim().equals(""))
            return null;
        Date date = new SimpleDateFormat("yyyyMMdd").parse(value);
        LOG.info("In getParameter with key=" + key + " and value=" + value);
        return date;
    }
}
