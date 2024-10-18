package com.sapienter.jbilling.test.framework.builders.nges;

import com.sapienter.jbilling.server.ediTransaction.EDITypeWS;
import com.sapienter.jbilling.server.fileProcessing.FileConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.AccountTypeBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import static com.sapienter.jbilling.server.fileProcessing.FileConstants.*;

/**
 * Created by marcomanzicore on 27/11/15.
 */
@org.testng.annotations.Test(groups = {"integration", "nges"})
public class NGESBuilder {
    private TestEnvironmentBuilder testEnvironmentCreator;
    JbillingAPI escoAPI;    //Parent company which  is called ESCO in energy industry.
    JbillingAPI ldcAPI;     // Child company which is called LDC in energy industry.


    public static final String CATEGORY_NAME = "Commodity";
    public static final String ELECTRICITY_PRODUCT_NAME = "Electricity";
    public static final String ELECTRICITY_PRODUCT_RATE = "0.1030";
    public static final String ELECTRICITY_PLAN_ITEM_NAME = "Golden Plan";
    public static final String ELECTRICITY_PLAN_RATE = "0.0";
    public static final String ELECTRICITY_PLAN_CODE = "Plan-Golden Plan";
    public static final String ELECTRICITY_PLAN_ITEM_RATE = "0.07";

    public enum CompanyType {
        ESCO,
        LDC
    }

    public enum EDI {
        ENROLLMENT_READ("814_Customer_Enrollment", "814"),
        METER_READ("867_Meter_Read", "867"),
        CHANGE_REQUEST("814_Customer_Change_Request", "814CQ"),
        PAYMENT_READ("820_Payment_Read", "820"),
        CUSTOMER_TERMINATION("814_Customer_Termination", "t814"),
        INVOICE_READ("810_Invoice_Read", "810");

        String name;
        String ediSuffix;

        EDI(String name, String ediSuffix) {
            this.name = name;
            this.ediSuffix = ediSuffix;
        }

        public String getName() {
            return name;
        }

        public String getEdiSuffix() {
            return ediSuffix;
        }
    }

    public final static Map<String, Object> metaFieldValues = new HashMap<String, Object>() {{
        put(BILLING_MODEL, BILLING_MODEL_RATE_READY);
        put(DURATION, "12");
        put(INTERVAL_USAGE_REQUIRED_METAFIELD, false);
        put(SEND_RATE_CHANGE_DAILY, false);
        put(EARLY_TERMINATION_FEE_AMOUNT_META_FIELD, new BigDecimal(10));
    }};

    public NGESBuilder(TestEnvironmentBuilder creator) {
        this.testEnvironmentCreator = creator;
        escoAPI = testEnvironmentCreator.getPrancingPonyApi();
        ldcAPI = testEnvironmentCreator.getResellerApi();
    }

    /*
    * Create account types according to NGES requirement
    * */
    public NGESBuilder buildAccountType(NGESAccountTypeBuild.AccountType accountType, CompanyType type) {
        JbillingAPI api = type.equals(CompanyType.ESCO) ? escoAPI : ldcAPI;
        AccountTypeBuilder accountTypeBuilder = testEnvironmentCreator.accountTypeBuilder(api);
        NGESAccountTypeBuild builder = new NGESAccountTypeBuild(accountTypeBuilder);

        if (accountType.equals(NGESAccountTypeBuild.AccountType.Residential)) {
            builder.buildResidentialAccountType();
        } else if (accountType.equals(NGESAccountTypeBuild.AccountType.Commercial)) {
            builder.buildCommercialAccountType();
        }

        if (type.equals(CompanyType.LDC)) {
            builder.addAccountInformationAIT();
        }

        accountTypeBuilder.build();

        return this;
    }

    /*
    * Create category and item in it.
    * */
    public NGESBuilder buildProducts() {
        ItemBuilder itemBuilder = testEnvironmentCreator.itemBuilder(ldcAPI);
        Integer testCategoryId = itemBuilder.itemType().withCode(CATEGORY_NAME).useExactCode(true).build();
        itemBuilder.item().withType(testCategoryId)
                .withCode(ELECTRICITY_PRODUCT_NAME)
                .useExactCode(true)
                .withFlatPrice(ELECTRICITY_PRODUCT_RATE)
                .withMetaField(FileConstants.COMMODITY, "E")
                .build();
        return this;
    }

