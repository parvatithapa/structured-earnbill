package com.sapienter.jbilling.server.mediation.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Partition User based on active mediated order's order line count so each stepExecution will have
 * such user which has many active mediated order lines. Tasks ensures that all slave will get engaged
 * during JMR to Order Process for better utilization of available slaves in cluster.
 * @author Krunal Bhavsar
 *
 */
public class OrderLineCountBasedUserPartitioningTask extends AbstractUserPartitioningTask {

    private static final Logger logger = LoggerFactory.getLogger(OrderLineCountBasedUserPartitioningTask.class);

    @Override
    public List<List<Integer>> doPartition(Integer batchSize, List<Integer> userIds) {
        List<List<Integer>> partitionedIds = choppedList(sortUserIdByOrderLineCount(userIds), batchSize);
        if(!partitionedIds.isEmpty() && partitionedIds.size() >= getGridSize()) {
            List<Integer> topUserWithHighOrderLineCount = partitionedIds.remove(0);
            List<Integer> newPartitionedUserListSlot = new ArrayList<>();

            while(!topUserWithHighOrderLineCount.isEmpty()) {
                for(List<Integer> partitionedIdSubList : partitionedIds) {
                    if(topUserWithHighOrderLineCount.isEmpty()) {
                        break ;
                    }
                    newPartitionedUserListSlot.add(partitionedIdSubList.remove(0));
                    partitionedIdSubList.add(topUserWithHighOrderLineCount.remove(0));
                }
            }

            partitionedIds.add(newPartitionedUserListSlot);

        }
        logger.debug("Sorted Partitioned User Ids {} ", partitionedIds);
        return partitionedIds;
    }

    private final String sortUserIdByOrderLineCount = 
            "SELECT po.user_id "
            + "FROM purchase_order as po , order_line as ol "
            + "WHERE po.id = ol.order_id "
            + "AND po.deleted = 0 "
            + "AND period_id = 1 "
            + "AND po.status_id IN "
	            + "(SELECT id "
	            + "FROM order_status "
	            + "WHERE order_status_flag = 0) "
            + "AND po.is_mediated = 't' "
            + "AND ol.deleted = 0 "
            + "AND po.user_id IN (%s) "
            + "GROUP BY 1 "
            + "ORDER BY COUNT(ol.*) DESC";
    
    @SuppressWarnings("unchecked")
    private List<Integer> sortUserIdByOrderLineCount(List<Integer> userIds) {
        if(userIds.isEmpty()) {
            return userIds;
        }
        List<Integer> dbResult = getJdbcTemplate().queryForList(String.format(sortUserIdByOrderLineCount, userIds.stream().map(String::valueOf).collect(Collectors.joining(","))), Integer.class);
        dbResult.addAll(CollectionUtils.disjunction(userIds, dbResult));
        return dbResult;
    }
}
