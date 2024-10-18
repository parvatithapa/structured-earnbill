package com.sapienter.jbilling.api.automation.plans;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import org.joda.time.DateTime;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Test cases involving
 * plans, plans meta-fields
 * orders with plans.
 *
 * @author Vojislav Stanojevikj
 * @since 20-Jun-2016.
 */
@Test(groups = {"api-automation"}, testName = "PlansTest")
public class PlansTest {

    private static final String META_FIELD_NAME = "subscription.id";
    private static final String META_FIELD_NAME_2 = "volume";

    private EnvironmentHelper environmentHelper;
    private TestBuilder testBuilder;

    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();
    }

    @AfterClass
    public void tearDown(){
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != environmentHelper){
            environmentHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest().givenForMultiple(testEnvironmentCreator -> {

            environmentHelper = EnvironmentHelper.getInstance(testEnvironmentCreator.getPrancingPonyApi());
        });
    }


    @Test(priority = 1, description = "Validates that a plan subscription order contains order line meta-field values, or the plan subscription item meta-fields.")
    public void testPlanMetaFields(){

        final String planCategoryCode = "PlanCategory";
        final String globalCategoryCode = "GlobalCategory";
        final String globalProductCode = "GlobalProduct";
        final String subscriptionCode = "SubscriptionItem";
        final String planCode = "NP-1";
        final String planDescription = "New Plan";
        final String orderCode = "TestOrder";
        final String customerCode = "PlanTestCustomer";
        final Integer randomSubscriptionId = Integer.valueOf(new Random().nextInt(Integer.MAX_VALUE - 1));
        testBuilder.given(envBuilder -> {


            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer customerId = buildAndPersistDefaultMonthlyCustomer(envBuilder, customerCode, null, api);
            Integer planCategoryId = buildAndPersistCategory(envBuilder, planCategoryCode, true, api);
            Integer subscriptionItemId = createFlatProduct(subscriptionCode, planCategoryId, "0.1", true,
                    envBuilder, api, buildOrderLineMetaFields(api));

            Integer itemCategoryId = buildAndPersistCategory(envBuilder, globalCategoryCode, true, api);
            Integer globalItemId = createFlatProduct(globalProductCode, itemCategoryId, "5", true,
                    envBuilder, api);

            buildAndPersistPlan(envBuilder, planCode, planDescription, environmentHelper.getOrderPeriodMonth(api),
                    subscriptionItemId, api, new ArrayList<>(), buildPlanItem(globalItemId, environmentHelper.getOrderPeriodMonth(api),
                            "1", new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, api.getCallerCurrencyId())));

            buildAndPersistOrderWithOrderLines(envBuilder, orderCode, customerId, new Date(), null,
                    environmentHelper.getOrderPeriodMonth(api), api,
                    buildOrderLine(subscriptionItemId, BigDecimal.ONE, randomSubscriptionId, BigDecimal.valueOf(100), envBuilder, api));

        }).test(env -> {

            final JbillingAPI api = env.getPrancingPonyApi();
            OrderWS order = api.getLatestOrder(env.idForCode(customerCode));
            OrderLineWS[] orderLines = order.getOrderLines();
            assertNotNull(orderLines, "Order lines expected!");
            assertEquals(Integer.valueOf(orderLines.length), Integer.valueOf(2), "Invalid number of order lines!");
            MetaFieldValueWS[] metaFieldValues = orderLines[0].getMetaFields();
            assertNotNull(metaFieldValues, "Meta-fields expected!");
            assertEquals(Integer.valueOf(metaFieldValues.length), Integer.valueOf(2), "Invalid number of meta-fields!");
            Arrays.sort(metaFieldValues, (o1, o2) -> o1.getFieldName().compareTo(o2.getFieldName()));
            validateMetaField(metaFieldValues[0], META_FIELD_NAME, DataType.INTEGER, randomSubscriptionId);
            validateMetaField(metaFieldValues[1], META_FIELD_NAME_2, DataType.DECIMAL, BigDecimal.valueOf(100).setScale(2, RoundingMode.CEILING));
            metaFieldValues = orderLines[1].getMetaFields();
            assertNotNull(metaFieldValues, "Meta-fields expected!");
            assertEquals(Integer.valueOf(metaFieldValues.length), Integer.valueOf(0), "Invalid number of meta-fields!");
        });
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

    private MetaFieldWS[] buildOrderLineMetaFields(JbillingAPI api){
        return new MetaFieldWS[]{
                ApiBuilderHelper.getMetaFieldWS(META_FIELD_NAME, DataType.INTEGER, EntityType.ORDER_LINE, api.getCallerCompanyId()),
                ApiBuilderHelper.getMetaFieldWS(META_FIELD_NAME_2, DataType.DECIMAL, EntityType.ORDER_LINE, api.getCallerCompanyId())};
    }

    private OrderLineWS buildOrderLine(Integer itemId, BigDecimal quantity,
                                       Integer subscriptionIdMetaFieldValue, BigDecimal volumeMetaFieldValue,
                                       TestEnvironmentBuilder testEnvironmentBuilder, JbillingAPI api){

        return testEnvironmentBuilder.orderBuilder(api).orderLine()
                .withItemId(itemId)
                .withQuantity(quantity)
                .withMetaField(ApiBuilderHelper.getMetaFieldValueWS(META_FIELD_NAME, subscriptionIdMetaFieldValue))
                .withMetaField(ApiBuilderHelper.getMetaFieldValueWS(META_FIELD_NAME_2, volumeMetaFieldValue))
                .build();
    }

    private void validateMetaField(MetaFieldValueWS metaFieldValue, String name, DataType dataType, Object value){

        assertNotNull(metaFieldValue, "Meta-Field value can not be null!");
        assertEquals(metaFieldValue.getFieldName(), name, "Invalid meta-field name!");
        assertEquals(metaFieldValue.getMetaField().getDataType(), dataType, "Invalid meta-field data type!");
        if (metaFieldValue.getValue() instanceof BigDecimal){
            assertEquals(((BigDecimal) metaFieldValue.getValue()).setScale(2, RoundingMode.CEILING),
                    value, "Invalid meta-field value!");
        } else
        assertEquals(metaFieldValue.getValue(), value, "Invalid meta-field value!");
    }

    private Integer createFlatProduct(String productCode, Integer categoryId, String price, boolean global,
                                      TestEnvironmentBuilder testEnvironmentBuilder, JbillingAPI api, MetaFieldWS... orderLineMetaFields){
        return testEnvironmentBuilder.itemBuilder(api)
                .item()
                .withCode(productCode)
                .withFlatPrice(price)
                .withType(categoryId)
                .withOrderLineMetaFields(Arrays.asList(orderLineMetaFields))
                .global(global)
                .build();
    }


    private Integer buildAndPersistDefaultMonthlyCustomer(TestEnvironmentBuilder environmentBuilder, String username, Date nextInvoiceDate, JbillingAPI api){

        UserWS userWS = environmentBuilder
                .customerBuilder(api)
                .withUsername(username)
                .addTimeToUsername(false)
                .build();

        if (null != nextInvoiceDate){
            DateTime nid = new DateTime(nextInvoiceDate);
            userWS.setMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api),
                    nid.getDayOfMonth()));
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
        }

        return userWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder environmentBuilder, String code, boolean global, JbillingAPI api){
        return environmentBuilder
                .itemBuilder(api)
                .itemType()
                .withCode(code)
                .global(global)
                .build();
    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder environmentBuilder,
                                       String code, String description,
                                       Integer periodId, Integer itemId,
                                       JbillingAPI api, List<Integer> usagePools,
                                       PlanItemWS... planItems) {
        return environmentBuilder
                .planBuilder(api, code)
                .withDescription(description)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    private Integer buildAndPersistOrderWithOrderLines(TestEnvironmentBuilder environmentBuilder,
                                                      String code, Integer userId, Date activeSince,
                                                      Date activeUntil, Integer orderPeriodId, JbillingAPI api,
                                                      OrderLineWS... orderLines) {
        return environmentBuilder
                .orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withPeriod(orderPeriodId)
                .withOrderLines(Arrays.asList(orderLines))
                .build();
    }


}
