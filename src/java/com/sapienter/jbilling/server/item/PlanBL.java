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

package com.sapienter.jbilling.server.item;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sapienter.jbilling.server.adennet.ws.PrimaryPlanWS;
import com.sapienter.jbilling.server.util.Constants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.CommonConstants;
import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.item.db.PlanItemDAS;
import com.sapienter.jbilling.server.item.db.PlanItemDTO;
import com.sapienter.jbilling.server.item.event.NewPlanEvent;
import com.sapienter.jbilling.server.item.event.PlanDeletedEvent;
import com.sapienter.jbilling.server.item.event.PlanUpdatedEvent;
import com.sapienter.jbilling.server.metafields.MetaFieldBL;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderPeriodDAS;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pricing.PriceModelBL;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.system.event.EventManager;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.CustomerPriceBL;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.user.db.CompanyDTO;
import com.sapienter.jbilling.server.user.db.CustomerDTO;
import com.sapienter.jbilling.server.user.db.CustomerPriceDTO;

import java.util.stream.Collectors;

/**
 * Business Logic for PlanDTO CRUD operations and for subscribing and un-subscribing a
 * customer to a given plan. This class should be used for all Plan/Customer interactions.
 *
 * @author Brian Cowdery
 * @since 30-08-2010
 */
public class PlanBL {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private PlanDAS planDas;

    private PlanDTO  plan;

    public static final String PLAN_SUPPORTED_MODEMS_TABLE = "plan_supported_modem";
    public static final String PLAN_INFORMATION_OPTIONAL_TABLE = "plan_optional";

    public PlanBL() {
        _init();
    }

    public PlanBL(Integer planId) {
        _init();
        set(planId);
    }

    public PlanBL(PlanDTO plan) {
        _init();
        this.plan = plan;
    }

    /**
     * Convert a given PlanDTO into a PlanWS web-service object
     * @param dto dto to convert
     * @return converted web-service object
     */
    public static PlanWS getWS(PlanDTO dto) {
        if (dto != null) {
        	PlanWS retValue = getWS(dto, PlanItemBL.getWS(dto.getPlanItems()));

        	MetaFieldValueWS[] metaFields = new MetaFieldValueWS[0];

            List<Integer> companyIds = null;
            if (dto.getItem().isGlobal()) {
                companyIds = new CompanyDAS().getChildEntitiesIds(dto.getItem().getEntityId());
                companyIds.add(dto.getItem().getEntityId());
            } else {
                companyIds = dto.getItem().getChildEntitiesIds();
            }

            for (Integer id : companyIds) {
                MetaFieldValueWS[] childMetaFields = MetaFieldBL.convertMetaFieldsToWS(id, dto);
        		retValue.getMetaFieldsMap().put(id, childMetaFields);
        		metaFields = (MetaFieldValueWS[]) ArrayUtils.addAll(metaFields, childMetaFields);
            }

            retValue.setMetaFields(metaFields);

        	return retValue;
        }
        return null;
    }
    
    public static final PlanWS getWS(PlanDTO dto, List<PlanItemWS> planItems) {
       
    	PlanWS ws =new PlanWS();
    	ws.setId(dto.getId());
        ws.setDescription(dto.getDescription());
        ws.setFreeTrial(dto.isFreeTrial());
        ws.setEditable(dto.getEditable());
        ws.setPlanItems(planItems);
        
        if (dto.getItem() != null) ws.setItemId(dto.getItem().getId());
        if (dto.getPeriod() != null)ws.setPeriodId(dto.getPeriod().getId());
        if (null != dto.getUsagePools() && !dto.getUsagePools().isEmpty()) {
        	Integer []usagePoolIds = new Integer[dto.getUsagePools().size()];
        	int index = 0;
        	for (UsagePoolDTO fupDto : dto.getUsagePools()) {
        		usagePoolIds[index++] = fupDto.getId();
        	}
        	ws.setUsagePoolIds(usagePoolIds);
        	
        }
        return ws;
    }
    /**
     * Returns the given list of PlanDTO entities as WS objects.
     *
     * @param dtos list of PlanDTO to convert
     * @return plans as WS objects, or an empty list if source list is empty.
     */
    public static List<PlanWS> getWS(List<PlanDTO> dtos) {
        if (dtos == null)
            return Collections.emptyList();

        List<PlanWS> ws = new ArrayList<PlanWS>(dtos.size());
        for (PlanDTO plan : dtos)
            ws.add(getWS(plan));
        return ws;
    }

