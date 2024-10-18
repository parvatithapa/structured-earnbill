package com.sapienter.jbilling.server.discount;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.cxf.CxfSMapStringStringAdapter;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;

@ApiModel(value = "DiscountWS", description = "DiscountWS model")
public class DiscountWS implements WSSecured, Serializable {

	public static final String ATTRIBUTE_WILDCARD = "*";
	
	private int id;
	private Integer entityId;
	@NotEmpty(message = "validation.error.notnull")
	@Size (min=1,max=20, message="validation.error.size,1,20")
	private String code;
	private String type;
	@NotNull(message = "validation.error.notnull")
	@Digits(integer=12, fraction=4, message = "validation.error.invalid.number.or.fraction.4.decimals")
	private String rate;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
	private Date startDate;
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
	private Date endDate;
	private SortedMap<String, String> attributes = new TreeMap<String, String>();
	private String description = null;
	
	@NotNull(message = "validation.error.notnull")
	@NotEmpty(message = "validation.error.notempty")
	private List<InternationalDescriptionWS> descriptions = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));

	private boolean applyToAllPeriods = false;

	public DiscountWS() {
		
	}
	
	@Override
    @JsonIgnore
	public Integer getOwningEntityId() {
		return getEntityId();
	}

	@Override
    @JsonIgnore
	public Integer getOwningUserId() {
		return null;
	}

    @ApiModelProperty(value = "Unique ID of the discount")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

    @ApiModelProperty(value = "Unique code of the discount")
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

    @ApiModelProperty(value = "DiscountStrategyType (ONE_TIME_AMOUNT, ONE_TIME_PERCENTAGE, RECURRING_PERIODBASED)")
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

    @ApiModelProperty(value = "​This required field is where you enter the value of the  discount. For an amount based discount, this is the amount of discount that will be applied to an order or a line. For a percentage discount, it is the percentage that will be applied on the discountable amount on an order or a line. The discount rate cannot be zero or negative; it should be a positive integer and can be a decimal amount. For example, an entry of 10.5 will give a percentage discount of 10.5% or a discount value of $10.50. This field is mandatory")
	public String getRate() {
		return rate;
	}

    @JsonIgnore
	public BigDecimal getRateAsDecimal() {
		return Util.string2decimal(rate);
	}

	public void setRate(String rate) {
		this.rate = rate;
	}
	
	public void setRate(BigDecimal rate) {
		this.rate = rate != null ? rate.toString() : null;
	}

    @ApiModelProperty(value = "This is the date the discount comes into effect and can be applied")
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

    @ApiModelProperty(value = "This is the date on which the discount expires and can no longer be applied")
	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public boolean isApplyToAllPeriods() {
		return applyToAllPeriods;
	}

	public void setApplyToAllPeriods(boolean applyToAllPeriods) {
		this.applyToAllPeriods = applyToAllPeriods;
	}

	@XmlJavaTypeAdapter(CxfSMapStringStringAdapter.class)
    @ApiModelProperty(value = "Name and value map of attributes used for the different types of discounts")
	public SortedMap<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(SortedMap<String, String> attributes) {
		this.attributes = attributes;
	}

	/**
	 *  Returns an english description.
     * 
     * @return String
     */
    @ApiModelProperty(value = "​This is the name of the discount. At least one description is required. The description can be in any language")
    public String getDescription() {
        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() == Constants.LANGUAGE_ENGLISH_ID.intValue()) {
                return description.getContent();
            }
        }
        return "";
    }

    /**
     * Sets the a description in english.
     * 
     * @param newDescription
     *            The description to set
     */
    public void setDescription(String newDescription) {
        description = newDescription;

        for (InternationalDescriptionWS description : descriptions) {
            if (description.getLanguageId() == Constants.LANGUAGE_ENGLISH_ID.intValue()) {
                description.setContent(newDescription);
                return;
            }
        }
        InternationalDescriptionWS newDescriptionWS = new InternationalDescriptionWS();
        newDescriptionWS.setContent(newDescription);
        newDescriptionWS.setPsudoColumn("description");
        newDescriptionWS.setLanguageId(Constants.LANGUAGE_ENGLISH_ID);
        descriptions.add(newDescriptionWS);
    }

    @ApiModelProperty(value = "List of descriptions in different languages for the discount. This field is mandatory")
    public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    @JsonIgnore
	public boolean isPeriodBased() {
		return this.getType().equals("RECURRING_PERIODBASED");
	}
	
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("DiscountWS [id=%s, entityId=%s, code=%s, type=%s, rate=%s, startDate=%s, endDate=%s, attributes=%s, description=%s, descriptions=%s, applyToAllPeriods=%s]",
                        	 id, entityId, code, type, rate, startDate, endDate, attributes, description, descriptions, applyToAllPeriods);
    }

    @ApiModelProperty(value = "Current company ID.")
	public Integer getEntityId() {
		return entityId;
	}

	public void setEntityId(Integer entityId) {
		this.entityId = entityId;
	}

    
}
