package com.sapienter.jbilling.server.spc;

import java.util.List;

public class OptusMURFileProcessedReconciliation extends AbstractSPCMediationReconciliationTask {

    @Override
    public Integer getRecordCountFromFile(String filePath) {

        SPCMediationReconciliationFileUtils fileUtils = new SPCMediationReconciliationFileUtils();
        Integer recordCount = 0;
        List<String[]> list = fileUtils.getExcludedRecordsSetFromFile(filePath);
        for(String[] excludedRecord :  list) {
            if(excludedRecord.length > 0 && "G".equals(excludedRecord[0])) {
                recordCount++;
            }
        }

        return recordCount;
    }
}