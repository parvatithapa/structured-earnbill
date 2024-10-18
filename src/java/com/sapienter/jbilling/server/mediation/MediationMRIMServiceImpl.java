package com.sapienter.jbilling.server.mediation;

import java.math.BigDecimal;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sapienter.jbilling.server.pricing.RatingSchemeBL;


@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
public class MediationMRIMServiceImpl implements MediationMRIMService {

	@Override
	public BigDecimal getQuantity(Integer mediationConfigurationId, Integer entityId, Integer quantity) {
		Integer ratingSchemeId = RatingSchemeBL.getRatingSchemeIdForMediation(mediationConfigurationId, entityId);
		return RatingSchemeBL.getQuantity(ratingSchemeId, quantity);
	}
}
