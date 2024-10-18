package com.sapienter.jbilling.server.mediation.listener;

import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationErrorRecordDao;
import com.sapienter.jbilling.server.mediation.quantityRating.usage.RecycleMediationCacheManager;
import com.sapienter.jbilling.server.mediation.quantityRating.usage.RecycleMediationCacheProvider;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_MEDIATION_CONFIG_ID_KEY;
import static com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation.PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY;

/**
 * Created by andres on 20/10/15.
 */
public class RecycleMediationJobListener implements JobExecutionListener, RecycleMediationCacheManager {

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Autowired
    private MediationProcessService mediationProcessService;

    private Map<Integer, Set<UUID>> processIds = new HashMap<>();

    @Override
    public void beforeJob(JobExecution jobExecution) {
    	String mediationCfgId = jobExecution.getJobParameters().getString(PARAMETER_MEDIATION_CONFIG_ID_KEY);
    	String processIdForRecycle = jobExecution.getJobParameters().getString(PARAMETER_RECYCLE_MEDIATION_PROCESS_ID_KEY);

    	Integer mediationConfig = Integer.parseInt(mediationCfgId);
    	if (processIdForRecycle != null) {
    		UUID processId = UUID.fromString(processIdForRecycle);
    		jmErrorRepository.setErrorRecordsToBeRecycledForMediationAndProcessId(mediationConfig, processId);
            processIds.put(mediationConfig, new HashSet<>(Arrays.asList(processId)));
    	} else {
    		jmErrorRepository.setErrorRecordsToBeRecycledForMediationCfgId(mediationConfig);
            Set<UUID> processIdsToUpdate = jmErrorRepository
                    .getMediationErrorRecordsToBeRecycle(mediationConfig)
    				.stream().map(er -> er.getProcessId()).collect(Collectors.toSet());

    		processIds.put(mediationConfig, processIdsToUpdate);
    	}
        jobExecution.getExecutionContext().put("mediationCfgId", mediationConfig);

        Optional<RecycleMediationCacheProvider> cacheProvider = recycleMediationCacheProvider();
        cacheProvider.ifPresent(provider -> provider.init());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Integer mediationCfgId = (Integer) jobExecution.getExecutionContext().get("mediationCfgId");

        Set<UUID> processIdsToUpdate = processIds.get(mediationCfgId);
        for (UUID processId: processIdsToUpdate) {
            mediationProcessService.updateMediationProcessCounters(processId);
        }

        List<JbillingMediationErrorRecordDao> records = jmErrorRepository
                .getMediationErrorRecordsToBeRecycle(mediationCfgId, processIdsToUpdate);
        jmErrorRepository.delete(records);

        processIds.remove(mediationCfgId);

        Optional<RecycleMediationCacheProvider> cacheProvider = recycleMediationCacheProvider();
        cacheProvider.ifPresent(provider -> provider.reset());
    }
}
