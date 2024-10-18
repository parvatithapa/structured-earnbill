package com.sapienter.jbilling.server.mediation;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.sapienter.jbilling.server.filter.Filter;

/**
 * Created by marcolin on 08/10/15.
 */
public interface MediationService {
    static final String BEAN_NAME = "mediationService";

    boolean isMediationProcessRunning();
    public MediationProcessStatus  mediationProcessStatus();

    void launchMediation(Integer entityId, Integer mediationCfgId, String jobName);
    void launchMediation(Integer entityId, Integer mediationCfgId, String jobName, File file);
    String launchMediationForCdr(Integer entityId, Integer mediationCfgId, String jobName, String records);
    public UUID triggerMediationJobLauncherByConfiguration(Integer entityId, Integer mediationCfgId, String jobName, File file);
    List<JbillingMediationRecord> launchMediation(MediationContext mediationContext);
    void deleteErrorMediationRecords(UUID processId);
    void deleteDuplicateMediationRecords(UUID processId);
    void deleteMediationRecords(List<JbillingMediationRecord> recordList);
    void processCdr(Integer entityId, Integer mediationCfgId, String jobName, String record);

    List<JbillingMediationErrorRecord> findMediationErrorRecordsByFilters(Integer page, Integer size, List<Filter> filters);
    List<JbillingMediationErrorRecord> findMediationDuplicateRecordsByFilters(Integer page, Integer size, List<Filter> filters);
    List<JbillingMediationErrorRecord> getMediationErrorRecordsForMediationConfigId(Integer mediationCfgId);
    List<JbillingMediationErrorRecord> getMediationErrorRecordsForProcess(UUID processId);
    int getMediationErrorRecordCountForProcess(UUID processId);
    List<JbillingMediationErrorRecord> getMediationDuplicatesRecordsForProcess(UUID processId);
    int getMediationDuplicatesRecordCountForProcess(UUID processId);

    List<Integer> getOrdersForMediationProcess(UUID processId);
    default List<Integer> getOrdersForMediationProcessByStatusExcluded(UUID processId, Integer excludedOrderStatus){
        return Collections.emptyList();
    }
    int getOrderCountForMediationProcess(UUID processId);
    List<JbillingMediationRecord> findMediationRecordsByFilters(Integer page, Integer size, List<Filter> filters);
    List<JbillingMediationRecord> getMediationRecordsForMediationConfigId(Integer mediationCfgId);
    List<JbillingMediationRecord> getMediationRecordsForOrder(Integer orderId);
    List<JbillingMediationRecord> getMediationRecordsForProcess(UUID processId);
    List<JbillingMediationRecord> getMediationRecordsForProcessAndOrder(UUID processId, Integer orderId);
    List<JbillingMediationRecord> getMediationRecordsForOrderLine(Integer orderLineId);
    Long countMediationRecordsByFilters(List<Filter> filters);
    Long countMediationErrorsByFilters(List<Filter> filters);

    List<String> getCdrTypes(List<Filter> filters);
    /**
     * Returns the count of mediation records filtered by processId and status.
     *
     * @param processId
     * @param status
     * @return An Integer value for mediation records count
     */
    Integer getMediationRecordCountByProcessIdAndStatus(UUID processId, String status);

    void recycleCdr(Integer entityId, Integer mediationCfgId, String jobName);
    void recycleCdr(Integer entityId, Integer mediationCfgId, String jobName, UUID processId);
    UUID triggerRecycleCdrAsync(Integer entityId, Integer mediationCfgId, String jobName, UUID processId);
    Integer getMediationErrorRecordCountForMediationConfigId(Integer mediationCfgId);

    //TODO: USED FOR DIAMETER, THIS CAN GO IN A DIAMETER TABLE INSTEAD OF BE SAVED HERE
    void saveDiameterEventAsJMR(JbillingMediationRecord diameterEvent);

    List<JbillingMediationRecord> getUnBilledMediationEventsByUser(Integer userId, int offset, int limit);
}
