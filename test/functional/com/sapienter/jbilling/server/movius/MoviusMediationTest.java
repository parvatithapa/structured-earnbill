package com.sapienter.jbilling.server.movius;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.cache.MatchType;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.CompanyWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.test.framework.builders.UsagePoolBuilder;
import com.sapienter.jbilling.test.framework.builders.PlanBuilder;
import com.sapienter.jbilling.server.order.OrderLineWS;

@Test(groups = { "movius" }, testName = "movius")
public class MoviusMediationTest {

	private static final Logger LOG = LoggerFactory.getLogger(MoviusMediationTest.class);
	private TestBuilder       testBuilder;
	private Integer           accountTypeId;
	private static final Integer CC_PM_ID                                      = 5;
	private static final String  ACCOUNT_NAME                                  = "Movius Test Account";
	private static final String  MEDIATED_USAGE_CATEGORY                       = "Movius Mediation Usage Category";
	private static final String  META_FIELD_CUSTOMER_ORG_ID                    = "Org Id";
	private static final String  META_FIELD_ANVEO_OUTGOING_CALL_COUNTRY_CODES  = "Anveo Country Code List";
	private static final String  META_FIELD_ANVEO_CALL_ITEM_ID                 = "Set Item Id For Anveo Calls";
	private static final String  META_FIELD_TATA_CALL_ITEM_ID                  = "Set Item Id For Tata Calls";
	private static final String  META_FIELD_SMS_ITEM_ID                        = "Set Item Id For SMS";
	private static final String  ANVEO_CALL_ITEM                               = "ANVEO CALL ITEM";
	private static final String  TATA_CALL_ITEM                                = "TATA CALL ITEM";
	private static final String  SMS_ITEM                                      = "SMS ITEM";
	private static final String  TEST_USER_1                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  MOVIUS_MEDIATION_CONFIG_NAME                  = "moviusMediationJob";
	private static final String  MOVIUS_MEDIATION_JOB_NAME                     = "moviusMediationJobLauncher";
	private static final int     MONTHLY_ORDER_PERIOD                          =  2;
	private static final int     NEXT_INVOICE_DAY                              =  1;
	private static final String  TEST_USER_2                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  SMS_RATE_CARD_ID                              = "11";
	private static final String  CALL_RATE_CARD_ID                             = "12";
	private static final String  OVERRIDE_CALL_RATE_CARD_ID                    = "13";
	private static final String  TEST_USER_3                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_4                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_5                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_6                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_7                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_8                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  TEST_USER_9                                   = "Test-User-"+ UUID.randomUUID().toString();
	private static final String  SUBSCRIPTION_PROD_01                          = "testPlanSubscriptionItem_01";
	private static final String  USAGE_POOL_01                                 = "UP with 100 Quantity"+System.currentTimeMillis();
	private static final String  PLAN_01                                       = "100 free minute Plan";
	private static final String  USER_01                                       = "Test-1-"+System.currentTimeMillis();
	private static final int     TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID         = 320104;
	private static final String  ORDER_01                                      = "testSubScriptionOrderO1";
	private static final String  ORDER_02                                      = "testSubScriptionOrderO2";
	public static final  String  INBOUND_MEDIATION_LAUNCHER                    = "inboundCallsMediationJobLauncher";
	private static final int     ORDER_CHANGE_STATUS_APPLY_ID                  = 3;
	private static final String  QUANTITY                                      = "0";
	private static final String  PRICE                                      = "1.50";
	private              UUID    uuid										   = null;
	// Mediated Usage Products
	public static final int      INBOUND_USAGE_PRODUCT_ID                      = 320101;
	public static final int      CHAT_USAGE_PRODUCT_ID                         = 320102;
	public static final int      ACTIVE_RESPONSE_USAGE_PRODUCT_ID              = 320103;

	private static final String  TEST_USER_10                                   = "Test-User-";
	private static final String  OUT_GOING_CALL_CDR_FORMAT                     = "2017-07-11 01:13:19 -0400|%s|sls|2013|0|14704471424"
			+ "|direct number|outgoing-call|14702987206|%s|1|0.010000|3946803075204777953|0"
			+ "|%s|0|VoIP|tx";

	private static final String  OUT_GOING_SMS_CDR_FORMAT                      = "2017-07-11 14:03:10 -0400|%s|sms-receiver|%s|0|14704471257|direct number|outgoing-sms|"
			                                                                     + "%s|%s";

