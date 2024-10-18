package com.sapienter.jbilling.server.MeteredUsageService;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.utility.DateUtility;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@Test(groups = {"meteredUsageService"}, testName = "ReservedIncludedUsageTest")
public class ReservedIncludedUsageTest extends MeteredUsageTestAdapter{


	private static final String PRODUCT_FLAT_PRICE = "10";
	private static final String PLAN_FLAT_PRICE = "2200";
	private static final String MONTHLY_PLAN_TEST_CODE = "Reserved 12 Monthly Plan";
	private static final String UPFRONT_PLAN_TEST_CODE = "Reserved 12 Upfront Plan";
	private static final String USAGE_POOL_CODE = "fup_dynamic";
	private static final String USAGE_POOL_CYCLE_PERIOD_MONTHS = "Months";

	protected static final BigDecimal ORDER_LINE_PLAN_QUANTITY = BigDecimal.valueOf(1);
	protected static final BigDecimal ORDER_LINE_PLAN_AMOUNT = BigDecimal.valueOf(200);


	public static final String ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF = "Last Reserved Monthly Report Date";

	private Integer enumId1;
	private Integer enumId2;
	private Integer fupId;
	private Integer monthlyPlanId;
	private Integer monthlyPlanOrderId;
	private Integer itemId;
	private PriceModelWS flatPriceModel;
	private Integer categoryId;
	private Integer userId;

	private Integer upfrontPlanId;
	private Integer upfrontPlanOrderId;
	private Integer[] usagePoolsId;

	protected Integer scheduledTaskPluginId;

	TestEnvironmentBuilder environmentBuilder;


	@BeforeClass
	public void init() {
		super.init();
		meteredConfigBuilder = getMeteredSetup();
	}

	@AfterClass
	public void tearDown() {
		super.tearDown();
		clear();
	}

	private TestBuilder getMeteredSetup() {

		return TestBuilder.newTest().givenForMultiple(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			environmentBuilder = envBuilder;

			buildAndPersistMetafield(envBuilder, METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER, DataType.STRING, EntityType.CUSTOMER);

			userId = buildAndPersistUser(envBuilder, CUSTOMER_CODE, customerMetaFieldValues);

			setUpOrderStatus(userId,envBuilder);

			buildAndPersistMeteredPlugin(envBuilder, INTERNAL_TASK_PLUGIN_CODE, INTERNAL_EVENT_PLUGIN_TYPE_ID)  ;

			buildAndPersistMetafield(envBuilder, ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF, DataType.DATE, EntityType.ORDER);

			categoryId = buildAndPersistCategory(envBuilder, api, TEST_CATEGORY_CODE, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

			flatPriceModel = buildFlatPriceModel(PRODUCT_FLAT_PRICE, api.getCallerCurrencyId());

			itemId = buildAndPersistProduct(envBuilder,  TEST_ITEM_CODE, true, envBuilder.idForCode(TEST_CATEGORY_CODE), flatPriceModel, true, TEST_ITEM_DES);



			usagePoolsId = new Integer[1];

			fupId  = createFreeUsagePool(envBuilder,"Usage pool For Reserved" +System.currentTimeMillis(), "0", USAGE_POOL_CYCLE_PERIOD_MONTHS, "Hours Per Calendar Month", itemId);

			usagePoolsId[0] = fupId;


			enumId1 = buildAndPersistEnumeration(envBuilder, ENUM_DURATION_VALUES, METAFIELD_DURATION);
			enumId2 = buildAndPersistEnumeration(envBuilder, ENUM_PAYMENT_OPTION_VALUES, METAFIELD_PAYMENT_OPTION);

			buildAndPersistMetafield(envBuilder, METAFIELD_PAYMENT_OPTION, DataType.ENUMERATION, EntityType.PLAN);
			buildAndPersistMetafield(envBuilder, METAFIELD_DURATION, DataType.ENUMERATION, EntityType.PLAN);

			MetaFieldValueWS[] upfrontPlanMetafields = getupPlanMetafieldsValue("12", "UPFRONT");
			upfrontPlanId = buildAndPersistReservedPlan(envBuilder,  UPFRONT_PLAN_TEST_CODE, itemId, upfrontPlanMetafields,  usagePoolsId , UPFRONT_PLAN_TEST_CODE, flatPriceModel, categoryId, ORDER_LINE_PLAN_AMOUNT);
			Integer planItemId  = api.getPlanWS(upfrontPlanId).getItemId();

			List<OrderLineWS> planOrderLines = new ArrayList<OrderLineWS>() {
				{
					add(buildOrderLines(envBuilder, planItemId , ORDER_LINE_PLAN_DESCRIPTION, ORDER_LINE_PLAN_QUANTITY, ORDER_LINE_PLAN_AMOUNT));
				}};

			OrderWS planOrder = buildOrder(envBuilder, userId, planOrderLines, MONTHLY_ORDER_PERIOD_ID );

			upfrontPlanOrderId  = envBuilder.orderBuilder(api).withCodeForTests(UPFRONT_PLAN_TEST_CODE).persistOrder(planOrder);

			List<OrderLineWS> itemOrderLines = new ArrayList<OrderLineWS>() {
				{
					add(buildOrderLines(envBuilder, itemId, ORDER_LINE_ITEM_DESCRIPTION, ORDER_LINE_ITEM_QUANTITY, ORDER_LINE_ITEM_AMOUNT));
				}};
			OrderWS itemOrder = buildOrder(envBuilder, userId, itemOrderLines, MONTHLY_ORDER_PERIOD_ID );

			itemOrder.setIsMediated(true);

			envBuilder.orderBuilder(api).withCodeForTests(ORDER_CODE).persistOrder(itemOrder);

			scheduledTaskPluginId = buildAndPersistMeteredPlugin(envBuilder, SCHEDULED_TASK_PLUGIN_CODE, SCHEDULED_TASK_PLUGIN_TYPE_ID)  ;

		});
	}

