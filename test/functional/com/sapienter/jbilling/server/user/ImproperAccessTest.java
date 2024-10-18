package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 7/30/15.
 */
@Test(testName = "user.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Integer GANDALF_USER_ID = 2;
    private static final Integer GANDALF_STATUS_ID = 1;
    private static final String GANDALF_USER_CODE_IDENTIFIER =  "test-code-link";
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testGetUserWS() {

        // Cross Company
        try {
            capsuleAdminApi.getUserWS(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserWS(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserWS(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateUser() {

        UserWS user = oscorpAdminApi.getUserWS(GANDALF_USER_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateUser(user);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateUser(user);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateUser(user);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testDeleteUser() {
        // Cross Company
        try {
            capsuleAdminApi.deleteUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.deleteUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.deleteUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserContactsWS() {

        // Cross Company
        try {
            capsuleAdminApi.getUserContactsWS(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserContactsWS(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserContactsWS(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUsersByStatus() {
        // Cross Company
        try {
            capsuleAdminApi.getUsersByStatus(GANDALF_STATUS_ID, true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUsersByStatus(GANDALF_STATUS_ID, true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 1, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUsersByStatus(GANDALF_STATUS_ID, true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 1, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testUpdateUserContact() {
        // Cross Company
        try {
            capsuleAdminApi.updateUserContact(GANDALF_USER_ID, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateUserContact(GANDALF_USER_ID, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateUserContact(GANDALF_USER_ID, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUsersInStatus() {

        // Cross Company
        try {
            capsuleAdminApi.getUsersInStatus(GANDALF_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUsersInStatus(GANDALF_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 1, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUsersInStatus(GANDALF_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 1, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testGetUsersNotInStatus() {

        // Cross Company
        try {
            capsuleAdminApi.getUsersNotInStatus(GANDALF_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUsersNotInStatus(GANDALF_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 1, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUsersNotInStatus(GANDALF_STATUS_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_STATUS_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 1, PENDUNSUS_LOGIN)));
        }

    }

    @Test
    public void testCreate() {

        UserWS user = oscorpAdminApi.getUserWS(GANDALF_USER_ID);

        // Cross Company
        try {
            capsuleAdminApi.create(user, null, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.create(user, null, null);;
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.create(user, null, null);;
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testCreateUserCode() {
        UserCodeWS userCode = new UserCodeWS();
        userCode.setUserId(GANDALF_USER_ID);
        userCode.setIdentifier(GANDALF_USER_CODE_IDENTIFIER);
        // Cross Company
        try {
            capsuleAdminApi.createUserCode(userCode);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createUserCode(userCode);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createUserCode(userCode);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserCodesForUser() {
        // Cross Company
        try {
            capsuleAdminApi.getUserCodesForUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserCodesForUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserCodesForUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateUserCode() {
        UserCodeWS userCode = new UserCodeWS();
        userCode.setUserId(GANDALF_USER_ID);
        // Cross Company
        try {
            capsuleAdminApi.updateUserCode(userCode);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateUserCode(userCode);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateUserCode(userCode);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test(enabled = false)//TODO: not secured yet
    public void testGetCustomersByUserCode() {

        // Cross Company
        try {
            capsuleAdminApi.getCustomersByUserCode(GANDALF_USER_CODE_IDENTIFIER);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getCustomersByUserCode(GANDALF_USER_CODE_IDENTIFIER);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getCustomersByUserCode(GANDALF_USER_CODE_IDENTIFIER);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test(enabled = false)//TODO: not secured yet
    public void testGetOrdersByUserCode() {
        // Cross Company
        try {
            capsuleAdminApi.getOrdersByUserCode(GANDALF_USER_CODE_IDENTIFIER);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_CODE_IDENTIFIER));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrdersByUserCode(GANDALF_USER_CODE_IDENTIFIER);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_CODE_IDENTIFIER));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrdersByUserCode(GANDALF_USER_CODE_IDENTIFIER);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_CODE_IDENTIFIER));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetOrdersLinkedToUser() {
        // Cross Company
        try {
            capsuleAdminApi.getOrdersLinkedToUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrdersLinkedToUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrdersLinkedToUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetCustomersLinkedToUser() { //TODO: not secured yet
        // Cross Company
        try {
            capsuleAdminApi.getCustomersLinkedToUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getCustomersLinkedToUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getCustomersLinkedToUser(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUserExistsWithId() {
        // Cross Company
        try {
            capsuleAdminApi.userExistsWithId(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.userExistsWithId(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.userExistsWithId(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

}
