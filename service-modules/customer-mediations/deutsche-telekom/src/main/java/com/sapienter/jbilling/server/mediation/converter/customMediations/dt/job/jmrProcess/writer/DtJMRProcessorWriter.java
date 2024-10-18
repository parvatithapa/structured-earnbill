package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.jmrProcess.writer;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationErrorRecordDao;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.processor.JmrProcessorAggregator;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.MediationEventResultList;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.util.Context;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DtJMRProcessorWriter implements ItemWriter<List<JbillingMediationRecord>> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    @Qualifier("dtOrderService")
    private OrderService orderService;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    private Integer batchSize;
    private boolean sendMetrics;
    private TransactionTemplate trxnTemplate;

    @PostConstruct
    public void init() {
        trxnTemplate = transactionTemplate();
    }

    @Override
    public void write(List<? extends List<JbillingMediationRecord>> listsByUser) throws Exception {

        JmrProcessorAggregator aggregator = getAggregator();

        for (List<JbillingMediationRecord> jmrsByUser : listsByUser) {
            if (jmrsByUser.size() > 0) {
                logger.debug("PreWrite: User [{}] JMR count [{}]", jmrsByUser.get(0).getUserId(), jmrsByUser.size());
            }

            aggregator.clear();
            Instant start = Instant.now();

            List<JbillingMediationRecord> filtered = filterJMRsEligibleForAggregation(jmrsByUser, aggregator);
            Iterator<List<JbillingMediationRecord>> iterator = lazyPartition(filtered, batchSize);

            long timeForOrderCreation = 0L;
            while (iterator.hasNext()) {
                List<JbillingMediationRecord> batch = iterator.next();
                logger.debug("Getting next batch with {} JMRs", batch.size());

                timeForOrderCreation += processBatch(batch);
            }

            timeForOrderCreation += processBatch(new ArrayList<>(aggregator.getAggregates()));

            logger.debug("Processed {} CDRs in {} ms and order creation took {} ms",
                    jmrsByUser.size(), Duration.between(start, Instant.now()).toMillis(), timeForOrderCreation);
        }
    }

    private Iterator<List<JbillingMediationRecord>> lazyPartition(
            final List<JbillingMediationRecord> listByUser,
            final int batchSize) {

        return new Iterator<List<JbillingMediationRecord>>() {

            private final List<JbillingMediationRecord> sourceList = listByUser;
            private final int subListSize = batchSize > 0 ? batchSize : listByUser.size();
            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < sourceList.size();
            }

            @Override
            public List<JbillingMediationRecord> next() {
                final int begin = currentIndex;
                final int end = Math.min(begin + subListSize, sourceList.size());

                currentIndex += subListSize;
                return sourceList.subList(begin, end);
            }
        };
    }

    private List<JbillingMediationRecord> filterJMRsEligibleForAggregation(
            final List<JbillingMediationRecord> jmrsByUser,
            final JmrProcessorAggregator aggregator) {

        try {
            return trxnTemplate.execute(transactionStatus -> {

                List<JbillingMediationRecord> toReturn = new ArrayList<>();
                List<JbillingMediationRecord> toRemove = new ArrayList<>();

                for (JbillingMediationRecord jmr : jmrsByUser) {
                    if (aggregator.aggregate(jmr)) {
                        jmr.setStatus(JbillingMediationRecord.STATUS.AGGREGATED);
                        toRemove.add(jmr);
                    } else {
                        toReturn.add(jmr);
                    }
                }

                if (toRemove.size() > 0) {
                    logger.debug("Updating {} records as STATUS.AGGREGATED", toRemove.size());
                    updateMediationRecords(toRemove);
                }

                logger.debug("{} Total, {} To-be-processed, {} Aggregated", jmrsByUser.size(),
                        toReturn.size() + aggregator.getAggregates().size(), toRemove.size());
                return toReturn;
            });
        } catch (Exception e) {
            logger.error("Error while segregating aggregation-eligible records", e);
            aggregator.clear();
            return jmrsByUser;
        }
    }

    private long processBatch(final List<JbillingMediationRecord> jmrsByUser) {

        if (jmrsByUser.isEmpty()) {
            logger.debug("No more records to process");
            return 0L;
        }

        Integer userId = jmrsByUser.get(0).getUserId();
        logger.debug("Starting processing for user {}", userId);

        long timeForOrder;

        timeForOrder = jmrToOrder(jmrsByUser);
        sendMetric(jmrsByUser);

        logger.debug("Time taken for current batch of size {}, for user {} : {} ms",
                jmrsByUser.size(), userId, timeForOrder);

        return timeForOrder;
    }

    private long jmrToOrder(List<JbillingMediationRecord> jmrList) {
        Instant start = Instant.now();
        MediationEventResultList resultList = null;
        try {
            resultList = orderService.addMediationEventList(jmrList);
        } catch (Exception e) {
            logger.error("FATAL: Exception in order creation", e);
        }

        if (resultList == null || !processResults(jmrList, resultList)) {
            retryFailedOrderCreation(jmrList);
        }

        return Duration.between(start, Instant.now()).toMillis();
    }

    private boolean processResults(List<JbillingMediationRecord> jmrList, MediationEventResultList resultList) {
        logger.debug("A rollback in order creation: {}", resultList.isRolledBack());
        if (resultList.isRolledBack()) {
            return false;
        } else {
            return trxnTemplate.execute(transactionStatus -> {
                List<MediationEventResult> mediationEventResults = resultList.results();
                for (int i = 0; i < mediationEventResults.size(); i++) {
                    process(jmrList.get(i), mediationEventResults.get(i));
                }
                return true;
            });
        }
    }

    // Assuming order creation failed for a record in a batch, all the orders
    // in that batch are rolled back; so, we process each JMR in its own transaction
    // to isolate the rollback in OrderServiceImpl to the erroneous JMR.
    private void retryFailedOrderCreation(List<JbillingMediationRecord> jmrList) {
        logger.debug("There was a failure in order creation");

        for (JbillingMediationRecord jmr : jmrList) {
            logger.debug("Retrying jmr with key: {}", jmr.getRecordKey());
            MediationEventResult result = null;
            try {
                result = orderService.addMediationEvent(jmr);
                logger.debug("Processed retried jmr with key: {}", jmr.getRecordKey());

            } catch (Exception e) {
                logger.error("Exception while retrying order creation", e);
                result = new MediationEventResult();
                result.setExceptionMessage(e.getMessage());
            } finally {
                processInIsolation(jmr, result);
            }
        }
    }

    private void processInIsolation(JbillingMediationRecord jmr, MediationEventResult mediationEventResult) {
        trxnTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
                process(jmr, mediationEventResult);
            }
        });
    }

    private void process(JbillingMediationRecord jmr, MediationEventResult mediationEventResult) {
        try {
            if (StringUtils.isNotBlank(mediationEventResult.getErrorCodes())) {
                moveJMRToErrorRecord(jmr, mediationEventResult.getErrorCodes(),
                        mediationEventResult.isQuantityResolutionSuccess());
            }
            else if (mediationEventResult.hasException()) {
                logger.debug("JMR -> has errors, moving to JMError: {}", jmr.getRecordKey());
                moveJMRToErrorRecord(jmr, mediationEventResult.getExceptionMessage());
            } else {
                logger.debug("JMR -> Order done, updating JMR: {}", jmr.getRecordKey());
                jmr.setOrderId(mediationEventResult.getCurrentOrderId());
                jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
                jmr.setRatedPrice(mediationEventResult.getAmountForChange());

                if (mediationEventResult.hasQuantityEvaluated()) {
                    jmr.setRatedCostPrice(mediationEventResult.getCostAmountForChange());
                    jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
                } else {
                    jmr.setStatus(JbillingMediationRecord.STATUS.NOT_BILLABLE);
                }

                updateMediationRecord(jmr);
                logger.debug("JMR -> Updated JMR: {}", jmr.getRecordKey());
            }
        } catch (Exception e) {
            logger.error("Exception while updating JMR with result", e);
            moveJMRToErrorRecord(jmr, e.getMessage());
        }
    }

    public void updateMediationRecords(List<JbillingMediationRecord> jbillingMediationRecords) {

        List<JbillingMediationRecordDao> daos = new ArrayList<>();
        for (JbillingMediationRecord record : jbillingMediationRecords) {
            daos.add(DaoConverter.getMediationRecordDao(record));
        }

        jmrRepository.save(daos);
    }

    public JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(
                jmrRepository.save(DaoConverter.getMediationRecordDao(jbillingMediationRecord)));
    }

    private void sendMetric(List<JbillingMediationRecord> records) {
        if (sendMetrics) {
            try {
                for (JbillingMediationRecord callDataRecord : records) {
                    MetricsHelper.log("Processed JMR: " + callDataRecord.toString(),
                            InetAddress.getLocalHost().toString(),
                            MetricsHelper.MetricType.ORDER_CREATED.name());
                }
            } catch (Exception e) {}
        }
    }

    private void moveJMRToErrorRecord(JbillingMediationRecord jmr, String errorCodes, boolean quantityResSuccess) {
        logger.error("Error in resolving quantity for CDR Key: {}, errors: {}", jmr.getRecordKey(), errorCodes);
        jmErrorRepository.save(getErrorRecord(jmr, errorCodes, quantityResSuccess));
        jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
    }

    private void moveJMRToErrorRecord(JbillingMediationRecord jmr, String errorMessage) {
        logger.error("Exception occurred while processing JMR for CDR key {} : {}", jmr.getRecordKey(), errorMessage);
        jmErrorRepository.save(getErrorRecord(jmr, true));
        jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
    }

    private JbillingMediationErrorRecordDao getErrorRecord(JbillingMediationRecord jmr, String errorCodes,
                                                           boolean quantityResSuccess) {
        JbillingMediationErrorRecordDao errorRecordDao = DaoConverter.getMediationErrorRecordDao(errorCodes, jmr);
        errorRecordDao.setErrorUsageRecord(DaoConverter.getErrorUsageRecordDao(jmr, quantityResSuccess));
        return errorRecordDao;
    }

    private JbillingMediationErrorRecordDao getErrorRecord(JbillingMediationRecord jmr, boolean quantityResSuccess) {
        JbillingMediationErrorRecordDao errorRecordDao = DaoConverter.getMediationErrorRecordDao(jmr);
        errorRecordDao.setErrorUsageRecord(DaoConverter.getErrorUsageRecordDao(jmr, quantityResSuccess));
        return errorRecordDao;
    }

    private TransactionTemplate transactionTemplate() {
        return new TransactionTemplate(getMediationTransactionManager(),
                getDefaultTransactionDef());
    }

    public PlatformTransactionManager getMediationTransactionManager() {
        return com.sapienter.jbilling.server.util.Context.getBean(
                Context.Name.MEDIATION_TRANSACTION_MANAGER);
    }

    private DefaultTransactionDefinition getDefaultTransactionDef() {
        return new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    private JmrProcessorAggregator getAggregator() {
        return (JmrProcessorAggregator) Context.getBean("dtOfflineCdrJmrProcessorAggregator");
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public void setSendMetrics(boolean sendMetrics) {
        this.sendMetrics = sendMetrics;
    }
}
