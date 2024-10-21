package com.sapienter.jbilling.rest;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.client.util.Constants;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.ContactInformationWS;
import com.sapienter.jbilling.server.user.CustomerRestWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.PreferenceWS;

/**
 * @author Vojislav Stanojevikj
 * @since 18-Oct-2016.
 */
@Test(groups = {"rest"}, testName = "UserRestTest")
public class UserRestTest extends RestTestCase{

    private static final String TEST_CUSTOMER = "TestRestCustomer";
    private static final Integer GANDALF_ID = Integer.valueOf(2);
    private String random = String.valueOf(new Random().nextInt(1000));

    private static final String TEST_CUSTOMER_LEVEL_MF = "Account PIN";
    private static final String TEST_CUSTOMER_LEVEL_MF_VALUE = "test-account-PIN";
    private static final String STATUS_URL = "/status/in/1/";
    private static final String TEST_CUSTOMER_LEVEL_MF_EMAIL = "test.email";

    private RestOperationsHelper accountTypeRestHelper;
    private RestOperationsHelper preferenceRestHelper;
    private RestOperationsHelper userRestHelper;
    private Integer DUMMY_TEST_AC_ID;
    private static final Integer PREF_TYPE_ID = Integer.valueOf(105);

    @BeforeClass
    public void setup(){
        super.setup("users");
        accountTypeRestHelper = RestOperationsHelper.getInstance("accounttypes");
        preferenceRestHelper = RestOperationsHelper.getInstance("preferences");
        userRestHelper = RestOperationsHelper.getInstance("users");
        DUMMY_TEST_AC_ID = restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders, RestEntitiesHelper.buildAccountTypeMock(0, "TestRestUserAccountType" + random), AccountTypeWS.class).getBody().getId();
    }

    @AfterClass()
    public void tearDown(){
    // ToDo this delete will not be possible do to foreign key constraint in updateUser test
//        if (null != DUMMY_TEST_AC_ID){
//            restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl() + DUMMY_TEST_AC_ID, HttpMethod.DELETE,
//                    getOrDeleteHeaders, null);
//        }
    }

    @Test
    public void postUser(){

        ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER , DUMMY_TEST_AC_ID, true), UserWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        UserWS postedUser = postResponse.getBody();
        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedUser.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class);
        UserWS fetchedUser = fetchedResponse.getBody();

        assertEquals(postedUser, fetchedUser, "Users do not match!");

        restTemplate.sendRequest(REST_URL + postedUser.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void postUserWithNullAccountType(){
        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, null, true), UserWS.class);
            fail("Should fail!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,accountTypeId,validation.error.account.type.not.defined"));
        }
    }

    /**
     * C33367665
     */
    @Test
    public void postUserWithInvalidAccountType(){
        try {
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, Integer.MAX_VALUE, true), UserWS.class);
            fail("Should fail!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,accountTypeId,validation.error.account.type.not.exist"));
        }
    }

    @Test
    public void postUserWithNonExistingParent(){
        try {
            UserWS userWS = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID, true);
            userWS.setParentId(Integer.MAX_VALUE);
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, userWS, UserWS.class);
            fail("Should fail!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,parentId,validation.error.parent.does.not.exist"));
        }
    }

    @Test
    public void postUserWithInvalidParent(){
        UserWS nonParent = RestEntitiesHelper.buildUserMock("InvalidParentCustomer", DUMMY_TEST_AC_ID, true);
        nonParent.setIsParent(Boolean.FALSE);
        ResponseEntity<UserWS> nonParentResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, nonParent, UserWS.class);
        try {
            UserWS userWS = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID);
            userWS.setParentId(nonParentResponse.getBody().getId());
            restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                    postOrPutHeaders, userWS, UserWS.class);
            fail("Should fail!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,parentId,validation.error.not.allowed.parentId," + nonParentResponse.getBody().getId()));
        } finally {
            restTemplate.sendRequest(REST_URL + nonParentResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void postUserWithDuplicateUsername(){
        UserWS testUser = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID);
        ResponseEntity<UserWS> testUserResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, testUser, UserWS.class);
        try {
            UserWS userWS = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID);
            restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, userWS, UserWS.class);
            fail("Should fail!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,userName,validation.error.user.already.exists"));
        } finally {
            restTemplate.sendRequest(REST_URL + testUserResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getUser(){

        ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID, true), UserWS.class);

        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
        assertEquals(fetchedResponse.getBody(), postResponse.getBody(), "Users do not match!");

        restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getUserThatDoNotExist(){

        validateUserNotFound(Integer.MAX_VALUE);
    }

    @Test
    public void deleteUser(){

        ResponseEntity<UserWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID, true), UserWS.class);

        ResponseEntity deletedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);

        assertNotNull(deletedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(deletedResponse, Response.Status.NO_CONTENT.getStatusCode());
        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedResponse.getBody().getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class);
        assertTrue(fetchedResponse.getBody().getDeleted() == 1, "User not deleted!");
    }

    private void validateUserNotFound(Integer userId){

        try {
            restTemplate.sendRequest(REST_URL + userId, HttpMethod.GET, getOrDeleteHeaders, null, UserWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteUserThatDoNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void deleteUserThatCanNotBeDeleted(){

        UserWS parent = RestEntitiesHelper.buildUserMock("Test-CanNotBeDeleted-ParentUser", DUMMY_TEST_AC_ID, true);
        parent.setIsParent(Boolean.TRUE);
        ResponseEntity<UserWS> parentResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, parent, UserWS.class);

        UserWS child = RestEntitiesHelper.buildUserMock("Test-CanNotBeDeleted-ChildUser", DUMMY_TEST_AC_ID);
        child.setParentId(parentResponse.getBody().getId());
        ResponseEntity<UserWS> childResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, child, UserWS.class);

        try {
            restTemplate.sendRequest(REST_URL + parentResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
            fail("No no");
        } catch (HttpStatusCodeException e){
            String msg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.CONFLICT.getStatusCode());
            assertTrue(msg.contains("UserWS,childIds,validation.error.parent.user.cannot.be.deleted"));
        } finally {
            restTemplate.sendRequest(REST_URL + childResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + parentResponse.getBody().getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test(enabled = false)
    @Produces(value = { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void updateUser(){

        ResponseEntity<AccountInformationTypeWS> postAITResponse = restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl() + DUMMY_TEST_AC_ID + "/aits",
                HttpMethod.POST, postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact-updateUser"), AccountInformationTypeWS.class);

        UserWS updatedMock = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,  true);
        setAITMetaFields(postAITResponse.getBody().getId(), updatedMock, Constants.EPOCH_DATE, "test.email", "testCustomer@test.com");
        ResponseEntity<UserWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, updatedMock, UserWS.class);

        updatedMock = postedResponse.getBody();
        Date updatedDate = Util.truncateDate(new Date());
        setAITMetaFields(postAITResponse.getBody().getId(), updatedMock, updatedDate, "test.email", "updatedTestCustomer@test.com");
        ResponseEntity<UserWS> updatedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(),
                HttpMethod.PUT, postOrPutHeaders, updatedMock, UserWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());

        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class);
        validateAITMetaFieldUpdateOnUser(fetchedResponse.getBody(), 1, postAITResponse.getBody().getId(),
                buildValuesTimeLine(updatedDate, buildValueProjection("test.email", "updatedTestCustomer@test.com")),
                buildValuesTimeLine(Constants.EPOCH_DATE, buildValueProjection("test.email", "testCustomer@test.com")));

        updatedMock.setAccountInfoTypeFieldsMap(null);
        updatedMock.setTimelineDatesMap(null);
        updatedMock.setPassword("NewP@ssw0rd");
        restTemplate.sendRequest(REST_URL + updatedMock.getId(),
                HttpMethod.PUT, postOrPutHeaders, updatedMock, UserWS.class);
        restTemplate.sendRequest(REST_URL + updatedMock.getId(), HttpMethod.DELETE,
                getOrDeleteHeaders, null);

        //ToDo this fails due to foreign key constraint customer_account_info_type_timeline_meta_field_value_id_fk in customer_account_info_type_timeline
//        restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl() + DUMMY_TEST_AC_ID + "/aits/" + postAITResponse.getBody().getId(),
//                HttpMethod.DELETE, deleteHeaders, null);
    }

    @Test(enabled = false)
    public void updateUserThatDoNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE, HttpMethod.PUT,
                    postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID, true));
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    // ToDo we should revisit this, we are updating account type to non existing and this should be error
    @Test(enabled = false)
    public void updateUserForInvalidAccountType(){

        ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID, true), UserWS.class);
        UserWS user = postResponse.getBody();
        user.setAccountTypeId(Integer.MAX_VALUE);
        try {
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.PUT,
                    postOrPutHeaders, user);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } finally {
            restTemplate.sendRequest(REST_URL + user.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test(enabled = false)
    public void updateToNonExistingParent(){

        ResponseEntity<UserWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID, true), UserWS.class);
        UserWS userWS = postedResponse.getBody();
        userWS.setParentId(Integer.MAX_VALUE);
        try {
            restTemplate.sendRequest(REST_URL + userWS.getId(), HttpMethod.PUT, postOrPutHeaders, userWS);
            fail("No no");
        } catch (HttpServerErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,parentId,validation.error.parent.does.not.exist"));
        } finally {
            restTemplate.sendRequest(REST_URL + userWS.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }

    }

    // ToDo we should revisit this, we are changing isParent flag to false on parent customer with child.
    // The update should indicate error or something
    @Test(enabled = false)
    public void updateToAbandonChild(){

        UserWS parent = RestEntitiesHelper.buildUserMock("parentCustomer", DUMMY_TEST_AC_ID);
        parent.setIsParent(Boolean.TRUE);
        ResponseEntity<UserWS> parentResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, parent, UserWS.class);

        UserWS child = RestEntitiesHelper.buildUserMock("childCustomer", DUMMY_TEST_AC_ID, true);
        child.setParentId(parentResponse.getBody().getId());
        ResponseEntity<UserWS> childResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, child, UserWS.class);
        child = childResponse.getBody();
        try {
            parent = parentResponse.getBody();
            parent.setIsParent(Boolean.FALSE);
            parent.setChildIds(null);
            restTemplate.sendRequest(REST_URL + parent.getId(), HttpMethod.PUT,
                    postOrPutHeaders, parent, UserWS.class);
            fail("No no!");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,parentId,validation.error.not.allowed.parentId," + parentResponse.getBody().getId()));
        } finally {
            restTemplate.sendRequest(REST_URL + child.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);

            restTemplate.sendRequest(REST_URL + parent.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    // ToDo we should revisit this, we are updating username of an existing user to value that another user has
    // The update should indicate error or something
    @Test(enabled = false)
    public void updateUsernameToExistingOne(){
        UserWS testUser = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID);
        ResponseEntity<UserWS> testUserResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, testUser, UserWS.class);
        testUser = testUserResponse.getBody();

        UserWS userWS = RestEntitiesHelper.buildUserMock("AnotherTestCustomer", DUMMY_TEST_AC_ID);
        ResponseEntity<UserWS> anotherResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, userWS, UserWS.class);
        userWS = anotherResponse.getBody();
        userWS.setUserName("TestCustomer");
        try {
            restTemplate.sendRequest(REST_URL + userWS.getId(), HttpMethod.PUT,
                    postOrPutHeaders, userWS, UserWS.class);
            fail("No no");
        } catch (HttpClientErrorException e){
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("UserWS,userName,validation.error.user.already.exists"));
        } finally {
            restTemplate.sendRequest(REST_URL + testUser.getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
            restTemplate.sendRequest(REST_URL + userWS.getId(), HttpMethod.DELETE,
                    getOrDeleteHeaders, null);
        }
    }

    @Test
    public void getInvoicesForUser(){
        final List<Integer> containedIds = Arrays.asList(1, 2, 3, 4, 5);
        ResponseEntity<InvoiceWS[]> allInvoicesResponse = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(allInvoicesResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(allInvoicesResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(collectIds(allInvoicesResponse.getBody()).containsAll(containedIds), "Ids not found!");
    }

    @Test
    public void getInvoicesForUserLimited(){
        ResponseEntity<InvoiceWS[]> allInvoicesResponse = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        List<Integer> allInvoicesIds = collectIds(allInvoicesResponse.getBody());

        ResponseEntity<InvoiceWS[]> limitedResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + GANDALF_ID + "/invoices", new RestQueryParameter<>("limit", Integer.valueOf(2))), HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(limitedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(limitedResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(limitedResponse.getBody().length == 2, "Invalid number of invoices!");
        assertTrue(allInvoicesIds.containsAll(collectIds(limitedResponse.getBody())), "Ids not found!");

        limitedResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + GANDALF_ID + "/invoices", new RestQueryParameter<>("limit", Integer.MAX_VALUE)), HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(limitedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(limitedResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(allInvoicesIds.size() == limitedResponse.getBody().length, "Invalid number of invoices!");
        assertTrue(allInvoicesIds.containsAll(collectIds(limitedResponse.getBody())), "Ids not found!");
    }

    @Test
    public void getInvoicesWithNegativeLimit(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + GANDALF_ID + "/invoices", new RestQueryParameter<>("limit", Integer.valueOf(-2))), HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void getInvoicesForUserWithOffset(){
        ResponseEntity<InvoiceWS[]> allInvoicesResponse = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        List<Integer> allInvoicesIds = collectIds(allInvoicesResponse.getBody());

        ResponseEntity<InvoiceWS[]> limitedResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + GANDALF_ID + "/invoices", new RestQueryParameter<>("offset", Integer.valueOf(2))), HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(limitedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(limitedResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(allInvoicesIds.size() == limitedResponse.getBody().length + 2, "Invalid number of invoices!");
        assertTrue(allInvoicesIds.containsAll(collectIds(limitedResponse.getBody())), "Ids not found!");
    }

    @Test
    public void getInvoicesWithNegativeOffset(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + GANDALF_ID + "/invoices", new RestQueryParameter<>("offset", Integer.valueOf(-2))), HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void getInvoicesForUserLimitedAndWithOffset(){
        ResponseEntity<InvoiceWS[]> allInvoicesResponse = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        List<Integer> allInvoicesIds = collectIds(allInvoicesResponse.getBody());

        ResponseEntity<InvoiceWS[]> limitedOffsetResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + GANDALF_ID + "/invoices", new RestQueryParameter<Integer>("limit", Integer.valueOf(2)),
                        new RestQueryParameter<Integer>("offset", Integer.valueOf(2))), HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(limitedOffsetResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(limitedOffsetResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(allInvoicesIds.size() > limitedOffsetResponse.getBody().length, "Invalid number of invoices!");
        assertEquals(limitedOffsetResponse.getBody().length, 2, "Invalid number of invoices!");
        assertTrue(allInvoicesIds.containsAll(collectIds(limitedOffsetResponse.getBody())), "Ids not found!");
    }

    @Test
    public void getInvoicesForUserThatDoNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE + "/invoices", HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getLastInvoiceForUser(){

        ResponseEntity<InvoiceWS[]> allInvoicesResponse = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices?offset=0", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        List<String> allInvoicesIds = collectPublicNrs(allInvoicesResponse.getBody());
        Collections.sort(allInvoicesIds, (o1, o2) -> new Integer(o2).compareTo(new Integer(o1)));

        ResponseEntity<InvoiceWS> lastInvoice = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices/last", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS.class);

        assertNotNull(lastInvoice, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(lastInvoice, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertEquals(allInvoicesIds.get(0), lastInvoice.getBody().getNumber(), "Invalid last invoice!");
    }

    @Test
    public void getNumberOfLastInvoicesForUser(){

        ResponseEntity<InvoiceWS[]> allInvoicesResponse = restTemplate.sendRequest(REST_URL + GANDALF_ID + "/invoices", HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        List<Integer> allInvoicesIds = collectIds(allInvoicesResponse.getBody());

        ResponseEntity<InvoiceWS[]> limitedResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + GANDALF_ID + "/invoices/last", new RestQueryParameter<>("number", Integer.valueOf(2))), HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(limitedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(limitedResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(limitedResponse.getBody().length == 2, "Invalid number of invoices!");

        limitedResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                        REST_URL + GANDALF_ID + "/invoices/last", new RestQueryParameter<>("number", Integer.MAX_VALUE)), HttpMethod.GET,
                getOrDeleteHeaders, null, InvoiceWS[].class);

        assertNotNull(limitedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(limitedResponse, Response.Status.OK.getStatusCode());
        //ToDo a better solution should be applied when order rest end points will be available
        assertTrue(allInvoicesIds.size() == limitedResponse.getBody().length, "Invalid number of invoices!");
        assertTrue(allInvoicesIds.containsAll(collectIds(limitedResponse.getBody())), "Ids not found!");
    }

    @Test
    public void getNegativeLastInvoices(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + GANDALF_ID + "/invoices/last", new RestQueryParameter<>("number", Integer.valueOf(-2))), HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }


    @Test
    public void getLastInvoicesForUserThatDoNotExist(){

        try {
            restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE + "/invoices/last", HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS[].class);
            fail("No no");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getPaymentsForUserThatDoNotExist(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + Integer.MAX_VALUE + "/payments", new RestQueryParameter<>("limit", Integer.valueOf(10)), new RestQueryParameter<>("offset", Integer.valueOf(2))),
                    HttpMethod.GET, getOrDeleteHeaders, null, PaymentWS[].class);
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    @Test
    public void getLastPaymentsForUserThatDoNotExist(){

        try {
            restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            REST_URL + Integer.MAX_VALUE + "/payments/last", new RestQueryParameter<>("number", Integer.valueOf(10))),
                    HttpMethod.GET, getOrDeleteHeaders, null, PaymentWS[].class);
        }catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C33367674- Verify user can create a Parent/Child relationship within the Customer tab. Create Parent Customer Brian Smith, with            Sarah Wilson as his Child (Sub) Account.
     */
    @Test
    public void postParentChildRelationship() {
        UserWS parentCustomer = null, childCustomer = null;
        try {
        parentCustomer = RestEntitiesHelper.buildUserMock("Brian Smith" + String.valueOf(DUMMY_TEST_AC_ID), DUMMY_TEST_AC_ID, true);
        parentCustomer.setIsParent(Boolean.TRUE);
        childCustomer = RestEntitiesHelper.buildUserMock("Sarah Wilson" + String.valueOf(DUMMY_TEST_AC_ID), DUMMY_TEST_AC_ID, true);
        parentCustomer = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, parentCustomer, UserWS.class).getBody();
        childCustomer.setParentId(parentCustomer.getId());
        childCustomer = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders, childCustomer, UserWS.class).getBody();
        } catch (HttpClientErrorException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
        } finally {
            if (parentCustomer != null) {
                restTemplate.sendRequest(REST_URL + String.valueOf(childCustomer.getId()), HttpMethod.DELETE, getOrDeleteHeaders, null);
            }
            if (childCustomer != null) {
                restTemplate.sendRequest(REST_URL + String.valueOf(parentCustomer.getId()), HttpMethod.DELETE, getOrDeleteHeaders, null);
            }
        }

    }

    /**
     * C33367673 - Verify that you can create a Customer with a mandatory meta field from the account type
     */
    @Test
    public void postCustomerWithMandatoryMetaFieldFromAccountType() {
        ResponseEntity<AccountInformationTypeWS> postAITResponse = restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl() + DUMMY_TEST_AC_ID + "/aits", HttpMethod.POST, postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Contact-MandatoryMetaField"), AccountInformationTypeWS.class);

        UserWS userWS = RestEntitiesHelper.buildUserMock("Customer A" + String.valueOf(DUMMY_TEST_AC_ID), DUMMY_TEST_AC_ID, true);
        setAITMetaFields(postAITResponse.getBody().getId(), userWS, Constants.EPOCH_DATE, "test.email", "testCustomer@jBilling.com");
        ResponseEntity<UserWS> userResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, userWS, UserWS.class);
        validateAITMetaFieldUpdateOnUser(userResponse.getBody(), 1, postAITResponse.getBody().getId(), buildValuesTimeLine(Constants.EPOCH_DATE, buildValueProjection("test.email", "testCustomer@jBilling.com")));
        RestValidationHelper.validateStatusCode(userResponse, Response.Status.CREATED.getStatusCode());
        restTemplate.sendRequest(REST_URL + String.valueOf(userResponse.getBody().getId()), HttpMethod.DELETE, getOrDeleteHeaders, null);

    }
    @Test
    public void getUserByCustomerMetaField() {

        UserWS userWS = RestEntitiesHelper.buildUserWithCustomerMetafieldMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,
                TEST_CUSTOMER_LEVEL_MF, TEST_CUSTOMER_LEVEL_MF_VALUE, true);
        ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                userWS, UserWS.class);

        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(REST_URL + TEST_CUSTOMER_LEVEL_MF + "/"
                + TEST_CUSTOMER_LEVEL_MF_VALUE, HttpMethod.GET, getOrDeleteHeaders, null, UserWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());
        assertEquals(fetchedResponse.getBody().getId(), postResponse.getBody().getId(), "Users do not match!");

        restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void getUsersByStatus() {

        UserWS userWS = RestEntitiesHelper.buildUserWithCustomerMetafieldMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,
                TEST_CUSTOMER_LEVEL_MF, TEST_CUSTOMER_LEVEL_MF_VALUE, true);
        ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST, postOrPutHeaders,
                userWS, UserWS.class);

        /**
         * This should fetch above created customer since we are passing the Active status ID as 1 and true to include
         * all the active customers
         */
        ResponseEntity<Integer[]> fetchedResponseForIncludedCustomer = restTemplate.sendRequest(REST_URL + STATUS_URL
                + true, HttpMethod.GET, getOrDeleteHeaders, null, Integer[].class);

        assertNotNull(fetchedResponseForIncludedCustomer, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponseForIncludedCustomer, Response.Status.OK.getStatusCode());
        assertTrue(Arrays.asList(fetchedResponseForIncludedCustomer.getBody()).contains(postResponse.getBody().getId()),
                "Active user not found");

        /**
         * This should not fetch the above created customer since we are passing the Active status ID as 1 and false to
         * include all the customers other than the Active status
         */
        ResponseEntity<Integer[]> fetchedResponseForExcludedCustomer = restTemplate.sendRequest(REST_URL + STATUS_URL
                + false, HttpMethod.GET, getOrDeleteHeaders, null, Integer[].class);

        assertNotNull(fetchedResponseForExcludedCustomer, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponseForExcludedCustomer, Response.Status.OK.getStatusCode());
        assertTrue(!(Arrays.asList(fetchedResponseForExcludedCustomer.getBody()).contains(postResponse.getBody().getId())),
                "Active user found");

        restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
    }

    @Test
    public void updateContactInfo(){
        UserWS user =  RestEntitiesHelper.buildUserMock(TEST_CUSTOMER , 1, true);
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(user.getUserName() + "@shire.com");
        metaField1.setGroupId(1);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Frodo");
        metaField2.setGroupId(1);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Baggins");
        metaField3.setGroupId(1);

        user.setMetaFields(new MetaFieldValueWS[] {metaField1, metaField2, metaField3 });

        ResponseEntity<UserWS> postResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, user, UserWS.class);

        assertNotNull(postResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
        UserWS postedUser = postResponse.getBody();
        ResponseEntity<UserWS> fetchedResponse = restTemplate.sendRequest(REST_URL + postedUser.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class);
        UserWS fetchedUser = fetchedResponse.getBody();

        assertEquals(postedUser.getId(), fetchedUser.getId(), "Users do not match!");

        ContactInformationWS newContact = new ContactInformationWS();
        Map<String, Object> newAitMap = new HashMap<>();
        newAitMap.put("contact.email", "testUpdateUser@test.com");
        newAitMap.put("contact.first.name", "TestUpdateUserFName");
        newAitMap.put("contact.last.name", "TestUpdateUserLName");
        newContact.setUserId(postedUser.getId());
        newContact.setGroupName("Contact");
        newContact.setMetaFields(newAitMap);
        ResponseEntity<UserWS> postResponse1 = restTemplate.sendRequest(REST_URL+"updatecustomercontactinfo", HttpMethod.POST,
                postOrPutHeaders, newContact, UserWS.class);
        fetchedUser = postResponse1.getBody();
        assertNotNull("Contact email should not be null", getMetaField(fetchedUser.getMetaFields(), "contact.email"));
        assertNotNull("Contact first name should not be null", getMetaField(fetchedUser.getMetaFields(), "contact.first.name"));
        assertNotNull("Contact last name not be null", getMetaField(fetchedUser.getMetaFields(), "contact.last.name"));

        assertEquals(getMetaField(fetchedUser.getMetaFields(), "contact.email").getStringValue(), "testUpdateUser@test.com", "email should be " );
        assertEquals(getMetaField(fetchedUser.getMetaFields(), "contact.first.name").getStringValue(), "TestUpdateUserFName", "first name should be ");
        assertEquals(getMetaField(fetchedUser.getMetaFields(), "contact.last.name").getStringValue(), "TestUpdateUserLName","last name should be " );

    }
    
    @Test(enabled=true)
    public void postUserWithInvoiceDeliveryMethod(){
    	
    	PreferenceWS expectedPreference = RestEntitiesHelper.buildPreference(Integer.valueOf(105), null, null,
                RestEntitiesHelper.buildPreferenceType(PREF_TYPE_ID, "Set Invoice Delivery Method as Email and Paper on the Customer Account if Email Address is not provided.", null, null), "1");

        // Get the preference and verify the response
        ResponseEntity<PreferenceWS> getResponse = restTemplate.sendRequest(preferenceRestHelper.getFullRestUrl() + PREF_TYPE_ID,
                HttpMethod.GET, getOrDeleteHeaders, null, PreferenceWS.class);
        assertNotNull(getResponse, "GET response should not be null.");
        RestValidationHelper.validateStatusCode(getResponse, Response.Status.OK.getStatusCode());
        
        expectedPreference.setValue("1");
        
        ResponseEntity<PreferenceWS> putResponse = restTemplate.sendRequest(preferenceRestHelper.getFullRestUrl() + PREF_TYPE_ID,
                HttpMethod.PUT, postOrPutHeaders, expectedPreference, PreferenceWS.class);
        RestValidationHelper.validateStatusCode(putResponse, Response.Status.OK.getStatusCode());
        
    	ResponseEntity<AccountInformationTypeWS> postAITResponse = restTemplate.sendRequest(accountTypeRestHelper.getFullRestUrl() + DUMMY_TEST_AC_ID + "/aits",
                HttpMethod.POST, postOrPutHeaders, RestEntitiesHelper.buildAccountInformationTypeMock(DUMMY_TEST_AC_ID, "Mailing Address"), AccountInformationTypeWS.class);

    	// Scenario 1: New Customer invoice delivery method is Email and Email Id field is blank
    	UserWS userWS1 = RestEntitiesHelper.buildUserWithCustomerMetafieldMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,
    			TEST_CUSTOMER_LEVEL_MF_EMAIL, null, true);
    	
    	userWS1.setInvoiceDeliveryMethodId(Constants.D_METHOD_EMAIL);
    	
        ResponseEntity<UserWS> postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, userWS1, UserWS.class);
        
        userWS1 = postedResponse.getBody();
        
        assertEquals(userWS1.getInvoiceDeliveryMethodId(), Constants.D_METHOD_EMAIL_AND_PAPER, "Invoice delivery method should be Email & Paper!");
        
        restTemplate.sendRequest(REST_URL + userWS1.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        
        // Scenario 2: Update customer with invoice delivery method as Email and Email Id field is blank

    	UserWS userWS2 = RestEntitiesHelper.buildUserWithCustomerMetafieldMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,
                TEST_CUSTOMER_LEVEL_MF_EMAIL, null, true);
    	// Disable preference 
    	expectedPreference.setValue("0");
        
        restTemplate.sendRequest(preferenceRestHelper.getFullRestUrl() + PREF_TYPE_ID,
                HttpMethod.PUT, postOrPutHeaders, expectedPreference, PreferenceWS.class);
    	
        userWS2.setInvoiceDeliveryMethodId(Constants.D_METHOD_EMAIL);
    	
    	
    	// Create an user
        postedResponse = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST,
                postOrPutHeaders, userWS2, UserWS.class);
        
        userWS2 = postedResponse.getBody();
        
        assertEquals(userWS2.getInvoiceDeliveryMethodId(), Constants.D_METHOD_EMAIL, "Invoice delivery method should be Email!");
        
    	// Enable preference 
    	expectedPreference.setValue("1");
        
        restTemplate.sendRequest(preferenceRestHelper.getFullRestUrl() + PREF_TYPE_ID,
                HttpMethod.PUT, postOrPutHeaders, expectedPreference, PreferenceWS.class);
        
        ResponseEntity<CustomerRestWS> fetchedResponse = restTemplate.sendRequest(userRestHelper.getFullRestUrl()+"customerattributes/" + userWS2.getId(), HttpMethod.GET,
                getOrDeleteHeaders, null, CustomerRestWS.class);
        
        CustomerRestWS customerRestWS = fetchedResponse.getBody();
        
        customerRestWS.setDueDateValue(3);
        
        ResponseEntity<CustomerRestWS> updatedResponse = restTemplate.sendRequest(userRestHelper.getFullRestUrl()+"customerattributes/" + customerRestWS.getUserId(),
                HttpMethod.PUT, postOrPutHeaders, customerRestWS, CustomerRestWS.class);
        
        CustomerRestWS updatedCustomerRestWS = updatedResponse.getBody();
        
        assertEquals(updatedCustomerRestWS.getInvoiceDeliveryMethodId(), Constants.D_METHOD_EMAIL_AND_PAPER, "Invoice delivery method should be Email & Paper!");
        
        restTemplate.sendRequest(REST_URL + updatedCustomerRestWS.getUserId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        
        // Scenario 3: Create user with populated email id and check invoice delivery method

    	UserWS userWS3 = RestEntitiesHelper.buildUserMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,  true);
    	
    	userWS3.setInvoiceDeliveryMethodId(Constants.D_METHOD_EMAIL_AND_PAPER);
    	setAITMetaFields(postAITResponse.getBody().getId(), userWS3, Constants.EPOCH_DATE, "test.email", "testCustomer@test.com");
        postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, userWS3, UserWS.class);
        
        userWS3 = postedResponse.getBody();
        
        assertEquals(userWS3.getInvoiceDeliveryMethodId(), Constants.D_METHOD_EMAIL_AND_PAPER, "Invoice delivery method should be Email and Paper!");
        
        restTemplate.sendRequest(REST_URL + userWS3.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        
        // Scenario 4: New customer with invoice delivery method as Paper and Email Id field is populated
        
        UserWS userWS4 = RestEntitiesHelper.buildUserWithCustomerMetafieldMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,
    			TEST_CUSTOMER_LEVEL_MF_EMAIL, "testCustomer@test.com", true);
    	
        userWS4.setInvoiceDeliveryMethodId(Constants.D_METHOD_PAPER);
    	
        postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, userWS4, UserWS.class);
        
        userWS4 = postedResponse.getBody();
        
        assertEquals(userWS4.getInvoiceDeliveryMethodId(), Constants.D_METHOD_PAPER, "Invoice delivery method should be Paper!");
        
        restTemplate.sendRequest(REST_URL + userWS4.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        
        // Scenario 5: New customer with invoice delivery method as Paper and Email Id field is blank
        
        UserWS userWS5 = RestEntitiesHelper.buildUserWithCustomerMetafieldMock(TEST_CUSTOMER, DUMMY_TEST_AC_ID,
    			TEST_CUSTOMER_LEVEL_MF_EMAIL, null, true);
    	
        userWS5.setInvoiceDeliveryMethodId(Constants.D_METHOD_PAPER);
    	
        postedResponse = restTemplate.sendRequest(REST_URL, HttpMethod.POST,
                postOrPutHeaders, userWS5, UserWS.class);
        
        userWS5 = postedResponse.getBody();
        
        assertEquals(userWS5.getInvoiceDeliveryMethodId(), Constants.D_METHOD_PAPER, "Invoice delivery method should be Paper!");
        
        restTemplate.sendRequest(REST_URL + userWS5.getId(), HttpMethod.DELETE, getOrDeleteHeaders, null);
        
        // Disable preference
        expectedPreference.setValue("0");
        
        restTemplate.sendRequest(preferenceRestHelper.getFullRestUrl() + PREF_TYPE_ID,
                HttpMethod.PUT, postOrPutHeaders, expectedPreference, PreferenceWS.class);
    }
    
    private void validateAITMetaFieldUpdateOnUser(UserWS user, int numberOfAITs, Integer metaFieldGroupId, DateMetaFieldValueMap... timeLines){
        assertEquals(user.getAccountInfoTypeFieldsMap().size(), numberOfAITs, "Invalid number of AITs!");
        assertTrue(user.getAccountInfoTypeFieldsMap().containsKey(metaFieldGroupId), "No ait meta-field found!");
        HashMap<Date, ArrayList<MetaFieldValueWS>> metaFieldsTimeLine = user.getAccountInfoTypeFieldsMap().get(metaFieldGroupId);
        for (DateMetaFieldValueMap timeLine : timeLines){
            assertTrue(metaFieldsTimeLine.containsKey(timeLine.date), "Time line not found!");
            ArrayList<MetaFieldValueWS> metaFieldValues = metaFieldsTimeLine.get(timeLine.date);
            for (MetaFieldValueWS metaFieldValue : metaFieldValues){
                boolean found = false;
                for (MetaFieldValueProjection valueProjection : timeLine.metaFieldValue){
                    found = valueProjection.getMetaFieldName().equals(metaFieldValue.getFieldName()) &&
                            valueProjection.getMetaFieldValue().equals(metaFieldValue.getStringValue());
                    if (found){
                        break;
                    }
                }
                assertTrue(found, "MetaFieldValue invalid!");
            }
        }
    }

    private class DateMetaFieldValueMap{

        private Date date;
        List<MetaFieldValueProjection> metaFieldValue;

        public DateMetaFieldValueMap(Date date, List<MetaFieldValueProjection> metaFieldValue) {
            this.date = date;
            this.metaFieldValue = metaFieldValue;
        }

        public Date getDate() {
            return date;
        }

        public List<MetaFieldValueProjection> getMetaFieldValue() {
            return metaFieldValue;
        }
    }

    private class MetaFieldValueProjection{
        private String metaFieldName;
        private String metaFieldValue;

        public MetaFieldValueProjection(String metaFieldName, String metaFieldValue) {
            this.metaFieldName = metaFieldName;
            this.metaFieldValue = metaFieldValue;
        }

        public String getMetaFieldName() {
            return metaFieldName;
        }

        public String getMetaFieldValue() {
            return metaFieldValue;
        }
    }

    private MetaFieldValueProjection buildValueProjection(String name, String value){
        return new MetaFieldValueProjection(name, value);
    }

    private DateMetaFieldValueMap buildValuesTimeLine(Date date, MetaFieldValueProjection... values){
        return new DateMetaFieldValueMap(date, Arrays.asList(values));
    }

    private void setAITMetaFields(Integer aitId, UserWS user, Date date, String metaFieldName, String metaFieldValue){
        Map<Integer, HashMap<Date, ArrayList<MetaFieldValueWS>>> aitMetaFields = new HashMap<>();

        HashMap<Date, ArrayList<MetaFieldValueWS>> timeLineMetaFields = new HashMap<>();
        ArrayList<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        MetaFieldValueWS metaFieldValueWS = RestEntitiesHelper.buildMetaFieldValue(metaFieldName, metaFieldValue, aitId);
        metaFieldValues.add(metaFieldValueWS);
        user.setMetaFields(new MetaFieldValueWS[]{metaFieldValueWS});
        timeLineMetaFields.put(date, metaFieldValues);
        aitMetaFields.put(aitId, timeLineMetaFields);
        user.setAccountInfoTypeFieldsMap(aitMetaFields);
        ArrayList<Date> dates = new ArrayList<>();
        dates.add(date);
        Map<Integer, ArrayList<Date>> datesMap = new HashMap<>();
        datesMap.put(aitId, dates);
        user.setTimelineDatesMap(datesMap);
        Map<Integer, Date> effectiveDateMap = new HashMap<>();
        effectiveDateMap.put(aitId, date);
        user.setEffectiveDateMap(effectiveDateMap);
    }

    private List<Integer> collectIds(InvoiceWS[] invoices){
        if (null != invoices){
            List<Integer> ids = new ArrayList<>(invoices.length);
            for (InvoiceWS invoice : invoices){
                ids.add(invoice.getId());
            }
            return ids;
        }
        return null;
    }

    private List<String> collectPublicNrs(InvoiceWS[] invoices){
        if (null != invoices){
            List<String> ids = new ArrayList<>(invoices.length);
            for (InvoiceWS invoice : invoices){
                ids.add(invoice.getNumber());
            }
            return ids;
        }
        return null;
    }

    private static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
            String fieldName) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                return ws;
            }
        }
        return null;
    }

}
