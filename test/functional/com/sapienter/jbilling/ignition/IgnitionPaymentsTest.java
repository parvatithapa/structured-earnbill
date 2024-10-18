package com.sapienter.jbilling.ignition;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.ignition.IgnitionConstants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTemplateWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.*;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Created by wajeeha on 3/12/18.
 */
@Test(groups = {"test-ignition", "ignition" }, testName = "IgnitionPaymentsTest")
public class IgnitionPaymentsTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String IGNITION_SCHEDULING_TASK = "com.sapienter.jbilling.server.payment.tasks.IgnitionScheduledBatchJobTask";
    private static final String IGNITION_PAYMENT_TASK = "com.sapienter.jbilling.server.payment.tasks.PaymentIgnitionTask";
    private static final String ABSA_FOLDER = Util.getSysProp("base_dir") + "absa_payments/";
    private static final String STANDARD_BANK_FOLDER = Util.getSysProp("base_dir") + "Standard_Bank/";
    private static final String CLIENT_CODE = "1111";
    private static final String IGNITION_RESPONSE_MANAGER_TASK = "com.sapienter.jbilling.server.payment.tasks.IgnitionResponseManagerTask";
    private static final String IGNITION_TRANSMISSION_FAILURE_TASK = "com.sapienter.jbilling.server.payment.tasks.IgnitionTransmissionFailureTask";

    private Integer orderChangeStatusApplyId;

    private JbillingAPI api;
    private Integer accountTypeId;
    private Integer paymentMethodId;
    private String categoryName;
    private Integer entityId = 1;
    private Integer ignitionScheduledTaskId;
    private Integer ignitionPaymentTaskId;
    private Integer dataTableId;
    private Date nextPaymentDate;
    private Date currentDate;
    private Integer policyNoMetafieldId;
    private Integer actionDateMetafieldId;
    private Integer paymentTransactionNoMetafieldId;
    private Integer paymentActionDateMetafieldId;
    private Integer paymentSentOnMetafieldId;
    private Integer paymentSequenceNoMetafieldId;
    private Integer paymentTypeMetafieldId;
    private Integer paymentClinetCodeMetafieldId;
    private Integer paymentUserReferenceMetafieldId;
    private Integer paymentTransmissionDateMetafieldId;
    private Integer itemId;
    private Integer ignitionResponseManagerTaskId;
    private Integer ignitionTransmissionFailureTaskId;
    private Integer ORDER_PERIOD_MONTHLY;


    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();

        createIgnitionMetafields();
        orderChangeStatusApplyId = getOrCreateOrderChangeApplyStatus(api);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        accountTypeId = createAccountType();
        paymentMethodId = createPaymentMethod();
        Integer categoryId = createItemType(true);
        itemId = createItem(true, categoryId);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        currentDate = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 5);
        nextPaymentDate = calendar.getTime();

        logger.debug("Current date" + currentDate);
        logger.debug("nextPaymentDate" + nextPaymentDate);

        dataTableId = createServiceProfile("Service_Profiles", "Service_Profiles");

        FileUtils.deleteDirectory(new File(ABSA_FOLDER + entityId ));
        FileUtils.deleteDirectory(new File(STANDARD_BANK_FOLDER + entityId));
        BillingProcessConfigurationWS billingConfiguartionWS = api
                .getBillingProcessConfiguration();
        billingConfiguartionWS.setAutoPaymentApplication(1);
        api.createUpdateBillingProcessConfiguration(billingConfiguartionWS);
    }

    private void createIgnitionMetafields() {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Action Date");
        metafieldWS.setEntityType(EntityType.CUSTOMER);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.DATE);

        actionDateMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.POLICY_NUMBER);
        metafieldWS.setEntityType(EntityType.ORDER);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        policyNoMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_ACTION_DATE);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentActionDateMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_SENT_ON);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentSentOnMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentSequenceNoMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_TYPE);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentTypeMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_CLIENT_CODE);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentClinetCodeMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_USER_REFERENCE);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentUserReferenceMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_TRANSMISSION_DATE);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentTransmissionDateMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER);
        metafieldWS.setEntityType(EntityType.PAYMENT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        paymentTransactionNoMetafieldId = api.createMetaField(metafieldWS);
    }

    @AfterClass
    public void tearDown() {

        if (null != dataTableId)
            api.deleteRoute(dataTableId);

        if (null != paymentMethodId)
            api.deletePaymentMethodType(paymentMethodId);

        if (null != accountTypeId)
            api.deleteAccountType(accountTypeId);

        if (null != actionDateMetafieldId)
            api.deleteMetaField(actionDateMetafieldId);

        if (null != policyNoMetafieldId)
            api.deleteMetaField(policyNoMetafieldId);

        if (null != paymentActionDateMetafieldId)
            api.deleteMetaField(paymentActionDateMetafieldId);

        if (null != paymentClinetCodeMetafieldId)
            api.deleteMetaField(paymentClinetCodeMetafieldId);

        if (null != paymentSentOnMetafieldId)
            api.deleteMetaField(paymentSentOnMetafieldId);

        if (null != paymentSequenceNoMetafieldId)
            api.deleteMetaField(paymentSequenceNoMetafieldId);

        if (null != paymentTransactionNoMetafieldId)
            api.deleteMetaField(paymentTransactionNoMetafieldId);

        if (null != paymentTransmissionDateMetafieldId)
            api.deleteMetaField(paymentTransmissionDateMetafieldId);

        if (null != paymentUserReferenceMetafieldId)
            api.deleteMetaField(paymentUserReferenceMetafieldId);

        if (null != paymentTypeMetafieldId)
            api.deleteMetaField(paymentTypeMetafieldId);

    }

    private void createOrderAndInvoice(Integer userId, Integer itemId) {
        // Create

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        order.setActiveSince(cal.getTime());

        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.setFieldName(IgnitionConstants.POLICY_NUMBER);
        metaField.setStringValue("12345");

        order.setMetaFields(new MetaFieldValueWS[]{metaField});

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[1];

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setDescription(String.format("Line for product %d", itemId));
        line.setItemId(itemId);

        line.setUseItem(Boolean.TRUE);
        lines[0] = line;

        order.setOrderLines(lines);

        logger.debug("Creating order ... {}", order);
        api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, orderChangeStatusApplyId));
    }

    private static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api) {
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for (OrderChangeStatusWS orderChangeStatus : list) {
            if (orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)) {
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if (statusId != null) {
            return statusId;
        } else {
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(ApplyToOrder.YES);
            newStatus.setDeleted(0);
            newStatus.setOrder(1);
            newStatus.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private Integer createAccountType() {

        BigDecimal ZERO = new BigDecimal("0");
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setEntityId(entityId);
        accountType.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        accountType.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        accountType.setCreditLimit(ZERO);
        accountType.setInvoiceDeliveryMethodId(1);
        accountType.setMainSubscription(new MainSubscriptionWS(
                Constants.PERIOD_UNIT_MONTH, 1));

        List<InternationalDescriptionWS> descriptions = new ArrayList<InternationalDescriptionWS>();
        InternationalDescriptionWS description = new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                "Ignition Customer:" + System.currentTimeMillis());
        descriptions.add(description);
        accountType.setDescriptions(descriptions);
        Integer accountTypeId = api.createAccountType(accountType);

        logger.debug("Account type created with id " + accountTypeId);

        return accountTypeId;
    }

    private Integer createPaymentMethod() {
        PaymentMethodTypeWS paymentMethod = new PaymentMethodTypeWS();
        paymentMethod.setAllAccountType(Boolean.TRUE);
        PaymentMethodTemplateWS paymentMethodTemplateWS = api.getPaymentMethodTemplate(-1);

        MetaFieldWS[] metaFields = new MetaFieldWS[7];

        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Account Type");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        api.createMetaField(metafieldWS);
        metaFields[0] = metafieldWS;

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Bank Name");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        api.createMetaField(metafieldWS);
        metaFields[1] = metafieldWS;

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Branch Code");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        api.createMetaField(metafieldWS);
        metaFields[2] = metafieldWS;

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Account Name");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        api.createMetaField(metafieldWS);
        metaFields[3] = metafieldWS;

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Account Number");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        api.createMetaField(metafieldWS);
        metaFields[4] = metafieldWS;

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Next Payment Date");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.DATE);

        api.createMetaField(metafieldWS);
        metaFields[5] = metafieldWS;

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Debit Day");
        metafieldWS.setEntityType(EntityType.PAYMENT_METHOD_TYPE);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.INTEGER);

        api.createMetaField(metafieldWS);
        metaFields[6] = metafieldWS;

        paymentMethod.setMetaFields(metaFields);
        paymentMethod.setTemplateId(paymentMethodTemplateWS.getId());
        paymentMethod.setMethodName("Direct Debit" + ":" + (System.currentTimeMillis() % 10000));
        paymentMethod.setIsRecurring(true);
        Integer paymentMethodId = api.createPaymentMethodType(paymentMethod);

        logger.debug("Payment method created with id " + paymentMethodId);

        return paymentMethodId;

    }

    private Integer createServiceProfile(String fileName, String tableName) throws JbillingAPIException, IOException {
        File temporalFile = File.createTempFile(fileName, ".csv");
        RouteWS routeWS = new RouteWS();
        routeWS.setName(tableName);
        routeWS.setRootTable(false);
        routeWS.setRouteTable(false);
        routeWS.setOutputFieldName("");
        routeWS.setDefaultRoute("");

        String data = "serviceprovider,brandname,bankaccountnumber,bankaccountname,bankaccounttype," +
                "bankaccountbranch,serviceprofile,shortname,acbusercode,username,code,typesofdebitservices," +
                "toflfolderlocation,fromflfolderlocation,filesequencenumber,generationnumber," +
                "transactionnumber,cutofftime,entityname,islive\n" +
                "ABSA," + categoryName + ",4078103569,Test,Current,632005,Test " +
                "ABSA,Test ," + CLIENT_CODE + ",Test ,Test," +
                "TWO DAY,ABSA//EFT//Test//FromFI//,ABSA//EFT//Test//ToFI//,1,1,1,23:59," +
                "Test ,TRUE\n" +
                "Standard Bank," + categoryName + ",240264347,Test,Current,51001,Test Standard Bank," +
                "Test," + CLIENT_CODE + ",Test,Test,TWO DAY,StandardBank//EFT//Test//FromFI//,StandardBank//EFT//" +
                "Test//ToFI//,0,0,0,23:59,Test,FALSE";

        writeToFile(temporalFile, data);
        Integer id;
        id = api.createRoute(routeWS, temporalFile);
        temporalFile.delete();

        logger.debug("Data table created with id " + id);

        return id;
    }

    private void writeToFile(File file, String content) throws IOException {
        FileWriter fw = new FileWriter(file);
        fw.write(content);
        fw.close();
    }

    private Integer createItemType(boolean global) {
        ItemTypeWS itemType = new ItemTypeWS();
        categoryName = "ignition-test-category:" + System.currentTimeMillis();

        itemType.setDescription(categoryName);
        itemType.setEntityId(entityId);
        if (global) {
            itemType.setGlobal(global);
        } else {
            itemType.setEntities(new ArrayList<Integer>(entityId));
        }
        itemType.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);

        Integer categoryId = api.createItemCategory(itemType);

        logger.debug("Item category created with id " + categoryId + " name " + categoryName);

        return categoryId;
    }

    private Integer createItem(boolean global, Integer type) {
        ItemDTOEx item = new ItemDTOEx();
        item.setDescription("TestItem: " + System.currentTimeMillis());
        item.setNumber("TestWS-" + System.currentTimeMillis());
        item.setTypes(new Integer[]{type});
        item.setPrice(new BigDecimal(55));

        item.setExcludedTypes(new Integer[]{});
        if (global) {
            item.setGlobal(global);
        } else {
            item.setGlobal(false);
        }
        item.setEntityId(entityId);
        Integer itemId = api.createItem(item);

        logger.debug("Item created with id " + itemId);

        return itemId;
    }

    private Integer createUser(Date actionDateValue, Date nextPaymentDateValue, String userBankName) throws JbillingAPIException,
            IOException {

        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("IgnitionUser"+userBankName+"-"
                + Calendar.getInstance().getTimeInMillis());

        newUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        newUser.setAccountTypeId(accountTypeId);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        newUser.setInvoiceChild(false);
        newUser.setMainRoleId(com.sapienter.jbilling.client.util.Constants.ROLE_CUSTOMER);
        newUser.setMainSubscription(new MainSubscriptionWS(ORDER_PERIOD_MONTHLY, 1));
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH,1);
        newUser.setNextInvoiceDate(calendar.getTime());

        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.setFieldName("Action Date");
        metaField.setDateValue(actionDateValue);

        newUser.setMetaFields(new MetaFieldValueWS[]{metaField});

        PaymentInformationWS directDebit = new PaymentInformationWS();

        MetaFieldValueWS accountName = new MetaFieldValueWS();
        accountName.setFieldName(IgnitionConstants.PAYMENT_ACCOUNT_NAME);
        accountName.setStringValue("Test");

        MetaFieldValueWS accountNo = new MetaFieldValueWS();
        accountNo.setFieldName(IgnitionConstants.PAYMENT_ACCOUNT_NUMBER);
        accountNo.setStringValue("0123456789");

        MetaFieldValueWS bankName = new MetaFieldValueWS();
        bankName.setFieldName(IgnitionConstants.PAYMENT_BANK_NAME);
        bankName.setStringValue(userBankName);

        MetaFieldValueWS accountType = new MetaFieldValueWS();
        accountType.setFieldName(IgnitionConstants.PAYMENT_ACCOUNT_TYPE);
        accountType.setStringValue("SAVINGS");

        MetaFieldValueWS nextPaymentDate = new MetaFieldValueWS();
        nextPaymentDate.setFieldName(IgnitionConstants.PAYMENT_NEXT_PAYMENT_DATE);
        nextPaymentDate.setDateValue(nextPaymentDateValue);

        MetaFieldValueWS branchCode = new MetaFieldValueWS();
        branchCode.setFieldName(IgnitionConstants.PAYMENT_BRANCH_CODE);
        branchCode.setStringValue("012345");

        MetaFieldValueWS debitDay = new MetaFieldValueWS();
        debitDay.setFieldName(IgnitionConstants.PAYMENT_DEBIT_DAY);
        debitDay.setIntegerValue(28);

        directDebit.setPaymentMethodTypeId(paymentMethodId);
        directDebit.setProcessingOrder(new Integer(1));
        directDebit.setMetaFields(new MetaFieldValueWS[]{accountName, accountNo,
                bankName, accountType, branchCode, nextPaymentDate, debitDay});
        directDebit.setPaymentMethodId(Constants.PAYMENT_METHOD_CUSTOM);

        newUser.getPaymentInstruments().add(directDebit);

        logger.debug("Creating user ...");
        Integer userId = api.createUser(newUser);

        logger.debug("User created with id:{}", userId);
        return userId;
    }

    private void updatePluginSetCronExpressionAndParameter() {
        PluggableTaskWS ignitionPaymentTask = new PluggableTaskWS();
        ignitionPaymentTask.setProcessingOrder(524);
        PluggableTaskTypeWS ignitionPaymentTaskType = api.getPluginTypeWSByClassName(
                IGNITION_PAYMENT_TASK);
        ignitionPaymentTask.setTypeId(ignitionPaymentTaskType.getId());

        ignitionPaymentTask.setParameters(new Hashtable<>(ignitionPaymentTask.getParameters()));
        Hashtable<String, String> parameters1 = new Hashtable<>();
        ignitionPaymentTask.setParameters(parameters1);

        ignitionPaymentTaskId = api.createPlugin(ignitionPaymentTask);

        PluggableTaskWS ignitionSchedulingTask = new PluggableTaskWS();
        ignitionSchedulingTask.setProcessingOrder(523);
        PluggableTaskTypeWS ignitionSchedulingTaskType = api.getPluginTypeWSByClassName(
                IGNITION_SCHEDULING_TASK);
        ignitionSchedulingTask.setTypeId(ignitionSchedulingTaskType.getId());

        ignitionSchedulingTask.setParameters(new Hashtable<>(ignitionSchedulingTask.getParameters()));
        Hashtable<String, String> parameters = new Hashtable<>();
        // Set cron expression to trigger every minute
        CompanyWS companyWS =  api.getCompany();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(companyWS.getTimezone()));
        calendar.add(Calendar.SECOND,90);
        int min = calendar.get(Calendar.MINUTE);
        int hour =  calendar.get(Calendar.HOUR_OF_DAY);
        String cronExpression = "0 "+min+" "+hour+" 1/1 * ? *";
        parameters.put("cron_exp", cronExpression);
        parameters.put("Process Latest Invoice", "false");
        ignitionSchedulingTask.setParameters(parameters);

        logger.debug("Starting scheduling task with cron expression "+cronExpression
                + " current date: "+ calendar.getTime() + ", company time zone = "
                +companyWS.getTimezone());

        ignitionScheduledTaskId = api.createPlugin(ignitionSchedulingTask);
    }


    @Test(priority = 1, enabled = true)
    public void testCreateIgnitionPaymentsForABSA() throws Exception {

        Integer absaUserId = createUser(currentDate, nextPaymentDate, "ABSA");
        createOrderAndInvoice(absaUserId, itemId);

        updatePluginSetCronExpressionAndParameter();

        logger.debug("Processing ignition payments for user "+absaUserId);

        sleep(120000);

        Integer[] unpaidInvoices = api.getUnpaidInvoices(absaUserId);
        assertEquals("Unpaid Invoices for user id " + absaUserId + " should be 0", 0, unpaidInvoices.length);

        logger.debug("Validating payment meta-fields for ABSA user");

        PaymentWS paymentWS = api.getLatestPayment(absaUserId);

        MetaFieldValueWS[] paymentMetaFieldValues = paymentWS.getMetaFields();

        for (MetaFieldValueWS metaFieldValueWS : paymentMetaFieldValues) {
            String metaFieldName = metaFieldValueWS.getFieldName();

            DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

            if (metaFieldName.equals(IgnitionConstants.PAYMENT_ACTION_DATE)) {
                String actionDate = df.format(DateConvertUtils.asLocalDateTime(nextPaymentDate));
                assertEquals("Incorrect action date", actionDate, metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_SENT_ON)) {
                assertEquals("Incorrect bank", "Test ABSA", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER)) {
                assertEquals("Incorrect sequence no", "000001", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_TYPE)) {
                assertEquals("Incorrect payment Type", "EFT", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_CLIENT_CODE)) {
                assertEquals("Incorrect client code", "Test", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_USER_REFERENCE)) {
                assertEquals("Incorrect user reference no", "Test      12345", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_TRANSMISSION_DATE)) {
                String transmissionDate = df.format(DateConvertUtils.asLocalDateTime(currentDate));
                assertEquals("Incorrect transmission date", transmissionDate, metaFieldValueWS.getStringValue());
            }

        }

        logger.debug("Validated payment meta-fields for user " + absaUserId);

        Integer invoiceId = paymentWS.getInvoiceIds()[0];

        api.removePaymentLink(invoiceId, paymentWS.getId());
        InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
        api.deleteInvoice(invoiceId);
        api.deleteOrder(invoiceWS.getOrders()[0]);
        api.deletePayment(paymentWS.getId());
        api.deleteUser(absaUserId);
        api.deletePlugin(ignitionScheduledTaskId);
        api.deletePlugin(ignitionPaymentTaskId);

    }

    @Test(priority = 2, enabled = true)
    public void testCreateIgnitionPaymentsForStandardBank() throws Exception {

        Integer standardBankUserId = createUser(currentDate, nextPaymentDate, "STANDARD BANK");
        createOrderAndInvoice(standardBankUserId, itemId);

        updatePluginSetCronExpressionAndParameter();

        logger.debug("Processing ignition payments for user "+standardBankUserId);

        sleep(120000);

        PaymentWS paymentWS = api.getLatestPayment(standardBankUserId);

        MetaFieldValueWS[] paymentMetaFieldValues = paymentWS.getMetaFields();

        for (MetaFieldValueWS metaFieldValueWS : paymentMetaFieldValues) {
            String metaFieldName = metaFieldValueWS.getFieldName();

            DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);

            if (metaFieldName.equals(IgnitionConstants.PAYMENT_ACTION_DATE)) {
                String actionDate = df.format(DateConvertUtils.asLocalDateTime(nextPaymentDate));
                assertEquals("Incorrect action date", actionDate, metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_TRANSACTION_NUMBER)) {
                assertEquals("Incorrect transaction no", "1", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_SENT_ON)) {
                assertEquals("Incorrect bank", "Test Standard Bank", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_SEQUENCE_NUMBER)) {
                assertEquals("Incorrect sequence no", "1", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_TYPE)) {
                assertEquals("Incorrect payment Type", "EFT", metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_CLIENT_CODE)) {
                assertEquals("Incorrect client code", CLIENT_CODE, metaFieldValueWS.getStringValue());
            } else if (metaFieldName.equals(IgnitionConstants.PAYMENT_USER_REFERENCE)) {
                assertEquals("Incorrect user reference no", "Test      12345               ",
                        metaFieldValueWS.getStringValue());
            }

        }

        logger.debug("Validated payment meta-fields for user " + standardBankUserId);

        Integer invoiceId = paymentWS.getInvoiceIds()[0];

        api.removePaymentLink(invoiceId, paymentWS.getId());
        InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
        api.deleteInvoice(invoiceId);
        api.deleteOrder(invoiceWS.getOrders()[0]);
        api.deletePayment(paymentWS.getId());
        api.deleteUser(standardBankUserId);
        api.deletePlugin(ignitionScheduledTaskId);
        api.deletePlugin(ignitionPaymentTaskId);

    }

    @Test (priority = 3, enabled = true)
    void testUpdateIgnitionNextPaymentDate() throws InterruptedException, IOException, JbillingAPIException {
        Integer userId = createUser(null, currentDate,"ABSA");
        createOrder(userId, itemId);
        updatePluginSetCronExpressionAndParameter();

        logger.debug("Processing ignition payments");

        sleep(120000);

        UserWS userWS = api.getUserWS(userId);

        MetaFieldValueWS[]  metaFieldValues = userWS.getPaymentInstruments().get(0).getMetaFields();
        boolean metafieldFound = false;
        Date nextPaymentDate = null;

        for(MetaFieldValueWS metafieldValue :metaFieldValues){
            if (metafieldValue.getFieldName().equals(IgnitionConstants.METAFIELD_NEXT_PAYMENT_DATE_INDENTIFIER)){
                nextPaymentDate = metafieldValue.getDateValue();
                metafieldFound =true;
                break;
            }
        }

        CompanyWS companyWS =  api.getCompany();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(companyWS.getTimezone()));
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        calendar.set(Calendar.DAY_OF_MONTH,28);

        calendar.add(Calendar.MONTH,1);

        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            calendar.add(Calendar.DAY_OF_MONTH,-1);

        if(calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY)
            calendar.add(Calendar.DAY_OF_MONTH,-1);

        assertEquals("Next Payment Date meta-field not found",metafieldFound,true);

        assertEquals("Incorrect Next Payment Date",0,DateTimeComparator.getDateOnlyInstance().compare(calendar.getTime(), nextPaymentDate));

        if(null != userId)
            api.deleteUser(userId);
        api.deletePlugin(ignitionScheduledTaskId);
        api.deletePlugin(ignitionPaymentTaskId);

    }

    @Test(priority = 4, enabled = true, dependsOnMethods = "testCreateIgnitionPaymentsForABSA")
    public void testAbsaPaymentFile() throws IOException {
        File absa_folder = new File(ABSA_FOLDER + entityId + "/Test");
        String[] files = absa_folder.list();

        assertNotNull("absa_payments folder is empty", files);
        assertEquals("No input file found for Absa", 1, files.length);

        List<String> lines = Files.readAllLines(Paths.get(absa_folder + "/" + files[0]));

        String header = lines.get(1);
        assertEquals("Incorrect header length",198,header.length());

        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);
        String actionDate = df.format(DateConvertUtils.asLocalDateTime(nextPaymentDate));
        String transmissionDate = df.format(DateConvertUtils.asLocalDateTime(currentDate));

        assertEquals("Incorrect action date",actionDate,header.substring(22,28));
        assertEquals("Incorrect transmission date",transmissionDate,header.substring(10,16));

        String transactionRecord = lines.get(2);

        assertEquals("Transaction record length is incorrect",198,transactionRecord.length());
        assertEquals("Incorrect client code", CLIENT_CODE, transactionRecord.substring(23, 27));
        assertEquals("Incorrect sequence no", "000001", transactionRecord.substring(27, 33));

    }

    @Test(priority = 5, enabled = true, dependsOnMethods = "testCreateIgnitionPaymentsForStandardBank")
    public void testStandardBankPaymentFile() throws IOException {

        String path = STANDARD_BANK_FOLDER + entityId + "/TWO_DAY/TRHTHSB.COMIT.Test.BEFT" + CLIENT_CODE + ".INPUT";

        Boolean pathExists = new File(path).exists();

        assertEquals("No input file found for Standard Bank", Boolean.TRUE, pathExists);

        List<String> lines = Files.readAllLines(Paths.get(path));

        String header = lines.get(1);
        assertEquals("Incorrect header length",180,header.length());

        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_DATE_FORMAT);
        String actionDate = df.format(DateConvertUtils.asLocalDateTime(nextPaymentDate));
        String transmissionDate = df.format(DateConvertUtils.asLocalDateTime(currentDate));

        assertEquals("Incorrect action date",actionDate,header.substring(18,24));
        assertEquals("Incorrect transmission date",transmissionDate,header.substring(6,12));

        String transactionRecord = lines.get(2);

        assertEquals("Transaction record length is incorrect",180,transactionRecord.length());
        assertEquals("Incorrect client code", CLIENT_CODE, transactionRecord.substring(19, 23));
        assertEquals("Incorrect sequence no", "000001", transactionRecord.substring(23, 29));

    }

    @Test(priority = 6, enabled = true, dependsOnMethods = "testCreateIgnitionPaymentsForStandardBank")
    public void testFailedPaymentForStandardBank() throws IOException, InterruptedException, JbillingAPIException {

        Integer standardBankUserId = createUser(currentDate, nextPaymentDate, "STANDARD BANK");
        createOrderAndInvoice(standardBankUserId, itemId);

        updatePluginSetCronExpressionAndParameter();

        logger.debug("Processing ignition payments for user "+standardBankUserId);

        sleep(120000);

        api.deletePlugin(ignitionScheduledTaskId);
        api.deletePlugin(ignitionPaymentTaskId);

        String vetFileName = "TEST-VETDATA-FILE.txt";
        String filePath = STANDARD_BANK_FOLDER + entityId + File.separator+"Response_Files"+File.separator+vetFileName;

        String fileContent = "FHAVETQVE24201706152017061720170619070306004992                                        " +
                "                                                                                             \n" +
                "0471570000000000001706151706170000000000SAMEDAY                                                     " +
                "                                                                                \n" +
                "610457260006255267871570000020503210028077285810000000700017062000001YPHOB      YM-58070276-3       " +
                "VUYISWA CANTA                  00000000000000000000      00170615 00            \n" +
                "20045726000625526787157000001457260006255267800000001420017061511    JHB      000000000004          " +
                "                                                                 21            \n" +
                "927157000000000000170615170617000004000000000000000000017900000000000000000000000000";

        Path pathToFile = Paths.get(filePath);
        if (Files.notExists(pathToFile)) {
            Files.createDirectories(pathToFile.getParent());
            Files.createFile(pathToFile);
        }

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            File file = new File(filePath);
            fileWriter = new FileWriter(file.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(fileContent);

        } catch (IOException exception) {
            logger.error("Exception = " + exception);
        } finally {
            try {
                if (bufferedWriter != null)
                    bufferedWriter.close();

                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException exception) {
                logger.error("Exception = " + exception);
            }
        }

        logger.info("file wrote "+filePath);

        setResponseManagerPlugin();

        logger.debug("Processing ignition response files for Standard bank");

        sleep(120000);
        api.deletePlugin(ignitionResponseManagerTaskId);
        api.deletePlugin(ignitionTransmissionFailureTaskId);

        PaymentWS paymentWS = api.getLatestPayment(standardBankUserId);
        InvoiceWS invoice = api.getInvoiceWS(paymentWS.getInvoiceIds()[0]);

        assertEquals("Invoice status should be unpaid", new Integer(2), invoice.getStatusId());
        assertEquals("Incorrect invoice balance",new BigDecimal("55.0000000000"),invoice.getBalanceAsDecimal());

        api.removePaymentLink(invoice.getId(), paymentWS.getId());
        InvoiceWS invoiceWS = api.getInvoiceWS(invoice.getId());
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(invoiceWS.getOrders()[0]);
        api.deletePayment(paymentWS.getId());
        api.deleteUser(standardBankUserId);
        FileUtils.deleteDirectory(new File(STANDARD_BANK_FOLDER + entityId + File.separator + "Response_Files"));

    }

    @Test(priority = 7, enabled = true, dependsOnMethods = "testCreateIgnitionPaymentsForABSA")
    public void testFailedPaymentForABSA() throws IOException, InterruptedException, JbillingAPIException {

        Integer absaUserId = createUser(currentDate, nextPaymentDate, "ABSA");
        createOrderAndInvoice(absaUserId, itemId);

        updatePluginSetCronExpressionAndParameter();

        logger.debug("Processing ignition payments for user "+absaUserId);

        sleep(120000);

        api.deletePlugin(ignitionScheduledTaskId);
        api.deletePlugin(ignitionPaymentTaskId);

        String outputFileName = "OUTPUT.TEST.PAYMENT.txt";
        String filePath = ABSA_FOLDER + entityId + File.separator + IgnitionConstants.ABSA_RESPONSE_FOLDER +
                File.separator + outputFileName;
        DateTimeFormatter df = DateTimeFormatter.ofPattern(IgnitionConstants.ABSA_TRANSMISSION_DATE_FORMAT);
        String transmissionDate = df.format(DateConvertUtils.asLocalDateTime(currentDate));
        String userReference = StringUtils.rightPad("Test      12345",30," ");

        String fileContent = "000L2017061600000COMIT TECHNOLOGIES PTY LTD 2  000290303883                            " +
                "                                                                                                    " +
                "           \n" +
                "010LA617000204201                                                                                   " +
                "                                                                                                  \n" +
                "011LA61763200500000040781325120120170515                                                            " +
                "                                                                                                  \n" +
                "013L50"+transmissionDate+"000002632005000000920444991000000002900"+userReference+"00592801001240   " +
                "             MOKOMA LERATO                                                                        \n" +
                "014L0000001130000000000000011078342952290000000054230000000000000000                                " +
                "                                                                                                  \n" +
                "019L0000001130000000000000011078342952290000000054230000000000000000                                " +
                "                                                                                                  \n" +
                "999L000000134                                                                                       " +
                "                                                                                                  \n";

        Path pathToFile = Paths.get(filePath);
        if (Files.notExists(pathToFile)) {
            Files.createDirectories(pathToFile.getParent());
            Files.createFile(pathToFile);
        }

        FileWriter fileWriter = null;
        BufferedWriter bufferedWriter = null;

        try {
            File file = new File(filePath);
            fileWriter = new FileWriter(file.getAbsoluteFile());
            bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write(fileContent);

        } catch (IOException exception) {
            logger.error("Exception = " + exception);
        } finally {
            try {
                if (bufferedWriter != null)
                    bufferedWriter.close();

                if (fileWriter != null)
                    fileWriter.close();
            } catch (IOException exception) {
                logger.error("Exception = " + exception);
            }
        }

        logger.info("file wrote "+filePath);

        setResponseManagerPlugin();

        logger.debug("Processing ignition response files for ABSA");

        sleep(120000);

        api.deletePlugin(ignitionResponseManagerTaskId);
        api.deletePlugin(ignitionTransmissionFailureTaskId);

        PaymentWS paymentWS = api.getLatestPayment(absaUserId);
        InvoiceWS invoice = api.getInvoiceWS(paymentWS.getInvoiceIds()[0]);

        assertEquals("Invoice status should be unpaid", new Integer(2), invoice.getStatusId());
        assertEquals("Incorrect invoice balance",new BigDecimal("55.0000000000"),invoice.getBalanceAsDecimal());

        api.removePaymentLink(invoice.getId(), paymentWS.getId());
        InvoiceWS invoiceWS = api.getInvoiceWS(invoice.getId());
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(invoiceWS.getOrders()[0]);
        api.deletePayment(paymentWS.getId());
        api.deleteUser(absaUserId);

        FileUtils.deleteDirectory(new File(ABSA_FOLDER + entityId + File.separator +
                IgnitionConstants.ABSA_RESPONSE_FOLDER ));

    }

    private void setResponseManagerPlugin() {
        PluggableTaskWS ignitionPaymentTask = new PluggableTaskWS();
        ignitionPaymentTask.setProcessingOrder(123);
        PluggableTaskTypeWS ignitionPaymentTaskType = api.getPluginTypeWSByClassName(
                IGNITION_TRANSMISSION_FAILURE_TASK);
        ignitionPaymentTask.setTypeId(ignitionPaymentTaskType.getId());

        ignitionPaymentTask.setParameters(new Hashtable<>(ignitionPaymentTask.getParameters()));
        Hashtable<String, String> parameters1 = new Hashtable<>();
        ignitionPaymentTask.setParameters(parameters1);

        ignitionTransmissionFailureTaskId = api.createPlugin(ignitionPaymentTask);

        PluggableTaskWS ignitionSchedulingTask = new PluggableTaskWS();
        ignitionSchedulingTask.setProcessingOrder(124);
        PluggableTaskTypeWS ignitionSchedulingTaskType = api.getPluginTypeWSByClassName(
                IGNITION_RESPONSE_MANAGER_TASK);
        ignitionSchedulingTask.setTypeId(ignitionSchedulingTaskType.getId());

        ignitionSchedulingTask.setParameters(new Hashtable<>(ignitionSchedulingTask.getParameters()));
        Hashtable<String, String> parameters = new Hashtable<>();
        // Set cron expression to trigger every minute
        CompanyWS companyWS = api.getCompany();

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(companyWS.getTimezone()));
        calendar.add(Calendar.SECOND, 90);
        int min = calendar.get(Calendar.MINUTE);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        String cronExpression = "0 " + min + " " + hour + " 1/1 * ? *";
        parameters.put("cron_exp", cronExpression);
        parameters.put("username", "test");
        parameters.put("password", "test");
        parameters.put("host", "test");
        parameters.put("port", "22");
        ignitionSchedulingTask.setParameters(parameters);

        logger.debug("Starting scheduling task with cron expression " + cronExpression
                + " current date: " + calendar.getTime() + ", company time zone = "
                + companyWS.getTimezone());

        ignitionResponseManagerTaskId = api.createPlugin(ignitionSchedulingTask);
    }

    private static Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(1 == period.getValue().intValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId().intValue()){
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(1);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private Integer createOrder(Integer userId, Integer itemId) {

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY);
        order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
        Calendar cal = Calendar.getInstance();
        order.setActiveSince(cal.getTime());

        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.setFieldName(IgnitionConstants.POLICY_NUMBER);
        metaField.setStringValue("12345");

        order.setMetaFields(new MetaFieldValueWS[] { metaField });

        // Add Lines
        OrderLineWS lines[] = new OrderLineWS[1];

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(1);
        line.setDescription(String.format("Line for product %d", itemId));
        line.setItemId(itemId);

        line.setUseItem(Boolean.TRUE);
        lines[0] = line;

        order.setOrderLines(lines);

        logger.debug("Creating order ... {}", order);
        Integer orderId = api.createOrder(order,
                OrderChangeBL.buildFromOrder(order, orderChangeStatusApplyId));
        return orderId;
    }
}
