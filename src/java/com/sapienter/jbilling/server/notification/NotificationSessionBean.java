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

package com.sapienter.jbilling.server.notification;

import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDAS;
import com.sapienter.jbilling.paymentUrl.db.PaymentUrlLogDTO;
import com.sapienter.jbilling.paymentUrl.db.Status;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.spa.SpaImportBL;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.util.Context;

import org.apache.commons.lang.StringEscapeUtils;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.notification.db.NotificationMessageArchDAS;
import com.sapienter.jbilling.server.notification.db.NotificationMessageArchDTO;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.pluggableTask.NotificationTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;

import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.jms.*;

import java.lang.invoke.MethodHandles;
import java.util.List;

@Transactional( propagation = Propagation.REQUIRED )
public class NotificationSessionBean implements INotificationSessionBean {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    /**
     * Sends an email with the invoice to a customer.
     * This is used to manually send an email invoice from the GUI
     * @param invoiceId
     * @return
    */
    public Boolean emailInvoice(Integer invoiceId)
            throws SessionInternalError {
        Boolean retValue;
        try {
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            if(invoice.getEntity().isReviewInvoice()) {
                throw new SessionInternalError("Can not send Email as invoice is in review status",
                        new String[]{"InvoiceWS,review.status,invoice.prompt.failure.email.invoice.review.status,"
                                +invoice.getEntity().getId()});
            }
            UserBL user = new UserBL(invoice.getEntity().getBaseUser());
            Integer entityId = user.getEntity().getEntity().getId();
            Integer languageId = user.getEntity().getLanguageIdField();
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getInvoiceEmailMessage(entityId,
                    languageId, invoice.getEntity());
            retValue = notify(user.getEntity(), message);

        } catch (NotificationNotFoundException e) {
            retValue = new Boolean(false);
        }

        return retValue;
    }

    /**
     * Sends an email with the Payment information to the customer.
     * This is used to manually send an email Payment notification from the GUI (Show Payments)
     * @param paymentId
     * @return
    */
    public Boolean emailPayment(Integer paymentId)
            throws SessionInternalError {
        Boolean retValue;
        PaymentBL payment = new PaymentBL(paymentId);
        UserBL user = new UserBL(payment.getEntity().getBaseUser());
        Integer entityId = user.getEntity().getEntity().getId();
        try {
            NotificationBL notif = new NotificationBL();
            PaymentDTOEx paymentDTOEx = payment.getDTOEx(user.getEntity().getLanguageIdField());
            MessageDTO message = notif.getPaymentMessage(entityId,
                    paymentDTOEx,
                    payment.getEntity().getPaymentResult().getId());
            retValue = notify(user.getEntity(), message);
            paymentDTOEx.close();
        } catch (NotificationNotFoundException e) {
            retValue = new Boolean(false);
            if (SpaImportBL.isDistributel(entityId)) {
                SpaImportBL.emailPayment(entityId, paymentId);
                return true;
            }
        } catch (Exception e) {
        	logger.error("Error creating/sending Payment Notification: \n {}", e.getMessage());
            retValue = new Boolean(false);
        } 
        
        return retValue;
    }

    public void notify(Integer userId, MessageDTO message)
            throws SessionInternalError {
    	logger.debug("Entering notify()");

        try {
            UserBL user = new UserBL(userId);
            notify(user.getEntity(), message);
        } catch (Exception e) {
            throw new SessionInternalError("Problems getting user entity" +
                    " for id " + userId + "." + e.getMessage());
        }
    }

    public void asyncNotify(Integer userId, MessageDTO message)
            throws SessionInternalError {
        logger.debug("Entering notify()");

        try {
            UserBL user = new UserBL(userId);
            asyncNotify(user.getEntity(), message);
        } catch (Exception e) {
            throw new SessionInternalError("Problems getting user entity" +
                    " for id " + userId + "." + e.getMessage());
        }
    }

    /**
     * Sends a notification to a user. Returns true if no exceptions were
     * thrown, otherwise false. This return value could be considered
     * as if this message was sent or not for most notifications (emails).
     */
    public Boolean asyncNotify(UserDTO user, MessageDTO message)
            throws SessionInternalError {
        logger.debug("Entering notify()");
        Boolean retValue = new Boolean(true);
        try {
            // verify that the message is good
            if (message.validate() == false) {
                throw new SessionInternalError("Invalid message");
            }
           
            retValue = sendMessageWithNotification(user, message, retValue, message.getContent());

        } catch (Exception e) {
            logger.error("Exception in notify", e);
            throw new SessionInternalError(e);
        }

        return retValue;
    }

