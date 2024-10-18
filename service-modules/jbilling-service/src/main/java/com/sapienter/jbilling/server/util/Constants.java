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

package com.sapienter.jbilling.server.util;

import com.sapienter.jbilling.common.CommonConstants;

/**
 * @author emilc
 *
 */
public final class Constants implements CommonConstants {
	
	/*
	 * 
	 * Spring Batch Constants
	 * 
	 */
	public static final String BATCH_JOB_PARAM_ENTITY_ID = "entityId";
	public static final String BATCH_JOB_PARAM_UNIQUE = "unique";
	
    /*
     * DATA BASE CONSTANTS
     * These values are in the database, should be initialized by the
     * InitDataBase program and remain static.
     */
    // the agreed maximum length for a varchar.
    public static final int MAX_VARCHAR_LENGTH = 1000;
    // this should be equal to hibernate.jdbc.batch_size
    public static final int HIBERNATE_BATCH_SIZE = 100;
    // tables
    public static final String TABLE_ITEM = "item";
    public static final String TABLE_PUCHASE_ORDER = "purchase_order";
    public static final String TABLE_ORDER_PROCESSING_RULE = "order_processing_rule";
    public static final String TABLE_ORDER_PERIOD = "order_period";
    public static final String TABLE_ORDER_LINE_TYPE = "order_line_type";
    public static final String TABLE_BILLING_PROCESS = "billing_process";
    public static final String TABLE_BILLING_PROCESS_RUN = "process_run";
    public static final String TABLE_BILLING_PROCESS_RUN_TOTAL = "process_run_total";
    public static final String TABLE_BILLING_PROCESS_RUN_TOTAL_PM = "process_run_total_pm";
    public static final String TABLE_BILLING_PROCESS_CONFIGURATION = "billing_process_configuration";
    public static final String TABLE_INVOICE = "invoice";
    public static final String TABLE_INVOICE_STATUS = "invoice_status";
    public static final String TABLE_INVOICE_LINE= "invoice_line";
    public static final String TABLE_EVENT_LOG = "event_log";
    public static final String TABLE_INTERNATIONAL_DESCRIPTION = "international_description";
    public static final String TABLE_LANGUAGE = "language";
    public static final String TABLE_ENTITY = "entity";
    public static final String TABLE_USER_TYPE = "user_type";
    public static final String TABLE_BASE_USER = "base_user";
    public static final String TABLE_CUSTOMER = "customer";
    public static final String TABLE_PERIOD_UNIT = "period_unit";
    public static final String TABLE_ORDER_BILLING_TYPE = "order_billing_type";
    public static final String TABLE_ORDER_STATUS = "order_status";
    public static final String TABLE_ORDER_LINE = "order_line";
    public static final String TABLE_PLUGGABLE_TASK_TYPE_CATEGORY = "pluggable_task_type_category";
    public static final String TABLE_PLUGGABLE_TASK_TYPE = "pluggable_task_type";
    public static final String TABLE_PLUGGABLE_TASK = "pluggable_task";
    public static final String TABLE_PLUGGABLE_TASK_PARAMETER = "pluggable_task_parameter";
    public static final String TABLE_CONTACT = "contact";
    public static final String TABLE_CONTACT_FIELD = "contact_field";
    public static final String TABLE_CONTACT_FIELD_TYPE = "contact_field_type";
    public static final String TABLE_CONTACT_TYPE = "contact_type";
    public static final String TABLE_CONTACT_MAP = "contact_map";
    public static final String TABLE_CANCELLATION_REQUEST= "cancellation_request";
    public static final String TABLE_INVOICE_LINE_TYPE = "invoice_line_type";
    public static final String TABLE_PAYMENT = "payment";
    public static final String TABLE_PAYMENT_INFO_CHEQUE = "payment_info_cheque";
    public static final String TABLE_PAYMENT_INFORMATION = "payment_information";
    public static final String TABLE_PAYMENT_RESULT = "payment_result";
    public static final String TABLE_PAYMENT_METHOD = "payment_method";
    public static final String TABLE_PAYMENT_INVOICE_MAP = "payment_invoice";
    public static final String TABLE_EVENT_LOG_MODULE = "event_log_module";
    public static final String TABLE_EVENT_LOG_MESSAGE = "event_log_message";
    public static final String TABLE_ORDER_PROCESS = "order_process";
    public static final String TABLE_PREFERENCE = "preference";
    public static final String TABLE_PREFERENCE_TYPE = "preference_type";
    public static final String TABLE_NOTIFICATION_MESSAGE = "notification_message";
    public static final String TABLE_NOTIFICATION_MESSAGE_SECTION = "notification_message_section";
    public static final String TABLE_NOTIFICATION_MESSAGE_TYPE = "notification_message_type";
    public static final String TABLE_NOTIFICATION_MESSAGE_LINE = "notification_message_line";
    public static final String TABLE_NOTIFICATION_MESSAGE_ARCHIVE = "notification_message_arch";
    public static final String TABLE_NOTIFICATION_MESSAGE_ARCHIVE_LINE = "notification_message_arch_line";
    public static final String TABLE_REPORT = "report";
    public static final String TABLE_REPORT_TYPE = "report_type";
    public static final String TABLE_PERMISSION = "permission";
    public static final String TABLE_PERMISSION_TYPE = "permission_type";
    public static final String TABLE_ROLE= "role";
    public static final String TABLE_PERMISSION_ROLE_MAP= "permission_role_map";
    public static final String TABLE_USER_ROLE_MAP= "user_role_map";
    public static final String TABLE_MENU_OPTION = "menu_option";
    public static final String TABLE_COUNTRY = "country";
    public static final String TABLE_PARTNER = "partner";
    public static final String TABLE_PARTNER_RANGE = "partner_range";
    public static final String TABLE_PARTNER_PAYOUT = "partner_payout";
    public static final String TABLE_USER_STATUS = "user_status";
    public static final String TABLE_USER_SUBSCRIBER_STATUS = "subscriber_status";
    public static final String TABLE_ITEM_TYPE = "item_type";
    public static final String TABLE_ITEM_USER_PRICE= "item_user_price";
    public static final String TABLE_PROMOTION= "promotion";
    public static final String TABLE_CREDIT_CARD= "credit_card";
    public static final String TABLE_USER_CREDIT_CARD_MAP= "user_credit_card_map";
    public static final String TABLE_PAYMENT_AUTHORIZATION="payment_authorization";
    public static final String TABLE_CURRENCY = "currency";
    public static final String TABLE_CURRENCY_ENTITY_MAP = "currency_entity_map";
    public static final String TABLE_CURRENCY_EXCHANGE= "currency_exchange";
    public static final String TABLE_ITEM_PRICE = "item_price";
    public static final String TABLE_AGEING_ENTITY_STEP = "ageing_entity_step";
    public static final String TABLE_INVOICE_DELIVERY_METHOD = "invoice_delivery_method";
    public static final String TABLE_ENTITY_DELIVERY_METHOD_MAP = "entity_delivery_method_map";
    public static final String TABLE_PAPER_INVOICE_BATCH = "paper_invoice_batch";
    public static final String TABLE_ACH = "ach";
    public static final String TABLE_LIST_ENTITY = "list_entity";
    public static final String TABLE_LIST_FIELD_ENTITY = "list_field_entity";
    public static final String TABLE_MEDIATION_CFG = "mediation_cfg";
    public static final String TABLE_BLACKLIST = "blacklist";
    public static final String TABLE_GENERIC_STATUS_TYPE = "generic_status_type";
    public static final String TABLE_GENERIC_STATUS = "generic_status";
    public static final String TABLE_ORDER_LINE_PROVISIONING_STATUS = "order_line_provisioning_status";
    public static final String TABLE_MEDIATION_RECORD_STATUS = "mediation_record_status";
    public static final String TABLE_PROCESS_RUN_STATUS = "process_run_status";
    public static final String TABLE_NOTIFICATION_CATEGORY = "notification_category";
    public static final String TABLE_ASSET = "asset";
    public static final String TABLE_ASSET_STATUS = "asset_status";
    public static final String TABLE_ASSET_TRANSITION = "asset_transition";
    public static final String TABLE_CREDIT_NOTE="credit_note";
    public static final String TABLE_ENUMERATION = "enumeration";
    public static final String TABLE_ENUMERATION_VALUES = "enumeration_values";
    public static final String TABLE_METAFIELD_GROUP = "meta_field_group";
    public static final String TABLE_METAFIELD= "meta_field_name";
    public static final String TABLE_VALIDATION_RULE= "validation_rule";
    public static final String TABLE_PAYMENT_PROCESS_RUN = "payment_process_run";
    
