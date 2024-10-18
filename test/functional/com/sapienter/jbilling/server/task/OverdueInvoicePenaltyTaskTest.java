/*
JBILLING CONFIDENTIAL
_____________________

[2003] - [2012] Enterprise jBilling Software Ltd.
All Rights Reserved.

NOTICE:  All information contained herein is, and remains
the property of Enterprise jBilling Software.
The intellectual and technical concepts contained
herein are proprietary to Enterprise jBilling Software
and are protected by trade secret or copyright law.
Dissemination of this information or reproduction of this material
is strictly forbidden.
 */

package com.sapienter.jbilling.server.task;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.ItemTypeWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.process.db.ProratingType;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.test.ApiTestCase;

/**
 * A jUnit test class to test OverdueInvoicePenaltyTask plug-in.
 * 
 * @author Ashok Kale
 * @since 11-FEB-2011
 * 
 */
@Test(groups = { "integration", "task", "penalty", "overdueInvoicePenalty" }, testName = "OverdueInvoicePenaltyTaskTest")
public class OverdueInvoicePenaltyTaskTest extends ApiTestCase {

    private static final Logger logger = LoggerFactory.getLogger(OverdueInvoicePenaltyTaskTest.class);
    private static final Integer OVERDUE_INVOICE_PENALTY_TASK_TYPE_ID = 97;
    private final static String PLUGIN_PARAMETER_ITEM = "penalty_item_id";
    private final static String PLUGIN_PARAMETER_CHARGE_ITEM = "penalty_charge_item_id";
    private static int CURRENCY_USD;
    private static int LANGUAGE_ID;
    JbillingAPI api;
    private static int PRANCING_PONY_ACCOUNT_TYPE;
    private final static int ORDER_PERIOD_MONTHLY = Integer.valueOf(2);
    public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-dd-MM");
    private Integer penaltyPluginId = null;
    
    protected void prepareTestInstance() throws Exception {
		super.prepareTestInstance();
		api = JbillingAPIFactory.getAPI();
		CURRENCY_USD = Constants.PRIMARY_CURRENCY_ID;
		LANGUAGE_ID = Constants.LANGUAGE_ENGLISH_ID;
		PRANCING_PONY_ACCOUNT_TYPE = Integer.valueOf(1);
	}
    
    /**
     * Test that an Overdue Invoice (Unpaid Invoice) causes a new Penalty Order to be created for that user,
     * just prior to the next billing run. The penalty order is created using a Penalty Category Item
     * @throws Exception
     */
    	
