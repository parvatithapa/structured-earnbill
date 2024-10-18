package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
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

    @Value("#{jobParameters['filePath']}")
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
            } else if (SPCConstants.TOTAL_CHARGES.equals(fieldName) ||
                    SPCConstants.AUD_TOTAL_CHARGES.equals(fieldName)) {
                fieldValue = new BigDecimal(fieldValue).setScale(2, RoundingMode.HALF_UP).toString();
            }
            record.addField(new PricingField(fieldName, fieldValue), false);
        }
        return record;
    }

    private void populateOptusFixedLinePricingField(FieldSet fieldSet, ICallDataRecord record) {
        String typeId = "";
        String jurisdiction = "";
        String codeString = "";

        for (String fieldName : fieldSet.getNames()) {
            if (SPCConstants.TYPE_ID_USG.equals(fieldName)) {
                typeId = fieldSet.readString(fieldName).trim();
            }
            if (SPCConstants.JURISDICTION_CODE.equals(fieldName)) {
                jurisdiction = String.valueOf(fieldSet.readInt(fieldName));
            }
        }

        if (resource != null && resource.contains(STN1)) {
            codeString = jurisdiction;
        } else if (resource != null && resource.contains(STN2)) {
            codeString = getJoinedString(COLON, CODE_STRING_PREFIX, typeId, jurisdiction);
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
        boolean isContentRecord = false;
        switch (OptusMobileRecord.fromTypeCode(fieldSet.readString(SPCConstants.CDR_IDENTIFIER))) {
        case HOME:
            codeString = getJoinedString(COLON,
                    fieldSet.readString(SPCConstants.CDR_IDENTIFIER),
                    fieldSet.readString(SPCConstants.CALL_DIRECTION),
                    fieldSet.readString(SPCConstants.CALLED_PLACE),
                    fieldSet.readString(SPCConstants.PRODUCT_PLAN_CODE),
                    fieldSet.readString(SPCConstants.CALL_TYPE));
            break;
        case ROAM:
            calledPlace =
            StringUtils.isEmpty(fieldSet.readString(SPCConstants.CALLED_PLACE).trim())
            ? "MOC" : "MTC"+COLON+fieldSet.readString(SPCConstants.CALLED_PLACE).trim();
            calledNumber =
                    StringUtils.isEmpty(fieldSet.readString(SPCConstants.TO_NUMBER).trim())
                    ? "0" : fieldSet.readString(SPCConstants.TO_NUMBER).substring(0, 3);
            codeString = getJoinedString(COLON,
                    fieldSet.readString(SPCConstants.CDR_IDENTIFIER),
                    calledPlace,
                    fieldSet.readString(SPCConstants.CALL_DIRECTION),
                    fieldSet.readString(SPCConstants.CALL_TYPE),
                    calledNumber);
            break;
        case SMS:
            codeString = getJoinedString(COLON,
                    fieldSet.readString(SPCConstants.CDR_IDENTIFIER),
                    fieldSet.readString(SPCConstants.PRODUCT_PLAN_CODE),
                    fieldSet.readString(SPCConstants.DIRECTION),
                    fieldSet.readString(SPCConstants.SMS_RELAXED_RULE),
                    fieldSet.readString(SPCConstants.TO_NUMBER).substring(0, 3));
            break;
        case CONTENT:
            codeString = getJoinedString(COLON,
                    fieldSet.readString(SPCConstants.CDR_IDENTIFIER),
                    fieldSet.readString(SPCConstants.CONTENT_CHARGE_TYPE),
                    fieldSet.readString(SPCConstants.CONTENT_DELIVERY_METHOD));
            isContentRecord = true;
            break;
        case DATA:
            String dataCalledNumber = fieldSet.readString(SPCConstants.TO_NUMBER);
            calledNumber =
                    (StringUtils.isEmpty(dataCalledNumber.trim()) || Integer.valueOf(dataCalledNumber) == 0)
                    ? "0" : dataCalledNumber.substring(0, 3);
            codeString = getJoinedString(COLON,
                    fieldSet.readString(SPCConstants.CDR_IDENTIFIER),
                    fieldSet.readString(SPCConstants.PRODUCT_PLAN_CODE),
                    fieldSet.readString(SPCConstants.USAGE_IDENTIFIER),
                    calledNumber,
                    fieldSet.readString(SPCConstants.CHARGE_OVERRIDE));
            break;
        default:
            logger.debug("Did not find matching record type to build code string");
            break;
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
        record.setKey(cdrEventId);
    }

    private void populateAaptVoipPricingField(FieldSet fieldSet, ICallDataRecord record) {
        String jurisdiction = "";
        String codeString = "";
        String usageType = "";
        String appendJurisdiction = "JUR";

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

        //Adding code-string pricing field
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);
        //Setting unique identifier of the call
        record.setKey(fieldSet.readString(SPCConstants.CDR_ID));
    }

    private void populateTelstraPricingField(FieldSet fieldSet, ICallDataRecord record) {
        String productBillingIdentifier = fieldSet.readString(SPCConstants.TELSTRA_PODUCT_BILLING_IDENTIFIER);
        String billingElementCode = fieldSet.readString(SPCConstants.TELSTRA_BILLING_ELEMENT_CODE);

        //Adding code-string pricing field
        record.addField(new PricingField(SPCConstants.CODE_STRING, (productBillingIdentifier + billingElementCode).trim()), false);
        //Adding cdr key
        String cdrkey = new StringBuilder().append(fieldSet.readString(SPCConstants.TELSTRA_RECORD_TYPE))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_EVENT_SEQ_NUMBER))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_EVET_FILE_INSTANCE_ID))
                .append("-")
                .append(fieldSet.readString(SPCConstants.TELSTRA_ORIGINATING_NUMBER))
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

}
