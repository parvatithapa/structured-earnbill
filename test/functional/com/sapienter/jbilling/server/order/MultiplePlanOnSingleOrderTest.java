package com.sapienter.jbilling.server.order;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.process.db.PeriodUnitDTO;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
/**
 * @author Harshad Pathan
 */
@Test(groups = { "web-services", "order" }, testName = "MultiplePlanOnSingleOrderTest")
public class MultiplePlanOnSingleOrderTest {

	private static final Logger logger = LoggerFactory.getLogger(MultiplePlanOnSingleOrderTest.class);
	public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	JbillingAPI api;
	private	Integer orderId = null;
	private Integer userId = null;
	List<PlanWS> plans;
	private static Integer PRANCING_PONY_ACCOUNT_TYPE_ID = Integer.valueOf(1);
	private static final int CC_PM_ID = 1;
	private static final String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private static final String CC_MF_NUMBER = "cc.number";
	private static final String CC_MF_TYPE = "cc.type";
	private static final String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private static Integer ORDER_PERIOD_MONTHLY;
	OrderWS order;
	UserWS user  ;
	@org.testng.annotations.BeforeClass
	protected void setUp() throws Exception {
		
		api = JbillingAPIFactory.getAPI();
		PlanWS planOne = api.getPlanWS(7); 
		PlanWS planTwo = api.getPlanWS(101);
		ORDER_PERIOD_MONTHLY = getOrCreateMonthlyOrderPeriod(api);
		user = createUser(true, null, Constants.PRIMARY_CURRENCY_ID, true, api, PRANCING_PONY_ACCOUNT_TYPE_ID);
		plans = new ArrayList<PlanWS>();
		plans.add(planOne);
		plans.add(planTwo);
	}

