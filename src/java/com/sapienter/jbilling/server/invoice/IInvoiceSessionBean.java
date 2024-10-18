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

package com.sapienter.jbilling.server.invoice;

import java.util.Date;
import java.util.Map;
import java.util.SortedSet;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;

/**
 *
 * This is the session facade for the invoices in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 **/
public interface IInvoiceSessionBean {

    public InvoiceDTO getInvoice(Integer invoiceId);

    public InvoiceDTO create(Integer entityId, Integer userId,
            NewInvoiceContext newInvoice);

    public String getFileName(Integer invoiceId);

    /**
     * The transaction requirements of this are not big. The 'atom' is 
     * just a single invoice. If the next one fails, it's ok that the
     * previous ones got updated. In fact, they should, since the email
     * has been sent.
     */
    public void sendReminders(Date today);

    public InvoiceDTO getInvoiceEx(Integer invoiceId, Integer languageId);

    public byte[] getPDFInvoice(Integer invoiceId);

    public void delete(Integer invoiceId, Integer executorId);

    /**
     * The real path is known only to the web server
     * It should have the token _FILE_NAME_ to be replaced by the generated file
     */
    public String generatePDFFile(Map<Object, Object> map, String realPath);

    public SortedSet<Integer> getAllInvoices(Integer userId);
}    
