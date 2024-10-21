package com.sapienter.jbilling.server.mediation;

import com.sapienter.jbilling.server.filter.Filter;

import java.util.List;
import java.util.UUID;

/**
 * Created by andres on 19/10/15.
 */
public interface MediationProcessService {
    public static final String BEAN_NAME = "mediationProcessService";
    MediationProcess getMediationProcess(UUID id);
    MediationProcess saveMediationProcess(Integer entityId, Integer configurationId, String fileName);
    MediationProcess updateMediationProcess(MediationProcess mediationProcess);
    MediationProcess updateMediationProcessCounters(UUID mediationProcessId);
    void deleteMediationProcess(UUID mediationProcessId);
    List<MediationProcess> findLatestMediationProcess(Integer entityId, int page, int size);
    List<MediationProcess> findMediationProcessByFilters(Integer entityId, int page, int size, String sort, String order, List<Filter> filters);
    public Integer getCfgIdForMediattionProcessId(UUID mediationProcessId);
    public UUID getLastMediationProcessId(Integer entityId);
    public boolean isMediationProcessRunning(Integer entityId);
    long countMediationProcessByFilters(Integer entityId, List<Filter> filters);
    MediationProcessCDRCountInfo saveCDRCountInfo(UUID processId, String callType, Integer count, String recordStatus);
    List<String> getCdrTypesForMediationProcessId(UUID mediationProcessId, String status);
    List<MediationProcessCDRCountInfo> getCdrCountForMediationProcessAndStatus(UUID mediationProcessId, String status);
}
