package com.sapienter.jbilling.server.mediation.evaluation.task;

import java.util.Date;

import com.sapienter.jbilling.server.order.db.OrderDTO;

/**
 *
 * @author Swapnil
 *
 */

public interface IMediationEvaluationStrategyTask {

    Integer getCurrent(Integer userId, Date eventDate, Integer itemId, String mediationProcessId, OrderDTO order, String assetIdentifier);
}
