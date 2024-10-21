package com.sapienter.jbilling.server.invoiceline;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.usagePool.UsagePoolConsumptionActionWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.EnumerationWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;

@Test(groups = {"invoiceLineTaxSupport"}, testName = "InvoiceLineTaxSupportTest")
@ContextConfiguration(classes = TaxSupportTestConfig.class)
public class InvoiceLineTaxSupportTest extends AbstractTestNGSpringContextTests {

    private Logger logger = LoggerFactory.getLogger(InvoiceLineTaxSupportTest.class);

    private static final Integer PRANCING_PONY_ENTITY_ID                = 1;
    private static final Integer CC_PM_ID                               = 5;
    private static final String QUANTITY                                = "5";
    private static final String PRICE                                   = "5";
    private static final String ENUMERATION_METAFIELD_NAME              = "Tax Scheme";
    private static final String FUP_STANDARD_RATE                       = "Fup_Standard_rate-" + System.currentTimeMillis();
    private static final String DU_USAGE_CATEGORY                       = "DU Mediation Usage Category";
    private static final String TAX_DATE_FORMAT_VALUE                   = "dd-MM-yyyy";
    private static final String TAX_TABLE_NAME                          = "Tax Table Name";
    private static final String TAX_TABLE_VALUE                         = "route_1_taxes";
    private static final String TAX_DATE_FORMAT                         = "Tax Date Format";
    private static final String PLAN_WITH_TAX_SCHEME_2                  = "Plan with tax cheme - 2";
    private static final String PLAN_WITH_TAX_SCHEME_4                  = "Plan with tax cheme - 4";
    private static final String PLAN_WITH_TAX_SCHEME_6                  = "Plan with tax cheme - 6";
    private static final String PLAN_WITH_TAX_SCHEME_8                  = "Plan with tax cheme - 8";
    private static final String PLAN_WITH_TAX_SCHEME_10                 = "Plan with tax cheme - 10";
    private static final String NEGATIVE_AMOUNT_PLAN_AND_TAX_2          = "Plan with negative amount and tax 2";
    private static final String ACCOUNT_NAME                            = "DU Account Test";
    private static final String PRODUCT_ITEM_CODE_1                     = "DU-Product-Item_01";
    private static final String PRODUCT_ITEM_CODE_2                     = "DU-Product-Item_02";
    private static final String PRODUCT_ITEM_CODE_3                     = "DU-Product-Item_03";
    private static final String PRODUCT_ITEM_CODE_4                     = "DU-Product-Item_04";
    private static final String PRODUCT_ITEM_CODE_5                     = "DU-Product-Item_05";
    private static final String PRODUCT_WITH_NEGATIVE_AMOUNT            = "DU-Product-With_Negative_Amount";
    private static final String PLAN_ITEM_CODE_1                        = "DU-Plan-Item_01";
    private static final String PLAN_ITEM_CODE_2                        = "DU-Plan-Item_02";
    private static final String PLAN_ITEM_CODE_3                        = "DU-Plan-Item_03";
    private static final String PLAN_ITEM_CODE_4                        = "DU-Plan-Item_04";
    private static final String NEGATIVE_PLAN_ITEM_CODE                 = "DU-Negative_Plan-Item";

    private static final String TEST_CUSTOMER_CODE                         = "DU_" + System.currentTimeMillis();
    private static final String[] ENUMERATION_METAFIELD_VALUES_ARRAY    = {"Tax Scheme - 2", "Tax Scheme - 4", "Tax Scheme - 6",
                                                                            "Tax Scheme - 8", "Tax Scheme - 10"};
    private static List<String[]> TAX_SCHEME_RECORDS;
    private static DateTimeFormatter formatter;

    private static final String INSERT_QUERY_TEMPLATE;
    private Integer accountTypeId;
    private Integer planWithTaxScheme_2;
    private Integer planWithTaxScheme_4;
    private Integer planWithTaxScheme_6;
    private Integer planWithTaxScheme_8;
    private Integer planWithTaxScheme_10;
    private Integer negativePlanWithTaxScheme_2;
    private Integer userId;
    private TestBuilder testBuilder;
    private EnvironmentHelper envHelper;
    private static JbillingAPI api;
    private Integer itemCategoryId;
    private PreferenceWS tieredPricingPreference;

