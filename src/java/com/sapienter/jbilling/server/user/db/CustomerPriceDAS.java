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

package com.sapienter.jbilling.server.user.db;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.BooleanUtils;
import org.hibernate.Query;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * @author Brian Cowdery
 * @since 30-08-2010
 */
public class CustomerPriceDAS extends AbstractDAS<CustomerPriceDTO> {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PLAN_ITEM_FIND_HQL =
            "select price "
                    + " from CustomerPriceDTO price "
                    + " where price.id.baseUser.id = :user_id"
                    + " and price.id.planItem.id = :plan_item_id";

    private static final String CUSTOMER_PRICE_FIND_HQL =
            "select price.id.planItem "
                    + " from CustomerPriceDTO price "
                    + " where price.id.planItem.item.id = :item_id "
                    + " and price.id.baseUser.id = :user_id ";

    public CustomerPriceDTO find(Integer userId, Integer planItemId) {
        Query query = getSession().createQuery(PLAN_ITEM_FIND_HQL);
        query.setParameter("user_id", userId);
        query.setParameter("plan_item_id", planItemId);

        return (CustomerPriceDTO) query.uniqueResult();
    }

    /**
     * Fetch a list of all customer specific prices.
     *
     * @param userId user id of the customer
     * @return list of customer specific prices, empty list if none found
     */
    @SuppressWarnings("unchecked")
    public List<PlanItemDTO> findAllCustomerSpecificPrices(Integer userId) {
        Query query = getSession().getNamedQuery("PlanItemDTO.findAllCustomerSpecificPrices");
        query.setParameter("user_id", userId);

        return query.list();
    }

    /**
     * Fetch a list of all customer prices.
     *
     * @param userId user id of the customer
     * @return list of customer prices, empty list if none found
     */
    @SuppressWarnings("unchecked")
    public List<PlanItemDTO> findAllCustomerPrices(Integer userId) {
        Query query = getSession().getNamedQuery("PlanItemDTO.findAllCustomerPrices");
        query.setParameter("user_id", userId);

        return query.list();
    }

    /**
     * Fetch the customer price for the given customer and item.
     *
     * @param userId user id of the customer
     * @param itemId item id of the item being priced
     * @return customer price for the given item, or null if none found
     */
    public PlanItemDTO findPriceByItem(Integer userId, Integer itemId, Boolean planPricingOnly, Date pricingDate, Integer planId) {

        logger.debug("logging customer {}  for item {} for date {} price before query", userId, itemId, pricingDate);
        logCustomerPriceForUser(userId, pricingDate);
        StringBuffer hql = new StringBuffer();
        hql.append(CUSTOMER_PRICE_FIND_HQL);

        if ( null != pricingDate ) {
            hql.append(PRICING_DATE_CONDITION);
        }

        if (planPricingOnly != null) {
            hql.append(planPricingOnly ? PLAN_PRICE_ONLY_CONDITION_HQL : CUSTOMER_PRICE_ONLY_CONDITION_HQL);
            if(planPricingOnly && null != planId) {
                hql.append(PLAN_PRICE_WITH_ID_CONDITION_HQL);
            }
        }

        hql.append(PRICE_ATTRIBUTE_ORDER_HQL);
        logger.debug("Constructed HQL query with attributes: {} ", hql);


        Query query = getSession().createQuery(hql.toString());

        query.setParameter("user_id", userId, StandardBasicTypes.INTEGER);
        query.setParameter("item_id", itemId, StandardBasicTypes.INTEGER);
        if(BooleanUtils.isTrue(planPricingOnly) && null != planId) {
            query.setParameter("plan_id", planId, StandardBasicTypes.INTEGER);
        }
        if ( null != pricingDate ) {
            query.setParameter("active_date", pricingDate, StandardBasicTypes.DATE);
            query.setParameter("expiry_date", pricingDate, StandardBasicTypes.DATE);
        }
        query.setMaxResults(1);

        PlanItemDTO result = (PlanItemDTO) query.uniqueResult();
        logger.debug("logging customer {}  for item {} for date {} price after query", userId, itemId, pricingDate);
        logCustomerPriceForUser(userId, pricingDate);
        return result;
    }

