/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2016] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.*;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Created by hitesh on 2/9/16.
 * <p>
 * <p>
 * Flat Line Calculation:
 * flatLineAmount = (flatLinePrice * totalUsage)
 * <p>
 * Line Loss Line Calculation:
 * lineLossLineAmount = apply percentage on price according to RRCPercentagePricingStrategy.
 * lineLossLinePrice =  (lineLossLineAmount/flatLineAmount)*100
 */

@Test(groups = {"web-services", "pricing", "rrcp"}, testName = "RRCPercentagePricingStrategyTest")
public class RRCPercentagePricingStrategyTest {

    private static final Integer PRANCING_PONY = Integer.valueOf(1);
    private static final Integer ENABLED = Integer.valueOf(1);
    private static final Integer DISABLED = Integer.valueOf(0);
    private static final Integer US_DOLLAR = Integer.valueOf(1);
    private static final String COMMODITY_VALUE = "Electricity-" + System.currentTimeMillis();
    private static final String CHARGES_LINE_LOSS = "LineLoss-" + System.currentTimeMillis();
    private static final String CHARGES_CATEGORY = "Charges-" + System.currentTimeMillis();

    private final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static Integer PP_ACCOUNT_TYPE = 1;
    private static Integer PP_MONTHLY_PERIOD = 6;

    JbillingAPI api;
    ItemDTOEx flatItem;
    ItemDTOEx lineLossItem;
    RatingUnitWS ratingUnit;
    RouteRateCardWS rateCard;
    MatchingFieldWS zoneMatchingField;
    MatchingFieldWS effectiveDateMatchingField;
    MetaFieldWS zone;
    AccountInformationTypeWS ait;
    ItemTypeWS chargesCategory;
    UserWS user;
    PriceModelWS flatPriceModel;

    @BeforeClass
    public void setup() throws Exception {
        api = JbillingAPIFactory.getAPI();
        routeRateConfiguration();
        metaFieldConfiguration();
        createItemCategory();
        createItems();
        createUser();
    }

    @AfterClass
    public void cleanup() throws Exception {
        api.deleteUser(user.getUserId());
        api.deleteItem(flatItem.getId());
        api.deleteItem(lineLossItem.getId());
        api.deleteItemCategory(chargesCategory.getId());
        api.deleteMatchingField(zoneMatchingField.getId());
        api.deleteMatchingField(effectiveDateMatchingField.getId());
        api.deleteRouteRateCard(rateCard.getId());
        api.deleteRatingUnit(ratingUnit.getId());
        api.deleteMetaField(zone.getId());
        api.deleteAccountInformationType(ait.getId());
    }

    @Test
    public void test001RRCPercentagePricingStrategyWithOutBreakPoint() throws Exception {
        //Calculation for totalUsage = 100, flatLinePrice = 2.5
        BigDecimal price = new BigDecimal(2.5);
        Integer totalUsage = 100;
        updateFlatItemDefaultPrice(price);

        OrderWS order = generateAndProcessOrder("01/01/2016", "01/31/2016", user, totalUsage);
        assertThat(order.getOrderLines().length, is(2));

        //Flat Line Calculation:
        assertEquals(price, order.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("250.00"), order.getOrderLines()[0].getAmountAsDecimal());

        //Line Loss Line Calculation:
        assertEquals(new BigDecimal("5.00"), order.getOrderLines()[1].getPriceAsDecimal());
        assertEquals(new BigDecimal("12.50"), order.getOrderLines()[1].getAmountAsDecimal());
    }

    @Test
    public void test002RRCPercentagePricingStrategyWithOutBreakPoint() throws Exception {
        //Calculation for totalUsage = 100, flatLinePrice = 1.5
        BigDecimal price = new BigDecimal(1.5);
        Integer totalUsage = 150;
        updateFlatItemDefaultPrice(price);

        OrderWS order = generateAndProcessOrder("02/01/2016", "02/29/2016", user, totalUsage);
        assertThat(order.getOrderLines().length, is(2));

        //Flat Line Calculation:
        assertEquals(price, order.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("225.00"), order.getOrderLines()[0].getAmountAsDecimal());

        //Line Loss Line Calculation:
        assertEquals(new BigDecimal("6.00"), order.getOrderLines()[1].getPriceAsDecimal());
        assertEquals(new BigDecimal("13.5"), order.getOrderLines()[1].getAmountAsDecimal());
    }

