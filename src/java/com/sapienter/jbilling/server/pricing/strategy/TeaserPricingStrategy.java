package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.tasks.PricingResult;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.Usage;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderChangeTypeDAS;
import com.sapienter.jbilling.server.pricing.db.ChainPosition;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeDefinition;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.MainSubscriptionDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import com.sapienter.jbilling.server.util.time.ProRatePeriodCalculator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.math.NumberUtils;

import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.DECIMAL;
import static com.sapienter.jbilling.server.pricing.util.AttributeDefinition.Type.STRING;

/**
 * The teaser rate pricing strategy allows the user to specify different prices based on the time since
 * they enrolled. The period unit is set from the order or the user can choose one.
 *
 * The strategy to store the rates is
 *   [period] -> [rate]
 *   
 * Example:
 * 
 * Period: Monthly
 * 1 -> 50
 * 3 -> 55
 * 6 -> 60
 * 
 * The strategy is inclusive. Thus, from the sixth month, the rate of the item is 60.
 *
 * @author Gerhard Maree
 * @since 22-12-2015
 */
public class TeaserPricingStrategy extends AbstractPricingStrategy implements CycleStrategy {

    public enum UseOrderPeriod {
        YES, NO;

        public static boolean isUseOrderPeriod(String value) {
            try {
                return YES.equals(valueOf(value));
            } catch (IllegalArgumentException iae) {
                LOG.debug("By default the price should use the order period");
                return true;
            }
        }
    }

    private static final List<OrderChangeDTO> EMPTY_ORDER_CHANGES = new ArrayList<>();
    public static final String PERIOD = "period";
    public static final String USE_ORDER_PERIOD = "use_order_period";
    public static final String FIRST_PERIOD = "1";

    private List<BigDecimal> endPeriods;
    private OrderChangeStatusDAS orderChangeStatusDAS;
    private OrderChangeStatusDTO orderChangeStatus;
    private OrderChangeTypeDTO orderChangeType;

    public TeaserPricingStrategy() {
        setAttributeDefinitions(
                new AttributeDefinition(USE_ORDER_PERIOD, STRING, false),
                new AttributeDefinition(PERIOD, STRING, false),
                new AttributeDefinition(FIRST_PERIOD, DECIMAL, true)
        );

        setChainPositions(ChainPosition.START);
        setRequiresUsage(false);
        setUsesDynamicAttributes(true);
        setVariableUsagePricing(false);
    }

    @Override
    public void applyTo(OrderDTO pricingOrder, PricingResult result, List<PricingField> fields, PriceModelDTO planPrice,
                        BigDecimal quantity, Usage usage, boolean singlePurchase, OrderLineDTO orderLineDTO) {
        result.setPrice(AttributeUtils.parseDecimal(planPrice.getAttributes().get(FIRST_PERIOD)));
    }

    @Override
    public void validate(PriceModelDTO priceModel) {
        if (!TeaserPricingStrategy.UseOrderPeriod.isUseOrderPeriod(priceModel.getAttributes().get(USE_ORDER_PERIOD)) &&
                (priceModel.getAttributes().get(PERIOD) == null || priceModel.getAttributes().get(PERIOD).isEmpty())) {
            throw new SessionInternalError("At least one price must be defined",
                    new String[] {"bean.TeaserPricingStrategy.strategy.validation.error.select.at.one.period"});
        }

        SortedMap<BigDecimal, BigDecimal> tiers = getTiers(priceModel.getAttributes());
        if (tiers.isEmpty()) {
            throw new SessionInternalError("At least one price must be defined",
                    new String[] {"bean.TeaserPricingStrategy.strategy.validation.error.at.least.one.period"});
        }

        //Check that all the attributes added dynamically been numbers
        if ((tiers.size() + 2) != priceModel.getAttributes().size()) {
            throw new SessionInternalError("All the attributes should be a number",
                    new String[] {"bean.TeaserPricingStrategy.strategy.validation.error.all.attr.should.be.numbers"});
        }

        boolean containNegativeNumbers = tiers.entrySet()
                                              .stream()
                                              .anyMatch(entry -> entry.getKey().signum() < 0);

        if (containNegativeNumbers) {
            throw new SessionInternalError("All the attributes should be a number",
                    new String[] {"bean.TeaserPricingStrategy.strategy.validation.error.all.attr.should.be.positive.numbers"});
        }

        boolean containNonIntegers = tiers.entrySet()
                                          .stream()
                                          .anyMatch(entry -> entry.getKey().scale() > 0 ||
                                                             entry.getKey().stripTrailingZeros().scale() > 0);

        if (containNonIntegers) {
            throw new SessionInternalError("All the attributes should be a number",
                    new String[] {"bean.TeaserPricingStrategy.strategy.validation.error.all.attr.should.be.integer.numbers"});
        }
    }

