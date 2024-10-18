package com.sapienter.jbilling.test;

import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.LoggingValidator;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.ApplyToOrder;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentAuthorizationDTOEx;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentMethodHelper;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.user.CreateResponseWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.ValidatePurchaseWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.PreferenceTypeWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;

/**
 * Created by Wajeeha on 12/20/2016.
 */

@Test(groups = {"improper-access", "web-services"})
public class PaymentInformationBackwardCompatibilityTestCase {

    private static JbillingAPI api;
    private static Integer PRANCING_PONY_ACCOUNT_TYPE;
    private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private final static String CC_HOLDER = "Frodo Baggins";
    private final static String CC_MF_NUMBER = "cc.number";
    private final static String CC_NUMBER = "4111111111111152";
    private final static String LEVEL_DEBUG = "level=\"DEBUG\"";
    private final static String LEVEL_INFO = "level=\"INFO\"";

    private static Integer CC_PAYMENT_TYPE;
    private static Integer ACH_PAYMENT_TYPE;
    private static Integer CURRENCY_USD;
    private static Integer LANGUAGE_ID;
    private static Integer PAYMENT_PERIOD;
    private static Integer ORDER_PERIOD_ONCE;
    private static Integer DYNAMIC_BALANCE_MANAGER_PLUGIN_ID;

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();

