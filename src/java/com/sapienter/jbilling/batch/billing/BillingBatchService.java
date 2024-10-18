package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;

import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;

public class BillingBatchService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String NL = System.getProperty("line.separator");

    //@formatter:off
    private static final String SQL_INSERT_USER = String.join("\n",
            "INSERT INTO billing_batch_job_data",
            "            (billing_process_id, user_id, status)",
            "     VALUES (?, ?, 0)");
    //@formatter:on

    public void createBillingProcessData (ScrollableResults userIds, int billingProcessId) {
        try {
            while (userIds.next()) {
                jdbcTemplate.batchUpdate(SQL_INSERT_USER, new AbstractInterruptibleBatchPreparedStatementSetter() {

                    @Override
                    protected boolean setValuesIfAvailable (PreparedStatement ps, int i) throws SQLException {
                        if (userIds.getRowNumber() == -1) {
                            logger.trace("i: {}", i);
                            return false;
                        }
                        ps.setInt(1, billingProcessId);
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
    private static final String SQL_RESET_FAILED_USERS = String.join(NL,
            "UPDATE billing_batch_job_data",
            "   SET status = 0",
            " WHERE billing_process_id = ?",
            "   AND status < 0");
    //@formatter:on

    public void resetFailedUsers (int billingProcessId) {
        jdbcTemplate.update(SQL_RESET_FAILED_USERS, billingProcessId);
    }

    //@formatter:off
    private static final String SQL_DELETE_BILLING_PROCESS_DATA  = String.join("\n",
            "DELETE",
            "  FROM billing_batch_job_data",
            " WHERE billing_process_id = ?");
    //@formatter:on

    public void cleanupBillingProcessData (int billingProcessId) {
        jdbcTemplate.update(SQL_DELETE_BILLING_PROCESS_DATA, billingProcessId);
    }

    //@formatter:off
    private static final String SQL_UPDATE_USER_STATUS = String.join(NL,
            "UPDATE billing_batch_job_data",
            "   SET status = ?",
            " WHERE billing_process_id = ?",
            "   AND user_id = ?");
    //@formatter:on

    private void updateUserStatus (int billingProcessId, int userId, long status) {
        jdbcTemplate.update(SQL_UPDATE_USER_STATUS, status, billingProcessId, userId);
    }

    public void markUserAsSuccessful (int billingProcessId, int userId, long executionId) {
        updateUserStatus(billingProcessId, userId, executionId);
    }

    public void markUserAsFailedWithStatus (int billingProcessId, int userId, long status) {
        updateUserStatus(billingProcessId, userId, status);
    }

    //@formatter:off
    private static final String SQL_COUNT_INPUT_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM billing_batch_job_data",
            " WHERE billing_process_id = ?");
    //@formatter:on

    public int countInputUsers (int billingProcessId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_INPUT_USERS, Integer.class, billingProcessId);
    }

    //@formatter:off
    public static final String SQL_COUNT_SUCCESSFUL_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM billing_batch_job_data",
            " WHERE billing_process_id = ?",
            "   AND status = ?");
    //@formatter:on

    public int countSuccessfulUsers (int billingProcessId, long executionId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_SUCCESSFUL_USERS, Integer.class, billingProcessId, executionId);
    }

    //@formatter:off
    private static final String SQL_COUNT_FAILED_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM billing_batch_job_data",
            " WHERE billing_process_id = ?",
            "   AND status < 0");
    //@formatter:on

    public int countFailedUsers (int billingProcessId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_FAILED_USERS, Integer.class, billingProcessId);
    }

    //@formatter:off
    private static final String SQL_GET_USER_ID = String.join(NL,
            "SELECT user_id",
            "  FROM billing_batch_job_data",
            " WHERE billing_process_id = ?",
            " ORDER BY user_id",
            "OFFSET ?",
            " LIMIT 1");
    //@formatter:on

    public int getUserAtOffset (int billingProcessId, int offset) {
        return jdbcTemplate.queryForObject(SQL_GET_USER_ID, Integer.class, billingProcessId, offset);
    }

    //@formatter:off
    public static final String SQL_LIST_FAILED_USERS = String.join(NL,
            "SELECT user_id",
            "  FROM billing_batch_job_data",
            " WHERE billing_process_id = ?",
            "   AND status < 0");
    //@formatter:on

    public List<Integer> listFailedUsers (int billingProcessId) {
        return jdbcTemplate.queryForList(SQL_LIST_FAILED_USERS, Integer.class, billingProcessId);
    }

    //@formatter:off
    private static final String SQL_UPDATE_PARTITION = String.join(NL,
            "UPDATE billing_batch_job_data",
            "   SET partition_num = ?",
            " WHERE billing_process_id = ?",
            "   AND user_id BETWEEN ? AND ?");
    //@formatter:on

    public int assignPartitionNumber (int partitionNumber, int billingProcessId, int minId, int maxId) {
        return jdbcTemplate.update(SQL_UPDATE_PARTITION, partitionNumber, billingProcessId, minId, maxId);
    }
    
    //@formatter:off
    public static final String SQL_ESTIMATED_FAILED_USERS_COUNT = String.join(NL,
            "SELECT COUNT(c.user_id) FROM customer c, billing_batch_job_data b ",
            "WHERE c.user_id = b.user_id AND c.invoice_delivery_method_id IN (1,3) ",
            "AND b.billing_process_id = ? AND b.status < 0");

    //@formatter:on
    public Integer getEstimatedFailedUsersCount (int billingProcessId) {
        return jdbcTemplate.queryForObject(SQL_ESTIMATED_FAILED_USERS_COUNT, Integer.class, billingProcessId);
    }

}
