package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.StringJoiner;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.beans.factory.annotation.Value;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.CallDataRecord;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.OptusMobileRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;

/**
 * @author Harshad
 * @since Dec 18, 2018
 */
public class SPCMediationRecordFieldSetMapper implements CustomFieldSetMapper<ICallDataRecord> {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String COLON = ":";
    private static final String PERIOD = ".";
    private static final String STN1 = "stn1";
    private static final String STN2 = "stn2";
    private static final String CODE_STRING_PREFIX = "INB";
    private static final String[] MOBILE_ORIGINATED_TYPE_IDS = {"000047","000055"};
    private static final String LOCAL_CALL_TYPE_CODE = "1";
    private static final String SPECIAL_AND_INTL_CALL_TYPE_CODE = "6";
    private static final String SMS_EVENT_TYPE_CODE = "00";
    private static final String ON_NET_ON_ACCOUNT_CALL = "11614";
    private static final String MMS_EVENT = "MMS";
    private static final String STANDARD_MMS_USAGE_IDENTIFIER = "MM1_M1000000";
    private static final String CALLED_NUMBER_FOR_INTL_MMS_STARTS = "11";
    private static final String CALL_DIRECTION_FOR_MOBILE_TO_LANDLINE = "1";
    private static final String CALL_DIRECTION_FOR_MOBILE_TO_MOBILE = "3";
    private static final String CODE_STRING_ERROR = "ERROR";

    @Value("#{stepExecutionContext['fileToRead']}")
    private String resource;

    @Override
    public ICallDataRecord mapLineToRecord(FieldSet fieldSet, MediationServiceType serviceType) {
        ICallDataRecord record = mapFieldSet(fieldSet);
        //Custom logic to map mediation service specific pricing fields
        switch (serviceType) {
        case OPTUS_FIXED_LINE:
            populateOptusFixedLinePricingField(fieldSet, record);
            break;
        case OPTUS_MOBILE:
            populateOptusMobilePricingField(fieldSet, record);
            break;
        case AAPT_VOIP_CTOP:
            populateAaptVoipPricingField(fieldSet, record);
            break;
        case TELSTRA_FIXED_LINE:
            populateTelstraPricingField(fieldSet, record);
            break;
        case TELSTRA_FIXED_LINE_MONTHLY:
            populateTelstraMonthlyPricingField(fieldSet, record);
            break;
        default:
            logger.debug("Did not find CDR resolver {} ", serviceType);
            break;
        }
        return record;
    }

    @Override
    public ICallDataRecord mapFieldSet(FieldSet fieldSet) {
        //Generic code to map all tokenized fields
        ICallDataRecord record = new CallDataRecord();
        for (String fieldName : fieldSet.getNames()) {
            //Converting few specific fields to appropriate data type
            String fieldValue = fieldSet.readString(fieldName).trim();
            if (SPCConstants.DURATION.equals(fieldName) ||
                    SPCConstants.OFF_PEAK_USAGE.equals(fieldName) ||
                    SPCConstants.OTHER_USAGE.equals(fieldName) ||
                    SPCConstants.PEAK_USAGE.equals(fieldName)) {
                fieldValue = String.valueOf(Integer.valueOf(fieldValue));
            }
            record.addField(new PricingField(fieldName, fieldValue), false);
        }
        return record;
    }