    public NGESBuilder buildPlanLevelMetaFields() {
//todo enumeration with same name as of meta field. So, having issue with current TestEnvironment flow. Enumeration is not necessary right now.
//        testEnvironmentCreator.configurationBuilder(ldcAPI)
//                .addEnumeration(BILLING_MODEL, BILLING_MODEL_RATE_READY, BILLING_MODEL_BILL_READY, BILLING_MODEL_DUAL)
//                .addEnumeration(DURATION, "12", "24")
//                .build();

        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField(BILLING_MODEL, DataType.ENUMERATION, EntityType.PLAN)
                .addMetaField(DURATION, DataType.ENUMERATION, EntityType.PLAN)
                .addMetaField(INTERVAL_USAGE_REQUIRED_METAFIELD, DataType.BOOLEAN, EntityType.PLAN)
                .addMetaField(SEND_RATE_CHANGE_DAILY, DataType.BOOLEAN, EntityType.PLAN)
                .addMetaField(EARLY_TERMINATION_FEE_AMOUNT_META_FIELD, DataType.DECIMAL, EntityType.PLAN)
                .build();
        return this;
    }

    public NGESBuilder buildPlan(Map<String, Object> metaFieldValues) {
        ItemBuilder itemBuilder = testEnvironmentCreator.itemBuilder(ldcAPI);
        Integer itemId = ldcAPI.getItemID(ELECTRICITY_PRODUCT_NAME);
        ItemDTOEx item = ldcAPI.getItem(itemId, null, null);
        Integer planItemId = itemBuilder.item().withType(item.getTypes()[0])
                .withCode(ELECTRICITY_PLAN_ITEM_NAME)
                .useExactCode(true)
                .withFlatPrice(ELECTRICITY_PLAN_RATE)
                .build();

        ItemBuilder.PlanBuilder builder = itemBuilder.plan()
                .withPlanItem(planItemId)
                .withCode(ELECTRICITY_PLAN_CODE)
                .addBundleItem(itemId, new BigDecimal(ELECTRICITY_PLAN_ITEM_RATE));
        metaFieldValues.entrySet().stream().forEach(e -> builder.withMetaField(e.getKey(), e.getValue()));
        builder.build();
        return this;
    }

    public NGESBuilder buildEDIEnrollment() {
        EDITypeWS enrollment = testEnvironmentCreator.ediTypeBuilder(ldcAPI)
                .withName(EDI.ENROLLMENT_READ.getName())
                .withEdiSuffix(EDI.ENROLLMENT_READ.getEdiSuffix())
                .withEDIStatuses("Ready to send", "Send immediately", "Accepted", "Internal Error", "Acknowledged", "File transfer error", "Invalid File", "Sent to LDC")
                .withEDIStatusAndExceptionCodes("Rejected", "0001", "0002", "0014", "0026", "0027", "0028", "0031", "0032", "0033", "0034", "0035", "0039", "0043", "A03", "A76", "A77", "A79", "A91", "CW2", "CW5", "DIV", "EAS", "ESGMDM", "FRB", "FRC", "FRF", "FRG", "IBO", "MAR", "MVE", "RNE", "RRC", "RRPM", "ZIP", "0003", "0004", "00016", "0017", "0018", "00019", "0024", "0030", "0040", "008", "8", "A03", "A78", "ANL", "B30", "CAB", "ESGDUP", "ESGIDT", "I1J", "IGP", "IMI", "OA", "RWD", "SBD", "UND", "UNE", "UNS", "WIP", "UNE", "UNS", "0020", "SSR", "TEI", "W05", "0006", "ANE", "ANK", "ANM", "API", "B33")
                .build();

        // Add value to company level meta field ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME
        testEnvironmentCreator.companyBuilder(ldcAPI)
                .withMetaField(FileConstants.ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME, enrollment.getId())
                .update();
        return this;
    }

