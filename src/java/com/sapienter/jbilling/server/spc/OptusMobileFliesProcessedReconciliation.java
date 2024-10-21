package com.sapienter.jbilling.server.spc;

public class OptusMobileFliesProcessedReconciliation extends AbstractSPCMediationReconciliationTask {

    private static final String RECORD_TYPE_10 = "10";
    private static final String RECORD_TYPE_20 = "20";
    private static final String RECORD_TYPE_30 = "30";
    private static final String RECORD_TYPE_40 = "40";
    private static final String RECORD_TYPE_50 = "50";

    @Override
    public Integer getRecordCountFromFile(String filePath) {

        SPCMediationReconciliationFileUtils fileUtils = new SPCMediationReconciliationFileUtils();
        Integer recordCount = 0;
        for(String record :  fileUtils.getRecordsFromFile(filePath)) {
            String recordType = record.substring(0,2);
            if((RECORD_TYPE_10).equals(recordType) ||
                    (RECORD_TYPE_20).equals(recordType) ||
                    (RECORD_TYPE_30).equals(recordType) ||
                    (RECORD_TYPE_40).equals(recordType) ||
                    (RECORD_TYPE_50).equals(recordType) ) {
                recordCount++;
            }
        }
        return recordCount;
    }

}