    /**
     * Convert a given PlanWS web-service object into a PlanDTO
     * @param ws ws object to convert
     * @return converted DTO object
     */
    public static PlanDTO getDTO(PlanWS ws) {
        if (ws != null) {
            if (ws.getItemId() == null)
                throw new SessionInternalError("PlanDTO must have a plan subscription item.");

            if (ws.getPeriodId() == null)
                throw new SessionInternalError("PlanDTO must have an applicable order period.");

            // subscription plan item
            ItemDTO item = new ItemBL(ws.getItemId()).getEntity();

            // plan period
            OrderPeriodDTO period = new OrderPeriodDAS().find(ws.getPeriodId());
            Set<UsagePoolDTO> usagePools = new HashSet<UsagePoolDTO>(0);
            if (null != ws.getUsagePoolIds()) {
	            for (Integer usagePoolId : ws.getUsagePoolIds()) {
	            	if (null != usagePoolId && NumberUtils.isNumber(usagePoolId.toString())) {
		            	UsagePoolDTO usagePool = new UsagePoolDAS().find(usagePoolId);
		            	usagePools.add(usagePool);
	            	}
	            }
            }

            PlanDTO retValue = new PlanDTO(ws, item, period, PlanItemBL.getDTO(ws.getPlanItems()), usagePools);

            List<Integer> allEntities = null;
            if (retValue.getItem().isGlobal()) {
                allEntities = new CompanyDAS().getChildEntitiesIds(retValue.getItem().getEntityId());
                allEntities.add(retValue.getItem().getEntityId());
            } else {
                allEntities = item.getChildEntitiesIds();
            }

            for (Integer id : allEntities) {
                MetaFieldBL.fillMetaFieldsFromWS(id, retValue, ws.getMetaFieldsMap().get(id));
            }

            return retValue;
        }
        return null;
    }

