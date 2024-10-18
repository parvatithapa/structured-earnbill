package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.listener;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.jms.core.JmsTemplate;

import com.sapienter.jbilling.server.mediation.converter.MediationServiceImplementation;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtCacheClearMessage;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtCacheClearMessage.ActionType;
import com.sapienter.jbilling.server.mediation.listener.MediationJobListener;
import com.sapienter.jbilling.server.util.Context;
import com.sapienter.jbilling.server.util.Context.Name;


public class DtMediationJobListener extends MediationJobListener {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Resource(name = "mediationJobExplorer")
    private JobExplorer jobExplorer;

    @Override
    public void afterJob(JobExecution jobExecution) {
        super.afterJob(jobExecution);
        postLocalCacheClearMessageOnQueue(jobExecution.getId(), ActionType.RESET);
    }

    private void postLocalCacheClearMessageOnQueue(Long jobExecutionId, ActionType actionType) {
        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);
            UUID processId = (UUID) jobExecution.getExecutionContext().get(MediationServiceImplementation.PARAMETER_MEDIATION_PROCESS_ID_KEY);
            String entityId = jobExecution.getJobParameters().getString(MediationServiceImplementation.PARAMETER_MEDIATION_ENTITY_ID_KEY);

            DtCacheClearMessage cacheClearMessage = new DtCacheClearMessage(jobExecution.getId(), jobExecution.getJobInstance().getJobName(),
                    Integer.valueOf(entityId), processId.toString(), actionType);
            logger.debug("Posting local cache clear message {} for entity {} for job {}", cacheClearMessage, cacheClearMessage.getEntity(), cacheClearMessage.getJobName());
            JmsTemplate jmsTemplate = Context.getBean(Name.JMS_TEMPLATE);
            Destination destination = Context.getBean(Name.DT_CLSUTER_CACHE_CLEAR_DESTINATION);

            jmsTemplate.send(destination, session -> {
                ObjectMessage objectMessage = session.createObjectMessage();
                objectMessage.setObject(cacheClearMessage);
                return objectMessage;
            });
            logger.debug("Message Posted On queue {}", cacheClearMessage);
        } catch(Exception ex) {
            logger.error("Post message failed!", ex);
        }
    }

}
