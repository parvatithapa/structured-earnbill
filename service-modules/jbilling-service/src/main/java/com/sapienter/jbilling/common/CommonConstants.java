/*
/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.common;

import java.math.BigDecimal;
import java.util.Date;

import org.joda.time.DateTime;

/**
 * @author Emil
 */
public interface CommonConstants {

    public static final Date EPOCH_DATE = new DateTime(1970, 1, 1, 0, 0, 0, 0).withTime(0,0,0,0).toDate();
    public static final Date CURRENT_DATE = new DateTime().withTime(0,0,0,0).toDate();

    public static final Integer INTEGER_TRUE  = Integer.valueOf(1);
    public static final Integer INTEGER_FALSE = Integer.valueOf(0);

    public static final String LIST_TYPE_ITEM_TYPE = "type";
    public static final String LIST_TYPE_CUSTOMER = "customer";
    public static final String LIST_TYPE_CUSTOMER_SIMPLE = "customerSimple";
    public static final String LIST_TYPE_PARTNERS_CUSTOMER = "partnersCustomer";
    public static final String LIST_TYPE_SUB_ACCOUNTS = "sub_accounts";
    public static final String LIST_TYPE_ITEM = "item";
    public static final String LIST_TYPE_ITEM_ORDER = "itemOrder";
    public static final String LIST_TYPE_ITEM_USER_PRICE = "price";
    public static final String LIST_TYPE_PROMOTION = "promotion";
    public static final String LIST_TYPE_PAYMENT = "payment";
    public static final String LIST_TYPE_PAYMENT_USER = "paymentUser";
    public static final String LIST_TYPE_ORDER = "order";
    public static final String LIST_TYPE_INVOICE = "invoice";
    public static final String LIST_TYPE_REFUND = "refund";
    public static final String LIST_TYPE_INVOICE_GRAL = "invoiceGeneral";
    public static final String LIST_TYPE_PROCESS = "process";
    public static final String LIST_TYPE_PROCESS_INVOICES = "processInvoices";
    public static final String LIST_TYPE_PROCESS_RUN_SUCCESSFULL_USERS = "processRunSuccessfullUsers";
    public static final String LIST_TYPE_PROCESS_RUN_FAILED_USERS = "processRunFailedUsers";
    public static final String LIST_TYPE_PROCESS_ORDERS= "processOrders";
    public static final String LIST_TYPE_NOTIFICATION_TYPE= "notificationType";
    public static final String LIST_TYPE_PARTNER = "partner";
    public static final String LIST_TYPE_PAYOUT = "payout";
    public static final String LIST_TYPE_INVOICE_ORDER = "invoicesOrder";

    // results from payments
    // this has to by in synch with how the database is initialized
    public static final Integer RESULT_OK = new Integer(1);
    public static final Integer RESULT_FAIL = new Integer(2);
    public static final Integer RESULT_UNAVAILABLE = new Integer(3);
    public static final Integer RESULT_ENTERED = new Integer(4);
    public static final Integer RESULT_BILLING_INFORMATION_NOT_FOUND = new Integer(5);
    // a special one, to represent 'no result' (for filers, routers, etc)
    public static final Integer RESULT_NULL = new Integer(0);

    // user types, these have to by in synch with the user_type table
    // these are needed in the server side and the jsps
    public static final Integer TYPE_INTERNAL = new Integer(1);
    public static final Integer TYPE_ROOT = new Integer(2);
    public static final Integer TYPE_CLERK = new Integer(3);
    public static final Integer TYPE_PARTNER = new Integer(4);
    public static final Integer TYPE_CUSTOMER = new Integer(5);
    public static final Integer TYPE_SYSTEM_ADMIN = new Integer(-1);

