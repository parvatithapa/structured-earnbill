package com.sapienter.jbilling.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.sapienter.jbilling.server.item.ItemDTOEx;
import com.sapienter.jbilling.server.item.PlanItemBundleWS;
import com.sapienter.jbilling.server.item.PlanItemWS;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.OrderLineWS;
import com.sapienter.jbilling.server.order.OrderWS;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.pricing.db.PriceModelStrategy;
import com.sapienter.jbilling.server.user.ContactWS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.api.JbillingAPI;

/**
 * User: Nikhil Date: 10/8/12 Description: A utility class written to help
 * developers write test cases quickly covering some common tasks such as a)
 * Creating An OrderWS b) Adding An Order Line to an OrderWS c) Creating A
 * PaymentWS d) Creating A Customer returning UserWS e) Creating A Customer
 * ConatctWS f) Pausing the thread for some seconds g) Updating the billing
 * configuration h) Creating PlanWS i) Get Contact Field Content
 */
public class CreateObjectUtil {

    /**
     * Creates an OrderWS object
     * 
     * @param userId
     * @param currencyId
     * @param billingType
     * @param orderPeriod
     * @param activeSince
     * @return
     */
    public static OrderWS createOrderObject(Integer userId, Integer currencyId,
            Integer billingType, Integer orderPeriod, Date activeSince) {
        /*
         * Create
         */
        OrderWS newOrder = new OrderWS();

        newOrder.setUserId(userId);
        newOrder.setCurrencyId(currencyId);
        newOrder.setBillingTypeId(billingType);
        newOrder.setPeriod(orderPeriod);

        // Defaults
        newOrder.setNotes("Domain: www.test.com");

        newOrder.setActiveSince(activeSince);
        // On some branches this field is present, please uncomment if required
        // newOrder.setCycleStarts(cal.getTime());

        return newOrder;
    }

