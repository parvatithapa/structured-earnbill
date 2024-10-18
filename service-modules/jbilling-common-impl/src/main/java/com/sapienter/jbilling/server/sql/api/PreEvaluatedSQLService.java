package com.sapienter.jbilling.server.sql.api;

import java.util.List;

import com.sapienter.jbilling.server.sql.api.QueryResultWS;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLDTO;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLParameterDTO;
import com.sapienter.jbilling.server.sql.api.db.QueryParameterWS;

public interface PreEvaluatedSQLService {

    public String BEAN_NAME = "preEvaluatedSQLService";
    public QueryResultWS getQueryResult(PreEvaluatedSQLDTO query, QueryParameterWS[] parameters, Integer limit, Integer offSet); 
    public Integer createPreEvaluatedSQL(PreEvaluatedSQLDTO dto);
    public PreEvaluatedSQLDTO getPreEvaluatedSQLByQueryCode(String queryCode);
    public List<QueryParameterWS> getParametersByQueryCode(String queryCode);
    public void createPreEvaluatedSQLParameters(List<PreEvaluatedSQLParameterDTO> parameters);
}