    private void populateOptusFixedLinePricingField(FieldSet fieldSet, ICallDataRecord record) {
        String typeId = "";
        String jurisdiction = "";
        String codeString = "";

        try {
            for (String fieldName : fieldSet.getNames()) {
                if (SPCConstants.TYPE_ID_USG.equals(fieldName)) {
                    typeId = fieldSet.readString(fieldName).trim();
                }
                if (SPCConstants.JURISDICTION_CODE.equals(fieldName)) {
                    jurisdiction = String.valueOf(fieldSet.readInt(fieldName));
                }
            }

            BigDecimal callCharges = BigDecimal.ZERO;
            String totalCharges = fieldSet.readString(SPCConstants.TOTAL_CHARGES);
            if(NumberUtils.isCreatable(StringUtils.stripStart(totalCharges,"0"))){
                callCharges = new BigDecimal(totalCharges).divide(new BigDecimal(100), 10, BigDecimal.ROUND_HALF_UP);
            }
            record.addField(new PricingField(SPCConstants.CALL_CHARGE, callCharges), false);
            if (resource != null && resource.contains(STN1)) {
                codeString = jurisdiction;
            } else if (resource != null && resource.contains(STN2)) {
                // If the calls are mobile originated to 1300 or 1800 calls only then
                // Jurisdiction Code should be appended to the Code String formation
                if(ArrayUtils.contains(MOBILE_ORIGINATED_TYPE_IDS, typeId)) {
                    codeString = getJoinedString(COLON, CODE_STRING_PREFIX, typeId, jurisdiction);
                } else {
                    codeString = getJoinedString(COLON, CODE_STRING_PREFIX, typeId);
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while constructing code string for Optus Fixed Line", ex);
            codeString = CODE_STRING_ERROR;
        }

        // Adding code-string pricing field
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);
        // Setting unique identifier of the call
        record.setKey(fieldSet.readString(SPCConstants.CDR_ID));
    }

    private void populateOptusMobilePricingField(FieldSet fieldSet, ICallDataRecord record) {
        //Code string logic
        String codeString = "";
        String calledPlace = "";
        String calledNumber = "";
        String productPlanCode = "";
        String callDirection = "";
        String recordType = "";
        String callType = "";
        boolean isContentRecord = false;
        try {
            switch (OptusMobileRecord.fromTypeCode(fieldSet.readString(SPCConstants.CDR_IDENTIFIER))) {
            case HOME:
                callType = fieldSet.readString(SPCConstants.CALL_TYPE).trim();
                recordType = fieldSet.readString(SPCConstants.CDR_IDENTIFIER).trim();
                calledPlace = fieldSet.readString(SPCConstants.CALLED_PLACE).trim();
                calledNumber = fieldSet.readString(SPCConstants.TO_NUMBER).trim();
                callDirection = fieldSet.readString(SPCConstants.CALL_DIRECTION).trim();
                productPlanCode = fieldSet.readString(SPCConstants.PRODUCT_PLAN_CODE).trim();
                String fromNumber = fieldSet.readString(SPCConstants.FROM_NUMBER).trim();
                String[] specialEvents = {"190 AIRTIME", "Connect2", "Dir Assist", "OPER.INQUIRY", "Operator",
                        "SENSIS 1234", "*188 Guide", "966 Zoo", "SurePage", "Telecard",
                        "INFORMATION", "Free Call"};
                String[] specialNumbers = {"000", "028", "029", "122", "130", "131", "133", "180"};
                String[] tollFreeNumbers = {"13", "18"};
                String[] MobToMobNumbers = {"11", "61", "04", "01"};

                if (productPlanCode.equals("IDDSP")) {
                    //Mobile to International Calls.
                    codeString = getJoinedString(COLON, recordType, productPlanCode, callDirection, callType, calledPlace);
                } else {
                    if (ArrayUtils.contains(specialEvents, calledPlace)) {
                        codeString = getJoinedString(COLON, recordType, callDirection, calledPlace, productPlanCode, callType);
                    } else if (StringUtils.length(calledNumber) > 4 && ON_NET_ON_ACCOUNT_CALL.equals(calledNumber.substring(0, 5))) {
                        //"On Net-On Account" Calls. Use first 5 digits of Called Number in Code String.
                        fromNumber = fromNumber.substring(0, 2);
                        calledNumber = calledNumber.substring(0, 5);
                        codeString = getJoinedString(COLON, recordType, productPlanCode, callDirection, callType, fromNumber, calledNumber);
                    } else if (LOCAL_CALL_TYPE_CODE.equals(callType) &&
                            callDirection.equalsIgnoreCase(CALL_DIRECTION_FOR_MOBILE_TO_LANDLINE) &&
                            ArrayUtils.contains(tollFreeNumbers, calledNumber.substring(0, 2))) {
                        //Calls to 1300/1800 Numbers for Call Type 1 and Call Direction 1. Use first 2 digits of called number in Code String.
                        calledNumber = calledNumber.substring(0, 2);
                        codeString = getJoinedString(COLON, recordType, productPlanCode, callDirection, callType, calledNumber);
                    } else if (LOCAL_CALL_TYPE_CODE.equals(callType) &&
                            callDirection.equalsIgnoreCase(CALL_DIRECTION_FOR_MOBILE_TO_MOBILE) &&
                            ArrayUtils.contains(MobToMobNumbers, calledNumber.substring(0, 2))) {
                        //Calls to Mobile Number For Call Type 1 and Call Direction 3
                        calledNumber = calledNumber.substring(0, 2);
                        codeString = getJoinedString(COLON, recordType, productPlanCode, callDirection, callType, calledNumber);
                    }else if (SPECIAL_AND_INTL_CALL_TYPE_CODE.equals(callType) &&
                            ArrayUtils.contains(specialNumbers, calledNumber.substring(0, 3))) {
                        //Mobile to Special/Fixed Calls. Use first 3 digits of called number in Code String.
                        calledNumber = calledNumber.substring(0, 3);
                        codeString = getJoinedString(COLON, recordType, productPlanCode, callDirection, callType, calledNumber);
                    } else {
                        codeString = getJoinedString(COLON, recordType, productPlanCode, callDirection, callType);
                    }
                }
                break;
            case ROAM:
                calledNumber = fieldSet.readString(SPCConstants.TO_NUMBER).trim();
                calledPlace = fieldSet.readString(SPCConstants.CALLED_PLACE).trim();
                recordType = fieldSet.readString(SPCConstants.CDR_IDENTIFIER).trim();
                String gsmServiceType = fieldSet.readString(SPCConstants.GSM_SERVICE_TYPE).trim();
                String calledPlaceCode = StringUtils.isEmpty(calledPlace) ? ":MOC:" : ":MTC:";

                if (StringUtils.length(calledNumber) > 2) {
                    //Use first 3 digits of called number
                    calledNumber = calledNumber.substring(0, 3);
                }
                codeString = getJoinedString(COLON, recordType, calledPlaceCode, gsmServiceType, calledNumber);
                break;
            case SMS:
                calledNumber = fieldSet.readString(SPCConstants.TO_NUMBER).trim();
                recordType = fieldSet.readString(SPCConstants.CDR_IDENTIFIER).trim();
                productPlanCode = fieldSet.readString(SPCConstants.PRODUCT_PLAN_CODE).trim();
                String direction = fieldSet.readString(SPCConstants.DIRECTION).trim();
                String eventType = fieldSet.readString(SPCConstants.EVENT_TYPE).trim();
                String smsRelaxedRule = fieldSet.readString(SPCConstants.SMS_RELAXED_RULE).trim();

                if (StringUtils.length(calledNumber) > 2) {
                    //Use first 3 digits of called number
                    calledNumber = calledNumber.substring(0, 3);
                }
                if (SMS_EVENT_TYPE_CODE.equals(eventType)) {
                    //The second last field is EventType and value will be zero.
                    codeString = getJoinedString(COLON, recordType, productPlanCode, direction, smsRelaxedRule, calledNumber);
                } else {
                    smsRelaxedRule = StringUtils.stripStart(smsRelaxedRule, "0");
                    codeString = getJoinedString(COLON, recordType, productPlanCode, direction, smsRelaxedRule, calledNumber);
                }
                break;
            case CONTENT:
                codeString = getJoinedString(COLON,
                        fieldSet.readString(SPCConstants.CDR_IDENTIFIER).trim(),
                        fieldSet.readString(SPCConstants.CONTENT_CHARGE_TYPE).trim(),
                        fieldSet.readString(SPCConstants.CONTENT_DELIVERY_METHOD).trim());
                isContentRecord = true;
                break;
            case DATA:
                calledNumber = fieldSet.readString(SPCConstants.TO_NUMBER).trim();
                calledPlace = fieldSet.readString(SPCConstants.CALLED_PLACE).trim();
                recordType = fieldSet.readString(SPCConstants.CDR_IDENTIFIER).trim();
                productPlanCode = fieldSet.readString(SPCConstants.PRODUCT_PLAN_CODE).trim();
                String chargeOverride = fieldSet.readString(SPCConstants.CHARGE_OVERRIDE).trim();
                String directionFlag = fieldSet.readString(SPCConstants.DIRECTION_FLAG).trim();
                String networkType = fieldSet.readString(SPCConstants.NETWORK_TYPE).trim();
                String usageIdentifier = fieldSet.readString(SPCConstants.USAGE_IDENTIFIER).trim();

                if (StringUtils.isNotEmpty(chargeOverride)) {
                    codeString = getJoinedString(COLON, recordType, productPlanCode, networkType, directionFlag, chargeOverride);
                } else if (STANDARD_MMS_USAGE_IDENTIFIER.equals(usageIdentifier)) {
                    if (StringUtils.isNotEmpty(calledNumber) && calledNumber.startsWith(CALLED_NUMBER_FOR_INTL_MMS_STARTS)) {
                        codeString = getJoinedString(COLON, recordType, productPlanCode, networkType, directionFlag, MMS_EVENT, "I");
                    } else {
                        codeString = getJoinedString(COLON, recordType, productPlanCode, networkType, directionFlag, MMS_EVENT);
                    }
                } else {
                    codeString = getJoinedString(COLON, recordType, productPlanCode, networkType, directionFlag);
                }
                break;
            default:
                logger.debug("Did not find matching record type to build code string");
                codeString = CODE_STRING_ERROR;
                break;
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while constructing code string for Optus Mobile", ex);
            codeString = CODE_STRING_ERROR;
        }

        logger.debug("Optus mobile code string {}", codeString.trim());
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString.trim()), false);

        //Creating unique identifier of the call and setting it as key
        String customKeySegment = (isContentRecord) ?
                fieldSet.readString(SPCConstants.CONTENT_TRANS_ID).trim() :
                    fieldSet.readString(SPCConstants.TO_NUMBER).trim();
        String cdrEventId = getJoinedString(PERIOD,
                 fieldSet.readString(SPCConstants.CDR_IDENTIFIER),
                 fieldSet.readString(SPCConstants.FROM_NUMBER).trim(),
                 customKeySegment,
                 fieldSet.readString(SPCConstants.EVENT_DATE));
        if(OptusMobileRecord.DATA.equals(OptusMobileRecord.fromTypeCode(fieldSet.readString(SPCConstants.CDR_IDENTIFIER)))) {
                cdrEventId = getJoinedString(PERIOD,cdrEventId,
                     fieldSet.readString(SPCConstants.SESSION_ID),
                     fieldSet.readString(SPCConstants.PEAK_USAGE),
                     fieldSet.readString(SPCConstants.OFF_PEAK_USAGE),
                     fieldSet.readString(SPCConstants.OTHER_USAGE));
         }
         record.setKey(cdrEventId);
    }

