package com.sapienter.jbilling.server.task;

import com.sapienter.jbilling.server.item.*;
import com.sapienter.jbilling.server.order.*;

import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.process.PeriodOfTime;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.item.db.FreeTrialPeriod;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.util.MapPeriodToCalendar;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.order.task.FreeTrialPlanPeriodHandlerTask;

import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.test.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.math.BigDecimal;

import org.testng.annotations.Test;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.testng.AssertJUnit.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;


@Test(groups = {"integration", "task", "order-processing-task", "single-test"}, testName = "FreeTrialPlanPeriodHandlerTaskTest")
public class FreeTrialPlanPeriodHandlerTaskTest {
    private static final Logger logger = LoggerFactory.getLogger(FreeTrialPlanPeriodHandlerTaskTest.class);


    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final Integer CURRENCY_ID = Constants.PRIMARY_CURRENCY_ID;
    private static final Integer LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
    private static final Integer ORDER_LINE_TYPE_ITEM = Constants.ORDER_LINE_TYPE_ITEM;
    private static final Integer ORDER_BILLING_POST_PAID = Constants.ORDER_BILLING_POST_PAID;
    private static final String FREE_TRIAL_SUBSCRIPTION_META_FIELD = Constants.FREE_TRIAL_SUBSCRIPTION_ORDER_ID;


    private static BigDecimal TWENTY = new BigDecimal("20.00");
    private static BigDecimal ONE_HUNDRED = new BigDecimal("100.00");


    private static Integer PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID;
    private static Integer FREE_TRIAL_SUBSCRIPTION_META_FIELD_ID;
    private static Integer PRANCING_PONY_CATEGORY_ID;
    private static Integer PRANCING_PONY_USER_ID;
    private static Integer ORDER_PERIOD_MONTHLY;
    private static Integer ORDER_PERIOD_3_MONTH;
    private static Integer ORDER_PERIOD_YEARLY;
    private static Integer FREE_TRIAL_PLUGIN_ID;
    private static Integer DISCOUNT_PRODUCT_ID;
    private static JbillingAPI api;


