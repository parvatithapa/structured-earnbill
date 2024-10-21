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

package com.sapienter.jbilling.server.process;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.AgeingWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.CollectionType;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.TestConstants;


@Test(groups = { "integration", "process" }, testName = "SuspendedUsersBillingProcessFilterTaskTest")
public class SuspendedUsersBillingProcessFilterTaskTest {

	private static final Logger logger = LoggerFactory.getLogger(SuspendedUsersBillingProcessFilterTaskTest.class);
	private  JbillingAPI api;
	private int ORDER_CHANGE_STATUS_APPLY_ID;
	private Integer suspendedUserBillingProcessFilterTaskPlugnInId;
	private String BASIC_BILLING_PROCESS_FILTER_TASK;
	private String SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK;
	private Integer dailyPeriodId;
	
    @BeforeClass
    protected void setUp() throws Exception {
    	api = JbillingAPIFactory.getAPI();
    	BASIC_BILLING_PROCESS_FILTER_TASK = "com.sapienter.jbilling.server.process.task.BasicBillingProcessFilterTask";
    	SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK = "com.sapienter.jbilling.server.process.task.SuspendedUsersBillingProcessFilterTask";
    	ORDER_CHANGE_STATUS_APPLY_ID = 3;
    	dailyPeriodId = createDailyOrderPeriod(api, 1);
    }
    
