package com.sapienter.jbilling.batch.ignition;

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

/**
 * Created by wajeeha on 3/6/18.
 */
public class IgnitionPaymentFileCreationReader extends JdbcCursorItemReader<Integer>
        implements ItemReader<Integer>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{stepExecution.jobExecution.jobId}")
    private Long jobId;
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
        setSql(IgnitionBatchService.SQL_READ_PAYMENTS);
        setRowMapper(new RowMapper<Integer>() {
            @Override
            public Integer mapRow (ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("payment_id");
            }
        });
        ListPreparedStatementSetter listPreparedStatementSetter = new ListPreparedStatementSetter();
        listPreparedStatementSetter.setParameters(Arrays.asList(jobId, 0));
        setPreparedStatementSetter(listPreparedStatementSetter);
        setSaveState(false);
        setVerifyCursorPosition(false);
        setFetchSize(500);
        logger.debug("jobId: {}, this: {}", jobId, this);
    }

    public void setStatus (int status) {
        this.status = status;
    }
}
