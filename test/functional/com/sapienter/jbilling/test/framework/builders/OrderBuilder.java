package com.sapienter.jbilling.test.framework.builders;

import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEntityType;
import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Created by marco manzi on 21/01/16.
 */
public class OrderBuilder extends AbstractBuilder{

    private String codeForTest;
    private Integer userId;
    private Integer orderPeriodId;
    private List<Integer> productIds = new ArrayList<>();
    private HashMap<Integer, List<Integer>> orderLineChildItemRelations = new HashMap<>();
    private List<OrderLineWS> orderLines = new ArrayList<>();
    private List<DiscountLineWS> discountLines = new ArrayList<>();
    private OrderWS parentOrder;
    private List<OrderWS> childOrders = new ArrayList<>();
    private Date activeSince;
    private Date activeUntil;
    private Date effectiveDate;
    private Integer orderChangeStatusId;
    private Integer minimumPeriod;
    private String cancellationFeeType;
    private Integer cancellationFee;
    private Integer orderStatusId;
    private boolean prorate;
    private int billingTypeId = Constants.ORDER_BILLING_POST_PAID;
    private Integer dueDateUnit;
    private Integer dueDateValue;
    private boolean isMediated = false;

    private OrderBuilder(JbillingAPI api, TestEnvironment testEnvironment) {
        super(api, testEnvironment);
    }

    public static OrderBuilder getBuilder(JbillingAPI api, TestEnvironment testEnvironment){
        return new OrderBuilder(api, testEnvironment);
    }

    public static OrderBuilder getBuilderWithoutEnv() {
        return new OrderBuilder(null, null);
    }

    public OrderBuilder withCodeForTests(String codeForTest) {
        this.codeForTest = codeForTest;
        return this;
    }

    public OrderBuilder withOrderLine(OrderLineWS orderLine){
        orderLines.add(orderLine);
        return this;
    }


    public OrderBuilder withOrderLines(List<OrderLineWS> orderLines) {
        this.orderLines = orderLines;
        return this;

    }

    public OrderBuilder withDiscountLine(DiscountLineWS discountLine)
    {
        discountLines.add(discountLine);
        return this;
    }

    public OrderBuilder withActiveSince(Date activeSince){
        this.activeSince = activeSince;
        return this;
    }

    public OrderBuilder withActiveUntil(Date activeUntil){
        this.activeUntil = activeUntil;
        return this;
    }

    public OrderBuilder withDueDateUnit(Integer dueDateUnit){
        this.dueDateUnit = dueDateUnit;
        return this;
    }

    public OrderBuilder withDueDateValue(Integer dueDateValue){
        this.dueDateValue = dueDateValue;
        return this;
    }

    public OrderBuilder withEffectiveDate(Date effectiveDate) {

        this.effectiveDate = effectiveDate;
        return this;
    }

    public OrderBuilder forUser(Integer userId) {
        this.userId = userId;
        return this;
    }

    public OrderBuilder withPeriod(Integer orderPeriodId) {
        this.orderPeriodId = orderPeriodId;
        return this;
    }

    public OrderBuilder withProducts(Integer... productIds) {
        this.productIds = Arrays.asList(productIds);
        return this;
    }

    public OrderBuilder withParent(OrderWS parentOrder) {
        this.parentOrder = parentOrder;
        return this;
    }

    public OrderBuilder  withOrderStatus(Integer orderStatusId){
        this.orderStatusId = orderStatusId;
        return this;
    }

    public OrderBuilder withOrderChangeStatus(Integer orderChangeStatusId){
        this.orderChangeStatusId = orderChangeStatusId;
        return this;
    }

    public OrderBuilder withChildOrder(OrderWS childOrder){
        childOrders.add(childOrder);
        return this;
    }

    public OrderBuilder withCancelationMinimumPeriod(Integer minimumPeriod){
        this.minimumPeriod = minimumPeriod;
        return this;
    }

    public OrderBuilder withCancellationFeeType(String cancellationFeeType){
        this.cancellationFeeType = cancellationFeeType;
        return this;
    }

    public OrderBuilder withCancellationFee(Integer cancellationFee){
        this.cancellationFee=cancellationFee;
        return this;
    }

    public OrderBuilder withBillingTypeId (int billingTypeId) {
        this.billingTypeId = billingTypeId;
        return this;
    }

    public OrderBuilder withProrate(boolean prorate) {
        this.prorate = prorate;
        return this;
    }

    public OrderBuilder withIsMediated(boolean isMediated) {
        this.isMediated = isMediated;
        return this;
    }

