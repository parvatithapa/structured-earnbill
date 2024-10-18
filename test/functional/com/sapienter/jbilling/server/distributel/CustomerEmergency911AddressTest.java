package com.sapienter.jbilling.server.distributel;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.spa.AddressType;
import com.sapienter.jbilling.server.spa.CustomerEmergency911AddressWS;
import com.sapienter.jbilling.server.spa.SpaAddressWS;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.DistributelAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingDistributelAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@Test(groups = { "test-distributel", "distributel" }, testName = "CustomerEmergency911AddressTest")
public class CustomerEmergency911AddressTest {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static JbillingDistributelAPI distributelApi;
    private EnvironmentHelper envHelper;
    private TestBuilder testBuilder;
    private static final String  ASSET_ENABLED_USAGE_CATEGORY                   = "Dist Asset Enabled Category";
    private static final String  TEST_ITEM                                      = "Test Product";
    private static final String  TEST_USER_1                                    = "Test-User-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                    = "Test-User-"+ UUID.randomUUID().toString();
    private static final String  ASSET_CODE_1                                   = "Test-Asset-"+ UUID.randomUUID().toString();
    private static final String  ASSET_CODE_2                                   = "Test-Asset-"+ UUID.randomUUID().toString();
    private static final String  ASSET_NUM_1                                    = "1234567890";
    private static final String  ASSET_NUM_2                                    = "1234567891";
    private static final String  TEST_ORDER_1                                   = "Test-Order-"+ UUID.randomUUID().toString();
    private static final String  TEST_ORDER_2                                   = "Test-Order-"+ UUID.randomUUID().toString();
    private static final int     MONTHLY_ORDER_PERIOD                           =  2;
    private static final int     NEXT_INVOICE_DAY                               =  1;
    private static final int     ORDER_CHANGE_STATUS_APPLY_ID                   =  3;
    private static Integer EMERGENCY_ADDRESS_GROUP_ID;
    private static Integer CONTACT_ADDRESS_GROUP_ID;

