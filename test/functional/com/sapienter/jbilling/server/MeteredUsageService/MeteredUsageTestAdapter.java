package com.sapienter.jbilling.server.MeteredUsageService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.Assert;
import org.springframework.http.MediaType;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import wiremock.org.eclipse.jetty.http.HttpHeader;


public abstract class MeteredUsageTestAdapter {

	protected static final String TEST_ITEM_CODE = "test_OTC_Product";
	protected static final String TEST_CATEGORY_CODE = "testItemCategory";
	protected static final String TEST_ITEM_DES = "otc product";
	protected static final String TEST_BILLLING_UNIT = "unit";

	protected static final String CUSTOMER_CODE = "testCustomer";
	protected static final String ORDER_LINE_ITEM_DESCRIPTION = "testDescription";
	protected static final String ORDER_CODE = "testOrderCode";

	protected static final String INTERNAL_TASK_PLUGIN_CODE = "testPluginInternal";
	protected static final String SCHEDULED_TASK_PLUGIN_CODE = "testPluginScheduled";

	protected static final BigDecimal ORDER_LINE_ITEM_QUANTITY = BigDecimal.valueOf(5);
	protected static final BigDecimal ORDER_LINE_ITEM_AMOUNT = BigDecimal.valueOf(50);



	protected static final String ORDER_LINE_PLAN_DESCRIPTION = "testOrderDescription";

	protected static final int MONTHLY_ORDER_PERIOD_ID = 2;


	protected static final String DEFAULT_METERED_API_ASYNC_MODE = "0";
	protected static final String DEFAULT_METERED_API_CONNECT_TIMEOUT = "3000";
	protected static final String DEFAULT_METERED_API_READ_TIMEOUT = "3000";
	protected static final String DEFAULT_METERED_API_RETRIES = "2";
	protected static final String DEFAULT_METERED_API_RETRY_WAIT = "2000";
	protected static final String METERED_API_URL= "http://localhost:9000/api/integration/v1/billing/usage";
	protected static final String METERED_API = "/api/integration/v1/billing/usage";
	protected static final String METERED_API_CONSUMER_KEY = "DUMMY";
	protected static final String METERED_API_CONSUMER_SECRET = "DUMMY";
	protected  String UPLOADED_ORDER_STATUS_ID = "1";
	protected  String FAILED_ORDER_STATUS_ID = "1";
	protected static final String ACTIVE_ORDER_STATUS_ID = "1";

	protected static final String RUN_POLICY = "1";

	protected static final String PARAM_METERED_API_URL="metered_api_url";
	protected static final String PARAM_METERED_API_CONSUMER_KEY = "metered_api_consumer_key";
	protected static final String PARAM_METERED_API_CONSUMER_SECRET = "metered_api_consumer_secret";
	protected static final String PARAM_METERED_API_ASYNC_MODE = "metered_api_async_mode";
	protected static final String PARAM_UPLOADED_ORDER_STATUS_ID = "order_status_uploaded";
	protected static final String PARAM_ACTIVE_ORDER_STATUS_ID = "order_status_active";
	protected static final String PARAM_UPLOAD_FAILED_ORDER_STATUS_ID = "order_status_upload_failed";
	protected static final String PARAM_METERED_API_CONNECT_TIMEOUT = "metered_api_connect_timeout (ms)";
	protected static final String PARAM_METERED_API_READ_TIMEOUT = "metered_api_read_timeout (ms)";
	protected static final String PARAM_METERED_API_RETRIES = "metered_api_retries";
	protected static final String PARAM_METERED_API_RETRY_WAIT = "metered_api_retry_wait (ms)";
	protected static final String PARAM_RUN_POLICY = "Run policy";

	protected static final int SCHEDULED_TASK_PLUGIN_TYPE_ID = 220;
	protected static final int INTERNAL_EVENT_PLUGIN_TYPE_ID = 253;
	protected static final int PLUGIN_ORDER = 111;

	protected static final String METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER = "externalAccountIdentifier";
	protected static final String METAFIELD_CONTACT_EMAIL = "contact.email";
	protected static final String METAFIELD_PAYMENT_OPTION = "Payment Option";
	protected static final String METAFIELD_DURATION = "Duration";

	protected static final String TEST_EXTERNAL_ACCOUNT_IDENTIFIER = "1b42fdd6-ade7-4f87-a8cb-3873ccd74283";
	protected static final String TEST_CONTACT_EMAIL = "user@test.com";



	protected  static final Map<String, Object> customerMetaFieldValues = new HashMap<String, Object>() {
		{
			put(METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER, TEST_EXTERNAL_ACCOUNT_IDENTIFIER);
			put(METAFIELD_CONTACT_EMAIL, TEST_CONTACT_EMAIL);
		}};

	protected static final List<EnumerationValueWS> ENUM_DURATION_VALUES = new ArrayList<EnumerationValueWS>()  {
		{
			add(new EnumerationValueWS("12"));
			add(new EnumerationValueWS("24"));
			add(new EnumerationValueWS("36"));

		}};

