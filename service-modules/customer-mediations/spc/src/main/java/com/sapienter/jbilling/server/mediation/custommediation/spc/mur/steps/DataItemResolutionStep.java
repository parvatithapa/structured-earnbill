package com.sapienter.jbilling.server.mediation.custommediation.spc.mur.steps;

import static com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants.DATA_ITEM_ID_FIELD_NAME;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;

@Component("optusMurDataItemResolutionStep")
class DataItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Autowired
    private SPCMediationHelperService service;

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            Integer entityId = context.getEntityId();
            Map<String, String> companyLevelMetaFieldMap = service.getMetaFieldsForEntity(entityId);
            String dataItemIdValue = companyLevelMetaFieldMap.get(DATA_ITEM_ID_FIELD_NAME);
            if(StringUtils.isEmpty(dataItemIdValue)) {
                logger.debug("Data Item Meta Field Value not set at company level meta field for entity {}", entityId);
                result.addError("DATA-TYPE-ITEM-ID-NOT-SET-ON-COMPANY-LEVEL-METAFIELD");
                return false;
            }
            if(!NumberUtils.isDigits(dataItemIdValue)) {
                logger.debug("Invalid {} Data type item id set for entity {}", dataItemIdValue, entityId);
                result.addError("INVALID-DATA-TYPE-ITEM-ID");
                return false;
            }
            Integer itemId = Integer.parseInt(dataItemIdValue);
            if(!service.isItemPresentForId(itemId)) {
                result.addError("ITEM-NOT-RESOLVED");
                return false;
            }
            logger.debug("Item {} Resolved for User {} of entity {}", itemId, result.getUserId(), entityId);
            result.setItemId(itemId);
            return true;
        } catch(Exception ex) {
            logger.error("Data Item Resolution Failed!", ex);
            result.addError("ERR-ITEM-NOT-RESOLVED");
            return false;
        }
    }

}
