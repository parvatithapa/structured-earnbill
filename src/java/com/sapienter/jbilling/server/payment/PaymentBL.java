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

package com.sapienter.jbilling.server.payment;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EntityNotFoundException;
import javax.sql.rowset.CachedRowSet;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.customer.CustomerSignupResponseWS;
import com.sapienter.jbilling.server.customerInspector.domain.ListField;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldExternalHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.notification.NotificationNotFoundException;
import com.sapienter.jbilling.server.payment.db.PaymentAuthorizationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentDAS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInstrumentInfoDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInstrumentInfoDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodTypeDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDTO;
import com.sapienter.jbilling.server.payment.event.AbstractPaymentEvent;
import com.sapienter.jbilling.server.payment.event.PaymentDeletedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentUnlinkedFromInvoiceEvent;
import com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentInfoTask;
import com.sapienter.jbilling.server.pluggableTask.PaymentTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskManager;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandBL;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningStatusDAS;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.partner.db.PartnerPayout;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.audit.LogMessage;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;

public class PaymentBL extends ResultList implements PaymentSQL {

    private static final Logger logger = LoggerFactory.getLogger(PaymentBL.class);

    private PaymentDAS paymentDas = null;
    private PaymentMethodDAS methodDas = null;
    private PaymentInvoiceMapDAS mapDas = null;
    private PaymentDTO payment = null;
    private EventLogger eLogger = null;

    private PaymentInformationDAS piDas = null;
    private PaymentInstrumentInfoDAS piiDas = null;

    public PaymentBL(Integer paymentId) {
        init();
        set(paymentId);
    }

    /**
     * Validates that a refund payment must be linked to a payment and the amount of the refund payment should
     * be equal to the linked payment. Return true if valid.
     *
     * @param refundPayment
     * @return boolean
     */
    public static synchronized Boolean validateRefund(PaymentWS refundPayment) {
        if (refundPayment.getPaymentId() == null) {
            String msg = "There is no linked payment with this refund";
            String logMsg = getEnhancedActionMessage(msg);
            logger.info(logMsg);
            return false;
        }
        // fetch the linked payment from database
        PaymentDTO linkedPayment = new PaymentBL(refundPayment.getPaymentId()).getEntity();
        
        if (linkedPayment == null) {
            String msg = "There is no linked payment with this refund";
            String logMsg = getEnhancedActionMessage(msg);
            logger.info(logMsg);
            return false;
        }
        BigDecimal refundableBalance = linkedPayment.getBalance();
        String msg = String.format("Selected payment with ID: %d can be refunded for %s", linkedPayment.getId(), refundableBalance);
        String logMsg = getEnhancedActionMessage(msg);
        logger.debug(logMsg);

        PaymentDTO originalRefundPayment = new PaymentBL(refundPayment.getId()).getEntity();
        if (null != originalRefundPayment && refundPayment.getAmountAsDecimal().compareTo(refundableBalance.add(originalRefundPayment.getAmount())) <= 0) {
            return true;
        } else if (refundPayment.getAmountAsDecimal().compareTo(refundableBalance) > 0) {
            msg = "Cannot refund more than the refundableBalance";
            logMsg = getEnhancedActionMessage(msg);
            logger.info(logMsg);
            return false;
        } else if (BigDecimal.ZERO.compareTo(refundableBalance) >= 0) {
            msg = "Cannot refund a zero or negative refundable balance";
            logMsg = getEnhancedActionMessage(msg);
            logger.info(logMsg);
            return false;
        } else if (BigDecimal.ZERO.compareTo(refundPayment.getAmountAsDecimal()) >= 0) {
            msg = "Cannot refund a zero or negative refund amount";
            logMsg = getEnhancedActionMessage(msg);
            logger.info(logMsg);
            return false;
        }

        return true;
    }

    private static String getEnhancedActionMessage(String msg) {
        return new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                .action(LogConstants.ACTION_EVENT.toString()).message(msg).build().toString();
    }

    public PaymentBL() {
        init();
    }

    public PaymentBL(PaymentDTO payment) {
        init();
        this.payment = payment;
    }

    public void set(PaymentDTO payment) {
        this.payment = payment;
    }

