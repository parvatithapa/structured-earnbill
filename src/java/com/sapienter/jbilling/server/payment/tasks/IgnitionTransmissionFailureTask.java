package com.sapienter.jbilling.server.payment.tasks;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentResponseEvent;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentSuccessfulEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Wajeeha Ahmed on 9/19/17.
 */
public class IgnitionTransmissionFailureTask  extends PluggableTask implements IInternalEventsTask {

    private static final FormatLogger logger = new FormatLogger(IgnitionTransmissionFailureTask.class);
    private IWebServicesSessionBean webServicesSessionBean;
    private List<Integer> absaFailedPaymentIds = null;
    private List<Integer> standardBankFailedPaymentIds = null;
    private String transmissionDate = null;

    // Subscribed Events
    private static final Class<Event> events[] = new Class[] {
            IgnitionPaymentResponseEvent.class
    };

    @Override
    public void process(Event event) throws PluggableTaskException {

        try {
            if(event instanceof IgnitionPaymentResponseEvent) {

                absaFailedPaymentIds = new ArrayList<>();
                standardBankFailedPaymentIds = new ArrayList<>();

                logger.debug("Processing Transmission Failure");
                IgnitionPaymentResponseEvent paymentFailureEvent = (IgnitionPaymentResponseEvent) event;
                this.webServicesSessionBean = Context.getBean(Context.Name.WEB_SERVICES_SESSION);

                if(paymentFailureEvent.getServiceProvider().equals((IgnitionConstants.SERVICE_PROVIDER_ABSA))){
                    updateABSAPayments(paymentFailureEvent.getPaymentIds(), paymentFailureEvent.getPaymentFailureType());
                    transmissionDate = paymentFailureEvent.getTransmissionDate();
                }else {

                    Map<String, String> metaFields = new HashMap<>();
                    Integer fileSequenceNo = paymentFailureEvent.getFileSequenceNo();
                    String clientCode = paymentFailureEvent.getAcbCode().trim();

                    metaFields.put(IgnitionConstants.PAYMENT_CLIENT_CODE, clientCode);
                    metaFields.put(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER, fileSequenceNo.toString());

                    PaymentDAS paymentDAS = new PaymentDAS();
                    List<Integer> paymentIds = paymentDAS.findAllPaymentsByMetaFields(metaFields);

                    if (paymentIds.size() > 0) {

                        if (paymentFailureEvent.getPaymentFailureType().equals(IgnitionConstants.PaymentStatus.SB_TRANSMISSION_FAILURE)) {
                            logger.debug("Processing Transmission Failure");
                            //If header gets error
                            for (Integer paymentId : paymentIds) {
                                PaymentWS payment = webServicesSessionBean.getPayment(paymentId);
                                String[] errorDetails = paymentFailureEvent.getTransactionDetails().get(0).split(";",2);
                                UnlinkInvoiceAndDeletePayment(payment,errorDetails[0], errorDetails[1], IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);
                                standardBankFailedPaymentIds.add(payment.getId());
                            }

                        } else if (paymentFailureEvent.getPaymentFailureType().equals(IgnitionConstants.PaymentStatus.SB_TRANSACTION_FAILURE)) {
                            logger.debug("Processing Transaction Failure");

                            Map<Integer, String> transactionDetails = paymentFailureEvent.getTransactionDetails();

                            for (Integer paymentId : paymentIds) {
                                PaymentWS payment = webServicesSessionBean.getPayment(paymentId);
                                MetaFieldValueWS[] paymentMetafields = payment.getMetaFields();
                                Integer transactionNo = null;

                                for (MetaFieldValueWS metafield : paymentMetafields) {
                                    if (metafield.getFieldName().equals(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER)) {
                                        transactionNo = Integer.parseInt((String) metafield.getValue());
                                        logger.debug("Transaction No: " + transactionNo);
                                        break;
                                    }
                                }

                                if (transactionDetails.keySet().contains(transactionNo)) {
                                    // Payment failed
                                    logger.debug("processing payment failure for payment " + payment.getId());

                                    String[] errorDetails = transactionDetails.get(transactionNo).split(";",2);
                                    String errorCode = errorDetails[0];
                                    String prefix = paymentFailureEvent.getFileType() + "-";

                                    if (parameters.keySet().contains(prefix + errorCode)) {
                                        logger.debug("Transmission failed for payment " + payment.getId());
                                        UnlinkInvoiceAndDeletePayment(payment, errorCode, errorDetails[1],  IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);
                                        standardBankFailedPaymentIds.add(payment.getId());
                                    } else if(errorCode.equals(IgnitionConstants.SB_OUTPUT_FILE_ERROR_TRANSACTION_SUCCESSFUL)){
                                        if(payment.getAuthorizationId() == null) {
                                            updatePaymentStatus(payment);
                                        }else {
                                            logger.debug("Payment Authorization already exists for payment id : " +payment.getId());
                                        }
                                    } else {
                                        logger.debug("Transaction failed for payment " + payment.getId());

                                        if (errorDetails.length >1 && !errorDetails[1].isEmpty()){
                                            IgnitionUtility.updatePaymentWithErrorCodeAndMetaField(paymentId, errorCode, errorDetails[1], webServicesSessionBean, paymentFailureEvent.getEntityId(), IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);
                                        }
                                        else {
                                            IgnitionUtility.updatePaymentWithErrorCodeAndMetaField(paymentId, errorCode, "Interim/Final Audit no error description received", webServicesSessionBean, paymentFailureEvent.getEntityId(),  IgnitionConstants.SERVICE_PROVIDER_STANDARD_BANK);
                                        }

                                        PaymentBL paymentBL = new PaymentBL(paymentId);
                                        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(webServicesSessionBean.getCallerLanguageId());

                                        IgnitionPaymentFailedEvent ignitionPaymentFailed = new IgnitionPaymentFailedEvent(getEntityId(), paymentDTOEx, paymentFailureEvent.getFileType());
                                        EventManager.process(ignitionPaymentFailed);
                                    }
                                } else {
                                    if(payment.getAuthorizationId() == null) {
                                        updatePaymentStatus(payment);
                                    }else {
                                        logger.debug("Payment Authorization already exists for payment id : " +payment.getId());
                                    }
                                }
                            }
                        }
                    } else {
                        logger.debug("Payments not found for sequence no " + fileSequenceNo + " and client code " + clientCode);
                    }
                }

                if(absaFailedPaymentIds.size() > 0 || standardBankFailedPaymentIds.size() > 0)
                {
                   sendTransmissionFailureNotification(paymentFailureEvent.getAcbCode(), transmissionDate, paymentFailureEvent.getFileSequenceNo());
                }
            }
        }catch (Exception exception){
            logger.debug("Exception: "+exception);
        }
    }

