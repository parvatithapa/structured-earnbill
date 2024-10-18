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

package com.sapienter.jbilling.server.pricing.tasks;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.BOOLEAN;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.STR;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.tasks.IPricing;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.UsageBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineInfo;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskDTO;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pricing.PriceContextDTO;
import com.sapienter.jbilling.server.pricing.PriceModelResolutionContext;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;

/**
 * Pricing plug-in that calculates prices using the customer price map and PriceModelDTO
 * pricing strategies. This plug-in allows for complex pricing strategies to be applied
 * based on a customers subscribed plans, quantity purchased and the current usage.
 *
 * @author Brian Cowdery
 * @since 16-08-2010
 */
public class PriceModelPricingTask extends PluggableTask implements IPricing {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Type of usage calculation
     */
    private enum UsageType {
        /** Count usage from the user making the pricing request */
        USER,

        /** Count usage from the user that holds the price */
        PRICE_HOLDER;

        public static UsageType valueOfIgnoreCase(String value) {
            return UsageType.valueOf(value.trim().toUpperCase());
        }
    }

    private static final ParameterDescription USE_ATTRIBUTES = new ParameterDescription("use_attributes", false, BOOLEAN);
    private static final ParameterDescription USE_WILDCARDS = new ParameterDescription("use_wildcards", false, BOOLEAN);
    private static final ParameterDescription USAGE_TYPE = new ParameterDescription("usage_type", false, STR);
    private static final ParameterDescription SUB_ACCOUNT_USAGE = new ParameterDescription("include_sub_account_usage", false, BOOLEAN);
    private static final ParameterDescription USE_NEXT_INVOICE_DATE = new ParameterDescription("use_next_invoice_date", false, BOOLEAN);
    private static final ParameterDescription USE_COMPANY_TIMEZONE_FOR_EVENT_DATE = new ParameterDescription(
            "use_company_timezone_for_event_date", false, BOOLEAN);

    private static final boolean DEFAULT_USE_ATTRIBUTES = false;
    private static final String DEFAULT_USAGE_TYPE = UsageType.PRICE_HOLDER.name();
    private static final boolean DEFAULT_SUB_ACCOUNT_USAGE = false;
    private static final boolean DEFAULT_USE_NEXT_INVOICE_DATE = false;
    private static final boolean DEFAULT_USE_COMPANY_TIMEZONE_FOR_EVENT_DATE = false;
    protected Boolean USE_WILDCARD_ATTRIBUTES;

    private PricingModelDataProvider dataProvider;

    public PriceModelPricingTask() {
        descriptions.add(USE_ATTRIBUTES);
        descriptions.add(USE_WILDCARDS);
        descriptions.add(USAGE_TYPE);
        descriptions.add(SUB_ACCOUNT_USAGE);
        descriptions.add(USE_NEXT_INVOICE_DATE);
        descriptions.add(USE_COMPANY_TIMEZONE_FOR_EVENT_DATE);
    }

    @Override
    public void initializeParamters(PluggableTaskDTO task) throws PluggableTaskException {
        super.initializeParamters(task);
        USE_WILDCARD_ATTRIBUTES = getParameter(USE_WILDCARDS.getName(), false);
        dataProvider = Context.getBean(PricingModelDataProvider.BEAN_NAME);
    }

