package com.sapienter.jbilling.server.mediation;

import java.math.BigDecimal;

public interface MediationMRIMService {
	BigDecimal getQuantity(Integer mediationConfigurationId, Integer entityId, Integer quantity);
}
