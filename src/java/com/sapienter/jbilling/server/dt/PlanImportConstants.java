package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.common.Util;

import java.io.File;

/**
 * Created by Taimoor Choudhary on 3/29/18.
 */
public class PlanImportConstants {

    private static final String PLAN_FILES_DIR = Util.getSysProp("base_dir") + "bulkUploadFiles" + File.separator + "plans";

    public static final String JOB_PARAM_INPUT_FILE = "input_file";
    public static final String JOB_PARAM_ERROR_FILE = "error_file";
    public static final String JOB_PARAM_ENTITY_ID = "entity_id";
    public static final String JOB_PARAM_USER_ID = "user_id";
    public static final String JOB_PARAM_START = "startDate";
    public static final String PLAN_NUMBER_COL = "plan_number";
    public static final String PLAN_DESC_COL = "plan_description";
    public static final String START_DATE_COL = "price_start_date";
    public static final String AVAILABILITY_START_COL = "availability_startdate";
    public static final String AVAILABILITY_END_COL = "availability_enddate";
    public static final String CURRENCY_CODE_COL = "currency";
    public static final String PLAN_PERIOD_COL = "plan_period";
    public static final String PLAN_RATE_COL = "plan_rate";
    public static final String PLAN_CATEGORY_COL = "plan_category";
    public static final String PLAN_MF_PAYMENT_OPTION_COL = "plan_payment_option";
    public static final String PLAN_MF_DURATION_COL = "plan_duration";
    public static final String FREE_USAGE_POOL_COL = "free_usage_pool_name";
    public static final String FREE_USAGE_POOL_QTY_COL = "free_usage_pool_qty";
    public static final String TYPE_COL = "type";
    public static final String BUNDLE_QTY_COL = "bundle_qty";
    public static final String BUNDLE_PERIOD_COL = "bundle_period";
    public static final String BUNDLE_ITEM_CODE_COL = "bundle_item";

    // Parameters in JobInstance's ExecutionContext
    public static final String JOB_PARAM_TOTAL_LINE_COUNT    = "total_lines";
    public static final String JOB_PARAM_ERROR_LINE_COUNT    = "error_lines";

    public enum ColumnIdentifier {
        PLAN ("PLAN"),
        FUP ("FUP"),
        ITEM ("ITEM"),
        FLAT ("FLAT"),
        TIER ("TIER");

        private final String name;

        private ColumnIdentifier(String s) {
            name = s;
        }

        public boolean equalsName(String otherName) {
            return (otherName == null) ? false : name.equals(otherName);
        }

        public String toString() {
            return this.name;
        }
    }
}
