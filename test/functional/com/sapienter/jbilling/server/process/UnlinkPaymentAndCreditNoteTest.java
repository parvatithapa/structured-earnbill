package com.sapienter.jbilling.server.process;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeUtil;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.invoiceSummary.InvoiceSummaryScenarioBuilder;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;

/**
 * UnlinkPaymentAndCreditNoteTest
 *
 * @author Pranay Raherkar
 * @since 11/06/2018
 */
@Test(groups = { "integration" }, testName = "UnlinkPaymentAndCreditNoteTest", priority = 20)
public class UnlinkPaymentAndCreditNoteTest {
    private static final Logger logger = LoggerFactory.getLogger(AgeingStopActivationOnPaymentTest.class);

    private TestBuilder testBuilder;
    private TestEnvironment environment;
    private EnvironmentHelper environmentHelper;

    private static final String CATEGORY_CODE = "TestCategory";
    private static final String PRODUCT_CODE = "TestProduct";
    private static final String PRODUCT_CODE1 = "TestProduct1";
    private static final String ACCOUNT_TYPE_CODE = "TestAccountType";

    private static final String CUSTOMER_CODE1 = "TestCustomer1";
    private static final String CUSTOMER_CODE2 = "TestCustomer2";
    public final static int ONE_TIME_ORDER_PERIOD = 1;
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final String Order_CODE = "monthlyOrder";

