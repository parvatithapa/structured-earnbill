package com.sapienter.jbilling.server.mediation.sapphire.cdr.resolution.steps;

import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME;
import static com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationConstants.CDR_TYPE;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.sapphire.SapphireMediationHelperService;

public class ProductResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private SapphireMediationHelperService service;

    public ProductResolutionStep(SapphireMediationHelperService service) {
        this.service = service;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String cdrType = context.getPricingField(CDR_TYPE).getStrValue();
            Integer userId = result.getUserId();
            if(null == userId) {
                logger.debug("item id can not be resolved without user");
                result.addError("ITEM-NOT-RESOLVED");
                return false;
            }
            String dataTableName = service.getMetaFieldsForEntity(context.getEntityId()).get(ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME);

            if (!service.isAccountTypeAvailable(dataTableName, userId)) {
                result.addError("ITEM-NOT-AVAILABLE-FOR-ACCOUNT-TYPE");
                return false;
            }

            Optional<Integer> itemId = service.getItemIdByUserAndCdrType(dataTableName, userId, cdrType);
            if(!itemId.isPresent()) {
                logger.debug("Item not found for user {} for cdr type {}", userId, cdrType);
                result.addError("ITEM-NOT-FOUND");
                return false;
            }
            logger.debug("Item id {} for user {} for cdr type {}", itemId.get(), userId, cdrType);
            result.setItemId(itemId.get());
            return true;

        } catch(Exception ex) {
            logger.error("ProductResolution failed!", ex);
            result.addError("ERROR-ITEM-NOT-RESOLVED");
            return false;
        }
    }

}
