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

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.entity.InvoiceLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeStatusWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderPeriodWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.CalendarUtils;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CreateObjectUtil;
import com.sapienter.jbilling.test.ApiTestCase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

import junit.framework.AssertionFailedError;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static com.sapienter.jbilling.test.Asserts.assertContainsError;
import static org.testng.Assert.assertEqualsNoOrder;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * BillingProcessTestCase This is the base class for all Billing process test classes that have been written to test
 * Billing Process functionality. It has all the methods that are reusable across all Billing cycle Test.
 * 
 * @author mazhar
 * @since 15-JUN-2014
 */
// @TestExecutionListeners(NoInvoiceFilterTestExecutionListener.class)
@ContextConfiguration(classes = BillingProcessTestConfig.class, loader = AnnotationConfigContextLoader.class)
public abstract class BillingProcessTestCase extends ApiTestCase {

    public static final Logger logger = LoggerFactory.getLogger(BillingProcessTestCase.class);
    private static final Pattern InvoiceLinePattern = Pattern.compile("Order line: (\\d*) Period from (\\d{2})/(\\d{2})/(\\d{4}) to (\\d{2}/\\d{2}/\\d{4})");

    public static void sortInvoiceLines(InvoiceWS invoice) {
        Arrays.sort(invoice.getInvoiceLines(), (i1, i2) -> i1.getId().compareTo(i2.getId()));
    }

    protected static InvoiceLineDTO[] sortInvoiceLinesByStartDate (InvoiceWS invoice) {
        InvoiceLineDTO[] lines = invoice.getInvoiceLines();
        Arrays.sort(lines, (i1, i2) -> startDateFromDescription(i1).compareTo(startDateFromDescription(i2)));
        return lines;
    }

    private static String startDateFromDescription (InvoiceLineDTO i1) {
        Matcher m = InvoiceLinePattern.matcher(i1.getDescription());
        m.matches();
        m.groupCount();
        return m.group(4) + m.group(2) + m.group(3);
    }
    
    private ArrayList<String>  failures = new ArrayList<>();

    @Autowired
    protected OrderPeriodWS           daily;
    @Autowired
    protected OrderPeriodWS           weekly;
    @Autowired
    protected OrderPeriodWS           semiMonthly;
    @Autowired
    protected OrderPeriodWS           monthly;
    @Autowired
    protected OrderChangeStatusWS     applyToOrderYes;

    private static final Integer      DEFAULT_BILLING_DAY_WEEKLY      = 2;                      // monday
    private static final Integer      DEFAULT_BILLING_DAY_SEMIMONTHLY = 1;
    private static final Integer      DEFAULT_BILLING_DAY_MONTHLY     = 1;

    protected TestUserBuilder         dailyBilledUserBuilder;
    protected TestUserBuilder         weeklyBilledUserBuilder;
    protected TestUserBuilder         semiMonthlyBilledUserBuilder;
    protected TestUserBuilder         monthlyBilledUserBuilder;

    protected TestOrderBuilderFactory orderBuilderFactory;
    protected TestOrderLineBuilder    orderLineBuilder;
    protected BillingProcessBuilder   billingProcessBuilder;
    protected ScenarioVerifierFactory scenarioVerifier;

    @Override
    protected void prepareTestInstance () throws Exception {
        dailyBilledUserBuilder = new TestUserBuilder(daily).billingDay(1);
        weeklyBilledUserBuilder = new TestUserBuilder(weekly).billingDay(DEFAULT_BILLING_DAY_WEEKLY);
        semiMonthlyBilledUserBuilder = new TestUserBuilder(semiMonthly).billingDay(DEFAULT_BILLING_DAY_SEMIMONTHLY);
        monthlyBilledUserBuilder = new TestUserBuilder(monthly).billingDay(DEFAULT_BILLING_DAY_MONTHLY);

        orderBuilderFactory = new TestOrderBuilderFactory();
        orderLineBuilder = new TestOrderLineBuilder();
        billingProcessBuilder = new BillingProcessBuilder(monthly);
        scenarioVerifier = new ScenarioVerifierFactory();
    }

