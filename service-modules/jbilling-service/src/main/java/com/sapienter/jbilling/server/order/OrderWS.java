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
import com.sapienter.jbilling.server.discount.DiscountLineWS;
import com.sapienter.jbilling.server.invoice.InvoiceWS;
import com.sapienter.jbilling.server.metafields.MetaFieldValueWS;
import com.sapienter.jbilling.server.order.validator.DateBetween;
import com.sapienter.jbilling.server.order.validator.DateRange;
import com.sapienter.jbilling.server.security.HierarchicalEntity;
import com.sapienter.jbilling.server.security.WSSecured;
import com.sapienter.jbilling.server.timezone.ConvertToTimezone;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandWS;
import com.wordnik.swagger.annotations.ApiModel;
import com.wordnik.swagger.annotations.ApiModelProperty;

import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.*;

import static org.springframework.util.ObjectUtils.nullSafeEquals;
import static org.springframework.util.ObjectUtils.nullSafeHashCode;

/**
 * @author Emil
 */

@DateRange(start = "activeSince", end = "activeUntil", message = "validation.activeUntil.before.activeSince")
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
@ApiModel(value = "Order data", description = "OrderWS model")
@JsonIgnoreProperties(value = { "parentOrder" })
public class OrderWS implements WSSecured, Serializable, HierarchicalEntity {

    private static final long serialVersionUID = 20130704L;

    private Integer id;
    private Integer statusId;
    @NotNull(message = "validation.error.null.user.id")
    private Integer userId = null;
    @NotNull(message = "validation.error.null.currency")
    private Integer currencyId = null;
    @NotNull(message = "validation.error.null.billing.type")
    private Integer billingTypeId;
    @NotNull(message = "validation.error.null.period")
    private Integer period = null;
    @ConvertToTimezone
    private Date createDate;
    private Integer createdBy;
    @NotNull(message = "validation.error.null.activeSince")
    @DateBetween(start = "01/01/1901", end = "12/31/9999")
    private Date activeSince;
    private Date activeUntil;
    private Date nextBillableDay;
    private int deleted;
    private Integer notify;
    private Date lastNotified;
    private Integer notificationStep;
    private Integer dueDateUnitId;
    private Integer dueDateValue;
    private Integer dfFm;
    private Integer anticipatePeriods;
    private Integer ownInvoice;
    private String notes;
    private Integer notesInInvoice;
    @Valid
    private OrderLineWS orderLines[] = new OrderLineWS[0];
    private DiscountLineWS discountLines[] = null;
    private String pricingFields = null;
    private InvoiceWS[] generatedInvoices= null;
    @Valid
    private MetaFieldValueWS[] metaFields;
    @Valid
    private OrderWS parentOrder;
    @Valid
    private OrderWS[] childOrders;

    private String userCode;

    //Verifone - get PlanBundledItems with adjustedPrices
    private OrderLineWS[] planBundleItems = null;

    private ProvisioningCommandWS[] provisioningCommands = null;
    private List<Integer> accessEntities;

    /**
     * Verifone - AdjustedTotal after applying order level discount.
     */
    private String adjustedTotal;
    
    // balances
    private String total;

    // textual descriptions
    private String statusStr = null;
    private String timeUnitStr = null;
    private String periodStr = null;
    private String billingTypeStr = null;
    private Boolean prorateFlag = Boolean.FALSE;
    private boolean isDisable= false;
    private Integer customerBillingCycleUnit;
    private Integer customerBillingCycleValue;
    private String proratingOption;

    // optlock (not necessary)
    private Integer versionNum;
    private String cancellationFeeType;
    private Integer cancellationFee;
    private Integer cancellationFeePercentage;
    private Integer cancellationMaximumFee;
    private Integer cancellationMinimumPeriod;

    private String objectId;
    private String freeUsageQuantity;
    private Boolean prorateAdjustmentFlag = Boolean.FALSE;

    private Boolean autoRenew = Boolean.FALSE;
    private Integer renewNotification = 1;
    private Boolean isMediated = Boolean.FALSE;
    private Map<String,String> orderMetaFieldMap;

