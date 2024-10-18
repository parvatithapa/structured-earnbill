package com.sapienter.jbilling.server.mediation.customMediations.movius;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public interface MoviusUtil {

    String MEDIATION_JOB_CONFIGURATION_BEAN_NAME = "moviusMediationJobLauncher";
    String RECORD_LINE_CONVERTER_BEAN_NAME = "moviusMediationConverter";
    String CDR_RESOLVER_BEAN_NAME = "moviusJMRProcessor";
    String JMR_DEFAULT_WRITER_BEAN = "jmrDefaultWriter";
    String RECYCLE_JOB_CONFIGURATION_BEAN_NAME = "moviusRecycleJobLauncher";
    String PHONE_UTIL = "phoneUtil";

    Cache<String, Object> mediationCache =  CacheBuilder.newBuilder()
            .concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
            .maximumSize(Long.MAX_VALUE) // maximum records can be cached
            .expireAfterAccess(10, TimeUnit.MINUTES) // cache will expire after 10 minutes of access
            .<String,Object>build();
    
    
    static void clearCache() {
        mediationCache.invalidateAll();
    }
    
    @SuppressWarnings("unchecked")
    static Map<String, String> getCompanyLevelMetaFieldValueByEntity(Integer entityId) {
        String key ="Company-Level-MetaFieldValue-EntityId-"+entityId;
        Map<String, String> cachedCompanyLevelMetaFieldValueMap = (Map<String, String>) mediationCache.getIfPresent(key); 
        if(null!= cachedCompanyLevelMetaFieldValueMap) {
            return cachedCompanyLevelMetaFieldValueMap;
        }
        MoviusHelperService service = Context.getBean(MoviusHelperService.BEAN_NAME);
        mediationCache.put(key, service.getMetaFieldsForEntity(entityId));
        return (Map<String, String>) mediationCache.getIfPresent(key);
    }
    
    String FIND_PARENT_ID_SQL = "SELECT parent_id FROM entity WHERE id = ?";
    
    static Integer getParentEntityIdForGivenEntity(Integer entityId) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        SqlRowSet rs = jdbcTemplate.queryForRowSet(FIND_PARENT_ID_SQL, entityId);
        if(rs.next()) {
            return rs.getInt("parent_id");    
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    static Map<Integer, Integer> getUserIdByOrgIdMetaField(Integer entityId, String orgId) {
        String key = String.format("EntityId-%s-User-OrgId-%s", entityId, orgId);

        Map<Integer, Integer> cacheValue = (Map<Integer, Integer>) mediationCache.getIfPresent(key);

        if(null!= cacheValue) {
            return cacheValue;
        }
        
        final String SQL = " SELECT id, currency_id "
			+ "FROM base_user "
			+ "WHERE id = "
				+ "( SELECT user_id "
					+ "FROM customer "
					+ "WHERE id = "
						+ "(SELECT customer_id "
						+ "FROM customer_meta_field_map "
						+ "WHERE meta_field_value_id = "
							+ "(SELECT id "
							+ "FROM meta_field_value "
							+ "WHERE meta_field_name_id = "
								+ "(SELECT id "
								+ "FROM meta_field_name "
								+ "WHERE name = '"+ MoviusMetaFieldName.CUSTOMER_ORG_ID.getFieldName() +"' "
								+ "AND entity_id = ?) "
							+ "AND string_value = ? )))";

        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        SqlRowSet rs = jdbcTemplate.queryForRowSet(SQL,entityId,orgId);

        if(rs.next()) {
            Map<String, Integer> result = new HashMap<>();
            result.put(MediationStepResult.USER_ID, rs.getInt("id"));
            result.put(MediationStepResult.CURRENCY_ID, rs.getInt("currency_id"));
            mediationCache.put(key, result);
            return (Map<Integer, Integer>) mediationCache.getIfPresent(key);
        }

        return Collections.emptyMap();
    }
    
    static <K,V> boolean isEmpty(Map<K, V> map) {
        return null == map || map.isEmpty();
    }
}
