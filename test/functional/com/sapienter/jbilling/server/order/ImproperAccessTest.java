/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.order;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

@Test(testName = "order.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final Integer GANDALF_ORDER_ID = 1;
    private static final Integer GANDALF_USER_ID = 2;
    private static final Integer GANDALF_ORDER_PERIOD_ID = 2;
    private static final Integer GANDALF_ORDER_STATUS_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;

    @Test
    public void testGetOrder() {
        // Cross Company
        try {
            capsuleAdminApi.getOrder(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrder(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrder(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateOrder() {
        OrderWS order = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID);
        OrderChangeWS[] orderChanges = oscorpCustomerApi.getOrderChanges(GANDALF_ORDER_ID);

        // Cross Company
        try {
            capsuleAdminApi.updateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testCreateUpdateOrder() {
        OrderWS order = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID);
        OrderChangeWS[] orderChanges = oscorpCustomerApi.getOrderChanges(GANDALF_ORDER_ID);

        // Cross Company
        try {
            capsuleAdminApi.createUpdateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createUpdateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createUpdateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testDeleteOrder() {
        // Cross Company
        try {
            capsuleAdminApi.deleteOrder(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.deleteOrder(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.deleteOrder(GANDALF_ORDER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetCurrentOrder() {
        Date date = createDate(Calendar.JULY, 26, 2006);

        // Cross Company
        try {
            capsuleAdminApi.getCurrentOrder(GANDALF_USER_ID, date);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getCurrentOrder(GANDALF_USER_ID, date);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getCurrentOrder(GANDALF_USER_ID, date);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateCurrentOrder() {
        Date date = createDate(Calendar.JULY, 26, 2006);

        OrderWS currentOrder = oscorpCustomerApi.getCurrentOrder(GANDALF_USER_ID, date);

        // Cross Company
        try {
            capsuleAdminApi.updateCurrentOrder(GANDALF_USER_ID, currentOrder.getOrderLines(), null, date, null);
            fail(String.format("Unauthorized access to current order by user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateCurrentOrder(GANDALF_USER_ID, currentOrder.getOrderLines(), null, date, null);
            fail(String.format("Unauthorized access to current order by user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateCurrentOrder(GANDALF_USER_ID, currentOrder.getOrderLines(), null, date, null);
            fail(String.format("Unauthorized access to current order by user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetOrderLine() {
        Integer orderLineId = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID).getOrderLines()[0].getId();

        // Cross Company
        try {
            capsuleAdminApi.getOrderLine(orderLineId);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, orderLineId));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrderLine(orderLineId);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, orderLineId));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrderLine(orderLineId);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, orderLineId));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateOrderLine() {
        OrderLineWS orderLine = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID).getOrderLines()[0];

        // Cross Company
        try {
            capsuleAdminApi.updateOrderLine(orderLine);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, orderLine.getId()));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateOrderLine(orderLine);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, orderLine.getId()));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateOrderLine(orderLine);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, orderLine.getId()));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetOrderByPeriod() {
        // Cross Company
        try {
            capsuleAdminApi.getOrderByPeriod(GANDALF_USER_ID, Constants.ORDER_PERIOD_ONCE);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrderByPeriod(GANDALF_USER_ID, Constants.ORDER_PERIOD_ONCE);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrderByPeriod(GANDALF_USER_ID, Constants.ORDER_PERIOD_ONCE);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetLatestOrder() {
        // Cross Company
        try {
            capsuleAdminApi.getLatestOrder(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLatestOrder(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLatestOrder(GANDALF_USER_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetLastOrders() {
        // Cross Company
        try {
            capsuleAdminApi.getLastOrders(GANDALF_USER_ID, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLastOrders(GANDALF_USER_ID, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLastOrders(GANDALF_USER_ID, 1);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserOrdersPage() {
        // Cross Company
        try {
            capsuleAdminApi.getUserOrdersPage(GANDALF_USER_ID, 10, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserOrdersPage(GANDALF_USER_ID, 10, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserOrdersPage(GANDALF_USER_ID, 10, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testRateOrder() {
        OrderWS order = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID);
        OrderChangeWS[] orderChanges = oscorpCustomerApi.getOrderChanges(GANDALF_ORDER_ID);

        // Cross Company
        try {
            capsuleAdminApi.rateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.rateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.rateOrder(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testRateOrders() {
        OrderWS[] orders = oscorpCustomerApi.getUserOrdersPage(GANDALF_USER_ID, 1, 0);
        OrderChangeWS[] orderChanges = oscorpCustomerApi.getOrderChanges(orders[0].getId());

        // Cross Company
        try {
            capsuleAdminApi.rateOrders(orders, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.rateOrders(orders, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.rateOrders(orders, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testCreateOrderPreAuthorize() {
        OrderWS order = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID);
        OrderChangeWS[] orderChanges = oscorpCustomerApi.getOrderChanges(GANDALF_ORDER_ID);

        // Cross Company
        try {
            capsuleAdminApi.createOrderPreAuthorize(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.createOrderPreAuthorize(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.createOrderPreAuthorize(order, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    //TODO: Review calculateSwapPlanChanges 2nd and 3rd parameters, existingPlanItemId and swapPlanItemId.
    public void testCalculateSwapPlanChanges() {
        OrderWS order = oscorpCustomerApi.getOrder(GANDALF_ORDER_ID);
        Date effectiveDate = createDate(Calendar.JULY, 26, 2006);

        // Cross Company
        try {
            capsuleAdminApi.calculateSwapPlanChanges(order, 1, 2, SwapMethod.DEFAULT, effectiveDate);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.calculateSwapPlanChanges(order, 1, 2, SwapMethod.DEFAULT, effectiveDate);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.calculateSwapPlanChanges(order, 1, 2, SwapMethod.DEFAULT, effectiveDate);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testUpdateOrders() {
        OrderWS[] orders = oscorpCustomerApi.getUserOrdersPage(GANDALF_USER_ID, 1, 0);
        OrderChangeWS[] orderChanges = oscorpCustomerApi.getOrderChanges(orders[0].getId());

        // Cross Company
        try {
            capsuleAdminApi.updateOrders(orders, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.updateOrders(orders, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.updateOrders(orders, orderChanges);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_ORDER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetLastOrdersPage() {
        // Cross Company
        try {
            capsuleAdminApi.getLastOrdersPage(GANDALF_USER_ID, 10, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getLastOrdersPage(GANDALF_USER_ID, 10, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getLastOrdersPage(GANDALF_USER_ID, 10, 0);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetOrdersByDate() {
        Date since = createDate(Calendar.JULY, 26, 2006);
        Date until = createDate(Calendar.AUGUST, 1, 2006);

        // Cross Company
        try {
            capsuleAdminApi.getOrdersByDate(GANDALF_USER_ID, since, until);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getOrdersByDate(GANDALF_USER_ID, since, until);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getOrdersByDate(GANDALF_USER_ID, since, until);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testGetUserSubscriptions() {
        // Cross Company
        try {
            capsuleAdminApi.getUserSubscriptions(GANDALF_USER_ID);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }

        // Cross Customer (child user)
        try {
            frenchSpeakerApi.getUserSubscriptions(GANDALF_USER_ID);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, FRENCH_SPEAKER_LOGIN)));
        }

        // Cross Customer (another user)
        try {
            pendunsus1Api.getUserSubscriptions(GANDALF_USER_ID);
            fail(String.format("Unauthorized access to current order, user ID %d", GANDALF_USER_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE,ex.getMessage(), containsString(String.format(CROSS_CUSTOMER_ERROR_MSG, PRANCING_PONY_ENTITY_ID, 2, PENDUNSUS_LOGIN)));
        }
    }

    @Test
    public void testDeleteOrderPeriod() {
        // Cross Company
        try {
            capsuleAdminApi.deleteOrderPeriod(GANDALF_ORDER_PERIOD_ID);
            fail(String.format("Unauthorized access to order period ID %d", GANDALF_ORDER_PERIOD_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateOrderPeriods() {
        OrderPeriodWS[] orderPeriods = oscorpCustomerApi.getOrderPeriods();

        // Cross Company
        try {
            capsuleAdminApi.updateOrderPeriods(orderPeriods);
            fail("Unauthorized access!");
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testUpdateOrCreateOrderPeriod() {
        OrderPeriodWS[] orderPeriods = oscorpCustomerApi.getOrderPeriods();

        // Cross Company
        try {
            capsuleAdminApi.updateOrCreateOrderPeriod(orderPeriods[0]);
            fail("Unauthorized access!");
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeleteOrderStatus() {
        OrderStatusWS orderStatus = oscorpAdminApi.findOrderStatusById(GANDALF_ORDER_STATUS_ID);

        // Cross Company
        try {
            capsuleAdminApi.deleteOrderStatus(orderStatus);
            fail(String.format("Unauthorized access to order status ID %d", GANDALF_ORDER_STATUS_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testCreateUpdateOrderStatus() {
        OrderStatusWS orderStatus = oscorpAdminApi.findOrderStatusById(GANDALF_ORDER_STATUS_ID);

        // Cross Company
        try {
            capsuleAdminApi.createUpdateOrderStatus(orderStatus);
            fail(String.format("Unauthorized access to order status ID %d", GANDALF_ORDER_STATUS_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testFindOrderStatusById() {
        // Cross Company
        try {
            capsuleAdminApi.findOrderStatusById(GANDALF_ORDER_STATUS_ID);
            fail(String.format("Unauthorized access to order status ID %d", GANDALF_ORDER_STATUS_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetDefaultOrderStatusId() {
        // Cross Company
        try {
            capsuleAdminApi.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, 1);
            fail(String.format("Unauthorized access to order status ID %d", GANDALF_ORDER_STATUS_ID));
        } catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    private Date createDate(int month, int day, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year + 1900, month, day);
        return calendar.getTime();
    }
}
