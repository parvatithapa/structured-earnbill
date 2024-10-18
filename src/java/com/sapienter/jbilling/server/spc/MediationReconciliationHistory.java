package com.sapienter.jbilling.server.spc;

public class MediationReconciliationHistory {
    String processedDir;
    String processedArchive;
    String processedFile;
    boolean verified;
    Integer retryCount;

    public MediationReconciliationHistory() {}

    public MediationReconciliationHistory(String processedDir, String processedArchive, String processedFile, boolean verified,
            Integer retryCount) {
        this.processedDir = processedDir;
        this.processedArchive = processedArchive;
        this.processedFile = processedFile;
        this.verified = verified;
        this.retryCount = retryCount;
    }

    public String getProcessedDir() {
        return processedDir;
    }

    public void setProcessedDir(String processedDir) {
        this.processedDir = processedDir;
    }

    public String getProcessedArchive() {
        return processedArchive;
    }

    public void setProcessedArchive(String processedArchive) {
        this.processedArchive = processedArchive;
    }

    public String getProcessedFile() {
        return processedFile;
    }

    public void setProcessedFile(String processedFile) {
        this.processedFile = processedFile;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "MediationReconciliationHistory [processedDir=" + processedDir + ", processedArchive=" + processedArchive
                + ", processedFile=" + processedFile + ", isCountMatched=" + verified + ", retryCount=" + retryCount + "]";
    }

}
