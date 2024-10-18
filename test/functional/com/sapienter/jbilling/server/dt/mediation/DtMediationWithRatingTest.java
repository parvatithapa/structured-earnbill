package com.sapienter.jbilling.server.dt.mediation;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.AssertJUnit.assertNotNull;

import java.lang.invoke.MethodHandles;
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
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;

@SuppressWarnings("Duplicates")
@Test(groups = {"dt-mediation"}, testName = "DtMediationWithRatingTest")
public class DtMediationWithRatingTest {

    private static final String ACCOUNT_NAME                                = "DT-Test-Account";
    private static final String	PRODUCT_CATEGORY                            = "OTC-Product-Rating-Suite";

    private static final String TIERED_RATING_SCHEME                        = "TieredRating101_" + System.currentTimeMillis();
    private static final String OTC_TIERED_RATING_PRODUCT                   = "OTC_OBS_TIERED_RATING_PROD_" + System.currentTimeMillis();

    private static final String KMS_RATING_SCHEME                           = "KMSRating101_" + System.currentTimeMillis();
    private static final String OTC_KMS_RATING_PRODUCT                      = "OTC_OBS_KMS_RATING_PROD_" + System.currentTimeMillis();

    private static final String DNS_RATING_SCHEME                           = "DNSRating101_" + System.currentTimeMillis();
    private static final String OTC_DNS_RATING_PRODUCT                      = "OTC_OBS_DNS_RATING_PROD_" + System.currentTimeMillis();

    private static final String MEDIATION_CONFIG_NAME                       = "dtMediationJob";

    private static final String METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER  = "externalAccountIdentifier";
    private static final String TEST_USER                                   = "TestUser-" + UUID.randomUUID().toString();
    private static final String TEST_USER_IDENTIFIER                        = UUID.randomUUID().toString();


    private TestBuilder testBuilder;
    private UUID uuid = null;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String TIERED_RATING_CDR =
            "20|19701010000000|%s|eu-de||otc.service.type.obs|otc.resource.type.obs|0||%s|20180420110000|20180420115959|accumulate_factor|%s|172.16.32.82,200,04660000016147A40D7CAED78D27C00F,obs-tc8425-w,99,99,08|%s|19701010000000|19701010000000|";

    private static final String KMS_RATING_CDR =
            "20|19701010000000|%s|eu-de||otc.service.type.obs|otc.resource.type.obs|0||%s|20180420110000|20180420115959|accumulate_factor|%s|172.16.32.82,200,04660000016147A40D7CAED78D27C00F,obs-tc8425-w,99,99,08|%s|19701010000000|19701010000000|";

    private static final String DNS_RATING_CDR =
            "20|19701010000000|%s|eu-de||otc.service.type.obs|otc.resource.type.obs|0||%s|20180420110000|20180420115959|accumulate_factor|%s|172.16.32.82,200,04660000016147A40D7CAED78D27C00F,obs-tc8425-w,99,99,08|%s|19701010000000|19701010000000|";


    @BeforeClass
    public void initializeTests() {

        testBuilder = DtMediationTestHelper.getTestEnvironment();

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            // account type
            DtMediationTestHelper.buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME);

            // products
            DtMediationTestHelper.buildAndPersistCategory(envBuilder, api, PRODUCT_CATEGORY, false,
                    ItemBuilder.CategoryType.ORDER_LINE_TYPE_ITEM);
            buildProducts(envBuilder, api);

            // customer Level metaField
            DtMediationTestHelper.buildAndPersistMetafield(testBuilder,
                    METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER,
                    DataType.STRING, EntityType.CUSTOMER);

            // create customer/user
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
            assertNotNull("OTC Item Creation Failed ", testEnvBuilder.idForCode(OTC_TIERED_RATING_PRODUCT));
            assertNotNull("OTC Item Creation Failed ", testEnvBuilder.idForCode(OTC_DNS_RATING_PRODUCT));
            assertNotNull("OTC Item Creation Failed ", testEnvBuilder.idForCode(OTC_KMS_RATING_PRODUCT));

