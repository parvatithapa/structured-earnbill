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

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class OrderStatusDAS extends AbstractDAS<OrderStatusDTO> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderStatusDAS.class));

    public OrderStatusDTO createOrderStatus (OrderStatusDTO orderStatus) {
        return save(orderStatus);
    }

    public OrderStatusWS findOrderStatusById (Integer orderStatusId) {
        return OrderStatusBL.getOrderStatusWS(findNow(orderStatusId));
    }

    public int findByOrderStatusFlag (OrderStatusFlag orderStatusFlag, Integer entityId) {

        return getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", orderStatusFlag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .list()
                .size();
    }

    @SuppressWarnings("unchecked")
    public List<OrderStatusDTO> findAllByOrderStatusFlag (OrderStatusFlag orderStatusFlag, Integer entityId) {

        return getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", orderStatusFlag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .addOrder(Order.asc("id"))
                .list();
    }

    public int getDefaultOrderStatusId (OrderStatusFlag flag, Integer entityId) {
        Criteria criteria = getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", flag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .addOrder(Order.asc("id"))
                .setMaxResults(1);
        @SuppressWarnings("unchecked")
        List<OrderStatusDTO> list = criteria.list();
        LOG.debug("Order Status Dto == %s", list.get(0));
        OrderStatusDTO orderStatusDTO = list.get(0);
        LOG.debug("Order Status Dto Id == %s", orderStatusDTO.getId());
        return orderStatusDTO.getId();
    }

    @SuppressWarnings("unchecked")
    public List<OrderStatusDTO> findAll (Integer companyId) {
        return getSession()
                .createCriteria(OrderStatusDTO.class)
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", companyId))
                .list();
    }

    public OrderStatusDTO findByOrderStatusFlagAndEntityId(OrderStatusFlag orderStatusFlag, Integer entityId) {
        return (OrderStatusDTO) getSession()
                .createCriteria(OrderStatusDTO.class)
                .add(Restrictions.eq("orderStatusFlag", orderStatusFlag))
                .createAlias("entity", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .addOrder(Order.asc("id"))
                .uniqueResult();
    }
  }
