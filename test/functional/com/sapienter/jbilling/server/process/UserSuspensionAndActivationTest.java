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

package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.SwapMethod;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import static com.sapienter.jbilling.server.order.WSTest.buildOneTimePostPaidOrder;
import static com.sapienter.jbilling.server.order.WSTest.buildOrder;
import static com.sapienter.jbilling.server.order.WSTest.createCreditCard;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Leandro ZOi
 * @since 01/15/18
 */

@Test(groups = { "billing", "process" }, testName = "UserSuspensionAndActivationTest")
public class UserSuspensionAndActivationTest extends BillingProcessTestCase {

    private static final Integer AGEING_PROCESS_TASK_ID = 6061;
    private static final Integer CC_PMT_ID = 5;
    private static final Integer ENTITY_ID = 1;
    private static final Integer ITEM_TYPE_ID = 1;
    private static final Integer PAYMENT_PERIOD = 1;
    private static final String ACTIVE = "Active";
    private static final String PAYMENT_DUE_STEP = "Ageing Day 0";
    private static final String GRACE_PERIOD_STEP = "Ageing Day 2";
    private static final String FIRST_RETRY_STEP = "Ageing Day 3";
    private static final String SUSPENDED_STEP = "Ageing Day 7";
    private static final String LAST_STEP = "Ageing Day 17";
    private static final String CC_NAME = "cc_name";
    private static final String CC_NUMBER_1 = "4111111111111152";
    private static final String CC_NUMBER_2 = "4024007124307900";
    private static final String CC_NUMBER_3 = "4532506702333522";
    private static final String NOTES = "Notes";
    private static final String ACTIVE_PERIOD_CHARGING_TASK = "com.sapienter.jbilling.server.pluggableTask.ActivePeriodChargingTask";
    private static final String CRON_EXPRESSION = "0 0 3,15 * * ?";
    private static final String CRON_EXPRESSION_YEAR = " 0 0 0 ? * * " + (LocalDate.now().getYear() + 10);
    private static final String SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK = "com.sapienter.jbilling.server.process.task.SuspendedUsersBillingProcessFilterTask";
    private static final Date EXPIRE_DATE = DateConvertUtils.asUtilDate(LocalDate.now().plusYears(10));

    private Integer orderChangeStatusApply;
    private Integer orderPeriodMontlhy;
    private Integer suspendedUserBillingProcessFilterTaskPluginId;
    private Integer activePeriodChargingTask;

    @BeforeClass
    protected void setUp () throws Exception {
        super.prepareTestInstance();
        orderPeriodMontlhy = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
        orderChangeStatusApply = PricingTestHelper.getOrCreateOrderChangeApplyStatus(api);

        api.saveAgeingConfigurationWithCollectionType(buildAgeingSteps(api),
                                                      api.getCallerLanguageId(),
                                                      CollectionType.REGULAR);

        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setMaximumPeriods(99);
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(7);
        config.setLastDayOfMonth(false);
        config.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());
        api.createUpdateBillingProcessConfiguration(config);

