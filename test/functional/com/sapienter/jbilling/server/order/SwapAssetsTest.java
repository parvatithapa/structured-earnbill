package com.sapienter.jbilling.server.order;

import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.SwapAssetWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;

@Test(groups = { "web-services", "order" }, testName = "order.SwapAssetsTest")
public class SwapAssetsTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private TestBuilder       testBuilder;
    private EnvironmentHelper envHelper;
    private static final Integer CC_PM_ID                                      = 5;
    private static final String  ACCOUNT_NAME                                  = "Test Account "+ UUID.randomUUID().toString();
    private static final int     NEXT_INVOICE_DAY                              =  1;
    private static final int     MONTHLY_ORDER_PERIOD                          =  2;
    private static final int     ORDER_CHANGE_STATUS_APPLY_ID                  =  3;
    private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID         = 320104;
    private static final String  TEST_USER_1                                   = "user "+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "user "+ UUID.randomUUID().toString();
    private static final String  TEST_USER_3                                   = "user "+ UUID.randomUUID().toString();
    private static final String  ORDER_01                                      = "testSubScriptionOrderO1"+ UUID.randomUUID().toString();
    private static final String  ORDER_02                                      = "testSubScriptionOrderO2"+ UUID.randomUUID().toString();
    private static final String  ORDER_03                                      = "testSubScriptionOrderO3"+ UUID.randomUUID().toString();

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            // Creating account type
            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
        });

    }

    @Test
    public void test01SwapAsset() {
        String asset1 = RandomStringUtils.random(6, true, true);
        String asset2 = RandomStringUtils.random(6, true, true);
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate);
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_1)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate)
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .build();
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_1));
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnv.getPrancingPonyApi();
            ItemDTOEx item = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);
            Integer asset = buildAndPersistAsset(testEnvBuilder, item.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, asset1);
            logger.debug("asset 1 {} created", asset);

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Integer orderId = createOrder(ORDER_01 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api), false,
                    productQuantityMap, productAssetMap, TEST_USER_1);
            logger.debug("Order Created {}", orderId);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnv.getPrancingPonyApi();
            ItemDTOEx item = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);
            Integer asset = buildAndPersistAsset(testEnvBuilder, item.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, asset2);
            logger.debug("asset 2 {} created", asset);
            Integer orderId = testEnvBuilder.idForCode(ORDER_01);
            OrderWS order = api.getOrder(orderId);
            api.swapAssets(orderId, new SwapAssetWS[] { new SwapAssetWS(asset1, asset2)});
            Assert.assertTrue(asset + " not found on order "+ orderId, isAssetFoundOnOrder(order, asset));
        });
    }

    @Test
    public void test02InvalidSwapAsset() {
        String asset1 = RandomStringUtils.random(6, true, true);
        String asset2 = RandomStringUtils.random(6, true, true);
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate);
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_2)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate)
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .build();
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_2));
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnv.getPrancingPonyApi();
            ItemDTOEx item = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);
            Integer asset = buildAndPersistAsset(testEnvBuilder, item.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, asset1);
            logger.debug("asset 1 {} created", asset);

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Integer orderId = createOrder(ORDER_02 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api), false,
                    productQuantityMap, productAssetMap, TEST_USER_2);
            logger.debug("Order Created {}", orderId);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnv.getPrancingPonyApi();
            Integer orderId = testEnvBuilder.idForCode(ORDER_02);
            try {
                api.swapAssets(orderId, new SwapAssetWS[] { new SwapAssetWS(asset1, asset2) });
                Assert.fail("Asset Swap should fail, since invalid asset " + asset2 + " was passed to api.");
            } catch(SessionInternalError error) {
                logger.error("asset swap failed for invalid asset {}", asset2, error);
            }
        });
    }

    @Test
    public void test03SwapAssetWithEqualAssets() {
        String asset1 = RandomStringUtils.random(6, true, true);
        String asset2 = RandomStringUtils.random(6, true, true);
        testBuilder.given(envBuilder -> {
            Date nextInvoiceDate = Date.from(LocalDate.of(2016, 8, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            logger.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate);
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_3)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate)
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .build();
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_3));
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnv.getPrancingPonyApi();
            ItemDTOEx item = api.getItem(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, null, null);
            Integer asset = buildAndPersistAsset(testEnvBuilder, item.getTypes()[0], TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, asset1);
            logger.debug("asset 1 {} created", asset);

            Map<Integer, List<Integer>> productAssetMap = new HashMap<>();
            productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, Arrays.asList(asset));

            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();

            productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);

            Date activeSinceDate = Date.from(LocalDate.of(2016, 7, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            Integer orderId = createOrder(ORDER_03 , activeSinceDate, null, envHelper.getOrderPeriodMonth(api), false,
                    productQuantityMap, productAssetMap, TEST_USER_3);
            logger.debug("Order Created {}", orderId);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnv.getPrancingPonyApi();
            Integer orderId = testEnvBuilder.idForCode(ORDER_03);
            try {
                api.swapAssets(orderId, new SwapAssetWS[] { new SwapAssetWS(asset1, asset1) });
                Assert.fail(String.format("Asset Swap should fail, since euqal assets [%s,%s] was passed to api.", asset1, asset2));
            } catch(SessionInternalError error) {
                logger.error("asset swap failed for euqal assets[{},{}]", asset1, asset2, error);
            }
        });
    }

    private boolean isAssetFoundOnOrder(OrderWS order, Integer assetId) {
        for(OrderLineWS line : order.getOrderLines()) {
            if(Arrays.binarySearch(line.getAssetIds(), assetId)!=-1) {
                return true;
            }
        }
        return false;
    }

    private Integer createOrder(String code,Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode) {
        this.testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
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
                        if(null!=productAssetMap && !productAssetMap.isEmpty()
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
            .withProrate(prorate)
            .withOrderLines(lines)
            .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID)
            .build();
        }).test((testEnv, envBuilder) -> assertNotNull("Order creation failed", envBuilder.idForCode(code)));
        return testBuilder.getTestEnvironment().idForCode(code);
    }

    private Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
        Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                .equals("Available")).collect(Collectors.toList()).get(0).getId();
        return envBuilder.assetBuilder(api)
                .withItemId(itemId)
                .withAssetStatusId(assetStatusId)
                .global(true)
                .withIdentifier(phoneNumber)
                .withCode(phoneNumber)
                .build();
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        testBuilder = null;
    }

}