    @BeforeTest
    public void initializeTests() throws Exception {
        // Prancing Pony entities
        api = JbillingAPIFactory.getAPI();
        PRANCING_PONY_CATEGORY_ID = createItemCategory(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        ORDER_PERIOD_3_MONTH = getOrCreate3MonthOrderPeriod(api);
        ORDER_PERIOD_YEARLY = getOrCreateYearlyOrderPeriod(api);
        FREE_TRIAL_SUBSCRIPTION_META_FIELD_ID = createMetafields();
        PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
        PRANCING_PONY_USER_ID = CreateObjectUtil.createUser(true, null, CURRENCY_ID, true).getId();
        DISCOUNT_PRODUCT_ID = api.createItem(createProduct(2, BigDecimal.ZERO, "Discount Product".concat(String.valueOf(System.currentTimeMillis())), false));

        FREE_TRIAL_PLUGIN_ID = enablePlugin();
    }

    @AfterTest
    public void cleanUp() {
        if (null != DISCOUNT_PRODUCT_ID) {
            api.deleteItem(DISCOUNT_PRODUCT_ID);
            DISCOUNT_PRODUCT_ID = null;
        }

        if (null != FREE_TRIAL_PLUGIN_ID) {
            api.deletePlugin(FREE_TRIAL_PLUGIN_ID);
            FREE_TRIAL_PLUGIN_ID = null;
        }

        if (null != ORDER_PERIOD_MONTHLY) {
            ORDER_PERIOD_MONTHLY = null;
        }

        if (null != ORDER_PERIOD_3_MONTH) {
            ORDER_PERIOD_MONTHLY = null;
        }

        if (null != ORDER_PERIOD_YEARLY) {
            ORDER_PERIOD_MONTHLY = null;
        }

        if (null != FREE_TRIAL_SUBSCRIPTION_META_FIELD_ID) {
            api.deleteMetaField(FREE_TRIAL_SUBSCRIPTION_META_FIELD_ID);
            FREE_TRIAL_SUBSCRIPTION_META_FIELD_ID = null;
        }

        if (null != PRANCING_PONY_CATEGORY_ID) {
            api.deleteItemCategory(PRANCING_PONY_CATEGORY_ID);
            PRANCING_PONY_CATEGORY_ID = null;
        }

        if (null != PRANCING_PONY_USER_ID) {
            api.deleteUser(PRANCING_PONY_USER_ID);
            PRANCING_PONY_USER_ID = null;
        }

        if (null != api) {
            api = null;
        }
    }

    @Test
    public void test001FreeTrialDays() {
        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);
        Integer itemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));

        PlanItemWS bundledFirstItem = createPlanItem(itemId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY);
        PlanWS plan = createPlan(10, "RP", ONE_HUNDRED, ORDER_PERIOD_MONTHLY, FreeTrialPeriod.DAYS, Arrays.asList(bundledFirstItem), api);
        logger.debug("Creating plan ... {}", plan);

        OrderLineWS lines[] = new OrderLineWS[1];
        lines[0] = buildOrderLine(plan.getPlanSubscriptionItemId(), 1, null, false);
        newOrder.setOrderLines(lines);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);
        logger.debug("Created order ... {}", order);

        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("101.0000000000"), new BigDecimal(order.getTotal()));

        OrderWS discountOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        if (discountOrder != null) {
            assertEquals("Discount order not created", false, discountOrder.getId().equals(orderId));
            for (MetaFieldValueWS value : discountOrder.getMetaFields()) {
                if (value.getFieldName().equals(FREE_TRIAL_SUBSCRIPTION_META_FIELD)) {
                    assertEquals("Discount order has incorrect subscription order id", true, value != null && value.getValue().equals(orderId));
                    break;
                }
            }
        }
        logger.debug("Discount order ... {}", discountOrder);

        UserWS user = api.getUserWS(PRANCING_PONY_USER_ID);
        MainSubscriptionWS mainSubscription = user.getMainSubscription();

        Date activeUntilDate = freeTrialPlanEndDate(FreeTrialPeriod.DAYS, plan.getFreeTrialPeriodValue(), order.getActiveSince(), mainSubscription);
        PeriodOfTime cycle = new PeriodOfTime(order.getActiveSince(), activeUntilDate, 0);
        int noOfDays = cycle.getDaysInPeriod() + 1;
        int maxDaysOfMonth = DateConvertUtils.asLocalDate(cycle.getStart()).lengthOfMonth();
        logger.debug("activeUntilDate={}, noOfDays={}, maxDaysOfMonth={}", activeUntilDate, noOfDays, maxDaysOfMonth);

        assertEquals("Discount order has incorrect active until date", true, discountOrder.getActiveUntil().equals(activeUntilDate));
        assertEquals("Discount order has incorrect amount", discountOrder.getTotalAsDecimal(), new BigDecimal("-101.0000000000"));

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2007, 10, 3);

        Integer[] invoiceIds = api.createInvoiceWithDate(PRANCING_PONY_USER_ID, cal.getTime(), null, null, false);
        InvoiceWS invoiceWs = api.getInvoiceWS(invoiceIds[0]);
        assertNotNull("invoice generated", invoiceWs);

        api.deleteInvoice(invoiceWs.getId());
        api.deleteOrder(orderId);
        api.deletePlan(plan.getId());
        api.deleteItem(itemId);
    }

    @Test
    public void test001FreeTrialMonth() {
        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);
        Integer itemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));

        PlanItemWS bundledFirstItem = createPlanItem(itemId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY);
        PlanWS plan = createPlan(10, "RP", ONE_HUNDRED, ORDER_PERIOD_MONTHLY, FreeTrialPeriod.MONTHS, Arrays.asList(bundledFirstItem), api);
        logger.debug("Creating plan ... {}", plan);

        OrderLineWS lines[] = new OrderLineWS[1];
        lines[0] = buildOrderLine(plan.getPlanSubscriptionItemId(), 1, null, false);
        newOrder.setOrderLines(lines);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);
        logger.debug("Created order ... {}", order);

        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("101.0000000000"), new BigDecimal(order.getTotal()));

        OrderWS discountOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        if (discountOrder != null) {
            assertEquals("Discount order not created", false, discountOrder.getId().equals(orderId));
            for (MetaFieldValueWS value : discountOrder.getMetaFields()) {
                if (value.getFieldName().equals(FREE_TRIAL_SUBSCRIPTION_META_FIELD)) {
                    assertEquals("Discount order has incorrect subscription order id", true, value != null && value.getValue().equals(orderId));
                    break;
                }
            }
        }
        logger.debug("Discount order ... {}", discountOrder);

        UserWS user = api.getUserWS(PRANCING_PONY_USER_ID);
        MainSubscriptionWS mainSubscription = user.getMainSubscription();

        Date activeUntilDate = freeTrialPlanEndDate(FreeTrialPeriod.MONTHS, plan.getFreeTrialPeriodValue(), order.getActiveSince(), mainSubscription);
        PeriodOfTime cycle = new PeriodOfTime(order.getActiveSince(), activeUntilDate, 0);
        int noOfDays = cycle.getDaysInPeriod() + 1;
        int maxDaysOfMonth = DateConvertUtils.asLocalDate(cycle.getStart()).lengthOfMonth();
        logger.debug("activeUntilDate={}, noOfDays={}, maxDaysOfMonth={}", activeUntilDate, noOfDays, maxDaysOfMonth);

        assertEquals("Discount order has incorrect active until date", true, discountOrder.getActiveUntil().equals(activeUntilDate));
        assertEquals("Discount order has incorrect amount", discountOrder.getTotalAsDecimal(), new BigDecimal("-101.0000000000"));

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2007, 10, 3);

        Integer[] invoiceIds = api.createInvoiceWithDate(PRANCING_PONY_USER_ID, cal.getTime(), null, null, false);
        InvoiceWS invoiceWs = api.getInvoiceWS(invoiceIds[0]);
        assertNotNull("invoice generated", invoiceWs);

        api.deleteInvoice(invoiceWs.getId());
        api.deleteOrder(orderId);
        api.deletePlan(plan.getId());
        api.deleteItem(itemId);
    }

    @Test
    public void test001FreeTrial3Month() {
        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_3_MONTH);
        Integer itemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));

        PlanItemWS bundledFirstItem = createPlanItem(itemId, BigDecimal.ONE, ORDER_PERIOD_3_MONTH);
        PlanWS plan = createPlan(10, "RP", ONE_HUNDRED, ORDER_PERIOD_3_MONTH, FreeTrialPeriod.MONTHS, Arrays.asList(bundledFirstItem), api);
        logger.debug("Creating plan ... {}", plan);

        OrderLineWS lines[] = new OrderLineWS[1];
        lines[0] = buildOrderLine(plan.getPlanSubscriptionItemId(), 1, null, false);
        newOrder.setOrderLines(lines);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);
        logger.debug("Created order ... {}", order);

        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("101.0000000000"), new BigDecimal(order.getTotal()));

        OrderWS discountOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        if (discountOrder != null) {
            assertEquals("Discount order not created", false, discountOrder.getId().equals(orderId));
            for (MetaFieldValueWS value : discountOrder.getMetaFields()) {
                if (value.getFieldName().equals(FREE_TRIAL_SUBSCRIPTION_META_FIELD)) {
                    assertEquals("Discount order has incorrect subscription order id", true, value != null && value.getValue().equals(orderId));
                    break;
                }
            }
        }
        logger.debug("Discount order ... {}", discountOrder);

        UserWS user = api.getUserWS(PRANCING_PONY_USER_ID);
        MainSubscriptionWS mainSubscription = user.getMainSubscription();

        Date activeUntilDate = freeTrialPlanEndDate(FreeTrialPeriod.MONTHS, plan.getFreeTrialPeriodValue(), order.getActiveSince(), mainSubscription);
        PeriodOfTime cycle = new PeriodOfTime(order.getActiveSince(), activeUntilDate, 0);
        int noOfDays = cycle.getDaysInPeriod() + 1;
        int maxDaysOfMonth = (int) DAYS.between(DateConvertUtils.asLocalDate(order.getActiveSince()), DateConvertUtils.asLocalDate(order.getActiveSince()).plusMonths(3));
        logger.debug("activeUntilDate={}, noOfDays={}, maxDaysOfMonth={}", activeUntilDate, noOfDays, maxDaysOfMonth);

        assertEquals("Discount order has incorrect active until date", true, discountOrder.getActiveUntil().equals(activeUntilDate));
        assertEquals("Discount order has incorrect amount", discountOrder.getTotalAsDecimal(), new BigDecimal("-101.0000000000"));

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2007, 10, 3);

        Integer[] invoiceIds = api.createInvoiceWithDate(PRANCING_PONY_USER_ID, cal.getTime(), null, null, false);
        InvoiceWS invoiceWs = api.getInvoiceWS(invoiceIds[0]);
        assertNotNull("invoice generated", invoiceWs);

        api.deleteInvoice(invoiceWs.getId());
        api.deleteOrder(orderId);
        api.deletePlan(plan.getId());
        api.deleteItem(itemId);
    }

    @Test
    public void test001FreeTrialYearly() {
        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_YEARLY);
        Integer itemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));

        PlanItemWS bundledFirstItem = createPlanItem(itemId, BigDecimal.ONE, ORDER_PERIOD_YEARLY);
        PlanWS plan = createPlan(10, "RP", ONE_HUNDRED, ORDER_PERIOD_YEARLY, FreeTrialPeriod.YEARS, Arrays.asList(bundledFirstItem), api);
        logger.debug("Creating plan ... {}", plan);

        OrderLineWS lines[] = new OrderLineWS[1];
        lines[0] = buildOrderLine(plan.getPlanSubscriptionItemId(), 1, null, false);
        newOrder.setOrderLines(lines);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);
        logger.debug("Created order ... {}", order);

        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("101.0000000000"), new BigDecimal(order.getTotal()));

        OrderWS discountOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        if (discountOrder != null) {
            assertEquals("Discount order not created", false, discountOrder.getId().equals(orderId));
            for (MetaFieldValueWS value : discountOrder.getMetaFields()) {
                if (value.getFieldName().equals(FREE_TRIAL_SUBSCRIPTION_META_FIELD)) {
                    assertEquals("Discount order has incorrect subscription order id", true, value != null && value.getValue().equals(orderId));
                    break;
                }
            }
        }
        logger.debug("Discount order ... {}", discountOrder);

        UserWS user = api.getUserWS(PRANCING_PONY_USER_ID);
        MainSubscriptionWS mainSubscription = user.getMainSubscription();

        Date activeUntilDate = freeTrialPlanEndDate(FreeTrialPeriod.YEARS, plan.getFreeTrialPeriodValue(), order.getActiveSince(), mainSubscription);
        PeriodOfTime cycle = new PeriodOfTime(order.getActiveSince(), activeUntilDate, 0);
        int noOfDays = cycle.getDaysInPeriod() + 1;
        int maxDaysOfMonth = (int) DAYS.between(DateConvertUtils.asLocalDate(order.getActiveSince()), DateConvertUtils.asLocalDate(order.getActiveSince()).plusYears(1));
        logger.debug("activeUntilDate={}, noOfDays={}, maxDaysOfMonth={}", activeUntilDate, noOfDays, maxDaysOfMonth);

        assertEquals("Discount order has incorrect active until date", true, discountOrder.getActiveUntil().equals(activeUntilDate));
        assertEquals("Discount order has incorrect amount", discountOrder.getTotalAsDecimal(), new BigDecimal("-101.0000000000"));

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2007, 10, 3);

        Integer[] invoiceIds = api.createInvoiceWithDate(PRANCING_PONY_USER_ID, cal.getTime(), null, null, false);
        InvoiceWS invoiceWs = api.getInvoiceWS(invoiceIds[0]);
        assertNotNull("invoice generated", invoiceWs);

        api.deleteInvoice(invoiceWs.getId());
        api.deleteOrder(orderId);
        api.deletePlan(plan.getId());
        api.deleteItem(itemId);
    }

    @Test
    public void test001FreeTrialBillingCycle() {
        OrderWS newOrder = buildOrder(PRANCING_PONY_USER_ID, ORDER_BILLING_POST_PAID, ORDER_PERIOD_MONTHLY);
        Integer itemId = api.createItem(createProduct(2, TWENTY, "Product".concat(String.valueOf(System.currentTimeMillis())), false));

        PlanItemWS bundledFirstItem = createPlanItem(itemId, BigDecimal.ONE, ORDER_PERIOD_MONTHLY);
        PlanWS plan = createPlan(10, "RP", ONE_HUNDRED, ORDER_PERIOD_MONTHLY, FreeTrialPeriod.BILLING_CYCLE, Arrays.asList(bundledFirstItem), api);
        logger.debug("Creating plan ... {}", plan);

        OrderLineWS lines[] = new OrderLineWS[1];
        lines[0] = buildOrderLine(plan.getPlanSubscriptionItemId(), 1, null, false);
        newOrder.setOrderLines(lines);
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, PRANCING_PONY_ORDER_CHANGE_STATUS_APPLY_ID));

        assertNotNull("Didn't get created order", orderId);
        OrderWS order = api.getOrder(orderId);
        logger.debug("Created order ... {}", order);

        assertEquals("Order has incorrect order lines size", 2, order.getOrderLines().length);
        assertEquals("Order has incorrect amount", new BigDecimal("101.0000000000"), new BigDecimal(order.getTotal()));

        OrderWS discountOrder = api.getLatestOrder(PRANCING_PONY_USER_ID);
        if (discountOrder != null) {
            assertEquals("Discount order not created", false, discountOrder.getId().equals(orderId));
            for (MetaFieldValueWS value : discountOrder.getMetaFields()) {
                if (value.getFieldName().equals(FREE_TRIAL_SUBSCRIPTION_META_FIELD)) {
                    assertEquals("Discount order has incorrect subscription order id", true, value != null && value.getValue().equals(orderId));
                    break;
                }
            }
        }
        logger.debug("Discount order ... {}", discountOrder);

        UserWS user = api.getUserWS(PRANCING_PONY_USER_ID);
        MainSubscriptionWS mainSubscription = user.getMainSubscription();

        Date activeUntilDate = freeTrialPlanEndDate(FreeTrialPeriod.BILLING_CYCLE, plan.getFreeTrialPeriodValue(), order.getActiveSince(), mainSubscription);
        PeriodOfTime cycle = new PeriodOfTime(order.getActiveSince(), activeUntilDate, 0);
        int noOfDays = cycle.getDaysInPeriod();
        int maxDaysOfMonth = DateConvertUtils.asLocalDate(cycle.getStart()).lengthOfMonth();
        logger.debug("activeUntilDate={}, noOfDays={}, maxDaysOfMonth={}", activeUntilDate, noOfDays, maxDaysOfMonth);


        assertEquals("Discount order has incorrect active until date", true, discountOrder.getActiveUntil().equals(activeUntilDate));
        assertEquals("Discount order has incorrect amount", discountOrder.getTotalAsDecimal(), new BigDecimal("-101.0000000000"));


        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2007, 10, 3);

        Integer[] invoiceIds = api.createInvoiceWithDate(PRANCING_PONY_USER_ID, cal.getTime(), null, null, false);
        InvoiceWS invoiceWs = api.getInvoiceWS(invoiceIds[0]);
        assertNotNull("invoice generated", invoiceWs);

        api.deleteInvoice(invoiceWs.getId());
        api.deleteOrder(orderId);
        api.deletePlan(plan.getId());
        api.deleteItem(itemId);
    }

    private Integer enablePlugin() {
        PluggableTaskTypeWS freeTrialPluginTypeId = api.getPluginTypeWSByClassName(FreeTrialPlanPeriodHandlerTask.class.getName());
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setTypeId(freeTrialPluginTypeId.getId());
        plugin.setProcessingOrder(60);

        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("Free Trial Discount Product ID", DISCOUNT_PRODUCT_ID.toString());
        parameters.put("Free Trial Subscription Order Meta Field", FREE_TRIAL_SUBSCRIPTION_META_FIELD);
        plugin.setParameters(parameters);

        return api.createPlugin(plugin);
    }

    private Integer createMetafields() {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName(FREE_TRIAL_SUBSCRIPTION_META_FIELD);
        metafieldWS.setEntityType(EntityType.ORDER);
        metafieldWS.setPrimary(false);
        metafieldWS.setDataType(DataType.INTEGER);

        return api.createMetaField(metafieldWS);
    }

    private Integer createItemCategory(JbillingAPI api) {
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("category" + Short.toString((short) System.currentTimeMillis()));
        itemType.setOrderLineTypeId(ORDER_LINE_TYPE_ITEM);
        itemType.setAllowAssetManagement(0);
        return api.createItemCategory(itemType);
    }

    private String trimToLength(String value, int length) {
        if (value == null || value.length() < length) return value;
        return value.substring(0, length);
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api) {
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (1 == period.getValue() && PeriodUnitDTO.MONTH == period.getPeriodUnitId()) {
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

    private Integer getOrCreate3MonthOrderPeriod(JbillingAPI api) {
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (3 == period.getValue() && PeriodUnitDTO.MONTH == period.getPeriodUnitId()) {
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(3);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(LANGUAGE_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private Integer getOrCreateYearlyOrderPeriod(JbillingAPI api) {
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (1 == period.getValue() && PeriodUnitDTO.YEAR == period.getPeriodUnitId()) {
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.YEAR);
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(LANGUAGE_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    public static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api) {
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for (OrderChangeStatusWS orderChangeStatus : list) {
            if (orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if (statusId != null) {
            return statusId;
        } else {
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(ApplyToOrder.YES);
            newStatus.setDeleted(0);
            newStatus.setOrder(1);
            newStatus.addDescription(new InternationalDescriptionWS(LANGUAGE_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private BigDecimal calculateSingleDayPrice(BigDecimal amount, int maxNoOfDays) {
        return amount.divide(new BigDecimal(maxNoOfDays), Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
    }

    private PlanItemWS createPlanItem(Integer itemId, BigDecimal quantity, Integer periodId) {
        PlanItemWS planItemWS = new PlanItemWS();
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(periodId);
        bundle.setQuantity(quantity);
        planItemWS.setItemId(itemId);
        planItemWS.setBundle(bundle);
        planItemWS.addModel(CommonConstants.EPOCH_DATE, new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, CURRENCY_ID));
        return planItemWS;
    }

    public static OrderWS buildOrder(Integer userId, Integer billingTypeId, Integer periodId) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingTypeId);
        order.setPeriod(periodId);
        order.setCurrencyId(CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 9, 3);
        order.setActiveSince(cal.getTime());
        return order;
    }

    private ItemDTOEx createProduct(int testNumber, BigDecimal price, String productNumber, boolean assetsManagementEnabled) {
        ItemDTOEx product = CreateObjectUtil.createItem(PRANCING_PONY_ENTITY_ID, price, CURRENCY_ID, PRANCING_PONY_CATEGORY_ID, trimToLength("OrderWS " + testNumber + "-" + productNumber, 35));
        product.setNumber(trimToLength("OrderWS " + testNumber + "-" + productNumber, 50));
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
        return product;
    }

    public static OrderLineWS buildOrderLine(Integer itemId, Integer quantity, BigDecimal price, boolean isPercentage, Integer... assetsId) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(ORDER_LINE_TYPE_ITEM);
        line.setQuantity(quantity);
        line.setDescription(String.format("Line for product %d", itemId));
        line.setItemId(itemId);
        line.setPercentage(isPercentage);

        if (null != assetsId) {
            line.setAssetIds(assetsId);
        }
        if (null == price) {
            line.setUseItem(Boolean.TRUE);
        } else {
            line.setPrice(price);
            line.setAmount(price.multiply(new BigDecimal(quantity)));
        }
        return line;
    }

    private Date freeTrialPlanEndDate(FreeTrialPeriod cyclePeriodUnit, Integer cyclePeriodValue, Date periodStartDate, MainSubscriptionWS mainSubscription) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(periodStartDate);
        OrderPeriodWS orderPeriod = api.getOrderPeriodWS(mainSubscription.getPeriodId());
        switch (cyclePeriodUnit) {
            case MONTHS:
                cal.add(Calendar.MONTH, cyclePeriodValue);
                break;
            case YEARS:
                cal.add(Calendar.YEAR, cyclePeriodValue);
            case BILLING_CYCLE:
                int unit = MapPeriodToCalendar.map(orderPeriod.getPeriodUnitId());
                cal.add(unit, orderPeriod.getValue() * cyclePeriodValue);
                if (unit == Calendar.MONTH) {
                    int noOfdays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
                    int daysToAdd = noOfdays < mainSubscription.getNextInvoiceDayOfPeriod() ? noOfdays : mainSubscription.getNextInvoiceDayOfPeriod();
                    cal.set(Calendar.DAY_OF_MONTH, daysToAdd);
                }
                break;
            case DAYS:
                cal.add(Calendar.DATE, cyclePeriodValue);
                break;
            default:
                break;
        }

        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }

    private PlanWS createPlan(int testNumber, String planName, BigDecimal price, Integer orderPeriod, FreeTrialPeriod trialPeriodUnit, List<PlanItemWS> planBundleItems, JbillingAPI api) {
        ItemDTOEx planItem = createProduct(testNumber, price, planName, false);
        planItem.setId(api.createItem(planItem));
        PlanWS plan = new PlanWS();
        plan.setEditable(0);
        plan.setPeriodId(orderPeriod);
        plan.setItemId(planItem.getId());
        plan.setPlanItems(planBundleItems);
        plan.setFreeTrial(true);
        plan.setFreeTrialPeriodUnit(trialPeriodUnit.name());
        if (trialPeriodUnit.equals(FreeTrialPeriod.DAYS)) {
            plan.setFreeTrialPeriodValue(5);
        } else {
            plan.setFreeTrialPeriodValue(1);
        }
        Integer planId = api.createPlan(plan);
        return api.getPlanWS(planId);
    }
}