	private static final String  IN_COMING_CALL_CDR_FORMAT                     = "2017-07-11 14:48:41 -0400|%s|sls|2017|0|18887555674|"
			                                             + "direct number|incoming-call|18887555674|%s|9|0.180000|0|3947201375594859739|12067922180|2017|TDM|TDM|normal";


	private static final String IN_COMING_SMS_DETAILS_CDR_FORMAT               = "2017-07-11 08:36:14 -0400|%s|sls|%s|0|13134233417|direct number|"
			+ "incoming-sms-details|14704471263|2.0|2017|TDM|0|3525693936660438174|VoIP";

	private static final String  INVALID_OUT_GOING_CALL_CDR_FORMAT             = "2017-07-11 01:13:19 -0400|%s|sls|2013|0|14704471424"
            + "|direct number|invalid-outgoing-call|14702987206|%s|1|0.010000|3946803075204777953|0"
            + "|%s|0|VoIP|tx";

	private static final String  NON_BILLABLE_OUT_GOING_SMS_CDR_FORMAT         = "2017-07-11 14:03:10 -0400|%s|sms-receiver1|%s|0|14704471257|direct number|outgoing-sms|"
            + "%s|%s";

	private TestBuilder getTestEnvironment() {
		return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {});
	}

	@BeforeClass
	public void initializeTests() {
		testBuilder = getTestEnvironment();
		testBuilder.given(envBuilder -> {
			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			// Creating account type
			accountTypeId = buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);

            // Creating mediated usage category
            buildAndPersistCategory(envBuilder, api, MEDIATED_USAGE_CATEGORY, false, ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);

            PriceModelWS smsRateCardPrice = buildRateCardPriceModel(SMS_RATE_CARD_ID,   MatchType.COUNTRY_CODE_MATCH);
            PriceModelWS callRateCardPrice = buildRateCardPriceModel(CALL_RATE_CARD_ID, MatchType.COUNTRY_AREA_CODE_MATCH);

            // Creating Mediated Products with rate pricing
            buildAndPersistProduct(envBuilder, api, ANVEO_CALL_ITEM, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY),
                    callRateCardPrice, true);

            buildAndPersistProduct(envBuilder, api, TATA_CALL_ITEM,  false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY),
                    callRateCardPrice, true);

            buildAndPersistProduct(envBuilder, api, SMS_ITEM,        false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY),
                    smsRateCardPrice, true);

            // Creating Customer Level MetaField
            buildAndPersistMetafield(testBuilder, META_FIELD_CUSTOMER_ORG_ID, DataType.STRING, EntityType.CUSTOMER);

            // Creating Company Level MetaField
            buildAndPersistMetafield(testBuilder, META_FIELD_ANVEO_OUTGOING_CALL_COUNTRY_CODES, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, META_FIELD_ANVEO_CALL_ITEM_ID, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, META_FIELD_TATA_CALL_ITEM_ID, DataType.STRING, EntityType.COMPANY);
            buildAndPersistMetafield(testBuilder, META_FIELD_SMS_ITEM_ID, DataType.STRING, EntityType.COMPANY);

            // Setting Company Level Meta Fields
            setCompanyLevelMetaField(testBuilder.getTestEnvironment());

            //Create usage products
            buildAndPersistProduct(envBuilder, api, SUBSCRIPTION_PROD_01, false, envBuilder.idForCode(MEDIATED_USAGE_CATEGORY),
                    new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("99"), api.getCallerCurrencyId()), true);

            //Usage product item ids
            List<Integer> items = Arrays.asList(INBOUND_USAGE_PRODUCT_ID, CHAT_USAGE_PRODUCT_ID, ACTIVE_RESPONSE_USAGE_PRODUCT_ID);
            Calendar pricingDate = Calendar.getInstance();
            pricingDate.set(Calendar.YEAR, 2018);
            pricingDate.set(Calendar.MONTH, 0);
            pricingDate.set(Calendar.DAY_OF_MONTH, 1);

            PlanItemWS planItemProd01WS = buildPlanItem(api, items.get(0), MONTHLY_ORDER_PERIOD, QUANTITY, PRICE, pricingDate.getTime());
            PlanItemWS planItemProd02WS = buildPlanItem(api, items.get(1), MONTHLY_ORDER_PERIOD, QUANTITY, PRICE, pricingDate.getTime());
            PlanItemWS planItemProd03WS = buildPlanItem(api, items.get(2), MONTHLY_ORDER_PERIOD, QUANTITY, PRICE, pricingDate.getTime());

            //Create usage pool with 100 free minutes
            buildAndPersistUsagePool(envBuilder,api,USAGE_POOL_01, "100", envBuilder.idForCode(MEDIATED_USAGE_CATEGORY), items);

            //Create 100 min free plan
            buildAndPersistPlan(envBuilder,api, PLAN_01, "100 Free Minutes Plan", MONTHLY_ORDER_PERIOD,
                    envBuilder.idForCode(SUBSCRIPTION_PROD_01), Arrays.asList(envBuilder.idForCode(USAGE_POOL_01)),planItemProd01WS, planItemProd02WS, planItemProd03WS);

            // Creating Movius Job Launcher
            buildAndPersistMediationConfiguration(envBuilder, api, MOVIUS_MEDIATION_CONFIG_NAME, MOVIUS_MEDIATION_JOB_NAME);

		}).test((testEnv, testEnvBuilder) -> {

			assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
			assertNotNull("Mediated Categroy Creation Failed ", testEnvBuilder.idForCode(MEDIATED_USAGE_CATEGORY));
			assertNotNull("Customer Level MetaField Creation Failed ", testEnvBuilder.idForCode(META_FIELD_CUSTOMER_ORG_ID));
			assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(META_FIELD_ANVEO_OUTGOING_CALL_COUNTRY_CODES));
			assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(META_FIELD_ANVEO_CALL_ITEM_ID));
			assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(META_FIELD_TATA_CALL_ITEM_ID));
			assertNotNull("Company Level MetaField Creation Failed ", testEnvBuilder.idForCode(META_FIELD_SMS_ITEM_ID));
			assertNotNull("Anveo Item Creation Failed ", testEnvBuilder.idForCode(ANVEO_CALL_ITEM));
			assertNotNull("Tata Item Creation Failed ", testEnvBuilder.idForCode(TATA_CALL_ITEM));
			assertNotNull("SMS Item Creation Failed ", testEnvBuilder.idForCode(SMS_ITEM));
			assertNotNull("Mediation Configuration  Creation Failed ", testEnvBuilder.idForCode(MOVIUS_MEDIATION_CONFIG_NAME));
            assertNotNull("Usage pool Creation Failed", testEnvBuilder.idForCode(USAGE_POOL_01));
            assertNotNull("Product Creation Failed", testEnvBuilder.idForCode(SUBSCRIPTION_PROD_01));
            assertNotNull("Plan Creation Failed", testEnvBuilder.idForCode(PLAN_01));
		});

	}

	@AfterClass
	public void tearDown() {
		testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		testBuilder.removeEntitiesCreatedOnJBilling();
		testBuilder = null;
	}

	@Test
	public void test01MediationUpload() {
		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.set(Calendar.YEAR, 2017);
		nextInvoiceDate.set(Calendar.MONTH, 6);
		nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
		String orgId = "2020";
		List<String> cdrs = buildCDR(Arrays.asList(orgId));
		testBuilder.given(envBuilder -> {
			LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			UserWS userWS = envBuilder.customerBuilder(api)
					.withUsername(TEST_USER_1)
					.withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
					.addTimeToUsername(false)
					.withNextInvoiceDate(nextInvoiceDate.getTime())
					.withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
					.withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
					.build();

			userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
			api.updateUser(userWS);
		}).validate((testEnv, testEnvBuilder) -> {
			assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_1));
		}).validate((testEnv, testEnvBuilder) -> {
			// trigger mediation
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			uuid = api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
			LOG.debug("Mediation ProcessId {}", uuid);
			assertNotNull("Mediation triggered should return uuid", uuid);
		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(1), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_1));
            assertNotNull("Mediation Should Create Order", order);
            /**
             * test undo mediation : bugfix/JBMOV-191
             */
            api.undoMediation(uuid);
            MediationProcess mediationProcessUndone = api.getMediationProcess(uuid);
            assertNull(mediationProcessUndone, "Mediation process not expected!");
		});
	}

	@Test
	public void test02RecycleMediation() {

		String orgId = "2021";
		List<String> cdrs = buildCDR(Arrays.asList(orgId));
		testBuilder.given(envBuilder -> {
			// trigger mediation
			JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
			api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
			LOG.debug("Mediation Process {}", mediationProcess);
			assertEquals("Mediation Error Record ", Integer.valueOf(4), mediationProcess.getErrors());
		}).validate((testEnv, testEnvBuilder) -> {

			Calendar nextInvoiceDate = Calendar.getInstance();
			nextInvoiceDate.set(Calendar.YEAR, 2017);
			nextInvoiceDate.set(Calendar.MONTH, 6);
			nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

			LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
			final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			UserWS userWS = testEnvBuilder.customerBuilder(api)
					.withUsername(TEST_USER_2)
					.withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
					.addTimeToUsername(false)
					.withNextInvoiceDate(nextInvoiceDate.getTime())
					.withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
					.withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
					.build();

			userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
			api.updateUser(userWS);
		}).validate((testEnv, testEnvBuilder) -> {
			assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_2));
		}).validate((testEnv, testEnvBuilder) -> {
			// trigger mediation
		    JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			api.runRecycleForProcess(api.getMediationProcessStatus().getMediationProcessId());
			pauseUntilMediationStarts(30, api);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(1), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_2));
            assertNotNull("Mediation Should Create Order", order);
		});
	}

	@Test
    public void test03SMSRateCardMediation() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgId = "2023";

        List<String> cdrs = Arrays.asList(String.format(OUT_GOING_SMS_CDR_FORMAT, UUID.randomUUID().toString(), orgId,
                         "919960338097", UUID.randomUUID().toString()),

                         String.format(OUT_GOING_SMS_CDR_FORMAT, UUID.randomUUID().toString(), orgId,
                                 "8801718962156", UUID.randomUUID().toString()));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_3)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_3));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_3));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("SMS Order Amount Should be ", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.14"));

        });
    }

	@Test
    public void test04CallRateCardMediation() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgId = "2024";

        List<String> cdrs = Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14704471424"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14034471424"));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_4)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_4));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_4));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("CALL Order Amount Should be ", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.02"));
        });
    }

	@Test
    public void test05OveerideCallRateCardMediation() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgId = "2025";

        List<String> cdrs = Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14704471424"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14034471424"));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_5)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);

            PlanItemWS price = new PlanItemWS();
            price.setItemId(envBuilder.idForCode(ANVEO_CALL_ITEM));

            PriceModelWS callRateCardPrice = buildRateCardPriceModel(OVERRIDE_CALL_RATE_CARD_ID, MatchType.COUNTRY_AREA_CODE_MATCH);

            price.getModels().put(CommonConstants.EPOCH_DATE, callRateCardPrice);
            api.createCustomerPrice(userWS.getId(), price,  null);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_5));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_5));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("SMS Order Amount Should be ", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.03"));
        });
    }

	@Test
    public void test06invalidCDRUpload() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgId = "2026";

        List<String> cdrs = Arrays.asList(String.format(INVALID_OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14704471424"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14034471424"));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_6)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);

        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_6));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_6));
            assertNotNull("Mediation Should Create Order", order);
        });
    }

	@Test
    public void test07CallMobileNumberRateCardMediation() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgId = "2027";

        List<String> cdrs = Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "447378112311"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "447378112312"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "447378112313"));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_7)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_7));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_7));
            assertNotNull("Mediation Should Create Order", order);
            assertEquals("CALL Order Amount Should be ", order.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.02"));
        });
    }

	@Test
    public void test08MediationUploadForNonBillableJMR() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
        String orgId = "2022";
        List<String> cdrs = new ArrayList<>();
        cdrs.addAll(Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14704471424"),
                String.format(NON_BILLABLE_OUT_GOING_SMS_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "919960338097" ,UUID.randomUUID().toString()),
                String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14704471424")));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_8)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_8));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(1), mediationProcess.getDoneAndNotBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_8));
            assertNotNull("Mediation Should Create Order", order);


            long count = Arrays.stream(api.getMediationEventsForOrder(order.getId()))
                               .filter(jmr -> jmr.getOrderLineId() == null)
                               .count();
            LOG.debug("Mediation Event With no order line count {}", count);
            assertEquals("Mediation Event With no order line count ", 1L, count);

        });
    }

	@Test
    public void test09CheckOrderLineAndJMRQuantity() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgId = "2028";

        List<String> cdrs = Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "93709799799"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "93709799799"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "93709799799"));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWS = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_9)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                    .build();

            userWS.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWS);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_9));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_9));
            assertNotNull("Mediation Should Create Order", order);

            BigDecimal totalJMRPriceSum = Arrays.stream(api.getMediationEventsForOrder(order.getId()))
                    .map(JbillingMediationRecord::getRatedPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            LOG.debug("Mediation Event Total Amount {}", totalJMRPriceSum);
            assertEquals("Order Total ", order.getTotalAsDecimal(), totalJMRPriceSum);


        });
    }

	@Test
	public void test10checkMediationforUserHavingTwoSubsriptionOrders() {
	    testBuilder.given(envBuilder -> {
	        TestEnvironment environment = testBuilder.getTestEnvironment();

	        Calendar nextInvoiceDate = Calendar.getInstance();
	        nextInvoiceDate.set(Calendar.YEAR, 2017);
	        nextInvoiceDate.set(Calendar.MONTH, 6);
	        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

	        LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());

	        Calendar activeSince = Calendar.getInstance();
	        activeSince.set(Calendar.YEAR, 2017);
	        activeSince.set(Calendar.MONTH, 5);
	        activeSince.set(Calendar.DAY_OF_MONTH, 1);
	        final JbillingAPI api = envBuilder.getPrancingPonyApi();

	        AssetWS scenario01Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
	        Map<Integer, Integer> productAssetMap = new HashMap<>();
	        productAssetMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario01Asset.getId());

	        Map<Integer, BigDecimal> productQuantityMap = new HashMap<>();
	        productQuantityMap.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
	        productQuantityMap.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);


	        buildAndPersistCustomer(envBuilder,api, USER_01,
	                accountTypeId, nextInvoiceDate.getTime(), MONTHLY_ORDER_PERIOD,NEXT_INVOICE_DAY);

	        createOrder(envBuilder,ORDER_01, activeSince.getTime(),USER_01,null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_POST_PAID,
	                ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap, productAssetMap, false);


	        List<String> cdrs = buildInboundCDR(getAssetIdentifiers(USER_01),"600","05/01/2017");
	        triggerMediation(envBuilder,INBOUND_MEDIATION_LAUNCHER, cdrs);

	        AssetWS scenario02Asset = getAssetIdByProductId(api,TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID);
	        Map<Integer, Integer> productAssetMap01 = new HashMap<>();
	        productAssetMap01.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, scenario02Asset.getId());

	        Map<Integer, BigDecimal> productQuantityMap01 = new HashMap<>();
	        productQuantityMap01.put(TOLL_FREE_8XX_NUMBER_ASSET_PRODUCT_ID, BigDecimal.ONE);
	        productQuantityMap01.put(environment.idForCode(SUBSCRIPTION_PROD_01), BigDecimal.ONE);

	        createOrder(envBuilder,ORDER_02, activeSince.getTime(),USER_01,null, MONTHLY_ORDER_PERIOD,Constants.ORDER_BILLING_POST_PAID,
	                ORDER_CHANGE_STATUS_APPLY_ID, true, productQuantityMap01, productAssetMap01, false);

	        cdrs = buildInboundCDR(getAssetIdentifiers(USER_01),"600","05/01/2017");
	        triggerMediation(envBuilder,INBOUND_MEDIATION_LAUNCHER, cdrs);

	    }).validate((testEnv, testEnvBuilder) -> {
	        final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
	        OrderWS order = api.getOrder(testEnv.idForCode(ORDER_01));
	        assertTrue(order.getOrderLines()[0].getAmountAsDecimal().compareTo(new BigDecimal("0.0")) >= 0);
	        assertTrue(order.getOrderLines()[0].getPriceAsDecimal().compareTo(new BigDecimal("0.0")) >= 0);
	    });
	}

	@Test
    public void test10UseParentPricingRateCardMediation() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);

        String orgOneId = "990001"; // parent 			- 0.02
        String orgTwoId = "990002"; // self price		- 0.01
        String orgThreeId = "990003"; // use parent price	- 0.02

        List<String> cdrs = Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgOneId, "14704471424"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgTwoId, "14704471424"),
                                          String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgThreeId, "14704471424"));

        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} ", nextInvoiceDate.getTime());
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS userWSOne = envBuilder.customerBuilder(api)
                    .withUsername(TEST_USER_10+orgOneId)
                    .withIsParent(Boolean.TRUE)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgOneId)
                    .build();
            userWSOne.setNextInvoiceDate(nextInvoiceDate.getTime());
            api.updateUser(userWSOne);

            UserWS userWSTwo = envBuilder.customerBuilder(api)
                    .withParentId(userWSOne.getId())
                    .withUsername(TEST_USER_10+orgTwoId)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgTwoId)
                    .build();
            userWSTwo.setNextInvoiceDate(nextInvoiceDate.getTime());
            userWSTwo.setInvoiceChild(Boolean.TRUE);
            userWSTwo.setUseParentPricing(Boolean.FALSE);
            api.updateUser(userWSTwo);

            UserWS userWSThree = envBuilder.customerBuilder(api)
                    .withParentId(userWSOne.getId())
                    .withUsername(TEST_USER_10+orgThreeId)
                    .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                    .addTimeToUsername(false)
                    .withNextInvoiceDate(nextInvoiceDate.getTime())
                    .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                    .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgThreeId)
                    .build();
            userWSThree.setNextInvoiceDate(nextInvoiceDate.getTime());
            userWSThree.setInvoiceChild(Boolean.FALSE);
            userWSThree.setUseParentPricing(Boolean.TRUE);
            api.updateUser(userWSThree);

            PlanItemWS price = new PlanItemWS();
            price.setItemId(envBuilder.idForCode(ANVEO_CALL_ITEM));
            PriceModelWS callRateCardPrice = buildRateCardPriceModel(OVERRIDE_CALL_RATE_CARD_ID, MatchType.COUNTRY_AREA_CODE_MATCH);
            price.getModels().put(CommonConstants.EPOCH_DATE, callRateCardPrice);
            api.createCustomerPrice(userWSOne.getId(), price,  null);
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_10+orgOneId));
        }).validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            api.processCDR(getMediationConfiguration(api, MOVIUS_MEDIATION_JOB_NAME), cdrs);
        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            LOG.debug("Mediation Process {}", mediationProcess);
            assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
            OrderWS order1 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_10+orgOneId));
            OrderWS order2 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_10+orgTwoId));
            OrderWS order3 = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER_10+orgThreeId));
            assertNotNull("Mediation Should Create Order", order1);
            assertEquals("Order Amount Should be ", order1.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.02"));
            assertEquals("Order Amount Should be ", order2.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.01"));
            assertEquals("Order Amount Should be ", order3.getTotalAsDecimal().setScale(2, BigDecimal.ROUND_HALF_UP),
                    new BigDecimal("0.02"));
        });
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
			boolean global, Integer categoryId, PriceModelWS priceModelWS, boolean allowDecimal) {
		return envBuilder.itemBuilder(api)
				.item()
				.withCode(code)
				.withType(categoryId)
				.withPriceModel(priceModelWS)
				.global(global)
				.allowDecimal(allowDecimal)
				.build();
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

	private void setCompanyLevelMetaField(TestEnvironment environment) {
		JbillingAPI api = environment.getPrancingPonyApi();
		CompanyWS company = api.getCompany();
		List<MetaFieldValueWS> values = new ArrayList<>();
		values.addAll(Arrays.stream(company.getMetaFields()).collect(Collectors.toList()));

		values.add(new MetaFieldValueWS(META_FIELD_ANVEO_CALL_ITEM_ID, null, DataType.STRING, true, environment.idForCode(ANVEO_CALL_ITEM).toString()));
		values.add(new MetaFieldValueWS(META_FIELD_ANVEO_OUTGOING_CALL_COUNTRY_CODES, null, DataType.STRING, true, "1"));
		values.add(new MetaFieldValueWS(META_FIELD_SMS_ITEM_ID, null, DataType.STRING, true, environment.idForCode(SMS_ITEM).toString()));
		values.add(new MetaFieldValueWS(META_FIELD_TATA_CALL_ITEM_ID, null, DataType.STRING, true, environment.idForCode(TATA_CALL_ITEM).toString()));
		int entityId = api.getCallerCompanyId();
		LOG.debug("Created Company Level MetaFields {}", values);
		values.forEach(value -> {
			value.setEntityId(entityId);
		});
		company.setTimezone(company.getTimezone());
		company.setMetaFields(values.toArray(new MetaFieldValueWS[0]));
		api.updateCompany(company);

	}

	private Integer buildAndPersistMediationConfiguration(TestEnvironmentBuilder envBuilder, JbillingAPI api, String configName, String jobLauncherName) {
		return envBuilder.mediationConfigBuilder(api)
         		  .withName(configName)
         		  .withLauncher(jobLauncherName)
         		  .withLocalInputDirectory(com.sapienter.jbilling.common.Util.getSysProp("base_dir") + "movius-test")
         		  .build();
	}

	private Integer getMediationConfiguration(JbillingAPI api, String mediationJobLauncher) {
	    MediationConfigurationWS[] allMediationConfigurations = api.getAllMediationConfigurations();
	    for (MediationConfigurationWS mediationConfigurationWS: allMediationConfigurations) {
	        if (null != mediationConfigurationWS.getMediationJobLauncher() &&
	                (mediationConfigurationWS.getMediationJobLauncher().equals(mediationJobLauncher))) {
	            return mediationConfigurationWS.getId();
	        }
	    }
	    return null;
	}

	private List<String> buildCDR(List<String> orgs) {
		List<String> cdrs = new ArrayList<>();
		for(String orgId : orgs) {
			cdrs.addAll(Arrays.asList(String.format(OUT_GOING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "14704471424"),
					String.format(OUT_GOING_SMS_CDR_FORMAT, UUID.randomUUID().toString(), orgId, "919960338097" ,UUID.randomUUID().toString()),
					String.format(IN_COMING_CALL_CDR_FORMAT, UUID.randomUUID().toString(), orgId),
					String.format(IN_COMING_SMS_DETAILS_CDR_FORMAT, UUID.randomUUID().toString(), orgId)));

		}
		return cdrs;
	}

	private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch(InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void pauseUntilMediationStarts(long seconds, JbillingAPI api) {
        for (int i = 0; i < seconds; i++) {
            if (!api.isMediationProcessRunning()) {
                return ;
            }
            sleep(1000L);
        }
        throw new RuntimeException("Mediation startup wait was timeout in "+ seconds);
    }

    private PriceModelWS buildRateCardPriceModel(String rateCardId, MatchType matchType){
        PriceModelWS rateCardPrice = new PriceModelWS(PriceModelStrategy.RATE_CARD.name(), null, 1);
        SortedMap<String, String> attributes = new TreeMap<>();
        attributes.put("rate_card_id", rateCardId);
        attributes.put("lookup_field", "Destination Number");
        attributes.put("match_type", matchType.name());
        rateCardPrice.setAttributes(attributes);
        return rateCardPrice;
    }

    private List<String> buildInboundCDR(List<String> indentifiers, String quantity, String eventDate) {
        List<String> cdrs = new ArrayList<>();
        indentifiers.forEach(asset ->
            cdrs.add("us-cs-telephony-voice-101108.vdc-070016UTC-" + UUID.randomUUID().toString()+",6165042651,tressie.johnson,Inbound,"+ asset +","+eventDate+","+"12:00:16 AM,4,3,47,2,0,"+quantity+",47,0,null")
        );

        return cdrs;
    }

    private List<String> getAssetIdentifiers(String userName) {
        if( null == userName || userName.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> identifiers = new ArrayList<>();
        testBuilder.given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();

            OrderWS [] orders = api.getUserSubscriptions(envBuilder.idForCode(userName));
            Arrays.stream(orders)
            .forEach(order -> {
                Arrays.stream(order.getOrderLines())
                .forEach(line -> {
                    Integer[] assetIds = line.getAssetIds();
                    if(null!=assetIds && assetIds.length!= 0 ) {
                        Arrays.stream(assetIds)
                        .forEach(assetId ->
                            identifiers.add(api.getAsset(assetId).getIdentifier())
                        );
                    }
                });

            });

        });
        return identifiers;
    }

    private void createOrder(TestEnvironmentBuilder envBuilder, String code,Date activeSince,String userName, Date activeUntil, Integer orderPeriodId, int billingTypeId, int statusId,
            boolean prorate, Map<Integer, BigDecimal> productQuantityMap, Map<Integer, Integer> productAssetMap, boolean createNegativeOrder) {
        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        List<OrderLineWS> lines = productQuantityMap.entrySet()
                .stream()
                .map(lineItemQuatityEntry -> {
                    OrderLineWS line = new OrderLineWS();
                    line.setItemId(lineItemQuatityEntry.getKey());
                    line.setTypeId(Integer.valueOf(1));
                    ItemDTOEx item = api.getItem(lineItemQuatityEntry.getKey(), null, null);
                    line.setDescription(item.getDescription());
                    line.setQuantity(lineItemQuatityEntry.getValue());
                    line.setUseItem(true);
                    if(createNegativeOrder) {
                        line.setUseItem(false);
                        line.setPrice(item.getPriceAsDecimal().negate());
                        line.setAmount(line.getQuantityAsDecimal().multiply(line.getPriceAsDecimal()));
                    }
                    if(null!=productAssetMap && !productAssetMap.isEmpty()
                            && productAssetMap.containsKey(line.getItemId())) {
                        line.setAssetIds(new Integer[] {productAssetMap.get(line.getItemId())});
                    }
                    return line;
                }).collect(Collectors.toList());

        envBuilder.orderBuilder(api)
        .withCodeForTests(code)
        .forUser(envBuilder.idForCode(userName))
        .withActiveSince(activeSince)
        .withActiveUntil(activeUntil)
        .withEffectiveDate(activeSince)
        .withPeriod(orderPeriodId)
        .withBillingTypeId(billingTypeId)
        .withProrate(prorate)
        .withOrderLines(lines)
        .withOrderChangeStatus(statusId)
        .build();

    }

    private Integer buildAndPersistUsagePool(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String quantity,Integer categoryId, List<Integer>  items) {
        return UsagePoolBuilder.getBuilder(api, envBuilder.env(), code)
                .withQuantity(quantity)
                .withResetValue("Reset To Initial Value")
                .withItemIds(items)
                .addItemTypeId(categoryId)
                .withCyclePeriodUnit(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)
                .withCyclePeriodValue(Integer.valueOf(1)).withName(code)
                .build();
    }

    private Integer buildAndPersistPlan(TestEnvironmentBuilder envBuilder, JbillingAPI api, String code, String desc,
            Integer periodId, Integer itemId, List<Integer> usagePools, PlanItemWS... planItems) {
        return envBuilder.planBuilder(api, code)
                .withDescription(desc)
                .withPeriodId(periodId)
                .withItemId(itemId)
                .withUsagePoolsIds(usagePools)
                .withPlanItems(Arrays.asList(planItems))
                .build().getId();
    }


    private Integer buildAndPersistCustomer(TestEnvironmentBuilder envBuilder, JbillingAPI api, String username,
            Integer accountTypeId, Date nextInvoiceDate, Integer periodId, Integer nextInvoiceDay) {

        UserWS userWS = envBuilder.customerBuilder(api)
                .withUsername(username)
                .withAccountTypeId(accountTypeId)
                .addTimeToUsername(false)
                .withNextInvoiceDate(nextInvoiceDate)
                .withMainSubscription(new MainSubscriptionWS(periodId, nextInvoiceDay))
                .build();
        userWS.setNextInvoiceDate(nextInvoiceDate);
        api.updateUser(userWS);
        return userWS.getId();
    }

    private void triggerMediation(TestEnvironmentBuilder envBuilder, String jobConfigName, List<String> cdr) {
        JbillingAPI api = envBuilder.getPrancingPonyApi();
        api.processCDR(getMediationConfiguration(api, jobConfigName), cdr);
    }

    private PlanItemWS buildPlanItem(JbillingAPI api, Integer itemId, Integer periodId, String quantity, String price, Date pricingDate) {
        return PlanBuilder.PlanItemBuilder.getBuilder()
                .withItemId(itemId)
                .withModel(new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .addModel(pricingDate, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(price), api.getCallerCurrencyId()))
                .withBundledPeriodId(periodId)
                .withBundledQuantity(quantity)
                .build();
    }

    private static AssetWS getAssetIdByProductId(JbillingAPI api, Integer productId) {
        // setup a BasicFilter which will be used to filter assets on Available status
        BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
        SearchCriteria criteria = new SearchCriteria();
        criteria.setMax(1);
        criteria.setOffset(0);
        criteria.setSort("id");
        criteria.setTotal(-1);
        criteria.setFilters(new BasicFilter[]{basicFilter});

        AssetSearchResult assetsResult = api.findProductAssetsByStatus(productId, criteria);
        assertNotNull("No available asset found for product "+productId, assetsResult);
        AssetWS[] availableAssets = null != assetsResult ? assetsResult.getObjects() : null;
        assertTrue("No assets found for product .", null != availableAssets && availableAssets.length != 0);
        Integer assetIdProduct = availableAssets[0].getId();
        LOG.debug("Asset Available for product {} = {}", productId, assetIdProduct);
        return availableAssets[0];
    }
}
