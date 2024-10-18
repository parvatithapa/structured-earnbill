package com.sapienter.jbilling.server.item.event;

import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by Taimoor Choudhary on 1/11/18.
 */
public class BulkUploadEvent implements Event{

    public enum UploadType{
        DEFAULT_PRODUCT,
        ACCOUNT_LEVEL_PRICE,
        CUSTOMER_PRICE,
        PLAN_PRICE
    }

    String sourceFilePath;
    String errorFilePath;
    Integer entityId;
    Integer callerId;
    Long executionId;
    UploadType eventType;

    public BulkUploadEvent(String sourceFilePath, String errorFilePath, Integer entityId, Integer callerId, UploadType eventType) {
        this.sourceFilePath = sourceFilePath;
        this.errorFilePath = errorFilePath;
        this.entityId = entityId;
        this.callerId = callerId;
        this.eventType = eventType;
    }

    @Override
    public String getName() {
        return "Bulk Upload Event";
    }

    @Override
    public Integer getEntityId() {
        return this.entityId;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public String getErrorFilePath() {
        return this.errorFilePath;
    }

    public Integer getCallerId() {
        return this.callerId;
    }

    public Long getExecutionId() {
        return this.executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public UploadType getEventType() {
        return eventType;
    }
}