    /**
     * Validates all pricing models within the plan to ensure that they have the
     * correct attributes.
     *
     * @param plan plan to validate
     * @throws SessionInternalError if attributes are missing or of an incorrect type
     */
    public static void validateAttributes(PlanDTO plan) throws SessionInternalError {
        List<String> errors = new ArrayList<String>();

        for (PlanItemDTO planItem : plan.getPlanItems()) {
            for (PriceModelDTO model : planItem.getModels().values()) {
                for (PriceModelDTO next = model; next != null; next = next.getNext()) {
                    try {
                        AttributeUtils.validateAttributes(next.getAttributes(), next.getStrategy());
                        model.getStrategy().validate(next);
                    } catch (SessionInternalError e) {
                        errors.addAll(Arrays.asList(e.getErrorMessages()));
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new SessionInternalError("Plan pricing attributes failed validation.",
                                           errors.toArray(new String[errors.size()]));
        }
    }

    /**
     * Validates that if a plan is a One Per Customer or One Per Order plan then it does not
     * contain a product from any of the given categories
     * and if plan does not belong to any such category then only one such product is allowed
     *
     * @param plan						PlanDTO object
     * @throws SessionInternalError
     */
    public static void validateItems(PlanDTO plan) throws SessionInternalError {
    	int onePerCategoryProducts = 0;
    	for(PlanItemDTO planItem : plan.getPlanItems()) {
    		for(ItemTypeDTO type : planItem.getItem().getItemTypes()) {
    			if(type.isOnePerOrder() || type.isOnePerCustomer()) {

    				if(planItem.getBundle().getQuantity().intValue() > 1) {
    					throw new SessionInternalError("Oner Per Customer/Order product can not have more than 1 quantity.",
    							new String[]{"validation.error.one.per.category.quantity"});
    				}

    				if(onePerCategoryProducts > 0) {
    					throw new SessionInternalError("Plan can not have more than one product from One Per Customer/Order category.",
    							new String[]{"validation.error.cannot.have.two.one.per.category.product"});
    				}
    				onePerCategoryProducts++;
    			}
    		}
    	}
    }

    /**
     * Check that all contained products/plans are active for the dates of the plan.
     * If the plan does not have a active since or until date it will be set to the earliest expiration date
     * of contained products.
     *
     * @param plan
     */
    private void validateExpirationDates(PlanDTO plan) {
        Date[] fromTo = new Date[] { null, null };
        ItemDTO subscriptionItem = plan.getItem();

        validateContainedItemExpirationDates(plan, subscriptionItem, fromTo);

        if(subscriptionItem.getActiveSince() == null) {
            subscriptionItem.setActiveSince(fromTo[0]);
        }
        if(subscriptionItem.getActiveUntil() == null) {
            subscriptionItem.setActiveUntil(fromTo[1]);
        }

        validateContainingPlans(subscriptionItem);
    }

    /**
     * Validate that this product's validity doesn't fall inside the dates of any plans that it
     * may be contained in.
     *
     * @param item
     */
    public void validateContainingPlans(ItemDTO item) {
        if(item.getActiveSince() != null || item.getActiveUntil() != null) {
            List<PlanDTO> parentPlans = getPlansByAffectedItem(item.getId());
            for(PlanDTO parentPlan: parentPlans) {
                ItemDTO parentItem = parentPlan.getItem();
                //the active since date may not result in a parent plan having invalid active from/until dates
                if(item.getActiveSince() != null && (parentItem.getActiveSince() == null || parentItem.getActiveSince().before(item.getActiveSince()))) {
                    throw new SessionInternalError("Product will cause a plan it is contained in to have invalid active since/until dates.",
                            new String[]{"ItemDTOEx,activeSince,validation.error.item.activeSince.plan.inactive,"+parentItem.getInternalNumber()});
                }

                //the until since date may not result in a parent plan having invalid active from/until dates
                if(item.getActiveUntil() != null && (parentItem.getActiveUntil() == null || parentItem.getActiveUntil().after(item.getActiveUntil()))) {
                    throw new SessionInternalError("Product will cause a plan it is contained in to have invalid active since/until dates.",
                            new String[]{"ItemDTOEx,activeUntil,validation.error.item.activeUntil.plan.inactive,"+parentItem.getInternalNumber()});
                }
            }
        }
    }

    /**
     * Check that all contained products/plans are active for the dates of the plan.
     * The latest active from and earliest active until dates will returned in fromTo
     * @param plan
     * @param subscriptionItem
     * @param fromTo  - fromTo[0] = latest active from, fromTo[1] = earliest active until
     */
    private void validateContainedItemExpirationDates(PlanDTO plan, ItemDTO subscriptionItem, Date[] fromTo) {
        for (PlanItemDTO planItem : plan.getPlanItems()) {
            ItemDTO itemDTO = planItem.getItem();

            if (itemDTO.getActiveUntil() != null) {
                //if the contained product expires before the plan's active until date
                if(subscriptionItem.getActiveUntil() != null && itemDTO.getActiveUntil().before(subscriptionItem.getActiveUntil())) {
                    throw new SessionInternalError("Cannot Update Plan. Contained item is not valid for entire period.",
                            new String[]{"PlanWS,planItems,validation.error.plan.planItem.expired,"+itemDTO.getInternalNumber()});
                }
                //if the contained product expires before the plan's active since date
                if(subscriptionItem.getActiveSince() != null && subscriptionItem.getActiveSince().after(itemDTO.getActiveUntil())) {
                    throw new SessionInternalError("Cannot Update Plan. Contained item is not valid for entire period.",
                            new String[]{"PlanWS,planItems,validation.error.plan.planItem.expired,"+itemDTO.getInternalNumber()});
                }

                //find the earliest active until date
                if(fromTo[1] == null || itemDTO.getActiveUntil().before(fromTo[1]) ) {
                    fromTo[1] = itemDTO.getActiveUntil();
                }
            }

            if (itemDTO.getActiveSince() != null) {
                //if the contained product is valid from a date after the plan's active from date
                if(subscriptionItem.getActiveSince() != null && itemDTO.getActiveSince().after(subscriptionItem.getActiveSince())) {
                    throw new SessionInternalError("Cannot Update Plan. Contained item is not valid for entire period.",
                            new String[]{"PlanWS,planItems,validation.error.plan.planItem.expired,"+itemDTO.getInternalNumber()});
                }
                //if the contained product is valid from a date after the plan's active until date
                if(subscriptionItem.getActiveUntil() != null && subscriptionItem.getActiveUntil().before(itemDTO.getActiveSince())) {
                    throw new SessionInternalError("Cannot Update Plan. Contained item is not valid for entire period.",
                            new String[]{"PlanWS,planItems,validation.error.plan.planItem.expired,"+itemDTO.getInternalNumber()});
                }

                //find the latest active since date
                if(fromTo[0] == null || itemDTO.getActiveSince().after(fromTo[0]) ) {
                    fromTo[0] = itemDTO.getActiveSince();
                }
            }
        }
    }

    /**
     * Subscribes a customer to all plans held by the given "plan subscription" item, adding all
     * plan item prices to a customer price map.
     *
     * @param userId user id of the customer to subscribe
     * @param itemId item representing the subscription to a plan
     * @return list of saved customer price entries, empty if no prices applied to customer.
     */
    public static List<CustomerPriceDTO> subscribe(Integer userId, Integer itemId, Date activeSince) {
        logger.debug("Subscribing customer {} to plan subscription item {}", userId, itemId);

        List<CustomerPriceDTO> saved = new ArrayList<CustomerPriceDTO>();

        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(itemId)) {
            saved.addAll(customerPriceBl.addPrices(plan.getPlanItems()));
        }

        //8922 - the price should be active for the subscription date, in other words, a plan subscribed
        //since a future date should not make the prices of the plan available to the customer today
        for(CustomerPriceDTO customerPrice: saved) {
            customerPrice.setPriceSubscriptionDate(activeSince);
        }

        return saved;
    }

    /**
     * Un-subscribes a customer from all plans held by the given "plan subscription" item,
     * removing all plan item prices from the customer price map.
     *
     * @param userId user id of the customer to un-subscribe
     * @param itemId item representing the subscription to a plan
     */
    public static void unsubscribe(Integer userId, Integer itemId, Date effectiveDate) {
        logger.debug("Un-subscribing customer {} from plan subscription item {} since date {}", userId, itemId, effectiveDate);

        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(itemId)) {

            //TODO delete historical prices if old enough
            //This should be based on a preference.
            //        The preference should read 'MAX PERIOD FOR LATE USAGE RATING'
            //customerPriceBl.removePrices(plan.getId());

            customerPriceBl.expirePrices(plan.getId(), effectiveDate);
        }
    }

    /**
     * Returns true if the customer is subscribed to a plan held by the given "plan subscription" item.
     *
     * @param userId user id of the customer to check
     * @param itemId plan subscription item id
     * @return true if customer is subscribed, false if not
     */
    public static boolean isSubscribed(Integer userId, Integer itemId) {
        // items can have multiple plans, but it's possible that a customer may only
        // be subscribed to 1 of the plans depending on where we are in the workflow
        return new PlanDAS().isSubscribedByItem(userId, itemId);
    }

    /**
     * Returns true if the customer is subscribed to a plan held by the given "plan subscription" item.
     *
     * @param userId user id of the customer to check
     * @param itemId plan subscription item id
     * @return true if customer is subscribed with orderStatus finished, false if not
     */
    public static boolean isSubscribedFinished(Integer userId, Integer itemId) {
        // items can have multiple plans, but it's possible that a customer may only
        // be subscribed to 1 of the plans depending on where we are in the workflow
        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(itemId)){
            if (new PlanDAS().isSubscribedFinished(userId, plan.getId())){
                return true; // only return true if subscribed to one of the plans, otherwise keep checking.
            }
        }
        return false;
    }
    
