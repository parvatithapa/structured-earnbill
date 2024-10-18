/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.fc.mediation;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.beans.PropertyVetoException;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.ProcessStatusWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;


@Test(groups = { "fullcreative" }, testName = "FullCreativeMediationInvoiceGenerationTest")
@ContextConfiguration(classes = FullCreativeMediationInvoiceGenerationTest.class)
@Configuration
public class FullCreativeMediationInvoiceGenerationTest extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(FullCreativeMediationInvoiceGenerationTest.class);
    private static String INBOUND_MEDIATION_LAUNCHER;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static Integer MEDIATION_COUNT = 0;
    private UUID processId;
    private int product8XXTollFreeId;
    private JbillingAPI api;
    GregorianCalendar cal;
    private static final Integer BILLING_PROCESS_YEAR = 2016;
    private int totalInvoices = 0;
    private Integer basicItemManagerPlugInId;
    private Integer invoiceCompositionPlugInId;
    private Integer ratingSchemeId;
    private Integer partitionedPluginId;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Bean(name = "fcDataSource")
    public DataSource dataSource(Environment environment) throws PropertyVetoException {
        String dbUser = environment.getProperty("JBILLING_DB_USER") !=null ? environment.getProperty("JBILLING_DB_USER") : "jbilling";
        String dbName = environment.getProperty("JBILLING_DB_NAME")!=null ? environment.getProperty("JBILLING_DB_NAME") : "jbilling_test";
        String dbHost = environment.getProperty("JBILLING_DB_HOST")!=null ? environment.getProperty("JBILLING_DB_HOST") : "localhost";
        String dbPort = environment.getProperty("JBILLING_DB_PORT")!=null ? environment.getProperty("JBILLING_DB_PORT") : "5432";
        String dbParams   = environment.getProperty("JBILLING_DB_PARAMS")!=null ? environment.getProperty("JBILLING_DB_PARAMS") : "";
        String dbPassword = environment.getProperty("JBILLING_DB_PASSWORD")!=null ? environment.getProperty("JBILLING_DB_PASSWORD"): "";
        String url = String.format("jdbc:postgresql://%s:%s/%s%s", dbHost, dbPort, dbName, dbParams);
        ComboPooledDataSource dataSource = new ComboPooledDataSource();
        dataSource.setDriverClass("org.postgresql.Driver");
        dataSource.setJdbcUrl(url);
        dataSource.setUser(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setInitialPoolSize(2);
        dataSource.setMaxPoolSize(4);
        dataSource.setMinPoolSize(1);
        dataSource.setAcquireIncrement(3);
        dataSource.setMaxIdleTime(300);
        dataSource.setCheckoutTimeout(10000);
        dataSource.setTestConnectionOnCheckout(false);
        dataSource.setIdleConnectionTestPeriod(20);
        return dataSource;
    }

    @Bean(name = "fcJdbcTemplate")
    public JdbcTemplate jdbcTemplate(@Qualifier("fcDataSource") DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.afterPropertiesSet();
        return template;
    }

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI("apiClient");
        product8XXTollFreeId = FullCreativeTestConstants.TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID;
        INBOUND_MEDIATION_LAUNCHER = FullCreativeTestConstants.INBOUND_MEDIATION_LAUNCHER;
        basicItemManagerPlugInId = FullCreativeTestConstants.BASIC_ITEM_MANAGER_PLUGIN_ID;
        invoiceCompositionPlugInId= FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID;
        PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(FullCreativeTestConstants.ORDER_LINE_BASED_COMPOSITION_TASK_NAME);
        try {
            PluggableTaskWS plugin = api.getPluginWS(invoiceCompositionPlugInId);
            logger.debug("plugin {}" , plugin);
            plugin.setTypeId(type.getId());
            api.updatePlugin(plugin);
        } catch(Exception ex) {
            PluggableTaskWS plugIn = new PluggableTaskWS();
            plugIn.setNotes("Test -Plugin");
            plugIn.setOwningEntityId(api.getCallerCompanyId());
            plugIn.setProcessingOrder(123);
            plugIn.setTypeId(type.getId());
            invoiceCompositionPlugInId = api.createPlugin(plugIn);
        }
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);
        logger.debug("Creating Global Rating Scheme...");
        if(api.getRatingSchemesForEntity().length==0) {
            MediationRatingSchemeWS ratingScheme = FullCreativeUtil.getRatingScheme("Prancing Pony Rating Scheme");
            logger.debug("Creating Rating Scheme with initial increment 30 and main increment 6.");
            ratingSchemeId =  api.createRatingScheme(ratingScheme);
            assertNotNull("Rating Scheme should not be null",ratingSchemeId);
            logger.debug("Rating Scheme created with Id :::: {}", ratingSchemeId);
        } else {
            ratingSchemeId = -1;
        }

        PluggableTaskTypeWS partitionedTask = api.getPluginTypeWSByClassName(FullCreativeTestConstants.ORDER_LINE_COUNT_BASED_USER_PARTITIONING_TASK);
        PluggableTaskWS partitionedPlugIn = new PluggableTaskWS();
        partitionedPlugIn.setNotes("Test Partitioned Plugin");
        partitionedPlugIn.setOwningEntityId(api.getCallerCompanyId());
        partitionedPlugIn.setProcessingOrder(1);
        partitionedPlugIn.setTypeId(partitionedTask.getId());
        partitionedPluginId = api.createPlugin(partitionedPlugIn);
        logger.debug("Configured OrderLineCountBasedUserPartitioningTask Plugin {} .", partitionedPluginId);

    }


    private static final String CUSTOMER_PRICE_SQL =
            "SELECT user_id, price_subscription_date, price_expiry_date FROM customer_price WHERE user_id = ?";

    private void logCustomerPrice(Integer userId) {
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(CUSTOMER_PRICE_SQL, userId);
        logger.debug("fetching customer price for user {}", userId);
        for(Map<String, Object> rowEntry : rows) {
            logger.debug("customer price {}", rowEntry);
        }
    }

    @Test
    public void Test001acMediationInvoiceGenerationTest() throws Exception {

        // setup a BasicFilter which will be used to filter assets on Available status
        AssetWS asset = createAsset("123qwert", product8XXTollFreeId);
        logger.debug("Asset created for product id 603 = {}", asset);

        logger.debug("Creating Account");
        logger.debug("Creating user...");
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2016);
        calendar.set(Calendar.MONTH, 5);
        calendar.set(Calendar.DAY_OF_MONTH, 23);
        UserWS user = FullCreativeUtil.createUser(calendar.getTime()); // Creating User

        UserWS customer = api.getUserWS(user.getId());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2);
        mainSubscription.setNextInvoiceDayOfPeriod(23);

        customer.setMainSubscription(mainSubscription);
        customer.setNextInvoiceDate(calendar.getTime());
        api.updateUser(customer);
        logger.debug("User Id Is: {}", user.getId());


        PlanWS planWS = api.getPlanWS(FullCreativeTestConstants.AF_INTRO_PLAN_ID); //Loading the previously created Plan

        logger.debug("Plan Found for id 700 : {}", planWS);

        //////////////////////////////////////// Creating Order ////////////////////////////////////////////
        Date since= new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2016-06-23 00:00:00.000");
        logger.debug("##Creating order...");
        OrderWS order = new OrderWS();
        order.setUserId(user.getId());
        order.setActiveSince(since);
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(new Integer(2)); // monthly
        order.setCurrencyId(new Integer(1));

        OrderLineWS subscriptionLine = new OrderLineWS();
        subscriptionLine.setItemId(planWS.getItemId());
        subscriptionLine.setAmount("225.00");
        subscriptionLine.setPrice("225.00");
        subscriptionLine.setTypeId(Integer.valueOf(1));
        subscriptionLine.setDescription("");
        subscriptionLine.setQuantity("0");
        subscriptionLine.setUseItem(true);

        OrderLineWS line = new OrderLineWS();
        line.setItemId(Integer.valueOf(product8XXTollFreeId));
        line.setDescription("DID-8XX");
        line.setQuantity("1");
        line.setAmount("0.00");
        line.setTypeId(Integer.valueOf(1));
        line.setPrice("0.95");
        line.setUseItem(true);
        logger.debug("asset : {}", asset.getId());
        line.setAssetIds(new Integer[]{asset.getId()});

        order.setOrderLines(new OrderLineWS[]{subscriptionLine, line});

        //Setting up the asset with the plan
        OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        for (OrderChangeWS ws : orderChanges) {
            ws.setApplicationDate(calendar.getTime());
            ws.setStartDate(calendar.getTime());
            if (ws.getItemId().intValue() == planWS.getItemId().intValue()) {
                OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(product8XXTollFreeId);
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setBundledQuantity(1);
                orderChangePlanItem.setDescription("DID-8XX");
                orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
                orderChangePlanItem.setAssetIds(new int[]{asset.getId()});
                ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
                break;
            }
        }

        Integer orderId	= api.createOrder(order, orderChanges);
        logger.debug("##Order Created with asset:: {}", orderId);
        logCustomerPrice(user.getId());

        OrderWS order1 = api.getOrder(orderId);

        ////////////////////////////////////Running Mediation//////////////////////////////////////////////

        processId = MediationProcess(FullCreativeTestConstants.INBOUND_FILE_NAME1);

        Thread.sleep(60000);

        ProcessStatusWS status = api.getMediationProcessStatus();
        // check previous calls
        assertNotNull("Mediation should be triggered at first time!", processId);
        // todo: possible fail if incorrect mediation configuration or 'quick' machines
        assertNotNull("Status should be retrieved!", status);
        assertNotNull("Start date should be presented", status.getStart());


        ProcessStatusWS completedStatus = api.getMediationProcessStatus();
        assertNotNull("Status should be retrieved", completedStatus);
        if (completedStatus.getMediationProcessId().equals(processId)) {
            logger.debug("Status : {}", completedStatus);
        }

        MediationProcess process = api.getMediationProcess(completedStatus.getMediationProcessId());
        assertNotNull("Mediation process should be retrieved", process);
        assertEquals("Mediation process should be filled", process.getId(), completedStatus.getMediationProcessId());
        assertEquals("Done and Billable records",  new Integer(10), process.getDoneAndBillable());
        assertEquals("Error Detected records", new Integer(0), process.getErrors());
        assertEquals("Total CDR records process",  new Integer(10), process.getRecordsProcessed());

        List<Integer> mediatedOrderIds = Arrays.asList(process.getOrderIds());

        assertNotNull("Order Created through mediation", mediatedOrderIds.size());
        assertEquals("Total Order Created By Process ", new Integer(1), process.getOrderAffectedCount());
        // New asserts to check fetch mediated records count of getMediationRecordsByMediationProcessAndStatus API.
        List<JbillingMediationRecord> mediationRecordListByProcess = Arrays.asList(api.getMediationRecordsByMediationProcessAndStatus(completedStatus.getMediationProcessId().toString(), JbillingMediationRecord.STATUS.PROCESSED.getId()));
        logger.debug("Total mediated records in this mediation process:::::::::: {}", mediationRecordListByProcess.size());
        assertEquals("Total mediated records of status Done and Billable. ", 10, mediationRecordListByProcess.size());
        List<JbillingMediationErrorRecord>  errorRecords= Arrays.asList(api.getMediationErrorRecordsByMediationProcess(completedStatus.getMediationProcessId(), null));
        assertEquals("Total mediated records of status Error Detected. ", 0, errorRecords.size());


        Integer orId = mediatedOrderIds.get(0);
        order1 = api.getOrder(orId);
        logger.debug("Fullcreative Mediation scenario 1 order id {}", orId);
        OrderLineWS orderLine = order1.getOrderLines()[0];
        assertEquals("Expected Order line Quantity: ", "14.4000000000", orderLine.getQuantity());
        assertEquals("Expected Order line Price: " , new BigDecimal("2.5000000000") , orderLine.getPriceAsDecimal());
        assertEquals("Expected Order line Amount: " , new BigDecimal("36.0000000000") , orderLine.getAmountAsDecimal());
        assertEquals("Expected Call Identifier Number: " , asset.getIdentifier() , orderLine.getCallIdentifier());

        /////////////////////////////////Running Mediation 1//////////////////////////////////////////////////
        processId = MediationProcess(FullCreativeTestConstants.INBOUND_FILE_NAME2);

        // check previous calls
        assertNotNull("Mediation should be triggered at first time!", processId);
        // todo: possible fail if incorrect mediation configuration or 'quick' machines

        Thread.sleep(60000);

        ProcessStatusWS status1 = api.getMediationProcessStatus();

        assertNotNull("Status should be retrieved!", status1);
        assertNotNull("Start date should be presented", status1.getStart());


        ProcessStatusWS completedStatus1 = api.getMediationProcessStatus();
        assertNotNull("Status should be retrieved", completedStatus1);
        logger.debug("Status 2:::::::::: {} {}", completedStatus1.getMediationProcessId(), processId);
        if (completedStatus1.getMediationProcessId().equals(processId)) {
            logger.debug("Status : {}", completedStatus1);
        }

        MediationProcess process1 = api.getMediationProcess(completedStatus1.getMediationProcessId());
        assertNotNull("Mediation process should be retrieved", process1);
        assertEquals("Mediation process should be filled", process1.getId(), completedStatus1.getMediationProcessId());
        assertEquals("Done and Billable records",  new Integer(5), process1.getDoneAndBillable());
        assertEquals("Error Detected records", new Integer(0), process1.getErrors());

        assertEquals("Total CDR records process", new Integer(5), process1.getRecordsProcessed());

        mediatedOrderIds = Arrays.asList(process1.getOrderIds());
        assertEquals("Total Order Created By Process ", new Integer(1), process1.getOrderAffectedCount());
        // New asserts to check fetch mediated records count of getMediationRecordsByMediationProcessAndStatus API.
        mediationRecordListByProcess = Arrays.asList(api.getMediationRecordsByMediationProcessAndStatus(completedStatus1.getMediationProcessId().toString(), JbillingMediationRecord.STATUS.PROCESSED.getId()));
        logger.debug("Total mediated records in this mediation process:::::::::: {}", mediationRecordListByProcess.size());
        assertEquals("Total mediated records of status Done and Billable. ", 5, mediationRecordListByProcess.size());
        errorRecords= Arrays.asList(api.getMediationErrorRecordsByMediationProcess(completedStatus1.getMediationProcessId(), null));
        assertEquals("Total mediated records of status Error Detected. ", 0, errorRecords.size());
        assertNotNull("Order Created through mediation", mediatedOrderIds.size());
        assertEquals("Total Order Created By Process ", new Integer(1), process1.getOrderAffectedCount());
        orId = mediatedOrderIds.get(0);



        order1 = api.getOrder(orId);
        orderLine = order1.getOrderLines()[0];
        assertEquals("Expected Order line Quantity: " ,"7.2000000000", orderLine.getQuantity());
        assertEquals("Expected Order line Price: " , new BigDecimal("2.5000000000") , orderLine.getPriceAsDecimal());
        assertEquals("Expected Order line Amount: " , new BigDecimal("18.0000000000") , orderLine.getAmountAsDecimal());
        assertEquals("Expected Call Identifier Number: " , asset.getIdentifier() , orderLine.getCallIdentifier());

        /////////////////////////////////Running Billing Process////////////////////////////////////////////////////////////////////

        cal = new GregorianCalendar();
        cal.clear();
        cal.set(BILLING_PROCESS_YEAR, GregorianCalendar.JUNE, 23, 0, 0, 0);
        triggerBilling(cal.getTime());

        totalInvoices = 65;
        Integer[] invoiceIds = api.getLastInvoices(user.getUserId(), 1);
        InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
        logger.debug("Generated Invoice Id for {}:{}", order1, invoice.getId());
        logger.debug("Total Invoice lines for {} : {}", invoice.getId(), invoice.getInvoiceLines().length);
        assertEquals("Invoice Lines for " + order1 + " parent child should be" , totalInvoices , totalInvoices , invoice.getInvoiceLines().length);
        InvoiceLineDTO[] lines = invoice.getInvoiceLines();
        logger.debug("Invoice ID: {} \n Inspecting invoices lines..", invoice.getId());

        for (InvoiceLineDTO invoiceLine : lines) {
            if (invoiceLine.getDescription() != null && invoiceLine.getDescription().startsWith("Inbound")) {
                assertEquals("Expected Call Identifier Number On Invoice Line: " , orderLine.getCallIdentifier() , invoiceLine.getCallIdentifier());
            }
        }

        ////////////////////////////////////////////Mediation///////////////////////////////////////////////////////////////////////////
        processId = MediationProcess(FullCreativeTestConstants.INBOUND_FILE_NAME3);

        FullCreativeUtil.waitForMediationComplete(api, 70 * 70 * 100);
        ProcessStatusWS status2 = api.getMediationProcessStatus();
        // check previous calls
        assertNotNull("Mediation should be triggered at first time!", processId);
        // todo: possible fail if incorrect mediation configuration or 'quick' machines
        assertNotNull("Status should be retrieved!", status2);
        assertNotNull("Start date should be presented", status2.getStart());

        ProcessStatusWS completedStatus2 = api.getMediationProcessStatus();
        assertNotNull("Status should be retrieved", completedStatus2);
        if (completedStatus2.getMediationProcessId().equals(processId)) {
            logger.debug("Status : {}", completedStatus2);
        }

        MediationProcess process2 = api.getMediationProcess(completedStatus2.getMediationProcessId());
        assertNotNull("Mediation process should be retrieved", process2);
        assertEquals("Mediation process should be filled", process2.getId(), completedStatus2.getMediationProcessId());
        assertEquals("Done and Billable records",  new Integer(4), process2.getDoneAndBillable());
        assertEquals("Error Detected records", new Integer(0), process2.getErrors());
        assertEquals("Total CDR records process", new Integer(4), process2.getRecordsProcessed());
        mediatedOrderIds = Arrays.asList(process2.getOrderIds());

        assertNotNull("Order Created through mediation", mediatedOrderIds.size());
        assertEquals("Total Order Created By Process ", new Integer(1), process2.getOrderAffectedCount());
        // New asserts to check fetch mediated records count of getMediationRecordsByMediationProcessAndStatus API.
        mediationRecordListByProcess = Arrays.asList(api.getMediationRecordsByMediationProcessAndStatus(completedStatus2.getMediationProcessId().toString(), JbillingMediationRecord.STATUS.PROCESSED.getId()));
        logger.debug("Total mediated records in this mediation process:::::::::: {}", mediationRecordListByProcess.size());
        assertEquals("Total mediated records of status Done and Billable. ", 4, mediationRecordListByProcess.size());
        errorRecords= Arrays.asList(api.getMediationErrorRecordsByMediationProcess(completedStatus2.getMediationProcessId(), null));
        assertEquals("Total mediated records of status Error Detected. ", 0, errorRecords.size());
        assertNotNull("Order Created through mediation", mediatedOrderIds.size());

        orId = mediatedOrderIds.get(0);

        order1 = api.getOrder(orId);
        orderLine = order1.getOrderLines()[0];
        assertEquals("Expected Order line Quantity: " ,"6.6000000000", orderLine.getQuantity());
        assertEquals("Expected Order line Price: " , new BigDecimal("2.5000000000") , orderLine.getPriceAsDecimal());
        assertEquals("Expected Order line Amount: " , new BigDecimal("16.5000000000") , orderLine.getAmountAsDecimal());
        assertEquals("Expected Call Identifier Number: " , asset.getIdentifier() , orderLine.getCallIdentifier());

        ///////////////Billing Process//////////////

        cal.clear();
        cal.set(BILLING_PROCESS_YEAR, GregorianCalendar.JULY, 23, 0, 0, 0);
        triggerBilling(cal.getTime());
        Thread.sleep(90000);

        totalInvoices = 6;
        invoiceIds = api.getLastInvoices(user.getUserId(), 1);
        invoice = api.getInvoiceWS(invoiceIds[0]);
        logger.debug("Generated Invoice Id for {} : {}", order1, invoice.getId());
        logger.debug("Total Invoice lines for Invoice {} : {}", invoice.getId(), (invoice.getInvoiceLines().length-1) );
        assertEquals("Invoice Lines for " + order1 + " parent child should be" , totalInvoices , totalInvoices , (invoice.getInvoiceLines().length - 1));

        invoiceIds = api.getAllInvoices(user.getUserId());
        logger.debug("User {} should have two invoices 2, actual result = {}", user.getUserId(), invoiceIds.length);

        lines = invoice.getInvoiceLines();
        logger.debug("Inspecting invoice lines for Invoice: {}", invoice.getId());

        for (InvoiceLineDTO invoiceLine : lines) {
            if (invoiceLine.getDescription() != null && invoiceLine.getDescription().startsWith("Inbound")) {
                assertEquals("Expected Call Identifier Number On Invoice Line: " , orderLine.getCallIdentifier() , invoiceLine.getCallIdentifier());
            }
        }
    }

    @Test
    public void testOrderAffectedCountForInbondMediationJob() throws Exception {

        // setup a BasicFilter which will be used to filter assets on Available status
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setFilters(new BasicFilter[]{basicFilter});
        criteria.setMax(10);

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
        assertNotNull("No available asset found for product "+product8XXTollFreeId, assetsResult);
        AssetWS[] availableAssets = assetsResult.getObjects();
        assertNotNull("No available asset found for product "+product8XXTollFreeId, availableAssets);
        String inboundIdentifier1 = availableAssets[0].getIdentifier();
        String inboundIdentifier2 = availableAssets[1].getIdentifier();

        OrderWS orderForInboundIdentifier1 = createUserAndOrderForAsset(availableAssets[0].getId());
        assertNotNull("Order Creation Failed For Identifier "+ inboundIdentifier1, orderForInboundIdentifier1);

        OrderWS orderForInboundIdentifier2 = createUserAndOrderForAsset(availableAssets[1].getId());
        assertNotNull("Order Creation Failed For Identifier "+ inboundIdentifier2, orderForInboundIdentifier2);


        logger.debug("Asset Available for Inbound Product = {}", inboundIdentifier1);
        logger.debug("Asset Available for Inbound Product = {}", inboundIdentifier2);

        List<String> inboundCdr = new ArrayList<String>();

        inboundCdr.add("us-cs-telephony-voice-101108.vdc-070016UTC-SDsnf8001-3b40152d65e3f05d973466a3a3e3ed2b-v3000i1" + new Random().nextInt(200) +
                ",6165042651,tressie.johnson,Inbound,"+ inboundIdentifier1 +",03/19/2015,12:00:16 AM,4,3,47,2,0,353,47,0,null");

        inboundCdr.add("us-cs-telephony-voice-201108.vdc-070016UTC-SDsnf8001-3b40152d65e3f05d973466a3a3e3ed2b-v3000i1" + new Random().nextInt(300) +
                ",6165042651,tressie.johnson,Inbound,"+ inboundIdentifier2 +",03/19/2015,12:00:16 AM,4,3,47,2,0,353,47,0,null");

        // Trigger Mediation Process For Inbound

        UUID mediationProcessId = api.processCDR(getMediationConfiguration(INBOUND_MEDIATION_LAUNCHER), inboundCdr);

        logger.debug("Mediation Process id is {}", mediationProcessId);

        MediationProcess process = api.getMediationProcess(mediationProcessId);
        assertEquals("Total Order Created By Process ", new Integer(2), process.getOrderAffectedCount());
        logger.debug("OrderAffectedCount is {}", process.getOrderAffectedCount());
    }

    @Test
    public void testCityStateCountryForInbondMediationJob() throws Exception {

        // setup a BasicFilter which will be used to filter assets on Available status
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setFilters(new BasicFilter[]{basicFilter});
        criteria.setMax(10);

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
        assertNotNull("No available asset found for product "+product8XXTollFreeId, assetsResult);

        AssetWS[] availableAssets = assetsResult.getObjects();
        assertNotNull("No available asset found for product "+product8XXTollFreeId, availableAssets);
        String inboundIdentifier = availableAssets[0].getIdentifier();

        OrderWS order = createUserAndOrderForAsset(availableAssets[0].getId());
        assertNotNull("Order Creation Failed For Identifier "+ inboundIdentifier, order);

        logger.debug("Asset Available for Inbound Product = {}", inboundIdentifier);

        List<String> inboundCdr = new ArrayList<String>();

        inboundCdr.add("us-cs-telephony-voice-101108.vdc-070016UTC-SDsnf8001-"+UUID.randomUUID().toString()+
                ",6165042651,tressie.johnson,Inbound,"+ inboundIdentifier +",03/19/2015,12:00:16 AM,4,3,47,2,0,353,47,0,null");

        inboundCdr.add("us-cs-telephony-voice-101108.vdc-070016UTC-SDsnf8001-"+UUID.randomUUID().toString()+
                ",6165042651,tressie.johnson,Inbound,"+ inboundIdentifier +",03/19/2015,12:00:16 AM,4,3,47,2,0,353,47,0,null");

        // Trigger Mediation Process For Inbound

        UUID mediationProcessId = api.processCDR(getMediationConfiguration(INBOUND_MEDIATION_LAUNCHER), inboundCdr);

        logger.debug("Mediation Process id is {}", mediationProcessId);

        Arrays.stream(api.getMediationRecordsByMediationProcessAndStatus(mediationProcessId.toString(), 1))
        .forEach(record -> {
            assertNotNull("City Value Is Null", record.getPricingFieldValueByName("city"));
            assertEquals("Populated City Should Be", "PHOENIX", record.getPricingFieldValueByName("city"));
            assertNotNull("State Value Is Null", record.getPricingFieldValueByName("state"));
            assertEquals("Populated State Should Be", "AZ", record.getPricingFieldValueByName("state"));
            assertNotNull("Country Value Is Null", record.getPricingFieldValueByName("country"));
            assertEquals("Populated Country Should Be", "United States", record.getPricingFieldValueByName("country"));
        });
    }

    @Test
    public void testFailedInboundMediationProcess() throws Exception {

        PluggableTaskWS plugin = api.getPluginWS(basicItemManagerPlugInId);
        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("DNIS_Field_Name", "INVALID-DNIS");
        plugin.setParameters(parameters);
        logger.debug("Updating TelcoUsageManagerTask with invalid DNIS field name");
        api.updatePlugin(plugin);

        // setup a BasicFilter which will be used to filter assets on Available status
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setSort("id");
        criteria.setFilters(new BasicFilter[]{basicFilter});
        criteria.setMax(10);

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
        assertNotNull("No available asset found for product "+product8XXTollFreeId, assetsResult);
        AssetWS[] availableAssets = assetsResult.getObjects();
        assertNotNull("No available asset found for product "+product8XXTollFreeId, availableAssets);
        String inboundIdentifier = availableAssets[0].getIdentifier();

        OrderWS orderForInboundIdentifier = createUserAndOrderForAsset(availableAssets[0].getId());
        assertNotNull("Order Creation Failed For Identifier "+ inboundIdentifier, orderForInboundIdentifier);

        logger.debug("Asset Available for Inbound Product = {}", inboundIdentifier);

        List<String> inboundCdrs = new ArrayList<String>();

        inboundCdrs.add("us-cs-telephony-voice-1111108.vdc-070016UTC-SDsnf8001-3b40152d65e3f05d973466a3a3e3ed2b-v3000i1" + new Random().nextInt(200) +
                ",6165042651,tressie.johnson,Inbound,"+ inboundIdentifier +",03/19/2015,12:00:16 AM,4,3,47,2,0,353,47,0,null");

        inboundCdrs.add("us-cs-telephony-voice-2011108.vdc-070016UTC-SDsnf8001-3b40152d65e3f05d973466a3a3e3ed2b-v3000i1" + new Random().nextInt(300) +
                ",6165042651,tressie.johnson,Inbound,"+ inboundIdentifier +",03/19/2015,12:00:16 AM,4,3,47,2,0,353,47,0,null");

        // Trigger Mediation Process For Inbound

        UUID mediationProcessId = api.processCDR(getMediationConfiguration(INBOUND_MEDIATION_LAUNCHER), inboundCdrs);

        logger.debug("Mediation Process id is {}", mediationProcessId);
        MediationProcess process = api.getMediationProcess(mediationProcessId);
        assertNotNull("Mediation process should be retrieved", process);
        assertEquals("Total Order Created By Process ", new Integer(0), process.getOrderAffectedCount());
        assertEquals("Error Detected records", new Integer(2), process.getErrors());
        assertNotNull("Mediation process is still running", process.getEndDate());

        // Updating TELCO Usage MAnager Task With Valid DNIS field Name
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME, api);

    }

    private OrderWS createUserAndOrderForAsset(Integer assetId) throws Exception {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2016);
        calendar.set(Calendar.MONTH, 5);
        calendar.set(Calendar.DAY_OF_MONTH, 23);

        UserWS user = FullCreativeUtil.createUser(calendar.getTime()); // Creating User

        UserWS customer = api.getUserWS(user.getId());
        MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
        mainSubscription.setPeriodId(2);
        mainSubscription.setNextInvoiceDayOfPeriod(23);

        customer.setMainSubscription(mainSubscription);
        customer.setNextInvoiceDate(calendar.getTime());
        api.updateUser(customer);
        logger.debug("User Id Is: {}", user.getId());

        OrderWS order = new OrderWS();
        order.setUserId(user.getId());
        order.setActiveSince(new Date());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(new Integer(2)); // monthly
        order.setCurrencyId(new Integer(1));

        OrderLineWS line = new OrderLineWS();
        line.setItemId(Integer.valueOf(product8XXTollFreeId));
        line.setDescription("DID-8XX");
        line.setQuantity("1");
        line.setAmount("0.00");
        line.setTypeId(Integer.valueOf(1));
        line.setPrice("0.95");
        line.setUseItem(true);
        line.setAssetIds(new Integer[]{assetId});

        order.setOrderLines(new OrderLineWS[]{line});

        OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);

        Integer orderId	= api.createOrder(order, orderChanges);
        logger.debug("Order Created with asset :: {}", orderId);

        return api.getOrder(orderId);
    }


    private Integer getMediationConfiguration(String mediationJobLauncher) {

        MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
        for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
            if (null != mediationConfigurationWS.getMediationJobLauncher() &&
                    (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
                return mediationConfigurationWS.getId();
            }
        }
        return null;
    }

    private UUID MediationProcess(String fileName){
        MEDIATION_COUNT++;
        logger.debug("testTrigger-Inbound{}", MEDIATION_COUNT);
        String localResourceDir = Util.getSysProp("base_dir") + "mediation/data/";
        File iFile=new File(localResourceDir+fileName);
        logger.debug("ACMediationInvoiceGenerationTest  ############## File Name is:::::: {}", iFile);
        Integer INBOUND_CALLS_CFG_ID =  getMediationConfiguration(INBOUND_MEDIATION_LAUNCHER);
        UUID processId = api.triggerMediationByConfigurationByFile(INBOUND_CALLS_CFG_ID,iFile);
        return processId;

    }

    private void triggerBilling(Date runDate) {
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        config.setNextRunDate(runDate);
        config.setRetries(new Integer(1));
        config.setDaysForRetry(new Integer(5));
        config.setGenerateReport(new Integer(0));
        config.setAutoPaymentApplication(new Integer(0));
        config.setDfFm(new Integer(0));
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(new Integer(0));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(99));
        config.setOnlyRecurring(new Integer(0));

        logger.debug("B - Setting config to: {}", config);
        api.createUpdateBillingProcessConfiguration(config);

        logger.debug("Running Billing Process for {}", runDate );
        api.triggerBilling(runDate);
    }

    @AfterClass
    public void cleanUp(){
        FullCreativeUtil.updatePlugin(basicItemManagerPlugInId, FullCreativeTestConstants.BASIC_ITEM_MANAGER_TASK_NAME, api);
        PluggableTaskWS plugin = api.getPluginWS(invoiceCompositionPlugInId);
        plugin.setTypeId(api.getPluginTypeWSByClassName(FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME).getId());
        api.updatePlugin(plugin);
        if(null!= partitionedPluginId) {
            api.deletePlugin(partitionedPluginId);
        }
        try {
            if(ratingSchemeId!=-1) {
                logger.debug("Deleting persisted rating scheme.");
                Boolean deleted = api.deleteRatingScheme(ratingSchemeId);
                assertTrue(deleted);
            }
        } catch (Exception e) {
            fail("Rating Scheme deletion failed.");
        }
    }

    private AssetWS createAsset(String identifierValue, Integer itemId) {
        ItemTypeWS itemTypeWS = api.getItemCategoryById(api.getItem(itemId, null, null).getTypes()[0]);
        Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                .equals("Available")).collect(Collectors.toList()).get(0).getId();
        logger.debug("Creating asset...");
        AssetWS asset = new AssetWS();
        asset.setIdentifier(identifierValue);
        asset.setItemId(itemId);
        asset.setEntityId(Integer.valueOf(1));
        asset.setEntities(Arrays.asList(api.getCallerCompanyId()));
        asset.setAssetStatusId(assetStatusId);
        asset.setDeleted(0);

        Integer assetId = api.createAsset(asset);
        asset = api.getAsset(assetId);

        logger.debug("Asset created with id:{}", asset.getId());
        return asset;
    }
}