   /**
    * Sends a notification to a user asynchronously by posting the notification messages to a JMS queue.
    * It also post a notification messages for the other users that needs to be notified (admin, parents, partner)
    */
    public Boolean notify(UserDTO user, MessageDTO message)
            throws SessionInternalError {
        try {
            // verify that the message is good
            if (message.validate() == false) {
                throw new SessionInternalError("Invalid message");
            }
            // parse this message contents with the parameters
            String specificAddress = (String) message.getParameters().get(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS);
            message.setSpecificEmailAddress(specificAddress);
            MessageSection sections[] = message.getContent();
            for (int f=0; f < sections.length; f++) {
                MessageSection section = sections[f];
                section.setContent(NotificationBL.parseParameters(
                		StringEscapeUtils.unescapeJava(section.getContent()), message.getParameters()));
            }
            //avoid serialization of parameters
            message.getParameters().clear();
            logger.debug("Entering notify()");
            postNotificationMessage(user, message);

            if(isChecked(message.getNotifyAdmin())){
                for (UserDTO admin : new UserDAS().findAdminUsers(user.getEntity().getId())) {
                    postNotificationMessage(admin, message);
                }
            }
            if(isChecked(message.getNotifyPartner())){
                if (user.getPartner() != null) {
                    postNotificationMessage(user.getPartner().getUser(), message);
                }
            }
            CustomerDTO userParent = null;
            if(isChecked(message.getNotifyParent())){
                CustomerDTO customer = user.getCustomer();
                if (customer != null) {
                    userParent = customer.getParent();
                    if (userParent != null) {
                        postNotificationMessage(userParent.getBaseUser(), message);
                    }
                }
            }
            if(isChecked(message.getNotifyAllParents()) && userParent != null){
                CustomerDTO parentOfParent = userParent.getParent();
                while(parentOfParent!=null) {
                    postNotificationMessage(parentOfParent.getBaseUser(), message);
                    parentOfParent = parentOfParent.getParent();
                }
            }

        } catch (Exception e) {
            logger.error("Exception in notify", e);
            throw new SessionInternalError(e);
        }

        return true;
    }

    private void postNotificationMessage(final UserDTO user, final MessageDTO message) {

        logger.debug("Entering notify with message");

        JmsTemplate jmsTemplate = (JmsTemplate) Context.getBean(
                Context.Name.JMS_TEMPLATE);

        Destination destination = (Destination) Context.getBean(Context.Name.NOTIFICATIONS_LOCALHOST_DESTINATION);

        jmsTemplate.send(destination, (session) -> {
            ObjectMessage objectMessage = session.createObjectMessage();
            objectMessage.setObject(message);
            objectMessage.setIntProperty("userId", user.getId());
            return objectMessage;
        });

    }

    private Boolean sendMessageWithNotification(UserDTO user, MessageDTO message, Boolean retValue,
                                                MessageSection[] sections)
            throws com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException {
        // now do the delivery with the pluggable tasks
        PluggableTaskManager<NotificationTask> taskManager =
            new PluggableTaskManager<NotificationTask>( user.getEntity().getId(),
                Constants.PLUGGABLE_TASK_NOTIFICATION);

        NotificationTask task = taskManager.getNextClass();
        NotificationMessageArchDAS messageHome = new NotificationMessageArchDAS();
        boolean delivered = false;

        // deliver the message to the first task it can process by medium type(s)
        // continue to the next notification task only if the message was not delivered by the previous
        while (!delivered && executeTask(task, message.getMediumTypes())) {
            NotificationMessageArchDTO messageRecord = messageHome.create(message.getTypeId(), sections, user);
            try {
                delivered = deliverNotification("user", user, message, task);

            } catch (TaskException e) {
                messageRecord.setResultMessage(Util.truncateString(
                        e.getMessage(), 200));
                logger.error("Notification task error", e);
            }
            task = taskManager.getNextClass();
        }
        return delivered;
    }

    private boolean executeTask(NotificationTask task, List<NotificationMediumType> mediumTypes) {
        if (task == null) return false;
        else if (mediumTypes.size() == NotificationMediumType.values().length) return true;
        else {
            boolean oneMediumIsHandledFromTask = false;
            for (NotificationMediumType mediumType: mediumTypes) {
                oneMediumIsHandledFromTask =
                        oneMediumIsHandledFromTask ||
                        task.mediumHandled().contains(mediumType);
            }
            return oneMediumIsHandledFromTask;
        }
    }

    private boolean isChecked(Integer checkboxValue) {
        return Integer.valueOf(1).equals(checkboxValue);
    }

    private boolean deliverNotification(String userDescription, UserDTO user, MessageDTO message, NotificationTask task) throws TaskException {
        logger.debug("Sending notification to {} : {}",userDescription , user.getUserName());
        logger.debug("Sending notification to {}", user.getId());
        return task.deliver(user, message);
    }

