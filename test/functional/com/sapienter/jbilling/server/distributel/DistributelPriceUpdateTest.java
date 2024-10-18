package com.sapienter.jbilling.server.distributel;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CustomerNoteWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.DistributelTestConfig;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

/**
 *
 * @author Krunal Bhavsar
 *
 */
@Test(groups = { "test-distributel", "distributel" }, testName = "DistributelPriceUpdateTest")
@ContextConfiguration(classes = DistributelTestConfig.class)
public class DistributelPriceUpdateTest extends AbstractTestNGSpringContextTests {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TABEL_NAME = "price_update_detail";
    private static final Map<String, String> COLUMN_CONSTRAINST_MAP;
    private static final Map<String, String> COLUMN_DETAIL_MAP;
    private static final String INSERT_QUERY_TEMPLATE;
    private TestBuilder       testBuilder;
    private static final Integer CC_PM_ID                                      = 5;
    private static final String  ACCOUNT_NAME                                  = "Dist Test Account For Price Update";
    private static final String  PRODUCT_CATEGORY                              = "Dist Category";
    private static final String  TEST_PRODUCT                                  = "Dist Test Item";
    private static final String  TEST_USER_1                                   = "Test-User-"+ UUID.randomUUID().toString();
    private static final String  TEST_ORDER_1                                  = "Test-Order-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "Test-User-"+ UUID.randomUUID().toString();
    private static final String  TEST_ORDER_2                                  = "Test-Order-"+ UUID.randomUUID().toString();
    private static final int     MONTHLY_ORDER_PERIOD                          =  2;
    private static final int     NEXT_INVOICE_DAY                              =  1;
    private static final String  PRICE_UPDATE_PLUGNIN                          = "Plugin-"+ UUID.randomUUID().toString();
    private static final String  PARAM_TABLE_NAME                              = "data_table_name";
    private static final String  PARAM_CRON_EXPRESSION                         = "cron_exp";
    private static final String  PARAM_ORDER_LEVEL_META_FIELD_NAME             = "order_level_mf_name";
    private static final String  ORDER_INVOICE_META_FIELD_NAME                 = "Order Invoice Note";
    private EnvironmentHelper    environmentHelper;

