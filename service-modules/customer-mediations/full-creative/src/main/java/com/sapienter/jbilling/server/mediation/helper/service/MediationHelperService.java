package com.sapienter.jbilling.server.mediation.helper.service;

import java.util.List;
import java.util.Map;

public interface MediationHelperService {

	public Map<String, Object> resolveUserByAssetField(Integer entityId, String assetField);
	
    public List<Map<String, Object>> getPreferencesByEntity(Integer entityId);
    
    public String getMediationJobLauncherByConfigId(Integer configId);
	
    public Integer getUserCompanyByUserId(Integer userId);
	public Boolean doesAssetIdentifierExist(String assetIdentifier);
	public Integer getParentCompanyId(Integer entityId);
	public boolean isProductVisibleToCompany(Integer itemId, Integer entityId,Integer parentId);
	public boolean isMediationConfigurationGlobal(Integer configId);
	public List<Integer> getChildEntitiesIds(Integer parentId);
	public Map<String, String> getCompanyLevelMetaFieldValueByEntity(Integer entityId);
	public boolean isTablePresent(String tableName);
	public Map<String, Object> getCityStateCountryByNPANXX(String [] NPANXX, String tableName);
}
