package com.sapienter.jbilling.server.mediation.customMediations.movius.configurable.steps;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.MediationMRIMService;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.util.Context;

public class ItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {


	private static final Logger LOG = LoggerFactory.getLogger(ItemQuantityResolutionStep.class);

    private final String durationField;
    private final String cdrType;

    public ItemQuantityResolutionStep(String durationField, String cdrType) {
        this.durationField = durationField;
        this.cdrType = cdrType;
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {

            String cdrTypeField = PricingField.find(context.getPricingFields(), cdrType).getStrValue();
            //TODO : Write Custom logic to resolve quantity for sms details
            if(cdrTypeField.contains("outgoing-sms-details") || cdrTypeField.contains("outgoing-sms")
                    || cdrTypeField.contains("incoming-sms-details")) {
                result.setQuantity(BigDecimal.ONE);
                result.setOriginalQuantity(BigDecimal.ONE);
                return true;
            }

            MediationMRIMService mrimService = Context.getBean("mediationMRIMServiceImpl");

            PricingField duration = PricingField.find(context.getPricingFields(), durationField);
            if(null == duration) {
                result.addError("ERR-ITEM-QUANTITY-NOT-FOUND");
                return false;
            }
            int intQuantity = Integer.parseInt(duration.getStrValue());
            BigDecimal quantity = mrimService.getQuantity(result.getMediationCfgId() , result.getjBillingCompanyId(), intQuantity);

            if(quantity.compareTo(BigDecimal.ZERO) < 0) {
                result.addError("ERR-ITEM-QUANTITY-NEGATIVE");
                return false;
            }

            result.setQuantity(quantity);
            result.setOriginalQuantity(new BigDecimal(intQuantity));
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-QUANTITY-RESOLUTION");
            LOG.error(ex.getMessage(), ex);
            return false;
        }

    }
}
