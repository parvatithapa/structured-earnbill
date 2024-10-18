package com.sapienter.jbilling.server.partner;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.partner.PartnerWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/4/15.
 */
@Test(testName = "partner.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int PARTNER_ONE_USER_ID = 10740;
    private final static int PARTNER_ONE_ID = 10;
    private final static int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testUpdatePartner() {

        UserWS user = oscorpAdminApi.getUserWS(PARTNER_ONE_USER_ID);
        PartnerWS partner = oscorpAdminApi.getPartner(PARTNER_ONE_ID);

        // Cross Company
        try {
            capsuleAdminApi.updatePartner(user, partner);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updatePartner(user, partner);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 10740, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updatePartner(user, partner);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 10740, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testDeletePartner() {

        // Cross Company
        try {
            capsuleAdminApi.deletePartner(PARTNER_ONE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.deletePartner(PARTNER_ONE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 10740, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.deletePartner(PARTNER_ONE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 10740, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetPartner() {
        // Cross Company
        try {
            capsuleAdminApi.getPartner(PARTNER_ONE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getPartner(PARTNER_ONE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 10740, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getPartner(PARTNER_ONE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PARTNER_ONE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 10740, PENDUNSUS_LOGIN)));
        }
    }

}