    /**
     * To add a line to an order
     * 
     * @param order
     * @param lineQty
     * @param lineTypeId
     * @param lineItemId
     * @param linePrice
     * @param description
     * @return
     */
    public static OrderWS addLine(OrderWS order, Integer lineQty,
            Integer lineTypeId, Integer lineItemId, BigDecimal linePrice,
            String description) {

        // store the existing lines
        OrderLineWS[] existingLines = order.getOrderLines();
        List<OrderLineWS> finalLines = new ArrayList<OrderLineWS>();
        
        if (existingLines != null && existingLines.length > 0) {
	        // iterate over the array and add to the ArrayList
	        for (OrderLineWS oneItem : existingLines) {
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
        line.setUseItem(Boolean.TRUE);
        finalLines.add(line);
        OrderLineWS[] simpleArray = new OrderLineWS[finalLines.size()];
        finalLines.toArray(simpleArray);
        order.setOrderLines(simpleArray);
        return order;
    }

    /**
     * To Create Customer Contact
     * 
     * @param email
     * @return
     */
    public static ContactWS createCustomerContact(String email) {
        ContactWS contact = new ContactWS();
        contact.setEmail(email);
        // rest of the fields are not mandatory
        return contact;
    }

    /**
     * To Pause the thread
     * 
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
     * To create a plan
     * 
     * @param entityId
     * @param cost
     * @param currencyID
     * @param planItemType
     * @param periodID
     * @param quantity
     * @param api
     * @return
     */
    public static PlanWS createPlan(Integer entityId, BigDecimal cost,
            Integer currencyID, Integer planItemType, Integer periodID,
            BigDecimal quantity, JbillingAPI api) {

        ItemDTOEx planItem = new ItemDTOEx();
        planItem.setNumber(String.valueOf(new Date().getTime()));
        planItem.setEntityId(entityId);
        planItem.setDescription("Test plan " + new Date().getTime());
        planItem.setDefaultPrice(new PriceModelWS(PriceModelStrategy.FLAT
                .name(), cost, currencyID));
        planItem.setCurrencyId(currencyID);
        planItem.setTypes(new Integer[] { planItemType });
        planItem.setPrice(cost);

        Integer planItemId = api.createItem(planItem);

        PlanItemWS sms = new PlanItemWS();
        sms.setItemId(planItemId);

        // Present at some repo, please uncomment and use if required
        // sms.setModel(new PriceModelWS(PriceModelStrategy.ZERO.name(), new
        // BigDecimal("0"), currencyID));

        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setQuantity(quantity);
        sms.setBundle(bundle);

        PlanWS plan = new PlanWS();
        plan.setDescription("Test plan description "
                + String.valueOf(new Date().getTime()));
        plan.setPeriodId(periodID);
        plan.setItemId(planItemId);
        plan.addPlanItem(sms);

        return plan;
    }

	/**
	 * To Create Customer Contact
	 * 
	 * @param email
	 * @param zipCode
	 * @return
	 */
	public static ContactWS createCustomerContact(String email, String zipCode) {
		ContactWS contact = new ContactWS();
		contact.setEmail(email);
		contact.setPostalCode(zipCode);
		// rest of the fields are not mandatory
		return contact;
	}

	public static PlanItemWS createPlanItem(Integer entityId, BigDecimal cost,
			Integer currencyID, Integer planItemType, BigDecimal quantity,
			JbillingAPI api) {
		return createPlanItem(entityId, cost, currencyID, planItemType,
				quantity, null, api);
	}

	public static PlanItemWS createPlanItem(Integer entityId, BigDecimal cost,
			Integer currencyID, Integer planItemType, BigDecimal quantity,
			MetaFieldValueWS[] metaFieldValueWSs, JbillingAPI api) {

		ItemDTOEx planItem = new ItemDTOEx();
		planItem.setNumber(String.valueOf(new Date().getTime()));
		planItem.setEntityId(entityId);
		planItem.setDescription("Test plan " + new Date().getTime());
		planItem.setDefaultPrice(new PriceModelWS(PriceModelStrategy.FLAT
				.name(), cost, currencyID));
		planItem.setCurrencyId(currencyID);
		planItem.setTypes(new Integer[] { planItemType });
		planItem.setPrice(cost);
		if (metaFieldValueWSs != null) {
			planItem.setMetaFields(metaFieldValueWSs);
		}

		Integer planItemId = api.createItem(planItem);

		PlanItemWS sms = new PlanItemWS();
		sms.setItemId(planItemId);

		// Present at some repo, please uncomment and use if required
		// sms.setModel(new PriceModelWS(PriceModelStrategy.ZERO.name(), new
		// BigDecimal("0"), currencyID));

		PlanItemBundleWS bundle = new PlanItemBundleWS();
		bundle.setQuantity(quantity);
		sms.setBundle(bundle);

		return sms;
	}

	public static Integer createItem(String description, String price,
			String number, String itemTypeCode, JbillingAPI api) {
		ItemDTOEx newItem = new ItemDTOEx();
		newItem.setDescription(description);
		newItem.setPrice(new BigDecimal(price));
		newItem.setNumber(number);

		Integer types[] = new Integer[1];
		types[0] = new Integer(itemTypeCode);
		newItem.setTypes(types);

		System.out.println("Creating item ..." + newItem);
		return api.createItem(newItem);
	}

    public static PlanWS createPlan(Integer entityId, BigDecimal cost,
            Integer currencyID, Integer planItemType, Integer periodID,
            BigDecimal quantity, JbillingAPI api, boolean createBundleItem) {

    	PlanWS plan = null;
    	if (createBundleItem) {
	        ItemDTOEx planItem = new ItemDTOEx();
	        planItem.setNumber(String.valueOf(new Date().getTime()));
	        planItem.setEntityId(entityId);
	        planItem.setDescription("Test plan " + new Date().getTime());
	        planItem.setDefaultPrice(new PriceModelWS(PriceModelStrategy.FLAT
	                .name(), cost, currencyID));
	        planItem.setCurrencyId(currencyID);
	        planItem.setTypes(new Integer[] { planItemType });
	        planItem.setPrice(cost);
	
	        Integer planItemId = api.createItem(planItem);
	        
	        ItemDTOEx planBundleItem = new ItemDTOEx();
	        planBundleItem.setNumber(String.valueOf(new Date().getTime()));
	        planBundleItem.setEntityId(entityId);
	        planBundleItem.setDescription("Test plan bundle " + new Date().getTime());
	        planBundleItem.setDefaultPrice(new PriceModelWS(PriceModelStrategy.FLAT
	                .name(), cost, currencyID));
	        planBundleItem.setCurrencyId(currencyID);
	        planBundleItem.setTypes(new Integer[] { planItemType });
	        planBundleItem.setPrice(cost);
	
	        Integer planBundleItemId = api.createItem(planBundleItem);
	
	        PlanItemWS sms = new PlanItemWS();
	        sms.setItemId(planBundleItemId);
	
	        // Present at some repo, please uncomment and use if required
	        // sms.setModel(new PriceModelWS(PriceModelStrategy.ZERO.name(), new
	        // BigDecimal("0"), currencyID));
	
	        PlanItemBundleWS bundle = new PlanItemBundleWS();
	        bundle.setQuantity(quantity);
	        bundle.setPeriodId(periodID);
	        sms.setBundle(bundle);
	
	        plan = new PlanWS();
	        plan.setDescription("Test plan description "
	                + String.valueOf(new Date().getTime()));
	        plan.setPeriodId(periodID);
	        plan.setItemId(planItemId);
	        plan.addPlanItem(sms);
    	} else {
    		plan = createPlan(entityId, cost, currencyID, planItemType, periodID, quantity, api);
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
	        bundledItem.setBundle(bundle);
	        
	        plan.addPlanItem(bundledItem);
	        
	        planItemCount++;
        }

        // Create & Add a percentage item to the plan
        ItemDTOEx item = createPercentageItem(entityId,
    			currencyID, planItemType, "Test Plan Item (" + (planItemCount+1) + ")");
        Integer itemId = api.createItem(item);
        PlanItemWS bundledItem = new PlanItemWS();
        bundledItem.setItemId(itemId);
        PlanItemBundleWS bundle = new PlanItemBundleWS();
        bundle.setQuantity(quantity);
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
    
    public static ItemDTOEx createPercentageItem(Integer entityId, Integer currencyID, Integer itemType, String description) {
    	ItemDTOEx item = new ItemDTOEx();
    	item.setNumber(String.valueOf(new Date().getTime()));
    	item.setEntityId(entityId);
    	item.setDescription("Percentage Item: " + description + Constants.SINGLE_SPACE + new Date().getTime());
    	item.setCurrencyId(currencyID);
    	item.setTypes(new Integer[] { itemType });
    	return item;
    }

}