    public static Map<Integer, BigDecimal> calculateItemDiffQuantityForPlans(Integer sourcePlanItemId, Integer targetPlanItemId) {
        Map<Integer, BigDecimal> itemsQuantityMap = new HashMap<Integer, BigDecimal>();
        PlanBL planBL = new PlanBL();
        addPlanItemsQuantityToMap(planBL.getPlansBySubscriptionItem(sourcePlanItemId), itemsQuantityMap, BigDecimal.ONE.negate());
        addPlanItemsQuantityToMap(planBL.getPlansBySubscriptionItem(targetPlanItemId), itemsQuantityMap, BigDecimal.ONE);
        return itemsQuantityMap;
    }

    // todo: add event logging for plans

    private static void addPlanItemsQuantityToMap(List<PlanDTO> plans, Map<Integer, BigDecimal> itemsQuantityMap, BigDecimal coef) {
        for (PlanDTO plan : plans) {
            if (plan.getEditable() == 0) {
                BigDecimal quantity = itemsQuantityMap.get(plan.getItemId());
                if (quantity == null) {
                    quantity = BigDecimal.ZERO;
                }
                quantity = quantity.add(BigDecimal.ONE.multiply(coef));
                itemsQuantityMap.put(plan.getItemId(), quantity);
                for (PlanItemDTO planItem : plan.getPlanItems()) {
                    Integer itemId = planItem.getItem().getId();
                    quantity = itemsQuantityMap.get(itemId);
                    if (quantity == null) {
                        quantity = BigDecimal.ZERO;
                    }
                    quantity = quantity.add(planItem.getBundle().getQuantity().multiply(coef));
                    itemsQuantityMap.put(itemId, quantity);
                }
            }
        }
    }
    
    public void set(Integer planId) {
        this.plan = planDas.findNow(planId);
    }