    public static final String TABLE_DISCOUNT = "discount";
    
    public static final String TABLE_ORDER_CHANGE_STATUS = "order_change_status";
    public static final String TABLE_ORDER_CHANGE = "order_change";
    
    public static final String TABLE_USAGE_POOL = "usage_pool";

    public static final String TABLE_ORDER_CHANGE_PLAN_ITEM = "order_change_plan_item";

    public static final String TABLE_RATE_CARDS = "rate_card";

    public static final String TABLE_PAYMENT_METHOD_TYPE = "payment_method_type";
    public static final String TABLE_INVOICE_SUMMARY="invoice_summary";
    public static final String TABLE_RATING_CONFIGURATION="rating_configuration";
    public static final String TABLE_ORDER_LINE_TIER = "order_line_tier";

    // psudo column values from international description
    public static final String PSUDO_COLUMN_TITLE = "title";
    public static final String PSUDO_COLUMN_DESCRIPTION = "description";
    
    // order line types
    public static final int ORDER_LINE_TYPE_ITEM = 1;
    public static final int ORDER_LINE_TYPE_TAX = 2;
    public static final int ORDER_LINE_TYPE_PENALTY = 3;
    public static final int ORDER_LINE_TYPE_DISCOUNT = 4;
    public static final int ORDER_LINE_TYPE_SUBSCRIPTION = 5;
    public static final int ORDER_LINE_TYPE_TAX_QUOTE = 6;
    public static final int ORDER_LINE_TYPE_ADJUSTMENT = 7;

