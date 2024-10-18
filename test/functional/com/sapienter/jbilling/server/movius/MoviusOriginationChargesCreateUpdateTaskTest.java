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

package com.sapienter.jbilling.server.movius;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.WSTest;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;


import static org.junit.Assert.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Aadil Nazir
 * @since 11-10-2017
 */
@Test(groups = { "test-movius", "movius" }, testName = "MoviusOriginationChargesCreateUpdateTaskTest")
public class MoviusOriginationChargesCreateUpdateTaskTest {

	public final String TAG_NAME_CHARGES = "charges";
    public final String TAG_NAME_ORG_ID = "org-id";
    public final String TAG_NAME_COUNT = "count";
    public final String TAG_NAME_NAME = "name";
    public static final String ORIGINATION_XML = "Origination.xml";
    public static final String ANVEO_AUSTRALIA = "Anveo-Australia";
    public static final String TATA_UK = "Tata-UK";
    public static final String SUBSCRIPTION_ITEM = "Subscription-Item";
    public static final String MOVIUS_ORIGINATION_PLUGIN_CODE = "Movis-Origination-Plugin";
    public static final String MOVIUS_ORIGINATION_ITEM_TYPE_CODE = "Movis-Origination-item-type";
    public static final String MOVIUS_ORIGINATION_ACCOUNT_TYPE = "Movis-Origination-account-type";
	private static final Logger LOGGER = LoggerFactory.getLogger(MoviusOriginationChargesCreateUpdateTaskTest.class);
    private static TestBuilder testBuilder;
    private static Integer subscriptionItemId;
    private static Integer userId;
    private static Integer monthlyPeriodId;
    private static final String ORDER_CODE = "Order Code for Subscription Order";
    private static final Integer PRANCING_PONY = 1;
    private static final String BASE_DIR = Util.getSysProp("base_dir");
    private static final String MOVIUS_TEST = "movius-test";
    private static final String XSD_DIR = "xsd";
    private static final String ORIGINATION_DIR = concatenateString(BASE_DIR, MoviusConstants.ORG_DIR);
    private static final String ORIGINATION_XSD_PATH = concatenateString(BASE_DIR, MOVIUS_TEST, File.separator, XSD_DIR);
    private static final String ORIGINATION_PRANCING_PONY_DIR = concatenateString(ORIGINATION_DIR, File.separator, PRANCING_PONY.toString());
    private String MoviusOriginationChargesCreateUpdateTaskName = "com.sapienter.jbilling.server.order.task.MoviusOriginationChargesCreateUpdateTask";
    private static final String ORG_ID_MF_NAME = "Org Id";
    private final static String  META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER = "Set Item Id for Org Hierarchy Order";
    private final static Integer CC_PM_ID = 5;
    private final static Integer PLUGIN_ORDER = 77;
    private final static Integer ALLOW_ASSET_MANAGEMENT = 0;

