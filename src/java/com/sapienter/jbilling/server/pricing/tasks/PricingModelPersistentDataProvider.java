package com.sapienter.jbilling.server.pricing.tasks;


import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.user.CustomerPriceBL;
import com.sapienter.jbilling.server.user.db.AccountTypePriceBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;


public class PricingModelPersistentDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String BEAN_NAME = "pricingModelPersistentDataProvider";
    
    private static final Integer MAX_RESULTS = 1;


    NavigableMap<Date, PriceModelDTO> getProductPrice(Integer itemId, Integer entityId) {
        SortedMap<Date, PriceModelDTO> models = new ItemBL(itemId).getEntity().getDefaultPricesByCompany(entityId);
        return models == null ? null : new TreeMap<>(models);
    }

    NavigableMap<Date, PriceModelDTO> getProductGlobalPrice(Integer itemId) {
        SortedMap<Date, PriceModelDTO> models = new ItemBL(itemId).getEntity().getGlobalDefaultPrices();
        return models == null ? null : new TreeMap<>(models);
    }

    NavigableMap<Date, PriceModelDTO> getCustomerPriceModel(
            Integer userId,
            Integer itemId,
            Date pricingDate,
            boolean useAttributes,
            Map<String, String> attributes,
            boolean useWildcardAttributes) {

        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        List<PlanItemDTO> items;

        if (useAttributes) {
            if (useWildcardAttributes) {
                logger.debug("Fetching customer price using wildcard attributes: {}", attributes);
                items = customerPriceBl.getPricesByWildcardAttributes(
                        itemId, attributes, Boolean.FALSE, null, pricingDate, null);

                if (!items.isEmpty() && items.size() > 1) {
                    logger.warn("{} Price Models were retrieved but only one is returned for pricing.", items.size());
                }

                return !items.isEmpty() ? new TreeMap<>(items.get(0).getModels()) : null;

            } else {
                logger.debug("Fetching customer price using attributes: {}", attributes);
                items = customerPriceBl.getPricesByAttributes(
                        itemId, attributes, Boolean.FALSE, null, pricingDate, null);

                if (!items.isEmpty() && items.size() > 1) {
                    logger.warn("{} Price Models were retrieved but only one is found.", items.size());
                }
            }
        } else {
            // not configured to query prices with attributes, or no attributes given
            // determine customer price normally
            logger.debug("Fetching customer price without attributes (no PricingFields given or 'use_attributes' = false)");
            items = customerPriceBl.getAllCustomerPricesForDate(
                    itemId, Boolean.FALSE, pricingDate, null);
        }

        // Customer prices today are saved one model (having a startDate) per planItem.
        // Each price creates a new planItem. Therefore it is important to get all the
        // prices and sort them by their dates planItem.priceModel.startDate
        // TODO - Every new Customer Price or Account Type should not create a new Plan Item,
        // todo (contd..) instead, only add a priceModel with a new startDate

        NavigableMap<Date, PriceModelDTO> models = new TreeMap<>();
        for (PlanItemDTO item : items) {
            models.putAll(item.getModels());
        }

        return models.size() > 0 ? models : null;
    }

    NavigableMap<Date, PriceModelDTO> getAccountTypePriceModel(
            CustomerDTO customer,
            Integer itemId,
            Date pricingDate) {

        AccountTypePriceBL accountTypePriceBL =
                new AccountTypePriceBL(customer.getAccountType());

        logger.debug("Fetching account type pricing for account type: {} and item: {}",
                accountTypePriceBL.getAccountTypeId(), itemId);

        List<PlanItemDTO> items = accountTypePriceBL
                .getPricesForItemAndPricingDate(itemId, pricingDate);

        NavigableMap<Date, PriceModelDTO> models = new TreeMap<>();
        for (PlanItemDTO item : items) {
            models.putAll(item.getModels());
        }

        return models.size() > 0 ? models : null;
    }

    NavigableMap<Date, PriceModelDTO> getCustomersPlanPriceModel(
            Integer userId,
            Integer itemId,
            Date pricingDate,
            boolean useAttributes,
            Map<String, String> attributes,
            boolean useWildcardAttributes,
            Boolean planPricingOnly,
            Integer planId) {

        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        if (useAttributes) {
            if (useWildcardAttributes) {
                logger.debug("Fetching customer price using wildcard attributes: {}", attributes);
                List<PlanItemDTO> items = customerPriceBl.getPricesByWildcardAttributes(
                        itemId, attributes, planPricingOnly, MAX_RESULTS, pricingDate, planId);

                if (!items.isEmpty() && items.size() > 1) {
                    logger.warn("{} Price Models were retrieved but only one is returned for pricing.", items.size());
                }

                return !items.isEmpty() ? new TreeMap<>(items.get(0).getModels()) : null;

            } else {
                logger.debug("Fetching customer price using attributes: {}", attributes);
                List<PlanItemDTO> items = customerPriceBl.getPricesByAttributes(
                        itemId, attributes, planPricingOnly, MAX_RESULTS, pricingDate, planId);

                if (!items.isEmpty() && items.size() > 1) {
                    logger.warn("{} Price Models were retrieved but only one is returned for pricing.", items.size());
                }
                return !items.isEmpty() ? new TreeMap<>(items.get(0).getModels()) : null;
            }
        } else {
            // not configured to query prices with attributes, or no attributes given
            // determine customer price normally
            logger.debug("Fetching customer plan price without attributes (no PricingFields given or 'use_attributes' = false) for planId : {}",planId);

            PlanItemDTO item = customerPriceBl.getPriceForDate(
                    itemId, planPricingOnly, pricingDate, planId);

            logger.warn("Only one Price Model retrieved, could be more but one is returned for pricing.");

            return item != null ? new TreeMap<>(item.getModels()) : null;
        }
    }
}
