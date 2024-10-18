package com.sapienter.jbilling.server.process;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import com.sapienter.jbilling.server.util.Constants;

import org.joda.time.DateMidnight;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;

import org.testng.AssertJUnit;

import com.sapienter.jbilling.fc.FullCreativeUtil;

@Test(groups = { "billing-and-discounts", "billing" }, testName = "BillingProcessLinkingTest")
public class BillingProcessLinkingTest  extends BillingProcessTestCase{

	private TestBuilder testBuilder;
	private EnvironmentHelper environmentHelper;

	private static final String CATEGORY_CODE = "TestCategory";
	private static final String PRODUCT_CODE = "TestProduct";
	private static final String ACCOUNT_TYPE_CODE = "TestAccountType";


	private static final String CUSTOMER_CODE1 = "TestCustomer1";
	private static final String CUSTOMER_CODE2 = "TestCustomer2";
	private static final String CUSTOMER_CODE3 = "TestCustomer3";


	private Integer CATEGORY_ID;
	private Integer PRODUCT_ID;
	private Integer ACCOUNT_ID;

	private final String ORDER01 = "MONTHLY_01" ;
	private final String ORDER02 = "MONTHLY_02" ;
	private final String ORDER03 = "MONTHLY_03" ;
	private final String ORDER04 = "oneTime" ;

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
			final JbillingAPI api = envCreator.getPrancingPonyApi();
			environmentHelper = EnvironmentHelper.getInstance(api);

