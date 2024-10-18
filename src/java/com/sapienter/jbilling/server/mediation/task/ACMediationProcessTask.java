package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.IMediationSessionBean;
import com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.FullCreativeConstants;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.notification.NotificationBL;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.contact.db.ContactDAS;
import com.sapienter.jbilling.server.user.contact.db.ContactDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class ACMediationProcessTask extends AbstractCronTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(ACMediationProcessTask.class));
	private static final String PROPERTY_RUN_MEDIATION = "process.run_mediation";
	private static final AtomicBoolean running = new AtomicBoolean(false);
	
	@Override
	public String getTaskName() {
		return "mediation process: , entity id " + getEntityId() + ", taskId " + getTaskId();
	}

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		super.execute(context);
		 String localResourceDir = Util.getSysProp("base_dir") + Constants.MEDIATION_HOME;
         
		if (running.compareAndSet(false, true)) {

            IMediationSessionBean mediation = (IMediationSessionBean) Context.getBean(Context.Name.MEDIATION_SESSION);
            StringBuilder errorMessages = new StringBuilder("Errors during mediation triggering: \n");
            try {
                if (Util.getSysPropBooleanTrue(PROPERTY_RUN_MEDIATION)) {
                	Calendar cal = Calendar.getInstance();
                	cal.add(Calendar.DATE, -1); 
                    LOG.info("Starting mediation at " + cal.getTime());
                    File file = null;
                    String fileName = getFileName(cal.getTime());
                    StringBuilder mediationCfgNames = new StringBuilder();
                    for (MediationConfiguration cfg : mediation.getAllConfigurations(getEntityId(), false)) {
                        try {
                        	file = new File(localResourceDir+getFolderName(cfg.getMediationJobLauncher())+fileName);
                            LOG.info("File name : "+ file.getName());
                        	if(!file.exists()){
                        		mediationCfgNames.append(cfg.getName());
                        		mediationCfgNames.append("\n");
                        		continue;
                        	}
                        	//mediation.triggerMediationByConfigurationWithFileInjection(cfg.getId(), getEntityId(), file);
                        } catch (Exception ex) {
                            LOG.error("Exception occurred triggering mediation configuration %s", cfg.getId(), ex);
                            errorMessages.append(ex.getMessage());
                        }
                    }
                 // throw a SessionInternalError of errors were returned from the configuration run (possible plugin errors)
                    if (errorMessages.length() > 50) {
                        throw new SessionInternalError(errorMessages.toString());
                    }
                    if(mediationCfgNames.length() > 0){ 
                    	String[] params = {localResourceDir, mediationCfgNames.toString()};
                    	ContactDAS contactDAS = new ContactDAS();
                    	ContactDTO adminUser = contactDAS.findByEntityAndUserName("admin", getEntityId());
                    	try {
							NotificationBL.sendSapienterGmail(adminUser.getEmail(), getEntityId(), "mediation.file.notfound", null, params);
						} catch (Exception e) {
							throw new SessionInternalError(e);
						} 
                    }
                    LOG.info("Ended mediation at " + TimezoneHelper.serverCurrentDate());
                }
            } finally {
                running.set(false);
            }
        } else {
            LOG.warn("Failed to trigger mediation process at " + context.getFireTime()
                    + ", another process is already running.");
        }
	}
	
	private static String getFileName(Date current){
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		StringBuilder fileName = new StringBuilder(Constants.MEDIATION_FILE_NAME_PREFIX);
		fileName.append(format.format(current));
		fileName.append(Constants.MEDIATION_FILE_NAME_SUFFIX);
		return fileName.toString();
	}
	private static String getFolderName(String cfgName){
		if(cfgName.contains(FullCreativeConstants.ACTIVE_RESPONSE_MEDIATION_CONFIGURATION)){
			return FullCreativeConstants.ACTIVE_RESPONSE_FOLDER_NAME;
		}
		if(cfgName.contains(FullCreativeConstants.CHAT_MEDIATION_CONFIGURATION)){
			return FullCreativeConstants.CHAT_FOLDER_NAME;
		}
		if(cfgName.contains(FullCreativeConstants.INBOUND_CALL_MEDIATION_CONFIGURATION)){
			return FullCreativeConstants.INBOUND_CALLS_FOLDER_NAME;
		}
        if(cfgName.contains(FullCreativeConstants.SPANISH_MEDIATION_CONFIGURATION)){
            return FullCreativeConstants.SPANISH_FOLDER_NAME;
        }
        if(cfgName.contains(FullCreativeConstants.SUPERVISOR_MEDIATION_CONFIGURATION)){
            return FullCreativeConstants.SUPERVISOR_FOLDER_NAME;
        }
        if(cfgName.contains(FullCreativeConstants.CALL_RELAY_MEDIATION_CONFIGURATION)){
            return FullCreativeConstants.CALL_RELAY_FOLDER_NAME;
        }
        if(cfgName.contains(FullCreativeConstants.LIVE_RECEPTION_MEDIATION_CONFIGURATION)){
            return FullCreativeConstants.LIVE_RECEPTION_FOLDER_NAME;
        }
        if(cfgName.contains(FullCreativeConstants.IVR_MEDIATION_CONFIGURATION)){
            return FullCreativeConstants.IVR_FOLDER_NAME;
        }
		throw new SessionInternalError("Configuration not matched");
	}
}
