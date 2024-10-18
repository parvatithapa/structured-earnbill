/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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

package com.sapienter.jbilling.server.usagePool;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.item.ItemBL;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.ItemTypeDAS;
import com.sapienter.jbilling.server.item.db.ItemTypeDTO;
import com.sapienter.jbilling.server.item.db.PlanDAS;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.notification.NotificationMediumType;
import com.sapienter.jbilling.server.pricing.util.AttributeUtils;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolConsumptionActionDTO;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.user.db.CompanyDAS;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.db.InternationalDescriptionDTO;
import com.sapienter.jbilling.server.util.db.LanguageDTO;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
/**
 * UsagePoolBL
 * Server side CRUD and other related functions for FUP.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class UsagePoolBL {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


	private UsagePoolDAS usagePoolDas = null;
    private UsagePoolDTO usagePool = null;
    
    public UsagePoolBL(Integer usagePoolId)
            throws SessionInternalError {
        try {
            init();
            set(usagePoolId);
        } catch (Exception e) {
            throw new SessionInternalError("Setting Usage Pool", UsagePoolBL.class, e);
        }
    }

    public UsagePoolBL() {
        init();
    }

    public UsagePoolBL(UsagePoolDTO usagePool) {
        this.usagePool = usagePool;
        init();
    }

    public void set(Integer usagePoolId) {
        usagePool = usagePoolDas.find(usagePoolId);
    }

    private void init() {
        usagePoolDas = new UsagePoolDAS();
    }

    public UsagePoolDTO getEntity() {
        return usagePool;
    }
    
    public UsagePoolWS getWS(UsagePoolDTO dto) {
		if (dto == null) {
			dto = this.usagePool;
		}
		
		return getUsagePoolWS(dto);
    }
    
    public static UsagePoolWS getUsagePoolWS(UsagePoolDTO dto){
    	
		UsagePoolWS ws = new UsagePoolWS();
		ws.setId(dto.getId());
		ws.setQuantity(null != dto.getQuantity() ? dto.getQuantity().toString()
				: "");
		ws.setPrecedence(dto.getPrecedence());
		ws.setCyclePeriodUnit(dto.getCyclePeriodUnit());
		ws.setCyclePeriodValue(dto.getCyclePeriodValue());
		if (null != dto.getItemTypes()) {
			ws.setItemTypes(new Integer[dto.getItemTypes().size()]);
			int index = 0;
			for (ItemTypeDTO itemTypeDto : dto.getItemTypes()) {
				ws.getItemTypes()[index++] = itemTypeDto.getId();
			}
		}
		if (null != dto.getItems()) {
			ws.setItems(new Integer[dto.getItems().size()]);
			int index = 0;
			for (ItemDTO itemDto : dto.getItems()) {
				ws.getItems()[index++] = itemDto.getId();
			}
		}
		ws.setUsagePoolResetValue(null != dto.getUsagePoolResetValue() ? dto
				.getUsagePoolResetValue().toString() : "");
		ws.setEntityId(dto.getEntity().getId());
		ws.setCreatedDate(dto.getCreatedDate());
		for (UsagePoolConsumptionActionDTO actionDto : dto
				.getConsumptionActions()) {
			ws.addConsumptionActions(new UsagePoolBL()
					.getUsagePoolConsumptionWS(actionDto));
		}
		ws.setOwningEntityId(getOwningEntityId(ws));
		return ws;
    }
    
    public static final Integer getOwningEntityId(UsagePoolWS ws) {
	    if (ws.getId() <= 0) {
	        return null;
	    }
	    return new UsagePoolBL(ws.getId()).getEntity().getEntity().getId();
    }

    public UsagePoolConsumptionActionWS getUsagePoolConsumptionWS(UsagePoolConsumptionActionDTO actionDto) {
        UsagePoolConsumptionActionWS ws = new UsagePoolConsumptionActionWS();
        ws.setId("" + actionDto.getId());
        ws.setPercentage("" + actionDto.getPercentage());
        ws.setType(actionDto.getType());
        if (actionDto.getNotificationId() != null) {
            ws.setNotificationId("" + actionDto.getNotificationId());
            ws.setMediumType(NotificationMediumType.valueOf(actionDto.getMediumType()));
        } else {
            ws.setProductId("" + actionDto.getProductId());
        }
        return ws;
    }

    public UsagePoolConsumptionActionDTO getUsagePoolConsumptionDto(UsagePoolConsumptionActionWS actionWS) {
        UsagePoolConsumptionActionDTO action = new UsagePoolConsumptionActionDTO();
        if (actionWS.getType().toLowerCase().contains("fee")) {
            action.setProductId(toint(actionWS.getProductId()));
        } else {
            action.setNotificationId(toint(actionWS.getNotificationId()));
            if (actionWS.getMediumType() != null) {
                action.setMediumType(actionWS.getMediumType().name());
            }
        }
        action.setPercentage(percentToInt(actionWS.getPercentage()));
        action.setType(actionWS.getType());
        action.setId(toint(actionWS.getId()));
        return action;
    }
    
	private Integer percentToInt(String s) {
		String value = StringUtils.trimToNull(s);
		if (StringUtils.isNotBlank(value)) {
			int val = NumberUtils.toInt(value);
			return val < 0 ? null : val;
		}
		return null;
	}

    private Integer toint(String s) {
	String value= StringUtils.trimToNull(s);
        if (StringUtils.isNotBlank(value)) {
		int val = NumberUtils.toInt(value);
            return val==0 ? null : val;
        }
        return null;
    }
    
    /**
     * This method validates the quantity on usage pool is not negative and not zero.
     * It also makes sure that at least one product or category is specified on the usage pool.
     * @param usagePoolWs
     * @throws SessionInternalError
     */
    public void validateUsagePoolWS(UsagePoolWS usagePoolWs) throws SessionInternalError {

        //Value cannot be negative
    	if(!NumberUtils.isNumber(usagePoolWs.getQuantity()) || new BigDecimal(usagePoolWs.getQuantity()).doubleValue() < 0.00){
			throw new SessionInternalError("Usage Pool Quantity must be a positive value.",
					new String[]{ "UsagePoolWS,quantity,quantity.must.be.positive" });
		}

		//For reserved products - "HOURS PER CALENDAR MONTH" reset value ie. dynamic usage pool,
		// the quantity will be set dynamically and must not be entered by the user
		// Quantity positive for general case
		if(new BigDecimal(usagePoolWs.getQuantity()).doubleValue() == 0.00 && !usagePoolWs.getUsagePoolResetValue().equals(UsagePoolResetValueEnum.HOURS_PER_CALENDER_MONTH.toString())) {
    		throw new SessionInternalError("Usage Pool Quantity must be a positive value.",
    			new String[]{ "UsagePoolWS,quantity,quantity.must.be.positive" });
    	}

        // Quantity zero for reserved
    	if (new BigDecimal(usagePoolWs.getQuantity()).doubleValue() > 0.00 && usagePoolWs.getUsagePoolResetValue().equals(UsagePoolResetValueEnum.HOURS_PER_CALENDER_MONTH.toString())){
			throw new SessionInternalError("Usage Pool Quantity must be 0 for reset value: hours per calendar month",
					new String[]{ "UsagePoolWS,quantity,quantity.must.be.zero.for.reset.value.hours.per.calendar.month" });
		}
    	
    	if(null == usagePoolWs.getItemTypes() && null == usagePoolWs.getItems()) {
    		throw new SessionInternalError("Select Atleast one Category or Product.", 
        			new String[]{ "UsagePoolWS,itemTypes,select.atleast.one.category.or.product" });
    	}
    	
    	//Added validations for Cycle Period value to resolve issue #7349
       	if(null == usagePoolWs.getCyclePeriodValue()) {
    		throw new SessionInternalError("Usage Pool Cycle Period is empty or missing.", 
        			new String[]{ "UsagePoolWS,cyclePeriodValue,usagePool.error.cycle.period.empty" });
    	}
    	
    	if(null != usagePoolWs.getCyclePeriodValue() && (!NumberUtils.isNumber(usagePoolWs.getCyclePeriodValue().toString()) ||
    			usagePoolWs.getCyclePeriodValue().intValue() <= 0)) {
    		throw new SessionInternalError("Usage Pool Cycle Period must be greater than zero and a whole number", 
        			new String[]{ "UsagePoolWS,cyclePeriodValue,usagePool.error.cycle.period.must.be.positive.integer" });
    	}

    	if (null != usagePoolWs && null != usagePoolWs.getConsumptionActions() && !usagePoolWs.getConsumptionActions().isEmpty()) {
    		for(UsagePoolConsumptionActionWS consumptionAction : usagePoolWs.getConsumptionActions()) {
    			String percentage = consumptionAction.getPercentage();
    			String type = consumptionAction.getType();
                String notificationId = consumptionAction.getNotificationId();
                NotificationMediumType mediumType = consumptionAction.getMediumType();
                String productId = StringUtils.trim(consumptionAction.getProductId());
                ItemDTO item = new ItemDAS().findNow(toint(productId));
                boolean isGlobal = null != item ? item.isGlobal() : false;
    			if (!StringUtils.isEmpty(percentage)) {
					if (!StringUtils.isNumeric(percentage) || AttributeUtils.parseInteger(percentage) < 0 ||
							AttributeUtils.parseInteger(percentage) > 100) {
						throw new SessionInternalError("Usage Pool Consumption % must be an integer value greater than zero and less than or equal to 100",
								new String[]{ "UsagePoolWS,attribute_key,usagePool.error.attribute.key.must.be.positive.integer" });
					} else {
						if (null == type || (null != type && type.isEmpty())) {
							throw new SessionInternalError("Usage Pool Action is empty or missing",
									new String[]{ "UsagePoolWS,attribute_value,usagePool.error.attribute.value.empty"});
						} else if (type.equals(Constants.FUP_CONSUMPTION_NOTIFICATION)) {
							if (null == notificationId || mediumType == null) {
								throw new SessionInternalError("Usage Pool Action notification don't have a medium type and a notification id",
										new String[]{ "UsagePoolWS,notification,usagePool.error.attribute.notification.info.empty"});
							}
						} else if (type.equals(Constants.FUP_CONSUMPTION_FEE)) {
							if (StringUtils.isEmpty(productId) || !StringUtils.isNumeric(productId) ||
									AttributeUtils.parseInteger(productId) < 0) {
								throw new SessionInternalError("Usage Pool Action Fee Product Id has to be > 0",
										new String[]{ "UsagePoolWS,productId,usagePool.error.attribute.fee.empty"});
							} else if(!new ItemDAS().isProductAvailableToCompany (toint(productId), usagePoolWs.getEntityId()) && !isGlobal) {
								//TODO Replace above condition with company hierarchies validation,
								//ItemDAS.isProductAvailableToCompany (productId, usagePoolWs.getEntityId())
								throw new SessionInternalError("The product for this id doesn't exist ",
										new String[]{ "UsagePoolWS,productId,usagePool.error.attribute.product.not.exist,"+ productId});
							}
						}
					}	
				}
    		}
    	}

    	//Check if the reserved instance has the monthly cycle
    	if(usagePoolWs.getUsagePoolResetValue().equals(UsagePoolResetValueEnum.HOURS_PER_CALENDER_MONTH.toString()) && !usagePoolWs.getCyclePeriodUnit().equals(Constants.USAGE_POOL_CYCLE_PERIOD_MONTHS)){

			throw new SessionInternalError("Reset Value: Hours Per Calendar Month, Is Valid Only For Monthly cycle.",
					new String[]{ "Reset.Value:'Hours.Per.Calender.Month'.Is.Valid.Only.For.Monthly.cycle" });
		}
    }
    
    /**
     * Method that makes sure usage pool name is not null or blank.
     * @param usagePoolWs
     * @throws SessionInternalError
     */
    private void checkRequirdUsagePoolName(UsagePoolWS usagePoolWs) throws SessionInternalError {
    	
    	List<InternationalDescriptionWS> names = usagePoolWs.getNames();
    	boolean hasAtleastOneName = false;
    	for (InternationalDescriptionWS name : names) {
    		 if (name.getContent() != null && !name.getContent().trim().equals("")) {
    			 hasAtleastOneName = true;
    			 break;
       		 }
    	}
    	
    	if (!hasAtleastOneName) {
    		 throw new SessionInternalError("Usage Pool Name Requird", 
		    			new String[]{ "UsagePoolWS,name,name.empty" });	
    	}
    	
    	// check if all names are to delete
        boolean hasAtleastOneNonDeletedName = false;
        for (InternationalDescriptionWS name : names) {
            if (!name.isDeleted()) {
            	hasAtleastOneNonDeletedName = true;
                break;
            }
        }
     
        if (!hasAtleastOneNonDeletedName) {
        	 throw new SessionInternalError("Usage Pool Name Requird", 
		    			new String[]{ "UsagePoolWS,name,name.empty" });
        }
        
    }
    
    /**
     * Method that checks for duplicate usage pool name. This method can be called
     * while creating or updating a usage pool. If duplicate name is found, it raises
     * SessionInternalError exception with appropriate error message.
     * @param usagePoolWs
     * @param usagePoolDto
     * @throws SessionInternalError
     */
    private void checkDuplicateUsagePoolName(UsagePoolWS usagePoolWs, UsagePoolDTO usagePoolDto) throws SessionInternalError {
    	
    	boolean isDuplicateUsagePoolName = false;
    	UsagePoolDAS usagePoolDas = new UsagePoolDAS();
    			
    	if (usagePoolDto.getId() == 0) {
    		// Its a create call
	    	for (InternationalDescriptionWS name : usagePoolWs.getNames()) {
	    		Collection<InternationalDescriptionDTO> names = 
	    				usagePoolDas.nameExists(Constants.TABLE_USAGE_POOL, 
	    											  "name", 
	    											  name.getContent()!= null ? name.getContent().trim():"", 
	    											  name.getLanguageId(),
	    											  usagePoolDto.getEntityId());
	    		if (names != null && !names.isEmpty()) {
	    			isDuplicateUsagePoolName = true;
	    			break;
	    		}
	    	}
    	} else {
    		// its an update
    		for (InternationalDescriptionWS name : usagePoolWs.getNames()) {
	    		Collection<InternationalDescriptionDTO>  names = 
	    				usagePoolDas.nameExists(Constants.TABLE_USAGE_POOL, 
													  "name", 
													  name.getContent()!= null ? name.getContent().trim():"",
													  name.getLanguageId(),
													  usagePoolDto.getEntityId(),
													  usagePoolDto.getId());
	    		if (names != null && !names.isEmpty()) {
	    			isDuplicateUsagePoolName = true;
	    			break;
	    		}
	    	}
    	}
    	
    	if (isDuplicateUsagePoolName) {
	    	String[] errmsgs= new String[1];
			errmsgs[0]= "UsagePoolWS,name,usagePool.error.name.already.exists.for.entity";
			throw new SessionInternalError("The usagePool name is duplicate.", errmsgs);
    	}
    }
    
    /**
     * Persists the usage pool, after getting the WS object and converting it into appropriate dto object.
     * The same method can be used to create a new usage pool or update an existing one. 
     * @param usagePoolWs
     * @param languageId
     * @return usage pool id
     * @throws SessionInternalError
     */
    public Integer createOrUpdate(UsagePoolWS usagePoolWs,  Integer languageId) 
    throws SessionInternalError{
    	
    	// validations
    	validateUsagePoolWS(usagePoolWs);
    	checkRequirdUsagePoolName(usagePoolWs);

    	UsagePoolDTO usagePoolDto = getCreateOrUpdateDTO(usagePoolWs);
    	
    	checkDuplicateUsagePoolName(usagePoolWs, usagePoolDto);
    	    	
    	usagePoolDto = new UsagePoolDAS().save(usagePoolDto);
    	
    	// save-delete descriptions
    	for (InternationalDescriptionWS name : usagePoolWs.getNames()) {
           if (name.getLanguageId() != null) {
               if (name.isDeleted()) {
               	usagePoolDto.deleteDescription(name.getLanguageId());
               } else {
            	   if (null != name.getContent() && !name.getContent().trim().equals("")) {
            		   usagePoolDto.setDescription(name.getContent(), name.getLanguageId());
            	   }
               }
           }
        }
   	    	
    	return usagePoolDto != null ? usagePoolDto.getId() : null;
    }
    
    /**
     * Method that takes a usage pool ws object and returns a usage pool dto that can be persisted.
     * The same method can be used for getting a dto in case of create and update.
     * In case of create, it simply creates a new instance of the UsagePoolDTO, whereas,
     * in case of update, it finds the UsagePoolDTO by id and sets it in the hibernate context for update.
     * @param usagePoolWs
     * @return
     */
    private UsagePoolDTO getCreateOrUpdateDTO(UsagePoolWS usagePoolWs) {
    	UsagePoolDTO usagePoolDto = null;
    	
    	if (usagePoolWs.getId() > 0 ) {
    		usagePoolDto = new UsagePoolDAS().findForUpdate(usagePoolWs.getId());
    	} else {
    		usagePoolDto = new UsagePoolDTO();
    	}
    	
    	if (usagePoolWs.getEntityId()!=null){
    		usagePoolDto.setEntity(new CompanyDAS().find(usagePoolWs.getEntityId()));
    	}
    	
    	usagePoolDto.setCyclePeriodUnit(usagePoolWs.getCyclePeriodUnit());
    	usagePoolDto.setCyclePeriodValue(usagePoolWs.getCyclePeriodValue());
    	
    	usagePoolDto.getItems().clear();
    	if (null != usagePoolWs.getItems()) {
    		ItemBL itemBl = new ItemBL();
    		for (Integer itemId : usagePoolWs.getItems()) {
    			usagePoolDto.getItems().add(new ItemDAS().find(itemId));
    		}
    	}
    	
    	usagePoolDto.getItemTypes().clear();
    	if (null != usagePoolWs.getItemTypes()) {
    		for (Integer itemTypeId : usagePoolWs.getItemTypes()) {
    			usagePoolDto.getItemTypes().add(new ItemTypeDAS().find(itemTypeId));
    		}
    	}
    	if (null != usagePoolWs.getQuantity() && usagePoolWs.getQuantity().length() > 0) {
    		usagePoolDto.setQuantity(new BigDecimal(usagePoolWs.getQuantity()));
    	}
    	if (null != usagePoolWs.getPrecedence()) {
    		usagePoolDto.setPrecedence(usagePoolWs.getPrecedence());
    	}
    	if (null != usagePoolWs.getUsagePoolResetValue() && usagePoolWs.getUsagePoolResetValue().length() > 0) {
    		usagePoolDto.setUsagePoolResetValue(UsagePoolResetValueEnum.getUsagePoolResetValueEnumByValue(usagePoolWs.getUsagePoolResetValue()));
    	}
    	
    	if (usagePoolWs.getId() == 0) {
			usagePoolDto.setCreatedDate(TimezoneHelper.serverCurrentDate());
		}
    
        Iterator<UsagePoolConsumptionActionDTO> consumptionActionAlreadyOnDto = usagePoolDto.getConsumptionActions().iterator();
        Set<UsagePoolConsumptionActionDTO> consumptionActionsNew = getConsumptionActions(usagePoolDto, usagePoolWs);

        while (consumptionActionAlreadyOnDto.hasNext()) {
            UsagePoolConsumptionActionDTO consumptionActionDTO =  consumptionActionAlreadyOnDto.next();
            UsagePoolConsumptionActionDTO consumptionActionInList = getConsumptionActionInList(consumptionActionDTO, consumptionActionsNew);
            if (consumptionActionInList != null) {
                consumptionActionsNew.remove(consumptionActionInList);
            } else {
                consumptionActionAlreadyOnDto.remove();
            }
        }
        usagePoolDto.getConsumptionActions().addAll(consumptionActionsNew);

    	this.usagePool = usagePoolDto;
    	return usagePoolDto;
    }

    private Set<UsagePoolConsumptionActionDTO> getConsumptionActions(UsagePoolDTO usagePoolDto, UsagePoolWS usagePoolWs) {
        Set<UsagePoolConsumptionActionDTO> actionsDto = new HashSet<UsagePoolConsumptionActionDTO>();
        for (UsagePoolConsumptionActionWS actionWS: usagePoolWs.getConsumptionActions()) {
            actionsDto.add(new UsagePoolBL(usagePoolDto.getId()).getUsagePoolConsumptionDto(actionWS));
        }
        return actionsDto;
    }

    private UsagePoolConsumptionActionDTO getConsumptionActionInList(UsagePoolConsumptionActionDTO consumptionActionDTO,
                                                                     Set<UsagePoolConsumptionActionDTO> consumptionActions) {
        for (UsagePoolConsumptionActionDTO actionInList: consumptionActions) {
            if (actionInList.getId() == consumptionActionDTO.getId()) {
                return actionInList;
            }
        }
        return null;
    }

    /**
     * This method deletes the usage pool by the given usage pool id.
     * It first checks if the usage pool being deleted is being used on any of the plans.
     * If found to be used on plans, it raises SessionInternalError with message 
     * saying this usage pool is in use and cannot be deleted.
     * @return
     * @throws SessionInternalError
     */
    public boolean delete() throws SessionInternalError {
    	boolean retVal = !isDeletable();
    	try {
			usagePool.setItems(null);
			usagePool.setItemTypes(null);
	    	if (retVal) {
				new UsagePoolDAS().delete(this.usagePool);
			} else {
				throw new SessionInternalError("Usage Pool cannot be deleted.", new String[] {
											   "UsagePoolWS,id,usage.pool.in.use," + this.usagePool.getId()
				});
			}
    	}catch (Exception e) {
    		logger.debug("Exception while deleting usage pool: {}",e);
    		throw new SessionInternalError(e);
    	}

    	return retVal;
    }
    
    /**
     * Method that checks if usage pool is deletable or not. 
     * If the usage pool is in use on the plans, then it cannot be deleted.
     * @return Boolean
     */
    public boolean isDeletable(){
    	if (this.usagePool != null) {
    		return new PlanDAS().countPlansByUsagePoolId(this.usagePool.getId()) > 0 ||
				   new CustomerUsagePoolDAS().countCustomerUsagePoolsByUsagePoolId(this.usagePool.getId()) > 0;
    	}

    	return false;
    }
    
 }
