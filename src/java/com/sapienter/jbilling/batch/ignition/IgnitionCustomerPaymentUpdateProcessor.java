package com.sapienter.jbilling.batch.ignition;

import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.MetaFieldDAS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.security.RunAsCompanyAdmin;
import com.sapienter.jbilling.server.security.RunAsUser;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wajeeha on 3/7/18.
 */
public class IgnitionCustomerPaymentUpdateProcessor implements ItemProcessor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name="webServicesSession")
    private IWebServicesSessionBean webServicesSessionBean;
    @Resource
    private MetaFieldDAS metaFieldDAS;
    @Resource
    private IgnitionBatchService jdbcService;

    @Value("#{stepExecution.jobExecution.jobId}")
    private Long jobId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    //@formatter:off
    private String[] metafieldNames = new String[] {
            IgnitionConstants.PAYMENT_USER_REFERENCE,
            IgnitionConstants.PAYMENT_ACTION_DATE,
            IgnitionConstants.PAYMENT_CLIENT_CODE,
            IgnitionConstants.PAYMENT_SENT_ON,
            IgnitionConstants.PAYMENT_TYPE,
            IgnitionConstants.PAYMENT_SEQUENCE_NUMBER,
            IgnitionConstants.PAYMENT_TRANSMISSION_DATE,
            IgnitionConstants.PAYMENT_TRANSACTION_NUMBER,
            IgnitionConstants.PAYMENT_CONTRACT_REFERENCE,
            IgnitionConstants.PAYMENT_NAEDO_TYPE,
            IgnitionConstants.PAYMENT_TRACKING_DAYS
    };
    //@formatter:on

    @Override
    public Integer process (Integer paymentId) throws Exception {
        String paymentMetaFieldInformation = jdbcService.readMetafieldsData(jobId, paymentId);
        try (RunAsUser ctx = new RunAsCompanyAdmin(entityId)) {

            String[] metafieldInformationList = paymentMetaFieldInformation.split(",",
                    paymentMetaFieldInformation.length());

            PaymentWS paymentWS = webServicesSessionBean.getPayment(paymentId);

            List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>();

            for (int i = 0; i < metafieldNames.length; i++) {

                MetaField metaField = metaFieldDAS.getFieldByName(entityId, new EntityType[] { EntityType.PAYMENT },
                        metafieldNames[i]);

                if (metaField != null) {
                    MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
                    metaFieldValueWS.setFieldName(metafieldNames[i]);
                    metaFieldValueWS.setStringValue(metafieldInformationList[i]);
                    metaFieldValueList.add(metaFieldValueWS);
                }
            }
            MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
            metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

            paymentWS.setMetaFields(updatedMetaFieldValueWSArray);

            webServicesSessionBean.updatePayment(paymentWS);
            return paymentWS.getId();
        } catch (Exception exception) {
            logger.error("Exception: ", exception);
        }
        return null;
    }
}
