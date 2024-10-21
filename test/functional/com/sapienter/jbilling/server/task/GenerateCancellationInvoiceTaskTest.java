package com.sapienter.jbilling.server.task;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.billing.task.GenerateCancellationInvoiceTask;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.CancellationRequestWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.db.CancellationRequestStatus;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;

import org.apache.commons.lang.ArrayUtils;

@Test(groups = { "integration" }, testName="GenerateCancellationInvoiceTaskTest")
public class GenerateCancellationInvoiceTaskTest {

	private TestBuilder testBuilder;
	private EnvironmentHelper environmentHelper;
	private static final Logger logger = Logger.getLogger(GenerateCancellationInvoiceTaskTest.class);

	private final String reasonText = "User has requested to cancel subscription orders";
	private static final String CATEGORY_CODE = "CRTestCategory";
	private static final String PRODUCT_CODE = "CRTestProduct";
	private static final String ACCOUNT_TYPE_CODE = "CRTestAccountType";
	private static final String CUSTOMER_CODE = "CRTestCustomer"+System.currentTimeMillis();
	private static final String PLUGIN_CODE = "Plugin-Code";
	private static final String ORDER_STATUS_FINISHED = "Finished";
	private static final String USER_CANCELLATION_STATUS = "Cancelled on Request";

	private static final String GENERATE_CANCELLATION_INVOICE_TASK_CLASS_NAME = "com.sapienter.jbilling.server.billing.task.GenerateCancellationInvoiceTask";
    private static final String PRODUCT_CODE_2 = "CRTestProduct_2";
    private static final Integer ADJUSTMENT_PRODUCT_ID = 320111;
    private boolean gcitPresent = false;
    private Integer gcitPluginId;
    private JbillingAPI api;

	@BeforeClass
	public void initializeTests(){
		testBuilder = getTestEnvironment();
	}


	@AfterClass
	public void tearDown(){
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
			api = envCreator.getPrancingPonyApi();
			environmentHelper = EnvironmentHelper.getInstance(api);

			envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(true).build();
			envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(envCreator.idForCode(CATEGORY_CODE))
					.withFlatPrice("0.50").build();
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE_2).global(true).withType(envCreator.idForCode(CATEGORY_CODE))
                .withFlatPrice("10.00").build();
			envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();
			
