package com.sapienter.jbilling.server.discounts;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/11/15.
 */
@Test(testName = "discounts.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_DISCOUNT_ID = 100;
    public static final int MORDOR_ENTITY_ID = 1;

    @Test
    public void testCreateOrUpdateDiscount() {
        // Cross Company
        try {
            capsuleAdminApi.createOrUpdateDiscount(oscorpAdminApi.getDiscountWS(PRANCING_PONY_DISCOUNT_ID));
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_DISCOUNT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, MORDOR_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetDiscountWS() {
        // Cross Company
        try {
            capsuleAdminApi.getDiscountWS(PRANCING_PONY_DISCOUNT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_DISCOUNT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, MORDOR_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteDiscount() {
        // Cross Company
        try {
            capsuleAdminApi.deleteDiscount(PRANCING_PONY_DISCOUNT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_DISCOUNT_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, MORDOR_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
