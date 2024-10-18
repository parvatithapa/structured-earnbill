package com.sapienter.jbilling.server.distributel;

import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationErrorRecord;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderStatusWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.spa.AddressType;
import com.sapienter.jbilling.server.spa.SpaAddressWS;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaErrorCodes;
import com.sapienter.jbilling.server.spa.SpaImportWS;
import com.sapienter.jbilling.server.spa.SpaPaymentCredentialWS;
import com.sapienter.jbilling.server.spa.SpaPaymentResultWS;
import com.sapienter.jbilling.server.spa.SpaProductsOrderedWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.DistributelAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by pablo_galera on 25/01/17.
 */
@Test(groups = { "test-distributel", "distributel" }, testName = "DistributelMediationTest")
    public class DistributelMediationTest {

    private static final Logger logger = LoggerFactory.getLogger(DistributelMediationTest.class);
    private static final String MEDIATION_CONFIGURATION = "TestConfiguration";
    private static final String DISTRIBUTEL_LAUNCHER_JOB = "distributelMediationJob";
    private static final String DISTRIBUTEL_MEDIATION_FILE_NAME = "ACANAC.20160918.0.%s.1of1.INV.tmp";
    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static Integer ORDER_PERIOD_MONTHLY;
    private static JbillingAPI api;
    private static Integer PRANCING_PONY_HARDWARE_CATEGORY_ID;
    private static Integer PRANCING_PONY_FEE_CATEGORY_ID;
    private static String PROVINCE_QC = "QC" + System.currentTimeMillis();
    private static final String ASSET_STATE_IN_USE = "In Use";
    private static final String ASSET_STATE_AVAILABLE = "Available";
    private static final String ASSET_STATE_PENDING = "Pending";
    private static Integer ASSET_STATE_IN_USE_ID;
    private static Integer HARDWARE_PRODUCT_ID;
    private static Integer FEE_PRODUCT_ID;
    private static Integer MODEM_PLAN_ID;
    private static Integer SIMPLE_PLAN_ID;
    private static final String BANFF_ACCOUNT_ID = "1111AAAA";
    private static final String DISTRIBUTEL_ITEM_CODE = "DISTRIBUTEL-ITEM";
    private static Integer DISTRIBUTEL_PRODUCT_CATEGORY_ID;
    private Integer updateDistriubtelCustomerTaskId;

    @BeforeClass
    public void initializeSPAImportEnrollment() throws Exception {
        api = JbillingAPIFactory.getAPI();
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        DistributelTestUtil.buildAccountType(api);
        DistributelTestUtil.updatePaymentMethodTypeCreditCard(api);
        DistributelTestUtil.initMetafieldCreation();
        PRANCING_PONY_HARDWARE_CATEGORY_ID = DistributelTestUtil.createItemCategory("MCF Rated", Constants.ORDER_LINE_TYPE_ITEM, true, api);
        PRANCING_PONY_FEE_CATEGORY_ID = DistributelTestUtil.createItemCategory("FEE", Constants.ORDER_LINE_TYPE_ITEM, false, api);
        DISTRIBUTEL_PRODUCT_CATEGORY_ID = DistributelTestUtil.createItemCategory("Distributel category", Constants.ORDER_LINE_TYPE_ITEM, false, api);
        createTestProduct();
        MODEM_PLAN_ID = createPlanModem();
        createFeePlan();
        SIMPLE_PLAN_ID = createSimplePlan();

        ItemTypeWS itemTypeWS = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);
        DistributelTestUtil.addAssetStatus(itemTypeWS, "Reserved");
        updateDistriubtelCustomerTaskId = DistributelTestUtil.enablePlugin(DistributelTestUtil.UPDATE_DISTRIBUTEL_CUSTOMER, api);

    }


    @Test
    public void testDistributelMediationWithOneValidCDR() throws Exception {
        String phoneNumber = String.valueOf(new Random().nextInt());
        SpaImportWS spaImportWS = getSpaImportWSForTest(phoneNumber);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        AssertJUnit.assertFalse(String.format("User %s not found", spaImportWS.getCustomerName()), SpaErrorCodes.GENERAL_ERROR.getValue().equals(userId));
        UserWS userWS = api.getUserWS(userId);
        AssertJUnit.assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);
        Integer mediationConfigurationId = createMediationConfiguration();

        logger.debug("validating file");
        String localResourceDir = "mediation/data/" + String.format(DISTRIBUTEL_MEDIATION_FILE_NAME,"1");
        File aFile = new File(localResourceDir);
        try {
            FileUtils.writeStringToFile(aFile, validCallDataRecord(userId.toString(), phoneNumber, new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("DistributelMediationTest File Name is {}", aFile);
        UUID processId = api.triggerMediationByConfigurationByFile(mediationConfigurationId, aFile);
        logger.debug("mediationProcessId is {}", processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        MediationProcess mediationProcess = api.getMediationProcess(processId);
        logger.debug("mediationProcess.getDoneAndBillable() {}", mediationProcess.getDoneAndBillable());
        logger.debug("mediationProcess.getErrors() {}", mediationProcess.getErrors());
        logger.debug("mediationProcess.getDuplicates() {}", mediationProcess.getDuplicates());
        assertEquals(new Integer(1), mediationProcess.getDoneAndBillable());
        OrderWS order = api.getLatestOrder(userId);
        boolean detailFileNameMFFound = false;
        for (MetaFieldValueWS mf : order.getMetaFields()) {
            logger.debug("mf.getFieldName() {}", mf.getFieldName());
            logger.debug("mf.getStringValue() {}", mf.getStringValue());
            if (SpaConstants.DETAIL_FILE_NAMES.equals(mf.getFieldName()) &&
                    "ACANAC.20160918.207139.4184765472.1000.pdf".equals(mf.getStringValue())) {
                detailFileNameMFFound = true;
            }
        }

        assertTrue("Detail File Name present", detailFileNameMFFound);
        assertTrue("Total for Order", new BigDecimal("100").compareTo(order.getTotalAsDecimal()) == 0);
        assertEquals("Order Line description", order.getOrderLines()[0].getDescription(), "VOIP 4184765472 long distance");

        api.deleteOrder(order.getId());
        api.deleteUser(userId);
        api.undoMediation(processId);
        aFile.delete();

    }

    @Test
    public void testDistributelMediationErrors() throws Exception {
        String phoneNumber = String.valueOf(new Random().nextInt());
        SpaImportWS spaImportWS = getSpaImportWSForTest(phoneNumber);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        AssertJUnit.assertFalse(String.format("User %s not found", spaImportWS.getCustomerName()), SpaErrorCodes.GENERAL_ERROR.getValue().equals(userId));
        UserWS userWS = api.getUserWS(userId);
        AssertJUnit.assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);
        Integer mediationConfigurationId = createMediationConfiguration();

        logger.debug("validating file");
        String localResourceDir = "mediation/data/" + String.format(DISTRIBUTEL_MEDIATION_FILE_NAME,"2");
        File aFile = new File(localResourceDir);
        try {
            FileUtils.writeStringToFile(aFile, validCallDataRecord(userId.toString(), phoneNumber, new Date()) + "\n" + validCallDataRecord("0", "00000000", new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("DistributelMediationTest File Name is {}", aFile);
        UUID processId = api.triggerMediationByConfigurationByFile(mediationConfigurationId, aFile);
        logger.debug("mediationProcessId is {}", processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        MediationProcess mediationProcess = api.getMediationProcess(processId);

        assertNotNull("Mediation process expected!", mediationProcess);
        assertEquals("Invalid number of processed records!", Integer.valueOf(2), mediationProcess.getRecordsProcessed());
        assertEquals("Invalid number of done and billable records!", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
        assertEquals("Invalid number of error records!", Integer.valueOf(1), mediationProcess.getErrors());

        JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(processId, null);
        assertTrue("Invalid error code!", errorRecords[0].getErrorCodes().contains("User not found, JB-USER-NOT-RESOLVED"));

        api.deleteOrder(api.getLatestOrder(userId).getId());
        api.deleteUser(userId);
        api.undoMediation(processId);
        aFile.delete();
    }

    @Test
    public void testDistributelMediationRecycleItemError() throws Exception {
        String phoneNumber = String.valueOf(new Random().nextInt());
        SpaImportWS spaImportWS = getSpaImportWSForTest(phoneNumber);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        AssertJUnit.assertFalse(String.format("User %s not found", spaImportWS.getCustomerName()), SpaErrorCodes.GENERAL_ERROR.getValue().equals(userId));
        UserWS userWS = api.getUserWS(userId);
        AssertJUnit.assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);
        Integer mediationConfigurationId = createMediationConfiguration();

        ItemDTOEx[] items = api.getItemByCategory(DISTRIBUTEL_PRODUCT_CATEGORY_ID);
        items[0].setNumber(DISTRIBUTEL_ITEM_CODE +"-test");
        items[0].setReservationDuration(60000);
        api.updateItem(items[0]);

        logger.debug("validating file");
        String localResourceDir = "mediation/data/" + String.format(DISTRIBUTEL_MEDIATION_FILE_NAME,"1");
        File aFile = new File(localResourceDir);
        try {
            FileUtils.writeStringToFile(aFile, validCallDataRecord(userId.toString(), phoneNumber, new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("DistributelMediationTest File Name is {}", aFile);
        UUID processId = api.triggerMediationByConfigurationByFile(mediationConfigurationId, aFile);
        logger.debug("mediationProcessId is {}", processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        MediationProcess mediationProcess = api.getMediationProcess(processId);
        assertEquals("Invalid number of done and billable records!", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
        assertEquals("Invalid number of error records!", Integer.valueOf(1), mediationProcess.getErrors());

        items = api.getItemByCategory(DISTRIBUTEL_PRODUCT_CATEGORY_ID);
        items[0].setNumber(DISTRIBUTEL_ITEM_CODE);
        items[0].setReservationDuration(60000);
        api.updateItem(items[0]);
        processId = api.runRecycleForProcess(processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        mediationProcess = api.getMediationProcess(processId);
        assertEquals("Invalid number of done and billable records with recycle process!", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
        assertEquals("Invalid number of error records with recycle process!", Integer.valueOf(0), mediationProcess.getErrors());

        api.deleteOrder(api.getLatestOrder(userId).getId());
        api.deleteUser(userId);
        api.undoMediation(processId);
        aFile.delete();
    }

    /**
     * Mediatioin with valid CDR records and users latest order is finished
     *
     * @throws Exception
     */
    @Test
    public void testMediationScenario1() throws Exception {
        String phoneNumber = String.valueOf(new Random().nextInt());
        SpaImportWS spaImportWS = getSpaImportWSForTest(phoneNumber);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        AssertJUnit.assertFalse(String.format("User %s not found", spaImportWS.getCustomerName()), SpaErrorCodes.GENERAL_ERROR.getValue().equals(userId));
        UserWS userWS = api.getUserWS(userId);
        AssertJUnit.assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);
        Integer mediationConfigurationId = createMediationConfiguration();

        OrderWS[] orders = api.getUserSubscriptions(userId);

        List<Integer> orderIds = Arrays.asList(orders)
                .stream()
                .map(order -> order.getId())
                .collect(Collectors.toList());
        Collections.sort(orderIds, Collections.reverseOrder());
        OrderWS latestOrder = api.getOrder(orderIds.get(0));
        latestOrder.setActiveUntil(new Date());
        latestOrder.setOrderStatusWS(new OrderStatusWS(api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId()),
                                                    api.getCompany(),
                                                    OrderStatusFlag.FINISHED,
                                                    "Finished"));
        api.updateOrder(latestOrder, null);
        latestOrder = api.getOrder(latestOrder.getId());

        logger.debug("validating file");
        String localResourceDir = "mediation/data/" + String.format(DISTRIBUTEL_MEDIATION_FILE_NAME,"1");
        File aFile = new File(localResourceDir);
        try {
            FileUtils.writeStringToFile(aFile, validCallDataRecord(userId.toString(), phoneNumber, new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("DistributelMediationTest File Name is {}", aFile);
        UUID processId = api.triggerMediationByConfigurationByFile(mediationConfigurationId, aFile);
        logger.debug("mediationProcessId is {}", processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        MediationProcess mediationProcess = api.getMediationProcess(processId);
        logger.debug("mediationProcess.getDoneAndBillable() {}", mediationProcess.getDoneAndBillable());
        logger.debug("mediationProcess.getErrors() {}", mediationProcess.getErrors());
        logger.debug("mediationProcess.getDuplicates() {}", mediationProcess.getDuplicates());
        assertEquals(new Integer(1), mediationProcess.getDoneAndBillable());
        OrderWS order = api.getLatestOrder(userId);
        boolean detailFileNameMFFound = false;
        for (MetaFieldValueWS mf : order.getMetaFields()) {
            logger.debug("mf.getFieldName() {}", mf.getFieldName());
            logger.debug("mf.getStringValue() {}", mf.getStringValue());
            if (SpaConstants.DETAIL_FILE_NAMES.equals(mf.getFieldName()) &&
                    "ACANAC.20160918.207139.4184765472.1000.pdf".equals(mf.getStringValue())) {
                detailFileNameMFFound = true;
            }
        }

        assertTrue("Detail File Name present", detailFileNameMFFound);
        assertTrue("Order quantity", new BigDecimal("1").compareTo(order.getOrderLines()[0].getQuantityAsDecimal()) == 0);
        assertTrue("Total for Order", new BigDecimal("100").compareTo(order.getTotalAsDecimal()) == 0);
        assertEquals("Order Line description", order.getOrderLines()[0].getDescription(), "VOIP 4184765472 long distance");

        api.deleteOrder(order.getId());
        api.deleteUser(userId);
        api.undoMediation(processId);
        aFile.delete();
    }


    /**
     * Mediatioin with valid CDR records and users second last latest order is finished
     *
     * @throws Exception
     */
    @Test
    public void testMediationScenario2() throws Exception {
        String phoneNumber = String.valueOf(new Random().nextInt());
        SpaImportWS spaImportWS = getSpaImportWSForTest(phoneNumber);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        AssertJUnit.assertFalse(String.format("User %s not found", spaImportWS.getCustomerName()), SpaErrorCodes.GENERAL_ERROR.getValue().equals(userId));
        UserWS userWS = api.getUserWS(userId);
        AssertJUnit.assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);
        Integer mediationConfigurationId = createMediationConfiguration();

        OrderWS[] orders = api.getUserSubscriptions(userId);
        List<Integer> orderIds = Arrays.asList(orders)
                .stream()
                .map(order -> order.getId())
                .collect(Collectors.toList());
        Collections.sort(orderIds);
        OrderWS latestOrder = api.getOrder(orderIds.get(0));
        latestOrder.setActiveUntil(new Date());
        latestOrder.setOrderStatusWS(new OrderStatusWS(api.getDefaultOrderStatusId(OrderStatusFlag.FINISHED, api.getCallerCompanyId()),
                                                    api.getCompany(),
                                                    OrderStatusFlag.FINISHED,
                                                    "Finished"));
        api.updateOrder(latestOrder, null);
        latestOrder = api.getOrder(latestOrder.getId());
        logger.debug("validating file");
        String localResourceDir = "mediation/data/" + String.format(DISTRIBUTEL_MEDIATION_FILE_NAME,"1");
        File aFile = new File(localResourceDir);
        try {
            FileUtils.writeStringToFile(aFile, validCallDataRecord(userId.toString(), phoneNumber, new Date()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("DistributelMediationTest File Name is {}", aFile);
        UUID processId = api.triggerMediationByConfigurationByFile(mediationConfigurationId, aFile);
        logger.debug("mediationProcessId is {}", processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        MediationProcess mediationProcess = api.getMediationProcess(processId);
        logger.debug("mediationProcess.getDoneAndBillable() {}", mediationProcess.getDoneAndBillable());
        logger.debug("mediationProcess.getErrors() {}", mediationProcess.getErrors());
        logger.debug("mediationProcess.getDuplicates() {}", mediationProcess.getDuplicates());
        assertEquals(new Integer(1), mediationProcess.getDoneAndBillable());

        OrderWS order = api.getLatestOrder(userId);
        boolean detailFileNameMFFound = false;
        for (MetaFieldValueWS mf : order.getMetaFields()) {
            logger.debug("mf.getFieldName() {}", mf.getFieldName());
            logger.debug("mf.getStringValue() {}", mf.getStringValue());
            if (SpaConstants.DETAIL_FILE_NAMES.equals(mf.getFieldName()) &&
                    "ACANAC.20160918.207139.4184765472.1000.pdf".equals(mf.getStringValue())) {
                detailFileNameMFFound = true;
            }
        }

        assertTrue("Detail File Name present", detailFileNameMFFound);
        assertTrue("Order quantity", new BigDecimal("1").compareTo(order.getOrderLines()[0].getQuantityAsDecimal()) == 0);
        assertTrue("Total for Order", new BigDecimal("100").compareTo(order.getTotalAsDecimal()) == 0);
        assertEquals("Order Line description", order.getOrderLines()[0].getDescription(), "VOIP 4184765472 long distance");

        api.deleteOrder(order.getId());
        api.deleteUser(userId);
        api.undoMediation(processId);
        aFile.delete();
    }

    /**
     * Mediation with CDR record but mediate record active since date is not between users latest orders active since and active until date.
     *
     * @throws Exception
     */
    @Test
    public void testMediationScenario3() throws Exception {
        String phoneNumber = String.valueOf(new Random().nextInt());
        SpaImportWS spaImportWS = getSpaImportWSForTest(phoneNumber);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        AssertJUnit.assertFalse(String.format("User %s not found", spaImportWS.getCustomerName()), SpaErrorCodes.GENERAL_ERROR.getValue().equals(userId));
        UserWS userWS = api.getUserWS(userId);
        AssertJUnit.assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);
        Integer mediationConfigurationId = createMediationConfiguration();

        OrderWS[] orders = api.getUserSubscriptions(userId);
        List<Integer> orderIds = Arrays.asList(orders)
                .stream()
                .map(order -> order.getId())
                .collect(Collectors.toList());
        Collections.sort(orderIds, Collections.reverseOrder());
        OrderWS latestOrder = api.getOrder(orderIds.get(0));
        latestOrder.setActiveSince(getYesterdaysDate());
        latestOrder.setActiveUntil(new Date());
        api.updateOrder(latestOrder, null);
        latestOrder = api.getOrder(latestOrder.getId());

        logger.debug("validating file");
        String localResourceDir = "mediation/data/" + String.format(DISTRIBUTEL_MEDIATION_FILE_NAME,"1");
        File aFile = new File(localResourceDir);
        try {
            FileUtils.writeStringToFile(aFile, validCallDataRecord(userId.toString(), phoneNumber, getTomorrowsDate()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.debug("DistributelMediationTest File Name is {}", aFile);
        UUID processId = api.triggerMediationByConfigurationByFile(mediationConfigurationId, aFile);
        logger.debug("mediationProcessId is {}", processId);
        FullCreativeUtil.waitForMediationComplete(api, 80 * 80 * 100);

        MediationProcess mediationProcess = api.getMediationProcess(processId);
        logger.debug("mediationProcess.getDoneAndBillable() {}", mediationProcess.getDoneAndBillable());
        logger.debug("mediationProcess.getErrors() {}", mediationProcess.getErrors());
        logger.debug("mediationProcess.getDuplicates() {}", mediationProcess.getDuplicates());

        assertNotNull("Mediation process expected!", mediationProcess);
        assertEquals("Invalid number of processed records!", Integer.valueOf(1), mediationProcess.getRecordsProcessed());
        assertEquals("Invalid number of done and billable records!", Integer.valueOf(0), mediationProcess.getDoneAndBillable());
        assertEquals("Invalid number of error records!", Integer.valueOf(1), mediationProcess.getErrors());

        JbillingMediationErrorRecord[] errorRecords = api.getMediationErrorRecordsByMediationProcess(processId, null);
        assertTrue("Invalid error code!", errorRecords[0].getErrorCodes().contains("User has invalid order, Inactive Customer, JB-USER-NOT-RESOLVED"));

        api.deleteOrder(api.getLatestOrder(userId).getId());
        api.deleteUser(userId);
        api.undoMediation(processId);
        aFile.delete();
    }

    private Date getYesterdaysDate() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, -1);
        return c.getTime();
    }
    private Date getTomorrowsDate() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        c.add(Calendar.DATE, 1);
        return c.getTime();
    }

    private String validCallDataRecord(String pCustomerNumber, String phoneNumber, Date date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        String InvRecordVersion = "INV1";
        String RecSequenceNumber ="41";
        String BillingDate =format.format(date);
        String RevenueStartDate =format.format(date);
        String RevenueEndDate =format.format(date);
        String BusinessUnit ="ACANAC";
        String CustomerID =pCustomerNumber;
        String BillingIdentifier=phoneNumber;
        String ProductNature ="LONGDISTANCE";
        String BillingIndicator="10";
        String SubscriptionOrderID ="1000";
        String InvoiceSegment="0";
        String Language="E";
        String InvoiceDescriptiveText="VOIP 4184765472 long distance";
        String Calls="1";
        String DollarAmount="100";
        String HST ="0";
        String GST="0";
        String PST="0";
        String  TaxProvinceCode="0";
        String TaxFlag="0";
        String DetailType ="2";
        String DetailBaseFilename ="ACANAC.20160918.207139.4184765472.1000";

        return StringUtils.join(Arrays.asList(
                InvRecordVersion,
                RecSequenceNumber,
                BillingDate,
                RevenueStartDate,
                RevenueEndDate,
                BusinessUnit,
                CustomerID,
                BillingIdentifier,
                ProductNature,
                BillingIndicator,
                SubscriptionOrderID,
                InvoiceSegment,
                Language,
                InvoiceDescriptiveText,
                Calls,
                DollarAmount,
                HST,
                GST,
                PST,
                TaxProvinceCode,
                TaxFlag,
                DetailType,
                DetailBaseFilename
        ), ",");
    }

    private void createMetafield(JbillingAPI api) {
        MetaFieldWS metafieldOrder = DistributelTestUtil.getMetafield(SpaConstants.DETAIL_FILE_NAMES, DataType.STRING, EntityType.ORDER);
        MetaFieldWS[] metafields = api.getMetaFieldsForEntity(EntityType.ORDER.name());
        boolean existMF = Arrays.stream(metafields).anyMatch(t -> t.getName().equals(metafieldOrder.getName()));
        if (!existMF) {
            api.createMetaField(DistributelTestUtil.getMetafield(SpaConstants.DETAIL_FILE_NAMES, DataType.STRING, EntityType.ORDER));
        }
    }

    private SpaImportWS getSpaImportWSForTest(String phoneNumber) {
        SpaImportWS spaImportWS = new SpaImportWS();
        String customerName = "customer-" + new Random().nextInt();
        spaImportWS.setCustomerName(customerName);
        spaImportWS.setCustomerCompany("customerCompany");
        spaImportWS.setPhoneNumber1("phoneNumber1");
        spaImportWS.setPhoneNumber2("phoneNumber2");
        spaImportWS.setEmailAddress("test@jbilling.com");
        spaImportWS.setEmailVerified(new Date());
        spaImportWS.setLanguage("E");
        SpaAddressWS serviceAddress = new SpaAddressWS();
        serviceAddress.setAddressType(AddressType.SERVICE.name());
        serviceAddress.setPostalCode("postalCode");
        serviceAddress.setCity("city");
        serviceAddress.setProvince(PROVINCE_QC);
        List<SpaAddressWS> addresses = new ArrayList<>();
        addresses.add(serviceAddress);
        SpaAddressWS billingAddress = new SpaAddressWS();
        billingAddress.setAddressType(AddressType.BILLING.name());
        billingAddress.setPostalCode("postalCode");
        billingAddress.setCity("city");
        billingAddress.setProvince(PROVINCE_QC);
        addresses.add(billingAddress);

        spaImportWS.setAddresses(addresses);

        SpaProductsOrderedWS mainOfferingPlan = new SpaProductsOrderedWS();
        mainOfferingPlan.setServiceType("VOIP");
        mainOfferingPlan.setPhoneNumber(phoneNumber);
        mainOfferingPlan.setInstallationTime("08:20");
        mainOfferingPlan.setStartDate(new Date());
        mainOfferingPlan.setPlanId(SIMPLE_PLAN_ID);
        mainOfferingPlan.setModemId(MODEM_PLAN_ID);
        mainOfferingPlan.setBanffAccountId(BANFF_ACCOUNT_ID);

        List<SpaProductsOrderedWS> productsOrderedWSList = new ArrayList<SpaProductsOrderedWS>();
        productsOrderedWSList.add(mainOfferingPlan);
        spaImportWS.setProductsOrdered(productsOrderedWSList);
        SpaPaymentCredentialWS spaPaymentCredentialWS = new SpaPaymentCredentialWS();
        spaPaymentCredentialWS.setCcname("Frodo Baggins");
        spaPaymentCredentialWS.setCcmonth("01");
        spaPaymentCredentialWS.setCcyear("2017");
        spaPaymentCredentialWS.setCcnumber("************1111");
        spaPaymentCredentialWS.setCustomerToken("123456");
        SpaPaymentResultWS spaPaymentResultWS = new SpaPaymentResultWS();
        spaPaymentResultWS.setAmount(new BigDecimal("10"));
        spaPaymentResultWS.setResult("Successful");
        spaPaymentResultWS.setTransactionToken("transactiontokentest");
        spaImportWS.setPaymentCredential(spaPaymentCredentialWS);
        spaImportWS.setPaymentResult(spaPaymentResultWS);

        return spaImportWS;
    }

    private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api) {
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for (OrderPeriodWS period : periods) {
            if (1 == period.getValue() &&
                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()) {
                return period.getId();
            }
        }
        //there is no monthly order period so create one
        OrderPeriodWS monthly = new OrderPeriodWS();
        monthly.setEntityId(api.getCallerCompanyId());
        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
        monthly.setValue(1);
        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
        return api.createOrderPeriod(monthly);
    }

    private void createTestProduct() {
        ItemDTOEx firstItem = createProduct(BigDecimal.ONE, "Modem".concat(String.valueOf(System.currentTimeMillis())), true, PRANCING_PONY_HARDWARE_CATEGORY_ID);
        HARDWARE_PRODUCT_ID = api.createItem(firstItem);

        ItemDTOEx secondItem = createProduct(BigDecimal.TEN, "FEE Product".concat(String.valueOf(System.currentTimeMillis())), false, PRANCING_PONY_FEE_CATEGORY_ID);
        FEE_PRODUCT_ID = api.createItem(secondItem);

        ItemDTOEx thirdItem = createProduct(BigDecimal.TEN, "Product".concat(String.valueOf(System.currentTimeMillis())), false, PRANCING_PONY_FEE_CATEGORY_ID);
        api.createItem(thirdItem);

        if(api.getItemID(DISTRIBUTEL_ITEM_CODE) == null) {
            ItemDTOEx distributelItem = createProduct(BigDecimal.TEN, DISTRIBUTEL_ITEM_CODE, false, DISTRIBUTEL_PRODUCT_CATEGORY_ID);

            PriceModelWS graduatedPrices = new PriceModelWS();
            graduatedPrices.setCurrencyId(api.getCallerCurrencyId());
            graduatedPrices.setType(PriceModelStrategy.FIELD_BASED.name());
            graduatedPrices.addAttribute("rate_pricing_field_name", "DollarAmount");
            graduatedPrices.addAttribute("apply_percentage", "0");
            distributelItem.addDefaultPrice(new Date(), graduatedPrices);
            api.createItem(distributelItem);
        }
    }

    private ItemDTOEx createProduct(BigDecimal price, String productNumber, boolean assetsManagementEnabled, Integer categoryId) {
        ItemDTOEx product = CreateObjectUtil.createItem(
                PRANCING_PONY_ENTITY_ID, price, Constants.PRIMARY_CURRENCY_ID, categoryId,
                trimToLength("Test " + productNumber, 35));
        product.setNumber(trimToLength(productNumber, 50));
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
        return product;
    }

    private String trimToLength(String value, int length) {
        if (value == null || value.length() < length) return value;
        return value.substring(0, length);
    }

    private Integer createPlanModem() {

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap();
        models.put(Constants.EPOCH_DATE, priceModel);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle1.setQuantity(BigDecimal.ONE);
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setItemId(HARDWARE_PRODUCT_ID);
        pi1.setPrecedence(-1);
        pi1.setModels(models);
        pi1.setBundle(bundle1);

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        bundle2.setQuantity(BigDecimal.ONE);
        bundle2.setAddIfExists(true);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(FEE_PRODUCT_ID);
        pi2.setPrecedence(-1);
        priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID);
        models = new TreeMap();
        models.put(Constants.EPOCH_DATE, priceModel);
        pi2.setModels(models);
        pi2.setBundle(bundle2);

        ItemDTOEx planItem = createProduct(BigDecimal.ZERO,  "PLAN - " + Short.toString((short) System.currentTimeMillis()), false);
        planItem.setId(api.createItem(planItem));
        PlanWS goldServicePlan = new PlanWS();
        goldServicePlan.setItemId(planItem.getId());
        goldServicePlan.setDescription("Test Plan - " + Short.toString((short) System.currentTimeMillis()));
        goldServicePlan.setPeriodId(ORDER_PERIOD_MONTHLY);
        goldServicePlan.addPlanItem(pi1);
        goldServicePlan.addPlanItem(pi2);

        //goldServicePlan.setMetaFields(metafields);
        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap();
        //metafieldMap.put(PRANCING_PONY_ENTITY_ID, metafields);
        goldServicePlan.setMetaFieldsMap(metafieldMap);
        return api.createPlan(goldServicePlan);
    }

    private Integer createFeePlan() {
        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        bundle2.setQuantity(BigDecimal.ONE);
        bundle2.setAddIfExists(true);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(FEE_PRODUCT_ID);
        pi2.setPrecedence(-1);
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap();
        models.put(Constants.EPOCH_DATE, priceModel);
        pi2.setModels(models);
        pi2.setBundle(bundle2);

        ItemDTOEx planItem = createProduct(BigDecimal.ZERO,  "PLAN - " + Short.toString((short) System.currentTimeMillis()), false);
        planItem.setId(api.createItem(planItem));
        PlanWS goldServicePlan = new PlanWS();
        goldServicePlan.setItemId(planItem.getId());
        goldServicePlan.setDescription("Test Plan - " + Short.toString((short) System.currentTimeMillis()));
        goldServicePlan.setPeriodId(ORDER_PERIOD_MONTHLY);
        goldServicePlan.addPlanItem(pi2);

        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap();
        goldServicePlan.setMetaFieldsMap(metafieldMap);
        return api.createPlan(goldServicePlan);
    }

    private ItemDTOEx createProduct(BigDecimal price, String productNumber, boolean assetsManagementEnabled) {
        ItemDTOEx product = CreateObjectUtil.createItem(
                PRANCING_PONY_ENTITY_ID, price, Constants.PRIMARY_CURRENCY_ID, HARDWARE_PRODUCT_ID,
                productNumber);
        product.setNumber(productNumber);
        product.setDescription(productNumber);
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
        Integer itemTypes[]= new Integer[1];
        itemTypes[0]= new Integer(1);
        product.setTypes(itemTypes);
        product.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_TAX);
        return product;
    }

    private Integer createSimplePlan() {
        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        bundle2.setQuantity(BigDecimal.ONE);
        bundle2.setAddIfExists(true);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(HARDWARE_PRODUCT_ID);
        pi2.setPrecedence(-1);
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap();
        models.put(Constants.EPOCH_DATE, priceModel);
        pi2.setModels(models);
        pi2.setBundle(bundle2);

        ItemDTOEx planItem = createProduct(BigDecimal.ZERO,  "PLAN - " + Short.toString((short) System.currentTimeMillis()), false);
        planItem.setId(api.createItem(planItem));
        PlanWS goldServicePlan = new PlanWS();
        goldServicePlan.setItemId(planItem.getId());
        goldServicePlan.setDescription("Test Plan - " + Short.toString((short) System.currentTimeMillis()));
        goldServicePlan.setPeriodId(ORDER_PERIOD_MONTHLY);
        goldServicePlan.addPlanItem(pi2);

        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap();
        goldServicePlan.setMetaFieldsMap(metafieldMap);
        return api.createPlan(goldServicePlan);
    }

    private Integer createMediationConfiguration(){

        MediationConfigurationWS[] mediationConfigurations = api.getAllMediationConfigurations();
        for(MediationConfigurationWS conf : mediationConfigurations) {
            if(conf.getName().equals(MEDIATION_CONFIGURATION) && conf.getMediationJobLauncher().equals(DISTRIBUTEL_LAUNCHER_JOB)){
                return conf.getId();
            }
        }
        MediationConfigurationWS mediationConfigurationWS = new MediationConfigurationWS();
        mediationConfigurationWS.setCreateDatetime(Calendar.getInstance().getTime());
        mediationConfigurationWS.setEntityId(PRANCING_PONY_ENTITY_ID);
        mediationConfigurationWS.setGlobal(true);
        mediationConfigurationWS.setName(MEDIATION_CONFIGURATION);
        mediationConfigurationWS.setMediationJobLauncher(DISTRIBUTEL_LAUNCHER_JOB);
        mediationConfigurationWS.setOrderValue("1");
        mediationConfigurationWS.setLocalInputDirectory(com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "distributel");
        return api.createMediationConfiguration(mediationConfigurationWS);
    }

    @AfterClass
    public void tearDown() {
        DistributelTestUtil.disablePlugin(updateDistriubtelCustomerTaskId, api);
    }
}