    // payment methods (db - synch)
    public static final Integer PAYMENT_METHOD_CUSTOM = new Integer(-1);
    public static final Integer PAYMENT_METHOD_CHEQUE = new Integer(1);
    public static final Integer PAYMENT_METHOD_VISA = new Integer(2);
    public static final Integer PAYMENT_METHOD_MASTERCARD = new Integer(3);
    public static final Integer PAYMENT_METHOD_AMEX = new Integer(4);
    public static final Integer PAYMENT_METHOD_ACH = new Integer(5);
    public static final Integer PAYMENT_METHOD_DISCOVER = new Integer(6);
    public static final Integer PAYMENT_METHOD_DINERS = new Integer(7);
    public static final Integer PAYMENT_METHOD_PAYPAL = new Integer(8);
    public static final Integer PAYMENT_METHOD_GATEWAY_KEY = new Integer(9);
    public static final Integer PAYMENT_METHOD_INSTAL_PAYMENT = new Integer(10);
    public static final Integer PAYMENT_METHOD_JCB = new Integer(11);
    public static final Integer PAYMENT_METHOD_LASER = new Integer(12);
    public static final Integer PAYMENT_METHOD_MAESTRO = new Integer(13);
    public static final Integer PAYMENT_METHOD_VISA_ELECTRON = new Integer(14);
    public static final Integer PAYMENT_METHOD_CREDIT = new Integer(15);
    public static final Integer PAYMENT_METHOD_PAYPALIPN = new Integer(16);
    public static final Integer PAYMENT_METHOD_PAYPAL_ECO = new Integer(17);
    public static final Integer PAYMENT_METHOD_MIGRATION = new Integer(18);
    public static final Integer PAYMENT_EDI = new Integer(16);
    public static final Integer PAYMENT_METHOD_BANK_WIRE = new Integer(17);
    public static final Integer PAYMENT_METHOD_BPAY = -2;

    //payment result
    public static final Integer PAYMENT_RESULT_SUCCESSFUL = new Integer(1);
    public static final Integer PAYMENT_RESULT_FAILED = new Integer(2);
    public static final Integer PAYMENT_RESULT_PROCESSOR_UNAVAILABLE = new Integer(3);
    public static final Integer PAYMENT_RESULT_ENTERED = new Integer(4);

    // billing process review status
    public static final Integer REVIEW_STATUS_GENERATED = new Integer(1);
    public static final Integer REVIEW_STATUS_APPROVED = new Integer(2);
    public static final Integer REVIEW_STATUS_DISAPPROVED = new Integer(3);