    public class MetaFieldValueBuilder {
        private final String fieldName;
        private Object       value;
        private int          group;

        public MetaFieldValueBuilder (String fieldName) {
            this.fieldName = fieldName;
        }

        public MetaFieldValueBuilder value (Object value) {
            this.value = value;
            return this;
        }

        public MetaFieldValueBuilder group (int group) {
            this.group = group;
            return this;
        }

        public MetaFieldValueWS build () {
            MetaFieldValueWS newMetaField = new MetaFieldValueWS();
            newMetaField.setFieldName(fieldName);
            newMetaField.setValue(value);
            if (group != 0) {
                newMetaField.setGroupId(group);
            }
            return newMetaField;
        }
    }

    public class TestUserBuilder {

        private final OrderPeriodWS billingPeriod;

        private int billingDay;
        private Date nextInvoiceDate;
        private boolean invoiceChild;
        private boolean isParent;
        private UserWS parent;

        public TestUserBuilder (OrderPeriodWS billingPeriod) {
            this.billingPeriod = billingPeriod;
        }

        public TestUserBuilder billingDay (int billingDay) {
            this.billingDay = billingDay;
            return this;
        }

        public TestUserBuilder invoiceChild (boolean invoiceChild) {
            this.invoiceChild = invoiceChild;
            return this;
        }

        public TestUserBuilder isParent () {
            this.isParent = true;
            this.invoiceChild = false;
            this.parent = null;
            return this;
        }

        public TestUserBuilder withParent (UserWS parent) {
            this.isParent = false;
            this.invoiceChild = false;
            this.parent = parent;
            return this;
        }

        public TestUserBuilder nextInvoiceDate (String nextInvoiceDate) {
            this.nextInvoiceDate = AsDate(nextInvoiceDate);
            return this;
        }

        private MetaFieldValueWS[] fakeUserMetaFields (UserWS user) {
            return new MetaFieldValueWS[] {
                new MetaFieldValueBuilder("partner.prompt.fee").value("serial-from-ws").build(),
                // the plug-in parameter of the processor
                new MetaFieldValueBuilder("ccf.payment_processor").value("FAKE_2").build(),
                // contact info
                new MetaFieldValueBuilder("contact.first.name").value("Peter").group(1).build(),
                new MetaFieldValueBuilder("contact.last.name").value("Pan").group(1).build(),
                new MetaFieldValueBuilder("contact.email").value(user.getUserName() + "@shire.com").group(1).build()
            };
        }

        private ContactWS fakeContact (UserWS user) {
            ContactWS contact = CreateObjectUtil.createCustomerContact(user.getUserName() + "@shire.com");
            contact.setFirstName("J");
            contact.setLastName("Biller");
            return contact;
        }

        private UserWS constructBasicUser () {
            UserWS newUser = new UserWS();

            newUser.setLanguageId(TEST_LANGUAGE_ID);
            newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
            newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
            int BASIC_ACCOUNT_TYPE_ID = 1;
            newUser.setAccountTypeId(BASIC_ACCOUNT_TYPE_ID);

            return newUser;
        }

        public UserWS build () {
            UserWS newUser = constructBasicUser();
            newUser.setMainSubscription(new MainSubscriptionWS(billingPeriod.getId(), billingDay));
            newUser.setUserName("testUserName-" + DateTime.now().getMillis());
            newUser.setPassword("Asdfasdf@1");
            newUser.setContact(fakeContact(newUser));
            newUser.setMetaFields(fakeUserMetaFields(newUser));
            newUser.setInvoiceChild(invoiceChild);
            newUser.setIsParent(isParent);
            if (parent != null) {
                newUser.setParentId(parent.getId());
            }

            logger.debug("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
            logger.debug("User[{}] was created. next invoice date: {}", newUser.getUserId(), newUser.getNextInvoiceDate());

            newUser.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(newUser);
            newUser = api.getUserWS(newUser.getUserId());
            logger.debug("User[{}] was updated. next invoice date: {}", newUser.getUserId(), newUser.getNextInvoiceDate());

            return newUser;
        }
    }