    private List<Integer> itemsToDelete = new ArrayList<>();
    private List<Integer> invoicesToDelete = new ArrayList<>();
    private List<Integer> ordersToDelete = new ArrayList<>();
    private List<Integer> usersToDelete = new ArrayList<>();

    @Resource(name = "taxSupportJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    private static final Map<String, String> COLUMN_CONSTRAINST_MAP;
    private static final Map<String, String> COLUMN_DETAIL_MAP;

    static {
        COLUMN_CONSTRAINST_MAP = new LinkedHashMap<>();
        COLUMN_DETAIL_MAP      = new LinkedHashMap<>();

        COLUMN_CONSTRAINST_MAP.put("id", "SERIAL NOT NUll");
        COLUMN_DETAIL_MAP.put("Description", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("Tax_Code", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("Start_Date", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("End_Date", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("Tax_Rate", "VARCHAR(255)");
        COLUMN_CONSTRAINST_MAP.putAll(COLUMN_DETAIL_MAP);
        COLUMN_CONSTRAINST_MAP.put("PRIMARY KEY", " ( id ) ");

        INSERT_QUERY_TEMPLATE = new StringBuilder().append("INSERT INTO ")
                .append(TAX_TABLE_VALUE)
                .append(" ")
                .append('(')
                .append(COLUMN_DETAIL_MAP.entrySet().stream().map(Entry::getKey).collect(Collectors.joining(",")))
                .append(')')
                .append(" VALUES (")
                .append(COLUMN_DETAIL_MAP.entrySet().stream().map(entry -> "?").collect(Collectors.joining(",")))
                .append(" )")
                .toString();

        formatter = DateTimeFormatter.ofPattern(TAX_DATE_FORMAT_VALUE);
        String todaysDateString = LocalDate.now().format(formatter);
        String startDateString = LocalDate.now().minusDays(10).format(formatter);
        String endDateString = LocalDate.now().plusDays(10).format(formatter);

        String[] record1 = {"Tax Scheme - 2", "SR_5%", startDateString, endDateString, "2"};
        String[] record2 = {"Tax Scheme - 4", "ZR_4%", todaysDateString, endDateString, "4"};
        String[] record3 = {"Tax Scheme - 6", "ZR_6%", startDateString, todaysDateString, "6"};
        String[] record4 = {"Tax Scheme - 8", "ZR_8%", startDateString, startDateString, "8"};
        String[] record5 = {"Tax Scheme - 10", "ZR_10%", endDateString, endDateString, "10"};
        TAX_SCHEME_RECORDS = Arrays.asList(record1, record2, record3, record4, record5);
    }

    @BeforeClass
    public void initializeTests(){
        logger.debug("Initializing the data!");
        testBuilder = getTestEnvironment();
        TestBuilder.newTest(false).givenForMultiple(envBuilder -> {

            //creating data table with name 'route_1_taxes'
            createTable(TAX_TABLE_VALUE, COLUMN_CONSTRAINST_MAP);

            //insert data into 'route_1_taxes'
            TAX_SCHEME_RECORDS.stream().forEach(this :: insertTaxRateDetails);

            //setting company level metafields
            buildAndPersistMetafield(testBuilder, TAX_TABLE_NAME, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, TAX_DATE_FORMAT, DataType.STRING, EntityType.COMPANY);

            //setting plan and product level metafields
            buildAndPersistMetafield(testBuilder, ENUMERATION_METAFIELD_NAME, DataType.ENUMERATION, EntityType.PLAN);
            buildAndPersistMetafield(testBuilder, ENUMERATION_METAFIELD_NAME, DataType.ENUMERATION, EntityType.PRODUCT);
            createEnumeration(ENUMERATION_METAFIELD_NAME, ENUMERATION_METAFIELD_VALUES_ARRAY);

            setCompanyLevelMetaField(testBuilder.getTestEnvironment());

            //Adding the tax rate preference
            tieredPricingPreference = api.getPreference(Constants.PREFERENCE_INVOICE_LINE_TAX);
            tieredPricingPreference.setValue("1");
            api.updatePreference(tieredPricingPreference);

            // Creating account type
            accountTypeId = buildAndPersistAccountType(envBuilder, ACCOUNT_NAME, CC_PM_ID);

            // Creating mediated usage category
            itemCategoryId = buildAndPersistCategory(envBuilder, DU_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            //Create usage products
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PRODUCT_ITEM_CODE_1, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("5"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[0]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PRODUCT_ITEM_CODE_2, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("7"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[1]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PRODUCT_ITEM_CODE_3, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("9"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[2]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PRODUCT_ITEM_CODE_4, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("9"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[2]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PRODUCT_ITEM_CODE_5, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[2]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PRODUCT_WITH_NEGATIVE_AMOUNT, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("-10"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[2]));

            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PLAN_ITEM_CODE_1, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("10"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[3]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PLAN_ITEM_CODE_2, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("11"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[4]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PLAN_ITEM_CODE_3, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("12"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[0]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, PLAN_ITEM_CODE_4, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("13"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[1]));
            itemsToDelete.add(buildAndPersistProduct(envBuilder, api, NEGATIVE_PLAN_ITEM_CODE, false, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("-10"), api.getCallerCurrencyId()), true,
                    ENUMERATION_METAFIELD_VALUES_ARRAY[0]));

            List<Integer> items = Arrays.asList(envBuilder.idForCode(PLAN_ITEM_CODE_1), envBuilder.idForCode(PLAN_ITEM_CODE_2));
            List<UsagePoolConsumptionActionWS> usagePoolConsumptionActions = new ArrayList<>();
            UsagePoolConsumptionActionWS actionWS = new UsagePoolConsumptionActionWS();
            actionWS.setPercentage("100");
            actionWS.setMediumType(NotificationMediumType.EMAIL);
            actionWS.setNotificationId("1");
            actionWS.setType(Constants.FUP_CONSUMPTION_NOTIFICATION);
            usagePoolConsumptionActions.add(actionWS);
            //Create usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder, FUP_STANDARD_RATE, "100", envBuilder.idForCode(DU_USAGE_CATEGORY), items,
                    usagePoolConsumptionActions);

            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 1970);
            pricingDate.set(Calendar.MONTH, 0);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            PlanItemWS planItem1 = buildPlanItem(envBuilder.idForCode(PLAN_ITEM_CODE_1), getOrCreateOrderPeriod(PeriodUnitDTO.MONTH), QUANTITY, PRICE, pricingDate.getTime());
            PlanItemWS planItem2 = buildPlanItem(envBuilder.idForCode(PLAN_ITEM_CODE_2), getOrCreateOrderPeriod(PeriodUnitDTO.MONTH), QUANTITY, PRICE, pricingDate.getTime());
            PlanItemWS planItem3 = buildPlanItem(envBuilder.idForCode(PLAN_ITEM_CODE_3), getOrCreateOrderPeriod(PeriodUnitDTO.MONTH), QUANTITY, PRICE, pricingDate.getTime());
            PlanItemWS negativePlanItem = buildPlanItem(envBuilder.idForCode(NEGATIVE_PLAN_ITEM_CODE), getOrCreateOrderPeriod(PeriodUnitDTO.MONTH), QUANTITY, "-5", pricingDate.getTime());

            planWithTaxScheme_2 = buildAndPersistPlan(envBuilder, PLAN_WITH_TAX_SCHEME_2, getOrCreateOrderPeriod(PeriodUnitDTO.MONTH),
                    envBuilder.idForCode(PRODUCT_ITEM_CODE_1), Arrays.asList(envBuilder.idForCode(FUP_STANDARD_RATE)), planItem1);
            setPlanLevelMetaField(testBuilder.getTestEnvironment(), planWithTaxScheme_2, ENUMERATION_METAFIELD_VALUES_ARRAY[0]);

            planWithTaxScheme_4 = buildAndPersistPlan(envBuilder, PLAN_WITH_TAX_SCHEME_4, getOrCreateOrderPeriod(PeriodUnitDTO.MONTH),
                    envBuilder.idForCode(PRODUCT_ITEM_CODE_2), Arrays.asList(envBuilder.idForCode(FUP_STANDARD_RATE)), planItem2);
            setPlanLevelMetaField(testBuilder.getTestEnvironment(), planWithTaxScheme_4, ENUMERATION_METAFIELD_VALUES_ARRAY[1]);

            planWithTaxScheme_6 = buildAndPersistPlan(envBuilder, PLAN_WITH_TAX_SCHEME_6, getOrCreateOrderPeriod(PeriodUnitDTO.MONTH),
                    envBuilder.idForCode(PRODUCT_ITEM_CODE_3), Arrays.asList(envBuilder.idForCode(FUP_STANDARD_RATE)), planItem1, planItem2);
            setPlanLevelMetaField(testBuilder.getTestEnvironment(), planWithTaxScheme_6, ENUMERATION_METAFIELD_VALUES_ARRAY[2]);

            planWithTaxScheme_8 = buildAndPersistPlan(envBuilder, PLAN_WITH_TAX_SCHEME_8, getOrCreateOrderPeriod(PeriodUnitDTO.MONTH),
                    envBuilder.idForCode(PRODUCT_ITEM_CODE_4), Arrays.asList(envBuilder.idForCode(FUP_STANDARD_RATE)), planItem1, planItem2, planItem3);
            setPlanLevelMetaField(testBuilder.getTestEnvironment(), planWithTaxScheme_8, ENUMERATION_METAFIELD_VALUES_ARRAY[3]);

            planWithTaxScheme_10 = buildAndPersistPlan(envBuilder, PLAN_WITH_TAX_SCHEME_10, getOrCreateOrderPeriod(PeriodUnitDTO.MONTH),
                    envBuilder.idForCode(PRODUCT_ITEM_CODE_5), Arrays.asList(envBuilder.idForCode(FUP_STANDARD_RATE)), planItem1);
            setPlanLevelMetaField(testBuilder.getTestEnvironment(), planWithTaxScheme_10, ENUMERATION_METAFIELD_VALUES_ARRAY[3]);

            negativePlanWithTaxScheme_2 = buildAndPersistPlan(envBuilder, NEGATIVE_AMOUNT_PLAN_AND_TAX_2, getOrCreateOrderPeriod(PeriodUnitDTO.MONTH),
                    envBuilder.idForCode(PRODUCT_WITH_NEGATIVE_AMOUNT), Arrays.asList(envBuilder.idForCode(FUP_STANDARD_RATE)), negativePlanItem);
            setPlanLevelMetaField(testBuilder.getTestEnvironment(), negativePlanWithTaxScheme_2, ENUMERATION_METAFIELD_VALUES_ARRAY[0]);
        });
    }

    private TestBuilder getTestEnvironment() {
        logger.debug("getting test environment!");
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
        });
    }

    /**
     * one order with the plan having Tax Scheme - 2 and other order with plan having Tax Scheme - 4
     *
     * Again plan items are having different Tax Schemes defined above
     */
    @Test(priority = 1, enabled = true)
    public void testScenario01() {
        logger.debug("test scenario no 1");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            Integer orderId1 = testEnvBuilder.orderBuilder(api)
                .forUser(userId)
                .withProducts(api.getPlanWS(planWithTaxScheme_2).getItemId())
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(todaysDate)
                .withEffectiveDate(todaysDate)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("Order -11")
                .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                .build();
            logger.debug("order created with {}", orderId1);
            ordersToDelete.add(orderId1);

            Integer orderId2 = testEnvBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withProducts(api.getPlanWS(planWithTaxScheme_4).getItemId())
                    .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                    .withActiveSince(todaysDate)
                    .withEffectiveDate(todaysDate)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withCodeForTests("Order -12")
                    .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                    .build();
            logger.debug("order created with {}", orderId2);
            ordersToDelete.add(orderId2);
        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        }).validate((testEnv, testEnvBuilder) -> {
            
        });
    }

    /**
     * One order with Tax Scheme - 4 and Other order with Tax Scheme - 10
     *
     * Again plan items are having different Tax Schemes defined above
     */
    @Test(priority = 2, enabled = true)
    public void testScenario02() {
        logger.debug("test scenario no 2");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            Integer orderId1 = testEnvBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withProducts(api.getPlanWS(planWithTaxScheme_4).getItemId())
                    .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                    .withActiveSince(todaysDate)
                    .withEffectiveDate(todaysDate)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withCodeForTests("Order -21")
                    .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                    .build();
            logger.debug("order created with {}", orderId1);
            ordersToDelete.add(orderId1);

            Integer orderId2 = testEnvBuilder.orderBuilder(api)
                .forUser(userId)
                .withProducts(api.getPlanWS(planWithTaxScheme_10).getItemId())
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(todaysDate)
                .withEffectiveDate(todaysDate)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("Order -22")
                .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                .build();
            logger.debug("order created with {}", orderId2);
            ordersToDelete.add(orderId2);
        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        });
    }

    /**
     * Order with plan having Tax Scheme - 6 and discount of 10
     *
     * Again plan items are having different Tax Schemes defined above
     */
    @Test(priority = 3, enabled = true)
    public void testScenario03() {
        logger.debug("test scenario no 3");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            String code = "OTD" + System.currentTimeMillis();
            Integer discountId = testEnvBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withDescription(code)
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_AMOUNT.name())
                    .withDescription(code)
                    .build();

            DiscountLineWS discountLineWS = testEnvBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .build();

            Integer orderId = testEnvBuilder.orderBuilder(api)
                .forUser(userId)
                .withProducts(api.getPlanWS(planWithTaxScheme_6).getItemId())
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(todaysDate)
                .withEffectiveDate(todaysDate)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDiscountLine(discountLineWS)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("Order -3")
                .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                .build();
            logger.debug("order created with {}", orderId);
            ordersToDelete.add(orderId);
        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        });
    }

    /**
     * Order with plan having Tax Scheme - 6 and negative amount
     *
     * Again plan items are having different Tax Schemes defined above
     */
    @Test(priority = 4, enabled = true)
    public void testScenario04() {
        logger.debug("test scenario no 4");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            Integer orderId = testEnvBuilder.orderBuilder(api)
                .forUser(userId)
                .withProducts(api.getPlanWS(negativePlanWithTaxScheme_2).getItemId())
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(todaysDate)
                .withEffectiveDate(todaysDate)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("Order -4")
                .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                .build();
            logger.debug("order created with {}", orderId);
            ordersToDelete.add(orderId);
        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        });
    }

    /**
     * Order with one item and other is having two items.
     *
     * The item-1 is having Tax Scheme - 6 and item-2 is having Tax Scheme - 10
     */
    @Test(priority = 5, enabled = true)
    public void testScenario05() {
        logger.debug("test scenario no 5");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            Integer itemId1 = testEnvBuilder.itemBuilder(api)
                    .item()
                    .withCode("test-item-1").global(false)
                    .withMetaField(ENUMERATION_METAFIELD_NAME, ENUMERATION_METAFIELD_VALUES_ARRAY[2])
                    .withType(itemCategoryId)
                    .withFlatPrice("0.50")
                    .build();
            Integer itemId2 = testEnvBuilder.itemBuilder(api)
                    .item()
                    .withCode("test-item-2").global(false)
                    .withMetaField(ENUMERATION_METAFIELD_NAME, ENUMERATION_METAFIELD_VALUES_ARRAY[4])
                    .withType(itemCategoryId)
                    .withFlatPrice("1.00")
                    .build();
            Integer orderId1 = testEnvBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withProducts(itemId1)
                    .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                    .withActiveSince(todaysDate)
                    .withEffectiveDate(todaysDate)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(1)
                    .withCodeForTests("code")
                    .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                    .build();
            logger.debug("order created with {}", orderId1);
            ordersToDelete.add(orderId1);

            Integer orderId2 = testEnvBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withProducts(itemId1, itemId2)
                    .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                    .withActiveSince(todaysDate)
                    .withEffectiveDate(todaysDate)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(1)
                    .withCodeForTests("code")
                    .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                    .build();
            logger.debug("order created with {}", orderId2);
            ordersToDelete.add(orderId2);
        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        });
    }

    /**
     * order with zero order lines
     *
     */
    @Test(priority = 6, enabled = true)
    public void testScenario06() {
        logger.debug("test scenario no 6");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            Integer orderId = testEnvBuilder.orderBuilder(api)
                    .forUser(userId)
                    .withProducts(api.getPlanWS(negativePlanWithTaxScheme_2).getItemId())
                    .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                    .withActiveSince(todaysDate)
                    .withEffectiveDate(todaysDate)
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(Integer.valueOf(1))
                    .withCodeForTests("Order -4")
                    .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                    .build();
                logger.debug("order created with {}", orderId);
                ordersToDelete.add(orderId);

                OrderWS orderWS = api.getOrder(orderId);
                Arrays.asList(orderWS.getOrderLines()).stream().forEach(line -> {
                    line.setDeleted(1);
                    api.updateOrderLine(line);
                });

        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        });
    }

    /**
     * Order with plan having Tax Scheme - 6 and discount of 10%
     *
     * Again plan items are having different Tax Schemes defined above
     */
    @Test(priority = 7, enabled = true)
    public void testScenario07() {
        logger.debug("test scenario no 7");
        final Date todaysDate = new Date();

        testBuilder.given(envBuilder -> {
            userId = createCustomer(envBuilder, TEST_CUSTOMER_CODE, accountTypeId, todaysDate);
            logger.debug("user created {}", userId);
            usersToDelete.add(userId);

        }).validate((testEnv, testEnvBuilder) -> {
            String code = "OTD" + System.currentTimeMillis();
            Integer discountId = testEnvBuilder.discountBuilder(api)
                    .withCodeForTests(code)
                    .withDescription(code)
                    .withRate("10")
                    .withType(DiscountStrategyType.ONE_TIME_PERCENTAGE.name())
                    .withDescription(code)
                    .build();

            DiscountLineWS discountLineWS = testEnvBuilder.discountBuilder(api).
                    dicountLine()
                    .withDiscountId(discountId)
                    .withDescription("TestDescription" + System.currentTimeMillis())
                    .build();

            Integer orderId = testEnvBuilder.orderBuilder(api)
                .forUser(userId)
                .withProducts(api.getPlanWS(planWithTaxScheme_6).getItemId())
                .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                .withActiveSince(todaysDate)
                .withEffectiveDate(todaysDate)
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDiscountLine(discountLineWS)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("Order -7")
                .withPeriod(getOrCreateOrderPeriod(PeriodUnitDTO.MONTH))
                .build();
            logger.debug("order created with {}", orderId);
            ordersToDelete.add(orderId);
        }).validate((testEnv, testEnvBuilder) -> {

            invoicesToDelete.addAll(Arrays.asList(api.createInvoiceWithDate(userId, todaysDate, null, null, false)));

            Arrays.asList(api.getAllInvoicesForUser(userId))
                        .stream()
                        .filter(invoice -> null != invoice.getInvoiceLines())
                        .forEach(invoice -> {
                            Arrays.asList(invoice.getInvoiceLines()).stream()
                            .filter(line -> line != null && null != line.getItemId())
                            .forEach(invoiceLine -> {
                                ItemDTOEx itemDTOEx = api.getItem(invoiceLine.getItemId(), userId, null);
                                String taxScheme[] = {""};
                                if(itemDTOEx.getIsPlan()) {
                                    Integer[] plans = api.getPlansBySubscriptionItem(itemDTOEx.getId());
                                    Arrays.asList(api.getPlanWS(plans[0]).getMetaFields())
                                                .stream()
                                                .filter(mf -> null != mf)
                                                .forEach(mf -> {
                                                    if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                                        taxScheme[0] = mf.getValue().toString();
                                                    }
                                                });
                                } else {
                                    Arrays.asList(itemDTOEx.getMetaFields())
                                    .stream()
                                    .filter(mf -> (null != mf) && (null != mf.getValue()) )
                                    .forEach(mf -> {
                                        if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                                            taxScheme[0] = mf.getValue().toString();
                                        }
                                    });
                                }

                                BigDecimal expectedGrossAmount = invoiceLine.getAmountAsDecimal().subtract(invoiceLine.getTaxAmountAsDecimal());

                                assertEquals(invoiceLine.getTaxRateAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), getExpectedTaxRate(taxScheme[0], todaysDate).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Tax rate is wrong");
                                assertEquals(invoiceLine.getTaxAmountAsDecimal(), getTaxAmount(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()), "Wrong tax amount!");
                                assertEquals(invoiceLine.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), calculateAmountWithTax(invoiceLine.getGrossAmountAsDecimal(), invoiceLine.getTaxRateAsDecimal()).setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Amount is wrong with the invoice line.");
                                assertEquals(invoiceLine.getGrossAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP), expectedGrossAmount.setScale(2, BigDecimal.ROUND_HALF_UP),
                                        "Wrong gross amount!");
                            });
                        });
        });
    }

    private void createTable(String tableName, Map<String, String> columnDetails) {
        logger.debug("creating the table {}", tableName);
        try {
            String createTableQuery = "CREATE TABLE "+ tableName;
            StringBuilder columnBuilder = new StringBuilder().append(" (");

            columnBuilder.append(columnDetails.entrySet().stream()
                    .map(entry -> entry.getKey() + " " + entry.getValue())
                    .collect(Collectors.joining(",")));
            columnBuilder.append(" )");
            jdbcTemplate.execute(createTableQuery + columnBuilder.toString());
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed During table creation ", ex);
        }
    }

    private void dropTable(String tableName) {
        logger.debug("droping the table {}", tableName);
        jdbcTemplate.execute("DROP TABLE "+ tableName);
    }

    private void insertTaxRateDetails(String[] record) {
        logger.debug("inserting the tax rate details to table!");
        try {
            jdbcTemplate.update(INSERT_QUERY_TEMPLATE, new Object[] {
                    record[0], record[1], record[2], record[3], record[4]
            });
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed Insertion In data table "+ TAX_TABLE_NAME, ex);
        }
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        logger.debug("creating the metafield {}", name);
        MetaFieldWS value =  new MetaFieldBuilder()
                                .name(name)
                                .dataType(dataType)
                                .entityType(entityType)
                                .primary(true)
                                .build();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return id;

    }

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        logger.debug("setting the company level meta fields!");
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));
        values.add(new MetaFieldValueWS(TAX_TABLE_NAME, null, DataType.STRING, true, TAX_TABLE_VALUE));
        values.add(new MetaFieldValueWS(TAX_DATE_FORMAT, null, DataType.STRING, true, TAX_DATE_FORMAT_VALUE));
        int entityId = api.getCallerCompanyId();
        logger.debug("Created Company Level MetaFields {}", values);
        values.forEach(value -> {
            value.setEntityId(entityId);
        });
        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updateCompany(company);

    }

    private void setPlanLevelMetaField(TestEnvironment environment, Integer planId, String taxScheme) {
        logger.debug("setting the plan level metafields for plan {}", planId);
        PlanWS plan = api.getPlanWS(planId);
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(plan.getMetaFields()).collect(Collectors.toList()));
        Arrays.asList(plan.getMetaFields()).forEach(mf -> {
            if(mf.getFieldName().equals(ENUMERATION_METAFIELD_NAME)) {
                mf.setValue(taxScheme);
                values.add(mf);
            }
        });
        values.forEach(value -> {
            value.setEntityId(api.getCallerCompanyId());
        });
        plan.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
        api.updatePlan(plan);

    }

    private Integer createEnumeration(String name, String... values){
        logger.debug("creating the enumeration {}", name);
        EnumerationWS enumerationWS2 = api.getEnumerationByName(name);
        if(enumerationWS2==null) {
            EnumerationWS enumerationWS = new EnumerationWS(name);
            enumerationWS.setEntityId(PRANCING_PONY_ENTITY_ID);

            for (String value : values) {
                enumerationWS.addValue(value);
            }

            return api.createUpdateEnumeration(enumerationWS);
        }  else {
            return enumerationWS2.getId();
        }
    }

    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, String code, String quantity,Integer categoryId,
            List<Integer>  items, List<UsagePoolConsumptionActionWS> consumptionActions) {
        logger.debug("creating usage pool {} ", code);
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
                .withQuantity(quantity)
                .withResetValue("Reset To Initial Value")
                .withItemIds(items)
                .addItemTypeId(categoryId)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_BILLING_PERIODS)
                .withCyclePeriodValue(Integer.valueOf(1)).withName(code)
                .withConsumptionActions(consumptionActions)
                .build();
    }

    private PlanItemWS buildPlanItem(Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {
        logger.debug("creating plan item {} ", itemId);
        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .addModel(pricingDate, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .withBundledPeriodId(periodId)
                .withBundledQuantity(quantity)
                .build();
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, String name,
            Integer ...paymentMethodTypeId) {
        logger.debug("creating account type {} ", name);
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, String code, boolean global,
            ItemBuilder.CategoryType categoryType) {
        logger.debug("creating category {}", code);
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, PriceModelWS priceModelWS, boolean allowDecimal, String metaFieldValue) {
        logger.debug("creating product {}", code);
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withPriceModel(priceModelWS)
                .withMetaField(ENUMERATION_METAFIELD_NAME, metaFieldValue)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, String code, Integer periodId, Integer itemId,
            List<Integer> usagePools, PlanItemWS... planItems) {
        logger.debug("creating the plan {}", code);
        return envBuilder.planBuilder(api, code)
                .withDescription(code)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }

    private Integer getOrCreateOrderPeriod(int periodUnit){
        logger.debug("getting or creting the order period {}", periodUnit);
        Integer orderPeriodWS = null;
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(periodUnit == period.getPeriodUnitId() && period.getValue()  == 1){
                orderPeriodWS = period.getId();
            }
        }

        if (orderPeriodWS == null) {
            orderPeriodWS = createPeriod(periodUnit);
        }

        return orderPeriodWS;
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
        logger.debug("creting the customer {}", code);
        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                .withUsername(code).withAccountTypeId(accountTypeId)
                .withMainSubscription(new MainSubscriptionWS(envHelper.getOrderPeriodMonth(api), getDay(nid)));

        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(nid);
        nextInvoiceDate.add(Calendar.MONTH, 1);
        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
        return user.getId();
    }

    private Integer getDay(Date inputDate) {
        logger.debug("getting the day {}", inputDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }

    private Integer createPeriod(int periodUnit) {
        logger.debug("creating the period");
        if (PeriodUnitDTO.MONTH == periodUnit) {
            OrderPeriodWS monthly = new OrderPeriodWS();
            monthly.setEntityId(api.getCallerCompanyId());
            monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);
            monthly.setValue(1);
            monthly.setDescriptions(Arrays
                    .asList(new InternationalDescriptionWS(
                            Constants.LANGUAGE_ENGLISH_ID, "MONTHLY")));
            return api.createOrderPeriod(monthly);
        }
        return null;
    }

    private BigDecimal calculateAmountWithTax(BigDecimal amount, BigDecimal taxRate) {
        logger.debug("calculating the amount with tax {} {}", amount, taxRate);
        return amount != null || taxRate != null ?
                amount.add(getTaxAmount(amount, taxRate)) : amount;
    }

    private BigDecimal getTaxAmount(BigDecimal grossAmount, BigDecimal taxRate) {
        logger.debug("getting the tax amount with {} {}", grossAmount, taxRate);
        return grossAmount != null && taxRate != null ?
                grossAmount.multiply(taxRate).divide(BigDecimal.valueOf(100L), Constants.BIGDECIMAL_SCALE,
                        Constants.BIGDECIMAL_ROUND) : BigDecimal.ZERO;
    }

    public BigDecimal getExpectedTaxRate(String taxScheme, Date invoiceGenerationDate) {
        logger.debug("getting expected tax rate with tax scheme {}", taxScheme);
        if(null == taxScheme)
            return BigDecimal.ZERO;
        BigDecimal[] taxRate = {BigDecimal.ZERO};

        LocalDate dateToCompare = invoiceGenerationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        TAX_SCHEME_RECORDS.stream().forEach(arr -> {
            if (taxScheme.equals(arr[0]) &&
                ((dateToCompare.equals(LocalDate.parse(arr[2], formatter)) || dateToCompare.equals(LocalDate.parse(arr[3], formatter))) ||
                     (dateToCompare.isAfter(LocalDate.parse(arr[2], formatter)) && dateToCompare.isBefore(LocalDate.parse(arr[3], formatter))))) {
                taxRate[0] =  new BigDecimal((String)arr[4]);
            }
        });
            return taxRate[0];
    }

    @AfterClass
    public void cleanUp() {
        logger.debug("cleaning the data after test!");

        dropTable(TAX_TABLE_VALUE);

        invoicesToDelete.forEach(api :: deleteInvoice);

        ordersToDelete.forEach(api :: deleteOrder);

        usersToDelete.forEach(api :: deleteUser);

        itemsToDelete.stream().forEach(api :: deleteItem);

        tieredPricingPreference.setValue("0");
        api.updatePreference(tieredPricingPreference);

        api = null;
    }
}
