package com.sapienter.jbilling.server.mediation.converter.db;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;



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
 * Created by marcolin on 06/10/15.
 */
/**
 * Basic record to mediate. It has all the information cooked so
 * jBilling can update the current order without any processing.
 *
 */
@SuppressWarnings("serial")
@Entity
@Table(name="jbilling_mediation_record")
public class JbillingMediationRecordDao implements Serializable {

    public enum STATUS {
        UNPROCESSED, PROCESSED, NOT_BILLABLE, AGGREGATED
    }

    public enum TYPE {
        MEDIATION, DIAMETER
    }


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "jbilling_mediation_record_generator")
    @SequenceGenerator(name ="jbilling_mediation_record_generator", sequenceName = "jbilling_mediation_record_seq", allocationSize = 1)
    private Long id;
    @Enumerated(value = EnumType.STRING)
    @Column(name="status", nullable=false)
    private STATUS status = STATUS.UNPROCESSED;
    @Enumerated(value = EnumType.STRING)
    @Column(name="type", nullable=false)
    private TYPE type = TYPE.MEDIATION;
    @Column(name="jbilling_entity_id")
    private Integer jBillingCompanyId = null;
    @Column(name="mediation_cfg_id")
    private Integer mediationCfgId = null;
    @Column(name="record_key")
    private String recordKey = null;
    @Column(name="user_id")
    private Integer userId = null;
    @Column(name="event_date")
    private Date eventDate = null;
    @Column(name="processing_date")
    private Date processingDate = null;
    @Column(name="quantity")
    private BigDecimal quantity = null;
    @Column(name="description")
    private String description = null;
    @Column(name="currency_id")
    private Integer currencyId = null;
    @Column(name="item_id")
    private Integer itemId = null;
    @Column(name="order_id")
    private Integer orderId = null;
    @Column(name="order_line_id")
    private Integer orderLineId = null;
    @Column(name="rated_price")
    private BigDecimal ratedPrice;
    @Column(name="rated_cost_price")
    private BigDecimal ratedCostPrice;
    @Column(name="process_id")
    private UUID processId = null;
    @Column(name="source", nullable = true)
    private String source;
    @Column(name="destination" , nullable = true)
    private String destination;
    @Column(name="cdr_type" , nullable = true)
    private String cdrType;
    @Column(name="original_quantity" , nullable = true)
    private BigDecimal originalQuantity;

    @Column(name="resource_id") //nullable
    private String resourceId = null;

    // these are pricing fields needed to resolve pricing. For example, the
    // destination number dialed for
    // long distance pricing based on a rate card
    // The String will later be processed with PricingField.getPricingFieldsValue()
    @Column(name="pricing_fields", length = 1000)
    private String pricingFields = null;
    @Column(name = "chargeable", nullable = false)
    private Boolean chargeable = Boolean.TRUE;
    @Column(name="tax_amount")
    private BigDecimal taxAmount;
    @Column(name="rated_price_with_tax")
    private BigDecimal ratedPriceWithTax;

    public JbillingMediationRecordDao() {}

    public JbillingMediationRecordDao(Long id, STATUS status, TYPE type, Integer jBillingCompanyId, Integer mediationCfgId,
            String recordKey, Integer userId, Date eventDate, BigDecimal quantity,
            String description, Integer currencyId, Integer itemId, Integer orderId,
            Integer orderLineId, String pricingFields, BigDecimal ratedPrice,
            BigDecimal ratedCostPrice, UUID processId, String source, String destination,
            String cdrType, BigDecimal originalQuantity, String resourceId, Boolean chargeable,
            BigDecimal taxAmount, BigDecimal ratedPriceWithTax) {
        this.id = id;
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
        this.pricingFields = pricingFields;
        this.orderLineId = orderLineId;
        this.ratedPrice = ratedPrice;
        this.ratedCostPrice = ratedCostPrice;
        this.processId = processId;
        this.processingDate = new Date();
        this.source = source;
        this.destination = destination;
        this.cdrType = cdrType;
        this.originalQuantity = originalQuantity;
        this.resourceId = resourceId;
        this.chargeable = chargeable;
        this.taxAmount = taxAmount;
        this.ratedPriceWithTax =  ratedPriceWithTax;
    }

    public JbillingMediationRecordDao(Long id, STATUS status, TYPE type, Integer jBillingCompanyId,
            Integer mediationCfgId, String recordKey, Integer userId,
            Date eventDate, BigDecimal quantity, String description,
            Integer currencyId, Integer itemId, Integer orderId, Integer orderLineId,
            String pricingFields, BigDecimal ratedPrice, BigDecimal ratedCostPrice,
            UUID processId, String source, String destination, String cdrType,
            BigDecimal originalQuantity,BigDecimal taxAmount, BigDecimal ratedPriceWithTax) {

        this(id, status, type, jBillingCompanyId, mediationCfgId, recordKey, userId, eventDate,
                quantity, description, currencyId, itemId, orderId, orderLineId, pricingFields,
                ratedPrice, ratedCostPrice, processId, source, destination, cdrType,
                originalQuantity, null, Boolean.TRUE,taxAmount,ratedPriceWithTax);
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

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public Integer getjBillingCompanyId() {
        return jBillingCompanyId;
    }

    public void setjBillingCompanyId(Integer jBillingCompanyId) {
        this.jBillingCompanyId = jBillingCompanyId;
    }

    public Integer getMediationCfgId() {
        return mediationCfgId;
    }

    public void setMediationCfgId(Integer mediationCfgId) {
        this.mediationCfgId = mediationCfgId;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(Integer currencyId) {
        this.currencyId = currencyId;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
    }

    public String getPricingFields() {
        return pricingFields;
    }

    public void setPricingFields(String pricingFields) {
        this.pricingFields = pricingFields;
    }

    public BigDecimal getRatedPrice() {
        return ratedPrice;
    }

    public void setRatedPrice(BigDecimal ratedPrice) {
        this.ratedPrice = ratedPrice;
    }

    public BigDecimal getRatedCostPrice() {
        return ratedCostPrice;
    }

    public void setRatedCostPrice(BigDecimal ratedCostPrice) {
        this.ratedCostPrice = ratedCostPrice;
    }

    public UUID getProcessId() {
        return processId;
    }

    public void setProcessId(UUID processId) {
        this.processId = processId;
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

    public BigDecimal getRatedPriceWithTax() {
        return ratedPriceWithTax;
    }

    public void setRatedPriceWithTax(BigDecimal ratedPriceWithTax) {

        this.ratedPriceWithTax = ratedPriceWithTax;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}
