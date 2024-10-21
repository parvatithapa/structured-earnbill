package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNull;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;

import java.time.LocalDate;
import java.time.ZoneId;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.math.BigDecimal;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import com.sapienter.jbilling.server.order.task.OrderChangeUpdateTask;
/**
 *
 * This class contains the test methods to test the fix done for ticket
 * JBSPC-881 The asserts are added around the order line, invoice lines and JMR
 * amounts and the tax amounts
 *
 * */
@Test(groups = "agl", testName = "agl.PendingOrderChangesUpdateTest")
public class PendingOrderChangesUpdateTest extends SPCBaseConfiguration {

    String assetIdentifier1 = "04" + randomLong(10000000L, 99999999L);
    String assetIdentifier2 = "61" + randomLong(10000000L, 99999999L);
    Integer userId1;
    Integer assetId1;
    Integer planId;
    Integer productId;
    Integer categoryId;
    String DISCOUNT = "Discount";
    Integer firstPlanOrderId;
    Date activeSinceDate = null;
    Date activeUntilDate = null;
    String planCode = "SPCMO-1147";
    String planType = "Optus";
    String planCategory = "Mobile";
    String planDescription = "Optus Budget - $10";
    String planOrigin = "SPC";
    String planRating = "SPC-OM-Plan-Rating-1";
    String price = "9.0909";
    String dataBoostQuantity = "1024"; // Quantity In MegaBytes
    String mainPoolQuantity = "2147483648"; // Quantity In bytes
    List<Integer> invoicesGenerated = new ArrayList<Integer>();
    String userName1 = "Test-" + randomLong(1000L, 1999L);
    String assetName1 = "Asset-" + randomLong(1000L, 1999L);
    LocalDate baseDate = LocalDate.now().withDayOfMonth(1);
    Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
    Integer discountOrderId = null;

    public static final String PRICE = "-10.0000";
    private static final String FUTURE_DATE = "future_date";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @BeforeClass
    public void initialize() {
        logger.debug("OptusMobileDataMediationTest.initialize started....", testBuilder);
        if (null == testBuilder) {
            testBuilder = getTestEnvironment();
        }
        logger.debug("OptusMobileDataMediationTest.initialize completed....", testBuilder);
    }

    @AfterClass
    public void afterTests() {

        logger.debug("PendingOrderChangesUpdateTest.afterTests started.....");
        logger.debug("cleaning the data of user created in test runs....");
        clearTestDataForUser(userId1);
        logger.debug("completed the cleaning process....");
    }

