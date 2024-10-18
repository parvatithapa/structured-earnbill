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

package com.sapienter.jbilling.server.usagePool.db;

import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * CustomerUsagePoolDAS
 * This DAS has various finder methods to fetch customer usage pools based on various criteria.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class CustomerUsagePoolDAS extends AbstractDAS<CustomerUsagePoolDTO>{

    /**
     * A finder method that returns a list of Customer Usage Pools
     * based on customer id provided to it as parameter.
     * @param customerId
     * @return List<CustomerUsagePoolDTO>
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> findAllCustomerUsagePoolsByCustomerId(Integer customerId) {
        Query query = getSession().getNamedQuery("CustomerUsagePoolDTO.findAllCustomerUsagePoolsByCustomerId");
        query.setParameter("customer_id", customerId);

        return query.list();
    }

    /**
     * A query to find customer usage pool based on given customer id and usage pool id.
     */
    private static final String findCustomerUsagePoolByusagePoolIdAndCustomerIdNameSQL =
            " FROM CustomerUsagePoolDTO a " +
                    " WHERE a.usagePool.id = :usagePoolId "+
                    " AND a.customer.id = :customerId";

    /**
     * This method fetches the specific Customer Usage Pool given the customer id and usage pool id.
     * @param usagePoolId
     * @param customerId
     * @return CustomerUsagePoolDTO
     */
    public CustomerUsagePoolDTO getCustomerUsagePoolByPoolIdAndCustomerId(Integer usagePoolId, Integer customerId) {
        Query query = getSession().createQuery(findCustomerUsagePoolByusagePoolIdAndCustomerIdNameSQL);
        query.setParameter("usagePoolId", usagePoolId);
        query.setParameter("customerId", customerId);
        return  null != query.list() ? (CustomerUsagePoolDTO)query.list().get(0) : null;
    }

    /**
     * A finder method to fetch customer usage pool by its id.
     * @param customerUsagePoolId
     * @return CustomerUsagePoolDTO
     */
    public CustomerUsagePoolDTO findCustomerUsagePoolsById(Integer customerUsagePoolId) {
        Criteria criteria = getSession().createCriteria(CustomerUsagePoolDTO.class);
        criteria.add(Restrictions.eq("id", customerUsagePoolId));
        criteria.add(Restrictions.gt("cycleEndDate", Util.getEpochDate()));
        return (CustomerUsagePoolDTO) criteria.uniqueResult();
    }

    /**
     * This method returns all customer usage pool ids for records
     * that are eligible for evaluation and update of cycle end date and quantity.
     * It simply picks up all records that have cycle end date less that equal to current date/time.
     * @return List<Integer> customer usage pool ids
     */
    @SuppressWarnings("unchecked")
    public List<Integer> findCustomerUsagePoolsForEvaluation(Integer entityId, Date runDate, Integer maxResults, Integer offset) {
        Criteria criteria = getSession().createCriteria(getPersistentClass(), "customerUsagePool")
                .createAlias("customer", "customer", JoinType.INNER_JOIN)
                .createAlias("order", "order", JoinType.INNER_JOIN)
                .createAlias("customer.baseUser", "user", JoinType.INNER_JOIN)
                .createAlias("user.company", "entity", JoinType.INNER_JOIN)
                .createAlias("user.userStatus", "us", JoinType.INNER_JOIN)
                .createAlias("us.ageingEntityStep", "agStep", JoinType.LEFT_OUTER_JOIN)
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("user.deleted", 0))
                .add(Restrictions.le("customerUsagePool.cycleEndDate", runDate))
                .add(Restrictions.gt("customerUsagePool.cycleEndDate", Util.getEpochDate()))
                .add(Restrictions.or(
                		Restrictions.isNull("order.activeUntil"), 
                		Restrictions.gt("order.activeUntil", runDate)
                		))
                .add(Restrictions.or(
                        Restrictions.eq("us.id", UserDTOEx.STATUS_ACTIVE),
                        Restrictions.eq("agStep.suspend", 0)
                        ))
                        .setProjection(Projections.id());
        if (null != maxResults) {
            criteria.setMaxResults(maxResults);
            criteria.setFetchSize(maxResults);
        }
        if (null != offset) {
            criteria.setFirstResult(offset);
        }
        criteria.addOrder(Order.asc("id"));
        return criteria.list();
    }

    /**
     * A finder method that fetches list of customer usage pools given the customer id.
     * @param customerId
     * @return List<CustomerUsagePoolDTO>
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> findCustomerUsagePoolByCustomerId(Integer customerId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("customer.id", customerId));

        return criteria.list();
    }

    /**
     * A method that fetches list of customer usage pools given the customer id.
     * @param customerId
     * @return List<CustomerUsagePoolDTO>
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByCustomerId(Integer customerId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass());
        criteria.add(Restrictions.eq("customer.id", customerId));
        criteria.add(Restrictions.gt("cycleEndDate", Util.getEpochDate()));
        criteria.addOrder(Order.asc("createDate"));
        return criteria.list();
    }

    /**
     * Finds customer usage pool for given customerId and orderId.
     * @param customerId
     * @param orderId
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByCustomerIdAndOrderId(Integer customerId, Integer orderId) {
        return getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("customer.id", customerId))
                .add(Restrictions.eq("order.id", orderId))
                .add(Restrictions.gt("cycleEndDate", Util.getEpochDate()))
                .addOrder(Order.asc("createDate"))
                .list();
    }

    /**
     * A method that fetches list of customer usage pools given the plan id that created them.
     * @param planId
     * @return List<CustomerUsagePoolDTO>
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByPlanId(Integer planId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("plan.id", planId));

        return criteria.list();
    }

    /**
     * A finder method that returns a list of Customer Usage Pools
     * based on orderId provided to it as parameter.
     * @param orderId
     * @return List<CustomerUsagePoolDTO>
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> findAllCustomerUsagePoolsByOrderId(Integer orderId) {
        Query query = getSession().getNamedQuery("CustomerUsagePoolDTO.findAllCustomerUsagePoolsByOrderId");
        query.setParameter("order_id", orderId);

        return query.list();
    }

    /**
     * A method that fetches list of customer usage pools given the order id.
     * @param orderId
     * @return List<CustomerUsagePoolDTO>
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByOrderId(Integer orderId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass());
        criteria.add(Restrictions.eq("order.id", orderId));
        criteria.add(Restrictions.gt("cycleEndDate", Util.getEpochDate()));

        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public Long countCustomerUsagePoolsByUsagePoolId(Integer usagePoolId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("usagePool", "usagePool")
                .add(Restrictions.eq("usagePool.id", usagePoolId))
                .setProjection(Projections.rowCount());

        return (Long) criteria.uniqueResult();
    }


    /**
     * returns {@link CustomerUsagePoolDTO} for given customerId and date range
     * @param customerId
     * @param startDate
     * @param endDate
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByCustomerSubscriptionAndDateRange(Integer customerId,
            Integer subscriptionOrderId, Date startDate, Date endDate) {
        Criteria criteria = getSession().createCriteria(getPersistentClass());
        criteria.add(Restrictions.eq("customer.id", customerId));
        criteria.add(Restrictions.eq("order.id", subscriptionOrderId));
        criteria.add(Restrictions.between("cycleEndDate", startDate, endDate));
        criteria.add(Restrictions.gt("cycleEndDate", Util.getEpochDate()));
        criteria.addOrder(Order.asc("createDate"));
        return criteria.list();
    }

    /**
     * Find the list of customer usage pools based on user id and asset identifier
     * @param userId
     * @param assetIdentifier
     * @return
     */
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByUserAndAssetIdentifier(Integer userId, String assetIdentifier) {
        Criteria criteria = getSession().createCriteria(CustomerUsagePoolDTO.class)
                .createAlias("order", "order")
                .add(Restrictions.eq("order.deleted", 0))
                .createAlias("order.baseUserByUserId", "u")
                .add(Restrictions.eq("u.id", userId))
                .createAlias("order.lines", "line")
                .add(Restrictions.eq("line.deleted", 0))
                .createAlias("line.assets", "asset")
                .add(Restrictions.eq("asset.identifier", assetIdentifier));
        return criteria.list();
    }

    @SuppressWarnings("unchecked")
    public List<CustomerUsagePoolDTO> getCustomerUsagePoolsByOrderAndPlanId(Integer orderId, Integer planId) {
        return getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("plan.id", planId))
                .add(Restrictions.eq("order.id", orderId))
                .add(Restrictions.gt("cycleEndDate", Util.getEpochDate()))
                .list();
    }

    @SuppressWarnings("unchecked")
    public boolean exist(CustomerUsagePoolDTO customerUsagePool) {
        Criteria criteria = getSession().createCriteria(getPersistentClass());
        criteria.add(Restrictions.ne("id", customerUsagePool.getId()));
        criteria.add(Restrictions.eq("customer.id", customerUsagePool.getCustomer().getId()));
        criteria.add(Restrictions.eq("usagePool.id", customerUsagePool.getUsagePool().getId()));
        criteria.add(Restrictions.eq("plan.id", customerUsagePool.getPlan().getId()));
        criteria.add(Restrictions.eq("order.id", customerUsagePool.getOrder().getId()));
        criteria.add(Restrictions.eq("cycleStartDate", customerUsagePool.getCycleStartDate()));
        criteria.add(Restrictions.eq("cycleEndDate", customerUsagePool.getCycleEndDate()));
        criteria.add(Restrictions.gt("cycleEndDate", Util.getEpochDate()));
        return criteria.uniqueResult() != null;
    }

}
