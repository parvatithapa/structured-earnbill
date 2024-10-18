package com.sapienter.jbilling.server.spc;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.task.MediationDirectoryBasedProcessTask;

public class SpcMediationDirectoryBasedProcessTask extends MediationDirectoryBasedProcessTask {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> MUR_MEDIATION_JOB_NAME_LIST = Arrays.asList(
            SPCConstants.OPTUS_MUR_JOB_NAME, SPCConstants.OPTUS_MUR_RECYCLE_JOB_NAME);

    private static final String FIND_RUNNING_JOB_NAME_SQL = "SELECT mediation_job_launcher FROM mediation_cfg WHERE id IN "
            + "(SELECT configuration_id FROM jbilling_mediation_process WHERE end_date IS NULL AND entity_id = ?)";

    /**
     * Returns true if given jobName is mur mediation job, else returns false.
     * @param jobName
     * @return
     */
    private boolean isMurMediationJob(String jobName) {
        Assert.hasLength(jobName, "jobName required!");
        return MUR_MEDIATION_JOB_NAME_LIST.contains(jobName);
    }

    @Override
    protected boolean shouldSkipMediation(String upcomingJobName, MediationService mediationService,
            JobExplorer jobExplorer, JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "jdbcTemplate is required!");
        Assert.notNull(mediationService, "mediationService is required!");
        Assert.notNull(jobExplorer, "jobExplorer is required!");
        Assert.hasLength(upcomingJobName, "upcomingJobName is required!");
        Integer entityId = getEntityId();
        List<String> runningJobs = jdbcTemplate.queryForList(FIND_RUNNING_JOB_NAME_SQL, String.class, entityId);
        if(CollectionUtils.isEmpty(runningJobs)) {
            return false;
        }
        if(runningJobs.size() > 1) {
            logger.debug("mulitple mediation jobs {} running for entity id {}, so skipping {} mediation",
                    runningJobs, entityId, upcomingJobName);
            return true;
        }
        String currentlyRunningJobName = runningJobs.get(0);
        if(StringUtils.isEmpty(currentlyRunningJobName)) {
            return false;
        }
        if(!isMurMediationJob(currentlyRunningJobName) && isMurMediationJob(upcomingJobName)) {
            logger.debug("current running job {} is non mur and upcoming job {} is mur, so allowed",
                    currentlyRunningJobName, upcomingJobName);
            return false;
        } else if(!isMurMediationJob(currentlyRunningJobName) && !isMurMediationJob(upcomingJobName)) {
            logger.debug("both jobs [current-{}, upcoming-{}] are non mur job so skipping upcoming job",
                    currentlyRunningJobName, upcomingJobName);
            return true;
        } else if(isMurMediationJob(currentlyRunningJobName) && isMurMediationJob(upcomingJobName)) {
            logger.debug("both jobs [current-{}, upcoming-{}] are mur job so skipping upcoming job",
                    currentlyRunningJobName, upcomingJobName);
            return true;
        } else if(isMurMediationJob(currentlyRunningJobName) && !isMurMediationJob(upcomingJobName)) {
            logger.debug("current running job {} is mur and upcoming job {} is non mur, so allowed",
                    currentlyRunningJobName, upcomingJobName);
            return false;
        }
        logger.debug("current running job {}, so skipping upcoming job {}", currentlyRunningJobName, upcomingJobName);
        return true;
    }
}
