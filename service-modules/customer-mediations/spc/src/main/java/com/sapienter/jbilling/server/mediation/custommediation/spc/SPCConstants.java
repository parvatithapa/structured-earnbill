package com.sapienter.jbilling.server.mediation.custommediation.spc;

import java.util.Arrays;

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
    public static final String P2_DATA_VOLUME_EC_VOLUME     = "P2_DATA_VOLUME_EC_VOLUME";
    public static final String P2_MESSAGES_EC_VOLUME        = "P2_MESSAGES_EC_VOLUME";
    public static final String P3_ROUNDED_VOLUME_EC_VOLUME  = "P3_ROUNDED_VOLUME_EC_VOLUME";
    public static final String P3_RATED_VOLUME_EC_VOLUME    = "P3_RATED_VOLUME_EC_VOLUME";
    public static final String AMOUNT_TELSTRA_4G_MOBILE     = "P3_RATED_FLAT_AMOUNT_EC_AMOUNT";
    public static final String AMOUNT_TELSTRA_FIXED_LINE_MONTHLY = "Amount";
    public static final String BILLING_NAME                 = "P2_TARIFF_INFO_EC_SN_PKEY";
    public static final String UNIT_OF_MEASURE              = "P3_RATED_VOLUME_EC_UOM_PKEY";
    public static final String DISABLE_TAX                  = "P3_CHARGE_INFO_EC_DISABLE_TAX";
    public static final String P1_O_P_NUMBER_EC_ADDRESS     = "P1_O_P_NUMBER_EC_ADDRESS";
    public static final String PEAK_USAGE                   = "PEAK_USAGE";
    public static final String OFF_PEAK_USAGE               = "OFF_PEAK_USAGE";
    public static final String OTHER_USAGE                  = "OTHER_USAGE";
    public static final String CONTENT_TRANS_ID             = "CONTENT_TRANS_ID";
    public static final String TOTAL_CHARGES                = "TOTAL_CHARGES";
    public static final String TOTAL_TAX                    = "TAX";
    public static final String SE_VOICE_DATE_FORMAT         = "yyyy-MM-dd HH:mm:ss";
    public static final String ENGIN_SCON_TEL4G_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String START_DATETIME               = "START_DATETIME";
    public static final String END_DATETIME                 = "END_DATETIME";
    public static final String PLAN_NAME                    = "PLAN_NAME";
    public static final String CALL_CHARGE                  = "CALL_CHARGE";
    public static final String P1_INITIAL_START_TIME_EC_TIMESTAMP   = "P1_INITIAL_START_TIME_EC_TIMESTAMP";
    public static final String P1_INITIAL_START_TIME_EC_TIME_OFFSET   = "P1_INITIAL_START_TIME_EC_TIME_OFFSET";
    public static final String PURCHASE_ORDER_ID            = "PURCHASE_ORDER_ID";
    public static final String NUMBER_OF_DAYS_TO_BACK_DATED_EVENTS = "Number of days to back dated events";
    public static final String ACTIVE_SINCE_DATE            = "activeSinceDate";
    public static final String ACTIVE_UNTIL_DATE            = "activeUntilDate";
    public static final String DIRECTION_FLAG               = "DIRECTION_FLAG";
    public static final String NETWORK_TYPE                 = "NETWORK_TYPE";
    public static final String GSM_SERVICE_TYPE             = "GSM_SERVICE_TYPE";
    public static final String PF_ASSET_NUMBER              = "asset+number";
    public static final String AMOUNT_CHARGED               = "AMOUNT_CHARGED";
    public static final String PLAN_ID                      = "PLAN_ID";
    public static final String CALL_DURATION                = "Call+Duration";
    public static final String UNIT_OF_MEASURE_CODE         = "Unit+of+Measure+Code";
    public static final String DISTANCE_RANGE_CODE          = "Distance+Range+Code";
    public static final String AMOUNT                       = "Amount";

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
    public static final String TELSTRA_INPUT_LINXONLINE_EBILL_FILE_ID          = "Input LinxOnline eBill File Id";
    public static final String TELSTRA_UNIT_OF_MEASURE_CODE                    = "Unit of Measure Code";
    public static final String TELSTRA_ORIGINATING_DATE          = "Originating Date";
    public static final String TELSTRA_FULL_NATIONAL_NUMBER      = "Full National Number";
    public static final String TELSTRA_DATE_FORMAT               = "yyyyMMddHH:mm:ss";
    public static final String TELSTRA_ORIGINATING_NUMBER        = "Originating Number";
    public static final String TELSTRA_DESTINATION_NUMBER        = "Destination Number";
    public static final String TELSTRA_QUANTITY                  = "Quantity";
    public static final String TELSTRA_DISTANCE_RANGE_CODE       = "Distance Range Code";
    public static final String TELSTRA_ASSET_NUMBER              = "Asset Number";

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
    public static final String COMPANY_LEVEL_MF_NAME_FOR_CUSTOMER_CARE_NUMBER_ITEM_ID = "Customer Care Number Item Id";

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
    public static final String AAPT_INTERNET_DATE_TIME_FORMAT                     = "yyyy-MM-dd";

    public static final String SPC_COLUMN_BEFORE_TAX                   = "Ex GST Amount";
    public static final String SPC_COLUMN_TAX                          = "GST";
    public static final String SPC_COLUMN_AFTER_TAX                    = "Inc GST Amount";

    // Constants of SPC Paper delivery
    public static final String PO_BOX = "PO BOX";
    public static final String SUB_PREMISES = "Sub Premises";
    public static final String STREET_NUMBER = "Street Number";
    public static final String STREET_NAME = "Street Name";
    public static final String STREET_TYPE = "Street Type";
    public static final String CITY = "City";
    public static final String STATE = "State";
    public static final String POST_CODE = "Post Code";
    public static final Integer SPC_BILLING_ADDRESS_GROUP_ID = 23;
    public static final String SEPARATOR = " ";
    public static final String AGL_INVOICE = "agl_invoice";
    public static final String SPC_INVOICE = "";

    // Telstra Monthly Invoice
    public static final String SECTION_TYPE = "Section Type";
    public static final String GIRN = "GIRN";
    public static final String START_DATE = "Start Date";
    public static final String END_DATE = "End Date";
    public static final String BILLING_TRANS_DESC = "Billing Trans Desc";
    public static final String TRANS_TYPE_DESC = "Trans Type Desc";
    public static final String PRODUCT_BILLING_ID = "Product Billing ID";
    public static final String DOC_REF_NBR = "Doc Ref Nbr";
    public static final String INVOICE_RECORD_SEQUENCE_NBR = "Invoice record sequence Nbr";

    public static final String CDR_TYPE_OCD = "OCD";
    public static final String CDR_TYPE_SED = "SED";
    public static final String CSG_CONTRIBUTION = "CSG CONTRIBUTION";
    public static final String SERVICE_ID = "ServiceId";
    public static final String FREE_USAGE_CONSUMPTION_PARAM = "usageConsumption";
    public static final String FREE_USAGE_LEFT_PARAM = "freeUsageLeft";
    public static final String PLAN_NAME_PARAM = "planName";
    public static final String PLAN_CODE_PARAM = "planCode";
    public static final String PLAN_ID_PARAM = "planId";
    public static final String SERVICE_IDS_PARAM = "serviceIds";
    public static final String DATA_BOOST_PARAM = "dataBoost";
    public static final String FREE_USAGES_POOL_QUANTITY = "freeUsagesPoolQuantity";
    public static final String PERCENTAGE_CONSUMPTION_PARAM = "percentageConsumption";
    public static final String NOTIFICATION_TYPE_PARAM = "type";
    public static final String CURRENT_USAGE_POOL_NAME_PARAM = "currentUsagePoolName";
    public static final String USER_ID_PARAM = "user_id";
    public static final String FIRST_NAME_PARAM = "first_name";
    public static final String LAST_NAME_PARAM = "last_name";
    public static final String EMAIL_CONTENT_PARAM = "emailContent";
    public static final String CRM_ACCOUNT_ID = "crmAccountID";
    public static final String CRM_ORDER_ID = "crmOrderID";
    public static final String CRM_SERVICE_ID = "crmServiceID";
    public static final String VENDER_REFEREBCE = "vendorReference";
    public static final String WOOKIE_CREDIT_POOL_SMS_TABLE = "route_70_wookie_credit_pool_alert";
    public static final String WOOKIE_OPTUS_MUR_SMS_TABLE = "route_70_wookie_optus_mur_alert";
    public static final String WOOKIE_USAGE_POOL_SMS_TABLE = "route_70_wookie_usage_pool_alert";
    public static final String WOOKIE_SMS_TABLE_NAME_PARAM = "wookieTableName";
    public static final String DATA_BOOST_TOKEN = "Data Boost ";
    public static final String DATA_BOOST_NAME = "Boost-";
    public static final String ORIGIN = "Origin";
    public static final String PLAN_GL = "Plan GL ";
    public static final String COSTS_GL_CODE = "Costs GL Code";
    public static final String TAX_SCHEME = "Tax Scheme";
    public static final String EMAIL = "Email";
	public static final Integer NOTIFICATION_TYPE_AGL_INVOICE_EMAIL = 1020;
	public static final String SESSION_ID = "SESSION_ID";

    private SPCConstants() {
        throw new IllegalStateException("Constants class");
    }

    public static final String SPC_JMR_PRICING_FIELDS = String.join(",", Arrays.asList(PURCHASE_ORDER_ID,
            PF_ASSET_NUMBER, FROM_NUMBER, SERVICE_TYPE, CODE_STRING, TARIFF_CODE, CALL_CHARGE, TOTAL_CHARGES,
            DURATION, AMOUNT_CHARGED, PEAK_USAGE, OFF_PEAK_USAGE, OTHER_USAGE, EVENT_DATE, PLAN_ID, COUNTRY,
            START_DATETIME, END_DATETIME, CALL_TYPE, TELSTRA_QUANTITY, CALL_DURATION, UNIT_OF_MEASURE_CODE,
            DISTANCE_RANGE_CODE, AMOUNT));
}
