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
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.SessionFactory;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.hibernate4.HibernateOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.CurrencyBL;
import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.payment.blacklist.CsvProcessor;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.payment.db.PaymentResultDAS;
import com.sapienter.jbilling.server.payment.event.PaymentFailedEvent;
import com.sapienter.jbilling.server.payment.event.PaymentLinkedToInvoiceEvent;
import com.sapienter.jbilling.server.payment.event.PaymentSuccessfulEvent;
import com.sapienter.jbilling.server.payment.event.ProcessPaymentEvent;
import com.sapienter.jbilling.server.process.AgeingBL;
import com.sapienter.jbilling.server.process.ConfigurationBL;
import com.sapienter.jbilling.server.process.db.AgeingEntityStepDTO;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.user.db.UserStatusDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.PreferenceBL;
import com.sapienter.jbilling.server.util.TransactionInfoUtil;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.audit.LogMessage;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.server.util.db.CurrencyDAS;

/**
 *
 * This is the session facade for the payments in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 */
public class PaymentSessionBean implements IPaymentSessionBean {

    private final Logger logger = LoggerFactory.getLogger(PaymentSessionBean.class);

   /**
    * This method goes over all the over due invoices for a given entity and
    * generates a payment record for each of them.
    */
    @Transactional( propagation = Propagation.REQUIRED )
    public void processPayments(Integer entityId) throws SessionInternalError {
        try {
            entityId.intValue(); // just to avoid the warning ;)
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /** 
    * This is meant to be called from the billing process, where the information
    * about how the payment is going to be done is not known. This method will
    * call a pluggable task that finds this information (usually a cc) before
    * calling the realtime processing.
    * Later, this will have to be changed for some file creation with all the
    * payment information to be sent in a batch mode to the processor at the 
    * end of the billing process. 
    * This is called only if the user being process has as a preference to 
    * process the payment with billing process, meaning that a payment has
    * to be created and processed real-time.
    * @return If the payment was not successful for any reason, null, 
    * otherwise the payment method used for the payment
    */
    @Transactional( propagation = Propagation.REQUIRED )
    public Integer generatePayment(InvoiceDTO invoice) 
            throws SessionInternalError {

        logger.debug("Generating payment for invoice {}", invoice.getId());
        // go fetch the entity for this invoice
        Integer userId = invoice.getBaseUser().getUserId();
        UserDAS userDas = new UserDAS();
        Integer entityId = userDas.find(userId).getCompany().getId();
        Integer retValue = null;
        // create the dto with the information of the payment to create
        try {
            // get this payment information. Now we only expect one pl.tsk
            // to get the info, I don't see how more could help
            PaymentDTOEx dto = PaymentBL.findPaymentInstrument(entityId, userId);
            if (dto != null) {
                List<PaymentInformationDTO> paymentInformations = dto.getPaymentInstruments();
                if(PaymentInformationBL.isPaymentAuthorizationPreferenceEnabled(entityId)) {
                    List<PaymentInformationDTO> paymentInstruments = PaymentInformationBL.filterForAutoAuthorization(paymentInformations);
                    if (CollectionUtils.isNotEmpty(paymentInstruments)) {
                        dto.setPaymentInstruments(paymentInstruments);
                    } else {
                        dto = null;
                    }
                }
            }

            boolean noInstrument = false;
            if (dto == null) {
                noInstrument = true;
                dto = new PaymentDTOEx();
            }

            dto.setIsRefund(0); //it is not a refund
            dto.setUserId(userId);
            dto.setAmount(invoice.getBalance());
            dto.setCurrency(new CurrencyDAS().find(invoice.getCurrency().getId()));
            dto.setAttempt(invoice.getPaymentAttempts() + 1);
            // when the payment is generated by the system (instead of
            // entered manually by a user), the payment date is sysdate
            dto.setPaymentDate(TimezoneHelper.convertToTimezone(Calendar.getInstance().getTime(),
                    TimezoneHelper.getCompanyLevelTimeZone(entityId)));

            logger.debug("Prepared payment {}", dto);
            // it could be that the user doesn't have a payment 
            // instrument (cc) in the db, or that is invalid (expired).
            if (!noInstrument) {
                Integer result = processAndUpdateInvoice(dto, invoice.getId(), null);
                logger.debug("After processing. Result= {}", result);
                if (result != null && result.equals(Constants.RESULT_OK)) {
                    retValue = dto.getInstrument().getPaymentMethod().getId();
                }
            } else {
                // audit that this guy was about to get a payment
                EventLogger eventLogger = new EventLogger();
                eventLogger.auditBySystem(entityId, userId,
                                          Constants.TABLE_BASE_USER, userId,
                                          EventLogger.MODULE_PAYMENT_MAINTENANCE, EventLogger.PAYMENT_INSTRUMENT_NOT_FOUND,
                                          null, null, null);
                // update the invoice attempts
                invoice.setPaymentAttempts(dto.getAttempt() == null ? new Integer(1) : dto.getAttempt());
                // treat this as a failed payment
                PaymentFailedEvent event = new PaymentFailedEvent(entityId, dto);
                EventManager.process(event);
            }
            dto.close();
            
        } catch (Exception e) {
            logger.debug("Problems generating payment.", e);
            throw new SessionInternalError(
                "Problems generating payment.");
        }

        logger.debug("Done. Returning:{}", retValue);
        return retValue;
    }
    
    /**
     * This method soft deletes a payment
     * 
     * @param paymentId
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void deletePayment(Integer paymentId) throws SessionInternalError {

        try {
            PaymentBL bl = new PaymentBL(paymentId);
            bl.delete();

        } catch (Exception e) {
            logger.warn("Problem deleteing payment.", e);
            throw new SessionInternalError("Problem deleteing payment");
        }
    }
    
    
    /**
     * It creates the payment record, makes the calls to the authorization
     * processor and updates the invoice if successful.
     *
     * @param dto
     * @param invoiceId
     * @throws SessionInternalError
     */
    public Integer processAndUpdateInvoice(PaymentDTOEx dto, 
            Integer invoiceId, Integer executorUserId) throws SessionInternalError {

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);

        //only these IDs are shared between the
        //first and the second transaction
        Integer result = null;
        Integer paymentId = null;

        TransactionStatus transaction = null;

        /******************************* FIRST TRANSACTION SCOPE **************************************
         *  The first transaction scope is responsible for doing online processing if needed and
         *  creation of a payment record in database. Apart from the payment record in this first
         *  transaction few other records can be created, such as notification messages.
         *********************************************************************************************/

        logger.debug("2-Transaction status before first transaction: {}", TransactionInfoUtil.getTransactionStatus(true));
        try {

            transactionDefinition.setName("2-processAndUpdateInvoice-create-payment-transaction-" + System.nanoTime());
            transaction = transactionManager.getTransaction(transactionDefinition);
            logger.debug("2-Transaction info for payment: {}", TransactionInfoUtil.getTransactionStatus(true));

            InvoiceDTO invoice = new InvoiceBL(invoiceId).getEntity();

            if (invoice.getIsReview().compareTo(1) == 0) {
                String msg = getEnhancedLogMessage("2-Invoice is a review invoice, can not process payment against it.");
                logger.error(msg);
                throw new SessionInternalError("Invoice is a review invoice, can not process payment against it.");
            }

            PaymentBL bl = new PaymentBL();
            Integer entityId = invoice.getBaseUser().getEntity().getId();
            
            // set the attempt
            if (dto.getIsRefund() == 0) {
                // take the attempt from the invoice
                dto.setAttempt(invoice.getPaymentAttempts() + 1);
            } else { // is a refund
                dto.setAttempt(1);
            } 
                
            // payment notifications require some fields from the related
            // invoice
            dto.getInvoiceIds().add(invoice.getId());
                
            // process the payment (will create the db record as well, if
            // there is any actual processing). Do not process negative
            // payments (from negative invoices), unless allowed.
            if (dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
                result = bl.processPayment(entityId, dto, executorUserId);
                paymentId = bl.getEntity().getId(); //remember the payment id
            } else {
                // only process if negative payments are allowed
                int preferenceAllowNegativePayments = 0;
                try {
                    preferenceAllowNegativePayments =
                    	PreferenceBL.getPreferenceValueAsIntegerOrZero(
                    		entityId, Constants.PREFERENCE_ALLOW_NEGATIVE_PAYMENTS);
                    
                } catch (EmptyResultDataAccessException fe) {
                    String msg = getEnhancedLogMessage(fe.getMessage());
                    logger.error(msg);
                    // use default
                }
                if (preferenceAllowNegativePayments == 1) {
                    logger.warn("Processing payment with negative amount {}",dto.getAmount());
                    result = bl.processPayment(entityId, dto, executorUserId);
                    paymentId = bl.getEntity().getId(); //remember the payment id
                } else {
                    logger.warn("Skiping payment processing. Payment with negative amount {}", dto.getAmount());
                }
            }

            // while still in the first transaction scope
            // update the payment record
            if (null != result) {
                bl.getEntity().setPaymentResult(new PaymentResultDAS().find(result));
            }

            String msg = getEnhancedLogMessage("Processed payment with ID: " + paymentId + ", with result: " + result);
            logger.info(msg);

            //the commit will flush the hibernate session
            transactionManager.commit(transaction);

            //after successful commit we are removing only that invoice entity from
            //hibernate session to force the second transaction to reload the entity
            //from database and load a fresh version of that record. This reduces
            //the chances for optimistic locking exception on invoice.
            SessionFactory sessionFactory = Context.getBean(Context.Name.HIBERNATE_SESSION);
            //clear 2nd level cache
            if(sessionFactory.getCache().containsEntity(InvoiceDTO.class, invoice.getId())){
                logger.debug("2-Removing invoiceDTO[{}] entity from 2nd level cache", invoice.getId());
                sessionFactory.getCache().evictEntity(InvoiceDTO.class, invoice.getId());
            }

        } catch (SessionInternalError sie) {
            logger.error("2-Session Internal Error in transaction block.", sie);
            if(!transaction.isCompleted()){
                logger.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw sie;
        } catch(TransactionException te) {
            logger.error("2-Transaction exception occurred.", te);
            throw new SessionInternalError(te);
        } catch (Exception e) {
            logger.error("2-An exception occurred.", e);
            if(!transaction.isCompleted()){
                logger.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw new SessionInternalError(e);
        }


        /******************************* SECOND TRANSACTION SCOPE ********************************
        * The second transaction's job is to apply the payment to the invoice. The problem here is
        * that the invoice could also be updated by different thread/transactions so here we risk
        * optimistic locking exception. Because of this we try to repeat the applying of payment to
        * invoice a number of times.
        *****************************************************************************************/
        try {
            //try to commit transaction with retry
            Exception exception = null;
            int numAttempts = 0;
            do {
                numAttempts++;
                try {
                    if (result != null) {
                        logger.debug("3-Apply payment[{}] to invoice[{}]. Before transaction start: {}", TransactionInfoUtil.getTransactionStatus(true), paymentId, invoiceId);
                        transactionDefinition.setName("3-processAndUpdateInvoice-apply-payment-to-invoice-transaction-" + System.nanoTime());
                        transaction = transactionManager.getTransaction(transactionDefinition);
                        logger.debug("3-Apply payment[{}] to invoice[{}]. After transaction start: {}", TransactionInfoUtil.getTransactionStatus(true), paymentId, invoiceId);

                        PaymentBL paymentBL = new PaymentBL(paymentId);

                        // update the dto with the created id
                        dto.setId(paymentBL.getEntity().getId());
                        // the balance will be the same as the amount
                        // if the payment failed, it won't be applied to the invoice
                        // so the amount will be ignored
                        dto.setBalance(dto.getAmount());

                        // Note: I could use the return of the last call to fetch another
                        // dto with a different cc number to retry the payment

                        //reload invoice data
                        InvoiceDTO invoice = new InvoiceBL(invoiceId).getEntity();
                        // get all the invoice's fields updated with this payment
                        BigDecimal paid = applyPayment(dto, invoice, result.equals(Constants.RESULT_OK));

                        if (dto.getIsRefund() == 0) {
                            // Update the link between invoice and payment. This part of
                            // the code should not cause optimistic locking exception
                            // since it is only an insert against a table.
                            paymentBL.createMap(invoice, paid);
                        }

                        logger.debug("3-Attempting to commit transaction: {}", TransactionInfoUtil.getTransactionStatus(true));
                        transactionManager.commit(transaction);
                        logger.debug("3-Transaction committed: {}", TransactionInfoUtil.getTransactionStatus(true));

                    }
                    return result;

                //catches exceptions for which a retry is wanted
                } catch (HibernateOptimisticLockingFailureException ex) {
                    exception = ex;
                    logger.error("31. Could not commit transaction.", ex);
                    //wait 100 milliseconds
                    Thread.sleep(100);
                } catch (StaleObjectStateException ex){
                    exception = ex;
                    logger.error("32. Could not commit transaction.", ex);
                    //wait 100 milliseconds
                    Thread.sleep(100);
                } catch (DeadlockLoserDataAccessException ex){
                    exception = ex;
                    logger.error("33. Could not commit transaction.", ex);
                    //wait 100 milliseconds
                    Thread.sleep(100);
                }

                logger.debug("3-Applying payment to invoice retry: {}", numAttempts);
            } while (numAttempts <= 10); //retry 10 times

            logger.debug("3. Failed to apply payment[{}] to invoice[{}], propagating exception", paymentId, invoiceId);
            throw exception;
        } catch (SessionInternalError sie){
            if(!transaction.isCompleted()){
                logger.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            logger.error("3-Session Internal Error in transaction block.", sie);
            throw sie;
        } catch (TransactionException te){
            logger.error("3-Transaction exception occurred.", te);
            throw new SessionInternalError(te);
        } catch (Exception e) {
            logger.error("3-An exception occurred.", e);
            if(!transaction.isCompleted()){
                logger.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw new SessionInternalError(e);
        }
    }

    /**
     * This is called from the client to process real-time a payment, usually cc. 
     * 
     * @param dto
     * @param invoiceId
     * @throws SessionInternalError
     */
    public Integer processAndUpdateInvoice(PaymentDTOEx dto, 
            Integer invoiceId, Integer entityId, Integer executorUserId) throws SessionInternalError {

        PlatformTransactionManager transactionManager = Context.getBean(Context.Name.TRANSACTION_MANAGER);
        DefaultTransactionDefinition transactionDefinition =
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
        transactionDefinition.setName("1-processAndUpdateInvoice1-outer");
        TransactionStatus transaction = transactionManager.getTransaction(transactionDefinition);
        logger.debug("Transaction after: {}", TransactionInfoUtil.getTransactionStatus(true));

        try {
        	//for refunds, invoiceIds must be populated
        	if (dto.getIsRefund() == 1) {
        		PaymentBL linkedPayment = new PaymentBL(dto.getPayment().getId());

        		if (!linkedPayment.getEntity().getInvoicesMap().isEmpty()) {
                    String msg = getEnhancedLogMessage("The refunds linked payment has some paid Invoices.");
                    logger.debug(msg);
                    for (PaymentInvoiceMapDTO entry : linkedPayment.getEntity().getInvoicesMap()) {
                        dto.getPayment().getInvoiceIds().add(entry.getInvoiceEntity().getId());
                    }
        		}
        	}

            if (dto.getIsRefund() == 0 && invoiceId != null) {
                InvoiceBL bl = new InvoiceBL(invoiceId);
                List<Integer> inv = new ArrayList<Integer>();
                inv.add(invoiceId);
                dto.setInvoiceIds(inv);
                transactionManager.commit(transaction);
                return processAndUpdateInvoice(dto, bl.getEntity().getId(), executorUserId);

            } else {
            	
            	if (dto.getIsRefund() == 1 && dto.getPayment() != null ) {
            	    /* && !dto.getPayment().getInvoiceIds().isEmpty()){*/
                    /*InvoiceBL bl = new InvoiceBL((Integer) dto.getPayment().getInvoiceIds().get(0));
                    return processAndUpdateInvoice(dto, bl.getEntity(), executorUserId);*/
                    logger.debug("We changed the rules, you can't refund the Payment amount linked to Invoice. So no need to involve invoices in Refunds.");
                } 
                // without an invoice, it's just creating the payment row
                // and calling the processor
                logger.info("The payment may be a refund and its linked payment has no invoices connected to it");
                PaymentBL bl = new PaymentBL();
                Integer result = bl.processPayment(entityId, dto, executorUserId);

                //try to commit transaction with retry
                Exception exception = null;
                int numAttempts = 0;
                do {
                    numAttempts++;
                    TransactionStatus innerTransaction = transactionManager.getTransaction(
                            new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));
                    try {
                        if (result != null) {
                            bl.getEntity().setPaymentResult(new PaymentResultDAS().find(result));
                        }

                        if (result != null && result.equals(Constants.RESULT_OK)) {

                            logger.debug("A successful Refund. But original payment not linked to Invoices.");
                            //Therefore, will not be auto-applied to other Invoices 
                            //The refund then in-fact must reduce the Payment Balance it is linked to.
                            if (dto.getIsRefund() == 1) {
                                logger.debug("Linked payment balance after refund application {}", bl.getEntity().getPayment().getBalance());
                            } else {
                                // if the configured, pay any unpaid invoices
                                ConfigurationBL config = new ConfigurationBL(entityId);
                                if (config.getEntity().getAutoPaymentApplication() == 1) {
                                    bl.automaticPaymentApplication();
                                }
                            }
                        }
                        transactionManager.commit(innerTransaction);
                        logger.debug("Attempting to commit transaction.");
                        transactionManager.commit(transaction);
                        logger.debug("Transaction commited.");
                        return result;
                    } catch (Exception ex) {
                        if(!innerTransaction.isCompleted()) {
                            logger.debug("Transaction not completed, initiate rollback");
                            transactionManager.rollback(innerTransaction);
                        }
                        exception = ex;
                        logger.error("Could not commit transaction.", ex);
                        //wait 100 milliseconds
                        Thread.sleep(100);
                    }
                }
                while (numAttempts <= 10); //retry 10 times
                throw exception;
            }
        } catch (Exception e) {
            logger.error("An exception occurred. Payment amount {} is failed for user id {}", dto.getAmount(), dto.getUserId(), e);
            if (!transaction.isCompleted()) {
                logger.debug("Transaction not completed, initiate rollback");
                transactionManager.rollback(transaction);
            }
            throw new SessionInternalError(e);
        }
    }
    
    /**
     * This is called from the client to apply an existing payment to
     * an invoice.
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void applyPayment(Integer paymentId, Integer invoiceId) {

        String msg = String.format("Applying payment %s to invoice %s", paymentId, invoiceId);
        String message = new LogMessage.Builder()
                .module(LogConstants.MODULE_PAYMENT.toString()).action(LogConstants.ACTION_APPLY.toString())
                .message(msg).status(LogConstants.STATUS_SUCCESS.toString()).build().toString();
        logger.debug(message);

        if (paymentId == null || invoiceId == null) {
            logger.warn("Got null parameters to apply a payment");
            return;
        }
        try {
            PaymentBL payment = new PaymentBL(paymentId);
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            
            BigDecimal paid = applyPayment(payment.getDTO(), invoice.getEntity(), true);
            // link it with the invoice
            payment.createMap(invoice.getEntity(), paid);
            msg = String.format("Payment %s is applied to Invoice %s", paymentId, invoiceId);
            message=new LogMessage.Builder()
                    .module(LogConstants.MODULE_PAYMENT.toString()).action(LogConstants.ACTION_APPLY.toString())
                    .message(msg).status(LogConstants.STATUS_SUCCESS.toString()).build().toString();
            logger.info(message);
        } catch (Exception e) {
            msg = e.getMessage();
            message = new LogMessage.Builder()
                    .module(LogConstants.MODULE_PAYMENT.toString()).action(LogConstants.ACTION_APPLY.toString())
                    .message(msg).status(LogConstants.STATUS_NOT_SUCCESS.toString()).build().toString();
            logger.error(message);
            throw new SessionInternalError(e);
        }

    }
    private String getEnhancedLogMessage(String msg, LogConstants action,LogConstants status){
        return new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString()).action(action.toString())
                .message(msg).status(status.toString()).build().toString();
    }
    /**
     * Applys a payment to an invoice, updating the invoices fields with
     * this payment.
     * @param payment
     * @param invoice
     * @param success
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public BigDecimal applyPayment(PaymentDTO payment, InvoiceDTO invoice, boolean success) throws SQLException {

        BigDecimal totalPaid = BigDecimal.ZERO;
        if (invoice != null) {
            // set the attempt of the invoice
            String msg = String.format("Applying payment with ID: %d, to invoice with ID: %s", payment.getId(), invoice.getId());
            String message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
            logger.info(message);
            if (payment.getIsRefund() == 0) {
                //invoice can't take nulls. Default to 1 if so.
                invoice.setPaymentAttempts(payment.getAttempt() == null ? new Integer(1) : payment.getAttempt());
            }

            PaymentBL paymentBL = new PaymentBL(payment.getId());
            if (success) {
                // update the invoice's balance if applicable
                BigDecimal balance = invoice.getBalance();
                // get current invoice balance
                msg = String.format("Current invoice balance is %s", balance);
                message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                logger.debug(message);

                if (balance != null) {
                    boolean balanceSign = balance.compareTo(BigDecimal.ZERO) >= 0;
                    msg = String.format("balance sign is %s", balanceSign);
                    message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                    logger.debug(message);

                    BigDecimal newBalance = null;
                    if (payment.getIsRefund() == 0) {
                        msg = String.format("Payment with ID: %d is a normal payment", payment.getId());
                        message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                        logger.debug(message);

                        newBalance = balance.subtract(payment.getBalance());
                        msg = String.format("new balance is %s",newBalance);
                        message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                        logger.debug(message);

                        // I need the payment record to update its balance
                        if (payment.getId() == 0) {
                            throw new SessionInternalError("The ID of the payment to has to be present in the DTO");
                        }

                        BigDecimal paymentBalance = payment.getBalance().subtract(balance);
                        msg = String.format("payment balance is %s", paymentBalance);
                        message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                        logger.debug(message);
                        // payment balance cannot be negative, must be at least zero
                        if (BigDecimal.ZERO.compareTo(paymentBalance) > 0) {
                            msg = String.format("setting the paymentBalance which was %s to ZERO", paymentBalance);
                            message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                            logger.debug(message);
                            paymentBalance = BigDecimal.ZERO;
                        }

                        totalPaid = payment.getBalance().subtract(paymentBalance);

                        paymentBL.getEntity().setBalance(paymentBalance);
                        payment.setBalance(paymentBalance);
                        
                        // only level the balance if the original balance wasn't negative
                        if (newBalance.compareTo(Constants.BIGDECIMAL_ONE_CENT) < 0 && balanceSign) {
                            msg = String.format("new balance is %s and BIGDECIMAL_ONE_CENT is %s and balance sign is true",
                                                newBalance,
                                                Constants.BIGDECIMAL_ONE_CENT);

                            message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                            logger.debug(message);

                            message = getEnhancedLogMessage("setting the new balance to ZERO",LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                            logger.debug(message);

                            // the payment balance was greater than the invoice's
                            newBalance = BigDecimal.ZERO;
                        }
                        
                        invoice.setBalance(newBalance);
                        msg = String.format("Set invoice balance to: %s", invoice.getBalance());
                        message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                        logger.debug(message);
                                            
                        if (BigDecimal.ZERO.compareTo(newBalance) == 0) {
                            // update the to_process flag if the balance is 0
                            invoice.setToProcess(0);
                        } else {
                            // a refund might make this invoice payabale again
                            invoice.setToProcess(1);
                        }

                    } else { // refunds add to the invoice
                        msg = "Refunds do not add to Invoice anymore. You cannot refund a Payment that is linked to an Invoice. First un-link it.";
                        message = getEnhancedLogMessage(msg,LogConstants.ACTION_APPLY,LogConstants.STATUS_NOT_SUCCESS);
                        logger.debug(message);
                    }
                        
                } else {
                    // with no balance, we assume the the invoice got all paid
                    message = getEnhancedLogMessage("The balance of the invoice is null",
                                                    LogConstants.ACTION_APPLY,LogConstants.STATUS_SUCCESS);
                    logger.debug(message);
                    invoice.setToProcess(0);
                }

                // if the user is in the ageing process, she should be out
                if (invoice.getToProcess() == 0 || !UserBL.isUserBalanceEnoughToAge(invoice.getBaseUser())) {
                    new AgeingBL().out(invoice.getBaseUser(), invoice.getId(), Util.truncateDate(payment.getPaymentDate()));
                }
            } else {
                if (payment.getIsRefund() == 0) {
                    paymentBL.getEntity().setBalance(BigDecimal.ZERO);
                    payment.setBalance(BigDecimal.ZERO);
                }
            }
        }

        if (!totalPaid.equals(BigDecimal.ZERO)) {
            //Fire the event if the payment actually pays something.
            Integer entityId;
            if(payment.getBaseUser() == null){
                PaymentDTOEx paymentDTOEx = (PaymentDTOEx) payment;
                entityId = new UserDAS().find(paymentDTOEx.getUserId()).getEntity().getId();
            }else {
                entityId = payment.getBaseUser().getEntity().getId();
            }

            PaymentLinkedToInvoiceEvent event = new PaymentLinkedToInvoiceEvent(entityId,new PaymentDTOEx(payment),invoice,totalPaid);
            EventManager.process(event);
        }
        return totalPaid;
    }

    /**
     * This method is called from the client, when a payment needs only to 
     * be applyed without realtime authorization by a processor
     * Finds this invoice entity, creates the payment record and calls the 
     * apply payment  
     * Id does suport invoiceId = null because it is possible to get a payment
     * that is not paying a specific invoice, a deposit for prepaid models.
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public Integer applyPayment(PaymentDTOEx payment, Integer invoiceId, Integer executorUserId)  
            throws SessionInternalError {
        String msg ;
        String logMsg;
        logger.info(getEnhancedLogMessage("Applying payment", LogConstants.ACTION_APPLY, LogConstants.STATUS_SUCCESS));
        try {
            PaymentBL paymentBl = new PaymentBL();
            // create the payment record
            PaymentInformationBL piBl = new PaymentInformationBL();
            
            if(payment.getPaymentInstruments().size() > 0) {
            	payment.setInstrument(payment.getPaymentInstruments().iterator().next());
            	// set the payment method
            	payment.getInstrument().setPaymentMethod(
                        new PaymentMethodDAS().find(piBl.getPaymentMethodForPaymentMethodType(payment.getInstrument())));
            }
            
            // set the attempt to an initial value, if the invoice is there,
            // it's going to be updated
            payment.setAttempt(1);
            if (payment.getPaymentResult() == null) {
                payment.setPaymentResult(new PaymentResultDAS().find(Constants.RESULT_ENTERED));
            }
            payment.setBalance(payment.getAmount());
            paymentBl.create(payment, executorUserId);
            // this is necessary for the caller to get the Id of the
            // payment just created
            payment.setId(paymentBl.getEntity().getId());
            
            boolean wasPaymentApplied= false;
            if (payment.getIsRefund() == 0) { // normal payment
                if (invoiceId != null) {
                    // find the invoice
                    InvoiceBL invoiceBl = new InvoiceBL(invoiceId);
                    // set the attempts from the invoice
                    payment.setAttempt(invoiceBl.getEntity().getPaymentAttempts() + 1);
                    // apply the payment to the invoice
                    BigDecimal paid = applyPayment(payment, invoiceBl.getEntity(), true);
                    // link it with the invoice
                    paymentBl.createMap(invoiceBl.getEntity(), paid);
                    
                    //payment was applied successfully
                    wasPaymentApplied = true;
                } else {
                    // this payment was done without an explicit invoice
                    // We'll try to link it to invoices with balances then provided automatic payment is set
                    msg = "Trying to link the payment to invoices with automatic payment.";
                    logMsg = getEnhancedLogMessage(msg);
                    logger.debug(logMsg);
                	Integer userId= payment.getUserId();
                	UserDTO userDTO= new UserDAS().find(userId);
                    if (null != userDTO) {
	                	ConfigurationBL config = new ConfigurationBL(userDTO.getCompany().getId());
	                    if (config.getEntity().getAutoPaymentApplication() == 1) {
	                        wasPaymentApplied = paymentBl.automaticPaymentApplication();
	                    }
                	}
                }
                // let know about this payment with an event
                PaymentSuccessfulEvent event = new PaymentSuccessfulEvent(
                        paymentBl.getEntity().getBaseUser().getEntity().getId(),payment);
                EventManager.process(event);
            } else {
                msg = String.format("Payment with ID: %d is linked to payment with ID: %s and may be linked with invoice",
                                                                            payment.getId(), payment.getPayment().getId());
                logMsg = getEnhancedLogMessage(msg);
                logger.info(logMsg);
                
                // fetch the linked payment from database
                PaymentDTO linkedPayment = new PaymentBL(payment.getPayment().getId()).getEntity();
                
                if ( null != linkedPayment ) {
	                /*
	                 * Since payment is not linked to any invoice now, 
	                 * we must subtract the payment balance with that of 
	                 * the refund payment value only when result is OK or ENTERED.
                	*/
                    int resultId = payment.getPaymentResult().getId();
                    if (resultId == Constants.RESULT_OK.intValue() || resultId == Constants.RESULT_ENTERED.intValue()) {
	                    linkedPayment.setBalance(linkedPayment.getBalance().subtract(payment.getAmount()));
	                    wasPaymentApplied= true;
                    }
                }
                else {
                    msg = "This refund is not linked with any payment which is wrong";
                    logMsg = getEnhancedLogMessage(msg);
                    logger.error(logMsg);
                    //maybe throw exception
                }
            }
            
            //should we notify the customer of this payment
            if (wasPaymentApplied) {
                msg = "Payment with ID: " + payment.getId() + " is applied.";
                logMsg = getEnhancedLogMessage(msg);
                logger.info(logMsg);
                //this notification prevents multiple notifications sent for each application of the payment to an Invoice
                msg = "Invoking Payment notification for the Payment Entered since it was applied to at least 1 Invoice.";
                logMsg = getEnhancedLogMessage(msg);
                logger.debug(logMsg);
            }
            if (payment.isSendNotification()) {
                if (payment.getInvoiceIds().isEmpty() && invoiceId != null) {
                    paymentBl.sendNotification(payment, new UserDAS().find(payment.getUserId()).getCompany().getId(),
                            payment.getPaymentResult() != null ? payment.getPaymentResult().getId() : Constants.RESULT_OK, invoiceId);
                } else {
                    paymentBl.sendNotification(payment, new UserDAS().find(payment.getUserId()).getCompany().getId(),
                            payment.getPaymentResult() != null ? payment.getPaymentResult().getId() : Constants.RESULT_OK);
                }
            }
            msg = "Payment: " + payment.getId() + " was created.";
            logMsg = getEnhancedLogMessage(msg, LogConstants.ACTION_APPLY, LogConstants.STATUS_SUCCESS);
            logger.info(logMsg);
            return paymentBl.getEntity().getId();
        } catch (Exception e) {
            msg = e.getMessage();
            logMsg = getEnhancedLogMessage(msg);
            logger.error(logMsg);
            throw new SessionInternalError(e);
        }
    }
    private String getEnhancedLogMessage(String msg){
    return new LogMessage.Builder().module(LogConstants.MODULE_PAYMENT.toString())
            .action(LogConstants.ACTION_EVENT.toString()).message(msg).build().toString();
    }

