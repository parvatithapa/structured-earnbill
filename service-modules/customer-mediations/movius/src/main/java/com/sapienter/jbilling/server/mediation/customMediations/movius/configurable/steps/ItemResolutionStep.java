package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;

public class ItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

	private static final Logger LOG = LoggerFactory.getLogger(ItemResolutionStep.class);

    
    private final String itemIdMetaFieldName;
    
    public ItemResolutionStep(String itemIdMetaFieldName) {
        this.itemIdMetaFieldName = itemIdMetaFieldName;
    }
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            Map<String, String> metaFieldMap = MoviusUtil.getCompanyLevelMetaFieldValueByEntity(context.getEntityId());
            if(MoviusUtil.isEmpty(metaFieldMap)) {
                Integer parentId = MoviusUtil.getParentEntityIdForGivenEntity(context.getEntityId());
                if(null == parentId) {
                    return addValidation(result);
                }
                metaFieldMap = MoviusUtil.getCompanyLevelMetaFieldValueByEntity(parentId);
            }
            if(MoviusUtil.isEmpty(metaFieldMap)) {
                return addValidation(result);
            }
            result.setItemId(Integer.parseInt(metaFieldMap.get(itemIdMetaFieldName)));
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-NOT-RESOLVED");
            LOG.error(ex.getMessage(), ex);
            return false;
        }
    }
    
    private boolean addValidation(MediationStepResult result) {
        result.addError("ITEM-ID-NOT-CONFIGURED-ON-COMPANY-LEVEL-META-FIELD");
        return false;
    }

}
