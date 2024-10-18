package com.sapienter.jbilling.server.integration.db;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

/**
 *
 * @author krunal bhavsar
 *
 */
public class OutBoundInterchangeDAS extends AbstractDAS<OutBoundInterchange> {

    @SuppressWarnings("unchecked")
    public List<Integer> findAllFailedOutBoundInterchangeRequestIdsForEntity(Integer entityId, Integer retryCount) {
        Session session = getSession();
        Disjunction disjunction = Restrictions.disjunction();
        disjunction.add(Restrictions.or(Restrictions.lt("retryCount", retryCount)));
        disjunction.add(Restrictions.or(Restrictions.isNull("retryCount")));
        return session.createCriteria(getPersistentClass())
                .createAlias("company", "entity")
                .add(Restrictions.eq("entity.id", entityId))
                .add(Restrictions.eq("status", Status.FAILED))
                .add(disjunction)
                .setProjection(Projections.id())
                .addOrder(Order.asc("id"))
                .list();
    }

    public OutBoundInterchange findFailedFailedOutBoundInterchangeRequestWithLock(Integer id) {
        return (OutBoundInterchange) getSession().createCriteria(getPersistentClass())
                .add(Restrictions.idEq(id))
                .add(Restrictions.eq("status", Status.FAILED))
                .setLockMode(LockMode.PESSIMISTIC_WRITE)
                .uniqueResult();
    }
}
