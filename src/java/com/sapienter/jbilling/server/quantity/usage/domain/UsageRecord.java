package com.sapienter.jbilling.server.quantity.usage.domain;

import java.math.BigDecimal;
import java.util.Date;


public class UsageRecord implements IUsageRecord {

    public static final UsageRecord ZERO = builder().quantity(BigDecimal.ZERO).build();

    private Integer itemId;
    private Integer userId;
    private String resourceId;
    private BigDecimal quantity;
    private Date startDate;
    private Date endDate;

    private UsageRecord() {}

    public static UsageRecord withNewQuantity(IUsageRecord usageRecord, BigDecimal quantity) {
        return builder().user(usageRecord.getUserId())
                .item(usageRecord.getItemId())
                .resource(usageRecord.getResourceId())
                .startDate(usageRecord.getStartDate())
                .endDate(usageRecord.getEndDate())
                .quantity(quantity)
                .build();
    }

    public static UsageRecordBuilder builder() {
        return new UsageRecordBuilder();
    }

    @Override
    public Integer getItemId() {
        return this.itemId;
    }

    @Override
    public Integer getUserId() {
        return this.userId;
    }

    @Override
    public String getResourceId() {
        return resourceId;
    }

    @Override
    public BigDecimal getQuantity() {
        return this.quantity;
    }

    @Override
    public Date getStartDate() {
        return this.startDate;
    }

    @Override
    public Date getEndDate() {
        return this.endDate;
    }

    @Override
    public String toString() {

        return new StringBuilder().append("UsageRecord { ")
          .append("itemId=" )
          .append(itemId)
          .append(", userId=")
          .append(userId)
          .append(", resourceId=")
          .append(resourceId)
          .append(", quantity=")
          .append(quantity)
          .append(" }")
          .toString();

    }

    public static class UsageRecordBuilder {

        private UsageRecord managedInstance = new UsageRecord();

        public UsageRecordBuilder item(Integer itemId) {
            managedInstance.itemId = itemId;
            return this;
        }

        public UsageRecordBuilder user(Integer userId) {
            managedInstance.userId = userId;
            return this;
        }

        public UsageRecordBuilder resource(String resourceId) {
            managedInstance.resourceId = resourceId;
            return this;
        }

        public UsageRecordBuilder quantity(BigDecimal quantity) {
            managedInstance.quantity = quantity;
            return this;
        }

        public UsageRecordBuilder startDate(Date startDate) {
            managedInstance.startDate = startDate;
            return this;
        }

        public UsageRecordBuilder endDate(Date endDate) {
            managedInstance.endDate = endDate;
            return this;
        }

        public UsageRecord build() {
            return managedInstance;
        }
    }
}
