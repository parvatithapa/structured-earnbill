package com.sapienter.jbilling.server.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.server.discount.DiscountWS;
import com.sapienter.jbilling.server.discount.strategy.DiscountStrategyType;
import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.DataType;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.payment.PaymentInformationWS;
import com.sapienter.jbilling.server.payment.PaymentWS;
import com.sapienter.jbilling.server.payment.tasks.paypal.dto.CreditCardType;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.process.BillingProcessConfigurationWS;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.user.MainSubscriptionWS;
import com.sapienter.jbilling.server.user.UserDTOEx;
import com.sapienter.jbilling.server.user.UserWS;
import com.sapienter.jbilling.server.user.partner.CommissionProcessConfigurationWS;
import com.sapienter.jbilling.server.util.api.JbillingAPI;
import com.sapienter.jbilling.server.util.api.JbillingAPIException;
import com.sapienter.jbilling.server.util.api.JbillingAPIFactory;

/**
 * User: Nikhil
 * Date: 10/8/12
 * Description: A utility class written to help developers write test cases quickly covering some common tasks such as
 * a) Creating An OrderWS
 * b) Adding An Order Line to an OrderWS
 * c) Creating A PaymentWS
 * d) Creating A Customer returning UserWS
 * e) Creating A Customer ConatctWS
 * f) Pausing the thread for some seconds
 * g) Updating the billing configuration
 * h) Creating PlanWS
 * i) Get Contact Field Content
 */
public class CreateObjectUtil {

    /**
     * Creates an OrderWS object
     * @param userId
     * @param currencyId
     * @param billingType
     * @param orderPeriod
     * @param activeSince
     * @return
     */
    private static final Logger logger = LoggerFactory.getLogger(CreateObjectUtil.class);
	private final static int CC_PM_ID = 1;
	private final static String CC_MF_CARDHOLDER_NAME = "cc.cardholder.name";
	private final static String CC_MF_NUMBER = "cc.number";
	private final static String CC_MF_EXPIRY_DATE = "cc.expiry.date";
	private final static String CC_MF_TYPE = "cc.type";
    private final static String COUNTRY_CODE = "CA";
    private final static String STATE = "Ontario";
    private final static String ADDRESS1 = "333 Preston Street";
	
    public static OrderWS createOrderObject(
            Integer userId, Integer currencyId, Integer billingType, Integer orderPeriod,  Date activeSince) {
        /*
        * Create
        */
        OrderWS newOrder = new OrderWS();

        newOrder.setUserId(userId);
        newOrder.setCurrencyId(currencyId);
        newOrder.setBillingTypeId(billingType);
        newOrder.setPeriod(orderPeriod);

        //Defaults
        newOrder.setNotes("Domain: www.test.com");

        newOrder.setActiveSince(activeSince);
//         On some branches this field is present, please uncomment if required
//        newOrder.setCycleStarts(cal.getTime());

        return newOrder;
    }

    /**
     * To add a line to an order
     * @param order
     * @param lineQty
     * @param lineTypeId
     * @param lineItemId
     * @param linePrice
     * @param description
     * @return
     */
    public static OrderWS addLine(OrderWS order, Integer lineQty, Integer lineTypeId, Integer lineItemId, BigDecimal linePrice, String description) {

        // store the existing lines
        OrderLineWS[] existingLines = order.getOrderLines();
        List<OrderLineWS> finalLines = new ArrayList<OrderLineWS>();
        // iterate over the array and add to the ArrayList
        if(null != existingLines) {
	        for( OrderLineWS oneItem : existingLines ) {
	            finalLines.add(oneItem);
	        }
        }
        // Now add some 1 line
        OrderLineWS line;
        line = new OrderLineWS();
        line.setTypeId(lineTypeId);
        line.setItemId(lineItemId);
        if (null != linePrice) {
            line.setPrice(linePrice);
        }
        line.setAmount(linePrice);
        line.setQuantity(lineQty);
        line.setDescription(description);
        finalLines.add(line);
        OrderLineWS[] simpleArray = new OrderLineWS[ finalLines.size() ];
        finalLines.toArray( simpleArray );
        order.setOrderLines(simpleArray);
        return order;
    }

