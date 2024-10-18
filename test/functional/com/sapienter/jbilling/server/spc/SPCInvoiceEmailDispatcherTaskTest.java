package com.sapienter.jbilling.server.spc;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.billing.task.InvoiceEmailDispatcherTask;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.spc.util.CreatePlanUtility;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;

@Test(groups = "agl", testName = "agl.SPCInvoiceEmailDispatcherTaskTest")
public class SPCInvoiceEmailDispatcherTaskTest extends SPCBaseConfiguration {

    public static final String CYCLE_START_DATE = "Cycle start date {}";
    public static final boolean PRORATE_TRUE = true;
    public static final String QUANTITY_ZERO = "0.0000000000";

    private static final String  USER_01 = "TestUser01" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_01       = "TestOrder01"+ System.currentTimeMillis();
    private static final String ASSET01 = "1221"+ getRandomNumberString();
    private static final String  USER_02 = "TestUser02" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_02       = "TestOrder02"+ System.currentTimeMillis();
    private static final String ASSET02 = "1221"+ getRandomNumberString();
    private static final String  USER_03 = "TestUser03" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_03       = "TestOrder03"+ System.currentTimeMillis();
    private static final String ASSET03 = "1221"+ getRandomNumberString();
    private static final String  USER_04 = "TestUser04" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_04       = "TestOrder04"+ System.currentTimeMillis();
    private static final String ASSET04 = "1221"+ getRandomNumberString();
    private static final String  USER_05 = "TestUser05" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_05       = "TestOrder05"+ System.currentTimeMillis();
    private static final String ASSET05 = "1221"+ getRandomNumberString();
    private static final String  USER_06 = "TestUser06" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_06       = "TestOrder06"+ System.currentTimeMillis();
    private static final String ASSET06 = "1221"+ getRandomNumberString();
    private static final String  USER_07 = "TestUser07" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_07       = "TestOrder07"+ System.currentTimeMillis();
    private static final String ASSET07 = "1221"+ getRandomNumberString();
    private static final String  USER_09 = "TestUser09" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_09       = "TestOrder09"+ System.currentTimeMillis();
    private static final String ASSET09 = "1221"+ getRandomNumberString();
    private static final String  USER_10 = "TestUser10" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_10       = "TestOrder10"+ System.currentTimeMillis();
    private static final String ASSET11 = "1221"+ getRandomNumberString();
    private static final String  USER_11 = "TestUser11" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_11       = "TestOrder11"+ System.currentTimeMillis();
    private static final String ASSET12 = "1221"+ getRandomNumberString();
    private static final String  USER_12 = "TestUser12" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_12       = "TestOrder12"+ System.currentTimeMillis();
    private static final String ASSET13 = "1221"+ getRandomNumberString();
    private static final String  USER_13 = "TestUser13" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_13       = "TestOrder13"+ System.currentTimeMillis();
    private static final String ASSET14 = "1221"+ getRandomNumberString();
    private static final String  USER_14 = "TestUser14" + System.currentTimeMillis();
    private static final String SUBSCRIPTION_ORDER_14       = "TestOrder14"+ System.currentTimeMillis();
    private static final String ASSET10 = "1221"+ getRandomNumberString();
    private static final String OPTUS_PLAN_02 = "SPCMO-02926";
    private static final String PLAN_CREATION_ASSERT = "Plan Creation Failed";
    public static final String VALUE_1024 = "1024.0000000000";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final int BILLIING_TYPE_MONTHLY = 1;
    private String proratingAuto = ProratingType.PRORATING_AUTO_ON.getProratingType();
    List<AssetWS> assetWSs = new ArrayList<>();
    UserWS userWS;
    Map<Integer, BigDecimal> productQuantityMapOptus = new HashMap<>();
    Map<Integer, BigDecimal> productQuantityMapTelstra = new HashMap<>();
    Integer planId;
    Integer pluginId;
    String tableName;
    @Resource(name = "spcJdbcTemplate")
    private JdbcTemplate jdbcTemplate;
    String cutOffDefaultValueWS;
    Integer billingMonth = -1;
    