    /**
     *
     * This method created a Subscription order with active since date in future
     * Initially Order is created with status as PENDING
     * The OrderChangeUpdateTask is called with the same date as that of active since of the order
     * The Order status should move to APPLY now
     * There should not be any error in order change
     *
     * */
    @Test(enabled = true, priority = 1)
    void test01PendingOrderChangesUpdateTest() {

        testBuilder
                .given(envBuilder -> {
                    api = testBuilder.getTestEnvironment().getPrancingPonyApi();

                    Date nextInvoiceDate = Date.from(baseDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                    UserWS spcTestUserWS = getSPCTestUserWS(envBuilder, userName1, nextInvoiceDate, "", CUSTOMER_TYPE_VALUE_POST_PAID,
                            AUSTRALIA_POST, CC);

                    userId1 = spcTestUserWS.getId();
                    assertNotNull("User Creation Failed", userId1);
                    logger.debug("User created, user id is : {}", userId1);

                	planId = CreatePlanUtility.createPlan(api, planCode, planType, planCategory, planDescription, planOrigin, planRating,
                                "x", new BigDecimal(price), true, new BigDecimal(mainPoolQuantity), 3, new BigDecimal(dataBoostQuantity));
                    logger.debug("Plan created with Id : {}", planId);
                    PlanWS planWS = api.getPlanWS(planId);
                    assertNotNull("Plan creation failed", planWS);
                    List<AssetWS> assetWSs = new ArrayList<>();
                    productQuantityMap.put(planWS.getItemId(), BigDecimal.ONE);
                    assetId1 = buildAndPersistAssetWithServiceId(envBuilder,
                            getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_NUMBERS_CATEGORY),
                            getItemIdByCode(testBuilder.getTestEnvironment(), USAGE_PRODUCT_CODE_MOBILE_NUMBERS), assetIdentifier1,
                            assetName1, assetIdentifier1);

                    assertNotNull("Asset Creation Failed", assetId1);
                    logger.debug("Asset created with Id : {}", assetId1);
                    assetWSs.add(api.getAsset(assetId1));
                    activeSinceDate = Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());
                    logger.debug("Active since for the order is {}", activeSinceDate);
                    firstPlanOrderId = createOrderWithAsset("TestOrder", userId1, activeSinceDate, null, 2, 1, true, productQuantityMap,
                            assetWSs, planId, assetIdentifier1);
                    assertNotNull("First subscription order Creation Failed", firstPlanOrderId);
                    logger.debug("First Subscription order id {} for user {}", firstPlanOrderId, userId1);
                })
                .validate((testEnv, testEnvBuilder) -> {

                    logger.debug("Checking the Order changes....");
                    validateOrder(firstPlanOrderId, "Active", new BigDecimal(price), activeSinceDate, false);

                    OrderChangeWS[] orderchangeWS = api.getOrderChanges(firstPlanOrderId);
                    for(OrderChangeWS orderchange : orderchangeWS) {
                        logger.debug("Order change status is {}"+orderchange.getStatus());
                        assertEquals("Invalid order change status", "PENDING", orderchange.getStatus());
                    }
                    ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                    configureOrderChangeUpdateTask(confBuilder);
                    confBuilder.build();

                    PluggableTaskWS pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(OrderChangeUpdateTask.class.getName())
                            .getId());

                    Map<String, String> orderChangeUpdateTaskParams = pluggableTask.getParameters();
                    orderChangeUpdateTaskParams.put(FUTURE_DATE, getDateFormatted(LocalDate.now().plusDays(5),"yyyy-MM-dd"));
                    updateExistingPlugin(api, pluggableTask.getId(), OrderChangeUpdateTask.class.getName(), orderChangeUpdateTaskParams);
                    confBuilder.build();
                    logger.debug("Updated the plugin OrderChangeUpdateTask FUTURE_DATE as .."+getDateFormatted(LocalDate.now().plusDays(5),"yyyy-MM-dd"));

                    pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(OrderChangeUpdateTask.class.getName())
                            .getId());

                    Integer orderchnageUpdateTaskPluginId = pluggableTask.getId();
                    api.triggerScheduledTask(orderchnageUpdateTaskPluginId, new Date());
                    waitFor(5L);

