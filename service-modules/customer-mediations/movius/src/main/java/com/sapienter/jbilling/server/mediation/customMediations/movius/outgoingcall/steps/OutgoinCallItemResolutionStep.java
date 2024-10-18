package com.sapienter.jbilling.server.mediation.customMediations.movius.outgoingcall.steps;

import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusMetaFieldName;
import com.sapienter.jbilling.server.mediation.customMediations.movius.MoviusUtil;
import com.sapienter.jbilling.server.util.Context;

public class OutgoinCallItemResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final String DESTINATION_NUMBER_FIELD_NAME = "Called Number";
    
    private static final Logger LOG = LoggerFactory.getLogger(OutgoinCallItemResolutionStep.class);
    
    
    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField calledNumberField = PricingField.find(context.getPricingFields(), DESTINATION_NUMBER_FIELD_NAME);
            PhoneNumberUtil phoneUtil = Context.getBean(MoviusUtil.PHONE_UTIL);
            String rawNumber = calledNumberField.getStrValue();
            if(!rawNumber.startsWith("+")) {
                rawNumber = "+".concat(rawNumber); 
            }
            PhoneNumber number = phoneUtil.parse(rawNumber, "");
            Map<String, String> metaFieldMap = MoviusUtil.getCompanyLevelMetaFieldValueByEntity(context.getEntityId());
            String countryCodeList = metaFieldMap.get(MoviusMetaFieldName.ANVEO_OUTGOING_CALL_COUNTRY_CODES.getFieldName());
            if(StringUtils.isEmpty(countryCodeList)) {
                result.addError("ITEM-NOT-RESOLVED");
                return false;
            }
            // Anveo item for Anveo specific rate card
            if(ArrayUtils.contains(countryCodeList.split(","), String.valueOf(number.getCountryCode()))) {
                Integer itemId = Integer.parseInt(metaFieldMap.get(MoviusMetaFieldName.ANVEO_CALL_ITEM_ID.getFieldName()));
                result.setItemId(itemId);
                return true;
            }
            
            // other country code will use out going call item and rate card
            Integer itemId = Integer.parseInt(metaFieldMap.get(MoviusMetaFieldName.TATA_CALL_ITEM_ID.getFieldName()));
            result.setItemId(itemId);
            return true;
        } catch (Exception ex) {
            result.addError("ERR-ITEM-NOT-RESOLVED");
            LOG.error(ex.getMessage(), ex);
            return false;
        }
    }

}
