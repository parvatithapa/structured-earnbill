package com.sapienter.jbilling.server.integration.common.job.readers;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemReader;

import com.sapienter.jbilling.server.integration.ChargeType;
import com.sapienter.jbilling.server.integration.Constants;
import com.sapienter.jbilling.server.integration.common.service.HelperDataAccessService;

public class MeteredUsagePartitionedReader implements ItemReader<Integer>, StepExecutionListener {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Getter
  @Setter
  private HelperDataAccessService dataAccessService;
  private Iterator<Integer> userIds;

  private Integer currentPartition;
  private Integer totalPartitions;
  private int activeOrderStatusId;
  private Date lastMediationRunDate = new Date();

  @Override
  public synchronized Integer read() {
    while (userIds.hasNext()) {
      return userIds.next();
    }
    return null;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    return stepExecution.getExitStatus();
  }

  @Override
  public void beforeStep(StepExecution stepExecution) {
    String entityId = stepExecution.getJobParameters().getString(Constants.ENTITY_ID);
    if (currentPartition == null) {
      currentPartition = stepExecution.getExecutionContext().getInt(Constants.PARM_CURRENT_PARTITION);
    }

    if (totalPartitions == null) {
      totalPartitions = stepExecution.getExecutionContext().getInt(Constants.PARM_NUMBER_OF_PARTITIONS);
    }

    activeOrderStatusId = stepExecution.getJobParameters().getLong(Constants.ORDER_ACTIVE_STATUS_ID).intValue();
    String type = stepExecution.getJobParameters().getString(Constants.CHARGE_TYPE);
    ChargeType chargeType = ChargeType.valueOf(type);

    if (chargeType.equals(ChargeType.USAGE)) {
     lastMediationRunDate = (Date) stepExecution.getJobParameters().getDate(Constants.LAST_SUCCESS_MEDIATION_RUN_DATE);
    }


    List<Integer> users = new LinkedList<>();
    users.addAll(getUsersByChargeType(Integer.parseInt(entityId), activeOrderStatusId, totalPartitions, currentPartition, chargeType, lastMediationRunDate));

    userIds = users.iterator();
    logger.debug("partition={}, numOfUsers={}", currentPartition, users.size());
    String listString = users.stream().map(Object::toString)
      .collect(Collectors.joining(", "));
    logger.debug("users={}", listString);
  }

  private List<Integer> getUsersByChargeType(Integer entityId, int activeOrderStatusId, int totalPartitions, int currentPartition, ChargeType chargeType, Date lastMediationRunDate) {
    List<Integer> users = new ArrayList<>();
    switch (chargeType) {
      case USAGE:
        users = dataAccessService.getUsersWithMediatedOrderAndPartition(entityId, activeOrderStatusId, lastMediationRunDate, totalPartitions, currentPartition);
        break;
        
      case RESERVED_MONTHLY_PREPAID:
        users = dataAccessService.getUsersWithReservedMonthlyPlans(entityId, activeOrderStatusId, totalPartitions, currentPartition);
        break;
    }
    return users;
  }
}
