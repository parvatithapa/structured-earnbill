package com.sapienter.jbilling.server.mediation;

import java.io.File;
import java.util.UUID;

/**
 * Created by marcolin on 05/11/15.
 */
public class MediationContext {
    private Integer entityId;
    private Integer mediationCfgId;
    private String jobName;
    private File fileWithCdrs;
    private JbillingMediationRecord recordToProcess;
    private UUID processIdForMediation;

    public UUID getProcessIdForMediation() {
        return processIdForMediation;
    }

    public void setProcessIdForMediation(UUID processIdForMediation) {
        this.processIdForMediation = processIdForMediation;
    }

    public Integer getEntityId() {
        return entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    public Integer getMediationCfgId() {
        return mediationCfgId;
    }

    public void setMediationCfgId(Integer mediationCfgId) {
        this.mediationCfgId = mediationCfgId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public File getFileWithCdrs() {
        return fileWithCdrs;
    }

    public void setFileWithCdrs(File fileWithCdrs) {
        this.fileWithCdrs = fileWithCdrs;
    }

    public JbillingMediationRecord getRecordToProcess() {
        return recordToProcess;
    }

    public void setRecordToProcess(JbillingMediationRecord recordToProcess) {
        this.recordToProcess = recordToProcess;
    }

    @Override
    public String toString() {
        return "MediationContext [entityId=" + entityId + ", mediationCfgId="
                + mediationCfgId + ", jobName=" + jobName + ", fileWithCdrs="
                + fileWithCdrs + ", processIdForMediation="
                + processIdForMediation + "]";
    }
    
    
}
