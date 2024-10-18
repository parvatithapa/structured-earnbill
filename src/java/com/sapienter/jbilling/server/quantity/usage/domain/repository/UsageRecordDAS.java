package com.sapienter.jbilling.server.quantity.usage.domain.repository;


import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.type.BigDecimalType;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.orm.hibernate4.support.HibernateDaoSupport;

import com.sapienter.jbilling.server.quantity.usage.domain.IUsageRecord;
import com.sapienter.jbilling.server.quantity.usage.domain.UsageRecord;
import com.sapienter.jbilling.server.util.Context;


public class UsageRecordDAS extends HibernateDaoSupport {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public UsageRecordDAS() {
        setSessionFactory(Context.getBean(Context.Name.HIBERNATE_SESSION));
    }


    private static class Queries {

        private static final String SQL_ITEM_USAGE_JMR =
                " SELECT sum(jmr.original_quantity) as quantity " +
                " FROM   jbilling_mediation_record jmr " +
                " WHERE  jmr.item_id = :item_id " +
                " AND    jmr.user_id = :user_id " +
                " AND    jmr.jbilling_entity_id = :entity_id " +
                " AND    jmr.event_date >= :start_date " +
                " AND    jmr.event_date < :end_date " +
                " AND    jmr.process_id != :process_id " +
                " AND    jmr.status in ('NOT_BILLABLE', 'PROCESSED') ";

        private static final String SQL_ITEM_USAGE_ERROR =
                " SELECT sum(jmr.original_quantity) as quantity " +
                " FROM   jm_error_usage_record jmr, jbilling_mediation_error_record jer " +
                " WHERE  jmr.error_record_id = jer.id " +
                " AND    jmr.record_key = jer.record_key " +
                " AND    jmr.mediation_cfg_id = jer.mediation_cfg_id" +
                " AND    jmr.entity_id = jer.jbilling_entity_id" +
                " AND 	 jer.jbilling_entity_id = :entity_id " +
                " AND    jmr.item_id = :item_id " +
                " AND 	 jmr.user_id = :user_id " +
                " AND 	 jmr.event_date >= :start_date " +
                " AND 	 jmr.event_date < :end_date " +
                " AND 	 jer.process_id != :process_id " +
                " AND 	 jer.error_codes LIKE '%PROCESSED-WITH-ERROR%' ";

        private static final String SQL_CONDITION_RESOURCE =
                " AND jmr.resource_id = :resource_id ";

        public static final String SQL_ITEM_USAGE =
                "SELECT SUM(t.quantity) as quantity " +
                " FROM (" +
                SQL_ITEM_USAGE_JMR +
                " UNION " +
                SQL_ITEM_USAGE_ERROR +
                ") t";

        public static final String SQL_ITEM_USAGE_RESOURCE =
                "SELECT SUM(t.quantity) as quantity " +
                " FROM (" +
                SQL_ITEM_USAGE_JMR + SQL_CONDITION_RESOURCE +
                " UNION " +
                SQL_ITEM_USAGE_ERROR + SQL_CONDITION_RESOURCE +
                ") t";
    }


    public IUsageRecord getItemUsage(Integer itemId, Integer userId, Integer entityId,
                                     Date startDate, Date endDate,
                                     String mediationProcessId) {

        Query query = getSession().createSQLQuery(Queries.SQL_ITEM_USAGE)
                .addScalar("quantity", BigDecimalType.INSTANCE)
                .setParameter("item_id", itemId)
                .setParameter("user_id", userId)
                .setParameter("entity_id", entityId)
                .setParameter("start_date", startDate)
                .setParameter("end_date", endDate)
                .setParameter("process_id", UUID.fromString(mediationProcessId));

        logger.debug("getItemUsage: {}", query.getQueryString());
        BigDecimal quantity = (BigDecimal) query.uniqueResult();

        logger.info("Quantity: {}", quantity);
        return UsageRecord.builder()
                .item(itemId)
                .user(userId)
                .startDate(startDate)
                .endDate(endDate)
                .quantity(quantity == null ? BigDecimal.ZERO: quantity)
                .build();
    }


    public IUsageRecord getItemResourceUsage(Integer itemId, Integer userId, Integer entityId,
                                             String resourceId, Date startDate, Date endDate,
                                             String mediationProcessId) {

        Query query = getSession().createSQLQuery(Queries.SQL_ITEM_USAGE_RESOURCE)
                .addScalar("quantity", BigDecimalType.INSTANCE)
                .setParameter("item_id", itemId)
                .setParameter("user_id", userId)
                .setParameter("entity_id", entityId)
                .setParameter("start_date", startDate)
                .setParameter("end_date", endDate)
                .setParameter("resource_id", resourceId)
                .setParameter("process_id", UUID.fromString(mediationProcessId));

        logger.debug("getItemResourceUsage: {}", query.getQueryString());
        BigDecimal quantity = (BigDecimal) query.uniqueResult();

        logger.info("Quantity: {}", quantity);
        return UsageRecord.builder()
                .item(itemId)
                .user(userId)
                .resource(resourceId)
                .startDate(startDate)
                .endDate(endDate)
                .quantity(quantity == null ? BigDecimal.ZERO: quantity)
                .build();
    }

    private Session getSession() {
        return currentSession();
    }
}
