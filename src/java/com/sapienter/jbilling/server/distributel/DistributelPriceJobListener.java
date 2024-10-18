package com.sapienter.jbilling.server.distributel;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;

public class DistributelPriceJobListener implements JobExecutionListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ERROR_DIR_NAME ;

    static {
        ERROR_DIR_NAME = com.sapienter.jbilling.common.Util.getSysProp("base_dir") +
                DistributelPriceJobConstants.ERROR_DIR_NAME;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String notificationId = jobExecution.getJobParameters().getString(DistributelPriceJobConstants.PARAM_NOTIFICATION_EMAIL_ID);
        String entityId = jobExecution.getJobParameters().getString(DistributelPriceJobConstants.PARAM_ENTITY_ID);

        if(StringUtils.isNotEmpty(notificationId)) {
            String errorFilePath = jobExecution.getExecutionContext().getString(DistributelPriceJobConstants.PARAM_ERROR_FILE_PATH);
            if(StringUtils.isNotEmpty(errorFilePath)) {
                try {
                    if (Paths.get(errorFilePath).toFile().length() == 0L) {
                        logger.debug("No error found for entity {}, so skipping error notification", entityId);
                        return;
                    }
                    logger.debug("Sending Error details to {}", notificationId);
                    NotificationBL.sendSapienterEmail(notificationId, Integer.valueOf(entityId),DistributelPriceJobConstants.MESSAGE_KEY,
                            errorFilePath, null);
                } catch (Exception e) {
                    logger.error("Could no send the email with the paper invoices for entity {} ", entityId, e);
                }
            }
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String entityId = jobExecution.getJobParameters().getString(DistributelPriceJobConstants.PARAM_ENTITY_ID);
        try {
            Path dirPath = Paths.get(ERROR_DIR_NAME);
            if(!dirPath.toFile().exists()) {
                Files.createDirectory(dirPath);
            }
        } catch(IOException ex) {
            logger.error("Error Folder Creation Failed for entity {}", entityId , ex);
            return ;
        }

        DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
        String fileName =  new StringBuilder().append(File.separator)
                .append(entityId)
                .append("-")
                .append("Error-")
                .append(dateFormat.format(TimezoneHelper.companyCurrentDate(Integer.valueOf(entityId))))
                .append(".csv")
                .toString();
        String filePath = ERROR_DIR_NAME + fileName;
        logger.debug("Creating Error File {} for entity {}", fileName, entityId);
        jobExecution.getExecutionContext().put(DistributelPriceJobConstants.PARAM_ERROR_FILE_PATH, filePath);
        Path path = Paths.get(filePath);
        if(!path.toFile().exists()) {
            try {
                Files.createFile(Paths.get(filePath));
                logger.debug("Error File {} created for entity {}", filePath, entityId);
            } catch (IOException ex) {
                logger.error("Error File Creation Failed for entity {}", entityId , ex);
                jobExecution.getExecutionContext().put(DistributelPriceJobConstants.PARAM_ERROR_FILE_PATH, null);
            }
        }
    }

}
