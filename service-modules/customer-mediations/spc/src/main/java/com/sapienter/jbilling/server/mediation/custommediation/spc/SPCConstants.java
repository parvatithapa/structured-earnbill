package com.sapienter.jbilling.server.mediation.custommediation.spc;

/**
 * This is SPC specific constant class.
 */
public final class SPCConstants {

    //Constants used in SPC Mediation
    public static final String FROM_NUMBER                  = "SERVICE_NUMBER";
    public static final String TO_NUMBER                    = "POINT_TARGET";
    public static final String DATE_FORMAT                  = "dd-MM-yyyy HH:mm:ss";
    public static final String TIMESTAMP                    = "Timestamp";
    public static final String OPTUS_FIXED_LINE_QUANTITY    = "Optus Fixed Line Quantity";
    public static final String SERVICE_TYPE                 = "SERVICE_TYPE";
    public static final String CDR_IDENTIFIER               = "CDR_IDENTIFIER";
    public static final String CDR_ID                       = "CDR_ID";
    public static final String CODE_STRING                  = "CODE_STRING";
    public static final String TARIFF_CODE                  = "TARIFF_CODE";
    public static final String ASSET_NUMBER                 = "asset number";
    public static final String PRODUCT_CODE                 = "PRODUCT_CODE";
    public static final String PLAN_RATING                  = "Plan Rating";
    public static final String DURATION                     = "DURATION";
    public static final String EVENT_DATE                   = "EVENT_DATE";
    public static final Integer MONTHLY                     = Integer.valueOf(700);
    public static final String CALL_TYPE                    = "CALL_TYPE";
    public static final String COUNTRY                      = "COUNTRY";
    public static final String DIRECTION                    = "DIRECTION";
    public static final String CALL_DIRECTION               = "CALL_DIRECTION";
    public static final String CALLED_PLACE                 = "CALLED_PLACE";
    public static final String PRODUCT_PLAN_CODE            = "PRODUCT_PLAN_CODE";
    public static final String SMS_RELAXED_RULE             = "SMS_RELAXED_RULE";
    public static final String EVENT_TYPE                   = "EVENT_TYPE";
    public static final String CONTENT_CHARGE_TYPE          = "CONTENT_CHARGE_TYPE";
    public static final String CONTENT_DELIVERY_METHOD      = "CONTENT_DELIVERY_METHOD";
    public static final String USAGE_IDENTIFIER             = "USAGE_IDENTIFIER";
    public static final String CHARGE_OVERRIDE              = "CHARGE_OVERRIDE";
    public static final String TYPE_ID_USG                  = "TYPE_ID_USG";
    public static final String JURISDICTION_CODE            = "JURISDICTION_CODE";
    public static final String P1_S_P_NUMBER_EC_ADDRESS     = "P1_S_P_NUMBER_EC_ADDRESS";
    public static final String P2_DURATION_EC_VOLUME        = "P2_DURATION_EC_VOLUME";
    public static final String P1_O_P_NUMBER_EC_ADDRESS     = "P1_O_P_NUMBER_EC_ADDRESS";
    public static final String PEAK_USAGE                   = "PEAK_USAGE";
    public static final String OFF_PEAK_USAGE               = "OFF_PEAK_USAGE";
    public static final String OTHER_USAGE                  = "OTHER_USAGE";
    public static final String CONTENT_TRANS_ID             = "CONTENT_TRANS_ID";
    public static final String AUD_TOTAL_CHARGES            = "AUD_TOTAL_CHARGES";
    public static final String TOTAL_CHARGES                = "TOTAL_CHARGES";
    public static final String SE_VOICE_DATE_FORMAT         = "yyyy-MM-dd HH:mm:ss";
    public static final String ENGIN_SCON_TEL4G_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String START_DATETIME               = "START_DATETIME";
    public static final String END_DATETIME                 = "END_DATETIME";
    public static final String PLAN_NAME                    = "PLAN_NAME";
    public static final String CALL_CHARGE                  = "CALL_CHARGE";
    public static final String P2_START_TIME_EC_TIMESTAMP   = "P2_START_TIME_EC_TIMESTAMP";

    public static final String INTERNET_USAGE_ITEM_ID           = "INTERNET_USAGE_ITEM_ID";
    public static final String INTERNET_TECHNOLOGY_TYPE         = "INTERNET_TECHNOLOGY_TYPE";
    public static final String INTERNET_USAGE_CHARGEABLE_UNIT   = "INTERNET_USAGE_CHARGEABLE_UNIT";

