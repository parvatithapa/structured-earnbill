package com.sapienter.jbilling.server.spc;

import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.util.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import junit.framework.TestCase;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.resources.CancelOrderInfo;
import com.sapienter.jbilling.server.creditnote.CreditNoteInvoiceMapWS;
import com.sapienter.jbilling.server.creditnote.CreditNoteWS;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoice.task.SpcCreditOrderCreationTask;
import com.sapienter.jbilling.server.item.AssetStatusDTOEx;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.item.RatingConfigurationWS;
import com.sapienter.jbilling.server.item.tasks.BasicItemManager;
import com.sapienter.jbilling.server.item.tasks.SPCRemoveAssetFromActiveOrderTask;
import com.sapienter.jbilling.server.item.tasks.SPCUsageManagerTask;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.custommediation.spc.SPCConstants;
import com.sapienter.jbilling.server.mediation.evaluation.task.SPCMediationCurrentPeriodEvaluationStrategyTask;
import com.sapienter.jbilling.server.mediation.evaluation.task.SPCMediationEvaluationStrategyTask;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.notification.MessageSection;
import com.sapienter.jbilling.server.notification.builder.NotificationBuilder;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.order.task.AssetAssignmentEvaluationStrategy;
import com.sapienter.jbilling.server.order.task.RefundOnCancelTask;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.pluggableTask.FullCreativeCustomEmailTokenTask;
import com.sapienter.jbilling.server.pluggableTask.FullCreativeCustomInvoiceFieldsTokenTask;
import com.sapienter.jbilling.server.pluggableTask.OrderChangeBasedCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.OrderFilterTask;
import com.sapienter.jbilling.server.pluggableTask.PaperInvoiceNotificationTask;
import com.sapienter.jbilling.server.pluggableTask.SPCOrderFilterTask;
import com.sapienter.jbilling.server.pluggableTask.SPCPaperInvoiceNotificationTask;
import com.sapienter.jbilling.server.pluggableTask.TelcoInvoiceParametersTask;
import com.sapienter.jbilling.server.pluggableTask.TelcoOrderLineBasedCompositionTask;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.RatingUnitWS;
import com.sapienter.jbilling.server.pricing.RouteRecordWS;
import com.sapienter.jbilling.server.pricing.cache.MatchingFieldType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryWS;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryWSMapper;
import com.sapienter.jbilling.server.spc.billing.SPCUserFilterTask;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.usagePool.task.CustomerPlanSubscriptionProcessingTask;
import com.sapienter.jbilling.server.usagePool.task.CustomerUsagePoolConsumptionActionTask;
import com.sapienter.jbilling.server.usagePool.task.ProrateCustomerUsagePoolTask;
import com.sapienter.jbilling.server.usagePool.task.SPCCustomerUsagePoolTask;
import com.sapienter.jbilling.server.usagePool.task.SPCUsagePoolFeeChargingTask;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.MatchingFieldWS;
import com.sapienter.jbilling.server.user.RouteRateCardWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryWS;
import com.sapienter.jbilling.server.servicesummary.ServiceSummaryWSMapper;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.NameValueString;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.DiscountWS;

import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;


import com.sapienter.jbilling.server.billing.task.InvoiceEmailDispatcherTask;

import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.pluggableTask.FullCreativeCustomInvoiceFieldsTokenTask;
import com.sapienter.jbilling.server.pluggableTask.FullCreativeCustomEmailTokenTask;
import com.sapienter.jbilling.server.order.task.OrderChangeUpdateTask;
import com.sapienter.jbilling.server.spc.SPCReportCSVExporterTask;


@ContextConfiguration(classes = SPCTestConfig.class)
public class SPCBaseConfiguration extends AbstractTestNGSpringContextTests {

    protected static final String CUSTOMER_TYPE_VALUE_PRE_PAID = "Pre Paid";
    protected static final String CUSTOMER_TYPE_VALUE_POST_PAID = "Post Paid";

    //@formatter:off
    protected static final Logger logger                        = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Integer PRANCING_PONY = Integer.valueOf(1);
    protected static final String BASE_DIR                      = "base_dir";
    protected static final String CDR_BASE_DIRECTORY            = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/cdr";
    protected static final String ROUTE_RATE_CARD_FILE          = Util.getSysProp(BASE_DIR) + "/spc-mediation-test/rrc/";
    protected static final String PLAN_LEVEL_METAFIELD          = "Plan Rating";

    // Creating plan level meta-field
    protected static final String PLAN_METAFIELD_PLAN_RATING_NAME              = "Plan Rating";
    protected static final String PLAN_METAFIELD_QUANTITY_RESOLUTION_UNIT_NAME = "Quantity Resolution Unit";
    protected static final String PLAN_METAFIELD_INTERNET_TECHNOLOGY_NAME      = "Internet Technology";
    protected static final String PLAN_METAFIELD_TAX_SCHEME_NAME               = "Tax Scheme";
    protected static final String PLAN_METAFIELD_PLAN_GL_NAME                  = "Plan GL";
    protected static final String PLAN_METAFIELD_COSTS_GL_CODE_NAME            = "Costs GL Code";
    protected static final String PLAN_METAFIELD_USAGE_POOL_GL_CODE            = "Usage Pool GL Code";
    protected static final String PLAN_METAFIELD_USAGE_POOL_COSTS_GL_CODE      = "Usage Pool Costs GL Code";

    protected static final String ORDER_CREATION_ASSERT         = "Order Creation Failed";
    protected static final String SPC_MEDIATED_USAGE_CATEGORY   = "SPC Mediation Usage Category";
    protected static final String SPC_MEDIATION_JOB_NAME        = "spcMediationJobLauncher";
    protected static final String CODE_STRING                   = "CODE_STRING";
    protected static final String ROUTE_ID                      = "route_id";
    protected static final String ACCOUNT_NAME                  = "SPC Test Account";
    private static final String SPC_MEDIATION_CONFIG_NAME       = "spcMediationJob";
    private static final String OPTUS_MUR_MEDIATION_CONFIG_NAME = "optusMurMediationJob";
    protected static final String OPTUS_MUR_MEDIATION_JOB_NAME  = "optusMurMediationJob";
    private static final int ORDER_CHANGE_STATUS_APPLY_ID       = 3;
    private static final File TEMP_DIR_PATH                     = new File(System.getProperty("java.io.tmpdir"));
    private static final String DATA_RATING_UNIT_NAME           = "SPC Data Rating Unit";
    private static final String UNIT_NAME_BYTE_TO_MB_COVERTER   = "ByteToMBCoverter";
    private static final String UNIT_NAME_KB_TO_MB_CONVERTER   = "KBToMBConverter";
    public static final String INTERNET_ASSET_PLAN_ITEM_CODE   = "internet-user-names";
    public static final String SPC_CUSTOMER_CARE_ITEM_CODE     = "Calls to Southern Phone";
    public static final String SPC_CUSTOMER_CARE_ITEM_ID       = SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_CUSTOMER_CARE_NUMBER_ITEM_ID;
    public static final int BASIC_ITEM_MANAGER_PLUGIN_ID = 1;
    public static final int ORDER_FILTER_TASK_PLUGIN_ID = 4;

    public static final String PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT = "Quantity Resolution Unit";
    public static final String PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE = "Internet Technology";
    public Integer mediationEvaluationStrategyPluginId;
    private static final Integer REFUND_ON_CANCEL_TASK_PLUGIN_ID = 440;
    private static final String PARAM_LABEL_ADJUSTMENT_PRODUCT_ID = "adjustment_product_id";
    private static final Integer ADJUSTMENT_PRODUCT_ID = 320111;

    private final static String PARAM_CREDIT_POOL_TABLE_NAME = "credit_pool";
    private final static String PARAM_TAX_TABLE_NAME = "tax_scheme";
    private final static String PARAM_ROUNDING_MODE = "rounding mode";
    private final static String PARAM_ROUND_HALF_UP = "ROUND_HALF_UP";
    private final static String PARAM_TAX_DATE_FORMAT = "dd-MM-yyyy";
    private final static String PARAM_ROUNDING_SCALE = "rounding scale";
    private final static String PARAM_MINIMUM_CHARGE = "minimum charge";
    protected static final Integer CURRENCY_AUD = 11;
    private static final Integer CURRENCY_USD = 1;
    private static final String backupExchangeRate = "1.5000";
    private static final String backupSystemRate = "1.2880";
    // Data tables
    private final static String DATA_TABLE_PLAN_BASED_FREE_CALL_INFO = "plan_based_free_call_info";
    protected final static String DATA_TABLE_OPTUS_MUR_ALERT = "route_70_wookie_optus_mur_alert";
    protected final static String DATA_TABLE_USAGE_POOL_ALERT = "route_70_wookie_usage_pool_alert";
    private final static String DATA_TABLE_CALLTOZERO = "calltozero";
    
    // Services
    protected static final String INTERNET_SERVICES_CATEGORY = "Internet Services";
    protected static final String MOBILE_SERVICES_CATEGORY = "Mobile Services";
    protected static final String VOICE_SERVICES_CATEGORY = "Voice Services";
    // Adjustments
    protected static final String CREDIT_PRODUCTS_CATEGORY = "Credit Products Category";
    protected static final String DEBIT_PRODUCTS_CATEGORY = "Debit Products Category";
    protected static final String CREDIT_DEBIT_ADJUSTMENTS_CATEGORY = "Credit/Debit Adjustments";
    protected static final String OTHER_CHARGES_AND_CREDITS_CATEGORY = "Other Charges and Credits";
    protected static final String CREDIT_ADJUSTMENT_CATEGORY_CATEGORY = "Credit Adjustment Category";
    // Usage
    protected static final String INTERNET_USAGE_CATEGORY = "Internet Usage";
    protected static final String MOBILE_USAGE_CATEGORY = "Mobile Usage";
    protected static final String VOICE_USAGE_CATEGORY = "Voice Usage";
    protected static final String INBOUND_SERVICES_CATEGORY = "Inbound Services";
    protected static final String INBOUND_USAGE_CATEGORY = "Inbound Usage";

    // Numbers
    protected static final String VOIP_NUMBERS_CATEGORY = "Voip Numbers";
    protected static final String MOBILE_NUMBERS_CATEGORY = "Mobile Numbers";
    protected static final String VOICE_NUMBERS_CATEGORY = "Voice Numbers";
    
    protected static final String CREDIT_POOL_CATEGORY = "Credit_Pool_Category";
    protected static final String EXCLUDE_PRICE_ZERO_ASSETS_CATEGORY = "Exclude Price Zero Assets";
    protected static final String ACCOUNT_CHARGES_CATEGORY = "Account Charges";
    protected static final String UNEARNED_REVENUE_EXCLUDED_CATEGORY = "Unearned Revenue - Excluded";
    protected static final String UNBILLED_REVENUE_EXCLUDED_CATEGORY = "Unbilled Revenue - Excluded";
    protected static final String MIGRATION_ADJUSTMENT_CATEGORY = "Migration Adjustment";
    protected static final String INTERNET_USER_NAMES_CATEGORY = "Internet User Names";
    protected static final String INTERNET_USER_NAMES_PRODUCT = "internet_user_names";
    protected static final String USAGE_PRODUCT_CODE_MOBILE_NUMBERS = "mobile_numbers";
    protected static final String USAGE_PRODUCT_CODE_OM_MOBILE_DATA = "om_mobile_data"; // KB to MB
    protected static final String USAGE_PRODUCT_CODE_OM_MUR_MOBILE_DATA = "om_mur_mobile_data";// byte to MB
    protected static final String USAGE_PRODUCT_CODE_OM_MOBILE_TO_INTERNATIONAL = "om_mobile_to_international";
    protected static final String USAGE_PRODUCT_CODE_OM_VOICEMAIL = "om_voicemail";
    protected static final String USAGE_PRODUCT_CODE_OM_MMS = "om_mms";
    protected static final String USAGE_PRODUCT_CODE_OM_MOBILE_TO_FIXED_CALLS = "om_mobile_to_fixed_calls";
    protected static final String USAGE_PRODUCT_CODE_OM_MOBILE_TO_MOBILE_CALLS = "om_mobile_to_mobile_calls";
    protected static final String USAGE_PRODUCT_CODE_OM_ROAMING = "om_roaming";
    protected static final String USAGE_PRODUCT_CODE_OM_MOBILE_SPECIAL_CALLS = "om_mobile_special_calls";
    protected static final String USAGE_PRODUCT_CODE_OM_SMS = "om_sms";
    protected static final String USAGE_PRODUCT_CODE_VOICE_NUMBERS = "voice_numbers";
    protected static final String USAGE_PRODUCT_CODE_SCONNECT_TOTAL = "SCONNECT-INT-TOTAL";
    protected static final String USAGE_PRODUCT_CODE_OM_INTERNATIONAL_MMS = "om_international_mms";
    protected static final String USAGE_PRODUCT_CODE_OM_INTERNATIONAL_SMS = "om_international_sms";
    protected static final String USAGE_PRODUCT_CODE_OM_MOBILE_PREMIUM_SMS = "om_mobile_premium_sms";
    
    protected static final String USAGE_PRODUCT_CODE_TM_SMS = "tm_sms";
    protected static final String USAGE_PRODUCT_CODE_TM_VOICEMAIL = "tm_voicemail";
    protected static final String USAGE_PRODUCT_CODE_TM_MOBILE_ROAMING_CHARGES_NO_GST = "tm_mobile_roaming_charges_(no_gst)";
    protected static final String USAGE_PRODUCT_CODE_TM_MMS = "tm_mms";
    protected static final String USAGE_PRODUCT_CODE_TM_MOBILE_SPECIAL_CALLS = "tm_mobile_special_calls";
    protected static final String USAGE_PRODUCT_CODE_TM_MOBILE_TO_MOBILE_CALLS = "tm_mobile_to_mobile_calls";
    protected static final String USAGE_PRODUCT_CODE_TM_MOBILE_TO_INTERNATIONAL = "tm_mobile_to_international";
    protected static final String USAGE_PRODUCT_CODE_TM_MOBILE_DATA = "tm_mobile_data";
    protected static final String USAGE_PRODUCT_CODE_TM_MOBILE_TO_FIXED_CALLS = "tm_mobile_to_fixed_calls";
    
    protected static final String USAGE_PRODUCT_CODE_AA_LONG_DISTANCE_CALLS = "aa_long_distance_calls";
    protected static final String USAGE_PRODUCT_CODE_AA_DIRECTORY_AND_ASSISTED_CALLS = "aa_directory_&_assisted_calls";
    protected static final String USAGE_PRODUCT_CODE_AA_CALLS_TO_MOBILES = "aa_calls_to_mobiles";
    protected static final String USAGE_PRODUCT_CODE_AA_INTERNATIONAL_CALLS = "aa_international_calls";
    protected static final String USAGE_PRODUCT_CODE_AA_NATIONAL_CALLS = "aa_national_calls";
    protected static final String USAGE_PRODUCT_CODE_AA_CALLS_TO_13_NUMBERS = "aa_calls_to_13_numbers";
    protected static final String USAGE_PRODUCT_CODE_AA_LOCAL_CALLS = "aa_local_calls";
    
    protected static final String USAGE_PRODUCT_CODE_SC_NATIONAL_CALLS = "sc_national_calls";
    protected static final String USAGE_PRODUCT_CODE_SC_INTERNATIONAL_CALLS = "sc_international_calls";
    protected static final String USAGE_PRODUCT_CODE_SC_INTERNATIONAL_CALLS_SP_DEST = "sc_international_calls_sp_dest";
    protected static final String USAGE_PRODUCT_CODE_SC_INTERNATIONAL_CALLS_SP_DEST_30 = "sc_international_calls_sp_dest_30";
    protected static final String USAGE_PRODUCT_CODE_SC_CALLS_TO_MOBILES = "sc_calls_to_mobiles";
    protected static final String USAGE_PRODUCT_CODE_SC_LOCAL_CALLS = "sc_local_calls";
    protected static final String USAGE_PRODUCT_CODE_SC_CALLS_TO_13_NUMBERS = "sc_calls_to_13_numbers";
    protected static final String USAGE_PRODUCT_CODE_SC_OTHER_CALLS = "sc_other_calls";
    protected static final String USAGE_PRODUCT_CODE_SC_DIRECTORY_AND_ASSISTED_CALLS = "sc_directory_&_assisted_calls";
    // EN
    protected static final String USAGE_PRODUCT_CODE_EN_CALLS_TO_MOBILES = "en_calls_to_mobiles";
    protected static final String USAGE_PRODUCT_CODE_EN_INTERNATIONAL_CALLS = "en_international_calls";
    protected static final String USAGE_PRODUCT_CODE_EN_NATIONAL_CALLS = "en_national_calls";
    protected static final String USAGE_PRODUCT_CODE_EN_LOCAL_CALLS = "en_local_calls";
    protected static final String USAGE_PRODUCT_CODE_EN_CALLS_TO_13_NUMBERS = "en_calls_to_13_numbers";
    protected static final String USAGE_PRODUCT_CODE_VOIP_NUMBER_EXCLUDE_ZERO_PRICE_ASSET = "voip_number_exclude_zero_price_asset";
    protected static final String USAGE_PRODUCT_CODE_OP_INBOUND_LOCAL_TO_1300 = "op_inbound_local_to_1300";

    protected static final String INBOUND_PRODUCT_CODE_OP_INBOUND_COMMUNITY_CALLS = "op_inbound_community_calls";
    protected static final String INBOUND_PRODUCT_CODE_OP_INBOUND_MOBILE_TO_1300 = "op_inbound_mobile_to_1300";
    protected static final String INBOUND_PRODUCT_CODE_OP_INBOUND_NATIONAL_TO_1800 = "op_inbound_national_to_1800";
    protected static final String INBOUND_PRODUCT_CODE_OP_INBOUND_LOCAL_TO_1800 = "op_inbound_local_to_1800";
    protected static final String INBOUND_PRODUCT_CODE_OP_INBOUND_NATIONAL_TO_1300 = "op_inbound_national_to_1300";
    protected static final String INBOUND_PRODUCT_CODE_OP_INBOUND_MOBILE_TO_1800 = "op_inbound_mobile_to_1800";
    
    public static final String CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT = "CR-PCT";
    public static final String CREDIT_ADJUSTMENT_PRODUCT_CODE_MC = "MC";
    public static final String CREDIT_ADJUSTMENT_PRODUCT_CODE_VC = "VC";
    public static final String DEBIT_ADJUSTMENT_PRODUCT_CODE_DR_PCT = "DR-PCT";
    public static final String DEBIT_ADJUSTMENT_PRODUCT_CODE_DR_FLT = "DR-FLT";
    public static final String TELSTRA_4G_ASSET_PRODUCT = "Telstra_4G_Asset_Product";
    public static final String OPTUS_MOBILE_ASSET_PRODUCT = "Optus_Mobile_Asset_Product";
    public static final String AAPT_VOIP_ASSET_PRODUCT = "AAPT_VOIP_Asset_Product";
    protected static final String UNBILLED_REVENUE_INCLUDED_CATEGORY = "Unbilled Revenue - Included";
    protected static final String TELSTRA_4G_MOBILE_ASSET_CATEGORY = "Telstra 4G Mobile Asset";
    protected static final String OPTUS_MOBILE_ASSET_CATEGORY = "Optus Mobile Asset";
    protected static final String AAPT_VOIP_CATEGORY = "AAPT VOIP Asset";
    protected static final String UNEARNED_REVENUE_INCLUDED_CATEGORY = "Unearned Revenue - Included";
    protected static final String CUSTOMER_CARE_CATEGORY = "Customer_Care_Category";
    
    protected static final int  ONE_TIME_PERIOD     	   	=  1;
    protected static final String CUSTOMER_DETAILS = "Customer Details";
    protected static final String BILLING_ADDRESS = "Billing Address";
    protected static final int    MONTHLY_ORDER_PERIOD                          =  2;
    protected static final Random random = new Random ();

    private static final String CC_CARDHOLDER_NAME = "cc.cardholder.name";
    private static final String CC_NUMBER = "cc.number";
    private static final String CC_EXPIRY_DATE = "cc.expiry.date";
    private static final String CC_AUTOPAYMENT_AUTHORIZATION = "autopayment.authorization";

    private static final String BD_BANK_NAME = "Bank Name";
    private static final String BD_ACCOUNT_NUMBER = "Account Number";
    private static final String BD_BSB = "BSB";
    private static final String BD_ACCOUNT_NAME = "Account Name";

    protected static final String ENUMERATION_METAFIELD_NAME  = "Plan Rating";
    protected static final String ENUMERATION_CUSTOMER_TYPE   = "Customer Type";
    protected static final String ENUMERATION_STATE           = "State";
    protected static final String DIRECT_MARKETING            = "direct_marketing";
    protected static final String CRM_ACCOUNT_NUMBER          = "crmAccountNumber";

    private static final String PAYMENT_TYPE_CODE_BPAY = "BpayPaymentType";
    private static final String PAYMENT_TYPE_CODE_AUSTRALIA_POST= "Australia Post";
    private static final String PAYMENT_TYPE_CODE_BANK_DEBIT= "Bank Debit";
    Integer bpayPaymentMethodId = null;
    Integer apostPaymentMethodId = null;
    Integer bankDebitPaymentMethodId = null;
    
    protected static final String AUSTRALIA_POST    = "AUSTRALIA POST";
    protected static final String BANK_DEBIT        = "BANK DEBIT";
    protected static final String BPAY          = "BPAY";
    protected static final String CC                = "CC";
    protected static final String REFERENCE_NUMBER = "Reference Number";
    protected static final String BILLER_CODE     = "Biller Code";
    protected static final Integer CC_PM_ID       = 9;
    
