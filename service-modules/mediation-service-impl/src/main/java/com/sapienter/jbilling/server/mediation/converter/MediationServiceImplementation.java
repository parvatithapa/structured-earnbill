package com.sapienter.jbilling.server.mediation.converter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationContext;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationProcessStatus;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.converter.common.MediationJob;
import com.sapienter.jbilling.server.mediation.converter.common.reader.DistributelMediationReader;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepositoryDAS;
import com.sapienter.jbilling.server.validator.mediation.InvalidJobParameterException;
import com.sapienter.jbilling.server.validator.mediation.MediationJobParameterValidator;


/**
 * Created by marcolin on 08/10/15.
 */
public class MediationServiceImplementation implements MediationService, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PARAMETER_MEDIATION_PROCESS_ID_KEY = "mediationProcessId";
    public static final String PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY = "recycleProcessId";
    public static final String PARAMETER_MEDIATION_FILE_PATH_KEY = "filePath";
    public static final String PARAMETER_MEDIATION_CONFIG_ID_KEY = "mediationCfgId";
    private static final String DISTRIBUTEL_MEDIATION_JOB = "distributelMediationJob";
    private static final String DISTRIBUTEL_MEDIATION_READER = "distributelMediationReader";
    public static final String PARAMETER_MEDIATION_ENTITY_ID_KEY = "entityId";
    public static final String PARAMETER_MEDIATION_ORDER_SERVICE_BEAN_NAME_KEY = "orderServiceBeanName";
    public static final String PARAMETER_MEDIATION_JOB_NAME_KEY = "jobName";

    private ApplicationContext applicationContext;

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private JMRRepositoryDAS jmrRepositoryDAS;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Override
    public boolean isMediationProcessRunning() {
        JobExplorer jobExplorer = (JobExplorer) applicationContext.getBean("mediationJobExplorer");
        Set<JobExecution> runningJobExecutions = getMediationJobExecutions(jobExplorer);
        return !runningJobExecutions.isEmpty();

    }

    private Set<JobExecution> getMediationJobExecutions(JobExplorer jobExplorer) {
        for (MediationJob mediationJob : MediationJobs.getJobs()){
            Set<JobExecution> runningJobExecutions = jobExplorer.findRunningJobExecutions(mediationJob.getJob());
            if (!runningJobExecutions.isEmpty()) {
                return runningJobExecutions;
            }
        }
        return Collections.<JobExecution>emptySet();
    }

    @Override
    public MediationProcessStatus mediationProcessStatus() {
        JobExplorer jobExplorer = (JobExplorer) applicationContext.getBean("mediationJobExplorer");
        Set<JobExecution> runningJobExecutions = getMediationJobExecutions(jobExplorer);
        if(!runningJobExecutions.isEmpty()) {
            JobExecution inProgress = runningJobExecutions.iterator().next();
            return MediationProcessStatus.valueOf(inProgress.getStatus().name());
        }
        return MediationProcessStatus.COMPLETED;
    }

    @Override
    @Transactional(value = Transactional.TxType.NOT_SUPPORTED)
    public void launchMediation(Integer entityId, Integer mediationCfgId, String jobName) {
        launchMediation(entityId, mediationCfgId, jobName, null);
    }

    @Override
    public void launchMediation(Integer entityId, Integer mediationCfgId, String jobName, File file) {
        MediationContext mediationContext = new MediationContext();
        mediationContext.setEntityId(entityId);
        mediationContext.setMediationCfgId(mediationCfgId);
        mediationContext.setJobName(jobName);
        mediationContext.setFileWithCdrs(file);
        launchMediation(mediationContext);
    }

    @Override
    public String launchMediationForCdr(Integer entityId, Integer mediationCfgId, String jobName, String records) {

        try {
            Path path = Files.createTempFile("records", ".tmp");
            path = Files.write(path, records.getBytes(Charset.defaultCharset()));

            return triggerMediationJobLauncherByConfiguration(entityId, mediationCfgId,
                    jobName, path.toFile()).toString();

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public List<JbillingMediationRecord> launchMediation(MediationContext mediationContext) {
        validateMediationContext(mediationContext);
        Optional<JobParameters> jobParameters = createParameters(mediationContext.getEntityId(), mediationContext.getMediationCfgId(),
                mediationContext.getJobName(), parameters -> {
                    if (mediationContext.getFileWithCdrs() != null){
                        parameters.put(PARAMETER_MEDIATION_FILE_PATH_KEY, new JobParameter("" + mediationContext.getFileWithCdrs().getPath()));
                    }
                    if (mediationContext.getProcessIdForMediation() != null) {
                        parameters.put(PARAMETER_MEDIATION_PROCESS_ID_KEY, new JobParameter("" + mediationContext.getProcessIdForMediation()));
                    }
                });
        if(jobParameters.isPresent()) {
            logger.debug("Triggering meidation job {} for entity {} with parameters {}", mediationContext.getJobName(),
                    mediationContext.getEntityId(), jobParameters);
            triggerMediation(mediationContext.getJobName(), jobParameters.get());
        }
        UUID mediationProcessId =  mediationContext.getProcessIdForMediation();
        if (mediationProcessId == null) {
            return Collections.emptyList();
        } else {
            MediationProcessService mediationProcessService = (MediationProcessService)applicationContext.getBean(MediationProcessService.BEAN_NAME);
            mediationProcessId = mediationProcessService.getLastMediationProcessId(mediationContext.getEntityId());
            return getMediationRecordsForProcess(mediationProcessId);
        }
    }

    private void validateMediationContext(MediationContext mediationContext) {
        if (mediationContext.getEntityId() == null ||
                mediationContext.getMediationCfgId() == null ||
                mediationContext.getJobName() == null)  {
            throw new IllegalArgumentException("A mediation needs a entity id, a configuration id and a job name to start. " +
                    "entityId:" + mediationContext.getEntityId() + "," +
                    "mediationConfigId:" + mediationContext.getMediationCfgId() + "," +
                    "jobName:" + mediationContext.getJobName());
        }

        validateDistributelMediationContext(mediationContext);
    }

    @Override
    public void processCdr(Integer entityId, Integer mediationCfgId, String jobName, String records) {
        try {
            File recordsFile = File.createTempFile("records", ".tmp");
            PrintWriter printWriter = new PrintWriter(recordsFile);
            printWriter.println(records);
            printWriter.close();
            launchMediation(entityId, mediationCfgId, jobName, recordsFile);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }


    private Optional<JobParameters> createParameters(Integer entityId, Integer mediationCfgId, String jobName,
            Consumer<Map<String, JobParameter>> parameterConsumer) {
        MediationJob mediationJob = MediationJobs.getJobForName(jobName);
        if (mediationJob != null) {
            Map<String, JobParameter> parametersMap = new HashMap<>();
            parametersMap.put(PARAMETER_MEDIATION_JOB_NAME_KEY, new JobParameter(jobName));
            parametersMap.put("datetime", new JobParameter(new Date()));
            parametersMap.put(PARAMETER_MEDIATION_ENTITY_ID_KEY, new JobParameter("" + entityId));
            parametersMap.put(PARAMETER_MEDIATION_CONFIG_ID_KEY, new JobParameter("" + mediationCfgId));
            parametersMap.put("isGlobal", new JobParameter(1L));
            parametersMap.put(PARAMETER_MEDIATION_ORDER_SERVICE_BEAN_NAME_KEY, new JobParameter(mediationJob.getOrderServiceBeanName()));
            parameterConsumer.accept(parametersMap);

            JobParameters jobParameters = new JobParameters(parametersMap);
            MediationJobParameterValidator parameterValidator = mediationJob.getParameterValidator();
            parameterValidator.validate(jobParameters);
            return Optional.of(jobParameters);
        }
        return Optional.empty();
    }

    private void triggerMediation(String jobName, JobParameters jobParameters) {
        try {
            MediationJob mediationJob = MediationJobs.getJobForName(jobName);
            JobLauncher jobLauncher = (JobLauncher) applicationContext.getBean("mediationJobLauncher");
            jobLauncher.run(getJob(mediationJob.getJob()), jobParameters);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public List<JbillingMediationErrorRecord> findMediationErrorRecordsByFilters(Integer page, Integer size, List<Filter> filters) {
        return jmrRepositoryDAS.findMediationErrorRecordsByFilters(page, size, filters).stream()
                .map(DaoConverter::getMediationErrorRecord).collect(Collectors.toList());
    }

    @Override
    public List<JbillingMediationErrorRecord> findMediationDuplicateRecordsByFilters(Integer page, Integer size, List<Filter> filters) {
        return jmrRepositoryDAS.findMediationDuplicateRecordsByFilters(page, size, filters).stream()
                .map(DaoConverter::getMediationErrorRecord).collect(Collectors.toList());
    }

    @Override
    public List<JbillingMediationRecord> findMediationRecordsByFilters(Integer page, Integer size, List<Filter> filters) {
        return jmrRepositoryDAS.findMediationRecordsByFilters(page, size, filters).stream()
                .map(DaoConverter::getMediationRecord).collect(Collectors.toList());
    }

    @Override
    public List<String> getCdrTypes(List<Filter> filters) {
        return jmrRepositoryDAS.getCdrTypes(filters).stream()
                .collect(Collectors.toList());
    }

    @Override
    public Long countMediationRecordsByFilters(List<Filter> filters) {
        return jmrRepositoryDAS.countMediationRecordsByFilters(filters);
    }

    @Override
    public Long countMediationErrorsByFilters(List<Filter> filters) {
        return jmrRepositoryDAS.countMediationErrorsByFilters(filters);
    }

    @Override
    public List<JbillingMediationRecord> getMediationRecordsForMediationConfigId(Integer mediationCfgId) {
        return jmrRepository.getMediationRecordsForConfigId(mediationCfgId).stream().map(DaoConverter::getMediationRecord)
                .collect(Collectors.toList());
    }

    private Job getJob(String name) {
        return (Job) applicationContext.getBean(name);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<JbillingMediationRecord> getMediationRecordsForProcess(UUID processId) {
        return jmrRepository.getMediationRecordsForProcess(processId).stream().map(DaoConverter::getMediationRecord)
                .collect(Collectors.toList());
    }

    @Override
    public Integer getMediationRecordCountByProcessIdAndStatus(UUID processId, String status) {
        return jmrRepository.getMediationRecordCountByProcessIdAndStatus(processId, status);
    }

    @Override
    public List<JbillingMediationErrorRecord> getMediationErrorRecordsForMediationConfigId(Integer mediationCfgId) {
        return jmErrorRepository.getMediationErrorRecordsForMediationConfigId(mediationCfgId).stream()
                .map(DaoConverter::getMediationErrorRecord).collect(Collectors.toList());
    }

    @Override
    public List<JbillingMediationErrorRecord> getMediationErrorRecordsForProcess(UUID processId) {
        return jmErrorRepository.getMediationErrorRecordsForProcess(processId).stream()
                .map(DaoConverter::getMediationErrorRecord).collect(Collectors.toList());
    }

    @Override
    public int getMediationErrorRecordCountForProcess(UUID processId) {
        return jmErrorRepository.getMediationErrorRecordCountForProcess(processId);
    }

    @Override
    public List<JbillingMediationErrorRecord> getMediationDuplicatesRecordsForProcess(UUID processId) {
        return jmErrorRepository.getMediationDuplicateRecordsForProcess(processId).stream()
                .map(DaoConverter::getMediationErrorRecord).collect(Collectors.toList());
    }

    @Override
    public int getMediationDuplicatesRecordCountForProcess(UUID processId) {
        return jmErrorRepository.getMediationDuplicateRecordCountForProcess(processId);
    }

    @Override
    public List<Integer> getOrdersForMediationProcess(UUID processId) {
        return jmrRepository.getOrdersForMediationProcess(processId);
    }

    @Override
    public int getOrderCountForMediationProcess(UUID processId) {
        return jmrRepository.getOrderCountForMediationProcess(processId);
    }

    @Override
    public List<JbillingMediationRecord> getMediationRecordsForOrder(Integer orderId) {
        return jmrRepository.getMediationRecordsForOrderId(orderId).stream().map(DaoConverter::getMediationRecord)
                .collect(Collectors.toList());
    }

    @Override
    public List<JbillingMediationRecord> getMediationRecordsForProcessAndOrder(UUID processId, Integer orderId) {
        return jmrRepository.getMediationRecordsForProcessIdOrderId(processId, orderId).stream()
                .map(DaoConverter::getMediationRecord).collect(Collectors.toList());
    }

    @Override
    public List<JbillingMediationRecord> getMediationRecordsForOrderLine(Integer orderLineId) {
        return jmrRepository.getMediationRecordsForOrderLineId(orderLineId);
    }

    @Override
    public void deleteErrorMediationRecords(UUID processId) {
        jmErrorRepository.delete(jmErrorRepository.getMediationErrorRecordsForProcess(processId));
    }

    @Override
    public void deleteDuplicateMediationRecords(UUID processId) {
        jmErrorRepository.delete(jmErrorRepository.getMediationDuplicateRecordsForProcess(processId));
    }

    @Override
    public void deleteMediationRecords(List<JbillingMediationRecord> recordList) {
        jmrRepository.delete(recordList.stream().map(DaoConverter::getMediationRecordDao).collect(Collectors.toList()));
    }

    @Override
    public void recycleCdr(Integer entityId, Integer mediationCfgId, String jobName) {
        String recycleJobName = MediationJobs.getRecycleJobForMediationJob(jobName).getJob();
        Optional<JobParameters> jobParameters = createParameters(entityId, mediationCfgId, recycleJobName, parameters ->
        parameters.put(PARAMETER_MEDIATION_CONFIG_ID_KEY, new JobParameter("" + mediationCfgId)));
        if(jobParameters.isPresent()) {
            triggerMediation(recycleJobName, jobParameters.get());
        }
    }

    @Override
    public void recycleCdr(Integer entityId, Integer mediationCfgId, String jobName, UUID processId) {
        String recycleJobName = MediationJobs.getRecycleJobForMediationJob(jobName).getJob();
        Optional<JobParameters> jobParameters = createParameters(entityId, mediationCfgId, recycleJobName, parameters -> {
            parameters.put(PARAMETER_MEDIATION_CONFIG_ID_KEY, new JobParameter("" + mediationCfgId));
            parameters.put(PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY, new JobParameter("" + processId));
        });
        if(jobParameters.isPresent()) {
            triggerMediation(recycleJobName, jobParameters.get());
        }
    }

    @Override
    public void saveDiameterEventAsJMR(JbillingMediationRecord diameterEvent) {
        jmrRepository.save(DaoConverter.getMediationRecordDao(diameterEvent));
    }

    @Override
    @Transactional(value = Transactional.TxType.NOT_SUPPORTED)
    public UUID triggerMediationJobLauncherByConfiguration(Integer entityId, Integer mediationCfgId, String jobName, File file) {
        MediationProcessService mediationProcessService = (MediationProcessService)applicationContext.getBean(MediationProcessService.BEAN_NAME);
        final MediationContext mediationContext = new MediationContext();
        mediationContext.setEntityId(entityId);
        mediationContext.setMediationCfgId(mediationCfgId);
        mediationContext.setJobName(jobName);
        mediationContext.setFileWithCdrs(file);
        validateMediationContext(mediationContext);
        UUID mediationProcessId = mediationProcessService.saveMediationProcess(entityId, mediationCfgId).getId();
        logger.debug("mediation process id [{}] created for entity[{}] for file [{}]", mediationProcessId, entityId, file.getName());
        logger.debug("size of file [{}] is [{}] for mediation process id [{}]", file.getName(),
                FileUtils.byteCountToDisplaySize(FileUtils.sizeOf(file)), mediationProcessId);
        mediationContext.setProcessIdForMediation(mediationProcessId);
        try {
            Optional<JobParameters> jobParameters = createParameters(mediationContext.getEntityId(), mediationContext.getMediationCfgId(),
                    mediationContext.getJobName(), parameters -> {
                        if (mediationContext.getFileWithCdrs() != null){
                            parameters.put(PARAMETER_MEDIATION_FILE_PATH_KEY, new JobParameter("" + mediationContext.getFileWithCdrs().getPath()));
                        }
                        if (mediationContext.getProcessIdForMediation() != null) {
                            parameters.put(PARAMETER_MEDIATION_PROCESS_ID_KEY, new JobParameter("" + mediationContext.getProcessIdForMediation().toString()));
                        }
                    });
            if(jobParameters.isPresent()) {
                new Thread(()-> {
                    logger.debug("trigger mediation job [{}] for entity [{}] with parameters [{}]", jobName, entityId, jobParameters);
                    triggerMediation(mediationContext.getJobName(), jobParameters.get());
                }).start();
            } else {
                logger.debug("parameter not found for mediation job [{}] for entity[{}]", jobName, entityId);
                updateEndDate(mediationProcessId);
            }
        } catch(InvalidJobParameterException ex) {
            updateEndDate(mediationProcessId);
            throw ex;
        }
        return mediationProcessId;
    }

    @Override
    @Transactional(value = Transactional.TxType.NOT_SUPPORTED)
    public UUID triggerRecycleCdrAsync(final Integer entityId, final Integer mediationCfgId, final String jobName, final UUID processId) {
        MediationProcessService mediationProcessService = (MediationProcessService)applicationContext.getBean(MediationProcessService.BEAN_NAME);
        final UUID mediationProcessId = mediationProcessService.saveMediationProcess(entityId, mediationCfgId).getId();
        try {
            String recycleJobName = MediationJobs.getRecycleJobForMediationJob(jobName).getJob();
            Optional<JobParameters> jobParameters = createParameters(entityId, mediationCfgId, recycleJobName , parameters -> {
                parameters.put(PARAMETER_MEDIATION_CONFIG_ID_KEY, new JobParameter("" + mediationCfgId));
                parameters.put(PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY, new JobParameter(null!=processId ? processId.toString() : null));
                parameters.put(PARAMETER_MEDIATION_PROCESS_ID_KEY, new JobParameter(mediationProcessId.toString()));
            });
            if(jobParameters.isPresent()) {
                logger.debug("Triggering meidation job {} for entity {} with parameters {}", jobName, entityId, jobParameters);
                new Thread(() -> triggerMediation(recycleJobName, jobParameters.get())).start();
            } else {
                logger.warn("Parameter not found!");
                updateEndDate(mediationProcessId);
            }
        } catch(InvalidJobParameterException ex) {
            logger.error("Parameter not valid!", ex);
            updateEndDate(mediationProcessId);
            throw ex;
        } catch (Exception ex) {
            logger.error("recycle trigger failed!", ex);
            updateEndDate(mediationProcessId);
            throw ex;
        }
        return mediationProcessId;
    }

    @Override
    public Integer getMediationErrorRecordCountForMediationConfigId(Integer mediationCfgId) {
        return jmErrorRepository.getMediationErrorRecordCountForMediationConfigId(mediationCfgId);
    }

    private void validateDistributelMediationContext(MediationContext context) {
        if(DISTRIBUTEL_MEDIATION_JOB.equals(context.getJobName())){
            DistributelMediationReader distributelMediationReader = (DistributelMediationReader)applicationContext.getBean(DISTRIBUTEL_MEDIATION_READER);
            if(!distributelMediationReader.hasValidExtension(context.getFileWithCdrs())){
                throw new IllegalArgumentException("It has invalid extension.");
            }
        }
    }

    @Override
    public List<Integer> getOrdersForMediationProcessByStatusExcluded(UUID processId, Integer excludedOrderStatus) {
        return jmrRepository.getOrdersForMediationProcessByStatusExcluded(processId,excludedOrderStatus);
    }

    private void updateEndDate(UUID processId) {
        MediationProcessService mediationProcessService = (MediationProcessService) applicationContext.getBean(MediationProcessService.BEAN_NAME);
        MediationProcess mediationProcess = mediationProcessService.getMediationProcess(processId);
        if(null == mediationProcess.getStartDate()) {
            mediationProcess.setStartDate(new Date());
            logger.debug("updating start date {} of mediation process id {}", processId, mediationProcess.getStartDate());
        }
        logger.debug("updating end date {} of mediation process id {}", processId, mediationProcess.getEndDate());
        mediationProcess.setEndDate(new Date());
        mediationProcessService.updateMediationProcess(mediationProcess);
    }

    @Override
    public List<JbillingMediationRecord> getUnBilledMediationEventsByUser(Integer userId, int offset, int limit) {
        return jmrRepository.getUnBilledMediationEventsByUser(userId, offset, limit)
                .stream()
                .map(DaoConverter::getMediationRecord)
                .collect(Collectors.toList());
    }
}