        CURRENCY_USD = Constants.PRIMARY_CURRENCY_ID;
        LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
        PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
        PAYMENT_PERIOD = Integer.valueOf(1);
        ORDER_PERIOD_ONCE = Integer.valueOf(1);
        CC_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildCCTemplateMethod(api));
        ACH_PAYMENT_TYPE = api.createPaymentMethodType(PaymentMethodHelper.buildACHTemplateMethod(api));

        DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = getOrCreatePluginWithoutParams(
                "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask", 10008);

    }

    @AfterClass
    protected void tearDown() throws Exception {
        if(null != DYNAMIC_BALANCE_MANAGER_PLUGIN_ID) {
            api.deletePlugin(DYNAMIC_BALANCE_MANAGER_PLUGIN_ID);
        }
    }

    @Test
    public void testProcessPayment() {
        //setup
        UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "4111111111111111");
        user.setId(api.createUser(user));
        UserWS savedUser = api.getUserWS(user.getId());
        PaymentInformationWS instrument = savedUser.getPaymentInstruments().iterator().next();
        AssertJUnit.assertEquals(DataType.CHAR,getMetaField(instrument.getMetaFields(), CommonConstants.METAFIELD_NAME_CC_CARDHOLDER_NAME).getMetaField().getDataType());

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
        item.setId(api.createItem(item));

        OrderWS order = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("10.00"));

        // create the payment
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("5.00"));
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(CURRENCY_USD);
        payment.setUserId(user.getId());

        //  try a credit card number that fails
        // note that creating a payment with a NEW credit card will save it and associate
        // it with the user who made the payment.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5);

        PaymentInformationWS cc = createCreditCard(
                CC_PAYMENT_TYPE, CC_HOLDER, "4111111111111111", cal.getTime());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_CREDIT);
        payment.getPaymentInstruments().add(cc);

        System.out.println("processing payment.");
        PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);

        // check payment failed
        assertNotNull("Payment result not null", authInfo);
        assertFalse("Payment Authorization result should be FAILED", authInfo.getResult().booleanValue());

        //cleanup
        System.out.println("Deleting invoices and orders.");
        api.deleteItem(item.getId());
        api.deleteItemCategory(itemType.getId());
        api.deleteUser(user.getId());
    }

    @Test
    public void testAchFakePayments() {
        UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE);

        //remove payment instruments and add only ACH payment instrument
        user.getPaymentInstruments().clear();
        user.getPaymentInstruments().add(
                createACH(ACH_PAYMENT_TYPE, CC_HOLDER,
                        "Shire Financial Bank", "123456789", "123456789", PRANCING_PONY_ACCOUNT_TYPE));

        System.out.println("Creating user with ACH record and no CC...");
        user.setId(api.createUser(user));

        // get ach
        try (PaymentInformationWS ach = user.getPaymentInstruments().get(0)) {

            System.out.println("Testing ACH payment with even amount (should pass)");
            PaymentWS payment = new PaymentWS();
            payment.setAmount(new BigDecimal("15.00"));
            payment.setIsRefund(new Integer(0));
            payment.setMethodId(Constants.PAYMENT_METHOD_ACH);
            payment.setPaymentDate(Calendar.getInstance().getTime());
            payment.setResultId(Constants.RESULT_ENTERED);
            payment.setCurrencyId(CURRENCY_USD);
            payment.setUserId(user.getId());
            payment.setPaymentNotes("Notes");
            payment.setPaymentPeriod(PAYMENT_PERIOD);
            payment.getPaymentInstruments().add(ach);

            PaymentAuthorizationDTOEx resultOne = api.processPayment(payment, null);
            AssertJUnit.assertEquals("ACH payment with even amount should pass",
                    Constants.RESULT_OK, api.getPayment(resultOne.getPaymentId()).getResultId());

            System.out.println("Testing ACH payment with odd amount (should fail)");
            payment = new PaymentWS();
            payment.setAmount(new BigDecimal("15.01"));
            payment.setIsRefund(new Integer(0));
            payment.setMethodId(Constants.PAYMENT_METHOD_ACH);
            payment.setPaymentDate(Calendar.getInstance().getTime());
            payment.setResultId(Constants.RESULT_ENTERED);
            payment.setCurrencyId(CURRENCY_USD);
            payment.setUserId(user.getId());
            payment.setPaymentNotes("Notes");
            payment.setPaymentPeriod(PAYMENT_PERIOD);
            payment.getPaymentInstruments().add(ach);

            PaymentAuthorizationDTOEx resultTwo = api.processPayment(payment, null);
            AssertJUnit.assertEquals("ACH payment with odd amount should fail",
                    Constants.RESULT_FAIL, api.getPayment(resultTwo.getPaymentId()).getResultId());

            //cleanup

            api.deletePayment(resultTwo.getPaymentId());
            api.deletePayment(resultOne.getPaymentId());
            api.deleteUser(user.getId());
        }catch (Exception exception){
            System.out.println("exception: " + exception);
        }
    }

    @Test
    public void testPaymentSuccessfulEvent() throws Exception
    {

        UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "4111111111111111");
        user.setId(api.createUser(user));

        // create the payment
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("5.00"));
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(Constants.PAYMENT_METHOD_VISA);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(new Integer(1));
        payment.setUserId(user.getId());

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 5);

        PaymentInformationWS cc = createCreditCard(1, "Provisioning test Payment",
                "4111111111111152", cal.getTime());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

        payment.getPaymentInstruments().clear();
        payment.getPaymentInstruments().add(cc);

        System.out.println("processing payment.");
        PaymentAuthorizationDTOEx authInfo = api.processPayment(payment, null);
        // wait for the provisioning to be processed
        pause(9000);

        // check payment successful
        assertNotNull("Payment result not null", authInfo);
        assertNotNull("Auth id not null", authInfo.getId());
        assertTrue("Payment Authorization result should be OK", authInfo.getResult().booleanValue());

        // check payment was made
        PaymentWS lastPayment = api.getLatestPayment(user.getId());
        assertNotNull("payment can not be null", lastPayment);
        assertNotNull("auth in payment can not be null", lastPayment.getAuthorizationId());
        AssertJUnit.assertEquals("payment ids match", lastPayment.getId(), authInfo.getPaymentId().intValue());
        Asserts.assertEquals("correct payment amount", new BigDecimal("5"), lastPayment.getAmountAsDecimal());

        assertTrue("provisioning commands created", lastPayment.getProvisioningCommands().length > 0);

        ProvisioningCommandWS[] commands = lastPayment.getProvisioningCommands();
        AssertJUnit.assertEquals("There is no provisioning command generated for the payment", 1, commands.length);

        // clean up
        api.deletePayment(lastPayment.getId());
    }

    @Test
    public void testCreateUpdateDeleteUser() throws IOException, JbillingAPIException {
        System.out.println("#testCreateUpdateDeleteUser");
        JbillingAPI api = JbillingAPIFactory.getAPI();
        JBillingLogFileReader jbLog = new JBillingLogFileReader();

        String callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
        String apiMethod = "api=\"updateUser\"";

        UserWS parentCreated = null;
        UserWS newUser = null;

        try {

            // check that the validation works
            UserWS badUser = createUser(true, true, null, null, false);
            // create: the user id has to be 0
            badUser.setUserId(99);
            try {
                api.createUser(badUser);
            } catch (SessionInternalError e) {
                AssertJUnit.assertEquals("One error", 1, e.getErrorMessages().length);
                AssertJUnit.assertEquals("Error message",
                        "UserWS,id,validation.error.max,0",
                        e.getErrorMessages()[0]);
            }

            // now add the wrong user name
            badUser.setUserName("");
            try {
                api.createUser(badUser);
            } catch (SessionInternalError e) {
                AssertJUnit.assertEquals("Two errors", 2, e.getErrorMessages().length);
                assertTrue(
                        "Error message",
                        "UserWS,userName,validation.error.size,1,512"
                                .compareTo(e.getErrorMessages()[0]) == 0
                                || "UserWS,userName,validation.error.size,1,512"
                                .compareTo(e.getErrorMessages()[1]) == 0);
            }

            // update user
            badUser.setUserId(0);
            badUser.setUserName("12345"); // bring it back to at least 5 length
            try {
                badUser.setPassword(null);
                api.updateUser(badUser);
            } catch (SessionInternalError e) {
                AssertJUnit.assertEquals("One error", 1, e.getErrorMessages().length);
                AssertJUnit.assertEquals("Error message",
                        "UserWS,id,validation.error.min,1",
                        e.getErrorMessages()[0]);
            }

            // now add the wrong user name
            badUser.setUserName("");
            badUser.setUserId(1); // reset so we can test the name validator
            try {
                badUser.setPassword(null);
                api.updateUser(badUser);
            } catch (SessionInternalError e) {
                AssertJUnit.assertEquals("Two errors", 1, e.getErrorMessages().length);
                assertTrue("Error message",
                        "UserWS,userName,validation.error.size,1,512".equals(e
                                .getErrorMessages()[0]));
            }

            System.out.println("Validation tested");

            // Create - This passes the password validation routine.
            parentCreated = createParent(api);

            newUser = createUser(true,parentCreated.getId(), null);
            Integer newUserId = newUser.getUserId();
            String newUserName = newUser.getUserName();
            assertNotNull("The user was not created", newUserId);

            System.out.println("Getting the id of the new user: " + newUserName);
            Integer ret = api.getUserId(newUserName);
            AssertJUnit.assertEquals("Id of new user found", newUserId, ret);

            // verify the created user
            System.out.println("Getting created user " + newUserId);
            UserWS retUser = api.getUserWS(newUserId);
            PaymentInformationWS instrument = retUser.getPaymentInstruments().iterator().next();

            AssertJUnit.assertEquals("created username", retUser.getUserName(),
                    newUser.getUserName());
            AssertJUnit.assertEquals("create user parent id", new Integer(parentCreated.getId()),
                    retUser.getParentId());
            System.out.println("My user: " + retUser);

            AssertJUnit.assertEquals("created credit card name",
                    "Frodo Baggins", new String(getMetaField(instrument.getMetaFields(), CommonConstants.METAFIELD_NAME_CC_CARDHOLDER_NAME).getCharValue()));

            //  Make a create mega call

            System.out.println("Making mega call");
            retUser.setUserName("MU"
                    + Long.toHexString(System.currentTimeMillis()));

            retUser.setParentId(null);
            OrderWS newOrder = getOrder();

            Calendar activeSinceDate = Calendar.getInstance();
            activeSinceDate.setTime(new Date());
            activeSinceDate.add(Calendar.MONTH, -1);
            newOrder.setActiveSince(activeSinceDate.getTime());
            
            retUser.setUserId(0);
            retUser.setPassword("P@ssword1");
            OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(newOrder,
                    ORDER_CHANGE_STATUS_APPLY_ID);
            
            for(OrderChangeWS change : changes) {
                change.setStartDate(newOrder.getActiveSince());
                change.setApplicationDate(newOrder.getActiveSince());
            }
            
            CreateResponseWS mcRet = api.create(retUser, newOrder, changes);

            System.out.println("Validating new invoice");
            // validate that the results are reasonable
            assertNotNull("Mega call result can't be null", mcRet);
            assertNotNull("Mega call invoice result can't be null",
                    mcRet.getInvoiceId());
            // there should be a successfull payment
            AssertJUnit.assertEquals("Payment result OK", true, mcRet.getPaymentResult()
                    .getResult().booleanValue());
            AssertJUnit.assertEquals("Processor code", "fake-code-default", mcRet
                    .getPaymentResult().getCode1());
            // get the invoice
            InvoiceWS retInvoice = api.getInvoiceWS(mcRet.getInvoiceId());
            assertNotNull("New invoice not present", retInvoice);
            assertEquals("Balance of invoice should be zero, is paid",
                    new BigDecimal("0.00"), retInvoice.getBalanceAsDecimal());
            assertEquals("Total of invoice should be total of order",
                    new BigDecimal("20.00"), retInvoice.getTotalAsDecimal());
            AssertJUnit.assertEquals("New invoice paid", retInvoice.getToProcess(),
                    new Integer(0));

            // now update the created user
            System.out.println("Updating user - Pass 1 - Should succeed");
            retUser = api.getUserWS(newUserId);
            retUser.setCreditLimit(new BigDecimal("112233.0"));
            System.out.println("Updating user...");
            updateMetaField(retUser.getPaymentInstruments().iterator().next()
                    .getMetaFields(), CC_MF_NUMBER, "4111111111111152");
            retUser.setPassword(null);

            jbLog.setWatchPoint();
            api.updateUser(retUser);
            String msg = "User updated successfully: " + retUser.getId();
            String fullLog = jbLog.readLogAsString();
            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_DEBUG, callerClass, apiMethod, LogConstants.MODULE_USER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE, msg);

            System.out.println("Getting updated user ");
            retUser = api.getUserWS(newUserId);
            assertNotNull("Didn't get updated user", retUser);

            AssertJUnit.assertEquals(
                    "Credit card updated",
                    "4111111111111152",
                    new String(getMetaField(
                            retUser.getPaymentInstruments().iterator().next()
                                    .getMetaFields(), CC_MF_NUMBER)
                            .getCharValue()));
            assertEquals("credit limit updated", new BigDecimal("112233.00"),
                    retUser.getCreditLimitAsDecimal());


            retUser.setPassword(null); // should not change the password
            api.updateUser(retUser);
            // fetch the user
            UserWS updatedUser = api.getUserWS(newUserId);

            System.out.println("Update result:" + updatedUser);

            // update credit card details
            System.out.println("Removing first payment method");

            // remove payment information
            api.removePaymentInstrument(updatedUser.getPaymentInstruments()
                    .iterator().next().getId());

            // get updated user with removed payment instrument
            updatedUser = api.getUserWS(newUserId);
            AssertJUnit.assertEquals("Credit card removed", (int) new Integer(0), (int) updatedUser.getPaymentInstruments().size());

            System.out.println("Creating credit card");
            String ccName = "New ccName";
            String ccNumber = "4012888888881881";
            Date ccExpiry = Util.truncateDate(Calendar.getInstance().getTime());

            PaymentInformationWS newCC = createCreditCard(CC_PAYMENT_TYPE, ccName, ccNumber,
                    ccExpiry);
            updatedUser.getPaymentInstruments().add(newCC);

            updatedUser.setPassword(null);
            api.updateUser(updatedUser);

            // check updated cc details
            retUser = api.getUserWS(newUserId);
            PaymentInformationWS retCc = retUser.getPaymentInstruments()
                    .iterator().next();
            AssertJUnit.assertEquals("new cc name", ccName,
                    new String(getMetaField(retCc.getMetaFields(), CommonConstants.METAFIELD_NAME_CC_CARDHOLDER_NAME)
                            .getCharValue()));
            AssertJUnit.assertEquals("updated cc number", ccNumber,
                    new String(getMetaField(retCc.getMetaFields(), CC_MF_NUMBER)
                            .getCharValue()));
            AssertJUnit.assertEquals("updated cc expiry", DateTimeFormat.forPattern(
                            Constants.CC_DATE_FORMAT).print(ccExpiry.getTime()),
                    new String(getMetaField(retCc.getMetaFields(), CommonConstants.METAFIELD_NAME_CC_EXPIRY_DATE)
                            .getCharValue()));

            // set the credit card ID so that we update the existing card with
            // the API call
            newCC.setId(retCc.getId());

            // now delete this new guy
            System.out.println("Deleting user..." + newUserId);

            jbLog.setWatchPoint();
            api.deleteUser(newUserId);
            callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
            apiMethod = "api=\"deleteUser\"";
            msg = "User with ID: " + newUserId + " has been deleted.";
            fullLog = jbLog.readLogAsString();

            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_USER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE, msg);

            // try to fetch the deleted user
            System.out.println("Getting deleted user " + newUserId);
            updatedUser = api.getUserWS(newUserId);
            AssertJUnit.assertEquals(updatedUser.getDeleted(), 1);

            // verify I can't delete users from another company
            try {
                System.out.println("Deleting user base user ... 13");
                api.getUserWS(new Integer(13));
                fail("Shouldn't be able to access user 13");
            } catch (Exception e) {
            }

            System.out.println("Done");

        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        } finally {
            if (newUser != null) api.deleteUser(newUser.getId());
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
        }
    }

    @Test
    public void testPurchaseAuthorization(){
        System.out.println("#testPurchaseAuthorization");
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            UserWS myUser = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
            Integer myId = myUser.getUserId();

            // update credit limit
            myUser.setCreditLimit(new BigDecimal("10"));
            myUser.setPassword(null);
            api.updateUser(myUser);
            PaymentWS payment = new PaymentWS();
            payment.setUserId(myId);
            payment.setMethodId(1);
            Calendar calendar = Calendar.getInstance();
            payment.setPaymentDate(calendar.getTime());
            payment.setCurrencyId(1);
            payment.setAmount("10");
            payment.setIsRefund(0);

            // check that user's dynamic balance is 20
            UserWS user =  api.getUserWS(myId);
            System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
            PaymentInformationWS cc = createCreditCard(CC_PAYMENT_TYPE,"Frodo Baggins", "4111111111111111", new Date());
            cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
            payment.getPaymentInstruments().add(cc);

            api.createPayment(payment);

            // update the user
            user.setPassword(null);
            api.updateUser(user);
            user = api.getUserWS(myId);
            System.out.println("User's dynamic balance earlier was "+user.getDynamicBalanceAsDecimal());
            ItemDTOEx newItem = new ItemDTOEx();
            newItem.setDescription("TEST Item");
            newItem.setPrice(new BigDecimal("20"));
            newItem.setNumber("WS-001");
            Integer types[] = new Integer[1];
            types[0] = new Integer(1);
            newItem.setTypes(types);
            System.out.println("Creating item ..." + newItem);
            Integer itemId = api.createItem(newItem);

            //credit_limit(10)+balance(10)=order total(20)
            ValidatePurchaseWS result=api.validatePurchase(myId,itemId,null);
            AssertJUnit.assertEquals("validate purchase success 1", Boolean.valueOf(true), result.getSuccess());
            AssertJUnit.assertEquals("validate purchase authorized 1", Boolean.valueOf(true), result.getAuthorized());
            assertEquals("validate purchase quantity 1", new BigDecimal("1.00"), result.getQuantityAsDecimal());

            // now create a one time order, the balance should decrease this one time order is of order total 20
            OrderWS order = getOrder();
            order.setUserId(myId);
            System.out.println("creating one time order");
            Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, 3));
            System.out.println("Validating new balance");
            myUser = api.getUserWS(myId);
            assertEquals("user should have -ve 10 balance", new BigDecimal("10.0").negate(), myUser.getDynamicBalanceAsDecimal());

            //now balance(-10)+credit_limit(10) and order total -10 so the result should fail
            result=api.validatePurchase(myId,itemId,null);
            AssertJUnit.assertEquals("validate purchase success 2", Boolean.valueOf(true), result.getSuccess());
            AssertJUnit.assertEquals("validate purchase authorized 2", Boolean.valueOf(false), result.getAuthorized());
            assertEquals("validate purchase quantity 1", new BigDecimal("0.00"), result.getQuantityAsDecimal());

            api.deleteUser(myId);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        }
    }

    @Test
    public void testProcessSignupPaymentAPIWithHugeAmount() throws Exception {

        Integer userId =null;
        System.out.println("processSignupPayment API With Huge Payment Amount....");
        UserWS user = createUser(true,null,null);

        PaymentWS payment = createPayment(user.getId(), new BigDecimal("9999999999"), user.getPaymentInstruments().get(0));
        System.out.println("Processing token payment...");
        try {
            userId = api.processSignupPayment(user, payment);
        } catch (SessionInternalError e) {
            assertNotNull(e);
        }
        assertNull("UserId should be null as user would not get created", userId);
    }

    @Test
    public void testSaveLegacyPayment() {
        //setup
        UserWS user = buildUser(1, "Frodo", "Baggins", CC_NUMBER);
        user.setId(api.createUser(user));

        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal("15.00"));
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(Constants.PAYMENT_METHOD_CREDIT);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(CURRENCY_USD);
        payment.setUserId(user.getId());

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		// add credit card
		payment.getPaymentInstruments().add(PaymentMethodHelper
				.createCreditCard(CC_PAYMENT_TYPE, CC_HOLDER, CC_NUMBER, expiry.getTime()));

        Integer paymentId = api.saveLegacyPayment(payment);
        assertNotNull("Payment should be saved", paymentId);

        PaymentWS retPayment = api.getPayment(paymentId);
        assertNotNull(retPayment);
        assertEquals(retPayment.getAmountAsDecimal(), payment.getAmountAsDecimal());
        AssertJUnit.assertEquals(retPayment.getIsRefund(), payment.getIsRefund());
        AssertJUnit.assertEquals(retPayment.getMethodId(), payment.getMethodId());
        AssertJUnit.assertEquals(retPayment.getResultId(), payment.getResultId());
        AssertJUnit.assertEquals(retPayment.getCurrencyId(), payment.getCurrencyId());
        AssertJUnit.assertEquals(retPayment.getUserId(), payment.getUserId());
        AssertJUnit.assertEquals(retPayment.getPaymentNotes(), "This payment is migrated from legacy system.");
        AssertJUnit.assertEquals(retPayment.getPaymentPeriod(), payment.getPaymentPeriod());

        //cleanup
        api.deletePayment(retPayment.getId());
        api.deleteUser(user.getId());
    }

    @Test
    public void testSuccessfulPaymentWithEnteredPayment() throws Exception {

        resetBillingConfiguration();

        BillingProcessConfigurationWS billingProcessConfiguration = api.getBillingProcessConfiguration();

        final Date activeSince = new DateTime(billingProcessConfiguration.getNextRunDate()).toDate();

        UserWS user = buildUser(PRANCING_PONY_ACCOUNT_TYPE, "4111111111111111");
        Integer customerId = api.createUser(user);
        user.setId(customerId);
        updateNextInvoiceDate(customerId);
        System.out.println("user id = " +user.getId() +" | user.getUserId = " +user.getUserId() +" | customer id = " + customerId);
        //int customerId = user.getCustomerId();

        ItemTypeWS itemType = buildItemType();
        itemType.setId(api.createItemCategory(itemType));

        ItemDTOEx item = buildItem(itemType.getId(), api.getCallerCompanyId());
        item.setId(api.createItem(item));

        // first, create two unpaid invoices
        Integer orderChangeApplyStatus = getOrCreateOrderChangeApplyStatus(api);
        OrderWS newOrder = buildOrder(user.getId(), Arrays.asList(item.getId()), new BigDecimal("10.00"));
        Integer orderId = api.createOrder(newOrder, OrderChangeBL.buildFromOrder(newOrder, orderChangeApplyStatus));

        triggerBillingProcess(activeSince, api);

        Integer[] invoices = api.createInvoice(customerId, false);

        InvoiceWS invoiceWS = api.getLatestInvoice(customerId);
        System.out.println("invoice ws = " +invoiceWS);

        Integer invoiceId = invoiceWS.getId();

        PaymentWS paymentWS = new PaymentWS();
        paymentWS.setUserId(customerId);
        paymentWS.setAmount(new BigDecimal("0.25"));
        paymentWS.setIsRefund(Integer.valueOf(0));
        paymentWS.setMethodId(Constants.PAYMENT_METHOD_VISA);
        paymentWS.setCurrencyId(api.getCallerCurrencyId());
        paymentWS.setPaymentInstruments(api.getUserWS(customerId).getPaymentInstruments());
        paymentWS.setPaymentDate(new Date());
        api.applyPayment(paymentWS, invoiceId);

        PaymentWS paymentWS1 = null;
        PaymentWS paymentWS2 = null;
        invoiceId = null;
        try {
            invoiceWS = api.getLatestInvoice(customerId);
            invoiceId = invoiceWS.getId();

            paymentWS1 = api.getLatestPayment(customerId);
            org.testng.Assert.assertNotNull(paymentWS1);
            org.testng.Assert.assertEquals(paymentWS1.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("0.25").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Amount");
            org.testng.Assert.assertEquals(paymentWS1.getResultId(), Constants.PAYMENT_RESULT_ENTERED);

            api.triggerAgeing(new org.joda.time.LocalDate(activeSince).plusDays(1).toDate());
            api.triggerAgeing(new org.joda.time.LocalDate(activeSince).plusDays(3).toDate());
            api.triggerAgeing(new org.joda.time.LocalDate(activeSince).plusDays(4).toDate());

            pause(2000);
            paymentWS2 = api.getLatestPayment(customerId);
            org.testng.Assert.assertNotNull(paymentWS2);
            org.testng.Assert.assertEquals(paymentWS2.getAmountAsDecimal().setScale(2, BigDecimal.ROUND_CEILING),
                    new BigDecimal("0.25").setScale(2, BigDecimal.ROUND_CEILING), "Invalid Amount");
            org.testng.Assert.assertEquals(paymentWS2.getResultId(), Integer.valueOf(4));
        } finally {

            if (paymentWS1 != null) {
                api.deletePayment(paymentWS1.getId());
            }

            if (paymentWS2 != null) {
                api.deletePayment(paymentWS2.getId());
            }

            if (invoiceId != null) {
                api.deleteInvoice(invoiceId);
            }
            updateCustomerStatusToActive(customerId, api);
            resetBillingConfiguration();
        }
    }

    @Test
    public void testDeletePaymentThatHasRefund() throws Exception{
        System.out.println("testDeletePaymentThatHasRefund()");

        //create user
        UserWS user = createUser(true,null,null);
        assertTrue(user.getUserId() > 0);
        System.out.println("User created successfully " + user.getUserId());

        PaymentInformationWS cc = createCreditCard(CC_PAYMENT_TYPE, "Frodo Baggins", "4111111111111111", new Date());
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

        //make payment
        Integer paymentId= createPayment(api, cc.getPaymentMethodId(), "100.00", false, user.getUserId(), null, cc);
        System.out.println("Created payment " + paymentId);
        assertNotNull("Didn't get the payment id", paymentId);

        //check payment balance = payment amount
        PaymentWS payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(payment.getAmountAsDecimal(), payment.getBalanceAsDecimal());

        assertTrue(payment.getInvoiceIds().length == 0);

        //create refund for above payment, refund amount = payment amount
        Integer refundId= createPayment(api, cc.getPaymentMethodId(), "100.00", true, user.getUserId(), paymentId, cc);
        System.out.println("Created refund " + refundId);
        assertNotNull("Didn't get the payment id", refundId);

        //check payment balance = 0
        payment = api.getPayment(paymentId);
        assertNotNull(payment);
        assertEquals(BigDecimal.ZERO, payment.getBalanceAsDecimal());

        try {
            api.deletePayment(paymentId);
            fail("A refund can not be deleted");
        } catch (Exception e) {
            //expected
        }

        //cleanup
        api.deleteUser(user.getId());
    }

    private UserWS buildUser(Integer accountType) {
        return buildUser(accountType, "Frodo", "Baggins", CC_NUMBER);
    }

    private UserWS buildUser(Integer accountType, String ccNumber) {
        return buildUser(accountType, "Frodo", "Baggins", ccNumber);
    }

    private UserWS buildUser(Integer accountTypeId, String firstName, String lastName, String ccNumber) {
        UserWS newUser = new UserWS();
        newUser.setUserName("payment-test-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_ID);
        newUser.setMainRoleId(new Integer(5));
        newUser.setAccountTypeId(accountTypeId);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(CURRENCY_USD);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(newUser.getUserName() + "@test.com");
        metaField1.setGroupId(accountTypeId);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue(firstName);
        metaField2.setGroupId(accountTypeId);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue(lastName);
        metaField3.setGroupId(accountTypeId);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3
        });

        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        // add credit card
        newUser.getPaymentInstruments().add(createCreditCard(CC_PAYMENT_TYPE, CC_HOLDER, ccNumber, expiry.getTime()));

        return newUser;
    }

    public static PaymentInformationWS createCreditCard(Integer methodId, String cardHolderName, String cardNumber, Date date) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(methodId);
        cc.setProcessingOrder(Integer.valueOf(1));
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_CC_CARDHOLDER_NAME, false, true, DataType.STRING, 1, cardHolderName);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_CC_NUMBER, false, true, DataType.STRING, 2, cardNumber);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_CC_EXPIRY_DATE, false, true, DataType.STRING, 3,
                (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(date.getTime())));
        // have to pass meta field card type for it to be set
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_CC_TYPE, true, false, DataType.STRING, 4, CreditCardType.VISA);
        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
    }

    public static PaymentInformationWS createACH(Integer methodId, String customerName, String bankName,
                                                 String routingNumber, String accountNumber, Integer accountType) {
        PaymentInformationWS cc = new PaymentInformationWS();
        cc.setPaymentMethodTypeId(methodId);
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_ACH);
        cc.setProcessingOrder(new Integer(2));

        List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_ACH_ROUTING_NUMBER, false, true,
                DataType.STRING, 1, routingNumber);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_ACH_CUSTOMER_NAME, false, true,
                DataType.STRING, 2, customerName);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_ACH_ACCOUNT_NUMBER, false, true,
                DataType.STRING, 3, accountNumber);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_ACH_BANK_NAME, false, true,
                DataType.STRING, 4, bankName);
        addMetaField(metaFields, CommonConstants.METAFIELD_NAME_ACH_ACCOUNT_TYPE, false, true,
                DataType.ENUMERATION, 5, accountType == 1 ? Constants.ACH_CHECKING : Constants.ACH_SAVING);

        cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

        return cc;
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


    private ItemTypeWS buildItemType() {
        ItemTypeWS type = new ItemTypeWS();
        type.setDescription("Invoice, Item Type:" + System.currentTimeMillis());
        type.setOrderLineTypeId(1);//items
        type.setAllowAssetManagement(0);//does not manage assets
        type.setOnePerCustomer(false);
        type.setOnePerOrder(false);
        return type;
    }

    private ItemDTOEx buildItem(Integer itemTypeId, Integer priceModelCompanyId) {
        ItemDTOEx item = new ItemDTOEx();
        long millis = System.currentTimeMillis();
        String name = String.valueOf(millis) + new Random().nextInt(10000);
        item.setDescription("Payment, Product:" + name);
        item.setPriceModelCompanyId(priceModelCompanyId);
        item.setPrice(new BigDecimal("10"));
        item.setNumber("PYM-PROD-" + name);
        item.setAssetManagementEnabled(0);
        Integer typeIds[] = new Integer[]{itemTypeId};
        item.setTypes(typeIds);
        return item;
    }

    private OrderWS buildOrder(int userId, List<Integer> itemIds, BigDecimal linePrice) {
        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ORDER_PERIOD_ONCE); // once
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(new Date());
        order.setProrateFlag(Boolean.FALSE);

        ArrayList<OrderLineWS> lines = new ArrayList<OrderLineWS>(itemIds.size());
        for (int i = 0; i < itemIds.size(); i++) {
            OrderLineWS nextLine = new OrderLineWS();
            nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
            nextLine.setDescription("Order line: " + i);
            nextLine.setItemId(itemIds.get(i));
            nextLine.setQuantity(1);
            nextLine.setPrice(linePrice);
            nextLine.setAmount(nextLine.getQuantityAsDecimal().multiply(linePrice));

            lines.add(nextLine);
        }
        order.setOrderLines(lines.toArray(new OrderLineWS[lines.size()]));
        return order;
    }

    private void pause(long t) {
        System.out.println("pausing for " + t);
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private  UserWS createUser(boolean setPassword, boolean goodCC, Integer parentId,
                               Integer currencyId, boolean doCreate) throws JbillingAPIException,
            IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();


        // Create - This passes the password validation routine.

        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-"
                + Calendar.getInstance().getTimeInMillis());
        if (setPassword) {
            newUser.setPassword("P@ssword1");
        }
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(false);

        if (parentId != null) {
            UserWS parent = api.getUserWS(parentId);
            MainSubscriptionWS parentSubscription = parent.getMainSubscription();
            newUser.setMainSubscription(
                    new MainSubscriptionWS(parentSubscription.getPeriodId(), parentSubscription.getNextInvoiceDayOfPeriod()));
            newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
        }

        System.out.println("User properties set");
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[] { metaField1, metaField2,
                metaField3, metaField4, metaField5 });

        System.out.println("Meta field values set");

        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        PaymentInformationWS cc = createCreditCard(CC_PAYMENT_TYPE, "Frodo Baggins",
                goodCC ? "4111111111111152" : "4111111111111111",
                expiry.getTime());

        newUser.getPaymentInstruments().add(cc);

        if (doCreate) {
            System.out.println("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
            if (parentId != null) {
                UserWS parent = api.getUserWS(parentId);
                newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
                api.updateUser(newUser);
                newUser = api.getUserWS(newUser.getId());
            }
            newUser.setPassword(null);

        }
        System.out.println("User created with id:" + newUser.getUserId());
        return newUser;
    }

    private UserWS createParent(JbillingAPI api) throws JbillingAPIException, IOException {
        UserWS parentCreated = createUser(true, null, null);
        parentCreated.setIsParent(true);
        parentCreated.setPassword(null);
        api.updateUser(parentCreated);
        assertNotNull("The parent user was not created", parentCreated);
        return parentCreated;
    }


    private  UserWS createUser(boolean goodCC, Integer parentId,
                                    Integer currencyId) throws JbillingAPIException, IOException {
        return createUser(true, goodCC, parentId, currencyId, true);
    }

    private  MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
                                                 String fieldName) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                return ws;
            }
        }
        return null;
    }

    private  OrderWS getOrder() {
        // need an order for it
        OrderWS newOrder = new OrderWS();
        newOrder.setUserId(new Integer(-1)); // it does not matter, the user
        // will be created
        newOrder.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        newOrder.setPeriod(new Integer(1)); // once
        newOrder.setCurrencyId(new Integer(1));
        newOrder.setActiveSince(new Date());

        // now add some lines
        OrderLineWS lines[] = new OrderLineWS[2];
        OrderLineWS line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("First line");
        line.setItemId(new Integer(1));
        lines[0] = line;

        line = new OrderLineWS();
        line.setPrice(new BigDecimal("10.00"));
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setQuantity(new Integer(1));
        line.setAmount(new BigDecimal("10.00"));
        line.setDescription("Second line");
        line.setItemId(new Integer(3));
        lines[1] = line;

        newOrder.setOrderLines(lines);

        return newOrder;
    }

    private  void updateMetaField(MetaFieldValueWS[] metaFields,
                                       String fieldName, Object value) {
        for (MetaFieldValueWS ws : metaFields) {
            if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
                ws.getMetaField().setDataType(DataType.STRING);
                ws.setValue(value);
            }
        }
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

    public static Integer getOrCreateOrderChangeApplyStatus(JbillingAPI api){
        OrderChangeStatusWS[] list = api.getOrderChangeStatusesForCompany();
        Integer statusId = null;
        for(OrderChangeStatusWS orderChangeStatus : list){
            if(orderChangeStatus.getApplyToOrder().equals(ApplyToOrder.YES)){
                statusId = orderChangeStatus.getId();
                break;
            }
        }
        if(statusId != null){
            return statusId;
        }else{
            OrderChangeStatusWS newStatus = new OrderChangeStatusWS();
            newStatus.setApplyToOrder(ApplyToOrder.YES);
            newStatus.setDeleted(0);
            newStatus.setOrder(1);
            newStatus.addDescription(new InternationalDescriptionWS(com.sapienter.jbilling.server.util.Constants.LANGUAGE_ENGLISH_ID, "status1"));
            return api.createOrderChangeStatus(newStatus);
        }
    }

    private void wait(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            System.out.println("Errors while waiting for operations to complete");
        }
    }

    private void triggerBillingProcess(Date runDate, JbillingAPI api) {
        api.triggerBilling(runDate);
        while (api.isBillingRunning(api.getCallerCompanyId())) {
            wait(2000);
        }
    }

    /**
     * Resets the billing configuration to the default state found in a fresh
     * load of the testing 'jbilling_test.sql' file.
     *
     * @throws Exception possible api exception
     */
    private void resetBillingConfiguration() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setNextRunDate(new DateMidnight(2006, 10, 26).toDate());
        config.setGenerateReport(1);
        config.setDaysForReport(3);
        config.setRetries(0);
        config.setDaysForRetry(1);
        config.setDueDateValue(1);
        config.setDueDateUnitId(PeriodUnitDTO.MONTH);

        config.setOnlyRecurring(1);
        config.setInvoiceDateProcess(0);
        config.setMaximumPeriods(1);

        api.createUpdateBillingProcessConfiguration(config);

        // reset continuous invoice date
        PreferenceWS continuousDate = new PreferenceWS(new PreferenceTypeWS(Constants.PREFERENCE_CONTINUOUS_DATE), null);
        api.updatePreference(continuousDate);

    }

    private void updateCustomerStatusToActive(Integer customerId, JbillingAPI api){

        UserWS user = api.getUserWS(customerId);
        user.setStatusId(Integer.valueOf(1));
        user.setStatus("Active");
        api.updateUser(user);
    }

    //Helper method to create payment
    private Integer createPayment(JbillingAPI api, Integer paymentMethodId, String amount, boolean isRefund,
                                  Integer userId, Integer linkedPaymentId, PaymentInformationWS paymentInformationWS) {
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal(amount));
        payment.setIsRefund(isRefund ? new Integer(1) : new Integer(0));
        payment.setMethodId(paymentMethodId);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setResultId(Constants.RESULT_ENTERED);
         payment.setCurrencyId(CURRENCY_USD);
        payment.setUserId(userId);
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));
        payment.setPaymentId(linkedPaymentId);

        payment.getPaymentInstruments().add(paymentInformationWS);

        System.out.println("Creating " + (isRefund ? " refund." : " payment."));
        return api.createPayment(payment);
    }
    
    private void updateNextInvoiceDate(Integer userId) {
        UserWS user = api.getUserWS(userId);
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
    }

    private Integer getOrCreatePluginWithoutParams(String className, int processingOrder) {
        PluggableTaskWS[] taskWSs = api.getPluginsWS(api.getCallerCompanyId(), className);
        if(taskWSs.length != 0){
            return taskWSs[0].getId();
        }
        PluggableTaskWS pluggableTaskWS = new PluggableTaskWS();
        pluggableTaskWS.setTypeId(api.getPluginTypeWSByClassName(className).getId());
        pluggableTaskWS.setProcessingOrder(processingOrder);
        pluggableTaskWS.setOwningEntityId(api.getCallerCompanyId());
        return api.createPlugin(pluggableTaskWS);
    }
}
