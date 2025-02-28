/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.item;

import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.order.db.OrderChangeStatusDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.testng.AssertJUnit.*;

/**
 * @author kkulagin
 * @since 10.01.12
 */
@Test(groups = { "integration", "currency" }, testName = "CurrencyExchangeTest", priority = 1)
public class CurrencyExchangeTest {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyExchangeTest.class);
    private static final Integer CURRENCY_USD = 1;
    private static final Integer PRANCING_PONY = 1;
    private static final Integer CURRENCY_AUD = 11;

    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
    private static Integer TEST_USER_ID;

    private static JbillingAPI api;

	@BeforeClass
	public void initializeTests() throws IOException, JbillingAPIException {
		if (null == api) {
			api = JbillingAPIFactory.getAPI();
		}
		// Create And Persist User
		UserWS customer = null;
		try {
			customer = com.sapienter.jbilling.server.user.WSTest
					.createUser(true, true, null, CURRENCY_USD, true);
		} catch (Exception e) {
			fail("Error creating customer!!!");
		}
		TEST_USER_ID = customer.getId();
		ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeApplyStatus(api);
	}

	@AfterClass
	public void tearDown() {

		if (null != TEST_USER_ID) {
			api.deleteUser(TEST_USER_ID);
			TEST_USER_ID = null;
		}

		if (null != api) {
			api = null;
		}
	}

    /**
     * should remove record for specified date with null amount
     */
    @Test
    public void testRecordRemoving() {

        final CurrencyWS[] currencies = api.getCurrencies();
        final CurrencyWS audCurrency = getCurrencyById(CURRENCY_AUD, currencies);
        final BigDecimal rate = new BigDecimal("999999.0");
        audCurrency.setRate(rate);
        audCurrency.setFromDate(new Date());
        audCurrency.setSysRateAsDecimal(audCurrency.getSysRateAsDecimal().setScale(4));
        // this should add new currency exchange record which current date
        api.updateCurrency(audCurrency);

        final Integer currency1Id = audCurrency.getId();

        final CurrencyWS[] currenciesAfterRateUpdate = api.getCurrencies();
        final CurrencyWS currency1AfterRateUpdate = getCurrencyById(currency1Id, currenciesAfterRateUpdate);
        // check that current rate has a correct value
        assertEquals(rate.compareTo(currency1AfterRateUpdate.getRateAsDecimal()), 0);

        currency1AfterRateUpdate.setFromDate(new Date());
        currency1AfterRateUpdate.setRate((String) null);
        currency1AfterRateUpdate.setSysRateAsDecimal(currency1AfterRateUpdate.getSysRateAsDecimal().setScale(4));
        // this should remove currency exchange record
        api.updateCurrency(currency1AfterRateUpdate);

        final CurrencyWS[] currenciesAfterRemove = api.getCurrencies();
        final CurrencyWS currency1AfterRemove = getCurrencyById(currency1Id, currenciesAfterRemove);
        assertNotSame(currency1AfterRemove.getRateAsDecimal(), rate);
    }


    /**
     * This will create 2 different exchange rates for AUD for 2 different dates.
     * After that create an Item and an Order with this Item.
     * Depending on order's ActiveSince property price should vary correspondingly
     */
    @Test
    public void testRecordSomething() {

        final CurrencyWS[] currencies = api.getCurrencies();
        final CurrencyWS audCurrency = getCurrencyById(CURRENCY_AUD, currencies);

        Date date1 = newDate(2100, 3, 1);
        Date date2 = newDate(2100, 4, 15);

        BigDecimal firstDateExchangeRate = new BigDecimal("10.0");
        BigDecimal secondDateExchangeRate = new BigDecimal("100.0");
        BigDecimal itemPrice = new BigDecimal("1.0");

        try {
            audCurrency.setRate(firstDateExchangeRate);
            audCurrency.setFromDate(date1);
            audCurrency.setSysRateAsDecimal(audCurrency.getSysRateAsDecimal().setScale(4));
            api.updateCurrency(audCurrency);

            audCurrency.setRate(secondDateExchangeRate);
            audCurrency.setFromDate(date2);
            audCurrency.setSysRate(secondDateExchangeRate);
            api.updateCurrency(audCurrency);

            Integer itemTypeId = null;
            Integer itemId = null;

            try {

                // Create Test Item Type
                ItemTypeWS itemType = new ItemTypeWS();
                itemType.setDescription("TestCategoryCurrencyExchange: " + System.currentTimeMillis());
                itemType.setEntityId(PRANCING_PONY);
                itemType.setEntities(new ArrayList<Integer>(PRANCING_PONY));
                itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);

                // Persist
                itemTypeId = api.createItemCategory(itemType);

                ItemDTOEx item = new ItemDTOEx();
                item.setCurrencyId(CURRENCY_USD);
                item.setPrice(itemPrice);
                item.setDescription("Test Item for Currency Exchange");
                item.setEntityId(PRANCING_PONY);
                item.setNumber("Number");
                item.setTypes(new Integer[]{itemTypeId});
                itemId = api.createItem(item);
                item.setId(itemId);


                assertNotNull("item created", item.getId());

                Integer orderId = null;
                try {
                    OrderWS order = new OrderWS();
                    order.setUserId(TEST_USER_ID);
                    order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
                    order.setPeriod(1);
                    order.setCurrencyId(CURRENCY_AUD);
                    order.setActiveSince(date1);

                    OrderLineWS line = new OrderLineWS();
                    line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
                    line.setItemId(itemId);
                    line.setUseItem(true);
                    line.setQuantity(1);
                    order.setOrderLines(new OrderLineWS[]{line});

	                OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
	                for(OrderChangeWS change : changes) change.setStartDate(order.getActiveSince());

                    orderId = api.createOrder(order, changes);
                    order.setId(orderId); // create order

                    final OrderWS result = api.rateOrder(order, changes);

                    final OrderLineWS[] orderLines = result.getOrderLines();
                    final BigDecimal amount = orderLines[0].getAmountAsDecimal();
                    assertEquals(amount.compareTo(firstDateExchangeRate.multiply(itemPrice)), 0);

                } finally {
                    if (orderId != null) {
                        try {
                            api.deleteOrder(orderId);
                        } catch (Throwable e) {
	                        logger.error("Error trying to delete order", e);
	                        fail("Failed to delete the order with id:" + orderId);
                        }
                    }
                }

                orderId = null;
                try {
                    OrderWS order = new OrderWS();
                    order.setUserId(TEST_USER_ID);
                    order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
                    order.setPeriod(1);
                    order.setCurrencyId(CURRENCY_AUD);
                    order.setActiveSince(date2);

                    OrderLineWS line = new OrderLineWS();
                    line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
                    line.setItemId(itemId);
                    line.setUseItem(true);
                    line.setQuantity(1);
                    order.setOrderLines(new OrderLineWS[]{line});

	                OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
	                for(OrderChangeWS change : changes) change.setStartDate(order.getActiveSince());

                    orderId = api.createOrder(order, changes);
                    order.setId(orderId); // create order

                    final OrderWS result = api.rateOrder(order, changes);

                    final OrderLineWS[] orderLines = result.getOrderLines();
                    final BigDecimal amount = orderLines[0].getAmountAsDecimal();
                    assertEquals(amount.compareTo(secondDateExchangeRate.multiply(itemPrice)), 0);
                } finally {
                    if (orderId != null) {
                        try {
                            api.deleteOrder(orderId);
                        } catch (Throwable e) {
	                        logger.error("Error trying to delete the order", e);
	                        fail("Failed to delete the order with id:" + orderId);
                        }
                    }
                }


            } finally {
                if (itemId != null) {
                    try {
                        api.deleteItem(itemId);
                    } catch (Throwable e) {
                        logger.error("Error trying to delete item", e);
	                    fail("Failed to delete the item with id:" + itemId);
                    }
                }
                if(null != itemTypeId){
                    api.deleteItemCategory(itemTypeId);
                }
            }
        } finally {
            // clear currency exchange records
            audCurrency.setRate((String) null);
            audCurrency.setFromDate(date1);
            audCurrency.setSysRateAsDecimal(audCurrency.getSysRateAsDecimal().setScale(4));
            api.updateCurrency(audCurrency);

            audCurrency.setRate((String) null);
            audCurrency.setFromDate(date2);
            api.updateCurrency(audCurrency);

        }

    }

    private static CurrencyWS getCurrencyById(Integer currencyId, CurrencyWS[] currencies) {
        for (CurrencyWS currency : currencies) {
            if (currencyId.equals(currency.getId())) {
                return currency;
            }
        }
        throw new IllegalStateException("Currency with id = " + currencyId + " not found.");
    }

	private static Date newDate(int year, int month, int day) {
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month-1);
		calendar.set(Calendar.DAY_OF_MONTH, day);
		return calendar.getTime();
	}

	public static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api) {
		OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
		Integer statusId = null;
		for (OrderChangeStatusWS orderChangeStatus : list) {
			if (orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
				statusId = orderChangeStatus.getId();
				break;
			}
		}
		if (statusId != null) {
			return statusId;
		} else {
			OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
			newStatus.setApplyToOrder(ApplyToOrder.YES);
			newStatus.setDeleted(0);
			newStatus.setOrder(1);
			newStatus.addDescription(new InternationalDescriptionWS(
					com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
			return api.createOrderChangeStatus(newStatus);
		}
	}
}
