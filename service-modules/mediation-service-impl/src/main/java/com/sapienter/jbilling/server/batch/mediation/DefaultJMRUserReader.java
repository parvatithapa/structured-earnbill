package com.sapienter.jbilling.server.batch.mediation;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.mediation.processor.JmrProcessorConstants;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public class DefaultJMRUserReader implements ItemReader<Integer>, StepExecutionListener {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    private List<Integer> userIds;

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String commaSeparatedUsers = stepExecution.getExecutionContext().getString(JmrProcessorConstants.PARM_USER_LIST);
        userIds = Arrays.stream(commaSeparatedUsers.split(","))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toList());
        logger.debug("Collected users {}", userIds);
    }

    @Override
    public synchronized Integer read() throws Exception {
        try {
            if(CollectionUtils.isNotEmpty(userIds)) {
                Integer userId = userIds.remove(0);
                logger.debug("Reterived user {} for entity {}", userId, entityId);
                return userId;
            }
            return null;
        } catch(Exception ex) {
            logger.error("Read failed!", ex);
            throw ex;
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }

}
