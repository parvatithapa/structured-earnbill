package com.sapienter.jbilling.auth;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

@Component
public class RefreshTokenDAS extends AbstractDAS<RefreshTokenDTO> {

	public RefreshTokenDTO findByUserId(Integer userId) {
		return (RefreshTokenDTO) getSession().createCriteria(getPersistentClass(), "refreshToken")
				.add(Restrictions.eq("refreshToken.userId", userId))
				.uniqueResult();
	}

	public RefreshTokenDTO findByToken(String refreshToken) {
		return (RefreshTokenDTO) getSession().createCriteria(getPersistentClass(), "refreshToken")
				.add(Restrictions.eq("refreshToken.token", refreshToken))
				.uniqueResult();
	}
}