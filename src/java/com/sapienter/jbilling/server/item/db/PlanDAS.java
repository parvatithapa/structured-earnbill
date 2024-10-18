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

package com.sapienter.jbilling.server.item.db;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.strategy.CompanyPooledPricingStrategy;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Brian Cowdery
 * @since 30-08-2010
 */
public class PlanDAS extends AbstractDAS<PlanDTO> {

    private static final String getPoolContributingPlans =
            "select " +
                    "	{p.*} " +
                    "from " +
                    "	plan p " +
                    "	join plan_item pi on pi.plan_id = p.id " +
                    "	join plan_item_price_timeline piptl on pi.id = piptl.plan_item_id " +
                    "	join price_model pm on pm.id = piptl.price_model_id " +
                    "	join price_model_attribute pma on pma.price_model_id = pm.id " +
                    "where " +
                    "	pma.attribute_name = :attr_name " +
                    "	and pma.attribute_value = :attr_value " +
                    "	and pm.strategy_type = :price_strategy ";

    /**
     * Fetch a list of all customers that have subscribed to the given plan
     * by adding the "plan subscription" item to a recurring order.
     *
     * @param planId id of plan
     * @return list of customers subscribed to the plan, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<CustomerDTO> findCustomersByPlan(Integer planId) {
        Query query = getSession().getNamedQuery("CustomerDTO.findCustomersByPlan");
        query.setParameter("plan_id", planId);

        return query.list();
    }

    /**
     * Returns true if the customer is subscribed to to the given plan id.
     *
     * @param userId user id of the customer
     * @param planId plan id
     * @return true if customer is subscribed to the plan, false if not.
     */
    public boolean isSubscribed(Integer userId, Integer planId, Date pricingDate) {
        Query query = getSession().getNamedQuery("PlanDTO.isSubscribed");
        query.setParameter("user_id", userId);
        query.setParameter("plan_id", planId);
        query.setParameter("pricingDate", pricingDate);
        return !query.list().isEmpty();
    }

    /**
     * Returns true if the customer is subscribed to plan founded by the given item id.
     *
     * @param userId user id of the customer
     * @param itemId plan id
     * @return true if customer is subscribed to the plan, false if not.
     */
    public boolean isSubscribedByItem(Integer userId, Integer itemId) {
        String queryIsSubscriberByItem = "" +
                "SELECT * FROM order_line ol, plan p, base_user u, purchase_order po, order_status os " +
                "WHERE p.item_id = ol.item_id and po.id = ol.order_id and u.id = po.user_id and po.status_id = os.id " +
                "and p.id = (SELECT id FROM plan WHERE item_id = :item_id) " +
                "and u.id = :user_id " +
                "and ol.deleted = 0 " +
                "and po.deleted = 0 " +
                "and po.period_id <> 1 " +
                "and os.order_status_flag <> 1";
        Query query = getSession().createSQLQuery(queryIsSubscriberByItem).setParameter("user_id", userId).setParameter("item_id", itemId);
        List result = query.list();
        return result != null && result.size() > 0;
    }

    /**
     * Returns true if the customer is subscribed to to the given plan id.
     *
     * @param userId user id of the customer
     * @param planId plan id
     * @return true if customer is subscribed to the plan, false if not.
     */
    public boolean isSubscribedFinished(Integer userId, Integer planId) {
        Query query = getSession().getNamedQuery("PlanDTO.isSubscribedFinished");
        query.setParameter("user_id", userId);
        query.setParameter("plan_id", planId);

        return !query.list().isEmpty();

    }

