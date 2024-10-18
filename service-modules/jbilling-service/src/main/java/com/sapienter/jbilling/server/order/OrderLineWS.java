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

package com.sapienter.jbilling.server.order;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sapienter.jbilling.common.Util;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.validator.ConditionalNotNullConstraint;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.api.validation.CreateValidationGroup;
import com.sapienter.jbilling.server.util.api.validation.UpdateValidationGroup;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

import org.apache.commons.lang.ArrayUtils;

/**
 * @author Emil
 */
@ConditionalNotNullConstraint(item = "itemId", type = "typeId", message = "validation.error.missing.item.id")
@XmlAccessorType(XmlAccessType.FIELD)
@ApiModel(value = "Order line data", description = "OrderLineWS model")
@JsonIgnoreProperties(value = { "parentLine" })
public class OrderLineWS implements Serializable {

    private static final long serialVersionUID = 20130704L;

    private int id;
    private Integer orderId;
    private String amount; // use strings instead of BigDecimal for WS compatibility
    @NotNull(message = "validation.error.null.quantity")
    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class})
    private String quantity;
    @Digits(integer = 12, fraction = 10, message = "validation.error.not.a.number", groups = {CreateValidationGroup.class, UpdateValidationGroup.class})
    private String price;
    @ConvertToTimezone
    private Date createDatetime;
    private int deleted;
    private String description;
    private Integer versionNum;
    private String callIdentifier;
    private Boolean editable = null;
    private Integer[] assetIds;
    private Integer[] assetAssignmentIds;
    private String sipUri;

    @Valid
    private MetaFieldValueWS[] metaFields;

    //provisioning fields
    private Integer provisioningStatusId;
    private String provisioningRequestId;


    // other fields, non-persistent
    private String priceStr = null;
    private Integer typeId = null;
    private Boolean useItem = null;
    private Integer itemId = null;
    private OrderLineUsagePoolWS[] orderLineUsagePools = null;

    private Date startDate;
    private Date endDate;

    private String productCode;
    private Boolean isSwapPlanCondition = false;
    @Valid
    @XmlAttribute(name = "parentLineId")
    @XmlIDREF
    private OrderLineWS parentLine;

    @Valid
    @XmlElement(name = "childLine")
    @XmlIDREF
    private OrderLineWS[] childLines;

    @XmlAttribute
    @XmlID
    private String objectId;
    private boolean isPercentage =false;
    private boolean isAllowedToUpdateOrderChange = true;
    private OrderLineTierWS[] orderLineTiers = null;
    private Boolean isPlan = false;
    private Integer planId = null;

    private Long callCounter = 0L;

    @JsonIgnore
    public String getObjectId() {
        return objectId;
    }

    /**
     * Verifone - AdjustedPrice after applying line level discount.
     * Only for display
     */
    private String adjustedPrice;

    private ProvisioningCommandWS provisioningCommands[];

    @JsonIgnore
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    @ApiModelProperty(value = "Array of usage pools defined for the order line")
    public OrderLineUsagePoolWS[] getOrderLineUsagePools() {
        return orderLineUsagePools;
    }

    public void setOrderLineUsagePools(OrderLineUsagePoolWS[] orderLineUsagePools) {
        this.orderLineUsagePools = orderLineUsagePools;
    }

    public OrderLineWS() {
        objectId = UUID.randomUUID().toString();
    }

    public OrderLineWS(Integer id, Integer itemId, String description, BigDecimal amount, BigDecimal quantity,
                       BigDecimal price,
                       Date create, Integer deleted, Integer newTypeId, Boolean editable, Integer orderId,
                       Boolean useItem, Integer version,String callIdentifier, Integer provisioningStatusId, String provisioningRequestId, 
                       String productCode, Integer[] assetIds, MetaFieldValueWS[] metaFields, 
                       String sipUri, Long callCounter) {
        setId(id);
        setItemId(itemId);
        setDescription(description);
        setAmount(amount);
        setQuantity(quantity);
        setPrice(price);
        setCreateDatetime(create);
        setDeleted(deleted);
        setTypeId(newTypeId);
        setEditable(editable);
        setOrderId(orderId);
        setUseItem(useItem);
        setVersionNum(version);
        setCallIdentifier(callIdentifier);
        setProvisioningStatusId(provisioningStatusId);
        setProvisioningRequestId(provisioningRequestId);
        setAssetIds(assetIds);
        setSipUri(sipUri);
        setProductCode(productCode);
        setMetaFields(metaFields);
        setCallCounter(callCounter);
        objectId = UUID.randomUUID().toString();
    }

    public OrderLineWS(Integer id, Integer itemId, String description, BigDecimal amount, BigDecimal quantity,
            BigDecimal price, Date create, Integer deleted, Integer newTypeId, Boolean editable, Integer orderId,
            Boolean useItem, Integer version,String callIdentifier, Integer provisioningStatusId,
            String provisioningRequestId, OrderLineUsagePoolWS[] orderLineUsagePools, String productCode,
            Integer[] assetIds, MetaFieldValueWS[] metaFields, String sipUri, boolean isPercentage,
            boolean isPlan, Integer planId, Long callCounter) {
        setId(id);
        setItemId(itemId);
        setDescription(description);
        setAmount(amount);
        setQuantity(quantity);
        setPrice(price);
        setCreateDatetime(create);
        setDeleted(deleted);
        setTypeId(newTypeId);
        setEditable(editable);
        setOrderId(orderId);
        setUseItem(useItem);
        setVersionNum(version);
        setCallIdentifier(callIdentifier);
        setProvisioningStatusId(provisioningStatusId);
        setProvisioningRequestId(provisioningRequestId);
        setAssetIds(assetIds);
        setSipUri(sipUri);
        setProductCode(productCode);
        setOrderLineUsagePools(orderLineUsagePools);
        setMetaFields(metaFields);
        objectId = UUID.randomUUID().toString();
        setProductCode(productCode);
        setIsPercentage(isPercentage);
        setIsPlan(isPlan);
        setPlanId(planId);
        setCallCounter(callCounter);
    }

    public OrderLineWS(Integer id, Integer itemId, String description, BigDecimal amount, BigDecimal quantity,
                       BigDecimal price,
                       Date create, Integer deleted, Integer newTypeId, Boolean editable, Integer orderId,
                       Boolean useItem, Integer version, Integer provisioningStatusId, String provisioningRequestId) {
        setId(id);
        setItemId(itemId);
        setDescription(description);
        setAmount(amount);
        setQuantity(quantity);
        setPrice(price);
        setCreateDatetime(create);
        setDeleted(deleted);
        setTypeId(newTypeId);
        setEditable(editable);
        setOrderId(orderId);
        setUseItem(useItem);
        setVersionNum(version);
        setProvisioningStatusId(provisioningStatusId);
        setProvisioningRequestId(provisioningRequestId);
    }

    @ApiModelProperty(value = "Unique identifier of the type id of the order line")
    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    @ApiModelProperty(value = "Flag that if set the order line will take the price and description from the item")
    public Boolean getUseItem() {
        return useItem == null ? new Boolean(false) : useItem;
    }

    public void setUseItem(Boolean useItem) {
        this.useItem = useItem;
    }

    @ApiModelProperty(value = "Unique identifier of the item this order line refers to")
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    @JsonIgnore
    public String getAmount() {
        return amount;
    }

    @ApiModelProperty(value = "The total amount of the order line", dataType = "BigDecimal")
    @JsonProperty("amount")
    public BigDecimal getAmountAsDecimal() {
        return Util.string2decimal(amount);
    }

    @JsonIgnore
    public void setAmountAsDecimal(BigDecimal amount) {
        setAmount(amount);
    }

    @JsonIgnore
    public void setAmount(String amount) {
        this.amount = amount;
    }

    @JsonProperty("amount")
    public void setAmount(BigDecimal amount) {
        this.amount = (amount != null ? amount.toString() : null);
    }

    @ApiModelProperty(value = "Timestamp when this record is created")
    public Date getCreateDatetime() {
        return createDatetime;
    }

    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @ApiModelProperty(value = "Flag that indicates if this record is logically deleted in the database." +
            " 0 - not deleted; 1 - deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "Description of the order line")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Indicates whether this order line is editable or not")
    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    @ApiModelProperty(value = "Unique identifier of the order line", required = true)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the order containing the order line")
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @JsonIgnore
    public String getPrice() {
        return price;
    }

    @ApiModelProperty(value = "The price of one item part of the order line, or null if there is no related item",
            dataType = "BigDecimal")
    @JsonProperty("price")
    public BigDecimal getPriceAsDecimal() {
        return Util.string2decimal(price);
    }

    @JsonIgnore
    public void setPriceAsDecimal(BigDecimal price) {
        setPrice(price);
    }

    @JsonIgnore
    public void setPrice(String price) {
        this.price = price;
    }

    @JsonProperty("price")
    public void setPrice(BigDecimal price) {
        this.price = (price != null ? price.toString() : null);
    }

    @JsonIgnore
    public String getPriceStr() {
        return priceStr;
    }

    @JsonIgnore
    public String getQuantity() {
        return quantity;
    }

    @ApiModelProperty(value = "Quantity of the items included in the line, or null if a quantity doesn't apply",
            dataType = "BigDecimal")
    @JsonProperty("quantity")
    public BigDecimal getQuantityAsDecimal() {
        return Util.string2decimal(quantity);
    }

    @JsonIgnore
    public void setQuantityAsDecimal(BigDecimal quantity) {
        setQuantity(quantity);
    }

    @JsonIgnore
    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    @JsonIgnore
    public void setQuantity(Integer quantity) {
        setQuantity(new BigDecimal(quantity));
    }

    @JsonProperty("quantity")
    public void setQuantity(BigDecimal quantity) {
        this.quantity = (quantity != null ? quantity.toString() : null);
    }

    @JsonIgnore
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @ApiModelProperty(value = "Call Identifier")
    public String getCallIdentifier() {
        return callIdentifier;
    }

    public void setCallIdentifier(String callIdentifier) {
        this.callIdentifier = callIdentifier;
    }
    
    @ApiModelProperty(value = "Field used to track the destination number called in each order line" +
            " when doing a diameter request")
    public String getSipUri() {
        return sipUri;
    }

    public void setSipUri(String sipUri) {
        this.sipUri = sipUri;
    }

    /**
     * @return the provisioningStatusId
     */
    @ApiModelProperty(value = "Unique identifier of the provisioning status for the order line")
    public Integer getProvisioningStatusId() {
        return provisioningStatusId;
    }

    /**
     * @param provisioningStatusId the provisioningStatusId to set
     */
    public void setProvisioningStatusId(Integer provisioningStatusId) {
        this.provisioningStatusId = provisioningStatusId;
    }

    @JsonIgnore
    public boolean hasAssets() {
        return assetIds != null && assetIds.length > 0;
    }

    @JsonIgnore
    public boolean hasOrderLineTiers() {
        return ArrayUtils.isNotEmpty(getOrderLineTiers());
    }


    @ApiModelProperty(value = "Array of asset ids that are used in the order line")
    public Integer[] getAssetIds() {
        return assetIds;
    }

    public void setAssetIds(Integer[] assetIds) {
        this.assetIds = assetIds;
    }

    public void removeAsset(Integer id) {
        List ids = new ArrayList(Arrays.asList(assetIds));
        ids.remove(id);
        assetIds = (Integer[]) ids.toArray(new Integer[ids.size()]);
    }

    public void removeAllAssets(Integer id) {
        assetIds = new Integer[0];
        quantity = "0";
    }

    @ApiModelProperty(value = "Array of assets ids assigned to the order line")
    public Integer[] getAssetAssignmentIds() {
        return assetAssignmentIds;
    }

    public void setAssetAssignmentIds(Integer[] assetAssignmentIds) {
        this.assetAssignmentIds = assetAssignmentIds;
    }

    @ApiModelProperty(value = "Array of meta fields defined for the order line")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "Array of provisioning commands defined for the order line")
    public ProvisioningCommandWS[] getProvisioningCommands() {
        return provisioningCommands;
    }

    public void setProvisioningCommands(ProvisioningCommandWS[] provisioningCommands) {
        this.provisioningCommands = provisioningCommands;
    }

    /**
     * @return the provisioningRequestId
     */
    @ApiModelProperty(value = "Unique identifier of the provisioning request for the order line")
    public String getProvisioningRequestId() {
        return provisioningRequestId;
    }

    /**
     * @param provisioningRequestId the provisioningRequestId to set
     */
    public void setProvisioningRequestId(String provisioningRequestId) {
        this.provisioningRequestId = provisioningRequestId;
    }

    /**
     * Renturns true if the line has assets assigned to it
     *
     * @return
     */
    public boolean hasLinkedAssets() {
        return (assetIds != null && assetIds.length > 0);
    }

    @ApiModelProperty(value = "Product code for the item used in the order line")
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    @JsonIgnore
    public String getAdjustedPrice() {
        return adjustedPrice;
    }

    @JsonIgnore
    public void setAdjustedPrice(String adjustedPrice) {
        this.adjustedPrice = adjustedPrice;
    }

    @JsonProperty("adjustedPrice")
    public void setAdjustedPrice(BigDecimal adjustedPrice) {
        this.adjustedPrice = (adjustedPrice != null ? adjustedPrice.toString() : null);
    }

    @ApiModelProperty(value = "Adjusted price after applying line level discount")
    @JsonProperty("adjustedPrice")
    public BigDecimal getAdjustedPriceAsDecimal() {
        return Util.string2decimal(adjustedPrice);
    }

    @ApiModelProperty(value = "The parent order line for this one")
    public OrderLineWS getParentLine() {
        return parentLine;
    }

    public void setParentLine(OrderLineWS parentLine) {
        this.parentLine = parentLine;
    }

    @ApiModelProperty(value = "Array of child order lines of the current order line")
    public OrderLineWS[] getChildLines() {
        return childLines;
    }

    public void setChildLines(OrderLineWS[] childLines) {
        this.childLines = childLines;
    }

    public boolean isPercentage() {
        return isPercentage;
    }

    @ApiModelProperty(value = "Flag set if the order line is percentage of the total order")
    public boolean getIsPercentage() {
        return isPercentage;
    }

    public void setPercentage(boolean isPercentage) {
        this.isPercentage = isPercentage;
    }


    public boolean isAllowedToUpdateOrderChange() {
        return isAllowedToUpdateOrderChange;
    }

    public void setAllowedToUpdateOrderChange(boolean allowedToUpdateOrderChange) {
        isAllowedToUpdateOrderChange = allowedToUpdateOrderChange;
    }
    public void setIsPercentage(boolean isPercentage) {
        this.isPercentage = isPercentage;
    }


    @ApiModelProperty(value = "Is this a plan swap")
    public Boolean getIsSwapPlanCondition() {
        return isSwapPlanCondition;
    }

    public void setIsSwapPlanCondition(Boolean isSwapPlanCondition) {
        this.isSwapPlanCondition = isSwapPlanCondition;
    }

    public OrderLineTierWS[] getOrderLineTiers() {
        return orderLineTiers;
    }

    public void setOrderLineTiers(OrderLineTierWS[] orderLineTiers) {
        this.orderLineTiers = orderLineTiers;
    }

    public Boolean getIsPlan() {
        return isPlan;
    }

    public void setIsPlan(Boolean isPlan) {
        this.isPlan = isPlan;
    }

    public Integer getPlanId() {
        return planId;
    }

    public void setPlanId(Integer planId) {
        this.planId = planId;
    }

    public Long getCallCounter() {
        return callCounter;
    }

    public void setCallCounter(Long callCounter) {
        this.callCounter = callCounter;
    }

    @Override 
    public String toString() {

        return "OrderLineWS{"
                + "id=" + id
                +", orderLineUsagePools="+orderLineUsagePools
                + ", amount='" + amount + '\''
                + ", quantity='" + quantity + '\''
                + ", price='" + price + '\''
                + ", deleted=" + deleted
                + ", description='" + description + '\''
                + ", useItem=" + useItem
                + ", isPercentage=" + isPercentage
                + ", itemId=" + itemId
                + ", typeId=" + typeId
                + ", parentLineId=" + (parentLine != null ? parentLine.getId() : null)
                + ", callIdentifier=" + callIdentifier
                + ", metaFields=" + ((metaFields == null) ? "null" : Arrays.asList(metaFields))
                + ", orderLineTiers=" + getOrderLineTiersString()
                + '}';
    }

    private String getOrderLineTiersString(){
        String str = null;
        if (hasOrderLineTiers()){
            for(OrderLineTierWS orderLineTierWS : getOrderLineTiers()){
                str = str + orderLineTierWS.toString();
            }
        }else{
            str = "[]";
        }
        return str;
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof OrderLineWS)) {
            return false;
        }
        OrderLineWS orderLine = (OrderLineWS) object;
        return (this.id == orderLine.id) &&
                Util.decimalEquals(this.getQuantityAsDecimal(), orderLine.getQuantityAsDecimal()) &&
                Util.decimalEquals(this.getPriceAsDecimal(), orderLine.getPriceAsDecimal()) &&
                Util.decimalEquals(this.getAmountAsDecimal(), orderLine.getAmountAsDecimal()) &&
                nullSafeEquals(this.orderId, orderLine.orderId) &&
                (this.deleted == orderLine.deleted) &&
                nullSafeEquals(this.description, orderLine.description) &&
                nullSafeEquals(this.createDatetime, orderLine.createDatetime) &&
                nullSafeEquals(this.editable, orderLine.editable) &&
                nullSafeEquals(this.assetIds, orderLine.assetIds) &&
                nullSafeEquals(this.assetAssignmentIds, orderLine.assetAssignmentIds) &&
                nullSafeEquals(this.sipUri, orderLine.sipUri) &&
                nullSafeEquals(this.metaFields, orderLine.metaFields) &&
                nullSafeEquals(this.typeId, orderLine.typeId) &&
                nullSafeEquals(this.useItem, orderLine.useItem) &&
                nullSafeEquals(this.itemId, orderLine.itemId) &&
                nullSafeEquals(this.orderLineUsagePools, orderLine.orderLineUsagePools) &&
                nullSafeEquals(this.productCode, orderLine.productCode) &&
                nullSafeEquals(this.parentLine == null ? 0 : this.parentLine.getId(), orderLine.parentLine == null ? 0 : orderLine.parentLine.getId()) &&
                nullSafeEquals(this.childLines, orderLine.childLines) &&
                (this.isPercentage == orderLine.isPercentage);
    }

    @Override
    public int hashCode() {

        int result = id;
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getQuantityAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getPriceAsDecimal()));
        result = 31 * result + nullSafeHashCode(orderId);
        result = 31 * result + deleted;
        result = 31 * result + nullSafeHashCode(description);
        result = 31 * result + nullSafeHashCode(createDatetime);
        result = 31 * result + nullSafeHashCode(editable);
        result = 31 * result + nullSafeHashCode(assetIds);
        result = 31 * result + nullSafeHashCode(assetAssignmentIds);
        result = 31 * result + nullSafeHashCode(sipUri);
        result = 31 * result + nullSafeHashCode(metaFields);
        result = 31 * result + nullSafeHashCode(typeId);
        result = 31 * result + nullSafeHashCode(useItem);
        result = 31 * result + nullSafeHashCode(itemId);
        result = 31 * result + nullSafeHashCode(orderLineUsagePools);
        result = 31 * result + nullSafeHashCode(productCode);
        result = 31 * result + nullSafeHashCode(this.parentLine == null ? 0 : this.parentLine.getId());
        result = 31 * result + nullSafeHashCode(childLines);
        result = 31 * result + (isPercentage ? 1 : 0);
        return result;
    }
}