    public class TestOrderBuilderFactory {
        public TestOrderBuilder forUser (UserWS user) {
            return new TestOrderBuilder(user);
        }
    }

    public class TestOrderBuilder {

        private OrderPeriodWS billingPeriod;
        private UserWS        user;
        private Date          activeSince;
        private Date          effectiveDate;
        private Date          activeUntil;
        private int           billingType = Constants.ORDER_BILLING_POST_PAID;
        private boolean       proRate;
        private OrderLineWS   orderLine   = orderLineBuilder.build();

        private TestOrderBuilder (UserWS user) {
            this.user = user;
            this.activeSince = user.getNextInvoiceDate();
        }

        public TestOrderBuilder prePaid () {
            this.billingType = Constants.ORDER_BILLING_PRE_PAID;
            return this;
        }

        public TestOrderBuilder postPaid () {
            this.billingType = Constants.ORDER_BILLING_POST_PAID;
            return this;
        }

        public TestOrderBuilder daily () {
            this.billingPeriod = daily;
            return this;
        }

        public TestOrderBuilder weekly () {
            this.billingPeriod = weekly;
            return this;
        }

        public TestOrderBuilder semiMonthly () {
            this.billingPeriod = semiMonthly;
            return this;
        }

        public TestOrderBuilder monthly () {
            this.billingPeriod = monthly;
            return this;
        }

        public TestOrderBuilder activeSince (String activeSinceDate) {
            this.activeSince = AsDate(activeSinceDate);
            return this;
        }

        public TestOrderBuilder effectiveDate (String effectiveDate) {
            this.effectiveDate = AsDate(effectiveDate);
            return this;
        }

        public TestOrderBuilder activeSince (int year, int month, int day) {
            this.activeSince = AsDate(year, month, day);
            return this;
        }

        public TestOrderBuilder activeUntil (String activeUntilDate) {
            this.activeUntil = AsDate(activeUntilDate);
            return this;
        }

        public TestOrderBuilder activeUntil (int year, int month, int day) {
            this.activeUntil = AsDate(year, month, day);
            return this;
        }

        public TestOrderBuilder proRate (boolean proRate) {
            this.proRate = proRate;
            return this;
        }

        public TestOrderBuilder orderLine (OrderLineWS orderLine) {
            this.orderLine = orderLine;
            return this;
        }

        private final static int ORDER_CHANGE_STATUS_APPLY_ID = 3;

        public OrderWS build () {
            OrderWS order = new OrderWS();
            order.setUserId(user.getId());
            order.setBillingTypeId(billingType);
            order.setPeriod(billingPeriod.getId());
            order.setCurrencyId(Constants.PRIMARY_CURRENCY_ID);
            order.setActiveSince(activeSince);
            order.setActiveUntil(activeUntil);
            order.setProrateFlag(proRate);
            order.setNextBillableDay(activeSince);
            order.setOrderLines(new OrderLineWS[] { orderLine });

            OrderChangeWS[] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
            for (OrderChangeWS change : orderChanges) {
                change.setStartDate(effectiveDate != null ? effectiveDate : activeSince);
                change.setEndDate(activeUntil);
            }

            int orderId = api.createUpdateOrder(order, orderChanges);
            return api.getOrder(orderId);
        }
    }

    public class TestOrderChangeBuilder {

        private OrderLineWS orderLine;

        private OrderWS     order;
        OrderChangeStatusWS applyToOrder = applyToOrderYes;
        private Date        applicationDate;
        private BigDecimal  quantity;
        private BigDecimal  price;

        public TestOrderChangeBuilder () {
        }

        public TestOrderChangeBuilder (OrderLineWS orderLine) {
            this.orderLine = orderLine;
            this.quantity = orderLine.getQuantityAsDecimal();
            this.price = orderLine.getPriceAsDecimal();
        }