    /**
     * Fetch all plans for the given plan subscription item id.
     *
     * @param planItemId plan subscription item id
     * @return list of plans, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<PlanDTO> findByPlanSubscriptionItem(Integer planItemId) {
        Query query = getSession().getNamedQuery("PlanDTO.findByPlanItem");
        query.setParameter("plan_item_id", planItemId);

        return query.list();
    }

    /**
     * Fetch all plans that affect the pricing of the given item id, or include
     * the item in a bundle.
     *
     * @param affectedItemId affected item id
     * @return list of plans, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<PlanDTO> findByAffectedItem(Integer affectedItemId) {
        Query query = getSession().getNamedQuery("PlanDTO.findByAffectedItem");
        query.setParameter("affected_item_id", affectedItemId);

        return query.list();
    }

    public List<PlanDTO> getPoolContributingPlans(Integer poolItemCategory) {
        SQLQuery query = getSession().createSQLQuery(getPoolContributingPlans);
        query.setParameter("attr_name", CompanyPooledPricingStrategy.POOL_CATEGORY_ATTR_NAME);
        query.setParameter("attr_value", poolItemCategory.toString());
        query.setParameter("price_strategy", PriceModelStrategy.COMPANY_POOLED.name());
        query.setComment("PlanDAS.getPoolContributingPlans.");
        query.addEntity("p", PlanDTO.class);
        return query.list();
    }

    /**
     * Fetch all plans for the given entity (company) id.
     *
     * @param entityId entity id
     * @return list of plans, empty if none found
     */
    @SuppressWarnings("unchecked")
    public List<PlanDTO> findAll(Integer entityId) {

        boolean isRoot = new CompanyDAS().isRoot(entityId);

        DetachedCriteria criteria = DetachedCriteria.forClass(PlanDTO.class);
        criteria.createAlias("item", "it");

        if(isRoot){
            criteria.createAlias("it.entities", "en", Criteria.LEFT_JOIN);
        }else{
            criteria.createAlias("it.entities", "en");
            criteria.add(Restrictions.eq("en.id", entityId));
        }

        return (List<PlanDTO>)getHibernateTemplate().findByCriteria(criteria);
    }

    public List<PlanDTO> findAllActive(Integer entityId) {
        CompanyDAS companyDAS = new CompanyDAS();
        CompanyDTO companyDTO = companyDAS.find(entityId);

        Query query = getSession().getNamedQuery("PlanDTO.findAllActiveByEntity");
        query.setParameterList("entityIds", companyDAS.findAllCurrentAndChildEntities(entityId));
        query.setParameter("entityId", companyDTO.getParent() == null ? entityId : companyDTO.getParent().getId());

        return query.list();
    }


    public List<PlanDTO> findAllActiveAvailable(Integer entityId) {
        CompanyDAS companyDAS = new CompanyDAS();
        CompanyDTO companyDTO = companyDAS.find(entityId);
        Date today = new Date();
        Query query = getSession().getNamedQuery("PlanDTO.findAllActiveAvailable");
        query.setParameterList("entityIds", companyDAS.findAllCurrentAndChildEntities(entityId));
        query.setParameter("entityId", companyDTO.getParent() == null ? entityId : companyDTO.getParent().getId());
        query.setParameter("date", today);
        return query.list();
    }

    /**
	* Fetch all plans for the given item id.
	*
	* @param itemId
	* item id
	* @return list of plans, empty if none found
	*/
	@SuppressWarnings("unchecked")
	public List<PlanDTO> findByItemId(Integer itemId) {
		//TODO (VCA): we temporarily say that the flush mode is COMMIT and
		//not AUTO to avoid flushing the session for a simple query
		//this is sometimes a problem during creation of orders with assets
		Session session = getSession();
		FlushMode prevFlushMode = session.getFlushMode();
		session.setFlushMode(FlushMode.COMMIT);
		Query query = session.getNamedQuery("PlanDTO.findByItemId");
		query.setParameter("item_id", itemId);

		List<PlanDTO> plans = query.list();
		session.setFlushMode(prevFlushMode);
		return plans;
	}

    public PlanDTO findPlanByItemId(Integer itemID) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("item", "item")
                .add(Restrictions.eq("item.id", itemID));

