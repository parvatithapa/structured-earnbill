package com.sapienter.jbilling.server.distributel;

import com.sapienter.jbilling.server.item.AssetAssignmentWS;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.spa.AddressType;
import com.sapienter.jbilling.server.spa.SpaAddressWS;
import com.sapienter.jbilling.server.spa.SpaConstants;
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
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import northern911.api.service.Error;
import northern911.api.service.IService;
import northern911.api.service.N911Response;
import northern911.api.service.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;
import java.util.SortedMap;
import java.util.TimeZone;
import java.util.TreeMap;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Created by wajeeha on 4/4/17.
 */


@Test(groups = { "external-system" }, testName = "CustomerEmergency911AddressExternalTest")
public class CustomerEmergency911AddressExternalTest {
    private static final Logger logger = LoggerFactory.getLogger(CustomerEmergency911AddressExternalTest.class);

    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static Integer ORDER_PERIOD_MONTHLY;
    private static JbillingAPI api;
    private static Integer PRANCING_PONY_HARDWARE_CATEGORY_ID;
    private static Integer PRANCING_PONY_FEE_CATEGORY_ID;
    private static Integer HARDWARE_PRODUCT_ID;
    private static Integer FEE_PRODUCT_ID;
    private static Integer MODEM_PLAN_ID;
    private static Integer SIMPLE_PLAN_ID;

    private static final String BANFF_ACCOUNT_ID = "1111AAAA";
    private final  String VENDOR_CODE="298";
    private final  String PASS_CODE="A4c*N8d#";
    
    private static String NEW_CUSTOMER_PHONE = "6136942101";
    private static String SERVICE_ADDRESS_PHONE = "5236942102";
    private static String ASSET_VALID_PHONE = "4336942103";
    private static String ASSET_VALID_PHONE_2 = "4336942105";
    private static String ASSET_INVALID_PHONE = "3436942104";
    private static String ASSET_INVALID_PHONE_2 = "12445136";
    private static String EMERGENCY_ADDRESS_PHONE = "2536942104";
    private static String EMERGENCY_ADDRESS_PHONE_2 = "2536942104";

    private static final String EMERGENCY_ADDRESS_UPDATED_MF = "Emergency Address Updated";
    private static final String PHONE_NUMBER_MF = "Phone Number";
    
    private static final String EMERGENCY_ADDRESS_UPDATE_NORTHERN_911_TASK = "com.sapienter.jbilling.server.user.tasks.EmergencyAddressUpdateNorthern911Task";
    private static final String DISTRIBUTEL_EMERGENCY_ADDRESS_UPDATE_TASK = "com.sapienter.jbilling.server.spa.DistributelEmergencyAddressUpdateTask";
    
    private Integer emergencyAddressUpdateNorthern911TaskId;
    private Integer distributelEmergencyAddressUpdateTaskId;
    private Integer updateDistriubtelCustomerTaskId;

    @BeforeClass
    public void initializeSPAImportEnrollment() throws Exception {
        api = JbillingAPIFactory.getAPI();

        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        DistributelTestUtil.buildAccountType(api);
        DistributelTestUtil.updatePaymentMethodTypeCreditCard(api);

        DistributelTestUtil.initMetafieldCreation();

        PRANCING_PONY_HARDWARE_CATEGORY_ID = DistributelTestUtil.createItemCategory("HARDWARE", Constants.ORDER_LINE_TYPE_ITEM, true, api);
        PRANCING_PONY_FEE_CATEGORY_ID = DistributelTestUtil.createItemCategory("FEE", Constants.ORDER_LINE_TYPE_ITEM, false, api);

        ItemTypeWS itemTypeWS = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);

        DistributelTestUtil.addAssetStatus(itemTypeWS, "Reserved");

