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

package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.payment.PaymentDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.util.Constants;

import org.apache.log4j.Logger;

/**
 * Created with IntelliJ IDEA.
 *
 *  This will check the preferred payment method by default
 *  If preferred method is not available it will check the next available payment method info
 *
 * @author Panche.Isajeski
 * @since 17/05/12
 */
public class AlternativePaymentInfoTask extends BasicPaymentInfoTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(AlternativePaymentInfoTask.class));

    @Override
    public PaymentDTOEx getPaymentInfo(Integer userId) throws TaskException {
        PaymentDTOEx retValue = new PaymentDTOEx();
        try {
            UserBL userBL = new UserBL(userId);
            
            for(PaymentInformationDTO paymentInformation : userBL.getEntity().getPaymentInstruments()) {
        		// If its a payment/credit card
        		if(paymentInformation.getPaymentMethodType().getMethodName().equalsIgnoreCase(CommonConstants.PAYMENT_CARD)) {
        			processCreditCard(retValue, paymentInformation);
        		} else if(paymentInformation.getPaymentMethodType().getMethodName().equalsIgnoreCase(CommonConstants.ACH)) {
        			processACH(retValue, paymentInformation);
        		} else if(paymentInformation.getPaymentMethodType().getMethodName().equalsIgnoreCase(CommonConstants.CHEQUE)){
        			processCheque(retValue, paymentInformation);
        		}
        		
        		if(retValue.getPaymentInstruments().size()>0) {
        			return retValue;
        		}
            }
        } catch (Exception e) {
            throw new TaskException(e);
        }
        if (retValue == null) {
            LOG.debug("Could not find payment instrument for user " + userId);
        }
        return null;
    }
}
