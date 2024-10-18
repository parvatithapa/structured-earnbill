package com.sapienter.jbilling.server.mediation.converter.customMediations.fullCreative;

/**
 * @author Harshad Pathan
 */
public class FullCreativeConstants {

	// Mediation jobs configuration
    public static final String INBOUND_CALL_MEDIATION_CONFIGURATION = "inboundCallsMediationJobLauncher";
    public static final String ACTIVE_RESPONSE_MEDIATION_CONFIGURATION = "activeResponseMediationJobLauncher";
    public static final String CHAT_MEDIATION_CONFIGURATION = "chatMediationJobLauncher";
    public static final String IVR_MEDIATION_CONFIGURATION = "ivrMediationJobLauncher";
    public static final String SPANISH_MEDIATION_CONFIGURATION = "spanishMediationJobLauncher";
    public static final String SUPERVISOR_MEDIATION_CONFIGURATION = "supervisorMediationJobLauncher";
    public static final String CALL_RELAY_MEDIATION_CONFIGURATION = "callRelayMediationJobLauncher";
    public static final String LIVE_RECEPTION_MEDIATION_CONFIGURATION = "liveReceptionMediationJobLauncher";
    public static final String FC_MEDIATION_CONFIGURATION = "fcMediationJobLauncher";

    // Recycle jobs configuration
    public static final String INBOUND_CALL_RECYCLE_CONFIGURATION = "inboundCallsRecycleJobLauncher";
    public static final String FC_RECYCLE_CONFIGURATION = "fcRecycleJobLauncher";
    public static final String ACTIVE_RESPONSE_RECYCLE_CONFIGURATION = "activeResponseRecycleJobLauncher";
    public static final String CHAT_RECYCLE_CONFIGURATION = "chatRecycleJobLauncher";
    public static final String IVR_RECYCLE_CONFIGURATION = "ivrRecycleJobLauncher";
    public static final String SPANISH_RECYCLE_CONFIGURATION = "spanishRecycleJobLauncher";
    public static final String SUPERVISOR_RECYCLE_CONFIGURATION = "supervisorRecycleJobLauncher";
    public static final String CALL_RELAY_RECYCLE_CONFIGURATION = "callRelayRecycleJobLauncher";
    public static final String LIVE_RECEPTION_RECYCLE_CONFIGURATION = "liveReceptionRecycleJobLauncher";

    // Common mediation beans configuration
    public static final String JMR_DEFAULT_WRITER_BEAN = "jmrDefaultWriter";

    public static final String INBOUND_CALL_MEDIATION_CONVERTER_BEAN = "inboundCallMediationConverter";
    public static final String CHAT_MEDIATION_CONVERTER_BEAN = "chatMediationConverter";
    public static final String ACTIVE_RESPONSE_MEDIATION_CONVERTER_BEAN = "activeResponseMediationConverter";
    public static final String FC_MEDIATION_CONVERTER_BEAN = "fcRecordLineConverter";

    public static final String INBOUND_CALL_MEDIATION_CDR_RESOLVER_BEAN = "inboundCallRecordMediationCdrResolver";
    public static final String CHAT_MEDIATION_CDR_RESOLVER_BEAN = "chatRecordMediationCdrResolver";
    public static final String ACTIVE_RESPONSE_MEDIATION_CDR_RESOLVER_BEAN = "activeResponseMediationCdrResolver";
    public static final String FC_MEDIATION_CDR_RESOLVER_BEAN = "fcCdrProcessor";

    public static final String FILE_INBOUND_IDENTIFIER = "Inbound";
    public static final String FILE_OUTBOUND_IDENTIFIER = "Outbound";
    public static final String FILE_ACTIVE_IDENTIFIER = "AR";
    public static final String FILE_CHAT_IDENTIFIER = "CHAT";
    public static final String FILE_IVR_IDENTIFIER = "IVR";
    public static final String FILE_WEBFORM_CALL_IDENTIFIER = "Webform";
    public static final String FILE_VOICE_MAIL_IDENTIFIER = "Voicemail";
    public static final String FILE_SPANISH_IDENTIFIER = "Spanish";
    public static final String FILE_SUPERVISOR_IDENTIFIER = "Supervisor";
    public static final String FILE_CALL_RELAY_IDENTIFIER = "CR";
    public static final String FILE_LIVE_RECEPTION_IDENTIFIER = "LR";

    public static final String ACTIVE_RESPONSE_FOLDER_NAME = "AR/";
    public static final String CHAT_FOLDER_NAME = "chat/";
    public static final String INBOUND_CALLS_FOLDER_NAME = "inbound/";
    public static final String SPANISH_FOLDER_NAME = "spanish/";
    public static final String SUPERVISOR_FOLDER_NAME = "supervisor/";
    public static final String LIVE_RECEPTION_FOLDER_NAME = "livereception/";
    public static final String CALL_RELAY_FOLDER_NAME = "callrelay/";
    public static final String IVR_FOLDER_NAME = "ivr/";

    //These are FC specific MetaFieldsName
    public enum MetaFieldName {

        LIVE_ANSWER_ITEM("Set ItemId For Live Answer"),
		INBOUND_CALL_ITEM("Set ItemId For InBound Calls") ,
		ACTIVE_RESPONSE_ITEM("Set ItemId For Active Response") ,
		CHAT_ITEM("Set ItemId For Chat") ,
		PAYPAL_BILLING_GROUP_NAME("Billing Contact Info Group Name"),
		IVR_ITEM("Set ItemId For IVR"),
		INBOUND_CALL_TYPE("Inbound Call Type"),
		CHAT_CALL_TYPE("Chat Call Type"),
		ACTIVE_RESPONSE_CALL_TYPE("Active Response Call Type"),
		SPANISH_CALL_TYPE("Spanish (Inbound) Call Type"),
		SUPERVISOR_CALL_TYPE("Supervisor (Inbound) Call Type"),
		CALL_RELAY_CALL_TYPE("Call Relay (Inbound) Call Type"),
		LIVE_RECEPTION_CALL_TYPE("Live Reception Call Type"),
		IVR_CALL_TYPE("IVR (Incoming) Call Type"),
		MOBILE_APP_CALL_TYPE("Mobile App (Outbound) Call Type"),
		VOICE_MAIL_CALL_TYPE("Voicemail Call Type"),
		WEB_FORM_CALL_TYPE("Webform to Call (IVR) Call Type"),
		NOTIFICATION_MAILING_ADDRESS("Mailing Contact Info Group Name"),
		NPA_NXX_TABEL_NAME("NPA NXX Table Name"),
		DORMANCY_PLAN_ID("Dormancy plan Id");

        public static final String TAX_SCHEME = "Tax Scheme";
        public static final String TAX_TABLE_NAME = "Tax_Table_Name";

		private String metaFieldName;

		private MetaFieldName(String metaFieldName) {
			this.metaFieldName = metaFieldName;
		}

		public String getMetaFieldName() {
			return metaFieldName;
		}

	}

}
