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
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;

import java.util.Map;

import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.BOOLEAN;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.DATE;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.INT;
import static com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription.Type.STR;

public class AdennetExternalConfigurationTask extends PluggableTask implements ExternalConfigurationTask {

    public static final ParameterDescription USAGE_MANAGEMENT_SERVICE_URL = new ParameterDescription("usage_management_url", true, STR);
    public static final ParameterDescription DOWNGRADE_FEE_ID = new ParameterDescription("downgrade_fee_id", false, STR);
    public static final ParameterDescription SIM_PRICE_ID = new ParameterDescription("sim_price_id", false, STR);
    public static final ParameterDescription ADD_ON_PRODUCT_ID = new ParameterDescription("add_on_product_id", true, STR);

    public static final ParameterDescription IMAGE_SERVER_ADDRESS = new ParameterDescription("image_server_address", true, STR);
    public static final ParameterDescription IMAGE_SERVER_USERNAME = new ParameterDescription("image_server_username", true, STR);
    public static final ParameterDescription IMAGE_SERVER_PASSWORD = new ParameterDescription("image_server_password", true, STR, true);
    public static final ParameterDescription IMAGE_SERVER_FOLDER_LOCATION = new ParameterDescription("image_server_folder_location", true, STR);
    public static final ParameterDescription MEDIATION_SERVICE_URL = new ParameterDescription("mediation_service_url", true, STR);
    public static final ParameterDescription MEDIATION_SERVICE_PASSWORD = new ParameterDescription("mediation_service_password", true, STR, true);

    public static final ParameterDescription SIM_REISSUE_FEE_ID = new ParameterDescription("sim_reissue_fee_id", true, STR);

    public static final ParameterDescription RADIUS_SERVER_SERVICE_URL = new ParameterDescription("radius_server_service_url", true, STR);

    public static final ParameterDescription HSS_SSH_IP = new ParameterDescription("hss_ssh_ip", false, STR);

    public static final ParameterDescription HSS_SSH_USERNAME = new ParameterDescription("hss_ssh_username", false, STR);
    public static final ParameterDescription HSS_SSH_PASSWORD = new ParameterDescription("hss_ssh_password", false, STR, true);
    public static final ParameterDescription IS_PROD_ENV = new ParameterDescription("is_prod_env", true, BOOLEAN, false, "false");
    public static final ParameterDescription SUBSCRIBER_RELEASE_LIMIT_IN_DAYS = new ParameterDescription("subscriber_release_limit_in_days", true, INT);
    public static final ParameterDescription MAXIMUM_RECHARGE_AND_REFUND_LIMIT = new ParameterDescription("maximum_recharge_and_refund_limit", true, INT);

    public static final ParameterDescription EMPLOYEE_DEFAULT_PLAN_ID = new ParameterDescription("employee_default_plan_id", true, INT);
    public static final ParameterDescription AUTO_RECHARGE_CYCLE_IN_DAYS = new ParameterDescription("auto_recharge_cycle_in_days", true, INT, "30");
    public static final ParameterDescription AUTO_RECHARGE_LAST_RUN_DATE = new ParameterDescription("auto_recharge_last_run_date", true, DATE, "08-DEC-2022");
    public static final ParameterDescription REISSUE_COUNT_LIMIT = new ParameterDescription("reissue_count_limit", true, INT);
    public static final ParameterDescription REISSUE_COUNT_DURATION_IN_MONTH = new ParameterDescription("reissue_count_duration_in_month", true, INT);
    public static final ParameterDescription AUDIT_LOG_REPORT_START_DATE = new ParameterDescription("audit_log_report_start_date_time", true, DATE, "14-FEB-2024");
    public static final ParameterDescription ASSET_ENABLE_PRODUCT_ID = new ParameterDescription("asset_enable_product_id", true, INT);
    public static final ParameterDescription ORDER_LEVEL_SUBSCRIPTION_ORDER_ID_MF_NAME = new ParameterDescription("Subscription order id meta field name", true, STR);
    public static final ParameterDescription ORDER_LEVEL_SUBSCRIBER_TYPE_MF_NAME = new ParameterDescription("Subscriber type meta field name", true, STR);
    public static final ParameterDescription DATA_PRODUCT_ID = new ParameterDescription("prepaid_data_product_id", true, INT);
    public static final ParameterDescription VOICE_PRODUCT_ID = new ParameterDescription("prepaid_voice_product_id", true, INT);
    public static final ParameterDescription SMS_PRODUCT_ID = new ParameterDescription("prepaid_sms_product_id", true, INT);
    public static final ParameterDescription ADD_ON_PKG_CATEGORY_ID = new ParameterDescription("add_on_pkg_category_id", true, INT);

    public AdennetExternalConfigurationTask() {
        descriptions.add(USAGE_MANAGEMENT_SERVICE_URL);
        descriptions.add(DOWNGRADE_FEE_ID);
        descriptions.add(SIM_PRICE_ID);
        descriptions.add(ADD_ON_PRODUCT_ID);
        descriptions.add(IMAGE_SERVER_ADDRESS);
        descriptions.add(IMAGE_SERVER_USERNAME);
        descriptions.add(IMAGE_SERVER_PASSWORD);
        descriptions.add(IMAGE_SERVER_FOLDER_LOCATION);
        descriptions.add(MEDIATION_SERVICE_URL);
        descriptions.add(MEDIATION_SERVICE_PASSWORD);
        descriptions.add(SIM_REISSUE_FEE_ID);
        descriptions.add(RADIUS_SERVER_SERVICE_URL);
        descriptions.add(HSS_SSH_IP);
        descriptions.add(HSS_SSH_USERNAME);
        descriptions.add(HSS_SSH_PASSWORD);
        descriptions.add(IS_PROD_ENV);
        descriptions.add(SUBSCRIBER_RELEASE_LIMIT_IN_DAYS);
        descriptions.add(MAXIMUM_RECHARGE_AND_REFUND_LIMIT);
        descriptions.add(AUTO_RECHARGE_CYCLE_IN_DAYS);
        descriptions.add(EMPLOYEE_DEFAULT_PLAN_ID);
        descriptions.add(AUTO_RECHARGE_LAST_RUN_DATE);
        descriptions.add(REISSUE_COUNT_LIMIT);
        descriptions.add(REISSUE_COUNT_DURATION_IN_MONTH);
        descriptions.add(AUDIT_LOG_REPORT_START_DATE);
        descriptions.add(ASSET_ENABLE_PRODUCT_ID);
        descriptions.add(ORDER_LEVEL_SUBSCRIPTION_ORDER_ID_MF_NAME);
        descriptions.add(ORDER_LEVEL_SUBSCRIBER_TYPE_MF_NAME);
        descriptions.add(DATA_PRODUCT_ID);
        descriptions.add(VOICE_PRODUCT_ID);
        descriptions.add(SMS_PRODUCT_ID);
        descriptions.add(ADD_ON_PKG_CATEGORY_ID);
    }

    @Override
    public Map<String, String> getExternalConfiguration() {
        return getParameters();
    }

}