    public NGESBuilder buildMeterReadEDIType() {
        EDITypeWS meterReadType = testEnvironmentCreator.ediTypeBuilder(ldcAPI)
                .withName(EDI.METER_READ.getName())
                .withEdiSuffix(EDI.METER_READ.getEdiSuffix())
                .withEDIStatuses("Historical Meter Read", "Rejected", "Acknowledged", "Deprecated", "Invalid Data", "Done", "EXP002", "EXP001")
                .withEDIStatusAndExceptionCodes("EXP001", "JBE123")
                .withEDIStatusAndExceptionCodes("EXP002", "JBE142")
                .withEDIStatusAndExceptionCodes("Rejected", "JBE101", "JBE102")
                .withEDIStatusAndExceptionCodes("Invalid Data", "JBE103","JBE104","JBE121","JBE124","JBE143","JBE144","JBE105","JBE106","JBE107","JBE122","JBE140","JBE141")
                .build();

        // Add value to company level meta field ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME
        testEnvironmentCreator.companyBuilder(ldcAPI)
                .withMetaField(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME, meterReadType.getId())
                .update();
        return this;
    }

    public NGESBuilder buildChangeRequestEDIType() {
        EDITypeWS changeRequest = testEnvironmentCreator.ediTypeBuilder(ldcAPI)
                .withName(EDI.CHANGE_REQUEST.getName())
                .withEdiSuffix(EDI.CHANGE_REQUEST.getEdiSuffix())
                .withEDIStatuses("Invalid Data", "Ready to send", "Done", "Deprecated", "Rejected", "Sent to LDC")
                .withEDIStatusAndExceptionCodes("Rejected", "API", "0034")
                .withEDIStatusAndExceptionCodes("Invalid Data", "B38", "0003", "API", "0034")
                .build();

        // Add value to company level meta field ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME
        testEnvironmentCreator.companyBuilder(ldcAPI)
                .withMetaField(FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME, changeRequest.getId())
                .update();
        return this;
    }

    public NGESBuilder buildInvoiceReadEDIType() {
        EDITypeWS invoiceReadType = testEnvironmentCreator.ediTypeBuilder(ldcAPI)
                .withName(EDI.INVOICE_READ.getName())
                .withEdiSuffix(EDI.INVOICE_READ.getEdiSuffix())
                .withEDIStatuses("EXP001", "Rejected", "Acknowledged", "EXP002", "Accepted", "Deprecated", "Invalid File", "MisMatch", "Ready to send", "Sent to LDC")
                .withEDIStatusAndExceptionCodes("Invalid File", "JBE201","JBE202","JBE203","JBE204","JBE205","JBE206","JBE207","JBE221","JBE222","JBE223","JBE224","JBE241","JBE242","JBE243","JBE244","JEB245","JBE246")
                .build();

        // Add value to company level meta field ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME
        testEnvironmentCreator.companyBuilder(ldcAPI)
                .withMetaField(FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME, invoiceReadType.getId())
                .update();
        return this;
    }

    public NGESBuilder buildPaymentReadEDIType() {
        EDITypeWS paymentReadType = testEnvironmentCreator.ediTypeBuilder(ldcAPI)
                .withName(EDI.PAYMENT_READ.getName())
                .withEdiSuffix(EDI.PAYMENT_READ.getEdiSuffix())
                .withEDIStatuses("Acknowledged", "INVALID_DATA", "DONE", "EXP002", "Rejected", "Invalid Payment Transaction", "DUPLICATE_PAYMENT", "INCONSISTENT_PAYMENT")
                .withEDIStatusAndExceptionCodes("Invalid File", "JBE201","JBE202","JBE203","JBE204","JBE205","JBE206","JBE207","JBE221","JBE222","JBE223","JBE224","JBE241","JBE242","JBE243","JBE244","JEB245","JBE246")
                .build();

        // Add value to company level meta field ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME
        testEnvironmentCreator.companyBuilder(ldcAPI)
                .withMetaField(FileConstants.PAYMENT_EDI_TYPE_ID_META_FIELD_NAME, paymentReadType.getId())
                .update();
        return this;
    }

