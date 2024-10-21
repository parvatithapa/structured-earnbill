package com.sapienter.jbilling.server.spc;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.util.Context;

public abstract class AbstractSPCMediationReconciliationTask {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);

    public Map<File, MediationReconciliationRecord> getRecordforAsset(String basePath, Map<String, String> fileProcessedMap) {
        Map<File, MediationReconciliationRecord> reconciliationRecords = new HashMap<>();
        try {
            List<String> filePathList = new ArrayList<>();
            SPCMediationReconciliationFileUtils fileUtils = new SPCMediationReconciliationFileUtils();
            fileUtils.getAllFiles(filePathList, basePath);

            for (String filePath : filePathList) {
                File file = new File(filePath);
                String fileName = file.getName();
                Integer fileProcessedRecordCount = spcHelperService.getFileProcessedRecordCount(fileName);
                Integer recordCount = getRecordCountFromFile(filePath);
                int count = null != fileProcessedRecordCount ? fileProcessedRecordCount : 0;
                Integer difference = recordCount - count;
                String errorMessage = fileProcessedMap.get(fileName);
                MediationReconciliationRecord reconciliationRecord = new MediationReconciliationRecord(fileName, recordCount, count,
                        difference, null != errorMessage ? errorMessage : StringUtils.EMPTY);
                fileProcessedMap.remove(fileName);
                reconciliationRecords.put(file, reconciliationRecord);

            }

            if (!fileProcessedMap.isEmpty()) {
                for (Map.Entry<String, String> entry : fileProcessedMap.entrySet()) {
                    String errorMessage = entry.getValue();
                    MediationReconciliationRecord reconciliationRecord = new MediationReconciliationRecord(entry.getKey(), 0, 0, -1,
                            null != errorMessage ? errorMessage : StringUtils.EMPTY);
                    reconciliationRecords.put(new File(entry.getKey()), reconciliationRecord);
                }
            }
        } catch (Exception e) {
            logger.error("Error while executing SPCMediationReconciliationTask {} ", e.getMessage());
        }
        return reconciliationRecords;
    }

    public abstract Integer getRecordCountFromFile(String filePath);
}
