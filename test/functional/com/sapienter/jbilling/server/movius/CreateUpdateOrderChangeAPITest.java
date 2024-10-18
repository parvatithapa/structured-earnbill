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

import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.movius.integration.MoviusConstants;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.OrderBuilder;

/**
 * @author Harshad Pathan
 * @since 03-01-2018
 */
@Test(groups = { "test-movius", "movius" }, testName = "CreateUpdateOrderChangeAPITest")
public class CreateUpdateOrderChangeAPITest {

	private static final Logger logger = LoggerFactory.getLogger(CreateUpdateOrderChangeAPITest.class);
	private static EnvironmentHelper envHelper;
	private static TestBuilder testBuilder;
	private static final Integer PRANCING_PONY = 1;
	private static final String BASE_DIR = Util.getSysProp("base_dir");
	private static final String MOVIUS_TEST = "movius-test";
	private static final String XSD_DIR = "xsd";
	private static final String ORIGINATION_DIR = concatenateString(BASE_DIR, MoviusConstants.ORG_DIR);
	private static final String ORIGINATION_XSD_PATH = concatenateString(BASE_DIR, MOVIUS_TEST, File.separator, XSD_DIR);
	private static final String ORIGINATION_PRANCING_PONY_DIR = concatenateString(ORIGINATION_DIR, File.separator, PRANCING_PONY.toString());

	private String MoviusOriginationChargesCreateUpdateTaskName = "com.sapienter.jbilling.server.order.task.MoviusOriginationChargesCreateUpdateTask";

	public final String TAG_NAME_CHARGES = "charges";
	public final String TAG_NAME_ORG_ID = "org-id";
	public final String TAG_NAME_COUNT = "count";
	public final String TAG_NAME_NAME = "name";
	public static final String ORIGINATION_XML = "Origination.xml";
	public static final String ANVEO_AUSTRALIA = "Anveo-Australia";

	public static final String SUBSCRIPTION_CHARGES = "Subscription Charges";
	public static final String TATA_UK = "Tata-UK";
	private static final String ORG_ID_MF_NAME = "Org Id";
	public static final String MOVIUS_ORIGINATION_PLUGIN_CODE = "Movis-Origination-Plugin";
	public static final String MOVIUS_ORIGINATION_ITEM_TYPE_CODE = "Origination";
	public static final String MOVIUS_SUBSCRIPTION_ITEM_TYPE_CODE = "Subscription";

	public static final String MOVIUS_ORIGINATION_ACCOUNT_TYPE = "Movis-Origination-account-type";
	private final static Integer CC_PM_ID = 5;
	private final static Integer PLUGIN_ORDER = 77;
	private final static Integer ALLOW_ASSET_MANAGEMENT = 0;
	private static final String XML_BASE_DIRECTORY = Util.getSysProp("base_dir") + "movius-test/xml";
	private static final String XSD_BASE_DIRECTORY = Util.getSysProp("base_dir") + "movius-test/xsd";

	private final static String  META_FIELD_BILLING_PLAN_ID                    = "Billing Plan Id";
	private final static String  META_FIELD_BILLING_PLAN_NAME                  = "Billing Plan Name";
	private final static String  META_FIELD_TIMEZONE                           = "Timezone";
	private final static String  META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER = "Set Item Id for Org Hierarchy Order";
	private final static String  META_FIELD_SET_ITEM_ID_FOR_ORG_ORIGINATION = "Origination Item type";
	private final static String  ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY 				= "Set Account Type Id for Org Hierarchy User";
	private String PRE_DEFINED_XML_DIR = "pre-defined";
	private static final String MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME =
			"com.sapienter.jbilling.server.process.task.MoviusOrgHierarchyMappingTask";
	protected static final String PLUGIN_CODE = "Plugin-Code";

	protected static final String PLUGIN_CODE_INVOICE = "Plugin-Code-Invoice" ;
	private static final String MOVIUSINVOICECOMPOSITIONTASK = "com.sapienter.jbilling.server.pluggableTask.MoviusInvoiceCompositionTask";
	private static final String ORDERCHANGEBASEDCPMPOSITIONTASK = "com.sapienter.jbilling.server.pluggableTask.OrderChangeBasedCompositionTask";

