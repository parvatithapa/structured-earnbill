package com.sapienter.jbilling.server.ignition;

/**
 * Created by Taimoor Choudhary on 7/31/17.
 */
public class IgnitionConstants {

    public static final String ABSA_BASE_FOLDER = "absa_payments";
    public static final String ABSA_RESPONSE_FOLDER = "response";
    public static final String ABSA_TIME_FORMAT = "HH:mm MM/dd/uuuu";
    public static final String ABSA_DATE_FORMAT = "yyMMdd";
    public static final String ABSA_TRANSMISSION_DATE_FORMAT = "yyyyMMdd";
    public static final String PARAMETER_HOLIDAY = "holiday";
    public static final String PARAMETER_DEBIT_DATE_HOLIDAY = "debit date update";
    public static final String SERVICE_PROVIDER_ABSA_NAEDO = "ABSA NAEDO";
    public static final String SERVICE_PROVIDER_ABSA = "ABSA";
    public static final String SERVICE_PROVIDER_STANDARD_BANK = "Standard Bank";
    public static final String DATA_TABLE_NAME = "Service_Profiles";
    public static final String ABSA_OUTPUT_FILE_NAME_DATE_FORMAT = "yyMMddhhmm";
    public static final String ABSA_PROCESSED_FILE_NAME_DATE_FORMAT = "yyMMddhhmmss";
    public static final String IGNITION_ARCHIVE_FOLDER = "Archive";
    public static final String IGNITION_SCHEDULED_PAYMENT_NOTE = "Created from Ignition Payment Scheduling plugin";

    // Payment Instrument
    public static final String POLICY_NUMBER = "Policy Number";
    public static final String PAYMENT_NEXT_PAYMENT_DATE = "Next Payment Date";
    public static final String PAYMENT_BRANCH_CODE = "Branch Code";
    public static final String PAYMENT_ACCOUNT_NAME = "Account Name";
    public static final String PAYMENT_ACCOUNT_NUMBER = "Account Number";
    public static final String PAYMENT_BANK_NAME = "Bank Name";
    public static final String PAYMENT_ACCOUNT_TYPE = "Account Type";

    // Payment Meta-Fields
    public static final String PAYMENT_ACTION_DATE = "Action Date";
    public static final String PAYMENT_TRANSMISSION_DATE = "Transmission Date";
    public static final String PAYMENT_SEQUENCE_NUMBER = "Sequence Number";
    public static final String PAYMENT_USER_REFERENCE = "User Reference";
    public static final String PAYMENT_CONTRACT_REFERENCE = "Contract Reference";
    public static final String PAYMENT_TRANSACTION_NUMBER="Transaction Number";
    public static final String PAYMENT_SENT_ON = "Payment Sent On";
    public static final String PAYMENT_DATE="Bank Processing Date";
    public static final String PAYMENT_CLIENT_CODE="Payment Client Code";
    public static final String PAYMENT_TYPE = "Payment Type";
    public static final String PAYMENT_NAEDO_TYPE = "NAEDO Type";
    public static final String PAYMENT_TRACKING_DAYS = "Tracking Days";
    public static final String PAYMENT_DEBIT_DAY = "Debit Day";

