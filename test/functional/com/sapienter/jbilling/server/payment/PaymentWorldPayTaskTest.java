package com.sapienter.jbilling.server.payment;

import static org.junit.Assert.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sapienter.jbilling.test.framework.builders.PaymentMethodTypeBuilder;

import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodTypeWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.AccountInformationTypeWS;
import com.sapienter.jbilling.server.user.AccountTypeWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;


@Test(groups="external-system", testName="PaymentWorldPayTaskTest")
public class PaymentWorldPayTaskTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private JbillingAPI api;
    private static TestBuilder testBuilder;
    private TestEnvironment environment;
    private int worldPayPaymentMethodTypeId;
    private Integer accountTypeId = 60106;
    private Integer billingSectionId = 16;
    private Integer paymentMethodId;
    private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
    private final static String CC_MF_NUMBER = "cc.number";
    private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
    private final static String CC_MF_TYPE = "cc.type";
    private final static String GATEWAY_KEY = "cc.gateway.key";
    private final static String VALID_CREDIT_CARD_NUMBER_1 = "5555555555554444";
    private final static String VALID_CREDIT_CARD_NUMBER_2 = "4444333322221111";
    private final static String INVALID_CREDIT_CARD_NUMBER = "5257279846844523";
    private final static String PAYMENT_FAKE_TASK_CLASS_NAME = "com.sapienter.jbilling.server.pluggableTask.PaymentFakeTask";
    private final static String PAYMENT_ROUTER_CCF_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentRouterCCFTask";
    private final static String PAYMENT_FILTER_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentFilterTask";
    private final static String PAYMENT_ROUTER_CURRENCY_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentRouterCurrencyTask";
    public  final static String PAYMENT_WORLDPAY_EXTERNAL_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.PaymentWorldPayExternalTask";
    public  final static String SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME = "com.sapienter.jbilling.server.payment.tasks.SaveCreditCardExternallyTask";
    private Integer paymentWorldPayExternalPluginId;
    private Integer saveCreditCardPluginId;
    private Integer userId = null;
    private final static String SERVICE_KEY = "T_S_7e1a31ff-e3d6-4ba1-9021-993d3d8b2f26";
    private final static String CLIENT_KEY = "T_C_9dac4292-f93e-485e-8ba8-a6befe3271dd"; 
    private static final String WORLDPAY_PAYMENT_METHOD_NAME = "WorldPay Token";
    private static final String USER_CREATION_FAILED = "Failed to create user ";
    private static final String PAYMENT_NOT_NULL = "Payment can not be null";
    private static final String USER_ID_NOT_NULL="User Id cannot be null";
    private static final String PAYMENT_RESULT_NOT_NULL="Payment result not null";


    @BeforeClass
    public void initializeTests() throws JbillingAPIException, IOException {
        logger.debug("Initialize WorldPay test");
        api = JbillingAPIFactory.getAPI();
        testBuilder = getTestEnvironment();
        environment = testBuilder.getTestEnvironment();
        configureWorldpayPlugin();        
       
    }
    public TestBuilder configureWorldpayPlugin() throws JbillingAPIException, IOException {
        return TestBuilder
                .newTest(false)
                .givenForMultiple(envCreator -> {
                    updateProcessingOrderOfPlugin(api, PAYMENT_FAKE_TASK_CLASS_NAME);
                    updateProcessingOrderOfPlugin(api, PAYMENT_FILTER_TASK_CLASS_NAME);
                    updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CCF_TASK_CLASS_NAME);
                    updateProcessingOrderOfPlugin(api, PAYMENT_ROUTER_CURRENCY_TASK_CLASS_NAME);
                    updateProcessingOrderOfPlugin(api, PAYMENT_WORLDPAY_EXTERNAL_TASK_CLASS_NAME);
                    updateProcessingOrderOfPlugin(api, SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME);

                    testBuilder.given(envBuilder -> {
                        worldPayPaymentMethodTypeId = buildAndPersistPaymentMethodType(WORLDPAY_PAYMENT_METHOD_NAME);
                        AccountTypeWS accountType = new AccountTypeBuilder()
                        .addDescription("World Pay Test Account "+System.currentTimeMillis(), api.getCallerLanguageId())
                        .paymentMethodTypeIds(new Integer[]{worldPayPaymentMethodTypeId})
                        .create(api);
                        accountTypeId = accountType.getId();
                        logger.debug("accountTypeId :: "+accountTypeId);
                        assertNotNull("Account Type should be created", accountTypeId);
                        
                        
                        Map<MetaFieldType, DataType> contactSection = new HashMap<MetaFieldType, DataType>();
                        contactSection.put(MetaFieldType.FIRST_NAME, DataType.STRING);
                        contactSection.put(MetaFieldType.LAST_NAME, DataType.STRING);
                        contactSection.put(MetaFieldType.COUNTRY_CODE, DataType.STRING);
                        contactSection.put(MetaFieldType.POSTAL_CODE, DataType.STRING);
                        contactSection.put(MetaFieldType.STATE_PROVINCE, DataType.STRING);

                        Map<MetaFieldType, DataType> billingSection = new HashMap<MetaFieldType, DataType>();
                        billingSection.put(MetaFieldType.INITIAL, DataType.BOOLEAN);
                        billingSection.put(MetaFieldType.FIRST_NAME, DataType.STRING);
                        billingSection.put(MetaFieldType.LAST_NAME, DataType.STRING);
                        billingSection.put(MetaFieldType.COUNTRY_CODE, DataType.STRING);
                        billingSection.put(MetaFieldType.POSTAL_CODE, DataType.STRING);
                        billingSection.put(MetaFieldType.STATE_PROVINCE, DataType.STRING);

                        billingSectionId = buildAitWithMetaFields(accountType, billingSection, "Billing Info");
                        logger.debug("billingSectionId :::::: "+ billingSectionId);
                        assertNotNull("Billing Section shoud be created", billingSectionId);
                    }).test((testEnv, testEnvBuilder) -> {
                        assertNotNull("Account Creation Failed", testEnvBuilder.idForCode(WORLDPAY_PAYMENT_METHOD_NAME));
                    });                   

                    PluggableTaskWS paymentWorldPayPlugin = new PluggableTaskWS();
                    PluggableTaskWS[]  worldPayPluggableTask = api.getPluginsWS(api.getCallerCompanyId(), PAYMENT_WORLDPAY_EXTERNAL_TASK_CLASS_NAME);
                    if (worldPayPluggableTask.length > 0)
                        paymentWorldPayPlugin = worldPayPluggableTask[0]; 

                    paymentWorldPayPlugin.setProcessingOrder(1);
                    paymentWorldPayPlugin.setTypeId(api.getPluginTypeWSByClassName(PAYMENT_WORLDPAY_EXTERNAL_TASK_CLASS_NAME).getId());

                    Hashtable<String, String> parameters = new Hashtable<String, String>();

                    parameters.put("ServiceKey", SERVICE_KEY);
                    parameters.put("ClientKey", CLIENT_KEY);

                    paymentWorldPayPlugin.setParameters(parameters);
                    paymentWorldPayPlugin.setOwningEntityId(api.getCallerCompanyId());
                    paymentWorldPayExternalPluginId = api.createPlugin(paymentWorldPayPlugin);
                    System.out.println("paymentWorldPayExternalPluginIdddd   :"+paymentWorldPayExternalPluginId);// create new plugin
                    //api.updatePlugin(paymentWorldPayPlugin);
                    //paymentWorldPayExternalPluginId = paymentWorldPayPlugin.getId(); 
                    logger.debug("PayExternalPluginId : "+paymentWorldPayExternalPluginId);

                    PluggableTaskWS saveCreditCardExternalPlugin = new PluggableTaskWS();

                    PluggableTaskWS[]  saveCreditCardExternalPlugins = api.getPluginsWS(api.getCallerCompanyId(), SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME);
                    if (saveCreditCardExternalPlugins.length > 0)
                        saveCreditCardExternalPlugin = saveCreditCardExternalPlugins[0]; 

                    saveCreditCardExternalPlugin.setOwningEntityId(api.getCallerCompanyId());
                    saveCreditCardExternalPlugin.setProcessingOrder(1);
                    saveCreditCardExternalPlugin.setTypeId(api.getPluginTypeWSByClassName(SAVE_CREDIT_CARD_EXTERNAL_TASK_CLASS_NAME).getId());
                    Hashtable<String, String> saveCreditCardExternalparameters = new Hashtable<String, String>();

                    saveCreditCardExternalparameters = new Hashtable<String, String>();
                    saveCreditCardExternalparameters.put("contactType", billingSectionId.toString());
                    saveCreditCardExternalparameters.put("externalSavingPluginId",paymentWorldPayExternalPluginId.toString());
                    saveCreditCardExternalparameters.put("obscureOnFail", "false");
                    saveCreditCardExternalparameters.put("removeOnFail", "false");
                    saveCreditCardExternalPlugin.setParameters(saveCreditCardExternalparameters);
                    saveCreditCardPluginId = api.createPlugin(saveCreditCardExternalPlugin);
                    System.out.println("saveCreditCardPluginIdddd   :"+saveCreditCardPluginId);
                   // api.updatePlugin(saveCreditCardExternalPlugin);
                    saveCreditCardPluginId = saveCreditCardExternalPlugin.getId();
                    logger.debug("saveCreditCardPluginId : ", saveCreditCardPluginId );
                    });
    }
    private TestBuilder getTestEnvironment() {

        return TestBuilder.newTest(false).givenForMultiple(testEnvCreator -> {
            EnvironmentHelper.getInstance(testEnvCreator.getPrancingPonyApi());
        });
    }
    
    
    @Test
    public void test001GatewayTokenCreationWithValidCard() throws Exception {
        
        
        testBuilder.given(
                envBuilder -> {
                    try{
                        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
                        assertNotNull(USER_CREATION_FAILED, user);
                        userId = api.createUser(user);
                        logger.debug("userId ::::::::::::: {}", userId);
                        assertNotNull("USER_ID_NOT_NULL", userId);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
        
                }).test(env -> {        
        
        
                    UserWS user = api.getUserWS(userId);

        assertNotNull(USER_CREATION_FAILED, user);
        
        MetaFieldValueWS[] metaFields = user.getPaymentInstruments().get(0).getMetaFields();
        
        char[] token = Arrays.stream(metaFields)
                                  .filter(metaField -> metaField.getFieldName().equals(GATEWAY_KEY))
                                  .map(metaField -> metaField.getCharValue())
                                  .findFirst()
                                  .orElse(" ".toCharArray());
        assertNotNull("Token cannot be null", token);
        char[] cardNumber = Arrays.stream(metaFields)
                .filter(metaField -> metaField.getFieldName().equals(CC_MF_NUMBER))
                .map(metaField -> metaField.getCharValue())
                .findFirst()
                .orElse(" ".toCharArray());

        assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));
        api.deleteUser(userId);
        
                });
    }


   /* @Test //Need to confirm if user will be created or not for invalid card.
    public void test002GatewayTokenCreationWithInvalidCard() throws Exception {
        UserWS user = createUser(new Date(), true, Boolean.FALSE, true, accountTypeId, billingSectionId);
        assertNotNull("user Should be created", user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        //System.out.println("User id for invalid caerd is :"+user.getId());
    }*/

     @Test
    public void test003MultiplePaymentWithRecurringToken() throws Exception {

         testBuilder.given(
                envBuilder -> {
                    try {
                        UserWS user = createUser(new Date(), true,
                                Boolean.TRUE, true, accountTypeId,
                                billingSectionId);
                        assertNotNull(USER_CREATION_FAILED, user);
                        userId = api.createUser(user);
                        logger.debug("userId ::::::::::::: {}", userId);
                        assertNotNull("USER_ID_NOT_NULL", userId);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }).test(env -> {  
        
                 UserWS user = api.getUserWS(userId);

        //first payment
        PaymentWS payment = createPayment(user.getId(), new BigDecimal("15"), user.getPaymentInstruments().get(0));
        logger.debug("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

        // check payment has zero balance
        PaymentWS firstPayment = api.getLatestPayment(user.getId());
        assertNotNull(PAYMENT_NOT_NULL, firstPayment);
        System.out.println("Authorization Id :"+firstPayment.getAuthorizationId());
        assertNotNull("auth in payment can not be null", firstPayment.getAuthorizationId());
        assertEquals("correct payment amount", new BigDecimal("15.0000000000"), firstPayment.getAmountAsDecimal());

        //second payment
        payment = createPayment(user.getId(), new BigDecimal("25"), user.getPaymentInstruments().get(0));
        logger.debug("Processing token payment...");
        authInfo = api.processPayment( payment, null);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

        // check payment has zero balance
        PaymentWS secondpayment = api.getLatestPayment(user.getId());
        assertNotNull(PAYMENT_NOT_NULL, secondpayment);
        System.out.println("Authorization Id :"+secondpayment.getAuthorizationId());
        assertNotNull("auth in payment can not be null", secondpayment.getAuthorizationId());
        assertEquals("correct payment amount", new BigDecimal("25.0000000000"), secondpayment.getAmountAsDecimal());

        api.deletePayment(firstPayment.getId());
        api.deletePayment(secondpayment.getId());
        api.deleteUser(userId);
             });
    }

      @Test
    public void test004SuccessOneTimePayment() throws Exception{

        logger.debug("test004SuccessOneTimePayment");
        testBuilder.given(
                envBuilder -> {                    
                try{
        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull(USER_CREATION_FAILED, user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
        
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).test(env -> {  
                UserWS user = api.getUserWS(userId);
        assertNotNull("user Should be created", user);

        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
                VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
        logger.debug("creditCard number :::::::: {}", VALID_CREDIT_CARD_NUMBER_2);
        // Added new credit card for payment instead of user saved instrument
        PaymentWS payment = createPayment(user.getId(), new BigDecimal("30"), cc);

        // Testing one time payment through Worldpay
        logger.debug("Processing token payment...");
        PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

        // check payment has zero balance
        PaymentWS lastPayment = api.getLatestPayment(user.getId());

        assertNotNull(PAYMENT_NOT_NULL, lastPayment);
        assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
        assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
        assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());
        api.deleteUser(userId);
            });
    }
      
      @Test
      public void test004SuccessOneTimePaymentWithoutAnyInstrumentRegistered() throws Exception{

          logger.debug("test004SuccessOneTimePayment");
          testBuilder.given(
                  envBuilder -> {                    
                  try{
          UserWS user = createUser(new Date(), true, Boolean.TRUE, false, accountTypeId, billingSectionId);
          assertNotNull(USER_CREATION_FAILED, user);
          userId = api.createUser(user);
          logger.debug("userId ::::::::::::: {}", userId);
          assertNotNull("USER_ID_NOT_NULL", userId);
          
                  } catch (Exception e) {
                      e.printStackTrace();
                  }

              }).test(env -> {  
                  UserWS user = api.getUserWS(userId);
          assertNotNull("user Should be created", user);

          // add a credit card
          Calendar expiry = Calendar.getInstance();
          expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

          PaymentInformationWS cc = createCreditCard("Frodo Baggins",
                  VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
          logger.debug("creditCard number :::::::: {}", VALID_CREDIT_CARD_NUMBER_2);
          // Added new credit card for payment instead of user saved instrument
          PaymentWS payment = createPayment(user.getId(), new BigDecimal("30"), cc);

          // Testing one time payment through Worldpay
          logger.debug("Processing token payment...");
          PaymentAuthorizationDTOEx authInfo = api.processPayment( payment, null);
          assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
          assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

          // check payment has zero balance
          PaymentWS lastPayment = api.getLatestPayment(user.getId());

          assertNotNull(PAYMENT_NOT_NULL, lastPayment);
          assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
          assertEquals("correct payment amount", new BigDecimal("30.0000000000"), lastPayment.getAmountAsDecimal());
          assertEquals("correct payment balance",new BigDecimal("30.0000000000"), lastPayment.getBalanceAsDecimal());
          api.deleteUser(userId);
              });
      }


    
    public void test005AddPaymentSecondInstrumentForExistingCustomer() throws Exception
    {
        
        testBuilder.given(
                envBuilder -> {                    
                try{
        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull(USER_CREATION_FAILED, user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
        
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).test(env -> { 
        
        UserWS user = api.getUserWS(userId);
        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
                VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
        user.getPaymentInstruments().clear();
        user.getPaymentInstruments().add(cc);        
        api.updateUser(user);
        //api.deleteUser(userId);
            });
    }
   
    public void testUpdateUsersPaymentInstrument () throws Exception {
        testBuilder.given(
                envBuilder -> {                    
                try{
        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull(USER_CREATION_FAILED, user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).test(env -> { 
        UserWS user = api.getUserWS(userId);
        boolean userUpdated = false;
        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        PaymentInformationWS cc = createCreditCard("Frodo BagginsUpdate",
                VALID_CREDIT_CARD_NUMBER_2, expiry.getTime());
        user.getPaymentInstruments().clear();
        api.updateUser(user);   
        UserWS newUser = api.getUserWS( user.getId() );
        newUser.getPaymentInstruments().add(cc);
        api.updateUser(newUser);
        api.deleteUser(userId);
            });
    }


    public void testRemoveUsersPaymentInstrument () throws Exception {
        testBuilder.given(
                envBuilder -> {                    
                try{
        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull(USER_CREATION_FAILED, user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
        user = api.getUserWS(userId);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).test(env -> { 
        UserWS newUser = api.getUserWS(userId);

        List<PaymentInformationWS> paymentInstruments = newUser.getPaymentInstruments();
        MetaFieldValueWS[] metaFields = (paymentInstruments.get(0)).getMetaFields();

        char[] cardNumber = Arrays.stream(metaFields)
                .filter(metaField -> metaField.getFieldName().equals(CC_MF_NUMBER))
                .map(metaField -> metaField.getCharValue())
                .findFirst()
                .orElse(" ".toCharArray());

        assertTrue("Credit Card Number should be obscure",cardNumber != null && new String(cardNumber).startsWith("************"));

        newUser.getPaymentInstruments().clear();
        api.updateUser(newUser);
        newUser = api.getUserWS(newUser.getId());
        api.deleteUser(userId);
            });
    }
     
    public void testRefundPayment() throws Exception
    {
        testBuilder.given(
                envBuilder -> {                    
                try{

        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull(USER_CREATION_FAILED, user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).test(env -> { 
        UserWS user = api.getUserWS(userId);
        PaymentWS payment = createPayment(user.getId(), new BigDecimal("22"), user.getPaymentInstruments().get(0));
        System.out.println("payment is :"+payment);
        logger.debug("Processing token payment...");

        PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());


        // check payment has zero balance
        PaymentWS lastPayment = api.getLatestPayment(user.getId());
        System.out.println("lastPayment is :"+lastPayment);
        assertNotNull(PAYMENT_NOT_NULL, lastPayment);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
        PaymentWS refundPayment = createPayment(user.getId(), new BigDecimal(lastPayment.getAmount()), user.getPaymentInstruments().get(0));

        refundPayment.setPaymentId(lastPayment.getId());
        refundPayment.setIsRefund(1);
        PaymentAuthorizationDTOEx refundAuthInfo = api.processPayment(refundPayment, null);
        System.out.println("refundPayment is :"+refundPayment);
        assertNotNull(PAYMENT_NOT_NULL, refundPayment);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, refundAuthInfo);
        api.deleteUser(userId);
            });
    }

    public void testPartialRefundPayment() throws Exception
    {
        testBuilder.given(
                envBuilder -> {                    
                try{
        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull(USER_CREATION_FAILED, user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).test(env -> { 
        UserWS user = api.getUserWS(userId);
        PaymentWS payment = createPayment(user.getId(), new BigDecimal("22"), user.getPaymentInstruments().get(0));
        logger.debug("Processing token payment...");

        PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());


        // check payment has zero balance
        PaymentWS lastPayment = api.getLatestPayment(user.getId());
        assertNotNull(PAYMENT_NOT_NULL, lastPayment);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, authInfo);
        assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
        PaymentWS refundPayment = createPayment(user.getId(), new BigDecimal("15"), user.getPaymentInstruments().get(0));

        refundPayment.setPaymentId(lastPayment.getId());
        refundPayment.setIsRefund(1);
        PaymentAuthorizationDTOEx refundAuthInfo = api.processPayment(refundPayment, null);
        assertNotNull(PAYMENT_NOT_NULL, refundPayment);
        assertNotNull(PAYMENT_RESULT_NOT_NULL, refundAuthInfo);
        api.deleteUser(userId);
            });
    }

    public static UserWS createUser(Date nextInvoiceDate, boolean populateBillingGroupAddress, 
            Boolean isValidCreditcard, boolean shouldAddPaymentInstrument, Integer accountTypeId, 
            Integer groupId) throws JbillingAPIException, IOException {
        UserWS newUser = new UserWS();
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
        newUser.setUserId(0);
        newUser.setUserName("testUserName-"
                + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("P@ssword12");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(accountTypeId);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(null);
        newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
        newUser.setInvoiceChild(new Boolean(false));
        
        logger.debug("User properties set");
        metaFieldValues = getMetafieldValues(populateBillingGroupAddress, groupId);
        newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));
        logger.debug("Meta field values set");
        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        if (shouldAddPaymentInstrument) {
            String creditCard =null;
            if(null != isValidCreditcard)
                creditCard = (isValidCreditcard == Boolean.TRUE ? VALID_CREDIT_CARD_NUMBER_1 : INVALID_CREDIT_CARD_NUMBER);
            logger.debug("creditCard :::::::: {}", creditCard);
            PaymentInformationWS cc = createCreditCard("test name",
                    creditCard, expiry.getTime()); //Frodo Baggins
            newUser.getPaymentInstruments().add(cc);
        }
        logger.debug("Creating user ...");
        MainSubscriptionWS billing = new MainSubscriptionWS();
        billing.setPeriodId(2);
        billing.setNextInvoiceDayOfPeriod(1);
        newUser.setMainSubscription(billing);
        newUser.setNextInvoiceDate(nextInvoiceDate);
        logger.debug("User created with id: {}", newUser.getUserId());
        return newUser;
    }
    
    private static List<MetaFieldValueWS> getMetafieldValues(
            boolean populateBillingGroupAddress, Integer groupId) {
        List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("Country");
        metaField1.getMetaField().setDataType(DataType.STRING);
        metaField1.setValue("CA");
        metaFieldValues.add(metaField1);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("State/Province");
        metaField2.getMetaField().setDataType(DataType.STRING);
        metaField2.setValue("OR");
        metaFieldValues.add(metaField2);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("First Name");
        metaField3.getMetaField().setDataType(DataType.STRING);
        metaField3.setValue("Frodo");
        metaFieldValues.add(metaField3);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("Last Name");
        metaField4.getMetaField().setDataType(DataType.STRING);
        metaField4.setValue("Baggins");
        metaFieldValues.add(metaField4);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("Address 1");
        metaField5.getMetaField().setDataType(DataType.STRING);
        metaField5.setValue("Baggins");
        metaFieldValues.add(metaField5);

        MetaFieldValueWS metaField6 = new MetaFieldValueWS();
        metaField6.setFieldName("City");
        metaField6.getMetaField().setDataType(DataType.STRING);
        metaField6.setValue("Baggins");
        metaFieldValues.add(metaField6);

        MetaFieldValueWS metaField7 = new MetaFieldValueWS();
        metaField7.setFieldName("Email Address");
        metaField7.getMetaField().setDataType(DataType.STRING);
        metaField7.setValue("test@shire.com");
        metaFieldValues.add(metaField7);

        MetaFieldValueWS metaField8 = new MetaFieldValueWS();
        metaField8.setFieldName("Postal Code");
        metaField8.getMetaField().setDataType(DataType.STRING);
        metaField8.setValue("K0");
        metaFieldValues.add(metaField8);

        if (populateBillingGroupAddress) {

            MetaFieldValueWS metaField9 = new MetaFieldValueWS();
            metaField9.setFieldName("COUNTRY_CODE");
            metaField9.getMetaField().setDataType(DataType.STRING);
            metaField9.setValue("CA");
            metaField9.setGroupId(groupId);
            metaFieldValues.add(metaField9);

            MetaFieldValueWS metaField10 = new MetaFieldValueWS();
            metaField10.setFieldName("STATE_PROVINCE");
            metaField10.getMetaField().setDataType(DataType.STRING);
            metaField10.setValue("OR");
            metaField10.setGroupId(groupId);
            metaFieldValues.add(metaField10);

            MetaFieldValueWS metaField12 = new MetaFieldValueWS();
            metaField12.setFieldName("LAST_NAME");
            metaField12.getMetaField().setDataType(DataType.STRING);
            metaField12.setValue("Baggins");
            metaField12.setGroupId(groupId);
            metaFieldValues.add(metaField12);
            MetaFieldValueWS metaField16 = new MetaFieldValueWS();
            metaField16.setFieldName("POSTAL_CODE");
            metaField16.setValue("K0");
            metaField16.getMetaField().setDataType(DataType.STRING);
            metaField16.setGroupId(groupId);
            metaFieldValues.add(metaField16);
        }
            return metaFieldValues;
        }

    public static PaymentInformationWS createCreditCard(String cardHolderName,
            String cardNumber, Date date) {
        PaymentInformationWS cc = new PaymentInformationWS();
        TestEnvironment environment = testBuilder.getTestEnvironment();
        cc.setPaymentMethodTypeId(environment.idForCode(WORLDPAY_PAYMENT_METHOD_NAME));
        cc.setProcessingOrder(new Integer(1));
        cc.setPaymentMethodId(CommonConstants.PAYMENT_METHOD_MASTERCARD);
        //cc

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
                DataType.CHAR, 1, cardHolderName.toCharArray());
        addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2,
                cardNumber!=null ? cardNumber.toCharArray(): cardNumber);
        addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
                DataType.CHAR, 3, (new SimpleDateFormat(
                        Constants.CC_DATE_FORMAT).format(date)).toCharArray());

        // have to pass meta field card type for it to be set
        /*addMetaField(metaFields, CC_MF_TYPE, false, false,
                DataType.STRING, 4, CreditCardType.MASTER_CARD);*/
        addMetaField(metaFields, GATEWAY_KEY ,false, false, DataType.CHAR, 5, null );
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));
        cc.setCvv("123");

        return cc;
    }
    
    

    public static PaymentWS createPayment(Integer userId, BigDecimal amount, PaymentInformationWS paymentInformation) {

        PaymentWS payment = new PaymentWS();
        payment.setAmount(amount);
        payment.setIsRefund(0);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(Integer.valueOf(1));
        payment.setUserId(userId);
        payment.getPaymentInstruments().add(paymentInformation);

        return payment;

    }

    public static PaymentWS createRefund(Integer userId, BigDecimal amount, PaymentInformationWS paymentInformation) {

        PaymentWS payment = new PaymentWS();
        payment.setAmount(amount);
        payment.setIsRefund(1);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(Integer.valueOf(1));
        payment.setUserId(userId);
        payment.getPaymentInstruments().add(paymentInformation);

        return payment;

    }
    private static void addMetaField(List<MetaFieldValueWS> metaFields,
            String fieldName, boolean disabled, boolean mandatory,
            DataType dataType, Integer displayOrder, Object value) {
        MetaFieldValueWS ws = new MetaFieldValueWS();
        ws.setFieldName(fieldName);
        ws.getMetaField().setDisabled(disabled);
        ws.getMetaField().setMandatory(mandatory);
        ws.getMetaField().setDataType(dataType);
        ws.getMetaField().setDisplayOrder(displayOrder);
        ws.setValue(value);

        metaFields.add(ws); 
    }

    private Integer buildAitWithMetaFields(AccountTypeWS accountType, Map<MetaFieldType, DataType> metaFieldMap, String sectionName) {
        // create Metafield
        List<MetaFieldWS> metaFields = metaFieldMap
                .entrySet()
                .stream()
                .map(fieldEntry -> {
                    MetaFieldWS metaField = new MetaFieldBuilder()
                    .dataType(fieldEntry.getValue())
                    .entityType(EntityType.ACCOUNT_TYPE)
                    .name(fieldEntry.getKey().name())
                    .fieldUsage(fieldEntry.getKey())
                    .build();
                    return metaField;
                }).collect(Collectors.toList());
        // create AIT
        AccountInformationTypeWS ait = new AccountInformationTypeBuilder(
                accountType).name(sectionName).addMetaFields(metaFields)
                .build();

        return api.createAccountInformationType(ait);
    }

    private void updateProcessingOrderOfPlugin(JbillingAPI api, String className) {
        PluggableTaskWS[] plugins = api.getPluginsWS(api.getCallerCompanyId(), className);
        Arrays.stream(plugins).forEach(
                pluggableTaskWS -> {
                    pluggableTaskWS.setProcessingOrder(pluggableTaskWS
                            .getProcessingOrder() + 100);
                    pluggableTaskWS.setParameters(new Hashtable<>(
                            pluggableTaskWS.getParameters()));
                    api.updatePlugin(pluggableTaskWS);
                });
    }
    
    private Integer buildAndPersistPaymentMethodType(String code) {
        List<Integer> accountTypeIds = new ArrayList<>();
        accountTypeIds.add(accountTypeId);
        PaymentMethodTypeWS paymentMethod = PaymentMethodTypeBuilder.getBuilder(api, environment, code)
                .withMethodName(code).withAccountTypes(accountTypeIds).withTemplateId(1)
                .withMetaFields(getMetafieldList()).isRecurring(true).build();
        List<MetaFieldWS> metaFields = getMetafieldList();
        paymentMethod.setMetaFields(metaFields.toArray(new MetaFieldWS[metaFields.size()]));
        api.updatePaymentMethodType(paymentMethod);
        return paymentMethod.getId();

    }
    
    private List<MetaFieldWS> getMetafieldList() {
        List<MetaFieldWS> list = new ArrayList<>();
        list.add(buildAndPersistMetafield(CC_MF_CARDHOLDER_NAME, DataType.CHAR, MetaFieldType.TITLE, Integer.valueOf(1)));
        list.add(buildAndPersistMetafield(CC_MF_NUMBER, DataType.CHAR, MetaFieldType.PAYMENT_CARD_NUMBER,
                Integer.valueOf(2)));
        list.add(buildAndPersistMetafield(CC_MF_EXPIRY_DATE, DataType.CHAR, MetaFieldType.DATE, Integer.valueOf(3)));
        list.add(buildAndPersistMetafield(CC_MF_TYPE, DataType.STRING, MetaFieldType.CC_TYPE, Integer.valueOf(4)));
        list.add(buildAndPersistMetafield("autopayment.authorization", DataType.BOOLEAN,
                MetaFieldType.AUTO_PAYMENT_AUTHORIZATION, Integer.valueOf(5)));
        list.add(buildAndPersistMetafield(GATEWAY_KEY, DataType.CHAR, MetaFieldType.GATEWAY_KEY, Integer.valueOf(6)));
        return list;
    }

    private MetaFieldWS buildAndPersistMetafield(String name, DataType dataType, MetaFieldType fieldUsage,
            Integer displayOrder) {
        return new MetaFieldBuilder().name(name).dataType(dataType).entityType(EntityType.PAYMENT_METHOD_TYPE)
                .fieldUsage(fieldUsage).displayOrder(displayOrder).build();
    }
    
    /* public void test002UpdateUserAddInstrument() throws JbillingAPIException, IOException
    {
        Calendar expiry = Calendar.getInstance();
        UserWS user = createUser(new Date(), true, Boolean.TRUE, true, accountTypeId, billingSectionId);
        assertNotNull("user Should be created", user);
        userId = api.createUser(user);
        logger.debug("userId ::::::::::::: {}", userId);
        assertNotNull("USER_ID_NOT_NULL", userId);
        user = api.getUserWS(userId);
        user.getPaymentInstruments().get(0).setProcessingOrder(0);;
        logger.debug("creditCard :::::::: {}", VALID_CREDIT_CARD_NUMBER_2);
        PaymentInformationWS cc = createCreditCard("test name",
        VALID_CREDIT_CARD_NUMBER_2, expiry.getTime()); 
        user.getPaymentInstruments().add(cc);
        logger.debug("Total numer of cards for this customer  " + user.getUserName() +  "is :"+user.getPaymentInstruments().size());    
    }
*/
    public static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
            String fieldName) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                return ws;
            }
        }
        return null;
    }


    
    @AfterClass
    private void cleanUp() {
        logger.debug("cleaning up WorldPay test");
        if (null != paymentWorldPayExternalPluginId)
            api.deletePlugin(paymentWorldPayExternalPluginId);
        if (null != saveCreditCardPluginId) 
            api.deletePlugin(saveCreditCardPluginId);
    }

}
