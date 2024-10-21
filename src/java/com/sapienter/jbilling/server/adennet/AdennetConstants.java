/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet;

import com.sapienter.jbilling.server.externalservice.configuration.ExternalConfigurationTask;

import java.time.format.DateTimeFormatter;

public class AdennetConstants {
    // MetaFields - plan
    public static final String PLAN_FEE = "Plan Fee";
    public static final String DOWNLOAD_CAPACITY = "Download Capacity";

    // MetaFields - Customer Type
    public static final String META_FIELD_CUSTOMER_TYPE = "Customer Type";
    public static final String CUSTOMER_TYPE_VIP = "VIP";
    public static final String CUSTOMER_TYPE_GOVERNMENT = "Government";

    public static final String META_FIELD_GOVERNORATE = "Governorate";
    public static final String ENUMERATION_IDENTIFICATION_TYPE = "Identification Type";
    public static final String IDENTIFICATION_TYPE_NATIONAL_ID = "National ID";
    public static final String IDENTIFICATION_TYPE_PASSPORT = "Passport";
    public static final String IDENTIFICATION_TYPE_COMPANY_LETTER = "Company Letter";
    public static final String IDENTIFICATION_TYPE_OFFICIAL_LETTER = "Official Letter";

    // Receipt
    public static final String SOURCE_POS = "POS";
    public static final String OPRATION_TYPE_WALLET_TOP_UP = "Top up";
    public static final String OPRATION_TYPE_CHARGE_RECEIPT = "Charge Receipt";
    public static final String PLAN_PRICE = "Plan price";
    public static final String PRE_PAID = "Pre-Paid";
    public static final String REFUND_AMOUNT = " Refund ";


    // User Conatct info
    public static final String FIRST_NAME = "First Name";
    public static final String CONTACT_NUMBER = "Contact Number";
    public static final String EMAIL_ID = "Email Id";
    public static final String ADDRESS_LINE_1 = "Address Line 1";

    //Asset
    public static final String ASSET_STATUS_AVAILABLE = "Available";
    public static final String ASSET_STATUS_RELEASED = "Released";
    public static final String ASSET_STATUS_IN_USE = "In Use";
    public static final String ASSET_STATUS_DISCARDED = "Discarded";
    public static final String SIM_REISSUE_FEE_NARRATION = "SIM reissue fee";
    public static final String SIM_REISSUE = "SIM reissue fee";
    public static final DateTimeFormatter ADENNET_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // transaction recharge history status
    public static final String TRN_STATUS_WALLET_TOP_UP = "Wallet Top Up";
    public static final String TRN_STATUS_RECHARGE = "Recharge";
    public static final String TRN_STATUS_RECHARGE_REQUEST = "Recharge Request";
    public static final String TRN_STATUS_REFUND = "Refund";
    public static final String TRN_STATUS_REFUNDED = "Refunded";
    public static final String TRN_STATUS_BUY_SUBSCRIPTION = "Buy Subscription";
    public static final String TRN_STATUS_SIM_REISSUED = "SIM Reissue";
    public static final DateTimeFormatter DATE_TIME_FORMATTER_TRANSACTION = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    public static final String ACTIVE_CONSUMPTION_USAGE_MAP = "ACTIVE";

    // jbilling roles
    public static final String ROLE_POS_MEMBER = "POS Member";
    public static final String ROLE_OPERATION_USER = "Operation User";

    // Adennet Permissions
    public static final String PERMISSION_RECHARGE = "CUSTOMER_2002";
    public static final String PERMISSION_BUY_SUBSCRIPTION = "CUSTOMER_2003";
    public static final String PERMISSION_RECHARGE_HISTORY_MENU = "MENU_2004";
    public static final String PERMISSION_SUSPEND_ACTIVATE = "PRODUCT_CATEGORY_STATUS_AND_ASSETS_2005";
    public static final String PERMISSION_REISSUE = "PRODUCT_CATEGORY_STATUS_AND_ASSETS_2006";
    public static final String PERMISSION_SUSPEND_ACTIVATE_OR_REISSUE = "hasAnyRole('PRODUCT_CATEGORY_STATUS_AND_ASSETS_2005', 'PRODUCT_CATEGORY_STATUS_AND_ASSETS_2006')";
    public static final String PERMISSION_VIEW_IDENTITY_DOCUMENT = "CUSTOMER_2007";
    public static final String PERMISSION_CHANGE_CUSTOMER_TYPE = "CUSTOMER_2008";
    public static final String PERMISSION_REFUND_WALLET_BALANCE = "CUSTOMER_2009";
    public static final String PERMISSION_VIEW_ALL_RECHARGE_TRANSACTIONS = "CUSTOMER_2010";
    public static final String PERMISSION_REFUND_RECHARGE_TRANSACTION = "CUSTOMER_2011";
    public static final String PERMISSION_RELEASE = "PRODUCT_CATEGORY_STATUS_AND_ASSETS_2012";
    public static final String PERMISSION_VIEW_CRM_USER_REPORT = "REPORT_2013";
    public static final String PERMISSION_VIEW_BANK_USER_REPORT = "REPORT_2014";
    public static final String PERMISSION_VIEW_FINANCE_TOTAL_REPORT = "REPORT_2015";
    public static final String PERMISSION_VIEW_INACTIVE_SUBSCRIBER_NUMBERS_REPORT = "REPORT_2016";
    public static final String PERMISSION_TOTAL_CUSTOMER_COUNT = "CUSTOMER_2017";

    //report
    public static final String REPORT_TYPE_ADENNET = "adennet";
    public static final String REPORT_CRM_USER = "ums_crm_user_report";
    public static final String REPORT_BANK_USER = "ums_bank_user_report";
    public static final String REPORT_FINANCE_TOTAL = "ums_finance_total_report";
    public static final String REPORT_INACTIVE_SUBSCRIBER_NUMBERS = "ums_inactive_subscriber_numbers_report";
    public static final String REPORT_AUDIT_LOG = "audit_log_report";

    public static final String ADENNET_PIN_PATTERN = "^\\d{4}$";
    public static final String ADENNET_PUK_PATTERN = "^\\d{8}$";
    public static final String CUSTOMER_TYPE_EMPLOYEE = "Employee";
    public static final String GOVERNERATE = "Aden";
    public static final String SOURCE_AUTO_RECHARGE = "auto-recharge";
    public static final Integer USER_STATUS_DEACTIVATED = 3;
    public static final String PERMISSION_VIEW_ADD_FILTERS = "CUSTOMER_2018";

    public static final Integer INT_ASSET_STATUS_IN_USE = 202;
    public static final Integer INT_ASSET_STATUS_AVAILABLE = 200;
    public static final Integer INT_ASSET_STATUS_RELEASED = 300;
    public static final String TOKEN_TYPE_BEARER = "Bearer";
    public static final String PERMISSION_VIEW_AUDIT_LOG_REPORT = "REPORT_2019";
    public static final String ADENNET_TIMEZONE = "Asia/Aden";

    //External config
    public static final String EXTERNAL_SERVICE_CLASS_NAME = AdennetExternalConfigurationTask.class.getName();
    public static final String EXTERNAL_SERVICE_INTERFACE_NAME = ExternalConfigurationTask.class.getName();
    public static final String ORDER_PRE_PAID = "pre paid";
    public static final String ORDER_POST_PAID = "post paid";

    //Usage Map
    public static final String MAP_TYPE_DATA = "data";
    public static final String MAP_TYPE_VOICE = "voice";
}
