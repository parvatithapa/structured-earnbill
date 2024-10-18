package com.sapienter.jbilling.server.discounts;

import com.gurock.testrail.TestRailClass;
import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Created by Mahesh Shivarkar 
 * on 11/2/18.
 */
@Test(groups = {"billing-and-discounts", "discounts"}, testName = "UpdateDiscountOrderTest")
@TestRailClass(description="UpdateDiscountOrderTest")
public class UpdateDiscountOrderTest {

    private static final Logger logger = LoggerFactory.getLogger(UpdateDiscountOrderTest.class);
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

    @Test(priority = 0)
    public void test01UpdateDiscountOrder(){
        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] orderIds = new Integer[1];


        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();


            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();
            logger.debug("### userId: {}", userWS.getId());
            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());


        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String code = "TD" + System.currentTimeMillis();
            Integer discountId = envBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                    .withDescription("t" + System.currentTimeMillis())
                    .build();

            DiscountLineWS discountLineWS = envBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .build();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[0])// default quantity 1
                    .build();

            orderIds[0] = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortests")
                    .withDiscountLine(discountLineWS)
                    .build();

            assertNotNull(orderIds[0]);
            OrderWS orderWS = api.getOrder(orderIds[0]);
            assertNotNull(orderWS);
            assertEquals(orderWS.getChildOrders().length, 1, "Invalid number of child orders");
            OrderWS[] orderWSes = orderWS.getChildOrders();
            OrderWS childOrderWS = orderWSes[0];
            assertNotNull(childOrderWS);
            assertEquals(childOrderWS.getOrderLines().length, 1, "Invalid number order lines");
            assertEquals(childOrderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("-10").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Total");
            // Update child discount order set active until date
            childOrderWS.setActiveUntil(new Date());
            api.updateOrder(childOrderWS,null);

            // Update parent order set active until date
            orderWS.setActiveUntil(new Date());
            api.updateOrder(orderWS,null);
            
        });
    }

    @Test(priority = 1)
    public void test02UpdatePercentageDiscountOrder(){
        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] orderIds = new Integer[1];


        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();


            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();
            logger.debug("### userId: {}", userWS.getId());
            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());


        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String code = "TD" + System.currentTimeMillis();
            Integer discountId = envBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name())
                    .withDescription("Discount-" + System.currentTimeMillis() + " % 10")
                    .build();

            DiscountLineWS discountLineWS = envBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .build();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[0])// default quantity 1
                    .build();

            orderIds[0] = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(Constants.ORDER_PERIOD_ONCE)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortests")
                    .withDiscountLine(discountLineWS)
                    .build();

            assertNotNull(orderIds[0]);
            OrderWS orderWS = api.getOrder(orderIds[0]);
            assertNotNull(orderWS);
            assertEquals(orderWS.getChildOrders().length, 1, "Invalid number of child orders");
            OrderWS[] orderWSes = orderWS.getChildOrders();
            OrderWS childOrderWS = orderWSes[0];
            assertNotNull(childOrderWS);
            assertEquals(childOrderWS.getOrderLines().length, 1, "Invalid number order lines");
            assertEquals(childOrderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("-1.20").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Total");
            // Update child discount order set active until date
            childOrderWS.setActiveUntil(new Date());
            api.updateOrder(childOrderWS,null);

            // Update parent order set active until date
            orderWS.setActiveUntil(new Date());
            api.updateOrder(orderWS,null);
            
        });
    }

    @Test(priority = 2)
    public void test03UpdatePeriodDiscountOrder(){
        final Integer[] userIds = new Integer[1];
        final Integer[] productIds = new Integer[1];
        final Integer[] orderIds = new Integer[1];

        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername("TestCustomer")
                    .build();
            userIds[0] = userWS.getId();
            logger.debug("### userId: {}", userWS.getId());
            String categoryCode = "Test Category" + System.currentTimeMillis();
            Integer categoryId = createCategory(categoryCode, true, Integer.valueOf(0), envBuilder, api);

            String productCode = "TestProduct" + System.currentTimeMillis();
            productIds[0] = createProduct(productCode, categoryId, "12", true, Integer.valueOf(0),
                    envBuilder, api, api.getCallerCompanyId());

        }).test((env, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            String code = "TD" + System.currentTimeMillis();
            DiscountWS discountWs = createPeriodBasedAmountDiscount(new Date(),"18");
            Integer discountId = api.createOrUpdateDiscount(discountWs);

            DiscountLineWS discountLineWS = envBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .build();

            OrderBuilder orderBuilder = envBuilder.orderBuilder(api);

            OrderLineWS parentOrderLine = orderBuilder.orderLine()
                    .withItemId(productIds[0])// default quantity 1
                    .build();

            orderIds[0] = orderBuilder
                    .forUser(userIds[0])
                    .withPeriod(Constants.ORDER_BILLING_POST_PAID)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests("codefortests")
                    .withDiscountLine(discountLineWS)
                    .build();

            assertNotNull(orderIds[0]);
            OrderWS orderWS = api.getOrder(orderIds[0]);
            assertNotNull(orderWS);
            assertEquals(orderWS.getChildOrders().length, 1, "Invalid number of child orders");
            OrderWS[] orderWSes = orderWS.getChildOrders();
            OrderWS childOrderWS = orderWSes[0];
            assertNotNull(childOrderWS);
            assertEquals(childOrderWS.getOrderLines().length, 1, "Invalid number order lines");
            assertEquals(childOrderWS.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("-10").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Total");
            // Update child discount order set active until date
            childOrderWS.setActiveUntil(new Date());
            api.updateOrder(childOrderWS,null);

            // Update parent order set active until date
            orderWS.setActiveUntil(new Date());
            api.updateOrder(orderWS,null);

        });
    }

    private Integer createProduct(String productCode,Integer category,String price,Boolean global,
                                  Integer assetmanagment,TestEnvironmentBuilder testEnvironmentBuilder,
                                  JbillingAPI api, Integer... entityId ){

        return testEnvironmentBuilder.itemBuilder(api)
                .item()
                .withCode(productCode)
                .global(global)
                .withType(category)
                .withEntities(entityId)
                .withAssetManagementEnabled(assetmanagment)
                .withFlatPrice(price)
                .build();
    }

    private Integer createCategory(String categoryCode, boolean global, Integer allowAssetManagement,
                                   TestEnvironmentBuilder testEnvironmentBuilder, JbillingAPI api){


        return testEnvironmentBuilder.itemBuilder(api)
                .itemType()
                .global(global)
                .withCode(categoryCode)
                .allowAssetManagement(allowAssetManagement)
                .build();
    }

    private DiscountWS createPeriodBasedAmountDiscount(Date discountStartDate, String periodValue) {
        Calendar startOfThisMonth = Calendar.getInstance();
        startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);

        Calendar afterOneMonth = Calendar.getInstance();
        afterOneMonth.setTime(startOfThisMonth.getTime());
        afterOneMonth.add(Calendar.MONTH, 1);

        DiscountWS discountWs = new DiscountWS();
        discountWs.setCode("DS-"+ System.currentTimeMillis());
        discountWs.setDescription("Discount Period 1 Month off $1 "+System.currentTimeMillis());
        discountWs.setRate("10");
        discountWs.setType(DiscountStrategyType.RECURRING_PERIODBASED.name());

        SortedMap<String, String> attributes = new TreeMap<>();
        attributes.put("periodUnit", "2");    // period unit month
        attributes.put("periodValue", periodValue);
        attributes.put("isPercentage", "0");    // Consider rate as amount
        discountWs.setAttributes(attributes);

        if (discountStartDate != null) {
            discountWs.setStartDate(discountStartDate);
        }

        return discountWs;
    }
}
