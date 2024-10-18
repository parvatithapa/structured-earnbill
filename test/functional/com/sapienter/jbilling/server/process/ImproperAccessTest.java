package com.sapienter.jbilling.server.process;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/6/15.
 */
@Test(testName = "process.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_ID = 1;
    private static final int PRANCING_PONY_BILLING_PROCESS_ID = 12;

    @Test
    public void testCreateUpdateBillingProcessConfiguration() {
        BillingProcessConfigurationWS ws = oscorpAdminApi.getBillingProcessConfiguration();
        // Cross Company
        try {
            capsuleAdminApi.createUpdateBillingProcessConfiguration(ws);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetBillingProcess() {
        Integer processId = oscorpAdminApi.getLastBillingProcess();
        // Cross Company
        try {
            capsuleAdminApi.getBillingProcess(processId);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testCreateUpdateCommissionProcessConfiguration() {
        CommissionProcessConfigurationWS ws = new CommissionProcessConfigurationWS();
        ws.setEntityId(PRANCING_PONY_ID);
        // Cross Company
        try {
            capsuleAdminApi.createUpdateCommissionProcessConfiguration(ws);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testIsBillingRunning() {
        // Cross Company
        try {
            capsuleAdminApi.isBillingRunning(PRANCING_PONY_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetOrderProcesses() {
        // Cross Company
        try {
            capsuleAdminApi.getOrderProcesses(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetOrderProcessesByInvoice() {
        // Cross Company
        try {
            capsuleAdminApi.getOrderProcessesByInvoice(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetBillingProcessGeneratedInvoices() {
        // Cross Company
        try {
            capsuleAdminApi.getBillingProcessGeneratedInvoices(PRANCING_PONY_BILLING_PROCESS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, 1));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

}
