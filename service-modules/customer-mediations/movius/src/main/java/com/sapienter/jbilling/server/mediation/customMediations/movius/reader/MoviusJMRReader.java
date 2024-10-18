package com.sapienter.jbilling.server.mediation.customMediations.movius.reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;
import com.sapienter.jbilling.server.mediation.processor.JmrProcessorConstants;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class MoviusJMRReader implements ItemReader<Integer>, JmrProcessorConstants, StepExecutionListener {

    
    private List<Integer> userIdList = new ArrayList<> ();
    
    @Override
    public synchronized Integer read() throws Exception {
        while(!userIdList.isEmpty()) {
            return userIdList.remove(0);
        }
        return null;
    }
    
    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        return stepExecution.getExitStatus();
    }


    @Override
    public void beforeStep(StepExecution stepExecution) {
        String commaSeparatedUsers = stepExecution.getExecutionContext().getString(PARM_USER_LIST);
        userIdList = Arrays.stream(commaSeparatedUsers.split(","))
                           .filter(s -> !s.isEmpty())
                           .map(Integer::parseInt)
                           .collect(Collectors.toList());
    }

}