    @Override
    public BigDecimal getPrice(
            PriceContextDTO priceContext,
            BigDecimal defaultPrice,
            OrderDTO pricingOrder,
            OrderLineDTO orderLine,
            boolean singlePurchase) throws TaskException {

        Date eventDate = priceContext.getEventDate();
        Date pricingDate = (null == eventDate) ? getPricingDate(pricingOrder) : getTimeZonedEventDate(eventDate, task.getEntityId());
        BigDecimal quantity = priceContext.getQuantity();
        ItemDTO item = priceContext.getItem();
        Integer userId = priceContext.getUserId();
        logger.debug("Calling PriceModelPricingTask with pricing order: {}, for date {}", pricingOrder, pricingDate);
        logger.debug("Pricing item {}, quantity {} - for user {}", item.getId(), quantity, userId);

        if (userId != null) {
            // get customer pricing model, use fields as attributes
            Map<String, String> attributes = getAttributes(priceContext.getFields());

            NavigableMap<Date, PriceModelDTO> models = null;

            // iterate through parents until a price is found.
            UserBL user = new UserBL(userId);
            CustomerDTO customer = user.getEntity() != null ? user.getEntity().getCustomer() : null;

            PriceModelResolutionContext ctx = null;

            if (customer != null && customer.useParentPricing()) {
                boolean parentModelsFound = false;
                while (customer.getParent() != null && !parentModelsFound) {
                    customer = customer.getParent();

                    logger.debug("Looking for price from parent user {}", customer.getBaseUser().getId());

                    ctx = priceModelResolutionContext(
                            customer.getBaseUser().getId(),
                            item.getId(),
                            pricingDate,
                            attributes,
                            isMediated(pricingOrder));
                    NavigableMap<Date, PriceModelDTO> parentModels = getPricesByHierarchy(ctx);

                    if (parentModels != null && !parentModels.isEmpty()) {
                        parentModelsFound = true;
                        logger.debug("Found price from parent user: {}", models);
                        models = parentModels;
                    }
                }
            }

            if (MapUtils.isEmpty(models)) {
                // price for customer depending on the product pricing hierarchy
                ctx = priceModelResolutionContext(
                        userId,
                        item.getId(),
                        pricingDate,
                        attributes,
                        isMediated(pricingOrder));
                models = getPricesByHierarchy(ctx);
            }

            logger.debug("Prices found by hierarchy: {}", models);

            PriceModelDTO model = getPriceModelForDate(models, pricingDate);
            if (model == null) {
                // no customer price, the customer has not subscribed to a plan affecting this
                // item, or does not have a customer specific price set. Use the item default price.
                long productPriceLoad = System.currentTimeMillis();
                models = getProductPriceModel(ctx);
                logger.debug("getProductPriceModel took {} miliseconds for user {}",
                        (System.currentTimeMillis() - productPriceLoad), ctx.getUserId());
                if (models != null && !models.isEmpty()) {
                    logger.debug("fetched product level prices for user {} for item {} for pricing date {}", userId, item.getId(), pricingDate);
                    model = getPriceModelForDate(models, pricingDate);
                }
            }

            logger.debug("Price date: {}", pricingDate);

            // apply price model
            if(model != null) {
                logger.debug("Applying price model {}", model);

                Usage usage = null;
                long applyPriceModel = System.currentTimeMillis();
                PricingResult result = new PricingResult(item.getId(), quantity, userId, priceContext.getCurrencyId());
                for (PriceModelDTO next = model; next != null; next = next.getNext()) {
                    // fetch current usage of the item if the pricing strategy requires it
                    if (next.getStrategy().requiresUsage()) {
                        UsageType type = UsageType.valueOfIgnoreCase(getParameter(USAGE_TYPE.getName(), DEFAULT_USAGE_TYPE));
                        Integer priceUserId = customer != null ? customer.getBaseUser().getId() : userId;
                        long getUsageLoadTime = System.currentTimeMillis();
                        usage = getUsage(type, item.getId(), userId, priceUserId, pricingOrder);
                        logger.debug("getUsage took {} miliseconds for user {}",
                                (System.currentTimeMillis() - getUsageLoadTime), userId);
                        logger.debug("Current usage of item {} : {}", item.getId(), usage);
                    } else {
                        logger.debug("Pricing strategy {} does not require usage.", next.getType());
                    }

                    if (null != next.getNext()) {
                        result.setIsChained(true);
                    }
                    logger.debug("Call Before apply");
                    next.applyTo(pricingOrder, orderLine, result.getQuantity(), result, priceContext.getFields(),
                            usage, singlePurchase, pricingDate);
                    logger.debug("Price discovered: {}", result.getPrice());
                }
                logger.debug("applyPriceModel took {} for user {}", (System.currentTimeMillis() - applyPriceModel), userId);
                if (result.isPercentage() ) {
                    item.setPercentage(result.getPrice());
                }
                item.setIsPercentage(result.isPercentage());

                if (needToRecalculate(model, orderLine)) {
                    recalculatePrice(orderLine, result, model);
                }

                return result.getPrice();
            } else {
                logger.debug("No price model found, using default price.");
                return defaultPrice;
            }
        }

        logger.debug("No price model found, using default price.");
        return defaultPrice;
    }

