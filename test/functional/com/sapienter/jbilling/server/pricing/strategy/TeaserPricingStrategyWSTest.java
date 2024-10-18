package com.sapienter.jbilling.server.pricing.strategy;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.PricingTestHelper;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessTestCase;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Teaser Pricing Strategy test
 *
 * @author Leandro Bagur
 * @since 26/09/2017
 */
@Test(groups = { "web-services", "pricing" }, testName = "TeaserPricingStrategyWSTest")
public class TeaserPricingStrategyWSTest {

    public static final Logger LOGGER = LoggerFactory.getLogger(TeaserPricingStrategyWSTest.class);

    private static final Integer PRANCING_PONY = 1;
    private static final Integer US_DOLLAR = 1;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private static Integer PP_ACCOUNT_TYPE = 1;
    private static Integer CAT_CALLS = 2201;
    
    private JbillingAPI api;
    private ItemDTOEx item;
    private UserWS user;
    private Integer PP_MONTHLY_PERIOD;
    private Integer PP_SEMI_MONTHLY_PERIOD;
    private Integer PP_WEEKLY_PERIOD;
    private Integer PP_YEARLY_PERIOD;
    private static final String ORDER_CHANGE_STATUS_PENDING_ID = "PENDING";

    @BeforeClass
    public void getAPI() throws Exception {
        api = JbillingAPIFactory.getAPI();
	    PP_MONTHLY_PERIOD = PricingTestHelper.getOrCreateMonthlyOrderPeriod(api);
        PP_SEMI_MONTHLY_PERIOD = PricingTestHelper.getOrCreateSemiMonthlyOrderPeriod(api);
        PP_WEEKLY_PERIOD = PricingTestHelper.getOrCreateWeeklyOrderPeriod(api);
        PP_YEARLY_PERIOD = PricingTestHelper.getOrCreateYearlyOrderPeriod(api);
        user = PricingTestHelper.buildUser("teaser", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        user.setUserId(api.createUser(user));
        assertNotNull("customer created", user.getUserId());
        
        BillingProcessConfigurationWS process = api.getBillingProcessConfiguration();
        process.setMaximumPeriods(99);
        process.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());
        api.createUpdateBillingProcessConfiguration(process);
    }

    @Test(priority = 1)
    public void testCreateItemWithTeaserPricing() throws Exception {
        // new item with teaser pricing
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute(TeaserPricingStrategy.USE_ORDER_PERIOD, TeaserPricingStrategy.UseOrderPeriod.YES.name());
        priceModel.addAttribute(TeaserPricingStrategy.PERIOD, null);
        priceModel.addAttribute(TeaserPricingStrategy.FIRST_PERIOD, "10");
        priceModel.addAttribute("3", "20");

        item = createItemDTOEx(priceModel);
        assertNotNull("item created", item.getId());
    }