    private Integer getOrCreateOrderChangeApplyStatus(){
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for(OrderChangeStatusWS orderChangeStatus : list){
            if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if(statusId != null){
            return statusId;
        }else{
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(ApplyToOrder.YES);
            newStatus.setDeleted(0);
            newStatus.setOrder(1);
            newStatus.setEntityId(api.getCallerCompanyId());
            newStatus.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    public Integer build() {
        return persistOrder(buildOrder());
    }

    public Integer persistOrder (OrderWS order, OrderChangeWS[] orderChanges) {
        Integer orderId = api.createOrder(order, orderChanges);
        testEnvironment.add(codeForTest, orderId, codeForTest, api, TestEntityType.ORDER);
        return orderId;
    }

    public Integer persistOrder (OrderWS order) {
        Date startDate = null == effectiveDate ? new Date() : effectiveDate;
        if (null == orderChangeStatusId) {
            orderChangeStatusId = getOrCreateOrderChangeApplyStatus();
        }
        OrderChangeWS[] orderChanges = buildFromOrder(order, orderChangeStatusId, startDate);
        Integer orderId = api.createOrder(order, orderChanges);
        testEnvironment.add(codeForTest, orderId, codeForTest, api, TestEntityType.ORDER);
        return orderId;
    }

    public OrderWS buildOrder() {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingTypeId);
        order.setProrateFlag(prorate);
        order.setPeriod(orderPeriodId);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        order.setActiveSince(null != activeSince ? activeSince : new Date());
        order.setActiveUntil(activeUntil);
        order.setCancellationFee(cancellationFee);
        order.setCancellationMinimumPeriod(minimumPeriod);
        order.setCancellationFeeType(cancellationFeeType);
        order.setStatusId(orderStatusId);
        order.setOrderLines((!orderLines.isEmpty()) ? orderLines.toArray(new OrderLineWS[orderLines.size()]) :
                createOrderLinesFor(null));
        order.setParentOrder(parentOrder);
        order.setChildOrders(!childOrders.isEmpty() ? childOrders.toArray(new OrderWS[childOrders.size()]) :
                null);
        order.setDiscountLines(!discountLines.isEmpty()? discountLines.toArray(new DiscountLineWS[discountLines.size()])
                :null);
        order.setDueDateUnitId(dueDateUnit);
        order.setDueDateValue(dueDateValue);
        order.setIsMediated(isMediated);
        return order;
    }

    private OrderLineWS[] createOrderLinesFor(Integer parentItemId) {
        List<OrderLineWS> orderLineWSes = new ArrayList<>();
        List<Integer> itemsToUse = productIds;
        if (parentItemId != null) {
            itemsToUse = orderLineChildItemRelations.get(parentItemId);
        }
        if (itemsToUse != null) {
            for (Integer productId: itemsToUse) {
                OrderLineWS orderLine = new OrderLineWS();
                orderLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
                orderLine.setDescription("Order line for tests: " + orderLine.hashCode());
                orderLine.setUseItem(true);
                orderLine.setItemId(productId);
                orderLine.setQuantity(1);
                orderLine.setPrice(BigDecimal.ONE);
                orderLine.setAmount(BigDecimal.ONE);
                orderLine.setChildLines(createOrderLinesFor(productId));
                orderLineWSes.add(orderLine);
            }

        }
        return orderLineWSes.toArray(new OrderLineWS[0]);
    }

    public OrderBuilder withChildLines(HashMap<Integer, List<Integer>> orderLineWithChildLinesRelations) {
        orderLineChildItemRelations.putAll(orderLineWithChildLinesRelations);
        return this;
    }


    public OrderLineBuilder orderLine(){
        return new OrderLineBuilder();
    }

    public class OrderLineBuilder {

        private int orderLineType = Constants.ORDER_LINE_TYPE_ITEM; // default
        private String description = "TestDesc";
        private boolean useItem = true;
        private Integer itemId;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal amount = BigDecimal.ONE;
        private OrderLineWS parentLine;
        private List<OrderLineWS> childLines = new ArrayList<>();
        private List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        private List<Integer> assetIds = new ArrayList<>();

        public OrderLineBuilder type(int orderLineType){
            this.orderLineType = orderLineType;
            return this;
        }

        public OrderLineBuilder withDescription(String description){
            this.description = description;
            return this;
        }

        public OrderLineBuilder useItem(boolean useItem){
            this.useItem = useItem;
            return this;
        }

        public OrderLineBuilder withItemId(int itemId){
            this.itemId = itemId;
            return this;
        }

        public OrderLineBuilder withQuantity(BigDecimal quantity){
            this.quantity = quantity;
            return this;
        }

        public OrderLineBuilder withAmount(BigDecimal amount){
            this.amount = amount;
            return this;
        }

        public OrderLineBuilder withParentLine(OrderLineWS parentLine){
            this.parentLine = parentLine;
            return this;
        }

        public OrderLineBuilder withChildLine(OrderLineWS childLine){
            this.childLines.add(childLine);
            return this;
        }

        public OrderLineBuilder withMetaField(MetaFieldValueWS metaField){
            this.metaFieldValues.add(metaField);
            return this;
        }

        public OrderLineBuilder withAsset (Integer assetIds) {
            this.assetIds.add(assetIds);
            return this;
        }

        public OrderLineWS build(){

            OrderLineWS orderLine = new OrderLineWS();
            orderLine.setTypeId(Integer.valueOf(orderLineType));
            orderLine.setDescription(description);
            orderLine.setUseItem(useItem);
            orderLine.setAssetIds(assetIds.toArray(new Integer[assetIds.size()]));
            orderLine.setItemId(itemId);
            orderLine.setQuantity(quantity);
            orderLine.setAmount(amount);
            orderLine.setParentLine(parentLine);
            orderLine.setChildLines(orderLines.toArray(new OrderLineWS[orderLines.size()]));
            orderLine.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[metaFieldValues.size()]));

            return orderLine;
        }
    }