    // these are the preference's types. This has to be in synch with the DB
    //public static Integer PREFERENCE_PAYMENT_WITH_PROCESS = new Integer(1); obsolete
    public static Integer PREFERENCE_CSS_LOCATION = new Integer(2);
    public static Integer PREFERENCE_LOGO_LOCATION = new Integer(3);
    public static Integer PREFERENCE_GRACE_PERIOD = new Integer(4);
    public static Integer PREFERENCE_PAPER_SELF_DELIVERY = new Integer(13);
    public static Integer PREFERENCE_SHOW_NOTE_IN_INVOICE = new Integer(14);
    public static Integer PREFERENCE_DAYS_ORDER_NOTIFICATION_S1 = new Integer(15);
    public static Integer PREFERENCE_DAYS_ORDER_NOTIFICATION_S2 = new Integer(16);
    public static Integer PREFERENCE_DAYS_ORDER_NOTIFICATION_S3 = new Integer(17);
    public static Integer PREFERENCE_INVOICE_PREFIX = new Integer(18);
    public static Integer PREFERENCE_INVOICE_NUMBER = new Integer(19);
    public static Integer PREFERENCE_INVOICE_DELETE = new Integer(20);
    public static Integer PREFERENCE_USE_INVOICE_REMINDERS = new Integer(21);
    public static Integer PREFERENCE_FIRST_REMINDER = new Integer(22);
    public static Integer PREFERENCE_NEXT_REMINDER = new Integer(23);
    public static Integer PREFERENCE_USE_DF_FM = new Integer(24);
    public static Integer PREFERENCE_USE_OVERDUE_PENALTY = new Integer(25);
    public static Integer PREFERENCE_PAGE_SIZE = new Integer(26);
    public static Integer PREFERENCE_USE_ORDER_ANTICIPATION = new Integer(27);
    public static Integer PREFERENCE_PAYPAL_ACCOUNT = new Integer(28);
    public static Integer PREFERENCE_PAYPAL_BUTTON_URL = new Integer(29);
    public static Integer PREFERENCE_URL_CALLBACK = new Integer(30);
    public static Integer PREFERENCE_CONTINUOUS_DATE = new Integer(31);
    public static Integer PREFERENCE_PDF_ATTACHMENT= new Integer(32);
    public static Integer PREFERENCE_ORDER_OWN_INVOICE = new Integer(33);
    public static Integer PREFERENCE_PRE_AUTHORIZE_CC = new Integer(34);
    public static Integer PREFERENCE_ORDER_IN_INVOICE_LINE = new Integer(35);
    public static Integer PREFERENCE_CUSTOMER_CONTACT_EDIT = new Integer(36);
    public static Integer PREFERENCE_LINK_AGEING_TO_SUBSCRIPTION = new Integer(38);
    public static Integer PREFERENCE_FAILED_LOGINS_LOCKOUT = new Integer(39);
    public static Integer PREFERENCE_PASSWORD_EXPIRATION = new Integer(40);
    public static Integer PREFERENCE_USE_CURRENT_ORDER = new Integer(41);
    public static Integer PREFERENCE_USE_PRO_RATING = new Integer(42);
    public static Integer PREFERENCE_USE_BLACKLIST = new Integer(43);
    public static Integer PREFERENCE_ALLOW_NEGATIVE_PAYMENTS = new Integer(44);
    public static Integer PREFERENCE_DELAY_NEGATIVE_PAYMENTS = new Integer(45);
    public static Integer PREFERENCE_ALLOW_INVOICES_WITHOUT_ORDERS = new Integer(46);
    public static Integer PREFERENCE_MEDIATION_JDBC_READER_LAST_ID = new Integer(47);
    public static Integer PREFERENCE_USE_PROVISIONING = new Integer(48);
    public static Integer PREFERENCE_AUTO_RECHARGE_THRESHOLD = new Integer(49);
    public static Integer PREFERENCE_INVOICE_DECIMALS = new Integer(50);
    public static Integer PREFERENCE_MINIMUM_BALANCE_TO_IGNORE_AGEING = new Integer(51);
    public static Integer PREFERENCE_ATTACH_INVOICE_TO_NOTIFICATIONS = new Integer(52);
    public static Integer PREFERENCE_FORCE_UNIQUE_EMAILS = new Integer(53);
    public static Integer PREFERENCE_UNIQUE_PRODUCT_CODE = new Integer(55);
    public static Integer PREFERENCE_PARTNER_DEFAULT_COMMISSION_TYPE = new Integer(61);
    public static Integer PREFERENCE_SHOW_NO_IMPACT_PLAN_ITEMS = 62;
    public static Integer PREFERENCE_USE_JQGRID = 63;
    public static Integer PREFERENCE_DIAMETER_DESTINATION_REALM = new Integer(64);
    public static Integer PREFERENCE_DIAMETER_QUOTA_THRESHOLD = new Integer(65);
    public static Integer PREFERENCE_DIAMETER_SESSION_GRACE_PERIOD_SECONDS = new Integer(66);
    public static Integer PREFERENCE_DIAMETER_UNIT_DIVISOR = Integer.valueOf(67);
    public static Integer PREFERENCE_ACCOUNT_LOCKOUT_TIME = new Integer(68);
    public static Integer PREFERENCE_ITG_INVOICE_NOTIFICATION = new Integer(69);
    public static Integer PREFERENCE_EXPIRE_INACTIVE_AFTER_DAYS = new Integer(70);
    public static Integer PREFERENCE_FORGOT_PASSWORD_EXPIRATION = new Integer(71);
    public static Integer PREFERENCE_CREATE_CREDENTIALS_BY_DEFAULT = new Integer(75);
    public static Integer PREFERENCE_ASSET_RESERVATION_DURATION = new Integer(76);
    public static Integer PREFERENCE_PARENT_CHILD_CUSTOMER_HIERARCHY = new Integer(77);
    public static Integer PREFERENCE_EMAIL_INVOICE_BUNDLE = new Integer(78);
    public static Integer PREFERENCE_PHONE_NUMBER_FORMAT = new Integer(79);
    public static Integer PREFERENCE_ADJUSTMENT_ORDER_CREATION = Integer.valueOf(80);
    public static Integer PREFERENCE_FREE_MINUTES_TOKEN_SKIP_PRODUCT_CODE = Integer.valueOf(81);
    public static Integer PREFERENCE_BACKGROUND_CSV_EXPORT = new Integer(82);
    public static Integer PREFERENCE_ALLOW_DUPLICATE_META_FIELDS_IN_COPY_COMPANY = new Integer(83);
    public static Integer PREFERENCE_CHRONOLOGICAL_AGEING = new Integer(85);
    public static Integer PREFERENCE_SUBMIT_TO_PAYMENT_GATEWAY = new Integer(86);
    public static Integer PREFERENCE_ORDER_LINE_TIER = new Integer(87);
    public static Integer PREFERENCE_AGEING_REVALUATION = new Integer(88);
    public static Integer PREFERENCE_ONE_ORDER_PER_MEDIATION_RUN = new Integer(89);
    public static Integer PREFERENCE_MEDIATED_ORDER_PRICING_CACHE = new Integer(90);
    public static Integer PREFERENCE_USE_INVOICE_ID_AS_INVOICE_NUMBER_IN_INVOICE_LINE_DESCRIPTIONS = new Integer(91);
    public static Integer PREFERENCE_APPLY_ONLY_TO_UPGRADE_ORDERS = new Integer(92);
    public static Integer PREFERENCE_SWAP_PLAN = 93;
    public static Integer PREFERENCE_APPLY_NOW = new Integer(94);
    public static Integer PREFERENCE_INVOICE_LINE_TAX = 95;
    public static Integer PREFERENCE_DECIMAL_DIGITS_FOR_MEDIATION_EVENTS = 96;
    public static Integer PREFERENCE_INVOICE_LINE_PRECISION = 97;
    public static Integer REQUIRE_PAYMENT_AUTHORIZATION_FOR_COLLECTION_PROCESS = 98;
    public static Integer PREFERENCE_REQUIRE_CVV_FOR_ONE_TIME_PAYMENTS = 99;
    public static Integer PREFERENCE_USE_ASSET_LINKED_FREE_USAGE_POOLS_ONLY = 100;
    public static Integer PREFERENCE_REMOVE_USER_FROM_AGEING_ON_PAYING_OVERDUE_INVOICE = 101;

