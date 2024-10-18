package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDTO;
import com.sapienter.jbilling.server.order.db.OrderStatusDAS;
import com.sapienter.jbilling.server.order.event.IgnitionOrderStatusEvent;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * Created by Wajeeha Ahmed on 9/7/17.
 */
public class IgnitionCustomerAccountSuspensionTask extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(IgnitionCustomerAccountSuspensionTask.class);

    // Subscribed Events
    private static final Class<Event> events[] = new Class[] {
            IgnitionPaymentFailedEvent.class
    };


    @Override
    public void process(Event event) throws PluggableTaskException {

        if(event instanceof IgnitionPaymentFailedEvent){
            IWebServicesSessionBean webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
            PaymentDTOEx paymentDTOEx = ((IgnitionPaymentFailedEvent) event).getPayment();

            Integer userId = paymentDTOEx.getUserId();
            PaymentAuthorizationDTO paymentAuthorizationDTO = paymentDTOEx.getAuthorization();
            String errorCode = paymentAuthorizationDTO.getCode1();

            String prefix = ((IgnitionPaymentFailedEvent) event).getFileType() + "-";

            if(parameters.containsKey(prefix+errorCode)) {

                UserStatusDAS userStatusDAS = new UserStatusDAS();
                UserStatusDTO status = null;
                List<UserStatusDTO> statusDTOs = userStatusDAS.findByEntityId(getEntityId());

                CompanyDAS companyDas = new CompanyDAS();
                CompanyDTO company = companyDas.findEntityByName(companyDas.findCompanyNameByEntityId(getEntityId()));


                for (UserStatusDTO statusDto : statusDTOs) {
                    if (statusDto.getDescription(company.getLanguageId()).equals(IgnitionConstants.CUSTOMER_ACCOUNT_STATUS_SUSPEND)) {
                        status = statusDto;
                    }
                }

                if(status != null) {

                    UserWS user = webServicesSessionBean.getUserWS(userId);

                    int oldStatusId = user.getStatusId();

                    user.setStatusId(status.getId());
                    webServicesSessionBean.updateUser(user);

                    // Raise Event to Notify Listeners about status change
                    NewUserStatusEvent userStatusEvent = new NewUserStatusEvent(UserBL.getUserEntity(userId), this.getEntityId(), oldStatusId, status.getId());
                    EventManager.process(userStatusEvent);

                    List<OrderDTO> orderDTOList = new OrderDAS().findAllUserByUserId(userId);

                    if (!CollectionUtils.isEmpty(orderDTOList)) {
                        OrderStatusDTO orderStatus = null;
                        List<OrderStatusDTO> orderStatusDTOList = new OrderStatusDAS().findAll(this.getEntityId());

                        for (OrderStatusDTO orderStatusDTO : orderStatusDTOList) {
                            if (orderStatusDTO.getDescription(webServicesSessionBean.getCallerLanguageId()).equals(IgnitionConstants.ORDER_STATUS_LAPSED)) {
                                orderStatus = orderStatusDTO;
                                break;
                            } else if (orderStatusDTO.getOrderStatusFlag().equals(OrderStatusFlag.SUSPENDED_AGEING)) {
                                orderStatus = orderStatusDTO;
                            }
                        }

                        if(orderStatus != null) {
                            for (OrderDTO order : orderDTOList) {
                                order.setStatusId(orderStatus.getId());
                                new OrderDAS().save(order);

                                // Raise Ignition Custom Event to notify for order status change.
                                IgnitionOrderStatusEvent ignitionOrderStatusEvent = new IgnitionOrderStatusEvent(this.getEntityId(), userId,
                                        orderStatus.getId(), order.getId());
                                EventManager.process(ignitionOrderStatusEvent);
                            }
                        }else{
                            logger.debug("Suspend or Lapsed status not available for order");
                        }
                    } else {
                        logger.debug("No Order found for the given User: %s", userId);
                    }
                }
                else {
                    logger.debug("Suspend status not available for customer");
                }
            }
            else {
                logger.debug("Error code not found: " +errorCode);
            }
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
