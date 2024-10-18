package com.sapienter.jbilling.server.user;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.partner.*;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.CurrencyWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.Asserts;
import com.sapienter.jbilling.test.TestUtils;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.fail;

@Test(groups = { "web-services", "partner" }, testName = "CommissionTest")
public class CommissionTest {

    private static final Logger logger = LoggerFactory.getLogger(CommissionTest.class);
    private static final Integer ADMIN_USER_ID = 2;
    private static final Integer PARTNER_ROLE_ID = 4;
    private static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private final static String AGENT_NAME = "AgentTest";
    private final static String ITEM_NAME = "ItemTest";

    private Date startDate = TestUtils.AsDate(2003, 2, 1);
    private Date oneMonthAfterStartDate = TestUtils.AsDate(2003, 3, 1);
    private Date endDate = TestUtils.AsDate(2003, 6, 1);
    private Date commissionRunDate = TestUtils.AsDate(2003, 4, 1);
    private DateTime ccommissionRunDateStartOfDay = new DateTime(commissionRunDate).withTimeAtStartOfDay();
    private DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy");
    private JbillingAPI api = null;

    @BeforeClass
    private void init() {
        try {
            api = JbillingAPIFactory.getAPI();
        } catch (Exception e) {
            logger.error("Cannot initialize JBilling API.", e);
        }
    }

    @Test
    public void testComissionWithoutConfiguration() {
        try {
            trigger();
        } catch (SessionInternalError sie) {
            assertEquals("Commission process trigger should fail",
                         sie.getErrorMessages()[0],
                         "partner.error.commissionProcess.no.configForEntity");
        }
    }

