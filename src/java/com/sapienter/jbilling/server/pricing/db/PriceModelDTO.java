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

package com.sapienter.jbilling.server.pricing.db;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SortComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDAS;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderLineUsagePoolDTO;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.strategy.PricingStrategy;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolBL;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;

/**
 * @author Brian Cowdery
 * @since 30-07-2010
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "price_model")
@TableGenerator(name            = "price_model_GEN",
table           = "jbilling_seqs",
pkColumnName    = "name",
valueColumnName = "next_id",
pkColumnValue   = "price_model")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PriceModelDTO implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles
            .lookup().lookupClass());
    public static final String ATTRIBUTE_WILDCARD = "*";

    private Integer id;
    private PriceModelStrategy type;
    private SortedMap<String, String> attributes = new TreeMap<>();
    private BigDecimal rate;
    private CurrencyDTO currency;

    // price model chaining
    private PriceModelDTO next;

    public PriceModelDTO() {
    }

    public PriceModelDTO(PriceModelStrategy type, BigDecimal rate,
            CurrencyDTO currency) {
        this.type = type;
        this.rate = rate;
        this.currency = currency;
    }

    public PriceModelDTO(PriceModelWS ws, CurrencyDTO currency) {
        setId(ws.getId());
        setType(PriceModelStrategy.valueOf(ws.getType()));
        setAttributes(new TreeMap<String, String>(ws.getAttributes()));
        setRate(ws.getRateAsDecimal());
        setCurrency(currency);
    }

    /**
     * Copy constructor.
     *
     * @param model
     *            model to copy
     */
    public PriceModelDTO(PriceModelDTO model) {
        this.id = model.getId();
        this.type = model.getType();
        this.attributes = new TreeMap<>(model.getAttributes());
        this.rate = model.getRate();
        this.currency = model.getCurrency();

        if (model.getNext() != null) {
            this.next = new PriceModelDTO(model.getNext());
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "price_model_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", nullable = false, length = 25)
    public PriceModelStrategy getType() {
        return type;
    }

    public void setType(PriceModelStrategy type) {
        this.type = type;
    }

    @Transient
    public PricingStrategy getStrategy() {
        return getType() != null ? getType().getStrategy() : null;
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @JoinTable(name = "price_model_attribute", joinColumns = @JoinColumn(name = "price_model_id"))
    @MapKeyColumn(name = "attribute_name", nullable = true)
    @Column(name = "attribute_value", nullable = true, length = 255)
    @SortComparator(value = AttributesValuesComparator.class)
    @Fetch(FetchMode.SELECT)
    public SortedMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(SortedMap<String, String> attributes) {
        this.attributes = attributes;
        setAttributeWildcards();
    }

    /**
     * Sets the given attribute. If the attribute is null, it will be persisted
     * as a wildcard "*".
     *
     * @param name
     *            attribute name
     * @param value
     *            attribute value
     */
    public void addAttribute(String name, String value) {
        this.attributes.put(name, (value != null ? value : ATTRIBUTE_WILDCARD));
    }

    /**
     * Replaces null values in the attribute list with a wildcard character.
     * Null values cannot be persisted using the @CollectionOfElements, and make
     * for uglier 'optional' attribute queries.
     */
    public void setAttributeWildcards() {
        if (getAttributes() != null && !getAttributes().isEmpty()) {
            for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
                if (entry.getValue() == null) {
                    entry.setValue(ATTRIBUTE_WILDCARD);
                }
            }
        }
    }

    /**
     * Returns the pricing rate. If the strategy type defines an overriding
     * rate, the strategy rate will be returned.
     *
     * @return pricing rate.
     */
    @Column(name = "rate", nullable = true, precision = 10, scale = 22)
    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = true)
    public CurrencyDTO getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyDTO currency) {
        this.currency = currency;
    }

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "next_model_id", nullable = true)
    public PriceModelDTO getNext() {
        return next;
    }

    public void setNext(PriceModelDTO next) {
        this.next = next;
    }

    /**
     * Applies this pricing to the given PricingResult.
     *
     * This method will automatically convert the calculated price to the
     * currency of the given PricingResult if the set currencies differ.
     *
     * @see com.sapienter.jbilling.server.pricing.strategy.PricingStrategy
     * @param pricingOrder
     *            target order for this pricing request (may be null)
     * @param quantity
     *            quantity of item being priced
     * @param result
     *            pricing result to apply pricing to
     * @param usage
     *            total item usage for this billing period
     * @param singlePurchase
     *            true if pricing a single purchase/addition to an order, false
     *            if pricing a quantity that already exists on the pricingOrder.
     * @param pricingDate
     *            pricing date
     */
    @Transient
    public void applyTo(OrderDTO pricingOrder, OrderLineDTO orderLine,
            BigDecimal quantity, PricingResult result,
            List<PricingField> fields, Usage usage, boolean singlePurchase,
            Date pricingDate) {

        OrderDTO currentOrder = null != orderLine
                && null != orderLine.getPurchaseOrder() ? orderLine
                        .getPurchaseOrder() : pricingOrder;
                        // check that FUP is not used for ZERO pricing products
                        if (null != quantity && null != this
                                && !this.getType().equals(PriceModelStrategy.ZERO)) {

                            if (null == result.getQuantity()) {
                                result.setQuantity(quantity);
                            }

                            logger.debug("PriceModelDTO.applyTo orderLine: {}", orderLine);
                            if (null != orderLine) {
                                // before applying pricing, apply the free usage pools for this
                                // customer
                                if (null == orderLine.getMediatedQuantity()) {
                                    orderLine.setMediatedQuantity(BigDecimal.ZERO);
                                }
                                if (orderLine.isMediated()) {
                                    if (orderLine.getMediatedQuantity().compareTo(
                                            BigDecimal.ZERO) > 0
                                            && !orderLine.getFreeUsagePoolQuantity().equals(
                                                    quantity)) {
                                        applyFreeUsagePools(currentOrder, orderLine, usage,
                                                result);
                                    }
                                } else {
                                    logger.debug("Before applyFreeUsagePools: {}", usage);
                                    applyFreeUsagePools(currentOrder, orderLine, usage, result);
                                }

                            }
                            if (null != usage) {
                                usage.setFreeUsageQuantity(null != orderLine
                                        && null != currentOrder ? currentOrder
                                                .getFreeUsagePoolsTotalQuantity() : BigDecimal.ZERO);
                            }
                            result.setFreeUsageQuantity(null != orderLine
                                    && null != currentOrder ? currentOrder
                                            .getFreeUsagePoolsTotalQuantity() : BigDecimal.ZERO);

                            quantity = result.getQuantity();
                        }

                        // To handle Mediation and UI Scenario of FUP for Volume pricing.
                        if (null != orderLine && orderLine.hasOrderLineUsagePools()
                                && this.getType().equals(PriceModelStrategy.VOLUME_PRICING)
                                && usage != null) {
                            if (orderLine.isMediated()) {
                                usage.setQuantity(quantity);
                            } else {
                                usage.setQuantity(usage.getQuantity().subtract(
                                        result.getFreeUsageQuantity()));
                            }
                        }
                        this.getType()
                        .getStrategy()
                        .applyTo(pricingOrder, result, fields, this, quantity, usage,
                                singlePurchase, orderLine);

                        // convert currency if necessary
                        if (result.getUserId() != null && result.getCurrencyId() != null
                                && result.getPrice() != null && this.getCurrency() != null
                                && this.getCurrency().getId() != result.getCurrencyId()
                                && !result.isPerCurrencyRateCard()) {

                            Integer entityId = new UserBL().getEntityId(result.getUserId());
                            if (pricingDate == null) {
                                pricingDate = TimezoneHelper.companyCurrentDate(entityId);
                            }

                            // pricingDate will be equal to event_date when called from
                            // Mediation
                            final BigDecimal converted = result.isPercentage() ? result
                                    .getPrice() : new CurrencyBL().convert(this.getCurrency()
                                            .getId(), result.getCurrencyId(), result.getPrice(),
                                            pricingDate, entityId);

                                    logger.debug("price: {} ", converted);
                                    result.setPrice(converted);
                        }
    }

    private class FreeUsageQuantityCalculator {
        private BigDecimal quantity;
        private BigDecimal otherPoolsUtilizedFreeQuantity;
        private PricingResult result;

        public FreeUsageQuantityCalculator(
                BigDecimal otherPoolsUtilizedFreeQuantity, PricingResult result) {
            this.otherPoolsUtilizedFreeQuantity = otherPoolsUtilizedFreeQuantity;
            this.result = result;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public BigDecimal calculateFreeUsageQuantity(
                BigDecimal mediatedQuantity, BigDecimal availableFreeQuantity,
                PricingResult result) {
            if (mediatedQuantity.compareTo(availableFreeQuantity) <= 0) {
                mediatedQuantity = BigDecimal.ZERO;
            } else {
                mediatedQuantity = mediatedQuantity
                        .subtract(availableFreeQuantity);
            }
            return result.getQuantity().subtract(mediatedQuantity);
        }

        public BigDecimal getFreeUsageQuantity(BigDecimal quantity,
                BigDecimal availableFreeQuantity) {
            BigDecimal mediatedQuantity = new BigDecimal(quantity.toString());
            BigDecimal freeUsageQuantity = calculateFreeUsageQuantity(
                    mediatedQuantity, availableFreeQuantity, result);
            this.quantity = quantity.subtract(freeUsageQuantity);
            if (otherPoolsUtilizedFreeQuantity != null) {
                if (otherPoolsUtilizedFreeQuantity.compareTo(quantity) == 0) {
                    freeUsageQuantity = BigDecimal.ZERO;
                    this.quantity = BigDecimal.ZERO;
                } else if (quantity.compareTo(otherPoolsUtilizedFreeQuantity) > 0) {
                    BigDecimal originalFreeUsageQuantity = quantity
                            .subtract(otherPoolsUtilizedFreeQuantity);
                    if (originalFreeUsageQuantity
                            .compareTo(availableFreeQuantity) > 0) {
                        freeUsageQuantity = availableFreeQuantity;
                        this.quantity = originalFreeUsageQuantity
                                .subtract(availableFreeQuantity);
                    } else {
                        freeUsageQuantity = originalFreeUsageQuantity;
                        this.quantity = BigDecimal.ZERO;
                    }
                }
            }
            return freeUsageQuantity;
        }
    }

    public Integer getFreeUsagePoolByOrderLine(
            Map<Integer, Integer> utilizedFreeUsagePoolAndOrderLineMap,
            Integer orderLineId) {
        Integer freeUsagePoolId = null;
        for (Entry<Integer, Integer> entry : utilizedFreeUsagePoolAndOrderLineMap
                .entrySet()) {
            if (entry.getValue().intValue() == orderLineId.intValue()) {
                freeUsagePoolId = entry.getKey();
                break;
            }
        }
        return freeUsagePoolId;
    }

    @Transient
    private void applyFreeUsagePools(OrderDTO pricingOrder,
            OrderLineDTO orderLine, Usage usage, PricingResult result) {

        BigDecimal quantity = result.getQuantity();
        UserDTO user = null;
        if (null != usage) {
            user = new UserDAS().find(usage.getUserId());
        } else {
            // usage will be null in case of pricing strategies that are not
            // usage based
            if (null != result) {
                user = new UserDAS().find(result.getUserId());
            }
        }

        if (null == user) {
            // if user is still not found, lets not apply free usage pools
            logger.debug("Cannot apply free usage pool, no user found.");
            return;
        } else {
            logger.debug("User Id: {}", user.getId());
        }

        CustomerDTO customer = user.getCustomer();
        logger.debug("Customer Id: {} ", customer.getId());

        CustomerUsagePoolBL bl = new CustomerUsagePoolBL();
        List<CustomerUsagePoolDTO> freeUsagePools = excludeExpiredCustomerUsagePools(bl
                .getCustomerUsagePoolsByCustomerId(customer.getId()));

        if (CollectionUtils.isNotEmpty(freeUsagePools)) {
            Map<Integer, BigDecimal> currentOLUPReleasedQuantityMap = new HashMap<>();
            if (freeUsagePools.size() > 1) {
                if (orderLine.getId() > 0 && !orderLine.isMediated()) {
                    OrderLineDTO orderLineDto = new OrderLineDAS()
                    .find(orderLine.getId());
                    if (quantity.setScale(2, BigDecimal.ROUND_HALF_UP)
                            .compareTo(
                                    orderLineDto.getQuantity().setScale(2,
                                            BigDecimal.ROUND_HALF_UP)) != 0) {
                        quantity = result.getQuantity().subtract(
                                orderLineDto.getQuantity());
                        if (quantity.compareTo(BigDecimal.ZERO) < 0) {
                            for (OrderLineUsagePoolDTO orderLineUsagePool : orderLine
                                    .getOrderLineUsagePools()) {
                                for (CustomerUsagePoolDTO freeUsagePool : freeUsagePools) {
                                    if (orderLineUsagePool
                                            .getCustomerUsagePool().getId() == freeUsagePool
                                            .getId()) {
                                        BigDecimal releaseQuantity = currentOLUPReleasedQuantityMap
                                                .getOrDefault(
                                                        freeUsagePool.getId(),
                                                        BigDecimal.ZERO);
                                        currentOLUPReleasedQuantityMap.put(
                                                freeUsagePool.getId(),
                                                orderLineUsagePool
                                                .getQuantity()
                                                .add(releaseQuantity));
                                        break;
                                    }
                                }
                            }

                            orderLine.getOrderLineUsagePools().clear();
                            pricingOrder.getLineById(orderLine.getId())
                            .getOrderLineUsagePools().clear();
                            quantity = result.getQuantity();
                        }
                        result.setQuantity(quantity);
                    } else {
                        if (orderLine.hasOrderLineUsagePools()) {
                            quantity = BigDecimal.ZERO;
                            result.setQuantity(quantity);
                        }
                    }
                }
                // sort based on preference or created date if preference is
                // same
                Collections
                .sort(freeUsagePools,
                        CustomerUsagePoolDTO.CustomerUsagePoolsByPrecedenceOrCreatedDateComparator);
            }

            ItemDAS itemDas = new ItemDAS();
            logger.debug("Before for ....");

            for (CustomerUsagePoolDTO freeUsagePool : freeUsagePools) {

                if(PreferenceBL.getPreferenceValueAsIntegerOrZero(user.getEntity().getId(),
                        Constants.PREFERENCE_USE_ASSET_LINKED_FREE_USAGE_POOLS_ONLY) == 1) {
                    logger.debug("Asset Linked Free Usage Pools Only is enabled");
                    CustomerUsagePoolBL customerUsagePoolBl = new CustomerUsagePoolBL(freeUsagePool.getId());
                    // Use the pool that the asset is subscribed to use for the service plan
                    // If the asset is not subscribed to use this pool through its own service plan, try the next one
                    if (orderLine.isMediated() &&
                            !customerUsagePoolBl.isAssetSubscribedToCustomerUsagePool(user.getId(),
                                    orderLine.getCallIdentifier())) {
                        continue;
                    }
                }
                if (null != orderLine.getPurchaseOrder().getActiveSince()
                        && freeUsagePool
                        .isActive(new Date(orderLine.getPurchaseOrder()
                                .getActiveSince().getTime()))) {
                    if (quantity.compareTo(BigDecimal.ZERO) > 0) {
                        logger.debug("freeUsagePool quantity: {} ",
                                freeUsagePool.getQuantity());
                        BigDecimal releasedFreeUsageQuantity = currentOLUPReleasedQuantityMap
                                .getOrDefault(freeUsagePool.getId(),
                                        BigDecimal.ZERO);
                        if (null != orderLine && orderLine.getDeleted() == 1) {
                            // the order line is being removed, we need to
                            // release any free usage
                            // from this order line for usage by other lines, or
                            // release it back to pool
                            releasedFreeUsageQuantity = orderLine
                                    .getFreeUsagePoolQuantity();
                        }
                        logger.debug("releasedFreeUsageQuantity: {}",
                                releasedFreeUsageQuantity);
                        if (freeUsagePool.getQuantity()
                                .add(releasedFreeUsageQuantity)
                                .compareTo(BigDecimal.ZERO) > 0
                                && freeUsagePool.getAllItems().contains(
                                        itemDas.find(result.getItemId()))) {

                            logger.debug("Inside if .....");
                            BigDecimal persistedFreeUsageQuantity = BigDecimal.ZERO;

                            OrderLineDTO currentOrderLine = null;
                            if (null != pricingOrder
                                    && null != pricingOrder.getId()
                                    && pricingOrder.getId() > 0) {
                                OrderDTO order = new OrderDAS()
                                .find(pricingOrder.getId());
                                persistedFreeUsageQuantity = order
                                        .getFreeUsagePoolsTotalQuantity(
                                                freeUsagePool.getId())
                                                .subtract(
                                                        currentOLUPReleasedQuantityMap.getOrDefault(
                                                                freeUsagePool.getId(),
                                                                BigDecimal.ZERO));
                                for (OrderLineDTO line : order.getLines()) {
                                    if (line.getId() == orderLine.getId()) {
                                        currentOrderLine = line;
                                        break;
                                    }
                                }
                            }

                            logger.debug("currentOrderLine: {}",
                                    currentOrderLine);
                            logger.debug("pricingOrder:::::::::::::{} ",
                                    pricingOrder);
                            logger.debug("persistedFreeUsageQuantity :::::{} ",
                                    persistedFreeUsageQuantity);
                            BigDecimal nonPersistedFreeUsageQuantity = pricingOrder
                                    .getFreeUsagePoolsTotalQuantity(
                                            freeUsagePool.getId()).subtract(
                                                    persistedFreeUsageQuantity);
                            if (nonPersistedFreeUsageQuantity
                                    .compareTo(BigDecimal.ZERO) == 0
                                    && result.isChained()) {
                                // check if order line has ol usage pools
                                // populated, scnario for chained pricing
                                nonPersistedFreeUsageQuantity = orderLine
                                        .getFreeUsagePoolQuantity(freeUsagePool
                                                .getId());
                            }
                            if (nonPersistedFreeUsageQuantity
                                    .compareTo(BigDecimal.ZERO) < 0) {
                                nonPersistedFreeUsageQuantity = BigDecimal.ZERO;
                            }

                            logger.debug("freeUsagePool.getQuantity() ::: {}",
                                    freeUsagePool.getQuantity());
                            logger.debug("nonPersistedFreeUsageQuantity :::{}",
                                    nonPersistedFreeUsageQuantity);
                            BigDecimal availableFreeQuantity;
                            /*
                             * To handle Create order with quantity is less than
                             * Free usage pool quantity & Edit order line, save
                             * with increase in quantity scenario.
                             */
                            if (orderLine.getId() > 0
                                    || orderLine.hasOrderLineUsagePools()) {
                                if (freeUsagePools.size() > 1
                                        && !orderLine.isMediated()) {
                                    availableFreeQuantity = freeUsagePool
                                            .getQuantity()
                                            .subtract(
                                                    nonPersistedFreeUsageQuantity)
                                                    .add(currentOLUPReleasedQuantityMap
                                                            .getOrDefault(freeUsagePool
                                                                    .getId(),
                                                                    BigDecimal.ZERO));
                                } else {
                                    availableFreeQuantity = freeUsagePool
                                            .getQuantity()
                                            .add(orderLine
                                                    .getFreeUsagePoolQuantity(freeUsagePool
                                                            .getId()))
                                                            .subtract(
                                                                    nonPersistedFreeUsageQuantity);
                                }
                            } else {
                                availableFreeQuantity = freeUsagePool
                                        .getQuantity().subtract(
                                                nonPersistedFreeUsageQuantity);
                            }
                            if (availableFreeQuantity
                                    .compareTo(BigDecimal.ZERO) < 0
                                    && !result.isChained()) {
                                availableFreeQuantity = freeUsagePool
                                        .getQuantity();
                            }

                            logger.debug("availableFreeQuantity :::::{} ",
                                    availableFreeQuantity);
                            BigDecimal otherPoolsUtilizedFreeQuantity = orderLine
                                    .isMediated() ? getOtherPoolUtilizedQuantity(
                                            orderLine, freeUsagePool.getId()) : null;
                                            FreeUsageQuantityCalculator freeUsageQuantityCalculator = new FreeUsageQuantityCalculator(
                                                    otherPoolsUtilizedFreeQuantity, result);
                                            BigDecimal freeUsageQuantity = freeUsageQuantityCalculator
                                                    .getFreeUsageQuantity(quantity,
                                                            availableFreeQuantity);
                                            quantity = freeUsageQuantityCalculator
                                                    .getQuantity();

                                            logger.debug("freeUsageQuantity: {} ",
                                                    freeUsageQuantity);
                                            logger.debug("result.getQuantity(): {} ",
                                                    result.getQuantity());
                                            logger.debug("quantity: {} ", quantity);

                                            if (freeUsageQuantity.compareTo(BigDecimal.ZERO) > 0
                                                    && orderLine.isMediated()
                                                    || ((null != orderLine && orderLine
                                                    .getDeleted() == 1) || (null == currentOrderLine || (null != currentOrderLine)))) {
                                                // create or update order line usage pools
                                                // association
                                                logger.debug("create or update order line usage pools association");
                                                setOrderLineUsagePoolMap(orderLine,
                                                        freeUsagePool, freeUsageQuantity,
                                                        result);
                                            }
                        } else {
                            if (freeUsagePool.getAllItems().contains(
                                    itemDas.find(result.getItemId()))) {
                                BigDecimal freeUsageQuantity;
                                if (quantity.compareTo(orderLine
                                        .getFreeUsagePoolQuantity()) < 0
                                        && (freeUsagePool.getQuantity()
                                                .compareTo(BigDecimal.ZERO) > 0 || freeUsagePools
                                                .size() == 1)) {
                                    freeUsageQuantity = quantity;
                                    setOrderLineUsagePoolMap(orderLine,
                                            freeUsagePool, freeUsageQuantity,
                                            result);
                                }
                            }
                        }

                    }
                    result.setQuantity(quantity);
                }
            }
        }
    }

    /**
     * When a free usage pool gets used, store the free quantity used in order
     * line usage pool association map.
     *
     * @param orderLine
     * @param freeUsagePool
     * @param freeUsageQuantity
     */
    @Transient
    private void setOrderLineUsagePoolMap(OrderLineDTO orderLine,
            CustomerUsagePoolDTO freeUsagePool, BigDecimal freeUsageQuantity,
            PricingResult result) {

        if (orderLine.getDeleted() == 0) {
            logger.debug("OrderLine ::::::: {} ", orderLine);
            ItemDAS itemDas = new ItemDAS();
            CustomerDTO customer = freeUsagePool.getCustomer();
            List<CustomerUsagePoolDTO> freeUsagePools = customer
                    .getCustomerUsagePools();
            Date effectiveDate = orderLine.getPurchaseOrder().getActiveSince();
            // exclude expired customer usage pools
            freeUsagePools = excludeExpiredCustomerUsagePools(freeUsagePools);

            if (orderLine.hasOrderLineUsagePools()) {
                Boolean usagePoolExist = false;
                for (OrderLineUsagePoolDTO orderLineUsagePool : orderLine
                        .getOrderLineUsagePools()) {
                    logger.debug("freeUsageQuantity ::::::: {}",
                            freeUsageQuantity);
                    // set the quantity for free usage, matching by customer
                    // usage pool id.
                    if (orderLineUsagePool.hasCustomerUsagePool()
                            && orderLineUsagePool.getCustomerUsagePool()
                            .getId() == freeUsagePool.getId()) {
                        // for multiple Customer Usage pools
                        if (freeUsagePools.size() > 1
                                && !orderLine.isMediated()) {
                            orderLineUsagePool.setQuantity(orderLineUsagePool
                                    .getQuantity().add(freeUsageQuantity));
                        } else {
                            orderLineUsagePool.setQuantity(freeUsageQuantity);
                        }
                        usagePoolExist = true;
                        break;
                    }
                }
                if (!usagePoolExist
                        && freeUsagePool.getAllItems().contains(
                                itemDas.find(result.getItemId()))
                                && freeUsageQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    Set<OrderLineUsagePoolDTO> olUsagePools = orderLine
                            .hasOrderLineUsagePools() ? orderLine
                                    .getOrderLineUsagePools() : new HashSet<>();
                                    olUsagePools.add(new OrderLineUsagePoolDTO(0, orderLine,
                                            freeUsageQuantity, freeUsagePool, effectiveDate));
                                    orderLine.getOrderLineUsagePools().addAll(olUsagePools);
                }
            } else {
                if (freeUsageQuantity.compareTo(BigDecimal.ZERO) > 0) {
                    Set<OrderLineUsagePoolDTO> olUsagePools = orderLine
                            .hasOrderLineUsagePools() ? orderLine
                                    .getOrderLineUsagePools() : new HashSet<>();
                                    olUsagePools.add(new OrderLineUsagePoolDTO(0, orderLine,
                                            freeUsageQuantity, freeUsagePool, effectiveDate));
                                    orderLine.getOrderLineUsagePools().addAll(olUsagePools);
                }
            }
        }
    }

    /**
     * exclude expired customer usage pools (cycle end date is 1970-01-01)
     *
     * @param customerUsagePoolDtos
     * @return
     */
    private List<CustomerUsagePoolDTO> excludeExpiredCustomerUsagePools(
            List<CustomerUsagePoolDTO> customerUsagePoolDtos) {
        List<CustomerUsagePoolDTO> customerUsagePools = new ArrayList<>();
        for (CustomerUsagePoolDTO customerUsagePool : customerUsagePoolDtos) {
            if (customerUsagePool.getCycleEndDate().after(Util.getEpochDate())) {
                customerUsagePools.add(customerUsagePool);
            }
        }
        return customerUsagePools;
    }

    private BigDecimal getOtherPoolUtilizedQuantity(OrderLineDTO line, int fupId) {
        BigDecimal quantity = BigDecimal.ZERO;
        if (line.getDeleted() == 0) {
            for (OrderLineUsagePoolDTO olUsagePool : line
                    .getOrderLineUsagePools()) {
                if (olUsagePool.getCustomerUsagePool().getId() != fupId) {
                    quantity = quantity.add(olUsagePool.getQuantity());
                }
            }
        }
        return quantity.compareTo(BigDecimal.ZERO) > 0 ? quantity : null;
    }

    public boolean equalsModel(PriceModelDTO that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }

        if (attributes != null ? !attributes.equals(that.attributes)
                : that.attributes != null) {
            return false;
        }
        if (currency != null ? !currency.equals(that.currency)
                : that.currency != null) {
            return false;
        }
        if (rate != null ? !rate.equals(that.rate) : that.rate != null) {
            return false;
        }
        return type != that.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PriceModelDTO that = (PriceModelDTO) o;

        if (attributes != null ? !attributes.equals(that.attributes)
                : that.attributes != null) {
            return false;
        }
        if (currency != null ? !currency.equals(that.currency)
                : that.currency != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }
        if (rate != null ? !rate.equals(that.rate) : that.rate != null) {
            return false;
        }
        return type != that.type;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (attributes != null ? attributes.hashCode() : 0);
        result = 31 * result + (rate != null ? rate.hashCode() : 0);
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PriceModelDTO{" + "id=" + id + ", type=" + type
                + ", attributes=" + attributes + ", rate=" + rate
                + ", currencyId="
                + (currency != null ? currency.getId() : null) + ", next="
                + next + '}';
    }

    public String getAuditKey(Serializable id) {
        // todo: needs some back-references so that we can log the owning entity
        // id and item id (or whatever its attached to)
        return id.toString();
    }

    public static final class AttributesValuesComparator implements
    Comparator<String>, Serializable {

        @Override
        public int compare(String o1, String o2) {
            if (NumberUtils.isNumber(o1) && NumberUtils.isNumber(o2)) {
                return new BigDecimal(o1).compareTo(new BigDecimal(o2));
            }

            return o1.compareTo(o2);
        }
    }
}