    /**
     * Fetch a list of all customer prices for a specific item.
     *
     *
     * @param userId user id of the customer
     * @param planPricingOnly
     *@param itemId item id  @return list of customer prices, empty list if none found
     */
    @SuppressWarnings("unchecked")
    public List<PlanItemDTO> findAllCustomerPricesByItem(Integer userId, Boolean planPricingOnly, Integer itemId, Integer planId) {

        StringBuffer hql = new StringBuffer();
        hql.append(CUSTOMER_PRICE_FIND_HQL);

        if (planPricingOnly != null) {
            hql.append(planPricingOnly ? PLAN_PRICE_ONLY_CONDITION_HQL : CUSTOMER_PRICE_ONLY_CONDITION_HQL);
            if(planPricingOnly && null != planId) {
                hql.append(PLAN_PRICE_WITH_ID_CONDITION_HQL);
            }
        }

        hql.append(PRICE_ATTRIBUTE_ORDER_HQL);
        logger.debug("Constructed HQL query with attributes: {} ", hql);

        Query query = getSession().createQuery(hql.toString());
        query.setParameter("user_id", userId);
        query.setParameter("item_id", itemId);
        if(BooleanUtils.isTrue(planPricingOnly) && null != planId) {
            query.setParameter("plan_id", planId);
        }

        return query.list();
    }

    /**
     * Deletes all customer prices for the given plan id for a customer.
     *
     * @param userId user id of the customer
     * @param planId id of the plan
     * @return number of rows deleted
     */
    public int deletePrices(Integer userId, Integer planId) {
        Query query = getSession().getNamedQuery("CustomerPriceDTO.deletePriceByPlan");
        query.setParameter("plan_id", planId);
        query.setParameter("user_id", userId);

        return query.executeUpdate();
    }

    /**
     * Expires all customer prices for the given plan id for a customer.
     *
     * @param userId user id of the customer
     * @param planId id of the plan
     * @return number of rows deleted
     */
    public int expirePrices(Integer userId, Integer planId, Date effectiveDate) {
        Query query = getSession().getNamedQuery("CustomerPriceDTO.expirePriceByPlan");
        query.setParameter("end_date", Util.truncateDate(effectiveDate));
        query.setParameter("plan_id", planId);
        query.setParameter("user_id", userId);

        return query.executeUpdate();
    }

    /**
     * Expires planItem price for a customer.
     *
     * @param effectiveDate user id of the customer
     * @param planItemId id of the plan
     * @return number of rows deleted
     */
    public int expireAllCustomersPrice(Integer planItemId, Date effectiveDate) {
        Query query = getSession().getNamedQuery("CustomerPriceDTO.expirePrice");
        query.setParameter("end_date", Util.truncateDate(effectiveDate));
        query.setParameter("plan_item_id", planItemId);

        return query.executeUpdate();
    }

    /**
     * Expires ALL customer prices using the given plan items.
     *
     * @param planItems plan items to remove from customer pricing
     * @return number of rows deleted
     */
    public int expireAllCustomersPricesByItems (List<PlanItemDTO> planItems, Date effectiveDate) {
        if (planItems.size() < 1) {
            return 0;
        } else {
            List<Integer> ids = new ArrayList<Integer>(planItems.size());
            for (PlanItemDTO planItem : planItems) {
                ids.add(planItem.getId());
            }

            Query query = getSession().getNamedQuery("CustomerPriceDTO.expirePricesByItems");
            query.setParameter("end_date", Util.truncateDate(effectiveDate));
            query.setParameterList("plan_item_ids", ids);

            return query.executeUpdate();
        }
    }

    /**
     * Refreshes the priceSubscriptionDate for the user's plan
     * @param userId
     * @param planId
     * @param newSubscriptionDate
     * @return
     */
    public int updatePriceSubscriptionDate(Integer userId, Integer planId, Date newSubscriptionDate) {
        Query query = getSession().getNamedQuery("CustomerPriceDTO.updatePriceSubscriptionDateByPlan");
        query.setParameter("new_active_date", Util.truncateDate(newSubscriptionDate));
        query.setParameter("plan_id", planId);
        query.setParameter("user_id", userId);

        return query.executeUpdate();
    }

    /**
     * Deletes all the given customer prices for the given list of plan items.
     *
     * @param userId    user id of the customer
     * @param planItems list of plan items to delete
     * @return number of rows deleted
     */
    public int deletePrices(Integer userId, List<PlanItemDTO> planItems) {
        int deleted = 0;

        for (PlanItemDTO planItem : planItems) {
            deleted += deletePrice(userId, planItem.getId());
        }

        return deleted;
    }

    /**
     * Deletes the customer price for the given plan item id (plan item price).
     *
     * @param userId     user id of the customer
     * @param planItemId plan item price id
     * @return number of rows deleted
     */
    public int deletePrice(Integer userId, Integer planItemId) {
        Query query = getSession().getNamedQuery("CustomerPriceDTO.deletePrice");
        query.setParameter("user_id", userId);
        query.setParameter("plan_item_id", planItemId);

        return query.executeUpdate();
    }