    public static OrderChangeWS[] buildFromOrder(OrderWS orderWS, Integer statusId, Date startDate) {

        List<OrderChangeWS> orderChanges = new ArrayList<>();
        Map<OrderLineWS, OrderChangeWS> lineToChangeMap = new HashMap<>();
        OrderWS rootOrder = findRootOrderIfPossible(orderWS);
        for (OrderLineWS orderLine : rootOrder.getOrderLines()){
            OrderChangeWS orderChange = buildChangeFromLine(orderWS, orderLine, statusId, startDate);
            orderChanges.add(orderChange);
            lineToChangeMap.put(orderLine, orderChange);
        }
        for (OrderWS childOrder : findAllChildren(rootOrder)) {
            for (OrderLineWS line : childOrder.getOrderLines()) {
                OrderChangeWS change = buildChangeFromLine(childOrder, line, statusId, startDate);
                orderChanges.add(change);
                lineToChangeMap.put(line, change);
            }
        }
        for (OrderLineWS line : lineToChangeMap.keySet()) {
            if (line.getParentLine() != null) {
                OrderChangeWS change = lineToChangeMap.get(line);
                if (line.getParentLine().getId() > 0) {
                    change.setParentOrderLineId(line.getParentLine().getId());
                } else {
                    OrderChangeWS parentChange = lineToChangeMap.get(line.getParentLine());
                    change.setParentOrderChange(parentChange);
                }
            }
        }
        return orderChanges.toArray(new OrderChangeWS[orderChanges.size()]);
    }

    public static OrderChangeWS buildChangeFromLine(OrderWS order, OrderLineWS line, Integer statusId, Date startDate) {

        OrderChangeWS ws = new OrderChangeWS();
        ws.setOptLock(1);
        ws.setOrderChangeTypeId(Constants.ORDER_CHANGE_TYPE_DEFAULT);
        ws.setUserAssignedStatusId(statusId);
        ws.setStartDate(startDate);
        ws.setApplicationDate(startDate);
        if (line.getOrderId() != null && line.getOrderId() > 0) {
            ws.setOrderId(line.getOrderId());
        } else {
            ws.setOrderWS(order);
        }
        if (line.getId() > 0) {
            ws.setOrderLineId(line.getId());
        } else {
            // new line
            ws.setUseItem(line.getUseItem() ? 1 : 0);
        }
        if (line.getParentLine() != null && line.getParentLine().getId() > 0) {
            ws.setParentOrderLineId(line.getParentLine().getId());
        }
        ws.setDescription(line.getDescription());
        ws.setItemId(line.getItemId());
        ws.setAssetIds(line.getAssetIds());
        ws.setPrice(line.getPriceAsDecimal());
        if (line.getDeleted() == 0) {
            if (line.getId() > 0) {
                ws.setQuantity(BigDecimal.ZERO);
            } else {
                ws.setQuantity(line.getQuantityAsDecimal());
            }
        } else {
            ws.setQuantity(line.getQuantityAsDecimal().negate());
        }
        ws.setRemoval(line.getDeleted());
        if (order != null) {
            ws.setNextBillableDate(order.getNextBillableDay());
        }
        ws.setPercentage(line.isPercentage());
        ws.setMetaFields(MetaFieldHelper.copy(line.getMetaFields(), true));
        return ws;
    }

    private static OrderWS findRootOrderIfPossible(OrderWS order) {
        OrderWS rootOrder = order;
        // find root order, prevent cycle if it exists in hierarchy
        while (rootOrder.getParentOrder() != null && !rootOrder.getParentOrder().equals(order)) {
            rootOrder = rootOrder.getParentOrder();
        }
        return rootOrder;
    }

    private static LinkedHashSet<OrderWS> findAllChildren(OrderWS rootOrder) {
        LinkedHashSet<OrderWS> orders = new LinkedHashSet<OrderWS>();
        findChildren(rootOrder, orders);
        return orders;
    }

    private static void findChildren(OrderWS order, LinkedHashSet<OrderWS> orders) {
        if (order == null) return;
        if (order.getChildOrders() != null) {
            List<OrderWS> newChildren = new LinkedList<OrderWS>(Arrays.asList(order.getChildOrders()));
            newChildren.removeAll(orders);
            orders.addAll(newChildren);
            for (OrderWS childOrder : newChildren) {
                findChildren(childOrder, orders);
            }
        }
    }

}
