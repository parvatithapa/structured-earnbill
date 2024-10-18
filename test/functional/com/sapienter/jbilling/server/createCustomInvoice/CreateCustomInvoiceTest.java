package com.sapienter.jbilling.server.createCustomInvoice;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.JBillingTestUtils;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import static com.sapienter.jbilling.server.util.BetaCustomerConstants.*;
import static com.sapienter.jbilling.test.framework.builders.ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.fail;

@Test(groups = { "test-earnbill", "earnbill" }, testName = "CreateCustomInvoiceTest")
public class CreateCustomInvoiceTest {

    private static final Logger logger = LoggerFactory.getLogger(CreateCustomInvoiceTest.class);
    private static JbillingAPI api;
    private AccountTypeWS refAccountType;
    private Integer userId;
    private static final String TEST_CUSTOMER_CODE = "sam" + System.currentTimeMillis();
    private EnvironmentHelper envHelper;
    private Integer itemCategoryId;
    private static final String DU_USAGE_CATEGORY = "Beta Product Category";
    private static final String PRODUCT_ITEM_CODE_1 = "common";
    private static final String PRODUCT_ITEM_CODE_2 = "fragile";
    private static final String O_LINE_MONTH = "O,Shipments wise charges for standard services provided,table_type_month\n";
    private static final String O_LINE_WEEKDAY = "O,Special charges for shipments in peak season (In Weekdays),table_type_weekday\n";
    private static final String O_Line_WEEKEND_HOLIDAY = "O,Special charges for shipments in peak season (In Weekend / Holidays),table_type_weekend_holiday\n";
    private static final String O_LINE_SPEED_DELIVERY = "O,Special charges for shipments in peak season (fast delivery),table_type_speed_delivery\n";
    private  String invoiceDate= null;
    private  String invoiceDueDate= null;

