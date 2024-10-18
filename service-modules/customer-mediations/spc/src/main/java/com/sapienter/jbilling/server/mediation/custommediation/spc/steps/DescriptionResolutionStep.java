package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.TelstraRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.OptusMobileRecord;

/**
 * @author Neelabh
 * @since Dec 19, 2018
 */
public class DescriptionResolutionStep  extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private String sourceNumber;
    private String destinationNumber;

    public DescriptionResolutionStep(String sourceNumber, String destinationNumber) {
        this.sourceNumber = sourceNumber;
        this.destinationNumber = destinationNumber;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            String descriptionText = "";
            List<PricingField> fields = context.getPricingFields();
            PricingField mediationType = PricingField.find(fields, SPCConstants.SERVICE_TYPE);
            PricingField recordType = PricingField.find(fields, SPCConstants.CDR_IDENTIFIER);
            if (MediationServiceType.OPTUS_MOBILE == MediationServiceType.fromServiceName(mediationType.getStrValue()) &&
                    OptusMobileRecord.CONTENT == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                PricingField sourceField = PricingField.find(fields, sourceNumber);
                if(null == sourceField) {
                    result.addError("ERR-DESCRIPTION-NOT-FOUND");
                    return false;
                }
                result.setSource(sourceField.getStrValue());
                descriptionText = "Content originated from "+sourceField.getStrValue().trim();
            } else if (MediationServiceType.TELSTRA_MOBILE_4G == MediationServiceType.fromServiceName(mediationType.getStrValue())){
                PricingField field = PricingField.find(fields, SPCConstants.BILLING_NAME);
                PricingField callingField = PricingField.find(fields, sourceNumber);
                PricingField calledField = PricingField.find(fields, destinationNumber);
                result.setSource(callingField.getStrValue());
                result.setDestination(calledField.getStrValue());

                if(TelstraRecord.GPRS.getTypeCode().equalsIgnoreCase(field.getStrValue())){
                    descriptionText = "GPRS usage from "+ callingField.getStrValue();
                } else {
                    descriptionText = field.getStrValue() +" originated from "+callingField.getStrValue() + " to " + calledField.getStrValue();
                }
            }
            else if(MediationServiceType.fromServiceName(mediationType.getStrValue()) == MediationServiceType.TELSTRA_FIXED_LINE_MONTHLY ) {
                PricingField billingTransDesc = PricingField.find(fields, SPCConstants.BILLING_TRANS_DESC);
                PricingField transTypeDesc = PricingField.find(fields, SPCConstants.TRANS_TYPE_DESC);
                if(null == billingTransDesc || null == transTypeDesc) {
                    result.addError("ERR-DESCRIPTION-NOT-FOUND");
                    return false;
                }
                descriptionText = billingTransDesc.getStrValue().trim() + " " + transTypeDesc.getStrValue().trim();
            } else {
            	String calledFieldStr = "";
                PricingField callingField = PricingField.find(fields, sourceNumber);
                PricingField calledField = PricingField.find(fields, destinationNumber);
                if(null == callingField || null == calledField) {
                    result.addError("ERR-DESCRIPTION-NOT-FOUND");
                    return false;
                }
                result.setSource(callingField.getStrValue());
                if(!calledField.getStrValue().chars().allMatch(Character::isDigit)){
                	calledFieldStr = calledField.getStrValue().substring(2);
                } else {
                	calledFieldStr = calledField.getStrValue();
                }
                result.setDestination(calledFieldStr);
                descriptionText = "Call from " + callingField.getStrValue() + " to " + calledFieldStr;
            }
            result.setDescription(descriptionText);
            return true;
        } catch(Exception ex) {
            result.addError("ERR-DESCRIPTION-NOT-RESOLVED");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }

}
