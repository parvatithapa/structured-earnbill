package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

/**
 */
public class DiscountRestTest extends RestTestCase {

    private static final Integer ENTITY_ID = Integer.valueOf(1);
    private static final Logger logger = LoggerFactory.getLogger(DiscountRestTest.class);

    @BeforeClass
    public void setup(){
        super.setup("discounts");
    }

    @Test
    public void createUpdateDeleteDiscount() {

        // Build an order status
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));

        ResponseEntity<DiscountWS> postResponse = null;

        try {
            // Create the discount
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
            logger.debug("Created discount with id {}", postResponse.getBody().getId());

            // Verify that the discount is generated
            ResponseEntity<DiscountWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);

            assertNotNull(getResponse, "GET response should not be null.");
            assertEquals(BigDecimal.TEN.intValue(), getResponse.getBody().getRateAsDecimal().intValue());

            //change details and update the discount
            discount = getResponse.getBody();
            discount.setRate(BigDecimal.ONE);
            getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            assertNotNull(getResponse, "GET response should not be null.");

            //verify udpate worked
            getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
            assertEquals(BigDecimal.ONE.intValue(), getResponse.getBody().getRateAsDecimal().intValue());
        } finally {
            // Delete the discount
            if (null != postResponse) {
                logger.debug("postResponse: {}", postResponse);
                logger.debug("postResponse.getBody(): {}", postResponse.getBody());

                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }


    @Test
    public void createDuplicateDiscount() {
        // Build an order status
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestDup" + new SimpleDateFormat("SSS").format(new Date()));

        ResponseEntity<DiscountWS> postResponse = null;

        try {
            // Create the discount
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Create duplicate discount
            restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            fail("Duplicate discount should fail");
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            logger.debug("error {}", errorMsg);
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("DiscountDTO,code,discount.error.code.already.exists"));
        } finally {
            // Delete the discount
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    @Test
    public void updateDiscountError() throws Exception {
//        // Build an order status
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestErr" + new SimpleDateFormat("SSS").format(new Date()));

        ResponseEntity<DiscountWS> postResponse = null;

        try {
            // Discount with id -100 should not exist
            discount.setId(-100);
            restTemplate.sendRequest(REST_URL + "-100",
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            fail("Duplicate discount should fail");
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            logger.debug("error {}", errorMsg);
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }

        try {
            // Create the discount
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());
            discount.setId(postResponse.getBody().getId());

            // Discount invalid rate
            discount.setRate(BigDecimal.ZERO);
            restTemplate.sendRequest(REST_URL + discount.getId(),
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            fail("Invalid rate");
        } catch (HttpClientErrorException e) {
            String errorMsg = e.getResponseBodyAsString();
            logger.debug("error {}", errorMsg);
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("DiscountDTO,rate,discount.error.rate.can.not.be.zero.or.less"));
        } finally {
            // Delete the discount
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    /**
     * C37927960
     */
    @Test
    public void deleteNonExistingDiscount() {

        // Try to delete a non-existing order status and verify the response status
        try {
            restTemplate.sendRequest(REST_URL + "9999999", HttpMethod.DELETE, getOrDeleteHeaders,
                    null);
            fail("The test should fail before this line.");
        } catch (HttpStatusCodeException e){
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C37925981
     */
    @Test
    public void createDiscountWithInvalidId() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        discount.setId(Integer.MAX_VALUE);
        try {
            // Create the discount with long id
            restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * C37926008
     */
    @Test
    public void createDiscountWithInvalidCode() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        discount.setCode("RestCRUDOperations-" + Integer.MAX_VALUE);
        try {
            restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertTrue(errorMsg.contains("DiscountWS,code,validation.error.size,1,20"));
        }
    }

    /**
     * C37926009
     */
    @Test
    public void createDiscountWithInvalidType() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        discount.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name()+"123");
        try {
            restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("DiscountDTO,type,discount.type.must.selected"));
        }
    }

    /**
     * C37926010
     */
    @Test
    public void createDiscountWithInvalidRate() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        discount.setRate("-1000000000000000000.00");
        try {
            restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }

    /**
     * C37927663
     */
    @Test
    public void getDiscountWithInvalidId() {
        try {
            // Get the discount with invalid id
            ResponseEntity<DiscountWS> getResponse = restTemplate.sendRequest(REST_URL + Integer.MAX_VALUE,
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);

            assertNull(getResponse, "GET response should be null.");
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.NOT_FOUND.getStatusCode());
        }
    }

    /**
     * C37927963
     */
    @Test
    public void updateDiscountWithInvalidId() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        ResponseEntity<DiscountWS> postResponse = null;

        try {
            // Create the discount
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the discount is generated
            ResponseEntity<DiscountWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);

            assertNotNull(getResponse, "GET response should not be null.");
            assertEquals(BigDecimal.TEN.intValue(), getResponse.getBody().getRateAsDecimal().intValue());

            //update discount with Invalid Id
            discount = getResponse.getBody();
            discount.setId(99999);
            getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            assertNotNull(getResponse, "GET response should not be null.");

            //verify update worked
            getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);
            assertNotNull(getResponse, "GET response should not be null.");
        } finally {
            // Delete the discount
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    /**
     * C37927964
     */
    @Test
    public void updateDiscountWithInvalidCode() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        ResponseEntity<DiscountWS> postResponse = null;
        try {
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            //update discount with Invalid Code
            discount = postResponse.getBody();
            discount.setCode("RestUpdateOperation-123");
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            assertTrue(errorMsg.contains("DiscountWS,code,validation.error.size,1,20"));
        } finally {
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    /**
     * C37927965
     */
    @Test
    public void updateDiscountWithInvalidType() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        ResponseEntity<DiscountWS> postResponse = null;
        try {
            // Create the discount
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            // Verify that the discount is generated
            ResponseEntity<DiscountWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);

            assertNotNull(getResponse, "GET response should not be null.");
            //update discount with Invalid Type
            discount = getResponse.getBody();
            discount.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name()+"123");
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            String errorMsg = e.getResponseBodyAsString();
            assertEquals(e.getStatusCode().value(), Response.Status.BAD_REQUEST.getStatusCode());
            assertTrue(errorMsg.contains("DiscountDTO,type,discount.type.must.selected"));
        } finally {
            // Delete the discount
            if (null != postResponse) {
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }

    /**
     * C37927966
     */
    @Test
    public void updateDiscountWithInvalidRate() {
        DiscountWS discount = CreateObjectUtil.createAmountDiscount(new Date(), "RestCRUD-" + new SimpleDateFormat("SSS").format(new Date()));
        ResponseEntity<DiscountWS> postResponse = null;
        try {
            postResponse = restTemplate.sendRequest(REST_URL,
                    HttpMethod.POST, postOrPutHeaders, discount, DiscountWS.class);
            RestValidationHelper.validateStatusCode(postResponse, Response.Status.CREATED.getStatusCode());

            ResponseEntity<DiscountWS> getResponse = restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.GET, getOrDeleteHeaders, null, DiscountWS.class);

            assertNotNull(getResponse, "GET response should not be null.");
            //update discount with Invalid Type
            discount = getResponse.getBody();
            discount.setRate("-1000000000000000000.00");
            restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(),
                    HttpMethod.PUT, postOrPutHeaders, discount, DiscountWS.class);
            fail("No no");
        } catch (HttpStatusCodeException e) {
            assertEquals(e.getStatusCode().value(), Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        } finally {
            // Delete the discount
            if (null != postResponse) {
                logger.debug("postResponse.getBody(): {}", postResponse.getBody());
                restTemplate.sendRequest(REST_URL + postResponse.getBody().getId(), HttpMethod.DELETE,
                        getOrDeleteHeaders, null);
            }
        }
    }
}
