package com.sapienter.jbilling.server.mediation.processor;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.util.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by marcolin on 13/10/15.
 */
public class JMRPaginationProcessorWriterImpl implements ItemWriter<List<JbillingMediationRecord>>, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private JMRRepository jmrRepository;

    private OrderService orderService;

    @Autowired
    private JMErrorRepository jmErrorRepository;


    public void setJmrRepository(JMRRepository jmrRepository) {
        this.jmrRepository = jmrRepository;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    @Override
    public void write(List<? extends List<JbillingMediationRecord>> listsByUser) throws Exception {
        for (List<JbillingMediationRecord> jmrsByUser : listsByUser) {
            Date startDate = new Date();
            Integer userId = jmrsByUser.get(0).getUserId();
            logger.debug("Starting processing for user {} at date {} ", userId, new SimpleDateFormat("hh:mm:ss").format(new Date()));
            long timeForOrderCreation = 0;
            for (JbillingMediationRecord jmr : jmrsByUser) {
                Date before = new Date();
                try {
                    MediationEventResult mediationEventResult = orderService.addMediationEvent(jmr);
                    timeForOrderCreation += new Date().getTime() - before.getTime();
                    if (!mediationEventResult.hasException()) {
                        if (mediationEventResult.hasQuantityEvaluated()) {
                            jmr.setOrderId(mediationEventResult.getCurrentOrderId());
                            jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
                            jmr.setRatedPrice(mediationEventResult.getAmountForChange());
                            jmr.setRatedCostPrice(mediationEventResult.getCostAmountForChange());
                            jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
                        } else {
                            jmr.setOrderId(mediationEventResult.getCurrentOrderId());
                            jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
                            jmr.setRatedPrice(mediationEventResult.getAmountForChange());
                            jmr.setStatus(JbillingMediationRecord.STATUS.NOT_BILLABLE);
                        }
                        updateMediationRecord(jmr);
                    } else {
                        moveJMRToErrorRecord(jmr, mediationEventResult.getExceptionMessage());
                    }
                } catch (Exception ex) {
                    moveJMRToErrorRecord(jmr, ex.getMessage());
                }
                sendMetric(jmr);

            }
            logger.debug("Processed user {} {} cdrs in {} ms and order creation took {} ms", userId, jmrsByUser.size(),
                    (new Date().getTime() - startDate.getTime()), timeForOrderCreation);
        }
    }

    public JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(
                jmrRepository.save(DaoConverter.getMediationRecordDao(jbillingMediationRecord)));
    }

    private static void sendMetric(JbillingMediationRecord callDataRecord) {
        try {
            MetricsHelper.log("Readed JMR: " + callDataRecord.toString(),
                    InetAddress.getLocalHost().toString(),
                    MetricsHelper.MetricType.ORDER_CREATED.name());
        } catch (Exception e) {}
    }

    private void moveJMRToErrorRecord(JbillingMediationRecord jmr, String errorMessage) {
        logger.error("Exception occurred while processing JMR for CDR key {} {}", jmr.getRecordKey(), errorMessage);
        jmErrorRepository.save(DaoConverter.getMediationErrorRecordDao(jmr));
        jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String beanName = stepExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_ORDER_SERVICE_BEAN_NAME_KEY);
        setOrderService(Context.getBean(beanName));
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }
}
