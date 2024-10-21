package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.converter.db.DaoConverter;
import com.sapienter.jbilling.server.mediation.converter.db.JMErrorRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JMRRepository;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationErrorRecordDao;
import com.sapienter.jbilling.server.mediation.converter.db.JbillingMediationRecordDao;
import com.sapienter.jbilling.server.order.OrderService;

public class JMRFetcher {

    private static final String JMR_DUPLICATE_ERROR_CODE = "[JB-DUPLICATE]";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer PAGE_SIZE  = 30;

    private static final String NL = System.getProperty("line.separator");

    private static final String FIND_JMR_FOR_USER_SQL =
            " SELECT jmr FROM JbillingMediationRecordDao jmr "
                    + " WHERE jmr.userId = :userId "
                    + " AND jmr.status   = :status "
                    + " AND jmr.processId = :processId "
                    + " AND jmr.chargeable = :chargeable ";

    private static final String ORDER_BY_EVENT_DATE_SQL = String.join(NL,
            " ORDER BY jmr.eventDate ASC ");

    private static final String LAST_ID_AND_EVENT_DATE_CHECK_SQL = String.join(NL,
            " AND jmr.id != :id AND jmr.eventDate >= :ed ");

    private final EntityManager entityManager;
    private final OrderService orderService;
    private final Integer userId;
    private final UUID mediationProcessId;
    private final JMRRepository jmrRepository;
    private final JMErrorRepository jmErrorRepository;

    public JMRFetcher(EntityManager entityManager, OrderService orderService, Integer userId, UUID mediationProcessId,
            JMRRepository jmrRepository, JMErrorRepository jmErrorRepository) {
        this.entityManager = entityManager;
        this.orderService = orderService;
        this.userId = userId;
        this.mediationProcessId = mediationProcessId;
        this.jmrRepository = jmrRepository;
        this.jmErrorRepository = jmErrorRepository;
    }

    /**
     * Converts {@link JbillingMediationRecordDao} to {@link JbillingMediationRecord} and check for duplicates,
     * if duplicate found then move record to {@link JbillingMediationErrorRecordDao} as duplicate.
     * @param jmrs
     * @return
     */
    private List<JbillingMediationRecord> convertAndMoveDuplicateJMRToError(List<JbillingMediationRecordDao> jmrs) {
        List<JbillingMediationRecord> result = jmrs.stream()
                .map(DaoConverter::getMediationRecord)
                .collect(Collectors.toList());
        if(null == result || result.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> recordkeys = new HashSet<>();
        ListIterator<JbillingMediationRecord> jmrIterator = result.listIterator();
        while(jmrIterator.hasNext()) {
            JbillingMediationRecord jmr = jmrIterator.next();
            if(recordkeys.contains(jmr.getRecordKey()) || isProcessed(jmr)) {
                jmrIterator.remove(); // remove duplicate jmr.
                logger.debug("duplicate jmr {} found, moving to error", jmr);
                moveJMRToErrorRecord(jmr, JMR_DUPLICATE_ERROR_CODE, JMR_DUPLICATE_ERROR_CODE);
            } else {
                recordkeys.add(jmr.getRecordKey());
            }
        }
        return result;
    }

    private boolean isProcessed(JbillingMediationRecord jmr) {
        long startTime = System.currentTimeMillis();
        boolean processed = orderService.isJMRProcessed(jmr);
        logger.debug("time taken {} for isProcessed in miliseconds", (System.currentTimeMillis() - startTime));
        return processed;
    }

    @SuppressWarnings("unchecked")
    public List<JbillingMediationRecord> fetchJmrList(JbillingMediationRecordDao.STATUS status, boolean chargeable,
            int pageSize, Long lastJMRId, Date lastJMREventDate) {
        long startTime = System.currentTimeMillis();
        String query = FIND_JMR_FOR_USER_SQL;
        if(null != lastJMRId && null != lastJMREventDate) {
            query = query + LAST_ID_AND_EVENT_DATE_CHECK_SQL;
        }
        query = query + ORDER_BY_EVENT_DATE_SQL;
        Query jmrQuery = entityManager.createQuery(query,JbillingMediationRecordDao.class)
                .setParameter("userId", userId)
                .setParameter("status", status)
                .setParameter("processId", mediationProcessId)
                .setParameter("chargeable", chargeable);
        if(null!= lastJMRId) {
            jmrQuery.setParameter("id", lastJMRId);
        }
        if(null!= lastJMREventDate) {
            jmrQuery.setParameter("ed", lastJMREventDate);
        }
        if(pageSize <=0 || pageSize > PAGE_SIZE) {
            pageSize = PAGE_SIZE;
        }
        jmrQuery.setMaxResults(pageSize);
        List<JbillingMediationRecord> results = convertAndMoveDuplicateJMRToError(jmrQuery.getResultList());
        logger.debug("time taken {} for fetchJmrList in miliseconds", (System.currentTimeMillis() - startTime));
        return results;
    }

    private void moveJMRToErrorRecord(JbillingMediationRecord jmr, String errorMessage, String errorCodes) {
        logger.error("Exception occurred while processing JMR for CDR key [{} : {}]", jmr.getRecordKey(), errorMessage);
        jmErrorRepository.save(DaoConverter.getMediationErrorRecordDao(errorCodes, jmr));
        jmrRepository.delete(DaoConverter.getMediationRecordDao(jmr));
    }

}
