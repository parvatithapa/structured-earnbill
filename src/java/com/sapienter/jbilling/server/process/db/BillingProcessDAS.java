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

package com.sapienter.jbilling.server.process.db;

import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.order.OrderStatusFlag;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class BillingProcessDAS extends AbstractDAS<BillingProcessDTO> {

    public BillingProcessDTO create(CompanyDTO entity, Date billingProcess,
            Integer periodId, Integer periodValue, Integer retries) {

        PeriodUnitDTO period = new PeriodUnitDAS().find(periodId);
        BillingProcessDTO dto = new BillingProcessDTO();
        dto.setEntity(entity);
        dto.setBillingDate(billingProcess);
        dto.setPeriodUnit(period);
        dto.setPeriodValue(periodValue);
        dto.setIsReview(0);        

        return save(dto);
    }

    public BillingProcessDTO findReview(Integer entityId) {
        Criteria criteria = getSession().createCriteria(BillingProcessDTO.class);
        criteria.createAlias("entity", "ent").add(Restrictions.eq("ent.id", entityId));
        criteria.add(Restrictions.eq("isReview", 1));

        return (BillingProcessDTO) criteria.uniqueResult();
    }

    public BillingProcessDTO isPresent(Integer entityId, Integer isReview, Date billingDate) {
        Criteria criteria = getSession().createCriteria(BillingProcessDTO.class);
        criteria.createAlias("entity", "ent").add(Restrictions.eq("ent.id", entityId));
        criteria.add(Restrictions.eq("isReview", isReview));
        criteria.add(Restrictions.eq("billingDate", billingDate));

        return (BillingProcessDTO) criteria.uniqueResult();
    }

    public ScrollableResults findUsersToProcess(int entityId) {
        Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("company", "c")
                .add(Restrictions.eq("c.id", entityId))
                .createAlias("userStatus", "us")
                .createAlias("us.ageingEntityStep", "agStep", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.or(
                        Restrictions.eq("us.id", UserDTOEx.STATUS_ACTIVE),
                        Restrictions.eq("agStep.suspend", 0)
                ))
                .setProjection(Projections.id())
                .setComment("BillingProcessDAS.findUsersToProcess " + entityId);
        return criteria.scroll();
    }

    public ScrollableResults findAllUsersExceptLastStepOfCollectionToProcess(int entityId) {
        
    	DetachedCriteria maxDays = DetachedCriteria.forClass(AgeingEntityStepDTO.class)
    			.add(Restrictions.eq("company.id", entityId))
    			.setProjection(Property.forName("days").max());
    	
    	DetachedCriteria ageingStepCollectionId = DetachedCriteria.forClass(AgeingEntityStepDTO.class)
    			.add(Restrictions.eq("company.id", entityId))
    			.add(Subqueries.propertyIn("days", maxDays))
    			.setProjection(Property.forName("id"));
    	
    	Criteria criteria = getSession().createCriteria(UserDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("company", "c")
                .add(Restrictions.eq("c.id", entityId))
                .createAlias("userStatus", "us")
                .createAlias("us.ageingEntityStep", "agStep", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.or(
                        Restrictions.eq("us.id", UserDTOEx.STATUS_ACTIVE),
                        Subqueries.propertyNotIn("agStep.id", ageingStepCollectionId)
                ))
                .setProjection(Projections.id())
                .setComment("BillingProcessDAS.findUsersToProcess " + entityId);
        return criteria.scroll();
    }

    public void reset() {
        getSession().flush();
        getSession().clear();
    }
    
    public List getCountAndSum(Integer processId) {
        final String hql =
                "select count(id), sum(total), currency.id " +
                "  from InvoiceDTO " +
                " where billingProcess.id = :processId " +
                " group by currency.id";

        Query query = getSession().createQuery(hql);
        query.setParameter("processId", processId);
        return query.list();
    }
    
    /**
     * Search succesfull payments in Payment_Invoice map (with quantity > 0)
     * and returns result, groupped by currency
     *
     * @param processId
     * @return Iterator with currency, method and sum of amount fields of query
     */
    public List getSuccessfulProcessCurrencyMethodAndSum(Integer processId) {
        final String hql =
                "select invoice.currency.id, method.id, sum(invoice.total) " +
                "  from InvoiceDTO invoice inner join invoice.paymentMap paymentMap " +
                " join paymentMap.payment payment join payment.paymentMethod method " +
                " where invoice.billingProcess.id = :processId and paymentMap.amount > 0" +
                " group by invoice.currency.id, method.id " +
                " having sum(invoice.total) > 0";

        Query query = getSession().createQuery(hql);
        query.setParameter("processId", processId);
        return query.list();
    }

    /**
     * Selection records from Invoice table without payment records or
     * with payments of 0 amount. Result groupped by currency
     * @param processId
     * @return Iterator with currency and amount value
     */
    public List getFailedProcessCurrencyAndSum(Integer processId) {
        final String hql =
                "select invoice.currency.id, sum(invoice.total) " +
                "  from InvoiceDTO invoice left join invoice.paymentMap paymentMap" +
                " where invoice.billingProcess.id = :processId and (paymentMap is NULL or paymentMap.amount = 0) " +
                " group by invoice.currency.id";


        Query query = getSession().createQuery(hql);
        query.setParameter("processId", processId);
        return query.list();
    }

    private static final String BILLABLE_USERS_TO_PROCESS =
            "SELECT a.id "
            + " FROM UserDTO a, OrderDTO o"
            + " WHERE a.id = o.baseUserByUserId.id"
            + " AND ( "
            + "     o.nextBillableDay is null "
            + "     or cast(o.nextBillableDay as date) <= :dueDate "
            + " ) "
            + " AND o.deleted = 0 "
            + " AND a.company.id = :entity ";

    public ScrollableResults findBillableUsersToProcess(int entityId, Date processDate) {
        Query query = getSession().createQuery(BILLABLE_USERS_TO_PROCESS);
        query.setParameter("dueDate", processDate);
        query.setParameter("entity", entityId);

        return query.scroll();
    }

    private static final String BILLABLE_USERS_WITH_ORDER_HQL =
            "select user.id "
            + " from OrderDTO purchaseOrder "
            + "     join purchaseOrder.baseUserByUserId as user "
            + " where "
            + "     user.deleted = 0 "
            + "     and user.company.id = :entity_id "
            + "     and purchaseOrder.orderStatus.orderStatusFlag = :active_status_flag "
            + "     and ( "
            + "         purchaseOrder.nextBillableDay is null"
            + "         or cast(purchaseOrder.nextBillableDay as date) <= :process_date "
            + "     )";

    /**
     * Returns all billable users with an order to process. This can be either any open one-time
     * order or an active recurring order with a valid next billable date.
     *
     * @param entityId entity id to find orders for
     * @param processDate billing process run date
     * @return billable users
     */
    public ScrollableResults findBillableUsersWithOrdersToProcess(int entityId, Date processDate) {
        Query query = getSession().createQuery(BILLABLE_USERS_WITH_ORDER_HQL);
        query.setParameter("entity_id", entityId);
        query.setParameter("active_status_flag", OrderStatusFlag.INVOICE);
        query.setParameter("process_date", processDate);
        
        return query.scroll();
    }
    
    private static final String findOrderProcessCountSQL =
    		"SELECT count(*) " +
    				"  FROM OrderProcessDTO op " +
    				" WHERE op.billingProcess.id = :billingProcessId ";

    private static final String findInvoiceProcessCountSQL =
    		"SELECT count(*) " +
    				"  FROM InvoiceDTO i " +
    				" WHERE i.billingProcess.id = :billingProcessId "+
    				"   AND i.deleted = 0";

    public Long getOrderProcessCount(Integer billingProcessId) {
    	Query query = getSession().createQuery(findOrderProcessCountSQL);
    	query.setParameter("billingProcessId", billingProcessId);
    	return (Long) query.uniqueResult();
    }

    public Long getInvoiceProcessCount(Integer billingProcessId) {
    	Query query = getSession().createQuery(findInvoiceProcessCountSQL);
    	query.setParameter("billingProcessId", billingProcessId);
    	return (Long) query.uniqueResult();
    }

    private static final String findLastBillingProcessDateSQL = "select max(billing_date) from billing_process where entity_id = :entityId and is_review = 0";
    
    /**
     * Get the last billing process run date based on entityId
     * @param entityId
     * @return
     */
    public Date getLastBillingProcessDate(Integer entityId){
    	SQLQuery query = getSession().createSQLQuery(findLastBillingProcessDateSQL);
    	query.setParameter("entityId", entityId);
    	return (Date) query.uniqueResult();
    }
    
    private static final String findBillingProcessIdSQL = "select min(id) from billing_process where billing_process.billing_date >= :invoicePeriodDate and entity_id = :entityId and billing_process.is_review = 0";
    
    /**
     * Get billing process Id for invoice on the basis of invoicePeriodDate and entityId
     * @param invoicePeriodDate
     * @param entityId
     * @return
     */
    public Integer getBillingProcessIdForInvoice(Date invoicePeriodDate, Integer entityId) {
    	SQLQuery query = getSession().createSQLQuery(findBillingProcessIdSQL);
    	query.setParameter("invoicePeriodDate", invoicePeriodDate);
    	query.setParameter("entityId", entityId);
    	return (Integer) query.uniqueResult();
    }
}
