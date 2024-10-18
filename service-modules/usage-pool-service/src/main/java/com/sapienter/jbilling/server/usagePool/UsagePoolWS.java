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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.item.validator.NonNegative;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;

import org.hibernate.validator.constraints.NotEmpty;
import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * UsagePoolWS
 * The client side POJO for FUP.
 * @author Amol Gadre
 * @since 01-Dec-2013
 */

public class UsagePoolWS implements WSSecured, Serializable {
	
	public static final Integer DEFAULT_PRECEDENCE = -1;
	
	private int id;
	@NotNull(message = "validation.error.notnull")
    @NonNegative(message = "validation.error.nonnegative")
	@Digits(integer = 12, fraction = 10, message="validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class} )
	private String quantity;
	@NotNull(message = "validation.error.notnull")
	@Digits(integer = 3, fraction = 0, message="validation.error.invalid.digits.max.three", groups = {CreateValidationGroup.class, UpdateValidationGroup.class} )
	private Integer precedence = DEFAULT_PRECEDENCE;
	private String cyclePeriodUnit;
	private Integer cyclePeriodValue;
	private Integer[] itemTypes;
	private Integer[] items;
	private String usagePoolResetValue;
	private Integer entityId;
    @ConvertToTimezone
	private Date createdDate;
	@Size(min=1,max=50, message="validation.error.size,1,50")
    private String name = null;
	private Integer owningEntityId;
	@NotNull(message = "validation.error.notnull")
	@NotEmpty(message = "validation.error.notempty")
	private List<InternationalDescriptionWS> names = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));
	private List<UsagePoolConsumptionActionWS> consumptionActions = new ArrayList<UsagePoolConsumptionActionWS>();
    private SortedMap<String, String> attributes = new TreeMap<String, String>();


    public UsagePoolWS() {
		
	}
	
	/**
	 * Parametrized constructor that creates a new UsagePoolWS object instance 
	 * from the UsagePoolDTO object reference provided to it.
	 * @param ws
	 */
	
	public UsagePoolWS(UsagePoolWS ws) {
        this.id = ws.getId();
        this.quantity = ws.getQuantity();
		this.precedence = ws.getPrecedence();
		this.cyclePeriodUnit = ws.getCyclePeriodUnit();
		this.cyclePeriodValue = ws.getCyclePeriodValue();
		this.itemTypes = ws.getItemTypes();
		this.items = ws.getItems();
		this.usagePoolResetValue = ws.getUsagePoolResetValue();
		this.entityId = ws.getEntityId();
		this.createdDate = ws.getCreatedDate();
        this.consumptionActions = ws.getConsumptionActions();
    }
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getQuantity() {
		return quantity;
	}
	public void setQuantity(String quantity) {
		this.quantity = quantity;
	}
	public Integer getPrecedence() {
        return precedence;
    }
    public void setPrecedence(Integer precedence) {
        this.precedence = precedence;
    }
	public String getCyclePeriodUnit() {
		return cyclePeriodUnit;
	}
	public void setCyclePeriodUnit(String cyclePeriodUnit) {
		this.cyclePeriodUnit = cyclePeriodUnit;
	}
	public Integer getCyclePeriodValue() {
		return cyclePeriodValue;
	}
	public void setCyclePeriodValue(Integer cyclePeriodValue) {
		this.cyclePeriodValue = cyclePeriodValue;
	}
	public Integer[] getItemTypes() {
		return itemTypes;
	}
	public void setItemTypes(Integer[] itemTypes) {
		this.itemTypes = itemTypes;
	}
	public Integer[] getItems() {
		return items;
	}
	public void setItems(Integer[] items) {
		this.items = items;
	}
	public String getUsagePoolResetValue() {
		return usagePoolResetValue;
	}
	public void setUsagePoolResetValue(String usagePoolResetValue) {
		this.usagePoolResetValue = usagePoolResetValue;
	}
	public Integer getEntityId() {
		return entityId;
	}
	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	/**
	 * Given the language id, this method provides the usage pool 
	 * name in String form for that language.
	 * @param languageId
	 * @return String - usage pool name
	 */
	public String getUsagePoolNameByLanguageId(Integer languageId) {
		for(InternationalDescriptionWS name: names){
			if (name.getLanguageId().intValue() == languageId.intValue()) {
				return name.getContent();
			}
		}
		return null;
	}
	
	@Override
	public Integer getOwningEntityId() {
		return owningEntityId;
	}
	public void setOwningEntityId(Integer owningEntityId){
		this.owningEntityId = owningEntityId;
	}

	@Override
	public Integer getOwningUserId() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public List<InternationalDescriptionWS> getNames() {
        return names;
    }

    public void setNames(List<InternationalDescriptionWS> names) {
        this.names = names;
    }
    
    public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }

    public List<UsagePoolConsumptionActionWS> getConsumptionActions() {
        return consumptionActions;
    }

    public void setConsumptionActions(List<UsagePoolConsumptionActionWS> consumptionActions) {
        this.consumptionActions = consumptionActions;
    }

    public void addConsumptionActions(UsagePoolConsumptionActionWS consumptionActionDTO) {
        this.consumptionActions.add(consumptionActionDTO);
    }


    /**
     * This method sets the given String name as an InternationalDescriptionWS
     * in the English language. Method is particularly useful for creating 
     * usage pools from API when you just want to create a name in English.
     * The method is used extensively from test cases.
     * @param newName
     */
    public void setName(String newName) {
        name = newName;

        for (InternationalDescriptionWS name : names) {
            if (name.getLanguageId() == Constants.LANGUAGE_ENGLISH_ID) {
                name.setContent(newName);
                return;
            }
        }
        InternationalDescriptionWS newDescriptionWS = new InternationalDescriptionWS();
        newDescriptionWS.setContent(newName);
        newDescriptionWS.setPsudoColumn("description");
        newDescriptionWS.setLanguageId( Constants.LANGUAGE_ENGLISH_ID);
        names.add(newDescriptionWS);
    }
    
	@Override
	public String toString() {
		return "UsagePoolWS={name=" + names +
			",quantity=" + quantity + 
			", precedence=" + precedence +
			",cyclePeriodUnit=" + cyclePeriodUnit +
			",cyclePeriodValue=" + cyclePeriodValue + 
			",itemTypes=" + itemTypes +
			",items=" + items + 
			",usagePoolResetValue=" + usagePoolResetValue +
			",entity=" + entityId + 
			",createdDate=" + createdDate +
			",consumptionActionDTO=" + consumptionActions+
			"}";
	}


    public SortedMap<String, String> getAttributes() {
        return attributes;
    }
}