			CATEGORY_ID = envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(true).build();
			PRODUCT_ID = envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(CATEGORY_ID)
					.withFlatPrice("0.50").build();
			ACCOUNT_ID = envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();
		});
	}

	@Test(priority = 1)
	public void test001BillingProcessLinkingForMultipleUsersHavingSingleOrder(){

		//May 01 2015
		final Date nid = FullCreativeUtil.getDate(04,01,2015);
		//April 01 2015
		final Date activeSince = FullCreativeUtil.getDate(03,01,2015);

		testBuilder.given(envBuilder -> {

			final JbillingAPI api = envBuilder.getPrancingPonyApi();
			Integer user1 = createCustomer(envBuilder, CUSTOMER_CODE1, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nid);
			AssertJUnit.assertNotNull("CustomerId should not be null",user1);

			Integer user2 = createCustomer(envBuilder, CUSTOMER_CODE2, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nid);
			AssertJUnit.assertNotNull("CustomerId should not be null",user2);

			Integer user3 = createCustomer(envBuilder, CUSTOMER_CODE3, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nid);
			AssertJUnit.assertNotNull("CustomerId should not be null",user1);

			envBuilder.orderBuilder(api)
			.forUser(user1)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests(ORDER01)
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();

			envBuilder.orderBuilder(api)
			.forUser(user2)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(FullCreativeUtil.getDate(03,10,2015))
			.withEffectiveDate(FullCreativeUtil.getDate(03,10,2015))
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests(ORDER02)
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.withProrate(true)
			.build();
			
			envBuilder.orderBuilder(api)
			.forUser(user3)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(activeSince)
			.withEffectiveDate(activeSince)
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests(ORDER03)
			.withPeriod(environmentHelper.getOrderPeriodMonth(api))
			.build();

			envBuilder.orderBuilder(api)
			.forUser(user3)
			.withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
			.withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
			.withActiveSince(FullCreativeUtil.getDate(03,10,2015))
			.withEffectiveDate(FullCreativeUtil.getDate(03,10,2015))
			.withDueDateUnit(Constants.PERIOD_UNIT_DAY)
			.withDueDateValue(Integer.valueOf(1))
			.withCodeForTests(ORDER04)
			.withPeriod(environmentHelper.getOrderPeriodOneTime(api))
			.withProrate(true)
			.build();
		}).test( env -> {
			Integer userId1 = env.idForCode(CUSTOMER_CODE1);
			Integer userId2 = env.idForCode(CUSTOMER_CODE2);
			Integer userId3 = env.idForCode(CUSTOMER_CODE3);

			Date billingDate = new DateMidnight(2015,05, 1).toDate();
			Integer[] invoiceIdsOfUser1= api.createInvoiceWithDate(userId1,billingDate, null, null, false);
			AssertJUnit.assertNotNull("invoiceIdsOfUser1 should not be null",invoiceIdsOfUser1);
			verifyBeforeLinking(invoiceIdsOfUser1);
			Integer[] invoiceIdsOfUser2= api.createInvoiceWithDate(userId2,billingDate, null, null, false);
			AssertJUnit.assertNotNull("invoiceIdsOfUSer2 should not be null",invoiceIdsOfUser2);
			verifyBeforeLinking(invoiceIdsOfUser2);
			
			Integer[] invoiceIdsOfUser3= api.createInvoiceWithDate(userId3,billingDate, null, null, false);
			AssertJUnit.assertNotNull("invoiceIdsOfUser1 should not be null",invoiceIdsOfUser3);
			verifyBeforeLinking(invoiceIdsOfUser3);

			//Configure 
			Integer pluginId = configurePluginSetCronExpression();
			try {

				Thread.sleep(60000);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.debug("pluging configured with id {}", pluginId);
			api.deletePlugin(pluginId);
			verifyAfterLinking(invoiceIdsOfUser1);
			verifyAfterLinking(invoiceIdsOfUser2);
			verifyAfterLinking(invoiceIdsOfUser3);
		});
	}


	private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
		final JbillingAPI api = envBuilder.getPrancingPonyApi();

		CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
				.withUsername(code).withAccountTypeId(accountTypeId)
				.withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), 1));

		UserWS user = customerBuilder.build();
		user.setNextInvoiceDate(nid);
		api.updateUser(user);
		return user.getId();
	}

	private void verifyBeforeLinking(Integer[] invoiceIdsOfUser){
		Arrays.asList(invoiceIdsOfUser).stream().forEach(invoiceId -> {
			InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
			AssertJUnit.assertNull("Billing process associated with invoice should be null", invoiceWS.getBillingProcess());
			logger.debug("invoice with id: {} invoice create date {}",invoiceId, api.getInvoiceWS(invoiceId).getCreateDatetime());
		});

	}

	private void verifyAfterLinking(Integer[] invoiceIdsOfUser){
		Arrays.asList(invoiceIdsOfUser).stream().forEach(invoiceId -> {
			InvoiceWS invoiceWS = api.getInvoiceWS(invoiceId);
			AssertJUnit.assertNotNull("Billing process associated with invoice should not be null", invoiceWS.getBillingProcess());
			logger.debug("invoice with id: {} invoice create date: {}", invoiceId, api.getInvoiceWS(invoiceId).getCreateDatetime());
			invoiceWS.getBillingProcess().getOrderProcesses().
			forEach(OrderProcess -> {
				AssertJUnit.assertEquals("Associated Billing process Id",
						OrderProcess.getBillingProcessId(), invoiceWS.getBillingProcess().getId());
			});
		});

	}

	private Integer configurePluginSetCronExpression() {

		PluggableTaskWS invoiceBillingProcessLinkingTask= new PluggableTaskWS();
		invoiceBillingProcessLinkingTask.setProcessingOrder(10);
		PluggableTaskTypeWS invoiceBillingProcessLinkingTaskType = 
				api.getPluginTypeWSByClassName("com.sapienter.jbilling.server.billing.task.InvoiceBillingProcessLinkingTask");
		invoiceBillingProcessLinkingTask.setTypeId(invoiceBillingProcessLinkingTaskType.getId());    	

		invoiceBillingProcessLinkingTask.setParameters(new Hashtable<String,String>(invoiceBillingProcessLinkingTask.getParameters()));
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put("cron_exp", "10 0/1 * * * ?");
		invoiceBillingProcessLinkingTask.setParameters(parameters);

		return api.createPlugin(invoiceBillingProcessLinkingTask);
	}
}
