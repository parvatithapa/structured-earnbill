package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.catalogue.DtPlanWS;
import com.sapienter.jbilling.common.Util;

import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.utility.DateUtility;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.subscribe.DtSubscribeRequestPayload;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import jbilling.DtReserveInstanceService;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import wiremock.org.eclipse.jetty.http.HttpHeader;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.testng.Assert.*;

@Test(groups = {"dt-rest"}, testName = "DtReserveInstanceRestTest")
public class DtReserveInstanceRestTest extends RestTestCase {


	private static final String METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER = "externalAccountIdentifier";
	private static final String TEST_EXTERNAL_ACCOUNT_IDENTIFIER = "1b42fdd6-ade7-4f87-a8cb-3873ccd74283";

	private static final String METAFIELD_CONTACT_EMAIL = "contact.email";
	private static final String TEST_CONTACT_EMAIL = "user@test.com";

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");


	private static final Map<String, Object> customerMetaFieldValues = new HashMap<String, Object>() {
		{
			put(METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER, TEST_EXTERNAL_ACCOUNT_IDENTIFIER);
			put(METAFIELD_CONTACT_EMAIL, TEST_CONTACT_EMAIL);

		}
	};
	private static final String CUSTOMER_CODE = "testCustomer";
	private static final String TEST_CATEGORY_CODE = "testItemCategory";
	private static final String TEST_CATEGORY_CODE_INVALID = "testItemCategory2";
	private static final String PRODUCT_FLAT_PRICE = "10";
	private static final String TEST_ITEM_CODE = "test_OTC_Product";
	private static final String TEST_ITEM_DES = "otc product";
	private static final String TEST_PLAN_CODE = "PLAN 001";
	private static final String TEST_PLAN_CODE_NEW = "PLAN 002";
	private static final int MONTHLY_ORDER_PERIOD_ID = 2;


	private static final String METAFIELD_PAYMENT_OPTION = "Payment Option";
	private static final String METAFIELD_DURATION = "Duration";
	public static final String METAFIELD_FEATURES = "Features";
	private static final String USAGE_POOL_CYCLE_PERIOD_MONTHS = "Months";
	private static final BigDecimal ORDER_LINE_PLAN_AMOUNT = BigDecimal.valueOf(200);

	MetaFieldValueWS[] planMetafields = new MetaFieldValueWS[]{
			new MetaFieldValueWS(METAFIELD_PAYMENT_OPTION, null, null, false, "MONTHLY"),
			new MetaFieldValueWS(METAFIELD_DURATION, null, null, false, "12")

	};

	MetaFieldValueWS[] productMetafields = new MetaFieldValueWS[]{
			new MetaFieldValueWS(METAFIELD_FEATURES, null, null, false, "ram:32GB,cpu:8Core,os:UBUNTU")
	};

	private static final List<EnumerationValueWS> ENUM_DURATION_VALUES = new ArrayList<EnumerationValueWS>() {
		{
			add(new EnumerationValueWS("12"));
			add(new EnumerationValueWS("24"));
			add(new EnumerationValueWS("36"));

		}
	};

	private static final List<EnumerationValueWS> ENUM_PAYMENT_OPTION_VALUES = new ArrayList<EnumerationValueWS>() {

		{
			add(new EnumerationValueWS("MONTHLY"));
			add(new EnumerationValueWS("UPFRONT"));


		}
	};

	public static final String RESERVED_UPGRADE_DESCRIPTION = "Reserved Upgrade Adjustment";

	public static final String METAFIELD_ORDER_ADJUSTMENT = "Adjustment";
	public static final String METAFIELD_ORDER_UPGRADED_TO = "Upgraded to";
	public static final String METAFIELD_ORDER_REPORT_DATE = "Last Reserved Monthly Report Date";


