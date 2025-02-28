ALTER TABLE account_type DROP CONSTRAINT "account_type_currency_id_FK";
ALTER TABLE account_type DROP CONSTRAINT "account_type_entity_id_FK";
ALTER TABLE account_type DROP CONSTRAINT "account_type_language_id_FK";
ALTER TABLE account_type DROP CONSTRAINT "account_type_main_subscription_period_FK";
ALTER TABLE account_type DROP CONSTRAINT "invoice_delivery_method_id_FK";
ALTER TABLE ach DROP CONSTRAINT "ach_fk_1";
ALTER TABLE ageing_entity_step DROP CONSTRAINT "ageing_entity_step_fk_2";
ALTER TABLE asset DROP CONSTRAINT "asset_fk_1";
ALTER TABLE asset DROP CONSTRAINT "asset_fk_2";
ALTER TABLE asset DROP CONSTRAINT "asset_fk_3";
ALTER TABLE asset DROP CONSTRAINT "asset_fk_4";
--ALTER TABLE asset_entity_map DROP CONSTRAINT "asset_entity_map_fk1";
--ALTER TABLE asset_entity_map DROP CONSTRAINT "asset_entity_map_fk2";
ALTER TABLE asset_meta_field_map DROP CONSTRAINT "asset_meta_field_map_fk_1";
ALTER TABLE asset_meta_field_map DROP CONSTRAINT "asset_meta_field_map_fk_2";
ALTER TABLE asset_provisioning_command_map DROP CONSTRAINT "asset_provisioning_command_map_fk_1";
ALTER TABLE asset_provisioning_command_map DROP CONSTRAINT "asset_provisioning_command_map_fk_2";
ALTER TABLE asset_status DROP CONSTRAINT "asset_status_fk_1";
ALTER TABLE asset_transition DROP CONSTRAINT "asset_transition_fk_1";
ALTER TABLE asset_transition DROP CONSTRAINT "asset_transition_fk_2";
ALTER TABLE asset_transition DROP CONSTRAINT "asset_transition_fk_3";
ALTER TABLE asset_transition DROP CONSTRAINT "asset_transition_fk_4";
ALTER TABLE asset_transition DROP CONSTRAINT "asset_transition_fk_5";
ALTER TABLE base_user DROP CONSTRAINT "base_user_fk_3";
ALTER TABLE base_user DROP CONSTRAINT "base_user_fk_4";
ALTER TABLE base_user DROP CONSTRAINT "base_user_fk_5";
ALTER TABLE base_user DROP CONSTRAINT "base_user_fk_6";
ALTER TABLE billing_process DROP CONSTRAINT "billing_process_fk_1";
ALTER TABLE billing_process DROP CONSTRAINT "billing_process_fk_2";
ALTER TABLE billing_process DROP CONSTRAINT "billing_process_fk_3";
ALTER TABLE billing_process_configuration DROP CONSTRAINT "billing_proc_configtn_fk_2";
ALTER TABLE blacklist DROP CONSTRAINT "blacklist_fk_1";
ALTER TABLE blacklist DROP CONSTRAINT "blacklist_fk_2";
ALTER TABLE blacklist DROP CONSTRAINT "blacklist_fk_4";
ALTER TABLE charge_sessions DROP CONSTRAINT "fk_sessions_user";
ALTER TABLE contact_map DROP CONSTRAINT "contact_map_fk_1";
ALTER TABLE contact_map DROP CONSTRAINT "contact_map_fk_3";
ALTER TABLE currency_entity_map DROP CONSTRAINT "currency_entity_map_fk_1";
ALTER TABLE currency_entity_map DROP CONSTRAINT "currency_entity_map_fk_2";
ALTER TABLE currency_exchange DROP CONSTRAINT "currency_exchange_fk_1";
ALTER TABLE customer DROP CONSTRAINT "customer_account_type_fk";
ALTER TABLE customer DROP CONSTRAINT "customer_fk_1";
ALTER TABLE customer DROP CONSTRAINT "customer_fk_2";
ALTER TABLE customer DROP CONSTRAINT "customer_fk_3";
ALTER TABLE customer DROP CONSTRAINT "customer_main_subscription_period_FK";
ALTER TABLE customer_account_info_type_timeline DROP CONSTRAINT "customer_account_info_type_timeline_account_info_type_id_fk";
ALTER TABLE customer_account_info_type_timeline DROP CONSTRAINT "customer_account_info_type_timeline_customer_id_fk";
ALTER TABLE customer_account_info_type_timeline DROP CONSTRAINT "customer_account_info_type_timeline_meta_field_value_id_fk";
ALTER TABLE customer_meta_field_map DROP CONSTRAINT "customer_meta_field_map_fk_1";
ALTER TABLE customer_meta_field_map DROP CONSTRAINT "customer_meta_field_map_fk_2";
ALTER TABLE customer_notes DROP CONSTRAINT "customer_notes_customer_id_FK";
ALTER TABLE customer_notes DROP CONSTRAINT "customer_notes_entity_id_FK";
ALTER TABLE customer_notes DROP CONSTRAINT "customer_notes_user_id_FK";
ALTER TABLE customer_price DROP CONSTRAINT "customer_price_plan_item_id_fk";
ALTER TABLE customer_price DROP CONSTRAINT "customer_price_user_id_fk";
ALTER TABLE customer_usage_pool_map DROP CONSTRAINT "customer_usage_pool_map_fk_1";
ALTER TABLE customer_usage_pool_map DROP CONSTRAINT "customer_usage_pool_map_fk_2";
ALTER TABLE data_table_query DROP CONSTRAINT "data_table_query_next_FK";
ALTER TABLE data_table_query_entry DROP CONSTRAINT "data_table_query_entry_next_FK";
ALTER TABLE discount DROP CONSTRAINT "discount_entity_id_fk";
ALTER TABLE discount_attribute DROP CONSTRAINT "discount_attr_id_fk";
ALTER TABLE discount_line DROP CONSTRAINT "discount_line_discount_id_fk";
ALTER TABLE discount_line DROP CONSTRAINT "discount_line_item_id_fk";
ALTER TABLE discount_line DROP CONSTRAINT "discount_line_order_id_fk";
ALTER TABLE discount_line DROP CONSTRAINT "discount_line_order_line_id_fk";
ALTER TABLE discount_line DROP CONSTRAINT "discount_line_plan_item_id_fk";
ALTER TABLE entity DROP CONSTRAINT "entity_fk_1";
ALTER TABLE entity DROP CONSTRAINT "entity_fk_2";
ALTER TABLE entity DROP CONSTRAINT "entity_fk_3";
ALTER TABLE entity_delivery_method_map DROP CONSTRAINT "entity_delivry_methd_map_fk1";
ALTER TABLE entity_delivery_method_map DROP CONSTRAINT "entity_delivry_methd_map_fk2";
ALTER TABLE entity_item_price_map DROP CONSTRAINT "item_price_model_map_fk1";
ALTER TABLE entity_item_price_map DROP CONSTRAINT "item_price_model_map_fk2";
ALTER TABLE entity_payment_method_map DROP CONSTRAINT "entity_payment_method_map_fk_1";
ALTER TABLE entity_payment_method_map DROP CONSTRAINT "entity_payment_method_map_fk_2";
ALTER TABLE entity_report_map DROP CONSTRAINT "report_map_entity_id_fk";
ALTER TABLE entity_report_map DROP CONSTRAINT "report_map_report_id_fk";
ALTER TABLE enumeration_values DROP CONSTRAINT "enumeration_values_fk_1";
ALTER TABLE event_log DROP CONSTRAINT "event_log_fk_1";
ALTER TABLE event_log DROP CONSTRAINT "event_log_fk_2";
ALTER TABLE event_log DROP CONSTRAINT "event_log_fk_3";
ALTER TABLE event_log DROP CONSTRAINT "event_log_fk_4";
ALTER TABLE event_log DROP CONSTRAINT "event_log_fk_5";
ALTER TABLE event_log DROP CONSTRAINT "event_log_fk_6";
ALTER TABLE generic_status DROP CONSTRAINT "generic_status_entity_id_fk";
ALTER TABLE generic_status DROP CONSTRAINT "generic_status_fk_1";
ALTER TABLE international_description DROP CONSTRAINT "international_description_fk_1";
ALTER TABLE invoice DROP CONSTRAINT "invoice_fk_1";
ALTER TABLE invoice DROP CONSTRAINT "invoice_fk_2";
ALTER TABLE invoice DROP CONSTRAINT "invoice_fk_3";
ALTER TABLE invoice DROP CONSTRAINT "invoice_fk_4";
ALTER TABLE invoice_line DROP CONSTRAINT "invoice_line_fk_1";
ALTER TABLE invoice_line DROP CONSTRAINT "invoice_line_fk_2";
ALTER TABLE invoice_line DROP CONSTRAINT "invoice_line_fk_3";
ALTER TABLE invoice_line DROP CONSTRAINT "invoice_line_fk_4";
ALTER TABLE invoice_meta_field_map DROP CONSTRAINT "invoice_meta_field_map_fk_1";
ALTER TABLE invoice_meta_field_map DROP CONSTRAINT "invoice_meta_field_map_fk_2";
ALTER TABLE item DROP CONSTRAINT "item_fk_1";
ALTER TABLE item_dependency DROP CONSTRAINT "item_dependency_fk1";
ALTER TABLE item_dependency DROP CONSTRAINT "item_dependency_fk2";
ALTER TABLE item_dependency DROP CONSTRAINT "item_dependency_fk3";
ALTER TABLE item_entity_map DROP CONSTRAINT "item_entity_map_fk1";
ALTER TABLE item_entity_map DROP CONSTRAINT "item_entity_map_fk2";
ALTER TABLE item_meta_field_map DROP CONSTRAINT "item_meta_field_map_fk_1";
ALTER TABLE item_meta_field_map DROP CONSTRAINT "item_meta_field_map_fk_2";
ALTER TABLE item_price_timeline DROP CONSTRAINT "item_pm_map_model_map_id_fk";
ALTER TABLE item_price_timeline DROP CONSTRAINT "item_pm_map_price_model_id_fk";
ALTER TABLE item_type DROP CONSTRAINT "item_type_fk_1";
ALTER TABLE item_type DROP CONSTRAINT "parent_id_fk";
ALTER TABLE item_type_entity_map DROP CONSTRAINT "item_type_entity_map_fk1";
ALTER TABLE item_type_entity_map DROP CONSTRAINT "item_type_entity_map_fk2";
ALTER TABLE item_type_exclude_map DROP CONSTRAINT "item_type_exclude_item_id_fk";
ALTER TABLE item_type_exclude_map DROP CONSTRAINT "item_type_exclude_type_id_fk";
ALTER TABLE item_type_map DROP CONSTRAINT "item_type_map_fk_1";
ALTER TABLE item_type_map DROP CONSTRAINT "item_type_map_fk_2";
ALTER TABLE item_type_meta_field_def_map DROP CONSTRAINT "item_type_meta_field_def_map_fk_1";
ALTER TABLE item_type_meta_field_def_map DROP CONSTRAINT "item_type_meta_field_def_map_fk_2";
ALTER TABLE matching_field DROP CONSTRAINT "matching_field_route_id_FK";
ALTER TABLE matching_field DROP CONSTRAINT "matching_field_route_rate_card_id_FK";
ALTER TABLE mediation_cfg DROP CONSTRAINT "fk_mediation_cfg_root_route";
ALTER TABLE mediation_cfg DROP CONSTRAINT "mediation_cfg_fk_1";
ALTER TABLE mediation_order_map DROP CONSTRAINT "mediation_order_map_fk_1";
ALTER TABLE mediation_order_map DROP CONSTRAINT "mediation_order_map_fk_2";
ALTER TABLE mediation_process DROP CONSTRAINT "mediation_process_fk_1";
ALTER TABLE meta_field_group DROP CONSTRAINT "account_type_main_subscription_period_FK2";
ALTER TABLE meta_field_name DROP CONSTRAINT "meta_field_entity_id_fk";
ALTER TABLE meta_field_name DROP CONSTRAINT "meta_field_name_fk_1";
ALTER TABLE meta_field_name DROP CONSTRAINT "validation_rule_fk_1";
ALTER TABLE meta_field_value DROP CONSTRAINT "meta_field_value_fk_1";
ALTER TABLE nested_plan DROP CONSTRAINT "nested_plan_parent_plan_id_fk";
ALTER TABLE nested_plan DROP CONSTRAINT "nested_plan_plan_id_fk";
ALTER TABLE notification_message DROP CONSTRAINT "notification_message_fk_1";
ALTER TABLE notification_message DROP CONSTRAINT "notification_message_fk_2";
ALTER TABLE notification_message DROP CONSTRAINT "notification_message_fk_3";
ALTER TABLE notification_message_arch_line DROP CONSTRAINT "notif_mess_arch_line_fk_1";
ALTER TABLE notification_message_line DROP CONSTRAINT "notification_message_line_fk_1";
ALTER TABLE notification_message_section DROP CONSTRAINT "notification_msg_section_fk_1";
ALTER TABLE notification_message_type DROP CONSTRAINT "category_id_fk_1";
ALTER TABLE order_change DROP CONSTRAINT "order_change_item_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_order_change_type_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_order_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_order_line_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_order_status_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_parent_order_change_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_parent_order_line_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_status_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_user_id_fk";
ALTER TABLE order_change DROP CONSTRAINT "order_change_user_status_id_fk";
ALTER TABLE order_change_asset_map DROP CONSTRAINT "order_change_asset_map_asset_id_fk";
ALTER TABLE order_change_asset_map DROP CONSTRAINT "order_change_asset_map_change_id_fk";
ALTER TABLE order_change_meta_field_map DROP CONSTRAINT "order_change_meta_field_map_fk_1";
ALTER TABLE order_change_meta_field_map DROP CONSTRAINT "order_change_meta_field_map_fk_2";
ALTER TABLE order_change_plan_item DROP CONSTRAINT "order_change_plan_item_fk1";
ALTER TABLE order_change_plan_item DROP CONSTRAINT "order_change_plan_item_fk2";
ALTER TABLE order_change_plan_item_asset_map DROP CONSTRAINT "order_change_plan_item_asset_map_fk1";
ALTER TABLE order_change_plan_item_asset_map DROP CONSTRAINT "order_change_plan_item_asset_map_fk2";
ALTER TABLE order_change_plan_item_meta_field_map DROP CONSTRAINT "order_change_plan_item_meta_field_map_fk1";
ALTER TABLE order_change_plan_item_meta_field_map DROP CONSTRAINT "order_change_plan_item_meta_field_map_fk2";
ALTER TABLE order_change_type DROP CONSTRAINT "order_change_type_entity_id_fk";
ALTER TABLE order_change_type_item_type_map DROP CONSTRAINT "order_change_type_item_type_map_change_type_id_fk";
ALTER TABLE order_change_type_item_type_map DROP CONSTRAINT "order_change_type_item_type_map_item_type_id_fk";
ALTER TABLE order_change_type_meta_field_map DROP CONSTRAINT "order_change_type_meta_field_map_change_type_id_fk";
ALTER TABLE order_change_type_meta_field_map DROP CONSTRAINT "order_change_type_meta_field_map_meta_field_id_fk";
ALTER TABLE order_line DROP CONSTRAINT "order_line_fk_1";
ALTER TABLE order_line DROP CONSTRAINT "order_line_fk_2";
ALTER TABLE order_line DROP CONSTRAINT "order_line_fk_3";
ALTER TABLE order_line DROP CONSTRAINT "order_line_parent_line_id_fk";
ALTER TABLE order_line_meta_field_map DROP CONSTRAINT "ol_meta_field_map_fk_1";
ALTER TABLE order_line_meta_field_map DROP CONSTRAINT "ol_meta_field_map_fk_2";
ALTER TABLE order_line_meta_fields_map DROP CONSTRAINT "ol_meta_fields_map_fk_1";
ALTER TABLE order_line_meta_fields_map DROP CONSTRAINT "ol_meta_fields_map_fk_2";
ALTER TABLE order_line_provisioning_command_map DROP CONSTRAINT "order_line_provisioning_command_map_fk_1";
ALTER TABLE order_line_provisioning_command_map DROP CONSTRAINT "order_line_provisioning_command_map_fk_2";
ALTER TABLE order_line_usage_pool_map DROP CONSTRAINT "order_line_usage_pool_map_fk_1";
ALTER TABLE order_line_usage_pool_map DROP CONSTRAINT "order_line_usage_pool_map_fk_2";
ALTER TABLE order_meta_field_map DROP CONSTRAINT "order_meta_field_map_fk_1";
ALTER TABLE order_meta_field_map DROP CONSTRAINT "order_meta_field_map_fk_2";
ALTER TABLE order_period DROP CONSTRAINT "order_period_fk_1";
ALTER TABLE order_period DROP CONSTRAINT "order_period_fk_2";
ALTER TABLE order_process DROP CONSTRAINT "order_process_fk_1";
ALTER TABLE order_provisioning_command_map DROP CONSTRAINT "order_provisioning_command_map_fk_1";
ALTER TABLE order_provisioning_command_map DROP CONSTRAINT "order_provisioning_command_map_fk_2";
ALTER TABLE partner DROP CONSTRAINT "partner_fk_4";
ALTER TABLE partner_commission DROP CONSTRAINT "partner_commission_currency_id_FK";
ALTER TABLE partner_meta_field_map DROP CONSTRAINT "partner_meta_field_map_fk_1";
ALTER TABLE partner_meta_field_map DROP CONSTRAINT "partner_meta_field_map_fk_2";
ALTER TABLE partner_payout DROP CONSTRAINT "partner_payout_fk_1";
ALTER TABLE payment DROP CONSTRAINT "payment_fk_1";
ALTER TABLE payment DROP CONSTRAINT "payment_fk_2";
ALTER TABLE payment DROP CONSTRAINT "payment_fk_3";
ALTER TABLE payment DROP CONSTRAINT "payment_fk_4";
ALTER TABLE payment DROP CONSTRAINT "payment_fk_5";
ALTER TABLE payment DROP CONSTRAINT "payment_fk_6";
ALTER TABLE payment_authorization DROP CONSTRAINT "payment_authorization_fk_1";
ALTER TABLE payment_info_cheque DROP CONSTRAINT "payment_info_cheque_fk_1";
ALTER TABLE payment_information DROP CONSTRAINT "payment_information_FK1";
ALTER TABLE payment_information DROP CONSTRAINT "payment_information_FK2";
ALTER TABLE payment_information_meta_fields_map DROP CONSTRAINT "payment_information_meta_fields_map_FK1";
ALTER TABLE payment_information_meta_fields_map DROP CONSTRAINT "payment_information_meta_fields_map_FK2";
ALTER TABLE payment_instrument_info DROP CONSTRAINT "payment_instrument_info_FK1";
ALTER TABLE payment_instrument_info DROP CONSTRAINT "payment_instrument_info_FK2";
ALTER TABLE payment_instrument_info DROP CONSTRAINT "payment_instrument_info_FK3";
ALTER TABLE payment_instrument_info DROP CONSTRAINT "payment_instrument_info_FK4";
ALTER TABLE payment_invoice DROP CONSTRAINT "payment_invoice_fk_1";
ALTER TABLE payment_invoice DROP CONSTRAINT "payment_invoice_fk_2";
ALTER TABLE payment_meta_field_map DROP CONSTRAINT "payment_meta_field_map_fk_1";
ALTER TABLE payment_meta_field_map DROP CONSTRAINT "payment_meta_field_map_fk_2";
ALTER TABLE payment_method_account_type_map DROP CONSTRAINT "payment_method_account_type_map_FK1";
ALTER TABLE payment_method_account_type_map DROP CONSTRAINT "payment_method_account_type_map_FK2";
ALTER TABLE payment_method_meta_fields_map DROP CONSTRAINT "payment_method_meta_fields_map_FK1";
ALTER TABLE payment_method_meta_fields_map DROP CONSTRAINT "payment_method_meta_fields_map_FK2";
ALTER TABLE payment_method_template_meta_fields_map DROP CONSTRAINT "payment_method_template_meta_fields_map_FK1";
ALTER TABLE payment_method_template_meta_fields_map DROP CONSTRAINT "payment_method_template_meta_fields_map_FK2";
ALTER TABLE payment_method_type DROP CONSTRAINT "payment_method_type_FK1";
ALTER TABLE payment_method_type DROP CONSTRAINT "payment_method_type_FK2";
ALTER TABLE payment_provisioning_command_map DROP CONSTRAINT "payment_provisioning_command_map_fk_1";
ALTER TABLE payment_provisioning_command_map DROP CONSTRAINT "payment_provisioning_command_map_fk_2";
ALTER TABLE permission DROP CONSTRAINT "permission_fk_1";
ALTER TABLE permission_role_map DROP CONSTRAINT "permission_role_map_fk_1";
ALTER TABLE permission_role_map DROP CONSTRAINT "permission_role_map_fk_2";
ALTER TABLE permission_user DROP CONSTRAINT "permission_user_fk_1";
ALTER TABLE permission_user DROP CONSTRAINT "permission_user_fk_2";
ALTER TABLE plan DROP CONSTRAINT "plan_item_id_fk";
ALTER TABLE plan DROP CONSTRAINT "plan_period_id_fk";
ALTER TABLE plan_item DROP CONSTRAINT "plan_item_bundle_id_fk";
ALTER TABLE plan_item DROP CONSTRAINT "plan_item_item_id_fk";
ALTER TABLE plan_item DROP CONSTRAINT "plan_item_plan_id_fk";
ALTER TABLE plan_item_bundle DROP CONSTRAINT "plan_item_bundle_period_fk";
ALTER TABLE plan_item_price_timeline DROP CONSTRAINT "plan_itm_timelin_plan_itm_fk";
ALTER TABLE plan_item_price_timeline DROP CONSTRAINT "plnitmtimelnprc_mode_fk";
ALTER TABLE plan_meta_field_map DROP CONSTRAINT "plan_meta_field_map_fk_1";
ALTER TABLE plan_meta_field_map DROP CONSTRAINT "plan_meta_field_map_fk_2";
ALTER TABLE plan_usage_pool_map DROP CONSTRAINT "plan_usage_pool_map_fk_1";
ALTER TABLE plan_usage_pool_map DROP CONSTRAINT "plan_usage_pool_map_fk_2";
ALTER TABLE pluggable_task DROP CONSTRAINT "pluggable_task_fk_1";
ALTER TABLE pluggable_task DROP CONSTRAINT "pluggable_task_fk_2";
ALTER TABLE pluggable_task_parameter DROP CONSTRAINT "pluggable_task_parameter_fk_1";
ALTER TABLE pluggable_task_type DROP CONSTRAINT "pluggable_task_type_fk_1";
ALTER TABLE preference DROP CONSTRAINT "preference_fk_1";
ALTER TABLE preference DROP CONSTRAINT "preference_fk_2";
ALTER TABLE price_model DROP CONSTRAINT "price_model_company_id_fk";
ALTER TABLE price_model DROP CONSTRAINT "price_model_currency_id_fk";
ALTER TABLE price_model DROP CONSTRAINT "price_model_next_id_fk";
ALTER TABLE price_model_attribute DROP CONSTRAINT "price_model_attr_model_id_fk";
ALTER TABLE process_run DROP CONSTRAINT "process_run_fk_1";
ALTER TABLE process_run DROP CONSTRAINT "process_run_fk_2";
ALTER TABLE process_run_total DROP CONSTRAINT "process_run_total_fk_1";
ALTER TABLE process_run_total DROP CONSTRAINT "process_run_total_fk_2";
ALTER TABLE process_run_total_pm DROP CONSTRAINT "process_run_total_pm_fk_1";
ALTER TABLE process_run_user DROP CONSTRAINT "process_run_user_fk_1";
ALTER TABLE process_run_user DROP CONSTRAINT "process_run_user_fk_2";
ALTER TABLE promotion DROP CONSTRAINT "promotion_fk_1";
ALTER TABLE promotion_user_map DROP CONSTRAINT "promotion_user_map_fk_1";
ALTER TABLE promotion_user_map DROP CONSTRAINT "promotion_user_map_fk_2";
ALTER TABLE provisioning_command DROP CONSTRAINT "entity_provisioning_command_fk_1";
ALTER TABLE provisioning_command_parameter_map DROP CONSTRAINT "provisioning_command_parameter_fk_1";
ALTER TABLE provisioning_request_result_map DROP CONSTRAINT "provisioning_request_result_fk_1";
ALTER TABLE purchase_order DROP CONSTRAINT "order_primary_order_fk_1";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_fk_1";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_fk_2";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_fk_3";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_fk_4";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_fk_5";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_parent__order_id_fk";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_statusId_fk";
ALTER TABLE rate_card DROP CONSTRAINT "rate_card_entity_id_fk";
ALTER TABLE rate_card_child_entity_map DROP CONSTRAINT "rate_card_child_entity_map_fk1";
ALTER TABLE rate_card_child_entity_map DROP CONSTRAINT "rate_card_child_entity_map_fk2";
ALTER TABLE rating_unit DROP CONSTRAINT "rating_unit_entity_id_FK";
ALTER TABLE reseller_entityid_map DROP CONSTRAINT "reseller_entityid_map_fk_1";
ALTER TABLE reseller_entityid_map DROP CONSTRAINT "reseller_entityid_map_fk_2";
ALTER TABLE reserved_amounts DROP CONSTRAINT "fk_reservations_session";
ALTER TABLE role DROP CONSTRAINT "role_entity_id_fk";
ALTER TABLE route_rate_card DROP CONSTRAINT "route_rate_card_rating_unit_id";
ALTER TABLE tab_configuration DROP CONSTRAINT "tab_configuration_fk_1";
ALTER TABLE tab_configuration_tab DROP CONSTRAINT "tab_configuration_tab_fk_1";
ALTER TABLE tab_configuration_tab DROP CONSTRAINT "tab_configuration_tab_fk_2";
ALTER TABLE usage_pool DROP CONSTRAINT "usage_pool_entity_id_fk";
ALTER TABLE usage_pool_attribute DROP CONSTRAINT "usage_pool_attr_pool_id_fk";
ALTER TABLE usage_pool_consumption_actions DROP CONSTRAINT "usage_pool_consumption_actions_fk";
ALTER TABLE usage_pool_consumption_log DROP CONSTRAINT "usage_pool_consumption_log_fk";
ALTER TABLE usage_pool_item_map DROP CONSTRAINT "usage_pool_item_map_fk_1";
ALTER TABLE usage_pool_item_map DROP CONSTRAINT "usage_pool_item_map_fk_2";
ALTER TABLE usage_pool_item_type_map DROP CONSTRAINT "usage_pool_item_type_map_fk_1";
ALTER TABLE usage_pool_item_type_map DROP CONSTRAINT "usage_pool_item_type_map_fk_2";
ALTER TABLE user_role_map DROP CONSTRAINT "user_role_map_fk_1";
ALTER TABLE user_role_map DROP CONSTRAINT "user_role_map_fk_2";
ALTER TABLE validation_rule_attributes DROP CONSTRAINT "validation_rule_fk_2";
ALTER TABLE account_type DROP CONSTRAINT "account_type_pkey";
ALTER TABLE ach DROP CONSTRAINT "ach_pkey";
ALTER TABLE ageing_entity_step DROP CONSTRAINT "ageing_entity_step_pkey";
ALTER TABLE asset DROP CONSTRAINT "asset_pkey";
ALTER TABLE asset_status DROP CONSTRAINT "asset_status_pkey";
ALTER TABLE asset_transition DROP CONSTRAINT "asset_transition_pkey";
ALTER TABLE base_user DROP CONSTRAINT "base_user_pkey";
ALTER TABLE batch_job_execution DROP CONSTRAINT "batch_job_execution_pkey";
ALTER TABLE batch_job_execution_context DROP CONSTRAINT "batch_job_execution_context_pkey";
ALTER TABLE batch_job_instance DROP CONSTRAINT "batch_job_instance_pkey";
ALTER TABLE batch_process_info DROP CONSTRAINT "billing_process_info_pkey";
ALTER TABLE batch_step_execution DROP CONSTRAINT "batch_step_execution_pkey";
ALTER TABLE batch_step_execution_context DROP CONSTRAINT "batch_step_execution_context_pkey";
ALTER TABLE billing_process DROP CONSTRAINT "billing_process_pkey";
ALTER TABLE billing_process_configuration DROP CONSTRAINT "billing_process_config_pkey";
ALTER TABLE billing_process_failed_user DROP CONSTRAINT "billing_process_failed_user_pkey";
ALTER TABLE blacklist DROP CONSTRAINT "blacklist_pkey";
ALTER TABLE breadcrumb DROP CONSTRAINT "breadcrumb_pkey";
ALTER TABLE cdrentries DROP CONSTRAINT "cdrentries_pkey";
ALTER TABLE charge_sessions DROP CONSTRAINT "charge_sessions_pkey";
ALTER TABLE contact DROP CONSTRAINT "contact_pkey";
ALTER TABLE contact_map DROP CONSTRAINT "contact_map_pkey";
ALTER TABLE country DROP CONSTRAINT "country_pkey";
ALTER TABLE credit_card DROP CONSTRAINT "credit_card_pkey";
ALTER TABLE currency DROP CONSTRAINT "currency_pkey";
ALTER TABLE currency_entity_map DROP CONSTRAINT "currency_entity_map_compositekey";
ALTER TABLE currency_exchange DROP CONSTRAINT "currency_exchange_pkey";
ALTER TABLE customer DROP CONSTRAINT "customer_pkey";
ALTER TABLE customer_account_info_type_timeline DROP CONSTRAINT "customer_account_info_type_timeline_pkey";
ALTER TABLE customer_meta_field_map DROP CONSTRAINT "customer_meta_field_map_compositekey";
ALTER TABLE customer_notes DROP CONSTRAINT "pk_customer_notes";
ALTER TABLE customer_price DROP CONSTRAINT "customer_price_pkey";
ALTER TABLE customer_usage_pool_map DROP CONSTRAINT "customer_usage_pool_map_pkey";
ALTER TABLE data_table_query DROP CONSTRAINT "pk_data_table_query";
ALTER TABLE data_table_query_entry DROP CONSTRAINT "pk_data_table_query_entry";
--ALTER TABLE databasechangelog DROP CONSTRAINT "pk_databasechangelog";
--ALTER TABLE databasechangeloglock DROP CONSTRAINT "pk_databasechangeloglock";
ALTER TABLE discount DROP CONSTRAINT "discount_pkey";
ALTER TABLE discount_attribute DROP CONSTRAINT "discount_attribute_pkey";
ALTER TABLE discount_line DROP CONSTRAINT "discount_line_pkey";
ALTER TABLE entity DROP CONSTRAINT "entity_pkey";
ALTER TABLE entity_delivery_method_map DROP CONSTRAINT "entity_delivery_method_map_compositekey";
ALTER TABLE entity_item_price_map DROP CONSTRAINT "item_price_model_map_pkey";
ALTER TABLE entity_payment_method_map DROP CONSTRAINT "entity_payment_method_map_compositekey";
ALTER TABLE entity_report_map DROP CONSTRAINT "entity_report_map_compositekey";
ALTER TABLE enumeration DROP CONSTRAINT "enumeration_pkey";
ALTER TABLE enumeration_values DROP CONSTRAINT "enumeration_values_pkey";
ALTER TABLE event_log DROP CONSTRAINT "event_log_pkey";
ALTER TABLE event_log_message DROP CONSTRAINT "event_log_message_pkey";
ALTER TABLE event_log_module DROP CONSTRAINT "event_log_module_pkey";
ALTER TABLE filter DROP CONSTRAINT "filter_pkey";
ALTER TABLE filter_set DROP CONSTRAINT "filter_set_pkey";
ALTER TABLE generic_status DROP CONSTRAINT "generic_status_pkey";
ALTER TABLE generic_status_type DROP CONSTRAINT "generic_status_type_pkey";
ALTER TABLE international_description DROP CONSTRAINT "international_description_pkey";
ALTER TABLE invoice DROP CONSTRAINT "invoice_pkey";
ALTER TABLE invoice_delivery_method DROP CONSTRAINT "invoice_delivery_method_pkey";
ALTER TABLE invoice_line DROP CONSTRAINT "invoice_line_pkey";
ALTER TABLE invoice_line_type DROP CONSTRAINT "invoice_line_type_pkey";
ALTER TABLE invoice_meta_field_map DROP CONSTRAINT "invoice_meta_field_map_compositekey";
ALTER TABLE item DROP CONSTRAINT "item_pkey";
ALTER TABLE item_dependency DROP CONSTRAINT "item_dependency_pk";
ALTER TABLE item_meta_field_map DROP CONSTRAINT "item_meta_field_map_compositekey";
ALTER TABLE item_price_timeline DROP CONSTRAINT "item_price_timeline_pkey";
ALTER TABLE item_type DROP CONSTRAINT "item_type_pkey";
ALTER TABLE item_type_exclude_map DROP CONSTRAINT "item_type_exclude_map_pkey";
ALTER TABLE item_type_map DROP CONSTRAINT "item_type_map_compositekey";
ALTER TABLE jbilling_seqs DROP CONSTRAINT "jbilling_seqs_pk";
ALTER TABLE jbilling_table DROP CONSTRAINT "jbilling_table_pkey";
ALTER TABLE language DROP CONSTRAINT "language_pkey";
ALTER TABLE matching_field DROP CONSTRAINT "matching_field_pkey";
ALTER TABLE mediation_cfg DROP CONSTRAINT "mediation_cfg_pkey";
ALTER TABLE mediation_errors DROP CONSTRAINT "mediation_errors_pkey";
ALTER TABLE mediation_order_map DROP CONSTRAINT "mediation_order_map_compositekey";
ALTER TABLE mediation_process DROP CONSTRAINT "mediation_process_pkey";
ALTER TABLE meta_field_group DROP CONSTRAINT "metafield_group_pkey";
ALTER TABLE meta_field_name DROP CONSTRAINT "meta_field_name_pkey";
ALTER TABLE notification_category DROP CONSTRAINT "notification_category_pk";
ALTER TABLE notification_medium_type DROP CONSTRAINT "pk_notification_medium_type";
ALTER TABLE notification_message DROP CONSTRAINT "notifictn_msg_pkey";
ALTER TABLE notification_message_arch DROP CONSTRAINT "notifictn_msg_arch_pkey";
ALTER TABLE notification_message_arch_line DROP CONSTRAINT "notifictn_msg_arch_line_pkey";
ALTER TABLE notification_message_line DROP CONSTRAINT "notifictn_msg_line_pkey";
ALTER TABLE notification_message_section DROP CONSTRAINT "notifictn_msg_section_pkey";
ALTER TABLE notification_message_type DROP CONSTRAINT "notifictn_msg_type_pkey";
ALTER TABLE order_billing_type DROP CONSTRAINT "order_billing_type_pkey";
ALTER TABLE order_change DROP CONSTRAINT "order_change_pkey";
ALTER TABLE order_change_plan_item DROP CONSTRAINT "porder_change_plan_item_pkey";
ALTER TABLE order_change_type DROP CONSTRAINT "order_change_type_pkey";
ALTER TABLE order_line DROP CONSTRAINT "order_line_pkey";
ALTER TABLE order_line_type DROP CONSTRAINT "order_line_type_pkey";
ALTER TABLE order_line_usage_pool_map DROP CONSTRAINT "order_line_usage_pool_map_pkey";
ALTER TABLE order_meta_field_map DROP CONSTRAINT "order_meta_field_map_compositekey";
ALTER TABLE order_period DROP CONSTRAINT "order_period_pkey";
ALTER TABLE order_process DROP CONSTRAINT "order_process_pkey";
ALTER TABLE order_status DROP CONSTRAINT "order_status_pkey";
ALTER TABLE paper_invoice_batch DROP CONSTRAINT "paper_invoice_batch_pkey";
ALTER TABLE partner DROP CONSTRAINT "partner_pkey";
ALTER TABLE partner_meta_field_map DROP CONSTRAINT "partner_meta_field_map_compositekey";
ALTER TABLE partner_payout DROP CONSTRAINT "partner_payout_pkey";
ALTER TABLE payment DROP CONSTRAINT "payment_pkey";
ALTER TABLE payment_authorization DROP CONSTRAINT "payment_authorization_pkey";
ALTER TABLE payment_info_cheque DROP CONSTRAINT "payment_info_cheque_pkey";
ALTER TABLE payment_information DROP CONSTRAINT "payment_information_pkey";
ALTER TABLE payment_instrument_info DROP CONSTRAINT "payment_instrument_info_pkey";
ALTER TABLE payment_invoice DROP CONSTRAINT "payment_invoice_pkey";
ALTER TABLE payment_meta_field_map DROP CONSTRAINT "payment_meta_field_map_compositekey";
ALTER TABLE payment_method DROP CONSTRAINT "payment_method_pkey";
ALTER TABLE payment_method_template DROP CONSTRAINT "payment_method_template_pkey";
ALTER TABLE payment_method_type DROP CONSTRAINT "payment_method_type_pkey";
ALTER TABLE payment_result DROP CONSTRAINT "payment_result_pkey";
ALTER TABLE period_unit DROP CONSTRAINT "period_unit_pkey";
ALTER TABLE permission DROP CONSTRAINT "permission_pkey";
ALTER TABLE permission_type DROP CONSTRAINT "permission_type_pkey";
ALTER TABLE permission_user DROP CONSTRAINT "permission_user_pkey";
ALTER TABLE plan DROP CONSTRAINT "plan_pkey";
ALTER TABLE plan_item DROP CONSTRAINT "plan_item_pkey";
ALTER TABLE plan_item_bundle DROP CONSTRAINT "plan_item_bundle_pkey";
ALTER TABLE plan_item_price_timeline DROP CONSTRAINT "plan_item_price_timeline_pkey";
ALTER TABLE pluggable_task DROP CONSTRAINT "pluggable_task_pkey";
ALTER TABLE pluggable_task_parameter DROP CONSTRAINT "pluggable_task_parameter_pkey";
ALTER TABLE pluggable_task_type DROP CONSTRAINT "pluggable_task_type_pkey";
ALTER TABLE pluggable_task_type_category DROP CONSTRAINT "pluggable_task_type_cat_pkey";
ALTER TABLE preference DROP CONSTRAINT "preference_pkey";
ALTER TABLE preference_type DROP CONSTRAINT "preference_type_pkey";
ALTER TABLE price_model DROP CONSTRAINT "price_model_pkey";
ALTER TABLE price_model_attribute DROP CONSTRAINT "price_model_attribute_pkey";
ALTER TABLE process_run DROP CONSTRAINT "process_run_pkey";
ALTER TABLE process_run_total DROP CONSTRAINT "process_run_total_pkey";
ALTER TABLE process_run_total_pm DROP CONSTRAINT "process_run_total_pm_pkey";
ALTER TABLE process_run_user DROP CONSTRAINT "process_run_user_pkey";
ALTER TABLE promotion DROP CONSTRAINT "promotion_pkey";
ALTER TABLE promotion_user_map DROP CONSTRAINT "promotion_user_map_compositekey";
ALTER TABLE provisioning_command DROP CONSTRAINT "provisioning_command_pkey";
ALTER TABLE provisioning_request DROP CONSTRAINT "provisioning_request_pkey";
ALTER TABLE purchase_order DROP CONSTRAINT "purchase_order_pkey";
ALTER TABLE rate_card DROP CONSTRAINT "rate_card_pkey";
ALTER TABLE rating_unit DROP CONSTRAINT "pk_rating_unit";
ALTER TABLE recent_item DROP CONSTRAINT "recent_item_pkey";
ALTER TABLE report DROP CONSTRAINT "report_pkey";
ALTER TABLE report_parameter DROP CONSTRAINT "report_parameter_pkey";
ALTER TABLE report_type DROP CONSTRAINT "report_type_pkey";
ALTER TABLE reserved_amounts DROP CONSTRAINT "reserved_amounts_pkey";
ALTER TABLE reset_password_code DROP CONSTRAINT "pk_reset_password_code";
ALTER TABLE role DROP CONSTRAINT "role_pkey";
ALTER TABLE route DROP CONSTRAINT "route_pkey";
ALTER TABLE route_rate_card DROP CONSTRAINT "route_rate_card_pkey";
ALTER TABLE shortcut DROP CONSTRAINT "shortcut_pkey";
ALTER TABLE sure_tax_txn_log DROP CONSTRAINT "sure_tax_txn_log_pkey";
ALTER TABLE tab DROP CONSTRAINT "tab_pkey";
ALTER TABLE tab_configuration DROP CONSTRAINT "tab_configuration_pkey";
ALTER TABLE tab_configuration_tab DROP CONSTRAINT "tab_configuration_tab_pkey";
ALTER TABLE usage_pool DROP CONSTRAINT "usage_pool_pkey";
ALTER TABLE usage_pool_consumption_action DROP CONSTRAINT "usage_pool_con_action_pkey";
ALTER TABLE usage_pool_consumption_actions DROP CONSTRAINT "usage_pool_consumption_actions_pkey";
ALTER TABLE usage_pool_consumption_log DROP CONSTRAINT "usage_pool_consumption_log_pkey";
ALTER TABLE user_code DROP CONSTRAINT "user_code_pkey";
ALTER TABLE user_code_link DROP CONSTRAINT "user_code_link_pkey";
ALTER TABLE user_role_map DROP CONSTRAINT "user_role_map_compositekey";
ALTER TABLE user_status DROP CONSTRAINT "user_status_pkey";
ALTER TABLE validation_rule DROP CONSTRAINT "validation_rule_pkey";
ALTER TABLE ageing_entity_step DROP CONSTRAINT "entity_step_days";
--ALTER TABLE asset_entity_map DROP CONSTRAINT "asset_entity_map_uc_1";
ALTER TABLE customer_account_info_type_timeline DROP CONSTRAINT "customer_account_info_type_timeline_uk";
ALTER TABLE item_entity_map DROP CONSTRAINT "item_entity_map_uc_1";
ALTER TABLE item_price_timeline DROP CONSTRAINT "itmprctimelnprc_model_id_key";
ALTER TABLE meta_field_value DROP CONSTRAINT "meta_field_value_id_key";
ALTER TABLE payment_method_template DROP CONSTRAINT "payment_method_template_template_name_key";
ALTER TABLE plan_item_price_timeline DROP CONSTRAINT "plnitmprctimelnprc_mdl_key";
ALTER TABLE rate_card DROP CONSTRAINT "rate_card_table_name_key";
ALTER TABLE reset_password_code DROP CONSTRAINT "reset_password_code_base_user_id_key";


