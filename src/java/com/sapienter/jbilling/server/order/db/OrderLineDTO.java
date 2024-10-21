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
package com.sapienter.jbilling.server.order.db;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.SortComparator;

import com.sapienter.jbilling.common.Constants;
import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.audit.Auditable;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDAS;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.mediation.JbillingMediationRecord;
import com.sapienter.jbilling.server.mediation.MediationService;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderLinePlanItemDTOEx;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningStatusDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningStatusDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.util.Context;

@Entity
@DynamicUpdate(true)
@TableGenerator(
        name            = "order_line_GEN",
        table           = "jbilling_seqs",
        pkColumnName    = "name",
        valueColumnName = "next_id",
        pkColumnValue   = "order_line",
        allocationSize  = 100
        )
@Table(name="order_line")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderLineDTO extends CustomizedEntity implements Serializable, Comparable, IProvisionable, Auditable {

    private static final FormatLogger LOG =  new FormatLogger(Logger.getLogger(OrderLineDTO.class));

    private int id;
    private OrderLineTypeDTO orderLineTypeDTO;
    private ItemDTO item;
    private OrderDTO orderDTO;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal price;
    private Date createDatetime;
    private int deleted;
    private Boolean useItem = true;
    private String description;
    private Integer versionNum;
    private Boolean editable = null;
    private Set<AssetDTO> assets = new HashSet<>(2);
    private Set<AssetAssignmentDTO> assetAssignments = new HashSet<>(0);
    private OrderLineDTO parentLine;
    private Set<OrderLineDTO> childLines = new HashSet<>(0);

    //provisioning fields
    private ProvisioningStatusDTO provisioningStatus;
    private String provisioningRequestId;

    private String sipUri;

    private Set<OrderChangeDTO> orderChanges = new HashSet<>(0);

    private Date startDate;
    private Date endDate;

    // other fields, non-persistent
    private String priceStr = null;
    private Boolean totalReadOnly = null;
    private String provisioningStatusStr;
    private OrderLinePlanItemDTOEx[] orderLinePlanItems;

    private boolean isTouched = false;
    private Set<OrderLineUsagePoolDTO> orderLineUsagePools = new HashSet<>(0);

    private boolean mediated = false;
    private BigDecimal mediatedQuantity;
    private boolean isPercentage =false;

    private Long callCounter = 0L;

    private String callIdentifier;
    private List<OrderLineTierDTO> orderLineTiers = new ArrayList<>(0);
    private List<OrderLineItemizedUsageDTO> orderLineItemizedUsages = new ArrayList<>();
    private OrderLineInfo oldOrderLineInfo;

    public OrderLineDTO() {
    }

    public OrderLineDTO(OrderLineDTO other) {
        this.id = other.getId();
        this.orderLineTypeDTO = other.getOrderLineType();
        this.item = other.getItem();
        this.amount = other.getAmount();
        this.quantity = other.getQuantity();
        this.price = other.getPrice();
        this.createDatetime = other.getCreateDatetime();
        this.deleted = other.getDeleted();
        this.useItem = other.getUseItem();
        this.description = other.getDescription();
        this.orderDTO = other.getPurchaseOrder();
        this.versionNum = other.getVersionNum();
        this.assets.addAll(other.getAssets());
        this.parentLine = other.getParentLine();
        this.childLines.addAll(other.getChildLines());
        this.orderLineUsagePools.addAll(other.getOrderLineUsagePools());
        this.startDate = other.getStartDate();
        this.endDate = other.getEndDate();
        this.isPercentage =other.isPercentage();
        this.callIdentifier=other.getCallIdentifier();
        this.getMetaFields().addAll(other.getMetaFields());
    }

    public OrderLineDTO(int id, BigDecimal amount, Date createDatetime, Integer deleted) {
        this.id = id;
        this.amount = amount;
        this.createDatetime = createDatetime;
        this.deleted = deleted != null ? deleted : 0;
    }

    public OrderLineDTO(int id, OrderLineTypeDTO orderLineTypeDTO, ItemDTO item, OrderDTO orderDTO, BigDecimal amount,
            BigDecimal quantity, BigDecimal price, Date createDatetime, Integer deleted,
            String description, ProvisioningStatusDTO provisioningStatus, String provisioningRequestId) {
        this.id = id;
        this.orderLineTypeDTO = orderLineTypeDTO;
        this.item = item;
        this.orderDTO = orderDTO;
        this.amount = amount;
        this.quantity = quantity;
        this.price = price;
        this.createDatetime = createDatetime;
        this.deleted = deleted;
        this.description = description;
        this.provisioningStatus=provisioningStatus;
        this.provisioningRequestId=provisioningRequestId;
    }

    @Transient
    public OrderLinePlanItemDTOEx[] getOrderLinePlanItems() {
        return orderLinePlanItems;
    }

    public void setOrderLinePlanItems(OrderLinePlanItemDTOEx[] orderLinePlanItems) {
        this.orderLinePlanItems = orderLinePlanItems;
    }

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="order_line_GEN")
    @Column(name="id", unique=true, nullable=false)
    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="type_id", nullable=false)
    public OrderLineTypeDTO getOrderLineType() {
        return this.orderLineTypeDTO;
    }

    public void setOrderLineType(OrderLineTypeDTO orderLineTypeDTO) {
        this.orderLineTypeDTO = orderLineTypeDTO;
    }

    @OneToMany(fetch=FetchType.LAZY, mappedBy="orderLine")
    @Cascade({org.hibernate.annotations.CascadeType.SAVE_UPDATE, org.hibernate.annotations.CascadeType.MERGE, org.hibernate.annotations.CascadeType.DETACH})
    public Set<AssetDTO> getAssets() {
        return assets;
    }

    public void setAssets(Set<AssetDTO> assets) {
        this.assets = assets;
    }

    public void addAssets(Set<AssetDTO> assets) {
        for(AssetDTO asset : assets) {
            addAsset(asset);
        }
    }

    public void addAsset(AssetDTO asset) {
        assets.add(asset);
        asset.setOrderLine(this);
    }

    public void removeAsset(AssetDTO asset) {
        assets.remove(asset);
        asset.setOrderLine(null);
    }

    public Integer[] convertAssetIds() {
        Integer[] ids = new Integer[assets.size()];
        int idx=0;
        for(AssetDTO asset : assets) {
            ids[idx++] = asset.getId();
        }
        return ids;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "orderLine")
    @Cascade({org.hibernate.annotations.CascadeType.PERSIST,
        org.hibernate.annotations.CascadeType.REMOVE})
    public Set<AssetAssignmentDTO> getAssetAssignments() {
        return assetAssignments;
    }

    public void setAssetAssignments(Set<AssetAssignmentDTO> assetAssignments) {
        this.assetAssignments = assetAssignments;
    }

    @Transient
    public Integer[] getAssetAssignmentIds() {
        Integer[] ids = new Integer[assetAssignments.size()];
        int idx = 0;
        for (AssetAssignmentDTO assign : assetAssignments) {
            ids[idx++] = assign.getId();
        }
        return ids;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "order_line_meta_field_map",
            joinColumns = @JoinColumn(name = "order_line_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
            )
    @SortComparator(value = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    @Override
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ORDER_LINE };
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_line_id")
    public OrderLineDTO getParentLine() {
        return parentLine;
    }

    public void setParentLine(OrderLineDTO parentLine) {
        this.parentLine = parentLine;
    }

    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "parentLine")
    public Set<OrderLineDTO> getChildLines() {
        return childLines;
    }

    public void setChildLines(Set<OrderLineDTO> childLines) {
        this.childLines = childLines;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="item_id")
    public ItemDTO getItem() {
        return this.item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="order_id")
    public OrderDTO getPurchaseOrder() {
        return this.orderDTO;
    }

    public void setPurchaseOrder(OrderDTO orderDTO) {
        this.orderDTO = orderDTO;
    }

    /**
     * Returns the total amount for this line. Usually this would be
     * the {@code price * quantity}
     *
     * @return amount
     */
    @Column(name="amount", nullable=false, precision=17, scale=17)
    public BigDecimal getAmount() {
        return this.amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Column(name="quantity", precision=17, scale=17)
    public BigDecimal getQuantity() {
        return this.quantity;
    }

    @Transient
    public void setQuantity(Integer quantity) {
        setQuantity(new BigDecimal(quantity));
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    @Transient
    public void setQuantity(Double quantity) {
        setQuantity(new BigDecimal(quantity).setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND));
    }

    /**
     * Returns the price of a single unit of this item.
     *
     * @return unit price
     */
    @Column(name="price", precision=17, scale=17)
    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    @Column(name="create_datetime", nullable=false, length=29)
    public Date getCreateDatetime() {
        return this.createDatetime;
    }
    public void setCreateDatetime(Date createDatetime) {
        this.createDatetime = createDatetime;
    }

    @Column(name="deleted", nullable=false)
    public int getDeleted() {
        return this.deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Column(name = "use_item", nullable = false)
    public Boolean getUseItem() {
        return useItem;
    }

    public void setUseItem(Boolean useItem) {
        this.useItem = useItem;
    }

    @Column(name = "call_counter")
    public Long getCallCounter() {
        return callCounter;
    }

    public void setCallCounter(Long callCounter) {
        this.callCounter = callCounter;
    }

    @Column(name="description", length=1000)
    public String getDescription() {
        return this.description;
    }
    public void setDescription(String description) {
        if (description != null && description.length() > 1000) {
            description = description.substring(0, 1000);
            LOG.warn("Truncated an order line description to " + description);
        }

        this.description = description;
    }

    @Column(name = "call_identifier")
    public String getCallIdentifier() {
        return callIdentifier;
    }

    public void setCallIdentifier(String callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }

    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @Transient
    public List<JbillingMediationRecord> getEvents() {
        if (this.getPurchaseOrder() != null) {
            MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
            return mediationService.getMediationRecordsForOrder(this.getPurchaseOrder().getId());
        }
        return new ArrayList<>();
    }

    @Transient
    public List<JbillingMediationRecord> getOrderLineEvents() {
        MediationService mediationService = Context.getBean(MediationService.BEAN_NAME);
        return mediationService.getMediationRecordsForOrderLine(this.id);
    }

    /*
     * Conveniant methods to ease migration from entity beans
     */
    @Transient
    public Integer getItemId() {
        return (getItem() == null) ? null : getItem().getId();
    }

    public void setItemId(Integer itemId) {
        ItemDAS das = new ItemDAS();
        setItem(das.find(itemId));
    }

    @Transient
    public boolean hasItem() {
        return getItem() != null;
    }

    @Transient
    public void addQuantityAndAmount(BigDecimal quantity, BigDecimal amount) {
        if(null == this.quantity) {
            setQuantity(BigDecimal.ZERO);
        }
        if(null == this.amount) {
            setAmount(BigDecimal.ZERO);
        }
        setQuantity(this.quantity.add(quantity, MathContext.DECIMAL128));
        setAmount(this.amount.add(amount, MathContext.DECIMAL128));
        setPrice(this.amount.divide(this.quantity, MathContext.DECIMAL128));
    }

    @Transient
    public void incrementCallCounter() {
        if(null == callCounter) {
            callCounter = 1L;
        }
        ++callCounter;
    }

    @Transient
    public Boolean getEditable() {
        if (editable == null) {
            editable = getOrderLineType().getEditable() == 1;
        }
        return editable;
    }
    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    @Transient
    public String getPriceStr() {
        return priceStr;
    }

    public void setPriceStr(String priceStr) {
        this.priceStr = priceStr;
    }

    @Transient
    public Boolean getTotalReadOnly() {
        if (totalReadOnly == null) {
            setTotalReadOnly(false);
        }
        return totalReadOnly;
    }

    public void setTotalReadOnly(Boolean totalReadOnly) {
        this.totalReadOnly = totalReadOnly;
    }

    @Transient
    public String getProvisioningStatusStr() {
        return provisioningStatusStr;
    }

    public void setProvisioningStatusStr(String provisioningStatusStr) {
        this.provisioningStatusStr = provisioningStatusStr;
    }

    @Transient
    public Integer getTypeId() {
        return getOrderLineType() == null ? null : getOrderLineType().getId();
    }

    public void setTypeId(Integer typeId) {
        OrderLineTypeDAS das = new OrderLineTypeDAS();
        setOrderLineType(das.find(typeId));
    }

    @Transient
    public Integer getQuantityInt() {
        if (quantity == null) {
            return null;
        }
        return this.quantity.intValue();
    }

    /**
     * @return the provisioningStatus
     */
    @Override
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="provisioning_status")
    @OptimisticLock(excluded = true)
    public ProvisioningStatusDTO getProvisioningStatus() {
        return provisioningStatus;
    }

    /**
     * @param provisioningStatus the provisioningStatus to set
     */
    @Override
    public void setProvisioningStatus(ProvisioningStatusDTO provisioningStatus) {
        this.provisioningStatus = provisioningStatus;
    }

    @Override
    @Transient
    public Integer getProvisioningStatusId() {
        return getProvisioningStatus() == null ? null :
            getProvisioningStatus().getId();
    }

    public void setProvisioningStatusId(Integer provisioningStatusId) {
        ProvisioningStatusDAS das = new ProvisioningStatusDAS();
        setProvisioningStatus(das.find(provisioningStatusId));
    }

    /**
     * @return the provisioningRequestId
     */
    @Column(name="provisioning_request_id")
    public String getProvisioningRequestId() {
        return provisioningRequestId;
    }

    /**
     * @param provisioningRequestId the provisioningRequestId to set
     */
    public void setProvisioningRequestId(String provisioningRequestId) {
        this.provisioningRequestId = provisioningRequestId;
    }

    @Column(name = "sip_uri", nullable = true)
    public String getSipUri () {
        return sipUri;
    }

    public void setSipUri (String sipUri) {
        this.sipUri = sipUri;
    }

    public void addExtraFields(Integer languageId) {
        if (getProvisioningStatus() != null) {
            provisioningStatusStr = getProvisioningStatus().getDescription(languageId);
        }
    }

    @OneToMany(cascade= CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="orderLine")
    @Cascade( value= org.hibernate.annotations.CascadeType.DELETE_ORPHAN )
    public Set<OrderLineUsagePoolDTO> getOrderLineUsagePools() {
        return orderLineUsagePools;
    }

    public void setOrderLineUsagePools(Set<OrderLineUsagePoolDTO> orderLineUsagePools) {
        this.orderLineUsagePools = orderLineUsagePools;
    }

    public void addOrderLineUsagePools(Set<OrderLineUsagePoolDTO> orderLineUsagePools) {
        for(OrderLineUsagePoolDTO orderLineUsagePool : orderLineUsagePools) {
            addOrderLineUsagePool(orderLineUsagePool);
        }
    }

    public void addOrderLineUsagePool(OrderLineUsagePoolDTO orderLineUsagePool) {
        orderLineUsagePools.add(orderLineUsagePool);
        orderLineUsagePool.setOrderLine(this);
    }

    @Transient
    public boolean isMediated() {
        return mediated;
    }

    public void setMediated(boolean mediated) {
        this.mediated = mediated;
    }

    @Transient
    public BigDecimal getMediatedQuantity() {
        return mediatedQuantity;
    }

    public void setMediatedQuantity(BigDecimal mediatedQuantity) {
        this.mediatedQuantity = mediatedQuantity;
    }

    @Transient
    public boolean hasOrderLineUsagePools() {
        return null != getOrderLineUsagePools() && !getOrderLineUsagePools().isEmpty();
    }

    @Transient
    public BigDecimal getFreeUsagePoolQuantity() {
        BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
        for (OrderLineUsagePoolDTO olUsagePool : getOrderLineUsagePools()) {
            freeUsagePoolQuantity = freeUsagePoolQuantity.add(olUsagePool.getQuantity());
        }
        return freeUsagePoolQuantity;
    }

    @Transient
    public BigDecimal getFreeUsagePoolQuantity(Integer freeUsagePoolId) {
        BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
        for (OrderLineUsagePoolDTO olUsagePool : getOrderLineUsagePools()) {
            if (olUsagePool.getCustomerUsagePool().getId() == freeUsagePoolId.intValue()) {
                freeUsagePoolQuantity = freeUsagePoolQuantity.add(olUsagePool.getQuantity());
            }
        }
        return freeUsagePoolQuantity;
    }

    @Transient
    public BigDecimal getNonPersistedFreeUsagePoolQuantity() {
        BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
        for (OrderLineUsagePoolDTO olUsagePool : getOrderLineUsagePools()) {
            freeUsagePoolQuantity = freeUsagePoolQuantity.add(olUsagePool.getQuantity());
        }
        return freeUsagePoolQuantity;
    }

    @Transient
    public BigDecimal getCustomerUsagePoolQuantity() {
        BigDecimal customerUsagePoolQuantity = BigDecimal.ZERO;
        if(getPurchaseOrder()!=null){
            List<CustomerUsagePoolDTO> customerUsagePools = getPurchaseOrder().getUser().getCustomer().getCustomerUsagePools();
            for(CustomerUsagePoolDTO customerUsagePool: customerUsagePools) {
                if(customerUsagePool.getCycleEndDate().compareTo(TimezoneHelper.serverCurrentDate()) >= 0) {
                    customerUsagePoolQuantity = customerUsagePoolQuantity.add(customerUsagePool.getQuantity());
                }
            }
        }
        return customerUsagePoolQuantity;
    }

    @Override
    @Transient
    public List<ProvisioningCommandDTO> getProvisioningCommands() {
        List<ProvisioningCommandDTO> commands = new ArrayList<ProvisioningCommandDTO>();
        for (OrderChangeDTO change : getOrderChanges()) {
            commands.addAll(change.getProvisioningCommands());
        }
        return commands;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "orderLine")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<OrderChangeDTO> getOrderChanges() {
        return orderChanges;
    }

    public void setOrderChanges(Set<OrderChangeDTO> orderChanges) {
        this.orderChanges = orderChanges;
    }

    public void addOrderChange(OrderChangeDTO orderChange) {
        orderChanges.add(orderChange);
        orderChange.setOrderLine(this);
    }

    @Column(name = "start_date", nullable = true)
    @Temporal(TemporalType.DATE)
    public Date getStartDate () {
        return startDate;
    }

    public void setStartDate (Date startDate) {
        this.startDate = startDate;
    }

    @Column(name = "end_date", nullable = true)
    @Temporal(TemporalType.DATE)
    public Date getEndDate () {
        return endDate;
    }

    public void setEndDate (Date endDate) {
        this.endDate = endDate;
    }

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "orderLine")
    public List<OrderLineItemizedUsageDTO> getOrderLineItemizedUsages() {
        return orderLineItemizedUsages;
    }

    public void setOrderLineItemizedUsages(List<OrderLineItemizedUsageDTO> orderLineItemizedUsages) {
        this.orderLineItemizedUsages = orderLineItemizedUsages;
    }

    @Transient
    public void addOrderLineItemizedUsage(String separator, BigDecimal amount , BigDecimal quantity) {
        OrderLineItemizedUsageDTO lineUsageInfo = getOrderLineItemizedUsageDTO(this.getId(), separator);
        if(null == lineUsageInfo) {
            lineUsageInfo = new OrderLineItemizedUsageDTO(this, separator);
            this.orderLineItemizedUsages.add(lineUsageInfo);
        }
        lineUsageInfo.addAmountAndQuantity(amount, quantity);
    }

    @Transient
    public OrderLineItemizedUsageDTO getOrderLineItemizedUsageDTO(Integer lineId, String separator) {
        for(OrderLineItemizedUsageDTO usageInfo : getOrderLineItemizedUsages()) {
            if(usageInfo.getSeparator().equals(separator) &&
                    usageInfo.getOrderLine().getId() == lineId) {
                return usageInfo;
            }
        }
        return null;
    }

    public void touch() {
        // touch entity with possible cycle dependencies only once
        if (isTouched) {
            return;
        }
        isTouched = true;

        getCreateDatetime();
        if (getItem() != null) {
            new ItemDAS().findNow(getItemId()).getInternalNumber();
        }
        getEditable();

        for(AssetDTO asset: assets) {
            asset.touch();
        }
        if (getParentLine() != null) {
            getParentLine().touch();
        }
        for (OrderLineDTO childLine : getChildLines()) {
            childLine.touch();
        }

        for(MetaFieldValue value: getMetaFields()) {
            value.touch();
        }

        for (OrderLineUsagePoolDTO olUsagePool : getOrderLineUsagePools()) {
            olUsagePool.touch();
        }

        for (OrderChangeDTO orderChange : getOrderChanges()) {
            orderChange.touch();
        }
    }

    /**
     * Returns trye if the OrderLine is linked to asset with {@code id}
     * @param id
     * @return
     */
    public boolean containsAsset(int id) {
        for(AssetDTO assetDTO : assets) {
            if(assetDTO.getId() == id) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public List<OrderChangeDTO> getOrderChangesSortedByStartDate () {
        List<OrderChangeDTO> sortedchanges = new ArrayList<>(getOrderChanges());
        Collections.sort(sortedchanges, OrderLineChangeDTOStartDateComparator);
        return sortedchanges;
    }
    public static final Comparator<OrderChangeDTO> OrderLineChangeDTOStartDateComparator;

    static {
        OrderLineChangeDTOStartDateComparator = (change1, change2) -> {
            Date charge1Start = change1.getStartDate();
            Date charge2Start = change2.getStartDate();

            // ascending order
            int result = charge1Start.compareTo(charge2Start);
            if (result != 0) {
                return result;
            }
            // same start date case
            return change1.getCreateDatetime().compareTo(change2.getCreateDatetime());
        };
    }

    @Transient
    public List<OrderChangeDTO> getOrderChangesSortedByCreateDateTime () {
        List<OrderChangeDTO> sortedchanges = new ArrayList<>(getOrderChanges());
        Collections.sort(sortedchanges, OrderLineChangeDTOCreateDateTimeComparator);
        return sortedchanges;
    }
    public static final Comparator<OrderChangeDTO> OrderLineChangeDTOCreateDateTimeComparator;

    static {
        OrderLineChangeDTOCreateDateTimeComparator = (change1, change2) -> change1.getCreateDatetime().compareTo(change2.getCreateDatetime());
    }

    @Transient
    public void moveToOtherOrder (OrderDTO otherOrder) {
        this.setPurchaseOrder(otherOrder);
        this.getPurchaseOrder().getLines().add(this);

        for (OrderChangeDTO change : orderChanges) {
            change.setOrder(this.getPurchaseOrder());
        }
    }

    @Transient
    public void setDefaults() {
        if (getCreateDatetime() == null) {
            setCreateDatetime(Calendar.getInstance().getTime());
        }
    }

    @Transient
    public void clearOrderLineUsagePools() {
        if(hasOrderLineUsagePools()) {
            getOrderLineUsagePools().clear();
        }
    }

    // this helps to add lines to the treeSet
    @Override
    public int compareTo(Object o) {
        OrderLineDTO other = (OrderLineDTO) o;
        if (other.getItem() == null || this.getItem() == null) {
            return -1;
        }
        return new Integer(this.getItem().getId()).compareTo(other.getItem().getId());
    }

    @Column(name = "is_Percentage", nullable = false, updatable = true)
    public boolean isPercentage() {
        return isPercentage;
    }

    public void setPercentage(boolean isPercentage) {
        this.isPercentage = isPercentage;
    }

    @Transient
    public boolean isDiscount() {
        return Integer.valueOf(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_DISCOUNT).equals(getTypeId());
    }

    @Transient
    public boolean isTaxQuote() {
        return Integer.valueOf(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_TAX_QUOTE).equals(getTypeId());
    }

    @Transient
    public boolean isLineTypeItem() {
        return Integer.valueOf(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ITEM).equals(getTypeId());
    }

    @Transient
    public boolean isSubscription() {
        return Integer.valueOf(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_SUBSCRIPTION).equals(getTypeId());
    }

    @Transient
    public boolean isPenalty() {
        return Integer.valueOf(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_PENALTY).equals(getTypeId());
    }

    @Transient
    public boolean isAdjustment() {
        return Integer.valueOf(com.sapienter.jbilling.server.util.Constants.ORDER_LINE_TYPE_ADJUSTMENT).equals(getTypeId());
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="orderLine", orphanRemoval = true)
    @OrderBy(clause="id")
    public List<OrderLineTierDTO> getOrderLineTiers() {
        return this.orderLineTiers;
    }

    public void setOrderLineTiers(List<OrderLineTierDTO> orderLineTiers) {
        this.orderLineTiers = orderLineTiers;
    }

    @Transient
    public boolean hasOrderLineTiers() {
        return null != getOrderLineTiers() && !getOrderLineTiers().isEmpty();
    }

    @Transient
    public OrderLineTierDTO getOrderLineTierByTierNumber(int tierNumber) {
        if(null != this.getOrderLineTiers()) {
            for (OrderLineTierDTO oltier : this.getOrderLineTiers()) {
                if (null != oltier.getTierNumber() && oltier.getTierNumber().intValue() == tierNumber) {
                    return oltier;
                }
            }
            return null;
        }
        return null;
    }

    @Transient
    public OrderLineInfo getOldOrderLineInfo() {
        return oldOrderLineInfo;
    }

    public void setOldOrderLineInfo(OrderLineInfo oldOrderLineInfo) {
        this.oldOrderLineInfo = oldOrderLineInfo;
    }

    @Transient
    public void createOrUpdateOrderLineUsagePool(CustomerUsagePoolDTO customerUsagePool, BigDecimal freeQuantity, Date effectiveDate) {
        OrderLineUsagePoolDTO orderLineUsagePoolDTO = getOrderLineUsagePoolByCustomerUsagePoolId(customerUsagePool.getId());
        if(null == orderLineUsagePoolDTO) {
            orderLineUsagePoolDTO = new OrderLineUsagePoolDTO();
            orderLineUsagePoolDTO.setCustomerUsagePool(customerUsagePool);
            orderLineUsagePoolDTO.setQuantity(BigDecimal.ZERO);
            orderLineUsagePoolDTO.setEffectiveDate(effectiveDate);
            orderLineUsagePoolDTO.setOrderLine(this);
            getOrderLineUsagePools().add(orderLineUsagePoolDTO);
        }
        orderLineUsagePoolDTO.setQuantity(orderLineUsagePoolDTO.getQuantity()
                .add(freeQuantity, MathContext.DECIMAL128));
    }

    @Transient
    public OrderLineUsagePoolDTO getOrderLineUsagePoolByCustomerUsagePoolId(Integer customerUsagePoolId) {
        for(OrderLineUsagePoolDTO orderLineUsagePool : getOrderLineUsagePools()) {
            if(orderLineUsagePool.getCustomerUsagePool().getId() == customerUsagePoolId) {
                return orderLineUsagePool;
            }
        }
        return null;
    }
    @Override
    public String toString() {
        return "OrderLine:[id=" + id +
                " orderLineType=" + ((orderLineTypeDTO == null) ? "null" : orderLineTypeDTO.getId()) +
                " item=" +  ((item==null) ? "null" : item.getId()) +
                " order id=" + ((orderDTO == null) ? "null" : orderDTO.getId()) +
                " amount=" +  amount +
                " quantity=" +  quantity +
                " price=" +  price +
                " isPercentage=" +  isPercentage +
                " createDatetime=" +  createDatetime +
                " deleted=" + deleted  +
                " useItem=" + useItem +
                " description=" + description +
                " versionNum=" + versionNum  +
                " provisioningStatus=" + provisioningStatus  +
                " provisionningRequestId=" + provisioningRequestId  +
                " parentLineId=" + (parentLine != null ? parentLine.getId() : "null")  +
                " metaFields=" + getMetaFieldsList() +
                " orderLineUsagePools=" + getOrderLineUsagePools() +
                " editable=" + editable + "]";
    }


    @Override
    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getPurchaseOrder().getUser().getCompany().getId())
        .append("-usr-")
        .append(getPurchaseOrder().getUser().getId())
        .append("-ord-")
        .append(getPurchaseOrder().getId())
        .append("-")
        .append(id);

        return key.toString();
    }

    @Transient
    public List<Integer> getAssetIds() {
        return assets.stream()
                .map(AssetDTO::getId)
                .sorted()
                .collect(Collectors.toList());
    }

}
