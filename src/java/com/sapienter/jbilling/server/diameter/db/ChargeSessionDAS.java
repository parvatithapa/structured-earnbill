package com.sapienter.jbilling.server.diameter.db;

import org.hibernate.criterion.Restrictions;

import com.sapienter.jbilling.server.util.db.AbstractDAS;

public class ChargeSessionDAS extends AbstractDAS<ChargeSessionDTO> {

	public ChargeSessionDAS() {
		super();
	}
	
	public ChargeSessionDTO findByToken(String sessionId) {
		return (ChargeSessionDTO) getSession().createCriteria(ChargeSessionDTO.class)
				.add(Restrictions.eq("sessionId", sessionId))
				.uniqueResult();
	}

}
