package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.db.LanguageDTO;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.AssertJUnit.*;

/**
 * Quick graduated rate card test.
 *
 * @author Shweta Gupta
 * @since 12-Aug-2013
 */
@Test(groups = { "web-services", "pricing", "rate-card" }, testName = "GraduatedRateCardPricingStrategyWSTest")
public class GraduatedRateCardPricingStrategyWSTest {

    private static final Logger logger = LoggerFactory.getLogger(GraduatedRateCardPricingStrategyWSTest.class);
    JbillingAPI api;
    static final String TEST_GRADUATED_RATE_CARD_ID_1 = "6";
    static final String TEST_GRADUATED_RATE_CARD_ID_2 = "7";
    static Integer USER_ID = null;

	private static Integer PP_ACCOUNT_TYPE = 1;
	private static Integer PP_MONTHLY_PERIOD;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;


    @BeforeTest
    public void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();

	    PP_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);

        // create user to test pricing with
        UserWS user = PricingTestHelper.buildUser("graduated-rate-card", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
        USER_ID = api.createUser(user);
        logger.debug("Created Customer : {}", USER_ID);
        assertNotNull("customer created", user.getUserId());
    }

    @AfterClass
    public void cleanUp() throws Exception {
        api.deleteUser(USER_ID);
    }

    @Test
    public void testGraduatedRateCardUsingRouteRateCard() {
        PriceModelWS routeRateCardPrice = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), new BigDecimal("0.00"), LanguageDTO.ENGLISH_LANGUAGE_ID);
        routeRateCardPrice.addAttribute("cdr_duration_field_name", "duration");
        routeRateCardPrice.addAttribute("route_rate_card_id", TEST_GRADUATED_RATE_CARD_ID_1);

        PriceModelWS graduatedPrice = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), new BigDecimal("0.00"), LanguageDTO.ENGLISH_LANGUAGE_ID);
        graduatedPrice.addAttribute("included", "50");

        routeRateCardPrice.setNext(graduatedPrice);

        ItemDTOEx item = PricingTestHelper.buildItem("TEST-GRADUATED", "Graduated pricing test", 1);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, routeRateCardPrice);

        item.setId(api.createItem(item));
        logger.debug("Created Item : {}", item.getId());
        assertNotNull("item created", item.getId());

        Date eventDate= null;
        try { eventDate= DateTimeFormat.forPattern("MM/dd/yyyy").parseDateTime("01/01/2013").toDate(); } catch (Exception e){}
        
        PricingField[] pf = {
            new PricingField("event_date", eventDate),
            new PricingField("duration", 100),
            new PricingField("free_destination", 456)
        };

        OrderWS order = PricingTestHelper.buildMonthlyOrder(USER_ID, PP_MONTHLY_PERIOD);
        order.setPricingFields(PricingField.setPricingFieldsValue(pf));

        OrderLineWS line = PricingTestHelper.buildOrderLine(item.getId(), 100);
        order.setOrderLines(new OrderLineWS[] { line });

        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        assertThat(order.getOrderLines().length, is(1));

        /*Route resolved : null
        Rate card resolved : name = RC3,
                             start_date = 01/01/2013,
                             initialIncrement = 60,
                             subsequentIncrement = 5,
                             chargePerMinute = 0.80,
                             free_destination = 456
        totalSeconds = 100 (duration)
        Price resolve = 1.3333
        [Price resolving formula :
            ChargeTime = if (totalSeconds > initialIncrement) {
                          return (initialIncrement) + (Math.ceil((totalSeconds - initialIncrement) / subsequentIncrement) * subsequentIncrement);
                        } else {
                          return initialIncrement;
                        }
           Price = chargeTime * (chargePerMinute / 60);
        ]

        Its a Graduated pricing for 50 units included, 100 purchased ... from which 0 existing usage units 100 current usage units and quantity: 100
        Purchased quantity + existing usage exceeds included quantity, applying a partial rate of 0.0066666667
        Price discovered per sec: 0.0066666667 ; total = 100 x 0.0066666667 = 0.6666666700

        */
        logger.debug("Rate : {}", order.getOrderLines()[0].getAmountAsDecimal());
        assertBigDecimalEquals("", new BigDecimal("0.00"), order.getOrderLines()[0].getAmountAsDecimal());

        //clean up
        api.deleteItem(item.getId());
    }

    @Test
    public void testGraduatedRateCardUsingRoute() {
        PriceModelWS routeRateCardPrice = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), new BigDecimal("0.00"), LanguageDTO.ENGLISH_LANGUAGE_ID);
        routeRateCardPrice.addAttribute("cdr_duration_field_name", "duration");
        routeRateCardPrice.addAttribute("route_rate_card_id", TEST_GRADUATED_RATE_CARD_ID_2);
        routeRateCardPrice.addAttribute("1", "route_id");

        PriceModelWS graduatedPrice = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), new BigDecimal("0.00"), LanguageDTO.ENGLISH_LANGUAGE_ID);
        graduatedPrice.addAttribute("included", "100");

        routeRateCardPrice.setNext(graduatedPrice);

        ItemDTOEx item = PricingTestHelper.buildItem("TEST-GRADUATED", "Graduated pricing test", 1);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, routeRateCardPrice);

        item.setId(api.createItem(item));
        logger.debug("Created Item : {}", item.getId());
        assertNotNull("item created", item.getId());

        Date eventDate= null;
        try { eventDate= DateTimeFormat.forPattern("MM/dd/yyyy").parseDateTime("01/01/2013").toDate(); } catch (Exception e){}
        
        PricingField[] pf = {
                new PricingField("event_date", eventDate),
                new PricingField("duration", 100),
                new PricingField("dialed", 613),
                new PricingField("source", 514)
        };

        OrderWS order = PricingTestHelper.buildMonthlyOrder(USER_ID, PP_MONTHLY_PERIOD);
        order.setPricingFields(PricingField.setPricingFieldsValue(pf));

        OrderLineWS line = PricingTestHelper.buildOrderLine(item.getId(), 200);
        order.setOrderLines(new OrderLineWS[] { line });

        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        assertThat(order.getOrderLines().length, is(1));

        /*Route resolved : null
        Rate card resolved : name = RC2,
                             start_date = 01/01/2013,
                             initialIncrement = 50,
                             subsequentIncrement = 7,
                             chargePerMinute = 0.50,
                             dialed = 613.
                             source = 514
        totalSeconds = 100 (duration)
        Price resolve = 0.8833
        [Price resolving formula :
            ChargeTime = if (totalSeconds > initialIncrement) {
                          return (initialIncrement) + (Math.ceil((totalSeconds - initialIncrement) / subsequentIncrement) * subsequentIncrement);
                        } else {
                          return initialIncrement;
                        }
           Price = chargeTime * (chargePerMinute / 60);
        ]

        Its a Graduated pricing for 100 units included, 200 purchased ... from which 0 existing usage units 200 current usage units and quantity: 200
        Purchased quantity + existing usage exceeds included quantity, applying a partial rate of 0.0022083334
        Price discovered per sec: 0.0022083334; total = 200 x 0.0022083334 = 0.4416666667
        */
        logger.debug("Rate : {}", order.getOrderLines()[0].getAmountAsDecimal());
        assertBigDecimalEquals("", new BigDecimal("0.00"), order.getOrderLines()[0].getAmountAsDecimal());

        //clean up
        api.deleteItem(item.getId());
    }

    private static void assertBigDecimalEquals(String message, BigDecimal expected, BigDecimal actual) {
        AssertJUnit.assertEquals(message,
                (Object) (expected == null ? null : expected.setScale(2, RoundingMode.HALF_UP)),
                (Object) (actual == null ? null : actual.setScale(2, RoundingMode.HALF_UP)));
    }
}