    private void populateAaptVoipPricingField(FieldSet fieldSet, ICallDataRecord record) {
        String jurisdiction = "";
        String codeString = "";
        String usageType = "";
        String appendJurisdiction = "JUR";

        try {
            for (String fieldName : fieldSet.getNames()) {
                if (SPCConstants.JURISDICTION_CODE.equals(fieldName)) {
                    jurisdiction = String.valueOf(fieldSet.readInt(fieldName));
                }
                if (SPCConstants.TYPE_ID_USG.equals(fieldName)) {
                    usageType = String.valueOf(fieldSet.readInt(fieldName));
                }
            }

            if(jurisdiction.equals("0")) {
                codeString = usageType;
            } else {
                codeString = getJoinedString(COLON, appendJurisdiction, jurisdiction);
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while constructing code string for AAPT VOIP", ex);
            codeString = CODE_STRING_ERROR;
        }

        //Adding code-string pricing field
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);
        //Setting unique identifier of the call
        record.setKey(fieldSet.readString(SPCConstants.CDR_ID));
    }

    private void populateTelstraPricingField(FieldSet fieldSet, ICallDataRecord record) {
    	
    	String productBillingIdentifier = fieldSet.readString(SPCConstants.TELSTRA_PODUCT_BILLING_IDENTIFIER);
        String billingElementCode = fieldSet.readString(SPCConstants.TELSTRA_BILLING_ELEMENT_CODE);
        String distanceRangeCode = fieldSet.readString(SPCConstants.TELSTRA_DISTANCE_RANGE_CODE);
        String originatingNumber = fieldSet.readString(SPCConstants.TELSTRA_ORIGINATING_NUMBER);
        String codeString = (productBillingIdentifier + billingElementCode).trim();
        if(StringUtils.isNotBlank(distanceRangeCode.trim())){
            codeString = getJoinedString(COLON, codeString, distanceRangeCode);
        }
        //Adding code-string pricing field
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);