    /**
     * Deletes ALL customer prices using the given plan items.
     *
     * @param planItems plan items to remove from customer pricing
     * @return number of rows deleted
     */
    public int deletePricesByItems (List<PlanItemDTO> planItems) {
        if (planItems.size() < 1) {
            return 0;
        } else {
            List<Integer> ids = new ArrayList<Integer>(planItems.size());
            for (PlanItemDTO planItem : planItems) {
                ids.add(planItem.getId());
            }

            Query query = getSession().getNamedQuery("CustomerPriceDTO.deletePricesByItems");
            query.setParameterList("plan_item_ids", ids);

            return query.executeUpdate();
        }
    }

    // it would be nice to do this with hibernate criteria, but unfortunately criteria
    // queries do not support collections of values (attributes['key_name'] = ?), so we
    // need to manually construct the HQL query by hand.

    private static final String PRICE_ATTRIBUTE_QUERY_HQL =
            "select price.id.planItem "
                    + " from CustomerPriceDTO price "
                    + "  join price.id.planItem.model as model "
                    + " where price.id.planItem.item.id = :item_id "
                    + "  and price.id.baseUser.id = :user_id ";

    //expiry date not inclusive
    private static final String PRICING_DATE_CONDITION =
            " and ( price.priceSubscriptionDate is null or price.priceSubscriptionDate <= :active_date ) " +
                    " and ( price.priceExpiryDate is null or price.priceExpiryDate > :expiry_date ) ";

    private static final String PLAN_PRICE_ONLY_CONDITION_HQL =
            " and price.id.planItem.plan is not null";

    private static final String CUSTOMER_PRICE_ONLY_CONDITION_HQL =
            " and price.id.planItem.plan is null";

    private static final String PRICE_ATTRIBUTE_ORDER_HQL =
            " order by price.id.planItem.precedence, price.createDatetime desc";

    private static final String PLAN_PRICE_WITH_ID_CONDITION_HQL =
            " and price.id.planItem.plan.id = :plan_id";
    /**
     * Fetch all customer pricing in order of precedence (highest first), where
     * all plan attributes <strong>must</strong> match the given map of attributes.
     *
     *
     * @param userId     user id of the customer
     * @param itemId     item id of the item being priced
     * @param attributes attributes of pricing to match
     * @param planPricingOnly
     *@param maxResults maximum limit of returned query results  @return list of found plan prices, empty list if none found.
     */
    @SuppressWarnings("unchecked")
    public List<PlanItemDTO> findPriceByAttributes(Integer userId, Integer itemId, Map<String, String> attributes,
                                                   Boolean planPricingOnly, Integer maxResults, Date pricingDate, Integer planId) {

        StringBuffer hql = new StringBuffer();
        hql.append(PRICE_ATTRIBUTE_QUERY_HQL);

        if ( null != pricingDate ) {
            hql.append(PRICING_DATE_CONDITION);
        }

        // build collection of values query from attributes
        // clause - "and price.attributes['key'] = :key"
        for (String key : attributes.keySet()) {
            hql.append(" and ")
            .append(getAttributeClause(key))
            .append(" = ")
            .append(getAttributeNamedParameter(key));
        }

        if (planPricingOnly != null) {
            hql.append(planPricingOnly ? PLAN_PRICE_ONLY_CONDITION_HQL : CUSTOMER_PRICE_ONLY_CONDITION_HQL);
            if(planPricingOnly && null != planId) {
                hql.append(PLAN_PRICE_WITH_ID_CONDITION_HQL);
            }
        }

        hql.append(PRICE_ATTRIBUTE_ORDER_HQL);
        logger.debug("Constructed HQL query with attributes: {} ", hql);

        Query query = getSession().createQuery(hql.toString());
        query.setParameter("user_id", userId);
        query.setParameter("item_id", itemId);
        if(BooleanUtils.isTrue(planPricingOnly) && null != planId) {
            query.setParameter("plan_id", planId);
        }

        if ( null != pricingDate ) {
            query.setParameter("active_date", pricingDate);
            query.setParameter("expiry_date", pricingDate);
        }

        // bind attribute named parameters
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.list();
    }

