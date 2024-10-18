package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.payment.PaymentInformationRestWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.SecurePaymentWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.CustomerRestWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.junit.Assert;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.fail;


/**
 * @author amey.pelapkar
 * @since 23rd JUN 2021
 */
@Test(groups = {"rest"}, testName = "UserImproperAccessRestTest")
public class UserImproperAccessRestTest extends BaseRestImproperAccessTest {

    private static final boolean ENABLED_TEST = true;
    private static final String CONTEXT_STRING = "users";

    private static final Integer USER_ID_COMPANY1_CUSTOMER1 = Integer.valueOf(2);
    private static final Integer USER_ID_COMPANY1_CUSTOMER2 = Integer.valueOf(10750);
    private static final Integer USER_ID_COMPANY1_CUSTOMER3 = Integer.valueOf(53);
    private static final Integer USER_ID_PARENT1_COMPANY3_CUSTOMER1 = Integer.valueOf(10810);
    private static final Integer USER_ID_PARENT1_COMPANY10_CUSTOMER1 = Integer.valueOf(108142);
    private static final Integer USER_ID_COMPANY2_CUSTOMER1 = Integer.valueOf(13);

    private static final Integer USER_ID_PARENT2_COMPANY11_CUSTOMER1 = Integer.valueOf(108144);

    private static PaymentInformationRestWS buildPaymentInformationWSMock(Integer userId, Integer paymentMethodTypeId, String ccNumber, String expiryDate, String intendId) {
        PaymentInformationRestWS instrument = new PaymentInformationRestWS();

        instrument.setUserId(userId);
        instrument.setProcessingOrder(1);
        instrument.setPaymentMethodId(2);
        instrument.setPaymentMethodTypeId(paymentMethodTypeId);

        Map<String, Object> metaFields = new HashMap<String, Object>();

        if (intendId == null) {
            metaFields.put("cc.cardholder.name", "test payment intrument");
            metaFields.put("cc.number", ccNumber);
            metaFields.put("cc.expiry.date", expiryDate);
        }
        if (intendId != null) {
            metaFields.put("cc.stripe.intent.id", intendId);
        }

        instrument.setMetaFields(metaFields);
        return instrument;
    }

    /**
     * Create user.
     * end point : /api/users
     * method : POST
     */
    @Override
    @Test(enabled = ENABLED_TEST)
    public void testCreate() {
        // Login as admin : Cross Company, create customer for another company
        try {
            UserWS userWS = RestEntitiesHelper.buildUserMock("test-comp-hierarchy", 1, true);
            RestEntitiesHelper.setContactDetails(1, userWS, "testCompHierarchy@gmail.com");
            userWS.setEntityId(Integer.valueOf(ENTITY_ID_COMPANY_ONE));

            createUser(company2AdminApi, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ENTITY, ENTITY_ID_COMPANY_ONE));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : create customer for another company
        try {
            UserWS userWS = RestEntitiesHelper.buildUserMock("test-comp-hierarchy", 2, true);
            RestEntitiesHelper.setContactDetails(2, userWS, "testCompHierarchy@gmail.com");
            userWS.setEntityId(Integer.valueOf(ENTITY_ID_COMPANY_TWO));

            createUser(company1Customer2Api, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ENTITY, ENTITY_ID_COMPANY_TWO));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : create customer in same company
        try {
            UserWS userWS = RestEntitiesHelper.buildUserMock("test-comp-hierarchy", 2, true);
            RestEntitiesHelper.setContactDetails(2, userWS, "testCompHierarchy@gmail.com");
            userWS.setEntityId(Integer.valueOf(ENTITY_ID_COMPANY_TWO));
            createUser(company1Customer3Api, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ENTITY, ENTITY_ID_COMPANY_ONE));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : create customer for parent company
        try {

            UserWS userWS = RestEntitiesHelper.buildUserMock("test-comp-hierarchy", 1, true);
            RestEntitiesHelper.setContactDetails(1, userWS, "testCompHierarchy@gmail.com");
            userWS.setEntityId(Integer.valueOf(ENTITY_ID_COMPANY_ONE));
            createUser(parent1Company3AdminApi, userWS);

            fail(String.format(UNAUTHORIZED_ACCESS_TO_ENTITY, ENTITY_ID_COMPANY_ONE));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }

    }

