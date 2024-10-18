package com.sapienter.jbilling.batch.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.resource.ListPreparedStatementSetter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

public class PartitionedItemReader extends JdbcCursorItemReader<Integer>
        implements ItemReader<Integer>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{stepExecution.jobExecution.jobId}")
    private Long jobId;
    @Value("#{stepExecutionContext['partition']}")
    private Integer partitionId;
    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;
    @Value("#{stepExecution.stepName}")
    private String stepName;
//    @Value("#{status}")
    private int status;

    @Override
    @SuppressWarnings("squid:S1185")
    /**
     * Parent read method was overridden intentionally to make it synchronized. Because it is used in chunk with
     * throttle-limit > 1
     */
    public synchronized Integer read () throws Exception {
        return super.read();
    }

    @Override
    public void afterPropertiesSet () {
        logger.debug("stepName: {}, jobExecutionId: {}", stepName, jobExecutionId);

        setSql(PartitionService.SQL_READ_USERS_PARTITION);
        setRowMapper(new RowMapper<Integer>() {
            @Override
            public Integer mapRow (ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("user_id");
            }
        });
        ListPreparedStatementSetter listPreparedStatementSetter = new ListPreparedStatementSetter();
        listPreparedStatementSetter.setParameters(Arrays.asList(jobId, partitionId, status));

        setPreparedStatementSetter(listPreparedStatementSetter);
        setSaveState(false);
        setVerifyCursorPosition(false);
        setFetchSize(500);
        logger.debug("jobId: {}, partitionId: {}, this: {}", jobId, partitionId, this);
    }

    public void setStatus (int status) {
        this.status = status;
    }
}
