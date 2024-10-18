package com.sapienter.jbilling.server.dt.mediation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.server.mediation.MediationProcess;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;


@SuppressWarnings("Duplicates")
@Test(groups = {"dt-mediation"}, testName = "DtMediationTest")
public class DtMediationTest {

  private static final String  	ACCOUNT_NAME                                = "DT-Test-Account";
  private static final String		PRODUCT_CATEGORY                            = "OTC-Product-Suite";

  private static final String 	OTC_FLAT_PRODUCT                            = "OTC_OBS_FLAT_PROD_" + System.currentTimeMillis();
  private static final String 	OTC_TIERED_PRODUCT                          = "OTC_OBS_TIERED_PRODUCT_" + System.currentTimeMillis();

  private static final String 	MEDIATION_CONFIG_NAME                       = "dtMediationJob";

  private static final String 	METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER  = "externalAccountIdentifier";
  private static final String 	TEST_USER                                   ="TestUser-" + UUID.randomUUID().toString();
  private static final String 	TEST_USER_IDENTIFIER                        = UUID.randomUUID().toString();

  private TestBuilder testBuilder;
  private UUID uuid = null;

  private static final Logger LOG = LoggerFactory.getLogger(DtMediationTest.class);

  private static final String DEFAULT_CDR =
     "20|19701010000000|%s|eu-de||otc.service.type.obs|otc.resource.type.obs|0||%s|20180420110000|20180420115959|accumulate_factor|%s|172.16.32.82,200,04660000016147A40D7CAED78D27C00F,obs-tc8425-w,99,99,08|%s|19701010000000|19701010000000|";

	@BeforeClass
	public void initializeTests() {

		testBuilder = DtMediationTestHelper.getTestEnvironment();

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();

			// account type
			DtMediationTestHelper.buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME);

			// product
			DtMediationTestHelper.buildAndPersistCategory(envBuilder, api, PRODUCT_CATEGORY, false,
					ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
			buildProducts(envBuilder, api);

			// customer Level metaField
			DtMediationTestHelper.buildAndPersistMetafield(testBuilder,
					METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER,
					DataType.STRING, EntityType.CUSTOMER);

			// customer/user
			DtMediationTestHelper.buildAndPersistCustomer(envBuilder, api,
					TEST_USER,
					testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME),
					METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER,
					TEST_USER_IDENTIFIER);

