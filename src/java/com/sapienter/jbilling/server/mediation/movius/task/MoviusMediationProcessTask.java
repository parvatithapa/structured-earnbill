package com.sapienter.jbilling.server.mediation.movius.task;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.mediation.IMediationSessionBean;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusHelperService;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;

/**
 * 
 * @author Krunal Bhavsar
 *
 */
public class MoviusMediationProcessTask extends AbstractCronTask {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final String DONE_DIR_NAME = "-Done";
	private static final String FILE_RENAME_EXTENTION = ".done";

	@Override
	public void doExecute (JobExecutionContext context) throws JobExecutionException {
		MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);

		if(mediationService.isMediationProcessRunning()) {
			logger.debug("mediation process is still running");
			return ;
		}

		logger.debug("Running MoviusMediationProcessTask on batch-2 server");

		_init(context);
		IMediationSessionBean mediationBean = Context.getBean(Context.Name.MEDIATION_SESSION);
		MoviusHelperService moviusHelperService = Context.getBean(MoviusHelperService.BEAN_NAME);
		List<Integer> entities = moviusHelperService.getAllChildEntityForGivenEntity(getEntityId());
		for(MediationConfigurationWS configuration : mediationBean.getMediationConfigurations(entities)) {
			if(StringUtils.isNotEmpty(configuration.getLocalInputDirectory())
					&& StringUtils.isNotEmpty(configuration.getMediationJobLauncher())) {
				File mediationDirectory = new File(configuration.getLocalInputDirectory());
				if(!mediationDirectory.exists()) {
					logger.debug("Skipping Mediation, Because invalid directory path given for Mediation job config {} for entity {} "
							, configuration.getId(), configuration.getEntityId());
					continue;
				}
				logger.debug("Prcessing File Directory {}", mediationDirectory.getAbsoluteFile());
				File[] cdrFiles = mediationDirectory.listFiles(cdrFile -> !cdrFile.getName().endsWith(FILE_RENAME_EXTENTION));
				if(ArrayUtils.isEmpty(cdrFiles)) {
					logger.debug("Skipping Mediation, Because no cdr file found for Mediation job config {} for entity {} "
							, configuration.getId(), configuration.getEntityId());
					continue;
				}
				Arrays.sort(cdrFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
				logger.debug("Sorted Cdr files {}", Arrays.toString(cdrFiles));
				for(File cdrFile : cdrFiles) {
					try {
						if(mediationService.isMediationProcessRunning()) {
							// checks twice before triggering mediation to avoid any issue with mediation process.
							logger.debug("Skipping file {} upload for entity {}  since mediation already running!", cdrFile.getName(), configuration.getEntityId());
							return ;
						}
						String doneDirPath = mediationDirectory.getAbsolutePath()+ DONE_DIR_NAME;
						if(!Paths.get(doneDirPath).toFile().exists()) {
							new File(doneDirPath).mkdir();
						}
						File tempCdrFile = File.createTempFile(cdrFile.getName(), ".tmp");
						FileUtils.copyFile(cdrFile, tempCdrFile);
						logger.debug("Moving {} file to Dir {}", cdrFile.getName(), doneDirPath);
						if(cdrFile.renameTo(new File(doneDirPath + File.separator + cdrFile.getName() + FILE_RENAME_EXTENTION))) {
							logger.debug("Moved {} file to Dir {}", cdrFile.getName(), doneDirPath);
							mediationService.triggerMediationJobLauncherByConfiguration(configuration.getEntityId(),
									configuration.getId(), configuration.getMediationJobLauncher(), tempCdrFile, cdrFile.getName());
							logger.debug("Uploading cdr file {} from configuration {} for entity {}", cdrFile.getName(), configuration.getId(), configuration.getEntityId());
							break;
						} else {
							logger.debug("File can not moved to directory directory {}", doneDirPath);
							logger.debug("Mediation Upload for file {} skipped ", cdrFile.getName());
						}
					} catch(IOException ioException) {
						throw new SessionInternalError(ioException);
					}
				}

			}
		}
	}

	@Override
	public String getTaskName() {
		return this.getClass().getName() + "-" + getEntityId();
	}

}
