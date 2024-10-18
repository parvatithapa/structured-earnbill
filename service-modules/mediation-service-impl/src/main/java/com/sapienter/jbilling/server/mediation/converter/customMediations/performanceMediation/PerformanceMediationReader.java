package com.sapienter.jbilling.server.mediation.converter.customMediations.performanceMediation;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.customMediations.sampleMediation.SampleMediationRecordLineConverter;
import com.sapienter.jbilling.server.util.Context;
import org.joda.time.format.DateTimeFormat;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;

import java.util.*;

/**
 * Created by marcolin on 14/04/16.
 */
public class PerformanceMediationReader implements ItemReader<ICallDataRecord> {

    private Integer stepsStarted = 0;
    private static List<UUID> uuidsAlreadyChosen = new ArrayList<>();
    @BeforeStep
    public void resetCounter(StepExecution stepExecution) {
        stepsStarted = stepsStarted + 1;
    }

    @AfterStep
    public void removeOneStep(StepExecution stepExecution) {
        stepsStarted = 0;
        SharedCounter.reset();
    }

    @Override
    public ICallDataRecord read() {
        SampleMediationRecordLineConverter sampleMediationRecordLineConverter = Context.getBean("sampleMediationConverter");
        sampleMediationRecordLineConverter.setDateFormat(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Integer nextCount = SharedCounter.getNextCount();
        if (nextCount > 1000 * stepsStarted) {
            return null;
        }
        return sampleMediationRecordLineConverter.convertLineToRecord(getUUID().toString() + ",38922734081,38925634973,1975-05-25 00:00:00,1,320100," + username(nextCount));
    }

    private String username(Integer nextCount) {
        Integer userNumber = nextCount % 11 + 1;
        return "mediation-batch-test-" + (userNumber < 10 ? "0" : "") + userNumber;
    }

    synchronized static UUID getUUID() {
        UUID uuid = UUID.randomUUID();
        while (uuidsAlreadyChosen.contains(uuid))
            uuid = UUID.randomUUID();
        uuidsAlreadyChosen.add(uuid);
        return uuid;
    }

}
