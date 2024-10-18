package com.sapienter.jbilling.server.fileProcessing;

import com.sapienter.jbilling.common.Util;

import java.io.File;

/**
 * Created by aman on 31/8/15.
 */
public class FileConstants {
    public static final String TAG_NAME_RECORDS = "records";
    public static final String TAG_NAME_STRUCTURE = "structure";
    public static final String TAG_NAME_RECORD = "record";

    public static final String TAG_NAME_FIELDS = "fields";
    public static final String TAG_NAME_FIELD = "field";
    public static final String TAG_NAME_REC_ID = "rec-id";
    public static final String TAG_NAME_FIELD_NAME = "field-name";
    public static final String TAG_NAME_MAX_SIZE = "max-size";
    public static final String TAG_NAME_DATE_FORMAT = "date-format";
    public static final String TAG_NAME_DEFAULT_VALUE = "default-value";
    public static final String TAG_NAME_POSSIBLE_VALUES = "possible-values";
    public static final String TAG_NAME_OPTION = "option";
    public static final String TAG_NAME_VALUE = "value";
    public static final String TAG_NAME_OUTBOUND = "outbound";
    public static final String TAG_NAME_INBOUND = "inbound";
    public static final String TAG_NAME_NOT_USED = "not-used";
    public static final String TAG_NAME_COMMENT = "comment";
    public static final String TAG_ATTR_LOOP = "loop";
    public static final String TAG_ATTR_REQUIRED = "required";
    public static final String INBOUND_PATH = "inbound";
    public static final String OUTBOUND_PATH = "outbound";
    public static final String EXPORT_PATH = "export";
    public static final String TEMP_PATH = "temp";
    public static final String NGES_BASE_DIR = "nges";
    public static final String EDI_TYPE_FORMAT_PATH = NGES_BASE_DIR+File.separator+"edi"+File.separator+"format";
    public static final String EDI_TYPE_DEFAULT_FORMAT_PATH = NGES_BASE_DIR+File.separator+"defaultFormatFile";

    public static final String EDI_TYPE_PATH = "edi";

    public static final String KEY = "KEY";
    public static final String HDR = "HDR";
    public static final String NME = "NME";
    public static final String ACT = "ACT";
    public static final String DTE = "DTE";
    public static final String MTR = "MTR";
    public static final String CDE = "CDE";

    public static final String TRANS_REF_NR = "TRANS_REF_NR";
    public static final String LIFECYCLE_NR = "LIFECYCLE_NR";


    public static final int EDI_STATUS_PROCESSING=1;
    public static final int EDI_STATUS_PROCESSED=2;
    public static final int EDI_STATUS_ERROR_DETECTED=3;

    public static final String ERROR_MSG_FIELD_MANDATORY="record.field.mandatory";
    public static final String ERROR_MSG_FIELD_OPTION_NOT_EXIST="record.field.option.not.exist";
    public static final String ERROR_MSG_FIELD_VALUE_MAX_SIZE_EXIST ="record.field.value.max.exist";

    public static final String HASH_SEPARATOR = "#";
    public static final String UNDERSCORE_SEPARATOR = "_";
    public static final String DOT_SEPARATOR = ".";
    public static final String HYPHEN_SEPARATOR = "-";

    public static final String RESIDENTIAL_ACCOUNT_TYPE = "Residential";
    public static final String COMMERCIAL_ACCOUNT_TYPE = "Commercial/Industrial";
    public static final String SERVICE_INFORMATION_AIT = "Service Information";
    public static final String CUSTOMER_INFORMATION_AIT = SERVICE_INFORMATION_AIT;
    public static final String BUSINESS_INFORMATION_AIT = SERVICE_INFORMATION_AIT;
    public static final String BILLING_INFORMATION_AIT = "Billing Information";
    public static final String CONTACT_INFORMATION_AIT = BILLING_INFORMATION_AIT;
    public static final String ACCOUNT_INFORMATION_AIT = "Account Information";

    public static final String CUSTOMER_ENROLLMENT_FOLDER = "customerEnrollment";

