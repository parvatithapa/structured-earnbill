package com.sapienter.jbilling.server.orderChangeType;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/11/15.
 */
@Test(testName = "orderChangeType.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_ORDER_CHANGE_TYEP_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test(enabled = false)
    public void testGetOrderChangeTypeByName() {
        // Cross Company
        try {
            capsuleAdminApi.getOrderChangeTypeByName("Default");
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ORDER_CHANGE_TYEP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetOrderChangeTypeById() {
        // Cross Company
        try {
            capsuleAdminApi.getOrderChangeTypeById(PRANCING_PONY_ORDER_CHANGE_TYEP_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ORDER_CHANGE_TYEP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testCreateUpdateOrderChangeType() {
        OrderChangeTypeWS orderChangeTypeWS = oscorpAdminApi.getOrderChangeTypeById(PRANCING_PONY_ORDER_CHANGE_TYEP_ID);

        // Cross Company
        try {
            capsuleAdminApi.createUpdateOrderChangeType(orderChangeTypeWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ORDER_CHANGE_TYEP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteOrderChangeType() {
        // Cross Company
        try {
            capsuleAdminApi.deleteOrderChangeType(PRANCING_PONY_ORDER_CHANGE_TYEP_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ORDER_CHANGE_TYEP_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
