package com.sapienter.jbilling.server.pluggable;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/10/15.
 */
@Test(testName = "plan.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int PLUGGABLE_TASK_ID = 1;
    private final static int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testGetPluginWS() {
        // Cross Company
        try {
            capsuleAdminApi.getPluginWS(PLUGGABLE_TASK_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PLUGGABLE_TASK_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdatePlugin() {
        PluggableTaskWS pluginWS = oscorpCustomerApi.getPluginWS(PLUGGABLE_TASK_ID);

        // Cross Company
        try {
            capsuleAdminApi.updatePlugin(pluginWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PLUGGABLE_TASK_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeletePlugin() {
        // Cross Company
        try {
            capsuleAdminApi.deletePlugin(PLUGGABLE_TASK_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PLUGGABLE_TASK_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testRescheduleScheduledPlugin() {
        // Cross Company
        try {
            capsuleAdminApi.rescheduleScheduledPlugin(PLUGGABLE_TASK_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PLUGGABLE_TASK_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
