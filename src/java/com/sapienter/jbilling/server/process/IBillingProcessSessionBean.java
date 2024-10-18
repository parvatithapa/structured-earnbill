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

package com.sapienter.jbilling.server.process;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.payment.db.PaymentProcessRunDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessConfigurationDTO;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;

/**
 *
 * This is the session facade for the all the billing process and its 
 * related services. 
 */
public interface IBillingProcessSessionBean {

    /**
     * Gets the invoices for the specified process id. The returned collection
     * is of extended dtos (InvoiceDTO).
     * @param processId
     * @return A collection of InvoiceDTO objects
     * @throws SessionInternalError
     */
    public Collection getGeneratedInvoices(Integer processId);
    
    /**
     * @param entityId
     * @param languageId
     * @param collectionType
     * @return
     * @throws SessionInternalError
     */
    public AgeingDTOEx[] getAgeingSteps(Integer entityId, 
            Integer executorLanguageId, Integer languageId, CollectionType collectionType);
    
    /**
     * @param entityId
     * @param languageId
     * @param steps
     * @param collectionType
     * @throws SessionInternalError
     */
    public void setAgeingSteps(Integer entityId, Integer languageId, 
            AgeingDTOEx[] steps, CollectionType collectionType);

    public void generateReview(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue);

    /**
     * Creates the billing process record. This has to be done in its own
     * transaction (thus, in its own method), so new invoices can link to
     * an existing process record in the db.
     */
    public BillingProcessDTO createProcessRecord(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue, boolean isReview,
            Integer retries);

    public Integer createRetryRun(Integer processId);
    
    public void processEntity(Integer entityId, Date billingDate,
            Integer periodType, Integer periodValue, boolean isReview);

    /**
     * This method process a payment synchronously. It is a wrapper to the payment processing  
     * so it runs in its own transaction
     */
    public void processPayment(Integer processId, Integer runId, Integer invoiceId);

    /**
     * This method marks the end of payment processing. It is a wrapper
     * so it runs in its own transaction
     */
    public void endPayments(Integer runId);

    public boolean verifyIsRetry(Integer processId, int retryDays, Date today);

    public void email(Integer entityId, Integer invoiceId, Integer processId); 

    /**
     * Process a user, generating the invoice/s,
     * @param billingDate 
     * @param userId
     */
    public Integer[] processUser(Integer processId, Date billingDate, Integer userId,
            boolean isReview, boolean onlyRecurring);

    public BillingProcessDTOEx getDto(Integer processId, Integer languageId);

    public BillingProcessConfigurationDTO getConfigurationDto(Integer entityId);

    public Integer createUpdateConfiguration(Integer executorId, BillingProcessConfigurationDTO dto);

    public Integer getLast(Integer entityId);

    public BillingProcessDTOEx getReviewDto(Integer entityId, Integer languageId);

    public BillingProcessConfigurationDTO setReviewApproval(Integer executorId, Integer entityId, Boolean flag);

    public boolean trigger(Date pToday, Integer entityId);
    public boolean triggerAsync (Date pToday, Integer entityId);

    /**
     * @return the id of the invoice generated
     */
    public InvoiceDTO generateInvoice(Integer orderId, Integer invoiceId, Integer languageId, Integer executorUserId);

    public void reviewUsersStatus(Integer entityId, Date today);

    /**
     *  Reviews user status, determines if ageing is needed
     *
     * @param entityId
     * @param userId
     * @param today
     */
    public List<InvoiceDTO> reviewUserStatus(Integer entityId, Integer userId, Date today);

    /**
     * Update status of BillingProcessRun in new transaction
     * for accessing from other thread
     * @param billingProcessId id of billing process for searching ProcessRun
     * @param processRunStatusId id of finished process run status (success or failure)
     * @return id of updated ProcessRunDTO
     */
    public Integer updateProcessRunFinished(Integer billingProcessId, Integer processRunStatusId);

    /**
     * Adds ProcessRunUser in new transaction
     * for accessing from other thread
     * @param billingProcessId id of billing process for searching ProcessRun
     * @param userId ID of user
     * @param status Status of billing process for specified user: 0 - failed, 1 - succeeded
     * @return id of inserted ProcessRunUserDTO
     */
    public Integer addProcessRunUser(Integer billingProcessId, Integer userId, Integer status);

    /**
     * Returns true if the Billing Process is currently running.
     * @param entityId
     * @return
     */
    public boolean isBillingRunning(Integer entityId) ;

    /**
     * Returns status of last billing process for the entity specified
     * @param entityId entity for status retrieve
     * @return ProcessStatusWS with current state of execution
     */
    public ProcessStatusWS getBillingProcessStatus(Integer entityId);

    /**
     * Returns true if the Ageing Process is currently running.
     * @return
     */
    public boolean isAgeingProcessRunning(Integer entityId);

    /**
     * Returns status of last ageing process for the entity specified
     * @param entityId entity for status retrieve
     * @return ProcessStatusWS with current state of execution
     */
    public ProcessStatusWS getAgeingProcessStatus(Integer entityId);
    
    /**
     * Finds all user ids of those customers who have credit card or ACH and have pending invoices
     * @param entityId
     * @return
     */
    public Set<Integer> findInvoicesForAutoPayments(Integer entityId);
    
    /**
     * Finds payment process for particular billing process
     * @param billingProcessId
     * @return
     */
    public PaymentProcessRunDTO findPaymentProcessRun(Integer billingProcessId);
    
    public PaymentProcessRunDTO saveOrUpdatePaymentProcessRun(PaymentProcessRunDTO paymentProcessRunDTO);
    
    public boolean isPaymentProcessRunning(Integer entityId);
    public BillingProcessDTOEx getSimpleDto(Integer processId);

	public void linkInvoicesToBillingProcess(Integer entityId,Date linkingDate);
	
	public List<Integer> getInvoiceIdsByBillingProcessAndByUser(Integer processId, Integer userId);

	public Integer getInvoiceCountByBillingProcessId(Integer processId);

    public List<Integer> getPagedGeneratedInvoices(Integer processId, Integer limit, Integer offset);

    public List<Integer> findAllInvoiceIdsForByProcessId(Integer processId);

    public List<Integer> findAllInvoiceIdsForByProcessIdAndInvoiceDesign(Integer billingProcessId, String invoiceDesign);
}