    private void init() {
        try {
            eLogger = EventLogger.getInstance();

            paymentDas = new PaymentDAS();

            methodDas = new PaymentMethodDAS();

            mapDas = new PaymentInvoiceMapDAS();

            piDas = new PaymentInformationDAS();

            piiDas = new PaymentInstrumentInfoDAS();

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public PaymentDTO getEntity() {
        return payment;
    }

    public PaymentDAS getHome() {
        return paymentDas;
    }

    public String getMethodDescription(PaymentMethodDTO method, Integer languageId) {
        // load directly from the DB, otherwise proxies get in the way
        logger.debug("Loading description for method: {}", method);
        return new PaymentMethodDAS().find(method.getId()).getDescription(languageId);
    }

    public void set(Integer id) {
        payment = paymentDas.findNow(id);
    }

    public void create(PaymentDTOEx dto, Integer executorUserId) {
        // create the record
        // The method column here is null now because method of payment will be decided after the payment processing

        payment = paymentDas.create(dto.getAmount(), null,
                dto.getUserId(), dto.getAttempt(), dto.getPaymentResult(), dto.getCurrency());
        UserDTO userDTO = new UserBL(dto.getUserId()).getEntity();
        PaymentInformationDTO savedInstrument = null;
        for (PaymentInformationDTO paymentInformationDTO : userDTO.getPaymentInstruments()) {
            if (null != paymentInformationDTO && null != dto.getInstrument() &&
                    paymentInformationDTO.getId().equals(dto.getInstrument().getId())) {
                savedInstrument = paymentInformationDTO;
            }
        }

        dto = null != savedInstrument ? getDtoWithValidBankAccountNumber(dto, savedInstrument) : dto;
        if (dto.getInstrument() != null && dto.getPaymentInstruments().size() > 0) {
            payment.setPaymentMethod(dto.getInstrument().getPaymentMethod());
            payment.getPaymentInstrumentsInfo().add(new PaymentInstrumentInfoDTO(payment,
                    dto.getPaymentResult(), dto.getInstrument().getPaymentMethod(), dto.getInstrument().getSaveableDTO()));
        }

        if (dto.getPaymentMethod() != null) {
            payment.setPaymentMethod(dto.getPaymentMethod());
        }

        payment.setPaymentDate(dto.getPaymentDate());
        payment.setBalance(dto.getBalance());

        // may be this is a refund
        if (dto.getIsRefund() == 1) {
            payment.setIsRefund(1);

            // refund balance is always set to ZERO
            payment.setBalance(BigDecimal.ZERO);
            String msg = String.format("dto of paymentDTOEX contains %s", dto);
            String logMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                    .action(LogConstants.ACTION_EVENT.toString()).message(msg).build().toString();
            logger.debug(logMsg);
            if (dto.getPayment() != null) {
                msg = String.format("Refund is linked to some payment %s", dto.getPayment());
                logMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                        .action(LogConstants.ACTION_EVENT.toString()).message(msg).build().toString();
                logger.debug(logMsg);
                // this refund is link to a payment
                PaymentBL linkedPayment = new PaymentBL(dto.getPayment().getId());
                payment.setPayment(linkedPayment.getEntity());
            }
        }

        // preauth payments
        if (dto.getIsPreauth() != null && dto.getIsPreauth() == 1) {
            payment.setIsPreauth(1);
        }

        // the payment period length this payment was expected to last
        if (dto.getPaymentPeriod() != null) {
            payment.setPaymentPeriod(dto.getPaymentPeriod());

        }
        // the notes related to this payment
        if (dto.getPaymentNotes() != null) {
            payment.setPaymentNotes(dto.getPaymentNotes());
        }

        // meta fields
        UserBL userBL = new UserBL(dto.getUserId());
        payment.updateMetaFieldsWithValidation(userBL.getLanguage(), new UserBL().getEntityId(dto.getUserId()), null, dto);

        dto.setId(payment.getId());
        dto.setCurrency(payment.getCurrency());

        //Update real amount from PaymentDTO into PaymentDTOEx
        dto.setAmount(payment.getAmount());
        dto.setBalance(payment.getAmount());

        dto.setPayment(payment.getPayment());
        paymentDas.save(payment);
        paymentDas.flush();
        // add a log row for convenience
        UserDAS user = new UserDAS();

        if (null != executorUserId) {
            eLogger.audit(executorUserId, dto.getUserId(), Constants.TABLE_PAYMENT, dto.getId(),
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        } else {
            eLogger.auditBySystem(user.find(dto.getUserId()).getCompany().getId(),
                    dto.getUserId(), Constants.TABLE_PAYMENT, dto.getId(),
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_CREATED, null, null, null);
        }

    }

    private PaymentDTOEx getDtoWithValidBankAccountNumber(PaymentDTOEx dto, PaymentInformationDTO savedInstrument) {
        List<MetaFieldValue> savedMetaFields = savedInstrument.getMetaFields();
        MetaFieldValue savedMetaField = null;
        for (MetaFieldValue metaFieldValue : savedMetaFields) {
            if (MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED.equals(metaFieldValue.getField().getFieldUsage())) {
                savedMetaField = metaFieldValue;
            }
        }
        for (int i = 0; i < dto.getInstrument().getMetaFields().size(); i++) {
            MetaFieldValue metaFieldValue = dto.getInstrument().getMetaFields().get(i);
            if (null != metaFieldValue && null != savedMetaField && MetaFieldType.BANK_ACCOUNT_NUMBER_ENCRYPTED.equals(metaFieldValue.getField().getFieldUsage()) &&
                    DataType.CHAR.equals(metaFieldValue.getField().getDataType()) && MetaFieldExternalHelper.hasSpecialCharacters(new String((char[]) metaFieldValue.getValue()))) {
                dto.getInstrument().getMetaFields().get(i).setValue(savedMetaField.getValue());
            }
        }
        return dto;
    }

    public void createMap(InvoiceDTO invoice, BigDecimal amount) {
        BigDecimal realAmount;
        if (new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_FAIL) || new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_UNAVAILABLE)) {
            realAmount = BigDecimal.ZERO;
        } else {
            realAmount = amount;
        }
        mapDas.create(invoice, payment, realAmount);
    }

    /**
     * Updates a payment record, including related cheque or credit card
     * records. Only valid for entered payments not linked to an invoice.
     *
     * @param dto The DTO with all the information of the new payment record.
     */
    public void update(Integer executorId, PaymentDTOEx dto)
            throws SessionInternalError {
        // the payment should've been already set when constructing this
        // object
        if (payment == null) {
            EmptyResultDataAccessException ex = new EmptyResultDataAccessException("Payment to update not set", 1);
            String errMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                    .status(LogConstants.STATUS_NOT_SUCCESS.toString()).action(LogConstants.ACTION_UPDATE.toString())
                    .message("Payment to update not set").build().toString();
            logger.warn(errMsg, ex);
            throw ex;
        }

        // we better log this, so this change can be traced
        eLogger.audit(executorId, payment.getBaseUser().getId(),
                Constants.TABLE_PAYMENT, payment.getId(),
                EventLogger.MODULE_PAYMENT_MAINTENANCE,
                EventLogger.ROW_UPDATED, null, payment.getAmount().toString(),
                null);

        payment.setPaymentResult(dto.getPaymentResult());

        // start with the payment's own fields
        payment.setUpdateDatetime(Calendar.getInstance().getTime());
        payment.setAmount(dto.getAmount());
        // since the payment can't be linked to an invoice, the balance
        // has to be equal to the total of the payment
        if (CollectionUtils.isEmpty(dto.getInvoiceIds()) && dto.getIsRefund() == 0) {
            payment.setBalance(dto.getAmount());
        }

        payment.setPaymentDate(dto.getPaymentDate());

        // the payment period length this payment was expected to last
        if (dto.getPaymentPeriod() != null) {
            payment.setPaymentPeriod(dto.getPaymentPeriod());

        }

        // Update payment instrument information only when the payment status is Entered
        if (CommonConstants.PAYMENT_RESULT_ENTERED.equals(dto.getResultId())) {
            List<PaymentInstrumentInfoDTO> paymentInstrumentInfoList = payment.getPaymentInstrumentsInfo();
            PaymentInstrumentInfoDAS paymentInstrumentInfoDAS = new PaymentInstrumentInfoDAS();

            Iterator<PaymentInstrumentInfoDTO> it = paymentInstrumentInfoList.iterator();
            while (it.hasNext()) {
                PaymentInstrumentInfoDTO paymentInstrumentInfoDTO = it.next();
                paymentInstrumentInfoDAS.delete(paymentInstrumentInfoDTO);
                it.remove();
            }

            if (!dto.getPaymentInstruments().isEmpty()) {
                PaymentInformationDTO paymentInformationDTO = dto.getPaymentInstruments().get(0);
                if (paymentInformationDTO != null) {
                    payment.setPaymentMethod(paymentInformationDTO.getPaymentMethod());
                    paymentInstrumentInfoList.add(new PaymentInstrumentInfoDTO(payment,
                            dto.getPaymentResult(), paymentInformationDTO.getPaymentMethod(), paymentInformationDTO.getSaveableDTO()));
                }
            }
        }

        // the notes related to this payment
        if (dto.getPaymentNotes() != null) {
            payment.setPaymentNotes(dto.getPaymentNotes());
        }
        UserDTO userDTO = new UserBL(dto.getUserId()).getEntity();
        payment.updateMetaFieldsWithValidation(userDTO.getLanguage().getId(),
                userDTO.getEntity().getId(), null, dto);

        String msg = "Payment with ID: " + payment.getId() + " has been successfully updated.";
        logger.info(getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS));
    }

