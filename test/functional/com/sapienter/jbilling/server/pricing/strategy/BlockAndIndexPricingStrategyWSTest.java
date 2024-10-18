package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Block and Index tests
 *
 * @author Gerhard Maree
 * @since 10-Nov-2015
 */
@Test(groups = { "web-services", "pricing", "rate-card" }, testName = "BlockAndIndexPricingStrategyWSTest")
public class BlockAndIndexPricingStrategyWSTest {

    private static final Integer PRANCING_PONY = Integer.valueOf(1);
    private static final Integer ENABLED = Integer.valueOf(1);
    private static final Integer DISABLED = Integer.valueOf(0);
    private static final Integer US_DOLLAR = Integer.valueOf(1);

    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private static Integer PP_ACCOUNT_TYPE = 1;
	private static Integer PP_MONTHLY_PERIOD = 6;
    private static Integer CAT_CALLS = 2201;
    JbillingAPI api;
    ItemDTOEx item;
    RatingUnitWS ratingUnit;
    RouteRateCardWS rateCard;
    MatchingFieldWS mfkeyMf;
    MatchingFieldWS eventDateMf;

    @BeforeClass
    public void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();

        ratingUnit = new RatingUnitWS();
        ratingUnit.setName("Single BI");
        ratingUnit.setPriceUnitName("single");
        ratingUnit.setIncrementUnitName("single");
        ratingUnit.setIncrementUnitQuantity("1");
        ratingUnit.setIsCanBeDeleted(true);
        ratingUnit.setId(api.createRatingUnit(ratingUnit));

        rateCard = new RouteRateCardWS();
        rateCard.setRatingUnitId(ratingUnit.getId());
//        rateCard.setTableName();
        rateCard.setEntityId(1);
        rateCard.setName("bitest" + System.currentTimeMillis());
        rateCard.setId(api.createRouteRateCard(rateCard, createRateCardFile()));

        mfkeyMf = new MatchingFieldWS();
        mfkeyMf.setDescription("mfkey");
        mfkeyMf.setMatchingField("mfkey");
        mfkeyMf.setMediationField("mfkey");
        mfkeyMf.setOrderSequence("2");
        mfkeyMf.setRouteRateCardId(rateCard.getId());
        mfkeyMf.setType(MatchingFieldType.EXACT.name());
        mfkeyMf.setRequired(true);
        mfkeyMf.setId(api.createMatchingField(mfkeyMf));

        eventDateMf = new MatchingFieldWS();
        eventDateMf.setDescription("event_date");
        eventDateMf.setMatchingField("active_dates");
        eventDateMf.setMediationField("event_date");
        eventDateMf.setOrderSequence("1");
        eventDateMf.setRouteRateCardId(rateCard.getId());
        eventDateMf.setType(MatchingFieldType.ACTIVE_DATE.name());
        eventDateMf.setRequired(true);
        eventDateMf.setId(api.createMatchingField(eventDateMf));

	    PP_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);

        item = createItem(false, false, CAT_CALLS);

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.BLOCK_AND_INDEX.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute("block_quantity", "100");
        priceModel.addAttribute("block_rate", "50");
        priceModel.addAttribute("route_rate_card_id", rateCard.getId().toString());
        priceModel.addAttribute("pf_mfkey", "GROUP1");

        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        item.setId(api.createItem(item));
    }

    private File createRateCardFile() throws Exception {
        File file = File.createTempFile("ratecard", "csv");
        List<String> rows = Arrays.asList("id,name,surcharge,initial_increment,subsequent_increment,charge,active_dates,mfkey",
                "1,JERSYCITY NJ,0,0,1,0.11,11/01/2015-11/30/2015,GROUP1",
                "2,JERSYCITY NJ,0,0,1,0.12,12/01/2015-12/31/2015,GROUP1",
                "3,JERSYCITY NJ,0,0,1,0.13,01/01/2016-01/31/2015,GROUP1",
                "4,JERSYCITY NJ,0,0,1,0.14,02/01/2016-02/28/2015,GROUP1",
                "5,JERSYCITY NJ,0,0,1,0.21,11/01/2015-11/30/2015,GROUP2",
                "6,JERSYCITY NJ,0,0,1,0.22,12/01/2015-12/31/2015,GROUP2",
                "7,JERSYCITY NJ,0,0,1,0.23,01/01/2016-01/31/2015,GROUP2",
                "8,JERSYCITY NJ,0,0,1,0.24,02/01/2016-02/28/2015,GROUP2");

        FileUtils.writeLines(file, rows);
        return file;
    }

    @AfterClass
    public void cleanup() throws Exception {
        api.deleteItem(item.getId());
        api.deleteMatchingField(mfkeyMf.getId());
        api.deleteMatchingField(eventDateMf.getId());
        api.deleteRouteRateCard(rateCard.getId());
        api.deleteRatingUnit(ratingUnit.getId());
    }

    @Test
    public void test001BlockAndIndex() throws Exception {
        api = JbillingAPIFactory.getAPI();
        // long distance call uses the rate card
        // see the test db for details
        final int LONG_DISTANCE_CALL = 2800;

        // create user to test pricing with
        UserWS user = PricingTestHelper.buildUser("rate-card", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE);
        user.setUserId(api.createUser(user));
        assertNotNull("customer created", user.getUserId());


        // rate a 100 minute call to 55999 @ 0.33/min
        // 100 x 0.33 = $33.00
        PricingField[] pf = {
            new PricingField("event_date", "11/15/2015")
        };

        OrderWS order = PricingTestHelper.buildMonthlyOrder(user.getUserId(), PP_MONTHLY_PERIOD);
        order.setPricingFields(PricingField.setPricingFieldsValue(pf));

        OrderLineWS line = PricingTestHelper.buildOrderLine(item.getId(), 90);
        order.setOrderLines(new OrderLineWS[] { line });

        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("48.90"), order.getOrderLines()[0].getAmountAsDecimal());

        line.setQuantity(110);
        order.setPricingFields(PricingField.setPricingFieldsValue(pf));
        order.setOrderLines(new OrderLineWS[] { line });

        order = api.rateOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("51.10"), order.getOrderLines()[0].getAmountAsDecimal());


        // clean up
        api.deleteUser(user.getUserId());
    }

    protected static ItemDTOEx createItem(boolean allowAssetManagement, boolean global, Integer... types){
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("TestItem: " + System.currentTimeMillis());
        item.setNumber("TestWS-" + System.currentTimeMillis());
        item.setTypes(types);
        if(allowAssetManagement){
            item.setAssetManagementEnabled(ENABLED);
        }
        item.setExcludedTypes(new Integer[]{});
        item.setHasDecimals(DISABLED);
        if(global) {
            item.setGlobal(global);
        } else {
            item.setGlobal(false);
        }
        item.setDeleted(DISABLED);
        item.setEntityId(PRANCING_PONY);
        ArrayList<Integer> entities = new ArrayList<Integer>();
        entities.add(PRANCING_PONY);
        item.setEntities(entities);
        return item;
    }
}
