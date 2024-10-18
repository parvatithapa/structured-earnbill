package com.sapienter.jbilling.batch.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import javax.annotation.Resource;

import org.hibernate.ScrollableResults;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.AbstractInterruptibleBatchPreparedStatementSetter;

/**
 * 
 * It has methods to perform DB operations to Email Batch operations 
 *
 * @author Abhijeet Kore
 *
 */
public class EmailBatchService {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String NL = System.getProperty("line.separator");

    //@formatter:off
    private static final String SQL_INSERT_INVOICE_EMAILS = String.join("\n",
            "INSERT INTO email_batch_job_data",
            "            (billing_process_id, invoice_id, status)",
            "     VALUES (?, ?, 0)");
    //@formatter:on

    public void createEmailProcessData (ScrollableResults invoiceIds, int billingProcessId) {
        try {
            
            while (invoiceIds.next()) {
                jdbcTemplate.batchUpdate(SQL_INSERT_INVOICE_EMAILS, new AbstractInterruptibleBatchPreparedStatementSetter() {

                    @Override
                    protected boolean setValuesIfAvailable (PreparedStatement ps, int i) throws SQLException {
                        if (invoiceIds.getRowNumber() == -1) {
                            return false;
                        }
                        ps.setInt(1, billingProcessId);
                        ps.setInt(2, (Integer) invoiceIds.get()[0]);
                        if (i < getBatchSize() - 1) {
                            invoiceIds.next();
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
            invoiceIds.close();
        }
    }

    //@formatter:off
    private static final String SQL_DELETE_BILLING_PROCESS_DATA  = String.join("\n",
            "DELETE",
            "  FROM email_batch_job_data",
            " WHERE billing_process_id = ?");
    //@formatter:on

    public void cleanupEmailJobData (int billingProcessId) {
        jdbcTemplate.update(SQL_DELETE_BILLING_PROCESS_DATA, billingProcessId);
    }

    //@formatter:off
    private static final String SQL_UPDATE_INVOICE_STATUS = String.join(NL,
            "UPDATE email_batch_job_data",
            "   SET status = ?",
            " WHERE billing_process_id = ?",
            "   AND invoice_id = ?");
    //@formatter:on

    private void updateInvoiceStatus (int billingProcessId, int invoiceId, long status) {
        jdbcTemplate.update(SQL_UPDATE_INVOICE_STATUS, status, billingProcessId, invoiceId);
    }

    public void markInvoiceAsSuccessful (int billingProcessId, int userId, long executionId) {
        updateInvoiceStatus(billingProcessId, userId, executionId);
    }

    public void markInvoiceAsFailedWithStatus (int billingProcessId, int userId, long status) {
        updateInvoiceStatus(billingProcessId, userId, status);
    }

    //@formatter:off
    private static final String SQL_COUNT_INPUT_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM email_batch_job_data",
            " WHERE billing_process_id = ?");
    //@formatter:on

    public int countInputInvoices (int billingProcessId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_INPUT_USERS, Integer.class, billingProcessId);
    }

    //@formatter:off
    public static final String SQL_COUNT_SUCCESSFUL_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM email_batch_job_data",
            " WHERE billing_process_id = ?",
            "   AND status = ?");
    //@formatter:on

    public int countSuccessfulUsers (int billingProcessId, long executionId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_SUCCESSFUL_USERS, Integer.class, billingProcessId, executionId);
    }

    //@formatter:off
    private static final String SQL_COUNT_FAILED_USERS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM email_batch_job_data",
            " WHERE billing_process_id = ?",
            "   AND status < 0");
    //@formatter:on

    public int countFailedUsers (int billingProcessId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_FAILED_USERS, Integer.class, billingProcessId);
    }

    //@formatter:off
    private static final String SQL_GET_INVOICE_ID = String.join(NL,
            "SELECT invoice_id",
            "  FROM email_batch_job_data",
            " WHERE billing_process_id = ?",
            " ORDER BY invoice_id",
            "OFFSET ?",
            " LIMIT 1");
    //@formatter:on

    public int getInvoiceAtOffset (int billingProcessId, int offset) {
        return jdbcTemplate.queryForObject(SQL_GET_INVOICE_ID, Integer.class, billingProcessId, offset);
    }

    //@formatter:off
    private static final String SQL_UPDATE_PARTITION = String.join(NL,
            "UPDATE email_batch_job_data",
            "   SET partition_num = ?",
            " WHERE billing_process_id = ?",
            "   AND invoice_id BETWEEN ? AND ?");

    public int assignPartitionNumber (int partitionNumber, int billingProcessId, int minId, int maxId) {
        return jdbcTemplate.update(SQL_UPDATE_PARTITION, partitionNumber, billingProcessId, minId, maxId);
    }
    
    
    private static final String SQL_EMAIL_JOB_ESTIMATED_COUNT = String.join(NL,
            "SELECT COUNT(*) FROM email_batch_job_data WHERE billing_process_id=?");

    public Integer getEmailJobEstimatedEmails (Integer billingProcessId) {
        return jdbcTemplate.queryForObject(SQL_EMAIL_JOB_ESTIMATED_COUNT,Integer.class, billingProcessId);
    }
    
    //@formatter:off
    public static final String SQL_EMAIL_JOB_RUNNING =
            "SELECT count(*) FROM invoice_email_process_info WHERE end_datetime is null";
    /*
     * This method checks if any email job is in process.
     * 
     * */
    public boolean isEmailJobRunning() {        
        Integer cnt = jdbcTemplate.queryForObject(
                SQL_EMAIL_JOB_RUNNING, Integer.class);
        return cnt != null && cnt > 0;
    }
    
    //@formatter:off
    private static final String LATEST_BILL_RUN_ID =
            "SELECT id "
                    + "FROM billing_process "
                    + "WHERE id = (SELECT max(id) FROM billing_process WHERE is_review = 0 AND entity_id = ?)";

    public Integer getLastBillRunId (Integer entityId) {
        return jdbcTemplate.queryForObject(LATEST_BILL_RUN_ID,Integer.class, entityId);
    }
    
    //@formatter:off
    public static final String SQL_LIST_HOLIDAYS =
            "SELECT holiday_date"+
            " FROM route_%s_%s";
    
    public List<String> getHolidayList(Integer entityId, String tableName) {
        try {
            return jdbcTemplate.queryForList(String.format(SQL_LIST_HOLIDAYS,entityId, tableName), String.class);    
        } catch ( Exception e) {
            logger.error("The Holiday list table {} does not exists ", tableName);
        }
        return Collections.emptyList();
    }
}
