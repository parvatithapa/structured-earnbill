package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.jmrProcess.reader;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.processor.JmrProcessorConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class DtJMRReader implements ItemReader<List<JbillingMediationRecord>>, JmrProcessorConstants, StepExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Iterator<Integer> userIdIterator;

    @Autowired
    private JMRRepository jmrRepository;

    private boolean sendMetrics;

    private Integer currentPartition;
    private Integer totalPartitions;

    public void setCurrentPartition(Integer currentPartition) {
        this.currentPartition = currentPartition;
    }

    public void setTotalPartitions(Integer totalPartitions) {
        this.totalPartitions = totalPartitions;
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        if(currentPartition == null) {
            currentPartition = stepExecution.getExecutionContext().getInt(PARM_CURRENT_PARTITION);
        }

        if(totalPartitions == null) {
            totalPartitions = stepExecution.getExecutionContext().getInt(PARM_NUMBER_OF_PARTITIONS);
        }

        List<Integer> userIds = jmrRepository.findUsersByStatusAndPartition(JbillingMediationRecordDao.STATUS.UNPROCESSED.name(),
                totalPartitions, currentPartition);

        logger.debug("Partition [{}], total [{}], users [{}]", currentPartition, totalPartitions, userIds);
        userIdIterator = userIds.iterator();
    }

    @Override
    public synchronized List<JbillingMediationRecord> read() throws Exception {
        while (userIdIterator.hasNext()) {
            Integer userId = userIdIterator.next();
            List<JbillingMediationRecord> recordsForUser = getMediationRecordsForUserAndStatus(userId,
                    JbillingMediationRecordDao.STATUS.UNPROCESSED);
            logger.debug("Read: User [{}] JMR count [{}]", userId, recordsForUser.size());
            sendMetric(recordsForUser);
            return recordsForUser;
        }
        return null;
    }

    private List<JbillingMediationRecord> getMediationRecordsForUserAndStatus(Integer userId,
                                                                              JbillingMediationRecordDao.STATUS status) {
        List<JbillingMediationRecordDao> byUserIdAndStatus = jmrRepository.findByUserIdAndStatus(userId,
                status);
        return byUserIdAndStatus.stream().map(DaoConverter::getMediationRecord).collect(Collectors.toList());
    }

    private void sendMetric(List<JbillingMediationRecord> recordsForUser) {
        if (sendMetrics) {
            recordsForUser.forEach(callDataRecord -> {
                try {
                    MetricsHelper.log("Read JMR: " + callDataRecord.toString(),
                            InetAddress.getLocalHost().toString(),
                            MetricsHelper.MetricType.JMR_READ.name());
                } catch (Exception e) {}
            });
        }
    }

    public void setSendMetrics(boolean sendMetrics) {
        this.sendMetrics = sendMetrics;
    }
}
