package com.sapienter.jbilling.server.mediation.custommediation.spc.writer;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationErrorRecordDao;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.mediation.custommediation.spc.JMRFetcher;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.order.MediationEventResult;
import com.sapienter.jbilling.server.order.MediationEventResultList;
import com.sapienter.jbilling.server.order.OrderService;
import com.sapienter.jbilling.server.util.Constants;

public class SPCJMRToOrderWriter implements ItemWriter<Integer> {

    private static final String JMR_DUPLICATE_ERROR_CODE = "[JB-DUPLICATE]";
    private static final String JMR_PROCESSED_WITH_ERROR_CODE = "[PROCESSED-WITH-ERROR]";
    private static final String JMR_INVALID_ASSET_DATES = "[ERR-EVENT-DATE-NOT-BETWEEN-ASSET-START-END-DATES]";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer PAGE_SIZE  = 30;

    @Autowired
    private JMRRepository jmrRepository;

    @Resource(name = "spcOrderService")
    private OrderService orderService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private JMErrorRepository jmErrorRepository;

    @Autowired
    private SPCMediationHelperService spcMediationHelperService;

    @Value("#{jobExecutionContext['mediationProcessId']}")
    private UUID mediationProcessId;

    private void processSingleJMR(JbillingMediationRecord jmr) {
        if(orderService.isJMRProcessed(jmr)) {
            moveJMRToErrorRecord(jmr, JMR_DUPLICATE_ERROR_CODE, JMR_DUPLICATE_ERROR_CODE);
            return;
        }
        MediationEventResult mediationEventResult = orderService.addMediationEvent(jmr);
        updateOrMarkAsError(jmr, mediationEventResult);
    }

    /**
     * Updates jmr if no exception occur, else mark jmr as error.
     * @param jmr
     * @param mediationEventResult
     */
    private void updateOrMarkAsError(JbillingMediationRecord jmr, MediationEventResult mediationEventResult) {
        if (!mediationEventResult.hasException()) {
            jmr.setOrderId(mediationEventResult.getCurrentOrderId());
            jmr.setOrderLineId(mediationEventResult.getOrderLinedId());
            jmr.setRatedPrice(mediationEventResult.getAmountForChange());
            if (mediationEventResult.hasQuantityEvaluated()) {
                jmr.setRatedCostPrice(mediationEventResult.getCostAmountForChange());
                jmr.setStatus(JbillingMediationRecord.STATUS.PROCESSED);
            } else {
                jmr.setStatus(JbillingMediationRecord.STATUS.NOT_BILLABLE);
            }
            updateMediationRecord(jmr);
        } else {
            moveJMRToErrorRecord(jmr, mediationEventResult.getExceptionMessage(),JMR_PROCESSED_WITH_ERROR_CODE);
        }
    }


    /**
     * Processes user's all unprocessed jmrs in batch.
     * @param userId
     */
    private void processUserJMRS(Integer userId) {
        JMRFetcher jmrFetcher = new JMRFetcher(entityManager, orderService, userId,
                mediationProcessId, jmrRepository, jmErrorRepository);
        JbillingMediationRecordDao.STATUS status = JbillingMediationRecordDao.STATUS.UNPROCESSED;
        List<JbillingMediationRecord> jmrRecords = jmrFetcher.fetchJmrList(status, true, PAGE_SIZE, null, null);
        while(null!= jmrRecords && !jmrRecords.isEmpty()) {
            try {
                MediationEventResultList result = orderService.addMediationEventList(jmrRecords);
                if(result.isRolledBack()) {
                    logger.debug("jmr marked as rollback.");
                    logger.debug("process single jmr");
                    for(JbillingMediationRecord jmr : jmrRecords) {
                        processSingleJMR(jmr);
                    }
                } else {
                    for(Entry<JbillingMediationRecord, MediationEventResult> resultEntry :
                        result.getResultJmrRecordMap().entrySet()) {
                        updateOrMarkAsError(resultEntry.getKey(), resultEntry.getValue());
                    }
                }
            } catch(Exception ex) {
                logger.error("error adding jmr on order", ex);
                markAsError(jmrRecords, ex.getLocalizedMessage(), JMR_PROCESSED_WITH_ERROR_CODE);
            }
            // flush and clear current session before processing next batch of jmrs.
            entityManager.flush();
            entityManager.clear();
            JbillingMediationRecord lastJMR = jmrRecords.get(jmrRecords.size() - 1);
            jmrRecords = jmrFetcher.fetchJmrList(status, true,
                    PAGE_SIZE, lastJMR.getId(), lastJMR.getEventDate());
        }
    }

    /**
     * deletes {@link JbillingMediationRecordDao} and stores in {@link JbillingMediationErrorRecordDao}.
     * @param jmrs
     * @param errorMessage
     * @param errorCodes
     */
    private void markAsError(List<JbillingMediationRecord> jmrs, String errorMessage, String errorCodes) {
        for(JbillingMediationRecord jmr : jmrs) {
            moveJMRToErrorRecord(jmr, errorMessage, errorCodes);
        }
    }

    @Override
    public void write(List<? extends Integer> userIds) throws Exception {
        for (Integer userId : userIds) {
            long startTime = System.currentTimeMillis();
            processUserJMRS(userId);
            logger.debug("{} milliseconds taken to mediate user {}  ",
                    (System.currentTimeMillis() - startTime), userId);
        }
    }

    private JbillingMediationRecord updateMediationRecord(JbillingMediationRecord jbillingMediationRecord) {
        return DaoConverter.getMediationRecord(jmrRepository.save(DaoConverter
                .getMediationRecordDao(jbillingMediationRecord)));
    }

    private void moveJMRToErrorRecord(JbillingMediationRecord jmr, String errorMessage, String errorCodes) {
        if(errorMessage != null && errorMessage.contains(Constants.SUBSCRIPTION_ORDER_NOT_FOUND)) {
            errorCodes = JMR_INVALID_ASSET_DATES;	// If the error is due to invalid asset start and end dates
        }
        logger.error("Exception occurred while processing JMR for CDR key [{} : {}]", jmr.getRecordKey(), errorMessage);
        jmErrorRepository.save(DaoConverter.getMediationErrorRecordDao(errorCodes, jmr));
        jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
    }

}
