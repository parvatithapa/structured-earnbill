package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

@Test(groups = { "integration", "task", "order-processing-task" }, testName = "BasicLineTotalTaskTest", priority = 5)
public class BasicLineTotalTaskTest {

    private static final Logger logger = LoggerFactory.getLogger(BasicLineTotalTaskTest.class);
    // Prancing pony
    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final Integer ORDER_BILLING_POST_PAID = Constants.ORDER_BILLING_POST_PAID;
    private static final Integer ORDER_PERIOD_ONCE = Constants.ORDER_PERIOD_ONCE;
    private static final Integer CURRENCY_ID = Constants.PRIMARY_CURRENCY_ID;
    private static final Integer LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
    private static final Integer ORDER_LINE_TYPE_ITEM = Constants.ORDER_LINE_TYPE_ITEM;
    private static Integer PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID;
    private static Integer PRANCING_PONY_USER_ID;
    private static Integer ORDER_PERIOD_MONTHLY;
    private static Integer PRANCING_PONY_CATEGORY_ID;

    private static final String NOTE = "Notes";
    private static final String DESCRIPTION_PERCENTAGE = "Item Percentage";

    private static BigDecimal EIGHT = new BigDecimal("8.00");
    private static BigDecimal TWENTY = new BigDecimal("20.00");
    private static BigDecimal ONE_HUNDRED = new BigDecimal("100.00");

    // Api
    private static JbillingAPI api;

    @BeforeTest
    public void initializeTests() throws Exception {
        // Prancing Pony entities
        api = JbillingAPIFactory.getAPI();
        PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
        PRANCING_PONY_CATEGORY_ID = createItemCategory(api);
        PRANCING_PONY_USER_ID = CreateObjectUtil.createUser(true, null, CURRENCY_ID, true).getId();
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
    }

    @AfterTest
    public void cleanUp(){
        if(null != api){
            api = null;
        }

        if(null != PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID){
            PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = null;
        }

        if(null != ORDER_PERIOD_MONTHLY){
            ORDER_PERIOD_MONTHLY = null;
        }

        if(null != PRANCING_PONY_CATEGORY_ID){
           PRANCING_PONY_CATEGORY_ID = null;
        }

        if(null != PRANCING_PONY_USER_ID){
            PRANCING_PONY_CATEGORY_ID = null;
        }
    }

    @Test
    public void test001ItemPercentageLineApplyToPlans(){
        OrderWS prancingOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_ONCE);

