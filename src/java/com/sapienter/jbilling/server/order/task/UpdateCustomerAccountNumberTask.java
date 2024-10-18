package com.sapienter.jbilling.server.order.task;

import java.util.*;

import org.apache.log4j.Logger;
import org.apache.commons.lang.math.NumberUtils;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.db.MetaField;
import com.sapienter.jbilling.server.metafields.db.value.IntegerMetaFieldValue;
import com.sapienter.jbilling.server.metafields.db.value.StringMetaFieldValue;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.event.NewOrderEvent;
import com.sapienter.jbilling.server.order.event.NewQuantityEvent;
import com.sapienter.jbilling.server.pluggableTask.PluggableTask;
import com.sapienter.jbilling.server.pluggableTask.admin.ParameterDescription;
import com.sapienter.jbilling.server.pluggableTask.admin.PluggableTaskException;
import com.sapienter.jbilling.server.system.event.Event;
import com.sapienter.jbilling.server.system.event.task.IInternalEventsTask;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.UserDAS;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
/**
 * 
 * @author mazhar
 *
 */
public class UpdateCustomerAccountNumberTask extends PluggableTask implements
		IInternalEventsTask {

	private static final FormatLogger LOG = new FormatLogger(Logger.getLogger(PooledTarrifPlanTask.class));
	
	public static final ParameterDescription PHONE_NUMBER_CATEGORY_ID =
	    	new ParameterDescription("PhoneNumberCategoryId", true, ParameterDescription.Type.INT);
	public static final ParameterDescription DID_8XX =
	    	new ParameterDescription("DID_8XX", true, ParameterDescription.Type.STR);
	public static final ParameterDescription DID_800 =
	    	new ParameterDescription("DID_800", true, ParameterDescription.Type.STR);
	public static final ParameterDescription DID_LOCAL_ECF =
	    	new ParameterDescription("DID_LOCAL_ECF", true, ParameterDescription.Type.STR);
	
	 {
		 descriptions.add(DID_8XX);
		 descriptions.add(DID_800);
		 descriptions.add(DID_LOCAL_ECF);
	 }
	
	@SuppressWarnings("unchecked")
    private static final Class<Event> events[] = new Class[] { 
        NewQuantityEvent.class,
        NewOrderEvent.class
    };
	
	@SuppressWarnings("unchecked")
	@Override
	public void process(Event event) throws PluggableTaskException {
		
		String parameter_DID_8XX = getProductNumberDID_8XX();
		String parameter_DID_800 = getProductNumberDID_800();
		String parameter_DID_LOCAL_ECF = getProductNumberDID_LOCAL_ECF();
		
		if (parameter_DID_8XX == null){ 
			throw new PluggableTaskException("UpdateCustomerAccountNumberTask parameter value not set for parameter_DID_8XX");
		}
		if (parameter_DID_800 == null){ 
			throw new PluggableTaskException("UpdateCustomerAccountNumberTask parameter value not set for parameter_DID_800");
		}
		if (parameter_DID_LOCAL_ECF == null){ 
			throw new PluggableTaskException("UpdateCustomerAccountNumberTask parameter value not set for parameter_DID_LOCAL_ECF");
		}
		
		LOG.debug("parameter_DID_8XX: "+parameter_DID_8XX);
		LOG.debug("parameter_DID_800: "+parameter_DID_800);
		LOG.debug("parameter_DID_LOCAL_ECF: "+parameter_DID_LOCAL_ECF);
		
		OrderDTO orderDTO = null;
		if (event instanceof NewQuantityEvent){ 
			// check if it is an update order event
			OrderBL orderBL = new OrderBL(((NewQuantityEvent) event).getOrderId());
			orderDTO = orderBL.getDTO();
			
		} else {
			orderDTO = ((NewOrderEvent) event).getOrder();
		}
		
		// swap parent with child if child order has bundled item
		if (orderDTO.getChildOrders().size() > 0) {
			// get category Id plugin parameter. It'll be the category which has phone numbers
			String itemTypeIdStr = getPhoneNumberCategoryId();
			if (itemTypeIdStr == null || itemTypeIdStr.length() == 0){ // category Id is compulsory
				throw new PluggableTaskException("UpdateCustomerAccountNumberTask parameter value not set for PHONE_NUMBER_CATEGORY_ID");
			}
			Integer itemTypeId = Integer.valueOf(itemTypeIdStr);
			OrderDTO temp = getOrderWithBundleItem(orderDTO.getChildOrders(), itemTypeId);
			if(temp != null){
				orderDTO = temp;
			}
		}
		
		CustomerDTO customerDTO = orderDTO.getUser().getCustomer();
		StringMetaFieldValue primaryNumber = (StringMetaFieldValue)customerDTO.getMetaField(Constants.CUSTOMER_PRIMARY_ACCOUNT_NUMBER);
		LOG.debug("primary meta field: "+primaryNumber);
		LOG.debug("primary order: "+orderDTO.hasPrimaryOrder());
		LOG.debug("child orders: "+orderDTO.getChildOrders().size());
		
		if (primaryNumber == null) {
			// proceed only in case of null
			AssetDTO phoneNumber = null;
			
			List<AssetDTO> assets8xxPlanIdentifiers = null;
			List<AssetDTO> assets800PlanIdentifiers = null;
			List<AssetDTO> assetsLocalECFPlanIdentifiers = null;
			List<AssetDTO> assetsOthersPlanIdentifiers = null;
			
			List<AssetDTO> assets8xxIdentifiers = null;
			List<AssetDTO> assets800Identifiers = null;
			List<AssetDTO> assetsLocalECFIdentifiers = null;
			List<AssetDTO> assetsOthersIdentifiers = null;
			
			ItemDTO planSubscriptionItem = getPlanSubscriptionItem(orderDTO);
			PlanDTO plan = (null != planSubscriptionItem) ? planSubscriptionItem.getPlans().iterator().next() : null;
			
			for (OrderLineDTO orderLine: orderDTO.getLines()) {
				if (null != orderLine.getItem()){
					IntegerMetaFieldValue planItemId = (IntegerMetaFieldValue) orderLine.getMetaField(Constants.PLAN_ITEM_ID);
					if (parameter_DID_8XX.equals(orderLine.getItem().getNumber())) {
						// if item is found on plan's bundled items
						if (null != plan && null != plan.findPlanItem(orderLine.getItemId()) && null != planItemId && assets8xxPlanIdentifiers == null) {
							// populate assets8xxPlanIdentifiers
							assets8xxPlanIdentifiers = new ArrayList<AssetDTO>();
							assets8xxPlanIdentifiers.addAll(orderLine.getAssets()); // note this will happen only once in the loop
						} else {
							// populate assets8xxIdentifiers
							if (assets8xxIdentifiers == null) assets8xxIdentifiers = new ArrayList<AssetDTO>();
							assets8xxIdentifiers.addAll(orderLine.getAssets());
						}
					} else if (parameter_DID_800.equals(orderLine.getItem().getNumber())) {
						// if item is found on plan's bundled items
						if (null != plan && null != plan.findPlanItem(orderLine.getItemId()) && null != planItemId && assets800PlanIdentifiers == null) {
							// populate assets800PlanIdentifiers
							assets800PlanIdentifiers = new ArrayList<AssetDTO>();
							assets800PlanIdentifiers.addAll(orderLine.getAssets()); // note this will happen only once in the loop
						} else {
							// populate assets800Identifiers
							if (assets800Identifiers == null) assets800Identifiers = new ArrayList<AssetDTO>();
							assets800Identifiers.addAll(orderLine.getAssets());
						}
					} else if (parameter_DID_LOCAL_ECF.equals(orderLine.getItem().getNumber())) {
						// if item is found on plan's bundled items
						if (null != plan && null != plan.findPlanItem(orderLine.getItemId()) && null != planItemId && assetsLocalECFPlanIdentifiers == null) {
							// populate assetsLocalECFPlanIdentifiers
							assetsLocalECFPlanIdentifiers = new ArrayList<AssetDTO>();
							assetsLocalECFPlanIdentifiers.addAll(orderLine.getAssets()); // note this will happen only once in the loop
						} else {
							// populate assetsLocalECFIdentifiers
							if (assetsLocalECFIdentifiers == null) assetsLocalECFIdentifiers = new ArrayList<AssetDTO>();
							assetsLocalECFIdentifiers.addAll(orderLine.getAssets());
						}
					} else {
						// if item is found on plan's bundled items
						if (null != plan && null != plan.findPlanItem(orderLine.getItemId()) && null != planItemId && assetsOthersPlanIdentifiers == null) {
							// populate assetsOthersPlanIdentifiers
							assetsOthersPlanIdentifiers = new ArrayList<AssetDTO>();
							assetsOthersPlanIdentifiers.addAll(orderLine.getAssets()); // note this will happen only once in the loop
						} else {
							// populate assetsOthersIdentifiers
							if (assetsOthersIdentifiers == null) assetsOthersIdentifiers = new ArrayList<AssetDTO>();
							assetsOthersIdentifiers.addAll(orderLine.getAssets());
						}
					}
				}
			}
			
			Comparator<AssetDTO> assetComparator = new Comparator<AssetDTO>() {
				public int compare(AssetDTO s1, AssetDTO s2) {
					if (!NumberUtils.isNumber(s1.getIdentifier()) && !NumberUtils.isNumber(s2.getIdentifier())) {
						return -1;
					}
					Long s1Identifier = Long.parseLong(s1.getIdentifier());
					Long s2Identifier = Long.parseLong(s2.getIdentifier());
					return s1Identifier.compareTo(s2Identifier);
				}
			};
			
			if (null != assets8xxPlanIdentifiers && !assets8xxPlanIdentifiers.isEmpty()) {
				Collections.sort(assets8xxPlanIdentifiers, assetComparator);
				LOG.debug("assets8xxPlanIdentifiers: " + assets8xxPlanIdentifiers);
				phoneNumber = assets8xxPlanIdentifiers.get(0);
			} else if (null != assets800PlanIdentifiers && !assets800PlanIdentifiers.isEmpty()) {
				Collections.sort(assets800PlanIdentifiers, assetComparator);	
				LOG.debug("assets800PlanIdentifiers: " + assets800PlanIdentifiers);
				phoneNumber = assets800PlanIdentifiers.get(0);
			} else if (null != assetsLocalECFPlanIdentifiers && !assetsLocalECFPlanIdentifiers.isEmpty()) {
				Collections.sort(assetsLocalECFPlanIdentifiers, assetComparator);
				LOG.debug("assetsLocalECFPlanIdentifiers: " + assetsLocalECFPlanIdentifiers);
				phoneNumber = assetsLocalECFPlanIdentifiers.get(0);
			} else if (null != assetsOthersPlanIdentifiers && !assetsOthersPlanIdentifiers.isEmpty()) {
				Collections.sort(assetsOthersPlanIdentifiers, assetComparator);
				LOG.debug("assetsOthersPlanIdentifiers: " + assetsOthersPlanIdentifiers);
				phoneNumber = assetsOthersPlanIdentifiers.get(0);
			} else if (null != assets8xxIdentifiers && !assets8xxIdentifiers.isEmpty()) {
				Collections.sort(assets8xxIdentifiers, assetComparator);
				LOG.debug("assets8xxIdentifiers: " + assets8xxIdentifiers);
				phoneNumber = assets8xxIdentifiers.get(0);
			} else if (null != assets800Identifiers && !assets800Identifiers.isEmpty()) {
				Collections.sort(assets800Identifiers, assetComparator);	
				LOG.debug("assets800Identifiers: " + assets800Identifiers);
				phoneNumber = assets800Identifiers.get(0);
			} else if (null != assetsLocalECFIdentifiers && !assetsLocalECFIdentifiers.isEmpty()) {
				Collections.sort(assetsLocalECFIdentifiers, assetComparator);
				LOG.debug("assetsLocalECFIdentifiers: " + assetsLocalECFIdentifiers);
				phoneNumber = assetsLocalECFIdentifiers.get(0);
			} else if (null != assetsOthersIdentifiers && !assetsOthersIdentifiers.isEmpty()) {
				Collections.sort(assetsOthersIdentifiers, assetComparator);
				LOG.debug("assetsOtherIdentifiers: " + assetsOthersIdentifiers);
				phoneNumber = assetsOthersIdentifiers.get(0);
			}
			
			if (phoneNumber!=null) {
				LOG.debug("Asset found: ======== "+phoneNumber.getIdentifier());
				// update user after setting primary account number meta field 
				EntityType[] type = {EntityType.CUSTOMER};
				UserDTO userDTO = orderDTO.getUser();
				UserDAS userDAS = new UserDAS();
				MetaField primaryNumberMeta = MetaFieldBL.getFieldByName(orderDTO.getUser().getCompany().getId(), type, Constants.CUSTOMER_PRIMARY_ACCOUNT_NUMBER);
				customerDTO.setMetaField(primaryNumberMeta, phoneNumber.getIdentifier());
				userDTO.setCustomer(customerDTO);
				userDAS.makePersistent(userDTO); 
			} else {
				LOG.debug("Asset NOT found ...");
			}
		}
	}

	/**
	 * Get Plan subscription item from the order
	 * @param orderDTO
	 * @return
	 */
	private ItemDTO getPlanSubscriptionItem(OrderDTO orderDTO) {
		for (OrderLineDTO line : orderDTO.getLines()) {
			if (null != line.getItem() && line.getItem().hasPlans()) {
				return line.getItem();
			}
		}
		return null;
	}
	/**
	 * Get order which have bundled item in it. It'll be used in case of child orders.
	 * @param dtos
	 * @param categoryId
	 * @return
	 */
	private OrderDTO getOrderWithBundleItem(Set<OrderDTO> dtos,
			Integer categoryId) {
		for (OrderDTO orderDTO : dtos) {
			for (OrderLineDTO orderLine : orderDTO.getLines()) {
				if (null != orderLine.getItem()
						&& isPhoneNumberCategory(orderLine.getItem()
								.getItemTypes(), categoryId)) {
					return orderDTO;
				}
			}
		}
		return null;
	}
	@Override
	public Class<Event>[] getSubscribedEvents() {
		return events;
	}
	
	public String getPhoneNumberCategoryId() throws PluggableTaskException {
        return ensureGetParameter(PHONE_NUMBER_CATEGORY_ID.getName());
    }
	
	public String getProductNumberDID_8XX() throws PluggableTaskException {
        return ensureGetParameter(DID_8XX.getName());
    }
	
	public String getProductNumberDID_800() throws PluggableTaskException {
        return ensureGetParameter(DID_800.getName());
    }
	
	public String getProductNumberDID_LOCAL_ECF() throws PluggableTaskException {
        return ensureGetParameter(DID_LOCAL_ECF.getName());
    }
	
	public final String ensureGetParameter(String key) throws PluggableTaskException {
        Object value = parameters.get(key);
        if (!(value instanceof String)) {
            throw new PluggableTaskException("Missed or wrong parameter for: " + key + ", string expected: " + value);
        }

        return (String) value;
    }
	
	private boolean isPhoneNumberCategory(Set<ItemTypeDTO> itemTypes, Integer categoryId){
		for(ItemTypeDTO itemType: itemTypes){
			if(itemType.getId() == categoryId.intValue()){
				return true;
			}
		}
		return false;
	}
	
	private boolean validateIdentifier8XX(String identifier) {
		String RegEx = "^[8]"+identifier.charAt(1)+"{2}{2,}\\d{7}+";
		return identifier.matches(RegEx);
	}
	
	private boolean validateIdentifier800(String identifier) {
		String RegEx = "^8[0]{2}{2,}\\d{7}+";
		return identifier.matches(RegEx);
	}
	
	private boolean validateIdentifierChatOrActiveResponse(String identifier) {
		String RegEx = "^191|^121\\d{7}+";
		return identifier.matches(RegEx);
	}
}