    private void _init() {
        this.planDas = new PlanDAS();
    }

    public PlanDTO getEntity() {
        return plan;
    }

    /**
     * Convert this plan into a PlanWS web-service object
     * @return this plan as a web-service object
     */
    public PlanWS getWS() {
        return PlanBL.getWS(plan);
    }

    private void validateUsagePools(PlanDTO plan) throws SessionInternalError {
    	List<ItemDTO> allItemsOnUsagePool = new ArrayList<ItemDTO>();
    	List<String> usagePoolNamesNotApplicableToThisPlan = new ArrayList<String>();
    	for (UsagePoolDTO usagePool : plan.getUsagePools()) {
    		for (ItemTypeDTO itemType : usagePool.getItemTypes()) {
    			allItemsOnUsagePool.addAll(itemType.getItems());
    		}
    		allItemsOnUsagePool.addAll(usagePool.getItems());

    		boolean usagePoolContainsPlanItem = false;
        	if (!allItemsOnUsagePool.isEmpty()) {
    	    	for (PlanItemDTO planItem : plan.getPlanItems()) {
    	    		if (allItemsOnUsagePool.contains(planItem.getItem())) {
    	    			usagePoolContainsPlanItem = true;
    	    			break;
    	    		}
    	    	}
        	}

        	if (!usagePoolContainsPlanItem) {
        		// the usage pool is not applicable to this plan, so take its name so that be displayed to user.
        		usagePoolNamesNotApplicableToThisPlan.add(usagePool.getDescription(usagePool.getEntity().getLanguageId(), "name"));
        	}
    	}
    	if (!usagePoolNamesNotApplicableToThisPlan.isEmpty()) {
    		StringBuilder usagePoolNames = new StringBuilder();
    		String separator = ", ";
    		for(String usagePoolName :usagePoolNamesNotApplicableToThisPlan) {
    			usagePoolNames.append(separator).append(usagePoolName);
    		}
    		throw new SessionInternalError("The selected usage pool(s) not applicable for the bundled items on this plan.", new String[] {
                    "PlanWS,usagepool,not.applicable.for.the.bundled.items.on.this.plan" + usagePoolNames});
    	}
    }

    public Integer create(PlanDTO plan) {
        if (plan != null) {
            validateAttributes(plan);
            validateUsagePools(plan);
            validateItems(plan);
            validateExpirationDates(plan);
            // update and validate meta fields
            // for parent entity
            if (plan.getItem().isGlobal()) {
                plan.updateMetaFieldsWithValidation(plan.getItem().getEntity().getLanguageId(), plan.getItem().getEntityId(), null, plan);
                for (CompanyDTO company : new CompanyDAS().findChildEntities(plan.getItem().getEntityId())) {
                    plan.updateMetaFieldsWithValidation(company.getLanguageId(), company.getId(), null, plan);
                }
            } else {
                for (Integer id : plan.getItem().getChildEntitiesIds()) {
                    plan.updateMetaFieldsWithValidation(new CompanyDAS().find(id).getLanguageId(), id, null, plan);
                }
            }
            this.plan = planDas.save(plan);

            // trigger internal event
            NewPlanEvent newPlanEvent = new NewPlanEvent(this.plan);
            EventManager.process(newPlanEvent);
            for(Integer entityId : this.plan.getItem().getChildEntitiesIds()) {
            	newPlanEvent.setEntityId(entityId);
            	EventManager.process(newPlanEvent);
            }

            return this.plan.getId();
        }

        logger.error("Cannot save a null PlanDTO!");
        return null;
    }

