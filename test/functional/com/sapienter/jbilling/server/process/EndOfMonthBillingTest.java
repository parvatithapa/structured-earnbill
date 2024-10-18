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

import com.sapienter.jbilling.server.invoice.*;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.user.UserWS;

import java.util.Arrays;
import java.util.Date;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Points to testOrders: Orders : - next billable day - to_process - start/end of billing period - invoice has (not)
 * generated - billing process relationship - some amounts of the generated invoice Invoices : - if the invoice has been
 * processed or no - to_process - delegated_invoice_id is updated
 *
 * @author Leandro Zoi
 */
@Test(groups = { "integration", "process" }, testName = "process.BillingProcessTest")
public class EndOfMonthBillingTest extends BillingProcessTestCase {

    @BeforeClass
    protected void setUp () throws Exception {
        super.prepareTestInstance();
    }

    @Test(enabled = true)
    public void test001EndOfMonthCorrectionMonthlyOrders() throws Exception {
        UserWS user1 = monthlyBilledUserBuilder.billingDay(29)
                                               .nextInvoiceDate("01/29/2012")
                                               .build();

        UserWS user2 = monthlyBilledUserBuilder.billingDay(30)
                                               .nextInvoiceDate("01/30/2012")
                                               .build();

        UserWS user3 = monthlyBilledUserBuilder.billingDay(31)
                                               .nextInvoiceDate("01/31/2012")
                                               .build();

        UserWS user4 = monthlyBilledUserBuilder.billingDay(31)
                                               .nextInvoiceDate("01/31/2012")
                                               .build();

        OrderWS order1 = orderBuilderFactory.forUser(user1)
                                            .monthly()
                                            .prePaid()
                                            .proRate(false)
                                            .activeSince("01/29/2012")
                                            .build();

        OrderWS order2 = orderBuilderFactory.forUser(user2)
                                            .monthly()
                                            .prePaid()
                                            .proRate(false)
                                            .activeSince("01/30/2012")
                                            .build();

        OrderWS order3 = orderBuilderFactory.forUser(user3)
                                            .monthly()
                                            .prePaid()
                                            .proRate(false)
                                            .activeSince("01/31/2012")
                                            .build();

        OrderWS order4 = orderBuilderFactory.forUser(user4)
                                            .monthly()
                                            .prePaid()
                                            .proRate(true)
                                            .activeSince("01/31/2012")
                                            .effectiveDate("02/05/2012")
                                            .build();

        triggerBillingForDate(AsDate("01/29/2012"));
        scenarioVerifier.forOrder(order1)
                        .nbd("02/29/2012")
                        .nid("02/29/2012")
                        .from("01/29/2012")
                        .to("02/28/2012")
                        .invoiceLines(1)
                        .verify();

        scenarioVerifier.forOrder(order2)
                        .nbd("02/29/2012")
                        .nid("02/29/2012")
                        .from("01/30/2012")
                        .to("02/28/2012")
                        .invoiceLines(1)
                        .verify();

        scenarioVerifier.forOrder(order3)
                        .nbd("02/29/2012")
                        .nid("02/29/2012")
                        .from("01/31/2012")
                        .to("02/28/2012")
                        .invoiceLines(1)
                        .verify();

        scenarioVerifier.forOrder(order4)
                        .nbd("02/29/2012")
                        .nid("02/29/2012")
                        .from("02/05/2012")
                        .to("02/28/2012")
                        .invoiceLines(1)
                        .verify();

        triggerBillingForDate(AsDate("02/29/2012"));
        scenarioVerifier.forOrder(order1)
                        .nbd("03/29/2012")
                        .nid("03/29/2012")
                        .from("02/29/2012")
                        .to("03/28/2012")
                        .invoiceLines(2)
                        .verify();

        scenarioVerifier.forOrder(order2)
                        .nbd("03/30/2012")
                        .nid("03/30/2012")
                        .from("02/29/2012")
                        .to("03/29/2012")
                        .invoiceLines(2)
                        .verify();

        scenarioVerifier.forOrder(order3)
                        .nbd("03/31/2012")
                        .nid("03/31/2012")
                        .from("02/29/2012")
                        .to("03/30/2012")
                        .invoiceLines(2)
                        .verify();

        scenarioVerifier.forOrder(order4)
                        .nbd("03/31/2012")
                        .nid("03/31/2012")
                        .from("02/29/2012")
                        .to("03/30/2012")
                        .invoiceLines(2)
                        .verify();

        //Delete all the objects created
        logger.debug("Deleting invoices");
        Arrays.stream(api.getAllInvoices(user1.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(i -> api.deleteInvoice(i));

        Arrays.stream(api.getAllInvoices(user2.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(i -> api.deleteInvoice(i));

        Arrays.stream(api.getAllInvoices(user3.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(i -> api.deleteInvoice(i));

        Arrays.stream(api.getAllInvoices(user4.getUserId()))
              .sorted((i1, i2) -> i2.compareTo(i1))
              .forEach(i -> api.deleteInvoice(i));

        logger.debug("Deleting orders");
        api.deleteOrder(order1.getId());
        api.deleteOrder(order2.getId());
        api.deleteOrder(order3.getId());
        api.deleteOrder(order4.getId());

        logger.debug("Deleting users");
        api.deleteUser(user1.getId());
        api.deleteUser(user2.getId());
        api.deleteUser(user3.getId());
        api.deleteUser(user4.getId());
    }

    @Test(enabled = true)
    public void test002EndOfMonthCorrectionForInvoiceGeneratedManually() throws Exception {
        UserWS user = monthlyBilledUserBuilder.billingDay(31)
                                              .nextInvoiceDate("05/31/2012")
                                              .build();


        OrderWS order = orderBuilderFactory.forUser(user)
                                           .monthly()
                                           .prePaid()
                                           .proRate(false)
                                           .activeSince("05/31/2012")
                                           .activeUntil("12/31/2012")
                                           .build();

        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setMaximumPeriods(99);
        api.createUpdateBillingProcessConfiguration(config);

        InvoiceWS invoice = api.getInvoiceWS(api.createInvoiceFromOrder(order.getId(), null));
        sortInvoiceLines(invoice);

        assertEquals("Invoices Lines Quantity not match", invoice.getInvoiceLines().length, 7);
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[0].getDescription().matches("(.*) Period from 05/31/2012 to 06/29/2012"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[1].getDescription().matches("(.*) Period from 06/30/2012 to 07/30/2012"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[2].getDescription().matches("(.*) Period from 07/31/2012 to 08/30/2012"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[3].getDescription().matches("(.*) Period from 08/31/2012 to 09/29/2012"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[4].getDescription().matches("(.*) Period from 09/30/2012 to 10/30/2012"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[5].getDescription().matches("(.*) Period from 10/31/2012 to 11/29/2012"));
        assertTrue("Invoices Lines Description not match", invoice.getInvoiceLines()[6].getDescription().matches("(.*) Period from 11/30/2012 to 12/30/2012"));

        //Delete all the objects created
        api.deleteInvoice(invoice.getId());
        api.deleteOrder(order.getId());
        api.deleteUser(user.getId());
    }
}