    public final String BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG = concatenateString("<origination-charges system-id='Prancing Pony'>\n",
            "\t<provider>\n",
            "\t\t<name>Anveo</name>\n",
            "\t\t<country>\n",
            "\t\t\t<name>Australia</name>\n",
            "\t\t\t<charges></charges>\n",
            "\t\t\t<org-mapping>\n",
            "\t\t\t\t<org>\n",
            "\t\t\t\t\t<org-id></org-id>\n",
            "\t\t\t\t\t<count></count>\n",
            "\t\t\t\t</org>\n",
            "\t\t\t</org-mapping>\n",
            "\t\t</country>\n",
            "\t</provider>\n",
            "</origination-charges>");

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {});
    }

    public static String concatenateString (String... strings) {
        StringBuilder builder = new StringBuilder();
        for (String arg : strings) {
            builder.append(arg);
        }
        return builder.toString();
    }

    private void createItemType(final boolean global){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.itemBuilder(api)
                    .itemType()
                    .withCode(MOVIUS_ORIGINATION_ITEM_TYPE_CODE)
                    .useExactCode(true)
                    .global(global)
                    .allowAssetManagement(ALLOW_ASSET_MANAGEMENT)
                    .withEntities(new Integer[]{PRANCING_PONY})
                    .build();
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Item Type Creation Failed", testEnvBuilder.idForCode(MOVIUS_ORIGINATION_ITEM_TYPE_CODE));
        });

    }

    protected static void createItem(boolean global, String code){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.itemBuilder(api)
                    .item()
                    .withCode(code)
                    .allowDecimal(false)
                    .global(global)
                    .useExactCode(true)
                    .withCompany(PRANCING_PONY)
                    .withEntities(new Integer[]{PRANCING_PONY})
                    .withFlatPrice("5.00")
                    .withType(envBuilder.idForCode(MOVIUS_ORIGINATION_ITEM_TYPE_CODE))
                    .build();
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Item Creation Failed", testEnvBuilder.idForCode(code));
        });
    }

    protected static void createSubscriptionItem(boolean global){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            subscriptionItemId = envBuilder.itemBuilder(api)
                    .item()
                    .withCode(SUBSCRIPTION_ITEM)
                    .allowDecimal(false)
                    .global(global)
                    .withFlatPrice("5.00")
                    .withType(envBuilder.idForCode(MOVIUS_ORIGINATION_ITEM_TYPE_CODE))
                    .build();
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Item Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_ITEM));
        });
    }


    private String replaceTagWithProperValue(String xmlValue, String tagName, String initialValue, String value){

        String toReplace = null == initialValue ? concatenateString("<",tagName,"></",tagName,">")
                : concatenateString("<",tagName,">", initialValue ,"</",tagName,">");
        String replaceWith = concatenateString("<",tagName,">",value,"</",tagName,">");
        return xmlValue.replace(toReplace, replaceWith);
    }

    private boolean createRequiredFolders () {
        return new File(ORIGINATION_PRANCING_PONY_DIR).mkdirs();
    }

    private boolean createFile(String fileName, String data) {
        try {
            FileOutputStream outputStream = new FileOutputStream(fileName);
            byte[] strToBytes = data.getBytes();
            outputStream.write(strToBytes);
        } catch (Exception ex) {
            LOGGER.error("Exception : {}", ex);
            return false;
        }
        return true;
    }

    private boolean createXmlFile (String fileName, String data) {

        String defaultFileName = (null == fileName || fileName.trim().isEmpty()) ? ORIGINATION_XML : fileName;
        File path = new File(concatenateString(ORIGINATION_PRANCING_PONY_DIR, File.separator, defaultFileName));
        return createFile(path.getAbsolutePath(), data);

    }

    private void schedulePlugin(JbillingAPI api, TestEnvironmentBuilder envBuilder) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 1);
        api.triggerScheduledTask(envBuilder.idForCode(MOVIUS_ORIGINATION_PLUGIN_CODE), calendar.getTime());
    }

    private void createMetaField () {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.configurationBuilder(api)
                    .addMetaField(ORG_ID_MF_NAME, DataType.STRING, EntityType.CUSTOMER)
                    .build();
        });
    }

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        JbillingAPI api = environment.getPrancingPonyApi();
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));

        values.add(new MetaFieldValueWS(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER, null, DataType.STRING, true,
                environment.idForCode(SUBSCRIPTION_ITEM).toString()));
        int entityId = api.getCallerCompanyId();
        LOGGER.debug("Created Company Level MetaFields {}", values);
        values.forEach(value -> {
            value.setEntityId(entityId);
        });
        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[values.size()]));
        api.updateCompany(company);

    }

    private void createAccountType () {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.accountTypeBuilder(api)
                    .withName(MOVIUS_ORIGINATION_ACCOUNT_TYPE)
                    .withPaymentMethodTypeIds(new Integer[]{CC_PM_ID})
                    .build();
        });
    }

    private void setupFilesAndFolderForMovius () {
        // create resources/origination/1 folder
        createRequiredFolders();
    }

    private void setupMoviusData () {
        setupFilesAndFolderForMovius();
        createMetaField();
        createAccountType();
        createItemType(true);
        createItem(true, ANVEO_AUSTRALIA);
        createItem(true, TATA_UK);
        createSubscriptionItem(true);
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            buildAndPersistMetafield(testBuilder, META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER, DataType.STRING, EntityType.COMPANY);
            setCompanyLevelMetaField(testBuilder.getTestEnvironment());
            PluggableTaskTypeWS MoviusOriginationChargesCreateUpdateTaskType = api.getPluginTypeWSByClassName(MoviusOriginationChargesCreateUpdateTaskName);
            envBuilder.pluginBuilder(api)
                    .withCode(MOVIUS_ORIGINATION_PLUGIN_CODE)
                    .withTypeId(MoviusOriginationChargesCreateUpdateTaskType.getId())
                    .withOrder(PLUGIN_ORDER)
                    .withParameter(MoviusConstants.ORIGINATION_XML_PARAMETER_BASE_DIR, ORIGINATION_PRANCING_PONY_DIR)
                    .withParameter(MoviusConstants.ORIGINATION_XSD_PARAMETER_BASE_DIR, ORIGINATION_XSD_PATH)
                    .build();
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER));
            assertNotNull("Plugin Creation Failed", testEnvBuilder.idForCode(MOVIUS_ORIGINATION_PLUGIN_CODE));
        });
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        MetaFieldValueWS[] metaFieldValueWSArray = api.getCompany().getMetaFields();
        MetaFieldValueWS subscriptionMetaField = Arrays.stream(metaFieldValueWSArray).filter(metaFieldValueWS -> metaFieldValueWS.getFieldName().equals(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER)).findFirst().orElse(null);
        Integer id = null;
        if(null == subscriptionMetaField){
            LOGGER.debug("Creating subscription metaField {}", META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER);
            MetaFieldWS value =  new MetaFieldBuilder()
                    .name(name)
                    .dataType(dataType)
                    .entityType(entityType)
                    .primary(true)
                    .build();
            id = api.createMetaField(value);

        } else {
            MetaFieldWS[] metaFieldsForEntity = api.getMetaFieldsForEntity(EntityType.COMPANY.name());
            MetaFieldWS metaField = Arrays.stream(metaFieldsForEntity).filter(metaFieldWS -> metaFieldWS.getName().equals(name)).findFirst().orElse(null);
            id = null != metaField ?  metaField.getId() : 0;
        }
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
    }

    private void createCustomer(JbillingAPI api, TestEnvironmentBuilder envBuilder, String userName, Long customerOrgId) {
        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(userName)
                .withAccountTypeId(envBuilder.idForCode(MOVIUS_ORIGINATION_ACCOUNT_TYPE))
                .addTimeToUsername(false)
                .withMainSubscription(WSTest.createUserMainSubscription())
                .withMetaField(ORG_ID_MF_NAME, customerOrgId.toString())
                .build();
        userId = userWS.getId();
        LOGGER.debug("Customer Created {}", userWS.getId());
    }

    public Integer buildAndPersistOrder(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, Integer userId,
                                        Date activeSince, Date activeUntil, Integer orderPeriodId, int billingTypeId,
                                        boolean prorate, Map<Integer, BigDecimal> productQuantityMap) {
        OrderBuilder orderBuilder = envBuilder.orderBuilder(api)
                .withCodeForTests(code)
                .forUser(userId)
                .withActiveSince(activeSince)
                .withActiveUntil(activeUntil)
                .withEffectiveDate(activeSince)
                .withPeriod(orderPeriodId)
                .withBillingTypeId(billingTypeId)
                .withProrate(prorate);

        for (Map.Entry<Integer, BigDecimal> entry : productQuantityMap.entrySet()) {
            orderBuilder.withOrderLine(
                    orderBuilder.orderLine()
                            .withItemId(entry.getKey())
                            .withQuantity(entry.getValue())
                            .build());
        }

        return orderBuilder.build();
    }

    public Integer buildAndPersistOrderPeriod(TestEnvironmentBuilder envBuilder, JbillingAPI api,
                                              String description, Integer value, Integer unitId) {

        return envBuilder.orderPeriodBuilder(api)
                .withDescription(description)
                .withValue(value)
                .withUnitId(unitId)
                .build();
    }

    private boolean validateOrder (OrderWS orderWS, Integer rowCount, Integer productId, Integer quantity,
    		BigDecimal price,String itemDescription) {

        OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

        LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, rowCount);
        assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                rowCount, orderLineWSArray.length), rowCount, Integer.valueOf(orderLineWSArray.length));

        if (0 != orderLineWSArray.length) {

            OrderLineWS orderLineWS = orderLineWSArray[0];
            orderLineWS.getItemId();
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}",orderLineWS.getDescription(),itemDescription);
            assertEquals("Order line description must match with production description",itemDescription,
                         orderLineWS.getDescription());
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), productId);
        assertEquals(String.format("field %s Expected %s, got %s", "Product ID",
                productId, orderLineWS.getItemId()), productId, orderLineWS.getItemId());
            Integer orderLineQuantity = Integer.valueOf(orderLineWS.getQuantityAsDecimal().intValue());
            LOGGER.debug("orderLineWS.getQuantityAsDecimal() : {} - quantity : {}", orderLineQuantity, quantity);
        assertEquals(String.format("field %s Expected %s, got %s", "Quantity",
                quantity, orderLineQuantity), quantity, orderLineQuantity);
            BigDecimal orderLinePrice = orderLineWS.getPriceAsDecimal().setScale(2, RoundingMode.HALF_UP);
            LOGGER.debug("orderLineWS.getPriceAsDecimal() : {} - price : {}", orderLinePrice, price);
        assertEquals(String.format("field %s Expected %s, got %s", "Price",
                price, orderLinePrice), price, orderLinePrice);
        }

        return true;
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException ex) {

        }
    }

    @BeforeClass
    public void initialize() throws Exception {
        testBuilder = getTestEnvironment();
        setupMoviusData();
    }

    @AfterClass
    private void cleanUp() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        testBuilder = null;
    }

    /*
    Task to test following
    CASE 1. Incorrect XML Schema
    CASE 2. Incorrect Product
    CASE 3. Create Order
    CASE 4. Same XML, no change
    CASE 5. Update order with charges
    CASE 6. Update Order with count increase
    CASE 7. Update Order with count decrease
     */

    @Test
    public void test001InvalidXmlSchema() throws Exception {

        BigDecimal charges = new BigDecimal("10.00");
        Integer count = new Integer("7");

        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_6 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_6 = replaceTagWithProperValue(case_6, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_6 = replaceTagWithProperValue(case_6, TAG_NAME_COUNT, null, count.toString());
        case_6 = case_6.replace("\t\t\t<org-mapping>\n", "");
        createXmlFile(ORIGINATION_XML, case_6);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS userOrder = api.getLatestOrder(customerId);
            assertEquals("Order should be null", null, userOrder);
        });

    }


    @Test
    public void test002IncorrectProductName() throws Exception {

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        BigDecimal charges = new BigDecimal("20.00");
        Integer count = new Integer("15");
        String invalidProductName = "invalidProduct";

        String case_6 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_6 = replaceTagWithProperValue(case_6, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_6 = replaceTagWithProperValue(case_6, TAG_NAME_COUNT, null, count.toString());
        case_6 = replaceTagWithProperValue(case_6, TAG_NAME_NAME, "Anveo", invalidProductName);
        createXmlFile(ORIGINATION_XML, case_6);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS userOrder = api.getLatestOrder(customerId);
            assertEquals("Order should be null", null, userOrder);
        });
    }


    @Test
    public void test003CreateOriginationCharges() throws Exception {

        // test values for case 3
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed",orderWS);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(orderWS, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, charges,itemDescription);
            api.deleteOrder(orderWS.getId());
        });
    }

    @Test
    public void test004NoChangeOriginationCharges() throws Exception {

        // test values for case 4
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(orderWS, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, charges,itemDescription);
        });

        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_2);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS userOrder = api.getLatestOrder(customerId);
            assertNotNull("Order Should bot be null",userOrder);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(userOrder, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, charges,itemDescription);
            api.deleteOrder(userOrder.getId());
        });
    }

    @Test
    public void test005UpdateCharges() throws Exception {

        // test values for case 5
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(orderWS, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, charges,itemDescription);
        });

        // update charges value
        BigDecimal newCharges = new BigDecimal("10.00");

        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, newCharges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_2);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS userOrder = api.getLatestOrder(customerId);
            assertNotNull("Order Should bot be null",userOrder);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(userOrder, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, newCharges,itemDescription);
            api.deleteOrder(userOrder.getId());
        });

    }

    @Test
    public void test006UpdateCountIncrement() throws Exception {

        // test values for case 6
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(orderWS, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, charges,itemDescription);
        });

        // update charges value
        Integer newCount = new Integer("15");

        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, newCount.toString());
        createXmlFile(ORIGINATION_XML, case_2);


        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS userOrder = api.getLatestOrder(customerId);
            assertNotNull("Order Should bot be null",userOrder);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(userOrder, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), newCount, charges,itemDescription);
            api.deleteOrder(userOrder.getId());
        });

    }

    @Test
    public void test007UpdateCountDecrement() throws Exception {

        // test values for case 1
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(orderWS, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), count, charges,itemDescription);
        });

        // update charges value
        Integer newCount = new Integer("5");

        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, newCount.toString());
        createXmlFile(ORIGINATION_XML, case_2);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(80000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS userOrder = api.getLatestOrder(customerId);
            assertNotNull("Order Should bot be null",userOrder);
            String itemDescription = api.getItem(testEnvBuilder.idForCode(ANVEO_AUSTRALIA), null, null).getDescription();
            validateOrder(userOrder, 1, testEnvBuilder.idForCode(ANVEO_AUSTRALIA), newCount, charges, itemDescription);
            api.deleteOrder(userOrder.getId());
        });
    }

    @Test
    public void test008UpdateOriginationAndSubscription() throws Exception {

        // test values for case 8
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            monthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testMonthlyPeriod", 1, PeriodUnitDTO.MONTH);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(envBuilder.env().idForCode(SUBSCRIPTION_ITEM), new BigDecimal("4"));
            buildAndPersistOrder(envBuilder, api, ORDER_CODE, userId,
                    new Date(), null, monthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, productQuantityMap);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 1);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    1, orderLineWSArray.length), 1, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    4, orderLineWS.getQuantityAsDecimal().intValue()), 4, orderLineWS.getQuantityAsDecimal().intValue());
        });


        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 2);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    2, orderLineWSArray.length), 2, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    10, orderLineWS.getQuantityAsDecimal().intValue()), 10, orderLineWS.getQuantityAsDecimal().intValue());
        });
    }

    @Test
    public void test009ReduceOriginationAndSubscriptionQuantity() throws Exception {

        // test values for case 8
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("11");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            monthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testMonthlyPeriod", 1, PeriodUnitDTO.MONTH);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(envBuilder.env().idForCode(SUBSCRIPTION_ITEM), new BigDecimal("5"));
            buildAndPersistOrder(envBuilder, api, ORDER_CODE, userId,
                    new Date(), null, monthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, productQuantityMap);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 1);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    1, orderLineWSArray.length), 1, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    5, orderLineWS.getQuantityAsDecimal().intValue()), 5, orderLineWS.getQuantityAsDecimal().intValue());
        });

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 2);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    2, orderLineWSArray.length), 2, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    11, orderLineWS.getQuantityAsDecimal().intValue()), 11, orderLineWS.getQuantityAsDecimal().intValue());
        });


        // update count value
        Integer newCount = new Integer("0");

        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, newCount.toString());
        createXmlFile(ORIGINATION_XML, case_2);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 0);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    0, orderLineWSArray.length), 0, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNull("Subscription OrderLine should be null after setting quantity to 0",orderLineWS);
        });
    }

    @Test
    public void test0010MatchOriginationAndSubscriptionQuantity() throws Exception {

        // test values for case 10
        BigDecimal charges = new BigDecimal("7.25");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            monthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testMonthlyPeriod", 1, PeriodUnitDTO.MONTH);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(envBuilder.env().idForCode(SUBSCRIPTION_ITEM), new BigDecimal("10"));
            buildAndPersistOrder(envBuilder, api, ORDER_CODE, userId,
                    new Date(), null, monthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, productQuantityMap);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 1);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    1, orderLineWSArray.length), 1, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    10, orderLineWS.getQuantityAsDecimal().intValue()), 10, orderLineWS.getQuantityAsDecimal().intValue());
        });

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 2);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    2, orderLineWSArray.length), 2, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    10, orderLineWS.getQuantityAsDecimal().intValue()), 10, orderLineWS.getQuantityAsDecimal().intValue());
        });


        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, count.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_NAME, "Anveo", "Tata");
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_NAME, "Australia", "UK");
        createXmlFile(ORIGINATION_XML, case_2);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 3);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    3, orderLineWSArray.length), 3, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field %s Expected %s, got %s", "Subscription Order Line Quantity",
                    20, orderLineWS.getQuantityAsDecimal().intValue()), 20, orderLineWS.getQuantityAsDecimal().intValue());
        });
    }

    @Test
    public void test0011UpdatePriceShouldNotUpdateSubscriptionOrderChangeEndDate() throws Exception {

        // test values for case 10
        BigDecimal charges = new BigDecimal("5");
        Integer count = new Integer("10");

        // create Customer
        Long customerOrgId = System.currentTimeMillis();
        final String userName = concatenateString("testUserName-", customerOrgId.toString());

        String case_1 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_COUNT, null, count.toString());
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_NAME, "Anveo", "Tata");
        case_1 = replaceTagWithProperValue(case_1, TAG_NAME_NAME, "Australia", "UK");
        createXmlFile(ORIGINATION_XML, case_1);

        testBuilder.given(envBuilder -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DATE, -1);
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            createCustomer(api, envBuilder, userName, customerOrgId);
            monthlyPeriodId = buildAndPersistOrderPeriod(envBuilder, api, "testMonthlyPeriod", 1, PeriodUnitDTO.MONTH);
            Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
            productQuantityMap.put(envBuilder.env().idForCode(SUBSCRIPTION_ITEM), new BigDecimal("10"));
            buildAndPersistOrder(envBuilder, api, ORDER_CODE, userId,
                    calendar.getTime(), null, monthlyPeriodId, Constants.ORDER_BILLING_POST_PAID,
                    true, productQuantityMap);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            LOGGER.debug("New orderWS"+ orderWS);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 1);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    1, orderLineWSArray.length), 1, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field 1 %s Expected %s, got %s", "Subscription Order Line Quantity",
                    10, orderLineWS.getQuantityAsDecimal().intValue()), 10, orderLineWS.getQuantityAsDecimal().intValue());
            assertEquals(String.format("field 1 %s Expected %s, got %s", "Subscription Order Line Price",
                    5, orderLineWS.getPriceAsDecimal().intValue()), 5, orderLineWS.getPriceAsDecimal().intValue());
        });


        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 2);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    2, orderLineWSArray.length), 2, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field 3 %s Expected %s, got %s", "Subscription Order Line Quantity",
                    10, orderLineWS.getQuantityAsDecimal().intValue()), 10, orderLineWS.getQuantityAsDecimal().intValue());
        });

        charges = new BigDecimal("3");
        String case_2 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_ORG_ID, null, customerOrgId.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_COUNT, null, count.toString());
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_NAME, "Anveo", "Tata");
        case_2 = replaceTagWithProperValue(case_2, TAG_NAME_NAME, "Australia", "UK");
        createXmlFile(ORIGINATION_XML, case_2);

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            schedulePlugin(api, envBuilder);
            sleep(10000);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            Integer customerId = testEnvBuilder.idForCode(userName);
            LOGGER.debug("userName {} : customerId {}",userName, customerId);
            OrderWS orderWS = api.getLatestOrder(customerId);
            LOGGER.debug("Updated orderWS"+ orderWS);
            assertNotNull("Order creation failed", orderWS);
            OrderLineWS [] orderLineWSArray = orderWS.getOrderLines();

            LOGGER.debug("orderLineWSArray.length : {} - rowCount : {}", orderLineWSArray.length, 2);
            assertEquals(String.format("field %s Expected %s, got %s", "Order Lines",
                    2, orderLineWSArray.length), 2, orderLineWSArray.length);

            OrderLineWS orderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 == orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            OrderLineWS orgOrderLineWS = Arrays.stream(orderLineWSArray).filter(orderLineWS1 -> 0 != orderLineWS1.getItemId().compareTo(subscriptionItemId)).findFirst().orElse(null);
            assertNotNull("Subscription OrderLine should not be null",orderLineWS);
            LOGGER.debug("orderLineWS.getItemId() : {} - productId : {}", orderLineWS.getItemId(), subscriptionItemId);
            assertEquals(String.format("field 2 %s Expected %s, got %s", "Subscription Order Line Quantity",
                    10, orderLineWS.getQuantityAsDecimal().intValue()), 10, orderLineWS.getQuantityAsDecimal().intValue());
            assertEquals(String.format("field 2 %s Expected %s, got %s", "Subscription Order Line Price",
                    5, orderLineWS.getPriceAsDecimal().intValue()), 5, orderLineWS.getPriceAsDecimal().intValue());
            assertEquals(String.format("field 2 %s Expected %s, got %s", "Origination Order Line Price",
                    3, orgOrderLineWS.getPriceAsDecimal().intValue()), 3, orgOrderLineWS.getPriceAsDecimal().intValue());
        });
    }

}
