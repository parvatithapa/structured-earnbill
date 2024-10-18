package com.sapienter.jbilling.server.dt;

import com.sapienter.jbilling.common.Util;

import java.io.File;

/**
 * Created by Wajeeha Ahmed on 11/27/17.
 */
public class ProductImportConstants {
    private static final String PRODUCT_FILES_DIR = Util.getSysProp("base_dir") + "bulkUploadFiles" + File.separator + "products";
    private static final String PRODUCT_DOWNLOAD_FILES_DIR = Util.getSysProp("base_dir") + "bulkDownloadFiles" + File.separator + "products";
    private static final String ACCOUNT_LEVEL_PRICE_DOWNLOAD_FILES_DIR = Util.getSysProp("base_dir") + "bulkDownloadFiles" + File.separator + "accountLevelPrices";
    private static final String CUSTOMER_PRICE_DOWNLOAD_FILES_DIR = Util.getSysProp("base_dir") + "bulkDownloadFiles" + File.separator + "customerPrices";
    private static final String PLANS_DOWNLOAD_FILES_DIR = Util.getSysProp("base_dir") + "bulkDownloadFiles" + File.separator + "plans";

    public static final String JOB_PARAM_INPUT_FILE = "input_file";
    public static final String JOB_PARAM_OUTPUT_FILE = "output_file";
    public static final String JOB_PARAM_ERROR_FILE = "error_file";
    public static final String JOB_PARAM_ENTITY_ID = "entity_id";
    public static final String JOB_PARAM_USER_ID = "user_id";
    public static final String JOB_PARAM_START = "startDate";
    public static final String JOB_PARAM_IDENTIFICATION_CODE = "identificationCode";
    public static final String PROD_CODE_COL = "product_code";
    public static final String PROD_DESC_COL = "product_description";
    public static final String START_DATE_COL = "price_start_date";
    public static final String PROD_CAT_COL = "product_categories";
    public static final String AVAILABILITY_START_COL = "availability_startdate";
    public static final String AVAILABILITY_END_COL = "availability_enddate";
    public static final String ACCOUNT_TYPE_ID_COL = "account_type_id";
    public static final String CUSTOMER_ID_COL = "customer_id";
    public static final String CURRENCY_CODE_COL = "currency";
    public static final String FLAT_RATE_COL = "flat_rate";
    public static final String TYPE_COL = "type";
    public static final String PROD_ALLOW_DECIMAL_QUANTITY_COL = "allow_decimal_quantity";
    public static final String PROD_COMPANY = "company";
    public static final String PROD_CHAINED="chained";
    public static final String EXPIRY_DATE_COL="price_expiry_date";
    public static final String RATING_UNIT_NAME="rating_unit_name";
    public static final String RATING_SCHEME_NAME="rating_scheme_name";
    public static final String PRICE_UNIT="price_unit";
    public static final String META_FIELD_FEATURES="meta_field_features";

    public static final String TIER_RATE_COL = "tier_rate";
    public static final String QTY_FROM_COL = "from";


    // Parameters in JobInstance's ExecutionContext
    public static final String JOB_PARAM_TOTAL_LINE_COUNT    = "total_lines";
    public static final String JOB_PARAM_ERROR_LINE_COUNT    = "error_lines";

    public enum ColumnIdentifier {
        PRODUCT ("PROD"),
        PRICE ("PRICE"),
        PRICE_MODEL_FLAT ("FLAT"),
        PRICE_MODEL_TIERED ("TIER"),
        RATING_UNIT ("RATING"),
        META_FIELDS ("META");

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
