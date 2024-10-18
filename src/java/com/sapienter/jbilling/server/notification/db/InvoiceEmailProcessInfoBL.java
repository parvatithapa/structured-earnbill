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

import java.lang.invoke.MethodHandles;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.list.ResultList;
import com.sapienter.jbilling.server.process.ProcessSQL;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;


public class InvoiceEmailProcessInfoBL extends ResultList implements ProcessSQL {

    private InvoiceEmailProcessInfoDAS invoiceEmailProcessInfoDAS = null;
    private InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO = null;

    public InvoiceEmailProcessInfoBL(Integer id) {
        init();
        set(id);
    }

    public InvoiceEmailProcessInfoBL() {
        init();
    }

    public InvoiceEmailProcessInfoBL(InvoiceEmailProcessInfoDTO row) {
        init();
        invoiceEmailProcessInfoDTO = row;
    }

    private void init() {
        invoiceEmailProcessInfoDAS = new InvoiceEmailProcessInfoDAS();
    }

    public InvoiceEmailProcessInfoDTO getEntity() {
        return invoiceEmailProcessInfoDTO;
    }

    public InvoiceEmailProcessInfoDAS getHome() {
        return invoiceEmailProcessInfoDAS;
    }

    public void set(Integer id) {
        invoiceEmailProcessInfoDTO = invoiceEmailProcessInfoDAS.findNow(id);
    }

    public void set(InvoiceEmailProcessInfoDTO pEntity) {
        invoiceEmailProcessInfoDTO = pEntity;
    }
    
    public InvoiceEmailProcessInfoDTO saveInvoiceEmailProcessInfo(InvoiceEmailProcessInfoDTO invoiceEmailProcessInfoDTO) {
        return invoiceEmailProcessInfoDAS.save(invoiceEmailProcessInfoDTO);
    }
    
    public InvoiceEmailProcessInfoDTO create(BillingProcessDTO billingProcessDTO, Integer jobExecutionId, 
            Integer emailsEstimated, Integer emailsSent, Integer emailsFailed, Date startDateTime, Date endDateTime, String source) {
        InvoiceEmailProcessInfoDTO dto = new InvoiceEmailProcessInfoDTO(billingProcessDTO, jobExecutionId, 
                emailsEstimated, emailsSent, emailsFailed, startDateTime, endDateTime, source);
        return saveInvoiceEmailProcessInfo(dto);
    }
    
    public InvoiceEmailProcessInfoDTO update(Integer id, BillingProcessDTO billingProcessDTO, Integer jobExecutionId, 
            Integer emailsEstimated, Integer emailsSent, Integer emailsFailed, Date startDateTime, Date endDateTime, String source) {
        InvoiceEmailProcessInfoDTO dto = new InvoiceEmailProcessInfoDTO(id,billingProcessDTO, jobExecutionId, 
                emailsEstimated, emailsSent, emailsFailed, startDateTime, endDateTime, source);
        return saveInvoiceEmailProcessInfo(dto);
    }


}