    public MessageDTO getDTO(Integer typeId, Integer languageId,
            Integer entityId) throws SessionInternalError {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO retValue = null;
            int plugInSections = notif.getSections(entityId);
            notif.set(typeId, languageId, entityId);
            if (notif.getEntity() != null) {
                retValue = notif.getDTO();
            } else {
                retValue = new MessageDTO();
                retValue.setTypeId(typeId);
                retValue.setLanguageId(languageId);
                MessageSection sections[] =
                        new MessageSection[plugInSections];
                for (int f = 0; f < sections.length; f++) {
                    sections[f] = new MessageSection(new Integer(f + 1), "");
                }
                retValue.setContent(sections);
            }

            if (retValue.getContent().length < plugInSections) {
                // pad any missing sections, due to changes to a new plug-in with more sections
                for (int f = retValue.getContent().length ; f < plugInSections; f++) {
                    retValue.addSection(new MessageSection(new Integer(f + 1), ""));
                }
            } else if (retValue.getContent().length > plugInSections) {
                // remove excess sections
                retValue.setContentSize(plugInSections);
            }


            return retValue;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Integer createUpdate(MessageDTO dto, Integer entityId) {
        try {
            NotificationBL notif = new NotificationBL();
            return notif.createUpdate(entityId, dto);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public String getEmails(Integer entityId, String separator)
            throws SessionInternalError {
        try {
            NotificationBL notif = new NotificationBL();

            return notif.getEmails(separator, entityId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public Boolean sendEmailNotification(List<Integer> absaFailedPayments, List<Integer> standardBankFailedPayments, String clientCode,
                                         Integer entityId, String transmissionDate, Integer fileSequenceNo)
            throws SessionInternalError {
        logger.debug("Preparing to send email for Ignition payment failed ");
        Boolean retValue = true;
        try {
            NotificationBL notificationBL = new NotificationBL();

            for (UserDTO admin : new UserDAS().findAdminUsers(entityId)) {

                if(absaFailedPayments.size()>0) {
                    MessageDTO message = notificationBL.getAbsaPaymentsFailedNotificationMessage(absaFailedPayments, clientCode,
                            entityId, transmissionDate);

                    message.addParameter("first_name", admin.getContact().getFirstName());
                    message.addParameter("last_name", admin.getContact().getLastName());
                    String specificAddress = (String) message.getParameters().get(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS);
                    message.setSpecificEmailAddress(specificAddress);
                    MessageSection sections[] = message.getContent();

                    for (int f = 0; f < sections.length; f++) {
                        MessageSection section = sections[f];
                        section.setContent(NotificationBL.parseParameters(
                                StringEscapeUtils.unescapeJava(section.getContent()), message.getParameters()));
                    }

                    postNotificationMessage(admin, message);
                }

                if(standardBankFailedPayments.size() > 0){
                    MessageDTO message = notificationBL.getStandardBankPaymentsFailedNotificationMessage(standardBankFailedPayments, fileSequenceNo,
                            entityId);

                    message.addParameter("first_name", admin.getContact().getFirstName());
                    message.addParameter("last_name", admin.getContact().getLastName());
                    String specificAddress = (String) message.getParameters().get(MessageDTO.PARAMETER_SPECIFIC_EMAIL_ADDRESS);
                    message.setSpecificEmailAddress(specificAddress);
                    MessageSection sections[] = message.getContent();

                    for (int f = 0; f < sections.length; f++) {
                        MessageSection section = sections[f];
                        section.setContent(NotificationBL.parseParameters(
                                StringEscapeUtils.unescapeJava(section.getContent()), message.getParameters()));
                    }

                    postNotificationMessage(admin, message);
                }
            }

            logger.debug("Emails Sent");

        } catch (Exception e) {
            logger.error("Error creating/sending Payment Notification: \n {}", e.getMessage());
            retValue = new Boolean(false);
        }
        return retValue;
    }

    /**
     * Sends an email with a payment link to a customer.
     * This is used to manually send a payment link from the GUI
     *
     * @param invoiceId
     * @return
     */
    public Boolean sendPaymentLinkToCustomer(Integer invoiceId)
            throws SessionInternalError {
        Boolean retValue = false;
        try {
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            UserBL user = new UserBL(invoice.getEntity().getBaseUser());
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getPaymentLinkEmailMessage(user.getEntity().getEntity().getId(),
                    user.getEntity().getLanguageIdField(), invoice.getEntity());
            PaymentUrlLogDAS paymentUrlLogDAS = new PaymentUrlLogDAS();
            PaymentUrlLogDTO paymentUrlLogDTO = paymentUrlLogDAS.findByInvoiceId(invoiceId);
            if( paymentUrlLogDTO.getStatus().equals(Status.GENERATED) ) {
                retValue = notify(user.getEntity(), message);
                if( retValue ) {
                    paymentUrlLogDTO.setStatus(Status.SENT);
                    paymentUrlLogDAS.save(paymentUrlLogDTO);
                }
            }

        } catch (NotificationNotFoundException e) {
            retValue = new Boolean(false);
        }

        return retValue;
    }
}
