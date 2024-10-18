package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

/**
 * Test cases for Payment method type
 * builder creations and clean up.
 *
 * @author Vojislav Stanojevikj
 * @since 10-JUN-2016.
 */
@Test(groups = {"integration", "test-framework"})
public class TestPaymentMethodTypeCreationForTests {

    @Test
    public void testPaymentMethodTypeCreationAndCleanUp() {

        final TestBuilder testBuilder = TestBuilder.newTest();
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        final String[] paymentTypesCodes = new String[]{"CreditCardTestPaymentMethodType",
                "ACHTestPaymentMethodType", "ChequeTestPaymentMethodType"};

        TestEnvironment testEnvironment = testBuilder.given(envBuilder -> {
            envBuilder.paymentMethodTypeBuilder(api, paymentTypesCodes[0])
                    .buildCCPaymentMethodType();
            envBuilder.paymentMethodTypeBuilder(api, paymentTypesCodes[1])
                    .buildACHPaymentMethodType();
            envBuilder.paymentMethodTypeBuilder(api, paymentTypesCodes[2])
                    .buildChequePaymentMethodType();
        }).test((env) -> {

            Integer paymentMethodTypeId = env.idForCode(paymentTypesCodes[0]);
            assertNotNull(paymentMethodTypeId, "Payment method id not found!");
            PaymentMethodTypeWS paymentMethodType = api.getPaymentMethodType(paymentMethodTypeId);
            validatePaymentMethodTypeFields(paymentMethodType, "Payment Card", api.getCallerCompanyId(),
                    Integer.valueOf(1), false);
            validatePaymentMethodMetaFields(paymentMethodType, Integer.valueOf(5), new ValidatedMetaField[]{
                    new ValidatedMetaField("cc.cardholder.name", DataType.STRING),
                    new ValidatedMetaField("cc.expiry.date", DataType.STRING),
                    new ValidatedMetaField("cc.gateway.key", DataType.STRING),
                    new ValidatedMetaField("cc.number", DataType.STRING),
                    new ValidatedMetaField("cc.type", DataType.INTEGER)
            });

            paymentMethodTypeId = env.idForCode(paymentTypesCodes[1]);
            assertNotNull(paymentMethodTypeId, "Payment method id not found!");
            paymentMethodType = api.getPaymentMethodType(paymentMethodTypeId);
            validatePaymentMethodTypeFields(paymentMethodType, "ACH", api.getCallerCompanyId(),
                    Integer.valueOf(2), false);
            validatePaymentMethodMetaFields(paymentMethodType, Integer.valueOf(6), new ValidatedMetaField[]{
                    new ValidatedMetaField("ach.account.number", DataType.STRING),
                    new ValidatedMetaField("ach.account.type", DataType.ENUMERATION),
                    new ValidatedMetaField("ach.bank.name", DataType.STRING),
                    new ValidatedMetaField("ach.customer.name", DataType.STRING),
                    new ValidatedMetaField("ach.gateway.key", DataType.STRING),
                    new ValidatedMetaField("ach.routing.number", DataType.STRING)
            });

            paymentMethodTypeId = env.idForCode(paymentTypesCodes[2]);
            assertNotNull(paymentMethodTypeId, "Payment method id not found!");
            paymentMethodType = api.getPaymentMethodType(paymentMethodTypeId);
            validatePaymentMethodTypeFields(paymentMethodType, "Cheque", api.getCallerCompanyId(),
                    Integer.valueOf(3), false);
            validatePaymentMethodMetaFields(paymentMethodType, Integer.valueOf(3), new ValidatedMetaField[]{
                    new ValidatedMetaField("cheque.bank.name", DataType.STRING),
                    new ValidatedMetaField("cheque.date", DataType.DATE),
                    new ValidatedMetaField("cheque.number", DataType.STRING)
            });
        });
        validateDeletedPaymentMethodType(paymentTypesCodes[0], testEnvironment);
        validateDeletedPaymentMethodType(paymentTypesCodes[1], testEnvironment);
        validateDeletedPaymentMethodType(paymentTypesCodes[2], testEnvironment);
    }

    @Test
    public void testPaymentMethodTypeFromScratchCreationAndCleanUpUsingTemplate() {

        final TestBuilder testBuilder = TestBuilder.newTest();
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        final String[] paymentTypesCodes = new String[]{"TestPaymentMethodType"};
        TestEnvironment testEnvironment = testBuilder.given(envBuilder -> {
            envBuilder.paymentMethodTypeBuilder(api, paymentTypesCodes[0])
                    .withMethodName("TestMethodName")
                    .withOwningEntityId(api.getCallerId())
                    .withTemplateId(Integer.valueOf(1)) // Credit Card Template
                    .allAccountType(true)
                    .isRecurring(true)
                    .build();
        }).test((env) -> {

            Integer paymentMethodTypeId = env.idForCode(paymentTypesCodes[0]);
            assertNotNull(paymentMethodTypeId, "Payment method id not found!");
            PaymentMethodTypeWS paymentMethodType = api.getPaymentMethodType(paymentMethodTypeId);
            validatePaymentMethodTypeFields(paymentMethodType, "TestMethodName", api.getCallerCompanyId(),
                    Integer.valueOf(1), true);
            validatePaymentMethodMetaFields(paymentMethodType, Integer.valueOf(5), new ValidatedMetaField[]{
                    new ValidatedMetaField("cc.cardholder.name", DataType.STRING),
                    new ValidatedMetaField("cc.expiry.date", DataType.STRING),
                    new ValidatedMetaField("cc.gateway.key", DataType.STRING),
                    new ValidatedMetaField("cc.number", DataType.STRING),
                    new ValidatedMetaField("cc.type", DataType.INTEGER)
            });
        });
        validateDeletedPaymentMethodType(paymentTypesCodes[0], testEnvironment);
    }

