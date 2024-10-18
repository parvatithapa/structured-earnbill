package com.sapienter.jbilling.server.pluggableTask;

import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.sapienter.jbilling.server.notification.INotificationSessionBean;
import com.sapienter.jbilling.server.notification.MessageDTO;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.report.ReportBL;
import com.sapienter.jbilling.server.report.ReportExportDTO;
import com.sapienter.jbilling.server.report.ReportExportFormat;
import com.sapienter.jbilling.server.report.db.ReportDAS;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaImportHelper;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

public class AverageRevenueReportScheduleTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(AverageRevenueReportScheduleTask.class);

    private static final String WRONG_FORMAT_MSG = "Please choose a valid format: PDF, CSV, or XLS (Microsoft Excel format)";
    private static final String OUTPUT_DIR_MSG = "Please specify the directory to store the reports";

    // 0 is placeholder for the report creation timestamp, 1 is placeholder for
    // file extension
    private static final String FILE_NAME_PATTERN = "average-revenue-{0}{1}";

    public static final ParameterDescription PARAM_REPORT_FORMAT = new ParameterDescription(
	    "Report target format (CSV, PDF, XSL)", true, ParameterDescription.Type.STR, "PDF");
    public static final ParameterDescription PARAM_OUTPUT_DIR = new ParameterDescription("Output directory", true,
	    ParameterDescription.Type.STR);
    public static final ParameterDescription PARAM_NOTIFICATION_ID = new ParameterDescription("Notification ID", true,
	    ParameterDescription.Type.INT);
    public static final ParameterDescription PARAM_REPORT_ID = new ParameterDescription("Report ID", true,
	    ParameterDescription.Type.INT);

    private static final String REPORT_PARAM_MONTH = "month";
    private static final String REPORT_PARAM_YEAR = "year";

    {
	descriptions.add(PARAM_REPORT_FORMAT);
	descriptions.add(PARAM_OUTPUT_DIR);
	descriptions.add(PARAM_NOTIFICATION_ID);
	descriptions.add(PARAM_REPORT_ID);
    }
    // these are variables defined in the notification template
    private static final String NOTIF_VAR_TASK = "task-name";
    private static final String NOTIF_VAR_EXEC_TIMESTAMP = "execution-timestamp";
    private static final String NOTIF_VAR_ENTITY_ID = "entity-id";
    //


    private static final String FILE_CREATION_TIMESTAMP_PATTERN = "yyyyMMdd'T'HHmmss";
    private static final String EXE_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    //
    private Integer notfMsgTypeId;
    private String outputDirPath;
    private Integer reportId;
    private String reportFormatStr;

    private CompanyDAS companyDas;

    // -------------------------------------------------------------------------------------

    public AverageRevenueReportScheduleTask() {
	super();
	companyDas = new CompanyDAS();
    }

    // -------------------------------------------------------------------------------------

    @Override
    public String getTaskName() {
	return "Average Revenue Report Scheduled Task";
    }

    // -------------------------------------------------------------------------------------

    public void doExecute(JobExecutionContext jobExeCtx) throws JobExecutionException {
	logger.debug("ENTER << doExecute");

	UserDTO adminUser = null;
	try {
	    CompanyDTO company = companyDas.find(getEntityId());
	    adminUser = new UserDAS().findAdminUsers(getEntityId()).get(0);
	    Integer adminUserId = adminUser.getId();
	    String companyTimeZone = company.getTimezone();
	    logger.debug("Company time-zone is: {}", companyTimeZone);
	    logger.debug("Admin-user is with ID: {}", adminUserId);
	    // get and validate the arguments to the instance
	    getTaskArguments(jobExeCtx);

	    ReportBL reportBl = new ReportBL();
	    
	    logger.debug("Trying to generate a report with ID: {} defined for Company with ID: {}", reportId,
		    getEntityId());
	    ReportExportFormat reportFormat = ReportExportFormat.valueOf(reportFormatStr);
	    // prepare reportBL instance to generate the target report
	    reportBl.set(reportId);
	    reportBl.setEntityId(getEntityId());
	    reportBl.setLocale(adminUserId);
	    reportBl.updateAdminRole(adminUserId);
	    reportBl.addAditionalParameters(getMonthYearReportParameters(jobExeCtx.getFireTime()));
	    ReportExportDTO exportReportObj = reportBl.export(reportFormat);
	    //
	    storeReport(exportReportObj, jobExeCtx.getFireTime());
	    logger.info("A report has successfully created, and stored in file-system");
	} catch (Throwable e) {
	    logger.error("Error occurred during execuation of: " + getTaskName(), e);
	    try {
		notifyAdmin(jobExeCtx.getFireTime(), adminUser);
	    } catch (Exception ex) {
		logger.error("Error raised while trying to notify admin about an already occurred error", ex);
	    }
	}
	logger.debug("EXIT >>> doExecute");
    }

    // -------------------------------------------------------------------------------------
    /**
     * Validate arguments to the instance
     * 
     * @return a list of validation message. The list is empty if arguments are
     *         all valid.
     */
    public List<String> validateArguments() throws PluggableTaskException {
	List<String> validationMsgList = new ArrayList<>();
	ReportExportFormat format = ReportExportFormat.valueOf(reportFormatStr);
	if (format == null) {
	    validationMsgList.add(WRONG_FORMAT_MSG);
	}

	Resource outputDirRsc = new FileSystemResource(outputDirPath);
	if (outputDirRsc.exists()) {
	    try {
		File outputDir = outputDirRsc.getFile();
		if (!outputDir.canWrite()) {
		    validationMsgList.add("jBilling cannot does not have enough rights to write to: " + outputDirPath);
		}
	    } catch (Exception e) {
		String msg = "Error occurred while trying to get directory object: " + outputDirPath;
		logger.error(msg, e);
		throw new PluggableTaskException(msg, e);
	    }
	} else {
	    validationMsgList.add("The spacified directory: " + outputDirPath + " does not exist. " + OUTPUT_DIR_MSG);
	}

	try {
	    ReportDAS reportDas = new ReportDAS();
	    if (reportDas.findNow(reportId) == null) {
		validationMsgList.add("The given Report-ID: " + reportId
			+ " did not identify any report. Please investigate");
	    }
	} catch (Exception e) {
	    String msg = "Error occurred while trying to retrieve a report with ID: " + reportId;
	    logger.error(msg, e);
	    throw new PluggableTaskException(msg, e);
	}

	return validationMsgList;
    }

    // --------------------------------------------------------------------------------------------

    private void notifyAdmin(Date execTimestamp, UserDTO admin) throws PluggableTaskException {

	NotificationBL notificationBL = new NotificationBL();
	Integer langId = getLanguageId();
	notificationBL.set(notfMsgTypeId, langId, getEntityId());
	MessageDTO message = notificationBL.getDTO();
	message.getParameters().put(NOTIF_VAR_TASK, getTaskName());
	message.getParameters().put(NOTIF_VAR_EXEC_TIMESTAMP, getExecTimestamp(execTimestamp));
	message.getParameters().put(NOTIF_VAR_ENTITY_ID, getEntityId());
	INotificationSessionBean notifSession = Context.getBean(Context.Name.NOTIFICATION_SESSION);
	notifSession.notify(admin, message);
    }

    // --------------------------------------------------------------------------------------------

    private Integer getLanguageId() {
	return SpaImportHelper.getLanguageId(SpaConstants.ENGLISH_LANGUAGE);
    }

    // --------------------------------------------------------------------------------------------

    private String getExecTimestamp(Date execDate) {
	return new SimpleDateFormat(EXE_TIMESTAMP_PATTERN).format(execDate);
    }

    // --------------------------------------------------------------------------------------------

    private Map<String, Object> getMonthYearReportParameters(Date execDate) {
	Map<String, Object> retv = new HashMap<>();
	Calendar execDateCal = Calendar.getInstance();
	execDateCal.setTime(execDate);
	int month = execDateCal.get(Calendar.MONTH)+1;
	int year = execDateCal.get(Calendar.YEAR);
	logger.debug("Report parameter: {} will have value: {}", REPORT_PARAM_MONTH, month);
	logger.debug("Report parameter: {} will have value: {}", REPORT_PARAM_YEAR, year);
	
	retv.put(REPORT_PARAM_MONTH, month);
	retv.put(REPORT_PARAM_YEAR, year);

	return retv;
    }

    // ---------------------------------------------------------------------------------------------

    private void storeReport(ReportExportDTO exportReport, Date execDate) throws Exception {
	File outputDir = getOutputDir();
	String fileExtension = getFileExtension(exportReport);
	String timestamp = getFileNameTimestamp(execDate);
	String fileName = MessageFormat.format(FILE_NAME_PATTERN, timestamp, fileExtension);
	logger.debug("A report is going to be stored in directory: {}, and file: {}", outputDir.getAbsolutePath(),
		fileName);
	File outputFile = new File(outputDir, fileName);
	try (FileOutputStream fos = new FileOutputStream(outputFile)) {
	    fos.write(exportReport.getBytes());
	} catch (Exception e) {
	    logger.error("Error occurred while trying to store a report in the file system", e);
	    throw e;
	}
	logger.debug("A report has been successfully stored in:  {}", outputFile.getAbsoluteFile());
    }

    // ---------------------------------------------------------------------------------------------

    private File getOutputDir() throws Exception {
	try {
	    Resource outputDirRsc = new FileSystemResource(outputDirPath);
	    return outputDirRsc.getFile();
	} catch (Exception e) {
	    String msg = "Error occurred while trying to get directory object: " + outputDirPath;
	    logger.error(msg, e);
	    throw e;
	}
    }

    // ---------------------------------------------------------------------------------------------

    private String getFileExtension(ReportExportDTO exportReport) {
	String fileName = exportReport.getFileName();
	int fileExtIdxStart = fileName.lastIndexOf('.');
	String fileExtension = fileName.substring(fileExtIdxStart);
	logger.debug("file extension is: {}", fileExtension);
	return fileExtension;
    }

    // ---------------------------------------------------------------------------------------------

    private String getFileNameTimestamp(Date date) {
	return new SimpleDateFormat(FILE_CREATION_TIMESTAMP_PATTERN).format(date);
    }

    // ---------------------------------------------------------------------------------------------

    private void getTaskArguments(JobExecutionContext jobExeCtx) throws PluggableTaskException {
	JobDataMap jobParamArguMap = jobExeCtx.getMergedJobDataMap();
	outputDirPath = jobParamArguMap.getString(PARAM_OUTPUT_DIR.getName());
	reportId = jobParamArguMap.getIntegerFromString(PARAM_REPORT_ID.getName());
	notfMsgTypeId = jobParamArguMap.getIntegerFromString(PARAM_NOTIFICATION_ID.getName());
	reportFormatStr = jobParamArguMap.getString(PARAM_REPORT_FORMAT.getName());
	logger.debug("Task: {} is triggered with following arguments: Output-directory: {}, Report-Id:  {}, ",
		reportId, reportFormatStr, notfMsgTypeId);
	//
	List<String> valdiationMsgList = validateArguments();
	if (valdiationMsgList.isEmpty()) {
	    return;
	}
	String msg = "The instance is called with some invalid argument(s), so please investigate. "
		+ String.join("; ", valdiationMsgList);
	logger.warn(msg);
	throw new PluggableTaskException(msg);
    }
    // ---------------------------------------------------------------------------------------------

}