    public static final String CUST_ENROLL_AGREE_DT = "CUST_ENROLL_AGREE_DT";
    public static final String ACTUAL_START_DATE = "Actual Start Date";
    public static final String COMPLETION_DT = "COMPLETION_DT";
    //COMPLETION_DATE used for managing the customer subscribtion end date
    public static final String CUSTOMER_COMPLETION_DATE_METAFIELD = "COMPLETION_DATE";
    public static final String CUSTOMER_TERMINATION_DATE_METAFIELD = "TERMINATION_DATE";
    public static final String DEFAULT_PLAN = "DEFAULT_PLAN";
    public static final String PLAN = "PLAN";
    public static final String DURATION = "DURATION";
    public static final String COMMODITY = "COMMODITY";
    public static final String COMMODITY_GAS = "Gas";
    public static final String COMMODITY_ELECTRICITY = "Electricity";
    public static final String DIVISION = "DIVISION";

    public static final String NAME = "NAME";
    public static final String ADDRESS1 = "ADDRESS1";
    public static final String ADDRESS2 = "ADDRESS2";
    public static final String CITY = "CITY";
    public static final String STATE = "STATE";
    public static final String ZIP_CODE = "ZIP_CODE";
    public static final String ZONE = "ZONE";
    public static final String TELEPHONE = "TELEPHONE";
    public static final String EMAIL = "Email";

    public static final String NOTIFICATION_METHOD = "Notification Method";
    public static final String METER_TYPE = "METER_TYPE";
    public static final String ANNUAL_USAGE = "Annual Usage";
    public static final String UOM = "UoM";
    public static final String CUSTOMER_SPECIFIC_RATE = "Customer Specific Rate";
    public static final String ADDER_FEE_METAFIELD_NAME = "Adder Fee";
    public static final String CUSTOMER_NEXT_READ_DT_META_FIELD = "NEXT_READ_DT";

    public static final String CUSTOMER_ACCOUNT_KEY = "UTILITY_CUST_ACCT_NR";
    public static final String METER_READ_RECORD_TYPE = "867_PURPOSE_CD";
    public static final String INVOICE_TOTAL = "INVOICE_TOTAL";
    public static final String CODE = "CODE";
    public static final String END_SERVICE_DT = "END_SERVICE_DT";
    public static final String END_SERVICE_DT_FORMAT = "yyyyMMdd";
    public static final String CUST_LIFE_SUPPORT = "CUST_LIFE_SUPPORT";
    public static final String REC_TYPE = "REC_TYPE";

    public static final String PROCESSING = "Processing";
    public static final String PROCESSED = "Processed";
    public static final String ERROR_DETECTED = "Error Detected";

    public static final String TERMINATION_META_FIELD = "Termination";
    public static final String TERMINATION_PROCESSING = "Termination Processing";
    public static final String DROPPED = "Dropped";
    public static final String TERMINATION_ESCO_INITIATED = "Esco Initiated";
    public static final String TERMINATION_ESCO_REJECTED = "Esco Rejected";

    //Payment EDI
    public static final String TRACE_NR = "TRACE_NR";
    public static final String INVOICE_NR = "INVOICE_NR";
    public static final String TOTAL_TRANS_AMT = "TOTAL_TRANS_AMT";
    public static final String PYMT_AMOUNT = "PYMT_AMOUNT";
    public static final String DISCOUNT_AMT = "DISCOUNT_AMT";
    //todo remove duplicate constant
    public static final String UTILITY_CUST_ACCT_NR = "UTILITY_CUST_ACCT_NR";
    public static final String ORG_INVOICE_AMT = "ORG_INVOICE_AMT";
    public static final String ACCOUNT_RECORD_KEY = "ACT";
    public static final String EDI_TRANSACTION_TYPE_METER_READ = "867";
    public static final String INVALID_DATA = "Invalid Data";

    //Exception code

