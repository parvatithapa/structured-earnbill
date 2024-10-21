package com.sapienter.jbilling.server.movius;

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

/**
 * 
 * @author Manish Bansod
 * @Date 10-27-2017
 *
 */
@Test(groups = { "movius" }, testName = "movius")
public class MoviusOrgHierarchyMappingTaskTest {


    //@formatter:off
    private static final Logger LOGGER = LoggerFactory.getLogger(MoviusOrgHierarchyMappingTaskTest.class);
    private EnvironmentHelper envHelper;
    private TestBuilder       testBuilder;
    private static final Integer CC_PM_ID                                       = 5;
    private static final String ACCOUNT_NAME                                    = "Movius Test Account";
    private static final String MEDIATED_USAGE_CATEGORY                         = "Movius Mediation Usage Category";
    private static final String META_FIELD_CUSTOMER_ORG_ID                      = "Org Id";
    private static final String META_FIELD_TATA_CALL_ITEM_ID                    = "Set Item Id For Tata Calls";
    private static final String META_FIELD_BILLING_PLAN_ID                      = "Billing Plan Id";
    private static final String META_FIELD_BILLING_PLAN_NAME                    = "Billing Plan Name";
    private static final String META_FIELD_TIMEZONE                             = "Timezone";
    private static final String META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER  = "Set Item Id for Org Hierarchy Order";
    private static final String ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY               = "Set Account Type Id for Org Hierarchy User";
    private static final String TATA_CALL_ITEM                                  = "TATA CALL ITEM";
    private static final String ORIGINATION_ITEM                                = "ORIGINATION ITEM";
    private static final String ORIGINATION_ITEM_2                              = "ORIGINATION ITEM 2";
    private static final String DONE_XML_DIR                                    = "done";
    private static final String PRE_DEFINED_XML_DIR                             = "pre-defined";
    private static final String PLUGIN_CODE                                     = "Plugin-Code";
    private static final String MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME    = "com.sapienter.jbilling.server.process.task.MoviusOrgHierarchyMappingTask";
    private static final String XML_BASE_DIRECTORY                              = Util.getSysProp("base_dir") + "movius-test/xml";
    private static final String XSD_BASE_DIRECTORY                              = Util.getSysProp("base_dir") + "movius-test/xsd";
    private static final String BASIC_BILLING_PROCESS_FITLER_TASK               = "com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask";
    private static final String MOVIUS_INVOICE_COMPOSITION_TASK                 = "com.sapienter.jbilling.server.pluggableTask.MoviusInvoiceCompositionTask";
    private static final String SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK     = "com.sapienter.jbilling.server.process.task.SuspendedUsersBillingProcessFilterTask";
    private static final String CHECK_NEXT_INVOICE_DATE_OF_USER_AFTER           = "Check next invoice date of user after ";
    private static final Integer AUSTRALIAN_DOLLOR_CURRENCY_ID                  = 11;
    private static final String CUSTOMER_NOT_UPDATED                            = "Customer not Updated";
    private static final String COMPANY_LEVEL_META_FIELD_CREATION_FAILED        = "Company Level MetaField Creation Failed ";
    private static final String CUSTOMER_LEVEL_META_FIELD_CREATION_FAILED       = "Customer Level MetaField Creation Failed ";
    private static final String PARAM_NAME_XML_BASE_DIRECTORY                   = "XML Base Directory";
    private static final String ORDER_IS_NOT_EXPECTED                           = "Order is not expected";
    private static final String ORDER_NOT_CREATED                               = "Order not Created";
    private static final String CUSTOMER_NOT_CREATED                            = "Customer not created";
    private static final String TEST450_450A                                    = "test450-450A";
    private static final String XYZ_ORG_1517                                    = "XYZ Org-1517";
    private static final String TEST_1234                                       = "Test-1234";
    private static final String PLOKIJ_449A                                     = "PLOKIJ-449A";
    private static final String PARENT_ID_SHOULD_BE_MATCH                       = "Parent ID should be match";
    private static final String ORDER_CREATED                                   = "Order created";
    private static final String J_BILLING_TOP_ORG_612A                          = "JBilling Top Org-612A";
    private static final String FIRST_NAME                                      = "First Name";
    private static final String AUTOMATION_ORGANIZATION_152A                    = "AutomationOrganization-152A";
    private static final String ABC_ORG_2516                                    = "ABC Org-2516";
    private static final String ABC_ORG_1516                                    = "ABC Org-1516";
    private static final String ORGFOR_EAP_459A                                 = "459OrgforEAP-459A";
    private static final String ABC_ORG_3516                                    = "ABC Org-3516";
    private static final String MOVIUS_HIERARCHY_1515                           = "Movius-Hierarchy-1515";
    private static final String ORDER_CHANGES_COUNT_MUST_BE_1                   = "Order changes count must be 1";
    private static final String BEEANS_UPDATED_5000                             = "Beeans-Updated-5000";
    private static final String ORDR_NT_EXPCTD_EVNT_CONT_GIVN_BT_NO_PRNT_SUB_FOUND = "Order is not expected event count is given, but no parent subscription found";
    private static final String ORIGINATION_ORDER                               = "Origination Order";
    private static final String PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH        = "Parent NID and Child NID should be match";
    private static final String USE_PARENT_PRICING_SHOULD_BE_FALSE              = "Use Parent Pricing should be false";
    private static final String ORDER_UPDATED                                   = "Order updated";
    private static final String ORDER_LINE_FOUND                                = "Order Line found";
    private static final String NO_ORDERS_EXPECTED                              = "No orders expected!";
    private static final String FIVE                                            = "5.0000000000";
    private static final String THREE_ONE_FIVE_POINT_THREE                      = "315.3000000000";
    private static final String FIFTEEN                                         = "15.0000000000";
    private static final String THIRTY                                          = "30.0000000000";
    private Integer groupId                                                     = null;

