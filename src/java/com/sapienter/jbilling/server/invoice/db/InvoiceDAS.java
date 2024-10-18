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

package com.sapienter.jbilling.server.invoice.db;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.invoice.NewInvoiceContext;
import com.sapienter.jbilling.server.process.db.BillingProcessDAS;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.InvoiceCommissionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.db.TotalBalance;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StringType;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceDAS extends AbstractDAS<InvoiceDTO> {
    public static final Logger logger = LoggerFactory.getLogger(InvoiceDAS.class);

    private static final String FIND_ALL_UNPAID_INVOICES_SQL =
            "SELECT inv.id " +
                    "   FROM invoice inv" +
                    "      JOIN base_user u" +
                    "           ON u.id=inv.user_id" +
                    "  WHERE inv.user_id = :userId AND inv.deleted = 0" +
                    "       AND u.status_id = 1" +
                    "       AND inv.status_id = " +
                    "       (SELECT gs.id" +
                    "           FROM generic_status gs" +
                    "         WHERE status_value = :statusValue " +
                    "               AND dtype = 'invoice_status')" +
                    "   ORDER BY inv.id DESC";

    private static final String FIND_LAST_INVOICE_FOR_USER_SQL =
            "SELECT MAX (i.id)" +
                    "   FROM invoice i" +
                    "   JOIN base_user u ON i.user_id=u.id" +
                    "       JOIN invoice_line il on i.id = il.invoice_id" +
                    " WHERE i.id = il.invoice_id" +
                    "       AND i.user_id = :userId " +
                    "       AND is_review = 0 " +
                    "       AND i.deleted = 0";

    // used for the web services call to get the latest X
    public List<Integer> findIdsByUserLatestFirst(Integer userId, Integer maxResults) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isReview", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setMaxResults(maxResults);

        return criteria.list();
    }


    public List<InvoiceDTO> findInvoicesByUserPaged(Integer userId, Integer maxResults, Integer offset) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isReview", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.desc("createDatetime"));
        StringBuilder comment = new StringBuilder("findIdsByUserPaged, userId:" + userId);
        if (null != maxResults) {
            criteria.setMaxResults(maxResults);
            comment.append(", maxResults:" + maxResults);
        }
        if (null != offset) {
            criteria.setFirstResult(offset);
            comment.append(", offset:" + offset);
        }
        criteria.setComment(comment.toString());
        return criteria.list();
    }

    // used for the web services call to get the latest X that contain a particular item type
    public List<Integer> findIdsByUserAndItemTypeLatestFirst(Integer userId, Integer itemTypeId, int maxResults) {

        String hql = "select distinct(invoice.id)" +
                "  from InvoiceDTO invoice" +
                "  inner join invoice.invoiceLines line" +
                "  inner join line.item.itemTypes itemType" +
                "  where invoice.baseUser.id = :userId" +
                "    and invoice.deleted = 0" +
                "    and itemType.id = :typeId" +
                "  order by invoice.id desc";
        List<Integer> data = getSession()
                .createQuery(hql)
                .setParameter("userId", userId)
                .setParameter("typeId", itemTypeId)
                .setMaxResults(maxResults)
                .list();
        return data;
    }

    // used for checking if a user was subscribed to something at a given date
    public List<Integer> findIdsByUserAndPeriodDate(Integer userId, Date date) {

        String hql = "select pr.invoice.id" +
                "  from OrderProcessDTO pr " +
                "  where pr.invoice.baseUser.id = :userId" +
                "    and pr.invoice.deleted = 0" +
                "    and pr.periodStart <= :date" +
                "    and pr.periodEnd > :date" + // the period end is not included
                "    and pr.isReview = 0";

        List<Integer> data = getSession()
                .createQuery(hql)
                .setParameter("userId", userId)
                .setParameter("date", date)
                .setComment("InvoiceDAS.findIdsByUserAndPeriodDate " + userId + " - " + date)
                .list();
        return data;
    }


    public BigDecimal findTotalForPeriod(Integer userId, Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        criteria.setProjection(Projections.sum("total"));
        return (BigDecimal) criteria.uniqueResult();
    }

    public BigDecimal findAmountForPeriodByItem(Integer userId, Integer itemId,
                                                Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        addItemCriteria(criteria, itemId);
        criteria.setProjection(Projections.sum("invoiceLines.amount"));
        return (BigDecimal) criteria.uniqueResult();
    }

    public BigDecimal findQuantityForPeriodByItem(Integer userId, Integer itemId,
                                                  Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        addItemCriteria(criteria, itemId);
        criteria.setProjection(Projections.sum("invoiceLines.quantity"));
        return (BigDecimal) criteria.uniqueResult();
    }

    public Long findLinesForPeriodByItem(Integer userId, Integer itemId,
                                         Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        addItemCriteria(criteria, itemId);
        criteria.setProjection(Projections.count("id"));
        return (Long) criteria.uniqueResult();
    }

    public BigDecimal findAmountForPeriodByItemCategory(Integer userId,
                                                        Integer categoryId, Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        addItemCategoryCriteria(criteria, categoryId);
        criteria.setProjection(Projections.sum("invoiceLines.amount"));
        return (BigDecimal) criteria.uniqueResult();
    }

    public BigDecimal findQuantityForPeriodByItemCategory(Integer userId,
                                                          Integer categoryId, Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        addItemCategoryCriteria(criteria, categoryId);
        criteria.setProjection(Projections.sum("invoiceLines.quantity"));
        return (BigDecimal) criteria.uniqueResult();
    }

    public Long findLinesForPeriodByItemCategory(Integer userId,
                                                 Integer categoryId, Date start, Date end) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, start, end);
        addItemCategoryCriteria(criteria, categoryId);
        criteria.setProjection(Projections.count("id"));
        return (Long) criteria.uniqueResult();
    }


    public boolean isReleatedToItemType(Integer invoiceId, Integer itemTypeId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addItemCategoryCriteria(criteria, itemTypeId);
        criteria.add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("id", invoiceId));

        return criteria.uniqueResult() != null;
    }

    private void addUserCriteria(Criteria criteria, Integer userId) {
        criteria.add(Restrictions.eq("deleted", 0))
                .createAlias("baseUser", "u").add(
                Restrictions.eq("u.id", userId));
    }

    private void addPeriodCriteria(Criteria criteria, Date start, Date end) {
        criteria.add(Restrictions.ge("createDatetime", start)).add(
                Restrictions.lt("createDatetime", end));
    }

    private void addItemCriteria(Criteria criteria, Integer itemId) {
        criteria.createAlias("invoiceLines", "invoiceLines").add(
                Restrictions.eq("invoiceLines.item.id", itemId));
    }

