package com.sapienter.jbilling.batch.support;

import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PartitionService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String NL = System.getProperty("line.separator");

    //@formatter:off
    public static final String SQL_READ_USERS_PARTITION = String.join(NL,
            "SELECT user_id",
            "  FROM batch_job_user_ids",
            " WHERE job_instance_id = ?",
            "   AND partition_num = ?",
            "   AND status = ?");
    //@formatter:on

    //@formatter:off
    private static final String SQL_INSERT_USER = String.join(NL,
            "INSERT INTO batch_job_user_ids",
            "            (job_instance_id, user_id, status)",
            "     VALUES (?, ?, 0)");
    //@formatter:on

    private static int BATCH_SIZE = 1000;

    public void createJobUsersIds (long jobId, ScrollableResults userIds) {
        try {
            while (userIds.next()) {
                jdbcTemplate.batchUpdate(SQL_INSERT_USER, new AbstractInterruptibleBatchPreparedStatementSetter() {

                    @Override
                    protected boolean setValuesIfAvailable (PreparedStatement ps, int i) throws SQLException {
                        if (userIds.getRowNumber() == -1) {
                            logger.trace("i: {}", i);
                            return false;
                        }
                        ps.setLong(1, jobId);
                        //Note: Getting user Id from current row
                        ps.setInt(2, (Integer) userIds.get()[0]);
                        if (i < getBatchSize() - 1) {
                            userIds.next();
                        }
                        return true;
                    }

                    @Override
                    public int getBatchSize () {
                        return BATCH_SIZE;
                    }
                });
            }
        } finally {
            userIds.close();
        }
    }

    //@formatter:off
    private static final String SQL_DELETE_JOB_DATA  = String.join(NL,
            "DELETE",
            "  FROM batch_job_user_ids",
            " WHERE job_instance_id = ?");
    //@formatter:on

    public void cleanupJobUsersIds (long jobId) {
        jdbcTemplate.update(SQL_DELETE_JOB_DATA, jobId);
    }

    //@formatter:off
    private static final String SQL_COUNT_INPUT_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM batch_job_user_ids",
            " WHERE job_instance_id = ?");
    //@formatter:on

    public int countJobInputUsers (long jobId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_INPUT_USERS, Integer.class, jobId);
    }

    //@formatter:off
    private static final String SQL_GET_USER_ID = String.join(NL,
            "SELECT user_id",
            "  FROM batch_job_user_ids",
            " WHERE job_instance_id = ?",
            " ORDER BY user_id",
            "OFFSET ?",
            " LIMIT 1");
    //@formatter:on

    public int getUserAtOffset (long jobId, int offset) {
        return jdbcTemplate.queryForObject(SQL_GET_USER_ID, Integer.class, jobId, offset);
    }

    //@formatter:off
    private static final String SQL_UPDATE_PARTITION = String.join(NL,
            "UPDATE batch_job_user_ids",
            "   SET partition_num = ?",
            " WHERE job_instance_id = ?",
            "   AND user_id BETWEEN ? AND ?");
    //@formatter:on

    public int assignPartitionNumber (int partitionNumber, long jobId, int minId, int maxId) {
        return jdbcTemplate.update(SQL_UPDATE_PARTITION, partitionNumber, jobId, minId, maxId);
    }

    //@formatter:off
    private static final String SQL_UPDATE_USER_STATUS = String.join(NL,
            "UPDATE batch_job_user_ids",
            "   SET status = ?",
            " WHERE job_instance_id = ?",
            "   AND user_id = ?");
    //@formatter:on

    private void updateUserStatus (long jobId, int userId, long status) {
        jdbcTemplate.update(SQL_UPDATE_USER_STATUS, status, jobId, userId);
    }

    public void markUserAsFailedWithStatus (long jobId, int userId, long status) {
        updateUserStatus(jobId, userId, status);
    }

    public void markUserAsSuccessful (long jobId, int userId, long status) {
        updateUserStatus(jobId, userId, status);
    }
}