    public static final String METER_READ_DUPLICATE_TRANSACTION_EXP_CODE = "JBE101";
    public static final String METER_READ_UNKNOWN_ACCOUNT_EXP_CODE = "JBE102";
    public static final String METER_READ_GAP_EXP_CODE = "JBE103";
    public static final String METER_READ_OVERLAP_EXP_CODE = "JBE104";
    public static final String METER_READ_ENROLLMENT_DATE_SHOULD_BE_GREATER_THEN_AGREEMENT_DATE_EXP_CODE = "JBE105";
    public static final String METER_READ_DIFFRENCE_BETRWEEN_START_AND_END_DATE_GREATER_THEN_TOLLERANCE_EXP_CODE = "JBE106";
    public static final String METER_READ_DROP_CUSTOMER_EXP_CODE = "JBE107";
    public static final String METER_READ_CANCELLATION_NOT_MATCH_WITH_ORIGINAL_CODE_EXP_CODE = "JBE121" ;
    public static final String METER_READ_CANCELLATION_WITHOUT_ORIGINAL_CODE_EXP_CODE = "JBE122" ;
    public static final String METER_READ_CANCELLATION_AFTER_PANDING_REPLACEMENT_EXP_CODE = "JBE123";
    public static final String METER_READ_DUPLICATE_CANCELLATION_EXP_CODE = "JBE124";
    public static final String MISSING_CANCELLATION_FOR_REPLACEMENT_EXP_CODE = "JBE142" ;
    public static final String MISMATCH_ORIGINAL_FOR_REPLACEMENT_EXP_CODE = "JBE141" ;
    public static final String MISSING_ORIGINAL_FOR_REPLACEMENT_EXP_CODE = "JBE140" ;
    public static final String ORIGINAL_METER_READ_FOR_REPLACEMENT_DOES_NOT_EXIST_EXP_CODE = "JBE143";
    public static final String METER_READ_DUPLICATE_REPLACEMENT_EXP_CODE = "JBE144";
    public static final String PAYMENT_UNKNOWN_ACCOUNT_EXP_CODE = "JBE301";
    public static final String PAYMENT_DUPLICATE_TRANSACTION_EXP_CODE = "JBE302";
    public static final String PAYMENT_INVALID_PAYMENT_TALLY_EXP_CODE = "JBE303";
    public static final String PAYMENT_INVALID_POT_EXP_CODE = "JBE304";
    public static final String PAYMENT_INCORRECT_CLCULTION_EXP_CODE = "JBE305";
    public static final String PAYMENT_INCONSISTENT_RECORD_EXP_CODE = "JBE306";

    public static final String METER_READ_NOT_FOUND_FOR_INVOICE_RECORD_EXP_CODE = "JBE201";
    public static final String INVOICE_READ_DUPLICATE_TRANSACTION_EXP_CODE = "JBE202";
    public static final String INVOICE_READ_DOES_NOT_MATCH_METER_READ_EXP_CODE = "JBE203";
    public static final String INVOICE_READ_MULTIPLE_MATCHING_METER_READ_EXP_CODE = "JBE204";
    public static final String INVOICE_READ_UNKNOWN_ACCOUNT_EXP_CODE = "JBE205";
    public static final String INVOICE_READ_INVALID_USAGE_EXP_CODE = "JBE206";
    public static final String INVOICE_READ_INVALID_CALCULATION_EXP_CODE = "JBE207";
    public static final String CANCELLATION_INVOICE_READ_INVOICE_TO_CANCEL_NOT_EXIST_EXP_CODE = "JBE221";
    public static final String CANCELLATION_INVOICE_READ_INVOICE_ALLREADY_CANCELLED_EXP_CODE = "JBE222";
    public static final String CANCELLATION_INVOICE_READ_PENDING_INVOICE_REPLACEMENT_EXP_CODE = "JBE223";
    public static final String CANCELLATION_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_MATCH_EXP_CODE = "JBE224";
    public static final String REPLACEMENT_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_MATCH_EXP_CODE = "JBE241";
    public static final String REPLACEMENT_INVOICE_READ_MISSING_CANCELLATION_INVOICE_MATCH_EXP_CODE = "JBE242";
    public static final String REPLACEMENT_INVOICE_READ_ORIGINAL_INVOICE_READ_NOT_EXIST_EXP_CODE = "JBE243";
    public static final String REPLACEMENT_INVOICE_READ_DUPLICATE_REPLACEMENT_READ_EXP_CODE = "JBE244";
    public static final String REPLACEMENT_INVOICE_READ_METER_READ_DOES_NOT_MATCH_EXP_CODE = "JEB245";
    public static final String REPLACEMENT_INVOICE_READ_MULTIPLE_MATCHING_METER_READ_MATCH_EXP_CODE = "JBE246";
    public static final String EXCEPTION_ESCO_TERMINATION_REQUEST_DATA = "JBE401";
    public static final String EXCEPTION_ESCO_TERMINATION_REJECTED = "JBE402";
    public static final String CUSTOMER_EITHER_IN_PROCESS_OR_DROPPED_EXP_CODE = "B39";
    //Enrollment exception code
    public static final String DUPLICATE_TRANSACTION_EXP_CODE = "JBE505";
    public static final String CUSTOMER_NOT_FOUND = "JBE506";

