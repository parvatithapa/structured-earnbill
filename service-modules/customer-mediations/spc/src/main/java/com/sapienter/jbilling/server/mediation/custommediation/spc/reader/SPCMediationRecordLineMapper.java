package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import java.util.Date;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import static com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.EnginSConnectRecord;

/**
 * @author Neelabh Dubey
 * @since Jan 23, 2019
 */
public class SPCMediationRecordLineMapper extends BasicMediationRecordLineConverter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String COLON = ":";
    String strDateTime = null;

    public ICallDataRecord mapLineToRecord(String line, Format format, MediationServiceType serviceType) {
        ICallDataRecord record = super.convertLineToRecord(line, format);
        //Custom logic to map CDR specific pricing fields
        switch (serviceType) {
            case ENGIN:
            case SCONNECT:
                populateEnginSConnectPricingFields(record);
                break;
            case TELSTRA_MOBILE_4G:
                populateTelstra4GMobiletPricingFields(record);
                break;
            case SERVICE_ELEMENTS_VOICE:
                populateServiceElementVoicePricingFields(record);
                break;
            case SERVICE_ELEMENTS_DATA:
                populateServiceElementsDataPricingFields(record);
                break;
            case SCONNECT_DATA:
                populateVocusInternetPricingFields(record);
                break;
            case AAPT_INTERNET_USAGE:
                populateAaptInternetUsagePricingFields(record, strDateTime);
                break;
            default:
                logger.debug("Did not find CDR resolver {} ", serviceType);
                break;
        }
        return record;
    }

    private void populateEnginSConnectPricingFields(ICallDataRecord record) {
        //Adding code-string pricing field
        PricingField callType = PricingField.find(record.getFields(), SPCConstants.CALL_TYPE);
        String codeString = callType != null ? callType.getStrValue() : null;
        if (EnginSConnectRecord.INTERNATIONAL.getTypeCode().equals(codeString)) {
            PricingField country = PricingField.find(record.getFields(), SPCConstants.COUNTRY);
            codeString = codeString + COLON + country.getStrValue();
        }
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);
        //Setting unique identifier of the call
        PricingField cdrId = PricingField.find(record.getFields(), SPCConstants.CDR_ID);
        record.setKey(cdrId.getStrValue());
    }

    private void populateTelstra4GMobiletPricingFields(ICallDataRecord record) {
        String codeString = buildCodeStringForTelstra4G(record);
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);
        record.setKey(createKeyForTelstra(record));
    }
    
    private void populateVocusInternetPricingFields(ICallDataRecord record) {
        String accountSessionId = PricingField.find(record.getFields(),"ACCT_SESSION_ID").getStrValue();
        record.setKey(accountSessionId);
    }

    private String createKeyForTelstra(ICallDataRecord record) {
        String fromNumber = PricingField.find(record.getFields(), "P1_S_P_NUMBER_EC_ADDRESS").getStrValue();
        String toNumber = PricingField.find(record.getFields(), "P1_O_P_NUMBER_EC_ADDRESS").getStrValue();
        String eventTimeStamp = PricingField.find(record.getFields(), "P2_START_TIME_EC_TIMESTAMP").getStrValue();
        return fromNumber+":"+toNumber+":"+eventTimeStamp;
    }

    private String buildCodeStringForTelstra4G(ICallDataRecord record) {
        PricingField p2TraficInfoECZNpKey = PricingField.find(record.getFields(), "P2_TARIFF_INFO_EC_ZN_PKEY");
        PricingField p2RoutingNumberECAddress = PricingField.find(record.getFields(), "P2_ROUTING_NUMBER_EC_ADDRESS");
        PricingField p2TrafficInfoECSNpKey = PricingField.find(record.getFields(), "P2_TARIFF_INFO_EC_SN_PKEY");
        PricingField p1CallTypeInfo = PricingField.find(record.getFields(), "P1_BASIC_CALL_TYPE_INFO_EC_CALL_TYPE");

        List<String> csFieldsList = new ArrayList<>();
        csFieldsList.add(null!=p2TraficInfoECZNpKey ? p2TraficInfoECZNpKey.getStrValue().trim() : "");
        csFieldsList.add(null!=p2RoutingNumberECAddress ? p2RoutingNumberECAddress.getStrValue().trim() : "");
        csFieldsList.add(null!=p2TrafficInfoECSNpKey ? p2TrafficInfoECSNpKey.getStrValue().trim() : "");
        csFieldsList.add(null!=p1CallTypeInfo ? p1CallTypeInfo.getStrValue().trim() : "");

        StringJoiner telstra4GCodeString = new StringJoiner(":");
        for (String value : csFieldsList) {
            telstra4GCodeString.add(value);
        }
        return telstra4GCodeString.toString();
    }

    private void populateServiceElementVoicePricingFields(ICallDataRecord record) {
        //Adding code-string pricing field
        PricingField callType = PricingField.find(record.getFields(), SPCConstants.CALL_TYPE);
        String codeString = callType != null && StringUtils.isNotBlank(callType.getStrValue()) ? callType.getStrValue() : "";
        //Call Type is used as code string
        record.addField(new PricingField(SPCConstants.CODE_STRING, codeString), false);
        //Setting unique identifier of the call
        String cdrId = getJoinedString(COLON,
                            PricingField.find(record.getFields(), SPCConstants.FROM_NUMBER).getStrValue(),
                            PricingField.find(record.getFields(), SPCConstants.TO_NUMBER).getStrValue(),
                            PricingField.find(record.getFields(), SPCConstants.EVENT_DATE).getStrValue());
        record.setKey(cdrId);
    }

    private void populateServiceElementsDataPricingFields(ICallDataRecord record) {
        String userName = PricingField.find(record.getFields(),SPCConstants.USER_NAME).getStrValue();
        String eventDate = PricingField.find(record.getFields(),SPCConstants.EVENT_DATE).getStrValue();
        String download = PricingField.find(record.getFields(),SPCConstants.DOWNLOAD).getStrValue();
        String upload = PricingField.find(record.getFields(),SPCConstants.UPLOAD).getStrValue();
        record.setKey(getJoinedString(COLON, userName, eventDate, download, upload));
    }

    private String getJoinedString(String delimiter, String... values){
        StringJoiner strJoiner = new StringJoiner(delimiter);
        for (String string : values) {
            strJoiner.add(string);
        }
        return strJoiner.toString();
    }

    private void populateAaptInternetUsagePricingFields(ICallDataRecord record, String strDateTime) {
        String cdrRecordFirstField = PricingField.find(record.getFields(),"USER_NAME").getStrValue();
        if (cdrRecordFirstField.startsWith("*Generating at:")) {
            String dateField = cdrRecordFirstField.substring(16, 44);
            if(null == strDateTime) {
                this.strDateTime = dateField;
            }
        }
        record.addField(new PricingField(SPCConstants.EVENT_DATE, strDateTime), false);
        if(!(cdrRecordFirstField.startsWith("*Generating"))) {
            record.setKey(cdrRecordFirstField+":"+strDateTime);
        }
    }
}
