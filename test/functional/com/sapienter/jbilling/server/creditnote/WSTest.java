package com.sapienter.jbilling.server.creditnote;

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

import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.order.*;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import static org.testng.AssertJUnit.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by nazish on 6/8/15.
 */

@Test(groups = { "web-services", "creditNote" }, testName = "creditNote.WSTest")
public class WSTest {

    private static final Logger logger = LoggerFactory.getLogger(WSTest.class);

    private JbillingAPI api;
    private static Integer ORDER_CHANGE_STATUS_APPLY_ID;
    private static Integer DYNAMIC_BALANCE_MANAGER_PLUGIN_ID;

    @BeforeClass
    private void setUp() throws Exception {
        api = JbillingAPIFactory.getAPI();
        ORDER_CHANGE_STATUS_APPLY_ID = getOrCreateOrderChangeStatusApply();
        DYNAMIC_BALANCE_MANAGER_PLUGIN_ID = getOrCreatePluginWithoutParams(
                "com.sapienter.jbilling.server.user.balance.DynamicBalanceManagerTask", 10003);
    }

    @AfterClass
    protected void tearDown() throws Exception {
        if(null != DYNAMIC_BALANCE_MANAGER_PLUGIN_ID) {
            api.deletePlugin(DYNAMIC_BALANCE_MANAGER_PLUGIN_ID);
        }
    }

