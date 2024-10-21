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

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Alexander Aksenov
 * @since 09.07.13
 */
public class OrderChangeDAS extends AbstractDAS<OrderChangeDTO> {

    @SuppressWarnings("unchecked")
    public List<OrderChangeDTO> findByOrder(Integer orderId) {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("order", "o1")
                .add(Restrictions.eq("o1.deleted", 0))
                .add(Restrictions.eq("o1.id", orderId))
                .addOrder(Order.desc("id"))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .setComment("findOrderChangesByOrder " + orderId);
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<OrderChangeDTO> findByOrderLine(Integer orderLineId) {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("orderLine", "ol")
                .add(Restrictions.eq("ol.deleted", 0))
                .add(Restrictions.eq("ol.id", orderLineId))
                .addOrder(Order.desc("id"))
                .setComment("findOrderChangesByOrderLine " + orderLineId);
        return criteria.list();
    }

    public OrderChangeDTO findByOrderChangeByAssetIdInPlanItems(Integer assetId) {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("orderChangePlanItems", "ocpi")
                .createAlias("ocpi.assets", "a")
                .add(Restrictions.eq("a.deleted", 0))
                .add(Restrictions.eq("a.id", assetId))
                .setComment("findByOrderLineByAssetId " + assetId);
        return (OrderChangeDTO) criteria.uniqueResult();
    }

    public OrderChangeDTO findByOrderChangeByAssetId(Integer assetId) {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("orderChangePlanItems", "ocpi")
                .createAlias("ocpi.assets", "a")
                .add(Restrictions.eq("a.deleted", 0))
                .add(Restrictions.eq("a.id", assetId))
                .setComment("findByOrderLineByAssetId " + assetId)
                .addOrder(Order.desc("id"))
                .setMaxResults(1);
        return (OrderChangeDTO) criteria.uniqueResult();
    }

    private final static String FIND_CHANGES_FOR_HIERARCHY_GROUPING_SQL =
            "select new Map(change.id as changeId, o1.id as orderId, o2.id as parentId, o3.id as grandParentId) " +
            " from " + OrderChangeDTO.class.getSimpleName() + " change " +
                    " inner join change.order o1 " +
                    " left join o1.parentOrder o2 " +
                    " left join o2.parentOrder o3 " +
            " where o1.baseUserByUserId.company.id = :entityId " +
                    " and change.startDate >= :onDate and change.startDate < :toDate" +
                    " and change.status.id = :status";

    /**
     * Find order changes for grouping by orders hierarchy. Return change id, its order, parent order and grand parent order.
     * @param entityId Target entity id
     * @param onDate Date until application change date should be
     * @return List of maps, described order change and its order id with all order parents ids
     */
    public List<Map<String,Object>> findApplicableChangesForGrouping(Integer entityId, Date onDate) {

        Query query = getSession().createQuery(FIND_CHANGES_FOR_HIERARCHY_GROUPING_SQL)
                .setParameter("entityId", entityId)
                .setParameter("onDate", onDate)
                .setParameter("toDate", Util.addDays(onDate, 1))
                .setParameter("status", Constants.ORDER_CHANGE_STATUS_PENDING);
        return (List<Map<String,Object>>) query.list();
    }

    public boolean orderChangeHasStatus(Integer statusId)
    {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("orderStatusToApply", "o")
                .add(Restrictions.eq("o.id", statusId));
        List<OrderChangeDTO> orderChangeDTOs;
        orderChangeDTOs = criteria.list();
        if(!(orderChangeDTOs.isEmpty()))
        {
            return true;
        }
        else
        {
            return false;
        }

    }

    public List<Integer> findByOrderAndStatus(Integer orderId, Integer statusId) {
        return (List<Integer>) getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("order", "order")
                .createAlias("status", "status")
                .add(Restrictions.eq("order.deleted", 0))
                .add(Restrictions.eq("order.id", orderId))
                .add(Restrictions.eq("status.id", statusId))
                .setProjection(Projections.property("id"))
                .addOrder(Order.desc("id")).list();
    }

    private static final String FIND_ORDER_CHANGE_IDS_BY_ASSET_ID = "SELECT order_change_id FROM order_change_asset_map WHERE asset_id  = :assetId ";
    private static final String FIND_ORDER_CHANGE_IDS_FROM_ORDER_CHANGE_PLAN_BY_ASSET_ID = "SELECT order_change_id FROM order_change_plan_item WHERE id IN (SELECT order_change_plan_item_id FROM order_change_plan_item_asset_map ocpi WHERE ocpi.asset_id = :assetId)";
    public List<Integer> findOrderChangeIdsByAssetId(Integer assetId) {
        SQLQuery query = getSession().createSQLQuery(FIND_ORDER_CHANGE_IDS_BY_ASSET_ID);
        query.setParameter("assetId", assetId);
        @SuppressWarnings("unchecked")
        List<Integer> result = query.list();

        SQLQuery planItemQuery = getSession().createSQLQuery(FIND_ORDER_CHANGE_IDS_FROM_ORDER_CHANGE_PLAN_BY_ASSET_ID);
        planItemQuery.setParameter("assetId", assetId);
        result.addAll(planItemQuery.list());
        return result;
    }

    public boolean isAssetAttachedToOtherOrderChanges(Integer currentOrderChangeId, List<Integer> relatedOrderChangeIds) {
        return (Long)getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("order", "order")
                .add(Restrictions.eq("order.deleted", 0))
                .add(Restrictions.ne("id", currentOrderChangeId))
                .add(Restrictions.in("id", relatedOrderChangeIds))
                .setProjection(Projections.rowCount()).uniqueResult() > 0;
    }

    public List<OrderChangeDTO> getOrderChangeDtos(Integer currentOrderChangeId, List<Integer> relatedOrderChangeIds){
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
        .createAlias("order", "order")
        .add(Restrictions.eq("order.deleted", 0))
        .add(Restrictions.ne("id", currentOrderChangeId))
        .add(Restrictions.in("id", relatedOrderChangeIds));
        return criteria.list();
    }
        
    public List<OrderChangeDTO> getOrderChangesWithEffectiveDate(Integer userId, Date effectiveDate) {
        Criteria criteria = getSession().createCriteria(OrderChangeDTO.class)
                .createAlias("order", "order")
                .add(Restrictions.eq("order.deleted", 0))
                .createAlias("order.baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.isNotNull("startDate"))
                .add(Restrictions.eq("startDate", effectiveDate))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }
}
