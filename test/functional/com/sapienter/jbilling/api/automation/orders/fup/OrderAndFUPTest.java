package com.sapienter.jbilling.api.automation.orders.fup;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.api.automation.orders.OrdersTestHelper;
import com.sapienter.jbilling.api.automation.utils.FileHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test cases involving FUP
 * and orders.
 *
 * @author Vojislav Stanojevikj
 * @since 15-JUN-2016.
 */
@Test(groups = {"api-automation"}, testName = "OrderAndFUPTest")
public class OrderAndFUPTest {

    private static final String CATEGORY_CODE = "NationalMobileCalls";
    private static final String PRODUCT_CODE = "RoamingCallRates";
    private static final String FEE_PRODUCT_CODE = "FeeRates";
    private static final String FUP_CODE = "100NationalCallMinutesFree";

    private Integer CATEGORY_ID;
    private Integer PRODUCT_ID;

    private EnvironmentHelper environmentHelper;
    private OrdersTestHelper testHelper;
    private TestBuilder testBuilder;
    private TestBuilder partialTestBuilder;

    @BeforeClass
    public void initializeTests(){
        partialTestBuilder = getPluginsAndItemSetup();
        testBuilder = getTestEnvironment();
    }

    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != environmentHelper){
            environmentHelper = null;
        }
        if (null != testHelper){
            testHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
        if (null != partialTestBuilder){
            partialTestBuilder = null;
        }
    }

    private TestBuilder getPluginsAndItemSetup(){
        return TestBuilder.newTest().givenForMultiple(envCreator -> {

            final JbillingAPI api = envCreator.getPrancingPonyApi();
            environmentHelper = EnvironmentHelper.getInstance(api);
            testHelper = OrdersTestHelper.INSTANCE;


            ConfigurationBuilder configurationBuilder = envCreator.configurationBuilder(api);
            addPluginIfAbsent(configurationBuilder, "com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolConsumptionActionTask",
                    api.getCallerCompanyId());
            addPluginIfAbsent(configurationBuilder, "com.sapienter.jbilling.server.user.tasks.EventBasedCustomNotificationTask",
                    api.getCallerCompanyId());
            configurationBuilder.build();

            CATEGORY_ID = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(true).build();
            PRODUCT_ID = envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(CATEGORY_ID)
                    .withFlatPrice("0.50").build();
        });
    }

    private TestBuilder getTestEnvironment() {
        return partialTestBuilder.givenForMultiple(envCreator -> {

            final JbillingAPI api = envCreator.getPrancingPonyApi();

            final Integer feeItemId = envCreator.itemBuilder(api).item().withCode(FEE_PRODUCT_CODE).global(true).withType(CATEGORY_ID)
                    .withFlatPrice("0.10").build();
            
            envCreator.usagePoolBuilder(api, FUP_CODE).withQuantity("100").withResetValue("Reset To Initial Value")
                    .addItemId(PRODUCT_ID).addItemTypeId(CATEGORY_ID).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                            String.valueOf(environmentHelper.getUsagePoolNotificationId(api)), "10", null))
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_FEE, NotificationMediumType.EMAIL, null, "50", 
                            String.valueOf(feeItemId))).build();
        });
    }



    @Test(priority = 1)
    public void testFUPWithConsumptionActionsCreation() {
        testBuilder.test(env -> {

            final JbillingAPI api = env.getPrancingPonyApi();

            UsagePoolWS persistedUsagePool = api.getUsagePoolWS(env.idForCode(FUP_CODE));
            validateUsagePool(persistedUsagePool, Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS, Integer.valueOf(1),
                    BigDecimal.valueOf(100), "Reset To Initial Value", new Integer[]{env.idForCode(PRODUCT_CODE)},
                    new Integer[]{env.idForCode(CATEGORY_CODE)});
            List<UsagePoolConsumptionActionWS> usagePoolConsumptionActions = persistedUsagePool.getConsumptionActions();
            assertEquals(Integer.valueOf(usagePoolConsumptionActions.size()), Integer.valueOf(2), "Invalid number of actions!");
            Collections.sort(usagePoolConsumptionActions, (o1, o2) -> o1.getPercentage().compareTo(o2.getPercentage()));
            validateConsumptionAction(usagePoolConsumptionActions.get(0), NotificationMediumType.EMAIL, "10",
                    Constants.FUP_CONSUMPTION_NOTIFICATION, null, String.valueOf(environmentHelper.getUsagePoolNotificationId(api)));
            validateConsumptionAction(usagePoolConsumptionActions.get(1), null, "50",
                    Constants.FUP_CONSUMPTION_FEE, String.valueOf(env.idForCode(FEE_PRODUCT_CODE)), null);
        });
    }

    @Test(priority = 2)
    public void testOrderWithPlanSubscriptionAndFUP(){

        final String customerCode = "FUPCustomer1";
        final String subscriptionCode = "TestSubscriptionId";
        final String planCode = "MobileCalls";
        final String orderCode = "TestOrder";

        createEnvWithFUPSubscriptionOrder(customerCode, subscriptionCode, planCode, orderCode,
                Date.from(LocalDate.of(1999, 3, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()), null)
            .test(env -> {

                final JbillingAPI api = env.getPrancingPonyApi();
                Integer userId = env.idForCode(customerCode);
                Integer customerId = api.getUserWS(userId).getCustomerId();
                CustomerUsagePoolWS[] customerUsagePool = api.getCustomerUsagePoolsByCustomerId(customerId);
                assertNotNull(customerUsagePool, "Customer usage pools expected!");
                assertEquals(Integer.valueOf(customerUsagePool.length), Integer.valueOf(1), "Invalid number of customer usage pools!");
                validateCustomerUsagePool(customerUsagePool[0], customerId, userId, BigDecimal.valueOf(100), env.idForCode(planCode),
                        env.idForCode(FUP_CODE));

                //ToDo no api call for customerUsagePool deletion exists, no way to deleted them
                //ToDo this causes exception during usage pool cleanup, but the test passes.

            });

    }

    @Test(priority = 3)
    public void testExpiredFUP(){

        final String specificCustomerCode = "SpecificTestCustomer";
        final String subscriptionCode = "TestSubscriptionId";
        final String planCode = "MobileCalls";
        final String orderCode = "TestOrder";

        createEnvWithFUPSubscriptionOrder(specificCustomerCode, subscriptionCode, planCode, orderCode,
                Date.from(LocalDate.of(1999, 3, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()), null)

                .given(envBuilder -> {
                    envBuilder.getPrancingPonyApi().deleteOrder(envBuilder.env().idForCode(orderCode));
                })
                .test(env -> {
                    final JbillingAPI api = env.getPrancingPonyApi();
                    Integer userId = env.idForCode(specificCustomerCode);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    CustomerUsagePoolWS[] customerUsagePools = api.getCustomerUsagePoolsByCustomerId(customerId);
                    assertNotNull(customerUsagePools, "Customer usage pools expected!");
                    assertEquals(Integer.valueOf(customerUsagePools.length), Integer.valueOf(0), "Invalid number of customer usage pools!");
                });

    }

    @Test(priority = 4)
    public void testFUPConsumption(){

        final String customerCode = "FUPCustomer2";
        final String subscriptionCode = "TestSubscriptionId";
        final String planCode = "MobileCalls";
        final String orderCode = "TestOrder";

        createEnvWithFUPSubscriptionOrder(customerCode, subscriptionCode, planCode, orderCode,
                Date.from(LocalDate.of(1999, 3, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()), null)
                .given(envBuilder -> {

                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    testHelper.buildAndPersistOrder(envBuilder, "TestOneTime", testBuilder.getTestEnvironment().idForCode(customerCode),
                            new Date(), null, environmentHelper.getOrderPeriodOneTime(api), api,
                            buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(PRODUCT_CODE), BigDecimal.valueOf(10)));

                })
                .test(env -> {

                    final JbillingAPI api = env.getPrancingPonyApi();
                    Integer userId = env.idForCode(customerCode);
                    Integer customerId = api.getUserWS(userId).getCustomerId();
                    CustomerUsagePoolWS customerUsagePool = api.getCustomerUsagePoolsByCustomerId(customerId)[0];
                    validateCustomerUsagePool(customerUsagePool, customerId, userId, BigDecimal.valueOf(90),
                            env.idForCode(planCode), env.idForCode(FUP_CODE));
                });

    }

    @Test(priority = 5)
    public void testFUPActionConsumptionNotifications(){

        final String customerCode = "FUPCustomer3";
        final String subscriptionCode = "TestSubscriptionId";
        final String planCode = "MobileCalls";
        final String orderCode = "TestOrder";
        final String emailTestNotificationFile = "./resources/emails_sent.txt";

        //This test doesn't work in a multi node environment
        if(TestEnvironment.isMultiNode()) {
            return;
        }

        createEnvWithFUPSubscriptionOrder(customerCode, subscriptionCode, planCode, orderCode,
                Date.from(LocalDate.of(1999, 3, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()), null)
            .given(envBuilder -> {

                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                FileHelper.deleteFile(emailTestNotificationFile);

                testHelper.buildAndPersistOrder(envBuilder, "TestOneTime", testBuilder.getTestEnvironment().idForCode(customerCode),
                        new Date(), null, environmentHelper.getOrderPeriodOneTime(api), api,
                        buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(PRODUCT_CODE), BigDecimal.valueOf(20)));

            })
            .test(env -> {

                final JbillingAPI api = env.getPrancingPonyApi();
                UserWS user = api.getUserWS(env.idForCode(customerCode));
                String email = user.getMetaFields()[0].getStringValue();
                UsagePoolWS usagePool = api.getUsagePoolWS(env.idForCode(FUP_CODE));
                String percentage = usagePool.getConsumptionActions()
                        .stream().filter(consumptionActionWS -> consumptionActionWS.getType().equals(Constants.FUP_CONSUMPTION_NOTIFICATION))
                        .collect(Collectors.toList()).get(0).getPercentage();
                List<String> lines = FileHelper.readLines(emailTestNotificationFile);
                assertNotNull(lines, "Lines expected!");
                assertEquals(Integer.valueOf(lines.size()), Integer.valueOf(7), "Invalid number of lines!");
                assertEquals(lines.get(1), String.format("To: %s", email), "Invalid line content!");
                assertEquals(lines.get(2), "From: admin@prancingpony.me", "Invalid line content!");
                assertEquals(lines.get(3), "Subject: Usage pool consumption status.", "Invalid line content!");
                assertEquals(lines.get(4), String.format("Body: Dear %s, You have used %s%% from Free Usage Pool - %s. Thanks.",
                        user.getUserName(), percentage, FUP_CODE), "Invalid line content!");
                assertEquals(lines.get(5), "Attachement: null", "Invalid line content!");
                FileHelper.deleteFile(emailTestNotificationFile);
                OrderWS order = api.getLatestOrder(env.idForCode(customerCode));

                // not managed by the testing framework
                api.deleteOrder(order.getId());
            });

    }

    @Test(priority = 6)
    public void testFUPFeeActionTriggered(){

        final String customerCode = "FUPCustomer4";
        final String subscriptionCode = "TestSubscriptionId";
        final String planCode = "MobileCalls";
        final String orderCode = "TestOrder";
        final String emailTestNotificationFile = "./resources/emails_sent.txt";

        //This test doesn't work in a multi node environment
        if(TestEnvironment.isMultiNode()) {
            return;
        }

        createEnvWithFUPSubscriptionOrder(customerCode, subscriptionCode, planCode, orderCode,
                Date.from(LocalDate.of(1999, 3, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()), null)
                .given(envBuilder -> {

                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    FileHelper.deleteFile(emailTestNotificationFile);

                    testHelper.buildAndPersistOrder(envBuilder, "TestOneTime", testBuilder.getTestEnvironment().idForCode(customerCode),
                            new Date(), null, environmentHelper.getOrderPeriodOneTime(api), api,
                            buildProductQuantityEntry(testBuilder.getTestEnvironment().idForCode(PRODUCT_CODE), BigDecimal.valueOf(50)));

                })
                .test(env -> {

                    final JbillingAPI api = env.getPrancingPonyApi();
                    OrderWS order = api.getLatestOrder(env.idForCode(customerCode));
                    assertNotNull(order, "Null order!");
                    assertEquals(order.getPeriod(), environmentHelper.getOrderPeriodOneTime(api), "Invalid order period!");
                    assertEquals(order.getTotalAsDecimal().setScale(2, RoundingMode.CEILING),
                            BigDecimal.valueOf(0.10d).setScale(2, RoundingMode.CEILING), "Invalid order total!");
                    assertEquals(Integer.valueOf(order.getOrderLines().length), Integer.valueOf(1), "Invalid number of order lines!");
                    OrderLineWS orderLine = order.getOrderLines()[0];
                    assertEquals(orderLine.getItemId(), env.idForCode(FEE_PRODUCT_CODE), "Invalid order line item!");
                    assertEquals(orderLine.getAmountAsDecimal().setScale(2, RoundingMode.CEILING),
                            BigDecimal.valueOf(0.10d).setScale(2, RoundingMode.CEILING), "Invalid order line amount!");
                    assertEquals(orderLine.getQuantityAsDecimal().setScale(2, RoundingMode.CEILING),
                            BigDecimal.valueOf(1).setScale(2, RoundingMode.CEILING), "Invalid order line quantity!");

                    // not managed by the testing framework
                    api.deleteOrder(order.getId());
                });

    }

    @Test(priority = 7)
    public void testNotUsedFUPDeletion() {
        final String fupCode = "DeletedFUP";
        final Integer[] initialNumberOfPools = {null};
        testBuilder.given(envBuilder -> {

            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            initialNumberOfPools[0] = api.getAllUsagePools().length;

            envBuilder.usagePoolBuilder(api, fupCode).withQuantity("10").withResetValue("Zero")
                    .addItemId(testBuilder.getTestEnvironment().idForCode(PRODUCT_CODE))
                    .addItemTypeId(testBuilder.getTestEnvironment().idForCode(CATEGORY_CODE))
                    .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(fupCode)
                    .build();

        }).test(env -> {
            final JbillingAPI api = env.getPrancingPonyApi();
            assertEquals(Integer.valueOf(api.getAllUsagePools().length), Integer.valueOf(initialNumberOfPools[0] + 1));
            api.deleteUsagePool(env.idForCode(fupCode));
            assertEquals(Integer.valueOf(api.getAllUsagePools().length), initialNumberOfPools[0]);
        });
    }

    @Test(priority = 8, expectedExceptions = SessionInternalError.class,
    expectedExceptionsMessageRegExp = ".*UsagePoolWS,id,usage.pool.in.use.*")
    public void testFUPDeletionFailed(){

        final String customerCode = "FUPCustomer5";
        final String subscriptionCode = "TestSubscriptionId";
        final String planCode = "MobileCalls";
        final String orderCode = "TestOrder";

        createEnvWithFUPSubscriptionOrder(customerCode, subscriptionCode, planCode, orderCode,
                Date.from(LocalDate.of(1999, 3, 3).atStartOfDay(ZoneId.systemDefault()).toInstant()), null)
            .test(testEnvironment -> {
                final JbillingAPI api = testEnvironment.getPrancingPonyApi();
                api.deleteUsagePool(testEnvironment.idForCode(FUP_CODE));
            });
    }


    @Test(priority = 9, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,attribute_key,usagePool.error.attribute.key.must.be.positive.integer.*")
    public void testFUPWithNegativeConsumptionActionPercentage(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {
            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                            String.valueOf(environmentHelper.getUsagePoolNotificationId(api)), "-1", null))
                    .build();
        });
    }

    @Test(priority = 10, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,attribute_key,usagePool.error.attribute.key.must.be.positive.integer.*")
    public void testFUPWithInvalidConsumptionActionPercentage(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                            String.valueOf(environmentHelper.getUsagePoolNotificationId(api)), "I'm not a number I'm a free man", null))
                    .build();
        });
    }

    @Test(priority = 11, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,attribute_value,usagePool.error.attribute.value.empty.*")
    public void testFUPWithNullConsumptionActionType(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(null, NotificationMediumType.EMAIL,
                            String.valueOf(environmentHelper.getUsagePoolNotificationId(api)), "10", null))
                    .build();
        });
    }

    @Test(priority = 12, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,attribute_value,usagePool.error.attribute.value.empty.*")
    public void testFUPWithEmptyConsumptionActionType(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction("", NotificationMediumType.EMAIL,
                            String.valueOf(environmentHelper.getUsagePoolNotificationId(api)), "10", null))
                    .build();
        });
    }

    @Test(priority = 13, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,notification,usagePool.error.attribute.notification.info.empty.*")
    public void testFUPWithNullConsumptionActionMediumType(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, null,
                            String.valueOf(environmentHelper.getUsagePoolNotificationId(api)), "10", null))
                    .build();
        });
    }

    @Test(priority = 14, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,notification,usagePool.error.attribute.notification.info.empty.*")
    public void testFUPWithNullConsumptionActionNotificationId(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                            null, "10", null))
                    .build();
        });
    }

    @Test(priority = 15, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,notification,usagePool.error.attribute.notification.info.empty.*",
            enabled = false)
    public void testFUPWithEmptyConsumptionActionNotificationId(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                            "", "10", null))
                    .build();
        });
    }

    @Test(priority = 16, expectedExceptions = SessionInternalError.class, enabled = false)
    public void testFUPWithInvalidConsumptionActionNotificationMediumType(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_NOTIFICATION, NotificationMediumType.EMAIL,
                            "I'm not a number I'm a free man", "10", null))
                    .build();
        });
    }

    @Test(priority = 17, expectedExceptions = SessionInternalError.class,
        expectedExceptionsMessageRegExp = ".*UsagePoolWS,productId,usagePool.error.attribute.fee.empty.*")
    public void testFUPWithNullConsumptionActionFeeItemId(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_FEE, NotificationMediumType.EMAIL, null, "50", null))
                    .build();
        });
    }

    @Test(priority = 18, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,productId,usagePool.error.attribute.fee.empty.*")
    public void testFUPWithEmptyConsumptionActionFeeItemId(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_FEE, NotificationMediumType.EMAIL, null, "50", ""))
                    .build();
        });
    }

    @Test(priority = 19, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,productId,usagePool.error.attribute.product.not.exist.*")
    public void testFUPWithNotExistingConsumptionActionFeeItemId(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);

            envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                    .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                    .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                    .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_FEE, NotificationMediumType.EMAIL, null, "50", String.valueOf(Integer.MAX_VALUE)))
                    .build();
        });
    }

    @Test(priority = 20, expectedExceptions = SessionInternalError.class,
            expectedExceptionsMessageRegExp = ".*UsagePoolWS,productId,usagePool.error.attribute.product.not.exist.*")
    public void testFUPWithNotAvailableConsumptionActionFeeItemId(){
        final String INVALID_FUP_CODE = "Invalid FUP code";
        partialTestBuilder.test((testEnvironment, envCreator) -> {

            final JbillingAPI api = testEnvironment.getPrancingPonyApi();
            JbillingAPI resellerApi = testEnvironment.getResellerApi();
            Integer categoryId = testEnvironment.idForCode(CATEGORY_CODE);
            Integer itemId = testEnvironment.idForCode(PRODUCT_CODE);
            Integer feeItemId = envCreator.itemBuilder(resellerApi).item().withCode(FEE_PRODUCT_CODE).global(false)
                    .withType(CATEGORY_ID).withFlatPrice("0.10").build();

            try {
                envCreator.usagePoolBuilder(api, INVALID_FUP_CODE).withQuantity("10").withResetValue("Reset To Initial Value")
                        .addItemId(itemId).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                        .withCyclePeriodValue(Integer.valueOf(1)).withName(INVALID_FUP_CODE)
                        .addConsumptionAction(buildConsumptionAction(Constants.FUP_CONSUMPTION_FEE, NotificationMediumType.EMAIL, null, "50", String.valueOf(feeItemId)))
                        .build();

            } finally {
                resellerApi.deleteItem(feeItemId);
            }
        });
    }

    private void addPluginIfAbsent(ConfigurationBuilder configurationBuilder, String pluginClassName, Integer entityId){

        if (!configurationBuilder.pluginExists(pluginClassName, entityId)){
            configurationBuilder.addPlugin(pluginClassName);
        }
    }


    private void validateUsagePool(UsagePoolWS usagePool, String periodUnit,
                                   Integer periodValue, BigDecimal usagePoolQty,
                                   String resetValue, Integer[] itemsIds,
                                   Integer[] itemTypesIds){

        assertNotNull(usagePool, "Usage pool not found!!");
        assertEquals(usagePool.getCyclePeriodUnit(), periodUnit, "Invalid period unit!");
        assertEquals(usagePool.getCyclePeriodValue(), periodValue, "Invalid period value!");
        assertEquals(usagePool.getQuantityAsDecimal().setScale(2, RoundingMode.CEILING),
                usagePoolQty.setScale(2, BigDecimal.ROUND_CEILING), "Invalid quantity!");
        assertEquals(usagePool.getUsagePoolResetValue(), resetValue, "Invalid reset value!");
        assertEquals(usagePool.getItems(), itemsIds, "Invalid items ids!");
        assertEquals(usagePool.getItemTypes(), itemTypesIds, "Invalid items ids!");

    }

    private void validateConsumptionAction(UsagePoolConsumptionActionWS consumptionAction, NotificationMediumType mediumType,
                                           String percentage, String type, String productId, String notificationId){

        assertNotNull(consumptionAction, "Action can not be null!");
        assertEquals(consumptionAction.getMediumType(), mediumType, "Invalid medium type!");
        assertEquals(consumptionAction.getPercentage(), percentage, "Invalid percentage!");
        assertEquals(consumptionAction.getType(), type, "Invalid type!");
        assertEquals(consumptionAction.getProductId(), productId, "Invalid product id!");
        assertEquals(consumptionAction.getNotificationId(), notificationId, "Invalid notification id!");
    }

    private void validateCustomerUsagePool(CustomerUsagePoolWS customerUsagePool,
                                           Integer customerId, Integer userId,
                                           BigDecimal qty, Integer planId,
                                           Integer usagePoolId) {

        assertNotNull(customerUsagePool, "Customer usage pool can not be null!");
        assertEquals(customerUsagePool.getCustomerId(), customerId, "Customer id invalid!");
        assertEquals(customerUsagePool.getUserId(), userId, "User id invalid!");
        assertEquals(customerUsagePool.getQuantityAsDecimal().setScale(2, RoundingMode.CEILING),
                qty.setScale(2, BigDecimal.ROUND_CEILING), "Invalid quantity!");
        assertEquals(customerUsagePool.getPlanId(), planId, "Plan id invalid!");
        assertEquals(customerUsagePool.getUsagePoolId(), usagePoolId, "Usage pool id invalid!");
    }


    private UsagePoolConsumptionActionWS buildConsumptionAction(String type, NotificationMediumType mediumType,
                                                                String notificationId, String percentage,
                                                                String productId){

        return UsagePoolBuilder.consumptionActionBuilder()
                .withType(type)
                .withMediumType(mediumType)
                .withNotificationId(notificationId)
                .withPercentage(percentage)
                .withProductId(productId)
                .build();
    }

    private PlanItemWS buildPlanItem(Integer itemId, Integer periodId, String qty, PriceModelWS price){

        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(price)
                .addModel(price)
                .withBundledPeriodId(periodId)
                .withBundledQuantity(qty)
                .build();
    }

    private TestBuilder createEnvWithFUPSubscriptionOrder(String customerCode ,String subscriptionCode, String planCode,
                                                          String orderCode, Date activeSince, Date activeUntil){

        return testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            final Integer subscriptionPlanItemId = envBuilder.itemBuilder(api).item().withCode(subscriptionCode)
                    .global(false).withType(testBuilder.getTestEnvironment().idForCode(CATEGORY_CODE)).withFlatPrice("1")
                    .build();

            envBuilder.planBuilder(api, planCode).withPeriodId(environmentHelper.getOrderPeriodMonth(api))
                    .withItemId(subscriptionPlanItemId).addUsagePoolId(testBuilder.getTestEnvironment().idForCode(FUP_CODE))
                    .addPlanItem(buildPlanItem(testBuilder.getTestEnvironment().idForCode(PRODUCT_CODE), environmentHelper.getOrderPeriodMonth(api), "1",
                            new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, api.getCallerCurrencyId())))
                    .build();

            final Date date = Date.from(LocalDate.of(1999, 3, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Integer customerId = testHelper.buildAndPersistCustomer(envBuilder, customerCode, date, environmentHelper.getOrderPeriodMonth(api), api);
            
            envBuilder.orderBuilder(api).withCodeForTests(orderCode).forUser(customerId).withActiveSince(activeSince)
                    .withActiveUntil(activeUntil).withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .withProducts(subscriptionPlanItemId).build();
        });

    }

    private Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity){

        Map<Integer, BigDecimal> mapEntry = new HashMap<>();
        mapEntry.put(productId, quantity);
        return mapEntry;
    }
}
