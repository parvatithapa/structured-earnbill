package com.sapienter.jbilling.fc; 

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.joda.time.LocalDate;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.fail;

import com.sapienter.jbilling.api.automation.EnvironmentHelper;
import com.sapienter.jbilling.fc.FullCreativeTestConstants;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.user.CustomerNoteWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.BillingProcessWS;
import com.sapienter.jbilling.server.user.CancellationRequestWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.metafield.builder.MetaFieldBuilder;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.test.framework.TestBuilder;
import com.sapienter.jbilling.test.framework.TestEntityType;
import com.sapienter.jbilling.test.framework.TestEnvironment;
import com.sapienter.jbilling.test.framework.TestEnvironmentBuilder;
import com.sapienter.jbilling.test.framework.builders.ConfigurationBuilder;
import com.sapienter.jbilling.test.framework.builders.CustomerBuilder;
import com.sapienter.jbilling.test.framework.builders.ItemBuilder;
import com.sapienter.jbilling.test.framework.helpers.ApiBuilderHelper;

@Test(groups = {"fullcreative"}, testName = "CustomerAnnouncementTest") 
public class FullCreativeCustomerAnnouncementsTest {

    private Logger logger = LoggerFactory.getLogger(FullCreativeCustomerAnnouncementsTest.class);
    private TestBuilder testBuilder;
    private EnvironmentHelper environmentHelper;
    private static final String CATEGORY_CODE = "TestCategory";
    private static final String PRODUCT_CODE = "TestProduct";
    private static final String ONE_TIME_INVOICE_NOTE = "One Time Invoice Note";
    private static final String ACCOUNT_TYPE_CODE = "TestAccountType";
    private static final String CUSTOMER_CODE = "TestCustomer"+System.currentTimeMillis();

    private Date nextRunDate = null;
    private Date activeSince = null;
    BillingProcessWS lastBillingProcess = null;

    private TestEnvironmentBuilder testEnvironmentBuilder;

