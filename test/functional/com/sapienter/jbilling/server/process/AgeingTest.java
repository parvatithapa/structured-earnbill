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

package com.sapienter.jbilling.server.process;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sapienter.jbilling.server.order.OrderChangeBL;

import org.joda.time.DateMidnight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;


/**
 * AgeingTest
 *
 * @author Brian Cowdery
 * @since 31/05/11
 */
@Test(groups = { "integration", "ageing" }, testName = "AgeingTest", priority = 4)
public class AgeingTest {

    private static final Logger logger = LoggerFactory.getLogger(AgeingTest.class);
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;

    private final static int AGEING_STEP_PAUSE_FOR_PROVISIONING = 2000; // milliseconds

    Calendar calendar = GregorianCalendar.getInstance();

    @BeforeClass
    protected void setUp() throws Exception {
        calendar.clear();
    }

    /**
     * Test Ageing.
     *
     * Create an invoice and trigger the ageing process for various dates to simulate natural ageing of an invoice.
     *
     * Note that this test runs quickly when run standalone, but very slowly if ran after the billing process
     * when the system contains thousands of invoices to be aged.
     *
     * @throws Exception unhandled exception from API
     */
    @Test
    public void testAgeing() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        // configure billing so that the generated invoices due date will be 1 month after the invoice
        // is created. This should make it easy to determine ageing dates.
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setNextRunDate(new DateMidnight(2006, 10, 26).toDate());
        config.setDueDateUnitId(Constants.PERIOD_UNIT_MONTH);
        config.setDueDateValue(1);

        UserWS user = createUser(1, null, null);

        Integer AGEING_TEST_USER_ID = api.createUser(user);
        user.setUserId(AGEING_TEST_USER_ID);

