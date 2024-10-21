package com.sapienter.jbilling.server.distributel;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.item.AssetAssignmentWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanBL;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.spa.AddressType;
import com.sapienter.jbilling.server.spa.SpaAddressWS;
import com.sapienter.jbilling.server.spa.SpaCommonFields;
import com.sapienter.jbilling.server.spa.SpaConstants;
import com.sapienter.jbilling.server.spa.SpaHappyFox;
import com.sapienter.jbilling.server.spa.SpaImportWS;
import com.sapienter.jbilling.server.spa.SpaPaymentCredentialWS;
import com.sapienter.jbilling.server.spa.SpaPaymentResultWS;
import com.sapienter.jbilling.server.spa.SpaProductsOrderedWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.DistributelAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingDistributelAPI;

@Test(groups = { "test-distributel", "distributel" }, testName = "CustomerTicketInfoResultTest")
public class CustomerTicketInfoResultTest {

    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static final String BANFF_ACCOUNT_ID = "1111AAAA";

    private static Integer ORDER_PERIOD_MONTHLY;
    private static JbillingAPI api;
    private static JbillingDistributelAPI distributelApi;
    private static Integer PRANCING_PONY_HARDWARE_CATEGORY_ID;
    private static Integer PRANCING_PONY_HARDWARE_TYPE_CATEGORY_ID;
    private static Integer PRANCING_PONY_FEE_CATEGORY_ID;
    private static String PROVINCE_QC = "QC" + System.currentTimeMillis();
    private static Integer HARDWARE_PRODUCT_ID;
    private static Integer FEE_PRODUCT_ID;
    private static Integer MODEM_PLAN_ID;
    private static Integer SIMPLE_PLAN_ID;

    private static StringBuffer CSV_PROCESS_CENTERS = new StringBuffer();

    private static StringBuffer CSV_PLAN_OPTIONAL = new StringBuffer();
    private static StringBuffer CSV_PLAN_SUPPORTED_MODEMS = new StringBuffer();
    private static StringBuffer CSV_CANADIAN_TAXES = new StringBuffer();
    private static Integer PRANCING_PONY_CATEGORY_ID;

    private static String PROVINCE_METAFIELD_VALUE = "PROVINCE TEST";
    private static String USER_TYPE_10_PUBLIC = "10: USER_TYPE PUBLIC TEST";
    private static String USER_TYPE_30_CSR = "30: USER_TYPE CSR TEST";

    private static String DSL = "DSL";
    private static String POSTAL_CODE = "postalCode" ;
    private static String SPA_HAPPY_FOX_MSG = "spaHappyFox should not be null" ;
    private static String SPA_COMMON_FIELD_MSG = "Common Filed should not be null" ;
    private static String SPA_PRIVATE_NOTE_NULL_MSG = "Private note should be null" ;
    private static String SPA_PRIVATE_NOTE_MSG = "Private note should not be null" ;
    private ArrayList<Integer> TEST_PLANS = new ArrayList<>();
    private List<Integer> idRoutes=new ArrayList<>();

