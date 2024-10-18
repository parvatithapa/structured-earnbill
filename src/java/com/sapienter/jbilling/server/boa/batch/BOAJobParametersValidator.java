package com.sapienter.jbilling.server.boa.batch;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.boa.batch.db.BoaBaiProcessedFileDAS;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;

import java.io.File;

/**
 * @author Javier Rivero
 * @since 05/01/16.
 */
public class BOAJobParametersValidator implements JobParametersValidator {
    private UserDAS userDas = new UserDAS();
    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(BOAJobParametersValidator.class));

    @Override
    public void validate(JobParameters jobParams)
            throws JobParametersInvalidException {
        Integer userId = Integer.parseInt(jobParams.getString(Constants.BOA_JOB_PARAM_DEFAULT_USER_ID));
        Integer companyId = userDas.getUserCompanyId(userId);
        if (null == userId) {
            throw new JobParametersInvalidException("Invalid default_user_id parameter");
        }

        if (!userDas.exists(userId, companyId)) {
            throw new JobParametersInvalidException("No user found with default_user_id:" + userId);
        }

        String readFromDailyFilesDirectory = jobParams.getString(Constants.BOA_JOB_PARAM_READ_FROM_DAILY_FILES_DIRECTORY);
        if (null == readFromDailyFilesDirectory || readFromDailyFilesDirectory.trim().isEmpty()) {
            throw new JobParametersInvalidException("Invalid read_from_directory parameter");
        }


        File dailyFilesReadDirectory = new File(readFromDailyFilesDirectory);

        if (!dailyFilesReadDirectory.exists() || !dailyFilesReadDirectory.isDirectory()) {
            throw new JobParametersInvalidException("read_from_directory:" + readFromDailyFilesDirectory + " does not exists or is not a directory");
        }

        if (!dailyFilesReadDirectory.canRead() || !dailyFilesReadDirectory.canWrite()) {
            throw new JobParametersInvalidException("read_from_directory:" + readFromDailyFilesDirectory + " does not have read and write permissions");
        }

        String readFromIntradayFilesDirectory = jobParams.getString(Constants.BOA_JOB_PARAM_READ_FROM_INTRADAY_FILES_DIRECTORY);
        if (null == readFromIntradayFilesDirectory || readFromIntradayFilesDirectory.trim().isEmpty()) {
            throw new JobParametersInvalidException("Invalid read_from_directory parameter");
        }

        File intradayFilesReadDirectory = new File(readFromIntradayFilesDirectory);

        if (!intradayFilesReadDirectory.exists() || !intradayFilesReadDirectory.isDirectory()) {
            throw new JobParametersInvalidException("read_from_directory:" + readFromIntradayFilesDirectory + " does not exists or is not a directory");
        }

        if (!intradayFilesReadDirectory.canRead() || !intradayFilesReadDirectory.canWrite()) {
            throw new JobParametersInvalidException("read_from_directory:" + readFromIntradayFilesDirectory + " does not have read and write permissions");
        }

        String moveToDailyFilesDirectory = jobParams.getString(Constants.BOA_JOB_PARAM_MOVE_TO_DAILY_FILES_DIRECTORY);
        if (null == moveToDailyFilesDirectory || moveToDailyFilesDirectory.trim().isEmpty()) {
            throw new JobParametersInvalidException("Invalid read_from_directory parameter");
        }

        File dailyFilesMoveDirectory = new File(moveToDailyFilesDirectory);
        if (!dailyFilesMoveDirectory.exists() || !dailyFilesMoveDirectory.isDirectory()) {
            throw new JobParametersInvalidException("move_to_directory:" + moveToDailyFilesDirectory + " does not exists or is not a directory");
        }

        if (!dailyFilesMoveDirectory.canRead() || !dailyFilesMoveDirectory.canWrite()) {
            throw new JobParametersInvalidException("move_to_directory:" + dailyFilesMoveDirectory + " does not have read and write permissions");
        }

        String moveToIntradayFilesDirectory = jobParams.getString(Constants.BOA_JOB_PARAM_MOVE_TO_INTRADAY_FILES_DIRECTORY);
        if (null == moveToIntradayFilesDirectory || moveToIntradayFilesDirectory.trim().isEmpty()) {
            throw new JobParametersInvalidException("Invalid read_from_directory parameter");
        }

        File intradayFilesMoveDirectory = new File(moveToIntradayFilesDirectory);
        if (!intradayFilesMoveDirectory.exists() || !intradayFilesMoveDirectory.isDirectory()) {
            throw new JobParametersInvalidException("move_to_directory:" + moveToIntradayFilesDirectory + " does not exists or is not a directory");
        }

        if (!intradayFilesMoveDirectory.canRead() || !intradayFilesMoveDirectory.canWrite()) {
            throw new JobParametersInvalidException("move_to_directory:" + intradayFilesMoveDirectory + " does not have read and write permissions");
        }

        //Validate if daily read file already processed.
        for (File dailyReadFile : dailyFilesReadDirectory.listFiles()) {
            try {
                if (null != dailyReadFile && dailyReadFile.isFile() && (new BoaBaiProcessedFileDAS().isProcessed(dailyReadFile.getName()))) {
                    LOG.info("File Already Processed So Moving file [From] - " + dailyReadFile.getAbsoluteFile().getAbsolutePath() +
                            " [To] - " + moveToDailyFilesDirectory + File.separator +  dailyReadFile.getName() + ".duplicate");
                    FileUtils.moveFile(dailyReadFile, new File(moveToDailyFilesDirectory + File.separator +
                            dailyReadFile.getName() + ".duplicate"));
                }
            } catch (Exception e) {
                throw new JobParametersInvalidException("File :" + dailyReadFile.getName() + " already processed");
            }
        }

        //Validate if intraday read file already processed.
        for (File intradayReadFile : intradayFilesReadDirectory.listFiles()) {
            try {
                if (null != intradayReadFile && intradayReadFile.isFile() && (new BoaBaiProcessedFileDAS().isProcessed(intradayReadFile.getName()))) {
                    LOG.info("File Already Processed So Moving file [From] - " + intradayReadFile.getAbsoluteFile().getAbsolutePath() +
                            " [To] - " + moveToIntradayFilesDirectory + File.separator +  intradayReadFile.getName() + ".duplicate");
                    FileUtils.moveFile(intradayReadFile, new File(moveToIntradayFilesDirectory + File.separator +
                            intradayReadFile.getName() + ".duplicate"));
                }
            } catch (Exception e) {
                throw new JobParametersInvalidException("File :" + intradayReadFile.getName() + " already processed");
            }
        }
    }

}
