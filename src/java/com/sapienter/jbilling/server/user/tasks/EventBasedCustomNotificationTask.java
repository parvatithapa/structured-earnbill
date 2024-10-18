/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.user.tasks;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.csv.export.event.ReportExportNotificationEvent;
import com.sapienter.jbilling.server.customer.event.NewCustomerEvent;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.pluggableTask.AutoRenewalEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.event.CustomEmailTokenEvent;
import com.sapienter.jbilling.server.spc.SpcHelperService;
import com.sapienter.jbilling.server.spc.SpcNotificationTask;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionActionDTO;
import com.sapienter.jbilling.server.usagePool.event.UsagePoolConsumptionNotificationEvent;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.event.NewContactEvent;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 * Event custom notification task.
 *
 * @author: Panche.Isajeski
 * @since: 12/07/12
 */
public class EventBasedCustomNotificationTask extends PluggableTask implements IInternalEventsTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ParameterDescription PARAMETER_NEW_CUSTOMER_NOTIFICATION_ID =
            new ParameterDescription("new_customer_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("new_contact_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("new_order_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_AUTO_RENEWAL_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("auto_renewal_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription PARAMETER_AUTO_RENEWAL_CONFIRMATION_CUSTOM_NOTIFICATION_ID =
            new ParameterDescription("auto_renewal_confirmation_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription REPORT_EXPORT_SUCCESS_NOTIFICATION_ID =
            new ParameterDescription("report_export_success_notification_id", false, ParameterDescription.Type.INT);
    private static final ParameterDescription REPORT_EXPORT_FAILURE_NOTIFICATION_ID =
            new ParameterDescription("report_export_failed_notification_id", false, ParameterDescription.Type.INT);

    private static final String CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER = "Custom notification id: {} does not exist for the user id {} ";
    private static final String NOTIFY_USER_FOR_NEW_CONTACT = "Notifying user: {} for a new contact event";

    SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);

    //initializer for pluggable params
    // add as many event - notification parameters
    {
        descriptions.add(PARAMETER_NEW_CUSTOMER_NOTIFICATION_ID);
        descriptions.add(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_AUTO_RENEWAL_CUSTOM_NOTIFICATION_ID);
        descriptions.add(PARAMETER_AUTO_RENEWAL_CONFIRMATION_CUSTOM_NOTIFICATION_ID);
        descriptions.add(REPORT_EXPORT_SUCCESS_NOTIFICATION_ID);
        descriptions.add(REPORT_EXPORT_FAILURE_NOTIFICATION_ID);
    }

    @SuppressWarnings("unchecked")
    private static final Class<Event>[] events = new Class[]{
        NewCustomerEvent.class,
        NewContactEvent.class,
        UsagePoolConsumptionNotificationEvent.class,
        NewOrderEvent.class,
        AutoRenewalEvent.class,
        ReportExportNotificationEvent.class,
    };

    @Override
    public void process(Event event) throws PluggableTaskException {
        INotificationSessionBean notificationSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
        if (event instanceof NewCustomerEvent) {
            fireNewCustomerEventNotification((NewCustomerEvent) event, notificationSession);
        }
        if (event instanceof NewContactEvent) {
            fireNewContactEventNotification((NewContactEvent) event, notificationSession);
        }
    	else if (event instanceof UsagePoolConsumptionNotificationEvent) {
        	fireCustomerUsagePoolConsumptionNotification((UsagePoolConsumptionNotificationEvent) event, notificationSession);
        }
        else if (event instanceof NewOrderEvent) {
            fireNewOrderEventNotification((NewOrderEvent) event, notificationSession);
        }
        else if (event instanceof AutoRenewalEvent) {
            fireAutoRenewalEventNotification((AutoRenewalEvent) event);
        }
        else if (event instanceof ReportExportNotificationEvent) {
        	fireReportExportNotification((ReportExportNotificationEvent) event, notificationSession);
        }
    }

    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }

    public boolean fireNewCustomerEventNotification(NewCustomerEvent newCustomerEvent, INotificationSessionBean notificationSession) {
        if (parameters.get(PARAMETER_NEW_CUSTOMER_NOTIFICATION_ID.getName()) == null || newCustomerEvent.getUser() == null) {
            return false;
        }
        Integer notificationMessageTypeId = Integer.parseInt((String) parameters
                .get(PARAMETER_NEW_CUSTOMER_NOTIFICATION_ID.getName()));

        MessageDTO message = null;
        Integer userId = newCustomerEvent.getUser().getId();
        try {
            message = new NotificationBL().getCustomNotificationMessage(
                    notificationMessageTypeId,
                    newCustomerEvent.getEntityId(),
                    userId,
                    newCustomerEvent.getUser().getLanguageIdField());

        } catch (NotificationNotFoundException e) {
            logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
        }
        if (message == null) {
            return false;
        }

        for (String propertyKey : newCustomerEvent.getParameters().keySet()) {
            message.addParameter(propertyKey, newCustomerEvent.getParameters().get(propertyKey));
        }

        logger.debug("Notifying user: {} for a new customer event", userId);
        if(StringUtils.isNotBlank(parameters.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName()))) {
            spcHelperService.notify(notificationMessageTypeId, userId, null, parameters);
        } else{
            notificationSession.notify(userId, message);
        }
        return true;
    }


    public boolean fireNewOrderEventNotification(NewOrderEvent newOrderEvent, INotificationSessionBean notificationSession) {
        if (parameters.get(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID.getName()) == null || newOrderEvent.getOrder() == null) {
            return false;
        }

        Integer notificationMessageTypeId = Integer.parseInt((String) parameters
                .get(PARAMETER_NEW_ORDER_CUSTOM_NOTIFICATION_ID.getName()));

        MessageDTO message = null;
        Integer userId = newOrderEvent.getOrder().getUserId();

        try {
            UserBL userBL = new UserBL(userId);
            message = new NotificationBL().getCustomNotificationMessage(
                    notificationMessageTypeId,
                    newOrderEvent.getEntityId(),
                    userId,
                    userBL.getLanguage());
        } catch (NotificationNotFoundException e) {
            logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
        }

        if (message == null) {
            return false;
        }

        logger.debug(NOTIFY_USER_FOR_NEW_CONTACT, userId);
        if(StringUtils.isNotBlank(parameters.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName()))) {
            spcHelperService.notify(notificationMessageTypeId, userId, null, parameters);
        } else{
            notificationSession.notify(userId, message);
        }
        return true;

    }

    public boolean fireNewContactEventNotification(NewContactEvent newContactEvent,
                                                    INotificationSessionBean notificationSession) {
        if (parameters.get(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID.getName()) == null || newContactEvent.getContactDto()  == null) {
            return false;
        }
        Integer notificationMessageTypeId = Integer.parseInt((String) parameters
                .get(PARAMETER_NEW_CONTACT_CUSTOM_NOTIFICATION_ID.getName()));

        MessageDTO message = null;
        Integer userId = newContactEvent.getContactDto().getUserId();

        try {
            UserBL userBL = new UserBL(userId);
            message = new NotificationBL().getCustomNotificationMessage(
                    notificationMessageTypeId,
                    newContactEvent.getEntityId(),
                    userId,
                    userBL.getLanguage());

        } catch (NotificationNotFoundException e) {
            logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
        }

        if (message == null) {
            return false;
        }

        logger.debug(NOTIFY_USER_FOR_NEW_CONTACT, userId);
        if(StringUtils.isNotBlank(parameters.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName()))) {
            spcHelperService.notify(notificationMessageTypeId, userId, null, parameters);
        } else{
            notificationSession.notify(userId, message);
        }
        return true;
    }
    
    public boolean fireCustomerUsagePoolConsumptionNotification(UsagePoolConsumptionNotificationEvent usagePoolConsumptionNotificationEvent,
            									INotificationSessionBean notificationSession) {
        UsagePoolConsumptionActionDTO action = usagePoolConsumptionNotificationEvent.getAction();
        Integer notificationMessageTypeId = action.getNotificationId();

		Integer customerUsagePoolId = usagePoolConsumptionNotificationEvent.getCustomerUsagePoolId();
		CustomerUsagePoolDTO customerUsagePool = new CustomerUsagePoolDAS().find(customerUsagePoolId);
		Integer userId = customerUsagePool.getCustomer().getBaseUser().getUserId();
		
		if (notificationMessageTypeId == null && userId == null) {
			return false;
		}
		
		MessageDTO message = null;
		UserDTO user = null;
		String salutation = "";
		String usagePoolName = "";
		try {
			UserBL userBL = new UserBL(userId);
			user = userBL.getEntity();
			ContactDTO contact = userBL.getEntity().getContact();
	        if (null != contact && null != contact.getFirstName() && null != contact.getLastName()) 
	            salutation = contact.getFirstName() + " " + contact.getLastName();
	        else 
	            salutation = userBL.getEntity().getUserName();
	        
	        usagePoolName = customerUsagePool.getUsagePool().getDescription(user.getLanguage().getId(), "name");
	        
			message = new NotificationBL().getCustomNotificationMessage(
			notificationMessageTypeId,
			usagePoolConsumptionNotificationEvent.getEntityId(),
			userId,
			userBL.getLanguage());
			
		} catch (NotificationNotFoundException e) {
			logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
		}
		if (message == null) {
			return false;
		}

		message.addParameter("userSalutation", salutation);
		message.addParameter("usagePoolName", usagePoolName);
		message.addParameter("percentageConsumption", action.getPercentage());
		
		CustomEmailTokenEvent event = new CustomEmailTokenEvent(user.getEntity().getId(), userId, message);
		EventManager.process(event);
        
		message.addParameter("usageQuantity", (customerUsagePool.getInitialQuantity().subtract(customerUsagePool.getQuantity())
				.setScale(Constants.BIGDECIMAL_SCALE_STR,Constants.BIGDECIMAL_ROUND)));
		message.addParameter("usagePoolQuantity", customerUsagePool.getInitialQuantity().setScale(Constants.BIGDECIMAL_SCALE_STR,Constants.BIGDECIMAL_ROUND));
		
		logger.debug("Notifying user: {} for a consumption notification event", userId);
        notificationSession.notify(userId, message);
		return true;
	}

    public boolean fireAutoRenewalEventNotification(AutoRenewalEvent autoRenewalEvent) {
        Map<String, Object> messageParameters = new HashMap<>();
        String notificationIdParameterName;

        if (autoRenewalEvent.isRenewalReached()) {
            notificationIdParameterName = PARAMETER_AUTO_RENEWAL_CONFIRMATION_CUSTOM_NOTIFICATION_ID.getName();
        }
        else {
            notificationIdParameterName = PARAMETER_AUTO_RENEWAL_CUSTOM_NOTIFICATION_ID.getName();
            messageParameters.put("daysBeforeNotification", autoRenewalEvent.getDaysBeforeNotification());
        }

        return this.sendNotification(Integer.valueOf(parameters.get(notificationIdParameterName)), autoRenewalEvent.getEntityId(),
                autoRenewalEvent.getCustomer().getBaseUser().getId(), autoRenewalEvent.getName(), messageParameters);
    }
    
    public boolean fireReportExportNotification(ReportExportNotificationEvent csvEvent,
			INotificationSessionBean notificationSession) {

		Integer notificationMessageTypeId = null;

		if (parameters.get(REPORT_EXPORT_SUCCESS_NOTIFICATION_ID.getName()) != null 
				&& ReportExportNotificationEvent.NotificationStatus.PASSED.equals(csvEvent.getStatus()) ) {
			notificationMessageTypeId = Integer.valueOf((String) parameters
					.get(REPORT_EXPORT_SUCCESS_NOTIFICATION_ID.getName()));
		}else if (parameters.get(REPORT_EXPORT_FAILURE_NOTIFICATION_ID.getName()) != null 
				&& ReportExportNotificationEvent.NotificationStatus.FAILED.equals(csvEvent.getStatus())) {
			notificationMessageTypeId = Integer.valueOf((String) parameters
					.get(REPORT_EXPORT_FAILURE_NOTIFICATION_ID.getName()));
		}else {
			return false;
		}

         MessageDTO message = null;
         Integer userId = csvEvent.getUserId();
         try {
             UserBL userBL = new UserBL(userId);
             message = new NotificationBL().getCustomNotificationMessage(
                     notificationMessageTypeId,
                     csvEvent.getEntityId(),
                     userId,
                     userBL.getLanguage());
             String filePath = Util.getSysProp("generate.csv.file.path");
             message.addParameter("filePath", filePath);
             message.addParameter("fileName", csvEvent.getFileName());
             message.addParameter("adminUser", userBL.getDto().getUserName());

         } catch (NotificationNotFoundException e) {
             logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
         }

         if (message == null) {
             return false;
         }
         logger.debug(NOTIFY_USER_FOR_NEW_CONTACT, userId);
         if(StringUtils.isNotBlank(parameters.get(SpcNotificationTask.PARAMETER_EXTERNAL_API_URL.getName()))) {
             spcHelperService.notify(notificationMessageTypeId, userId, null, parameters);
         } else{
             notificationSession.notify(userId, message);
         }
         return true;
	}

    public boolean sendNotification(Integer notificationMessageTypeId, Integer entityId, Integer userId, String eventName, Map messageParameters) {
        try {
            UserDTO user = new UserDAS().findNow(userId);
            MessageDTO message = new NotificationBL().getCustomNotificationMessage(notificationMessageTypeId, entityId, userId, user.getLanguageIdField());

            if (messageParameters != null) {
                message.getParameters().putAll(messageParameters);
            }

            logger.debug("Notifying user: {} for {} event", userId, eventName);
            ((INotificationSessionBean) Context.getBean(Context.Name.NOTIFICATION_SESSION)).notify(userId, message);
            return true;
        }
        catch (NotificationNotFoundException e) {
            logger.debug(CUSTOM_NOTIFICATION_DOES_NOT_EXIST_FOR_USER, notificationMessageTypeId, userId);
            return false;
        }
        catch (SessionInternalError e) {
            logger.debug("Error sending custom notification id: {} for the user id {} ", notificationMessageTypeId, userId);
            return false;
        }
    }
}