    @BeforeClass
    public void initializeSPAImportEnrollment() throws Exception {

        testBuilder = getTestEnvironment();
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        distributelApi = DistributelAPIFactory.getAPI();
        DistributelTestUtil.buildAccountType(api);
        EMERGENCY_ADDRESS_GROUP_ID = DistributelTestUtil.emergencyAddressMetaFieldGroupId;
        CONTACT_ADDRESS_GROUP_ID = DistributelTestUtil.contactInformationMetaFieldGroupId;
        testBuilder.given(envBuilder -> {
            logger.debug("EMERGENCY_ADDRESS_GROUP_ID:::{}", EMERGENCY_ADDRESS_GROUP_ID);
            logger.debug("CONTACT_ADDRESS_GROUP_ID:::{}", CONTACT_ADDRESS_GROUP_ID);

            //Creating asset enabled  category
            buildAndPersistCategory(envBuilder, api, ASSET_ENABLED_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            ItemTypeWS type = api.getItemCategoryById(envBuilder.idForCode(ASSET_ENABLED_USAGE_CATEGORY));
            // Creating Meta Field Phone Number
            MetaFieldWS phoneNumber  = buildAndPersistMetafield(testBuilder, SpaConstants.MF_PHONE_NUMBER, DataType.STRING, EntityType.ASSET);
            type.getAssetMetaFields().add(phoneNumber);
            api.updateItemCategory(type);

            // Creating Product
            buildAndPersistFlatProduct(envBuilder, api, TEST_ITEM, false, envBuilder.idForCode(ASSET_ENABLED_USAGE_CATEGORY), "10.00", true);

            //Creating Assets for product
            buildAndPersistAsset(envBuilder, api, ASSET_CODE_1, false,
                    envBuilder.idForCode(TEST_ITEM), envBuilder.idForCode(ASSET_ENABLED_USAGE_CATEGORY), ASSET_NUM_1);

            buildAndPersistAsset(envBuilder, api, ASSET_CODE_2, false,
                    envBuilder.idForCode(TEST_ITEM), envBuilder.idForCode(ASSET_ENABLED_USAGE_CATEGORY), ASSET_NUM_2);


        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Category Creation Failed", testEnvBuilder.idForCode(ASSET_ENABLED_USAGE_CATEGORY));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(TEST_ITEM));
            assertNotNull("Asset Creation Failed", testEnvBuilder.idForCode(ASSET_CODE_1));
            assertNotNull("Asset Creation Failed", testEnvBuilder.idForCode(ASSET_CODE_2));
        });
    }

    @Test
    public void test01GetCustomerEmergency911AddressByPhoneNumber() throws Exception {
        testBuilder.given(envBuilder -> {
            Calendar nextInvoiceDate = Calendar.getInstance();
            nextInvoiceDate.set(Calendar.YEAR, 2018);
            nextInvoiceDate.set(Calendar.MONTH, 0);
            nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

            Calendar activeSince = Calendar.getInstance();
            activeSince.set(Calendar.YEAR, 2018);
            activeSince.set(Calendar.MONTH, 0);
            activeSince.set(Calendar.DAY_OF_MONTH, 1);

            Integer assetId = envBuilder.idForCode(ASSET_CODE_1);
            Integer productId = envBuilder.idForCode(TEST_ITEM);
            Map<Integer, Integer> productAssetMap = new HashMap<>();
            productAssetMap.put(productId, assetId);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(productId, BigDecimal.ONE);

            InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
            scenario01.createUser(TEST_USER_1,DistributelTestUtil.ACCOUNT_TYPE_ID,nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
            //Creating subscription order on 1st ofJan 2018
            .createOrder(TEST_ORDER_1, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_PRE_PAID,
                    ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);

        }).validate((testEnv, envBuilder) -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = api.getUserWS(envBuilder.idForCode(TEST_USER_1));
            assertNotNull("User is not null:", user);

            List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
            SpaAddressWS contactAddress = new SpaAddressWS();
            contactAddress.setAddressType(AddressType.BILLING.name());
            contactAddress.setPostalCode("123456");
            contactAddress.setCity("NewYork");
            contactAddress.setStreetNumber("10");
            contactAddress.setStreetName("MG Road");
            contactAddress.setStreetType("Road");
            contactAddress.setStreetDirecton("Abc");
            contactAddress.setStreetAptSuite("1");
            contactAddress.setProvince("NH");
            setContactInformationAITMetaFields(CONTACT_ADDRESS_GROUP_ID, metaFieldValues,contactAddress,true);
            user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
            api.updateUser(user);

            SpaAddressWS emergencyAddress = new SpaAddressWS();
            emergencyAddress.setAddressType(AddressType.BILLING.name());
            emergencyAddress.setPostalCode("411030");
            emergencyAddress.setCity("London");
            emergencyAddress.setStreetNumber("10");
            emergencyAddress.setStreetName("XX Road");
            emergencyAddress.setStreetType("Road");
            emergencyAddress.setStreetDirecton("Abc");
            emergencyAddress.setStreetAptSuite("1");
            emergencyAddress.setProvince("NH");
            addMetaFieldAddressInformation(metaFieldValues, EMERGENCY_ADDRESS_GROUP_ID,emergencyAddress,true);

            user = api.getUserWS(envBuilder.idForCode(TEST_USER_1));
            user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
            api.updateUser(user);

            logger.debug("Scenario #3 and Scenario #4: Test With Valid VOIP Phone Number and “provided” checkbox of Emergency 911 address is checked"
                    + "Test With Valid VOIP Phone Number");
            Integer assetId = envBuilder.idForCode(ASSET_CODE_1);
            AssetWS asset = api.getAsset(assetId);
            String identifier = ASSET_NUM_1;
            CustomerEmergency911AddressWS ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber(identifier);

            assertEquals("Address is correct.", "1,10,XX Road,Road,Abc,London,NH,411030", ws.getAddress());
            assertEquals("Customer name is correct.", "Test-Customer-1", ws.getCustomerName());
            assertEquals("Phone number is correct.", "1234567890", ws.getPhoneNumber());
            assertEquals("Address type is correct.", "911", ws.getAddressType());
            assertEquals("Effective date is correct.", parseDate(new Date()), ws.getEffectiveDate());
            assertEquals("Return code is correct.", new Integer(1), ws.getReturnCode());

            logger.debug("Scenario #1: Test With VOIP Phone Number which contain dashed and characters");
            try {
                ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber(identifier.replace(identifier.charAt(3), ' '));
            } catch (SessionInternalError e) {
                assertTrue(e.getMessage().contains("validation.error.not.a.number.10.integer"));
            }

            try {
                ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber(identifier.replace(identifier.charAt(3), '-'));
            } catch (SessionInternalError e) {
                assertTrue(e.getMessage().contains("validation.error.not.a.number.10.integer"));
            }

            logger.debug("Scenario #2: Test With Valid VOIP Phone Number and provided checkbox of Emergency 911 address is unchecked");
            addMetaFieldAddressInformation(metaFieldValues, EMERGENCY_ADDRESS_GROUP_ID,emergencyAddress,false);
            user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
            api.updateUser(user);

            identifier = ASSET_NUM_1;
            ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber(identifier);
            assertEquals("Address is correct.", "1,10,MG Road,Road,Abc,NewYork,NH,123456", ws.getAddress());
            assertEquals("Customer name is correct.", "Test-Customer-1", ws.getCustomerName());
            assertEquals("Phone number is correct.", "1234567890", ws.getPhoneNumber());
            assertEquals("Address type is correct.", "Contact Address", ws.getAddressType());
            assertEquals("Effective date is correct.", parseDate(new Date()), ws.getEffectiveDate());
            assertEquals("Return code is correct.", new Integer(1), ws.getReturnCode());

            logger.debug("Phone number not found for given asset identifier");
            ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber("1213141516");
            assertEquals("Address is correct.", null, ws.getAddress());
            assertEquals("Customer name is correct.", null, ws.getCustomerName());
            assertEquals("Phone number is correct.", null, ws.getPhoneNumber());
            assertEquals("Address type is correct.", null, ws.getAddressType());
            assertEquals("Effective date is correct.", null, ws.getEffectiveDate());
            assertEquals("Return code is correct.", new Integer(0), ws.getReturnCode());
            assertEquals("Return message is correct.", "Phone Number Not found!", ws.getReturnMessage());
        });
    }


    @Test
    public void test02GetCustomerEmergency911AddressByPhoneNumber()throws Exception {

            testBuilder.given(envBuilder -> {
                Calendar nextInvoiceDate = Calendar.getInstance();
                nextInvoiceDate.set(Calendar.YEAR, 2018);
                nextInvoiceDate.set(Calendar.MONTH, 0);
                nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

                Calendar activeSince = Calendar.getInstance();
                activeSince.set(Calendar.YEAR, 2018);
                activeSince.set(Calendar.MONTH, 0);
                activeSince.set(Calendar.DAY_OF_MONTH, 1);

                Integer assetId = envBuilder.idForCode(ASSET_CODE_2);
                Integer productId = envBuilder.idForCode(TEST_ITEM);
                Map<Integer, Integer> productAssetMap = new HashMap<>();
                productAssetMap.put(productId, assetId);
                Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
                productQuantityMap.put(productId, BigDecimal.ONE);

                InvoiceSummaryScenarioBuilder scenario01 = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenario01.createUser(TEST_USER_2,DistributelTestUtil.ACCOUNT_TYPE_ID,nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY)
                //Creating subscription order on 1st ofJan 2018
                .createOrder(TEST_ORDER_2, activeSince.getTime(),null, MONTHLY_ORDER_PERIOD, Constants.ORDER_BILLING_PRE_PAID,
                        ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);

            }).validate((testEnv, envBuilder) -> {
                logger.debug("Scenario #5: Test With AIT timelines");
                JbillingAPI api = envBuilder.getPrancingPonyApi();
                UserWS user = api.getUserWS(envBuilder.idForCode(TEST_USER_2));
                assertNotNull("User is not null:", user);

                ArrayList<MetaFieldValueWS> metaFieldValues = new ArrayList<>();

                SpaAddressWS contactAddress = new SpaAddressWS();
                contactAddress.setAddressType(AddressType.BILLING.name());
                contactAddress.setPostalCode("123456");
                contactAddress.setCity("NewYork");
                contactAddress.setStreetNumber("10");
                contactAddress.setStreetName("MG Road");
                contactAddress.setStreetType("Road");
                contactAddress.setStreetDirecton("Abc");
                contactAddress.setStreetAptSuite("1");
                contactAddress.setProvince("NH");
                setContactInformationAITMetaFields(CONTACT_ADDRESS_GROUP_ID, metaFieldValues,contactAddress,true);
                user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
                api.updateUser(user);

                Date today = new Date();
                final Date past = Constants.EPOCH_DATE;
                final Date current = DistributelTestUtil.getDate(getMonth(addMonths(today, -3)),01,getYear(addMonths(today, -3)));
                final Date future = DistributelTestUtil.getDate(getMonth(addMonths(today, 3)),01,getYear(addMonths(today, 3)));

                ArrayList<Date> effectiveDateList = new ArrayList<>();
                effectiveDateList.add(past);
                effectiveDateList.add(future);
                Map<Integer,ArrayList<Date>> timelineDatesMap = new HashMap<>();
                timelineDatesMap.put(EMERGENCY_ADDRESS_GROUP_ID, effectiveDateList);
                Map<Integer,Date> effectivedate = new HashMap<>();
                effectivedate.put(EMERGENCY_ADDRESS_GROUP_ID, past);

                SpaAddressWS emergencyAddress = new SpaAddressWS();
                emergencyAddress.setAddressType(AddressType.BILLING.name());
                emergencyAddress.setPostalCode("411333");
                emergencyAddress.setCity("PalmBeach");
                emergencyAddress.setStreetNumber("10");
                emergencyAddress.setStreetName("XYZ Road");
                emergencyAddress.setStreetType("Road");
                emergencyAddress.setStreetDirecton("Abc");
                emergencyAddress.setStreetAptSuite("1");
                emergencyAddress.setProvince("NH");
                addMetaFieldAddressInformation(metaFieldValues, EMERGENCY_ADDRESS_GROUP_ID,emergencyAddress,true);

                user = api.getUserWS(envBuilder.idForCode(TEST_USER_2));
                user.setTimelineDatesMap(timelineDatesMap);
                user.setEffectiveDateMap(effectivedate);
                user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
                api.updateUser(user);

                String phoneNumber = ASSET_NUM_2;
                CustomerEmergency911AddressWS ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber(phoneNumber);
                assertEquals("Address is correct.", "1,10,XYZ Road,Road,Abc,PalmBeach,NH,411333", ws.getAddress());
                assertEquals("Customer name is correct.", "Test-Customer-1", ws.getCustomerName());
                assertEquals("Phone number is correct.", "1234567890", ws.getPhoneNumber());
                assertEquals("Address type is correct.", "911", ws.getAddressType());
                assertEquals("Effective date is correct.", parseDate(today), ws.getEffectiveDate());
                assertEquals("Return code is correct.", new Integer(1), ws.getReturnCode());

                logger.debug("Scenario #6: Test With AIT timeline PAST and FUTURE");
                user = api.getUserWS(envBuilder.idForCode(TEST_USER_2));
                effectiveDateList = new ArrayList<>();
                effectiveDateList.add(past);
                effectiveDateList.add(current);
                effectiveDateList.add(future);

                timelineDatesMap = new HashMap<>();
                timelineDatesMap.put(EMERGENCY_ADDRESS_GROUP_ID, effectiveDateList);

                effectivedate = new HashMap<>();
                effectivedate.put(EMERGENCY_ADDRESS_GROUP_ID, current);
                addMetaFieldAddressInformation(metaFieldValues, EMERGENCY_ADDRESS_GROUP_ID,emergencyAddress,true);
                user.setTimelineDatesMap(timelineDatesMap);
                user.setEffectiveDateMap(effectivedate);
                user.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
                api.updateUser(user);

                phoneNumber = ASSET_NUM_2;
                ws = distributelApi.getCustomerEmergency911AddressByPhoneNumber(phoneNumber);
                assertEquals("Address is correct.", "1,10,XYZ Road,Road,Abc,PalmBeach,NH,411333", ws.getAddress());
                assertEquals("Customer name is correct.", "Test-Customer-1", ws.getCustomerName());
                assertEquals("Phone number is correct.", "1234567890", ws.getPhoneNumber());
                assertEquals("Address type is correct.", "911", ws.getAddressType());
                assertEquals("Effective date is correct.", parseDate(current), ws.getEffectiveDate());
                assertEquals("Return code is correct.", new Integer(1), ws.getReturnCode());
            });

    }

    @AfterClass
    public void tearDown() {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }

    public Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .allowAssetManagement(1)
                .global(global)
                .build();
    }

