package com.sapienter.jbilling.rest;

import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertNotNull;

/**
 * @author Vojislav Stanojevikj
 * @since 13-Oct-2016.
 */
final class RestValidationHelper {

    private RestValidationHelper() {}


    /**
     * Validates if the status code in the
     * <code>response</code> matches with the
     * code supplied in <code>expectedCode</code>.
     * @param response {@link ResponseEntity} representation of the response
     * @param expectedCode value for which the validation will go on.
     * @param <T> the body type in the response.
     */
    public static <T> void validateStatusCode(ResponseEntity<T> response, int expectedCode){
        assertNotNull(response.getStatusCode(), "Status code can not be null!");
        assertEquals(Integer.valueOf(response.getStatusCode().value()), Integer.valueOf(expectedCode), "Invalid status code!!");
    }

    /**
     * Checks if the <code>array</code> contains
     * all the <code>elements</code>.
     * The types of the array and elements
     * must have proper equals and hash code algorithms implemented.
     * @param array
     * @param elements
     * @param <T>
     * @return
     */
    @SafeVarargs
    public static <T> boolean arrayContainsAllElements(T[] array, T... elements){

        if (null == array || array.length == 0){
            return false;
        }
        if (null == elements || elements.length == 0){
            return false;
        }
        if (array.length < elements.length){
            throw new IllegalArgumentException("The number of elements is greater than the array length!");
        }

        List<T> convertedArray = Arrays.asList(array);
        for (T element : elements){
            if (!convertedArray.contains(element)){
                return false;
            }
        }
        return true;
    }

    public static void validatePayments(PaymentWS actual, PaymentWS expected){

        assertTrue(null != actual && null != expected, "Payments can not be null");
        assertEquals(actual.getId(), expected.getId(), "Ids do not match!");
        assertEquals(actual.getUserId(), expected.getUserId(), "User Ids do not match!");
        assertEquals(actual.getMethod(), expected.getMethod(), "Methods do not match!");
        assertEquals(actual.getCurrencyId(), expected.getCurrencyId(), "Currency Ids do not match!");
        assertEquals(actual.getPaymentDate(), expected.getPaymentDate(), "Payment dates do not match!");
        assertEquals(actual.getCreateDatetime(), expected.getCreateDatetime(), "Create dates do not match!");
        assertEquals(actual.getBalanceAsDecimal().setScale(4, BigDecimal.ROUND_CEILING),
                expected.getBalanceAsDecimal().setScale(4, BigDecimal.ROUND_CEILING), "Balances do not match!");
        assertEquals(actual.getAmountAsDecimal().setScale(4, BigDecimal.ROUND_CEILING),
                expected.getAmountAsDecimal().setScale(4, BigDecimal.ROUND_CEILING), "Amounts do not match!");
        assertEquals(actual.getDeleted(), expected.getDeleted(), "Delete flags do not match!");
        assertEquals(actual.getIsRefund(), expected.getIsRefund(), "Is refund flags do not match!");
        assertEquals(actual.getPaymentNotes(), expected.getPaymentNotes(), "Payment notes do not match!");
        validatePaymentInstruments(actual.getPaymentInstruments(), expected.getPaymentInstruments());
        validatePaymentInstruments(actual.getUserPaymentInstruments(), expected.getUserPaymentInstruments());
    }

    public static void validatePaymentInstruments(List <PaymentInformationWS> actualList, List<PaymentInformationWS> expectedList){
        assertTrue(null != actualList && null != expectedList, "Payment information can not be null");
        assertEquals(actualList.size(), expectedList.size(), "Size do not match!");
        for (int i = 0; i < actualList.size(); i++){
            PaymentInformationWS actual = actualList.get(i);
            PaymentInformationWS expected = expectedList.get(i);
            assertEquals(actual.getId(), expected.getId(), "Payment information ids do not match");
            assertEquals(actual.getUserId(), expected.getUserId(), "Payment information user ids do not match");
        }
    }
}
