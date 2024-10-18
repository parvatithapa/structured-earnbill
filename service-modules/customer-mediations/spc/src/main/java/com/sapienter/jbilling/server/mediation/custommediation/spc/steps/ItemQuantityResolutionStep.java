package com.sapienter.jbilling.server.mediation.custommediation.spc.steps;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Date;
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
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.TelstraRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.QuantityResolutionContext;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCMediationHelperService;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Context;

/**
 * @author Neelabh
 * @since Dec 19, 2018
 */
public class ItemQuantityResolutionStep extends AbstractMediationStep<MediationStepResult> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String ITEM_QUANTITY_NOT_FOUND_MESSAGE = "ERR-ITEM-QUANTITY-NOT-FOUND";
    private static final String ITEM_QUANTITY_NOT_RESOLVED_MESSAGE = "ERR-ITEM-QUANTITY-NOT-RESOLVED";
    private static final String ELAPSED_TIME_FOR_ITEM_QUANTITY_NOT_FOUND_MESSAGE = "ERR-ELAPSED-TIME-FOR-ITEM-QUANTITY-NOT-FOUND";
    private static final Integer ELAPSED_TIME_LENGTH_OPTUS_MOBILE = 6;
    private static final BigDecimal DIVISOR = new BigDecimal("100000");
    private static final String UNKNOWN_DATA_TO_RESOLVE_QUANTITY_MESSAGE = "ERR-UNKNOWN-DATA-TO-RESOLVE-QUANTITY";
    private static final String USAGE_IDENTIFIER_NOT_FOUND_MESSAGE = "ERR-USAGE-IDENTIFIER-NOT-FOUND";
    private static final String CALLED_NUMBER_NOT_FOUND_MESSAGE = "ERR-CALLED-NUMBER-NOT-FOUND";
    private static final String RATING_UNIT_NOT_FOUND_MESSAGE = "ERR-RATING-UNIT-NOT-FOUND";
    private static final String ITEM_NOT_FOUND_MESSAGE = "ERR-ITEM-NOT-FOUND";
    private static final String STANDARD_MMS_USAGE_IDENTIFIER = "MM1_M1000000";
    private static final String VMAIL_MMS_USAGE_IDENTIFIER = "MM7_M1300100";
    private static final String ROAMING_SMS_CDR_CODE = "888";
    private static final String ROAMING_DATA_CDR_CODE = "999";
    private static final String ZERO_CALLED_NUMBER = "0";
    private final String durationField;
    private final String dataDurationField;
    private final String msgDurationField;

    public ItemQuantityResolutionStep(String durationField) {
        this(durationField, SPCConstants.P2_DATA_VOLUME_EC_VOLUME, SPCConstants.P2_MESSAGES_EC_VOLUME);
    }

    public ItemQuantityResolutionStep(String durationField, String dataDurationField, String msgDurationField) {
        this.durationField = durationField;
        this.dataDurationField = dataDurationField;
        this.msgDurationField = msgDurationField;
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

    private BigDecimal convertSecondsToMinutes(Integer userId, String codeString, String assetNumber, BigDecimal durationInSeconds, Date eventDate) {
        SPCMediationHelperService spcMediationHelperService = Context.getBean(SPCMediationHelperService.class);
        QuantityResolutionContext quantityResolutionContext = spcMediationHelperService.constructQuantityResolutionContextForCodeString(userId,
                assetNumber, codeString, eventDate);
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
            BigDecimal originalQuantity = BigDecimal.ZERO;
            PricingField serviceType = PricingField.find(context.getPricingFields(), SPCConstants.SERVICE_TYPE);
            switch (MediationServiceType.fromServiceName(serviceType.getStrValue())) {
            case OPTUS_MOBILE:
                PricingField recordType = PricingField.find(context.getPricingFields(), SPCConstants.CDR_IDENTIFIER);
                if (OptusMobileRecord.DATA == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    PricingField usageIdentifierField = PricingField.find(context.getPricingFields(), SPCConstants.USAGE_IDENTIFIER);
                    if (usageIdentifierField == null || StringUtils.isBlank(usageIdentifierField.getStrValue())) {
                        result.addError(USAGE_IDENTIFIER_NOT_FOUND_MESSAGE);
                        return false;
                    }

                    PricingField calledNumberField = PricingField.find(context.getPricingFields(), SPCConstants.TO_NUMBER);
                    String calledNumberValue =
                            (calledNumberField != null && StringUtils.isNotBlank(calledNumberField.getStrValue())) ?
                                    calledNumberField.getStrValue().trim() : "";

                    String usageIdentifierValue = usageIdentifierField.getStrValue().trim();
                    if (STANDARD_MMS_USAGE_IDENTIFIER.equals(usageIdentifierValue)) {
                        //MMS Type Records
                        resolvedQuantity = BigDecimal.ONE;
                        originalQuantity = resolvedQuantity;
                    } else if (!VMAIL_MMS_USAGE_IDENTIFIER.equals(usageIdentifierValue) && ZERO_CALLED_NUMBER.equals(calledNumberValue)) {
                        //Data Type Records
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

                        if (StringUtils.isBlank(result.getItemId())) {
                            logger.debug("Data quantity cannot be resolved without product {}", result.getItemId());
                            result.addError(ITEM_NOT_FOUND_MESSAGE);
                            return false;
                        }
                        SPCMediationHelperService helperService = Context.getBean(SPCMediationHelperService.class);
                        Optional<BigDecimal> divisorQuantity =
                                helperService.getRatingUnitIncrementQuantityByItemId(Integer.valueOf(result.getItemId()));

                        if(!divisorQuantity.isPresent()) {
                            logger.debug("Rating unit is not configured on product {}", result.getItemId());
                            result.addError(RATING_UNIT_NOT_FOUND_MESSAGE);
                            return false;
                        }
                        resolvedQuantity = resolvedQuantity.divide(divisorQuantity.get(),
                                        Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    } else {
                        //Unknown Type Records
                        logger.debug("Quantity cannot be resolved with invalid usage identifier {}", usageIdentifierValue);
                        result.addError(UNKNOWN_DATA_TO_RESOLVE_QUANTITY_MESSAGE);
                        return false;
                    }
                } else if (OptusMobileRecord.CONTENT == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    resolvedQuantity = BigDecimal.ONE;
                    originalQuantity = resolvedQuantity;
                } else if (OptusMobileRecord.SMS == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    Optional<BigDecimal> smsQty = findDurationFromPricingField(durationField, context.getPricingFields());
                    if(!smsQty.isPresent()) {
                        result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                        return false;
                    }
                    resolvedQuantity = smsQty.get();
                    originalQuantity = resolvedQuantity;
                } else if (OptusMobileRecord.ROAM == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    PricingField calledNumberField = PricingField.find(context.getPricingFields(), SPCConstants.TO_NUMBER);
                    String calledNumber = calledNumberField != null ? calledNumberField.getStrValue().trim() : "";
                    if (calledNumber.startsWith(ROAMING_SMS_CDR_CODE)) {
                        //SMS Records
                        resolvedQuantity = BigDecimal.ONE;
                        originalQuantity = resolvedQuantity;
                    } else if (calledNumber.startsWith(ROAMING_DATA_CDR_CODE)) {
                        //DATA Records
                        String strDataUsage = StringUtils.removeStart(calledNumber, ROAMING_DATA_CDR_CODE);
                        originalQuantity = new BigDecimal(strDataUsage);

                        if (StringUtils.isBlank(result.getItemId())) {
                            logger.debug("Roaming data quantity cannot be resolved without product {}", result.getItemId());
                            result.addError(ITEM_NOT_FOUND_MESSAGE);
                            return false;
                        }

                        SPCMediationHelperService helperService = Context.getBean(SPCMediationHelperService.class);
                        Optional<BigDecimal> divisorQuantity =
                                helperService.getRatingUnitIncrementQuantityByItemId(Integer.valueOf(result.getItemId()));

                        if(!divisorQuantity.isPresent()) {
                            logger.debug("Rating unit is not configured on product {}", result.getItemId());
                            result.addError(RATING_UNIT_NOT_FOUND_MESSAGE);
                            return false;
                        }
                        resolvedQuantity = originalQuantity.divide(divisorQuantity.get(),
                                        Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    } else {
                        //ROAMING Voice Call
                        String elapsedTime = PricingField.find(context.getPricingFields(), durationField).getStrValue();
                        if(StringUtils.isBlank(elapsedTime) || elapsedTime.length() > ELAPSED_TIME_LENGTH_OPTUS_MOBILE) {
                            logger.debug("Quantity can not be resolved without proper elapsed time {}", elapsedTime);
                            result.addError(ELAPSED_TIME_FOR_ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                            return false;
                        }
                        resolvedQuantity = getTimeInSecondsForOptusMobileCallRecords(elapsedTime);
                        logger.debug("Elapsed time found {}, Quantity resolved in Seconds {}", elapsedTime, resolvedQuantity);
                        originalQuantity = resolvedQuantity;
                        resolvedQuantity = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                                assetNumberField.getStrValue(), resolvedQuantity, result.getEventDate());
                    }
                } else if (OptusMobileRecord.HOME == OptusMobileRecord.fromTypeCode(recordType.getStrValue())) {
                    String elapsedTime = PricingField.find(context.getPricingFields(), durationField).getStrValue();
                    if(StringUtils.isBlank(elapsedTime) || elapsedTime.length() > ELAPSED_TIME_LENGTH_OPTUS_MOBILE) {
                        logger.debug("Quantity can not be resolved without proper elapsed time {}", elapsedTime);
                        result.addError(ELAPSED_TIME_FOR_ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                        return false;
                    }
                    resolvedQuantity = getTimeInSecondsForOptusMobileCallRecords(elapsedTime);
                    logger.debug("Elapsed time found {}, Quantity resolved in Seconds {}", elapsedTime, resolvedQuantity);
                    originalQuantity = resolvedQuantity;
                    resolvedQuantity = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                            assetNumberField.getStrValue(), resolvedQuantity, result.getEventDate());
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
                        assetNumberField.getStrValue(), resolvedQuantity, result.getEventDate());
                break;
            case TELSTRA_FIXED_LINE_MONTHLY:
                Optional<BigDecimal> telstraFLmonthlyQty = findDurationFromPricingField(durationField, context.getPricingFields());
                if(!telstraFLmonthlyQty.isPresent()) {
                    result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                    return false;
                }
                resolvedQuantity = telstraFLmonthlyQty.get().abs();
                originalQuantity = resolvedQuantity;

                break;
            case TELSTRA_MOBILE_4G :
                List<PricingField> fields = context.getPricingFields();
                PricingField duration4GTelstra = PricingField.find(fields, durationField);
                if(null == duration4GTelstra) {
                    result.addError(ITEM_QUANTITY_NOT_FOUND_MESSAGE);
                    return false;
                }
                PricingField unitOfMeasure = PricingField.find(fields, SPCConstants.UNIT_OF_MEASURE);
                SPCMediationHelperService helperService = Context.getBean(SPCMediationHelperService.class);
                Optional<BigDecimal> divisorQuantity = helperService.getRatingUnitIncrementQuantityByItemId(Integer.valueOf(result.getItemId()));
                if ( TelstraRecord.BYTE.getTypeCode().equalsIgnoreCase(unitOfMeasure.getStrValue()) ) {
                    PricingField dataDuration4GTelstra = PricingField.find(fields,
                            StringUtils.isBlank(dataDurationField) ? SPCConstants.P2_DATA_VOLUME_EC_VOLUME : dataDurationField);
                    if(null == dataDuration4GTelstra) {
                        result.addError("ERR-ITEM-QUANTITY-FOR-DATA-NOT-FOUND");
                        return false;
                    }
                    resolvedQuantity = resolvedQuantity.add(getQuantityAsBigDecimal(dataDuration4GTelstra));
                    if (divisorQuantity.isPresent()) {
                        originalQuantity = resolvedQuantity;
                        resolvedQuantity = resolvedQuantity.divide(divisorQuantity.get(),
                                           Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
                    }
                } else if (TelstraRecord.MSG.getTypeCode().equalsIgnoreCase(unitOfMeasure.getStrValue())) {
                    PricingField msgDuration4GTelstra = PricingField.find(fields,
                            StringUtils.isBlank(msgDurationField) ? SPCConstants.P2_MESSAGES_EC_VOLUME : msgDurationField);
                    if(null == msgDuration4GTelstra) {
                        result.addError("ERR-ITEM-QUANTITY-FOR-MSG-NOT-FOUND");
                        return false;
                    }
                    String quantityStr  = msgDuration4GTelstra.getStrValue();
                    quantityStr         = quantityStr.replace(".", "");
                    int intQuantity     = Integer.parseInt(quantityStr);
                    resolvedQuantity    = BigDecimal.valueOf(intQuantity);
                    originalQuantity    = resolvedQuantity;
                }
                else {
                    String quantityStr  = duration4GTelstra.getStrValue();
                    quantityStr         =  quantityStr.replace(".", "");
                    int intQuantity     = Integer.parseInt(quantityStr);
                    resolvedQuantity    = BigDecimal.valueOf(intQuantity);
                    originalQuantity    = resolvedQuantity;
                    resolvedQuantity    = convertSecondsToMinutes(result.getUserId(), codeStringField.getStrValue(),
                                          assetNumberField.getStrValue(), resolvedQuantity, result.getEventDate());
                }
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
                                   assetNumberField.getStrValue(), resolvedQuantity, result.getEventDate());
                break;
            }
            logger.debug("original Quantity {} and resolved quantity {} for cdr key {}", originalQuantity,
                    resolvedQuantity, result.getCdrRecordKey());
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

    /**
     * This is helper method for Optus Mobile where the elapsedTime is in MMMMSS format.
     * Elapsed time = 006020 - call lasts for 1 hour and 20 seconds
     * Time in Seconds = 3620
     *
     * @param elapsedTime
     * @return Time in seconds
     */
    private BigDecimal getTimeInSecondsForOptusMobileCallRecords(String elapsedTime) {
        elapsedTime = appendZeroAtStart(elapsedTime);
        BigDecimal seconds = BigDecimal.ZERO;
        String minutesStr = elapsedTime.substring(0, 4);
        if (StringUtils.isNumeric(minutesStr)) {
            BigDecimal minutes = new BigDecimal(minutesStr);
            seconds = minutes.multiply(new BigDecimal(60));
        }
        String secondsStr = elapsedTime.substring(4, ELAPSED_TIME_LENGTH_OPTUS_MOBILE);
        if (StringUtils.isNumeric(secondsStr)) {
            BigDecimal inputSeconds = new BigDecimal(secondsStr);
            seconds = seconds.add(inputSeconds);
        }
        return seconds;
    }

    private String appendZeroAtStart(String elapsedTime) {
        StringBuilder sb = new StringBuilder(elapsedTime);
        if(sb.length() < ELAPSED_TIME_LENGTH_OPTUS_MOBILE) {
            while (sb.length() < ELAPSED_TIME_LENGTH_OPTUS_MOBILE) {
                sb.insert(0,  '0');
            }
        }
        return sb.toString();
    }
}
