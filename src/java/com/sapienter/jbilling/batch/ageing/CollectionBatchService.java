package com.sapienter.jbilling.batch.ageing;

import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.annotation.Resource;

public class CollectionBatchService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String NL = System.getProperty("line.separator");

    //@formatter:off
    private static final String SQL_INSERT_COLLECTION_USER = String.join("\n",
            "INSERT INTO collection_batch_job_data",
            "            (company_id, user_id)",
            "     VALUES (?, ?)");
    //@formatter:on

    public void createProcessData (ScrollableResults userIds, int companyId) {
        try {
            while (userIds.next()) {
                jdbcTemplate.batchUpdate(SQL_INSERT_COLLECTION_USER,
                        new AbstractInterruptibleBatchPreparedStatementSetter() {

                            @Override
                            protected boolean setValuesIfAvailable (PreparedStatement ps, int i) throws SQLException {
                                if (userIds.getRowNumber() == -1) {
                                    logger.trace("i: {}", i);
                                    return false;
                                }
                                ps.setInt(1, companyId);
                                ps.setInt(2, userIds.getInteger(0));
                                if (i < getBatchSize() - 1) {
                                    userIds.next();
                                }
                                return true;
                            }

                            @Override
                            public int getBatchSize () {
                                return 1000;
                            }
                        });
            }
        } finally {
            userIds.close();
        }
    }

    //@formatter:off
    private static final String SQL_DELETE_COLLECTION_PROCESS_DATA  = String.join("\n",
            "DELETE",
            "  FROM collection_batch_job_data",
            " WHERE company_id = ?");
    //@formatter:on

    public void cleanupProcessData (int companyId) {
        jdbcTemplate.update(SQL_DELETE_COLLECTION_PROCESS_DATA, companyId);
    }

    //@formatter:off
    private static final String SQL_COUNT_INPUT_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM collection_batch_job_data",
            " WHERE company_id = ?");
    //@formatter:on

    public int countInputUsers (int companyId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_INPUT_USERS, Integer.class, companyId);
    }

    //@formatter:off
    private static final String SQL_GET_USER_ID = String.join(NL,
            "SELECT user_id",
            "  FROM collection_batch_job_data",
            " WHERE company_id = ?",
            " ORDER BY user_id",
            "OFFSET ?",
            " LIMIT 1");
    //@formatter:on

    public int getUserAtOffset (int companyId, int offset) {
        return jdbcTemplate.queryForObject(SQL_GET_USER_ID, Integer.class, companyId, offset);
    }


    //@formatter:off
    private static final String SQL_UPDATE_PARTITION = String.join(NL,
            "UPDATE collection_batch_job_data",
            "   SET partition_num = ?",
            " WHERE company_id = ?",
            "   AND user_id BETWEEN ? AND ?");
    //@formatter:on

    public int assignPartitionNumber (int partitionNumber, int companyId, int minId, int maxId) {
        return jdbcTemplate.update(SQL_UPDATE_PARTITION, partitionNumber, companyId, minId, maxId);
    }
}
