package com.sapienter.jbilling.server.mediation;

import java.util.UUID;

public class MediationProcessCDRCountInfo {

    private Integer id;
    private UUID processId;
    private String callType;
    private Integer count;
    private String recordStatus;

    public static MediationProcessCDRCountInfo of(Integer id, UUID processId, String callType, Integer count, String recordStatus) {
        return new MediationProcessCDRCountInfo(id, processId, callType, count, recordStatus);
    }

    private MediationProcessCDRCountInfo(Integer id, UUID processId, String callType, Integer count, String recordStatus) {
        this.id = id;
        this.processId = processId;
        this.callType = callType;
        this.count = count;
        this.recordStatus = recordStatus;
    }

    public Integer getId() {
        return id;
    }

    public UUID getProcessId() {
        return processId;
    }

    public String getCallType() {
        return callType;
    }

    public Integer getCount() {
        return count;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    @Override
    public String toString() {
        return String
                .format("MediationProcessCDRCountInfo [id=%s, processId=%s, callType=%s, count=%s, recordStatus=%s]",
                        id, processId, callType, count, recordStatus);
    }


}
