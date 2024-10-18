/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */
package com.sapienter.jbilling.server.util.audit.db;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import com.sapienter.jbilling.server.util.db.AbstractDAS;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import java.util.Arrays;
import java.util.List;

public class EventLogDAS extends AbstractDAS<EventLogDTO> {

    private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(EventLogDAS.class));

    // QUERIES
    private static final String findLastTransition =
        "SELECT max(id) from EventLogDTO" +
        " WHERE eventLogModule.id = " + EventLogger.MODULE_WEBSERVICES +
        " AND eventLogMessage.id = " + EventLogger.USER_TRANSITIONS_LIST +
        " AND company.id = :entity";

    public Integer getLastTransitionEvent(Integer entityId) {
        Query query = getSession().createQuery(findLastTransition);
        query.setParameter("entity", entityId);
        Integer id = (Integer) query.uniqueResult();
        if (id == null) {
            LOG.warn("Can not find max value.");
            // it means that this is the very first time the web service
            // method is called with 'null,null'. Return all then.
            return 0;
        }
        EventLogDTO latest = find(id);
        return latest.getOldNum();
    }

    public List<EventLogDTO> getEventsByAffectedUser(Integer userId) {
        Criteria criteria = getSession().createCriteria(EventLogDTO.class)
                .add(Restrictions.eq("affectedUser.id", userId))
                .addOrder(Order.desc("createDatetime"));

        return criteria.list();
    }

    public List<EventLogDTO> getEventsByCompany(Integer entityId) {
        Criteria criteria = getSession().createCriteria(EventLogDTO.class)
                .add(Restrictions.eq("company.id", entityId))
                .addOrder(Order.desc("createDatetime"));

        return criteria.list();
    }

    public List<EventLogDTO> getEventLogByAffectedUserId(Integer userId) {
        List<Integer> messageIds = Arrays.asList(25, 9);

        DetachedCriteria query = DetachedCriteria.forClass(EventLogDTO.class, "event_log")
            .setProjection(Projections.max("event_log.createDatetime"))
            .add(Restrictions.eqProperty("event_log.eventLogMessage.id", "result.eventLogMessage.id"))
            .add(Restrictions.in("event_log.eventLogMessage.id", messageIds))
            .add(Restrictions.eq("event_log.jbillingTable.id", 10))
            .add(Restrictions.eq("event_log.affectedUser.id", userId));

        // criteria to get the max record by messageId 25 & 9
        Criteria criteria = getSession().createCriteria(EventLogDTO.class, "result")
            .add(Restrictions.in("eventLogMessage.id", messageIds))
            .add(Restrictions.eq("jbillingTable.id", 10))
            .add(Restrictions.eq("affectedUser.id", userId))
            .add(Subqueries.propertyEq("createDatetime", query));
        return criteria.list();
    }
}