    @Test
    public void test01CreditNoteGeneration() throws Exception{
        //Creating User
        UserWS user = createMockUser(false, null, 1, true);

        logger.debug("Creating Orders and Invoices.....");

        OrderWS order1 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, BigDecimal.TEN);
        Integer invoiceId1 = api.createOrderAndInvoice(order1, OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID));

        OrderWS order2 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, BigDecimal.TEN);
        Integer invoiceId2 = api.createOrderAndInvoice(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));

        user =  api.getUserWS(user.getId());
        assertEquals("Expected dynamic Balance " ,"-20.0000000000", user.getDynamicBalance() );
        assertEquals("Expected Owing Balance ","20.0000000000", user.getOwingBalance() );

        //Creating the negative Invoice for Credit Note generation
        logger.debug("Creating Negative Order and Invoice .......");

        OrderWS order3 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID,Constants.ORDER_LINE_TYPE_ITEM, new BigDecimal(-30));
        Integer invoiceId3 = api.createOrderAndInvoice(order3, OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID));

        user =  api.getUserWS(user.getId());
        assertEquals("Expected dynamic Balance ", "10.0000000000", user.getDynamicBalance() );
        assertEquals("Expected Owing Balance ", "-10.0000000000", user.getOwingBalance() );

        //Checking Ccredit Note creation
        Integer creationInvoiceId = 0;
        String balance = null;
        for(CreditNoteWS creditNoteWS : api.getAllCreditNotes(api.getCallerCompanyId())) {
            if(creditNoteWS.getCreationInvoiceId().equals(invoiceId3)) {
                creationInvoiceId = creditNoteWS.getCreationInvoiceId();
                balance = creditNoteWS.getBalance();
            }
        }
        assertEquals("Expected CreditNote creation ", invoiceId3, creationInvoiceId);

        InvoiceWS invoice1 = api.getInvoiceWS(invoiceId1);
        InvoiceWS invoice2 = api.getInvoiceWS(invoiceId2);

        assertEquals("Expected first Invoice Balance", "0E-10", invoice1.getBalance());
        assertEquals("Expected first Invoice status", Constants.INVOICE_STATUS_PAID, invoice1.getStatusId());
        assertEquals("Expected second Invoice Balance2", "0E-10", invoice2.getBalance());
        assertEquals("Expected second Invoice status", Constants.INVOICE_STATUS_PAID, invoice2.getStatusId());
        assertEquals("Expected credit Note balance ", "10.0000000000", balance);
    }

    /*
     * Linking Invoice to Credit Note where Invoice Balance is greater than Credit Note
     */
    @Test
    public void test02LinkInvoiceToCreditNote() throws Exception {
        //Creating User
        UserWS user = createMockUser(false, null, 1, true);

        //Creating the negative Invoice for Credit Note generation
        logger.debug("Creating Negative Order and Invoice .......");

        OrderWS order = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, new BigDecimal(-10));
        Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        user =  api.getUserWS(user.getId());
        assertEquals("Expected dynamic Balance ", "10.0000000000", user.getDynamicBalance());
        assertEquals("Expected Owing Balance ", "-10.0000000000", user.getOwingBalance());

        //Checking Credit Note creation
        Integer creationInvoiceId = 0;
        Integer creditNoteId = 0;
        for (CreditNoteWS creditNoteWS : api.getAllCreditNotes(api.getCallerCompanyId())) {
            Integer creditNoteCreationInvoiceId = creditNoteWS.getCreationInvoiceId();
            if (creditNoteCreationInvoiceId.equals(invoiceId)) {
                creationInvoiceId = creditNoteWS.getCreationInvoiceId();
                creditNoteId = creditNoteWS.getId();
            }
        }
        assertEquals("Expected CreditNote creation ", invoiceId, creationInvoiceId);

        //Creating debit Invoice
        logger.debug("Creating Order and Invoice.....");

        OrderWS order1 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, new BigDecimal(30));
        Integer invoiceId1 = api.createOrderAndInvoice(order1, OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID));

        InvoiceWS debitInvoice = api.getInvoiceWS(invoiceId1);
        CreditNoteWS creditNote = api.getCreditNote(creditNoteId);

        //Credit Note already applied to Credit Note because of "ApplyNegativeInvoiceToCreditNoteTask" Internal Event Task
        assertEquals("Expected debit Invoice balance ", "20.0000000000", debitInvoice.getBalance());
        assertEquals("Expected Credit note balance should be null", "0E-10", creditNote.getBalance());

        //Removing Credit Note because Internal Event Task "ApplyNegativeInvoiceToCreditNoteTask" links Newly created Invoices to Credit Notes with balance
        api.removeCreditNoteLink(invoiceId1,creditNoteId);

        debitInvoice = api.getInvoiceWS(invoiceId1);
        creditNote = api.getCreditNote(creditNoteId);

        assertEquals("Expected debit Invoice balance ", "30.0000000000", debitInvoice.getBalance());
        assertEquals("Expected Credit note balance ", "10.0000000000", creditNote.getBalance());

        api.applyCreditNoteToInvoice(creditNote.getId(), debitInvoice.getId());

        debitInvoice = api.getInvoiceWS(invoiceId1);
        creditNote = api.getCreditNote(creditNoteId);

        assertEquals("Expected debit Invoice balance ", "20.0000000000", debitInvoice.getBalance());
        assertEquals("Expected Credit note balance ", "0E-10", creditNote.getBalance());
    }

    /*
     * Linking Invoice to Credit Note where Invoice Balance is greater than Credit Note
     */
    @Test
    public void test03LinkInvoiceToCreditNote() throws Exception {
        //Creating User
        UserWS user = createMockUser(false, null, 1, true);

        //Creating the negative Invoice for Credit Note generation
        logger.debug("Creating Negative Order and Invoice .......");

        OrderWS order = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, new BigDecimal(-30));
        Integer invoiceId = api.createOrderAndInvoice(order, OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID));

        user =  api.getUserWS(user.getId());
        assertEquals("Expected dynamic Balance ", "30.0000000000", user.getDynamicBalance());
        assertEquals("Expected Owing Balance ", "-30.0000000000", user.getOwingBalance());

        //Checking Credit Note creation
        Integer creationInvoiceId = 0;
        Integer creditNoteId = 0;
        for (CreditNoteWS creditNoteWS : api.getAllCreditNotes(api.getCallerCompanyId())) {
            Integer creditNoteCreationInvoiceId = creditNoteWS.getCreationInvoiceId();
            if (creditNoteCreationInvoiceId.equals(invoiceId)) {
                creationInvoiceId = creditNoteWS.getCreationInvoiceId();
                creditNoteId = creditNoteWS.getId();
            }
        }
        assertEquals("Expected CreditNote creation ", invoiceId, creationInvoiceId);

        //Creating debit Invoice
        logger.debug("Creating Order and Invoice.....");

        OrderWS order1 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, new BigDecimal(10));
        Integer invoiceId1 = api.createOrderAndInvoice(order1, OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID));

        InvoiceWS debitInvoice = api.getInvoiceWS(invoiceId1);
        CreditNoteWS creditNote = api.getCreditNote(creditNoteId);

        //Credit Note already applied to Credit Note because of "ApplyNegativeInvoiceToCreditNoteTask" Internal Event Task
        assertEquals("Expected debit Invoice balance ", "0E-10", debitInvoice.getBalance());
        assertEquals("Expected Credit note balance ", "20.0000000000", creditNote.getBalance());

        //Removing Credit Note because Internal Event Task "ApplyNegativeInvoiceToCreditNoteTask" links Newly created Invoices to Credit Notes with balance
        api.removeCreditNoteLink(invoiceId1, creditNoteId);

        debitInvoice = api.getInvoiceWS(invoiceId1);
        creditNote = api.getCreditNote(creditNoteId);

        assertEquals("Expected debit Invoice balance ", "10.0000000000", debitInvoice.getBalance());
        assertEquals("Expected Credit note balance ", "30.0000000000", creditNote.getBalance());

        api.applyCreditNoteToInvoice(creditNote.getId(), debitInvoice.getId());

        debitInvoice = api.getInvoiceWS(invoiceId1);
        creditNote = api.getCreditNote(creditNoteId);

        assertEquals("Expected debit Invoice balance ", "0E-10", debitInvoice.getBalance());
        assertEquals("Expected Credit note balance ", "20.0000000000", creditNote.getBalance());
    }

    /*
     * Apply negative credit adjustment order to invoice against who has one carried invoice line.
     * credit adjustment order total amount is greater than invoice balance but less than amount.
     */
    @Test
    public void test04CreditNoteWithCarriedInvoice() throws Exception{
        //Creating User
        UserWS user = createMockUser(false, null, 1, true);

        logger.debug("Creating Orders and Invoices.....");

        OrderWS order1 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, BigDecimal.TEN);
        Integer orderId = api.createOrder(order1, OrderChangeBL.buildFromOrder(order1, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer[] invoiceId1 = api.createInvoiceWithDate(user.getId(), new Date(), null, null, false);

        OrderWS order2 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID, Constants.ORDER_LINE_TYPE_ITEM, BigDecimal.TEN);
        orderId = api.createOrder(order2, OrderChangeBL.buildFromOrder(order2, ORDER_CHANGE_STATUS_APPLY_ID));
        Integer[] invoiceId2 = api.createInvoiceWithDate(user.getId(), new Date(), null, null, false);
        InvoiceWS invoiceWs = api.getInvoiceWS(invoiceId2[0]);

        user =  api.getUserWS(user.getId());
        assertEquals("Expected dynamic Balance " ,"-20.0000000000", user.getDynamicBalance() );
        assertEquals("Expected Owing Balance ","20.0000000000", user.getOwingBalance() );
        assertEquals("Expected second Invoice carried balance", "10.0000000000", invoiceWs.getCarriedBalance());

        //Creating the negative Invoice for Credit Note generation
        logger.debug("Creating Negative Order and Invoice .......");

        OrderWS order3 = createMockOrder(user.getId(), Constants.ORDER_BILLING_POST_PAID,Constants.ORDER_LINE_TYPE_ITEM, new BigDecimal(-15));
        orderId = api.createOrder(order3, OrderChangeBL.buildFromOrder(order3, ORDER_CHANGE_STATUS_APPLY_ID));
        invoiceWs = api.getInvoiceWS(invoiceId2[0]);
        Integer invoiceId3 = api.applyOrderToInvoice(orderId, invoiceWs);

        user =  api.getUserWS(user.getId());
        assertEquals("Expected dynamic Balance ", "-5.0000000000", user.getDynamicBalance() );
        assertEquals("Expected Owing Balance ", "5.0000000000", user.getOwingBalance() );

        //Checking Ccredit Note creation
        Integer creationInvoiceId = 0;
        String balance = null;
        for(CreditNoteWS creditNoteWS : api.getAllCreditNotes(api.getCallerCompanyId())) {
            if(creditNoteWS.getCreationInvoiceId().equals(invoiceId3)) {
                creationInvoiceId = creditNoteWS.getCreationInvoiceId();
                balance = creditNoteWS.getBalance();
            }
        }
        assertEquals("Expected CreditNote creation ", invoiceId3, creationInvoiceId);

        InvoiceWS invoice1 = api.getInvoiceWS(invoiceId1[0]);
        InvoiceWS invoice2 = api.getInvoiceWS(invoiceId2[0]);

        assertEquals("Expected first Invoice Balance", "5.0000000000", invoice1.getBalance());
        assertEquals("Expected second Invoice Balance2", "0E-10", invoice2.getBalance());
        assertEquals("Expected Invoice Status", Constants.INVOICE_STATUS_PAID, invoice2.getStatusId());
        assertEquals("Expected credit Note balance ", "0E-10", balance);
    }

    /*
    * Create - This passes the password validation routine.
    */
    public UserWS createMockUser(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate) throws JbillingAPIException, IOException {
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // It is validated
        newUser.setUserName("john-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("Admin123@");
        newUser.setAccountTypeId(1);
        newUser.setLanguageId(1);
        newUser.setMainRoleId(5);
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(false);

        //Add a contact
        ContactWS contact = new ContactWS();
        contact.setEmail("amol.gadre@test.com");
        contact.setFirstName("AMOL");
        contact.setLastName("GADRE");
        contact.setAddress1("C/8 SWANAND SOCIETY LANE 2 SAHAKAR NAGAR 2");
        contact.setAddress2("");
        contact.setCity("PUNE");
        contact.setStateProvince("MAH");
        contact.setPostalCode("411009");
        contact.setCountryCode("IN");
        newUser.setContact(contact);

        List<MetaFieldValueWS> metaFieldValues = new ArrayList<>();
        metaFieldValues.add(new MetaFieldValueWS("contact.email", null, DataType.STRING, true, "test@test.com"));
        newUser.setMetaFields(metaFieldValues.toArray(new MetaFieldValueWS[metaFieldValues.size()]));

        if (doCreate) {
            logger.debug("Creating user ...");
            newUser.setUserId(api.createUser(newUser));
        }
        updateNextInvoiceDate(newUser.getId());
        return newUser;
    }

    private OrderWS createMockOrder(Integer userId, Integer billingTypeId, Integer orderBilTypId, BigDecimal purchaseOrderPrice)throws JbillingAPIException, IOException {
        //Create item category
        Integer itemTypeId = createMockItemType();
        assertNotNull(itemTypeId);

        //Create item
        ItemDTOEx item = createMockItem(0, itemTypeId, "10");
        Integer itemId = item.getId();
        assertNotNull(itemId);

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(billingTypeId);
        order.setPeriod(1); //Once
        order.setCurrencyId(1);
        order.setActiveSince(new Date());

        OrderLineWS line = new OrderLineWS();
        line.setTypeId(orderBilTypId);
        line.setPrice(purchaseOrderPrice);
        line.setQuantity(1);
        line.setDescription("Order line");
        line.setAmount(purchaseOrderPrice);
        line.setItemId(itemId);
        line.setUseItem(false);

        order.setOrderLines(new OrderLineWS[]{line});

        return order;
    }

    private ItemDTOEx createMockItem(int carrierCode, Integer itemTypeId, String amount)throws JbillingAPIException, IOException {
        ItemDTOEx newItem = new ItemDTOEx();
        newItem.setDescription("an item from ws");
        newItem.setPrice(new BigDecimal(amount));
        newItem.setNumber("WS-001");

        if(carrierCode != 0 ) {
            MetaFieldValueWS metaField1 = new MetaFieldValueWS();
            metaField1.setFieldName("CARRIER_CODE");
            metaField1.setValue("Carrier" + carrierCode);
            newItem.setMetaFields(new MetaFieldValueWS[] { metaField1 });
        }

        Integer types[] = new Integer[1];
        types[0] = itemTypeId;
        newItem.setTypes(types);

        logger.debug("Creating item {}", newItem);
        Integer ret = api.createItem(newItem);
        newItem.setId(ret);
        logger.debug("Done!");
        return newItem;
    }

    private Integer createMockItemType()throws JbillingAPIException, IOException {
        //Create item category
        ItemTypeWS itemType = new ItemTypeWS();
        itemType.setDescription("Frozen Food " + new Date().getTime());
        itemType.setOrderLineTypeId(1);
        return api.createItemCategory(itemType);
    }

    private Integer getOrCreateOrderChangeStatusApply() {
        OrderChangeStatusWS[] statuses = api.getOrderChangeStatusesForCompany();
        for (OrderChangeStatusWS status : statuses) {
            if (status.getApplyToOrder().equals(ApplyToOrder.YES)) {
                return status.getId();
            }
        }
        //There is no APPLY status in db so create one
        OrderChangeStatusWS apply = new OrderChangeStatusWS();
        String status1Name = "APPLY: " + System.currentTimeMillis();
        OrderChangeStatusWS status1 = new OrderChangeStatusWS();
        status1.setApplyToOrder(ApplyToOrder.YES);
        status1.setDeleted(0);
        status1.setOrder(1);
        status1.addDescription(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, status1Name));
        return api.createOrderChangeStatus(apply);
    }
    
    private void updateNextInvoiceDate(Integer userId) {
        UserWS user = api.getUserWS(userId);
        Calendar nextInvoiceDate = Calendar.getInstance();
        nextInvoiceDate.setTime(user.getNextInvoiceDate());
        nextInvoiceDate.add(Calendar.MONTH, 1);
        user.setNextInvoiceDate(nextInvoiceDate.getTime());
        api.updateUser(user);
    }

    private Integer getOrCreatePluginWithoutParams(String className, int processingOrder) {
        PluggableTaskWS[] taskWSs = api.getPluginsWS(api.getCallerCompanyId(), className);
        if(taskWSs.length != 0){
            return taskWSs[0].getId();
        }
        PluggableTaskWS pluggableTaskWS = new PluggableTaskWS();
        pluggableTaskWS.setTypeId(api.getPluginTypeWSByClassName(className).getId());
        pluggableTaskWS.setProcessingOrder(processingOrder);
        pluggableTaskWS.setOwningEntityId(api.getCallerCompanyId());
        return api.createPlugin(pluggableTaskWS);
    }
}
