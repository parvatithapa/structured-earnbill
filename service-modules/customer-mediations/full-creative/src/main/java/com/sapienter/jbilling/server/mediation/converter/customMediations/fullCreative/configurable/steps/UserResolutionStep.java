package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.configurable.steps;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.helper.service.MediationHelperService;

public class UserResolutionStep extends AbstractMediationStep<MediationStepResult>  {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String AGENT_LOGIN_FILED_NAME = "Agent_Login";
    private static final String TALK_TIME_FIELD_NAME = "Talk Time(s)";

    private String assetFieldName;
    private MediationHelperService mediationHelperService;

    public UserResolutionStep(String assetFieldName, MediationHelperService mediationHelperService) {
        Assert.hasLength(assetFieldName, "assetFieldName can not be null and empty!");
        Assert.notNull(mediationHelperService,"mediationHelperService can not be null!");
        this.assetFieldName = assetFieldName;
        this.mediationHelperService = mediationHelperService;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            List<PricingField> fields = context.getPricingFields();
            PricingField assetField = PricingField.find(fields, assetFieldName);
            String agentLogin = PricingField.find(fields, AGENT_LOGIN_FILED_NAME).getStrValue();
            Integer talkTime = PricingField.find(fields, TALK_TIME_FIELD_NAME).getIntValue();
            if (assetField != null) {
                if ( StringUtils.isEmpty(agentLogin) || "null".equalsIgnoreCase(agentLogin.trim()) ) {
                    result.setDone(true);
                    result.addError("ERR-NO-AGENT-SPECIFIED-CALL-NOT-ANSWERED");
                    logger.error("No agent specified, call not answered");
                    return false;
                }
                else if(talkTime <= 0){
                    result.setDone(true);
                    result.addError("ERR-TALK-TIME-LESS-OR-EQUAL-TO-ZERO-CALL-NOT-ANSWERED");
                    logger.error("Talk Time less than or equal to zero, call not answered");
                    return false;
                }
                else {
                    Map<String, Object> userDTOMap = MediationCacheManager.resolveUserByAssetField(context.getEntityId(), assetField.getStrValue());
                    if (userDTOMap.isEmpty()) {
                        boolean isAssetPresent = mediationHelperService.doesAssetIdentifierExist(assetField.getStrValue());
                        if (isAssetPresent) {
                            result.setDone(true);
                            result.addError("ERR-ASSET-NOT-ASSIGNED-TO-ANY-CUSTOMER");
                            logger.error("Asset {} found but is not assigned to any customer", assetField);
                            return false;
                        } else {
                            result.setDone(true);
                            result.addError("ERR-ASSET-NOT-FOUND");
                            logger.error("Asset {} not found", assetField);
                            return false;
                        }
                    }
                    result.setUserId((Integer)userDTOMap.get(MediationStepResult.USER_ID));
                    result.setCurrencyId((Integer)userDTOMap.get(MediationStepResult.CURRENCY_ID));
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            result.addError("ERR-USER-NOT-FOUND");
            logger.error("Error in UserResolutionStep", e);
            return false;
        }
    }



}
