package com.sapienter.jbilling.server.orderChangeStatus;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/11/15.
 */
@Test(testName = "orderChangeStatus.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_ORDER_CHANGE_STATUS_ID = 3;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testUpdateOrderChangeStatus() {
        OrderChangeStatusWS orderChangeStatusWS = null;

        for (OrderChangeStatusWS status : oscorpAdminApi.getOrderChangeStatusesForCompany()) {
            if (status.getEntityId() != null && status.getEntityId() == 1) {
                orderChangeStatusWS = status;
                break;
            }
        }

        // Cross Company
        try {
            capsuleAdminApi.updateOrderChangeStatus(orderChangeStatusWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ORDER_CHANGE_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteOrderChangeStatus() {
        // Cross Company
        try {
            capsuleAdminApi.deleteOrderChangeStatus(PRANCING_PONY_ORDER_CHANGE_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ORDER_CHANGE_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testSaveOrderChangeStatuses() {
        OrderChangeStatusWS[] orderChangeStatusWSes = oscorpAdminApi.getOrderChangeStatusesForCompany();

        // Cross Company
        try {
            capsuleAdminApi.saveOrderChangeStatuses(orderChangeStatusWSes);
            fail("Unauthorized access");
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