        public TestOrderChangeBuilder applicationDate (String applicationDate) {
            this.applicationDate = AsDate(applicationDate);
            return this;
        }

        public TestOrderChangeBuilder forOrder (OrderWS order) {
            this.order = order;
            return this;
        }

        public TestOrderChangeBuilder quantity (double quantity) {
            this.quantity = BigDecimal.valueOf(quantity);
            return this;
        }

        public TestOrderChangeBuilder price (double price) {
            this.price = BigDecimal.valueOf(price);
            return this;
        }

        public OrderChangeWS build () {
            logger.debug("** 001: {}", orderLine);
            OrderChangeWS change = OrderChangeBL.buildFromLine(orderLine, order, applyToOrder.getId());
            change.setOptLock(1);
            change.setApplicationDate(applicationDate);
            change.setPrice(price);
            change.setQuantity(quantity);
            return change;
        }

        public OrderChangeWS buildNew () {
            OrderChangeWS change = new OrderChangeWS();
            change.setOptLock(1);
            change.setUserAssignedStatusId(applyToOrder.getId());
            change.setStartDate(applicationDate);
            change.setApplicationDate(applicationDate);
            change.setOrderWS(order);
            change.setUseItem(Constants.INTEGER_FALSE);
            change.setItemId(1);
            change.setDescription("Order line: ");
            change.setOrderChangeTypeId(1);

            change.setPrice(price);
            change.setQuantity(quantity);
            change.setNextBillableDate(order.getNextBillableDay());

            order = api.getOrder(order.getId());
            if (order.getOrderLines().length > 0) {
                orderLine = order.getOrderLines()[0];
                change.setOrderLineId(orderLine.getId());
            }

            logger.debug("*<>* : {}", change);

            return change;
        }

        public OrderChangeWS buildNewAndApply () {
            OrderChangeWS change = buildNew();
            api.updateOrder(order, new OrderChangeWS[] { change });
            order = api.getOrder(order.getId());
            orderLine = order.getOrderLines()[0];
            logger.debug(" **001: {}", change);
            logger.debug(" * 001: {}", orderLine);
            return change;
        }

        public OrderChangeWS buildAndApply () {
            OrderChangeWS change = build();
            api.updateOrder(order, new OrderChangeWS[] { change });
            order = api.getOrder(order.getId());
            orderLine = order.getOrderLines()[0];
            logger.debug("* *001: {}", change);
            logger.debug("***001: {}", orderLine);
            return change;
        }
    }

    public class TestOrderLineBuilder {

        private int        typeId   = Constants.ORDER_LINE_TYPE_ITEM;
        private String     description;
        private BigDecimal quantity = BigDecimal.ONE;
        private BigDecimal price    = BigDecimal.valueOf(60);
        private int        itemId   = 3; //use an item id that is not a plan for default value

        public TestOrderLineBuilder typeId (int typeId) {
            this.typeId = typeId;
            return this;
        }

        public TestOrderLineBuilder itemId (int itemId) {
            this.itemId = itemId;
            return this;
        }

        public TestOrderLineBuilder description (String description) {
            this.description = description;
            return this;
        }

        public TestOrderLineBuilder quantity (BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public TestOrderLineBuilder price (BigDecimal price) {
            this.price = price;
            return this;
        }

        public OrderLineWS build () {
            OrderLineWS line = new OrderLineWS();
            line.setTypeId(typeId);
            line.setItemId(itemId);
            line.setUseItem(false);
            line.setDescription(description == null ? "Order line: " + itemId : description);
            line.setQuantity(quantity);
            line.setPrice(price);
            line.setAmountAsDecimal(quantity.multiply(price));
            return line;
        }
    }

    protected ArrayList<String> getErrors () {
        return failures;
    }

    protected boolean assertEqualsBilling (String message, Date expected, Date actual) {
        try {
            assertEquals(message, expected, actual);
            return true;
        } catch (AssertionFailedError error) {
            logger.error("Error assert Equals Billing", error);
        } catch (AssertionError error) {
            failures.add(error.getMessage());
        }
        return false;
    }

