package com.sapienter.jbilling.server.sql.api.db;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLDTO;

public interface PreEvaluatedSQLRepository  extends PagingAndSortingRepository<PreEvaluatedSQLDTO, Integer> {

    @Query(value ="SELECT * FROM  pre_evaluated_sql presql WHERE presql.query_code = :queryCode", nativeQuery = true)
    public PreEvaluatedSQLDTO getPreEvaluatedSQLByQueryCode(@Param("queryCode") String queryCode);
    
}
