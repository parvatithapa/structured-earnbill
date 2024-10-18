package com.sapienter.jbilling.server.mediation.custommediation.spc;

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
 * @author Harshad
 * @since Dec 18, 2018
 */
public interface SPCMediationUtil {

	//SPC mediation job launcher spring bean names
    String MEDIATION_JOB_LAUNCHER_BEAN_NAME = "spcMediationJobLauncher";
    String RECORD_LINE_CONVERTER_BEAN_NAME = "spcMediationConverter";
    String CDR_RESOLVER_BEAN_NAME = "spcJMRProcessor";
    String JMR_DEFAULT_WRITER_BEAN = "jmrDefaultWriter";
    String RECYCLE_JOB_LAUNCHER_BEAN_NAME = "spcRecycleJobLauncher";

    static final String FIND_USER_BY_IDENTIFIER =
            "SELECT id, currency_id FROM base_user WHERE id = "
                    + "(SELECT user_id FROM purchase_order WHERE id = "
                    + "(SELECT order_id FROM order_line WHERE id = "
                    + "(SELECT order_line_id FROM asset WHERE identifier = ? "
                    + " AND deleted = 0) "
                    + " AND deleted = 0) "
                    + " AND deleted = 0) "
                    + " AND deleted = 0 ";
    
    //Cache
    Cache<String, Object> mediationCache =  CacheBuilder.newBuilder()
            .concurrencyLevel(4) // Concurrency level 4 (Thread Safe Cache)
            .maximumSize(Long.MAX_VALUE) // maximum records can be cached
            .expireAfterAccess(10, TimeUnit.MINUTES) // cache will expire after 10 minutes of access
            .<String,Object>build();

    static void clearCache() {
        mediationCache.invalidateAll();
    }

 
    @SuppressWarnings("unchecked")
    static Map<String, Integer> getUserIdForAssetIdentifier(String identifier) {
        JdbcTemplate jdbcTemplate = Context.getBean(Name.JDBC_TEMPLATE);
        SqlRowSet rs = jdbcTemplate.queryForRowSet(FIND_USER_BY_IDENTIFIER, identifier);

        if(rs.next()) {
            Map<String, Integer> result = new HashMap<>();
            result.put(MediationStepResult.USER_ID, rs.getInt("id"));
            result.put(MediationStepResult.CURRENCY_ID, rs.getInt("currency_id"));

            return result;
        }

        return Collections.emptyMap();
    }

    static <K,V> boolean isEmpty(Map<K, V> map) {
        return null == map || map.isEmpty();
    }
}