    protected boolean assertEqualsBilling (String message, Integer expected, Integer actual) {
        try {
            assertEquals(message, expected, actual);
            return true;
        } catch (AssertionFailedError error) {
            logger.error("Error assert equals Billing", error);
        } catch (AssertionError error) {
            failures.add(error.getMessage());
        }
        return false;
    }

    protected boolean assertEqualsBilling (String message, BigDecimal expected, BigDecimal actual) {
        try {
            assertEquals(message, expected, actual);
            return true;
        } catch (AssertionFailedError error) {
            logger.error("Error assert Equals Billing", error);
        } catch (AssertionError error) {
            failures.add(error.getMessage());
        }
        return false;
    }

    protected boolean assertEqualsBilling (String message, String expected, String actual) {
        try {
            assertEquals(message, expected, actual);
            return true;
        } catch (AssertionFailedError error) {
            logger.error("Error in assert Equals Billing", error);
        } catch (AssertionError error) {
            failures.add(error.getMessage());
        }
        return false;
    }

    protected boolean assertContainsErrorBilling (SessionInternalError errors, String expected, String errorMessage) {
        try {
            assertContainsError(errors, expected, errorMessage);
            return true;
        } catch (AssertionFailedError error) {
            logger.error("Error in assert Containts Error Billing", error);
        } catch (AssertionError error) {
            failures.add(error.getMessage());
        }
        return false;
    }

    protected void assertNoErrorsAfterVerityAtDate (Date date) {
        getErrors().forEach(logger::debug);
        assertEquals(S("Tests failed on billing run on {}", date), 0, getErrors().size());
    }

    public class BillingProcessBuilder {

        private OrderPeriodWS                 billingPeriod;
        private Date                          nextRunDate;
        private BillingProcessConfigurationWS config;

        public BillingProcessBuilder (OrderPeriodWS billingPeriod) {
            config = api.getBillingProcessConfiguration();
            config.setRetries(1);
            config.setDaysForRetry(5);
            config.setGenerateReport(0);
            config.setAutoPaymentApplication(0);
            config.setDfFm(0);
            config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
            config.setDueDateValue(0);
            config.setInvoiceDateProcess(0);
            config.setMaximumPeriods(10);
            config.setOnlyRecurring(0);
            config.setProratingType(ProratingType.PRORATING_AUTO_ON.getProratingType());

            assignPeriodUnit(billingPeriod);
        }

        private void assignPeriodUnit (OrderPeriodWS period) {
            this.billingPeriod = period;
            config.setPeriodUnitId(period.getPeriodUnitId());
        }

        public BillingProcessBuilder daily () {
            assignPeriodUnit(daily);
            return this;
        }

        public BillingProcessBuilder weekly () {
            assignPeriodUnit(weekly);
            return this;
        }

        public BillingProcessBuilder semiMonthly () {
            assignPeriodUnit(semiMonthly);
            return this;
        }

        public BillingProcessBuilder monthly () {
            assignPeriodUnit(monthly);
            return this;
        }

        public BillingProcessBuilder nextRunDate (int year, int month, int day) {
            this.nextRunDate = AsDate(year, month, day);
            return this;
        }

        public BillingProcessBuilder nextRunDate (String nextRunDate) {
            this.nextRunDate = AsDate(nextRunDate);
            return this;
        }

        public void triggerReview (String fromDate, String toDate) {
            triggerReview(AsDate(fromDate), AsDate(toDate));
        }

        public void triggerReview (Date fromDate, Date toDate) {
            api.setReviewApproval(false);
            config.setGenerateReport(1);
            config.setDaysForReport(Days.daysBetween(new LocalDate(fromDate), new LocalDate(toDate)).getDays());
            triggerForDate(toDate);
        }

        public void triggerBilling (String forDateStr) {
            triggerBilling(AsDate(forDateStr));
        }

        public void triggerBilling (Date forDate) {
            api.setReviewApproval(false);
            config.setGenerateReport(0);
            triggerForDate(forDate);
        }

