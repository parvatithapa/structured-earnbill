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
import static org.testng.AssertJUnit.assertNull;

import java.util.*;

import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;

/**
 * MonthlyBillingCycleTest Test class for Monthly Billing Cycle.
 * 
 * @author Amol Gadre On 15 June 2014
 * @author Igor Poteryaev
 */
@Test(groups = { "billing-and-discounts", "billing" }, testName = "MonthlyBillingCycleTest")
public class MonthlyBillingCycleTest extends BillingProcessTestCase {

    protected ParentChildScenarioVerifierFactory customScenarioVerifier;

    private List<InvoiceLineDTO> filteredLines = new ArrayList<>();
    public final static Comparator<InvoiceLineDTO> OrderInvoiceLineById = (line1, line2) -> line1.getId() - line2.getId();

    @Override
    protected void prepareTestInstance () throws Exception {
        super.prepareTestInstance();
        customScenarioVerifier = new ParentChildScenarioVerifierFactory();
    }

    @Test
    public void testMonthlyBillingCycleTest () throws Exception {
        // ------------------ Non Prorating Scenarios -- H Series
        // ** H1 Scenario User and Order **
        // Create the user for H1 scenario and update its next invoice date to
        // 01-Feb
        UserWS userH1 = monthlyBilledUserBuilder.nextInvoiceDate("02/01/2010").build();
        OrderWS orderH1 = orderBuilderFactory.forUser(userH1).daily().activeSince("01/01/2010").build();
        logger.debug("OrderH1 id: {}", orderH1.getId());
        logger.debug("UserH1 id: {}", userH1.getUserId());

        // ------------------
        // ** H2 Scenario User and Order **
        // Create the user for H2 scenario and update its next invoice date to
        // 01-Feb
        UserWS userH2 = monthlyBilledUserBuilder.nextInvoiceDate("02/01/2010").build();
        OrderWS orderH2 = orderBuilderFactory.forUser(userH2).weekly().activeSince("01/01/2010").build();
        logger.debug("OrderH2 id: {}", orderH2.getId());
        logger.debug("UserH2 id: {}", userH2.getUserId());

        // ------------------
        // ** H3 Scenario User and Order **
        // Create the user for H3 scenario and update its next invoice date to
        // 01-Jan
        UserWS userH3 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderH3 = orderBuilderFactory.forUser(userH3).weekly().prePaid().activeSince("01/01/2010")
                .activeUntil("04/10/2010").build();
        logger.debug("OrderH3 id: {}", orderH3.getId());
        logger.debug("UserH3 id: {}", userH3.getUserId());

        // ------------------
        // ** H4 Scenario User and Order **
        // Create the user for H4 scenario and update its next invoice date to
        // 01-Feb
        UserWS userH4 = monthlyBilledUserBuilder.nextInvoiceDate("02/01/2010").build();
        // Create the semi-monthly post paid order for H4 scenario
        OrderWS orderH4 = orderBuilderFactory.forUser(userH4).semiMonthly().activeSince("01/01/2010").build();
        logger.debug("OrderH4 id: {}", orderH4.getId());
        logger.debug("UserH4 id: {}", userH4.getUserId());

        // ------------------
        // ** H5 Scenario User and Order **
        // Create the user for H5 scenario and update its next invoice date to
        // 01-Jan
        UserWS userH5 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderH5 = orderBuilderFactory.forUser(userH5).monthly().activeSince("01/15/2010").build();
        logger.debug("OrderH5 id: {}", orderH5.getId());
        logger.debug("UserH5 id: {}", userH5.getUserId());

        // ------------------
        // ** H6 Scenario User and Order **
        // Create the user for H6 scenario and update its next invoice date to
        // 01-Feb
        UserWS userH6 = monthlyBilledUserBuilder.nextInvoiceDate("02/01/2010").build();
        OrderWS orderH6 = orderBuilderFactory.forUser(userH6).monthly().prePaid().activeSince("01/15/2010").build();
        logger.debug("OrderH6 id: {}", orderH6.getId());
        logger.debug("UserH6 id: {}", userH6.getUserId());

        // ------------------
        // ** H7 Scenario User and Order **
        // Create the user for H7 scenario and update its next invoice date to
        // 01-Feb
        UserWS userH7 = monthlyBilledUserBuilder.nextInvoiceDate("02/01/2010").build();
        OrderWS orderH7 = orderBuilderFactory.forUser(userH7).monthly().prePaid().activeSince("01/01/2010").build();
        logger.debug("OrderH7 id: {}", orderH7.getId());
        logger.debug("UserH7 id: {}", userH7.getUserId());

        // ------------------ Prorating Scenarios -- G Series
        // ** G1 Scenario User and Order **
        // Create the user for G1 scenario and update its next invoice date to
        // 01-Jan
        Integer orderIdG1 = null;
        try {
            UserWS userG1 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
            logger.debug("UserG1 id: {}", userG1.getUserId());
            OrderWS orderG1 = orderBuilderFactory.forUser(userG1).daily().proRate(true).build();
            orderIdG1 = orderG1.getId();
        } catch (SessionInternalError sie) {
            assertContainsErrorBilling(sie, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            logger.debug("OrderG1 id: {}", orderIdG1);
            assertNull("orderIdG1 should be null as orderG1 would not get created", orderIdG1);
        }

        // ------------------
        // ** G2 Scenario User and Order **
        // Create the user for G2 scenario and update its next invoice date to
        // 01-Jan
        Integer orderIdG2 = null;
        try {
            UserWS userG2 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
            logger.debug("UserG2 id: {}", userG2.getUserId());
            OrderWS orderG2 = orderBuilderFactory.forUser(userG2).weekly().proRate(true).build();
            orderIdG2 = orderG2.getId();
        } catch (SessionInternalError sie) {
            assertContainsErrorBilling(sie, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            logger.debug("OrderG2 id: {}", orderIdG2);
            assertNull("orderIdG2 should be null as orderG2 would not get created", orderIdG2);
        }

        // ------------------
        // ** G4 Scenario User and Order **
        // Create the user for G4 scenario and update its next invoice date to
        // 01-Jan
        Integer orderIdG4 = null;
        try {
            UserWS userG4 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
            logger.debug("UserG4 id: {}", userG4.getUserId());
            OrderWS orderG4 = orderBuilderFactory.forUser(userG4).semiMonthly().proRate(true).build();
            orderIdG4 = orderG4.getId();
        } catch (SessionInternalError sie) {
            assertContainsErrorBilling(sie, "OrderWS,billingCycleUnit,order.period.unit.should.equal", null);
        } finally {
            logger.debug("OrderG4 id: {}", orderIdG4);
            assertNull("orderIdG4 should be null as orderG4 would not get created", orderIdG4);
        }

        // ------------------
        // ** G5 Scenario User and Order **
        // Create the user for G5 scenario and update its next invoice date to
        // 01-Jan
        UserWS userG5 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderG5 = orderBuilderFactory.forUser(userG5).monthly().activeSince("01/15/2010").proRate(true).build();
        logger.debug("OrderG5 id: {}", orderG5.getId());
        logger.debug("UserG5 id: {}", userG5.getUserId());

        // ------------------
        // ** G6 Scenario User and Order **
        // Create the user for G6 scenario and update its next invoice date to
        // 01-Jan
        UserWS userG6 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderG6 = orderBuilderFactory.forUser(userG6).monthly().prePaid().activeSince("01/15/2010")
                .proRate(true).build();
        logger.debug("OrderG6 id: {}", orderG6.getId());
        logger.debug("UserG6 id: {}", userG6.getUserId());

        // ------------------
        // ** G7 Scenario User and Order **
        // Create the user for G7 scenario and update its next invoice date to
        // 01-Jan
        UserWS userG7 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderG7 = orderBuilderFactory.forUser(userG7).monthly().activeSince("12/15/2009").proRate(true).build();
        logger.debug("OrderG7 id: {}", orderG7.getId());
        logger.debug("UserG7 id: {}", userG7.getUserId());

        // ------------------
        // ** G8 Scenario User and Order **
        // Create the user for G8 scenario and update its next invoice date to
        // 01-Jan
        UserWS userG8 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderG8 = orderBuilderFactory.forUser(userG8).monthly().prePaid().activeSince("12/15/2009")
                .proRate(true).build();
        logger.debug("OrderG8 id: {}", orderG8.getId());
        logger.debug("UserG8 id: {}", userG8.getUserId());

        // Create user monthly Billing cycle with main subscription 1 monthly
        UserWS userG9 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").build();
        OrderWS orderG9 = orderBuilderFactory.forUser(userG9).monthly().proRate(true).build();
        logger.debug("OrderG9 id: {}", orderG9.getId());
        logger.debug("UserG9 id: {}", userG9.getUserId());

        // ** J1 Scenario User and Order **
        // Create user monthly Billing cycle with main subscription 1 monthly
        UserWS parentUserJ1 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").isParent().build();
        OrderWS parentOrderJ1 = orderBuilderFactory.forUser(parentUserJ1).monthly().proRate(true).build();
        logger.debug("orderIdJ1 id: {}", parentOrderJ1.getId());
        logger.debug("parentUserJ1 id: {}", parentUserJ1.getUserId());

        // ** J1C1 Scenario User and Order **
        UserWS childUserJ1C1 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").withParent(parentUserJ1).build();
        OrderWS childOrderJ1C1 = orderBuilderFactory.forUser(childUserJ1C1).monthly().proRate(true).build();
        logger.debug("childOrderIdJ1C1 id: {}", childOrderJ1C1.getId());
        logger.debug("childUserJ1C1 id: {}", childUserJ1C1.getUserId());

        // ** J2 Scenario User and Order **
        // Create user monthly Billing cycle with main subscription 1 monthly
        UserWS parentUserJ2 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").isParent().build();
        OrderWS parentOrderJ2 = orderBuilderFactory.forUser(parentUserJ2).monthly().prePaid().proRate(true).build();
        logger.debug("orderIdJ2 id: {}", parentOrderJ2.getId());
        logger.debug("parentUserJ2 id: {}", parentUserJ2.getUserId());

        // ** J1C1 Scenario User and Order **
        UserWS childUserJ2C2 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").withParent(parentUserJ2).build();
        OrderWS childOrderJ2C2 = orderBuilderFactory.forUser(childUserJ2C2).monthly().prePaid().proRate(true).build();
        logger.debug("childOrderIdJ2C2 id: {}", childOrderJ2C2.getId());
        logger.debug("childUserJ2C2 id: {}", childUserJ2C2.getUserId());

        // ** J3 Scenario User and Order **
        // Create user monthly Billing cycle with main subscription 1 monthly
        UserWS parentUserJ3 = monthlyBilledUserBuilder.nextInvoiceDate("01/01/2010").isParent().build();
        OrderWS parentOrderJ3 = orderBuilderFactory.forUser(parentUserJ3).monthly().proRate(true).build();
        logger.debug("parentOrderIdJ3 id: {}", parentOrderJ3.getId());
        logger.debug("parentUserJ3 id: {}", parentUserJ3.getUserId());

        // ** J3C3 Scenario User and Order **
        UserWS childUserJ3C3 = monthlyBilledUserBuilder.billingDay(10).nextInvoiceDate("01/10/2010")
                .withParent(parentUserJ3).invoiceChild(true).build();
        OrderWS childOrderJ3C3 = orderBuilderFactory.forUser(childUserJ3C3).monthly().proRate(true).build();
        logger.debug("childOrderIdJ3C3 id: {}", childOrderJ3C3.getId());
        logger.debug("childUserJ3C3 id: {}", childUserJ3C3.getUserId());

        triggerBilling(AsDate("01/01/2010"));

        scenarioVerifier.forOrder(orderH3).nbd("02/05/2010").nid("02/01/2010").from("01/01/2010").to("01/07/2010")
                .invoiceLines(5).verify();
        scenarioVerifier.forOrder(orderH5).nbd("01/15/2010").nid("02/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderG5).nbd("01/15/2010").nid("02/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderG6).nbd("02/01/2010").nid("02/01/2010").from("01/15/2010").to("01/31/2010")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG7).nbd("01/01/2010").nid("02/01/2010").from("12/15/2009").to("12/31/2009")
                .invoiceLines(1).verify();

        // Testing G8 its has one partial and one full monthly order period
        testG8(userG8, orderG8, AsDate("02/01/2010"), AsDate("02/01/2010"));

        scenarioVerifier.forOrder(orderG9).nbd("01/01/2010").nid("02/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ1).nbd("01/01/2010").nid("02/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(childOrderJ1C1).nbd("01/01/2010").nid("02/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ2).nbd("02/01/2010").nid("02/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(2).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ2C2).nbd("02/01/2010").nid("02/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ3).nbd("01/01/2010").nid("02/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(0).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/01/2010"));

        // Run the billing process for 10th Jan
        triggerBilling(AsDate("01/10/2010"));

        // ????
        customScenarioVerifier.forOrder(childOrderJ3C3).nbd("01/10/2010").nid("02/10/2010").from("01/01/2010")
                .to("01/09/2010").invoiceLines(0).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/10/2010"));

        triggerBilling(AsDate("01/15/2010"));

        scenarioVerifier.forOrder(orderH7).nbd("01/01/2010").nid("02/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(0).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/15/2010"));

        // Run the billing process for 1st Feb
        triggerBilling(AsDate("02/01/2010"));

        scenarioVerifier.forOrder(orderH1).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010").to("01/01/2010")
                .invoiceLines(31).verify();
        scenarioVerifier.forOrder(orderH2).nbd("01/29/2010").nid("03/01/2010").from("01/01/2010").to("01/07/2010")
                .invoiceLines(4).verify();
        scenarioVerifier.forOrder(orderH3).nbd("03/05/2010").nid("03/01/2010").from("02/05/2010").to("02/11/2010")
                .invoiceLines(4).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH4).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010").to("01/15/2010")
                .invoiceLines(2).verify();
        scenarioVerifier.forOrder(orderH5).nbd("01/15/2010").nid("03/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderH6).nbd("03/15/2010").nid("03/01/2010").from("01/15/2010").to("02/14/2010")
                .invoiceLines(2).verify();
        scenarioVerifier.forOrder(orderH7).nbd("03/01/2010").nid("03/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(2).verify();
        scenarioVerifier.forOrder(orderG5).nbd("02/01/2010").nid("03/01/2010").from("01/15/2010").to("01/31/2010")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG6).nbd("03/01/2010").nid("03/01/2010").from("02/01/2010").to("02/28/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG7).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG8).nbd("03/01/2010").nid("03/01/2010").from("02/01/2010").to("02/28/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG9).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010").to("01/31/2010")
                .invoiceLines(1).verify();

        customScenarioVerifier.forOrder(parentOrderJ1).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(2).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ1C1).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ2).nbd("03/01/2010").nid("03/01/2010").from("02/01/2010")
                .to("02/28/2010").invoiceLines(2).dueInvoiceLines(1).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ2C2).nbd("03/01/2010").nid("03/01/2010").from("02/01/2010")
                .to("02/28/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ3).nbd("02/01/2010").nid("03/01/2010").from("01/01/2010")
                .to("01/31/2010").invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/01/2010"));

        triggerBilling(AsDate("02/10/2010"));

        customScenarioVerifier.forOrder(childOrderJ3C3).nbd("02/10/2010").nid("03/10/2010").from("01/10/2010")
                .to("02/09/2010").invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/10/2010"));

        triggerBilling(AsDate("03/01/2010"));

        scenarioVerifier.forOrder(orderH1).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010").to("02/01/2010")
                .invoiceLines(28).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH2).nbd("02/26/2010").nid("04/01/2010").from("01/29/2010").to("02/04/2010")
                .invoiceLines(4).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH3).nbd("04/02/2010").nid("04/01/2010").from("03/05/2010").to("03/11/2010")
                .invoiceLines(4).dueInvoiceLines(2).verify();
        //?? [2014-11-24 igor.poteryaev@jbilling.com] how it worked ?? .from("01/01/2010").to("01/15/2010")
        scenarioVerifier.forOrder(orderH4).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010").to("02/15/2010")
                .invoiceLines(2).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH5).nbd("02/15/2010").nid("04/01/2010").from("01/15/2010").to("02/14/2010")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH6).nbd("04/15/2010").nid("04/01/2010").from("03/15/2010").to("04/14/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH7).nbd("04/01/2010").nid("04/01/2010").from("03/01/2010").to("03/31/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG5).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010").to("02/28/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderG6).nbd("04/01/2010").nid("04/01/2010").from("03/01/2010").to("03/31/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderG7).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010").to("02/28/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderG8).nbd("04/01/2010").nid("04/01/2010").from("03/01/2010").to("03/31/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderG9).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010").to("02/28/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        customScenarioVerifier.forOrder(parentOrderJ1).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010")
                .to("02/28/2010").invoiceLines(2).dueInvoiceLines(1).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ1C1).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010")
                .to("02/28/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ2).nbd("04/01/2010").nid("04/01/2010").from("03/01/2010")
                .to("03/31/2010").invoiceLines(2).dueInvoiceLines(2).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ2C2).nbd("04/01/2010").nid("04/01/2010").from("03/01/2010")
                .to("03/31/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ3).nbd("03/01/2010").nid("04/01/2010").from("02/01/2010")
                .to("02/28/2010").invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("03/01/2010"));

        triggerBilling(AsDate("03/10/2010"));

        customScenarioVerifier.forOrder(childOrderJ3C3).nbd("03/10/2010").nid("04/10/2010").from("02/10/2010")
                .to("03/09/2010").invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("03/10/2010"));

        triggerBilling(AsDate("04/01/2010"));

        scenarioVerifier.forOrder(orderH2).nbd("03/26/2010").nid("05/01/2010").from("02/26/2010").to("03/04/2010")
                .invoiceLines(4).dueInvoiceLines(2).verify();

        // Testing H3
        logger.debug("ORDER ID H3  before method{}", orderH3.getId());
        testH3(userH3, orderH3, AsDate("04/02/2010"), AsDate("05/01/2010"));

        scenarioVerifier.forOrder(orderH4).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010").to("03/15/2010")
                .invoiceLines(2).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderH5).nbd("03/15/2010").nid("05/01/2010").from("02/15/2010").to("03/14/2010")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderH6).nbd("05/15/2010").nid("05/01/2010").from("04/15/2010").to("05/14/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderH7).nbd("05/01/2010").nid("05/01/2010").from("04/01/2010").to("04/30/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderG5).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010").to("03/31/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderG6).nbd("05/01/2010").nid("05/01/2010").from("04/01/2010").to("04/30/2010")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderG7).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010").to("03/31/2010")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderG8).nbd("05/01/2010").nid("05/01/2010").from("04/01/2010").to("04/30/2010")
                .invoiceLines(1).dueInvoiceLines(3).verify();
        scenarioVerifier.forOrder(orderG9).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010").to("03/31/2010")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        customScenarioVerifier.forOrder(parentOrderJ1).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010")
                .to("03/31/2010").invoiceLines(2).dueInvoiceLines(2).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ1C1).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010")
                .to("03/31/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ2).nbd("05/01/2010").nid("05/01/2010").from("04/01/2010")
                .to("04/30/2010").invoiceLines(2).dueInvoiceLines(3).verify();
        // ????
        customScenarioVerifier.forOrder(childOrderJ2C2).nbd("05/01/2010").nid("05/01/2010").from("04/01/2010")
                .to("04/30/2010").invoiceLines(0).verify();
        customScenarioVerifier.forOrder(parentOrderJ3).nbd("04/01/2010").nid("05/01/2010").from("03/01/2010")
                .to("03/31/2010").invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("04/01/2010"));

        triggerBilling(AsDate("04/10/2010"));

        customScenarioVerifier.forOrder(childOrderJ3C3).nbd("04/10/2010").nid("05/10/2010").from("03/10/2010")
                .to("04/09/2010").invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("04/10/2010"));
    }

    private void testG8 (UserWS userF3, OrderWS orderF3, Date orderNextBilableDate, Date userNextInvoiceDate) {

        InvoiceWS invoice = api.getInvoiceWS(api.getLastInvoices(userF3.getUserId(), 1)[0]);
        logger.debug("Generated Invoice Id :{}", invoice.getId());
        logger.debug("Total Invoice lines  :{}", invoice.getInvoiceLines().length);
        assertEquals("Invoice Lines should be 2 ", 2, invoice.getInvoiceLines().length);
        api.getOrderProcessesByInvoice(invoice.getId());

        for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
            logger.debug("Getting all invoice lines : {}", line);
            if (! line.getDescription().contains("Carried Invoice number")) {
                filteredLines.add(line);
                logger.debug("Getting all invoice lines : {}", line);
            }
        }
        filteredLines.sort(OrderInvoiceLineById);

        InvoiceLineDTO line = filteredLines.get(0);
        assertEquals("1st description for Invoice should be: ",
                S("Order line: {} Period from 12/15/2009 to 12/31/2009", line.getItemId()),
                line.getDescription());

        line = filteredLines.get(1);
        assertEquals("2nd description for Invoice should be: ",
                S("Order line: {} Period from 01/01/2010 to 01/31/2010", line.getItemId()),
                line.getDescription());

        assertNextBillableDay(api.getOrder(orderF3.getId()), orderNextBilableDate);
        assertNextInvoiceDate(api.getUserWS(orderF3.getUserId()), userNextInvoiceDate);
    }

    private void testH3 (UserWS userF3, OrderWS orderF3, Date orderNextBilableDate, Date userNextInvoiceDate) {

        InvoiceWS invoice = api.getInvoiceWS(api.getLastInvoices(userF3.getUserId(), 1)[0]);
        logger.debug("Generated Invoice Id :{}", invoice.getId());
        logger.debug("Total Invoice lines  :{}", invoice.getInvoiceLines().length);
        assertEquals("Invoice Lines should be 5=2+3(due) ", 5, invoice.getInvoiceLines().length);
        api.getOrderProcessesByInvoice(invoice.getId());

        filteredLines.clear();
        for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
            logger.debug("Getting all invoice lines : {}", line);
            if (! line.getDescription().contains("Carried Invoice number")) {
                filteredLines.add(line);
                logger.debug("Getting all invoice lines : {}", line);
            }
        }
        filteredLines.sort(OrderInvoiceLineById);

        InvoiceLineDTO line = filteredLines.get(0);
        assertEquals("1st description for Invoice should be: ",
                S("Order line: {} Period from 04/02/2010 to 04/08/2010", line.getItemId()),
                line.getDescription());

        line = filteredLines.get(1);
        assertEquals("2nd description for Invoice should be: ",
                S("Order line: {} Period from 04/09/2010 to 04/10/2010", line.getItemId()),
                line.getDescription());

        assertNextBillableDay(api.getOrder(orderF3.getId()), null);
        assertNextInvoiceDate(api.getUserWS(orderF3.getUserId()), userNextInvoiceDate);
    }

    public class ParentChildScenarioVerifierFactory {
        public ParentChildScenarioVerifier forOrder (OrderWS order) {
            return new ParentChildScenarioVerifier(order);
        }
    }

    public class ParentChildScenarioVerifier extends ScenarioVerifier {

        ParentChildScenarioVerifier (OrderWS order) {
            super(order);
        }

        @Override
        protected void verifyInvoiceLines () {
            logger.debug("Generated Invoices count: {}", order.getGeneratedInvoices().length);
            Integer[] invoiceIds = api.getLastInvoices(order.getUserId(), 1);
            if (invoiceIds.length == 0) {
                assertEquals(S("Should not expect invoice lines when no invoices are generated for {}", order), 0,
                        invoiceLines);
                return;
            }
            assertEquals(S("Should have last invoice for {}", order), 1, invoiceIds.length);
            InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
            InvoiceLineDTO[] lines = invoice.getInvoiceLines();

            logger.debug("Generated Invoice Id for {}:{}", order, invoice.getId());
            logger.debug("Total Invoice lines for  {}:{}", invoice.getId(), lines.length);
            assertEqualsBilling(
                    S("{} Invoice Lines count for {} should be {} + due lines {}", invoice.getCreateDatetime(), order,
                            invoiceLines, dueInvoiceLines), invoiceLines + dueInvoiceLines, lines.length);

            for (InvoiceLineDTO line : invoice.getInvoiceLines()) {
                if (line.getItemId() == null) {
                    continue;
                }
                logger.debug("Getting description for {}:{}", order.getId(), line.getDescription());
                assertEquals(S("1st description for {} Billibg Period should be: ", order.getId()),
                        S("Order line: {} Period from {} to {}", line.getItemId(), AsString(from), AsString(to)), line.getDescription());
            }
        }
    }
}
