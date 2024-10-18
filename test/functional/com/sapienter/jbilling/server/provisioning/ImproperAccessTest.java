package com.sapienter.jbilling.server.provisioning;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/10/15.
 */
@Test(testName = "provisioning.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int PRANCING_PONY_ID = 1;
    private final static int PRANCING_PONY_PROVISION_COMMAND_ID = 1;
    private final static int PRANCING_PONY_PROVISION_REQUEST_ID = 1;

    @Test(enabled = false) //TODO not secured
    public void testTriggerProvisioning() {
        // Cross Company
        try {
            capsuleAdminApi.triggerProvisioning();
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateOrderAndLineProvisioningStatus() {
        // Cross Company
        try {
            capsuleAdminApi.updateOrderAndLineProvisioningStatus(1, 1, "result");
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateLineProvisioningStatus() {
        // Cross Company
        try {
            capsuleAdminApi.updateLineProvisioningStatus(1, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test(enabled = false) //TODO not secured
    public void testGetProvisioningCommands() {
        // Cross Company
        try {
            capsuleAdminApi.getProvisioningCommands(ProvisioningCommandType.ASSET, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetProvisioningCommandById() {
        // Cross Company
        try {
            capsuleAdminApi.getProvisioningCommandById(PRANCING_PONY_PROVISION_COMMAND_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_PROVISION_COMMAND_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetProvisioningRequests() {
        // Cross Company
        try {
            capsuleAdminApi.getProvisioningRequests(PRANCING_PONY_PROVISION_COMMAND_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_PROVISION_COMMAND_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetProvisioningRequestById() {
        // Cross Company
        try {
            capsuleAdminApi.getProvisioningRequestById(PRANCING_PONY_PROVISION_REQUEST_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_PROVISION_REQUEST_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }
}