        public void triggerForDate (Date forDate) {
            config.setNextRunDate(nextRunDate);
            config.setNextRunDate(forDate);

            logger.debug("B - Setting config to: {}", config);
            api.createUpdateBillingProcessConfiguration(config);

            logger.debug("Running Billing Process for {}", forDate);
            api.triggerBilling(forDate);
        }
    }

    protected void triggerBilling (Date runDate) {
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        config.setNextRunDate(runDate);
        config.setRetries(1);
        config.setDaysForRetry(5);
        config.setGenerateReport(0);
        config.setAutoPaymentApplication(0);
        config.setDfFm(0);
        config.setPeriodUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
        config.setDueDateValue(0);
        config.setInvoiceDateProcess(0);
        config.setMaximumPeriods(99);
        config.setOnlyRecurring(0);
        config.setProratingType(ProratingType.PRORATING_MANUAL.getProratingType());

        logger.debug("B - Setting config to: {}", config);
        api.createUpdateBillingProcessConfiguration(config);

        logger.debug("Running Billing Process for {}", runDate);
        api.triggerBilling(runDate);
    }

    public class ScenarioVerifierFactory {
        public ScenarioVerifier forOrder (OrderWS order) {
            return new ScenarioVerifier(order);
        }
    }

    protected void assertNextBillableDay (OrderWS order, Date expected) {
        logger.debug("Processed order next billable day: {}", order.getNextBillableDay());
        if (expected == null) {
            assertNull(S("Order[{}] next billable date should be null", order.getId()), order.getNextBillableDay());
        } else {
            assertEquals(S("Order[{}] next billable date should be {}", order.getId(), expected), expected,
                    order.getNextBillableDay());
        }
    }

    protected void assertNextInvoiceDate (UserWS user, Date expected) {
        logger.debug("User[{}] next invoice date: {}", user.getId(), user.getNextInvoiceDate());
        assertEquals(S("User[{}] next billable date should be {}", user.getId(), expected), expected,
                user.getNextInvoiceDate());
    }

    public class ScenarioVerifier {

        protected OrderWS order;

        protected Date nbd;
        protected Date nid;
        protected Date from;
        protected Date to;
        protected int invoiceLines;
        protected int dueInvoiceLines;
        protected boolean skipPreviousInvoice;
        protected List<String> descriptions;

        protected ScenarioVerifier (OrderWS order) {
            this.order = order;
            this.descriptions = new ArrayList<>();
        }

        /**
         * Order next billable date
         * 
         * @param nbd
         *            string in "MM/dd/yyyy" format
         */
        public ScenarioVerifier nbd (String nbd) {
            this.nbd = AsDate(nbd);
            return this;
        }

        /**
         * User next invoice date
         * 
         * @param nid
         *            string in "MM/dd/yyyy" format
         */
        public ScenarioVerifier nid (String nid) {
            this.nid = AsDate(nid);
            return this;
        }

        public ScenarioVerifier from (String from) {
            this.from = AsDate(from);
            return this;
        }

        public ScenarioVerifier to (String to) {
            this.to = AsDate(to);
            return this;
        }

        public ScenarioVerifier invoiceLines (int invoiceLines) {
            this.invoiceLines = invoiceLines;
            return this;
        }

        public ScenarioVerifier dueInvoiceLines (int dueInvoiceLines) {
            this.dueInvoiceLines = dueInvoiceLines;
            return this;
        }

        public ScenarioVerifier skipPreviousInvoice () {
            this.skipPreviousInvoice = true;
            return this;
        }

        public ScenarioVerifier withDescription(String description) {
            descriptions.add(description);
            return this;
        }
        public void verify () {
            order = api.getOrder(order.getId());

            assertNextBillableDay(order, nbd);
            assertNextInvoiceDate(api.getUserWS(order.getUserId()), nid);

            if (!skipPreviousInvoice) {
                verifyInvoiceLines();
            } else {
                verifyDescriptionsInInvoiceLines();
            }
        }

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

