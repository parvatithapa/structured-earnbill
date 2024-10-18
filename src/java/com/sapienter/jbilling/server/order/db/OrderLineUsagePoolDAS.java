/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * OrderLineUsagePoolDAS
 * Does not contain any functions, only used for find method in AbstractDAS class.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class OrderLineUsagePoolDAS extends AbstractDAS<OrderLineUsagePoolDTO> {
	
	 /**
     * Returns the list of order line usage pool by given customer usage pool ID
     *
     * @param primaryOrderId
     * @return List<OrderDTO> - List of linked orders for the given Primary Order
     */
    @SuppressWarnings("unchecked")
    public List<OrderLineUsagePoolDTO> findByCustomerUsagePoolId(Integer customerUsagePoolId) {
        Criteria criteria = getSession().createCriteria(OrderLineUsagePoolDTO.class)
                .add(Restrictions.eq("customerUsagePool.id", customerUsagePoolId))
                .addOrder(Order.asc("id"));

        return criteria.list();
    }
    
    @SuppressWarnings("unchecked")
    public List<OrderLineUsagePoolDTO> findByUserItem(Integer userId, Integer itemId, Date startDate) {
        final String hql =
                "select olup" +
                        "  from OrderLineUsagePoolDTO olup " +
                        " where olup.orderLine.deleted = 0 " +
                        "   and olup.orderLine.item.id = :item " +
                        "   and olup.effectiveDate >= :effectiveDate " +
                        "   and olup.orderLine.purchaseOrder.baseUserByUserId.id = :user";

        Query query = getSession().createQuery(hql);
        query.setParameter("item", itemId);
        query.setParameter("user", userId);
        query.setParameter("effectiveDate", startDate);

        return query.list();
    }
    
    public BigDecimal getOrderLineTotalUsedFreeQuantity(Integer orderLineId, Integer freeUsagePoolId) {
    	final String hql =
                "select sum(olup.quantity)" +
                        " from OrderLineUsagePoolDTO olup " +
                        " where olup.orderLine.deleted = 0 " +
                        " and olup.orderLine.id = :orderLineId "+
                        " and olup.customerUsagePool.id <> :freeUsagePoolId";

        Query query = getSession().createQuery(hql);
        query.setParameter("orderLineId", orderLineId);
        query.setParameter("freeUsagePoolId", freeUsagePoolId);

    	return (BigDecimal) query.uniqueResult();
    }
    
}
