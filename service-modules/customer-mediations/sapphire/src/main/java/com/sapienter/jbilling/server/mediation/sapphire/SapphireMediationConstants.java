package com.sapienter.jbilling.server.mediation.sapphire;

import java.math.BigDecimal;


public class SapphireMediationConstants {

    private SapphireMediationConstants() {}

    public static final String INCOMING_CALL_CDR_TYPE     = "Incoming Call";
    public static final String OUT_GOING_CALL_CDR_TYPE    = "Out Going Call";
    public static final String FORWARDED_CALL_CDR_TYPE    = "Forwarded Call";
    public static final String ON_NET_CALL_CDR_TYPE       = "On Net Call";
    public static final String UNKNOWN_CALL_CDR_TYPE      = "unknown";
    public static final String LONG_CALL_CDR_TYPE         = "Long Call";
    public static final String AMBIGUOUS_CDR_TYPE         = "ambiguous (out going or on net)";
    public static final String CDR_TYPE                   = "Cdr Type";
    public static final String REST_OF_WORLD              = "Rest Of World";

    // pricing fields
    public static final String TRUNK_GROUP_ID            = "TrunkGroupId";
    public static final String RELAESE_TIME              = "ReleaseTime";
    public static final String CONNECT_TIME              = "ConnectTime";
    public static final String REQUESTED_ADDR            = "RequestedAddr";
    public static final String DEST_ADDR                 = "DestAddr";
    public static final String DURATION                  = "duration";
    public static final String OTHER_CARRIER_NAME        = "OTHERS";
    public static final String CALLING_PARTY_ADDR        = "CallingPartyAddr";
    public static final String CHARGE_ADDR               = "ChargeAddr";
    public static final String SEQUENCE_NUM              = "seqnum";
    public static final String LAST_REDIRECTING_ADDR     = "LastRedirectingAddr";
    public static final String CALL_TYPE                 = "Call Type";
    public static final String OFF_PEAK                  = "Off Peak";
    public static final String COUNTRY_CODE              = "Country Code";
    public static final String CARRIER_NAME              = "Carrier Name";
    public static final String LOCATION                  = "Location";
    public static final String HAS_FORWARDED_TAG         = "idForwarded";
    public static final String NATIONAL                  = "NATIONAL";
    public static final String INTERNATIONAL             = "INTERNATIONAL";
    public static final String MOBILE                    = "MOBILE";
    public static final String LANDLINE                  = "LANDLINE";
    public static final String SATELLITE                 = "SATELLITE";
    public static final String NGN                       = "NGN";
    public static final String ORIGINAL_QUANTITY         = "Original Quantity";
    public static final String CDR_CALL_TYPE             = "Cdr Call Type";
    public static final BigDecimal SECONDS               = new BigDecimal("60");

    public static final String CARRIER_TABLE_FIELD_NAME               = "Carrier Map";
    public static final String ACCOUNT_TYPE_ITEM_TABLE_FIELD_NAME     = "Account Type Item Map";
    public static final String PEAK_FIELD_NAME                        = "Peak (format [HH:mm-HH:mm])";
    public static final String HOLIDAY_DATA_TABLE_NAME                = "Holiday Data Table Name";
    public static final String SATELLITE_COUNTRY_CODE_DATA_TABLE_NAME = "Satellite Country Code Table Name";

    public static final String PLUS_PREFIX               = "+";
    public static final String UNDERSCORE                = "_";

    public static final String INCOMING_ITEM_ID  = "InComing Item Id";
    public static final String OUTGOING_ITEM_ID  = "OutGoing Item Id";
    public static final String ONNET_ITEM_ID     = "On net Item Id";
    public static final String FORWARDED_ITEM_ID = "Forwarded Item Id";

    public static final String DESCRIPTION_FORMAT   = "Call from [%s] to [%s]";
    public static final String DATE_FORMAT          = "yyyy-MM-dd HH:mm:ss";
    public static final String OFF_PEAK_DATE_FORMAT = "yyyy-MM-dd HH:mm";
    public static final String HOLIDAY_DATE_FORMAT  = "yyyy-MM-dd";

    // Mediation config
    public static final String JOB_NAME                = "sapphireMediationJob";
    public static final String CDR_CONVERTOR           = "cdrConvertor";
    public static final String READER                  = "sapphireMediationReader";
    public static final String PROCESSOR               = "sapphireMediationProcessor";
    public static final String WRITER                  = "sapphireMediationWriter";
    public static final String RECYCLE_JOB_NAME        = "sapphireRecycleJob";
    public static final String CDR_RESOLVER            = "sapphirecdrResolver";
    public static final String JMR_DEFAULT_WRITER_BEAN = "jmrDefaultWriter";
}