    private static SortedMap<BigDecimal, BigDecimal> getTiers(Map<String, String> attributes) {
        return attributes.entrySet()
                         .stream()
                         .filter(map -> NumberUtils.isNumber(map.getKey()) && NumberUtils.isNumber(map.getValue()))
                         .collect(Collectors.toMap(
                                 e -> AttributeUtils.parseDecimal(e.getKey()),
                                 e -> AttributeUtils.parseDecimal(e.getValue()),
                                 (v1, v2) -> { throw new IllegalStateException(); },
                                 TreeMap::new
                         ));
    }

    private List<TeaserPricingStrategy.CyclePrice> getCycles(Map<String, String> attributes, Date startDate,
                                                             Date proRateDate, OrderPeriodDTO orderPeriodDTO){
        return getTiers(attributes).entrySet()
                                   .stream()
                                   .map(entry -> new CyclePrice(entry.getKey(),
                                                                entry.getValue(),
                                                                startDate,
                                                                proRateDate,
                                                                orderPeriodDTO))
                                   .collect(Collectors.toList());
    }

    public List<OrderPeriodDTO> getOrderPeriods(Integer entityId) {
        return new OrderPeriodDAS().getOrderPeriods(entityId);
    }

    private OrderPeriodDTO getPeriod(PriceModelDTO planPrice, OrderDTO pricingOrder) {
        return TeaserPricingStrategy.UseOrderPeriod.isUseOrderPeriod(planPrice.getAttributes().get(USE_ORDER_PERIOD)) ? pricingOrder.getOrderPeriod() :
                new OrderPeriodDAS().find(AttributeUtils.getInteger(planPrice.getAttributes(), PERIOD)) ;
    }

    private static Date addPeriodToDate(Date startDate, BigDecimal period, OrderPeriodDTO orderPeriodDTO) {
        if (period == null) {
            return null;
        }

        LocalDate localDate = DateConvertUtils.asLocalDate(startDate);
        return DateConvertUtils.asUtilDate(PeriodUnit.valueOfPeriodUnit(localDate.getDayOfMonth(), orderPeriodDTO.getPeriodUnit().getId())
                               .addTo(localDate, (period.longValue() - 1) * orderPeriodDTO.getValue()));
    }

    private static boolean isValidDate(OrderDTO order, BigDecimal month, OrderPeriodDTO orderPeriodDTO) {
        return order.getActiveUntil() == null ||
                addPeriodToDate(order.getActiveSince(), month, orderPeriodDTO).before(order.getActiveUntil());
    }

    private class CyclePrice {
        private BigDecimal fromCycle;
        private BigDecimal rate;
        private Date startDate;
        private Date endDate;

        public CyclePrice(BigDecimal fromCycle, BigDecimal rate, Date startDate,
                          Date proRateDate, OrderPeriodDTO orderPeriodDTO) {
            this.fromCycle = fromCycle;
            this.rate = rate;
            this.startDate = getCorrectDate(fromCycle, startDate, proRateDate, orderPeriodDTO);
            this.endDate = getCorrectDate(endPeriods.remove(0), startDate, proRateDate,  orderPeriodDTO);
        }

        public BigDecimal getFromCycle() { return fromCycle; }

        public BigDecimal getRate() { return rate; }

        public Date getStartDate() { return startDate; }

        public Date getEndDate() { return endDate; }

        @Override
        public String toString() { return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE); }