    public void update(PlanDTO dto) {
        if (plan != null) {

            List<CustomerDTO> subscribers = null;
            if (dto.getEditable() == 0) {
                // un-subscribe existing customers before updating
                subscribers = getCustomersByPlan(plan.getId());

                /*
                We do not delete any historical prices
                for (CustomerDTO customer : subscribers) {
                    unsubscribe(customer.getBaseUser().getUserId());
                }

                // clean all remaining prices just-in-case there's an orphaned record
                if (plan.getPlanItems().size() > 0) {
                    purgeCustomerPrices();
                }

                TODO delete historical prices if old enough
                    This should be based on a preference.
                    The preference should read 'MAX PERIOD FOR LATE USAGE RATING'
                 */
            }

            // do update
            validateAttributes(dto);
            validateUsagePools(dto);
            validateItems(dto);
            validateExpirationDates(dto);

            //clear meta fields, in case we have different child entities than the ones assigned before we do not want there meta fields
            plan.getMetaFields().clear();

            if (dto.getItem().isGlobal() && null != dto.getItem().getEntity()) {
           		for (CompanyDTO company : new CompanyDAS().findChildEntities(dto.getItem().getEntity().getId())) {
           			plan.updateMetaFieldsWithValidation(company.getLanguageId(), company.getId(), null, dto);
           		}
            } else {
           		for (Integer entityId : dto.getItem().getChildEntitiesIds()) {
           			plan.updateMetaFieldsWithValidation(new CompanyDAS().find(entityId).getLanguageId(), entityId, null, dto);
           		}
           	}

            plan.setDescription(dto.getDescription());
            plan.setFreeTrial(dto.isFreeTrial());
            plan.setEditable(dto.getEditable());
            plan.setItem(dto.getItem());
            plan.setPeriod(dto.getPeriod());
            plan.setUsagePools(dto.getUsagePools());

            /*
              Purge prices for removed plan items:

              This is not a new functionality. This was previously done for all plan.planItems
              Unfortunately, that was bad for performance. Additionally, now, we are to preserve priceModel updates
              for late guided usage rating.

              Remove by compare only ids because it don't matter if planItem's model is updated.
            */
            List<PlanItemDTO> common= plan.getPlanItems()
                                        .stream()
                                        .filter(oldPI -> dto.getPlanItems()
                                                .stream()
                                                .filter(dtoPI -> oldPI.getId().equals(dtoPI.getId()))
                                                .count() > 0)
                                        .collect(Collectors.toList());

            logger.debug("Removing prices for all customers for items {}", plan.getPlanItems());
            new CustomerPriceBL().removeAllPrices(plan.getPlanItems());

            logger.debug("Common items {}", common);
            plan.getPlanItems().removeAll(common);
            plan.getPlanItems().clear();
            plan.getPlanItems().addAll(dto.getPlanItems());

            logger.debug("Saving updates to plan {}", plan.getId());
            this.plan = planDas.save(plan);

            if (dto.getEditable() == 0) {
                // re-subscribe customers after plan has been saved
                for (CustomerDTO customer : subscribers) {
                    subscribe(customer.getBaseUser().getUserId());
                }
            }

            // trigger internal event
            PlanUpdatedEvent planUpdatedEvent = new PlanUpdatedEvent(plan);
            EventManager.process(planUpdatedEvent);
            for(Integer entityId : this.plan.getItem().getChildEntitiesIds()) {
            	planUpdatedEvent.setEntityId(entityId);
            	EventManager.process(planUpdatedEvent);
            }
        } else {
            logger.error("Cannot update, PlanDTO not found or not set!");
        }
    }

    public void addPrice(PlanItemDTO planItem) {
        if (plan != null) {

            PriceModelBL.validateAttributes(planItem.getModels().values());

            plan.addPlanItem(planItem);

            logger.debug("Saving updates to plan {}", plan.getId());
            this.plan = planDas.save(plan);

            // trigger internal event
            PlanUpdatedEvent planUpdatedEvent = new PlanUpdatedEvent(plan);
            EventManager.process(planUpdatedEvent);
            for(Integer entityId : this.plan.getItem().getChildEntitiesIds()) {
            	planUpdatedEvent.setEntityId(entityId);
            	EventManager.process(planUpdatedEvent);
            }

        } else {
            logger.error("Cannot add price, PlanDTO not found or not set!");
        }
    }

    public void delete() {
        if (plan != null) {
            purgeCustomerPrices();
            purgePlanItemsPrices();
            planDas.delete(plan);

            // trigger internal event
            PlanDeletedEvent planDeletedEvent = new PlanDeletedEvent(plan);
            EventManager.process(planDeletedEvent);
            for(Integer entityId : this.plan.getItem().getChildEntitiesIds()) {
            	planDeletedEvent.setEntityId(entityId);
            	EventManager.process(planDeletedEvent);
            }

        } else {
            logger.error("Cannot delete, PlanDTO not found or not set!");
        }
    }

    /**
     * Refreshes the customer plan item price mappings for all customers that have
     * subscribed to this plan. This method will remove all existing prices for the plan
     * and insert the current list of plan items into the customer price map.
     */
    public void refreshCustomerPrices() {
        if (plan != null) {
            logger.debug("Refreshing customer prices for subscribers to plan {}", plan.getId());

            for (CustomerDTO customer : getCustomersByPlan(plan.getId())) {
                CustomerPriceBL bl = new CustomerPriceBL(customer);
                bl.removePrices(plan.getId());
                bl.addPrices(plan.getPlanItems());
            }
        } else {
            logger.error("Cannot update customer prices, PlanDTO not found or not set!");
        }
    }

