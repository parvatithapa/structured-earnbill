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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.ArrayUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Expression;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.DateType;
import org.hibernate.type.StringType;
import org.hibernate.type.IntegerType;
import org.joda.time.DateMidnight;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.db.AbstractDAS;


public class OrderDAS extends AbstractDAS<OrderDTO> {

    /**
     * Returns the newest active order for the given user id and period.
     *
     * @param userId user id
     * @param period period
     * @return newest active order for user and period.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findByUserAndPeriod(Integer userId, OrderPeriodDTO period, Date activeSince) {
        //we should strip time information
        activeSince= new DateMidnight(activeSince).toDate();

        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.eq("orderPeriod", period))
                //cast as date to compare only the date part, ignore time
                .add(Restrictions.sqlRestriction("cast(active_since as date) = ?", activeSince, DateType.INSTANCE))
                .addOrder(Order.asc("id"))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    /**
     * Returns the newest active order for the given user id and period and parent.
     *
     * @param userId user id
     * @param period period
     * @param parentOrder parentOrder
     * @return newest active order for user and period and parentOrder.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findByUserAndPeriodAndParentOrder(Integer userId, OrderPeriodDTO period, OrderDTO parentOrder) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.eq("orderPeriod", period))
                .add(Restrictions.eq("parentOrder", parentOrder))
                .addOrder(Order.asc("id"))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    /**
     * Returns the list of linked orders by given Primary Order Id
     *
     * @param primaryOrderId
     * @return List<OrderDTO> - List of linked orders for the given Primary Order
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findByPrimaryOrderId(Integer primaryOrderId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("parentOrder.id", primaryOrderId))
                .addOrder(Order.asc("id"));

        return criteria.list();
    }

    /**
     * Returns an order by id and that is deleted or not depending on the isDeleted parameter.
     *
     * @param orderId   Id of the order to find.
     * @param isDeleted <b>true</b> if we want to find a deleted order and <b>false</b> if we want a not deleted order.
     * @return Order retrieved by id and deleted true/false.
     */
    public OrderDTO findByIdAndIsDeleted(Integer orderId, boolean isDeleted) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("id", orderId))
                .add(Restrictions.eq("deleted", isDeleted ? 1 : 0))
                .setMaxResults(1);

        return (OrderDTO) criteria.uniqueResult();
    }

    public List<OrderDTO> findOrdersByUserPaged(Integer userId, Integer maxResults, Integer offset) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.desc("id"));
        StringBuilder comment = new StringBuilder("findOrdersByUserPaged, userId:" + userId);
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

    /**
     * Returns the oldest active order for the given user id that contains an item
     * with the given id and period different to once.
     *
     * @param userId user id
     * @param itemId item id
     * @return newest active order for user and period.
     */
    @SuppressWarnings("unchecked")
    public OrderDTO findRecurringOrder(Integer userId, Integer itemId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("lines", "l")
                .createAlias("l.item", "i")
                .add(Restrictions.eq("i.id", itemId))
                .addOrder(Order.desc("id"))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    /**
     * Returns the recurring (period different to once) active orders for the given user id.
     *
     * @param userId user id
     * @return All recurring active orders.
     */
    public List<OrderDTO> findRecurringOrders(Integer userId) {
        return findRecurringOrders(userId, new OrderStatusFlag[] { OrderStatusFlag.INVOICE });
    }

    @SuppressWarnings("unchecked")
    public List<OrderDTO> findRecurringOrders(Integer userId, OrderStatusFlag[] orderStatusFlags) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.in("s.orderStatusFlag", orderStatusFlags))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .addOrder(Order.desc("activeUntil"));
        return criteria.list();
    }

    public OrderProcessDTO findProcessByEndDate(Integer id, Date myDate) {
        return (OrderProcessDTO) getSession().createFilter(find(id).getOrderProcesses(),
                "where this.periodEnd = :endDate").setDate("endDate",
                        Util.truncateDate(myDate)).uniqueResult();

    }

    /**
     * Finds active recurring orders for a given user
     * @param userId
     * @return
     */
    public List<OrderDTO> findByUserSubscriptions(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("orderStatus", "s");

        Criterion orderActive = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE);

        Criterion orderFinished = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.FINISHED);
        Criterion untilFuture = Restrictions.gt("activeUntil", TimezoneHelper.companyCurrentDateByUserId(userId));
        LogicalExpression finishInFuture= Restrictions.and(orderFinished, untilFuture);

        LogicalExpression orderActiveOrEndsLater= Restrictions.or(orderActive, finishInFuture);

        // Criteria or condition
        criteria.add(orderActiveOrEndsLater);

        return criteria.list();
    }

    /**
     * find all orders of the user having free trial subscription
     *
     * @param userId
     * @return List<OrderDTO>
     */
    public List<OrderDTO> findByUserSubscriptionsAndFreeTrialSubscription(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("orderStatus", "s")
                .createAlias("lines", "line")
                .createAlias("line.item", "item")
                .createAlias("item.plans", "plan")
                .add(Restrictions.eq("plan.freeTrial", true));

        Criterion orderActive = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE);

        Criterion orderFinished = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.FINISHED);
        Criterion untilFuture = Restrictions.gt("activeUntil", TimezoneHelper.companyCurrentDateByUserId(userId));
        LogicalExpression finishInFuture= Restrictions.and(orderFinished, untilFuture);

        LogicalExpression orderActiveOrEndsLater= Restrictions.or(orderActive, finishInFuture);

        // Criteria or condition
        criteria.add(orderActiveOrEndsLater);

        return criteria.list();
    }

    public List<OrderDTO> findByPlanUserFreeTrialSubscription(Integer userId, Integer planId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("orderStatus", "s")
                .createAlias("lines", "line")
                .createAlias("line.item", "item")
                .createAlias("item.plans", "plan")
                .add(Restrictions.eq("plan.freeTrial", true))
                .add(Restrictions.eq("plan.id", planId));

        Criterion orderActive = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE);

        Criterion orderFinished = Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.FINISHED);
        Criterion untilFuture = Restrictions.gt("activeUntil", TimezoneHelper.companyCurrentDateByUserId(userId));
        LogicalExpression finishInFuture= Restrictions.and(orderFinished, untilFuture);

        LogicalExpression orderActiveOrEndsLater= Restrictions.or(orderActive, finishInFuture);

        // Criteria or condition
        criteria.add(orderActiveOrEndsLater);

        return criteria.list();
    }

    /**
     * Find order of the given user by asset number
     *
     * @param userId
     * @param assetNumber
     * @return OrderDTO
     */
    public OrderDTO findOrderByUserAndAssetIdentifier(Integer userId, String assetIdentifier) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("lines", "line")
                .add(Restrictions.eq("line.deleted", 0))
                .createAlias("line.assets", "asset")
                .add(Restrictions.eq("asset.identifier", assetIdentifier));
        return (OrderDTO) criteria.uniqueResult();
    }
    
    /**
     * Finds all active orders for a given user
     * @param userId
     * @return
     */
    public Object findEarliestActiveOrder(Integer userId) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.asc("nextBillableDay"));

        return findFirst(criteria);
    }

    /**
     * Returns a scrollable result set of orders with a specific status belonging to a user.
     *
     * You MUST close the result set after iterating through the results to close the database
     * connection and discard the cursor!
     *
     * <code>
     *     ScrollableResults orders = new OrderDAS().findByUser_Status(123, 1);
     *     // do something
     *     orders.close();
     * </code>
     *
     * @param userId user ID
     * @param statusId order status to include
     * @return scrollable results for found orders.
     */
    public ScrollableResults findByUser_Status(Integer userId, OrderStatusFlag... status) {
        // I need to access an association, so I can't use the parent helper class
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("baseUserByUserId", "u")
                .createAlias("orderStatus", "s")
                .createAlias("orderPeriod", "p")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.in("s.orderStatusFlag", status))
                .addOrder(Order.desc("p.id"));

        return criteria.scroll();
    }

    // used for the web services call to get the latest X orders
    public List<Integer> findIdsByUserLatestFirst(Integer userId, Integer maxResults) {
        return findIdsByUserLatestFirst(userId, maxResults, 0);
    }

    // used for the web services call to get the latest X orders with offset
    public List<Integer> findIdsByUserLatestFirst(Integer userId, Integer maxResults, Integer offset) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .setProjection(Projections.id())
                .addOrder(Order.desc("createDate"))
                .setMaxResults(maxResults)
                .setFirstResult(offset)
                .setComment("findIdsByUserLatestFirst " + userId + " " + maxResults + " " + offset);
        return criteria.list();
    }

    public List<Integer> findIdsByUserAndDate(Integer userId, Date since, Date until) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.ge("createDate", since))
                .add(Restrictions.lt("createDate", until))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setComment("findIdsByUserAndDate " + userId + " " + since + " " + until);
        return criteria.list();
    }


    // used for the web services call to get the latest X orders that contain an item of a type id
    @SuppressWarnings("unchecked")
    public List<Integer> findIdsByUserAndItemTypeLatestFirst(Integer userId, Integer itemTypeId, int maxResults) {
        // I'm a HQL guy, not Criteria
        String hql =
                "select distinct(orderObj.id)" +
                        " from OrderDTO orderObj" +
                        " inner join orderObj.lines line" +
                        " inner join line.item.itemTypes itemType" +
                        " where itemType.id = :typeId" +
                        "   and orderObj.baseUserByUserId.id = :userId" +
                        "   and orderObj.deleted = 0" +
                        " order by orderObj.id desc";
        List<Integer> data = getSession()
                .createQuery(hql)
                .setParameter("userId", userId)
                .setParameter("typeId", itemTypeId)
                .setMaxResults(maxResults)
                .list();
        return data;
    }

    /**
     * @author othman
     * @return list of active orders
     */
    public List<OrderDTO> findToActivateOrders(Date today) {
        today = Util.truncateDate(today);
        Criteria criteria = getSession().createCriteria(OrderDTO.class);

        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.or(Expression.le("activeSince", today),
                Expression.isNull("activeSince")));
        criteria.add(Restrictions.or(Expression.gt("activeUntil", today),
                Expression.isNull("activeUntil")));

        return criteria.list();
    }

    /**
     * @author othman
     * @return list of inactive orders
     */
    public List<OrderDTO> findToDeActiveOrders(Date today) {
        today = Util.truncateDate(today);
        Criteria criteria = getSession().createCriteria(OrderDTO.class);

        criteria.add(Restrictions.eq("deleted", 0));
        criteria.add(Restrictions.or(Expression.gt("activeSince", today),
                Expression.le("activeUntil", today)));

        return criteria.list();
    }

    public BigDecimal findIsUserSubscribedTo(Integer userId, Integer itemId) {
        String hql =
                "select sum(l.quantity) " +
                        "from OrderDTO o " +
                        "inner join o.lines l " +
                        "where l.item.id = :itemId and " +
                        "o.baseUserByUserId.id = :userId and " +
                        "o.orderPeriod.id != :periodVal and " +
                        "o.orderStatus.orderStatusFlag = :status and " +
                        "o.deleted = 0 and " +
                        "l.deleted = 0";

        BigDecimal result = (BigDecimal) getSession()
                .createQuery(hql)
                .setInteger("userId", userId)
                .setInteger("itemId", itemId)
                .setInteger("periodVal", Constants.ORDER_PERIOD_ONCE)
                .setInteger("status", OrderStatusFlag.INVOICE.ordinal())
                .uniqueResult();

        return (result == null ? BigDecimal.ZERO : result);
    }

    public Integer[] findUserItemsByCategory(Integer userId,
            Integer categoryId) {

        Integer[] result = null;

        final String hql =
                "select distinct(i.id) " +
                        "from OrderDTO o " +
                        "inner join o.lines l " +
                        "inner join l.item i " +
                        "inner join i.itemTypes t " +
                        "where t.id = :catId and " +
                        "o.baseUserByUserId.id = :userId and " +
                        "o.orderPeriod.id != :periodVal and " +
                        "o.deleted = 0 and " +
                        "l.deleted = 0";
        List qRes = getSession()
                .createQuery(hql)
                .setInteger("userId", userId)
                .setInteger("catId", categoryId)
                .setInteger("periodVal", Constants.ORDER_PERIOD_ONCE)
                .list();
        if (qRes != null && qRes.size() > 0) {
            result = (Integer[])qRes.toArray(new Integer[0]);
        }
        return result;
    }

    private static final String FIND_ONETIMERS_BY_DATE_HQL =
            "select o " +
                    "  from OrderDTO o " +
                    " where o.baseUserByUserId.id = :userId " +
                    "   and o.orderPeriod.id = :periodId " +
                    "   and cast(activeSince as date) = :activeSince " +
                    "   and o.orderStatus.orderStatusFlag != :status "+
                    "   and o.isMediated = :isMediated and deleted = 0";

    @SuppressWarnings("unchecked")
    public List<OrderDTO> findOneTimersByDate(Integer userId, Date activeSince, Boolean isMediated) {
        Query query = getSession().createQuery(FIND_ONETIMERS_BY_DATE_HQL)
                .setInteger("userId", userId)
                .setInteger("periodId", Constants.ORDER_PERIOD_ONCE)
                .setDate("activeSince", activeSince)
                .setInteger("status", OrderStatusFlag.FINISHED.ordinal())
                .setBoolean("isMediated", isMediated);

        return query.list();
    }

    /**
     * Find orders by user ID and order notes.
     *
     * @param userId user id
     * @param notes order notes to match
     * @return list of found orders, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findByNotes(Integer userId, String notes) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("notes", notes))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("baseUserByUserId.id", userId));

        return criteria.list();
    }

    /**
     * Find orders by user ID and where notes are like the given string. This method
     * can accept wildcard characters '%' for matching.
     *
     * @param userId user id
     * @param like string to match against order notes
     * @return list of found orders, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findByNotesLike(Integer userId, String like) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.like("notes", like))
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("baseUserByUserId.id", userId));

        return criteria.list();
    }

    /**
     * Find All non deleted orders by user ID.*
     * @param userId user id
     * @return list of found orders, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findAllUserByUserId(Integer userId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("baseUserByUserId.id", userId));

        return criteria.list();
    }

    /**
     * Find All non deleted active orders by user ID.*
     * @param userId user id
     * @return list of found orders, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO> findAllActiveSubscriptions(Integer userId, Integer itemId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("lines","lines")
                .createAlias("lines.item", "item")
                .add(Restrictions.eq("item.id", itemId))
                .setComment("findAllActiveOrdersByUserId " + userId);
        return criteria.list();
    }

    /**
     * Returns the latest active order for the given user id
     * that was created as an Invoice Overdue Penalty.
     *
     * @param userId user id
     * @param invoiceDto invoice
     * @return penalty order for invoice invoice
     */
    @SuppressWarnings("unchecked")
    public List<OrderDTO>  findPenaltyOrderForInvoice(InvoiceDTO invoice) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", invoice.getUserId()))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                //.add(Restrictions.eq("activeSince", invoice.getDueDate()))
                .createAlias("lines", "l")
                .add(Restrictions.eq("l.orderLineType.id", Constants.ORDER_LINE_TYPE_PENALTY))
                .add(Restrictions.ilike("l.description", "Overdue Penalty for Invoice Number ", MatchMode.ANYWHERE))
                .addOrder(Order.desc("id"));
        //.setMaxResults(1);

        return criteria.list();
    }

    private static final String CURRENCY_USAGE_FOR_ENTITY_SQL =
            " select count(*) " +
                    " from OrderDTO dto " +
                    " where dto.orderStatus.orderStatusFlag = :status " +
                    " and dto.currency.id = :currencyId " +
                    " and dto.baseUserByUserId.company.id = :entityId ";

    public Long findOrderCountByCurrencyAndEntity(Integer currencyId, Integer entityId ) {
        Query query = getSession().createQuery(CURRENCY_USAGE_FOR_ENTITY_SQL)
                .setParameter("status", OrderStatusFlag.INVOICE)
                .setParameter("currencyId", currencyId)
                .setParameter("entityId", entityId);
        return (Long) query.uniqueResult();
    }

    private static final String USAGE_FOR_ENTITY_SQL =
            " select count(*) " +
                    " from OrderDTO dto " +
                    " where dto.baseUserByUserId.company.id = :entityId ";

    public Long findOrderCountByEntity(Integer entityId ) {
        Query query = getSession().createQuery(USAGE_FOR_ENTITY_SQL)
                .setParameter("entityId", entityId);
        return (Long) query.uniqueResult();
    }

    public List<OrderDTO> findOrdersByUserAndResellerOrder(Integer userId, Integer resellerOrder) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("resellerOrder", resellerOrder))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    private static final String COUNT_ORDER_BY_ITEM =
            " select count(*) " +
                    " from OrderDTO dto " +
                    " join dto.lines as l where l.item.id = :itemId " +
                    " and dto.baseUserByUserId.company.parent != null" +
                    " and dto.deleted = 0";

    public Long findOrdersOfChildsByItem(Integer itemId) {
        Query query = getSession().createQuery(COUNT_ORDER_BY_ITEM)
                .setParameter("itemId", itemId);
        return (Long) query.uniqueResult();
    }

    /**
     * Detach all orders in hierarchy from hibernate context.
     * Touch all fields for orders and lines before actual evict persisted entity from hibernate context
     * @param persistedOrder target order for detach
     */
    public void detachOrdersHierarchy(OrderDTO persistedOrder) {
        Set<OrderDTO> processed = new HashSet<>();
        persistedOrder.touch();
        detachOrdersHierarchy(persistedOrder, processed);
    }

    private void detachOrdersHierarchy(OrderDTO order, Set<OrderDTO> processedOrders) {
        if (processedOrders.contains(order)) {
            return;
        }
        detach(order);
        processedOrders.add(order);
        if (order.getParentOrder() != null) {
            detachOrdersHierarchy(order.getParentOrder(), processedOrders);
        }
        for (OrderDTO child : order.getChildOrders()) {
            detachOrdersHierarchy(child, processedOrders);
        }
    }

    public boolean orderHasStatus(Integer statusId,Integer entity)
    {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("orderStatus", "o")
                .add(Restrictions.eq("o.id", statusId))
                .add(Restrictions.eq("o.entity.id", entity));
        List<OrderDTO> orderDTOs;
        orderDTOs = criteria.list();
        if(!(orderDTOs.isEmpty())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Verfies if sub account of a user has active orders containing a specific product
     *
     * @param userId
     * @param itemId
     * @param activeSince
     * @param activeUntil
     * @return
     */
    public boolean isSubscribed(Integer userId, Integer itemId, Date activeSince, Date activeUntil) {
        DetachedCriteria dc = DetachedCriteria.forClass(CustomerDTO.class).
                createAlias("parent", "parent").
                createAlias("parent.baseUser", "parentUser").
                add(Restrictions.eq("parentUser.id", userId)).
                createAlias("baseUser", "baseUser").
                setProjection(Projections.property("baseUser.id"));

        Disjunction dis1 = Restrictions.disjunction();
        if(activeUntil != null) {
            dis1.add(Restrictions.le("activeSince", activeUntil));
        }

        Disjunction dis2 = Restrictions.disjunction();
        dis2.add(Restrictions.isNull("activeUntil"));
        dis2.add(Restrictions.ge("activeUntil", activeSince));

        Criteria c = getSession().createCriteria(OrderDTO.class).
                add(Restrictions.eq("deleted", 0)).
                createAlias("baseUserByUserId","user").
                add(Property.forName("user.id").in(dc)).
                add(Restrictions.conjunction().
                        add(dis1).
                        add(dis2)).
                        createAlias("lines","lines").
                        createAlias("lines.item", "item").
                        add(Restrictions.eq("item.id", itemId)).
                        setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

        return c.list().size() > 0;
    }

    /**
     * Retrieve orders sorted by sortAttribute attribute and ordered sepcified in order attribute.
     * @param userId
     * @param maxResults
     * @param offset
     * @param sortAttribute
     * @param order
     * @return
     */
    public List<Integer> findOrdersByUserPagedSortedByAttribute(Integer userId, int maxResults, int offset, String sortAttribute, ListField.Order order) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .setProjection(Projections.id())
                .setMaxResults(maxResults)
                .setFirstResult(offset)
                .setComment("findOrdersByUserPagedSortedByAttribute " + userId + " " + maxResults + " " + offset);
        if(ListField.Order.ASC.equals(order)) {
            criteria.addOrder(Order.asc(sortAttribute));
        }
        else {
            criteria.addOrder(Order.desc(sortAttribute));
        }
        return criteria.list();
    }

    /**
     *
     * @param userId
     * @param since
     * @param until
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<Integer> getCustomersAllOneTimeUsageOrdersInCurrentBillingCycle(Integer userId, Date billingCycleStart,
            Date billingCycleEnd, OrderStatusFlag... flags) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("isMediated", Boolean.TRUE))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.ge("activeSince", billingCycleStart))
                .add(Restrictions.lt("activeSince", billingCycleEnd))
                .add(Restrictions.eq("prorateAdjustmentFlag", Boolean.FALSE))
                .add(Restrictions.isNull("parentOrder"))
                .createAlias("orderStatus", "s")
                .add(Restrictions.in("s.orderStatusFlag", flags))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.eq("p.id", Constants.ORDER_PERIOD_ONCE))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setComment("getCustomersAllOneTimeUsageOrdersInCurrentBillingCycle " + userId + " " + billingCycleStart + " " + billingCycleEnd);
        return criteria.list();
    }

    public OrderDTO findOrderByMetaFieldValue(Integer entityId, String name, String value){
        List<OrderDTO> orderList = findOrderByMetaFieldsValue(entityId, name, value);
        return (orderList.isEmpty()) ? null : orderList.get(0);
    }

    public List<OrderDTO> findOrderByMetaFieldsValue(Integer entityId, String name, String value){
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .createAlias("baseUserByUserId", "user")
                .createAlias("user.company", "company")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("metaField.name", name))
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.sqlRestriction("string_value =  ?", value, StringType.INSTANCE));

        return criteria.list();
    }

    public List<OrderDTO> findDiscountOrderByMetaFieldsValue(Integer entityId, String name, Integer value){
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("metaFields", "metaFieldValue")
                .createAlias("metaFieldValue.field", "metaField")
                .createAlias("baseUserByUserId", "user")
                .createAlias("user.company", "company")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.eq("metaField.name", name))
                .add(Restrictions.eq("company.id", entityId))
                .add(Restrictions.sqlRestriction("integer_value =  ?", value, IntegerType.INSTANCE));

        return criteria.list();
    }

    public ScrollableResults findByUsersAndStatus(Integer[] userIds,int status, int orderPeriodId, Date createDate) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.in("u.id", userIds))
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.id", status))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.eq("p.id", orderPeriodId))
                .add(Restrictions.le("createDate", createDate))
                .addOrder(Order.desc("p.id"));

        return criteria.scroll();
    }

    /**
     * Retrieves recurring orders of the user
     *
     * @param userId
     * @return
     */
    public List<Integer> getCustomerRecurringOrders(Integer userId, OrderStatusFlag... statusFlags) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .createAlias("orderStatus", "s");

        if (ArrayUtils.isNotEmpty(statusFlags)) {
            criteria.add(Restrictions.in("s.orderStatusFlag", statusFlags));
        }

        criteria.add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
        .setProjection(Projections.id())
        .addOrder(Order.desc("id"))
        .setComment("getCustomerRecurringOrders " + userId);
        return criteria.list();
    }

    /**
     * Retrieves usage orders in current billing period of the user
     *
     * @param userId
     * @param billingCycleStart
     * @param billingCycleEnd
     * @return
     */
    public List<Integer> getCustomersAllUsageOrdersInCurrentBillingCycle(Integer userId, Date billingCycleStart, Date billingCycleEnd) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.ge("activeSince", billingCycleStart))
                .add(Restrictions.lt("activeSince", billingCycleEnd))
                .add(Restrictions.isNull("parentOrder"))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.eq("p.id", Constants.ORDER_PERIOD_ONCE))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"))
                .setComment("getCustomersAllUsageOrdersInCurrentBillingCycle " + userId + " " + billingCycleStart + " " + billingCycleEnd);
        return criteria.list();
    }

    private static final String  findSubscriptionOrderActiveSinceDate = " select active_since from purchase_order where id in (select order_id from order_line where deleted = 0 and item_id in " +
            " (select item_id from plan where id in (select plan_id from plan_item where item_id = :itemId))) and user_id = :userId and deleted=0 and status_id in " +
            " (select id from order_status where order_status_flag = 0 ) " +
            " order by active_since limit 1";

    public Date getSubscriptionOrderActiveSinceDateByUsageItem(Integer userId, Integer itemId) {
        SQLQuery query = getSession().createSQLQuery(findSubscriptionOrderActiveSinceDate);
        query.setParameter("itemId", itemId);
        query.setParameter("userId", userId);
        @SuppressWarnings("unchecked")
        List<Date> result = query.list();
        return (result!=null && !result.isEmpty()) ? result.get(0): null;
    }

    private static final String  findSubscriptionOrderActiveSinceDateByAsset = "SELECT active_since FROM purchase_order WHERE id IN ( SELECT order_id FROM order_line WHERE id IN "
            + "(SELECT order_line_id FROM asset_assignment WHERE asset_id IN "
            + " (SELECT id FROM asset WHERE identifier = :assetIdentifier) AND "
            + "     CASE WHEN end_datetime IS NOT NULL "
            + "     THEN (start_datetime <= :eventDate AND end_datetime >= :eventDate) "
            + "     ELSE (start_datetime <= :eventDate) END)) "
            + " AND user_id = :userId";

    public Date getSubscriptionOrderActiveSinceDate(Integer userId, Date eventDate, String assetIdentifier) {
        SQLQuery query = getSession().createSQLQuery(findSubscriptionOrderActiveSinceDateByAsset);
        query.setParameter("eventDate", eventDate);
        query.setParameter("userId", userId);
        query.setParameter("assetIdentifier", assetIdentifier);
        @SuppressWarnings("unchecked")
        List<Date> result = query.list();
        return (result!=null && !result.isEmpty()) ? result.get(0): null;
    }
    /* NGES : This method return latest order(sorted by active until date) for a customer for the given item
     * @params userId customerId
     * @parasn itemId product id
     *
     * return order
     * */
    public OrderDTO findLastOrder(Integer userId, Integer itemId){
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .createAlias("orderPeriod", "p")
                .add(Restrictions.eq("p.id", Constants.ORDER_PERIOD_ONCE))
                .add(Restrictions.eq("u.id", userId))
                .createAlias("lines", "orderLine")
                .createAlias("orderLine.item", "item")
                .add(Restrictions.eq("item.id", itemId))
                .add(Restrictions.isNotNull("activeUntil"))
                .addOrder(Order.desc("activeUntil"))
                .setMaxResults(1);

        return findFirst(criteria);
    }

    @SuppressWarnings("unchecked")
    public List<OrderDTO> getOrdersByRenewAndActiveUntil(Date activeUntil) {
        return getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.ge("activeUntil", activeUntil))
                .add(Restrictions.lt("activeUntil", new Date(activeUntil.getTime() + TimeUnit.DAYS.toMillis(1))))
                .add(Restrictions.eq("autoRenew", true))
                .add(Restrictions.isNull("renewOrderId"))
                .addOrder(Order.desc("id"))
                .list();
    }

    @SuppressWarnings("unchecked")
    public List<OrderDTO> getOrdersByRenewNotification() {
        return getSession().createCriteria(OrderDTO.class)
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("deleted", 0))
                .add(Restrictions.isNotNull("renewNotification"))
                .add(Restrictions.isNotNull("activeUntil"))
                .add(Restrictions.eq("autoRenew", true))
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .addOrder(Order.desc("id"))
                .list();
    }

    public List<OrderDTO> getOrdersWithActiveUntil(Integer userId, Date activeUntil){
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.isNotNull("activeUntil"))
                .add(Restrictions.eq("activeUntil", activeUntil))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    public List<OrderDTO> getOrdersWithActiveSince(Integer userId, Date activeSince){
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .add(Restrictions.isNotNull("activeSince"))
                .add(Restrictions.eq("activeSince", activeSince))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    public List<OrderDTO> getEnrollmentOrdersByDate(Integer entityId, List<Integer> childEntities, Date startDate, Date endDate, OrderDate orderDate) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("baseUserByUserId", "user")
                .createAlias("user.company", "company")
                .add(Restrictions.or(Restrictions.eq("company.id", entityId),
                        Restrictions.in("company.id", childEntities)))
                        .add(Restrictions.eq("deleted", 0))
                        .add(Restrictions.ge(orderDate.getName(), startDate))
                        .add(Restrictions.le(orderDate.getName(), endDate));
        return criteria.list();
    }

    public List<OrderDTO> getEnrollmentOrdersActiveBetweenDates(Integer entityId, List<Integer> childEntities, Date startDate, Date endDate, OrderDate orderStartDate) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .createAlias("baseUserByUserId", "user")
                .createAlias("user.company", "company")
                .add(Restrictions.or(Restrictions.eq("company.id", entityId),
                        Restrictions.in("company.id", childEntities)))
                        .add(Restrictions.eq("deleted", 0))
                        .add(Restrictions.not(Restrictions.ge(orderStartDate.getName(), Util.truncateDate(endDate))))
                        .add(Restrictions.and(Restrictions.or(Restrictions.isNull("activeUntil"),
                                Restrictions.not(Restrictions.le("activeUntil", Util.truncateDate(startDate)))),
                                Restrictions.or(Restrictions.isNull("finishedDate"),
                                        Restrictions.not(Restrictions.le("finishedDate", Util.truncateDate(startDate))))));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<OrderDTO> findOrdersByUserAndPeriod(Integer userId, Integer periodId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .createAlias("orderStatus", "s")
                .add(Restrictions.eq("s.orderStatusFlag", OrderStatusFlag.INVOICE))
                .add(Restrictions.eq("p.id", periodId))
                .setComment("findOrdersByUserAndPeriod " + userId);
        return criteria.list();
    }

    public List<OrderDTO> getOrdersByActiveSinceProductAndStatus(Integer page, Integer size, Date activeSince, Integer productId, Integer orderStatusId, Integer entityId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.company.id", entityId));

        if (null != activeSince) {
            criteria.add(Restrictions.eq("activeSince", activeSince));
        }

        if (null != productId) {
            criteria.createAlias("lines", "orderLine")
            .createAlias("orderLine.item", "item")
            .add(Restrictions.eq("item.id", productId));
        }

        if (null != orderStatusId) {
            criteria.createAlias("orderStatus", "s")
            .add(Restrictions.eq("s.id", orderStatusId));
        }

        if (null != page && 0 < page && null != size && 0 < size && 50 >= size) {
            criteria.setFirstResult((page - 1) * size);
            criteria.setMaxResults(size);
        }

        criteria.addOrder(Order.desc("id"));
        return criteria.list();
    }

    private static final String COUNT_OF_ORDER_BY_ITEM = "SELECT COUNT(*) FROM purchase_order WHERE id IN "
            + "(SELECT order_id FROM order_line WHERE item_id = :itemId AND deleted = 0 ) AND "
            + "status_id IN (SELECT id FROM order_status WHERE order_status_flag = 0 "
            + "AND entity_id = :entityId) AND user_id = :userId AND deleted =0";

    public Integer getCountOfOrderByItem(Integer entityId, Integer userId, Integer itemId) {
        SQLQuery query = getSession().createSQLQuery(COUNT_OF_ORDER_BY_ITEM);
        query.setParameter("entityId", entityId);
        query.setParameter("itemId", itemId);
        query.setParameter("userId", userId);
        return BigInteger.class.cast(query.uniqueResult()).intValue();
    }

    private static final String COUNT_OF_ORDER_LINE_BY_PLAN_ITEM =
            "SELECT COUNT(*) "
                    + "FROM order_line ol "
                    + "INNER JOIN purchase_order po on po.id = ol.order_id "
                    + "WHERE ol.item_id = :itemId "
                    + "AND ol.deleted = 0 "
                    + "AND po.status_id IN "
                    + "(SELECT os.id "
                    + "FROM order_status os "
                    + "WHERE os.order_status_flag = 0 "
                    + "AND os.entity_id = :entityId) "
                    + "AND po.user_id = :userId "
                    + "AND po.id != :orderId "
                    + "AND po.deleted = 0 ";

    public Integer getCountOfOrderLineByPlanItem(Integer entityId, Integer userId, Integer itemId,Integer orderId) {
        SQLQuery query = getSession().createSQLQuery(COUNT_OF_ORDER_LINE_BY_PLAN_ITEM);
        query.setParameter("entityId", entityId);
        query.setParameter("itemId", itemId);
        query.setParameter("userId", userId);
        query.setParameter("orderId", orderId);
        return BigInteger.class.cast(query.uniqueResult()).intValue();
    }

    /**
     * Retrieves id of all one time active orders.
     *
     * @param userId
     * @return
     */
    public List<Integer> getUserOneTimeActiveOrderIdsForUser(Integer userId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.eq("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("orderStatus", "s")
                .add(Restrictions.ne("s.orderStatusFlag", OrderStatusFlag.FINISHED))
                .setProjection(Projections.id())
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    public enum OrderDate {
        ACTIVE_SINCE("activeSince"), CREATION_DATE("createDate");

        OrderDate(String name) {
            this.name = name;
        }

        private String name;

        public String getName() {
            return name;
        }
    }

    public List<OrderDTO> findRecurringOrdersByUserId(Integer userId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .createAlias("orderStatus", "s")
                .add(Restrictions.ne("s.orderStatusFlag", OrderStatusFlag.FINISHED))
                .addOrder(Order.desc("activeUntil"));
        return criteria.list();
    }

    /**
     * Retrieves all active orders of the user.
     *
     * @param userId
     * @return
     */
    public List<OrderDTO> getAllActiveOrdersByUserId(Integer userId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderStatus", "s")
                .add(Restrictions.ne("s.orderStatusFlag", OrderStatusFlag.FINISHED))
                .addOrder(Order.desc("id"));
        return criteria.list();
    }

    /**
     * Retrieve all orders of the user.
     * @param userId:
     * @return
     */
    public List<OrderDTO> findUsersAllSubscriptions(Integer userId) {
        Criteria criteria = getSession().createCriteria(OrderDTO.class)
                .add(Restrictions.eq("deleted", 0))
                .createAlias("baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("orderPeriod", "p")
                .add(Restrictions.ne("p.id", Constants.ORDER_PERIOD_ONCE))
                .addOrder(Order.desc("id"));

        return criteria.list();
    }
}