    public Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String flatPrice, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(flatPrice)
                .withAssetManagementEnabled(1)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    public Integer buildAndPersistAsset(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global,
            Integer productId, Integer typeId, String identifier) {
        ItemTypeWS itemTypeWS = api.getItemCategoryById(typeId);
        Integer assetStatusId = itemTypeWS.getAssetStatuses().stream().
                filter(assetStatusDTOEx -> assetStatusDTOEx.getIsAvailable() == 1 && assetStatusDTOEx.getDescription()
                        .equals("Available")).collect(Collectors.toList()).get(0).getId();

        MetaFieldValueWS value = new MetaFieldValueWS(SpaConstants.MF_PHONE_NUMBER, null, DataType.STRING, false, identifier);

        return envBuilder.assetBuilder(api)
                .withItemId(productId)
                .withAssetStatusId(assetStatusId)
                .global(global)
                .withCode(code)
                .withMetafields(Arrays.asList(value))
                .withIdentifier(identifier)
                .build();

    }

    private void setContactInformationAITMetaFields(Integer contactInformationAITid, List<MetaFieldValueWS> metaFieldValues,SpaAddressWS address,boolean provided) {
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CUSTOMER_NAME, contactInformationAITid, DataType.STRING, false, "Test-Customer-1"));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CUSTOMER_COMPANY, contactInformationAITid, DataType.STRING, false, "1"));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PHONE_NUMBER_1, contactInformationAITid, DataType.STRING, false, "1234567890"));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.EMAIL_ADDRESS, contactInformationAITid, DataType.STRING, false, "xyz@gmail.com"));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PROVINCE, contactInformationAITid, DataType.STRING, false, address.getProvince()));
        addMetaFieldAddressInformation(metaFieldValues, contactInformationAITid,address,provided);
    }

    private void addMetaFieldAddressInformation(List<MetaFieldValueWS> metaFieldValues, Integer groupId, SpaAddressWS address,boolean provided){
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.POSTAL_CODE, groupId, DataType.STRING, false, address.getPostalCode()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PROVINCE, groupId, DataType.STRING, false, address.getProvince()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.CITY, groupId, DataType.STRING, false, address.getCity()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_NUMBER, groupId, DataType.STRING, false, address.getStreetNumber()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_NAME, groupId, DataType.STRING, false, address.getStreetName()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_TYPE, groupId, DataType.STRING, false, address.getStreetType()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_STREET_DIRECTION, groupId, DataType.STRING, false, address.getStreetDirecton()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_APT_SUITE, groupId, DataType.STRING, false, address.getStreetAptSuite()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.PROVINCE, groupId, DataType.STRING, false, address.getProvince()));
        metaFieldValues.add(new MetaFieldValueWS(SpaConstants.MF_PROVIDED, groupId, DataType.BOOLEAN, false, provided));
    }

    private MetaFieldWS buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        return new MetaFieldBuilder()
                                .name(name)
                                .dataType(dataType)
                                .entityType(entityType)
                                .primary(true)
                                .build();

    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi()));
    }

    private String parseDate(Date date) {
        if(date == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        return sdf.format(date);
    }

    private static Date addMonths(Date inputDate, int months) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        cal.add(Calendar.MONTH, months);
        return cal.getTime();
    }
    private static Integer getYear(Date inputDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.YEAR));
    }

    private static Integer getMonth(Date inputDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.MONTH));
    }
}