                    orderchangeWS = api.getOrderChanges(firstPlanOrderId);
                    for(OrderChangeWS orderchange : orderchangeWS) {
                        logger.debug("Order change status checked again is {}"+orderchange.getStatus());
                        assertNull("The order error codes is ",orderchange.getErrorCodes());
                        assertNull("The order error message is ",orderchange.getErrorMessage());
                        assertEquals("Invalid order change status", "APPLY", orderchange.getStatus());
                    }
                     validateOrder(firstPlanOrderId, "Active", new BigDecimal(price), activeSinceDate, true);
                });
    }

    /**
     *
     * This method created a one time discount order with active since date in future
     * Initially Order is created with status as PENDING
     * The OrderChangeUpdateTask is called with the same date as that of active since of the order
     * The Order status should move to APPLY now
     * There should not be any error in order change
     *
     * */
    @Test(enabled = true, priority = 2)
    void test02PendingOrderChangesUpdateTest() {

        testBuilder
                .given(envBuilder -> {
                    api = testBuilder.getTestEnvironment().getPrancingPonyApi();

                    AssetWS assetWS = api.getAsset(assetId1);
                    MetaFieldWS metaFieldWS1 = ApiBuilderHelper.getMetaFieldWS(MF_NAME_SERVICE_ID, DataType.STRING, EntityType.ORDER_LINE,
                            api.getCallerCompanyId());
                    metaFieldWS1.setMandatory(false);
                    categoryId =  getCategoryIdByName(testBuilder.getTestEnvironment(), MOBILE_SERVICES_CATEGORY) ;
                    productId =  buildAndPersistFlatProductForMultipleCategoriesWithOrderLineMetaField(envBuilder, api, "Flat Discount", DISCOUNT,
                            false, Arrays.asList(new Integer[]{categoryId}), "0.00", true, 0, false, metaFieldWS1);

                    activeSinceDate = Date.from(LocalDate.now().plusDays(5).atStartOfDay(ZoneId.systemDefault()).toInstant());

                    activeUntilDate = Date.from(LocalDate.now().plusDays(50).atStartOfDay(ZoneId.systemDefault()).toInstant());

                    productQuantityMap.clear();
                    productQuantityMap.put(productId, BigDecimal.ONE);

                    discountOrderId = createServiceOrder("discountOrderId01", activeSinceDate, activeUntilDate,
        					MONTHLY_ORDER_PERIOD, false, productQuantityMap, 
        					null, userId1, PRICE, "", null);
                    logger.debug("Discount order id is : {}"+discountOrderId);
                    assertNotNull("Discount order not created", discountOrderId);

                })
                .validate((testEnv, testEnvBuilder) -> {

                    logger.debug("Checking the Order changes....");
                    validateOrder(discountOrderId, "Active", new BigDecimal(PRICE), activeSinceDate, false);

                    OrderChangeWS[] orderchangeWS = api.getOrderChanges(discountOrderId);
                    for(OrderChangeWS orderchange : orderchangeWS) {
                        logger.debug("Order change status is {}"+orderchange.getStatus());
                        assertEquals("Order change status should be PENDING", "PENDING", orderchange.getStatus());
                    }
                    ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                    configureOrderChangeUpdateTask(confBuilder);

                    PluggableTaskWS pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(OrderChangeUpdateTask.class.getName())
                            .getId());

                    Map<String, String> orderChangeUpdateTaskParams = pluggableTask.getParameters();
                    orderChangeUpdateTaskParams.put(FUTURE_DATE, getDateFormatted(LocalDate.now().plusDays(5),"yyyy-MM-dd"));
                    updateExistingPlugin(api, pluggableTask.getId(), OrderChangeUpdateTask.class.getName(), orderChangeUpdateTaskParams);
                    confBuilder.build();
                    logger.debug("Updated the plugin OrderChangeUpdateTask FUTURE_DATE as .."+getDateFormatted(LocalDate.now().plusDays(5),"yyyy-MM-dd"));

                    pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(OrderChangeUpdateTask.class.getName())
                            .getId());

                    Integer orderchnageUpdateTaskPluginId = pluggableTask.getId();
                    api.triggerScheduledTask(orderchnageUpdateTaskPluginId, new Date());
                    waitFor(5L);

                    orderchangeWS = api.getOrderChanges(discountOrderId);
                    for(OrderChangeWS orderchange : orderchangeWS) {
                        logger.debug("Order change status checked again is {}"+orderchange.getStatus());
                        assertNull("The order error codes is ",orderchange.getErrorCodes());
                        assertNull("The order error message is ",orderchange.getErrorMessage());
                        assertEquals("Invalid order change status", "APPLY", orderchange.getStatus());
                    }
                    
                     validateOrder(discountOrderId, "Active", new BigDecimal(PRICE), activeSinceDate, true);
                });
    }
    
	private void validateOrder(Integer orderId, String orderStatus, BigDecimal orderAmount, Date activeSinceDate, boolean applyOrderChange) {

		OrderWS orderWs = api.getOrder(orderId);
		logger.debug("orderWs.getStatusStr(): {}", orderWs.getStatusStr());
		assertEquals("Order status should be ", orderStatus, orderWs.getStatusStr());
		if (applyOrderChange) {
			BigDecimal calculatedOrderAmount = Arrays.stream(orderWs.getOrderLines())
					.map(line -> line.getAmountAsDecimal())
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			assertEquals("Order Amount not matched", orderAmount.setScale(4), calculatedOrderAmount.setScale(4));
		}
		assertEquals("Order active since not matched", activeSinceDate, orderWs.getActiveSince());
	}

}