    @Test
    public void test001OverdueInvoice() throws Exception {
	    ItemTypeWS penaltyCategory = buildPenaltyCategory();
	    penaltyCategory.setId(api.createItemCategory(penaltyCategory));

	    logger.debug("Creating the Penalty Item.");
	    Integer penaltyItemId = createPenaltyItem(penaltyCategory.getId(), PriceModelStrategy.LINE_PERCENTAGE.name(), new BigDecimal("1.50"));
	    assertNotNull("Late Fee Penalty Item Id  should not be null.", penaltyItemId);
	    
	    Integer penaltyChargeItemId = createPenaltyItem(penaltyCategory.getId(), PriceModelStrategy.FLAT.name(), new BigDecimal("2.00"));
	    assertNotNull("Late Fee Penalty Charge Item Id  should not be null.", penaltyChargeItemId);

	    logger.debug("# Penalty Item ID: {} \n# Penalty Charge Item ID {} \nAdding a OverdueInvoicePenaltyTask Plugin.", penaltyItemId, penaltyChargeItemId);

	    penaltyPluginId = enableOverdueInvoicePenaltyPlugin(penaltyItemId, penaltyChargeItemId);
	    assertNotNull("Plugin id is not null.", penaltyPluginId);
	    
        // create a user for testing
        Integer userId = api.createUser(createUserWS("invoice-penaly-01-" + System.currentTimeMillis(), Constants.PERIOD_UNIT_MONTH, 1));//due date 1 month or 30 days
        assertNotNull("Test fail at user creation.", userId);
        logger.debug("User Created with Id ::: {}", userId);
        
        UserWS user = api.getUserWS(userId);
        user.setNextInvoiceDate(getDate(1, 1, 2008));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("User Next Invoice Date :::{}", DATE_FORMAT.format(user.getNextInvoiceDate()));
        
        ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));
	    Integer itemId = createItem(itemCategory.getId());

        logger.debug("Plugin ID: {} \nCreating order for user...", penaltyPluginId);
     // Set active since to 07 June 2010
        Integer orderId = createOrderWs(userId, itemId, getDate(12, 07, 2007), PeriodUnitDTO.DAY);
        OrderWS orderWS = api.getOrder(orderId);
        assertNotNull("order created", orderWS.getId());
        logger.debug("Order Created with Id :::{}", orderId);
        
        // update the billing process
        updateBillingConfig(getDate(1, 1, 2008));
        // now trigger the billing, and the invoice should be made of the above order
        // trigger the billing on 1st JULY 2010
        logger.debug("Triggered Billing Process for Date ::: {}", getDate(1, 1, 2008));
        logger.debug("Wait 2 Minute for Billing Process to Complete :::");
	    api.triggerBilling(getDate(1, 1, 2008));
        // now check the orders of the customer
	    user = api.getUserWS(userId);
        OrderWS latestOrder = api.getLatestOrder(user.getUserId());
        // check out its amount
        logger.debug("The latest order's amount is {}", latestOrder.getTotalAsDecimal());

        // again trigger the billing, this time we should have a different order
        updateBillingConfig(getDate(2, 1, 2008));
        // trigger the billing on 1st AUG 2010
        logger.debug("Again Triggered Billing Process for Date :::{}", getDate(2, 1, 2008));
        logger.debug("Wait 2 Minute for Billing Process to Complete :::");
        api.triggerBilling(getDate(2, 1, 2008));
        // now see the orders

        // get the last 2 first
        Integer ids[] =   api.getLastOrders(user.getUserId(),2);
        // The second last
        latestOrder = api.getOrder(ids[0]);
        logger.debug("The id being checked is >> {}", ids[0]);
        logger.debug("The id being not checked is >> {}", ids[1]);
        logger.debug("The latest order's amount is {}", latestOrder.getTotalAsDecimal());
        // the latest order should have the item containing the above id
        assertEquals("latest order amount should be 1.5 % of 100 = 1.5$", new BigDecimal("1.5000000000"), latestOrder.getTotalAsDecimal());
    }


    /**
     * When an Unpaid, Invoice is paid, any Penalty Order created from OverdueInvoicePenaltyTask
     * must be updated to set an activeUntil date equal to the same value as the Payment Date.
     *
     * This is the test that activeUntil Date of such an order should not be null
     * and also equal to the Payment Date
     * @throws Exception
     */	
      @Test
      public void test002InvoicePayment() throws Exception {
        // create a user for testing
        Integer userId = api.createUser(createUserWS("invoice-penaly-01-" + System.currentTimeMillis(), Constants.PERIOD_UNIT_MONTH, 1));//due date 1 month or 30 days
        assertNotNull("Test fail at user creation.", userId);
        logger.debug("User Created with Id :::{}", userId);
        
        UserWS user = api.getUserWS(userId);
        user.setNextInvoiceDate(getDate(3, 1, 2008));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		logger.debug("User Next Invoice Date :::{}", DATE_FORMAT.format(user.getNextInvoiceDate()));
		
        ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));
	    Integer itemId = createItem(itemCategory.getId());

        logger.debug("Creating order......");
        //set the active since to 1st May 2011
        Integer orderId = createOrderWs(userId, itemId, getDate(02, 01, 2008), PeriodUnitDTO.MONTH);
        OrderWS orderWS = api.getOrder(orderId);
        assertNotNull("order created", orderWS.getId());
        
        // update the billing process
        updateBillingConfig(getDate(3, 1, 2008));
        // now trigger the billing, and the invoice should be made of the above order
        // trigger the billing on 1st JUNE 2011
        logger.debug("Triggered Billing Process for Date :::{}", getDate(3, 1, 2008));
        logger.debug("Wait 2 Minute for Billing Process to Complete :::");
	    api.triggerBilling(getDate(3, 1, 2008));

        // again trigger the billing, this time we should have a different order
        updateBillingConfig(getDate(4, 1, 2008));
        // trigger the billing on 1st JULY 2011
        logger.debug("Triggered Billing Process for Date :::{}", getDate(4, 1, 2008));
        logger.debug("Wait 2 Minute for Billing Process to Complete :::");
        api.triggerBilling(getDate(4, 1, 2008));
        
        // check the order generated
        // get the last 2 first
	    user = api.getUserWS(userId);
        Integer ids[] =   api.getLastOrders(user.getUserId(),2);
        // The second last
        OrderWS latestOrder = api.getOrder(ids[0]);
        logger.debug("The id being checked is >> {}", ids[0]);
        logger.debug("The id being not checked is >> {}", ids[1]);
        logger.debug("The latest order's amount is {}", latestOrder.getTotalAsDecimal());
        // check the status
        logger.debug("The order's status is >>>>>> {}", latestOrder.getOrderStatusWS().getId());
        logger.debug("The order's active until is >>>>>> {}", latestOrder.getActiveUntil());


       // now make a payment for the above invoice, sufficient to pay them all
        PaymentWS payment = createPaymentWS(user.getUserId(), getDate(4, 1, 2008), null);
        
        logger.debug("Created payemnt {}", payment.getId());
        assertNotNull("Didn't get the payment id", payment.getId());

        // get the order again
        pause(1000);
        orderWS = api.getOrder(orderWS.getId());

        logger.debug("The order's status is >>>>>> {}", orderWS.getOrderStatusWS().getId());
        logger.debug("The order's active until is >>>>>> {}", orderWS.getActiveUntil());
    }


     /**
     * A test case that asserts that the Penalty Item Order is created with a Penalty (1.5% in this case)
     * on only the Un-paid amount of the current Invoice and not on any carried balance on that Invoice
     * @throws Exception
     */

   /* @Test
    public void test003PenaltyOnOverdueAmountOnly() throws Exception {
    	
    	System.out.println("## test003PenaltyOnOverdueAmountOnly ##");
        // create a user for testing
        Integer userId = api.createUser(createUserWS("invoice-penaly-01-" + System.currentTimeMillis(), Constants.PERIOD_UNIT_MONTH, 1));//due date 1 month or 30 days
        assertNotNull("Test fail at user creation.", userId);
        
        System.out.println("User Created with Id :::"+ userId);
        
        UserWS user = api.getUserWS(userId);
        user.setNextInvoiceDate(getDate(4, 1, 2012));
		api.updateUser(user);
		
		user = api.getUserWS(userId);
		System.out.println("User Next Invoice Date :::"+ DATE_FORMAT.format(user.getNextInvoiceDate()));
        
        ItemTypeWS itemCategory = buildItemCategory();
	    itemCategory.setId(api.createItemCategory(itemCategory));
	    Integer itemId = createItem(itemCategory.getId());

        System.out.println("Creating order.......");
        // Set active since to 01 APRIL 2012
        Integer orderId = createOrderWs(userId, itemId, getDate(03, 01, 2012), PeriodUnitDTO.MONTH);
        OrderWS orderWS = api.getOrder(orderId);
        assertNotNull("order created", orderWS.getId());
        
        // update the billing process
        updateBillingConfig(getDate(4, 1, 2012));
        // now trigger the billing, and the invoice should be made of the above order
        // trigger the billing on 1st MAY 2012
	    System.out.println("Triggered Billing Process for Date :::"+ getDate(4, 1, 2012));
        System.out.println("Wait 2 Minute for Billing Process to Complete :::");
	    api.triggerBilling(getDate(4, 1, 2012));

        // again trigger the billing, this time we should have a different order
        updateBillingConfig(getDate(5, 1, 2012));
        // trigger the billing on 1st JUNE 2012
        System.out.println("Triggered Billing Process for Date :::"+ getDate(5, 1, 2012));
        System.out.println("Wait 2 Minute for Billing Process to Complete :::");
        api.triggerBilling(getDate(5, 1, 2012));
        
        // Updating Penalty order active since date to get picked in latest billing run.
        Integer ids[] =   api.getLastOrders(user.getUserId(),1);
        OrderWS latestOrder = api.getOrder(ids[0]);
        latestOrder.setActiveSince(getDate(5, 1, 2012));
        api.updateOrder(latestOrder, null);
        
        // again trigger the billing, this time we should have a different order
        updateBillingConfig(getDate(6, 1, 2012));
        // trigger the billing on 1st JULY 2012
        System.out.println("Triggered Billing Process for Date :::"+ getDate(6, 1, 2012));
        System.out.println("Wait 2 Minute for Billing Process to Complete :::");
        api.triggerBilling(getDate(6, 1, 2012));

        // get the latest order and check it would be of $3.02 because 201.50 * 1.5 % = 3.02 (Nothing to do with )
        // get the last 3 first
	    user = api.getUserWS(userId);
        Integer orderIds[] =   api.getLastOrders(user.getUserId(),3);
        // The third last
        latestOrder = api.getOrder(orderIds[0]);
        System.out.println("id of 1st is >> "+orderIds[0]);
        System.out.println("id of 2nd is >> "+orderIds[1]);
        System.out.println("id of 3rd is >> "+orderIds[2]);
        assertNotNull(latestOrder);
        assertEquals("Penalty order Total should be 3.02", new BigDecimal("3.02"),latestOrder.getTotalAsDecimal());
    }*/
    
    /**
     * Build User
     * @param userName
     * @param dueDateUnitId
     * @param dueDateUnitValue
     * @return
     */
    private UserWS createUserWS(String userName, Integer dueDateUnitId, Integer dueDateUnitValue) {
        UserWS newUser = new UserWS();
        newUser.setUserId(0);
        newUser.setUserName(userName);
        newUser.setPassword("Admin123@");
        newUser.setLanguageId(LANGUAGE_ID);
        newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
        newUser.setAccountTypeId(PRANCING_PONY_ACCOUNT_TYPE);
        newUser.setParentId(null);
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(CURRENCY_USD);
        newUser.setDueDateUnitId(dueDateUnitId);
        newUser.setDueDateValue(dueDateUnitValue);

        newUser.setMetaFields(createContactMetaFields(newUser.getUserName()));

        return newUser;
    }
    
    private MetaFieldValueWS[] createContactMetaFields(String username){
        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue(username + "@gmail.com");
        metaField1.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Test");
        metaField2.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue("Plugin");
        metaField3.setGroupId(PRANCING_PONY_ACCOUNT_TYPE);

        return new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
        };
    }
    
    private ItemTypeWS buildPenaltyCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Invoice Penalty Items:" + System.currentTimeMillis());
		type.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_PENALTY);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}
    
    private ItemTypeWS buildItemCategory() {
		ItemTypeWS type = new ItemTypeWS();
		type.setDescription("Penalty Test Item Cat:" + System.currentTimeMillis());
		type.setOrderLineTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		type.setAllowAssetManagement(0);//does not manage assets
		type.setOnePerCustomer(false);
		type.setOnePerOrder(false);
		return type;
	}
    
    private Integer createItem(Integer categoryId) {
		ItemDTOEx item = new ItemDTOEx();
		item.setCurrencyId(CURRENCY_USD);
		item.setPrice(new BigDecimal("5.0"));
		item.setDescription("ITEM-PENALTY");
		item.setEntityId(TEST_ENTITY_ID);
		item.setNumber("INVOICE-PEN-ITEM");
		item.setTypes(new Integer[]{categoryId});
		item.setId(api.createItem(item));
		return item.getId();
	}
    
    private Integer createPenaltyItem(Integer categoryId, String priceModelType, BigDecimal rate) {
		ItemDTOEx item = new ItemDTOEx();
		item.setCurrencyId(CURRENCY_USD);

		PriceModelWS linePercentagePrice = new PriceModelWS();
        linePercentagePrice.setType(PriceModelStrategy.FLAT.name());
        linePercentagePrice.setRate(new BigDecimal("1.50"));
        linePercentagePrice.setCurrencyId(CURRENCY_USD);
        item.addDefaultPrice(CommonConstants.EPOCH_DATE, linePercentagePrice);
		
		item.setDescription("Late Fees");
		item.setEntityId(TEST_ENTITY_ID);
		item.setNumber("OIPLF");// Overdue Invoice Penalty Late Fee
		item.setTypes(new Integer[]{categoryId});
		return api.createItem(item);
	}
    
    private Integer createOrderWs(Integer userId, Integer itemId, Date activeSince, Integer periodUnitId) {

        OrderWS order = new OrderWS();
        order.setUserId(userId);
        order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
        order.setPeriod(ORDER_PERIOD_MONTHLY); // Monthly
        order.setCurrencyId(CURRENCY_USD);
        order.setActiveSince(activeSince);
        order.setOrderLines(new OrderLineWS[] { createOrderLineWS(itemId) });
        order.setDueDateUnitId(periodUnitId);
        order.setDueDateValue(0);//order due
        order.setProrateFlag(true);
        
        OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		for (OrderChangeWS ws : orderChanges) {
                ws.setStartDate(activeSince);
			}
		Integer orderId =  api.createOrder(order, orderChanges);
		assertNotNull("orderId should not be null",orderId);
		return orderId;
    }
    
    private OrderLineWS createOrderLineWS(Integer itemId) {
        OrderLineWS line = new OrderLineWS();
        line.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
        line.setDescription("Order line");
        line.setItemId(itemId);
        line.setQuantity(1);
	    line.setUseItem(Boolean.FALSE);
        line.setPrice(new BigDecimal("50"));
        line.setAmount(new BigDecimal("50"));
        return line;
    }

    /*
    * Enable/disable the OverdueInvoicePenaltyTask plug-in.
    */
    private Integer enableOverdueInvoicePenaltyPlugin(Integer itemId, Integer chargeItemId) {
		PluggableTaskWS plugin = new PluggableTaskWS();
		plugin.setTypeId(OVERDUE_INVOICE_PENALTY_TASK_TYPE_ID);
		plugin.setProcessingOrder(161);
		
		// plug-in adds the given penalty fee item to the order
		Hashtable<String, String> parameters = new Hashtable<String, String>();
		parameters.put(PLUGIN_PARAMETER_ITEM, itemId.toString());
		parameters.put(PLUGIN_PARAMETER_CHARGE_ITEM, chargeItemId.toString());
		plugin.setParameters(parameters);

		return api.createPlugin(plugin);
	}
    
    private void updateBillingConfig(Date runDate) {
        BillingProcessConfigurationWS config = api.getBillingProcessConfiguration();

        config.setNextRunDate(runDate);
        config.setRetries(0);
        config.setDaysForRetry(5);
        config.setGenerateReport(0);     //review report true
        config.setAutoPaymentApplication(0);
        config.setDfFm(0);
        config.setPeriodUnitId(new Integer(Constants.PERIOD_UNIT_MONTH));
        config.setDueDateUnitId(Constants.PERIOD_UNIT_MONTH);
        config.setDueDateValue(1);
        config.setInvoiceDateProcess(0);
        config.setMaximumPeriods(99);
        config.setOnlyRecurring(new Integer(0));
        config.setProratingType(ProratingType.PRORATING_AUTO_ON.toString());
        
        logger.debug("Updating billing run date to : {}", runDate);
        api.createUpdateBillingProcessConfiguration(config);
	}
    
    private PaymentWS createPaymentWS(Integer userId, Date date, String note) throws Exception{
		JbillingAPI api = JbillingAPIFactory.getAPI();

		PaymentWS payment = new PaymentWS();
		payment.setAmount(new BigDecimal("100.00"));
		payment.setIsRefund(new Integer(0));
		payment.setMethodId(Constants.PAYMENT_METHOD_CHEQUE);
		payment.setPaymentDate(date);
		payment.setCreateDatetime(date);
		payment.setResultId(Constants.RESULT_ENTERED);
		payment.setCurrencyId(new Integer(1));
		payment.setUserId(userId);
		payment.setPaymentNotes(note);
		payment.setPaymentPeriod(new Integer(1));

		PaymentInformationWS cheque = com.sapienter.jbilling.server.user.WSTest.
				createCheque("ws bank", "2232-2323-2323", date);

		payment.getPaymentInstruments().add(cheque);
		logger.debug("Applying payment");
		Integer ret = api.applyPayment(payment, new Integer(35));
		logger.debug("Created payemnt {}", ret);
		assertNotNull("Didn't get the payment id", ret);

		payment.setId(ret);
		return payment;
	}

    private void pause(long t) {
        logger.debug("pausing for " + t + " ms...");
        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
        }
    }
    
    /**
	 * 
	 * @param day
	 * @param month
	 * @param year
	 * @return
	 */
	 public static Date getDate(int month, int day, int year) {
		
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.DAY_OF_MONTH,day);
		cal.set(Calendar.YEAR, year);
		
		return cal.getTime();
	}
	 
	@AfterClass
	private void cleanUp() {
		api.deletePlugin(penaltyPluginId);
	}

}
