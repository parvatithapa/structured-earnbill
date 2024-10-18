/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.mediation.task;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.db.MediationConfiguration;
import com.sapienter.jbilling.server.mediation.db.MediationConfigurationDAS;
import com.sapienter.jbilling.server.process.task.AbstractCronTask;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.util.Context;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

/**
 * Scheduled mediation process plug-in, executing the mediation process on a simple schedule.
 *
 * This plug-in accepts the standard {@link com.sapienter.jbilling.server.process.task.AbstractCronTask} plug-in parameters
 *
 * @see com.sapienter.jbilling.server.process.task.AbstractCronTask
 *
 * @author Brian Cowdery
 * @since 25-05-2010
 */
public class MediationProcessTask extends AbstractCronTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String PROPERTY_RUN_MEDIATION = "process.run_mediation";
    private static final String BASE_DIR = com.sapienter.jbilling.common.Util.getSysProp("base_dir");

    @Override
    public String getTaskName() {
        return this.getClass().getName() + "-" + getEntityId();
    }

    @Override
    public void doExecute(JobExecutionContext context) throws JobExecutionException {
        MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
        if (!mediationService.isMediationProcessRunning()) {
            if (Util.getSysPropBooleanTrue(PROPERTY_RUN_MEDIATION)) {
                logger.info("Starting mediation at {} for entity {} ", TimezoneHelper.serverCurrentDate(), getEntityId());
                List<MediationConfiguration> allByEntity = new MediationConfigurationDAS().findAllByEntity(getEntityId());
                for (MediationConfiguration mediationConfiguration : allByEntity) {
                    if(mediationConfiguration.getLocalInputDirectory() != null) {
                        processMediationFolder(mediationConfiguration, mediationService);
                    } else {
                        mediationService.launchMediation(getEntityId(), mediationConfiguration.getId(), mediationConfiguration.getMediationJobLauncher());
                    }
                }
            }
        } else {
            logger.warn("Failed to trigger mediation process at {} "
                    + "another process is already running.", context.getFireTime());
        }
    }

    private void processMediationFolder(MediationConfiguration mediationConfiguration, MediationService mediationService) {
        String absolutePath = BASE_DIR + mediationConfiguration.getLocalInputDirectory() + File.separator;
        File mediationFolder = new File(absolutePath);
        Arrays.stream(mediationFolder.listFiles())
        .forEach(file -> {
            try {
                mediationService.launchMediation(getEntityId(), mediationConfiguration.getId(), mediationConfiguration.getMediationJobLauncher(), file);
            }catch (Exception ex) {
                logger.error("launchMediation Error!", ex);
            }
        });
    }
}