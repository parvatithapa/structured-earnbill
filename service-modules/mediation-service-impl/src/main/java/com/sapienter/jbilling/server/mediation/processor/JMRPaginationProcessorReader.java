package com.sapienter.jbilling.server.mediation.processor;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Autowired;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;

/**
 * Created by martin on 21/10/15.
 */
public class JMRPaginationProcessorReader implements ItemReader<List<JbillingMediationRecord>>, JmrProcessorConstants {
    private Iterator<List<JbillingMediationRecord>> recordsToProcessByUserId;

    @Autowired
    private JMRRepository jmrRepository;

    private String userIds;

    public void setUserIds(String userIds) {
        this.userIds = userIds;
        beforeStepStepExecution(null);
    }

    @BeforeRead
    @BeforeStep
    public void beforeStepStepExecution (StepExecution stepExecution) {
        String commaSeparatedUsers = this.userIds;
        if (stepExecution != null) {
            commaSeparatedUsers = stepExecution.getExecutionContext().getString(PARM_USER_LIST);
        }

        List<Integer> userIds = Arrays.asList(commaSeparatedUsers.split(",")).stream()
                .filter(s -> !s.isEmpty())
                .map(s -> Integer.parseInt(s)).collect(Collectors.toList());
        List<List<JbillingMediationRecord>> listsForUsers = new LinkedList<>();
        for (Integer userId: userIds) {
            listsForUsers.add(getMediationRecordsForUserAndStatus(userId,
                    JbillingMediationRecordDao.STATUS.UNPROCESSED));
        }
        recordsToProcessByUserId = listsForUsers.iterator();
    }

    @Override
    public synchronized List<JbillingMediationRecord> read() throws Exception {
        while (recordsToProcessByUserId.hasNext()) {
            List<JbillingMediationRecord> recordsForUser = recordsToProcessByUserId.next();
            recordsForUser.forEach(r -> sendMetric(r));
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

    private static void sendMetric(JbillingMediationRecord callDataRecord) {
        try {
            MetricsHelper.log("Readed JMR: " + callDataRecord.toString(),
                    InetAddress.getLocalHost().toString(),
                    MetricsHelper.MetricType.JMR_READ.name());
        } catch (Exception e) {}
    }
}
