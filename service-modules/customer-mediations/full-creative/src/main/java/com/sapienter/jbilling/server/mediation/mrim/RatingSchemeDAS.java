package com.sapienter.jbilling.server.mediation.mrim;

import java.util.Map;

public interface RatingSchemeDAS {

	public Integer getRatingSchemeByMediationAndEntity(Integer mediationCfgId, Integer entityId);

	public Integer getGlobalRatingSchemeByEntity(Integer entityId);
	
	public Map<String, Integer> getMediationRatingSchemeById(Integer ratingSchemeId);
	
}