    @Test
    public void testCommissionCalculation() throws Exception {
        JbillingAPI api = JbillingAPIFactory.getAPI();
        Integer commissionItemId = null;
        PartnerWS standardPartner = null;
        PartnerWS masterPartner = null;
        PartnerWS referrerPartner = null;
        PartnerWS referrerPartnerWithCommission = null;
        PartnerWS childPartner = null;
        PartnerWS parentPartner = null;
        PartnerWS paymentPartner = null;
        PartnerWS exceptionPartner = null;
        PartnerWS dueExceptionPartner = null;
        PartnerWS unlimitedExceptionPartner = null;
        try {
            ItemDTOEx commissionItem = createItem(api, "commissionItem-" + new Date().getTime(), new BigDecimal("25.00"), new BigDecimal("50.00"));

            commissionItemId = commissionItem.getId();

            //Standard Partner
            standardPartner = createPartner(api, "Standard", PartnerType.STANDARD, null);
            createInvoiceForPartner(api, standardPartner, commissionItemId);
            logger.debug("standard finished");

            //Master Partner
            masterPartner = createPartner(api, "Master", PartnerType.MASTER, null);
            createInvoiceForPartner(api, masterPartner, commissionItemId);
            logger.debug("master finished");

            //Exception Partner
            exceptionPartner = createExceptionPartner(api, commissionItemId, null, null);
            createInvoiceForPartner(api, exceptionPartner, commissionItemId);
            logger.debug("exception finished");

            //Referrer Partner
            referrerPartner = createReferrerPartner(api, standardPartner);
            logger.debug("referrer finished");

            //Payment Partner
            paymentPartner = createPartner(api, "Payment", PartnerType.STANDARD, PartnerCommissionType.PAYMENT);
            Integer invoiceId = createInvoiceForPartner(api, paymentPartner, commissionItemId);
            applyPayment(api, paymentPartner.getUserId(), BigDecimal.ONE, invoiceId);
            logger.debug("payment finished");

            //Parent Partner
            parentPartner = createPartner(api, "Parent", PartnerType.STANDARD, null);
            logger.debug("parent finished");

            //Child Partner
            childPartner = createPartner(api, "Child", PartnerType.STANDARD, null);
            childPartner.setParentId(parentPartner.getId());
            updatePartner(api, childPartner);
            createInvoiceForPartner(api, childPartner, commissionItemId);
            logger.debug("child finished");

            //Due exception partner
            dueExceptionPartner = createExceptionPartner(api, commissionItemId, null, oneMonthAfterStartDate);
            createInvoiceForPartner(api, dueExceptionPartner, commissionItemId);
            logger.debug("dueException finished");

            //Unlimited exception partner
            unlimitedExceptionPartner = createExceptionPartner(api, commissionItemId, dtf.parseDateTime("01/01/2002").toDate(), null);
            createInvoiceForPartner(api, unlimitedExceptionPartner, commissionItemId);
            logger.debug("unlimitedException finished");

            //Referrer Partner with own commission
            referrerPartnerWithCommission = createReferrerPartner(api, unlimitedExceptionPartner);
            createInvoiceForPartner(api, referrerPartnerWithCommission, commissionItemId);
            logger.debug("referrer with commission finished");
            prepareAndTriggerCommissionProcess();

            //getting the commissionsRuns
            CommissionProcessRunWS[] commissionRuns = api.getAllCommissionRuns();

            CommissionProcessRunWS thisRun = null;
            for(CommissionProcessRunWS commissionRun : commissionRuns){
                if(new DateTime(commissionRun.getPeriodStart()).withTimeAtStartOfDay().equals(ccommissionRunDateStartOfDay) ){
                    logger.debug("This commission run found.");
                    thisRun = commissionRun;
                    break;
                }
            }

            if(thisRun == null){
                fail("Couldn't get the commission process run generated");
            }

            //Getting the commissions
            CommissionWS[] commissions = api.getCommissionsByProcessRunId(thisRun.getId());

            for(CommissionWS commission : commissions){
                //Standard Partner
                if(commission.getPartnerId().equals(standardPartner.getId())){
                    Asserts.assertEquals("Wrong standard partner commission amount", new BigDecimal("2.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type", CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Master Partner
                }else if (commission.getPartnerId().equals(masterPartner.getId())){
                    Asserts.assertEquals("Wrong master partner commission amount",new BigDecimal("5.00"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_MASTER_COMMISSION.name(), commission.getType());
                    //Exception Partner
                }else if (commission.getPartnerId().equals(exceptionPartner.getId())){
                    Asserts.assertEquals("Wrong exception partner commission amount",new BigDecimal("7.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.EXCEPTION_COMMISSION.name(), commission.getType());
                    //Referrer Partner
                }else if (commission.getPartnerId().equals(referrerPartner.getId())){
                    Asserts.assertEquals("Wrong referrer partner commission amount",new BigDecimal("1.25"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.REFERRAL_COMMISSION.name(), commission.getType());
                    //Parent Partner
                }else if (commission.getPartnerId().equals(parentPartner.getId())){
                    Asserts.assertEquals("Wrong parent partner commission amount",new BigDecimal("2.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Payment Partner
                }else if (commission.getPartnerId().equals(paymentPartner.getId())){
                    Asserts.assertEquals("Wrong payment partner commission amount",new BigDecimal("0.25"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Due Exception Partner
                }else if (commission.getPartnerId().equals(dueExceptionPartner.getId())){
                    Asserts.assertEquals("Wrong dueException partner commission amount",new BigDecimal("2.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_STANDARD_COMMISSION.name(), commission.getType());
                    //Unlimited Exception Partner
                }else if (commission.getPartnerId().equals(unlimitedExceptionPartner.getId())){
                    Asserts.assertEquals("Wrong unlimitedException partner commission amount",new BigDecimal("7.50"), commission.getAmountAsDecimal());
                    assertEquals("Wrong commission Type",CommissionType.EXCEPTION_COMMISSION.name(), commission.getType());
                    //Referrer partner with commission
                }else if (commission.getPartnerId().equals(referrerPartnerWithCommission.getId())){
                    Asserts.assertEquals("Wrong referrer partner with commission, commission amount",new BigDecimal("8.75"), commission.getAmountAsDecimal()); //5 for invoice. 3.75 referral
                    assertEquals("Wrong commission Type",CommissionType.DEFAULT_MASTER_COMMISSION.name(), commission.getType());
                }
            }
        } finally {
            //The partner deletes fail and probably should. We allow the removal of commissions linked to
            //a partner (look at deletePartner). This seems wrong. The WSSSB deletePArtner method specifically
            //checks for linked for commissions before allowing the delete.
//            api.deleteItem(commissionItemId);
//            deletePartner(api, standardPartner);
//            api.deleteUser(standardPartner.getUserId());
//
//            deletePartner(api, masterPartner);
//            api.deleteUser(masterPartner.getUserId());
//
//            deletePartner(api, referrerPartner);
//            api.deleteUser(referrerPartner.getUserId());
//
//            deletePartner(api, referrerPartnerWithCommission);
//            api.deleteUser(referrerPartnerWithCommission.getUserId());
//
//            deletePartner(api, childPartner);
//            api.deleteUser(childPartner.getUserId());
//
//            deletePartner(api, parentPartner);
//            api.deleteUser(parentPartner.getUserId());
//
//            deletePartner(api, paymentPartner);
//            api.deleteUser(paymentPartner.getUserId());
//
//            deletePartner(api, exceptionPartner);
//            api.deleteUser(exceptionPartner.getUserId());
//
//            deletePartner(api, dueExceptionPartner);
//            api.deleteUser(dueExceptionPartner.getUserId());
//
//            deletePartner(api, unlimitedExceptionPartner);
//            api.deleteUser(unlimitedExceptionPartner.getUserId());
        }
    }

    private PartnerWS createPartner(JbillingAPI api, String name, PartnerType partnerType, PartnerCommissionType partnerCommissionType){
        // new partner
        String version = ""+new Date().getTime();
        UserWS user = new UserWS();
        user.setUserName("partner-01-" + version);
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(1);
        user.setMainRoleId(PARTNER_ROLE_ID);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        ContactWS contact = new ContactWS();
        contact.setEmail(user.getUserName() + "@test.com");
        contact.setFirstName("Partner" + name);
        contact.setLastName(version);
        contact.setAddress1("123 Evergreen AV.");
        contact.setStateProvince("Springfield");
        contact.setCountryCode("US");
        user.setContact(contact);

        PartnerWS partner = new PartnerWS();
        partner.setType(partnerType.name());
        if(partnerCommissionType != null){
            partner.setCommissionType(partnerCommissionType.name());
        }

        // create partner
        partner.setId(api.createPartner(user, partner));

        return api.getPartner(partner.getId());
    }

    private void updatePartner(JbillingAPI api, PartnerWS partner){
        api.updatePartner(api.getUserWS(partner.getUserId()), partner);
    }

    private void deletePartner(JbillingAPI api, PartnerWS partner) {
        partner.setCommissions(new CommissionWS[0]);
        updatePartner(api, partner);
        api.deletePartner(partner.getId());
    }

    private PartnerWS createExceptionPartner(JbillingAPI api, Integer commissionItemId, Date startDate, Date endDate){
        PartnerWS exceptionPartner = createPartner(api, "Exception", PartnerType.STANDARD, null);
        PartnerCommissionExceptionWS commissionException = new PartnerCommissionExceptionWS();
        commissionException.setPercentage(new BigDecimal("75.00"));
        commissionException.setStartDate(startDate != null ? startDate : this.startDate);
        commissionException.setEndDate(endDate != null ? endDate : this.endDate);
        commissionException.setItemId(commissionItemId);
        exceptionPartner.setCommissionExceptions(new PartnerCommissionExceptionWS[]{commissionException});
        updatePartner(api, exceptionPartner);
        return  exceptionPartner;
    }

    private PartnerWS createReferrerPartner(JbillingAPI api, PartnerWS referralPartner){
        PartnerWS referrerPartner = createPartner(api, "Referrer", PartnerType.MASTER, null);
        PartnerReferralCommissionWS referralCommission = new PartnerReferralCommissionWS();
        referralCommission.setPercentage(new BigDecimal("50.00"));
        referralCommission.setStartDate(startDate);
        referralCommission.setEndDate(endDate);
        referralCommission.setReferralId(referralPartner.getId());
        referrerPartner.setReferrerCommissions(new PartnerReferralCommissionWS[]{referralCommission});
        updatePartner(api, referrerPartner);
        return referrerPartner;
    }

    private UserWS createCustomer(JbillingAPI api, PartnerWS partner){
        UserWS user = null;
        try{
            user = com.sapienter.jbilling.server.user.WSTest.createUser(true, null, null);
            user.setPartnerIds(new Integer[] {partner.getId()});
            user.setPassword(null);
            api.updateUser(user);
            updateNextInvoiceDate(user.getId());
            logger.debug("userUpdated with id: {}", user.getUserId());
        }catch (Exception e){
            fail("Exception creating customer");
        }
        return user;
    }

    private void updateNextInvoiceDate(Integer userId) {
        UserWS user = api.getUserWS(userId);
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
    }
    
    private Integer createInvoiceForPartner (JbillingAPI api, PartnerWS partner, Integer itemId) {
        UserWS user = createCustomer(api, partner);

        // setup orders
        OrderWS order = new OrderWS();
        order.setUserId(user.getUserId());
        order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
        order.setPeriod(1); // once
        order.setCurrencyId(1);
        order.setActiveSince(commissionRunDate);

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setItemId(itemId);
        line.setQuantity(1);
        line.setUseItem(true);

        order.setOrderLines(new OrderLineWS[] { line });

        // create orders
        Integer orderId1 = api.createOrder(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        // generate invoice using first order
        Integer invoiceId = api.createInvoiceFromOrder(orderId1, null);

        assertNotNull("Order 1 created", orderId1);
        assertNotNull("Invoice created", invoiceId);

        return invoiceId;
    }

    private ItemDTOEx createItem(JbillingAPI api, String description, BigDecimal standardPercentage, BigDecimal masterPercentage){
        try {
            ItemDTOEx newItem = new ItemDTOEx();

            List<InternationalDescriptionWS> descriptions = new java.util.ArrayList<InternationalDescriptionWS>();
            InternationalDescriptionWS enDesc = new InternationalDescriptionWS(1, description);
            descriptions.add(enDesc);

            newItem.setPriceModelCompanyId(new Integer(1));
            newItem.setDescriptions(descriptions);
            newItem.setPrice(new BigDecimal("10.0"));
            newItem.setNumber(description);
            newItem.setHasDecimals(0);
            newItem.setAssetManagementEnabled(0);
            newItem.setStandardPartnerPercentage(standardPercentage);
            newItem.setMasterPartnerPercentage(masterPercentage);
            Integer types[] = new Integer[1];
            types[0] = new Integer(1);
            newItem.setTypes(types);

            logger.debug("Creating item ... {}",  newItem);
            Integer ret = api.createItem(newItem);
            assertNotNull("The item was not created", ret);
            logger.debug("Done!");
            newItem.setId(ret);

            return newItem;
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception caught:" + e);
            return null;
        }
    }

    private void applyPayment(JbillingAPI api, Integer userId, BigDecimal amount, Integer invoiceId){

        PaymentWS payment = new PaymentWS();
        payment.setAmount(amount);
        payment.setIsRefund(new Integer(0));
        payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
        payment.setPaymentDate(commissionRunDate);
        payment.setResultId(Constants.RESULT_ENTERED);
        payment.setCurrencyId(new Integer(1));
        payment.setUserId(userId);
        payment.setPaymentNotes("Notes");
        payment.setPaymentPeriod(new Integer(1));

        PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest.createCheque("ws bank", "2232-2323-2323", Calendar.getInstance().getTime());
        payment.getPaymentInstruments().add(cheque);

        api.applyPayment(payment, invoiceId);
    }

    @Test
    public void testCommissionCurrencyConversion() {
        ItemDTOEx product = null;
        PartnerWS agent = null;
        PartnerWS japanAgent = null;
        Integer invoiceID = null;
        Integer invoiceID2 = null;
        try {
            //Create a product
            String itemName = ITEM_NAME + new Date();
            product = createItem(api,itemName,new BigDecimal(50),new BigDecimal(80));

            //Create an agent with USD currency
            Integer currencyID = null;
            for(CurrencyWS currency : api.getCurrencies()) {
                if(currency.getCode().equals("USD")) {
                    currencyID = currency.getId();
                }
            }
            String name = "COMMON_2_" + AGENT_NAME;
            agent = createPartnerWithCurrency(name,PartnerType.STANDARD, PartnerCommissionType.INVOICE, currencyID);

            //Create a customer
            createCustomer(api,agent);
            //Create an order & generate the invoice in USD
            invoiceID = createInvoiceForPartner(api,agent,product.getId());


            //Create japan agent with currency set to YEN
            for(CurrencyWS currency : api.getCurrencies()) {
                if(currency.getCode().equals("JPY")) {
                    currencyID = currency.getId();
                }
            }
            logger.debug("Currency {}", currencyID) ;
            name = "JAPAN_6_" + AGENT_NAME;
            japanAgent = createPartnerWithCurrency(name,PartnerType.STANDARD, PartnerCommissionType.INVOICE, currencyID);
            //Create an order & generate the invoice in USD
            invoiceID2 = createInvoiceForPartner(api,japanAgent,product.getId());


            //Trigger the commission process
            prepareAndTriggerCommissionProcess();
            CommissionWS[] commissions = getGeneratedCommissions2();
            for(CommissionWS commission : commissions) {
                Integer userID = api.getPartner(commission.getPartnerId()).getUserId();
                logger.debug("Integer agentID = " + userID);
                Integer agentCurrencyID = api.getUserWS(userID).getCurrencyId();
                logger.debug("Commission currencyID: {}", commission.getCurrencyId());
                logger.debug("Agent currencyID: {}", agentCurrencyID);
                assertEquals("The commission currency equals the agent currency", commission.getCurrencyId(), agentCurrencyID);
                if(commission.getPartnerId().equals(agent.getId())) {
                    assertEquals(new BigDecimal(5.00).floatValue(),commission.getAmountAsDecimal().floatValue(),0.001);
                }
                if(commission.getPartnerId().equals(japanAgent.getId())) {
                    assertEquals(new BigDecimal(557.00).floatValue(),commission.getAmountAsDecimal().floatValue(),0.001);
                }
            }
        } finally {
//            deleteInvoice(api, invoiceID2);
//            deleteInvoice(api, invoiceID);
//            deletePartner(api, agent);
//            api.deleteUser(agent.getUserId());
//            deletePartner(api, japanAgent);
//            api.deleteUser(japanAgent.getUserId());
//            api.deleteItem(product.getId());
        }
    }

    private void deleteInvoice(JbillingAPI api, Integer invoiceId) {
        Integer[] orderIds = api.getInvoiceWS(invoiceId).getOrders();
        api.deleteInvoice(invoiceId);
        for (Integer orderId: orderIds) {
            api.deleteOrder(orderId);
        }
    }


    private void prepareAndTriggerCommissionProcess() {
        try {
            CommissionProcessConfigurationWS configurationWS = new CommissionProcessConfigurationWS();
            configurationWS.setEntityId(1);
            configurationWS.setNextRunDate(commissionRunDate);
            configurationWS.setPeriodUnitId(PeriodUnitDTO.MONTH);
            configurationWS.setPeriodValue(1);
            api.createUpdateCommissionProcessConfiguration(configurationWS);
            logger.debug("commission process configuration finished");
        } catch(Exception e) {
            logger.error("The commission process is already running.", e);
        }
        trigger();
    }

    private void trigger() {
        //Trigger commission process.
        api.calculatePartnerCommissions();
        logger.debug("triggering the commission process finished.");
    }

    private PartnerWS createPartnerWithCurrency(String name, PartnerType partnerType,
                                                PartnerCommissionType partnerCommissionType, Integer currencyID) {
        // new partner
        String version = ""+new Date().getTime();
        UserWS user = new UserWS();
        user.setUserName(name);
        user.setPassword("P@ssword1");
        user.setLanguageId(1);
        user.setCurrencyId(currencyID);
        user.setMainRoleId(PARTNER_ROLE_ID);
        user.setStatusId(UserDTOEx.STATUS_ACTIVE);

        /*
        ContactWS contact = new ContactWS();
        contact.setEmail(user.getUserName() + "@test.com");
        contact.setFirstName("Partner" + name);
        contact.setLastName(version);
        user.setContact(contact);
*/
        PartnerWS partner = new PartnerWS();
        partner.setType(partnerType.name());

        // create partner
        partner.setId(api.createPartner(user, partner));

        return api.getPartner(partner.getId());
    }

    private CommissionWS[] getGeneratedCommissions2() {
        CommissionProcessRunWS[] commissionRuns = api.getAllCommissionRuns();
        //Getting the commissions
        return api.getCommissionsByProcessRunId(commissionRuns[commissionRuns.length-1].getId());
    }

}
