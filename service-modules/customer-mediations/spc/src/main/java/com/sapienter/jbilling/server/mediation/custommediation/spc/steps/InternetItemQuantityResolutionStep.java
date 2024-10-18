package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.util.Constants;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Neelabh
 * @since Mar 14, 2019
 */
public class InternetItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String downloadField;
    private final String uploadField;
    private SPCMediationHelperService service;

    public InternetItemQuantityResolutionStep(String downloadField, String uploadField, SPCMediationHelperService service) {
        this.downloadField = downloadField;
        this.uploadField = uploadField;
        this.service= service;
    }

    private Optional<BigDecimal> findDataUsageFromPricingField(String usageFieldName, List<PricingField> pricingFields) {
        PricingField qtyField = PricingField.find(pricingFields, usageFieldName);
        if(null == qtyField) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(qtyField.getStrValue()));
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            BigDecimal chargeableUsage = BigDecimal.ZERO;
            BigDecimal originalUsage = BigDecimal.ZERO;

            Optional<BigDecimal> divisorQuantity =
                    service.getRatingUnitIncrementQuantityByItemId(Integer.valueOf(result.getItemId()));

            if(!divisorQuantity.isPresent()) {
                result.addError("ERR-RATING-UNIT-NOT-FOUND");
                return false;
            }

            Optional<BigDecimal> uploadUsage = findDataUsageFromPricingField(uploadField, context.getPricingFields());
            Optional<BigDecimal> downloadUsage = findDataUsageFromPricingField(downloadField, context.getPricingFields());

            PricingField usageType = PricingField.find(context.getPricingFields(), SPCConstants.INTERNET_USAGE_CHARGEABLE_UNIT);
            switch (CdrRecordType.InternetDataUsage.fromUsageCode(usageType.getStrValue())) {
                case DOWNLOAD:
                    originalUsage = downloadUsage.isPresent() ? downloadUsage.get() : BigDecimal.ZERO;
                    chargeableUsage = originalUsage;
                    break;
                case UPLOAD:
                    originalUsage = uploadUsage.isPresent() ? uploadUsage.get() : BigDecimal.ZERO;
                    chargeableUsage = originalUsage;
                    break;
                default:
                    originalUsage = originalUsage.add(uploadUsage.isPresent() ? uploadUsage.get() : BigDecimal.ZERO);
                    originalUsage = originalUsage.add(downloadUsage.isPresent() ? downloadUsage.get() : BigDecimal.ZERO);
                    chargeableUsage = originalUsage;
                    break;
            }

            BigDecimal resolvedQuantity = chargeableUsage.divide(divisorQuantity.get(),
                    Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);

            if(BigDecimal.ZERO.compareTo(resolvedQuantity) > 0) {
                result.addError("ERR-DATA-USAGE-NEGATIVE");
                return false;
            }

            result.setQuantity(resolvedQuantity);
            result.setOriginalQuantity(originalUsage);
            return true;
        } catch(Exception ex) {
            result.addError("ERR-DATA-USAGE-RESOLUTION");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