    public NGESBuilder buildCustomerTerminationEDIType() {
        EDITypeWS paymentReadType = testEnvironmentCreator.ediTypeBuilder(ldcAPI)
                .withName(EDI.CUSTOMER_TERMINATION.getName())
                .withEdiSuffix(EDI.CUSTOMER_TERMINATION.getEdiSuffix())
                .withEDIStatuses("Accepted", "Acknowledged", "Invalid File", "Rejected", "Ready to send", "Sent to LDC")
                .build();

        // Add value to company level meta field ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME
        testEnvironmentCreator.companyBuilder(ldcAPI)
                .withMetaField(FileConstants.TERMINATION_EDI_TYPE_ID_META_FIELD_NAME, paymentReadType.getId())
                .update();
        return this;
    }

    private File getSampleFile(String fileName) {
        String path = Thread.currentThread()
                .getContextClassLoader().getResource(fileName).getPath();
        return new File(path);
    }



    private void addPluginsForChildCompany(ConfigurationBuilder configurationBuilder) {
        configurationBuilder

                .addPlugin("com.sapienter.jbilling.server.customerEnrollment.task.BrokerResponseManagerTask")
                .addPlugin("com.sapienter.jbilling.server.customerEnrollment.task.BrokerCatalogCreatorTask")

                .addPluginWithParameters("com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask",
                        new Hashtable<String, String>() {{
                            put("replacement_status", "EXP002");
                            put("done_status", "Done");
                            put("historical_status", "Historical Meter Read");
                            put("cancellation_status", "EXP001");
                            put("rejected_status", "Rejected");
                            put("invalid_data_status", "Invalid Data");
                            put("cron_exp", "20 1/3 * * * ?");
                        }})
                .addPluginWithParameters("com.sapienter.jbilling.server.ediTransaction.task.PaymentParserTask",
                        new Hashtable<String, String>() {{
                            put("inconsistent_payment_status", "INCONSISTENT_PAYMENT");
                            put("rejected", "Rejected");
                            put("invalid_data", "INVALID_DATA");
                            put("duplication_transaction", "DUPLICATE_PAYMENT");
                            put("done", "DONE");
                            put("cron_exp", "30 2/3 * * * ?");
                        }})
                .addPluginWithParameters("com.sapienter.jbilling.server.earlyTermination.task.CustomerTerminationTask",
                        new Hashtable<String, String>() {{
                            put("accept_status", "Accepted");
                            put("reject_status", "Rejected");
                            put("invalid_file_status", "Invalid File");
                            put("cron_exp", "40 1/3 * * * ?");
                        }})
                .addPluginWithParameters("com.sapienter.jbilling.server.ediTransaction.invoiceRead.InvoiceReadTask",
                        new Hashtable<String, String>() {{
                            put("replacement_status", "EXP002");
                            put("file_mismatch_status", "MisMatch");
                            put("invalid_file_status", "Invalid File");
                            put("accepted_status", "Accepted");
                            put("cancellation_status", "EXP001");
                            put("rejected_status", "Rejected");
                            put("meter_file_status", "Done");
                            put("deprecated_status", "Deprecated");
                            put("cron_exp", "50 0/3 * * * ?");
                        }})
                .addPluginWithParameters("com.sapienter.jbilling.server.ediTransaction.task.AcknowledgementParserTask",
                        new Hashtable<String, String>() {{
                            put("accept_status", "Accepted");
                            put("reject_status", "Rejected");
                            put("invalid_file_status", "Invalid File");
                            put("acknowledge_status", "Acknowledged");
                            put("cron_exp", "45 1/3 * * * ?");
                        }})
                .addPluginWithParameters("com.sapienter.jbilling.server.customerEnrollment.task.BulkEnrollmentReaderTask",
                        new Hashtable<String, String>() {{
                            put("cron_exp", "0 2/3 * * * ?");
                        }});
    }