    @Transactional( propagation = Propagation.REQUIRED )
    public PaymentDTOEx getPayment(Integer id, Integer languageId) 
            throws SessionInternalError {
        try {
            PaymentBL bl = new PaymentBL(id);
            return bl.getDTOEx(languageId);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Transactional( propagation = Propagation.REQUIRES_NEW )
    public Boolean processPaypalPayment(Integer invoiceId, String entityEmail,
            BigDecimal amount, String currency, Integer paramUserId, String userEmail) 
            throws SessionInternalError {
        
        if (userEmail == null && invoiceId == null && paramUserId == null) {
            logger.debug("Too much null, returned");
            return false;
        }
        try {
            boolean ret = false;
            InvoiceBL invoice;
            Integer entityId;
            Integer userId;
            CurrencyBL curr;
            if (invoiceId != null) {
                invoice = new InvoiceBL(invoiceId);
                entityId = invoice.getEntity().getBaseUser().getEntity().getId();
                userId = invoice.getEntity().getBaseUser().getUserId();
                curr = new CurrencyBL(
                        invoice.getEntity().getCurrency().getId());
            } else {
                UserBL user = new UserBL();
                // identify the user some other way
                if (paramUserId != null) {
                    // easy
                    userId = paramUserId;
                } else {
                    // find a user by the email address
                    userId = user.getByEmail(userEmail);
                    if (userId == null) {
                        logger.debug("Could not find a user for email {}", userEmail);
                        return false;
                    }
                }
                user = new UserBL(userId);
                entityId = user.getEntityId(userId);
                curr = new CurrencyBL(user.getCurrencyId());
            }
            
            // validate the entity
            String paypalAccount = PreferenceBL.getPreferenceValue(entityId, Constants.PREFERENCE_PAYPAL_ACCOUNT);
            if (paypalAccount != null && paypalAccount.equals(entityEmail)) {
                // now the currency
                if (curr.getEntity().getCode().equals(currency)) {
                    // all good, make the payment
                    PaymentDTOEx payment = new PaymentDTOEx();
                    payment.setAmount(amount);
                    payment.setPaymentMethod(new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_PAYPAL));
                    payment.setUserId(userId);
                    payment.setCurrency(curr.getEntity());
                    payment.setCreateDatetime(Calendar.getInstance().getTime());
                    payment.setPaymentDate(Calendar.getInstance().getTime());
                    payment.setIsRefund(0);
                    applyPayment(payment, invoiceId, null);
                    ret = true;
                    
                    // notify the customer that the payment was received
                    NotificationBL notif = new NotificationBL();
                    MessageDTO message = notif.getPaymentMessage(entityId, payment, payment.getPaymentResult() != null ? payment.getPaymentResult().getId() : Constants.RESULT_OK);
                    INotificationSessionBean notificationSess = Context.getBean(Context.Name.NOTIFICATION_SESSION);
                    notificationSess.notify(payment.getUserId(), message);
                    
                    // link to unpaid invoices
                    // TODO avoid sending two emails
                    PaymentBL bl = new PaymentBL(payment);
                    bl.automaticPaymentApplication();

                } else {
                    logger.debug("wrong currency {}", currency);
                }
            } else {
                logger.debug("wrong entity paypal account {} {}", paypalAccount, entityEmail);
            }
            
            return ret;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    public void doPaymentRetry(Integer userId, List<InvoiceDTO> overdueInvoices)
            throws SessionInternalError {
        try {
            UserDAS userDas = new UserDAS();
            UserDTO user = userDas.findNow(userId);
            userDas.refresh(user);
            UserStatusDTO status = user.getUserStatus();
            if (status == null) {
                return;
            }

            AgeingEntityStepDTO nextStep = status.getAgeingEntityStep();
            // preform payment retry
            if (nextStep != null && nextStep.getRetryPayment() == 1) {
                String msg = "Retrying payment for user " + userId + " based on the user status";
                String logMsg = getEnhancedLogMessage(msg);
                logger.debug(logMsg);
                // post the need of a payment for all unpaid invoices for this user
                for (InvoiceDTO invoice : overdueInvoices) {
                    ProcessPaymentEvent event = new ProcessPaymentEvent(invoice.getId(),
                            null, null, user.getEntity().getId());
                    EventManager.process(event);
                }
            }

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }
    
    /** 
     * Clients with the right privileges can update payments with result
     * 'entered' that are not linked to an invoice
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void update(Integer executorId, PaymentDTOEx dto) 
            throws SessionInternalError, EmptyResultDataAccessException {
        if (dto.getId() == 0) {
            throw new SessionInternalError("ID missing in payment to update");
        }
        String msg = String.format("Updating payment with ID: %s", dto.getId());
        String message = getEnhancedLogMessage(msg,LogConstants.ACTION_UPDATE,LogConstants.STATUS_SUCCESS);
        logger.info(message);
        PaymentBL bl = new PaymentBL(dto.getId());
        if (new Integer(bl.getEntity().getPaymentResult().getId()).equals(Constants.RESULT_ENTERED)) {
        } else {
            throw new SessionInternalError("Payment update only available" +" for entered payments");
        }
            
        bl.update(executorId, dto);
    }
    
    /** 
     * Removes a payment-invoice link
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public void removeInvoiceLink(Integer mapId) {
        PaymentBL payment = new PaymentBL();
        payment.removeInvoiceLink(mapId);
    }

    /** 
     * Processes the blacklist CSV file specified by filePath.
     * It will either add to or replace the existing uploaded 
     * blacklist for the given entity (company). Returns the number
     * of new blacklist entries created.
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public int processCsvBlacklist(String filePath, boolean replace, 
            Integer entityId) throws CsvProcessor.ParseException {
        CsvProcessor processor = new CsvProcessor();
        return processor.process(filePath, replace, entityId);
    }

    /**
     * Saves legacy payment information on jBilling related tables.
     *
     * @param paymentDTOEx The instance of payment information.
     * @throws SessionInternalError
     */
    @Transactional( propagation = Propagation.REQUIRED )
    public Integer saveLegacyPayment(PaymentDTOEx paymentDTOEx) throws SessionInternalError {
        PaymentBL paymentBL = new PaymentBL();
        paymentBL.create(paymentDTOEx, null);
	    return paymentBL.getDTO().getId();
    }

    /**
     * Finds last Payment Id of particular user with given result id
     */
    public Integer getLatestPayment(Integer userId, Integer resultId) throws SessionInternalError {
        return new PaymentBL().getLatestId(userId, resultId);
    }

    @Transactional( propagation = Propagation.REQUIRED )
    public void updatePaymentResult(Integer paymentId, Integer resultId)
            throws SessionInternalError {

        PaymentBL paymentBL = new PaymentBL(paymentId);
        PaymentDTO paymentDTO = paymentBL.getEntity();
        logger.debug("Updating Status of payment {} from {} to {}", paymentId, paymentDTO.getPaymentResult().getId(), resultId);
        paymentBL.updatePaymentResult(resultId);
    }
}