    // event date within active since and active until dates
    protected static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    
    protected static final List<EnumerationValueWS> ENUMERATION_CUSTOMER_TYPE_VALUES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS(CUSTOMER_TYPE_VALUE_PRE_PAID));
            add(new EnumerationValueWS(CUSTOMER_TYPE_VALUE_POST_PAID));
        }};
    protected static final List<EnumerationValueWS> ENUMERATION_DIRECT_MARKETING = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS("Yes"));
            add(new EnumerationValueWS("No"));
        }};
    protected static final List<EnumerationValueWS> ENUMERATION_STATES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS("NSW"));
            add(new EnumerationValueWS("WA"));
        }};


    private Integer creditPool85Notification = null;
    private Integer creditPool50Notification = null;
    private Integer creditPool100Notification = null;

    private static final String CREDIT_POOL_85 = "Credit Pool 85%";
    private static final String CREDIT_POOL_50 = "Credit Pool 50%";
    private static final String CREDIT_POOL_100 = "Credit Pool 100%";
    private static Map<String, String> PRODUCT_CATEGOY_MAP;
    private static Map<String, String> NOTIFICAION_MAP;
    protected static final String ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1        = "SPC_OM_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_1        = "SPC_TM_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_2        = "SPC_TM_Plan_Rating_2";
    protected static final String ROUTE_RATE_CARD_SPC_TF_PLAN_RATING_1        = "SPC_TF_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_OF_PLAN_RATING_1        = "SPC_OF_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_OMT_PLAN_RATING_1       = "SPC_OMT_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_AGL_OM_PLAN_RATING_1       = "AGL_OM_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_AGL_SC_PLAN_RATING_1       = "AGL_SC_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_TF_OF_Plan_Rating_1_1       = "SPC_SC_Plan_Rating_1_1";
    protected static final String ROUTE_RATE_CARD_SPC_SC_PLAN_RATING_1       = "SPC_SC_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_EN_PLAN_RATING_1       = "SPC_EN_Plan_Rating_1";
    protected static final String ROUTE_RATE_CARD_SPC_TAS_PLAN_RATING_1       = "SPC_TAS_Plan_Rating_1";
    protected static final String EMAIL_JOB_DEFAULT_CUT_OFF_TIME        = "Email job cut off time";
    protected static final String CUT_OFF_BILLING_PROCESS_ID            = "Cut Off Billing Process Id";
    protected static final String EMAIL_HOLIDAY_TABLE_NAME_META_FIELD   = "Holiday List";
    protected static final String PARAM_BILLING_PROCESS_ID              = "Billing Process Id";
    protected static final String PARAM_DISPATCH_EMAILS_AGAIN           = "Dispatch Emails Again";
    private final static String PARAM_EMAIL_HOLIDAY_TABLE_NAME             = "route_1_holiday_list";

    @Resource(name = "spcJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    protected static final List<EnumerationValueWS> ENUM_INTERNET_TECHNOLOGY_VALUES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS("ADSL"));
            add(new EnumerationValueWS("NBN"));
            add(new EnumerationValueWS("Fibre"));
            add(new EnumerationValueWS("Mobile"));

        }};
    protected static final List<EnumerationValueWS> ENUM_QUANTITY_RESOLUTION_UNIT_VALUES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS("Download"));
            add(new EnumerationValueWS("Upload"));
            add(new EnumerationValueWS("Total"));

        }};
    protected static final List<EnumerationValueWS> ENUM_TAX_SCHEME_VALUES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS("Regular GST"));
            add(new EnumerationValueWS("Tax Exempt"));
        }};
    protected static final List<EnumerationValueWS> ENUM_PLAN_RATING_VALUES = new ArrayList<EnumerationValueWS>()  {
        {
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_2));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_TF_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_OF_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_OMT_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_AGL_OM_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_AGL_SC_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_TF_OF_Plan_Rating_1_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_SC_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_EN_PLAN_RATING_1));
            add(new EnumerationValueWS(ROUTE_RATE_CARD_SPC_TAS_PLAN_RATING_1));
        }};
        
            private static final String CUSTOMER_CARE__TABLE_NAME  = "CC Table Name";
            private static final String CUSTOMER_CARE_TABLE_VALUE  = "route_1_calltozero";
            private static final String INSERT_QUERY_TEMPLATE;

            private static final Map<String, String> COLUMN_CONSTRAINST_MAP;
            private static final Map<String, String> COLUMN_DETAIL_MAP;
            private static List<String[]> CUSTOMER_CARE_RECORDS;
            static {
                COLUMN_CONSTRAINST_MAP = new LinkedHashMap<>();
                COLUMN_DETAIL_MAP      = new LinkedHashMap<>();

                COLUMN_CONSTRAINST_MAP.put("id", "SERIAL NOT NUll");
                COLUMN_DETAIL_MAP.put("calltozero", "VARCHAR(255)");
                COLUMN_CONSTRAINST_MAP.putAll(COLUMN_DETAIL_MAP);
                COLUMN_CONSTRAINST_MAP.put("PRIMARY KEY", " ( id ) ");

                INSERT_QUERY_TEMPLATE = new StringBuilder().append("INSERT INTO ")
                        .append(CUSTOMER_CARE_TABLE_VALUE)
                        .append(" ")
                        .append('(')
                        .append(COLUMN_DETAIL_MAP.entrySet().stream().map(Entry::getKey).collect(Collectors.joining(",")))
                        .append(')')
                        .append(" VALUES (")
                        .append(COLUMN_DETAIL_MAP.entrySet().stream().map(entry -> "?").collect(Collectors.joining(",")))
                        .append(" )")
                        .toString();


                String[] record1 = {"02244747100"};
                String[] record2 = {"02131464"};
                String[] record3 = {"021300790585"};
                String[] record4 = {"021800017461"};
                CUSTOMER_CARE_RECORDS = Arrays.asList(record1, record2, record3, record4);
            }
            //@formatter:on

    protected TestBuilder testBuilder;
    protected JbillingAPI api;
    protected Integer spcRatingUnitId;
    protected Map<String, Integer> spcDataUnitId = new HashMap<>();

    public static final String COMPANY_LEVEL_MF_NAME_FOR_BPAY_BILLER_CODE = "BPay Biller Code";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_​PLAN_BASED_FREE_CALL_INFO_TABLE_NAME = "​plan based free call info table name";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_CREDIT_PRODUCTS_CATEGORY_ID = "Credit Products Category Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_DEBIT_PRODUCTS_CATEGORY_ID = "Debit Products Category Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_TOTAL_BYTES = "Item Id For Charging Total Bytes";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_TAX_DATE_FORMAT = "Tax Date Format";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_DOWNLOAD_BYTES = "Item Id For Charging Download Bytes";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_TAX_TABLE_NAME = "Tax Table Name";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_PRODUCT_CATEGORY_ID_OF_INTERNET_USAGE_ITEMS = "Product Category Id Of Internet Usage Items";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_VOIP_USAGE_PRODUCT_CATEGORY_ID = "VOIP Usage Product Category Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_UPLOAD_BYTES = "Item Id For Charging Upload Bytes";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID = "Account Charges Product Category Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID = "Other Charges And Credits Product Category Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY_ID = "Exclude from Call Itemisation Category Id";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_OPTUS_PLAN_CODES = "Optus Plan Codes";
    public static final String COMPANY_LEVEL_MF_VALUE_OPTUS_PLAN_CODES = "SPCMO, ALGMO";
    public static final String COMPANY_LEVEL_MF_NAME_FOR_BILLING_ADDRESS_INFO_GROUP_NAME = "Billing Address Info Group Name";
    public static final String COMPANY_LEVEL_MF_VALUE_BILLING_ADDRESS = "Billing Address";
    public static final String COMPANY_LEVEL_MF_VALUE_CUSTOMER_DETAILS = "Customer Details";

    private static final String RATE_CARD_HEADER = "id,name,surcharge,initial_increment,subsequent_increment,charge,capped_charge,capped_increment,minimum_charge,markup,use_markup,tariff_code,"
            + "country_name,route_id,inc_surcharge,inc_charge,inc_capped_charge,inc_minimum_charge,inc_markup";
    // private Integer rateCardId;
    private Integer planRatingEnumId;

    public static final String SPC_CCC = "CCC";
    public static final String SPC_AAPT_INT_TOTAL = "AAPT-INT-TOTAL";
    public static final String SPC_SE_INT_TOTAL = "SE-INT-TOTAL";
    public static final String INTERNET_CREDIT_PRODUCT = "Internet_credit_Product";
    public static final String VOIP_CREDIT_PRODUCT = "Voip_credit_Product";
    public static final String MOBILE_CREDIT_PRODUCT = "Mobile_credit_Product";
    public static final String SPC_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY = "Exclude from Call Itemisation Category";

    protected static final String DATA_BOOST_PRODUCT_CODE_DBC_10_OPTUS = "DBC-10 (Optus)";
    protected static final String DATA_BOOST_PRODUCT_CODE_DBC_10_TELSTRA = "DBC-10 (Telstra)";

    public static final String MF_NAME_SERVICE_ID = "ServiceId";
    public static final String MF_NAME_SUBSCRIPTION_ORDER_ID = "Subscription Order Id";
    public static final String MF_NAME_CREDIT_POOLS_TARIFF_CODE = "credit pool name";
    private static final String PLAN_TYPE_OPTUS = "Optus";
    private static final String PLAN_TYPE_TELSTRA = "Telstra";
    private static final String SERVICE_TYPE_MOBILE = "Mobile";
    protected static Integer OM_MOBILE_DATA_ITEM_ID = null;

    private Integer planId;
    protected Integer optusMobileRateCardId;
    protected Integer telstraMobileRouteRateCardId;
    protected Integer telstraMobileRouteRateCardId2;
    protected Integer telstraFixedRouteRateCardId;
    protected Integer optusFixedRouteRateCardId;
    protected Integer optusMobileTierRouteRateCardId;
    protected Integer aglMobileRouteRateCardId;
    protected Integer aglSCRouteRateCardId;
    protected Integer spcTfOfPlanRating11RouteRateCardId;
    protected Integer spcSCPlanRating1RouteRateCardId;
    protected Integer spcENPlanRating1RouteRateCardId;
    protected Integer spcTASPlanRating1RouteRateCardId;
    protected Integer cutOffMetaFieldId;
    protected Integer cutOffBillingProcessMetaFieldId;
    
    protected TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
        });
    }

    @BeforeTest(alwaysRun = true)
    public void initializeTests() throws Exception {
        super.springTestContextPrepareTestInstance();
        logger.debug("SPCBaseConfiguration.initializeTests");
        testBuilder = getTestEnvironment();
        boolean shouldExecute = true;
        updateCurrency(CURRENCY_AUD,"1.0000",null);
        if (shouldExecute){
        testBuilder.given(envBuilder -> {

            updateCurrency(CURRENCY_AUD,"1.0000",null);

            buildAndPersistEnumeration (envBuilder,ENUMERATION_DIRECT_MARKETING, DIRECT_MARKETING);
            buildAndPersistEnumeration (envBuilder,ENUMERATION_STATES, ENUMERATION_STATE );
            buildAndPersistEnumeration (envBuilder,ENUMERATION_CUSTOMER_TYPE_VALUES, ENUMERATION_CUSTOMER_TYPE );
            buildAndPersistEnumeration (envBuilder, ENUM_QUANTITY_RESOLUTION_UNIT_VALUES, PLAN_METAFIELD_QUANTITY_RESOLUTION_UNIT_NAME);
            buildAndPersistEnumeration (envBuilder, ENUM_INTERNET_TECHNOLOGY_VALUES, PLAN_METAFIELD_INTERNET_TECHNOLOGY_NAME);
            buildAndPersistEnumeration (envBuilder, ENUM_TAX_SCHEME_VALUES, PLAN_METAFIELD_TAX_SCHEME_NAME);
            buildAndPersistEnumeration (envBuilder, ENUM_PLAN_RATING_VALUES, PLAN_METAFIELD_PLAN_RATING_NAME);

            // Creating account type
            bpayPaymentMethodId = buildAndPersistBPAYPaymentMethod ( envBuilder );
            apostPaymentMethodId = buildAndPersistAustraliaPostPaymentMethod ( envBuilder );
            bankDebitPaymentMethodId = buildAndPersistBankDebitPaymentMethod ( envBuilder );
            buildAndPersistMetafield(testBuilder, ENUMERATION_CUSTOMER_TYPE, DataType.ENUMERATION, EntityType.CUSTOMER);
            buildAndPersistMetafield(testBuilder, CRM_ACCOUNT_NUMBER, DataType.STRING, EntityType.CUSTOMER);

            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID,bpayPaymentMethodId,apostPaymentMethodId, bankDebitPaymentMethodId);

            buildAndPersistTaxSchemeDataTableRecord("Regular GST", "RG_10%", "20-11-2018", "30-12-2099", "10");
            buildAndPersistTaxSchemeDataTableRecord("Tax Exempt", "TE_0%", "20-11-2018", "30-12-2099", "0");
            buildAndPersistPlanBasedFreeCallInfoDataTableRecord("MNFV-28", "200", "sc_local_calls,sc_national_calls,sc_international_calls");
            buildAndPersistPlanBasedFreeCallInfoDataTableRecord("MNFV-29", "200", "sc_local_calls,sc_national_calls,sc_international_calls");
            buildAndPersistPlanBasedFreeCallInfoDataTableRecord("MNFV-30", "200", "sc_local_calls,sc_national_calls,sc_international_calls");

            buildAndPersistMetafield(testBuilder, EMAIL_HOLIDAY_TABLE_NAME_META_FIELD, DataType.STRING, EntityType.COMPANY);
            cutOffMetaFieldId = buildAndPersistMetafield(testBuilder, EMAIL_JOB_DEFAULT_CUT_OFF_TIME, DataType.STRING, EntityType.COMPANY);
            logger.debug("cutOffMetaFieldId ::{}",cutOffMetaFieldId);
            MetaFieldWS value = api.getMetaField(cutOffMetaFieldId);

            MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setStringValue(getCutOffTime(10));
            metaFieldValueWS.getMetaField().setDataType(value.getDataType());
            metaFieldValueWS.setFieldName(EMAIL_JOB_DEFAULT_CUT_OFF_TIME);

            value.setDefaultValue(metaFieldValueWS);
            value.setEntityType(EntityType.COMPANY);
            api.updateMetaField(value);

            cutOffBillingProcessMetaFieldId = buildAndPersistMetafield(testBuilder, CUT_OFF_BILLING_PROCESS_ID, DataType.STRING, EntityType.COMPANY);
            logger.debug("cutOffBillingProcessId ::{}",cutOffBillingProcessMetaFieldId);
            value = api.getMetaField(cutOffBillingProcessMetaFieldId);

            metaFieldValueWS = new MetaFieldValueWS();
            metaFieldValueWS.setStringValue("0");
            metaFieldValueWS.getMetaField().setDataType(value.getDataType());
            metaFieldValueWS.setFieldName(CUT_OFF_BILLING_PROCESS_ID);

            value.setDefaultValue(metaFieldValueWS);
            value.setEntityType(EntityType.COMPANY);
            api.updateMetaField(value);
            
            // Creating SPC rating unit
            buildAndPersistRatingUnit();
            //@formatter:off
            List<String> rateRecords1 = 
                    Arrays.asList("1,om_voicemail,0.00,60,60,0.1363636364,0.00,0.00,0.00,0.00,false,OM:MVD,Group Voicemail,10:S4MMM:1:B,0.00,0.15,0.00,0.00,0.00",
                            "2,om_mobile_to_fixed_calls,0.3636363636,60,60,0.9000000000,0.00,0.00,0.00,0.00,false,OM:MN,Mobile to Fixed Calls,10:S4NMR:1:6:029,0.40,0.99,0.00,0.00,0.00",
                            "3,om_mobile_to_mobile_calls,0.3636363636,60,60,0.9000000000,0.00,0.00,0.00,0.00,false,OM:MB,Mobile to Mobile (OTHER Network),10:T00015:3:1:04,0.40,0.99,0.00,0.00,0.00",
                            "4,om_mobile_to_international,0.3636363636,60,60,0.9000000000,0.00,0.00,0.00,0.00,false,OM:NEZ,New Zealand,10:IDDSP:1:6:NEW ZEALAND,0.40,0.99,0.00,0.00,0.00",
                            "5,om_sms,0.2272727273,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MS,Mobile Originated SMS (Domestic) Off-Net,30:SP094:1:0:010,0.25,0.00,0.00,0.00,0.00",
                            "6,om_mobile_data,0.00,1,1,0.0454545455,0.00,0.00,0.00,0.00,false,OM:GPRS,Mobile Data,50:TD050CON:00:2,0.00,0.00,0.00,0.00,0.00",
                            "7,om_mobile_special_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OM:MDA,Directory Assistance,10:1:OPER.INQUIRY:SP12M:6,0.00,0.00,0.00,0.00,10.00",
                            "8,om_roaming,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OM:ROAMD,Mobile Roaming Data,20::MOC::0:999,0.00,0.00,0.00,0.00,10.00",
                            "9,om_mms,0.7000000000,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MMS,MMS Mobile to Mobile (Domestic),50:MMS1SP020:00:0:MMS,0.77,0.00,0.00,0.00,0.00",
                            "10,om_mobile_to_fixed_calls,0.3636363636,60,60,0.9000000000,0.00,0.00,0.00,0.00,false,OM:MN,Mobile to Fixed Calls,10:S4NMR:1:1,0.40,0.99,0.00,0.00,0.00");

            // Create rate cards
            optusMobileRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords1);
            // building Rate Card pricing strategy
            PriceModelWS optusMobilePriceModel = buildRateCardPriceModel(optusMobileRateCardId, "DURATION");

            List<String> rateRecords2 =
                    Arrays.asList("1,tm_voicemail,0.50,1,1,0.40,0.00,0.00,0.00,0.00,false,TM:MVR,Calls direct to Voice Mail (101),GVZN000226:1411:GTEL:1,0.00,0.00,0.00,0.00,0.00",
                            "2,tm_mobile_to_fixed_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MN,SPC Telstra to Fixed Line,AUSFL:0156:GTEL:1,0.00,0.00,0.00,0.00,0.00",
                            "3,tm_mobile_to_international,0.3636363636,60,60,1.9545454545,0.00,0.00,0.00,0.00,false,TM:AFG,Afghanistan,AFGAL::GTEL:1,0.40,2.15,0.00,0.00,0.00",
                            "4,tm_sms,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MS,SMS,TEL01:0572:GSMS:1,0.00,0.00,0.00,0.00,0.00",
                            "5,tm_mobile_data,0.00,1,1,0.0454545455,0.00,0.00,0.00,0.00,false,TM:GPRS,Mobile Data,50:D3DP1INT:00:2,0.00,0.00,0.00,0.00,0.00",
                            "6,tm_mobile_special_calls,0.00,1,1,0.00,0.00,0.00,0.00,10.0000000000,true,TM:CIF800,Intl Freephone,CIF800::GTEL:1,0.00,0.00,0.00,0.00,21.00",
                            "7,tm_voicemail,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MVR,Calls direct to Voice Mail (101),GSZN000226:1411:GTEL:1,0.00,0.00,0.00,0.00,0.00",
                            "8,tm_mobile_data,0.00,1,1,0.0454545455,0.00,0.00,0.00,0.00,false,TM:GPRS,Mobile Data,W0061::GPRS:11,0.00,0.00,0.00,0.00,0.00",
                            "9,tm_mms,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MMS,MMS Mobile to Mobile,INDAM::GMMS:1,0.00,0.00,0.00,0.00,0.00");

            telstraMobileRouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords2);

            List<String> rateRecords2_1 =
                    Arrays.asList("1,tm_voicemail,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MVR,Calls direct to Voice Mail (101),GVZN000226:1411:GTEL:1,0.00,0.00,0.00,0.00,0.00",
                            "2,tm_mobile_to_fixed_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MN,SPC Telstra to Fixed Line,AUSFL:0156:GTEL:1,0.00,0.00,0.00,0.00,0.00",
                            "3,tm_mobile_to_international,0.3636363636,60,60,1.9545454545,0.00,0.00,0.00,0.00,false,TM:AFG,Afghanistan,AFGAL::GTEL:1,0.40,2.15,0.00,0.00,0.00",
                            "4,tm_sms,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MS,SMS,TEL01:0572:GSMS:1,0.00,0.00,0.00,0.00,0.00",
                            "5,tm_mobile_data,0.00,1,1,0.0454545455,0.00,0.00,0.00,0.00,false,TM:GPRS,Mobile Data,W0061::GPRS:11,0.00,0.00,0.00,0.00,0.00",
                            "6,tm_mobile_special_calls,0.00,1,1,0.00,0.00,0.00,0.00,10.0000000000,true,TM:CIF800,Intl Freephone,CIF800::GTEL:1,0.00,0.00,0.00,0.00,21.00",
                            "7,tm_mms,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MMS,MMS Mobile to Mobile,INDAM::GMMS:1,0.00,0.00,0.00,0.00,0.00",
                            "8,tm_voicemail,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,TM:MVR,Calls direct to Voice Mail (101),GSZN000226:1411:GTEL:1,0.00,0.00,0.00,0.00,0.00");

            telstraMobileRouteRateCardId2 = createRateCard(ROUTE_RATE_CARD_SPC_TM_PLAN_RATING_2, RATE_CARD_HEADER, rateRecords2_1);

            List<String> rateRecords3 =
                    Arrays.asList("1,tf_directory_&_assisted_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,TF:#DIR,ISDN 2 - Telephony National Operator,495130W0USAGE,0.00,0.00,0.00,0.00,10.00",
                            "2,tf_international_calls,0.3545454545,60,60,1.6818181818,0.00,0.00,0.00,0.00,false,TF:ANGL,Anguilla,U02035W1USAGE:9C,0.39,1.85,0.00,0.00,0.00",
                            "3,tf_other_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,TF:SERV,ISDN 2 Enhanced Complete- Tele Wake-up and Reminder,U01659W1USAGE,0.00,0.00,0.00,0.00,10.00",
                            "4,tf_international_calls,0.00,1,1,0.00,0.00,0.00,0.00,25.0000000000,true,TF:SAT,Satellite Call,U02033W1USAGE:5N,0.00,0.00,0.00,0.00,37.50",
                            "5,tf_service_&_equipment,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,TF:S&E,Telephone Handset Rental,62664000RENTAL,0.00,0.00,0.00,0.00,10.00");

            telstraFixedRouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_TF_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords3);

            List<String> rateRecords4 = 
                    Arrays.asList("1,op_directory_&_assisted_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OP:OPA,Operator Assistance,340,0.00,0.00,0.00,0.00,10.00",
                            "2,op_international_calls,0.3545454545,60,60,1.6818181818,0.00,0.00,0.00,0.00,false,OP:ANU,ANGUILLA,22,0.39,1.85,0.00,0.00,0.00",
                            "3,op_local_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OP:LTC,Local call,343,0.00,0.00,0.00,0.00,0.00",
                            "4,op_fixed_to_mobile_calls,0.3545454545,60,60,0.3363636364,1.3545454545,600,0.00,0.00,false,OP:F2M,Mobile Call,502,0.39,0.37,1.49,0.00,0.00",
                            "5,op_long_distance_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OP:NATCOM,National Call < 70 Km,1,0.00,0.00,0.00,0.00,0.00",
                            "6,op_calls_to_13_numbers,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,OP:C13,Calls to 13/1300,000344:B1,0.40,0.00,0.00,0.00,0.00");

            optusFixedRouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_OF_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords4);

            List<String> rateRecords5 = 
                    Arrays.asList("1,om_voicemail,0.00,30,30,0.1363636364,0.00,0.00,0.00,0.00,false,OM:MVD,Group Voicemail,10:S4MMM:1:B,0.00,0.15,0.00,0.00,0.00",
                            "2,om_mobile_to_fixed_calls,0.3545454545,30,30,0.0090909091,0.00,0.00,0.00,0.00,false,OM:MN,Mobile to Fixed Calls,10:S4NMR:1:6:029,0.39,0.01,0.00,0.00,0.00",
                            "3,om_mobile_to_mobile_calls,0.3545454545,30,30,0.0090909091,0.00,0.00,0.00,0.00,false,OM:MB,Mobile to Mobile (OTHER Network),10:T00015:3:1:04,0.39,0.01,0.00,0.00,0.00",
                            "4,om_mobile_to_international,0.3545454545,30,30,2.0636363636,0.00,0.00,0.00,0.00,false,OM:AFG,Afghanistan,10:IDDSP:1:6:AFGHANISTAN,0.39,2.27,0.00,0.00,0.00",
                            "5,om_sms,0.2272727273,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MS,Mobile Originated SMS (Domestic) Off-Net,30:SP094:1:0:010,0.25,0.00,0.00,0.00,0.00",
                            "6,om_mobile_data,0.00,1,1,0.4545454545,0.00,0.00,0.2272727273,0.00,false,OM:GPRS,Mobile Data,50:TD050CON:00:2,0.00,0.00,0.00,0.25,0.00",
                            "7,om_mobile_special_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OM:MDA,Directory Assistance,10:1:OPER.INQUIRY:SP12M:6,0.00,0.00,0.00,0.00,10.00",
                            "8,om_roaming,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OM:ROAMD,Mobile Roaming Data,20::MOC::0:999,0.00,0.00,0.00,0.00,10.00",
                            "9,om_mms,0.7000000000,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MMS,MMS Mobile to Mobile (Domestic),50:MMS1SP020:00:0:MMS,0.77,0.00,0.00,0.00,0.00");

            optusMobileTierRouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_OMT_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords5);

            List<String> rateRecords6 = 
                    Arrays.asList("1,om_voicemail,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MVD,Group Voicemail,10:S4MMM:1:B,0.00,0.00,0.00,0.00,0.00",
                            "2,om_mobile_to_fixed_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MN,Mobile to Fixed Calls,10:S4NMR:1:6:029,0.00,0.00,0.00,0.00,0.00",
                            "3,om_mobile_to_mobile_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MB,Mobile to Mobile (OTHER Network),10:T00015:3:1:04,0.00,0.00,0.00,0.00,0.00",
                            "4,om_mobile_to_international,0.3636363636,60,60,2.0636363636,0.00,0.00,0.00,0.00,false,OM:AFG,Afghanistan,10:IDDSP:1:6:AFGHANISTAN,0.40,2.27,0.00,0.00,0.00",
                            "5,om_sms,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MS,Mobile Originated SMS (Domestic) Off-Net,30:SP094:1:0:010,0.00,0.00,0.00,0.00,0.00",
                            "6,om_mobile_data,0.00,1,1,0.0088818182,0.00,0.00,0.00,0.00,false,OM:GPRS,Mobile Data,50:MD4GBINT:00:2:LTEFB,0.00,0.009766,0.00,0.00,0.00",
                            "7,om_international_mms,0.7000000000,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MMSINT,MMS Mobile to Mobile (International),50:MMS1SP094:00:0:MMS:I,0.77,0.00,0.00,0.00,0.00",
                            "8,om_international_sms,0.5000000000,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:SMSINT,SMS to International,30:MMSMS:1:24223:011,0.55,0.00,0.00,0.00,0.00",
                            "9,om_mobile_special_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OM:MDA,Directory Assistance,10:1:OPER.INQUIRY:SP12M:6,0.00,0.00,0.00,0.00,10.00",
                            "10,om_roaming,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OM:ROAMD,Mobile Roaming Data,20::MOC::0:999,0.00,0.00,0.00,0.00,10.00",
                            "11,om_mms,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OM:MMS,MMS Mobile to Mobile (Domestic),50:MMS1SP020:00:0:MMS,0.00,0.00,0.00,0.00,0.00");

            aglMobileRouteRateCardId = createRateCard(ROUTE_RATE_CARD_AGL_OM_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords6);

            List<String> rateRecords7 = 
                    Arrays.asList("1,sc_directory_&_assisted_calls,0.5909090909,1,1,0.00,0.00,0.00,0.00,0.00,false,SC:1223,Directory Assistance,sc:1223,0.65,0.00,0.00,0.00,0.00",
                            "2,sc_local_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,SC:FREE,Free Call,sc:6,0.00,0.00,0.00,0.00,0.00",
                            "3,sc_calls_to_mobiles,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,SC:F2M,Calls to Mobiles,sc:5,0.00,0.00,0.00,0.00,0.00",
                            "4,sc_calls_to_13_numbers,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,SC:1300,13/1300 Call,sc:11,0.40,0.00,0.00,0.00,0.00",
                            "5,sc_international_calls,0.3636363636,60,60,0.9727272727,0.00,0.00,0.00,0.00,false,SC:AFG,Afghanistan,sc:3:Afghanistan,0.4,1.07,0.00,0.00,0.00");

            aglSCRouteRateCardId = createRateCard(ROUTE_RATE_CARD_AGL_SC_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords7);

            List<String> rateRecords8 = 
                    Arrays.asList("1,tf_directory_&_assisted_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,TF:#DIR,ISDN 2 - Telephony National Operator,495130W0USAGE,0.00,0.00,0.00,0.00,10.00",
                            "2,tf_international_calls,0.3545454545,60,60,1.6818181818,0.00,0.00,0.00,0.00,false,TF:ANGL,Anguilla,U02035W1USAGE:9C,0.39,1.85,0.00,0.00,0.00",
                            "3,tf_other_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,TF:SERV,ISDN 2 Enhanced Complete- Tele Wake-up and Reminder,U01659W1USAGE,0.00,0.00,0.00,0.00,10.00",
                            "4,tf_service_&_equipment,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,TF:S&E,Telephone Handset Rental,62664000RENTAL,0.00,0.00,0.00,0.00,10.00",
                            "5,op_directory_&_assisted_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,true,OP:OPA,Operator Assistance,340,0.00,0.00,0.00,0.00,10.00",
                            "6,op_international_calls,0.3545454545,60,60,1.6818181818,0.00,0.00,0.00,0.00,false,OP:ANU,ANGUILLA,22,0.39,1.85,0.00,0.00,0.00",
                            "7,op_local_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OP:LTC,Local call,343,0.00,0.00,0.00,0.00,0.00",
                            "8,op_fixed_to_mobile_calls,0.3545454545,60,60,0.3363636364,1.3545454545,600,0.00,0.00,false,OP:F2M,Mobile Call,502,0.39,0.37,1.49,0.00,0.00",
                            "9,op_long_distance_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,OP:NATCOM,National Call < 70 Km,1,0.00,0.00,0.00,0.00,0.00",
                            "10,op_calls_to_13_numbers,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,OP:C13,Calls to 13/1300,000344:B1,0.40,0.00,0.00,0.00,0.00");

            spcTfOfPlanRating11RouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_TF_OF_Plan_Rating_1_1, RATE_CARD_HEADER, rateRecords8);

            List<String> rateRecords9 = 
                    Arrays.asList("1,sc_local_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,SC:FREE,Free Call,sc:6,0.00,0.00,0.00,0.00,0.00",
                            "2,sc_calls_to_mobiles,0.3545454545,60,60,0.3363636364,1.3545454545,600,0.00,0.00,false,SC:F2M,Calls to Mobiles,sc:5,0.39,0.37,1.49,0.00,0.00",
                            "3,sc_calls_to_13_numbers,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,SC:1300,13/1300 Call,sc:11,0.40,0.00,0.00,0.00,0.00",
                            "4,sc_international_calls,0.3545454545,60,60,0.6727272727,0.00,0.00,0.00,0.00,false,SC:ALBM,Albania Mobile,sc:3:Albania-Mobile-68,0.39,0.74,0.00,0.00,0.00");

            spcSCPlanRating1RouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_SC_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords9);

            List<String> rateRecords10 = 
                    Arrays.asList("1,en_local_calls,0.00,1,1,0.00,0.00,0.00,0.00,0.00,false,EN:FREE,Free Call,en:6,0.00,0.00,0.00,0.00,0.00",
                            "2,en_calls_to_mobiles,0.3545454545,60,60,0.3363636364,1.3545454545,600,0.00,0.00,false,EN:F2M,Calls to Mobiles,en:5,0.39,0.37,1.49,0.00,0.00",
                            "3,en_national_calls,0.3545454545,60,60,0.2272727273,1.8000000000,3600,0.00,0.00,false,EN:LD,National Call,en:2,0.39,0.25,1.98,0.00,0.00",
                            "4,en_calls_to_13_numbers,0.3636363636,1,1,0.00,0.00,0.00,0.00,0.00,false,EN:1300,13/1300 Call,en:11,0.40,0.00,0.00,0.00,0.00",
                            "5,en_international_calls,0.3545454545,60,60,0.6727272727,0.00,0.00,0.00,0.00,false,EN:ALBM,Albania Mobile,en:3:Albania-Mobile-68,0.39,0.74,0.00,0.00,0.00");

            spcENPlanRating1RouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_EN_PLAN_RATING_1, RATE_CARD_HEADER, rateRecords10);

            List<String> rateRecords11 = 
                    Arrays.asList("1,op_inbound_local_to_1300,0.00,1,1,0.1000000000,0.00,0.00,0.00,0.00,false,OP:13LOC,1300 Local,INB:000048,0.00,0.11,0.00,0.00,0.00",
                            "2,op_inbound_national_to_1300,0.00,1,1,0.1500000000,0.00,0.00,0.00,0.00,false,OP:13NAT,1300 National,INB:000046,0.00,0.165,0.00,0.00,0.00",
                            "3,op_inbound_mobile_to_1300,0.00,1,1,0.2000000000,0.00,0.00,0.00,0.00,false,OP:13M2F,1300 Mobile Originated & Not Terminated,INB:000047:501,0.00,0.22,0.00,0.00,0.00",
                            "4,op_inbound_local_to_1800,0.00,1,1,0.1000000000,0.00,0.00,0.00,0.00,false,OP:18LOC,1800 Local,INB:000054,0.00,0.11,0.00,0.00,0.00",
                            "5,op_inbound_community_calls,0.00,1,1,0.1500000000,0.00,0.00,0.00,0.00,false,OP:13COM,1300 Community,INB:000068,0.00,0.165,0.00,0.00,0.00",
                            "6,op_inbound_national_to_1800,0.00,1,1,0.1500000000,0.00,0.00,0.00,0.00,false,OP:18INAT,1800 National,INB:000051,0.00,0.165,0.00,0.00,0.00",
                            "7,op_inbound_mobile_to_1800,0.00,1,1,0.2000000000,0.00,0.00,0.00,0.00,false,OP:18M2F,1800 Mobile Originated & Not Terminated,INB:000055:501,0.00,0.22,0.00,0.00,0.00");
            //@formatter:on
                    spcTASPlanRating1RouteRateCardId = createRateCard(ROUTE_RATE_CARD_SPC_TAS_PLAN_RATING_1, RATE_CARD_HEADER,
                            rateRecords11);

                    // creating data table with name 'route_1_taxes'
                    createTable(CUSTOMER_CARE_TABLE_VALUE, COLUMN_CONSTRAINST_MAP);
                    CUSTOMER_CARE_RECORDS.stream().forEach(this::insertCustomerCareNumbers);
                    String priceUnitName = "byte";
                    String incrementUnitName = "GB";
                    String incrementUnitQuantity = "1073741824";
                    buildAndPersistDataRatingUnit(DATA_RATING_UNIT_NAME, priceUnitName, incrementUnitName, incrementUnitQuantity);

                    priceUnitName = "byte";
                    incrementUnitName = "MB";
                    incrementUnitQuantity = "1048576";
                    buildAndPersistDataRatingUnit(UNIT_NAME_BYTE_TO_MB_COVERTER, priceUnitName, incrementUnitName, incrementUnitQuantity);

                    priceUnitName = "KB";
                    incrementUnitName = "MB";
                    incrementUnitQuantity = "1024";
                    buildAndPersistDataRatingUnit(UNIT_NAME_KB_TO_MB_CONVERTER, priceUnitName, incrementUnitName, incrementUnitQuantity);

                    RatingConfigurationWS ratingConfigurationKBtoMB = new RatingConfigurationWS(
                        api.getRatingUnit(spcDataUnitId.get(UNIT_NAME_KB_TO_MB_CONVERTER)), null);

                    // Creating mediated usage category
                    buildAndPersistCategory(envBuilder, api, SPC_MEDIATED_USAGE_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

                    // Creating services categories
                    buildAndPersistCategory(envBuilder, api, INTERNET_SERVICES_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, MOBILE_SERVICES_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, VOICE_SERVICES_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,
                            0, true);

                    // Creating adjustment categories
                    buildAndPersistCategory(envBuilder, api, CREDIT_PRODUCTS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT, 0, true);
                    buildAndPersistCategory(envBuilder, api, DEBIT_PRODUCTS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT, 0, true);
                    buildAndPersistCategory(envBuilder, api, CREDIT_DEBIT_ADJUSTMENTS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT, 0, true);
                    buildAndPersistCategory(envBuilder, api, OTHER_CHARGES_AND_CREDITS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ADJUSTMENT, 0, true);
                    buildAndPersistCategory(envBuilder, api, CREDIT_ADJUSTMENT_CATEGORY_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);

                    // Creating usage categories
                    buildAndPersistCategory(envBuilder, api, INTERNET_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,
                            0, true);
                    buildAndPersistCategory(envBuilder, api, MOBILE_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,
                            0, true);
                    buildAndPersistCategory(envBuilder, api, VOICE_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0,
                            true);

                    buildAndPersistCategory(envBuilder, api, INBOUND_SERVICES_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, INBOUND_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,
                            0, true);

                    // Creating numbers categories
                    Integer voipNumbersCategoryId = buildAndPersistCategory(envBuilder, api, VOIP_NUMBERS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 1, true);
                    // Add Statuses
                    /*
                     * Set<AssetStatusDTOEx> statuses = new
                     * HashSet<AssetStatusDTOEx>();
                     * statuses.add(createAssetStatus("Pending", false, true,
                     * true, false, true, false, false));
                     * statuses.add(createAssetStatus("Available", true, true,
                     * false, false, false, false, false));
                     * statuses.add(createAssetStatus("Assigned", false, false,
                     * true, true, false, false, false));
                     * 
                     * ItemTypeWS itemTypeWS =
                     * api.getItemCategoryById(voipNumbersCategoryId);
                     * itemTypeWS.setAssetStatuses(statuses);
                     * itemTypeWS.setAssetMetaFields(createAssetMetaField());
                     * api.updateItemCategory(itemTypeWS);
                     */
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_TAX_SCHEME_NAME, DataType.ENUMERATION, EntityType.PRODUCT);
                    buildAndPersistCategoryWithAssetMetaFields(envBuilder, api, MOBILE_NUMBERS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 1, true, createAssetMetaField());
                    buildAndPersistCategory(envBuilder, api, VOICE_NUMBERS_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,
                            1, true);
                    buildAndPersistCategory(envBuilder, api, CREDIT_POOL_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0,
                            true);
                    buildAndPersistCategory(envBuilder, api, EXCLUDE_PRICE_ZERO_ASSETS_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, ACCOUNT_CHARGES_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, UNEARNED_REVENUE_EXCLUDED_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, UNBILLED_REVENUE_EXCLUDED_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, MIGRATION_ADJUSTMENT_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, INTERNET_USER_NAMES_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 1, true);
                    buildAndPersistCategory(envBuilder, api, SPC_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);

                    buildAndPersistCategory(envBuilder, api, UNBILLED_REVENUE_INCLUDED_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, TELSTRA_4G_MOBILE_ASSET_CATEGORY, true,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 1, true);
                    buildAndPersistCategory(envBuilder, api, OPTUS_MOBILE_ASSET_CATEGORY, true,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 1, true);
                    buildAndPersistCategory(envBuilder, api, AAPT_VOIP_CATEGORY, true, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 1,
                            true);
                    buildAndPersistCategory(envBuilder, api, UNEARNED_REVENUE_INCLUDED_CATEGORY, false,
                            ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM, 0, true);
                    buildAndPersistCategory(envBuilder, api, CUSTOMER_CARE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM,
                            0, true);

                    buildAndPersistFlatProduct(envBuilder, api, INTERNET_ASSET_PLAN_ITEM_CODE, null, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "0.00", true, 1, false);

                    buildAndPersistFlatProduct(envBuilder, api, SPC_CUSTOMER_CARE_ITEM_CODE, null, false,
                            envBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY), "0.00", true, 0, false);

                    MetaFieldWS metaFieldWS1 = ApiBuilderHelper.getMetaFieldWS(MF_NAME_SERVICE_ID, DataType.STRING, EntityType.ORDER_LINE,
                            api.getCallerCompanyId());
                    metaFieldWS1.setMandatory(false);

                    buildAndPersistFlatProductWithOrderLineMetaField(envBuilder, api, USAGE_PRODUCT_CODE_MOBILE_NUMBERS, "mobile number",
                            false, envBuilder.idForCode(MOBILE_NUMBERS_CATEGORY), "0.00", true, 1, false, metaFieldWS1);

                    buildAndPersistFlatProduct(envBuilder, api, USAGE_PRODUCT_CODE_VOICE_NUMBERS, "voice numbers", false,
                            envBuilder.idForCode(VOICE_NUMBERS_CATEGORY), "0.00", true, 1, false);

                    List<Integer> categoryIds = new ArrayList<>();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_MOBILE_PREMIUM_SMS,
                            "Mobile Premium SMS", false, categoryIds, "0.00", true, 0, false);
                    // AA
                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_LONG_DISTANCE_CALLS,
                            "Long Distance Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_DIRECTORY_AND_ASSISTED_CALLS,
                            "Directory & Assisted Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_CALLS_TO_MOBILES,
                            "Calls to Mobiles", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_INTERNATIONAL_CALLS,
                            "International Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_NATIONAL_CALLS,
                            "National Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_CALLS_TO_13_NUMBERS,
                            "Calls to 13 Numbers", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_AA_LOCAL_CALLS, "Local Calls",
                            false, categoryIds, "0.00", true, 0, false);

                    // SC
                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_NATIONAL_CALLS,
                            "National Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_INTERNATIONAL_CALLS,
                            "International Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_INTERNATIONAL_CALLS_SP_DEST,
                            "International Calls Global 70", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_INTERNATIONAL_CALLS_SP_DEST_30,
                            "International Calls Global 30", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_CALLS_TO_MOBILES,
                            "Calls to Mobiles", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_LOCAL_CALLS, "Local Calls",
                            false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_CALLS_TO_13_NUMBERS,
                            "Calls to 13 Numbers", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_OTHER_CALLS, "Other Calls",
                            false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SC_DIRECTORY_AND_ASSISTED_CALLS,
                            "Directory & Assisted Calls", false, categoryIds, "0.00", true, 0, false);

                    // EN
                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_EN_CALLS_TO_MOBILES,
                            "Calls to Mobiles", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_EN_INTERNATIONAL_CALLS,
                            "International Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_EN_NATIONAL_CALLS,
                            "National Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_EN_LOCAL_CALLS, "Local Calls",
                            false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_EN_CALLS_TO_13_NUMBERS,
                            "Calls to 13 Numbers", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(VOIP_NUMBERS_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(EXCLUDE_PRICE_ZERO_ASSETS_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api,
                            USAGE_PRODUCT_CODE_VOIP_NUMBER_EXCLUDE_ZERO_PRICE_ASSET, "Home Phone", false, categoryIds, "0.00", true, 1,
                            false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_INTERNATIONAL_SMS,
                            "International SMS", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_INTERNATIONAL_MMS,
                            "International MMS", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(INTERNET_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(INTERNET_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_SCONNECT_TOTAL, "Internet", false,
                            categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_TM_MOBILE_TO_FIXED_CALLS,
                            "Mobile to Fixed Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(SPC_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY));
                    buildAndPersistFlatProductWithRating(envBuilder, api, USAGE_PRODUCT_CODE_TM_MOBILE_DATA, "Mobile Data", false,
                            categoryIds, "0.00", true, 0, false, com.sapienter.jbilling.server.util.Util.getEpochDate(),
                            ratingConfigurationKBtoMB);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_TM_MOBILE_TO_INTERNATIONAL,
                            "Mobile to International", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_TM_MOBILE_TO_MOBILE_CALLS,
                            "Mobile to Mobile Calls", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategoriesWithMetaField(envBuilder, api, USAGE_PRODUCT_CODE_TM_VOICEMAIL, "Voicemail", false,
                            categoryIds, "0.00", true, 0, false, PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue());

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_TM_MMS, "MMS", false, categoryIds,
                            "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    /*buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_TM_VOICEMAIL, "Voicemail", false,
                            categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));*/
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_TM_SMS, "SMS", false, categoryIds,
                            "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(INBOUND_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(INBOUND_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));

                buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OP_INBOUND_LOCAL_TO_1300,
                            "Inbound Local to 1300", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INBOUND_PRODUCT_CODE_OP_INBOUND_COMMUNITY_CALLS,
                            "Inbound Community Calls", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INBOUND_PRODUCT_CODE_OP_INBOUND_MOBILE_TO_1300,
                            "Inbound Mobile to 1300", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INBOUND_PRODUCT_CODE_OP_INBOUND_NATIONAL_TO_1800,
                            "Inbound National to 1800", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INBOUND_PRODUCT_CODE_OP_INBOUND_LOCAL_TO_1800,
                            "Inbound Local to 1800", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INBOUND_PRODUCT_CODE_OP_INBOUND_NATIONAL_TO_1300,
                            "Inbound National to 1300", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INBOUND_PRODUCT_CODE_OP_INBOUND_MOBILE_TO_1800,
                            "Inbound Mobile to 1800", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(CREDIT_PRODUCTS_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(OTHER_CHARGES_AND_CREDITS_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));

                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, CREDIT_ADJUSTMENT_PRODUCT_CODE_CR_PCT,
                            "Credit Adjustment", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(INTERNET_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(INTERNET_USER_NAMES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, INTERNET_USER_NAMES_PRODUCT, "Internet User Names",
                            false, categoryIds, "0.00", true, 1, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(CREDIT_POOL_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));

                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, CREDIT_ADJUSTMENT_PRODUCT_CODE_MC,
                            "Mobile Call Credit", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(CREDIT_POOL_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, CREDIT_ADJUSTMENT_PRODUCT_CODE_VC,
                            "Voice Call Credit", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(DEBIT_PRODUCTS_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(ACCOUNT_CHARGES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));

                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, DEBIT_ADJUSTMENT_PRODUCT_CODE_DR_PCT,
                            "Debit Adjustment", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, DEBIT_ADJUSTMENT_PRODUCT_CODE_DR_FLT,
                            "Debit Adjustment", false, categoryIds, "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(TELSTRA_4G_MOBILE_ASSET_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));

                    /*
                     * Failed commented temporary
                     * buildAndPersistFlatProductForMultipleCategories
                     * (envBuilder, api, TELSTRA_4G_ASSET_PRODUCT,
                     * "Telstra 4G Asset Product", false, categoryIds, "0.00",
                     * true, 1, false);
                     */

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(OPTUS_MOBILE_ASSET_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));

                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, OPTUS_MOBILE_ASSET_PRODUCT,
                            "Optus Mobile Asset Product", false, categoryIds, "0.00", true, 1, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(AAPT_VOIP_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(VOICE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_EXCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_EXCLUDED_CATEGORY));

                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, AAPT_VOIP_ASSET_PRODUCT, "AAPT VOIP Asset Product",
                            false, categoryIds, "0.00", true, 1, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(SPC_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    OM_MOBILE_DATA_ITEM_ID = buildAndPersistFlatProductWithRating(envBuilder, api, USAGE_PRODUCT_CODE_OM_MOBILE_DATA, "Mobile Data", false,
                            categoryIds, "0.00", true, 0, false, com.sapienter.jbilling.server.util.Util.getEpochDate(),
                            ratingConfigurationKBtoMB);

                    RatingConfigurationWS ratingConfigurationByteoMB = new RatingConfigurationWS(api.getRatingUnit(spcDataUnitId
                            .get(UNIT_NAME_BYTE_TO_MB_COVERTER)), null);
                    buildAndPersistFlatProductWithRating(envBuilder, api, USAGE_PRODUCT_CODE_OM_MUR_MOBILE_DATA, "MUR Mobile Data", false,
                            categoryIds, "0.00", true, 0, false, com.sapienter.jbilling.server.util.Util.getEpochDate(),
                            ratingConfigurationByteoMB);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_MOBILE_TO_INTERNATIONAL,
                            "Mobile to International", false, categoryIds, "0.00", true, 0, false);

                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_VOICEMAIL, "Voicemail", false,
                            categoryIds, "0.00", true, 0, false);
                    
                    buildAndPersistFlatProductForMultipleCategoriesWithMetaField(envBuilder, api, USAGE_PRODUCT_CODE_OM_MOBILE_TO_FIXED_CALLS,
                            "Mobile to Fixed Calls", false, categoryIds, "0.00", true, 0, false, PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue());
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_MMS, "MMS", false, categoryIds,
                            "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_MOBILE_TO_MOBILE_CALLS,
                            "Mobile to Mobile Calls", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_ROAMING, "Roaming", false,
                            categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_MOBILE_SPECIAL_CALLS,
                            "Mobile Special Calls", false, categoryIds, "0.00", true, 0, false);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, USAGE_PRODUCT_CODE_OM_SMS, "SMS", false, categoryIds,
                            "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(CUSTOMER_CARE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNEARNED_REVENUE_INCLUDED_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, SPC_CCC, "Customer Care Calls", false, categoryIds,
                            "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(INTERNET_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(INTERNET_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, SPC_AAPT_INT_TOTAL, "Internet", false, categoryIds,
                            "0.00", true, 0, false);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(INTERNET_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(INTERNET_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, SPC_SE_INT_TOTAL, "Internet", false, categoryIds,
                            "0.00", true, 0, false);
                    buildAndPersistFlatProduct(envBuilder, api, INTERNET_CREDIT_PRODUCT, "Internet In Advance Plan Credit", false,
                            envBuilder.idForCode(CREDIT_ADJUSTMENT_CATEGORY_CATEGORY), "0.00", true, 0, false);
                    buildAndPersistFlatProduct(envBuilder, api, VOIP_CREDIT_PRODUCT, "Voip In Advance Plan Credit", false,
                            envBuilder.idForCode(CREDIT_ADJUSTMENT_CATEGORY_CATEGORY), "0.00", true, 0, false);
                    buildAndPersistFlatProduct(envBuilder, api, MOBILE_CREDIT_PRODUCT, "Mobile In Advance Plan Credit", false,
                            envBuilder.idForCode(CREDIT_ADJUSTMENT_CATEGORY_CATEGORY), "0.00", true, 0, false);

                    if (!isMetaFieldPresent(EntityType.COMPANY, SPC_CUSTOMER_CARE_ITEM_ID)) {
                        buildAndPersistMetafield(testBuilder, SPC_CUSTOMER_CARE_ITEM_ID, DataType.STRING, EntityType.COMPANY);
                    }

                    if (!isMetaFieldPresent(EntityType.ORDER_LINE, MF_NAME_SERVICE_ID)) {
                        buildAndPersistMetafield(testBuilder, MF_NAME_SERVICE_ID, DataType.STRING, EntityType.ORDER_LINE);
                    }

                    if (!isMetaFieldPresent(EntityType.ASSET, MF_NAME_SERVICE_ID)) {
                        buildAndPersistMetafield(testBuilder, MF_NAME_SERVICE_ID, DataType.STRING, EntityType.ASSET);
                    }

                    if (!isMetaFieldPresent(EntityType.ORDER, MF_NAME_SUBSCRIPTION_ORDER_ID)) {
                        buildAndPersistMetafield(testBuilder, MF_NAME_SUBSCRIPTION_ORDER_ID, DataType.INTEGER, EntityType.ORDER);
                    }

                    if (!isMetaFieldPresent(EntityType.ORDER, MF_NAME_CREDIT_POOLS_TARIFF_CODE)) {
                        buildAndPersistMetafield(testBuilder, MF_NAME_CREDIT_POOLS_TARIFF_CODE, DataType.STRING, EntityType.ORDER);
                    }

                    // Creating plan level meta-field
                    if (!isMetaFieldPresent(EntityType.PLAN, PLAN_METAFIELD_PLAN_RATING_NAME)) {
                        buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_PLAN_RATING_NAME, DataType.ENUMERATION, EntityType.PLAN);
                    }
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_QUANTITY_RESOLUTION_UNIT_NAME, DataType.ENUMERATION,
                            EntityType.PLAN);
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_INTERNET_TECHNOLOGY_NAME, DataType.ENUMERATION, EntityType.PLAN);
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_TAX_SCHEME_NAME, DataType.ENUMERATION, EntityType.PLAN);
                    // buildAndPersistMetafield(testBuilder,
                    // PLAN_METAFILD_TAX_SCHEME_NAME, DataType.ENUMERATION,
                    // EntityType.PLAN, true); // true

                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_COSTS_GL_CODE_NAME, DataType.STRING, EntityType.PLAN);
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_PLAN_GL_NAME, DataType.STRING, EntityType.PLAN);
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_USAGE_POOL_GL_CODE, DataType.STRING, EntityType.PLAN);
                    buildAndPersistMetafield(testBuilder, PLAN_METAFIELD_USAGE_POOL_COSTS_GL_CODE, DataType.STRING, EntityType.PLAN);

                    createNotifications(api);
                    logger.debug("creditPool85Notification: {}, creditPool50Notification: {}, creditPool100Notification: {}",
                            creditPool85Notification, creditPool50Notification, creditPool100Notification);
                    setProductCategoryWithIds(api);

                    // configure spc specific plugins:
                    configureAllSPCPlugins(envBuilder);
                    // Creating SPC Job Launcher
                    buildAndPersistMediationConfiguration(envBuilder, api, SPC_MEDIATION_CONFIG_NAME, SPC_MEDIATION_JOB_NAME);
                    buildAndPersistMediationConfiguration(envBuilder, api, OPTUS_MUR_MEDIATION_CONFIG_NAME, OPTUS_MUR_MEDIATION_JOB_NAME);

                    // Configure company level meta-field
                    createAndConfigureAllCompanyMetaFields(testBuilder, envBuilder);
                    updatePreference(Constants.PREFERENCE_INVOICE_LINE_TAX, "1");
                    //configure spc specific Preferences
                    updatePreference(Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION, "0");
                    updatePreference(Constants.PREFERENCE_SET_INVOICE_DELIVERY_METHOD_TO_EMAIL_AND_PAPER_IF_EMAIL_ID_IS_NOT_PROVIDED, "1");
                    //@formatter:off
            buildAndPersistAlertsDataTableRecord(
                    "SPCMO-01","",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMO-02","",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMO-03","",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMO-04","",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMO-05","",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "AGL-MOB-4","",
                    "50","AGL Mobile Data Alert. You've used 50% of your data allowance. If you use up your data allowance, we'll automatically give you a 1GB Data Boost for $10.",
                    "85","AGL Mobile Data Alert. You've used 85% of your data allowance. If you use up your data allowance, we'll automatically give you a 1GB Data Boost for $10.",
                    "100","AGL Mobile Data Alert. You've used up your data allowance, & now have 1GB Data Boost for $10. Once that‘s used, you'll get a second 1GB Data Boost for $10.",
                    "100% + First Boost","AGL Mobile Data Alert. You've used up your first 1GB Data Boost, & now have a second 1GB Data Boost for $10. Once that‘s used, you'll get a third 1GB Data Boost for $10. Data usage info: agl.com.au/mobiledata, AGL App or 1300 361 676",
                    "100% + Second Boost","AGL Mobile Data Alert. You've used up your second 1GB Data Boost, & now have a third 1GB Data Boost for $10. Once that's used, you won't be able to use additional data until next billing period. You may be charged for any additional data used ($0.01 per MB block), for a limited time before we discontinue your data access. If you'd like to continue to use additional data ($0.01 per MB block)",
                    "100% + Third Boost","AGL Mobile Data Alert. You won't be able to use additional data until the next billing period. If you do wish to use additional data (charged at $0.01 per MB block)",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "AGL-MOB-5","",
                    "50","AGL Mobile Data Alert. You've used 50% of your data allowance. If you use up your data allowance, we'll automatically give you a 1GB Data Boost for $10.",
                    "85","AGL Mobile Data Alert. You've used 85% of your data allowance. If you use up your data allowance, we'll automatically give you a 1GB Data Boost for $10.",
                    "100","AGL Mobile Data Alert. You've used up your data allowance, & now have 1GB Data Boost for $10. Once that‘s used, you'll get a second 1GB Data Boost for $10.",
                    "100% + First Boost","AGL Mobile Data Alert. You've used up your first 1GB Data Boost, & now have a second 1GB Data Boost for $10. Once that‘s used, you'll get a third 1GB Data Boost for $10.",
                    "100% + Second Boost","AGL Mobile Data Alert. You've used up your second 1GB Data Boost, & now have a third 1GB Data Boost for $10. Once that's used, you won't be able to use additional data until next billing period. You may be charged for any additional data used ($0.01 per MB block), for a limited time before we discontinue your data access. If you'd like to continue to use additional data ($0.01 per MB block)",
                    "100% + Third Boost","AGL Mobile Data Alert. You won't be able to use additional data until the next billing period. If you do wish to use additional data (charged at $0.01 per MB block)",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "AGL-MOB-6","",
                    "50","AGL Mobile Data Alert. You've used 50% of your data allowance. If you use up your data allowance, we'll automatically give you a 1GB Data Boost for $10.",
                    "85","AGL Mobile Data Alert. You've used 85% of your data allowance. If you use up your data allowance, we'll automatically give you a 1GB Data Boost for $10.",
                    "100","AGL Mobile Data Alert. You've used up your data allowance, & now have 1GB Data Boost for $10. Once that‘s used, you'll get a second 1GB Data Boost for $10.",
                    "100% + First Boost","AGL Mobile Data Alert. You've used up your first 1GB Data Boost, & now have a second 1GB Data Boost for $10. Once that‘s used, you'll get a third 1GB Data Boost for $10.",
                    "100% + Second Boost","AGL Mobile Data Alert. You've used up your second 1GB Data Boost, & now have a third 1GB Data Boost for $10. Once that's used, you won't be able to use additional data until next billing period. You may be charged for any additional data used ($0.01 per MB block), for a limited time before we discontinue your data access. If you'd like to continue to use additional data ($0.01 per MB block)",
                    "100% + Third Boost","AGL Mobile Data Alert. You won't be able to use additional data until the next billing period. If you do wish to use additional data (charged at $0.01 per MB block)",
                    "Y", DATA_TABLE_OPTUS_MUR_ALERT);
            // Usage pool alerts
            buildAndPersistAlertsDataTableRecord(
                    "SPCMT-02","Southern 4G $20",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_USAGE_POOL_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMT-18","NBN Family Pack",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_USAGE_POOL_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMT-20","Council Mobile $15",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_USAGE_POOL_ALERT);
            buildAndPersistAlertsDataTableRecord(
                "SPCMT-0211","Southern 4G $20",
                "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                "Y", DATA_TABLE_USAGE_POOL_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMT-26","NBN Triple Bundle",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_USAGE_POOL_ALERT);
            buildAndPersistAlertsDataTableRecord(
                    "SPCMT-29","Telstra Mobile Broadband $40",
                    "50","Southern Phone Data Alert. You have now used 50% of your included data.",
                    "85","Southern Phone Data Alert. You have now used 85% of your included data.",
                    "100","Southern Phone Data Alert. You have used your included data and have received your first Data Boost of 1 GB for $10.",
                    "100% + First Boost","Southern Phone Data Alert. You have used your included data and have received your second Data Boost of 1 GB for $10.",
                    "100% + Second Boost","Southern Phone Data Alert. You have used your included data and have received your third Data Boost of 1 GB for $10.",
                    "100% + Third Boost","Southern Phone: You've exceeded your data. Excess data is 5c/MB. Call 131464 for options including barring. This alert may be delayed by 48hrs",
                    "Y", DATA_TABLE_USAGE_POOL_ALERT);
            //@formatter:on
                    buildAndPersistCallToZeroDataTableRecord("21300790585", DATA_TABLE_CALLTOZERO);
                    buildAndPersistCallToZeroDataTableRecord("21800017461", DATA_TABLE_CALLTOZERO);

                    // Creating plan level meta-field
                    buildAndPersistMetafield(testBuilder, PLAN_LEVEL_METAFIELD, DataType.ENUMERATION, EntityType.PLAN);

                    categoryIds.clear();
                    categoryIds.add(envBuilder.idForCode(MOBILE_SERVICES_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(MOBILE_USAGE_CATEGORY));
                    categoryIds.add(envBuilder.idForCode(UNBILLED_REVENUE_INCLUDED_CATEGORY));
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, DATA_BOOST_PRODUCT_CODE_DBC_10_OPTUS, "Data Boost",
                            false, categoryIds, "9.0909", false, 0, true);
                    buildAndPersistFlatProductForMultipleCategories(envBuilder, api, DATA_BOOST_PRODUCT_CODE_DBC_10_TELSTRA,
                            "Data boost charges Telstra", false, categoryIds, "9.0909", false, 0, true);

                })
                .test((testEnv, testEnvBuilder) -> {
                    assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
                    assertNotNull("Mediated Categroy Creation Failed ", testEnvBuilder.idForCode(SPC_MEDIATED_USAGE_CATEGORY));
                    assertNotNull("Mediation Configuration Creation Failed ", testEnvBuilder.idForCode(SPC_MEDIATION_CONFIG_NAME));
                    assertNotNull("Internet Asset Product Creation Failed", testEnvBuilder.idForCode(INTERNET_ASSET_PLAN_ITEM_CODE));
                });

        }
    }

    @AfterTest(alwaysRun = true)
    public void tearDown() {
        boolean shouldExecute = false;
        if (shouldExecute) {
            logger.debug("SPCBaseConfiguration.tearDown");
            updateCurrency(CURRENCY_AUD, backupExchangeRate, backupSystemRate);
            // configure again BasicItemManager task.
            updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID, BasicItemManager.class.getName(), Collections.emptyMap());
            // configure again OrderFilterTask
            updateExistingPlugin(api, ORDER_FILTER_TASK_PLUGIN_ID, OrderFilterTask.class.getName(), Collections.emptyMap());

            Map<String, String> refundParams = new HashMap<>();
            refundParams.put(PARAM_LABEL_ADJUSTMENT_PRODUCT_ID, ADJUSTMENT_PRODUCT_ID.toString());
            updateExistingPlugin(api, REFUND_ON_CANCEL_TASK_PLUGIN_ID, RefundOnCancelTask.class.getName(), refundParams);
            Map<String, String> paperInvoiceNotificationParams = new HashMap<>();
            paperInvoiceNotificationParams.put("design", "simple_invoice_b2b");
            paperInvoiceNotificationParams.put("template", "1");
            PluggableTaskWS[]  tasks = api.getPluginsWS(api.getCallerCompanyId(), SPCPaperInvoiceNotificationTask.class.getName());
            updateExistingPlugin(api,tasks[0].getId(), PaperInvoiceNotificationTask.class.getName(),paperInvoiceNotificationParams);
            updatePreference(Constants.PREFERENCE_ITG_INVOICE_NOTIFICATION, "1");
            ConfigurationBuilder configurationBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
            configurationBuilder.addPlugin(ProrateCustomerUsagePoolTask.class.getName());
            if (configurationBuilder.pluginExists(TelcoInvoiceParametersTask.class.getName(), api.getCallerCompanyId())) {
                configurationBuilder.deletePlugin(TelcoInvoiceParametersTask.class.getName(), api.getCallerCompanyId());
            }
            dropTable(CUSTOMER_CARE_TABLE_VALUE);
            // testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
            // testBuilder.removeEntitiesCreatedOnJBilling();
            testBuilder = null;
        }

    }

    protected void setPlanLevelMetaField(Integer planId, String name, String value) {
        logger.debug("setting the plan level metafields for plan {}", planId);
        PlanWS plan = api.getPlanWS(planId);
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
        Arrays.asList(plan.getMetaFields()).forEach(mf -> {
            if (mf.getFieldName().equals(name)) {
                mf.setValue(value);
                values.add(mf);
            }
        });
        values.forEach(mfValue -> mfValue.setEntityId(api.getCallerCompanyId()));
        plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updatePlan(plan);
    }

    protected Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType,
            boolean mandatory) {
        MetaFieldWS value = new MetaFieldBuilder().name(name).dataType(dataType).entityType(entityType).mandatory(mandatory).primary(true)
                .build();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
    }

    private PriceModelWS getPriceModelWSByPrice(String price) {
        return new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId());
    }

    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String quantity,
            Integer categoryId, List<Integer> items, Integer precedence) {
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code).withQuantity(quantity).withResetValue("Reset To Initial Value")
                .withItemIds(items).addItemTypeId(categoryId).withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS)
                .withCyclePeriodValue(Integer.valueOf(1)).withName(code).build();
    }

    private void createAndConfigureAllCompanyMetaFields(TestBuilder testBuilder, TestEnvironmentBuilder envBuilder) {
        TestEnvironment environment = testBuilder.getTestEnvironment();

        logger.debug(SPC_CUSTOMER_CARE_ITEM_CODE + ": " + envBuilder.idForCode(SPC_CUSTOMER_CARE_ITEM_CODE).toString());
        setCompanyLevelMetaField(environment, SPC_CUSTOMER_CARE_ITEM_ID, envBuilder.idForCode(SPC_CUSTOMER_CARE_ITEM_CODE).toString());

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_BPAY_BILLER_CODE, DataType.STRING, EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_BPAY_BILLER_CODE, "12345");

        buildAndPersistMetafield(testBuilder, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID, DataType.STRING,
                EntityType.COMPANY);
        Integer sconnectItemId = envBuilder.idForCode(USAGE_PRODUCT_CODE_SCONNECT_TOTAL);
        logger.debug(USAGE_PRODUCT_CODE_SCONNECT_TOTAL + ": " + sconnectItemId);
        setCompanyLevelMetaField(environment, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_SCON_INTERNET_ITEM_ID,
                null != sconnectItemId ? sconnectItemId.toString() : "");

        buildAndPersistMetafield(testBuilder, SPCConstants.NUMBER_OF_DAYS_TO_BACK_DATED_EVENTS, DataType.STRING, EntityType.COMPANY);
        setCompanyLevelMetaField(environment, SPCConstants.NUMBER_OF_DAYS_TO_BACK_DATED_EVENTS, "1113");

        buildAndPersistMetafield(testBuilder, SPCConstants.DATA_ITEM_ID_FIELD_NAME, DataType.STRING, EntityType.COMPANY);
        Integer omMurMobileDataItemId = envBuilder.idForCode(USAGE_PRODUCT_CODE_OM_MUR_MOBILE_DATA);
        logger.debug(USAGE_PRODUCT_CODE_OM_MUR_MOBILE_DATA + ": " + omMurMobileDataItemId);
        setCompanyLevelMetaField(environment, SPCConstants.DATA_ITEM_ID_FIELD_NAME,
                null != omMurMobileDataItemId ? omMurMobileDataItemId.toString() : "");

        buildAndPersistMetafield(testBuilder, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_CUSTOMER_CARE_NUMBER_ITEM_ID, DataType.STRING,
                EntityType.COMPANY);
        Integer cccItemId = envBuilder.idForCode(SPC_CCC);
        logger.debug(SPC_CCC + ": " + cccItemId);
        setCompanyLevelMetaField(environment, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_CUSTOMER_CARE_NUMBER_ITEM_ID,
                null != cccItemId ? cccItemId.toString() : "");

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_​PLAN_BASED_FREE_CALL_INFO_TABLE_NAME, DataType.STRING,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_​PLAN_BASED_FREE_CALL_INFO_TABLE_NAME,
                DATA_TABLE_PLAN_BASED_FREE_CALL_INFO);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_CREDIT_PRODUCTS_CATEGORY_ID, DataType.STRING, EntityType.COMPANY);
        Integer creditCatId = envBuilder.idForCode(CREDIT_PRODUCTS_CATEGORY);
        logger.debug(CREDIT_PRODUCTS_CATEGORY + ": " + creditCatId);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_CREDIT_PRODUCTS_CATEGORY_ID,
                null != creditCatId ? creditCatId.toString() : "");

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_DEBIT_PRODUCTS_CATEGORY_ID, DataType.STRING, EntityType.COMPANY);
        Integer debitCategoryId = envBuilder.idForCode(DEBIT_PRODUCTS_CATEGORY);
        logger.debug(DEBIT_PRODUCTS_CATEGORY + ": " + debitCategoryId);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_DEBIT_PRODUCTS_CATEGORY_ID,
                null != debitCategoryId ? debitCategoryId.toString() : "");

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_TOTAL_BYTES, DataType.STRING,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_TOTAL_BYTES, null);

        buildAndPersistMetafield(testBuilder, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID, DataType.STRING,
                EntityType.COMPANY);
        Integer aaptItemId = envBuilder.idForCode(SPC_AAPT_INT_TOTAL);
        logger.debug(SPC_AAPT_INT_TOTAL + ": " + aaptItemId);
        setCompanyLevelMetaField(environment, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_AAPT_INTERNET_ITEM_ID,
                null != aaptItemId ? aaptItemId.toString() : "");

        buildAndPersistMetafield(testBuilder, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID, DataType.STRING,
                EntityType.COMPANY);
        Integer seIntTotal = envBuilder.idForCode(SPC_SE_INT_TOTAL);
        logger.debug(SPC_SE_INT_TOTAL + ": " + seIntTotal);
        setCompanyLevelMetaField(environment, SPCConstants.COMPANY_LEVEL_MF_NAME_FOR_SE_INTERNET_ITEM_ID,
                null != seIntTotal ? seIntTotal.toString() : "");

        // Integer
        Integer excludeCallItemisationCategoryId = envBuilder.idForCode(SPC_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY);
        logger.debug(SPC_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY + ": " + excludeCallItemisationCategoryId);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_EXCLUDE_FROM_CALL_ITEMISATION_CATEGORY_ID,
                null != excludeCallItemisationCategoryId ? excludeCallItemisationCategoryId : null);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_TAX_DATE_FORMAT, DataType.STRING, EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_TAX_DATE_FORMAT, PARAM_TAX_DATE_FORMAT);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_DOWNLOAD_BYTES, DataType.STRING,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_DOWNLOAD_BYTES, null);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_TAX_TABLE_NAME, DataType.STRING, EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_TAX_TABLE_NAME, PARAM_TAX_TABLE_NAME);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_PRODUCT_CATEGORY_ID_OF_INTERNET_USAGE_ITEMS, DataType.STRING,
                EntityType.COMPANY);
        Integer internetUsageCategoryId = envBuilder.idForCode(INTERNET_USAGE_CATEGORY);
        logger.debug(INTERNET_USAGE_CATEGORY + ": " + internetUsageCategoryId);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_PRODUCT_CATEGORY_ID_OF_INTERNET_USAGE_ITEMS,
                null != internetUsageCategoryId ? internetUsageCategoryId.toString() : "");

        // INTEGER
        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_VOIP_USAGE_PRODUCT_CATEGORY_ID, DataType.INTEGER,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_VOIP_USAGE_PRODUCT_CATEGORY_ID, null);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_UPLOAD_BYTES, DataType.STRING,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_ITEM_ID_FOR_CHARGING_UPLOAD_BYTES, null);

        // INTEGER
        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID, DataType.INTEGER,
                EntityType.COMPANY);
        Integer accountChargesCategoryId = envBuilder.idForCode(ACCOUNT_CHARGES_CATEGORY);
        logger.debug(ACCOUNT_CHARGES_CATEGORY + ": " + accountChargesCategoryId);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_ACCOUNT_CHARGES_PRODUCT_CATEGORY_ID, accountChargesCategoryId);

        // INTEGER
        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID, DataType.INTEGER,
                EntityType.COMPANY);
        logger.debug(OTHER_CHARGES_AND_CREDITS_CATEGORY + ": " + envBuilder.idForCode(OTHER_CHARGES_AND_CREDITS_CATEGORY));
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_OTHER_CHARGES_AND_CREDITS_PRODUCT_CATEGORY_ID,
                envBuilder.idForCode(OTHER_CHARGES_AND_CREDITS_CATEGORY));

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_OPTUS_PLAN_CODES, DataType.STRING, EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_OPTUS_PLAN_CODES, COMPANY_LEVEL_MF_VALUE_OPTUS_PLAN_CODES);

        buildAndPersistMetafield(testBuilder, COMPANY_LEVEL_MF_NAME_FOR_BILLING_ADDRESS_INFO_GROUP_NAME, DataType.STRING,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, COMPANY_LEVEL_MF_NAME_FOR_BILLING_ADDRESS_INFO_GROUP_NAME,
                COMPANY_LEVEL_MF_VALUE_BILLING_ADDRESS);

        buildAndPersistMetafield(testBuilder, Constants.CUSTOMER_CONTACT_DETAILS_AIT_GROUP_NAME, DataType.STRING,
                EntityType.COMPANY);
        setCompanyLevelMetaField(environment, Constants.CUSTOMER_CONTACT_DETAILS_AIT_GROUP_NAME,
                COMPANY_LEVEL_MF_VALUE_CUSTOMER_DETAILS);
    }

    private void createNotifications(JbillingAPI api) {
        NOTIFICAION_MAP = new HashMap<String, String>();

        Integer notificationTypeId1 = createMessageNotificationType(api, CREDIT_POOL_85);
        assertNotNull("Notification type creation failed for Credit Pool 85%", notificationTypeId1);
        String subject1 = "Credit Pool Notification 85%";
        String body1 = "Southern Phone credit Alert. " + "You have now used $percentageConsumption percent of your included credit. "
                + "Thanks, The Southern Phone Team";
        String htmlBody1 = "Southern Phone credit Alert. " + "You have now used $percentageConsumption percent of your included credit. "
                + "Thanks, The Southern Phone Team";
        creditPool85Notification = createNotification(api, notificationTypeId1, subject1, body1, htmlBody1);
        assertNotNull("Notification creation failed for Credit Pool 85%", creditPool85Notification);
        NOTIFICAION_MAP.put(CREDIT_POOL_85, notificationTypeId1.toString());

        Integer notificationTypeId2 = createMessageNotificationType(api, CREDIT_POOL_50);
        assertNotNull("Notification type creation failed for Credit Pool 50%", notificationTypeId2);
        String subject2 = "Credit Pool Notification 50%";
        String body2 = "Southern Phone credit Alert. " + "You have now used $percentageConsumption percent of your included credit"
                + ". Thanks, The Southern Phone Team";
        String htmlBody2 = "Southern Phone credit Alert. " + "You have now used $percentageConsumption percent of your included credit. "
                + "Thanks, The Southern Phone Team";
        creditPool50Notification = createNotification(api, notificationTypeId2, subject2, body2, htmlBody2);
        assertNotNull("Notification creation failed for Credit Pool 50%", creditPool50Notification);
        NOTIFICAION_MAP.put(CREDIT_POOL_50, notificationTypeId2.toString());

        Integer notificationTypeId3 = createMessageNotificationType(api, CREDIT_POOL_100);
        assertNotNull("Notification type creation failed for Credit Pool 100%", notificationTypeId3);
        String subject3 = "Credit Pool Notification 100%";
        String body3 = "Southern Phone credit Alert. " + "You have now used $percentageConsumption percent of your included credit. "
                + "Thanks, The Southern Phone Team";
        String htmlBody3 = "Southern Phone credit Alert. " + "You have now used $percentageConsumption percent of your included credit. "
                + "Thanks, The Southern Phone Team";
        creditPool100Notification = createNotification(api, notificationTypeId3, subject3, body3, htmlBody3);
        assertNotNull("Notification creation failed for Credit Pool 100%", creditPool100Notification);
        NOTIFICAION_MAP.put(CREDIT_POOL_100, notificationTypeId3.toString());
    }

    private Integer createMessageNotificationType(JbillingAPI api, String description) {
        Integer notificationTypeId = api.createMessageNotificationType(4, description, 1);
        return notificationTypeId;
    }

    private Integer createNotification(JbillingAPI api, Integer typeId, String subject, String body, String htmlBody) {
        NotificationBuilder notificationBuilder = new NotificationBuilder().withTypeId(typeId).withContent(
                new MessageSection[] { new MessageSection(1, subject), new MessageSection(2, body), new MessageSection(3, htmlBody) });

        try {
            return api.getIdFromCreateUpdateNotification(null, notificationBuilder.build());
        } catch (Exception e) {
            logger.debug("Exception: {}", e.getMessage());
            fail("The creation of the notification shouldn't fail");
        }
        return null;
    }

    private void setProductCategoryWithIds(JbillingAPI api) {
        ItemTypeWS[] itemTypes = api.getAllItemCategoriesByEntityId(api.getCallerCompanyId());
        if (ArrayUtils.isNotEmpty(itemTypes)) {
            PRODUCT_CATEGOY_MAP = new HashMap<String, String>();
            for (ItemTypeWS category : Arrays.asList(itemTypes)) {
                PRODUCT_CATEGOY_MAP.put(category.getDescription(), category.getId().toString());
            }
            logger.debug("Product category size: {}", PRODUCT_CATEGOY_MAP.size());
        }
    }

    private void configureAllSPCPlugins(TestEnvironmentBuilder envBuilder) {
        ConfigurationBuilder configurationBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
        configureSpcCreditOrderCreationTask(configurationBuilder);

        configureSpcServiceSummaryGenerationTask(configurationBuilder);

        configureSpcUsagePoolFeeChargingTask(configurationBuilder);

        configureOptusMurNotificationTask(configurationBuilder);
        configureTelcoOrderLineBasedCompositionTask(configurationBuilder);
        configureSpcNotificationTask(configurationBuilder);
        configureCustomerUsagePoolConsumptionActionTask(configurationBuilder);

        if (!configurationBuilder.pluginExists(SPCRemoveAssetFromActiveOrderTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.addPlugin(SPCRemoveAssetFromActiveOrderTask.class.getName()).withProcessingOrder(
                    SPCRemoveAssetFromActiveOrderTask.class.getName(), 1006);
        }
        if (!configurationBuilder.pluginExists(SPCCustomerUsagePoolTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.addPlugin(SPCCustomerUsagePoolTask.class.getName()).withProcessingOrder(
                    SPCCustomerUsagePoolTask.class.getName(), 1007);
        }
        if (!configurationBuilder.pluginExists(AssetAssignmentEvaluationStrategy.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.addPlugin(AssetAssignmentEvaluationStrategy.class.getName()).withProcessingOrder(
                    AssetAssignmentEvaluationStrategy.class.getName(), 1008);
        }

        // usage manager task.
        updateExistingPlugin(api, BASIC_ITEM_MANAGER_PLUGIN_ID, SPCUsageManagerTask.class.getName(), getSpcUsageManagerParams());

        updateExistingPlugin(api, ORDER_FILTER_TASK_PLUGIN_ID, SPCOrderFilterTask.class.getName(), Collections.emptyMap());

        Integer internetCreditProductId = envBuilder.idForCode(INTERNET_CREDIT_PRODUCT);
        Integer voipCreditProductId = envBuilder.idForCode(VOIP_CREDIT_PRODUCT);
        Integer mobileCreditProductId = envBuilder.idForCode(MOBILE_CREDIT_PRODUCT);
        logger.debug("Internet Credit ProductId: " + internetCreditProductId + ", VOIP Credit ProductId: " + voipCreditProductId
                + ", Mobile Credit ProductId: " + mobileCreditProductId);
        updateExistingPlugin(api, REFUND_ON_CANCEL_TASK_PLUGIN_ID, SPCProrateRefundOnCancelTask.class.getName(),
                getSpcRefundParams(internetCreditProductId, voipCreditProductId, mobileCreditProductId));
        PluggableTaskWS[]  tasks = api.getPluginsWS(api.getCallerCompanyId(), PaperInvoiceNotificationTask.class.getName());
        updateExistingPlugin(api,tasks[0].getId(), SPCPaperInvoiceNotificationTask.class.getName(),getSPCPaperInvoiceNotificationParams());
        //Remove FullCreative Plugins
        if (configurationBuilder.pluginExists(FullCreativeCustomInvoiceFieldsTokenTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.deletePlugin(FullCreativeCustomInvoiceFieldsTokenTask.class.getName(), api.getCallerCompanyId());
        }
        if (configurationBuilder.pluginExists(FullCreativeCustomEmailTokenTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.deletePlugin(FullCreativeCustomEmailTokenTask.class.getName(), api.getCallerCompanyId());
        }
        if (!configurationBuilder.pluginExists(TelcoInvoiceParametersTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.addPlugin(TelcoInvoiceParametersTask.class.getName()).withProcessingOrder(
                TelcoInvoiceParametersTask.class.getName(), 1061);
        }

        configureSpcOrderValidationTask(configurationBuilder);

        if (configurationBuilder.pluginExists(CustomerPlanSubscriptionProcessingTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> planSubscriptionParameters = new Hashtable<>();
            planSubscriptionParameters.put("Prorate Usage Pool Quantity for First Period?", "false");
            planSubscriptionParameters.put("Should rerate mediated order?", "false");
            PluggableTaskWS[] pluggableTask = api.getPluginsWS(api.getCallerCompanyId(),
                    CustomerPlanSubscriptionProcessingTask.class.getName());
            PluggableTaskWS plugin = pluggableTask[0];
            plugin.setParameters(planSubscriptionParameters);
            api.updatePlugin(plugin);
        }
        /*
         * // category: 22 if
         * (!configurationBuilder.pluginExists(SPCOptusMurUsagePoolScheduledTask
         * .class.getName(), api.getCallerCompanyId())) { Hashtable<String,
         * String> optusMurParameters = new Hashtable<>();
         * optusMurParameters.put("cron_exp", "0 0/1 0 ? * * 2020");
         * configurationBuilder
         * .addPluginWithParameters(SPCOptusMurUsagePoolScheduledTask
         * .class.getName(), optusMurParameters); }
         */

        // Category: 32
        configureSpcJMRPostProcessorTask(configurationBuilder);

        // Category: 33
        if (!configurationBuilder.pluginExists(SPCMediationCurrentPeriodEvaluationStrategyTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.addPlugin(SPCMediationCurrentPeriodEvaluationStrategyTask.class.getName()).withProcessingOrder(
                    SPCMediationCurrentPeriodEvaluationStrategyTask.class.getName(), 1);
        }
        // Category: 34
        configureSPCUserFilterTask(configurationBuilder);

        if (configurationBuilder.pluginExists(ProrateCustomerUsagePoolTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.deletePlugin(ProrateCustomerUsagePoolTask.class.getName(), api.getCallerId());
        }
        configurationBuilder.build();

        ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
        // Category: 35
        configureSPCCustomerUsagePoolEvaluationTask(confBuilder);
        // category 22
        createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,false);
        confBuilder.build();
    }

    private Map<String, String> getSpcUsageManagerParams() {
        Map<String, String> SPCUsageManagerParams = new HashMap<>();
        SPCUsageManagerParams.put("Internate_Usage_Field_Name", "USER_NAME");
        SPCUsageManagerParams.put("VOIP_Usage_Field_Name", "SERVICE_NUMBER");
        return SPCUsageManagerParams;
    }

    private Map<String, String> getSpcRefundParams(Integer internetCreditProductId, Integer voipCreditProductId,
            Integer mobileCreditProductId) {
        Map<String, String> spcRefundParams = new HashMap<>();

        spcRefundParams.put(INTERNET_SERVICES_CATEGORY, null != internetCreditProductId ? internetCreditProductId.toString() : "");
        spcRefundParams.put(VOICE_SERVICES_CATEGORY, null != voipCreditProductId ? voipCreditProductId.toString() : "");
        spcRefundParams.put(MOBILE_SERVICES_CATEGORY, null != mobileCreditProductId ? mobileCreditProductId.toString() : "");
        return spcRefundParams;
    }

    private Map<String, String> getSPCPaperInvoiceNotificationParams() {
        Map<String, String> spcPaperInvoiceNotificationParams = new HashMap<>();

        spcPaperInvoiceNotificationParams.put("design", "spc_invoice_main_report_v3");
        spcPaperInvoiceNotificationParams.put("remove_blank_page", "true");
        spcPaperInvoiceNotificationParams.put("sql_query", "true");
        spcPaperInvoiceNotificationParams.put("20190808", "spc_invoice_main_report");
        spcPaperInvoiceNotificationParams.put("20191030", "spc_invoice_main_report_v2");

        return spcPaperInvoiceNotificationParams;
    }

    private void configureSpcCreditOrderCreationTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SpcCreditOrderCreationTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> spcCreditOrderParams = new Hashtable<>();
            spcCreditOrderParams.put("Credit pool table name", PARAM_CREDIT_POOL_TABLE_NAME);
            spcCreditOrderParams.put("Subscription order id meta field name", MF_NAME_SUBSCRIPTION_ORDER_ID);
            spcCreditOrderParams.put("Credit pools tariff code MF name", MF_NAME_CREDIT_POOLS_TARIFF_CODE);
            spcCreditOrderParams.put("tax table name", PARAM_TAX_TABLE_NAME);
            configurationBuilder.addPluginWithParameters(SpcCreditOrderCreationTask.class.getName(), spcCreditOrderParams)
                    .withProcessingOrder(SpcCreditOrderCreationTask.class.getName(), 1000);
        }
    }

    private void configureSpcServiceSummaryGenerationTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SpcServiceSummaryGenerationTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> SPCServiceSummaryParams = new Hashtable<>();
            SPCServiceSummaryParams.put("Asset level service identifier mf name", MF_NAME_SERVICE_ID);
            SPCServiceSummaryParams.put("OrderLine level service identifier mf name", MF_NAME_SERVICE_ID);
            SPCServiceSummaryParams.put("Subscription order id meta field name", MF_NAME_SUBSCRIPTION_ORDER_ID);
            // SPCServiceSummaryParams.put("zero_price_exculded_categories",
            // "44,66,59,60");
            String zeroPriceExculdedCategories = String.join(",", PRODUCT_CATEGOY_MAP.get("Internet User Names"),
                    PRODUCT_CATEGOY_MAP.get("Exclude Price Zero Assets"), PRODUCT_CATEGOY_MAP.get("Mobile Numbers"),
                    PRODUCT_CATEGOY_MAP.get("Voice Numbers"));
            SPCServiceSummaryParams.put("zero_price_exculded_categories", zeroPriceExculdedCategories);

            configurationBuilder.addPluginWithParameters(SpcServiceSummaryGenerationTask.class.getName(), SPCServiceSummaryParams)
                    .withProcessingOrder(SpcServiceSummaryGenerationTask.class.getName(), 1002);
        }
    }

    private void configureSpcUsagePoolFeeChargingTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SPCUsagePoolFeeChargingTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> SPCFeeChargingParams = new Hashtable<>();
            SPCFeeChargingParams.put("Subscription order id meta field name", MF_NAME_SUBSCRIPTION_ORDER_ID);

            configurationBuilder.addPluginWithParameters(SPCUsagePoolFeeChargingTask.class.getName(), SPCFeeChargingParams)
                    .withProcessingOrder(SPCUsagePoolFeeChargingTask.class.getName(), 1003);
        }
    }

    // TelcoOrderLineBasedCompositionTask
    private void configureTelcoOrderLineBasedCompositionTask(ConfigurationBuilder configurationBuilder) {
        if (configurationBuilder.pluginExists(OrderChangeBasedCompositionTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.deletePlugin(OrderChangeBasedCompositionTask.class.getName(), api.getCallerCompanyId());
        }
        if (!configurationBuilder.pluginExists(TelcoOrderLineBasedCompositionTask.class.getName(), api.getCallerCompanyId())) {
            configurationBuilder.addPlugin(TelcoOrderLineBasedCompositionTask.class.getName()).withProcessingOrder(
                    TelcoOrderLineBasedCompositionTask.class.getName(), 991);
        }
    }

    private void configureOptusMurNotificationTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(OptusMurNotificationTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> SPCOptusNotificationParams = new Hashtable<>();

            SPCOptusNotificationParams.put("100", NOTIFICAION_MAP.get(CREDIT_POOL_100));
            SPCOptusNotificationParams.put("50", NOTIFICAION_MAP.get(CREDIT_POOL_50));
            SPCOptusNotificationParams.put("85", NOTIFICAION_MAP.get(CREDIT_POOL_85));

            configurationBuilder.addPluginWithParameters(OptusMurNotificationTask.class.getName(), SPCOptusNotificationParams)
                    .withProcessingOrder(OptusMurNotificationTask.class.getName(), 1004);
        }
    }

    private void configureSpcNotificationTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SpcNotificationTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> SPCNotificationParams = new Hashtable<>();

            SPCNotificationParams.put("100", NOTIFICAION_MAP.get(CREDIT_POOL_100));
            SPCNotificationParams.put("50", NOTIFICAION_MAP.get(CREDIT_POOL_50));
            SPCNotificationParams.put("85", NOTIFICAION_MAP.get(CREDIT_POOL_85));

            SPCNotificationParams.put("password", "******");
            SPCNotificationParams.put("send_email_notification", "true");
            SPCNotificationParams.put("crm_payload_prefixes", "11x,3x,,");
            SPCNotificationParams.put("use_access_token_endpoint", "false");
            SPCNotificationParams.put("username", "test");
            SPCNotificationParams.put("timeout", "10");

            configurationBuilder.addPluginWithParameters(SpcNotificationTask.class.getName(), SPCNotificationParams).withProcessingOrder(
                    SpcNotificationTask.class.getName(), 1005);
        }
    }

    private void configureCustomerUsagePoolConsumptionActionTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(CustomerUsagePoolConsumptionActionTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> customerUsagePoolConsumptionParams = new Hashtable<>();
            configurationBuilder.addPluginWithParameters(CustomerUsagePoolConsumptionActionTask.class.getName(), customerUsagePoolConsumptionParams)
                    .withProcessingOrder(CustomerUsagePoolConsumptionActionTask.class.getName(), 2011);
        }
    }

    private void configureSpcOrderValidationTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SpcOrderValidationTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> SPCOrderValidationParams = new Hashtable<>();
            // SPCOrderValidationParams.put("exculded_categories",
            // "40,22,54,55,61,57,37,34,49,67,28");
            SPCOrderValidationParams.put("Asset level service identifier mf name", MF_NAME_SERVICE_ID);
            SPCOrderValidationParams.put("OrderLine level service identifier mf name", MF_NAME_SERVICE_ID);

            String exculded_categories = String.join(",", PRODUCT_CATEGOY_MAP.get("Account Charges"),
                    PRODUCT_CATEGOY_MAP.get(CREDIT_DEBIT_ADJUSTMENTS_CATEGORY), PRODUCT_CATEGOY_MAP.get("Unearned Revenue - Excluded"),
                    PRODUCT_CATEGOY_MAP.get("Unbilled Revenue - Excluded"), PRODUCT_CATEGOY_MAP.get(CREDIT_POOL_CATEGORY),
                    PRODUCT_CATEGOY_MAP.get(MOBILE_USAGE_CATEGORY), PRODUCT_CATEGOY_MAP.get(MOBILE_SERVICES_CATEGORY),
                    PRODUCT_CATEGOY_MAP.get(VOICE_USAGE_CATEGORY), PRODUCT_CATEGOY_MAP.get(VOICE_SERVICES_CATEGORY),
                    PRODUCT_CATEGOY_MAP.get("Migration Adjustment"), PRODUCT_CATEGOY_MAP.get("Unbilled Revenue - Included"),
                    PRODUCT_CATEGOY_MAP.get(INTERNET_SERVICES_CATEGORY));

            SPCOrderValidationParams.put("exculded_categories", exculded_categories);

            configurationBuilder.addPluginWithParameters(SpcOrderValidationTask.class.getName(), SPCOrderValidationParams)
                    .withProcessingOrder(SpcOrderValidationTask.class.getName(), 1009);
        }
    }

    private void configureSpcJMRPostProcessorTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SpcJMRPostProcessorTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> spcJMRPostProcessorParams = new Hashtable<>();
            spcJMRPostProcessorParams.put(SpcJMRPostProcessorTask.PARAM_TAX_TABLE_NAME.getName(), PARAM_TAX_TABLE_NAME);
            spcJMRPostProcessorParams.put(SpcJMRPostProcessorTask.PARAM_CREDIT_POOL_TABLE_NAME.getName(), PARAM_CREDIT_POOL_TABLE_NAME);
            spcJMRPostProcessorParams.put(PARAM_ROUNDING_MODE, PARAM_ROUND_HALF_UP);
            spcJMRPostProcessorParams.put(PARAM_ROUNDING_SCALE, "2");
            spcJMRPostProcessorParams.put(SpcJMRPostProcessorTask.PARAM_TAX_DATE_FORMAT.getName(), PARAM_TAX_DATE_FORMAT);
            spcJMRPostProcessorParams.put(PARAM_MINIMUM_CHARGE, "0.00");

            configurationBuilder.addPluginWithParameters(SpcJMRPostProcessorTask.class.getName(), spcJMRPostProcessorParams)
                    .withProcessingOrder(SpcJMRPostProcessorTask.class.getName(), 1);
        }
    }

    private void configureSPCUserFilterTask(ConfigurationBuilder configurationBuilder) {
        if (!configurationBuilder.pluginExists(SPCUserFilterTask.class.getName(), api.getCallerCompanyId())) {
            Hashtable<String, String> userFilterrParameters = new Hashtable<>();
            userFilterrParameters.put(SPCUserFilterTask.PARAM_DAYS_TO_DELAY_BILLING.getName(), "3");
            userFilterrParameters.put(SPCUserFilterTask.PARAM_CUSTOMER_TYPE_MF_NAME.getName(), "Customer Type");
            userFilterrParameters.put(SPCUserFilterTask.PARAM_CUSTOMER_TYPE.getName(), "Post Paid");

            configurationBuilder.addPluginWithParameters(SPCUserFilterTask.class.getName(), userFilterrParameters).withProcessingOrder(
                    SPCUserFilterTask.class.getName(), 1);
        }
    }

    private void configureSPCCustomerUsagePoolEvaluationTask(ConfigurationBuilder confBuilder) {
        if (!confBuilder.pluginExists(SPCCustomerUsagePoolEvaluationTask.class.getName(), api.getCallerCompanyId())) {
            PluggableTaskWS pluggableTask = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(SPCUserFilterTask.class.getName())
                    .getId());
            Integer spcUserFilterTaskPluginId = pluggableTask.getId();

            Hashtable<String, String> customerEvaluationTaskParameters = new Hashtable<>();
            customerEvaluationTaskParameters.put("Spc User Filter Task Plugin Id", spcUserFilterTaskPluginId.toString());
            confBuilder.addPluginWithParameters(SPCCustomerUsagePoolEvaluationTask.class.getName(), customerEvaluationTaskParameters)
                    .withProcessingOrder(SPCCustomerUsagePoolEvaluationTask.class.getName(), 1);
        }
    }
    
    protected void configureOrderChangeUpdateTask(ConfigurationBuilder confBuilder, String date) {
        if (!confBuilder.pluginExists(OrderChangeUpdateTask.class.getName(), api.getCallerCompanyId())) {
            
            Hashtable<String, String> userFilterrParameters = new Hashtable<>();
            userFilterrParameters.put("future_date", date);
            
            confBuilder.addPluginWithParameters(OrderChangeUpdateTask.class.getName(), userFilterrParameters).withProcessingOrder(
                    OrderChangeUpdateTask.class.getName(), 25684);
        }
    }
    
    protected void deleteOrderChangeUpdateTask(ConfigurationBuilder confBuilder) {
        if (confBuilder.pluginExists(OrderChangeUpdateTask.class.getName(), api.getCallerCompanyId())) {
            confBuilder.deletePlugin(OrderChangeUpdateTask.class.getName(), api.getCallerCompanyId());
        }
    }
    
    protected PriceModelWS buildRateCardPriceModel(Integer routeRateCardId) {
        return buildRateCardPriceModel(routeRateCardId, "duration");
    }

    PriceModelWS buildRateCardPriceModel(Integer routeRateCardId, String quantityFieldName) {
        PriceModelWS routeRate = new PriceModelWS(PriceModelStrategy.ROUTE_BASED_RATE_CARD.name(), null, 1);
        SortedMap<String, String> attributes = new TreeMap<>();
        attributes.put("route_rate_card_id", Integer.toString(routeRateCardId));
        attributes.put("cdr_duration_field_name", quantityFieldName);
        routeRate.setAttributes(attributes);
        return routeRate;
    }

    protected Integer buildAndPersistEnumeration(TestEnvironmentBuilder envBuilder, List<EnumerationValueWS> values, String name) {
        EnumerationWS enUmerationcheck = api.getEnumerationByName(name);
        if (null != enUmerationcheck) {
            return enUmerationcheck.getId();
        }
        EnumerationWS enUmeration = new EnumerationWS();

        enUmeration.setValues(values);
        enUmeration.setName(name);
        enUmeration.setEntityId(envBuilder.getPrancingPonyApi().getCallerCompanyId());

        Integer enumId = envBuilder.getPrancingPonyApi().createUpdateEnumeration(enUmeration);
        envBuilder.env().add(name, enumId, name, envBuilder.getPrancingPonyApi(), TestEntityType.ENUMERATION);
        return enumId;

    }

    OrderLineWS getLineByItemId(OrderWS order, Integer itemId) {
        for (OrderLineWS orderLine : order.getOrderLines()) {
            if (orderLine.getItemId().equals(itemId)) {
                return orderLine;
            }
        }
        return null;
    }

    protected Integer createOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate,
            Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode) {
        this.testBuilder.given(
                envBuilder -> {
                    List<OrderLineWS> lines = productQuantityMap.entrySet().stream().map(lineItemQuatityEntry -> {
                        OrderLineWS line = new OrderLineWS();
                        line.setItemId(lineItemQuatityEntry.getKey());
                        line.setTypeId(Integer.valueOf(1));
                        ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                        line.setDescription(item.getDescription());
                        line.setQuantity(lineItemQuatityEntry.getValue());
                        line.setUseItem(true);
                        if (null != productAssetMap && !productAssetMap.isEmpty() && productAssetMap.containsKey(line.getItemId())) {
                            List<Integer> assets = productAssetMap.get(line.getItemId());
                            line.setAssetIds(assets.toArray(new Integer[0]));
                            line.setQuantity(assets.size());
                        }
                        return line;
                    }).collect(Collectors.toList());

                    envBuilder.orderBuilder(api).withCodeForTests(code).forUser(envBuilder.idForCode(userCode))
                            .withActiveSince(activeSince).withActiveUntil(activeUntil).withEffectiveDate(activeSince)
                            .withPeriod(orderPeriodId).withProrate(prorate).withOrderLines(lines)
                            .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID).build();
                }).test((testEnv, envBuilder) -> assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code)));
        return testBuilder.getTestEnvironment().idForCode(code);
    }
    
    protected Integer createOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate,
            Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, Integer userId,
            String price, String serviceId, String discription) {
        this.testBuilder.given(
                envBuilder -> {
                    List<OrderLineWS> lines = productQuantityMap.entrySet().stream().map(lineItemQuatityEntry -> {
                        MetaFieldValueWS serviceIdMetaFieldValue = new MetaFieldValueWS();
                        serviceIdMetaFieldValue.setFieldName(MF_NAME_SERVICE_ID);
                        serviceIdMetaFieldValue.setStringValue(serviceId);
                        serviceIdMetaFieldValue.getMetaField().setDataType(DataType.STRING);

                        OrderLineWS line = new OrderLineWS();
                        line.setItemId(lineItemQuatityEntry.getKey());
                        line.setTypeId(Integer.valueOf(1));
                        ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                        line.setDescription(StringUtils.isNotBlank(discription) ? discription : item.getDescription());
                        line.setQuantity(lineItemQuatityEntry.getValue());
                        line.setUseItem(false);
                        line.setAmount(price);
                        line.setMetaFields(new MetaFieldValueWS[]{serviceIdMetaFieldValue});
                        line.setPrice(price);
                        if (null != productAssetMap && !productAssetMap.isEmpty() && productAssetMap.containsKey(line.getItemId())) {
                            List<Integer> assets = productAssetMap.get(line.getItemId());
                            line.setAssetIds(assets.toArray(new Integer[0]));
                            line.setQuantity(assets.size());
                        }
                        return line;
                    }).collect(Collectors.toList());

                    envBuilder.orderBuilder(api).withCodeForTests(code).forUser(userId)
                    .withActiveSince(activeSince).withActiveUntil(activeUntil).withEffectiveDate(activeSince)
                    .withPeriod(orderPeriodId).withProrate(prorate).withOrderLines(lines).withCurrency(api.getUserWS(userId).getCurrencyId())
                    .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID).build();
                }).test((testEnv, envBuilder) -> assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code)));
        return testBuilder.getTestEnvironment().idForCode(code);
    }

    /**
     * 
     * @param orderCode
     * @param userId
     * @param activeSince
     * @param activeUntil
     * @param orderPeriodId
     * @param billingTypeId
     * @param prorate
     *            : True Or False
     * @param productQuantityMap
     *            : It must contain item Id of plan
     * @param assets
     *            : Get the list assets needs to add in plan order.
     * @param planId
     *            : Helping parameter for building order change plan Items.
     * @return
     */
    protected Integer createOrderWithAsset(String orderCode, Integer userId, Date activeSince, Date activeUntil, Integer orderPeriodId,
            Integer billingTypeId, boolean prorate, Map<Integer, BigDecimal> productQuantityMap, List<AssetWS> assets, Integer planId) {

        this.testBuilder.given(envBuilder -> {
            List<OrderLineWS> lines = productQuantityMap.entrySet().stream().map(lineItemQuatityEntry -> {
                OrderLineWS line = new OrderLineWS();
                line.setItemId(lineItemQuatityEntry.getKey());
                line.setTypeId(Integer.valueOf(1));
                ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                line.setDescription(item.getDescription());
                line.setQuantity(lineItemQuatityEntry.getValue());
                line.setUseItem(true);
                return line;
            }).collect(Collectors.toList());

            envBuilder.orderBuilder(api).forUser(userId).withCodeForTests(orderCode).withActiveSince(activeSince)
                    .withCurrency(CURRENCY_AUD).withActiveUntil(activeUntil).withEffectiveDate(activeSince)
                    .withBillingTypeId(billingTypeId).withPeriod(orderPeriodId).withProrate(prorate).withOrderLines(lines)
                    .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID).withPlanId(planId).build(assets);

        });
        return testBuilder.getTestEnvironment().idForCode(orderCode);
    }

    protected Integer createOrderWithAsset(String orderCode, Integer userId, Date activeSince, Date activeUntil, Integer orderPeriodId,
            Integer billingTypeId, boolean prorate, Map<Integer, BigDecimal> productQuantityMap, List<AssetWS> assets, Integer planId,
            String serviceId) {

        this.testBuilder.given(envBuilder -> {
            List<OrderLineWS> lines = productQuantityMap.entrySet().stream().map(lineItemQuatityEntry -> {

                MetaFieldValueWS serviceIdMetaFieldValue = new MetaFieldValueWS();
                serviceIdMetaFieldValue.setFieldName(MF_NAME_SERVICE_ID);
                serviceIdMetaFieldValue.setStringValue(serviceId);
                serviceIdMetaFieldValue.getMetaField().setDataType(DataType.STRING);

                OrderLineWS line = new OrderLineWS();
                line.setItemId(lineItemQuatityEntry.getKey());
                line.setTypeId(Integer.valueOf(1));
                ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                line.setDescription(item.getDescription());
                line.setQuantity(lineItemQuatityEntry.getValue());
                line.setUseItem(true);
                line.setMetaFields(new MetaFieldValueWS[] { serviceIdMetaFieldValue });
                return line;
            }).collect(Collectors.toList());

            envBuilder.orderBuilder(api).forUser(userId).withCodeForTests(orderCode).withActiveSince(activeSince)
                    .withActiveUntil(activeUntil).withCurrency(CURRENCY_AUD).withEffectiveDate(activeSince)
                    .withBillingTypeId(billingTypeId).withPeriod(orderPeriodId).withProrate(prorate).withOrderLines(lines)
                    .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID).withPlanId(planId).build(assets);

        });
        return testBuilder.getTestEnvironment().idForCode(orderCode);
    }

    protected Integer createOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate,
            Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, String userCode, Integer billingType) {
        Integer createdOrder = createOrder(code, activeSince, activeUntil, orderPeriodId, prorate, productQuantityMap, productAssetMap,
                userCode);
        OrderWS order = api.getOrder(createdOrder);
        order.setBillingTypeId(billingType);
        api.updateOrder(order, null);
        return createdOrder;
    }

    protected Map<Integer, BigDecimal> buildProductQuantityEntry(Integer productId, BigDecimal quantity) {
        return Collections.singletonMap(productId, quantity);
    }

    protected void setPlanLevelMetaField(Integer planId, String name) {
        logger.debug("setting the plan level metafields for plan {}", planId);
        PlanWS plan = api.getPlanWS(planId);
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
        Arrays.asList(plan.getMetaFields()).forEach(mf -> {
            if (mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                mf.setValue(name);
                values.add(mf);
            }
        });
        values.forEach(value -> value.setEntityId(api.getCallerCompanyId()));
        plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updatePlan(plan);
    }

    protected void setPlanLevelMetaFieldForInternet(Integer planId, String it, String qru) {
        logger.debug("setting the plan level metafields for plan {}", planId);
        PlanWS plan = api.getPlanWS(planId);
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
        Arrays.asList(plan.getMetaFields()).forEach(mf -> {
            if (mf.getFieldName().equals(PLAN_LEVEL_MF_NAME_FOR_QUANTITY_RESOLUTION_UNIT)) {
                mf.setValue(qru);
                values.add(mf);
            } else if (mf.getFieldName().equals(PLAN_LEVEL_MF_NAME_FOR_INTERNET_TECHNOLOGY_TYPE)) {
                mf.setValue(it);
                values.add(mf);
            }
        });
        values.forEach(value -> value.setEntityId(api.getCallerCompanyId()));
        plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updatePlan(plan);
    }

    protected Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name,
            Integer... paymentMethodTypeId) {
        Map<String, DataType> fieldsSPCCustomerDetails = new Hashtable();
        fieldsSPCCustomerDetails.put("PO Box", DataType.STRING);
        fieldsSPCCustomerDetails.put("Country", DataType.STRING);
        fieldsSPCCustomerDetails.put("Post Code", DataType.STRING);
        fieldsSPCCustomerDetails.put("State", DataType.STRING);
        fieldsSPCCustomerDetails.put("Street Name", DataType.STRING);
        fieldsSPCCustomerDetails.put("City", DataType.STRING);
        fieldsSPCCustomerDetails.put("Street Number", DataType.STRING);
        fieldsSPCCustomerDetails.put("Title", DataType.STRING);
        fieldsSPCCustomerDetails.put("First Name", DataType.STRING);
        fieldsSPCCustomerDetails.put("Last Name", DataType.STRING);
        fieldsSPCCustomerDetails.put("Business Name", DataType.STRING);
        fieldsSPCCustomerDetails.put("Date of Birth", DataType.STRING);
        fieldsSPCCustomerDetails.put("Email Address", DataType.STRING);
        fieldsSPCCustomerDetails.put("Contact Number", DataType.STRING);
        fieldsSPCCustomerDetails.put("direct_marketing", DataType.ENUMERATION);

        Map<String, DataType> fieldsSPCBillingAddress = new Hashtable();
        fieldsSPCBillingAddress.put("PO Box", DataType.STRING);
        fieldsSPCBillingAddress.put("Country", DataType.STRING);
        fieldsSPCBillingAddress.put("Sub Premises", DataType.STRING);
        fieldsSPCBillingAddress.put("Street Number", DataType.STRING);
        fieldsSPCBillingAddress.put("Street Name", DataType.STRING);
        fieldsSPCBillingAddress.put("Street Type", DataType.STRING);
        fieldsSPCBillingAddress.put("City", DataType.STRING);
        fieldsSPCBillingAddress.put("State", DataType.ENUMERATION);
        fieldsSPCBillingAddress.put("Post Code", DataType.STRING);

        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api).withName(name).withPaymentMethodTypeIds(paymentMethodTypeId)
                .useExactDescription(true).addAccountInformationType(CUSTOMER_DETAILS, fieldsSPCCustomerDetails)
                .addAccountInformationType(BILLING_ADDRESS, fieldsSPCBillingAddress).build();

        logger.debug("accountTypeWS.getId() :: {}", accountTypeWS.getId());
        return accountTypeWS.getId();
    }

    protected Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global,
            ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api).itemType().withCode(code).withCategoryType(categoryType).global(global).allowAssetManagement(1)
                .build();
    }

    protected Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global,
            ItemBuilder.CategoryType categoryType, Integer allowAssetManagement, Boolean useExactCode) {
        return envBuilder.itemBuilder(api).itemType().withCode(code).withCategoryType(categoryType).global(global)
                .allowAssetManagement(allowAssetManagement).useExactCode(useExactCode).build();
    }

    protected Integer buildAndPersistCategoryWithAssetMetaFields(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, ItemBuilder.CategoryType categoryType, Integer allowAssetManagement, Boolean useExactCode,
            Set<MetaFieldWS> assetMetaFields) {
        return envBuilder.itemBuilder(api).itemType().withCode(code).withCategoryType(categoryType).global(global)
                .allowAssetManagement(allowAssetManagement).useExactCode(useExactCode).withAssetMetaFields(assetMetaFields).build();
    }

    protected AssetStatusDTOEx createAssetStatus(String description, boolean isAvailable, boolean isDefault, boolean isOrderSaved,
            boolean isReserved, boolean isPending, boolean isActive, boolean isOrderFinished) {

        AssetStatusDTOEx assetStatusDTOEx = new AssetStatusDTOEx();
        assetStatusDTOEx.setDescription(description);
        assetStatusDTOEx.setIsAvailable(isAvailable ? Integer.valueOf(1) : Integer.valueOf(0));
        assetStatusDTOEx.setIsDefault(isDefault ? Integer.valueOf(1) : Integer.valueOf(0));
        assetStatusDTOEx.setIsOrderSaved(isOrderSaved ? Integer.valueOf(1) : Integer.valueOf(0));
        assetStatusDTOEx.setIsActive(isActive ? Integer.valueOf(1) : Integer.valueOf(0));
        assetStatusDTOEx.setIsPending(isPending ? Integer.valueOf(1) : Integer.valueOf(0));
        assetStatusDTOEx.setIsOrderFinished(isOrderFinished ? Integer.valueOf(1) : Integer.valueOf(0));
        return assetStatusDTOEx;
    }

    protected Set<MetaFieldWS> createAssetMetaField() {
        Set<MetaFieldWS> metaFields = new HashSet<MetaFieldWS>();
        logger.debug("Creating metafield!");
        // First Meta Filed
        MetaFieldWS metaField = new MetaFieldWS();
        metaField.setDataType(DataType.STRING);
        metaField.setName(MF_NAME_SERVICE_ID);
        metaField.setDisabled(false);
        metaField.setDisplayOrder(1);
        metaField.setEntityId(PRANCING_PONY);
        metaField.setEntityType(EntityType.ASSET);
        metaField.setMandatory(false);
        metaField.setPrimary(false);
        api.createMetaField(metaField);
        metaFields.add(metaField);
        return metaFields;
    }

    private void insertCustomerCareNumbers(String[] record) {
        logger.debug("inserting the tax rate details to table!");
        try {
            jdbcTemplate.update(INSERT_QUERY_TEMPLATE, new Object[] { record[0] });
        } catch (Exception ex) {
            logger.error("Error !", ex);
            fail("Failed Insertion In data table " + CUSTOMER_CARE__TABLE_NAME, ex);
        }
    }

    protected Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value = new MetaFieldBuilder().name(name).dataType(dataType).entityType(entityType).primary(true).build();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
    }

    protected Integer buildAndPersistMediationConfiguration(TestEnvironmentBuilder envBuilder, JbillingAPI api, String configName,
            String jobLauncherName) {
        return envBuilder.mediationConfigBuilder(api).withName(configName).withLauncher(jobLauncherName)
                .withLocalInputDirectory(com.sapienter.jbilling.common.Util.getSysProp(BASE_DIR) + "spc-mediation-test").build();
    }

    protected Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {
        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
        for (MediationConfigurationWS mediationConfigurationWS : allMediationConfigurations) {
            if (null != mediationConfigurationWS.getMediationJobLauncher()
                    && (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                return mediationConfigurationWS.getId();
            }
        }
        return null;
    }

    private void buildAndPersistRatingUnit() {
        String ratingUnitName = "SPC Rating Unit";
        if (!isRatingUnitPresent(ratingUnitName)) {
            RatingUnitWS ratingUnitWS = new RatingUnitWS();
            ratingUnitWS.setName(ratingUnitName);
            ratingUnitWS.setPriceUnitName("Minute");
            ratingUnitWS.setIncrementUnitName("Seconds");
            ratingUnitWS.setIncrementUnitQuantity("1");
            spcRatingUnitId = api.createRatingUnit(ratingUnitWS);
        }
    }

    private void buildAndPersistDataRatingUnit(String ratingUnitName, String priceUnitName, String incrementUnitName,
            String incrementUnitQuantity) {
        if (!isRatingUnitPresent(ratingUnitName)) {
            RatingUnitWS ratingUnitWS = new RatingUnitWS();
            ratingUnitWS.setName(ratingUnitName);
            ratingUnitWS.setPriceUnitName(priceUnitName);
            ratingUnitWS.setIncrementUnitName(incrementUnitName);
            ratingUnitWS.setIncrementUnitQuantity(incrementUnitQuantity);
            spcDataUnitId.put(ratingUnitName, api.createRatingUnit(ratingUnitWS));
        }
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void pauseUntilMediationCompletes(long seconds, JbillingAPI api) {
        sleep(3000L); // initial wait.
        for (int i = 0; i < seconds; i++) {
            if (!api.isMediationProcessRunning()) {
                return;
            }
            sleep(1000L);
        }
        throw new RuntimeException("Mediation startup wait was timeout in " + seconds);
    }

    protected void waitFor(long seconds) {
        logger.debug("wait for {}", seconds);
        for (int i = 0; i < seconds; i++) {
            logger.debug("...{}", i);
            sleep(1000L);
        }
        logger.debug("wait was timedout in {}", seconds);
    }

    protected Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber) {
        return buildAndPersistAsset(envBuilder, categoryId, itemId, phoneNumber, phoneNumber);
    }

    protected Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId, String phoneNumber,
            String code) {
        ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
        Integer assetStatusId = itemTypeWS
                .getAssetStatuses()
                .stream()
                .filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription().equals("Available"))
                .collect(Collectors.toList()).get(0).getId();
        return envBuilder.assetBuilder(api).withItemId(itemId).withAssetStatusId(assetStatusId).global(true).withIdentifier(phoneNumber)
                .withCode(code).build();
    }

    protected Integer buildAndPersistAssetWithServiceId(TestEnvironmentBuilder envBuilder, Integer categoryId, Integer itemId,
            String phoneNumber, String code, String serviceId) {
        ItemTypeWS itemTypeWS = api.getItemCategoryById(categoryId);
        Integer assetStatusId = itemTypeWS
                .getAssetStatuses()
                .stream()
                .filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription().equals("Available"))
                .collect(Collectors.toList()).get(0).getId();

        MetaFieldValueWS value = new MetaFieldValueWS(SPCConstants.SERVICE_ID, null, DataType.STRING, false, phoneNumber);
        value.setStringValue(serviceId);

        List<MetaFieldValueWS> metaFieldValueWS = new ArrayList();
        metaFieldValueWS.add(value);

        return envBuilder.assetBuilder(api).withItemId(itemId).withAssetStatusId(assetStatusId).global(true)
                .withMetafields(metaFieldValueWS).withIdentifier(phoneNumber).withCode(code).build();
    }

    protected PlanItemWS buildPlanItem(Integer itemId, Integer periodId, String quantity, PriceModelWS price, Date pricingDate) {
        return PlanBuilder.PlanItemBuilder.getBuilder().withItemId(itemId)
                .addModel(null != pricingDate ? pricingDate : com.sapienter.jbilling.server.util.Util.getEpochDate(), price)
                .withBundledPeriodId(periodId).withBundledQuantity(quantity).build();
    }

    protected Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String description,
            boolean global, Integer categoryId, String flatPrice, boolean allowDecimal, Integer allowAssets, boolean isPlan) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withType(categoryId).withFlatPrice(flatPrice)
                .global(global).useExactCode(true).allowDecimal(allowDecimal).withAssetManagementEnabled(allowAssets)
                .withMetaField(PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue())
                .build();
    }

    protected Integer buildAndPersistFlatProductWithOrderLineMetaField(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            String description, boolean global, Integer categoryId, String flatPrice, boolean allowDecimal, Integer allowAssets,
            boolean isPlan, MetaFieldWS... orderLineMetaFields) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withType(categoryId).withFlatPrice(flatPrice)
                .global(global).useExactCode(true).allowDecimal(allowDecimal).withAssetManagementEnabled(allowAssets)
                .withMetaField(PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue())
                .withOrderLineMetaFields(Arrays.asList(orderLineMetaFields)).build();
    }

    protected Integer buildAndPersistFlatProductForMultipleCategories(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            String description, boolean global, List<Integer> categoryIds, String flatPrice, boolean allowDecimal, Integer allowAssets,
            boolean isPlan) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withTypes(categoryIds)
                .withFlatPrice(flatPrice).global(global).useExactCode(true).allowDecimal(allowDecimal)
                .withAssetManagementEnabled(allowAssets)
                .withMetaField(PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue())
                .build();
    }
    
    protected Integer buildAndPersistFlatProductForMultipleCategoriesWithMetaField(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            String description, boolean global, List<Integer> categoryIds, String flatPrice, boolean allowDecimal, Integer allowAssets,
            boolean isPlan, String metaFieldName, Object MetaFieldValue) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withTypes(categoryIds)
                .withFlatPrice(flatPrice).global(global).useExactCode(true).allowDecimal(allowDecimal).withMetaField(metaFieldName, MetaFieldValue)
                .withAssetManagementEnabled(allowAssets).build();
    }

    protected Integer buildAndPersistFlatProductWithRating(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            String description, boolean global, List<Integer> categoryIds, String flatPrice, boolean allowDecimal, Integer allowAssets,
            boolean isPlan, Date ratingDate, RatingConfigurationWS ratingConfiguration) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withTypes(categoryIds)
                .withFlatPrice(flatPrice).global(global).useExactCode(true).allowDecimal(allowDecimal)
                .withAssetManagementEnabled(allowAssets).addRatingConfigurationWithDate(ratingDate, ratingConfiguration)
                .withMetaField(PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue())
                .build();
    }

    protected static MatchingFieldWS getMatchingField(String description, String orderSequence, String mediationField,
            String matchingField, Integer routeId, Integer routeRateCardId) throws SessionInternalError {
        MatchingFieldWS matchingFieldWS = new MatchingFieldWS();
        matchingFieldWS.setDescription(description);
        matchingFieldWS.setOrderSequence(orderSequence);
        matchingFieldWS.setMediationField(mediationField);
        matchingFieldWS.setMatchingField(matchingField);
        matchingFieldWS.setRequired(Boolean.TRUE);
        matchingFieldWS.setType(MatchingFieldType.EXACT.toString());
        matchingFieldWS.setRouteId(routeId);
        matchingFieldWS.setRouteRateCardId(routeRateCardId);
        matchingFieldWS.setMandatoryFieldsQuery("obsoleted");
        return matchingFieldWS;
    }

    /**
     * Creates file in temp directory with header and lines.
     *
     * @param fileName
     * @param fileExtension
     * @param header
     * @param lines
     * @return
     */
    protected String createFileWithData(String fileName, String fileExtension, String header, List<String> lines) {
        if (CollectionUtils.isEmpty(lines)) {
            throw new IllegalArgumentException("Please proives lines");
        }
        if (!fileExtension.startsWith(".")) {
            fileExtension = "." + fileExtension;
        }
        File file = new File(TEMP_DIR_PATH, fileName + System.currentTimeMillis() + fileExtension);
        if (file.exists()) {
            file.delete();
        }
        try {
            if (!file.createNewFile()) {
                throw new RuntimeException("File " + fileName + "Creation failed!");
            }
            if (StringUtils.isNotEmpty(header)) {
                Files.write(file.toPath(), (header + System.lineSeparator()).getBytes());
            }
            String lineContent = lines.stream().collect(Collectors.joining(System.lineSeparator()));
            Files.write(file.toPath(), lineContent.concat(System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);

            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Error in createFileWithData", e);
        }
    }

    protected Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc, Integer periodId,
            Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code).withDescription(desc).withPeriodId(periodId).withItemId(itemId)
                .withUsagePoolsIds(usagePools).withPlanItems(Arrays.asList(planItems)).build().getId();
    }

    protected void setCompanyLevelMetaField(TestEnvironment environment, String fieldName, Object fieldValue) {
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));
        values.add(new MetaFieldValueWS(fieldName, null, DataType.STRING, true, fieldValue));
        int entityId = api.getCallerCompanyId();
        // logger.debug("Created Company Level MetaFields {}", values);
        values.forEach(value -> {
            value.setEntityId(entityId);
        });
        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updateCompany(company);

    }
    protected Integer buildAndPersistFlatProductForMultipleCategoriesWithOrderLineMetaField(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            String description, boolean global, List<Integer> categoryIds, String flatPrice, boolean allowDecimal, Integer allowAssets,
            boolean isPlan, MetaFieldWS... orderLineMetaFields) {
        return envBuilder.itemBuilder(api).item().withCode(code).withDescription(description).withTypes(categoryIds)
                .withFlatPrice(flatPrice).global(global).useExactCode(true).allowDecimal(allowDecimal)
                .withAssetManagementEnabled(allowAssets)
                .withMetaField(PLAN_METAFIELD_TAX_SCHEME_NAME, ENUM_TAX_SCHEME_VALUES.get(0).getValue())
                .withOrderLineMetaFields(Arrays.asList(orderLineMetaFields))
                .build();
    }

    /**
     * @param ratingUnitName
     * @return
     */
    private boolean isRatingUnitPresent(String ratingUnitName) {
        boolean isRatingUnitPresent = false;
        for (RatingUnitWS ratingUnit : api.getAllRatingUnits()) {
            if (ratingUnit.getName().equalsIgnoreCase(ratingUnitName)) {
                if (ratingUnit.getName().contains(DATA_RATING_UNIT_NAME)) {
                    spcDataUnitId.put(ratingUnitName, ratingUnit.getId());
                } else {
                    spcRatingUnitId = ratingUnit.getId();
                }
                isRatingUnitPresent = true;
                break;
            }
        }
        return isRatingUnitPresent;
    }

    /**
     * @param entityType
     * @param metaFieldName
     * @return
     */
    protected boolean isMetaFieldPresent(EntityType entityType, String metaFieldName) {
        boolean companyLevelMFfound = false;
        for (MetaFieldWS mfws : api.getMetaFieldsForEntity(entityType.toString())) {
            if (mfws.getName().equalsIgnoreCase(metaFieldName)) {
                logger.debug("Metafield with name {} already present.", mfws.getName());
                companyLevelMFfound = true;
                break;
            }
        }
        return companyLevelMFfound;
    }

    protected void updateExistingPlugin(JbillingAPI api, Integer pluginId, String className, Map<String, String> params) {
        PluggableTaskWS plugin = api.getPluginWS(pluginId);
        if (null == plugin) {
            Assert.notNull(plugin, " no plugin found for id " + pluginId + " for entity " + api.getCallerCompanyId());
        }
        plugin.setTypeId(api.getPluginTypeWSByClassName(className).getId());
        Hashtable<String, String> parameters = new Hashtable<>();
        if (MapUtils.isNotEmpty(params)) {
            parameters.putAll(params);
        }
        plugin.setParameters(parameters);
        api.updatePlugin(plugin);
    }

    protected void createMediationEvaluationStrategyPlugin() {
        String className = SPCMediationEvaluationStrategyTask.class.getName();
        if (ArrayUtils.isNotEmpty(api.getPluginsWS(api.getCallerCompanyId(), className))) {
            return;
        }
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setProcessingOrder(1000);
        PluggableTaskTypeWS pluginType = api.getPluginTypeWSByClassName(className);
        plugin.setTypeId(pluginType.getId());
        mediationEvaluationStrategyPluginId = api.createPlugin(plugin);
    }

    private void dropTable(String tableName) {
        logger.debug("droping the table {}", tableName);
        jdbcTemplate.execute("DROP TABLE " + tableName);
    }

    private void createTable(String tableName, Map<String, String> columnDetails) {
        logger.debug("creating the table {}", tableName);
        try {
            String createTableQuery = "CREATE TABLE IF NOT  EXISTS  " + tableName;
            StringBuilder columnBuilder = new StringBuilder().append(" (");

            columnBuilder.append(columnDetails.entrySet().stream().map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(Collectors.joining(",")));
            columnBuilder.append(" )");
            jdbcTemplate.execute(createTableQuery + columnBuilder.toString());
        } catch (Exception ex) {
            logger.error("Error !", ex);
            fail("Failed During table creation ", ex);
        }
    }
    
    protected void buildAndPersistEmailHoliDayDataTableRecord(String day, String date) {
        RouteRecordWS dataRecord = new RouteRecordWS();
        List<NameValueString> list = new ArrayList<NameValueString>();

        list.add(createNameValueString("day", day));
        list.add(createNameValueString("holiday_date", date));

        NameValueString[] intArray = new NameValueString[list.size()];
        dataRecord.setAttributes(list.toArray(intArray));
        dataRecord.setId(null);

        api.createDataTableRecord(dataRecord, PARAM_EMAIL_HOLIDAY_TABLE_NAME);
    }

    protected void buildAndPersistCreditPoolDataTableRecord(String planId, String tariffCodesNote, String consumptionPercentages,
            String creditItemId, String freeAmount, String creditPoolName) {
        RouteRecordWS dataRecord = new RouteRecordWS();
        List<NameValueString> list = new ArrayList<NameValueString>();

        list.add(createNameValueString("plan_id", planId));
        list.add(createNameValueString("tariff_codes_note", tariffCodesNote));
        list.add(createNameValueString("consumption_percentages", consumptionPercentages));
        list.add(createNameValueString("credit_item_id", creditItemId));
        list.add(createNameValueString("free_amount", freeAmount));
        list.add(createNameValueString("credit_pool_name", creditPoolName));

        NameValueString[] intArray = new NameValueString[list.size()];
        dataRecord.setAttributes(list.toArray(intArray));
        dataRecord.setId(null);

        api.createDataTableRecord(dataRecord, PARAM_CREDIT_POOL_TABLE_NAME);
    }

    protected void buildAndPersistTaxSchemeDataTableRecord(String description, String taxCode, String startDate, String endDate,
            String taxRate) {
        RouteRecordWS dataRecord = new RouteRecordWS();
        List<NameValueString> list = new ArrayList<NameValueString>();

        list.add(createNameValueString("description", description));
        list.add(createNameValueString("tax_code", taxCode));
        list.add(createNameValueString("start_date", startDate));
        list.add(createNameValueString("end_date", endDate));
        list.add(createNameValueString("tax_rate", taxRate));

        NameValueString[] intArray = new NameValueString[list.size()];
        dataRecord.setAttributes(list.toArray(intArray));
        dataRecord.setId(null);

        api.createDataTableRecord(dataRecord, PARAM_TAX_TABLE_NAME);
    }

    protected void buildAndPersistPlanBasedFreeCallInfoDataTableRecord(String planCode, String freeCallCount, String itemCodes) {
        RouteRecordWS dataRecord = new RouteRecordWS();
        List<NameValueString> list = new ArrayList<NameValueString>();

        list.add(createNameValueString("plan_code", planCode));
        list.add(createNameValueString("free_call_count", freeCallCount));
        list.add(createNameValueString("item_codes", itemCodes));

        NameValueString[] intArray = new NameValueString[list.size()];
        dataRecord.setAttributes(list.toArray(intArray));
        dataRecord.setId(null);

        api.createDataTableRecord(dataRecord, DATA_TABLE_PLAN_BASED_FREE_CALL_INFO);
    }

    protected void buildAndPersistAlertsDataTableRecord(String jbPlanno, String jbPlanDescription, String trigger1, String smsMessage1,
            String trigger2, String smsMessage2, String trigger3, String smsMessage3, String trigger4, String smsMessage4, String trigger5,
            String smsMessage5, String trigger6, String smsMessage6, String autobarOption6, String tableName) {
        RouteRecordWS dataRecord = new RouteRecordWS();
        List<NameValueString> list = new ArrayList<NameValueString>();

        list.add(createNameValueString("jb_planno", jbPlanno));
        list.add(createNameValueString("jb_plan_description", jbPlanDescription));
        list.add(createNameValueString("trigger1", trigger1));
        list.add(createNameValueString("sms_message1", smsMessage1));
        list.add(createNameValueString("trigger2", trigger2));
        list.add(createNameValueString("sms_message2", smsMessage2));
        list.add(createNameValueString("trigger3", trigger3));
        list.add(createNameValueString("sms_message3", smsMessage3));
        list.add(createNameValueString("trigger4", trigger4));
        list.add(createNameValueString("sms_message4", smsMessage4));
        list.add(createNameValueString("trigger5", trigger5));
        list.add(createNameValueString("sms_message5", smsMessage5));
        list.add(createNameValueString("trigger6", trigger6));
        list.add(createNameValueString("sms_message6", smsMessage6));
        list.add(createNameValueString("autobar_option6", autobarOption6));

        NameValueString[] intArray = new NameValueString[list.size()];
        dataRecord.setAttributes(list.toArray(intArray));
        dataRecord.setId(null);

        api.createDataTableRecord(dataRecord, tableName);
    }

    private NameValueString createNameValueString(String name, String value) {
        NameValueString nmv = new NameValueString();
        nmv.setName(name);
        nmv.setValue(value);
        return nmv;
    }

    protected void buildAndPersistCallToZeroDataTableRecord(String calltozero, String tableName) {
        RouteRecordWS dataRecord = new RouteRecordWS();
        List<NameValueString> list = new ArrayList<NameValueString>();

        list.add(createNameValueString("calltozero", calltozero));

        NameValueString[] intArray = new NameValueString[list.size()];
        dataRecord.setAttributes(list.toArray(intArray));
        dataRecord.setId(null);

        api.createDataTableRecord(dataRecord, tableName);
    }

    protected Integer createRateCard(String rateCardName, String rateCardHeader, List<String> rateRecords) {
        RouteRateCardWS routeRateCardWS = new RouteRateCardWS();
        routeRateCardWS.setName(rateCardName);
        routeRateCardWS.setRatingUnitId(spcRatingUnitId);
        routeRateCardWS.setEntityId(api.getCallerCompanyId());

        String optusMobileRouteRateCardFilePath = createFileWithData(rateCardName, ".csv", rateCardHeader, rateRecords);
        logger.debug("Route Rate card file path {}", optusMobileRouteRateCardFilePath);
        Integer rateCardId = api.createRouteRateCard(routeRateCardWS, new File(optusMobileRouteRateCardFilePath));
        logger.debug("Route Rate Card id {}", rateCardId);
        Integer matchingFieldId = api.createMatchingField(getMatchingField(CODE_STRING, "1", CODE_STRING, ROUTE_ID, null, rateCardId));
        logger.debug("Matching Field id {}", matchingFieldId);

        // Creating Plan Rating Enumeration
        EnumerationValueWS valueWS = new EnumerationValueWS();
        valueWS.setValue(rateCardName);
        if (api.getEnumerationByName(ENUMERATION_METAFIELD_NAME) == null) {
            EnumerationWS enumeration = new EnumerationWS();
            enumeration.setEntityId(api.getCallerCompanyId());
            enumeration.setName(ENUMERATION_METAFIELD_NAME);
            enumeration.setValues(Arrays.asList(valueWS));
            planRatingEnumId = api.createUpdateEnumeration(enumeration);
        }
        return rateCardId;
    }

    private Integer buildAndPersistBPAYPaymentMethod(TestEnvironmentBuilder envCreator) {
        return envCreator
                .paymentMethodTypeBuilder(api, PAYMENT_TYPE_CODE_BPAY)
                .withMethodName(PAYMENT_TYPE_CODE_BPAY)
                .withOwningEntityId(api.getCallerCompanyId())
                .withTemplateId(8)
                .isRecurring(true)
                .allAccountType(true)
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule(BILLER_CODE, DataType.INTEGER, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), null, null))
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule(REFERENCE_NUMBER, DataType.INTEGER, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), null, null)).build().getId();
    }

    private Integer buildAndPersistAustraliaPostPaymentMethod(TestEnvironmentBuilder envCreator) {
        return envCreator
                .paymentMethodTypeBuilder(api, PAYMENT_TYPE_CODE_AUSTRALIA_POST)
                .withMethodName(PAYMENT_TYPE_CODE_AUSTRALIA_POST)
                .withOwningEntityId(api.getCallerCompanyId())
                .withTemplateId(-1)
                .isRecurring(true)
                .allAccountType(true)
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule(BILLER_CODE, DataType.INTEGER, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), null, null))
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule(REFERENCE_NUMBER, DataType.INTEGER, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), null, null)).build().getId();
    }

    private Integer buildAndPersistBankDebitPaymentMethod(TestEnvironmentBuilder envCreator) {
        return envCreator
                .paymentMethodTypeBuilder(api, PAYMENT_TYPE_CODE_BANK_DEBIT)
                .withMethodName(PAYMENT_TYPE_CODE_BANK_DEBIT)
                .withOwningEntityId(api.getCallerCompanyId())
                .withTemplateId(10)
                .isRecurring(true)
                .allAccountType(true)
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule("Bank Name", DataType.STRING, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), MetaFieldType.BANK_NAME, null))
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule("Account Name", DataType.STRING, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), MetaFieldType.INITIAL, null))
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule("Account Number", DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), MetaFieldType.BANK_ACCOUNT_NUMBER, null))
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule("BSB", DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), MetaFieldType.BANK_ROUTING_NUMBER, null))
                .addMetaField(
                        ApiBuilderHelper.getMetaFieldWithValidationRule("ach.gateway.key", DataType.CHAR, EntityType.PAYMENT_METHOD_TYPE,
                                api.getCallerCompanyId(), MetaFieldType.GATEWAY_KEY, null)).build().getId();
    }

    protected UserWS getSPCTestUserWS(TestEnvironmentBuilder envBuilder, String userName, Date nextInvoiceDate, String invoiceDesign,
                                      String customerType, String... instruments) {
        return getSPCTestUserWS(envBuilder, userName, nextInvoiceDate, invoiceDesign, customerType, true, instruments );
    }

    protected UserWS getSPCTestUserWS(TestEnvironmentBuilder envBuilder, String userName, Date nextInvoiceDate, String invoiceDesign,
            String customerType,boolean updateNID, String... instruments) {
        Map<String, Object> fieldsSPCCustomerDetails = new Hashtable();
        fieldsSPCCustomerDetails.put("PO Box", "1234");
        fieldsSPCCustomerDetails.put("Country", "AU");
        fieldsSPCCustomerDetails.put("Post Code", "41100");
        fieldsSPCCustomerDetails.put("State", "WA");
        fieldsSPCCustomerDetails.put("Street Name", "Test Street");
        fieldsSPCCustomerDetails.put("City", "TestCity");
        fieldsSPCCustomerDetails.put("Street Number", "11");
        fieldsSPCCustomerDetails.put("Title", "Mr");
        fieldsSPCCustomerDetails.put("First Name", "Test");
        fieldsSPCCustomerDetails.put("Last Name", "Customer");
        fieldsSPCCustomerDetails.put("Business Name", "TATA");
        fieldsSPCCustomerDetails.put("Date of Birth", "30-11-1989");
        fieldsSPCCustomerDetails.put("Email Address", "test@test.com");
        fieldsSPCCustomerDetails.put("Contact Number", "9595959595");
        fieldsSPCCustomerDetails.put("direct_marketing", "Yes");

        Map<String, Object> fieldsSPCBillingAddress = new Hashtable();
        fieldsSPCBillingAddress.put("PO Box", "12356");
        fieldsSPCBillingAddress.put("Country", "AU");
        fieldsSPCBillingAddress.put("Sub Premises", "TestSub");
        fieldsSPCBillingAddress.put("Street Number", "12335");
        fieldsSPCBillingAddress.put("Street Name", "Old Street");
        fieldsSPCBillingAddress.put("Street Type", "TestType");
        fieldsSPCBillingAddress.put("City", "Sydney");
        fieldsSPCBillingAddress.put("State", "WA");
        fieldsSPCBillingAddress.put("Post Code", "1231");

        UserWS newUser = envBuilder.customerBuilder(api).withUsername(userName)

        .withAccountTypeId(getAccountIdForName(envBuilder.env(), ACCOUNT_NAME)).addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate).withCurrency(CURRENCY_AUD)
                .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, getDayOfMonth(nextInvoiceDate)))
                .withPaymentInstruments(getInstruments(instruments)).withMetaField(ENUMERATION_CUSTOMER_TYPE, customerType)
                .withMetaField(CRM_ACCOUNT_NUMBER, "ACC" + random.nextInt(10000)).withAITGroup(BILLING_ADDRESS, fieldsSPCBillingAddress)
                .withAITGroup(CUSTOMER_DETAILS, fieldsSPCCustomerDetails)
                .withInvoiceDesign(StringUtils.isNotEmpty(invoiceDesign) ? invoiceDesign : "").build();
        if(updateNID) {
            newUser.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(newUser);
        }
        newUser = api.getUserWS(newUser.getId());
        return newUser;
    }

    public List<PaymentInformationWS> getInstruments(String[] instruments) {

        List<PaymentInformationWS> informationWSs = new ArrayList();

        for (String instrument : instruments) {

            if (instrument.equals(CC)) {
                informationWSs.add(createCCInstrument());
            }

            if (instrument.equals(BPAY)) {
                informationWSs.add(createBPAYInstrument());
            }

            if (instrument.equals(BANK_DEBIT)) {
                informationWSs.add(createBankDebitInstrument());
            }

            if (instrument.equals(AUSTRALIA_POST)) {
                informationWSs.add(createAusPostInstrument());
            }
        }

        return informationWSs;

    }

    private PaymentInformationWS createAusPostInstrument() {
        PaymentInformationWS informationWSBPAY = new PaymentInformationWS();
        informationWSBPAY.setPaymentMethodTypeId(getPaymentMethodId(apostPaymentMethodId, PAYMENT_TYPE_CODE_AUSTRALIA_POST));
        informationWSBPAY.setProcessingOrder(Integer.valueOf(6));
        List<MetaFieldValueWS> fieldValueListWBD = new ArrayList<>();

        MetaFieldValueWS metaFieldValueWS11 = new MetaFieldValueWS();
        metaFieldValueWS11.setFieldName(REFERENCE_NUMBER);
        metaFieldValueWS11.setIntegerValue(326598);
        fieldValueListWBD.add(metaFieldValueWS11);
        MetaFieldValueWS metaFieldValueWS21 = new MetaFieldValueWS();
        metaFieldValueWS21.setFieldName(BILLER_CODE);
        metaFieldValueWS21.setIntegerValue(1234567);
        fieldValueListWBD.add(metaFieldValueWS21);

        informationWSBPAY.setMetaFields(fieldValueListWBD.toArray(new MetaFieldValueWS[0]));
        return informationWSBPAY;
    }

    private PaymentInformationWS createBankDebitInstrument() {

        PaymentInformationWS informationWSBD = new PaymentInformationWS();
        informationWSBD.setPaymentMethodTypeId(getPaymentMethodId(bankDebitPaymentMethodId, PAYMENT_TYPE_CODE_BANK_DEBIT));
        informationWSBD.setProcessingOrder(Integer.valueOf(3));
        List<MetaFieldValueWS> fieldValueListWBD = new ArrayList<>();

        MetaFieldValueWS metaFieldValueWS11 = new MetaFieldValueWS();
        metaFieldValueWS11.setFieldName(BD_BANK_NAME);
        metaFieldValueWS11.setStringValue("Test Bank");
        fieldValueListWBD.add(metaFieldValueWS11);
        MetaFieldValueWS metaFieldValueWS21 = new MetaFieldValueWS();
        metaFieldValueWS21.setFieldName(BD_ACCOUNT_NUMBER);
        metaFieldValueWS21.setStringValue("1234567");
        fieldValueListWBD.add(metaFieldValueWS21);
        MetaFieldValueWS metaFieldValueWS31 = new MetaFieldValueWS();
        metaFieldValueWS31.setFieldName(BD_BSB);
        metaFieldValueWS31.setStringValue("123-654");
        fieldValueListWBD.add(metaFieldValueWS31);
        MetaFieldValueWS metaFieldValueWS41 = new MetaFieldValueWS();
        metaFieldValueWS41.setFieldName(BD_ACCOUNT_NAME);
        metaFieldValueWS41.setStringValue("Test Account");
        fieldValueListWBD.add(metaFieldValueWS41);
        informationWSBD.setMetaFields(fieldValueListWBD.toArray(new MetaFieldValueWS[0]));
        return informationWSBD;
    }

    private PaymentInformationWS createBPAYInstrument() {
        // bpayPaymentMethodId
        PaymentInformationWS informationWSBPAY = new PaymentInformationWS();
        informationWSBPAY.setPaymentMethodTypeId(getPaymentMethodId(bpayPaymentMethodId, PAYMENT_TYPE_CODE_BPAY));
        informationWSBPAY.setProcessingOrder(Integer.valueOf(9));
        List<MetaFieldValueWS> fieldValueListWBD = new ArrayList<>();

        MetaFieldValueWS metaFieldValueWS11 = new MetaFieldValueWS();
        metaFieldValueWS11.setFieldName(REFERENCE_NUMBER);
        metaFieldValueWS11.setIntegerValue(326598);
        fieldValueListWBD.add(metaFieldValueWS11);
        MetaFieldValueWS metaFieldValueWS21 = new MetaFieldValueWS();
        metaFieldValueWS21.setFieldName(BILLER_CODE);
        metaFieldValueWS21.setIntegerValue(326598);
        fieldValueListWBD.add(metaFieldValueWS21);

        informationWSBPAY.setMetaFields(fieldValueListWBD.toArray(new MetaFieldValueWS[0]));
        return informationWSBPAY;
    }

    private PaymentInformationWS createCCInstrument() {
        PaymentInformationWS informationWSCC = new PaymentInformationWS();
        informationWSCC.setPaymentMethodTypeId(9);
        informationWSCC.setProcessingOrder(Integer.valueOf(1));

        List<MetaFieldValueWS> fieldValueWSs = new ArrayList();
        MetaFieldValueWS metaFieldValueWS1 = new MetaFieldValueWS();
        metaFieldValueWS1.setFieldName(CC_CARDHOLDER_NAME);
        metaFieldValueWS1.setStringValue("Test Customer");

        fieldValueWSs.add(metaFieldValueWS1);
        MetaFieldValueWS metaFieldValueWS2 = new MetaFieldValueWS();
        metaFieldValueWS2.setFieldName(CC_NUMBER);
        metaFieldValueWS2.setStringValue("5163200000000008");

        fieldValueWSs.add(metaFieldValueWS2);
        MetaFieldValueWS metaFieldValueWS3 = new MetaFieldValueWS();
        metaFieldValueWS3.setFieldName(CC_EXPIRY_DATE);
        metaFieldValueWS3.setStringValue("02/2029");

        fieldValueWSs.add(metaFieldValueWS3);

        MetaFieldValueWS metaFieldValueWS5 = new MetaFieldValueWS();
        metaFieldValueWS5.setFieldName(CC_AUTOPAYMENT_AUTHORIZATION);
        metaFieldValueWS5.setBooleanValue(Boolean.TRUE);
        fieldValueWSs.add(metaFieldValueWS5);
        informationWSCC.setMetaFields(fieldValueWSs.toArray(new MetaFieldValueWS[0]));
        return informationWSCC;

    }

    public Integer getDayOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    public Calendar getDate(Integer addMonths, Integer dayOfMonth) {

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, addMonths);
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        return calendar;
    }

    public LocalDate getLocalDate(Integer addMonths, Integer dayOfMonth) {
        return LocalDate.now().plusMonths(addMonths).withDayOfMonth(dayOfMonth);
    }

    protected void updateCurrency(Integer currencyId, String exchangeRate, String systemRate) {
        final CurrencyWS[] currencies = api.getCurrencies();
        final CurrencyWS audCurrency = getCurrencyById(currencyId, currencies);
        final BigDecimal rate = new BigDecimal(exchangeRate);
        audCurrency.setRate(rate);
        audCurrency.setSysRateAsDecimal(rate.setScale(4));
        api.updateCurrency(audCurrency);
    }

    private static CurrencyWS getCurrencyById(Integer currencyId, CurrencyWS[] currencies) {
        for (CurrencyWS currency : currencies) {
            if (currencyId.equals(currency.getId())) {
                return currency;
            }
        }
        throw new IllegalStateException("Currency with id = " + currencyId + " not found.");
    }

    public Date getLocalDateAsDate(LocalDate localDate) {
        return (null != localDate) ? (Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())) : null;
    }

    protected Integer[] createInvoiceWithDateWithUsagePoolEvaluation(UserWS userWs, Date updatedNID, int seconds) {
        Date nextInvoiceDate = userWs.getNextInvoiceDate();
        Integer[] invoices = api.createInvoiceWithDate(userWs.getId(), nextInvoiceDate, null, null, false);
        api.triggerCustomerUsagePoolEvaluation(1, nextInvoiceDate);
        sleep(seconds * 1000);
        userWs.setNextInvoiceDate(updatedNID);
        api.updateUser(userWs);
        return invoices;
    }

    protected void cancelServiceOrder(Integer orderId, Date activeUntil) {
        CancelOrderInfo cancelOrderInfo = new CancelOrderInfo();
        cancelOrderInfo.setOrderId(orderId);
        cancelOrderInfo.setActiveUntil(activeUntil);
        api.cancelServiceOrder(cancelOrderInfo);
    }

    private static final String SERVICE_SUMMARY_BY_INVOICE_ID = "SELECT * FROM service_summary WHERE invoice_id = ?";

    protected List<ServiceSummaryWS> getServiceSummaryByInvoiceId(Integer invoiceId) {
        List<ServiceSummaryWS> serviceSummaryList = new ArrayList();
        try {
            logger.debug("Service summary: invoiceId: {}", invoiceId);
            serviceSummaryList = this.jdbcTemplate
                    .query(SERVICE_SUMMARY_BY_INVOICE_ID, 
                           new ServiceSummaryWSMapper(),
                           invoiceId);

        } catch (Exception ex) {
            logger.error("Error !", ex);
            fail("Failed getServiceSummaryByInvoiceIdAndUserId", ex);
        }
        return serviceSummaryList;
    }

    protected Calendar getDate(Integer addMonths, Integer dayOfMonth, boolean startOfDay) {
        Calendar calendar = getDate(addMonths, dayOfMonth);
        if (startOfDay) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
        }
        return calendar;
    }

    protected Integer getAccountIdForName(TestEnvironment testEnvironment, String accountName) {
        Integer accountTypeId = testEnvironment.idForCode(accountName);
        if (null == accountTypeId) {
            AccountTypeWS[] allAccountTypes = api.getAllAccountTypes();
            logger.debug("SPCBaseConfiguration.getAccountIdForName===accountName===={}", accountName);
            for (AccountTypeWS accountTypeWS : allAccountTypes) {
                String content = accountTypeWS.getDescription(api.getCallerLanguageId()).getContent();
                if (content.equalsIgnoreCase(accountName)) {
                    accountTypeId = accountTypeWS.getId();
                    break;
                }
            }
        }
        logger.debug("SPCBaseConfiguration.getAccountIdForName====account type id ===" + accountTypeId);
        return accountTypeId;
    }

    private Integer getPaymentMethodId(Integer paymentMethodId, String methodName) {
        if (null == paymentMethodId) {
            logger.debug("SPCBaseConfiguration.getPaymentMethodId===methodName=== {}", methodName);
            PaymentMethodTypeWS[] allPaymentMethodTypes = api.getAllPaymentMethodTypes();
            for (PaymentMethodTypeWS paymentMethodTypeWS : allPaymentMethodTypes) {
                if (paymentMethodTypeWS.getMethodName().equalsIgnoreCase(methodName)) {
                    paymentMethodId = paymentMethodTypeWS.getId();
                    break;
                }
            }
        }
        logger.debug("SPCBaseConfiguration.getPaymentMethodId===methodId===={}", paymentMethodId);
        return paymentMethodId;
    }

    protected Integer getCategoryIdByName(TestEnvironment testEnvironment, String categoryName) {
        Integer categoryId = testEnvironment.idForCode(categoryName);
        if (null == categoryId) {
            ItemTypeWS[] categoriesByEntityId = api.getAllItemCategoriesByEntityId(api.getCallerCompanyId());
            logger.debug("SPCBaseConfiguration.getCategoryId====categoryName===={}", categoryName);
            for (ItemTypeWS itemTypeWS : categoriesByEntityId) {
                if (itemTypeWS.getDescription().equalsIgnoreCase(categoryName)) {
                    categoryId = itemTypeWS.getId();
                    break;
                }
            }
        }
        logger.debug("SPCBaseConfiguration.getCategoryId====id====={}", categoryId);
        return categoryId;
    }

    protected Integer getItemIdByCode(TestEnvironment testEnvironment, String code) {
        Integer itemId = testEnvironment.idForCode(code);
        if (null == itemId) {
            logger.debug("SPCBaseConfiguration.getItemIdByCode=====code======{}", code);
            itemId = api.getItemID(code);
        }
        logger.debug("SPCBaseConfiguration.getItemIdByCode====item.id===={}", itemId);
        return itemId;
    }

    protected void clearTestDataForUser(Integer userId) {

        Integer[] allPayments = api.getPaymentsByUserId(userId);
        if (ArrayUtils.isNotEmpty(allPayments)) {
            Arrays.stream(allPayments).sorted(Comparator.reverseOrder()).forEach(api::removeAllPaymentLinks);
        }
        if (ArrayUtils.isNotEmpty(allPayments)) {
            Arrays.stream(allPayments).sorted(Comparator.reverseOrder()).forEach(api::deletePayment);
        }
        Integer[] allInvoices = api.getAllInvoices(userId);
        List<Date> invoiceDates = new ArrayList<>();
        if (ArrayUtils.isNotEmpty(allInvoices)) {
            Arrays.stream(allInvoices).forEach(i -> {
                invoiceDates.add(api.getInvoiceWS(i).getCreateDatetime());
            });
        }

        if (CollectionUtils.isNotEmpty(invoiceDates)) {
            Date invoiceCreationStartDate = Collections.min(invoiceDates);
            Date invoiceCreationEndDate = Collections.max(invoiceDates);

            CreditNoteInvoiceMapWS[] creditNoteInvoiceMap = api.getCreditNoteInvoiceMaps(invoiceCreationStartDate, invoiceCreationEndDate);
            if (ArrayUtils.isNotEmpty(creditNoteInvoiceMap)) {
                for (CreditNoteInvoiceMapWS creditNoteInvoiceMapWS : creditNoteInvoiceMap) {
                    api.removeCreditNoteLink(creditNoteInvoiceMapWS.getInvoiceId(), creditNoteInvoiceMapWS.getCreditNoteId());
                }
            }
            CreditNoteWS[] allCreditNotes = api.getAllCreditNotes(api.getCallerCompanyId());
            if (ArrayUtils.isNotEmpty(allCreditNotes)) {
                for (CreditNoteWS creditNote : allCreditNotes) {
                    if (userId.equals(creditNote.getUserId())) {
                        api.deleteCreditNote(creditNote.getId());
                    }
                }
            }
        }

        if (ArrayUtils.isNotEmpty(allInvoices)) {
            Arrays.stream(allInvoices).sorted(Comparator.reverseOrder()).forEach(api::deleteInvoice);
        }

        Integer[] allOrders = api.getOrdersByDate(userId, getLocalDateAsDate(LocalDate.now(ZoneId.systemDefault()).minusDays(1)),
                new Date());
        if (ArrayUtils.isNotEmpty(allOrders)) {
            Arrays.stream(allOrders).sorted(Comparator.reverseOrder()).forEach(api::deleteOrder);
        }
        api.deleteUser(userId);
    }

    /**
     * Helper method to fetch the created user id
     *
     * @param testEnvironment
     * @param userName
     * @return
     */
    protected Integer getUserIdByUserName(TestEnvironment testEnvironment, String userName) {
        Integer userId = testEnvironment.idForCode(userName);
        return null == userId ? api.getUserId(userName) : userId;
    }

    /**
     * Helper method to check if invoice created
     *
     * @param invoiceId
     * @return
     */
    protected void checkInvoiceAndStatus(Integer invoiceId, Integer invoiceStatusId) {
        InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
        assertNotNull("Invoice is not created.", invoiceWS);
        assertEquals(invoiceWS.getStatusId(), invoiceStatusId);
    }

    protected void validatePlanUsagePools(Integer planId, Integer poolCount, String mainPoolQuatility, String planBoostQuantity) {
        UsagePoolWS[] usagePoolsByPlan;
        usagePoolsByPlan = api.getUsagePoolsByPlanId(planId);
        assertEquals("Number of usage pools from plan must match", poolCount.intValue(), usagePoolsByPlan.length);
        for (UsagePoolWS telstraUsagePoolWS : usagePoolsByPlan) {
            if (telstraUsagePoolWS.getPrecedence().equals(-1)) {
                assertEquals("usage pool quantity from plan must match", mainPoolQuatility, telstraUsagePoolWS.getQuantity());
            } else {
                assertEquals("usage pool bost quantity from plan must match", planBoostQuantity, telstraUsagePoolWS.getQuantity());
            }
        }
    }

    /**
     * Collects order ids from invoice lines for given {@link InvoiceWS}
     *
     * @param invoice
     * @return
     */
    protected Integer[] collectOrdersFromInvoice(final InvoiceWS invoice) {
        return Arrays.stream(invoice.getOrders()).filter(Objects::nonNull).distinct().toArray(Integer[]::new);
    }

    protected Date getLastDayOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int lastDateInt = calendar.getActualMaximum(Calendar.DATE);
        calendar.set(Calendar.DATE, lastDateInt);
        return com.sapienter.jbilling.server.util.Util.getEndOfDay(calendar.getTime());
    }

    protected void validateMediationProcess(MediationProcess mediationProcess, int recordsProcessed,
            int doneAndBillable, int errors, int duplicates,
            Integer configId, int orderIdCount){

        assertNotNull("Mediation process expected!", mediationProcess);
        assertEquals("Invalid number of processed records!", mediationProcess.getRecordsProcessed(), Integer.valueOf(recordsProcessed));
        assertEquals("Invalid number of done and billable records!", mediationProcess.getDoneAndBillable(), Integer.valueOf(doneAndBillable));
        assertEquals("Invalid number of error records!", mediationProcess.getErrors(), Integer.valueOf(errors));
        assertEquals("Invalid number of error records!", mediationProcess.getDuplicates(), Integer.valueOf(duplicates));
        assertEquals("Invalid config id!", mediationProcess.getConfigurationId(), configId);
        assertEquals("Orders are not matched", orderIdCount, mediationProcess.getOrderIds().length);
    }

    protected Integer createProduct(String productCode,Integer category,String price,Boolean global,
            Integer assetmanagment,TestEnvironmentBuilder envBuilder,
            JbillingAPI api, Integer... entityId ){

        return envBuilder.itemBuilder(api)
                .item()
                .withCode(productCode)
                .global(global)
                .withType(category)
                .withEntities(entityId)
                .withAssetManagementEnabled(assetmanagment)
                .withFlatPrice(price)
                .build();
    }

    protected Integer createOrderWithDiscount( String orderCode,Date activeSince, Date activeUntil,
            Integer orderPeriodId,Integer billingTypeId,String description, Integer productId, Integer userId,
            String code, String discountDescription ,Date discountStartDate, String rate,
            String periodUnit, String periodValue, String isPercentage ) {

        this.testBuilder.given(envBuilder -> {
            DiscountWS discountWs =
                    createPeriodBasedAmountDiscount(code , discountDescription,discountStartDate ,
                            rate,periodUnit,periodValue,isPercentage);
            Integer discountId = api.createOrUpdateDiscount(discountWs);

            DiscountLineWS discountLineWS = envBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription(description)
                    .build();

            OrderLineWS parentOrderLine = envBuilder.orderBuilder(api).orderLine()
                    .withItemId(productId)// default quantity 1
                    .build();

            Integer orderId = envBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withPeriod(orderPeriodId)
                    .withOrderLine(parentOrderLine)
                    .withCodeForTests(orderCode)
                    .withActiveSince(activeSince)
                    .withEffectiveDate(activeSince)
                    .withBillingTypeId(billingTypeId)
                    .withDiscountLine(discountLineWS)
                    .build();

        });
        return testBuilder.getTestEnvironment().idForCode(orderCode);
    }

    private DiscountWS createPeriodBasedAmountDiscount(String code, String discountDescription ,Date discountStartDate,
                                                       String rate,String periodUnit, String periodValue, String isPercentage) {
        DiscountWS discountWs = new DiscountWS();
        discountWs.setCode(code);
        discountWs.setDescription(discountDescription);
        discountWs.setRate(rate);
        discountWs.setType(DiscountStrategyType.RECURRING_PERIODBASED.name());

        SortedMap<String, String> attributes = new TreeMap<>();
        attributes.put("periodUnit", periodUnit);    // period unit month
        attributes.put("periodValue", periodValue);
        attributes.put("isPercentage", isPercentage);    // Consider rate as amount
        discountWs.setAttributes(attributes);

        if (discountStartDate != null) {
            discountWs.setStartDate(discountStartDate);
        }

        return discountWs;
    }

    /**
     * Helper method to fetch date in string format
     * @param localDate
     * @param format
     * @return String
     */
    public static String getDateFormatted(LocalDate localDate, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        return dtf.format(localDate);

    }

    private static final String NL = System.getProperty("line.separator");
    private static final String SQL_UPDATE_ASSET_IDENTIFIER = String.join(NL,
            "UPDATE asset",
            "   SET identifier = ?",
            " WHERE identifier = ?");
    protected void updateAssetIdentifier(String assetIdentifier1, String assetIdentifier2 ) {
        logger.debug("updating Asset Identifier");
        try {
            jdbcTemplate.update(SQL_UPDATE_ASSET_IDENTIFIER, assetIdentifier1, assetIdentifier2);
        } catch (Exception ex) {
            logger.error("Error !", ex);
            fail("Failed Updating In table Asset", ex);
        }
    }

    private static final String NO_OF_ASSETS_FOR_IDENTIFIER =
            "SELECT count(*) AS cnt FROM  asset  WHERE identifier = ?";

    protected int getNoOfAssetsForIdentifier(String identifier) {
        List<ServiceSummaryWS> serviceSummaryList = new ArrayList();
        try {
            logger.debug("getNoOfAssetsForIdentifier: identifier: {}", identifier);
            SqlRowSet rs = jdbcTemplate.queryForRowSet(NO_OF_ASSETS_FOR_IDENTIFIER, identifier);

            while (rs.next()) {
                
               return rs.getInt("cnt");
                
            }
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed getNoOfAssetsForIdentifier",  ex);
        }
        return 0;
    }

    private static final String SQL_UPDATE_BILLING_PROCESS_DATE =
            "UPDATE billing_process  SET billing_date = ? WHERE is_review = 0 and id = ?";
    public void updateBillingProcessDate(Date BillingDate , Integer lastBillingProcessId) {
        logger.debug("updating Billing Date");
        try {
            jdbcTemplate.update(SQL_UPDATE_BILLING_PROCESS_DATE, BillingDate,lastBillingProcessId);
        } catch (Exception ex) {
            logger.error("Error !", ex);
            fail("Failed Updating In table Billing Process", ex);
        }
    }

    private static final String SQL_GET_BILLING_PROCESS_DATE =
            "SELECT billing_date from billing_process WHERE is_review = 0 and id = ?";
    public Date getBillingProcessDate(Integer lastBillingProcessId) {
        logger.debug("Getting Billing Date");
        Date date1= null;
        try {
            String dateString = jdbcTemplate.queryForObject(SQL_GET_BILLING_PROCESS_DATE,String.class, lastBillingProcessId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);
            LocalDate date = LocalDate.parse(dateString, formatter);
            return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (Exception ex) {
            logger.error("Error !", ex);
            fail("Failed Updating In table Billing Process", ex);
        }
        return date1;
    }

    protected void updatePreference(Integer preferenceTypeId, String preferenceValue) {
        PreferenceWS preferenceWS =  api.getPreference(preferenceTypeId);
        preferenceWS.setValue(preferenceValue);
        api.updatePreference(preferenceWS);
        logger.debug("Preference type id {} updated to : {}", preferenceTypeId ,preferenceValue);
    }

    protected LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                   .atZone(ZoneId.systemDefault())
                   .toLocalDateTime();
    }

    protected Date getFirstDayOfMonth(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int firstDayInt = calendar.getActualMinimum(Calendar.DATE);
        calendar.set(Calendar.DATE, firstDayInt);
        return com.sapienter.jbilling.server.util.Util.getStartOfDay(calendar.getTime());
    }

    protected void validatePricingFields ( JbillingMediationRecord[] viewEvents ) {
        for ( JbillingMediationRecord jbillingMediationRecord : viewEvents ) {
            String pricingFields = jbillingMediationRecord.getPricingFields();
            TestCase.assertFalse("pricing fields should not contain CDR_IDENTIFIER" , pricingFields.contains("CDR_IDENTIFIER"));
            if (!(pricingFields.contains("data") || pricingFields.contains("aaptiu"))) {
                TestCase.assertTrue("pricing fields should contain Service number", pricingFields.contains("SERVICE_NUMBER"));
            }

        }
    }

    /**
     * Helper method to fetch the random number within provided range
     * @param Min
     * @param Max
     * @return int
     */
    public static Long randomLong(Long Min, Long Max) {
        return (long) (Math.random()*(Max-Min))+Min;
    }

    /**
     * Helper method to fetch date in string format
     * @param localDateTime
     * @param format
     * @return String
     */
    public static String getDateFormatted(LocalDateTime localDateTime, String format) {

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        return dtf.format(localDateTime);

    }

    /**
     * Helper method to prepend zeros
     * @param str
     * @return String
     */
    public static String prependZero(String str, int i) {

        while (str.length() < i) {
            str="0"+str;
        }
        return str;
    }
    
    protected Integer createServiceOrder(String code, Date activeSince, Date activeUntil, Integer orderPeriodId, boolean prorate,
            Map<Integer, BigDecimal> productQuantityMap, Map<Integer, List<Integer>> productAssetMap, Integer userId,
            String price, String discription, Map<String,Object> metafields) {
        this.testBuilder.given(
        envBuilder -> {
        
            List<OrderLineWS> lines = productQuantityMap.entrySet().stream().map(lineItemQuatityEntry -> {
              List<MetaFieldValueWS> metaFieldValueWSs = new ArrayList<>();
            
              if (MapUtils.isNotEmpty(metafields)) {
                  metafields.entrySet().forEach(pair -> {
                      MetaFieldWS metaFieldWS1 = ApiBuilderHelper.getMetaFieldWS(pair.getKey(), getMetaFieldDataType(pair.getValue()), EntityType.ORDER,
                              api.getCallerCompanyId());
                      MetaFieldValueWS metaFieldValue = new MetaFieldValueWS(metaFieldWS1,null,pair.getValue());
                      metaFieldValueWSs.add(metaFieldValue);
                  });
              }
            
              OrderLineWS line = new OrderLineWS();
              line.setItemId(lineItemQuatityEntry.getKey());
              line.setTypeId(Integer.valueOf(1));
              ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
              line.setDescription(StringUtils.isNotBlank(discription) ? discription : item.getDescription());
              line.setQuantity(lineItemQuatityEntry.getValue());
              line.setUseItem(false);
              line.setAmount(price);
              line.setMetaFields(metaFieldValueWSs.toArray(new MetaFieldValueWS[metaFieldValueWSs.size()]));
              line.setPrice(price);
              if (null != productAssetMap && !productAssetMap.isEmpty() && productAssetMap.containsKey(line.getItemId())) {
                  List<Integer> assets = productAssetMap.get(line.getItemId());
                  line.setAssetIds(assets.toArray(new Integer[0]));
                  line.setQuantity(assets.size());
              }
              return line;
            }).collect(Collectors.toList());
            
            envBuilder.orderBuilder(api).withCodeForTests(code).forUser(userId)
                  .withActiveSince(activeSince).withActiveUntil(activeUntil).withEffectiveDate(activeSince)
                  .withPeriod(orderPeriodId).withProrate(prorate).withOrderLines(lines).withCurrency(api.getUserWS(userId).getCurrencyId())
                  .withOrderChangeStatus(ORDER_CHANGE_STATUS_APPLY_ID).build();
            }).test((testEnv, envBuilder) -> assertNotNull(ORDER_CREATION_ASSERT, envBuilder.idForCode(code)));
        return testBuilder.getTestEnvironment().idForCode(code);
    }
    
    private static DataType getMetaFieldDataType(Object value){
        if (value instanceof String) {
            return DataType.STRING;
        } else if (value instanceof Date) {
            return DataType.DATE;
        } else if (value instanceof Boolean) {
            return DataType.BOOLEAN;
        } else if (value instanceof BigDecimal) {
            return DataType.DECIMAL;
        } else if (value instanceof Integer) {
            return DataType.INTEGER;
        } else if (value instanceof List || value instanceof String[]) {
            // store List<String> as String[] for WS-compatible mode, perform manual convertion
            return DataType.LIST;
        } else if (value instanceof char[]) {
            return DataType.CHAR;
        }

        return DataType.STRING;
    }

    
    protected void triggerBilling(JbillingAPI api, Date runDate, Boolean review, String prorating, Integer numPeriods,
            Integer periodUnitId, Integer skipEmails, String skipEmailsDays) {
        try {
            BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
            config.setNextRunDate(runDate);
            config.setRetries(1);
            config.setDaysForRetry(5);
            config.setGenerateReport(review ? 1 : 0);
            config.setAutoPaymentApplication(0);
            config.setDfFm(0);
            config.setPeriodUnitId(new Integer(periodUnitId));
            config.setDueDateUnitId(periodUnitId);
            config.setDueDateValue(1);
            config.setInvoiceDateProcess(0);
            config.setMaximumPeriods(numPeriods);
            config.setOnlyRecurring(0);
            config.setProratingType(prorating);
            config.setSkipEmails(skipEmails);
            config.setSkipEmailsDays(skipEmailsDays);

            api.createUpdateBillingProcessConfiguration(config);
            api.triggerBilling(runDate);
            logger.debug("Is billing process running {}", api.isBillingRunning(1));
        } catch (Exception e) {
            logger.error("Failed to trigger billrun {}", e);
        }
    }
    
    protected String getCutOffTime(int addMinutes) {
        DateTimeFormatter timeParser = DateTimeFormatter.ofPattern("HH:mm");
        return timeParser.format(LocalDateTime.now().plusMinutes(addMinutes));
    }
    
    protected void createUpdateSPCInvoiceEmailDispatcherTask(ConfigurationBuilder configurationBuilder, Integer billingProcessId, boolean dispatchEmailsAgain) {
        String emailDispatcherClassName = InvoiceEmailDispatcherTask.class.getName();
        if (!configurationBuilder.pluginExists(emailDispatcherClassName, api.getCallerCompanyId())) {
            Hashtable<String, String> emailDispatcherParameters = new Hashtable<>();
            emailDispatcherParameters.put(PARAM_BILLING_PROCESS_ID, String.valueOf(billingProcessId));
            emailDispatcherParameters.put(PARAM_DISPATCH_EMAILS_AGAIN, String.valueOf(dispatchEmailsAgain));

            configurationBuilder.addPluginWithParameters(emailDispatcherClassName, emailDispatcherParameters).withProcessingOrder(
                    emailDispatcherClassName, 22);
        } else if (configurationBuilder.pluginExists(emailDispatcherClassName, api.getCallerCompanyId())) {
            Map<String, String> emailDispatcherParameters = new HashMap<>();
            emailDispatcherParameters.put(PARAM_BILLING_PROCESS_ID, String.valueOf(billingProcessId));
            emailDispatcherParameters.put(PARAM_DISPATCH_EMAILS_AGAIN, String.valueOf(dispatchEmailsAgain));

            PluggableTaskWS[]  tasks = api.getPluginsWS(api.getCallerCompanyId(), emailDispatcherClassName);
            updateExistingPlugin(api,tasks[0].getId(), emailDispatcherClassName,emailDispatcherParameters);
        }
    }

    protected void configureOrderChangeUpdateTask(ConfigurationBuilder confBuilder) {
        if (!confBuilder.pluginExists(OrderChangeUpdateTask.class.getName(), api.getCallerCompanyId())) {

            Hashtable<String, String> userFilterrParameters = new Hashtable<>();

            confBuilder.addPluginWithParameters(OrderChangeUpdateTask.class.getName(), userFilterrParameters).withProcessingOrder(
                    OrderChangeUpdateTask.class.getName(), 25684);
        }
    }
    
    
    protected void configureSPCReportCSVExporterTask(ConfigurationBuilder confBuilder) {   

        Hashtable<String, String> pluginParameters = new Hashtable<>();
        pluginParameters.put("spc_report_names_list", "detailed_billing_report");
        pluginParameters.put("spc_csv_export_path", "resources/spc-reports/");
        pluginParameters.put("spc_csv_files_split_limit", "10000");
        int processingOrder = randomLong(1000L,10000L).intValue();

        confBuilder.addPluginWithParameters(SPCReportCSVExporterTask.class.getName(), pluginParameters).withProcessingOrder(
                SPCReportCSVExporterTask.class.getName(), processingOrder);
    }
}