    public static final String LOAD_CURVE_MISSING_EXP_CODE="JBE108";

    public static final String DUPLICATE_REQUEST_RECEIVED = "ABN";
    public static final String REQUIRED_INFORMATION_MISSING = "API";
    public static final String CONTRACT_NUMBER_INVALID = "0034";
    public static final String ACCOUNT_HAS_BEEN_DROPPED = "B38";
    public static final String ACCOUNT_NOT_AN_ACTIVE_DIS_ACCOUNT = "0003";

    public static final String META_FIELD_METER_READ_FILE = "Meter read file";


    public static final String RESOURCES_PATH = "/resources";
    public static final String INTERVAL_LOAD_CURVE_COMPANY_METAFIELD = "Interval Load Curve";
    public static final String INTERVAL_LOAD_CURVE_CUSTOMER_METAFIELD = "Interval Load Curve";


    public static final String COMMUNICATION_OUTBOUND_PATH = Util.getSysProp("base_dir")  + File.separator  + NGES_BASE_DIR + File.separator  + "ediCommunication" + File.separator + OUTBOUND_PATH;
    public static final String COMMUNICATION_INBOUND_PATH = Util.getSysProp("base_dir")  + File.separator  + NGES_BASE_DIR + File.separator  + "ediCommunication" + File.separator + INBOUND_PATH;

    public static final String DEFAULT_EDI_COMMUNICATION_PATH = Util.getSysProp("base_dir") + File.separator  + NGES_BASE_DIR + File.separator + "ediCommunication" + File.separator;
    public static final String EDI_COMMUNICATION_DIR = "EDI Communication Directory";

    public static String getEDITypePath(Integer entityId, String ediPath, String transactionTpe) {
        return Util.getSysProp("base_dir") + File.separator  + NGES_BASE_DIR + File.separator  + FileConstants.EDI_TYPE_PATH + File.separator + entityId + File.separator + ediPath + File.separator + transactionTpe;
    }

    public static String getEDITypePath(Integer entityId, String ediPath) {
        return Util.getSysProp("base_dir") + File.separator  + NGES_BASE_DIR  + File.separator  + FileConstants.EDI_TYPE_PATH + File.separator + entityId + File.separator + ediPath ;
    }

    public static String getFormatFilePath() {
        return Util.getSysProp("base_dir") + File.separator + FileConstants.EDI_TYPE_FORMAT_PATH;
    }

    public static String getDefaultFormatFilePath() {
        return Util.getSysProp("base_dir") + File.separator + FileConstants.EDI_TYPE_DEFAULT_FORMAT_PATH;
    }

    public static final String EARLY_TERMINATION_INTERNAL_NUMBER = "early termination fee product";

//    Company Level Metafield
    public static final String SUPPLIER_DUNS_META_FIELD_NAME = "SUPPLIER_DUNS";
    public static final String SUPPLIER_NAME_META_FIELD_NAME = "SUPPLIER_NAME";
    public static final String UTILITY_DUNS_META_FIELD_NAME = "UTILITY_DUNS";
    public static final String UTILITY_NAME_META_FIELD_NAME = "UTILITY_NAME";
    public static final String BILL_CALC_META_FIELD_NAME = "BILL_CALC";
    public static final String BILL_DELIVER_META_FIELD_NAME = "BILL_DELIVER";
    public static final String ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME = "ESCO_TERMINATION_EDI_TYPE_ID";
    public static final String AUTO_RENEWAL_EDI_TYPE_ID_META_FIELD_NAME = "AUTO_RENEWAL_EDI_TYPE_ID";
    public static final String ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME = "ENROLLMENT_EDI_TYPE_ID";
    public static final String METER_READ_EDI_TYPE_ID_META_FIELD_NAME = "METER_READ_EDI_TYPE_ID";
    public static final String PAYMENT_EDI_TYPE_ID_META_FIELD_NAME = "PAYMENT_EDI_TYPE_ID";
    public static final String INVOICE_EDI_TYPE_ID_META_FIELD_NAME = "INVOICE_EDI_TYPE_ID";
    public static final String TERMINATION_EDI_TYPE_ID_META_FIELD_NAME = "TERMINATION_EDI_TYPE_ID";
    public static final String ACKNOWLEDGE_EDI_TYPE_ID_META_FIELD_NAME = "ACKNOWLEDGE_EDI_TYPE_ID";
    public static final String CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME = "CHANGE_REQUEST_EDI_TYPE_ID";
    public static final String COMMISSION_MIN_DAYS_META_FIELD_NAME = "Minimum Month for Commissions";


