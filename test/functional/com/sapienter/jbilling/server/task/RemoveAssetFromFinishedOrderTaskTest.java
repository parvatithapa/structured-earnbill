package com.sapienter.jbilling.server.task;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.tasks.RemoveAssetFromFinishedOrderTask;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.task.RemovalOfAssetsFromSubscriptionTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

/**
 *
 * @author Ashish Srivastava
 *@date 18-Dec-2019
 */
@Test(groups = { "integration" }, testName = "RemoveAssetFromFinishedOrderTaskTest")
public class RemoveAssetFromFinishedOrderTaskTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private TestBuilder testBuilder;
    private static final Integer CC_PM_ID = 5;
    private static final String ACCOUNT_NAME = "Remove Asset Test Account For Finished order";
    private static final String PRODUCT_CATEGORY = "Removal Asset Category";
    private static final String TEST_PRODUCT = "SC Removal Asset Test Item";
    private static final String TEST_USER_1 = "Test-User1-" + UUID.randomUUID().toString();
    private static final int MONTHLY_ORDER_PERIOD = 2;
    private static final int NEXT_INVOICE_DAY = 1;
    private static final String REMOVAL_ASSET_PLUGNIN = "Plugin-" + UUID.randomUUID().toString();
    private static final String NUMBER_OF_DAYS = "Number of Days";
    private static final String ASSET01_NUMBER = "0243897012";
    protected static final String ORDER_CREATION_ASSERT         = "Order Creation Failed";
    private static final int ORDER_CHANGE_STATUS_APPLY_ID       = 3;
    private static final String SUBSCRIPTION_ORDER_01                    = "subscription01"+ System.currentTimeMillis();
    private static final String REMOVE_ASSET_FROM_FINISHED_ORDER = "com.sapienter.jbilling.server.item.tasks.RemoveAssetFromFinishedOrderTask";
    Integer categoryId = null;
    Integer[] invoiceIds =null;
    Integer productId = null;
    Integer removeAssetFromSubscTaskId;
    protected JbillingAPI api;

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
        });
    }

    @BeforeClass
    public void before() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            api = envBuilder.getPrancingPonyApi();
            // Creating account type
                buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);

                // Creating product category.
                categoryId = buildAndPersistCategory(envBuilder, api, PRODUCT_CATEGORY, false,
                        ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
                productId = buildAndPersistFlatProduct(envBuilder, api, TEST_PRODUCT, false,
                        envBuilder.idForCode(PRODUCT_CATEGORY), "1", true, 1, false);
                //Deleting old plugin which is not scheduled task to prevent assset removal due to old plugin
                
                PluggableTaskWS removeAssetFromSubscTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(REMOVE_ASSET_FROM_FINISHED_ORDER).getId());
                removeAssetFromSubscTaskId = removeAssetFromSubscTask.getId();
                        if (removeAssetFromSubscTask != null) {
                            api.deletePlugin(removeAssetFromSubscTaskId);
                            removeAssetFromSubscTask = null;
                        }
                        
                PluggableTaskTypeWS removalofAssetFromSubscScheduledTask = api
                        .getPluginTypeWSByClassName(RemoveAssetFromFinishedOrderTask.class.getName());
                envBuilder
                        .pluginBuilder(api)
                        .withCode(REMOVAL_ASSET_PLUGNIN)
                        .withTypeId(removalofAssetFromSubscScheduledTask.getId())
                        .withOrder(12345678)
                        .withParameter(NUMBER_OF_DAYS, "5")
                        .build();

            }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
            assertNotNull("Product Categroy Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CATEGORY));
            assertNotNull("Product Creation Failed ", testEnvBuilder.idForCode(TEST_PRODUCT));
            assertNotNull("Plugin Creation Failed ", testEnvBuilder.idForCode(REMOVAL_ASSET_PLUGNIN));
        });
    }

    protected Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity) {
        return Collections.singletonMap(productId, quantity);
    }

    @AfterClass
    public void after() {
                          api.deleteInvoice(invoiceIds[0]);
                          testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
                          testBuilder.removeEntitiesCreatedOnJBilling(); testBuilder = null;
                          PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(REMOVE_ASSET_FROM_FINISHED_ORDER);
                          PluggableTaskWS plugIn = new PluggableTaskWS();
                          plugIn.setOwningEntityId(api.getCallerCompanyId());
                          plugIn.setProcessingOrder(555);
                          plugIn.setTypeId(type.getId());
                          removeAssetFromSubscTaskId = api.createPlugin(plugIn);
    }

    // Creating Remove asset PlanItem

    protected Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String flatPrice, boolean allowDecimal, Integer allowAssets,
            boolean isPlan) {
        return envBuilder.itemBuilder(api).item().withCode(code).withType(categoryId).withFlatPrice(flatPrice)
                .global(global).useExactCode(true).allowDecimal(allowDecimal).withAssetManagementEnabled(allowAssets)
                .build();
    }

    protected Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId,
            String phoneNumber) {
        return buildAndPersistAsset(envBuilder, categoryId, itemId, phoneNumber, phoneNumber);
    }

    protected Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId,
            String phoneNumber, String code) {
        ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
        Integer assetStatusId = itemTypeWS
                .getAssetStatuses()
                .stream()
                .filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1
                        && assetStatusDTOEx.getDescription().equals("Available")).collect(Collectors.toList()).get(0)
                .getId();
        return envBuilder.assetBuilder(api).withItemId(itemId).withAssetStatusId(assetStatusId).global(true)
                .withIdentifier(phoneNumber).withCode(code).build();
    }

    @Test
    public void test01SuccessAssetRemovedFromFinishedOrder() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2019);
        nextInvoiceDate.set(Calendar.MONTH, 11);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 7);
        testBuilder
                .given(envBuilder -> {
                    logger.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    UserWS userWS = envBuilder.customerBuilder(api).withUsername(TEST_USER_1)
                            .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                            .addTimeToUsername(false).withNextInvoiceDate(nextInvoiceDate.getTime())
                            .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                            .build();

                    Calendar activeSince = Calendar.getInstance();
                    activeSince.setTime(nextInvoiceDate.getTime());
                    activeSince.add(Calendar.MONTH, -1);

                    Calendar activeUntil = Calendar.getInstance();
                    activeUntil.setTime(nextInvoiceDate.getTime());
                    activeUntil.add(Calendar.DATE, -3);

                    Integer asset1 = buildAndPersistAsset(envBuilder, categoryId, productId, ASSET01_NUMBER, "asset-01"
                            + System.currentTimeMillis());
                    logger.debug("asset created {} for number {}", asset1, ASSET01_NUMBER);
                    
                    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                    productQuantityMap.putAll(buildProductQuantityEntry(productId, BigDecimal.ONE));

                    Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
                    productAssetMap.put(productId, Arrays.asList(asset1));

                    logger.debug("Creating Order with Active Since Date {}", activeSince.getTime());

                    Integer orderId = createOrder(SUBSCRIPTION_ORDER_01, activeSince.getTime(), activeUntil.getTime(), MONTHLY_ORDER_PERIOD,1, false,
                            productQuantityMap, productAssetMap, TEST_USER_1);
                    logger.debug("Subscription order id {} for user {}", orderId, envBuilder.idForCode(TEST_USER_1));
                })
                .validate((testEnv, testEnvBuilder) -> {
                    assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_1));
                    assertNotNull("Order Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ORDER_01));
                })
                .validate((testEnv, testEnvBuilder) -> {
                    JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                    //api.deletePlugin(6098);
                    invoiceIds = api.createInvoiceWithDate(testEnvBuilder.idForCode(TEST_USER_1), nextInvoiceDate.getTime(), null, null, false);
                    api.triggerScheduledTask(testEnvBuilder.idForCode(REMOVAL_ASSET_PLUGNIN), new Date());
                    try {
                        Thread.sleep(2000L); // waiting to start quartz job.
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_1));
                    assertNotNull("Order Not Found!", order);
                    assertEquals("Order Amount Should be ", new BigDecimal("1.00"), order.getTotalAsDecimal()
                            .setScale(2, BigDecimal.ROUND_HALF_UP));
                    assertEquals(order.hasLinkedAssets(), false, "asset Not removed from subcsribed order");
                    UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_1));
                });
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name,
            Integer... paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api).withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId).build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api).itemType().withCode(code).withCategoryType(categoryType).global(global)
                .allowAssetManagement(1).build();
    }
    
    protected Integer createOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId, Integer billingTypeId,
            boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode) {
        this.testBuilder.given(envBuilder -> {
            List<OrderLineWS> lines = productQuantityMap.entrySet()
                    .stream()
                    .map(lineItemQuatityEntry -> {
                        OrderLineWS line = new OrderLineWS();
                        line.setItemId(lineItemQuatityEntry.getKey());
                        line.setTypeId(Integer.valueOf(1));
                        ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                        line.setDescription(item.getDescription());
                        line.setQuantity(lineItemQuatityEntry.getValue());
                        line.setUseItem(true);
                        if (null != productAssetMap && !productAssetMap.isEmpty()
                                && productAssetMap.containsKey(line.getItemId())) {
                            List<Integer> assets = productAssetMap.get(line.getItemId());
                            line.setAssetIds(assets.toArray(new Integer[0]));
                            line.setQuantity(assets.size());
                        }
                        return line;
                    }).collect(Collectors.toList());

            envBuilder.orderBuilder(api)
            .withCodeForTests(code)
            .forUser(envBuilder.idForCode(userCode))
            .withActiveSince(activeSince)
            .withActiveUntil(activeUntil)
            .withEffectiveDate(activeSince)
            .withPeriod(orderPeriodId)
            .withBillingTypeId(billingTypeId)
            .withProrate(prorate)
            .withOrderLines(lines)
            .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID)
            .build();
        }).test((testEnv, envBuilder) ->
        assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code))
                );
        return testBuilder.getTestEnvironment().idForCode(code);
    }
}