    @BeforeClass
    public void initializeTests() throws Exception {

        api = JbillingAPIFactory.getAPI();
        refAccountType = api.getAccountType(1);
        AccountInformationTypeWS ait = api.getAccountInformationType(1);
        logger.debug("Initializing the data!");
        TestBuilder testBuilder = getTestEnvironment();
        final Date todaysDate = new Date();
        Calendar calendar = Calendar.getInstance();

        int singleMonth = calendar.get(Calendar.MONTH) + 1;

        invoiceDate= String.format("%d/%s/%d",calendar.get(Calendar.DAY_OF_MONTH),
                singleMonth<=9? "0"+singleMonth: singleMonth, calendar.get(Calendar.YEAR));
        calendar.add(Calendar.MONTH, 2);
        invoiceDueDate= String.format("%d/%s/%d",calendar.get(Calendar.DAY_OF_MONTH),
                singleMonth<=9? "0"+singleMonth: singleMonth, calendar.get(Calendar.YEAR));

        TestBuilder.newTest(false).givenForMultiple(envBuilder -> {
            // Creating mediated usage category
            itemCategoryId = buildAndPersistCategory(envBuilder, ait.getEntityId());
            logger.debug("Item category created {}", itemCategoryId);
            //create product
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_1);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_2);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_1+WD);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_2+WD);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_1+WH);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_2+WH);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_1+SD);
            createProductIfAbsent(envBuilder, PRODUCT_ITEM_CODE_2+SD);
            userId = createCustomer(envBuilder, refAccountType.getId(), todaysDate);
            logger.debug("user created {}", userId);
        });
    }

    @Test
    public void testTheCreateCustomInvoiceTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,").append(userId).append(",2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",996512,996513,996519\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",3\n")
                    .append(O_Line_WEEKEND_HOLIDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",4\n")
                    .append(O_LINE_SPEED_DELIVERY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",1\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",6");

            File tempFile = writeFile(String.valueOf(testData));
            Integer[] invoiceId = api.createCustomInvoice(tempFile);
            assertNotNull(invoiceId);
            for( Integer id : invoiceId ) {
                validateGeneratedInvoice(id, 8, 4);
            }

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught : " + e);
        }
    }

    @Test
    public void createCustomInvoiceWithDecimalQuantityTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,").append(userId).append(",2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",996512,996513,996519\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19.5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",3.7\n")
                    .append(O_Line_WEEKEND_HOLIDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",4.9\n");
            File tempFile = writeFile(String.valueOf(testData));
            Integer[] invoiceId = api.createCustomInvoice(tempFile);
            assertNotNull(invoiceId);
            for( Integer id : invoiceId ) {
                validateGeneratedInvoice(id, 6, 3);
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught : " + e);
        }
    }

    @Test
    public void createCustomInvoiceWithCharacterQuantityTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,").append(userId).append(",2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",996512,996513,996519\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",abd\n")
                    .append(O_Line_WEEKEND_HOLIDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",4\n")
                    .append(O_LINE_SPEED_DELIVERY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",1\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",6");
            File tempFile = writeFile(String.valueOf(testData));
            api.createCustomInvoice(tempFile);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "An exception occurred at Line 7. Product quantity should not be character");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createCustomInvoiceWithUnavailableProductTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,").append(userId).append(",2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",996512,996513,996519\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append("demo_product").append(",3\n")
                    .append(O_Line_WEEKEND_HOLIDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append("PRODUCT_ITEM_CODE_2").append(",4\n")
                    .append(O_LINE_SPEED_DELIVERY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",1\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",6");
            File tempFile = writeFile(String.valueOf(testData));
            api.createCustomInvoice(tempFile);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "No product found with product code: demo_product_wd");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createCustomInvoiceWithEmptyDataTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,").append(userId).append(",2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",996512,996513,996519\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",3\n")
                    .append("O,,table_type_weekend_holiday\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",4\n")
                    .append(O_LINE_SPEED_DELIVERY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",1\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",6");
            File tempFile = writeFile(String.valueOf(testData));
            api.createCustomInvoice(tempFile);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "An exception occurred at Line 8 .Please review the details ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createCustomInvoiceWithInvalidUserTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,1010000000,2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",996512,996513,996519\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",3\n")
                    .append(O_Line_WEEKEND_HOLIDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",4\n")
                    .append(O_LINE_SPEED_DELIVERY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",1\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",6");
            File tempFile = writeFile(String.valueOf(testData));
            api.createCustomInvoice(tempFile);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "User with id 1010000000 not found!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createCustomInvoiceWithoutSacCodeTest() {
        try {
            StringBuilder testData = new StringBuilder();
            testData.append("H,1010000000,2310198,").append(invoiceDate).append(",").append(invoiceDueDate).append(",,,\n")
                    .append(O_LINE_MONTH)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",19\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",10\n")
                    .append(O_LINE_WEEKDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",5\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",3\n")
                    .append(O_Line_WEEKEND_HOLIDAY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",2\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",4\n")
                    .append(O_LINE_SPEED_DELIVERY)
                    .append("L,").append(PRODUCT_ITEM_CODE_1).append(",1\n")
                    .append("L,").append(PRODUCT_ITEM_CODE_2).append(",6");
            File tempFile = writeFile(String.valueOf(testData));
            api.createCustomInvoice(tempFile);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "An exception occurred for SAC code .Please review the details ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void createCustomInvoiceWithoutOrderLinesTest() {
        try {
            String testData = "H," + userId + ",2310198,"+invoiceDate+","+invoiceDueDate+",996512,996513,996519";
            File tempFile = writeFile(testData);
            api.createCustomInvoice(tempFile);
            fail("Exception expected");
        } catch (SessionInternalError error) {
            JBillingTestUtils.assertContainsError(error, "No order Lines for respective data ");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private TestBuilder getTestEnvironment() {
        logger.debug("getting test environment!");
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
        });
    }

    //create customer
    private Integer createCustomer(TestEnvironmentBuilder envBuilder, Integer accountTypeId, Date nid) {
        logger.debug("creating the customer {}", TEST_CUSTOMER_CODE);
        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                .withUsername(TEST_CUSTOMER_CODE).withAccountTypeId(accountTypeId)
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

    private Integer buildAndPersistProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
                                           Integer categoryId, PriceModelWS priceModelWS) {
        logger.debug("creating product {}", code);
        return envBuilder.itemBuilder(api)
                .item()
                .useExactCode(true) // This is required 'true' for providing own product code.
                .withCode(code)
                .withType(categoryId)
                .withPriceModel(priceModelWS)
                .global(true)
                .allowDecimal(true)
                .build();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, Integer entityId) {
        logger.debug("creating category {}", DU_USAGE_CATEGORY);
        ItemTypeWS[] allItemCategoriesByEntityId = api.getAllItemCategoriesByEntityId(entityId);
        for( ItemTypeWS itemTypeWS : allItemCategoriesByEntityId ) {
            if( itemTypeWS.getDescription().equals(DU_USAGE_CATEGORY) ) {
                return itemTypeWS.getId();
            }
        }
        return envBuilder.itemBuilder(api)
                .itemType()
                .useExactCode(true)
                .withCode(DU_USAGE_CATEGORY)
                .withCategoryType(ORDER_LINE_TYPE_ITEM)
                .global(true)
                .build();
    }

    private static File writeFile(String testData) throws IOException {
        File tempFile = new File("/tmp/order.csv");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            writer.write(testData);
        }
        return tempFile;
    }

    private void createProductIfAbsent(TestEnvironmentBuilder envBuilder, String productCode) {
        if( null == api.getItemID(productCode) ) {
            Integer productId = buildAndPersistProduct(envBuilder, api, productCode, envBuilder.idForCode(DU_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(),
                            new BigDecimal("100").add(new BigDecimal(Math.random() * 100))
                                    .setScale(2, BigDecimal.ROUND_HALF_UP), api.getCallerCurrencyId()));
            logger.debug("Product created {}", productId);
        }
    }

    private void validateGeneratedInvoice(Integer id, Integer lineCount, Integer orderCount) {
        String customeInvoiceId = null;
        String sacCode = null;
        String tableType = null;
        String invoiceDate = null;
        String invoiceDueDate = null;
        InvoiceWS invoiceWS = api.getInvoiceWS(id);
        assertEquals(invoiceWS.getStatusDescr(), "Unpaid", "Created Invoice status should be Unpaid");
        assertEquals(invoiceWS.getUserId(), userId, "Invoice should be generated for provided user");
        assertEquals((Integer) invoiceWS.getInvoiceLines().length, lineCount,
                "Generated invoice line should match to provided invoice line");
        Integer[] orders = invoiceWS.getOrders();
        assertEquals((Integer) orders.length, orderCount,
                "Orders generated for invoice should be same as orders provided to file");
        Arrays.sort(orders);
        OrderWS orderWS = api.getOrder(orders[0]);
        assertEquals(orderWS.getPeriodStr(), "One time", "Order period for generated invoice should be one time");

        MetaFieldValueWS[] metaFieldValueWS = orderWS.getMetaFields();
        for( MetaFieldValueWS metaFieldValue : metaFieldValueWS ) {
            MetaFieldWS metaFieldWS = metaFieldValue.getMetaField();
            if( metaFieldWS.getName().equals(CUSTOM_INVOICE_NUMBER) ) {
                customeInvoiceId = metaFieldValue.getStringValue();
            }
            if( metaFieldWS.getName().equals("SAC 1") ) {
                sacCode = metaFieldValue.getStringValue();
            }
            if( metaFieldWS.getName().equals(TABLE_TYPE) ) {
                tableType = metaFieldValue.getStringValue();
            }
            if( metaFieldWS.getName().equals(INVOICE_DATE) ) {
                invoiceDate = metaFieldValue.getStringValue();
            }
            if( metaFieldWS.getName().equals(INVOICE_DUE_DATE) ) {
                invoiceDueDate = metaFieldValue.getStringValue();
            }
        }
        assertEquals(customeInvoiceId, "2310198", "Custom invoice number should be 2310198");
        assertEquals(sacCode, "996512", "SAC code should be 996512");
        assertEquals(tableType, TABLE_TYPE_MONTH, "Table type should be table_type_month");
        assertEquals(invoiceDate, invoiceDate, "Invoice date should be "+invoiceDate);
        assertEquals(invoiceDueDate, invoiceDueDate, "Invoice due date should be "+invoiceDueDate);

    }
}