	protected static final List<EnumerationValueWS> ENUM_PAYMENT_OPTION_VALUES = new ArrayList<EnumerationValueWS>()  {
		{
			add(new EnumerationValueWS("MONTHLY"));
			add(new EnumerationValueWS("UPFRONT"));


		}};




	Integer orderUploadStatusId;
	Integer orderFailedStatusId;

	protected WireMockServer wireMockServer;
	protected static final String STUB_SUCCESS_JSON_RESPONSE = "{\"success\":true,\"message\":\"Bill Usage API succeeded\"}";

	protected TestBuilder meteredConfigBuilder;

	public void init() {



		wireMockServer = new WireMockServer(options().port(9000));
		wireMockServer.start();


		wireMockServer.stubFor(post(urlMatching(METERED_API))

			.willReturn(aResponse()
				.withStatus(200)
				.withHeader(HttpHeader.CONTENT_TYPE.toString(), MediaType.APPLICATION_JSON.toString())
				.withBody(STUB_SUCCESS_JSON_RESPONSE)));


	}


	public void tearDown() {

		JbillingAPI api = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();
		meteredConfigBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		api.deleteOrderStatus(api.findOrderStatusById(orderFailedStatusId));

		wireMockServer.stop();
	}

	protected MetaFieldValueWS[] getupPlanMetafieldsValue(String duration, String paymentOption) {

		return new MetaFieldValueWS[] {
			new MetaFieldValueWS(METAFIELD_PAYMENT_OPTION, null, null, false, paymentOption),
			new MetaFieldValueWS(METAFIELD_DURATION, null, null, false, duration)

		};

	}

	protected Map getupPlanMetafields(String duration, String paymentOption) {
		return  new HashMap<String, Object>() {
			{
				put(METAFIELD_PAYMENT_OPTION, paymentOption);
				put(METAFIELD_DURATION, duration);
			}};
	}

	protected void setUpOrderStatus(Integer userId, TestEnvironmentBuilder envBuilder) {

		orderUploadStatusId = buildAndPersistOrderStatus(envBuilder, OrderStatusFlag.INVOICE, "UPLOADED"+System.currentTimeMillis(), userId);
		orderFailedStatusId = buildAndPersistOrderStatus(envBuilder, OrderStatusFlag.INVOICE, "FAILED", userId);
		UPLOADED_ORDER_STATUS_ID = Integer.toString(orderUploadStatusId);
		FAILED_ORDER_STATUS_ID = Integer.toString(orderFailedStatusId);
	}


	protected Integer buildAndPersistOrderStatus(TestEnvironmentBuilder envBuilder, OrderStatusFlag orderStatusFlag, String description, Integer userId) {

		OrderStatusWS orderStatus = new OrderStatusWS();
		CompanyWS company = envBuilder.getPrancingPonyApi().getCompany();

		orderStatus.setOrderStatusFlag(orderStatusFlag);
		orderStatus.setDescription(description);
		orderStatus.setEntity(company);
		orderStatus.setUserId(userId);

		return envBuilder.getPrancingPonyApi().createUpdateOrderStatus(orderStatus);

	}

	protected OrderWS buildOrder (TestEnvironmentBuilder envBuilder, Integer userId, List<OrderLineWS> orderLines, Integer periodId) {

		return envBuilder.orderBuilder(envBuilder.getPrancingPonyApi())
			.forUser(userId)
			.withOrderLines(orderLines).withPeriod(periodId).buildOrder();

	}

	protected OrderLineWS buildOrderLines (TestEnvironmentBuilder envBuilder, Integer itemId, String description, BigDecimal quantity, BigDecimal amount) {

		return  envBuilder.orderBuilder(envBuilder.getPrancingPonyApi()).orderLine()
			.withAmount(amount).withQuantity(quantity).withDescription(description)
			.withItemId(itemId).build();

	}

