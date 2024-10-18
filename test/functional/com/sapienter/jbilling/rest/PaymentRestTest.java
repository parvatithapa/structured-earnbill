package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertTrue;

/**
 * @author Vojislav Stanojevikj
 * @since 01-Nov-2016.
 */
@Test(groups = {"rest"}, testName = "PaymentRestTest")
public class PaymentRestTest extends RestTestCase{

    private static final Integer PC_PAYMENT_METHOD_TEMPLATE_ID = Integer.valueOf(1);
    private Integer DUMMY_TEST_AC_ID;
    private Integer DUMMY_TEST_PAYMENT_METHOD_TYPE_ID;

    private RestOperationsHelper accountTypeRestHelper;
    private RestOperationsHelper userRestHelper;
    private RestOperationsHelper paymentMethodTypeRestHelper;

    private static final Logger logger = LoggerFactory.getLogger(PaymentRestTest.class);


    @BeforeClass
    public void setup(){
        super.setup("payments");
        RestOperationsHelper paymentMethodTemplateRestHelper = RestOperationsHelper.getInstance("paymentMethodTemplates");
        paymentMethodTypeRestHelper = RestOperationsHelper.getInstance("paymentMethodTypes");
        accountTypeRestHelper = RestOperationsHelper.getInstance("accounttypes");
        userRestHelper = RestOperationsHelper.getInstance("users");

        DUMMY_TEST_AC_ID = restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, "TestPaymentUserAccountType" + RestTestUtils.getRandomString(3)), AccountTypeWS.class)
        .getBody().getId();

        ResponseEntity<PaymentMethodTemplateWS> templateResponse = restTemplate.sendRequest(paymentMethodTemplateRestHelper.getFullRestUrl() +
                PC_PAYMENT_METHOD_TEMPLATE_ID, HttpMethod.GET, getOrDeleteHeaders, null, PaymentMethodTemplateWS.class);

        DUMMY_TEST_PAYMENT_METHOD_TYPE_ID = restTemplate.sendRequest(paymentMethodTypeRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildPaymentMethodTypeMock(templateResponse.getBody()), PaymentMethodTypeWS.class)
        .getBody().getId();
        logger.info("EARNBILL-55: DUMMY_TEST_PAYMENT_METHOD_TYPE_ID {}", DUMMY_TEST_PAYMENT_METHOD_TYPE_ID);

    }

    @AfterClass()
    public void tearDown(){
        if (null != DUMMY_TEST_PAYMENT_METHOD_TYPE_ID){
            restTemplate.sendRequest(paymentMethodTypeRestHelper.getFullRestUrl() + DUMMY_TEST_PAYMENT_METHOD_TYPE_ID, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
        if (null != DUMMY_TEST_AC_ID){
            restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl() + DUMMY_TEST_AC_ID, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void postPayment(){

        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer"+RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();
        logger.info("EARNBILL-55: post payment for customer id {}", customerId);
        logger.info("EARNBILL-55: post payment rest url {}", REST_URL);

        ResponseEntity<PaymentWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID), PaymentWS.class);

        logger.info("EARNBILL-55: payment response id for post {}", postResponse.getBody().getId());

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        PaymentWS postedPayment = postResponse.getBody();
        ResponseEntity<PaymentWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedPayment.getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, PaymentWS.class);
        PaymentWS fetchedPayment = fetchedResponse.getBody();

        RestValidationHelper.validatePayments(fetchedPayment, postedPayment);
        logger.info("EARNBILL-55: sending the post payment id for delete {}", fetchedResponse.getBody().getId());
        restTemplate.sendRequest(REST_URL + postedPayment.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postNullPayment(){

        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                    null, PaymentWS.class);
            fail("No no");
        } catch (HttpClientErrorException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void postInvalidPayment(){

        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer"+RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();
        PaymentWS invalidPayment = RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID);
        invalidPayment.setPaymentInstruments(new ArrayList<>(0));

        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                    invalidPayment, PaymentWS.class);
            fail("No no");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("PaymentWS,paymentMethodId,validation.error.apply.without.method"));
        } finally {
            restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getPayment(){

        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer" + RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();
        logger.info("EARNBILL-55: get payment for customer id {}", customerId);
        logger.info("EARNBILL-55: get payment rest url {}", REST_URL);

        ResponseEntity<PaymentWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID), PaymentWS.class);
        logger.info("EARNBILL-55: sending the payment response id for get {}", postResponse.getBody().getId());

        ResponseEntity<PaymentWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, PaymentWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        RestValidationHelper.validatePayments(fetchedResponse.getBody(), postResponse.getBody());

        logger.info("EARNBILL-55: sending the payment id for delete {}", fetchedResponse.getBody().getId());
        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getPaymentThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET,
                    getOrDeleteHeaders, null, PaymentWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deletePayment(){

        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer" + RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();
        logger.info("EARNBILL-55: delete payment for customer id {}", customerId);
        logger.info("EARNBILL-55: delete payment rest url {}", REST_URL);
        try {
            ResponseEntity<PaymentWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID), PaymentWS.class);

            logger.info("EARNBILL-55: sending the payment response id for delete {}", postResponse.getBody().getId());

            ResponseEntity<PaymentWS> deletedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            assertNotNull(deletedResponse, "Response can not be null!!");
            RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

            ResponseEntity<PaymentWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                    getOrDeleteHeaders, null, PaymentWS.class);

            logger.info("EARNBILL-55: checking the payment response id if deleted {}", fetchedResponse.getBody().getDeleted());

            assertEquals(fetchedResponse.getBody().getDeleted(), 1, "Payment not deleted!");
        } catch (Exception e) {
            logger.error("Exception occurred in deletePayment {}", e.getMessage(), e);
            e.printStackTrace();
        } finally {
            restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }

    }

    @Test
    public void deletePaymentThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updatePayment(){

        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer" + RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();

        ResponseEntity<PaymentWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID), PaymentWS.class);

        PaymentWS updatedMock = postResponse.getBody();
        updatedMock.setPaymentNotes("Updated notes");

        ResponseEntity<PaymentWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, PaymentWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<PaymentWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, PaymentWS.class);
        RestValidationHelper.validatePayments(fetchedResponse.getBody(), updatedResponse.getBody());

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);
        restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void updatePaymentThatDoNotExists(){
        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer" + RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID),
                    PaymentWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        } finally {
            restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void updatePaymentInvalid(){

        Integer customerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("PaymentCustomer" + RestTestUtils.getRandomString(3), DUMMY_TEST_AC_ID), UserWS.class)
                .getBody().getId();

        ResponseEntity<PaymentWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildPaymentMock(customerId, new Date(), DUMMY_TEST_PAYMENT_METHOD_TYPE_ID), PaymentWS.class);

        PaymentWS updatedMock = postResponse.getBody();
        updatedMock.setUserId(Integer.MAX_VALUE);

        try {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                    postOrPutHeaders, updatedMock, PaymentWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
            restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

}
