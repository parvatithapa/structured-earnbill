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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.collections.FactoryUtils;
import org.apache.commons.collections.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sapienter.jbilling.server.item.validator.ItemTypes;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.metafields.MetaFieldWS;
import com.sapienter.jbilling.server.order.validator.DateRange;
import com.sapienter.jbilling.server.pricing.PriceModelWS;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.InternationalDescriptionWS;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.cxf.CxfSMapDatePriceModelAdapter;
import com.sapienter.jbilling.server.util.cxf.CxfSMapIntMetafieldsAdapter;
import com.sapienter.jbilling.server.util.json.deserilizers.PricesTimeLineDeserializer;
import com.sapienter.jbilling.server.util.json.serializers.PriceTimeLineSerializer;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@DateRange(start = "activeSince", end = "activeUntil", message = "validation.activeUntil.before.activeSince")
@ApiModel(value = "Item Data", description = "ItemDTOEx model")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ItemDTOEx implements WSSecured, HierarchicalEntity, Serializable {

    private static final long serialVersionUID = 20130704L;
    private final static int MAXIMUM_MINUTES_ALLOWED = 600000;
    private final static int MINIMUM_MINUTES_ALLOWED = 0;

    // ItemDTO
    private Integer id;
    @NotNull(message = "validation.error.notnull")
   	@Size(min=1,max=50, message="validation.error.size,1,50")
    private String number;
    @Size (min=0,max=50, message="validation.error.size,1,50")
    private String glCode;
    private Integer[] excludedTypes = null;
    private Integer hasDecimals;
    private Integer deleted;
    private Integer assetManagementEnabled = 0;
    private Integer entityId;

    // These prices are only entity specific or global
    private SortedMap<Date, PriceModelWS> defaultPrices = new TreeMap<Date, PriceModelWS>();
    private PriceModelWS defaultPrice;

    //@Size(min=1,max=50, message="validation.error.size,1,50")
    private String description = null;
    @ItemTypes
    private Integer[] types = null;
    private String promoCode = null;
    private Integer currencyId = null;
    @Digits(integer=12, fraction=10, message="validation.error.not.a.number")
    private String price = null;
    private Integer orderLineTypeId = null;
    @NotEmpty(message = "validation.error.notnull")
    private List<InternationalDescriptionWS> descriptions = ListUtils.lazyList(new ArrayList<InternationalDescriptionWS>(), FactoryUtils.instantiateFactory(InternationalDescriptionWS.class));
    @Valid
    private MetaFieldValueWS[] metaFields;
    @Valid
    private MetaFieldWS[] orderLineMetaFields;
    private SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap = new TreeMap<Integer, MetaFieldValueWS[]>();
    @Valid
    private ItemDependencyDTOEx[] dependencies;

    private boolean standardAvailability = true;
    private boolean global = false;
    private Integer[] accountTypes = null;

    private List<Integer> entities = new ArrayList<Integer>(0);
    private Integer priceModelCompanyId = null;
    @Range(min = 0, max = 100, message = "validation.error.invalid.percentage.value")
    private String standardPartnerPercentage;
    @Range(min = 0, max = 100, message = "validation.error.invalid.percentage.value")
    private String masterPartnerPercentage;

    private Date activeSince;
    private Date activeUntil;
    private boolean isPlan=false;

    @Range(min = MINIMUM_MINUTES_ALLOWED, max = MAXIMUM_MINUTES_ALLOWED, message = "reserve.duration.validation.message,{min},{max}")
    private Integer reservationDuration;

    @ApiModelProperty(value = "Return the rating config object contains with rating unit and rating scheme object")
    private SortedMap<Date,RatingConfigurationWS> ratingConfigurations= new TreeMap<Date, RatingConfigurationWS>();

    public ItemDTOEx() {
    }

    public ItemDTOEx(Integer id,String number, String glCode, Integer entity, String description,
                     Integer deleted, Integer currencyId, BigDecimal price,
                     Integer orderLineTypeId, Integer hasDecimals, Integer assetManagementEnabled) {

        this(id, number, glCode, hasDecimals, deleted, entity, assetManagementEnabled);
        setDescription(description);
        setCurrencyId(currencyId);
        setPrice(price);
        setOrderLineTypeId(orderLineTypeId);
    }

    public ItemDTOEx(Integer id, String number, String glCode, Integer hasDecimals,
                     Integer deleted, Integer entityId, Integer assetManagementEnabled) {
        this.id = id;
        this.number = number;
        this.glCode= glCode;
        this.hasDecimals = hasDecimals;
        this.deleted = deleted;
        this.entityId = entityId;
        this.assetManagementEnabled = assetManagementEnabled;
    }

    public ItemDTOEx(ItemDTOEx otherValue) {
        this.id = otherValue.id;
        this.number = otherValue.number;
        this.glCode = otherValue.glCode;
        this.hasDecimals = otherValue.hasDecimals;
        this.deleted = otherValue.deleted;
        this.entityId = otherValue.entityId;
        this.metaFields = otherValue.getMetaFields();
        this.assetManagementEnabled = otherValue.assetManagementEnabled;
        this.orderLineMetaFields = otherValue.getOrderLineMetaFields();
        this.standardPartnerPercentage = otherValue.standardPartnerPercentage;
        this.masterPartnerPercentage = otherValue.masterPartnerPercentage;
    }

   /* public ItemDTOEx(ItemDTO otherValue) {
        this.id = otherValue.getId();
        this.number = otherValue.getNumber();
        this.glCode = otherValue.getGlCode();
        this.hasDecimals = otherValue.getHasDecimals();
        this.deleted = otherValue.getDeleted();
        this.entityId = otherValue.getEntityId();
        if (null != otherValue.getMetaFields()) {
	        this.metaFields = new MetaFieldValueWS[otherValue.getMetaFields().size()];
	        int index = 0;
	        for (MetaFieldValue metaFieldValue : otherValue.getMetaFields()) {
	        	this.metaFields[index++] = MetaFieldBL.getWS(metaFieldValue);
	        }
        }
        this.assetManagementEnabled = otherValue.getAssetManagementEnabled();
        if (null != otherValue.getOrderLineMetaFields()) {
			this.orderLineMetaFields = new MetaFieldWS[otherValue.getOrderLineMetaFields().size()];
			int index=0;
			for (MetaField metaValue : otherValue.getOrderLineMetaFields()) {
				this.orderLineMetaFields[index++] = MetaFieldBL.getWS(metaValue);
			}
		}

        this.standardPartnerPercentage = null != otherValue.getStandardPartnerPercentage() ? otherValue.getStandardPartnerPercentage().toString() : "";
        this.masterPartnerPercentage = null != otherValue.getMasterPartnerPercentage() ? otherValue.getMasterPartnerPercentage().toString() : "";
        this.reservationDuration = otherValue.getReservationDuration();
    }*/

    @ApiModelProperty(value = "A flag that indicates asset management is enabled or not: 1 -  enabled; - 0 not enabled")
    public Integer getAssetManagementEnabled() {
        return assetManagementEnabled;
    }

    public void setAssetManagementEnabled(Integer assetManagementEnabled) {
        this.assetManagementEnabled = assetManagementEnabled;
    }

    @ApiModelProperty(value = "The id of the product entity")
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "This can be used to identify this item following an external coding system. For example, books can be identified by their ISBN codes", required = true)
    public String getNumber() {
        return this.number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @ApiModelProperty(value = "General Ledger code for integration with other systems like accounting systems")
    public String getGlCode() {
        return glCode;
    }

    public void setGlCode(String glCode) {
        this.glCode = glCode;
    }

    @JsonIgnore
    public String getMasterPartnerPercentage () {
        return this.masterPartnerPercentage;
    }

    @JsonProperty(value = "masterPartnerPercentage")
    @ApiModelProperty(value = "The identifier indicates that percentage of commission for partner type ‘Master’")
    public BigDecimal getMasterPartnerPercentageAsDecimal () {
        return masterPartnerPercentage != null ? new BigDecimal(masterPartnerPercentage) : null;
    }

    @JsonIgnore
    public void setMasterPartnerPercentageAsDecimal (BigDecimal percentage) {
        setMasterPartnerPercentage(percentage);
    }

    @JsonIgnore
    public void setMasterPartnerPercentage (String percentage) {
        this.masterPartnerPercentage = percentage;
    }

    @JsonProperty(value = "masterPartnerPercentage")
    public void setMasterPartnerPercentage (BigDecimal percentage) {
        this.masterPartnerPercentage = (percentage != null ? percentage.toString() : null);
    }

    @JsonIgnore
    public String getStandardPartnerPercentage () {
        return this.standardPartnerPercentage;
    }

    @JsonProperty(value = "standardPartnerPercentage")
    @ApiModelProperty(value = "The identifier indicates that percentage of commission for partner type ‘Standard’")
    public BigDecimal getStandardPartnerPercentageAsDecimal () {
        return standardPartnerPercentage != null ? new BigDecimal(standardPartnerPercentage) : null;
    }

    @JsonIgnore
    public void setStandardPartnerPercentageAsDecimal (BigDecimal percentage) {
        setStandardPartnerPercentage(percentage);
    }

    @JsonIgnore
    public void setStandardPartnerPercentage (String percentage) {
        this.standardPartnerPercentage = percentage;
    }

    @JsonProperty(value = "standardPartnerPercentage")
    public void setStandardPartnerPercentage (BigDecimal percentage) {
        this.standardPartnerPercentage = (percentage != null ? percentage.toString() : null);
    }

    @ApiModelProperty(value = "Array of product categories that this product is excluded from")
    public Integer[] getExcludedTypes() {
        return excludedTypes;
    }

    public void setExcludedTypes(Integer[] excludedTypes) {
        this.excludedTypes = excludedTypes;
    }

    @JsonProperty(value = "standardAvailability")
    @ApiModelProperty(value = "This is a field that goes together with the accountTypes field. If is true it means the product is available for all the account types. Otherwise, you need to select the account types (in the other field) that the product will be available for")
    public boolean isStandardAvailability() {
        return standardAvailability;
    }

    public void setStandardAvailability(boolean standardAvailability) {
        this.standardAvailability = standardAvailability;
    }

    @ApiModelProperty(value = "A list of the account types ids this product is available for. Remember customers belong to account types and sometimes belonging to an account type means the customer can't purchase all the items. So by linking a product to an account type you are limiting the list of products customers are able to purchase")
    public Integer[] getAccountTypes() {
        return accountTypes;
    }

    public void setAccountTypes(Integer[] accountTypes) {
        this.accountTypes = accountTypes;
    }

    @ApiModelProperty(value = "An internal flag indicating whether the item accepts decimal quantities. Can have the following values: 0 - No decimals, quantities are expressed as an integer; 1 - Decimals allowed in quantity values")
    public Integer getHasDecimals() {
        return this.hasDecimals;
    }

    public void setHasDecimals(Integer hasDecimals) {
        this.hasDecimals = hasDecimals;
    }

    @ApiModelProperty(value = "A flag that indicates if this record is logically deleted in the database. This allows for 'undo' of deletions. Valid values are: 0 - the record is not deleted; 1 - the record is considered deleted")
    public Integer getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "Identifier for the entity to which this item belongs")
    public Integer getEntityId() {
        return this.entityId;
    }

    public void setEntityId(Integer entityId) {
        this.entityId = entityId;
    }

    /**
     * Returns an english description.
     *
     * @return String
     */
    @JsonIgnore
    public String getDescription() {
        return getDescriptionForMultiLangBeans(descriptions);
    }

    /**
     * Sets the a description in english.
     *
     * @param newDescription
     *            The description to set
     */
    @JsonIgnore
    public void setDescription(String newDescription) {
        description = newDescription;
        	setDescriptionForMultiLangBeans(descriptions, newDescription);
    }

    @ApiModelProperty(value = "A list of type identifiers that indicates to which types (categories) this item belongs. An item must belong to at least one type")
    public Integer[] getTypes() {
        return types;
    }

    /*
     * Rules only work on collections of strings (operator contains)
     */
    @JsonIgnore
    public Collection<String> getStrTypes() {
        List<String> retValue = new ArrayList<String>();
        for (Integer i: types) {
            retValue.add(i.toString());
        }
        return retValue;
    }

    @JsonProperty(value = "types")
    public void setTypes(Integer[] vector) {
        types = vector;
    }

    @ApiModelProperty(value = "If this item is related to a promotion, this is the code that identifies the promotion")
    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String string) {
        promoCode = string;
    }

    @ApiModelProperty(value = "The order line type that this item will generate, such a 'taxes', or 'items'", allowableValues = "1 - Item, 2 - Tax, 3 - Penalty")
    public Integer getOrderLineTypeId() {
        return orderLineTypeId;
    }

    public void setOrderLineTypeId(Integer typeId) {
        orderLineTypeId = typeId;
    }

    @ApiModelProperty(value = "The id of the currency used")
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @JsonIgnore
    public String getPrice() {
        return price;
    }

    @JsonProperty(value = "price")
    @ApiModelProperty(value = "The price of this item or null if this is a percentage item")
    public BigDecimal getPriceAsDecimal() {
        return Util.string2decimal(price);
    }

    @JsonIgnore
    public void setPrice(String price) {
        this.price = price;
    }

    @JsonProperty(value = "price")
    public void setPrice(BigDecimal price) {
        setPrice((price != null ? price.toString() : null));
    }

    @ApiModelProperty(value = "Default price of the product for that date and with specified pricing strategy")
    @JsonSerialize(using = PriceTimeLineSerializer.class)
    @JsonDeserialize(using = PricesTimeLineDeserializer.class)
    @XmlJavaTypeAdapter(CxfSMapDatePriceModelAdapter.class)
    public SortedMap<Date, PriceModelWS> getDefaultPrices() {
        return defaultPrices;
    }

    public void setDefaultPrices(SortedMap<Date, PriceModelWS> defaultPrices) {
        this.defaultPrices = defaultPrices;
    }

    public void addDefaultPrice(Date date, PriceModelWS model) {
        this.defaultPrices.put(date, model);
    }

    @ApiModelProperty(value = "A default pricing model this Item follows")
    public PriceModelWS getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(PriceModelWS defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    @ApiModelProperty(value = "A list of meta field values")
	public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "This are meta fields defined in a product that will be filled in when adding the product to an order. The values are then linked to a specific order line")
    public MetaFieldWS[] getOrderLineMetaFields() {
        return orderLineMetaFields;
    }

    public void setOrderLineMetaFields(MetaFieldWS[] orderLineMetaFields) {
        this.orderLineMetaFields = orderLineMetaFields;
    }

    @ApiModelProperty(value = "This is a list of meta fields for the product. When creating a product you can have meta fields to fill in. Just like any other meta field in the system")
    @XmlJavaTypeAdapter(CxfSMapIntMetafieldsAdapter.class)
    public SortedMap <Integer, MetaFieldValueWS[]> getMetaFieldsMap() {
		return metaFieldsMap;
	}

	public void setMetaFieldsMap(SortedMap <Integer, MetaFieldValueWS[]> metaFieldsMap) {
		this.metaFieldsMap = metaFieldsMap;
	}

    @Override
	public Integer getOwningEntityId() {
        return getEntityId();
    }

    @ApiModelProperty(value = "List of descriptions of the product in the other languages", required = true)
	public List<InternationalDescriptionWS> getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(List<InternationalDescriptionWS> descriptions) {
        this.descriptions = descriptions;
    }

    @ApiModelProperty(value = "This is a list of dependencies defined for the product. So you can say that if you purchase product A you need to purchase 2 units of product B if not you won't be able to complete the order. You can make a dependency mandatory or not. You can even say if a dependency is for a specific product or any product belonging to a specific category.")
    public ItemDependencyDTOEx[] getDependencies() {
        return dependencies;
    }

    public void setDependencies(ItemDependencyDTOEx[] dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * From the dependencies extract those of type {@code type }
     *
     * @param type Type of dependecies to extract
     * @return
     */
    @JsonIgnore
    public ItemDependencyDTOEx[] getDependenciesOfType(ItemDependencyType type) {
        ArrayList<ItemDependencyDTOEx> result = new ArrayList<ItemDependencyDTOEx>();
        if(dependencies != null) {
            for(ItemDependencyDTOEx dependency : dependencies) {
                if(dependency.getType().equals(type)) {
                    result.add(dependency);
                }
            }
        }
        return result.toArray(new ItemDependencyDTOEx[result.size()]);
    }

    /**
     * From the dependencies extract the ids of those of type {@code type }
     *
     * @param type Type of dependecies to extract
     * @return
     */
    @JsonIgnore
    public Integer[] getDependencyIdsOfType(ItemDependencyType type) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(dependencies != null) {
            for(ItemDependencyDTOEx dependency : dependencies) {
                if(dependency.getType().equals(type)) {
                    result.add(dependency.getDependentId());
                }
            }
        }
        return result.toArray(new Integer[result.size()]);
    }

    /**
     * From the dependencies extract the ids of those of type {@code type }
     * which has a minimum required qty of 1
     *
     * @param type Type of dependecies to extract
     * @return
     */
    @JsonIgnore
    public Integer[] getMandatoryDependencyIdsOfType(ItemDependencyType type) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        if(dependencies != null) {
            for(ItemDependencyDTOEx dependency : dependencies) {
                if(dependency.getType().equals(type) && dependency.getMinimum() > 0) {
                    result.add(dependency.getDependentId());
                }
            }
        }
        return result.toArray(new Integer[result.size()]);
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningEntityId()}
     * @return null
     */
    @Override
    @JsonIgnore
    public Integer getOwningUserId() {
        return null;
    }

    @ApiModelProperty(value = "List of company ids to which the item is available")
	public List<Integer> getEntities() {
		return entities;
	}

	public void setEntities(List<Integer> entities) {
		this.entities = entities;
	}

	@ApiModelProperty(value = "Company specific price model id for this product. The price model stores the price/strategy and it is now company specific, besides it being date specific prior to company hierarchy feature")
	public Integer getPriceModelCompanyId() {
		return priceModelCompanyId;
	}

	public void setPriceModelCompanyId(Integer priceModelCompanyId) {
		this.priceModelCompanyId = priceModelCompanyId;
	}

	@JsonProperty(value = "global")
    @ApiModelProperty(value = "This is used in company hierarchies. If a product is marked as global, all child companies will be able to see it and use it. Otherwise it will be hidden")
	public boolean isGlobal() {
		return global;
	}

	public void setGlobal(boolean global) {
		this.global = global;
	}

	@ApiModelProperty(value = "Date when the item becomes available")
	public Date getActiveSince() {
		return activeSince;
	}

	public void setActiveSince(Date activeSince) {
		this.activeSince = activeSince;
	}

    @ApiModelProperty(value = "Date when the item stops being available")
	public Date getActiveUntil() {
		return activeUntil;
	}

	public void setActiveUntil(Date activeUntil) {
		this.activeUntil = activeUntil;
	}

	@ApiModelProperty(value = "The number of minutes the system will hold the reservation for the product")
    public Integer getReservationDuration() {
        return reservationDuration;
    }

    public void setReservationDuration(Integer reservationDuration) {
        this.reservationDuration = reservationDuration;
    }

    @ApiModelProperty(value = "Indicates if the item is a plan")
    public boolean getIsPlan() {
        return isPlan;
    }

    public void setIsPlan(boolean isPlan) {
        this.isPlan = isPlan;
    }



    public void addRatingConfiguration(Date date, RatingConfigurationWS ratingConfiguration) {
        this.ratingConfigurations.put(date, ratingConfiguration);
    }

    public SortedMap<Date, RatingConfigurationWS> getRatingConfigurations() {
        return ratingConfigurations;
    }

    public void setRatingConfigurations(SortedMap<Date, RatingConfigurationWS> ratingConfigurations) {
        this.ratingConfigurations = ratingConfigurations;
    }


    public boolean isIdentical(Object other) {
        if (other instanceof ItemDTOEx) {
            ItemDTOEx that = (ItemDTOEx) other;
            boolean lEquals = true;
            if (this.number == null) {
                lEquals = lEquals && (that.number == null);
            } else {
                lEquals = lEquals && this.number.equals(that.number);
            }
            if (this.glCode == null) {
                lEquals = lEquals && (that.glCode == null);
            } else {
                lEquals = lEquals && this.glCode.equals(that.glCode);
            }
            if (this.hasDecimals == null) {
                lEquals = lEquals && (that.hasDecimals == null);
            } else {
                lEquals = lEquals && this.hasDecimals.equals(that.hasDecimals);
            }
            if (this.deleted == null) {
                lEquals = lEquals && (that.deleted == null);
            } else {
                lEquals = lEquals && this.deleted.equals(that.deleted);
            }
            if (this.entityId == null) {
                lEquals = lEquals && (that.entityId == null);
            } else {
                lEquals = lEquals && this.entityId.equals(that.entityId);
            }
            if (this.masterPartnerPercentage == null) {
                lEquals = lEquals && (that.masterPartnerPercentage == null);
            } else {
                lEquals = lEquals && this.masterPartnerPercentage.equals(that.masterPartnerPercentage);
            }
            if (this.standardPartnerPercentage == null) {
                lEquals = lEquals && (that.standardPartnerPercentage == null);
            } else {
                lEquals = lEquals && this.standardPartnerPercentage.equals(that.standardPartnerPercentage);
            }

            return lEquals;
        } else {
            return false;
        }
    }

    //ToDo revisit this
    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;

        if (!(other instanceof ItemDTOEx))
            return false;

        ItemDTOEx that = (ItemDTOEx) other;
        boolean lEquals = true;
        if( this.id == null ) {
            lEquals = lEquals && ( that.id == null );
        } else {
            lEquals = lEquals && this.id.equals( that.id );
        }

        lEquals = lEquals && isIdentical(that);
        return lEquals;
    }

    @Override
    public int hashCode(){
        int result = 17;
        result = 37*result + ((this.id != null) ? this.id.hashCode() : 0);
        result = 37*result + ((this.number != null) ? this.number.hashCode() : 0);
        result = 37*result + ((this.glCode != null) ? this.glCode.hashCode() : 0);
        result = 37*result + ((this.hasDecimals != null) ? this.hasDecimals.hashCode() : 0);
        result = 37*result + ((this.deleted != null) ? this.deleted.hashCode() : 0);
        result = 37*result + ((this.entityId != null) ? this.entityId.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemDTOEx [currencyId=");
        builder.append(currencyId);
        builder.append(", deleted=");
        builder.append(deleted);
        builder.append(", description=");
        builder.append(description);
        builder.append(", entityId=");
        builder.append(entityId);
        builder.append(", entities=");
        builder.append(entities);
        builder.append(", hasDecimals=");
        builder.append(hasDecimals);
        builder.append(", id=");
        builder.append(id);
        builder.append(", number=");
        builder.append(number);
        builder.append(", glCode=");
        builder.append(glCode);
        builder.append(", orderLineTypeId=");
        builder.append(orderLineTypeId);
        builder.append(", price=");
        builder.append(price);
        builder.append(", promoCode=");
        builder.append(promoCode);
        builder.append(", types=");
        builder.append(Arrays.toString(types));
        builder.append(", excludedTypes=");
        builder.append(Arrays.toString(excludedTypes));
        builder.append(", dependencies=");
        builder.append(Arrays.toString(dependencies));
        builder.append(", masterAgentPercentage=");
        builder.append(masterPartnerPercentage);
        builder.append(", standardAgentPercentage=");
        builder.append(standardPartnerPercentage);
        builder.append(']');
        return builder.toString();
    }

    @JsonIgnore
    public  String getDescriptionForMultiLangBeans(List<InternationalDescriptionWS> descriptions) {
        for (InternationalDescriptionWS description : descriptions) {
            if (description != null &&
            		Constants.LANGUAGE_ENGLISH_ID.intValue() == description.getLanguageId()) {
                return description.getContent();
            }
        }
        return "";
    }

	public  void setDescriptionForMultiLangBeans(
			List<InternationalDescriptionWS> beanDescriptions,
			String newDescription) {
		for (InternationalDescriptionWS description : beanDescriptions) {
			if (description != null
					&& Constants.LANGUAGE_ENGLISH_ID.intValue() == description
							.getLanguageId()) {
				description.setContent(newDescription);
				return;
			}
		}
		beanDescriptions.add(new InternationalDescriptionWS("description",
				Constants.LANGUAGE_ENGLISH_ID, newDescription));
	}

    /**
     * Returns the list of jBilling Entity IDs within a hierarchy that have access to this object.
     *
     * @return list of entities that have access.
     */
    @Override
    @JsonIgnore
    public List<Integer> getAccessEntities() {
        return this.entities;
    }

    /**
     * Named differently to avoid name conflict with implementing entities.
     *
     * @return
     */
    @Override
    @JsonIgnore
    public Boolean ifGlobal() {
        return Boolean.valueOf(this.global);
    }

    @JsonIgnore
    public boolean isAssetEnabledItem() {
        Integer assetEnabled = getAssetManagementEnabled();
        return null!= assetEnabled && assetEnabled.intValue() == 1;
    }

}
