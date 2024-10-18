package com.sapienter.jbilling.batch.ageing;

import java.lang.invoke.MethodHandles;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.resource.ListPreparedStatementSetter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.RowMapper;

public class AgeingProcessUserReader extends JdbcCursorItemReader<Integer> implements ItemReader<Integer>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{stepExecutionContext['partition']}")
    private Integer partitionId;
    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Override
    public void afterPropertiesSet () {

        setRowMapper(new RowMapper<Integer>() {
            @Override
            public Integer mapRow (ResultSet rs, int rowNum) throws SQLException {
                return rs.getInt("user_id");
            }
        });
        ListPreparedStatementSetter listPreparedStatementSetter = new ListPreparedStatementSetter();
        listPreparedStatementSetter.setParameters(Arrays.asList(entityId, partitionId));
        setPreparedStatementSetter(listPreparedStatementSetter);
        setSaveState(false);
        setVerifyCursorPosition(false);
        setFetchSize(500);
        logger.debug("companyId: {}, partitionId: {}, this: {}", entityId, partitionId, this);
    }


    @Override
    @SuppressWarnings("squid:S1185")
    /**
     * Parent read method was overridden intentionally to make it synchronized. Because it is used in chunk with
     * throttle-limit > 1
     */
    public synchronized Integer read () throws Exception {
        return super.read();
    }
}