        //Adding telstra asset number and call charges pricing fields
        String telstraAssetNumber = StringUtils.length(originatingNumber) > 11 ? originatingNumber.substring(2, 12) : "ERROR";
        record.addField(new PricingField(SPCConstants.TELSTRA_ASSET_NUMBER, telstraAssetNumber), false);
        BigDecimal callCharges = BigDecimal.ZERO;
        String amount = fieldSet.readString(SPCConstants.AMOUNT_TELSTRA_FIXED_LINE_MONTHLY);
        if(NumberUtils.isCreatable(StringUtils.stripStart(amount,"0"))){
            callCharges = new BigDecimal(amount).divide(new BigDecimal(10000000), 10, BigDecimal.ROUND_HALF_UP);
        }
        record.addField(new PricingField(SPCConstants.CALL_CHARGE,callCharges), false);
        //Adding cdr key
        String cdrkey = new StringBuilder().append(fieldSet.readString(SPCConstants.TELSTRA_RECORD_TYPE))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_EVENT_SEQ_NUMBER))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_EVET_FILE_INSTANCE_ID))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_INPUT_LINXONLINE_EBILL_FILE_ID))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_UNIT_OF_MEASURE_CODE))
                .append("-")
                .append(originatingNumber)
                .toString();
        
        record.setKey(cdrkey);
    }

    private String getJoinedString(String delimiter, String... values){
        StringJoiner strJoiner = new StringJoiner(delimiter);
        for (String string : values) {
            strJoiner.add(string);
        }
        return strJoiner.toString();
    }

    private void populateTelstraMonthlyPricingField(FieldSet fieldSet, ICallDataRecord record) {
        String productBillingId = fieldSet.readString(SPCConstants.PRODUCT_BILLING_ID);
        String billingElementCode = fieldSet.readString(SPCConstants.TELSTRA_BILLING_ELEMENT_CODE);

        //Adding code-string pricing field
        record.addField(new PricingField(SPCConstants.CODE_STRING, (productBillingId + billingElementCode).trim()), false);

        String productBillingIdentifier = fieldSet.readString(SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER);
        record.addField(new PricingField(SPCConstants.TELSTRA_FULL_NATIONAL_NUMBER, productBillingIdentifier.trim()), false);
        String cdrkey = getJoinedString("-", fieldSet.readString(SPCConstants.DOC_REF_NBR).trim(),
                fieldSet.readString(SPCConstants.INVOICE_RECORD_SEQUENCE_NBR).trim(),
                fieldSet.readString(SPCConstants.GIRN).trim(),
                fieldSet.readString(SPCConstants.START_DATE));

        record.setKey(cdrkey);

    }

}
