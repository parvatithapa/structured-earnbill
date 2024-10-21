package com.sapienter.jbilling.server.spc;

public class TelstraMobileFliesProcessedReconciliation  extends AbstractSPCMediationReconciliationTask {

    @Override
    public Integer getRecordCountFromFile(String filePath) {
        SPCMediationReconciliationFileUtils fileUtils = new SPCMediationReconciliationFileUtils();
        return fileUtils.getRecordsFromFile(filePath).size();
    }

}