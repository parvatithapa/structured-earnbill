package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.mediation.MediationConfigurationWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.joda.time.format.DateTimeFormat;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Created by marcolin on 06/11/15.
 */
@Test(groups = {"integration", "test-framework"})
public class TestEnvironmentCreatorTests {

    @Test
    public void testCustomerCreationAndCleanAfterTest() {
        final Integer[] customerIds = new Integer[1];
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.customerBuilder(api)
                    .withUsername("testCustomer").build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            customerIds[0] = env.idForCode("testCustomer");
            UserWS testCustomer = api.getUserWS(customerIds[0]);
            assertNotNull(testCustomer);
            assertEquals(0, testCustomer.getDeleted());
        });
        assertEquals(1, testEnv.getPrancingPonyApi().getUserWS(customerIds[0]).getDeleted());
    }

    @Test
    public void testCustomerCreationWithPaymentInstrumentsAndCleanAfterTest() {
        String date = DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(Util.truncateDate(new Date()).getTime());
        final Integer[] customerIds = new Integer[1];
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer paymentMethodTypeId = envBuilder.paymentMethodTypeBuilder(api, "TestCC")
                    .buildCCPaymentMethodType().getId();
            CustomerBuilder customerBuilder = envBuilder.customerBuilder(api);
            customerBuilder.withUsername("testCustomer")
                    .addPaymentInstrument(
                            customerBuilder.paymentInformation()
                                    .withProcessingOrder(Integer.valueOf(1))
                                    .withPaymentMethodId(Constants.PAYMENT_METHOD_VISA)
                                    .withPaymentMethodTypeId(paymentMethodTypeId)
                                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS("cc.cardholder.name", "testCustomer"))
                                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS("cc.number", "4111111111111152"))
                                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS("cc.expiry.date", date))
                                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS("cc.type", Constants.PAYMENT_METHOD_VISA))
                                    .addMetaFieldValue(ApiBuilderHelper.getMetaFieldValueWS("cc.gateway.key", "zzzxxxaaa"))
                                    .build()
                    )
                    .build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            customerIds[0] = env.idForCode("testCustomer");
            UserWS testCustomer = api.getUserWS(customerIds[0]);
            List<PaymentInformationWS> paymentInstruments = testCustomer.getPaymentInstruments();
            assertNotNull("Payment instruments expected!", paymentInstruments);
            assertEquals("Invalid number of instruments!", Integer.valueOf(paymentInstruments.size()), Integer.valueOf(1));
            PaymentInformationWS paymentInstrument = paymentInstruments.get(0);
            assertEquals("Invalid processing order!", paymentInstrument.getProcessingOrder(), Integer.valueOf(1));
            assertEquals("Invalid method type Id!", paymentInstrument.getPaymentMethodTypeId(), env.idForCode("TestCC"));
