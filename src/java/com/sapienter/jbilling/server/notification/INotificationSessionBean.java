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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.user.db.UserDTO;

import java.util.List;

public interface INotificationSessionBean {
    
    /**
     * Sends an email with the invoice to a customer.
     * This is used to manually send an email invoice from the GUI
     * @param invoiceId
     * @return
    */
    public Boolean emailInvoice(Integer invoiceId) throws SessionInternalError;
    
    /**
     * Sends an email with the invoice to a customer.
     * This is used to manually send an email invoice from the GUI
     * @param paymentId
     * @return
    */
    public Boolean emailPayment(Integer paymentId) throws SessionInternalError;

    public void notify(Integer userId, MessageDTO message) 
            throws SessionInternalError;

    public void asyncNotify(Integer userId, MessageDTO message)
            throws SessionInternalError;
    
   /**
    * Sends a notification to a user. Returns true if no exceptions were
    * thrown, otherwise false. This return value could be considered
    * as if this message was sent or not for most notifications (emails).
    */
    public Boolean notify(UserDTO user, MessageDTO message) 
            throws SessionInternalError;

    public MessageDTO getDTO(Integer typeId, Integer languageId,
            Integer entityId) throws SessionInternalError;

    public Integer createUpdate(MessageDTO dto, Integer entityId) 
            throws SessionInternalError;

    public String getEmails(Integer entityId, String separator) 
            throws SessionInternalError;

    public Boolean sendEmailNotification(List<Integer> absaFailedPayments, List<Integer> standardBankFailedPayments, String clientCode,
                                         Integer entityId, String transmissionDate, Integer fileSequenceNo)
            throws SessionInternalError;
}
