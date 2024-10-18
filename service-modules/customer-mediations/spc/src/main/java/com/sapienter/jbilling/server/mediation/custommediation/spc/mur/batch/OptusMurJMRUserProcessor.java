package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.batch;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.custommediation.spc.JMRFetcher;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.order.OrderService;

@Component("optusMurJMRUserProcessor")
@Scope("step")
class OptusMurJMRUserProcessor implements ItemProcessor<Integer, Integer>{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final int PAGE_SIZE = 30;

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private SPCMediationHelperService spcMediationHelperService;

    @Value("#{jobExecutionContext['mediationProcessId']}")
    private UUID mediationProcessId;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Resource(name = "spcOrderService")
    private OrderService orderService;

    @Override
    public Integer process(Integer userId) throws Exception {
        JMRFetcher jmrFetcher = new JMRFetcher(entityManager, orderService, userId,
                mediationProcessId, jmrRepository, jmErrorRepository);
        JbillingMediationRecordDao.STATUS status = JbillingMediationRecordDao.STATUS.UNPROCESSED;
        List<JbillingMediationRecord> jmrRecords = jmrFetcher.fetchJmrList(status, false, PAGE_SIZE, null, null);
        long startTime = System.currentTimeMillis();
        while(null!= jmrRecords && !jmrRecords.isEmpty()) {
            try {
                spcMediationHelperService.notifyUserForJMRs(jmrRecords);
                //updating jmr
                for(JbillingMediationRecord jmrRecord : jmrRecords) {
                    jmrRecord.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
                    updateMediationRecord(jmrRecord);
                }
            } catch(Exception ex) {
                JbillingMediationRecord jmr = jmrRecords.get(0);
                logger.error("Error while sending notification to user {}",jmr.getUserId(), ex);
                for(JbillingMediationRecord jmrRecord : jmrRecords) {
                    moveJMRToErrorRecord(jmrRecord, ex.getLocalizedMessage());
                }
            }
            // flush and clear current session before processing next batch of jmrs.
            entityManager.flush();
            entityManager.clear();
            JbillingMediationRecord lastJMR = jmrRecords.get(jmrRecords.size() - 1);
            jmrRecords = jmrFetcher.fetchJmrList(status, false,
                    PAGE_SIZE, lastJMR.getId(), lastJMR.getEventDate());
        }
        logger.debug("time taken to send notification {} for user {}", (System.currentTimeMillis() - startTime), userId);
        return userId;
    }

    private JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(
                jmrRepository.save(DaoConverter.getMediationRecordDao(jbillingMediationRecord)));
    }

    private void moveJMRToErrorRecord(JbillingMediationRecord jmr, String errorMessage) {
        logger.error("Exception occurred while processing JMR for CDR key [{} : {}]", jmr.getRecordKey(), errorMessage);
        jmErrorRepository.save(DaoConverter.getMediationErrorRecordDao(jmr));
        jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
    }

}