			// configuring plugin 
			envCreator.pluginBuilder(api)
					  .withCode(PLUGIN_CODE)
					  .withTypeId(api.getPluginTypeWSByClassName(GENERATE_CANCELLATION_INVOICE_TASK_CLASS_NAME).getId())
					  .withOrder(12345677)
					  .build();
		});
	}


	@Test(priority = 1)
	public void test001GenerateCancellationInvoiceWithSingleOrder(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order -1")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();

		}).test((env, envBuilder)-> {
			JbillingAPI api = env.getPrancingPonyApi();
			Integer userId = env.idForCode(CUSTOMER_CODE);
			assertNotNull("UserId should not be null",userId);

			Integer customerId = api.getUserWS(userId).getCustomerId();
			assertNotNull("CustomerId should not be null",customerId);

			CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
			assertEquals("Cancellation date was not set correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));

			Integer cancellationRequestId = api.createCancellationRequest(crWS);
			assertNotNull("CancellationRequestId should not be null", cancellationRequestId);

			OrderWS[] orderWS = api.getUserSubscriptions(userId);
			assertEquals("Orders should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
			assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
			assertEquals("Order's active until date should be equal to cancellation date: ",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
			
			logger.debug("Running Cancellation Task....");
			api.triggerScheduledTask(env.idForCode(PLUGIN_CODE), new Date());
			sleep(3000);
			UserWS user = api.getUserWS(userId);
			 assertEquals("User's status sahould be: ",
						USER_CANCELLATION_STATUS,user.getStatus());
			Arrays.stream(orderWS)
			  .forEach(order -> {
				  OrderWS orderDb = api.getOrder(order.getId());
				  assertEquals("Order's status should be : ",
							ORDER_STATUS_FINISHED,orderDb.getStatusStr());
			  });

		});
	}


	@Test(priority=2)
	public void test002GenerateCancellationInvoiceWithMultipleOrders(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order-2")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(addDays(activeSince,5))
			.withEffectiveDate(addDays(activeSince,5))
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("Order-3")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();

		}).test((env)-> {

			JbillingAPI api = env.getPrancingPonyApi();

			Integer userId = env.idForCode(CUSTOMER_CODE);
			assertNotNull("UserId should not be null",userId);

			Integer customerId = api.getUserWS(userId).getCustomerId();
			assertNotNull("CustomerId should not be null",customerId);

			CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince,15),customerId,reasonText);
			assertEquals("Cancellation date is created correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));

			Integer cancellationRequestId = api.createCancellationRequest(crWS);
			assertNotNull("CancellationRequestId should not be null", cancellationRequestId);

			OrderWS[] ordersWS = api.getUserSubscriptions(userId);
			assertEquals("Number of subscription orders should be equal to 2.", 2, (ordersWS != null ? ordersWS.length : 0));

			Arrays.stream(ordersWS).forEach( orderWS -> {
				assertNotNull("Order's active untill date should not be null after Cancellation Request", orderWS.getActiveUntil());
				assertEquals("Order's active until date should be equal to cancellation date: ",
						TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
						TestConstants.DATE_FORMAT.format(orderWS.getActiveUntil()));
			});	                        	
			
			
			logger.debug("Running Cancellation Task....");
			api.triggerScheduledTask(env.idForCode(PLUGIN_CODE), new Date());
			sleep(1000);
			UserWS user = api.getUserWS(userId);
			 assertEquals("User's status sahould be: ",
						USER_CANCELLATION_STATUS,user.getStatus());
			Arrays.stream(ordersWS)
			  .forEach(order -> {
				  OrderWS orderDb = api.getOrder(order.getId());
				  assertEquals("Order's status should be : ",
							ORDER_STATUS_FINISHED,orderDb.getStatusStr());
			  });
		});
	}
	
	@Test(priority=3)
	public void test003GenerateCancellationInvoiceOfOneTimeOrder(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);
		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			final Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("One time Order")
			.withPeriod(environmentHelper.getOrderPeriodOneTime(api))
			.build();


		}).test((env)-> {

			JbillingAPI api = env.getPrancingPonyApi();

			Integer userId = env.idForCode(CUSTOMER_CODE);
			assertNotNull("UserId should not be null",userId);

			Integer customerId = api.getUserWS(userId).getCustomerId();
			assertNotNull("CustomerId should not be null",customerId);

			CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince,15),customerId,reasonText);
			assertEquals("Cancellation date is created correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));
			try {
				api.createCancellationRequest(crWS);
				fail("Exception Expected");
			} catch(SessionInternalError error) {
				
			}
			

			logger.debug("Running Cancellation Task....");
			api.triggerScheduledTask(env.idForCode(PLUGIN_CODE), new Date());
			sleep(1000);
			
			UserWS user = api.getUserWS(userId);
			InvoiceWS invoice = api.getLatestInvoice(userId);
			
			assertEquals("Invoice Should not be generated : ",
					null,invoice);
			
			 assertEquals("User's status should be: ",
						UserDTOEx.STATUS_ACTIVE,user.getStatusId());
		});
	}

	@Test(priority = 4)
	public void test004GenerateCancellationInvoiceWithPrePaidSingleOrder(){

		final Date activeSince = FullCreativeUtil.getDate(11,01,2016);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
			assertNotNull("UserId should not be null",userId);

			envBuilder.orderBuilder(api)
			.forUser(userId)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests("PrePaid Order")
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();

		}).test((env, envBuilder)-> {
			JbillingAPI api = env.getPrancingPonyApi();
			Integer userId = env.idForCode(CUSTOMER_CODE);
			assertNotNull("UserId should not be null",userId);

			Integer customerId = api.getUserWS(userId).getCustomerId();
			assertNotNull("CustomerId should not be null",customerId);

			CancellationRequestWS crWS = constructCancellationRequestWS(addDays(activeSince, 15), customerId, reasonText);
			assertEquals("Cancellation date was not set correctly for customer",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(FullCreativeUtil.getDate(11,16,2016)));

			Integer cancellationRequestId = api.createCancellationRequest(crWS);
			assertNotNull("CancellationRequestId should not be null", cancellationRequestId);

			OrderWS[] orderWS = api.getUserSubscriptions(userId);
			assertEquals("Orders should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
			assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
			assertEquals("Order's active until date should be equal to cancellation date: ",
					TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
					TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));
			
			logger.debug("Running Cancellation Task....");
			api.triggerScheduledTask(env.idForCode(PLUGIN_CODE), new Date());
			sleep(1000);
			UserWS user = api.getUserWS(userId);
			 assertEquals("User's status sahould be: ",
						USER_CANCELLATION_STATUS,user.getStatus());
			Arrays.stream(orderWS)
			  .forEach(order -> {
				  OrderWS orderDb = api.getOrder(order.getId());
				  assertEquals("Order's status should be : ",
							ORDER_STATUS_FINISHED,orderDb.getStatusStr());
			  });

		});
	}

	@Test(priority = 5)
    public void test005GenerateCancellationInvoiceWithPrePaidSingleOrder(){

        final Calendar activeSinceCal = Calendar.getInstance();
        activeSinceCal.set(Calendar.DATE, 1);
        final Calendar nextMonthNID = (Calendar) activeSinceCal.clone();
        nextMonthNID.add(Calendar.MONTH, 1);
        final Calendar todaysCal = Calendar.getInstance();
        DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
        final Calendar tomorrowsCal = Calendar.getInstance();
        tomorrowsCal.add(Calendar.DATE, 1);

        testBuilder.given(envBuilder -> {

            final JbillingAPI api = envBuilder.getPrancingPonyApi();
            Integer userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE),
                    activeSinceCal.getTime());
            assertNotNull("UserId should not be null",userId);

            UserWS userWS = api.getUserWS(userId);
            userWS.setNextInvoiceDate(activeSinceCal.getTime());
            api.updateUser(userWS);

            envBuilder.orderBuilder(api)
                .forUser(userId)
                .withProducts(envBuilder.env().idForCode(PRODUCT_CODE_2))
                .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
                .withActiveSince(activeSinceCal.getTime())
                .withEffectiveDate(activeSinceCal.getTime())
                .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                .withDueDateValue(Integer.valueOf(1))
                .withCodeForTests("PrePaid Order")
                .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                .withProrate(true)
                .build();
            api.createInvoiceWithDate(userId, activeSinceCal.getTime(), null, null, true);

            userWS = api.getUserWS(userId);
            userWS.setNextInvoiceDate(nextMonthNID.getTime());
            api.updateUser(userWS);
        }).test((env, envBuilder)-> {
            JbillingAPI api = env.getPrancingPonyApi();
            Integer userId = env.idForCode(CUSTOMER_CODE);
            assertNotNull("UserId should not be null",userId);

            Integer customerId = api.getUserWS(userId).getCustomerId();
            assertNotNull("CustomerId should not be null",customerId);

            configureRefundOnCancleTask(ADJUSTMENT_PRODUCT_ID);

            CancellationRequestWS crWS = constructCancellationRequestWS(todaysCal.getTime(), customerId, reasonText);
            assertEquals("Cancellation date was not set correctly for customer",
                    TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
                    TestConstants.DATE_FORMAT.format(todaysCal.getTime()));

            Integer cancellationRequestId = api.createCancellationRequest(crWS);
            assertNotNull("CancellationRequestId should not be null", cancellationRequestId);

            OrderWS[] orderWS = api.getUserSubscriptions(userId);
            assertEquals("Orders should be equal to 1.", 1, (orderWS != null ? orderWS.length : 0));
            assertNotNull("Order's active until date should not be null", orderWS[0].getActiveUntil());
            assertEquals("Order's active until date should be equal to cancellation date: ",
                    TestConstants.DATE_FORMAT.format(crWS.getCancellationDate()),
                    TestConstants.DATE_FORMAT.format(orderWS[0].getActiveUntil()));

            logger.debug("Running Cancellation Task....");
            api.triggerScheduledTask(env.idForCode(PLUGIN_CODE), new Date());
            sleep(1000);
            UserWS user = api.getUserWS(userId);
            assertEquals("User's status sahould be: ", USER_CANCELLATION_STATUS,user.getStatus());
            Arrays.stream(orderWS)
              .forEach(order -> {
                  OrderWS orderDb = api.getOrder(order.getId());
                  assertEquals("Order's status should be : ", ORDER_STATUS_FINISHED,orderDb.getStatusStr());
              });

            Integer[] orderIds = api.getOrdersByDate(userId, activeSinceCal.getTime(), tomorrowsCal.getTime());
            assertNotNull("Credit order should be created!", orderIds);
            Arrays.asList(orderIds).stream().filter(orderId -> !(orderId.equals(orderWS[0].getId()))).forEach(orderId -> {
                OrderWS creditOrder = api.getOrder(orderId);
                assertEquals("Credit order's active until should be tomorrows date", formatter.format(tomorrowsCal.getTime()),
                        formatter.format(creditOrder.getActiveSince()));
                Double subscriptionTotal = Double.valueOf(orderWS[0].getTotal());
                Integer totalDays = todaysCal.getActualMaximum(Calendar.DATE);
                Double creditTotal = Double.valueOf(creditOrder.getTotal());
                Integer remainingDays = totalDays - todaysCal.get(Calendar.DATE);
                assertEquals("Credit order's amount should be prorated amount of remaining days", Math.abs(Math.round((subscriptionTotal/totalDays)*remainingDays)),
                        Math.abs(Math.round(creditTotal)));
            });
            InvoiceWS[] invoices = api.getAllInvoicesForUser(userId);
            assertEquals("Credit note invoice should be generated!", (todaysCal.get(Calendar.DATE) == todaysCal.getActualMaximum(Calendar.DATE))
                    ? 1: 2, invoices.length);
        });
    }

	private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
		final JbillingAPI api = envBuilder.getPrancingPonyApi();

		CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
				.withUsername(code).withAccountTypeId(accountTypeId)
				.withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), getDay(nid)));
		
		Calendar nextInvoiceDate = Calendar.getInstance();
		nextInvoiceDate.setTime(nid);
		nextInvoiceDate.add(Calendar.MONTH, 1);
		UserWS user = customerBuilder.build();
		user.setNextInvoiceDate(nextInvoiceDate.getTime());
		api.updateUser(user);
		return user.getId();
	}

    private Integer configureGenrateCancellationInvoicePlugin() {

        PluggableTaskWS invoiceBillingProcessLinkingTask = new PluggableTaskWS();
        invoiceBillingProcessLinkingTask.setProcessingOrder(10);
        PluggableTaskTypeWS invoiceBillingProcessLinkingTaskType =
                api.getPluginTypeWSByClassName(GenerateCancellationInvoiceTask.class.getName());
        invoiceBillingProcessLinkingTask.setTypeId(invoiceBillingProcessLinkingTaskType.getId());

        invoiceBillingProcessLinkingTask.setParameters(new Hashtable<String, String>(invoiceBillingProcessLinkingTask.getParameters()));
        Hashtable<String, String> parameters = new Hashtable<>();
        parameters.put("cron_exp", "0 0/1 * 1/1 * ? *");
        invoiceBillingProcessLinkingTask.setParameters(parameters);
        return api.createPlugin(invoiceBillingProcessLinkingTask);
    }
	private static Date addDays(Date inputDate, int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(inputDate);
		cal.add(Calendar.DATE, days);
		return cal.getTime();
	}

	private static Integer getDay(Date inputDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(inputDate);
		return Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
	}

	private void configureRefundOnCancleTask(Integer adjustmentProductId) {
        PluggableTaskTypeWS pluginType = api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.order.task.RefundOnCancelTask");
        PluggableTaskWS refundOnCancelTask = api.getPluginWSByTypeId(pluginType.getId());
        Hashtable<String, String> refundOnCancelTaskPluginparameters = new Hashtable<>();
        refundOnCancelTaskPluginparameters.put("adjustment_product_id", adjustmentProductId.toString());
        if(null == refundOnCancelTask) {
            refundOnCancelTask = new PluggableTaskWS();
            refundOnCancelTask.setProcessingOrder(580);
            refundOnCancelTask.setTypeId(pluginType.getId());
            refundOnCancelTask.setParameters(refundOnCancelTaskPluginparameters);
            api.createPlugin(refundOnCancelTask);
        } else {
            refundOnCancelTask.setParameters(refundOnCancelTaskPluginparameters);
            api.updatePlugin(refundOnCancelTask);
        }
    }

    private CancellationRequestWS constructCancellationRequestWS(Date cancellationDate, Integer customerId,
            String reasonText) {
        CancellationRequestWS cancellationRequestWS = new CancellationRequestWS();
        cancellationRequestWS.setCancellationDate(cancellationDate);
        cancellationRequestWS.setCreateTimestamp(new Date());
        cancellationRequestWS.setCustomerId(customerId);
        cancellationRequestWS.setReasonText(reasonText);
        cancellationRequestWS.setStatus(CancellationRequestStatus.APPLIED);
        return cancellationRequestWS;
    }
	
	private void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch(InterruptedException ex) {

		}
	}
	
	
}
