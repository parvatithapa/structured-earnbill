/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
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

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.ratingUnit;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

@Test(testName = "ratingUnit.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Integer GANDALF_RATING_UNIT_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testUpdateRatingUnit() {
        RatingUnitWS ratingUnit = oscorpCustomerApi.getRatingUnit(GANDALF_RATING_UNIT_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateRatingUnit(ratingUnit);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_RATING_UNIT_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteRatingUnit() {
        // Cross Company
        try {
            capsuleAdminApi.deleteRatingUnit(GANDALF_RATING_UNIT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_RATING_UNIT_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetRatingUnit() {
        // Cross Company
        try {
            capsuleAdminApi.getRatingUnit(GANDALF_RATING_UNIT_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_RATING_UNIT_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
