package com.sapienter.jbilling.server.order;

import java.io.File;
import java.util.*;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.JMRQuantity;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

/**
 * Created by marcolin on 12/10/15.
 */
public interface OrderService {
    public static final int ORDER_PERIOD_ONCE_ID = 1;
    public static final String BEAN_NAME = "orderService";

    OrderWS getCurrentOrder(Integer userId, Date date) throws SessionInternalError;

    List<OrderWS> lastOrders(Integer userId, int numberOfOrdersToRetrieve);

    List<OrderChangeStatusWS> getOrderChangeStatusesForCompany();

    List<OrderChangeTypeWS> getOrderChangeTypesForCompany();

    MediationEventResultList addMediationEventList(List<JbillingMediationRecord> jmrList);

    MediationEventResult addMediationEvent(JbillingMediationRecord jmr);

    void undoMediation(UUID processId);

    MediationEventResult addMediationEventDistributel(JbillingMediationRecord jmr);

    OrderWS getOrder(Integer userId, Integer orderId);

    void updateOrderStatus(Integer entity, Integer userId, Integer orderId, OrderStatusFlag orderStatusFlag);

    void updateCustomOrderStatus(Integer entity, Integer userId, Integer orderId, int orderStatus);

    default JMRQuantity resolveQuantity(JbillingMediationRecord jmr, PricingField[] fields) {
        return JMRQuantity.NONE;
    }

    void updateOrderMetafield(Integer entity, Integer orderId, String metafieldName, Object value);

    public Map<Integer, List<OrderWS>> getOrderForCustomInvoice(File csvFile);
}
