package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative.validation.steps;

import java.lang.invoke.MethodHandles;
import java.util.List;

import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.cache.MediationCacheManager;
import com.sapienter.jbilling.server.mediation.converter.common.steps.IMediationStepValidation;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;

public class CdrTypeValidationStep implements IMediationStepValidation {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String directionFieldName;

    public CdrTypeValidationStep(String directionFieldName) {
        this.directionFieldName = directionFieldName;
    }

    @Override
    public boolean isValid(ICallDataRecord record, MediationStepResult result) {
        try {
            List<PricingField> fields = record.getFields();
            String direction = PricingField.find(fields, directionFieldName).getStrValue();
            List<String> directions = MediationCacheManager.getAllCRDDirectionsForEntity(record.getEntityId());
            if(!directions.contains(direction)) {
                result.setDone(true);
                result.addError("ERR-UNKNOWN-CALL-TYPE");
                logger.error("Unknown Call Type");
                return false;
            }

            result.setCdrType(direction);
            return true;
        } catch(Exception ex) {
            logger.error("Error in CdrTypeValidationStep", ex);
            result.addError("ERR-CDR-VALIDATION");
            return false;
        }
    }

}