    @Test
    public void testPaymentMethodTypeFromScratchCreationAndCleanUpWithCustomMetaFields() {

        final TestBuilder testBuilder = TestBuilder.newTest();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        final String[] paymentTypesCodes = new String[]{"TestPaymentMethodType"};
        TestEnvironment testEnvironment = testBuilder.given(envBuilder -> {
            envBuilder.paymentMethodTypeBuilder(api, paymentTypesCodes[0])
                    .withMethodName("TestMethodName")
                    .withOwningEntityId(api.getCallerId())
                    .withTemplateId(Integer.valueOf(-1)) // Custom template
                    .allAccountType(true)
                    .isRecurring(true)
                    .addMetaField(ApiBuilderHelper.getMetaFieldWS("xx.test.1", DataType.STRING,
                            EntityType.PAYMENT_METHOD_TYPE, api.getCallerCompanyId()))
                    .addMetaField(ApiBuilderHelper.getMetaFieldWS("xx.test.2", DataType.INTEGER,
                            EntityType.PAYMENT_METHOD_TYPE, api.getCallerCompanyId()))
                    .addMetaField(ApiBuilderHelper.getMetaFieldWS("xx.test.3", DataType.ENUMERATION,
                            EntityType.PAYMENT_METHOD_TYPE, api.getCallerCompanyId()))
                    .build();
        }).test((env) -> {

            Integer paymentMethodTypeId = env.idForCode(paymentTypesCodes[0]);
            assertNotNull(paymentMethodTypeId, "Payment method id not found!");
            PaymentMethodTypeWS paymentMethodType = api.getPaymentMethodType(paymentMethodTypeId);
            validatePaymentMethodTypeFields(paymentMethodType, "TestMethodName", api.getCallerCompanyId(),
                    Integer.valueOf(-1), true);
            validatePaymentMethodMetaFields(paymentMethodType, Integer.valueOf(3), new ValidatedMetaField[]{
                    new ValidatedMetaField("xx.test.1", DataType.STRING),
                    new ValidatedMetaField("xx.test.2", DataType.INTEGER),
                    new ValidatedMetaField("xx.test.3", DataType.ENUMERATION)
            });
        });
        validateDeletedPaymentMethodType(paymentTypesCodes[0], testEnvironment);
    }

    private void validatePaymentMethodTypeFields(PaymentMethodTypeWS paymentMethodType, String methodName,
                                                 Integer entityId, Integer templateId, boolean isRecurring){

        assertNotNull(paymentMethodType, "Payment method not found!");
        assertTrue(paymentMethodType.getMethodName().contains(methodName), "Invalid method name!");
        assertEquals(paymentMethodType.getOwningEntityId(), entityId, "Invalid owning entity id!");
        assertEquals(paymentMethodType.getTemplateId(), templateId, "Invalid template id!");
        assertEquals(paymentMethodType.getIsRecurring(), Boolean.valueOf(isRecurring), "Invalid isRecurring field!");
    }

    private void validatePaymentMethodMetaFields(PaymentMethodTypeWS paymentMethodType, Integer numberOfFields,
                                                 ValidatedMetaField... validatedMetaFields){

        MetaFieldWS[] metaFields = paymentMethodType.getMetaFields();
        assertNotNull(metaFields, "No meta-fields found!!");
        assertEquals(Integer.valueOf(metaFields.length), numberOfFields, "Invalid number of meta-fields!");
        sortMetaFieldsByName(metaFields);
        for (int i = 0; i < numberOfFields; i++){
            assertEquals(metaFields[i].getName(), validatedMetaFields[i].name, "Invalid meta-field name!");
            assertEquals(metaFields[i].getDataType(), validatedMetaFields[i].dataType, "Invalid meta-field data-type!");
            assertEquals(metaFields[i].getEntityType(), EntityType.PAYMENT_METHOD_TYPE, "Invalid meta-field entity-type!");
        }
    }

    private void validateDeletedPaymentMethodType(String paymentTypeCode, TestEnvironment testEnvironment){
        Integer paymentMethodTypeId = testEnvironment.idForCode(paymentTypeCode);
        assertNull(paymentMethodTypeId, "Payment method id not found");
        PaymentMethodTypeWS paymentMethodType = testEnvironment.getPrancingPonyApi().getPaymentMethodType(paymentMethodTypeId);
        assertNull(paymentMethodType, "Payment method not found");
    }

    private void sortMetaFieldsByName(MetaFieldWS[] metaFields){
        Arrays.sort(metaFields, (o1, o2) ->
                o1.getName().compareTo(o2.getName()));
    }

    private class ValidatedMetaField{

        String name;
        DataType dataType;

        public ValidatedMetaField(String name, DataType dataType) {
            this.name = name;
            this.dataType = dataType;
        }

    }

}