    private static Integer createDailyOrderPeriod(JbillingAPI api, int days) {
		OrderPeriodWS daily = new OrderPeriodWS();
		daily.setEntityId(api.getCallerCompanyId());
		daily.setPeriodUnitId(3);//Daily Period
		daily.setValue(days);
		daily.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "Tets Daily Period:"+days)));
		return api.createOrderPeriod(daily);
	}
    
	@Test
    public void testWithBasicBillingProcessFilterTask() throws Exception {

		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.set(Calendar.YEAR, 2008);
		nextInvoiceDate.set(Calendar.MONTH, 11);
		nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 2);
		
		List<AgeingWS> ageingList = Arrays.asList(api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR));
		assertNotNull("Agining Status Not Found For Entity -> "+api.getCallerCompanyId(), ageingList);
		assertTrue("No Status Found .", null != ageingList && !ageingList.isEmpty());
		
		// Creating User
		UserWS activeUser = createUserForStatus(nextInvoiceDate.getTime(), UserDTOEx.STATUS_ACTIVE);
		logger.debug("Active User ::::::::: {}", activeUser.getId());
		
		Integer suspendedStatusId = ageingList.get(ageingList.size()-2).getStatusId();
		UserWS suspendedUser = createUserForStatus(nextInvoiceDate.getTime(), suspendedStatusId);
		
		logger.debug("Suspended User ::::::::: {}", suspendedUser.getId());
		
		// Creating Order
		Calendar orderActiveSinceDate = Calendar.getInstance();
		orderActiveSinceDate.set(2008, 11, 1);
		
		Integer orderIdForActiveUser = createOrder(activeUser.getId(), orderActiveSinceDate.getTime());
		
		Integer orderIdForSuspendedUser = createOrder(suspendedUser.getId(), orderActiveSinceDate.getTime());
		
		
		Calendar runDate = Calendar.getInstance();
		runDate.set(2008, 11, 2);
		
		// Triggred Billing Process On 2nd of Dec 2008.
		triggerBilling(runDate.getTime());
		Thread.sleep(10000);
		
		InvoiceWS activeUserInvocie = api.getLatestInvoice(activeUser.getId());
		assertNotNull("Active User did not pick by BasicBillingProcessFilterTask ", activeUserInvocie);
		assertInvoiceForOrder(activeUserInvocie, orderIdForActiveUser);
		
		InvoiceWS suspendedUserInvocie = api.getLatestInvoice(suspendedUser.getId());
		assertNull("Suspended User picked by BasicBillingProcessFilterTask ", suspendedUserInvocie);
		
	}
	
	@Test
	public void testWithSuspendedUsersBillingProcessFilterTask() throws Exception {
		
		// Configuring SuspendedUsersBillingProcessFilterTask
		logger.debug("Configuring SuspendedUsersBillingProcessFilterTask..............");
		PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(SUSPENDED_USERS_BILLING_PROCESS_FILTER_TASK);
    	PluggableTaskWS plugIn = new PluggableTaskWS();
        plugIn.setNotes("Test- Plugin");
        plugIn.setOwningEntityId(api.getCallerCompanyId());
        plugIn.setProcessingOrder(1);
        plugIn.setTypeId(type.getId());
        suspendedUserBillingProcessFilterTaskPlugnInId = api.createPlugin(plugIn);
		
		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.set(Calendar.YEAR, 2008);
		nextInvoiceDate.set(Calendar.MONTH, 11);
		nextInvoiceDate.set(Calendar.DAY_OF_MONTH, 3);
		
		List<AgeingWS> ageingList = Arrays.asList(api.getAgeingConfigurationWithCollectionType(api.getCallerLanguageId(), CollectionType.REGULAR));
		assertNotNull("Agining Status Not Found For Entity -> "+api.getCallerCompanyId(), ageingList);
		assertTrue("No Status Found .", null != ageingList && !ageingList.isEmpty());
		
		// Creating User
		UserWS activeUser = createUserForStatus(nextInvoiceDate.getTime(), UserDTOEx.STATUS_ACTIVE);
		logger.debug("Active User ::::::::: {}", activeUser.getId());
		
		Integer suspendedSecondLastStatusId = ageingList.get(ageingList.size()-2).getStatusId();
		UserWS suspendedUserWithSecondLastStatus = createUserForStatus(nextInvoiceDate.getTime(), suspendedSecondLastStatusId);
		
		Integer suspendedLastStatusId = ageingList.get(ageingList.size()-1).getStatusId();
		UserWS suspendedUserWithLastStatus = createUserForStatus(nextInvoiceDate.getTime(), suspendedLastStatusId);
		
		// Creating Order
		Calendar orderActiveSinceDate = Calendar.getInstance();
		orderActiveSinceDate.set(2008, 11, 2);
		
		Integer orderIdForActiveUser = createOrder(activeUser.getId(), orderActiveSinceDate.getTime());
		
		Integer orderIdForSecodLastSuspendedUser = createOrder(suspendedUserWithSecondLastStatus.getId(), orderActiveSinceDate.getTime());
		
		Integer orderIdForLastSuspendedUser = createOrder(suspendedUserWithLastStatus.getId(), orderActiveSinceDate.getTime());
		
		
		Calendar runDate = Calendar.getInstance();
		runDate.set(2008, 11, 3);
		
		// Triggred Billing Process On 3nd of Dec 2008.
		triggerBilling(runDate.getTime());
		Thread.sleep(10000);
		
		InvoiceWS activeUserInvocie = api.getLatestInvoice(activeUser.getId());
		assertNotNull("Active User did not pick by SuspendedUsersBillingProcessFilterTask ", activeUserInvocie);
		assertInvoiceForOrder(activeUserInvocie, orderIdForActiveUser);
		
		InvoiceWS suspendedUserWithSecondLastStatusInvocie = api.getLatestInvoice(suspendedUserWithSecondLastStatus.getId());
		assertNotNull("Suspended User did not pick by SuspendedUsersBillingProcessFilterTask ", suspendedUserWithSecondLastStatusInvocie);
		assertInvoiceForOrder(suspendedUserWithSecondLastStatusInvocie, orderIdForSecodLastSuspendedUser);
		
		InvoiceWS suspendedUserWithLastStatusInvocie = api.getLatestInvoice(suspendedUserWithLastStatus.getId());
		assertNull("Suspended User With Last Aging Status picked by SuspendedUsersBillingProcessFilterTask ", suspendedUserWithLastStatusInvocie);
		
	}
	
	@AfterClass
	protected void cleanUp() throws Exception {
		logger.debug("Deleting  suspendedUserBillingProcessFilterTaskPlugnIn ..............");
		api.deletePlugin(suspendedUserBillingProcessFilterTaskPlugnInId);
	}
	
	private Integer createOrder(Integer userId, Date activeSinceDate) {
		// create an order for $10,
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(new Integer(1)); // One Time Order Period
		order.setCurrencyId(new Integer(1));
		order.setActiveSince(activeSinceDate);

		OrderLineWS line = new OrderLineWS();
		line.setPrice(new BigDecimal("10.00"));
		line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		line.setQuantity(new Integer(1));
		line.setAmount(new BigDecimal("10.00"));
		line.setItemId(TestConstants.INBOUND_USAGE_PRODUCT_ID);
		line.setDescription("Order created by test-case");

		order.setOrderLines(new OrderLineWS [] {line});
		
		OrderChangeWS orderChanges [] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		
		for(OrderChangeWS orderChange : orderChanges) {
			orderChange.setStartDate(activeSinceDate);
			orderChange.setApplicationDate(activeSinceDate);
		}
		
		Integer orderId = api.createOrder(order, orderChanges);
		return orderId;
	}
	
    private UserWS createUserForStatus(Date nextInvoiceDate, Integer statusId) throws Exception {
		// Create - This passes the password validation routine.
    	logger.debug("nextInvoiceDate::::::: {}", nextInvoiceDate);
    	UserWS newUser = new UserWS();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(Integer.valueOf(1));
		newUser.setParentId(null);
		newUser.setStatusId(statusId);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

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
		

		MainSubscriptionWS billing = new MainSubscriptionWS();
		billing.setPeriodId(dailyPeriodId);
		billing.setNextInvoiceDayOfPeriod(1);
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(nextInvoiceDate);
		
		logger.debug("Meta field values set");
		newUser = api.getUserWS(api.createUser(newUser));
		
		billing.setPeriodId(dailyPeriodId);
		billing.setNextInvoiceDayOfPeriod(1);
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(nextInvoiceDate);
		api.updateUser(newUser);
		logger.debug("User created with id:{}", newUser.getUserId());
		
		return newUser;
	}
    
    private void assertInvoiceForOrder(InvoiceWS invoice, Integer orderId) {
    	List<Integer> orderIds = Arrays.asList(invoice.getOrders());
    	assertTrue("Invoice Does not Contain  Order  "+ orderId, orderIds!=null && !orderIds.isEmpty() &&  orderIds.contains(orderId));
    }
    
    private void triggerBilling(Date runDate) {
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        config.setNextRunDate(runDate);
        config.setRetries(new Integer(1));
        config.setDaysForRetry(new Integer(5));
        config.setGenerateReport(new Integer(0));
        config.setAutoPaymentApplication(new Integer(0));
        config.setDfFm(new Integer(0));
        config.setPeriodUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(new Integer(0));
        config.setInvoiceDateProcess(new Integer(0));
        config.setMaximumPeriods(new Integer(99));
        config.setOnlyRecurring(new Integer(0));

        logger.debug("B - Setting config to: {}", config);
        api.createUpdateBillingProcessConfiguration(config);

        logger.debug("Running Billing Process for {}", runDate );
        api.triggerBilling(runDate);
    }
    
    private void updatePlugin (Integer plugInId, String className) {
		PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(className);
	    PluggableTaskWS plugin = api.getPluginWS(plugInId);
	    plugin.setTypeId(type.getId());
	    api.updatePlugin(plugin);
	}
}
