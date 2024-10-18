package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.batch;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.stream.Stream;

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
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;

@Component("optusMurJMRUserProcessor")
@Scope("step")
class OptusMurJMRUserProcessor implements ItemProcessor<Integer, Integer>{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Autowired
    private JMRRepository jmrRepository;

    @Autowired
    private SPCMediationHelperService spcMediationHelperService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Override
    public Integer process(Integer userId) throws Exception {
        try (Stream<JbillingMediationRecordDao> jmrStream =
                jmrRepository.findJMRByUserIdAndStatusAndChargeable(userId, JbillingMediationRecordDao.STATUS.UNPROCESSED.name(), Boolean.FALSE)) {
            Iterator<JbillingMediationRecordDao> jmrIterator = jmrStream.iterator();
            int counter = 0;
            while(jmrIterator.hasNext()) {
                JbillingMediationRecord jmr = DaoConverter.getMediationRecord(jmrIterator.next());
                try {
                    logger.debug("Notifying User {} for item {} for cdr type {}", jmr.getUserId(), jmr.getItemId(), jmr.getCdrType());
                    spcMediationHelperService.notifyUserForJMR(jmr);
                    jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
                    updateMediationRecord(jmr);
                } catch(Exception ex) {
                    logger.error("Error while sending notification to user {}",jmr.getUserId(), ex);
                    moveJMRToErrorRecord(jmr, ex.getLocalizedMessage());
                }
                if( ++counter % 100 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }
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
