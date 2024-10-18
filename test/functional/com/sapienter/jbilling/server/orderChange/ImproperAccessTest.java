package com.sapienter.jbilling.server.orderChange;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/12/15.
 */
@Test(testName = "orderChange.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int PRANCING_PONY_ID = 1;
    private final static int PRANCING_PONY_ORDER_ID = 100;

    @Test
    public void testGetOrderChanges() {

        // Cross Company
        try {
            capsuleAdminApi.getOrderChanges(PRANCING_PONY_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrderChanges(PRANCING_PONY_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 118, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrderChanges(PRANCING_PONY_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 118, PENDUNSUS_LOGIN)));
        }
    }

}
