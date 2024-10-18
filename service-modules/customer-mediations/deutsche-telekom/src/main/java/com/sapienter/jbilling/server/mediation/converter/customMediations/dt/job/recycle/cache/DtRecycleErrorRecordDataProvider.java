package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.job.recycle.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class DtRecycleErrorRecordDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private JdbcTemplate jdbcTemplate = null;

    public Map<String, BigDecimal> getErrorRecords() {

        Map<String, BigDecimal> result = new HashMap<>();

        jdbcTemplate.query(Queries.SQL_ERROR_RECORDS, rs -> {
            BigDecimal quantity = rs.getBigDecimal("quantity");
            if (quantity != null) {
                result.put(rs.getString("record_key"), quantity);
            }
        });

        logger.info("No. of records loaded: {}", result.size());

        return result;
    }

    public void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static class Queries {

        static final String SQL_ERROR_RECORDS =
                " SELECT jer.record_key, jeur.quantity " +
                " FROM	 jbilling_mediation_error_record jer, jm_error_usage_record jeur " +
                " WHERE	 jer.id = jeur.error_record_id " +
                " AND	 jer.record_key = jeur.record_key " +
                " AND	 jer.jbilling_entity_id = jeur.entity_id " +
                " AND	 jer.mediation_cfg_id = jeur.mediation_cfg_id " +
                " AND	 jer.status = 'TO_BE_RECYCLED' " +
                " AND	 jer.error_codes LIKE '%PROCESSED-WITH-ERROR%' ";   // this condition is critical !!
    }
}