    @Test(priority = 2)
    public void testOneTimeForCurrentDate() throws Exception {
        OrderWS order = getOrderWS(new Date(), true, item.getId());

        Integer orderId = api.createOrder(order, getOrderChanges(order, new Date()));

        order = api.getOrder(orderId);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("10.00"), order.getOrderLines()[0].getAmountAsDecimal());

        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertThat(invoice.getInvoiceLines().length, is(1));
        assertEquals(new BigDecimal("10.00"), invoice.getInvoiceLines()[0].getAmountAsDecimal());
    }

    @Test(priority = 3)
    public void testOneTimeForPastDate() throws Exception {
        LocalDate startLocalDate = LocalDate.now().minusMonths(6);
        Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        OrderWS order = getOrderWS(startDate, true, item.getId());
        Integer orderId = api.createOrder(order, getOrderChanges(order, startDate));

        order = api.getOrder(orderId);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("10.00"), order.getOrderLines()[0].getAmountAsDecimal());
    }


    @Test(priority = 4)
    public void testOneTimeForFutureDate() throws Exception {
        OrderWS order = getOrderWS(new Date(), true, item.getId());
        LocalDate startLocalDate = LocalDate.now().plusMonths(1);
        Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        Integer orderId = api.createOrder(order, getOrderChanges(order, startDate));
        order = api.getOrder(orderId);

        assertThat(order.getOrderLines().length, is(0));
    }

    @Test(priority = 5)
    public void testMonthlyForCurrentDate() throws Exception {
        OrderWS order = getOrderWS(new Date(), false, item.getId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);

        Integer orderId = api.createOrder(order, getOrderChanges(order, new Date()));
        order = api.getOrder(orderId);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("10.00"), order.getOrderLines()[0].getAmountAsDecimal());
    }


    @Test(priority = 6)
    public void testMonthlyForPastDate() throws Exception {
        LocalDate startLocalDate = LocalDate.now().minusMonths(4);
        Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        OrderWS order = getOrderWS(startDate, false, item.getId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);

        Integer orderId = api.createOrder(order, getOrderChanges(order, startDate));
        order = api.getOrder(orderId);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("10.00"), order.getOrderLines()[0].getAmountAsDecimal());

        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertThat(invoice.getInvoiceLines().length, is(5));
    }

    @Test(priority = 7)
    public void testMonthlyForPastDateUsingManualPeriod() throws Exception {
        OrderPeriodWS orderPeriod = new OrderPeriodWS();
        orderPeriod.setEntityId(user.getEntityId());
        orderPeriod.setPeriodUnitId(PeriodUnitDTO.WEEK);
        orderPeriod.setValue(1);
        orderPeriod.setDescriptions(Collections.singletonList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "PP:WEEKLY")));
        Integer orderPeriodId = api.createOrderPeriod(orderPeriod);

        // new item with teaser pricing
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute(TeaserPricingStrategy.USE_ORDER_PERIOD, TeaserPricingStrategy.UseOrderPeriod.NO.name());
        priceModel.addAttribute(TeaserPricingStrategy.PERIOD, orderPeriodId.toString());
        priceModel.addAttribute(TeaserPricingStrategy.FIRST_PERIOD, "10");
        priceModel.addAttribute("2", "15");
        priceModel.addAttribute("3", "20");

        ItemDTOEx itemDTO = createItemDTOEx(priceModel);

        LocalDate startLocalDate = LocalDate.now().minusMonths(1);
        Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

        OrderWS order = getOrderWS(startDate, false, itemDTO.getId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);

        Integer orderId = api.createOrder(order, getOrderChanges(order, startDate));
        order = api.getOrder(orderId);

        assertThat(order.getOrderLines().length, is(1));
        assertEquals(new BigDecimal("10.00"), order.getOrderLines()[0].getAmountAsDecimal());

        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);

        assertEquals(new BigDecimal("10.00"), invoice.getInvoiceLines()[0].getAmountAsDecimal());
        assertEquals(new BigDecimal("15.00"), invoice.getInvoiceLines()[1].getAmountAsDecimal());
        assertThat(invoice.getInvoiceLines().length, is(4));

        api.deleteItem(itemDTO.getId());
        api.deleteOrderPeriod(orderPeriodId);
    }

    @Test(priority = 8)
    public void testMonthlyOrderWithProRate() throws Exception {
        Date startDate = Date.from(LocalDate.of(2007, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(LocalDate.of(2007, 8, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate = Date.from(LocalDate.of(2007, 1, 14).atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserWS userWS = PricingTestHelper.buildUser("teaser", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        userWS.setUserId(api.createUser(userWS));
        userWS.getMainSubscription().setNextInvoiceDayOfPeriod(1);
        userWS.setNextInvoiceDate(startDate);
        api.updateUser(userWS);
        userWS = api.getUserWS(userWS.getId());

        OrderWS order = getOrderWS(startDate, false, item.getId());
        order.setUserId(userWS.getUserId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setActiveUntil(endDate);
        order.setProrateFlag(true);
        order = api.getOrder(api.createOrder(order, getOrderChanges(order, effectiveDate)));

        InvoiceWS invoice = api.getInvoiceWS(api.createInvoiceFromOrder(order.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice.getInvoiceLines().length, 8);

        LOGGER.debug("Checking the periods of the invoice lines");
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[0].getDescription().matches("(.*) Period from 01/14/2007 to 01/31/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[1].getDescription().matches("(.*) Period from 02/01/2007 to 02/28/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[2].getDescription().matches("(.*) Period from 03/01/2007 to 03/31/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[3].getDescription().matches("(.*) Period from 04/01/2007 to 04/30/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[4].getDescription().matches("(.*) Period from 05/01/2007 to 05/31/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[5].getDescription().matches("(.*) Period from 06/01/2007 to 06/30/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[6].getDescription().matches("(.*) Period from 07/01/2007 to 07/31/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[7].getDescription().matches("(.*) Period from 08/01/2007 to 08/31/2007"));

        LOGGER.debug("Checking the periods of the invoice lines");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[0].getAmount(), "5.8064516136");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[5].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[6].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[7].getAmount(), "20.0000000000");

        LOGGER.debug("Deleting data created into the test");
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(userWS.getUserId());
    }

    @Test(priority = 9)
    public void testSemiMonthlyOrderWithProRate() throws Exception {
        Date startDate1 = Date.from(LocalDate.of(2007, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date startDate2 = Date.from(LocalDate.of(2007, 1, 3).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date startDate3 = Date.from(LocalDate.of(2007, 6, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate1 = Date.from(LocalDate.of(2007, 5, 15).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate2 = Date.from(LocalDate.of(2007, 5, 17).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate3 = Date.from(LocalDate.of(2007, 8, 16).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate1 = Date.from(LocalDate.of(2007, 1, 9).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate2 = Date.from(LocalDate.of(2007, 1, 19).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate3 = Date.from(LocalDate.of(2007, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserWS user1 = PricingTestHelper.buildUser("teaserSemiMonthly1", PP_SEMI_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        user1.setUserId(api.createUser(user1));
        user1.getMainSubscription().setNextInvoiceDayOfPeriod(1);//1 & 16
        user1.setNextInvoiceDate(startDate1);
        api.updateUser(user1);
        user1 = api.getUserWS(user1.getId());

        UserWS user2 = PricingTestHelper.buildUser("teaserSemiMonthly2", PP_SEMI_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        user2.setUserId(api.createUser(user2));
        user2.getMainSubscription().setNextInvoiceDayOfPeriod(3);//3 & 18
        user2.setNextInvoiceDate(startDate2);
        api.updateUser(user2);
        user2 = api.getUserWS(user2.getId());

        UserWS user3 = PricingTestHelper.buildUser("teaserSemiMonthly3", PP_SEMI_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        user3.setUserId(api.createUser(user3));
        user3.getMainSubscription().setNextInvoiceDayOfPeriod(2);//2 & 17
        user3.setNextInvoiceDate(startDate3);
        api.updateUser(user3);
        user3 = api.getUserWS(user3.getId());

        OrderWS order1 = getOrderWS(startDate1, false, item.getId());
        order1.setUserId(user1.getUserId());
        order1.setPeriod(PP_SEMI_MONTHLY_PERIOD);
        order1.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order1.setActiveUntil(endDate1);
        order1.setProrateFlag(true);
        order1 = api.getOrder(api.createOrder(order1, getOrderChanges(order1, effectiveDate1)));

        OrderWS order2 = getOrderWS(startDate2, false, item.getId());
        order2.setUserId(user2.getUserId());
        order2.setPeriod(PP_SEMI_MONTHLY_PERIOD);
        order2.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order2.setActiveUntil(endDate2);
        order2.setProrateFlag(true);
        order2 = api.getOrder(api.createOrder(order2, getOrderChanges(order2, effectiveDate2)));

        OrderWS order3 = getOrderWS(startDate1, false, item.getId());
        order3.setUserId(user3.getUserId());
        order3.setPeriod(PP_SEMI_MONTHLY_PERIOD);
        order3.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order3.setActiveUntil(endDate3);
        order3.setProrateFlag(true);
        order3 = api.getOrder(api.createOrder(order3, getOrderChanges(order3, effectiveDate3)));

        InvoiceWS invoice1 = api.getInvoiceWS(api.createInvoiceFromOrder(order1.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice1);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice1.getInvoiceLines().length, 9);

        InvoiceWS invoice2 = api.getInvoiceWS(api.createInvoiceFromOrder(order2.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice2);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice2.getInvoiceLines().length, 8);

        InvoiceWS invoice3 = api.getInvoiceWS(api.createInvoiceFromOrder(order3.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice3);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice3.getInvoiceLines().length, 6);

        LOGGER.debug("Checking the periods of the invoice lines for user {}", invoice1.getId());
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[0].getDescription().matches("(.*) Period from 01/09/2007 to 01/15/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[1].getDescription().matches("(.*) Period from 01/16/2007 to 01/31/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[2].getDescription().matches("(.*) Period from 02/01/2007 to 02/15/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[3].getDescription().matches("(.*) Period from 02/16/2007 to 02/28/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[4].getDescription().matches("(.*) Period from 03/01/2007 to 03/15/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[5].getDescription().matches("(.*) Period from 03/16/2007 to 03/31/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[6].getDescription().matches("(.*) Period from 04/01/2007 to 04/15/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[7].getDescription().matches("(.*) Period from 04/16/2007 to 04/30/2007"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[8].getDescription().matches("(.*) Period from 05/01/2007 to 05/15/2007"));

        LOGGER.debug("Checking the periods of the invoice lines for user {}", invoice2.getId());
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[0].getDescription().matches("(.*) Period from 01/19/2007 to 02/02/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[1].getDescription().matches("(.*) Period from 02/03/2007 to 02/17/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[2].getDescription().matches("(.*) Period from 02/18/2007 to 03/02/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[3].getDescription().matches("(.*) Period from 03/03/2007 to 03/17/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[4].getDescription().matches("(.*) Period from 03/18/2007 to 04/02/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[5].getDescription().matches("(.*) Period from 04/03/2007 to 04/17/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[6].getDescription().matches("(.*) Period from 04/18/2007 to 05/02/2007"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[7].getDescription().matches("(.*) Period from 05/03/2007 to 05/17/2007"));

        LOGGER.debug("Checking the periods of the invoice lines for user {}", invoice3.getId());
        assertTrue("Invoices Lines Description not match", invoice3.getInvoiceLines()[0].getDescription().matches("(.*) Period from 06/01/2007 to 06/01/2007"));
        assertTrue("Invoices Lines Description not match", invoice3.getInvoiceLines()[1].getDescription().matches("(.*) Period from 06/02/2007 to 06/16/2007"));
        assertTrue("Invoices Lines Description not match", invoice3.getInvoiceLines()[2].getDescription().matches("(.*) Period from 06/17/2007 to 07/01/2007"));
        assertTrue("Invoices Lines Description not match", invoice3.getInvoiceLines()[3].getDescription().matches("(.*) Period from 07/02/2007 to 07/16/2007"));
        assertTrue("Invoices Lines Description not match", invoice3.getInvoiceLines()[4].getDescription().matches("(.*) Period from 07/17/2007 to 08/01/2007"));
        assertTrue("Invoices Lines Description not match", invoice3.getInvoiceLines()[5].getDescription().matches("(.*) Period from 08/02/2007 to 08/16/2007"));

        LOGGER.debug("Checking the periods of the invoice lines for user {}", invoice1.getId());
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[0].getAmount(), "4.6666666669");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[5].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[6].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[7].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[8].getAmount(), "20.0000000000");

        LOGGER.debug("Checking the periods of the invoice lines for user {}", invoice2.getId());
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[0].getAmount(), "9.3750000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[5].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[6].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[7].getAmount(), "20.0000000000");

        LOGGER.debug("Checking the periods of the invoice lines for user {}", invoice3.getId());
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice3.getInvoiceLines()[0].getAmount(), "0.6250000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice3.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice3.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice3.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice3.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice3.getInvoiceLines()[5].getAmount(), "20.0000000000");

        LOGGER.debug("Deleting data created into the test");
        api.deleteInvoice(invoice1.getId());
        api.deleteInvoice(invoice2.getId());
        api.deleteInvoice(invoice3.getId());
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteOrder(order3.getId());
        api.deleteUser(user1.getUserId());
        api.deleteUser(user2.getUserId());
        api.deleteUser(user3.getUserId());
    }

    @Test(priority = 10)
    public void testWeeklyOrderWithProRate() throws Exception {
        Date startDate = Date.from(LocalDate.of(2007, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(LocalDate.of(2007, 2, 25).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate = Date.from(LocalDate.of(2007, 1, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserWS userWS = PricingTestHelper.buildUser("teaserWeekly", PP_WEEKLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        userWS.setUserId(api.createUser(userWS));
        userWS.getMainSubscription().setNextInvoiceDayOfPeriod(2);//Monday
        userWS.setNextInvoiceDate(startDate);
        api.updateUser(userWS);
        userWS = api.getUserWS(userWS.getId());

        OrderWS order = getOrderWS(startDate, false, item.getId());
        order.setUserId(userWS.getUserId());
        order.setPeriod(PP_WEEKLY_PERIOD);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setActiveUntil(endDate);
        order.setProrateFlag(true);
        order = api.getOrder(api.createOrder(order, getOrderChanges(order, effectiveDate)));

        InvoiceWS invoice = api.getInvoiceWS(api.createInvoiceFromOrder(order.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice.getInvoiceLines().length, 8);

        LOGGER.debug("Checking the periods of the invoice lines");
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[0].getDescription().matches("(.*) Period from 01/04/2007 to 01/07/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[1].getDescription().matches("(.*) Period from 01/08/2007 to 01/14/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[2].getDescription().matches("(.*) Period from 01/15/2007 to 01/21/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[3].getDescription().matches("(.*) Period from 01/22/2007 to 01/28/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[4].getDescription().matches("(.*) Period from 01/29/2007 to 02/04/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[5].getDescription().matches("(.*) Period from 02/05/2007 to 02/11/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[6].getDescription().matches("(.*) Period from 02/12/2007 to 02/18/2007"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[7].getDescription().matches("(.*) Period from 02/19/2007 to 02/25/2007"));

        LOGGER.debug("Checking the periods of the invoice lines");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[0].getAmount(), "5.7142857144");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[5].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[6].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice.getInvoiceLines()[7].getAmount(), "20.0000000000");

        LOGGER.debug("Deleting data created into the test");
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(userWS.getUserId());
    }

    @Test(priority = 11)
    public void testYearlyOrderWithProRate() throws Exception {
        boolean isLeap = Year.now().isLeap();
        Date startDate1 = Date.from(LocalDate.of(2007, 1, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date startDate2 = Date.from(LocalDate.of(2007, 1, 3).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date nextInvoiceDate1 = Date.from(LocalDate.of(2007, 6, 1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date nextInvoiceDate2 = Date.from(LocalDate.of(2007, 1, 3).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate1 = Date.from(LocalDate.of(2017, 5, 31).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate2 = Date.from(LocalDate.of(2017, 1, 2).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate1 = Date.from(LocalDate.of(2007, 7, 4).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate2 = Date.from(LocalDate.of(2007, 1, 3).atStartOfDay(ZoneId.systemDefault()).toInstant());

        UserWS user1 = PricingTestHelper.buildUser("teaserYearly1", PP_YEARLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        user1.setUserId(api.createUser(user1));
        user1.getMainSubscription().setNextInvoiceDayOfPeriod(isLeap ? 153 : 152);//June 1
        user1.setNextInvoiceDate(nextInvoiceDate1);
        api.updateUser(user1);
        user1 = api.getUserWS(user1.getId());
        if (isLeap) {
            user1.getMainSubscription().setNextInvoiceDayOfPeriod(152);// June 1
            api.updateUser(user1);
            user1 = api.getUserWS(user1.getId());
        }
        UserWS user2 = PricingTestHelper.buildUser("teaserYearly2", PP_YEARLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        user2.setUserId(api.createUser(user2));
        user2.getMainSubscription().setNextInvoiceDayOfPeriod(3);//Jan 3
        user2.setNextInvoiceDate(nextInvoiceDate2);
        api.updateUser(user2);
        user2 = api.getUserWS(user2.getId());

        OrderWS order1 = getOrderWS(startDate1, false, item.getId());
        order1.setUserId(user1.getUserId());
        order1.setPeriod(PP_YEARLY_PERIOD);
        order1.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order1.setActiveUntil(endDate1);
        order1.setProrateFlag(true);
        order1 = api.getOrder(api.createOrder(order1, getOrderChanges(order1, effectiveDate1)));

        OrderWS order2 = getOrderWS(startDate2, false, item.getId());
        order2.setUserId(user2.getUserId());
        order2.setPeriod(PP_YEARLY_PERIOD);
        order2.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order2.setActiveUntil(endDate2);
        order2.setProrateFlag(true);
        order2 = api.getOrder(api.createOrder(order2, getOrderChanges(order2, effectiveDate2)));

        InvoiceWS invoice1 = api.getInvoiceWS(api.createInvoiceFromOrder(order1.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice1);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice1.getInvoiceLines().length, 10);

        InvoiceWS invoice2 = api.getInvoiceWS(api.createInvoiceFromOrder(order2.getId(), null));
        BillingProcessTestCase.sortInvoiceLines(invoice2);
        AssertJUnit.assertEquals("Invoices Lines Quantity not match", invoice2.getInvoiceLines().length, 10);

        LOGGER.debug("Checking the periods of the invoice lines for invoice {}", invoice1.getId());
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[0].getDescription().matches("(.*) Period from 07/04/2007 to 05/31/2008"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[1].getDescription().matches("(.*) Period from 06/01/2008 to 05/31/2009"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[2].getDescription().matches("(.*) Period from 06/01/2009 to 05/31/2010"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[3].getDescription().matches("(.*) Period from 06/01/2010 to 05/31/2011"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[4].getDescription().matches("(.*) Period from 06/01/2011 to 05/31/2012"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[5].getDescription().matches("(.*) Period from 06/01/2012 to 05/31/2013"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[6].getDescription().matches("(.*) Period from 06/01/2013 to 05/31/2014"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[7].getDescription().matches("(.*) Period from 06/01/2014 to 05/31/2015"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[8].getDescription().matches("(.*) Period from 06/01/2015 to 05/31/2016"));
        assertTrue("Invoices Lines Description not match", invoice1.getInvoiceLines()[9].getDescription().matches("(.*) Period from 06/01/2016 to 05/31/2017"));

        LOGGER.debug("Checking the periods of the invoice lines for invoice {}", invoice2.getId());
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[0].getDescription().matches("(.*) Period from 01/03/2007 to 01/02/2008"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[1].getDescription().matches("(.*) Period from 01/03/2008 to 01/02/2009"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[2].getDescription().matches("(.*) Period from 01/03/2009 to 01/02/2010"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[3].getDescription().matches("(.*) Period from 01/03/2010 to 01/02/2011"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[4].getDescription().matches("(.*) Period from 01/03/2011 to 01/02/2012"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[5].getDescription().matches("(.*) Period from 01/03/2012 to 01/02/2013"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[6].getDescription().matches("(.*) Period from 01/03/2013 to 01/02/2014"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[7].getDescription().matches("(.*) Period from 01/03/2014 to 01/02/2015"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[8].getDescription().matches("(.*) Period from 01/03/2015 to 01/02/2016"));
        assertTrue("Invoices Lines Description not match", invoice2.getInvoiceLines()[9].getDescription().matches("(.*) Period from 01/03/2016 to 01/02/2017"));

        LOGGER.debug("Checking the amount of the invoice lines for invoice {}", invoice1.getId());
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[0].getAmount(), "9.0983606652");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[5].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[6].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[7].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[8].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice1.getInvoiceLines()[9].getAmount(), "20.0000000000");

        LOGGER.debug("Checking the amount of the invoice lines for invoice {}", invoice2.getId());
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[0].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[1].getAmount(), "10.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[2].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[3].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[4].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[5].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[6].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[7].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[8].getAmount(), "20.0000000000");
        AssertJUnit.assertEquals("Invoices Lines Amount not match", invoice2.getInvoiceLines()[9].getAmount(), "20.0000000000");

        LOGGER.debug("Deleting data created into the test");
        api.deleteInvoice(invoice1.getId());
        api.deleteInvoice(invoice2.getId());
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteUser(user1.getUserId());
        api.deleteUser(user2.getUserId());
    }

    @Test(priority = 12)
    public void testProdcutLevelTeaserPricing(){
        Date startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        UserWS teaserProductUserWS = PricingTestHelper.buildUser("teaser", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        teaserProductUserWS.setUserId(api.createUser(teaserProductUserWS));
        teaserProductUserWS.getMainSubscription().setNextInvoiceDayOfPeriod(1);
        teaserProductUserWS.setNextInvoiceDate(startDate);
        api.updateUser(teaserProductUserWS);
        teaserProductUserWS = api.getUserWS(teaserProductUserWS.getId());
        assertNotNull("User created ", teaserProductUserWS);

        OrderWS order = getOrderWS(startDate, false, item.getId());
        order.setUserId(teaserProductUserWS.getUserId());
        order.setPeriod(PP_MONTHLY_PERIOD);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setProrateFlag(false);
        order = api.getOrder(api.createOrder(order, getOrderChanges(order, effectiveDate)));
        assertNotNull("Order created ",order);

        int count = 0;
        for (OrderChangeWS changes : api.getOrderChanges(order.getId())) {
            if(changes.getStatus().equals(ORDER_CHANGE_STATUS_PENDING_ID)){
                ++count;
            }
        }
        assertEquals("one pending lines will be created",1, count);
        api.deleteOrder(order.getId());
        api.deleteUser(teaserProductUserWS.getId());
    }

    @Test(priority = 13)
    public void testPlanLevelTeaserPricing(){
        PriceModelWS flatPriceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, US_DOLLAR);
        ItemDTOEx testPlanItem = createItemDTOEx(flatPriceModel);

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute(TeaserPricingStrategy.USE_ORDER_PERIOD, TeaserPricingStrategy.UseOrderPeriod.YES.name());
        priceModel.addAttribute(TeaserPricingStrategy.PERIOD, null);
        priceModel.addAttribute(TeaserPricingStrategy.FIRST_PERIOD, "10");
        priceModel.addAttribute("3", "30");
        priceModel.addAttribute("5", "40");
        priceModel.addAttribute("7", "60");
        SortedMap<Date, PriceModelWS> models = new TreeMap<Date, PriceModelWS>();
        models.put(Constants.EPOCH_DATE, priceModel);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(PP_MONTHLY_PERIOD);
        bundle1.setQuantity(BigDecimal.ONE);

        List<PlanItemWS> planItems = new ArrayList<>();
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setPrecedence(-1);
        pi1.setItemId(item.getId());
        pi1.setModels(models);
        pi1.setBundle(bundle1);
        planItems.add(pi1);
        PlanWS plan = createPlan(testPlanItem.getId(), PP_MONTHLY_PERIOD, planItems);
        Date startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        UserWS teaserPlanUserWS = PricingTestHelper.buildUser("NewUser", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        teaserPlanUserWS.setUserId(api.createUser(teaserPlanUserWS));
        teaserPlanUserWS.getMainSubscription().setNextInvoiceDayOfPeriod(1);
        teaserPlanUserWS.setNextInvoiceDate(startDate);
        api.updateUser(teaserPlanUserWS);
        teaserPlanUserWS = api.getUserWS(teaserPlanUserWS.getId());
        assertNotNull("User created ", teaserPlanUserWS);

        OrderWS order = getOrderWS(startDate, false, plan.getItemId());
        order.setUserId(teaserPlanUserWS.getUserId());
        order.setPeriod(PP_MONTHLY_PERIOD);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setProrateFlag(false);
        order = api.getOrder(api.createOrder(order, getOrderChanges(order, effectiveDate)));
        assertNotNull("Order created ",order);

        int count = 0;
        for (OrderChangeWS changes : api.getOrderChanges(order.getId())) {
            if(changes.getStatus().equals(ORDER_CHANGE_STATUS_PENDING_ID)){
                ++count;
            }
        }
        assertEquals("Thee pending lines will be created",3, count);
        api.deleteOrder(order.getId());
        api.deleteUser(teaserPlanUserWS.getId());
    }

    @Test(priority = 14)
    public void testAccountTypeLevelTeaserPricing(){
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute(TeaserPricingStrategy.USE_ORDER_PERIOD, TeaserPricingStrategy.UseOrderPeriod.YES.name());
        priceModel.addAttribute(TeaserPricingStrategy.PERIOD, null);
        priceModel.addAttribute(TeaserPricingStrategy.FIRST_PERIOD, "10");
        priceModel.addAttribute("3", "30");
        priceModel.addAttribute("5", "40");
        priceModel.addAttribute("7", "60");
        priceModel.addAttribute("10", "0");

        PlanItemWS price = new PlanItemWS();
        price.setItemId(item.getId());
        price.setPrecedence(1);
        price.getModels().put(new Date(), priceModel);
        price = api.createAccountTypePrice(PP_ACCOUNT_TYPE, price, null);

        Date startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        UserWS teaserPlanUserWS = PricingTestHelper.buildUser("NewUser", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        teaserPlanUserWS.setUserId(api.createUser(teaserPlanUserWS));
        teaserPlanUserWS.getMainSubscription().setNextInvoiceDayOfPeriod(1);
        teaserPlanUserWS.setNextInvoiceDate(startDate);
        api.updateUser(teaserPlanUserWS);
        teaserPlanUserWS = api.getUserWS(teaserPlanUserWS.getId());
        assertNotNull("User created ", teaserPlanUserWS);

        OrderWS order = getOrderWS(startDate, false, item.getId());
        order.setUserId(teaserPlanUserWS.getUserId());
        order.setPeriod(PP_MONTHLY_PERIOD);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setProrateFlag(false);
        order = api.getOrder(api.createOrder(order, getOrderChanges(order, effectiveDate)));
        assertNotNull("Order created ",order);

        int count = 0;
        for (OrderChangeWS changes : api.getOrderChanges(order.getId())) {
            if(changes.getStatus().equals(ORDER_CHANGE_STATUS_PENDING_ID)){
                ++count;
            }
        }
        assertEquals("Thee pending lines will be created",4, count);
        api.deleteAccountTypePrice(PP_ACCOUNT_TYPE, price.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(teaserPlanUserWS.getId());
    }

    @Test(priority = 15)
    public void testCustomerLevelTeaserPricing(){
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.TEASER_PRICING.name(), new BigDecimal("1.00"), US_DOLLAR);
        priceModel.addAttribute(TeaserPricingStrategy.USE_ORDER_PERIOD, TeaserPricingStrategy.UseOrderPeriod.YES.name());
        priceModel.addAttribute(TeaserPricingStrategy.PERIOD, null);
        priceModel.addAttribute(TeaserPricingStrategy.FIRST_PERIOD, "10");
        priceModel.addAttribute("3", "30");
        priceModel.addAttribute("5", "40");
        priceModel.addAttribute("7", "60");
        priceModel.addAttribute("10", "20");
        priceModel.addAttribute("15", "0");

        PlanItemWS price = new PlanItemWS();
        price.setItemId(item.getId());
        price.setPrecedence(1);
        price.getModels().put(new Date(), priceModel);

        Date startDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date effectiveDate = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        UserWS teaserPlanUserWS = PricingTestHelper.buildUser("NewUser", PP_MONTHLY_PERIOD, PP_ACCOUNT_TYPE, new ArrayList<>());
        teaserPlanUserWS.setUserId(api.createUser(teaserPlanUserWS));
        teaserPlanUserWS.getMainSubscription().setNextInvoiceDayOfPeriod(1);
        teaserPlanUserWS.setNextInvoiceDate(startDate);
        api.updateUser(teaserPlanUserWS);
        price = api.createCustomerPrice(teaserPlanUserWS.getId(), price, null);
        teaserPlanUserWS = api.getUserWS(teaserPlanUserWS.getId());
        assertNotNull("User created ", teaserPlanUserWS);

        OrderWS order = getOrderWS(startDate, false, item.getId());
        order.setUserId(teaserPlanUserWS.getUserId());
        order.setPeriod(PP_MONTHLY_PERIOD);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setProrateFlag(false);
        order = api.getOrder(api.createOrder(order, getOrderChanges(order, effectiveDate)));
        assertNotNull("Order created ",order);

        int count = 0;
        for (OrderChangeWS changes : api.getOrderChanges(order.getId())) {
            if(changes.getStatus().equals(ORDER_CHANGE_STATUS_PENDING_ID)){
                ++count;
            }
        }
        assertEquals("Thee pending lines will be created",5, count);
        api.deleteCustomerPrice(teaserPlanUserWS.getId(), price.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(teaserPlanUserWS.getId());
    }
    private PlanWS createPlan(Integer itemId,Integer periodId , List<PlanItemWS> planItems){
        PlanWS plan = new PlanWS();
        plan.setItemId(itemId);
        plan.setPeriodId(periodId);
        plan.setDescription("TestPlan"+System.currentTimeMillis());
        plan.setPlanItems(planItems);
        plan.setUsagePoolIds(new Integer[]{});
        Integer planId = api.createPlan(plan);
        return api.getPlanWS(planId);
    }

    private OrderWS getOrderWS(Date activeDate, boolean oneTime, Integer itemId) {
        OrderWS order = (oneTime) ? PricingTestHelper.buildOneTimeOrder(user.getUserId()) : PricingTestHelper.buildMonthlyOrder(user.getUserId(), PP_MONTHLY_PERIOD);
        order.setActiveSince(activeDate);
        OrderLineWS line = PricingTestHelper.buildOrderLine(itemId, 1);
        String itemPrice = item.getDefaultPrices().get(CommonConstants.EPOCH_DATE).getAttributes().get("1");
        line.setPrice(itemPrice);
        line.setAmount(itemPrice);
        order.setOrderLines(new OrderLineWS[] { line });
        return order;
    }
    
    private OrderChangeWS[] getOrderChanges(OrderWS order, Date effectiveDate) {
        OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS orderChange : orderChanges) {
            orderChange.setStartDate(effectiveDate);
        }
        return orderChanges;
    }

    private ItemDTOEx createItemDTOEx(PriceModelWS priceModel) {
        ItemDTOEx itemDTO = new ItemDTOEx();
        itemDTO.setDescription("TestItem: " + System.currentTimeMillis());
        itemDTO.setNumber("TestWS-" + System.currentTimeMillis());
        itemDTO.setTypes(new Integer[] { CAT_CALLS });
        itemDTO.setExcludedTypes(new Integer[]{});
        itemDTO.setHasDecimals(0);
        itemDTO.setGlobal(false);
        itemDTO.setDeleted(0);
        itemDTO.setEntityId(PRANCING_PONY);
        ArrayList<Integer> entities = new ArrayList<>();
        entities.add(PRANCING_PONY);
        itemDTO.setEntities(entities);
        itemDTO.addDefaultPrice(CommonConstants.EPOCH_DATE, priceModel);
        itemDTO.setId(api.createItem(itemDTO));
        return itemDTO;
    }

    @AfterClass
    public void cleanup() throws Exception {
        api.deleteItem(item.getId());
        Arrays.stream(api.getAllInvoices(user.getUserId())).forEach(api::deleteInvoice);
        Arrays.stream(api.getLastOrders(user.getUserId(), 99)).forEach(api::deleteOrder);
        api.deleteUser(user.getUserId());
    }
}
