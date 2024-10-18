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
package com.sapienter.jbilling.server.accountType;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

@Test(testName = "accountType.WSTest")
public class ImproperAccessTest extends BaseImproperAccessTest {
    private static final Logger logger = LoggerFactory.getLogger(ImproperAccessTest.class);

    private static final Integer GANDALF_ACCOUNT_TYPE_ID = 1;
    private static final Integer GANDALF_ACCOUNT_INFORMATION_TYPE_ID = 1;
    private static final Integer GANDALF_ORDER_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    // ACCOUNT TYPE

    @Test
    public void testUpdateAccountType() {
        AccountTypeWS accountType = oscorpCustomerApi.getAccountType(GANDALF_ACCOUNT_TYPE_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateAccountType(accountType);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error updating the Account Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteAccountType() {
        // Cross Company
        try {
            capsuleAdminApi.deleteAccountType(GANDALF_ACCOUNT_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error deleting the Account Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAccountType() {
        // Cross Company
        try {
            capsuleAdminApi.getAccountType(GANDALF_ACCOUNT_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error getting the Account Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    // ACCOUNT INFORMATION TYPE

    @Test
    public void testGetInformationTypesForAccountType() {
        // Cross Company
        try {
            capsuleAdminApi.getInformationTypesForAccountType(GANDALF_ACCOUNT_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error getting the Information Types for Account Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateAccountInformationType() {
        AccountInformationTypeWS accountInformationType = oscorpCustomerApi.getAccountInformationType(GANDALF_ACCOUNT_INFORMATION_TYPE_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateAccountInformationType(accountInformationType);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error updating the Information Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteAccountInformationType() {
        // Cross Company
        try {
            capsuleAdminApi.deleteAccountInformationType(GANDALF_ACCOUNT_INFORMATION_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error deleting the Account Information Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAccountInformationType() {
        // Cross Company
        try {
            capsuleAdminApi.getAccountInformationType(GANDALF_ACCOUNT_INFORMATION_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error getting the Account Information Type", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetLinkedOrders() {
        // Cross Company
        try {
            capsuleAdminApi.getLinkedOrders(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ACCOUNT_TYPE_ID));
        } catch (SecurityException | SessionInternalError ex) {
            logger.error("Error getting the linked Orders", ex);
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