	@Test
	public void test001CreateOrderWithMultiplePlan() {
		try {
			user = api.getUserWS(user.getId());
			assertNotNull("user should not be null",user);
			logger.debug("userId :: {}", user.getId());
			try {
				orderId = createOrder(plans,user.getUserId(),getDate(11, 15, 2015),null);
			} catch (SessionInternalError e) {
				assertEquals("validation.order.should.not.contain.multiple.plans", e.getErrorMessages()[0]);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		} 
	}
	
	@Test
	public void test002updateOrderWithMultiplePlan() {
		try {
			user = api.getUserWS(user.getId());
			userId = user.getId();
			logger.debug("userId :: {}", userId);
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);
					
			orderId = createOrder(plans.get(0),user.getUserId(),getDate(11, 15, 2015),null);
			order =  api.getOrder(orderId);
			logger.debug("order Id :: {}", order.getId());
			
			OrderLineWS line1 = new OrderLineWS();
			line1.setItemId(plans.get(1).getItemId());
			line1.setAmount("225.00");
			line1.setPrice("225.00");
			line1.setTypeId(Integer.valueOf(1));
			line1.setDescription(plans.get(1).getDescription());
			line1.setQuantity("1");
			line1.setUseItem(true);
			List<OrderLineWS> lines = new ArrayList<OrderLineWS>();
			for(OrderLineWS line: order.getOrderLines()) {
				lines.add(line);
			}
			lines.add(line1);
			order.setOrderLines(lines.toArray(new OrderLineWS[0]));
			OrderChangeWS orderChanges = OrderChangeBL.buildFromLine(line1, order, ORDER_CHANGE_STATUS_APPLY_ID);
		
			try {
				api.updateOrder(order, new OrderChangeWS[]{orderChanges});
			} catch (SessionInternalError e) {
				assertEquals("validation.order.should.not.contain.multiple.plans", e.getErrorMessages()[0]);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		} 
		OrderWS[] orderWSs = order.getChildOrders();
		for (OrderWS orderWS : orderWSs) {
			api.deleteOrder(orderWS.getId());
		}
		api.deleteOrder(order.getId());
	}
	
	@Test
	public void test003createUpdateOrderWithMultiplePlan() {
		try {
			user = api.getUserWS(user.getId());
			userId = user.getId();
			logger.debug("userId :: {}", userId);
			assertNotNull("user should not be null",user);
			user = api.getUserWS(userId);
					
			orderId = createOrder(plans.get(0),user.getUserId(),getDate(11, 15, 2015),null);
			order =  api.getOrder(orderId);
			logger.debug("order Id :: {}", order.getId());
			
			OrderLineWS line1 = new OrderLineWS();
			line1.setItemId(plans.get(1).getItemId());
			line1.setAmount("225.00");
			line1.setPrice("225.00");
			line1.setTypeId(Integer.valueOf(1));
			line1.setDescription(plans.get(1).getDescription());
			line1.setQuantity("1");
			line1.setUseItem(true);
			List<OrderLineWS> lines = new ArrayList<OrderLineWS>();
			for(OrderLineWS line: order.getOrderLines()) {
				lines.add(line);
			}
			lines.add(line1);
			order.setOrderLines(lines.toArray(new OrderLineWS[0]));
			OrderChangeWS orderChanges = OrderChangeBL.buildFromLine(line1, order, ORDER_CHANGE_STATUS_APPLY_ID);
		
			try {
				api.createUpdateOrder(order, new OrderChangeWS[]{orderChanges});
			} catch (SessionInternalError e) {
				assertEquals("validation.order.should.not.contain.multiple.plans", e.getErrorMessages()[0]);
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		} 
		OrderWS[] orderWSs = order.getChildOrders();
		for (OrderWS orderWS : orderWSs) {
			api.deleteOrder(orderWS.getId());
		}
		api.deleteOrder(order.getId());
	}
	
	private Integer createOrder(List<PlanWS> plans, Integer userId, Date activeSinceDate, Date activeUntilDate)
			 throws JbillingAPIException, IOException {

		logger.debug("##Creating subscription order...");
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setActiveSince(activeSinceDate);
		order.setActiveUntil(activeUntilDate);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(new Integer(2)); // monthly
		order.setCurrencyId(new Integer(1));
		order.setProrateFlag(true);
		
		OrderLineWS line = new OrderLineWS();
		line.setItemId(plans.get(0).getItemId());
		line.setAmount("225.00");
		line.setPrice("225.00");
		line.setTypeId(Integer.valueOf(1));
		line.setDescription(plans.get(0).getDescription());
		line.setQuantity("1");
		line.setUseItem(true);
		
		OrderLineWS line1 = new OrderLineWS();
		line1.setItemId(plans.get(1).getItemId());
		line1.setAmount("225.00");
		line1.setPrice("225.00");
		line1.setTypeId(Integer.valueOf(1));
		line1.setDescription(plans.get(1).getDescription());
		line1.setQuantity("1");
		line1.setUseItem(true);
		
		order.setOrderLines(new OrderLineWS[]{line,line1});
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		
		Integer orderId =  api.createOrder(order, orderChanges);
		return orderId;
		 
	 }
	
	private Integer createOrder(PlanWS planWS, Integer userId, Date activeSinceDate, Date activeUntilDate)
			 throws JbillingAPIException, IOException {
		
		logger.debug("##Creating subscription order...");
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setActiveSince(activeSinceDate);
		order.setActiveUntil(activeUntilDate);
		order.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		order.setPeriod(new Integer(2)); // monthly
		order.setCurrencyId(new Integer(1));
		order.setProrateFlag(true);
		
		OrderLineWS line = new OrderLineWS();
		line.setItemId(planWS.getItemId());
		line.setAmount("225.00");
		line.setPrice("225.00");
		line.setTypeId(Integer.valueOf(1));
		line.setDescription(planWS.getDescription());
		line.setQuantity("1");
		line.setUseItem(true);
		
		order.setOrderLines(new OrderLineWS[]{line});
		OrderChangeWS orderChanges[] = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		
		Integer orderId =  api.createOrder(order, orderChanges);
		return orderId;
		 
	 }
	
	private UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate, JbillingAPI api, Integer accountTypeId) {

        // Create - This passes the password validation routine.

        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("P@ssword1");
        newUser.setAccountTypeId(accountTypeId);
        newUser.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        newUser.setMainRoleId(Constants.TYPE_CUSTOMER);
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setInvoiceChild(Boolean.FALSE);
        newUser.setMainSubscription(new MainSubscriptionWS(ORDER_PERIOD_MONTHLY, 1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("contact.email");
        metaField1.setValue("test" + System.currentTimeMillis() + "@test.com");
        metaField1.setGroupId(accountTypeId);

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("contact.first.name");
        metaField2.setValue("Pricing Test");
        metaField2.setGroupId(accountTypeId);

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.last.name");
        metaField3.setValue(newUser.getUserName());
        metaField3.setGroupId(accountTypeId);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("ccf.payment_processor");
		metaField4.setValue("FAKE_2"); // the plug-in parameter of the processor

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4
        });

        logger.debug("Creating credit card");
		String ccName = "Frodo Baggins";
		String ccNumber = "4111111111111152";
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS newCC = createCreditCard(ccName, ccNumber,
				expiry.getTime());
		newUser.getPaymentInstruments().add(newCC);

        if (doCreate) {
            logger.debug("Creating user ...");
            newUser = api.getUserWS(api.createUser(newUser));
        }

        return newUser;
    }
	
	private  PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_GATEWAY_KEY);

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true,
				DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2,
				cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
                DataType.CHAR, 3, (DateTimeFormat.forPattern(
                        Constants.CC_DATE_FORMAT).print(date.getTime())).toCharArray());
		// have to pass meta field card type for it to be set
		addMetaField(metaFields, CC_MF_TYPE, true, false,
				DataType.STRING, 4, CreditCardType.VISA);
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields.size()]));

		return cc;
	}
	
	 private void addMetaField(List<MetaFieldValueWS> metaFields,
				String fieldName, boolean disabled, boolean mandatory,
				DataType dataType, Integer displayOrder, Object value) {
			MetaFieldValueWS ws = new MetaFieldValueWS();
			ws.setFieldName(fieldName);
			ws.getMetaField().setDisabled(disabled);
			ws.getMetaField().setMandatory(mandatory);
			ws.getMetaField().setDataType(dataType);
			ws.getMetaField().setDisplayOrder(displayOrder);
			ws.setValue(value);
			metaFields.add(ws);
		}
	 
	 private Integer getOrCreateMonthlyOrderPeriod(JbillingAPI api){
	        OrderPeriodWS[] periods = api.getOrderPeriods();
	        for(OrderPeriodWS period : periods){
	            if(1 == period.getValue() &&
	                    PeriodUnitDTO.MONTH == period.getPeriodUnitId()){
	                return period.getId();
	            }
	        }
	        //there is no monthly order period so create one
	        OrderPeriodWS monthly = new OrderPeriodWS();
	        monthly.setEntityId(api.getCallerCompanyId());
	        monthly.setPeriodUnitId(PeriodUnitDTO.MONTH);//monthly
	        monthly.setValue(1);
	        monthly.setDescriptions(Arrays.asList(new InternationalDescriptionWS(Constants.LANGUAGE_ENGLISH_ID, "ORD:MONTHLY")));
	        return api.createOrderPeriod(monthly);
	    }
	 
		/**
		 * 
		 * @param day
		 * @param month
		 * @param year
		 * @return
		 */
		 private Date getDate(int month, int day, int year) {
			
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MONTH, month);
			cal.set(Calendar.DAY_OF_MONTH,day);
			cal.set(Calendar.YEAR, year);
			
			return cal.getTime();
		}
	}
