package com.sapienter.jbilling.server.mediation.process;

import com.sapienter.jbilling.server.filter.Filter;
import com.sapienter.jbilling.server.filter.FilterConstraint;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationProcessCDRCountInfo;
import com.sapienter.jbilling.server.mediation.MediationProcessService;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.process.db.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andres on 19/10/15.
 */
public class MediationProcessServiceImpl implements MediationProcessService {

    @Autowired
    private MediationProcessRepository mediationProcessRepository;

    @Autowired
    private MediationProcessDAS mediationProcessDAS;

    @Autowired
    private MediationService mediationService;

    @Autowired
    private MediationProcessCDRCountRepository cdrCountRepository;

    @Override
    public MediationProcess getMediationProcess(UUID id) {
    	if (id == null) return null;
    	MediationProcessDAO mediationProcessDAO = mediationProcessRepository.findOne(id);
    	if(null != mediationProcessDAO) {
    		MediationProcess process = DaoConverter.getMediationProcess(mediationProcessDAO);
            List<Integer> ordersForMediationProcess = mediationService.getOrdersForMediationProcess(id);
            if (ordersForMediationProcess != null)
                process.setOrderIds(ordersForMediationProcess.toArray(new Integer[0]));
    		return process;
    	}
    	return null;
    }

    @Override
    public MediationProcess saveMediationProcess(Integer entityId, Integer configurationId) {
        MediationProcess mediationProcess = new MediationProcess();
        mediationProcess.setId(UUID.randomUUID());
        mediationProcess.setEntityId(entityId);
        mediationProcess.setConfigurationId(configurationId);
        return DaoConverter.getMediationProcess(mediationProcessRepository.save(DaoConverter.getMediationProcessDAO(mediationProcess)));
    }

    @Override
    public MediationProcess updateMediationProcess(MediationProcess mediationProcess) {
        return DaoConverter.getMediationProcess(mediationProcessRepository.save(DaoConverter.getMediationProcessDAO(mediationProcess)));
    }

    @Override
    public MediationProcess updateMediationProcessCounters(UUID mediationProcessId) {
        MediationProcess mediationProcess = getMediationProcess(mediationProcessId);
        mediationProcess.setDoneAndBillable(mediationService.getMediationRecordCountByProcessIdAndStatus(mediationProcessId, JbillingMediationRecord.STATUS.PROCESSED.toString()));
        mediationProcess.setDoneAndNotBillable(mediationService.getMediationRecordCountByProcessIdAndStatus(mediationProcessId, JbillingMediationRecord.STATUS.NOT_BILLABLE.toString()));
        mediationProcess.setErrors(mediationService.getMediationErrorRecordsForProcess(mediationProcessId).size());
        mediationProcess.setDuplicates(mediationService.getMediationDuplicatesRecordsForProcess(mediationProcessId).size());
        mediationProcess.setAggregated(mediationService.getMediationRecordCountByProcessIdAndStatus(mediationProcessId, JbillingMediationRecord.STATUS.AGGREGATED.toString()));
        mediationProcess.setRecordsProcessed(mediationProcess.getDoneAndBillable() + mediationProcess.getErrors() + mediationProcess.getDoneAndNotBillable() + mediationProcess.getDuplicates());
        return updateMediationProcess(mediationProcess);
    }

    @Override
    public void deleteMediationProcess(UUID mediationProcessId) {
        mediationProcessRepository.delete(mediationProcessId);
    }

    @Override
    public List<MediationProcess> findLatestMediationProcess(Integer entityId, int page, int size) {
        return findMediationProcessByFilters(entityId, page, size, "startDate", "desc", Arrays.asList());
    }

    @Override
    public List<MediationProcess> findMediationProcessByFilters(Integer entityId, int page, int size, String sort, String order, List<Filter> filters) {
        filters = new ArrayList<>(filters);
        filters.add(Filter.integer("entityId", FilterConstraint.EQ, entityId));
        List<Filter> filtersWithoutOrderIdFilter = filters.stream().filter(f -> !f.getFieldString().equals("orderId")).collect(Collectors.toList());
        List<MediationProcess> mediationProcessFilteredWithMediationProcessFilters =
                mediationProcessDAS.findMediationProcessByFilters(page, size, sort, order, filtersWithoutOrderIdFilter).stream()
                        .map(DaoConverter::getMediationProcess).collect(Collectors.toList());
        return filterByJMROrderId(filters.stream().filter(f -> f.getFieldString().equals("orderId")).findFirst(),
                mediationProcessFilteredWithMediationProcessFilters);
    }