    private void addEnumerationsForChildCompany(ConfigurationBuilder configurationBuilder) {
        configurationBuilder
                .addEnumeration("Termination", "Termination Processing", "Dropped", "Esco Initiated", "Esco Rejected")
                .addEnumeration("Billing Model", "Rate Ready", "Bill Ready", "Dual", "Supplier Consolidated")
                .addEnumeration("Notification Method", "Email", "Paper", "Both");
    }

    public NGESBuilder buildChildCompanyMF() {
        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField("BlackList", DataType.BOOLEAN, EntityType.CUSTOMER)
                .addMetaField(FileConstants.CUSTOMER_RATE_METAFIELD_NAME, DataType.DECIMAL, EntityType.CUSTOMER)
                .addMetaField("CYCLE_NUMBER", DataType.INTEGER, EntityType.CUSTOMER)
                .addMetaField("LAST_ENROLLMENT", DataType.DATE, EntityType.CUSTOMER)
                .addMetaField("Termination", DataType.ENUMERATION, EntityType.CUSTOMER)


                .addMetaField("edi_file_id", DataType.STRING, EntityType.ORDER)

                .addMetaField("INVOICE_NR", DataType.STRING, EntityType.INVOICE)
                .addMetaField("Suretax Response Trans Id", DataType.INTEGER, EntityType.INVOICE)
                .addMetaField("Meter read file", DataType.INTEGER, EntityType.INVOICE)

                .addMetaField("Billing Model", DataType.ENUMERATION, EntityType.PLAN)
                .addMetaField("DIVISION", DataType.ENUMERATION, EntityType.PLAN);

        return this;
    }

    public NGESBuilder buildProductLevelMetaFields() {
        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField(FileConstants.COMMODITY, DataType.STRING, EntityType.PRODUCT)
                .build();
        return this;
    }

    public NGESBuilder buildOrderLevelMetaFields() {
        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField("edi_file_id", DataType.STRING, EntityType.ORDER)
                .addMetaField(FileConstants.IS_REBILL_ORDER, DataType.BOOLEAN, EntityType.ORDER)
                .build();
        return this;
    }

    public NGESBuilder buildInvoiceLevelMetaFields() {
        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField(FileConstants.INVOICE_NR, DataType.STRING, EntityType.INVOICE)
                .addMetaField(FileConstants.META_FIELD_METER_READ_FILE, DataType.STRING, EntityType.INVOICE)
                .build();
        return this;
    }

    public NGESBuilder buildCustomerLevelMetaField(CompanyType type) {

        if(type==CompanyType.LDC){
            testEnvironmentCreator.configurationBuilder(ldcAPI)
                    .addMetaField(FileConstants.CUSTOMER_RATE_METAFIELD_NAME, DataType.DECIMAL, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_RATE_CHANGE_DATE_META_FIELD_NAME, DataType.DATE, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_RATE_ID_METAFILE_FIELD_NAME, DataType.STRING, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.INTERVAL_LOAD_CURVE_CUSTOMER_METAFIELD, DataType.STRING, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_METER_CYCLE_METAFIELD_NAME, DataType.INTEGER, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.RENEWED_DATE, DataType.DATE, EntityType.CUSTOMER)
                    .addMetaField("Annual Usage", DataType.DECIMAL, EntityType.CUSTOMER)
                    .addMetaField("UoM", DataType.STRING, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_ZONE_META_FIELD_NAME, DataType.STRING, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_TRANSMISSION_CONTRIBUTION_META_FIELD_NAME, DataType.DECIMAL, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_PEAK_LOAD_CONTRIBUTION_META_FIELD_NAME, DataType.DECIMAL, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_SUPPLIER_ID_META_FIELD_NAME, DataType.STRING, EntityType.CUSTOMER)
//                    .addMetaField("Termination", DataType.ENUMERATION, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.ADDER_FEE_META_FIELD, DataType.DECIMAL, EntityType.CUSTOMER)
                    .addMetaField(FileConstants.CUSTOMER_COMPLETION_DATE_METAFIELD, DataType.DATE, EntityType.CUSTOMER)
                    .build();
        }else {
            testEnvironmentCreator.configurationBuilder(escoAPI)
                    .addMetaField("Blacklisted", DataType.BOOLEAN, EntityType.CUSTOMER)
                    .addMetaField("LAST_ENROLLMENT", DataType.DATE, EntityType.CUSTOMER)
                    .addMetaField("Termination", DataType.ENUMERATION, EntityType.CUSTOMER)
                    .build();
        }


        return this;
    }

