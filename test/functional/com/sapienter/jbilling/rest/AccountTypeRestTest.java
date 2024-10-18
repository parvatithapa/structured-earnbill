package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import org.restlet.data.Language;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 12-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "AccountTypeRestTest")
public class AccountTypeRestTest extends RestTestCase{

    private static final String TESTING_ACCOUNT_TYPE_NAME = "TestingRestAccountTypeName";
    private String random = String.valueOf(new Random().nextInt(100));

    @BeforeClass
    public void setup(){
        super.setup("accounttypes");
    }

    @Test
    public void postAccountType(){

        ResponseEntity<List<AccountTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        int initialNumberOfEntities = response.getBody().size();

        ResponseEntity<AccountTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        AccountTypeWS postedAccountType = postResponse.getBody();
        ResponseEntity<AccountTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedAccountType.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AccountTypeWS.class);
        AccountTypeWS fetchedAccountType = fetchedResponse.getBody();

        assertEquals(fetchedAccountType, postedAccountType, "Account Types do not match!");

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of account types did not increased!");

        restTemplate.sendRequest(REST_URL + postedAccountType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postAccountTypeWithEmptyName(){

        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, ""), AccountTypeWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("\"errorMessages\":\"AccountTypeWS,descriptions,accountTypeWS.error.blank.name"),
                    "Response ->"+errorMsg);
        }

    }

    @Test
    public void postAccountTypeWithDuplicateName(){

        ResponseEntity<AccountTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);
        try{
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);
            fail("Test failed!");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("\"errorMessages\":\"AccountTypeWS,descriptions,accountTypeWS.error.unique.name"),
                    "Response ->"+errorMsg);
        } finally {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getAccountType(){

        ResponseEntity<AccountTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);

        ResponseEntity<AccountTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AccountTypeWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        AccountTypeWS fetchedAccountType = fetchedResponse.getBody();
        assertEquals(fetchedAccountType, postResponse.getBody(), "Account types do not match!");

        restTemplate.sendRequest(REST_URL + fetchedAccountType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getAccountTypeThatDonNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders, null, AccountTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getAllAccountTypes(){

        ResponseEntity<List<AccountTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertNotNull(response, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());

        int initialNumberOfEntities = response.getBody().size();

        ResponseEntity<AccountTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities + 1, response.getBody().size(), "Initial number of account types did not increased!");

        restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);

        assertEquals(initialNumberOfEntities, response.getBody().size(), "Current number of account types did not decreased!");
    }

    @Test
    public void deleteAccountType(){

        ResponseEntity<AccountTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);

        ResponseEntity<List<AccountTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        int currentNumberOfEntities = response.getBody().size();

        ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());

        response = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null);
        assertEquals(currentNumberOfEntities - 1, response.getBody().size(), "Current number of account types did not decreased!");
    }

    @Test
    public void deleteAccountTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteAccountTypeThatCanNotBeDeleted(){

        RestOperationsHelper userRestHelper = RestOperationsHelper.getInstance("users");

        ResponseEntity<AccountTypeWS> accountTypeResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME), AccountTypeWS.class);

        ResponseEntity<UserWS> postedResponse = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock("testRestAccountUser", accountTypeResponse.getBody().getId()), UserWS.class);

        try {
            restTemplate.sendRequest(REST_URL + accountTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
        } finally {
            restTemplate.sendRequest(userRestHelper.getFullRestUrl() + postedResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + accountTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void updateAccountType(){

        AccountTypeWS mock = RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME);

        ResponseEntity<AccountTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, mock, AccountTypeWS.class);

        AccountTypeWS updatedMock = postedResponse.getBody();
        updatedMock.setName("UpdatedTest", RestEntitiesHelper.TEST_LANGUAGE_ID);

        ResponseEntity<AccountTypeWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                postOrPutHeaders, updatedMock, AccountTypeWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<AccountTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AccountTypeWS.class);

        assertEquals(fetchedResponse.getBody(), updatedMock, "Account types do not match!");

        restTemplate.sendRequest(REST_URL + fetchedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void updateAccountTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME));
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateAccountTypeWithInvalidData(){

        AccountTypeWS mock = RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME);

        ResponseEntity<AccountTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, mock, AccountTypeWS.class);

        AccountTypeWS updatedMock = postedResponse.getBody();
        updatedMock.setName("", RestEntitiesHelper.TEST_LANGUAGE_ID);

        try {
            restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.PUT,
                    postOrPutHeaders, updatedMock, AccountTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("\"errorMessages\":\"AccountTypeWS,descriptions,accountTypeWS.error.blank.name"),
                    "Response ->"+errorMsg);
        } finally {
            restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    /**
     * C33367663,C33367664- Verify that you can create an account type with different language and different.
     */
    @Test
    public void postAccountTypeWithDifferentLanguageAndCurrency() {
        AccountTypeWS accountTypeWS = RestEntitiesHelper.buildAccountTypeMock(0, TESTING_ACCOUNT_TYPE_NAME + random);
        accountTypeWS.setLanguageId(2);
        accountTypeWS.setCurrencyId(2);
        ResponseEntity<AccountTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, accountTypeWS, AccountTypeWS.class);
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }
}