    // order periods. This are those NOT related with any single entity
    public static final Integer ORDER_PERIOD_ONCE = new Integer(1);
    
    public static final Integer ORDER_PERIOD_ALL_ORDERS = new Integer(5);

    // period unit types
    public static final Integer PERIOD_UNIT_MONTH = new Integer(1);
    public static final Integer PERIOD_UNIT_WEEK = new Integer(2);
    public static final Integer PERIOD_UNIT_DAY = new Integer(3);
    public static final Integer PERIOD_UNIT_YEAR= new Integer(4);
    public static final Integer PERIOD_UNIT_SEMI_MONTHLY= new Integer(5);
    
    // order billing types
    public static final Integer ORDER_BILLING_PRE_PAID = new Integer(1);
    public static final Integer ORDER_BILLING_POST_PAID = new Integer(2);
    
    // pluggable tasks categories
    public static final Integer PLUGGABLE_TASK_PROCESSING_ORDERS = new Integer(1);
    public static final Integer PLUGGABLE_TASK_ORDER_FILTER = new Integer(2);
    public static final Integer PLUGGABLE_TASK_INVOICE_FILTER = new Integer(3);
    public static final Integer PLUGGABLE_TASK_INVOICE_COMPOSITION = new Integer(4);
    public static final Integer PLUGGABLE_TASK_ORDER_PERIODS = new Integer(5);
    public static final Integer PLUGGABLE_TASK_PAYMENT = new Integer(6);
    public static final Integer PLUGGABLE_TASK_NOTIFICATION = new Integer(7);
    public static final Integer PLUGGABLE_TASK_PAYMENT_INFO = new Integer(8);
    public static final Integer PLUGGABLE_TASK_PENALTY = new Integer(9);
    public static final Integer PLUGGABLE_TASK_PROCESSOR_ALARM = new Integer(10);
    public static final Integer PLUGGABLE_TASK_SUBSCRIPTION_STATUS = new Integer(11);
    public static final Integer PLUGGABLE_TASK_ASYNC_PAYMENT_PARAMS = new Integer(12);
    public static final Integer PLUGGABLE_TASK_ITEM_MANAGER = new Integer(13);
    public static final Integer PLUGGABLE_TASK_ITEM_PRICING = new Integer(14);
    public static final Integer PLUGGABLE_TASK_MEDIATION_READER = new Integer(15);
    public static final Integer PLUGGABLE_TASK_MEDIATION_PROCESS = new Integer(16);
    public static final Integer PLUGGABLE_TASK_INTERNAL_EVENT = new Integer(17);
    public static final Integer PLUGGABLE_TASK_EXTERNAL_PROVISIONING = new Integer(18);
    public static final Integer PLUGGABLE_TASK_VALIDATE_PURCHASE = new Integer(19);
    public static final Integer PLUGGABLE_TASK_BILL_PROCESS_FILTER = new Integer(20);
    public static final Integer PLUGGABLE_TASK_MEDIATION_ERROR_HANDLER = new Integer(21);
    public static final Integer PLUGGABLE_TASK_SCHEDULED = new Integer(22);
    public static final Integer PLUGGABLE_TASK_RULES_GENERATOR = new Integer(23);
    public static final Integer PLUGGABLE_TASK_AGEING = new Integer(24);
    public static final Integer PLUGGABLE_TASK_PARTNER_COMMISSION = new Integer(25);
    public static final Integer PLUGGABLE_TASK_FILE_EXCHANGE = new Integer(26);
    public static final Integer PLUGGABLE_TASK_QUOTE_TAX_CALCULATION = new Integer(27);
    public static final Integer PLUGGABLE_TASK_MEDIATION_USER_PARTITIONING = new Integer(28);
    public static final Integer PLUGGABLE_TASK_ITEM_QUANTITY_RATING = new Integer(29);
    public static final Integer PLUGGABLE_TASK_UNDO_MEDIATION_TASK_FILTER= new Integer(30);
    public static final Integer PLUGGABLE_TASK_BILLABLE_USER_FILTER = 34;

