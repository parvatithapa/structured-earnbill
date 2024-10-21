/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2012] Enterprise jBilling Software Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Enterprise jBilling Software.
 * The intellectual and technical concepts contained
 * herein are proprietary to Enterprise jBilling Software
 * and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

/*
 * Created on Dec 18, 2003
 *
 */
package com.sapienter.jbilling.server.user;

import static com.sapienter.jbilling.server.metafields.EntityType.ACCOUNT_TYPE;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.ADDRESS1;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.ADDRESS2;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.CITY;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.COUNTRY_CODE;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.EMAIL;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.FIRST_NAME;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.INITIAL;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.LAST_NAME;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.ORGANIZATION;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.PHONE_NUMBER;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.POSTAL_CODE;
import static com.sapienter.jbilling.server.metafields.MetaFieldType.STATE_PROVINCE;
import static com.sapienter.jbilling.test.Asserts.assertEquals;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.AfterClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.LoggingValidator;
import com.sapienter.jbilling.server.accountType.builder.AccountInformationTypeBuilder;
import com.sapienter.jbilling.server.accountType.builder.AccountTypeBuilder;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.PreferenceWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.audit.logConstants.LogConstants;
import com.sapienter.jbilling.server.util.time.DateConvertUtils;
import com.sapienter.jbilling.server.util.time.PeriodUnit;
import com.sapienter.jbilling.test.Asserts;
import com.sapienter.jbilling.test.JBillingLogFileReader;

/**
 * @author Emil
 */
@Test(groups = { "web-services", "user" }, testName = "user.WSTest")
public class WSTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	private final static int CC_PM_ID = 1;
	private final static int ACH_PM_ID = 2;
	private final static int CHEQUE_PM_ID = 3;
    private static final Integer ROOT_ENTITY_ID = 1;
    private static final Integer MIGRATION_ACCOUNT_TYPE_ID = 60103;
    
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	private final static String CC_MF_AUTOPAYMENT_LIMIT = "autopayment.limit";
	
	private final static String ACH_MF_ROUTING_NUMBER = "ach.routing.number";
	private final static String ACH_MF_BANK_NAME = "ach.bank.name";
	private final static String ACH_MF_CUSTOMER_NAME = "ach.customer.name";
	private final static String ACH_MF_ACCOUNT_NUMBER = "ach.account.number";
	private final static String ACH_MF_ACCOUNT_TYPE = "ach.account.type";
	private final static String ACH_MF_GATEWAY_KEY = "ach.gateway.key";
	
	private final static String CHEQUE_MF_BANK_NAME = "cheque.bank.name";
	private final static String CHEQUE_MF_DATE = "cheque.date";
	private final static String CHEQUE_MF_NUMBER = "cheque.number";

    private final static String LEVEL_DEBUG = "level=\"DEBUG\"";
    private final static String LEVEL_INFO = "level=\"INFO\"";
    private final static String LEVEL_ERROR = "level=\"ERROR\"";
    private JbillingAPI api;
    private static Integer DYNAMIC_BALANCE_MANAGER_PLUGIN_ID;
    private static final String CONTACT_EMAIL = "contact.email";
    private static final String CONTACT_FIRST_NAME = "contact.first.name";
    private static final String CONTACT_LAST_NAME = "contact.last.name";
    private static final String EXCEPTION_MSG = "The updateContactInformation api must throw an exception";

    @BeforeClass
    protected void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = getOrCreatePluginWithoutParams(
                "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask", 10007);
    }

    @AfterClass
    protected void tearDown() throws Exception {
        if(null != DYNAMIC_BALANCE_MANAGER_PLUGIN_ID) {
            api.deletePlugin(DYNAMIC_BALANCE_MANAGER_PLUGIN_ID);
        }
    }

    @Test
	public void test001GetUser() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS userCreated = null;
		try {
            userCreated = createUser(true, null, null);
            logger.debug("Getting user {}", userCreated.getId());
			UserWS ret = api.getUserWS(new Integer(userCreated.getId()));
			assertEquals(userCreated.getId(), ret.getUserId());
			try {
				logger.debug("Getting invalid user 13");
				api.getUserWS(new Integer(13));
				fail("Shouldn't be able to access user 13");
			} catch (Exception e) {}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (userCreated != null) api.deleteUser(userCreated.getId());
        }
    }

	@Test
	public void test002MultipleUserWithSameUserNameCreation() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        JBillingLogFileReader jbLog = new JBillingLogFileReader();

        String callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
        String apiMethod = "api=\"createUser\"";
        String msg;
        String fullLog;

        UserWS parentUser = null;
        UserWS user = null;
        try {
            String usernameForTest = "sameUserName" + new Date().getTime();
            parentUser = createParent(api);
            user = createUser(true, true, parentUser.getId(), null, false);
			user.setUserName(usernameForTest);
			try {
				logger.debug("Creating the first user...");
                jbLog.setWatchPoint();
				user.setUserId(api.createUser(user));
                msg = "Created new user with ID: " + user.getId();
                fullLog = jbLog.readLogAsString();

                LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_USER,
                        LogConstants.STATUS_SUCCESS, LogConstants.ACTION_CREATE, msg);

				logger.debug("No exception is thrown because the username is not in use.");
			} catch (SessionInternalError e) {
				logger.error("Error validating the logs", e);
				fail("No error should occur");
			}

			logger.debug("Creating the second user with the same username as the first one...");
			UserWS user2 = createUser(true, true, parentUser.getId(), null, false);
			user2.setUserName(usernameForTest);
			try {
                jbLog.setWatchPoint();
                api.createUser(user2);
			} catch (SessionInternalError e) {
				logger.error("A SessionInternalError occurs because the username is already in use.");
				assertEquals("One error should occur", 1, e.getErrorMessages().length);
                msg = "User already exists with username " + user.getUserName();
                fullLog = jbLog.readLogAsString();
                callerClass = "class=\"c.s.j.s.u.WebServicesSessionSpringBean\"";
                LoggingValidator.validateEnhancedLog(fullLog, LEVEL_ERROR, callerClass, apiMethod, LogConstants.MODULE_USER,
                        LogConstants.STATUS_NOT_SUCCESS, LogConstants.ACTION_CREATE, msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (user != null) api.deleteUser(user.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

	public void testUserWithParentIdSameAsOwnUserId() {
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            String msg = "The parent id cannot be the same as user id for this customer.";
            JBillingLogFileReader jbLog = new JBillingLogFileReader();
            // create our test user
            UserWS user = createUser(true, true, null, null, false);
            try {
                logger.debug("Creating the test user...");
                user = api.getUserWS(api.createUser(user));
                logger.debug("No exception is thrown.");
            } catch (SessionInternalError e) {
                logger.error("Error getting the User", e);
                fail("No error should occur");
            }
            // Set the parent id same as user's own id.
            user.setParentId(user.getUserId());
            try {
                // invoke update of user after making parent id and user id as
                // same.
                user.setPassword(null);
                jbLog.setWatchPoint();
                api.updateUser(user);
            } catch (SessionInternalError e) {
                logger.error("A SessionInternalError occurs because the parent id set is the same as user's own id.", e);
                assertEquals("One error should occur", 1, e.getErrorMessages().length);
                assertTrue(jbLog.readLogAsString().contains(msg));
            }
        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}


//	  public void testOwingBalance() { try { JbillingAPI api =
//	  JbillingAPIFactory.getAPI();
//
//	  System.out.println("Getting balance of user 2"); UserWS ret =
//	  api.getUserWS(new Integer(2));
//	  assertEquals("Balance of Gandlaf starts at 1377287.98", new
//	  BigDecimal("1377287.98"), ret.getOwingBalanceAsDecimal());
//	  System.out.println("Gandalf's balance: " + ret.getOwingBalance());
//
//	  } catch (Exception e) { e.printStackTrace(); fail("Exception caught:" +
//	  e); } }
//

	@Test
	public void test003CreateUpdateDeleteUser() throws IOException, JbillingAPIException {
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
				assertEquals("One error", 1, e.getErrorMessages().length);
				assertEquals("Error message",
						"UserWS,id,validation.error.max,0",
						e.getErrorMessages()[0]);
			}

			// now add the wrong user name
			badUser.setUserName("");
			try {
				api.createUser(badUser);
			} catch (SessionInternalError e) {
				assertEquals("Two errors", 2, e.getErrorMessages().length);
				assertTrue(
						"Error message",
						"UserWS,userName,validation.error.size,1,512"
								.compareTo(e.getErrorMessages()[0]) == 0
								|| "UserWS,userName,validation.error.size,1,512"
										.compareTo(e.getErrorMessages()[1]) == 0);
            }

			// update: the user id has to be more 0
			badUser.setUserId(0);
			badUser.setUserName("12345"); // bring it back to at least 5 length
			try {
	            badUser.setPassword(null);
	            api.updateUser(badUser);
			} catch (SessionInternalError e) {
				assertEquals("One error", 1, e.getErrorMessages().length);
				assertEquals("Error message",
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
				assertEquals("Two errors", 1, e.getErrorMessages().length);
				assertTrue("Error message",
						"UserWS,userName,validation.error.size,1,512".equals(e
								.getErrorMessages()[0]));
			}

			logger.debug("Validation tested");

			// Create - This passes the password validation routine.
            parentCreated = createParent(api);

            newUser = createUser(true,parentCreated.getId(), null);
			Integer newUserId = newUser.getUserId();
			String newUserName = newUser.getUserName();
			assertNotNull("The user was not created", newUserId);

			logger.debug("Getting the id of the new user: {}", newUserName);
			Integer ret = api.getUserId(newUserName);
			assertEquals("Id of new user found", newUserId, ret);

			// verify the created user
			logger.debug("Getting created user {}", newUserId);
			UserWS retUser = api.getUserWS(newUserId);
			PaymentInformationWS instrument = retUser.getPaymentInstruments().iterator().next();

			assertEquals("created username", retUser.getUserName(),
                        newUser.getUserName());
            assertEquals("create user parent id", new Integer(parentCreated.getId()),
                        retUser.getParentId());
			logger.debug("My user: {}", retUser);

			assertEquals("created credit card name",
                        "Frodo Baggins", new String(getMetaField(instrument.getMetaFields(), CC_MF_CARDHOLDER_NAME).getCharValue()));

            //  Make a create mega call

            logger.debug("Making mega call");
            retUser.setUserName("MU"
                    + Long.toHexString(System.currentTimeMillis()));
            // need to reset the password, it came encrypted
            // let's use a long one

            // 2014-11-04 Igor Poteryaev. commented out
            // can't change password here, because of constraint for new passwords only once per day
            // retUser.setPassword("0fu3js8wl1;a$e2w)xRQ");

            // the new user shouldn't be a child
            retUser.setParentId(null);

            // need an order for it
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

            logger.debug("Validating new invoice");
            // validate that the results are reasonable
            assertNotNull("Mega call result can't be null", mcRet);
            assertNotNull("Mega call invoice result can't be null",
                    mcRet.getInvoiceId());
            // there should be a successfull payment
            assertEquals("Payment result OK", true, mcRet.getPaymentResult()
                    .getResult().booleanValue());
            assertEquals("Processor code", "fake-code-default", mcRet
                    .getPaymentResult().getCode1());
            // get the invoice
            InvoiceWS retInvoice = api.getInvoiceWS(mcRet.getInvoiceId());
            assertNotNull("New invoice not present", retInvoice);
            assertEquals("Balance of invoice should be zero, is paid",
                    new BigDecimal("0.00"), retInvoice.getBalanceAsDecimal());
            assertEquals("Total of invoice should be total of order",
                    new BigDecimal("20.00"), retInvoice.getTotalAsDecimal());
            assertEquals("New invoice paid", retInvoice.getToProcess(),
                    new Integer(0));

            // TO-DO test that the invoice total is equal to the order total

            // Update

            // now update the created user
            logger.debug("Updating user - Pass 1 - Should succeed");
            retUser = api.getUserWS(newUserId);
            retUser.setCreditLimit(new BigDecimal("112233.0"));
            logger.debug("Updating user...");
            updateMetaField(retUser.getPaymentInstruments().iterator().next()
                    .getMetaFields(), CC_MF_NUMBER, ("4111111111111152").toCharArray());
            retUser.setPassword(null);

            jbLog.setWatchPoint();
            api.updateUser(retUser);
            String msg = "User updated successfully: " + retUser.getId();
            String fullLog = jbLog.readLogAsString();
            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_DEBUG, callerClass, apiMethod, LogConstants.MODULE_USER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_UPDATE, msg);
            // and ask for it to verify the modification
            logger.debug("Getting updated user ");
            retUser = api.getUserWS(newUserId);
            assertNotNull("Didn't get updated user", retUser);

            assertEquals(
                    "Credit card updated",
                    "4111111111111152",
					new String(getMetaField(
                            retUser.getPaymentInstruments().iterator().next()
                                    .getMetaFields(), CC_MF_NUMBER)
                            .getCharValue()));
            assertEquals("credit limit updated", new BigDecimal("112233.00"),
                    retUser.getCreditLimitAsDecimal());

            // credit card is no longer implemented
            // retUser.setCreditCard(null);
            // call the update
            retUser.setPassword(null); // should not change the password
            api.updateUser(retUser);
            // fetch the user
            UserWS updatedUser = api.getUserWS(newUserId);
            // credit card functionality has been swapped by payment instrument
            // assertEquals("Credit card should stay the same",
            // "4111111111111152",
            // updatedUser.getCreditCard().getNumber());

            logger.debug("Update result: {}", updatedUser);

            // update credit card details
            logger.debug("Removing first payment method");
            // credit card functionality is no longer available this way, you
            // have to remove a payment information manually
            // api.updateCreditCard(newUserId, null);
            api.removePaymentInstrument(updatedUser.getPaymentInstruments()
                    .iterator().next().getId());
            // get updated user with removed payment instrument
            updatedUser = api.getUserWS(newUserId);
            assertEquals("Credit card removed", (int) new Integer(0), (int) updatedUser.getPaymentInstruments().size());

            logger.debug("Creating credit card");
            String ccName = "New ccName";
            String ccNumber = "4012888888881881";
            Date ccExpiry = Util.truncateDate(Calendar.getInstance().getTime());

            PaymentInformationWS newCC = createCreditCard(ccName, ccNumber,
                    ccExpiry);
			updatedUser.getPaymentInstruments().add(newCC);

			updatedUser.setPassword(null);
			api.updateUser(updatedUser);

			// check updated cc details
			retUser = api.getUserWS(newUserId);
			PaymentInformationWS retCc = retUser.getPaymentInstruments()
					.iterator().next();
			assertEquals("new cc name", ccName,
					new String(getMetaField(retCc.getMetaFields(), CC_MF_CARDHOLDER_NAME)
							.getCharValue()));
			assertEquals("updated cc number", ccNumber,
					new String(getMetaField(retCc.getMetaFields(), CC_MF_NUMBER)
							.getCharValue()));
			assertEquals("updated cc expiry", DateTimeFormat.forPattern(
							Constants.CC_DATE_FORMAT).print(ccExpiry.getTime()),
					new String(getMetaField(retCc.getMetaFields(), CC_MF_EXPIRY_DATE)
							.getCharValue()));

			// set the credit card ID so that we update the existing card with
			// the API call
			newCC.setId(retCc.getId());

            // following functionality is not part of design anymore

            // try and update the card details ignoring the credit card number
            // System.out.println("Updating credit card");
            // cc.setName("Updated ccName");
            // cc.setNumber(null);
            // api.updateCreditCard(newUserId, cc);
            // retUser = api.getUserWS(newUserId);
            // assertEquals("updated cc name", "Updated ccName",
            // retUser.getCreditCard().getName());
            // assertNotNull("cc number still there",
            // retUser.getCreditCard().getNumber());

            // try to update cc of user from different company
            // System.out.println("Attempting to update cc of a user from "
            // + "a different company");
            // try {
            // api.updateCreditCard(new Integer(13), cc);
            // fail("Shouldn't be able to update cc of user 13");
            // } catch (Exception e) {
            // }


            // Delete

            // now delete this new guy
            logger.debug("Deleting user... {}", newUserId);

            jbLog.setWatchPoint();
            api.deleteUser(newUserId);
            callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
            apiMethod = "api=\"deleteUser\"";
            msg = "User with ID: " + newUserId + " has been deleted.";
            fullLog = jbLog.readLogAsString();

            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_USER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE, msg);

            // try to fetch the deleted user
            logger.debug("Getting deleted user {}", newUserId);
            updatedUser = api.getUserWS(newUserId);
            assertEquals(updatedUser.getDeleted(), 1);

            // verify I can't delete users from another company
            try {
                logger.debug("Deleting user base user ... 13");
                api.getUserWS(new Integer(13));
                fail("Shouldn't be able to access user 13");
            } catch (Exception e) {
            }
        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (newUser != null) api.deleteUser(newUser.getId());
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
        }
    }

	@Test
	public void test004CreditCardUpdates() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentCreated = null;
        UserWS user = null;

