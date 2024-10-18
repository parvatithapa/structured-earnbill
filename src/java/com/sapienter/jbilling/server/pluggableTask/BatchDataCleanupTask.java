package com.sapienter.jbilling.server.pluggableTask;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * The job will delete old entries of COMPLETED jobs from the spring batch tables. It will either delete all jobs
 * older than a configured number of days or only jobs with certain names.
 * It can be configured with:
 *  - age_days : Only delete jobs older
 *  - job* : Any parameter name starting with job indicates a job name that must be deleted.
 *
 * @author Gerhard Maree
 * @since 17/05/2013
 */
public class BatchDataCleanupTask extends AbstractCronTask {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BatchDataCleanupTask.class));

    private static final String SQL_JOB_EXECUTION_SUB_QUERY =
            "(select e1.job_execution_id from batch_job_execution e1 where e1.job_execution_id in (" +
            "select p1.job_execution_id from batch_job_execution_params p1 where p1.key_name='entityId' and p1.string_val=? ) and e1.create_time < ? %SQL_JOB_NAME_SUBQUERY%) ";

    private static final String SQL_JOB_NAME_SUBQUERY = " and %ALIAS%job_instance_id in " +
            "( select e2.job_instance_id from batch_job_instance e2 where e2.job_name in (%NAME_PARM%)) ";

    private static final String SQL_STEP_EXECUTION_CONTEXT = "delete from batch_step_execution_context where step_execution_id in " +
            "(select e3.step_execution_id from batch_step_execution e3 where e3.job_execution_id in " +
                SQL_JOB_EXECUTION_SUB_QUERY +" )";

    private static final String SQL_STEP_EXECUTION = "delete from batch_step_execution where job_execution_id in " + SQL_JOB_EXECUTION_SUB_QUERY ;

    private static final String SQL_BATCH_EXECUTION_CONTEXT = "delete from batch_job_execution_context where job_execution_id in " + SQL_JOB_EXECUTION_SUB_QUERY ;

    private static final String SQL_BATCH_EXECUTION_PARAMS = "delete from batch_job_execution_params where job_execution_id in " + SQL_JOB_EXECUTION_SUB_QUERY ;

    private static final String SQL_JOB_INSTANCE_IDS =
            "select e1.job_instance_id from batch_job_execution e1 where e1.job_execution_id in (" +
                    "select p1.job_execution_id from batch_job_execution_params p1 where p1.key_name='entityId' and p1.string_val=? ) and e1.create_time < ? %SQL_JOB_NAME_SUBQUERY%";


    private static final String SQL_BATCH_EXECUTION = "delete from batch_job_execution where job_instance_id = ?";

    private static final String SQL_BATCH_INSTANCE = "delete from batch_job_instance where job_instance_id = ?";

    private static final String PARAM_AGE_DAYS_NAME = "age_days";
    private static final String PARAM_JOB_NAME_PREFIX = "job";

    protected static final ParameterDescription PARAM_AGE_DAYS =
            new ParameterDescription(PARAM_AGE_DAYS_NAME , false, ParameterDescription.Type.FLOAT);
    //initializer for pluggable params
    {
        descriptions.add(PARAM_AGE_DAYS);
    }

    public BatchDataCleanupTask() {

    }

    @Override
    public String getTaskName() {
        return "DataCleanupTask: , entity id " + getEntityId() + ", taskId " + getTaskId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        try {
            LOG.debug("DataCleanupTask started for entity %s", getEntityId());
            _init(context);

            float ageDays = Float.parseFloat(getParameter(PARAM_AGE_DAYS_NAME, "1"));
            Timestamp toDate = new Timestamp(System.currentTimeMillis() - (long)(ageDays * 1000 * 60 * 60 * 24));

            //construct the sql IN clause
            List<String> jobNames = extractJobNames();
            StringJoiner sj = new StringJoiner(",","","");
            for(String jobName : jobNames) {
                sj.add("?");
            }
            String jobNamesParamSql = sj.toString();

            //query parameters
            List<Object> queryParamsEntityDateNamesList = new ArrayList<>();
            queryParamsEntityDateNamesList.add(getEntityId());
            queryParamsEntityDateNamesList.add(toDate);
            queryParamsEntityDateNamesList.addAll(jobNames);
            Object[] queryParamsEntityDateNames = queryParamsEntityDateNamesList.toArray();

            //query parameter types
            int[] jobNamesSqlTypes = new int[jobNames.size()];
            int[] queryParamTypesEntityDateNames = new int[jobNames.size() + 2];
            queryParamTypesEntityDateNames[0] = Types.VARCHAR;
            queryParamTypesEntityDateNames[1] = Types.TIMESTAMP;
            for(int count =0; count<jobNames.size(); count++) {
                queryParamTypesEntityDateNames[count+2] = Types.VARCHAR;
                jobNamesSqlTypes[count] = Types.VARCHAR;
            }

            JdbcTemplate jdbcTemplate = Context.getBean(Context.Name.JDBC_TEMPLATE);

            String sql = buildSqlQuery(SQL_STEP_EXECUTION_CONTEXT, "e1.", jobNamesParamSql);
            LOG.debug("SQL_STEP_EXECUTION_CONTEXT %s", sql);
            jdbcTemplate.update(sql, queryParamsEntityDateNames, queryParamTypesEntityDateNames);

            sql = buildSqlQuery(SQL_STEP_EXECUTION, "e1.", jobNamesParamSql);
            LOG.debug("SQL_STEP_EXECUTION %s", sql);
            jdbcTemplate.update(sql, queryParamsEntityDateNames, queryParamTypesEntityDateNames);

            sql = buildSqlQuery(SQL_BATCH_EXECUTION_CONTEXT, "e1.", jobNamesParamSql);
            LOG.debug("SQL_BATCH_EXECUTION_CONTEXT %s", sql);
            jdbcTemplate.update(sql, queryParamsEntityDateNames, queryParamTypesEntityDateNames);

            sql = buildSqlQuery(SQL_JOB_INSTANCE_IDS, "e1.", jobNamesParamSql);
            LOG.debug("SQL_JOB_INSTANCE_IDS %s", sql);
            List<Integer> jobExecutionIds = jdbcTemplate.queryForList(sql, queryParamsEntityDateNames, queryParamTypesEntityDateNames, Integer.class);
            List<Object[]> executionIdParams = jobExecutionIds.stream().map(i -> new Object[] {i}).collect(Collectors.toList());

            sql = buildSqlQuery(SQL_BATCH_EXECUTION_PARAMS, "e1.", jobNamesParamSql);
            LOG.debug("SQL_BATCH_EXECUTION_PARAMS %s", sql);
            jdbcTemplate.update(sql, queryParamsEntityDateNames, queryParamTypesEntityDateNames);

            LOG.debug("SQL_BATCH_EXECUTION %s", sql);
            jdbcTemplate.batchUpdate(SQL_BATCH_EXECUTION, executionIdParams, new int[]{Types.INTEGER});

            LOG.debug("SQL_BATCH_INSTANCE %s", SQL_BATCH_INSTANCE);
            jdbcTemplate.batchUpdate(SQL_BATCH_INSTANCE, executionIdParams, new int[]{Types.INTEGER});

        } catch (Exception e) {
            LOG.error("Unable to delete old Spring Batch entries", e);
        }
        LOG.debug("DataCleanupTask ... finished");

    }

    private String buildSqlQuery(String sql, String alias, String jobNamesParamSql) {
        if(jobNamesParamSql.isEmpty()) {
            sql = sql.replace("%SQL_JOB_NAME_SUBQUERY%", "");
        } else {
            sql = sql.replace("%SQL_JOB_NAME_SUBQUERY%", SQL_JOB_NAME_SUBQUERY)
                    .replace("%NAME_PARM%", jobNamesParamSql)
                    .replace("%ALIAS%", alias);
        }
        return sql;
    }

    /**
     * Extract the list of job names that must be deleted from the configured parameters.
     *
     * @return the list of job names or an empty list if none are configured.
     */
    private List<String> extractJobNames() {
        List<String> jobNames = new ArrayList<>();
        for(Map.Entry<String, String> entry : parameters.entrySet()) {
            if(entry.getKey().startsWith(PARAM_JOB_NAME_PREFIX)) {
                jobNames.add(entry.getValue());
            }
        }
        return jobNames;
    }
}
