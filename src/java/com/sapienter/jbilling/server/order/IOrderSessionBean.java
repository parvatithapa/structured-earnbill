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

package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

/**
 *
 * This is the session facade for the orders in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 **/
public interface IOrderSessionBean {
    
    public void reviewNotifications(Date today);

    public OrderDTO getOrder(Integer orderId);

    public OrderDTO getOrderEx(Integer orderId, Integer languageId);

    public OrderDTO setStatus(Integer orderId, Integer statusId, 
            Integer executorId, Integer languageId);

    /**
     * This is a version used by the http api, should be
     * the same as the web service but without the 
     * security check
    public Integer create(OrderWS order, Integer entityId,
            String rootUser, boolean process);
     */

    public void delete(Integer id, Integer executorId);
 
    public OrderPeriodDTO[] getPeriods(Integer entityId, Integer languageId);

    public OrderPeriodDTO getPeriod(Integer languageId, Integer id);

    public void setPeriods(Integer languageId, OrderPeriodDTO[] periods);

    public void addPeriod(Integer entityId, Integer languageId);

    public Boolean deletePeriod(Integer periodId);

    public OrderDTO addItem(Integer itemID, BigDecimal quantity, OrderDTO order,
            Integer languageId, Integer userId, Integer entityId);
    
    public OrderDTO addItem(Integer itemID, Integer quantity, OrderDTO order,
            Integer languageId, Integer userId, Integer entityId);

    public OrderDTO recalculate(OrderDTO modifiedOrder, Integer entityId);

    public Integer createUpdate(Integer entityId, Integer executorId, Integer languageId,
            OrderDTO order,  Collection<OrderChangeDTO> orderChanges, Collection<Integer> deletedChanges);

    public Long getCountWithDecimals(Integer itemId);

    public void applyChangesToOrders(Collection<Integer> orderChangeIdsForHierarchy, Date onDate, Integer entityId);

    public void applyOrderChangesToOrders(Collection<OrderChangeDTO> orderChanges, Collection<OrderDTO> ordersForUpdate, Date onDate, Integer entityId,
                                          boolean throwOnError);

    void markOrderChangesAsApplyError(Integer entityId, Collection<Integer> orderChangeIds, Date onDate, String errorCode, String errorMessage);
    
    public void save(OrderDTO order);
}