	protected static final String ChangeBasedCompositionTask  = "Plugin-Code-CompositionTask" ;
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
		return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
			this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
		});
	}

	public static String concatenateString (String... strings) {
		StringBuilder builder = new StringBuilder();
		for (String arg : strings) {
			builder.append(arg);
		}
		return builder.toString();
	}

	private void createItemType(final boolean global,String code){
		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			envBuilder.itemBuilder(api)
			.itemType()
			.withCode(code)
			.useExactCode(true)
			.global(global)
			.allowAssetManagement(ALLOW_ASSET_MANAGEMENT)
			.withEntities(new Integer[]{PRANCING_PONY})
			.build();
		}).test((testEnv, testEnvBuilder) -> {
			assertNotNull("Item Type Creation Failed", testEnvBuilder.idForCode(code));
		});

	}

	protected static void createItem(boolean global, String code, String itemTypeCode){
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
			.withType(envBuilder.idForCode(itemTypeCode))
			.build();
		}).test((testEnv, testEnvBuilder) -> {
			assertNotNull("Item Creation Failed", testEnvBuilder.idForCode(code));
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
			logger.error("Exception : {}", ex);
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
			.addMetaField(META_FIELD_SET_ITEM_ID_FOR_ORG_ORIGINATION,DataType.STRING, EntityType.COMPANY)
			.addMetaField(META_FIELD_BILLING_PLAN_ID, DataType.INTEGER, EntityType.CUSTOMER)
			.addMetaField(META_FIELD_BILLING_PLAN_NAME, DataType.STRING, EntityType.CUSTOMER)
			.addMetaField(META_FIELD_TIMEZONE, DataType.STRING, EntityType.CUSTOMER)
			.build();
		});
	}

	private void setCompanyLevelMetaField(TestEnvironment environment) {
		JbillingAPI api = environment.getPrancingPonyApi();
		CompanyWS company = api.getCompany();
		List<MetaFieldValueWS> values = new ArrayList<>();
		values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));

		values.add(new MetaFieldValueWS(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER, null, DataType.STRING, true,
				environment.idForCode(SUBSCRIPTION_CHARGES).toString()));
		values.add(new MetaFieldValueWS(ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY, null, DataType.STRING, true,
				environment.idForCode(MOVIUS_ORIGINATION_ACCOUNT_TYPE).toString()));
		values.add(new MetaFieldValueWS(META_FIELD_SET_ITEM_ID_FOR_ORG_ORIGINATION, null, DataType.STRING, true,
				environment.idForCode(ANVEO_AUSTRALIA).toString()));


		int entityId = api.getCallerCompanyId();
		logger.debug("Created Company Level MetaFields {}", values);
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
			AccountTypeWS accountType = envBuilder.accountTypeBuilder(api)
					.withName(MOVIUS_ORIGINATION_ACCOUNT_TYPE)
					.withPaymentMethodTypeIds(new Integer[]{CC_PM_ID})
					.build();

			accountType.setInvoiceTemplateId(1);
			api.updateAccountType(accountType);
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
		createItemType(true,MOVIUS_ORIGINATION_ITEM_TYPE_CODE);
		createItemType(true, MOVIUS_SUBSCRIPTION_ITEM_TYPE_CODE);
		createItem(true, ANVEO_AUSTRALIA,MOVIUS_ORIGINATION_ITEM_TYPE_CODE);
		createItem(true, SUBSCRIPTION_CHARGES,MOVIUS_SUBSCRIPTION_ITEM_TYPE_CODE);
		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			buildAndPersistMetafield(testBuilder, ACCOUNT_TYPE_FOR_ID_ORG_HIERARCHY, DataType.STRING, EntityType.COMPANY);

			// Setting Company Level Meta Fields
			setCompanyLevelMetaField(testBuilder.getTestEnvironment());
			PluggableTaskTypeWS MoviusOriginationChargesCreateUpdateTaskType = api.getPluginTypeWSByClassName(MoviusOriginationChargesCreateUpdateTaskName);
			envBuilder.pluginBuilder(api)
			.withCode(MOVIUS_ORIGINATION_PLUGIN_CODE)
			.withTypeId(MoviusOriginationChargesCreateUpdateTaskType.getId())
			.withOrder(PLUGIN_ORDER)
			.withParameter(MoviusConstants.ORIGINATION_XML_PARAMETER_BASE_DIR, ORIGINATION_PRANCING_PONY_DIR)
			.withParameter(MoviusConstants.ORIGINATION_XSD_PARAMETER_BASE_DIR, ORIGINATION_XSD_PATH)
			.build();

			// configuring plugin 
			envBuilder.pluginBuilder(api)
			.withCode(PLUGIN_CODE)
			.withTypeId(api.getPluginTypeWSByClassName(MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME).getId())
			.withOrder(80)
			.withParameter("XML Base Directory", XML_BASE_DIRECTORY)
			.withParameter("XSD Base Directory", XSD_BASE_DIRECTORY)
			.withParameter("billing cycle period Id", "2")
			.withParameter("billing cycle day", "1")
			.build();

			api.deletePlugin(api.getPluginsWS(1,ORDERCHANGEBASEDCPMPOSITIONTASK)[0].getId());

			envBuilder.pluginBuilder(api).withCode(PLUGIN_CODE_INVOICE)
			.withTypeId(api.getPluginTypeWSByClassName(MOVIUSINVOICECOMPOSITIONTASK).getId())
			.withOrder(91)
			.build();

		}).test((testEnv, testEnvBuilder) -> {
			assertNotNull("Plugin Creation Failed", testEnvBuilder.idForCode(MOVIUS_ORIGINATION_PLUGIN_CODE));
		});
	}

	private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
		JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
		MetaFieldValueWS[] metaFieldValueWSArray = api.getCompany().getMetaFields();
		MetaFieldValueWS subscriptionMetaField = Arrays.stream(metaFieldValueWSArray).filter(metaFieldValueWS -> metaFieldValueWS.getFieldName().equals(META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER)).findFirst().orElse(null);
		Integer id = null;
		if(null == subscriptionMetaField){
			logger.debug("Creating subscription metaField {}", META_FIELD_SET_ITEM_ID_FOR_ORG_HIERARCHY_ORDER);
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
		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			envBuilder.pluginBuilder(api)
			.withCode(ChangeBasedCompositionTask)
			.withTypeId(api.getPluginTypeWSByClassName(ORDERCHANGEBASEDCPMPOSITIONTASK).getId())
			.withOrder(786)
			.build();
		});
		if (null != envHelper) {
			envHelper = null;
		}
		if (null != testBuilder) {
			testBuilder = null;
		}
	}

	private void placeNextXMLToProcess(JbillingAPI api, String nextFileName) {
		PluggableTaskWS pluggableTaskWS = api.getPluginWSByTypeId(api.getPluginTypeWSByClassName(MOVIUS_ORG_HIERARCHY_MAPPING_TASK_CLASS_NAME).getId());
		Map<String, String> parameters = pluggableTaskWS.getParameters(); 

		File currDir = new File(parameters.get("XML Base Directory") + File.separator + PRE_DEFINED_XML_DIR);
		File[] xmlFilesList = currDir.listFiles(file -> file.getName().endsWith(".xml"));
		Arrays.sort(xmlFilesList, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

		for(File f : xmlFilesList){
			if(f.isFile() && f.getName().equalsIgnoreCase(nextFileName)){
				File renamedFile = new File(parameters.get("XML Base Directory") + File.separator + f.getName());
				f.renameTo(renamedFile);
				break;
			}
		}
	}

	@Test
	public void test001CompleteCycle() throws Exception {

		BigDecimal charges = new BigDecimal("10.00");
		Integer count = new Integer("7");

		String case_0 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
		case_0 = replaceTagWithProperValue(case_0, TAG_NAME_ORG_ID, null, "78600");
		case_0 = replaceTagWithProperValue(case_0, TAG_NAME_COUNT, null, count.toString());
		createXmlFile(ORIGINATION_XML, case_0);

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "createupdateorderchange.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

			//run ORIGINATION_XML
			schedulePlugin(api, envBuilder);
			sleep(80000);
		});
	}

	/**
	 * This test case is only for Subscription. ie only Org hierarchy
	 * JBMOV-168
	 * 1) User 78602 subscribe 10 active subscriptions on 1st Nov 2017.
	 * 2) Increment the quantity for user id 78602, productCode Subscription by 5 on 15th Nov.
	 * 3) Call API method as follows:
	 * createUpdateOrderChange (78602, ‘Subscription’, null, new BigDecimal(15), ‘2017-11-15’)
	 * 
	 */
	@Test
	public void testUserStory001() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "userstory001.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

			UserWS userWS= api.getUserByCustomerMetaField("78602", ORG_ID_MF_NAME);
			logger.debug("User created  {} ", userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			Date activeSince = FullCreativeUtil.getDate(10,01,2017);
			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(activeSince);

			Date expectedStartDate = FullCreativeUtil.getDate(10,01,2017);
			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				assertOrderChange(orderChangeWS, new Date(), new BigDecimal(2), "10");
				orderChangeWS.setStartDate(expectedStartDate);
			}

			api.updateOrder(orderWS, orderChanges);
			orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				assertOrderChange(orderChangeWS, expectedStartDate, new BigDecimal(2), "10");
			}

			Date changeEffectiveDate = FullCreativeUtil.getDate(10,15,2017);

			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,
					new BigDecimal(2), new BigDecimal(15), changeEffectiveDate);

			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);
			InvoiceWS invoice = api.getLatestInvoice(userWS.getId());
			InvoiceLineDTO[] lines = invoice.getInvoiceLines();

			logger.debug("lines[0] {} price {} quantity {} amount  {} ",lines[0].getDescription(),
					lines[0].getPriceAsDecimal(), lines[0].getQuantityAsDecimal(), lines[0].getAmountAsDecimal());
			logger.debug("lines[1] {} price {} quantity {} amount  {} ",lines[1].getDescription(),
					lines[1].getPriceAsDecimal(), lines[1].getQuantityAsDecimal(), lines[1].getAmountAsDecimal());

			lines = getSortedInvoiceLine(lines);

			assertTrue(lines[0].getDescription().contains("Period from 11/01/2017 to 11/14/2017"));
			assertTrue(lines[1].getDescription().contains("Period from 11/15/2017 to 11/30/2017"));
			assertInvoiceLine(lines[0],  new BigDecimal(2),new BigDecimal(10), new BigDecimal(9.33).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			assertInvoiceLine(lines[1],  new BigDecimal(2),new BigDecimal(15), new BigDecimal(16).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			nextInvoiceDate = FullCreativeUtil.getDate(00,01,2018);
			userWS = api.getUserWS(userWS.getId());
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);
			lines = api.getLatestInvoice(userWS.getId()).getInvoiceLines();

			assertTrue(lines[1].getDescription().contains("Period from 12/01/2017 to 12/31/2017"));
			assertInvoiceLine(lines[1],  new BigDecimal(2),new BigDecimal(15), new BigDecimal(30).setScale(2, BigDecimal.ROUND_HALF_DOWN));
		});
	}

	/**
	 * User Story 2 - User 78603 currently has 15 active subscriptions. 
	 * Reduce the quantity for user id 78603, productCode Subscription by 10 on 15th Nov.Call API method as follows: 
	 * createUpdateOrderChange (78603, ‘Subscription’, null, new BigDecimal(5), ‘2017-11-15’) 
	 * During billing process on 1st Dec, the invoice will be generated as follows:
	 * 1st to 14th  Nov === Qty 15, Price @ $2.00, Amount = $30
	 * 15th to 30th Nov === Qty 5, Price @ $2.00, Amount = $5.33
	 * Next invoice system will generate invoice for effective order change only as follows
	 * 1st to 31st Dec === Qty 5, Price @ $2.00, Amount = $10.00
	 */
	@Test
	public void testUserStory002() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "userstory002.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

			UserWS userWS= api.getUserByCustomerMetaField("78603", ORG_ID_MF_NAME);
			logger.debug("User created  {} ", userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(FullCreativeUtil.getDate(10,01,2017));

			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				assertOrderChange(orderChangeWS, new Date(), new BigDecimal(2), "15");
				orderChangeWS.setStartDate(FullCreativeUtil.getDate(10,01,2017));
			}

			api.updateOrder(orderWS, orderChanges);
			Date changeEffectiveDate = FullCreativeUtil.getDate(10, 15, 2017);

			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,null, new BigDecimal(5), changeEffectiveDate);

			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			InvoiceWS invoiceWS = api.getLatestInvoice(userWS.getId());
			InvoiceLineDTO[] lines = invoiceWS.getInvoiceLines();
			lines = getSortedInvoiceLine(lines);

			logger.debug("lines[0] {} price {} quantity {} amount  {} ",lines[0].getDescription(),
					lines[0].getPriceAsDecimal(), lines[0].getQuantityAsDecimal(), lines[0].getAmountAsDecimal());
			logger.debug("lines[1] {} price {} quantity {} amount  {} ",lines[1].getDescription(),
					lines[1].getPriceAsDecimal(), lines[1].getQuantityAsDecimal(), lines[1].getAmountAsDecimal());

			assertTrue(lines[0].getDescription().contains("Period from 11/01/2017 to 11/14/2017"));
			assertTrue(lines[1].getDescription().contains("Period from 11/15/2017 to 11/30/2017"));
			assertInvoiceLine(lines[0],new BigDecimal(2),new BigDecimal(15), new BigDecimal(14).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			assertInvoiceLine(lines[1],new BigDecimal(2),new BigDecimal(5), new BigDecimal(5.33).setScale(2, BigDecimal.ROUND_HALF_DOWN));

			userWS = api.getUserWS(userWS.getId());
			nextInvoiceDate = FullCreativeUtil.getDate(00,01,2018);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);
			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			lines = api.getLatestInvoice(userWS.getId()).getInvoiceLines();
			assertTrue(lines[1].getDescription().contains("Period from 12/01/2017 to 12/31/2017"));
			assertInvoiceLine(lines[1],  new BigDecimal(2),new BigDecimal(05), new BigDecimal(10).setScale(2, BigDecimal.ROUND_HALF_DOWN));
		});
	}

	/**
	 * User Story 3 - User 78604 currently has 15 active subscriptions at the price of $2.25 per subscription. 
	 * Change the price per subscription to $2.50 on 15th Nov for all subscriptions.
	 * Call API method as follows: 
	 * createUpdateOrderChange (78604, ‘Subscription’, new BigDecimal(2.50), new BigDecimal(15), ‘2017-11-15’)
	 * createUpdateOrderChange (78604, ‘Subscription’, new BigDecimal(2.50), null, ‘2017-11-15’)
	 * Both above ways should achieve the desired result.
	 * During billing process on 1st Dec, the invoice will be generated as follows:
	 * 1st to 14th Nov  === Qty 15, Price @ $2.25, Amount = $15.75
	 * 15th to 30th Nov === Qty 15, Price @ $2.50, Amount = $20.00
	 * For the next invoice for month of Dec, system will generate invoice for effective order change only as follows:
	 * 1st to 31st Dec === Qty 15, Price @ $2.50, Amount = $37.50
	 */
	@Test
	public void testUserStory003() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "userstory003.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

			UserWS userWS= api.getUserByCustomerMetaField("78604", ORG_ID_MF_NAME);
			logger.debug("User created : {} ",  userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(FullCreativeUtil.getDate(10,01,2017));

			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				orderChangeWS.setStartDate(FullCreativeUtil.getDate(10,01,2017));
			}

			api.updateOrder(orderWS, orderChanges);
			Date changeEffectiveDate = FullCreativeUtil.getDate(10,15,2017);

			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,
					new BigDecimal(2.50), new BigDecimal(15), changeEffectiveDate);

			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			InvoiceWS invoiceWS = api.getLatestInvoice(userWS.getId());
			InvoiceLineDTO[] invoiceLines = invoiceWS.getInvoiceLines();
			invoiceLines = getSortedInvoiceLine(invoiceLines);
			assertTrue(invoiceLines[0].getDescription().contains("Period from 11/01/2017 to 11/14/2017"));
			assertTrue(invoiceLines[1].getDescription().contains("Period from 11/15/2017 to 11/30/2017"));
			assertInvoiceLine(invoiceLines[0],new BigDecimal(2.25),new BigDecimal(15), new BigDecimal(15.75).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			assertInvoiceLine(invoiceLines[1],new BigDecimal(2.50),new BigDecimal(15), new BigDecimal(20).setScale(2, BigDecimal.ROUND_HALF_DOWN));

		});
	}

	/**
	 * User Story 4 - User 78605 currently has 15 numbers(count) for ‘ANVEO_AUSTRALIA’ product code. 
	 * Remove this line as user has given up all his numbers in Canada on 15th Nov. 
	 * Call API method as follows: 
	 * createUpdateOrderChange (78605, ‘ANVEO_AUSTRALIA’, null, new BigDecimal(0), ‘2017-11-15’)
	 * During billing process on 1st Dec, the invoice will be generated as follows:
	 * 1st to 14th Nov  === Qty 15, Price @ $2.00, Amount = $14.00
	 * For the next invoice for month of Dec, 
	 * no line will reflect on the invoice for ANVEO_AUSTRALIA product as user has given up his numbers for 
	 * this product in the month of Nov.
	 **/

	@Test
	public void testUserStory004() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			BigDecimal charges = new BigDecimal("2.00");
			Integer count = new Integer("15");
			String case_4 = replaceTagWithProperValue(BASE_ONE_PROVIDER_ONE_COUNTRY_ONE_ORG, TAG_NAME_CHARGES, null, charges.toString());
			case_4 = replaceTagWithProperValue(case_4, TAG_NAME_ORG_ID, null, "78605");
			case_4 = replaceTagWithProperValue(case_4, TAG_NAME_COUNT, null, count.toString());
			createXmlFile(ORIGINATION_XML, case_4);

			placeNextXMLToProcess(api, "userstory004.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

			//run ORIGINATION_XML
			schedulePlugin(api, envBuilder);
			sleep(80000);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

			UserWS userWS= api.getUserByCustomerMetaField("78605", ORG_ID_MF_NAME);
			logger.debug("User created  {} ", userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(FullCreativeUtil.getDate(10,01,2017));

			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				orderChangeWS.setStartDate(FullCreativeUtil.getDate(10,01,2017));
			}

			api.updateOrder(orderWS, orderChanges);
			Arrays.asList(orderWS.getOrderLines()).forEach(line -> {
				line.setQuantityAsDecimal(new BigDecimal(15));
				api.updateOrderLine(line);
			});

			Date changeEffectiveDate = FullCreativeUtil.getDate(10,15,2017);
			api.createUpdateOrderChange(userWS.getId(), ANVEO_AUSTRALIA,null, new BigDecimal(0), changeEffectiveDate);
			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);
			InvoiceWS invoice =  api.getLatestInvoice(userWS.getId());

			logger.debug("Invoice Id {} ", invoice.getId());
			InvoiceLineDTO[] lines = invoice.getInvoiceLines();
			logger.debug("lines[0] {} price {} quantity {} amount  {} ",lines[0].getDescription(),
					lines[0].getPriceAsDecimal(), lines[0].getQuantityAsDecimal(), lines[0].getAmountAsDecimal());
			logger.debug("lines[1] {} price {} quantity {} amount  {} ",lines[1].getDescription(),
					lines[1].getPriceAsDecimal(), lines[1].getQuantityAsDecimal(), lines[1].getAmountAsDecimal());

			assertTrue(lines[0].getDescription().contains("Period from 11/01/2017 to 11/14/2017"));
			assertTrue(lines[1].getDescription().contains("Period from 11/01/2017 to 11/30/2017"));
			assertInvoiceLine(lines[0],new BigDecimal(2),new BigDecimal(15), new BigDecimal(14).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			assertInvoiceLine(lines[1],new BigDecimal(2),new BigDecimal(15), new BigDecimal(30).setScale(2, BigDecimal.ROUND_HALF_DOWN));

			nextInvoiceDate = FullCreativeUtil.getDate(00, 1, 2018);
			userWS = api.getUserWS(userWS.getId());
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);
			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			invoice =  api.getLatestInvoice(userWS.getId());
			logger.debug("Invoice Id {} ", invoice.getId());
			lines = invoice.getInvoiceLines();

			logger.debug("lines[0] {} price {} quantity {} amount  {} ",lines[0].getDescription(),
					lines[0].getPriceAsDecimal(), lines[0].getQuantityAsDecimal(), lines[0].getAmountAsDecimal());
			logger.debug("lines[1] {} price {} quantity {} amount  {} ",lines[1].getDescription(),
					lines[1].getPriceAsDecimal(), lines[1].getQuantityAsDecimal(), lines[1].getAmountAsDecimal());

			assertTrue(lines[1].getDescription().contains("Period from 12/01/2017 to 12/31/2017"));
			assertInvoiceLine(lines[1],new BigDecimal(2),new BigDecimal(15), new BigDecimal(30).setScale(2, BigDecimal.ROUND_HALF_DOWN));
		});
	}

	/**
	 * 
	 * User Story 5 - User 78606 subscribes to 10 active subscriptions on 1st Nov 2017 at price $6.5.
	 * Increment the quantity for user id 78606, Subscription by 5 on 15th Nov 2017 at price $8.5. 
	 * Call API method as follows: 
	 * createUpdateOrderChange (78606, ‘Subscription’, new BigDecimal(8.5), null, ‘2017-11-01’)
	 *  - this call would update the first order change line record created effective 1st Nov.
	 * createUpdateOrderChange (1001, ‘Subscription’, new BigDecimal(8.5), 15, ‘2017-11-15’)
	 *  - this call would update the second order change line record created effective 15th Nov.
	 *  The invoice generated after the API calls would reflect following lines:
	           •     1st to 14th Nov === Qty 10, Price @ $8.5
	           •    15th to 30th Nov === Qty 15, Price @ $8.5
	 */

	@Test
	public void testUserStory005() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "userstory005.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

			UserWS userWS= api.getUserByCustomerMetaField("78606", ORG_ID_MF_NAME);
			logger.debug("User created : {} ",  userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(FullCreativeUtil.getDate(10,01,2017));

			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				orderChangeWS.setStartDate(FullCreativeUtil.getDate(10,01,2017));
			}

			api.updateOrder(orderWS, orderChanges);
			Arrays.asList(orderWS.getOrderLines()).forEach(line -> {
				line.setQuantityAsDecimal(new BigDecimal(10));
				api.updateOrderLine(line);
			});

			Date changeEffectiveDate = FullCreativeUtil.getDate(10,01,2017);
			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,new BigDecimal(8.5), null, changeEffectiveDate);
			changeEffectiveDate = FullCreativeUtil.getDate(10,15,2017);
			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,null,new BigDecimal(15), changeEffectiveDate);

			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			InvoiceWS invoices = api.getLatestInvoice(userWS.getId());
			InvoiceLineDTO[] invoiceLines = invoices.getInvoiceLines();
			invoiceLines = getSortedInvoiceLine(invoiceLines);
			logger.debug("invoiceLines[0].getDescription() {}", invoiceLines[0].getDescription());
			logger.debug("invoiceLines[1].getDescription() {}", invoiceLines[1].getDescription());

			assertTrue(invoiceLines[0].getDescription().contains("Period from 11/01/2017 to 11/14/2017"));
			assertTrue(invoiceLines[1].getDescription().contains("Period from 11/15/2017 to 11/30/2017"));
			assertInvoiceLine(invoiceLines[0],new BigDecimal(8.50),new BigDecimal(10), new BigDecimal(39.67).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			assertInvoiceLine(invoiceLines[1],new BigDecimal(8.50),new BigDecimal(15), new BigDecimal(68.00).setScale(2, BigDecimal.ROUND_HALF_DOWN));

			nextInvoiceDate = FullCreativeUtil.getDate(00, 1, 2018);
			userWS = api.getUserWS(userWS.getId());
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);
			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			invoiceLines = api.getLatestInvoice(userWS.getId()).getInvoiceLines();
			logger.debug("invoiceLines[0].getDescription() {} ", invoiceLines[0].getDescription());
			logger.debug("invoiceLines[1].getDescription() {} ", invoiceLines[1].getDescription());

			assertTrue(invoiceLines[1].getDescription().contains("Period from 12/01/2017 to 12/31/2017"));
			assertInvoiceLine(invoiceLines[1],new BigDecimal(8.50),new BigDecimal(15), new BigDecimal(127.50).setScale(2, BigDecimal.ROUND_HALF_DOWN));

		});
	}

	/**
	 * User Story 6 - User 1001 subscribes to 10 active subscriptions on 1st Nov 2017 at price $6.5. 
	 * Increment the quantity for user id 78607, Subscription by 5 (ie. increase total quantity to 15) 
	 * on 15th Nov 2017 at price $6.5.
	 * Here, the client makes API call with changeEffectiveDate before order next billable date.
	 * The calls made are as follows:
	 * createUpdateOrderChange (1001, ‘Subscription’, new BigDecimal(8.5), null, ‘2017-10-01’)
	 *  - the changeEffectiveDate is 1st Oct (1 month before order next billable date of 1st Nov)
	 *  createUpdateOrderChange (1001, ‘Subscription’, new BigDecimal(8.5), null, ‘2017-10-31’)
	 *  - the changeEffectiveDate is 31st Oct which is a day before the order next billable date.
	 *  Both the above API calls will fail with the exception reason as 
	 *  ‘The changeEffectiveDate cannot be before the order next billable date’.
	 */
	@Test
	public void testUserStory006() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "userstory006.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			UserWS userWS   = api.getUserByCustomerMetaField("78607", ORG_ID_MF_NAME);
			logger.debug("User created : {} ",  userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(FullCreativeUtil.getDate(10,01,2017));

			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				orderChangeWS.setStartDate(FullCreativeUtil.getDate(10,01,2017));
			}

			api.updateOrder(orderWS, orderChanges);
			Arrays.asList(orderWS.getOrderLines()).forEach(line -> {
				line.setQuantityAsDecimal(new BigDecimal(10));
				api.updateOrderLine(line);
			});

			Date changeEffectiveDate = FullCreativeUtil.getDate(9,01,2017);
			Calendar date = Calendar.getInstance();
			date.setTime(new Date()); // Now use today date.
			date.add(Calendar.DATE,1);
			/**
			 * Scenario - 8 : check for future Effective date.
			 */

			try {
				api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,new BigDecimal(8.5), null, date.getTime());
			} catch (SessionInternalError e) {
				assertTrue(e.getMessage().contains("The changeEffectiveDate cannot be in future."));
			}

			try {
				api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,new BigDecimal(8.5), null, changeEffectiveDate);
			} catch (SessionInternalError e) {
				assertTrue(e.getMessage().contains("The changeEffectiveDate can be today or "
						+ "in the past but it cannot be before the order’s next billable date."));
			}

			try {
				changeEffectiveDate = FullCreativeUtil.getDate(9,31,2017);
				api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,new BigDecimal(8.5), null, changeEffectiveDate);
			} catch (SessionInternalError e) {
				assertTrue(e.getMessage().contains("The changeEffectiveDate can be today or "
						+ "in the past but it cannot be before the order’s next billable date."));
			}
		});
	}

	/**
	 * User Story 7 - User 1001 subscribes to 10 active subscriptions on 1st Nov 2017 at price $6.5.
	 * Increment the quantity for user id 1001, Subscription by 5
	 * (ie. increase total quantity to 15)on 15th Nov 2017 at price $6.5.
	 * Here, to increment the quantity by 5 the client makes API call with changeEffectiveDate after the order next billable date.
	 * The call made is as follows:
	 * createUpdateOrderChange (1001, ‘Subscription’, new BigDecimal(6.5), new BigDecimal(15), ‘2017-11-15’)
	 * - the changeEffectiveDate is 15th Nov (which is after order next billable date of 1st Nov)
	 * The above API call will succeed leading to ending of the existing order change line with end date of 14th Nov
	 * and create new order change record with effective date of 15th Nov and quantity of 15 @ rate of $6.5.
	 * 
	 * Now the client realized that he needs to charge the period of 1st Nov to 14th Nov 
	 * with quantity of 10 @ rate of $8.5 per subscription, and only because client bought 5 more subscriptions,
	 * he was given a discounted rate of $6.5 from 15th Nov.
	 * Therefore to update the rate correctly from 1st Nov to 14th Nov, the API call would be as follows:
	 * createUpdateOrderChange (1001, ‘Subscription’, new BigDecimal(8.5), new BigDecimal(10), ‘2017-11-01’)
	 * - the changeEffectiveDate is 1st Nov (which is equal to order next billable date of 1st Nov).
	 * Since the changeEffectiveDate is not before the order next billable date,
	 * this API call will succeed leading to desired price updated on 
	 * the order change record for the period 1st Nov to 14th Nov @ price of $8.5.
	 */
	@Test
	public void testUserStory007() throws Exception {

		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			placeNextXMLToProcess(api, "userstory007.xml");
			api.triggerScheduledTask(envBuilder.env().idForCode(PLUGIN_CODE), new Date());
			sleep(30000L);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();

			UserWS userWS= api.getUserByCustomerMetaField("78608", ORG_ID_MF_NAME);
			logger.debug("User created : {} ",  userWS.getUserName());
			Date nextInvoiceDate = FullCreativeUtil.getDate(11,01,2017);
			userWS.setNextInvoiceDate(nextInvoiceDate);
			api.updateUser(userWS);

			assertEquals("check next invoice date of user "+ userWS.getUserName() ,nextInvoiceDate, userWS.getNextInvoiceDate());

			OrderWS orderWS =  api.getLatestOrder(userWS.getId());
			orderWS.setActiveSince(FullCreativeUtil.getDate(10,01,2017));

			OrderChangeWS[] orderChanges = api.getOrderChanges(orderWS.getId());
			for (OrderChangeWS orderChangeWS : orderChanges) {
				orderChangeWS.setStartDate(FullCreativeUtil.getDate(10,01,2017));
			}

			api.updateOrder(orderWS, orderChanges);

			Arrays.asList(orderWS.getOrderLines()).forEach(line -> {
				line.setQuantityAsDecimal(new BigDecimal(10));
				api.updateOrderLine(line);
			});
			Date effectiveDate = FullCreativeUtil.getDate(10,15,2017);
			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,new BigDecimal(6.5), new BigDecimal(15), effectiveDate);

			effectiveDate = FullCreativeUtil.getDate(10,01,2017);
			api.createUpdateOrderChange(userWS.getId(), SUBSCRIPTION_CHARGES,new BigDecimal(8.5), new BigDecimal(10), effectiveDate);
			api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, 	null, null, false);

			InvoiceWS[] invoices = api.getAllInvoicesForUser(userWS.getId());
			InvoiceLineDTO[] invoiceLines = invoices[0].getInvoiceLines();
			invoiceLines = getSortedInvoiceLine(invoiceLines);
			logger.debug("invoiceLines[0].getDescription() {}", invoiceLines[0].getDescription());
			logger.debug("invoiceLines[1].getDescription() {}", invoiceLines[1].getDescription());

			assertTrue(invoiceLines[0].getDescription().contains("Period from 11/01/2017 to 11/14/2017"));
			assertTrue(invoiceLines[1].getDescription().contains("Period from 11/15/2017 to 11/30/2017"));
			assertInvoiceLine(invoiceLines[0],new BigDecimal(8.50),new BigDecimal(10), new BigDecimal(39.67).setScale(2, BigDecimal.ROUND_HALF_DOWN));
			assertInvoiceLine(invoiceLines[1],new BigDecimal(6.50),new BigDecimal(15), new BigDecimal(52.00).setScale(2, BigDecimal.ROUND_HALF_DOWN));
		});
	}

	private void assertOrderChange(OrderChangeWS changeWS,Date expectedStartDate, 
			BigDecimal expectedPrice, String expectedQuantity ){

		logger.debug("Check asserts on orderChanges of order with Id {}", changeWS.getOrderId() );
		assertEquals(TestConstants.DATE_FORMAT.format(changeWS.getStartDate()), TestConstants.DATE_FORMAT.format(expectedStartDate),"Check orderChange start date");
		assertEquals(expectedPrice.setScale(2),changeWS.getPriceAsDecimal().setScale(2),"Check orderChange price");
		assertEquals(changeWS.getQuantityAsDecimal().setScale(2), new BigDecimal(expectedQuantity).setScale(2),"Check orderChange quantity");
	}

	private void assertInvoiceLine(InvoiceLineDTO line, BigDecimal expectedPrice,BigDecimal expectedQuantity, BigDecimal expectedAmount ){

		logger.debug("Check asserts on invoice line of Invoice with Id {}", line.getId());
		assertEquals(expectedPrice.setScale(2),line.getPriceAsDecimal().setScale(2),"Check invoiceline price");
		assertEquals(expectedQuantity.setScale(2),line.getQuantityAsDecimal().setScale(2),"Check invoiceline quantity");
		assertEquals(expectedAmount,line.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_HALF_DOWN),"Check invoiceline amount");
	}

	private InvoiceLineDTO[] getSortedInvoiceLine(InvoiceLineDTO[] lines){

		Arrays.sort(lines, new Comparator<InvoiceLineDTO>() {
			@Override
			public int compare(InvoiceLineDTO o1, InvoiceLineDTO o2) {
				return 	o1.getId().compareTo(o2.getId());
			}
		});
		return lines;
	}

}


