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


import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 * 
 * @author abhijeet.kore
 * 
 */
public class InvoiceNotificationMessageArchDAS extends
        AbstractDAS<InvoiceNotificationMessageArchDTO> {

    public InvoiceNotificationMessageArchDTO create(NotificationMessageArchDTO notificationMessageArchDTO, UserDTO userDTO, InvoiceDTO invoiceDTO, Integer jobExecutionId) {
        InvoiceNotificationMessageArchDTO inma = new InvoiceNotificationMessageArchDTO();
        inma.setNotificationMessageArch(notificationMessageArchDTO);
        inma.setBaseUser(userDTO);
        inma.setInvoice(invoiceDTO);
        inma.setJobExecutionId(jobExecutionId);
        save(inma);
        return inma;
    }
    
    public void deleteAllWithInvoice(InvoiceDTO invoice) {
        InvoiceDTO inv = new InvoiceDAS().find(invoice.getId());
        Criteria criteria = getSession().createCriteria(InvoiceNotificationMessageArchDTO.class);
        criteria.add(Restrictions.eq("invoice", inv));

        List<InvoiceNotificationMessageArchDTO> results = criteria.list();

        if (results != null && !results.isEmpty()) {
            for (InvoiceNotificationMessageArchDTO inma : results) {
                delete(inma);
            }
        }
    }
}