    private PriceModelDTO getPriceModelForDate(NavigableMap<Date, PriceModelDTO> prices, Date pricingDate) {
        if (MapUtils.isEmpty(prices)) {
            logger.debug("No price models available");
            return null;
        }

        Map.Entry<Date, PriceModelDTO> priceModel;
        if (pricingDate == null) {
            priceModel = prices.firstEntry();
            logger.debug("No pricing date, returning the first price: {}", priceModel.getKey());
            return priceModel.getValue();
        }

        priceModel = prices.floorEntry(pricingDate);

        if (priceModel == null) {
            logger.debug("No effective price model.");
            return null;
        }

        logger.debug("Found effective price model: {}", priceModel.getKey());
        return priceModel.getValue();
    }

    /**
     * Is this a mediated order.
     *
     * @param pricingOrder
     * @return
     */
    private boolean isMediated(OrderDTO pricingOrder) {
        return null!=pricingOrder && pricingOrder.getIsMediated();
    }


    /**
     * Checks if price needs to be recalculated because of use of Free Usage Pools
     * In certain scenarios such as tiered and graduated, recalculation is not required when editing an order.
     * @param model
     * @param orderLine
     * @return true or false
     */
    private boolean needToRecalculate(PriceModelDTO model, OrderLineDTO orderLine) {
        boolean needToRecalculate = false;
        if (null != model && null != model.getType() && null != orderLine) {
            if ( (null != orderLine.getItem()) && new ItemDAS().isPlan(orderLine.getItem().getId())) {
                return false;
            }
            if (orderLine.isMediated()) {
                needToRecalculate = true;
            } else {
                BigDecimal customerUsagePoolQuantity = orderLine.getCustomerUsagePoolQuantity();
                if (((model.getType().equals(PriceModelStrategy.TIERED)) ||
                        (model.getType().equals(PriceModelStrategy.GRADUATED)) ||
                        (model.getType().equals(PriceModelStrategy.POOLED)) ||
                        (model.getType().equals(PriceModelStrategy.QUANTITY_ADDON))	) &&
                        (customerUsagePoolQuantity.compareTo(BigDecimal.ZERO) <= 0)
                        ) {

                    if (orderLine.getId() == 0) {
                        needToRecalculate = true;
                    }

                } else if (model.getType().equals(PriceModelStrategy.QUANTITY_ADDON) ||
                        (null != model.getNext() && model.getNext().getType().equals(PriceModelStrategy.PERCENTAGE))
                        ) {
                    needToRecalculate = false;
                } else {
                    needToRecalculate = true;
                }
            }
        }
        return needToRecalculate;
    }

