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

import org.testng.annotations.Test;

import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import java.util.*;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;
import java.lang.System;

/**
 * DailyBillingCycleTest Test class for Daily Billing Cycle.
 * 
 * @author Sagar Dond On 15 june 2014
 */
@Test(groups = { "billing-and-discounts", "billing" }, testName = "DailyBillingCycleTest")
public class DailyBillingCycleTest extends BillingProcessTestCase {

    @Test
    public void testDailyBillingCycle () throws Exception {
        // ** B1 Scenario User and Order **
        // Create the user for B1 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB1 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderB1 = orderBuilderFactory.forUser(userB1).weekly().build();
        logger.debug("OrderB1 id: {}", orderB1.getId());
        logger.debug("UserB1 id: {}", userB1.getUserId());

        // ------------------
        // ** B2 Scenario User and Order **
        // Create the user for B2 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB2 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderB2 = orderBuilderFactory.forUser(userB2).weekly().prePaid().build();
        logger.debug("OrderB2 id: {}", orderB2.getId());
        logger.debug("UserB2 id: {}", userB2.getUserId());

        // ------------------
        // ** B3 Scenario User and Order **
        // Create the user for B3 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB3 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderB3 = orderBuilderFactory.forUser(userB3).semiMonthly().build();
        logger.debug("OrderB3 id: {}", orderB3.getId());
        logger.debug("UserB3 id: {}", userB3.getUserId());

        // ------------------
        // ** B4 Scenario User and Order **
        // Create the user for B4 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB4 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderB4 = orderBuilderFactory.forUser(userB4).semiMonthly().prePaid().build();
        logger.debug("OrderB4 id: {}", orderB4.getId());
        logger.debug("UserB4 id: {}", userB4.getUserId());

        // ------------------
        // ** B5 Scenario User and Order **
        // Create the user for B5 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB5 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderB5 = orderBuilderFactory.forUser(userB5).monthly().activeSince("01/15/2011").build();
        logger.debug("OrderB5 id: {}", orderB5.getId());
        logger.debug("UserB5 id: {}", userB5.getUserId());

        // ------------------
        // ** B6 Scenario User and Order **
        // Create the user for B1 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userB6 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderB6 = orderBuilderFactory.forUser(userB6).monthly().prePaid().activeSince("01/15/2011").build();
        logger.debug("OrderB6 id: {}", orderB6.getId());
        logger.debug("UserB6 id: {}", userB6.getUserId());

        // create pro rated scenarios

        // ------------------
        // ** A1 Scenario User and Order **
        // Create the user for A1 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userA1 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderA1 = orderBuilderFactory.forUser(userA1).daily().proRate(true).build();
        logger.debug("OrderA1 id: {}", orderA1.getId());
        logger.debug("UserA1 id: {}", userA1.getUserId());

        // ------------------
        // ** A2 Scenario User and Order **
        // Create the user for A2 scenario and updated its next invoice date to
        // 01-Jan
        UserWS userA2 = dailyBilledUserBuilder.nextInvoiceDate("01/01/2011").build();
        OrderWS orderA2 = orderBuilderFactory.forUser(userA2).daily().prePaid().proRate(true).build();
        logger.debug("OrderA2 id: {}", orderA2.getId());
        logger.debug("UserA2 id: {}", userA2.getUserId());

        /**
         * Not Pro-Rate Scenario Billing Run date = 1st Jan 2011 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/01/2011"));

        // Not Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderB1).nbd("01/01/2011").nid("01/02/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2011").nid("01/02/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2011").nid("01/02/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2011").nid("01/02/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2011").nid("01/02/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2011").nid("01/02/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();

        // Pro rated scenarios asserts
        scenarioVerifier.forOrder(orderA1).nbd("01/01/2011").nid("01/02/2011").from("01/01/2011").to("01/01/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/02/2011").nid("01/02/2011").from("01/01/2011").to("01/01/2011")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/01/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 2th Jan 2011 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/02/2011"));

        scenarioVerifier.forOrder(orderB1).nbd("01/01/2011").nid("01/03/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2011").nid("01/03/2011").from("01/07/2011").to("01/07/2011")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2011").nid("01/03/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2011").nid("01/03/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2011").nid("01/03/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2011").nid("01/03/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();

        // Prorated
        scenarioVerifier.forOrder(orderA1).nbd("01/02/2011").nid("01/03/2011").from("01/01/2011").to("01/01/2011")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/03/2011").nid("01/03/2011").from("01/02/2011").to("01/02/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/02/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 3th Jan 2011 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/03/2011"));

        scenarioVerifier.forOrder(orderB1).nbd("01/01/2011").nid("01/04/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2011").nid("01/04/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2011").nid("01/04/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2011").nid("01/04/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2011").nid("01/04/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2011").nid("01/04/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();

        // Prorated
        scenarioVerifier.forOrder(orderA1).nbd("01/03/2011").nid("01/04/2011").from("01/02/2011").to("01/02/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/04/2011").nid("01/04/2011").from("01/03/2011").to("01/03/2011")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/03/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 4th Jan 2011 for Billing sceanrio B1 - B6 And Pro-Rate Scenario A1
         * and A2
         */
        triggerBilling(AsDate("01/04/2011"));

        scenarioVerifier.forOrder(orderB1).nbd("01/01/2011").nid("01/05/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/08/2011").nid("01/05/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB3).nbd("01/01/2011").nid("01/05/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB4).nbd("01/16/2011").nid("01/05/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).skipPreviousInvoice().verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2011").nid("01/05/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("01/15/2011").nid("01/05/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();

        /**
         * Pro-Rate Scenario Billing Run date = 4th Jan 2011 for Billing sceanrio A1 and A2
         */
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .to("01/04/2011")
        scenarioVerifier.forOrder(orderA1).nbd("01/04/2011").nid("01/05/2011").from("01/03/2011").to("01/03/2011")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderA2).nbd("01/05/2011").nid("01/05/2011").from("01/04/2011").to("01/04/2011")
                .invoiceLines(1).dueInvoiceLines(3).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/04/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 8th Jan 2011 for Billing sceanrio B1 and B2 Before billing run
         * update next invoice date of User to 8th Jan
         */
        userB1 = api.getUserWS(userB1.getUserId());
	    userB1.setPassword(null);
        userB1.setNextInvoiceDate(AsDate("01/08/2011"));
        api.updateUser(userB1);

        userB2 = api.getUserWS(userB2.getUserId());
	    userB2.setPassword(null);
        userB2.setNextInvoiceDate(AsDate("01/08/2011"));
        api.updateUser(userB2);

        triggerBilling(AsDate("01/08/2011"));

        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .nbd("01/01/2011")
        scenarioVerifier.forOrder(orderB1).nbd("01/08/2011").nid("01/09/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/15/2011").nid("01/09/2011").from("01/08/2011").to("01/14/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/08/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 9th Jan 2011 for Billing sceanrio B1 and B2
         */
        triggerBilling(AsDate("01/09/2011"));

        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ?? .nbd("01/01/2011")
        scenarioVerifier.forOrder(orderB1).nbd("01/08/2011").nid("01/10/2011").from("01/01/2011").to("01/07/2011")
                .invoiceLines(1).skipPreviousInvoice().verify();
        //?? [2014-11-21 igor.poteryaev@jbilling.com] how it worked ??  .nid("01/15/2011").from("01/10/2011")
        scenarioVerifier.forOrder(orderB2).nbd("01/15/2011").nid("01/10/2011").from("01/08/2011").to("01/14/2011")
                .invoiceLines(1).skipPreviousInvoice().verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/09/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 15th Jan 2011 for Billing sceanrio B1, B2, B5 and B6 Before billing
         * run update next invoice date of User to 15th Jan
         */

        userB1 = api.getUserWS(userB1.getUserId());
	    userB1.setPassword(null);
        userB1.setNextInvoiceDate(AsDate("01/15/2011"));
        api.updateUser(userB1);

        userB2 = api.getUserWS(userB2.getUserId());
	    userB2.setPassword(null);
        userB2.setNextInvoiceDate(AsDate("01/15/2011"));
        api.updateUser(userB2);

        userB5 = api.getUserWS(userB5.getUserId());
	    userB5.setPassword(null);
        userB5.setNextInvoiceDate(AsDate("01/15/2011"));
        api.updateUser(userB5);

        userB6 = api.getUserWS(userB6.getUserId());
	    userB6.setPassword(null);
        userB6.setNextInvoiceDate(AsDate("01/15/2011"));
        api.updateUser(userB6);

        triggerBilling(AsDate("01/15/2011"));

        scenarioVerifier.forOrder(orderB1).nbd("01/15/2011").nid("01/16/2011").from("01/08/2011").to("01/14/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB2).nbd("01/22/2011").nid("01/16/2011").from("01/15/2011").to("01/21/2011")
                .invoiceLines(1).dueInvoiceLines(2).verify();
        scenarioVerifier.forOrder(orderB5).nbd("01/15/2011").nid("01/16/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).verify();
        scenarioVerifier.forOrder(orderB6).nbd("02/15/2011").nid("01/16/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/15/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 16th Jan 2011 for Billing sceanrio B3 and B4
         */
        userB3 = api.getUserWS(userB3.getUserId());
	    userB3.setPassword(null);
        userB3.setNextInvoiceDate(AsDate("01/16/2011"));
        api.updateUser(userB3);

        userB4 = api.getUserWS(userB4.getUserId());
	    userB4.setPassword(null);
        userB4.setNextInvoiceDate(AsDate("01/16/2011"));
        api.updateUser(userB4);

        triggerBilling(AsDate("01/16/2011"));

        scenarioVerifier.forOrder(orderB3).nbd("01/16/2011").nid("01/17/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/01/2011").nid("01/17/2011").from("01/16/2011").to("01/31/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/16/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 17th Jan 2011 for Billing sceanrio B3 and B4
         */
        triggerBilling(AsDate("01/17/2011"));

        scenarioVerifier.forOrder(orderB3).nbd("01/16/2011").nid("01/18/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/01/2011").nid("01/18/2011").from("01/16/2011").to("01/31/2011")
                .invoiceLines(0).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/17/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 31st Jan 2011 for Billing sceanrio B3 and B4
         */
        userB3 = api.getUserWS(userB3.getUserId());
	    userB3.setPassword(null);
        userB3.setNextInvoiceDate(AsDate("01/31/2011"));
        api.updateUser(userB3);

        userB4 = api.getUserWS(userB4.getUserId());
	    userB4.setPassword(null);
        userB4.setNextInvoiceDate(AsDate("01/31/2011"));
        api.updateUser(userB4);

        triggerBilling(AsDate("01/31/2011"));

        scenarioVerifier.forOrder(orderB3).nbd("01/16/2011").nid("02/01/2011").from("01/01/2011").to("01/15/2011")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/01/2011").nid("02/01/2011").from("01/16/2011").to("01/31/2011")
                .invoiceLines(0).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("01/31/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 1st Feb 2011 for Billing sceanrio B3 and B4
         */
        triggerBilling(AsDate("02/01/2011"));

        scenarioVerifier.forOrder(orderB3).nbd("02/01/2011").nid("02/02/2011").from("01/16/2011").to("01/31/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB4).nbd("02/16/2011").nid("02/02/2011").from("02/01/2011").to("02/15/2011")
                .invoiceLines(1).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/01/2011"));

        /**
         * Not Pro-Rate Scenario Billing Run date = 15th Feb 2011 for Billing sceanrio B5 and B6 Update Next Invoice
         * Date to 15th Jan before Billing run
         */
        userB5 = api.getUserWS(userB5.getUserId());
	    userB5.setPassword(null);
        userB5.setNextInvoiceDate(AsDate("02/15/2011"));
        api.updateUser(userB5);

        userB6 = api.getUserWS(userB6.getUserId());
	    userB6.setPassword(null);
        userB6.setNextInvoiceDate(AsDate("02/15/2011"));
        api.updateUser(userB6);

        triggerBilling(AsDate("02/15/2011"));

        scenarioVerifier.forOrder(orderB5).nbd("02/15/2011").nid("02/16/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB6).nbd("03/15/2011").nid("02/16/2011").from("02/15/2011").to("03/14/2011")
                .invoiceLines(1).dueInvoiceLines(1).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/15/2011"));

        /**
         * Not Pro-Rate Billing Run date = 16th Feb 2011 for Billing sceanrio B5 and B6
         */
        triggerBilling(AsDate("02/16/2011"));

        scenarioVerifier.forOrder(orderB5).nbd("02/15/2011").nid("02/17/2011").from("01/15/2011").to("02/14/2011")
                .invoiceLines(0).dueInvoiceLines(1).verify();
        scenarioVerifier.forOrder(orderB6).nbd("03/15/2011").nid("02/17/2011").from("02/15/2011").to("03/14/2011")
                .invoiceLines(0).dueInvoiceLines(2).verify();

        assertNoErrorsAfterVerityAtDate(AsDate("02/16/2011"));
    }
}
