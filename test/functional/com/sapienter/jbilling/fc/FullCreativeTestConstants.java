package com.sapienter.jbilling.fc;

/**
 * @author neelabh.dubey
 * @since May 19, 2016
 */
public interface FullCreativeTestConstants {

    	// Mediated Usage Products
	public static final int INBOUND_USAGE_PRODUCT_ID = 320101;
	public static final int CHAT_USAGE_PRODUCT_ID = 320102;
	public static final int ACTIVE_RESPONSE_USAGE_PRODUCT_ID = 320103;

	// Mediation Job Luncher Name
	public static final String INBOUND_MEDIATION_LAUNCHER = "inboundCallsMediationJobLauncher";
	public static final String ACTIVE_MEDIATION_LAUNCHER = "activeResponseMediationJobLauncher";
	public static final String CHAT_MEDIATION_LAUNCHER = "chatMediationJobLauncher";
	public static final String FC_MEDIATION_LAUNCHER = "fcMediationJobLauncher";

	// Number Asset Products
	public static final int ACTIVE_RESPONSE_ACCOUNT_ASSET_PRODUCT_ID=320108;
	public static final int CHAT_ACCOUNT_ASSET_PRODUCT_ID=320107;
	public static final int LOCAL_ECF_NUMBER_ASSET_PRODUCT_ID=320106;
	public static final int TOLL_FREE_800_NUMBER_ASSET_PRODUCT_ID=320105;
	public static final int TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID=320104;

	//Plan Ids
	public static final int AF_BEST_VALUE_PLAN_ID=105;
	public static final int AF_BASIC_PLAN_ID=104;
	public static final int AF_INTRO_PLAN_ID=103;
	public static final int AF_SMALL_PLAN_ID=102;

	// Plan subscription Id
	public static final int AF_BEST_VALUE_PLAN_SUBSCRIPTION_ID=320115;
	public static final int AF_BASIC_PLAN_SUBSCRIPTION_ID=320114;
	public static final int AF_INTRO_PLAN_SUBSCRIPTION_ID=320113;
	public static final int AF_SMALL_PLAN_SUBSCRIPTION_ID=320112;

	// mediation files name
	public static final String INBOUND_FILE_NAME="Daily_CDR's_inbound_sample.csv";
	public static final String ACTIVE_RESPONSE_FILE_NAME="Daily_CDR's_active_response_sample.csv";
	public static final String CHAT_FILE_NAME="Daily_CDR's_chat_sample.csv";
	public static final String INBOUND_FILE_NAME1= "Daily_CDR's_inbound_sample1.csv";
	public static final String INBOUND_FILE_NAME2= "Daily_CDR's_inbound_sample2.csv";
	public static final String INBOUND_FILE_NAME3= "Daily_CDR's_inbound_sample3.csv";

	public static final int ANSWER_FORCE_PLANS_CATEGORY_ID=230306;
	public static final int NUMBER_ASSET_CATEGORY_ID = 230304;

	public static final String ORDER_LINE_BASED_COMPOSITION_TASK_NAME = "com.sapienter.jbilling.server.pluggableTask.TelcoOrderLineBasedCompositionTask";
	public static final String ORDER_LINE_BASED_COMPOSITION_TASK = "com.sapienter.jbilling.server.pluggableTask.OrderLineBasedCompositionTask";
	public static final String ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME = "com.sapienter.jbilling.server.pluggableTask.OrderChangeBasedCompositionTask";
	public static final int INVOICE_COMPOSITION_PLUGIN_ID = 6092;
	public static final String UPDATE_CUSTOMER_ACCOUNT_NUMBER_TASK_CLASS_NAME = "com.sapienter.jbilling.server.order.task.UpdateCustomerAccountNumberTask";
	public static final String TELCO_USAGE_MANAGER_TASK_NAME = "com.sapienter.jbilling.server.item.tasks.TelcoUsageManagerTask";
	public static final String BASIC_ITEM_MANAGER_TASK_NAME = "com.sapienter.jbilling.server.item.tasks.BasicItemManager";
	public static final int BASIC_ITEM_MANAGER_PLUGIN_ID = 1;

	public static final int PAYMENT_FAKE_TASK_ID1 = 20;
	public static final int PAYMENT_ROUTER_CCF_TASK_ID = 21;
	public static final int PAYMENT_FAKE_TASK_ID2 = 22;
	public static final int PAYMENT_FILTER_TASK_ID = 460;
	public static final int PAYMENT_ROUTER_CUREENCY_TASK_ID = 520;
	public static final String PAYMNET_PAYPAL_EXTERNAL_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentPaypalExternalTask";
	public static final String SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask";
	public static final String PAY_FLOW_EXTERNAL_ACH_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.paypal.PayflowExternalACHTask";
	public static final String SAVE_ACH_EXTERNALLY_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.SaveACHExternallyTask";

	public static final String SUSPENDED_USER_BILLING_PROCESS_FILTER_TASK = "com.sapienter.jbilling.server.process.task.SuspendedUsersBillingProcessFilterTask";

	public static final String ORDER_LINE_COUNT_BASED_USER_PARTITIONING_TASK = "com.sapienter.jbilling.server.mediation.task.OrderLineCountBasedUserPartitioningTask";
}
