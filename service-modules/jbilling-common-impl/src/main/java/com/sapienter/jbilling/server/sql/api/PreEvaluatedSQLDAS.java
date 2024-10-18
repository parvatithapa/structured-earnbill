package com.sapienter.jbilling.server.sql.api;

import java.util.Map;

import com.sapienter.jbilling.server.sql.api.QueryResultWS;

public interface PreEvaluatedSQLDAS {

    public QueryResultWS getQueryResult(String query, Map<String, Object> parameterMap, Integer limit, Integer offSet); 
}
