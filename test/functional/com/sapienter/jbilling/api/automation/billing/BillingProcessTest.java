package com.sapienter.jbilling.api.automation.billing;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

/**
 * JBQA-40
 * Tests covering the billing process.
 *
 * @author Bojan Dikovski
 * @since 28-JUN-2016
 */
@Test(groups = {"api-automation"}, testName = "BillingProcessTest")
public class BillingProcessTest {

    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;

    private String proratingAuto = ProratingType.PRORATING_AUTO_ON.getProratingType();

    private Integer applyStatusId;
    private Integer dailyPeriodId;
    private Integer weeklyPeriodId;
    private Integer semiMonthlyPeriodId;
    private Integer monthlyPeriodId;
    private Integer threeMonthlyPeriodId;
    private Integer accTypeId;

    private Integer monthlyUnitId = Constants.PERIOD_UNIT_MONTH;
    private Integer semiMonthlyUnitId = Constants.PERIOD_UNIT_SEMI_MONTHLY;

    private String testCat = "testCategory";
    private String prod01 = "testLemonade";
    private String prod02 = "testCallRate";
    private String prod03 = "testPhoneCase";
    private String prod04 = "testPhone";
    private String prod05 = "testPlanSubscriptionItem";
    private String prod06 = "testIPhone";
    private String plan01 = "testPlanOne";

    private String user01 = "JohnDoe";
    private String user02 = "LucyWilliams";
    private String user03 = "LeonardoWilliams";
    private String user04 = "JenifferHewitt";
    private String user05 = "LeslyJoseph";
    private String user06 = "LilyCatherine";
    private String user07 = "WeeklyPre";
    private String user08 = "SemimonthlyPost";
    private String user09 = "MonthlyUser";
    private String user10 = "ParentCustomer";
    private String user11 = "ChildCustomer";
    private String user12 = "LastTestCustomer";
    private String user13 = "RobinThomas";
    private String user14 = "RonnyApply";
    private String user15 = "JammyApply";
    private String user16 = "JacksonApply";
    private String user17 = "RahulJain";
    private String user18 = "SemimonthlyPre";
    private String user19 = "ThreeMonthlyPost";