    @BeforeClass
    public void initializeSPAImportEnrollment() throws Exception {
        api = JbillingAPIFactory.getAPI();
        distributelApi = DistributelAPIFactory.getAPI();
        PRANCING_PONY_CATEGORY_ID = DistributelTestUtil.createItemCategory(Constants.ORDER_LINE_TYPE_ITEM);
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        CSV_PLAN_OPTIONAL.append("plan_id,optional_plan_id\n")
                .append("%1$2S,%2$2S\n")
                .append("%1$2S,%3$2S\n")
                .append("%2$2S,%1$2S\n")
                .append("%2$2S,%3$2S\n")
                .append("%3$2S,%1$2S\n")
                .append("%3$2S,%2$2S\n");

        CSV_PLAN_SUPPORTED_MODEMS.append("plan_id,information_detail\n")
                .append("%1$2S,Detail 1\n")
                .append("%1$2S,Detail 2\n")
                .append("%1$2S,Detail 3\n")
                .append("%2$2S,Detail 1\n")
                .append("%2$2S,Detail 2\n")
                .append("%2$2S,Detail 3\n");

        CSV_CANADIAN_TAXES.append("province,GST,PST,HST,date,GST Reg Eng,GST Reg Fr,PST Reg Eng,PST Reg Fr,HST Reg Eng,HST Reg Fr\n")
                .append("ON,0,0,12,2014-03-12,GST (8564 02276 RT0001),TPS (8564 02276 RT0001),PST,TVP,N/A,N/A\n")
                .append("ON,0,0,15,2014-03-15,GST (8564 02276 RT0001),TPS (8564 02276 RT0001),PST,TVP,N/A,N/A\n")
                .append("ON,0,0,13,2017-03-12,GST (8564 02276 RT0001),TPS (8564 02276 RT0001),PST,TVP,N/A,N/A\n")
                .append("ON,0,0,22,2017-06-12,GST (8564 02276 RT0001),TPS (8564 02276 RT0001),PST,TVP,N/A,N/A\n")
                .append("ON,0,0,20,2018-03-12,GST (8564 02276 RT0001),TPS (8564 02276 RT0001),PST,TVP,N/A,N/A\n")
                .append("QC,0,0,15,2017-03-12,GST (8564 02276 RT0001),TPS (8564 02276 RT0001),PST,TVP,N/A,N/A\n");

        DistributelTestUtil.createPlanMetaField(SpaConstants.PROVINCE, EntityType.PLAN, DataType.STRING, 1);
        DistributelTestUtil.createPlanMetaField(SpaConstants.USER_TYPE, EntityType.PLAN, DataType.STRING, 2);

        MetaFieldValueWS provinceMetaFieldValue = DistributelTestUtil.createMetaFieldValue(SpaConstants.PROVINCE, DataType.STRING, PROVINCE_METAFIELD_VALUE, PRANCING_PONY_ENTITY_ID);
        MetaFieldValueWS userType10PublicMetaFieldValue = DistributelTestUtil.createMetaFieldValue(SpaConstants.USER_TYPE, DataType.STRING, USER_TYPE_10_PUBLIC, PRANCING_PONY_ENTITY_ID);
        MetaFieldValueWS userType30PublicMetaFieldValue = DistributelTestUtil.createMetaFieldValue(SpaConstants.USER_TYPE, DataType.STRING, USER_TYPE_30_CSR, PRANCING_PONY_ENTITY_ID);

        TEST_PLANS.add(createPlan(new MetaFieldValueWS[]{provinceMetaFieldValue}));
        TEST_PLANS.add(createPlan(new MetaFieldValueWS[]{userType10PublicMetaFieldValue}));
        TEST_PLANS.add(createPlan(new MetaFieldValueWS[]{provinceMetaFieldValue, userType10PublicMetaFieldValue}));
        TEST_PLANS.add(createPlan(new MetaFieldValueWS[]{provinceMetaFieldValue, userType30PublicMetaFieldValue}));

        createEnumeration(SpaConstants.USER_TYPE, USER_TYPE_10_PUBLIC, USER_TYPE_30_CSR);

        idRoutes.add(DistributelTestUtil.createRoute(PlanBL.PLAN_SUPPORTED_MODEMS_TABLE, PlanBL.PLAN_SUPPORTED_MODEMS_TABLE, String.format(CSV_PLAN_SUPPORTED_MODEMS.toString(), TEST_PLANS.get(0).toString(), TEST_PLANS.get(1).toString())));
        idRoutes.add(DistributelTestUtil.createRoute(PlanBL.PLAN_INFORMATION_OPTIONAL_TABLE, PlanBL.PLAN_INFORMATION_OPTIONAL_TABLE, String.format(CSV_PLAN_OPTIONAL.toString(), TEST_PLANS.get(0).toString(), TEST_PLANS.get(1).toString(), TEST_PLANS.get(2).toString())));

        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        DistributelTestUtil.buildAccountType(api);

        DistributelTestUtil.updatePaymentMethodTypeCreditCard(api);

        DistributelTestUtil.initMetafieldCreation();

        PRANCING_PONY_HARDWARE_CATEGORY_ID = DistributelTestUtil.createItemCategory("HARDWARE", Constants.ORDER_LINE_TYPE_ITEM, true, api);
        PRANCING_PONY_HARDWARE_TYPE_CATEGORY_ID = DistributelTestUtil.createItemCategory("HARDWARE-TYPE", Constants.ORDER_LINE_TYPE_ITEM, false, api);

        PRANCING_PONY_FEE_CATEGORY_ID = DistributelTestUtil.createItemCategory("FEE", Constants.ORDER_LINE_TYPE_ITEM, false, api);

        ItemTypeWS itemTypeWS = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);
        itemTypeWS.setDescription("Service Type - DSL");
        api.updateItemCategory(itemTypeWS);
        ItemTypeWS itemTypeWS1 = api.getItemCategoryById(PRANCING_PONY_HARDWARE_TYPE_CATEGORY_ID);
        itemTypeWS1.setDescription("Hardware Type - DSL");
        api.updateItemCategory(itemTypeWS1);
        itemTypeWS = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);
        DistributelTestUtil.addAssetStatus(itemTypeWS, "Reserved");

        createTestProduct();
        MODEM_PLAN_ID = createPlanModem();
        SIMPLE_PLAN_ID = createSimplePlan();
        CSV_PROCESS_CENTERS.append("process_center_id, email_address, template_id\n")
                .append("processcenter1,testing@jbilling.com,32\n")
                .append("processcenter2,processcenter2@OneTest.com,200\n");
        idRoutes.add(DistributelTestUtil.createRoute(SpaConstants.DT_PROCESS_CENTERS,SpaConstants.DT_PROCESS_CENTERS, String.format(CSV_PROCESS_CENTERS.toString())));
        BillingProcessConfigurationWS billingProcessConfiguration= api.getBillingProcessConfiguration();
        billingProcessConfiguration.setMaximumPeriods(1);
        api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
    }

    
    @Test
    public void test001CustomerTicketInfoResultTest() {
        SpaImportWS spaImportWS = getSpaImportWSForTest();
        Integer userId = distributelApi.processSpaImport(spaImportWS);
        assertNotNull("User not created ",userId);
        Integer[] orderIds = api.getLastOrders(userId, 10);
        AssetWS assetWS = null;
        for (Integer id : orderIds) {
            for (AssetAssignmentWS aa : api.getAssetAssignmentsForOrder(id)) {
                AssetWS asset = api.getAsset(aa.getAssetId());
                if(DistributelTestUtil.getMetaField(asset.getMetaFields(), SpaConstants.DOMAIN_ID) != null &&
                        DistributelTestUtil.getMetaField(asset.getMetaFields(), SpaConstants.DOMAIN_ID).getStringValue() != null) {
                    assetWS = asset;
                    break;
                }
            }
        }
        List<MetaFieldValueWS> metafields = new ArrayList<>();
        metafields.addAll(Arrays.asList(assetWS.getMetaFields()));
        metafields.add(createMetaField("Make/Model", "Test_Model"));
        metafields.add(createMetaField("Tracking Number", "Test_TrackingNumber"));
        metafields.add(createMetaField("courier", "Test_courier"));
        metafields.add(createMetaField("Serial Number", "Test_SerialNumber"));
        metafields.add(createMetaField("Mac Address", "Test_MacAddress"));
        metafields.add(createMetaField("Phone Number", "Test_phoneNumber"));

        assetWS.setMetaFields(metafields.toArray(new MetaFieldValueWS[0]));
        api.updateAsset(assetWS);
        //Customer Name Cannot Be Matched to Eco-System (Jbilling) Database
        SpaHappyFox spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest("Wrong-CustomerName",
                spaImportWS.getPhoneNumber1(), DSL, POSTAL_CODE));
        assertNull(SPA_PRIVATE_NOTE_NULL_MSG, spaHappyFox.getPrivateNotes());
        
        //Customer Input Field - Customer Postal Code Cannot Be Matched to Eco-System (Jbilling) Database
        spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest(spaImportWS.getCustomerName(),
                spaImportWS.getPhoneNumber1(), DSL, "Wrong-postalCode"));
        assertNull(SPA_PRIVATE_NOTE_NULL_MSG, spaHappyFox.getPrivateNotes());

        //Customer Input Field - Customer Phone Number Cannot Be Matched to Eco-System (Jbilling) Database
        spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest(spaImportWS.getCustomerName(),
                "Wrong-phoneNumber", DSL, POSTAL_CODE));
        assertNull(SPA_PRIVATE_NOTE_NULL_MSG, spaHappyFox.getPrivateNotes());

        //Pass service type which doesn’t have any data present in the Eco-System (Jbilling) Database.
        spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest(spaImportWS.getCustomerName(),
                spaImportWS.getPhoneNumber1(), null, POSTAL_CODE));
        assertNull(SPA_PRIVATE_NOTE_NULL_MSG, spaHappyFox.getPrivateNotes());

        //Fetch CustomerTicketInfoResult for service Type DCL
        spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest(spaImportWS.getCustomerName(),
                spaImportWS.getPhoneNumber1(), DSL, POSTAL_CODE));
        assertNotNull(SPA_HAPPY_FOX_MSG, spaHappyFox);
        assertNotNull(SPA_COMMON_FIELD_MSG, spaHappyFox.getCommonFields());
        assertNotNull(SPA_PRIVATE_NOTE_MSG, spaHappyFox.getPrivateNotes());
        assertEquals("City should be equal to city","city", spaHappyFox.getCommonFields().getCity());
        assertTrue("Province should contain QC", spaHappyFox.getCommonFields().getProvince().contains("QC"));
        assertNotNull("Service connection date should not be null", spaHappyFox.getPrivateNotes().getServiceConnectionDate());
        assertNotNull("Rate plan should not be null", spaHappyFox.getPrivateNotes().getRatePlan());
        assertNotNull("Service status should not be null", spaHappyFox.getPrivateNotes().getServiceStatus());
        assertNotNull("CTCIAC should not be null", spaHappyFox.getPrivateNotes().getServiceStatus());
        assertNotNull("Make/Model should not be null", spaHappyFox.getPrivateNotes().getcPEmakeModel());
        assertNotNull("Serial Number should not be null", spaHappyFox.getPrivateNotes().getcPEserialNumber());
        assertNotNull("Mac address should not be null", spaHappyFox.getPrivateNotes().getcPEMACaddress());

        //Fetch CustomerTicketInfoResult for service Type Cable Internet
        ItemTypeWS itemTypeWS  = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);
        itemTypeWS.setDescription("Service Type - Cable");
        api.updateItemCategory(itemTypeWS);
        ItemTypeWS itemTypeWS1 = api.getItemCategoryById(PRANCING_PONY_HARDWARE_TYPE_CATEGORY_ID);
        itemTypeWS1.setDescription("Hardware Type - Cable");
        api.updateItemCategory(itemTypeWS1);
        spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest(spaImportWS.getCustomerName(),
                spaImportWS.getPhoneNumber1(), "Cable Internet", POSTAL_CODE));
        assertNotNull(SPA_HAPPY_FOX_MSG, spaHappyFox);
        assertNotNull(SPA_COMMON_FIELD_MSG, spaHappyFox.getCommonFields());
        assertNotNull(SPA_PRIVATE_NOTE_MSG, spaHappyFox.getPrivateNotes());
        assertNotNull("Service connection date should not be null", spaHappyFox.getPrivateNotes().getServiceConnectionDate());
        assertNotNull("Rate plan should not be null", spaHappyFox.getPrivateNotes().getRatePlan());
        assertNotNull("Service status should not be null", spaHappyFox.getPrivateNotes().getServiceStatus());
        assertNotNull("CYX should not be null", spaHappyFox.getPrivateNotes().getcYX());
        assertNotNull("Make/Model should not be null", spaHappyFox.getPrivateNotes().getcPEmakeModel());
        assertNotNull("Serial Number should not be null", spaHappyFox.getPrivateNotes().getcPEserialNumber());
        assertNotNull("Mac address should not be null", spaHappyFox.getPrivateNotes().getcPEMACaddress());

        //Fetch CustomerTicketInfoResult for service Type Home phone
        itemTypeWS  = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);
        itemTypeWS.setDescription("Service Type - VOIP/DHP");
        api.updateItemCategory(itemTypeWS);
        itemTypeWS1 = api.getItemCategoryById(PRANCING_PONY_HARDWARE_TYPE_CATEGORY_ID);
        itemTypeWS1.setDescription("Hardware Type - VOIP/DHP");
        api.updateItemCategory(itemTypeWS1);
        spaHappyFox = distributelApi.getCustomerTicketInfoResult(getSpaHappyFoxRequest(spaImportWS.getCustomerName(),
                spaImportWS.getPhoneNumber1(), "Home Phone", POSTAL_CODE));
        assertNotNull(SPA_HAPPY_FOX_MSG, spaHappyFox);
        assertNotNull(SPA_COMMON_FIELD_MSG, spaHappyFox.getCommonFields());
        assertNotNull(SPA_PRIVATE_NOTE_MSG, spaHappyFox.getPrivateNotes());
        assertNotNull("Service connection date should not be null", spaHappyFox.getPrivateNotes().getServiceConnectionDate());
        assertNotNull("Rate plan should not be null", spaHappyFox.getPrivateNotes().getRatePlan());
        assertNotNull("Service Phone Number", spaHappyFox.getPrivateNotes().getServicePhoneNumber());
        assertNotNull("Make/Model should not be null", spaHappyFox.getPrivateNotes().getcPEmakeModel());
        assertNotNull("Mac address should not be null", spaHappyFox.getPrivateNotes().getcPEMACaddress());
        assertNotNull("Serial Number should not be null", spaHappyFox.getPrivateNotes().getcPEserialNumber());

    }

    private SpaHappyFox getSpaHappyFoxRequest(String fullCustomerName, String phoneNumber, String serviceType, String servicePostalCode ) {
        return new SpaHappyFox(new SpaCommonFields(phoneNumber, fullCustomerName, serviceType, servicePostalCode), null);
    }
    private MetaFieldValueWS createMetaField(String name , String value){
        MetaFieldValueWS metaField = new MetaFieldValueWS();
        metaField.setFieldName(name);
        metaField.setValue(value);
        return metaField;
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


    private ItemDTOEx createProduct(int testNumber, BigDecimal price, String productNumber, boolean assetsManagementEnabled, Integer categoryId) {
        ItemDTOEx product = CreateObjectUtil.createItem(
                PRANCING_PONY_ENTITY_ID, price, Constants.PRIMARY_CURRENCY_ID, categoryId,
                trimToLength("Test " + productNumber, 35));
        product.setNumber(trimToLength( testNumber + "-" + productNumber, 50));
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
        product.setTypes(new Integer[]{categoryId, PRANCING_PONY_HARDWARE_TYPE_CATEGORY_ID});
        return product;
    }

    private String trimToLength(String value, int length) {
        if (value == null || value.length() < length) return value;
        return value.substring(0, length);
    }

    private void createTestProduct(){
        ItemDTOEx firstItem = createProduct(20, BigDecimal.ONE, "Modem".concat(String.valueOf(System.currentTimeMillis())), true, PRANCING_PONY_HARDWARE_CATEGORY_ID);
        HARDWARE_PRODUCT_ID = api.createItem(firstItem);

        ItemDTOEx secondItem = createProduct(30, BigDecimal.TEN, "FEE Product".concat(String.valueOf(System.currentTimeMillis())), false, PRANCING_PONY_FEE_CATEGORY_ID);
        FEE_PRODUCT_ID = api.createItem(secondItem);

    }

    private Integer createPlanModem() {

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap<>();
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
        models = new TreeMap<>();
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

        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap<>();
        goldServicePlan.setMetaFieldsMap(metafieldMap);

        return api.createPlan(goldServicePlan);
    }

    private Integer createSimplePlan() {

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        bundle2.setQuantity(BigDecimal.ONE);
        bundle2.setAddIfExists(true);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(FEE_PRODUCT_ID);
        pi2.setPrecedence(-1);
        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap<>();
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

        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap<>();
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

    private void createEnumeration(String name, String... values){
        EnumerationWS enumerationWS2 = api.getEnumerationByName(name);
        if(enumerationWS2==null) {
            EnumerationWS enumerationWS = new EnumerationWS(name);
            enumerationWS.setEntityId(PRANCING_PONY_ENTITY_ID);

            for (String value : values) {
                enumerationWS.addValue(value);
            }
        }
    }

    private Integer createPlan(MetaFieldValueWS[] metafields) {
        //I'm creating a (nested) Gold service plan which has the following products:
        //- SMS Service (bundled quantity=1, period = monthly)
        //- GPRS Service (bundled quantity=1, period = monthly)
        //- SMS to NA (bundled quantity=1, period = monthly)

        ItemDTOEx smsServiceItem = new ItemDTOEx();
        smsServiceItem.setDescription("SMS Service");
        smsServiceItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        smsServiceItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        smsServiceItem.setPrice("1");
        smsServiceItem.setNumber("SMS");
        Integer smsServiceItemId = api.createItem(smsServiceItem);

        ItemDTOEx gprsServiceItem = new ItemDTOEx();
        gprsServiceItem.setDescription("GPRS Service");
        gprsServiceItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        gprsServiceItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        gprsServiceItem.setPrice("1");
        gprsServiceItem.setNumber("GPRS");
        Integer gprsServiceItemId = api.createItem(gprsServiceItem);

        ItemDTOEx smsToNaItem = new ItemDTOEx();
        smsToNaItem.setDescription("SMS to NA");
        smsToNaItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        smsToNaItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        smsToNaItem.setPrice("1");
        smsToNaItem.setNumber("SMSNA");
        Integer smsToNaItemId = api.createItem(smsToNaItem);

        ItemDTOEx goldServiceItem = new ItemDTOEx();
        goldServiceItem.setDescription("Gold Service Plan");
        goldServiceItem.setEntityId(PRANCING_PONY_ENTITY_ID);
        goldServiceItem.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_ID});
        goldServiceItem.setPrice("1");
        goldServiceItem.setNumber("GSP");
        Integer goldServiceItemId = api.createItem(goldServiceItem);

        PriceModelWS priceModel = new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.ONE, Constants.PRIMARY_CURRENCY_ID);
        SortedMap<Date, PriceModelWS> models = new TreeMap<>();
        models.put(Constants.EPOCH_DATE, priceModel);

        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle1.setQuantity(BigDecimal.ONE);
        PlanItemWS pi1 = new PlanItemWS();
        pi1.setItemId(smsServiceItemId);
        pi1.setPrecedence(-1);
        pi1.setModels(models);
        pi1.setBundle(bundle1);

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle2.setQuantity(BigDecimal.ONE);
        PlanItemWS pi2 = new PlanItemWS();
        pi2.setItemId(gprsServiceItemId);
        pi2.setPrecedence(-1);
        pi2.setModels(models);
        pi2.setBundle(bundle2);

        PlanItemBundleWS bundle3 = new PlanItemBundleWS();
        bundle3.setPeriodId(ORDER_PERIOD_MONTHLY);
        bundle3.setQuantity(BigDecimal.ONE);
        PlanItemWS pi3 = new PlanItemWS();
        pi3.setItemId(smsToNaItemId);
        pi3.setPrecedence(-1);
        pi3.setModels(models);
        pi3.setBundle(bundle3);

        PlanWS goldServicePlan = new PlanWS();
        goldServicePlan.setItemId(goldServiceItemId);
        goldServicePlan.setDescription("Gold Service Plan" + Short.toString((short) System.currentTimeMillis()));
        goldServicePlan.setPeriodId(ORDER_PERIOD_MONTHLY);
        goldServicePlan.addPlanItem(pi1);
        goldServicePlan.addPlanItem(pi2);
        goldServicePlan.addPlanItem(pi3);

        goldServicePlan.setMetaFields(metafields);

        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap<>();
        metafieldMap.put(PRANCING_PONY_ENTITY_ID, metafields);
        goldServicePlan.setMetaFieldsMap(metafieldMap);

        return api.createPlan(goldServicePlan);
    }


    private SpaImportWS getSpaImportWSForTest() {
        SpaImportWS spaImportWS = new SpaImportWS();
        String customerName = "customer"+ new Random().nextInt();
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
        mainOfferingPlan.setInstallationTime("from 08:20 to 10:20");
        mainOfferingPlan.setStartDate(new Date());
        mainOfferingPlan.setPlanId(SIMPLE_PLAN_ID);
        mainOfferingPlan.setModemId(MODEM_PLAN_ID);
        List<Integer> servicesIds = new ArrayList<>();
        servicesIds.add(SIMPLE_PLAN_ID);
        mainOfferingPlan.setServicesIds(servicesIds);
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

    @AfterClass
    public void tearDown() throws Exception {
        if (null != api) {
            for (Integer idRoute : idRoutes) {
                api.deleteRoute(idRoute);
            }
            for (Integer planId : TEST_PLANS) {
                api.deletePlan(planId);
            }
            BillingProcessConfigurationWS billingProcessConfiguration= api.getBillingProcessConfiguration();
            api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
            api = null;
        }
        if (distributelApi != null) {
            distributelApi = null;
        }

    }
}
