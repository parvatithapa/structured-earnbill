package com.sapienter.jbilling.server.movius.integration;


/**
 * This is Movius specific constant class.
 */
public final class MoviusConstants {

	public static final String TIMEZONE = "Timezone";
	public static final String BILLING_PLAN_NAME = "Billing Plan Name";
	public static final String BILLING_PLAN_ID = "Billing Plan Id";
	public static final String ORG_ID = "Org Id";
	public static final String ITEM_ID_FOR_ORG_HIERARCHY = "Set Item Id for Org Hierarchy Order";
	public static final String ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY = "Set Account Type Id for Org Hierarchy User";
	public static final String XSD_FILE_NAME = "org-hierarchy.xsd";
	public static final String DONE_DIR_NAME = "done";
	public static final String FILE_RENAME_EXTENSION = "done.";
	public static final String ERROR_FILE_RENAME_EXTENSION = "error.";
	public static final String ORIGINATION_MAX_RETRY_COUNT = "Max Retry Count";
	public static final String RETRY_FILE_RENAME_EXTENSION = "retry.";
	public static final Integer DEFAULT_MAX_RETRY_COUNT = 5;

	public static final String ORG_DIR = "origination";
	public static final String ORIGINATION_XSD_FILE_NAME = "origination-charges.xsd";
	public static final String ORIGINATION_XML_PARAMETER_BASE_DIR = "XML_Base Directory";
	public static final String ORIGINATION_XSD_PARAMETER_BASE_DIR = "XSD_Base Directory";
	public static final String EMPTY = "";
	public static final String ORIGINATION_DATE_FORMAT = "yyyy-MM-dd";
	public static final String ORIGINATION_ITEM_ID = "Item ID: {} ";

	public static final String ORIGINATION_ERROR_XSD_VALIDATION_FAILED = "XSD validation failed for file: '%s' ";
	public static final String ORIGINATION_ERROR_DONE_DIR_NOT_FOUND = "Done Directory not found";
	public static final String ORIGINATION_ERROR_ITEM_NOT_FOUND = "Item not found for item Code: '%s'";
	public static final String ORIGINATION_ERROR_CUSTOMER_NOT_FOUND = "Customer not found for org-id: '%s'";
	public static final String ORIGINATION_ERROR_QUANTITY_IS_NEGATIVE = "Quantity '%s' is negative for org-id: '%s'";
	public static final String ORIGINATION_ERROR_ORDER_ALREADY_CREATED = "Order already created/updated for org-id: '%s'";
	public static final String ORIGINATION_ERROR_COMPANY_NOT_FOUND = "No company found with this system id: '%s'";
	public static final String ORIGINATION_ERROR_XML_FILE_NOT_FOUND = "No unparsed file found in directory: '%s' ";
	public static final String ORIGINATION_ERROR_XSD_FILE_NOT_FOUND = "XSD File does not exist: '%s'";
	public static final String ORIGINATION_ERROR_ORDER_CREATION_FAILED = "Order creation failed";
	public static final String ORIGINATION_ERROR_FILE_RENAMING_FAILED = "Rename failed";
	public static final String ORIGINATION_ERROR_INVALID_XML_DIR = "Plugin entity id '%s' and XML Base Directory '%s' are different. So exiting the plugin";
	public static final String ORIGINATION_ERROR_INVALID_XML_ENTITY = "Plugin entity id '%s' and XML File entity id '%s' are different.";


	public static final String ORGANIZATION_HIERARCHY_MAPPING_TASK_MESSAGE_KEY = "movius.org.hierarchy.mapping.task.error.alert";
	public static final String ORGANIZATION_ERROR_NO_UNPARSED_FILE_FOUND = "No unparsed file found in directory {} ";
	public static final String ORIGINATION_SUCCESS_FILE_NAME_CHANGED = "File name changed successfully to: {}";
	public static final String ORIGINATION_SUCCESS_ORDER_CREATED = "Order created, ID: {}";
	public static final String ORIGINATION_ERROR_COMPANY_ADMIN_EMAIL_NOT_FOUND = "Billing Administrator's Email Id not found for entityId: {}";
	public static final String ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY = "movius.org_create_update_task_error_alert";
	public static final String COMPANY_ADMIN_EMAIL_MF_NAME = "Billing Administrator's Email Id";
	public static final String ORIGINATION_ERROR_XML_FILE_PARSING_FAILED = "XML file parsing failed for file: '%s'";
	public static final String ORGANIZATION_HIERARCHY_MAPPING_TASK_MESSAGE_KEY_FOR_SUCCESS = "movius.org.hierarchy.mapping.task.success.alert";
	public static final String ORIGINATION_CREATE_UPDATE_TASK_MESSAGE_KEY_FOR_SUCCESS = "movius.org_create_update_task.success.alert";
	
	public static final String CURRENCY_ID = "currencyId";
	public static final String LANGUAGE_ID = "languageId";

	//Constants used in MoviusConfiguration
	public static final String DEFAULT_PRICING_RESOLUTION_STEP = "DefaultPricingResolutionStep";
	public static final String CALLING_ORG_ID = "Calling Org Id";
	public static final String CALLING_NUMBER = "Calling Number";
	public static final String CALLED_NUMBER_OC = "Called Number";
	public static final String CALLED_NUMBER_IC = "called Number";
	public static final String CALL_FROM = "Call from ";
	public static final String CALL_DURATION_IN_MINUTES = "Call duration in Minutes";
	public static final String CDR_TYPE = "cdr-type";
	public static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss";
	public static final String TIMESTAMP = "Timestamp";
	public static final String PRIMARY_NUMBER = "primary number";
	public static final String TO_NUMBER = "To Number";
	public static final String SMS_FROM = "SMS from ";
	public static final String SMS_QUANTITY = "SMS Quantity";
	public static final String FROM_NUMBER = "From Number";
	public static final String INCOMING_SMS_FROM = "In Coming SMS from ";



	private MoviusConstants() {
	    throw new IllegalStateException("Constants class");
	}

}
