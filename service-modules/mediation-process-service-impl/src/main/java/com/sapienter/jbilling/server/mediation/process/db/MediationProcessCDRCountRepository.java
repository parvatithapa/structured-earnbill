package com.sapienter.jbilling.server.mediation.process.db;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

/**
 * Created by ashwin on 23/05/18.
 */
public interface MediationProcessCDRCountRepository extends
        PagingAndSortingRepository<MediationProcessCDRCountDAO, Integer> {

    @Query(value ="SELECT call_Type FROM mediation_process_cdr_count WHERE process_id = :processId and record_status = :recordStatus" , nativeQuery = true)
    List<String> getCdrTypesForMediationProcessId(@Param("processId") UUID processId, @Param("recordStatus") String recordStatus);

    @Query(value ="SELECT * FROM mediation_process_cdr_count WHERE process_id = :processId and record_status = :recordStatus" , nativeQuery = true)
    List<MediationProcessCDRCountDAO> getCdrCountForMediationProcessAndStatus(@Param("processId") UUID processId, @Param("recordStatus") String recordStatus);
}
