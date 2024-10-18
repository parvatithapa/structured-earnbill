package com.sapienter.jbilling.server.mediation.customMediations.movius;

import java.util.List;
import java.util.Map;

public interface MoviusHelperService {
    String BEAN_NAME = "moviusHelperServiceBean";
    Map<String, Integer> resolveUserIdByOrgId(Integer entityId, String orgId);
    List<Integer> getAllChildEntityForGivenEntity(Integer entityId);
    Map<String, String> getMetaFieldsForEntity(Integer entityId);
}
