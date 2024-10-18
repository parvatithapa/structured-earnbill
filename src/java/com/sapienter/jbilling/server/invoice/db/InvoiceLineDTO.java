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
package com.sapienter.jbilling.server.invoice.db;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.util.Constants;

@SuppressWarnings("serial")
@Entity
@TableGenerator(
        name            = "invoice_line_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "invoice_line",
        allocationSize  = 100)
@Table(name = "invoice_line")
public class InvoiceLineDTO implements Serializable {

    private int id;
    private InvoiceLineTypeDTO invoiceLineType;
    private ItemDTO item;
    private InvoiceDTO invoice;

    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal price;

    private Integer deleted;
    private String description;
    private Integer sourceUserId;
    private Integer isPercentage;
    private int versionNum;
    private String callIdentifier;
    private String assetIdentifier;

    private OrderDTO order;
    private Long callCounter = 0L;
    private InvoiceLineDTO parentLine;
    private Integer usagePlanId;
    boolean isOrderLineTier = false;

    private BigDecimal taxRate = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal grossAmount = BigDecimal.ZERO;

    public InvoiceLineDTO () { }

    public InvoiceLineDTO (final int id, final BigDecimal amount, final Integer deleted, final Integer isPercentage) {
        this.id = id;
        this.amount = amount;
        this.deleted = deleted;
        this.isPercentage = isPercentage;
    }

    public InvoiceLineDTO (final Integer id, final String description, final BigDecimal amount, final BigDecimal price, final BigDecimal quantity,
            final Integer typeId, final Integer deleted, final Integer itemId, final Integer sourceUserId, final Integer isPercentage) {
        setId(id == null ? 0 : id);
        setDescription(description);
        setAmount(amount);
        setPrice(price);
        setQuantity(quantity);
        setDeleted(deleted);
        setItem(itemId == null ? null : new ItemDTO(itemId));
        setSourceUserId(sourceUserId);
        setIsPercentage(isPercentage);
        setInvoiceLineType(new InvoiceLineTypeDTO(typeId));
    }

    public InvoiceLineDTO (final int id, final InvoiceLineTypeDTO invoiceLineType, final ItemDTO item, final InvoiceDTO invoice,
            final BigDecimal amount, final BigDecimal quantity, final BigDecimal price, final Integer deleted, final String description,
            final Integer sourceUserId, final Integer isPercentage) {
        this.id = id;
        this.invoiceLineType = invoiceLineType;
        this.item = item;
        this.invoice = invoice;
        this.amount = amount;
        this.quantity = quantity;
        this.price = price;
        this.deleted = deleted;
        this.description = description;
        this.sourceUserId = sourceUserId;
        this.isPercentage = isPercentage;
    }

    public InvoiceLineDTO (final int id2, final String description2, final BigDecimal amount, final BigDecimal price, final BigDecimal quantity2,
            final Integer deleted, final ItemDTO item, final Integer sourceUserId2, final Integer isPercentage) {
        this.id = id2;
        this.description = description2;
        this.amount = amount;
        this.price = price;
        this.quantity = quantity2;
        this.deleted = deleted;
        this.item = item;
        this.sourceUserId = sourceUserId2;
        this.isPercentage = isPercentage;
    }

    public static class Builder {

        private String description;
        private BigDecimal amount   = BigDecimal.ZERO;
        private BigDecimal quantity = BigDecimal.ZERO;
        private BigDecimal price    = BigDecimal.ZERO;
        private Integer type;
        private Integer itemId;
        private Integer sourceUserId;
        private OrderDTO order;
        private boolean isPercentage = false;
        private String callIdentifier;
        private String assetIdentifier;
        private Long callCounter = 0L;
        private Integer usagePlanId;

        private BigDecimal grossAmount;
        private BigDecimal taxRate;
        private BigDecimal taxAmount;

        public Builder description (final String description) {
            this.description = description;
            return this;
        }

