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
package com.sapienter.jbilling.server.enumeration;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

@Test(testName = "enumeration.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Integer GANDALF_ENUMERATION_ID = 15;
    private static final String GANDALF_ENUMERATION_NAME = "Sales Type Code";
    private static final int PRANCING_PONY_ENTITY_ID = 1;

   @Test
    public void testGetEnumeration() {
        // Cross Company
        try {
            capsuleAdminApi.getEnumeration(GANDALF_ENUMERATION_ID);
            fail(String.format("Unauthorized access to ID %d", GANDALF_ENUMERATION_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test(enabled = false) //TODO: not secured
    public void testGetEnumerationByName() {
        // Cross Company
        try {
            capsuleAdminApi.getEnumerationByName(GANDALF_ENUMERATION_NAME);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ENUMERATION_NAME));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteEnumeration() {
        // Cross Company
        try {
            capsuleAdminApi.deleteEnumeration(GANDALF_ENUMERATION_ID);
            fail(String.format("Unauthorized access to ID %d", GANDALF_ENUMERATION_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
