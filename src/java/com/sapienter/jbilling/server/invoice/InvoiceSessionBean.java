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
import java.util.ResourceBundle;
import java.util.SortedSet;

import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.order.db.OrderProcessDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.pluggableTask.PaperInvoiceNotificationTask;
import com.sapienter.jbilling.server.process.BillingProcessBL;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 *
 * This is the session facade for the invoices in general. It is a statless
 * bean that provides services not directly linked to a particular operation
 *
 * @author emilc
 **/
@Transactional( propagation = Propagation.REQUIRED )
public class InvoiceSessionBean implements IInvoiceSessionBean {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(
            InvoiceSessionBean.class));

    @Override
    public InvoiceDTO getInvoice(Integer invoiceId){
        InvoiceDTO dto =  new InvoiceDAS().findNow(invoiceId);
        if (dto != null)
        {
            dto.getBalance(); // touch
        }
        return dto;
    }

    @Override
    public InvoiceDTO create(Integer entityId, Integer userId, NewInvoiceContext newInvoice) {
        try {
            InvoiceBL invoice = new InvoiceBL();
            UserBL user = new UserBL();
            if (user.getEntityId(userId).equals(entityId) || user.getParentId(entityId).equals(user.getEntityId(userId))) {
                invoice.create(userId, newInvoice, null, null);
                invoice.createLines(newInvoice);
                return invoice.getDTO();
            } else {
                throw new SessionInternalError("User " + userId + " doesn't " +
                        "belong to entity " + entityId);
            }
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public String getFileName(Integer invoiceId) {
        try {
            InvoiceBL invoice = new InvoiceBL(invoiceId);
            UserBL user = new UserBL(invoice.getEntity().getBaseUser());
            ResourceBundle bundle = ResourceBundle.getBundle(
                    "entityNotifications", user.getLocale());
            String ret = bundle.getString("invoice.file.name") + '-' +
                    invoice.getEntity().getPublicNumber().replaceAll(
                            "[\\\\~!@#\\$%\\^&\\*\\(\\)\\+`=\\]\\[';/\\.,<>\\?:\"{}\\|]", "_");
            LOG.debug("name = %s", ret);
            return ret;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    /**
     * The transaction requirements of this are not big. The 'atom' is
     * just a single invoice. If the next one fails, it's ok that the
     * previous ones got updated. In fact, they should, since the email
     * has been sent.
     */
    @Override
    public void sendReminders(Date today){
        try {
            InvoiceBL invoice = new InvoiceBL();
            invoice.sendReminders(today);
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public InvoiceDTO getInvoiceEx(Integer invoiceId, Integer languageId)  {
        if (invoiceId == null) {
            return null;
        }
        InvoiceBL invoice = new InvoiceBL(invoiceId);
        InvoiceDTO ret = invoice.getDTOEx(languageId, true);
        for (PaymentInvoiceMapDTO map : ret.getPaymentMap()) {
            map.getPayment().getCreateDatetime(); // thouch
        }
        for (OrderProcessDTO process : ret.getOrderProcesses()) {
            process.getPurchaseOrder().getCreateDate(); // thouch
        }
        return ret;
    }

    @Override
    public byte[] getPDFInvoice(Integer invoiceId) {
        try {
            if (invoiceId == null) {
                return new byte[0];
            }
            NotificationBL notificationBL = new NotificationBL();
            InvoiceBL invoiceBL = new InvoiceBL(invoiceId);
            UserDTO user = invoiceBL.getEntity().getBaseUser();
            Integer entityId = user.getEntity().getId();
            // the language doesn't matter when getting a paper invoice
            MessageDTO message = notificationBL.getInvoicePaperMessage(entityId, null, user.getLanguageIdField(), invoiceBL.getEntity());
            PaperInvoiceNotificationTask paperInvoiceNotificationTask = NotificationBL.loadPaperInvoiceNotificationTaskForEntity(entityId);
            return paperInvoiceNotificationTask.getPDF(user, message);
        } catch(SessionInternalError error) {
            throw error;
        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public void delete(Integer invoiceId, Integer executorId) {
        InvoiceBL invoice = new InvoiceBL(invoiceId);
        invoice.delete(executorId);
    }

    /**
     * The real path is known only to the web server
     * It should have the token _FILE_NAME_ to be replaced by the generated file
     */
    @Override
    public String generatePDFFile(Map<Object, Object> map, String realPath) {
        Integer operationType = (Integer) map.get("operationType");

        try {
            InvoiceBL invoiceBL = new InvoiceBL();
            javax.sql.rowset.CachedRowSet cachedRowSet = null;
            Integer entityId = (Integer) map.get("entityId");

            if (operationType.equals(com.sapienter.jbilling.common.Constants.OPERATION_TYPE_CUSTOMER)) {
                Integer customer = (Integer) map.get("customer");

                //security check is done here for speed
                UserBL customerUserBL = new UserBL(customer);

                if (customerUserBL.getEntity().getEntity().getId() == entityId) {
                    cachedRowSet = invoiceBL.getInvoicesByUserId(customer);
                }
            } else if (operationType.equals(com.sapienter.jbilling.common.Constants.OPERATION_TYPE_RANGE)) {
                //security check is done in SQL
                cachedRowSet = invoiceBL.getInvoicesByIdRange(
                        (Integer) map.get("from"),
                        (Integer) map.get("to"),
                        entityId);
            } else if (operationType.equals(com.sapienter.jbilling.common.Constants.OPERATION_TYPE_PROCESS)) {
                Integer process = (Integer) map.get("process");

                //security check is done here for speed
                BillingProcessBL billingProcessBL = new BillingProcessBL(process);;
                if (billingProcessBL.getEntity().getEntity().getId() == entityId.intValue()) {
                    cachedRowSet = invoiceBL.getInvoicesToPrintByProcessId(process);
                }
            } else if (operationType.equals(com.sapienter.jbilling.common.Constants.OPERATION_TYPE_DATE)) {
                Date from = (Date) map.get("date_from");
                Date to = (Date) map.get("date_to");

                cachedRowSet = invoiceBL.getInvoicesByCreateDate(entityId, from, to);
            } else if (operationType.equals(com.sapienter.jbilling.common.Constants.OPERATION_TYPE_NUMBER)) {
                String from = (String) map.get("number_from");
                String to = (String) map.get("number_to");
                Integer fromId = invoiceBL.convertNumberToID(entityId, from);
                Integer toId= invoiceBL.convertNumberToID(entityId, to);

                if (fromId != null && toId != null &&
                        fromId.compareTo(toId) <= 0) {
                    cachedRowSet = invoiceBL.getInvoicesByIdRange(
                            fromId, toId, entityId);
                }
            }

            if (cachedRowSet == null) {
                return null;
            } else {
                PaperInvoiceBatchBL paperInvoiceBatchBL = new PaperInvoiceBatchBL();
                return paperInvoiceBatchBL.generateFile(cachedRowSet, entityId, realPath);
            }

        } catch (Exception e) {
            throw new SessionInternalError(e);
        }
    }

    @Override
    public SortedSet<Integer> getAllInvoices(Integer userId) {
        return new InvoiceDAS().getAllInvoiceIdForUser(userId);
    }
}
