package com.sapienter.jbilling.server.mediation.converter.customMediations.dt.helper;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MediationHelperService {

	public Map<String, Object> resolveCustomerByExternalAccountIdentifier(Integer entityId, String assetField);
    public Map.Entry<Integer, String> resolveItemById(Integer entityId, String itemIdentifier, String extendedParams);

    public List<Map<String, Object>> getPreferencesByEntity(Integer entityId);
    
    public String getMediationJobLauncherByConfigId(Integer configId);
    public String getMediationCdrFolderByConfigId(Integer configId);

    public Integer getUserCompanyByUserId(Integer userId);
    public Integer getParentCompanyId(Integer entityId);
    public boolean isMediationConfigurationGlobal(Integer configId);
    public List<Integer> getChildEntitiesIds(Integer parentId);

    public void loadProductMapCaches(Integer entityId);
    public Set<String> productsToAggregate();
}
