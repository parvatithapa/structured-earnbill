package com.sapienter.jbilling.server.invoice;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Date;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.sapienter.jbilling.server.process.event.NewUserStatusEvent;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.user.db.CancellationRequestDAS;
import com.sapienter.jbilling.server.user.db.CancellationRequestDTO;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDAS;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 *
 * @author krunal bhavsar
 *
 */
public class GenerateCancellationInvoiceMDB implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void onMessage(Message message) {
        Integer cancellationId = null;
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            IWebServicesSessionBean service = Context.getBean(Name.WEB_SERVICES_SESSION);
            Integer userId = (Integer) objectMessage.getObject();

            if (null != objectMessage.getObjectProperty(Constants.CANCELLATION_REQUEST_ID)) {
                Date cancellationDate = new Date((Long) objectMessage.getObjectProperty(Constants.CANCELLATION_REQUEST_DATE));
                cancellationId = (Integer) objectMessage.getObjectProperty(Constants.CANCELLATION_REQUEST_ID);

                InvoiceWS latestInvoice = service.getLatestInvoice(userId);
                updateCancellationStatus(cancellationId, CancellationRequestStatus.PROCESSED);
                if(null!=latestInvoice && cancellationDate.compareTo(latestInvoice.getCreateDatetime())<=0) {
                    return ;
                }
                Integer[] invoices = service.createInvoice(userId, false);
                logger.debug("Generated Invoice ids {} for user {}", Arrays.toString(invoices), userId);
                updateUserStatus(userId);
                if(null!=invoices && invoices.length!=0) {
                    Arrays.stream(invoices).forEach(service::notifyInvoiceByEmail);
                }
            } else {
                generateInvoiceForFreeTrialOrders(userId);
            }

        } catch (Exception ex) {
            if (cancellationId != null) {
                updateCancellationStatus(cancellationId, CancellationRequestStatus.APPLIED);
            }
            logger.error("Exception occurred during generate invoice {}", ex);
        }
    }

    /**
     * Helper method to generate Invoice for Free Trial orders after active until is expired
     * @param userId
     */
    private void generateInvoiceForFreeTrialOrders(Integer userId) {
        IWebServicesSessionBean service = Context.getBean(Name.WEB_SERVICES_SESSION);
        Integer[] invoices = service.createInvoice(userId, true);
        logger.debug("Generated Free Consumption Invoice ids {} for user {}", Arrays.toString(invoices), userId);
        if (null != invoices && invoices.length != 0) {
            Arrays.stream(invoices).forEach(service::notifyInvoiceByEmail);
        }
    }

    private TransactionTemplate getTransactionTemplate() {
        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        return new TransactionTemplate(transactionManager);
    }

    private void updateUserStatus(Integer userId) {
        getTransactionTemplate().execute(status -> {
            UserDAS userDAS = new UserDAS();
            UserDTO user = userDAS.findNow(userId);
            UserStatusDTO oldStatus = user.getStatus();
            UserStatusDTO newStatus = new UserStatusDAS().findByDescription(Constants.CUSTOMER_CANCELLATION_STATUS_DESCRIPTION, user.getLanguageIdField());
            user.setUserStatus(newStatus);
            NewUserStatusEvent event = new NewUserStatusEvent(user, user.getCompany().getId(), oldStatus.getId(), newStatus.getId());
            EventManager.process(event);
            return true;
        });
    }

    private void updateCancellationStatus(Integer cancellationRequestId, CancellationRequestStatus cancellationRequestStatus) {
        getTransactionTemplate().execute(status -> {
            CancellationRequestDAS cancellationRequestDAS = new CancellationRequestDAS();
            CancellationRequestDTO cancellationRequestDTO = cancellationRequestDAS.find(cancellationRequestId);
            cancellationRequestDTO.setStatus(cancellationRequestStatus);
            cancellationRequestDAS.save(cancellationRequestDTO);
            return true;
        });
    }

}
