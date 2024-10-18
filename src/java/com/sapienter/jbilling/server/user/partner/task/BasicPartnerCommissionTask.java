/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.user.partner.task;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.customerEnrollment.db.CustomerEnrollmentDAS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.invoice.db.InvoiceLineDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.PartnerBL;
import com.sapienter.jbilling.server.user.partner.PartnerCommissionType;
import com.sapienter.jbilling.server.user.partner.PartnerType;
import com.sapienter.jbilling.server.user.partner.db.CommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessConfigurationDTO;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDAS;
import com.sapienter.jbilling.server.user.partner.db.CommissionProcessRunDTO;
import com.sapienter.jbilling.server.user.partner.db.CustomerCommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.InvoiceCommissionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionExceptionDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerCommissionValueDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerDAS;
import com.sapienter.jbilling.server.user.partner.db.PartnerDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerReferralCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PaymentCommissionDAS;
import com.sapienter.jbilling.server.user.partner.db.PaymentCommissionDTO;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.util.PreferenceBL;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This implementation calculates the commissions for the partners,
 * for the period configured on the CommissionProcessConfigurationDTO
 */
public class BasicPartnerCommissionTask extends PluggableTask implements IPartnerCommissionTask{
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BasicPartnerCommissionTask.class));

    public static final BigDecimal ONE_HUNDRED = new BigDecimal("100.00");
    private Date startDate;
    private Date endDate;

    private InvoiceDAS invoiceDAS = null;
    private PaymentCommissionDAS paymentCommissionDAS = null;
    private PartnerCommissionDAS partnerCommissionDAS = null;
    private PartnerBL partnerBL = null;

    private CommissionProcessConfigurationDAS configurationDAS = null;
    private CommissionProcessRunDAS processRunDAS = null;
    private CompanyDAS companyDAS = null;
    private CommissionDAS commissionDAS = null;
    private PartnerDAS partnerDAS = null;
    private PartnerReferralCommissionDAS partnerReferralCommissionDAS = null;
    private CustomerEnrollmentDAS customerEnrollmentDAS = null;
    private PlatformTransactionManager transactionManager;

    private Map<Integer, Integer> companyMinPeriod = new HashMap<>();

    /**
     * Calculates all the agents' commissions for the given entity
     *
     * @param entityId company id
     */
    @Override
    public void calculateCommissions (Integer entityId) {
        init();
        CompanyDTO entity = companyDAS.find(entityId);

        //Get the commission process configuration.
        CommissionProcessConfigurationDTO configuration = configurationDAS.findByEntity(entity);
        startDate = configuration.getNextRunDate();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(startDate);

        if (CalendarUtils.isSemiMonthlyPeriod(configuration.getPeriodUnit())) {
            for(int i = 0; i<configuration.getPeriodValue(); i++) {
                calendar.setTime(CalendarUtils.addSemiMonthyPeriod(calendar.getTime()));
            }
        } else {
            calendar.add(MapPeriodToCalendar.map(configuration.getPeriodUnit().getId()), configuration.getPeriodValue());
        }

        endDate = calendar.getTime();

        LOG.debug("Calculating commissions for entity: %s, periodStart: %s, periodEnd: %s ", entity.getId(), startDate, endDate );

        CommissionContext commissionContext = new CommissionContext();
        commissionContext.entityId = entityId;

        //Create the commissionProcessRun object and save it.
        createCommissionRun(commissionContext);

        //Calculate the invoiceCommissions.
        calculateInvoiceCommissions(commissionContext);

        //Calculate referral commissions.
        partnerBL.calculateReferralCommissions(commissionContext.processRun);

        //Calculate the actual commissions.
        partnerBL.calculateCommissions(commissionContext.processRun);

        if(commissionContext.errorCount == 0) {
            CommissionProcessRunDTO commissionProcessRun =  commissionContext.processRun;
            commissionProcessRun.setErrorCount(commissionContext.errorCount);
            processRunDAS.reattach( commissionContext.processRun );

            //update configuration with new nextRunDate.
            Date nextRunDate = new DateTime(endDate).plusDays(1).toDate();
            configuration.setNextRunDate(nextRunDate);
            configurationDAS.save(configuration);
        }
    }

    private void init() {
        if(invoiceDAS == null) invoiceDAS = new InvoiceDAS();
        if(paymentCommissionDAS == null) paymentCommissionDAS = new PaymentCommissionDAS();
        if(partnerCommissionDAS == null) partnerCommissionDAS = new PartnerCommissionDAS();

        if(configurationDAS == null) configurationDAS = new CommissionProcessConfigurationDAS();
        if(processRunDAS == null) processRunDAS = new CommissionProcessRunDAS();
        if(companyDAS == null) companyDAS = new CompanyDAS();
        if(commissionDAS == null) commissionDAS = new CommissionDAS();
        if(partnerDAS == null) partnerDAS = new PartnerDAS();
        if(partnerReferralCommissionDAS  == null) partnerReferralCommissionDAS = new PartnerReferralCommissionDAS();
        if(customerEnrollmentDAS ==null) customerEnrollmentDAS = new CustomerEnrollmentDAS();
        if(transactionManager == null) transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);

        if(partnerBL == null) {
            partnerBL = new PartnerBL();
            partnerBL.setCommissionDAS(commissionDAS);
            partnerBL.setPartnerCommissionDAS(partnerCommissionDAS);
            partnerBL.setPartnerReferralCommissionDAS(partnerReferralCommissionDAS);
            partnerBL.setPartnerDAS(partnerDAS);
        }
    }

    private void calculateInvoiceCommissions(CommissionContext commissionContext){
        LOG.debug("Started calculating the invoice commissions.");

        List<PartnerDTO> partners = partnerDAS.findPartnersByCompany(commissionContext.entityId);
        for(PartnerDTO partner : partners){
            processPartner(commissionContext, partner.getId());
        }
    }

    private void createCommissionRun(CommissionContext commissionContext) {
        TransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction( def );
        try {
            //Create the commissionProcessRun object and save it.
            CommissionProcessRunDTO commissionProcessRun = new CommissionProcessRunDTO();
            commissionProcessRun.setEntity(companyDAS.find(commissionContext.entityId));
            commissionProcessRun.setRunDate(companyCurrentDate());
            commissionProcessRun.setPeriodStart(startDate);
            commissionProcessRun.setPeriodEnd(endDate);
            commissionProcessRun = processRunDAS.save(commissionProcessRun);
            commissionContext.processRun = commissionProcessRun;
            processRunDAS.flush(); //we are flushing because we will execute each partner in a new transaction and need the CommissionProcessRunDTO.id
            processRunDAS.detach(commissionProcessRun);
            transactionManager.commit( status );

            commissionContext.processRun = commissionProcessRun;
        } catch (Exception e) {
            LOG.error("Exception while creating commision run", e);
            transactionManager.rollback(status);
            throw e;
        }
        partnerDAS.flush();
        partnerDAS.clear();
    }

    /**
     * Process a single partner in a new transaction.
     *
     * @param commissionContext
     * @param partnerId
     */
    private void processPartner(CommissionContext commissionContext, int partnerId) {
        TransactionDefinition def = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction( def );
        try {
            PartnerDTO partner = partnerDAS.find(partnerId);
            LOG.debug("Processing partner %s", partner);
            CommissionProcessRunDTO commissionProcessRun = commissionContext.processRun;
            Integer entityId = commissionContext.entityId;

            PartnerCommissionType commissionType = findCommissionType(partner, entityId);

            List<Integer> invoiceIds = null;
            if(commissionType.equals(PartnerCommissionType.PAYMENT)){
                invoiceIds = paymentCommissionDAS.findInvoiceIdsByPartner(partner);
            }else{
                //get the invoices for the current partner for the period
                invoiceIds = invoiceDAS.findForPartnerCommissions(partner.getId(), endDate);
            }


            for(Integer invoiceId: invoiceIds){
                InvoiceDTO invoice = invoiceDAS.find(invoiceId);
                LOG.debug("Calculating commission for invoice: %s", invoice.getId());

                //If we have an invoice was received after the customer was dropped, we have to check if it is within the minimum period
                if(customerDroppedInMinimumTimePeriod(invoice)) {
                    continue;
                }

                findCustomerEnrollmentDateAndPartnerRate(commissionContext, invoice, partner);

                if(commissionType.equals(PartnerCommissionType.CUSTOMER)) {
                    LOG.debug("Customer based commission");
                    if(!commissionContext.customerIdsForCustomerCommission.contains(invoice.getUserId())) {
                        createCustomerCommission(commissionProcessRun, partner, invoice);
                        commissionContext.customerIdsForCustomerCommission.add(invoice.getUserId());
                    } else {
                        LOG.debug("Customer commission already calculated for customer %s", invoice.getUserId());
                    }
                } else {
                    //Create the commission objects.
                    InvoiceCommissionDTO invoiceCommission = new InvoiceCommissionDTO();
                    invoiceCommission.setInvoice(invoice);
                    invoiceCommission.setPartner(partner);
                    invoiceCommission.setCommissionProcessRun(commissionProcessRun);

                    //Calculate how much the invoice is payed and calculate a ratio between 0 & 1.
                    BigDecimal paidRatio = BigDecimal.ZERO;
                    if(commissionType.equals(PartnerCommissionType.PAYMENT)) {
                        LOG.debug("Payment based commission");
                        List<PaymentCommissionDTO> paymentCommissions = paymentCommissionDAS.findByInvoiceId(invoice.getId());
                        for(PaymentCommissionDTO paymentCommission : paymentCommissions){
                            paidRatio = paidRatio.add(paymentCommission.getPaymentAmount());
                        }

                        paidRatio = paidRatio.divide(invoice.getTotal(), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    } else {
                        LOG.debug("Invoice based commission");
                        //For invoice based commissions the ratio is 1
                        paidRatio = BigDecimal.ONE;
                    }

                    BigDecimal discountPercentage = BigDecimal.ZERO;

                    for (InvoiceLineDTO invoiceLine: invoice.getInvoiceLines()){
                        if(invoiceLine.getItem() != null){
                            BigDecimal percentage = invoiceLine.getItem().getPercentage();

                            if(percentage != null && !percentage.equals(BigDecimal.ZERO)){
                                discountPercentage = discountPercentage.add(percentage);
                            }else{
                                calculateInvoiceLinesCommission(commissionContext, invoiceLine, partner, invoiceCommission, paidRatio);
                            }
                        }
                    }

                    //Apply invoice discounts & taxes
                    if(!discountPercentage.equals(BigDecimal.ZERO)) {
                        BigDecimal percentage = discountPercentage.divide(ONE_HUNDRED);
                        if(invoiceCommission.getStandardAmount() != null) {
                            invoiceCommission.setStandardAmount(invoiceCommission.getStandardAmount().multiply(percentage));
                        }
                        if(invoiceCommission.getMasterAmount() != null) {
                            invoiceCommission.setMasterAmount(invoiceCommission.getMasterAmount().multiply(percentage));
                        }
                        if(invoiceCommission.getExceptionAmount() != null) {
                            invoiceCommission.setExceptionAmount(invoiceCommission.getExceptionAmount().multiply(percentage));
                        }
                    }

                    partnerCommissionDAS.save(invoiceCommission);
                    LOG.debug("Created invoice commission object, invoice: %s, partner %s", invoice.getId(), partner.getId());
                    LOG.debug("Standard amount: %s", invoiceCommission.getStandardAmount());
                    LOG.debug("Master amount: %s", invoiceCommission.getMasterAmount());
                    LOG.debug("Exception amount: %s", invoiceCommission.getExceptionAmount());
                }
            }

            //delete paymentCommissions
            paymentCommissionDAS.deleteAllForPartner(partner.getId());
            transactionManager.commit( status );
        } catch (Exception e) {
            LOG.error("Exception while processing partner with id "+partnerId, e);
            transactionManager.rollback(status);
            commissionContext.errorCount++;
        }
        partnerDAS.flush();
        partnerDAS.clear();
    }

    /**
     * Check if the customer was dropped within the minimum period required to receive a commission
     * @param invoice
     * @return
     */
    private boolean customerDroppedInMinimumTimePeriod(InvoiceDTO invoice) {
        UserDTO user = invoice.getBaseUser();

        if(user.getCustomer().isTerminatedOrDropped()) {
            List<OrderDTO> subscriptions = new OrderDAS()
                    .findByUserSubscriptions(user.getId());
            for(OrderDTO order : subscriptions) {
                if(order.getActiveUntil() != null) {
                    Date activeUntil = order.getActiveUntil();
                    Date lastEnrollmentDate = new CustomerEnrollmentDAS().findCustomerEnrollmentDate(user.getEntity().getId(),
                            user.getCustomer().getCustomerAccountInfoTypeMetaField(FileConstants.UTILITY_CUST_ACCT_NR).getMetaFieldValue().getValue().toString());
                    if(lastEnrollmentDate == null) {
                        LOG.warn("No last enrollment date for customer %s", user);
                        return false;
                    }

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(lastEnrollmentDate);
                    calendar.add(Calendar.MONTH, loadEntityCancellationPeriod(user.getCompany()));
                    Date reversalThreshold = calendar.getTime();

                    return (!activeUntil.after(reversalThreshold));
                }
            }
        }
        return false;
    }

    /**
     * Get the minimum period that a customer must be enrolled for the entity in order to generate commission
     * @param company
     * @return
     */
    private int loadEntityCancellationPeriod(CompanyDTO company) {
        Integer value = companyMinPeriod.get(company.getId());
        if(value != null) {
            return value;
        }
        MetaFieldValue<BigDecimal> metaFieldValue = company.getMetaField(FileConstants.COMMISSION_MIN_DAYS_META_FIELD_NAME);
        if(metaFieldValue != null) {
            value = ((BigDecimal)metaFieldValue.getValue()).intValue();
        } else {
            value = Integer.valueOf(0);
        }
        companyMinPeriod.put(company.getId(), value);
        return value;
    }

    /**
     * Find the enrollment date for the customer and the applicable commission rate for the partner based on the date.
     * @param context
     * @param invoice
     * @param partner
     */
    private void findCustomerEnrollmentDateAndPartnerRate(CommissionContext context, InvoiceDTO invoice, PartnerDTO partner) {
        UserDTO user = invoice.getBaseUser();
        try {
            context.customerEnrollmentDate = customerEnrollmentDAS.findCustomerEnrollmentDate(context.entityId, (String)user.getCustomer().getCustomerAccountInfoTypeMetaField(FileConstants.UTILITY_CUST_ACCT_NR).getMetaFieldValue().getValue());
        } catch (Exception e) {
            LOG.warn("User does not have an enrollment will use user create date: %s", user.getId());
            context.customerEnrollmentDate = user.getCreateDatetime();
        }
        context.customerRate = user.findCommissionRate(partner.getId());
        //we only need a rate from the partner if there is no customer specific rate
        if(context.customerRate == null) {
            Duration duration = Duration.between(context.customerEnrollmentDate.toInstant(), invoice.getCreateDatetime().toInstant());
            long days = duration.toDays();
            BigDecimal rate = BigDecimal.ZERO;
            for(PartnerCommissionValueDTO commissionValue : partner.getCommissionValues()) {
                if(commissionValue.getDays() < days || commissionValue.getDays() == 0) {
                    rate = commissionValue.getRate();
                }
            }
            context.rate = rate;
        }

        LOG.debug("Enrollment Date: %s", context.customerEnrollmentDate);
        LOG.debug("Rate: %s", context.rate);
    }

    /**
     * Called when we need to create a customer based commission. It will create one commission per customer.
     *
     * @param commissionProcessRun
     * @param partner
     * @param invoice
     */
    private void createCustomerCommission(CommissionProcessRunDTO commissionProcessRun, PartnerDTO partner, InvoiceDTO invoice) {
        UserDTO user = invoice.getBaseUser();
        List<CustomerCommissionDTO> customerCommissionList = partnerCommissionDAS.findCustomerCommission(user, partner);
        if(!customerCommissionList.isEmpty()) {
            LOG.debug("Customer commission already exists for user %s", user);
            return;
        }
        CustomerCommissionDTO customerCommission = new CustomerCommissionDTO();
        customerCommission.setUser(user);
        customerCommission.setPartner(partner);
        customerCommission.setCommissionProcessRun(commissionProcessRun);
        BigDecimal rate = user.findCommissionRate(partner.getId());
        if(rate == null) {
            rate = partner.getCommissionValues().get(0).getRate();
        }
        customerCommission.setAmount(rate);
        partnerCommissionDAS.save(customerCommission);
        LOG.debug("Created customer commission object, invoice: %s, partner %s", invoice.getId(), partner.getId());
        LOG.debug("Customer amount: %s", customerCommission.getAmount());
    }

    private PartnerCommissionType findCommissionType(PartnerDTO partner, Integer entityId){
        if(partner.getCommissionType() == null) {
            return getCommissionTypePreference(entityId);
        } else {
            return partner.getCommissionType();
        }
    }

    private void calculateInvoiceLinesCommission (CommissionContext commissionContext, InvoiceLineDTO invoiceLine, PartnerDTO partner, InvoiceCommissionDTO invoiceCommission, BigDecimal paidRatio) {
        if(paidRatio == null || paidRatio.equals(BigDecimal.ZERO))
            return;

        boolean isCommissionException = false;

        PartnerCommissionType commissionType = findCommissionType(partner, commissionContext.entityId);

        //customer specific rate has the highest priority. only look at exceptions if there is no customer rate
        if(commissionContext.customerRate == null) {
            for (PartnerCommissionExceptionDTO commissionException: partner.getCommissionExceptions()){
                if(commissionException.getItem().equals(invoiceLine.getItem())){
                    //Check if the commission exception is valid.
                    if(!PartnerBL.isCommissionValid(commissionException.getStartDate(), commissionException.getEndDate(),
                            invoiceLine.getInvoice().getCreateDatetime())){
                        break;
                    }

                    BigDecimal lineBaseAttribution = null;
                    BigDecimal percentage = commissionException.getPercentage();
                    if(PartnerCommissionType.CONSUMPTION.equals(commissionType)) {
                        MetaFieldValue<String> commodity = invoiceLine.getItem().getMetaField(FileConstants.COMMODITY);
                        if(commodity != null && !commodity.getValue().isEmpty()) {
                            lineBaseAttribution = invoiceLine.getQuantity().multiply(commissionException.getPercentage());
                      }
                    } else {
                        lineBaseAttribution = invoiceLine.getAmount();
                    }
                    invoiceCommission.setExceptionAmount(calculateCommissionAmount(commissionType,
                            invoiceCommission.getExceptionAmount(),
                            percentage,
                            lineBaseAttribution,
                            paidRatio,
                            percentage));

                    isCommissionException = true;
                    break;
                }
            }
        }

        if(!isCommissionException) {
            BigDecimal lineBaseAttribution = null;
            BigDecimal rate = null;
            if(PartnerCommissionType.CONSUMPTION.equals(commissionType)) {
                MetaFieldValue<String> commodity = invoiceLine.getItem().getMetaField(FileConstants.COMMODITY);
                if(commodity == null || commodity.getValue().isEmpty()) {
                    return;
                }
                lineBaseAttribution = invoiceLine.getQuantity();
            } else {
                lineBaseAttribution = invoiceLine.getAmount();
            }
            if(commissionContext.customerRate != null) {
                rate = commissionContext.customerRate;
            } else {
                rate = commissionContext.rate;
            }

            if(partner.getType().equals(PartnerType.STANDARD)) {
                invoiceCommission.setStandardAmount(calculateCommissionAmount(commissionType,
                        invoiceCommission.getStandardAmount(),
                        invoiceLine.getItem().getStandardPartnerPercentage(),
                        lineBaseAttribution,
                        paidRatio,
                        rate));
            } else {
                invoiceCommission.setMasterAmount(calculateCommissionAmount(commissionType,
                        invoiceCommission.getMasterAmount(),
                        invoiceLine.getItem().getMasterPartnerPercentage(),
                        lineBaseAttribution,
                        paidRatio,
                        rate));
            }
        }
    }

    /**
     * Calculates the percentage of the newAmount adding it to the currentAmount.
     * This value is multiplied with the paidRatio (between 0 & 1) before returning the value.
     * @param currentAmount
     * @param percentage
     * @param newAmount
     * @param paidRatio
     * @return
     */
    private BigDecimal calculateCommissionAmount (PartnerCommissionType commissionType, BigDecimal currentAmount, BigDecimal percentage, BigDecimal newAmount, BigDecimal paidRatio, BigDecimal rate) {

        if((commissionType.equals(PartnerCommissionType.INVOICE) || commissionType.equals(PartnerCommissionType.PAYMENT)) && percentage != null && newAmount != null){
            return currentAmount.add(percentage.divide(ONE_HUNDRED).multiply(newAmount)).multiply(paidRatio);
        } else if(commissionType.equals(PartnerCommissionType.CONSUMPTION) && newAmount != null) {
            return currentAmount.add(newAmount.multiply(rate));
        } else if((commissionType.equals(PartnerCommissionType.INVOICE) || commissionType.equals(PartnerCommissionType.PAYMENT)) && newAmount != null) {
            return currentAmount.add(newAmount.multiply(rate.divide(ONE_HUNDRED)));
        } else {
            return currentAmount;
        }
    }

    /**
     * Gets the preference defined for the company.
     * @param entityId
     * @return PartnerCommissionType
     */
    private PartnerCommissionType getCommissionTypePreference(Integer entityId){
        String prefValue = new PreferenceBL(entityId, CommonConstants.PREFERENCE_PARTNER_DEFAULT_COMMISSION_TYPE).getString();
        return PartnerCommissionType.valueOf(prefValue);
    }

    void setInvoiceDAS(InvoiceDAS invoiceDAS) {
        this.invoiceDAS = invoiceDAS;
    }

    void setPaymentCommissionDAS(PaymentCommissionDAS paymentCommissionDAS) {
        this.paymentCommissionDAS = paymentCommissionDAS;
    }

    void setPartnerCommissionDAS(PartnerCommissionDAS partnerCommissionDAS) {
        this.partnerCommissionDAS = partnerCommissionDAS;
    }

    void setConfigurationDAS(CommissionProcessConfigurationDAS configurationDAS) {
        this.configurationDAS = configurationDAS;
    }

    void setProcessRunDAS(CommissionProcessRunDAS processRunDAS) {
        this.processRunDAS = processRunDAS;
    }

    void setCompanyDAS(CompanyDAS companyDAS) {
        this.companyDAS = companyDAS;
    }

    void setCommissionDAS(CommissionDAS commissionDAS) {
        this.commissionDAS = commissionDAS;
    }

    void setPartnerDAS(PartnerDAS partnerDAS) {
        this.partnerDAS = partnerDAS;
    }

    void setPartnerReferralCommissionDAS(PartnerReferralCommissionDAS partnerReferralCommissionDAS) {
        this.partnerReferralCommissionDAS = partnerReferralCommissionDAS;
    }

    void setCustomerEnrollmentDAS(CustomerEnrollmentDAS customerEnrollmentDAS) {
        this.customerEnrollmentDAS = customerEnrollmentDAS;
    }

    void setPartnerBL(PartnerBL partnerBL) {
        this.partnerBL = partnerBL;
    }

    void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /**
     * Context for the current executing task
     */
    private class CommissionContext {
        CommissionProcessRunDTO processRun;
        //ids of all customers who already received customer comission in this run
        Set<Integer> customerIdsForCustomerCommission = new HashSet<>();
        Integer entityId;
        //date of initial customer enrollment
        Date customerEnrollmentDate;
        //PartnerCommissionValue to apply
        BigDecimal rate;
        //Customer specific rate from CustomerCommissionDTO
        BigDecimal customerRate;
        //partners which failed
        int errorCount = 0;
    }
}
