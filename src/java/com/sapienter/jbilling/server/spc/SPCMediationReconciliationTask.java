package com.sapienter.jbilling.server.spc;

import static com.sapienter.jbilling.server.spc.SPCMediationReconciliationFileUtils.*;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

public class SPCMediationReconciliationTask extends AbstractCronTask {

    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ParameterDescription PARAM_BACKUP_BASE_DIR = new ParameterDescription("Email CSV Dir", true,
            ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_SPC_OPTUS_MOBILE_BACKUP_DIRECTORY_PATH = new ParameterDescription(
            "Mediation Receiving Optus Mobile Backup Dir", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_SPC_OPTUS_MUR_BACKUP_DIRECTORY_PATH = new ParameterDescription(
            "Mediation Receiving Optus MUR Backup Dir", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_TELSTRA_MOBILE_BACKUP_DIRECTORY_PATH = new ParameterDescription(
            "Mediation Receiving Telstra Mobile Backup Dir", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_SPC_OPTUS_MOBILE_BASE_DIRECTORY_PATH = new ParameterDescription(
            "Mediation Receiving Optus Mobile Dir", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_SPC_OPTUS_MUR_BASE_DIRECTORY_PATH = new ParameterDescription(
            "Mediation Receiving Optus MUR Dir", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_TELSTRA_MOBILE_BASE_DIRECTORY_PATH = new ParameterDescription(
            "Mediation Receiving Telstra Mobile Dir", true, ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_START_TIME_IN_SECONDS = new ParameterDescription("Start Time In Seconds", false,
            ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_BUFFER_SIZE = new ParameterDescription("Buffer Size", false,
            ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_EMAIL_ADDRESSES = new ParameterDescription("Email Addresses", true,
            ParameterDescription.Type.STR);
    private static final ParameterDescription PARAM_DAYS_TO_RECONCILIATION = new ParameterDescription("Days To Reconciliation", false,
            ParameterDescription.Type.INT);
    private static final ParameterDescription PARAM_RETRY_COUNT = new ParameterDescription("Retry Count", false,
            ParameterDescription.Type.INT);

    private static final String MESSAGE_KEY_01 = "spc.mediation.reconciliation.diff.found.message";
    private static final String MESSAGE_KEY_02 = "spc.mediation.reconciliation.diff.not.found.message";
    private static final String RECON_DIR = "reconcilation" + File.separator;
    private static int BUFFER_SIZE = 4096;
    private static int START_TIME_SECONDS = 44;
    private String optusMobFilePath;
    private String optusMurFilePath;
    private String telstraMobFilePath;

    SpcHelperService spcHelperService = Context.getBean(SpcHelperService.class);

    public SPCMediationReconciliationTask() {
        descriptions.add(PARAM_SPC_OPTUS_MOBILE_BACKUP_DIRECTORY_PATH);
        descriptions.add(PARAM_SPC_OPTUS_MUR_BACKUP_DIRECTORY_PATH);
        descriptions.add(PARAM_TELSTRA_MOBILE_BACKUP_DIRECTORY_PATH);
        descriptions.add(PARAM_SPC_OPTUS_MOBILE_BASE_DIRECTORY_PATH);
        descriptions.add(PARAM_SPC_OPTUS_MUR_BASE_DIRECTORY_PATH);
        descriptions.add(PARAM_TELSTRA_MOBILE_BASE_DIRECTORY_PATH);
        descriptions.add(PARAM_START_TIME_IN_SECONDS);
        descriptions.add(PARAM_EMAIL_ADDRESSES);
        descriptions.add(PARAM_BUFFER_SIZE);
        descriptions.add(PARAM_BACKUP_BASE_DIR);
        descriptions.add(PARAM_DAYS_TO_RECONCILIATION);
        descriptions.add(PARAM_RETRY_COUNT);
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        String companyTimeZone = TimezoneHelper.getCompanyLevelTimeZone(getEntityId());
        LocalDateTime curDate = LocalDateTime.now(ZoneId.of(companyTimeZone));
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String curDir = dateFormatter.format(curDate) + File.separator;
        List<String> dateList = new ArrayList<>();
        try {
            _init(context);
            logger.debug("Executing : {} : ", getTaskName());
            SPCMediationReconciliationFileUtils fileUtils = new SPCMediationReconciliationFileUtils(getParameter(
                    PARAM_BUFFER_SIZE.getName(), BUFFER_SIZE),getParameter(PARAM_RETRY_COUNT.getName(), 1));
            int dayToRecon = getParameter(PARAM_DAYS_TO_RECONCILIATION.getName(), 1);
            for (int i = dayToRecon - 1; i >= 0; i--) {
                dateList.add(dateFormatter.format(curDate.minusDays(i)) + File.separator);
            }

            Map<File, MediationReconciliationRecord> recordMap = new HashMap<>();
            Map<File, MediationReconciliationRecord> optusMobRecordMap = new HashMap<>();
            Map<File, MediationReconciliationRecord> optusMurRecordMap = new HashMap<>();
            Map<File, MediationReconciliationRecord> telstraMobRecordMap = new HashMap<>();
            optusMobFilePath = getParameter(PARAM_SPC_OPTUS_MOBILE_BACKUP_DIRECTORY_PATH.getName(), StringUtils.EMPTY);
            optusMurFilePath = getParameter(PARAM_SPC_OPTUS_MUR_BACKUP_DIRECTORY_PATH.getName(), StringUtils.EMPTY);
            telstraMobFilePath = getParameter(PARAM_TELSTRA_MOBILE_BACKUP_DIRECTORY_PATH.getName(), StringUtils.EMPTY);
            Map<String, File> allFilesMap = new HashMap<>();
            for (String newDir : dateList) {
                AbstractSPCMediationReconciliationTask optusMobRecon = new OptusMobileFliesProcessedReconciliation();
                String backupDir = optusMobFilePath + newDir;
                logger.debug("Processing Optus mobile files for reconciliation from dir {}", backupDir);
                Map<String, String> fileProcessedMap = fileUtils.extractAllOptusMobileFiles(backupDir, backupDir + RECON_DIR,
                        allFilesMap);
                optusMobRecordMap.putAll(optusMobRecon.getRecordforAsset(backupDir + RECON_DIR, fileProcessedMap));

                AbstractSPCMediationReconciliationTask optusMurRecon = new OptusMURFileProcessedReconciliation();
                backupDir = optusMurFilePath + newDir;
                logger.debug("Processing Optus Mur files for reconciliation from dir {}", backupDir);
                fileProcessedMap = fileUtils.extractAllOptusMurFiles(backupDir, backupDir + RECON_DIR, allFilesMap);
                optusMurRecordMap.putAll(optusMurRecon.getRecordforAsset(backupDir + RECON_DIR, fileProcessedMap));

                AbstractSPCMediationReconciliationTask teltraMobRecon = new TelstraMobileFliesProcessedReconciliation();
                backupDir = telstraMobFilePath + newDir;
                logger.debug("Processing Telstra mobile files for reconciliation from dir {}", backupDir);
                fileProcessedMap = fileUtils.extractAllTelstraMobileFiles(backupDir, backupDir + RECON_DIR, allFilesMap);
                telstraMobRecordMap.putAll(teltraMobRecon.getRecordforAsset(backupDir + RECON_DIR, fileProcessedMap));
            }

            recordMap.putAll(optusMobRecordMap);
            recordMap.putAll(optusMurRecordMap);
            recordMap.putAll(telstraMobRecordMap);

            DateTimeFormatter dateTimeFormatterWithTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String currentDate = dateTimeFormatterWithTime.format(curDate);

            boolean containsNonZeroValues = recordMap.values().stream().anyMatch(r -> r.getDifference() != 0);

            if (!recordMap.isEmpty() && containsNonZeroValues) {
                File file = writeCsvFile(recordMap, currentDate, getParameter(PARAM_BACKUP_BASE_DIR.getName(), StringUtils.EMPTY));

                sendEmail(getParameter(PARAM_EMAIL_ADDRESSES.getName(), StringUtils.EMPTY), MESSAGE_KEY_01, file.getAbsolutePath(),
                        new String[] { file.getName() });

                startAfterSeconds(getParameter(PARAM_START_TIME_IN_SECONDS.getName(), START_TIME_SECONDS));

                logger.debug("Started copying files to base dir path of mediation");

                fileUtils.copyFiles(getFilesToCopy(optusMobRecordMap.keySet(), allFilesMap),
                        getParameter(PARAM_SPC_OPTUS_MOBILE_BASE_DIRECTORY_PATH.getName(), StringUtils.EMPTY) + curDir);
                fileUtils.copyFiles(getFilesToCopy(optusMurRecordMap.keySet(), allFilesMap),
                        getParameter(PARAM_SPC_OPTUS_MUR_BASE_DIRECTORY_PATH.getName(), StringUtils.EMPTY) + curDir);
                fileUtils.copyFiles(getFilesToCopy(telstraMobRecordMap.keySet(), allFilesMap),
                        getParameter(PARAM_TELSTRA_MOBILE_BASE_DIRECTORY_PATH.getName(), StringUtils.EMPTY) + curDir);
                logger.debug("Finished copying files to base dir path of mediation");

            } else {
                sendEmail(getParameter(PARAM_EMAIL_ADDRESSES.getName(), StringUtils.EMPTY), MESSAGE_KEY_02, null,
                        new String[] { currentDate });
            }

            spcHelperService.createOrUpdateMediationReconciliationData(fileUtils.getReconciledFilesMap(recordMap,allFilesMap));
        } catch (Exception ex) {
            logger.error("Error executing SPCMediationReconciliationTask ", ex);
        } finally {
            logger.debug("Deleting temp directories");
            try {
                for (String newDir : dateList) {
                    deleteDirectoriesFromDir(optusMobFilePath + newDir);
                    deleteDirectoriesFromDir(optusMurFilePath + newDir);
                    deleteDirectoriesFromDir(telstraMobFilePath + newDir);
                }
            } catch (Exception ex) {
                logger.error("Error deleting dirs ", ex);
            }
        }
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    private static void startAfterSeconds(int seconds) {
        while (true) {
            if (LocalTime.now().getSecond() > seconds) {
                logger.debug("Current time {}", LocalTime.now());
                break;
            }
        }
    }

    private void sendEmail(String emailIds, String messageKey, String filePath, String[] params) {
        for (String email : emailIds.split(",")) {
            try {
                NotificationBL.sendSapienterEmail(email, getEntityId(), messageKey, filePath, params);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

}