    public NGESBuilder buildEnrollmentLevelMetaField() {

        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField("Rate", DataType.DECIMAL, EntityType.ENROLLMENT)
                .build();
        return this;
    }

    public NGESBuilder buildCompanyLevelMetaFields() {
        testEnvironmentCreator.configurationBuilder(ldcAPI)
                .addMetaField(FileConstants.COMPANY_LEAD_TIME_1_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.COMPANY_LEAD_TIME_2_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.SUPPLIER_DUNS_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField("METER_TYPE", DataType.STRING, EntityType.COMPANY)        //todo Add constant in code
                .addMetaField("SERVICE_REQUESTED2", DataType.STRING, EntityType.COMPANY) //todo Add constant in code
                .addMetaField(FileConstants.COMPANY_CALENDAR_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.COMPANY_BUFFER_TIME_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.SUPPLIER_NAME_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.UTILITY_DUNS_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.RATE_CODE_TABLE_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.UTILITY_NAME_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.BILL_CALC_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.BILL_DELIVER_META_FIELD_NAME, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.ENROLLMENT_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.METER_READ_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.PAYMENT_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.INVOICE_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.TERMINATION_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.CHANGE_REQUEST_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.ESCO_TERMINATION_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField("Wiring Instructions Line 1", DataType.STRING, EntityType.COMPANY)   //todo Add constant in code
                .addMetaField("Wiring Instructions Line 2", DataType.STRING, EntityType.COMPANY)
                .addMetaField("Wiring Instructions Line 3", DataType.STRING, EntityType.COMPANY)
                .addMetaField("Wiring Instructions Line 4", DataType.STRING, EntityType.COMPANY)
                .addMetaField("Customer Emergency Phone Nr", DataType.STRING, EntityType.COMPANY)
                .addMetaField(Constants.COMPANY_METAFIELD_INVOICE_LINES_PRODUCT_TYPES, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.INTERVAL_LOAD_CURVE_COMPANY_METAFIELD, DataType.STRING, EntityType.COMPANY)
                .addMetaField(FileConstants.ACKNOWLEDGE_EDI_TYPE_ID_META_FIELD_NAME, DataType.INTEGER, EntityType.COMPANY)
                .addMetaField(FileConstants.DEFAULT_PLAN, DataType.STRING, EntityType.COMPANY)
                .build();
        return this;
    }

    protected ConfigurationBuilder addEnumerationsForParentCompany(ConfigurationBuilder configurationBuilder) {
        return configurationBuilder.addEnumeration("STATE", "New York", "Texas")
                .addEnumeration("DIVISION", "East", "West", "North", "South");
    }

    public NGESBuilder configureEnrollmentFileGenerationPlugin(String code) {
        PluggableTaskTypeWS pluginType = ldcAPI.getPluginTypeWSByClassName("com.sapienter.jbilling.server.customerEnrollment.task.CustomerEnrollmentFileGenerationTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(pluginType.getId())
                .withOrder(205)
                .withParameter("edi-status", "Ready to send")
                .build();

        return this;
    }

    public NGESBuilder configureSendEnrollmentPlugin(String code) {
        PluggableTaskTypeWS sendEnrollmentTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.customerEnrollment.task.SendToLdcTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(sendEnrollmentTask.getId())
                .withOrder(206)
                .withParameter("success_status", "Sent to LDC")
                .withParameter("error_status", "File transfer error")
                .withParameter("source_status", "Ready to send")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();

        return this;
    }

    public NGESBuilder configureResponseParserPlugin(String code) {
        PluggableTaskTypeWS enrollmentResponseParserTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.customerEnrollment.task.EnrollmentResponseParserTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(enrollmentResponseParserTask.getId())
                .withOrder(207)
                .withParameter("accept_status", "Accepted")
                .withParameter("reject_status", "Rejected")
                .withParameter("invalid_file_status", "Invalid File")
                .withParameter("internal_error", "Internal Error")
                .withParameter("success_status", "Sent to LDC")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();

        return this;
    }

    public NGESBuilder configureMeterReadParserPlugin(String code) {
        PluggableTaskTypeWS meterReadPArserTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.ediTransaction.task.MeterReadParserTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(meterReadPArserTask.getId())
                .withOrder(208)
                .withParameter("replacement_status", "EXP002")
                .withParameter("cancellation_status", "EXP001")
                .withParameter("historical_status", "Historical Meter Read")
                .withParameter("invalid_data_status", "Invalid Data")
                .withParameter("done_status", "Done")
                .withParameter("deprecated_status", "Deprecated")
                .withParameter("rejected_status", "Rejected")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();
        return this;
    }

    public NGESBuilder configureInvoiceReadParserPlugin(String code) {
        PluggableTaskTypeWS invoiceReadTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.ediTransaction.invoiceRead.InvoiceReadTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(invoiceReadTask.getId())
                .withOrder(209)
                .withParameter("cancellation_status", "EXP001")
                .withParameter("replacement_status", "EXP002")
                .withParameter("invalid_file_status", "Invalid File")
                .withParameter("file_mismatch_status", "MisMatch")
                .withParameter("accepted_status", "Accepted")
                .withParameter("deprecated_status", "Deprecated")
                .withParameter("rejected_status", "Rejected")
                .withParameter("meter_file_status", "Done")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();
        return this;
    }

    public NGESBuilder configurePaymentReadParserPlugin(String code) {
        PluggableTaskTypeWS invoiceReadTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.ediTransaction.task.PaymentParserTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(invoiceReadTask.getId())
                .withOrder(210)
                .withParameter("inconsistent_payment_status", "INCONSISTENT_PAYMENT")
                .withParameter("duplication_transaction", "DUPLICATE_PAYMENT")
                .withParameter("done", "DONE")
                .withParameter("invalid_data", "INVALID_DATA")
                .withParameter("rejected", "Rejected")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();
        return this;
    }

    public NGESBuilder configureUpdateOrderStatusPlugin(String code) {
        PluggableTaskTypeWS invoiceReadTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.ediTransaction.task.UpdateEDIStatusProcessTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(invoiceReadTask.getId())
                .withOrder(211)
                .withParameter("payment_read_status", "DONE")
                .withParameter("meter_read_status", "Done")
                .withParameter("invoice_read_status", "Accepted")
                .build();
        return this;
    }

    public NGESBuilder configureChangeRequestParserPlugin(String code) {
        PluggableTaskTypeWS changeRequestTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.ediTransaction.task.ChangeRequestParserTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(changeRequestTask.getId())
                .withOrder(212)
                .withParameter("sent_to_ldc_status", "Sent to LDC")
                .withParameter("done_status", "Done")
                .withParameter("rejected_status", "Rejected")
                .withParameter("invalid_data_status", "Invalid Data")
                .withParameter("ready_to_send_status", "Ready to send")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();
        return this;
    }
    public NGESBuilder configureCustomerTerminationParserPlugin(String code) {
        PluggableTaskTypeWS changeRequestTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.earlyTermination.task.CustomerTerminationTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(changeRequestTask.getId())
                .withOrder(213)
                .withParameter("reject_status", "Rejected")
                .withParameter("accept_status", "Accepted")
                .withParameter("invalid_file_status", "Invalid File")
                .withParameter("accept_status", "Accepted")
                .withParameter("ready_to_send_status", "Ready to send")
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();
        return this;
    }

    public NGESBuilder configureRateChangePlugin(String code){
        PluggableTaskTypeWS rateChangeTask = testEnvironmentCreator.getResellerApi().getPluginTypeWSByClassName("com.sapienter.jbilling.server.ediTransaction.task.RateChangeTask");
        testEnvironmentCreator.pluginBuilder(ldcAPI)
                .withCode(code)
                .withTypeId(rateChangeTask.getId())
                .withOrder(214)
                .withParameter("cron_exp", "0 0 12 * * ?")
                .build();
        return this;
    }
}