	@Test(priority = 1)
	public void testReserve_UPFRONT_PlanPurchaseReportTest() {

		String expectedDescription  = getReservedPurchaseDescription(UPFRONT_PLAN_TEST_CODE);

		Map<String,Object> planMetaFields = getupPlanMetafields("12", "UPFRONT");
		String expectedCustomUnit = getReservedPurchaseCustomUnit(Integer.toString(upfrontPlanId),  planMetaFields);
		int itemCount = 1;

		wireMockServer.verify(postRequestedFor(urlMatching(METERED_API))
			.withRequestBody(matchingJsonPath("$.account[?(@.accountIdentifier == "+"\""+TEST_EXTERNAL_ACCOUNT_IDENTIFIER+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.quantity == "+ORDER_LINE_PLAN_QUANTITY+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.price == "+ ORDER_LINE_PLAN_AMOUNT+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+expectedDescription+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+expectedCustomUnit+"\""+")]"))
			.withRequestBody(matchingJsonPath("$[?(@.items.size() == "+itemCount+")]")));



	}

	@Test(priority = 2)
	public void testReserve_MONTHLY_PlanPurchaseReportTest() {

		JbillingAPI api = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();

		MetaFieldValueWS[] monthlyPlanMetafields = getupPlanMetafieldsValue("12", "MONTHLY");

		monthlyPlanId = buildAndPersistReservedPlan(environmentBuilder,  MONTHLY_PLAN_TEST_CODE, itemId, monthlyPlanMetafields,  usagePoolsId , MONTHLY_PLAN_TEST_CODE, flatPriceModel, categoryId, ORDER_LINE_PLAN_AMOUNT);

		Integer planItemId = api.getPlanWS(monthlyPlanId).getItemId();


		List<OrderLineWS> planOrderLines = new ArrayList<OrderLineWS>() {
			{
				add(buildOrderLines(environmentBuilder, planItemId , ORDER_LINE_PLAN_DESCRIPTION, ORDER_LINE_PLAN_QUANTITY, ORDER_LINE_PLAN_AMOUNT));
			}};

		OrderWS planOrder = buildOrder(environmentBuilder, userId, planOrderLines, MONTHLY_ORDER_PERIOD_ID );

		monthlyPlanOrderId  = environmentBuilder.orderBuilder(api).withCodeForTests(MONTHLY_PLAN_TEST_CODE).persistOrder(planOrder);

		String expectedDescription  = getReservedPurchaseDescription(MONTHLY_PLAN_TEST_CODE);

		Map<String,Object> planMetaFields = getupPlanMetafields("12", "MONTHLY");

		String expectedCustomUnit = getReservedPurchaseCustomUnit(Integer.toString(monthlyPlanId),  planMetaFields);
		int itemCount = 1;

		Date planActiveDate = setTimeToStartOfDay(Calendar.getInstance());
		BigDecimal proratedQuantity = prorateQuantity(planActiveDate, lastDayOfMonth(planActiveDate) ,BigDecimal.ONE);

		wireMockServer.verify(postRequestedFor(urlMatching(METERED_API))
			.withRequestBody(matchingJsonPath("$.account[?(@.accountIdentifier == "+"\""+TEST_EXTERNAL_ACCOUNT_IDENTIFIER+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.quantity == "+proratedQuantity+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.price == "+ ORDER_LINE_PLAN_AMOUNT+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+expectedDescription+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+expectedCustomUnit+"\""+")]"))
			.withRequestBody(matchingJsonPath("$[?(@.items.size() == "+itemCount+")]")));
	}