    private void UnlinkInvoiceAndDeletePayment(PaymentWS payment, String errorCode, String errorDescription, String bankName) {
        logger.debug("Unlink Invoice and remove payment for id " +payment.getId());

        if (!StringUtils.isEmpty(errorCode)){

            IgnitionUtility.updatePaymentWithErrorCodeAndMetaField(payment.getId(), errorCode, errorDescription, webServicesSessionBean, getEntityId(), bankName);
        }

        PaymentBL paymentBL = new PaymentBL(payment.getId());

        if (payment.getInvoiceIds() != null) {
            if (payment.getInvoiceIds().length > 0) {
                for (Integer invoiceId : payment.getInvoiceIds()) {
                    logger.debug("Removing invoice " + invoiceId + " from payment " + payment.getId());
                    paymentBL.unLinkFromInvoice(invoiceId);
                }
            }
        }
        paymentBL.delete();
    }

    private void updatePaymentStatus(PaymentWS payment) {
        logger.debug("Updating payment " + payment.getId() + " status");

        payment.setResultId(CommonConstants.PAYMENT_RESULT_SUCCESSFUL);
        webServicesSessionBean.updatePayment(payment);

        PaymentBL paymentBL = new PaymentBL(payment.getId());
        CompanyWS companyWS = webServicesSessionBean.getCompanyByEntityId(getEntityId());
        PaymentDTOEx paymentDTOEx = paymentBL.getDTOEx(companyWS.getLanguageId());

        // Raise Event to notify Webhooks for success
        IgnitionPaymentSuccessfulEvent event = new IgnitionPaymentSuccessfulEvent(getEntityId(), paymentDTOEx);
        EventManager.process(event);
    }

    private Boolean sendTransmissionFailureNotification(String clientCode, String transmissionDate, Integer fileSequenceNo){
        logger.debug("Sending email notification for payment failure");

        try {
            INotificationSessionBean notificationSessionBean = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSessionBean.sendEmailNotification(absaFailedPaymentIds, standardBankFailedPaymentIds, clientCode,getEntityId(),transmissionDate,fileSequenceNo );
        }catch (Exception exception){
            logger.debug("Exception: " +exception);
            return false;
        }
        return true;
    }

    private void updateABSAPayments(List<Integer> paymentIds, IgnitionConstants.PaymentStatus status){
        for(Integer paymentId : paymentIds) {

            logger.debug("Requesting status update for payment: %s", paymentId);
            PaymentWS payment = webServicesSessionBean.getPayment(paymentId);

            if(status.equals(IgnitionConstants.PaymentStatus.ABSA_REJECTED)){

                UnlinkInvoiceAndDeletePayment(payment, "REJECTED", null, IgnitionConstants.SERVICE_PROVIDER_ABSA);
                absaFailedPaymentIds.add(payment.getId());

            }else{
                if(payment.getAuthorizationId() == null) {
                    updatePaymentStatus(payment);
                }else {
                    logger.debug("Payment Authorization already exists for payment id : " +payment.getId());
                }
            }
        }
    }
    @Override
    public Class<Event>[] getSubscribedEvents() {
        return events;
    }
}
