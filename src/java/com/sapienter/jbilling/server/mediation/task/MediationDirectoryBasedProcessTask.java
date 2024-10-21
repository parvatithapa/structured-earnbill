package com.sapienter.jbilling.server.mediation.task;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.mediation.IMediationSessionBean;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;
import com.sapienter.jbilling.server.util.IWebServicesSessionBean;

/**
 * task will use dir path from mediation configuration and
 * execute job if file found and moved it to done dir.
 * @author Krunal Bhavsar
 *
 */
public class MediationDirectoryBasedProcessTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DONE_DIR_NAME = "-Done";
    private static final String FILE_RENAME_EXTENTION = ".done";

    @Override
    public void doExecute (JobExecutionContext context) throws JobExecutionException {
        MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
        IWebServicesSessionBean webServicesSession  = Context.getBean(Context.Name.WEB_SERVICES_SESSION);
        JobExplorer jobExplorer = Context.getBean("mediationJobExplorer");
        _init(context);
        IMediationSessionBean mediationBean = Context.getBean(Context.Name.MEDIATION_SESSION);
        for(MediationConfigurationWS configuration : mediationBean.getMediationConfigurations(Arrays.asList(getEntityId()))) {
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
                boolean isBillingRunning = false;
                for(File cdrFile : cdrFiles) {
                    try {
                        isBillingRunning = webServicesSession.isBillingRunning(configuration.getEntityId());
                        logger.debug("Is billing process running: {} for entity: {}",
                                isBillingRunning, configuration.getEntityId());
                        if(isBillingRunning) {
                            // checks twice before triggering mediation to avoid any issue with mediation process.
                            logger.debug("Skipping mediation CDR file {} upload for entity {} since the billing process already in running status.", cdrFile.getName(), configuration.getEntityId());
                            return;
                        }
                        if(shouldSkipMediation(configuration.getMediationJobLauncher(), mediationService,
                                jobExplorer, Context.getBean(Name.JDBC_TEMPLATE))) {
                            // checks twice before triggering mediation to avoid any issue with mediation process.
                            logger.debug("Skipping file {} upload for entity {}  since mediation already running!", cdrFile.getName(), configuration.getEntityId());
                            break;
                        }
                        String doneDirPath = mediationDirectory.getAbsolutePath() + DONE_DIR_NAME;
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
                    } catch(Exception ex) {
                        logger.error("Failed Mediation Job!", ex);
                    }
                }

            }
        }
    }

    /**
     * Returns true then mediation upload will be skipped,
     * if returns false then mediation upload will be triggred.
     * @param upcomingJobName
     * @param mediationService
     * @param jobExplorer
     * @return
     */
    protected boolean shouldSkipMediation(String upcomingJobName, MediationService mediationService,
            JobExplorer jobExplorer, JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "jdbcTemplate is required!");
        Assert.notNull(mediationService, "mediationService is required!");
        Assert.notNull(jobExplorer, "jobExplorer is required!");
        Assert.hasLength(upcomingJobName, "upcomingJobName is required!");
        logger.debug("upcoming job {} for entity id {}", upcomingJobName, getEntityId());
        return mediationService.isMediationProcessRunning();
    }

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }
}