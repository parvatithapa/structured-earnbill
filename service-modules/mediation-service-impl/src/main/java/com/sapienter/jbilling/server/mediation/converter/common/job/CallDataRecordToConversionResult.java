package com.sapienter.jbilling.server.mediation.converter.common.job;

import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ConversionResult;
import com.sapienter.jbilling.server.mediation.MetricsHelper;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.common.FormatLogger;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.JMRMediationCdrResolver;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationResolverStatus;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import org.apache.log4j.Logger;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Created by marcolin on 07/10/15.
 */
public class CallDataRecordToConversionResult implements ItemProcessor<CallDataRecord, ConversionResult> {
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(CallDataRecordToConversionResult.class));

    private JMRMediationCdrResolver resolver;
    private JobExecution jobExecution;

    public void setResolver(JMRMediationCdrResolver resolver) {
        this.resolver = resolver;
    }

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        jobExecution = stepExecution.getJobExecution();
    }

    @Override
    public ConversionResult process(CallDataRecord callDataRecord) throws Exception {
        return processCallDataRecord(Integer.parseInt(jobExecution.getJobParameters().getString("entityId")),
                Integer.parseInt(jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_CONFIG_ID_KEY)),
                callDataRecord, resolver);
    }

    public static ConversionResult processCallDataRecord(Integer entityId, Integer mediationCfgId,
                                                         CallDataRecord callDataRecord, IMediationCdrResolver cdrResolver) {
        callDataRecord.setEntityId(entityId);
        callDataRecord.setMediationCfgId(mediationCfgId);

        MediationStepResult result = new MediationStepResult(callDataRecord);
        MediationResolverStatus mediationResolverStatus = cdrResolver.resolveCdr(result, callDataRecord);
        ConversionResult conversionResult = new ConversionResult();
        switch (mediationResolverStatus) {
            case SUCCESS:
                conversionResult.setRecordCreated(result.tojBillingMediationRecord());
                break;
            default:
                conversionResult.setErrorRecord(result.toJBillingMediationError());
                break;
        }
        sendMetric(callDataRecord);
        conversionResult.setRecordProcessed(callDataRecord);
        return conversionResult;
    }

    private static void sendMetric(CallDataRecord callDataRecord) {
        try {
            MetricsHelper.log("Readed CDR: " + callDataRecord.toString(),
                    InetAddress.getLocalHost().toString(),
                    MetricsHelper.MetricType.CDR_READ.name());
        } catch (Exception e) {}
    }
}