        logger.debug("Configuring SuspendedUsersBillingProcessFilterTask..............");
        PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK);
        PluggableTaskWS plugIn = new PluggableTaskWS();
        plugIn.setOwningEntityId(api.getCallerCompanyId());
        plugIn.setProcessingOrder(1);
        plugIn.setTypeId(type.getId());
        suspendedUserBillingProcessFilterTaskPluginId = api.createPlugin(plugIn);

        logger.debug("Configuring ActivePeriodChargingTask..............");
        type = api.getPluginTypeWSByClassName(ACTIVE_PERIOD_CHARGING_TASK);
        plugIn = new PluggableTaskWS();
        plugIn.setOwningEntityId(api.getCallerCompanyId());
        plugIn.setProcessingOrder(555);
        plugIn.setTypeId(type.getId());
        activePeriodChargingTask = api.createPlugin(plugIn);

        logger.debug("Configuring AgeingProcessTask..............");
        plugIn = api.getPluginWS(AGEING_PROCESS_TASK_ID);
        plugIn.getParameters().clear();
        Hashtable<String, String> parameters = new Hashtable<>();
        // Set cron expression to trigger every minute
        parameters.put("cron_exp", CRON_EXPRESSION_YEAR);
        plugIn.setParameters(parameters);
        api.updatePlugin(plugIn);
    }

    @AfterClass
    private void cleanUp() {
        if (suspendedUserBillingProcessFilterTaskPluginId != null) {
            api.deletePlugin(suspendedUserBillingProcessFilterTaskPluginId);
            suspendedUserBillingProcessFilterTaskPluginId = null;
        }

        if (activePeriodChargingTask != null) {
            api.deletePlugin(activePeriodChargingTask);
            activePeriodChargingTask = null;
        }

        PluggableTaskWS plugIn = api.getPluginWS(AGEING_PROCESS_TASK_ID);
        plugIn.getParameters().clear();
        Hashtable<String, String> parameters = new Hashtable<>();
        // Set cron expression to trigger every minute
        parameters.put("cron_exp", CRON_EXPRESSION);
        plugIn.setParameters(parameters);
        api.updatePlugin(plugIn);

        updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME);
    }

    @Test(enabled = true)
    public void test001ReactivationWithCommonOrders () throws Exception {
        UserWS user1 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("04/01/2012")
                                               .build();

        UserWS user2 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("04/01/2012")
                                               .build();

        UserWS user3 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("04/01/2012")
                                               .build();

        UserWS user4 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("04/01/2012")
                                               .build();

        OrderWS order1 = orderBuilderFactory.forUser(user1)
                                            .monthly()
                                            .postPaid()
                                            .proRate(false)
                                            .activeSince("03/01/2012")
                                            .build();

        OrderWS order2 = orderBuilderFactory.forUser(user2)
                                            .weekly()
                                            .postPaid()
                                            .proRate(false)
                                            .activeSince("03/01/2012")
                                            .build();

        OrderWS order3 = orderBuilderFactory.forUser(user3)
                                            .semiMonthly()
                                            .postPaid()
                                            .proRate(false)
                                            .activeSince("03/01/2012")
                                            .build();

        OrderWS order4 = orderBuilderFactory.forUser(user4)
                                            .monthly()
                                            .postPaid()
                                            .proRate(false)
                                            .activeSince("03/01/2012")
                                            .build();

        triggerBillingForDate(AsDate("04/01/2012"));
        scenarioVerifier.forOrder(order1)
                        .invoiceLines(1)
                        .nbd("04/01/2012")
                        .nid("05/01/2012")
                        .skipPreviousInvoice()
                        .verify();

        scenarioVerifier.forOrder(order2)
                        .invoiceLines(5)
                        .nbd("03/29/2012")
                        .nid("05/01/2012")
                        .skipPreviousInvoice()
                        .verify();

        scenarioVerifier.forOrder(order3)
                        .invoiceLines(2)
                        .nbd("04/01/2012")
                        .nid("05/01/2012")
                        .skipPreviousInvoice()
                        .verify();

        scenarioVerifier.forOrder(order4)
                        .invoiceLines(1)
                        .nbd("04/01/2012")
                        .nid("05/01/2012")
                        .skipPreviousInvoice()
                        .verify();

        api.triggerAgeing(AsDate("04/09/2012"));
        checkUsersStatus(PAYMENT_DUE_STEP, user1.getId(), user2.getId(), user3.getId(), user4.getId());
        api.triggerAgeing(AsDate("04/11/2012"));
        checkUsersStatus(GRACE_PERIOD_STEP, user1.getId(), user2.getId(), user3.getId(), user4.getId());
        api.triggerAgeing(AsDate("04/12/2012"));
        checkUsersStatus(FIRST_RETRY_STEP, user1.getId(), user2.getId(), user3.getId(), user4.getId());
        api.triggerAgeing(AsDate("04/17/2012"));
        checkUsersStatus(SUSPENDED_STEP, user1.getId(), user2.getId(), user3.getId(), user4.getId());

        payInvoice(user1, getPaymentInformation(CC_NAME, CC_NUMBER_1, EXPIRE_DATE), AsDate("04/20/2012"), api.getUnpaidInvoices(user1.getUserId()));
        payInvoice(user2, getPaymentInformation(CC_NAME, CC_NUMBER_2, EXPIRE_DATE), AsDate("04/21/2012"), api.getUnpaidInvoices(user2.getUserId()));
        payInvoice(user3, getPaymentInformation(CC_NAME, CC_NUMBER_3, EXPIRE_DATE), AsDate("04/22/2012"), api.getUnpaidInvoices(user3.getUserId()));

        checkUsersStatus(ACTIVE, user1.getId(), user2.getId(), user3.getId());
        checkUsersStatus(SUSPENDED_STEP, user4.getId());

        triggerBillingForDate(AsDate("05/01/2012"));
        scenarioVerifier.forOrder(order1)
                        .dueInvoiceLines(0)
                        .invoiceLines(2)
                        .nbd("05/01/2012")
                        .nid("06/01/2012")
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 04/01/2012 to 04/16/2012")
                        .withDescription("(.*) Period from 04/20/2012 to 04/30/2012")
                        .verify();

        scenarioVerifier.forOrder(order2)
                        .dueInvoiceLines(0)
                        .invoiceLines(4)
                        .nbd("04/26/2012")
                        .nid("06/01/2012")
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 03/29/2012 to 04/04/2012")
                        .withDescription("(.*) Period from 04/05/2012 to 04/11/2012")
                        .withDescription("(.*) Period from 04/12/2012 to 04/16/2012")
                        .withDescription("(.*) Period from 04/21/2012 to 04/25/2012")
                        .verify();

        scenarioVerifier.forOrder(order3)
                        .nbd("05/01/2012")
                        .nid("06/01/2012")
                        .dueInvoiceLines(0)
                        .invoiceLines(3)
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 04/01/2012 to 04/15/2012")
                        .withDescription("(.*) Period from 04/16/2012 to 04/16/2012")
                        .withDescription("(.*) Period from 04/22/2012 to 04/30/2012")
                        .verify();

        scenarioVerifier.forOrder(order4)
                        .nbd("05/01/2012")
                        .nid("06/01/2012")
                        .dueInvoiceLines(1)
                        .invoiceLines(2)
                        .skipPreviousInvoice()
                        .withDescription("(.*) due date 04/08/2012")
                        .withDescription("(.*) Period from 04/01/2012 to 04/16/2012")
                        .verify();

        api.triggerAgeing(AsDate("05/09/2012"));
        checkUsersStatus(PAYMENT_DUE_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("05/11/2012"));
        checkUsersStatus(GRACE_PERIOD_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("05/12/2012"));
        checkUsersStatus(FIRST_RETRY_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("05/17/2012"));
        checkUsersStatus(SUSPENDED_STEP, user1.getId(), user2.getId(), user3.getId());

        payInvoice(user2, getPaymentInformation(CC_NAME, CC_NUMBER_2, EXPIRE_DATE), AsDate("05/23/2012"), api.getUnpaidInvoices(user2.getUserId()));

        checkUsersStatus(ACTIVE, user2.getId());
        checkUsersStatus(SUSPENDED_STEP, user1.getId(), user3.getId());
        checkUsersStatus(LAST_STEP, user4.getId());

        triggerBillingForDate(AsDate("06/01/2012"));
        scenarioVerifier.forOrder(order1)
                        .nbd("06/01/2012")
                        .nid("07/01/2012")
                        .dueInvoiceLines(1)
                        .invoiceLines(1)
                        .skipPreviousInvoice()
                        .withDescription("(.*) due date 05/08/2012")
                        .withDescription("(.*) Period from 05/01/2012 to 05/16/2012")
                        .verify();

        scenarioVerifier.forOrder(order2)
                        .nbd("05/31/2012")
                        .nid("07/01/2012")
                        .dueInvoiceLines(0)
                        .invoiceLines(3)
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 04/26/2012 to 05/02/2012")
                        .withDescription("(.*) Period from 05/03/2012 to 05/09/2012")
                        .withDescription("(.*) Period from 05/10/2012 to 05/16/2012")
                        .verify();

        scenarioVerifier.forOrder(order3)
                        .nbd("06/01/2012")
                        .nid("07/01/2012")
                        .dueInvoiceLines(1)
                        .invoiceLines(2)
                        .skipPreviousInvoice()
                        .withDescription("(.*) due date 05/08/2012")
                        .withDescription("(.*) Period from 05/01/2012 to 05/15/2012")
                        .withDescription("(.*) Period from 05/16/2012 to 05/16/2012")
                        .verify();

        //Delete all the objects created
        logger.debug("Deleting invoices");
        Arrays.stream(api.getAllInvoices(user1.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        Arrays.stream(api.getAllInvoices(user2.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        Arrays.stream(api.getAllInvoices(user3.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        Arrays.stream(api.getAllInvoices(user4.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        logger.debug("Deleting orders");
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteOrder(order3.getId());
        api.deleteOrder(order4.getId());

        logger.debug("Updating users to active");
        updateCustomerStatusToActive(user1.getId(), api);
        updateCustomerStatusToActive(user2.getId(), api);
        updateCustomerStatusToActive(user3.getId(), api);
        updateCustomerStatusToActive(user4.getId(), api);

        logger.debug("Deleting users");
        api.deleteUser(user1.getId());
        api.deleteUser(user2.getId());
        api.deleteUser(user3.getId());
        api.deleteUser(user4.getId());
    }

    @Test(enabled = true)
    public void test002ReactivationWithFUPOrders () throws Exception {
        updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_LINE_BASED_COMPOSITION_TASK);

        UserWS user1 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("07/01/2012")
                                               .build();

        UserWS user2 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("07/01/2012")
                                               .build();

        UserWS user3 = monthlyBilledUserBuilder.billingDay(1)
                                               .nextInvoiceDate("07/01/2012")
                                               .build();

        assertNotNull("User Creation Failed ", user1.getId());

        ItemDTOEx item = CreateObjectUtil.createItem(ENTITY_ID, new BigDecimal("2.00"), Constants.PRIMARY_CURRENCY_ID, ITEM_TYPE_ID, "Product A");
        item.setNumber(String.format("Number %s", System.currentTimeMillis()));
        item.setAssetManagementEnabled(0);
        item.setHasDecimals(1);
        item = api.getItem(api.createItem(item), user1.getUserId(), null);
        assertNotNull("Item Creation Failed ", item.getId());

        Integer usagePoolWith100QuantityId = populateFreeUsagePoolObject("100", item.getId());
        assertNotNull("Usage Pool Creation Failed ", usagePoolWith100QuantityId);
        Integer usagePoolWith225QuantityId = populateFreeUsagePoolObject("225", item.getId());
        assertNotNull("Usage Pool Creation Failed ", usagePoolWith225QuantityId);

        PlanWS planWith100FUP = createPlan(orderPeriodMontlhy, usagePoolWith100QuantityId, "100",
                                           createPlanItem(item.getId(), BigDecimal.ZERO, orderPeriodMontlhy,
                                                          new BigDecimal("2.00"), PriceModelStrategy.FLAT.name()));
        assertNotNull("Plan Creation Failed ", planWith100FUP.getId());
        PlanWS planWith225FUP = createPlan(orderPeriodMontlhy, usagePoolWith225QuantityId,  "225",
                                           createPlanItem(item.getId(), BigDecimal.ZERO, orderPeriodMontlhy,
                                                          new BigDecimal("2.00"), PriceModelStrategy.FLAT.name()));
        assertNotNull("Plan Creation Failed ", planWith225FUP.getId());

        //Create orders for User1
        OrderWS order1 = buildOrder(user1.getUserId(), Constants.ORDER_BILLING_POST_PAID, orderPeriodMontlhy);
        order1.setActiveSince(AsDate("06/01/2012"));
        order1.setProrateFlag(true);
        order1.setOrderLines(new OrderLineWS[] {
            buildOrderLine(planWith100FUP.getItemId(), planWith100FUP.getDescription(), "1.00", "1")
        });

        OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order1, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("06/01/2012"));
        }

        order1 = api.getOrder(api.createUpdateOrder(order1, changes));
        assertNotNull("Order Creation Failed ", order1.getId());

        //Create orders for User2
        OrderWS order2 = buildOrder(user2.getUserId(), Constants.ORDER_BILLING_POST_PAID, orderPeriodMontlhy);
        order2.setActiveSince(AsDate("06/01/2012"));
        order2.setProrateFlag(true);
        order2.setOrderLines(new OrderLineWS[] {
            buildOrderLine(planWith100FUP.getItemId(), planWith100FUP.getDescription(), "1.00", "1")
        });

        changes = OrderChangeBL.buildFromOrder(order2, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("06/01/2012"));
        }

        order2 = api.getOrder(api.createUpdateOrder(order2, changes));
        assertNotNull("Order Creation Failed ", order2.getId());

        //Create orders for User3
        OrderWS order3 = buildOrder(user3.getUserId(), Constants.ORDER_BILLING_POST_PAID, orderPeriodMontlhy);
        order3.setActiveSince(AsDate("06/01/2012"));
        order3.setProrateFlag(true);
        order3.setOrderLines(new OrderLineWS[] {
            buildOrderLine(planWith100FUP.getItemId(), planWith100FUP.getDescription(), "1.00", "1")
        });

        changes = OrderChangeBL.buildFromOrder(order3, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("06/01/2012"));
        }

        order3 = api.getOrder(api.createUpdateOrder(order3, changes));
        assertNotNull("Order Creation Failed ", order3.getId());

        //Create consumption order for User3
        OrderWS order4 = buildOneTimePostPaidOrder(user3.getUserId());
        order4.setActiveSince(AsDate("06/01/2012"));
        order4.setOrderLines(new OrderLineWS[] {
            buildOrderLine(item.getId(), item.getDescription(), "2.00", "110")
        });

        changes = OrderChangeBL.buildFromOrder(order4, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("06/01/2012"));
        }

        order4 = api.getOrder(api.createUpdateOrder(order4, changes));
        assertNotNull("Order Creation Failed ", order4.getId());
        assertEquals("Order Creation Failed ", order4.getTotal(), "19.9999999980");

        OrderChangeWS[] changesToSwap = api.calculateSwapPlanChanges(order3,
                                                                     planWith100FUP.getItemId(),
                                                                     planWith225FUP.getItemId(),
                                                                     SwapMethod.DIFF,
                                                                     Util.truncateDate(order3.getActiveSince()));
        assertNotNull("Swap changes should be calculated", changesToSwap);
        api.getOrder(api.createUpdateOrder(order3, changesToSwap));
        order4 = api.getOrder(order4.getId());
        assertEquals("Incorrect Total of the Order", order4.getTotal(), "0E-10");
        triggerBillingForDate(AsDate("07/01/2012"));

        //Create consumption order for User1
        OrderWS order5 = buildOneTimePostPaidOrder(user2.getUserId());
        order5.setActiveSince(AsDate("07/01/2012"));
        order5.setOrderLines(new OrderLineWS[] {
            buildOrderLine(item.getId(), item.getDescription(), "2.00", "110")
        });

        changes = OrderChangeBL.buildFromOrder(order5, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("07/01/2012"));
        }

        order5 = api.getOrder(api.createUpdateOrder(order5, changes));
        assertNotNull("Order Creation Failed ", order5.getId());
        assertEquals("Order Creation Failed ", order5.getTotal(), "19.9999999980");

        api.triggerAgeing(AsDate("07/09/2012"));
        checkUsersStatus(PAYMENT_DUE_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("07/11/2012"));
        checkUsersStatus(GRACE_PERIOD_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("07/12/2012"));
        checkUsersStatus(FIRST_RETRY_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("07/17/2012"));
        checkUsersStatus(SUSPENDED_STEP, user1.getId(), user2.getId(), user3.getId());

        order4 = api.getOrder(order4.getId());
        order5 = api.getOrder(order5.getId());
        assertEquals("Incorrect Total of the Order", order4.getTotal(), "0E-10");
        assertEquals("Incorrect Total of the Order", order5.getTotal(), "116.7742000040");

        order2 = api.getOrder(order2.getId());
        changesToSwap = api.calculateSwapPlanChanges(order2,
                                                     planWith100FUP.getItemId(),
                                                     planWith225FUP.getItemId(),
                                                     SwapMethod.DIFF,
                                                     order2.getNextBillableDay());

        assertNotNull("Swap changes should be calculated", changesToSwap);
        api.getOrder(api.createUpdateOrder(order2, changesToSwap));
        order5 = api.getOrder(order5.getId());
        //assertEquals("Incorrect Total of the Order", order5.getTotal(), "0E-10");

        order1 = api.getOrder(order1.getId());
        order2 = api.getOrder(order2.getId());
        order3 = api.getOrder(order3.getId());

        logger.debug("Paying the invoices");
        payInvoice(user1, getPaymentInformation(CC_NAME, CC_NUMBER_1, EXPIRE_DATE), AsDate("07/20/2012"), api.getUnpaidInvoices(user1.getUserId()));
        payInvoice(user2, getPaymentInformation(CC_NAME, CC_NUMBER_2, EXPIRE_DATE), AsDate("07/21/2012"), api.getUnpaidInvoices(user2.getUserId()));
        payInvoice(user3, getPaymentInformation(CC_NAME, CC_NUMBER_3, EXPIRE_DATE), AsDate("07/22/2012"), api.getUnpaidInvoices(user3.getUserId()));

        checkUsersStatus(ACTIVE, user1.getId(), user2.getId(), user3.getId());
        order4 = api.getOrder(order4.getId());
        order5 = api.getOrder(order5.getId());
        assertEquals("Incorrect Total of the Order", order4.getTotal(), "0E-10");
        //assertEquals("Incorrect Total of the Order", order5.getTotal(), "0E-10");

        triggerBillingForDate(AsDate("08/01/2012"));
        scenarioVerifier.forOrder(order1)
                        .nbd("08/01/2012")
                        .nid("09/01/2012")
                        .dueInvoiceLines(0)
                        .invoiceLines(2)
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 07/01/2012 to 07/16/2012")
                        .withDescription("(.*) Period from 07/20/2012 to 07/31/2012")
                        .verify();

        scenarioVerifier.forOrder(order2)
                        .nbd("08/01/2012")
                        .nid("09/01/2012")
                        .dueInvoiceLines(0)
                        .invoiceLines(2)
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 07/01/2012 to 07/16/2012")
                        .withDescription("(.*) Period from 07/21/2012 to 07/31/2012")
                        .withDescription(item.getDescription() + "(.*)")
                        .verify();

        scenarioVerifier.forOrder(order3)
                        .nbd("08/01/2012")
                        .nid("09/01/2012")
                        .dueInvoiceLines(0)
                        .invoiceLines(2)
                        .skipPreviousInvoice()
                        .withDescription("(.*) Period from 07/01/2012 to 07/16/2012")
                        .withDescription("(.*) Period from 07/22/2012 to 07/31/2012")
                        .verify();

        triggerBillingForDate(AsDate("09/01/2012"));

        api.triggerAgeing(AsDate("09/09/2012"));
        checkUsersStatus(PAYMENT_DUE_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("09/11/2012"));
        checkUsersStatus(GRACE_PERIOD_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("09/12/2012"));
        checkUsersStatus(FIRST_RETRY_STEP, user1.getId(), user2.getId(), user3.getId());
        api.triggerAgeing(AsDate("09/17/2012"));
        checkUsersStatus(SUSPENDED_STEP, user1.getId(), user2.getId(), user3.getId());

        checkCustomerUsagePoolCycle(api.getCustomerUsagePoolsByCustomerId(user1.getCustomerId()),
                                    AsDate(2012, 9, 1),
                                    AsDate(2012, 9, 16),
                                    "53.3333000000");

        checkCustomerUsagePoolCycle(api.getCustomerUsagePoolsByCustomerId(user2.getCustomerId()),
                                    AsDate(2012, 9, 1),
                                    AsDate(2012, 9, 16),
                                    "120.0000000000");

        checkCustomerUsagePoolCycle(api.getCustomerUsagePoolsByCustomerId(user3.getCustomerId()),
                                    AsDate(2012, 9, 1),
                                    AsDate(2012, 9, 16),
                                    "120.0000000000");

        triggerBillingForDate(AsDate("10/01/2012"));

        logger.debug("Paying the invoices");
        payInvoice(user1, getPaymentInformation(CC_NAME, CC_NUMBER_1, EXPIRE_DATE), AsDate("10/20/2012"), api.getUnpaidInvoices(user1.getUserId()));
        payInvoice(user2, getPaymentInformation(CC_NAME, CC_NUMBER_2, EXPIRE_DATE), AsDate("10/21/2012"), api.getUnpaidInvoices(user2.getUserId()));
        payInvoice(user3, getPaymentInformation(CC_NAME, CC_NUMBER_3, EXPIRE_DATE), AsDate("10/22/2012"), api.getUnpaidInvoices(user3.getUserId()));

        checkCustomerUsagePoolCycle(api.getCustomerUsagePoolsByCustomerId(user1.getCustomerId()),
                                    AsDate(2012, 10, 1),
                                    AsDate(2012, 10, 31),
                                    "38.7097000000");

        checkCustomerUsagePoolCycle(api.getCustomerUsagePoolsByCustomerId(user2.getCustomerId()),
                                    AsDate(2012, 10, 1),
                                    AsDate(2012, 10, 31),
                                    "79.8387000000");

        checkCustomerUsagePoolCycle(api.getCustomerUsagePoolsByCustomerId(user3.getCustomerId()),
                                    AsDate(2012, 10, 1),
                                    AsDate(2012, 10, 31),
                                    "72.5806000000");

        //Delete all the objects created
        logger.debug("Deleting invoices");
        Arrays.stream(api.getAllInvoices(user1.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        Arrays.stream(api.getAllInvoices(user2.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        Arrays.stream(api.getAllInvoices(user3.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(api::deleteInvoice);

        logger.debug("Deleting orders");
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteOrder(order3.getId());
        api.deleteOrder(order4.getId());
        api.deleteOrder(order5.getId());

        logger.debug("Deleting plans");
        api.deletePlan(planWith100FUP.getId());
        api.deletePlan(planWith225FUP.getId());

        logger.debug("Updating users to active");
        updateCustomerStatusToActive(user1.getId(), api);
        updateCustomerStatusToActive(user2.getId(), api);
        updateCustomerStatusToActive(user3.getId(), api);

        logger.debug("Deleting users");
        api.deleteUser(user1.getId());
        api.deleteUser(user2.getId());
        api.deleteUser(user3.getId());
    }

    @Test(enabled = true)
    public void test003SuspendedOrderScenarios (){

        //create user for prepaid order
        UserWS user1 = monthlyBilledUserBuilder.billingDay(1)
                .nextInvoiceDate("01/01/2018")
                .build();
        assertNotNull("User Creation Failed ", user1.getId());

        //create user for PostPaid order
        UserWS user2 = monthlyBilledUserBuilder.billingDay(1)
                .nextInvoiceDate("01/01/2018")
                .build();
        assertNotNull("User Creation Failed ", user2.getId());

        ItemDTOEx item = CreateObjectUtil.createItem(ENTITY_ID, new BigDecimal("2.00"), Constants.PRIMARY_CURRENCY_ID, ITEM_TYPE_ID, "Product Test Item");
        item.setNumber(String.format("Number %s", System.currentTimeMillis()));
        item.setAssetManagementEnabled(0);
        item.setHasDecimals(1);
        item = api.getItem(api.createItem(item), user1.getUserId(), null);
        assertNotNull("Item Creation Failed ", item.getId());

        Integer usagePoolWith100QuantityId = populateFreeUsagePoolObject("100", item.getId());
        assertNotNull("Usage Pool Creation Failed ", usagePoolWith100QuantityId);
        Integer usagePoolWith225QuantityId = populateFreeUsagePoolObject("225", item.getId());
        assertNotNull("Usage Pool Creation Failed ", usagePoolWith225QuantityId);

        PlanWS planWith100FUP = createPlan(orderPeriodMontlhy, usagePoolWith100QuantityId, "100",
                                           createPlanItem(item.getId(), BigDecimal.ZERO, orderPeriodMontlhy,
                                                          new BigDecimal("2.00"), PriceModelStrategy.FLAT.name()));
        assertNotNull("Plan Creation Failed ", planWith100FUP.getId());
        PlanWS planWith225FUP = createPlan(orderPeriodMontlhy, usagePoolWith225QuantityId,  "225",
                                           createPlanItem(item.getId(), BigDecimal.ZERO, orderPeriodMontlhy,
                                                          new BigDecimal("2.00"), PriceModelStrategy.FLAT.name()));
        assertNotNull("Plan Creation Failed ", planWith225FUP.getId());

        //Create prepaid order for User1
        OrderWS order1 = buildOrder(user1.getUserId(), Constants.ORDER_BILLING_PRE_PAID, orderPeriodMontlhy);
        order1.setActiveSince(AsDate("01/01/2018"));
        order1.setProrateFlag(true);
        order1.setOrderLines(new OrderLineWS[] {
            buildOrderLine(planWith100FUP.getItemId(), planWith100FUP.getDescription(), "1.00", "1")
        });
        OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order1, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("01/01/2018"));
        }

        order1 = api.getOrder(api.createUpdateOrder(order1, changes));
        assertNotNull("Order Creation Failed ", order1.getId());

        //Create postpaid orders for User2
        OrderWS order2 = buildOrder(user2.getUserId(), Constants.ORDER_BILLING_POST_PAID, orderPeriodMontlhy);
        order2.setActiveSince(AsDate("12/01/2017"));
        order2.setProrateFlag(true);
        order2.setOrderLines(new OrderLineWS[] {
            buildOrderLine(planWith225FUP.getItemId(), planWith225FUP.getDescription(), "1.00", "1")
        });
        changes = OrderChangeBL.buildFromOrder(order2, orderChangeStatusApply);
        for (OrderChangeWS orderChange: changes) {
            orderChange.setStartDate(AsDate("12/01/2017"));
        }
        order2 = api.getOrder(api.createUpdateOrder(order2, changes));
        assertNotNull("Order Creation Failed ", order2.getId());

        Calendar billingDate = Calendar.getInstance();
        billingDate.set(Calendar.YEAR, 2018);
        billingDate.set(Calendar.MONTH, 0);
        billingDate.set(Calendar.DAY_OF_MONTH, 1);

        //Create invoice for user1
        api.createInvoiceWithDate(user1.getUserId(), billingDate.getTime(), null, null, false);
        //Validate invoice for User1
        validateInvoice(user1.getId(), "Period from 01/01/2018 to 01/31/2018");

        //Create invoice for user2
        api.createInvoiceWithDate(user2.getUserId(), billingDate.getTime(), null, null, false);
        //Validate invoice for User2
        validateInvoice(user2.getId(), "Period from 12/01/2017 to 12/31/2017");

        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2018);
        nextInvoiceDate.set(Calendar.MONTH, 1);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        user1.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user1);
        user1 = api.getUserWS(user1.getId());

        user2.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user2);
        user2 = api.getUserWS(user2.getId());

        api.triggerAgeing(AsDate("01/09/2018"));
        checkUsersStatus(PAYMENT_DUE_STEP, user1.getId());
        checkUsersStatus(PAYMENT_DUE_STEP, user2.getId());
        api.triggerAgeing(AsDate("01/11/2018"));
        checkUsersStatus(GRACE_PERIOD_STEP, user1.getId());
        checkUsersStatus(GRACE_PERIOD_STEP, user2.getId());
        api.triggerAgeing(AsDate("01/12/2018"));
        checkUsersStatus(FIRST_RETRY_STEP, user1.getId());
        checkUsersStatus(FIRST_RETRY_STEP, user2.getId());
        api.triggerAgeing(AsDate("01/17/2018"));
        checkUsersStatus(SUSPENDED_STEP, user1.getId());
        checkUsersStatus(SUSPENDED_STEP, user2.getId());

        //create invoice for user1
        api.createInvoiceWithDate(user1.getUserId(), nextInvoiceDate.getTime(), null, null, false);
        //Validate invoice for User1
        validateInvoice(user1.getId(), "Period from 01/01/2018 to 01/31/2018");

        payInvoice(user2, getPaymentInformation(CC_NAME, CC_NUMBER_1, EXPIRE_DATE), AsDate("01/20/2018"), api.getUnpaidInvoices(user2.getUserId()));
        checkUsersStatus(ACTIVE, user2.getId());
        //create invoice for user2
        api.createInvoiceWithDate(user2.getUserId(), nextInvoiceDate.getTime(), null, null, false);
        //Validate invoice for User2
        InvoiceWS invoice = api.getLatestInvoice(user2.getUserId());
        assertNotNull("Invoice Creation Failed ", invoice.getId());
        assertTrue(invoice.getInvoiceLines()[0].getDescription().contains("Period from 01/01/2018 to 01/16/2018"));
        assertTrue(invoice.getInvoiceLines()[1].getDescription().contains("Period from 01/20/2018 to 01/31/2018"));

        for (Integer payment : api.getPaymentsByUserId(user2.getId())) {
            api.deletePayment(payment);
        }

        //Deleting Invoices
        deleteInvoice(user1.getId());

        //Deleting order
        api.deleteOrder(order1.getId());
        //Updating users to active
        updateCustomerStatusToActive(user1.getId(), api);
        updateCustomerStatusToActive(user2.getId(), api);

        //Deleting users
        api.deleteUser(user1.getId());
        api.deleteUser(user2.getId());
    }

    private PaymentInformationWS getPaymentInformation(String ccName, String ccNumber, Date expireDate){
        PaymentInformationWS cc = createCreditCard(ccName,ccNumber, expireDate);
        cc.setPaymentMethodId(null);
        cc.setPaymentMethodTypeId(CC_PMT_ID);

        return cc;
    }

    private AgeingWS[] buildAgeingSteps(JbillingAPI api) {
        AgeingWS[] ageingSteps = new AgeingWS[5];
        ageingSteps[0] = buildAgeingStep(PAYMENT_DUE_STEP, 0, false, false, false, api, CollectionType.REGULAR);
        ageingSteps[1] = buildAgeingStep(GRACE_PERIOD_STEP, 2, false, true, false, api, CollectionType.REGULAR);
        ageingSteps[2] = buildAgeingStep(FIRST_RETRY_STEP, 3, true, false, false, api, CollectionType.REGULAR);
        ageingSteps[3] = buildAgeingStep(SUSPENDED_STEP, 7, false, false, true, api, CollectionType.REGULAR);
        ageingSteps[4] = buildAgeingStep(LAST_STEP, 17, false, false, false, api, CollectionType.REGULAR);

        return ageingSteps;
    }

    private AgeingWS buildAgeingStep(String statusStep,Integer days, boolean payment , boolean sendNotification,
                                     boolean suspended, JbillingAPI api, CollectionType collectionType) {

        AgeingWS ageingWS = new AgeingWS();
        ageingWS.setEntityId(api.getCallerCompanyId());
        ageingWS.setStatusStr(statusStep);
        ageingWS.setDays(days);
        ageingWS.setPaymentRetry(payment);
        ageingWS.setSendNotification(sendNotification);
        ageingWS.setSuspended(suspended);
        ageingWS.setStopActivationOnPayment(false);
        ageingWS.setCollectionType(collectionType);
        return  ageingWS;
    }

    private void checkUsersStatus(String ageingStatusDesc, Integer... users) {
        for (Integer userId: users) {
            UserWS user = api.getUserWS(userId);
            assertEquals("The User Status for user Id:".concat(userId.toString())
                                                       .concat(" should be ")
                                                       .concat(ageingStatusDesc), ageingStatusDesc, user.getStatus());
        }
    }

    private void payInvoice(UserWS user, PaymentInformationWS creditCard, Date effectiveDate, Integer... invoiceIds) {
        logger.debug("Unpaid invoices {}", Arrays.toString(invoiceIds));
        for (Integer invoiceId: invoiceIds) {
            InvoiceWS invoice = api.getInvoiceWS(invoiceId);
            PaymentWS payment = new PaymentWS();
            payment.setAmount(invoice.getTotalAsDecimal());
            payment.setIsRefund(0);
            payment.setMethodId(Constants.PAYMENT_METHOD_CREDIT);
            payment.setPaymentDate(effectiveDate);
            payment.setResultId(Constants.RESULT_ENTERED);
            payment.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
            payment.setUserId(user.getId());
            payment.setPaymentNotes(NOTES);
            payment.setPaymentPeriod(PAYMENT_PERIOD);
            payment.getPaymentInstruments().add(creditCard);
            payment.setInvoiceIds(new Integer[]{invoice.getId()});

            logger.debug("Applying payment");
            Integer paymentId = api.applyPayment(payment, invoice.getId());
            logger.debug("Created payment {}", paymentId);
            assertNotNull("Didn't get the payment id", paymentId);
        }

    }

    private Integer populateFreeUsagePoolObject(String fupQuantity, Integer itemId) {
        UsagePoolWS usagePool = new UsagePoolWS();
        usagePool.setName(String.format("%s Free Min %s", fupQuantity, System.currentTimeMillis()));
        usagePool.setQuantity(fupQuantity);
        usagePool.setPrecedence(-1);
        usagePool.setCyclePeriodUnit("Billing Periods");
        usagePool.setCyclePeriodValue(1);
        usagePool.setUsagePoolResetValue("Reset To Initial Value");
        usagePool.setItemTypes(new Integer[] { ITEM_TYPE_ID });
        usagePool.setItems(new Integer[] { itemId });
        usagePool.setEntityId(ENTITY_ID);

        return api.createUsagePool(usagePool);
    }

    private PlanWS createPlan(Integer periodId, Integer usagePoolId, String price, PlanItemWS... planItems) {
        PlanWS plan = new PlanWS();
        plan.setItemId(createItem(price));
        plan.setPeriodId(periodId);
        plan.setDescription("Plan " + System.currentTimeMillis());
        plan.setPlanItems(Arrays.asList(planItems));
        plan.setUsagePoolIds(new Integer[] { usagePoolId });

        return api.getPlanWS(api.createPlan(plan));
    }

    private PlanItemWS createPlanItem(Integer itemId, BigDecimal quantity, Integer periodId,
                                      BigDecimal price, String priceModel) {
        PlanItemWS planItemWS = new PlanItemWS();
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setPeriodId(periodId);
        bundle.setQuantity(quantity);
        planItemWS.setItemId(itemId);
        planItemWS.setBundle(bundle);
        planItemWS.addModel(CommonConstants.EPOCH_DATE, new PriceModelWS(priceModel, price, Constants.PRIMARY_CURRENCY_ID));

        return planItemWS;
    }

    private Integer createItem(String price) {
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("Item " + System.currentTimeMillis());
        item.setEntityId(ENTITY_ID);
        item.setTypes(new Integer[] { ITEM_TYPE_ID });
        item.setPrice(price);
        item.setNumber("GSP-" + System.currentTimeMillis());

        return api.createItem(item);
    }

    private OrderLineWS buildOrderLine(Integer itemId, String description, String price, String quantity) {
        OrderLineWS line = new OrderLineWS();
        line.setItemId(itemId);
        line.setPrice(price);
        line.setQuantity(quantity);
        line.setAmount(line.getPriceAsDecimal().multiply(new BigDecimal(line.getQuantity())));
        line.setTypeId(1);
        line.setDescription(description);
        line.setUseItem(true);

        return line;
    }

    private void updatePlugin(Integer pluginId, String className) {
        logger.debug("Configuring plugin {}", className);
        PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(className);
        PluggableTaskWS plugin = api.getPluginWS(pluginId);
        plugin.setTypeId(type.getId());

        api.updatePlugin(plugin);
    }

    private void checkCustomerUsagePoolCycle(CustomerUsagePoolWS[] customerUsagePools, Date startDate, Date endDate, String availableQuantity) {
        Arrays.stream(customerUsagePools)
              .filter(customerUsagePoolWS -> customerUsagePoolWS.getCycleStartDate().after(com.sapienter.jbilling.server.util.Util.getEpochDate()))
              .forEach(cup -> {
                  assertEquals(S("The cycle start date should be {}", startDate), cup.getCycleStartDate(), startDate);
                  assertEquals(S("The cycle end date should be {}", endDate), cup.getCycleStartDate(), startDate);
                  assertEquals(S("The available quantity should be {}", availableQuantity), cup.getQuantity(), availableQuantity);
              });
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }

    private void validateInvoice(Integer userId, String description){
        InvoiceWS invoice = api.getLatestInvoice(userId);
        assertNotNull("Invoice Creation Failed ", invoice.getId());
        for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
            if(null != line.getItemId()){
                assertTrue(line.getDescription().contains(description));
            }
        }
    }

    private void deleteInvoice(Integer userId){
        for (Integer invoice : api.getAllInvoices(userId)) {
            api.deleteInvoice(invoice);
        }
    }

}