        private Date getCorrectDate(BigDecimal fromCycle, Date startDate, Date proRateDate, OrderPeriodDTO orderPeriodDTO) {
            if (fromCycle != null && BigDecimal.ONE.compareTo(fromCycle) == 0) {
                return addPeriodToDate(startDate, fromCycle, orderPeriodDTO);
            } else {
                return addPeriodToDate(proRateDate != null ? proRateDate : startDate, fromCycle, orderPeriodDTO);
            }
        }
    }

    private OrderChangeDTO buildOrderChange(OrderChangeDTO orderChange, TeaserPricingStrategy.CyclePrice cyclePrice) {

        Date currentDate = TimezoneHelper.serverCurrentDate();

        OrderChangeDTO orderChangeDTO = new OrderChangeDTO();
        orderChangeDTO.setQuantity(BigDecimal.ZERO);
        orderChangeDTO.setOrderChangeType(orderChangeType);
        orderChangeDTO.setUser(orderChange.getUser());
        orderChangeDTO.setNextBillableDate(cyclePrice.getStartDate());
        orderChangeDTO.setDescription(orderChange.getDescription());
        orderChangeDTO.setStartDate(cyclePrice.getStartDate());
        orderChangeDTO.setOrder(orderChange.getOrder());
        orderChangeDTO.setItem(orderChange.getItem());
        orderChangeDTO.setPrice(cyclePrice.getRate());
        orderChangeDTO.setOrderLine(orderChange.getOrderLine());
        orderChangeDTO.setEndDate(cyclePrice.getEndDate());
        orderChangeDTO.setAssets(orderChange.getAssets());
        orderChangeDTO.setOrderChangePlanItems(orderChange.getOrderChangePlanItems());
        orderChangeDTO.setCreateDatetime(currentDate);
        orderChangeDTO.setUseItem(0);
        if (!Util.truncateDate(orderChangeDTO.getStartDate()).after(Util.truncateDate(currentDate))) {
            orderChangeDTO.setStatus(orderChange.getStatus());
            orderChangeDTO.setApplicationDate(currentDate);
            orderChangeDTO.setAppliedManually(1);
        } else {
            orderChangeStatus = orderChangeStatusDAS.find(Constants.ORDER_CHANGE_STATUS_PENDING);
            orderChangeDTO.setStatus(orderChangeStatus);
            orderChangeDTO.setAppliedManually(0);
        }

        OrderChangeStatusDTO appliedStatus = orderChangeStatusDAS.findApplyStatus(orderChange.getUser().getCompany().getId());
        orderChangeDTO.setUserAssignedStatus(appliedStatus);

        return orderChangeDTO;
    }

    public List<OrderChangeDTO> generateOrderChangesByCycles(OrderChangeDTO orderChangeDTO, OrderDTO order, PriceModelDTO priceModel) {
        if (order != null && !Constants.ORDER_PERIOD_ONCE.equals(order.getPeriodId())) {
            orderChangeStatusDAS = new OrderChangeStatusDAS();
            orderChangeType = new OrderChangeTypeDAS().find(Constants.ORDER_CHANGE_TYPE_DEFAULT);

            endPeriods = priceModel.getAttributes()
                                   .keySet()
                                   .stream()
                                   .filter(key -> !key.equals(FIRST_PERIOD) && NumberUtils.isNumber(key))
                                   .map(AttributeUtils::parseDecimal)
                                   .collect(Collectors.toList());

            // For the last cycle, the period is infinite
            endPeriods.add(null);

            Integer appliedManually = orderChangeDTO.getAppliedManually();
            Date startDate;
            Date proRateDate = null;
            if (appliedManually != null && appliedManually == 1 && !isPartOfAPlan(orderChangeDTO)) {
                startDate = TimezoneHelper.serverCurrentDate();
            } else if (orderChangeDTO.getOrder().getProrateFlag()) {
                startDate = orderChangeDTO.getStartDate();
                proRateDate = getDateFromMainSubscription(order.getUser().getCustomer().getMainSubscription(), startDate, order.getUser().getCustomer().getNextInvoiceDate());
            } else {
                startDate = orderChangeDTO.getStartDate();
            }

            OrderPeriodDTO orderPeriod = getPeriod(priceModel, order);

            return getCycles(priceModel.getAttributes(),
                             startDate,
                             proRateDate,
                             orderPeriod).stream()
                                         .filter(cyclePrice -> !cyclePrice.getFromCycle().equals(new BigDecimal(TeaserPricingStrategy.FIRST_PERIOD)) &&
                                                               isValidDate(order, cyclePrice.getFromCycle(), orderPeriod))
                                         .map(cycle -> buildOrderChange(orderChangeDTO, cycle))
                                         .collect(Collectors.toList());
        }

        return EMPTY_ORDER_CHANGES;
    }

    private boolean isPartOfAPlan(OrderChangeDTO orderChangeDTO) {
        OrderDTO order = orderChangeDTO.getOrder().getParentOrder() == null ? orderChangeDTO.getOrder() :
                                                                              orderChangeDTO.getOrder().getParentOrder();
        int itemId = orderChangeDTO.getItem().getId();

        if (order != null) {
            OrderLineDTO orderLine = order.getLines().stream()
                                                     .filter(line -> line.getItem().isPlan() &&
                                                                     line.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                                                     .findFirst()
                                                     .orElse(null);

            if (orderLine != null) {
                return orderLine.getItem().getPlans().stream()
                                                     .flatMap(plan -> plan.getPlanItems().stream())
                                                     .anyMatch(planItem -> planItem.getItem().getId() == itemId);
            }
        }

        return false;
    }

    private Date getDateFromMainSubscription(MainSubscriptionDTO mainSubscription, Date startDate, Date nextInvoiceDate) {
        ProRatePeriodCalculator calculator = ProRatePeriodCalculator.valueOfPeriodUnit(mainSubscription.getSubscriptionPeriod()
                                                                                                       .getPeriodUnit()
                                                                                                       .getId());
        LocalDate localStartDate = DateConvertUtils.asLocalDate(startDate);
        LocalDate localDate = calculator.getDate(localStartDate, nextInvoiceDate, mainSubscription);

        while (localDate.isAfter(localStartDate)) {
            localDate = calculator.getNextBeforeDate(localDate, mainSubscription);
        }

        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