        updateCustomerNextInvoiceDate(user.getId(), api);
        // create a new order and a invoice to be aged
        OrderWS order = new OrderWS();
        order.setUserId(AGEING_TEST_USER_ID);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2006, 10, 1).toDate());

        OrderLineWS line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Generic order line");
        line.setItemId(1);

        order.setOrderLines(new OrderLineWS[] { line });

        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);


        // create another order and a invoice not to be aged
        order = new OrderWS();
        order.setUserId(AGEING_TEST_USER_ID);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2006, 10, 1).toDate());
        order.setDueDateUnitId(PeriodUnitDTO.DAY);
        order.setDueDateValue(70);

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Generic order line");
        line.setItemId(1);

        order.setOrderLines(new OrderLineWS[] { line });

        Integer order2Id = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer invoice2Id = api.createInvoiceFromOrder(order2Id, null);

        /*
           Ageing days... The days are calculated after the invoice due date

               Ageing Day 1     = 1     send ageing notification
               Ageing Day 4     = 4     retry payment
               Ageing Day 5     = 5     send ageing notification
               Ageing Day 15    = 15    retry payment
               Ageing Day 16    = 16    send ageing notification
               Ageing Day 27    = 27    none
               Ageing Day 30    = 30    none
               Ageing Day 31    = 31    retry payment
               Ageing Day 32    = 32    none
               Ageing Day 40    = 40    retry payment
               Ageing Day 45    = 45    send ageing notification
               Ageing Day 50    = 50    retry payment
               Ageing Day 55    = 55    retry payment, send ageing notification
               Ageing Day 65    = 65    retry payment
               Ageing Day 90    = 90    retry payment
               Ageing Day 180   = 180   suspend, send ageing notification
        */
        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        Date dueDate = Util.truncateDate(invoice.getDueDate());

        calendar.clear();
        calendar.setTime(invoice.getDueDate());
        logger.debug("Due date: {}", calendar.getTime());

        AgeingStatusChecker statusChecker = new AgeingStatusChecker(api);
        Thread statusCheckingThread = new Thread(statusChecker);
        statusCheckingThread.start();

        Long start = new Date().getTime();
        api.triggerAgeing(calendar.getTime());
        Long end = new Date().getTime();

        statusChecker.stopChecking();
        logger.debug("Ageing process occupy {} ms", end - start);
        if (start + 1500 < end) { // we have not time to check status if ageing has been done very quickly
            assertTrue("Ageing has to been active", statusChecker.wasRunning);
        }

        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be active still, since it is before the due date. ", UserDTOEx.STATUS_ACTIVE, user.getStatusId());

        calendar.setTime(Util.addDays(dueDate, 1));
        logger.debug("Day 1 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 1", UserDTOEx.STATUS_ACTIVE + 1, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 2));
        logger.debug("Still Day 1 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("Still user status should be Day 1", UserDTOEx.STATUS_ACTIVE + 1, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 4));
        logger.debug("Day 4 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 4", UserDTOEx.STATUS_ACTIVE + 2, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 5));
        logger.debug("Day 5 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 5", UserDTOEx.STATUS_ACTIVE + 3, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 15));
        logger.debug("Day 15 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 15", UserDTOEx.STATUS_ACTIVE + 4, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 16));
        logger.debug("Day 16 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 16", UserDTOEx.STATUS_ACTIVE + 5, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 27));
        logger.debug("Day 27 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 27", UserDTOEx.STATUS_ACTIVE + 6, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 30));
        logger.debug("Day 30 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 30", UserDTOEx.STATUS_ACTIVE + 7, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 31));
        logger.debug("Day 31 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 31", UserDTOEx.STATUS_ACTIVE + 8, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 32));
        logger.debug("Day 32 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 32", UserDTOEx.STATUS_ACTIVE + 9, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 40));
        logger.debug("Day 40 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 40", UserDTOEx.STATUS_ACTIVE + 10, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 45));
        logger.debug("Day 45 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 45", UserDTOEx.STATUS_ACTIVE + 11, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 50));
        logger.debug("Day 50 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 50", UserDTOEx.STATUS_ACTIVE + 12, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 55));
        logger.debug("Day 55 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 55", UserDTOEx.STATUS_ACTIVE + 13, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 65));
        logger.debug("Day 65 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 65", UserDTOEx.STATUS_ACTIVE + 14, user.getStatusId().intValue());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        updateUserWithCreditCard(api, user);

        calendar.setTime(Util.addDays(dueDate, 90));
        logger.debug("Day 90 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        // wait for the payment retry to be processed
        pause(10000);
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Active, since there are no overdue invoices on today's date",
                UserDTOEx.STATUS_ACTIVE.intValue(), user.getStatusId().intValue());

        // invoice 1 should have been paid
        InvoiceWS invoice1 = api.getInvoiceWS(invoiceId);
        assertEquals("Invoice should be paid on payment retry",
                Constants.INVOICE_STATUS_PAID,
                invoice1.getStatusId());

        // invoice 2 should still remain unpaid since it's not overdue
        InvoiceWS invoice2 = api.getInvoiceWS(invoice2Id);
        assertEquals("Overdue invoice should remain 'unpaid'",
                Constants.INVOICE_STATUS_UNPAID,
                invoice2.getStatusId());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 100));
        logger.debug("Day 1 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 1, since there is 1 overdue invoice", UserDTOEx.STATUS_ACTIVE + 1,
                user.getStatusId().intValue());

        // invoice 2 should still remain unpaid since the Day1 does not do automatic payment retry
        invoice2 = api.getInvoiceWS(invoice2Id);
        assertEquals("Overdue invoice should remain 'unpaid'",
                Constants.INVOICE_STATUS_UNPAID,
                invoice2.getStatusId());

        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);

        calendar.setTime(Util.addDays(dueDate, 180));
        logger.debug("Day 4 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        // wait for the payment retry to be processed
        pause(AGEING_STEP_PAUSE_FOR_PROVISIONING);
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Active, since the overdue invoice has been paid by retry payment",
                UserDTOEx.STATUS_ACTIVE.intValue(),
                user.getStatusId().intValue());

        // invoice 2 should have been paid
        invoice2 = api.getInvoiceWS(invoice2Id);
        assertEquals("Invoice should be paid on payment retry since Day4 status does payment retry'",
                Constants.INVOICE_STATUS_PAID,
                invoice2.getStatusId());

        logger.debug("Deleting user: {}", AGEING_TEST_USER_ID);
        // remove payment info and delete the user
        api.removePaymentInstrument(user.getPaymentInstruments().iterator().next().getId());
        updateCustomerStatusToActive(AGEING_TEST_USER_ID, api);
        api.deleteUser(AGEING_TEST_USER_ID);
    }

    @Test
    public void testAgeingAutoSuspend() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        UserWS user = createUser(2, null, null);

        Integer AGEING_TEST_USER_ID = api.createUser(user);
        user.setUserId(AGEING_TEST_USER_ID);

        updateCustomerNextInvoiceDate(user.getId(), api);
        // create a new order and a invoice to be aged
        OrderWS order = new OrderWS();
        order.setUserId(AGEING_TEST_USER_ID);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(Constants.ORDER_PERIOD_ONCE);
        order.setCurrencyId(1);
        order.setActiveSince(new DateMidnight(2006, 10, 1).toDate());

        OrderLineWS line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Generic order line");
        line.setItemId(1);

        order.setOrderLines(new OrderLineWS[] { line });

        Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer invoiceId = api.createInvoiceFromOrder(orderId, null);

        InvoiceWS invoice = api.getInvoiceWS(invoiceId);
        Date dueDate = Util.truncateDate(invoice.getDueDate());

        calendar.clear();
        calendar.setTime(invoice.getDueDate());
        logger.debug("Due date: {}", calendar.getTime());

        AgeingStatusChecker statusChecker = new AgeingStatusChecker(api);
        Thread statusCheckingThread = new Thread(statusChecker);
        statusCheckingThread.start();

        Long start = new Date().getTime();
        api.triggerAgeing(calendar.getTime());
        Long end = new Date().getTime();

        statusChecker.stopChecking();
        logger.debug("Ageing process occupy {} ms", end - start);
        if (start + 1500 < end) { // we have not time to check status if ageing has been done very quickly
            assertTrue("Ageing has to been active", statusChecker.wasRunning);
        }

        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("still active", UserDTOEx.STATUS_ACTIVE, user.getStatusId());

        calendar.setTime(Util.addDays(dueDate, 1));
        logger.debug("Day 1 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 1", UserDTOEx.STATUS_ACTIVE + 1, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 2));
        logger.debug("Still Day 1 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("Still user status should be Day 1", UserDTOEx.STATUS_ACTIVE + 1, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 4));
        logger.debug("Day 4 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 4", UserDTOEx.STATUS_ACTIVE + 2, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 5));
        logger.debug("Day 5 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 5", UserDTOEx.STATUS_ACTIVE + 3, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 15));
        logger.debug("Day 15 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 15", UserDTOEx.STATUS_ACTIVE + 4, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 16));
        logger.debug("Day 16 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 16", UserDTOEx.STATUS_ACTIVE + 5, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 27));
        logger.debug("Day 27 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 27", UserDTOEx.STATUS_ACTIVE + 6, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 30));
        logger.debug("Day 30 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 30", UserDTOEx.STATUS_ACTIVE + 7, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 31));
        logger.debug("Day 31 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 31", UserDTOEx.STATUS_ACTIVE + 8, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 32));
        logger.debug("Day 32 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 32", UserDTOEx.STATUS_ACTIVE + 9, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 40));
        logger.debug("Day 40 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 40", UserDTOEx.STATUS_ACTIVE + 10, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 45));
        logger.debug("Day 45 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 45", UserDTOEx.STATUS_ACTIVE + 11, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 50));
        logger.debug("Day 50 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 50", UserDTOEx.STATUS_ACTIVE + 12, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 55));
        logger.debug("Day 55 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 55", UserDTOEx.STATUS_ACTIVE + 13, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 65));
        logger.debug("Day 65 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 65", UserDTOEx.STATUS_ACTIVE + 14, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 90));
        logger.debug("Day 90 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 90", UserDTOEx.STATUS_ACTIVE + 15, user.getStatusId().intValue());

        calendar.setTime(Util.addDays(dueDate, 180));
        logger.debug("Day 180 status on: {}", calendar.getTime());
        api.triggerAgeing(calendar.getTime());
        user = api.getUserWS(AGEING_TEST_USER_ID);
        assertEquals("User status should be Day 180", UserDTOEx.STATUS_ACTIVE + 16, user.getStatusId().intValue());
        // after step 180, the user has to be suspended and blacklisted

        updateCustomerStatusToActive(AGEING_TEST_USER_ID, api);
        api.deleteUser(AGEING_TEST_USER_ID);
    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        api.updateUser(user);
    }

    private void updateUserWithCreditCard(JbillingAPI api, UserWS user) {

        // add valid payment information for this user
        // add a credit card
    	// valid credit card must have a future expiry date to be valid for payment processing
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

    	PaymentInformationWS cc = CreateObjectUtil.createCreditCard("Peter Pan",
        		"30569309025904", expiry.getTime());

            user.getPaymentInstruments().clear();
            user.getPaymentInstruments().add(cc);

        api.updateUser(user);
    }

    @Test
    public void testAgeingProcessStatus() throws Exception {

        JbillingAPI api = JbillingAPIFactory.getAPI();

        assertFalse("No active ageing processes yet!", api.isAgeingProcessRunning());

        ProcessStatusWS status = api.getAgeingProcessStatus();
        assertNotNull("Status should be retrieved", status);
        assertEquals("Process status should be FINISHED", ProcessStatusWS.State.FINISHED, status.getState());
    }

    private class AgeingStatusChecker implements Runnable {
        protected Boolean wasRunning = false;
        protected AtomicBoolean active = new AtomicBoolean(true);
        private JbillingAPI api = null;

        public void stopChecking() {
            active.set(false);
        }
        public AgeingStatusChecker(JbillingAPI api) {
            this.api = api;
        }

        public void run() {
            while (active.get() && !wasRunning) {
                wasRunning = api.isAgeingProcessRunning();
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Helper method to create user
    private static UserWS createUser(Integer initials, Integer parentId, Integer currencyId) throws JbillingAPIException, IOException {
        logger.debug("createUser called");
        JbillingAPI api = JbillingAPIFactory.getAPI();

        /*
        * Create - This passes the password validation routine.
        */
        UserWS newUser = new UserWS();
        newUser.setUserName("ageing-test-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("As$fasdf1");
        newUser.setLanguageId(new Integer(1));
        newUser.setMainRoleId(new Integer(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        newUser.setMetaFields(new MetaFieldValueWS[]{metaField1, metaField2});

        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Peter" + initials);
        metaField4.setGroupId(1);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Pan");
        metaField5.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        return newUser;
    }

    private void pause(long t) throws InterruptedException {
        logger.debug("pausing for {} ms", t);
        Thread.sleep(t);
    }

    private UserWS updateCustomerNextInvoiceDate(Integer userId, JbillingAPI api) {
        UserWS user = api.getUserWS(userId);
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
        return api.getUserWS(userId);
    }
}