    // pluggable task types (belongs to a category)
    public static final Integer PLUGGABLE_TASK_T_PAPER_INVOICE = new Integer(12);
    
    // invoice line types
    public static final Integer INVOICE_LINE_TYPE_ITEM_RECURRING = new Integer(1);
    public static final Integer INVOICE_LINE_TYPE_TAX = new Integer(2);
    public static final Integer INVOICE_LINE_TYPE_DUE_INVOICE = new Integer(3);
    public static final Integer INVOICE_LINE_TYPE_PENALTY = new Integer(4);
    public static final Integer INVOICE_LINE_TYPE_SUB_ACCOUNT = new Integer(5);
    public static final Integer INVOICE_LINE_TYPE_ITEM_ONETIME = new Integer(6);
    public static final Integer INVOICE_LINE_TYPE_ADJUSTMENT = new Integer(7);

    // permission types - this should be moved to PermissionConstant.java
    public static final Integer PERMISSION_TYPE_MENU= new Integer(1);
    
    // languages - when the project is a big company, we can do this right ! :p
    public static final Integer LANGUAGE_ENGLISH_ID = new Integer(1);
    public static final String LANGUAGE_ENGLISH_STR = "English";
    public static final Integer LANGUAGE_SPANISH_ID = new Integer(2);
    public static final String LANGUAGE_SPANISH_STR = "Spanish";
    public static final Integer LANGUAGE_GERMAN_ID = new Integer(3);
    public static final String LANGUAGE_GERMAN_STR = "German";

    public static final Integer ORDER_PROCESS_ORIGIN_PROCESS = new Integer(1);
    public static final Integer ORDER_PROCESS_ORIGIN_MANUAL = new Integer(2);

    //Notification Preference Types
    public static final Integer PREFERENCE_TYPE_SELF_DELIVER_PAPER_INVOICES = new Integer(13);
    public static final Integer PREFERENCE_TYPE_INCLUDE_CUSTOMER_NOTES = new Integer(14);
    public static final Integer PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP = new Integer(15);
    public static final Integer PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP2 = new Integer(16);
    public static final Integer PREFERENCE_TYPE_DAY_BEFORE_ORDER_NOTIF_EXP3 = new Integer(17);
    public static final Integer PREFERENCE_TYPE_USE_INVOICE_REMINDERS = new Integer(21);
    public static final Integer PREFERENCE_TYPE_NO_OF_DAYS_INVOICE_GEN_1_REMINDER = new Integer(22);
    public static final Integer PREFERENCE_TYPE_NO_OF_DAYS_NEXT_REMINDER = new Integer(23);

