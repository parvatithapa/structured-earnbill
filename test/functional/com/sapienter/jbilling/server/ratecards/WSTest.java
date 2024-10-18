package com.sapienter.jbilling.server.ratecards;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.*;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.NameValueString;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import static org.testng.AssertJUnit.*;
import static org.testng.AssertJUnit.assertEquals;

import com.sapienter.jbilling.server.util.db.StringList;
import com.sapienter.jbilling.server.util.hibernate.StringListTypeDescriptor;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Test(groups = { "web-services", "route-rate-cards" }, testName = "ratecards.WSTest")
public class WSTest {

    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);
	private static final Integer SYSTEM_CURRENCY_ID = 1;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	
	private static Integer TEST_USER_ID= null;
    private static Integer TIME_RATING_UNIT = 1;
	private static Integer PRODUCT_ROUTE_RATE_CARD_ID= Integer.valueOf(1);
	private static Integer PLAN_ROUTE_RATE_CARD_ID= Integer.valueOf(2);
	private static Integer ACCOUNT_ROUTE_RATE_CARD_ID= Integer.valueOf(3);
	private static Integer CUSTOMER_ROUTE_RATE_CARD_ID= Integer.valueOf(4);
	private static Integer RATE_CALL_PRODUCT_ID= null;

	private static Integer PP_ACCOUNT_TYPE = 1;
	private static Integer PP_MONTHLY_PERIOD;

    private static StringBuilder CSV_WITH_NYPHEN = new StringBuilder();
    // Api
    private static JbillingAPI api;

	@BeforeClass
	public void getAPI() throws Exception {
		api = JbillingAPIFactory.getAPI();

		PP_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
        AccountTypeWS[] accountTypes= api.getAllAccountTypes();
        if (null == accountTypes || accountTypes.length == 0) {
            AccountTypeWS accountTypeWS = createAccountType();
            Integer newAccountTypeId = api.createAccountType(accountTypeWS);
            logger.debug("New Account Type ID {}", newAccountTypeId);
            assertEquals(newAccountTypeId, api.getAccountType(newAccountTypeId).getId());
            PP_ACCOUNT_TYPE= newAccountTypeId;
        }

        CSV_WITH_NYPHEN.append("id,name,surcharge,initial_increment,subsequent_increment,charge,free_destination,route_id,active_date,a-number\n")
                       .append("1,F1,0,30,6,1,6132991908,CANON,2011/01/01-2012/01/01,\n")
                       .append("2,F2,0,60,6,1,6132661717,CANOM,1/1/2011,\n")
                       .append("3,F3,0,45,6,1,8192234512,NAARG,1/1/2011,\n")
                       .append("4,F4,0,60,60,1,8192234555,PRARG,1/1/2011,\n")
                       .append("5,F6,0,60,66,1,6132991888,CANON,1/1/2012,\n");
	}

	@Test
	public void test001RateCardHiearachy() {
		logger.debug("Test Hierarchical Route Based Rate Card");

		try {

			ItemTypeWS itemType= new ItemTypeWS();
			itemType.setDescription("Rate Card Items_"+ System.currentTimeMillis());
			itemType.setOrderLineTypeId(1);
			itemType.setAllowAssetManagement(0);
			Integer itemTypeId= api.createItemCategory(itemType);
			logger.debug("Created Item Type {}", itemTypeId);

            assertNotNull(api.getAccountType(PP_ACCOUNT_TYPE));

			UserWS newUser= PricingTestHelper.buildUser("test-route-cards-1", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
			TEST_USER_ID= api.createUser(newUser);
			assertNotNull(TEST_USER_ID);
			logger.debug("Created User ID {}", TEST_USER_ID) ;

			ItemDTOEx rateCardItem= PricingTestHelper.buildItem("RCITEM101", "Test Rate Card Item", itemTypeId);

			PriceModelWS routeRate = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
            SortedMap<String, String> attributes = new TreeMap<String, String>();
            attributes.put("route_rate_card_id", String.valueOf(PRODUCT_ROUTE_RATE_CARD_ID));
            attributes.put("1", "route_id");
            routeRate.setAttributes(attributes);
            routeRate.setCurrencyId(1);
            rateCardItem.addDefaultPrice(CommonConstants.EPOCH_DATE, routeRate);

			RATE_CALL_PRODUCT_ID= api.createItem(rateCardItem);
			assertNotNull(RATE_CALL_PRODUCT_ID);
			logger.debug("Create item {}", RATE_CALL_PRODUCT_ID);

            //add to plan
            ItemDTOEx planItem= PricingTestHelper.buildItem("ROUTE PLAN", "Route Rate Card Plan", 1);
            Integer PLAN_ITEM_ID= api.createItem(planItem);

            PriceModelWS routeRateCard = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
            routeRateCard.addAttribute("route_rate_card_id", String.valueOf(PLAN_ROUTE_RATE_CARD_ID));
            routeRateCard.addAttribute("1", "route_id");
            routeRateCard.setCurrencyId(1);

            PlanItemWS rateCardProd = new PlanItemWS();
            rateCardProd.setItemId(RATE_CALL_PRODUCT_ID);
            rateCardProd.getModels().put(CommonConstants.EPOCH_DATE, routeRateCard);

            PlanWS plan = new PlanWS();
            plan.setItemId(PLAN_ITEM_ID);
            plan.setDescription("Rate Card calls.");
            plan.setPeriodId(2);//
            plan.addPlanItem(rateCardProd);

            plan.setId(api.createPlan(plan)); // create plan
            logger.debug("Created Plan with rate card , id {}", plan.getId());

            //add account type price
            routeRateCard = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
            routeRateCard.addAttribute("route_rate_card_id", String.valueOf(ACCOUNT_ROUTE_RATE_CARD_ID));
            routeRateCard.addAttribute("1", "route_id");
            routeRateCard.setCurrencyId(1);

            rateCardProd = new PlanItemWS();
            rateCardProd.setItemId(RATE_CALL_PRODUCT_ID);
            rateCardProd.getModels().put(CommonConstants.EPOCH_DATE, routeRateCard);

            rateCardProd= api.createAccountTypePrice(1, rateCardProd, null);
            assertNotNull (rateCardProd);
            logger.debug("Created Account Type Price {}", rateCardProd.getId());

            //add customer price
            routeRateCard = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
            routeRateCard.addAttribute("route_rate_card_id", String.valueOf(CUSTOMER_ROUTE_RATE_CARD_ID));
            routeRateCard.addAttribute("1", "route_id");
            routeRateCard.setCurrencyId(1);

            rateCardProd = new PlanItemWS();
            rateCardProd.setItemId(RATE_CALL_PRODUCT_ID);
            rateCardProd.getModels().put(CommonConstants.EPOCH_DATE, routeRateCard);

            rateCardProd= api.createCustomerPrice(TEST_USER_ID, rateCardProd, null);
            assertNotNull (rateCardProd);
			logger.debug("Created Customer Price {}", rateCardProd.getId());

			//Best Match, destination 8192234500
	        PricingField[] BEST_MATCH_FIELDS = {
	            new PricingField("event_date", new Date()),
	            new PricingField("duration", 60),
	            new PricingField("destination", "8192234500"),
	        };

	        OrderWS order = PricingTestHelper.buildMonthlyOrder(TEST_USER_ID, PP_MONTHLY_PERIOD);
	        order.setPricingFields(PricingField.setPricingFieldsValue(BEST_MATCH_FIELDS));

	        OrderLineWS line = PricingTestHelper.buildOrderLine(RATE_CALL_PRODUCT_ID, 1);
	        order.setOrderLines(new OrderLineWS[] { line });

	        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	        assertEquals(order.getOrderLines().length, 1);

	        /*
	        Customer Rate Card:
	        id,name,route_id,start_date,end_date,surcharge,initial_increment,subs_increment,charge,destination
			1,F1,CANON,1/1/2011,1/1/2012,0,30,6,0.1,6132991
			2,F2,CANOM,1/1/2011,,0,30,7,0.2,61326617
			3,F3,NAARG,1/1/2011,,0,30,8,0.3,8192234544
			4,F4,PRARG,1/1/2011,,0,30,9,0.4,81922345
			5,F6,CANON,1/1/2012,,0,30,10,0.5,613299

			Best Match 8192234500 should resolve Rate Card F4, Route PRARG
			For duration 60: First 30 seconds = 0.4/2 = 0.2
			Remaining 30 seconds (4 units of 9 in 30): = (0.4/60)*9*4 =0.24
	        Total = 0.24 + 0.2 = 0.44

	        */

	        logger.debug("Order Amount: {}", order.getOrderLines()[0].getAmountAsDecimal());
	        logger.debug("Rate : {}", order.getOrderLines()[0].getPrice());

	        assertBigDecimalEquals("", new BigDecimal("0.20"), order.getOrderLines()[0].getAmountAsDecimal());

	        //Best Match, destination 8192234544
	        PricingField[] BEST_MATCH_FIELDS2 = {
	            new PricingField("event_date", new Date()),
	            new PricingField("duration", 67),
	            new PricingField("destination", "8192234544"),
	        };

	        logger.debug("New Order");
	        order = PricingTestHelper.buildMonthlyOrder(TEST_USER_ID, PP_MONTHLY_PERIOD);
	        order.setPricingFields(PricingField.setPricingFieldsValue(BEST_MATCH_FIELDS2));//almost exact match

	        line = PricingTestHelper.buildOrderLine(RATE_CALL_PRODUCT_ID, 1);
	        order.setOrderLines(new OrderLineWS[] { line });

	        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	        assertEquals(order.getOrderLines().length, 1);


	        /*
	         *  Best Match 8192234544 should resolve Rate Card F3, Route NAARG
				For duration 67: First 30 seconds = 0.3/2 = 0.15
				Remaining 37 seconds (5 units of 8 in 37): = (0.3/60)*8*5 =0.20
		        Total = 0.15 + 0.20 = 0.35

	        */

	        logger.debug("Order Amount: {}", order.getOrderLines()[0].getAmountAsDecimal());
	        logger.debug("Rate : {}", order.getOrderLines()[0].getPrice());
	        assertBigDecimalEquals("", new BigDecimal("0.15"), order.getOrderLines()[0].getAmountAsDecimal());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}

    @Test
	public void test002RateCardFallBackToRouteIDForPricing() {
		logger.debug("Test Fall back to RouteID. Use Route ID to resolve the rate.");

		try {

			/**
			 * As per the current design, with presence of one or more matching fields in the configuration,
			 * the matching fields are give higher priority to resolve a rate card. IF a price is not resolved using
			 * matching fields, then no price is returned.
			 *
			 * In absence of any Rate Card matching fields, Route is used to resolve the price directly.
			 */
			logger.debug("Deleting configured matching fields for the Product 's configured rate cards.{}", RATE_CALL_PRODUCT_ID);
			for (int i=10 ; i < 14 ; i++ ) {
				api.deleteMatchingField(i);
			}

			logger.debug("The rate cards have no matching field now, will use an existing route for testing");
/*
			 id |            name            | routeid | dialed | source | countrycode
			 ----+----------------------------+---------+--------+--------+-------------
			   1 | Canada Ontario             | CANON   | 613    | 613    | ca
			   2 | Canada Ontario             | CANON   | 613    | 819    | ca
			   3 | Canada Ottawa Montreal     | CANOM   | 613    | 514    | ca
			   4 | Argentina                  | ARG     | 54     | 22     |
			   5 | North America Argentina    | NAARG   | 54     | 1      |
			   6 | Puerto Rico Argentina      | PRARG   | 54     | 1      | pr
			   7 | North America Buenos Aires | NAARBA  | 5411   | 1      |

			   non-uniqe: "2","Canada Ontario","CANON","613","819","ca"

*/

            /*
	         * Unique match from route: 1,Canada Ontario,CANON,613,613,ca
	         * Unique match from route rate card considering event date: 5,F6,CANON,1/1/2012,,0,30,10,0.5,613299
	         */
			logger.debug("New Order");
            logger.debug("Test active(event) date filter");

			PricingField[] ROUTE_MATCHING_FIELDS = {
		            new PricingField("event_date", new Date()),
		            new PricingField("duration", 67),
		            new PricingField("dialed", "613"),
		            new PricingField("source", "613"),
		        };


	        OrderWS order = PricingTestHelper.buildMonthlyOrder(TEST_USER_ID, PP_MONTHLY_PERIOD);
	        order.setPricingFields(PricingField.setPricingFieldsValue(ROUTE_MATCHING_FIELDS));//almost exact match

	        OrderLineWS line = PricingTestHelper.buildOrderLine(RATE_CALL_PRODUCT_ID, 1);
	        order.setOrderLines(new OrderLineWS[] { line });

            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertEquals(order.getOrderLines().length, 1);

            logger.debug("Order Amount: {}", order.getOrderLines()[0].getAmountAsDecimal());
            logger.debug("Rate : {}", order.getOrderLines()[0].getPrice());

            assertBigDecimalEquals("", new BigDecimal("0.00"), order.getOrderLines()[0].getAmountAsDecimal());

            /*
	         * Unique match from route: 1,Canada Ontario,CANON,613,613,ca
	         * Non- Unique match from route rate card excluding the event date:
	         * 1,F1,CANON,1/1/2011,1/1/2012,0,30,6,0.1,6132991
	         * 5,F6,CANON,1/1/2012,,0,30,10,0.5,613299
	         */
            logger.debug("Test non-unique for failure.");
            logger.debug("New Order");

            PricingField[] NON_UNIQUE_ROUTE_MATCHING_FIELDS = {
                    new PricingField("duration", 67),
                    new PricingField("dialed", "613"),
                    new PricingField("source", "613"),
            };


            order = PricingTestHelper.buildMonthlyOrder(TEST_USER_ID, PP_MONTHLY_PERIOD);
            order.setPricingFields(PricingField.setPricingFieldsValue(NON_UNIQUE_ROUTE_MATCHING_FIELDS));// missing event date

            line = PricingTestHelper.buildOrderLine(RATE_CALL_PRODUCT_ID, 1);
            order.setOrderLines(new OrderLineWS[] { line });
            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertBigDecimalEquals("", new BigDecimal("0"), order.getOrderLines()[0].getAmountAsDecimal());

	        /*
	         * Unique match: "3","Canada Ottawa Montreal","CANOM","613","514","ca"
	         *
	         */
	        ROUTE_MATCHING_FIELDS = new PricingField[] {
		            new PricingField("event_date", new Date()),
		            new PricingField("duration", 67),
		            new PricingField("dialed", "613"),
		            new PricingField("source", "514"),
		        };


	        order = PricingTestHelper.buildMonthlyOrder(TEST_USER_ID, PP_MONTHLY_PERIOD);
	        order.setPricingFields(PricingField.setPricingFieldsValue(ROUTE_MATCHING_FIELDS));//almost exact match

	        line = PricingTestHelper.buildOrderLine(RATE_CALL_PRODUCT_ID, 67);
	        order.setOrderLines(new OrderLineWS[] { line });

	        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
	        assertEquals(order.getOrderLines().length, 1);


	        /*
	         *  Match should resolve 2,F2,CANOM,1/1/2011,,0,60,6,0.2,61326617
				For duration 67: First 30 seconds = 0.2/2 = 0.1
				Remaining 37 seconds (6 units of 7 in 37): = (0.2/60)*6*7 =0.14
		        Total = 0.1 + 0.14 = 0.24

	        */

	        logger.debug("Order Amount: {}", order.getOrderLines()[0].getAmountAsDecimal());
	        logger.debug("Rate : {}", order.getOrderLines()[0].getPrice());
	        assertBigDecimalEquals("", new BigDecimal("0.00"), order.getOrderLines()[0].getAmountAsDecimal());
		} catch (Exception e) {
			fail("Exception caught:" + e);
		}
	}

    @Test
    public void test003RatingUnitTest() throws Exception {

        Integer dataRatingUnitId = null;
        Integer testRouteRateCardId = null;
        Integer matchingFieldId = null;
        Integer itemId = null;
        Integer userId = null;

        try {

            // 1. Create new rating unit: Data --> 1MB = 1024KB
            // use the default unit for initial rating: Time --> 1min = 60sec
            RatingUnitWS ratingUnitWS = new RatingUnitWS();
            ratingUnitWS.setName("Data");
            ratingUnitWS.setPriceUnitName("MB");

            ratingUnitWS.setIncrementUnitName("KB");
            ratingUnitWS.setIncrementUnitQuantityAsDecimal(BigDecimal.valueOf(1024));

            dataRatingUnitId = api.createRatingUnit(ratingUnitWS);

            // 2. Create new route rate card csv file
            File routeRateCardFile = File.createTempFile("testRouteRateCard", ".csv");
            writeToFile(routeRateCardFile,
                    "id,name,surcharge,initial_increment,subsequent_increment,charge,route_id\n" +
                            "1,F1,0,0,1,0.5,CANON\n" +
                            "4,F4,0,0,1,0.4,PRARG"
            );

            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName("test_rating_unit_rate_card");
            routeRateCardWS.setRatingUnitId(TIME_RATING_UNIT);
            routeRateCardWS.setTableName("test_unit_rate_card");

            testRouteRateCardId = api.createRouteRateCard(routeRateCardWS, routeRateCardFile);

            // 3. Create matching field for the route rate card
            MatchingFieldWS matchingFieldWS = new MatchingFieldWS();
            matchingFieldWS.setDescription("test Route");
            matchingFieldWS.setMatchingField("route_id");
            matchingFieldWS.setMediationField("route_id");
            matchingFieldWS.setOrderSequence("1");
            matchingFieldWS.setRouteRateCardId(testRouteRateCardId);
            matchingFieldWS.setType(MatchingFieldType.EXACT.name());
            matchingFieldWS.setRequired(true);

            matchingFieldId = api.createMatchingField(matchingFieldWS);

            // 4. Create a product with the configured route rate card
            ItemDTOEx rateCardItem= PricingTestHelper.buildItem("TRURCI", "Test Rating Unit Rate Card Item", 1);

            PriceModelWS routeRate = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
            SortedMap<String, String> attributes = new TreeMap<String, String>();
            attributes.put("route_rate_card_id", String.valueOf(testRouteRateCardId));
            routeRate.setAttributes(attributes);
            rateCardItem.addDefaultPrice(CommonConstants.EPOCH_DATE, routeRate);

            itemId = api.createItem(rateCardItem);
            assertNotNull(itemId);

            // 5. Create test user
            UserWS newUser= PricingTestHelper.buildUser("test-rating-unit-route-cards", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
            userId= api.createUser(newUser);
            assertNotNull(userId);
            logger.debug("Created User ID {}", userId) ;

            // 6. Rate an order with the created product
            PricingField[] TEST_UNIT_MATCH_FIELDS = {
                    new PricingField("duration", 20),
                    new PricingField("route_id", "CANON"),
            };

            OrderWS order = PricingTestHelper.buildMonthlyOrder(userId, PP_MONTHLY_PERIOD);
            order.setPricingFields(PricingField.setPricingFieldsValue(TEST_UNIT_MATCH_FIELDS));

            OrderLineWS line = PricingTestHelper.buildOrderLine(itemId, 1);
            order.setOrderLines(new OrderLineWS[] { line });

            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertEquals(order.getOrderLines().length, 1);

            /**
             * Rating Unit: 1min = 60 sec
             *
             * Pricing: min --> 0.5 $
             * Incremental: sec --> 20 quantity(sec)
             *
             * Final Price: 0.5/60 * 20 = 0.16(6)
             */

            logger.debug("Order Amount: {}", order.getOrderLines()[0].getAmountAsDecimal());
            logger.debug("Rate : {}", order.getOrderLines()[0].getPrice());
            assertBigDecimalEquals("", new BigDecimal("0.01"), order.getOrderLines()[0].getAmountAsDecimal());

            // 7. Change the rating unit of the route rate card
            routeRateCardWS = api.getRouteRateCard(testRouteRateCardId);
            routeRateCardWS.setRatingUnitId(dataRatingUnitId);
            api.updateRouteRateCard(routeRateCardWS, routeRateCardFile);

            // check if the id has changes; IT SHOULD NOT!
            routeRateCardWS = api.getRouteRateCard(testRouteRateCardId);
            assertNotNull(routeRateCardWS);

            // 8. Rate another order with the same product and note the difference
            order = PricingTestHelper.buildMonthlyOrder(userId, PP_MONTHLY_PERIOD);
            order.setPricingFields(PricingField.setPricingFieldsValue(TEST_UNIT_MATCH_FIELDS));

            line = PricingTestHelper.buildOrderLine(itemId, 1);
            order.setOrderLines(new OrderLineWS[] { line });

            order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
            assertEquals(order.getOrderLines().length, 1);

            /**
             * Rating Unit: 1MB = 1024 KB
             *
             * Pricing: MB --> 0.5 $
             * Incremental: KB --> 20 quantity(KB)
             *
             * Final Price: 0.5/1024 * 20 = 0.009765625
             */

            logger.debug("Order Amount: {}", order.getOrderLines()[0].getAmountAsDecimal());
            logger.debug("Rate : {}", order.getOrderLines()[0].getPrice());
            assertBigDecimalEquals("", new BigDecimal("0.00"), order.getOrderLines()[0].getAmountAsDecimal());

        } catch (Exception e) {
            fail("Exception caught:" + e);
        } finally {
            // delete all created items in this test;
            if (matchingFieldId != null) {
                api.deleteMatchingField(matchingFieldId);
            }
            if (testRouteRateCardId != null) {
                api.deleteRouteRateCard(testRouteRateCardId);
            }
            if (dataRatingUnitId != null) {
                api.deleteRatingUnit(dataRatingUnitId);
            }
            if (itemId != null) {
                api.deleteItem(itemId);
            }
            if (userId != null) {
                api.deleteUser(userId);
            }
        }

    }

    @Test
    public void test004RouteManagementTest() throws Exception {
        Integer testRouteId = null;
        Integer matchingFieldId = null;

        try {

            // 2. Create new route rate card csv file
            File routeFile = File.createTempFile("testRoute", ".csv");
            writeToFile(routeFile,
                    "id,name,routeid,dialed\n" +
                            "1,Canada Ontario,CANON,613\n" +
                            "5,North America Argentina,NAARG,54"
            );

            RouteWS routeWS = new RouteWS();
            routeWS.setName("test_route");
            routeWS.setTableName("test_route");

            testRouteId = api.createRoute(routeWS, routeFile);

            // 3. Create matching field for the route rate card
            MatchingFieldWS matchingFieldWS = new MatchingFieldWS();
            matchingFieldWS.setDescription("Termination");
            matchingFieldWS.setMatchingField("dialed");
            matchingFieldWS.setMediationField("dialed");
            matchingFieldWS.setOrderSequence("1");
            matchingFieldWS.setRouteId(testRouteId);
            matchingFieldWS.setType(MatchingFieldType.BEST_MATCH.name());
            matchingFieldWS.setRequired(true);

            matchingFieldId = api.createMatchingField(matchingFieldWS);

            routeWS = api.getRoute(testRouteId);
            routeWS.setName("test_route_id");
            api.createRoute(routeWS, routeFile);

            routeWS = api.getRoute(testRouteId);
            assertNotNull("Route with Id should exists: " + testRouteId, routeWS);

        } catch (Exception e) {

        } finally {
            // delete all created items in this test;
            if (matchingFieldId != null) {
                api.deleteMatchingField(matchingFieldId);
            }
            if (testRouteId != null) {
                api.deleteRoute(testRouteId);
            }
        }
    }

    @Test
    public void test005RouteTableCrud() throws Exception {

        RouteRecordWS ws = new RouteRecordWS();
        List<NameValueString> nv = new ArrayList<NameValueString>(6);
        try {
            api.createRouteRecord(ws, 2);
            fail("Should have exception");
        } catch (SessionInternalError e) {

        }

        ws.setName("CRUD Test.name");
        ws.setRouteId("CRUD Test.routeid");
        nv = new ArrayList<NameValueString>(6);
        nv.add(new NameValueString("dialed", "CRUD Test.dialed"));
        nv.add(new NameValueString("source", "CRUD Test.source"));
        nv.add(new NameValueString("countrycode", "CRUD Test.countrycode"));
        ws.setAttributes(nv.toArray(new NameValueString[nv.size()]));
        int idInt = api.createRouteRecord(ws, 2);

        String content = api.getRouteTable(2);
        String[] lines = content.split("[\n]");
        List<String> headers = Arrays.asList(lines[0].replaceAll("\"","").split(","));

        String id = null;
        for(String line: lines) {
            if(line.contains("CRUD")) {
                List<String> cols = Arrays.asList(line.replaceAll("\"","").split(","));
                id = cols.get(headers.indexOf("id"));
                assertEquals(idInt, Integer.parseInt(id));
                assertEquals("CRUD Test.name", cols.get(headers.indexOf("name")));
                assertEquals("CRUD Test.routeid", cols.get(headers.indexOf("routeid")));
                assertEquals("CRUD Test.dialed", cols.get(headers.indexOf("dialed")));
                assertEquals("CRUD Test.source", cols.get(headers.indexOf("source")));
                assertEquals("CRUD Test.countrycode", cols.get(headers.indexOf("countrycode")));
            }
        }
        assertNotNull(id);

        ws.setName("CRUD Test.name2");
        ws.setRouteId("CRUD Test.routeid2");
        ws.setId(new Integer(id));
        nv = new ArrayList<NameValueString>(6);
        nv.add(new NameValueString("dialed", "CRUD Test.dialed2"));
        nv.add(new NameValueString("source", "CRUD Test.source2"));
        nv.add(new NameValueString("countrycode", "CRUD Test.countrycode2"));
        ws.setAttributes(nv.toArray(new NameValueString[nv.size()]));

        api.updateRouteRecord(ws, 2);

        content = api.getRouteTable(2);
        lines = content.split("[\n]");
        headers = Arrays.asList(lines[0].replaceAll("\"","").split(","));

        id = null;
        for(String line: lines) {
            if(line.contains("CRUD")) {
                List<String> cols = Arrays.asList(line.replaceAll("\"","").split(","));
                id = cols.get(headers.indexOf("id"));
                assertEquals(idInt, Integer.parseInt(id));
                assertEquals("CRUD Test.name2", cols.get(headers.indexOf("name")));
                assertEquals("CRUD Test.routeid2", cols.get(headers.indexOf("routeid")));
                assertEquals("CRUD Test.dialed2", cols.get(headers.indexOf("dialed")));
                assertEquals("CRUD Test.source2", cols.get(headers.indexOf("source")));
                assertEquals("CRUD Test.countrycode2", cols.get(headers.indexOf("countrycode")));
            }
        }
        assertNotNull(id);

        api.deleteRouteRecord(2, idInt);

        content = api.getRouteTable(2);
        lines = content.split("[\n]");
        headers = Arrays.asList(lines[0].replaceAll("\"","").split(","));

        id = null;
        for(String line: lines) {
            if(line.contains("CRUD")) {
                fail("line should be deleted");
            }
        }

        ws.setName("CRUD Test.name3");
        ws.setRouteId("CRUD Test.routeid3");
        ws.setId(1001);
        nv = new ArrayList<NameValueString>(6);
        nv.add(new NameValueString("dialed", "CRUD Test.dialed3"));
        nv.add(new NameValueString("source", "CRUD Test.source3"));
        nv.add(new NameValueString("countrycode", "CRUD Test.countrycode3"));
        ws.setAttributes(nv.toArray(new NameValueString[nv.size()]));
        api.createRouteRecord(ws, 2);

        content = api.getRouteTable(2);
        lines = content.split("[\n]");
        headers = Arrays.asList(lines[0].replaceAll("\"","").split(","));

        id = null;
        for(String line: lines) {
            if(line.contains("CRUD")) {
                List<String> cols = Arrays.asList(line.replaceAll("\"","").split(","));
                id = cols.get(headers.indexOf("id"));
                assertEquals(1001, Integer.parseInt(id));
                assertEquals("CRUD Test.name3", cols.get(headers.indexOf("name")));
                assertEquals("CRUD Test.routeid3", cols.get(headers.indexOf("routeid")));
                assertEquals("CRUD Test.dialed3", cols.get(headers.indexOf("dialed")));
                assertEquals("CRUD Test.source3", cols.get(headers.indexOf("source")));
                assertEquals("CRUD Test.countrycode3", cols.get(headers.indexOf("countrycode")));
            }
        }
        assertNotNull(id);
    }

    @Test
    public void test006RouteSearch() throws Exception {
        //one filter search
        SearchCriteria c = new SearchCriteria();
        c.setFilters(new BasicFilter[] {
                new BasicFilter("dialed", Filter.FilterConstraint.EQ, "613")
        });
        c.setMax(10);
        SearchResult<String> r = api.searchDataTable(2, c);
        assertEquals(3, r.getRows().size());
        assertTrue(r.getColumnNames().contains("id"));
        assertTrue(r.getColumnNames().contains("name"));
        assertTrue(r.getColumnNames().contains("routeid"));
        assertTrue(r.getColumnNames().contains("dialed"));
        assertTrue(r.getColumnNames().contains("source"));
        assertTrue(r.getColumnNames().contains("countrycode"));

        Set<String> ids = new HashSet<String>();
        int idIdx = r.getColumnNames().indexOf("id");
        int dialedIdx = r.getColumnNames().indexOf("dialed");
        for(List<String> row : r.getRows()) {
            assertFalse(ids.contains(row.get(idIdx)));
            ids.add(row.get(idIdx));
            assertEquals("613", row.get(dialedIdx));
        }

        //two filter search
        c = new SearchCriteria();
        c.setMax(10);
        c.setFilters(new BasicFilter[] {
                new BasicFilter("dialed", Filter.FilterConstraint.EQ, "613"),
                new BasicFilter("source", Filter.FilterConstraint.EQ, "819")
        });
        r = api.searchDataTable(2, c);
        assertEquals(1, r.getRows().size());
        idIdx = r.getColumnNames().indexOf("id");
        assertEquals("2", r.getRows().get(0).get(idIdx));

        //in search
        c = new SearchCriteria();
        c.setFilters(new BasicFilter[] {
                new BasicFilter("source", Filter.FilterConstraint.IN, Arrays.asList("613", "819"))
        });
        c.setMax(10);
        r = api.searchDataTable(2, c);
        assertEquals(2, r.getRows().size());
        idIdx = r.getColumnNames().indexOf("id");

        Set<String> idSet = new HashSet<String>(Arrays.asList("1", "2"));
        for(List<String> row : r.getRows()) {
            String id = row.get(idIdx);

            assertTrue(idSet.contains(id));
        }

        //like search
        c = new SearchCriteria();
        c.setFilters(new BasicFilter[] {
                new BasicFilter("routeid", Filter.FilterConstraint.LIKE,  "CAN")
        });
        c.setMax(10);
        r = api.searchDataTable(2, c);
        assertEquals(3, r.getRows().size());
        idIdx = r.getColumnNames().indexOf("id");

        idSet = new HashSet<String>(Arrays.asList("1", "2", "3"));
        for(List<String> row : r.getRows()) {
            String id = row.get(idIdx);

            assertTrue(idSet.contains(id));
        }
    }


    @Test
    public void test007DataTableQueryCrud() throws Exception {
        DataTableQueryWS query = new DataTableQueryWS();
        query.setName("Q1");
        query.setRouteId(1);
        query.setGlobal(0);

        DataTableQueryEntryWS entry1 = new DataTableQueryEntryWS();
        StringList sl = new StringList();
        sl.setValue(Arrays.asList("source,dialed"));
        entry1.setColumns(sl);
        entry1.setRouteId(1);

        query.setRootEntry(entry1);

        query.setId( api.createDataTableQuery(query) );

        DataTableQueryWS q2 = api.getDataTableQuery(query.getId());
        assertEquals(query.getName(), q2.getName());
        assertEquals(query.getRouteId(), q2.getRouteId());
        assertEquals(q2.getUserId(), 1);
        DataTableQueryEntryWS e1 = q2.getRootEntry();

        compare(entry1, e1);

        api.deleteDataTableQuery(query.getId());

        try {
            q2 = api.getDataTableQuery(query.getId());
            fail("Query does not exist");
        } catch (Throwable t) {}

    }

    @Test
    public void test008DataTableQuerySearch() throws Exception {
        //find the global entry
        DataTableQueryWS[] ws = api.findDataTableQueriesForTable(1);
        assertEquals(1, ws.length);
        assertEquals("Query1", ws[0].getName());

        DataTableQueryWS query = new DataTableQueryWS();
        query.setName("Q1");
        query.setRouteId(1);
        query.setGlobal(0);

        DataTableQueryEntryWS entry1 = new DataTableQueryEntryWS();
        StringList sl = new StringList();
        sl.setValue(Arrays.asList("source,dialed"));
        entry1.setColumns(sl);
        entry1.setRouteId(1);

        query.setRootEntry(entry1);

        query.setId( api.createDataTableQuery(query) );
        ws = api.findDataTableQueriesForTable(1);
        assertEquals(2, ws.length);
        Set<Integer> idsToFind = new HashSet();
        idsToFind.add(1);
        idsToFind.add(query.getId());

        for(DataTableQueryWS q : ws) {
            idsToFind.remove(q.getId());
        }

        if(!idsToFind.isEmpty()) {
            fail("Ids not found "+idsToFind);
        }
        assertEquals("Query1", ws[0].getName());
    }

    private void compare(DataTableQueryEntryWS e1, DataTableQueryEntryWS e2) {
        assertEquals(e1.getRouteId(), e2.getRouteId());
        assertEquals(StringListTypeDescriptor.convertToString(e1.getColumns()), StringListTypeDescriptor.convertToString(e2.getColumns()));
        if(e1.getNextQuery() != null) {
            compare(e1.getNextQuery(), e2.getNextQuery());
        }
    }

    @Test
    public void test009NonRouteDataTable() throws Exception {
        Integer testRouteId = null;

        try {
            // 1. Create new data table csv file
            File routeFile = File.createTempFile("nonRoute", ".csv");
            writeToFile(routeFile,
                    "name,abbr\n" +
                            "Canada,CA\n" +
                            "United States,US"
            );

            RouteWS routeWS = new RouteWS();
            routeWS.setName("test_non_route");
            routeWS.setTableName("test_non_route");
            routeWS.setRouteTable(false);

            testRouteId = api.createRoute(routeWS, routeFile);
            assertNotNull(testRouteId);

            routeWS = api.getRoute(testRouteId);
            routeWS.setName("test_non_route_2");
            api.createRoute(routeWS, routeFile);

            routeWS = api.getRoute(testRouteId);
            assertNotNull("Route with Id should exists: " + testRouteId, routeWS);

            //2. Add new record
            RouteRecordWS routeRecordWS = new RouteRecordWS();
            ArrayList<NameValueString> nv = new ArrayList<NameValueString>(6);
            nv.add(new NameValueString("name", "Mexico"));
            nv.add(new NameValueString("abbr", "MX"));
            routeRecordWS.setAttributes(nv.toArray(new NameValueString[nv.size()]));
            Integer recordId = api.createRouteRecord(routeRecordWS, testRouteId);
            assertNotNull(recordId);

            //check that the record was added
            String content = api.getRouteTable(testRouteId);
            String[] lines = content.split("[\n]");
            List<String> headers = Arrays.asList(lines[0].replaceAll("\"","").split(","));

            String id = null;
            for(String line: lines) {
                if(line.contains("Mexico")) {
                    List<String> cols = Arrays.asList(line.replaceAll("\"","").split(","));
                    id = cols.get(headers.indexOf("id"));
                    assertEquals(recordId.intValue(), Integer.parseInt(id));
                    assertEquals("Mexico", cols.get(headers.indexOf("name")));
                    assertEquals("MX", cols.get(headers.indexOf("abbr")));
                }
            }
            assertNotNull(id);

            //3. Update the record
            routeRecordWS.setId(recordId);
            nv = new ArrayList<NameValueString>(6);
            nv.add(new NameValueString("name", "Mexico"));
            nv.add(new NameValueString("abbr", "ME"));
            routeRecordWS.setAttributes(nv.toArray(new NameValueString[nv.size()]));
            api.updateRouteRecord(routeRecordWS, testRouteId);

            //check that the record was updated
            content = api.getRouteTable(testRouteId);
            lines = content.split("[\n]");
            headers = Arrays.asList(lines[0].replaceAll("\"","").split(","));

            id = null;
            for(String line: lines) {
                if(line.contains("Mexico")) {
                    List<String> cols = Arrays.asList(line.replaceAll("\"","").split(","));
                    id = cols.get(headers.indexOf("id"));
                    assertEquals(recordId.intValue(), Integer.parseInt(id));
                    assertEquals("Mexico", cols.get(headers.indexOf("name")));
                    assertEquals("ME", cols.get(headers.indexOf("abbr")));
                }
            }
            assertNotNull(id);
        } catch (Exception e) {

        } finally {
            // delete all created items in this test;

            if (testRouteId != null) {
                api.deleteRoute(testRouteId);
            }
        }
    }

    @Test
    public void test010RouteRateCardWithHyphen() throws IOException {
        Integer routeRateCardId = null;
        try {
            RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
            routeRateCardWS.setName("test_route_rate_card_with_nyphen");
            routeRateCardWS.setRatingUnitId(TIME_RATING_UNIT);

            File routeRateCardFile = File.createTempFile("testRouteRateCard", ".csv");
            writeToFile(routeRateCardFile, CSV_WITH_NYPHEN.toString());

            routeRateCardId = api.createRouteRateCard(routeRateCardWS, routeRateCardFile);

            assertNotNull("Didn't get the route rate card id", routeRateCardId);
        } finally {
            // delete all created items in this test;

            if (routeRateCardId != null) {
                api.deleteRouteRateCard(routeRateCardId);
            }
        }
    }

	private AccountTypeWS createAccountType() {
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setCreditLimit(new BigDecimal(0));
        accountType.setCurrencyId(new Integer(1));
        accountType.setEntityId(1);
        accountType.setInvoiceDeliveryMethodId(1);
        accountType.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        accountType.setMainSubscription(new MainSubscriptionWS(
                Constants.PERIOD_UNIT_DAY, 1));
        accountType.setCreditNotificationLimit1("0");
        accountType.setCreditNotificationLimit2("0");
        accountType.setCreditLimit("0");

        accountType.setName("Account Type_" + System.currentTimeMillis(),
                Constants.LANGUAGE_ENGLISH_ID);
        return accountType;
    }

	private static void assertBigDecimalEquals(String message, BigDecimal expected, BigDecimal actual) {
        assertEquals(message,
                (Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
                (Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP)));
    }

    private void writeToFile(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }
	
}
