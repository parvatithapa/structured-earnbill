package com.sapienter.jbilling.batch.ignition;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;

public class IgnitionBatchService {

    @Resource
    private JdbcTemplate jdbcTemplate;

    private static final String NL = System.getProperty("line.separator");

    //@formatter:off
    public static final String SQL_READ_PAYMENTS_PARTITION = String.join(NL,
            "SELECT payment_id",
            "  FROM batch_job_data_ignition",
            " WHERE job_instance_id = ?",
            "   AND partition_num = ?");
    //@formatter:on

    //@formatter:off
    public static final String SQL_READ_PAYMENTS = String.join(NL,
            "SELECT payment_id",
            "  FROM batch_job_data_ignition",
            " WHERE job_instance_id = ?",
            "   AND status = ?");
    //@formatter:on

    //@formatter:off
    private static final String SQL_INSERT_PAYMENT = String.join(NL,
            "INSERT INTO batch_job_data_ignition",
            "            (job_instance_id, payment_id, status)",
            "     VALUES (?, ?, 0)");
    //@formatter:on

    public int createJobPaymentId (long jobId, int payment_id) {
        return jdbcTemplate.update(SQL_INSERT_PAYMENT, jobId, payment_id);
    }

    //@formatter:off
    private static final String SQL_UPDATE_PAYMENT_METAFILEDS = String.join(NL,
            "UPDATE batch_job_data_ignition",
            "   SET payment_metafields = ?",
            " WHERE job_instance_id = ?",
            "   AND payment_id = ?");
    //@formatter:on

    public int persistMetafieldsData (long jobId, int payment_id, String paymentMetafields) {
        return jdbcTemplate.update(SQL_UPDATE_PAYMENT_METAFILEDS, paymentMetafields, jobId, payment_id);
    }

    //@formatter:off
    private static final String SQL_READ_PAYMENT_METAFILEDS = String.join(NL,
            "SELECT payment_metafields",
            "  FROM batch_job_data_ignition",
            " WHERE job_instance_id = ?",
            "   AND payment_id = ?");
    //@formatter:on

    public String readMetafieldsData (long jobId, int payment_id) {
        return jdbcTemplate.queryForObject(SQL_READ_PAYMENT_METAFILEDS, String.class, jobId, payment_id);
    }

    //@formatter:off
    private static final String SQL_COUNT_INPUT_PAYMENTS = String.join(NL,
            "SELECT COUNT(*)",
            "  FROM batch_job_data_ignition",
            " WHERE job_instance_id = ?");
    //@formatter:on

    public int countJobInputPayments (long jobId) {
        return jdbcTemplate.queryForObject(SQL_COUNT_INPUT_PAYMENTS, Integer.class, jobId);
    }

    //@formatter:off
    private static final String SQL_GET_PAYMENT_ID = String.join(NL,
            "SELECT payment_id",
            "  FROM batch_job_data_ignition",
            " WHERE job_instance_id = ?",
            " ORDER BY payment_id",
            "OFFSET ?",
            " LIMIT 1");
    //@formatter:on

    public int getPaymentAtOffset (long jobId, int offset) {
        return jdbcTemplate.queryForObject(SQL_GET_PAYMENT_ID, Integer.class, jobId, offset);
    }

    //@formatter:off
    private static final String SQL_UPDATE_PARTITION = String.join(NL,
            "UPDATE batch_job_data_ignition",
            "   SET partition_num = ?",
            " WHERE job_instance_id = ?",
            "   AND payment_id BETWEEN ? AND ?");
    //@formatter:on

    public int assignPartitionNumber (int partitionNumber, long jobId, int minId, int maxId) {
        return jdbcTemplate.update(SQL_UPDATE_PARTITION, partitionNumber, jobId, minId, maxId);
    }

    //@formatter:off
    private static final String SQL_DELETE_JOB_DATA  = String.join(NL,
            "DELETE",
            "  FROM batch_job_data_ignition",
            " WHERE job_instance_id = ?");
    //@formatter:on

    public void cleanupJobPaymentsIds (long jobId) {
        jdbcTemplate.update(SQL_DELETE_JOB_DATA, jobId);
    }
}