    @Test
    public void test003RRCPercentagePricingStrategyWithBreakPoint() throws Exception {
        //Calculation for totalUsage = 1000, flatLinePrice = 0.103
        BigDecimal price = new BigDecimal(0.103);
        Integer totalUsage = 1000;
        updateFlatItemDefaultPrice(price);

        OrderWS order3 = generateAndProcessOrder("01/16/2016", "02/20/2016", user, totalUsage);
        assertThat(order3.getOrderLines().length, is(2));

        //Flat Line Calculation:
        assertEquals(price, order3.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("103.00"), order3.getOrderLines()[0].getAmountAsDecimal());

        //Line Loss Line Calculation:
        assertEquals(new BigDecimal("5.571428571"), order3.getOrderLines()[1].getPriceAsDecimal());
        assertEquals(new BigDecimal("5.738571429"), order3.getOrderLines()[1].getAmountAsDecimal());

    }

    @Test
    public void test004RRCPercentagePricingStrategyWithBreakPoint() throws Exception {
        //Calculation for totalUsage=1848, flatLinePrice = 2.0581904
        BigDecimal price = new BigDecimal(2.0581904);
        Integer totalUsage = 1848;
        updateFlatItemDefaultPrice(price);

        OrderWS order = generateAndProcessOrder("01/16/2016", "03/15/2016", user, totalUsage);
        assertThat(order.getOrderLines().length, is(2));

        //Flat Line Calculation:
        assertEquals(new BigDecimal("2.0581904"), order.getOrderLines()[0].getPriceAsDecimal());
        assertEquals(new BigDecimal("3803.5358592"), order.getOrderLines()[0].getAmountAsDecimal());

        //Line Loss Line Calculation:
        assertEquals(new BigDecimal("5.491525424"), order.getOrderLines()[1].getPriceAsDecimal());
        assertEquals(new BigDecimal("208.872138707"), order.getOrderLines()[1].getAmountAsDecimal());

    }

    private void routeRateConfiguration() throws Exception {
        ratingUnit = new RatingUnitWS();
        ratingUnit.setName("Single BI");
        ratingUnit.setPriceUnitName("single");
        ratingUnit.setIncrementUnitName("single");
        ratingUnit.setIncrementUnitQuantity("1");
        ratingUnit.setIsCanBeDeleted(true);
        ratingUnit.setId(api.createRatingUnit(ratingUnit));

        rateCard = new RouteRateCardWS();
        rateCard.setRatingUnitId(ratingUnit.getId());
        rateCard.setEntityId(1);
        rateCard.setName("nYMEX" + System.currentTimeMillis());
        rateCard.setId(api.createRouteRateCard(rateCard, createRateCardFile()));

        zoneMatchingField = new MatchingFieldWS();
        zoneMatchingField.setDescription("zone");
        zoneMatchingField.setMatchingField("zone");
        zoneMatchingField.setMediationField("ZONE");
        zoneMatchingField.setOrderSequence("2");
        zoneMatchingField.setRouteRateCardId(rateCard.getId());
        zoneMatchingField.setType(MatchingFieldType.EXACT.name());
        zoneMatchingField.setRequired(true);
        zoneMatchingField.setId(api.createMatchingField(zoneMatchingField));

        effectiveDateMatchingField = new MatchingFieldWS();
        effectiveDateMatchingField.setDescription("event_date");
        effectiveDateMatchingField.setMatchingField("active_dates");
        effectiveDateMatchingField.setMediationField("effective_date");
        effectiveDateMatchingField.setOrderSequence("1");
        effectiveDateMatchingField.setRouteRateCardId(rateCard.getId());
        effectiveDateMatchingField.setType(MatchingFieldType.ACTIVE_DATE.name());
        effectiveDateMatchingField.setRequired(true);
        effectiveDateMatchingField.setId(api.createMatchingField(effectiveDateMatchingField));

    }

    private void metaFieldConfiguration() {
        //Customer level meta field
        zone = new MetaFieldBuilder()
                .name(FileConstants.ZONE)
                .dataType(DataType.STRING)
                .entityType(EntityType.CUSTOMER)
                .primary(true)
                .entityId(PRANCING_PONY)
                .build();
        zone.setId(api.createMetaField(zone));

        //build ait with commodity meta field
        ait = new AccountInformationTypeBuilder(new Integer(1))
                .name("AccountInformation-" + System.currentTimeMillis())
                .addMetaField(new MetaFieldBuilder()
                        .dataType(DataType.STRING)
                        .entityType(EntityType.ACCOUNT_TYPE)
                        .name(FileConstants.COMMODITY)
                        .build())
                .entityId(PRANCING_PONY)
                .entityType(EntityType.ACCOUNT_TYPE)
                .build();
        ait.setId(api.createAccountInformationType(ait));
    }

