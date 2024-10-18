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

/*
 * Created on April 12, 2017
 *
 */
package com.sapienter.jbilling.server.distributel;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetAssignmentWS;
import com.sapienter.jbilling.server.item.AssetStatusDTOEx;
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
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
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
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.DistributelAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.api.JbillingDistributelAPI;
import com.sapienter.jbilling.server.util.search.SearchResultString;

/**
 * @author developer
 */
@Test(groups = { "test-distributel", "distributel" }, testName = "SpaImportEnrollmentTest")
public class SpaImportEnrollmentTest {

    private static final Logger logger = LoggerFactory.getLogger(SpaImportEnrollmentTest.class);
    private static final Integer PRANCING_PONY_ENTITY_ID = 1;
    private static Integer ORDER_PERIOD_MONTHLY;
    private static Integer ORDER_PERIOD_DAILY;
    private static JbillingAPI api;
    private static JbillingDistributelAPI distributelApi;
    private static Integer PRANCING_PONY_HARDWARE_CATEGORY_ID;
    private static Integer PRANCING_PONY_FEE_CATEGORY_ID;
    private static String PROVINCE_QC = "QC" + System.currentTimeMillis();
    private static final String ASSET_STATE_IN_USE = "In Use";
    private static final String ASSET_STATE_AVAILABLE = "Available";
    private static final String ASSET_STATE_PENDING = "Pending";
    private static Integer ASSET_STATE_IN_USE_ID;
    private static Integer ASSET_STATE_AVAILABLE_ID;
    private static Integer HARDWARE_PRODUCT_ID;
    private static Integer FEE_PRODUCT_ID;
    private static Integer SIMPLE_PRODUCT_ID;
    private static Integer MODEM_PLAN_ID;
    private static Integer FEE_PLAN_ID;
    private static Integer SIMPLE_PLAN_ID;
    private static final String TRACKING_NUMBER = "2222BBBB";
    private static final String BANFF_ACCOUNT_ID = "1111AAAA";
    private static String BANFF_ACCOUNT_ID_PROVISIONING= "2222BBBB";
    private static final String MAC_ADDRESS = "12-34-56-78-9A-BC";
    private static final String SERIAL_NUMBER= "12345678";
    private static final String NEW_SERIAL_NUMBER= "987654321";
    private static final String NEW_MAC_ADDRESS = "AA-BB-CC-DD-EE-FF";
    private static final String NEW_CARRIER = "Carrier-123456";
    private static final String MODEL= "model TEST-54";
    private static final String PROCCESS_CENTER= "processcenter1";
    private static final String HEXENCODED_MESSAGE= "68657861646563696d616c74657874";
    private static StringBuffer CSV_PROCESS_CENTERS = new StringBuffer();

    private static StringBuffer CSV_PLAN_OPTIONAL = new StringBuffer();
    private static StringBuffer CSV_PLAN_SUPPORTED_MODEMS = new StringBuffer();
    private static StringBuffer CSV_CANADIAN_TAXES = new StringBuffer();
    private static Integer PRANCING_PONY_CATEGORY_ID;
    private static Integer PRANCING_PONY_CATEGORY_TAXES_ID;
    private static  String TAX_EFECTIVE_DATE;

    private static String PROVINCE_METAFIELD_VALUE = "PROVINCE TEST";
    private static String USER_TYPE_PUBLIC = "USER_TYPE PUBLIC TEST";
    private static String USER_TYPE_10_PUBLIC = "10: USER_TYPE PUBLIC TEST";
    private static String USER_TYPE_30_CSR = "30: USER_TYPE CSR TEST";
    private static Integer BP_MAXIMUM_PERIODS;

    private static final String TRACKING_NUMBER_TEST = "2222AAAA";
    private static final String NEW_CARRIER_TEST = "Carrier-test-123456";
    private static final String NEW_SERIAL_NUMBER_TEST = "852369741";
    private static final String NEW_MAC_ADDRESS_TEST = "12-34-56-78-9A-AC";
    private static final String MODEL_TEST = "model TEST-59";

    private ArrayList<Integer> TEST_PLANS = new ArrayList();
    private List<PlanWS> plansForQuote=new ArrayList<>();
    private List<Integer> idRoutes=new ArrayList<>();
    private Integer userTypeEnumerationId;
    private Integer updateDistriubtelCustomerTaskId;





    private void createEnumeration(String name, String... values){
        EnumerationWS enumerationWS2 = api.getEnumerationByName(name);
        if(enumerationWS2==null) {
            EnumerationWS enumerationWS = new EnumerationWS(name);
            enumerationWS.setEntityId(PRANCING_PONY_ENTITY_ID);

            for (String value : values) {
                enumerationWS.addValue(value);
            }

            userTypeEnumerationId = api.createUpdateEnumeration(enumerationWS);
        }  else {
            userTypeEnumerationId = enumerationWS2.getId();
        }
    }

