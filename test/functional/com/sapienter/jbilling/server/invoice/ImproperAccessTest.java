package com.sapienter.jbilling.server.invoice;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * r
 * Created by Andres Canevaro on 20/07/15.
 */
@Test(testName = "invoice.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Integer GANDALF_ENTITY_ID = 1;
    private static final Integer GANDALF_USER_ID = 2;
    private static final Integer GANDALF_ORDER_ID = 1;
    private static final Integer GANDALF_INVOICE_ID = 1;
    private static final Integer GANDALF_PAYMENT_ID = 1;

    @Test
    public void testGetInvoiceWS() {
        // Cross Company
        try {
            capsuleAdminApi.getInvoiceWS(GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_INVOICE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getInvoiceWS(GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_INVOICE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getInvoiceWS(GANDALF_INVOICE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_INVOICE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testCreateInvoice() {
        String methodName = "createInvoice";

        // Cross Company
        try {
            capsuleAdminApi.createInvoice(GANDALF_USER_ID, false);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createInvoice(GANDALF_USER_ID, false);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createInvoice(GANDALF_USER_ID, false);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testCreateInvoiceFromOrder() {
        String methodName = "createInvoiceFromOrder";

        // Cross Company
        try {
            capsuleAdminApi.createInvoiceFromOrder(GANDALF_ORDER_ID, GANDALF_INVOICE_ID);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createInvoiceFromOrder(GANDALF_ORDER_ID, GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createInvoiceFromOrder(GANDALF_ORDER_ID, GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testApplyOrderToInvoice() {
        String methodName = "applyOrderToInvoice";
        InvoiceWS invoiceWS = oscorpCustomerApi.getInvoiceWS(GANDALF_INVOICE_ID);

        // Cross Company
        try {
            capsuleAdminApi.applyOrderToInvoice(GANDALF_ORDER_ID, invoiceWS);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG,GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.applyOrderToInvoice(GANDALF_ORDER_ID, invoiceWS);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.applyOrderToInvoice(GANDALF_ORDER_ID, invoiceWS);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testCreateInvoiceWithDate() {
        String methodName = "createInvoiceWithDate";

        // Cross Company
        try {
            capsuleAdminApi.createInvoiceWithDate(GANDALF_USER_ID, null, null, null, false);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createInvoiceWithDate(GANDALF_USER_ID, null, null, null, false);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createInvoiceWithDate(GANDALF_USER_ID, null, null, null, false);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testDeleteInvoice() {
        String methodName = "deleteInvoice";

        // Cross Company
        try {
            capsuleAdminApi.deleteInvoice(GANDALF_INVOICE_ID);
            fail(String.format("Cross Company validation failed for %S method, delete invoice ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.deleteInvoice(GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, delete invoice ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.deleteInvoice(GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, delete invoice ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testSaveLegacyInvoice() {
        String methodName = "saveLegacyInvoice";
        InvoiceWS gandalfInvoiceWS = oscorpCustomerApi.getInvoiceWS(GANDALF_INVOICE_ID);

        // Cross Company
        try {
            capsuleAdminApi.saveLegacyInvoice(gandalfInvoiceWS);
            fail(String.format("Cross Company validation failed for %S method, invoice ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.saveLegacyInvoice(gandalfInvoiceWS);
            fail(String.format("Cross Customer (child user) failed for %S method, invoice ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.saveLegacyInvoice(gandalfInvoiceWS);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, invoice ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testSaveLegacyPayment() {
        String methodName = "saveLegacyPayment";
        PaymentWS payment = new PaymentWS();
        payment.setUserId(GANDALF_USER_ID);

        // Cross Company
        try {
            capsuleAdminApi.saveLegacyPayment(payment);
            fail(String.format("Cross Company validation failed for %S method, ID %d should be restricted to other companies.", methodName, GANDALF_PAYMENT_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.saveLegacyPayment(payment);
            fail(String.format("Cross Customer (child user) failed for %S method, ID %d should be restricted to child users.", methodName, GANDALF_PAYMENT_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.saveLegacyPayment(payment);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, ID %d should be restricted to other users.", methodName, GANDALF_PAYMENT_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testSaveLegacyOrder() {
        String methodName = "saveLegacyOrder";
        OrderWS order = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID);

        // Cross Company
        try {
            capsuleAdminApi.saveLegacyOrder(order);
            fail(String.format("Cross Company validation failed for %S method, ID %d should be restricted to other companies.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.saveLegacyOrder(order);
            fail(String.format("Cross Customer (child user) failed for %S method, ID %d should be restricted to child users.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.saveLegacyOrder(order);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, ID %d should be restricted to other users.", methodName, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetAllInvoices() {
        // Cross Company
        try {
            capsuleAdminApi.getAllInvoices(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getAllInvoices(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getAllInvoices(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetLatestInvoice() {
        String methodName = "getLatestInvoice";

        // Cross Company
        try {
            capsuleAdminApi.getLatestInvoice(GANDALF_USER_ID);
            fail(String.format("Cross Company validation failed for %S method, last invoice from user ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLatestInvoice(GANDALF_USER_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, last invoice from user ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLatestInvoice(GANDALF_USER_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, last invoice from user ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetLastInvoices() {
        String methodName = "getLastInvoices";
        Integer INVOICE_NUMBER = 1;

        // Cross Company
        try {
            capsuleAdminApi.getLastInvoices(GANDALF_USER_ID, INVOICE_NUMBER);
            fail(String.format("Cross Company validation failed for %S method, last invoices from user ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLastInvoices(GANDALF_USER_ID, INVOICE_NUMBER);
            fail(String.format("Cross Customer (child user) failed for %S method, last invoices from user ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLastInvoices(GANDALF_USER_ID, INVOICE_NUMBER);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, last invoices from user ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserInvoicesByDate() {
        String methodName = "getUserInvoicesByDate";
        String sinceDate = "2015-07-01";
        String untilDate = "2015-07-20";

        // Cross Company
        try {
            capsuleAdminApi.getUserInvoicesByDate(GANDALF_USER_ID, sinceDate, untilDate);
            fail(String.format("Cross Company validation failed for %S method, invoices for user ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserInvoicesByDate(GANDALF_USER_ID, sinceDate, untilDate);
            fail(String.format("Cross Customer (child user) failed for %S method, invoices for user ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserInvoicesByDate(GANDALF_USER_ID, sinceDate, untilDate);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, invoices for user ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserInvoicesPage() {
        String methodName = "getUserInvoicesPage";
        Integer limit = 5;
        Integer offset = 2;

        // Cross Company
        try {
            capsuleAdminApi.getUserInvoicesPage(GANDALF_USER_ID, limit, offset);
            fail(String.format("Cross Company validation failed for %S method, invoices for user ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserInvoicesPage(GANDALF_USER_ID, limit, offset);
            fail(String.format("Cross Customer (child user) failed for %S method, invoices for user ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserInvoicesPage(GANDALF_USER_ID, limit, offset);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, invoices for user ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUnpaidInvoices() {
        String methodName = "getUnpaidInvoices";

        // Cross Company
        try {
            capsuleAdminApi.getUnpaidInvoices(GANDALF_USER_ID);
            fail(String.format("Cross Company validation failed for %S method, unpaid invoice for user ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUnpaidInvoices(GANDALF_USER_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, unpaid invoice for user ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUnpaidInvoices(GANDALF_USER_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, unpaid invoice for user ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetPaperInvoicePDF() {
        String methodName = "getPaperInvoicePDF";

        // Cross Company
        try {
            capsuleAdminApi.getPaperInvoicePDF(GANDALF_INVOICE_ID);
            fail(String.format("Cross Company validation failed for %S method, pdf invoice with ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getPaperInvoicePDF(GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (child user) failed for %S method,  pdf invoice with ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getPaperInvoicePDF(GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, pdf invoice with ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testNotifyInvoiceByEmail() {
        String methodName = "notifyInvoiceByEmail";

        // Cross Company
        try {
            capsuleAdminApi.notifyInvoiceByEmail(GANDALF_INVOICE_ID);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.notifyInvoiceByEmail(GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.notifyInvoiceByEmail(GANDALF_INVOICE_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testNotifyPaymentByEmail() {
        String methodName = "notifyPaymentByEmail";

        // Cross Company
        try {
            capsuleAdminApi.notifyPaymentByEmail(GANDALF_PAYMENT_ID);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.notifyPaymentByEmail(GANDALF_PAYMENT_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            assertTrue("Invalid error message! [Expected] " + String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN) + "[Got] " + ex.getMessage()
                    , ex.getMessage().contains(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.notifyPaymentByEmail(GANDALF_PAYMENT_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetAllInvoicesForUser() {
        String methodName = "getAllInvoicesForUser";

        // Cross Company
        try {
            capsuleAdminApi.getAllInvoicesForUser(GANDALF_USER_ID);
            fail(String.format("Cross Company validation failed for %S method, access to ID %d should be restricted to other companies.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, GANDALF_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getAllInvoicesForUser(GANDALF_USER_ID);
            fail(String.format("Cross Customer (child user) failed for %S method, access to ID %d should be restricted to child users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getAllInvoicesForUser(GANDALF_USER_ID);
            fail(String.format("Cross Customer (another user not in the hierarchy) test failed for %S method, access to ID %d should be restricted to other users.", methodName, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, GANDALF_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }
}