    private void createUser() {
        //assign the zone meta field value
        final MetaFieldValueWS zoneMF = new MetaFieldValueWS();
        zoneMF.setFieldName(zone.getName());
        zoneMF.setValue("A");

        //assign the commodity meta field value
        final MetaFieldValueWS commodityMF = new MetaFieldValueWS();
        commodityMF.setFieldName(FileConstants.COMMODITY);
        commodityMF.setValue(COMMODITY_VALUE);
        commodityMF.setGroupId(ait.getId());

        user = PricingTestHelper.buildUser("rate-card", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new LinkedList<MetaFieldValueWS>() {{
            add(zoneMF);
            add(commodityMF);
        }});
        user.setUserId(api.createUser(user));
        assertNotNull("customer created", user.getUserId());
    }

    private File createRateCardFile() throws Exception {
        File file = File.createTempFile("ratecard", "csv");
        List<String> rows = Arrays.asList("id,name,surcharge,initial_increment,subsequent_increment,charge,active_dates,zone",
                "1,Jan,0,0,0,5,01/01/2016-02/01/2016,A",
                "2,Feb,0,0,0,6,02/01/2016-03/01/2016,A",
                "3,Mar,0,0,0,5,03/01/2016-04/01/2016,A",
                "4,Apr,0,0,0,6,04/01/2016-05/01/2016,A",
                "5,Jan,0,0,0,5,11/01/2015-11/30/2015,B",
                "6,Feb,0,0,0,6,02/01/2016-02/29/2016,B",
                "7,Mar,0,0,0,5,03/01/2016-03/31/2016,B",
                "8,Apr,0,0,0,6,04/01/2016-04/30/2016,B");

        FileUtils.writeLines(file, rows);
        return file;
    }


    protected static ItemDTOEx createItem(String description, boolean allowAssetManagement, boolean global, Integer... types) {
        ItemDTOEx item = new ItemDTOEx();
        description = description != null ? description : "TestItem: " + System.currentTimeMillis();
        item.setDescription(description);
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

    protected OrderWS generateAndProcessOrder(String from, String to, UserWS user, Integer lineQuantity) throws Exception {
        OrderWS order = PricingTestHelper.buildMonthlyOrder(user.getUserId(), Constants.ORDER_PERIOD_ONCE);
        order.setActiveSince(format.parse(from));
        order.setActiveUntil(format.parse(to));

        OrderLineWS flatLine = PricingTestHelper.buildOrderLine(flatItem.getId(), lineQuantity);
        flatLine.setDescription(COMMODITY_VALUE);
        OrderLineWS lineLossLine = PricingTestHelper.buildOrderLine(lineLossItem.getId(), lineQuantity);
        lineLossLine.setPercentage(true);
        lineLossLine.setDescription(CHARGES_LINE_LOSS);

        OrderChangeWS flatLineChange = OrderChangeBL.buildFromLine(flatLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
        flatLineChange.setStartDate(format.parse(from));
        OrderChangeWS lineLossChange = OrderChangeBL.buildFromLine(lineLossLine, order, ORDER_CHANGE_STATUS_APPLY_ID);
        lineLossChange.setStartDate(format.parse(from));
        lineLossChange.setParentOrderChange(flatLineChange);
        return api.rateOrder(order, new OrderChangeWS[]{flatLineChange, lineLossChange});
    }

    private void createItemCategory() {
        chargesCategory = new ItemTypeWS();
        chargesCategory.setDescription(CHARGES_CATEGORY);
        chargesCategory.setEntityId(api.getCallerCompanyId());
        chargesCategory.setEntities(Arrays.asList(api.getCallerCompanyId()));
        chargesCategory.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        chargesCategory.setId(api.createItemCategory(chargesCategory));
        assertNotNull("category created", chargesCategory.getId());
    }

    private void createItems() {
        flatItem = createItem(COMMODITY_VALUE, false, false, chargesCategory.getId());
        flatPriceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("1.00"), US_DOLLAR);
        flatItem.addDefaultPrice(CommonConstants.EPOCH_DATE, flatPriceModel);
        flatItem.setId(api.createItem(flatItem));

        lineLossItem = createItem(CHARGES_LINE_LOSS, false, false, chargesCategory.getId());
        PriceModelWS rrcPriceModel = new PriceModelWS(PriceModelStrategy.RRC_PERCENTAGE.name(), new BigDecimal("1.00"), US_DOLLAR);
        rrcPriceModel.addAttribute("route_rate_card_id", rateCard.getId().toString());
        lineLossItem.addDefaultPrice(CommonConstants.EPOCH_DATE, rrcPriceModel);
        lineLossItem.setId(api.createItem(lineLossItem));
    }

    private void updateFlatItemDefaultPrice(BigDecimal rate) {
        flatPriceModel.setRate(rate);
        api.updateItem(flatItem);
    }
}