    public static final String BILLING_MODEL="Billing Model";
    public static final String BILLING_MODEL_BILL_READY="Bill Ready";
    public static final String BILLING_MODEL_RATE_READY="Rate Ready";
    public static final String BILLING_MODEL_DUAL="Dual";
    public static final String RATE_CODE_TABLE_NAME="Rate Code Table Name";
    public static final String COMPANY_CALENDAR_META_FIELD_NAME="Cycle Calendar";
    public static final String COMPANY_BUFFER_TIME_META_FIELD_NAME="Buffer Time";
    public static final String COMPANY_LEAD_TIME_1_META_FIELD_NAME="Lead Time 1";
    public static final String COMPANY_LEAD_TIME_2_META_FIELD_NAME="Lead Time 2";
    public static final String COMPANY_SUPPLIER_ACCOUNT_NUMBER_META_FIELD_NAME="UTILITY_SUPPLIER_ACCT_NR";
    public static final String CUSTOMER_ZONE_META_FIELD_NAME="ZONE";
    public static final String CUSTOMER_RATE_ID_METAFILE_FIELD_NAME="rate_id";
    public static final String CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME="PEAK_LOAD_CONTRIBUTION";
    public static final String CUSTOMER_TRANSMISSION_CONTRIBUTION_META_FIELD_NAME="TRANSMISSION_CONTRIBUTION";
    public static final String CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME="Rate Change Date";
    public static final String CUSTOMER_SUPPLIER_ID_META_FIELD_NAME="Supplier ID";

    public static final String CUSTOMER_METER_CYCLE_METAFIELD_NAME="CYCLE_NUMBER";
    public static final String CUSTOMER_RATE_METAFIELD_NAME="Rate";
    public static final String RENEWED_DATE="Renewed Date";

    public static final String EARLY_TERMINATION_FEE_AMOUNT_META_FIELD="Early Termination Fee Amount";

    public static final String SEND_RATE_CHANGE_DAILY = "Send rate change daily";
    public static final String ADDER_FEE_META_FIELD="Adder Fee";
    public static final String EXCEPTION_CODE_KEY="EXCEPTION_CODE";

    public static final String INTERVAL_USAGE_REQUIRED_METAFIELD = "Interval Usage Required";
    public static final String COMPANY_TYPE_LDC="LDC";
    public static final String COMPANY_TYPE_ESCO="ESCO";

    public static final String META_FIELD_PAYMENT_TYPE_CODE = "Type Code";
    public static final String META_FIELD_RECEIVE_EMAIL = "Receive emails";

    public static final Integer DEFAULT_INTERVAL_SIZE=60;
    public static final String CUSTOMER_TAX_METAFIELD="Taxes";
    public static final String CUSTOMER_CALCULATE_TAX_MANUALLY="Calculate Tax Manually";
    public static final String READY_TO_SEND_STATUS = "Ready to send";
    public static final String IS_REBILL_ORDER="Rebill Order";
    public static final String COMMODITY_GAS_CODE="G";
    public static final String COMMODITY_ELECTRICITY_CODE="E";
    public static final String COMMODITY_GAS_UNTI="TD";
    public static final String PASS_THROUGH_CHARGES_META_FIELD = "Pass through charges";
    public static final String COMMODITY_ELECTRICITY_UNTI="KH";
    public static final String REPORT_SEARCH_STATUS_PROCESSED = "processed";
    public static final String REPORT_SEARCH_STATUS_ON_HOLD = "onHold";
    public static final String REPORT_SEARCH_STATUS_UN_PROCESSABLE = "unProcessable";
    public static final String REPORT_SEARCH_STATUS_CRITICAL_ERROR = "criticalError";
}
