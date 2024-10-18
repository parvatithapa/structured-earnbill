/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2015] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.payment;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 7/15/15.
 */
@Test(testName = "payment.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Integer GANDALF_PAYMENT_ID = 1;
    private static final Integer GANDALF_USER_ID = 2;
    private static final Integer GANDALF_INVOICE_ID = 3;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testGetPayment() {

        // Cross Company
        try {
            capsuleAdminApi.getPayment(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getPayment(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getPayment(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetLatestPayment() {

        // Cross Company
        try {
            capsuleAdminApi.getLatestPayment(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLatestPayment(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLatestPayment(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testGetLastPayments() {

        Integer[] lastPaymentsIds = oscorpCustomerApi.getLastPayments(GANDALF_USER_ID,10);
        assertTrue("There are at least one payment in the list of last payments: ", lastPaymentsIds.length>0);

        // Cross Company
        try {
            capsuleAdminApi.getLastPayments(GANDALF_USER_ID,10);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLastPayments(GANDALF_USER_ID,10);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLastPayments(GANDALF_USER_ID,10);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testGetLastPaymentsPage() {
        // Cross Company
        try {
            capsuleAdminApi.getLastPaymentsPage(GANDALF_USER_ID,100,0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLastPaymentsPage(GANDALF_USER_ID,100,0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLastPaymentsPage(GANDALF_USER_ID,100,0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testGetPaymentsByDate() {
        DateTime dateTime = new DateTime();
        DateTime startDate = dateTime.withYear(2000);
        DateTime endtDate = dateTime.withYear(2015);
        Integer[] paymentsIds = oscorpCustomerApi.getPaymentsByDate(GANDALF_USER_ID,startDate.toDate(),endtDate.toDate());
        assertNotNull("Payment id for user gandalf is: " + paymentsIds);

        // Cross Company
        try {
            capsuleAdminApi.getPaymentsByDate(GANDALF_PAYMENT_ID,startDate.toDate(),endtDate.toDate());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getPaymentsByDate(GANDALF_USER_ID,startDate.toDate(),endtDate.toDate());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getPaymentsByDate(GANDALF_USER_ID,startDate.toDate(),endtDate.toDate());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testGetUserPaymentInstrument() {

        PaymentWS paymentWS = oscorpCustomerApi.getUserPaymentInstrument(GANDALF_USER_ID);
        assertNotNull("Payment instruments for gandalf are: " + paymentWS.getPaymentInstruments());

        // Cross Company
        try {
            capsuleAdminApi.getUserPaymentInstrument(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserPaymentInstrument(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserPaymentInstrument(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testDeletePayment() {

        // Cross Company
        try {
            capsuleAdminApi.deletePayment(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getPayment(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getPayment(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testPayInvoice() {

        // Cross Company
        try {
            capsuleAdminApi.payInvoice(GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.payInvoice(GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.payInvoice(GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testApplyPayment() {
        PaymentWS paymentWS = new PaymentWS();
        paymentWS.setUserId(GANDALF_USER_ID);
        // Cross Company
        try {
            capsuleAdminApi.applyPayment(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.applyPayment(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.applyPayment(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testProcessPayment() {
        PaymentWS paymentWS = new PaymentWS();
        paymentWS.setUserId(GANDALF_USER_ID);

        // Cross Company
        try {
            capsuleAdminApi.processPayment(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.processPayment(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.processPayment(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testProcessPayments() {
        PaymentWS p = new PaymentWS();
        p.setUserId(GANDALF_USER_ID);
        PaymentWS[] paymentWS = new PaymentWS[]{p};

        // Cross Company
        try {
            capsuleAdminApi.processPayments(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.processPayments(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.processPayments(paymentWS,GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserPaymentsPage() {

        // Cross Company
        try {
            capsuleAdminApi.getUserPaymentsPage(GANDALF_USER_ID,100,0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserPaymentsPage(GANDALF_USER_ID,100,0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserPaymentsPage(GANDALF_USER_ID,100,0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdatePayment() {

        PaymentWS paymentWS = new PaymentWS();
        paymentWS.setUserId(GANDALF_USER_ID);

        // Cross Company
        try {
            capsuleAdminApi.updatePayment(paymentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updatePayment(paymentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updatePayment(paymentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }


    @Test
    public void testCreatePaymentLink() { //TODO: method not secured

        // Cross Company
        try {
            capsuleAdminApi.createPaymentLink(GANDALF_INVOICE_ID,GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createPaymentLink(GANDALF_INVOICE_ID,GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createPaymentLink(GANDALF_INVOICE_ID,GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testRemovePaymentLink() {

        // Cross Company
        try {
            capsuleAdminApi.removePaymentLink(GANDALF_INVOICE_ID,GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.removePaymentLink(GANDALF_INVOICE_ID,GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.removePaymentLink(GANDALF_INVOICE_ID,GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testRemoveAllPaymentLinks() {

        try {
            capsuleAdminApi.removeAllPaymentLinks(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.removeAllPaymentLinks(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.removeAllPaymentLinks(GANDALF_PAYMENT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testGetTotalRevenueByUser() {
        // Cross Company
        try {
            capsuleAdminApi.getTotalRevenueByUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getTotalRevenueByUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getTotalRevenueByUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

}