    private Integer createPlan(MetaFieldValueWS[] metafields) {
        //        I'm creating a (nested) Gold service plan which has the following products:
//                - SMS Service (bundled quantity=1, period = monthly)
//                - GPRS Service (bundled quantity=1, period = monthly)
//                - SMS to NA (bundled quantity=1, period = monthly)

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
        SortedMap<Date, PriceModelWS> models = new TreeMap();
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

        TreeMap<Integer, MetaFieldValueWS[]> metafieldMap = new TreeMap();
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

    @Test
    public void test001GetPlansByProvince() {
        PlanWS[] plans = distributelApi.getPlans(PROVINCE_METAFIELD_VALUE, null);
        List<PlanWS> planList = new ArrayList<>();
        for (PlanWS plan : plans) {
            for (MetaFieldValueWS mfValue : plan.getMetaFields()) {
                if (SpaConstants.PROVINCE.equals(mfValue.getFieldName()) && mfValue.getValue() != null){
                    planList.add(plan);
                    break;
                }
            }
        }

        plans = planList.toArray(new PlanWS[planList.size()]);

        assertEquals(3, plans.length);

        HashMap<Integer, PlanWS> map = new HashMap();
        map.put(plans[0].getId(), plans[0]);
        map.put(plans[1].getId(), plans[1]);
        map.put(plans[2].getId(), plans[2]);

        assertTrue(map.containsKey(TEST_PLANS.get(0)));
        assertTrue(map.containsKey(TEST_PLANS.get(2)));
        assertTrue(map.containsKey(TEST_PLANS.get(3)));
    }

    @Test
    public void test002GetPlansByUserType() {
        PlanWS[] plans = distributelApi.getPlans(null, USER_TYPE_PUBLIC);

        for (PlanWS plan : plans) {
            assertFalse(plan.getId().equals(TEST_PLANS.get(3)));
        }
    }

    @Test
    public void test003GetPlansByProvinceAndUserType() {
        PlanWS[] plans = distributelApi.getPlans(PROVINCE_METAFIELD_VALUE, USER_TYPE_PUBLIC);
        List<PlanWS> planList = new ArrayList<>();
        for (PlanWS plan : plans) {
            for (MetaFieldValueWS mfValue : plan.getMetaFields()) {
                if (SpaConstants.PROVINCE.equals(mfValue.getFieldName()) && mfValue.getValue() != null){
                    planList.add(plan);
                    break;
                }
            }
        }

        plans = planList.toArray(new PlanWS[planList.size()]);

        assertEquals(2, plans.length);

        HashMap<Integer, PlanWS> map = new HashMap();
        map.put(plans[0].getId(), plans[0]);
        map.put(plans[1].getId(), plans[1]);

        assertTrue(map.containsKey(TEST_PLANS.get(0)));
        assertTrue(map.containsKey(TEST_PLANS.get(2)));

    }

    @Test
    public void test004GetSupportedModemsSearchResultByPlan() throws IOException {

        SearchResultString result = distributelApi.getSupportedModemsSearchResultByPlan(TEST_PLANS.get(0));

        assertEquals(new Integer(3).intValue(), result.getRows().size());

        List<List<String>> rows = result.getStringRows();

        Integer a = 1;
        String informationDetailExpected = "Detail %1$d";
        for (List<String> row : rows) {
            assertEquals(row.get(1), TEST_PLANS.get(0).toString());
            assertEquals(row.get(2), String.format(informationDetailExpected, a++));
        }
    }

    @Test
    public void test005GetOptionalPlansSearchResultByPlan() throws IOException {

        Integer plan1 = TEST_PLANS.get(0);
        Integer plan2 = TEST_PLANS.get(1);
        Integer plan3 = TEST_PLANS.get(2);

        SearchResultString result = distributelApi.getOptionalPlansSearchResultByPlan(plan1);
        List<List<String>> rows = result.getStringRows();
        assertEquals(Integer.parseInt("2"), rows.size());

        assertEquals(rows.get(0).get(1), plan1.toString());
        assertEquals(rows.get(0).get(2), plan2.toString());

        assertEquals(rows.get(1).get(1), plan1.toString());
        assertEquals(rows.get(1).get(2), plan3.toString());
    }

    @Test
    public void test006GetSupportedModemsByPlan() throws IOException {
        String[] result = distributelApi.getSupportedModemsByPlan(TEST_PLANS.get(0));

        assertEquals(Integer.parseInt("3"), result.length);
        List<String> rows = new ArrayList(Arrays.asList(result));

        assertTrue(rows.contains("Detail 1"));
        assertTrue(rows.contains("Detail 2"));
        assertTrue(rows.contains("Detail 3"));
    }

    @Test
    public void test007GetOptionalPlansByPlan() throws IOException {

        Integer plan1 = TEST_PLANS.get(0);
        Integer plan2 = TEST_PLANS.get(1);
        Integer plan3 = TEST_PLANS.get(2);

        PlanWS[] result = distributelApi.getOptionalPlansByPlan(plan1);

        assertEquals(Integer.parseInt("2"), result.length);
        assertTrue("Id is  " + result[0].getId() , result[0].getId().equals(plan2) || result[0].getId().equals(plan3));
        assertTrue("Id is  " + result[1].getId() ,result[1].getId().equals(plan2) || result[1].getId().equals(plan3));
    }

    @Test
    public void test008GetQuote() throws Exception{

        idRoutes.add(DistributelTestUtil.createRoute("canadian_taxes","canadian_taxes",String.format(CSV_CANADIAN_TAXES.toString())));
        /*create taxes as product*/
        ItemDTOEx GST=createProduct(BigDecimal.TEN, "GST (8564 02276 RT0001)", false);
        GST.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_TAXES_ID});
        Integer GTSId = api.createItem(GST);
        GST.setId(GTSId);
        ItemDTOEx PST=createProduct(BigDecimal.TEN, "PST", false);
        PST.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_TAXES_ID});
        Integer PSTId = api.createItem(PST);
        PST.setId(PSTId);
        ItemDTOEx HST=createProduct(BigDecimal.TEN, "N/A", false);
        HST.setTypes(new Integer[]{PRANCING_PONY_CATEGORY_TAXES_ID});
        Integer HSTId = api.createItem(HST);
        HST.setId(HSTId);


        /*id=100,description=PlanA, items:
        ItemA1 - Type=Recurring, Price=10
        ItemA2 - Type=One-Time, Price=20*/
        ItemDTOEx smsActivation = createProduct(BigDecimal.TEN, "test Item A1-SMS Service Activation", false);
        Integer smsActivationId = api.createItem(smsActivation);
        smsActivation.setId(smsActivationId);

        ItemDTOEx gprsActivation = createProduct(new BigDecimal(20), "test Item A2-GPRS Service Activation", false);
        Integer gprsActivationId = api.createItem(gprsActivation);
        gprsActivation.setId(gprsActivationId);

        List<PlanItemWS> planItems = new LinkedList<PlanItemWS>();
        planItems.add(DistributelTestUtil.createPlanItem(smsActivationId, BigDecimal.ONE, Constants.ORDER_PERIOD_ALL_ORDERS,new PriceModelWS(PriceModelStrategy.FLAT.name(), BigDecimal.TEN, Constants.PRIMARY_CURRENCY_ID)));
        planItems.add(DistributelTestUtil.createPlanItem(gprsActivationId, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE,new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(20), Constants.PRIMARY_CURRENCY_ID)));

        PlanWS planA = createPlan("PLAN A test", BigDecimal.TEN,planItems);
        plansForQuote.add(planA);
        /*id=200,description=PlanB, items:
        ItemB1 - Type=Recurring, Price=30
        ItemB2 - Type=One-Time, Price=40*/

        ItemDTOEx gggActivation = createProduct(BigDecimal.ONE, "Item B1-3G Service Activation", false);
        Integer gggActivationId = api.createItem(gggActivation);
        gggActivation.setId(gggActivationId);

        ItemDTOEx smsToAmerica = createProduct(BigDecimal.ONE, "Item B2-SMS to North America", false);
        Integer smsToAmericaId = api.createItem(smsToAmerica);
        smsToAmerica.setId(smsToAmericaId);

        planItems = new LinkedList<PlanItemWS>();
        planItems.add(DistributelTestUtil.createPlanItem(gggActivationId, BigDecimal.ONE, Constants.ORDER_PERIOD_ALL_ORDERS,new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(30), Constants.PRIMARY_CURRENCY_ID)));
        planItems.add(DistributelTestUtil.createPlanItem(smsToAmericaId, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE,new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(40), Constants.PRIMARY_CURRENCY_ID)));

        PlanWS planB = createPlan("PLAN B test", BigDecimal.TEN,planItems);
        plansForQuote.add(planB);

        /*id=300,description=PlanC, items:
        ItemC1 - Type=Recurring, Price=50
        ItemC2 - Type=One-Time, Price=60*/
        ItemDTOEx smsActivation2 = createProduct(BigDecimal.TEN, "test Item C1-SMS Service Activation", false);
        Integer smsActivationId2 = api.createItem(smsActivation2);
        smsActivation.setId(smsActivationId2);

        ItemDTOEx gprsActivation2 = createProduct(new BigDecimal(20), "test Item C2-GPRS Service Activation", false);
        Integer gprsActivationId2 = api.createItem(gprsActivation2);
        gprsActivation.setId(gprsActivationId2);

        planItems = new LinkedList<PlanItemWS>();
        planItems.add(DistributelTestUtil.createPlanItem(smsActivationId2, BigDecimal.ONE, Constants.ORDER_PERIOD_ALL_ORDERS,new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(50), Constants.PRIMARY_CURRENCY_ID)));
        planItems.add(DistributelTestUtil.createPlanItem(gprsActivationId2, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE,new PriceModelWS(PriceModelStrategy.FLAT.name(),  new BigDecimal(60), Constants.PRIMARY_CURRENCY_ID)));

        PlanWS planC = createPlan("PLAN C test", BigDecimal.TEN,planItems);
        plansForQuote.add(planC);
        /*id=400,description=PlanD, items
        ItemD1 - Type=Recurring, Price=70
        ItemD2 - Type=One-Time, Price=80*/
        ItemDTOEx gggActivation2 = createProduct(BigDecimal.ONE, "Item D1-3G Service Activation", false);
        Integer gggActivationId2 = api.createItem(gggActivation2);
        gggActivation.setId(gggActivationId2);

        ItemDTOEx smsToAmerica2 = createProduct(BigDecimal.ONE, "Item D2-SMS to North America", false);
        Integer smsToAmericaId2 = api.createItem(smsToAmerica2);
        smsToAmerica.setId(smsToAmericaId2);

        planItems = new LinkedList<PlanItemWS>();
        planItems.add(DistributelTestUtil.createPlanItem(gggActivationId2, BigDecimal.ONE, Constants.ORDER_PERIOD_ALL_ORDERS,new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(70), Constants.PRIMARY_CURRENCY_ID)));
        planItems.add(DistributelTestUtil.createPlanItem(smsToAmericaId2, BigDecimal.ONE, Constants.ORDER_PERIOD_ONCE,new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(80), Constants.PRIMARY_CURRENCY_ID)));

        PlanWS planD = createPlan("PLAN D test", BigDecimal.TEN,planItems);
        plansForQuote.add(planD);

        PlanWS planWs = distributelApi.getQuote(planA.getId(), "ON", TAX_EFECTIVE_DATE, SpaConstants.ENGLISH_LANGUAGE, planB.getId(), planC.getId());
        BigDecimal taxOneTime= new BigDecimal(BigInteger.ZERO);
        BigDecimal taxRecurrent= new BigDecimal(BigInteger.ZERO);

        for (PlanItemWS planItem : planWs.getPlanItems()) {
            ItemDTOEx item=api.getItem(planItem.getItemId(),null,null);

            if(item.getOrderLineTypeId().equals(Constants.ORDER_LINE_TYPE_TAX)){
                if(planItem.getBundle().getPeriodId().equals(Constants.ORDER_PERIOD_ONCE)){
                    taxOneTime=new BigDecimal(planItem.getModel().getRate());
                }else{
                    taxRecurrent=(new BigDecimal(planItem.getModel().getRate()));
                }
            }
        }
        assertEquals(taxOneTime,new BigDecimal("27.30")); // one time 15.60 + recurring 11.70
        assertEquals(taxRecurrent,new BigDecimal("11.70"));
        api.deleteItem(GST.getId());
        api.deleteItem(HST.getId());
        api.deleteItem(PST.getId());

    }


    @Test
    public void test009SPAImportEnrollmentTest() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getPaymentCredential().setPaymentProfileId(UUID.randomUUID().toString());
        SpaProductsOrderedWS orderRequest = spaImportWS.getProductsOrdered().get(0);
        orderRequest.setSerialNumber("FIRST_ASSET_IDENTIFIER");
        Calendar activeSinceDate = Calendar.getInstance();
        activeSinceDate.set(Calendar.DAY_OF_MONTH, 1);
        orderRequest.setStartDate(activeSinceDate.getTime());
        Integer userId = distributelApi.processSpaImport(spaImportWS);


        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), userWS);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        Integer[] orderIds = api.getLastOrders(userWS.getId(), 10);

        assertEquals(String.format("Should be 6 orders intance of %s",orderIds.length), 6,orderIds.length);

        for (Integer orderId : orderIds) {
            OrderWS orderWS = api.getOrder(orderId);
            assertEquals("Incorrect user ", userId, orderWS.getUserId());
        }

        InvoiceWS[] invoices = api.getAllInvoicesForUser(userId);
        assertEquals("User should be have one invoice, but has " + invoices.length, 1, invoices.length);
        assertEquals("The total invoice expected is incorrect. Actual: " + invoices[0].getTotalAsDecimal(), new BigDecimal("31"), invoices[0].getTotalAsDecimal());

        AssetAssignmentWS[] asset = null;
        for (Integer id : orderIds) {
            asset = api.getAssetAssignmentsForOrder(id);
            if (asset != null && asset.length != 0) {
                break;
            }
        }
        assertNotNull("There is not a asset.", asset);
        assertEquals("It should be an asset.", 1, asset.length);
        assertTrue("There is an incorrect asset.", api.getAsset(asset[0].getAssetId()).getIdentifier().contains("VOIP -"));

        Integer [] payments =api.getPaymentsByUserId(userId);
        assertEquals("Expected payments for user", 1, payments.length );
        PaymentWS payment= api.getPayment(payments[0]);
        assertEquals("Expected amount of payment", BigDecimal.TEN, payment.getAmountAsDecimal());
    }

    @Test
    public void test010SPAImportEnrollmentWithRequiredAdjustmentTest() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("SECOND_ASSET_IDENTIFIER");
        spaImportWS.setRequiredAdjustmentDetails("Adjust Discount orders 20%");
        Integer userId = distributelApi.processSpaImport(spaImportWS);


        UserWS userWS = api.getUserWS(userId);
        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), api.getUserWS(userId));
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName(), userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        Integer[] orderIds = api.getLastOrders(userWS.getId(), 10);

        assertEquals(String.format("Should be 6 orders intance of %s",orderIds.length), 6,orderIds.length);

        for(Integer orderId : orderIds){
            OrderWS orderWS = api.getOrder(orderId);
            assertEquals("Incorrect user ",userId, orderWS.getUserId());
        }

        InvoiceWS[] invoices = api.getAllInvoicesForUser(userId);
        assertEquals("User should have no invoice, but has " + invoices.length, 0, invoices.length);


        Integer [] payments =api.getPaymentsByUserId(userId);
        assertEquals("Expected payments for user", 0, payments.length );
    }

    @Test
    public void test011SPAImportEnrollmentWithoutProvinceOrProductsOrdered() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getAddress(AddressType.BILLING).setProvince(null);
        Integer userId = distributelApi.processSpaImport(spaImportWS);

        assertEquals("result should be error code", SpaErrorCodes.GENERAL_ERROR.getValue(), userId);

        spaImportWS = getSpaImportWSForTest();
        spaImportWS.setProductsOrdered(Collections.EMPTY_LIST);
        userId = distributelApi.processSpaImport(spaImportWS);

        assertEquals("result should be error code", SpaErrorCodes.GENERAL_ERROR.getValue(), userId);

    }

    @Test
    public void test012SPAImportEnrollmentWithoutPaymentCredentialsOrResults() throws Exception {
        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getProductsOrdered().get(0).setSerialNumber(null);
        spaImportWS.setPaymentCredential(null);

        Integer userId = distributelApi.processSpaImport(spaImportWS);

        Integer [] payments =api.getPaymentsByUserId(userId);
        assertEquals("Expected payments for user", 0, payments.length );

        spaImportWS = getSpaImportWSForTest();
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("THIRD_ASSET_IDENTIFIER");
        spaImportWS.setPaymentResult(null);

        userId = distributelApi.processSpaImport(spaImportWS);

        payments =api.getPaymentsByUserId(userId);
        assertEquals("Expected payments for user", 0, payments.length );
    }

    @Test
    public void test0013SetFurtherOrderAndAssetInformation() throws Exception {
        assertEquals(false, distributelApi.setFurtherOrderAndAssetInformation(null, TRACKING_NUMBER, null, null, MAC_ADDRESS, MODEL,null));
        BANFF_ACCOUNT_ID_PROVISIONING = "BANFFID" + Calendar.getInstance().getTimeInMillis();

        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getProductsOrdered().get(0).setSerialNumber(SERIAL_NUMBER);
        spaImportWS.getProductsOrdered().get(0).setBanffAccountId(BANFF_ACCOUNT_ID_PROVISIONING);
        Integer userId = distributelApi.processSpaImport(spaImportWS);

        assertEquals(true, distributelApi.setFurtherOrderAndAssetInformation(BANFF_ACCOUNT_ID_PROVISIONING, TRACKING_NUMBER, NEW_CARRIER, NEW_SERIAL_NUMBER, NEW_MAC_ADDRESS, MODEL,null));

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

        assertNotNull(assetWS);
        assertEquals("Expected asset carrier.", NEW_CARRIER, DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.COURIER).getStringValue() );

        assertEquals("Expected asset tracking number.", TRACKING_NUMBER, DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.TRACKING_NUMBER).getStringValue() );

        assertEquals("Expected asset serial number.", NEW_SERIAL_NUMBER, DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.MF_SERIAL_NUMBER).getStringValue() );

        assertEquals("Expected mac address in asset ", NEW_MAC_ADDRESS, DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.MF_MAC_ADRESS).getStringValue());

        assertEquals("Expected model in asset ", MODEL,
                DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.MF_MODEL).getStringValue());

        //update metafields for given identifire.
        assertEquals(false, distributelApi.setFurtherOrderAndAssetInformation(null, TRACKING_NUMBER, null, null, MAC_ADDRESS, MODEL,null));
        assertEquals(true, distributelApi.setFurtherOrderAndAssetInformation(null, TRACKING_NUMBER_TEST, NEW_CARRIER_TEST, NEW_SERIAL_NUMBER_TEST, NEW_MAC_ADDRESS_TEST, MODEL_TEST,assetWS.getIdentifier()));
        assetWS = api.getAsset(assetWS.getId());
        assertEquals("Expected asset carrier.", NEW_CARRIER_TEST,
                DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.COURIER).getStringValue() );

        assertEquals("Expected asset tracking number.", TRACKING_NUMBER_TEST,
                DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.TRACKING_NUMBER).getStringValue() );

        assertEquals("Expected asset serial number.", NEW_SERIAL_NUMBER_TEST,
                DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.MF_SERIAL_NUMBER).getStringValue() );

        assertEquals("Expected mac address in asset ", NEW_MAC_ADDRESS_TEST,
                DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.MF_MAC_ADRESS).getStringValue());

        assertEquals("Expected model in asset ", MODEL_TEST,
                DistributelTestUtil.getMetaField(assetWS.getMetaFields(), SpaConstants.MF_MODEL).getStringValue());

    }

    @Test
    public void test014ProvisioningProcessCenter()throws Exception {

        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getPaymentCredential().setPaymentProfileId(UUID.randomUUID().toString());
        SpaProductsOrderedWS orderRequest = spaImportWS.getProductsOrdered().get(0);
        orderRequest.setSerialNumber("FOURTH_ASSET_IDENTIFIER");
        orderRequest.setProcessCenter(PROCCESS_CENTER);
        orderRequest.setHexencodedMessage(HEXENCODED_MESSAGE);

        Calendar activeSinceDate = Calendar.getInstance();
        activeSinceDate.set(Calendar.DAY_OF_MONTH, 1);
        orderRequest.setStartDate(activeSinceDate.getTime());

        Integer userId = distributelApi.processSpaImport(spaImportWS);

        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), api.getUserWS(userId));
        UserWS userWS = api.getUserWS(userId);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName().split("_")[0], userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        Integer[] orderIds = api.getLastOrders(userWS.getId(), 10);

        assertEquals(String.format("Should be 6 orders intance of %s",orderIds.length), 6,orderIds.length);

        for (Integer orderId : orderIds) {
            OrderWS orderWS = api.getOrder(orderId);
            assertEquals("Incorrect user ", userId, orderWS.getUserId());
        }

        InvoiceWS[] invoices = api.getAllInvoicesForUser(userId);
        assertEquals("User should be have one invoice, but has " + invoices.length, 1, invoices.length);

        AssetAssignmentWS[] asset = null;
        for (Integer id : orderIds) {
            asset = api.getAssetAssignmentsForOrder(id);
            if (asset != null && asset.length != 0) {
                break;
            }
        }
        assertNotNull("There is not a asset.", asset);
        assertEquals("It should be an asset.", 1, asset.length);
        assertTrue("There is an incorrect asset.", api.getAsset(asset[0].getAssetId()).getIdentifier().contains("VOIP -"));

        Integer [] payments =api.getPaymentsByUserId(userId);
        assertEquals("Expected payments for user", 1, payments.length );
        PaymentWS payment= api.getPayment(payments[0]);
        assertEquals("Expected amount of payment", BigDecimal.TEN, payment.getAmountAsDecimal());

        //clean up
        //if (PROCESS_CENTER_DT_ID != null) {api.deleteRoute(PROCESS_CENTER_DT_ID);}
    }
    
    @Test
    public void test015UpdateCustomer()throws Exception {

        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getPaymentCredential().setPaymentProfileId(UUID.randomUUID().toString());
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FOURTH_ASSET_IDENTIFIER");
        spaImportWS.getProductsOrdered().get(0).setProcessCenter(PROCCESS_CENTER);
        spaImportWS.getProductsOrdered().get(0).setHexencodedMessage(HEXENCODED_MESSAGE);
        Integer userId = distributelApi.processSpaImport(spaImportWS);

        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), api.getUserWS(userId));
        UserWS userWS = api.getUserWS(userId);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName().split("_")[0], userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        SIMPLE_PLAN_ID = createSimplePlan();
        SpaProductsOrderedWS mainOfferingPlan = new SpaProductsOrderedWS();
        mainOfferingPlan.setServiceType("VOIP");
        mainOfferingPlan.setInstallationTime("from 08:20 to 10:20");
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, 5);
        mainOfferingPlan.setStartDate(new Date());
        mainOfferingPlan.setPlanId(SIMPLE_PLAN_ID);
        List<Integer> servicesIds = new ArrayList();
        servicesIds.add(SIMPLE_PLAN_ID);
        mainOfferingPlan.setServicesIds(servicesIds);
        mainOfferingPlan.setBanffAccountId(BANFF_ACCOUNT_ID);

        List<SpaProductsOrderedWS> productsOrderedWSList = new ArrayList<SpaProductsOrderedWS>();
        productsOrderedWSList.add(mainOfferingPlan);
        spaImportWS.setProductsOrdered(productsOrderedWSList);

        List<SpaAddressWS> addresses = new ArrayList<>();
        SpaAddressWS billingAddress = new SpaAddressWS();
        billingAddress.setAddressType(AddressType.BILLING.name());
        billingAddress.setPostalCode("23142");
        billingAddress.setCity("city");
        billingAddress.setProvince(PROVINCE_QC);
        addresses.add(billingAddress);

        spaImportWS.setAddresses(addresses);
        spaImportWS.setCustomerId(userId);
        spaImportWS.setCustomerName("updatedCustomerName");
        spaImportWS.setCustomerCompany("UpdatedCustomerCompany");
        spaImportWS.setPhoneNumber1("phoneNumberNew1");
        spaImportWS.setPhoneNumber2("phoneNumberNew2");
        spaImportWS.setEmailAddress("updated.test@jbilling.com");
        spaImportWS.setEmailVerified(new Date());
        spaImportWS.setLanguage("E");

        userId = distributelApi.processSpaImportInternalProcess(spaImportWS);
        assertEquals("User ID should be equal ",(Integer)userWS.getId(),userId);
        userWS = api.getUserWS(userId);
        assertEquals("Customer name should be equal to  updatedCustomerName ","updatedCustomerName",DistributelTestUtil.getMetaField(userWS.getMetaFields(),SpaConstants.CUSTOMER_NAME).getStringValue());
        assertEquals("Customer company name should be equal to  UpdatedCustomerCompany ","UpdatedCustomerCompany",DistributelTestUtil.getMetaField(userWS.getMetaFields(),SpaConstants.CUSTOMER_COMPANY).getStringValue());
        assertEquals("Customer phoneNumber 1 should be equal to  phoneNumberNew1 ","phoneNumberNew1",DistributelTestUtil.getMetaField(userWS.getMetaFields(),SpaConstants.PHONE_NUMBER_1).getStringValue());
        assertEquals("Customer phoneNumber 2 should be equal to  phoneNumberNew2 ","phoneNumberNew2",DistributelTestUtil.getMetaField(userWS.getMetaFields(),SpaConstants.PHONE_NUMBER_2).getStringValue());
        assertEquals("Customer email address should be equal to  updated.test@jbilling.com ","updated.test@jbilling.com",DistributelTestUtil.getMetaField(userWS.getMetaFields(),SpaConstants.EMAIL_ADDRESS).getStringValue());

    }

    @Test
    public void test016UpdateCustomerWithNewTimeLine()throws Exception {

        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getPaymentCredential().setPaymentProfileId(UUID.randomUUID().toString());
        spaImportWS.getProductsOrdered().get(0).setSerialNumber("FOURTH_ASSET_IDENTIFIER");
        spaImportWS.getProductsOrdered().get(0).setProcessCenter(PROCCESS_CENTER);
        spaImportWS.getProductsOrdered().get(0).setHexencodedMessage(HEXENCODED_MESSAGE);
        Integer userId = distributelApi.processSpaImport(spaImportWS);

        assertNotNull(String.format("User %s not found", spaImportWS.getCustomerName()), api.getUserWS(userId));
        UserWS userWS = api.getUserWS(userId);
        assertEquals(String.format("Expected User %s and actual user %s", spaImportWS.getCustomerName().split("_")[0], userWS.getUserName()), spaImportWS.getCustomerName(), userWS.getUserName().split("_")[0]);

        SIMPLE_PLAN_ID = createSimplePlan();
        SpaProductsOrderedWS mainOfferingPlan = new SpaProductsOrderedWS();
        mainOfferingPlan.setServiceType("VOIP");
        mainOfferingPlan.setInstallationTime("from 08:20 to 10:20");
        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.DAY_OF_MONTH, 5);
        mainOfferingPlan.setStartDate(startDate.getTime());
        mainOfferingPlan.setPlanId(SIMPLE_PLAN_ID);
        List<Integer> servicesIds = new ArrayList();
        servicesIds.add(SIMPLE_PLAN_ID);
        mainOfferingPlan.setServicesIds(servicesIds);
        mainOfferingPlan.setBanffAccountId(BANFF_ACCOUNT_ID);

        List<SpaProductsOrderedWS> productsOrderedWSList = new ArrayList<SpaProductsOrderedWS>();
        productsOrderedWSList.add(mainOfferingPlan);
        spaImportWS.setProductsOrdered(productsOrderedWSList);

        List<SpaAddressWS> addresses = new ArrayList<>();
        SpaAddressWS billingAddress = new SpaAddressWS();
        billingAddress.setAddressType(AddressType.BILLING.name());
        billingAddress.setPostalCode("23142");
        billingAddress.setCity("city");
        billingAddress.setProvince(PROVINCE_QC);
        addresses.add(billingAddress);

        spaImportWS.setAddresses(addresses);
        spaImportWS.setCustomerId(userId);
        spaImportWS.setCustomerName("updatedCustomerName");
        spaImportWS.setCustomerCompany("UpdatedCustomerCompany");
        spaImportWS.setPhoneNumber1("phoneNumberNew1");
        spaImportWS.setPhoneNumber2("phoneNumberNew2");
        spaImportWS.setEmailAddress("updated.test@jbilling.com");
        spaImportWS.setEmailVerified(new Date());
        spaImportWS.setLanguage("E");

        userId = distributelApi.processSpaImportInternalProcess(spaImportWS);
        assertEquals("User ID should be equal ",(Integer)userWS.getId(),userId);
        userWS = api.getUserWS(userId);
        Optional<Integer> groupId = Arrays.stream(api.getAccountType(userWS.getAccountTypeId()).getInformationTypeIds())
              .filter(aitId -> "Contact Information".equals(api.getAccountInformationType(aitId).getDescription()))
              .findFirst();
        if(!groupId.isPresent()) {
             assertFalse("Contact information should be present", 1==2);
        }
        MetaFieldValueWS[] metaFieldValues = null;
        HashMap<Date, ArrayList<MetaFieldValueWS>> metaFieldsTimeLine = userWS.getAccountInfoTypeFieldsMap().get(groupId.get());

		for (Map.Entry<Date, ArrayList<MetaFieldValueWS>> entry : metaFieldsTimeLine.entrySet()) {
			if(entry.getKey().after(new Date())){
				metaFieldValues = entry.getValue().toArray(new MetaFieldValueWS[entry.getValue().size()]);
			}
		}
       assertEquals("Customer name should be equal to  updatedCustomerName ","updatedCustomerName",DistributelTestUtil.getMetaField(metaFieldValues,SpaConstants.CUSTOMER_NAME).getStringValue());
       assertEquals("Customer company name should be equal to  UpdatedCustomerCompany ","UpdatedCustomerCompany",DistributelTestUtil.getMetaField(metaFieldValues,SpaConstants.CUSTOMER_COMPANY).getStringValue());
       assertEquals("Customer phoneNumber 1 should be equal to  phoneNumberNew1 ","phoneNumberNew1",DistributelTestUtil.getMetaField(metaFieldValues,SpaConstants.PHONE_NUMBER_1).getStringValue());
       assertEquals("Customer phoneNumber 2 should be equal to  phoneNumberNew2 ","phoneNumberNew2",DistributelTestUtil.getMetaField(metaFieldValues,SpaConstants.PHONE_NUMBER_2).getStringValue());
       assertEquals("Customer email address should be equal to  updated.test@jbilling.com ","updated.test@jbilling.com",DistributelTestUtil.getMetaField(metaFieldValues,SpaConstants.EMAIL_ADDRESS).getStringValue());
    }

    /**
     * Test case for service asset identifier
     * @throws Exception
     */
    @Test
    public void test0017processSpaImportTest() throws Exception {
        BANFF_ACCOUNT_ID_PROVISIONING = "BANFFID" + Calendar.getInstance().getTimeInMillis();

        SpaImportWS spaImportWS = getSpaImportWSForTest();
        spaImportWS.getProductsOrdered().get(0).setSerialNumber(SERIAL_NUMBER);
        spaImportWS.getProductsOrdered().get(0).setBanffAccountId(BANFF_ACCOUNT_ID_PROVISIONING);
        spaImportWS.getProductsOrdered().get(0).setModemAssetIdentifier("Test-123456789");
        Integer userId = distributelApi.processSpaImport(spaImportWS);

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
        assertEquals("Assert identifire should be equals to ","Test-123456789",assetWS.getIdentifier());
    }

    @BeforeClass
    public void initializeSPAImportEnrollment() throws Exception {
        api = JbillingAPIFactory.getAPI();
        distributelApi = DistributelAPIFactory.getAPI();
        PRANCING_PONY_CATEGORY_ID = DistributelTestUtil.createItemCategory(Constants.ORDER_LINE_TYPE_ITEM);
        PRANCING_PONY_CATEGORY_TAXES_ID = DistributelTestUtil.createItemCategory(Constants.ORDER_LINE_TYPE_TAX);
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
        TAX_EFECTIVE_DATE="2017-06-05";


        /////////////////////////////////////////////////////////////////////////////////////////////////////////////
        ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
        ORDER_PERIOD_DAILY = getOrCreateDailyOrderPeriod(api);
        DistributelTestUtil.buildAccountType(api);

        DistributelTestUtil.updatePaymentMethodTypeCreditCard(api);

        DistributelTestUtil.initMetafieldCreation();

        PRANCING_PONY_HARDWARE_CATEGORY_ID = DistributelTestUtil.createItemCategory("HARDWARE", Constants.ORDER_LINE_TYPE_ITEM, true, api);
        PRANCING_PONY_FEE_CATEGORY_ID = DistributelTestUtil.createItemCategory("FEE", Constants.ORDER_LINE_TYPE_ITEM, false, api);

        ItemTypeWS itemTypeWS = api.getItemCategoryById(PRANCING_PONY_HARDWARE_CATEGORY_ID);
        Set<AssetStatusDTOEx> states = itemTypeWS.getAssetStatuses();
        for(AssetStatusDTOEx state : states){
            if(ASSET_STATE_IN_USE.equals(state.getDescription())){
                ASSET_STATE_IN_USE_ID = state.getId();
            }
            if(ASSET_STATE_AVAILABLE.equals(state.getDescription())){
                ASSET_STATE_AVAILABLE_ID = state.getId();
            }
        }

        DistributelTestUtil.addAssetStatus(itemTypeWS, "Reserved");

        createTestProduct();
        MODEM_PLAN_ID = createPlanModem();
        FEE_PLAN_ID = createFeePlan();
        SIMPLE_PLAN_ID = createSimplePlan();
        CSV_PROCESS_CENTERS.append("process_center_id, email_address, template_id\n")
                .append("processcenter1,testing@jbilling.com,32\n")
                .append("processcenter2,processcenter2@OneTest.com,200\n");
        idRoutes.add(DistributelTestUtil.createRoute(SpaConstants.DT_PROCESS_CENTERS,SpaConstants.DT_PROCESS_CENTERS, String.format(CSV_PROCESS_CENTERS.toString())));
        BillingProcessConfigurationWS billingProcessConfiguration= api.getBillingProcessConfiguration();
        BP_MAXIMUM_PERIODS= billingProcessConfiguration.getMaximumPeriods();
        billingProcessConfiguration.setMaximumPeriods(1);
        api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
        updateDistriubtelCustomerTaskId = DistributelTestUtil.enablePlugin(DistributelTestUtil.UPDATE_DISTRIBUTEL_CUSTOMER, api);
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

    private Integer getOrCreateDailyOrderPeriod(JbillingAPI api) {
	    OrderPeriodWS daily = new OrderPeriodWS();
	    daily.setEntityId(api.getCallerCompanyId());
	    daily.setPeriodUnitId(PeriodUnitDTO.DAY);
	    daily.setValue(1);
	    daily.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
	            "ORD:DAILY:"+1)));
	    Integer dailyPeriod = api.createOrderPeriod(daily);

	    return dailyPeriod;
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
        SIMPLE_PRODUCT_ID = api.createItem(thirdItem);

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


    private PlanWS createPlan(String planName,BigDecimal price,
                              List<PlanItemWS> planBundleItems){
        ItemDTOEx planItem = createProduct(price, "Subscription "+planName, false);
        planItem.setId(api.createItem(planItem));
        PlanWS plan = new PlanWS();
        plan.setDescription(planName);
        plan.setEditable(0);
        plan.setPeriodId(Constants.ORDER_PERIOD_ONCE);
        plan.setItemId(planItem.getId());
        plan.setPlanItems(planBundleItems);
        Integer planId = api.createPlan(plan);
        return api.getPlanWS(planId);
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


    @AfterClass
    public void tearDown() throws Exception {
        if (null != api) {
            DistributelTestUtil.disablePlugin(updateDistriubtelCustomerTaskId, api);
            //clean up
            for (PlanWS plan : plansForQuote) {
                api.deletePlan(plan.getId());
            }
            for (Integer idRoute : idRoutes) {
                api.deleteRoute(idRoute);
            }
            for (Integer planId : TEST_PLANS) {
                api.deletePlan(planId);
            }
            api.deleteEnumeration(userTypeEnumerationId);
            BillingProcessConfigurationWS billingProcessConfiguration= api.getBillingProcessConfiguration();
            billingProcessConfiguration.setMaximumPeriods(BP_MAXIMUM_PERIODS);
            api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
            api = null;
        }
        if (distributelApi != null) {
            distributelApi = null;
        }

    }
}
