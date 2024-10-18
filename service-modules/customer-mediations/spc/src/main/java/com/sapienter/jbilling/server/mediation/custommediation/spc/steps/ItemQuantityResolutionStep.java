package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.converter.common.processor.MediationStepContext;
import com.sapienter.jbilling.server.mediation.converter.common.steps.AbstractMediationStep;
import com.sapienter.jbilling.server.mediation.converter.common.steps.MediationStepResult;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.OptusMobileRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.QuantityResolutionContext;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.util.Context;

/**
 * @author Neelabh
 * @since Dec 19, 2018
 */
public class ItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ITEM_QUANTITY_NOT_FOUND_MESSAGE = "ERR-ITEM-QUANTITY-NOT-FOUND";
    private static final String ITEM_QUANTITY_NOT_RESOLVED_MESSAGE = "ERR-ITEM-QUANTITY-NOT-RESOLVED";
    private static final BigDecimal DIVISOR = new BigDecimal("100000");
    private final String durationField;

    public ItemQuantityResolutionStep(String durationField) {
        this.durationField = durationField;
    }

    private Optional<BigDecimal> findDurationFromPricingField(String durationFieldName, List<PricingField> pricingFields) {
        PricingField duration = PricingField.find(pricingFields, durationFieldName);
        if(null == duration) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(duration.getStrValue()));
    }

    private BigDecimal getQuantityAsBigDecimal(PricingField pricingFieldQty) {
        return pricingFieldQty != null && StringUtils.isNotBlank(pricingFieldQty.getStrValue())
                ? new BigDecimal(pricingFieldQty.getStrValue().trim()) : BigDecimal.ZERO;
    }

    private BigDecimal convertSecondsToMinutes(Integer userId, String codeString, String assetNumber, BigDecimal durationInSeconds) {
        SPCMediationHelperService spcMediationHelperService = Context.getBean(SPCMediationHelperService.class);
        QuantityResolutionContext quantityResolutionContext = spcMediationHelperService.constructQuantityResolutionContextForCodeString(userId,
                assetNumber, codeString);
        return quantityResolutionContext.resolveQuantity(durationInSeconds);
    }

    @Override
    public boolean executeStep(MediationStepContext context) {
        MediationStepResult result = context.getResult();
        try {
            PricingField assetNumberField = PricingField.find(context.getPricingFields(), SPCConstants.ASSET_NUMBER);
            PricingField codeStringField = PricingField.find(context.getPricingFields(), SPCConstants.CODE_STRING);
            if(null == assetNumberField ||
                    StringUtils.isEmpty(assetNumberField.getStrValue())) {
                logger.debug("Quantity can not be resolved without asset number");
                result.addError(ITEM_QUANTITY_NOT_RESOLVED_MESSAGE);
                return false;
            }

            if(null == codeStringField ||
                    StringUtils.isEmpty(codeStringField.getStrValue())) {
                logger.debug("Quantity can not be resolved without code string");
                result.addError(ITEM_QUANTITY_NOT_RESOLVED_MESSAGE);
                return false;
            }
            BigDecimal resolvedQuantity = BigDecimal.ZERO;
            BigDecimal originalQuantity;
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            switch (MediationServiceType.fromServiceName(serviceType.getStrValue())) {
            case OPTUS_MOBILE:
                PricingField recordType = PricingField.find(context.getPricingFields(), SPCConstants.CDR_IDENTIFIER);
                if (OptusMobileRecord.DATA == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    PricingField peakUsageField = PricingField.find(context.getPricingFields(), SPCConstants.PEAK_USAGE);
                    PricingField offPeakUsageField = PricingField.find(context.getPricingFields(), SPCConstants.OFF_PEAK_USAGE);
                    PricingField otherUsageField = PricingField.find(context.getPricingFields(), SPCConstants.OTHER_USAGE);
                    if (peakUsageField == null && offPeakUsageField == null && otherUsageField == null) {
                        result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                        return false;
                    }
                    resolvedQuantity = resolvedQuantity.add(getQuantityAsBigDecimal(peakUsageField));
                    resolvedQuantity = resolvedQuantity.add(getQuantityAsBigDecimal(offPeakUsageField));
                    resolvedQuantity = resolvedQuantity.add(getQuantityAsBigDecimal(otherUsageField));
                    originalQuantity = resolvedQuantity;
                } else if (OptusMobileRecord.CONTENT == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    resolvedQuantity = BigDecimal.ONE;
                    originalQuantity = resolvedQuantity;
                } else {
                    Optional<BigDecimal> optusMQty = findDurationFromPricingField(durationField, context.getPricingFields());
                    if(!optusMQty.isPresent()) {
                        result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                        return false;
                    }
                    resolvedQuantity = optusMQty.get();
                    originalQuantity = resolvedQuantity;
                    if (!OptusMobileRecord.SMS.equals(OptusMobileRecord.fromTypeCode(recordType.getStrValue()))) {
                        resolvedQuantity = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                                assetNumberField.getStrValue(), resolvedQuantity);
                    }
                }
                break;
            case TELSTRA_FIXED_LINE:
                Optional<BigDecimal> telstraFLQty = findDurationFromPricingField(durationField, context.getPricingFields());
                if(!telstraFLQty.isPresent()) {
                    result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                    return false;
                }
                resolvedQuantity = telstraFLQty.get().divide(DIVISOR);
                originalQuantity = resolvedQuantity;
                resolvedQuantity = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                        assetNumberField.getStrValue(), resolvedQuantity);
                break;
            case TELSTRA_MOBILE_4G :
                PricingField duration4GTelstra = PricingField.find(context.getPricingFields(), durationField);
                if(null == duration4GTelstra) {
                    result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                    return false;
                }
                String quantityStr= duration4GTelstra.getStrValue();
                quantityStr=  quantityStr.contains(".") ? quantityStr.replace(".", "") : quantityStr;
                int intQuantity = Integer.parseInt(quantityStr);
                resolvedQuantity = BigDecimal.valueOf(intQuantity);
                originalQuantity = resolvedQuantity;
                resolvedQuantity = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                        assetNumberField.getStrValue(), resolvedQuantity);
                break;
            default:
                Optional<BigDecimal> quantity = findDurationFromPricingField(durationField, context.getPricingFields());
                if(!quantity.isPresent()) {
                    result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                    return false;
                }
                resolvedQuantity = quantity.get();
                originalQuantity = resolvedQuantity;
                resolvedQuantity = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                        assetNumberField.getStrValue(), resolvedQuantity);
                logger.debug("original Quantity {} and resolved quantity {} for cdr key {}", originalQuantity,
                        resolvedQuantity, result.getCdrRecordKey());
                break;
            }

            if(BigDecimal.ZERO.compareTo(resolvedQuantity) > 0) {
                result.addError("ERR-ITEM-QUANTITY-NEGATIVE");
                logger.debug("negative quantity resolved for cdr key {}", result.getCdrRecordKey());
                return false;
            }

            result.setQuantity(resolvedQuantity);
            result.setOriginalQuantity(originalQuantity);
            return true;
        } catch(Exception ex) {
            result.addError("ERR-ITEM-QUANTITY-RESOLUTION");
            logger.error(ex.getMessage(), ex);
            return false;
        }
    }
}