    /**
     * Removes all customer prices for the plan's current set of plan items. This will remove
     * prices for subscribed customers AND orphaned prices where the customers order has been
     * deleted in a non-standard way (DB delete, non API usage).
     */
    public void purgeCustomerPrices() {
        if (plan != null) {
            logger.debug("Removing ALL remaining customer prices for plan {}", plan.getId());
            new CustomerPriceBL().removeAllPrices(plan.getPlanItems());
        } else {
            logger.error("Cannot purge customer prices, PlanDTO not found or not set!");
        }

    }

    /**
     * Delete all prices from plan items to prevent a exception when you whant delete a plan and its items has chain prices.
     */
    public void purgePlanItemsPrices()
    {
        List <PlanItemDTO> planItemsList = plan.getPlanItems();
        PlanItemDAS planItemDAS = new PlanItemDAS();
        for (PlanItemDTO planItem : planItemsList) {
            planItem.setModels(null);
            planItemDAS.save(planItem);
        }
    }

    /**
     * Subscribes a customer to all plans held by the given "plan subscription" item, adding all
     * plan item prices to a customer price map. 
     *
     * @param userId user id of the customer to subscribe
     * @param itemId item representing the subscription to a plan
     * @return list of saved customer price entries, empty if no prices applied to customer.
     */
    public static List<CustomerPriceDTO> subscribe(Integer userId, Integer itemId, Date activeSince, Date endDate) {
        logger.debug("Subscribing customer {} to plan subscription item {}", userId, itemId);

        List<CustomerPriceDTO> saved = new ArrayList<CustomerPriceDTO>();

        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(itemId)) {
            saved.addAll(customerPriceBl.addPrices(plan.getPlanItems()));
        }

        //8922 - the price should be active for the subscription date, in other words, a plan subscribed
        //since a future date should not make the prices of the plan available to the customer today
        for(CustomerPriceDTO customerPrice: saved) {
            customerPrice.setPriceSubscriptionDate(activeSince);
            if (null != endDate) {
                customerPrice.setPriceExpiryDate(endDate);
            }
        }

