package com.sapienter.jbilling.server.sql.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLDAS;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLParameterRepository;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLRepository;
import com.sapienter.jbilling.server.sql.api.QueryResultWS;
import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLService;
import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLValidator;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLDTO;
import com.sapienter.jbilling.server.sql.api.db.PreEvaluatedSQLParameterDTO;
import com.sapienter.jbilling.server.sql.api.db.QueryParameterWS;
import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLServiceImpl;

public class PreEvaluatedSQLServiceImpl implements PreEvaluatedSQLService {
	
	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PreEvaluatedSQLServiceImpl.class));
	
	@Autowired
	private PreEvaluatedSQLRepository preEvaluatedSQLRepository;

	@Autowired 
	private PreEvaluatedSQLDAS preEvaluatedSQLDAS;

	@Autowired
	private PreEvaluatedSQLParameterRepository preEvaluatedSQLParameterRepository;

	@Override
	public QueryResultWS getQueryResult(PreEvaluatedSQLDTO query, QueryParameterWS[] parameters, Integer limit, Integer offSet) {
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		for(QueryParameterWS parameter: parameters) {
			parameterMap.put(parameter.getParameterName(), parameter.getParameterType().getValue(parameter.getParameterValue()));
		}
		StopWatch watch = new StopWatch();
		watch.start();
		QueryResultWS result = preEvaluatedSQLDAS.getQueryResult(query.getSqlQuery(), parameterMap, limit, offSet);
		watch.stop();
		result.setExecutionTime(watch.getTotalTimeMillis());
		LOG.debug("Time Taken By Query Code: "+ query.getQueryCode() +" In Seconds is -> "+watch.getTotalTimeSeconds());
		return result;
	}

	@Override
	@Transactional(rollbackFor={Exception.class})
	public Integer createPreEvaluatedSQL(PreEvaluatedSQLDTO dto) {
		return preEvaluatedSQLRepository.save(dto).getId();
	}

	@Override
	public PreEvaluatedSQLDTO getPreEvaluatedSQLByQueryCode(String queryCode) {
		return preEvaluatedSQLRepository.getPreEvaluatedSQLByQueryCode(queryCode);
	}

	@Override
	public List<QueryParameterWS> getParametersByQueryCode(String queryCode) {
		List<QueryParameterWS> parameters = new ArrayList<QueryParameterWS>();
		for(PreEvaluatedSQLParameterDTO parameter: preEvaluatedSQLParameterRepository.getPreEvaluatedSQLParametersByQueryCode(queryCode)) {
			parameters.add(new QueryParameterWS(parameter.getParameterName(), parameter.getParameterType()));
		}
		return parameters;
	}

	@Override
	public void createPreEvaluatedSQLParameters(List<PreEvaluatedSQLParameterDTO> parameters) {
		for(PreEvaluatedSQLParameterDTO parameter: parameters) {
			preEvaluatedSQLParameterRepository.save(parameter);
		}
	}

}
