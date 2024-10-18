package com.sapienter.jbilling.server.sapphire;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;


@Test(groups = { "sapphire" }, testName = "sapphire")
@ContextConfiguration(classes = UpdateCustomerLoginNameTaskTest.class)
public class UpdateCustomerLoginNameTaskTest extends AbstractTestNGSpringContextTests {

    private TestBuilder       testBuilder;
    private EnvironmentHelper envHelper;
    private static final Integer CC_PM_ID                                      = 5;
    private static final String  ACCOUNT_NAME                                  = "Sapphire Test Account";
    private static final String  TEST_USER_1                                   = "Test-User-1-"+ UUID.randomUUID().toString();
    private static final String  TEST_USER_2                                   = "Test-User-2-"+ UUID.randomUUID().toString();
    private static final String  ALREADY_IN_USE_USER                           = "AlreadyinUseUser";
    private static final String  AIT_GTOUP_NAME_1                              = "AIT Group Name 1";
    private static final String  AIT_GTOUP_NAME_2                              = "AIT Group Name 2";
    private static final String  AIT_META_FIELD_NAME_1                         = "Customer Login Name 1";
    private static final String  AIT_META_FIELD_NAME_2                         = "Customer Login Name 2";
    private static final int     MONTHLY_ORDER_PERIOD                          = 2;
    private static final int     NEXT_INVOICE_DAY                              = 1;

