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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.AssertionFailedError;

import org.testng.annotations.Test;

import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;

@Test(groups = { "billing-and-discounts", "billing", "order-line-changes" }, testName = "OrderLineChangesBillingTest")
// @ContextConfiguration(classes = Feature9958TestConfig.class, loader =
// AnnotationConfigContextLoader.class)
public class OrderLineChangesBillingTest extends BillingProcessTestCase {

    private class Expectation {

        public Date       nextInvoiceDate;
        public Date       nextBillableDay;
        public BigDecimal invoiceTotal;

        public Expectation nextInvoiceDate (Date nextInvoiceDate) {
            this.nextInvoiceDate = nextInvoiceDate;
            return this;
        }

        public Expectation nextBillableDay (Date nextBillableDay) {
            this.nextBillableDay = nextBillableDay;
            return this;
        }

        public Expectation invoiceTotal (BigDecimal total) {
            this.invoiceTotal = total;
            return this;
        }
    }

    private TestScenario prePaid () {
        return new TestScenario(true);
    }

    private TestScenario postPaid () {
        return new TestScenario(false);
    }

    private TestScenario prePaidNonProRate () {
        return new TestScenario(true, false);
    }


    private class TestScenario {
        private UserWS                          user;
        private OrderWS                         order;
        private boolean                         failed;

        Map<Date, Expectation>                  expectations = new HashMap<Date, Expectation>();
        Map<Date, List<TestOrderChangeBuilder>> changes      = new HashMap<Date, List<TestOrderChangeBuilder>>();

        // Main Subscription (1 - Monthly) Pro Rating Enabled

        private TestScenario (boolean prePaid) {
            this(prePaid, true);
        }

        private TestScenario (boolean prePaid, boolean proRate) {
            user = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2015").build();
            // override default builder with proRate(true)
            TestOrderBuilder builder = orderBuilderFactory
                    .forUser(user)
                    .activeSince(2015, 1, 1)
                    .proRate(proRate)
                    // zero line
                    .orderLine(orderLineBuilder.price(new BigDecimal("0.0")).quantity(new BigDecimal("0.0")).build()) 
                    .monthly();
            if (prePaid) {
                builder.prePaid();
            }
            order = builder.build();
        }

        public void tearDown () {
            if (user != null) {
                logger.debug("Deleting user[{}]", user.getId());
                api.deleteUser(user.getId());
            }
        }

        public void applyChanges (Date date) {
            if (failed) {
                logger.debug("userId: {}, orderId: {}. skip failed scenario", user.getId(), order.getId());
                return;
            }
            if (changes.containsKey(date)) {
                for (TestOrderChangeBuilder change : changes.get(date)) {
                    change.buildNewAndApply();
                }
            } else {
                logger.debug("userId: {}, orderId: {}. no changes before billing at {}", user.getId(), order.getId(), date);
            }
        }

        public void verify (Date date) throws AssertionFailedError {
            if (failed) {
                logger.debug("userId: {}, orderId: {}. skip failed scenario", user.getId(), order.getId());
                return;
            }
            if (expectations.containsKey(date)) {
                verify(expectations.get(date));
            } else {
                logger.debug("userId: {}, orderId: {}. no expectation for billing at {}", user.getId(), order.getId(), date);
            }
        }

        private void verify (Expectation expected) throws AssertionFailedError {
            user = api.getUserWS(user.getUserId());
            order = api.getOrder(order.getId());

            logger.debug("user: {}", userDetailsAsString(user));
            logger.debug("order: {}", orderDetailsAsString(order));
            boolean isOk;
            isOk = assertEqualsBilling(S("userId: {}.", user.getId()), expected.nextInvoiceDate,
                    user.getNextInvoiceDate());
            if (!isOk) {
                failed = true;
                return;
            }
            isOk = assertEqualsBilling(S("orderId: {}.", order.getId()), expected.nextBillableDay,
                    order.getNextBillableDay());
            if (!isOk) {
                failed = true;
                return;
            }

            if (null != expected.invoiceTotal) {
                verifyInvoice(expected);
            }
        }

        private void verifyInvoice (Expectation expected) throws AssertionFailedError {
            if (failed) {
                return;
            }
            Integer[] invoiceIds = api.getLastInvoices(order.getUserId(), 1);
            InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);

            boolean isOk;
            isOk = assertEqualsBilling(S("invoiceId: {}.", invoice.getId()), expected.invoiceTotal, invoice
                    .getBalanceAsDecimal()
                    .setScale(2));
            if (!isOk) {
                failed = true;
                return;
            }

            InvoiceLineDTO[] lines = invoice.getInvoiceLines();
            // TODO verify invoice line content
        }

        public TestOrderChangeBuilder getChangeBuilder () {
            return new TestOrderChangeBuilder().forOrder(order);
        }

        private String orderDetailsAsString (OrderWS order) {
            order = api.getOrder(order.getId());
            return S("order[{}]:type: {}, period: {}, activeSince: {}, nextBillableDay: {}", order.getId(),
                    order.getBillingTypeStr(), order.getPeriodStr(), order.getActiveSince(), order.getNextBillableDay());
        }