    /**
     * Goes through the payment pluggable tasks, and calls them with the payment
     * information to get the payment processed. If a call fails because of the
     * availability of the processor, it will try with the next task. Otherwise
     * it will return the result of the process (approved or declined).
     *
     * @return the constant of the result allowing for the caller to attempt it
     * again with different payment information (like another cc number)
     */
    public Integer processPayment(Integer entityId, PaymentDTOEx info, Integer executorUserId)
            throws SessionInternalError {
        Integer retValue = null;
        try {
            PluggableTaskManager taskManager = new PluggableTaskManager(entityId, Constants.PLUGGABLE_TASK_PAYMENT);
            PaymentTask task = (PaymentTask) taskManager.getNextClass();

            if (task == null) {
                String errMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                        .status(LogConstants.STATUS_NOT_SUCCESS.toString())
                        .action(LogConstants.ACTION_EVENT.toString())
                        .message("No payment pluggable tasks configured").build().toString();
                logger.warn(errMsg);
                return null;
            }

            create(info, executorUserId);
            boolean processorUnavailable = true;
            while (task != null && processorUnavailable) {
                // see if this user has pre-auths
                PaymentInformationBL piBl = new PaymentInformationBL();
                PaymentAuthorizationBL authBL = new PaymentAuthorizationBL();
                PaymentAuthorizationDTO auth = null;
                String msg = "Total instruments for processing are: " + info.getPaymentInstruments().size();
                String logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
                logger.debug(logMsg);
                Iterator<PaymentInformationDTO> iterator = info.getPaymentInstruments().iterator();
                while (iterator.hasNext()) {
                    PaymentInformationDTO instrument = iterator.next();
                    // check if the instrument was blacklisted by filter if yes, then get the next ones
                    while (instrument.isBlacklisted() && iterator.hasNext()) {
                        instrument = iterator.next();
                    }
                    // if all the list is exhausted but we are still on black listed then move on to next processor
                    if (instrument.isBlacklisted()) {
                        break;
                    }

                    auth = authBL.getPreAuthorization(info.getUserId());

                    if (info.getPayment() != null && instrument.getId() == null) {
                        Integer instrumentIdOfPayment = piiDas.getInstrumentIdOfPayment(info.getPayment().getId());
                        if (instrumentIdOfPayment != 0) {
                            instrument.setId(instrumentIdOfPayment);
                        }
                    }

                    PaymentInformationDTO newInstrument;
                    // load fresh instrument if its a saved one or refresh its relations if its a new one
                    if (instrument.getId() != null) {
                        newInstrument = piDas.find(instrument.getId());
                        PaymentMethodDTO paymentMethodDTO = null;
                        paymentMethodDTO = methodDas.find(piBl.getPaymentMethodForPaymentMethodType(newInstrument));
                        // if payment method id is gateway key then used saved payment method id.
                        if (null != paymentMethodDTO && paymentMethodDTO.getId() == Constants.PAYMENT_METHOD_GATEWAY_KEY && null != newInstrument.getPaymentMethodId()) {
                            paymentMethodDTO = methodDas.find(newInstrument.getPaymentMethodId());
                        }
                        newInstrument.setPaymentMethod(paymentMethodDTO);
                    } else {
                        newInstrument = instrument.getDTO();
                        refreshRequiredRelations(newInstrument);
                    }

                    info.setInstrument(newInstrument);

                    if (info.getAutoPayment() != null && info.getAutoPayment() == 1) {
                        for (MetaFieldValue value : newInstrument.getMetaFields()) {
                            if (MetaFieldHelper.isValueOfType(value, MetaFieldType.AUTO_PAYMENT_LIMIT)) {
                                BigDecimal limit = (BigDecimal) value.getValue();
                                if (limit != null && BigDecimal.ZERO.compareTo(limit) < 0 && limit.compareTo(info.getAmount()) < 0) {
                                    msg = "Amount exceeding limit : " + info.getAmount();
                                    logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
                                    logger.debug(logMsg);
                                    info.setAmount(limit);
                                    payment.setAmount(info.getAmount());
                                    break;
                                }
                            }
                        }
                    }
                    msg = "Processing payment with instrument: " + newInstrument.getId();
                    logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
                    logger.debug(logMsg);

                    if (auth != null) {
                        processorUnavailable = task.confirmPreAuth(auth, info);
                        if (!processorUnavailable) {
                            if (new Integer(info.getPaymentResult().getId()).equals(Constants.RESULT_FAIL)) {
                                processorUnavailable = task.process(info);
                            }
                            // in any case, don't use this preAuth again
                            authBL.markAsUsed(info);
                        }
                    } else {
                        processorUnavailable = task.process(info);
                    }
                    if (info.getPaymentResult() != null) { // if task haven't created a result remaining code should be skipped
                        // if this is an output of filter when filter does not fail, no need to save it
                        if (Constants.RESULT_NULL != info.getPaymentResult().getId()) {
                            // create a payment instrument to link to payment information object
                            // create an information record of this payment method to link this instrument to payment
                            // obscure card number if its credit card
                            PaymentInformationDTO saveable = newInstrument.getSaveableDTO();
                            piBl.obscureCreditCardNumber(saveable);
                            payment.getPaymentInstrumentsInfo().add(new PaymentInstrumentInfoDTO(payment, info.getPaymentResult(), newInstrument.getPaymentMethod(), saveable));
                        }


                        // allow the pluggable task to do something if the payment
                        // failed (like notification, suspension, etc ... )
                        if (!processorUnavailable && new Integer(info.getPaymentResult().getId()).equals(Constants.RESULT_FAIL)) {
                            task.failure(info.getUserId(), info.getAttempt());

                            // if the processor was a filter then we need to eliminate the given method as it was not a success while filtering
                            if (task instanceof PaymentFilterTask) {
                                instrument.setBlacklisted(true);
                            }
                        }

                        // trigger an event
                        AbstractPaymentEvent event = AbstractPaymentEvent.forPaymentResult(entityId, info);

                        if (event != null) {
                            EventManager.process(event);
                        }
                        msg = String.format("Status of payment processor: %s", processorUnavailable);
                        logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
                        logger.debug(logMsg);

                        // if the processor has processed payment successfully then no need to iterate over
                        if (Constants.RESULT_OK.equals(info.getPaymentResult().getId())) {
                            processorUnavailable = false;
                            break;
                        }
                    }
                    
                    /* SCA - Strong Customer Authentication
                     * If multiple credit cards attached with customer profile and current credit card needs authentication to complete the payment
                     * then the next credit card should make attempt to process the payment
                    */
                    if(info.isAuthenticationRequired() && !processorUnavailable){
                    	processorUnavailable = false;
                        break;
                    }
                }

                // get the next task
                msg = String.format("Getting next task, processorUnavailable : %s", processorUnavailable);
                logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
                logger.debug(logMsg);
                task = (PaymentTask) taskManager.getNextClass();

            }

            // set last payment method for which given result will be displayed
            payment.setPaymentMethod(info.getInstrument().getPaymentMethod());
            // if after all the tasks, the processor in unavailable,
            // return that

            if (processorUnavailable || info.getPaymentResult() == null || info.getPaymentResult().getId() == Constants.RESULT_NULL) {
                logger.debug("Payment was unsuccessful. UserId: {}, Amount: {}, Result: {}", info.getUserId(), info.getAmount(), info.getPaymentResult());
                retValue = Constants.RESULT_UNAVAILABLE;
                info.setPaymentResult(new PaymentResultDAS().find(retValue));
            } else {
                retValue = info.getPaymentResult().getId();
            }

            String msg = String.format("Result for payment ID %d is %s", payment.getId(), retValue);
            String logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
            logger.info(logMsg);
            payment.setPaymentResult(new PaymentResultDAS().find(retValue));

            // the balance of the payment depends on the result
            if (retValue.equals(Constants.RESULT_OK) || retValue.equals(Constants.RESULT_ENTERED)) {
                //#3788 - Partial Refunds Adjustment to Linked Payment
                if (payment.getIsRefund() == 0) {
                    payment.setBalance(payment.getAmount());
                } else {// Payment is a Refund
                    // fetch the linked payment from database
                    PaymentDTO linkedPayment = new PaymentBL(payment
                            .getPayment().getId()).getEntity();
                    if (null != linkedPayment) {
                        /* Since payment is not linked to any invoice now, we
                         * must subtract the payment balance with that of the
                         * refund payment value */
                        linkedPayment.setBalance(linkedPayment.getBalance()
                                .subtract(payment.getAmount()));
                    } else {
                        msg = "This refund is not linked with any payment which is wrong";
                        logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_NOT_SUCCESS);
                        logger.error(logMsg);
                        // maybe throw exception
                    }
                }
            } else {
                payment.setBalance(BigDecimal.ZERO);
            }
        } catch (SessionInternalError e) {
            String logMsg = getEnhancedLogMessage(e.getMessage(), LogConstants.STATUS_NOT_SUCCESS);
            logger.error(logMsg);
            throw e;
        } catch (Exception e) {
            String logMsg = getEnhancedLogMessage("Problems handling payment task.", LogConstants.STATUS_NOT_SUCCESS);
            logger.error(logMsg, e);
            throw new SessionInternalError("Problems handling payment task.");
        }