    // notification message types
    public static final Integer NOTIFICATION_TYPE_INVOICE_EMAIL = 1;
    public static final Integer NOTIFICATION_TYPE_USER_REACTIVATED = 2;
    public static final Integer NOTIFICATION_TYPE_USER_OVERDUE = 3;
    public static final Integer NOTIFICATION_TYPE_USER_OVERDUE_2 = 4;
    public static final Integer NOTIFICATION_TYPE_USER_OVERDUE_3 = 5;
    public static final Integer NOTIFICATION_TYPE_USER_SUSPENDED = 6;
    public static final Integer NOTIFICATION_TYPE_USER_SUSPENDED_2 = 7;
    public static final Integer NOTIFICATION_TYPE_USER_SUSPENDED_3 = 8;
    public static final Integer NOTIFICATION_TYPE_USER_DELETED = 9;
    public static final Integer NOTIFICATION_TYPE_INVOICE_PAPER = 12;
    public static final Integer NOTIFICATION_TYPE_ORDER_EXPIRE_1 = 13;
    public static final Integer NOTIFICATION_TYPE_ORDER_EXPIRE_2 = 14;
    public static final Integer NOTIFICATION_TYPE_ORDER_EXPIRE_3 = 15;
    public static final Integer NOTIFICATION_TYPE_PAYMENT_SUCCESS = 16;
    public static final Integer NOTIFICATION_TYPE_PAYMENT_FAILED = 17;
    public static final Integer NOTIFICATION_TYPE_INVOICE_REMINDER = 18;
    public static final Integer NOTIFICATION_TYPE_CREDIT_CARD_UPDATE = 19;
    public static final Integer NOTIFICATION_TYPE_LOST_PASSWORD = 20;
    public static final Integer NOTIFICATION_TYPE_INITIAL_CREDENTIALS = 21;
    public static final Integer NOTIFICATION_TYPE_USAGE_POOL_CONSUMPTION = 25;
    public static final Integer NOTIFICATION_TYPE_SSO_ENABLED = 35;
    public static final Integer NOTIFICATION_TYPE_SCHEDULED_JOB_STARTED = 36;
    public static final Integer NOTIFICATION_TYPE_SCHEDULED_JOB_COMPLETED = 37;

    // contact type
    public static final Integer ENTITY_CONTACT_TYPE = new Integer(1);

    //Jbilling Table Ids
    public static final Integer ENTITY_TABLE_ID = new Integer(5);

    //Internal AssetStatus objects
    public static final Integer ASSET_STATUS_MEMBER_OF_GROUP = 1;

    // primary currency id is assumed to be USD currently
    public static final Integer PRIMARY_CURRENCY_ID = 1;

    //Euro currency Id
    public static final Integer EURO_CURRENCY_ID = 3;

    // JBilling Interface Name
    public static final String I_SCHEDULED_TASK = "com.sapienter.jbilling.server.process.task.IScheduledTask";
    // AbstractSimpleScheduledTask parameter
    public static final String PARAM_START_TIME = "start_time";
    public static final String PARAM_END_TIME = "end_time";
    public static final String PARAM_REPEAT = "repeat";
    // Scheduled Plugin Date/Time Format
    public static final String DATE_TIME_FORMAT = "yyyyMMdd-HHmm";

    public static final String CC_DATE_FORMAT = "MM/yyyy";
    public static final String OBSCURED_NUMBER_FORMAT = "************"; // + last four digits
    public static final Integer OBSCURED_CARD_LENGTH = new Integer(16);
    // Currency Field Names
    public static final String FIELD_CURRENCY = "currency";
    public static final String FIELD_FEE_CURRENCY = "feeCurrency";
    
    // Decimal Point String Constant
    public static final String DOT = ".";
    public static final String DECIMAL_POINT = ".";
    public static final String SINGLE_SPACE = " ";
    public static final String COLON = ":";
    public static final String PIPE = "|";

    // mediation tasks interface names
    public static final String MEDIATION_READER_INTERFACE = "com.sapienter.jbilling.server.mediation.task.IMediationReader";
	public static final String MEDIATION_PROCESSOR_INTERFACE = "com.sapienter.jbilling.server.mediation.task.IMediationProcess";

    //Account Type Table
    public static final String TABLE_ACCOUNT_TYPE = "account_type";
    
    // Reseller Customer Constants
    public static final String RESELLER_PASSWORD = "P@ssword1";
    
