package com.sapienter.jbilling.server.sql.api;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.sapienter.jbilling.server.sql.api.PreEvaluatedSQLDAS;
import com.sapienter.jbilling.server.sql.api.QueryResultWS;

public class PreEvaluatedSQLDASImpl implements PreEvaluatedSQLDAS {

	private EntityManager entityManager;

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public EntityManager getEntityManager() {
		return entityManager;
	}

	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public QueryResultWS getQueryResult(String query, Map<String, Object> parameterMap, Integer limit, Integer offSet) {

		Query dbQuery = getEntityManager().createNativeQuery(query)
				.setMaxResults(limit)
				.setFirstResult(offSet);
		for(Entry<String, Object> parameterEntry: parameterMap.entrySet()) {
			dbQuery.setParameter(parameterEntry.getKey(), parameterEntry.getValue());
		}
		
		@SuppressWarnings("unchecked")
		Object result[][] = (Object[][]) dbQuery.getResultList().toArray(new Object[0][0]);
		String [] columnNames = new String[0];
		if(result.length>0) {
			columnNames = populateColumnNames(query,parameterMap);
		}
		QueryResultWS sqlResult = new QueryResultWS(result, columnNames, result.length);
		return sqlResult; 

	} 

	private String[] populateColumnNames(String query, Map<String, Object> parameterMap) {
		return namedParameterJdbcTemplate.query(query +" limit 1", parameterMap, new ResultSetExtractor<String[]>() {

			public String[] extractData(ResultSet rs) throws SQLException, DataAccessException {
				List<String> columnNames = new ArrayList<String>();
				while(rs.next()) {
					ResultSetMetaData metaData = rs.getMetaData();
					for(int i=1;i<=metaData.getColumnCount();i++) {
						columnNames.add(metaData.getColumnName(i));
					}
				}
				return columnNames.toArray(new String[0]);
			}
		});
	}
}
