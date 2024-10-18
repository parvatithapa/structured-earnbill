package com.sapienter.jbilling.server.notification;

import java.lang.invoke.MethodHandles;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.SessionInternalError;

/**
 *  Notification async message listener
 *
 * @author Panche Isajeski
 * @since 08-Mar-2014
 */
@Transactional(propagation = Propagation.REQUIRED)
public class NotificationMDB implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private INotificationSessionBean notificationSessionBean;

    @Override
    public void onMessage(Message message) {
        try {
            logger.debug("Received a notification message: ");
            ObjectMessage objectMessage = (ObjectMessage) message;
            MessageDTO messageDTO = (MessageDTO) objectMessage.getObject();
            Integer userId = objectMessage.getIntProperty("userId");
            String fileName = messageDTO.getAttachmentFile();
            Integer invoiceId = messageDTO.getInvoiceId();
            if(null!= invoiceId && StringUtils.isNotEmpty(fileName)) {
                logger.debug("attached email file {} found for invoice {} for user{}", fileName, invoiceId, userId);
                if(!fileName.contains(invoiceId.toString())) {
                    logger.debug("Attachement mismatch found in NotificationMDB. Atached file {} is not for invoice {} for user {}", fileName, invoiceId, userId);
                    messageDTO.setAttachmentFile(null); // message contains wrong attachment.
                }
            }
            notificationSessionBean.asyncNotify(userId, messageDTO);
        } catch (Exception e) {
            logger.error("Error in  NotificationMDB!", e);
            throw new SessionInternalError(e);
        }

    }
}
