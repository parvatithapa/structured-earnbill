package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
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
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.apache.commons.io.FileUtils;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Created by jft on 23/6/16.
 */
@Test(groups = {"web-services", "lbmp"}, testName = "LBMPPlusBlendedRatePricingStrategyTest")
public class LBMPPlusBlendedRatePricingStrategyTest {

    private static final Integer PRANCING_PONY = Integer.valueOf(1);
    private static final Integer ENABLED = Integer.valueOf(1);
    private static final Integer DISABLED = Integer.valueOf(0);
    private static final Integer US_DOLLAR = Integer.valueOf(1);

    private final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static Integer PP_ACCOUNT_TYPE = 1;
    private static Integer PP_MONTHLY_PERIOD = 6;
    private static Integer CAT_CALLS = 2201;
    JbillingAPI api;
    ItemDTOEx item;
    RatingUnitWS ratingUnit;
    RouteRateCardWS rateCard;
    MatchingFieldWS zoneMatchingField;
    MatchingFieldWS effectiveDateMatchingField;
    MetaFieldWS zone;
    MetaFieldWS adderFee;
    PriceModelWS priceModel;
    UserWS user;

    @BeforeClass
    public void setup() throws Exception {
        api = JbillingAPIFactory.getAPI();

        ratingUnit = new RatingUnitWS();
        ratingUnit.setName("Single BI");
        ratingUnit.setPriceUnitName("single");
        ratingUnit.setIncrementUnitName("single");
        ratingUnit.setIncrementUnitQuantity("1");
        ratingUnit.setIsCanBeDeleted(true);


        rateCard = new RouteRateCardWS();
        rateCard.setEntityId(1);
        rateCard.setName("lBMP" + System.currentTimeMillis());


        zoneMatchingField = new MatchingFieldWS();
        zoneMatchingField.setDescription("zone");
        zoneMatchingField.setMatchingField("zone");
        zoneMatchingField.setMediationField("ZONE");
        zoneMatchingField.setOrderSequence("2");

        zoneMatchingField.setType(MatchingFieldType.EXACT.name());
        zoneMatchingField.setRequired(true);


        effectiveDateMatchingField = new MatchingFieldWS();
        effectiveDateMatchingField.setDescription("event_date");
        effectiveDateMatchingField.setMatchingField("active_dates");
        effectiveDateMatchingField.setMediationField("effective_date");
        effectiveDateMatchingField.setOrderSequence("1");

        effectiveDateMatchingField.setType(MatchingFieldType.ACTIVE_DATE.name());
        effectiveDateMatchingField.setRequired(true);


        //Customer level meta field
        zone = createMetaField(FileConstants.ZONE, DataType.STRING, EntityType.CUSTOMER);
        adderFee = createMetaField(FileConstants.ADDER_FEE_METAFIELD_NAME, DataType.DECIMAL, EntityType.CUSTOMER);

        //todo
//        PP_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);



        priceModel = new PriceModelWS(PriceModelStrategy.LBMP_PLUS_BLENDED_RATE.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute("adder_fee", "0.2");

    }

    @AfterClass
    public void cleanup() throws Exception {

        api.deleteMetaField(zone.getId());
        api.deleteMetaField(adderFee.getId());
    }

    private File createRateCardFile() throws Exception {
        File file = File.createTempFile("ratecard", "csv");
        List<String> rows = Arrays.asList("id,name,surcharge,initial_increment,subsequent_increment,charge,markup,capped_charge,capped_increment,minimum_charge,active_dates,zone",
                "1,Jan,0,0,0,0.10,0,0,0,0,01/01/2016-01/02/2016,A",
                "2,Jan,0,0,0,0.20,0,0,0,0,01/02/2016-01/03/2016,A",
                "3,Jan,0,0,0,0.30,0,0,0,0,01/03/2016-01/04/2016,A",
                "4,Jan,0,0,0,0.40,0,0,0,0,01/04/2016-01/05/2016,A",
                "5,Jan,0,0,0,0.50,0,0,0,0,01/05/2016-01/06/2016,A");

        FileUtils.writeLines(file, rows);
        return file;
    }
    private File createRateCardFile2() throws Exception {
        File file = File.createTempFile("ratecard", "csv");
        List<String> rows = Arrays.asList("id,name,surcharge,initial_increment,subsequent_increment,charge,markup,capped_charge,capped_increment,minimum_charge,active_dates,zone",
                "1,Jan,0,0,0,0.10,0,0,0,0,01/01/2016-01/02/2016,A",
                "3,Jan,0,0,0,0.30,0,0,0,0,01/03/2016-01/04/2016,A",
                "4,Jan,0,0,0,0.40,0,0,0,0,01/04/2016-01/05/2016,A",
                "5,Jan,0,0,0,0.50,0,0,0,0,01/05/2016-01/06/2016,A");

        FileUtils.writeLines(file, rows);
        return file;
    }


    @Test
    public void test001() throws Exception {
        ratingUnit.setId(api.createRatingUnit(ratingUnit));
        rateCard.setRatingUnitId(ratingUnit.getId());
        rateCard.setId(api.createRouteRateCard(rateCard, createRateCardFile()));
        zoneMatchingField.setRouteRateCardId(rateCard.getId());
        zoneMatchingField.setId(api.createMatchingField(zoneMatchingField));
        effectiveDateMatchingField.setRouteRateCardId(rateCard.getId());
        effectiveDateMatchingField.setId(api.createMatchingField(effectiveDateMatchingField));
        priceModel.addAttribute("route_rate_card_id", rateCard.getId().toString());

        item = createItem(false, false, CAT_CALLS);

        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        item.setId(api.createItem(item));
        api = JbillingAPIFactory.getAPI();

        // create user to test pricing with
        final MetaFieldValueWS zoneMF = new MetaFieldValueWS();
        zoneMF.setFieldName(zone.getName());
        zoneMF.setValue("A");


        UserWS user = PricingTestHelper.buildUser("rate-card", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new LinkedList<MetaFieldValueWS>() {{
            add(zoneMF);
        }});
        user.setUserId(api.createUser(user));
        assertNotNull("customer created", user.getUserId());

        OrderWS order1 = generateAndProcessOrder("01/01/2016", "01/05/2016", user);
        assertThat(order1.getOrderLines().length, is(1));
//        0.10+0.01
        assertEquals(new BigDecimal("0.5"), order1.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("50.00"), order1.getOrderLines()[0].getAmountAsDecimal());


        // clean up
        api.deleteUser(user.getUserId());
        api.deleteItem(item.getId());
        api.deleteMatchingField(zoneMatchingField.getId());
        api.deleteMatchingField(effectiveDateMatchingField.getId());
        api.deleteRouteRateCard(rateCard.getId());
        api.deleteRatingUnit(ratingUnit.getId());

    }

    @Test
    public void test002() throws Exception {
        ratingUnit.setId(api.createRatingUnit(ratingUnit));
        rateCard.setRatingUnitId(ratingUnit.getId());
        rateCard.setId(api.createRouteRateCard(rateCard, createRateCardFile2()));
        zoneMatchingField.setRouteRateCardId(rateCard.getId());
        zoneMatchingField.setId(api.createMatchingField(zoneMatchingField));
        effectiveDateMatchingField.setRouteRateCardId(rateCard.getId());
        effectiveDateMatchingField.setId(api.createMatchingField(effectiveDateMatchingField));
        priceModel.addAttribute("route_rate_card_id", rateCard.getId().toString());
        item = createItem(false, false, CAT_CALLS);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        item.setId(api.createItem(item));
        api = JbillingAPIFactory.getAPI();
        Boolean exceptionThrown=false;
        try {

            // create user to test pricing with
            final MetaFieldValueWS zoneMF = new MetaFieldValueWS();
            zoneMF.setFieldName(zone.getName());
            zoneMF.setValue("A");
            final MetaFieldValueWS adderFeeMF = new MetaFieldValueWS();
            adderFeeMF.setFieldName(adderFee.getName());
            adderFeeMF.setValue("0.05");
             user = PricingTestHelper.buildUser("rate-card", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new LinkedList<MetaFieldValueWS>() {{
                add(zoneMF);
//            add(adderFeeMF);
            }});
            user.setUserId(api.createUser(user));
            assertNotNull("customer created", user.getUserId());
            OrderWS order1 = generateAndProcessOrder("01/01/2016", "01/05/2016", user);

        }
        catch (Exception ex ){
            if(ex.getMessage().contains("No rate find for these periods : 01/02/2016"))exceptionThrown = true;
            assertTrue(exceptionThrown);
        }
        finally {
            // clean up
            if(null != user) api.deleteUser(user.getUserId());
            if(null != item)api.deleteItem(item.getId());
            if(null != zoneMatchingField)api.deleteMatchingField(zoneMatchingField.getId());
            if(null != effectiveDateMatchingField)api.deleteMatchingField(effectiveDateMatchingField.getId());
            if(null != rateCard)api.deleteRouteRateCard(rateCard.getId());
            if(null != ratingUnit)api.deleteRatingUnit(ratingUnit.getId());
        }



    }

    protected static ItemDTOEx createItem(boolean allowAssetManagement, boolean global, Integer... types) {
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("TestItem: " + System.currentTimeMillis());
        item.setNumber("TestWS-" + System.currentTimeMillis());
        item.setTypes(types);
        if (allowAssetManagement) {
            item.setAssetManagementEnabled(ENABLED);
        }
        item.setExcludedTypes(new Integer[]{});
        item.setHasDecimals(DISABLED);
        if (global) {
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

    protected MetaFieldWS createMetaField(String metaFieldName, DataType dataType, EntityType type) {
        MetaFieldWS newMetaField = new MetaFieldWS();
        newMetaField.setName(metaFieldName);
        newMetaField.setDataType(dataType);
        newMetaField.setEntityType(type);
        newMetaField.setPrimary(true);
        newMetaField.setEntityId(PRANCING_PONY);
        newMetaField.setId(api.createMetaField(newMetaField));
        return newMetaField;
    }

    protected OrderWS generateAndProcessOrder(String from, String to, UserWS user) throws Exception {
        OrderWS order = PricingTestHelper.buildMonthlyOrder(user.getUserId(), Constants.ORDER_PERIOD_ONCE);
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        order.setActiveSince(format.parse(from));
        order.setActiveUntil(format.parse(to));

        OrderLineWS line = PricingTestHelper.buildOrderLine(item.getId(), 100);
//        order.setOrderLines(new OrderLineWS[]{line});
        OrderChangeWS change = OrderChangeBL.buildFromLine(line, order, ORDER_CHANGE_STATUS_APPLY_ID);
        change.setStartDate(format.parse(from));
        return api.rateOrder(order, new OrderChangeWS[]{change});
    }
}
