package com.sapienter.jbilling.server.item.event;

import com.sapienter.jbilling.server.system.event.Event;

/**
 * Created by Taimoor Choudhary on 6/12/18.
 */
public class BulkDownloadEvent implements Event {

    public enum DownloadType{
        DEFAULT_PRODUCT,
        ACCOUNT_LEVEL_PRICE,
        CUSTOMER_PRICE,
        PLANS
    }

    String sourceFilePath;
    String errorFilePath;
    String identificationCode;
    Integer entityId;
    Integer callerId;
    Long executionId;
    DownloadType eventType;

    public BulkDownloadEvent(String sourceFilePath, String errorFilePath, Integer entityId, Integer callerId, DownloadType eventType) {
        this.sourceFilePath = sourceFilePath;
        this.errorFilePath = errorFilePath;
        this.entityId = entityId;
        this.callerId = callerId;
        this.eventType = eventType;
    }

    public BulkDownloadEvent(String sourceFilePath, String errorFilePath, Integer entityId, Integer callerId, DownloadType eventType, String identificationCode) {
        this.sourceFilePath = sourceFilePath;
        this.errorFilePath = errorFilePath;
        this.entityId = entityId;
        this.callerId = callerId;
        this.eventType = eventType;
        this.identificationCode = identificationCode;
    }

    @Override
    public String getName() {
        return "Bulk Download Event";
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

    public DownloadType getEventType() {
        return eventType;
    }

    public String getIdentificationCode() {
        return identificationCode;
    }

    public void setIdentificationCode(String identificationCode) {
        this.identificationCode = identificationCode;
    }
}
