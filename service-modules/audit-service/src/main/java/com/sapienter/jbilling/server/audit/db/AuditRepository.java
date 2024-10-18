package com.sapienter.jbilling.server.audit.db;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by marcomanzicore on 23/11/15.
 */
public interface AuditRepository extends PagingAndSortingRepository<AuditDAO, String> {

    @Query(value ="SELECT a.* FROM audit a WHERE a.type = :type AND a.audit_key LIKE :auditKey",
            nativeQuery = true)
    @Transactional
    List<AuditDAO> findByAuditKeyPrefix(@Param("type") String type, @Param("auditKey") String auditKey);

    @Query(value ="SELECT a FROM AuditDAO a WHERE a.type = :type AND a.auditKey = :auditKey")
    @Transactional
    List<AuditDAO> findByAuditAndAuditKey(@Param("type") String type, @Param("auditKey") String auditKey, Pageable pageable);

    @Query(value ="SELECT a.* FROM audit a WHERE a.type = :type AND a.audit_key = :auditKey " +
            "AND a.timestamp = :timestamp", nativeQuery = true)
    @Transactional
    AuditDAO findTop25ByTypeEntityIdAndTimeStamp(@Param("type") String type, @Param("auditKey") String auditKey,
                                                 @Param("timestamp") Date timestamp);

}