	@Test(priority = 3)
	public void testReservePlanOrderLastReportedDateUpdated() {

		JbillingAPI parentApi = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();
		OrderWS upfrontPlanOrder = parentApi.getOrder(upfrontPlanOrderId);
		OrderWS monthlyPlanOrder = parentApi.getOrder(monthlyPlanOrderId);

		MetaFieldValueWS[] upfrontPlanOrderMetaFields = upfrontPlanOrder.getMetaFields();
		MetaFieldValueWS[] monthlyPlanOrderMetaFields = monthlyPlanOrder.getMetaFields();

		MetaFieldValueWS lastReportedDateUpfrontOrder = Arrays.stream(upfrontPlanOrderMetaFields).filter(value -> value.getMetaField().getName().equals(ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF)).findAny().orElse(null);

		MetaFieldValueWS lastReportedDateMonthlyOrder = Arrays.stream(monthlyPlanOrderMetaFields).filter(value -> value.getMetaField().getName().equals(ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF)).findAny().orElse(null);


		Assert.assertNotNull(ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF + "metafield for Monthly Plan Order should be updated as Plan Purchase is reported", lastReportedDateUpfrontOrder.getDateValue());

		Assert.assertNotNull(ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF + "metafield for  Upfront Plan Order should be updated as Plan Purchase is reported", lastReportedDateMonthlyOrder.getDateValue());
	}



	@Test(priority = 4)
	public void testReservedIncludedMeteredUsage()  {

		JbillingAPI parentApi = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();
		parentApi.triggerScheduledTask(scheduledTaskPluginId, new Date());
		sleep(10000);
		String expectedDescription  = getReservedIncludedDescription(TEST_ITEM_DES, TEST_BILLLING_UNIT);

		Map<String,Object> planMetaFieldValues = getupPlanMetafields("12", "UPFRONT");
		String expectedCustomUnit = getReservedIncludedCustomUnit(TEST_ITEM_CODE, TEST_BILLLING_UNIT, planMetaFieldValues);

		int itemCount = 1;

		wireMockServer.verify(postRequestedFor(urlMatching(METERED_API))
			.withRequestBody(matchingJsonPath("$.account[?(@.accountIdentifier == "+"\""+TEST_EXTERNAL_ACCOUNT_IDENTIFIER+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.quantity == "+ORDER_LINE_ITEM_QUANTITY+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.price == "+ BigDecimal.ZERO+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+expectedDescription+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+expectedCustomUnit+"\""+")]"))
			.withRequestBody(matchingJsonPath("$[?(@.items.size() == "+itemCount+")]")));
	}



	private String getReservedIncludedDescription(String productDescription, String billingUnit) {

		return String.format("%s, %s (%s)", productDescription, Constants.RESERVED_IDENTIFIER_DESCRIPTION, billingUnit);
	}

	private String getReservedIncludedCustomUnit(String productCode, String billingUnit, Map<String, Object> attributes) {

		return String.format("%s:%s:%s:%s", productCode,attributes.get(METAFIELD_PAYMENT_OPTION).toString(), attributes.get(METAFIELD_DURATION).toString(), billingUnit);
	}


	private String getReservedPurchaseDescription(String productDescription) {

		return String.format("%s", productDescription);
	}

	private String getReservedPurchaseCustomUnit(String productCode, Map<String, Object> attributes) {

		return String.format("%s:%s:%s", productCode,attributes.get(Constants.PLAN_PAYMENT_OPTION_MF), attributes.get(Constants.PLAN_DURATION_MF));
	}

	private BigDecimal prorateQuantity(Date fromDate, Date toDate, BigDecimal quantity) {

		long difference = toDate.getTime() - fromDate.getTime();

		long daysCharged = TimeUnit.DAYS.convert(difference, TimeUnit.MILLISECONDS) + 1;
		Integer noOfDaysInMonth = DateUtility.numberOfDaysInMonth(fromDate);

		return quantity.multiply(BigDecimal.valueOf(daysCharged).divide(new BigDecimal(noOfDaysInMonth), 6, RoundingMode.HALF_DOWN)).setScale(10);
	}

	private static Date lastDayOfMonth(Date date) {
		if (date == null) {
			return null;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		int lastDateInt = calendar.getActualMaximum(Calendar.DATE);
		calendar.set(Calendar.DATE, lastDateInt);
		setTimeToEndOfDay(calendar);
		return calendar.getTime();
	}

	private static void setTimeToEndOfDay(Calendar calendar) {

		calendar.set(Calendar.HOUR_OF_DAY, 11);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
	}

	public static Date setTimeToStartOfDay(Calendar calendar) {

		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		return calendar.getTime();
	}

	private void  clear () {

		JbillingAPI api =meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();

		api.deletePlan(monthlyPlanId);
		api.deletePlan(upfrontPlanId);
		//api.deleteUsagePool(fupId);
		api.deleteEnumeration(enumId1);
		api.deleteEnumeration(enumId2);
	}
}