    private static final String GET_CSUTOMER_PRICE_SQL = "SELECT * FROM customer_price WHERE user_id = ?";
    private void logCustomerPriceForUser(Integer userId, Date pricingDate) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GET_CSUTOMER_PRICE_SQL, userId);
        for(Map<String, Object> row : rows) {
            logger.debug("customer price for user {} is {} for pricing date {}", userId, row, pricingDate);
        }
    }
    /**
     * Fetch all customer pricing in order of precedence (highest first), where
     * attributes matched are equal or saved in the database as a wildcard ('*'). Allows partial
     * matches of attributes to find the "best fit" pricing.
     * <p/>
     * Attributes may be persisted as a wildcard ('*') which will match any attribute value
     * passed into this method. This is useful for defining pricing that only need to match
     * on a single attribute out of many possible attributes.
     * <p/>
     * Eg.
     * <p/>
     * Item price with saved attributes:
     * <code>
     * lata = '*'
     * rateCenter = '*'
     * stateProvince = 'NC'
     * </code>
     * <p/>
     * Matches:
     * <code>
     * lata = '0772'
     * rateCenter = 'CHARLOTTE'
     * stateProvince = 'NC'
     * </code>
     *
     *
     * @param userId     user id of the customer
     * @param itemId     item id of the item being priced
     * @param attributes attributes of pricing to match
     * @param planPricingOnly
     *@param maxResults maximum limit of returned query results  @return list of found prices, empty list if none found.
     */
    @SuppressWarnings("unchecked")
    public List<PlanItemDTO> findPriceByWildcardAttributes(Integer userId, Integer itemId,Map<String, String> attributes,
                                                           Boolean planPricingOnly, Integer maxResults, Date pricingDate,
                                                           Integer planId) {

        StringBuffer hql = new StringBuffer();
        hql.append(PRICE_ATTRIBUTE_QUERY_HQL);

        if ( null != pricingDate ) {
            hql.append(PRICING_DATE_CONDITION);
        }

        // build collection of values query from attributes
        // clause - "and (price.attributes['key'] = :key or price.attributes['key'] = '*')"
        for (String key : attributes.keySet()) {
            hql.append(" and (")
            .append(getAttributeClause(key))
            .append(" = ")
            .append(getAttributeNamedParameter(key))
            .append(" or ")
            .append(getAttributeClause(key))
            .append(" = '*')");
        }

        if (planPricingOnly != null) {
            hql.append(planPricingOnly ? PLAN_PRICE_ONLY_CONDITION_HQL : CUSTOMER_PRICE_ONLY_CONDITION_HQL);
            if(planPricingOnly && null != planId) {
                hql.append(PLAN_PRICE_WITH_ID_CONDITION_HQL);
            }
        }

        hql.append(PRICE_ATTRIBUTE_ORDER_HQL);
        logger.debug("Constructed HQL query with wildcard attributes: {} ", hql);

        Query query = getSession().createQuery(hql.toString());
        query.setParameter("user_id", userId);
        query.setParameter("item_id", itemId);
        if(BooleanUtils.isTrue(planPricingOnly) && null != planId) {
            query.setParameter("plan_id", planId);
        }

        if ( null != pricingDate ) {
            query.setParameter("active_date", pricingDate);
            query.setParameter("expiry_date", pricingDate);
        }

        // bind attribute named parameters
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }

        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }

        return query.list();
    }

    private String getAttributeClause(String key) {
        return "model.attributes['" + key + "']";
    }

    private String getAttributeNamedParameter(String key) {
        return ":" + key;
    }


    /**
     * Fetch the customer price for the given customer and item.
     *
     * @param userId user id of the customer
     * @param itemId item id of the item being priced
     * @return customer price for the given item, or null if none found
     */
    public List<PlanItemDTO> findPricesByItemAndDate(Integer userId, Integer itemId, Boolean planPricingOnly, Date pricingDate,
            Integer planId) {
        StringBuffer hql = new StringBuffer();
        hql.append(CUSTOMER_PRICE_FIND_HQL);

        if ( null != pricingDate ) {
            hql.append(PRICING_DATE_CONDITION);
        }

        if (planPricingOnly != null) {
            hql.append(planPricingOnly ? PLAN_PRICE_ONLY_CONDITION_HQL : CUSTOMER_PRICE_ONLY_CONDITION_HQL);
            if(planPricingOnly && null != planId) {
                hql.append(PLAN_PRICE_WITH_ID_CONDITION_HQL);
            }
        }

        hql.append(PRICE_ATTRIBUTE_ORDER_HQL);
        logger.debug("Constructed HQL query with attributes: {} ", hql);


        Query query = getSession().createQuery(hql.toString());

        query.setParameter("user_id", userId, StandardBasicTypes.INTEGER);
        query.setParameter("item_id", itemId, StandardBasicTypes.INTEGER);
        if(BooleanUtils.isTrue(planPricingOnly) && null != planId) {
            query.setParameter("plan_id", planId, StandardBasicTypes.INTEGER);
        }
        if ( null != pricingDate ) {
            query.setParameter("active_date", pricingDate, StandardBasicTypes.DATE);
            query.setParameter("expiry_date", pricingDate, StandardBasicTypes.DATE);
        }

        return query.list();
    }


}
