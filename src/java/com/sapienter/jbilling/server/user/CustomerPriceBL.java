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

package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.server.item.db.PlanItemDAS;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.CustomerPriceDAS;
import com.sapienter.jbilling.server.user.db.CustomerPriceDTO;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Business logic for customer to plan item (plan item pricing) mappings.
 *
 * This class handles the application of plan pricing to a customer. Plan item prices can
 * be added and removed from a customer to either grant or revoke access to the plans
 * special pricing for an item.
 *
 * Customer specific pricing can be added by saving a {@link PlanItemDTO} that has no
 * association to a plan. 
 *
 * @see com.sapienter.jbilling.server.pricing.tasks.PriceModelPricingTask
 *
 * @author Brian Cowdery
 * @since 30-08-2010
 */
public class CustomerPriceBL {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private CustomerPriceDAS customerPriceDas;
    private UserBL userBl;

    private CustomerDTO customer;
    private Integer userId;
    private CustomerPriceDTO price;


    public CustomerPriceBL() {
        _init();
    }
    
    public CustomerPriceBL(Integer userId) {
        _init();
        setUserId(userId);
    }

    public CustomerPriceBL(CustomerDTO customer) {
        _init();
        this.customer = customer;
        this.userId = customer.getBaseUser().getId();
    }

    public CustomerPriceBL(Integer userId, Integer planItemId) {
        this(userId);
        setCustomerPrice(planItemId);
    }

    public CustomerPriceBL(CustomerDTO customer, Integer planItemId) {
        this(customer);
        setCustomerPrice(planItemId);

    }

    private void _init() {
        customerPriceDas = new CustomerPriceDAS();
        userBl = new UserBL();
    }

    public void setUserId(Integer userId) {
        userBl.set(userId);
        this.customer = userBl.getEntity().getCustomer();
        this.userId = userId;
    }

    public void setCustomerPrice(Integer planItemId) {
        this.price = customerPriceDas.find(userId, planItemId);

    }

    public void flush() {
        customerPriceDas.flush();
    }

    public CustomerDTO getCustomer() {
        return customer;
    }

    public Integer getUserId() {
        return userId;
    }

    public CustomerPriceDTO getEntity() {
        return price;
    }


    /**
     * Adds the given list of plan item prices to this customer, effectively
     * subscribing the customer to a plan and applying special item pricing.
     *
     * @param planItems plan items to add
     * @return list of saved customer prices
     */
    public List<CustomerPriceDTO> addPrices(List<PlanItemDTO> planItems) {
        List<CustomerPriceDTO> saved = new ArrayList<CustomerPriceDTO>(planItems.size());
        for (PlanItemDTO planItem : planItems)
            saved.add(addPrice(planItem));

        logger.debug("Saved {} customer price entries.",saved.size());
        return saved;
    }

    /**
     * Add a plan item price to this customer, applying a special price for an item.
     *
     * If the given PlanItemDTO is not linked to a PlanDTO, then this price
     * will be treated as a customer-specific price that applies only to
     * this customer.
     *
     * @param planItem plan item to add
     * @return saved customer price
     */
    public CustomerPriceDTO addPrice(PlanItemDTO planItem) {
        return create(planItem);
    }

    public CustomerPriceDTO create(PlanItemDTO planItem) {
        CustomerPriceDTO dto = new CustomerPriceDTO();
        dto.setCustomer(customer);

        /*
            TODO - 20150308 we should set the planItem.id to null
            In other words, in this method, we are simply duplicating the Plan Items of the plan
            into the customer_price table. Tomorrow, if the Plan is modified, the modified prices will be used for the customer.
            Irrespective of the plan being editable or not.
         */

        planItem = new PlanItemDAS().save(planItem);
        dto.setPlanItem(planItem);

        this.price = customerPriceDas.save(dto);
        return this.price;
    }

    public void update(PlanItemDTO planItem) {
        if (price != null) {
            planItem = new PlanItemDAS().save(planItem);
            price.setPlanItem(planItem);

            customerPriceDas.save(price);
        } else {

            logger.error("Cannot update, CustomerPriceDTO not found or not set!");
        }
    }

    public void delete() {
        if (price != null) {
            customerPriceDas.delete(price);
        } else {
            logger.error("Cannot delete, CustomerPriceDTO not found or not set!");
        }

    }

