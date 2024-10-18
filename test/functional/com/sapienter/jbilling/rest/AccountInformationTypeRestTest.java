package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * @author Vojislav Stanojevikj
 * @since 17-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "AccountInformationTypeRestTest")
public class AccountInformationTypeRestTest extends RestTestCase{

    private Integer DUMMY_TEST_AC_ID;

    @BeforeClass
    public void setup(){
        super.setup("accounttypes");

        ResponseEntity<AccountTypeWS> postResponse = restTemplate.sendRequest(restHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, "TestRestAccountType"), AccountTypeWS.class);
        DUMMY_TEST_AC_ID = postResponse.getBody().getId();
        REST_URL = REST_URL + DUMMY_TEST_AC_ID + "/aits/";
    }

    @AfterClass()
    public void tearDown(){
        if (null != DUMMY_TEST_AC_ID){
            restTemplate.sendRequest(restHelper.getFullRestUrl() + DUMMY_TEST_AC_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void postAccountInformationType(){

        ResponseEntity<AccountInformationTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        AccountInformationTypeWS postedAIT = postResponse.getBody();
        ResponseEntity<AccountInformationTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedAIT.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AccountInformationTypeWS.class);
        AccountInformationTypeWS fetchedAIT = fetchedResponse.getBody();

        assertEquals(postedAIT, fetchedAIT, "AITs do not match!");

        ResponseEntity<List<AccountInformationTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertEquals(Integer.valueOf(response.getBody().size()), Integer.valueOf(1), "Persisted number of AITs did not increased!");
        restTemplate.sendRequest(REST_URL + postedAIT.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postAccountInformationTypeForNonExistingAccountType(){

        try {
            restTemplate.sendRequest(restHelper.getFullRestUrl() + Integer.MAX_VALUE + "/aits", HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
            assertEquals(errorMsg, "Account type param id invalid!");
        }
    }

    @Test
    public void postInvalidAccountInformationType(){

        AccountInformationTypeWS ait = RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact");
        ait.setMetaFields(new MetaFieldWS[]{RestEntitiesHelper.buildMetaField(Integer.valueOf(0), EntityType.ACCOUNT_INFORMATION_TYPE, DataType.SCRIPT, "")});
        try {
            restTemplate.sendRequest(restHelper.getFullRestUrl() + Integer.MAX_VALUE + "/aits", HttpMethod.POST,
                    postOrPutHeaders, ait, AccountInformationTypeWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
            assertEquals(errorMsg, "Account type param id invalid!");
        }
    }

    @Test
    public void getAccountInformationType(){

        ResponseEntity<AccountInformationTypeWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);

        ResponseEntity<AccountInformationTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                HttpMethod.GET, getOrDeleteHeaders, null, AccountInformationTypeWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        AccountInformationTypeWS fetchedAccountType = fetchedResponse.getBody();
        assertEquals(postResponse.getBody(), fetchedAccountType, "AITs do not match!");

        restTemplate.sendRequest(REST_URL + fetchedAccountType.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getAccountInformationTypeThatDoNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.GET, getOrDeleteHeaders, null, AccountInformationTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getAllAccountInformationTypes(){

        ResponseEntity<AccountInformationTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);

        ResponseEntity<List<AccountInformationTypeWS>> response = restTemplate.sendRequest(REST_URL, HttpMethod.GET,
                getOrDeleteHeaders, null);

        assertNotNull(response, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(response, Response.Status.OK.getStatusCode());

        assertEquals(Integer.valueOf(response.getBody().size()), Integer.valueOf(1), "The number of AITs did not increased!");

        restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getAllAccountInformationTypesEmpty(){

        ResponseEntity<AccountInformationTypeWS[]> aitsResponse = restTemplate.sendRequest(REST_URL, HttpMethod.GET, getOrDeleteHeaders, null, AccountInformationTypeWS[].class);
        assertNotNull(aitsResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(aitsResponse, Response.Status.OK.getStatusCode());
        AccountInformationTypeWS[] aits = aitsResponse.getBody();
        assertNotNull(aits);
        assertEquals(aits.length, 0);
    }

    @Test
    public void deleteAccountInformationType(){

        ResponseEntity<AccountInformationTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);

        ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    public void deleteAccountInformationThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteAccountInformationTypeForNonExistingAccountType(){

        try {
            restTemplate.sendRequest(restHelper.getFullRestUrl() + Integer.MAX_VALUE + "/aits/" + Integer.MAX_VALUE, HttpMethod.DELETE,
                    getOrDeleteHeaders, AccountInformationTypeWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteAccountTypeThatCanNotBeDeleted(){

        AccountInformationTypeWS accountInformationType = RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact");
        accountInformationType.setUseForNotifications(true);

        ResponseEntity<AccountInformationTypeWS> accountTypeResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, accountInformationType, AccountInformationTypeWS.class);

        try {
            restTemplate.sendRequest(REST_URL + accountTypeResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
        } finally {
            clearPreferredNotificationAITId(accountTypeResponse);
        }
    }

    @Test
    public void updateAccountInformationType(){

        AccountInformationTypeWS updatedMock = RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact");
        ResponseEntity<AccountInformationTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, updatedMock, AccountInformationTypeWS.class);

        updatedMock = postedResponse.getBody();
        updatedMock.setUseForNotifications(true);
        ResponseEntity<AccountInformationTypeWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(),
                HttpMethod.PUT, postOrPutHeaders, updatedMock, AccountInformationTypeWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<AccountInformationTypeWS> fetchedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, AccountInformationTypeWS.class);

        updatedMock.setDateCreated(fetchedResponse.getBody().getDateCreated());
        assertEquals(fetchedResponse.getBody(), updatedMock, "AITs do not match!");
        clearPreferredNotificationAITId(updatedResponse);
    }

    @Test
    public void updateAccountInformationTypeThatDoNotExists(){
        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"));
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void updateAccountInformationTypeWithInvalidData(){

        ResponseEntity<AccountInformationTypeWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);

        AccountInformationTypeWS updatedMock = postedResponse.getBody();
        updatedMock.setMetaFields(new MetaFieldWS[]{RestEntitiesHelper.buildMetaField(Integer.valueOf(0), EntityType.ACCOUNT_INFORMATION_TYPE, DataType.SCRIPT, "")});

        try {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.PUT,
                    postOrPutHeaders, updatedMock, AccountInformationTypeWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("\"errorMessages\":\"AccountInformationTypeWS,metaFields,metafield.validation.filename.required"),
                    "Response ->"+errorMsg);
        } finally {
            restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void updateAccountInformationTypeForNonExistingAccountType(){

        try {
            restTemplate.sendRequest(restHelper.getFullRestUrl() + Integer.MAX_VALUE + "/aits/" + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact"), AccountInformationTypeWS.class);
            fail("Test failed!");
        } catch (HttpClientErrorException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    private void clearPreferredNotificationAITId(ResponseEntity<AccountInformationTypeWS> accountInformationTypeResponse) {
	 AccountInformationTypeWS updatedMock = accountInformationTypeResponse.getBody();
         updatedMock.setUseForNotifications(false);
         ResponseEntity<AccountInformationTypeWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(),
                 HttpMethod.PUT, postOrPutHeaders, updatedMock, AccountInformationTypeWS.class);
         restTemplate.sendRequest(REST_URL + updatedResponse.getBody().getId(), HttpMethod.DELETE,
                 getOrDeleteHeaders, null);
    }

}
