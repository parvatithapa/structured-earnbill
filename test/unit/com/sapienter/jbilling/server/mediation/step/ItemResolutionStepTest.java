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
//import com.sapienter.jbilling.server.item.db.ItemDTO;
//import com.sapienter.jbilling.server.mediation.step.item.ItemResolutionStep;
//import com.sapienter.jbilling.server.mediation.task.MediationResult;
//import com.sapienter.jbilling.server.order.db.OrderLineDTO;
//import junit.framework.TestCase;
//
//import java.math.BigDecimal;
//import java.util.ArrayList;
//import java.util.List;
//
//import static org.easymock.classextension.EasyMock.*;
//
///**
// * Created with IntelliJ IDEA.
// *
// * @author Panche Isajeski
// * @since 12/17/12
// */
//public class ItemResolutionStepTest extends TestCase {
//
//    private static final Integer ENTITY_ID = 1;
//    private static final Integer CURRENCY_ID = 1;
//
//    private static final String ITEM_NUMBER = "TST-01";
//    private static final Integer LD_CALL_ITEM_ID = 2800;
//
//    private static final BigDecimal CALL_DURATION = new BigDecimal("10.20");
//    private static final String CALL_DESTINATION = "+389144500";
//
//    /**
//     *
//     * @param itemId
//     * @param quantity
//     * @return
//     */
//    private static OrderLineDTO _mockOrderLineDTO(Integer itemId, BigDecimal quantity) {
//
//        OrderLineDTO orderLineDTO = new OrderLineDTO();
//        orderLineDTO.setItem(new ItemDTO(itemId));
//        orderLineDTO.setQuantity(quantity);
//        orderLineDTO.setDefaults();
//        return orderLineDTO;
//    }
//
//    public void testResolveItem() throws Exception {
//
//        ItemResolutionStep itemResolutionStep = createMock(ItemResolutionStep.class,
//                ItemResolutionStep.class.getMethod("newLine",
//                        new Class[] {Integer.class, BigDecimal.class}));
//
//
//        OrderLineDTO orderLineDTO = _mockOrderLineDTO(LD_CALL_ITEM_ID, CALL_DURATION);
//        expect(itemResolutionStep.newLine(LD_CALL_ITEM_ID, CALL_DURATION)).andReturn(
//                orderLineDTO).once();
//
//        replay(itemResolutionStep);
//
//        MediationResult result = new MediationResult("test_config", false);
//        itemResolutionStep.setItemId(LD_CALL_ITEM_ID);
//        itemResolutionStep.executeStep(ENTITY_ID, result, buildPricingFields());
//
//        verify(itemResolutionStep);
//
//        assertNotNull(result.getDescription());
//        assertFalse(result.getLines().isEmpty());
//        assertEquals(result.getLines().get(0), orderLineDTO);
//        assertEquals(result.getLines().get(0).getItemId(), LD_CALL_ITEM_ID);
//    }
//
//    private static List<PricingField> buildPricingFields() {
//
//        List<PricingField> pricingFields = new ArrayList<PricingField>();
//        PricingField.add(pricingFields, new PricingField("duration", CALL_DURATION));
//        PricingField.add(pricingFields, new PricingField("dst", CALL_DESTINATION));
//
//        return pricingFields;
//    }
//}