	private static final String DEFAULT_METERED_API_ASYNC_MODE = "0";
	private static final String DEFAULT_METERED_API_CONNECT_TIMEOUT = "3000";
	private static final String DEFAULT_METERED_API_READ_TIMEOUT = "3000";
	private static final String DEFAULT_METERED_API_RETRIES = "2";
	private static final String DEFAULT_METERED_API_RETRY_WAIT = "2000";
	private static final String METERED_API_URL= "http://localhost:9000/api/integration/v1/billing/usage";
	private static final String METERED_API = "/api/integration/v1/billing/usage";
	private static final String METERED_API_CONSUMER_KEY = "DUMMY";
	private static final String METERED_API_CONSUMER_SECRET = "DUMMY";
	private  String UPLOADED_ORDER_STATUS_ID = "1";
	private  String FAILED_ORDER_STATUS_ID = "1";
	private static final String ACTIVE_ORDER_STATUS_ID = "1";

	private static final String RUN_POLICY = "1";

	private static final String PARAM_METERED_API_URL="metered_api_url";
	private static final String PARAM_METERED_API_CONSUMER_KEY = "metered_api_consumer_key";
	private static final String PARAM_METERED_API_CONSUMER_SECRET = "metered_api_consumer_secret";
	private static final String PARAM_METERED_API_ASYNC_MODE = "metered_api_async_mode";
	private static final String PARAM_UPLOADED_ORDER_STATUS_ID = "order_status_uploaded";
	private static final String PARAM_ACTIVE_ORDER_STATUS_ID = "order_status_active";
	private static final String PARAM_UPLOAD_FAILED_ORDER_STATUS_ID = "order_status_upload_failed";
	private static final String PARAM_METERED_API_CONNECT_TIMEOUT = "metered_api_connect_timeout (ms)";
	private static final String PARAM_METERED_API_READ_TIMEOUT = "metered_api_read_timeout (ms)";
	private static final String PARAM_METERED_API_RETRIES = "metered_api_retries";
	private static final String PARAM_METERED_API_RETRY_WAIT = "metered_api_retry_wait (ms)";
	private static final String PARAM_RUN_POLICY = "Run policy";


	private static final int INTERNAL_EVENT_PLUGIN_TYPE_ID = 253;
	private static final int PLUGIN_ORDER = 111;

	private static final String INTERNAL_TASK_PLUGIN_CODE = "testPluginInternal";
	private static final String STUB_SUCCESS_JSON_RESPONSE = "{\"success\":true,\"message\":\"Bill Usage API succeeded\"}";
	private WireMockServer wireMockServer;

	private Integer userId;
	private Integer categoryId;
	private Integer invalidCategoryId;
	private Integer enumId1;
	private Integer enumId2;
	private Integer planId;
	private Integer newPlanId;
	private Integer orderId;

	private Integer newOrderId;

	private TestBuilder testConfigBuilder;

	@BeforeClass
	public void init() {

		testConfigBuilder = getSetup();
		super.setup("v1/reserve/dt");


		wireMockServer = new WireMockServer(options().port(9000));
		wireMockServer.start();


		wireMockServer.stubFor(post(urlMatching(METERED_API))

			.willReturn(aResponse()
				.withStatus(200)
				.withHeader(HttpHeader.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON.toString())
				.withBody(STUB_SUCCESS_JSON_RESPONSE)));
	}

	@AfterClass
	public void tearDown() {

		JbillingAPI api = testConfigBuilder.getTestEnvironment().getPrancingPonyApi();
		testConfigBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		api.deleteEnumeration(enumId1);
		api.deleteEnumeration(enumId2);


		wireMockServer.stop();

	}

