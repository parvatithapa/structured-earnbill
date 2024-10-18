package com.sapienter.jbilling.server.paymentMethod;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.test.BaseImproperAccessTest;
import org.junit.Assert;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

/**
 * Created by Fernando G. Morales on 8/13/15.
 */
@Test(testName = "paymentMethod.ImproperAccessTest")
public class ImproperAccessTest extends BaseImproperAccessTest {

    private static final int PRANCING_PONY_PAYMENT_METHOD_TYPE_ID = 1;
    private static final int GANDALF_PAYMENT_INFORMATION_ID = 1;
    private static final int PRANCING_PONY_ENTITY_ID = 1;


    @Test
    public void testUpdatePaymentMethodType() {
        PaymentMethodTypeWS paymentMethodTypeWS = oscorpAdminApi.getPaymentMethodType(PRANCING_PONY_PAYMENT_METHOD_TYPE_ID);
        // Cross Company
        try {
            capsuleAdminApi.updatePaymentMethodType(paymentMethodTypeWS);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_PAYMENT_METHOD_TYPE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testDeletePaymentMethodType() {
        // Cross Company
        try {
            capsuleAdminApi.deletePaymentMethodType(PRANCING_PONY_PAYMENT_METHOD_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_PAYMENT_METHOD_TYPE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    @Test
    public void testGetPaymentMethodType() {
        // Cross Company
        try {
            capsuleAdminApi.getPaymentMethodType(PRANCING_PONY_PAYMENT_METHOD_TYPE_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, PRANCING_PONY_PAYMENT_METHOD_TYPE_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }

    public void testRemovePaymentInstrument() {
        // Cross Company
        try {
            capsuleAdminApi.removePaymentInstrument(GANDALF_PAYMENT_INFORMATION_ID);
            fail(String.format(UNAUTHORIZED_ACCESS_TO_ID, GANDALF_PAYMENT_INFORMATION_ID));
        }
        catch (SecurityException | SessionInternalError ex) {
            Assert.assertThat(INVALID_ERROR_MESSAGE, ex.getMessage(), containsString(String.format(CROSS_COMPANY_ERROR_MSG, PRANCING_PONY_ENTITY_ID, MORDOR_LOGIN)));
        }
    }
}
