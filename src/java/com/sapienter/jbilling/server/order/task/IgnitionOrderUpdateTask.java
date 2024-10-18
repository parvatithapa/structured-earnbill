package com.sapienter.jbilling.server.order.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.IgnitionOrderStatusEvent;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * Created by Taimoor Choudhary on 9/5/17.
 */
public class IgnitionOrderUpdateTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(IgnitionOrderUpdateTask.class);

    // Subscribed Events
    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] {
            IgnitionPaymentFailedEvent.class
    };

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    @Override
    public void process(Event event) throws PluggableTaskException{

        if (event instanceof IgnitionPaymentFailedEvent) {
            IgnitionPaymentFailedEvent paymentFailedEvent = (IgnitionPaymentFailedEvent) event;

            PaymentDTOEx payment = paymentFailedEvent.getPayment();

            // Get User's Order
            Integer userId = payment.getUserId();
            List<OrderDTO> orderDTOList = new OrderDAS().findAllUserByUserId(userId);

            if (CollectionUtils.isEmpty(orderDTOList)) {
                logger.debug("No Order found for the given User: %s", userId);
                return;
            }

            OrderDTO orderDTO = orderDTOList.get(0);

            OrderLineDTO orderLineDTO = orderDTO.getLines().get(0);
            ItemDTO item = orderLineDTO.getItem();

            logger.debug("Order found: %s, linking to the Item: %s", orderDTO.getId(), item.getId());

            Integer numberOfTimesToLapse = 0;
            for (MetaFieldValue metaFieldValue : item.getMetaFields()) {
                if (metaFieldValue.getField().getName().equals(IgnitionConstants.NUMBER_OF_TIMES_TO_LAPSE)) {
                    numberOfTimesToLapse = Integer.valueOf(String.valueOf(metaFieldValue.getValue()));
                    break;
                }
            }

            IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            Integer[] lastPayments = webServicesSessionBean.getLastPayments(userId, numberOfTimesToLapse);

            boolean consecutiveFailedPaymentsFound = true;

            if(lastPayments != null && lastPayments.length == numberOfTimesToLapse) {
                for (Integer paymentId : lastPayments) {

                    Integer paymentResultIdForPayment = new PaymentDAS().getPaymentResultIdForPayment(userId, paymentId);

                    if (!paymentResultIdForPayment.equals(Constants.PAYMENT_RESULT_FAILED)) {
                        consecutiveFailedPaymentsFound = false;

                        logger.debug("Payment: %s didn't fail.", paymentId);
                        break;
                    }
                }
            }else{
                consecutiveFailedPaymentsFound = false;

                logger.debug("Required Number of Payments didn't fail.");
            }


            logger.debug("Number of times lapse allowed: %s. Consecutive failed payments found: %s", numberOfTimesToLapse, consecutiveFailedPaymentsFound);

            // Check and Update Order Status flag
            if (consecutiveFailedPaymentsFound) {

                List<OrderStatusDTO> orderStatusDTOList = new OrderStatusDAS().findAll(this.getEntityId());

                orderStatusDTOList.forEach(orderStatusDTO -> {
                    if (orderStatusDTO.getDescription(webServicesSessionBean.getCallerLanguageId()).equals(IgnitionConstants.ORDER_STATUS_LAPSED)) {
                        int orderStatusId = orderStatusDTO.getId();

                        logger.debug("Updating Order with Order Status Id: %s.", orderStatusId);

                        // Update Order Status to 'Lapsed'
                        orderDTO.setStatusId(orderStatusId);

                        // Update Order DTO
                        new OrderDAS().save(orderDTO);

                        // Raise Ignition Custom Event to notify for order being lapsed.
                        IgnitionOrderStatusEvent ignitionOrderStatusEvent = new IgnitionOrderStatusEvent(this.getEntityId(), userId, orderStatusId, orderDTO.getId());
                        EventManager.process(ignitionOrderStatusEvent);
                    }
                });
            }
        }
    }
}
