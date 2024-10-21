package com.sapienter.jbilling.server.mediation.custommediation.spc.reader;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.mediation.ICallDataRecord;
import com.sapienter.jbilling.server.mediation.converter.common.Format;
import com.sapienter.jbilling.server.mediation.converter.common.reader.BasicMediationRecordLineConverter;
import com.sapienter.jbilling.server.mediation.custommediation.spc.CdrRecordType.EnginSConnectRecord;
import com.sapienter.jbilling.server.mediation.custommediation.spc.MediationServiceType;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;

/**
 * @author Neelabh Dubey
 * @since Jan 23, 2019
 */
public class SPCMediationRecordLineMapper extends BasicMediationRecordLineConverter {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String COLON = ":";
    private String strAAPTInternetEventDate = null;

    public ICallDataRecord mapLineToRecord(String line, Format format, MediationServiceType serviceType) {
        ICallDataRecord record = super.convertLineToRecord(line, format);
        //Custom logic to map CDR specific pricing fields
        switch (serviceType) {
        case ENGIN:
        case SCONNECT:
            populateEnginSConnectPricingFields(record, serviceType);
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
            populateAaptInternetUsagePricingFields(record);
            break;
        default:
            logger.debug("Did not find CDR resolver {} ", serviceType);
            break;
        }
        return record;
    }

    private void populateEnginSConnectPricingFields(ICallDataRecord record, MediationServiceType serviceType) {
        //Adding code-string pricing field
        PricingField callType = PricingField.find(record.getFields(), SPCConstants.CALL_TYPE);
        String codeString = callType != null ? callType.getStrValue() : null;
        if (EnginSConnectRecord.INTERNATIONAL.getTypeCode().equals(codeString)) {
            PricingField country = PricingField.find(record.getFields(), SPCConstants.COUNTRY);
            codeString = codeString + COLON + country.getStrValue();
        }
        if (MediationServiceType.ENGIN.equals(serviceType)) {
            codeString = "en"+ COLON + codeString;
        }else if(MediationServiceType.SCONNECT.equals(serviceType)){
            codeString = "sc"+ COLON + codeString;
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
        String referenceNumber = PricingField.find(record.getFields(), "P1_UDR_REF_NUM").getStrValue();
        String fromNumber = PricingField.find(record.getFields(), "P1_S_P_NUMBER_EC_ADDRESS").getStrValue();
        String toNumber = PricingField.find(record.getFields(), "P1_O_P_NUMBER_EC_ADDRESS").getStrValue();
        String eventTimeStamp = PricingField.find(record.getFields(), "P2_START_TIME_EC_TIMESTAMP").getStrValue();
        PricingField originatingDateField = PricingField.find(record.getFields(), SPCConstants.P1_INITIAL_START_TIME_EC_TIME_OFFSET);
        String telstraOffset = originatingDateField.getStrValue();
        
        if(NumberUtils.isCreatable(eventTimeStamp) && NumberUtils.isCreatable(telstraOffset)) {
        	SimpleDateFormat formatter = new SimpleDateFormat(SPCConstants.DATA_EVENT_DATE_FORMAT);
        	try {
        		Date eventTimeStampTemp = new Date(formatter.parse(eventTimeStamp).getTime() + Long.parseLong(telstraOffset) * 1000);
        		eventTimeStamp = formatter.format(eventTimeStampTemp);
			} catch (NumberFormatException | ParseException e) {
				logger.warn("Exception occurred while parsing event date ", e.getMessage());
			}
        	
        } 
        return referenceNumber+":"+fromNumber+":"+toNumber+":"+eventTimeStamp;
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

    private void populateAaptInternetUsagePricingFields(ICallDataRecord record) {
        String cdrRecordFirstField = PricingField.find(record.getFields(),"USER_NAME").getStrValue();
        if (cdrRecordFirstField.startsWith("*Daily usage report")) {
            this.strAAPTInternetEventDate = cdrRecordFirstField.substring(23, 33);
        } else {
            record.addField(new PricingField(SPCConstants.EVENT_DATE, strAAPTInternetEventDate), false);
            record.setKey(getJoinedString(COLON, cdrRecordFirstField, strAAPTInternetEventDate));
        }
    }
}
