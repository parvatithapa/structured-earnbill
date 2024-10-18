//TODO MODULARIZATION: MOVE IN THE MODULES
//*
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
//import com.sapienter.jbilling.server.item.ItemBL;
//import com.sapienter.jbilling.server.item.PricingField;
//import com.sapienter.jbilling.server.item.db.ItemDTO;
//import com.sapienter.jbilling.server.mediation.step.pricing.PricingResolutionStep;
//import com.sapienter.jbilling.server.mediation.task.MediationResult;
//import com.sapienter.jbilling.server.order.db.OrderDTO;
//import com.sapienter.jbilling.server.order.db.OrderLineDTO;
//import com.sapienter.jbilling.server.user.db.UserDTO;
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
//public class PricingResolutionStepTest extends TestCase {
//
//    // mocks
//    private ItemBL mockItemBL = createMock(ItemBL.class);
//
//    private static final Integer ENTITY_ID = 1;
//    private static final Integer USER_ID = 1;
//    private static final String USERNAME = "test_mediation_user";
//    private static final Integer CURRENCY_ID = 1;
//
//    private static final String ITEM_NUMBER = "TST-01";
//    private static final Integer ITEM_ID = 10;
//    private static final BigDecimal ITEM_QUANTITY = new BigDecimal("10.20");
//    private static final BigDecimal ITEM_PRICE = new BigDecimal("100.20");
//
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        reset(mockItemBL);
//    }
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
//    public void testPriceResolution() {
//        OrderLineDTO orderLineDTO = _mockOrderLineDTO(ITEM_ID, ITEM_QUANTITY);
//        OrderDTO currentOrder = _mockOrderDTO(USER_ID, CURRENCY_ID);
//        List<PricingField> pricingFields = buildPricingFields(true, true);
//        expect(mockItemBL.getPrice(USER_ID, CURRENCY_ID, ITEM_QUANTITY, ENTITY_ID, currentOrder, orderLineDTO, true, null))
//                .andReturn(ITEM_PRICE).once();
//        mockItemBL.set(ITEM_ID);
//        mockItemBL.setPricingFields(pricingFields);
//
//        replay(mockItemBL);
//
//        MediationResult result = new MediationResult("test_config", false);
//        result.setCurrentOrder(currentOrder);
//        result.setCurrencyId(CURRENCY_ID);
//        result.setUserId(USER_ID);
//        result.getLines().add(orderLineDTO);
//
//        PricingResolutionStep priceResolutionStep = new PricingResolutionStep();
//        priceResolutionStep.setItemLoader(mockItemBL);
//
//        priceResolutionStep.executeStep(ENTITY_ID, result, pricingFields);
//
//        verify(mockItemBL);
//
//        assertFalse(result.getLines().isEmpty());
//        assertTrue(result.getLines().get(0).getPrice().compareTo(ITEM_PRICE) == 0);
//        assertTrue(result.getLines().get(0).getAmount()
//                .compareTo(ITEM_PRICE.multiply(ITEM_QUANTITY)) == 0);
//
//    }
//
//    private static List<PricingField> buildPricingFields(boolean includeQuantity, boolean includeItem) {
//
//        List<PricingField> pricingFields = new ArrayList<PricingField>();
//        if (includeQuantity) {
//            PricingField.add(pricingFields, new PricingField("quantity", ITEM_QUANTITY));
//        }
//        if (includeItem) {
//            PricingField.add(pricingFields, new PricingField("item_number", ITEM_NUMBER));
//        }
//
//        return pricingFields;
//    }
//}
