package com.sapienter.jbilling.batch.ignition;

import com.sapienter.jbilling.batch.support.PartitionService;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.IgnitionUtility;
import com.sapienter.jbilling.server.invoice.db.InvoiceDAS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserBL;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;

/**
 * Created by wajeeha on 2/15/18.
 */
public class IgnitionCustomerPaymentProcessor implements ItemProcessor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;
    @Resource
    private InvoiceDAS invoiceDAS;
    @Resource
    private OrderDAS orderDAS;
    @Resource
    private PartitionService partitionService;
    @Resource
    private IgnitionBatchService jdbcService;

    @Value("#{stepExecution.jobExecution.jobId}")
    private Long jobId;
    @Value("#{jobParameters['processLatestInvoice'] eq 'true'}")
    private boolean processLatestInvoice = false;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    private Integer findActiveOrdersForPayment (Integer userId) {

        List<OrderDTO> orders = orderDAS.findByUserSubscriptions(userId);

        if (orders.isEmpty()) {
            return null;
        }
        if(orders.size()>1) {
            logger.debug("More than one Active order found for the User {}",userId);
        }
        return orders.get(0).getId();
    }

    @Override
    public Integer process (Integer userId) throws Exception {

        Integer paymentId = null;
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {
            UserWS userWS = new UserBL(userId).getUserWS();
            Integer orderId = findActiveOrdersForPayment(userId);
            if (orderId != null) {
                OrderWS orderWS = IgnitionUtility.getOrder(userWS, orderId);
                logger.debug("orderWS: {}, User: {}", orderWS, userWS);
                String orderStatusDescription = orderWS.getOrderStatusWS().getDescription();

                if (!IgnitionConstants.ORDER_STATUS_LAPSED.equals(orderStatusDescription)) {
                    paymentId = applyPayment(userWS, null, orderWS.getTotalAsDecimal());
                } else {
                    // No Payment will be created for Orders in 'LAPSED' status
                    logger.debug("Not creating payment for order: {} as the Order Status is: {}", orderWS.getId(),
                            orderStatusDescription);
                }
            } else {
                logger.debug("No active orders for user id: {}", userId);
            }
        }

        partitionService.markUserAsSuccessful(jobId, userId, 1);
        if (paymentId != null) {
            jdbcService.createJobPaymentId(jobId, paymentId);
        }
        return paymentId;
    }

    private Integer applyPayment (UserWS userWS, Integer invoiceId, BigDecimal amount) {

        PaymentWS paymentWS = createPayment(userWS, amount);
        return webServicesSessionBean.applyPayment(paymentWS, invoiceId);
    }

    private PaymentWS createPayment (UserWS userWS, BigDecimal amount) {

        PaymentInformationWS paymentInformation = userWS.getPaymentInstruments().get(0);

        PaymentWS payment = new PaymentWS();
        payment.setAmount(amount);
        payment.setIsRefund(0);
        payment.setMethodId(Constants.PAYMENT_METHOD_CUSTOM);
        payment.setPaymentDate(TimezoneHelper.companyCurrentDate(userWS.getEntityId()));
        payment.setCreateDatetime(TimezoneHelper.companyCurrentDate(entityId));
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(userWS.getCurrencyId());
        payment.setUserId(userWS.getUserId());
        payment.setPaymentNotes(IgnitionConstants.IGNITION_SCHEDULED_PAYMENT_NOTE);
        payment.setPaymentPeriod(1);
        payment.getPaymentInstruments().add(paymentInformation);

        return payment;
    }
}
