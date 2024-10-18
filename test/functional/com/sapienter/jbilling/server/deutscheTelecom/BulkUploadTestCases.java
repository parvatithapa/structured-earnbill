package com.sapienter.jbilling.server.deutscheTelecom;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.dt.BulkLoaderUtility;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.EnumerationValueWS;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.DeutscheTelecomAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingDeutscheTelecomAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static java.lang.Thread.sleep;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by wajeeha on 5/11/18.
 */
@Test(groups = { "test-dt-bulkUpload", "dtBulkUpload" }, testName = "BulkUploadTestCases")

public class BulkUploadTestCases {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DT_BULK_UPLOAD_TASK = "com.sapienter.jbilling.server.dt.DTAGBulkLoaderTask";

    private static JbillingAPI api;
    private static JbillingDeutscheTelecomAPI dtApi;

    private static String defaultPricesFilePath;
    private static String accountTypePricesFilePath;
    private static String customerPricesFilePath;
    private static String updatedAccountTypePricesFilePath;
    private static String updatedCustomerPricesFilePath;
    private static String planPriceFilePath;
    private static String updatePlanPriceFilePath;
    private static String updatedAccountTypePricesErrorFilePath;
    private static String updatedCustomerPricesErrorFilePath;
    private static String updatedPlanPricesErrorFilePath;
    private static String updatedDefaultPricesFilePath;
    private static String productWithTieredPriceModel;
    private static String productWithFlatPriceModel;
    private static String plan;
    private static String category;
    private static String customerMetaFieldValue;
    private static Integer customerMetaFieldId;
    private static Integer durationMetafieldId;
    private static Integer paymentOptionMetafieldId;
    private static Integer durationEnumId;
    private static Integer paymentOptionEnumId;
    private static Integer parentUserId;
    private static Integer parentAccountTypeId;
    private static Integer pluginId;

    private String productUploadBatchProcessTestFilesDirectoryPath;
    private String planPricesErrorFilePath;
    private String customerPricesErrorFilePath;
    private String defaultPricesErrorFilePath;
    private String accountTypePricesErrorFilePath;

    private static final int SLEEPPERIOD = 3000;
    private static final int TIMEOUTPERIOD = 50000;

    private static final Integer ENTITY_ID = 1;

    @BeforeClass
    public void initializeTests() throws IOException, JbillingAPIException {

        if (null == api) {
            api = JbillingAPIFactory.getAPI("apiClient2");
            dtApi = DeutscheTelecomAPIFactory.getAPI();
        }

        parentAccountTypeId =  createAccountType();

        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName("externalAccountIdentifier");
        metafieldWS.setEntityType(EntityType.CUSTOMER);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        customerMetaFieldId = api.createMetaField(metafieldWS);

        List<EnumerationValueWS> values = new ArrayList<>();
        values.add(new EnumerationValueWS("12"));
        values.add(new EnumerationValueWS("24"));
        values.add(new EnumerationValueWS("36"));

        EnumerationWS enumerationWS = new EnumerationWS();
        enumerationWS.setEntityId(1);
        enumerationWS.setName("Duration");
        enumerationWS.setValues(values);

        durationEnumId = api.createUpdateEnumeration(enumerationWS);

        values = new ArrayList<>();
        values.add(new EnumerationValueWS("UPFRONT"));
        values.add(new EnumerationValueWS("MONTHLY"));

        enumerationWS = new EnumerationWS();
        enumerationWS.setEntityId(1);
        enumerationWS.setName("Payment Option");
        enumerationWS.setValues(values);

        paymentOptionEnumId = api.createUpdateEnumeration(enumerationWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Duration");
        metafieldWS.setEntityType(EntityType.PLAN);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.ENUMERATION);

        durationMetafieldId = api.createMetaField(metafieldWS);

        metafieldWS = new MetaFieldWS();
        metafieldWS.setName("Payment Option");
        metafieldWS.setEntityType(EntityType.PLAN);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.ENUMERATION);

        paymentOptionMetafieldId = api.createMetaField(metafieldWS);
        parentUserId = createUser(true, null, 1, true, parentAccountTypeId);

        String time= String.valueOf(new Date().getTime());
        productWithFlatPriceModel = "Test-Flat-Product-" + time;
        productWithTieredPriceModel = "Test-Tiered-Product-" + time;
        plan = "Test-Plan-" + time;
        category = "Test-Category-" + time;

