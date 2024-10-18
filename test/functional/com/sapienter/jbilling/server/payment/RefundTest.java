/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */

package com.sapienter.jbilling.server.payment;

import static com.sapienter.jbilling.server.user.WSTest.createCreditCard;
import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.LoggingValidator;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.test.JBillingLogFileReader;

/**
 * jUnit Test cases for jBilling's refund functionality
 *
 * @author Vikas Bodani
 * @since 04/01/12
 */
@Test(groups = {"web-services", "payment"}, testName = "RefundTest")
public class RefundTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static Integer CURRENCY_ID;
    private static Integer CURRENCY_GBP_ID;
    private static Integer ACCOUNT_TYPE;
    private static Integer LANGUAGE_ID;
    private static Integer ORDER_CHANGE_STATUS_APPLY;

    private final static int CC_PM_ID = 1;
    private final static int CHEQUE_PM_ID = 3;

    private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private final static String CC_MF_NUMBER = "cc.number";
    private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private final static String CC_MF_TYPE = "cc.type";

    private final static String CHEQUE_MF_BANK_NAME = "cheque.bank.name";
    private final static String CHEQUE_MF_DATE = "cheque.date";
    private final static String CHEQUE_MF_NUMBER = "cheque.number";

    private static JbillingAPI api;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        CURRENCY_ID = Constants.PRIMARY_CURRENCY_ID;
        CURRENCY_GBP_ID = Integer.valueOf(5);//Pound
        LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
        ACCOUNT_TYPE = Integer.valueOf(1);
        ORDER_CHANGE_STATUS_APPLY = getOrCreateOrderChangeStatusApply(api);
    }

    /**
     * 1. Simplest test scenario - A refund affects linked payments balance.
     */
    @Test
    public void testRefundPayment() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //make payment
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0);

        //create refund for above payment, refund amount = payment amount
        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteUser(user.getId());
    }

    /**
     * 2. A refund should bring the User's balance to its Original value before payment
     */
    public void testRefundUserBalanceUnchanged() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        user = api.getUserWS(user.getUserId());
        assertEquals(user.getOwingBalanceAsDecimal(), BigDecimal.ZERO);

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //make payment
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull("Payment should be created", payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        //check user's balance
        user = api.getUserWS(user.getUserId());
        BigDecimal userBalance = user.getOwingBalanceAsDecimal();
        assertNotNull(userBalance);
        assertTrue("User Balance should have been negetive", BigDecimal.ZERO.compareTo(userBalance) > 0);

        assertTrue(payment.getInvoiceIds().length == 0);

        //create refund for above payment, refund amount = payment amount
        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check user's balance = 0
        user = api.getUserWS(user.getUserId());
        assertNotNull(user);
        assertEquals(BigDecimal.ZERO, user.getOwingBalanceAsDecimal());

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteUser(user.getId());
    }

    /**
     * 3. A refund must link to a Payment ID (negetive)
     * because a refund is only issued against a surplus
     */
    @Test
    public void testRefundFailWhenNoPaymentLinked() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());
        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //create refund with no payment set
        try {
            createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), null, cheque);
            fail("Refund can not be created without link to Payment ID");
        } catch (SessionInternalError e) {
        }

        //cleanup
        api.deleteUser(user.getId());
    }

    /**
     * 4. Test payment balance unchanged when linked payment has zero balance and linked invoices,
     * but invoice balance increased from previous balance
     */
    @Test
    public void testRefundPaymentWithInvoiceLinked() {
        //CREATE USER
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId());
        item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE
        Integer invoiceId = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId);

        //check invoice balance greater then zero
        InvoiceWS invoice = api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        //check if an order was created
        Integer orderId = invoice.getOrders()[0];
        assertNotNull("Order should've been create", orderId);

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //MAKE PAYMENT
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO);

        //check payment balance = zero since invoice paid
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length > 0);

        //CREATE REFUND for above payment, refund amount = payment amount
        Integer refundId = null;
        try {
            createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
            fail("Cannot refund a linked payment.");
        } catch (Exception e) {
            logger.error("Error creating payment", e);
        }

        for (Integer invIds : payment.getInvoiceIds()) {
            api.removePaymentLink(invIds, paymentId);
        }

        logger.debug("Succesfully unlnked payment from Invoice");
        refundId = createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //check invoice balance is greater than zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        //invoice balance is equal to its total
        assertEquals(invoice.getBalanceAsDecimal(), invoice.getTotalAsDecimal());

        logger.debug("Invoice balance is {}", invoice.getBalance());

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteInvoice(invoiceId);
        api.deleteOrder(orderId);
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());
    }

    /**
     * Refund a payment that is linked to one invoice, paying it in full, but
     * having some balance left. Result: payment balance is Refund amount less amount used to pay invoice originally.
     * Invoice balance is equal to its total (used to be zero).
     */
    @Test
    public void testRefundWithPaymentBalance() {
        //CREATE USER
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId());
        item.setId(api.createItem(item));

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //CREATE ORDER & INVOICE
        Integer invoiceId = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId);

        //check invoice balance greater then zero
        InvoiceWS invoice = api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        //MAKE PAYMENT
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "200.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO);

        //check payment balance > zero since balance left after invoice paid
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertTrue(payment.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("100.00"), payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length > 0);

        //CREATE REFUND for above payment, refund amount = payment amount
        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //check invoice balance is greater than zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO);

        logger.debug("Invoice balance is {}", invoice.getBalance());

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteInvoice(invoiceId);
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());
    }

    /**
     * Refund a payment that is linked to many invoices, paying some partially,
     * some in full (uses the whole balance of the payment). Result: payment
     * balance remains zero. Invoice balance for each invoice = balance + amount
     * paid by the payment.
     */
    @Test
    public void testFailedRefundPaymentLinkedManyInvoices() {
        //CREATE USER
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId());
        item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE 1
        Integer invoiceId1 = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull("Invoice1 should be created", invoiceId1);

        Integer orderId1 = api.getInvoiceWS(invoiceId1).getOrders()[0];
        assertNotNull("Order1 should be created", orderId1);

        //2
        Integer invoiceId2 = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull("Invoice2 should be created", invoiceId2);

        Integer orderId2 = api.getInvoiceWS(invoiceId2).getOrders()[0];
        assertNotNull("Order2 should be created", orderId2);

        //3
        Integer invoiceId3 = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull("Invoice2 should be created", invoiceId3);

        Integer orderId3 = api.getInvoiceWS(invoiceId3).getOrders()[0];
        assertNotNull("Order3 should be created", orderId3);

        //check invoice balance greater then zero
        InvoiceWS invoice = api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        //MAKE PAYMENT
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "300.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO);

        //check payment balance = zero since invoice paid
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length == 3);

        //CREATE REFUND for above payment, refund amount = payment amount
        try {
            createPayment(api, cheque.getPaymentMethodId(), "300.00", true, user.getUserId(), paymentId, cheque);
            fail("Refund should not have succeeded");
        } catch (Exception e) {
            logger.error("Error creating payment", e);
        }

        logger.debug("Invoice balance is.. {}", invoice.getBalance());

        //cleanup
        api.deletePayment(paymentId);
        api.deleteInvoice(invoiceId1);
        api.deleteOrder(orderId1);
        api.deleteInvoice(invoiceId2);
        api.deleteOrder(orderId2);
        api.deleteInvoice(invoiceId3);
        api.deleteOrder(orderId3);
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());
    }

    /**
     * Refund a payment that is linked to many invoices, paying some partially,
     * some in full (uses the whole balance of the payment). Unlinking of the payments is done
     * before they are refunded
     */
    @Test
    public void testSuccessRefundPaymentLinkedManyInvoices() {
        //CREATE USER
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId());
        item.setId(api.createItem(item));

        //CREATE ORDER & INVOICE 1
        Integer invoiceId1 = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId1);

        //2
        Integer invoiceId2 = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId2);

        //3
        Integer invoiceId3 = createOrderAndInvoice(api, user.getUserId(), item.getId());
        assertNotNull(invoiceId3);

        //check invoice balance greater then zero
        InvoiceWS invoice = api.getLatestInvoice(user.getUserId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //MAKE PAYMENT
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "300.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check invoice balance is zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertEquals(invoice.getBalanceAsDecimal(), BigDecimal.ZERO);

        //check payment balance = zero since invoice paid
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //payment has linked invoices
        assertTrue(payment.getInvoiceIds().length == 3);

        api.removeAllPaymentLinks(paymentId);

        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "300.00", true, user.getUserId(), paymentId, cheque);

        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        //check invoice balance is greater than zero
        invoice = api.getInvoiceWS(invoice.getId());
        assertNotNull(invoice);
        assertTrue(invoice.getBalanceAsDecimal().compareTo(BigDecimal.ZERO) > 0);

        logger.debug("Invoice balance is {}", invoice.getBalance());

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteInvoice(invoiceId1);
        api.deleteInvoice(invoiceId2);
        api.deleteInvoice(invoiceId3);
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());

    }

    /*
     * Deleting a Payment that has been refunded must fail.
     */
    @Test
    public void testDeletePaymentThatHasRefund() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //make payment
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0);

        //create refund for above payment, refund amount = payment amount
        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        try {
            api.deletePayment(paymentId);
            fail("A refund can not be deleted");
        } catch (Exception e) {
            //expected
        }

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteUser(user.getId());
    }

    /**
     * A payment that has been refunded can not be updated.
     */
    @Test
    public void testUpdatePaymentThatHasRefund() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //make payment
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0);

        //create refund for above payment, refund amount = payment amount
        Integer refundId = createPayment(api, cheque.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cheque);
        logger.debug("Created refund {}", refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        try {
            payment.setAmount("150.00");
            api.updatePayment(payment);
            fail("A refunded payment can not be updated");
        } catch (Exception e) {
            //expected
        }

        //cleanup
//	    api.deletePayment(refundId);
//	    api.deletePayment(paymentId);
        api.deleteUser(user.getId());
    }

    /**
     * Cannot delete payment that has been refunded (negative)
     */
    @Test
    public void testNegativeAmountRefundPayment() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

        //make payment
        Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
        logger.debug("Created payment {}", paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //make refund payment with negative amount
        try {
            createPayment(api, cheque.getPaymentMethodId(), "-100.00", true, user.getUserId(), paymentId, cheque);
            fail("Should not be able to create a refund with negative amount");
        } catch (SessionInternalError e) {
        }

        //clean up
        api.deletePayment(paymentId);
        api.deleteUser(user.getUserId());
    }

    @Test
    public void testPaymentLoggedMessages() {
        //This test checks the log messages in following scenario:
        //1.Create User,Order and Invoice
        //2.Create manual payment that is automatically linked to an invoice
        //3.Unlink invoice from payment
        //4.Delete payment
        try {
            String callerClass = "class=\"c.s.j.server.payment.PaymentSessionBean\"";
            String apiMethod = "api=\"createPayment\"";
            String msg;

            final String LEVEL_DEBUG = "level=\"DEBUG\"";
            final String LEVEL_INFO = "level=\"INFO\"";

            ItemTypeWS itemType = buildItemType();
            itemType.setId(api.createItemCategory(itemType));
            ItemDTOEx item = buildItem(itemType.getId());
            item.setId(api.createItem(item));
            UserWS user = createUser();
            assertTrue(user.getUserId() > 0);
            //Create Order and Invoice so the Payment can be linked to the invoice
            Integer invoiceId = createOrderAndInvoice(api, user.getUserId(), item.getId());
            assertNotNull(invoiceId);

            InvoiceWS invoice = api.getLatestInvoice(user.getUserId());
            assertNotNull(invoice);
            Integer orderId = invoice.getOrders()[0];
            assertNotNull(orderId);

            PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

            JBillingLogFileReader logMonitor = new JBillingLogFileReader();
            logMonitor.setWatchPoint();
            Integer paymentId = createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);
            assertNotNull(paymentId);

            String loggedMessages = logMonitor.readLogAsString();
            msg = "Applying payment";

            LoggingValidator.validateEnhancedLog(loggedMessages, LEVEL_INFO, callerClass, apiMethod,
                    LogConstants.MODULE_PAYMENT,
                    LogConstants.STATUS_SUCCESS,
                    LogConstants.ACTION_APPLY, msg);

            msg = "Applying payment with ID: " + paymentId + ", to invoice with ID: " + invoiceId;

            LoggingValidator.validateEnhancedLog(loggedMessages, LEVEL_INFO, callerClass, apiMethod,
                    LogConstants.MODULE_PAYMENT,
                    LogConstants.STATUS_SUCCESS,
                    LogConstants.ACTION_APPLY, msg);

            PaymentWS payment = api.getPayment(paymentId);
            assertNotNull(payment);

            logMonitor.setWatchPoint();
            api.processPayment(payment, invoiceId);
            loggedMessages = logMonitor.readLogAsString();
            msg = "Processing payment with instrument: " + payment.getPaymentInstruments().get(0).getId();
            apiMethod = "api=\"processPayment\"";
            callerClass = "class=\"c.s.jbilling.server.payment.PaymentBL\"";
            LoggingValidator.validateEnhancedLog(loggedMessages, LEVEL_DEBUG, callerClass, apiMethod,
                    LogConstants.MODULE_PAYMENT,
                    LogConstants.STATUS_SUCCESS,
                    LogConstants.ACTION_EVENT, msg);

            for (Integer invIds : payment.getInvoiceIds()) {
                logMonitor.setWatchPoint();
                api.removePaymentLink(invIds, paymentId);
                String loggedText = logMonitor.readLogAsString();
                callerClass = "class=\"c.s.j.s.u.WebServicesSessionSpringBean\"";
                apiMethod = "api=\"removePaymentLink\"";
                msg = "Invoice with ID: " + invoiceId + " is unlinked from Payment with ID: " + paymentId;
                LoggingValidator.validateEnhancedLog(loggedText, LEVEL_INFO, callerClass, apiMethod,
                        LogConstants.MODULE_PAYMENT_LINK,
                        LogConstants.STATUS_SUCCESS,
                        LogConstants.ACTION_DELETE, msg);
            }

            payment.setAmount("20");

            logMonitor.setWatchPoint();
            api.updatePayment(payment);

            loggedMessages = logMonitor.readLogAsString();
            callerClass = "class=\"c.s.jbilling.server.payment.PaymentBL\"";
            apiMethod = "api=\"updatePayment\"";
            msg = "Payment with ID: " + payment.getId() + " has been successfully updated.";

            LoggingValidator.validateEnhancedLog(loggedMessages, LEVEL_INFO, callerClass, apiMethod,
                    LogConstants.MODULE_PAYMENT,
                    LogConstants.STATUS_SUCCESS,
                    LogConstants.ACTION_EVENT, msg);

            logMonitor.setWatchPoint();
            api.deletePayment(paymentId);

            loggedMessages = logMonitor.readLogAsString();

            callerClass = "class=\"c.s.jbilling.server.payment.PaymentBL\"";
            apiMethod = "api=\"deletePayment\"";
            msg = "Payment with ID: " + paymentId + " has been deleted.";

            LoggingValidator.validateEnhancedLog(loggedMessages, LEVEL_INFO, callerClass, apiMethod,
                    LogConstants.MODULE_PAYMENT,
                    LogConstants.STATUS_SUCCESS,
                    LogConstants.ACTION_DELETE, msg);

            //cleanup
            cleanup(invoiceId, orderId, item.getId(), itemType.getId(), user.getId());
        } catch (IOException ex) {
            fail("IOException while reading jbilling.log");
        }
    }

    @Test
    public void testPaymentErrorLoggedMessages() {
        try {
            JBillingLogFileReader jbLog = new JBillingLogFileReader();
            String callerClass;
            String apiMethod;
            String msg;
            String fullLog;

            final String LEVEL_ERROR = "level=\"ERROR\"";

            ItemTypeWS itemType = buildItemType();
            itemType.setId(api.createItemCategory(itemType));
            ItemDTOEx item = buildItem(itemType.getId());
            item.setId(api.createItem(item));
            UserWS user = createUser();
            //Create Order and Invoice so the Payment can be linked to the invoice
            Integer invoiceId = createOrderAndInvoice(api, user.getUserId(), item.getId());
            InvoiceWS invoice = api.getLatestInvoice(user.getUserId());
            Integer orderId = invoice.getOrders()[0];

            PaymentInformationWS cheque = createCheque("ws bank", "2232-2323-2323", new Date());

            createPayment(api, cheque.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cheque);

            try {
                jbLog.setWatchPoint();
                api.processPayment(null, null);
            } catch (SessionInternalError sie) {
                msg = "Supplied Payment is null.";
                apiMethod = "api=\"processPayment\"";
                callerClass = "class=\"c.s.j.s.u.WebServicesSessionSpringBean\"";
                fullLog = jbLog.readLogAsString();
                LoggingValidator.validateEnhancedLog(fullLog, LEVEL_ERROR, callerClass, apiMethod,
                        LogConstants.MODULE_PAYMENT,
                        LogConstants.STATUS_NOT_SUCCESS,
                        LogConstants.ACTION_PROCESS, msg);
            }

            jbLog.setWatchPoint();
            api.processPayment(null, invoiceId);
            msg = "Payment is null, requesting Payment for Invoice ID: " + invoiceId;
            apiMethod = "api=\"processPayment\"";
            callerClass = "class=\"c.s.j.s.u.WebServicesSessionSpringBean\"";
            fullLog = jbLog.readLogAsString();
            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_ERROR, callerClass, apiMethod,
                    LogConstants.MODULE_PAYMENT,
                    LogConstants.STATUS_NOT_SUCCESS,
                    LogConstants.ACTION_PROCESS, msg);

            // Create a payment
            PaymentInformationWS creditCard = createCreditCard(user.getUserName(), "4111111111111152", new Date());
            Integer paymentId2 = createPayment(api, creditCard.getPaymentMethodId(), "100.00", false, user.getUserId(), null, creditCard);
            PaymentWS payment2 = api.getPayment(paymentId2);
            PaymentAuthorizationDTOEx authInfo = api.processPayment(payment2, null);

            // Create a refund
            Integer paymentId = authInfo.getPaymentId();
            PaymentInformationWS creditCardRefund = createCreditCard(user.getUserName(), "4111111111111152", new Date());
            Integer refundPaymentId = createPayment(api, creditCardRefund.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, creditCardRefund);
            PaymentWS refundPayment = api.getPayment(refundPaymentId);

            try {
                jbLog.setWatchPoint();
                api.processPayment(refundPayment, null);
            } catch (SessionInternalError sie) {
                msg = "Either refund payment was not linked to any payment or the refund amount is in-correct";
                apiMethod = "api=\"processPayment\"";
                callerClass = "class=\"c.s.j.s.u.WebServicesSessionSpringBean\"";
                fullLog = jbLog.readLogAsString();
                LoggingValidator.validateEnhancedLog(fullLog, LEVEL_ERROR, callerClass, apiMethod,
                        LogConstants.MODULE_PAYMENT,
                        LogConstants.STATUS_NOT_SUCCESS,
                        LogConstants.ACTION_PROCESS, msg);
            }

            //cleanup
            cleanup(invoiceId, orderId, item.getId(), itemType.getId(), user.getId());
        } catch (IOException io) {
            fail("Exception thrown while trying to read Jbilling log file: " + io);
        }
    }

    @Test
    public void testPartialRefund() {
        //create user
        UserWS user = createUser();
        assertTrue(user.getUserId() > 0);
        logger.debug("User created successfully {}", user.getUserId());

        user = api.getUserWS(user.getUserId());

        // Create a payment
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("30.00"));
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(CURRENCY_GBP_ID);
        payment.setUserId(user.getUserId());

        // Add the token for this payment
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5); // dummy data, to pass validation
        PaymentInformationWS cc = createCreditCard(user.getUserName(), "4111111111111152", cal.getTime());
        payment.getPaymentInstruments().add(cc);

        // Process
        logger.debug("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
        assertNotNull("Payment result not null", authInfo);

        Integer paymentId = authInfo.getPaymentId();

        assertTrue("Payment Authorization result should be successful",
                authInfo.getResult().booleanValue());

        //remove paymentInvoices links.
        payment = api.getPayment(authInfo.getPaymentId());
        logger.debug("Balance after payment " + payment.getBalanceAsDecimal());

        // Create a refund
        PaymentWS payment2 = new PaymentWS();
        payment2.setAmount(new BigDecimal("200.00")); //Invalid amount
        payment2.setIsRefund(new Integer(1));
        payment2.setMethodId(Constants.PAYMENT_METHOD_VISA);
        payment2.setPaymentDate(Calendar.getInstance().getTime());
        payment2.setCurrencyId(CURRENCY_GBP_ID);
        payment2.setUserId(user.getUserId()); // Existing user Frank Thompson
        payment2.setPaymentId(paymentId);

        cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5); // dummy data, to pass validation
        // Add the token for this payment

        PaymentInformationWS cc2 = createCreditCard("Joe Bloggs",
                "4111111111111152", cal.getTime());
        payment2.getPaymentInstruments().add(cc2);

        // Process invalid refund
        logger.debug("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo2;
        try {
            authInfo2 = api.processPayment(payment2, null);
            fail("An exception should be thrown");
        } catch (SessionInternalError e) {
            assertEquals("There should be only one exception", 1, e.getErrorMessages().length);
            assertEquals("An exception should be thrown, the amount of the refund is greater thant the payment",
                    "PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount", e.getErrorMessages()[0]);
        }
        //now set a valid amount.
        payment2.setAmount(new BigDecimal("20.00"));
        authInfo2 = api.processPayment(payment2, null);
        assertNotNull("Payment result not null", authInfo2);

        assertTrue("Payment Authorization result should be successful",
                authInfo2.getResult().booleanValue());

        PaymentWS originalPayment = api.getPayment(paymentId);

        assertEquals("The original payments balance should have reduced to 10.00",
                BigDecimal.TEN, originalPayment.getBalanceAsDecimal());

        //cleanup
//		api.deletePayment(authInfo2.getPaymentId());//refund
//	    api.deletePayment(authInfo.getPaymentId());//original payment
        api.deleteUser(user.getId());
    }

    private ItemTypeWS buildItemType() {
        ItemTypeWS type = new ItemTypeWS();
        type.setDescription("Refund, Item Type:" + System.currentTimeMillis());
        type.setOrderLineTypeId(1);//items
        type.setAllowAssetManagement(0);//does not manage assets
        type.setOnePerCustomer(false);
        type.setOnePerOrder(false);
        return type;
    }

    private ItemDTOEx buildItem(Integer itemTypeId) {
        ItemDTOEx item = new ItemDTOEx();
        long millis = System.currentTimeMillis();
        String name = String.valueOf(millis) + new Random().nextInt(10000);
        item.setDescription("Invoice, Product:" + name);
        item.setPriceModelCompanyId(api.getCallerCompanyId());
        item.setPrice(new BigDecimal("10"));
        item.setNumber("RFN-PRD-" + name);
        item.setAssetManagementEnabled(0);
        Integer typeIds[] = new Integer[]{itemTypeId};
        item.setTypes(typeIds);
        return item;
    }

    //Helper method to create user
    private static UserWS createUser() {
        logger.debug("createUser called");
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("refund-test-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_ID);
        newUser.setMainRoleId(new Integer(5));
        newUser.setAccountTypeId(ACCOUNT_TYPE);
        newUser.setParentId(null); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(CURRENCY_ID);
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(ACCOUNT_TYPE);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(ACCOUNT_TYPE);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(ACCOUNT_TYPE);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // valid credit card must have a future expiry date to be valid for payment processing
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        newUser.getPaymentInstruments().add(
                createCreditCard(
                        "Frodo Baggins",
                        "4111111111111111",
                        expiry.getTime()));

        logger.debug("Creating user ...");
        newUser.setUserId(api.createUser(newUser));
        return updateCustomerNextInvoiceDate(newUser.getId());
    }

    private static UserWS updateCustomerNextInvoiceDate(Integer userId) {
        logger.debug("Updating Customer Next Invoice Date for user id {}", userId);
        UserWS user = api.getUserWS(userId);
        logger.debug("Old Next Invoice Date is {}", user.getNextInvoiceDate());
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        logger.debug("New Next Invoice Date is {}", nextInvoiceDate.getTime());
        api.updateUser(user);
        return api.getUserWS(userId);
    }

    //Helper method to create payment
    private Integer createPayment(JbillingAPI api, Integer paymentMethodId, String amount, boolean isRefund,
                                  Integer userId, Integer linkedPaymentId, PaymentInformationWS paymentInformationWS) {
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal(amount));
        payment.setIsRefund(isRefund ? new Integer(1) : new Integer(0));
        payment.setMethodId(paymentMethodId);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(CURRENCY_ID);
        payment.setUserId(userId);
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));
        payment.setPaymentId(linkedPaymentId);

        payment.getPaymentInstruments().add(paymentInformationWS);

        logger.debug("Creating {}", isRefund ? " refund." : " payment.");
        return api.createPayment(payment);
    }

    private PaymentInformationWS createCheque(String bankName, String chequeNumber, Date date) {
        PaymentInformationWS cheque = new PaymentInformationWS();
        cheque.setPaymentMethodTypeId(CHEQUE_PM_ID);
        cheque.setPaymentMethodId(Constants.PAYMENT_METHOD_CHEQUE);
        cheque.setProcessingOrder(new Integer(3));

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CHEQUE_MF_BANK_NAME, false, true,
                DataType.STRING, 1, bankName);
        addMetaField(metaFields, CHEQUE_MF_NUMBER, false, true,
                DataType.STRING, 2, chequeNumber);
        addMetaField(metaFields, CHEQUE_MF_DATE, false, true,
                DataType.DATE, 3, date);
        cheque.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cheque;
    }

    private void addMetaField(List<MetaFieldValueWS> metaFields,
                              String fieldName, boolean disabled, boolean mandatory,
                              DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws);
    }


    //Helper method to create order and invoice
    private static Integer createOrderAndInvoice(JbillingAPI api, Integer userId, Integer itemId) {
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(userId);
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        newOrder.setPeriod(Constants.ORDER_PERIOD_ONCE);
        newOrder.setCurrencyId(CURRENCY_ID);
        newOrder.setNotes("Lorem ipsum text.");

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2008, 1, 1);
        newOrder.setActiveSince(cal.getTime());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[1];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("100.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("100.00"));
        line.setDescription("Fist line");
        line.setItemId(itemId);
        lines[0] = line;
        newOrder.setOrderLines(lines);
        logger.debug("Creating order ... ");
        return api.createOrderAndInvoice(newOrder, OrderChangeBL.buildFromOrder(newOrder, ORDER_CHANGE_STATUS_APPLY));
    }

    private void cleanup(Integer invoiceId, Integer orderId, Integer itemId, Integer itemTypeId, Integer userId) {
        if (null != invoiceId) {
            api.deleteInvoice(invoiceId);
        }
        if (null != orderId) {
            api.deleteOrder(orderId);
        }
        if (null != itemId) {
            api.deleteItem(itemId);
        }
        if (null != itemTypeId) {
            api.deleteItemCategory(itemTypeId);
        }
        if (null != userId) {
            api.deleteUser(userId);
        }
    }

    private static Integer getOrCreateOrderChangeStatusApply(JbillingAPI api) {
        OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
        for (OrderChangeStatusWS status : statuses) {
            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status.getId();
            }
        }
        //there is no APPLY status in db so create one
        OrderChangeStatusWS apply = new OrderChangeStatusWS();
        String status1Name = "APPLY: " + System.currentTimeMillis();
        OrderChangeStatusWS status1 = new OrderChangeStatusWS();
        status1.setApplyToOrder(ApplyToOrder.YES);
        status1.setDeleted(0);
        status1.setOrder(1);
        status1.addDescription(new InternationalDescriptionWS(LANGUAGE_ID, status1Name));
        return api.createOrderChangeStatus(apply);
    }
}
