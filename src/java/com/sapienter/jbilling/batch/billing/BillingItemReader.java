package com.sapienter.jbilling.batch.billing;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.resource.ListPreparedStatementSetter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.batch.BatchConstants;

@SuppressWarnings("squid:S110")
public class BillingItemReader extends JdbcCursorItemReader<Integer> implements ItemReader<Integer>, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobExecutionContext['billingProcessId']}")
    private Integer billingProcessId;
    @Value("#{stepExecutionContext['partition']}")
    private Integer partitionId;
    @Value("#{stepExecution.jobExecutionId}")
    private Long jobExecutionId;
    @Value("#{stepExecution.stepName}")
    private String stepName;

    @Override
    public void afterPropertiesSet () throws Exception {
        logger.debug("stepName: {}, jobExecutionId: {}", stepName, jobExecutionId);

        setRowMapper((rs, rowNum) ->  rs.getInt("user_id"));

        ListPreparedStatementSetter listPreparedStatementSetter = new ListPreparedStatementSetter();
        String stepFirstName =  stepName.split(":")[0];

        if(BatchConstants.GENERATE_INVOICE_STEP_NAME.equals(stepFirstName)) {
            listPreparedStatementSetter.setParameters(Arrays.asList(billingProcessId, partitionId));
        } else if(BatchConstants.EMAIL_AND_PAYMENT_STEP_NAME.equals(stepFirstName)) {
            listPreparedStatementSetter.setParameters(Arrays.asList(billingProcessId, partitionId, jobExecutionId));
        }

        setPreparedStatementSetter(listPreparedStatementSetter);
        setSaveState(false);
        setVerifyCursorPosition(false);
        setFetchSize(500);
        logger.debug("billingProcessId: {}, partitionId: {}, this: {}", billingProcessId, partitionId, this);
        super.afterPropertiesSet();
    }

    @Override
    @SuppressWarnings("squid:S1185")
    /**
     * Parent read method was overridden intentionally to make it synchronized. Because it is used in chunk with
     * throttle-limit > 1
     */
    public synchronized Integer read () throws Exception {
        Integer userId = super.read();
        logger.trace("Billing process ID: {}, User ID: {} +++ Returned read()", billingProcessId, userId);
        return userId;
    }
}