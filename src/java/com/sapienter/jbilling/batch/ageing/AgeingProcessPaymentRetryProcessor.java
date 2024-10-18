package com.sapienter.jbilling.batch.ageing;

import java.lang.invoke.MethodHandles;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.payment.IPaymentSessionBean;
import com.sapienter.jbilling.server.payment.PaymentInformationBL;
import com.sapienter.jbilling.server.payment.db.PaymentInformationDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;

/**
 * 
 * @author Khobab
 *
 */
public class AgeingProcessPaymentRetryProcessor implements ItemProcessor<AgeingStatusResult, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private IPaymentSessionBean paymentSessionBean;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    /**
     * gets user id from item reader one by one and retry payment on each id
     */
    @Override
    public Integer process (AgeingStatusResult ageingStatusResult) throws Exception {
        logger.debug("Retrying payment of user with status # {}", ageingStatusResult);
        if(PaymentInformationBL.isPaymentAuthorizationPreferenceEnabled(entityId)) {
            List<PaymentInformationDTO> paymentInstruments = PaymentInformationBL.filterForAutoAuthorization(
                    new UserDAS().find(ageingStatusResult.getUserId()).getPaymentInstruments());
            if (CollectionUtils.isNotEmpty(paymentInstruments)) {
                paymentSessionBean.doPaymentRetry(ageingStatusResult.getUserId(), ageingStatusResult.getOverdueInvoices());
            } else {
                logger.debug("Skipping payment of user {} since no payment instruments with autopayment authorization",
                        ageingStatusResult.getUserId());
            }
        } else {
            paymentSessionBean.doPaymentRetry(ageingStatusResult.getUserId(), ageingStatusResult.getOverdueInvoices());
        }

        return ageingStatusResult.getUserId();
    }
}