    /**
     * To create a payment object
     */
    public static PaymentWS createPaymentObject(Integer userId, BigDecimal amount, Integer currencyId, boolean isRefund, Integer paymentMethodId,
                                                Date paymentDate, String paymentNotes, PaymentInformationWS instrument) {
        PaymentWS payment = new PaymentWS();
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setIsRefund((isRefund ? new Integer(1) : new Integer(0)));
        payment.setCurrencyId(currencyId);
        payment.setMethodId(paymentMethodId);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentNotes(paymentNotes);

        if(instrument != null) {
        	payment.getPaymentInstruments().add(instrument);
        }
        
        return payment;
    }

    /**
     * To create a customer
     */
    public static UserWS createCustomer( Integer currencyId, String userName, String password, Integer languageId, Integer mainRoleId, boolean isParent, Integer statusID,
                                         PaymentInformationWS instrument, ContactWS contact, MainSubscriptionWS subscriptionWS) {

        UserWS newUser = new UserWS();
        newUser.setUserName(userName);
        newUser.setLanguageId(languageId);
        newUser.setCurrencyId(currencyId);

        //Provide Defaults
        newUser.setPassword(password);
        newUser.setMainRoleId(mainRoleId);//customer
        newUser.setIsParent(isParent);//not parent
        newUser.setStatusId(statusID); //active user
        
        newUser.setAccountTypeId(Integer.valueOf(1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);
        
        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);
        
        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);
        
        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // add a contact
        if(contact != null) {
            newUser.setContact(contact);
        }

        // instrument can be credit card or ach
        if(instrument != null) {
        	newUser.getPaymentInstruments().add(instrument);
        }
        

        // not on some branches currently, so remove this and also the parameter :(
        if(subscriptionWS != null) {
            newUser.setMainSubscription(subscriptionWS);
        }

