package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/14/15.
 */
@Test(testName = "util.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_ID = 1;

    @Test
    public void testUpdateCompany() {
        CompanyWS company = oscorpAdminApi.getCompany();
        // Cross Company
        try {
            capsuleAdminApi.updateCompany(company);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

}