        createTestProduct();
        MODEM_PLAN_ID = createPlanModem();
        createFeePlan();
        SIMPLE_PLAN_ID = createSimplePlan();
        enablePlugin();
        updateDistriubtelCustomerTaskId = DistributelTestUtil.enablePlugin(DistributelTestUtil.UPDATE_DISTRIBUTEL_CUSTOMER, api);
    }

    private void enablePlugin() {
        PluggableTaskWS emergencyAddressUpdateNorthern911Task = new PluggableTaskWS();
        PluggableTaskWS distributelEmergencyAddressUpdateTask = new PluggableTaskWS();


        PluggableTaskTypeWS emergencyAddressUpdateNorthern911TaskType = api.getPluginTypeWSByClassName(EMERGENCY_ADDRESS_UPDATE_NORTHERN_911_TASK);
        emergencyAddressUpdateNorthern911Task.setTypeId(emergencyAddressUpdateNorthern911TaskType.getId());
        emergencyAddressUpdateNorthern911Task.setProcessingOrder(63);

        Hashtable<String, String> parameters = new Hashtable<String, String>();
        parameters.put("vendor_code", VENDOR_CODE);
        parameters.put("pass_code", PASS_CODE);
        parameters.put("soap_URL", "https://soapdev.northern911.com/soap/Service.svc?wsdl");
        emergencyAddressUpdateNorthern911Task.setParameters(parameters);

        PluggableTaskTypeWS distributelEmergencyAddressUpdateTaskType = api.getPluginTypeWSByClassName(DISTRIBUTEL_EMERGENCY_ADDRESS_UPDATE_TASK);
        distributelEmergencyAddressUpdateTask.setTypeId(distributelEmergencyAddressUpdateTaskType.getId());
        distributelEmergencyAddressUpdateTask.setProcessingOrder(64);

        emergencyAddressUpdateNorthern911TaskId = api.createPlugin(emergencyAddressUpdateNorthern911Task);
        distributelEmergencyAddressUpdateTaskId = api.createPlugin(distributelEmergencyAddressUpdateTask);

        logger.debug("emergencyAddressUpdateNorthern911TaskId = " + emergencyAddressUpdateNorthern911TaskId);
        logger.debug("distributelEmergencyAddressUpdateTaskId = " + distributelEmergencyAddressUpdateTaskId);
    }

    @AfterClass
    private void disablePlugin() {
        if (emergencyAddressUpdateNorthern911TaskId != null) {
            api.deletePlugin(emergencyAddressUpdateNorthern911TaskId);
            emergencyAddressUpdateNorthern911TaskId = null;
        }

        if (distributelEmergencyAddressUpdateTaskId != null) {
            api.deletePlugin(distributelEmergencyAddressUpdateTaskId);
            distributelEmergencyAddressUpdateTaskId = null;
        }
        DistributelTestUtil.disablePlugin(updateDistriubtelCustomerTaskId, api);
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


        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap();
        goldServicePlan.setMetaFieldsMap(metafieldMap);

        return api.createPlan(goldServicePlan);
    }

    private ItemDTOEx createProduct(int testNumber, BigDecimal price, String productNumber, boolean assetsManagementEnabled, Integer categoryId) {
        ItemDTOEx product = CreateObjectUtil.createItem(
                PRANCING_PONY_ENTITY_ID, price, Constants.PRIMARY_CURRENCY_ID, categoryId,
                trimToLength("Test " + productNumber, 35));
        product.setNumber(trimToLength( testNumber + "-" + productNumber, 50));
        product.setAssetManagementEnabled(assetsManagementEnabled ? 1 : 0);
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

        ItemDTOEx thirdItem = createProduct(30, BigDecimal.TEN, "Product".concat(String.valueOf(System.currentTimeMillis())), false, PRANCING_PONY_FEE_CATEGORY_ID);
        api.createItem(thirdItem);
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

    private Integer createSimplePlan() {
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

    @Test
    public void createCustomerWithNewPhoneNoInstallation()  throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest(NEW_CUSTOMER_PHONE);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), userWS);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        boolean isUpdated = false;
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                assertEquals("Emergency Address is not updated  ", metaFieldValueWS.getValue(), true);
                isUpdated = true;
            }
        }

        assertEquals("User is not updated  ", isUpdated, true);

        logger.debug("Delete Phone no {}", NEW_CUSTOMER_PHONE);
        deletePhoneNo(NEW_CUSTOMER_PHONE);
    }

    @Test
    public void updateServiceAddress() throws IOException, JbillingAPIException {
        SpaImportWS spaImportWS = getSpaImportWSForTest(SERVICE_ADDRESS_PHONE);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), userWS);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        logger.debug("Phone no {}", SERVICE_ADDRESS_PHONE);

        Integer emergencyAddressGroupAITId = DistributelTestUtil.emergencyAddressMetaFieldGroupId;
        Integer contactInformationGroupAITId = DistributelTestUtil.contactInformationMetaFieldGroupId;

        Integer groupAITId = null;
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            logger.debug(" Metafield name = {} | metaFieldValueWS.getGroupId() = {}", metaFieldValueWS.getFieldName(), metaFieldValueWS.getGroupId());
            if (metaFieldValueWS.getGroupId() != null && 
                metaFieldValueWS.getGroupId().equals(emergencyAddressGroupAITId) &&
                SpaConstants.SAME_AS_CUSTOMER_INFORMATION.equals(metaFieldValueWS.getFieldName())) {
                groupAITId = ((Boolean) metaFieldValueWS.getValue()) ? contactInformationGroupAITId : emergencyAddressGroupAITId;
                break;
            } else if (SpaConstants.MF_PROVIDED.equals(metaFieldValueWS.getFieldName())) {
                groupAITId = ((Boolean) metaFieldValueWS.getValue()) ? emergencyAddressGroupAITId : contactInformationGroupAITId;
                break;
            }
        }

        logger.debug("groupAITId = {}", groupAITId);

        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (metaFieldValueWS.getGroupId() != null && 
                metaFieldValueWS.getGroupId().equals(groupAITId) &&
                SpaConstants.STREET_APT_SUITE.equals(metaFieldValueWS.getFieldName())) {
                logger.debug("Setting apt suite");
                metaFieldValueWS.setValue("2");
            }
        }

        api.updateUser(userWS);

        String apt_no = "";

        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (metaFieldValueWS.getGroupId() != null && 
                metaFieldValueWS.getGroupId().equals(groupAITId) &&
                SpaConstants.STREET_APT_SUITE.equals(metaFieldValueWS.getFieldName())) {
                apt_no = (String) metaFieldValueWS.getValue();
            }
        }

        assertEquals("User's apt not updated ", "2", apt_no);

        logger.debug("Delete Phone no {}", SERVICE_ADDRESS_PHONE);
        deletePhoneNo(SERVICE_ADDRESS_PHONE);
    }

    @Test
    public void updateAssetWithValidPhone() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest(ASSET_VALID_PHONE);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), userWS);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        logger.debug("Phone no {}", ASSET_VALID_PHONE);

        MetaFieldValueWS[] metaFields = userWS.getMetaFields();

        boolean isUpdated = false;

        for(MetaFieldValueWS metaFieldValueWS : metaFields){

            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                assertEquals("Emergency Address is not updated  ", metaFieldValueWS.getValue(), true);
                isUpdated = true;
            }

            if (SpaConstants.NORTHERN_911_ERROR_CODE.equals(metaFieldValueWS.getFieldName())) {
                String emergencyCodes = ((String) metaFieldValueWS.getValue());
                logger.debug("Emergency code = {}", emergencyCodes);
            }
        }

        assertEquals("User is not updated  ", true, isUpdated);

        Integer[] orderIds = api.getLastOrders(userWS.getId(), 10);

        for (Integer orderId : orderIds) {
            OrderWS orderWS = api.getOrder(orderId);
            assertEquals("Incorrect user ", userId, orderWS.getUserId());
        }

        AssetAssignmentWS[] assetAssignmentWSes = null;
        for (Integer id : orderIds) {
            assetAssignmentWSes = api.getAssetAssignmentsForOrder(id);
            if (assetAssignmentWSes != null && assetAssignmentWSes.length != 0) {
                break;
            }
        }

        if (assetAssignmentWSes != null) {
            updatePhoneNumber(ASSET_VALID_PHONE_2, assetAssignmentWSes[0]);
        }

         isUpdated = false;

        for(MetaFieldValueWS metaFieldValueWS : metaFields){
            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                assertEquals("Emergency Address is not updated  ", metaFieldValueWS.getValue(), true);
                isUpdated = true;
            }
        }

        assertEquals("User is not updated  ", isUpdated, true);
        logger.debug("Delete Phone no {}", ASSET_VALID_PHONE);
        deletePhoneNo(ASSET_VALID_PHONE);
        logger.debug("Delete Phone no {}", ASSET_VALID_PHONE_2);
        deletePhoneNo(ASSET_VALID_PHONE_2);
    }

    @Test
    public void updateAssetWithInvalidPhone() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest(ASSET_INVALID_PHONE);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");

        SpaAddressWS service = spaImportWS.getAddress(AddressType.SERVICE);
        SpaAddressWS emergency = new SpaAddressWS();
        emergency.setCity(service.getCity());
        emergency.setStreetName(service.getStreetName());
        emergency.setStreetAptSuite(service.getStreetAptSuite());
        emergency.setPostalCode(service.getPostalCode());
        emergency.setStreetNumber(service.getStreetNumber());
        emergency.setProvince(service.getProvince());
        emergency.setAddressType(AddressType.EMERGENCY.toString());

        spaImportWS.getAddresses().add(emergency);