    private String order01 = "testOrderJohnDoe";
    private OrderLineWS orderLine01;
    private String order02 = "testOrderLucyWilliams";
    private String order03 = "testOrderLeonardoWilliams";
    private String order04 = "testOrderJenifferHewitt";
    private String order05 = "testOrderLeslyJoseph";
    private String order06 = "testOrderLilyCatherine";
    private String order07 = "testOrderWeeklyPre";
    private String order08 = "testOrderSemimonthlyPost";
    private String order09 = "testOrderMonthlyUser";
    private String order10 = "testOrderParentCustomer";
    private String order11 = "testOrderChildCustomer";
    private String order12 = "testOrderLastTestCustomer";
    private String order13 = "testOrderRobinThomas";
    private String order14 = "testOrderRonnyApply";
    private String order15 = "testOrderJammyApply";
    private String order16 = "testOrderJacksonApply";
    private String order17 = "testRahulJain";
    private String order18 = "testOrderSemimonthlyPre";
    private String order19 = "testOrderThreeMonthlyPost";

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
    }

    @AfterClass
    public void tearDown() {

        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != envHelper) {
            envHelper = null;
        }
        if (null != testBuilder) {
            testBuilder = null;
        }
    }

    private TestBuilder getTestEnvironment() {

        return TestBuilder.newTest().givenForMultiple(testEnvCreator -> {

            envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }

    @Test
    public void testBillingProcessWithOrderChanges() {

        testBuilder.given(envBuilder -> {

            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            Map<Integer, BigDecimal> prodQtyMap = null;

            buildAndPersistCategory(envBuilder, api, testCat, false);
            buildAndPersistFlatProduct(envBuilder, api, prod01, false, envBuilder.env().idForCode(testCat), "50");
            buildAndPersistGraduatedProduct(envBuilder, api, prod02, false, envBuilder.env().idForCode(testCat), "5", "3");
            buildAndPersistFlatProduct(envBuilder, api, prod03, false, envBuilder.env().idForCode(testCat), "10");
            buildAndPersistFlatProduct(envBuilder, api, prod04, false, envBuilder.env().idForCode(testCat), "20");
            buildAndPersistFlatProduct(envBuilder, api, prod05, false, envBuilder.env().idForCode(testCat), "40");
            buildAndPersistFlatProduct(envBuilder, api, prod06, false, envBuilder.env().idForCode(testCat), "500");

            // Step 0. - Create account type and order change status.
            accTypeId = buildAndPersistAccountType(envBuilder, api, "BillingAccount");
            applyStatusId = buildAndPersistOrderChangeStatus(envBuilder, api, "ApplyStatus", ApplyToOrder.YES);

            // Step 1. - Create different order periods.
            dailyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testDailyPeriod", 1, PeriodUnitDTO.DAY);
            weeklyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testWeeklyPeriod", 1, PeriodUnitDTO.WEEK);
            semiMonthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testSemiMonthlyPeriod", 1, PeriodUnitDTO.SEMI_MONTHLY);
            monthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testMonthlyPeriod", 1, PeriodUnitDTO.MONTH);
            threeMonthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testThreeMonthlyPeriodId", 3, PeriodUnitDTO.MONTH);

            // Step 2. - Create customer with the current date as NID, monthly billing period.
            buildAndPersistCustomer(envBuilder, api, user01, accTypeId, new LocalDate().now().toDate(),
                    monthlyPeriodId, new LocalDate().now().getDayOfMonth());
            // Step 3. - Create an prepaid order for that customer with one product,
            // active since set to the current date and effective date set to a future date.
            OrderWS orderWS = buildOrder(envBuilder, api, envBuilder.env().idForCode(user01), new LocalDate().now().toDate(),
                    null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID, false, envBuilder.env().idForCode(prod01));
            persistOrder(envBuilder, api, order01, orderWS, new OrderChangeWS[]{OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[0], applyStatusId, new LocalDate().now().plusMonths(1).toDate())});
            orderLine01 = orderWS.getOrderLines()[0];

            // Step 7. - Create customer with NID as 1.JUN.2015, monthly billing period.
            buildAndPersistCustomer(envBuilder, api, user02, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            // Step 8. - Create prepaid order for the customer from the previous step, one product, quantity 4,
            // active since date set to 5.JUN.2015, prorating enabled.
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("4"));
            buildAndPersistOrder(envBuilder, api, order02, envBuilder.env().idForCode(user02),
                    new LocalDate(2015, 6, 5).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 14. - Create customer with NID as 1.JUN.2015, monthly billing period.
            // Then create a monthly postpaid order with active since date set to 5.MAY.2015,
            // one product, quantity 4, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user03, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("4"));
            buildAndPersistOrder(envBuilder, api, order03, envBuilder.env().idForCode(user03),
                    new LocalDate(2015, 5, 5).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, prodQtyMap);

            // Step 28. - Create customer with NID as 1.JUN.2015, monthly billing period.
            // Then create a monthly prepaid order with active since date set to 15.JUN.2015,
            // one product, quantity 6, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user04, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("6"));
            buildAndPersistOrder(envBuilder, api, order04, envBuilder.env().idForCode(user04),
                    new LocalDate(2015, 6, 15).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 37. - Create customer with NID as 1.JUN.2015, monthly billing period.
            // Then create a monthly prepaid order with active since date set to 1.JUN.2015,
            // one product, quantity 3, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user05, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("3"));
            buildAndPersistOrder(envBuilder, api, order05, envBuilder.env().idForCode(user05),
                    new LocalDate(2015, 6, 1).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 38. - Create customer with NID as 1.JUL.2015, monthly billing period.
            // Then create a monthly postpaid order with active since date set to 1.JUN.2015,
            // one product, quantity 3, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user06, accTypeId, new LocalDate(2015, 7, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("3"));
            buildAndPersistOrder(envBuilder, api, order06, envBuilder.env().idForCode(user06),
                    new LocalDate(2015, 6, 1).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, prodQtyMap);

            // Step 44. - Create customer with NID as 1.JAN.2016, weekly billing period.
            // Then create a weekly prepaid order with active since date set to 1.JAN.2016,
            // one product, quantity 1, prorating enabled for the weekly customer.
            // Create customer with NID as 1.JAN.2016, semi-monthly billing period.
            // Then create a semi-monthly postpaid order with active since date set to 1.DEC.2015,
            // one product, quantity 3, prorating enabled for the semi-monthly customer.
            buildAndPersistCustomer(envBuilder, api, user07, accTypeId, new LocalDate(2016, 1, 1).toDate(),
                    weeklyPeriodId, 6);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order07, envBuilder.env().idForCode(user07),
                    new LocalDate(2016, 1, 1).toDate(), null, weeklyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);
            buildAndPersistCustomer(envBuilder, api, user08, accTypeId, new LocalDate(2016, 1, 1).toDate(),
                    semiMonthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("3"));
            buildAndPersistOrder(envBuilder, api, order08, envBuilder.env().idForCode(user08),
                    new LocalDate(2015, 12, 1).toDate(), null, semiMonthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, prodQtyMap);

            // Step 45. - Build a plan.
            // Create customer with NID as 1.FEB.2016, monthly billing period.
            // Then create a monthly prepaid order with active since date set to 1.FEB.2016,
            // containing the previously created plan, quantity 1, prorating enabled for the monthly customer.
            buildAndPersistPlan(envBuilder, api, plan01, plan01 + "_description", monthlyPeriodId,
                    envBuilder.env().idForCode(prod05), new ArrayList<>(),
                    buildPlanItem(api, envBuilder.env().idForCode(prod03), semiMonthlyPeriodId, "2", "10"),
                    buildPlanItem(api, envBuilder.env().idForCode(prod04), monthlyPeriodId, "1", "20"));
            buildAndPersistCustomer(envBuilder, api, user09, accTypeId, new LocalDate(2016, 2, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod05), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order09, envBuilder.env().idForCode(user09),
                    new LocalDate(2016, 2, 1).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 46. - Create parent customer with NID as 1.APR.2016, semi-monthly billing period.
            // Create child customer with NID as 1.APR.2016, semi-monthly billing period.
            // Create a semi-monthly prepaid order with active since date set to 1.APR.2016,
            // one product, quantity 5, prorating enabled for the parent customer.
            // Create a semi-monthly prepaid order with active since date set to 9.APR.2016,
            // one product, quantity 3, prorating enabled for the child customer.
            buildAndPersistCustomer(envBuilder, api, user10, accTypeId, new LocalDate(2016, 4, 1).toDate(),
                    semiMonthlyPeriodId, 1);
            UserWS userWS = api.getUserWS(envBuilder.env().idForCode(user10));
            userWS.setIsParent(true);
            api.updateUser(userWS);
            buildAndPersistCustomer(envBuilder, api, user11, accTypeId, new LocalDate(2016, 4, 1).toDate(),
                    semiMonthlyPeriodId, 1);
            userWS = api.getUserWS(envBuilder.env().idForCode(user11));
            userWS.setParentId(envBuilder.env().idForCode(user10));
            api.updateUser(userWS);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("5"));
            buildAndPersistOrder(envBuilder, api, order10, envBuilder.env().idForCode(user10),
                    new LocalDate(2016, 4, 1).toDate(), null, semiMonthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("3"));
            buildAndPersistOrder(envBuilder, api, order11, envBuilder.env().idForCode(user11),
                    new LocalDate(2016, 4, 9).toDate(), null, semiMonthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 48. - Create customer with NID as 1.MAY.2016, semi-monthly billing period.
            // Then create a monthly prepaid order with active since date set to 1.MAY.2016,
            // one product, quantity 1, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user12, accTypeId, new LocalDate(2016, 5, 1).toDate(),
                    semiMonthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order12, envBuilder.env().idForCode(user12),
                    new LocalDate(2016, 5, 1).toDate(), null, semiMonthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 50. - Create customer with NID as 1.JUN.2015, monthly billing period.
            // Then create a monthly prepaid order with active since date set to 15.JUN.2015
            // one product, quantity 1, prorating enabled.
            // Update the created order with active until date set to 21.JUN.2015
            buildAndPersistCustomer(envBuilder, api, user13, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order13, envBuilder.env().idForCode(user13),
                    new LocalDate(2015, 6, 15).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);
            orderWS = api.getOrder(envBuilder.env().idForCode(order13));
            orderWS.setActiveUntil(new LocalDate(2015, 6, 21).toDate());
            api.updateOrder(orderWS, null);

            // Step 52. - Create customer with NID as 1.JUN.2015, monthly billing period.
            // Create another customer with NID as 1.JUN.2015, monthly billing period.
            // Create monthly prepaid order with prorating for user14 with active since = 1.JUN.2015
            // and the following order lines
            // - prod01, effective date = 15.JUN.2015, quantity = 3, apply now checked
            // (setting apply date to the order active since date as well)
            // - prod02, effective date = 1.JUN.2015, quantity = 5, apply now not checked
            // Create monthly prepaid order with prorating for user15 with active since = 1.JUN.2015
            // and the following order lines
            // - prod01, effective date = 15.JUN.2015, quantity = 3, apply now checked
            // (setting apply date to the order active since date as well)
            // - prod02, effective date = 1.JUN.2015, quantity = 5, apply now not checked
            buildAndPersistCustomer(envBuilder, api, user14, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            buildAndPersistCustomer(envBuilder, api, user15, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            orderWS = buildOrder(envBuilder, api, envBuilder.env().idForCode(user14), new LocalDate(2015, 6, 1).toDate(),
                    null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID, true,
                    envBuilder.env().idForCode(prod01), envBuilder.env().idForCode(prod02));
            OrderChangeWS orderChangeOne = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[0], applyStatusId, new LocalDate(2015, 6, 15).toDate());
            orderChangeOne.setQuantity(new BigDecimal("3"));
            orderChangeOne.setAppliedManually(1);
            orderChangeOne.setApplicationDate(new LocalDate(2015, 6, 1).toDate());
            OrderChangeWS orderChangeTwo = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[1], applyStatusId, new LocalDate(2015, 6, 1).toDate());
            orderChangeTwo.setQuantity(new BigDecimal("5"));
            orderChangeTwo.setAppliedManually(0);
            persistOrder(envBuilder, api, order14, orderWS, new OrderChangeWS[]{orderChangeOne, orderChangeTwo});
            orderWS = buildOrder(envBuilder, api, envBuilder.env().idForCode(user15), new LocalDate(2015, 6, 1).toDate(),
                    null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID, true,
                    envBuilder.env().idForCode(prod01), envBuilder.env().idForCode(prod02));
            orderChangeOne = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[0], applyStatusId, new LocalDate(2015, 6, 15).toDate());
            orderChangeOne.setQuantity(new BigDecimal("3"));
            orderChangeOne.setAppliedManually(1);
            orderChangeOne.setApplicationDate(new LocalDate(2015, 6, 1).toDate());
            orderChangeTwo = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[1], applyStatusId, new LocalDate(2015, 6, 1).toDate());
            orderChangeTwo.setQuantity(new BigDecimal("5"));
            orderChangeTwo.setAppliedManually(0);
            persistOrder(envBuilder, api, order15, orderWS, new OrderChangeWS[]{orderChangeOne, orderChangeTwo});

            // Step 53. - Create customer with NID as 1.JUN.2015, monthly billing period.
            // Create monthly prepaid order with prorating for user16 with active since = 1.JUN.2015
            // and the following order lines
            // - prod01, effective date = 5.JUL.2015, quantity = 3, apply now checked
            // (setting apply date to the order active since date as well)
            // - prod02, effective date = 15.JUN.2015, quantity = 5, apply now checked
            // (setting apply date to the order active since date as well)
            // - prod06, effective date = 5.JUL.2015, quantity = 1, apply now not checked
            buildAndPersistCustomer(envBuilder, api, user16, accTypeId, new LocalDate(2015, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            orderWS = buildOrder(envBuilder, api, envBuilder.env().idForCode(user16), new LocalDate(2015, 6, 1).toDate(),
                    null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID, true,
                    envBuilder.env().idForCode(prod01), envBuilder.env().idForCode(prod02), envBuilder.env().idForCode(prod06));
            orderChangeOne = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[0], applyStatusId, new LocalDate(2015, 7, 5).toDate());
            orderChangeOne.setQuantity(new BigDecimal("3"));
            orderChangeOne.setAppliedManually(1);
            orderChangeOne.setApplicationDate(new LocalDate(2015, 6, 1).toDate());
            orderChangeTwo = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[1], applyStatusId, new LocalDate(2015, 6, 15).toDate());
            orderChangeTwo.setQuantity(new BigDecimal("5"));
            orderChangeTwo.setAppliedManually(1);
            orderChangeOne.setApplicationDate(new LocalDate(2015, 6, 1).toDate());
            OrderChangeWS orderChangeThree = OrderBuilder.buildChangeFromLine(
                    orderWS, orderWS.getOrderLines()[2], applyStatusId, new LocalDate(2015, 7, 5).toDate());
            orderChangeThree.setQuantity(new BigDecimal("1"));
            orderChangeThree.setAppliedManually(0);
            persistOrder(envBuilder, api, order16, orderWS, new OrderChangeWS[]{orderChangeOne, orderChangeTwo, orderChangeThree});

            // Step 58. - Create customer with NID as 1.JUN.2016, monthly billing period.
            // Then create a monthly prepaid order with active since date set to 1.JUN.2016
            // one product, quantity 1, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user17, accTypeId, new LocalDate(2016, 6, 1).toDate(),
                    monthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order17, envBuilder.env().idForCode(user17),
                    new LocalDate(2016, 6, 1).toDate(), null, monthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Step 60. - Create customer with NID as 15.JUN.2016, monthly billing period.
            // Then create a semiMonthly prepaid order with active since date set to 15.JUN.2016
            // one product, quantity 1, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user18, accTypeId, new LocalDate(2016, 6, 15).toDate(), semiMonthlyPeriodId, 15);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order18, envBuilder.env().idForCode(user18),
                    new LocalDate(2016, 6, 15).toDate(), null, semiMonthlyPeriodId, Constants.ORDER_BILLING_PRE_PAID,
                    true, prodQtyMap);

            // Create customer with NID as 1st.JUNE.2016, monthly billing period.
            // Then create a 3 Monthly postpaid order with active since date set to 1st.March.2016
            // one product, quantity 1, prorating enabled.
            buildAndPersistCustomer(envBuilder, api, user19, accTypeId, new LocalDate(2016, 6, 1).toDate(), threeMonthlyPeriodId, 1);
            prodQtyMap = new HashMap<>();
            prodQtyMap.put(envBuilder.env().idForCode(prod01), new BigDecimal("1"));
            buildAndPersistOrder(envBuilder, api, order19, envBuilder.env().idForCode(user19),
                    new LocalDate(2016, 3, 1).toDate(), null, threeMonthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, prodQtyMap);
        }).test((testEnv, testEnvBuilder) -> {

            final JbillingAPI api = testEnv.getPrancingPonyApi();

            OrderWS orderWS = null;
            OrderLineWS orderLineWS = null;
            OrderChangeWS orderChangeWS = null;
            Integer[] invoiceIds = null;

            try {

                // Step 1.1. - Verify that an order period can be updated successfully.
                OrderPeriodWS orderPeriodWS = findOrderPeriodById(weeklyPeriodId, api);
                assertNotNull(orderPeriodWS);
                orderPeriodWS.setDescriptions(Arrays.asList(
                        new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "Weekly New")));
                orderPeriodWS.setValue(2);
                api.updateOrCreateOrderPeriod(orderPeriodWS);
                orderPeriodWS = findOrderPeriodById(weeklyPeriodId, api);
                assertNotNull(orderPeriodWS);
                assertEquals(orderPeriodWS.getValue(), Integer.valueOf(2));
                assertEquals(orderPeriodWS.getDescription(Constants.LANGUAGE_ENGLISH_ID).getContent(), "Weekly New");
                orderPeriodWS.setDescriptions(Arrays.asList(
                        new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "testWeeklyPeriod")));
                orderPeriodWS.setValue(1);
                api.updateOrCreateOrderPeriod(orderPeriodWS);

                // Step 4. - Try to generate invoice from the updated order in Step 3.
                invoiceIds = api.createInvoice(testEnv.idForCode(user01), false);
                assertEquals(invoiceIds.length, 0);

                // Step 5. - Run review billing run for current date. Verify that no invoice is generated.
                triggerBilling(api, new LocalDate().now().toDate(), true, proratingAuto, 1, monthlyUnitId);
                assertEquals(api.getLastInvoices(testEnv.idForCode(user01), 1).length, 0);
                api.setReviewApproval(false);

                // Step 6. - Update the order created in Step 3 with an effective date set to a past date.
                // Generate an invoice from the order, and verify that the generated invoice has correct amount.
                orderWS = api.getOrder(testEnv.idForCode(order01));
                api.updateOrder(orderWS, new OrderChangeWS[]{OrderBuilder.buildChangeFromLine(
                        orderWS, orderLine01, applyStatusId, new LocalDate().now().toDate())});
                invoiceIds = api.createInvoice(testEnv.idForCode(user01), false);
                assertEquals(invoiceIds.length, 1);
                compareBigDecimalValue(api.getInvoiceWS(invoiceIds[0]).getTotalAsDecimal(), new BigDecimal("50"));
                api.deleteInvoice(invoiceIds[0]);

                // Run real billing run for date 1.JUN.2015.
                triggerBilling(api, new LocalDate(2015, 6, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 8. - Verify the generated invoice line for 1.JUN.2015.
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 6, 5), new LocalDate(2015, 6, 30),
                        new BigDecimal("4"), new BigDecimal("173.33"), new BigDecimal("50"), 1);

                // Step 14. - Verify the correct values for the invoice lines in the invoice generated for 1.JUN.2015.
                // Create order change with quantity = -2 for order03 as of 16.JUN.2015.
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 5, 5), new LocalDate(2015, 5, 31),
                        new BigDecimal("4"), new BigDecimal("174.19"), new BigDecimal("50"), 1);
                changeOrderPriceAndQuantity(testEnv, api, order03, prod01,
                        null, new BigDecimal("-2"), new LocalDate(2015, 6, 16).toDate());

                // Step 28. - Verify the correct values for the invoice lines in the invoice generated for 1.JUN.2015.
                // Create order change with price = 30 and quantity = 1 for order04 as of 15.JUL.2015
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 6, 15), new LocalDate(2015, 6, 30),
                        new BigDecimal("6"), new BigDecimal("160"), new BigDecimal("50"), 1);
                changeOrderPriceAndQuantity(testEnv, api, order04, prod01,
                        new BigDecimal("30"), new BigDecimal("1"), new LocalDate(2015, 7, 15).toDate());

                // Step 37. - Verify the correct values for the invoice lines in the invoice generated for 1.JUN.2015.
                // Create order change with quantity = 1 for order05 as of 5.JUL.2015.
                // Create order change with quantity = 1 for order05 as of 15.JUL.2015.
                // Create order change with quantity = 1 for order05 as of 25.JUL.2015.
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 6, 1), new LocalDate(2015, 6, 30),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                changeOrderPriceAndQuantity(testEnv, api, order05, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 7, 5).toDate());
                changeOrderPriceAndQuantity(testEnv, api, order05, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 7, 15).toDate());
                changeOrderPriceAndQuantity(testEnv, api, order05, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 7, 25).toDate());

                // Step 38. - Create order change with quantity = 1 for order06 as of 5.JUL.2015.
                // Create order change with quantity = 1 for orderSixas of 15.JUL.2015.
                // Create order change with quantity = 1 for order06 as of 25.JUL.2015.
                // Create order change with price = 40 for order06 as of 25.AUG.2015.
                changeOrderPriceAndQuantity(testEnv, api, order06, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 7, 5).toDate());
                changeOrderPriceAndQuantity(testEnv, api, order06, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 7, 15).toDate());
                changeOrderPriceAndQuantity(testEnv, api, order06, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 7, 25).toDate());
                changeOrderPriceAndQuantity(testEnv, api, order06, prod01,
                        new BigDecimal("40"), null, new LocalDate(2015, 8, 25).toDate());

                // Step 50. - Verify the correct values for the invoice lines in the invoice generated for 1.JUN.2015.
                assertInvoiceLine(testEnv, api, user13, prod01, new LocalDate(2015, 6, 15), new LocalDate(2015, 6, 21),
                        new BigDecimal("1"), new BigDecimal("11.6667"), new BigDecimal("50"), 1);

                // Step 52. - Verify the correct values for the invoice lines in the invoice generated for 1.JUN.2015.
                assertInvoiceLine(testEnv, api, user14, prod01, new LocalDate(2015, 6, 15), new LocalDate(2015, 6, 30),
                        new BigDecimal("3"), new BigDecimal("80"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user14, prod02, new LocalDate(2015, 6, 1), new LocalDate(2015, 6, 30),
                        new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("2"), 1);
                assertInvoiceLine(testEnv, api, user15, prod01, new LocalDate(2015, 6, 15), new LocalDate(2015, 6, 30),
                        new BigDecimal("3"), new BigDecimal("80"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user15, prod02, new LocalDate(2015, 6, 1), new LocalDate(2015, 6, 30),
                        new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("2"), 1);

                // Step 53. - Verify the correct values for the invoice lines in the invoice generated for 1.JUN.2015.
                assertInvoiceLine(testEnv, api, user16, prod02, new LocalDate(2015, 6, 15), new LocalDate(2015, 6, 30),
                        new BigDecimal("5"), new BigDecimal("5.33"), new BigDecimal("2"), 1);

                // Step 55. - Verify that additional invoice lines for products with effective date in the future of the
                // billing run date (even if 'apply now' is checked) are not existing
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user16)).getInvoiceLines().length, 1);

                // Run real billing run for date 1.JUL.2015.
                triggerBilling(api, new LocalDate(2015, 7, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 8. - Verify the generated invoice line for 1.JUL.2015.
                // Create order change to add quantity = 2 to order02 as of 15.AUG.2010.
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                changeOrderPriceAndQuantity(testEnv, api, order02, prod01,
                        null, new BigDecimal("2"), new LocalDate(2015, 8, 15).toDate());

                // Step 16. - Verify the correct values for the invoice lines in the invoice generated for 1.JUL.2015.
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 6, 1), new LocalDate(2015, 6, 30),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 6, 16), new LocalDate(2015, 6, 30),
                        new BigDecimal("-2"), new BigDecimal("-49.99"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 7, 1), new BigDecimal("174.19"));

                // Step 17. - Create order change with quantity = -1 for order03 as of 1.JUL.2015
                changeOrderPriceAndQuantity(testEnv, api, order03, prod01,
                        null, new BigDecimal("-1"), new LocalDate(2015, 7, 1).toDate());

                // Step 30. - Verify the correct values for the invoice lines in the invoice generated for 1.JUL.2015.
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("6"), new BigDecimal("300"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 7, 15), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("27.41"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 7, 15), new LocalDate(2015, 7, 31),
                        new BigDecimal("7"), new BigDecimal("-76.77"), new BigDecimal("-20"), 1);
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 7, 1), new BigDecimal("160"));

                // Step 39. - Verify the correct values for the invoice lines in the invoice generated for 1.JUL.2015.
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 7, 5), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("43.54"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 7, 15), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("27.41"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 7, 25), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("11.29"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user05, new LocalDate(2015, 7, 1), new BigDecimal("150"));
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 6, 1), new LocalDate(2015, 6, 30),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);

                // Run real billing run for date 1.AUG.2015.
                triggerBilling(api, new LocalDate(2015, 8, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 10. - Verify the correct values for the invoice lines in the invoice generated for 1.AUG.2015.
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 8, 15), new LocalDate(2015, 8, 31),
                        new BigDecimal("2"), new BigDecimal("54.83"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user02, new LocalDate(2015, 7, 1), new BigDecimal("173.33"));
                assertCarriedInvoiceLine(testEnv, api, user02, new LocalDate(2015, 8, 1), new BigDecimal("200"));

                // Step 11. - Update the order02 active since date to 1.JUL.2015. Verify the error message.
                orderWS = api.getOrder(testEnv.idForCode(order02));
                orderWS.setActiveSince(new LocalDate(2015, 7, 1).toDate());
                try {
                    api.updateOrder(orderWS, null);
                } catch (SessionInternalError e) {
                    assertTrue(e.getMessage().matches(".*OrderWS,activeSince,order.acitve.since.date.not.allowed.to.changes\\s*"));
                }

                // Step 12. - Create order change to add quantity = 1 to order02 as of 1.SEP.2015.
                changeOrderPriceAndQuantity(testEnv, api, order02, prod01,
                        null, new BigDecimal("1"), new LocalDate(2015, 9, 1).toDate());

                // Step 18. - Verify the correct values for the invoice lines in the invoice generated for 1.AUG.2015.
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("-2"), new BigDecimal("-100"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("-1"), new BigDecimal("-50"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 7, 1), new BigDecimal("174.19"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 8, 1), new BigDecimal("150"));

                // Step 19. - The due date for the invoice from step 18. should be 1.SEP.2015
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user03)).getDueDate(),
                        new LocalDate(2015, 9, 1).toDate());

                // Step 20. - Create order change with price = 40 for order03 as of 24.SEP.2015
                changeOrderPriceAndQuantity(testEnv, api, order03, prod01,
                        new BigDecimal("40"), null, new LocalDate(2015, 9, 24).toDate());

                // Step 31. - Verify the correct values for the invoice lines in the invoice generated for 1.AUG.2015.
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("6"), new BigDecimal("300"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("7"), new BigDecimal("-140"), new BigDecimal("-20"), 1);
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 7, 1), new BigDecimal("160"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 8, 1), new BigDecimal("250.65"));

                // Step 32. - Create order change with price = 60 and quantity = -1 for order04 as of 1.SEP.2015
                // Add new order line to order04 with productTwo, quantity = 5
                changeOrderPriceAndQuantity(testEnv, api, order04, prod01,
                        new BigDecimal("60"), new BigDecimal("-2"), new LocalDate(2015, 9, 1).toDate());
                addProductToOrder(testEnv, api, order04, prod02, new BigDecimal("5"), new LocalDate(2015, 9, 1).toDate());

                // Step 39.1. - Verify the correct values for the invoice lines in the invoice generated for 1.AUG.2015.
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 3);
                assertCarriedInvoiceLine(testEnv, api, user05, new LocalDate(2015, 7, 1), new BigDecimal("150"));
                assertCarriedInvoiceLine(testEnv, api, user05, new LocalDate(2015, 8, 1), new BigDecimal("232.26"));
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 7, 1), new LocalDate(2015, 7, 31),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 7, 5), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("43.54"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 7, 15), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("27.41"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 7, 25), new LocalDate(2015, 7, 31),
                        new BigDecimal("1"), new BigDecimal("11.29"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user06, new LocalDate(2015, 8, 1), new BigDecimal("150"));

                // Run real billing run for date 1.SEP.2015
                triggerBilling(api, new LocalDate(2015, 9, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 13. - Verify the correct values for the invoice lines in the invoice generated for 1.SEP.2015.
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user02, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user02, new LocalDate(2015, 7, 1), new BigDecimal("173.33"));
                assertCarriedInvoiceLine(testEnv, api, user02, new LocalDate(2015, 8, 1), new BigDecimal("200"));
                assertCarriedInvoiceLine(testEnv, api, user02, new LocalDate(2015, 9, 1), new BigDecimal("254.84"));

                // Step 13.1 - Create a customer with next invoice day 15. The created customer next invoice date should
                // be 15.SEP.2015 (after the last billing run).
                testEnvBuilder.customerBuilder(api)
                        .withUsername("step13Customer")
                        .withAccountTypeId(accTypeId)
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new LocalDate(2016, 1, 15).toDate())
                        .withMainSubscription(new MainSubscriptionWS(monthlyPeriodId, 15))
                        .build();
                // Currently this fails, bug related to JB-296
                //assertEquals(api.getUserWS(testEnv.idForCode("step13Customer")).getNextInvoiceDate(),
                //        new LocalDate(2015, 9, 15).toDate());

                // Step 21. - Verify that an invoice with due date for 1.OCT.2015 is generated.
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user03)).getDueDate(),
                        new LocalDate(2015, 10, 1).toDate());

                // Step 33. - Verify the correct values for the invoice lines in the invoice generated for 1.SEP.2015.
                // Create order change with quantity = -2 for order04 as of 1.OCT.2015 for productTwo.
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("6"), new BigDecimal("300"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("7"), new BigDecimal("-140"), new BigDecimal("-20"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("-2"), new BigDecimal("-60"), new BigDecimal("30"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("5"), new BigDecimal("150"), new BigDecimal("30"), 1);
                assertInvoiceLine(testEnv, api, user04, prod02, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("2"), 1);
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 7, 1), new BigDecimal("160"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 8, 1), new BigDecimal("250.65"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 9, 1), new BigDecimal("210"));
                changeOrderPriceAndQuantity(testEnv, api, order04, prod02,
                        null, new BigDecimal("-2"), new LocalDate(2015, 10, 1).toDate());

                // Step 42. - Verify the correct values for the invoice lines in the invoice generated for 1.SEP.2015.
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user05, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 3);
                assertCarriedInvoiceLine(testEnv, api, user05, new LocalDate(2015, 7, 1), new BigDecimal("150"));
                assertCarriedInvoiceLine(testEnv, api, user05, new LocalDate(2015, 8, 1), new BigDecimal("232.26"));
                assertCarriedInvoiceLine(testEnv, api, user05, new LocalDate(2015, 9, 1), new BigDecimal("300"));
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 8, 1), new LocalDate(2015, 8, 31),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 3);
                assertInvoiceLine(testEnv, api, user06, prod01, new LocalDate(2015, 8, 25), new LocalDate(2015, 8, 31),
                        new BigDecimal("6"), new BigDecimal("-13.54"), new BigDecimal("-10"), 1);
                assertCarriedInvoiceLine(testEnv, api, user06, new LocalDate(2015, 8, 1), new BigDecimal("150"));
                assertCarriedInvoiceLine(testEnv, api, user06, new LocalDate(2015, 9, 1), new BigDecimal("232.26"));

                // Run real billing run for date 1.OCT.2015
                triggerBilling(api, new LocalDate(2015, 10, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 22. - Verify the correct values for the invoice lines in the invoice generated for 1.OCT.2015.
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("-2"), new BigDecimal("-100"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 9, 1), new LocalDate(2015, 9, 30),
                        new BigDecimal("-1"), new BigDecimal("-50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 9, 24), new LocalDate(2015, 9, 30),
                        new BigDecimal("1"), new BigDecimal("-2.33"), new BigDecimal("-10"), 1);
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 7, 1), new BigDecimal("174.19"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 8, 1), new BigDecimal("150"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 9, 1), new BigDecimal("50"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 10, 1), new BigDecimal("50"));

                // Step 23. - Create order change with price = 30 for order03 as of 1.OCT.2015
                changeOrderPriceAndQuantity(testEnv, api, order03, prod01,
                        new BigDecimal("30"), null, new LocalDate(2015, 10, 1).toDate());

                // Step 33. - Verify the correct values for the invoice lines in the invoice generated for 1.OCT.2015.
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("6"), new BigDecimal("300"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("7"), new BigDecimal("-140"), new BigDecimal("-20"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("-2"), new BigDecimal("-60"), new BigDecimal("30"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("5"), new BigDecimal("150"), new BigDecimal("30"), 1);
                assertInvoiceLine(testEnv, api, user04, prod02, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("5"), new BigDecimal("10"), new BigDecimal("2"), 1);
                assertInvoiceLine(testEnv, api, user04, prod02, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("-2"), new BigDecimal("-4"), new BigDecimal("2"), 1);
                assertInvoiceLine(testEnv, api, user04, prod02, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("3"), new BigDecimal("-6"), new BigDecimal("-2"), 1);
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 7, 1), new BigDecimal("160"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 8, 1), new BigDecimal("250.65"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 9, 1), new BigDecimal("210"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 10, 1), new BigDecimal("310"));

                // Step 34. - Remove productTwo from order04 as of 1.NOV.2015
                removeProductFromOrder(testEnv, api, order04, prod02, new LocalDate(2015, 11, 1).toDate());

                // Run real billing run for date 1.NOV.2015
                triggerBilling(api, new LocalDate(2015, 11, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 24. - Verify the correct values for the invoice lines in the invoice generated for 1.NOV.2015.
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("-2"), new BigDecimal("-100"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("-1"), new BigDecimal("-50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 10, 1), new LocalDate(2015, 10, 31),
                        new BigDecimal("1"), new BigDecimal("-10"), new BigDecimal("-10"), 2);
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 7, 1), new BigDecimal("174.19"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 8, 1), new BigDecimal("150"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 9, 1), new BigDecimal("50"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 10, 1), new BigDecimal("50"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 11, 1), new BigDecimal("47.67"));

                // Step 25. - Create order change with price = 60 for order03 as of 1.NOV.2015
                changeOrderPriceAndQuantity(testEnv, api, order03, prod01,
                        new BigDecimal("60"), null, new LocalDate(2015, 11, 1).toDate());

                // Step 35. - Verify the correct values for the invoice lines in the invoice generated for 1.NOV.2015.
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("6"), new BigDecimal("300"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("7"), new BigDecimal("-140"), new BigDecimal("-20"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("-2"), new BigDecimal("-60"), new BigDecimal("30"), 1);
                assertInvoiceLine(testEnv, api, user04, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("5"), new BigDecimal("150"), new BigDecimal("30"), 1);
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 7, 1), new BigDecimal("160"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 8, 1), new BigDecimal("250.65"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 9, 1), new BigDecimal("210"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 10, 1), new BigDecimal("310"));
                assertCarriedInvoiceLine(testEnv, api, user04, new LocalDate(2015, 11, 1), new BigDecimal("300"));
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user04)).getInvoiceLines().length, 10);

                // Step 36. - Remove productOne from order04 as of 1.OCT.2015, verify that backdated changes are not allowed.
                try {
                    removeProductFromOrder(testEnv, api, order04, prod01, new LocalDate(2015, 10, 1).toDate());
                } catch (SessionInternalError e) {
                    assertTrue(e.getMessage().matches(".*validation.error.incorrect.effective.date\\s*"));
                }

                // Run real billing run for date 1.DEC.2015
                triggerBilling(api, new LocalDate(2015, 12, 1).toDate(), false, proratingAuto, 1, monthlyUnitId);

                // Step 26. - Verify the correct values for the invoice lines in the invoice generated for 1.DEC.2015.
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("4"), new BigDecimal("200"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("-2"), new BigDecimal("-100"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("-1"), new BigDecimal("-50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("1"), new BigDecimal("-10"), new BigDecimal("-10"), 2);
                assertInvoiceLine(testEnv, api, user03, prod01, new LocalDate(2015, 11, 1), new LocalDate(2015, 11, 30),
                        new BigDecimal("1"), new BigDecimal("30"), new BigDecimal("30"), 1);
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 7, 1), new BigDecimal("174.19"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 8, 1), new BigDecimal("150"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 9, 1), new BigDecimal("50"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 10, 1), new BigDecimal("50"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 11, 1), new BigDecimal("47.67"));
                assertCarriedInvoiceLine(testEnv, api, user03, new LocalDate(2015, 12, 1), new BigDecimal("30"));

                // Step 27. - Verify that no additional invoice lines were generated from the changes
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user03)).getInvoiceLines().length, 12);

                // Step 44. - Run real billing run for date 1.JAN.2016. Verify generated invoice lines.
                // Create order change with quantity = 1 for order08 as of 8.JAN.2016.
                // Run real billing run for date 8.JAN.2016. Verify generated invoice lines.
                // Run real billing run for date 15.JAN.2016. Verify generated invoice lines.
                triggerBilling(api, new LocalDate(2016, 1, 1).toDate(), false, proratingAuto, 15, monthlyUnitId);
                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 1, 1), new LocalDate(2016, 1, 7),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 1, 8), new LocalDate(2016, 1, 14),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 1, 15), new LocalDate(2016, 1, 21),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 1, 22), new LocalDate(2016, 1, 28),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 1, 29), new LocalDate(2016, 2, 4),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user08, prod01, new LocalDate(2015, 12, 1), new LocalDate(2015, 12, 15),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user08, prod01, new LocalDate(2015, 12, 16), new LocalDate(2015, 12, 31),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                changeOrderPriceAndQuantity(testEnv, api, order08, prod01,
                        null, new BigDecimal("1"), new LocalDate(2016, 1, 8).toDate());
                triggerBilling(api, new LocalDate(2016, 1, 8).toDate(), false, proratingAuto, 15, monthlyUnitId);
                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 2, 5), new LocalDate(2016, 2, 11),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user07, new LocalDate(2016, 2, 1), new BigDecimal("250"));

                assertInvoiceLine(testEnv, api, user08, prod01, new LocalDate(2016, 1, 1), new LocalDate(2016, 1, 15),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user08, prod01, new LocalDate(2016, 1, 16), new LocalDate(2016, 1, 31),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user08, prod01, new LocalDate(2016, 1, 8), new LocalDate(2016, 1, 15),
                        new BigDecimal("1"), new BigDecimal("26.66"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user08, prod01, new LocalDate(2016, 1, 16), new LocalDate(2016, 1, 31),
                        new BigDecimal("1"), new BigDecimal("50.00"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user08, new LocalDate(2016, 2, 1), new BigDecimal("300"));

                triggerBilling(api, new LocalDate(2016, 1, 15).toDate(), false, proratingAuto, 15, monthlyUnitId);

                assertInvoiceLine(testEnv, api, user07, prod01, new LocalDate(2016, 2, 12), new LocalDate(2016, 2, 18),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertCarriedInvoiceLine(testEnv, api, user07, new LocalDate(2016, 2, 1), new BigDecimal("250"));
                assertCarriedInvoiceLine(testEnv, api, user07, new LocalDate(2016, 2, 8), new BigDecimal("50"));


                // Step 45. - Verify generated invoice lines.
                // Create order change with quantity = -1 for prod03 in order09 as of 16.MAR.2016.
                // Run real billing run for date 1.MAR.2016. Verify generated invoice lines.
                assertInvoiceLine(testEnv, api, user09, prod03, new LocalDate(2016, 2, 1), new LocalDate(2016, 2, 15),
                        new BigDecimal("2"), new BigDecimal("20"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user09, prod03, new LocalDate(2016, 2, 16), new LocalDate(2016, 2, 29),
                        new BigDecimal("2"), new BigDecimal("20"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user09, prod04, new LocalDate(2016, 2, 1), new LocalDate(2016, 2, 29),
                        new BigDecimal("1"), new BigDecimal("20"), new BigDecimal("20"), 1);
                assertInvoiceLine(testEnv, api, user09, prod05, new LocalDate(2016, 2, 1), new LocalDate(2016, 2, 29),
                        new BigDecimal("1"), new BigDecimal("40"), new BigDecimal("40"), 1);

                changeOrderPriceAndQuantity(testEnv, api, order09, prod03,
                        null, new BigDecimal("-1"), new LocalDate(2016, 3, 16).toDate());
                triggerBilling(api, new LocalDate(2016, 3, 1).toDate(), false, proratingAuto, 15, monthlyUnitId);

                assertInvoiceLine(testEnv, api, user09, prod03, new LocalDate(2016, 3, 1), new LocalDate(2016, 3, 15),
                        new BigDecimal("2"), new BigDecimal("20"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user09, prod03, new LocalDate(2016, 3, 16), new LocalDate(2016, 3, 31),
                        new BigDecimal("2"), new BigDecimal("20"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user09, prod03, new LocalDate(2016, 3, 16), new LocalDate(2016, 3, 31),
                        new BigDecimal("-1"), new BigDecimal("-10"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user09, prod04, new LocalDate(2016, 3, 1), new LocalDate(2016, 3, 31),
                        new BigDecimal("1"), new BigDecimal("20"), new BigDecimal("20"), 1);
                assertInvoiceLine(testEnv, api, user09, prod05, new LocalDate(2016, 3, 1), new LocalDate(2016, 3, 31),
                        new BigDecimal("1"), new BigDecimal("40"), new BigDecimal("40"), 1);
                assertCarriedInvoiceLine(testEnv, api, user09, new LocalDate(2016, 2, 8), new BigDecimal("100"));

                // Step 46. - Run real billing run for date 1.APR.2016. Verify generated invoice lines.
                // Create order change with quantity = 1 and price = 40 for product01 in order11 as of 5.APR.2016.
                // Verify that backdated changes are not allowed.
                // Create order change with quantity = -1 and price = 60 for product01 in order10 as of 17.APR.2016.
                // Create order change with quantity = 1 and price = 30 for product01 in order11 as of 20.APR.2016.
                // Run real billing run for date 16.APR.2016. Verify generated invoice lines.
                // Run real billing run for date 1.MAY.2016. Verify generated invoice lines.
                triggerBilling(api, new LocalDate(2016, 4, 1).toDate(), false, proratingAuto, 1, semiMonthlyUnitId);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 1), new LocalDate(2016, 4, 15),
                        new BigDecimal("5"), new BigDecimal("250"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 9), new LocalDate(2016, 4, 15),
                        new BigDecimal("3"), new BigDecimal("70"), new BigDecimal("50"), 1);
                try {
                    changeOrderPriceAndQuantity(testEnv, api, order11, prod01,
                            new BigDecimal("40"), new BigDecimal("1"), new LocalDate(2016, 4, 5).toDate());
                } catch (SessionInternalError e) {
                    assertTrue(e.getMessage().matches(".*OrderChangeWS,startDate,validation.error.incorrect.start.date\\s*"));
                }
                changeOrderPriceAndQuantity(testEnv, api, order10, prod01,
                        new BigDecimal("60"), new BigDecimal("-1"), new LocalDate(2016, 4, 17).toDate());
                changeOrderPriceAndQuantity(testEnv, api, order11, prod01,
                        new BigDecimal("30"), new BigDecimal("1"), new LocalDate(2016, 4, 20).toDate());
                triggerBilling(api, new LocalDate(2016, 4, 16).toDate(), false, proratingAuto, 1, semiMonthlyUnitId);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 16), new LocalDate(2016, 4, 30),
                        new BigDecimal("5"), new BigDecimal("250"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 17), new LocalDate(2016, 4, 30),
                        new BigDecimal("-1"), new BigDecimal("-46.66"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 17), new LocalDate(2016, 4, 30),
                        new BigDecimal("4"), new BigDecimal("37.33"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 16), new LocalDate(2016, 4, 30),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 20), new LocalDate(2016, 4, 30),
                        new BigDecimal("1"), new BigDecimal("36.66"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 4, 20), new LocalDate(2016, 4, 30),
                        new BigDecimal("4"), new BigDecimal("-58.66"), new BigDecimal("-20"), 1);
                assertCarriedInvoiceLine(testEnv, api, user10, new LocalDate(2016, 4, 16), new BigDecimal("320"));
                triggerBilling(api, new LocalDate(2016, 5, 1).toDate(), false, proratingAuto, 1, semiMonthlyUnitId);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 5, 1), new LocalDate(2016, 5, 15),
                        new BigDecimal("5"), new BigDecimal("250"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 5, 1), new LocalDate(2016, 5, 15),
                        new BigDecimal("-1"), new BigDecimal("-50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 5, 1), new LocalDate(2016, 5, 15),
                        new BigDecimal("4"), new BigDecimal("40"), new BigDecimal("10"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 5, 1), new LocalDate(2016, 5, 15),
                        new BigDecimal("3"), new BigDecimal("150"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 5, 1), new LocalDate(2016, 5, 15),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertInvoiceLine(testEnv, api, user10, prod01, new LocalDate(2016, 5, 1), new LocalDate(2016, 5, 15),
                        new BigDecimal("4"), new BigDecimal("-80"), new BigDecimal("-20"), 1);
                assertCarriedInvoiceLine(testEnv, api, user10, new LocalDate(2016, 4, 16), new BigDecimal("320"));
                assertCarriedInvoiceLine(testEnv, api, user10, new LocalDate(2016, 5, 1), new BigDecimal("368.67"));

                // Step 48. - Verify generated invoice for 1.MAY.2016.
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user12)).getCreateDatetime(),
                        new LocalDate(2016, 5, 1).toDate());
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user12)).getDueDate(),
                        new LocalDate(2016, 5, 16).toDate());

                // Step 59. - Run real billing run for date 1.JUN.2016. Invoice periods set to 99.
                // Verify generated invoice lines, and that no future periods are billed.
                triggerBilling(api, new LocalDate(2016, 6, 1).toDate(), false, proratingAuto, 99, monthlyUnitId);
                assertInvoiceLine(testEnv, api, user17, prod01, new LocalDate(2016, 6, 1), new LocalDate(2016, 6, 30),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user17)).getInvoiceLines().length, 1);
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user17)).getDueDate(), new LocalDate(2016, 7, 1).toDate());

                // Test for user19 who have 3 Monthly period on billing run for date 1.JUN.2016
                assertEquals(api.getLatestInvoice(testEnv.idForCode(user19)).getInvoiceLines().length, 1);
                assertEquals(api.getUserWS(testEnv.idForCode(user19)).getNextInvoiceDate(), new LocalDate(2016, 9, 1).toDate());
                assertEquals(api.getOrder(testEnv.idForCode(order19)).getNextBillableDay(), new LocalDate(2016, 6, 1).toDate());

                // Step 60. - Run real billing run for date 15.JUN.2016. Invoice periods set to 99.
                // Verify generated invoice lines, and that no future periods are billed.
                triggerBilling(api, new LocalDate(2016, 6, 15).toDate(), false, proratingAuto, 1, semiMonthlyUnitId);
                assertInvoiceLine(testEnv, api, user18, prod01, new LocalDate(2016, 6, 15), new LocalDate(2016, 6, 29),
                        new BigDecimal("1"), new BigDecimal("50"), new BigDecimal("50"), 1);
            } finally {
                // Cleanup
                deleteGeneratedInvoices(testEnv, api,
                        user01, user02, user03, user04, user05, user06, user07, user08, user09, user10,
                        user11, user12, user13, user14, user15, user16, user17);
            }
        });

    }

    private void changeOrderPriceAndQuantity(TestEnvironment testEnv, JbillingAPI api, String orderCode, String prodCode,
                                             BigDecimal price, BigDecimal quantity, Date effectiveDate) {

        OrderWS orderWS = api.getOrder(testEnv.idForCode(orderCode));
        OrderChangeWS orderChangeWS = null;
        for (OrderLineWS line : orderWS.getOrderLines()) {
            if (line.getItemId().equals(testEnv.idForCode(prodCode))) {
                if (null != price) {
                    line.setUseItem(false);
                }
                orderChangeWS = OrderBuilder.buildChangeFromLine(
                        orderWS, line, applyStatusId, effectiveDate);
            }
        }
        if (null == orderChangeWS) {
            for (OrderWS childOrder : orderWS.getChildOrders()) {
                for (OrderLineWS line : childOrder.getOrderLines()) {
                    if (line.getItemId().equals(testEnv.idForCode(prodCode))) {
                        if (null != price) {
                            line.setUseItem(false);
                        }
                        orderWS = childOrder;
                        orderChangeWS = OrderBuilder.buildChangeFromLine(
                                orderWS, line, applyStatusId, effectiveDate);
                        break;
                    }
                }
            }
        }
        if (null == orderChangeWS) {
            fail("Could not find order line with product for code: " + prodCode + ".");
        }
        if (null != price) {
            orderChangeWS.setPrice(price);
        }
        if (null != quantity) {
            orderChangeWS.setQuantity(quantity);
        }
        api.updateOrder(orderWS, new OrderChangeWS[]{orderChangeWS});
    }

    private void addProductToOrder(TestEnvironment testEnv, JbillingAPI api, String orderCode, String prodCode,
                                   BigDecimal quantity, Date effectiveDate) {

        OrderLineWS newLine = new OrderLineWS();
        newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        newLine.setQuantity(quantity);
        newLine.setItemId(testEnv.idForCode(prodCode));
        newLine.setUseItem(true);

        OrderWS orderWS = api.getOrder(testEnv.idForCode(orderCode));
        OrderLineWS[] orderLines = new OrderLineWS[orderWS.getOrderLines().length + 1];
        for (int i = 0; i < orderWS.getOrderLines().length; i++) {
            orderLines[i] = orderWS.getOrderLines()[i];
        }
        orderLines[orderWS.getOrderLines().length] = newLine;
        orderWS.setOrderLines(orderLines);

        OrderChangeWS orderChangeWS = OrderBuilder.buildChangeFromLine(orderWS, newLine, applyStatusId, effectiveDate);
        api.updateOrder(orderWS, new OrderChangeWS[]{orderChangeWS});
    }

    private void removeProductFromOrder(TestEnvironment testEnv, JbillingAPI api, String orderCode, String prodCode,
                                        Date effectiveDate) {

        BigDecimal quantity = null;
        OrderWS orderWS = api.getOrder(testEnv.idForCode(orderCode));
        OrderChangeWS orderChangeWS = null;
        for (OrderLineWS line : orderWS.getOrderLines()) {
            if (line.getItemId().equals(testEnv.idForCode(prodCode))) {
                orderChangeWS = OrderBuilder.buildChangeFromLine(orderWS, line, applyStatusId, effectiveDate);
                quantity = line.getQuantityAsDecimal().negate();
            }
        }
        if (null == orderChangeWS) {
            fail("Could not find order line with product for code: " + prodCode + ".");
        }
        orderChangeWS.setQuantity(quantity);
        orderChangeWS.setRemoval(1);
        api.updateOrder(orderWS, new OrderChangeWS[]{orderChangeWS});
    }

    private void assertInvoiceLine(TestEnvironment testEnv, JbillingAPI api, String userCode, String prodCode,
                                   LocalDate start, LocalDate end, BigDecimal quantity, BigDecimal amount,
                                   BigDecimal price, Integer times) {

        Integer found = 0;
        DateTimeFormatter format = DateTimeFormat.forPattern("MM/dd/yyyy");
        String itemDescription = api.getItem(testEnv.idForCode(prodCode), null, null).getDescription();
        String description = itemDescription + " Period from " + start.toString(format) + " to " + end.toString(format);
        for (InvoiceLineDTO line : api.getLatestInvoice(testEnv.idForCode(userCode)).getInvoiceLines()) {
            if (line.getDescription().equals(description) &&
                    compareBigDecimalValue(line.getQuantityAsDecimal(), quantity) &&
                    compareBigDecimalValue(line.getAmountAsDecimal(), amount) &&
                    compareBigDecimalValue(line.getPriceAsDecimal(), price)) {
                found += 1;
                if (found.equals(times)) {
                    return;
                }
            }
        }
        fail("Invoice line with description: " + description +
                ", quantity: " + quantity.toString() +
                ", amount: " + amount.toString() +
                ", price: " + price.toString() +
                " was found " + found + " times, expected was " + times + " times.");
    }

    private void assertCarriedInvoiceLine(TestEnvironment testEnv, JbillingAPI api, String userCode,
                                          LocalDate dueDate, BigDecimal amount) {

        DateTimeFormatter format = DateTimeFormat.forPattern("MM/dd/yyyy");
        String description = "Carried Invoice number (.*) due date " + dueDate.toString(format);
        for (InvoiceLineDTO line : api.getLatestInvoice(testEnv.idForCode(userCode)).getInvoiceLines()) {
            if (line.getDescription().matches(description) &&
                    compareBigDecimalValue(line.getAmountAsDecimal(), amount)) {
                return;
            }
        }
        fail("Carried invoice line for due date: " + dueDate.toString(format) +
                " and  amount: " + amount.toString() + " was not found.");
    }

    private boolean compareBigDecimalValue(BigDecimal one, BigDecimal two) {

        return one.setScale(2, BigDecimal.ROUND_DOWN).equals(two.setScale(2, BigDecimal.ROUND_DOWN));
    }

    public Integer buildAndPersistOrderChangeStatus(TestEnvironmentBuilder envBuilder, JbillingAPI api,
                                                    String description, ApplyToOrder applyToOrder) {

        return envBuilder.orderChangeStatusBuilder(api)
                .withApplyToOrder(applyToOrder)
                .withDescription(description)
                .build();
    }

    public Integer buildAndPersistOrderPeriod(TestEnvironmentBuilder envBuilder, JbillingAPI api,
                                              String description, Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api)
                .withDescription(description)
                .withValue(value)
                .withUnitId(unitId)
                .build();
    }

    public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .global(global)
                .build();
    }

    public Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
                                              boolean global, Integer categoryId, String flatPrice) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(flatPrice)
                .global(global)
                .build();
    }

    public Integer buildAndPersistGraduatedProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
                                                   boolean global, Integer categoryId, String graduatedPrice, String includedUnits) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withGraduatedPrice(graduatedPrice, includedUnits)
                .global(global)
                .build();
    }

    private PlanItemWS buildPlanItem(JbillingAPI api, Integer itemId, Integer periodId, String quantity, String price) {

        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .addModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .withBundledPeriodId(periodId)
                .withBundledQuantity(quantity)
                .build();
    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
                                        Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    public Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name) {

        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .build();

        return accountTypeWS.getId();
    }

    public Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, JbillingAPI api, String username,
                                           Integer accountTypeId, Date nextInvoiceDate, Integer periodId, Integer nextInvoiceDay) {

        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(username)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate)
                .withMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay))
                .build();

        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);

        return userWS.getId();
    }

    public OrderWS buildOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, Integer userId,
                              Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId,
                              boolean prorate, Integer... productsIds) {

        return envBuilder.orderBuilder(api)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withPeriod(orderPeriodId)
                .withBillingTypeId(billingTypeId)
                .withProrate(prorate)
                .withProducts(productsIds)
                .buildOrder();
    }

    public Integer persistOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, OrderWS order,
                                OrderChangeWS[] orderChanges) {

        return envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .persistOrder(order, orderChanges);
    }

    public Integer buildAndPersistOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, Integer userId,
                                        Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId,
                                        boolean prorate, Map<Integer, BigDecimal> productQuantityMap) {

        OrderBuilder orderBuilder = envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(activeSince)
                .withPeriod(orderPeriodId)
                .withBillingTypeId(billingTypeId)
                .withProrate(prorate);

        for (Map.Entry<Integer, BigDecimal> entry : productQuantityMap.entrySet()) {
            orderBuilder.withOrderLine(
                    orderBuilder.orderLine()
                            .withItemId(entry.getKey())
                            .withQuantity(entry.getValue())
                            .build());
        }

        return orderBuilder.build();
    }

    private OrderPeriodWS findOrderPeriodById(Integer periodId, JbillingAPI api) {

        for (OrderPeriodWS periodWS : api.getOrderPeriods()) {
            if (periodWS.getId().equals(periodId)) {
                return periodWS;
            }
        }
        return null;
    }

    private void triggerBilling(JbillingAPI api, Date runDate, Boolean review, String prorating,
                                Integer numPeriods, Integer periodUnitId) {
        try {
			BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
			config.setNextRunDate(runDate);
			config.setRetries(1);
			config.setDaysForRetry(5);
			config.setGenerateReport(review ? 1 : 0);
			config.setAutoPaymentApplication(0);
			config.setDfFm(0);
			config.setPeriodUnitId(new Integer(periodUnitId));
			config.setDueDateUnitId(periodUnitId);
			config.setDueDateValue(1);
			config.setInvoiceDateProcess(0);
			config.setMaximumPeriods(numPeriods);
			config.setOnlyRecurring(0);
			config.setProratingType(prorating);

			api.createUpdateBillingProcessConfiguration(config);
			api.triggerBilling(runDate);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    private void deleteGeneratedInvoices(TestEnvironment testEnv, JbillingAPI api, String... userCodes) {

        for (String userCode : userCodes) {

            for (Integer invoiceId : api.getLastInvoices(testEnv.idForCode(userCode), 48)) {
                api.deleteInvoice(invoiceId);
            }
        }
    }

}