    /**
     * Get user by id.
     * end point : /api/users/{userId}
     * method : GET
     */
    @Override
    @Test(enabled = ENABLED_TEST)
    public void testRead() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getUserWS(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getUserWS(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            getUserWS(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getUserWS(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    /**
     * Update user.
     * end point : /api/users/{userId}
     * method : PUT
     */
    @Override
    @Test(enabled = ENABLED_TEST)
    public void testUpdate() {

        UserWS userWS = null;

        // Login as admin :	update customer of another company
        try {
            userWS = getUserWS(company1AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            updateUser(company2AdminApi, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer  : update customer of another company
        try {
            userWS = getUserWS(company2AdminApi, USER_ID_COMPANY2_CUSTOMER1);
            updateUser(company1Customer2Api, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer :	update customer in same company
        try {
            userWS = getUserWS(company1AdminApi, USER_ID_COMPANY1_CUSTOMER2);
            updateUser(company1Customer3Api, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        // Login as admin(child company) : update customer in parent company
        try {
            updateUser(parent1Company3AdminApi, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Override
    @Test(enabled = ENABLED_TEST)
    public void testDelete() {

        UserWS userWS = null;

        // Login as admin :	update customer of another company
        try {
            userWS = getUserWS(company1AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            deleteUser(company2AdminApi, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer  : update customer of another company
        try {
            userWS = getUserWS(company2AdminApi, USER_ID_COMPANY2_CUSTOMER1);
            deleteUser(company1Customer2Api, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer :	update customer in same company
        try {
            userWS = getUserWS(company1AdminApi, USER_ID_COMPANY1_CUSTOMER2);
            deleteUser(company1Customer3Api, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        // Login as admin(child company) : update customer in parent company
        try {
            deleteUser(parent1Company3AdminApi, userWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    /**
     * Get payment instruments for User.
     * end point : /api/users/paymentinstruments/{userId}
     * method : GET
     */
    @Test(enabled = false)
    public void testAccessPaymentInstrument() {
        // Login as admin :	get payment instruments of customer of another company
        try {
            getPaymentInstrument(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer  : get payment instruments of customer of another company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY2_CUSTOMER1, 6, "4111111111111111", "02/2030", null);

            createPaymentInstrument(company2AdminApi, instrumentWS);
            getPaymentInstrument(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer :	get payment instruments of customer in same company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER2, 1, "4111111111111111", "02/2030", null);
            createPaymentInstrument(company1AdminApi, instrumentWS);

            getPaymentInstrument(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        // Login as admin(child company) :get payment instruments of customer in parent company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER2, 1, "4111111111111111", "02/2030", null);
            createPaymentInstrument(company1AdminApi, instrumentWS);

            getPaymentInstrument(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testAddPaymentInstrument() {
        // Login as admin :	add payment instruments to customer of another company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER1, 1, "4111111111111111", "02/2030", null);
            createPaymentInstrument(company2AdminApi, instrumentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer  : add payment instruments to customer of another company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY2_CUSTOMER1, 6, "4111111111111111", "02/2030", null);
            createPaymentInstrument(company1Customer2Api, instrumentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer :	add payment instruments to customer in same company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER1, 1, "4111111111111111", "02/2030", null);
            createPaymentInstrument(company1Customer2Api, instrumentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER1, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as admin(child company) :add payment instruments of customer in parent company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER2, 1, "4111111111111111", "02/2030", null);
            createPaymentInstrument(parent1Company3AdminApi, instrumentWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = false)
    public void testRemovePaymentInstrument() {
        SecurePaymentWS securePaymentWS;

        // Login as admin :	get payment instruments of customer of another company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER1, 1, "4111111111111111", "02/2030", null);

            securePaymentWS = createPaymentInstrument(company1AdminApi, instrumentWS);

            removePaymentInstrument(company2AdminApi, securePaymentWS.getBillingHubRefId());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer  : get payment instruments of customer of another company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY2_CUSTOMER1, 6, "4111111111111111", "02/2030", null);

            securePaymentWS = createPaymentInstrument(company2AdminApi, instrumentWS);
            removePaymentInstrument(company1Customer2Api, securePaymentWS.getBillingHubRefId());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_TWO, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer :	get payment instruments of customer in same company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER2, 1, "4111111111111111", "02/2030", null);
            securePaymentWS = createPaymentInstrument(company1AdminApi, instrumentWS);

            removePaymentInstrument(company1Customer3Api, securePaymentWS.getBillingHubRefId());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        // Login as admin(child company) :get payment instruments of customer in parent company
        try {
            PaymentInformationRestWS instrumentWS = buildPaymentInformationWSMock(USER_ID_COMPANY1_CUSTOMER2, 1, "4111111111111111", "02/2030", null);
            securePaymentWS = createPaymentInstrument(company1AdminApi, instrumentWS);

            removePaymentInstrument(parent1Company3AdminApi, securePaymentWS.getBillingHubRefId());
            fail(String.format(UNAUTHORIZED_ACCESS_TO_PAYMENT_INSTRUMENT, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = false)
    public void testGetUserprofile() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getUserprofile(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(UNAUTHORIZED_INVALID_USER_ID));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getUserprofile(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(UNAUTHORIZED_INVALID_USER_ID));
        }

        // Login as customer : get customer in same company
        try {
            getUserprofile(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getUserprofile(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = false)
    public void testGetCustomerMetafields() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getCustomerMetafields(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(UNAUTHORIZED_INVALID_USER_ID));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getCustomerMetafields(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(UNAUTHORIZED_INVALID_USER_ID));
        }

        // Login as customer : get customer in same company
        try {
            getCustomerMetafields(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getCustomerMetafields(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testGetInvoices() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getInvoices(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getInvoices(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            getInvoices(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getInvoices(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testGetPayments() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getPayments(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getPayments(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            getPayments(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getPayments(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testGetCustomerAttributes() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getCustomerAttributes(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getCustomerAttributes(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            getCustomerAttributes(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getCustomerAttributes(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testGetLastInvoice() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getLastInvoice(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getLastInvoice(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            getLastInvoice(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getLastInvoice(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = false)
    public void testGetLastPayment() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getLastPayment(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString("Please enter valid userId"));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getLastPayment(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString("Please enter valid userId"));
        }

        // Login as customer : get customer in same company
        try {
            getLastPayment(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getLastPayment(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testGetUsagePools() {
        // Login as admin : Cross Company, get customer of another company
        try {
            getUsagePools(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            getUsagePools(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            getUsagePools(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            getUsagePools(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = false)
    public void testResetPassword() {
        // Login as admin : Cross Company, get customer of another company
        try {
            resetPassword(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            resetPassword(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            resetPassword(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            resetPassword(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    @Test(enabled = ENABLED_TEST)
    public void testUpdateCustomerAttributes() {
        // Login as admin : Cross Company, get customer of another company
        try {
            updateCustomerAttributes(company2AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_COMPANY2_ADMIN_MORDOR)));
        }

        // Login as customer : Cross Customer (child user), get customer of another company
        try {
            updateCustomerAttributes(company1Customer2Api, USER_ID_COMPANY2_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY2_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, 2, LOGIN_USER_COMPANY1_CUSTOMER2_FRENCH_SPEAKER)));
        }

        // Login as customer : get customer in same company
        try {
            updateCustomerAttributes(company1Customer3Api, USER_ID_COMPANY1_CUSTOMER2);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER2));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, ENTITY_ID_COMPANY_ONE, USER_ID_COMPANY1_CUSTOMER2, LOGIN_USER_COMPANY1_CUSTOMER3_PENDUNSUS)));
        }

        //   Login as admin(child company) : get customer of parent company
        try {
            updateCustomerAttributes(parent1Company3AdminApi, USER_ID_COMPANY1_CUSTOMER1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, USER_ID_COMPANY1_CUSTOMER1));
        } catch (RestClientResponseException responseError) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, responseError.getResponseBodyAsString(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, ENTITY_ID_COMPANY_ONE, LOGIN_USER_PARENT1_COMPANY3_ADMIN)));
        }
    }

    private UserWS getUserWS(RestConfig restConfig, Integer userId) {
        ResponseEntity<UserWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.toString(userId)), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, UserWS.class);
        return response.getBody();
    }

    private void createUser(RestConfig restConfig, UserWS userWS) {
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, null), HttpMethod.POST,
                getAuthHeaders(restConfig, true, true), userWS, UserWS.class);
    }

    private void updateUser(RestConfig restConfig, UserWS userWS) {
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(userWS.getId()).toString()), HttpMethod.PUT,
                getAuthHeaders(restConfig, true, true), userWS, null);
    }

    private void deleteUser(RestConfig restConfig, UserWS userWS) {
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, Integer.valueOf(userWS.getId()).toString()), HttpMethod.DELETE,
                getAuthHeaders(restConfig, true, true), userWS, null);
    }

    private PaymentInformationRestWS[] getPaymentInstrument(RestConfig restConfig, Integer userId) {
        ResponseEntity<PaymentInformationRestWS[]> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("paymentinstruments/" + Integer.toString(userId))), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, PaymentInformationRestWS[].class);
        return response.getBody();
    }

    private void removePaymentInstrument(RestConfig restConfig, Integer paymentInstrumenId) {
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("removeinstrument/" + Integer.toString(paymentInstrumenId))), HttpMethod.DELETE,
                getAuthHeaders(restConfig, true, false), null, Object.class);
    }

    private SecurePaymentWS createPaymentInstrument(RestConfig restConfig, PaymentInformationRestWS pirWS) {
        ResponseEntity<SecurePaymentWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, "addpaymentinstrument"), HttpMethod.POST,
                getAuthHeaders(restConfig, true, true), pirWS, SecurePaymentWS.class);
        return response.getBody();
    }

    private CustomerRestWS getUserprofile(RestConfig restConfig, Integer userId) {
        ResponseEntity<CustomerRestWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("userprofile/" + Integer.toString(userId))), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, CustomerRestWS.class);
        return response.getBody();
    }

    private UserWS getCustomerMetafields(RestConfig restConfig, Integer userId) {
        ResponseEntity<UserWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("getcustomermetafields/" + Integer.toString(userId))), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, UserWS.class);
        return response.getBody();
    }

    private UserWS getInvoices(RestConfig restConfig, Integer userId) {
        ResponseEntity<UserWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, (Integer.toString(userId)) + "/invoices"), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, UserWS.class);
        return response.getBody();
    }

    private void getPayments(RestConfig restConfig, Integer userId) {
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, (Integer.toString(userId)) + "/payments?limit=10&offset=10"), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, null);
    }

    private UserWS getCustomerAttributes(RestConfig restConfig, Integer userId) {
        ResponseEntity<UserWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("customerattributes/" + Integer.toString(userId))), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, UserWS.class);
        return response.getBody();
    }

    private InvoiceWS getLastInvoice(RestConfig restConfig, Integer userId) {
        ResponseEntity<InvoiceWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, (Integer.toString(userId) + "/invoices/last")), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, InvoiceWS.class);
        return response.getBody();
    }

    private PaymentWS[] getLastPayment(RestConfig restConfig, Integer userId) {
        ResponseEntity<PaymentWS[]> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, (Integer.toString(userId) + "/payments/last")), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, PaymentWS[].class);
        return response.getBody();
    }

    private UsagePoolWS getUsagePools(RestConfig restConfig, Integer userId) {
        ResponseEntity<UsagePoolWS> response = restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, (Integer.toString(userId) + "/usagepools")), HttpMethod.GET,
                getAuthHeaders(restConfig, true, false), null, UsagePoolWS.class);
        return response.getBody();
    }

    private void resetPassword(RestConfig restConfig, Integer userId) {
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("resetpassword/" + Integer.toString(userId))), HttpMethod.POST,
                getAuthHeaders(restConfig, true, true), null, null);
    }

    private void updateCustomerAttributes(RestConfig restConfig, Integer userId) {
        CustomerRestWS customerRestWS = new CustomerRestWS();
        customerRestWS.setUserId(userId);
        customerRestWS.setStatusId(1);
        customerRestWS.setAccountExpired(false);
        restTemplate.sendRequest(getFullUrl(restConfig, CONTEXT_STRING, ("customerattributes/" + Integer.toString(userId))), HttpMethod.PUT,
                getAuthHeaders(restConfig, true, true), customerRestWS, null);
    }
}