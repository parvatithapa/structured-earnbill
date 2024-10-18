//TODO MODULARIZATION: MOVE IN THE MODULES
///*
// JBILLING CONFIDENTIAL
// _____________________
//
// [2003] - [2012] Enterprise jBilling Software Ltd.
// All Rights Reserved.
//
// NOTICE:  All information contained herein is, and remains
// the property of Enterprise jBilling Software.
// The intellectual and technical concepts contained
// herein are proprietary to Enterprise jBilling Software
// and are protected by trade secret or copyright law.
// Dissemination of this information or reproduction of this material
// is strictly forbidden.
// */
//
//package com.sapienter.jbilling.server.mediation.step;
//
//import com.sapienter.jbilling.server.item.PricingField;
//import com.sapienter.jbilling.server.mediation.task.MediationResult;
//import com.sapienter.jbilling.server.order.db.OrderDTO;
//import com.sapienter.jbilling.server.user.db.UserDTO;
//import junit.framework.TestCase;
//import org.joda.time.DateMidnight;
//
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//
//import static org.easymock.classextension.EasyMock.*;
//import static org.easymock.classextension.EasyMock.reset;
//
//
///**
// * Created with IntelliJ IDEA.
// *
// * @author Panche Isajeski
// * @since 12/17/12
// */
//public class CurrentOrderResolutionStepTest extends TestCase {
//
//    private static final Integer ENTITY_ID = 1;
//    private static final Integer USER_ID = 1;
//    private static final String USERNAME = "test_mediation_user";
//    private static final Integer CURRENCY_ID = 1;
//    private static final Date EVENT_DATE = new DateMidnight(2013, 1, 1).toDate();
//
//    private static final String ITEM_NUMBER = "TST-01";
//    private static final Integer ITEM_ID = 10;
//
//    /**
//     * Construct a mock OrderDTO
//     *
//     * @param userId
//     * @param currencyId
//     * @return
//     */
//    private static OrderDTO _mockOrderDTO(Integer userId, Integer currencyId) {
//
//        OrderDTO orderDTO = new OrderDTO();
//        orderDTO.setCurrencyId(currencyId);
//        orderDTO.setBaseUserByUserId(new UserDTO(userId));
//        return orderDTO;
//    }
//
//    public void testValidateDurationOnCurrentOrderResolution() throws Exception {
//
//        CurrentOrderResolutionStep myObject = createMock(CurrentOrderResolutionStep.class,
//                CurrentOrderResolutionStep.class.getMethod("getCurrentOrder",
//                        new Class[] {MediationResult.class}));
//
//        MediationResult result = new MediationResult("test_config", false);
//        result.setUserId(USER_ID);
//        result.setCurrencyId(CURRENCY_ID);
//        result.setEventDate(EVENT_DATE);
//        result.setPersist(false);
//
//        replay(myObject);
//
//        boolean status = myObject.executeStep(ENTITY_ID, result, buildPricingFields(false, false));
//
//        verify(myObject);
//
//        assertFalse(status);
//        assertTrue(result.isDone());
//        assertFalse(result.getErrors().isEmpty());
//        assertEquals(result.getErrors().get(0), "ERR-DURATION");
//
//    }
//
//    public void testValidateDispositionOnCurrentOrderResolution() throws Exception {
//
//        CurrentOrderResolutionStep myObject = createMock(CurrentOrderResolutionStep.class,
//                CurrentOrderResolutionStep.class.getMethod("getCurrentOrder",
//                        new Class[] {MediationResult.class}));
//
//        MediationResult result = new MediationResult("test_config", false);
//        result.setUserId(USER_ID);
//        result.setCurrencyId(CURRENCY_ID);
//        result.setEventDate(EVENT_DATE);
//        result.setPersist(false);
//
//        replay(myObject);
//
//        boolean status = myObject.executeStep(ENTITY_ID, result, buildPricingFields(true, false));
//
//        verify(myObject);
//
//        assertFalse(status);
//        assertTrue(result.isDone());
//        assertTrue(result.getErrors().isEmpty());
//
//    }
//
//    public void testFullCurrentOrderResolution() throws Exception {
//
//        CurrentOrderResolutionStep myObject = createMock(CurrentOrderResolutionStep.class,
//                CurrentOrderResolutionStep.class.getMethod("getCurrentOrder",
//                        new Class[] {MediationResult.class}));
//
//        MediationResult result = new MediationResult("test_config", false);
//        result.setUserId(USER_ID);
//        result.setCurrencyId(CURRENCY_ID);
//        result.setEventDate(EVENT_DATE);
//        result.setPersist(false);
//
//        OrderDTO currentOrder = _mockOrderDTO(USER_ID, CURRENCY_ID);
//        expect(myObject.getCurrentOrder(result)).andReturn(currentOrder).once();
//
//        replay(myObject);
//
//        boolean status = myObject.executeStep(ENTITY_ID, result, buildPricingFields(true, true));
//
//        verify(myObject);
//
//        assertTrue(status);
//        assertTrue(result.getErrors().isEmpty());
//        assertNotNull(result.getCurrentOrder());
//        assertEquals(result.getCurrentOrder(), currentOrder);
//
//    }
//
//    private static List<PricingField> buildPricingFields(boolean validDuration, boolean validDisposition) {
//
//        List<PricingField> pricingFields = new ArrayList<PricingField>();
//
//        PricingField.add(pricingFields, new PricingField("duration", validDuration ? 10 : -5));
//        PricingField.add(pricingFields, new PricingField("disposition", validDisposition
//                ? "ANSWERED" : "REJECTED"));
//
//        return pricingFields;
//    }
//}