        private String userDetailsAsString (UserWS user) {
            user = api.getUserWS(user.getId());
            return S("user[{}]: period: {}, customer: {}, nextInvoiceDate: {}", user.getId(), user
                    .getMainSubscription()
                    .getPeriodId(), user.getCustomerId(), user.getNextInvoiceDate());
        }

        public void addChange (TestOrderChangeBuilder change, Date date) {
            if (!changes.containsKey(date)) {
                changes.put(date, new ArrayList<TestOrderChangeBuilder>(1));
            }
            changes.get(date).add(change);
        }

        public void addExpecation (Expectation expected, Date date) {
            expectations.put(date, expected);
        }
    }

    private List<TestScenario> scenarios    = new ArrayList<TestScenario>();
    private List<Date>         billingDates = new ArrayList<Date>();
    {
        billingDates.add(AsDate(2015, 1, 1));
        billingDates.add(AsDate(2015, 2, 1));
        billingDates.add(AsDate(2015, 3, 1));
        billingDates.add(AsDate(2015, 4, 1));
        billingDates.add(AsDate(2015, 5, 1));
    }

    @Override
    protected void afterTestClass () throws Exception {
        for (TestScenario scenario : scenarios) {
            scenario.tearDown();
        }
    }

    @Override
    protected void prepareTestInstance () throws Exception {
        super.prepareTestInstance();

        // Monthly, Post-Paid, Quantity Change

        scenarios.add(buildTestScenarioQuantityDecreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioQuantityDecreaseForUnbIlledPeriod());
        scenarios.add(buildTestScenarioQuantityIncreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioQuantityIncreaseForUnbIlledPeriod());

        // Monthly, Post-Paid, Price Change

        scenarios.add(buildTestScenarioPriceDecreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioPriceDecreaseForUnbIlledPeriod());
        scenarios.add(buildTestScenarioPriceIncreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioPriceIncreaseForUnbIlledPeriod());

        // Monthly, Post-Paid, Quantity and Price Change

        scenarios.add(buildTestScenarioPriceAndQuantityDecreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioPriceAndQuantityDecreaseForUnbIlledPeriod());
        scenarios.add(buildTestScenarioPriceAndQuantityIncreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioPriceAndQuantityIncreaseForUnbIlledPeriod());

        // Monthly, Pre-Paid, Quantity and Price Change

        scenarios.add(buildTestScenarioPrePaidPriceAndQuantityDecreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioPrePaidPriceAndQuantityDecreaseForUnbIlledPeriod());
        scenarios.add(buildTestScenarioPrePaidPriceAndQuantityIncreaseForBIlledPeriod());
        scenarios.add(buildTestScenarioPrePaidPriceAndQuantityIncreaseForUnbIlledPeriod());

        // Monthly, Pre-Paid, Back-dated Quantity Change
        scenarios.add(buildTestScenarioPrePaidBackDatedQuantityIncreaseForBIlledPeriod());
    }

    @Test
    public void test000ConfigurationIsOk () {
        assertNotNull("jBilling API wasn't configured", api);
    }

    @Test(enabled = true)
    public void test001BIllng () {
        for (Date date : billingDates) {
            for (TestScenario scenario : scenarios) {
                scenario.applyChanges(date);
            }
            billingProcessBuilder.triggerForDate(date);
            for (TestScenario scenario : scenarios) {
                scenario.verify(date);
            }
            for (String error : getErrors()) {
                logger.debug(error);
            }
            assertEquals(S("Tests failed on billing run for date {}", date), 0, getErrors().size());
        }
    }

    private TestScenario buildTestScenarioQuantityDecreaseForBIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(-2.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("45.00" /* "34.84" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("40.00"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioQuantityDecreaseForUnbIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(-1.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 2, 1));
        change = scenario.getChangeBuilder().applicationDate("02/25/2015").quantity(-1.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("50.00"/* "47.42" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("46.79" /* "42.68" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("40.00"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioQuantityIncreaseForBIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/05/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("43.55"/* "41.93" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        change = scenario.getChangeBuilder().applicationDate("04/15/2015").quantity(1.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 4, 1));
        change = scenario.getChangeBuilder().applicationDate("04/25/2015").quantity(4.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 4, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("50.00" /* "84.72" */));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 5, 1))
                .nextInvoiceDate(AsDate(2015, 6, 1))
                .invoiceTotal(new BigDecimal("56.67"));
        scenario.addExpecation(expected, AsDate(2015, 5, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioQuantityIncreaseForUnbIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/05/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(2.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 2, 1));
        change = scenario.getChangeBuilder().applicationDate("02/25/2015").quantity(2.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("43.55"/* "43.86" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("56.43" /* "64.64" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("70.00"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    // Monthly, Post-Paid, Price Change

    private TestScenario buildTestScenarioPriceDecreaseForBIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        change = scenario.getChangeBuilder().applicationDate("03/15/2015").quantity(0.0).price(3.0);
        scenario.addChange(change, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("50.00" /* "19.68" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("39.03"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPriceDecreaseForUnbIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(0.0).price(4.0);
        scenario.addChange(change, AsDate(2015, 2, 1));
        change = scenario.getChangeBuilder().applicationDate("02/25/2015").quantity(0.0).price(3.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("50.00"/* "44.84" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("43.57" /* "35.36" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("30.00"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPriceIncreaseForBIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/05/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("43.55"/* "41.93" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        change = scenario.getChangeBuilder().applicationDate("04/15/2015").quantity(0.0).price(6.0);
        scenario.addChange(change, AsDate(2015, 4, 1));
        change = scenario.getChangeBuilder().applicationDate("04/25/2015").quantity(0.0).price(10.0);
        scenario.addChange(change, AsDate(2015, 4, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("50.00" /* "119.44" */));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 5, 1))
                .nextInvoiceDate(AsDate(2015, 6, 1))
                .invoiceTotal(new BigDecimal("63.33"));
        scenario.addExpecation(expected, AsDate(2015, 5, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPriceIncreaseForUnbIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/05/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(0.0).price(7.0);
        scenario.addChange(change, AsDate(2015, 2, 1));
        change = scenario.getChangeBuilder().applicationDate("02/25/2015").quantity(0.0).price(9.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("43.55" /* "45.80" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("62.86" /* "79.28" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("90.00"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    // Monthly, Post-Paid, Price and Quantity Change

    private TestScenario buildTestScenarioPriceAndQuantityDecreaseForBIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        change = scenario.getChangeBuilder().applicationDate("03/15/2015").quantity(-2.0).price(4.0);
        scenario.addChange(change, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("50.00" /* "22.72" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("40.13"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPriceAndQuantityDecreaseForUnbIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(-2.0).price(4.0);
        scenario.addChange(change, AsDate(2015, 2, 1));
        change = scenario.getChangeBuilder().applicationDate("02/25/2015").quantity(-2.0).price(3.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("50.00"/* "40.72" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("39.00" /* "30.51" */));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("18.00"));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPriceAndQuantityIncreaseForBIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/05/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("43.55"/* "41.93" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        change = scenario.getChangeBuilder().applicationDate("04/15/2015").quantity(2.0).price(6.0);
        scenario.addChange(change, AsDate(2015, 4, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 5, 1))
                .invoiceTotal(new BigDecimal("50.00" /* "105.35" */));
        scenario.addExpecation(expected, AsDate(2015, 4, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 5, 1))
                .nextInvoiceDate(AsDate(2015, 6, 1))
                .invoiceTotal(new BigDecimal("61.73"));
        scenario.addExpecation(expected, AsDate(2015, 5, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPriceAndQuantityIncreaseForUnbIlledPeriod () {
        TestScenario scenario = postPaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/05/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation().nextBillableDay(AsDate(2015, 1, 1)).nextInvoiceDate(AsDate(2015, 2, 1));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(2.0).price(7.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("43.55" /* "59.44" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("67.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        return scenario;
    }

    // Monthly, Pre-Paid, Quantity and Price Change

    private TestScenario buildTestScenarioPrePaidPriceAndQuantityDecreaseForBIlledPeriod () {
        TestScenario scenario = prePaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 2, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/25/2015").quantity(-2.0).price(4.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("47.43" /* "28.53" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("32.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPrePaidPriceAndQuantityDecreaseForUnbIlledPeriod () {
        TestScenario scenario = prePaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 2, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(-2.0).price(4.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("41.00" /* "41.65" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("32.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPrePaidPriceAndQuantityIncreaseForBIlledPeriod () {
        TestScenario scenario = prePaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 2, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/20/2015").quantity(2.0).price(6.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("57.07"/* "79.78" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("72.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPrePaidPriceAndQuantityIncreaseForUnbIlledPeriod () {
        TestScenario scenario = prePaid();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(10.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 2, 1))
                .invoiceTotal(new BigDecimal("50.00"));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/20/2015").quantity(2.0).price(6.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("57.07" /* "56.27" */));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 4, 1))
                .nextInvoiceDate(AsDate(2015, 4, 1))
                .invoiceTotal(new BigDecimal("72.00"));
        scenario.addExpecation(expected, AsDate(2015, 3, 1));

        return scenario;
    }

    private TestScenario buildTestScenarioPrePaidBackDatedQuantityIncreaseForBIlledPeriod () {
        TestScenario scenario = prePaidNonProRate();
        TestOrderChangeBuilder change;
        Expectation expected;

        change = scenario.getChangeBuilder().applicationDate("01/01/2015").quantity(2.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 1, 1));
        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 2, 1))
                .nextInvoiceDate(AsDate(2015, 2, 1))
                .invoiceTotal(new BigDecimal("10.00"));
        scenario.addExpecation(expected, AsDate(2015, 1, 1));

        change = scenario.getChangeBuilder().applicationDate("02/15/2015").quantity(1.0).price(5.0);
        scenario.addChange(change, AsDate(2015, 2, 1));

        expected = new Expectation()
                .nextBillableDay(AsDate(2015, 3, 1))
                .nextInvoiceDate(AsDate(2015, 3, 1))
                .invoiceTotal(new BigDecimal("15.00"));
        scenario.addExpecation(expected, AsDate(2015, 2, 1));

        return scenario;
    }

}