    @ApiModelProperty(value = "Order created by mediation")
    public Boolean getIsMediated() {
        return isMediated;
    }

    public void setIsMediated(Boolean isMediated) {
        this.isMediated = isMediated;
    }
    

    @JsonIgnore
	public String getObjectId() {
        return objectId;
    }

    @XmlAttribute @XmlID
    @JsonIgnore
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    private Integer primaryOrderId; 
    private OrderStatusWS orderStatusWS;

    public OrderWS() {
        objectId = UUID.randomUUID().toString();
    }

    public OrderWS(Integer id, Integer billingTypeId, Integer notify, Date activeSince, Date activeUntil,
		            Date createDate, Date nextBillableDay, Integer createdBy,Integer statusId, OrderStatusWS orderStatusWS, Integer deleted,
		            Integer currencyId, Date lastNotified, Integer notifStep, Integer dueDateUnitId, Integer dueDateValue,
		            Integer anticipatePeriods, Integer dfFm, String notes, Integer notesInInvoice, Integer ownInvoice,
		            Integer period, Integer userId, Integer version, BigDecimal freeUsageQuantity,Boolean prorateFlag, Boolean prorateAdjustmentFlag) {
		 setId(id);
		 setBillingTypeId(billingTypeId);
		 setNotify(notify);
		 setActiveSince(activeSince);
		 setActiveUntil(activeUntil);
		 setAnticipatePeriods(anticipatePeriods);
		 setCreateDate(createDate);
		 setStatusId(statusId);
		 setNextBillableDay(nextBillableDay);
		 setCreatedBy(createdBy);
		 setOrderStatusWS(orderStatusWS);
		 setDeleted(deleted.shortValue());
		 setCurrencyId(currencyId);
		 setLastNotified(lastNotified);
		 setNotificationStep(notifStep);
		 setDueDateUnitId(dueDateUnitId);
		 setDueDateValue(dueDateValue);
		 setDfFm(dfFm);
		 setNotes(notes);
		 setNotesInInvoice(notesInInvoice);
		 setOwnInvoice(ownInvoice);
		 setPeriod(period);
		 setUserId(userId);
		 setVersionNum(version);
		 setFreeUsageQuantity(null != freeUsageQuantity ? freeUsageQuantity.toString() : "");
		 objectId = UUID.randomUUID().toString();
		 setProrateFlag(prorateFlag);
		 setProrateAdjustmentFlag(prorateAdjustmentFlag);
    }

    @ApiModelProperty(value = "Order status for the order")
    public OrderStatusWS getOrderStatusWS() {
		return orderStatusWS;
	}

	public void setOrderStatusWS(OrderStatusWS orderStatusWS) {
		this.orderStatusWS = orderStatusWS;
	}

    @ApiModelProperty(value = "An array of invoices generated generated from the order")
	public InvoiceWS[] getGeneratedInvoices() {
		return generatedInvoices;
	}

	public void setGeneratedInvoices(InvoiceWS[] generatedInvoices) {
		this.generatedInvoices = generatedInvoices;
	}

    @ApiModelProperty(value = "Unique identifier of the order", required = true)
	public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ApiModelProperty(value = "Unique identifier of the order status")
    public Integer getStatusId() {
        return statusId;
    }

    public void setStatusId(Integer statusId) {
        this.statusId = statusId;
    }