	private TestBuilder getSetup() {

		return TestBuilder.newTest().givenForMultiple(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();


			buildAndPersistMeteredPlugin(envBuilder, INTERNAL_TASK_PLUGIN_CODE, INTERNAL_EVENT_PLUGIN_TYPE_ID)  ;

			buildAndPersistMetafield(envBuilder, METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER, DataType.STRING, EntityType.CUSTOMER);

			buildAndPersistMetafield(envBuilder, METAFIELD_ORDER_ADJUSTMENT, DataType.DECIMAL, EntityType.ORDER);
			buildAndPersistMetafield(envBuilder, METAFIELD_ORDER_UPGRADED_TO , DataType.INTEGER, EntityType.ORDER);
			buildAndPersistMetafield(envBuilder, METAFIELD_ORDER_REPORT_DATE , DataType.DATE, EntityType.ORDER);
			buildAndPersistMetafield(envBuilder, METAFIELD_FEATURES , DataType.STRING, EntityType.PRODUCT);

			userId = buildAndPersistUser(envBuilder, CUSTOMER_CODE, customerMetaFieldValues);

			categoryId = buildAndPersistCategory(envBuilder, api, TEST_CATEGORY_CODE, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
			invalidCategoryId = buildAndPersistCategory(envBuilder, api, TEST_CATEGORY_CODE_INVALID, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
			PriceModelWS flatPriceModel = buildFlatPriceModel(PRODUCT_FLAT_PRICE, api.getCallerCurrencyId());

			Integer itemId = buildAndPersistProduct(envBuilder, TEST_ITEM_CODE, true, envBuilder.idForCode(TEST_CATEGORY_CODE), flatPriceModel, true, TEST_ITEM_DES, productMetafields);

			Integer[] usagePoolsId = new Integer[1];

			usagePoolsId[0] = createFreeUsagePool(envBuilder, "Usage pool For Reserved" + System.currentTimeMillis(), "0", USAGE_POOL_CYCLE_PERIOD_MONTHS, "Hours Per Calendar Month", itemId);


			enumId1 = buildAndPersistEnumeration(envBuilder, ENUM_DURATION_VALUES, METAFIELD_DURATION);
			enumId2 = buildAndPersistEnumeration(envBuilder, ENUM_PAYMENT_OPTION_VALUES, METAFIELD_PAYMENT_OPTION);

			buildAndPersistMetafield(envBuilder, METAFIELD_PAYMENT_OPTION, DataType.ENUMERATION, EntityType.PLAN);
			buildAndPersistMetafield(envBuilder, METAFIELD_DURATION, DataType.ENUMERATION, EntityType.PLAN);


			planId = buildAndPersistReservedPlan(envBuilder, TEST_PLAN_CODE, itemId, planMetafields, usagePoolsId, TEST_PLAN_CODE, flatPriceModel, categoryId, ORDER_LINE_PLAN_AMOUNT);

			newPlanId = buildAndPersistReservedPlan(envBuilder, TEST_PLAN_CODE_NEW, itemId, planMetafields, usagePoolsId, TEST_PLAN_CODE_NEW, flatPriceModel, categoryId, ORDER_LINE_PLAN_AMOUNT);
		});
	}


	private Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, String code,
										   boolean global, Integer categoryId, PriceModelWS priceModelWS, boolean allowDecimal,
										   String description, MetaFieldValueWS[] productMetafields) {

		SortedMap<Date, PriceModelWS> prices = new TreeMap<>();
		prices.put(new Date(), priceModelWS);
		ItemDTOEx item = new ItemDTOEx();
		item.setGlCode(code);
		item.setDefaultPrices(prices);
		item.setGlobal(global);
		item.setTypes(new Integer[]{categoryId});
		item.setHasDecimals(allowDecimal ? 1 : 0);
		item.setDescription(description);
		item.setNumber(code);
		item.setMetaFields(productMetafields);
		item.getMetaFieldsMap().put(envBuilder.getPrancingPonyApi().getCallerCompanyId(), productMetafields);
		return envBuilder.getPrancingPonyApi().createItem(item);

	}

