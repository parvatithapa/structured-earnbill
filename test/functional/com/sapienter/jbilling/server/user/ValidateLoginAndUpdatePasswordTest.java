package com.sapienter.jbilling.server.user;


import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;


import java.lang.invoke.MethodHandles;


import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;


/**
 * @author Sumit Bhatt
 */

@Test(groups = { "web-services", "user" }, testName = "user.ValidateLoginAndUpdatePasswordTest")
public class ValidateLoginAndUpdatePasswordTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private TestBuilder testBuilder;
    private EnvironmentHelper environmentHelper;
    private Integer ACCOUNT_ID;
    private String USER_NAME;
    private Integer USER_ID;
    private static final String ACCOUNT_TYPE_CODE = "TestAccountType";
    private static final String CUSTOMER_CODE = "TestCustomer";
    private static final String PASSWORD = "$2a$10$hsrtKgAtA/ATECxW3A5rIOOr0EpCG6dWSDDXe/P/mbYX/OaR1O4Re";
    private static final String currentPassword = "Abcd@123456";
    private static final String newPassword = "Admin123@";
    

    @BeforeClass
    protected void setUp() throws Exception {
        testBuilder = getTestEnvironment();
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            ACCOUNT_ID = envBuilder.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();
        });
    }
   
    @AfterClass
    protected void tearDown() throws Exception {
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        if (null != environmentHelper){
            environmentHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
    }
    
    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(envCreator -> {
            environmentHelper = EnvironmentHelper.getInstance(envCreator.getPrancingPonyApi());
        });
    }

    @Test
    public void test001validateLoginValidCredentials() throws Exception {
         testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                UserWS createdUser = api.getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
                UserWS apiResponseUser = api.validateLogin(USER_NAME, "Abcd@123456");
                logger.info("UserID Fetched from validateLogin API {}", apiResponseUser.getId());
                assertEquals(apiResponseUser.getId(), createdUser.getId());
                assertEquals(apiResponseUser.getUserName(), createdUser.getUserName());
             });
    }
    
    @Test
    public void test002validateLoginInvalidUserName(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
               try{
                   UserWS apiResponseUser = api.validateLogin("", "Abcd@123456");
                   }catch(Exception e){
                   logger.debug("validateLogin returns an error in response");
                   assertTrue("Invalid error message!!", e.getMessage().contains("Please enter userName"));
               }
             });
    }
    @Test
    public void test003validateLoginInvalidPassword(){
          testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
               try{
                   UserWS apiResponseUser = api.validateLogin(USER_NAME, "");
                   }catch(Exception e){
                   logger.debug("validateLogin returns an error in response");
                   assertTrue("Invalid error message!!", e.getMessage().contains("Please enter password"));
               }
             });
    }
    @Test
    public void test004validateLoginNoUser(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                try{
                   UserWS apiResponseUser = api.validateLogin("abcdefgh", "Password@123");
                   }catch(Exception e){
                   logger.debug("validateLogin returns an error in response");
                   assertTrue("Invalid error message!!", e.getMessage().contains("Please enter a valid user name"));
               }
             });
    }
    @Test
    public void test005validateLoginInvalidCredentials(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
               try{
                   UserWS apiResponseUser = api.validateLogin(USER_NAME, "1234qwe");
                   }catch(Exception e){
                   logger.debug("validateLogin returns an error in response");
                   assertTrue("Invalid error message!!", e.getMessage().contains("The entered current password does not match the stored password"));
               }
             });
    }
    @Test
    public void test006updatePasswordValidTest(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                UserWS createdUser = api.getUserWS(USER_ID);
                try{
                    api.updatePassword(USER_ID, currentPassword, newPassword);
                   }catch(Exception ex){
                    logger.debug("Error while calling updatePassword "+ex.getMessage());
                   }
                try{
                    UserWS apiResponseUser = api.validateLogin(USER_NAME, newPassword);
                    logger.info("UserID Fetched from validateLogin API with updated Password {}", apiResponseUser.getId());
                    assertNotNull("UserWS should not be null",createdUser);
                    assertNotEquals(createdUser.getPassword(), apiResponseUser.getPassword());
                    assertEquals(apiResponseUser.getId(), createdUser.getId());
                    assertEquals(apiResponseUser.getUserName(), createdUser.getUserName());
                }catch(Exception ex){
                    logger.debug("Error while validating Login using validateLogin api " +ex.getMessage());
                }
                
             });
   }
    @Test
    public void test007updatePasswordInvalidUser(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                try{
                    api.updatePassword(null, currentPassword, newPassword);
                   }catch(Exception ex){
                    logger.debug("Error while calling updatePassword");
                    assertTrue("Invalid error message!!", ex.getMessage().contains("Invalid user Id"));
                   }
                });
    }
    @Test
    public void test008updatePasswordInvalidPass(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                try{
                    api.updatePassword(USER_ID, currentPassword, "");
                   }catch(Exception ex){
                    logger.debug("Error while calling updatePassword");
                    assertTrue("Invalid error message!!", ex.getMessage().contains("Invalid password"));
                   }
                });
    }
    @Test
    public void test009updatePasswordSimilarPass(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                try{
                    api.updatePassword(USER_ID, currentPassword, newPassword);
                   }catch(Exception ex){
                    logger.debug("Error while calling updatePassword");
                   }
                //again updating password to the last one 
                try{
                 api.updatePassword(USER_ID, newPassword, currentPassword);
                }catch(Exception ex){
                    logger.debug("Error while calling updatePassword");
                    assertTrue("Invalid error message!!", ex.getMessage().contains("password is similar to one of the last six passwords. Please enter a unique Password"));
                }
                });
    }
    @Test
    public void test010upatePasswordCriteira(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                try{
                    api.updatePassword(USER_ID, currentPassword, "TestPass");
                   }catch(Exception ex){
                    logger.debug("Error while calling updatePassword");
                    assertTrue("Invalid Error Message !", ex.getMessage().contains("password must contain at least one upper case, one lower case, one digit, one special character. The password should be between 8 and 40 characters long"));
                   }
               });
    }
    @Test
    public void test011updatePasswordInvalidCurrentPass(){
        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            UserWS user = envBuilder.customerBuilder(api)
                    .withAccountTypeId(ACCOUNT_ID)
                    .withUsername(CUSTOMER_CODE)
                    .withPassword(PASSWORD)
                    .build();
             USER_NAME = user.getUserName();
             USER_ID = user.getId();
             logger.info("User Successfully Created with username {} and UserID {}", USER_NAME,USER_ID);
            }).validate((testEnv, testEnvBuilder)->{
                UserWS createdUser = testEnv.getPrancingPonyApi().getUserWS(USER_ID);
                assertNotNull("UserWS should not be null",createdUser);
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                try{
                    api.updatePassword(USER_ID, newPassword, currentPassword);
                   }catch(Exception ex){
                    logger.debug("Error while calling updatePassword");
                    assertTrue("Invalid Error Message !", ex.getMessage().contains("Invalid credential - the current password does not match"));
                   }
               });
    }
}   
  
