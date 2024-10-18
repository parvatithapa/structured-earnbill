package com.sapienter.jbilling.server.batch.mediation;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.OrderService;

public class DefaultUserJMRProcessor implements ItemProcessor<Integer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Value("#{jobParameters['entityId']}")
    private Integer entityId;

    @Autowired
    private JMRRepository jmrRepository;

    @Resource(name = "orderService")
    private OrderService orderService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Override
    public Integer process(Integer userId) throws Exception {
        try (Stream<JbillingMediationRecordDao> jmrStream =
                jmrRepository.findJMRByUserIdAndStatusAndChargeable(userId, JbillingMediationRecordDao.STATUS.UNPROCESSED.name(), Boolean.TRUE)) {
            Iterator<JbillingMediationRecordDao> jmrIterator = jmrStream.iterator();
            int counter = 0;
            while(jmrIterator.hasNext()) {
                processJMR(jmrIterator.next());
                if( ++counter % 100 == 0) {
                    entityManager.flush();
                    entityManager.clear();
                }
            }
        }
        return userId;
    }


    private void processJMR(JbillingMediationRecordDao jmrDBRecord) {
        JbillingMediationRecord jmr = null;
        try {
            jmr = DaoConverter.getMediationRecord(jmrDBRecord);
            MediationEventResult mediationEventResult = orderService.addMediationEvent(jmr);
            if(!mediationEventResult.hasException()) {
                jmr.setOrderId(mediationEventResult.getCurrentOrderId());
                jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
                jmr.setRatedPrice(mediationEventResult.getAmountForChange());
                if(mediationEventResult.hasQuantityEvaluated()) {
                    jmr.setRatedCostPrice(mediationEventResult.getCostAmountForChange());
                    jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
                } else {
                    jmr.setStatus(JbillingMediationRecord.STATUS.NOT_BILLABLE);
                }
                updateMediationRecord(jmr);
            } else {
                moveJMRToErrorRecord(jmr, mediationEventResult.getExceptionMessage());
            }
        } catch(Exception ex) {
            if(Objects.nonNull(jmr)) {
                moveJMRToErrorRecord(jmr, ex.getMessage());
            }
        }
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