    private Integer[] groupids;
    private List<Integer> userIds = new ArrayList<>();
    private int aitGroupId1;
    private int aitGroupId2;
    private int updateCustomerLoginNamePluginId;
    private JbillingAPI api;
    private PluggableTaskTypeWS pluginType;
    

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            envHelper = EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
            api = testEnvCreator.getPrancingPonyApi();
        });
    }

    @BeforeClass
    public void beforeClass() {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            // Creating account type
            buildAndPersistAccountType(envBuilder, api, ACCOUNT_NAME, CC_PM_ID);
            pluginType = api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.customer.task.UpdateCustomerLoginNameTask");
            PluggableTaskWS updateCustomerLoginNamePlugin = new PluggableTaskWS();
            Hashtable<String, String> updateCustomerLoginNamePluginParameters = new Hashtable<>();
            updateCustomerLoginNamePluginParameters.put("Login Name Meta Field Name", AIT_META_FIELD_NAME_1);
            updateCustomerLoginNamePluginParameters.put("AIT Meta Field Group Name", AIT_GTOUP_NAME_1);
            updateCustomerLoginNamePlugin.setParameters(updateCustomerLoginNamePluginParameters);
            updateCustomerLoginNamePlugin.setTypeId(pluginType.getId());
            updateCustomerLoginNamePlugin.setProcessingOrder(190);
            updateCustomerLoginNamePluginId = api.createPlugin(updateCustomerLoginNamePlugin);

            for (Integer groupid : groupids) {
                AccountInformationTypeWS ait = api.getAccountInformationType(groupid);
                if(null != ait){
                    if (AIT_GTOUP_NAME_1.equals(ait.getName())) {
                        aitGroupId1 = ait.getId();
                    }else if (AIT_GTOUP_NAME_2.equals(ait.getName())) {
                        aitGroupId2 = ait.getId();
                    }
                }
            }
        }).test((testEnv, testEnvBuilder) -> {
            assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(ACCOUNT_NAME));
        });
    }

    @Test
    public void test01UpdateCustomerLoginNameTaskTest() {
        try {
            testBuilder.given(envBuilder -> {

                logger.info("creating user 1");
                UserWS user1 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_1)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .withMetaField(AIT_META_FIELD_NAME_1, "NewupdatedUser_1", aitGroupId1)
                        .withMetaField(AIT_META_FIELD_NAME_2, "NewupdatedUser_2", aitGroupId1)
                        .withMetaField(AIT_META_FIELD_NAME_1, "NewupdatedUser_11", aitGroupId2)
                        .withMetaField(AIT_META_FIELD_NAME_2, "NewupdatedUser_12", aitGroupId2)
                        .build();
                userIds.add(user1.getId());
                UserWS user2 = envBuilder.customerBuilder(api)
                        .withUsername(TEST_USER_2)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .withMetaField(AIT_META_FIELD_NAME_1, ALREADY_IN_USE_USER, aitGroupId1)
                        .build();
                userIds.add(user2.getId());
                UserWS user3 = envBuilder.customerBuilder(api)
                        .withUsername(ALREADY_IN_USE_USER)
                        .withAccountTypeId(testBuilder.getTestEnvironment().idForCode(ACCOUNT_NAME))
                        .addTimeToUsername(false)
                        .withNextInvoiceDate(new Date())
                        .withMainSubscription(new MainSubscriptionWS(MONTHLY_ORDER_PERIOD, NEXT_INVOICE_DAY))
                        .build();
                userIds.add(user3.getId());
            }).validate((testEnv, testEnvBuilder) -> {
                //Scenario 1 user name updated with Metafield name.
                UserWS user = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_1));
                api.updateUser(user);
                user = api.getUserWS(user.getId());
                assertEquals("NewupdatedUser_1",user.getUserName());

                //Scenario 2 login name already in use validation scenario
                try{
                    UserWS user1 = api.getUserWS(testEnvBuilder.idForCode(TEST_USER_2));
                    api.updateUser(user1);
                    assertEquals("User should not be update here ","1", "2" );
                }catch (Exception e) {
                    assertTrue("User not updated here ", true);
                }

                //scenario 3 Test with different AIT group and AIT meta-field value.
                //3.a With different metafield
                updateCustomerLoginNamePluginId = updatePlugin(AIT_META_FIELD_NAME_2, AIT_GTOUP_NAME_1);
                api.updateUser(user);
                user = api.getUserWS(user.getId());
                assertEquals("NewupdatedUser_2",user.getUserName());

                //3.b With Different Metafield group 
                updateCustomerLoginNamePluginId = updatePlugin(AIT_META_FIELD_NAME_1, AIT_GTOUP_NAME_2);
                api.updateUser(user);
                user = api.getUserWS(user.getId());
                assertEquals("NewupdatedUser_11",user.getUserName());

                updateCustomerLoginNamePluginId = updatePlugin(AIT_META_FIELD_NAME_2, AIT_GTOUP_NAME_2);
                api.updateUser(user);
                user = api.getUserWS(user.getId());
                assertEquals("NewupdatedUser_12",user.getUserName());

                //scenario 4 By removing plugin
                api.deletePlugin(updateCustomerLoginNamePluginId);
                MetaFieldValueWS metaFieldValueWS1 = new MetaFieldValueWS();
                metaFieldValueWS1.setFieldName(AIT_META_FIELD_NAME_2);
                metaFieldValueWS1.getMetaField().setDataType(DataType.STRING);
                metaFieldValueWS1.setGroupId(aitGroupId2);
                metaFieldValueWS1.setValue("ChangedNewupdatedUSer_12");
                user = api.getUserWS(user.getId());
                MetaFieldValueWS[] metefields1 = {metaFieldValueWS1};
                user.setMetaFields(metefields1);
                api.updateUser(user);
                user = api.getUserWS(user.getId());
                assertNotEquals("ChangedNewupdatedUser_12", user.getUserName() );

            });
        }catch(Exception e){
            assertEquals("User should not be update here ","1", "2" );
            logger.error(e.getMessage());
        }finally{
            for (Integer userid : userIds) {
                api.deleteUser(userid);
            }
        }
    }

    private Integer buildAndPersistAccountType(TestEnvironmentBuilder envBuilder, JbillingAPI api, String name, Integer ...paymentMethodTypeId) {
        HashMap<String,DataType> metaFields = new HashMap<>();
        metaFields.put(AIT_META_FIELD_NAME_1, DataType.STRING);
        metaFields.put(AIT_META_FIELD_NAME_2, DataType.STRING);
        AccountTypeWS accountTypeWS = envBuilder.accountTypeBuilder(api)
                .withName(name)
                .withPaymentMethodTypeIds(paymentMethodTypeId)
                .addAccountInformationType(AIT_GTOUP_NAME_1, metaFields)
                .addAccountInformationType(AIT_GTOUP_NAME_2, metaFields)
                .build();
        accountTypeWS = api.getAccountType(accountTypeWS.getId());
        groupids = accountTypeWS.getInformationTypeIds();
        return accountTypeWS.getId();
    }

    private Integer updatePlugin(String metefieldName, String groupNmae) {
        api.deletePlugin(updateCustomerLoginNamePluginId);
        PluggableTaskWS updateCustomerLoginNamePlugin = new PluggableTaskWS();
        Hashtable<String, String> updateCustomerLoginNamePluginParameters = new Hashtable<>();
        updateCustomerLoginNamePluginParameters.put("Login Name Meta Field Name", metefieldName);
        updateCustomerLoginNamePluginParameters.put("AIT Meta Field Group Name", groupNmae);
        updateCustomerLoginNamePlugin.setParameters(updateCustomerLoginNamePluginParameters);
        updateCustomerLoginNamePlugin.setTypeId(pluginType.getId());
        updateCustomerLoginNamePlugin.setProcessingOrder(190);
        return api.createPlugin(updateCustomerLoginNamePlugin);
    }

    @AfterClass
    public void tearDown() {
        testBuilder.removeEntitiesCreatedOnJBilling();
        if (null != envHelper) {
            envHelper = null;
        }
        testBuilder = null;
    }
}