    //@formatter:on
    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> 
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi())
        );
    }

    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            // Creating account type
                Integer accountTypeId = buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);
                AccountTypeWS accountType = api.getAccountType(accountTypeId);
                accountType.setInvoiceTemplateId(1);
                api.updateAccountType(accountType);
                // Creating mediated usage category
                buildAndPersistCategory(envBuilder, api, MEDIATED_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

                // Creating Mediated Products
                buildAndPersistFlatProduct(envBuilder, api, TATA_CALL_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), "2.00", true);
                buildAndPersistFlatProduct(envBuilder, api, ORIGINATION_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), "5.00", true);
                buildAndPersistFlatProduct(envBuilder, api, ORIGINATION_ITEM_2, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), "10.00", true);

                // Creating Customer Level MetaField
                buildAndPersistMetafield(testBuilder, META_FIELD_CUSTOMER_ORG_ID, DataType.STRING, EntityType.CUSTOMER);
                buildAndPersistMetafield(testBuilder, META_FIELD_BILLING_PLAN_ID, DataType.INTEGER, EntityType.CUSTOMER);
                buildAndPersistMetafield(testBuilder, META_FIELD_BILLING_PLAN_NAME, DataType.STRING, EntityType.CUSTOMER);
                buildAndPersistMetafield(testBuilder, META_FIELD_TIMEZONE, DataType.STRING, EntityType.CUSTOMER);

                // Creating Company Level MetaField
                buildAndPersistMetafield(testBuilder, META_FIELD_TATA_CALL_ITEM_ID, DataType.STRING, EntityType.COMPANY);
                buildAndPersistMetafield(testBuilder, META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER, DataType.STRING, EntityType.COMPANY);
                buildAndPersistMetafield(testBuilder, ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY, DataType.STRING, EntityType.COMPANY);

                // Setting Company Level Meta Fields
                setCompanyLevelMetaField(testBuilder.getTestEnvironment());

                // configuring plugin
                envBuilder.pluginBuilder(api)
                        .withCode(PLUGIN_CODE)
                        .withTypeId(api.getPluginTypeWSByClassName(MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME).getId())
                        .withOrder(7)
                        .withParameter(PARAM_NAME_XML_BASE_DIRECTORY, XML_BASE_DIRECTORY)
                        .withParameter("XSD Base Directory", XSD_BASE_DIRECTORY)
                        .withParameter("billing cycle period Id", "2")
                        .withParameter("billing cycle day", "1")
                        .build();

                // configuring plugin
                envBuilder.pluginBuilder(api)
                        .withCode(MOVIUS_INVOICE_COMPOSITION_TASK)
                        .withTypeId(api.getPluginTypeWSByClassName(MOVIUS_INVOICE_COMPOSITION_TASK).getId())
                        .withOrder(3)
                        .build();

            }).test((testEnv, testEnvBuilder) -> {

            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
            assertNotNull("Mediated Categroy Creation Failed ", testEnvBuilder.idForCode(MEDIATED_USAGE_CATEGORY));
            assertNotNull(CUSTOMER_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(META_FIELD_CUSTOMER_ORG_ID));
            assertNotNull(CUSTOMER_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(META_FIELD_BILLING_PLAN_ID));
            assertNotNull(CUSTOMER_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(META_FIELD_BILLING_PLAN_NAME));
            assertNotNull(CUSTOMER_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(META_FIELD_TIMEZONE));
            assertNotNull(COMPANY_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(META_FIELD_TATA_CALL_ITEM_ID));
            assertNotNull(COMPANY_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER));
            assertNotNull(COMPANY_LEVEL_META_FIELD_CREATION_FAILED, testEnvBuilder.idForCode(ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY));
            assertNotNull("Tata Item Creation Failed ", testEnvBuilder.idForCode(TATA_CALL_ITEM));
            assertNotNull("Origination Item Creation Failed ", testEnvBuilder.idForCode(ORIGINATION_ITEM));
            assertNotNull("Origination Item 2 Creation Failed ", testEnvBuilder.idForCode(ORIGINATION_ITEM_2));
        });
    }

    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        envHelper = null;
        testBuilder = null;
    }

    /**
     * Basic user and order creation test from provided xml after scheduler run
     */
    @Test(priority = 1, enabled = true)
    public void test001CheckUsersOrdersCreatedFromXML() {
        String beeans1000 = "Beeans-1000";
        String abcOrg1001 = "ABC Org-1001";
        String xyzOrg1002 = "XYZ Org-1002";
        String telstraAustralia2000 = "Telstra Australia-2000";
        String telstraABCOrg2001 = "Telstra ABC Org-2001";
        String abcSalesDept2100 = "ABC Sales Department-2100";
        String abcCustomerDept2 = "ABC Customer Department-2101";
        String telstraXYZorf2002 = "Telstra XYZ Org-2002";
        String thirdOrg2003 = "Third Org-2003";
        String movius3000 = "Movius-3000";
        testBuilder
                .given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
                    sleep(30000L);
                    PluggableTaskWS pluggableTaskWS = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(
                            MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME).getId());
                    Map<String, String> parameters = pluggableTaskWS.getParameters();

                    File currDir = new File(parameters.get(PARAM_NAME_XML_BASE_DIRECTORY) + File.separator + DONE_XML_DIR);
                    File[] xmlFilesList = currDir.listFiles(file -> file.getName().endsWith(".xml"));
                    Arrays.sort(xmlFilesList, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

                    for (File f : xmlFilesList) {
                        if (f.isFile()) {
                            File renamedFile = new File(parameters.get(PARAM_NAME_XML_BASE_DIRECTORY) + File.separator + f.getName());
                            f.renameTo(renamedFile);
                        }
                    }
                }).validate((testEnv, testEnvBuilder) -> {
                    final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

                    assertEquals(CUSTOMER_NOT_CREATED, beeans1000,
                            api.getUserByCustomerMetaField("1000", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, abcOrg1001,
                            api.getUserByCustomerMetaField("1001", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, xyzOrg1002,
                            api.getUserByCustomerMetaField("1002", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, telstraAustralia2000,
                            api.getUserByCustomerMetaField("2000", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, telstraABCOrg2001,
                            api.getUserByCustomerMetaField("2001", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, abcSalesDept2100,
                            api.getUserByCustomerMetaField("2100", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, abcCustomerDept2,
                            api.getUserByCustomerMetaField("2101", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, telstraXYZorf2002,
                            api.getUserByCustomerMetaField("2002", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, thirdOrg2003,
                            api.getUserByCustomerMetaField("2003", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, movius3000,
                            api.getUserByCustomerMetaField("3000", META_FIELD_CUSTOMER_ORG_ID).getUserName());

                    // Order created for users
                        OrderWS order1 = api.getLatestOrder(api.getUserId(beeans1000));
                        assertNotNull(ORDER_NOT_CREATED, order1);
                        OrderWS order2 = api.getLatestOrder(api.getUserId(abcOrg1001));
                        assertNotNull(ORDER_NOT_CREATED, order2);
                        OrderWS order3 = api.getLatestOrder(api.getUserId(xyzOrg1002));
                        assertNull(order3, ORDER_IS_NOT_EXPECTED);
                        OrderWS order4 = api.getLatestOrder(api.getUserId(telstraAustralia2000));
                        assertNotNull(ORDER_NOT_CREATED, order4);
                        OrderWS order5 = api.getLatestOrder(api.getUserId(telstraABCOrg2001));
                        assertNull(order5, ORDER_IS_NOT_EXPECTED);
                        OrderWS order6 = api.getLatestOrder(api.getUserId(abcSalesDept2100));
                        assertNull(order6, ORDER_IS_NOT_EXPECTED);
                        OrderWS order7 = api.getLatestOrder(api.getUserId(abcCustomerDept2));
                        assertNull(order7, ORDER_IS_NOT_EXPECTED);
                        OrderWS order8 = api.getLatestOrder(api.getUserId(telstraXYZorf2002));
                        assertNull(order8, ORDER_IS_NOT_EXPECTED);
                        OrderWS order9 = api.getLatestOrder(api.getUserId(thirdOrg2003));
                        assertNull(order9, ORDER_IS_NOT_EXPECTED);
                        OrderWS order10 = api.getLatestOrder(api.getUserId(movius3000));
                        assertNotNull(ORDER_NOT_CREATED, order10);

                    }).validate((testEnv, testEnvBuilder) -> {
                    final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

                    api.triggerScheduledTask(testEnvBuilder.env().idForCode(PLUGIN_CODE), new Date());
                    sleep(30000L);

                    assertEquals(CUSTOMER_NOT_CREATED, beeans1000,
                            api.getUserByCustomerMetaField("1000", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, abcOrg1001,
                            api.getUserByCustomerMetaField("1001", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, xyzOrg1002,
                            api.getUserByCustomerMetaField("1002", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, telstraAustralia2000,
                            api.getUserByCustomerMetaField("2000", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, telstraABCOrg2001,
                            api.getUserByCustomerMetaField("2001", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, abcSalesDept2100,
                            api.getUserByCustomerMetaField("2100", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, abcCustomerDept2,
                            api.getUserByCustomerMetaField("2101", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, telstraXYZorf2002,
                            api.getUserByCustomerMetaField("2002", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, thirdOrg2003,
                            api.getUserByCustomerMetaField("2003", META_FIELD_CUSTOMER_ORG_ID).getUserName());
                    assertEquals(CUSTOMER_NOT_CREATED, movius3000,
                            api.getUserByCustomerMetaField("3000", META_FIELD_CUSTOMER_ORG_ID).getUserName());

                    // Order created for users
                        OrderWS order1 = api.getLatestOrder(api.getUserId(beeans1000));
                        assertNotNull(ORDER_NOT_CREATED, order1);
                        OrderWS order2 = api.getLatestOrder(api.getUserId(abcOrg1001));
                        assertNotNull(ORDER_NOT_CREATED, order2);
                        OrderWS order3 = api.getLatestOrder(api.getUserId(xyzOrg1002));
                        assertNull(order3, ORDER_IS_NOT_EXPECTED);
                        OrderWS order4 = api.getLatestOrder(api.getUserId(telstraAustralia2000));
                        assertNotNull(ORDER_NOT_CREATED, order4);
                        OrderWS order5 = api.getLatestOrder(api.getUserId(telstraABCOrg2001));
                        assertNull(order5, ORDER_IS_NOT_EXPECTED);
                        OrderWS order6 = api.getLatestOrder(api.getUserId(abcSalesDept2100));
                        assertNull(order6, ORDER_IS_NOT_EXPECTED);
                        OrderWS order7 = api.getLatestOrder(api.getUserId(abcCustomerDept2));
                        assertNull(order7, ORDER_IS_NOT_EXPECTED);
                        OrderWS order8 = api.getLatestOrder(api.getUserId(telstraXYZorf2002));
                        assertNull(order8, ORDER_IS_NOT_EXPECTED);
                        OrderWS order9 = api.getLatestOrder(api.getUserId(thirdOrg2003));
                        assertNull(order9, ORDER_IS_NOT_EXPECTED);
                        OrderWS order10 = api.getLatestOrder(api.getUserId(movius3000));
                        assertNotNull(ORDER_NOT_CREATED, order10);

                    });
    }

    /**
     * Change the user name of existing customer 'Beeans-1000' in XML file, there should no new user created for this
     */
    @Test(priority = 2, enabled = true)
    public void test002UpdateUserNameFromXML() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "1.update-username.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("1000", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_UPDATED, "Beeans-Updated-1000", userWS.getUserName());

            // no price or quanity has changed in xml, so order should not be updated
                OrderWS order = api.getLatestOrder(userWS.getId());
                assertEquals("Order should not be updated", new BigDecimal(FIVE), getAmount(order));
            });
    }

    /**
     * Change the org id of existing customer 'Beeans-Updated-1000' in XML file, there should new user created for this.
     * And if there are existing child for the same user those should be shifted to newly created user
     */
    @Test(priority = 3, enabled = true)
    public void test003OrgIdChangeForUserInXML() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "2.update-orgId.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("5000", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_CREATED, BEEANS_UPDATED_5000, userWS.getUserName());
            assertEquals("ABC Org-1001 not shifted to new parent Beeans-Updated-5000", api.getUserId(BEEANS_UPDATED_5000),
                    api.getUserByCustomerMetaField("1001", META_FIELD_CUSTOMER_ORG_ID).getParentId());
            assertEquals("XYZ Org-1002 not shifted to new parent Beeans-Updated-5000", api.getUserId(BEEANS_UPDATED_5000),
                    api.getUserByCustomerMetaField("1002", META_FIELD_CUSTOMER_ORG_ID).getParentId());

            // Since org id changed and new customer created, new order must be created if subscription is given in xml
                OrderWS order = api.getLatestOrder(userWS.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal(FIVE), getAmount(order));

                // Check order changes quaitity and price
                OrderChangeWS[] changes = api.getOrderChanges(order.getId());
                assertEquals(ORDER_CHANGES_COUNT_MUST_BE_1, 1, changes.length);
                assertEquals("Order change price must be 5 ", new BigDecimal(FIVE), changes[0].getPriceAsDecimal());
                assertEquals("Order change quantity must be 1", new BigDecimal("1.0000000000"), changes[0].getQuantityAsDecimal());
            });
    }

    /**
     * Change the org id of existing customer 'Beeans-Updated-1000' in XML file, there should new user created for this.
     * And if there are existing child for the same user those should be shifted to newly created user
     */
    @Test(priority = 4, enabled = true)
    public void test004PriceAndQuantityChangeForUserInXML() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "3.update-price-quantity.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("5000", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_UPDATED, BEEANS_UPDATED_5000, userWS.getUserName());

            OrderWS order = api.getLatestOrder(userWS.getId());
            assertEquals(ORDER_UPDATED, new BigDecimal("28.0000000000"), getAmount(order));

            /*
             * Since price and quantity changed in xml, order must be updated and since its updated on same date, the
             * existing order change to be updated and there must be only one order change
             */
                OrderChangeWS[] changes = api.getOrderChanges(order.getId());
                assertEquals(ORDER_CHANGES_COUNT_MUST_BE_1, 1, changes.length);
                assertEquals("Order change price must be updated to 7", new BigDecimal("7.0000000000"), changes[0].getPriceAsDecimal());
                assertEquals("Order change quantity must be updated to 4", new BigDecimal("4.0000000000"), changes[0].getQuantityAsDecimal());
            });
    }

    /**
     * Create org hierarchy without any subscription order
     */
    @Test(priority = 5, enabled = true)
    public void test005OrgHierarchyWithNoSubscrption() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "4.org-hierarchy-without-any-subscription.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("1515", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_CREATED, MOVIUS_HIERARCHY_1515, userWS.getUserName());

            // no price or quanity has provided in xml, so order should not be created
                OrderWS order = api.getLatestOrder(userWS.getId());
                assertNull(order, NO_ORDERS_EXPECTED);
                OrderWS order1 = api.getLatestOrder(api.getUserId(ABC_ORG_1516));
                assertNull(order1, NO_ORDERS_EXPECTED);
                OrderWS order2 = api.getLatestOrder(api.getUserId(XYZ_ORG_1517));
                assertNull(order2, NO_ORDERS_EXPECTED);
            });
    }

    /**
     * Update above org hierarchy without any subscription order which is created in
     * test005OrgHierarchyWithNoSubscrption with price and quantity, order must be created for hierarchy
     */
    @Test(priority = 6, enabled = true)
    public void test006UpdateOrgHierarchyWithSubscrption() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "5.update-org-hierarchy-with-subscription.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS1 = api.getUserByCustomerMetaField("1515", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS2 = api.getUserByCustomerMetaField("1516", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS3 = api.getUserByCustomerMetaField("1517", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_UPDATED, MOVIUS_HIERARCHY_1515, userWS1.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, ABC_ORG_1516, userWS2.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, XYZ_ORG_1517, userWS3.getUserName());

            // Since price and quantity changed in xml, order must be updated
                OrderWS order = api.getLatestOrder(userWS1.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal("42.0000000000"), getAmount(order));
                OrderWS order1 = api.getLatestOrder(userWS2.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal("9.0000000000"), getAmount(order1));
                OrderWS order2 = api.getLatestOrder(userWS3.getId());
                assertNull(order2, "No order expected!");
            });
    }

    /**
     * Update above org hierarchy without any subscription order which is created in
     * test006UpdateOrgHierarchyWithSubscrption with price and quantity, order must be updated for hierarchy
     */
    @Test(priority = 7, enabled = true)
    public void test007UpdateOrgHierarchyWithQuanity() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "6.update-org-hierarchy-with-quantity.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

            UserWS userWS1 = api.getUserByCustomerMetaField("1515", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS2 = api.getUserByCustomerMetaField("1516", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS3 = api.getUserByCustomerMetaField("1517", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_UPDATED, MOVIUS_HIERARCHY_1515, userWS1.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, ABC_ORG_1516, userWS2.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, XYZ_ORG_1517, userWS3.getUserName());

            // Since price and quantity changed in xml, order must be updated
                OrderWS order = api.getLatestOrder(userWS1.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal("91.0000000000"), getAmount(order));
                OrderWS order1 = api.getLatestOrder(userWS2.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal("54.0000000000"), getAmount(order1));
                OrderWS order2 = api.getLatestOrder(userWS3.getId());
                assertNull(order2, "No order expected!");
            });
    }

    // **************************ORIGINATION TASK RAN BEFORE ORG HIERARCHY TASK***********************//
    /**
     * Create org hierarchy without any subscription order and add origination order
     */
    @Test(priority = 8, enabled = true)
    public void test008OrgHierarchyWithNoSubscrptionAndCreateOriginationOrder() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "7.org-hierarchy-without-any-subscription-2.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("2515", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Movius-Hierarchy-2515", userWS.getUserName());

            // no price or quanity has provided in xml, so order should not be created
                OrderWS order = api.getLatestOrder(userWS.getId());
                assertNull(order, NO_ORDERS_EXPECTED);
                OrderWS order1 = api.getLatestOrder(api.getUserId(ABC_ORG_2516));
                assertNull(order1, NO_ORDERS_EXPECTED);
                OrderWS order2 = api.getLatestOrder(api.getUserId("XYZ Org-2517"));
                assertNull(order2, NO_ORDERS_EXPECTED);

                testEnvBuilder.orderBuilder(api)
                        .forUser(userWS.getId())
                        .withProducts(testEnvBuilder.env().idForCode(ORIGINATION_ITEM))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(new LocalDate().toDate())
                        .withEffectiveDate(new LocalDate().toDate())
                        .withDueDateUnit(Constants.PERIOD_UNIT_MONTH)
                        .withDueDateValue(1)
                        .withProrate(true)
                        .withCodeForTests(ORIGINATION_ORDER)
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .build();

                order = api.getLatestOrder(userWS.getId());
                assertEquals(ORDER_CREATED, new BigDecimal(FIVE), getAmount(order));

                UserWS userWS1 = api.getUserByCustomerMetaField("2516", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, ABC_ORG_2516, userWS1.getUserName());

                testEnvBuilder.orderBuilder(api)
                        .forUser(userWS1.getId())
                        .withProducts(testEnvBuilder.env().idForCode(ORIGINATION_ITEM))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(new LocalDate().toDate())
                        .withEffectiveDate(new LocalDate().toDate())
                        .withDueDateUnit(Constants.PERIOD_UNIT_MONTH)
                        .withDueDateValue(1)
                        .withProrate(true)
                        .withCodeForTests(ORIGINATION_ORDER)
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .build();

                order = api.getLatestOrder(userWS1.getId());
                assertEquals(ORDER_CREATED, new BigDecimal(FIVE), getAmount(order));
            });
    }

    /**
     * Upload new file and since origination order is already there, the new order should not be created and existing
     * order must be updated
     */
    @Test(priority = 9, enabled = true)
    public void test009UpdateOriginationOrder() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "8.update-org-hierarchy-with-subscription-2.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

            UserWS userWS1 = api.getUserByCustomerMetaField("2515", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS2 = api.getUserByCustomerMetaField("2516", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS3 = api.getUserByCustomerMetaField("2517", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_UPDATED, "Movius-Hierarchy-2515", userWS1.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, ABC_ORG_2516, userWS2.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, "XYZ Org-2517", userWS3.getUserName());

            // Since Origination order is already there, the existing order must be updated
                OrderWS order = api.getLatestOrder(userWS1.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal("14.0000000000"), getAmount(order));
                OrderWS order1 = api.getLatestOrder(userWS2.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal(FIVE), getAmount(order1));
                OrderWS order2 = api.getLatestOrder(userWS3.getId());
                assertNull(order2, NO_ORDERS_EXPECTED);

                OrderChangeWS[] changes = api.getOrderChanges(order.getId());
                assertEquals("Order changes count must be 2", 2, changes.length);

                OrderChangeWS[] changes1 = api.getOrderChanges(order1.getId());
                assertEquals(ORDER_CHANGES_COUNT_MUST_BE_1, 1, changes1.length);
            });
    }

    // *****************************tests for JBMOV-104*****************************//
    /**
     * Create org hierarchy without any subscription order and add origination order
     */
    @Test(priority = 10, enabled = true)
    public void test010CreateOriginationOrder() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "9.org-hierarchy-without-any-subscription-2.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("3515", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Movius-Hierarchy-3515", userWS.getUserName());

            // no price or quanity has provided in xml, so order should not be created
                OrderWS order = api.getLatestOrder(userWS.getId());
                assertNull(order, NO_ORDERS_EXPECTED);
                OrderWS order1 = api.getLatestOrder(api.getUserId(ABC_ORG_3516));
                assertNull(order1, NO_ORDERS_EXPECTED);
                OrderWS order2 = api.getLatestOrder(api.getUserId("XYZ Org-3517"));
                assertNull(order2, NO_ORDERS_EXPECTED);

                testEnvBuilder.orderBuilder(api)
                        .forUser(userWS.getId())
                        .withProducts(testEnvBuilder.env().idForCode(ORIGINATION_ITEM), testEnvBuilder.env().idForCode(ORIGINATION_ITEM_2))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(new LocalDate().toDate())
                        .withEffectiveDate(new LocalDate().toDate())
                        .withDueDateUnit(Constants.PERIOD_UNIT_MONTH)
                        .withDueDateValue(1)
                        .withProrate(true)
                        .withCodeForTests(ORIGINATION_ORDER)
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .build();

                order = api.getLatestOrder(userWS.getId());
                assertEquals(ORDER_CREATED, new BigDecimal(FIFTEEN), getAmount(order));

                UserWS userWS1 = api.getUserByCustomerMetaField("3516", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, ABC_ORG_3516, userWS1.getUserName());

                testEnvBuilder.orderBuilder(api)
                        .forUser(userWS1.getId())
                        .withProducts(testEnvBuilder.env().idForCode(ORIGINATION_ITEM), testEnvBuilder.env().idForCode(ORIGINATION_ITEM_2))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(new LocalDate().toDate())
                        .withEffectiveDate(new LocalDate().toDate())
                        .withDueDateUnit(Constants.PERIOD_UNIT_MONTH)
                        .withDueDateValue(1)
                        .withProrate(true)
                        .withCodeForTests(ORIGINATION_ORDER)
                        .withPeriod(envHelper.getOrderPeriodMonth(api))
                        .build();

                order = api.getLatestOrder(userWS1.getId());
                assertEquals(ORDER_CREATED, new BigDecimal(FIFTEEN), getAmount(order));
            });
    }

    /**
     * Upload new file and since origination order is already there, the new order should not be created and existing
     * order must be updated and subscription order line quantity must be same as sum of all origination order line
     * quantities
     */
    @Test(priority = 11, enabled = true)
    public void test011UpdateOriginationOrder() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "10.update-org-hierarchy-with-subscription-2.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

            UserWS userWS1 = api.getUserByCustomerMetaField("3515", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS2 = api.getUserByCustomerMetaField("3516", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS3 = api.getUserByCustomerMetaField("3517", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_UPDATED, "Movius-Hierarchy-3515", userWS1.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, ABC_ORG_3516, userWS2.getUserName());
            assertEquals(CUSTOMER_NOT_UPDATED, "XYZ Org-3517", userWS3.getUserName());

            // Since Origination order is already there, the existing order must be updated
                OrderWS order = api.getLatestOrder(userWS1.getId());
                assertEquals(ORDER_UPDATED, new BigDecimal("33.0000000000"), getAmount(order));
                OrderWS order1 = api.getLatestOrder(userWS2.getId());
                assertEquals(ORDER_CREATED, new BigDecimal(FIFTEEN), getAmount(order1));
                OrderWS order2 = api.getLatestOrder(userWS3.getId());
                assertNull(order2, NO_ORDERS_EXPECTED);

                OrderChangeWS[] changes = api.getOrderChanges(order.getId());
                assertEquals("Order changes count must be 3", 3, changes.length);
            });
    }

    // *****************************tests for JBMOV-146*****************************//

    /**
     * Upload new file which has billable as well as non-billable orgs, 1) Parent org must have subscription charges and
     * count so that it will create the order if its billable, else only user will create 2) Negative quantity should
     * not be allowed, in this case, only user will create and continue rest file without creating order 3) If the child
     * org is given with subscription and count, then it should create subscription order if its billable 4) If the
     * child org is given with no subscription but count is given and if its non billable, then it should not create
     * subscription order instead the quantity must roll up to parent subscription. The idea is count and subscription
     * is compulsory for both parent and child if its billable. But If only count is given for non-billable child org,
     * then quantity must roll up to parent subscription 5) If the billable/non-billable child org is given with no
     * subscription and no count, then it should not be creating subscription order
     */
    @Test(priority = 12, enabled = true)
    public void test012OrgHierarchyWithBillableAndNonBillableOrgs() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "11.org-hierarchy-US.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(30000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

            // *****************************Asserts for 1st Org Hierarchy*****************************//
                UserWS userWS1 = api.getUserByCustomerMetaField("11190", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Top Org-4-11190", userWS1.getUserName());
                OrderWS order1 = api.getLatestOrder(userWS1.getId());
                assertNull(order1, "Order is not expected since no subscription and count");

                UserWS userWS2 = api.getUserByCustomerMetaField("11191", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Child Org 1st lebel-11191", userWS2.getUserName());
                OrderWS order2 = api.getLatestOrder(userWS2.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal(THREE_ONE_FIVE_POINT_THREE), getAmount(order2));

                UserWS userWS3 = api.getUserByCustomerMetaField("11192", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "XYZ Org-11192", userWS3.getUserName());
                OrderWS order3 = api.getLatestOrder(userWS3.getId());
                assertNull(order3, "Order is not expected since count is negative");

                UserWS userWS4 = api.getUserByCustomerMetaField("11193", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "PQR Org-11193", userWS4.getUserName());
                OrderWS order4 = api.getLatestOrder(userWS4.getId());
                assertNull(order4, ORDR_NT_EXPCTD_EVNT_CONT_GIVN_BT_NO_PRNT_SUB_FOUND);
                // *****************************END for 1st Org Hierarchy*****************************//

                // *****************************Asserts for 2nd Org Hierarchy*****************************//
                UserWS userWS5 = api.getUserByCustomerMetaField("200", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Top Org-1-200", userWS5.getUserName());
                OrderWS order5 = api.getLatestOrder(userWS5.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal("1080.0000000000"), getAmount(order5));

                UserWS userWS6 = api.getUserByCustomerMetaField("300", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Child Org 1st lebel-300", userWS6.getUserName());
                OrderWS order6 = api.getLatestOrder(userWS6.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal(THREE_ONE_FIVE_POINT_THREE), getAmount(order6));

                UserWS userWS7 = api.getUserByCustomerMetaField("301", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "XYZ Org-301", userWS7.getUserName());
                OrderWS order7 = api.getLatestOrder(userWS7.getId());
                assertNull(order7, "Order is not expected since count is negative");

                UserWS userWS8 = api.getUserByCustomerMetaField("302", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "PQR Org-302", userWS8.getUserName());
                OrderWS order8 = api.getLatestOrder(userWS8.getId());
                assertNull(order8, "Order is not expected since non-billable org");
                // *****************************END for 2nd Org Hierarchy*****************************//

                // *****************************Asserts for 3rd Org Hierarchy*****************************//
                UserWS userWS9 = api.getUserByCustomerMetaField("400", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Top Org-2-400", userWS9.getUserName());
                OrderWS order9 = api.getLatestOrder(userWS9.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal("546.0000000000"), getAmount(order9));

                UserWS userWS10 = api.getUserByCustomerMetaField("500", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "1st Level-1-500", userWS10.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS9.getId()), Integer.valueOf(userWS10.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS9.getNextInvoiceDate(), userWS10.getNextInvoiceDate());
                OrderWS order10 = api.getLatestOrder(userWS10.getId());
                assertNull(order10, ORDR_NT_EXPCTD_EVNT_CONT_GIVN_BT_NO_PRNT_SUB_FOUND);

                UserWS userWS11 = api.getUserByCustomerMetaField("600", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "2nd level-1-600", userWS11.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS10.getId()), Integer.valueOf(userWS11.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS10.getNextInvoiceDate(), userWS11.getNextInvoiceDate());
                OrderWS order11 = api.getLatestOrder(userWS11.getId());
                assertNull(order11, ORDR_NT_EXPCTD_EVNT_CONT_GIVN_BT_NO_PRNT_SUB_FOUND);

                UserWS userWS12 = api.getUserByCustomerMetaField("800", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "3rd level-1-800", userWS12.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS11.getId()), Integer.valueOf(userWS12.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS11.getNextInvoiceDate(), userWS12.getNextInvoiceDate());
                OrderWS order12 = api.getLatestOrder(userWS12.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal("379.8000000000"), getAmount(order12));

                UserWS userWS13 = api.getUserByCustomerMetaField("900", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "4th level-1-900", userWS13.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS12.getId()), Integer.valueOf(userWS13.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS12.getNextInvoiceDate(), userWS13.getNextInvoiceDate());
                OrderWS order13 = api.getLatestOrder(userWS13.getId());
                assertNull(order13, ORDR_NT_EXPCTD_EVNT_CONT_GIVN_BT_NO_PRNT_SUB_FOUND);

                UserWS userWS14 = api.getUserByCustomerMetaField("601", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "2nd level-2-601", userWS14.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS10.getId()), Integer.valueOf(userWS14.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS10.getNextInvoiceDate(), userWS14.getNextInvoiceDate());
                OrderWS order14 = api.getLatestOrder(userWS14.getId());
                assertNull(order14, ORDR_NT_EXPCTD_EVNT_CONT_GIVN_BT_NO_PRNT_SUB_FOUND);

                UserWS userWS15 = api.getUserByCustomerMetaField("501", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "1st Level-2-501", userWS15.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS9.getId()), Integer.valueOf(userWS15.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS9.getNextInvoiceDate(), userWS15.getNextInvoiceDate());
                OrderWS order15 = api.getLatestOrder(userWS15.getId());
                assertNull(order15, "Order is not expected since non-billable org");

                UserWS userWS16 = api.getUserByCustomerMetaField("502", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "1st Level-3-502", userWS16.getUserName());
                assertEquals(PARENT_ID_SHOULD_BE_MATCH, Integer.valueOf(userWS9.getId()), Integer.valueOf(userWS16.getParentId()));
                assertEquals(PARENT_NID_AND_CHILD_NID_SHOULD_BE_MATCH, userWS9.getNextInvoiceDate(), userWS16.getNextInvoiceDate());
                OrderWS order16 = api.getLatestOrder(userWS16.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal("400.0000000000"), getAmount(order16));
                // *****************************END for 3rd Org Hierarchy*****************************//

                // *****************************Asserts for 4th Org Hierarchy*****************************//
                UserWS userWS17 = api.getUserByCustomerMetaField("700", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Top Org-3-700", userWS17.getUserName());
                OrderWS order17 = api.getLatestOrder(userWS17.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal("4.0000000000"), getAmount(order17));
                // *****************************END for 4th Org Hierarchy*****************************//

            });
    }

    /**
     * Change the user name of existing customer 'Beeans-1000' in XML file, there should no new user created for this
     */
    @Test(priority = 13, enabled = true)
    public void test013checkNIDandMainSubscription() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "checkNIDandMainSubscription.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(30000L);
        })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
                            UserWS userWS = api.getUserByCustomerMetaField("7860", META_FIELD_CUSTOMER_ORG_ID);

                            assertEquals(CUSTOMER_NOT_UPDATED, "Beans-7860", userWS.getUserName());
                            assertEquals("Customer mainsubscription period id must be equal to plugin parameter ", new Integer(2), userWS
                                    .getMainSubscription().getPeriodId());
                            assertEquals("Customer mainsubscription NextInvoiceDayOfPeriod must be equal to plugin parameter ", new Integer(1),
                                    userWS.getMainSubscription().getNextInvoiceDayOfPeriod());

                            BillingProcessConfigurationWS beforeExecution = api.getBillingProcessConfiguration();

                            Calendar nextRunDate = Calendar.getInstance();
                            nextRunDate.set(Calendar.YEAR, 2018);
                            nextRunDate.set(Calendar.MONTH, 0);
                            nextRunDate.set(Calendar.DAY_OF_MONTH, 1);

                            BillingProcessConfigurationWS afterExecution = beforeExecution;
                            afterExecution.setNextRunDate(nextRunDate.getTime());
                            afterExecution.setPeriodUnitId(PeriodUnitDTO.MONTH);
                            api.createUpdateBillingProcessConfiguration(afterExecution);

                            Calendar nextInvoiceDate = Calendar.getInstance();
                            nextInvoiceDate.set(Calendar.YEAR, 2017);
                            nextInvoiceDate.set(Calendar.MONTH, 10);
                            nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 15);

                            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
                            MainSubscriptionWS mainSubscriptionWS = userWS.getMainSubscription();
                            mainSubscriptionWS.setNextInvoiceDayOfPeriod(15);
                            userWS.setMainSubscription(mainSubscriptionWS);

                            List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
                            if (null != userWS.getMetaFields()) {
                                metaFieldValues.addAll(Arrays.asList(userWS.getMetaFields()));
                            }

                            MetaFieldValueWS orgIdMetaField = new MetaFieldValueWS();
                            orgIdMetaField.setFieldName(FIRST_NAME);
                            orgIdMetaField.setDataType(DataType.STRING);
                            orgIdMetaField.setValue("Org to Test");
                            orgIdMetaField.setGroupId(groupId);
                            metaFieldValues.add(orgIdMetaField);
                            userWS.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
                            userWS.setLanguageId(Constants.LANGUAGE_SPANISH_ID);

                            // update user with main subscription and next invoice date.
                        api.updateUser(userWS);
                        placeNextXMLToProcess(api, "checkNIDandMainSubscriptionofchildorgs.xml");
                        api.triggerScheduledTask(testEnvBuilder.env().idForCode(PLUGIN_CODE), new Date());
                        sleep(30000L);

                        userWS = api.getUserByCustomerMetaField("7860", META_FIELD_CUSTOMER_ORG_ID);
                        for (MetaFieldValueWS mfV : userWS.getMetaFields()) {
                            if (mfV.getMetaField().getName().equals(FIRST_NAME)) {
                                assertEquals("Customer AIT Value Updated", "Org to Test", mfV.getStringValue());
                            }
                        }

                        assertEquals("Customer Language ID should be SPANISH", Constants.LANGUAGE_SPANISH_ID, userWS.getLanguageId());
                        UserWS billableChildOrg = api.getUserByCustomerMetaField("7863", META_FIELD_CUSTOMER_ORG_ID);
                        assertEquals(CUSTOMER_NOT_UPDATED, "Manish-7863", billableChildOrg.getUserName());
                        assertEquals("Customer mainsubscription period id must be equal to plugin parameter ", new Integer(2), billableChildOrg
                                .getMainSubscription().getPeriodId());
                        assertEquals("Customer mainsubscription NextInvoiceDayOfPeriod must be equal to plugin parameter ", new Integer(1),
                                billableChildOrg.getMainSubscription().getNextInvoiceDayOfPeriod());

                        UserWS nonBillableChildOrg = api.getUserByCustomerMetaField("7864", META_FIELD_CUSTOMER_ORG_ID);
                        assertEquals(CUSTOMER_NOT_UPDATED, "Harshad-7864", nonBillableChildOrg.getUserName());
                        assertEquals("Customer mainsubscription period id must be equal to parent's mainsubscription ", new Integer(2),
                                nonBillableChildOrg.getMainSubscription().getPeriodId());
                        assertEquals("Customer mainsubscription NextInvoiceDayOfPeriod must be equal to parent's mainsubscription ", userWS
                                .getMainSubscription().getNextInvoiceDayOfPeriod(), nonBillableChildOrg.getMainSubscription()
                                .getNextInvoiceDayOfPeriod());

                        api.createUpdateBillingProcessConfiguration(beforeExecution);

                    });
    }

    /**
     * Test for JBMOV-183 Check Next Invoice Date after billing process for all childs in hierarchy.
     */
    @Test(priority = 14, enabled = true)
    public void test014checkNIDAfterBillingProcess() {
        testBuilder
                .given(envBuilder -> {
                    final JbillingAPI api = envBuilder.getPrancingPonyApi();
                    placeNextXMLToProcess(api, "12.org-hierarchy-billing-issue.xml");
                    api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
                    sleep(60000L);
                })
                .validate(
                        (testEnv, testEnvBuilder) -> {

                            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

                            testEnvBuilder.pluginBuilder(api).withCode(BASIC_BILLING_PROCESS_FITLER_TASK)
                                    .withTypeId(api.getPluginTypeWSByClassName(BASIC_BILLING_PROCESS_FITLER_TASK).getId())
                                    .withOrder(1)
                                    .build();

                            Date nextInvoiceDate = getDate(7, 01, 2016);
                            String[] orgIds = { "A2200", "A2210", "A2211", "A2212", "A2213",
                                    "A2214", "A2215", "A2216", "A2217", "A2218", "A2251", "A2252", "A2253", "A2254", "A2255" };
                            for (String orgId : orgIds) {
                                UserWS userWS = api.getUserByCustomerMetaField(orgId, META_FIELD_CUSTOMER_ORG_ID);
                                LOGGER.debug("customer to be updated ::: {}", userWS.getId());

                                userWS.setNextInvoiceDate(nextInvoiceDate);
                                api.updateUser(userWS);

                                userWS = api.getUserByCustomerMetaField(orgId, META_FIELD_CUSTOMER_ORG_ID);
                                assertEquals(CHECK_NEXT_INVOICE_DATE_OF_USER_AFTER + userWS.getUserName(),
                                        TestConstants.DATE_FORMAT.format(userWS.getNextInvoiceDate()),
                                        TestConstants.DATE_FORMAT.format(nextInvoiceDate));

                                OrderWS orderWS = api.getLatestOrder(userWS.getId());
                                if (null != orderWS) {
                                    Date activeSince = getDate(6, 01, 2016);
                                    orderWS.setActiveSince(activeSince);

                                    OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
                                    for (OrderChangeWS orderChangeWS : orderChanges) {
                                        orderChangeWS.setStartDate(activeSince);
                                    }
                                    api.updateOrder(orderWS, orderChanges);
                                    LOGGER.debug("order updated with active since date ::: {}", api.getOrder(orderWS.getId()).getActiveSince());
                                    orderChanges = api.getOrderChanges(orderWS.getId());
                                    for (OrderChangeWS orderChangeWS : orderChanges) {
                                        LOGGER.debug("order updated with active since date ::: {}", orderChangeWS.getStartDate());
                                    }
                                }
                            }
                            Date billingDate = getDate(7, 01, 2016);
                            triggerBilling(api, billingDate);

                            for (String orgId : orgIds) {
                                UserWS userWS = api.getUserByCustomerMetaField(orgId, META_FIELD_CUSTOMER_ORG_ID);
                                nextInvoiceDate = getDate(8, 01, 2016);
                                assertEquals(CHECK_NEXT_INVOICE_DATE_OF_USER_AFTER + userWS.getUserName(),
                                        TestConstants.DATE_FORMAT.format(userWS.getNextInvoiceDate()),
                                        TestConstants.DATE_FORMAT.format(nextInvoiceDate));

                            }
                            api.deletePlugin(api.getPluginsWS(api.getCallerCompanyId(), BASIC_BILLING_PROCESS_FITLER_TASK)[0].getId());
                        })
                .validate(
                        (testEnv, testEnvBuilder) -> {
                            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

                            testEnvBuilder.pluginBuilder(api).withCode(SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK)
                                    .withTypeId(api.getPluginTypeWSByClassName(SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK).getId())
                                    .withOrder(1)
                                    .build();

                            Date billingDate = getDate(8, 01, 2016);
                            triggerBilling(api, billingDate);

                            String[] orgIds = { "A2200", "A2210", "A2211", "A2212", "A2213",
                                    "A2214", "A2215", "A2216", "A2217", "A2218", "A2251", "A2252", "A2253", "A2254", "A2255" };
                            for (String orgId : orgIds) {
                                UserWS userWS = api.getUserByCustomerMetaField(orgId, META_FIELD_CUSTOMER_ORG_ID);
                                Date nextInvoiceDate = getDate(9, 01, 2016);
                                assertEquals(CHECK_NEXT_INVOICE_DATE_OF_USER_AFTER + userWS.getUserName(),
                                        TestConstants.DATE_FORMAT.format(userWS.getNextInvoiceDate()),
                                        TestConstants.DATE_FORMAT.format(nextInvoiceDate));
                            }
                        });
    }

    @Test(priority = 15, enabled = true)
    public void test015OrgHierarchyWithUseParentPrice() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "15.org-hierarchy-US.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(30000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            // *****************************Asserts for 1st Org Hierarchy*****************************//
                UserWS userWS1 = api.getUserByCustomerMetaField("15190", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Top Org-15-15190", userWS1.getUserName());
                assertTrue(!userWS1.getUseParentPricing(), USE_PARENT_PRICING_SHOULD_BE_FALSE);
                OrderWS order1 = api.getLatestOrder(userWS1.getId());
                assertNull(order1, "Order is not expected since no subscription and count");
                UserWS userWS2 = api.getUserByCustomerMetaField("15191", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "Child Org 1st level-15191", userWS2.getUserName());
                assertTrue(userWS2.getUseParentPricing(), "Use Parent Pricing should be true");
                OrderWS order2 = api.getLatestOrder(userWS2.getId());
                assertEquals(ORDER_NOT_CREATED, new BigDecimal(THREE_ONE_FIVE_POINT_THREE), getAmount(order2));
                UserWS userWS3 = api.getUserByCustomerMetaField("15192", META_FIELD_CUSTOMER_ORG_ID);
                assertEquals(CUSTOMER_NOT_CREATED, "XYZ Org 1st level-15192", userWS3.getUserName());
                assertTrue(userWS3.getUseParentPricing(), "Use Parent Pricing should be true");
                // testing scenario after an update on parentPricing checkbox for non-billable user
                userWS3.setUseParentPricing(false);
                api.updateUser(userWS3);
                assertTrue(!userWS3.getUseParentPricing(), USE_PARENT_PRICING_SHOULD_BE_FALSE);
            }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "15.org-hierarchy-US.xml");
            api.triggerScheduledTask(testEnvBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(30000L);
            UserWS userWS3 = api.getUserByCustomerMetaField("15192", META_FIELD_CUSTOMER_ORG_ID);
            assertTrue(!userWS3.getUseParentPricing(), USE_PARENT_PRICING_SHOULD_BE_FALSE);
        });
    }

    @Test(priority = 16, enabled = true)
    public void test016CreateOrgHierarchyForOrderLineDeletionScanario() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            LOGGER.debug("Api:::::: Start");
            placeNextXMLToProcess(api, "10.org-hierarchy-JBMOV-218-Create.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(40000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

            UserWS userWS = api.getUserByCustomerMetaField("152A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, AUTOMATION_ORGANIZATION_152A, userWS.getUserName());
            OrderWS order = api.getLatestOrder(api.getUserId(AUTOMATION_ORGANIZATION_152A));
            assertNull(order, NO_ORDERS_EXPECTED);

            UserWS userWS1 = api.getUserByCustomerMetaField("449A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, PLOKIJ_449A, userWS1.getUserName());
            OrderWS order1 = api.getLatestOrder(api.getUserId(PLOKIJ_449A));
            assertNull(order1, NO_ORDERS_EXPECTED);

            UserWS userWS2 = api.getUserByCustomerMetaField("460A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "460OrgforEAP-460A", userWS2.getUserName());
            OrderWS order2 = api.getLatestOrder(userWS2.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("300.0000000000"), getAmount(order2));

            UserWS userWS3 = api.getUserByCustomerMetaField("461A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "461OrgforEAP-461A", userWS3.getUserName());
            OrderWS order3 = api.getLatestOrder(userWS3.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("200.0000000000"), getAmount(order3));

            UserWS userWS4 = api.getUserByCustomerMetaField("459A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, ORGFOR_EAP_459A, userWS4.getUserName());
            OrderWS order4 = api.getLatestOrder(api.getUserId(ORGFOR_EAP_459A));
            assertNull(order4, NO_ORDERS_EXPECTED);

            UserWS userWS5 = api.getUserByCustomerMetaField("450A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, TEST450_450A, userWS5.getUserName());
            OrderWS order5 = api.getLatestOrder(api.getUserId(TEST450_450A));
            assertNull(order5, NO_ORDERS_EXPECTED);

            UserWS userWS6 = api.getUserByCustomerMetaField("612A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, J_BILLING_TOP_ORG_612A, userWS6.getUserName());
            OrderWS order6 = api.getLatestOrder(api.getUserId(J_BILLING_TOP_ORG_612A));
            assertNull(order6, NO_ORDERS_EXPECTED);

            UserWS userWS7 = api.getUserByCustomerMetaField("615A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in Dollar-615A", userWS7.getUserName());
            OrderWS order7 = api.getLatestOrder(userWS7.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal(FIFTEEN), getAmount(order7));

            UserWS userWS8 = api.getUserByCustomerMetaField("616A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in Euro-616A", userWS8.getUserName());
            OrderWS order8 = api.getLatestOrder(userWS8.getId());
            assertEquals(ORDER_CREATED, new BigDecimal(THIRTY), getAmount(order8));

            UserWS userWS9 = api.getUserByCustomerMetaField("617A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in GBP-617A", userWS9.getUserName());
            OrderWS order9 = api.getLatestOrder(userWS9.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("45.0000000000"), getAmount(order9));

            UserWS userWS10 = api.getUserByCustomerMetaField("618A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in Yen-618A", userWS10.getUserName());
            OrderWS order10 = api.getLatestOrder(userWS10.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("60.0000000000"), getAmount(order10));

            UserWS userWS11 = api.getUserByCustomerMetaField("613A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "GC2-613A", userWS11.getUserName());
            OrderWS order11 = api.getLatestOrder(userWS11.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("300.0000000000"), getAmount(order11));

            UserWS userWS12 = api.getUserByCustomerMetaField("784A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 2 GBP-784A", userWS12.getUserName());
            OrderWS order12 = api.getLatestOrder(userWS12.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("10.0000000000"), getAmount(order12));

            UserWS userWS13 = api.getUserByCustomerMetaField("785A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 3 Euro-785A", userWS13.getUserName());
            OrderWS order13 = api.getLatestOrder(userWS13.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("20.0000000000"), getAmount(order13));

            UserWS userWS14 = api.getUserByCustomerMetaField("786A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 4 Yen-786A", userWS14.getUserName());
            OrderWS order14 = api.getLatestOrder(userWS14.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal(THIRTY), getAmount(order14));

            UserWS userWS15 = api.getUserByCustomerMetaField("452A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "testingsuborg-452A", userWS15.getUserName());
            OrderWS order15 = api.getLatestOrder(userWS15.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal("78.0000000000"), getAmount(order15));

            UserWS userWS16 = api.getUserByCustomerMetaField("1234", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, TEST_1234, userWS16.getUserName());
            OrderWS order16 = api.getLatestOrder(api.getUserId(TEST_1234));
            assertNull(order16, NO_ORDERS_EXPECTED);

        });
    }

    @Test(priority = 17, enabled = true)
    public void test017UpdateOrgHierarchyForOrderLineDeletionScanario() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            LOGGER.debug("Api:::::: Start");
            placeNextXMLToProcess(api, "11.org-hierarchy-JBMOV-218-update.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(40000L);
            LOGGER.debug("Api:::::: end");
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

            UserWS userWS = api.getUserByCustomerMetaField("152A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, AUTOMATION_ORGANIZATION_152A, userWS.getUserName());
            OrderWS order = api.getLatestOrder(api.getUserId(AUTOMATION_ORGANIZATION_152A));
            assertNull(order, NO_ORDERS_EXPECTED);

            UserWS userWS1 = api.getUserByCustomerMetaField("449A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, PLOKIJ_449A, userWS1.getUserName());
            OrderWS order1 = api.getLatestOrder(api.getUserId(PLOKIJ_449A));
            assertNull(order1, NO_ORDERS_EXPECTED);

            UserWS userWS2 = api.getUserByCustomerMetaField("460A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "460OrgforEAP-460A", userWS2.getUserName());

            UserWS userWS3 = api.getUserByCustomerMetaField("461A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "461OrgforEAP-461A", userWS3.getUserName());

            UserWS userWS4 = api.getUserByCustomerMetaField("459A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, ORGFOR_EAP_459A, userWS4.getUserName());
            OrderWS order4 = api.getLatestOrder(api.getUserId(ORGFOR_EAP_459A));
            assertNull(order4, NO_ORDERS_EXPECTED);

            UserWS userWS5 = api.getUserByCustomerMetaField("450A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, TEST450_450A, userWS5.getUserName());
            OrderWS order5 = api.getLatestOrder(api.getUserId(TEST450_450A));
            assertNull(order5, NO_ORDERS_EXPECTED);

            UserWS userWS6 = api.getUserByCustomerMetaField("612A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, J_BILLING_TOP_ORG_612A, userWS6.getUserName());
            OrderWS order6 = api.getLatestOrder(api.getUserId(J_BILLING_TOP_ORG_612A));
            assertNull(order6, NO_ORDERS_EXPECTED);

            UserWS userWS7 = api.getUserByCustomerMetaField("615A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in Dollar-615A", userWS7.getUserName());
            OrderWS order7 = api.getLatestOrder(userWS7.getId());
            assertEquals(ORDER_NOT_CREATED, new BigDecimal(THIRTY), getAmount(order7));

            UserWS userWS8 = api.getUserByCustomerMetaField("616A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in Euro-616A", userWS8.getUserName());
            OrderWS order8 = api.getLatestOrder(userWS8.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order8.getOrderLines().length);

            UserWS userWS9 = api.getUserByCustomerMetaField("617A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in GBP-617A", userWS9.getUserName());
            OrderWS order9 = api.getLatestOrder(userWS9.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order9.getOrderLines().length);

            UserWS userWS10 = api.getUserByCustomerMetaField("618A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Billing in Yen-618A", userWS10.getUserName());
            OrderWS order10 = api.getLatestOrder(userWS10.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order10.getOrderLines().length);

            UserWS userWS11 = api.getUserByCustomerMetaField("613A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "GC2-613A", userWS11.getUserName());
            OrderWS order11 = api.getLatestOrder(userWS11.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order11.getOrderLines().length);

            UserWS userWS12 = api.getUserByCustomerMetaField("783A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 1 Dollar-783A", userWS12.getUserName());
            OrderWS order12 = api.getLatestOrder(userWS12.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order12.getOrderLines().length);

            UserWS userWS13 = api.getUserByCustomerMetaField("787A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "DollarNoInvoice-787A", userWS13.getUserName());
            OrderWS order13 = api.getLatestOrder(api.getUserId("DollarNoInvoice-787A"));
            assertNull(order13, NO_ORDERS_EXPECTED);

            UserWS userWS14 = api.getUserByCustomerMetaField("784A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 2 GBP-784A", userWS14.getUserName());
            OrderWS order14 = api.getLatestOrder(userWS14.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order14.getOrderLines().length);

            UserWS userWS15 = api.getUserByCustomerMetaField("788A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "GBPNoInvoice-788A", userWS15.getUserName());
            OrderWS order15 = api.getLatestOrder(api.getUserId("GBPNoInvoice-788A"));
            assertNull(order15, NO_ORDERS_EXPECTED);

            UserWS userWS16 = api.getUserByCustomerMetaField("785A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 3 Euro-785A", userWS16.getUserName());
            OrderWS order16 = api.getLatestOrder(userWS16.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order16.getOrderLines().length);

            UserWS userWS17 = api.getUserByCustomerMetaField("1232A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "EuroNoInvoice-1232A", userWS17.getUserName());
            OrderWS order17 = api.getLatestOrder(api.getUserId("EuroNoInvoice-1232A"));
            assertNull(order17, NO_ORDERS_EXPECTED);

            UserWS userWS18 = api.getUserByCustomerMetaField("786A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Sub org 4 Yen-786A", userWS18.getUserName());
            OrderWS order18 = api.getLatestOrder(userWS18.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order18.getOrderLines().length);

            UserWS userWS19 = api.getUserByCustomerMetaField("1233A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "YenNoInvoice-1233A", userWS19.getUserName());
            OrderWS order19 = api.getLatestOrder(api.getUserId("YenNoInvoice-1233A"));
            assertNull(order19, NO_ORDERS_EXPECTED);

            UserWS userWS20 = api.getUserByCustomerMetaField("452A", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "testingsuborg-452A", userWS20.getUserName());
            OrderWS order20 = api.getLatestOrder(userWS20.getId());
            assertEquals(ORDER_LINE_FOUND, 0, order20.getOrderLines().length);

            UserWS userWS21 = api.getUserByCustomerMetaField("1234", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, TEST_1234, userWS21.getUserName());
            OrderWS order21 = api.getLatestOrder(api.getUserId(TEST_1234));
            assertNull(order21, NO_ORDERS_EXPECTED);

            UserWS userWS22 = api.getUserByCustomerMetaField("615BB", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "child-615BB", userWS22.getUserName());
            OrderWS order22 = api.getLatestOrder(userWS22.getId());
            assertNull(order22, NO_ORDERS_EXPECTED);

        });
    }

    @Test(priority = 18, enabled = true)
    public void test018OrgHierarchyWithDifferntCurrency() {

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "18_different-currency.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            UserWS userWS = api.getUserByCustomerMetaField("786080", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS01 = api.getUserByCustomerMetaField("786081", META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS02 = api.getUserByCustomerMetaField("786082", META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_CREATED, "Currency Test-786080", userWS.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "Child Currency Test-01-786081", userWS01.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "Child Currency Test-02-786082", userWS02.getUserName());

            userWS.setCurrencyId(AUSTRALIAN_DOLLOR_CURRENCY_ID);
            api.updateUser(userWS);
            userWS01.setCurrencyId(AUSTRALIAN_DOLLOR_CURRENCY_ID);
            api.updateUser(userWS01);
            userWS02.setCurrencyId(AUSTRALIAN_DOLLOR_CURRENCY_ID);
            api.updateUser(userWS02);

            placeNextXMLToProcess(api, "18_different-currency_add_new-child-org.xml");
            api.triggerScheduledTask(testEnvBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
            UserWS userWS03 = api.getUserByCustomerMetaField("786083", META_FIELD_CUSTOMER_ORG_ID);
            assertEquals(CUSTOMER_NOT_CREATED, "Child Currency Test-03-786083", userWS03.getUserName());
            assertEquals("Child's currency should be equal to parent", userWS.getCurrencyId(), userWS03.getCurrencyId());
        });
    }

    /**
     * JBMOV-227 - issue fix test case for multilevel non-billable childs
     */
    @Test(priority = 19, enabled = true)
    public void test019CheckUsersOrdersCreatedFromXML() {
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            placeNextXMLToProcess(api, "19.org-hierarchy-multilevel-quantity-issue.xml");
            api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
            sleep(20000L);
        }).validate((testEnv, testEnvBuilder) -> {
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            String orgId956560 = "956560";
            String orgId956561 = "956561";
            String orgId956562 = "956562";
            String orgId956563 = "956563";
            String orgId956564 = "956564";
            String orgId956565 = "956565";
            UserWS userWS00 = api.getUserByCustomerMetaField(orgId956560, META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS01 = api.getUserByCustomerMetaField(orgId956561, META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS02 = api.getUserByCustomerMetaField(orgId956562, META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS03 = api.getUserByCustomerMetaField(orgId956563, META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS04 = api.getUserByCustomerMetaField(orgId956564, META_FIELD_CUSTOMER_ORG_ID);
            UserWS userWS05 = api.getUserByCustomerMetaField(orgId956565, META_FIELD_CUSTOMER_ORG_ID);

            assertEquals(CUSTOMER_NOT_CREATED, "CustomerA-" + orgId956560, userWS00.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "CustomerB-" + orgId956561, userWS01.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "CustomerC-" + orgId956562, userWS02.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "CustomerD-" + orgId956563, userWS03.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "CustomerE-" + orgId956564, userWS04.getUserName());
            assertEquals(CUSTOMER_NOT_CREATED, "CustomerF-" + orgId956565, userWS05.getUserName());

            OrderWS order00 = api.getLatestOrder(userWS00.getId());
            assertNotNull(ORDER_NOT_CREATED, order00);
            OrderWS order01 = api.getLatestOrder(userWS01.getId());
            assertNull(order01, ORDER_IS_NOT_EXPECTED);
            OrderWS order02 = api.getLatestOrder(userWS02.getId());
            assertNull(order02, ORDER_IS_NOT_EXPECTED);
            OrderWS order03 = api.getLatestOrder(userWS03.getId());
            assertNull(order03, ORDER_IS_NOT_EXPECTED);
            OrderWS order04 = api.getLatestOrder(userWS04.getId());
            assertNull(order04, ORDER_IS_NOT_EXPECTED);
            OrderWS order05 = api.getLatestOrder(userWS05.getId());
            assertNull(order05, ORDER_IS_NOT_EXPECTED);

            BigDecimal quantity = BigDecimal.ZERO; 
            for (OrderLineWS ol : order00.getOrderLines()) {
                quantity = quantity.add(ol.getQuantityAsDecimal());
            }
            assertEquals("Order quantity should be 22", new BigDecimal("22.0000000000"), quantity);
            assertEquals("Order amount should be 110", new BigDecimal("110.0000000000"), getAmount(order00));
        });
    }

    private void triggerBilling(JbillingAPI api, Date date) {

        BillingProcessConfigurationWS billingConfig = api.getBillingProcessConfiguration();

        billingConfig.setNextRunDate(date);
        billingConfig.setOnlyRecurring(0);
        billingConfig.setGenerateReport(0);
        billingConfig.setPeriodUnitId(Constants.PERIOD_UNIT_MONTH);
        // update the billing process configuration
        api.createUpdateBillingProcessConfiguration(billingConfig);
        api.triggerBilling(date);
    }

    private Date getDate(int month, int day, int year) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.YEAR, year);
        return cal.getTime();
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer... paymentMethodTypeId) {
        // create a valid meta field
        MetaFieldWS fnMetaField = new MetaFieldBuilder()
                .dataType(DataType.STRING)
                .entityType(EntityType.ACCOUNT_TYPE)
                .name(FIRST_NAME)
                .build();

        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();

        // build account information type without account type
        AccountInformationTypeWS ait = new AccountInformationTypeBuilder(accountTypeWS)
                .addMetaField(fnMetaField)
                .build();
        // can't save ait with no data. Exception is expected
        groupId = api.createAccountInformationType(ait);
        return accountTypeWS.getId();
    }

    private Integer buildAndPersistCategory(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, boolean global,
            ItemBuilder.CategoryType categoryType) {
        return envBuilder.itemBuilder(api)
                .itemType()
                .withCode(code)
                .withCategoryType(categoryType)
                .global(global)
                .build();
    }

    private Integer buildAndPersistFlatProduct(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code,
            boolean global, Integer categoryId, String flatPrice, boolean allowDecimal) {
        return envBuilder.itemBuilder(api)
                .item()
                .withCode(code)
                .withType(categoryId)
                .withFlatPrice(flatPrice)
                .global(global)
                .allowDecimal(allowDecimal)
                .build();
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value = new MetaFieldBuilder()
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

    private void setCompanyLevelMetaField(TestEnvironment environment) {
        JbillingAPI api = environment.getPrancingPonyApi();
        CompanyWS company = api.getCompany();
        List<MetaFieldValueWS> values = new ArrayList<>();
        values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));

        values.add(new MetaFieldValueWS(META_FIELD_TATA_CALL_ITEM_ID, null, DataType.STRING, true,
                environment.idForCode(TATA_CALL_ITEM).toString()));
        values.add(new MetaFieldValueWS(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER, null, DataType.STRING, true,
                environment.idForCode(TATA_CALL_ITEM).toString()));
        values.add(new MetaFieldValueWS(ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY, null, DataType.STRING, true,
                environment.idForCode(ACCOUNT_NAME).toString()));
        int entityId = api.getCallerCompanyId();
        LOGGER.debug("Created Company Level MetaFields {}", values);
        values.forEach(value ->
                value.setEntityId(entityId)
                );
        company.setTimezone(company.getTimezone());
        company.setMetaFields(values.toArray(new MetaFieldValueWS[values.size()]));
        api.updateCompany(company);

    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void placeNextXMLToProcess(JbillingAPI api, String nextFileName) {
        PluggableTaskWS pluggableTaskWS = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME)
                .getId());
        Map<String, String> parameters = pluggableTaskWS.getParameters();

        File currDir = new File(parameters.get(PARAM_NAME_XML_BASE_DIRECTORY) + File.separator + PRE_DEFINED_XML_DIR);
        File[] xmlFilesList = currDir.listFiles(file -> file.getName().endsWith(".xml"));
        Arrays.sort(xmlFilesList, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

        for (File f : xmlFilesList) {
            if (f.isFile() && f.getName().equalsIgnoreCase(nextFileName)) {
                File renamedFile = new File(parameters.get(PARAM_NAME_XML_BASE_DIRECTORY) + File.separator + f.getName());
                f.renameTo(renamedFile);
                break;
            }
        }
    }

    private BigDecimal getAmount(OrderWS orderWS) {
        BigDecimal amount = BigDecimal.ZERO;

        for (OrderLineWS ol : orderWS.getOrderLines()) {
            amount = amount.add(ol.getAmountAsDecimal());
        }

        return amount;
    }
}
