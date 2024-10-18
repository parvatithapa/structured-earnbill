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
package com.sapienter.jbilling.server.order.db;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;

import com.sapienter.jbilling.server.order.OrderProcessWS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderProcessDAS extends AbstractDAS<OrderProcessDTO> {
    
    //used to check of the order has any invoices (non deleted not cancelled)
    public List<Integer> findActiveInvoicesForOrder(Integer orderId) {

        String hql = "select pr.invoice.id" +
                     "  from OrderProcessDTO pr " +
                     "  where pr.purchaseOrder.id = :orderId" +
                     "    and pr.invoice.deleted = 0" + 
                     "    and pr.isReview = 0";

        List<Integer> data = getSession()
                        .createQuery(hql)
                        .setParameter("orderId", orderId)
                        .setComment("OrderProcessDAS.findActiveInvoicesForOrder " + orderId)
                        .list();
        return data;
    }

    public List<Integer> findByBillingProcess(Integer processId) {

        String hql = "select pr.id" +
                "  from OrderProcessDTO pr " +
                "  where pr.billingProcess.id =:processId";

        List<Integer> data = getSession()
                .createQuery(hql)
                .setParameter("processId", processId)
                .list();
        return data;
    }
    
    /**
     * Get Minimum Period start date of order from order_process table when isReview flag is 0.
     * @param orderId
     * @return
     */
    public Date getFirstInvoicePeriodStartDateByOrderId(Integer orderId) {
        
        String hql = "select min(pr.periodStart) from OrderProcessDTO pr " +
        "where pr.isReview = 0 " +
        "and pr.invoice.deleted = 0 " +
        "and pr.purchaseOrder.deleted = 0 " +
        "and pr.purchaseOrder.id = :orderId";
       
        Query query = getSession().createQuery(hql);
        query.setInteger("orderId", orderId);

        return (Date) query.uniqueResult();
       }
    
    private static final String findOrderPrcoessesByPrcoessIdSQL = "select id as id , order_id as orderId, invoice_id as invoiceId," +
    		"billing_process_id as billingProcessId, " +
    		"periods_included as periodsIncluded, period_start as periodStart, " +
    		"period_end as periodEnd, is_review as isReview, origin as origin " +
    		"from order_process where billing_process_id = :processId";
    
    /**Returns List of OrderProcessWS from Billing process Id.
     * Method does scalar query and wrap result into List 
     * of OrderProcessWS.
     * @param processId
     * @return List<OrderProcessWS>
     */
    public List<OrderProcessWS> getOrderPrcoessesByPrcoessId(Integer processId) {
		Query query = getSession().createSQLQuery(findOrderPrcoessesByPrcoessIdSQL)
	                .addScalar("id")
	                .addScalar("orderId")
	                .addScalar("invoiceId")
	                .addScalar("billingProcessId")
	                .addScalar("periodsIncluded")
	                .addScalar("periodStart")
	                .addScalar("periodEnd")
	                .addScalar("isReview")
	                .addScalar("origin")
	                .setResultTransformer(Transformers.aliasToBean(OrderProcessWS.class));
		query.setParameter("processId", processId);
		List<OrderProcessWS> results = query.list();
		return results!=null ? results: Collections.<OrderProcessWS>emptyList();
    }
    

    /**
     * Returns List of OrderProcessWS from InvoiceId
     * @param InvoiceId
     * @return
     */
    public List<OrderProcessDTO> getOrderProcessByInvoiceId( Integer InvoiceId) {
	Criteria criteria = getSession().createCriteria(OrderProcessDTO.class)
				.createAlias("invoice", "i")
				.add(Restrictions.eq("i.id",InvoiceId))
				.add(Restrictions.isNull("billingProcess"))
				.add(Restrictions.isNotNull("periodEnd"));
	return criteria.list();
    }
    
}