    // IDs of default set of order statuses pre created.
    public static final Integer DEFAULT_ORDER_INVOICE_STATUS_ID = new Integer(1);
    public static final Integer DEFAULT_ORDER_FINISHED_STATUS_ID = new Integer(2);
    public static final Integer DEFAULT_ORDER_NOT_INVOICE_STATUS_ID = new Integer(3);
    public static final Integer DEFAULT_ORDER_SUSPENDED_AGEING_STATUS_ID = new Integer(4);

    // order change status, in synch with db
    public static final Integer ORDER_CHANGE_STATUS_PENDING = new Integer(1);
    public static final Integer ORDER_CHANGE_STATUS_APPLY_ERROR = new Integer(2);

    // order change type, in synch with db
    public static final Integer ORDER_CHANGE_TYPE_DEFAULT = new Integer(1);

    // invoice status, in synch with db
    public static final Integer INVOICE_STATUS_PAID = new Integer(1);
    public static final Integer INVOICE_STATUS_UNPAID = new Integer(2);
    public static final Integer INVOICE_STATUS_UNPAID_AND_CARRIED = new Integer(3);

    // process run status, in synch with db
    public static final Integer PROCESS_RUN_STATUS_RINNING = new Integer(1);
    public static final Integer PROCESS_RUN_STATUS_SUCCESS = new Integer(2);
    public static final Integer PROCESS_RUN_STATUS_FAILED = new Integer(3);