    //Ignition
    public static final String SB_INSTALLATION_HEADER_RECORD_IDENTIFIER = "02";
    public static final String SB_INSTALLATION_HEADER_VOLMUE_NO = "1001";
    public static final String SB_INSTALLATION_HEADER_TAPE_SERIAL_NO = "00000001";
    public static final String SB_INSTALLATION_HEADER_INSTALLATION_CODE="0021";
    public static final String SB_INSTALLATION_HEADER_INSTALLATION_GENERATION_NO="0000";
    public static final String SB_INSTALLATION_HEADER_BLOCK_LENGTH = "1800";
    public static final String SB_INSTALLATION_HEADER_RECORD_LENGTH = "0180";
    public static final String SB_INSTALLATION_HEADER_SERVICE = "MAGTAPE";
    public static final String SB_USER_HEADER_RECORD = "04";
    public static final Integer SB_STANDARD_TRANSACTION_RECORD_IDENTIFIER_CREDIT = 10;
    public static final Integer SB_STANDARD_TRANSACTION_RECORD_IDENTIFIER_DEBIT= 50;
    public static final Integer SB_STANDARD_TRANSACTION_RECORD_HOMING_INSTITUTE = 21;
    public static final Integer SB_CONTRA_RECORD_INDENTIFIER_CREDIT=12;
    public static final Integer SB_CONTRA_RECORD_INDENTIFIER_DEBIT=52;
    public static final String SB_USER_TRAILER_RECORD = "92";
    public static final String SB_INSTALLATION_TRAILER_RECORD_IDENTIFIER = "94";
    public static final String SB_AUDIT_FILE_HEADER_RECORD_IDENTIFIER = "SB";
    public static final String SB_AUDIT_FILE_DETAIL_RECORD_IDENTIFIER = "SM";
    public static final String SB_VET_AND_UNPAID_FILE_HEADER_RECORD_IDENTIFIER = "FH";
    public static final String SB_VET_AND_UNPAID_FILE_TRANSACTION_RECORD_IDENTIFIER_CREDIT = "21";
    public static final String SB_VET_AND_UNPAID_FILE_TRANSACTION_RECORD_IDENTIFIER_DEBIT = "61";
    public static final String SB_VET_AND_UNPAID_FILE_CONSOLIDATION_RECORD_IDENTIFIER_CREDIT = "20";
    public static final String SB_VET_AND_UNPAID_FILE_CONSOLIDATION_RECORD_IDENTIFIER_DEBIT = "60";
    public static final String METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER = "Next Payment Date";
    public static final String METAFIELD_ORIGINAL_NEXT_PAYMENT_DATE_INDENTIFIER = "Original Next Payment Date";
    public static final String SB_RESPONSE_FILES_FOLDER = "Response_Files";
    public static final String SB_UNPAID_FILE_HEADER_IDENTIFIER = "AUNP";
    public static final String SB_INTERIM_FILE_IDENTIFIER="INTAUD";
    public static final String SB_FINAL_AUDIT_FILE_IDENTIFIER="FINAUD";
    public static final String SB_UNPAID_FILE_IDENTIFIER="UNPDATA";
    public static final String SB_VET_FILE_IDENTIFIER="VETDATA";

    // User Meta-Fields
    public static final String USER_ACTION_DATE = "Action Date";
    public static final String USER_IN_NAEDO = "In NAEDO";
    public static final String USER_FAILED_PAYMENT_COUNT = "Payment Fail Count";
    public static final String USER_NAEDO_TYPE = "NAEDO Type";
    public static final String USER_LAST_NAEDO_RESULT = "Last NAEDO Result";
    public static final String USER_LOWER_LEVEL_NAEDO_COUNT = "Lower level NAEDO count";

    //Item
    public static final String Brand_Name = "Brand Name";
    public static final String NUMBER_OF_TIMES_TO_LAPSE = "NumberofTimestoLapse";

    // Order
    public static final String FAILED_PAYMENTS_COUNT = "Failed Payments Count";
    public static final String ORDER_STATUS_LAPSED = "Lapsed";

    //Standard Bank Output file error Code
    public static final String SB_OUTPUT_FILE_ERROR_INVALID_RECORD_TYPE = "01";
    public static final String SB_OUTPUT_FILE_ERROR_INVALID_ACB_CODE = "03";
    public static final String SB_OUTPUT_FILE_ERROR_INVALID_CATS_USER_ID = "04";
    public static final String SB_OUTPUT_FILE_ERROR_NOT_LINKED_TO_BEFT_SYSTEM="05";
    public static final String SB_OUTPUT_FILE_ERROR_ACB_HEADER_SEQUENCE_NUMBER_INCORRECT="06";
    public static final String SB_OUTPUT_FILE_ERROR_TRANSACTION_SUCCESSFUL = "VA";

    public static final String CUSTOMER_ACCOUNT_STATUS_SUSPEND = "Suspend";

    //Ignition Customer Suspension Task
    public static final String OUTPUT_FILE="OUTPUT";
    public static final String VET_FILE="VET";
    public static final String UNPAID_FILE="UNPAID";
    public static final String INTERIM_FILE="INTERIM";
    public static final String FINAL_AUDIT_FILE="FINAL";

    //Transmission Failure Task
    public static final String ABSA_PAYMENTS_FAILED_NOTIFICATION = "ABSA Payments Failed Notification";
    public static final String STANDARD_BANK_PAYMENTS_FAILED_NOTIFICATION = "Standard Bank Payments Failed Notification";

    public enum TransactionType{
        Credit_Transaction,
        Debit_Transaction
    }

    public enum AccountType{
        Account_Type_Current,
        Account_Type_Savings,
        Account_Type_Transmission,
        Account_Type_Bond,
        Account_Type_Subscription_Share
    }

    public enum NAEDO{
        Naedo_Request,
        Naedo_Recall
    }

    public enum ServiceType{

        SAME_DAY("SAMEDAY"),
        TWO_DAY("TWO DAY");

        private final String name;

        ServiceType(String name) {
            this.name = name;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return name;
        }
    }

    public enum NAEDOResponseCode {

        TRANSACTION_SUCCESSFUL("00"),
        RECALL_SUCCESSFUL("E8");

