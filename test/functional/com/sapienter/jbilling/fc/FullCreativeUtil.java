package com.sapienter.jbilling.fc;


import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.server.TestConstants;
import com.sapienter.jbilling.server.item.AssetSearchResult;
import com.sapienter.jbilling.server.item.AssetWS;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationRatingSchemeWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.OrderChangeBL;
import com.sapienter.jbilling.server.order.OrderChangePlanItemWS;
import com.sapienter.jbilling.server.order.OrderChangeWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskTypeWS;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.usagePool.CustomerUsagePoolWS;
import com.sapienter.jbilling.server.usagePool.UsagePoolWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;
import com.sapienter.jbilling.server.util.search.BasicFilter;
import com.sapienter.jbilling.server.util.search.Filter.FilterConstraint;
import com.sapienter.jbilling.server.util.search.SearchCriteria;
import com.sapienter.jbilling.server.order.OrderLineUsagePoolWS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FullCreativeUtil {

	private static final Logger logger = LoggerFactory.getLogger(FullCreativeUtil.class);
	private static final String File_Name = "fullcreative-test.properties";
	
	private static JbillingAPI api = null;
	private final static int CC_PM_ID = 1;
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
	public final static int ORDER_PERIOD = 2;
	public final static Integer ACCOUNT_TYPE = 60103;
	public static final int ORDER_PERIOD_MONTHLY = 2;
	public final static Integer ENTITY_ID = Integer.valueOf(1);
	public static final int ORDER_CHANGE_STATUS_APPLY_ID = 3;
	public static final int ITEM_ID = 230303;
	public static final int TYPE_ID = 230304;
	public static Properties properties;
	public static int product8XXTollFreeId;
	
	public static int inboundProductId;
	public static int chatProductId;
	public static int activeResponceProductId;
	public static int setupFeeProductId;

	public static OrderWS createMockOrder(int userId, int orderPeriod,
			Date activeSince, int billingType) throws Exception {
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setBillingTypeId(billingType);
		order.setPeriod(orderPeriod);
		order.setCurrencyId(1);
		order.setActiveSince(activeSince);
		api = JbillingAPIFactory.getAPI();
		MetaFieldWS[] meta = api.getMetaFieldsForEntity(EntityType.ORDER_LINE
				.name());
		MetaFieldWS inbound = null;
		for(MetaFieldWS metaFieldWS: meta){
			if(metaFieldWS.getName().equals(Constants.PHONE_META_FIELD)){
				inbound = metaFieldWS;
				break;
			}
		}
		OrderLineWS lines[] = new OrderLineWS[1];

		ItemDTOEx item = new ItemDTOEx();
		item.setNumber(String.valueOf(new Date().getTime()));
		item.setDescription("Phone PRODUCT" + new Date().getTime());
		item.setCurrencyId(new Integer(1));

		item.setPrice(new BigDecimal("29.1"));
		item.setPriceModelCompanyId(new Integer(api.getCompany().getId()));
		item.setEntityId(api.getCompany().getId());
		item.setPrice(new BigDecimal("10.0"));
		Integer types[] = new Integer[1];
		types[0] = new Integer(TYPE_ID);
		item.setTypes(types);
		MetaFieldValueWS phoneNumber = new MetaFieldValueWS();
		phoneNumber.setFieldName(inbound.getName());
		phoneNumber.setStringValue("1-800-46789");
		phoneNumber.getMetaField().setDataType(inbound.getDataType());

		item.setOrderLineMetaFields(meta);
		Integer itemId = api.createItem(item);
		logger.debug("Item Created : {}", itemId);

		OrderLineWS nextLine = new OrderLineWS();
		nextLine.setTypeId(Constants.ORDER_LINE_TYPE_ITEM);
		nextLine.setDescription("Order line: 1");
		nextLine.setItemId(itemId);
		nextLine.setQuantity(new Integer(1));
		nextLine.setPrice(new BigDecimal("10.00"));
		nextLine.setAmount(new BigDecimal("10.00"));

		nextLine.setMetaFields(new MetaFieldValueWS[] { phoneNumber });

		lines[0] = nextLine;
		order.setOrderLines(lines);
		return order;
	}
	
	public static UserWS createUser(String userName) throws JbillingAPIException, IOException {
		
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = new UserWS();
		newUser.setUserId(0);
		newUser.setUserName(userName
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(ACCOUNT_TYPE);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));
		
		logger.debug("User properties set");
		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("Country");
		metaField1.getMetaField().setDataType(DataType.STRING);
		metaField1.setValue("CA");
		
		MetaFieldValueWS metaField2 = new MetaFieldValueWS();
		metaField2.setFieldName("State/Province");
		metaField2.getMetaField().setDataType(DataType.STRING);
		metaField2.setValue("OR");
		
		
		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("First Name");
		metaField3.getMetaField().setDataType(DataType.STRING);
		metaField3.setValue("Frodo");
		
		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("Last Name");
		metaField4.getMetaField().setDataType(DataType.STRING);
		metaField4.setValue("Baggins");
		
		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("Address 1");
		metaField5.setValue("Baggins");
		metaField5.getMetaField().setDataType(DataType.STRING);
		newUser.setMetaFields(new MetaFieldValueWS[] {  
				  metaField5 });
		MetaFieldValueWS metaField6 = new MetaFieldValueWS();
		metaField6.setFieldName("City");
		metaField6.setValue("Baggins");
		metaField6.getMetaField().setDataType(DataType.STRING);
		
		MetaFieldValueWS metaField7 = new MetaFieldValueWS();
		metaField7.setFieldName("Email Address");
		metaField7.setValue(newUser.getUserName() + "@shire.com");
		metaField7.getMetaField().setDataType(DataType.STRING);
		
		MetaFieldValueWS metaField8 = new MetaFieldValueWS();
		metaField8.setFieldName("Postal Code");
		metaField8.setValue("K0");
		metaField8.getMetaField().setDataType(DataType.STRING);
		
		MetaFieldValueWS metaField9 = new MetaFieldValueWS();
		metaField9.setFieldName("COUNTRY_CODE");
		metaField9.getMetaField().setDataType(DataType.STRING);
		metaField9.setValue("CA");
		
		MetaFieldValueWS metaField10 = new MetaFieldValueWS();
		metaField10.setFieldName("STATE_PROVINCE");
		metaField10.getMetaField().setDataType(DataType.STRING);
		metaField10.setValue("OR");
		
		
		MetaFieldValueWS metaField11 = new MetaFieldValueWS();
		metaField11.setFieldName("ORGANIZATION");
		metaField11.getMetaField().setDataType(DataType.STRING);
		metaField11.setValue("Frodo");
		
		MetaFieldValueWS metaField12 = new MetaFieldValueWS();
		metaField12.setFieldName("LAST_NAME");
		metaField12.getMetaField().setDataType(DataType.STRING);
		metaField12.setValue("Baggins");
		
		MetaFieldValueWS metaField13 = new MetaFieldValueWS();
		metaField13.setFieldName("ADDRESS1");
		metaField13.getMetaField().setDataType(DataType.STRING);
		metaField13.setValue("Baggins");
		
		MetaFieldValueWS metaField14 = new MetaFieldValueWS();
		metaField14.setFieldName("CITY");
		metaField14.getMetaField().setDataType(DataType.STRING);
		metaField14.setValue("Baggins");
		
		MetaFieldValueWS metaField15 = new MetaFieldValueWS();
		metaField15.setFieldName("BILLING_EMAIL");
		metaField15.getMetaField().setDataType(DataType.STRING);
		metaField15.setValue(newUser.getUserName() + "@shire.com");
		
		MetaFieldValueWS metaField16 = new MetaFieldValueWS();
		metaField16.setFieldName("POSTAL_CODE");
		metaField16.getMetaField().setDataType(DataType.STRING);
		metaField16.setValue("K0");
		
		newUser.setMetaFields(new MetaFieldValueWS[] {  metaField1, metaField2, metaField3, metaField4, metaField5, metaField6, metaField7, metaField8,
		        metaField9, metaField10, metaField11, metaField12, metaField13, metaField14, metaField15, metaField16 });
		
		logger.debug("Meta field values set");
		
		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
		
		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				"4111111111111152", expiry.getTime());
		
		newUser.getPaymentInstruments().add(cc);
		
		logger.debug("Creating user ...");
		MainSubscriptionWS billing = new MainSubscriptionWS();
		billing.setPeriodId(ORDER_PERIOD);
		billing.setNextInvoiceDayOfPeriod(1);
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(new Date());
		newUser.setUserId(api.createUser(newUser));
		logger.debug("User created with id : {}", newUser.getUserId());
		
		return newUser;
	}
	
	public static UserWS createUser(Date nextInvoiceDate) throws JbillingAPIException,
			IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		UserWS newUser = new UserWS();
		newUser.setUserId(0);
		newUser.setUserName("testUserName-"
				+ Calendar.getInstance().getTimeInMillis());
		newUser.setPassword("P@ssword12");
		newUser.setLanguageId(Integer.valueOf(1));
		newUser.setMainRoleId(Integer.valueOf(5));
		newUser.setAccountTypeId(ACCOUNT_TYPE);
		newUser.setParentId(null);
		newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
		newUser.setCurrencyId(null);
		newUser.setBalanceType(Constants.BALANCE_NO_DYNAMIC);
		newUser.setInvoiceChild(new Boolean(false));

		logger.debug("User properties set");
		MetaFieldValueWS metaField1 = new MetaFieldValueWS();
		metaField1.setFieldName("Country");
		metaField1.getMetaField().setDataType(DataType.STRING);
		metaField1.setValue("CA");

		MetaFieldValueWS metaField2 = new MetaFieldValueWS();
		metaField2.setFieldName("State/Province");
		metaField2.getMetaField().setDataType(DataType.STRING);
		metaField2.setValue("OR");


		MetaFieldValueWS metaField3 = new MetaFieldValueWS();
		metaField3.setFieldName("First Name");
		metaField3.getMetaField().setDataType(DataType.STRING);
		metaField3.setValue("Frodo");

		MetaFieldValueWS metaField4 = new MetaFieldValueWS();
		metaField4.setFieldName("Last Name");
		metaField4.getMetaField().setDataType(DataType.STRING);
		metaField4.setValue("Baggins");

		MetaFieldValueWS metaField5 = new MetaFieldValueWS();
		metaField5.setFieldName("Address 1");
		metaField5.getMetaField().setDataType(DataType.STRING);
		metaField5.setValue("Baggins");
		newUser.setMetaFields(new MetaFieldValueWS[] {  
				  metaField5 });
		MetaFieldValueWS metaField6 = new MetaFieldValueWS();
		metaField6.setFieldName("City");
		metaField6.getMetaField().setDataType(DataType.STRING);
		metaField6.setValue("Baggins");
		
		MetaFieldValueWS metaField7 = new MetaFieldValueWS();
		metaField7.setFieldName("Email Address");
		metaField7.getMetaField().setDataType(DataType.STRING);
		metaField7.setValue(newUser.getUserName() + "@shire.com");
		
		MetaFieldValueWS metaField8 = new MetaFieldValueWS();
		metaField8.setFieldName("Postal Code");
		metaField8.getMetaField().setDataType(DataType.STRING);
		metaField8.setValue("K0");

        MetaFieldValueWS metaField9 = new MetaFieldValueWS();
		metaField9.setFieldName("COUNTRY_CODE");
		metaField9.getMetaField().setDataType(DataType.STRING);
        metaField9.setValue("CA");
        metaField9.setGroupId(14);

        MetaFieldValueWS metaField10 = new MetaFieldValueWS();
		metaField10.setFieldName("STATE_PROVINCE");
		metaField10.getMetaField().setDataType(DataType.STRING);
        metaField10.setValue("OR");
        metaField10.setGroupId(14);

        MetaFieldValueWS metaField11 = new MetaFieldValueWS();
		metaField11.setFieldName("ORGANIZATION");
		metaField11.getMetaField().setDataType(DataType.STRING);
        metaField11.setValue("Frodo");
        metaField11.setGroupId(14);

        MetaFieldValueWS metaField12 = new MetaFieldValueWS();
		metaField12.setFieldName("LAST_NAME");
		metaField12.getMetaField().setDataType(DataType.STRING);
        metaField12.setValue("Baggins");
        metaField12.setGroupId(14);

        MetaFieldValueWS metaField13 = new MetaFieldValueWS();
		metaField13.setFieldName("ADDRESS1");
		metaField13.getMetaField().setDataType(DataType.STRING);
        metaField13.setValue("Baggins");
        metaField13.setGroupId(14);

        MetaFieldValueWS metaField14 = new MetaFieldValueWS();
		metaField14.setFieldName("CITY");
		metaField14.getMetaField().setDataType(DataType.STRING);
        metaField14.setValue("Baggins");
        metaField14.setGroupId(14);

        MetaFieldValueWS metaField15 = new MetaFieldValueWS();
		metaField15.setFieldName("BILLING_EMAIL");
		metaField15.getMetaField().setDataType(DataType.STRING);
        metaField15.setValue(newUser.getUserName() + "@shire.com");
        metaField15.setGroupId(14);
        
        MetaFieldValueWS metaField16 = new MetaFieldValueWS();
		metaField16.setFieldName("POSTAL_CODE");
		metaField16.getMetaField().setDataType(DataType.STRING);
        metaField16.setValue("K0");
        metaField16.setGroupId(14);

        newUser.setMetaFields(new MetaFieldValueWS[] {  metaField1, metaField2, metaField3, metaField4, metaField5, metaField6, metaField7, metaField8,
                metaField9, metaField10, metaField11, metaField12, metaField13, metaField14, metaField15, metaField16 });

		logger.debug("Meta field values set");

		// add a credit card
		Calendar expiry = Calendar.getInstance();
		expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);

		PaymentInformationWS cc = createCreditCard("Frodo Baggins",
				"5257279846844529", expiry.getTime());

		newUser.getPaymentInstruments().add(cc);

		logger.debug("Creating user ...");
		MainSubscriptionWS billing = new MainSubscriptionWS();
		billing.setPeriodId(ORDER_PERIOD);
		billing.setNextInvoiceDayOfPeriod(1);
		newUser.setMainSubscription(billing);
		newUser.setNextInvoiceDate(nextInvoiceDate);
		newUser.setUserId(api.createUser(newUser));
		logger.debug("User created with id : {}", newUser.getUserId());

		return newUser;
	}

	public static void triggerBilling(Date runDate) {
		BillingProcessConfigurationWS config = api
				.getBillingProcessConfiguration();

		config.setNextRunDate(runDate);
		config.setRetries(new Integer(1));
		config.setDaysForRetry(new Integer(5));
		config.setGenerateReport(new Integer(0));
		config.setAutoPaymentApplication(new Integer(0));
		config.setDfFm(new Integer(0));
		config.setDueDateUnitId(Constants.PERIOD_UNIT_DAY);
		config.setDueDateValue(new Integer(0));
		config.setInvoiceDateProcess(new Integer(0));
		config.setMaximumPeriods(new Integer(99));
		config.setOnlyRecurring(new Integer(0));

		logger.debug("B - Setting config to: {}", config);
		api.createUpdateBillingProcessConfiguration(config);

		logger.debug("Running Billing Process for {}", runDate);
		api.triggerBilling(runDate);
	}

	public static PaymentInformationWS createCreditCard(String cardHolderName,

			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);

		List<MetaFieldValueWS> metaFields = new ArrayList<MetaFieldValueWS>(5);
		addMetaField(metaFields, CC_MF_CARDHOLDER_NAME, false, true, DataType.CHAR, 1, cardHolderName.toCharArray());
		addMetaField(metaFields, CC_MF_NUMBER, false, true, DataType.CHAR, 2, cardNumber.toCharArray());
		addMetaField(metaFields, CC_MF_EXPIRY_DATE, false, true,
				DataType.CHAR, 3, new SimpleDateFormat(Constants.CC_DATE_FORMAT).format(date).toCharArray());
		addMetaField(metaFields, CC_MF_TYPE, true, false, DataType.STRING, 4,
				CreditCardType.VISA);
		cc.setMetaFields(metaFields.toArray(new MetaFieldValueWS[metaFields
				.size()]));

		return cc;
	}

	private static void addMetaField(List<MetaFieldValueWS> metaFields,
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
	
	public static Properties loadProperties() {
		
		Properties properties = new Properties(); 
		try {
			properties.load(FullCreativeUtil.class.getClassLoader().getResourceAsStream(File_Name));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return properties;
	}
	
	 public static Integer createOrder(PlanWS planWS, Integer userId, Date activeSinceDate, Date activeUntilDate)
			 throws JbillingAPIException, IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();
	 	product8XXTollFreeId = TestConstants.PRODUCT_8XX_TOLL_FREE_ID;
	 	
		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(3);
		criteria.setOffset(3);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});
    	
    	// get an available asset's id for plan subscription item (id = 320104)
		AssetSearchResult assetsResult320104 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);
		
		assertNotNull("No available asset found for product 320104", assetsResult320104);
		AssetWS[] available320104Assets = assetsResult320104.getObjects();
		assertTrue("No assets found for product 320104.", null != available320104Assets && available320104Assets.length != 0);
		Integer assetIdProduct320104_1 = available320104Assets[0].getId();
		logger.debug("Asset Available for product id 320104 = {}", assetIdProduct320104_1);
		Integer assetIdProduct320104_2 = available320104Assets[1].getId();
		logger.debug("Asset Available for product id 320104 = {}", assetIdProduct320104_2);
	 
		logger.debug("Creating subscription order...");
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
		for (OrderChangeWS ws : orderChanges) {
			if (ws.getItemId().intValue() == Integer.valueOf(planWS.getItemId())) {
				
				OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
                orderChangePlanItem.setItemId(product8XXTollFreeId);
                orderChangePlanItem.setId(0);
                orderChangePlanItem.setOptlock(0);
                orderChangePlanItem.setBundledQuantity(1);
                orderChangePlanItem.setDescription("DID-8XX");
                orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);
                
                orderChangePlanItem.setAssetIds(new int[]{assetIdProduct320104_2});
				
                ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
                ws.setStartDate(activeSinceDate);
			}
		}
		
		Integer orderId =  api.createOrder(order, orderChanges);
		assertNotNull("orderId should not be null",orderId);
		
		return orderId;
		 
	 }
	 
	 public static Integer createOneTimeOrder(Integer userId, Date activeSinceDate, String inboundProductQuantity,
			 String chatProductQuantity, String activeResposeProductQuantity) throws JbillingAPIException, IOException {
		 
		JbillingAPI api = JbillingAPIFactory.getAPI();
                inboundProductId = TestConstants.INBOUND_USAGE_PRODUCT_ID;
                chatProductId = TestConstants.CHAT_USAGE_PRODUCT_ID;
                activeResponceProductId = TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID;
		
		logger.debug("Creating One time usage order...");
		OrderWS oTOrder = new OrderWS();
		oTOrder.setUserId(userId);
		oTOrder.setActiveSince(activeSinceDate);
		oTOrder.setBillingTypeId(Constants.ORDER_BILLING_POST_PAID);
		oTOrder.setPeriod(Integer.valueOf(1)); // Onetime
		oTOrder.setCurrencyId(Integer.valueOf(1));
		oTOrder.setIsMediated(true);
		
		OrderLineWS oTline1 = new OrderLineWS();
		oTline1.setItemId(Integer.valueOf(inboundProductId));
		oTline1.setDescription("Inbound");
		oTline1.setQuantity(inboundProductQuantity);
		oTline1.setTypeId(Integer.valueOf(1));
		oTline1.setPrice("0.00");
		oTline1.setAmount("0.00");
		oTline1.setUseItem(true);
		
		OrderLineWS oTline2 = new OrderLineWS();
		oTline2.setItemId(Integer.valueOf(chatProductId));
		oTline2.setDescription("Chat");
		oTline2.setQuantity(chatProductQuantity);
		oTline2.setTypeId(Integer.valueOf(1));
		oTline2.setPrice("0.00");
		oTline2.setAmount("0.00");
		oTline2.setUseItem(true);
		
		OrderLineWS oTline3 = new OrderLineWS();
		oTline3.setItemId(Integer.valueOf(activeResponceProductId));
		oTline3.setDescription("Active Response");
		oTline3.setQuantity(activeResposeProductQuantity);
		oTline3.setTypeId(Integer.valueOf(1));
		oTline3.setPrice("0.00");
		oTline3.setAmount("0.00");
		oTline3.setUseItem(true);
		
		oTOrder.setOrderLines(new OrderLineWS[]{oTline1, oTline2, oTline3});
		
		Integer oneTimeOrderId = api.createOrder(oTOrder, OrderChangeBL.buildFromOrder(oTOrder, ORDER_CHANGE_STATUS_APPLY_ID));
		logger.debug("Created one time usage order with Id: {}", oneTimeOrderId);
		assertNotNull("one time usage order creation failed", oneTimeOrderId);
		
		return oneTimeOrderId;
	 }
	 
	 public static Integer createPlan(String fupQuantity,Integer productId1, Integer productId2, String planDescription) 
			  throws Exception {
	  
	  	JbillingAPI api = JbillingAPIFactory.getAPI();
     	chatProductId = TestConstants.CHAT_USAGE_PRODUCT_ID;
     	product8XXTollFreeId = TestConstants.PRODUCT_8XX_TOLL_FREE_ID;
     	
		UsagePoolWS usagePool1 = new UsagePoolWS();
		usagePool1.setName("FUP-"+fupQuantity+"-"+ Calendar.getInstance().getTimeInMillis());
		usagePool1.setQuantity(fupQuantity);
		usagePool1.setPrecedence(Integer.valueOf(-1));
		usagePool1.setCyclePeriodUnit("Billing Periods");
		usagePool1.setCyclePeriodValue(Integer.valueOf(1));
		Integer itemTypes[] = new Integer[1];
		itemTypes[0] = Integer.valueOf(ITEM_ID);
		usagePool1.setItemTypes(itemTypes);
		Integer items[] = new Integer[2];
        items[0] = Integer.valueOf(productId1);
        items[1] = Integer.valueOf(chatProductId);
        usagePool1.setItems(items);
        usagePool1.setEntityId(api.getCompany().getId());
        usagePool1.setUsagePoolResetValue("Reset To Initial Value");
       
        Integer usagePoolID1 = api.createUsagePool(usagePool1);
        assertNotNull("The item was not created", usagePoolID1);
        logger.debug("Created usagePool1 ID : {}", usagePoolID1);
       
        ItemDTOEx item = new ItemDTOEx();
        item.setEntityId(api.getCompany().getId());
        item.setDescription(planDescription);
        Integer types[] = new Integer[1];
        types[0] = new Integer(TYPE_ID);
        item.setTypes(types);
        item.setPrice(fupQuantity);
        item.setNumber(String.valueOf(new Date().getTime()));
        item.setCurrencyId(new Integer(1));
        item.setPriceModelCompanyId(api.getCompany().getId());
        Integer itemId = api.createItem(item);
        logger.debug("Item Created : {}", itemId);
       
        PlanItemBundleWS bundle1 = new PlanItemBundleWS();
        bundle1.setPeriodId(ORDER_PERIOD); //monthly
        bundle1.setQuantity(BigDecimal.ONE);

        PlanItemBundleWS bundle2 = new PlanItemBundleWS();
        bundle2.setPeriodId(ORDER_PERIOD);
        bundle2.setQuantity("0");

        PlanItemBundleWS bundle3 = new PlanItemBundleWS();
        bundle3.setPeriodId(ORDER_PERIOD);
        bundle3.setQuantity("0");
       
        PlanItemBundleWS bundle4 = new PlanItemBundleWS();
        bundle4.setPeriodId(ORDER_PERIOD);
        bundle4.setQuantity("0");

        PlanItemWS planItem1 = new PlanItemWS();
        planItem1.setItemId(Integer.valueOf(product8XXTollFreeId));
        planItem1.setBundle(bundle1);
        planItem1.setPrecedence(Integer.valueOf(1));

        PlanItemWS planItem2 = new PlanItemWS();
        planItem2.setItemId(Integer.valueOf(productId2));
        planItem2.setPrecedence(Integer.valueOf(2));
        planItem2.setBundle(bundle2);

        PlanItemWS planItem3 = new PlanItemWS();
        planItem3.setItemId(Integer.valueOf(chatProductId));
        planItem3.setPrecedence(Integer.valueOf(3));
        planItem3.setBundle(bundle3);
       
        PlanItemWS planItem4 = new PlanItemWS();
        planItem4.setItemId(Integer.valueOf(320110));
        planItem4.setPrecedence(Integer.valueOf(4));
        planItem4.setBundle(bundle4);

        List<PlanItemWS> planItems= new ArrayList<PlanItemWS>();

        PlanWS newPlan = new PlanWS();
        newPlan.setItemId(itemId);
        newPlan.setDescription(planDescription);
        newPlan.setPeriodId(ORDER_PERIOD);
        planItems.add(planItem1);
        planItems.add(planItem2);
        planItems.add(planItem3);
        planItems.add(planItem4);
        newPlan.setPlanItems(planItems);
        newPlan.setUsagePoolIds(new Integer[]{usagePoolID1});

        for(PlanItemWS planItem : newPlan.getPlanItems() ) {
	       	if(planItem.getItemId() == product8XXTollFreeId) {
	               planItem.addModel(CommonConstants.EPOCH_DATE,
	                       new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.00"), 1));
	       	} else {
	               planItem.addModel(CommonConstants.EPOCH_DATE,
	                       new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.95"), 1));
	       	}
        }

        Integer planId = api.createPlan(newPlan);
        logger.debug("Plan created Successfully : {}", planId);
        return planId;
	 }
	 
	/**
	 * @param orderActiveSinceDate
	 * @param orderActiveUntilDate
	 * @param nextInvoiceDate
	 * @param usagePoolQuantity
	 * @return expected Prorated Quantity
	 */
	
	public static BigDecimal calculateExpectedProratedQuantity(Date orderActiveSinceDate,Date orderActiveUntilDate, Date nextInvoiceDate , BigDecimal usagePoolQuantity) {
		if(null!=orderActiveSinceDate && (null!=orderActiveUntilDate || null!= nextInvoiceDate)) {
			Date endOfPeriod = null; 
			if (null == orderActiveUntilDate || (null != orderActiveUntilDate && null != nextInvoiceDate && orderActiveUntilDate.compareTo(nextInvoiceDate) >= 0)) {
				endOfPeriod = nextInvoiceDate;
			} else {
				endOfPeriod = new Date(orderActiveUntilDate.getTime()+(24*60*60*1000));
			}
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(orderActiveSinceDate);
			int noOfDaysInPeriod = 0;
			int noOfDaysInMonth = 0;
			BigDecimal oneDayCharge = BigDecimal.ZERO;
			noOfDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
			noOfDaysInPeriod = Days.daysBetween(new LocalDate(orderActiveSinceDate.getTime()),new LocalDate(endOfPeriod.getTime())).getDays();
			if(noOfDaysInPeriod==noOfDaysInMonth) {
			return usagePoolQuantity.setScale(10, BigDecimal.ROUND_HALF_UP);
			} else {
			    oneDayCharge = usagePoolQuantity.divide(BigDecimal.valueOf(noOfDaysInMonth),Constants.BIGDECIMAL_SCALE,Constants.BIGDECIMAL_ROUND);
			}
			return oneDayCharge.multiply(BigDecimal.valueOf(noOfDaysInPeriod)).setScale(Constants.BIGDECIMAL_QUANTITY_SCALE,Constants.BIGDECIMAL_ROUND);
		}
			return BigDecimal.ZERO.setScale(Constants.BIGDECIMAL_SCALE,Constants.BIGDECIMAL_ROUND);
	}
	
	/**
	 * getDate() function takes daysOfMonth of month and numberOfMonthtoAddInCurrentMonth as input and  .
	 * if addMonth is 1 it will add One month in current Month
	 * E.g (if current month is JANUARY and addMonth is 1 then this function returns 
	 * 25th of FEBRUARY)
	 * @param daysOfMonth 
	 * @param numberOfMonthtoAddInCurrentMonth
	 * @return Calendar Instance
	 */
	public static Calendar getDate(Integer daysOfMonth,Integer numberOfMonthtoAddInCurrentMonth) {
		if(daysOfMonth<=31 && daysOfMonth>0) {
			Calendar calendar = Calendar.getInstance();
			if(calendar.get(Calendar.DAY_OF_MONTH)==calendar.getActualMaximum(Calendar.DAY_OF_MONTH) && daysOfMonth<=9 && numberOfMonthtoAddInCurrentMonth!=0) {
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
			calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
			calendar.set(Calendar.DAY_OF_MONTH,calendar.get(Calendar.DAY_OF_MONTH)+daysOfMonth);
			} else {
				calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+numberOfMonthtoAddInCurrentMonth);
				calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
				calendar.set(Calendar.DAY_OF_MONTH, daysOfMonth);
			}
			return calendar;
		} else {
			throw new IllegalArgumentException(" illegal days passed as input "+daysOfMonth);
		}
	}

	/**
	 * this function returns expected remaining quantity after mediated usage
	 * @param customerId
	 * @param orderlines
	 * @return remainingQuantity
	 */
	public static BigDecimal calculateExpectedRemainingQuantity(Integer customerId, OrderLineWS[] orderlines)
			throws JbillingAPIException, IOException {
		if(null != customerId && null != orderlines) {
			
			BigDecimal totalFreeUsageQuantity = getTotalFreeUsageQuantity(customerId);
			BigDecimal totalQuantityUtilized = getTotalQuantityUtilized(orderlines);
			
			if(totalFreeUsageQuantity.compareTo(totalQuantityUtilized) >= 0) {
				return totalFreeUsageQuantity.subtract(totalQuantityUtilized);
			} else {
				return BigDecimal.ZERO.setScale(Constants.BIGDECIMAL_SCALE,Constants.BIGDECIMAL_ROUND);
			}
		} else {
			throw new IllegalArgumentException("## Null parameters");
		}
	}
	
	/**
	 * this function returns expected adjustment order quantity after changing the subscription order 
	 * active active until date
	 * @param customerId
	 * @param orderlines
	 * @return expectedAdjustmentQuantity
	 */
	public static BigDecimal calculateExpectedAdjustmentQuantity(Integer customerId, OrderLineWS[] orderlines)
			throws JbillingAPIException, IOException {
		if(null != customerId && null != orderlines) {
			
			BigDecimal totalFreeUsageQuantity = getTotalFreeUsageQuantity(customerId);
			BigDecimal totalQuantityUtilized = getTotalQuantityUtilized(orderlines);
			
			if(totalFreeUsageQuantity.compareTo(totalQuantityUtilized) <= 0) {
				return totalQuantityUtilized.subtract(totalFreeUsageQuantity)
						.setScale(Constants.BIGDECIMAL_SCALE_STR,Constants.BIGDECIMAL_ROUND);
			} else {
				return totalFreeUsageQuantity.subtract(totalQuantityUtilized)
						.setScale(Constants.BIGDECIMAL_SCALE_STR,Constants.BIGDECIMAL_ROUND);
			}
		} else {
			throw new IllegalArgumentException("## Null parameters");
		}		
	}
	
	public static BigDecimal getTotalFreeUsageQuantity(Integer customerId)
			throws JbillingAPIException, IOException {
		if(null != customerId ) {
			JbillingAPI api = JbillingAPIFactory.getAPI();
			CustomerUsagePoolWS[] custUsagePool = api.getCustomerUsagePoolsByCustomerId(customerId);
			
			BigDecimal totalFreeUsageQuantity = BigDecimal.ZERO;
			for (CustomerUsagePoolWS fupId : custUsagePool) {
				totalFreeUsageQuantity = totalFreeUsageQuantity.add(fupId.getInitialQuantityAsDecimal()
						.setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
			}
			return totalFreeUsageQuantity;
		} else {
			throw new IllegalArgumentException("## Null parameter");
		}
	}
	
	public static BigDecimal getTotalQuantityUtilized(OrderLineWS[] orderlines) {
		if(null != orderlines) {
			BigDecimal totalQuantityUtilized = BigDecimal.ZERO;
			for (OrderLineWS lineWS: orderlines) {
				totalQuantityUtilized= totalQuantityUtilized.add(lineWS.getQuantityAsDecimal());
			}
			return totalQuantityUtilized;
		} else {
			throw new IllegalArgumentException("## Null parameter");
		}
	}
	
	public  static List<Integer> getOrdersByMediationProcessId(UUID processId) {
	    try {
	    Set<Integer> orderIds = new HashSet<Integer>();	
	    JbillingMediationRecord[] records = JbillingAPIFactory.getAPI().getMediationRecordsByMediationProcessAndStatus(processId.toString(), JbillingMediationRecord.STATUS.PROCESSED.getId());
	    for(JbillingMediationRecord record: records) {
		orderIds.add(record.getOrderId());
	    }
	    	return new ArrayList<Integer>(orderIds);
	    } catch(Exception ex) {
		ex.printStackTrace();
		return Collections.emptyList();
	    }
	}
	
	public static  void waitForMediationComplete(JbillingAPI api, Integer maxTime) {
		Long start = new Date().getTime();
		while (api.isMediationProcessRunning() && new Date().getTime() < maxTime + start) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (new Date().getTime() > maxTime + start) {
			fail("Max time for mediation completion is exceeded");
		}
	}
	 
	 public static UsagePoolWS populateFreeUsagePoolObject(String fupQuantity) {
			
		 	inboundProductId = TestConstants.INBOUND_USAGE_PRODUCT_ID;
	        chatProductId = TestConstants.CHAT_USAGE_PRODUCT_ID;
	        activeResponceProductId = TestConstants.ACTIVE_RESPONSE_USAGE_PRODUCT_ID;
	        
	        UsagePoolWS usagePool = new UsagePoolWS();
	        usagePool.setName(fupQuantity+" "+"Free Min "+Calendar.getInstance().getTimeInMillis());
			usagePool.setQuantity(fupQuantity);
			usagePool.setPrecedence(Integer.valueOf(-1));
			usagePool.setCyclePeriodUnit("Billing Periods");
			usagePool.setCyclePeriodValue(Integer.valueOf(1));
			Integer itemTypes[] = new Integer[1];
			itemTypes[0] = Integer.valueOf(ITEM_ID);
			usagePool.setItemTypes(itemTypes);
			Integer items[] = new Integer[3];
	        items[0] = Integer.valueOf(inboundProductId);
	        items[1] = Integer.valueOf(chatProductId);
	        items[2] = Integer.valueOf(activeResponceProductId);
	        usagePool.setItems(items);
	        usagePool.setEntityId(ENTITY_ID);
	        usagePool.setUsagePoolResetValue("Reset To Initial Value");
	        
	        return usagePool;
		}
	 
	 /**
	  * Get Total of order lines quantities
	  * @param order
	  * @return
	  */
	 public static BigDecimal getOrderTotalQuantity(OrderWS order) {
			
		BigDecimal totalQuantity = BigDecimal.ZERO;
		for(OrderLineWS orderLine: order.getOrderLines()) {
			totalQuantity = (totalQuantity.add(orderLine.getQuantityAsDecimal()))
					.setScale(Constants.BIGDECIMAL_SCALE_STR, BigDecimal.ROUND_HALF_UP);
        }
		
		return totalQuantity;
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
	 
	 public  static PlanWS createPlan(String planPrice, String bundlePrice, Integer[] usagePoolIds, String planDescription, JbillingAPI api, Integer ...itemIds) {
	       if(null!=planPrice && null!=planDescription && null!=usagePoolIds && usagePoolIds.length>0 && null!=itemIds && itemIds.length>0) {
	        Integer itemId = createItem(TestConstants.ANSWER_FORCE_PLAN_CATEGORY_ID, planDescription, api, planPrice);
	        logger.debug("Item Created : {}", itemId);
	       
	        PlanItemBundleWS bundles[] = new PlanItemBundleWS[itemIds.length+1];
	        
	        // Bundle for DID-8XX Product
	        bundles[0] = new PlanItemBundleWS();
	        bundles[0].setPeriodId(2); //monthly
	        bundles[0].setQuantity(BigDecimal.ONE);
	        
	        for(int i=1;i<(itemIds.length+1);i++) {
   		bundles[i] = new PlanItemBundleWS();
   		bundles[i].setPeriodId(2); //monthly
	        bundles[i].setQuantity(BigDecimal.ZERO);
	        }
	        
	        PlanItemWS planItems[] = new PlanItemWS[itemIds.length+1];
	        
	        // PlanItem for DID-8XX Product
	        planItems[0] = new PlanItemWS(); 
	 		planItems[0].setItemId(Integer.valueOf(TestConstants.PRODUCT_8XX_TOLL_FREE_ID));
	 		planItems[0].setBundle(bundles[0]);
	 		planItems[0].setPrecedence(Integer.valueOf(1));
	 		planItems[0].addModel(CommonConstants.EPOCH_DATE, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal("0.00"), 1));
	 		
	        for(int i=1;i<(itemIds.length+1);i++) {
	     		planItems[i] = new PlanItemWS(); 
	     		planItems[i].setItemId(Integer.valueOf(itemIds[i-1]));
	     		planItems[i].setBundle(bundles[i]);
	     		planItems[i].setPrecedence(Integer.valueOf(i+1));
	     		planItems[i].addModel(CommonConstants.EPOCH_DATE, new PriceModelWS(PriceModelStrategy.FLAT.name(), new BigDecimal(bundlePrice), 1));
	        }
	        
	        PlanWS plan = new PlanWS();
	        plan.setItemId(itemId);
	        plan.setDescription(planDescription);
	        plan.setPeriodId(2);
	        plan.setPlanItems(Arrays.asList(planItems));
	        plan.setUsagePoolIds(usagePoolIds);

	        Integer planId = api.createPlan(plan);
	        assertNotNull("The Plan was not created", planId);
	        logger.debug("Plan created Successfully : {}", planId);
	        plan = api.getPlanWS(planId);
	        	return plan; 
	       } else {
	    	   	throw new NullPointerException("Input Parameters to createPlan Method Should not null"); 
	       }
	 }
	 
	 private static Integer createItem(Integer itemTypeId, String description, JbillingAPI api, String productPrice) {
		if(null!=itemTypeId && null!=description && null!=productPrice) {
			ItemDTOEx item = new ItemDTOEx();
		    item.setEntityId(api.getCallerCompanyId());
		    item.setDescription(description);
		    item.setTypes(new Integer[]{itemTypeId});
		    item.setPrice(productPrice);
		    item.setNumber(String.valueOf(new Date().getTime()));
		    item.setCurrencyId(new Integer(1));
		    item.setAssetManagementEnabled(0);
		    item.setHasDecimals(1);
		    item.setPriceModelCompanyId(api.getCallerCompanyId());
		    Integer itemId = api.createItem(item);
		    assertNotNull("The item was not created", itemId);
		    logger.debug("Item Created : {}", itemId);
		    return itemId;
		} else {
			throw new NullPointerException("Parameters to createItem Method Should not Null......");
		}
	}
	
	 public static  Date getCustomerUsagePoolCycleStartDateByPlanId(CustomerUsagePoolWS[] customerUsagePools, int planId) {
  		Date cycleStartDate = Util.getEpochDate();
		for(CustomerUsagePoolWS customerUsagePool : customerUsagePools) {
			if(customerUsagePool.getPlanId().equals(planId)) {
			    cycleStartDate = customerUsagePool.getCycleStartDate();
			    break;
			}
		}
		return cycleStartDate;
	 }
	 
	 public static  Date getCustomerUsagePoolCycleEndDateByPlanId(CustomerUsagePoolWS[] customerUsagePools, int planId) {
	     		Date cycleEndDate = Util.getEpochDate();
			for(CustomerUsagePoolWS customerUsagePool : customerUsagePools) {
				if(customerUsagePool.getPlanId().equals(planId)) {
					cycleEndDate = customerUsagePool.getCycleEndDate();
					break;
				}
			}
			return cycleEndDate;
	}
	 
	public static BigDecimal getTotalFreeUsageQuantity(CustomerUsagePoolWS[] customerUsagePools) {
	     BigDecimal totalFreeQuantity = BigDecimal.ZERO;
		for(CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
		    totalFreeQuantity = totalFreeQuantity.add(customerUsagePool.getInitialQuantityAsDecimal());
		}
		return totalFreeQuantity.setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
	}
	
	public static BigDecimal getTotalFreeUsageQuantityByOrder(OrderWS order) {
	     BigDecimal totalFreeQuantity = BigDecimal.ZERO;
		for(OrderLineWS line: order.getOrderLines()) {
		    for(OrderLineUsagePoolWS olUsagepool: line.getOrderLineUsagePools()) {
			totalFreeQuantity = totalFreeQuantity.add(new BigDecimal(olUsagepool.getQuantity()));	
		    }
		}
		return totalFreeQuantity.setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
	}
	
	public static BigDecimal getCustomerAvailableQuantity(CustomerUsagePoolWS[] customerUsagePools) {
	     BigDecimal totalAvailableQuantity = BigDecimal.ZERO;
		for(CustomerUsagePoolWS customerUsagePool: customerUsagePools) {
		    totalAvailableQuantity = totalAvailableQuantity.add(customerUsagePool.getQuantityAsDecimal());
		}
		return totalAvailableQuantity.setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
	}

	public static void updatePlugin (Integer basicItemManagerPlugInId, String className, JbillingAPI api) {
		PluggableTaskTypeWS type = api.getPluginTypeWSByClassName(className);
	    PluggableTaskWS plugin = api.getPluginWS(basicItemManagerPlugInId);
	    plugin.setTypeId(type.getId());
	    Hashtable<String, String> parameters = new Hashtable<String, String>();
	    if (className.equals(FullCreativeTestConstants.TELCO_USAGE_MANAGER_TASK_NAME)) {
            parameters.put("DNIS_Field_Name", "DNIS");
            plugin.setParameters(parameters);
	    } else {
            plugin.setParameters(parameters);
	    }
	    api.updatePlugin(plugin);
	}

	public static MediationRatingSchemeWS getRatingScheme(String name) {
		MediationRatingSchemeWS ratingSchemeWS = new MediationRatingSchemeWS();
		ratingSchemeWS.setName(name);
		ratingSchemeWS.setInitialIncrement(30);
		ratingSchemeWS.setInitialRoundingMode(BigDecimal.ROUND_UP);
		ratingSchemeWS.setMainIncrement(6);
		ratingSchemeWS.setMainRoundingMode(BigDecimal.ROUND_UP);
		ratingSchemeWS.setGlobal(Boolean.TRUE);
		ratingSchemeWS.setAssociations(new ArrayList<>());
		return ratingSchemeWS;
	}

	public static MediationRatingSchemeWS getRatingSchemeWithHalfRoundUp(String name) {
		MediationRatingSchemeWS ratingSchemeWS = new MediationRatingSchemeWS();
		ratingSchemeWS.setName(name);
		ratingSchemeWS.setInitialIncrement(60);
		ratingSchemeWS.setInitialRoundingMode(BigDecimal.ROUND_HALF_UP);
		ratingSchemeWS.setMainIncrement(60);
		ratingSchemeWS.setMainRoundingMode(BigDecimal.ROUND_UP);
		ratingSchemeWS.setGlobal(Boolean.TRUE);
		ratingSchemeWS.setAssociations(new ArrayList<>());
		return ratingSchemeWS;
	}

	public static Integer createPrepaidOrder(PlanWS planWS, Integer userId, Date activeSinceDate, Date activeUntilDate)
			throws JbillingAPIException, IOException {
		JbillingAPI api = JbillingAPIFactory.getAPI();
		product8XXTollFreeId = TestConstants.PRODUCT_8XX_TOLL_FREE_ID;

		BasicFilter basicFilter = new BasicFilter("status", FilterConstraint.EQ, "Available");
		SearchCriteria criteria = new SearchCriteria();
		criteria.setMax(3);
		criteria.setOffset(3);
		criteria.setSort("id");
		criteria.setTotal(-1);
		criteria.setFilters(new BasicFilter[]{basicFilter});

		// get an available asset's id for plan subscription item (id = 320104)
		AssetSearchResult assetsResult320104 = api.findProductAssetsByStatus(product8XXTollFreeId, criteria);

		assertNotNull("No available asset found for product 320104", assetsResult320104);
		AssetWS[] available320104Assets = assetsResult320104.getObjects();
		assertTrue("No assets found for product 320104.", null != available320104Assets && available320104Assets.length != 0);
		Integer assetIdProduct320104_1 = available320104Assets[0].getId();
		logger.debug("Asset Available for product id 320104 = {}", assetIdProduct320104_1);
		Integer assetIdProduct320104_2 = available320104Assets[1].getId();
		logger.debug("Asset Available for product id 320104 = {}", assetIdProduct320104_2);

		logger.debug("Creating subscription order...");
		OrderWS order = new OrderWS();
		order.setUserId(userId);
		order.setActiveSince(activeSinceDate);
		order.setActiveUntil(activeUntilDate);
		order.setBillingTypeId(Constants.ORDER_BILLING_PRE_PAID);
		order.setPeriod(2); // monthly
		order.setCurrencyId(1);
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
		OrderChangeWS [] orderChanges = OrderChangeBL.buildFromOrder(order, ORDER_CHANGE_STATUS_APPLY_ID);
		for (OrderChangeWS ws : orderChanges) {
			if (ws.getItemId().intValue() == planWS.getItemId().intValue()) {

				OrderChangePlanItemWS orderChangePlanItem = new OrderChangePlanItemWS();
				orderChangePlanItem.setItemId(product8XXTollFreeId);
				orderChangePlanItem.setId(0);
				orderChangePlanItem.setOptlock(0);
				orderChangePlanItem.setBundledQuantity(1);
				orderChangePlanItem.setDescription("DID-8XX");
				orderChangePlanItem.setMetaFields(new MetaFieldValueWS[0]);

				orderChangePlanItem.setAssetIds(new int[]{assetIdProduct320104_2});

				ws.setOrderChangePlanItems(new OrderChangePlanItemWS[]{orderChangePlanItem});
				ws.setStartDate(activeSinceDate);
			}
		}

		Integer orderId =  api.createOrder(order, orderChanges);
		assertNotNull("orderId should not be null",orderId);

		return orderId;

	}

}