    @ApiModelProperty(value = "User code for the user to whom this order belongs")
    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    @ApiModelProperty(value = "Unique identifier of the user to whom the order belongs")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "Unique identifier of the currency used in the order")
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "Indicates if this order is to be paid for before or after the service is provided." +
            " 1 - PRE-PAID; 2 - POST-PAID")
    public Integer getBillingTypeId() {
        return billingTypeId;
    }

    public void setBillingTypeId(Integer billingTypeId) {
        this.billingTypeId = billingTypeId;
    }

    @ApiModelProperty(value = "Unique identifier for the period of the order")
    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    @ApiModelProperty(value = "Timestamp when the order was originally created")
    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    @ApiModelProperty(value = "Unique identifier of the user that created the order")
    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    @ApiModelProperty(value = "Date when this order will start being active")
    public Date getActiveSince() {
        return activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    @ApiModelProperty(value = "Date when this order will stop being active")
    public Date getActiveUntil() {
        return activeUntil;
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    @ApiModelProperty(value = "Date when this order should generate a new invoice")
    public Date getNextBillableDay() {
        return nextBillableDay;
    }

    public void setNextBillableDay(Date nextBillableDay) {
        this.nextBillableDay = nextBillableDay;
    }

    @ApiModelProperty(value = "Flag that indicates if this record is logically deleted in the database." +
            " 0 - not deleted; 1 - deleted")
    public int getDeleted() {
        return deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @ApiModelProperty(value = "A flag to indicate if this order will generate notification as the 'activeSince' date approaches")
    public Integer getNotify() {
        return notify;
    }

    public void setNotify(Integer notify) {
        this.notify = notify;
    }

    @ApiModelProperty(value = "When the last expiration notification was sent")
    public Date getLastNotified() {
        return lastNotified;
    }

    public void setLastNotified(Date lastNotified) {
        this.lastNotified = lastNotified;
    }

    @ApiModelProperty(value = "What step has been completed in the order notifications")
    public Integer getNotificationStep() {
        return notificationStep;
    }

    public void setNotificationStep(Integer notificationStep) {
        this.notificationStep = notificationStep;
    }

    @ApiModelProperty(value = "Unique identifier of the unit for the due date, if there is one")
    public Integer getDueDateUnitId() {
        return dueDateUnitId;
    }

    public void setDueDateUnitId(Integer dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    @ApiModelProperty(value = "Value of the unit for the due date, if there is one")
    public Integer getDueDateValue() {
        return dueDateValue;
    }

    public void setDueDateValue(Integer dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    @ApiModelProperty(value = "Only used for specific Italian business rules")
    public Integer getDfFm() {
        return dfFm;
    }

    public void setDfFm(Integer dfFm) {
        this.dfFm = dfFm;
    }

    @ApiModelProperty(value = "How many periods in advance the order should invoice for")
    public Integer getAnticipatePeriods() {
        return anticipatePeriods;
    }

    public void setAnticipatePeriods(Integer anticipatePeriods) {
        this.anticipatePeriods = anticipatePeriods;
    }

    @ApiModelProperty(value = "A flag to indicate if this order should generate an invoice on its own")
    public Integer getOwnInvoice() {
        return ownInvoice;
    }

    public void setOwnInvoice(Integer ownInvoice) {
        this.ownInvoice = ownInvoice;
    }

    @ApiModelProperty(value = "Notes for the order")
    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @ApiModelProperty(value = "1 - the order notes will be included in the invoice; 0 - they will not be included")
    public Integer getNotesInInvoice() {
        return notesInInvoice;
    }

    public void setNotesInInvoice(Integer notesInInvoice) {
        this.notesInInvoice = notesInInvoice;
    }

    @ApiModelProperty(value = "Array of the order lines belonging to this order")
    public OrderLineWS[] getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(OrderLineWS[] orderLines) {
        this.orderLines = orderLines;
    }

    @ApiModelProperty(value = "Array of discount lines that indicate the discounts applied to this order")
    public DiscountLineWS[] getDiscountLines() {
        return discountLines;
    }

    public void setDiscountLines(DiscountLineWS[] discountLines) {
        this.discountLines = discountLines;
    }

    public boolean hasDiscountLines() {
    	return this.getDiscountLines() != null && this.getDiscountLines().length > 0;
    }

    @ApiModelProperty(value = "An array of pricing fields encoded as a String")
    public String getPricingFields() {
        return pricingFields;
    }

    public void setPricingFields(String pricingFields) {
        this.pricingFields = pricingFields;
    }

    @JsonIgnore
    public String getTotal() {
        return total;
    }

    @XmlTransient
    @ApiModelProperty(value = "Sum-total of all the order lines of this order", dataType = "BigDecimal")
    @JsonProperty("total")
    public BigDecimal getTotalAsDecimal() {
        return Util.string2decimal(total);
    }

    @JsonIgnore
    public void setTotal(String total) {
        this.total = total;
    }

    @JsonProperty("total")
    public void setTotal(BigDecimal total) {
        this.total = (total != null ? total.toString() : null);
    }

    @ApiModelProperty(value = "Description of the current order status")
    public String getStatusStr() {
        return statusStr;
    }

    public void setStatusStr(String statusStr) {
        this.statusStr = statusStr;
    }

    @ApiModelProperty(value = "Description of the time unit used for billable periods")
    public String getTimeUnitStr() {
        return timeUnitStr;
    }

    public void setTimeUnitStr(String timeUnitStr) {
        this.timeUnitStr = timeUnitStr;
    }

    @ApiModelProperty(value = "String description of the order period")
    public String getPeriodStr() {
        return periodStr;
    }

    public void setPeriodStr(String periodStr) {
        this.periodStr = periodStr;
    }

    @ApiModelProperty(value = "String description of the order billing type")
    public String getBillingTypeStr() {
        return billingTypeStr;
    }

    public void setBillingTypeStr(String billingTypeStr) {
        this.billingTypeStr = billingTypeStr;
    }

    @ApiModelProperty(value = "An identifier to enable or disable prorating")
    public Boolean getProrateFlag() {
		return prorateFlag;
	}

	public void setProrateFlag(Boolean prorateFlag) {
		this.prorateFlag = prorateFlag;
	}

    @ApiModelProperty(value = "Order was adjusted")
	public Boolean getProrateAdjustmentFlag() {
		return prorateAdjustmentFlag;
	}

	public void setProrateAdjustmentFlag(Boolean prorateAdjustmentFlag) {
		this.prorateAdjustmentFlag = prorateAdjustmentFlag;
	}

	@JsonIgnore
	public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @ApiModelProperty(value = "Unique identifier for the parent order of this order")
    public Integer getPrimaryOrderId() {
		return primaryOrderId;
	}

	public void setPrimaryOrderId(Integer primaryOrderId) {
		this.primaryOrderId = primaryOrderId;
	}

    @ApiModelProperty(value = "An array of user defined meta fields")
    public MetaFieldValueWS[] getMetaFields() {
        return metaFields;
    }

    public void setMetaFields(MetaFieldValueWS[] metaFields) {
        this.metaFields = metaFields;
    }

    @ApiModelProperty(value = "Parent order for this order")
    public OrderWS getParentOrder() {
        return parentOrder;
    }

    @ApiModelProperty(value = "Array of provisioning commands for the order")
    public ProvisioningCommandWS[] getProvisioningCommands() {
        return provisioningCommands;
    }

    public void setProvisioningCommands(ProvisioningCommandWS[] provisioningCommands) {
        this.provisioningCommands = provisioningCommands;
    }

    @XmlTransient
    public void setParentOrder(OrderWS parentOrder) {
        this.parentOrder = parentOrder;
    }

    /**
     * Used only in java <-> XML mapping to solve child order xml deserialization problem 
     * whn parent order not found
     */
    private String parentOrderId;

    @XmlAttribute(name = "parentOrderId")
    @ApiModelProperty(value = "Unique identifier of parent order for this order")
    public String getParentOrderId () {
        return parentOrderId;
    }

    public void setParentOrderId (String orderId) {
        parentOrderId = orderId;
    }

    /**
     * magic method used by JAX-RS before marshalling java instance to xml
     * 
     * @param marshaller
     */
    @SuppressWarnings("unused")
    private void beforeMarshal(final Marshaller marshaller) {
        if (parentOrder != null) {
            setParentOrderId (parentOrder.getObjectId());
        }
    }

    /**
     * magic method used by JAX-RS before unmarshalling xml to java instance
     * 
     * @param marshaller
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (final Unmarshaller u, final Object parent) {
        if (parent instanceof OrderWS) {
            this.parentOrder = (OrderWS)parent;
        }
    }

    @ApiModelProperty(value = "Array of the child orders for this order")
    public OrderWS[] getChildOrders() {
        return childOrders;
    }

    @XmlElement(name="childOrder")
    public void setChildOrders(OrderWS[] childOrders) {
        this.childOrders = childOrders;
    }

    /**
     * Returns true if any line has an asset linked to it
     * @return
     */
    public boolean hasLinkedAssets() {
        for(OrderLineWS line : orderLines) {
            if(line.hasLinkedAssets()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if any line has an orderLineTier linked to it
     * @return
     */
    public boolean hasOrderLineTiers() {
        for(OrderLineWS line : orderLines) {
            if(line.hasOrderLineTiers()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Unsupported, web-service security enforced using {@link #getOwningUserId()}
     * @return null
     */
    @XmlTransient
    @JsonIgnore
    public Integer getOwningEntityId() {
        return null;
    }

    @XmlTransient
    @JsonIgnore
    public Integer getOwningUserId() {
        return getUserId();
    }

    /**
     * Orders array, with only one OrderLineWS per bundled Item.
     * The adjustedPrice is a property of the LineWS object
     * @return the bundledItems 
     */
    @ApiModelProperty(value = "An array of plan bundled items with adjusted prices")
    public OrderLineWS[] getPlanBundledItems() {
        return planBundleItems;
    }
    
    /**
     * @param bundledItems the bundledItems to set
     */
    public void setPlanBundledItems(OrderLineWS[] bundledItems) {
        this.planBundleItems = bundledItems;
    }

    @JsonIgnore
    public String getAdjustedTotal() {
		return adjustedTotal;
	}

    @ApiModelProperty(value = "Adjusted total after applying order level discount")
    @JsonProperty("adjustedTotal")
    public BigDecimal getAdjustedTotalAsDecimal() {
		return Util.string2decimal(adjustedTotal);
	}

	@JsonIgnore
	public void setAdjustedTotal(String adjustedTotal) {
		this.adjustedTotal = adjustedTotal;
	}

	@JsonProperty("adjustedTotal")
	public void setAdjustedTotal(BigDecimal adjustedTotal) {
		this.adjustedTotal = (adjustedTotal != null ? adjustedTotal.toString() : null);
	}

	public Map<String, String> getOrderMetaFieldMap() {
        return orderMetaFieldMap;
    }

    public void setOrderMetaFieldMap(Map<String, String> orderMetaFieldMap) {
        this.orderMetaFieldMap = orderMetaFieldMap;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append("OrderWS");
        sb.append("{id=").append(id);
        sb.append(", userId=").append(userId);
        sb.append(", currencyId=").append(currencyId);
        sb.append(", activeUntil=").append(activeUntil);
        sb.append(", activeSince=").append(activeSince);
        sb.append(", statusStr='").append(statusStr).append('\'');
        sb.append(", periodStr='").append(periodStr).append('\'');
        sb.append(", periodId=").append(period);
        sb.append(", billingTypeStr='").append(billingTypeStr).append('\'');

        sb.append(", lines=");
        if (getOrderLines() != null) {
            sb.append(Arrays.toString(getOrderLines()));
        } else {
            sb.append("[]");
        }
        
        sb.append(", discountLines=");
        if (getDiscountLines() != null) {
            sb.append(Arrays.toString(getDiscountLines()));
        } else {
            sb.append("[]");
        }

        sb.append('}');
        sb.append(", parentOrderId=").append(parentOrder != null ? parentOrder.getId() : "null" ).append(',');
        sb.append(" childOrderIds:[");
        if (getChildOrders() != null) {
            for (OrderWS childOrder: getChildOrders()) {
                sb.append( null != childOrder ? childOrder.getId() : null).append("-");
            }
        }
        sb.append("]");

        sb.append(", userCode=").append(userCode);

        return sb.toString();
    }

    @ApiModelProperty(value = "This field indicates the order cancellation fee type")
	public String getCancellationFeeType() {
        return cancellationFeeType;
    }

    public void setCancellationFeeType(String cancellationFeeType) {
        this.cancellationFeeType = cancellationFeeType;
    }

    @ApiModelProperty(value = "Percentage for cancellation fees")
    public Integer getCancellationFeePercentage() {
        return cancellationFeePercentage;
    }

    public void setCancellationFeePercentage(Integer cancellationFeePercentage) {
        this.cancellationFeePercentage = cancellationFeePercentage;
    }

    @ApiModelProperty(value = "Order cancellation fees")
    public Integer getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(Integer cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    @ApiModelProperty(value = "Maximum cancellation fees that can be applied on order")
    public Integer getCancellationMaximumFee() {
        return cancellationMaximumFee;
    }

    public void setCancellationMaximumFee(Integer cancellationMaximumFee) {
        this.cancellationMaximumFee = cancellationMaximumFee;
    }

    @ApiModelProperty(value = "Minimum period for order cancellation")
    public Integer getCancellationMinimumPeriod() {
        return cancellationMinimumPeriod;
    }

    public void setCancellationMinimumPeriod(Integer cancellationMinimumPeriod) {
        this.cancellationMinimumPeriod = cancellationMinimumPeriod;
    }

    @ApiModelProperty(value = "The free quantity that the order has utilized")
    public String getFreeUsageQuantity() {
		return null != freeUsageQuantity && !freeUsageQuantity.isEmpty() ? freeUsageQuantity : "0";
	}

	public void setFreeUsageQuantity(String freeUsageQuantity) {
		this.freeUsageQuantity = freeUsageQuantity;
	}

    @ApiModelProperty(value = "Flag to set if the order is disabled")
    @JsonProperty("disable")
	public boolean isDisable() {
		return isDisable;
	}

	public void setDisable(boolean isDisable) {
		this.isDisable = isDisable;
	}

    @ApiModelProperty(value = "Unique identifier of the customer’s main subscription billing cycle unit")
	public Integer getCustomerBillingCycleUnit() {
		return customerBillingCycleUnit;
	}

    @ApiModelProperty(value = "Value for the customer’s main subscription billing cycle unit")
	public Integer getCustomerBillingCycleValue() {
		return customerBillingCycleValue;
	}

	public void setCustomerBillingCycleUnit(Integer customerBillingCycleUnit) {
		this.customerBillingCycleUnit = customerBillingCycleUnit;
	}

	public void setCustomerBillingCycleValue(Integer customerBillingCycleValue) {
		this.customerBillingCycleValue = customerBillingCycleValue;
	}

    @ApiModelProperty(value = "String that indicates whether the prorating option is AUTO ON, AUTO OFF or MANUAL")
	public String getProratingOption() {
		return proratingOption;
	}

	public void setProratingOption(String proratingOption) {
		this.proratingOption = proratingOption;
	}

    public Boolean getAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(Boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public Integer getRenewNotification() {return renewNotification;}

    public void setRenewNotification(Integer renewNotification) {
        this.renewNotification = renewNotification;
    }
	
    @Override
    @JsonIgnore
    public List<Integer> getAccessEntities() {
        return this.accessEntities;
    }

    @JsonIgnore
    public void setAccessEntities(List<Integer> accessEntities) {
        this.accessEntities = accessEntities;
    }

    @Override
    public Boolean ifGlobal() {
        return Boolean.FALSE;
    }

    
    @Transient
    @JsonIgnore
    public Object[] getTaxQuoteLines() {
        List<OrderLineWS> taxOrderLines = new ArrayList<OrderLineWS>();
        for(OrderLineWS orderLine : this.getOrderLines()) {
            if(orderLine.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX_QUOTE)) {
                taxOrderLines.add(orderLine);
            }
        }
        return taxOrderLines.toArray();
    }

    @Override
    public boolean equals(Object object) {

        if (this == object) {
            return true;
        }
        if (!(object instanceof OrderWS)) {
            return false;
        }
        OrderWS order = (OrderWS) object;
        return nullSafeEquals(this.id, order.id) &&
                Util.decimalEquals(this.getTotalAsDecimal(), order.getTotalAsDecimal()) &&
                Util.decimalEquals(this.getAdjustedTotalAsDecimal(), order.getAdjustedTotalAsDecimal()) &&
                nullSafeEquals(this.statusId, order.statusId) &&
                nullSafeEquals(this.userId, order.userId) &&
                nullSafeEquals(this.currencyId, order.currencyId) &&
                nullSafeEquals(this.billingTypeId, order.billingTypeId) &&
                nullSafeEquals(this.period, order.period) &&
                nullSafeEquals(this.createDate, order.createDate) &&
                nullSafeEquals(this.createdBy, order.createdBy) &&
                nullSafeEquals(this.activeSince, order.activeSince) &&
                nullSafeEquals(this.activeUntil, order.activeUntil) &&
                nullSafeEquals(this.nextBillableDay, order.nextBillableDay) &&
                (this.deleted == order.deleted) &&
                nullSafeEquals(this.notify, order.notify) &&
                nullSafeEquals(this.lastNotified, order.lastNotified) &&
                nullSafeEquals(this.notificationStep, order.notificationStep) &&
                nullSafeEquals(this.dueDateUnitId, order.dueDateUnitId) &&
                nullSafeEquals(this.dueDateValue, order.dueDateValue) &&
                nullSafeEquals(this.dfFm, order.dfFm) &&
                nullSafeEquals(this.anticipatePeriods, order.anticipatePeriods) &&
                nullSafeEquals(this.ownInvoice, order.ownInvoice) &&
                nullSafeEquals(this.notes, order.notes) &&
                nullSafeEquals(this.notesInInvoice, order.notesInInvoice) &&
                nullSafeEquals(this.orderLines, order.orderLines) &&
                nullSafeEquals(this.discountLines, order.discountLines) &&
                nullSafeEquals(this.pricingFields, order.pricingFields) &&
                // TODO equals not implemented in InvoiceWS yet
                // nullSafeEquals(this.generatedInvoices, order.generatedInvoices) &&
                nullSafeEquals(this.metaFields, order.metaFields) &&
                nullSafeEquals(this.parentOrder == null ? 0 : this.parentOrder.getId(), order.parentOrder == null ? 0 : order.parentOrder.getId()) &&
                nullSafeEquals(this.childOrders, order.childOrders) &&
                nullSafeEquals(this.userCode, order.userCode) &&
                nullSafeEquals(this.planBundleItems, order.planBundleItems) &&
                nullSafeEquals(this.prorateFlag, order.prorateFlag) &&
                nullSafeEquals(this.proratingOption, order.proratingOption) &&
                (this.isDisable == order.isDisable) &&
                nullSafeEquals(this.customerBillingCycleUnit, order.customerBillingCycleUnit) &&
                nullSafeEquals(this.customerBillingCycleValue, order.customerBillingCycleValue) &&
                nullSafeEquals(this.cancellationFee, order.cancellationFee) &&
                nullSafeEquals(this.cancellationFeeType, order.cancellationFeeType) &&
                nullSafeEquals(this.cancellationFeePercentage, order.cancellationFeePercentage) &&
                nullSafeEquals(this.cancellationMaximumFee, order.cancellationMaximumFee) &&
                nullSafeEquals(this.cancellationMinimumPeriod, order.cancellationMinimumPeriod) &&
                nullSafeEquals(this.freeUsageQuantity, order.freeUsageQuantity);
    }

    @Override
    public int hashCode() {

        int result = nullSafeHashCode(id);
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getTotalAsDecimal()));
        result = 31 * result + nullSafeHashCode(Util.getScaledDecimal(getAdjustedTotalAsDecimal()));
        result = 31 * result + nullSafeHashCode(createDate);
        result = 31 * result + nullSafeHashCode(activeSince);
        result = 31 * result + nullSafeHashCode(activeUntil);
        result = 31 * result + nullSafeHashCode(userId);
        return result;
    }
}