    @Resource(name = "distJdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobExplorer jobExplorer;

    static {

        COLUMN_CONSTRAINST_MAP = new LinkedHashMap<>();
        COLUMN_DETAIL_MAP      = new LinkedHashMap<>();

        COLUMN_CONSTRAINST_MAP.put("id", "SERIAL NOT NUll");
        COLUMN_DETAIL_MAP.put("scheduled_date_for_adjustment", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("order_id", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("customer_id", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("product_id", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("new_order_line_price", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("invoice_note", "VARCHAR(255)");
        COLUMN_DETAIL_MAP.put("status", "VARCHAR(255)");
        COLUMN_CONSTRAINST_MAP.putAll(COLUMN_DETAIL_MAP);
        COLUMN_CONSTRAINST_MAP.put("PRIMARY KEY", " ( id ) ");

        INSERT_QUERY_TEMPLATE = new StringBuilder().append("INSERT INTO ")
                .append(TABEL_NAME)
                .append(" ")
                .append('(')
                .append(COLUMN_DETAIL_MAP.entrySet().stream().map(Entry::getKey).collect(Collectors.joining(",")))
                .append(')')
                .append(" VALUES (")
                .append(COLUMN_DETAIL_MAP.entrySet().stream().map(entry -> "?").collect(Collectors.joining(",")))
                .append(" )")
                .toString();

    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {});
    }

    @BeforeClass
    public void before() {
        createTable(TABEL_NAME, COLUMN_CONSTRAINST_MAP);
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            environmentHelper = EnvironmentHelper.getInstance(api);
            // Creating account type
            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);

            // Creating product category.
            buildAndPersistCategory(envBuilder, api, PRODUCT_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            buildAndPersistProduct(envBuilder, api, TEST_PRODUCT, false, envBuilder.idForCode(PRODUCT_CATEGORY), true, "0.00");

            // Creating order level meta field
            buildAndPersistMetafield(testBuilder, ORDER_INVOICE_META_FIELD_NAME, DataType.STRING, EntityType.ORDER);

            // Configure Price Update plug in.
            PluggableTaskTypeWS priceUpdateTask = api.getPluginTypeWSByClassName(DistributelPriceUpdateTask.class.getName());
            envBuilder.pluginBuilder(api)
            .withCode(PRICE_UPDATE_PLUGNIN)
            .withTypeId(priceUpdateTask.getId())
            .withOrder(12345678)
            .withParameter(PARAM_TABLE_NAME, TABEL_NAME)
            .withParameter(PARAM_ORDER_LEVEL_META_FIELD_NAME, ORDER_INVOICE_META_FIELD_NAME)
            .withParameter(PARAM_CRON_EXPRESSION, String.format("0 0 0 ? * * %s", LocalDate.now().getYear() + 1))
            .build();


        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
            assertNotNull("Product Categroy Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CATEGORY));
            assertNotNull("Product Creation Failed ", testEnvBuilder.idForCode(TEST_PRODUCT));
            assertNotNull("Plugin Creation Failed ", testEnvBuilder.idForCode(PRICE_UPDATE_PLUGNIN));
        });
    }


    @AfterClass
    public void after() {
       deleteTable(TABEL_NAME);
       testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
       testBuilder.removeEntitiesCreatedOnJBilling();
       testBuilder = null;
    }


    @Test
    public void test01SuccessUpdatePrice() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
        testBuilder.given(envBuilder -> {
            logger.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_1)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .build();

            Calendar activeSince = Calendar.getInstance();
            activeSince.setTime(nextInvoiceDate.getTime());
            activeSince.add(Calendar.MONTH, -1);

            logger.debug("Creating Order with Active Since Date {}",  activeSince.getTime());

            Integer orderId = envBuilder.orderBuilder(api)
                    .forUser(userWS.getId())
                    .withProducts(envBuilder.env().idForCode(TEST_PRODUCT))
                    .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                    .withActiveSince(activeSince.getTime())
                    .withEffectiveDate(activeSince.getTime())
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(1)
                    .withCodeForTests(TEST_ORDER_1)
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .build();

            DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
            DistributelPriceUpdateRequest request = DistributelPriceUpdateRequest.builder()
                    .scheduledDateForAdjustment(dateFormat.format(new Date()))
                    .orderId(orderId)
                    .customerId(userWS.getId())
                    .productId(envBuilder.env().idForCode(TEST_PRODUCT))
                    .newOrderLinePrice(BigDecimal.ONE)
                    .build();

            insertPriceRecord(request);

            Calendar futureDate = Calendar.getInstance();
            futureDate.add(Calendar.MONTH, DistributelPriceJobConstants.MONTH_TO_ADD);

            DistributelPriceUpdateRequest futureRequest = DistributelPriceUpdateRequest.builder()
                    .scheduledDateForAdjustment(dateFormat.format(futureDate.getTime()))
                    .orderId(orderId)
                    .customerId(userWS.getId())
                    .productId(envBuilder.env().idForCode(TEST_PRODUCT))
                    .newOrderLinePrice(BigDecimal.ONE)
                    .build();

            insertPriceRecord(futureRequest);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_1));
            assertNotNull("Order Creation Failed", testEnvBuilder.idForCode(TEST_ORDER_1));
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.triggerScheduledTask(testEnvBuilder.idForCode(PRICE_UPDATE_PLUGNIN), new Date());
            try {
                Thread.sleep(2000L); // waiting to start quartz job.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if(!isJobRunning(DistributelPriceJobConstants.PARAM_JOB_NAME)) {
                fail("Distributel Price update job failed!");
            }
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_1));
            assertNotNull("Order Not Found!", order);
            assertEquals("Order Amount Should be ", BigDecimal.ONE.intValue(), order.getTotalAsDecimal().intValue());

            Optional<MetaFieldValueWS> orderInvoiceNote = Arrays.stream(order.getMetaFields())
                    .filter(mfValue -> mfValue.getFieldName().equals(ORDER_INVOICE_META_FIELD_NAME))
                    .findFirst();
            if(!orderInvoiceNote.isPresent()) {
                fail("Order Invoice Note MetaField Not found!");
            }
            Integer passedRecordCount = findRecordCountByStatus(testEnvBuilder.idForCode(TEST_USER_1).toString(),
                    DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS);

            assertEquals("Pass Record Count Should be ", Integer.valueOf(1), passedRecordCount);

            UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_1));

            CustomerNoteWS[] notes = user.getCustomerNotes();

            assertNotNull("User Note creation step faild!", notes);
            assertEquals("Note size should be", 1, notes.length);

            String content = notes[0].getNoteContent();
            assertNotNull("Note Content Not Found!", content);

            if(!content.contains(order.getId() + "")) {
                fail("Note creation failed!");
            }

        });
    }

    @Test
    public void test02FailedUpdatePrice() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);


        testBuilder.given(envBuilder -> {
            logger.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_2)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .build();

            Calendar activeSince = Calendar.getInstance();
            activeSince.setTime(nextInvoiceDate.getTime());
            activeSince.add(Calendar.MONTH, -1);

            logger.debug("Creating Order with Active Since Date {}",  activeSince.getTime());

            Integer orderId = envBuilder.orderBuilder(api)
                    .forUser(userWS.getId())
                    .withProducts(envBuilder.env().idForCode(TEST_PRODUCT))
                    .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                    .withActiveSince(activeSince.getTime())
                    .withEffectiveDate(activeSince.getTime())
                    .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                    .withDueDateValue(1)
                    .withCodeForTests(TEST_ORDER_2)
                    .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                    .build();

            DateFormat dateFormat = new SimpleDateFormat("M/d/yyyy");
            DistributelPriceUpdateRequest request = DistributelPriceUpdateRequest.builder()
                    .scheduledDateForAdjustment(dateFormat.format(new Date()))
                    .orderId(orderId)
                    .customerId(userWS.getId())
                    .productId(envBuilder.env().idForCode(TEST_PRODUCT))
                    .newOrderLinePrice(BigDecimal.ONE)
                    .build();

            insertPriceRecord(request); //inserting valid record.

            request.setProductId(-1);
            insertPriceRecord(request); // inserting invalid record.

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_2));
            assertNotNull("Order Creation Failed", testEnvBuilder.idForCode(TEST_ORDER_2));
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.triggerScheduledTask(testEnvBuilder.idForCode(PRICE_UPDATE_PLUGNIN), new Date());
            try {
                Thread.sleep(2000L); // waiting to start quartz job.
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if(!isJobRunning(DistributelPriceJobConstants.PARAM_JOB_NAME)) {
                fail("Distributel Price update job failed!");
            }
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_2));
            assertNotNull("Order Not Found!", order);
            assertEquals("Order Amount Should be ", BigDecimal.ONE.intValue(), order.getTotalAsDecimal().intValue());

            Integer passedRecordCount = findRecordCountByStatus(testEnvBuilder.idForCode(TEST_USER_2).toString(),
                    DistributelPriceJobConstants.REUQUEST_SUCCESS_STATUS);

            assertEquals("Pass Record Count Should be ", Integer.valueOf(1), passedRecordCount);

            Integer failedRecordCount = findRecordCountByStatus(testEnvBuilder.idForCode(TEST_USER_2).toString(),
                    DistributelPriceJobConstants.REUQUEST_FAILED_STATUS);

            assertEquals("Failed Record Count Should be ", Integer.valueOf(1), failedRecordCount);

        });
    }

    private boolean isJobRunning(String jobName) {
        for(int i = 0; i <= 10; i++) {
            try {
                Set<JobExecution> executions = jobExplorer.findRunningJobExecutions(jobName);
                if(!executions.isEmpty()) {
                    Thread.sleep(1000L);
                } else {
                    return true;
                }
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                logger.error("Failed!", e);
                return false;
            }
        }
        return false;
    }

    private void createTable(String tableName, Map<String, String> columnDetails) {
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

    private void deleteTable(String tableName) {
        jdbcTemplate.execute("DROP TABLE "+ tableName);
    }

    private void insertPriceRecord(DistributelPriceUpdateRequest record) {
        try {
            jdbcTemplate.update(INSERT_QUERY_TEMPLATE, new Object[] {
                    record.getScheduledDateForAdjustment(),
                    record.getOrderId().toString(),
                    record.getCustomerId().toString(),
                    record.getProductId().toString(),
                    "$" + record.getNewOrderLinePrice().toString(),
                    "Test-"+System.currentTimeMillis(),""
            });
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed Insertion In data table "+ TABEL_NAME, ex);
        }
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global, ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, boolean allowDecimal, String rate) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(rate)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private Integer findRecordCountByStatus(String userId, String status) {
        try {
            String sql = "SELECT count(*) FROM " + TABEL_NAME + " WHERE customer_id = ? and status = ?";
            return jdbcTemplate.queryForObject(sql, Integer.class, userId, status);
        } catch(Exception ex) {
            logger.error("Error !", ex);
            fail("Failed findRecordCountByStatus",  ex);
            return -1;
        }
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value =  new MetaFieldBuilder()
                                .name(name)
                                .dataType(dataType)
                                .entityType(entityType)
                                .primary(true)
                                .build();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);

    }

}
