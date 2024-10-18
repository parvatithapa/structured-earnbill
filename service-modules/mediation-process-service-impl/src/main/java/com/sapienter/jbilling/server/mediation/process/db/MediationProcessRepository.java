package com.sapienter.jbilling.server.mediation.process.db;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Created by andres on 19/10/15.
 */
public interface MediationProcessRepository extends
        PagingAndSortingRepository<MediationProcessDAO, UUID>{

    @Query(value ="SELECT configuration_id FROM jbilling_mediation_process WHERE id = :mediationProcessId" , nativeQuery = true)
    int getCfgIdForMediationProcess(@Param("mediationProcessId") UUID mediationProcessId);

    @Query(value ="SELECT id FROM jbilling_mediation_process WHERE entity_id = :entityId and end_date is null" , nativeQuery = true)
    List<Integer> getRunningMediationPrcoessIdForEntity(@Param("entityId") Integer entityId);
}