	private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
		return envBuilder.itemBuilder(api)
				.itemType()
				.withCode(code)
				.withCategoryType(categoryType)
				.global(global)
				.build();
	}

	private Integer buildAndPersistMetafield(TestEnvironmentBuilder envBuilder, String name, DataType dataType, EntityType entityType) {
		MetaFieldWS value = new MetaFieldBuilder()
				.name(name)
				.dataType(dataType)
				.entityType(entityType)
				.primary(true)
				.build();
		JbillingAPI api = envBuilder.getPrancingPonyApi();
		Integer id = api.createMetaField(value);
		envBuilder.env().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
		return envBuilder.idForCode(name);

	}

	private Integer buildAndPersistUser(TestEnvironmentBuilder envBuilder, String userName, Map<String, Object> metaFieldValues) {


		CustomerBuilder builder = envBuilder.customerBuilder(envBuilder.getPrancingPonyApi()).addTimeToUsername(false).withUsername(userName);
		metaFieldValues.entrySet().stream().forEach(e -> builder.withMetaField(e.getKey(), e.getValue()));

		UserWS user = builder.build();

		user.setMetaFields(builder.buildMetaField());

		envBuilder.getPrancingPonyApi().updateUser(user);

		return user.getId();

	}


	private PriceModelWS buildFlatPriceModel(String flatPrice, Integer currencyId) {
		return new PriceModelWS(PriceModelStrategy.FLAT.name(),
				new BigDecimal(flatPrice), currencyId);


	}


	private Integer buildAndPersistReservedPlan(TestEnvironmentBuilder envBuilder, String code, Integer itemId, MetaFieldValueWS[] planMetafields, Integer[] usagePoolsId, String description, PriceModelWS price, Integer planTypeId, BigDecimal cost) {

		PlanWS plan = CreateObjectUtil.createPlan(envBuilder.getPrancingPonyApi().getCallerCompanyId(), BigDecimal.valueOf(2200), 1, planTypeId, 2, BigDecimal.ZERO, envBuilder.getPrancingPonyApi());


		ItemDTOEx planItem = new ItemDTOEx();
		planItem.setNumber(String.valueOf(new Date().getTime()));
		planItem.setEntityId(envBuilder.getPrancingPonyApi().getCallerCompanyId());
		planItem.setDescription(description);
		planItem.setDefaultPrice(price);
		planItem.setCurrencyId(1);
		planItem.setTypes(new Integer[]{planTypeId});
		planItem.setPrice(cost);
		planItem.setActiveSince(new Date());

		Integer planItemId = envBuilder.getPrancingPonyApi().createItem(planItem);

		plan.setDescription(description);


		PlanItemWS pi1 = new PlanItemWS();
		pi1.setItemId(itemId);
		pi1.setPrecedence(-1);
		pi1.setModel(price);

		SortedMap<Date, PriceModelWS> prices = new TreeMap<>();
		prices.put(Util.truncateDate(new Date()), new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, Integer.valueOf(1)));
		pi1.setModels(prices);

		plan.setPlanItems(new ArrayList<PlanItemWS>() {{
			add(pi1);
		}});

		plan.setMetaFields(planMetafields);
		plan.getMetaFieldsMap().put(envBuilder.getPrancingPonyApi().getCallerCompanyId(), planMetafields);
		plan.setDescription(description);
		plan.setUsagePoolIds(usagePoolsId);
		plan.setItemId(planItemId);
		plan.setPeriodId(2);

		Integer planId = envBuilder.getPrancingPonyApi().createPlan(plan);
		envBuilder.env().add(code, planId, plan.getDescription(), envBuilder.getPrancingPonyApi(), TestEntityType.PLAN);
		return planId;

	}


	private Integer createFreeUsagePool(TestEnvironmentBuilder envBuilder, String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue, Integer itemId) {
		UsagePoolWS usagePool = populateFreeUsagePoolObject(usagePoolName, quantity, cyclePeriodUnit, resetValue, envBuilder.getPrancingPonyApi().getCallerCompanyId(), itemId);
		Integer poolId = envBuilder.getPrancingPonyApi().createUsagePool(usagePool);
		return poolId;
	}

	private UsagePoolWS populateFreeUsagePoolObject(String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue, Integer entityId, Integer itemId) {

		UsagePoolWS usagePool = new UsagePoolWS();
		usagePool.setName(usagePoolName);
		usagePool.setQuantity(quantity);
		usagePool.setPrecedence(new Integer(1));
		usagePool.setCyclePeriodUnit(cyclePeriodUnit);
		usagePool.setCyclePeriodValue(new Integer(1));

		usagePool.setItems(new Integer[]{itemId});
		usagePool.setEntityId(entityId);
		usagePool.setUsagePoolResetValue(resetValue);

		return usagePool;
	}

	private Integer buildAndPersistEnumeration(TestEnvironmentBuilder envBuilder, List<EnumerationValueWS> values, String name) {

		EnumerationWS enUmeration = new EnumerationWS();

		enUmeration.setValues(values);
		enUmeration.setName(name);
		enUmeration.setEntityId(envBuilder.getPrancingPonyApi().getCallerCompanyId());

		Integer enumId = envBuilder.getPrancingPonyApi().createUpdateEnumeration(enUmeration);
		envBuilder.env().add(name, enumId, name, envBuilder.getPrancingPonyApi(), TestEntityType.ENUMERATION);
		return enumId;

	}

	private Integer buildAndPersistMeteredPlugin(TestEnvironmentBuilder envBuilder, String code, int pluginTypeId) {

		return  envBuilder.pluginBuilder(envBuilder.getPrancingPonyApi()).withCode(code).withOrder(PLUGIN_ORDER)
			.withParameter(PARAM_ACTIVE_ORDER_STATUS_ID, ACTIVE_ORDER_STATUS_ID)
			.withParameter(PARAM_METERED_API_ASYNC_MODE, DEFAULT_METERED_API_ASYNC_MODE)
			.withParameter(PARAM_METERED_API_CONNECT_TIMEOUT, DEFAULT_METERED_API_CONNECT_TIMEOUT)
			.withParameter(PARAM_METERED_API_CONSUMER_KEY, METERED_API_CONSUMER_KEY)
			.withParameter(PARAM_METERED_API_CONSUMER_SECRET, METERED_API_CONSUMER_SECRET)
			.withParameter(PARAM_METERED_API_READ_TIMEOUT, DEFAULT_METERED_API_READ_TIMEOUT)
			.withParameter(PARAM_METERED_API_RETRIES, DEFAULT_METERED_API_RETRIES)
			.withParameter(PARAM_METERED_API_RETRY_WAIT, DEFAULT_METERED_API_RETRY_WAIT)
			.withParameter(PARAM_METERED_API_URL, METERED_API_URL)
			.withParameter(PARAM_RUN_POLICY, RUN_POLICY)
			.withParameter(PARAM_UPLOAD_FAILED_ORDER_STATUS_ID, FAILED_ORDER_STATUS_ID)
			.withParameter(PARAM_UPLOADED_ORDER_STATUS_ID, UPLOADED_ORDER_STATUS_ID)
			.withTypeId(pluginTypeId).build().getId();

	}

	private String getAdjustmentUsageDescription(String initialOrderId, String newOrderid) {

		return String.format("%s:%s:%s", initialOrderId, newOrderid, RESERVED_UPGRADE_DESCRIPTION);
	}

	private String getAdjustmentUsageCustomUnit(String initialOrderId, String newOrderid) {

		return String.format("%s:%s", initialOrderId, newOrderid);
	}

	public BigDecimal getAdjustment(BigDecimal priceReported, Date activeUntil){
		Calendar cal = Calendar.getInstance();
		cal.setTime(activeUntil);
		int monthMaxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		int noOfDaysRemaining = monthMaxDays - cal.get(Calendar.DATE);
		BigDecimal adjustment = BigDecimal.valueOf(noOfDaysRemaining).divide(BigDecimal.valueOf(monthMaxDays),6, RoundingMode.HALF_DOWN);
		adjustment = adjustment.multiply(priceReported).setScale(10);
		return adjustment.negate();
	}
	@Test(priority = 2)
	public void testListReservePlanCatalogue() {

		String queryParam = "catalogue?pageNumber=1&pageSize=1&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=" + SearchCriteria.SortDirection.ASC +"&ram=32GB&cpu=8Core&os=UBUNTU";

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
				getOrDeleteHeaders, null);

		LinkedHashMap dtPlanList = (LinkedHashMap) response.getBody();

		List records = (List<DtPlanWS>) dtPlanList.get("records");

		assertEquals(records.size(), 1, "More than 1 plan found");

		LinkedHashMap dtPlan = (LinkedHashMap) records.get(0);

		assertEquals(200.0, dtPlan.get("planPrice"));
		assertEquals(Integer.valueOf(12), dtPlan.get("duration"));
		assertEquals("MONTHLY", dtPlan.get("paymentMode"));

	}

	@Test(priority = 3)
	public void testListCatalogueForInvalidParameter() {
	try{
		String queryParam = "catalogue?pageNumber=50&pageSize=100&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=" + SearchCriteria.SortDirection.ASC;

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
				getOrDeleteHeaders, null);
	}
	catch (HttpClientErrorException e){
		assertEquals(HttpStatus.BAD_REQUEST,e.getStatusCode());
	}
	}

	@Test(priority = 5)
	public void testSubscribePlan() {

		DtSubscribeRequestPayload payload = new DtSubscribeRequestPayload(TEST_EXTERNAL_ACCOUNT_IDENTIFIER, planId.toString(), new Date());

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
				postOrPutHeaders, payload);

		LinkedHashMap dtOrderPlan = (LinkedHashMap) response.getBody();

		orderId = (Integer) dtOrderPlan.get("orderId");

		assertEquals("PLAN 001", dtOrderPlan.get("enPlanName"));
		assertEquals(Integer.valueOf(12), dtOrderPlan.get("duration"));
		assertEquals("MONTHLY", dtOrderPlan.get("paymentMode"));
	}

	@Test(priority = 4)
	public void testListReservedSubscriptionWhenNoReservedInstanceAvailableForUser() {
	try{
		String queryParam = TEST_EXTERNAL_ACCOUNT_IDENTIFIER + "?pageNumber=1&pageSize=1&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=" + SearchCriteria.SortDirection.ASC;

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
				getOrDeleteHeaders, null);

	}
	catch (HttpClientErrorException e){
		assertEquals(HttpStatus.NOT_FOUND,e.getStatusCode());
	}
	}

	@Test(priority = 6)
	public void testListReservedSubscription() {

		String queryParam = TEST_EXTERNAL_ACCOUNT_IDENTIFIER + "?pageNumber=1&pageSize=1&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=" + SearchCriteria.SortDirection.ASC + "&ram=32GB";

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
				getOrDeleteHeaders, null);

		LinkedHashMap dtPlanList = (LinkedHashMap) response.getBody();

		List records = (List<DtPlanWS>) dtPlanList.get("records");

		assertEquals(records.size(), 1, "Customer subscribed to More than 1 plan");

		LinkedHashMap dtPlan = (LinkedHashMap) records.get(0);

		assertEquals(orderId, dtPlan.get("orderId"));
		assertEquals("Active", dtPlan.get("orderStatus"));
		assertEquals(Integer.valueOf(12), dtPlan.get("duration"));
		assertEquals("MONTHLY", dtPlan.get("paymentMode"));
	}

	@Test(priority = 6)
	public void testUpgradePlan() {

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, 2);

		DtSubscribeRequestPayload payload = new DtSubscribeRequestPayload(TEST_EXTERNAL_ACCOUNT_IDENTIFIER, newPlanId.toString(), cal.getTime());

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + orderId, HttpMethod.POST,
				postOrPutHeaders, payload);

		LinkedHashMap dtOrderPlan = (LinkedHashMap) response.getBody();

		newOrderId = (Integer) dtOrderPlan.get("orderId");

		assertEquals("PLAN 002", dtOrderPlan.get("enPlanName"));
		assertEquals(Integer.valueOf(12), dtOrderPlan.get("duration"));
		assertEquals("MONTHLY", dtOrderPlan.get("paymentMode"));
		assertEquals(sdf.format(cal.getTime()).toString(),dtOrderPlan.get("activeSince"));
		Calendar calx = Calendar.getInstance();
		calx.setTime(new Date());
		calx.add(Calendar.MONTH,12);
		calx.add(Calendar.DATE,-1);
		assertEquals(sdf.format(calx.getTime()).toString(),dtOrderPlan.get("activeUntil"));
	}


	@Test(priority = 7)
	public void testAdjustmentUsageTest() {


		Date activeUntill = DateUtility.addDaysToDate(new Date(), 1);
		Calendar cal = Calendar.getInstance();
		cal.setTime(activeUntill);
		DateUtility.setTimeToEndOfDay(cal);
		activeUntill = cal.getTime();

		BigDecimal adjustmentPrice = getAdjustment(ORDER_LINE_PLAN_AMOUNT, activeUntill);

		String expectedCustomUnit = getAdjustmentUsageCustomUnit(Integer.toString(orderId),Integer.toString(newOrderId));
		String expectedDescription = getAdjustmentUsageDescription(Integer.toString(orderId),Integer.toString(newOrderId));

		 wireMockServer.verify(postRequestedFor(urlMatching(METERED_API))
			 .withRequestBody(matchingJsonPath("$.account[?(@.accountIdentifier == "+"\""+TEST_EXTERNAL_ACCOUNT_IDENTIFIER+"\""+")]"))
			 .withRequestBody(matchingJsonPath("$.items[?(@.quantity == 1)]"))
			 .withRequestBody(matchingJsonPath("$.items[?(@.price == "+ adjustmentPrice +")]"))
			 .withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+expectedDescription+"\""+")]"))
			 .withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+expectedCustomUnit+"\""+")]")));


	 }

	 @Test(priority = 8)
	public void testCancelStatus() {

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + "cancelSubscription/" + TEST_EXTERNAL_ACCOUNT_IDENTIFIER, HttpMethod.GET,
				getOrDeleteHeaders, null);

		LinkedHashMap cancellationResponse = (LinkedHashMap) response.getBody();

		assertFalse((Boolean) cancellationResponse.get("cancellationAllowed"));

	}

	@Test(priority = 1)
	public void testCancelStatusWhenUserNotSubscribedPlan() {

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + "cancelSubscription/" + TEST_EXTERNAL_ACCOUNT_IDENTIFIER, HttpMethod.GET,
				getOrDeleteHeaders, null);

		LinkedHashMap cancellationResponse = (LinkedHashMap) response.getBody();

		assertTrue((Boolean) cancellationResponse.get("cancellationAllowed"));

	}

	@Test(priority = 9)
	public void testCancelStatusForInvalidSubscriptionId() {
		try {
			ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + "cancelSubscription/" + "Invalid Id", HttpMethod.GET,
					getOrDeleteHeaders, null);
			LinkedHashMap cancellationResponse = (LinkedHashMap) response.getBody();
		}
		catch (HttpClientErrorException e){
			assertEquals(HttpStatus.NOT_FOUND,e.getStatusCode());
		}
	}

	@Test(priority = 15)
	public void testCancelStatusForInvalidSubscriptionIdForPurchase() {
		DtSubscribeRequestPayload payload = new DtSubscribeRequestPayload("Invalid Id", planId.toString(), new Date());

		try {
			ResponseEntity<?> response = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
					postOrPutHeaders, payload);
			LinkedHashMap cancellationResponse = (LinkedHashMap) response.getBody();
		}
		catch (HttpClientErrorException e){
			assertEquals(HttpStatus.NOT_FOUND,e.getStatusCode());
		}
	}

	@Test(priority = 10)
	public void testListReservedSubscriptionForInvalidSubscriptionId() {
		try {
			String queryParam = TEST_EXTERNAL_ACCOUNT_IDENTIFIER + "?pageNumber=1&pageSize=1&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=" + SearchCriteria.SortDirection.ASC;
			ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
					getOrDeleteHeaders, null);

		} catch (HttpClientErrorException e) {
			assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
		}
	}

	@Test(priority = 11)
	public void testListReservedSubscriptionOfUserForInvalidParameter() {
		try {
			String queryParam = TEST_EXTERNAL_ACCOUNT_IDENTIFIER + "?pageNumber=1&pageSize=100&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=DSC";
			ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
					getOrDeleteHeaders, null);
		} catch (HttpClientErrorException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
		}
	}

	@Test(priority = 12)
	public void testListReservePlanCategory(){
		String queryParam = "categories";

		ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
				getOrDeleteHeaders, null);

		Map dtPlanList = (HashMap) response.getBody();

		List records = (List<String>) dtPlanList.get("Categories");

		assertEquals(records.size(), 1, "Wrong category returned");

	}

	@Test(priority = 13)
	public void testListReservePlanCatalogueProductFilterNotApplicable(){
		try {
			String queryParam = "catalogue?pageNumber=1&pageSize=1&sortField=" + DtReserveInstanceService.SORT_FIELD_ACTIVE_SINCE + "&sortOrder=" + SearchCriteria.SortDirection.ASC + "&ram=32GB&cpu=8Core&os=FEDORA";

			ResponseEntity<?> response = restTemplate.sendRequest(REST_URL + queryParam, HttpMethod.GET,
					getOrDeleteHeaders, null);
		}catch (HttpClientErrorException e) {

			assertEquals(HttpStatus.NO_CONTENT, e.getStatusCode());
		}
	}


}

