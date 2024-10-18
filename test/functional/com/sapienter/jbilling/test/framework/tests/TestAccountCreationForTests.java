package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Created by marcolin on 06/11/15.
 */
@Test(groups = {"integration", "test-framework"})
public class TestAccountCreationForTests {

    @Test
    public void testBasicAccountCreationAndCleanAfterTest() {
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.accountTypeBuilder(api)
                    .withName("testAccount").build();
        }).test((environment) -> {
            assertNotNull(getAccountByDescription(environment.getPrancingPonyApi(), environment.jBillingCode("testAccount")));
        });
        assertNull(getAccountByDescription(testEnv.getPrancingPonyApi(), testEnv.jBillingCode("testAccount")));
    }

    @Test
    public void testAccountWithInformationTypeShouldHaveMetaFields() {
        try {
            TestBuilder.newTest().given(environmentCreator -> {
                JbillingAPI api = environmentCreator.getPrancingPonyApi();
                environmentCreator.accountTypeBuilder(api)
                        .withName("testAccount")
                        .addAccountInformationType("testInformationType", new HashMap<>())
                        .build();
                fail("The account information without metafields should throw illegal argument exception");
            });
        } catch (IllegalArgumentException e) {} catch (Exception e) {
            fail("The account information without metafields should throw illegal argument exception");
        }
    }

    @Test
    public void testAccountWithMetaFieldsCreationAndCleanAfterTest() {
        String accountName = "testAccount";
        String informationName = "testInformationType";
        String metaFieldName = "testMetaFieldName";
        TestEnvironment testEnv = TestBuilder.newTest().given(environmentCreator -> {
            JbillingAPI api = environmentCreator.getPrancingPonyApi();
            environmentCreator.accountTypeBuilder(api)
                    .withName(accountName)
                    .addAccountInformationType(informationName, new HashMap<String, DataType>() {{
                        put(metaFieldName, DataType.STRING);
                    }})
                    .build();
        }).test((environment) -> {
            AccountTypeWS testAccount = getAccountByDescription(environment.getPrancingPonyApi(), environment.jBillingCode(accountName));
            assertEquals(environment.idForCode(informationName), testAccount.getInformationTypeIds()[0]);
        });
        assertNull(getAccountByDescription(testEnv.getPrancingPonyApi(), testEnv.jBillingCode("testAccount")));
    }

    @Test
    public void testAccountTwoInformationTypesTest() {
        String accountName = "testAccount";
        String informationName = "testInformationType";
        String informationName2 = "testInformationType2";
        String metaFieldName = "testMetaFieldName";
        String metaFieldName2 = "testMetaFieldName2";
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.accountTypeBuilder(api)
                    .withName(accountName)
                    .addAccountInformationType(informationName, new HashMap<String, DataType>() {{
                        put(metaFieldName, DataType.STRING);
                    }})
                    .addAccountInformationType(informationName2, new HashMap<String, DataType>() {{
                        put(metaFieldName2, DataType.STRING);
                    }})
                    .build();
        }).test((env) -> {
            AccountTypeWS testAccount = getAccountByDescription(env.getPrancingPonyApi(), env.jBillingCode(accountName));
            assertEquals(2, testAccount.getInformationTypeIds().length);
        });
        assertNull(getAccountByDescription(testEnv.getPrancingPonyApi(), testEnv.jBillingCode("testAccount")));
    }

    @Test
    public void testAccountWithRandomNameAndCleanAfterTest() {
        String accountName = "testAccount";
        String informationName = "testInformationType";
        String metaFieldName = "testMetaFieldName";
        TestEnvironment testEnvironment = TestBuilder.newTest().given(environmentCreator -> {
            JbillingAPI api = environmentCreator.getPrancingPonyApi();
            environmentCreator.accountTypeBuilder(api)
                    .withName(accountName)
                    .addAccountInformationType(informationName, new HashMap<String, DataType>() {{
                        put(metaFieldName, DataType.STRING);
                    }})
                    .build();
        }).test((environment) -> {
            AccountTypeWS testAccount = getAccountByDescription(environment.getPrancingPonyApi(), environment.jBillingCode(accountName));
            assertNotNull("Description should exist", testAccount.getDescription(environment.getPrancingPonyApi().getCallerLanguageId()));
            assertTrue("Description should be exact", !testAccount.getDescription(environment.getPrancingPonyApi().getCallerLanguageId()).getContent().equals(accountName));
        });
        assertNull(getAccountByDescription(testEnvironment.getPrancingPonyApi(), testEnvironment.jBillingCode("testAccount")));
    }

    @Test
    public void testAccountWithExactNameAndCleanAfterTest() {
        String accountName = "testAccount";
        String informationName = "testInformationType";
        String metaFieldName = "testMetaFieldName";
        TestEnvironment testEnvironment = TestBuilder.newTest().given(environmentCreator -> {
            JbillingAPI api = environmentCreator.getPrancingPonyApi();
            environmentCreator.accountTypeBuilder(api)
                    .withName(accountName)
                    .useExactDescription(true)
                    .addAccountInformationType(informationName, new HashMap<String, DataType>() {{
                        put(metaFieldName, DataType.STRING);
                    }})
                    .build();
        }).test((environment) -> {
            AccountTypeWS testAccount = getAccountByDescription(environment.getPrancingPonyApi(), environment.jBillingCode(accountName));
            assertNotNull("Description should exist", testAccount.getDescription(environment.getPrancingPonyApi().getCallerLanguageId()));
            assertTrue("Description should be exact", testAccount.getDescription(environment.getPrancingPonyApi().getCallerLanguageId()).getContent().equals(accountName));
        });
        assertNull(getAccountByDescription(testEnvironment.getPrancingPonyApi(), testEnvironment.jBillingCode("testAccount")));
    }

    private AccountTypeWS getAccountByDescription(JbillingAPI api, String description) {
        AccountTypeWS[] allAccountTypes = api.getAllAccountTypes();
        for (AccountTypeWS accountTypeWS: allAccountTypes) {
            if (accountTypeWS.getDescription(api.getCallerLanguageId()).getContent().equals(description))
                return accountTypeWS;
        }
        return null;
    }
}