//            assertEquals("Invalid method Id!", paymentInstrument.getPaymentMethodId(), Constants.PAYMENT_METHOD_VISA);
            MetaFieldValueWS[] metaFieldValues = paymentInstrument.getMetaFields();
            assertEquals("Invalid number of meta-field values!", Integer.valueOf(metaFieldValues.length),
                    Integer.valueOf(5));
            sortMetaFieldsByName(metaFieldValues);
            validateMetaFieldValue(metaFieldValues[0], "cc.cardholder.name", "testCustomer");
            validateMetaFieldValue(metaFieldValues[1], "cc.expiry.date", date);
            validateMetaFieldValue(metaFieldValues[2], "cc.gateway.key", "zzzxxxaaa");
            validateMetaFieldValue(metaFieldValues[3], "cc.number", "4111111111111152");
            validateMetaFieldValue(metaFieldValues[4], "cc.type", Constants.PAYMENT_METHOD_VISA);

        });
        assertEquals(1, testEnv.getPrancingPonyApi().getUserWS(customerIds[0]).getDeleted());
    }

    @Test
    public void testProductCategoryCreationAndCleanAfterTest() {
        final Integer[] itemCategoryIdCreated = {null};
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            itemCategoryIdCreated[0] = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            ItemTypeWS testCategory = api.getItemCategoryById(env.idForCode("testCategory"));
            assertNotNull(testCategory);
        });
        assertNull(testEnv.getPrancingPonyApi().getItemCategoryById(itemCategoryIdCreated[0]));
    }

    @Test
    public void testProductCreationAndCleanAfterTest() {
        final String productCode = "testProduct";
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            envBuilder.itemBuilder(api).item().withCode(productCode).withType(testCategory).build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer itemId = api.getItemID(env.jBillingCode(productCode));
            assertEquals(env.idForCode(productCode), itemId);
        });
        assertNull(testEnv.getPrancingPonyApi().getItemID(productCode));
    }

    @Test
    public void testProductWithFlatPriceCreationAndCleanAfterTest() {
        final String productCode = "testProduct";
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.customerBuilder(api).withUsername("testCustomer").build();
            Integer testCategory = envBuilder.itemBuilder(api).itemType().withCode("testCategory").build();
            envBuilder.itemBuilder(api).item().withCode(productCode).withType(testCategory)
                    .withFlatPrice("1").build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            ItemDTOEx item = api.getItem(env.idForCode(productCode), env.idForCode("testCustomer"), null);
            assertEquals(env.idForCode(productCode), item.getId());
            assertEquals(BigDecimal.ONE.setScale(2), item.getPriceAsDecimal().setScale(2));
        });
        assertNull(testEnv.getPrancingPonyApi().getItemID(productCode));
    }

    @Test
    public void testProductWithFlatPriceCreationAndUpdateOfFlatPrice() {
        String testCustomer = "testCustomer";
        String testCategory = "testCategory";
        String testProduct = "testProduct";
        TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.customerBuilder(api).withUsername(testCustomer).build();
            Integer testCategoryId = envBuilder.itemBuilder(api).itemType().withCode(testCategory).build();
            envBuilder.itemBuilder(api).item().withCode(testProduct).withType(testCategoryId)
                    .withFlatPrice("1").build();
        }).test((env) -> {
            JbillingAPI api = env.getPrancingPonyApi();
            ItemDTOEx item = api.getItem(env.idForCode(testProduct), env.idForCode(testCustomer), null);
            assertEquals("1", item.getPrice().substring(0, 1));
            item.getDefaultPrice().setRate("3");
            api.updateItem(item);
            ItemDTOEx itemUpdated = api.getItem(env.idForCode(testProduct), env.idForCode(testCustomer), null);
            assertEquals("3", itemUpdated.getPrice().substring(0, 1));
        });
    }


    @Test
    public void testMediationConfigCreationAndCleanAfterTest() {
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder ->
                        envBuilder.mediationConfigBuilder(envBuilder.getPrancingPonyApi()).withName("TestConfig")
                                .withLauncher("sampleMediationJob").build()
        ).test((env) -> {
            MediationConfigurationWS[] allMediationConfigurations = env.getPrancingPonyApi().getAllMediationConfigurations();
            assertTrue(Arrays.asList(allMediationConfigurations).stream()
                    .anyMatch(mc -> mc.getId().equals(env.idForCode("TestConfig"))));
        });
        MediationConfigurationWS[] allMediationConfigurations = testEnv.getPrancingPonyApi().getAllMediationConfigurations();
        assertFalse(Arrays.asList(allMediationConfigurations).stream()
                .anyMatch(mc -> mc.getId().equals(testEnv.idForCode("TestConfig"))));
    }

    private void sortMetaFieldsByName(MetaFieldValueWS[] metaFieldValues){

        Arrays.sort(metaFieldValues, (o1, o2) ->
                o1.getFieldName().compareTo(o2.getFieldName()));
    }

    private void validateMetaFieldValue(MetaFieldValueWS metaFieldValue, String name, Object value){

        assertEquals("Invalid meta-field name!!", metaFieldValue.getFieldName(), name);
        assertEquals("Invalid meta-field value!!", metaFieldValue.getValue(), value);
    }
}
