package com.sapienter.jbilling.server.sql.api.db;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLParameterDTO;

public interface PreEvaluatedSQLParameterRepository extends PagingAndSortingRepository<PreEvaluatedSQLParameterDTO, Integer> {
    
    @Query(value ="SELECT * FROM  pre_evaluated_sql_parameter presqlparm WHERE presqlparm.pre_evaluated_sql_id = (SELECT presql.id FROM  pre_evaluated_sql presql where presql.query_code = :queryCode )", nativeQuery = true)
    public List<PreEvaluatedSQLParameterDTO> getPreEvaluatedSQLParametersByQueryCode(@Param("queryCode") String queryCode);
}
