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
package com.sapienter.jbilling.server.notification.db;


import com.sapienter.jbilling.server.util.db.AbstractDAS;

import org.hibernate.Query;

/**
 * 
 * @author abhijeet.kore
 * 
 */
public class InvoiceEmailProcessInfoDAS extends
        AbstractDAS<InvoiceEmailProcessInfoDTO> {
    
    private static final String NL = System.getProperty("line.separator");
    
    public InvoiceEmailProcessInfoDTO create() {
        InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO = new InvoiceEmailProcessInfoDTO();        
        return save(invoiceEmailProcessInfoDTO);
    }

    public InvoiceEmailProcessInfoDTO create(InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO) {
        return save(invoiceEmailProcessInfoDTO);
    }
    
    
    public static final String GET_INVOICE_EMAIL_PROCESS_INFO_ID= String.join(NL,
            "SELECT inf.jobExecutionId FROM ",
            "InvoiceEmailProcessInfoDTO inf, InvoiceDTO i ",
            "WHERE i.id=:invoiceId AND inf.billingProcess.id=i.billingProcess.id ",
            "ORDER BY inf.startDatetime desc");
    
    public Integer getJobExecutionIdByInvoice(Integer invoiceId) {
        Query query = getSession().createQuery(GET_INVOICE_EMAIL_PROCESS_INFO_ID);
        query.setParameter("invoiceId", invoiceId);
        query.setFirstResult(0).setMaxResults(1);
        return (Integer) query.uniqueResult();
    }
}