    //Route Table
    public static final String TABLE_ROUTE = "route";
    //Route RateCard Table
    public static final String TABLE_ROUTE_RATE_CARD = "route_rate_card";
    // Usage Pool Cycle Periods
    public static final String USAGE_POOL_CYCLE_PERIOD_DAYS = "Days";
    public static final String USAGE_POOL_CYCLE_PERIOD_MONTHS = "Months";
    public static final String USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS = "Billing Periods";
    public static final String CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_DELETE = "CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_DELETE";
    public static final String CUSTOMER_PLAN_UNSUBSCRIBE_UPDATE_ACTIVE_UNTIL = "CUSTOMER_PLAN_UNSUBSCRIBE_UPDATE_ACTIVE_UNTIL";
    public static final String CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_FINISHED = "CUSTOMER_PLAN_UNSUBSCRIBE_ORDER_FINISHED";
    public static final String CUSTOMER_PLAN_UNSUBSCRIBE_PLAN_REMOVE = "CUSTOMER_PLAN_UNSUBSCRIBE_PLAN_REMOVE";
    
    public static final String PRICE_FIELD_FILE_NAME = "file_name";

    // Usage Pool Consumption
    public static final String FUP_CONSUMPTION_NOTIFICATION = "Usage Pool Consumption Notification";
    public static final String FUP_CONSUMPTION_FEE = "Usage Pool Consumption Fee";
    
    // Answer connect mediation configuration
    public static final String PHONE_META_FIELD = "INBOUND_CALLS_IDENTIFIER";
    public static final String CUSTOMER_PRIMARY_ACCOUNT_NUMBER = "primaryAccountNumber";
    public static final String ACTIVE_RESPONSE_IDENTIFIER = "ACTIVE_RESPONSE_IDENTIFIER";
    public static final String CHAT_IDENTIFIER = "CHAT_IDENTIFIER";
    
    public static final String MEDIATION_HOME = "mediation/";
    public static final String MEDIATION_FILE_NAME_PREFIX = "Daily-CDRs_";
    public static final String MEDIATION_FILE_NAME_SUFFIX = ".csv";
    public static final String MEDIATION_FILE_RECYCLED_EXTENSION = "- Recycled";
    public static final String INBOUND_CALLS_CFG_NAME = "Inbound Calls";
    public static final String ACTIVE_RESPONSE_CFG_NAME = "Active Response";
    public static final String CHAT_CFG_NAME = "Chat";

    public static final String PROPERTY_RUN_CUSTOMER_USAGE_POOL = "process.run_customer_usage_pool";
    public static final String PROPERTY_RUN_COMMISION = "process.run_commission";
    public static final String PROPERTY_RUN_API_ONLY_BUT_NO_BATCH = "process.run_api_only_but_no_batch";
    public static final String PROPERTY_RUN_ORDER_UPDATE = "process.run_order_update";

    public static final String BLANK_STRING = "";
    
    // Main Subscription Period Temlate Name
    public static final String TEMPLATE_MONTHLY = "monthly";
    public static final String TEMPLATE_WEEKLY = "weekly";
    public static final String TEMPLATE_DAILY = "daily";
    public static final String TEMPLATE_YEARLY= "yearly";
    public static final String TEMPLATE_SEMI_MONTHLY= "semiMonthly";
    
    public static final Integer SEMI_MONTHLY_END_OF_MONTH= new Integer(15);

    public static final String PLANS_INTERNAL_CATEGORY_NAME = "plans";

    //encryption scheme
    public static final String PASSWORD_ENCRYPTION_SCHEME = "security.password_encrypt_scheme";
	public static final String PROPERTY_LOCKOUT_PASSWORD = "security.lockout_password";

    //Invoice Templates
    public static final String TABLE_INVOICE_TEMPLATE = "invoice_template";
    public static final String TABLE_INVOICE_TEMPLATE_VERSION = "invoice_template_version";
    public static final String TABLE_INVOICE_TEMPLATE_FILE = "invoice_template_file";
    public static final String DEFAULT_ITG = "DEFAULT_ITG";

    public static final String COMPANY_METAFIELD_INVOICE_LINES_PRODUCT_TYPES = "Product Categories in Invoice";
    // Enumerations
    public static final Integer ENUMERATION_VALUE_MAX_LENGTH = Integer.valueOf(50);

    public static final String RESERVAED_STATUS = "Reserved";

    /*
        Response error codes
    */
    public static final Integer ERROR_CODE_404 = Integer.valueOf(404);
    public static final String PLAN_ITEM_ID = "PLAN_ITEM_ID";
    public static final String PLAN_ID = "PLAN_ID";

