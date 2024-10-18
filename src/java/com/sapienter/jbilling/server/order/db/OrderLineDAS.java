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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.transform.Transformers;
import org.hibernate.type.LongType;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

public class OrderLineDAS extends AbstractDAS<OrderLineDTO> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderLineDAS.class));

    public Long findLinesWithDecimals(Integer itemId) {

        final String hql =
                "select count(*)" +
                        "  from OrderLineDTO ol " +
                        " where ol.deleted = 0 " +
                        "   and ol.item.id= :item and (ol.quantity - cast(ol.quantity as integer)) <> 0";

        Query query = getSession().createQuery(hql);
        query.setParameter("item", itemId);

        return (Long) query.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findByUserItem(Integer userId, Integer itemId) {
        final String hql =
                "select ol" +
                        "  from OrderLineDTO ol " +
                        " where ol.deleted = 0 " +
                        "   and ol.item.id = :item " +
                        "   and ol.purchaseOrder.baseUserByUserId.id = :user";

        Query query = getSession().createQuery(hql);
        query.setParameter("item", itemId);
        query.setParameter("user", userId);

        return query.list();
    }

    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findRecurringByUser(Integer userId) {
        final String hql =
                "select line "
                        + "  from OrderLineDTO line "
                        + "where line.deleted = 0 "
                        + "  and line.purchaseOrder.baseUserByUserId.id = :userId "
                        + "  and line.purchaseOrder.orderPeriod.id != :period "
                        + "  and line.purchaseOrder.orderStatus.orderStatusFlag = :status "
                        + "  and line.purchaseOrder.deleted = 0 ";

        Query query = getSession().createQuery(hql);
        query.setParameter("userId", userId);
        query.setParameter("period", Constants.ORDER_PERIOD_ONCE);
        query.setParameter("status", OrderStatusFlag.INVOICE.ordinal());

        return query.list();
    }

    /**
     * Returns the first recurring order line found for the given user and item ID.
     *
     * @param userId user id
     * @param itemId item id
     * @return first recurring order line found, null if none found
     */
    @SuppressWarnings("unchecked")
    public OrderLineDTO findRecurringByUserItem(Integer userId, Integer itemId) {
        final String hql =
                "select line "
                        + "  from OrderLineDTO line "
                        + "where line.deleted = 0 "
                        + "  and line.item.id = :itemId "
                        + "  and line.purchaseOrder.baseUserByUserId.id = :userId "
                        + "  and line.purchaseOrder.orderPeriod.id != :period "
                        + "  and line.purchaseOrder.orderStatus.orderStatusFlag = :status "
                        + "  and line.purchaseOrder.deleted = 0 ";

        Query query = getSession().createQuery(hql);
        query.setParameter("itemId", itemId);
        query.setParameter("userId", userId);
        query.setParameter("period", Constants.ORDER_PERIOD_ONCE);
        query.setParameter("status", OrderStatusFlag.INVOICE.ordinal());

        return findFirst(query);
    }

    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findOnetimeByUserItem(Integer userId, Integer itemId) {
        final String hql =
                "select line "
                        + "  from OrderLineDTO line "
                        + "where line.deleted = 0 "
                        + "  and line.item.id = :itemId "
                        + "  and line.purchaseOrder.baseUserByUserId.id = :userId "
                        + "  and line.purchaseOrder.orderPeriod.id = :period "
                        + "  and line.purchaseOrder.orderStatus.orderStatusFlag = :status "
                        + "  and line.purchaseOrder.deleted = 0 ";

        Query query = getSession().createQuery(hql);
        query.setParameter("itemId", itemId);
        query.setParameter("userId", userId);
        query.setParameter("period", Constants.ORDER_PERIOD_ONCE);
        query.setParameter("status", OrderStatusFlag.INVOICE.ordinal());

        return query.list();
    }

    /**
     * Returns a list of all active and finished one time orders going back n number
     * of months, containing the given item id for the given user.
     *
     * @param userId user id of orders
     * @param itemId item id of order lines
     * @param months previous number of months to include (1 = this month plus the previous)
     * @return list of found one-time orders, empty list if none found
     */
    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findOnetimeByUserItem(Integer userId, Integer itemId, Integer months) {
        final String hql =
                "select line "
                        + "  from OrderLineDTO line "
                        + "where line.deleted = 0 "
                        + "  and line.item.id = :itemId "
                        + "  and line.purchaseOrder.baseUserByUserId.id = :userId "
                        + "  and line.purchaseOrder.orderPeriod.id = :period "
                        + "  and (line.purchaseOrder.orderStatus.orderStatusFlag = :active_status"
                        + "       or line.purchaseOrder.orderStatus.orderStatusFlag = :finished_status)"
                        + "  and line.purchaseOrder.deleted = 0 "
                        + "  and line.purchaseOrder.createDate > :startdate";

        Query query = getSession().createQuery(hql);
        query.setParameter("itemId", itemId);
        query.setParameter("userId", userId);
        query.setParameter("period", Constants.ORDER_PERIOD_ONCE);
        query.setParameter("active_status", OrderStatusFlag.INVOICE.ordinal());
        query.setParameter("finished_status", OrderStatusFlag.FINISHED.ordinal());

        LocalDate startDate = TimezoneHelper.companyCurrentLDTByUserId(userId).minusMonths(months).toLocalDate();
        query.setParameter("startdate", DateConvertUtils.asUtilDate(startDate));

        return query.list();
    }

    /**
     * Returns a list of all active and finished one-time orders going back n number
     * of months, for all direct immediate of the given parent user id. This is useful for
     * determining usage across all child users.
     *
     * @param parentUserId parent user id
     * @param itemId item id of order lines
     * @param months previous number of months to include (1 = 1 month period starting from today)
     * @return list of found one-time orders, empty list if none found
     */
    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findOnetimeByParentUserItem(Integer parentUserId, Integer itemId, Integer months) {
        UserDTO parent = new UserBL(parentUserId).getEntity();
        if (parent == null || parent.getCustomer() == null) {
            LOG.warn("Parent user " + parentUserId + " does not exist or is not a customer!");
            return Collections.emptyList();
        }

        final String hql =
                "select line "
                        + " from OrderLineDTO line "
                        + " where line.deleted = 0 "
                        + "  and line.item.id = :itemId "
                        + "  and line.purchaseOrder.baseUserByUserId.customer.parent.id = :parentId"
                        + "  and line.purchaseOrder.orderPeriod.id = :period "
                        + "  and (line.purchaseOrder.orderStatus.orderStatusFlag = :active_status"
                        + "       or line.purchaseOrder.orderStatus.orderStatusFlag = :finished_status)"
                        + "  and line.purchaseOrder.deleted = 0 "
                        + "  and line.purchaseOrder.createDate > :startdate ";

        Query query = getSession().createQuery(hql);
        query.setParameter("itemId", itemId);
        query.setParameter("parentId", parent.getCustomer().getId());
        query.setParameter("period", Constants.ORDER_PERIOD_ONCE);
        query.setParameter("active_status", OrderStatusFlag.INVOICE.ordinal());
        query.setParameter("finished_status", OrderStatusFlag.FINISHED);

        LocalDate startDate = TimezoneHelper.companyCurrentLDTByUserId(parentUserId).minusMonths(months).toLocalDate();
        query.setParameter("startdate", DateConvertUtils.asUtilDate(startDate));

        return query.list();
    }

    /**
     * Find order lines by user ID and description.
     *
     * @param userId user id
     * @param description order line description to match
     * @return list of found orders lines, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findByDescription(Integer userId, String description) {
        final String hql =
                "select line "
                        + "  from OrderLineDTO line "
                        + "where line.deleted = 0 "
                        + "  and line.purchaseOrder.deleted = 0 "
                        + "  and line.purchaseOrder.baseUserByUserId.id = :userId "
                        + "  and line.description = :description";

        Query query = getSession().createQuery(hql);
        query.setParameter("userId", userId);
        query.setParameter("description", description);

        return query.list();
    }

    /**
     * Find order lines by user ID and where description is like the given string. This method
     * can accept wildcard characters '%' for matching.
     *
     * @param userId user id
     * @param like string to match against order line description
     * @return list of found orders lines, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderLineDTO> findByDescriptionLike(Integer userId, String like) {
        final String hql =
                "select line "
                        + "  from OrderLineDTO line "
                        + "where line.deleted = 0 "
                        + "  and line.purchaseOrder.deleted = 0 "
                        + "  and line.purchaseOrder.baseUserByUserId.id = :userId "
                        + "  and line.description like :description";

        Query query = getSession().createQuery(hql);
        query.setParameter("userId", userId);
        query.setParameter("description", like);

        return query.list();
    }

    final static String GET_ORDER_LINE_ID_BY_ORDER_ID =
            "SELECT line.id "
                    + "FROM order_line line "
                    + "WHERE line.deleted = 0 "
                    + "AND line.order_id = :orderId ";

    public List<Integer> findOrderLinesByOrder(Integer orderId) {
        SQLQuery query = getSession().createSQLQuery(GET_ORDER_LINE_ID_BY_ORDER_ID);
        query.setParameter("orderId", orderId);
        @SuppressWarnings("unchecked")
        List<Integer> result = query.list();
        return result!=null ? result: Collections.<Integer>emptyList();
    }

    final String getOrderLineValuesById = "select item_id as itemId, quantity as quantity, price as price, amount as amount, " +
            "description as description, call_counter as callCounter from order_line where id = :lineId";

    public OrderLineFieldsWrapper getOrderLineValuesByLineId(Integer lineId) {
        Query query = getSession().createSQLQuery(getOrderLineValuesById)
                .addScalar("itemId")
                .addScalar("quantity")
                .addScalar("price")
                .addScalar("amount")
                .addScalar("description")
                .addScalar("callCounter", new LongType())
                .setResultTransformer(Transformers.aliasToBean(OrderLineFieldsWrapper.class));
        query.setParameter("lineId", lineId);
        return (OrderLineFieldsWrapper) query.uniqueResult();
    }

    private static final String FIND_LINE_BY_ITEM_AND_CALLIDENTIFIER_SQL =
            "SELECT * FROM order_line "
                    + "      WHERE order_id = :orderId "
                    + "        AND item_id = :itemId "
                    + "        AND call_identifier = :callIdentifier";

    public OrderLineDTO findOrderLineByItemAndCallIdentifier(Integer orderId, Integer itemId, String callIdentifier) {
        return (OrderLineDTO) getSession().createSQLQuery(FIND_LINE_BY_ITEM_AND_CALLIDENTIFIER_SQL)
                .addEntity(getPersistentClass())
                .setParameter("orderId", orderId)
                .setParameter("itemId", itemId)
                .setParameter("callIdentifier", callIdentifier)
                .uniqueResult();
    }
}
