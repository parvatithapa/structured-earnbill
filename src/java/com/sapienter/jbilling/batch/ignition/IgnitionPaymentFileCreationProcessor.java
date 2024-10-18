package com.sapienter.jbilling.batch.ignition;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.ignition.ServiceProfile;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.event.IgnitionPaymentEvent;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;

import java.lang.invoke.MethodHandles;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by wajeeha on 3/6/18.
 */
public class IgnitionPaymentFileCreationProcessor implements ItemProcessor<Integer, Integer>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;
    @Resource
    private IgnitionBatchService jdbcService;

    @Value("#{stepExecution.jobExecution.jobId}")
    private Long jobId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    private Map<String, Map<String, ServiceProfile>> allServiceProfiles;

    @Resource
    private OrderDAS orderDAS;

    public void setAllServiceProfiles (Map<String, Map<String, ServiceProfile>> allServiceProfiles) {
        this.allServiceProfiles = allServiceProfiles;
    }

    @Override
    public Integer process (Integer paymentId) throws Exception {
        String paymentMetafieldsInformation = null;

        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {

            PaymentWS paymentWS = webServicesSessionBean.getPayment(paymentId);

            if (paymentWS != null) {
                Integer orderId = findActiveOrdersForPayment(paymentWS.getUserId());
                EventManager.process(
                        new IgnitionPaymentEvent(paymentWS, this.entityId, orderId, this.allServiceProfiles));
                paymentMetafieldsInformation = generateMetafieldsString(paymentWS);
            } else {
                logger.debug("No payment found");
            }
        }
        jdbcService.persistMetafieldsData(jobId, paymentId, paymentMetafieldsInformation);
        return paymentId;
    }

    public String generateMetafieldsString (PaymentWS paymentWS) {
        StringBuilder metaFieldValue = new StringBuilder();
        Map<String, String> paymentInformation = new LinkedHashMap<>();

        paymentInformation.put(IgnitionConstants.PAYMENT_USER_REFERENCE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_ACTION_DATE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_CLIENT_CODE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_SENT_ON, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_TYPE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_TRANSMISSION_DATE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_CONTRACT_REFERENCE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_NAEDO_TYPE, "");
        paymentInformation.put(IgnitionConstants.PAYMENT_TRACKING_DAYS, "");

        for (MetaFieldValueWS metaField : paymentWS.getMetaFields()) {
            if (paymentInformation.containsKey(metaField.getFieldName())) {
                paymentInformation.replace(metaField.getFieldName(), metaField.getStringValue());
            }
        }

        for (String value : paymentInformation.values()) {
            metaFieldValue.append(value);
            metaFieldValue.append(",");
        }

        return metaFieldValue.toString();
    }

    @Override
    public void afterPropertiesSet () throws Exception {
        allServiceProfiles = (Map<String, Map<String, ServiceProfile>>) Context.getApplicationContext()
                .getBean("allServiceProfilesGroupedByBrand", entityId);
    }

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
}