    private Integer CATEGORY_ID;

    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();
        environment = testBuilder.getTestEnvironment();
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
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(CATEGORY_ID)
            .withFlatPrice("100.00").build();
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE1).global(true).withType(CATEGORY_ID)
            .withFlatPrice("-100.00").build();
            envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();
        });
    }

    @Test
    public void test001UnlinkPaymentFromInvoice(){
        final Date activeSince = FullCreativeUtil.getDate(0,01,2016);
        final Date nextInvoiceDate = FullCreativeUtil.getDate(1,01,2016);

        try{
            testBuilder.given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Integer userId = createCustomer(envBuilder, CUSTOMER_CODE1, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
                logger.debug("### userId: {}", userId);
                assertNotNull("UserId should not be null",userId);
                envBuilder.orderBuilder(api)
                       .forUser(userId)
                        .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(activeSince)
                        .withEffectiveDate(activeSince)
                        .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                        .withDueDateValue(Integer.valueOf(1))
                        .withCodeForTests("Order")
                        .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                        .build();
                logger.debug("### order Id: {}", api.getLatestOrder(userId));
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                Integer[] invoiceIdsOfUser= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE1),nextInvoiceDate, null, null, false);
                logger.debug("### invoiceIdsOfUser: {}", invoiceIdsOfUser[0]);
                InvoiceWS invoice = api.getInvoiceWS(invoiceIdsOfUser[0]);
                assertNotNull("invoiceIdsOfUser should not be null",invoice);
                //invoice status should be in unpaid status
                assertEquals("Unpaid", invoice.getStatusDescr());
                Calendar paymentDate = Calendar.getInstance();
                paymentDate.set(Calendar.YEAR, 2016);
                paymentDate.set(Calendar.MONTH, 2);
                paymentDate.set(Calendar.DAY_OF_MONTH, 5);
                InvoiceSummaryScenarioBuilder scenarioBuilder = new InvoiceSummaryScenarioBuilder(testBuilder);
                scenarioBuilder.selectUserByName(CUSTOMER_CODE1)
                         .makePayment("100.00", paymentDate.getTime(), false);
                Integer[] payment = api.getPaymentsByUserId(environment.idForCode(CUSTOMER_CODE1));
                api.applyPaymentsToInvoices(environment.idForCode(CUSTOMER_CODE1));
                invoice = api.getInvoiceWS(invoice.getId());
                assertEquals("Paid", invoice.getStatusDescr());
                api.removePaymentLink(invoice.getId(), payment[0]);
                assertNotNull("Payment should not be null",payment);
                invoice = api.getInvoiceWS(invoice.getId());
                assertEquals("Unpaid", invoice.getStatusDescr());
            });
        }finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getPaymentsByUserId(environment.idForCode(CUSTOMER_CODE1)))
                    .forEach(paymentId -> {
                        api.deletePayment(paymentId);
                     });
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE1), 10, 0))
                    .forEach(invoice -> {
                        api.deleteInvoice(invoice.getId());
                    });
            api.deleteUser(environment.idForCode(CUSTOMER_CODE1));
	}
    }

    @Test
    public void test002UnlinkInvoiceFromCreditNote(){
        final Date activeSince = FullCreativeUtil.getDate(0,01,2016);
        final Date nextInvoiceDate = FullCreativeUtil.getDate(1,01,2016);

        try{
            testBuilder.given(envBuilder -> {
                final JbillingAPI api = envBuilder.getPrancingPonyApi();
                Integer userId = createCustomer(envBuilder, CUSTOMER_CODE2, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), nextInvoiceDate);
                logger.debug("### userId: {}", userId);
                assertNotNull("UserId should not be null",userId);
                envBuilder.orderBuilder(api)
                       .forUser(userId)
                        .withProducts(envBuilder.env().idForCode(PRODUCT_CODE1))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(activeSince)
                        .withEffectiveDate(activeSince)
                        .withPeriod(ONE_TIME_ORDER_PERIOD)
                        .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                        .withDueDateValue(Integer.valueOf(1))
                        .withCodeForTests("Order")
                        .build();
                envBuilder.orderBuilder(api)
                        .forUser(userId)
                        .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
                        .withBillingTypeId(Constants.ORDER_BILLING_POST_PAID)
                        .withActiveSince(FullCreativeUtil.getDate(1,01,2016))
                        .withEffectiveDate(FullCreativeUtil.getDate(1,01,2016))
                        .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
                        .withDueDateValue(Integer.valueOf(1))
                        .withCodeForTests(Order_CODE)
                        .withPeriod(environmentHelper.getOrderPeriodMonth(api))
                        .build();
                logger.debug("### order Id: {}", api.getLatestOrder(userId));
            }).test((env)-> {
                JbillingAPI api = env.getPrancingPonyApi();
                //create credit note
                Integer[] invoiceIdsOfUser= api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE2),nextInvoiceDate, null, null, false);
                logger.debug("### invoiceIdsOfUser: {}", invoiceIdsOfUser[0]);
                InvoiceWS invoice = api.getInvoiceWS(invoiceIdsOfUser[0]);
                Integer[] creditNote = api.getLastCreditNotes(environment.idForCode(CUSTOMER_CODE2),1);
                assertNotNull("invoiceIdsOfUser should not be null",invoice);
                //invoice status should be in unpaid status
                assertEquals("Paid", invoice.getStatusDescr());

                Calendar invoiceDate = Calendar.getInstance();
                invoiceDate.set(Calendar.YEAR, 2016);
                invoiceDate.set(Calendar.MONTH, 2);
                invoiceDate.set(Calendar.DAY_OF_MONTH, 1);
                UserWS userWS = api.getUserWS(environment.idForCode(CUSTOMER_CODE2));
                userWS.setNextInvoiceDate(invoiceDate.getTime());
                api.updateUser(userWS);
                //create invoice for monthly order
                Integer[] invoiceIdsOfUser1 = api.createInvoiceWithDate(environment.idForCode(CUSTOMER_CODE2),invoiceDate.getTime(), null, null, false);
                assertNotNull("Invoice should be null",invoiceIdsOfUser1);
                invoice = api.getInvoiceWS(invoiceIdsOfUser1[0]);
                assertEquals("Paid", invoice.getStatusDescr());

                //unlink credit note from invice
                api.removeCreditNoteLink(invoice.getId(), creditNote[0]);
                invoice = api.getInvoiceWS(invoice.getId());
                assertEquals("Unpaid", invoice.getStatusDescr());
            });
        }finally {
            final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
            Arrays.stream(api.getPaymentsByUserId(environment.idForCode(CUSTOMER_CODE2)))
                    .forEach(paymentId -> {
                        api.deletePayment(paymentId);
                     });
            Arrays.stream(api.getUserInvoicesPage(testBuilder.getTestEnvironment().idForCode(CUSTOMER_CODE2), 10, 0))
                    .forEach(invoice -> {
                        api.deleteInvoice(invoice.getId());
                    });
            api.deleteUser(environment.idForCode(CUSTOMER_CODE2));
	}
    }
    private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
        final JbillingAPI api = envBuilder.getPrancingPonyApi();
        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                    .withUsername(code).withAccountTypeId(accountTypeId)
                    .withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodMonth(api), getDay(nid)));
        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nid);
        api.updateUser(user);
        return user.getId();
    }

    private static Integer getDay(Date inputDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(inputDate);
        return Integer.valueOf(cal.get(Calendar.DAY_OF_MONTH));
    }

}
