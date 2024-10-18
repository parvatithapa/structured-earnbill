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
package com.sapienter.jbilling.server.payment.db;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.db.TotalBalance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.transform.Transformers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentDAS extends AbstractDAS<PaymentDTO> {

    public static final Logger logger = LoggerFactory.getLogger(PaymentDAS.class);

    @SuppressWarnings("unchecked")
    // used for the web services call to get the latest X
    public List<Integer> findIdsByUserLatestFirst(Integer userId, int limit, int offset) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .setProjection(Projections.id()).addOrder(Order.desc("createDatetime"))
                .setMaxResults(limit)
                .setFirstResult(offset);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findIdsByUserAndDate(Integer userId, Date since, Date until) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.ge("paymentDate", since))
                .add(Restrictions.lt("paymentDate", until))
                .setProjection(Projections.id())
                .addOrder(Order.desc("createDatetime"))
                .setComment("findIdsByUserAndDate " + userId + " " + since + " " + until);
        return criteria.list();
    }

    /**
     * This method used for find the payment id's by entityId.
     *
     * @param entityId
     * @return List<Integer>
     */
    public List<Integer> findIdsByEntity(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class)
                .setLockMode(LockMode.NONE)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .createAlias("u.customer", "customer")
                .createAlias("u.company", "company")
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("u.deleted", 0))
                .setProjection(Projections.id())
                .addOrder(Order.asc("id"))
                .setComment("findIdsByEntity " + entityId);

        ScrollableResults scrollableResults = criteria.scroll();
        List<Integer> paymentIds = new ArrayList<>();
        if (scrollableResults != null) {
            try {
                while (scrollableResults.next()) {
                    paymentIds.add(scrollableResults.getInteger(0));
                }
            } finally {
                scrollableResults.close();
            }
        }
        Collections.sort(paymentIds);

        return paymentIds;
    }

    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findPaymentsByUserPaged(Integer userId, int maxResults, int offset) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.desc("id"))
                .setMaxResults(maxResults)
                .setFirstResult(offset);
        return criteria.list();
    }

    public PaymentDTO create(BigDecimal amount, PaymentMethodDTO paymentMethod,
                             Integer userId, Integer attempt, PaymentResultDTO paymentResult,
                             CurrencyDTO currency) {

        PaymentDTO payment = new PaymentDTO();
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setBaseUser(new UserDAS().find(userId));
        payment.setAttempt(attempt);
        payment.setPaymentResult(paymentResult);
        payment.setCurrency(new CurrencyDAS().find(currency.getId()));
        payment.setCreateDatetime(Calendar.getInstance().getTime());
        payment.setDeleted(0);
        payment.setIsRefund(0);
        payment.setIsPreauth(0);

        return save(payment);

    }

    /**
     * * query="SELECT OBJECT(p) FROM payment p WHERE p.userId = ?1 AND
     * p.balance >= 0.01 AND p.isRefund = 0 AND p.isPreauth = 0 AND p.deleted =
     * 0"
     *
     * @param userId
     * @return
     */
    public Collection findWithBalance(Integer userId) {

        UserDTO user = new UserDAS().find(userId);

        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("baseUser", user));
        criteria.add(Restrictions.ge("balance", Constants.BIGDECIMAL_ONE_CENT));
        criteria.add(Restrictions.eq("isRefund", 0));
        criteria.add(Restrictions.eq("isPreauth", 0));
        criteria.add(Restrictions.eq("deleted", 0));

        return criteria.list();
    }

    /**
     * Revenue = Payments minus Refunds
     *
     * @param userId
     * @param from   (optional) From date for payments
     * @param until  (optional) Until date for payments
     * @return
     */
    public BigDecimal findTotalRevenueByUser(Integer userId, Date from, Date until) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("paymentResult", "pr");

        if (from != null) {
            Restrictions.gt("createDatetime", from);
        }
        if (until != null) {
            Restrictions.le("createDatetime", from);
        }


        Criterion PAYMENT_SUCCESSFUL = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        Criterion PAYMENT_ENTERED = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED);

        LogicalExpression successOrEntered = Restrictions.or(PAYMENT_ENTERED, PAYMENT_SUCCESSFUL);

        // Criteria or condition
        criteria.add(successOrEntered);

        criteria.add(Restrictions.eq("isRefund", 0));
        criteria.add(Restrictions.ne("paymentMethod.id", CommonConstants.PAYMENT_METHOD_CREDIT));
        criteria.setProjection(Projections.sum("amount"));
        criteria.setComment("PaymentDAS.findTotalRevenueByUser-Gross Receipts");

        BigDecimal grossReceipts = criteria.uniqueResult() == null ? BigDecimal.ZERO : (BigDecimal) criteria.uniqueResult();
        DetachedCriteria creditPaymentIds = DetachedCriteria.forClass(PaymentDTO.class);
        creditPaymentIds.add(Restrictions.eq("paymentMethod.id", CommonConstants.PAYMENT_METHOD_CREDIT)).setProjection(Property.forName("id"));

        //Calculate Refunds
        Criteria criteria2 = getSession().createCriteria(PaymentDTO.class);
        criteria2.add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("paymentResult", "pr");

        // Criteria or condition
        criteria2.add(successOrEntered);
        criteria2.add(Subqueries.propertyNotIn("id", creditPaymentIds));
        criteria2.add(Restrictions.eq("isRefund", 1));
        criteria2.setProjection(Projections.sum("amount"));
        criteria2.setComment("PaymentDAS.findTotalRevenueByUser-Gross Refunds");

        BigDecimal refunds = criteria2.uniqueResult() == null ? BigDecimal.ZERO : (BigDecimal) criteria2.uniqueResult();

        //net revenue = gross - all refunds
        BigDecimal netRevenueFromUser = grossReceipts.subtract(refunds);

        logger.debug("Gross receipts {} minus Gross Refunds {}: {}", grossReceipts, refunds, netRevenueFromUser);
        return netRevenueFromUser;
    }

    @SuppressWarnings("unchecked")
    public List<TotalBalance> findTotalBalanceByUser(Integer userId) {

        //user's payments which are not refunds
        Criteria criteria = getSession().createCriteria(PaymentDTO.class)
                .createAlias("paymentResult", "pr")
                .add(Restrictions.eq("deleted", 0)).createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.eq("isRefund", 0))
                .add(Restrictions.or(Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL),
                        Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED)))
                .setProjection(Projections.sum("balance"))
                .setProjection(Projections.groupProperty("currency"))
                .setProjection(Projections.projectionList()
                        .add(Projections.property("balance"), "balance")
                        .add(Projections.property("currency.id"), "currency"))
                .setResultTransformer(Transformers.aliasToBean(TotalBalance.class))
                .setComment("PaymentDAS.findTotalBalanceByUser");

        return criteria.list();
    }


    /**
     * query="SELECT OBJECT(p) FROM payment p WHERE
     * p.userId = ?1 AND
     * p.balance >= 0.01 AND
     * p.isRefund = 0 AND
     * p.isPreauth = 1 AND
     * p.deleted = 0"
     *
     * @param userId
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<PaymentDTO> findPreauth(Integer userId) {

        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.add(Restrictions.ge("balance", Constants.BIGDECIMAL_ONE_CENT));
        criteria.add(Restrictions.eq("isRefund", 0));
        criteria.add(Restrictions.eq("isPreauth", 1));
        criteria.add(Restrictions.eq("deleted", 0));

        return criteria.list();

    }

    private static final String BILLING_PROCESS_GENERATED_PAYMENTS_HQL =
            "select payment "
                    + " from PaymentDTO payment "
                    + " join payment.invoicesMap as invoiceMap "
                    + " where invoiceMap.invoiceEntity.billingProcess.id = :billing_process_id "
                    + " and payment.deleted = 0 "
                    + " and payment.createDatetime >= :start "
                    + " and payment.createDatetime <= :end";

    /**
     * Returns a list of all payments that were made to invoices generated by
     * the billing process between the processes start & end times.
     * <p>
     * Payments represent the amount automatically paid by the billing process.
     *
     * @param processId billing process id
     * @param start     process run start date
     * @param end       process run end date
     * @return list of payments generated by the billing process.
     */
    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findBillingProcessGeneratedPayments(Integer processId, Date start, Date end) {
        Query query = getSession().createQuery(BILLING_PROCESS_GENERATED_PAYMENTS_HQL);
        query.setParameter("billing_process_id", processId);
        query.setParameter("start", start);
        query.setParameter("end", end);

        return query.list();
    }

    private static final String BILLING_PROCESS_PAYMENTS_HQL =
            "select paymentInvoice "
                    + " from PaymentInvoiceMapDTO paymentInvoice "
                    + " join paymentInvoice.payment as payment "
                    + " where paymentInvoice.invoiceEntity.billingProcess.id = :billing_process_id "
                    + " and payment.paymentResult.id != :payment_result_failed_id "
                    + " and payment.paymentResult.id != :payment_result_processor_unavailable_id "
                    + " and payment.deleted = 0 "
                    + " and payment.createDatetime > :end";

    /**
     * Returns a list of all payments that were made to invoices generated by
     * the billing process, after the billing process run had ended.
     * <p>
     * Payments made to generated invoices after the process has finished are still
     * relevant to the process as it shows how much of the balance was paid by
     * users (or paid in a retry process) for this billing period.
     *
     * @param processId billing process id
     * @param end       process run end date
     * @return list of payments applied to the billing processes invoices.
     */
    @SuppressWarnings("unchecked")
    public List<PaymentInvoiceMapDTO> findBillingProcessPayments(Integer processId, Date end) {
        Query query = getSession().createQuery(BILLING_PROCESS_PAYMENTS_HQL);
        query.setParameter("payment_result_failed_id", CommonConstants.PAYMENT_RESULT_FAILED);
        query.setParameter("payment_result_processor_unavailable_id", CommonConstants.PAYMENT_RESULT_PROCESSOR_UNAVAILABLE);
        query.setParameter("billing_process_id", processId);
        query.setParameter("end", end);

        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findAllPaymentByBaseUserAndIsRefund(Integer userId, Integer isRefund) {

        UserDTO user = new UserDAS().find(userId);
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("baseUser", user));
        criteria.add(Restrictions.eq("isRefund", isRefund));
        criteria.add(Restrictions.eq("deleted", 0));

        return criteria.list();

    }

    public List<PaymentDTO> getRefundablePayments(Integer userId) {

        UserDTO user = new UserDAS().find(userId);
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("baseUser", user));
        criteria.add(Restrictions.eq("isRefund", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.gt("balance", BigDecimal.ZERO));
        // all payments of the given user which are not refund payments
        @SuppressWarnings("unchecked")
        List<PaymentDTO> allPayments = criteria.list();

        return allPayments;
    }

    /**
     * Find if the passed payment id has been refunded at all.
     *
     * @param paymentId
     * @return
     */
    public boolean isRefundedPartiallyOrFully(Integer paymentId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("isRefund", 1));
        criteria.add(Restrictions.ne("id", paymentId));
        criteria.add(Restrictions.eq("payment.id", paymentId));
        criteria.add(Restrictions.eq("deleted", 0));
        return criteria.list().size() > 0;
    }

    /**
     * Get the total Refunded amount for this Payment ID
     *
     * @param isRefund
     * @return
     */

    public BigDecimal getRefundedAmount(Integer paymentId) {

        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("isRefund", 1));
        criteria.add(Restrictions.ne("id", paymentId));
        criteria.add(Restrictions.eq("payment.id", paymentId));
        criteria.setProjection(Projections.sum("amount"));
        criteria.setComment("PaymentDAS.getRefundedAmount - for paymentId");

        return criteria.uniqueResult() == null ? BigDecimal.ZERO : (BigDecimal) criteria.uniqueResult();
    }

    public Boolean findPaymentProcessed(String txnId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("paymentAuthorizations", "p");
        criteria.add(Restrictions.eq("p.transactionId", txnId));
        return criteria.list().size() > 0;

    }

    public PaymentDTO findPaymentProcessedDTO(String parentTxnId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("paymentAuthorizations", "p");
        criteria.add(Restrictions.eq("p.transactionId", parentTxnId));
        return (PaymentDTO) criteria.uniqueResult();

    }

    /**
     * Finds last Payment Id of particular user with given result id
     *
     * @param userId
     * @param resultId
     * @return Payment Id
     */
    public Integer getLatest(Integer userId, Integer resultId) {
        DetachedCriteria criteria = DetachedCriteria.forClass(PaymentDTO.class);
        criteria.createAlias("baseUser", "u");
        criteria.createAlias("paymentResult", "pr");
        criteria.setProjection(Projections.max("id"));
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.add(Restrictions.eq("pr.id", resultId));
        criteria.add(Restrictions.eq("deleted", 0));
        List maxId = getHibernateTemplate().findByCriteria(criteria);
        if (maxId != null && maxId.size() > 0) {
            return (Integer) maxId.get(0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    // used for the web services call to get the latest X
    public List<Integer> findPaymentByUserIdOldestFirst(Integer userId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("paymentResult", "pr");
        Criterion PAYMENT_SUCCESSFUL = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        Criterion PAYMENT_ENTERED = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED);

        LogicalExpression successOrEntered = Restrictions.or(PAYMENT_ENTERED, PAYMENT_SUCCESSFUL);

        // Criteria or condition
        criteria.add(successOrEntered);
        criteria.add(Restrictions.ge("balance", Constants.BIGDECIMAL_ONE_CENT));
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.setProjection(Projections.id());
        criteria.addOrder(Order.asc("paymentDate"));
        return criteria.list();
    }

    /**
     * Find Payments for given user id
     *
     * @param userId
     * @return list of Payments
     */
    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findSuccessfulOrEnteredPaymentsByUser(Integer userId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("paymentResult", "pr");
        Criterion PAYMENT_SUCCESSFUL = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        Criterion PAYMENT_ENTERED = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED);

        LogicalExpression successOrEntered = Restrictions.or(PAYMENT_ENTERED, PAYMENT_SUCCESSFUL);

        // Criteria or condition
        criteria.add(successOrEntered);
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.ne("paymentMethod.id", Constants.PAYMENT_METHOD_CREDIT));
        criteria.addOrder(Order.desc("paymentDate"));
        return criteria.list();
    }

    private static final String PaymentsWithBalanceDoneAfterPreviousInvoiceDate =
            " select p.payment_date as payment_date, p.balance as amount, p.is_refund "
                    + " from payment p "
                    + " where p.deleted = 0 "
                    + " and (p.result_id = " + CommonConstants.PAYMENT_RESULT_SUCCESSFUL + " "
                    + " or p.result_id = " + CommonConstants.PAYMENT_RESULT_ENTERED + ") "
                    + " and p.user_id =:userId "
                    //+ " and p.payment_date >= (select create_datetime from invoice where id =:previousInvoiceId) "
                    + " and p.is_refund =  0 "
                    + " and p.balance >  0 "
                    + " and p.id in "
                    + " (select pi.payment_id "
                    + " from payment_invoice pi "
                    + " inner join payment p on p.id = pi.payment_id "
                    + " where p.user_id =:userId) "
                    + " union all "
                    + " select p.payment_date as payment_date, p.amount as amount, p.is_refund "
                    + " from payment p "
                    + " where p.deleted = 0 "
                    + " and (p.result_id = " + CommonConstants.PAYMENT_RESULT_SUCCESSFUL + " "
                    + " or p.result_id = " + CommonConstants.PAYMENT_RESULT_ENTERED + ") "
                    + " and p.user_id =:userId "
                    + " and p.payment_date >= (select create_datetime from invoice where id =:previousInvoiceId) "
                    + " and p.is_refund = 0 "
                    + " and p.id not in "
                    + " (select pi.payment_id "
                    + " from payment_invoice pi "
                    + " inner join payment p on p.id = pi.payment_id "
                    + " where p.user_id =:userId ) "
                    + " union all "
                    + " select p.payment_date as payment_date, p.amount as amount, p.is_refund "
                    + " from payment p "
                    + " where p.deleted = 0 "
                    + " and (p.result_id = " + CommonConstants.PAYMENT_RESULT_SUCCESSFUL + " "
                    + " or p.result_id = " + CommonConstants.PAYMENT_RESULT_ENTERED + ") "
                    + " and p.user_id =:userId "
                    + " and p.payment_date >= (select create_datetime from invoice where id = 45124) "
                    + " and p.is_refund = 1 "
                    + " and p.id not in "
                    + " (select pi.payment_id "
                    + " from payment_invoice pi "
                    + " where pi.invoice_id =:previousInvoiceId "
                    + " or pi.invoice_id =:currentInvoiceId ) ";

    /**
     * Get the payments with balance done after the previous invoice date
     *
     * @param User     Id
     * @param Previous Invoice Id
     * @return Count
     */
    public List<PaymentDTO> getPaymentsWithBalanceDoneAfterPreviousInvoiceDate(Integer userId, Integer previousInvoiceId, Integer currentInvoiceId) {

        SQLQuery sqlQuery = getSession().createSQLQuery(PaymentsWithBalanceDoneAfterPreviousInvoiceDate);
        sqlQuery.setParameter("userId", userId);
        sqlQuery.setParameter("previousInvoiceId", previousInvoiceId);
        sqlQuery.setParameter("currentInvoiceId", currentInvoiceId);
        return null != sqlQuery.list() && !sqlQuery.list().isEmpty() ? sqlQuery.list() : null;
    }

    /**
     * get All payments of user
     *
     * @param userId
     * @return
     */
    public List<Integer> findIdsByUserId(Integer userId) {

        String hql = "select p.id" +
                "  from PaymentDTO p " +
                "  where p.baseUser.id = :userId" +
                "  and p.deleted = 0";

        List<Integer> data = getSession()
                .createQuery(hql)
                .setParameter("userId", userId)
                .setComment("PaymentDAS.findIdsByUserId " + userId)
                .list();
        return data;
    }

    /**
     * Retrieve payments sorted by sortAttribute attribute and ordered sepcified in order attribute.
     *
     * @param userId
     * @param maxResults
     * @param offset
     * @param sortAttribute
     * @param order
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findPaymentsByUserPagedSortedByAttribute(Integer userId, int maxResults, int offset, String sortAttribute, ListField.Order order) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .setMaxResults(maxResults)
                .setFirstResult(offset);
        if (ListField.Order.ASC.equals(order)) {
            criteria.addOrder(Order.asc(sortAttribute));
        } else {
            criteria.addOrder(Order.desc(sortAttribute));
        }
        return criteria.list();
    }

    public PaymentDTO findPaymentByBankReferenceId(Integer userId, String bankRefId) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        sb.append("p.* ");
        sb.append("from ");
        sb.append("payment p, ");
        sb.append("payment_instrument_info pii, ");
        sb.append("payment_information pi, ");
        sb.append("payment_information_meta_fields_map pimfm, ");
        sb.append("meta_field_value mfv ");
        sb.append("where ");
        //sb.append("p.user_id = :userId");
        sb.append("p.id = pii.payment_id ");
        sb.append("and ");
        sb.append("pii.instrument_id = pi.id ");
        sb.append("and ");
        sb.append("pi.id = pimfm.payment_information_id ");
        sb.append("and ");
        sb.append("mfv.id = pimfm.meta_field_value_id ");
        sb.append("and ");
        sb.append("mfv.string_value = :transactionId");

        Query query = getSessionFactory().getCurrentSession().createSQLQuery(sb.toString())
                .addEntity(PaymentDTO.class)
                //.setParameter("userId", userId)
                .setParameter("transactionId", bankRefId);
        return (PaymentDTO) query.uniqueResult();
    }

    /**
     *
     *
     */
    private static final String creditPaymentAmount =
            "select sum(amount) as credit_payment_amount "
                    + " from "
                    + " payment p "
                    + " where deleted = 0 "
                    + " and p.user_id = (select user_id from invoice where id =:invoiceId ) "
                    // payment of type credit
                    + " and method_id = " + CommonConstants.PAYMENT_METHOD_CREDIT + ""
                    // where payment date between 1st day of previous month and last day of month
                    + " and p.create_datetime >= "
                    + " (select cast (date_trunc('month', create_datetime - interval '1 month') as date) from invoice where id =:invoiceId ) "
                    + " and p.create_datetime <= "
                    + " (select (cast(date_trunc('month', create_datetime) as date) - 1) from invoice where id =:invoiceId ) ";

    /**
     * Returns the credit payment amount
     *
     * @param User     Id
     * @param Previous Invoice Id
     * @return Count
     */
    public BigDecimal getCreditPaymentAmount(Integer invoiceId) {

        SQLQuery sqlQuery = getSession().createSQLQuery(creditPaymentAmount);
        sqlQuery.setParameter("invoiceId", invoiceId);
        return (BigDecimal) (null != sqlQuery.uniqueResult() ? sqlQuery.uniqueResult() : BigDecimal.ZERO);
    }

    private static final String getTransactionIdByPaymentSql =
            "select transaction_id from payment_authorization where payment_id = :payemntId";

    public String getTrasactionIdByPayment(Integer paymentId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(getTransactionIdByPaymentSql);
        sqlQuery.setParameter("payemntId", paymentId);
        return (String) sqlQuery.uniqueResult();
    }

    /**
     * Revenue = Payments minus Refunds
     *
     * @param userId
     * @param from   (optional) From date for payments
     * @param until  (optional) Until date for payments
     * @return
     */
    public BigDecimal findTotalRevenueByUserInBetweenTwoInvoices(Integer userId, Date from, Date until) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("paymentResult", "pr");

        if (from != null) {
            criteria.add(Restrictions.ge("paymentDate", from));
        }
        criteria.add(Restrictions.lt("paymentDate", until));

        Criterion PAYMENT_SUCCESSFUL = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        Criterion PAYMENT_ENTERED = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED);

        LogicalExpression successOrEntered = Restrictions.or(PAYMENT_ENTERED, PAYMENT_SUCCESSFUL);

        // Criteria or condition
        criteria.add(successOrEntered);

        criteria.add(Restrictions.eq("isRefund", 0));
        criteria.add(Restrictions.ne("paymentMethod.id", CommonConstants.PAYMENT_METHOD_CREDIT));
        criteria.setProjection(Projections.sum("amount"));
        criteria.setComment("PaymentDAS.findTotalRevenueByUser-Gross Receipts");

        BigDecimal grossReceipts = criteria.uniqueResult() == null ? BigDecimal.ZERO : (BigDecimal) criteria.uniqueResult();
        DetachedCriteria creditPaymentIds = DetachedCriteria.forClass(PaymentDTO.class);
        creditPaymentIds.add(Restrictions.eq("paymentMethod.id", CommonConstants.PAYMENT_METHOD_CREDIT))
                .setProjection(Property.forName("id"));

        //Calculate Refunds
        Criteria criteria2 = getSession().createCriteria(PaymentDTO.class);
        criteria2.add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("paymentResult", "pr");
        if (from != null) {
            criteria2.add(Restrictions.ge("paymentDate", from));
        }
        criteria2.add(Restrictions.lt("paymentDate", until));
        // Criteria or condition
        criteria2.add(successOrEntered);
        criteria2.add(Subqueries.propertyNotIn("id", creditPaymentIds));
        criteria2.add(Restrictions.eq("isRefund", 1));
        criteria2.setProjection(Projections.sum("amount"));
        criteria2.setComment("PaymentDAS.findTotalRevenueByUser-Gross Refunds");

        BigDecimal refunds = criteria2.uniqueResult() == null ? BigDecimal.ZERO : (BigDecimal) criteria2.uniqueResult();

        //net revenue = gross - all refunds
        BigDecimal netRevenueFromUser = grossReceipts.subtract(refunds);

        logger.debug("Gross receipts {} minus Gross Refunds {}: {}", grossReceipts, refunds, netRevenueFromUser);

        return netRevenueFromUser;
    }

    /**
     * Fetch all payments of type CREDIT in between last 2 invoices
     *
     * @param userId
     * @param from
     * @param until
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findCreditPaymentsBetweenLastAndCurrentInvoiceDates(Integer userId, Date from, Date until) {

        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("paymentResult", "pr");
        Criterion PAYMENT_SUCCESSFUL = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        Criterion PAYMENT_ENTERED = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED);

        LogicalExpression successOrEntered = Restrictions.or(PAYMENT_ENTERED, PAYMENT_SUCCESSFUL);

        // Criteria or condition
        criteria.add(successOrEntered);
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("paymentMethod.id", Constants.PAYMENT_METHOD_CREDIT));
        if (from != null) {
            criteria.add(Restrictions.ge("paymentDate", from));
        }
        criteria.add(Restrictions.lt("paymentDate", until));
        criteria.addOrder(Order.desc("paymentDate"));
        return criteria.list();
    }

    /**
     * Fetch all payments (ENTERED AND SUCCESSFUL) in between last 2 invoices
     *
     * @param userId
     * @param from
     * @param until
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<PaymentDTO> findPaymentsBetweenLastAndCurrentInvoiceDates(Integer userId, Date from, Date until) {

        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.createAlias("paymentResult", "pr");
        Criterion PAYMENT_SUCCESSFUL = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        Criterion PAYMENT_ENTERED = Restrictions.eq("pr.id", CommonConstants.PAYMENT_RESULT_ENTERED);

        LogicalExpression successOrEntered = Restrictions.or(PAYMENT_ENTERED, PAYMENT_SUCCESSFUL);

        // Criteria or condition
        criteria.add(successOrEntered);
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.ne("paymentMethod.id", Constants.PAYMENT_METHOD_CREDIT));
        if (from != null) {
            criteria.add(Restrictions.ge("paymentDate", from));
        }
        criteria.add(Restrictions.lt("paymentDate", until));
        criteria.addOrder(Order.desc("paymentDate"));
        return criteria.list();
    }

    public Integer findPaymentByMetaFields(Map<String, String> metaFieldMap) {

        String db_query = "select p.id from payment p";
        int i = 1;
        for (String metaFieldName : metaFieldMap.keySet()) {
            db_query = db_query + " join payment_meta_field_map pmfm" + i + " on (pmfm" + i + ".payment_id = p.id and pmfm" + i + ".meta_field_value_id"
                    + " in (select mfv.id from meta_field_value mfv join meta_field_name mfn on mfn.id = mfv.meta_field_name_id"
                    + " where p.deleted = 0 and mfn.name = '" + metaFieldName + "' and mfv.String_value = '" + metaFieldMap.get(metaFieldName) + "'))";
            i = i + 1;
        }

        SQLQuery sqlQuery = getSession().createSQLQuery(db_query);
        return (Integer) sqlQuery.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<Integer> findAllPaymentsByMetaFields(Map<String, String> metaFieldMap) {

        String db_query =
                "SELECT p.id " +
                        "FROM payment p";
        int i = 1;
        for (String metaFieldName : metaFieldMap.keySet()) {
            db_query = db_query + " " +
                    "JOIN payment_meta_field_map pmfm" + i + " " +
                    "ON (pmfm" + i + ".payment_id = p.id " +
                    "AND pmfm" + i + ".meta_field_value_id" + " " +
                    "IN (" +
                    "SELECT mfv.id " +
                    "FROM meta_field_value mfv " +
                    "JOIN meta_field_name mfn " +
                    "ON mfn.id = mfv.meta_field_name_id" + " " +
                    "WHERE mfn.name = '" + metaFieldName + "' " +
                    "AND mfv.String_value = '" + metaFieldMap.get(metaFieldName) + "' " +
                    "AND p.deleted = 0))";
            i = i + 1;
        }

        SQLQuery sqlQuery = getSession().createSQLQuery(db_query);
        return sqlQuery.list();
    }

    public Integer getPaymentResultIdForPayment(Integer userId, Integer paymentId) {

        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("id", paymentId))
                .setProjection(Projections.property("paymentResult.id"));

        return (Integer) criteria.uniqueResult();
    }

    public Integer findFirstPaymentByMetaFields(Map<String, String> metaFieldMap, Integer userId) {

        String db_query = "select p.id from payment p";
        int i = 1;
        for (String metaFieldName : metaFieldMap.keySet()) {
            db_query = db_query + " join payment_meta_field_map pmfm" + i + " on (pmfm" + i + ".payment_id = p.id and pmfm" + i + ".meta_field_value_id"
                    + " in (select mfv.id from meta_field_value mfv join meta_field_name mfn on mfn.id = mfv.meta_field_name_id"
                    + " where p.deleted = 0 and mfn.name = '" + metaFieldName + "' and mfv.String_value = '" + metaFieldMap.get(metaFieldName) + "'))";
            i = i + 1;
        }

        db_query = db_query + " WHERE p.user_id = :userId ORDER BY p.id DESC LIMIT 1";

        SQLQuery sqlQuery = getSession().createSQLQuery(db_query);
        sqlQuery.setParameter("userId", userId);

        return (Integer) sqlQuery.uniqueResult();
    }

    /**
     * Get the count of payments for the given userId.
     *
     * @param userId
     * @return Count
     */
    public Long getPaymentsCountByUserId(Integer userId) {
        Criteria criteria = getSession().createCriteria(PaymentDTO.class);
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.setProjection(Projections.count("id"));
        return (Long) criteria.uniqueResult();
    }
}