        return newUser;
    }

    /**
     * To Create Customer Contact
     * @param email
     * @return
     */
    public static ContactWS createCustomerContact(String email) {
        ContactWS contact = new ContactWS();
        contact.setEmail(email);
        contact.setCountryCode(COUNTRY_CODE);
        contact.setStateProvince(STATE);
        contact.setAddress1(ADDRESS1);

        // rest of the fields are not mandatory
        return contact;
    }

    /**
     * To Pause the thread
     * @param t
     */
    public static void pause(long t) {

        try {
            Thread.sleep(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * To update the billing configuration
     * @param nextRunDate
     * @param maxPeriods
     * @param entityID
     * @param generateReport
     * @param onlyRecurring
     * @param invoiceDateProcess
     * 
     * @return
     */
    public static BillingProcessConfigurationWS updateBillingConfig(Date nextRunDate, Integer maxPeriods, Integer entityID,
                                                                    Integer generateReport, Integer onlyRecurring,
                                                                    Integer invoiceDateProcess) {

        BillingProcessConfigurationWS config = new BillingProcessConfigurationWS();

        config.setNextRunDate(nextRunDate);
        config.setMaximumPeriods(maxPeriods);
        config.setEntityId(entityID);
        config.setGenerateReport(generateReport);
        config.setInvoiceDateProcess(invoiceDateProcess);
        config.setOnlyRecurring(onlyRecurring);
        // present in some branches, pls uncomment if required
//        config.setPeriodUnitId(Constants.PERIOD_UNIT_MONTH);
//        config.setPeriodValue(1);
        return config;

    }

    public static PlanWS createPlan(Integer entityId, BigDecimal cost, Integer currencyID,
            Integer planItemType, Integer periodID, BigDecimal quantity, JbillingAPI api) {

        ItemDTOEx planItem = new ItemDTOEx();
        planItem.setNumber(String.valueOf(new Date().getTime()));
        planItem.setEntityId(entityId);
        planItem.setDescription("Test plan " + new Date().getTime());
        planItem.setDefaultPrice(new PriceModelWS(PriceModelStrategy.FLAT.name(), cost, currencyID));
        planItem.setCurrencyId(currencyID);
        planItem.setTypes(new Integer[]{planItemType});
        planItem.setPrice(cost);

        Integer planItemId = api.createItem(planItem);

        PlanItemWS sms = new PlanItemWS();
        sms.setItemId(planItemId);
      //  Present at some repo, please uncomment and use if required
//        sms.setModel(new PriceModelWS(PriceModelStrategy.ZERO.name(), new BigDecimal("0"), currencyID));
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setQuantity(quantity);
        sms.setBundle(bundle);
//	    bundle.setPeriodId(periodID);

        PlanWS plan = new PlanWS();
        plan.setDescription("Test plan description " + String.valueOf(new Date().getTime()));
        plan.setPeriodId(periodID);
        plan.setItemId(planItemId);
        plan.addPlanItem(sms);

        return plan;
    }

    // Present on some branches, uncomment if required
    /*public static String getContactFieldContent(ContactWS contact, Integer fieldId) {
        Integer index = 0;
        for(Integer id : contact.getFieldIDs()){
            if(id==fieldId){
                break;
            }
            index++;
        }

        return contact.getFieldValues()[index];
    }*/
    
    
    /**
	* To create a customer
	*/
    public static UserWS createCustomer(Integer currencyId, String userName,
            String password, Integer languageId, Integer mainRoleId,
            boolean isParent, Integer statusID, PaymentInformationWS instrument,
            ContactWS contact) {

        UserWS newUser = new UserWS();
        newUser.setUserName(userName);
        newUser.setLanguageId(languageId);
        newUser.setCurrencyId(currencyId);

        //defautl account type id
        newUser.setAccountTypeId(Integer.valueOf(1));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor


        //contact info
        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);
        
        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("Frodo");
        metaField4.setGroupId(1);
        
        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("Baggins");
        metaField5.setGroupId(1);
        
        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });
        
        // Provide Defaults
        newUser.setPassword(password);
        newUser.setMainRoleId(mainRoleId);// customer
        newUser.setIsParent(isParent);// not parent
        newUser.setStatusId(statusID); // active user

        // add a contact
        if (contact != null) {
            newUser.setContact(contact);
        }

        // instrument can be credit card or ach
        if(instrument != null) {
        	newUser.getPaymentInstruments().add(instrument);
        }

        return newUser;
    }

    
    /**
	* To create a plan with specified number of bundled items
	* @return PlanWS
	*/
    public static PlanWS createPlanWithBundledItems(
		    Integer periodID, BigDecimal quantity,
		    Integer subscriptionItemId, List<Integer> bundledItems) {

        PlanWS plan = new PlanWS();
        plan.setDescription("Test Plan Description " + String.valueOf(new Date().getTime()));
        plan.setPeriodId(periodID);
        plan.setItemId(subscriptionItemId);
        
        for (Integer bundleItemId : bundledItems) {
			PlanItemWS bundledItem = new PlanItemWS();
			bundledItem.setItemId(bundleItemId);
			PlanItemBundleWS bundle = new PlanItemBundleWS();
			bundle.setQuantity(quantity);
			bundledItem.setBundle(bundle);
			plan.addPlanItem(bundledItem);
        }

        return plan;
    }
    
    /**
	* To create a plan with specified number of bundled items
	*
	* @param entityId
	* @param cost
	* @param currencyID
	* @param planItemType
	* @param periodID
	* @param quantity
	* @param numberOfBundledItems
	* @param api
	* @return PlanWS
	*/
    public static PlanWS createPlanBundledItems(Integer entityId, BigDecimal cost,
            Integer currencyID, Integer planItemType, Integer periodID,
            BigDecimal quantity, int numberOfBundledItems, JbillingAPI api) {

    	ItemDTOEx subscriptionItem = createItem(entityId, cost,
    			currencyID, planItemType, "Test Plan Subscription Item");
        Integer subscriptionItemId = api.createItem(subscriptionItem);
        
        PlanWS plan = new PlanWS();
        plan.setDescription("Test Plan Description "
                + String.valueOf(new Date().getTime()));
        plan.setPeriodId(periodID);
        plan.setItemId(subscriptionItemId);
        
        int planItemCount = 0;
        for (int i=0; i < numberOfBundledItems; i++) {
        
	        ItemDTOEx item = createItem(entityId, cost.add(BigDecimal.ONE),
	        		currencyID, planItemType, "Test Plan Item (" + (i+1) + ")");
	        Integer itemId = api.createItem(item);
	        
			PlanItemWS bundledItem = new PlanItemWS();
			bundledItem.setItemId(itemId);
			PlanItemBundleWS bundle = new PlanItemBundleWS();
			bundle.setQuantity(quantity);
			bundle.setPeriodId(periodID);
			bundledItem.setBundle(bundle);
			
			plan.addPlanItem(bundledItem);
			
			planItemCount++;
        }

        // Create & Add a percentage item to the plan
        ItemDTOEx item = createPercentageItem(entityId, BigDecimal.TEN,
        currencyID, planItemType, "Test Plan Item (" + (planItemCount+1) + ")");
        Integer itemId = api.createItem(item);
        PlanItemWS bundledItem = new PlanItemWS();
        bundledItem.setItemId(itemId);
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setQuantity(quantity);
        bundle.setPeriodId(periodID);
        bundledItem.setBundle(bundle);
        plan.addPlanItem(bundledItem);
        // done adding a percentage item to the plan
        
        return plan;
    }
    
    public static PlanWS createPlanBundledItems(Integer entityId, BigDecimal cost,
            Integer currencyID, Integer planItemType, Integer periodID,
            BigDecimal quantity, int numberOfBundledItems, JbillingAPI api, Integer usagePoolId) {

    	ItemDTOEx subscriptionItem = createItem(entityId, cost,
    			currencyID, planItemType, "Test Plan Subscription Item");
        Integer subscriptionItemId = api.createItem(subscriptionItem);
        
        PlanWS plan = new PlanWS();
        plan.setDescription("Test Plan Description "
                + String.valueOf(new Date().getTime()));
        plan.setPeriodId(periodID);
        plan.setItemId(subscriptionItemId);
        plan.setUsagePoolIds(new Integer[]{usagePoolId});
        
        int planItemCount = 0;
        for (int i=0; i < numberOfBundledItems; i++) {
        
	        ItemDTOEx item = createItem(entityId, cost.add(BigDecimal.ONE),
	        		currencyID, planItemType, "Test Plan Item (" + (i+1) + ")");
	        Integer itemId = api.createItem(item);
	        
			PlanItemWS bundledItem = new PlanItemWS();
			bundledItem.setItemId(itemId);
			PlanItemBundleWS bundle = new PlanItemBundleWS();
			bundle.setQuantity(quantity);
			bundle.setPeriodId(periodID);
			bundledItem.setBundle(bundle);
			
			plan.addPlanItem(bundledItem);
			
			planItemCount++;
        }

        // Create & Add a percentage item to the plan
        ItemDTOEx item = createPercentageItem(entityId, BigDecimal.TEN,
        currencyID, planItemType, "Test Plan Item (" + (planItemCount+1) + ")");
        Integer itemId = api.createItem(item);
        PlanItemWS bundledItem = new PlanItemWS();
        bundledItem.setItemId(itemId);
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setQuantity(quantity);
        bundle.setPeriodId(periodID);
        bundledItem.setBundle(bundle);
        plan.addPlanItem(bundledItem);
        // done adding a percentage item to the plan
        
        return plan;
    }
    
    /**
	* To create a product without calling the createItem API.
	*
	* @param entityId
	* @param cost
	* @param currencyID
	* @param itemType
	* @return ItemDTOEx
	*/
    public static ItemDTOEx createItem(Integer entityId, BigDecimal cost, Integer currencyID, Integer itemType, String description) {
	     ItemDTOEx item = new ItemDTOEx();
	     item.setNumber(String.valueOf(new Date().getTime()));
	     item.setEntityId(entityId);
	     item.setDescription(description + Constants.SINGLE_SPACE + new Date().getTime());
	     item.setDefaultPrice(new PriceModelWS(PriceModelStrategy.FLAT
	                .name(), cost, currencyID));
	     item.setCurrencyId(currencyID);
	     item.setTypes(new Integer[] { itemType });
	     item.setPrice(cost);
	     return item;
    }

    /**
     * To create a Tiered Pricing product without calling the createItem API.
     *
     * @param entityId
     * @param currencyID
     * @param itemType
     * @param attributeMap
     * @return ItemDTOEx
     */
    public static ItemDTOEx createItemWithTieredPricing(Integer entityId, Integer currencyID, Integer itemType,
                                                        String description, Map<String, String> attributeMap, Date date) {
        ItemDTOEx item = new ItemDTOEx();
        item.setNumber(String.valueOf(new Date().getTime()));
        item.setEntityId(entityId);
        item.setDescription(description + Constants.SINGLE_SPACE + new Date().getTime());
        PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.TIERED.name(), null, currencyID);
        for(Map.Entry<String, String> entry : attributeMap.entrySet()){
            priceModelWS.addAttribute(entry.getKey(),  entry.getValue());
        }
        item.setDefaultPrice(priceModelWS);
        SortedMap<Date, PriceModelWS> sortedMap = new TreeMap<>();
        sortedMap.put(date, priceModelWS);
        item.setDefaultPrices(sortedMap);
        item.setPriceModelCompanyId(entityId);
        item.setCurrencyId(currencyID);
        item.setTypes(new Integer[] { itemType });
        return item;
    }

    /**
     * To create a Graduated Pricing product without calling the createItem API.
     *
     * @param entityId
     * @param cost
     * @param currencyID
     * @param itemType
     * @param included
     * @return ItemDTOEx
     */
    public static ItemDTOEx createItemWithGraduatedPricing(Integer entityId, BigDecimal cost, Integer currencyID,
                                                           Integer itemType, String description, String included, Date date) {
        ItemDTOEx item = new ItemDTOEx();
        item.setNumber(String.valueOf(new Date().getTime()));
        item.setEntityId(entityId);
        item.setDescription(description + Constants.SINGLE_SPACE + new Date().getTime());
        PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.GRADUATED.name(), cost, currencyID);
        priceModelWS.addAttribute("included",  included);
        item.setDefaultPrice(priceModelWS);
        SortedMap<Date, PriceModelWS> sortedMap = new TreeMap<>();
        sortedMap.put(date, priceModelWS);
        item.setPriceModelCompanyId(entityId);
        item.setDefaultPrices(sortedMap);
        item.setCurrencyId(currencyID);
        item.setTypes(new Integer[] { itemType });
        return item;
    }

    /**
     * To create a Capped Graduated Pricing product without calling the createItem API.
     *
     * @param entityId
     * @param cost
     * @param currencyID
     * @param itemType
     * @param included
     * @param max
     * @return ItemDTOEx
     */
    public static ItemDTOEx createItemWithCappedGraduatedPricing(Integer entityId, BigDecimal cost, Integer currencyID,
                                                                 Integer itemType, String description, String included,
                                                                 String max, Date date) {
        ItemDTOEx item = new ItemDTOEx();
        item.setNumber(String.valueOf(new Date().getTime()));
        item.setEntityId(entityId);
        item.setDescription(description + Constants.SINGLE_SPACE + new Date().getTime());
        PriceModelWS priceModelWS = new PriceModelWS(PriceModelStrategy.CAPPED_GRADUATED.name(), cost, currencyID);
        priceModelWS.addAttribute("included",  included);
        priceModelWS.addAttribute("max", max);
        item.setDefaultPrice(priceModelWS);
        SortedMap<Date, PriceModelWS> sortedMap = new TreeMap<>();
        sortedMap.put(date, priceModelWS);
        item.setDefaultPrices(sortedMap);
        item.setPriceModelCompanyId(entityId);
        item.setCurrencyId(currencyID);
        item.setTypes(new Integer[] { itemType });
        return item;
    }

    public static ItemDTOEx createPercentageItem(Integer entityId, BigDecimal percentage, Integer currencyID, Integer itemType, String description) {
        return createPercentageItem(entityId, percentage, currencyID, itemType, description, null);
    }

    public static ItemDTOEx createPercentageItem(Integer entityId, BigDecimal percentage, Integer currencyID, Integer itemType, String description, PriceModelWS price) {
	     ItemDTOEx item = new ItemDTOEx();
	     item.setNumber(String.valueOf(new Date().getTime()));
	     item.setEntityId(entityId);
	     item.setDescription("Percentage Item: " + description + Constants.SINGLE_SPACE + new Date().getTime());
	     item.setCurrencyId(currencyID);
	     item.setTypes(new Integer[] { itemType });
         if(price == null) {
             item.getDefaultPrices().put(new DateTime(2014, 12, 15, 0, 0, 0, 0).withTime(0, 0, 0, 0).toDate(), new PriceModelWS(PriceModelStrategy.LINE_PERCENTAGE
                     .name(), percentage, currencyID));
         }else{
             item.setDefaultPrice(new PriceModelWS(PriceModelStrategy.LINE_PERCENTAGE
                     .name(), percentage, currencyID));
         }

	     return item;
    }

    public static DiscountWS createAmountDiscount(Date discountStartDate, String code) {
        Calendar startOfThisMonth = Calendar.getInstance();
        startOfThisMonth.set(startOfThisMonth.get(Calendar.YEAR), startOfThisMonth.get(Calendar.MONTH), 1);

        Calendar afterOneMonth = Calendar.getInstance();
        afterOneMonth.setTime(startOfThisMonth.getTime());
        afterOneMonth.add(Calendar.MONTH, 1);

        DiscountWS discountWs = new DiscountWS();
        discountWs.setCode("D-AMNT-" + code);
        discountWs.setDescription("Discount-" + code + " Amount $" + BigDecimal.TEN);
        discountWs.setRate(BigDecimal.TEN);
        discountWs.setType(DiscountStrategyType.ONE_TIME_AMOUNT.name());

        if (discountStartDate != null) {
            discountWs.setStartDate(discountStartDate);
        }

        return discountWs;
    }

    public static CommissionProcessConfigurationWS createCommissionProcessConfig(Integer entityId, Date nextRunDate, Integer periodUnitId, Integer periodValue) {
        CommissionProcessConfigurationWS conf = new CommissionProcessConfigurationWS();
        conf.setEntityId(entityId);
        conf.setNextRunDate(nextRunDate);
        conf.setPeriodUnitId(periodUnitId);
        conf.setPeriodValue(periodValue);

        return conf;
    }

    public static UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId) throws JbillingAPIException, IOException {
        return createUser(goodCC, parentId, currencyId, true);
    }

    public static UserWS createUser(boolean goodCC, Integer parentId, Integer currencyId, boolean doCreate) throws JbillingAPIException, IOException {
        JbillingAPI api = JbillingAPIFactory.getAPI();

        /*
        * Create - This passes the password validation routine.
        */
        UserWS newUser = new UserWS();
        newUser.setUserId(0); // it is validated
        newUser.setUserName("testUserName-" + Calendar.getInstance().getTimeInMillis());
        newUser.setPassword("As$fasdf1");
        newUser.setLanguageId(Integer.valueOf(1));
        newUser.setMainRoleId(Integer.valueOf(5));
        newUser.setAccountTypeId(Integer.valueOf(1));
        newUser.setParentId(parentId); // this parent exists
        newUser.setStatusId(UserDTOEx.STATUS_ACTIVE);
        newUser.setCurrencyId(currencyId);
        newUser.setCreditLimit("1");
        newUser.setInvoiceChild(new Boolean(false));

        MetaFieldValueWS metaField1 = new MetaFieldValueWS();
        metaField1.setFieldName("partner.prompt.fee");
        metaField1.setValue("serial-from-ws");

        MetaFieldValueWS metaField2 = new MetaFieldValueWS();
        metaField2.setFieldName("ccf.payment_processor");
        metaField2.setValue("FAKE_2"); // the plug-in parameter of the processor

        MetaFieldValueWS metaField3 = new MetaFieldValueWS();
        metaField3.setFieldName("contact.email");
        metaField3.setValue(newUser.getUserName() + "@shire.com");
        metaField3.setGroupId(1);

        MetaFieldValueWS metaField4 = new MetaFieldValueWS();
        metaField4.setFieldName("contact.first.name");
        metaField4.setValue("FrodoRecharge");
        metaField4.setGroupId(1);

        MetaFieldValueWS metaField5 = new MetaFieldValueWS();
        metaField5.setFieldName("contact.last.name");
        metaField5.setValue("BagginsRecharge");
        metaField5.setGroupId(1);

        newUser.setMetaFields(new MetaFieldValueWS[]{
                metaField1,
                metaField2,
                metaField3,
                metaField4,
                metaField5
        });

        // valid credit card must have a future expiry date to be valid for payment processing
        Calendar expiry = Calendar.getInstance();
        expiry.set(Calendar.YEAR, expiry.get(Calendar.YEAR) + 1);
        
        // add a credit card
        PaymentInformationWS cc = createCreditCard("Frodo Rech Baggins", goodCC ? "4929974024420784" : "4111111111111111",
				expiry.getTime());

        newUser.getPaymentInstruments().add(cc);

        if (doCreate) {
            logger.debug("Creating user ...");
	        Integer userId = api.createUser(newUser);
	        newUser = api.getUserWS(userId);
        }

        return newUser;
    }
    
    public static PaymentInformationWS createCreditCard(String cardHolderName,
			String cardNumber, Date date) {
		PaymentInformationWS cc = new PaymentInformationWS();
		cc.setPaymentMethodTypeId(CC_PM_ID);
		cc.setProcessingOrder(new Integer(1));
		cc.setPaymentMethodId(Constants.PAYMENT_METHOD_VISA);
		
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
    
    public static void addMetaField(List<MetaFieldValueWS> metaFields,
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
}
