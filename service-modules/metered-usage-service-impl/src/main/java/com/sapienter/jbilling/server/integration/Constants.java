package com.sapienter.jbilling.server.integration;

public final class Constants {
    public static final String METERED_USAGE_API_ENDPOINT = "meteredUsage.Endpoint";
    public static final String METERED_USAGE_API_CONSUMER_KEY = "meteredUsage.ConsumerKey";
    public static final String METERED_USAGE_API_CONSUMER_SECRET = "meteredUsage.ConsumerSecret";
    public static final String METERED_USAGE_API_CONNECT_TIMEOUT= "meteredUsage.connectTimeout";
    public static final String METERED_USAGE_API_READ_TIMEOUT= "meteredUsage.readTimeout";
    public static final String METERED_USAGE_API_RETRIES= "meteredUsage.retries";
    public static final String METERED_USAGE_API_RETRY_WAIT= "meteredUsage.retryWait";
    public static final String METERED_USAGE_API_ASYNC= "meteredUsage.async";

    public static final String ORDER_UPLOADED_STATUS_ID = "order.UploadedOrderStatus";
    public static final String ORDER_ACTIVE_STATUS_ID = "order.ActiveOrderStatus";
    public static final String ORDER_UPLOAD_FAILED_STATUS_ID = "order.uploadFailedOrderStatus";

    public static final String CUSTOMER_EXTERNAL_ACCOUNT_IDENTIFIER_MF = "externalAccountIdentifier";

    public static final String PARM_CURRENT_PARTITION = "partitions.current";
    public static final String PARM_NUMBER_OF_PARTITIONS = "partitions.total";

    public static final String METERED_USAGE_API_PATH = "/api/integration/v1/billing/usage";

    public static final String PLAN_PAYMENT_OPTION_MF = "Payment Option";
    public static final String PLAN_DURATION_MF = "Duration";
    public static final String RESERVED_IDENTIFIER_DESCRIPTION = "Reserved Included Usage ";
    public static final String RESERVED_UPGRADE_DESCRIPTION = "Reserved Upgrade Adjustment";
    public static final String TIERED_FROM = "From";
    public static final String TIERED_TO = "To";
    public static final String PLAN_PAYMENT_OPTION_MONTHLY = "MONTHLY";
    public static final String ORDER_LAST_RESERVED_MONTHLY_REPORT_DATE_MF = "Last Reserved Monthly Report Date";
    public static final String UPGRADED_TO = "Upgraded to";

    public static final String INITIAL = "Initial";
    public static final String FINAL = "Final";

    public static final String ADJUSTMENT = "Adjustment";


    public static final Integer TIER_DECIMAL_PLACES = 2;
    public static final String  TIER_PLUS = "plus";
    public static final String DEFAULT_BILLING_UNIT = "unit";

    public static final String BATCH_ASYNC_JOB_LAUNCHER  =  "asyncJobLauncher";

    public static final String ENTITY_ID = "entityId";

    public static final String CHARGE_TYPE = "chargeType";

    public static final String LAST_SUCCESS_MEDIATION_RUN_DATE = "lastSuccessMediationRunDate";

    public static final String PRODUCT_FEATURES_MF = "Features";
}
