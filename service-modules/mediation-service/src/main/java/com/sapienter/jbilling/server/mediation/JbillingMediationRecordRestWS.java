package com.sapienter.jbilling.server.mediation;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.wordnik.swagger.annotations.ApiModelProperty;

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

/**
 * Created by MaheshK on 23/04/19.
 */

/**
 * Basic record to mediate. It has all the information cooked so
 * jBilling can update the current order without any processing.
 *
 */
public class JbillingMediationRecordRestWS implements Serializable {

    public enum STATUS {
        UNPROCESSED(0), PROCESSED(1), NOT_BILLABLE(2), AGGREGATED(3);
        private final int id;

        STATUS(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum TYPE {
        MEDIATION, DIAMETER
    }

    private STATUS status = STATUS.UNPROCESSED;
    private Integer jBillingCompanyId = null;
    private Integer mediationCfgId = null;
    private String recordKey = null;
    private Integer userId = null;
    private Date eventDate = null;
    private BigDecimal quantity = null;
    private String description = null;
    private Integer currencyId = null;
    private Integer itemId = null;
    private Integer orderId = null;
    private Integer orderLineId;
    private BigDecimal ratedPrice;
    private BigDecimal ratedCostPrice;
    private UUID processId;
    private String source;
    private String destination;
    private String cdrType;
    private BigDecimal originalQuantity;
    private String resourceId;

    // these are pricing fields needed to resolve pricing. For example, the
    // destination number dialed for
    // long distance pricing based on a rate card
    // The String will later be processed with PricingField.getPricingFieldsValue()
    private Map<String, String> pricingFields = null;
    private TYPE type = TYPE.MEDIATION;
    private Boolean chargeable;

    public JbillingMediationRecordRestWS(STATUS status, TYPE type, Integer jBillingCompanyId,
            Integer mediationCfgId, String recordKey, Integer userId, Date eventDate, 
            BigDecimal quantity, String description, Integer currencyId, Integer itemId, 
            Integer orderId, Integer orderLineId,BigDecimal ratedPrice, BigDecimal ratedCostPrice,
            UUID processId, String source, String destination, String cdrType,
            BigDecimal originalQuantity, String resourceId, Boolean chargeable) {

        this.status = status;
        this.type = type;
        this.jBillingCompanyId = jBillingCompanyId;
        this.mediationCfgId = mediationCfgId;
        this.recordKey = recordKey;
        this.userId = userId;
        this.eventDate = eventDate;
        this.quantity = quantity;
        this.description = description;
        this.currencyId = currencyId;
        this.itemId = itemId;
        this.orderId = orderId;
        this.orderLineId = orderLineId;
        this.ratedPrice = ratedPrice;
        this.ratedCostPrice = ratedCostPrice;
        this.processId = processId;
        this.source = source;
        this.destination = destination;
        this.cdrType = cdrType;
        this.originalQuantity = originalQuantity;
        this.resourceId = resourceId;
        this.chargeable = chargeable;
    }

    public JbillingMediationRecordRestWS(JbillingMediationRecord record) {
        this(STATUS.valueOf(record.getStatus().toString()), TYPE.valueOf(record.getType().toString()),
                record.getjBillingCompanyId(), record.getMediationCfgId(), record.getRecordKey(), 
                record.getUserId(), record.getEventDate(), record.getQuantity(), record.getDescription(),
                record.getCurrencyId(), record.getItemId(), record.getOrderId(), record.getOrderLineId(),
                record.getRatedPrice(), record.getRatedCostPrice(), record.getProcessId(), record.getSource(),
                record.getDestination(), record.getCdrType(), record.getOriginalQuantity());
    }

    public JbillingMediationRecordRestWS(STATUS status, TYPE type, Integer jBillingCompanyId,
            Integer mediationCfgId, String recordKey, Integer userId,
            Date eventDate, BigDecimal quantity, String description,
            Integer currencyId, Integer itemId, Integer orderId, Integer orderLineId,
            BigDecimal ratedPrice, BigDecimal ratedCostPrice, UUID processId, 
            String source, String destination, String cdrType,
            BigDecimal originalQuantity) {
        this(status, type, jBillingCompanyId, mediationCfgId, recordKey, userId, eventDate, quantity,
                description, currencyId, itemId, orderId, orderLineId, ratedPrice,
                ratedCostPrice, processId, source, destination, cdrType, originalQuantity, null, Boolean.TRUE);
    }

    @Override
    public String toString() {
        return "JbillingMediationRecord{" +
                "status='" + status + '\'' +
                ", jBillingEntiytId='" + jBillingCompanyId + '\'' +
                ", mediationCfgId=" + mediationCfgId +
                ", recordKey=" + recordKey +
                ", processId=" + processId +
                ", userId=" + userId +
                ", eventDate='" + eventDate + '\'' +
                ", quantity=" + quantity +
                ", description=" + description +
                ", currencyId='" + currencyId + '\'' +
                ", itemId=" + itemId +
                '}';
    }

    @ApiModelProperty(value = "Status")
    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    @ApiModelProperty(value = "Type")
    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    @ApiModelProperty(value = "Company ID")
    public Integer getjBillingCompanyId() {
        return jBillingCompanyId;
    }

    public void setjBillingCompanyId(Integer jBillingCompanyId) {
        this.jBillingCompanyId = jBillingCompanyId;
    }

    @ApiModelProperty(value = "Mediation Config ID")
    public Integer getMediationCfgId() {
        return mediationCfgId;
    }

    public void setMediationCfgId(Integer mediationCfgId) {
        this.mediationCfgId = mediationCfgId;
    }

    @ApiModelProperty(value = "Record key")
    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    @ApiModelProperty(value = "User ID")
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    @ApiModelProperty(value = "Event Date")
    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    @ApiModelProperty(value = "Quantity")
    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @ApiModelProperty(value = "Description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ApiModelProperty(value = "Currency ID")
    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    @ApiModelProperty(value = "Product ID")
    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Map<String, String> getPricingFields() {
        return pricingFields;
    }

    public void setPricingFields(Map<String, String> pricingFields) {
        this.pricingFields = pricingFields;
    }

    @ApiModelProperty(value = "Order ID")
    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    @ApiModelProperty(value = "Order Line ID")
    public Integer getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }

    @ApiModelProperty(value = "Rated Price")
    public BigDecimal getRatedPrice() {
        return ratedPrice;
    }

    public void setRatedPrice(BigDecimal ratedPrice) {
        this.ratedPrice = ratedPrice;
    }

    @ApiModelProperty(value = "Unique identifier")
    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
    }

    @ApiModelProperty(value = "Rated Cost Price")
    public BigDecimal getRatedCostPrice() {
        return ratedCostPrice;
    }

    public void setRatedCostPrice(BigDecimal ratedCostPrice) {
        this.ratedCostPrice = ratedCostPrice;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getCdrType() {
        return cdrType;
    }

    public void setCdrType(String cdrType) {
        this.cdrType = cdrType;
    }

    public BigDecimal getOriginalQuantity() {
        return originalQuantity;
    }

    public void setOriginalQuantity(BigDecimal originalQuantity) {
        this.originalQuantity = originalQuantity;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public Boolean getChargeable() {
        return chargeable;
    }

    public void setChargeable(Boolean chargeable) {
        this.chargeable = chargeable;
    }
}