			// dt mediation job launcher
			DtMediationTestHelper.buildAndPersistDtMediationConfiguration(envBuilder, api, MEDIATION_CONFIG_NAME);

		}).test((testEnv, testEnvBuilder) -> {

			assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));

			assertNotNull("Product Category Creation Failed ", testEnvBuilder.idForCode(PRODUCT_CATEGORY));
			assertNotNull("OTC Item Creation Failed ", testEnvBuilder.idForCode(OTC_FLAT_PRODUCT));
			assertNotNull("OTC Item Creation Failed ", testEnvBuilder.idForCode(OTC_TIERED_PRODUCT));

			assertNotNull("Customer Level MetaField Creation Failed ", testEnvBuilder.idForCode(METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER));
			assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER));

			assertNotNull("Mediation Configuration  Creation Failed ", testEnvBuilder.idForCode(MEDIATION_CONFIG_NAME));

		});
	}


	// TESTS

	@Test
	public void test_Mediation_FlatPrice(){

		final List<String> cdrs = new ArrayList<String>() {{
			add(DtMediationTestHelper.buildCDR(DEFAULT_CDR, "100", TEST_USER_IDENTIFIER,OTC_FLAT_PRODUCT));
			add(DtMediationTestHelper.buildCDR(DEFAULT_CDR, "150", TEST_USER_IDENTIFIER,OTC_FLAT_PRODUCT));
		}};

		testBuilder.validate((testEnv, testEnvBuilder) -> {
			// trigger mediation
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			uuid = api.processCDR(DtMediationTestHelper.getMediationConfiguration(api,
					DtMediationTestHelper.getMediationJobName()), cdrs);

			LOG.debug("Mediation ProcessId {}", uuid);
			assertNotNull("Mediation triggered should return uuid", uuid);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			LOG.debug("Mediation Process Status {}", api.getMediationProcessStatus());

			MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
			LOG.debug("Mediation Process {}", mediationProcess);

			assertEquals("Mediation Done And Billable ", Integer.valueOf(2), mediationProcess.getDoneAndBillable());
			assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());

			OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER));
			assertNotNull("Mediation Should Create Order", order);

			BigDecimal amount=BigDecimal.ZERO;
			BigDecimal quantity=BigDecimal.ZERO;
			for (OrderLineWS orderLine : order.getOrderLines()) {
				if (orderLine.getItemId().intValue() == testEnv.idForCode(OTC_FLAT_PRODUCT).intValue()) {
					    amount=amount.add(orderLine.getAmountAsDecimal());
						  quantity = quantity.add(orderLine.getQuantityAsDecimal());
				}
			}
			assertTrue(new BigDecimal("255").compareTo(amount) == 0);
			assertTrue(new BigDecimal("250").compareTo(quantity) == 0);
			api.undoMediation(uuid);
			MediationProcess mediationProcessUndone = api.getMediationProcess(uuid);
			assertNull(mediationProcessUndone, "Mediation process not expected!");
		});
	}

	@Test
	public void test_Mediation_TieredPrice(){

		final List<String> cdrs = new ArrayList<String>() {{
			add(DtMediationTestHelper.buildCDR(DEFAULT_CDR, "1500", TEST_USER_IDENTIFIER,OTC_TIERED_PRODUCT));
			add(DtMediationTestHelper.buildCDR(DEFAULT_CDR, "6700", TEST_USER_IDENTIFIER,OTC_TIERED_PRODUCT));
			add(DtMediationTestHelper.buildCDR(DEFAULT_CDR, "2300", TEST_USER_IDENTIFIER,OTC_TIERED_PRODUCT));
		}};

		testBuilder.validate((testEnv, testEnvBuilder) -> {
			// trigger mediation
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			uuid = api.processCDR(DtMediationTestHelper.getMediationConfiguration(api,
					DtMediationTestHelper.getMediationJobName()), cdrs);

			LOG.debug("Mediation ProcessId {}", uuid);
			assertNotNull("Mediation triggered should return uuid", uuid);

		}).validate((testEnv, testEnvBuilder) -> {
			JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
			LOG.debug("Mediation Process Status {}", api.getMediationProcessStatus());

			MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
			LOG.debug("Mediation Process {}", mediationProcess);

			assertEquals("Mediation Done And Billable ", Integer.valueOf(3), mediationProcess.getDoneAndBillable());
			assertEquals("Mediation Done And Not Billable", Integer.valueOf(0), mediationProcess.getDoneAndNotBillable());

			OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER));
			assertNotNull("Mediation Should Create Order", order);

			BigDecimal quantity = BigDecimal.ZERO;
			for (OrderLineWS orderLine : order.getOrderLines()) {
				if (orderLine.getItemId().intValue() == testEnv.idForCode(OTC_TIERED_PRODUCT).intValue()) {
					quantity = quantity.add(orderLine.getQuantityAsDecimal());
				}
			}
			assertTrue(new BigDecimal("10500").compareTo(quantity) == 0);
			api.undoMediation(uuid);
			MediationProcess mediationProcessUndone = api.getMediationProcess(uuid);
			assertNull(mediationProcessUndone, "Mediation process not expected!");
		});
	}

	// TEAR-DOWN

	@AfterClass
	public void tearDown() {
		testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
		testBuilder.removeEntitiesCreatedOnJBilling();
		testBuilder = null;
	}


	// non-test methods

	private void buildProducts(TestEnvironmentBuilder envBuilder, JbillingAPI api) {
		// 1. flat pricing
		DtMediationTestHelper.buildAndPersistProduct(envBuilder, api, OTC_FLAT_PRODUCT, false,
				envBuilder.idForCode(PRODUCT_CATEGORY), true, PriceModelStrategy.FLAT);

		// 2. tiered pricing
		DtMediationTestHelper.buildAndPersistProduct(envBuilder, api, OTC_TIERED_PRODUCT, false,
				envBuilder.idForCode(PRODUCT_CATEGORY), true, PriceModelStrategy.TIERED);
	}
}
