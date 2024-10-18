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
package com.sapienter.jbilling.server.payment.event;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;

import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.db.value.BooleanMetaFieldValue;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentBL;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.db.PaymentDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDAS;
import com.sapienter.jbilling.server.payment.db.PaymentInvoiceMapDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/*
 * The configuration needs to be done specifically for each installation/scenario
 * using the file resources.xml
 */
public class AutoPaymentMDB implements MessageListener {

    private final FormatLogger LOG = new FormatLogger(Logger.getLogger(AutoPaymentMDB.class));

    public void onMessage(Message message) {
        try {
            LOG.debug("Processing message. Processor " + message.getStringProperty("processor") + 
                    " entity " + message.getIntProperty("entityId") + " by " + this.hashCode());
            MapMessage myMessage = (MapMessage) message;
            if(myMessage.getJMSRedelivered()) {
                Integer userId = (myMessage.getInt("userId") == -1) ? null : myMessage.getInt("userId");
                LOG.debug("AutoPayment MDB re trying payment for user " +userId);
                return ;
            }

            String type = message.getStringProperty("type"); 
            if (type.equals("payment")) {
                LOG.debug("Now processing asynch payment:" +
                        " userId: " + myMessage.getInt("userId"));
                Integer userId = (myMessage.getInt("userId") == -1) ? null : myMessage.getInt("userId");

                processUser(userId, message.getIntProperty("entityId")); 

                LOG.debug("Done");
            } else {
                LOG.error("Can not process message of type " + type);
            }
        } catch (Exception e) {
            LOG.error("Generating payment", e);
        }
    }

    private void processUser(Integer userId, Integer entityId){
        LOG.debug("Processing user: "+userId);
        IWebServicesSessionBean sessionBean = (IWebServicesSessionBean) 
                Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        Integer[] invoiceIds = sessionBean.getUnpaidInvoicesOldestFirst(userId);
        if(invoiceIds.length > 0){
            LOG.debug("Processing invoices");
            IPaymentSessionBean paymentBean =  (IPaymentSessionBean) 
                    Context.getBean(Context.Name.PAYMENT_SESSION);
            UserDTO user = new UserDAS().find(userId);
            List<PaymentInformationWS> paymentInstruments = convertPaymentInformationDTOtoWS(user.getPaymentInstruments());
            BigDecimal totalBalance = new InvoiceBL().getTotalBalanceByInvoiceIds(invoiceIds);
            PaymentWS paymentWS = new PaymentWS();
            paymentWS.setAmount(totalBalance);
            paymentWS.setBalance(totalBalance);
            paymentWS.setCreateDatetime(Calendar.getInstance().getTime());
            paymentWS.setCurrencyId(user.getCurrencyId());
            paymentWS.setPaymentInstruments(paymentInstruments);
            paymentWS.setUserId(userId);
            paymentWS.setAutoPayment(1);
            paymentWS.setUserPaymentInstruments(paymentInstruments);
            paymentWS.setPaymentDate(TimezoneHelper.convertToTimezone(Calendar.getInstance().getTime(),
                    TimezoneHelper.getCompanyLevelTimeZone(user.getEntity().getId())));
            paymentWS.setIsRefund(0);
            PaymentDTOEx dto = new PaymentDTOEx(paymentWS);
            Integer result = paymentBean.processAndUpdateInvoice(dto, null, entityId, userId);
            Integer paymentId = paymentBean.getLatestPayment(userId, result);

            if (Constants.RESULT_OK.equals(result)){

                InvoiceDAS invoiceDAS = new InvoiceDAS();
                PaymentInvoiceMapDAS paymentInvoiceMapDAS = new PaymentInvoiceMapDAS();

                for (Integer invoiceId: invoiceIds){
                    // check if payment is already linked to this invoice
                    boolean paymentAlreadyLinkedToInvoice = false;
                    List<PaymentInvoiceMapDTO> paymentInvoiceMap = 
                            paymentInvoiceMapDAS.getLinkedPaymentsByInvoiceId(invoiceId);

                    if (null != paymentInvoiceMap && !paymentInvoiceMap.isEmpty()) {
                        for (PaymentInvoiceMapDTO paymentInvoice : paymentInvoiceMap) {
                            if (paymentId == paymentInvoice.getPayment().getId()) {
                                paymentAlreadyLinkedToInvoice = true;
                                break;
                            }
                        }
                    }

                    if (!paymentAlreadyLinkedToInvoice) {
                        PaymentBL paymentBL = new PaymentBL();
                        // the payment is not already linked to this invoice, so link it
                        invoiceDAS.findForUpdate(invoiceId); //lock it
                        paymentBean.applyPayment(paymentId, invoiceId);
                        paymentBL.set(paymentId);
                        PaymentDTO paymentDTO = paymentBL.getDTO();
                        if (paymentDTO.getBalance().compareTo(BigDecimal.ZERO) <= 0){
                            break;
                        }
                    }
                }
            }
        }
    }

    private List<PaymentInformationWS> convertPaymentInformationDTOtoWS(List<PaymentInformationDTO> dtos) {
        List<PaymentInformationWS> result = new ArrayList<>();
        if (null != dtos && !dtos.isEmpty()) {
            result = dtos.stream()
                    .filter(pi -> pi.getProcessingOrder() != null &&
                                  pi.getProcessingOrder() > 0 &&
                                  pi.getMetaFields().stream().anyMatch(mf -> MetaFieldType.AUTO_PAYMENT_AUTHORIZATION.equals(mf.getField().getFieldUsage()) &&
                                        ((BooleanMetaFieldValue) mf).getValue()))
                    .sorted(Comparator.comparing(PaymentInformationDTO::getProcessingOrder))
                    .map(PaymentInformationBL::getWS)
                    .collect(Collectors.toList());
        }
        return result;
    }
}