        createProductUploadProcessTestFiles();
        pluginId = enableDTAGBulkLoaderPlugin();
    }

    @AfterClass
    public void tearDown(){

        if (null!= parentUserId){
            api.deleteUser(parentUserId);
        }
        if (null!= parentAccountTypeId){
            api.deleteAccountType(parentAccountTypeId);
        }

        if(customerMetaFieldId != null){
            api.deleteMetaField(customerMetaFieldId);
        }

        if(durationMetafieldId != null){
            api.deleteMetaField(durationMetafieldId);
        }

        if(paymentOptionMetafieldId != null){
            api.deleteMetaField(paymentOptionMetafieldId);
        }

        if(durationEnumId != null){
            api.deleteEnumeration(durationEnumId);
        }

        if(paymentOptionEnumId != null){
            api.deleteEnumeration(paymentOptionEnumId);
        }

        if(productWithFlatPriceModel != null) {
            api.deleteItem(api.getItemID(productWithFlatPriceModel));
        }

        if(productWithTieredPriceModel != null) {
            api.deleteItem(api.getItemID(productWithTieredPriceModel));
        }

        if(api.getPlanByInternalNumber(plan,ENTITY_ID) !=null) {
            api.deletePlan(api.getPlanByInternalNumber(plan, ENTITY_ID).getId());
        }

        for (ItemTypeWS itemTypeWS:api.getAllItemCategoriesByEntityId(ENTITY_ID)) {
            if(itemTypeWS.getDescription().equals("ReservedInstance") || itemTypeWS.getDescription().equals(category)){
                api.deleteItemCategory(itemTypeWS.getId());
            }
        }

        for (UsagePoolWS usagePoolWS:api.getAllUsagePools()){
            if(usagePoolWS.getNames().get(0).getContent().equals("testUsagePool")){
                api.deleteUsagePool(usagePoolWS.getId());
                break;
            }
        }

        if(pluginId != null){
            api.deletePlugin(pluginId);
        }
    }

    @Test(timeOut = TIMEOUTPERIOD, priority = 1)
    public void testDefaultPricesUpload() throws InterruptedException {

        dtApi.uploadDefaultPrices(defaultPricesFilePath, defaultPricesErrorFilePath);
        int noOfAttempts = 0;
        ItemDTOEx item = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                item = api.getItem(api.getItemID(productWithFlatPriceModel),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Product should not be null", item);
        assertEquals("Product-Description", item.getDescription());
        assertEquals(true, item.isGlobal());
        assertEquals(Util.getDate(2017, 1, 1),item.getActiveSince());
        assertEquals(Util.getDate(2018, 12, 12),item.getActiveUntil());

        PriceModelWS priceModelWS = item.getDefaultPrices().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.FLAT.name(), priceModelWS.getType());
        assertEquals("Rate",new BigDecimal("10.0000000000"), priceModelWS.getRateAsDecimal());

        ItemTypeWS itemTypeWS = api.getItemCategoryById(item.getTypes()[0]);
        assertEquals(category, itemTypeWS.getDescription());

        noOfAttempts = 0;
        item = null;
        do {
            try {
                noOfAttempts++;
                item = api.getItem(api.getItemID(productWithTieredPriceModel),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Product should not be null", item);
        assertEquals("Product-Description", item.getDescription());
        assertEquals(true, item.isGlobal());
        assertEquals(Util.getDate(2017, 1, 1),item.getActiveSince());
        assertEquals(Util.getDate(2018, 12, 12),item.getActiveUntil());

        itemTypeWS = api.getItemCategoryById(item.getTypes()[0]);
        assertEquals(category, itemTypeWS.getDescription());

        assertEquals(1,item.getTypes().length);

        priceModelWS = item.getDefaultPrices().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.TIERED.name(), priceModelWS.getType());
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(10)));
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("11")).equals(BigDecimal.valueOf(15)));
    }

    @Test(dependsOnMethods = "testDefaultPricesUpload",timeOut = TIMEOUTPERIOD)
    public void testUpdatedDefaultPrices() {

        dtApi.uploadDefaultPrices(updatedDefaultPricesFilePath, defaultPricesErrorFilePath);

        int noOfAttempts = 0;
        ItemDTOEx item = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                item = api.getItem(api.getItemID(productWithFlatPriceModel),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Product should not be null", item);
        assertEquals("Product-Description-updated", item.getDescription());
        assertEquals(false, item.isGlobal());
        assertEquals(Util.getDate(2017, 1, 1),item.getActiveSince());
        assertEquals(Util.getDate(2018, 12, 12),item.getActiveUntil());

        PriceModelWS priceModelWS = item.getDefaultPrices().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.FLAT.name(), priceModelWS.getType());
        assertEquals("Rate",new BigDecimal("20.0000000000"), priceModelWS.getRateAsDecimal());

        dtApi.uploadDefaultPrices(updatedDefaultPricesFilePath, defaultPricesErrorFilePath);
        noOfAttempts = 0;
        item = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                item = api.getItem(api.getItemID(productWithTieredPriceModel),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Product should not be null", item);
        assertEquals("Product-Description-updated", item.getDescription());
        assertEquals(false, item.isGlobal());
        assertEquals(Util.getDate(2017, 1, 1),item.getActiveSince());
        assertEquals(Util.getDate(2018, 12, 12),item.getActiveUntil());

        priceModelWS = item.getDefaultPrices().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.TIERED.name(), priceModelWS.getType());
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(5)));
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("11")).equals(BigDecimal.valueOf(10)));

    }

    @Test(dependsOnMethods = "testDefaultPricesUpload",timeOut = TIMEOUTPERIOD)
    public void testUploadAccountTypePrices() throws InterruptedException {

        dtApi.uploadAccountTypePrices(accountTypePricesFilePath, accountTypePricesErrorFilePath);
        int noOfAttempts = 0;
        PlanItemWS planItemWS = null;

        do {
            try {
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithFlatPriceModel);
                planItemWS = api.getAccountTypePrice(parentAccountTypeId,itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Account Type Price should not be null", planItemWS);

        PriceModelWS priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.FLAT.name(), priceModelWS.getType());
        assertEquals("Rate",new BigDecimal("10.0000000000"), priceModelWS.getRateAsDecimal());

        noOfAttempts = 0;
        planItemWS = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithTieredPriceModel);
                planItemWS = api.getAccountTypePrice(parentAccountTypeId,itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Account Level Price should not be null", planItemWS);

        priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.TIERED.name(), priceModelWS.getType());
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(5)));
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("10")).equals(BigDecimal.valueOf(10)));
    }

    @Test(dependsOnMethods = "testUploadAccountTypePrices",timeOut = TIMEOUTPERIOD)
    public void testUpdatedAccountTypePrices() {

        dtApi.uploadAccountTypePrices(updatedAccountTypePricesFilePath, updatedAccountTypePricesErrorFilePath);
        int noOfAttempts = 0;
        PlanItemWS planItemWS = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithFlatPriceModel);
                planItemWS = api.getAccountTypePrice(parentAccountTypeId,itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Account Level Price should not be null", planItemWS);

        PriceModelWS priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.FLAT.name(), priceModelWS.getType());
        assertEquals("Rate",new BigDecimal("60.0000000000"), priceModelWS.getRateAsDecimal());

        noOfAttempts = 0;
        planItemWS = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithTieredPriceModel);
                planItemWS = api.getAccountTypePrice(parentAccountTypeId,itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Account Level Price should not be null", planItemWS);

        priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.TIERED.name(), priceModelWS.getType());
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(2.5)));
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("10")).equals(BigDecimal.valueOf(5)));

    }

    @Test(dependsOnMethods = "testDefaultPricesUpload",timeOut = TIMEOUTPERIOD)
    public void testUploadCustomerLevelPrices() throws InterruptedException {

        dtApi.uploadCustomerLevelPrices(customerPricesFilePath, customerPricesErrorFilePath);
        int noOfAttempts = 0;
        PlanItemWS planItemWS = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithFlatPriceModel);
                planItemWS = api.getCustomerPrice(parentUserId, itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Customer Level Price should not be null", planItemWS);

        PriceModelWS priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.FLAT.name(), priceModelWS.getType());
        assertEquals("Rate",new BigDecimal("75.0000000000"), priceModelWS.getRateAsDecimal());

        noOfAttempts = 0;
        planItemWS = null;
        do {
            try {
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithTieredPriceModel);
                planItemWS = api.getCustomerPrice(parentUserId,itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Customer Level Price should not be null", planItemWS);

        priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.TIERED.name(), priceModelWS.getType());
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(1.5)));
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("10")).equals(BigDecimal.valueOf(3)));
    }

    @Test(dependsOnMethods = "testUploadCustomerLevelPrices",timeOut = TIMEOUTPERIOD)
    public void testUpdateCustomerLevelPrices() {

        dtApi.uploadCustomerLevelPrices(updatedCustomerPricesFilePath, updatedCustomerPricesErrorFilePath);
        int noOfAttempts = 0;
        PlanItemWS planItemWS = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithFlatPriceModel);
                planItemWS = api.getCustomerPrice(parentUserId, itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Customer Level Price should not be null", planItemWS);

        PriceModelWS priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.FLAT.name(), priceModelWS.getType());
        assertEquals("Rate",new BigDecimal("100.0000000000"), priceModelWS.getRateAsDecimal());

        noOfAttempts = 0;
        planItemWS = null;
        do {
            try {
                noOfAttempts++;
                Integer itemId = api.getItemID(productWithTieredPriceModel);
                planItemWS = api.getCustomerPrice(parentUserId,itemId);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planItemWS == null && noOfAttempts < 3);

        assertNotNull("Customer Level Price should not be null", planItemWS);

        priceModelWS = planItemWS.getModels().get(Util.getDate(2017, 1, 1));
        assertEquals(PriceModelStrategy.TIERED.name(), priceModelWS.getType());
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(2.5)));
        assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("10")).equals(BigDecimal.valueOf(5)));
    }

    @Test (dependsOnMethods = "testDefaultPricesUpload",timeOut = TIMEOUTPERIOD)
    public void testUploadPlanPrices() throws InterruptedException {
        dtApi.uploadPlanPrices(planPriceFilePath,planPricesErrorFilePath);
        int noOfAttempts = 0;
        PlanWS planWS = null;

        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                planWS = api.getPlanByInternalNumber(plan,ENTITY_ID);
                logger.debug("Plan id: " +planWS);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planWS == null && noOfAttempts < 3);

        assertNotNull("Plan not found", planWS);

        List<PlanItemWS> items = planWS.getPlanItems();

        assertNotNull("Plan items not found",items);
        assertEquals(items.size(),2);

        for(PlanItemWS planItemWS: items){
            PriceModelWS priceModelWS = planItemWS.getModels().get(Util.getDate(1970, 1, 1));

            if(PriceModelStrategy.FLAT.name().equals(priceModelWS.getType())){
                assertEquals("Rate",new BigDecimal("75.0000000000"), priceModelWS.getRateAsDecimal());
            }
            else if(PriceModelStrategy.TIERED.name().equals(priceModelWS.getType())) {
                assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(5)));
                assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("5")).equals(BigDecimal.valueOf(10)));
            }
        }

        Integer[] usagePoolIds = planWS.getUsagePoolIds();

        assertEquals(usagePoolIds.length,1);

        UsagePoolWS usagePoolWS = api.getUsagePoolWS(usagePoolIds[0]);
        assertNotNull("Usage pool should not be null",usagePoolWS);
        assertEquals(true, usagePoolWS.getNames().get(0).getContent().equals(new String("testUsagePool")));
    }

    @Test (dependsOnMethods = "testUploadPlanPrices",timeOut = TIMEOUTPERIOD)
    public void testUpdatePlanPrices() throws InterruptedException {
        dtApi.uploadPlanPrices(updatePlanPriceFilePath,updatedPlanPricesErrorFilePath);
        int noOfAttempts = 0;
        PlanWS planWS = null;

        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                planWS = api.getPlanByInternalNumber(plan,ENTITY_ID);
                logger.debug("Plan id: " +planWS);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (planWS == null && noOfAttempts < 3);

        assertNotNull("Plan not found", planWS);

        List<PlanItemWS> items = planWS.getPlanItems();

        assertNotNull("Plan items not found",items);
        assertEquals(items.size(),2);

        for(PlanItemWS planItemWS: items){
            PriceModelWS priceModelWS = planItemWS.getModels().get(Util.getDate(1970, 1, 1));

            if(PriceModelStrategy.FLAT.name().equals(priceModelWS.getType())){
                assertEquals("Rate",new BigDecimal("100.0000000000"), priceModelWS.getRateAsDecimal());
            }
            else if(PriceModelStrategy.TIERED.name().equals(priceModelWS.getType())) {
                assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("0")).equals(BigDecimal.valueOf(15)));
                assertEquals(true, new BigDecimal(priceModelWS.getAttributes().get("5")).equals(BigDecimal.valueOf(30)));
            }
        }

        Integer[] usagePoolIds = planWS.getUsagePoolIds();

        assertEquals(usagePoolIds.length,1);

        UsagePoolWS usagePoolWS = api.getUsagePoolWS(usagePoolIds[0]);

        assertNotNull("Usage pool should not be null",usagePoolWS);
        logger.debug("Usage pool: "+usagePoolWS);
        assertEquals(true, usagePoolWS.getNames().get(0).getContent().equals(new String("testUsagePool")));
    }

    @Test
    public void testCategoryErrorForProduct() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithIncorrectData.csv";
        String defaultPricesFileContent=
                "PROD,TestProductWithError,Product-Description,false,,1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(errorFilePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(defaultPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);
        assertEquals("Invalid number of error lines!", 2, errorLines.size());

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Product Category/ies are required"));
    }

    @Test
    public void testInvalidCurrencyForProduct() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithIncorrectData.csv";
        String defaultPricesFileContent=
                "PROD,TestProductWithError,Product-Description,false,"+category+",1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,,Prancing Pony,false\n" +
                        "FLAT,10";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(errorFilePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(defaultPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);
        assertEquals("Invalid number of error lines!", 2, errorLines.size());

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Invalid Currency Code"));
    }

    @Test
    public void testMissingPriceModelErrorForProduct() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithIncorrectData.csv";
        String defaultPricesFileContent=
                "PROD,TestProductWithError,Product-Description,,Test description,1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(errorFilePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(defaultPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);
        assertEquals("Invalid number of error lines!", 2, errorLines.size());

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Missing Price Model from Product"));
    }

    @Test
    public void testInvalidAvailabilityDateForProduct() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithIncorrectData.csv";
        String defaultPricesFileContent=
                "PROD,TestProductWithError,Product-Description,,Test description,1/1/2019,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(errorFilePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(defaultPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);
        assertEquals("Invalid number of error lines!", 2, errorLines.size());

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Availability End Date should be greater than " +
                "Availability Start Date"));
    }

    @Test
    public void testInvalidFormatForProduct() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithIncorrectData.csv";
        String defaultPricesFileContent=
                "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(errorFilePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(defaultPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("PROD identifier row not available"));
    }

    @Test
    public void testInvalidFormatForAccountTypePrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "AccountTypePricesWithIncorrectData.csv";
        String accountTypePricesFileContent=
                "FLAT,10";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "AccountTypePriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, accountTypePricesFileContent);

        dtApi.uploadAccountTypePrices(errorFilePath,accountTypePricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(accountTypePricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("PRICE identifier row not available"));

        errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "AccountTypePricesWithIncorrectData.csv";
        accountTypePricesFileContent=
                "PRICE,TestProductWithError,"+parentAccountTypeId+",01/01/2017,12/12/2018,USD,false";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "AccountTypePriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, accountTypePricesFileContent);

        dtApi.uploadAccountTypePrices(errorFilePath,accountTypePricesErrorFilePath);

        sleep(SLEEPPERIOD);

        errorLines = CSVFileHelper.readLines(accountTypePricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Missing Price Model from Product"));
    }

    @Test
    public void testInvalidProductForAccountTypePrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "AccountTypePricesWithIncorrectData.csv";
        String accountTypePricesFileContent=
                "PRICE,TestProductWithError"+new Date().getTime()+","+parentAccountTypeId+",01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,10";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "AccountTypePriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, accountTypePricesFileContent);

        dtApi.uploadAccountTypePrices(errorFilePath,accountTypePricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(accountTypePricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(0);
        assertTrue("Invalid error message!!", message.contains("Item doesn't exist with given Product Code OR is not " +
                "available to the calling Company."));
    }

    @Test
    public void testInvalidAccountTypeForAccountTypePrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "AccountTypePricesWithIncorrectData.csv";
        String accountTypePricesFileContent=
                "PRICE,TestProductWithError,,01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,10";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "AccountTypePriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, accountTypePricesFileContent);

        dtApi.uploadAccountTypePrices(errorFilePath,accountTypePricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(accountTypePricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Invalid Account Type ID"));
    }

    @Test
    public void testInvalidCurrencyForAccountTypePrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "AccountTypePricesWithIncorrectData.csv";
        String accountTypePricesFileContent=
                "PRICE,TestProductWithError,"+parentAccountTypeId+",01/01/2017,12/12/2018,,false\n" +
                        "FLAT,10";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "AccountTypePriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, accountTypePricesFileContent);

        dtApi.uploadAccountTypePrices(errorFilePath,accountTypePricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(accountTypePricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Currency Code does not exist for the provided Company"));
    }

    @Test
    public void testInvalidAvailabilityDateForAccountTypePrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "AccountTypePricesWithIncorrectData.csv";
        String accountTypePricesFileContent=
                "PRICE,TestProductWithError,"+parentAccountTypeId+",01/01/2019,12/12/2018,USD,false\n" +
                        "FLAT,10";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "AccountTypePriceErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, accountTypePricesFileContent);

        dtApi.uploadAccountTypePrices(errorFilePath,accountTypePricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(accountTypePricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Availability End Date should be greater than " +
                "Availability Start Date"));
    }

    @Test
    public void testInvalidFormatForCustomerLevelPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesWithIncorrectData.csv";
        String CustomerLevelPricesPricesFileContent=
                "FLAT,10";

        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, CustomerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("PRICE identifier row not available"));

        errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "CustomerLevelPricesWithIncorrectData.csv";
        String customerLevelPricesPricesFileContent=
                "PRICE,TestProductWithError,"+parentUserId+",01/01/2017,12/12/2018,USD,false";

        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, customerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Missing Price Model from Product"));
    }

    @Test
    public void testInvalidProductForCustomerLevelPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesWithIncorrectData.csv";
        String customerLevelPricesPricesFileContent=
                "PRICE,TestProductWithError"+new Date().getTime()+","+customerMetaFieldValue+",01/01/2017,12/12/2018," +
                        "USD,false\n" +
                        "FLAT,10";

        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, customerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(0);
        assertTrue("Invalid error message!!", message.contains("Item doesn't exist with given Product Code OR is not " +
                "available to the calling Company."));
    }

    @Test
    public void testInvalidCustomerIdentifierForCustomerLevelPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesWithIncorrectData.csv";
        String customerLevelPricesPricesFileContent=
                "PRICE,TestProductWithError"+",,01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,10";

        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, customerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Customer Identifier can't be empty"));

        errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "CustomerLevelPricesWithIncorrectData.csv";
        customerLevelPricesPricesFileContent=
                "PRICE,TestProductWithError"+","+new Date().getTime()+",01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,10";

        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, customerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        message = errorLines.get(0);
        assertTrue("Invalid error message!!", message.contains("Customer doesn't exist with the given Identifier"));
    }

    @Test
    public void testInvalidCurrencyForCustomerLevelPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesWithIncorrectData.csv";
        String customerLevelPricesPricesFileContent=
                "PRICE,TestProductWithError,"+customerMetaFieldValue+",01/01/2017,12/12/2018,,false\n" +
                        "FLAT,10";

        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, customerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Currency Code does not exist for the provided Company"));
    }

    @Test
    public void testInvalidAvailabilityDateForCustomerLevelPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesWithIncorrectData.csv";
        String customerLevelPricesPricesFileContent=
                "PRICE,TestProductWithError,"+customerMetaFieldValue+",01/01/2019,12/12/2018,,false\n" +
                        "FLAT,10";

        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "CustomerLevelPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, customerLevelPricesPricesFileContent);

        dtApi.uploadCustomerLevelPrices(errorFilePath,customerPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(customerPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Availability End Date should be greater than " +
                "Availability Start Date"));
    }

    @Test(dependsOnMethods = "testDefaultPricesUpload")
    public void testInvalidPlanFoPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,,Test plan for bulk upload,Month,USD,20,01/01/2017,12/12/2018,ReservedInstance,UPFRONT,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel+"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Invalid Plan Number"));
    }

    @Test
    public void testInvalidProductFoPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,Month,USD,20,01/01/2017,12/12/2018,ReservedInstance," +
                        "UPFRONT,12\n" +
                        "ITEM,30,Month,"+"Test-Product-"+System.currentTimeMillis() +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("No item found for the given Product Code in the " +
                "current Company"));
    }

    @Test(dependsOnMethods = "testUploadPlanPrices")
    public void testInvalidDescriptionFoPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",,Month,USD,20,01/01/2017,1/1/2018,ReservedInstance,UPFRONT,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(0);
        assertTrue("Invalid error message!!", message.contains("Must have a description"));
    }

    @Test(dependsOnMethods = "testUploadPlanPrices")
    public void testInvalidCurrencyFoPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,,20,01/01/2017,1/1/2018,ReservedInstance,UPFRONT,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Currency Code does not exist for the provided Company"));
    }

    @Test(dependsOnMethods = "testUploadPlanPrices")
    public void testInvalidAvailabilityDatePlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,USD,20,01/01/2019,1/1/2018,ReservedInstance,UPFRONT,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Availability End Date should be greater than " +
                "Availability Start Date"));
    }

    @Test(dependsOnMethods = "testUploadPlanPrices")
    public void testInvalidPlanCategoryDateForPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,USD,20,01/01/2017,1/1/2018,,UPFRONT,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Plan Category name needs to be provided"));
    }

    @Test(dependsOnMethods = "testUploadPlanPrices")
    public void testInvalidPaymentOptionForPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,USD,20,01/01/2017,1/1/2018,ReservedInstance,,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Plan Payment Option Meta-Filed can't be empty"));

        errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,USD,20,01/01/2017,1/1/2018,ReservedInstance," +
                        "InvalidPaymentOption,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        message = errorLines.get(0);
        assertTrue("Invalid error message!!", message.contains("No Payment Options enumeration found for the provided " +
                "value: InvalidPaymentOption"));
    }

    @Test(dependsOnMethods = "testUploadPlanPrices")
    public void testInvalidDurationForPlanPrices() throws InterruptedException {
        String errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        String planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,USD,20,01/01/2017,1/1/2018,ReservedInstance,UPFRONT,\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Plan Duration Meta-Filed can't be empty"));

        errorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesWithIncorrectData.csv";
        planPricesPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,,USD,20,01/01/2017,1/1/2018,ReservedInstance," +
                        "InvalidPaymentOption,12\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel +"\n" +
                        "FLAT,75";

        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "PlanPricesErrorFilePath.csv";

        CSVFileHelper.write(errorFilePath, planPricesPricesFileContent);

        dtApi.uploadPlanPrices(errorFilePath,planPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        errorLines = CSVFileHelper.readLines(planPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        message = errorLines.get(0);
        assertTrue("Invalid error message!!", message.contains("No Payment Options enumeration found for the provided "
                + "value: InvalidPaymentOption"));
    }

    @Test
    public void testMultiLanguageDescriptionForProduct() throws InterruptedException {
        String productCode = "Multi language product"+System.currentTimeMillis();
        String filePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithMultiLanguageDescription.csv";
        String defaultPricesFileContent=
                "PROD,"+productCode+",\"2:Portuguese Description,1:English Description\",false,"+category+",1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(filePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(filePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        int noOfAttempts = 0;
        ItemDTOEx item = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                item = api.getItem(api.getItemID(productCode),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Item should not be null", item);
        assertEquals("No of descriptions should be 2",2,item.getDescriptions().size());

        for(InternationalDescriptionWS internationalDescription : item.getDescriptions()) {
            if(internationalDescription.getLanguageId().equals(new Integer(1))) {
                assertEquals("Incorrect Description", "English Description", internationalDescription.getContent());
            }
            else{
                assertEquals("Incorrect Description", "Portuguese Description", internationalDescription.getContent());
            }
        }

        api.deleteItem(item.getId());
    }

    @Test
    public void testMetafieldsForProduct() throws InterruptedException {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName(com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF);
        metafieldWS.setEntityType(EntityType.PRODUCT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        Integer testMetaFieldId1 = api.createMetaField(metafieldWS);

        String productCode = "Product With Metafields"+System.currentTimeMillis();
        String filePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithMetaField.csv";
        String defaultPricesFileContent=
                "PROD,"+productCode+",\"English Description\",false,"+category+",1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10\n"+
                        "META,\"feature1:value,feature2:value\"";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(filePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(filePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        int noOfAttempts = 0;
        ItemDTOEx item = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                item = api.getItem(api.getItemID(productCode),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Item should not be null", item);

        int metafieldCount = 0;

        for(MetaFieldValueWS metaFieldValueWS: item.getMetaFields()){
            if(metaFieldValueWS.getFieldName().equals(com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF) ){
                assertEquals("Incorrect metafield value", "feature1:value,feature2:value", metaFieldValueWS.getStringValue());
                metafieldCount++;
            }
        }

        assertEquals("Metafield not found",1,metafieldCount);
        api.deleteItem(item.getId());
        api.deleteMetaField(testMetaFieldId1);
    }

    @Test
    public void testInvalidMetafieldsForProduct() throws InterruptedException {
        String productCode = "Product With Invalid Metafields"+System.currentTimeMillis();
        String filePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithInvalidMetafield.csv";
        String defaultPricesFileContent=
                "PROD,"+productCode+",\"English Description\",false,"+category+",1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10\n"+
                        "META,\"InvalidMetafield:testValue,\"";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(filePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(filePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        List<String> errorLines = CSVFileHelper.readLines(defaultPricesErrorFilePath);
        assertNotNull("Error lines expected!!", errorLines);

        String message = errorLines.get(1);
        assertTrue("Invalid error message!!", message.contains("Given MetaField: Features is not available for Entity: 1"));
    }

    @Test
    public void testEmptydMetafieldValueForProduct() throws InterruptedException {
        MetaFieldWS metafieldWS = new MetaFieldWS();
        metafieldWS.setName(com.sapienter.jbilling.server.integration.Constants.PRODUCT_FEATURES_MF);
        metafieldWS.setEntityType(EntityType.PRODUCT);
        metafieldWS.setPrimary(true);
        metafieldWS.setDataType(DataType.STRING);

        Integer testMetaFieldId1 = api.createMetaField(metafieldWS);

        String productCode = "Product With Empty Metafields"+System.currentTimeMillis();
        String filePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceWithEmptyMetafieldValue.csv";
        String defaultPricesFileContent=
                "PROD,"+productCode+",\"English Description\",false,"+category+",1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10\n"+
                        "META,\"\"";

        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "DefaultPriceErrorFilePath.csv";

        CSVFileHelper.write(filePath, defaultPricesFileContent);

        dtApi.uploadDefaultPrices(filePath,defaultPricesErrorFilePath);

        sleep(SLEEPPERIOD);

        int noOfAttempts = 0;
        ItemDTOEx item = null;
        do {
            try {
                sleep(SLEEPPERIOD);
                noOfAttempts++;
                item = api.getItem(api.getItemID(productCode),null,null);
            } catch (Exception e) {
                logger.debug("exception while retrieving product");
            }
        } while (item == null && noOfAttempts < 3);

        assertNotNull("Item should not be null", item);

        api.deleteItem(item.getId());
        api.deleteMetaField(testMetaFieldId1);
    }

    private void createProductUploadProcessTestFiles() {

        productUploadBatchProcessTestFilesDirectoryPath = BulkLoaderUtility.createDirectory(Util.getSysProp("base_dir")
                + File.separator + "bulkupload" + File.separator + "test").getPath() + File.separator;
        defaultPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "defaultPricesErrorFile.csv";
        accountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "accountTypePricesErrorFile.csv";
        customerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "customerPricesErrorFile.csv";
        planPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath + "planPricesErrorFile.csv";
        updatedAccountTypePricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "updatedAccountTypePricesErrorFile.csv";
        updatedCustomerPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "updatedCustomerPricesErrorFile.csv";
        updatedPlanPricesErrorFilePath = productUploadBatchProcessTestFilesDirectoryPath
                + "updatedPlanPricesErrorFile.csv";

        String defaultPricesFileContent = null;
        String accountTypePricesFileContent = null;
        String customerPricesFileContent = null;
        String updatedAccountTypePricesFileContent = null;
        String updatedCustomerPricesFileContent = null;
        String planPricesFileContent = null;
        String updatedPlanPricesFileContent = null;

        defaultPricesFilePath = productUploadBatchProcessTestFilesDirectoryPath + "defaultPrices.csv";
        defaultPricesFileContent=

                "PROD,"+productWithFlatPriceModel+",Product-Description,false,"+category+",1/1/2017,12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,false\n" +
                        "FLAT,10\n"+
                        "PROD,"+productWithTieredPriceModel+",Product-Description,false,"+category+",1/1/2017," +
                        "12/12/2018,\n" +
                        "PRICE,1/1/2017,USD,Prancing Pony,true\n" +
                        "TIER,0,10\n" +
                        "TIER,11,15";


        CSVFileHelper.write(defaultPricesFilePath, defaultPricesFileContent);

        updatedDefaultPricesFilePath = productUploadBatchProcessTestFilesDirectoryPath + "updatedDefaultPrices.csv";
        defaultPricesFileContent=
                "PROD,"+productWithFlatPriceModel+",Product-Description-updated,false,"+category+",1/1/2017," +
                        "12/12/2018," + "Prancing Pony\n" +
                        "PRICE,01/01/2017,USD,Prancing Pony,false\n" +
                        "FLAT,20\n" +
                        "PROD,"+productWithTieredPriceModel+",Product-Description-updated,false,"+category+
                        ",1/1/2017," + "12/12/2018,Prancing Pony\n" +
                        "PRICE,01/01/2017,USD,Prancing Pony,false\n" +
                        "TIER,0,5\n" +
                        "TIER,11,10";

        CSVFileHelper.write(updatedDefaultPricesFilePath, defaultPricesFileContent);

        accountTypePricesFilePath = productUploadBatchProcessTestFilesDirectoryPath + "accountTypePrices.csv";
        accountTypePricesFileContent =
                "PRICE,"+productWithFlatPriceModel+","+parentAccountTypeId+",01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,10\n"+
                        "PRICE,"+productWithTieredPriceModel+","+parentAccountTypeId+",01/01/2017,12/12/2018,USD," +
                        "false\n" +
                        "TIER,0,5\n" +
                        "TIER,10,10";

        CSVFileHelper.write(accountTypePricesFilePath, accountTypePricesFileContent);

        customerPricesFilePath = productUploadBatchProcessTestFilesDirectoryPath + "customerPrices.csv";
        customerPricesFileContent =
                "PRICE,"+productWithFlatPriceModel+","+customerMetaFieldValue+",01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,75\n"+
                        "PRICE,"+productWithTieredPriceModel+","+customerMetaFieldValue+",01/01/2017,12/12/2018," +
                        "USD,false\n" +
                        "TIER,0,1.5\n"+
                        "TIER,10,3";

        CSVFileHelper.write(customerPricesFilePath, customerPricesFileContent);

        updatedAccountTypePricesFilePath = productUploadBatchProcessTestFilesDirectoryPath +
                "updatedAccountTypePrices.csv";
        updatedAccountTypePricesFileContent =
                "PRICE,"+productWithFlatPriceModel+","+parentAccountTypeId+",01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,60\n"+
                        "PRICE,"+productWithTieredPriceModel+","+parentAccountTypeId+",01/01/2017,12/12/2018,USD,false\n" +
                        "TIER,0,2.5\n" +
                        "TIER,10,5";

        CSVFileHelper.write(updatedAccountTypePricesFilePath, updatedAccountTypePricesFileContent);

        updatedCustomerPricesFilePath = productUploadBatchProcessTestFilesDirectoryPath + "updatedCustomerPrices.csv";
        updatedCustomerPricesFileContent =
                "PRICE,"+productWithFlatPriceModel+","+customerMetaFieldValue+",01/01/2017,12/12/2018,USD,false\n" +
                        "FLAT,100\n"+
                        "PRICE,"+productWithTieredPriceModel+","+customerMetaFieldValue+",01/01/2017,12/12/2018,USD," +
                        "false\n" +
                        "TIER,0,2.5\n" +
                        "TIER,10,5";

        CSVFileHelper.write(updatedCustomerPricesFilePath, updatedCustomerPricesFileContent);

        planPriceFilePath = productUploadBatchProcessTestFilesDirectoryPath+"planPrices.csv";
        planPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,Month,USD,20,01/01/2017,12/12/2018,ReservedInstance,UPFRONT,12\n" +
                        "FUP,testUsagePool\n" +
                        "ITEM,30,Month,"+productWithTieredPriceModel+"\n" +
                        "TIER,0,5\n" +
                        "TIER,5,10\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel+"\n" +
                        "FLAT,75";

        CSVFileHelper.write(planPriceFilePath,planPricesFileContent);

        updatePlanPriceFilePath = productUploadBatchProcessTestFilesDirectoryPath+"updatedPlanPrices.csv";
        updatedPlanPricesFileContent=
                "PLAN,"+plan+",Test plan for bulk upload,Month,USD,20,01/01/2017,12/12/2018,ReservedInstance,UPFRONT,12\n" +
                        "FUP,testUsagePool\n" +
                        "ITEM,30,Month,"+productWithTieredPriceModel+"\n" +
                        "TIER,0,15\n" +
                        "TIER,5,30\n" +
                        "ITEM,30,Month,"+productWithFlatPriceModel+"\n" +
                        "FLAT,100";

        CSVFileHelper.write(updatePlanPriceFilePath,updatedPlanPricesFileContent);
    }

    private Integer createUser(boolean setPassword, Integer parentId,
                               Integer currencyId, boolean doCreate,
                               Integer accountTypeId) throws JbillingAPIException, IOException {
        UserWS newUser = new UserWS();
        newUser.setUserId(0);
        newUser.setUserName("DT-testUserName-"
                + Calendar.getInstance().getTimeInMillis());
        if (setPassword) {
            newUser.setPassword("P@ssword1");
        }
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(accountTypeId);
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(false);

        customerMetaFieldValue = String.valueOf(Calendar.getInstance().getTimeInMillis());

        List<MetaFieldValueWS> metaFieldValueList = new ArrayList<>();

        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
        metaFieldValueWS.setFieldName(Constants.DeutscheTelekom.EXTERNAL_ACCOUNT_IDENTIFIER);
        metaFieldValueWS.setStringValue(String.valueOf(customerMetaFieldValue));
        metaFieldValueList.add(metaFieldValueWS);


        MetaFieldValueWS[] updatedMetaFieldValueWSArray = new MetaFieldValueWS[metaFieldValueList.size()];
        metaFieldValueList.toArray(updatedMetaFieldValueWSArray);

        newUser.setMetaFields(updatedMetaFieldValueWSArray);

        if (parentId != null) {
            UserWS parent = api.getUserWS(parentId);
            MainSubscriptionWS parentSubscription = parent.getMainSubscription();
            newUser.setMainSubscription(
                    new MainSubscriptionWS(parentSubscription.getPeriodId(), parentSubscription.getNextInvoiceDayOfPeriod()));
            newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
        }

        if (doCreate) {
            logger.debug("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
            if (parentId != null) {
                UserWS parent = api.getUserWS(parentId);
                newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
                api.updateUser(newUser);
                newUser = api.getUserWS(newUser.getId());
            }
            newUser.setPassword(null);

        }
        logger.debug("User created with id:" + newUser.getUserId());
        return newUser.getUserId();
    }

    private Integer createAccountType() {
        AccountTypeWS accountType = new AccountTypeWS();
        accountType.setCreditLimit(new BigDecimal(0));
        accountType.setCurrencyId(new Integer(1));
        accountType.setInvoiceDeliveryMethodId(1);
        accountType.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        accountType.setMainSubscription(new MainSubscriptionWS(
                Constants.PERIOD_UNIT_MONTH, 1));
        accountType.setCreditNotificationLimit1("0");
        accountType.setCreditNotificationLimit2("0");
        accountType.setCreditLimit("0");

        accountType.setName("Test account type_" + System.currentTimeMillis(),
                Constants.LANGUAGE_ENGLISH_ID);

        return api.createAccountType(accountType);
    }

    private Integer enableDTAGBulkLoaderPlugin() {
        PluggableTaskWS plugin = new PluggableTaskWS();
        plugin.setProcessingOrder(160);
        PluggableTaskTypeWS pluggableTaskTypeWS = api.getPluginTypeWSByClassName(
                DT_BULK_UPLOAD_TASK);
        plugin.setTypeId(pluggableTaskTypeWS.getId());
        return api.createPlugin(plugin);
    }

}