        Integer firstItemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));
        Integer percentageItemId = api.createItem(CreateObjectUtil.createPercentageItem(PRANCING_PONY_ENTITY_ID, EIGHT,
                                                  CURRENCY_ID, PRANCING_PONY_CATEGORY_ID, DESCRIPTION_PERCENTAGE));

        // Create Bundle (bundled quantity=1, period = monthly)
        PlanItemWS bundledFirstItem = createPlanItem(firstItemId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY);
        PlanWS plan = createPlan(10, "RP", ONE_HUNDRED, Arrays.asList(bundledFirstItem), api);

        OrderLineWS lines[] = new OrderLineWS[2];
        lines[0] = buildOrderLine(percentageItemId, 1, EIGHT, true);
        lines[1] = buildOrderLine(plan.getPlanSubscriptionItemId(), 1, null, false);

        prancingOrder.setOrderLines(lines);
        logger.debug("Creating order ... {}", prancingOrder);

        Integer orderId = api.createOrder(prancingOrder, OrderChangeBL.buildFromOrder(prancingOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);

        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("108.0800000000"), new BigDecimal(order.getTotal()));
        assertEquals("Percentage Order Line has incorrect amount", new BigDecimal("8.0800000000"), getAmountPercentageLine(order.getOrderLines()));

        api.deleteOrder(orderId);
        api.deletePlan(plan.getId());
        api.deleteItem(percentageItemId);
        api.deleteItem(firstItemId);
    }

    @Test
    public void test002ItemPercentageLineApplyToDependencies(){
        OrderWS prancingOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_ONCE);
        OrderWS prancingChildOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_ONCE);

        ItemDTOEx firstProduct = createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false);
        Integer secondItemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));
        Integer thirdItemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.LINE_PERCENTAGE.name(), EIGHT, CURRENCY_ID);
        Integer percentageItemId = api.createItem(CreateObjectUtil.createPercentageItem(PRANCING_PONY_ENTITY_ID, EIGHT,
                                                  CURRENCY_ID, PRANCING_PONY_CATEGORY_ID, DESCRIPTION_PERCENTAGE, priceModel));

        setDependency(firstProduct, secondItemId, 1, 1);
        setDependency(firstProduct, thirdItemId, 1, 1);

        Integer firstItemId = api.createItem(firstProduct);

        OrderLineWS lines[] = new OrderLineWS[3];
        lines[0] = buildOrderLine(firstItemId, 1, TWENTY, false);
        OrderLineWS line = buildOrderLine(secondItemId, 1, TWENTY, false);
        line.setParentLine(lines[0]);
        lines[1] = line;
        lines[2] = buildOrderLine(percentageItemId, 1, EIGHT, true);

        prancingOrder.setOrderLines(lines);

        logger.debug("Creating order ... {}", prancingOrder);
        Integer orderId;
        try{
            orderId = api.createOrder(prancingOrder, OrderChangeBL.buildFromOrder(prancingOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));
            fail(String.format("Exception expected!!\n Creation of order %d should fail!!", orderId));
        } catch (SessionInternalError sie){
            logger.error("Error creating order!", sie);
        }

        line = buildOrderLine(thirdItemId, 1, TWENTY, false);
        line.setParentLine(lines[0]);

        prancingChildOrder.setOrderLines(new OrderLineWS[]{line});
        prancingOrder.setChildOrders(new OrderWS[]{prancingChildOrder});
        prancingChildOrder.setParentOrder(prancingOrder);

        orderId = api.createOrder(prancingOrder, OrderChangeBL.buildFromOrder(prancingOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);

        assertEquals("Order has incorrect order lines size", 3, order.getOrderLines().length);
        assertEquals("Child order is not found", 1, order.getChildOrders().length);
        assertEquals("Order Child has incorrect order lines size", 1, order.getChildOrders()[0].getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("44.8000000000"), new BigDecimal(order.getTotal()));
        assertEquals("Percentage Order Line has incorrect amount", new BigDecimal("4.8000000000"), getAmountPercentageLine(order.getOrderLines()));

        firstProduct = api.getItem(firstItemId, null, null);
        firstProduct.setDependencies(null);

        api.updateItem(firstProduct);
        api.deleteOrder(orderId);
        api.deleteItem(firstItemId);
        api.deleteItem(secondItemId);
        api.deleteItem(thirdItemId);
        api.deleteItem(percentageItemId);
    }

    public static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api){
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
            newStatus.addDescription(new InternationalDescriptionWS(LANGUAGE_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private ItemDTOEx createProduct(int testNumber, BigDecimal price, String productNumber, boolean assetsManagementEnabled) {
        ItemDTOEx product = CreateObjectUtil.createItem(PRANCING_PONY_ENTITY_ID, price, CURRENCY_ID,
                PRANCING_PONY_CATEGORY_ID, trimToLength("OrderWS " + testNumber + "-" + productNumber, 35));
        product.setNumber(trimToLength("OrderWS " + testNumber + "-" + productNumber, 50));
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
        return product;
    }

    private String trimToLength(String value, int length) {
        if (value == null || value.length() < length) return value;
        return value.substring(0, length);
    }

    public static OrderWS buildOrder(Integer userId, Integer billingTypeId, Integer periodId){
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingTypeId);
        order.setPeriod(periodId);
        order.setCurrencyId(CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        order.setActiveSince(cal.getTime());
        // notes can only be 200 long... but longer should not fail
        order.setNotes(NOTE);

        return order;
    }

    private Integer createItemCategory(JbillingAPI api){
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("category"+Short.toString((short)System.currentTimeMillis()));
        itemType.setOrderLineTypeId(ORDER_LINE_TYPE_ITEM);
        itemType.setAllowAssetManagement(1);
        itemType.setAssetStatuses(createAssetStatusForCategory());
        return api.createItemCategory(itemType);
    }

    private Set<AssetStatusDTOEx> createAssetStatusForCategory(){
        Set<AssetStatusDTOEx> assetStatuses = new HashSet<>();
        AssetStatusDTOEx addToOrderStatus = new AssetStatusDTOEx();
        addToOrderStatus.setDescription("AddToOrderStatus");
        addToOrderStatus.setIsAvailable(0);
        addToOrderStatus.setIsDefault(0);
        addToOrderStatus.setIsInternal(0);
        addToOrderStatus.setIsOrderSaved(1);
        addToOrderStatus.setIsActive(1);
        addToOrderStatus.setIsPending(0);
        assetStatuses.add(addToOrderStatus);

        AssetStatusDTOEx available = new AssetStatusDTOEx();
        available.setDescription("Available");
        available.setIsAvailable(1);
        available.setIsDefault(1);
        available.setIsInternal(0);
        available.setIsOrderSaved(0);
        available.setIsActive(0);
        available.setIsPending(0);
        assetStatuses.add(available);

        AssetStatusDTOEx notAvailable = new AssetStatusDTOEx();
        notAvailable.setDescription("NotAvailable");
        notAvailable.setIsAvailable(0);
        notAvailable.setIsDefault(0);
        notAvailable.setIsInternal(0);
        notAvailable.setIsOrderSaved(0);
        notAvailable.setIsActive(0);
        notAvailable.setIsPending(0);
        assetStatuses.add(notAvailable);

        AssetStatusDTOEx pending = new AssetStatusDTOEx();
        pending.setDescription("Pending");
        pending.setIsAvailable(0);
        pending.setIsDefault(0);
        pending.setIsInternal(0);
        pending.setIsOrderSaved(1);
        pending.setIsActive(0);
        pending.setIsPending(1);
        assetStatuses.add(pending);

        return assetStatuses;
    }

    private PlanItemWS createPlanItem(Integer itemId, BigDecimal quantity, Integer periodId) {
        PlanItemWS planItemWS = new PlanItemWS();
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(periodId);
        bundle.setQuantity(quantity);
        planItemWS.setItemId(itemId);
        planItemWS.setBundle(bundle);
        planItemWS.addModel(CommonConstants.EPOCH_DATE,
                new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, CURRENCY_ID));
        return planItemWS;
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(LANGUAGE_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private PlanWS createPlan(int testNumber, String planName, BigDecimal price,
                              List<PlanItemWS> planBundleItems, JbillingAPI api) {

        ItemDTOEx planItem = createProduct(testNumber, price, planName, false);
        planItem.setId(api.createItem(planItem));
        PlanWS plan = new PlanWS();
        plan.setEditable(0);
        plan.setPeriodId(ORDER_PERIOD_MONTHLY);
        plan.setItemId(planItem.getId());
        plan.setPlanItems(planBundleItems);
        Integer planId = api.createPlan(plan);
        return api.getPlanWS(planId);
    }

    public static OrderLineWS buildOrderLine(Integer itemId, Integer quantity, BigDecimal price, boolean isPercentage, Integer... assetsId) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ORDER_LINE_TYPE_ITEM);
        line.setQuantity(quantity);
        line.setDescription(String.format("Line for product %d", itemId));
        line.setItemId(itemId);
        line.setPercentage(isPercentage);

        if(null != assetsId){
            line.setAssetIds(assetsId);
        }
        if(null == price){
            line.setUseItem(Boolean.TRUE);
        } else {
            line.setPrice(price);
            line.setAmount(price.multiply(new BigDecimal(quantity)));
        }
        return line;
    }

    private void setDependency(ItemDTOEx product, Integer itemId, Integer min, Integer max) {
        ItemDependencyDTOEx dep1 = new ItemDependencyDTOEx();
        dep1.setDependentId(itemId);
        dep1.setMinimum(min);
        dep1.setMaximum(max);
        dep1.setType(ItemDependencyType.ITEM);
        ItemDependencyDTOEx[] deps = product.getDependencies();
        ItemDependencyDTOEx[] newDepts = new ItemDependencyDTOEx[deps == null ? 1 : (deps.length+1)];
        int idx = 0;
        if(deps != null) {
            for(ItemDependencyDTOEx d : deps) {
                newDepts[idx++] = d;
            }
        }
        newDepts[idx] = dep1;
        product.setDependencies(newDepts);
    }

    private BigDecimal getAmountPercentageLine(OrderLineWS[] orderLines){
        BigDecimal amount = BigDecimal.ZERO;

        for(OrderLineWS line: orderLines){
            if(line.isPercentage()){
                amount = new BigDecimal(line.getAmount());
                break;
            }
        }

        return amount;
    }
}
