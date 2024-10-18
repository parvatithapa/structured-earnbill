package com.sapienter.jbilling.test.framework.tests;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestBuilder;
import org.testng.annotations.Test;

import java.util.Hashtable;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Created by marcolin on 06/11/15.
 */
@Test(groups = {"integration", "test-framework"})
public class TestCompanyConfigurationForTests {

    @Test
    public void testCreateEnumerationAndCleanAfterTest() {
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.configurationBuilder(api).addEnumeration("STATE", "New York", "Texas").build();
        }).test((env) -> {
            assertNotNull(env.getPrancingPonyApi().getEnumerationByName("STATE"));
        });
        assertNull(testEnv.getPrancingPonyApi().getEnumerationByName("STATE"));
    }

    @Test
    public void testCreateMetaFieldOnEntityAndRemoveAfterTest() {
        String testMetafieldName = "TestMetaFieldName";
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.configurationBuilder(api).addMetaField(testMetafieldName, DataType.BOOLEAN, EntityType.CUSTOMER).build();
        }).test((env) -> {
            MetaFieldWS metaField = env.getPrancingPonyApi().getMetaField(env.idForCode(testMetafieldName));
            assertEquals(DataType.BOOLEAN, metaField.getDataType());
            assertEquals(testMetafieldName, metaField.getName());
            assertEquals(EntityType.CUSTOMER, metaField.getEntityType());
            assertEquals(true, metaField.isPrimary());
        });
        for (MetaFieldWS metaFieldWS: testEnv.getPrancingPonyApi().getMetaFieldsForEntity(EntityType.CUSTOMER.name())) {
            if (metaFieldWS.getName().equals(testMetafieldName)) fail("There should not be any metafield after the test");
        }
    }

    @Test
    public void testSetPluginForTestAndRemoveIdAfterTest() {
        String pluginTestClass = "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask";
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI api = envBuilder.getPrancingPonyApi();
            envBuilder.configurationBuilder(api).addPlugin(pluginTestClass).build();
        }).test((env) -> {
            assertNotNull(env.getPrancingPonyApi().getPluginWS(env.idForCode(pluginTestClass)));
        });
        try {
            testEnv.getPrancingPonyApi().getPluginWS(testEnv.idForCode(pluginTestClass));
            fail("It should throw an exception because the plugin is not there anymore");
        } catch (SessionInternalError e) {
            // If Plugin is not there jBilling throw an exception
        }
    }

    @Test
    public void testSetPluginWithParametersTest() {
        String pluginTestClass = "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask";
        String testParameterKey = "TestParameter";
        String testParameterValue = "TestParameterValue";
        TestBuilder.newTest().given(envBuilder ->
                        envBuilder.configurationBuilder(envBuilder.getPrancingPonyApi())
                                .addPluginWithParameters(pluginTestClass,
                                        new Hashtable<String, String>() {{
                                            put(testParameterKey, testParameterValue);
                                        }})
                                .build()
        ).test((env) -> {
            PluggableTaskWS pluginWS = env.getPrancingPonyApi().getPluginWS(env.idForCode(pluginTestClass));
            assertNotNull(pluginWS);
            Map.Entry<String, String> entry = pluginWS.getParameters().entrySet().iterator().next();
            assertEquals(testParameterKey, entry.getKey());
            assertEquals(testParameterValue, entry.getValue());
        });
    }

    @Test
    public void testCreateDataTableAndCleanAfterTests() {
        String testRouteName = "TestRoute";
        String testFilePath = "./test/functional/com/sapienter/jbilling/test/framework/tests/testDataTable.csv";
        final Integer[] routeId = new Integer[1];
        TestEnvironment testEnv = TestBuilder.newTest().given(envBuilder -> {
            JbillingAPI prancingPonyApi = envBuilder.getPrancingPonyApi();
            envBuilder.configurationBuilder(prancingPonyApi).addRoute(testRouteName, testFilePath).build();
        }).test((env) -> {
            routeId[0] = env.idForCode(testRouteName);
            assertNotNull(env.getPrancingPonyApi().getRoute(routeId[0]));
        });
        assertNull(testEnv.getPrancingPonyApi().getRoute(routeId[0]));
    }
}