    // invoice delivery method types
    public static final Integer D_METHOD_EMAIL = new Integer(1);
    public static final Integer D_METHOD_PAPER = new Integer(2);
    public static final Integer D_METHOD_EMAIL_AND_PAPER = new Integer(3);
    public static final Integer D_METHOD_NONE = new Integer(4);

    // automatic payment methods
    // how a customer wants to pay in the automatic process
    public static final Integer AUTO_PAYMENT_TYPE_CC = new Integer(1);
    public static final Integer AUTO_PAYMENT_TYPE_ACH =  new Integer(2);
    public static final Integer AUTO_PAYMENT_TYPE_CHEQUE = new Integer(3);

    // types of PDF batch generation
    public static final Integer OPERATION_TYPE_CUSTOMER = new Integer(1);
    public static final Integer OPERATION_TYPE_RANGE = new Integer(2);
    public static final Integer OPERATION_TYPE_PROCESS = new Integer(3);
    public static final Integer OPERATION_TYPE_DATE = new Integer(4);
    public static final Integer OPERATION_TYPE_NUMBER = new Integer(5);

    // Payment Methods Type MetaFields Names
    public static final String METAFIELD_NAME_CC_CARDHOLDER_NAME = "cc.cardholder.name";
    public static final String METAFIELD_NAME_CC_NUMBER = "cc.number";
    public static final String METAFIELD_NAME_CC_EXPIRY_DATE = "cc.expiry.date";
    public static final String METAFIELD_NAME_CC_GATEWAY_KEY = "cc.gateway.key";
    public static final String METAFIELD_NAME_CC_TYPE = "cc.type";
    public static final String METAFIELD_NAME_CARD_NUMBER = "Card Number";
    public static final String METAFIELD_NAME_CARD_TYPE = "Card Type";
    public static final String METAFIELD_NAME_BRAINTREE_CUSTOMER_ID = "BT Customer Id";
    public static final String METAFIELD_NAME_BRAINTREE_CVV = "CVV";

    public static final String METAFIELD_NAME_CHEQUE_BANK_NAME = "cheque.bank.name";
    public static final String METAFIELD_NAME_CHEQUE_NUMBER = "cheque.number";
    public static final String METAFIELD_NAME_CHEQUE_DATE = "cheque.date";

    public static final String METAFIELD_NAME_ACH_ROUTING_NUMBER = "ach.routing.number";
    public static final String METAFIELD_NAME_ACH_CUSTOMER_NAME = "ach.customer.name";
    public static final String METAFIELD_NAME_ACH_ACCOUNT_NUMBER = "ach.account.number";
    public static final String METAFIELD_NAME_ACH_BANK_NAME = "ach.bank.name";
    public static final String METAFIELD_NAME_ACH_GATEWAY_KEY = "ach.gateway.key";
    public static final String METAFIELD_NAME_ACH_ACCOUNT_TYPE = "ach.account.type";

    /**
     * BigDecimal caculation constants <br/>
     * This value must be inline with underlying SQL data type
     */
    public static final int BIGDECIMAL_SCALE = 10;
    /**
     * Round to 2 decimals for view. Use it with formatters and/or toString
     */
    public static final int BIGDECIMAL_SCALE_STR = 2;
    public static final int BIGDECIMAL_QUANTITY_SCALE = 4;
    public static final int BIGDECIMAL_ROUND = BigDecimal.ROUND_HALF_UP;

    public static final BigDecimal BIGDECIMAL_ONE = new BigDecimal("1");
    public static final BigDecimal BIGDECIMAL_ONE_CENT = new BigDecimal("0.01");