            logger.debug("Generated Invoice Id for {}: {}", order, invoice.getId());
            logger.debug("Total Invoice lines for {}: {}", invoice.getId(), lines.length);
            assertEqualsBilling(
                    S("{} Invoice Lines count for {} should be {} + due lines {}", invoice.getCreateDatetime(), order,
                            invoiceLines, dueInvoiceLines), invoiceLines + dueInvoiceLines, lines.length);

            Date startDate = from;
            Date endDate = to;
            String startDateInvoice = AsString(startDate);
            String endDateInvoice = AsString(endDate);

            String[] actualDescriptions   = new String[lines.length];
            String[] expectedDescriptions = new String[lines.length];
            int index = 0;

            for (InvoiceLineDTO line : lines) {
                if (line.getItemId() == null) {
                    actualDescriptions[index]   = null;
                    expectedDescriptions[index] = null;
                    index++;
                    continue;
                }

                logger.debug("Getting description for {}: {}", order.getId(), line.getDescription());
                actualDescriptions[index]   = line.getDescription();
                expectedDescriptions[index] = S("Order line: {} Period from {} to {}", line.getItemId(), startDateInvoice, endDateInvoice);
                index++;

                Integer orderPeriod = order.getPeriod();
                if (orderPeriod.equals(daily.getId())) {
                    startDate = new LocalDate(startDate).plusDays(1).toDate();
                    endDate = new LocalDate(endDate).plusDays(1).toDate();
                }
                if (orderPeriod.equals(weekly.getId())) {
                    startDate = new LocalDate(startDate).plusWeeks(1).toDate();
                    endDate = new LocalDate(endDate).plusWeeks(1).toDate();
                }
                if (orderPeriod.equals(semiMonthly.getId())) {
                    // ????
                    startDate = CalendarUtils.addSemiMonthyPeriod(startDate);
                    endDate = CalendarUtils.addSemiMonthyPeriod(endDate);
                }
                if (orderPeriod.equals(monthly.getId())) {
                    startDate = new LocalDate(startDate).plusMonths(1).toDate();
                    endDate = new LocalDate(endDate).plusMonths(1).toDate();
                }
                startDateInvoice = AsString(startDate);
                endDateInvoice = AsString(endDate);
            }

            if (order.getProrateFlag()) {
                logger.debug(Arrays.toString(actualDescriptions));
                logger.debug(Arrays.toString(expectedDescriptions));
            }
            assertEqualsNoOrder(actualDescriptions, expectedDescriptions, S("Description for {} differs from expected", order.getId()));
        }

        protected void verifyDescriptionsInInvoiceLines () {
            if (!descriptions.isEmpty()) {
                logger.debug("Generated Invoices count: {}", order.getGeneratedInvoices().length);
                Integer[] invoiceIds = api.getLastInvoices(order.getUserId(), 1);
                if (invoiceIds.length == 0) {
                    assertEquals(S("Should not expect invoice lines when no invoices are generated for {}", order), 0, invoiceLines);
                    return;
                }

                assertEquals(S("Should have last invoice for {}", order), 1, invoiceIds.length);
                InvoiceWS invoice = api.getInvoiceWS(invoiceIds[0]);
                InvoiceLineDTO[] lines = invoice.getInvoiceLines();
                for (InvoiceLineDTO line: lines) {
                    assertTrue(S("Description for {} differs from expected", order.getId()), descriptions.stream()
                                                                                                         .anyMatch(pattern -> line.getDescription()
                                                                                                                                  .matches(pattern)));
                }
            }
        }
    }

    protected void triggerBillingForDate(Date date) {
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
        config.setNextRunDate(date);
        config.setLastDayOfMonth(true);
        config.setGenerateReport(0);
        config.setInvoiceDateProcess(0);
        api.createUpdateBillingProcessConfiguration(config);
        config = api.getBillingProcessConfiguration();
        logger.debug("Trigger billing process for {}", config.getNextRunDate());
        api.triggerBilling(config.getNextRunDate());
    }
}