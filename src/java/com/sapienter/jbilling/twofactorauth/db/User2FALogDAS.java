package com.sapienter.jbilling.twofactorauth.db;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class User2FALogDAS extends AbstractDAS<User2FALogDTO> {

    public User2FALogDTO findBySessionIdAndUserId(String sessionId, Integer userId) {
        return (User2FALogDTO) getSession().createCriteria(User2FALogDTO.class)
                .add(Restrictions.eq("sessionId", sessionId))
                .add(Restrictions.eq("userId", userId))
                .uniqueResult();
    }

    public User2FALogDTO findByUserId(Integer userId) {
        return (User2FALogDTO) getSession().createCriteria(User2FALogDTO.class)
                .add(Restrictions.eq("userId", userId))
                .addOrder(Order.asc("timestamp"))
                .setFirstResult(0)
                .setMaxResults(1)
                .uniqueResult();
    }

    public User2FALogDTO findBySessionId(String sessionId) {
        return (User2FALogDTO) getSession().createCriteria(User2FALogDTO.class)
                .add(Restrictions.eq("sessionId", sessionId))
                .uniqueResult();
    }
}