    //Constants used in SPCMediationConfiguration
    public static final String SPC_PRICING_RESOLUTION_STEP      = "spcPricingResolutionStep";
    public static final String DEFAULT_PRICING_RESOLUTION_STEP  = "DefaultPricingResolutionStep";

    // Constants for Optus Mur
    public static final String DATA_ITEM_ID_FIELD_NAME    = "Set Data Type Item Id";
    public static final String DATA_EVENT_DATE_FIELD_NAME = "Start Date and Time";
    public static final String DATA_EVENT_DATE_FORMAT     = "yyyyMMddhhmmss";
    public static final String OPTUS_MUR_JOB_NAME         = "optusMurMediationJob";
    public static final String OPTUS_MUR_RECYCLE_JOB_NAME = "optusMurRecycleMediationJob";
    public static final String OPTUS_MUR_READER           = "optusMurReader";
    public static final String OPTUS_MUR_PROCESSOR        = "optusMurCdrProcessor";
    public static final String OPTUS_MUR_LINE_MAPPER      = "optusMurLineMapper";
    public static final String JMR_DEFAULT_WRITER_BEAN    = "jmrDefaultWriter";

    // Telstra Mediation file constants
    public static final String TELSTRA_PODUCT_BILLING_IDENTIFIER = "Product Billing Identifier";
    public static final String TELSTRA_BILLING_ELEMENT_CODE      = "Billing Element Code";
    public static final String TELSTRA_RECORD_TYPE               = "Interface Record Type";
    public static final String TELSTRA_EVET_FILE_INSTANCE_ID     = "Event File Instance Id";
    public static final String TELSTRA_EVENT_SEQ_NUMBER          = "Event Record Sequence Number";
    public static final String TELSTRA_ORIGINATING_DATE          = "Originating Date";
    public static final String TELSTRA_FULL_NATIONAL_NUMBER      = "Full National Number";
    public static final String TELSTRA_DATE_FORMAT               = "yyyyMMddHH:mm:ss";
    public static final String TELSTRA_ORIGINATING_NUMBER        = "Originating Number";
    public static final String TELSTRA_DESTINATION_NUMBER        = "Destination Number";
    public static final String TELSTRA_QUANTITY                  = "Quantity";
    
    //Vocus Internet Usage
    public static final String ACCT_STOP_TIME                                      = "ACCT_STOP_TIME";
    public static final String ACCT_START_TIME                                     = "ACCT_START_TIME";
    public static final String ACCT_SESSION_TIME                                   = "ACCT_SESSION_TIME";
    public static final String SCONNECT_DATA_DATE_TIME_FORMAT                     = "dd/MM/yyyy HH:mm";
    public static final String ACCT_INPUT_OCTETS                                   = "ACCT_INPUT_OCTETS";
    public static final String ACCT_OUTPUT_OCTETS                                  = "ACCT_OUTPUT_OCTETS";

    /**
     *  Company level meta-field names defined based on Internet Carrier types
     */
    public static final String COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID  = "AAPT Internet Item Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID  = "SConnect Internet Item Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID    = "Service Element Internet Item Id";

    /**
     *  Plan level meta-field names defined for Internet Technology (ADSL OR NBN)
     *  And Quantity Resolution Unit (Download, Upload and Total). These two
     *  meta-fields will be of Enumeration type.
     */
    public static final String PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT = "Quantity Resolution Unit";
    public static final String PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE = "Internet Technology";

    // Service Element Internet file constants
    public static final String USER_NAME                = "USER_NAME";
    public static final String SE_DATA_DATE_FORMAT      = "yyyy-MM-dd";
    public static final String DOWNLOAD                 = "DOWNLOAD";
    public static final String UPLOAD                   = "UPLOAD";

    //AAPT Internet Usage CONSTANT
    public static final String USAGE_UPLOAD                                       = "USAGE_UPLOAD";
    public static final String USAGE_DOWNLOAD                                     = "USAGE_DOWNLOAD";
    public static final String AAPT_INTERNET_DATE_TIME_FORMAT                     = "EEE MMM dd HH:mm:ss zzz yyyy";

    public static final String SPC_COLUMN_BEFORE_TAX                   = "Ex GST Amount";
    public static final String SPC_COLUMN_TAX                          = "GST";
    public static final String SPC_COLUMN_AFTER_TAX                    = "Inc GST Amount";

    private SPCConstants() {
        throw new IllegalStateException("Constants class");
    }

}
