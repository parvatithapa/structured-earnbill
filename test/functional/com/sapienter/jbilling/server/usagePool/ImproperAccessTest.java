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
package com.sapienter.jbilling.server.usagePool;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

@Test(testName = "usagePool.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Logger logger = LoggerFactory.getLogger(ImproperAccessTest.class);
    private static Integer GANDALF_USAGE_POOL_ID = 1;
    private static Integer GANDALF_PLAN_ID = 9;
    private static Integer GANDALF_CUSTOMER_ID = 1;
    private static Integer GANDALF_CUSTOMER_USAGE_POOL_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testUpdateUsagePool() {
        UsagePoolWS usagePoolWS = oscorpAdminApi.getUsagePoolWS(GANDALF_USAGE_POOL_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateUsagePool(usagePoolWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USAGE_POOL_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetUsagePoolWS() {
        // Cross Company
        try {
            capsuleAdminApi.getUsagePoolWS(GANDALF_USAGE_POOL_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USAGE_POOL_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error getting the Usage Pool", ex.getMessage());
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteUsagePool() {
        // Cross Company
        try {
            capsuleAdminApi.deleteUsagePool(GANDALF_USAGE_POOL_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USAGE_POOL_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetUsagePoolsByPlanId() {
        // Cross Company
        try {
            capsuleAdminApi.getUsagePoolsByPlanId(GANDALF_PLAN_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PLAN_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetCustomerUsagePoolById() {
        // Cross Company
        try {
            capsuleAdminApi.getCustomerUsagePoolById(GANDALF_CUSTOMER_USAGE_POOL_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_CUSTOMER_USAGE_POOL_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetCustomerUsagePoolsByCustomerId() {
        // Cross Company
        try {
            capsuleAdminApi.getCustomerUsagePoolsByCustomerId(GANDALF_CUSTOMER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_CUSTOMER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