        return saved;
    }

    /**
     * Subscribes a customer to this plan, adding all plan item prices to the customer price map.
     *
     * @param userId user id of the customer to subscribe
     * @return list of saved customer price entries, empty if no prices applied to customer.
     */
    public List<CustomerPriceDTO> subscribe(Integer userId) {
        logger.debug("Subscribing customer {} to plan {}", userId, plan.getId());

        List<CustomerPriceDTO> saved = new ArrayList<CustomerPriceDTO>();

        CustomerPriceBL customerPriceBl = new CustomerPriceBL(userId);
        saved.addAll(customerPriceBl.addPrices(plan.getPlanItems()));

        return saved;
    }

    /**
     * Un-subscribes a customer from this plan, removing all plan item prices from the customer price map.
     *
     * @param userId user id of the customer to un-subscribe
     */
    public void unsubscribe(Integer userId) {
        logger.debug("Un-subscribing customer {} from plan {}", userId, plan.getId());
        new CustomerPriceBL(userId).removePrices(plan.getId());
    }

    /**
     * Returns true if the customer is subscribed to a plan held by the given "plan subscription" item.
     *
     * @param userId user id of the customer to check
     * @param itemId plan subscription item id
     * @return true if customer is subscribed, false if not
     */
    public static boolean isSubscribed(Integer userId, Integer itemId, Date pricingDate) {
        // items can have multiple plans, but it's possible that a customer may only
        // be subscribed to 1 of the plans depending on where we are in the workflow
        for (PlanDTO plan : new PlanBL().getPlansBySubscriptionItem(itemId))
            if (new PlanDAS().isSubscribed(userId, plan.getId(), pricingDate))
                return true; // only return true if subscribed to one of the plans, otherwise keep checking.

        return false;
    }

    /**
     * Returns true if the customer is subscribed to this plan.
     * Returns false if plan not exist in system.
     *
     * @param userId user id of the customer to check
     * @return true if customer is subscribed, false if not
     */
    public boolean isSubscribed(Integer userId, Date pricingDate) {
        // if plan is not found then customer can not be subscribed to that.
        if(plan==null) {
            return false;
        }
        return planDas.isSubscribed(userId, plan.getId(), pricingDate);
    }

    /**
     * Returns a list of all customers that have subscribed to the given plan. A customer
     * subscribes to a plan by adding the plan subscription item to a recurring order.
     *
     * @param planId id of plan
     * @return list of customers subscribed to the plan, empty if none found
     */
    public List<CustomerDTO> getCustomersByPlan(Integer planId) {
        return planDas.findCustomersByPlan(planId);
    }

    /**
     * Returns all plans that use the given item as the "plan subscription" item.
     *
     * @param itemId item id
     * @return list of plans, empty list if none found
     */
    public List<PlanDTO> getPlansBySubscriptionItem(Integer itemId) {
        return planDas.findByPlanSubscriptionItem(itemId);
    }

    /**
     * Returns all plans that affect the pricing of the given item, or that include
     * the item in a bundle.
     *
     * @param itemId item id
     * @return list of plans, empty list if none found
     */
    public List<PlanDTO> getPlansByAffectedItem(Integer itemId) {
        return planDas.findByAffectedItem(itemId);
    }

    public List<PlanDTO> getPoolContributingPlans(Integer poolItemCategory) {
        return planDas.getPoolContributingPlans(poolItemCategory);
    }

    /**
     * This method used for finding the plan id's by entity id.
     *
     * @param entityId .
     * @return List<Integer> plan id's.
     */
    public List<Integer> findIdsByEntity(Integer entityId) {
        List<Integer> result = new PlanDAS().findIdsByEntity(entityId);
        return result;
    }


    public BigDecimal calculateTotalPlanAmount(Integer categoryId) {
        if (plan == null) {
            logger.warn("Called with null parameters");
            return BigDecimal.ZERO;
        }

        Date today = new Date();
        BigDecimal total = plan.getPlanSubscriptionItem().getPrice(today).getRate();
        plan.getPlanItems()
            .stream()
            .filter(planItem -> planItem.getItem()
                                        .getItemTypes()
                                        .stream()
                                        .noneMatch(itemType -> categoryId != null && itemType.getId() == categoryId))
            .map(planItem -> planItem.getPrice(today)
                                     .getRate()
                                     .multiply(planItem.getBundle().getQuantity()))
            .reduce(total, BigDecimal::add);

        return total;
    }

    public BigDecimal getTaxRate(Date invoiceGenerationDate, String taxTableName, String taxDateFormat) {
        @SuppressWarnings("rawtypes")
        MetaFieldValue taxSchemeMetaField = plan.getMetaField(CommonConstants.TAX_SCHEME);
        return taxSchemeMetaField != null ? new ItemDAS().getTaxRate((String) taxSchemeMetaField.getValue(), taxTableName,
                invoiceGenerationDate, taxDateFormat) :  BigDecimal.ZERO;
    }

    public List<PlanDTO> findPlanByPlanNumber(String planNumber, Integer entityId) {
        return planDas.findPlanByPlanNumber(planNumber, entityId);
    }

    public PrimaryPlanWS getPlan(Integer planId, Integer addOnCategoryId, String taxTableName, String taxDateFormat) {
        PlanDTO plan = planDas.findNow(planId);
        return buildPrimaryPlanWS(addOnCategoryId, taxTableName, taxDateFormat, plan);
    }

    public PrimaryPlanWS getPlan(String internalNumber, Integer entityId, Integer addOnCategoryId, String taxTableName, String taxDateFormat) {
        PlanDTO plan = planDas.findByPlanNumber(internalNumber, entityId);
        return buildPrimaryPlanWS(addOnCategoryId, taxTableName, taxDateFormat, plan);
    }

    private PrimaryPlanWS buildPrimaryPlanWS(Integer addOnCategoryId, String taxTableName, String taxDateFormat, PlanDTO plan) {
        this.plan = plan;
        Boolean isAddon = Arrays.asList(plan.getItem().getTypes()).contains(addOnCategoryId);
        BigDecimal taxRate = getTaxRate(new Date(), taxTableName, taxDateFormat);
        BigDecimal price = plan.getItem().getPrice(new Date()).getRate();
        BigDecimal planPrice = price.multiply(BigDecimal.ONE.add(taxRate.divide(new BigDecimal(100))))
                .setScale(2, RoundingMode.HALF_UP);

        return PrimaryPlanWS.builder().id(plan.getId())
                .description(plan.getItem().getDescription(Constants.LANGUAGE_ENGLISH_ID))
                .validityInDays(new OrderPeriodDAS().find(plan.getPeriod().getId()).getValue())
                .price(planPrice)
                .isAddOn(isAddon)
                .build();
    }
}
