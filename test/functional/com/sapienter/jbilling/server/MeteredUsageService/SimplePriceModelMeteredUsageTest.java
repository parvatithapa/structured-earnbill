package com.sapienter.jbilling.server.MeteredUsageService;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@Test(groups = {"meteredUsageService"}, testName = "SimplePriceModelMeteredUsageTest")
public class SimplePriceModelMeteredUsageTest extends MeteredUsageTestAdapter {

	private static final String PRODUCT_FLAT_PRICE = "10";
	private Integer pluginId;
	private Integer flatProductOrderId;


	@BeforeClass
	public void init() {
		super.init();
		meteredConfigBuilder = getMeteredSetup();
	}

	@AfterClass
	public void tearDown() {
		super.tearDown();
	}

	private TestBuilder getMeteredSetup() {

		return TestBuilder.newTest().givenForMultiple(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			buildAndPersistMetafield(envBuilder, METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER, DataType.STRING, EntityType.CUSTOMER);

			buildAndPersistCategory(envBuilder, api, TEST_CATEGORY_CODE, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

			PriceModelWS flatPriceModel = buildFlatPriceModel(PRODUCT_FLAT_PRICE, api.getCallerCurrencyId());

			Integer flatProductId = buildAndPersistProduct(envBuilder, TEST_ITEM_CODE, true, envBuilder.idForCode(TEST_CATEGORY_CODE), flatPriceModel, true, TEST_ITEM_DES);

			Integer userId = buildAndPersistUser(envBuilder, CUSTOMER_CODE, customerMetaFieldValues);

			List<OrderLineWS> orderLinesForFlatProduct = new ArrayList<OrderLineWS>() {
				{
					add(buildOrderLines(envBuilder, flatProductId, ORDER_LINE_ITEM_DESCRIPTION, ORDER_LINE_ITEM_QUANTITY, ORDER_LINE_ITEM_AMOUNT));
				}};

			OrderWS flatProductOrder = buildOrder(envBuilder, userId, orderLinesForFlatProduct, MONTHLY_ORDER_PERIOD_ID );

			flatProductOrder.setIsMediated(true);

			flatProductOrderId = envBuilder.orderBuilder(api).withCodeForTests(ORDER_CODE).persistOrder(flatProductOrder);

			setUpOrderStatus(userId,envBuilder);

			pluginId = buildAndPersistMeteredPlugin(envBuilder, SCHEDULED_TASK_PLUGIN_CODE, SCHEDULED_TASK_PLUGIN_TYPE_ID);
		});
	}


	/*

	 */
	@Test(priority = 1)
	public void testSimpleMeteredUsageReported()  {

		JbillingAPI parentApi = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();
		parentApi.triggerScheduledTask(pluginId, new Date());
		sleep(10000);

		String expectedDescription  = getSimpleDescription(TEST_ITEM_DES, TEST_BILLLING_UNIT);
		String expectedCustomUnit = getSimpleCustomUnit(TEST_ITEM_CODE, TEST_BILLLING_UNIT);
		int itemCount = 1;

		wireMockServer.verify(postRequestedFor(urlMatching(METERED_API))
			.withRequestBody(matchingJsonPath("$.account[?(@.accountIdentifier == "+"\""+TEST_EXTERNAL_ACCOUNT_IDENTIFIER+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.quantity == "+ORDER_LINE_ITEM_QUANTITY+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.price == "+PRODUCT_FLAT_PRICE+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+expectedDescription+"\""+")]"))
			.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+expectedCustomUnit+"\""+")]"))
			.withRequestBody(matchingJsonPath("$[?(@.items.size() == "+itemCount+")]")));
	}


	@Test(priority = 2)
	public void testOrderStatusChangeToUploaded() {

		OrderWS itemOrder = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi().getOrder(flatProductOrderId);

		OrderStatusWS orderStatusWS = itemOrder.getOrderStatusWS();

		Assert.assertEquals("Order Status should be changed to Uploaded ", UPLOADED_ORDER_STATUS_ID, Integer.toString(orderStatusWS.getId()));

	}

	private String getSimpleDescription(String productDescription, String billingUnit) {

		return String.format("%s (%s)", productDescription, billingUnit);
	}

	private String getSimpleCustomUnit(String productCode, String billingUnit) {

		return String.format("%s:%s", productCode, billingUnit);
	}
}
