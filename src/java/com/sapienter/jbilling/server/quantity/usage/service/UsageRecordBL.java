package com.sapienter.jbilling.server.quantity.usage.service;

import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.repository.UsageRecordDAS;

import java.util.Date;


public class UsageRecordBL {

    private final UsageRecordDAS das;

    public UsageRecordBL() {
        this.das = new UsageRecordDAS();
    }


    public IUsageRecord getItemUsage(Integer itemId, Integer userId, Integer entityId,
                                     Date startDate, Date endDate,
                                     String mediationProcessId) {

        return this.das.getItemUsage(itemId, userId, entityId, startDate,
                endDate, mediationProcessId);
    }


    public IUsageRecord getItemResourceUsage(Integer itemId, Integer userId, Integer entityId,
                                             String itemResourceId, Date startDate, Date endDate,
                                             String mediationProcessId) {

        return this.das.getItemResourceUsage(itemId, userId, entityId,
                itemResourceId, startDate, endDate, mediationProcessId);
    }
}
