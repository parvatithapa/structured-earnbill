/*
 JBILLING CONFIDENTIAL
 _____________________

 [2003] - [2012] Enterprise jBilling Software Ltd.
 All Rights Reserved.

 NOTICE:  All information contained herein is, and remains
 the property of Enterprise jBilling Software.
 The intellectual and technical concepts contained
 herein are proprietary to Enterprise jBilling Software
 and are protected by trade secret or copyright law.
 Dissemination of this information or reproduction of this material
 is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation.converter.customMediations.distributelMediation;

import com.sapienter.jbilling.server.order.OrderService;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;

import java.util.List;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.common.FormatLogger;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by igutierrez on 24/01/17.
 */
public class DistributelWriter implements ItemWriter<JbillingMediationRecord> {

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private OrderService orderServiceDistributel;

    @Autowired
    private JMErrorRepository errorRepository;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    private final static FormatLogger LOG = new FormatLogger(DistributelWriter.class);

    public void setJmrRepository(JMRRepository jmrRepository) {
        this.jmrRepository = jmrRepository;
    }

    @Override
    public void write(List<? extends JbillingMediationRecord> listsByUser) throws Exception {
        for (JbillingMediationRecord jmr : listsByUser) {
            Date before = new Date();
            Integer userId = jmr.getUserId();
            LOG.debug("Starting processing for user " + userId + " at date " + new SimpleDateFormat("hh:mm:ss").format(new Date()));

            MediationEventResult mediationEventResult = orderServiceDistributel.addMediationEventDistributel(jmr);

            if (null != mediationEventResult) {
                if (!mediationEventResult.hasException()) {
                    jmr.setOrderId(mediationEventResult.getCurrentOrderId());
                    jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
                    jmr.setRatedPrice(mediationEventResult.getAmountForChange());
                    jmr.setRatedCostPrice(mediationEventResult.getCostAmountForChange());
                    jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
                    updateMediationRecord(jmr);
                } else {
                    LOG.error("Exception occurred while processing JMR for CDR key '" + jmr.getRecordKey() + "' : " +
                            mediationEventResult.getExceptionMessage());
                    jmErrorRepository.save(DaoConverter.getMediationErrorRecordDao(jmr));
                    jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
                }
            } else {
                jmr.setStatus(JbillingMediationRecord.STATUS.NOT_BILLABLE);
                updateMediationRecord(jmr);
            }
            sendMetric(jmr);
        }
    }

    private static void sendMetric(JbillingMediationRecord callDataRecord) {
        try {
            MetricsHelper.log("Readed JMR: " + callDataRecord.toString(),
                    InetAddress.getLocalHost().toString(),
                    MetricsHelper.MetricType.ORDER_CREATED.name());
        } catch (Exception e) {
        }
    }

    public JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(
                jmrRepository.save(DaoConverter.getMediationRecordDao(jbillingMediationRecord)));
    }
}