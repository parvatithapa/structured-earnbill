package com.sapienter.jbilling.server.payment.tasks.paysafe;

import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Created by Matías Cabezas on 08/11/17.
 */
public class PaySafePaymentNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(PaySafePaymentNotificationTask.class);

    private static final ParameterDescription PAYMENT_RESULT_SUCCESFUL_NOTIFICATION_ID = new ParameterDescription("Payment Succesful Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription PAYMENT_RESULT_FAILURE_NOTIFICATION_ID = new ParameterDescription("Payment Failure Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription PAYMENT_RESULT_DISABLED_NOTIFICATION_ID = new ParameterDescription("Payment Disabled Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription PAYMENT_RESULT_CANCELLED_NOTIFICATION_ID = new ParameterDescription("Payment Cancelled Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription REFUND_RESULT_SUCCESFUL_NOTIFICATION_ID = new ParameterDescription("Refund Succesful Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription REFUND_RESULT_FAILURE_NOTIFICATION_ID = new ParameterDescription("Refund Failure Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription REFUND_RESULT_DISABLED_NOTIFICATION_ID = new ParameterDescription("Refund Disabled Notification Id", false, ParameterDescription.Type.STR, false);
    private static final ParameterDescription REFUND_RESULT_CANCELLED_NOTIFICATION_ID = new ParameterDescription("Refund Cancelled Notification Id", false, ParameterDescription.Type.STR, false);

    //initializer for pluggable params
    {
        descriptions.add(PAYMENT_RESULT_SUCCESFUL_NOTIFICATION_ID);
        descriptions.add(PAYMENT_RESULT_FAILURE_NOTIFICATION_ID);
        descriptions.add(PAYMENT_RESULT_DISABLED_NOTIFICATION_ID);
        descriptions.add(PAYMENT_RESULT_CANCELLED_NOTIFICATION_ID);
        descriptions.add(REFUND_RESULT_SUCCESFUL_NOTIFICATION_ID);
        descriptions.add(REFUND_RESULT_FAILURE_NOTIFICATION_ID);
        descriptions.add(REFUND_RESULT_DISABLED_NOTIFICATION_ID);
        descriptions.add(REFUND_RESULT_CANCELLED_NOTIFICATION_ID);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[]{
            PaySafeProcessedPaymentEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        if (event instanceof PaySafeProcessedPaymentEvent) {
            PaySafeProcessedPaymentEvent paySafeProcessedPaymentEvent = (PaySafeProcessedPaymentEvent) event;
            boolean isRefund = paySafeProcessedPaymentEvent.getPaymentDTOEx().getIsRefund() == 1;
            String notificationId = null;
            switch (paySafeProcessedPaymentEvent.getResult()) {
                case SUCESSFUL: {
                    if (isRefund) {
                        notificationId = parameters.get(REFUND_RESULT_SUCCESFUL_NOTIFICATION_ID.getName());
                    } else {
                        notificationId = parameters.get(PAYMENT_RESULT_SUCCESFUL_NOTIFICATION_ID.getName());
                    }
                    break;
                }
                case FAILURE: {
                    if (isRefund) {
                        notificationId = parameters.get(REFUND_RESULT_FAILURE_NOTIFICATION_ID.getName());
                    } else {
                        notificationId = parameters.get(PAYMENT_RESULT_FAILURE_NOTIFICATION_ID.getName());
                    }
                    break;
                }
                case DISABLED: {
                    if (isRefund) {
                        notificationId = parameters.get(REFUND_RESULT_DISABLED_NOTIFICATION_ID.getName());
                    } else {
                        notificationId = parameters.get(PAYMENT_RESULT_DISABLED_NOTIFICATION_ID.getName());
                    }
                    break;
                }
                case CANCELLED: {
                    if (isRefund) {
                        notificationId = parameters.get(REFUND_RESULT_CANCELLED_NOTIFICATION_ID.getName());
                    } else {
                        notificationId = parameters.get(PAYMENT_RESULT_CANCELLED_NOTIFICATION_ID.getName());
                    }
                    break;
                }
            }

            if (StringUtils.isNotEmpty(notificationId)) {
                MessageDTO message = null;
                try {
                    message = new NotificationBL().getPaymentMessage(paySafeProcessedPaymentEvent.getEntityId(), paySafeProcessedPaymentEvent.getPaymentDTOEx(),
                            paySafeProcessedPaymentEvent.getPaymentDTOEx().getPaymentResult().getId(), Integer.valueOf(notificationId));
                    Integer userId = paySafeProcessedPaymentEvent.getPaymentDTOEx().getUserId();
                    if (paySafeProcessedPaymentEvent.getIsNewSession() && PaySafeResultType.SUCESSFUL.equals(paySafeProcessedPaymentEvent.getResult())) {
                        UserBL user = new UserBL(userId);
                        BigDecimal userBalance = user.getBalance(userId);
                        BigDecimal amount = paySafeProcessedPaymentEvent.getPaymentDTOEx().getAmount();
                        BigDecimal totalOwed = (isRefund) ? userBalance.add(amount) : userBalance.subtract(amount);
                        message.addParameter("total_owed", Util.decimal2string(totalOwed, user.getLocale(), Util.AMOUNT_FORMAT_PATTERN));
                    }

                } catch (NotificationNotFoundException e) {
                    logger.debug("Custom notification id: %s does not exist for the user id %s ",
                            notificationId,
                            paySafeProcessedPaymentEvent.getPaymentDTOEx().getUserId(), e);
                }
                if (message == null) {
                    return;
                }
                logger.debug("Notifying user: %s, with result %s and is a %s",
                        paySafeProcessedPaymentEvent.getPaymentDTOEx().getUserId(),
                        paySafeProcessedPaymentEvent.getResult(),
                        paySafeProcessedPaymentEvent.getPaymentDTOEx().getIsRefund() == 1 ? "Refund" : "Payment");
                INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
                notificationSession.notify(paySafeProcessedPaymentEvent.getPaymentDTOEx().getUserId(), message);

            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
