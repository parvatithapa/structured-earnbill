package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by coredevelopment on 03/03/16.
 */
@Test(groups = {"integration", "test-framework"})
public class TestForMultipleTestsWithSameEnvironment {
    private static TestBuilder testBuilder;

    @BeforeClass
    public static void initTestEnvironment() {
        testBuilder = TestBuilder.newTest().givenForMultiple(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.accountTypeBuilder(api)
                    .withName("testAccount").build();
        });
        TestEnvironment testEnv = TestForMultipleTestsWithSameEnvironment.testBuilder.test((environment) -> {
            assertNotNull(getAccountByDescription(environment.getPrancingPonyApi(), environment.jBillingCode("testAccount")));
        });
        assertNotNull(getAccountByDescription(testEnv.getPrancingPonyApi(), testEnv.jBillingCode("testAccount")));
    }

    @Test
    public void testAccountIsStillInTheSystem() {
        assertNotNull(getAccountByDescription(testBuilder.getTestEnvironment().getPrancingPonyApi(),
                testBuilder.getTestEnvironment().jBillingCode("testAccount")));
    }

    @Test
    public void testThingsCreatedDuringTestInTheEnvironmentShouldBeRemoved() {
        String accountToCreateForTest = "testAccount2";
        TestEnvironment testEnv = testBuilder.given(envBuilder -> {
            envBuilder.accountTypeBuilder(envBuilder.getPrancingPonyApi()).withName(accountToCreateForTest).build();
        }).test(env -> {
            assertNotNull(getAccountByDescription(env.getPrancingPonyApi(), env.jBillingCode(accountToCreateForTest)));
        });
        assertNull(getAccountByDescription(testEnv.getPrancingPonyApi(), testEnv.jBillingCode(accountToCreateForTest)));
    }

    @AfterClass
    public void destroyTestEnvironmentForMultipleTests() {
        String testAccountCode = testBuilder.getTestEnvironment().jBillingCode("testAccount");
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        assertNull(getAccountByDescription(testBuilder.getTestEnvironment().getPrancingPonyApi(), testAccountCode));
    }

    private static AccountTypeWS getAccountByDescription(JbillingAPI api, String description) {
        AccountTypeWS[] allAccountTypes = api.getAllAccountTypes();
        for (AccountTypeWS accountTypeWS: allAccountTypes) {
            if (accountTypeWS.getDescription(api.getCallerLanguageId()).getContent().equals(description))
                return accountTypeWS;
        }
        return null;
    }

}