    @BeforeClass
    public void initialize() {
        logger.debug("SPCInvoiceEmailDispatcherTaskTest  : {}" , testBuilder);
        if(null == testBuilder) {
            testBuilder = getTestEnvironment();
        }        
        boolean shouldExecute = true;
        if (shouldExecute) {
            testBuilder.given(envBuilder -> {
                markAllUsersDeleted();
                createDataTable();
                configurePlans();
                pluginId = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(InvoiceEmailDispatcherTask.class.getName()).getId()).getId();
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_HOLIDAY_TABLE_NAME_META_FIELD,"holiday_list");
                cutOffDefaultValueWS = api.getCompany().getMetaFieldByName(EMAIL_JOB_DEFAULT_CUT_OFF_TIME).getDefaultValue().toString();
            });
        }        
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/01
     * 2.Create holiday for 2021/09/02 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 1 
     * 5.Trigger billing for 2021/09/01 
     * 6.Validate email invoice process
     * info and notification arch
     */
    @Test(enabled = true,priority = 1)
    public void test001SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                Date nextInvoiceDate = getDate(billingMonth, 1).getTime();
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 2),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_01 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName()); 

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET01, "asset-01");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_01, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "1");
                validateBillingConfiguration("1",1);

            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/04
     * 2.Create holiday for 2021/09/04 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 4
     * 5.Trigger billing for 2021/09/04 
     * 6.Validate email invoice process
     * info and notification arch
     */
    @Test(enabled = true,priority = 2)
    public void test002SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                Date nextInvoiceDate = getDate(billingMonth, 4).getTime();
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 4),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_02 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName()); 

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET02, "asset-02");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_02, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "4");

                validateBillingConfiguration("4",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/05
     * 2.Create holiday for 2021/09/05 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag false
     * 4.Update billing process configurations and set value of skip emails days
     * to 5 
     * 5.Trigger billing for 2021/09/05 
     * 6.Validate email invoice process
     * info and notification arch
     */
    @Test(enabled = true,priority = 3)
    public void test003SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                Date nextInvoiceDate = getDate(billingMonth, 5).getTime();
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 5),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_03 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName()); 

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET03, "asset-03");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_03, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 0, "5");
                validateBillingConfiguration("5",0);

            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/06
     * 2.Create holiday for 2021/09/05 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag false
     * 4.Update billing process configurations and set value of skip emails days
     * to 6 
     * 5.Trigger billing for 2021/09/06 
     * 6.Validate email invoice process
     * info and notification arch
     */
    @Test(enabled = true,priority = 4)
    public void test004SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                Date nextInvoiceDate = getDate(billingMonth, 6).getTime();
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 5),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_04 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET04, "asset-04");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_04, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 0, "6");
                validateBillingConfiguration("6",0);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/07
     * 2.Create holiday for 2021/09/07 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 7 
     * 5.Trigger billing for 2021/09/07 
     * 6.Validate email invoice process
     * info and notification arch 
     * 7.Trigger SPCInvoiceEmailDispatcherTask
     * 8.Validate email invoice process info and notification arch
     */
    @Test(enabled = false,priority = 5)
    public void test005SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                Date nextInvoiceDate = getDate(billingMonth, 7).getTime();
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 7),DATE_FORMAT));
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(-4));
                userWS = getSPCTestUserWS(envBuilder, USER_05 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET05, "asset-05");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_05, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "7");

                validateBillingConfiguration("7",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 7)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 7)));
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/08
     * 2.Create holiday for 2021/09/09 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 8 
     * 5.Trigger billing for 2021/09/08 
     * 6.Validate email invoice process
     * info and notification arch 
     * 7.Update Dispatch email again parameter to false
     * 8.Trigger SPCInvoiceEmailDispatcherTask on
     * 2021/09/10
     * 9.Validate email invoice process info and notification arch
     */
    @Test(enabled = true,priority = 6)
    public void test006SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                Date nextInvoiceDate = getDate(billingMonth, 8).getTime();
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 8),DATE_FORMAT));
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(-4));
                userWS = getSPCTestUserWS(envBuilder, USER_06 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET06, "asset-06");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_06, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "8");

                validateBillingConfiguration("8",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 9).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 8)));
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/10
     * 2.Create holiday for 2021/09/10 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 10 
     * 5.Trigger billing for 2021/09/10 
     * 6.Validate email invoice process
     * info and notification arch 
     * 7.Update Dispatch email again parameter to
     * true 
     * 8.Trigger SPCInvoiceEmailDispatcherTask on 2021/09/11 9.Validate
     * email invoice process info and notification arch
     */
    @Test(enabled = true,priority = 7)
    public void test007SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                Date nextInvoiceDate = getDate(billingMonth, 10).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(4));
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 10),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_07 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET07, "asset-07");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_07, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "10");

                validateBillingConfiguration("10",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 10).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 10)));
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/13
     * 2.Create holiday for 2021/09/13 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 4,20 
     * 5.Trigger billing for 2021/09/13 
     * 6.Validate email invoice process
     * info and notification arch 
     * 7.Update Dispatch email again parameter to
     * true 
     * 8.Trigger SPCInvoiceEmailDispatcherTask on 2021/09/14 9.Validate
     * email invoice process info and notification arch
     */
    @Test(enabled = true,priority = 8)
    public void test008SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                Date nextInvoiceDate = getDate(billingMonth, 13).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,null);
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 13),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_09 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET09, "asset-09");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_09, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "4,20");
                validateBillingConfiguration("4,20",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateSPCInvoiceEmailDispatcherTaskWithDispatchAgainParameter(billingProcessWS, "Billing Process");
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 13).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithDispatchAgainParameter(billingProcessWS, "Email Job");
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/14
     * 2.Create holiday for 2021/09/14 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 14 
     * 5.Trigger billing for 2021/09/14 
     * 6.Validate email invoice process
     * info and notification arch 
     * 7.Update Dispatch email again parameter to true 
     * 8.Update Email job cut off time to 20:00 pm 
     * 9.Trigger SPCInvoiceEmailDispatcherTask on 2021/09/15 
     * 10.Validate email invoice
     * process info and notification arch
     */
    @Test(enabled = true,priority = 9)
    public void test009SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                Date nextInvoiceDate = getDate(billingMonth, 14).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(-2));
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 14),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_10 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET10, "asset-10");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_10, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "14");
                validateBillingConfiguration("14",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 14).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 14).plusDays(1)));
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create user and Subscription order with NID and Active since 2021/09/15
     * 2.Create holiday for 2021/09/15 in datatable route_70_holiday_list
     * 3.Update billing process configurations and set skip emails flag true
     * 4.Update billing process configurations and set value of skip emails days
     * to 15 
     * 5.Trigger billing for 2021/09/15 
     * 6.Validate email invoice process
     * info and notification arch 
     * 7.Update Dispatch email again parameter to false 
     * 8.Update Email job cut off time to 20:00 pm 
     * 9.Trigger
     * SPCInvoiceEmailDispatcherTask on 2021/09/16 
     * 10.Validate email invoice
     * process info and notification arch
     */
    @Test(enabled = false,priority = 10)
    public void test010SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                Date nextInvoiceDate = getDate(billingMonth, 15).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(-4));
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 15),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_11 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET11, "asset-11");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_11, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "15");

                validateBillingConfiguration("15",1);
            }).validate((testEnv, testEnvBuilder) -> {
                BillingProcessWS billingProcessWS = api.getBillingProcess(api.getLastBillingProcess());
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 15).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 15).plusDays(1)));
            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    /**
     * 1.Create 3 user and Subscription order with NID's and Active since's 2021/09/17,2021/09/18,2021/09/19
       2.Create holiday for 2021/09/17,2021/09/18,2021/09/19 in datatable route_70_holiday_list
       3.Update billing process configurations and set skip emails flag true
       4.Update billing process configurations and set value of skip emails days to 17,18,19
       5.Trigger billing for 2021/09/17,2021/09/18,2021/09/19
       6.Validate email invoice process info and notification arch
       7.Update Dispatch email again parameter to false
       8.Update Email job cut off time to 20:00 pm
       9.Update InvoiceEmailDispatcherTask with parameter billing process id which is done on 2021/09/18
       10.Validate billrun and email invoice process info and notification arch
       11.Trigger SPCInvoiceEmailDispatcherTask on 2021/09/20
       12.Validate email invoice process info and notification arch
       13.Update company metafield Cut Off Billing Process Id with billing process id which is done on 2021/09/18
       14.Update InvoiceEmailDispatcherTask with parameter billing process id which is done on 2021/09/17
       15.Validate billrun and email invoice process info and notification arch
       16.Trigger SPCInvoiceEmailDispatcherTask on 2021/09/20
       17.Validate email invoice process info and notification arch
       18.Update InvoiceEmailDispatcherTask with parameter billing process id which is done on 2021/09/19
       19.Validate billrun and email invoice process info and notification arch
       20.Trigger SPCInvoiceEmailDispatcherTask on 2021/09/20
       21.Validate email invoice process info and notification arch

     */
    @Test(enabled = true,priority = 11)
    public void test011SpcInvoiceEmailDispatcherTest() {

        try {
            testBuilder.given(envBuilder -> {
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),CUT_OFF_BILLING_PROCESS_ID,String.valueOf(api.getBillingProcess(api.getLastBillingProcess()).getId()));
                Date nextInvoiceDate = getDate(billingMonth, 17).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(4));
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 17),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_12 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                Integer asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET12, "asset-12");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                PlanWS optusPlanWS = api.getPlanWS(planId);
                assertNotNull("optusPlanWS : ", optusPlanWS);
                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                Integer orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_12, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "17");

                validateBillingConfiguration("17",1);
                assetWSs.clear();
                productQuantityMapOptus.clear();

                nextInvoiceDate = getDate(billingMonth, 18).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(4));
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 18),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_13 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET13, "asset-13");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_13, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "18");
                assetWSs.clear();
                productQuantityMapOptus.clear();

                nextInvoiceDate = getDate(billingMonth, 19).getTime();
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,0,true);
                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),EMAIL_JOB_DEFAULT_CUT_OFF_TIME,getCutOffTime(4));
                buildAndPersistEmailHoliDayDataTableRecord("New Year's Day",getDateFormatted(getLocalDate(billingMonth, 19),DATE_FORMAT));
                userWS = getSPCTestUserWS(envBuilder, USER_14 + System.currentTimeMillis(), nextInvoiceDate, "",
                        CUSTOMER_TYPE_VALUE_PRE_PAID, AUSTRALIA_POST, CC);
                assertNotNull("SPC test customer created ", userWS);
                logger.debug("spcTestUserWS created {}", userWS.getUserName());

                asset1   = buildAndPersistAsset(envBuilder, getCategoryIdByName (testBuilder.getTestEnvironment(),MOBILE_NUMBERS_CATEGORY),
                        getItemIdByCode (testBuilder.getTestEnvironment(),USAGE_PRODUCT_CODE_MOBILE_NUMBERS), ASSET14, "asset-14");
                assertNotNull("Asset created ", asset1);
                assetWSs.add(api.getAsset(asset1));

                productQuantityMapOptus.put(optusPlanWS.getItemId(), BigDecimal.ONE);
                orderId = createOrderWithAsset(SUBSCRIPTION_ORDER_14, userWS.getId(), nextInvoiceDate, null, MONTHLY_ORDER_PERIOD,
                        BILLIING_TYPE_MONTHLY, true, productQuantityMapOptus, assetWSs, optusPlanWS.getId());
                assertNotNull("Plan Order created :  ", orderId);
                logger.debug("Plan Order created {}", orderId);
                triggerBilling(api, nextInvoiceDate, false, proratingAuto, 3, PeriodUnitDTO.MONTH, 1, "19");

                validateBillingConfiguration("19",1);
            }).validate((testEnv, testEnvBuilder) -> {
                ConfigurationBuilder confBuilder = ConfigurationBuilder.getBuilder(api, testBuilder.getTestEnvironment());
                Integer cutOffBillingId = getCutOffBillingId(1,1);
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,cutOffBillingId,false);
                logger.debug("randomBillingId 1 {}",cutOffBillingId);
                BillingProcessWS billingProcessWS = api.getBillingProcess(cutOffBillingId);
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 19).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 19)));

                setCompanyLevelMetaField(testBuilder.getTestEnvironment(),CUT_OFF_BILLING_PROCESS_ID,String.valueOf(cutOffBillingId));
                Integer beforeCutOffBillingId = getCutOffBillingId(1,2);
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,beforeCutOffBillingId,false);
                logger.debug("randomBillingId 2 {}",beforeCutOffBillingId);
                billingProcessWS = api.getBillingProcess(beforeCutOffBillingId);
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,beforeCutOffBillingId,false);
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 19).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 19)));

                Integer afterCutOffBillingId = getCutOffBillingId(1,0);
                logger.debug("randomBillingId 3 {}",afterCutOffBillingId);
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,afterCutOffBillingId,false);
                billingProcessWS = api.getBillingProcess(afterCutOffBillingId);
                validateBillRunWithSkipEmails(billingProcessWS, "Billing Process",userWS.getId());
                createUpdateSPCInvoiceEmailDispatcherTask(confBuilder,afterCutOffBillingId,false);
                api.triggerScheduledTask(pluginId, getLocalDateAsDate(getLocalDate(billingMonth, 19).plusDays(1)));
                validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(billingProcessWS, "Email Job",userWS.getId(), getLocalDateAsDate(getLocalDate(billingMonth, 19)));

            });
        } finally {
            assetWSs.clear();
            productQuantityMapOptus.clear();
            clearTestDataForUser(userWS.getId());
        }
    }

    private void configurePlans() {
        String optusPlanDescription = "NBN Triple Bundle";
        String planTypeOptus = "Optus";
        String optusPlanServiceType = "Mobile";
        BigDecimal optusPlanPrice = new BigDecimal("21.8182");
        BigDecimal optusPlanUsagePoolQuantity = new BigDecimal("209715200");
        BigDecimal optusPlanBoostQuantity = new BigDecimal("1024");
        Integer optusPlanBoostCount = new Integer("3");
        String rate_card_name_1_with_hypen = ROUTE_RATE_CARD_SPC_OM_PLAN_RATING_1.replace('_', '-');

        planId = CreatePlanUtility.createPlan(api, OPTUS_PLAN_02, planTypeOptus, optusPlanServiceType,
                optusPlanDescription, "SPC", rate_card_name_1_with_hypen, "x", optusPlanPrice, PRORATE_TRUE, optusPlanUsagePoolQuantity,
                optusPlanBoostCount, optusPlanBoostQuantity);
        Assert.assertNotNull(PLAN_CREATION_ASSERT, planId);
        validatePlanUsagePools(planId, 4, "200.0000000000", VALUE_1024);

    }

    public static String getRandomNumberString() {
        Random rnd = new Random();
        int number = rnd.nextInt(999999);
        return String.format("%06d", number);
    }

    public Map<String, Object> findInvoiceEmailProcessInfoByBillingId(Integer billingId) {
        String sql = "SELECT iepi.job_execution_id, " + "iepi.billing_process_id, " + "iepi.emails_estimated, " + "iepi.emails_sent, "
                + "iepi.emails_failed, " + "iepi.start_datetime, " + "iepi.end_datetime, " + "iepi.source "
                + "FROM invoice_email_process_info iepi " + "WHERE iepi.billing_process_id = ? ";

        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, billingId);
        Map<String, Object> invoiceEmailProcessInfoMap = new HashMap<>();
        if (row.next()) {
            invoiceEmailProcessInfoMap.put("job_execution_id", row.getInt("job_execution_id"));
            invoiceEmailProcessInfoMap.put("billing_process_id", row.getInt("billing_process_id"));
            invoiceEmailProcessInfoMap.put("emails_estimated", row.getInt("emails_estimated"));
            invoiceEmailProcessInfoMap.put("emails_sent", row.getInt("emails_sent"));
            invoiceEmailProcessInfoMap.put("emails_failed", row.getInt("emails_failed"));
            invoiceEmailProcessInfoMap.put("start_datetime", row.getDate("start_datetime"));
            invoiceEmailProcessInfoMap.put("end_datetime", row.getDate("end_datetime"));
            invoiceEmailProcessInfoMap.put("source", row.getString("source"));
        }
        return invoiceEmailProcessInfoMap;
    }

    private boolean shouldTriggerEmailJob(LocalDate date,String cutOff, List<String> holidaysList) {

        String companyTimeZone = api.getCompany().getTimezone();
        LocalDate ld = LocalDate.now(ZoneId.of(companyTimeZone));  
        LocalDate nextDate = ld.plusDays(1);
        DayOfWeek day = DayOfWeek.of(ld.get(ChronoField.DAY_OF_WEEK));
        /* return false in case of below conditions met */
        /* If day of week is Saturday */
        /* If day of week is Sunday and next day is Holiday */
        /* If day of week is Sunday and trigger time is before cut off */
        /* If day of week is Holiday and trigger time is before cut off */
        /* If day of week is holiday and next day is Holiday as well */
        /* If day of week is holiday and next day is Saturday OR Sunday */
        return !((day == DayOfWeek.SATURDAY) ||
                (day == DayOfWeek.SUNDAY && CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(nextDate), holidaysList)) ||
                (day == DayOfWeek.SUNDAY && isBeforeCutOff(LocalDateTime.now(ZoneId.of(companyTimeZone)), cutOff))||
                (CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(ld), holidaysList) && isBeforeCutOff(LocalDateTime.now(ZoneId.of(companyTimeZone)), cutOff) )||
                (CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(ld), holidaysList) && CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(nextDate), holidaysList))||
                (CalendarUtils.isHoliday(DateConvertUtils.asUtilDate(ld), holidaysList) && CalendarUtils.isWeekend(DateConvertUtils.asUtilDate(nextDate))));

    }

    private boolean isBeforeCutOff(LocalDateTime currentDateTime, String cutOff) {

        Date cd = DateConvertUtils.getNow();
        LocalDate currentDate = DateConvertUtils.asLocalDate(cd);      
        DateTimeFormatter timeParser = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime cutOffTime = timeParser.parse(cutOff, LocalTime::from);
        LocalDateTime cutOffDateTime = LocalDateTime.of(currentDate, cutOffTime);
        return currentDateTime.isBefore(cutOffDateTime);
    }

    private void validateSPCInvoiceEmailDispatcherTaskWithCutOffTime(BillingProcessWS billingProcessWS, String source, Integer userId, Date jobTriggerDate) {
        waitFor(10);
        Map<String, Object> map = findInvoiceEmailProcessInfoByBillingId(billingProcessWS.getId());
        MetaFieldValueWS cutOffValueWS = api.getCompany().getMetaFieldByName(EMAIL_JOB_DEFAULT_CUT_OFF_TIME);
        String cutOff = StringUtils.EMPTY;
        if (null != cutOffValueWS && null != cutOffValueWS.getValue()) {
            cutOff = cutOffValueWS.getStringValue();
        }
        List<String> holidaysList = getHolidayList(1);
        Integer billingId = billingProcessWS.getId();
        boolean shouldTrigger = shouldTriggerEmailJob(DateConvertUtils.asLocalDate(jobTriggerDate), cutOff, holidaysList);
        logger.debug("shouldTrigger :: {} for billingId :: {}", shouldTrigger, billingId);
        if (!shouldTrigger) {
            assertNotNull("Billing process id can not be null ", billingId);
            assertNotNull("Billing process not finished", billingProcessWS.getProcessRuns().get(0).getFinished());
            assertEquals("Billing process not successful ", "Finished: successful", billingProcessWS.getProcessRuns().get(0).getStatusStr());
            Assert.assertTrue("Invoice email process should be empty", map.isEmpty());
        } else if (billingCount(billingId, "Billing Process") > 0) {
            assertNotNull("Billing process id can not be null ", billingProcessWS.getId());
            assertNotNull("Billing process not finished", billingProcessWS.getProcessRuns().get(0).getFinished());
            assertEquals("Billing process not successful ", "Finished: successful", billingProcessWS.getProcessRuns().get(0).getStatusStr());

            assertEquals("Estimated email count not matched ", 0, estimatedEmailCount(billingId, source));
            assertEquals("Sent email count not matched ", 0, sentEmailCount(billingId, source));
            assertEquals("Failed email count not matched ", 0, failedEmailCount(billingId, source));
        } else {
            assertNotNull("Billing process id can not be null ", billingProcessWS.getId());
            assertNotNull("Billing process not finished", billingProcessWS.getProcessRuns().get(0).getFinished());
            assertEquals("Billing process not successful ", "Finished: successful", billingProcessWS.getProcessRuns().get(0).getStatusStr());

            assertEquals("Estimated email count not matched ", billingCount(billingId, source) * actualInvoiceCount(billingId),
                    estimatedEmailCount(billingId, source));
            assertEquals("Sent email count not matched ", billingCount(billingId, source) * actualInvoiceCount(billingId),
                    sentEmailCount(billingId, source));
            assertEquals("Failed email count not matched ", 0, failedEmailCount(billingId, source));
        }
    }

    private void validateSPCInvoiceEmailDispatcherTaskWithDispatchAgainParameter(BillingProcessWS billingProcessWS, String source) {
        waitFor(10);
        Integer billingId = billingProcessWS.getId();
        assertNotNull("Billing process id can not be null ", billingId);
        assertNotNull("Billing process not finished", billingProcessWS.getProcessRuns().get(0).getFinished());
        assertEquals("Billing process not successful ", "Finished: successful", billingProcessWS.getProcessRuns().get(0).getStatusStr());
        logger.debug("billingId :: {}", billingId);
        
        int estimatedCount = estimatedEmailCount(billingId, source);
        int sentEmailCount  = sentEmailCount(billingId, source);
        
        if("Email Job".equals(source)) {
            
            estimatedCount = (billingCount(billingId, "Billing Process") * actualInvoiceCount(billingId)) - estimatedEmailCount(billingId, "Billing Process");
            sentEmailCount = (billingCount(billingId, "Billing Process") * actualInvoiceCount(billingId)) - sentEmailCount(billingId, "Billing Process") ;
            
            assertEquals("Estimated email count not matched ", estimatedEmailCount(billingId, source),estimatedCount);
            assertEquals("Sent email count not matched ", sentEmailCount(billingId, source),sentEmailCount);
            assertEquals("Failed email count not matched ", 0, failedEmailCount(billingId, source));
            
        } else {
            assertEquals("Estimated email count not matched ", (billingCount(billingId, source) * actualInvoiceCount(billingId)),estimatedCount);
            assertEquals("Sent email count not matched ", (billingCount(billingId, source) * actualInvoiceCount(billingId)),sentEmailCount);
            assertEquals("Failed email count not matched ", 0, failedEmailCount(billingId, source));
        }
    }

    private int billingCount(Integer billingId, String source) {
        String sql = "SELECT count(*) as count FROM invoice_email_process_info WHERE billing_process_id = ? and source = ?";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, billingId, source);
        return row.next() ? row.getInt("count") : 0;
    }

    private int actualInvoiceCount(Integer billingId) {
        String sql = "SELECT count(*) as count FROM invoice WHERE billing_process_id = ? ";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, billingId);
        return row.next() ? row.getInt("count") : 0;
    }

    private int estimatedEmailCount(Integer billingId, String source) {
        String sql = "SELECT sum(emails_estimated) as emails_estimated FROM invoice_email_process_info WHERE billing_process_id = ?  and source = ? GROUP BY billing_process_id";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, billingId, source);
        return row.next() ? row.getInt("emails_estimated") : 0;
    }

    private int sentEmailCount(Integer billingId, String source) {
        String sql = "SELECT sum(emails_sent) as emails_sent FROM invoice_email_process_info WHERE billing_process_id = ?  and source = ? GROUP BY billing_process_id";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, billingId, source);
        return row.next() ? row.getInt("emails_sent") : 0;
    }

    private int failedEmailCount(Integer billingId, String source) {
        String sql = "SELECT sum(emails_failed) as failed_emails FROM invoice_email_process_info WHERE billing_process_id = ?  and source = ? GROUP BY billing_process_id";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, billingId, source);
        return row.next() ? row.getInt("failed_emails") : 0;
    }

    private List<String> getHolidayList(Integer entityId) {
        String sql = "SELECT holiday_date" + " FROM route_%s_holiday_list";
        return jdbcTemplate.queryForList(String.format(sql, entityId), String.class);
    }

    private void validateBillingConfiguration(String skipDays, Integer skipCheck) {
        BillingProcessConfigurationWS ws = api.getBillingProcessConfiguration();
        assertEquals("skip emails days not updated ", skipDays, ws.getSkipEmailsDays());
        assertEquals("skip check not updated ", skipCheck, ws.getSkipEmails());
    }

    private void createDataTable() {
        String sql = "CREATE TABLE route_1_holiday_list (id integer NOT NULL,day character varying(255) NOT NULL,holiday_date character varying(255) NOT NULL)";
        jdbcTemplate.execute(sql);
    }

    public static boolean skipEmail(boolean shouldSkipEmails, Date billRunDate, List<String> skipEmailDaysList, List<String> holidaysList) {
        return shouldSkipEmails
                && (isSkipEmailDay(billRunDate, skipEmailDaysList) && (isWeekend(billRunDate) || isHoliday(billRunDate, holidaysList)));
    }

    public static boolean isSkipEmailDay(Date billRunDate, List<String> skipEmailDays) {
        return skipEmailDays.contains(String.valueOf(billRunDate.getDate()));
    }

    public static boolean isHoliday(Date billRunDate, List<String> holidaysList) {
        LocalDate date = DateConvertUtils.asLocalDate(billRunDate);
        String dateStr = getDateFormatted(date, DATE_FORMAT);
        return holidaysList.contains(dateStr);
    }

    public static boolean isWeekend(Date billRunDate) {
        LocalDate ld = DateConvertUtils.asLocalDate(billRunDate);
        DayOfWeek day = DayOfWeek.of(ld.get(ChronoField.DAY_OF_WEEK));
        return day == DayOfWeek.SUNDAY || day == DayOfWeek.SATURDAY;
    }

    public static String getDateFormatted(LocalDate date, String format) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(format);
        return dtf.format(date);
    }

    private void validateBillRunWithSkipEmails(BillingProcessWS billingProcessWS, String source, Integer userId) {
        waitFor(5);
        Map<String, Object> map = findInvoiceEmailProcessInfoByBillingId(billingProcessWS.getId());
        BillingProcessConfigurationWS billingProcesssConfig = api.getBillingProcessConfiguration();

        boolean skipEmails = billingProcesssConfig.getSkipEmails() == 1 ? true : false;
        logger.debug("skip emails flag value is # {}", skipEmails);
        List<String> skipEmailDaysList = Collections.emptyList();
        String skipEmailDays = billingProcesssConfig.getSkipEmailsDays();
        if (skipEmails) {
            skipEmailDaysList = Arrays.asList(skipEmailDays == null ? new String[0] : skipEmailDays.split(","));
        }
        List<String> holidaysList = getHolidayList(1);
        Integer billingId = billingProcessWS.getId();
        if (skipEmail(skipEmails, billingProcessWS.getBillingDate(), skipEmailDaysList, holidaysList)) {
            assertNotNull("Billing process id can not be null ", billingId);
            assertNotNull("Billing process not finished", billingProcessWS.getProcessRuns().get(0).getFinished());
            assertEquals("Billing process not successful ", "Finished: successful", billingProcessWS.getProcessRuns().get(0).getStatusStr());
            Assert.assertTrue("Invoice email process should be empty", map.isEmpty());
        } else {
            assertNotNull("Billing process id can not be null ", billingProcessWS.getId());
            assertNotNull("Billing process not finished", billingProcessWS.getProcessRuns().get(0).getFinished());
            assertEquals("Billing process not successful ", "Finished: successful", billingProcessWS.getProcessRuns().get(0).getStatusStr());

            assertEquals("Estimated email count not matched ", billingCount(billingId, source) * actualInvoiceCount(billingId),
                    estimatedEmailCount(billingId, source));
            assertEquals("Sent email count not matched ", billingCount(billingId, source) * actualInvoiceCount(billingId),
                    sentEmailCount(billingId, source));
            assertEquals("Failed email count not matched ", 0, failedEmailCount(billingId, source));
        }
    }

    private int getCutOffBillingId(int limit, int offset) {
        String sql = "SELECT id FROM billing_process ORDER BY id DESC LIMIT ? OFFSET ?";
        SqlRowSet row = jdbcTemplate.queryForRowSet(sql, limit, offset);
        return row.next() ? row.getInt("id") : 0;
    }
    
    private void markAllUsersDeleted() {
        logger.debug("Calling mark all user deleted...");
        String sql = "UPDATE base_user SET deleted=1 WHERE id IN (SELECT user_id FROM customer)";
        jdbcTemplate.execute(sql);
    }
}
