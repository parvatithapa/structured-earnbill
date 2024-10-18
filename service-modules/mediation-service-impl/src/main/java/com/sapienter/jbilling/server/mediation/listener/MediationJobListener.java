package com.sapienter.jbilling.server.mediation.listener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.FilterConstraint;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;

/**
 * Created by andres on 20/10/15.
 */
public class MediationJobListener implements JobExecutionListener {

    private static final Logger LOG = LoggerFactory.getLogger(MediationJobListener.class);

    @Autowired
    private MediationProcessService mediationProcessService;

    @Autowired
    private MediationService mediationService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String mediationProcessString = jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
        UUID mediationProcessId = null;
        if (mediationProcessString != null) {
            mediationProcessId = UUID.fromString(mediationProcessString);
        }
        if (mediationProcessId == null) {
            int entityId = Integer.parseInt(jobExecution.getJobParameters().getString("entityId"));
            int configurationId = Integer.parseInt(jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_CONFIG_ID_KEY));
            mediationProcessId = mediationProcessService.saveMediationProcess(entityId, configurationId, null).getId();
        }

        MediationProcess mediationProcess = mediationProcessService.getMediationProcess(mediationProcessId);
        mediationProcess.setStartDate(jobExecution.getStartTime());
        String fileName  = jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_NAME);
        if(StringUtils.isBlank(fileName)) {
            String filePath  = jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
            if(StringUtils.isNotEmpty(filePath)) {
                fileName = new File(filePath).getName(); //When mediation fun from the controller
            }
        }
        mediationProcess.setFileName(StringUtils.isNotBlank(fileName) ? fileName : mediationProcess.getStartDate() + " - " + mediationProcess.getEndDate());
        mediationProcessService.updateMediationProcess(mediationProcess);
        jobExecution.getExecutionContext().put(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY, mediationProcessId);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        UUID mediationProcessId = (UUID) jobExecution.getExecutionContext().get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
        MediationProcess mediationProcess = mediationProcessService.getMediationProcess(mediationProcessId);
        mediationProcess.setStartDate(jobExecution.getStartTime());
        mediationProcess.setEndDate(jobExecution.getEndTime());

        mediationProcess.setDoneAndBillable(mediationService.getMediationRecordCountByProcessIdAndStatus(mediationProcessId, JbillingMediationRecord.STATUS.PROCESSED.toString()));
        mediationProcess.setDoneAndNotBillable(mediationService.getMediationRecordCountByProcessIdAndStatus(mediationProcessId, JbillingMediationRecord.STATUS.NOT_BILLABLE.toString()));
        mediationProcess.setAggregated(mediationService.getMediationRecordCountByProcessIdAndStatus(mediationProcessId, JbillingMediationRecord.STATUS.AGGREGATED.toString()));

        mediationProcess.setErrors(mediationService.getMediationErrorRecordCountForProcess(mediationProcessId));
        mediationProcess.setDuplicates(mediationService.getMediationDuplicatesRecordCountForProcess(mediationProcessId));

        mediationProcess.setRecordsProcessed(mediationProcess.getDoneAndBillable() + mediationProcess.getErrors() +
                mediationProcess.getAggregated() +
                mediationProcess.getDuplicates() + mediationProcess.getDoneAndNotBillable());
        mediationProcess.setOrderAffectedCount(mediationService.getOrderCountForMediationProcess(mediationProcessId));
        mediationProcessService.updateMediationProcess(mediationProcess);
        updateCdrCount(mediationProcess, JbillingMediationRecordDao.STATUS.PROCESSED);
        updateCdrCount(mediationProcess, JbillingMediationRecordDao.STATUS.NOT_BILLABLE);
        String filePath  = jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_FILE_PATH_KEY);
        if(StringUtils.isNotEmpty(filePath)) {
            LOG.debug("File path for mediation ProcessId is {}", filePath);
            String fileName = FilenameUtils.getName(filePath);
            if(StringUtils.isNotEmpty(fileName)) {
                LOG.debug("MediationProcess Id {} has processed {} file", mediationProcessId, fileName);
            }
        } else {
            LOG.warn("File Name not found for MediationProcess Id {}", mediationProcessId);
        }
        LOG.debug("Mediation Process count info for Id {} is {}", mediationProcessId, mediationProcess);
        if(mediationProcess.getErrors()!=0) {
            LOG.debug("Mediation Process has error records for mediation process id {}. Number of errors is {}", mediationProcessId, mediationProcess.getErrors());
        }
    }

    private void updateCdrCount(MediationProcess mediationProcess, JbillingMediationRecordDao.STATUS status) {
        LOG.debug("Updating CDR Count for Mediation Process Id {} for status {}", mediationProcess.getId(),status);
        List<String> cdrTypes = mediationService.getCdrTypes(
                getFilters(mediationProcess.getEntityId(), mediationProcess.getId(), status, null));
        for(String cdrType : cdrTypes) {
            Integer count = mediationService.countMediationRecordsByFilters(
                    getFilters(mediationProcess.getEntityId(), mediationProcess.getId(), status, cdrType)).intValue();
            mediationProcessService.saveCDRCountInfo(mediationProcess.getId(), cdrType, count, status.name());
        }
    }

    private List<Filter> getFilters(Integer entityId, UUID processId, JbillingMediationRecordDao.STATUS status, String cdrType){
        List<Filter> filters = new ArrayList<>();
        filters.add(Filter.integer("jBillingCompanyId", FilterConstraint.EQ, entityId));
        filters.add(Filter.uuid("processId", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, processId));
        filters.add(Filter.enumFilter("status", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, status));
        filters.add(Filter.string("cdrType", com.sapienter.jbilling.server.filter.FilterConstraint.EQ, cdrType));
        return filters;
    }
}