//		  Note, a more direct test would be to write a unit test for the
//		  CreditCardDTO class itself, but our current testing framework doesn't
//		  support this style. Instead, test CreditCardBL which should the
//		  standard service interface for all credit card interaction.
//
//
//
//		  After implementation of #6215 - account payment information now we
//		  can update each payment information individually and there is not
//		  updating of most recent credit card as now user can enter more than
//		  one credit cards
        try {

            parentCreated = createParent(api);

            user = createUser(true, parentCreated.getId(), null);
            user = api.getUserWS(user.getUserId());

            // Visa
            updateMetaField(user.getPaymentInstruments().iterator().next()
                    .getMetaFields(), CC_MF_NUMBER, ("4556737877253135").toCharArray());
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());

            PaymentInformationWS card = user.getPaymentInstruments().iterator()
                    .next();
            logger.debug("Updated card {}",  card.getId());
            assertEquals("card type Visa", CreditCardType.VISA.toString(),
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());

            // Mastercard
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, ("5111111111111985").toCharArray());
            logger.debug("Updating credit card {}", card.getId()
                    + " With a Mastercard number");
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            logger.debug("Updated card {}", card.getId());
            assertEquals("card type Mastercard",
                    CreditCardType.MASTER_CARD.toString(),
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());

            // American Express
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, ("378949830612125").toCharArray());
            logger.debug("Updating credit card {} with an American Express number", card.getId());
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            logger.debug("Updated card {}", card.getId());
            assertEquals("card type American Express",
                    CreditCardType.AMEX.toString(),
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());

            // Diners Club
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, ("3611111111111985").toCharArray());
            logger.debug("Updating credit card with a Diners Club number", card.getId());
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            logger.debug("Updated card {}", card.getId());
            assertEquals("card type Diners", CreditCardType.DINERS.toString(),
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());

            // Discovery
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, ("6011874982335947").toCharArray());
            logger.debug("Updating credit card with a Discovery card number", card.getId());
            user.setPassword(null);
            api.updateUser(user);

            user = api.getUserWS(user.getUserId());
            card = user.getPaymentInstruments().iterator().next();
            logger.debug("Updated card {}", card.getId());
            assertEquals("card type Discovery", CreditCardType.DISCOVER.toString(),
                    getMetaField(card.getMetaFields(), CC_MF_TYPE).getStringValue());
        } finally {
            // cleanup
            if (user != null) api.deleteUser(user.getId());
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
        }

	}

    private UserWS createParent(JbillingAPI api) throws JbillingAPIException, IOException {
        UserWS parentCreated = createUser(true, null, null);
        parentCreated.setIsParent(true);
        parentCreated.setPassword(null);
        api.updateUser(parentCreated);
        assertNotNull("The parent user was not created", parentCreated);
        return parentCreated;
    }

    @Test
	public void test005LanguageId() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer newUserId = null;
        try {
            UserWS newUser = new UserWS();
			newUser.setUserName("language-test" + new Date().getTime());
			newUser.setPassword("As$fasdf1");
			newUser.setLanguageId(new Integer(2)); // French
			newUser.setMainRoleId(new Integer(5));
			newUser.setAccountTypeId(Integer.valueOf(1));
			newUser.setIsParent(new Boolean(true));
			newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

			MetaFieldValueWS metaField1 = new MetaFieldValueWS();
			metaField1.setFieldName("contact.email");
			metaField1.setValue(newUser.getUserName() + "@shire.com");
			metaField1.setGroupId(1);

			newUser.setMetaFields(new MetaFieldValueWS[] { metaField1 });

			logger.debug("Creating user ...");
			// do the creation
			newUserId = api.createUser(newUser);

			// get user
			UserWS createdUser = api.getUserWS(newUserId);
			assertEquals("Language id", 2, createdUser.getLanguageId()
					.intValue());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            // clean up
            if (newUserId != null) api.deleteUser(newUserId);
        }
    }

    //TODO: Test commented because I can't understand what it's testing and how
	/*@Test
	public void test006UserTransitions() throws Exception {
		System.out.println("#test006UserTransitions");
        JbillingAPI api = JbillingAPIFactory.getAPI();

        try {
            Date beforeTransitionDates = new Date();
            System.out.println("Getting complete list of user transitions");
            UserWS user = createUser(true, null, null);
            user.setStatusId(6);
            api.updateUser(user);
            UserTransitionResponseWS[] ret = api.getUserTransitions(beforeTransitionDates, new Date());

			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals(6, ret.length);

			// Check the ids of the returned transitions
			assertEquals(ret[0].getId().intValue(), 1);
			assertEquals(ret[1].getId().intValue(), 2);
			// Check the value of returned data
			assertEquals(ret[0].getUserId().intValue(), 2);
			assertEquals(ret[0].getFromStatusId().intValue(), 2);
			assertEquals(ret[0].getToStatusId().intValue(), 1);
			assertEquals(ret[1].getUserId().intValue(), 2);
			assertEquals(ret[1].getFromStatusId().intValue(), 2);
			assertEquals(ret[1].getToStatusId().intValue(), 1);

			// save an ID for later
			Integer myId = ret[4].getId();

			System.out
					.println("Getting first partial list of user transitions");
			ret = api.getUserTransitions(new Date(2000 - 1900, 0, 0), new Date(
					2007 - 1900, 0, 1));
			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals(ret.length, 1);

			assertEquals(ret[0].getId().intValue(), 1);
			assertEquals(ret[0].getUserId().intValue(), 2);
			assertEquals(ret[0].getFromStatusId().intValue(), 2);
			assertEquals(ret[0].getToStatusId().intValue(), 1);

			System.out
					.println("Getting second partial list of user transitions");
			ret = api.getUserTransitions(null, null);
			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals(5, ret.length);

			assertEquals(ret[0].getId().intValue(), 2);
			assertEquals(ret[0].getUserId().intValue(), 2);
			assertEquals(ret[0].getFromStatusId().intValue(), 2);
			assertEquals(ret[0].getToStatusId().intValue(), 1);

			System.out.println("Getting list after id");
			ret = api.getUserTransitionsAfterId(myId);
			if (ret == null)
				fail("Transition list should not be empty!");
			assertEquals("Only one transition after id " + myId, 1, ret.length);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}*/



