package com.sapienter.jbilling.server.movius;

import static org.testng.AssertJUnit.assertNotNull;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleType;
import com.sapienter.jbilling.server.metafields.validation.ValidationRuleWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;

@Test(groups = { "movius" }, testName = "movius")
public class UniqueMetaFieldOrgIdValidationTest {

    private static final Logger LOG = LoggerFactory.getLogger(MoviusMediationTest.class);
    private EnvironmentHelper envHelper;
    private TestBuilder       testBuilder;
    private static final Integer CC_PM_ID                                      = 5;
    private static final String  ACCOUNT_NAME                                  = "Movius Test Account";
    private static final String  META_FIELD_CUSTOMER_ORG_ID                    = "Org Id";
    private static final int     NEXT_INVOICE_DAY                              =  1;
    private static final String  TEST_USER_1                                   = "Test-User-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "Test-User-"+ UUID.randomUUID().toString();
    private static final int     MONTHLY_ORDER_PERIOD                          =  2;
    
    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            this.envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }
    
    @BeforeClass
    public void initializeTests() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            
            // Creating account type 
            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);
            
            // Creating Customer Level MetaField with validation 
            ValidationRuleWS validationRule = new ValidationRuleWS();
            validationRule.setEnabled(true);
            validationRule.setErrorMessages(Arrays.asList(new InternationalDescriptionWS(api.getCallerLanguageId(), "Please Enter Unique Org Id")));
            validationRule.setRuleType(ValidationRuleType.UNIQUE_VALUE.name());
            
            buildAndPersistMetafield(testBuilder, META_FIELD_CUSTOMER_ORG_ID, DataType.STRING, EntityType.CUSTOMER, validationRule);
            
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
        });

    }
    
    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        testBuilder.removeEntitiesCreatedOnJBilling();
        envHelper = null;
        testBuilder = null;
    }
    
    @Test(expectedExceptions = SessionInternalError.class, expectedExceptionsMessageRegExp = ".* Unique Org .*")
    public void test001UnqiueOrgIdValidation() {
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.set(Calendar.YEAR, 2017);
        nextInvoiceDate.set(Calendar.MONTH, 6);
        nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
        String orgId = "2020";
        testBuilder.given(envBuilder -> {
            LOG.debug("Creating User With Next InvoiceDate {} with Org Id {}", nextInvoiceDate.getTime(), orgId);
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.customerBuilder(api)
                      .withUsername(TEST_USER_1)
                      .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                      .addTimeToUsername(false)
                      .withNextInvoiceDate(nextInvoiceDate.getTime())
                      .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                      .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                      .build();
            
        }).validate((testEnv, testEnvBuilder) -> {
            assertNotNull("Customer Creation Failed", testEnvBuilder.idForCode(TEST_USER_1));
        }).validate((testEnv, testEnvBuilder) -> {
            LOG.debug("Creating User With Next InvoiceDate {} with Org Id {}", nextInvoiceDate.getTime(), orgId);
            final JbillingAPI api = testEnvBuilder.getPrancingPonyApi();
            testEnvBuilder.customerBuilder(api)
                          .withUsername(TEST_USER_2)
                          .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                          .addTimeToUsername(false)
                          .withNextInvoiceDate(nextInvoiceDate.getTime())
                          .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                          .withMetaField(META_FIELD_CUSTOMER_ORG_ID, orgId)
                          .build();
        });
    }
    
    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .build();
        return accountTypeWS.getId();
    }
    
    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType, ValidationRuleWS rule) {
        MetaFieldWS value =  new MetaFieldBuilder()
                                .name(name)
                                .dataType(dataType)
                                .entityType(entityType)
                                .primary(true)
                                .validationRule(rule)
                                .build();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
        
    }
}