    Integer userId ;
    @BeforeClass
    public void initializeTests(){
        testBuilder = getTestEnvironment();
        // Configuring Order Line Based Composition Task
     	updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID, FullCreativeTestConstants.ORDER_LINE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);
    }

    private TestBuilder getTestEnvironment() {
        return TestBuilder.newTest(false).givenForMultiple(envCreator -> {
            final JbillingAPI api = envCreator.getPrancingPonyApi();
            environmentHelper = EnvironmentHelper.getInstance(api);

            envCreator.itemBuilder(api).itemType().withCode(CATEGORY_CODE).global(true).build();
            envCreator.itemBuilder(api).item().withCode(PRODUCT_CODE).global(true).withType(envCreator.idForCode(CATEGORY_CODE))
                    .withFlatPrice("0.50").build();
            envCreator.accountTypeBuilder(api).withName(ACCOUNT_TYPE_CODE).build().getId();

            logger.debug("## Last billing process id ::::::::::::: {}",api.getLastBillingProcess());
            lastBillingProcess = api.getBillingProcess(api.getLastBillingProcess());
            logger.debug("## Last billing process billing date  ::::::::::::: {}",lastBillingProcess.getBillingDate());

            nextRunDate = new LocalDate(lastBillingProcess.getBillingDate()).plusDays(1).toDate();
            activeSince = new LocalDate(lastBillingProcess.getBillingDate()).plusDays(1).toDate();

             });
    }

    @Test(priority = 1)
    public void test001TestOneTimeInvoiceNote(){

        testBuilder.given(envBuilder -> {
            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
            assertNotNull("UserId should not be null",userId);

            CustomerNoteWS note = new CustomerNoteWS();
            note.setNoteTitle("One Time Invoice Note");
            String s = "This is a <style isBold=\"true\" isItalic=\"true\" isUnderline=\"true\">static text</style> element containing styled text. <style backcolor=\"yellow\" isBold=\"true\" isItalic=\"true\">Styled text</style> elements are introduced by setting the <style forecolor=\"blue\" isItalic=\"true\">markup</style> attribute available for the <style isBold=\"true\" forecolor=\"magenta\">textElement</style> tag to <style forecolor=\"red\" isItalic=\"true\">styled</style> and by formatting the text content using nested <style isBold=\"true\" forecolor=\"green\">style</style> tags and simple HTML tags.";
            note.setNoteContent(s);
            note.setUserId(api.getCallerId());
            note.setCustomerId(api.getUserWS(userId).getCustomerId());
            note.setCreationTime(new Date());
            note.setNotesInInvoice(true);
            api.createCustomerNote(note);

            envBuilder.orderBuilder(api)
            .forUser(userId)
            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
            .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
            .withActiveSince(activeSince)
            .withEffectiveDate(activeSince)
            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
            .withDueDateValue(1)
            .withCodeForTests("Order -1")
            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
            .build();
             updateBillingProcessConfiguration(nextRunDate, api, 0, Constants.PERIOD_UNIT_DAY);

        }).test((env)-> { 
            final JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS = null;

            logger.debug("## billingDate ::::::::::::: {}",activeSince);
            api.createInvoiceWithDate(userId, activeSince, null, null, false);

            UserWS userWS = api.getUserWS(userId);
            logger.debug("## User id:: {}",userWS.getId());
            assertEquals(userWS.getStatus(), "Active");

            invoiceWS = api.getLatestInvoice(userId);
            logger.debug("## Invoice id:: {}",invoiceWS.getId());
            logger.debug("## One Time Invoice Note ::::::::::::: {}",invoiceWS.getCustomerNotes());
            assertNotNull(invoiceWS.getCustomerNotes(), "One Time Invoice Note not popuplated in invoice");
            assertEquals(invoiceWS.getCustomerNotes(), "This is a <style isBold=\"true\" isItalic=\"true\" isUnderline=\"true\">static text</style> element containing styled text. <style backcolor=\"yellow\" isBold=\"true\" isItalic=\"true\">Styled text</style> elements are introduced by setting the <style forecolor=\"blue\" isItalic=\"true\">markup</style> attribute available for the <style isBold=\"true\" forecolor=\"magenta\">textElement</style> tag to <style forecolor=\"red\" isItalic=\"true\">styled</style> and by formatting the text content using nested <style isBold=\"true\" forecolor=\"green\">style</style> tags and simple HTML tags.");
            Date nextInvoiceDate = new DateTime(activeSince).plusMonths(1).toDate();
            logger.debug("## nextInvoiceDate::: {}",nextInvoiceDate);
            userWS.setNextInvoiceDate(nextInvoiceDate);
            api.updateUser(userWS);
            api.createInvoiceWithDate(userWS.getId(), nextInvoiceDate, null, null, false);
            invoiceWS = api.getLatestInvoice(userWS.getId());
            assertEquals(invoiceWS.getCustomerNotes(), null);
        });
    }

    @Test(priority = 2)
    public void test002TestOneTimeInvoiceNoteInReviewInvoice(){

        testBuilder.given(envBuilder -> {

            final JbillingAPI api = envBuilder.getPrancingPonyApi();

            userId = createCustomer(envBuilder, CUSTOMER_CODE, envBuilder.env().idForCode(ACCOUNT_TYPE_CODE), activeSince);
            assertNotNull("UserId should not be null",userId);

            CustomerNoteWS note = new CustomerNoteWS();
            note.setNoteTitle("One Time Invoice Note");
            String s = "This is a <style isBold=\"true\" isItalic=\"true\" isUnderline=\"true\">static text</style> element containing styled text. <style backcolor=\"yellow\" isBold=\"true\" isItalic=\"true\">Styled text</style> elements are introduced by setting the <style forecolor=\"blue\" isItalic=\"true\">markup</style> attribute available for the <style isBold=\"true\" forecolor=\"magenta\">textElement</style> tag to <style forecolor=\"red\" isItalic=\"true\">styled</style> and by formatting the text content using nested <style isBold=\"true\" forecolor=\"green\">style</style> tags and simple HTML tags.";
            note.setNoteContent(s);
            note.setUserId(api.getCallerId());
            note.setCustomerId(api.getUserWS(userId).getCustomerId());
            note.setCreationTime(new Date());
            note.setNotesInInvoice(true);
            api.createCustomerNote(note);

            envBuilder.orderBuilder(api)
            .forUser(userId)
            .withProducts(envBuilder.env().idForCode(PRODUCT_CODE))
            .withBillingTypeId(Constants.ORDER_BILLING_PRE_PAID)
            .withActiveSince(activeSince)
            .withEffectiveDate(activeSince)
            .withDueDateUnit(Constants.PERIOD_UNIT_DAY)
            .withDueDateValue(1)
            .withCodeForTests("Order -1")
            .withPeriod(environmentHelper.getOrderPeriodMonth(api))
            .build();

        }).test((env)-> {
            final JbillingAPI api = env.getPrancingPonyApi();
            InvoiceWS invoiceWS = null;

            logger.debug("## billingDate ::::::::::::: {}",nextRunDate);

            UserWS userWS = api.getUserWS(userId);
            logger.debug("## User id:: {}",userWS.getId());
            assertEquals(userWS.getStatus(), "Active");
            //Review billing run
            updateBillingProcessConfiguration(nextRunDate, api, 1, Constants.PERIOD_UNIT_DAY);

            BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();
            logger.debug("## billing process config is review ::::::::::::: {}",config.getGenerateReport());
            logger.debug("## config.getNextRunDate() ::::::::::::: {}",config.getNextRunDate());
            api.triggerBilling(config.getNextRunDate());
            Integer[] reviewInvoices = api.getAllInvoices(userWS.getId());
            for (Integer reviewInvoice : reviewInvoices) {
                invoiceWS = api.getInvoiceWS(reviewInvoice);
                if (null != invoiceWS.getCustomerNotes()
                        && !invoiceWS.getCustomerNotes().isEmpty()
                        && !Constants.BLANK_STRING.equals(invoiceWS.getCustomerNotes())
                        && 1 == invoiceWS.getIsReview()) {
                    logger.debug("## Invoice id:: {}",invoiceWS.getId());
                    logger.debug("## invoiceWS.getIsReview():: {}",invoiceWS.getIsReview());
                    logger.debug("## One Time Invoice Note ::::::::::::: {}",invoiceWS.getCustomerNotes());
                    assertEquals(invoiceWS.getIsReview(), Integer.valueOf(1));
                    assertNotNull(invoiceWS.getCustomerNotes(), "One Time Invoice Note not popuplated in invoice");
                    assertEquals(invoiceWS.getCustomerNotes(), "This is a <style isBold=\"true\" isItalic=\"true\" isUnderline=\"true\">static text</style> element containing styled text. <style backcolor=\"yellow\" isBold=\"true\" isItalic=\"true\">Styled text</style> elements are introduced by setting the <style forecolor=\"blue\" isItalic=\"true\">markup</style> attribute available for the <style isBold=\"true\" forecolor=\"magenta\">textElement</style> tag to <style forecolor=\"red\" isItalic=\"true\">styled</style> and by formatting the text content using nested <style isBold=\"true\" forecolor=\"green\">style</style> tags and simple HTML tags.");
                    break;
                }
            }

            //Final billing run
            updateBillingProcessConfiguration(nextRunDate, api, 0, Constants.PERIOD_UNIT_DAY);

            config = api.getBillingProcessConfiguration();
            logger.debug("## billing process config is review ::::::::::::: {}",config.getGenerateReport());
            logger.debug("## config.getNextRunDate() ::::::::::::: {}",config.getNextRunDate());
            api.triggerBilling(config.getNextRunDate());
            Integer[] finalInvoices = api.getAllInvoices(userWS.getId());
            for (Integer finalInvoice : finalInvoices) {
                invoiceWS = api.getInvoiceWS(finalInvoice);
                if (null != invoiceWS.getCustomerNotes()
                        && !invoiceWS.getCustomerNotes().isEmpty()
                        && !Constants.BLANK_STRING.equals(invoiceWS.getCustomerNotes())
                        && 0 == invoiceWS.getIsReview()) {
                    logger.debug("## Invoice id:: {}",invoiceWS.getId());
                    logger.debug("## invoiceWS.getIsReview():: {}",invoiceWS.getIsReview());
                    logger.debug("## One Time Invoice Note ::::::::::::: {}",invoiceWS.getCustomerNotes());
                    assertEquals(invoiceWS.getIsReview(), Integer.valueOf(0));
                    assertNotNull(invoiceWS.getCustomerNotes(), "One Time Invoice Note not popuplated in invoice");
                    assertEquals(invoiceWS.getCustomerNotes(), "This is a <style isBold=\"true\" isItalic=\"true\" isUnderline=\"true\">static text</style> element containing styled text. <style backcolor=\"yellow\" isBold=\"true\" isItalic=\"true\">Styled text</style> elements are introduced by setting the <style forecolor=\"blue\" isItalic=\"true\">markup</style> attribute available for the <style isBold=\"true\" forecolor=\"magenta\">textElement</style> tag to <style forecolor=\"red\" isItalic=\"true\">styled</style> and by formatting the text content using nested <style isBold=\"true\" forecolor=\"green\">style</style> tags and simple HTML tags.");
                    break;
                }
            }
        });
    }

    private Integer createCustomer(TestEnvironmentBuilder envBuilder,String code, Integer accountTypeId, Date nid){
        final JbillingAPI api = envBuilder.getPrancingPonyApi();

        CustomerBuilder customerBuilder = envBuilder.customerBuilder(api)
                .withUsername(code).withAccountTypeId(accountTypeId)
                .withMainSubscription(new MainSubscriptionWS(environmentHelper.getOrderPeriodDay(api), Integer.valueOf(1)));

        UserWS user = customerBuilder.build();
        user.setNextInvoiceDate(nid);
        api.updateUser(user);
        return user.getId();
    }

    @AfterClass
    public void tearDown(){
        updateBillingProcessConfiguration(new LocalDate(nextRunDate).plusDays(1).toDate(), testBuilder.getTestEnvironment().getPrancingPonyApi(), 0, lastBillingProcess.getPeriodUnitId());
        testBuilder.removeEntitiesCreatedOnJBillingForMultipleTests();
        updatePlugin(FullCreativeTestConstants.INVOICE_COMPOSITION_PLUGIN_ID,
                FullCreativeTestConstants.ORDER_CHANGE_BASED_COMPOSITION_TASK_NAME, testBuilder, null);
        if (null != environmentHelper){
            environmentHelper = null;
        }
        if (null != testBuilder){
            testBuilder = null;
        }
    }

    private Integer buildAndPersistMetafield(TestBuilder testBuilder, String name, DataType dataType, EntityType entityType) {
        MetaFieldWS value =  new MetaFieldBuilder()
                                .name(name)
                                .dataType(dataType)
                                .entityType(entityType)
                                .primary(true)
                                .build();
        JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        Integer id = api.createMetaField(value);
        testBuilder.getTestEnvironment().add(name, id, id.toString(), api, TestEntityType.META_FIELD);
        return testBuilder.getTestEnvironment().idForCode(name);
    }

    private void updateBillingProcessConfiguration(Date nextRunDate, JbillingAPI api, Integer isReview, Integer periodUnit){

        BillingProcessConfigurationWS billingProcessConfiguration = api.getBillingProcessConfiguration();
        billingProcessConfiguration.setMaximumPeriods(100);
        billingProcessConfiguration.setNextRunDate(nextRunDate);
        billingProcessConfiguration.setPeriodUnitId(periodUnit);
        billingProcessConfiguration.setReviewStatus(Integer.valueOf(1));
        billingProcessConfiguration.setGenerateReport(isReview);
        billingProcessConfiguration.setOnlyRecurring(Integer.valueOf(0));
        api.createUpdateBillingProcessConfiguration(billingProcessConfiguration);
    }

	private void updatePlugin(Integer basicItemManagerPlugInId, String className, TestBuilder testBuilder, Hashtable<String, String> parameters) {
        final JbillingAPI api = testBuilder.getTestEnvironment().getPrancingPonyApi();
        PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(className);
        PluggableTaskWS plugin = api.getPluginWS(basicItemManagerPlugInId);
        plugin.setTypeId(type.getId());
        if(null!=parameters && !parameters.isEmpty()) {
            plugin.setParameters(parameters);
            }
        api.updatePlugin(plugin);
    }
}
