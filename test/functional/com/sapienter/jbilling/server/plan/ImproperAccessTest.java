package com.sapienter.jbilling.server.plan;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/10/15.
 */
@Test(testName = "plan.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private final static int PRANCING_PONY_ID = 1;
    private final static int ITEM_PRANCING_PONY = 2900;
    private static final Integer GANDALF_USER_ID = 2;
    private final static int PRANCING_PONY_PLAN_ID = 1;

    @Test
    public void testGetPlanWS() {
        // Cross Company
        try {
            capsuleAdminApi.getPlanWS(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetAllPlans() {
        // Cross Company
        try {
            capsuleAdminApi.getPlanWS(PRANCING_PONY_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdatePlan() {
        PlanWS plan = oscorpAdminApi.getPlanWS(PRANCING_PONY_PLAN_ID);
        // Cross Company
        try {
            capsuleAdminApi.updatePlan(plan);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeletePlan() {
        // Cross Company
        try {
            capsuleAdminApi.deletePlan(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testAddPlanPrice() {
        // Cross Company
        try {
            capsuleAdminApi.addPlanPrice(1, new PlanItemWS());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testIsCustomerSubscribed() {
        // Cross Company
        try {
            capsuleAdminApi.isCustomerSubscribed(1, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testIsCustomerSubscribedForDate() {
        // Cross Company
        try {
            capsuleAdminApi.isCustomerSubscribedForDate(1, 1, new Date());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetSubscribedCustomers() {
        // Cross Company
        try {
            capsuleAdminApi.getSubscribedCustomers(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetPlansBySubscriptionItem() {
        // Cross Company
        try {
            capsuleAdminApi.getPlansBySubscriptionItem(1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetPlansByAffectedItem() {
        // Cross Company
        try {
            capsuleAdminApi.getPlansByAffectedItem(ITEM_PRANCING_PONY);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetItemUsage() {
        // Cross Company
        try {
            capsuleAdminApi.getItemUsage(1, ITEM_PRANCING_PONY, 1, null, null, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testCreateCustomerPrice() {
        // Cross Company
        try {
            capsuleAdminApi.createCustomerPrice(GANDALF_USER_ID, null, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createCustomerPrice(GANDALF_USER_ID, null, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createCustomerPrice(GANDALF_USER_ID, null, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateCustomerPrice() {
        // Cross Company
        try {
            capsuleAdminApi.updateCustomerPrice(GANDALF_USER_ID,null,null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateCustomerPrice(GANDALF_USER_ID,null,null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateCustomerPrice(GANDALF_USER_ID,null,null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testDeleteCustomerPrice() {
        // Cross Company
        try {
            capsuleAdminApi.deleteCustomerPrice(GANDALF_USER_ID, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
        // Cross Customer (child user)
        try {
            frenchSpeakerApi.deleteCustomerPrice(GANDALF_USER_ID, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.deleteCustomerPrice(GANDALF_USER_ID, null);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetCustomerPrices() {
        // Cross Company
        try {
            capsuleAdminApi.getCustomerPrices(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getCustomerPrices(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getCustomerPrices(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetCustomerPrice() {
        // Cross Company
        try {
            capsuleAdminApi.getCustomerPrice(GANDALF_USER_ID,1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getCustomerPrice(GANDALF_USER_ID,1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getCustomerPrice(GANDALF_USER_ID,1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetCustomerPriceForDate() {
        // Cross Company
        try {
            capsuleAdminApi.getCustomerPriceForDate(GANDALF_USER_ID, 1, null, true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ID, MORDOR_LOGIN)));
        }
        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getCustomerPriceForDate(GANDALF_USER_ID, 1, null, true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getCustomerPriceForDate(GANDALF_USER_ID, 1, null, true);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

}
