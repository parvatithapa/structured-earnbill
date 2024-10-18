package com.sapienter.jbilling.server.MeteredUsageService;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.PreferenceWS;
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

@Test(groups = {"meteredUsageService"}, testName = "TieredModelMeteredUsageTest")
public class TieredModelMeteredUsageTest extends MeteredUsageTestAdapter {

	private Integer pluginId;

	private Integer tieredProductOrderId;
	private static final String TIERED_TO= "To";
	private static final String TIERED_FROM = "From";
	private static final String TIER_PLUS = "plus";

	private static final String PRICE_TIER1 = "550";
	private static final String PRICE_TIER2 = "400";
	private static final String PRICE_TIER3 = "300";

	PreferenceWS tieredPricingPreference;


	Map<String, String> tieredAttributes = new LinkedHashMap<String, String>() {
		{
			put("0", PRICE_TIER1);
			put("100", PRICE_TIER2);
			put("200", PRICE_TIER3);
		}};

	
	@BeforeClass
	public void init() {
		super.init();
		meteredConfigBuilder = getMeteredSetup();
	}

	@AfterClass
	public void tearDown() {

		tieredPricingPreference.setValue("0");

		meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi().updatePreference(tieredPricingPreference);
		super.tearDown();

	}

	private TestBuilder getMeteredSetup() {

		return TestBuilder.newTest().givenForMultiple(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			tieredPricingPreference = api.getPreference(Constants.PREFERENCE_ORDER_LINE_TIER);
			tieredPricingPreference.setValue("1");
			api.updatePreference(tieredPricingPreference);

			buildAndPersistMetafield(envBuilder, METAFIELD_EXTERNAL_ACCOUNT_IDENTIFIER, DataType.STRING, EntityType.CUSTOMER);

			buildAndPersistCategory(envBuilder, api, TEST_CATEGORY_CODE, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);


			Integer userId = buildAndPersistUser(envBuilder, CUSTOMER_CODE, customerMetaFieldValues);


			PriceModelWS tieredPriceModel = buildTieredPriceModel(tieredAttributes, 1);


			Integer tieredProductId = buildAndPersistProduct(envBuilder, TEST_ITEM_CODE, true, envBuilder.idForCode(TEST_CATEGORY_CODE), tieredPriceModel, true, TEST_ITEM_DES);

			List<OrderLineWS> orderLinesForTieredProduct = new ArrayList<OrderLineWS>() {
				{
					add(buildOrderLines(envBuilder, tieredProductId, ORDER_LINE_ITEM_DESCRIPTION, BigDecimal.valueOf(250), ORDER_LINE_ITEM_AMOUNT));
				}};

			OrderWS tieredProductOrder = buildOrder(envBuilder, userId, orderLinesForTieredProduct, MONTHLY_ORDER_PERIOD_ID );

			tieredProductOrder.setIsMediated(true);

			tieredProductOrderId = envBuilder.orderBuilder(api).withCodeForTests(ORDER_CODE).persistOrder(tieredProductOrder);

			setUpOrderStatus(userId,envBuilder);

			pluginId = buildAndPersistMeteredPlugin(envBuilder, SCHEDULED_TASK_PLUGIN_CODE, SCHEDULED_TASK_PLUGIN_TYPE_ID);



		});
	}



	@Test(priority = 1)
	public void testTieredUsageReported()  {

		JbillingAPI parentApi = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi();



		parentApi.triggerScheduledTask(pluginId, new Date());
		sleep(10000);

		int itemCount = 3;

		Map<String, String> TierOneAttribute = getTierAttribute("0", "100");
		Map<String, String> TierTwoAttribute = getTierAttribute("100", "200");
		Map<String, String> TierThreeAttribute = getTierAttribute("200", null);


		String descriptionTierOne = getTieredCustomDescription(TEST_ITEM_DES, TEST_BILLLING_UNIT, TierOneAttribute);
		String descriptionTierTwo = getTieredCustomDescription(TEST_ITEM_DES, TEST_BILLLING_UNIT, TierTwoAttribute);
		String descriptionTierThree = getTieredCustomDescription(TEST_ITEM_DES, TEST_BILLLING_UNIT, TierThreeAttribute);
	
		String customUnitTierOne = getTieredCustomUnit(TEST_ITEM_CODE, TEST_BILLLING_UNIT, TierOneAttribute);
		String customUnitTierTwo = getTieredCustomUnit(TEST_ITEM_CODE, TEST_BILLLING_UNIT, TierTwoAttribute);
		String customUnitTierThree = getTieredCustomUnit(TEST_ITEM_CODE, TEST_BILLLING_UNIT, TierThreeAttribute);


		wireMockServer.verify(postRequestedFor(urlMatching(METERED_API))
				.withRequestBody(matchingJsonPath("$.account[?(@.accountIdentifier == "+"\""+TEST_EXTERNAL_ACCOUNT_IDENTIFIER+"\""+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.quantity == 100)]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.quantity == 100)]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.quantity == 50)]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.price == "+PRICE_TIER1+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.price == "+PRICE_TIER2+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.price == "+PRICE_TIER3+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+descriptionTierOne+"\""+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+descriptionTierTwo+"\""+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.description == "+"\""+descriptionTierThree+"\""+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+customUnitTierOne+"\""+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+customUnitTierTwo+"\""+")]"))
				.withRequestBody(matchingJsonPath("$.items[?(@.customUnit == "+"\""+customUnitTierThree+"\""+")]"))
				.withRequestBody(matchingJsonPath("$[?(@.items.size() == "+itemCount+")]")));


	}


	@Test(priority = 2)
	public void testOrderStatusChangeToUploaded() {
		OrderWS itemOrder = meteredConfigBuilder.getTestEnvironment().getPrancingPonyApi().getOrder(tieredProductOrderId);

		OrderStatusWS orderStatusWS = itemOrder.getOrderStatusWS();

		Assert.assertEquals("Order Status should be changed to Uploaded ", UPLOADED_ORDER_STATUS_ID, Integer.toString(orderStatusWS.getId()));
	}

	private String getTieredCustomDescription(String productDescription, String billingUnit, Map<String, String> attributes) {

		if(attributes.get(TIERED_TO) == null)
			return String.format("%s (%s), %s+ %s", productDescription, billingUnit, attributes.get(TIERED_FROM), billingUnit);
		else
			return  String.format("%s (%s), %s - %s %s", productDescription, billingUnit, attributes.get(TIERED_FROM),attributes.get(TIERED_TO), billingUnit);
	}


	private String getTieredCustomUnit( String productCode, String billingUnit, Map<String, String> attributes) {

		return String.format("%s:%s:%s:%s", productCode, billingUnit,attributes.get(TIERED_FROM),
			attributes.get(TIERED_TO) == null? TIER_PLUS :attributes.get(TIERED_TO));

	}

	private Map<String,String> getTierAttribute(String from, String to) {

		return new HashMap<String, String>() {

			{
				put(TIERED_FROM,from);
				put(TIERED_TO, to);
			}
		};

	}
}