//  private void addItemCategoryCriteria(Criteria criteria, Integer categoryId) {
//      criteria.createAlias("invoiceLines", "invoiceLines")
//      .createAlias("invoiceLines.item", "item")
//      .createAlias("item.itemTypes","itemTypes")
//      .add(Restrictions.eq("itemTypes.id", categoryId));
//  }

    private void addItemCategoryCriteria(Criteria criteria, Integer categoryId) {
        criteria
                .createAlias("invoiceLines", "invoiceLines")
                .createAlias("invoiceLines.item", "item")
                .add(Restrictions.eq("item.deleted", 0))
                .createAlias("item.itemTypes", "itemTypes")
                .add(Restrictions.eq("itemTypes.id", categoryId));
    }

    public List<Integer> findIdsOverdueForUser(Integer userId, Date date) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        criteria
                .add(Restrictions.lt("dueDate", date))
                .createAlias("invoiceStatus", "s")
                .add(Restrictions.ne("s.id", Constants.INVOICE_STATUS_PAID))
                .add(Restrictions.eq("isReview", 0))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public InvoiceDTO findOverdueInvoiceForUserFirstByDate(Integer userId, Date date, Integer excludedInvoiceId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        criteria
                .add(Restrictions.lt("dueDate", date))
                .createAlias("invoiceStatus", "s")
                .add(Restrictions.ne("s.id", Constants.INVOICE_STATUS_PAID))
                .add(Restrictions.eq("isReview", 0))
                .addOrder(Order.asc("dueDate"));
        if (excludedInvoiceId != null) {
            criteria.add(Restrictions.ne("id", excludedInvoiceId));
        }
        List<InvoiceDTO> invoices = criteria.list();
        return invoices.isEmpty() ? null : invoices.get(0);
    }

    /**
     * query="SELECT OBJECT(a) FROM invoice a WHERE a.billingProcess.id = ?1 AND
     * a.invoiceStatus.id = 2 AND a.isReview = 0 AND a.inProcessPayment = 1 AND
     * a.deleted = 0" result-type-mapping="Local"
     *
     * @param processId
     * @return
     */
    public Collection findProccesableByProcess(Integer processId) {

        BillingProcessDTO process = new BillingProcessDAS().find(processId);
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("billingProcess", process));
        criteria.createAlias("invoiceStatus", "s")
                .add(Restrictions.eq("s.id", Constants.INVOICE_STATUS_UNPAID));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("inProcessPayment", 1));
        criteria.add(Restrictions.eq("deleted", 0));

        return criteria.list();

    }

    public Collection<InvoiceDTO> findAllApplicableInvoicesByUser(Integer userId) {

        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("baseUser.id", userId));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.createAlias("invoiceStatus", "status");
        criteria.add(Restrictions.ne("status.id", Constants.INVOICE_STATUS_PAID));
        criteria.setProjection(Projections.id()).addOrder(Order.desc("id"));
        return criteria.list();
    }

    public InvoiceDTO create(Integer userId, NewInvoiceContext invoice,
                             BillingProcessDTO process) {

        InvoiceDTO entity = new InvoiceDTO();

        entity.setCreateDatetime(invoice.getBillingDate());
        entity.setCreateTimestamp(null != invoice.getCreateTimestamp() ? //in case of importing legacy invoices this can be given
                invoice.getCreateTimestamp() : Calendar.getInstance().getTime());
        entity.setDeleted(new Integer(0));
        entity.setDueDate(invoice.getDueDate());
        entity.setTotal(invoice.getTotal());
        entity.setBalance(invoice.getBalance());
        entity.setCarriedBalance(invoice.getCarriedBalance());
        entity.setPaymentAttempts(new Integer(0));
        entity.setInProcessPayment(invoice.getInProcessPayment());
        entity.setIsReview(invoice.getIsReview());
        entity.setCurrency(invoice.getCurrency());
        entity.setBaseUser(new UserDAS().find(userId));
        entity.setCustomerNotes(invoice.getCustomerNotes());

        // note: toProcess was replaced by a generic status InvoiceStatusDTO
        // ideally we should replace it here too, however in this case PAID/UNPAID statuses have a different
        // different meaning than "process" / "don't process" 

        // Initially the invoices are processable, this will be changed
        // when the invoice gets fully paid. This doesn't mean that the
        // invoice will be picked up by the main process, because of the
        // due date. (fix: if the total is > 0)
        if (invoice.getTotal().compareTo(new BigDecimal(0)) <= 0 || invoice.getBalance().compareTo(BigDecimal.ZERO) == 0) {
            entity.setToProcess(new Integer(0));
        } else {
            entity.setToProcess(new Integer(1));
        }
        //Added by Gurdev Parmar
        entity.setMetaFields(invoice.getMetaFields());
        if (process != null) {
            entity.setBillingProcess(process);
            InvoiceDTO saved = save(entity);
            // The next line is theoretically necessary. However, it will slow down the billing
            // process to a crawl. Since the column for the association is in the invoice table,
            // the DB is updated correctly wihout this line.
            // process.getInvoices().add(saved);
            return saved;
        }

        return save(entity);

    }

    /*
     * Collection findWithBalanceByUser(java.lang.Integer userId)"
     *             query="SELECT OBJECT(a)
     *                      FROM invoice a
     *                     WHERE a.userId = ?1
     *                       AND a.balance <> 0
     *                       AND a.isReview = 0
     *                       AND a.deleted = 0"
     *             result-type-mapping="Local"
     */
    public Collection findWithBalanceByUser(UserDTO user) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("baseUser", user));
        criteria.add(Restrictions.ne("balance", BigDecimal.ZERO));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));

        return criteria.list();
    }

    public List<InvoiceDTO> findWithBalanceOldestFirstByUser(UserDTO user) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("baseUser", user));
        criteria.add(Restrictions.ne("balance", BigDecimal.ZERO));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.addOrder(Order.asc("createDatetime"));
        criteria.addOrder(Order.asc("createTimestamp"));

        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<TotalBalance> findTotalBalanceByUser(Integer userId,Date ageingDate) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .add(Restrictions.eq("isReview", 0))
                .add(Restrictions.eq("deleted", 0))
                .setProjection(Projections.sum("balance"))
                .setProjection(Projections.groupProperty("currency"))
                .setProjection(Projections.projectionList()
                        .add(Projections.property("balance"), "balance")
                        .add(Projections.property("currency.id"), "currency"))
                .setResultTransformer(Transformers.aliasToBean(TotalBalance.class))
                .setComment("InvoiceDAS.findTotalBalanceByUser");
        if(ageingDate != null){
            criteria.add(Restrictions.lt("dueDate", ageingDate));
        }
        addUserCriteria(criteria, userId);

        return criteria.list();
    }

    /**
     * @param userId
     * @return
     */
    public BigDecimal getUnpaidAndCarriedInvoiceBalanceByUser(Integer userId, Date invoiceDate) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);

        addUserCriteria(criteria, userId);
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.createAlias("invoiceStatus", "status");
        criteria.add(Restrictions.eq("status.id", Constants.INVOICE_STATUS_UNPAID_AND_CARRIED));
        criteria.add(Restrictions.le("createDatetime", invoiceDate));
        criteria.setProjection(Projections.sum("balance"));
        criteria.setComment("InvoiceDAS.getUnpaidAndCarriedInvoiceBalanceByUser");

        Object ttlBal = criteria.uniqueResult();

        BigDecimal invoiceBalance = (ttlBal == null ? BigDecimal.ZERO : (BigDecimal) ttlBal);
        logger.debug("Total Invoice Balance for User {} is {}", userId, invoiceBalance);
        return invoiceBalance;
    }

    /**
     * Returns the sum total balance of all unpaid invoices for the given user.
     *
     * @param userId user id
     * @return total balance of all unpaid invoices.
     */
    public BigDecimal findTotalAmountOwed(Integer userId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        criteria.createAlias("invoiceStatus", "status");
        criteria.add(Restrictions.ne("status.id", Constants.INVOICE_STATUS_PAID));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.setProjection(Projections.sum("balance"));
        criteria.setComment("InvoiceDAS.findTotalAmountOwed");

        BigDecimal totalAmountOwed = (criteria.uniqueResult() == null ? BigDecimal.ZERO : (BigDecimal) criteria.uniqueResult());

        logger.debug("Total Amount Owed for User {} is {}", userId, totalAmountOwed);

        return totalAmountOwed;
    }

    /*
     * signature="Collection findProccesableByUser(java.lang.Integer userId)"
 *             query="SELECT OBJECT(a) 
 *                      FROM invoice a 
 *                     WHERE a.userId = ?1
 *                       AND a.invoiceStatus.id = 2 
 *                       AND a.isReview = 0
 *                       AND a.deleted = 0"
 *             result-type-mapping="Local"
     */
    public List<InvoiceDTO> findProccesableByUser(UserDTO user) {

        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("baseUser", user));
        criteria.createAlias("invoiceStatus", "s").add(Restrictions.in("s.id", new Object[]{Constants.INVOICE_STATUS_UNPAID, Constants.INVOICE_STATUS_UNPAID_AND_CARRIED}));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.addOrder(Order.asc("dueDate"));

        return criteria.list();
    }

    public Collection<InvoiceDTO> findByProcess(BillingProcessDTO process) {

        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("billingProcess", process));

        return criteria.list();
    }

    /**
     * This method returns computed details of invoices to be shown on UI
     *
     * @param processId
     * @return A Map of computed details of invoices based on currency id
     */
    public Map<Integer, Map<String, Object>> findInvoiceDetailsByProcessId(Integer processId) {
        final String sql = "select inv.currency_id, cur.symbol, count(inv.id) invoices, sum(inv.total)-sum(inv.carried_balance) totalInvoiced, sum(inv.carried_balance) totalCarried "
                + "from invoice inv inner join currency cur on inv.currency_id = cur.id where inv.billing_process_id = :processId "
                + "group by inv.currency_id, cur.symbol";

        Query query = getSession().createSQLQuery(sql);
        query.setParameter("processId", processId);
        Iterator iterator = query.list().iterator();

        Map<Integer, Map<String, Object>> result = new HashMap<>();
        while (iterator.hasNext()) {
            Object[] objects = (Object[]) iterator.next();
            Map<String, Object> invoicesMap = new HashMap<>();

            invoicesMap.put("symbol", objects[1].toString());
            invoicesMap.put("invoices", Integer.valueOf(objects[2].toString()));
            invoicesMap.put("totalInvoiced", Double.valueOf(objects[3].toString()));
            invoicesMap.put("totalCarried", Double.valueOf(objects[4].toString()));

            result.put(Integer.valueOf(objects[0].toString()), invoicesMap);
        }

        return result;
    }

    public List<Integer> findIdsByUserAndDate(Integer userId, Date since,
                                              Date until) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        addUserCriteria(criteria, userId);
        addPeriodCriteria(criteria, since, until);
        criteria.setProjection(Projections.id()).addOrder(Order.desc("createDatetime"));

        return criteria.list();
    }

    /**
     * This method returns the invoices that should be processed to calculate
     * the commissions for the given partner and period.
     *
     * @param partnerId
     * @param endDate
     */
    public List<Integer> findForPartnerCommissions(Integer partnerId, Date endDate) {
        DetachedCriteria invoicesWithCommissions = DetachedCriteria.forClass(InvoiceCommissionDTO.class)
                .createAlias("invoice", "_invoice")
                .add(Restrictions.eq("_invoice.deleted", 0))
                .createAlias("_invoice.baseUser", "_baseUser")
                .createAlias("_baseUser.customer", "_customer")
                .createAlias("_customer.partners", "_partner")
                .add(Restrictions.eq("_partner.id", partnerId))
                .add(Restrictions.eq("partner.id", partnerId))
                .add(Restrictions.le("_invoice.createDatetime", endDate))
                .setProjection(Property.forName("_invoice.id"));

        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isReview", 0))
                .createAlias("baseUser", "_baseUser")
                .createAlias("_baseUser.customer", "_customer")
                .createAlias("_customer.partners", "_partner")
                .add(Restrictions.eq("_partner.id", partnerId))
                .add(Restrictions.le("createDatetime", endDate))
                .add(Subqueries.propertyNotIn("id", invoicesWithCommissions))
                .setProjection(Property.forName("id"))
                .addOrder(Order.asc("createDatetime"));

        return criteria.list();
    }

    /**
     * Added for supporting OverdueInvoicePenaltyTask, to find unpaid, or paid after due date invoices
     *
     * @param userId
     * @return
     */
    public List<Integer> findLatePaidInvoicesForUser(Integer userId) {

        logger.debug("findLatePaidInvoicesForUser");

        String hql = "select distinct(invoice.id) " +
                "  from InvoiceDTO invoice right join invoice.paymentMap as map " +
                "  where ( (invoice.invoiceStatus.id = 1 and invoice.dueDate < map.createDatetime) ) " +
                "    and invoice.deleted = 0 " +
                "    and invoice.isReview = 0 " +
                "    and invoice.baseUser.id = :userId " +
                "  order by invoice.id desc";
        List<Integer> data = getSession()
                .createQuery(hql)
                .setParameter("userId", userId)
                .setComment("InvoiceDAS.findLatePaidInvoicesForUser " + userId)
                .list();
        return data;
    }

    /**
     * finds all the invoices with given order id
     *
     * @param orderId
     * @return
     */
    public List<InvoiceDTO> findInvoicesByOrder(Integer orderId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.createAlias("orderProcesses", "o");
        criteria.add(Restrictions.eq("o.purchaseOrder.id", orderId));
        criteria.addOrder(Order.desc("id"));
        return criteria.list();
    }

    /**
     * Finds customers who have unpaid invoices and have credit card or ACH as payment method
     *
     * @param entityId
     * @return list of Users
     */
    public Set<Integer> findInvoicesForAutoPayments(Integer entityId) {
        String FIND_INVOICES_FOR_AUTO_PAYMENTS = "select bu.id from base_user bu " +
                "INNER JOIN invoice inv ON bu.id = inv.user_id " +
                "INNER JOIN payment_information pi ON inv.user_id = pi.user_id " +
                "INNER JOIN payment_method_type pmt ON pi.payment_method_id = pmt.id " +
                "INNER JOIN payment_information_meta_fields_map pimm ON pimm.payment_information_id = pi.id " +
                "INNER JOIN meta_field_value mfv ON mfv.id = pimm.meta_field_value_id " +
                "where bu.entity_id =:entityId and pmt.template_id in(1,2) " +
                "and inv.status_id IN (select id from generic_status where status_value in(2,3) and dtype ='invoice_status') and " +
                "mfv.meta_field_name_id " +
                "IN (select id from meta_field_name where field_usage = 'AUTO_PAYMENT_AUTHORIZATION') " +
                "and mfv.boolean_value = 't' and bu.deleted = 0";

        List<Integer> users = getSession().createSQLQuery(FIND_INVOICES_FOR_AUTO_PAYMENTS)
                .setParameter("entityId", entityId).list();
        return new HashSet<Integer>(users); // to avoid duplicates
    }

    // used for the web services call to get the latest X
    public List<Integer> findInvoicesByUserIdOldestFirst(Integer userId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.createAlias("invoiceStatus", "s").add(Restrictions.ne("s.id", Constants.INVOICE_STATUS_PAID));
        criteria.add(Restrictions.ge("balance", Constants.BIGDECIMAL_ONE_CENT));
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.setProjection(Projections.id());
        criteria.addOrder(Order.asc("createDatetime"));
        return criteria.list();
    }

    /**
     * Get the count of invoices for the given userId.
     *
     * @param userId
     * @return Count
     */
    public Long getInvoiceCountByUserId(Integer userId, Date invoiceDate) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.le("createDatetime", invoiceDate));
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.setProjection(Projections.count("id"));
        return (Long) criteria.uniqueResult();
    }

    /**
     * Get the count of carried invoices for the given userId & less than invoice date.
     *
     * @param userId
     * @return Count
     */
    public Integer getCarriedInvoicesCountByUserIdAndInvoiceDate(Integer userId, Date invoiceDate) {
        String query = "select count(i.id) from invoice i, generic_status gs where i.user_id =:userId and i.create_datetime < :invoiceDate  and i.deleted = 0 and i.is_review=0 and i.status_id=gs.id and gs.status_value = " + CommonConstants.INVOICE_STATUS_UNPAID_AND_CARRIED + " and dtype ='invoice_status'";
        SQLQuery sqlQuery = getSession().createSQLQuery(query);
        sqlQuery.setParameter("userId", userId);
        sqlQuery.setParameter("invoiceDate", invoiceDate);
        return Integer.valueOf(sqlQuery.uniqueResult().toString());
    }

    /**
     * Retrieve invoices sorted by sortAttribute attribute and ordered specified in order attribute.
     *
     * @param userId
     * @param maxResults
     * @param offset
     * @param sortAttribute
     * @param order
     * @return
     */
    public List<InvoiceDTO> findInvoicesByUserPagedSortedByAttribute(Integer userId, int maxResults, int offset, String sortAttribute, ListField.Order order) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isReview", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("u.id", userId))
                .setMaxResults(maxResults)
                .setFirstResult(offset)
                .setComment("findInvoicesByUserPagedSortedByAttribute " + userId + " " + maxResults + " " + offset);
        if (ListField.Order.ASC.equals(order)) {
            criteria.addOrder(Order.asc(sortAttribute));
        } else {
            criteria.addOrder(Order.desc(sortAttribute));
        }
        return criteria.list();
    }

    public InvoiceDTO findInvoiceByPublicNumber(String invoiceNumber) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isReview", 0))
                .createAlias("baseUser", "u")
                .add(Restrictions.eq("publicNumber", invoiceNumber));

        return (InvoiceDTO) criteria.uniqueResult();
    }

    public Integer findInvoiceByMetaFieldValue(Integer entityId, String name, String value) {

        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .createAlias("baseUser", "user")
                .createAlias("user.company", "company")
                .setProjection(Projections.property("id"))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isReview", 0))
                .add(Restrictions.eq("metaField.name", name))
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.sqlRestriction("string_value =  ?", value, StringType.INSTANCE));

        return (Integer) criteria.uniqueResult();
    }

    public BigDecimal getTotalBalanceByInvoiceIds(Integer invoiceIds[]) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.in("id", invoiceIds));
        criteria.setProjection(Projections.sum("balance"));
        BigDecimal totalBalance = (BigDecimal) criteria.uniqueResult();
        return totalBalance != null ? totalBalance : BigDecimal.ZERO;
    }

    private static final String findInvoiceIdsByBillingProcessIdSQL =
            "select id from invoice where billing_process_id = :processId";

    /**
     * Returns List of InvoiceId by billing process id.
     *
     * @param processId
     * @return invoiceids
     */
    public List<Integer> getInvoiceIdsByBillingProcessId(Integer processId) {
        SQLQuery query = getSession().createSQLQuery(findInvoiceIdsByBillingProcessIdSQL);
        query.setParameter("processId", processId);
        List<Integer> invoiceIds = query.list();
        return invoiceIds != null ? invoiceIds : Collections.<Integer>emptyList();
    }

    private static final String findSecondLastInvoiceIdByUserIdSQL =
            "select max(i.id) from invoice i, invoice_line il where i.id = il.invoice_id and " +
                    "i.user_id =:userId and i.is_review = 0 and i.deleted = 0 and il.deleted = 0 and " +
                    "i.id <:invoiceId group by i.id having ROUND(sum(il.amount), 2) >= 0 order by i.id desc limit 1";

    /**
     * Returns Second Last Invoice Id on the basis of user id and invoice id
     * and except credit invoice (sum of invoice lines less than zero).
     *
     * @param userId
     * @return invoice Id
     */
    public Integer getLastInvoiceId(Integer userId, Integer invoiceId) {
        SQLQuery query = getSession().createSQLQuery(findSecondLastInvoiceIdByUserIdSQL);
        query.setParameter("userId", userId);
        query.setParameter("invoiceId", invoiceId);
        return (Integer) query.uniqueResult();
    }

    private static final String findUsagePlanId =
            "SELECT pi.plan_id AS plan_id "
                    + "FROM customer_price cp, plan_item pi "
                    + "WHERE cp.plan_item_id = pi.id "
                    + "AND pi.item_id =:itemId "
                    + "AND cp.user_id =:userId "
                    + "AND cp.price_expiry_date IS NULL "
                    + "AND cp.price_subscription_date = "
                    + "(SELECT MAX(cp.price_subscription_date) "
                    + "FROM customer_price cp, plan_item pi "
                    + "WHERE cp.plan_item_id = pi.id "
                    + "AND pi.item_id =:itemId "
                    + "AND cp.user_id =:userId "
                    + "AND cp.price_subscription_date <=:usageActiveSince)";

    private static final String findUsagePlanIdExpiryNotNull =
            "SELECT pi.plan_id AS plan_id "
                    + "FROM customer_price cp, plan_item pi "
                    + "WHERE cp.plan_item_id = pi.id "
                    + "AND pi.item_id =:itemId "
                    + "AND cp.user_id =:userId "
                    + "AND (cp.price_expiry_date >=:usageActiveSince)"
                    + "AND cp.price_subscription_date = "
                    + "(SELECT MAX(cp.price_subscription_date) "
                    + "FROM customer_price cp, plan_item pi "
                    + "WHERE cp.plan_item_id = pi.id "
                    + "AND pi.item_id =:itemId "
                    + "AND cp.user_id =:userId "
                    + "AND cp.price_subscription_date <=:usageActiveSince)";

    private static final String findSubscriptionOrderPlanId =
            "SELECT p.id "
                    + "FROM plan p, "
                    + "order_line ol "
                    + "WHERE p.item_id = ol.item_id "
                    + "AND ol.deleted = 0 "
                    + "AND ol.order_id IN (SELECT po.id "
                    + "FROM purchase_order po, "
                    + "order_line ol, "
                    + "asset_assignment aa, "
                    + "asset a "
                    + "WHERE a.id = aa.asset_id "
                    + "AND aa.order_line_id = ol.id "
                    + "AND po.id = ol.order_id "
                    + "AND po.user_id =:userId "
                    + "AND ol.deleted = 0 "
                    + "AND ol.item_id IS NOT NULL "
                    + "AND a.identifier =:identifier "
                    + "AND po.deleted = 0)";

    /**
     * Returns the usage plan Id for usage invoice line on the basis
     * of user id, item id and usage order active since date..
     * <p>
     * IMPORTANT NOTE: We have not handled this scenario which is very rare.
     * <p>
     * IF customer subscribes 2 plans which contains same bundle items.
     * AND Subscribes on same date so price subscription dates will also be same.
     * AND Both subscription orders will have active until date.
     * AND because of that both plans will have price expiry dats as well.
     * So, in that case system will not be able to find out usage plan id.
     *
     * @param userId
     * @param itemId
     * @param usageActiveSince
     * @return plan id.
     */
    public Integer getUsagePlanId(Integer userId, Integer itemId, Date usageActiveSince, String identifier) {
        List<?> usagePlanIdList =  getUsagePlanIdList(userId,itemId,usageActiveSince,findUsagePlanId);

        if (CollectionUtils.isNotEmpty(usagePlanIdList) ) {
            if (usagePlanIdList.size() > 1) {
                return getSubscriptionOrderPlanId(userId, identifier, findSubscriptionOrderPlanId);
            } else {
                return (Integer) usagePlanIdList.get(0);
            }
        }

        usagePlanIdList = getUsagePlanIdList(userId,itemId,usageActiveSince,findUsagePlanIdExpiryNotNull);
        if (CollectionUtils.isNotEmpty(usagePlanIdList) ) {
            if (usagePlanIdList.size() > 1) {
                return getSubscriptionOrderPlanId(userId, identifier, findSubscriptionOrderPlanId);
            } else {
                return (Integer) usagePlanIdList.get(0);
            }
        }

        return null;
    }

    /**
     * This method finds current effective/active plan for usage
     * product which itself is a bundle item of the plan.
     * <p>
     * The logic is based on latest price subscription date which
     * should be less than or equal to usage order's (one time)
     * active since date and should not have price expiry date.
     *
     * @param userId
     * @param itemId
     * @param usageActiveSince
     * @return
     */

    private static final String findInvoiceIdsSQL =
            "select inv.id from invoice inv INNER JOIN base_user bu ON bu.id = inv.user_id where inv.billing_process_id is null"
                    + " and inv.create_datetime >= :linkingStartDate and bu.entity_id = :entityId";

    /**
     * Returns List of InvoiceId where billing process id is null.
     *
     * @param entityId
     * @return invoiceids
     */
    public List<Integer> getBillingProcessUnlinkedInvoices(Integer entityId, Date linkingStartDate) {
        SQLQuery query = getSession().createSQLQuery(findInvoiceIdsSQL);
        query.setParameter("entityId", entityId);
        query.setParameter("linkingStartDate", linkingStartDate);
        List<Integer> invoiceIds = query.list();
        return invoiceIds != null ? invoiceIds : Collections.<Integer>emptyList();
    }

    private static final String saveBillingProceessLinkLog = "INSERT INTO billing_process_link_log (invoice_id, billing_process_id, "
            + "create_date) VALUES (:invoiceId, :billingProcessId, :createDateTime)";

    /**
     * Saves log of billing process and invoice linking process.
     *
     * @param invoiceId
     * @param billingProcessId
     */
    public void billingProceessLinkLog(Integer invoiceId, Integer billingProcessId) {
        Query query = getSession().createSQLQuery(saveBillingProceessLinkLog);
        query.setParameter("invoiceId", invoiceId);
        query.setParameter("billingProcessId", billingProcessId.intValue());
        query.setParameter("createDateTime", new Date());
        query.executeUpdate();
    }

    private static final String findInvoiceIdsByBillingProcessAndUserSQL =
            "select id from invoice where billing_process_id = :processId and user_id = :userId";

    /**
     * Returns List of InvoiceId by billing process and user .
     *
     * @param processId
     * @return invoiceids
     */
    public List<Integer> getInvoiceIdsByBillingProcessAndUser(Integer processId, Integer userId) {
        SQLQuery query = getSession().createSQLQuery(findInvoiceIdsByBillingProcessAndUserSQL);
        query.setParameter("processId", processId);
        query.setParameter("userId", userId);
        List<Integer> invoiceIds = query.list();
        return invoiceIds != null ? invoiceIds : Collections.<Integer>emptyList();
    }

    public List<Integer> findAllUnpaidInvoices(Integer userId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(FIND_ALL_UNPAID_INVOICES_SQL);
        sqlQuery.setParameter("userId", userId);
        sqlQuery.setParameter("statusValue", Constants.INVOICE_STATUS_UNPAID);
        return (List) sqlQuery.list();
    }

    public Integer findLastInvoiceForUser(Integer userId) {
        SQLQuery sqlQuery = getSession().createSQLQuery(FIND_LAST_INVOICE_FOR_USER_SQL);
        sqlQuery.setParameter("userId", userId);
        return (Integer) sqlQuery.uniqueResult();
    }

    public ScrollableResults getAllInvoicesForUser(Integer userId) {
        return getSession().createCriteria(InvoiceDTO.class)
                .createAlias("baseUser", "bu")
                .add(Restrictions.eq("bu.id", userId))
                .scroll();
    }

    @SuppressWarnings("unchecked")
    public SortedSet<Integer> getAllInvoiceIdForUser(Integer userId) {
        return new TreeSet<>(getSession().createCriteria(InvoiceDTO.class)
                .createAlias("baseUser", "bu")
                .add(Restrictions.eq("bu.id", userId))
                .setProjection(Projections.id())
                .addOrder(Order.asc("id"))
                .list());
    }

    private static final String TO_REMIND  =
        "SELECT i.* " +
        " FROM invoice i, base_user b " +
        " WHERE i.user_id = b.id " +
        " AND b.deleted = 0 " +
        " AND i.deleted = 0 " +
        " AND i.is_review = 0 " +
        " AND i.status_id = 27 " +
        " AND i.due_date > :dueDate " +
        " AND i.create_datetime <= :createDateTime " +
        " AND (i.last_reminder is null or " +
        "      i.last_reminder <= :lastReminder) " +
        " AND b.entity_id = :entityId";

    public ScrollableResults getListOfInvoicesToSendReminder(Integer entityId, Date dueDate, Date createDateTime, Date lastReminder) {
        SQLQuery query = getSession().createSQLQuery(TO_REMIND);
        query.addEntity(getPersistentClass());
        query.setParameter("entityId", entityId);
        query.setParameter("dueDate", dueDate);
        query.setParameter("createDateTime", createDateTime);
        query.setParameter("lastReminder", lastReminder);
        return query.scroll(ScrollMode.FORWARD_ONLY);
    }

    public List<?> getUsagePlanIdList(Integer userId, Integer itemId, Date usageActiveSince,String query) {
        return getSession().createSQLQuery(query)
                .setParameter("userId", userId).setParameter("itemId", itemId)
                .setParameter("usageActiveSince", usageActiveSince)
                .list();
    }

    public Integer getSubscriptionOrderPlanId(Integer userId, String identifier, String query) {
        return (Integer) getSession().createSQLQuery(query)
                .setParameter("userId", userId)
                .setParameter("identifier", identifier)
                .uniqueResult();
    }

    /**
     * Get the count of invoices for the given userId.
     *
     * @param userId
     * @return Count
     */
    public Long getInvoiceCountByUserId(Integer userId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class);
        criteria.add(Restrictions.eq("isReview", 0));
        criteria.add(Restrictions.eq("deleted", 0));
        criteria.createAlias("baseUser", "u");
        criteria.add(Restrictions.eq("u.id", userId));
        criteria.setProjection(Projections.count("id"));
        return (Long) criteria.uniqueResult();
    }

    private static final String findFirstOrderIdByInvoiceId =
            "SELECT l.order_id "
                    + "FROM invoice i, "
                    + "invoice_line l "
                    + "WHERE i.id = l.invoice_id "
                    + "AND i.id =:invoiceId "
                    + "GROUP BY l.order_id "
                    + "ORDER BY order_id ASC "
                    + "LIMIT 1 OFFSET 0";

    public Integer getFirstOrderIdByInvoiceId(Integer invoiceId) {
        return (Integer) getSession().createSQLQuery(findFirstOrderIdByInvoiceId)
                .setParameter("invoiceId", invoiceId)
                .uniqueResult();
    }
    public List<Integer> getInvoiceIdBetweenTowDates(Date startDate, Date endDate, Integer entityId) {
        Criteria criteria = getSession().createCriteria(InvoiceDTO.class)
                .createAlias("baseUser", "user")
                .createAlias("user.company", "company")
                .add(Restrictions.ge("createDatetime", startDate))
                .add(Restrictions.le("createDatetime", endDate))
                .setProjection(Projections.id())
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.eq("deleted", 0))
                .addOrder(Order.asc("id"));

        return criteria.list();
    }
}
