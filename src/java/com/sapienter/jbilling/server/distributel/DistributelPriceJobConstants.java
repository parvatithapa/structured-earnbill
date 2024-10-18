package com.sapienter.jbilling.server.distributel;

public class DistributelPriceJobConstants {

    private DistributelPriceJobConstants() {

    }

    public static final String PARAM_JOB_LAUNCHER_NAME      = "distributelJobLauncher";
    public static final String PARAM_ORDER_LEVEL_MF_NAME    = "order_level_mf_name";
    public static final String PARAM_JOB_NAME               = "distributelPriceUpdateJob";
    public static final String PARAM_PROCESSING_DATE_NAME   = "processing_date";
    public static final String PARAM_DATA_TABLE_NAME        = "data_table_name";
    public static final String PARAM_NOTIFICATION_EMAIL_ID  = "notification_email_id";
    public static final String PARAM_ENTITY_ID              = "entityId";
    public static final String PARAM_FUTURE_PROCESSING_DATE = "future_processing_date";
    public static final String ERROR_DIR_NAME               = "distributel-price-job-error";
    public static final String PARAM_ERROR_FILE_PATH        = "error-file-path";
    public static final String REUQUEST_FAILED_STATUS       = "FAILED";
    public static final String REUQUEST_SUCCESS_STATUS      = "SUCCEEDED";
    public static final String MESSAGE_KEY                  = "distributel.price.update.error";
    public static final String DEFAULT_DATE_FORMAT          = "M/d/yyyy";
    public static final int    MONTH_TO_ADD                 = 1;
    public static final int    DAY_TO_ADD                   = 1;
    public static final String DEFAULT_NOTE_TITLE           = "RATE ADJUSTMENT";
    public static final String PRICE_UPDATE_STEP_NAME       = "priceUpdateStep";
    public static final String USER_NOTE_CREATE_STEP_NAME   = "userNoteCreateStep";
    
    public static final String PARAM_PRICE_INCREASE_JOB_NAME               = "distributelPriceIncreaseAndReverseJob";
    public static final String PRICE_UPDATE_INCREASE_STEP_NAME             = "priceIncreaseStep";
    public static final String PRICE_UPDATE_REVERSE_STEP_NAME              = "priceReversalStep";
    public static final String PARAM_PRICE_INCREASE_DATA_TABLE_NAME        = "price_increase_data_table";
    public static final String PARAM_PRICE_REVERSAL_DATA_TABLE_NAME        = "price_reversal_data_table";


}
