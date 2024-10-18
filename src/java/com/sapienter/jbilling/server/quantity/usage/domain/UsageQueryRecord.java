package com.sapienter.jbilling.server.quantity.usage.domain;

import java.util.Date;


public class UsageQueryRecord implements IUsageQueryRecord {

    private Integer itemId;
    private Integer userId;
    private Integer entityId;
    private Date    startDate;
    private Date    endDate;
    private String  resourceId;
    private String  mediationProcessId;

    private UsageQueryRecord() {}

    public static UsageQueryRecordBuilder builder() {
        return new UsageQueryRecordBuilder();
    }

    @Override
    public Integer getItemId() {
        return itemId;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getMediationProcessId() {
        return this.mediationProcessId;
    }

    public static class UsageQueryRecordBuilder {

        UsageQueryRecord managedInstance = new UsageQueryRecord();

        public UsageQueryRecordBuilder item(Integer itemId) {
            managedInstance.itemId = itemId;
            return this;
        }

        public UsageQueryRecordBuilder user(Integer userId) {
            managedInstance.userId = userId;
            return this;
        }

        public UsageQueryRecordBuilder entity(Integer entityId) {
            managedInstance.entityId = entityId;
            return this;
        }

        public UsageQueryRecordBuilder resource(String resourceId) {
            managedInstance.resourceId = resourceId;
            return this;
        }

        public UsageQueryRecordBuilder startDate(Date startDate) {
            managedInstance.startDate = startDate;
            return this;
        }

        public UsageQueryRecordBuilder endDate(Date endDate) {
            managedInstance.endDate = endDate;
            return this;
        }

        public UsageQueryRecordBuilder mediationProcessId(String mediationProcessId) {
            managedInstance.mediationProcessId = mediationProcessId;
            return this;
        }

        public UsageQueryRecord build() {
            return managedInstance;
        }
    }

    @Override
    public String toString() {
        return new StringBuilder().append("UsageQueryRecord { ")
          .append("itemId=")
          .append(itemId)
          .append(", entityId=")
          .append(entityId)
          .append(", userId=")
          .append(userId)
          .append(", startDate=")
          .append(startDate)
          .append(", endDate=")
          .append(endDate)
          .append(", mediationProcessId=")
          .append(mediationProcessId)
          .append(" }")
          .toString();
    }
}