        public Builder amount (final BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder grossAmount (final BigDecimal grossAmount) {
            this.grossAmount = grossAmount;
            return this;
        }

        public Builder taxRate (final BigDecimal taxRate) {
            this.taxRate = taxRate;
            return this;
        }

        public Builder taxAmount (final BigDecimal taxAmount) {
            this.taxAmount = taxAmount;
            return this;
        }

        public Builder quantity (final BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder price (final BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder type (final Integer type) {
            this.type = type;
            return this;
        }

        public Builder itemId (final Integer itemId) {
            this.itemId = itemId;
            return this;
        }

        public Builder sourceUserId (final Integer sourceUserId) {
            this.sourceUserId = sourceUserId;
            return this;
        }

        public Builder order (final OrderDTO order) {
            this.order = order;
            return this;
        }

        public Builder isPercentage (final boolean isPercentage) {
            this.isPercentage = isPercentage;
            return this;
        }

        public Builder callIdentifier (final String callIdentifier) {
            this.callIdentifier = callIdentifier;
            return this;
        }

        public Builder assetIdentifier (final String assetIdentifier) {
            this.assetIdentifier = assetIdentifier;
            return this;
        }

        public Builder callCounter (final Long callCounter) {
            this.callCounter = callCounter;
            return this;
        }

        public Builder usagePlanId (final Integer usagePlanId) {
            this.usagePlanId = usagePlanId;
            return this;
        }

        public InvoiceLineDTO build () {
            InvoiceLineDTO newLine = new InvoiceLineDTO();
            newLine.setDeleted(0);
            newLine.setDescription(description);
            newLine.setAmount(amount);
            newLine.setGrossAmount(grossAmount);
            newLine.setTaxAmount(taxAmount);
            newLine.setTaxRate(taxRate);
            newLine.setQuantity(quantity);
            newLine.setPrice(price);
            newLine.setItem(itemId == null ? null : new ItemDTO(itemId));
            newLine.setSourceUserId(sourceUserId);
            newLine.setInvoiceLineType(new InvoiceLineTypeDTO(type));
            newLine.setIsPercentage(isPercentage ? 1 :0);
            newLine.setCallIdentifier(callIdentifier);
            newLine.setCallCounter(callCounter);
            newLine.setAssetIdentifier(assetIdentifier);
            newLine.setUsagePlanId(usagePlanId);

            if (order != null) {
                newLine.setOrder(order);
            }

            return newLine;
        }
    }

    public InvoiceLineDTO(final Integer id, final String description, final BigDecimal amount, final BigDecimal price, final BigDecimal quantity,
            final Integer typeId, final Integer deleted, final Integer itemId, final Integer sourceUserId, final Integer isPercentage,
            final String callIdentifier) {
        setId(id == null ? 0 : id);
        setDescription(description);
        setAmount(amount);
        setPrice(price);
        setQuantity(quantity);
        setDeleted(deleted);
        setItem(itemId == null ? null : new ItemDTO(itemId));
        setSourceUserId(sourceUserId);
        setIsPercentage(isPercentage);
        setInvoiceLineType(new InvoiceLineTypeDTO(typeId));
        setCallIdentifier(callIdentifier);
    }

    public InvoiceLineDTO(final Integer id, final String description, final BigDecimal amount, final BigDecimal price, final BigDecimal quantity,
            final Integer typeId, final Integer deleted, final Integer itemId, final Integer sourceUserId, final Integer isPercentage,
            final String callIdentifier, final Long callCounter, final String assetIdentifier, final Integer usagePlanId) {
        setId(id == null ? 0 : id);
        setDescription(description);
        setAmount(amount);
        setPrice(price);
        setQuantity(quantity);
        setDeleted(deleted);
        setItem(itemId == null ? null : new ItemDTO(itemId));
        setSourceUserId(sourceUserId);
        setIsPercentage(isPercentage);
        setInvoiceLineType(new InvoiceLineTypeDTO(typeId));
        setCallIdentifier(callIdentifier);
        setCallCounter(callCounter);
        setAssetIdentifier(assetIdentifier);
        setUsagePlanId(usagePlanId);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "invoice_line_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId () {
        return this.id;
    }

    public void setId (final int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id")
    public InvoiceLineTypeDTO getInvoiceLineType () {
        return this.invoiceLineType;
    }

    public void setInvoiceLineType (final InvoiceLineTypeDTO invoiceLineType) {
        this.invoiceLineType = invoiceLineType;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_line_id")
    public InvoiceLineDTO getParentLine() {
        return parentLine;
    }

    public void setParentLine(final InvoiceLineDTO parentLine) {
        this.parentLine = parentLine;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public ItemDTO getItem () {
        return this.item;
    }

    public void setItem (final ItemDTO item) {
        this.item = item;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    public InvoiceDTO getInvoice () {
        return this.invoice;
    }

    public void setInvoice (final InvoiceDTO invoice) {
        this.invoice = invoice;
    }

    /**
     * Returns the total amount for this line. Usually this would be the {@code price * quantity}
     *
     * @return amount
     */
    @Column(name = "amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getAmount () {
        return this.amount;
    }

    public void setAmount (final BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name = "quantity")
    public BigDecimal getQuantity () {
        return this.quantity;
    }

    public void setQuantity (final BigDecimal quantity) {
        this.quantity = quantity;
    }

    public void setQuantity (final Integer quantity) {
        setQuantity(new BigDecimal(quantity));
    }

    /**
     * Returns the price of a single unit of this item.
     *
     * @return unit price
     */
    @Column(name = "price", precision = 17, scale = 17)
    public BigDecimal getPrice () {
        return this.price;
    }

    public void setPrice (final BigDecimal price) {
        this.price = price;
    }

    @Column(name = "deleted", nullable = false)
    public Integer getDeleted () {
        return this.deleted;
    }

    public void setDeleted (final Integer deleted) {
        this.deleted = deleted;
    }

    @Column(name = "description", length = 1000)
    public String getDescription () {
        return this.description;
    }

    public void setDescription (final String description) {
        this.description = description;
    }

    @Column(name = "source_user_id")
    public Integer getSourceUserId () {
        return this.sourceUserId;
    }

    public void setSourceUserId (final Integer sourceUserId) {
        this.sourceUserId = sourceUserId;
    }

    /**
     * Indicates whether or not the item referenced by this line is a percentage item or not.
     *
     * 1 - Item is a percentage item 0 - Item is not a percentage item
     *
     * @return 1 if item is percentage, 0 if not
     */
    @Column(name = "is_percentage", nullable = false)
    public Integer getIsPercentage () {
        return this.isPercentage;
    }

    public void setIsPercentage (final Integer isPercentage) {
        this.isPercentage = isPercentage;
    }
    @Column(name = "call_counter")
    public Long getCallCounter() {
        return callCounter;
    }

    public void setCallCounter(final Long callCounter) {
        this.callCounter = callCounter;
    }

    @Version
    @Column(name = "OPTLOCK")
    public int getVersionNum () {
        return versionNum;
    }

    public void setVersionNum (final int versionNum) {
        this.versionNum = versionNum;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    public OrderDTO getOrder () {
        return this.order;
    }

    public void setOrder (final OrderDTO order) {
        this.order = order;
    }

    @Transient
    public boolean isReviewInvoiceLine() { return ( null != getInvoice() && getInvoice().isReviewInvoice()); }

    @Transient
    public int getOrderPosition () {
        return (null != getInvoiceLineType()) ? getInvoiceLineType().getOrderPosition() : 0;
    }

    @Transient
    public int getTypeId () {
        return (null != getInvoiceLineType()) ? getInvoiceLineType().getId() : 0;
    }

    @Transient
    public boolean dueInvoiceLine () {
        return Constants.INVOICE_LINE_TYPE_DUE_INVOICE.equals(getTypeId());
    }

    @Transient
    public boolean isTaxLine() {
        return Constants.INVOICE_LINE_TYPE_TAX.equals(getTypeId());
    }

    @Transient
    public boolean isDiscountLine() {
        if(getOrder()==null){
            return false;
        }

        return getOrder().isDiscountOrder();
    }

    @Transient
    public boolean adjustmentInvoiceLine () {
        return Constants.INVOICE_LINE_TYPE_ADJUSTMENT.equals(getTypeId());
    }

    public String getAuditKey(final Serializable id) {
        return new StringBuilder().append(getInvoice().getBaseUser().getCompany().getId())
                .append("-usr-")
                .append(getInvoice().getBaseUser().getId())
                .append("-inv-")
                .append(getInvoice().getId())
                .append("-")
                .append(id)
                .toString();
    }

    @Column(name = "call_identifier")
    public String getCallIdentifier() {
        return callIdentifier;
    }

    public void setCallIdentifier(final String callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    @Column(name = "asset_identifier")
    public String getAssetIdentifier() {
        return assetIdentifier;
    }

    public void setAssetIdentifier(final String assetIdentifier) {
        this.assetIdentifier = assetIdentifier;
    }

    @Column(name = "usage_plan_id")
    public Integer getUsagePlanId() {
        return usagePlanId;
    }

    public void setUsagePlanId(final Integer usagePlanId) {
        this.usagePlanId = usagePlanId;
    }

    public void setIsOrderLineTier(final boolean isOrderLineTier){
        this.isOrderLineTier=isOrderLineTier;
    }

    @Transient
    public boolean getIsOrderLineTier(){
        return isOrderLineTier;
    }

    @Column(name = "tax_rate", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(final BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    @Column(name = "tax_amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(final BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    @Column(name = "gross_amount", nullable = false, precision = 17, scale = 17)
    public BigDecimal getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(final BigDecimal grossAmount) {
        this.grossAmount = grossAmount;
    }

    @Transient
    public boolean hasItem() {
        return this.item!=null;
    }

    @Transient
    public boolean isPlanLine() {
        if(hasItem()) {
            return this.item.isPlan();
        }
        return false;
    }
}
