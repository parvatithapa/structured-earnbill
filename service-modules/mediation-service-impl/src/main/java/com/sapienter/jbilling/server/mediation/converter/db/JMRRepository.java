package com.sapienter.jbilling.server.mediation.converter.db;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;

/**
 * Created by marcolin on 08/10/15.
 */
public interface JMRRepository extends
JpaRepository<JbillingMediationRecordDao, JbillingMediationRecordId> {

    public static String orderByProcessingDateString = " ORDER BY mp.processing_date DESC";

    @Query(value ="SELECT * FROM  jbilling_mediation_record mp WHERE mp.process_id = :processId" + orderByProcessingDateString, nativeQuery = true)
    List<JbillingMediationRecordDao> getMediationRecordsForProcess(@Param("processId") UUID processId);

    /**
     * Returns the count of mediation records filtered by processId and status.
     *
     * @param processId
     * @param status
     * @return An Integer value for mediation records count
     */
    @Query(value ="SELECT count(*) FROM  jbilling_mediation_record mp WHERE mp.process_id = :processId AND mp.status = :status", nativeQuery = true)
    Integer getMediationRecordCountByProcessIdAndStatus(@Param("processId") UUID processId, @Param("status") String status);

    @Query(value ="SELECT distinct mp.order_id FROM  jbilling_mediation_record mp WHERE mp.process_id = :processId AND mp.status = 'PROCESSED'", nativeQuery = true)
    List<Integer> getOrdersForMediationProcess(@Param("processId") UUID processId);

    @Query(value ="SELECT count(distinct mp.order_id) FROM  jbilling_mediation_record mp WHERE mp.process_id = :processId AND mp.status = 'PROCESSED'", nativeQuery = true)
    Integer getOrderCountForMediationProcess(@Param("processId") UUID processId);

    @Query(value ="SELECT * FROM  jbilling_mediation_record mp WHERE mp.order_id = :orderId"
            + orderByProcessingDateString, nativeQuery = true)
    List<JbillingMediationRecordDao> getMediationRecordsForOrderId(@Param("orderId") Integer orderId);

    @Query(value=" SELECT jmr.* "
            + "      FROM jbilling_mediation_record jmr "
            + "INNER JOIN purchase_order po ON jmr.order_id = po.id "
            + "       AND po.deleted = 0 "
            + "       AND po.is_mediated = 't' "
            + "       AND po.user_id = :userId "
            + "INNER JOIN order_status os ON po.status_id = os.id "
            + "       AND os.order_status_flag <> 1 "
            + "  ORDER BY jmr.processing_date DESC "
            + "  LIMIT :limit OFFSET :offset ",
            nativeQuery = true)
    List<JbillingMediationRecordDao> getUnBilledMediationEventsByUser(@Param("userId") Integer userId, @Param("offset") int offset, @Param("limit") int limit);

    @Query(value=" SELECT count(jmr.*) "
            + "      FROM jbilling_mediation_record jmr "
            + "INNER JOIN purchase_order po ON jmr.order_id = po.id "
            + "       AND po.deleted = 0 "
            + "       AND po.is_mediated = 't' "
            + "       AND po.user_id = :userId "
            + "INNER JOIN order_status os ON po.status_id = os.id "
            + "       AND os.order_status_flag <> 1 ",
            nativeQuery = true)
    Long getCountOfUnBilledMediationEventsByUser(@Param("userId") Integer userId);


    @Query(value ="SELECT mp.order_id FROM jbilling_mediation_record mp WHERE mp.order_line_id = :orderLineId", nativeQuery = true)
    List<JbillingMediationRecord> getMediationRecordsForOrderLineId(@Param("orderLineId") Integer orderLineId);

    @Query(value ="SELECT * FROM  jbilling_mediation_record mp WHERE mp.process_id = :processId AND mp.order_id = :orderId"
            + orderByProcessingDateString, nativeQuery = true)
    List<JbillingMediationRecordDao> getMediationRecordsForProcessIdOrderId(
            @Param("processId") UUID processId, @Param("orderId") Integer orderId);

    @Query(value ="SELECT * FROM  jbilling_mediation_record mp WHERE mp.mediation_cfg_id = :configId"
            + orderByProcessingDateString, nativeQuery = true)
    List<JbillingMediationRecordDao> getMediationRecordsForConfigId(@Param("configId") Integer processId);

    @Query(value ="select m from JbillingMediationRecordDao m where m.recordKey = :recordKey")
    JbillingMediationRecordDao findByRecordKey(@Param("recordKey") String recordKey);

    @Query(value ="select count(*) from jbilling_mediation_record where record_key = :recordKey", nativeQuery= true)
    Integer getRecordKeyCount(@Param("recordKey") String recordKey);

    @Query(value ="select distinct user_id from jbilling_mediation_record where status = :status", nativeQuery= true)
    List<Integer> findUsersByStatus(@Param("status") String status);

    @Query(value ="select distinct user_id from jbilling_mediation_record where status = :status and (user_id % :totalPartitions = :currentPartition)", nativeQuery= true)
    List<Integer> findUsersByStatusAndPartition(@Param("status") String status, @Param("totalPartitions") Integer totalPartitions, @Param("currentPartition") Integer currentPartition);

    @Query(value="select distinct mp.order_id from jbilling_mediation_record mp,purchase_order p where mp.order_id=p.id and mp.process_id = :processId and mp.status = 'PROCESSED'and p.status_id != :excludedOrderStatus  ",nativeQuery = true)
    List<Integer> getOrdersForMediationProcessByStatusExcluded(@Param("processId") UUID processId, @Param("excludedOrderStatus") Integer excludedOrderStatus);


    List<JbillingMediationRecordDao> findByUserIdAndStatus(Integer userId, JbillingMediationRecordDao.STATUS status);
    List<JbillingMediationRecordDao> findByStatus(JbillingMediationRecordDao.STATUS status);

    @Query(value = "SELECT jmr.* "
            + "       FROM jbilling_mediation_record jmr "
            + "      WHERE jmr.user_id = ?1"
            + "        AND jmr.status  = ?2"
            + "        AND jmr.chargeable  = ?3"
            + "   ORDER BY jmr.processing_date ASC", nativeQuery = true)
    Stream<JbillingMediationRecordDao> findJMRByUserIdAndStatusAndChargeable(Integer userId, String status, Boolean chargeable);

    @Query(value = "SELECT SUM(jmr.quantity) "
            + "       FROM jbilling_mediation_record jmr "
            + "      WHERE jmr.user_id  = ?1"
            + "        AND jmr.item_id  = ?2"
            + "        AND jmr.cdr_type = ?3"
            + "        AND jmr.status   = ?4"
            + "        AND jmr.chargeable  = ?5"
            + "        AND jmr.processing_date >= ?6"
            + "        AND jmr.processing_date <= ?7", nativeQuery = true)
    BigDecimal sumOfJMRQuantityForUserItemAndCdrTypeForDateRange(Integer userId, Integer itemId, String cdrType,
            String status, Boolean chargeable, Date startDate, Date endDate);

}
