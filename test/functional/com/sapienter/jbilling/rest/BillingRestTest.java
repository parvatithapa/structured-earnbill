package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.resources.OrderInfo;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import javax.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static org.testng.Assert.*;

/**
 * @author Vojislav Stanojevikj
 * @since 07-Nov-2016.
 */
@Test(groups = {"rest"}, testName = "BillingRestTest")
public class BillingRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);
    private static final String PRORATING_AUTO = ProratingType.PRORATING_AUTO_ON.getProratingType();
    private static final Integer MONTHLY_UNIT_ID = Constants.PERIOD_UNIT_MONTH;

    private RestOperationsHelper accTypeRestHelper;
    private RestOperationsHelper userRestHelper;
    private RestOperationsHelper itemTypeRestHelper;
    private RestOperationsHelper itemRestHelper;
    private RestOperationsHelper periodRestHelper;
    private RestOperationsHelper ocStatusRestHelper;
    private RestOperationsHelper orderRestHelper;
    private RestOperationsHelper invoiceRestHelper;

    private Integer ACC_TYPE_ID;
    private Integer USER_ID;
    private Integer ITEM_TYPE_ID;
    private Integer ITEM_ONE_ID;
    private Integer ITEM_TWO_ID;
    private Integer PERIOD_ID;
    private Integer OC_STATUS_ID;
    private static final String OC_STATUS = "orderTestChangeStatus_" + System.currentTimeMillis();

    @BeforeClass
    public void setup() {

        super.setup("billing");

        // Create an account type
        accTypeRestHelper = RestOperationsHelper.getInstance("accounttypes");
        ACC_TYPE_ID = restTemplate.sendRequest(accTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildAccountTypeMock(0, "billingTestAccType"), AccountTypeWS.class).getBody().getId();
        // Create a user
        userRestHelper = RestOperationsHelper.getInstance("users");
        USER_ID = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildUserMock("billingTestUser", ACC_TYPE_ID), UserWS.class).getBody().getId();
        // Create an item type
        itemTypeRestHelper = RestOperationsHelper.getInstance("itemtypes");
        ITEM_TYPE_ID = restTemplate.sendRequest(itemTypeRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemTypeMock("billingTestItemType", true, true), ItemTypeWS.class).getBody().getId();
        // Create two items
        itemRestHelper = RestOperationsHelper.getInstance("items");
        ITEM_ONE_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("billingTestItemOne", false, true, ITEM_TYPE_ID), ItemDTOEx.class).getBody().getId();
        ITEM_TWO_ID = restTemplate.sendRequest(itemRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildItemMock("billingTestItemTwo", false, true, ITEM_TYPE_ID), ItemDTOEx.class).getBody().getId();
        // Create an order period
        periodRestHelper = RestOperationsHelper.getInstance("orderperiods");
        PERIOD_ID = restTemplate.sendRequest(periodRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildOrderPeriodMock(Constants.PERIOD_UNIT_MONTH, Integer.valueOf(1), "billingTestPeriod"),
                OrderPeriodWS.class).getBody().getId();
        // Create an order change status
        ocStatusRestHelper = RestOperationsHelper.getInstance("orderchangestatuses");
        OC_STATUS_ID = restTemplate.sendRequest(ocStatusRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                RestEntitiesHelper.buildOrderChangeStatus(
                        null, ENTITY_ID, ApplyToOrder.YES, Integer.valueOf(1), OC_STATUS),
                OrderChangeStatusWS.class).getBody().getId();

        orderRestHelper = RestOperationsHelper.getInstance("orders");
        invoiceRestHelper = RestOperationsHelper.getInstance("invoices");
    }

    @AfterClass()
    public void tearDown() {

        // Delete the two created items
        if (null != ITEM_ONE_ID) {
            restTemplate.sendRequest(
                    itemRestHelper.getFullRestUrl() + ITEM_ONE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        if (null != ITEM_TWO_ID) {
            restTemplate.sendRequest(
                    itemRestHelper.getFullRestUrl() + ITEM_TWO_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete the created item type
        if (null != ITEM_TYPE_ID) {
            restTemplate.sendRequest(
                    itemTypeRestHelper.getFullRestUrl() + ITEM_TYPE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete the created user
        if (null != USER_ID) {
            restTemplate.sendRequest(
                    userRestHelper.getFullRestUrl() + USER_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete the created account type
        if (null != ACC_TYPE_ID) {
            restTemplate.sendRequest(
                    accTypeRestHelper.getFullRestUrl() + ACC_TYPE_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
        // Delete created order change status
        if (null != OC_STATUS_ID){
            restTemplate.sendRequest(
                    ocStatusRestHelper.getFullRestUrl() + OC_STATUS_ID, HttpMethod.DELETE, getOrDeleteHeaders, null);
        }
    }

    @Test(priority = 0)
    public void getBillingConfiguration(){

        ResponseEntity<BillingProcessConfigurationWS> fetchedResponse = restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.GET,
                getOrDeleteHeaders, null, BillingProcessConfigurationWS.class);

        assertNotNull(fetchedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(fetchedResponse, Response.Status.OK.getStatusCode());

        BillingProcessConfigurationWS fetchedConfiguration = fetchedResponse.getBody();
        assertNotNull(fetchedConfiguration, "Billing configuration can not be null!");
        assertEquals(fetchedConfiguration.getEntityId(), RestEntitiesHelper.TEST_ENTITY_ID, "Entity id invalid");
    }
/*
    These tests do not work in combination with the rest of the test suite. Dates do not line up. Process runs too long.

    @Test(priority = 1)
    public void updateBillingConfiguration(){

        ResponseEntity<BillingProcessConfigurationWS> fetchedResponse = restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.GET,
                getOrDeleteHeaders, null, BillingProcessConfigurationWS.class);

        BillingProcessConfigurationWS fetchedConfiguration = fetchedResponse.getBody();
        Integer oldDaysForReport = fetchedConfiguration.getDaysForReport();
        fetchedConfiguration.setDaysForReport(Integer.valueOf(1 + oldDaysForReport));

        ResponseEntity<BillingProcessConfigurationWS> updatedResponse = restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.PUT,
                postOrPutHeaders, fetchedConfiguration, BillingProcessConfigurationWS.class);

        assertNotNull(updatedResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(updatedResponse, Response.Status.OK.getStatusCode());
        BillingProcessConfigurationWS updatedConfiguration = updatedResponse.getBody();

        assertEquals(updatedConfiguration.getDaysForReport(), Integer.valueOf(1 + oldDaysForReport), "Invalid update!");

        updatedConfiguration.setDaysForReport(oldDaysForReport);
        restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.PUT,
                postOrPutHeaders, updatedConfiguration, BillingProcessConfigurationWS.class);
    }

    @Test(priority = 2)
    public void triggerBillingGetLastInvoicesByUserAndGetLast(){
        ZoneId defaultZone = ZoneId.systemDefault();
        LocalDate billingRunDate = LocalDate.of(2016, 7, 1).atStartOfDay(defaultZone).toLocalDate();
        long millis = billingRunDate.atStartOfDay().atZone(defaultZone).toInstant().toEpochMilli();

        Integer orderId = null;
        Integer invoiceId = null;
        try {
            updateBillingConfig(billingRunDate, false, MONTHLY_UNIT_ID);
            updateCustomerNID(USER_ID, billingRunDate);
            orderId = persistOrder(billingRunDate);

            // Trigger billing
            ResponseEntity billingResponse = restTemplate.sendRequest(REST_URL + "processes/" + millis,
                    HttpMethod.POST, postOrPutHeaders, null, Void.class);

            assertNotNull(billingResponse, "Response can not be null!!");
            RestValidationHelper.validateStatusCode(billingResponse, Response.Status.CREATED.getStatusCode());

            ResponseEntity<InvoiceWS[]> lastInvoicesResponse = restTemplate.sendRequest(RestEntitiesHelper.addQueryParamsToUrl(
                            userRestHelper.getFullRestUrl() + USER_ID + "/invoices/last", new RestQueryParameter<>("number", Integer.valueOf(100))), HttpMethod.GET,
                    getOrDeleteHeaders, null, InvoiceWS[].class);

            assertNotNull(lastInvoicesResponse, "Response can not be null!!");
            RestValidationHelper.validateStatusCode(lastInvoicesResponse, Response.Status.OK.getStatusCode());
            InvoiceWS[] invoices = lastInvoicesResponse.getBody();
            assertEquals(invoices.length, 1, "Invalid number of generated invoice!");
            assertTrue(invoiceContainsOrder(invoices[0], orderId), "Order not invoiced!");
            invoiceId = invoices[0].getId();

            ResponseEntity<BillingProcessWS> getLastBilling = restTemplate.sendRequest(REST_URL + "processes/last", HttpMethod.GET,
                    getOrDeleteHeaders, null, BillingProcessWS.class);

            assertNotNull(getLastBilling, "Response can not be null!!");
            RestValidationHelper.validateStatusCode(getLastBilling, Response.Status.OK.getStatusCode());

            BillingProcessWS billingProcessWS = getLastBilling.getBody();
            assertEquals(billingProcessWS.getBillingDate(), Date.from(billingRunDate.atStartOfDay().atZone(defaultZone).toInstant()));

        } finally {
            // ToDo this is not working but it should
            // Delete the invoice
//            if (null != invoiceId) {
//                restTemplate.sendRequest(invoiceRestHelper.getFullRestUrl() + invoiceId, HttpMethod.DELETE,
//                        getOrDeleteHeaders, null);
//            }

//            // Delete the order
//            if (null != orderId) {
//                restTemplate.sendRequest(orderRestHelper.getFullRestUrl() + orderId, HttpMethod.DELETE,
//                        getOrDeleteHeaders, null);
//            }
        }

    }

    @Test(priority = 3)
    public void getProcessByIdAndGetInvoicesByProcessId(){
        ZoneId defaultZone = ZoneId.systemDefault();
        LocalDate billingRunDate = LocalDate.of(2016, 8, 1).atStartOfDay(defaultZone).toLocalDate();
        long millis = billingRunDate.atStartOfDay().atZone(defaultZone).toInstant().toEpochMilli();

        Integer orderId = null;
        Integer invoiceId = null;
        Integer testCustomerId = null;
        try {
            updateBillingConfig(billingRunDate, false, MONTHLY_UNIT_ID);

            testCustomerId = restTemplate.sendRequest(userRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders,
                    RestEntitiesHelper.buildUserMock("billingTestUser1", ACC_TYPE_ID), UserWS.class).getBody().getId();

            updateCustomerNID(testCustomerId, billingRunDate);
            orderId = persistOrder(billingRunDate);

            // Trigger billing
            restTemplate.sendRequest(REST_URL + "processes/" + millis,
                    HttpMethod.POST, postOrPutHeaders, null, Void.class);

            ResponseEntity<BillingProcessWS> getLastBilling = restTemplate.sendRequest(REST_URL + "processes/last", HttpMethod.GET,
                    getOrDeleteHeaders, null, BillingProcessWS.class);

            Integer billingId = getLastBilling.getBody().getId();

            ResponseEntity<InvoiceWS[]> allInvoices = restTemplate.sendRequest(REST_URL + "processes/" + billingId + "/invoices",
                    HttpMethod.GET, getOrDeleteHeaders, null, InvoiceWS[].class);

            assertNotNull(allInvoices, "Response can not be null!!");
            RestValidationHelper.validateStatusCode(allInvoices, Response.Status.OK.getStatusCode());

            InvoiceWS[] invoices = allInvoices.getBody();
            assertNotNull(invoices, "Invoices can not be null!");
            assertEquals(invoices.length, 1, "Invalid number of invoices!");
            assertTrue(invoiceContainsOrder(invoices[0], orderId));

            ResponseEntity<BillingProcessWS> getBilling = restTemplate.sendRequest(REST_URL + "processes/" + billingId, HttpMethod.GET,
                    getOrDeleteHeaders, null, BillingProcessWS.class);

            assertNotNull(getBilling, "Response can not be null!!");
            RestValidationHelper.validateStatusCode(getBilling, Response.Status.OK.getStatusCode());

            validateProcesses(getBilling.getBody(), getLastBilling.getBody());

        } finally {
            // ToDo this is not working but it should
            // Delete the invoice
//            if (null != invoiceId) {
//                restTemplate.sendRequest(invoiceRestHelper.getFullRestUrl() + invoiceId, HttpMethod.DELETE,
//                        getOrDeleteHeaders, null);
//            }

//            // Delete the order
//            if (null != orderId) {
//                restTemplate.sendRequest(orderRestHelper.getFullRestUrl() + orderId, HttpMethod.DELETE,
//                        getOrDeleteHeaders, null);
//            }
            // Delete the created user
            if (null != testCustomerId) {
                restTemplate.sendRequest(
                        userRestHelper.getFullRestUrl() + testCustomerId, HttpMethod.DELETE, getOrDeleteHeaders, null);
            }
        }

    }
*/
    private void updateBillingConfig(LocalDate runDate, Boolean review, Integer periodUnitId) {

        BillingProcessConfigurationWS config = restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.GET,
                getOrDeleteHeaders, null, BillingProcessConfigurationWS.class).getBody();
        config.setNextRunDate(Date.from(runDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));
        config.setGenerateReport(review ? 1 : 0);
        config.setRetries(1);
        config.setDaysForRetry(5);
        config.setDfFm(0);
        config.setAutoPaymentApplication(0);
        config.setInvoiceDateProcess(1);
        config.setPeriodUnitId(periodUnitId);
        config.setDueDateUnitId(periodUnitId);
        config.setDueDateValue(1);
        config.setMaximumPeriods(99);
        config.setOnlyRecurring(0);
        config.setProratingType(PRORATING_AUTO);

        restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.PUT,
                postOrPutHeaders, config, BillingProcessConfigurationWS.class);

    }


    private void updateCustomerNID(Integer customerId, LocalDate nextInvoiceDate){

        UserWS userWS = restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId, HttpMethod.GET,
                getOrDeleteHeaders, null, UserWS.class).getBody();
        Date datenew = new Date(nextInvoiceDate.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond() * 1000);

      userWS.setNextInvoiceDate(datenew);

        restTemplate.sendRequest(userRestHelper.getFullRestUrl() + customerId,
                HttpMethod.PUT, postOrPutHeaders, userWS, UserWS.class);
    }

    private OrderWS buildOrder(LocalDate activeSince) {
        Date active = new Date(activeSince.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond() * 1000);
        Date activeUntil = new Date(activeSince.plusMonths(1).atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond()*1000);

        OrderBuilder orderBuilder = OrderBuilder.getBuilderWithoutEnv()
                .forUser(USER_ID)
                .withActiveSince(active)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(active)
                .withPeriod(PERIOD_ID)
                .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                .withProrate(Boolean.FALSE);

        orderBuilder.withOrderLine(orderBuilder.orderLine()
                .withItemId(ITEM_ONE_ID)
                .withQuantity(BigDecimal.ONE)
                .build());

        orderBuilder.withOrderLine(orderBuilder.orderLine()
                .withItemId(ITEM_ONE_ID)
                .withQuantity(BigDecimal.TEN)
                .build());

        return orderBuilder.buildOrder();
    }

    private Integer persistOrder(LocalDate date){
        // Build an order and order changes
        OrderWS order = buildOrder(date);
        Date datenew = new Date(date.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond() * 1000);
        OrderChangeWS[] orderChanges = OrderBuilder.buildFromOrder(order, OC_STATUS_ID,
                datenew);
        OrderInfo orderInfo = new OrderInfo(order, orderChanges);

        // Create order
        return restTemplate.sendRequest(
                orderRestHelper.getFullRestUrl(), HttpMethod.POST, postOrPutHeaders, orderInfo, OrderWS.class).getBody().getId();
    }

    private boolean invoiceContainsOrder(InvoiceWS invoice, Integer orderId) {
        return null != invoice && Arrays.asList(invoice.getOrders()).contains(orderId);
    }

    private void validateProcesses(BillingProcessWS actual, BillingProcessWS expected){

        assertTrue(null != actual && null != expected);

        assertEquals(actual.getId(), expected.getId());
        assertEquals(actual.getPeriodUnitId(), expected.getPeriodUnitId());
        assertEquals(actual.getPeriodValue(), expected.getPeriodValue());
        assertEquals(actual.getBillingDate(), expected.getBillingDate());
        assertEquals(actual.getBillingDateEnd(), expected.getBillingDateEnd());
        assertEquals(actual.getReview(), expected.getReview());
        assertEquals(actual.getEntityId(), expected.getEntityId());
    }

    /**
     * C35232870,C35232866
     */
    @Test
    public void triggerSpecificBillingProcessRun() {
        ResponseEntity<BillingProcessConfigurationWS> fetchedResponse = restTemplate.sendRequest(REST_URL + "/configuration", HttpMethod.GET,
                getOrDeleteHeaders, null, BillingProcessConfigurationWS.class);

        BillingProcessConfigurationWS fetchedConfiguration = fetchedResponse.getBody();
        Date oldDaysForReport = fetchedConfiguration.getNextRunDate();
        LocalDate billingRunDate = oldDaysForReport.toInstant().atZone(ZoneId.of("America/New_York")).toLocalDate().plusDays(2);
        long millis = billingRunDate.atStartOfDay(ZoneId.of("America/New_York")).toEpochSecond();
        updateBillingConfig(billingRunDate, false, MONTHLY_UNIT_ID);
        //   Trigger billing
        ResponseEntity billingResponse = restTemplate.sendRequest(REST_URL + "processes/" + millis,
                HttpMethod.POST, postOrPutHeaders, null, Void.class);

        assertNotNull(billingResponse, "Response can not be null!!");
        RestValidationHelper.validateStatusCode(billingResponse, Response.Status.CREATED.getStatusCode());
    }

    /**
     * C35232871
     */
    @Test
    public void testInvalidBillingProcessId() {
        try {
            ResponseEntity<BillingProcessWS> billingResponse = restTemplate.sendRequest(REST_URL + "processes/" + Integer.MAX_VALUE,
                    HttpMethod.GET, getOrDeleteHeaders, null, BillingProcessWS.class);
            assertNull(billingResponse, "Response should be null!!");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode(), "Billing Process run Id not found");
        }
    }

    /**
     * C35232872
     */
    @Test
    public void retrieveLastBillingProcessRun() {
        ResponseEntity<BillingProcessWS> billingResponse = restTemplate.sendRequest(REST_URL + "processes/last",
                HttpMethod.GET, getOrDeleteHeaders, null, BillingProcessWS.class);
        RestValidationHelper.validateStatusCode(billingResponse, Response.Status.OK.getStatusCode());
    }

    /**
     * C35232867 - Commenting as billing not running as expected
     */
//    @Test
//    public void retrieveLastBillingProcessReview() {
//        ResponseEntity<BillingProcessWS> billingResponse = restTemplate.sendRequest(REST_URL + "processes/review",
//                HttpMethod.GET, getOrDeleteHeaders, null, BillingProcessWS.class);
//        RestValidationHelper.validateStatusCode(billingResponse, Response.Status.OK.getStatusCode());
//    }
}
