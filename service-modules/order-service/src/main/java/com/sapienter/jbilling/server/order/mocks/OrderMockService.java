package com.sapienter.jbilling.server.order.mocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.MediationEventResultList;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeTypeWS;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderWS;

/**
 * Created by andres on 16/10/15.
 */
public class OrderMockService implements OrderService {
    @Override
    public OrderWS getCurrentOrder(Integer userId, Date date) throws SessionInternalError {
        OrderWS currentOrderMock = null;
        if(userId.equals(new Integer(1))) {
            currentOrderMock = new OrderWS();
            currentOrderMock.setId(new Integer(1));
        }
        return currentOrderMock;
    }

    @Override
    public List<OrderWS> lastOrders(Integer userId, int numberOfOrdersToRetrieve) {
        return null;
    }

    @Override
    public List<OrderChangeStatusWS> getOrderChangeStatusesForCompany() {
        OrderChangeStatusWS orderChangeStatusWS = new OrderChangeStatusWS();
        orderChangeStatusWS.setApplyToOrder(ApplyToOrder.YES);
        orderChangeStatusWS.setId(1);
        List<OrderChangeStatusWS> orderChangeStatusWSList = new ArrayList<>();
        orderChangeStatusWSList.add(orderChangeStatusWS);
        return orderChangeStatusWSList;
    }

    @Override
    public List<OrderChangeTypeWS> getOrderChangeTypesForCompany() {
        OrderChangeTypeWS orderChangeTypeWS = new OrderChangeTypeWS();
        orderChangeTypeWS.setDefaultType(true);
        orderChangeTypeWS.setId(1);
        List<OrderChangeTypeWS> OrderChangeTypeWSList = new ArrayList<>();
        OrderChangeTypeWSList.add(orderChangeTypeWS);
        return OrderChangeTypeWSList;
    }

    @Override
    public MediationEventResultList addMediationEventList(List<JbillingMediationRecord> jmrList) {
        MediationEventResultList resultList = new MediationEventResultList();
        for(JbillingMediationRecord jmr : jmrList) {
            MediationEventResult result = addMediationEvent(jmr);
            resultList.addResult(jmr, result);
        }
        return resultList;
    }

    @Override
    public MediationEventResult addMediationEvent(JbillingMediationRecord jmr) {
        return new MediationEventResult();
    }

    @Override
    public void undoMediation(UUID processId) {
        //TODO Empty implementation
    }

    @Override
    public MediationEventResult addMediationEventDistributel(JbillingMediationRecord jmr){
        return null;
    }

    @Override
    public OrderWS getOrder(Integer userId, Integer orderId)  {
        return null;
    }

    @Override
    public void updateOrderStatus(Integer entity, Integer userId, Integer orderId, OrderStatusFlag orderStatusFlag) {

    }
    @Override
    public void updateCustomOrderStatus(Integer entity, Integer userId, Integer orderId, int orderStatus){

    }

    @Override
    public void updateOrderMetafield(Integer entity,  Integer orderId, String metafieldName, Object value){
    }

    @Override
    public boolean isJMRProcessed(JbillingMediationRecord jmr) {
        return true;
    }

}
