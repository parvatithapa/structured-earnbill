package com.sapienter.jbilling.server.mediation.processor;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by marcolin on 13/10/15.
 */
public class JMRProcessorReader implements ItemReader<JbillingMediationRecord> {
    private Iterator<JbillingMediationRecord> recordsToProcess;

    @Autowired
    private JMRRepository jmrRepository;

    public void setJmrRepository(JMRRepository jmrRepository) {
        this.jmrRepository = jmrRepository;
    }

    @BeforeStep
    public void beforeStepStepExecution (StepExecution stepExecution) {
        recordsToProcess = getUnprocessedMediationRecords().iterator();
    }

    @Override
    public JbillingMediationRecord read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        while (recordsToProcess.hasNext()) {
            JbillingMediationRecord next = recordsToProcess.next();
            sendMetric(next);
            return next;
        }
        return null;
    }


    public List<JbillingMediationRecord> getUnprocessedMediationRecords() {
        //TODO: Implements a DAS
        Iterable<JbillingMediationRecordDao> all = jmrRepository.findAll();
        List<JbillingMediationRecord> records = new ArrayList<>();
        for (JbillingMediationRecordDao record: all) {
            if (record.getStatus().equals(JbillingMediationRecordDao.STATUS.UNPROCESSED)) {
                records.add(DaoConverter.getMediationRecord(record));
            }
        }
        return records;
    }

    private static void sendMetric(JbillingMediationRecord callDataRecord) {
        try {
            MetricsHelper.log("Readed JMR: " + callDataRecord.toString(),
                    InetAddress.getLocalHost().toString(),
                    MetricsHelper.MetricType.JMR_READ.name());
        } catch (Exception e) {}
    }

}