    private void recalculatePriceForCapped(OrderLineDTO line, PricingResult result, PriceModelDTO model) {
        logger.debug("recalculatePriceForCapped called, line: {}", line);
        BigDecimal price = result.getPrice();
        logger.debug("recalculatePriceForCapped: Pricing Price: {} ", price);
        if (null != line) {
            BigDecimal freeUsageQuantity = line.getFreeUsagePoolQuantity();
            logger.debug("recalculatePriceForCapped: freeUsageQuantity: {}", freeUsageQuantity);
            if (freeUsageQuantity.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal maximum = AttributeUtils.getDecimal(model.getAttributes(), "max");
                BigDecimal chargeableQuantity = line.getQuantity().subtract(freeUsageQuantity);
                BigDecimal lineAmount = chargeableQuantity.multiply(price).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                if (lineAmount.compareTo(maximum) >= 0) {
                    price = maximum.divide(line.getQuantity(), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    result.setPrice(price);
                } else {
                    price = lineAmount.divide(line.getQuantity(), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    result.setPrice(price);
                }
            }
        }
    }

    /**
     * Recalculates the price after deducting free usage quantity. This is required because
     * the pricing strategy gives the price over the chargeable quantity after deducting free usage.
     * The recalculate here finds out the average price over the total quantity (of the line).
     * @param line
     * @param result
     */
    private void recalculatePrice(OrderLineDTO line, PricingResult result, PriceModelDTO model) {
        if(null == model) {
            return ;
        }
        if (null != line &&
                line.isMediated() && model.getType().equals(PriceModelStrategy.CAPPED_GRADUATED)) {
            logger.debug("recalculatePriceForCapped called, line: {}", line);
            recalculatePriceForCapped(line, result, model);
        } else if (!model.getType().equals(PriceModelStrategy.CAPPED_GRADUATED)
                && !model.getType().equals(PriceModelStrategy.BLOCK_AND_INDEX)) {
            BigDecimal price = result.getPrice();
            logger.debug("Pricing Task Price: {}", price);
            if (null != line) {
                logger.debug("recalculatePrice called, line: {} ", line);
                BigDecimal freeUsageQuantity = line.getFreeUsagePoolQuantity();
                logger.debug("recalculatePrice: freeUsageQuantity: {} ", freeUsageQuantity);
                BigDecimal recalculatedPrice = price;
                if( freeUsageQuantity.compareTo(line.getQuantity()) == 0 ) {
                    recalculatedPrice = BigDecimal.ZERO;
                    line.setAmount(BigDecimal.ZERO);
                } else {
                    if (freeUsageQuantity.compareTo(BigDecimal.ZERO) > 0 && !(line.getItem().isPlan())) {
                        if(!model.getType().equals(PriceModelStrategy.ROUTE_BASED_RATE_CARD)) {
                            BigDecimal chargeableQuantity = line.getQuantity().subtract(freeUsageQuantity);
                            BigDecimal lineAmount = chargeableQuantity.multiply(price).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                            recalculatedPrice = lineAmount.divide(line.getQuantity(), MathContext.DECIMAL128)
                                    .setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                        } else if(model.getType().equals(PriceModelStrategy.ROUTE_BASED_RATE_CARD) && line.isMediated()) {
                            OrderLineInfo oldLineInfo = line.getOldOrderLineInfo();
                            BigDecimal oldAmount = null!=oldLineInfo ? oldLineInfo.getAmount() : BigDecimal.ZERO;
                            BigDecimal newAmount = line.getMediatedQuantity().multiply(price, MathContext.DECIMAL128);
                            line.setAmount(oldAmount.add(newAmount, MathContext.DECIMAL128));
                            recalculatedPrice = line.getAmount().divide(line.getQuantity(), MathContext.DECIMAL128)
                                    .setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                        }
                        logger.debug("new calculated price is {} for user {} for order's {}'s line {} for item {}", recalculatedPrice,
                                result.getUserId(), line.getPurchaseOrder().getId(), line.getId(), result.getItemId());
                    }
                }
                result.setPrice(recalculatedPrice);
            }

        }

        /*
         * DONT REMOVE COMMENTED CODE.
         * This code is not being removed as it tries to handle one scenario of release of
         * free quantity to usage pools if line containing the free quantity is removed.
         * Currently this scenario is not supported, but we may look at it in future if required.
         *
		if (null != line && line.getDeleted() == 1 && freeUsageQuantity.compareTo(BigDecimal.ZERO) > 0) {
			// Since the line is deleted and it is using free usage quantity,
			// lets recalculate entire order so that the deleting lines free quantity can be given to any other lines
			for (OrderLineDTO ol : line.getPurchaseOrder().getLines()) {
				if (null != ol.getItem() && ol.getItemId() == line.getItemId()) {
					if (ol.getDeleted() == 0 && ol.getQuantity().compareTo(ol.getFreeUsagePoolQuantity()) > 0) {
						BigDecimal potentialFreeQuantity = ol.getQuantity().subtract(ol.getFreeUsagePoolQuantity());
						if (potentialFreeQuantity.compareTo(freeUsageQuantity) <= 0) {
							ol.setPrice(BigDecimal.ZERO);
						} else {
							// the potential quantity that can be made free is higher than freeUsageQuantity made free by the deleting order line.
							BigDecimal chargeableQuantity = potentialFreeQuantity.subtract(freeUsageQuantity);
							BigDecimal lineAmount = chargeableQuantity.multiply(price);
							BigDecimal recalculatedPrice = lineAmount.divide(line.getQuantity().setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
							recalculatedPrice = recalculatedPrice.setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
					    	ol.setPrice(recalculatedPrice);
						}

						// transfer the free usage quantity from one line to the other
						for (OrderLineUsagePoolDTO olUsagePool : line.getOrderLineUsagePools()) {
							if (olUsagePool.getQuantity().compareTo(potentialFreeQuantity) <= 0) {
								ol.getOrderLineUsagePools().add(new OrderLineUsagePoolDTO(0, ol, olUsagePool.getQuantity(), olUsagePool.getCustomerUsagePool()));
								potentialFreeQuantity = potentialFreeQuantity.subtract(olUsagePool.getQuantity());
								freeUsageQuantity = freeUsageQuantity.subtract(olUsagePool.getQuantity());
							} else {
								if (potentialFreeQuantity.compareTo(BigDecimal.ZERO) > 0) {
									ol.getOrderLineUsagePools().add(new OrderLineUsagePoolDTO(0, ol, potentialFreeQuantity, olUsagePool.getCustomerUsagePool()));
								}
								freeUsageQuantity = freeUsageQuantity.subtract(potentialFreeQuantity);
								potentialFreeQuantity = BigDecimal.ZERO;
							}
						}
					}
				}
			}

			line.getOrderLineUsagePools().clear();
		}
         */
    }

    private NavigableMap<Date, PriceModelDTO> getProductPriceModel(PriceModelResolutionContext ctx) {
        logger.debug("No customer price found, using item default price model.");
        return dataProvider.getProductPriceModel(ctx.getItemId(), getEntityId(), useCache(ctx.getMediatedOrder()));
    }


    /**
     * Fetches a price model for the given pricing request.
     *
     * If the parameter "use_attributes" is set, the given pricing fields will be used as
     * query attributes to determine the pricing model.
     *
     * If the parameter "use_wildcards" is set, the price model lookup will allow matches
     * on wildcard attributes (stored in the database as "*").
     *
     * @return found list of dated pricing models, or null if none found
     */
    private NavigableMap<Date, PriceModelDTO> getCustomersPlanPriceModel(
            PriceModelResolutionContext ctx, Boolean planPricingOnly, Integer planId) {

        return dataProvider.getCustomerPlanPriceModel(ctx.getUserId(),
                ctx.getItemId(), ctx.getPricingDate(),
                isAttributesUsed(ctx.getAttributes()), ctx.getAttributes(),
                USE_WILDCARD_ATTRIBUTES,
                planPricingOnly, useCache(ctx.getMediatedOrder()), planId);
    }

    /**
     *  Fetches the account type prices for provided item id
     *  and account type that the specified user(userId) belongs to
     *
     * @return found list of dated pricing models, or null if none found
     */
    private NavigableMap<Date, PriceModelDTO> getAccountTypePriceModel(PriceModelResolutionContext ctx) {
        UserBL userBL = new UserBL(ctx.getUserId());
        CustomerDTO customer = userBL.getEntity().getCustomer();
        if (customer == null || customer.getAccountType() == null) {
            logger.debug("Account Type Pricing not available for user: {}", ctx.getUserId());
            return null;
        }

        return dataProvider.getAccountTypePriceModel(customer, ctx.getItemId(),
                ctx.getPricingDate(), useCache(ctx.getMediatedOrder()));
    }

    private boolean hasParameters() {
        return MapUtils.isNotEmpty(parameters);
    }

    /**
     * Fetches a price model for the given pricing request.
     *
     * If the parameter "use_attributes" is set, the given pricing fields will be used as
     * query attributes to determine the pricing model.
     *
     * If the parameter "use_wildcards" is set, the price model lookup will allow matches
     * on wildcard attributes (stored in the database as "*").
     *
     * @return found list of dated pricing models, or null if none found
     */
    private NavigableMap<Date, PriceModelDTO> getCustomerPriceModel(PriceModelResolutionContext ctx) {

        return dataProvider.getCustomerPriceModel(ctx.getUserId(),
                ctx.getItemId(), ctx.getPricingDate(),
                isAttributesUsed(ctx.getAttributes()), ctx.getAttributes(),
                USE_WILDCARD_ATTRIBUTES,
                useCache(ctx.getMediatedOrder()));
    }

    private Boolean useCache(boolean isMediated) {
        return isMediated && enabledCachingPref();
    }

    private Boolean enabledCachingPref() {
        return PreferenceBL.getPreferenceValueAsIntegerOrZero(getEntityId(),
                Constants.PREFERENCE_MEDIATED_ORDER_PRICING_CACHE) == 1;
    }

    private boolean isAttributesUsed(Map<String, String> attributes) {
        return hasParameters() && getParameter(USE_ATTRIBUTES.getName(), DEFAULT_USE_ATTRIBUTES);
    }

    /**
     *  Retrieves the price model considering the pricing resolution hierarchy.
     *  If the pricing is found for the higher pricing resolution (ex. customer pricing), the search stops and those pricings are retrieved.
     *  Otherwise, the pricing search for the next pricing resolution steps in the hierarchy until pricings are found.
     *
     *  Pricing Resolution Hierararhy:
     *  <ul>
     *      <li>
     *          Customer Pricing Resolution
     *      </li>
     *      <li>
     *          Account Type pricing resolution
     *      </li>
     *      <li>
     *          Plan Pricing resolution - Note that the Plan pricing are resolved
     *          from the customer pricing that have a plan attached to the PlanItemDTO
     *      </li>
     *  </ul>
     *
     * @param context userId, itemId, pricingDate, attributes, planPricingOnly, isMediatedOrder
     * @return found list of dated pricing models, or null if none found
     */
    public NavigableMap<Date, PriceModelDTO> getPricesByHierarchy(PriceModelResolutionContext context) {

        // TODO (pai) make the implementation generic - separate the customer pricing from the plan pricing
        // 1. Customer pricing resolution
        long customerPriceLoad = System.currentTimeMillis();
        NavigableMap<Date, PriceModelDTO> models = getCustomerPriceModel(context);
        logger.debug("getCustomerPriceModel took {} miliseconds for user {}",
                (System.currentTimeMillis() - customerPriceLoad), context.getUserId());
        if (models != null && !models.isEmpty()) {
            logger.debug("fetched customer level prices from user {} for item {} for pricing date {}", context.getUserId(),
                    context.getItemId(), context.getPricingDate());
            return models;
        }

        long accountPriceLoad = System.currentTimeMillis();
        // 2. Account Type pricing resolution
        models = getAccountTypePriceModel(context);
        logger.debug("getAccountTypePriceModel took {} miliseconds for user {}",
                (System.currentTimeMillis() - accountPriceLoad), context.getUserId());
        if (models != null && !models.isEmpty()) {
            logger.debug("fetched account level prices from user {} for item {} for pricing date {}", context.getUserId(),
                    context.getItemId(), context.getPricingDate());
            return models;
        }

        // 3. Plan Pricing resolution - consider only the plan prices from the customer pricing
        Map<String, String> attributes = context.getAttributes();
        String planIdStr = attributes.get(Constants.PLAN_ID);
        Integer planId = null;
        if(StringUtils.isNumeric(planIdStr)) {
            planId = Integer.valueOf(planIdStr);
        }
        long customerPlanPriceLoad = System.currentTimeMillis();
        models = getCustomersPlanPriceModel(context, true, planId);
        logger.debug("getCustomersPlanPriceModel took {} miliseconds for user {}",
                (System.currentTimeMillis() - customerPlanPriceLoad), context.getUserId());
        if (models != null && !models.isEmpty()) {
            logger.debug("fetched Customers Plan level prices from user {} for item {} for pricing date {}", context.getUserId(),
                    context.getItemId(), context.getPricingDate());
            return models;
        }

        return null;
    }

    private PriceModelResolutionContext priceModelResolutionContext(
            Integer userId,
            Integer itemId,
            Date pricingDate,
            Map<String, String> attributes,
            Boolean isMediatedOrder) {

        return PriceModelResolutionContext.builder(itemId)
                .user(userId)
                .pricingDate(pricingDate)
                .attributes(attributes)
                .isMediatedOrder(isMediatedOrder)
                .build();
    }

    public enum PriceModelLevel {
        CUSTOMER_LEVEL,
        ACCOUNT_TYPE_LEVEL,
        PLAN_LEVEL,
        PRODUCT_LEVEL;

        public boolean isCustomerLevel() {
            return this.equals(CUSTOMER_LEVEL);
        }

        public boolean isAccountTypeLevel() {
            return this.equals(ACCOUNT_TYPE_LEVEL);
        }

        public boolean isPlanLevel() {
            return this.equals(PLAN_LEVEL);
        }

        public boolean isProductLevel() {
            return this.equals(PRODUCT_LEVEL);
        }
    }

    public PriceModelLevel getPriceModelLevel(Integer userId, Integer itemId, Map<String, String> attributes) {

        PriceModelResolutionContext ctx = priceModelResolutionContext(userId, itemId, null, attributes, false);
        // 1. Customer pricing resolution
        NavigableMap<Date, PriceModelDTO> models = getCustomerPriceModel(ctx);
        if (models != null && !models.isEmpty()) {
            return PriceModelLevel.CUSTOMER_LEVEL;
        }

        // 2. Account Type pricing resolution
        models = getAccountTypePriceModel(ctx);
        if (models != null && !models.isEmpty()) {
            return PriceModelLevel.ACCOUNT_TYPE_LEVEL;
        }

        // 3. Plan Pricing resolution - consider only the plan prices from the customer pricing
        models = getCustomerPriceModel(ctx);
        if (models != null && !models.isEmpty()) {
            return PriceModelLevel.PLAN_LEVEL;
        }

        return PriceModelLevel.PRODUCT_LEVEL;

    }

    /**
     * Returns the total usage of the given item for the set UsageType, and optionally include charges
     * made to sub-accounts in the usage calculation.
     *
     * @param type usage type to query, may use either USER or PRICE_HOLDER to determine usage
     * @param itemId item id to get usage for
     * @param userId user id making the price request
     * @param priceUserId user holding the pricing plan
     * @param pricingOrder working order (order being edited/created)
     * @return usage for customer and usage type
     */
    private Usage getUsage(UsageType type, Integer itemId, Integer userId, Integer priceUserId, OrderDTO pricingOrder) {
        UsageBL usage;
        switch (type) {
        case USER:
            usage = new UsageBL(userId, pricingOrder);
            break;

        case PRICE_HOLDER:
        default:
            usage = new UsageBL(priceUserId, pricingOrder);
            break;
        }

        // include usage from sub account?
        if (getParameter(SUB_ACCOUNT_USAGE.getName(), DEFAULT_SUB_ACCOUNT_USAGE)) {
            return usage.getSubAccountItemUsage(itemId);
        } else {
            return usage.getItemUsage(itemId);
        }
    }

    /**
     * Convert pricing fields into price model query attributes.
     *
     * @param fields pricing fields to convert
     * @return map of string attributes
     */
    public Map<String, String> getAttributes(List<PricingField> fields) {
        Map<String, String> attributes = new HashMap<>();
        if (fields != null) {
            for (PricingField field : fields) {
                attributes.put(field.getName(), field.getStrValue());
            }
        }
        return attributes;
    }

    /**
     * Return the date of this pricing request. The pricing date will be the "active" date of the pricing
     * order, or the next invoice date if the "use_next_invoice_date" parameter is set to true.
     *
     * If pricing order is null, then today's date will be used.
     *
     * @param pricingOrder pricing order
     * @return date to use for this pricing request
     */
    public Date getPricingDate(OrderDTO pricingOrder) {
        if (pricingOrder != null) {
            if (hasParameters() && getParameter(USE_NEXT_INVOICE_DATE.getName(), DEFAULT_USE_NEXT_INVOICE_DATE)) {
                // use next invoice date of this order
                return new OrderBL(pricingOrder).getInvoicingDate();
            } else {
                // use order active since date, or created date if no active since
                return pricingOrder.getPricingDate();
            }
        } else {
            // no pricing order, use today
            return companyCurrentDate();
        }
    }

    private Date getTimeZonedEventDate(Date eventDate, Integer entityId) {
        if (hasParameters() &&
                getParameter(USE_COMPANY_TIMEZONE_FOR_EVENT_DATE.getName(), DEFAULT_USE_COMPANY_TIMEZONE_FOR_EVENT_DATE)) {
            // get company level time zone
            String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(entityId);
            Date newDate = Date.from(Instant.ofEpochMilli(eventDate.getTime()).atZone(ZoneId.of(companyTimeZone)).toLocalDateTime()
                    .atZone(ZoneId.systemDefault()).toInstant());
            logger.debug("PriceModelPricingTask - company id : {}, time zone : {}, event date : {}, converted event date : {}",
                    entityId, companyTimeZone, eventDate, newDate);
            return newDate;
        }
        return eventDate;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