    @Override
    public long countMediationProcessByFilters(Integer entityId, List<Filter> filters) {
        filters = new ArrayList<>(filters);
        filters.add(Filter.integer("entityId", FilterConstraint.EQ, entityId));
        List<Filter> filtersWithoutOrderIdFilter = filters.stream().filter(f -> !f.getFieldString().equals("orderId")).collect(Collectors.toList());
        List<MediationProcess> mediationProcessFilteredWithMediationProcessFilters =
                mediationProcessDAS.findMediationProcessByFilters(0, 0, null, null, filtersWithoutOrderIdFilter).stream()
                        .map(DaoConverter::getMediationProcess).collect(Collectors.toList());

        return filterByJMROrderId(filters.stream().filter(f -> f.getFieldString().equals("orderId")).findFirst(),
                mediationProcessFilteredWithMediationProcessFilters).size();
    }

    private List<MediationProcess> filterByJMROrderId(Optional<Filter> orderIdFilterOption, final List<MediationProcess> mediationProcessToFilter) {
        if (orderIdFilterOption.isPresent()) {
            return mediationProcessToFilter.stream().filter(mediationProcess -> {
                List<JbillingMediationRecord> mediationRecordsForProcess = mediationService.getMediationRecordsForProcess(mediationProcess.getId());
                return mediationRecordsForProcess.stream().filter(jmr -> jmr.getOrderId() == Integer.parseInt(orderIdFilterOption.get().getValue().toString()))
                        .findFirst().isPresent();
            }).collect(Collectors.toList());
        }
        return mediationProcessToFilter;
    }

    @Override
    public Integer getCfgIdForMediattionProcessId(UUID mediationProcessId) {
        return mediationProcessRepository.getCfgIdForMediationProcess(mediationProcessId);
    }

    @Override
    public UUID getLastMediationProcessId(Integer entityid) {
        List<MediationProcess> mediationProcessByFilters = findMediationProcessByFilters(entityid, 0, 1, "startDate", "desc", Arrays.asList());
        if (mediationProcessByFilters.size() == 1) return mediationProcessByFilters.get(0).getId();
        return null;
    }

    @Override
    public boolean isMediationProcessRunning(Integer entityId) {
		return !mediationProcessRepository.getRunningMediationPrcoessIdForEntity(entityId).isEmpty();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, value = "jbillingMediationTransactionManager")
    public MediationProcessCDRCountInfo saveCDRCountInfo(UUID processId, String callType, Integer count, String recordStatus) {
        int id = cdrCountRepository.save(MediationProcessCDRCountDAO.of(processId, callType, count, recordStatus)).getId();
        return MediationProcessCDRCountInfo.of(id, processId, callType, count, recordStatus);
    }

    @Override
    public List<String> getCdrTypesForMediationProcessId(UUID mediationProcessId, String status) {
        return cdrCountRepository.getCdrTypesForMediationProcessId(mediationProcessId, status);
    }

    @Override
    public List<MediationProcessCDRCountInfo> getCdrCountForMediationProcessAndStatus(UUID mediationProcessId, String status) {
        List<MediationProcessCDRCountDAO> cdrCountDAOs = cdrCountRepository.
                getCdrCountForMediationProcessAndStatus(mediationProcessId, status);
        List<MediationProcessCDRCountInfo> cdrCountInfos = new ArrayList<>();
        for(MediationProcessCDRCountDAO cdrCountDao : cdrCountDAOs){
            cdrCountInfos.add(MediationProcessCDRCountInfo.of(cdrCountDao.getId(),
                    cdrCountDao.getProcessId(), cdrCountDao.getCallType(), cdrCountDao.getCount(),
                    cdrCountDao.getRecordStatus()));
        }
        return cdrCountInfos;
    }
}