            assertNotNull("Customer Level MetaField Creation Failed ", testEnvBuilder.idForCode(METAFIELD_CUST_EXTERNAL_ACCOUNT_IDENTIFIER));
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER));

            assertNotNull("Mediation Configuration  Creation Failed ", testEnvBuilder.idForCode(MEDIATION_CONFIG_NAME));

        });
    }


    // TESTS

    @Test
    public void test_Mediation_TieredRating(){

        final List<String> cdrs = new ArrayList<String>() {{
            add(DtMediationTestHelper.buildCDR(TIERED_RATING_CDR, "100", TEST_USER_IDENTIFIER,OTC_TIERED_RATING_PRODUCT));
            add(DtMediationTestHelper.buildCDR(TIERED_RATING_CDR, "4500", TEST_USER_IDENTIFIER,OTC_TIERED_RATING_PRODUCT));
            add(DtMediationTestHelper.buildCDR(TIERED_RATING_CDR, "3400", TEST_USER_IDENTIFIER,OTC_TIERED_RATING_PRODUCT));
            add(DtMediationTestHelper.buildCDR(TIERED_RATING_CDR, "2500", TEST_USER_IDENTIFIER,OTC_TIERED_RATING_PRODUCT));
        }};

        testBuilder.validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            uuid = api.processCDR(DtMediationTestHelper.getMediationConfiguration(api,
                    DtMediationTestHelper.getMediationJobName()), cdrs);

            logger.debug("Mediation ProcessId {}", uuid);
            assertNotNull("Mediation triggered should return uuid", uuid);

        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            logger.debug("Mediation Process Status {}", api.getMediationProcessStatus());

            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);

            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER));
            assertNotNull("Mediation Should Create Order", order);
            BigDecimal quantity=BigDecimal.ZERO;
            for (OrderLineWS orderLine : order.getOrderLines()) {
                if (orderLine.getItemId().intValue() == testEnv.idForCode(OTC_TIERED_RATING_PRODUCT).intValue()) {
                    quantity = quantity.add(orderLine.getQuantityAsDecimal());
                }
            }

            assertTrue(new BigDecimal("7").compareTo(quantity) == 0);

            api.undoMediation(uuid);
            MediationProcess mediationProcessUndone = api.getMediationProcess(uuid);
            assertNull(mediationProcessUndone, "Mediation process not expected!");
        });
    }

    @Test
    public void test_Mediation_DNSRating(){

        final List<String> cdrs = new ArrayList<String>() {{
            add(DtMediationTestHelper.buildCDR(DNS_RATING_CDR, "87000", TEST_USER_IDENTIFIER,OTC_DNS_RATING_PRODUCT));
            add(DtMediationTestHelper.buildCDR(DNS_RATING_CDR, "75000", TEST_USER_IDENTIFIER,OTC_DNS_RATING_PRODUCT));
        }};

        testBuilder.validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            uuid = api.processCDR(DtMediationTestHelper.getMediationConfiguration(api,
                    DtMediationTestHelper.getMediationJobName()), cdrs);

            logger.debug("Mediation ProcessId {}", uuid);
            assertNotNull("Mediation triggered should return uuid", uuid);

        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            logger.debug("Mediation Process Status {}", api.getMediationProcessStatus());

            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);

            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(1), mediationProcess.getDoneAndNotBillable());

            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER));
            assertNotNull("Mediation Should Create Order", order);

            BigDecimal quantity=BigDecimal.ZERO;
            for (OrderLineWS orderLine : order.getOrderLines()) {
                if (orderLine.getItemId().intValue() == testEnv.idForCode(OTC_DNS_RATING_PRODUCT).intValue()) {
                    quantity = quantity.add(orderLine.getQuantityAsDecimal());
                }
            }
            logger.debug("quantity {} ",quantity);
            assertTrue(new BigDecimal("1").compareTo(quantity) == 0);
            api.undoMediation(uuid);
            MediationProcess mediationProcessUndone = api.getMediationProcess(uuid);
            assertNull(mediationProcessUndone, "Mediation process not expected!");
        });
    }

    @Test
    public void test_Mediation_KMSRating(){

        final List<String> cdrs = new ArrayList<String>() {{
            add(DtMediationTestHelper.buildCDR(KMS_RATING_CDR, "100", TEST_USER_IDENTIFIER,OTC_KMS_RATING_PRODUCT));
            add(DtMediationTestHelper.buildCDR(KMS_RATING_CDR, "150", TEST_USER_IDENTIFIER,OTC_KMS_RATING_PRODUCT));
        }};

        testBuilder.validate((testEnv, testEnvBuilder) -> {
            // trigger mediation
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            uuid = api.processCDR(DtMediationTestHelper.getMediationConfiguration(api,
                    DtMediationTestHelper.getMediationJobName()), cdrs);

            logger.debug("Mediation ProcessId {}", uuid);
            assertNotNull("Mediation triggered should return uuid", uuid);

        }).validate((testEnv, testEnvBuilder) -> {
            JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            logger.debug("Mediation Process Status {}", api.getMediationProcessStatus());

            MediationProcess mediationProcess = api.getMediationProcess(api.getMediationProcessStatus().getMediationProcessId());
            logger.debug("Mediation Process {}", mediationProcess);

            assertEquals("Mediation Done And Billable ", Integer.valueOf(1), mediationProcess.getDoneAndBillable());
            assertEquals("Mediation Done And Not Billable", Integer.valueOf(1), mediationProcess.getDoneAndNotBillable());

            OrderWS order = api.getLatestOrder(testEnvBuilder.idForCode(TEST_USER));
            assertNotNull("Mediation Should Create Order", order);
            BigDecimal quantity=BigDecimal.ZERO;
            for (OrderLineWS orderLine : order.getOrderLines()) {
                if (orderLine.getItemId().intValue() == testEnv.idForCode(OTC_KMS_RATING_PRODUCT).intValue()) {
                    quantity = quantity.add(orderLine.getQuantityAsDecimal());
                }
            }
            logger.debug("quantity {} ",quantity);
            assertTrue(new BigDecimal("1").compareTo(quantity) == 0);
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
        // 1. tiered rating scheme
        Integer ratingSchemeId = DtMediationTestHelper.buildAndPersistRatingScheme(envBuilder, api,
                TIERED_RATING_SCHEME, DtMediationTestHelper.RatingSchemeType.TIERED);

        DtMediationTestHelper.buildAndPersistRatingProduct(envBuilder, api, OTC_TIERED_RATING_PRODUCT,
                false, envBuilder.idForCode(PRODUCT_CATEGORY), true, ratingSchemeId);

        // 2. dns rating scheme
        ratingSchemeId = DtMediationTestHelper.buildAndPersistRatingScheme(envBuilder, api,
                DNS_RATING_SCHEME, DtMediationTestHelper.RatingSchemeType.DNS);

        DtMediationTestHelper.buildAndPersistRatingProduct(envBuilder, api, OTC_DNS_RATING_PRODUCT, false,
                envBuilder.idForCode(PRODUCT_CATEGORY), true, ratingSchemeId);

        // 3. kms rating scheme
        ratingSchemeId = DtMediationTestHelper.buildAndPersistRatingScheme(envBuilder, api,
                KMS_RATING_SCHEME, DtMediationTestHelper.RatingSchemeType.KMS);

        DtMediationTestHelper.buildAndPersistRatingProduct(envBuilder, api, OTC_KMS_RATING_PRODUCT, false,
                envBuilder.idForCode(PRODUCT_CATEGORY), true, ratingSchemeId);
    }
}