//        System.out.println("==========PAUSE======= updateAssetWithInvalidPhone . processSpaImport");
//        Thread.sleep(10000);
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

//        System.out.println("==========PAUSE======= updateAssetWithInvalidPhone . getUserWS");
//        Thread.sleep(10000);
        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), userWS);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        MetaFieldValueWS[] metaFields = userWS.getMetaFields();

        logger.debug("Phone no {}", ASSET_INVALID_PHONE);

        boolean isUpdated = false;

        for(MetaFieldValueWS metaFieldValueWS : metaFields){
            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                assertEquals("Emergency Address is not updated  ", metaFieldValueWS.getValue(), true);
                isUpdated = true;
            }
        }

        logger.debug("isUpdated =  {}", isUpdated);
        assertEquals("User is not updated  ", isUpdated, true);

        Integer[] orderIds = api.getLastOrders(userWS.getId(), 10);

        for (Integer orderId : orderIds) {
            OrderWS orderWS = api.getOrder(orderId);
            assertEquals("Incorrect user ", userId, orderWS.getUserId());
        }

        AssetAssignmentWS[] assetAssignmentWSes = null;
        for (Integer id : orderIds) {
            assetAssignmentWSes = api.getAssetAssignmentsForOrder(id);
            if (assetAssignmentWSes != null && assetAssignmentWSes.length != 0) {
                break;
            }

        }

        if(assetAssignmentWSes != null){
            updatePhoneNumber(ASSET_INVALID_PHONE_2, assetAssignmentWSes[0]);
        }

        boolean metaFieldValueFound = false;

        metaFields = api.getUserWS(userId).getMetaFields();

        for (MetaFieldValueWS metaFieldValueWS : metaFields) {
            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                logger.debug("meta field name = {} | value = {}", metaFieldValueWS.getFieldName(),metaFieldValueWS.getValue());
                assertEquals("Emergency Address is not updated  ", false, metaFieldValueWS.getValue());
                metaFieldValueFound =true;
            }
        }

        assertEquals("User does not have metafield ", true, metaFieldValueFound);

        deletePhoneNo(ASSET_INVALID_PHONE);
        deletePhoneNo(ASSET_INVALID_PHONE_2);
    }

    @Test
    public void updateAssetAndEmergencyAddress() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest(EMERGENCY_ADDRESS_PHONE);
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Integer userId = DistributelAPIFactory.getAPI().processSpaImport(spaImportWS);

        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), userWS);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        boolean isUpdated = false;
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                assertEquals("Emergency Address is not updated  ", metaFieldValueWS.getValue(), true);
                isUpdated = true;
            }
        }
        assertEquals("User is not updated  ", isUpdated, true);

        Integer[] orderIds = api.getLastOrders(userWS.getId(), 10);

        for (Integer orderId : orderIds) {
            OrderWS orderWS = api.getOrder(orderId);
            assertEquals("Incorrect user ", userId, orderWS.getUserId());
        }

        AssetAssignmentWS[] assetAssignmentWSes = null;
        for (Integer id : orderIds) {
            assetAssignmentWSes = api.getAssetAssignmentsForOrder(id);
            if (assetAssignmentWSes != null && assetAssignmentWSes.length != 0) {
                break;
            }
        }

        if (assetAssignmentWSes != null) {
            updatePhoneNumber(EMERGENCY_ADDRESS_PHONE_2, assetAssignmentWSes[0]);
        }

        isUpdated = false;

        userWS = api.getUserWS(userId);

        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (EMERGENCY_ADDRESS_UPDATED_MF.equals(metaFieldValueWS.getFieldName())) {
                assertEquals("Emergency Address is not updated  ", metaFieldValueWS.getValue(), true);
                isUpdated = true;
            }
        }

        assertEquals("User asset is not updated  ", isUpdated, true);

        Integer emergencyAddressGroupAITId = DistributelTestUtil.emergencyAddressMetaFieldGroupId;
        Integer contactInformationGroupAITId = DistributelTestUtil.contactInformationMetaFieldGroupId;

        logger.debug("userWS.getEntityId() = {} | userWS.getAccountTypeId() = {} | emergencyAddressGroupAITId = {} | contactInformationGroupAITId = {}",
                userWS.getEntityId(), userWS.getAccountTypeId(), emergencyAddressGroupAITId, contactInformationGroupAITId);

        Integer groupAITId = null;

        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {

            if (metaFieldValueWS.getGroupId() != null && 
                metaFieldValueWS.getGroupId().equals(emergencyAddressGroupAITId) && 
                SpaConstants.SAME_AS_CUSTOMER_INFORMATION.equals(metaFieldValueWS.getFieldName())) {
                groupAITId = ((Boolean) metaFieldValueWS.getValue()) ? contactInformationGroupAITId : emergencyAddressGroupAITId;
                break;
            } else if (SpaConstants.MF_PROVIDED.equals(metaFieldValueWS.getFieldName())) {
                groupAITId = ((Boolean) metaFieldValueWS.getValue()) ? emergencyAddressGroupAITId : contactInformationGroupAITId;
                break;
            }
        }

        logger.debug("groupAITId = {}", groupAITId);
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (metaFieldValueWS.getGroupId() != null && 
                metaFieldValueWS.getGroupId().equals(groupAITId) &&
                SpaConstants.STREET_APT_SUITE.equals(metaFieldValueWS.getFieldName())) {
                logger.debug("Setting apt suite");
                metaFieldValueWS.setValue("3");
            }
        }
        api.updateUser(userWS);

        String apt_no = "";
        userWS = api.getUserWS(userId);
        for (MetaFieldValueWS metaFieldValueWS : userWS.getMetaFields()) {
            if (metaFieldValueWS.getGroupId() != null && 
                metaFieldValueWS.getGroupId().equals(groupAITId) &&
                SpaConstants.STREET_APT_SUITE.equals(metaFieldValueWS.getFieldName())) {
                apt_no = (String) metaFieldValueWS.getValue();
            }
        }

        assertEquals("User's apt not updated ", "3", apt_no);

        deletePhoneNo(EMERGENCY_ADDRESS_PHONE);
        deletePhoneNo(EMERGENCY_ADDRESS_PHONE_2);
    }

    private String getGMTDateString()    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    private void deletePhoneNo(String phoneNo){
        try {
            Service service = new Service(new URL("https://soapdev.northern911.com/soap/Service.svc?wsdl"));
            // Will be used to send requests to Northern911
            IService iservice = service.getBasicHttpBindingIService();
            String hash = createHash(VENDOR_CODE, PASS_CODE);
            N911Response response = iservice.deleteCustomer(VENDOR_CODE, phoneNo, hash);

            if (response.isAccepted()) {
               logger.debug("Phone no {} has been deleted", phoneNo) ;
            } else {
                logger.error("Error deleting {}", phoneNo );
                for (Error error : response.getErrors().getValue().getError()) {
                    Integer errorCode = error.getErrorCode();
                    logger.error("Error Code: " + errorCode);
                    logger.error("Error Message: " + error.getErrorMessage());
                }
            }
        } catch (Exception e) {
            logger.debug("Exception = " +e);
        }
    }

    private String createHash(String vendorCode, String passCode) {
        byte[] data = (vendorCode + passCode + getGMTDateString()).getBytes(StandardCharsets.US_ASCII);

        Formatter formatter = new Formatter();
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        digest.update(data);
        for (final byte b : digest.digest()) {
            formatter.format("%02x", b);
        }

        return formatter.toString();
    }

    private SpaImportWS getSpaImportWSForTest(String phoneNumber) {
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
        serviceAddress.setCity("OTTAWA");
        serviceAddress.setStreetName("De La Gauchetière Rue");
        serviceAddress.setStreetAptSuite("1");
        serviceAddress.setPostalCode("K1Y 0V9");
        serviceAddress.setStreetNumber("210");
        serviceAddress.setProvince("ON");

        List<SpaAddressWS> addresses = new ArrayList<>();
        addresses.add(serviceAddress);
        SpaAddressWS billingAddress = new SpaAddressWS();
        billingAddress.setAddressType(AddressType.BILLING.name());

        billingAddress.setCity("OTTAWA");
        billingAddress.setStreetName("De La Gauchetière Rue");
        billingAddress.setStreetAptSuite("1");
        billingAddress.setPostalCode("K1Y 0V9");
        billingAddress.setProvince("ON");

        addresses.add(billingAddress);

        spaImportWS.setAddresses(addresses);

        SpaProductsOrderedWS mainOfferingPlan = new SpaProductsOrderedWS();
        mainOfferingPlan.setServiceType("VOIP");
        mainOfferingPlan.setInstallationTime("from 08:20 to 10:20");
        mainOfferingPlan.setStartDate(new Date());
        mainOfferingPlan.setPlanId(SIMPLE_PLAN_ID);
        mainOfferingPlan.setModemId(MODEM_PLAN_ID);
        mainOfferingPlan.setPhoneNumber(phoneNumber);
        List<Integer> servicesIds = new ArrayList();
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
    
    private void updatePhoneNumber(String phoneNumber, AssetAssignmentWS assetAssignmentWS) {
        Integer assetID = assetAssignmentWS.getAssetId();
        AssetWS asset = api.getAsset(assetID);
        logger.debug("Previous asset identifier = "+ asset.getIdentifier());

        for (MetaFieldValueWS metaFieldValue : asset.getMetaFields()) {
            if (PHONE_NUMBER_MF.equals(metaFieldValue.getFieldName())) {
                metaFieldValue.setValue(phoneNumber);
                api.updateAsset(asset);
                break;
            }
        }
    }

}
