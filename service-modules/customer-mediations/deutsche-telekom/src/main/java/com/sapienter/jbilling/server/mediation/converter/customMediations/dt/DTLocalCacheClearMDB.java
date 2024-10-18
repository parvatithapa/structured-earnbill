package com.sapienter.jbilling.server.mediation.converter.customMediations.dt;

import java.lang.invoke.MethodHandles;
import java.util.Map.Entry;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.sapienter.jbilling.server.mediation.cache.CacheProvider;
import com.sapienter.jbilling.server.mediation.converter.customMediations.dt.DtCacheClearMessage.ActionType;
import com.sapienter.jbilling.server.mediation.quantityRating.usage.RecycleMediationCacheProvider;

public class DTLocalCacheClearMDB implements MessageListener {

    private Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private ApplicationContext applicationContext;

    @Resource(name = "mediationJobRepository")
    private JobRepository jobRepository;

    @Resource(name = "mediationJobExplorer")
    private JobExplorer jobExplorer;

    @Override
    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            DtCacheClearMessage dtMessage = (DtCacheClearMessage) objectMessage.getObject();
            if(DtConstants.OFFLINE_CDR_RECYCLE_JOB.equals(dtMessage.getJobName()) &&
                    ActionType.INIT.equals(dtMessage.getActionType())) {
                for(Entry<String, RecycleMediationCacheProvider> cacheProvider : applicationContext.getBeansOfType(RecycleMediationCacheProvider.class).entrySet()) {
                    initRestCache(cacheProvider.getKey(), cacheProvider.getValue(), dtMessage);
                }
            } else {
                for(Entry<String, CacheProvider> cacheProvider : applicationContext.getBeansOfType(CacheProvider.class).entrySet()) {
                    initRestCache(cacheProvider.getKey(), cacheProvider.getValue(), dtMessage);
                }
            }
        } catch(Exception ex) {
            logger.debug("Dt local cache clearing failed!", ex);
        }
    }

    private void initRestCache(String cacheName, CacheProvider cache, DtCacheClearMessage dtMessage) {
        try {
            if(ActionType.RESET.equals(dtMessage.getActionType())) {
                logger.debug("Clearing cache {} for entity {} for job {} for processId {} ", cacheName, dtMessage.getEntity(),
                        dtMessage.getJobName(), dtMessage.getProcessId());
                cache.reset();
            } else if(ActionType.INIT.equals(dtMessage.getActionType())) {
                logger.debug("Init cache {} for entity {} for job {} for processId {} ", cacheName, dtMessage.getEntity(),
                        dtMessage.getJobName(), dtMessage.getProcessId());
                cache.init();
            }
        } catch(Exception ex) {
            logger.error("Local cache {} setUp failed!", cacheName, ex);
        }
    }

}