//	    Parent 1 10752
//	          |
//	           +----+ ---------+-------+
//	           |    |          |       |
//	   10753 iCh1  Ch2 10754  Ch6     iCh7
//	          /\    |                  |
//	         /  \   |                 Ch8
//	      Ch3 iCh4 Ch5
//	    10755 10756 10757
//
//	   Ch3->Ch1
//	   Ch4->Ch4
//	   Ch1->Ch1
//	   Ch5->P1
//	   Ch2->P1
//	   Ch6->P1
//	   Ch7-> Ch7 (its own one time order)
//	   Ch8: no applicable orders

	@Test
	public void test007ParentChild() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        //  Create - This passes the password validation routine.
        Integer parentId = null;
        List<Integer> childrenToRemove = new ArrayList<Integer>();
        Integer child8Id = null;
        Integer child5Id = null;
        Integer child4Id = null;
        Integer child3Id = null;
        Integer child7Id = null;
        Integer child6Id = null;
        Integer child2Id = null;
        Integer child1Id = null;
	   try {
           UserWS newUser = new UserWS();
	       newUser.setUserName("parent1" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setLanguageId(new Integer(1));
	       newUser.setMainRoleId(new Integer(5));
	       newUser.setAccountTypeId(Integer.valueOf(1));
	       newUser.setIsParent(new Boolean(true));
	       newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

	       MetaFieldValueWS metaField1 = new MetaFieldValueWS();
	       metaField1.setFieldName("contact.email");
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       metaField1.setGroupId(1);

	       newUser.setMetaFields(new MetaFieldValueWS[]{metaField1});

	       logger.debug("Creating parent user ...");
	       // do the creation
	       parentId = api.createUser(newUser);
	       assertNotNull("The user was not created", parentId);

	       // verify the created user
	       logger.debug("Getting created user ");
	       UserWS retUser = api.getUserWS(parentId);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("create user is parent", new Boolean(true), retUser.getIsParent());

	       logger.debug("Creating child1 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child1" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.TRUE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child1Id = api.createUser(newUser);
           childrenToRemove.add(child1Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child1Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.TRUE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       Integer[] childIds = retUser.getChildIds();
	       assertEquals("1 child", 1, childIds.length);
	       assertEquals("created user child", child1Id, childIds[0]);

	       logger.debug("Creating child2 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child2" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child2Id = api.createUser(newUser);
           childrenToRemove.add(child2Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child2Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       childIds = retUser.getChildIds();
	       assertEquals("2 child", 2, childIds.length);
	       assertEquals("created user child", child2Id,
	               childIds[0].equals(child2Id) ? childIds[0] : childIds[1]);

	       logger.debug("Creating child6 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child6" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child6Id = api.createUser(newUser);
           childrenToRemove.add(child6Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child6Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       childIds = retUser.getChildIds();
	       assertEquals("3 child", 3, childIds.length);
	       assertEquals("created user child", child6Id,
	               childIds[0].equals(child6Id) ? childIds[0] :
	                       childIds[1].equals(child6Id) ? childIds[1] : childIds[2]);

	       logger.debug("Creating child7 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(parentId);
	       newUser.setUserName("child7" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.TRUE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child7Id = api.createUser(newUser);
           childrenToRemove.add(child7Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child7Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", parentId, retUser.getParentId());
	       assertEquals("created invoice child", Boolean.TRUE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(parentId);
	       childIds = retUser.getChildIds();
	       assertEquals("4 child", 4, childIds.length);

	       logger.debug("Creating child8 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(true));
	       newUser.setParentId(child7Id);
	       newUser.setUserName("child8" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child8Id = api.createUser(newUser);
           childrenToRemove.add(child8Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child8Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child7Id, retUser.getParentId());
	       assertEquals("created invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child7Id);
	       childIds = retUser.getChildIds();
	       assertEquals("1 child", 1, childIds.length);

	       logger.debug("Creating child3 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(false));
	       newUser.setParentId(child1Id);
	       newUser.setUserName("child3" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child3Id = api.createUser(newUser);
           childrenToRemove.add(child3Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child3Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child1Id, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child1Id);
	       childIds = retUser.getChildIds();
	       assertEquals("1 child", 1, childIds.length);
	       assertEquals("created user child", child3Id, childIds[0]);

	       logger.debug("Creating child4 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(false));
	       newUser.setParentId(child1Id);
	       newUser.setUserName("child4" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.TRUE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child4Id = api.createUser(newUser);
           childrenToRemove.add(child4Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child4Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child1Id, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.TRUE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child1Id);
	       childIds = retUser.getChildIds();
	       assertEquals("2 child for child1", 2, childIds.length);
	       assertEquals("created user child", child4Id, childIds[0].equals(child4Id) ? childIds[0] : childIds[1]);

	       logger.debug("Creating child5 user ...");
	       // now create the child
	       newUser.setIsParent(new Boolean(false));
	       newUser.setParentId(child2Id);
	       newUser.setUserName("child5" + new Date().getTime());
	       newUser.setPassword("As$fasdf1");
	       newUser.setInvoiceChild(Boolean.FALSE);
	       metaField1.setValue(newUser.getUserName() + "@shire.com");
	       child5Id = api.createUser(newUser);
           childrenToRemove.add(child5Id);
	       //test
	       logger.debug("Getting created user ");
	       retUser = api.getUserWS(child5Id);
	       assertEquals("created username", retUser.getUserName(),
	               newUser.getUserName());
	       assertEquals("created user parent", child2Id, retUser.getParentId());
	       assertEquals("created do not invoice child", Boolean.FALSE, retUser.getInvoiceChild());

	       // test parent has child id
	       retUser = api.getUserWS(child2Id);
	       childIds = retUser.getChildIds();
	       assertEquals("1 child for child2", 1, childIds.length);
	       assertEquals("created user child", child5Id, childIds[0]);

	       // create an order for all these users
	       logger.debug("Creating orders for all users");
	       
	       createOrder(parentId);
	       createOrder(child1Id);
	       createOrder(child2Id);
	       createOrder(child3Id);
	       createOrder(child4Id);
	       createOrder(child5Id);
	       createOrder(child6Id);
	       createOrder(child7Id);
	       
	       // run the billing process for each user, validating the results
	       logger.debug("Invoicing and validating...");
	       // parent1
	       Integer[] invoices = api.createInvoice(parentId, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       InvoiceWS invoice = api.getInvoiceWS(invoices[0]);
	       assertEquals("invoice should be 80$", new BigDecimal("80.00"), invoice.getTotalAsDecimal());
	       // child1
	       invoices = api.createInvoice(child1Id, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       invoice = api.getInvoiceWS(invoices[0]);
	       assertEquals("invoice should be 40$", new BigDecimal("40.00"), invoice.getTotalAsDecimal());
	       // child2
	       invoices = api.createInvoice(child2Id, false);
	       // CXF returns null for empty arrays
	       if (invoices != null) {
	           assertEquals("there should be no invoice", 0, invoices.length);
	       }
	       // child3
	       invoices = api.createInvoice(child3Id, false);
	       if (invoices != null) {
	           assertEquals("there should be no invoice", 0, invoices.length);
	       }
	       // child4
	       invoices = api.createInvoice(child4Id, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       invoice = api.getInvoiceWS(invoices[0]);
	       assertEquals("invoice should be 20$", new BigDecimal("20.00"), invoice.getTotalAsDecimal());
	       // child5
	       invoices = api.createInvoice(child5Id, false);
	       if (invoices != null) {
	           assertEquals("there should be no invoice", 0, invoices.length);
	       }
	       // child6
	       invoices = api.createInvoice(child6Id, false);
	       if (invoices != null) {
	           assertEquals("there should be one invoice", 0, invoices.length);
	       }
	       // child7 (for bug that would ignore an order from a parent if the
	       // child does not have any applicable)
	       invoices = api.createInvoice(child7Id, false);
	       assertNotNull("invoices cant be null", invoices);
	       assertEquals("there should be one invoice", 1, invoices.length);
	       invoice = api.getInvoiceWS(invoices[0]);
	       assertEquals("invoice should be 20$", new BigDecimal("20.00"), invoice.getTotalAsDecimal());


	   } catch (Exception e) {
	       e.printStackTrace();
	       fail("Exception caught:" + e);
	   } finally {
           // clean up
           deleteWithCheckUser(child8Id, api);
           deleteWithCheckUser(child5Id, api);
           deleteWithCheckUser(child4Id, api);
           deleteWithCheckUser(child3Id, api);
           deleteWithCheckUser(child7Id, api);
           deleteWithCheckUser(child6Id, api);
           deleteWithCheckUser(child2Id, api);
           deleteWithCheckUser(child1Id, api);
           deleteWithCheckUser(parentId, api);
       }

	}

    private void deleteWithCheckUser(Integer userId, JbillingAPI api) {
        if (userId != null) api.deleteUser(userId);
    }
    // todo: Returns 8 records as there are duplicate entries in the
	// user_credit_card_map. Appears to be a bug, fix later!

//	  public void testGetByCC() { // note: this method getUsersByCreditCard
//	  seems to have a bug. It does // not reutrn Gandlaf if there is not an
//	  updateUser call before try { JbillingAPI api =
//	  JbillingAPIFactory.getAPI(); Integer[] ids =
//	  api.getUsersByCreditCard("1152"); assertNotNull("Four customers with CC",
//	  ids); assertEquals("Four customers with CC", 6, ids.length); // returns
//	  credit cards from both clients? // 5 cards from entity 1, 1 card from
//	  entity 2 assertEquals("Created user with CC", 10792, ids[ids.length -
//	  1].intValue());
//
//	  // get the user
//	  assertNotNull("Getting found user",api.getUserWS(ids[0])); } catch
//	  (Exception e) { e.printStackTrace(); fail("Exception caught:" + e); } }
//

	@Test
	public void test008UserMainSubscription() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// now get the user
        Integer userID = createUser(true, null, null).getId();
        try {
            UserWS user = api.getUserWS(userID);
            user.setPassword(null);
            api.updateUser(user);
            MainSubscriptionWS existingMainSubscription = user.getMainSubscription();
            logger.debug("User's existing main subscription = {}", existingMainSubscription);

            MainSubscriptionWS newMainSubscription = new MainSubscriptionWS(2, 1);
            user.setNextInvoiceDate(api.getUserWS(userID).getNextInvoiceDate());
            user.setMainSubscription(newMainSubscription);
            logger.debug("User's new main subscription = {}", user.getMainSubscription());

            // update the user
            user.setPassword(null);
            api.updateUser(user);

            // validate that the user does have the new main subscription
            assertEquals("User does not have the correct main subscription",
                    newMainSubscription, api.getUserWS(userID)
                            .getMainSubscription());

            // update the user (restore main sub)
            user.setMainSubscription(existingMainSubscription);
            user.setPassword(null);
            api.updateUser(user);
            assertEquals("User does not have the original main subscription",
                    existingMainSubscription, api.getUserWS(userID)
                            .getMainSubscription());
        } finally {
            if (userID != null) api.deleteUser(userID);
        }
	}

	@Test
	public void test009PendingUnsubscription() {
        //TODO: This test have to be changed to not be based on an existing customer
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();
			OrderWS order = api.getLatestOrder(1055);
			order.setActiveUntil(new Date(2008 - 1900, 11 - 1, 1)); // sorry
			api.updateOrder(order, null);
			assertEquals("User 1055 should be now in pending unsubscription",
					UserDTOEx.SUBSCRIBER_PENDING_UNSUBSCRIPTION,
					api.getUserWS(1055).getSubscriberStatusId());
        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}

	@Test
	public void test010Currency() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer myId = null;
		try {
			UserWS myUser = createUser(true, null, 11);
			myId = myUser.getUserId();
			logger.debug("Checking currency of new user");
			myUser = api.getUserWS(myId);
			assertEquals("Currency should be A$", 11, myUser.getCurrencyId()
					.intValue());
			myUser.setCurrencyId(1);
			logger.debug("Updating currency to US$");
			myUser.setPassword(null); // otherwise it will try the encrypted
										// password
			api.updateUser(myUser);
			logger.debug("Checking currency ...");
			myUser = api.getUserWS(myId);
			assertEquals("Currency should be US$", 1, myUser.getCurrencyId()
					.intValue());
			logger.debug("Removing");

			JBillingLogFileReader jbLog = new JBillingLogFileReader();

			Integer userId = api.getUserWS(myId).getUserId();

            String callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
            String apiMethod = "api=\"deleteUser\"";
            String msg = "User with ID: " + userId + " has been deleted.";


            jbLog.setWatchPoint();
            api.deleteUser(userId);
            String fullLog = jbLog.readLogAsString();
            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_USER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE, msg);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (myId != null) api.deleteUser(myId);
        }
	}

	@Test
	public void test011PrePaidBalance() {
		try {
			JbillingAPI api = JbillingAPIFactory.getAPI();
            JBillingLogFileReader jbLog = new JBillingLogFileReader();
			UserWS myUser = createUser(true, null, null);
			Integer myId = myUser.getUserId();

			// update to pre-paid
            myUser.setPassword(null);
			api.updateUser(myUser);

			// get the current balance, it should be null or 0
			logger.debug("Checking initial balance type and dynamic balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 0 balance", BigDecimal.ZERO,
					myUser.getDynamicBalanceAsDecimal());

			// validate. room = 0, price = 7
			logger.debug("Validate with fields...");
			PricingField pf[] = { new PricingField("src", "604"),
					new PricingField("dst", "512") };
			ValidatePurchaseWS result = api.validatePurchase(myId, 2800, pf);
			assertEquals("validate purchase success 1", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 1",
					Boolean.valueOf(false), result.getAuthorized());

			assertEquals("validate purchase quantity 1", BigDecimal.ZERO, result.getQuantityAsDecimal());
			assertEquals("user should have 0 balance", BigDecimal.ZERO, myUser.getDynamicBalanceAsDecimal());

			// add a payment
			PaymentWS payment = new PaymentWS();
			payment.setAmount(new BigDecimal("20.00"));
			payment.setIsRefund(new Integer(0));
			payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
			payment.setPaymentDate(Calendar.getInstance().getTime());
			payment.setResultId(Constants.RESULT_ENTERED);
			payment.setCurrencyId(new Integer(1));
			payment.setUserId(myId);

			payment.getPaymentInstruments().add(createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime()));

			logger.debug("Applying payment");
			api.applyPayment(payment, null);
			// check new balance is 20
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 20 balance", new BigDecimal("20"), myUser.getDynamicBalanceAsDecimal());

			// now create a one time order, the balance should decrease
			OrderWS order = getOrder();
			order.setUserId(myId);
			logger.debug("creating one time order");
			Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 0 balance", BigDecimal.ZERO, myUser.getDynamicBalanceAsDecimal());

			// for the following, use line 2 with item id 2. item id 1 has
			// cancellation fees rules that affect the balance.
			// increase the quantity of the one-time order
			logger.debug("adding quantity to one time order");
			pause(2000); // pause while provisioning status is being updated
			order = api.getOrder(orderId);
			OrderLineWS line = order.getOrderLines()[0].getItemId() == 2 ? order
					.getOrderLines()[0] : order.getOrderLines()[1];
			line.setAmount(new BigDecimal("7").multiply(line.getPriceAsDecimal()));
			OrderChangeWS orderChange = OrderChangeBL.buildFromLine(line, null, ORDER_CHANGE_STATUS_APPLY_ID);
			orderChange.setQuantity(BigDecimal.valueOf(7).subtract(line.getQuantityAsDecimal()));
			line.setQuantity(7);

			BigDecimal delta = new BigDecimal("6.00").multiply(line.getPriceAsDecimal());
			api.updateOrder(order, new OrderChangeWS[] { orderChange });
			myUser = api.getUserWS(myId);
			assertEquals("user should have new balance", delta.negate(), myUser.getDynamicBalanceAsDecimal());

			// decrease the quantity of the one-time order
			logger.debug("remove quantity to one time order");
			order = api.getOrder(orderId);
			line = order.getOrderLines()[0].getItemId() == 2 ? order.getOrderLines()[0] : order.getOrderLines()[1];
			orderChange = OrderChangeBL.buildFromLine(line, null, ORDER_CHANGE_STATUS_APPLY_ID);
			orderChange.setQuantity(BigDecimal.valueOf(1).subtract(line.getQuantityAsDecimal()));
			line.setQuantity(1);
			line.setAmount(line.getQuantityAsDecimal().multiply(order.getOrderLines()[1].getPriceAsDecimal()));
			api.updateOrder(order, new OrderChangeWS[] { orderChange });
			myUser = api.getUserWS(myId);
			assertEquals("user should have new balance", BigDecimal.ZERO, myUser.getDynamicBalanceAsDecimal());

			// delete one line from the one time order
			logger.debug("remove one line from one time order");
			order = api.getOrder(orderId);

			List<OrderChangeWS> orderChanges = new LinkedList<OrderChangeWS>();
			for (OrderLineWS orderLine : order.getOrderLines()) {
				if (orderLine.getItemId() != 1) {
					orderChange = OrderChangeBL.buildFromLine(orderLine, null, ORDER_CHANGE_STATUS_APPLY_ID);
					orderChange.setQuantity(orderLine.getQuantityAsDecimal().negate());
					orderLine.setDeleted(1);
					orderChanges.add(orderChange);
				}
			}

			api.updateOrder(order, orderChanges.toArray(new OrderChangeWS[orderChanges.size()]));

			myUser = api.getUserWS(myId);
			assertEquals("user should have new balance", new BigDecimal("10"), myUser.getDynamicBalanceAsDecimal());

			// delete the order, the balance has to go back to 20
			logger.debug("deleting one time order");
			api.deleteOrder(orderId);
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 20 balance", new BigDecimal("20"), myUser.getDynamicBalanceAsDecimal());

			// now create a recurring order with invoice, the balance should
			// decrease
			order = getOrder();
			order.setUserId(myId);
			order.setPeriod(2);

			// make it half a month to test pro-rating
			order.setActiveSince(new DateMidnight(2009, 1, 1).toDate());
			order.setActiveUntil(new DateMidnight(2009, 1, 1).plusDays(15).toDate());
			order.setProrateFlag(true);

            OrderChangeWS[] orderChanges2 = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
            for (OrderChangeWS change: orderChanges2) {
                change.setStartDate(order.getActiveSince());
                change.setEndDate(order.getActiveUntil());
            }

			logger.debug("creating recurring order and invoice");
			api.createOrderAndInvoice(order, orderChanges2);
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);

			assertEquals("user should have 10.32 balance (15 out of 31 days)",
					new BigDecimal("10.32"),
					myUser.getDynamicBalanceAsDecimal());

			logger.debug("Removing");

            String callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
            String apiMethod = "api=\"deleteUser\"";
            String msg = "User with ID: " + myId + " has been deleted.";


            jbLog.setWatchPoint();
            api.deleteUser(myId);
            String fullLog = jbLog.readLogAsString();
            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_USER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE, msg);

		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		}
	}

	@Test
	public void test012CreditLimit() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer myId = null;
        try {
			UserWS myUser = createUser(true, null, null);
			myId = myUser.getUserId();

			// update to pre-paid
			myUser.setCreditLimit(new BigDecimal("1000.0"));
            myUser.setPassword(null);
			api.updateUser(myUser);

			// get the current balance, it should be null or 0
			System.out
					.println("Checking initial balance type and dynamic balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 0 balance", BigDecimal.ZERO,
					myUser.getDynamicBalanceAsDecimal());

			// now create a one time order, the balance should increase
			OrderWS order = getOrder();
			order.setUserId(myId);
			logger.debug("creating one time order");
			Integer orderId = api.createOrder(order, OrderChangeBL
					.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 20 balance", new BigDecimal("-20.0"),
					myUser.getDynamicBalanceAsDecimal());

			// delete the order, the balance has to go back to 0
			logger.debug("deleting one time order");
			api.deleteOrder(orderId);
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 0 balance", BigDecimal.ZERO,
					myUser.getDynamicBalanceAsDecimal());

			// now create a recurring order with invoice, the balance should
			// increase
			order = getOrder();
			order.setUserId(myId);
			order.setPeriod(2);
			logger.debug("creating recurring order and invoice");
			Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL
					.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 20 balance", new BigDecimal("-20.0"),
					myUser.getDynamicBalanceAsDecimal());

			// add a payment. I'd like to call payInvoice but it's not finding
			// the CC
			PaymentWS payment = new PaymentWS();
			payment.setAmount(new BigDecimal("20.00"));
			payment.setIsRefund(new Integer(0));
			payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
			payment.setPaymentDate(Calendar.getInstance().getTime());
			payment.setResultId(Constants.RESULT_ENTERED);
			payment.setCurrencyId(new Integer(1));
			payment.setUserId(myId);

			payment.getPaymentInstruments().add(createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime()));

			logger.debug("Applying payment");
			api.applyPayment(payment, invoiceId);
			// check new balance is 20
			logger.debug("Validating new balance");
			myUser = api.getUserWS(myId);
			assertEquals("user should have 0 balance", BigDecimal.ZERO,
					myUser.getDynamicBalanceAsDecimal());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            logger.debug("Removing");
            if (myId != null) api.deleteUser(myId);
        }
    }

	@Test
	public void test013RulesValidatePurchaseTask() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer userId = null;
        Integer orderId = null;
        try {
//			  Validate purchase runs using the mediation process when pricing
//			  fields are provided.
//
//			  If there are no pricing fields, then the validation becomes a
//			  simple dynamic balance check (Credit limit or pre-paid balance)
//			  to determine if the customer has the funds in their account
//			  necessary to make the purchase.
//

			final int LEMONADE_ITEM_ID = 2602;
			final int COFFEE_ITEM_ID = 3;


			// create user
            UserWS user = createUser(true, null, null);
			userId = user.getUserId();

			// update to credit limit
			user.setCreditLimit(new BigDecimal("1000.0"));
			user.setMainSubscription(createUserMainSubscription());
            user.setPassword(null);
			api.updateUser(user);

			// lemonade order
			orderId = createOrder(userId, 2);

			// try to get another lemonde
			ValidatePurchaseWS result = api.validatePurchase(userId,
					LEMONADE_ITEM_ID, null);
			assertEquals("validate purchase success 1", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 1",
					Boolean.valueOf(false), result.getAuthorized());
			assertEquals("validate purchase quantity 1",
					new BigDecimal("0.00"), result.getQuantityAsDecimal());

			// exception should be thrown
			PricingField pf[] = { new PricingField("fail", "fail") };
			result = api.validatePurchase(userId, LEMONADE_ITEM_ID, pf);
			assertEquals("validate purchase success 2", Boolean.valueOf(false),
					result.getSuccess());
			assertEquals("validate purchase authorized 2",
					Boolean.valueOf(false), result.getAuthorized());
			assertEquals("validate purchase quantity 2", BigDecimal.ZERO,
					result.getQuantityAsDecimal());
			assertEquals("validate purchase message 2",
					"Error: Thrown exception for testing",
					result.getMessage()[0]);

			// coffee quantity available should be 20
			result = api.validatePurchase(userId, COFFEE_ITEM_ID, null);
			assertEquals("validate purchase success 3", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 3",
					Boolean.valueOf(true), result.getAuthorized());
			assertEquals("validate purchase quantity 3",
					new BigDecimal("20.0"), result.getQuantityAsDecimal());

            //TODO: createItem
            // add 10 coffees to current order
			OrderLineWS newLine = new OrderLineWS();
			newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			newLine.setItemId(new Integer(3));
			newLine.setQuantity(new BigDecimal("10.0"));
			newLine.setUseItem(new Boolean(true)); // use pricing from the item

			// update the current order
			OrderWS currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// quantity available should be 10
			result = api.validatePurchase(userId, COFFEE_ITEM_ID, null);
			assertEquals("validate purchase success 3", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 3",
					Boolean.valueOf(true), result.getAuthorized());
			assertEquals("validate purchase quantity 3",
					new BigDecimal("10.0"), result.getQuantityAsDecimal());

			// add another 10 coffees to current order
			currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// exceeded account credit limit
			// quantity available should be 0
			result = api.validatePurchase(userId, COFFEE_ITEM_ID, null);
			assertEquals("validate purchase success 4", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 4",
					Boolean.valueOf(false), result.getAuthorized());
			assertEquals("validate purchase quantity 4", BigDecimal.ZERO,
					result.getQuantityAsDecimal());
			assertEquals("validate purchase message 4",
					"No more than 20 coffees are allowed.",
					result.getMessage()[0]);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            // clean up
            pause(2000);
            if (orderId != null) api.deleteOrder(orderId);
            if (userId != null) api.deleteUser(userId);
        }
    }

	@Test
    public void test014UserBalancePurchaseTaskHierarchical() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer childId = null;
        Integer parentId = null;
        Integer orderId = null;
        try {
            // create 2 users, child and parent
            UserWS newUser = new UserWS();
            newUser.setUserName("parent1" + new Date().getTime());
            newUser.setPassword("As$fasdf1");
            newUser.setLanguageId(new Integer(1));
            newUser.setMainRoleId(new Integer(5));
            newUser.setAccountTypeId(Integer.valueOf(1));
            newUser.setIsParent(new Boolean(true));
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setCreditLimit(new BigDecimal("2000.0"));

            MetaFieldValueWS metaField1 = new MetaFieldValueWS();
            metaField1.setFieldName("contact.email");
            metaField1.setValue(newUser.getUserName() + "@shire.com");
            metaField1.setGroupId(1);

            newUser.setMetaFields(new MetaFieldValueWS[]{
                    metaField1
            });

            logger.debug("Creating parent user ...");
            // do the creation
            parentId = api.createUser(newUser);

            // now create the child
            newUser.setIsParent(new Boolean(false));
            newUser.setParentId(parentId);
            newUser.setUserName("child1" + new Date().getTime());
            newUser.setPassword("As$fasdf1");
            newUser.setInvoiceChild(Boolean.FALSE);
            newUser.setCreditLimit((String) null);
            metaField1.setValue(newUser.getUserName() + "@shire.com");
            childId = api.createUser(newUser);

            // create an order for the child
            OrderWS order = getOrder();
            order.setUserId(childId);
            logger.debug("creating one time order");
            orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

            // validate the balance of the parent
            logger.debug("Validating new balance");
            UserWS parentUser = api.getUserWS(parentId);
            assertEquals("user should have -20 balance", new BigDecimal("20.0").negate(), parentUser.getDynamicBalanceAsDecimal());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
        } finally {
            // clean up
            if (childId != null) api.deleteUser(childId);
            if (parentId != null) api.deleteUser(parentId);
            if (orderId != null) api.deleteOrder(orderId);
        }
    }

	@Test
	public void test015ValidateMultiPurchase() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS myUser = null;
		try {
			myUser = createUser(true, null, null);
			Integer myId = myUser.getUserId();

			// update to credit limit
			myUser.setCreditLimit(new BigDecimal("1000.0"));
            myUser.setPassword(null);
			api.updateUser(myUser);

			// validate with items only
			ValidatePurchaseWS result = api.validateMultiPurchase(myId,
					new Integer[] { 2800, 2, 251 }, null);
			assertEquals("validate purchase success 1", Boolean.valueOf(true),
					result.getSuccess());
			assertEquals("validate purchase authorized 1",
					Boolean.valueOf(true), result.getAuthorized());
			assertEquals("validate purchase quantity 1",
					new BigDecimal("28.57"), result.getQuantityAsDecimal());

			logger.debug("Removing");
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (myUser != null) api.deleteUser(myUser.getId());
        }
    }

	@Test
	public void test016PenaltyTaskOrder() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();

        UserWS createdUser = null;
        OrderWS order = null;
        try {
            createdUser = createUser(true, null, null);

            Integer USER_ID = 53;
            final Integer ORDER_ID = 35;
            final Integer PENALTY_ITEM_ID = 270;

            // pluggable BasicPenaltyTask is configured for ageing_step 6
            // test that other status changes will not add a new order item
            UserWS user = api.getUserWS(USER_ID);
            user.setPassword(null);
            user.setStatusId(2);
            api.updateUser(user);

            assertEquals("Status was changed", 2, api.getUserWS(USER_ID)
                    .getStatusId().intValue());
            assertEquals("No new order was created", ORDER_ID,
                    api.getLatestOrder(USER_ID).getId());

            // new order will be created with the penalty item when status id = 6
            user.setStatusId(6);
            user.setPassword(null);
            api.updateUser(user);

            assertEquals("Status was changed", 6, api.getUserWS(USER_ID)
                    .getStatusId().intValue());

            order = api.getLatestOrder(USER_ID);
            assertFalse("New order was created, id does not equal original",
                    ORDER_ID.equals(order.getId()));
            assertEquals("New order has one item", 1, order.getOrderLines().length);

            OrderLineWS line = order.getOrderLines()[0];
            assertEquals("New order contains penalty item", PENALTY_ITEM_ID,
                    line.getItemId());
            assertEquals(
                    "Order penalty value is the item price (not a percentage)",
                    new BigDecimal("10.00"), line.getAmountAsDecimal());

        }
        finally {
            // delete order and invoice
            if (order != null) api.deleteOrder(order.getId());
            if (createdUser != null) api.deleteUser(createdUser.getId());
        }
	}

	@Test
	public void test017AutoRecharge() throws Exception {

		JbillingAPI api = JbillingAPIFactory.getAPI();

		UserWS user = createUser(true, null, null);

		user.setAutoRecharge(new BigDecimal("25.00")); // automatically charge
														// this user $25 when
														// the balance drops
														// below the threshold
        user.setPassword(null);
		// company (entity id 1) recharge threshold is set to $5
		api.updateUser(user);
		user = api.getUserWS(user.getUserId());

		assertEquals("Automatic recharge value updated",
				new BigDecimal("25.00"), user.getAutoRechargeAsDecimal());

		// create an order for $10,
		OrderWS order = new OrderWS();
		order.setUserId(user.getUserId());
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(new Integer(1));
		order.setCurrencyId(new Integer(1));
		order.setActiveSince(new Date());
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2008, 9, 3);

		OrderLineWS lines[] = new OrderLineWS[1];
		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setDescription("Fist line");
		line.setItemId(new Integer(1));
		lines[0] = line;

		order.setOrderLines(lines);
		Integer orderId = api.createOrder(order, OrderChangeBL.buildFromOrder(
				order, ORDER_CHANGE_STATUS_APPLY_ID)); // should emit a
														// NewOrderEvent that
														// will be handled by
														// the
														// DynamicBalanceManagerTask
		// where the user's dynamic balance will be updated to reflect the
		// charges

		// user's balance should be 0 - 10 + 25 = 15 (initial balance, minus
		// order, plus auto-recharge).
		UserWS updated = api.getUserWS(user.getUserId());
		assertEquals("balance updated with auto-recharge payment",
				new BigDecimal("15.00"), updated.getDynamicBalanceAsDecimal());

		// cleanup
		api.deleteOrder(orderId);
		api.deleteUser(user.getUserId());
	}

	@Test
	public void test018UpdateCurrentOrderNewQuantityEvents() throws IOException, JbillingAPIException {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        // create user
        UserWS user = null;
		try {
			user = createUser(true, null, null);
			Integer userId = user.getUserId();

			// update to credit limit
			user.setCreditLimit(new BigDecimal("1000.0"));
			user.setMainSubscription(createUserMainSubscription());
            user.setPassword(null);
			api.updateUser(user);

            //TODO: Create the item to add to this order
			// add 10 coffees to current order
			OrderLineWS newLine = new OrderLineWS();
			newLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
			newLine.setItemId(new Integer(3));
			newLine.setQuantity(new BigDecimal("10.0"));
			// take the price and description from the item
			newLine.setUseItem(new Boolean(true));

			// update the current order
			OrderWS currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// check dynamic balance increased (credit limit type)
			user = api.getUserWS(userId);
			assertEquals("dynamic balance", new BigDecimal("-150.0"),
                    user.getDynamicBalanceAsDecimal());

			// add another 10 coffees to current order
			currentOrderAfter = api.updateCurrentOrder(userId,
					new OrderLineWS[] { newLine }, null, new Date(),
					"Event from WS");

			// check dynamic balance increased (credit limit type)
			user = api.getUserWS(userId);
			assertEquals("dynamic balance", new BigDecimal("-300.0"),
					user.getDynamicBalanceAsDecimal());

			// update current order using pricing fields
			PricingField duration = new PricingField("duration", 5); // 5 min
			PricingField disposition = new PricingField("disposition",
					"ANSWERED");
			PricingField dst = new PricingField("dst", "12345678");
			currentOrderAfter = api.updateCurrentOrder(userId, null,
                    new PricingField[]{duration, disposition, dst},
                    new Date(), "Event from WS");

			// check dynamic balance increased (credit limit type)
			// 300 + (5 minutes * 5.0 price)
			user = api.getUserWS(userId);
			assertEquals("dynamic balance", new BigDecimal("-325.0"),
                    user.getDynamicBalanceAsDecimal());

			// clean up
			api.deleteUser(userId);
		} catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            // clean up
            if (user != null) api.deleteUser(user.getId());
        }
    }

	@Test
	public void test019UserACHCreation() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS newUser = null;
        try {
            newUser = new UserWS();
            newUser.setUserName("testUserName-"
                    + Calendar.getInstance().getTimeInMillis());
            newUser.setPassword("P@ssword1");
            newUser.setLanguageId(new Integer(1));
            newUser.setMainRoleId(new Integer(5));
            newUser.setAccountTypeId(Integer.valueOf(1));
            newUser.setParentId(null);
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setCurrencyId(null);

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

            // add a credit card
            Calendar expiry = Calendar.getInstance();
            expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
            PaymentInformationWS cc = createCreditCard("Frodo Baggins", "4111111111111152", expiry.getTime());
            newUser.getPaymentInstruments().add(cc);

            PaymentInformationWS ach = createACH("Frodo Baggins", "Shire Financial Bank", "123456789", "123456789", Integer.valueOf(1));
            newUser.getPaymentInstruments().add(ach);

            logger.debug("Creating user with ACH record...");
            newUser.setUserId(api.createUser(newUser));

            UserWS saved = api.getUserWS(newUser.getUserId());
            List<PaymentInformationWS> achs = getAch(saved.getPaymentInstruments());
            ach = achs.size() > 0 ? achs.iterator().next() : null;

            assertNotNull("Returned UserWS should not be null", saved);
            assertNotNull("Returned ACH record should not be null", ach);
            assertEquals("ABA Routing field does not match", "123456789", new String(getMetaField(ach.getMetaFields(), ACH_MF_ROUTING_NUMBER).getCharValue()));
            assertEquals("Account Name field does not match", "Frodo Baggins",
                    getMetaField(ach.getMetaFields(), ACH_MF_CUSTOMER_NAME).getStringValue());
            Integer accountTypeId = getMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_TYPE).getStringValue().equalsIgnoreCase(Constants.ACH_CHECKING) ?
                    Integer.valueOf(1) : Integer.valueOf(2);
            assertEquals("Account Type field does not match", Integer.valueOf(1),
                    accountTypeId);
            assertEquals("Bank Account field does not match", "123456789", new String(getMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_NUMBER).getCharValue()));
            assertEquals("Bank Name field does not match", "Shire Financial Bank",
                    getMetaField(ach.getMetaFields(), ACH_MF_BANK_NAME).getStringValue());

            logger.debug("Passed ACH record creation test");

            updateMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_NUMBER, ("987654321").toCharArray());

            saved.setPassword(null);
            api.updateUser(saved);

            saved = api.getUserWS(newUser.getUserId());
            ach = getAch(saved.getPaymentInstruments()).iterator().next();
            assertNotNull("Returned UserWS should not be null", saved);
            assertNotNull("Returned ACH record should not be null", ach);
            assertEquals("Bank Account field does not match", "987654321", new String(getMetaField(ach.getMetaFields(), ACH_MF_ACCOUNT_NUMBER).getCharValue()));

            logger.debug("Passed ACH record update test");
// #6315 - credit card and ach payment methods removed.
//		assertNull("Auto payment should be null",
//				api.getAutoPaymentType(newUser.getUserId()));
//
//		api.setAutoPaymentType(newUser.getUserId(),
//				Constants.AUTO_PAYMENT_TYPE_ACH, true);
//
//		assertNotNull("Auto payment should not be null",
//				api.getAutoPaymentType(newUser.getUserId()));
//		assertEquals("Auto payment type should be set to ACH",
//				Constants.AUTO_PAYMENT_TYPE_ACH,
//				api.getAutoPaymentType(newUser.getUserId()));
        }
        finally {
            if (newUser != null) api.deleteUser(newUser.getUserId());
        }

	}

	@Test
	public void test020UpdateInvoiceChild() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        try {
            logger.debug("Parent user parent(43)");
            user = createUser(true, 43, null);
            // userId
            Integer userId = user.getUserId();

            boolean flag = user.getInvoiceChild();
            // set the field
            user.setInvoiceChild(!user.getInvoiceChild());

            // Save
            user.setPassword(null);
            api.updateUser(user);

            // get user again
            user = api.getUserWS(userId);
            assertEquals("Successfully updated invoiceChild: ", new Boolean(!flag),
                    user.getInvoiceChild());

            logger.debug("Testing {} equals {}", !flag, user.getInvoiceChild());

            // cleanup
        }
        finally {
            if (user != null) api.deleteUser(user.getUserId());
        }
	}

	@Test
	public void test021UserExists() throws Exception {
        UserWS userCreated = null;
        JbillingAPI api = JbillingAPIFactory.getAPI();
        try {
            userCreated = createUser(true, null, null);

            // by user name
            assertFalse(api.userExistsWithName("USER_THAT_DOESNT_EXIST"));
            assertTrue(api.userExistsWithName(userCreated.getUserName()));

            // by id
            assertFalse(api.userExistsWithId(Integer.MAX_VALUE));
            assertTrue(api.userExistsWithId(userCreated.getId()));
        }
        finally {
            if (userCreated != null) api.deleteUser(userCreated.getId());
        }
	}

	@Test
	public void test022GetUserByEmail() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS userCreated = null;
        try {

            PreferenceWS uniqueEmailPref = api
                    .getPreference(Constants.PREFERENCE_FORCE_UNIQUE_EMAILS);
            uniqueEmailPref.setValue("0");
            api.updatePreference(uniqueEmailPref);

            userCreated = createUser(true, null, null);
            try {
                logger.debug("Getting valid user by email");
                Integer userId = api.getUserIdByEmail(userCreated.getUserName() + "@shire.com");
                fail("Shouldn't be able to access user by email");
            } catch (Exception e) {
            	e.printStackTrace();
            }

            uniqueEmailPref = api
                    .getPreference(Constants.PREFERENCE_FORCE_UNIQUE_EMAILS);
            uniqueEmailPref.setValue("1");
            api.updatePreference(uniqueEmailPref);

            try {

                logger.debug("Getting valid user by email");
                Integer userId = api.getUserIdByEmail(userCreated.getUserName() + "@shire.com");
                assertEquals("Returned user with ID", new Integer(userCreated.getId()), userId);
            } catch (Exception e) {
            	e.printStackTrace();
                fail("Shouldn't be able to access user by email");
            }

            // return the preference to it's original state
            uniqueEmailPref.setValue("0");
            api.updatePreference(uniqueEmailPref);
        }
        finally {
            if (userCreated != null) api.deleteUser(userCreated.getId());
        }


	}

	@Test
	public void test024CreditCardNumberFormat() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        UserWS parentCreated = null;

        try {
            parentCreated = createParent(api);
            user = createUser(true, 43, null);
            user = api.getUserWS(user.getUserId());

            PaymentInformationWS card = user.getPaymentInstruments().iterator()
                    .next();
            updateMetaField(card.getMetaFields(), CC_MF_NUMBER, ("&&&&&").toCharArray());
            // fetch card after each update to ensure that we're
            // always updating the most recent credit card
            // Visa
            try {
                user.setPassword(null);
                api.updateUser(user);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e,
                        "MetaFieldValue,value,Payment card number is not valid");
            }

            String cardType = getMetaField(
                    user.getPaymentInstruments().iterator().next().getMetaFields(),
                    CC_MF_TYPE).getStringValue();
            assertEquals("card type Visa", CreditCardType.VISA.toString(), cardType);
        } catch (Exception e) { throw  e; }
        finally {
            // cleanup
            if (parentCreated != null) api.deleteUser(parentCreated.getId());
            if (user != null) api.deleteUser(user.getUserId());
        }

	}

	@Test
    public void test025UserCodeCreate() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        try {
            user = createUser(true, true, null, null, true);

            UserCodeWS uc = new UserCodeWS();
            uc.setIdentifier(user.getUserName() + "0002");
            uc.setTypeDescription("ProgramDesc");
            uc.setType("ProgramType");
            uc.setExternalReference("translationId");
            uc.setValidFrom(new Date());
            uc.setUserId(user.getUserId());

            try {
                api.createUserCode(uc);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,validation.identifier.pattern.fail");
            }

            uc.setIdentifier(user.getUserName() + "00002");
            uc.setId(1);
            try {
                api.createUserCode(uc);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,id,validation.error.max,0");
            }

            uc.setId(0);
            uc.setId(api.createUserCode(uc));

            UserCodeWS uc2 = new UserCodeWS();
            uc2.setIdentifier(user.getUserName() + "00002");
            uc2.setTypeDescription("ProgramDesc");
            uc2.setType("ProgramType");
            uc2.setExternalReference("translationId");
            uc2.setValidFrom(new Date());
            uc2.setUserId(user.getUserId());
            try {
                api.createUserCode(uc2);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,userCode.validation.duplicate.identifier");
            }
        }
        finally {
            if (user != null) api.deleteUser(user.getUserId());
        }


    }

    @Test
    public void test026UserCodeUpdate() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        Integer userCode = null;
        Integer userCode2 = null;

        try {
            user = createUser(true, true, null, null, true);

            UserCodeWS uc = new UserCodeWS();
            uc.setIdentifier(user.getUserName() + "00002");
            uc.setTypeDescription("ProgramDesc");
            uc.setType("ProgramType");
            uc.setExternalReference("translationId");
            uc.setValidFrom(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24* 3));
            uc.setValidTo(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24* 2));
            uc.setUserId(user.getUserId());

            try {
                userCode = api.createUserCode(uc);
                uc.setId(userCode);
            } catch (SessionInternalError e) {
                logger.error("Error creating User Code", e);
                fail();
            }


            try {
                uc.setType("Another type");
                api.updateUserCode(uc);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,userCode.validation.update.expired");
            } catch (Exception e) {
                logger.error("Testing UserCodeWS,identifier,userCode.validation.update.expired", e);
                e.printStackTrace();
                throw e;
            }


            UserCodeWS uc2 = new UserCodeWS();
            uc2.setIdentifier(user.getUserName() + "00003");
            uc2.setTypeDescription("ProgramDesc");
            uc2.setType("ProgramType");
            uc2.setExternalReference("translationId");
            uc2.setValidFrom(new Date());
            uc2.setUserId(user.getUserId());
            userCode2 = api.createUserCode(uc2);
            uc2.setId(userCode2);

            try {
                uc2.setIdentifier(user.getUserName() + "00002");
                api.updateUserCode(uc2);
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserCodeWS,identifier,userCode.validation.duplicate.identifier");
            } catch (Exception e) {
                logger.error("Testing UserCodeWS,identifier,userCode.validation.duplicate.identifier", e);
                e.printStackTrace();
                throw e;
            }
        }
        finally {
            if (user != null) api.deleteUser(user.getId());
        }
    }

    @Test
    public void test027UserCodeLinks() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS user = null;
        UserWS user2 = null;

        try {
            user = createUser(true, true, null, null, true);
            String uc1 = user.getUserName() + "00002";
            String uc2 = user.getUserName() + "00003";

            UserCodeWS uc = new UserCodeWS();
            uc.setIdentifier(uc1);
            uc.setTypeDescription("ProgramDesc");
            uc.setType("ProgramType");
            uc.setExternalReference("translationId");
            uc.setValidFrom(new Date());
            uc.setUserId(user.getUserId());
            api.createUserCode(uc);

            uc.setIdentifier(uc2);
            api.createUserCode(uc);

            user2 = createUser(true, true, null, null, false);
            user2.setEntityId(ROOT_ENTITY_ID);
            user2.setUserCodeLink("aaaa");

            try {
                user2.setId(api.createUser(user2));
            } catch (SessionInternalError e) {
                Asserts.assertContainsError(e, "UserWS,linkedUserCodes,validation.error.userCode.not.exist,aaaa");
            }

            user2.setUserCodeLink(uc1);
            user2.setId(api.createUser(user2));

            user2 = api.getUserWS(user2.getId());

            Integer[] ids = api.getCustomersLinkedToUser(user.getUserId());
            assertEquals(1, ids.length);
            assertEquals(user2.getCustomerId().intValue(), ids[0].intValue());

            ids = api.getCustomersByUserCode(uc1);
            assertEquals(1, ids.length);
            assertEquals(user2.getCustomerId().intValue(), ids[0].intValue());

            user2 = api.getUserWS(user2.getId());
            assertEquals(uc1, user2.getUserCodeLink());

            user2.setUserCodeLink(uc2);
            api.updateUser(user2);

            user2 = api.getUserWS(user2.getId());
            assertEquals(uc2, user2.getUserCodeLink());
        }
        finally {
            if (user != null) api.deleteUser(user.getUserId());
            if (user2 != null) api.deleteUser(user2.getUserId());
        }
    }


    @Test
    public void test028ParentChildBillingCycleTest() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS childUser = null;
        UserWS childUser2 = null;

        try {
            parentUser = createUser(true, null, null);
	        parentUser.setPassword(null);
            parentUser.setIsParent(true);
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            parentUser = api.getUserWS(parentUser.getUserId());

            childUser = createUser(true, parentUser.getUserId(), null);
	        childUser.setPassword(null);
            childUser.setInvoiceChild(false);
            childUser.setNextInvoiceDate(parentUser.getNextInvoiceDate());
            childUser.setPassword(null);
            api.updateUser(childUser);

            // Scenario 1 - While creating/editing sub-account, if 'Invoice if Child' is unchecked, then billing cycle,
            // invoice generation day and next invoice date fields of the child account cannot be different than parent account.
            parentUser = api.getUserWS(parentUser.getUserId());
            Date nextInvoiceDateOfParent =  parentUser.getNextInvoiceDate();
            Integer invoiceGenerationDayOfParent = parentUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            childUser = api.getUserWS(childUser.getUserId());
            Date nextInvoiceDateOfChild =  childUser.getNextInvoiceDate();
            Integer invoiceGenerationDayOfChild = childUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            assertEquals("Both next invoice day generated should be equal", nextInvoiceDateOfChild, nextInvoiceDateOfParent);
            assertEquals("Both invoice generation day should be equal", invoiceGenerationDayOfChild,invoiceGenerationDayOfParent );


            // Scenario 2 - While creating/editing sub-account, if 'Invoice if Child' is checked, then billing cycle,
            // invoice generation day and next invoice date fields of the child account can be different than parent account.

            childUser = api.getUserWS(childUser.getUserId());
	        childUser.setPassword(null);
            childUser.setInvoiceChild(true);
            MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
            mainSubscription.setPeriodId(2); //monthly
            mainSubscription.setNextInvoiceDayOfPeriod(27); // 27th of the month
            childUser.setMainSubscription(mainSubscription);
            childUser.setPassword(null);
            api.updateUser(childUser);
            childUser = api.getUserWS(childUser.getUserId());
            Calendar childNextInvoiceDate = Calendar.getInstance();
            childNextInvoiceDate.setTime(new Date());
            childNextInvoiceDate.set(Calendar.DAY_OF_MONTH, mainSubscription.getNextInvoiceDayOfPeriod());
            childUser.setNextInvoiceDate(childNextInvoiceDate.getTime());
            api.updateUser(childUser);
            childUser = api.getUserWS(childUser.getUserId());
            nextInvoiceDateOfChild =  childUser.getNextInvoiceDate();
            invoiceGenerationDayOfChild = childUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            logger.debug("nextInvoiceDateOfChild != nextInvoiceDateOfParent ::::::::::::{} NOT EQUAL {}", nextInvoiceDateOfChild, nextInvoiceDateOfParent);
            logger.debug("invoiceGenerationDayOfChild != invoiceGenerationDayOfParent ::::::::::::{} NOT EQUAL {}", invoiceGenerationDayOfChild, invoiceGenerationDayOfParent);

            assertEquals(nextInvoiceDateOfChild.compareTo(nextInvoiceDateOfParent),1 );
            assertEquals( invoiceGenerationDayOfChild.compareTo(invoiceGenerationDayOfParent),1 );

            //Scenario 3. When a parent account is edited and updated for billing cycle fields,
            //post update it should also update the billing fields of all its sub accounts with 'invoice if child' flag unchecked.

            GregorianCalendar cal = new GregorianCalendar();
            cal.clear();
            cal.set(2010, GregorianCalendar.JANUARY, 01, 0, 0, 0);

            childUser2 = createUser(true, parentUser.getUserId(), null);
	        childUser2.setPassword(null);
            childUser2.setInvoiceChild(false);
            childUser2.setNextInvoiceDate(parentUser.getNextInvoiceDate());

            childUser2.setPassword(null);
            api.updateUser(childUser2);

            parentUser = api.getUserWS(parentUser.getUserId());
	        parentUser.setPassword(null);
            MainSubscriptionWS mainSubscription1 = new MainSubscriptionWS();
            mainSubscription1.setPeriodId(3); //weekly
            mainSubscription1.setNextInvoiceDayOfPeriod(1); //Monday

            parentUser.setNextInvoiceDate(cal.getTime());
            parentUser.setMainSubscription(mainSubscription1);
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            nextInvoiceDateOfParent =  parentUser.getNextInvoiceDate();
            invoiceGenerationDayOfParent = parentUser.getMainSubscription().getNextInvoiceDayOfPeriod();

            logger.debug("nextInvoiceDateOfChild != nextInvoiceDateOfParent ::::::::::::{} NOT EQUAL {}", nextInvoiceDateOfChild, nextInvoiceDateOfParent);
            logger.debug("invoiceGenerationDayOfChild != invoiceGenerationDayOfParent ::::::::::::{} NOT EQUAL {}", invoiceGenerationDayOfChild, invoiceGenerationDayOfParent);
            //For Child(Invoice id child = true) and parent
            assertEquals(nextInvoiceDateOfChild.compareTo(nextInvoiceDateOfParent),1 );
            assertEquals( invoiceGenerationDayOfChild.compareTo(invoiceGenerationDayOfParent),1 );

            childUser2 = api.getUserWS(childUser2.getUserId());
            Date nextInvoiceDateOfChild1 =  childUser2.getNextInvoiceDate();
            Integer invoiceGenerationDayOfChild1 = childUser2.getMainSubscription().getNextInvoiceDayOfPeriod();

            logger.debug("nextInvoiceDateOfChild1 == nextInvoiceDateOfParent ::::::::::::{} EQUAL {}", nextInvoiceDateOfChild1, nextInvoiceDateOfParent);
            logger.debug("invoiceGenerationDayOfChild1 != invoiceGenerationDayOfParent ::::::::::::{} EQUAL {}", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            //For Child(Invoice id child = false) and parent
            assertEquals(nextInvoiceDateOfChild1.compareTo(nextInvoiceDateOfParent),0 );
            assertEquals( invoiceGenerationDayOfChild1.compareTo(invoiceGenerationDayOfParent),0 );

            // Scenario 4. When a parent account is edited and updated for billing cycle fields,
            // post update it should NOT update the billing fields of all its sub accounts with 'invoice if child' flag CHECKED.

            cal.clear();
            cal.set(2010, GregorianCalendar.JANUARY, 02, 0, 0, 0);
            parentUser = api.getUserWS(parentUser.getUserId());
	        parentUser.setPassword(null);
            MainSubscriptionWS mainSubscription2 = new MainSubscriptionWS();
            mainSubscription2.setPeriodId(3); //Weekly
            mainSubscription2.setNextInvoiceDayOfPeriod(2); //Tuesday
            parentUser.setMainSubscription(mainSubscription2);
            parentUser.setNextInvoiceDate(cal.getTime());
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            parentUser = api.getUserWS(parentUser.getUserId());
            Integer parentBillingPeriodId = parentUser.getMainSubscription().getPeriodId();
            childUser = api.getUserWS(childUser.getUserId());
            Integer childBillingPeriodId = childUser.getMainSubscription().getPeriodId();

            logger.debug("parentBillingPeriodId != childBillingPeriodId ::::::::::::{} NOT EQUAL {}", parentBillingPeriodId, childBillingPeriodId);
            assertEquals( parentBillingPeriodId.compareTo(childBillingPeriodId),1 );
        }
        finally {
            if (childUser != null) api.deleteUser(childUser.getId());
            if (childUser2 != null) api.deleteUser(childUser2.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

    @Test
    public void test029ParentChildBillingCycleValidationTest() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS childUser = null;
        int customerUpdateFailed = 0;
        try{
            //User created
            parentUser = createUser(true, null, null);
            parentUser.setIsParent(true);
            parentUser.setNextInvoiceDate(new DateMidnight(2010, 9, 1).toDate());
            parentUser.setPassword(null);
            api.updateUser(parentUser);

            childUser = createUser(true, parentUser.getUserId(), null);

            childUser.setInvoiceChild(false);

            childUser = api.getUserWS(childUser.getUserId());
            childUser.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());

            childUser.setPassword(null);
            api.updateUser(childUser);
            Integer childUserId = childUser.getUserId();
            logger.debug("childUserId: {} ", childUserId);
        }catch(SessionInternalError ex){
            logger.error("User failed", ex);
        } finally {
            if (childUser != null) api.deleteUser(childUser.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

    @Test
    public void test030ParentChildBillingCycleCheckTest() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS parentUser = null;
        UserWS childUser1 = null;
        UserWS childUser2 = null;
        UserWS childUser3 = null;
        try {
            parentUser = createUser(true, null, null);
            parentUser.setIsParent(true);
            parentUser.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(parentUser);

            childUser1 = createUser(true, parentUser.getUserId(), null);
            childUser1.setInvoiceChild(false);
            childUser1.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(childUser1);
            childUser1 = api.getUserWS(childUser1.getUserId());

            childUser2 = createUser(true, parentUser.getUserId(), null);
            childUser2.setInvoiceChild(false);
            childUser2.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(childUser2);
            childUser2 = api.getUserWS(childUser2.getUserId());

            childUser3 = createUser(true, parentUser.getUserId(), null);
            childUser3.setInvoiceChild(false);
            childUser3.setNextInvoiceDate(new DateMidnight(2010, 1, 1).toDate());
            api.updateUser(childUser3);
            childUser3 = api.getUserWS(childUser3.getUserId());

            parentUser = api.getUserWS(parentUser.getUserId());

            // change billing cycle of parent
            MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
            mainSubscription.setPeriodId(2); //monthly
            mainSubscription.setNextInvoiceDayOfPeriod(10);
            parentUser.setMainSubscription(mainSubscription);
            parentUser.setNextInvoiceDate(new DateMidnight(2010, 1, 10).toDate());// billing cycle monthly 10
            api.updateUser(parentUser);

            parentUser = api.getUserWS(parentUser.getUserId());

            Date nextInvoiceDateOfParent = parentUser.getNextInvoiceDate();

            nextInvoiceDateOfParent = parentUser.getNextInvoiceDate();

            childUser1 = api.getUserWS(childUser1.getUserId());
            childUser2 = api.getUserWS(childUser2.getUserId());
            childUser3 = api.getUserWS(childUser3.getUserId());

            Date nextInvoiceDateOfChild1 =  childUser1.getNextInvoiceDate();
            Date nextInvoiceDateOfChild2 =  childUser2.getNextInvoiceDate();
            Date nextInvoiceDateOfChild3 =  childUser3.getNextInvoiceDate();

            Integer invoiceGenerationDayOfParent = parentUser.getMainSubscription().getNextInvoiceDayOfPeriod();
            Integer invoiceGenerationDayOfChild1 = childUser1.getMainSubscription().getNextInvoiceDayOfPeriod();
            Integer invoiceGenerationDayOfChild2 = childUser2.getMainSubscription().getNextInvoiceDayOfPeriod();
            Integer invoiceGenerationDayOfChild3 = childUser3.getMainSubscription().getNextInvoiceDayOfPeriod();

            Integer periodIdOfParent = parentUser.getMainSubscription().getPeriodId();
            Integer periodIdOfChild1 = childUser1.getMainSubscription().getPeriodId();
            Integer periodIdOfChild2 = childUser2.getMainSubscription().getPeriodId();
            Integer periodIdOfChild3 = childUser3.getMainSubscription().getPeriodId();

            assertEquals("Both PeriodId of child1 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both PeriodId of child2 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both PeriodId of child3 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);

            assertEquals("Both next invoice day of child1 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both next invoice day of child2 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);
            assertEquals("Both next invoice day of child3 and parent should be equal", invoiceGenerationDayOfChild1, invoiceGenerationDayOfParent);

            assertEquals("Both next invoice date of child1 and parent should be equal", nextInvoiceDateOfChild1, nextInvoiceDateOfParent);
            assertEquals("Both next invoice date child2 and parent should be equal", nextInvoiceDateOfChild2, nextInvoiceDateOfParent);
            assertEquals("Both next invoice date child3 and parent should be equal", nextInvoiceDateOfChild3, nextInvoiceDateOfParent);
        }
        finally {
            if (childUser1 != null) api.deleteUser(childUser1.getId());
            if (childUser2 != null) api.deleteUser(childUser2.getId());
            if (childUser3 != null) api.deleteUser(childUser3.getId());
            if (parentUser != null) api.deleteUser(parentUser.getId());
        }
    }

    @Test
    public void test031UserNIDCheck() throws Exception {
        UserWS monthlyUser = null;
        UserWS semiMonthlyUser = null;
        UserWS weeklyUser = null;
        UserWS yearlyUser = null;
        UserWS dailyUser = null;
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer weekly = getOrCreateOrderPeriod(api, PeriodUnitDTO.WEEK);
        Integer semiMonthly = getOrCreateOrderPeriod(api, PeriodUnitDTO.SEMI_MONTHLY);
        Integer monthly = getOrCreateOrderPeriod(api, PeriodUnitDTO.MONTH);
        Integer yearly = getOrCreateOrderPeriod(api, PeriodUnitDTO.YEAR);
        Integer daily = getOrCreateOrderPeriod(api, PeriodUnitDTO.DAY);

        Calendar calendarMonthlyNID = Calendar.getInstance();
        Calendar calendarWeeklyNID = Calendar.getInstance();
        Calendar calendarSemiMonthlyNID = Calendar.getInstance();
        Calendar calendarDailyNID = Calendar.getInstance();
        Calendar calendarYearlyNID = Calendar.getInstance();
        Calendar billingDate = Calendar.getInstance();
        billingDate.add(Calendar.DAY_OF_MONTH, 7);

        MainSubscriptionWS mainSubscriptionWeekly = new MainSubscriptionWS();
        mainSubscriptionWeekly.setPeriodId(weekly);
        mainSubscriptionWeekly.setNextInvoiceDayOfPeriod(Calendar.TUESDAY);
        MainSubscriptionWS mainSubscriptionMonthly = new MainSubscriptionWS();
        mainSubscriptionMonthly.setPeriodId(monthly);
        mainSubscriptionMonthly.setNextInvoiceDayOfPeriod(calendarMonthlyNID.get(Calendar.DAY_OF_MONTH));
        MainSubscriptionWS mainSubscriptionSemiMonthly = new MainSubscriptionWS();
        mainSubscriptionSemiMonthly.setPeriodId(semiMonthly);
        //In JBFC 804 development we need to call updateCustomerNextInvoiceDate method to set customers NID for next period to generate the invoice
        //but for semi-monthly scenario, It fails for end of the month condition. If month contain more than 30days.
        //this is failing only for assert expected value calculation, no impact on actual implementation of semi-monthly.
        //It's working as expected from UI and API.
        if(29 < calendarSemiMonthlyNID.get(Calendar.DAY_OF_MONTH)){
            calendarSemiMonthlyNID.set(Calendar.DAY_OF_MONTH, 29);
        }
        mainSubscriptionSemiMonthly.setNextInvoiceDayOfPeriod(calendarSemiMonthlyNID.get(Calendar.DAY_OF_MONTH));
        MainSubscriptionWS mainSubscriptionDaily= new MainSubscriptionWS();
        mainSubscriptionDaily.setPeriodId(daily);
        mainSubscriptionDaily.setNextInvoiceDayOfPeriod(calendarDailyNID.get(Calendar.DAY_OF_MONTH));
        MainSubscriptionWS mainSubscriptionYearly = new MainSubscriptionWS();
        mainSubscriptionYearly.setPeriodId(yearly);
        mainSubscriptionYearly.setNextInvoiceDayOfPeriod(calendarYearlyNID.get(Calendar.DAY_OF_YEAR));
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        Date originalNRD = config.getNextRunDate();
        config.setNextRunDate(billingDate.getTime());
        api.createUpdateBillingProcessConfiguration(config);
        logger.debug(" /// subscriptions ///");
        logger.debug("Monthly: {}",  mainSubscriptionMonthly.getPeriodId());
        logger.debug("Semi-Monthly: {}", mainSubscriptionSemiMonthly.getPeriodId());
        logger.debug("Weekly: {}", mainSubscriptionWeekly.getPeriodId());
        logger.debug("Daily: {}", mainSubscriptionDaily.getPeriodId());
        logger.debug("Yearly: {}", mainSubscriptionYearly.getPeriodId());

        try {
            //create Monthly user
            config.setPeriodUnitId(PeriodUnitDTO.MONTH);
            api.createUpdateBillingProcessConfiguration(config);
			logger.debug("BillingDateConfig: {}", config.getNextRunDate());
            monthlyUser = createUser(true, true, null, null, true, mainSubscriptionMonthly);
            // updating next Invoice date of user
            monthlyUser = updateCustomerNextInvoiceDate(monthlyUser.getId(), PeriodUnitDTO.MONTH);
			while (Util.truncateDate(config.getNextRunDate()).after(Util.truncateDate(
                    calendarMonthlyNID.getTime()))) {
                calendarMonthlyNID.add(Calendar.MONTH, 1);
            }

            logger.debug(" /// MONTHLY ///");
            logger.debug("Billing Date: {}", config.getNextRunDate());
            logger.debug("Actual User NID: {}", monthlyUser.getNextInvoiceDate());
            logger.debug("Expected User NID: {}", calendarMonthlyNID.getTime());

            assertTrue(api.userExistsWithId(monthlyUser.getId()));
            assertEquals(Util.truncateDate(calendarMonthlyNID.getTime()),
                    Util.truncateDate(monthlyUser.getNextInvoiceDate()));

            //create Weekly user
            weeklyUser = createUser(true, true, null, null, true, mainSubscriptionWeekly);
            // updating next Invoice date of user
            weeklyUser = updateCustomerNextInvoiceDate(weeklyUser.getId(), PeriodUnitDTO.WEEK);
            config.setPeriodUnitId(PeriodUnitDTO.WEEK);
            api.createUpdateBillingProcessConfiguration(config);
            calendarWeeklyNID.set(Calendar.DAY_OF_WEEK, mainSubscriptionWeekly.getNextInvoiceDayOfPeriod());
            int dayOfWeek = calendarWeeklyNID.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek > mainSubscriptionWeekly.getNextInvoiceDayOfPeriod()) {
                int day = 7 - (dayOfWeek - mainSubscriptionWeekly.getNextInvoiceDayOfPeriod());
                calendarWeeklyNID.add(Calendar.DAY_OF_MONTH, day);
            } else if (dayOfWeek < mainSubscriptionWeekly.getNextInvoiceDayOfPeriod()) {
                int day =  mainSubscriptionWeekly.getNextInvoiceDayOfPeriod() - dayOfWeek;
                calendarWeeklyNID.add(Calendar.DAY_OF_MONTH, day);
            }

            while (Util.truncateDate(config.getNextRunDate()).after(Util.truncateDate(calendarWeeklyNID.getTime()))) {
				calendarWeeklyNID.add(Calendar.DAY_OF_MONTH, 7);
			}
            
            logger.debug(" /// WEEKLY ///");
            logger.debug("Billing Date: {}", config.getNextRunDate());
            logger.debug("Actual User NID: {}", weeklyUser.getNextInvoiceDate());
            logger.debug("Expected User NID: {}", calendarWeeklyNID.getTime());

            assertTrue(api.userExistsWithId(weeklyUser.getId()));
            assertEquals(Util.truncateDate(calendarWeeklyNID.getTime()),
                    Util.truncateDate(weeklyUser.getNextInvoiceDate()));

            //create Semi-Monthly user
            semiMonthlyUser = createUser(true, true, null, null, true, mainSubscriptionSemiMonthly);
            semiMonthlyUser = updateCustomerNextInvoiceDate(semiMonthlyUser.getId(), PeriodUnitDTO.SEMI_MONTHLY);
            config.setPeriodUnitId(PeriodUnitDTO.SEMI_MONTHLY);
            api.createUpdateBillingProcessConfiguration(config);

			PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(mainSubscriptionSemiMonthly.getNextInvoiceDayOfPeriod(),
																	PeriodUnitDTO.SEMI_MONTHLY);

			LocalDate nextInvoiceDate = DateConvertUtils.asLocalDate(calendarSemiMonthlyNID.getTime());
			LocalDate nextRunDate = DateConvertUtils.asLocalDate(config.getNextRunDate());
			LocalDate initialNextInvoiceDate = DateConvertUtils.asLocalDate(calendarSemiMonthlyNID.getTime());
			nextInvoiceDate = periodUnit.getForDay(nextInvoiceDate, mainSubscriptionSemiMonthly.getNextInvoiceDayOfPeriod());

			while (nextRunDate.isAfter(nextInvoiceDate) || !(nextInvoiceDate.isAfter(initialNextInvoiceDate))) {
				nextInvoiceDate = periodUnit.addTo(nextInvoiceDate, 1);
			}

			calendarSemiMonthlyNID.setTime(DateConvertUtils.asUtilDate(nextInvoiceDate));

            logger.debug(" /// SEMI-MONTHLY ///");
            logger.debug("Billing Date: {}", config.getNextRunDate());
            logger.debug("Actual User NID: {}", semiMonthlyUser.getNextInvoiceDate());
            logger.debug("Expected User NID: {}", calendarSemiMonthlyNID.getTime());

            assertTrue(api.userExistsWithId(semiMonthlyUser.getId()));
            assertEquals(Util.truncateDate(calendarSemiMonthlyNID.getTime()),
                    Util.truncateDate(semiMonthlyUser.getNextInvoiceDate()));

			//create Yearly user
			config.setPeriodUnitId(PeriodUnitDTO.YEAR);
			api.createUpdateBillingProcessConfiguration(config);
			logger.debug("BillingDateConfig: {}", config.getNextRunDate());
            yearlyUser = createUser(true, true, null, null, true, mainSubscriptionYearly);
            yearlyUser = updateCustomerNextInvoiceDate(yearlyUser.getId(), PeriodUnitDTO.YEAR);
            calendarYearlyNID.set(Calendar.DAY_OF_YEAR, mainSubscriptionYearly.getNextInvoiceDayOfPeriod());
			while (Util.truncateDate(config.getNextRunDate()).after(Util.truncateDate(
					calendarYearlyNID.getTime()))) {
                calendarYearlyNID.add(Calendar.YEAR, 1);
			}

			logger.debug(" /// Yearly ///");
			logger.debug("Billing Date: {}", config.getNextRunDate());
			logger.debug("Actual User NID: {}", yearlyUser.getNextInvoiceDate());
			logger.debug("Expected User NID: {}", calendarYearlyNID.getTime());

			assertTrue(api.userExistsWithId(yearlyUser.getId()));
			assertEquals(Util.truncateDate(calendarYearlyNID.getTime()),
					Util.truncateDate(yearlyUser.getNextInvoiceDate()));

            //create Daily user
            config.setPeriodUnitId(PeriodUnitDTO.DAY);
            api.createUpdateBillingProcessConfiguration(config);
            logger.debug("BillingDateConfig: {}", config.getNextRunDate());
            dailyUser = createUser(true, true, null, null, true, mainSubscriptionDaily);
            dailyUser = updateCustomerNextInvoiceDate(dailyUser.getId(), PeriodUnitDTO.DAY);
            calendarDailyNID.set(Calendar.DAY_OF_MONTH, mainSubscriptionDaily.getNextInvoiceDayOfPeriod());
            while (Util.truncateDate(config.getNextRunDate()).after(Util.truncateDate(
                    calendarDailyNID.getTime()))) {
                calendarDailyNID.setTime(config.getNextRunDate());
            }

            logger.debug(" /// Daily ///");
            logger.debug("Billing Date: {}", config.getNextRunDate());
            logger.debug("Actual User NID: {}", dailyUser.getNextInvoiceDate());
            logger.debug("Expected User NID: {}", calendarDailyNID.getTime());

            assertTrue(api.userExistsWithId(dailyUser.getId()));
            assertEquals(Util.truncateDate(calendarDailyNID.getTime()),
                    Util.truncateDate(dailyUser.getNextInvoiceDate()));

		}

        finally {
            if (semiMonthlyUser != null) api.deleteUser(semiMonthlyUser.getId());
            if (monthlyUser != null) api.deleteUser(monthlyUser.getId());
            if (weeklyUser != null) api.deleteUser(weeklyUser.getId());
            if (yearlyUser != null) api.deleteUser(yearlyUser.getId());
            if (dailyUser != null) api.deleteUser(dailyUser.getId());
            config.setNextRunDate(originalNRD);
            config.setPeriodUnitId(PeriodUnitDTO.MONTH);
            api.createUpdateBillingProcessConfiguration(config);
            logger.debug("Config: date {} Period Unit {}",config.getNextRunDate(), config.getPeriodUnitId());

        }
    }

    @Test
	public void test032CreateUserWithInvalidCountryCode() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer newUserId = null;
		//Test create User with invalid country code (alphabet code)
		try {
			UserWS newUser = createUser("us");
			logger.debug("Creating user ...");
			newUserId = api.createUser(newUser);
		} catch (SessionInternalError e) {
            Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.country.code,(.*)"));
		} finally {
			// clean up
			if (newUserId != null) api.deleteUser(newUserId);
		}
	}

    @Test
	public void test033CreateUserWithNumericCountryCode() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer newUserId = null;
		//Test create User with invalid country code (numeric code)
		try {
			UserWS newUser = createUser("21");
			logger.debug("Creating user ...");
			newUserId = api.createUser(newUser);
		} catch (SessionInternalError e) {
            Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.country.code,(.*)"));
		} finally {
			// clean up
			if (newUserId != null) api.deleteUser(newUserId);
		}
	}

    @Test
	public void test034CreateUserWithValidCountryCode() throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		Integer newUserId = null;
		//Test create User with valid country code
		try {
			UserWS newUser = createUser("US");
			logger.debug("Creating user ...");
			newUserId = api.createUser(newUser);
			UserWS user =  api.getUserWS(newUserId);
			MetaFieldValueWS[]  fieldValueWSs = user.getMetaFields();
			for (MetaFieldValueWS metaFieldValueWS : fieldValueWSs) {
				String fieldValue = metaFieldValueWS.getStringValue();
				if (null != fieldValue && fieldValue.equals("contact.country.code")) {
					assertEquals("US",fieldValue);
				}
			}
		} catch (SessionInternalError e) {
			logger.error("Error creating the user", e);
		} finally {
			// clean up
			if (newUserId != null) api.deleteUser(newUserId);
		}
	}

    /**
     * JBFC-581 :
     * This test case is added to cover user creation with special characters
     */
    @Test
	public void test035CreateUserWithAllowedSpecialCharacters() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS validUser = null;
        UserWS invalidUser = null;
        Integer userId = null;
        try {
            //Creating user with valid special characters, which are allowed in user name patterns
			String validLoginName = "testUserNameWith. A-Za-z0-9_@-";
			validUser = createUser(true, true, null, null, false);
			validUser.setUserName(validLoginName);
			try {
				logger.debug("Creating user with valid special characters ...");
				userId = api.createUser(validUser);
				assertNotNull("User was not created with valid special characters in login name", userId);
				validUser.setUserId(userId);
				logger.debug("User was created with valid special characters in login name");
			} catch (SessionInternalError e) {
				e.printStackTrace();
				fail("User creation with valid special characters has failed");
			}

			//Creating user with invalid special characters, which are NOT allowed in user name patterns
			String invalidLoginName = "testUserNameWith#$%&()";
			invalidUser = createUser(true, true, null, null, false);
			invalidUser.setUserName(invalidLoginName);
			userId = null;
			try {
				logger.debug("Creating user with invalid special characters ...");
				userId = api.createUser(invalidUser);
				invalidUser.setUserId(userId);
				assertNull("User was created with invalid special characters in login name", userId);
			} catch (SessionInternalError e) {
				assertNull("The user was created", userId);
				logger.error("User creation with invalid special characters has failed as expected", e);
			}

		} catch (Exception ex) {
            logger.error("Error creating the user", ex);
			fail("Exception caught : " + ex);
		} finally {
			//Data Clean up
            if (validUser != null && validUser.getId() != 0) api.deleteUser(validUser.getId());
            if (invalidUser != null && invalidUser.getId() != 0) api.deleteUser(invalidUser.getId());
        }
    }

    @Test
    public void test036AddPaymentInstrumentLogMessageCorrect() throws Exception {
        UserWS user = null;
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            JBillingLogFileReader jbLog = new JBillingLogFileReader();
            user = createUser(false, true, null, 1, true);

            String callerClass = "class=\"c.sapienter.jbilling.server.user.UserBL\"";
            String apiMethod = "api=\"updateUser\"";

            assertNotNull("There should be one user", user);

            PaymentInformationWS secondCC = createCreditCard("Second ccName", "4012888888881881", new Date());
            secondCC.setProcessingOrder(2);
            user.getPaymentInstruments().add(secondCC);

            jbLog.setWatchPoint();
            api.updateUser(user);
            user = api.getUserWS(user.getId());
            String msg = "Payment instrument with ID: "
                    + user.getPaymentInstruments().get(1).getId()
                    + " successfully added for user "
                    + user.getId(); // Expected log message

            logger.debug("Log Message::::{}", msg);

            String fullLog = jbLog.readLogAsString();
           LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_USER,
                   LogConstants.STATUS_SUCCESS, LogConstants.ACTION_CREATE, msg);

        } catch (IOException io) {
            fail("Exception thrown while trying to read Jbilling log file: " + io.getMessage());
        } catch (Exception e) {
            fail("There was an error: " + e.getMessage());
        } finally {
            // data clean up
            if (user != null && user.getId() > 0) api.deleteUser(user.getId());
        }
    }

    @Test
    public void test037RemovePaymentInstrumentLogMessageCorrect() throws Exception {
        UserWS user = null;
        try {
            JbillingAPI api = JbillingAPIFactory.getAPI();
            JBillingLogFileReader jbLog = new JBillingLogFileReader();
            user = createUser(false, true, null, 1, true);

            String callerClass = "class=\"c.s.j.s.u.WebServicesSessionSpringBean\"";
            String apiMethod = "api=\"removePaymentInstrument\"";

            assertNotNull("There should be one user", user);

            PaymentInformationWS secondCC = createCreditCard("Second ccName", "4012888888881881", new Date());

            user.getPaymentInstruments().add(secondCC);

            api.updateUser(user);
            user = api.getUserWS(user.getId());
            PaymentInformationWS secondPaymentInstrument = user.getPaymentInstruments().get(1);
            assertEquals(user.getPaymentInstruments().size(), 2);

            //Remove the second payment instrument
            jbLog.setWatchPoint();
            api.removePaymentInstrument(user.getPaymentInstruments().get(1).getId());

            String msg = "Payment instrument: " + secondPaymentInstrument.getId() + " has been removed from user: " + user.getId();
            String fullLog = jbLog.readLogAsString();

            LoggingValidator.validateEnhancedLog(fullLog, LEVEL_INFO, callerClass, apiMethod, LogConstants.MODULE_CUSTOMER,
                    LogConstants.STATUS_SUCCESS, LogConstants.ACTION_DELETE, msg);

        } catch (IOException io) {
            fail("Exception thrown while trying to read Jbilling log file: " + io.getMessage());
        } catch (Exception e) {
            fail("There was error: " + e.getMessage());
        } finally {
            // data clean up
            if (user != null && user.getId() > 0) api.deleteUser(user.getId());
        }
    }

    @Test
    public void test038CreateUserWithAutoPaymentLimit() throws Exception {
    	Calendar nextInvoiceDate = Calendar.getInstance();
    	nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 1);
    	logger.debug("Invoice Date:::::::::{}", nextInvoiceDate.getTime());
    	UserWS user = createUser(nextInvoiceDate.getTime(), BigDecimal.TEN);
    	assertNotNull("User was not created ", user.getId());
    	
    	if(Objects.nonNull(user.getPaymentInstruments()) 
    			&& !user.getPaymentInstruments().isEmpty()) {
    		PaymentInformationWS instrument = user.getPaymentInstruments().get(0);
    		Optional<MetaFieldValueWS> autoPaymentLimit = Arrays.stream(instrument.getMetaFields())
    															.filter(metaFieldValue -> metaFieldValue.getFieldName()
    																	.equals(CC_MF_AUTOPAYMENT_LIMIT))
    															.findFirst();
    		
    		assertTrue("Auto Payment Limit MetaField Value Not Found ", autoPaymentLimit.isPresent());
    		assertEquals("Auto Payment Limit Mismatch ", BigDecimal.TEN,autoPaymentLimit.get().getValue());
    		user.setPaymentInstruments(new ArrayList<PaymentInformationWS>());
    		updateUser(user);
    		
    		MetaFieldValueWS[] paymentMetaFieldValues = instrument.getMetaFields();
    		updateMetaField(paymentMetaFieldValues, CC_MF_AUTOPAYMENT_LIMIT, null);
    		instrument.setId(null);
    		instrument.setMetaFields(paymentMetaFieldValues);
    		user.setPaymentInstruments(Arrays.asList(instrument));
    		UserWS updateUser = updateUser(user);
    		
    		Optional<MetaFieldValueWS> blankAutoPaymentLimit = Arrays.stream(updateUser.getPaymentInstruments().get(0).getMetaFields())
    															.filter(metaFieldValue -> metaFieldValue.getFieldName()
    																	.equals(CC_MF_AUTOPAYMENT_LIMIT))
    															.filter(metaFieldValue -> metaFieldValue.getValue() == null)
    															.findFirst();
    		assertTrue("Auto Payment Limit MetaField Value Should Null ", blankAutoPaymentLimit.get().getValue() == null);
    		
        	
    	} else {
    		assertTrue("User Does not have payment Instrument  ", false);
    	}
    	
    }
   	
   	/**
   	 * Test case for testing Create and Update User with CIM Profile validation 
   	 * @throws IOException
   	 * @throws JbillingAPIException
   	 */
	@Test
	public void test039CreateUpdateUserWithCIMProfileValidation() throws IOException, JbillingAPIException {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        JBillingLogFileReader jbLog = new JBillingLogFileReader();

        UserWS newUser = null;

        try {
        	// create our test user
        	newUser = createUpdateUserACHWithCIMProfile(true, true, null, null, false, true);
        	logger.debug("Creating the test user...");
        	newUser = api.createUserWithCIMProfileValidation(newUser);
        	assertNotNull("Return User object should be not null",newUser);
        	assertNull("CIM Profile error should be null",newUser.getCimProfileError());
        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (newUser != null) api.deleteUser(newUser.getId());
        }
    }
	
	/**
   	 * Test case for testing Create and Update User with CIM Profile validation for negative scenario
   	 * @throws IOException
   	 * @throws JbillingAPIException
   	 */
	@Test
	public void test040CreateUpdateUserWithCIMProfileValidation_negative() throws IOException, JbillingAPIException {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        JBillingLogFileReader jbLog = new JBillingLogFileReader();

        UserWS newUser = null;

        try {
        	// create our test user
        	newUser = createUpdateUserACHWithCIMProfile(true, true, null, null, false, false);
        	logger.debug("Creating the test user...");
        	newUser = api.createUserWithCIMProfileValidation(newUser);
        	assertNotNull("Return User object should be not null",newUser);
        	assertNotNull("Should dispaly error if Gateway key is not present in ACH Payment",newUser.getCimProfileError());
        } catch (Exception e) {
			e.printStackTrace();
			fail("Exception caught:" + e);
		} finally {
            if (newUser != null) api.deleteUser(newUser.getId());
        }
    }

    @Test
    public void test041CreateUpdateUserWithAccountTypeCustomizedWithContactWSMetaFields() throws IOException, JbillingAPIException {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        MetaFieldWS[] listMetaFields = new MetaFieldWS[12];

        listMetaFields[0] = createMetaFieldWithBuilder(DataType.STRING, ADDRESS1.name(), ADDRESS1);
        listMetaFields[1] = createMetaFieldWithBuilder(DataType.STRING, ADDRESS2.name(), ADDRESS2);
        listMetaFields[2] = createMetaFieldWithBuilder(DataType.STRING, CITY.name(), CITY);
        listMetaFields[3] = createMetaFieldWithBuilder(DataType.STRING, COUNTRY_CODE.name(), COUNTRY_CODE);
        listMetaFields[4] = createMetaFieldWithBuilder(DataType.STRING, EMAIL.name(), EMAIL);
        listMetaFields[5] = createMetaFieldWithBuilder(DataType.STRING, FIRST_NAME.name(), FIRST_NAME);
        listMetaFields[6] = createMetaFieldWithBuilder(DataType.STRING, INITIAL.name(), INITIAL);
        listMetaFields[7] = createMetaFieldWithBuilder(DataType.STRING, LAST_NAME.name(), LAST_NAME);
        listMetaFields[8] = createMetaFieldWithBuilder(DataType.STRING, ORGANIZATION.name(), ORGANIZATION);
        listMetaFields[9] = createMetaFieldWithBuilder(DataType.STRING, PHONE_NUMBER.name(), PHONE_NUMBER);
        listMetaFields[10] = createMetaFieldWithBuilder(DataType.STRING, POSTAL_CODE.name(), POSTAL_CODE);
        listMetaFields[11] = createMetaFieldWithBuilder(DataType.STRING, STATE_PROVINCE.name(), STATE_PROVINCE);

        AccountTypeWS accountType = new AccountTypeBuilder().create(api);
        Integer ati = api.createAccountInformationType(new AccountInformationTypeBuilder(accountType).addMetaFields(listMetaFields)
                .build());
        logger.debug("ati = {}", ati);
        // create our test user
        UserWS user = null;

        try {
            user = createUpdateUserACHWithCIMProfile(true, true, null, null, false, false);
            user.setAccountTypeId(accountType.getId());
            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[0], randomAlphabetic(101), ati)
            });

            //Testing size of ADDRESS1 MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,100"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[1], randomAlphabetic(101), ati)
            });

            //Testing size of ADDRESS2 MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,100"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[2], randomAlphabetic(51), ati)
            });

            //Testing size of CITY MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,50"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[3], randomAlphabetic(3), ati),
            });

            //Testing size of COUNTRY_CODE MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                logger.debug(":::::::::::::::::::::::::{}", e.getErrorMessages()[0]);
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,2"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[4], randomAlphabetic(5), ati),
            });

            //Testing size of EMAIL MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),6,320"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[5], randomAlphabetic(31), ati),
            });

            //Testing size of FIRST_NAME MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,30"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[6], randomAlphabetic(31), ati),
            });

            //Testing size of INITIAL MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,30"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[7], randomAlphabetic(31), ati),
            });

            //Testing size of LAST_NAME MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,30"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[8], randomAlphabetic(201), ati),
            });

            //Testing size of ORGANIZATION MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,200"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[9], randomAlphabetic(21), ati),
            });

            //Testing size of PHONE_NUMBER MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,20"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[10], randomAlphabetic(16), ati),
            });

            //Testing size of POSTAL_CODE MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,15"));
            }

            user.setMetaFields(new MetaFieldValueWS[]{
                    createMetaFieldValue(listMetaFields[11], randomAlphabetic(31), ati),
            });

            //Testing size of EMAIL MetaFieldType
            try {
                user = api.createUserWithCIMProfileValidation(user);
            } catch (SessionInternalError e) {
                Assert.assertTrue(e.getErrorMessages()[0].matches("MetaFieldValue,value,metafield.validation.error.size,(.*),0,30"));
            }

            user.setMetaFields(null);
            user = api.createUserWithCIMProfileValidation(user);
            assertNotNull("User wasn't created", user);
        } finally {
            if (user != null && user.getId() > 0) api.deleteUser(user.getId());
        }
    }

    @Test
    public void test042UserOwingBalanceWithMultipleFailedPayments() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS testUser = null;
        try {
            logger.debug("Creating user");
            testUser = createUser(false, true, null, 1, true);
            assertNotNull("User creation has failed", testUser);

            logger.debug("Creating one-time order");
            Integer orderId = createOrder(testUser.getId());
            assertNotNull("Order creation has failed", orderId);

            logger.debug("Generating Invoice");
            Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
            assertNotNull("Invoice generation has failed", invoiceId);

            logger.debug("Verifying user balance");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

            //Payment #1
            logger.debug("Creating entered payment with failed result");
            PaymentWS payment1 = createPayment("1.00", false, testUser, Constants.RESULT_FAIL, null);
            Integer payment1Id = api.createPayment(payment1);
            assertNotNull("Payment creation has failed", payment1Id);

            logger.debug("Verifying user balance after creating failed payment");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance after failed payment", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

            //Payment #2
            logger.debug("Creating entered payment with processor unavailable result");
            PaymentWS payment2 = createPayment("1.00", false, testUser, Constants.RESULT_UNAVAILABLE, null);
            Integer payment2Id = api.createPayment(payment2);
            assertNotNull("Payment creation has failed", payment2Id);

            logger.debug("Verifying user balance after creating payment with processor unavailable result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance after failed payment", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

            //Payment #3
            logger.debug("Creating entered payment with billing information not found result");
            PaymentWS payment3 = createPayment("1.00", false, testUser, Constants.RESULT_BILLING_INFORMATION_NOT_FOUND, null);
            Integer payment3Id = api.createPayment(payment3);
            assertNotNull("Payment creation has failed", payment3Id);

            logger.debug("Verifying user balance after creating payment with billing information not found result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance after failed payment", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

        } catch (Exception e) {
            fail("There was error: " + e.getMessage());
        } finally {
            // data clean up
            if (testUser != null) api.deleteUser(testUser.getId());
        }
    }

    @Test
    public void test043UserOwingBalanceWithEnteredPaymentAndMultipleRefundPayments() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS testUser = null;
        try {
            logger.debug("Creating user");
            testUser = createUser(false, true, null, 1, true);
            assertNotNull("User creation has failed", testUser);

            logger.debug("Creating one-time order");
            Integer orderId = createOrder(testUser.getId());
            assertNotNull("Order creation has failed", orderId);

            logger.debug("Generating Invoice");
            Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
            assertNotNull("Invoice generation has failed", invoiceId);

            logger.debug("Verifying user balance");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

            logger.debug("Creating entered payment");
            PaymentWS enteredPayment = createPayment("1.00", false, testUser, Constants.RESULT_ENTERED, null);
            Integer paymentId = api.createPayment(enteredPayment);
            assertNotNull("Payment creation has failed", paymentId);

            logger.debug("Verifying user balance after creating entered payment");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after entered payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            logger.debug("Unlinking payment from invoice");
            api.removePaymentLink(invoiceId, paymentId);

            //Refund payment #1
            logger.debug("Creating refund payment with failed result");
            PaymentWS refundPayment1 = createPayment("1.00", true, testUser, Constants.RESULT_FAIL, paymentId);
            Integer refundPayment1Id = api.createPayment(refundPayment1);
            assertNotNull("Payment creation has failed", refundPayment1Id);

            logger.debug("Verifying user balance after refund failed payment");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after refund failed payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            //Refund payment #2
            logger.debug("Creating refund payment with processor unavailable result");
            PaymentWS refundPayment2 = createPayment("1.00", true, testUser, Constants.RESULT_UNAVAILABLE, paymentId);
            Integer refundPayment2Id = api.createPayment(refundPayment2);
            assertNotNull("Payment creation has failed", refundPayment2Id);

            logger.debug("Verifying user balance after refund payment having processor unavailable result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after refund payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            //Refund payment #3
            logger.debug("Creating refund payment with billing information not found result");
            PaymentWS refundPayment3 = createPayment("1.00", true, testUser, Constants.RESULT_BILLING_INFORMATION_NOT_FOUND, paymentId);
            Integer refundPayment3Id = api.createPayment(refundPayment3);
            assertNotNull("Payment creation has failed", refundPayment3Id);

            logger.debug("Verifying user balance after refund payment having billing information not found result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after refund payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            //Refund payment #4
            logger.debug("Creating refund payment with entered result");
            PaymentWS refundPayment4 = createPayment("1.00", true, testUser, Constants.RESULT_ENTERED, paymentId);
            Integer refundPayment4Id = api.createPayment(refundPayment4);
            assertNotNull("Payment creation has failed", refundPayment4Id);

            logger.debug("Verifying user balance after refund payment having entered result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance after refund payment", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());
        } catch (Exception e) {
            fail("There was error: " + e.getMessage());
        } finally {
            // data clean up
            if (testUser != null) api.deleteUser(testUser.getId());
        }
    }

    @Test
    public void test044UserOwingBalanceWithSuccessfulPaymentAndMultipleRefundPayments() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS testUser = null;
        try {
            logger.debug("Creating user");
            testUser = createUser(false, true, null, 1, true);
            assertNotNull("User creation has failed", testUser);

            logger.debug("Creating one-time order");
            Integer orderId = createOrder(testUser.getId());
            assertNotNull("Order creation has failed", orderId);

            logger.debug("Generating Invoice");
            Integer invoiceId = api.createInvoiceFromOrder(orderId, null);
            assertNotNull("Invoice generation has failed", invoiceId);

            logger.debug("Verifying user balance");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

            logger.debug("Creating successful payment");
            PaymentWS enteredPayment = createPayment("1.00", false, testUser, Constants.RESULT_OK, null);
            Integer paymentId = api.createPayment(enteredPayment);
            assertNotNull("Payment creation has failed", paymentId);

            logger.debug("Verifying user balance after creating successful payment");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after successful payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            logger.debug("Unlinking payment from invoice");
            api.removePaymentLink(invoiceId, paymentId);

            //Refund payment #1
            logger.debug("Creating refund payment with failed result");
            PaymentWS refundPayment1 = createPayment("1.00", true, testUser, Constants.RESULT_FAIL, paymentId);
            Integer refundPayment1Id = api.createPayment(refundPayment1);
            assertNotNull("Payment creation has failed", refundPayment1Id);

            logger.debug("Verifying user balance after refund failed payment");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after refund failed payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            //Refund payment #2
            logger.debug("Creating refund payment with processor unavailable result");
            PaymentWS refundPayment2 = createPayment("1.00", true, testUser, Constants.RESULT_UNAVAILABLE, paymentId);
            Integer refundPayment2Id = api.createPayment(refundPayment2);
            assertNotNull("Payment creation has failed", refundPayment2Id);

            logger.debug("Verifying user balance after refund payment having processor unavailable result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after refund payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            //Refund payment #3
            logger.debug("Creating refund payment with billing information not found result");
            PaymentWS refundPayment3 = createPayment("1.00", true, testUser, Constants.RESULT_BILLING_INFORMATION_NOT_FOUND, paymentId);
            Integer refundPayment3Id = api.createPayment(refundPayment3);
            assertNotNull("Payment creation has failed", refundPayment3Id);

            logger.debug("Verifying user balance after refund payment having billing information not found result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 19 balance after refund payment", BigDecimal.valueOf(19.00), testUser.getOwingBalanceAsDecimal());

            //Refund payment #4
            logger.debug("Creating refund payment with successful result");
            PaymentWS refundPayment4 = createPayment("1.00", true, testUser, Constants.RESULT_OK, paymentId);
            Integer refundPayment4Id = api.createPayment(refundPayment4);
            assertNotNull("Payment creation has failed", refundPayment4Id);

            logger.debug("Verifying user balance after refund payment having successful result");
            testUser = api.getUserWS(testUser.getUserId());
            assertEquals("User should have 20 balance after refund payment", BigDecimal.valueOf(20.00), testUser.getOwingBalanceAsDecimal());

        } catch (Exception e) {
            fail("There was error: " + e.getMessage());
        } finally {
            // data clean up
            if (testUser != null) api.deleteUser(testUser.getId());
        }
    }

    @Test
    public void test045updateContactInformationTest() throws JbillingAPIException, IOException{
        JbillingAPI api = JbillingAPIFactory.getAPI();
        UserWS testUser = null;
        try {
            logger.debug("Creating user");
            testUser = createUser(false, true, null, 1, true);
            assertNotNull("User creation has failed", testUser);
            assertNotNull("Contact email should not be null", getMetaField(testUser.getMetaFields(), CONTACT_EMAIL));
            assertNotNull("Contact first name should not be null", getMetaField(testUser.getMetaFields(), CONTACT_FIRST_NAME));
            assertNotNull("Contact last name not be null", getMetaField(testUser.getMetaFields(), CONTACT_LAST_NAME));

            Map<String, Object> aitMap = new HashMap<>();
            aitMap.put("contact.email", "testUpdateUser@test.com");
            //Test with invalid accountId
            ContactInformationWS contact = new ContactInformationWS();
            contact.setUserId(0);
            contact.setGroupName("Contact");
            contact.setMetaFields(aitMap);

            try {
                api.updateCustomerContactInfo(contact);
                assertFalse(EXCEPTION_MSG, true);
            } catch (SessionInternalError e) {
                assertTrue(EXCEPTION_MSG, true);
            }

            //Test with invalid AIT name
            contact.setUserId(testUser.getId());
            contact.setGroupName("Invalid group name");
            try {
                api.updateCustomerContactInfo(contact);
                assertFalse(EXCEPTION_MSG, true);
            } catch (SessionInternalError e) {
                assertTrue(EXCEPTION_MSG, true);
            }

            //Test with invalid AIT name
            contact.setUserId(testUser.getId());
            contact.setGroupName("Contact");
            contact.setMetaField("Invalid mf Name", "test");

            try {
                api.updateCustomerContactInfo(contact);
                assertFalse(EXCEPTION_MSG, true);
            } catch (SessionInternalError e) {
                assertTrue(EXCEPTION_MSG, true);
            }
            ContactInformationWS newContact = new ContactInformationWS();
            Map<String, Object> newAitMap = new HashMap<>();
            newAitMap.put("contact.email", "testUpdateUser@test.com");
            newAitMap.put("contact.first.name", "TestUpdateUserFName");
            newAitMap.put("contact.last.name", "TestUpdateUserLName");
            newContact.setUserId(testUser.getId());
            newContact.setGroupName("Contact");
            newContact.setMetaFields(newAitMap);

            testUser = api.updateCustomerContactInfo(newContact);
            assertNotNull("Contact email should not be null", getMetaField(testUser.getMetaFields(), CONTACT_EMAIL));
            assertNotNull("Contact first name should not be null", getMetaField(testUser.getMetaFields(), CONTACT_FIRST_NAME));
            assertNotNull("Contact last name not be null", getMetaField(testUser.getMetaFields(), CONTACT_LAST_NAME));

            assertEquals("email should be ", "testUpdateUser@test.com", getMetaField(testUser.getMetaFields(), CONTACT_EMAIL).getStringValue());
            assertEquals("first name should be ", "TestUpdateUserFName", getMetaField(testUser.getMetaFields(), CONTACT_FIRST_NAME).getStringValue());
            assertEquals("last name should be ", "TestUpdateUserLName", getMetaField(testUser.getMetaFields(), CONTACT_LAST_NAME).getStringValue());

        } catch (Exception e) {
            fail("There was error: " + e.getMessage());
        }finally{
            api.deleteUser(testUser.getId());
        }
    }

    public static UserWS createUser(String value){

        UserWS newUser = new UserWS();
        newUser.setUserName("language-test" + new Date().getTime());
        newUser.setLanguageId(new Integer(2)); // French
        newUser.setMainRoleId(new Integer(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setIsParent(new Boolean(true));
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(newUser.getUserName() + "@shire.com");
        metaField1.setGroupId(1);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.country.code");
        metaField2.setValue(value);
        metaField2.setGroupId(1);
        newUser.setMetaFields(new MetaFieldValueWS[] { metaField1,metaField2 });
        return newUser;
    }

    private Date addSemiMonthlyPeriod (GregorianCalendar cal, Integer customerDayOfInvoice) {
        Integer nextInvoiceDay = cal.get(Calendar.DAY_OF_MONTH);
        Integer sourceDay = cal.get(Calendar.DAY_OF_MONTH);

        if (sourceDay < customerDayOfInvoice) {
            nextInvoiceDay = customerDayOfInvoice;
        } else if (customerDayOfInvoice <= 14) {
            nextInvoiceDay = Math.min(customerDayOfInvoice + 15, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            if (sourceDay >= nextInvoiceDay) {
                // Lets say today is 30th and nextInvoiceDay is 29th after adding 15 days.
                // then next invoice date should be 14th of the next month
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            }
        } else if (customerDayOfInvoice == 15 && sourceDay >= customerDayOfInvoice) {
            DateTime sourceDatetime = new DateTime(cal.getTime());
            sourceDatetime = sourceDatetime.withDayOfMonth(sourceDatetime.dayOfMonth().getMaximumValue());
            nextInvoiceDay = sourceDatetime.getDayOfMonth();

            if (sourceDay == nextInvoiceDay) {
                // Lets say today is 31st and nextInvoiceDay is 30 after adding 15 days
                // then next invoice date should be 15th of next month
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            } else if (sourceDay > customerDayOfInvoice) {
                // source day is 30th but not month end
                nextInvoiceDay = customerDayOfInvoice;
                cal.add(Calendar.MONTH, 1);
            }
        }
        cal.set(Calendar.DAY_OF_MONTH, nextInvoiceDay);
        return cal.getTime();
    }

    private static Integer getOrCreateOrderPeriod(JbillingAPI api, int periodUnit){
        Integer orderPeriodWS = null;
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(periodUnit == period.getPeriodUnitId() && period.getValue()  == 1){
                orderPeriodWS = period.getId();
            }
        }

        if (orderPeriodWS == null) {
            orderPeriodWS = createPeriod(api, periodUnit);
        }

        logger.debug("Created Period with id: {}", orderPeriodWS);

        return orderPeriodWS;
    }

    private OrderPeriodWS getOrderPeriod(int periodUnit) throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        OrderPeriodWS[] periods = api.getOrderPeriods();
        for(OrderPeriodWS period : periods){
            if(periodUnit == period.getPeriodUnitId()){
                return period;
            }
        }
        return null;
    }
    
    private UserWS updateCustomerNextInvoiceDate(Integer userId, int periodId) throws Exception {
        logger.debug("Updating Customer Next Invoice Date for user id {}", userId);
        JbillingAPI api = JbillingAPIFactory.getAPI();
        OrderPeriodWS orderPeriodWS = getOrderPeriod(periodId);
        UserWS user = api.getUserWS(userId);
        logger.debug("Old Next Invoice Date is {}", user.getNextInvoiceDate());

        int periodUnitId = null != orderPeriodWS ? orderPeriodWS.getPeriodUnitId() : 0;
        int dayOfMonth = user.getMainSubscription().getNextInvoiceDayOfPeriod();
        PeriodUnit periodUnit = PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);

        Date expectedNextInvoiceDate = DateConvertUtils.asUtilDate(periodUnit.addTo(DateConvertUtils.asLocalDate(user.getNextInvoiceDate()), orderPeriodWS.getValue()));

        user.setNextInvoiceDate(expectedNextInvoiceDate);
        logger.debug("New Next Invoice Date is {}", expectedNextInvoiceDate);
        api.updateUser(user);
        return api.getUserWS(userId);
    }
    
     private static Integer createPeriod(JbillingAPI api, int periodUnit) {
        if (PeriodUnitDTO.MONTH == periodUnit) {
            OrderPeriodWS monthly = new OrderPeriodWS();
            monthly.setEntityId(api.getCallerCompanyId());
            monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);
            monthly.setValue(1);
            monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                    "DSC:MONTHLY:"+1)));
            Integer monthlyPeriod = api.createOrderPeriod(monthly);

            return monthlyPeriod;
        }

        if (PeriodUnitDTO.WEEK == periodUnit) {
            OrderPeriodWS weekly = new OrderPeriodWS();
            weekly.setEntityId(api.getCallerCompanyId());
            weekly.setPeriodUnitId(PeriodUnitDTO.WEEK);
            weekly.setValue(1);
            weekly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                    "DSC:WEEK:" + 1)));
            Integer weeklyPeriod = api.createOrderPeriod(weekly);

            return weeklyPeriod;
        }

        if (PeriodUnitDTO.SEMI_MONTHLY == periodUnit) {
            OrderPeriodWS semiMonthly = new OrderPeriodWS();
            semiMonthly.setEntityId(api.getCallerCompanyId());
            semiMonthly.setPeriodUnitId(PeriodUnitDTO.SEMI_MONTHLY);
            semiMonthly.setValue(1);
            semiMonthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                    "DSC:SEMY_MONTHLY:" + 1)));
            Integer semiMonthlyPeriod = api.createOrderPeriod(semiMonthly);

            return semiMonthlyPeriod;
        }

        if (PeriodUnitDTO.DAY == periodUnit) {
            OrderPeriodWS daily = new OrderPeriodWS();
            daily.setEntityId(api.getCallerCompanyId());
            daily.setPeriodUnitId(PeriodUnitDTO.DAY);
            daily.setValue(1);
            daily.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                    "DSC:DAILY:"+1)));
            Integer dailyPeriod = api.createOrderPeriod(daily);

            return dailyPeriod;
        }

        if (PeriodUnitDTO.YEAR == periodUnit) {
            OrderPeriodWS yearly = new OrderPeriodWS();
            yearly.setEntityId(api.getCallerCompanyId());
            yearly.setPeriodUnitId(PeriodUnitDTO.YEAR);
            yearly.setValue(1);
            yearly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID,
                    "DSC:YEARLY:"+1)));
            Integer dailyPeriod = api.createOrderPeriod(yearly);

            return dailyPeriod;
        }

        return null;
    }

    public static UserWS createUser(boolean goodCC, Integer parentId,
			Integer currencyId) throws JbillingAPIException, IOException {
		return createUser(true, goodCC, parentId, currencyId, true);
	}

	public static UserWS createUser(boolean setPassword, boolean goodCC, Integer parentId,
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
		
		logger.debug("User properties set");
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

		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

       PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				goodCC ? "4111111111111152" : "4111111111111111",
				expiry.getTime());

		newUser.getPaymentInstruments().add(cc);

		if (doCreate) {
			logger.debug("Creating user ...");
			newUser = api.getUserWS(api.createUser(newUser));
	        if (parentId != null) {
	            UserWS parent = api.getUserWS(parentId);
	            newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
	            api.updateUser(newUser);
	            newUser = api.getUserWS(newUser.getId());
	        }
			newUser.setPassword(null);

		}
		logger.debug("User created with id:{}", newUser.getUserId());
		return newUser;
	}

    public static UserWS createUser(boolean setPassword, boolean goodCC, Integer parentId,
                                    Integer currencyId, boolean doCreate,
                                    MainSubscriptionWS mainSubscriptionWS ) throws JbillingAPIException,
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
        newUser.setMainSubscription(mainSubscriptionWS);

        logger.debug("User properties set");
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

        logger.debug("Meta field values set");

        // add a credit card
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

        PaymentInformationWS cc = createCreditCard("Frodo Baggins",
                goodCC ? "4111111111111152" : "4111111111111111",
                expiry.getTime());

        newUser.getPaymentInstruments().add(cc);

        if (doCreate) {
            logger.debug("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
            newUser.setPassword(null);
        }

        logger.debug("User created with id: {}", newUser.getUserId());
        return newUser;
    }
    
    public static UserWS createUpdateUserACHWithCIMProfile(boolean setPassword, boolean goodACH, Integer parentId,
			Integer currencyId, boolean doCreate, boolean withGatewayKey) throws JbillingAPIException,
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
		newUser.setAccountTypeId(Integer.valueOf(MIGRATION_ACCOUNT_TYPE_ID));
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
		
		String pin = ""+Calendar.getInstance().getTimeInMillis();
		
		logger.debug("User properties set");
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
		logger.debug("User properties set");
		
		MetaFieldValueWS metaField_1 = new MetaFieldValueWS();
		metaField_1.setFieldName("partner.prompt.fee");
		metaField_1.setValue("serial-from-ws");
		metaFieldValues.add(metaField_1);
		
		MetaFieldValueWS metaField_2 = new MetaFieldValueWS();
        metaField_2.setFieldName("ccf.payment_processor");
		metaField_2.setValue("FAKE_2"); // the plug-in parameter of the processor
		metaFieldValues.add(metaField_2);
		
		MetaFieldValueWS metaField_3 = new MetaFieldValueWS();
        metaField_3.setFieldName("contact.email");
		metaField_3.setValue(newUser.getUserName() + "@shire.com");
		metaField_3.setGroupId(1);
		metaFieldValues.add(metaField_3);
		
		MetaFieldValueWS metaField_4 = new MetaFieldValueWS();
        metaField_4.setFieldName("contact.first.name");
		metaField_4.setValue("Frodo");
		metaField_4.setGroupId(1);
		metaFieldValues.add(metaField_4);

		MetaFieldValueWS metaField_5 = new MetaFieldValueWS();
        metaField_5.setFieldName("contact.last.name");
		metaField_5.setValue("Baggins");
		metaField_5.setGroupId(1);
		metaFieldValues.add(metaField_5);
		
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
		metaField5.setValue("Baggins");
        metaField5.getMetaField().setDataType(DataType.STRING);
		metaFieldValues.add(metaField5);

		MetaFieldValueWS metaField6 = new MetaFieldValueWS();
        metaField6.setFieldName("City");
		metaField6.setValue("Baggins");
        metaField6.getMetaField().setDataType(DataType.STRING);
		metaFieldValues.add(metaField6);

		MetaFieldValueWS metaField7 = new MetaFieldValueWS();
        metaField7.setFieldName("Email Address");
		metaField7.setValue(newUser.getUserName() + "@shire.com");
        metaField7.getMetaField().setDataType(DataType.STRING);
		metaFieldValues.add(metaField7);

		MetaFieldValueWS metaField8 = new MetaFieldValueWS();
        metaField8.setFieldName("Postal Code");
		metaField8.setValue("K0");
        metaField8.getMetaField().setDataType(DataType.STRING);
		metaFieldValues.add(metaField8);

		MetaFieldValueWS metaField9 = new MetaFieldValueWS();
        metaField9.setFieldName("COUNTRY_CODE");
        metaField9.getMetaField().setDataType(DataType.STRING);
		metaField9.setValue("CA");
		metaField9.setGroupId(14);
		metaFieldValues.add(metaField9);

		MetaFieldValueWS metaField10 = new MetaFieldValueWS();
        metaField10.setFieldName("STATE_PROVINCE");
        metaField10.getMetaField().setDataType(DataType.STRING);
		metaField10.setValue("OR");
		metaField10.setGroupId(14);
		metaFieldValues.add(metaField10);

		MetaFieldValueWS metaField11 = new MetaFieldValueWS();
        metaField11.setFieldName("ORGANIZATION");
        metaField11.getMetaField().setDataType(DataType.STRING);
		metaField11.setValue("Frodo");
		metaField11.setGroupId(14);
		metaFieldValues.add(metaField11);

		MetaFieldValueWS metaField12 = new MetaFieldValueWS();
        metaField12.setFieldName("LAST_NAME");
        metaField12.getMetaField().setDataType(DataType.STRING);
		metaField12.setValue("Baggins");
		metaField12.setGroupId(14);
		metaFieldValues.add(metaField12);

		MetaFieldValueWS metaField13 = new MetaFieldValueWS();
        metaField13.setFieldName("ADDRESS1");
		metaField13.setValue("Baggins");
        metaField13.getMetaField().setDataType(DataType.STRING);
		metaField13.setGroupId(14);
		metaFieldValues.add(metaField13);

		MetaFieldValueWS metaField14 = new MetaFieldValueWS();
        metaField14.setFieldName("CITY");
		metaField14.setValue("Baggins");
        metaField14.getMetaField().setDataType(DataType.STRING);
		metaField14.setGroupId(14);
		metaFieldValues.add(metaField14);

		MetaFieldValueWS metaField15 = new MetaFieldValueWS();
        metaField15.setFieldName("BILLING_EMAIL");
		metaField15.setValue(newUser.getUserName() + "@shire.com");
        metaField15.getMetaField().setDataType(DataType.STRING);
		metaField15.setGroupId(14);
		metaFieldValues.add(metaField15);

		MetaFieldValueWS metaField16 = new MetaFieldValueWS();
        metaField16.setFieldName("POSTAL_CODE");
		metaField16.setValue("K0");
        metaField16.getMetaField().setDataType(DataType.STRING);
		metaField16.setGroupId(14);
		metaFieldValues.add(metaField16);
		
		newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

		logger.debug("Meta field values set");

		// add a ACH/e-cheque
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
		
		if(withGatewayKey){

       PaymentInformationWS ach = createACHWithGatewayKey("jBiller", 
    		   			"Shire Financial Bank", 
    		   			goodACH ? "111111118" : "111111128", 
    		   			"1234567801", 
    		   			Integer.valueOf(1),"asf123123");
        ach.setPaymentMethodId(new Integer(5));
		newUser.getPaymentInstruments().add(ach);
		} else {
			PaymentInformationWS ach = createACH("jBiller", 
		   			"Shire Financial Bank", 
		   			goodACH ? "111111118" : "111111128", 
		   			"1234567801", 
		   			Integer.valueOf(1));
    ach.setPaymentMethodId(new Integer(5));
	newUser.getPaymentInstruments().add(ach);
		}
		
		if (doCreate) {
			logger.debug("Creating user ...");
			newUser = api.getUserWS(api.createUser(newUser));
	        if (parentId != null) {
	            UserWS parent = api.getUserWS(parentId);
	            newUser.setNextInvoiceDate(parent.getNextInvoiceDate());
	            api.updateUser(newUser);
	            newUser = api.getUserWS(newUser.getId());
	        }
			newUser.setPassword(null);

		}
		logger.debug("User created with id:{}", newUser.getUserId());
		return newUser;
	}

	static OrderWS getOrder() {
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
	
	private static Integer createOrder(Integer userId) throws Exception {
	    OrderWS order = getOrder();
	    JbillingAPI api = JbillingAPIFactory.getAPI();
	    
	    Calendar activeSinceDate = Calendar.getInstance();
        activeSinceDate.setTime(new Date());
        activeSinceDate.add(Calendar.MONTH, -1);
        order.setActiveSince(activeSinceDate.getTime());
        order.setUserId(userId);
        OrderChangeWS[] changes = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
        
        for(OrderChangeWS change : changes) {
            change.setApplicationDate(order.getActiveSince());
            change.setStartDate(order.getActiveSince());
        }
        return api.createOrder(order, changes);
	}

	public static PaymentInformationWS createCreditCard(String cardHolderName, String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		
		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, (DateTimeFormat.forPattern(Constants.CC_DATE_FORMAT).print(date.getTime()).toCharArray()));
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false,
				DataType.STRING, 4, CreditCardType.VISA);
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}

	private static PaymentInformationWS createACH(String customerName,
			String bankName, String routingNumber, String accountNumber, Integer accountType) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(ACH_PM_ID);
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_ACH);
		cc.setProcessingOrder(new Integer(2));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, ACH_MF_ROUTING_NUMBER, false, true,
				DataType.CHAR, 1, routingNumber.toCharArray());
		addMetaField(metaFields, ACH_MF_CUSTOMER_NAME, false, true,
				DataType.STRING, 2, customerName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_NUMBER, false, true,
				DataType.CHAR, 3, accountNumber.toCharArray());
		addMetaField(metaFields, ACH_MF_BANK_NAME, false, true,
				DataType.STRING, 4, bankName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_TYPE, false, true,
				DataType.ENUMERATION, 5, accountType == 1 ? Constants.ACH_CHECKING : Constants.ACH_SAVING);

		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}
	
	private static PaymentInformationWS createACHWithGatewayKey(String customerName,
			String bankName, String routingNumber, String accountNumber, Integer accountType, String gatewayKey) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(ACH_PM_ID);
        cc.setPaymentMethodId(Constants.PAYMENT_METHOD_ACH);
		cc.setProcessingOrder(new Integer(2));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, ACH_MF_ROUTING_NUMBER, false, true,
				DataType.CHAR, 1, routingNumber.toCharArray());
		addMetaField(metaFields, ACH_MF_CUSTOMER_NAME, false, true,
				DataType.STRING, 2, customerName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_NUMBER, false, true,
				DataType.CHAR, 3, accountNumber.toCharArray());
		addMetaField(metaFields, ACH_MF_BANK_NAME, false, true,
				DataType.STRING, 4, bankName);
		addMetaField(metaFields, ACH_MF_ACCOUNT_TYPE, false, true,
				DataType.ENUMERATION, 5, accountType == 1 ? Constants.ACH_CHECKING : Constants.ACH_SAVING);
		addMetaField(metaFields, ACH_MF_GATEWAY_KEY, true, false, 
				DataType.CHAR, 6, gatewayKey.toCharArray());

		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}
	
	public static PaymentInformationWS createCheque(String bankName, String chequeNumber, Date date) {
		PaymentInformationWS cheque = new PaymentInformationWS();
		cheque.setPaymentMethodTypeId(CHEQUE_PM_ID);
        cheque.setPaymentMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		cheque.setProcessingOrder(new Integer(3));

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CHEQUE_MF_BANK_NAME, false, true,
				DataType.STRING, 1, bankName);
		addMetaField(metaFields, CHEQUE_MF_NUMBER, false, true,
				DataType.STRING, 2, chequeNumber);
		addMetaField(metaFields, CHEQUE_MF_DATE, false, true,
				DataType.DATE, 3, date);
		cheque.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cheque;
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

	private static Integer createOrder(Integer userId, Integer itemId)
			throws JbillingAPIException, IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();

		// create an order for this user
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(2); // monthly
		order.setCurrencyId(1); // USD

		// a main subscription order
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2009, 1, 1);
		order.setActiveSince(cal.getTime());

		// order lines
		OrderLineWS[] lines = new OrderLineWS[2];
		lines[0] = new OrderLineWS();
		lines[0].setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		lines[0].setQuantity(1);
		lines[0].setItemId(itemId);
		// take the price and description from the item
		lines[0].setUseItem(true);

		lines[1] = new OrderLineWS();
		lines[1].setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		lines[1].setQuantity(3);
		lines[1].setItemId(2602); // lemonade
		// take the price and description from the item
		lines[1].setUseItem(true);

		// attach lines to order
		order.setOrderLines(lines);

		// create the order
		return api.createOrder(order, OrderChangeBL.buildFromOrder(order,
				ORDER_CHANGE_STATUS_APPLY_ID));
	}

	public static MainSubscriptionWS createUserMainSubscription() {
		MainSubscriptionWS mainSubscription = new MainSubscriptionWS();
		mainSubscription.setPeriodId(2); // monthly
		mainSubscription.setNextInvoiceDayOfPeriod(1); // 1st of the month
		return mainSubscription;
	}
	
	private static MetaFieldValueWS getMetaField(MetaFieldValueWS[] metaFields,
			String fieldName) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				return ws;
			}
		}
		return null;
	}

	@Test(enabled=false)
	public static void updateMetaField(MetaFieldValueWS[] metaFields,
			String fieldName, Object value) {
		for (MetaFieldValueWS ws : metaFields) {
			if (ws.getFieldName().equalsIgnoreCase(fieldName)) {
				ws.setValue(value);
			}
		}
	}
	
	private List<PaymentInformationWS> getAch(List<PaymentInformationWS> instruments) {
		List<PaymentInformationWS> found = new ArrayList<PaymentInformationWS>();

		for (PaymentInformationWS instrument : instruments) {
			if (instrument.getPaymentMethodTypeId() == ACH_PM_ID) {
				found.add(instrument);
			}
		}
		return found;
	}
	
	private List<PaymentInformationWS> getCreditCard(List<PaymentInformationWS> instruments) {
		List<PaymentInformationWS> found = new ArrayList<PaymentInformationWS>();
		for(PaymentInformationWS instrument : instruments) {
			if(instrument.getPaymentMethodTypeId() == CC_PM_ID) {
				found.add(instrument);
			}
		}
		return found;
	}
	
	private void pause(long t) {
		logger.debug("pausing for {} ms...", t);
		try {
			Thread.sleep(t);
		} catch (InterruptedException e) {
		}
	}
	
	public static UserWS createUser(Date nextInvoiceDate, BigDecimal autoPaymentLimit) throws JbillingAPIException,
	IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = new UserWS();
		List<MetaFieldValueWS> metaFieldValues = new ArrayList<MetaFieldValueWS>();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword(null);
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(MIGRATION_ACCOUNT_TYPE_ID);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

		logger.debug("User properties set");
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
		metaField7.setValue(newUser.getUserName() + "@shire.com");
		metaFieldValues.add(metaField7);

		MetaFieldValueWS metaField8 = new MetaFieldValueWS();
        metaField8.setFieldName("Postal Code");
        metaField8.getMetaField().setDataType(DataType.STRING);
		metaField8.setValue("K0");
		metaFieldValues.add(metaField8);

		MetaFieldValueWS metaField9 = new MetaFieldValueWS();
        metaField9.setFieldName("COUNTRY_CODE");
        metaField9.getMetaField().setDataType(DataType.STRING);
		metaField9.setValue("CA");
		metaField9.setGroupId(14);
		metaFieldValues.add(metaField9);

		MetaFieldValueWS metaField10 = new MetaFieldValueWS();
        metaField10.setFieldName("STATE_PROVINCE");
        metaField10.getMetaField().setDataType(DataType.STRING);
		metaField10.setValue("OR");
		metaField10.setGroupId(14);
		metaFieldValues.add(metaField10);

		MetaFieldValueWS metaField11 = new MetaFieldValueWS();
        metaField11.setFieldName("ORGANIZATION");
        metaField11.getMetaField().setDataType(DataType.STRING);
		metaField11.setValue("Frodo");
		metaField11.setGroupId(14);
		metaFieldValues.add(metaField11);

		MetaFieldValueWS metaField12 = new MetaFieldValueWS();
        metaField12.setFieldName("LAST_NAME");
        metaField12.getMetaField().setDataType(DataType.STRING);
		metaField12.setValue("Baggins");
		metaField12.setGroupId(14);
		metaFieldValues.add(metaField12);

		MetaFieldValueWS metaField13 = new MetaFieldValueWS();
        metaField13.setFieldName("ADDRESS1");
        metaField13.getMetaField().setDataType(DataType.STRING);
		metaField13.setValue("Baggins");
		metaField13.setGroupId(14);
		metaFieldValues.add(metaField13);

		MetaFieldValueWS metaField14 = new MetaFieldValueWS();
        metaField14.setFieldName("CITY");
        metaField14.getMetaField().setDataType(DataType.STRING);
		metaField14.setValue("Baggins");
		metaField14.setGroupId(14);
		metaFieldValues.add(metaField14);

		MetaFieldValueWS metaField15 = new MetaFieldValueWS();
        metaField15.setFieldName("BILLING_EMAIL");
        metaField15.getMetaField().setDataType(DataType.STRING);
		metaField15.setValue(newUser.getUserName() + "@shire.com");
		metaField15.setGroupId(14);
		metaFieldValues.add(metaField15);

		MetaFieldValueWS metaField16 = new MetaFieldValueWS();
        metaField16.setFieldName("POSTAL_CODE");
        metaField16.getMetaField().setDataType(DataType.STRING);
		metaField16.setValue("K0");
		metaField16.setGroupId(14);
		metaFieldValues.add(metaField16);

		newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[0]));

		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				"5257279846844529", expiry.getTime());
		if(null!=autoPaymentLimit && !autoPaymentLimit.equals(BigDecimal.ZERO)) {
			List<MetaFieldValueWS> paymentMetaFields = new ArrayList<MetaFieldValueWS>();
			paymentMetaFields.addAll(Arrays.asList(cc.getMetaFields()));
			addMetaField(paymentMetaFields, CC_MF_AUTOPAYMENT_LIMIT, false, false, DataType.DECIMAL, 1, autoPaymentLimit);
			cc.setMetaFields(paymentMetaFields.toArray(new MetaFieldValueWS[0]));
		}
		
		newUser.getPaymentInstruments().add(cc);

		logger.debug("Creating user ...");
		MainSubscriptionWS billing = createUserMainSubscription();
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(nextInvoiceDate);
		newUser.setUserId(api.createUser(newUser));
		logger.debug("User created with id:{}",  newUser.getUserId());

		return newUser;
	}
	
	private static UserWS updateUser(UserWS user) throws Exception {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		api.updateUser(user);
		return api.getUserWS(user.getId());
	}

    private MetaFieldWS createMetaFieldWithBuilder(DataType dataType, String name, MetaFieldType fieldUsage){
        return new MetaFieldBuilder().dataType(dataType)
                .name(name)
                .fieldUsage(fieldUsage)
                .entityId(ROOT_ENTITY_ID)
                .entityType(ACCOUNT_TYPE)
                .build();
    }

    private MetaFieldValueWS createMetaFieldValue(MetaFieldWS metaField, Object value, Integer groupId){
        MetaFieldValueWS metaFieldValueWS = new MetaFieldValueWS();
        metaFieldValueWS.setValue(value);
        metaFieldValueWS.getMetaField().setDataType(metaField.getDataType());
        metaFieldValueWS.setFieldName(metaField.getName());
        metaFieldValueWS.setGroupId(groupId);

        return metaFieldValueWS;
    }

    //Helper method to create payment
    private PaymentWS createPayment(String amount, boolean isRefund, UserWS user, Integer resultId, Integer linkedPaymentId) {
        PaymentWS payment = new PaymentWS();
        payment.setAmount(new BigDecimal(amount));
        payment.setIsRefund(isRefund ? 1 : 0);
        payment.setPaymentDate(Calendar.getInstance().getTime());
        payment.setCurrencyId(1);
        payment.setUserId(user.getId());
        payment.setPaymentId(isRefund ? linkedPaymentId : null);
        payment.setResultId(resultId);
        payment.setBalance("0");
        payment.getPaymentInstruments().add(user.getPaymentInstruments().get(0));

        return payment;
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
