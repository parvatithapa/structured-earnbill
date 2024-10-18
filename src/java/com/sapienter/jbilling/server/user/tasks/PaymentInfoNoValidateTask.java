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

package com.sapienter.jbilling.server.user.tasks;

import java.util.Date;

import org.apache.log4j.Logger;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.payment.db.PaymentMethodDAS;
import com.sapienter.jbilling.server.pluggableTask.PaymentInfoTask;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.TaskException;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;
import org.joda.time.format.DateTimeFormat;

/**
 * This creates payment dto. It now only goes and fetches the credit card
 * of the given user. It doesn't need to initialize the rest of the payment
 * information (amount, etc), only the info for the payment processor, 
 * usually cc info but it could be electronic cheque, etc...
 * This task should consider that the user is a partner and is being paid
 * (like a refund) and therefore fetch some other information, as getting 
 * paid with a cc seems not to be the norm.
 * @author Emil
 */
public class PaymentInfoNoValidateTask 
        extends PluggableTask implements PaymentInfoTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PaymentInfoNoValidateTask.class));
    /** 
     * This will return an empty payment dto with only the credit card/ach set
     * if a valid credit card is found for the user. Otherwise null.
     * It will check the customer's preference for the automatic payment type.
     */
    public PaymentDTOEx getPaymentInfo(Integer userId) 
            throws TaskException {
        PaymentDTOEx retValue = new PaymentDTOEx();
        PaymentInformationBL paymentBL = new PaymentInformationBL();
        try {
            UserBL userBL = new UserBL(userId);

            for (PaymentInformationDTO paymentInformation : userBL.getEntity().getPaymentInstruments()) {
                LOG.debug("Payment instrument " + paymentInformation.getId());
                // If its a payment/credit card
                if (paymentInformation.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName().equalsIgnoreCase(CommonConstants.PAYMENT_CARD)) {
                    processCreditCard(retValue, paymentInformation, paymentBL);
                } else if (paymentInformation.getPaymentMethodType().getPaymentMethodTemplate().getTemplateName().equalsIgnoreCase(CommonConstants.ACH)) {
                    processACH(retValue, paymentInformation);
                }

                //paymentInformation.close();
            }

        } catch (Exception e) {
            throw new TaskException(e);
        }
        if (retValue.getPaymentInstruments().isEmpty()) {
            LOG.debug("Could not find payment instrument for user " + userId);
            retValue = null;
        }
        return retValue;
    }
    
    /**
     * Processes the payment instrument that is a credit card and if its a valid credit card then adds it to payment instruments
     * 
     * @param dto	PaymentDTOEx
     * @param paymentInstrument	PaymentInformationDTO
     * @param piBl	PaymentInformationBL
     * @throws UnsupportedOperationException
     */
    protected void processCreditCard(PaymentDTOEx dto, PaymentInformationDTO paymentInstrument, PaymentInformationBL piBl) throws UnsupportedOperationException {
    	// TODO: some fields like gateway key that were being process in old implementation have been removed at all
    	char[] cardNumber = piBl.getCharMetaFieldByType(paymentInstrument, MetaFieldType.PAYMENT_CARD_NUMBER);
    	Date ccExpiryDate = DateTimeFormat.forPattern("mm/yy").parseDateTime(piBl.getStringMetaFieldByType(paymentInstrument, MetaFieldType.DATE)).toDate();
    	LOG.debug("Expiry date is: " + ccExpiryDate);
    	if(piBl.validateCreditCard(ccExpiryDate, cardNumber)) {
    		LOG.debug("Card is valid");

            try(PaymentInformationDTO paymentInformation = paymentInstrument.getDTO()) {
                paymentInformation.setPaymentMethod(new PaymentMethodDAS().find(piBl.getPaymentMethod(cardNumber)));

                dto.getPaymentInstruments().add(paymentInformation);
            }catch (Exception exception){
                LOG.debug("Exception: " + exception);
            }
    	}
    }
    
    /**
     * Process the payment instrument if its ach
     * @param dto	PaymentDTOEx
     * @param paymentInstrument	PaymentInformationDTO
     */
    protected void processACH(PaymentDTOEx dto, PaymentInformationDTO paymentInstrument) {
    	// TODO: some fields like gateway key that were being process in old implementation have been removed at all

        try (PaymentInformationDTO paymentInformation = paymentInstrument.getDTO()) {
            paymentInformation.setPaymentMethod(new PaymentMethodDAS().find(Constants.PAYMENT_METHOD_ACH));

            dto.getPaymentInstruments().add(paymentInformation);
        }catch (Exception exception){
            LOG.debug("Exception: " + exception);
        }
    }

}