        private final String name;

        NAEDOResponseCode(String name) {
            this.name = name;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }
    }

    public enum ABSAResponseFileType{

        EFT("001"),
        EFT_PAYMENT_AGAINT_AVAILABLE_FUNDS("020"),
        NAEDO("050");

        private final String name;

        ABSAResponseFileType(String name) {
            this.name = name;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return name;
        }
    }

    public static enum PaymentStatus{
        SB_TRANSMISSION_FAILURE,
        SB_TRANSACTION_FAILURE,
        ABSA_ACCEPTED,
        ABSA_REJECTED
    }

    public enum NAEDOWorkflowType{

        NONE("None"),
        UPPER("Upper"),
        MIDDLE("Middle"),
        LOWER("Lower");

        private final String name;

        NAEDOWorkflowType(String name) {
            this.name = name;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return name;
        }

        public static NAEDOWorkflowType getNAEDOWorkflowType(String naedoType){

            switch (naedoType.toLowerCase()){
                case "upper":
                    return IgnitionConstants.NAEDOWorkflowType.UPPER;
                case "middle":
                    return IgnitionConstants.NAEDOWorkflowType.MIDDLE;
                case "lower":
                    return IgnitionConstants.NAEDOWorkflowType.LOWER;
                default:
                    return IgnitionConstants.NAEDOWorkflowType.NONE;
            }
        }
    }

    public enum LastNAEDOResult{

        NONE("N/A"),
        PAID("Successful"),
        UNPAID("Failed");

        private final String name;

        LastNAEDOResult(String name) {
            this.name = name;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return name;
        }

        public static LastNAEDOResult getNAEDOResultType(String naedoType){

            switch (naedoType.toLowerCase()){
                case "successful":
                    return IgnitionConstants.LastNAEDOResult.PAID;
                case "failed":
                    return IgnitionConstants.LastNAEDOResult.UNPAID;
                default:
                    return IgnitionConstants.LastNAEDOResult.NONE;
            }
        }
    }

    public enum IgnitionPaymentType{

        EFT("EFT"),
        NAEDO("NAEDO");

        private final String name;

        IgnitionPaymentType(String name) {
            this.name = name;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return name;
        }
    }

    public enum NAEDOPaymentTracking{

        NO_TRACKING("No Tracking", "12"),
        ONE_DAY_TRACKING("01 Day Tracking", "13"),
        TWO_DAY_TRACKING("02 Day Tracking", "01"),
        THREE_DAY_TRACKING("03 Day Tracking", "14"),
        FOUR_DAY_TRACKING("04 Day Tracking", "02"),
        FIVE_DAY_TRACKING("05 Day Tracking", "03"),
        SIX_DAY_TRACKING("06 Day Tracking", "04"),
        SEVEN_DAY_TRACKING("07 Day Tracking", "15"),
        EIGHT_DAY_TRACKING("08 Day Tracking", "05"),
        NINE_DAY_TRACKING("09 Day Tracking", "06"),
        TEN_DAY_TRACKING("10 Day Tracking", "07"),
        FOURTEEN_DAY_TRACKING("14 Day Tracking", "16"),
        TWENTY_ONE_DAY_TRACKING("21 Day Tracking", "17"),
        THIRTY_TWO_DAY_TRACKING("32 Day Tracking", "18");

        private final String name;
        private final String value;

        NAEDOPaymentTracking(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public boolean equalsName(String otherName) {
            return otherName != null && name.equalsIgnoreCase(otherName);
        }

        public String toString() {
            return this.name;
        }

        public String getName() {
            return name;
        }

        public String getValue(){
            return this.value;
        }

        public static NAEDOPaymentTracking getNAEDOPaymentTracking(String naedoTracking){

            switch (naedoTracking.toLowerCase()){
                case "no tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.NO_TRACKING;
                case "01 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.ONE_DAY_TRACKING;
                case "02 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.TWO_DAY_TRACKING;
                case "03 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.THREE_DAY_TRACKING;
                case "04 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.FOUR_DAY_TRACKING;
                case "05 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.FIVE_DAY_TRACKING;
                case "06 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.SIX_DAY_TRACKING;
                case "07 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.SEVEN_DAY_TRACKING;
                case "08 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.EIGHT_DAY_TRACKING;
                case "09 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.NINE_DAY_TRACKING;
                case "10 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.TEN_DAY_TRACKING;
                case "14 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.FOURTEEN_DAY_TRACKING;
                case "21 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.TWENTY_ONE_DAY_TRACKING;
                case "32 day tracking":
                    return IgnitionConstants.NAEDOPaymentTracking.THIRTY_TWO_DAY_TRACKING;
                default:
                    return null;
            }
        }

    }
}