    public static final String ACH_ACCOUNT_NUMBER_ENCRYPTED = "ach.account.number.encrypted";
    public static final String ACH_ACCOUNT_ROUTING_NUMBER = "ach.routing.number";
    public static final String ACH_ACCOUNT_NUMBER = "ach.account.number";
    public static final String ACH_CUSTOMER_NAME = "ach.customer.name";
    public static final String ACH_ACCOUNT_TYPE = "ach.account.type";
    public static final String AUTOPAYMENT_LIMIT = "autopayment.limit";
    public static final String AUTOPAYMENT_TASK_CLASS = "com.sapienter.jbilling.server.billing.task.PaymentProcessTask";
    public static final String BILLINGPROCESS_TASK_CLASS = "com.sapienter.jbilling.server.billing.task.BillingProcessTask";
    public static final String INTERVAL_PARAM = "interval";
    public static final String JOB_CHAIN_IDS_PARAM = "job_chain_ids";
    public static final String PAYMENTFAKE_TASK_CLASS = "com.sapienter.jbilling.server.pluggableTask.PaymentFakeTask";

    public static final String FLAT_FILE_PARSER = "flatFileParser";

    public static final String DEFAULT_TIMEZONE = "UTC";

    /*
     * Invoice Template Adjustment Type
     */
    public static final String CREDIT_TYPE_CREDIT_NOTE = "CREDIT NOTE";
    public static final String CREDIT_TYPE_CREDIT_PAYMENT = "CREDIT PAYMENT";
    public static final String CREDIT_TYPE_CREDIT_INVOICE_LINE = "CREDIT INVOICE LINE";
    
    /*
     * Cancellation Request Properties 
     */
    public static final String CANCELLATION_REQUEST_ID = "id";
    public static final String CANCELLATION_REQUEST_DATE = "CancellationDate";
    
    //Customer Cancellation Status Description
    public static final String CUSTOMER_CANCELLATION_STATUS_DESCRIPTION = "Cancelled on Request";

    public static final String UNDERSCORE_DELIMITER = "_";

    /**
     * Service Summary
     */
    public static final String TABLE_SERVICE_SUMMARY="service_summary";

    public class DeutscheTelekom {

        public static final String EXTERNAL_ACCOUNT_IDENTIFIER = "externalAccountIdentifier";

        public static final String APPDIRECT_SUBSCRIPTION_IDENTIFIER = "appdirectSubscriptionIdentifier";

        public static final String APPDIRECT_PRODUCT_IDENTIFIER = "appdirectProductIdentifier";

        public static final String APPDIRECT_PRODUCT_DETAILS = "appdirectProductDetails";

        public static final String FIRST_NAME = "First Name";

        public static final String LAST_NAME = "Last Name";

        public static final String POSTAL_CODE = "Postal Code";

        public static final String COUNTRY = "Country";

        public static final String STATE_PROVINCE = "State/Province";

        public static final String EMAIL_ADDRESS = "Email Address";

        public static final String CITY = "City";

        public static final String STR_DUMMY = "Dummy";

        public static final String COUNTRY_CODE = "DE";

        public static final String RESOURCE_TYPE_SUBSCRIPTION = "SUBSCRIPTION";

        public static final String APPDIRECT_COMPANY_API_BASE_URL = "api_company_base_url";

        public static final String APPDIRECT_CONSUMER_KEY = "appdirect_consumer_key";

        public static final String APPDIRECT_CONSUMER_SECRET = "appdirect_consumer_secret";

        public static final String RESOURCE_TYPE_COMPANY = "COMPANY";

        public static final String PAYLOAD_ACTION_CHANGED = "CHANGED";

        public static final String ACCOUNT_TYPE_ID= "account_type_id";
    }
    public static final String MF_DETAIL_FILE_FOLDER = "Detail File Folder";
    public static final String COPIED_ASSET = "Copied Asset";
    public static final String COPIED = "_Copied";
    //used for Swap Asset
    public static final String REPLACEMENT = "REPLACEMENT";
    public static final String ENROLLMENT = "ENROLLMENT";
    public static final String JMR_PROCESSED_STATUS = "PROCESSED";
    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";
    public static final String DATE_FORMAT_YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm";    
    public static final String SUBSCRIPTION_ORDER_NOT_FOUND="subscription order not found, so can not caluclate event date.";
    public static final String CUT_OFF_BILLING_PROCESS_ID = "Cut Off Billing Process Id";
}
