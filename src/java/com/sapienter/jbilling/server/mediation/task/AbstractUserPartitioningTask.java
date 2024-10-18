package com.sapienter.jbilling.server.mediation.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;

/**
 *
 * @author Krunal Bhavsar
 *
 */
public abstract class AbstractUserPartitioningTask extends PluggableTask implements IMediationPartitionTask {

    public List<List<Integer>> choppedList(List<Integer> userIds, Integer batchSize) {
        return partition(userIds, batchSize);
    }
    
    /**
     * 
     * @param list
     * @param batchSize
     * @return
     */
    private List<List<Integer>> partition(List<Integer> list, int batchSize) {
        
        if(CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        
        List<List<Integer>> parts = new ArrayList<List<Integer>>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            parts.add(new ArrayList<Integer>(
                    list.subList(i, Math.min(size, i + batchSize)))
                    );
        }
        return parts;
    }

    /**
     * returns grid size from environment variable if not set then returns 10
     * @return
     */
    public int getGridSize() {
        String value = System.getenv("JBILLING_BATCH_GRID_SIZE");
        return (null!= value && !value.isEmpty() ) ? Integer.parseInt(value) : 10;
    }

    public JdbcTemplate getJdbcTemplate() {
        return Context.getBean(Name.JDBC_TEMPLATE);
    }
}