    public static final String PASSWORD_PATTERN_4_UNIQUE_CLASSES = "^.*(?=.{8,40})(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*+=]).*$";
    public static final String PASSWORD_PATTERN = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789(?=.*[!@#$%^&*+=])/";
    public static final String USERNAME_PATTERN = "^[. A-Za-z0-9_@-]*$";

    // codes for login resuls
    public final static Integer AUTH_OK = new Integer(0);
    public final static Integer AUTH_WRONG_CREDENTIALS = new Integer(1);
    public final static Integer AUTH_LOCKED = new Integer(2);  // invalid login creds - bad attempt locked account
    public final static Integer AUTH_EXPIRED = new Integer(3); // login creds ok - password expired and needs updating

    // provisioning status constants
    public final static Integer PROVISIONING_STATUS_ACTIVE=new Integer(1);
    public final static Integer PROVISIONING_STATUS_INACTIVE=new Integer(2);
    public final static Integer PROVISIONING_STATUS_PENDING_ACTIVE=new Integer(3);
    public final static Integer PROVISIONING_STATUS_PENDING_INACTIVE=new Integer(4);
    public final static Integer PROVISIONING_STATUS_FAILED=new Integer(5);
    public final static Integer PROVISIONING_STATUS_UNAVAILABLE = new Integer(6);

    // types of balances
    public final static Integer BALANCE_NO_DYNAMIC = new Integer(1); // the default
    public final static Integer BALANCE_PRE_PAID = new Integer(2);
    public final static Integer BALANCE_CREDIT_LIMIT = new Integer(3);

    // mediation record status
    public final static Integer MEDIATION_RECORD_STATUS_DONE_AND_BILLABLE = new Integer(1);
    public final static Integer MEDIATION_RECORD_STATUS_DONE_AND_NOT_BILLABLE = new Integer(2);
    public final static Integer MEDIATION_RECORD_STATUS_ERROR_DETECTED = new Integer(3);
    public final static Integer MEDIATION_RECORD_STATUS_ERROR_DECLARED = new Integer(4);
    public final static Integer MEDIATION_RECORD_STATUS_DUPLICATE = new Integer(6);
    public final static Integer MEDIATION_RECORD_STATUS_AGGREGATED = new Integer(3);

    // mediation version
    public static final String PROPERTY_MEDIATION_VERSION = "process.run.mediation.version";
    public static final String MEDIATION_RECORD_MAX_LIMIT = "api.mediation.record.max.limit";

    // ITG
    public static final String ITG_USE_HBASE = "itg.useHBase";

    //Route Based Rate Cards
    public static final String DEFAULT_DATE_FIELD_NAME = "event_date";
    public static final String DEFAULT_DURATION_FIELD_NAME = "duration";
    public static final String ROUTE_ID_FIELD_NAME = "routeId";
    public static final String NEXT_ROUTE_FIELD_NAME = "next_route";
    public static final String PRODUCT_FIELD_NAME = "product";
    public static final String MATCHING_FIELD_TABLE_PLACEHOLDER = ":table:";

    // payment methods defined in system
    public static final String PAYMENT_CARD = "Payment Card";
    public static final String ACH = "ACH";
    public static final String ACH_CHECKING = "CHECKING";
    public static final String ACH_SAVING = "SAVINGS";
    public static final String CHEQUE = "Cheque";
    public static final String EDI = "EDI";
    public static final String CUSTOM = "Custom";

    // subscription product constants
    public static final String SUBSCRIPTION_ACCOUNT_PASSWORD = "subscription_password";

    /* #10256 - Asset Reservation - 10 minutes by default */
    public static final Integer DEFAULT_ASSET_RESERVATION_TIME_IN_MS = 600000;

    //11369 - Inactive User Account Management
    public static Integer MAX_VALUE_FOR_PREFERENCE_EXPIRE_INACTIVE_ACCOUNTS_IN_DAYS = new Integer(90);
    public static final String  METER_READ_ORIGINAL_RECORD = "00";
    public static final String METER_READ_CANCELLATION_RECORD = "01";
    public static final String METER_READ_REPLACEMENT_RECORD = "05";
    public static final String METER_READ_HISTORICAL_RECORD = "52";
    public static final String METER_READ_USAGE_SUMMARY = "SUM";
    public static final String METER_READ_USAGE_DETAIL = "DET";
    public static final String METER_READ_RECORD_TYPE = "867_PURPOSE_CD";
    public static final String METER_READ_HEADER_RECORD_VALUE = "HDR";
    public static final String METER_READ_KEY_RECORD_VALUE = "KEY";
    public static final String METER_READ_USAGE_RECORD = "UMR";
    public static final String METER_READ_CONSUMPTION_RECORD_KEY = "READ_CONSUMPTION";
    public static final String METER_READ_START_DATE = "START_SERVICE_DT";
    public static final String METER_READ_END_DATE = "END_SERVICE_DT";
    public static final String METER_READ_USAGE_TYPE = "USAGE_TYPE";
    public static final String METER_READ_CUSTOMER_ACCOUNT_KEY = "UTILITY_CUST_ACCT_NR";
    public static final String FILE_STATUS_FOR_MEDIATION_KEY = "Ready For Mediation";
    public static final String METER_READ_DONE_STATUS = "Done";
    public static final Integer METER_READ_DATE_TOLERANCE = 3;
    public static final String  METER_READ_FINAL_RECORD = "FINAL_IND";
    public static final String  METER_READ_FINAL_RECORD_VALUE = "F";

    public static String ROOT_USER = "copyCompany.root.user";
    public static String ADMIN_USERS_REGEX = "copyCompany\\.admin\\.(\\d)";
    public static String EMAIL_VALIDATION_REGEX = "^([a-zA-Z0-9#\\!$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]|\\\\ ))+(\\.([a-zA-Z0-9!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|}~-]|(\\\\@|\\\\,|\\\\\\[|\\\\\\]))+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.([A-Za-z]{2,})$";

    // Paypal payflow constants
    public static final String PAYPAL_PAYFLOW_ENVIRONMENT = "Live";
    public static final String PAYPAL_PAYFLOW_PARTNER = "PayPal";

    //CIM Profile error constant
    public static final Integer CIM_PROFILE_ERROR = Integer.valueOf(1);
    public static final Integer CIM_PROFILE_BILLING_INFO_ERROR = Integer.valueOf(2);

    public static final String TOKEN = "TOKEN";
    public static final String PAYER_ID = "PAYER_ID";
    public static final String MANDRILL_HOST_NAME= "smtp.mandrillapp.com";

    // generate csv path
    public static final String PROPERTY_GENERATE_CSV_FILE_PATH = "generate.csv.file.path";

    public static final String NEW_QUANTITY = "0.000000000000000001";
    public static final String CUSTOMER_USAGE_POOL_EVALUATION_PLUGIN_CLASS_NAME="com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolEvaluationTask";

    //BOA file processing batch job related constants
    public static final String BOA_JOB_PARAM_READ_FROM_DAILY_FILES_DIRECTORY = "read_from_daily_files_directory";
    public static final String BOA_JOB_PARAM_READ_FROM_INTRADAY_FILES_DIRECTORY = "read_from_intraday_files_directory";
    public static final String BOA_JOB_PARAM_MOVE_TO_DAILY_FILES_DIRECTORY = "move_to_daily_files_directory";
    public static final String BOA_JOB_PARAM_MOVE_TO_INTRADAY_FILES_DIRECTORY = "move_to_intraday_files_directory";
    public static final String BOA_JOB_PARAM_DEFAULT_USER_ID = "default_user_id";
    public static final String TABLE_BOA_BAI_PROCESSED_FILES = "boa_bai_processed_files";
    public static final String TABLE_BOA_BAI_PROCESSING_ERRORS = "boa_bai_processing_errors";
    public static final Integer PREFERENCE_BANK_PAYMENT_ALERT_USER_ID = new Integer(68);
    public static final String PROPERTY_RUN_BOA_FILE_JOB = "process.run_boa_file_job";


    //Just in Time User related Constants
    public static final String JIT_USER_PASSWORD = "123qweR!";
    public static final Integer JIT_USER_LANGUAGE_ID = 1;
    public static final Boolean JIT_USER_DEFAULT_FALSE = false;
    public static final Boolean JIT_USER_DEFAULT_TRUE = true;
    public static final Integer JIT_USER_DELETED = 0;
    public static final Integer JIT_USER_CURRENCY_ID = 1;
    public static final Integer JIT_USER_FAILED_ATTEMPTS = 0;
    public static final String JIT_USER_FIRST_NAME = "JIT";
    public static final String JIT_USER_LAST_NAME = "USER";
    public static final String JIT_USER_FIRST_ADDRESS1 = "Default Address";
    public static final String JIT_USER_LAST_ADDRESS2 = "";
    public static final String JIT_USER_CITY = "City";
    public static final String JIT_USER_STATE = "State";
    public static final String JIT_USER_POSTAL_CODE = "12345";
    public static final String JIT_USER_COUNTRY_CODE = "US";
    public static final String JIT_USER_COMPANY_NAME = "JIT_USER_COMPANY";

    public static final String SSO_ENABLED_USER = "sso.enabled.user";
    public static final String SSO_IDP_ID_USER = "sso.idp.id.user";
    public static final String SSO_IDP_APPDIRECT_UUID_USER = "sso.idp.appdirect.uuid.user";
    public static final String SSO_ENABLED_CUSTOMER = "sso.enabled.customer";
    public static final String SSO_IDP_ID_CUSTOMER = "sso.idp.id.customer";
    public static final String SSO_IDP_APPDIRECT_UUID_CUSTOMER = "sso.idp.appdirect.uuid.customer";
    public static final String SSO_ENABLED_AGENT = "sso.enabled.agent";
    public static final String SSO_IDP_ID_AGENT = "sso.idp.id.agent";
    public static final String SSO_IDP_APPDIRECT_UUID_AGENT = "sso.idp.appdirect.uuid.agent";

    public static final String CIT_JIT_META_FIELD_NAME = "Just in Time";
    public static final String CIT_DEFAULT_META_FIELD_NAME = "Default";
    public static final String CIT_SAML_IDP_METADATA_URL = "Saml Idp Metadata Url";
    public static final String CIT_SAML_IDP_ENTITY_ID = "Saml Idp Entity Id";
    public static final String CIT_RESET_PASSWORD_URL = "/forgotPassword";
    public static final String CIT_RESET_PASSWORD_URL_META_FIELD_NAME = "Reset Password URL";
    public static final String CIT_DEFAULT_ROLE_META_FIELD_NAME = "Default Role";
    public static final String CIT_CREATOR_UUID_META_FIELD_NAME = "Creator Uuid";

    public static final String CIT_TYPE_ROOT_ENUM_NAME = "TYPE_ROOT";
    public static final String CIT_TYPE_CLERK_ENUM_NAME = "TYPE_CLERK";
    public static final String CIT_TYPE_SYSTEM_ADMIN_ENUM_NAME = "TYPE_SYSTEM_ADMIN";

    public static final Integer PREFERENCE_SSO = new Integer(84);
    public static final String ORDER_STATUS_PENDING = "Pending";
    public static final String NOTIFICATION_EMAIL_ERROR = "notification.email.error";

    public static final String TAX_SCHEME = "Tax Scheme";
    public static final String TAX_TABLE_NAME = "Tax Table Name";
    public static final String TAX_DATE_FORMAT = "Tax Date Format";

    public static final String PRICE_NOT_FOUND_IN_RATE_CARD = "PRICE_NOT_FOUND_IN_RATE_CARD";
    
    /*
     * Response code 
    */
    
    public static final Integer ERROR_CODE_UNAUTHORIZED_DATA_ACCESS = Integer.valueOf(403);
    public static Integer PREFERENCE_DISPLAY_PAYMENT_URL_LINK_NOTIFICATION = new Integer(102);
}