        //send notification of all result types: OK, Entered, Failed
        String msg = "Sending notification to customer with retValue ****** " + retValue;
        String logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
        logger.debug(logMsg);
        sendNotification(info, entityId);

        return retValue;
    }

    private String getEnhancedLogMessage(String msg, LogConstants status) {
        return new LogMessage.Builder()
        .module(LogConstants.MODULE_PAYMENT.toString()).status(status.toString())
        .action(LogConstants.ACTION_EVENT.toString()).message(msg).build().toString();

    }

    public PaymentDTO getDTO() {
        if (null == payment) {
            return null;
        }
        PaymentDTO dto = new PaymentDTO(payment.getId(), payment.getAmount(), payment.getBalance(), payment.getCreateDatetime(), payment.getUpdateDatetime(), payment.getPaymentDate(), payment.getAttempt(), payment.getDeleted(),
                payment.getPaymentMethod(), payment.getPaymentResult(), payment.getIsRefund(), payment.getIsPreauth(), payment.getCurrency(), payment.getBaseUser());
        dto.setMetaFields(new LinkedList<>(payment.getMetaFields()));
        //for refunds
        dto.setProvisioningCommands(payment.getProvisioningCommands());
        dto.setPayment(payment.getPayment());
        return dto;
    }

    public PaymentDTOEx getDTOEx(Integer language) {
        PaymentDTO paymentDTO = getDTO();
        if (null == paymentDTO) {
            return null;
        }
        PaymentDTOEx dto = new PaymentDTOEx(paymentDTO);
        dto.setUserId(payment.getBaseUser().getUserId());
        // now add all the invoices that were paid by this payment
        Iterator it = payment.getInvoicesMap().iterator();
        while (it.hasNext()) {
            PaymentInvoiceMapDTO map = (PaymentInvoiceMapDTO) it.next();
            dto.getInvoiceIds().add(map.getInvoiceEntity().getId());

            dto.addPaymentMap(getMapDTO(map.getId()));
        }

        // payment method (international)
        PaymentMethodDTO method = payment.getPaymentMethod();
        if (method != null) {
            dto.setMethod(method.getDescription(language));
        }

        // refund fields if applicable
        dto.setIsRefund(payment.getIsRefund());
        if (payment.getPayment() != null && payment.getId() != payment.getPayment().getId()) {
            PaymentBL linkedPayment = new PaymentBL(payment.getPayment().getId());
            //#1890 - linking it to a payment
            dto.setPayment(linkedPayment.getDTOEx(language));
        }

        // the first authorization if any
        if (!payment.getPaymentAuthorizations().isEmpty()) {
            PaymentAuthorizationBL authBL = new PaymentAuthorizationBL(payment.getPaymentAuthorizations().iterator().next());
            dto.setAuthorization(authBL.getDTO());
        }

        // the result in string mode (international)
        if (payment.getPaymentResult() != null) {
            PaymentResultDTO result = payment.getPaymentResult();
            dto.setResultStr(result.getDescription(language));
        }

        // to which payout this payment has been included
        if (payment.getPartnerPayouts().size() > 0) {
            dto.setPayoutId(((PartnerPayout) payment.getPartnerPayouts().toArray()[0]).getId());
        }

        // the payment period length this payment was expected to last
        if (payment.getPaymentPeriod() != null) {
            dto.setPaymentPeriod(payment.getPaymentPeriod());

        }
        // the notes related to this payment
        if (payment.getPaymentNotes() != null) {
            dto.setPaymentNotes(payment.getPaymentNotes());
        }

        // transfer provisioning commands from DTO to DTOex
        if (payment.getProvisioningCommands() != null) {
            dto.setProvisioningCommands(payment.getProvisioningCommands());
        }

        //payment instruments info result
        dto.setPaymentInstrumentsInfo(payment.getPaymentInstrumentsInfo());
        if(null != payment.getPaymentInstrumentsInfo() && !payment.getPaymentInstrumentsInfo().isEmpty()) {
            dto.setInstrument((payment.getPaymentInstrumentsInfo().size()) > 1 ?
                    payment.getPaymentInstrumentsInfo().get(payment.getPaymentInstrumentsInfo().size() - 1).getPaymentInformation() :
                        payment.getPaymentInstrumentsInfo().get(0).getPaymentInformation());
        }

        return dto;
    }

    public static synchronized PaymentWS getWS(PaymentDTOEx dto) {
        if (null == dto) {
            return null;
        }
        PaymentWS ws = new PaymentWS();
        ws.setId(dto.getId());
        ws.setUserId(dto.getUserId());
        ws.setAmount(dto.getAmount());
        ws.setAttempt(dto.getAttempt());
        ws.setBalance(dto.getBalance());
        ws.setCreateDatetime(dto.getCreateDatetime());
        ws.setDeleted(dto.getDeleted());
        ws.setIsPreauth(dto.getIsPreauth());
        ws.setIsRefund(dto.getIsRefund());
        ws.setPaymentDate(dto.getPaymentDate());
        ws.setUpdateDatetime(dto.getUpdateDatetime());
        ws.setPaymentNotes(dto.getPaymentNotes());
        ws.setPaymentPeriod(dto.getPaymentPeriod());

        ws.setMetaFields(MetaFieldBL.convertMetaFieldsToWS(
                new UserBL().getEntityId(dto.getUserId()), dto));

        if (dto.getCurrency() != null) {
            ws.setCurrencyId(dto.getCurrency().getId());
        }

        if (dto.getPaymentResult() != null) {
            ws.setResultId(dto.getPaymentResult().getId());
        }

        ws.setUserId(dto.getUserId());
        ProvisioningCommandBL commandBL = new ProvisioningCommandBL();
        ws.setProvisioningCommands(commandBL.getCommandsList(dto));

        ws.setMethod(dto.getMethod());

        if (dto.getAuthorization() != null) {
            com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO authDTO = new com.sapienter.jbilling.server.entity.PaymentAuthorizationDTO();
            authDTO.setAVS(dto.getAuthorization().getAvs());
            authDTO.setApprovalCode(dto.getAuthorization().getApprovalCode());
            authDTO.setCardCode(dto.getAuthorization().getCardCode());
            authDTO.setCode1(dto.getAuthorization().getCode1());
            authDTO.setCode2(dto.getAuthorization().getCode2());
            authDTO.setCode3(dto.getAuthorization().getCode3());
            authDTO.setCreateDate(dto.getAuthorization().getCreateDate());
            authDTO.setId(dto.getAuthorization().getId());
            authDTO.setMD5(dto.getAuthorization().getMd5());
            authDTO.setProcessor(dto.getAuthorization().getProcessor());
            authDTO.setResponseMessage(dto.getAuthorization().getResponseMessage());
            authDTO.setTransactionId(dto.getAuthorization().getTransactionId());

            ws.setAuthorization(authDTO);
        } else {
            ws.setAuthorization(null);
        }

        ws.setInvoiceIds(dto.getInvoiceIds().toArray(new Integer[0]));

        if (dto.getPayment() != null) {
            ws.setPaymentId(dto.getPayment().getId());
        } else {
            ws.setPaymentId(null);
        }

        // set payment specific instruments, payment instruments are linked through the PaymentInstrumentInfo
        logger.debug("Payment instruments info are: {}", dto.getPaymentInstrumentsInfo());
        if (CollectionUtils.isNotEmpty(dto.getPaymentInstrumentsInfo())) {
            for (PaymentInstrumentInfoDTO paymentInstrument : dto.getPaymentInstrumentsInfo()) {
                ws.getPaymentInstruments().add(PaymentInformationBL.getWS(paymentInstrument.getPaymentInformation()));
            }
        }

        // set user payment instruments of user if this call is coming from findPaymentInstruments
        for (PaymentInformationDTO paymentInstrument : dto.getPaymentInstruments()) {
            ws.getUserPaymentInstruments().add(PaymentInformationBL.getWS(paymentInstrument));
        }

        if (dto.getPaymentMethod() != null) {
            ws.setMethodId(dto.getPaymentMethod().getId());
        }

        PaymentDTO savedPayment = new PaymentDAS().findNow(dto.getId());
        if(null!=savedPayment) {
            Set<PaymentInvoiceMapDTO> paymentInvoiceMap = savedPayment.getInvoicesMap();
            if(CollectionUtils.isNotEmpty(paymentInvoiceMap)) {
                ws.setPaymentInvoiceMap(paymentInvoiceMap
                        .stream()
                        .map(PaymentBL::convertToPaymentInvoiceMapWS)
                        .toArray(PaymentInvoiceMapWS[]::new));
            }
        }
        ws.setAccessEntities(getAccessEntities(new UserDAS().find(dto.getUserId())));
        return ws;
    }

    /**
     * Converts {@link PaymentInvoiceMapDTO} to {@link PaymentInvoiceMapWS}
     * @param dto
     * @return
     */
    private static PaymentInvoiceMapWS convertToPaymentInvoiceMapWS(PaymentInvoiceMapDTO dto) {
        CompanyDTO entity = dto.getPayment().getBaseUser().getEntity();
        Date createDateTime = TimezoneHelper.convertToTimezone(dto.getCreateDatetime(),
                TimezoneHelper.getCompanyLevelTimeZone(entity.getId()));
        PaymentInvoiceMapWS paymentInvoiceMap = new PaymentInvoiceMapWS(dto.getId(), dto.getInvoiceEntity().getId(),
                dto.getPayment().getId(), createDateTime, dto.getAmount());
        paymentInvoiceMap.setPaymentType(dto.getPayment().getIsRefund());
        paymentInvoiceMap.setPaymentStatus(dto.getPayment().getPaymentResult().getDescription(entity.getLanguageId()));
        return paymentInvoiceMap;
    }

    public CachedRowSet getList(Integer entityID, Integer languageId,
            Integer userRole, Integer userId, boolean isRefund) throws Exception {

        // the first variable specifies if this is a normal payment or
        // a refund list
        if (userRole.equals(Constants.TYPE_ROOT) || userRole.equals(Constants.TYPE_CLERK)) {
            prepareStatement(PaymentSQL.rootClerkList);
            cachedResults.setInt(1, isRefund ? 1 : 0);
            cachedResults.setInt(2, entityID);
            cachedResults.setInt(3, languageId);
        } else if (userRole.equals(Constants.TYPE_PARTNER)) {
            prepareStatement(PaymentSQL.partnerList);
            cachedResults.setInt(1, isRefund ? 1 : 0);
            cachedResults.setInt(2, entityID);
            cachedResults.setInt(3, userId);
            cachedResults.setInt(4, languageId);
        } else if (userRole.equals(Constants.TYPE_CUSTOMER)) {
            prepareStatement(PaymentSQL.customerList);
            cachedResults.setInt(1, isRefund ? 1 : 0);
            cachedResults.setInt(2, userId);
            cachedResults.setInt(3, languageId);
        } else {
            throw new Exception("The payments list for the type " + userRole + " is not supported");
        }

        execute();
        conn.close();
        return cachedResults;
    }

    /**
     * Validates the deletion of a payment
     *
     * @param
     * @return boolean
     */
    public boolean ifRefunded() {
        String msg = String.format("Checking if The payment id %d is refunded. ", payment.getId());
        String logMsg = getEnhancedLogMessage(msg, LogConstants.STATUS_SUCCESS);
        logger.debug(logMsg);
        return new PaymentDAS().isRefundedPartiallyOrFully(payment.getId());
    }

    /**
     * Does the actual work of deleting the payment
     *
     * @throws SessionInternalError
     */
    public void delete() throws SessionInternalError {

        try {
            String msg = String.format("Deleting payment with ID: %s", payment.getId());
            String message = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                    .action(LogConstants.ACTION_DELETE.toString()).message(msg).build().toString();
            logger.debug(message);
            Integer entityId = payment.getBaseUser().getEntity().getId();
            EventManager.process(new PaymentDeletedEvent(entityId, payment));
            //check if the payment is a refund & has entered status, then only allow to delete it.
            if (payment.getIsRefund() == 1 && payment.getResultId() != null && payment.getResultId() == 4) {
                PaymentDTO originalPayment = paymentDas.find(payment.getPayment().getId());
                // Add up entered refund payment amount back to original payment balance.
                originalPayment.setBalance(originalPayment.getBalance().add(payment.getAmount()));
                logger.debug("Original payment Id: {}", originalPayment.getId());
                paymentDas.save(originalPayment);
                // Soft delete entered refund payment
                payment.setDeleted(1);
            } else {
                payment.setUpdateDatetime(Calendar.getInstance().getTime());
                payment.setDeleted(1);
            }

            eLogger.auditBySystem(entityId, payment.getBaseUser().getId(),
                    Constants.TABLE_PAYMENT, payment.getId(),
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_DELETED, null, null, null);

            String logMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                    .action(LogConstants.ACTION_DELETE.toString()).status(LogConstants.STATUS_SUCCESS.toString())
                    .message("Payment with ID: " + payment.getId() + " has been deleted.").build().toString();
            logger.info(logMsg);
        } catch (Exception e) {
            String errMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
                    .status(LogConstants.STATUS_NOT_SUCCESS.toString())
                    .action(LogConstants.ACTION_DELETE.toString()).message("Problem deleting payment.").build().toString();
            logger.warn(errMsg, e);
            throw new SessionInternalError("Problem deleting payment.");
        }
    }

    /*
     * This is the list of payment that are refundable. It shows when entering a
     * refund.
     */
    public CachedRowSet getRefundableList(Integer languageId, Integer userId) throws Exception {
        prepareStatement(PaymentSQL.refundableList);
        cachedResults.setInt(1, 0); // is not a refund
        cachedResults.setInt(2, userId);
        cachedResults.setInt(3, languageId);
        execute();
        conn.close();
        return cachedResults;
    }

    public static synchronized PaymentDTOEx findPaymentInstrument(Integer entityId,
            Integer userId) throws PluggableTaskException,
            SessionInternalError, TaskException {

        PluggableTaskManager taskManager = new PluggableTaskManager(entityId,
                Constants.PLUGGABLE_TASK_PAYMENT_INFO);
        PaymentInfoTask task = (PaymentInfoTask) taskManager.getNextClass();

        if (task == null) {
            // at least there has to be one task configurated !
            LoggerFactory.getLogger(PaymentBL.class).error("No payment info pluggable tasks configurated for entity {}", entityId);
            throw new SessionInternalError("No payment info pluggable" + "tasks configurated for entity " + entityId);
        }

        // get this payment information. Now we only expect one pl.tsk
        // to get the info, I don't see how more could help
        return task.getPaymentInfo(userId);

    }


    public Integer getLatest(Integer userId) throws SessionInternalError {
        Integer retValue = null;
        try {
            prepareStatement(PaymentSQL.getLatest);
            cachedResults.setInt(1, userId);
            cachedResults.setInt(2, userId);
            execute();
            if (cachedResults.next()) {
                int value = cachedResults.getInt(1);
                if (!cachedResults.wasNull()) {
                    retValue = value;
                }
            }
            cachedResults.close();
            conn.close();
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }

        return retValue;
    }

    public Integer[] getManyWS(Integer userId, Integer limit, Integer offset, Integer languageId) {
        List<Integer> result = new PaymentDAS().findIdsByUserLatestFirst(userId, limit, offset);
        return result.toArray(new Integer[result.size()]);

    }

    public PaymentDTO[] getLastPayments(Integer userId, Integer limit) {
        List<PaymentDTO> result = new PaymentDAS().findPaymentsByUserPaged(
                userId, limit, 0);
        return result.toArray(new PaymentDTO[result.size()]);

    }

    public Integer[] getListIdsByDate(Integer userId, Date since, Date until) {

        // add a day to include the until date
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(until);
        cal.add(GregorianCalendar.DAY_OF_MONTH, 1);
        until = cal.getTime();

        List<Integer> result = new PaymentDAS().findIdsByUserAndDate(userId, since, until);
        return result.toArray(new Integer[result.size()]);
    }

    /**
     * This method used for find the payment id's by entityId.
     *
     * @param entityId
     * @return List<Integer>
     */
    public List<Integer> findIdsByEntity(Integer entityId) {
        if (entityId == null) {
            return null;
        }
        return new PaymentDAS().findIdsByEntity(entityId);
    }

    /**
     * Revenue = Payments minus Refunds
     *
     * @param userId
     * @param from   (optional) From date for payments
     * @param until  (optional) Until date for payments
     * @return
     */
    public BigDecimal findTotalRevenueByUser(Integer userId, Date from, Date until) {
        return paymentDas.findTotalRevenueByUser(userId, from, until);
    }

    private List<PaymentDTO> getPaymentsWithBalance(Integer userId) {
        // this will usually return 0 or 1 records, rearly a few more
        List<PaymentDTO> paymentsList;
        Collection payments = paymentDas.findWithBalance(userId);

        if (payments != null) {
            paymentsList = new ArrayList<>(payments); // needed for the
            // sort
            Collections.sort(paymentsList, new PaymentEntityComparator());
            Collections.reverse(paymentsList);
        } else {
            paymentsList = new ArrayList<>(); // empty
        }

        return paymentsList;
    }

    /**
     * Given an invoice, the system will look for any payment with a balance
     * and get the invoice paid with this payment.
     */
    public boolean automaticPaymentApplication(InvoiceDTO invoice)
            throws SQLException {
        boolean appliedAtAll = false;

        List<PaymentDTO> payments = getPaymentsWithBalance(invoice.getBaseUser().getUserId());

        //Bug fix 9970 - The older Payment's balance should be used to pay the
        //last invoice before new Payment's balance is used
        Collections.sort(payments, (o1, o2) -> o1.getCreateDatetime().compareTo(o2.getCreateDatetime()));

        for (int f = 0; f < payments.size() && invoice.getBalance().compareTo(BigDecimal.ZERO) > 0; f++) {
            payment = payments.get(f);
            if (new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_FAIL) || new Integer(payment.getPaymentResult().getId()).equals(Constants.RESULT_UNAVAILABLE)) {
                continue;
            }
            if (applyPaymentToInvoice(invoice)) {
                appliedAtAll = true;
            }
        }

        return appliedAtAll;
    }

    /**
     * Give an payment (already set in this object), it will look for any
     * invoices with a balance and get them paid, starting wiht the oldest.
     */
    public boolean automaticPaymentApplication() throws SQLException {
        boolean appliedAtAll = false;
        if (BigDecimal.ZERO.compareTo(payment.getBalance()) >= 0) {
            return false; // negative payment, skip
        }

        for (InvoiceDTO invoice : new InvoiceDAS().findWithBalanceOldestFirstByUser(payment.getBaseUser())) {
            // negative balances don't need paying
            if (BigDecimal.ZERO.compareTo(invoice.getBalance()) > 0) {
                continue;
            }

            //apply and set
            if (applyPaymentToInvoice(invoice)) {
                appliedAtAll = true;
            }

            if (BigDecimal.ZERO.compareTo(payment.getBalance()) >= 0) {
                break; // no payment balance remaining
            }
        }
        return appliedAtAll;
    }

    private boolean applyPaymentToInvoice(InvoiceDTO invoice) throws SQLException {
        // this is not actually getting de Ex, so it is faster
        PaymentDTOEx dto = new PaymentDTOEx(getDTO());

        // not pretty, but the methods are there
        IPaymentSessionBean psb = Context.getBean(Context.Name.PAYMENT_SESSION);
        // make the link between the payment and the invoice
        boolean isPaymentSuccessful = Constants.RESULT_OK.equals(dto.getResultId()) || Constants.RESULT_ENTERED.equals(dto.getResultId());
        BigDecimal paidAmount = psb.applyPayment(dto, invoice, isPaymentSuccessful);
        createMap(invoice, paidAmount);
        dto.getInvoiceIds().add(invoice.getId());

        // notify the customer
        dto.setUserId(invoice.getBaseUser().getUserId()); // needed for the
        // notification
        // the notification only understands ok or not, if the payment is
        // entered
        // it has to show as ok
        dto.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_OK));
        //sendNotification(dto, payment.getBaseUser().getEntity().getId());

        try {
            dto.close();
        } catch (Exception e) {
            logger.error("Exception in automaticPaymentApplication",e);
        }

        return true;
    }

    /**
     * sends an notification with a payment
     */
    public void sendNotification(PaymentDTOEx info, Integer entityId) {
        sendNotification(info, entityId, info.getPaymentResult().getId());
    }

    public void sendNotification(PaymentDTOEx info, Integer entityId, int result) {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getPaymentMessage(entityId, info, result);

            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
            notificationSess.notify(info.getUserId(), message);
        } catch (NotificationNotFoundException e1) {
            // won't send anyting because the entity didn't specify the
            // notification
            logger.warn("Can not notify a customer about a payment beacuse the entity lacks the notification. entity = {}", entityId);
        } catch (Exception exception) {
            logger.debug("Exception: ", exception);
        }
    }

    // for sending payment successful notification when invoice is not linked with payment.
    public void sendNotification(PaymentDTOEx info, Integer entityId, int result, Integer invoiceId) {
        try {
            NotificationBL notif = new NotificationBL();
            MessageDTO message = notif.getPaymentMessage(entityId, info, result);

            INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);

            InvoiceBL invoice = new InvoiceBL(invoiceId);
            UserBL user = new UserBL(invoice.getEntity().getUserId());
            message.addParameter("invoice_number", invoice.getEntity().getPublicNumber());
            message.addParameter("invoice", invoice.getEntity());
            message.addParameter("method", info.getInstrument().getPaymentMethod().getDescription(user.getEntity().getLanguage().getId()));
            notificationSess.notify(info.getUserId(), message);
        } catch (NotificationNotFoundException e1) {
            // won't send anyting because the entity didn't specify the
            // notification
            logger.warn("Can not notify a customer about a payment beacuse the entity lacks the notification. entity = {}", entityId);

        }
    }

    /*
     * The payment doesn't have to be set. It adjusts the balances of both the
     * payment and the invoice and deletes the map row.
     */
    public void removeInvoiceLink(Integer mapId) {
        try {
            // declare variables
            InvoiceDTO invoice;

            // find the map
            PaymentInvoiceMapDTO map = mapDas.find(mapId);
            // start returning the money to the payment's balance
            BigDecimal amount = map.getAmount();
            payment = map.getPayment();
            amount = amount.add(payment.getBalance());
            payment.setBalance(amount);

            // the balace of the invoice also increases
            invoice = map.getInvoiceEntity();
            amount = map.getAmount().add(invoice.getBalance());
            invoice.setBalance(amount);

            // this invoice probably has to be paid now
            if (invoice.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setToProcess(1);
            }

            // log that this was deleted, otherwise there will be no trace
            eLogger.info(invoice.getBaseUser().getEntity().getId(),
                    payment.getBaseUser().getId(), mapId,
                    EventLogger.MODULE_PAYMENT_MAINTENANCE,
                    EventLogger.ROW_DELETED,
                    Constants.TABLE_PAYMENT_INVOICE_MAP);

            // get rid of the map all together
            mapDas.delete(map);

        } catch (EntityNotFoundException enfe) {
            String msg = "Exception removing payment-invoice link: EntityNotFoundException";
            String logMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT_LINK.toString())
                    .action(LogConstants.ACTION_DELETE.toString()).status(LogConstants.STATUS_NOT_SUCCESS.toString())
                    .message(msg).build().toString();
            logger.error(logMsg, enfe);
        } catch (Exception e) {
            String msg = "Exception removing payment-invoice link";
            String logMsg = new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT_LINK.toString())
                    .action(LogConstants.ACTION_DELETE.toString()).status(LogConstants.STATUS_NOT_SUCCESS.toString())
                    .message(msg).build().toString();
            logger.error(logMsg, e);
            throw new SessionInternalError(e);
        }
    }

    /**
     * This method removes the link between this payment and the
     * <i>invoiceId</i> of the Invoice
     *
     * @param invoiceId Invoice Id to be unlinked from this payment
     */
    public boolean unLinkFromInvoice(Integer invoiceId) {

        InvoiceDTO invoice = new InvoiceDAS().find(invoiceId);
        Iterator<PaymentInvoiceMapDTO> it = invoice.getPaymentMap().iterator();
        boolean bSucceeded = false;
        while (it.hasNext()) {
            PaymentInvoiceMapDTO map = it.next();
            if (this.payment.getId() == map.getPayment().getId()) {
                this.removeInvoiceLink(map.getId());
                invoice.getPaymentMap().remove(map);
                payment.getInvoicesMap().remove(map);
                bSucceeded = true;

                //fire event
                PaymentUnlinkedFromInvoiceEvent event = new PaymentUnlinkedFromInvoiceEvent(
                        payment.getBaseUser().getEntity().getId(),
                        new PaymentDTOEx(payment),
                        invoice,
                        map.getAmount());
                EventManager.process(event);
                break;
            }
        }
        return bSucceeded;
    }

    public PaymentInvoiceMapDTOEx getMapDTO(Integer mapId) {
        // find the map
        PaymentInvoiceMapDTO map = mapDas.find(mapId);
        PaymentInvoiceMapDTOEx dto = new PaymentInvoiceMapDTOEx(map.getId(), map.getAmount(), map.getCreateDatetime());
        dto.setPaymentId(map.getPayment().getId());
        dto.setInvoiceId(map.getInvoiceEntity().getId());
        dto.setCurrencyId(map.getPayment().getCurrency().getId());
        return dto;
    }

    public List<PaymentDTO> findUserPaymentsPaged(Integer entityId, Integer userId, Integer limit, Integer offset) {

        return new PaymentDAS().findPaymentsByUserPaged(userId, limit, offset);
    }

    public void setProvisioningStatus(Integer provisioningStatus) {
        ProvisioningStatusDAS provisioningStatusDas = new ProvisioningStatusDAS();
        Integer oldStatus = payment.getProvisioningStatusId();

        payment.setProvisioningStatus(provisioningStatusDas.find(provisioningStatus));
        logger.debug("payment {}: updated provisioning status : {}", payment, payment.getProvisioningStatusId());

        Integer userId = payment.getBaseUser().getId();
        Integer companyId = payment.getBaseUser().getCompany().getId();

        // add a log for provisioning module
        eLogger.auditBySystem(companyId, userId,
                Constants.TABLE_ASSET, payment.getId(),
                EventLogger.MODULE_PROVISIONING, EventLogger.PROVISIONING_STATUS_CHANGE,
                oldStatus, null, null);
    }

    private void refreshRequiredRelations(PaymentInformationDTO newInstrument) {
        if (newInstrument.getPaymentMethod() != null) {
            newInstrument.setPaymentMethod(methodDas.find(newInstrument.getPaymentMethod().getId()));
        }

        if (newInstrument.getPaymentMethodType() != null) {
            newInstrument.setPaymentMethodType(new PaymentMethodTypeDAS().find(newInstrument.getPaymentMethodType().getId()));
        }
    }

    public Integer getLatestId(Integer userId, Integer resultId) {
        return new PaymentDAS().getLatest(userId, resultId);
    }

    public Integer[] getPaymentByUserId(Integer userId) {
        List<Integer> result = new PaymentDAS().findPaymentByUserIdOldestFirst(userId);
        return result.toArray(new Integer[result.size()]);
    }

    public Integer[] getAllPaymentsByUser(Integer userId) {
        List<Integer> results = new PaymentDAS().findIdsByUserId(userId);
        return results.toArray(new Integer[results.size()]);
    }

    public List<PaymentWS> findPaymentsByUserPagedSortedByAttribute(Integer userId, int maxResults, int offset,
            String sortAttribute, ListField.Order order, Integer callerLanguageId) {
        List<PaymentDTO> payments = new PaymentDAS().findPaymentsByUserPagedSortedByAttribute(userId, maxResults, offset, sortAttribute, order);
        if (payments == null) {
            return Collections.emptyList();
        }
        List<PaymentWS> paymentsWS = new ArrayList<>();
        for (PaymentDTO dto : payments) {
            PaymentBL bl = new PaymentBL(dto.getId());
            PaymentWS wsdto = PaymentBL.getWS(bl.getDTOEx(callerLanguageId));
            paymentsWS.add(wsdto);
        }
        return paymentsWS;
    }


    public PaymentDTO transferPaymentToUser(PaymentTransferWS paymentTransferWS) {
        this.payment.setBaseUser(new UserDAS().find(paymentTransferWS.getToUserId()));
        return paymentDas.save(this.payment);
    }

    private PaymentDTOEx getValidPaymentDTOEx(PaymentWS payment, Integer entityId) {
        if (null == payment) {
            logger.debug("Supplied Payment is null.");
            throw new SessionInternalError("Payment processing parameters not found!");
        }
        logger.debug("In process payment");

        Integer userId = payment.getOwningUserId();
        UserDTO user = new UserDAS().find(userId);
        if (null == user) {
            logger.debug("No owning user for payment id: {}", payment.getId());
            throw new SessionInternalError("There is not user for the supplied payment.");
        }

        Integer userCompanyId = user.getEntity().getId();
        if (!userCompanyId.equals(entityId)) {
            logger.debug("Payment owing entity id: {} not equals with invoking entity id: {}", userCompanyId, entityId);
            throw new SessionInternalError("Processing another entity's payments not supported!!");
        }

        // apply validations for refund payment
        if (payment.getIsRefund() == 1) {
            if (!PaymentBL.validateRefund(payment)) {
                throw new SessionInternalError("Either refund payment was not linked to any payment or the refund amount is in-correct",
                        new String[]{"PaymentWS,paymentId,validation.error.apply.without.payment.or.different.linked.payment.amount"});
            }
        }

        logger.debug("before dto conversion: {}", payment.getPaymentInstruments());
        if (null != payment.getPaymentInstruments() && !payment.getPaymentInstruments().isEmpty()) {
            PaymentInformationDAS paymentInformationDAS = new PaymentInformationDAS();
            for (PaymentInformationWS paymentInstrument : payment.getPaymentInstruments()) {
                if (!paymentInformationDAS.exists(payment.getUserId(), paymentInstrument.getPaymentMethodTypeId())) {
                    paymentInstrument.setId(null);
                }
            }
        }

        PaymentDTOEx dto = new PaymentDTOEx(payment);
        logger.debug("after dto conversion: {}", dto.getPaymentInstruments());
        // payment without Credit Card or ACH, fetch the users primary payment instrument for use
        if (dto.getPaymentInstruments().isEmpty()) {
            logger.debug("processPayment() called without payment method, fetching users automatic payment instrument.");
            try {
                PaymentDTOEx userPaymentInstrument = PaymentBL.findPaymentInstrument(entityId, payment.getUserId());
                if (null == userPaymentInstrument || userPaymentInstrument.getPaymentInstruments().isEmpty()) {
                    throw new SessionInternalError("User " + payment.getUserId() + "does not have a default payment instrument.",
                            new String[]{"PaymentWS,baseUserId,validation.error.no.payment.instrument"});
                }
                dto.setPaymentInstruments(userPaymentInstrument.getPaymentInstruments());

            } catch (Exception ex) {
                if (ex instanceof PluggableTaskException) {
                    throw new SessionInternalError("Exception occurred fetching payment info plug-in.",
                            new String[]{"PaymentWS,baseUserId,validation.error.no.payment.instrument"});
                } else if (ex instanceof TaskException) {
                    throw new SessionInternalError("Exception occurred with plug-in when fetching payment instrument.",
                            new String[]{"PaymentWS,baseUserId,validation.error.no.payment.instrument"});
                }

                throw new SessionInternalError(ex);
            }

        }
        return dto;
    }

    public CustomerSignupResponseWS makeSignupPayment(PaymentWS payment, Integer entityId, Integer callerId) {
        IPaymentSessionBean session = Context.getBean(Context.Name.PAYMENT_SESSION);
        PaymentDTOEx dto = getValidPaymentDTOEx(payment, entityId);
        Integer result = session.processAndUpdateInvoice(dto, null, entityId, callerId);
        logger.debug("paymentBean.processAndUpdateInvoice() Id= {}", result);
        CustomerSignupResponseWS response = new CustomerSignupResponseWS();
        response.setResult(result);
        response.setPaymentId(payment.getId());
        response.setUserId(payment.getUserId());
        response.setResponseCode(dto.getAuthorization().getCode1());
        response.setResponseMessage(dto.getAuthorization().getResponseMessage());
        return response;
    }

    private static List<Integer> getAccessEntities(UserDTO dto) {
        List<Integer> entityIds = new ArrayList<>();
        CompanyDTO company = dto.getEntity();
        while (company != null) {
            entityIds.add(company.getId());
            company = company.getParent();
        }
        return entityIds;
    }

    public void updatePaymentResult(Integer resultId) {
        if (null == resultId) {
            throw new EmptyResultDataAccessException("Payment result not set", 1);
        }
        payment.setPaymentResult(new PaymentResultDAS().find(resultId));
        paymentDas.save(payment);

    }

    public BigDecimal getTotalBalanceByUser(Integer userId) {
        CurrencyBL currencyBL = new CurrencyBL();
        UserDTO user = new UserDAS().find(userId);

        return new PaymentDAS().findTotalBalanceByUser(userId)
                .stream()
                .filter(totalBalance -> totalBalance.getBalance() !=null && totalBalance.getCurrency() != null)
                .map(totalBalance -> currencyBL.convert(totalBalance.getCurrency(),
                        user.getCurrency().getId(),
                        totalBalance.getBalance(),
                        TimezoneHelper.serverCurrentDate(),
                        user.getEntity().getId()))
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