        return (criteria.uniqueResult() == null ? null : (PlanDTO)criteria.uniqueResult() );
    }

    public Long countPlansByUsagePoolId(Integer usagePoolId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                                        .createAlias("usagePools", "usagePool")
                                        .add(Restrictions.eq("usagePool.id", usagePoolId))
                                        .setProjection(Projections.rowCount());

        return (Long) criteria.uniqueResult();
    }

    public PlanDTO findPlanById(Integer planId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .add(Restrictions.eq("id", planId));

        return (criteria.uniqueResult() == null ? null : (PlanDTO)criteria.uniqueResult() );
    }


    public String findInternalNumberByPlan(Integer planId) {
        Criteria criteria = getSession().createCriteria(getPersistentClass())
                .createAlias("item", "item")
                .add(Restrictions.eq("id", planId))
                .setProjection(Projections.distinct(Projections.property("item.internalNumber")));

        return (criteria.uniqueResult() == null ? null : (String) criteria.uniqueResult());
    }

    /*Method return non global plan which belongs to non global category and don't have non global plan item*/
    public List<PlanDTO> findNonGlobalPlan(Integer entityId) {
        Criteria criteria = getSession().createCriteria(PlanDTO.class)
                .createAlias("item", "item")
                .createAlias("item.entities", "entities", CriteriaSpecification.LEFT_JOIN)
                .createAlias("item.itemTypes","itemType")
                .createAlias("planItems","pi")
                .createAlias("pi.item","planItem")
                .createAlias("planItem.itemTypes", "planItemType")
                .add(Restrictions.eq("entities.id", entityId))
                .add(Restrictions.eq("itemType.global", false))
                .add(Restrictions.eq("planItemType.global", false))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);

        return criteria.list();
    }

    /**
     * This method used for finding the plan id's by entity id ByUsing ScrollableResult.
     *
     * @param entityId .
     * @return List<Integer> plan id's.
     */
    public List<Integer> findIdsByEntity(Integer entityId) {
        if (entityId == null) return null;
        Criteria criteria = getSession().createCriteria(PlanDTO.class)
                .setLockMode(LockMode.NONE)
                .createAlias("item", "item")
                .createAlias("item.entities", "entities", CriteriaSpecification.LEFT_JOIN)
                .add(Restrictions.eq("entities.id", entityId))
                .setProjection(Projections.id())
                .addOrder(Order.asc("id"))
                .setComment("findIdsByEntity " + entityId);

        ScrollableResults scrollableResults = criteria.scroll();
        List<Integer> planIds = new ArrayList<Integer>();
        if (scrollableResults != null) {
            try {
                while (scrollableResults.next()) {
                    planIds.add(scrollableResults.getInteger(0));
                }
            } finally {
                scrollableResults.close();
            }
        }
        Collections.sort(planIds);
        return planIds;
    }

    public List<PlanDTO> findPlanByPlanNumber(String planNumber, Integer entityId) {
        return getSession().createCriteria(PlanDTO.class)
                .createAlias("item", "item")
                .createAlias("item.entities", "entities")
                .add(Restrictions.eq("entities.id", entityId))
                .add(Restrictions.eq("item.deleted", 0))
                .add(Restrictions.eq("item.internalNumber", planNumber))
                .list();
    }

    /**
     * Fetch a list of all customers that have subscribed to free trails plan
     * by adding the "plan subscription" item to a recurring order.
     *
     * @param planItemId id of plan
     * @return list of customers subscribed to the plan, empty if none
     */
    @SuppressWarnings("unchecked")
    public List<Integer> findUsersByFreeTrailPlan(Date expiredDate) {
        Query query = getSession().getNamedQuery("PlanDTO.findUserByFreeTrialPlan");
        query.setParameter("expiry_date", expiredDate);

        return query.list();
    }

    public Integer findPlanIdIfUserSubscribedTo(Integer userId, Integer itemId) {
        String sqlQuery =
                "SELECT pi.plan_id " +
                "FROM order_line ol, plan p, base_user u, purchase_order po, order_status os, plan_item pi " +
                "WHERE p.item_id = ol.item_id AND po.id = ol.order_id AND u.id = po.user_id AND po.status_id = os.id AND p.id = pi.plan_id " +
                "AND pi.item_id = :item_id " +
                "AND u.id = :user_id " +
                "AND ol.deleted = 0 " +
                "AND po.deleted = 0 " +
                "AND po.period_id <> 1 " +
                "AND os.order_status_flag <> 1";
        Query query = getSession().createSQLQuery(sqlQuery);
        query.setParameter("user_id", userId);
        query.setParameter("item_id", itemId);
        List<Integer> result = query.list();
        return (result != null && result.size() > 0) ? result.get(0) : null;
    }

    private static final String FIND_PLAN_ID_BY_ORDER_ID = "SELECT id FROM plan WHERE item_id IN ("
            + "SELECT item_id FROM order_line WHERE deleted = 0 AND order_id = :order_id)";
    public Integer findPlanIdByOrderId(Integer orderId) {
        Query query = getSession().createSQLQuery(FIND_PLAN_ID_BY_ORDER_ID);
        query.setParameter("order_id", orderId);
        List<Integer> result = query.list();
        return CollectionUtils.isNotEmpty(result) ? result.get(0) : null;
    }
}