	protected Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder,  String code,
																					 boolean global, Integer categoryId, PriceModelWS priceModelWS, boolean allowDecimal,
																					 String description) {

		SortedMap<Date, PriceModelWS> prices = new TreeMap<>();
		prices.put(new Date(), priceModelWS);
		ItemDTOEx item = new ItemDTOEx();
		item.setGlCode(code);
		item.setDefaultPrices(prices);
		item.setGlobal(global);
		item.setTypes(new Integer[] {categoryId});
		item.setHasDecimals(allowDecimal ? 1: 0);
		item.setDescription(description);
		item.setNumber(code);

		return  envBuilder.getPrancingPonyApi().createItem(item);

	}

	protected Integer buildAndPersistEnumeration (TestEnvironmentBuilder envBuilder, List<EnumerationValueWS> values, String name) {

		EnumerationWS enUmeration = new EnumerationWS();

		enUmeration.setValues(values);
		enUmeration.setName(name);
		enUmeration.setEntityId(envBuilder.getPrancingPonyApi().getCallerCompanyId());

		Integer enumId =  envBuilder.getPrancingPonyApi().createUpdateEnumeration(enUmeration);
		envBuilder.env().add(name, enumId, name,  envBuilder.getPrancingPonyApi(), TestEntityType.ENUMERATION);
		return enumId;

	}


	protected Integer buildAndPersistReservedPlan(TestEnvironmentBuilder envBuilder, String code, Integer itemId, MetaFieldValueWS[] planMetafields, Integer[] usagePoolsId, String description, PriceModelWS price, Integer planTypeId, BigDecimal cost) {

		PlanWS plan = CreateObjectUtil.createPlan(envBuilder.getPrancingPonyApi().getCallerCompanyId(), BigDecimal.valueOf(2200), 1, planTypeId, 2, BigDecimal.ZERO, envBuilder.getPrancingPonyApi());


		ItemDTOEx planItem = new ItemDTOEx();
		planItem.setNumber(String.valueOf(new Date().getTime()));
		planItem.setEntityId(envBuilder.getPrancingPonyApi().getCallerCompanyId());
		planItem.setDescription(description);
		//planItem.setDefaultPrice(price);
		planItem.setCurrencyId(1);
		planItem.setTypes(new Integer[]{planTypeId});
		planItem.setPrice(cost);

		Integer planItemId = envBuilder.getPrancingPonyApi().createItem(planItem);

		plan.setDescription(description);


		PlanItemWS pi1 = new PlanItemWS();
		pi1.setItemId(itemId);
		pi1.setPrecedence(-1);
		pi1.setModel(price);

		plan.setPlanItems(new ArrayList<PlanItemWS>() {{ add(pi1);}});

		plan.setMetaFields(planMetafields);
		plan.getMetaFieldsMap().put(envBuilder.getPrancingPonyApi().getCallerCompanyId(), planMetafields);
		plan.setDescription(description);
		plan.setUsagePoolIds(usagePoolsId);
		plan.setItemId(planItemId);
		plan.setPeriodId(2);

		Integer planId = envBuilder.getPrancingPonyApi().createPlan(plan);
		envBuilder.env().add(code, planId, plan.getDescription(),  envBuilder.getPrancingPonyApi(), TestEntityType.PLAN);
		return planId;

	}



	protected Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
		return envBuilder.itemBuilder(api)
			.itemType()
			.withCode(code)
			.withCategoryType(categoryType)
			.global(global)
			.build();
	}

	protected Integer buildAndPersistMetafield(TestEnvironmentBuilder envBuilder, String name, DataType dataType, EntityType entityType) {
		MetaFieldWS value =  new MetaFieldBuilder()
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

	protected Integer buildAndPersistUser(TestEnvironmentBuilder envBuilder, String userName, Map<String, Object> metaFieldValues) {



		CustomerBuilder builder = envBuilder.customerBuilder(envBuilder.getPrancingPonyApi()).addTimeToUsername(false).withUsername(userName);
		metaFieldValues.entrySet().stream().forEach(e -> builder.withMetaField(e.getKey(), e.getValue()));

		UserWS user = builder.build();

		user.setMetaFields(builder.buildMetaField());

		envBuilder.getPrancingPonyApi().updateUser(user);

		return  user.getId();

	}

	protected Integer createFreeUsagePool(TestEnvironmentBuilder envBuilder, String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue, Integer itemId) {
		UsagePoolWS usagePool = populateFreeUsagePoolObject(usagePoolName, quantity, cyclePeriodUnit, resetValue, envBuilder.getPrancingPonyApi().getCallerCompanyId(),itemId);
		Integer poolId = envBuilder.getPrancingPonyApi().createUsagePool(usagePool);
		Assert.assertNotNull("Free usage pool should not be null ", poolId);
		return poolId;
	}

	private UsagePoolWS populateFreeUsagePoolObject(String usagePoolName, String quantity, String cyclePeriodUnit, String resetValue, Integer entityId, Integer itemId) {

		UsagePoolWS usagePool = new UsagePoolWS();
		usagePool.setName(usagePoolName );
		usagePool.setQuantity(quantity);
		usagePool.setPrecedence(new Integer(1));
		usagePool.setCyclePeriodUnit(cyclePeriodUnit);
		usagePool.setCyclePeriodValue(new Integer(1));

		usagePool.setItems(new Integer[]{itemId});
		usagePool.setEntityId(entityId);
		usagePool.setUsagePoolResetValue(resetValue);

		return usagePool;
	}

	protected Integer buildAndPersistMeteredPlugin(TestEnvironmentBuilder envBuilder, String code, int pluginTypeId) {

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

	protected PriceModelWS buildFlatPriceModel(String flatPrice, Integer currencyId ){
		return new PriceModelWS(PriceModelStrategy.FLAT.name(),
			new BigDecimal(flatPrice), currencyId);


	}

	protected PriceModelWS buildTieredPriceModel(Map<String, String> tieredAttributes, Integer currencyId ){
		PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.TIERED.name(), null, currencyId, "$");
		priceModelWS.setAttributes(tieredAttributes);
		return priceModelWS;
	}

	protected void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch(InterruptedException ex) {

		}
	}


}