    /**
     * Removes all plan item prices from this customer for the given plan id.
     *
     * @param planId id of plan
     */
    public void removePrices(Integer planId) {
        // batch delete by plan id, only executes 1 query
        int deleted = customerPriceDas.deletePrices(userId, planId);
        logger.debug("Removed {} customer price entries for plan {}", deleted,planId);
    }

    /**
     * Marks all plan item prices from this customer for the given plan id as 'expired'
     * i.e. priceExpiryDate, not inclusive, is set on the plan_time price.
     *
     * @param planId id of plan
     */
    public void expirePrices(Integer planId, Date priceExpiryDate) {
        // batch expire by plan id, only executes 1 query
        int deleted = customerPriceDas.expirePrices(userId, planId, priceExpiryDate);
        logger.debug("Expired {} customer price entries for plan {} effective from {}", deleted, planId, priceExpiryDate);
    }

    /**
     * Updates priceSubscriptionDate for customer prices for the plan's plan-items
     * i.e. newSubscriptionDate, not inclusive, is set on the plan_time price.
     *
     * @param planId id of plan
     */
    public void updatePriceSubscriptionDate(Integer planId, Date newSubscriptionDate) {
        // batch update by plan id, only executes 1 query
        int deleted = customerPriceDas.updatePriceSubscriptionDate(userId, planId, newSubscriptionDate);
        logger.debug("Refreshed {} customer price entries for plan {} effective from {}", deleted, planId, newSubscriptionDate);
    }

    /**
     * Removes the given list of plan item prices from this customer, effectively
     * un-subscribing the customer from a plan and revoking the special item pricing.
     *
     * @param planItems plan items to remove
     */
    public void removePrices(List<PlanItemDTO> planItems) {
        // executes multiple queries to delete each plan item from the customer price map
        int deleted = customerPriceDas.deletePrices(userId, planItems);
        logger.debug("Removed {} customer price entries for {} plan items.", deleted, planItems.size());
    }

    /**
     * Removes a plan item price from this customer, revoking the special price for an item.
     *
     * @param planItem plan item to remove
     */
    public void removePrice(PlanItemDTO planItem) {
        int deleted = customerPriceDas.deletePrice(userId, planItem.getId());
        logger.debug("Removed {} customer price entries for plan item: {}", deleted, planItem);
    }

    /**
     * Judiciously removes all prices from the customer pricing table, ensuring that no
     * customer subscriptions, orphaned prices and foreign keys exist on the given list
     * of plan items.
     *
     * @param planItems plan items to remove from customer pricing
     */
    public void removeAllPrices(List<PlanItemDTO> planItems) {
        int deleted = customerPriceDas.deletePricesByItems(planItems);
        logger.debug("Removed {} customer price entries for {} plan items.",deleted,planItems.size());
    }

    /**
     * Returns the customer's price for the given item. This method returns null
     * If the customer does not have any special pricing for the given item (customer
     * is not subscribed to a plan affecting the items price, or no customer-specific
     * price found).
     *
     * @param itemId item to price
     * @return customer price, null if no special price found
     */
    public PlanItemDTO getPrice(Integer itemId, Boolean planPricingOnly, Integer planId) {
        return customerPriceDas.findPriceByItem(userId, itemId, planPricingOnly, null, planId);
    }

    /**
     * Returns the customer's price for the given item. This method returns null
     * If the customer does not have any special pricing for the given item (customer
     * is not subscribed to a plan affecting the items price, or no customer-specific
     * price found).
     *
     * @param itemId item to price
     * @return customer price, null if no special price found
     */
    public PlanItemDTO getPriceForDate(Integer itemId, Boolean planPricingOnly, Date pricingDate, Integer planId) {
        return customerPriceDas.findPriceByItem(userId, itemId, planPricingOnly, pricingDate, planId);
    }

    /**
     * Returns a list of all customer-specific prices that apply only to this customer.
     * @return list of prices, empty list if none
     */
    public List<PlanItemDTO> getCustomerSpecificPrices() {
        return customerPriceDas.findAllCustomerSpecificPrices(userId);
    }

    /**
     * Returns a list of all prices for this customer. This will include customer-specific prices
     * and prices applied because the customer has subscribed to a plan.
     *
     * @return list of prices, empty list if none
     */
    public List<PlanItemDTO> getCustomerPrices() {
        return customerPriceDas.findAllCustomerPrices(userId);
    }

    /**
     * Returns a list of all prices for this customer and the given item id. This will include customer-specific
     * prices and prices applied because the customer has subscribed to a plan.
     *
     * @param itemId item id
     * @return list of prices, empty list if none
     */
    public List<PlanItemDTO> getCustomerPrices(Integer itemId) {
        return getCustomerPrices(itemId, null, null);
    }

    /**
     * Returns a list of all prices for this customer and the given item id. This will include customer-specific
     * prices and prices applied because the customer has subscribed to a plan.
     *
     * @param itemId item id
     * @return list of prices, empty list if none
     */
    public List<PlanItemDTO> getCustomerPrices(Integer itemId, Boolean planPricingOnly, Integer planId) {
        return customerPriceDas.findAllCustomerPricesByItem(userId, planPricingOnly, itemId, planId);
    }

    /**
     * Returns the customer's price for the given item and pricing attributes.
     *
     * @see CustomerPriceDAS#findPriceByAttributes(Integer, Integer, java.util.Map, Boolean, Integer, java.util.Date
     *
     * @param itemId id of item being priced
     * @param attributes attributes of pricing to match
     * @return list of found customer prices, empty list if none found.
     */
    public List<PlanItemDTO> getPricesByAttributes(Integer itemId, Map<String, String> attributes, Boolean planPricingOnly, Date pricingDate, Integer planId) {
        return customerPriceDas.findPriceByAttributes(userId, itemId, attributes, planPricingOnly, null, pricingDate, planId);
    }

    /**
     * Returns the customer's price for the given item and pricing attributes, limiting the number
     * of results returned (queried from database).
     *
     * @see CustomerPriceDAS#findPriceByAttributes(Integer, Integer, java.util.Map, Boolean, Integer, java.util.Date
     *
     * @param itemId id of item being priced
     * @param attributes attributes of pricing to match
     * @param maxResults limit database query return results
     * @return list of found customer prices, empty list if none found.
     */
    public List<PlanItemDTO> getPricesByAttributes(Integer itemId, Map<String, String> attributes, Boolean planPricingOnly, Integer maxResults, Date pricingDate, Integer planId) {
        return customerPriceDas.findPriceByAttributes(userId, itemId, attributes, planPricingOnly, maxResults, pricingDate, planId);
    }

    /**
     * Returns the customer's price for the given item and pricing attributes, allowing for wildcard
     * matches of pricing attributes.
     *
     * @see CustomerPriceDAS#findPriceByWildcardAttributes(Integer, Integer, java.util.Map, Boolean, Integer, java.util.Date)
     *
     * @param itemId id of item being priced
     * @param attributes attributes of pricing to match
     * @return list of found customer prices, empty list if none found.
     */
    public List<PlanItemDTO> getPricesByWildcardAttributes(Integer itemId, Map<String, String> attributes, Boolean planPricingOnly, Date pricingDate, Integer planId) {
        return customerPriceDas.findPriceByWildcardAttributes(userId, itemId, attributes, planPricingOnly, null, pricingDate, planId);
    }

    /**
     * Returns the customer's price for the given item and pricing attributes, allowing for wildcard
     * matches of plan attributes and limiting the number of results returned (queried from database).
     *
     * @see CustomerPriceDAS#findPriceByWildcardAttributes(Integer, Integer, java.util.Map, Boolean, Integer, java.util.Date)
     *
     * @param itemId id of item being priced
     * @param attributes attributes of plan pricing to match
     * @param maxResults limit database query return results
     * @return list of found customer prices, empty list if none found.
     */
    public List<PlanItemDTO> getPricesByWildcardAttributes(Integer itemId, Map<String, String> attributes, Boolean planPricingOnly, Integer maxResults, Date pricingDate, Integer planId) {
        return customerPriceDas.findPriceByWildcardAttributes(userId, itemId, attributes, planPricingOnly, maxResults, pricingDate, planId);
    }

    /**
     * Returns the customer's all available prices for the given item and pricing date. This method returns null
     * If the customer does not have any special pricing for the given item (customer
     * is not subscribed to a plan affecting the items price, or no customer-specific
     * price found).
     *
     * @param itemId item to price
     * @return customer price, null if no special price found
     */
    public List<PlanItemDTO> getAllCustomerPricesForDate(Integer itemId, Boolean planPricingOnly, Date pricingDate, Integer planId) {
        return customerPriceDas.findPricesByItemAndDate(userId, itemId, planPricingOnly, pricingDate, planId);
    }

}
