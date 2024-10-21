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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
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
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.SortComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sapienter.jbilling.common.SessionInternalError;
import com.sapienter.jbilling.server.audit.Auditable;
import com.sapienter.jbilling.server.discount.db.DiscountLineDTO;
import com.sapienter.jbilling.server.invoice.InvoiceBL;
import com.sapienter.jbilling.server.invoice.db.InvoiceDTO;
import com.sapienter.jbilling.server.item.PricingField;
import com.sapienter.jbilling.server.item.db.AssetAssignmentDTO;
import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.item.db.PlanDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.OrderStatusFlag;
import com.sapienter.jbilling.server.process.db.BillingProcessDTO;
import com.sapienter.jbilling.server.provisioning.db.IProvisionable;
import com.sapienter.jbilling.server.provisioning.db.OrderProvisioningCommandDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningStatusDAS;
import com.sapienter.jbilling.server.provisioning.db.ProvisioningStatusDTO;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDAS;
import com.sapienter.jbilling.server.usagePool.db.CustomerUsagePoolDTO;
import com.sapienter.jbilling.server.user.UserCodeAssociate;
import com.sapienter.jbilling.server.user.db.UserCodeLinkDTO;
import com.sapienter.jbilling.server.user.db.UserCodeOrderLinkDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.Util;
import com.sapienter.jbilling.server.util.csv.Exportable;
import com.sapienter.jbilling.server.util.db.CurrencyDTO;
import com.sapienter.jbilling.server.util.time.PeriodUnit;

@Entity
@org.hibernate.annotations.Entity(dynamicUpdate = true)
@TableGenerator(
        name="purchase_order_GEN",
        table="jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue="purchase_order",
        allocationSize = 100
        )
@Table(name="purchase_order")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderDTO extends CustomizedEntity implements Serializable, Exportable, UserCodeAssociate<UserCodeOrderLinkDTO>
, IProvisionable, Auditable {

    private static Logger logger = LoggerFactory.getLogger(OrderDTO.class);

    private Integer id;
    private UserDTO baseUserByUserId;
    private UserDTO baseUserByCreatedBy;
    private CurrencyDTO currencyDTO;
    private OrderStatusDTO orderStatusDTO;
    private OrderPeriodDTO orderPeriod;
    private OrderBillingTypeDTO orderBillingType;
    private OrderDTO primaryOrderDTO;
    private Date activeSince;
    private Date activeUntil;
    private Date finishedDate;
    private Date deletedDate;
    private Date cycleStarts;
    private Date createDate;
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
    // reseller entity order id
    private Integer resellerOrder;
    private String notes;
    private Integer notesInInvoice;
    private Set<OrderProcessDTO> orderProcesses = new HashSet<>(0);
    private List<OrderLineDTO> lines = new ArrayList<>(0);
    private List<DiscountLineDTO> discountLines = new ArrayList<>(0);
    private Set<UserCodeOrderLinkDTO> userCodeLinks = new HashSet<>(0);

    private Integer versionNum;
    private OrderDTO parentOrder;
    private Set<OrderDTO> childOrders = new HashSet<>(0);

    private List<OrderProvisioningCommandDTO> provisioningCommands = new ArrayList<>(0);
    private ProvisioningStatusDTO provisioningStatus;

    // other non-persitent fields
    private Collection<OrderProcessDTO> nonReviewPeriods = new ArrayList<>(0);
    private Collection<InvoiceDTO> invoices = new ArrayList<>(0);
    private Collection<BillingProcessDTO> billingProcesses = new ArrayList<>(0);
    private String periodStr = null;
    private String billingTypeStr = null;
    private String statusStr = null;
    private String timeUnitStr = null;
    private String currencySymbol = null;
    private String currencyName = null;
    private BigDecimal total = null;
    private List<PricingField> pricingFields = null;
    private String cancellationFeeType;
    private Integer cancellationFee;
    private Integer cancellationFeePercentage;
    private Integer cancellationMaximumFee;
    private Integer cancellationMinimumPeriod;
    private boolean isTouched = false;
    private BigDecimal freeUsageQuantity;

    private Boolean prorateFlag = Boolean.FALSE;
    private Integer planItemId;
    private Boolean prorateAdjustmentFlag = Boolean.FALSE;
    private Boolean isMediated = Boolean.FALSE;
    private OrderExportableWrapper orderExportableWrapper;
    private boolean autoRenew = false;
    private Integer renewNotification = 1;
    private Integer upgradeOrderId;
    private Integer renewOrderId;
    private Integer parentUpgradeOrderId;

    public OrderDTO() {
    }

    public OrderDTO(OrderDTO other) {
        init(other);
    }

    public void init(OrderDTO other) {
        this.id = other.getId();
        this.baseUserByUserId = other.getBaseUserByUserId();
        this.baseUserByCreatedBy = other.getBaseUserByCreatedBy();
        this.currencyDTO = other.getCurrency();
        this.orderStatusDTO = other.getOrderStatus();
        this.orderPeriod = other.getOrderPeriod();
        this.orderBillingType = other.getOrderBillingType();
        this.activeSince = other.getActiveSince();
        this.activeUntil = other.getActiveUntil();
        this.finishedDate = other.getFinishedDate();
        this.createDate = other.getCreateDate();
        this.nextBillableDay = other.getNextBillableDay();
        this.deleted = other.getDeleted();
        this.notify = other.getNotify();
        this.lastNotified = other.getLastNotified();
        this.notificationStep = other.getNotificationStep();
        this.dueDateUnitId = other.getDueDateUnitId();
        this.dueDateValue = other.getDueDateValue();
        this.dfFm = other.getDfFm();
        this.anticipatePeriods = other.getAnticipatePeriods();
        this.ownInvoice = other.getOwnInvoice();
        this.notes = other.getNotes();
        this.notesInInvoice = other.getNotesInInvoice();
        this.orderProcesses.addAll(other.getOrderProcesses());
        for (OrderLineDTO line: other.getLines()) {
            this.lines.add(new OrderLineDTO(line));
        }
        for (DiscountLineDTO discountLine: other.getDiscountLines()) {
            this.discountLines.add(new DiscountLineDTO(discountLine));
        }
        this.versionNum = other.getVersionNum();
        this.pricingFields = other.getPricingFields();
        this.cancellationFeeType = other.getCancellationFeeType();
        this.cancellationFee = other.getCancellationFee();
        this.cancellationFeePercentage = other.getCancellationFeePercentage();
        this.cancellationMaximumFee = other.getCancellationMaximumFee();
        this.parentOrder = other.getParentOrder();
        for (OrderDTO childOrder : other.getChildOrders()) {
            this.childOrders.add(new OrderDTO(childOrder));
        }
        this.userCodeLinks = other.userCodeLinks;
        this.setFreeUsageQuantity(other.getFreeUsageQuantity());
        this.prorateFlag = (null != other.getProrateFlag() ? other.getProrateFlag() : false);
        this.prorateAdjustmentFlag = (null != other.getProrateAdjustmentFlag() ? other.getProrateAdjustmentFlag() : false);
        this.upgradeOrderId = other.getUpgradeOrderId();
        this.renewOrderId = other.getRenewOrderId();
        this.parentUpgradeOrderId = other.getParentUpgradeOrderId();
        this.autoRenew = other.isAutoRenew();
        this.renewNotification = other.getRenewNotification();
    }

    public OrderDTO(int id, UserDTO baseUserByCreatedBy, CurrencyDTO currencyDTO, OrderStatusDTO orderStatusDTO, OrderBillingTypeDTO orderBillingTypeDTO, OrderDTO primaryOrderDTO, Date createDatetime, Integer deleted) {
        this.id = id;
        this.baseUserByCreatedBy = baseUserByCreatedBy;
        this.currencyDTO = currencyDTO;
        this.orderStatusDTO = orderStatusDTO;
        this.orderBillingType = orderBillingTypeDTO;
        this.primaryOrderDTO = primaryOrderDTO;
        this.createDate = createDatetime;
        this.deleted = deleted;
    }
    public OrderDTO(int id, UserDTO baseUserByUserId, UserDTO baseUserByCreatedBy, CurrencyDTO currencyDTO,
            OrderStatusDTO orderStatusDTO, OrderPeriodDTO orderPeriod,
            OrderBillingTypeDTO orderBillingTypeDTO, OrderDTO primaryOrderDTO, Date activeSince, Date activeUntil, Date createDatetime,
            Date nextBillableDay, Integer deleted, Integer notify, Date lastNotified, Integer notificationStep,
            Integer dueDateUnitId, Integer dueDateValue, Integer dfFm, Integer anticipatePeriods,
            Integer ownInvoice, String notes, Integer notesInInvoice, Set<OrderProcessDTO> orderProcesses,
            List<OrderLineDTO> orderLineDTOs, List<DiscountLineDTO> discountLineDTOs, Boolean prorateFlag, Boolean prorateAdjustmentFlag) {
        this.id = id;
        this.baseUserByUserId = baseUserByUserId;
        this.baseUserByCreatedBy = baseUserByCreatedBy;
        this.currencyDTO = currencyDTO;
        this.orderStatusDTO = orderStatusDTO;
        this.orderPeriod = orderPeriod;
        this.orderBillingType = orderBillingTypeDTO;
        this.primaryOrderDTO = primaryOrderDTO;
        this.activeSince = activeSince;
        this.activeUntil = activeUntil;
        this.createDate = createDatetime;
        this.nextBillableDay = nextBillableDay;
        this.deleted = deleted;
        this.notify = notify;
        this.lastNotified = lastNotified;
        this.notificationStep = notificationStep;
        this.dueDateUnitId = dueDateUnitId;
        this.dueDateValue = dueDateValue;
        this.dfFm = dfFm;
        this.anticipatePeriods = anticipatePeriods;
        this.ownInvoice = ownInvoice;
        this.notes = notes;
        this.notesInInvoice = notesInInvoice;
        this.orderProcesses = orderProcesses;
        this.lines = orderLineDTOs;
        this.discountLines = discountLineDTOs;
        this.prorateFlag = prorateFlag;
        this.prorateAdjustmentFlag = prorateAdjustmentFlag;
    }

    @Id
    @GeneratedValue(strategy= GenerationType.TABLE, generator="purchase_order_GEN")
    @Column(name="id", unique=true, nullable=false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    public UserDTO getBaseUserByUserId() {
        return this.baseUserByUserId;
    }
    public void setBaseUserByUserId(UserDTO baseUserByUserId) {
        this.baseUserByUserId = baseUserByUserId;
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="created_by")
    public UserDTO getBaseUserByCreatedBy() {
        return this.baseUserByCreatedBy;
    }

    public void setBaseUserByCreatedBy(UserDTO baseUserByCreatedBy) {
        this.baseUserByCreatedBy = baseUserByCreatedBy;
    }
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="currency_id", nullable=false)
    public CurrencyDTO getCurrency() {
        return this.currencyDTO;
    }

    public void setCurrency(CurrencyDTO currencyDTO) {
        this.currencyDTO = currencyDTO;
    }
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="status_id", nullable=false)
    public OrderStatusDTO getOrderStatus() {
        return this.orderStatusDTO;
    }

    public void setOrderStatus(OrderStatusDTO orderStatusDTO) {
        this.orderStatusDTO = orderStatusDTO;
    }

    @Transient
    public boolean isSuspended() {
        return OrderStatusFlag.SUSPENDED_AGEING.equals(this.getOrderStatus().getOrderStatusFlag());
    }

    @Transient
    public boolean isFinished() {
        return OrderStatusFlag.FINISHED.equals(this.getOrderStatus().getOrderStatusFlag());
    }

    @Transient
    public boolean isActive() {
        return OrderStatusFlag.INVOICE.equals(this.getOrderStatus().getOrderStatusFlag());
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="period_id")
    public OrderPeriodDTO getOrderPeriod() {
        return this.orderPeriod;
    }
    public void setOrderPeriod(OrderPeriodDTO orderPeriodDTO) {
        this.orderPeriod = orderPeriodDTO;
    }

    public void setOrderPeriodId(Integer id) {
        if (id != null) {
            setOrderPeriod(new OrderPeriodDAS().find(id));
        } else {
            setOrderPeriod(null);
        }
    }

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="billing_type_id", nullable=false)
    public OrderBillingTypeDTO getOrderBillingType() {
        return this.orderBillingType;
    }

    public void setOrderBillingType(OrderBillingTypeDTO orderBillingTypeDTO) {
        this.orderBillingType = orderBillingTypeDTO;
    }

    public boolean hasPrimaryOrder() {
        return this.parentOrder != null;
    }

    @Column(name="active_since", length=13)
    public Date getActiveSince() {
        return this.activeSince;
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = activeSince;
    }

    @Column(name="active_until", length=13)
    public Date getActiveUntil() {
        return this.activeUntil;
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = activeUntil;
    }

    @Column(name="finished_date", length=13)
    public Date getFinishedDate() {
        return finishedDate;
    }

    public void setFinishedDate(Date finishedDate) {
        this.finishedDate = finishedDate;
    }

    @Column(name="deleted_date", length=13)
    public Date getDeletedDate() {
        return this.deletedDate;
    }

    public void setDeletedDate(Date deletedDate) {
        this.deletedDate = deletedDate;
    }

    @Column(name="create_datetime", nullable=false, length=29)
    public Date getCreateDate() {
        return this.createDate;
    }

    public void setCreateDate(Date createDatetime) {
        this.createDate = createDatetime;
    }
    @Column(name="next_billable_day", length=29)
    public Date getNextBillableDay() {
        return this.nextBillableDay;
    }

    public void setNextBillableDay(Date nextBillableDay) {
        this.nextBillableDay = nextBillableDay;
    }

    @Transient
    public Date getBillingStartDate() {
        return null != this.nextBillableDay ? this.nextBillableDay : this.activeSince;
    }

    @Column(name="deleted", nullable=false)
    public int getDeleted() {
        return this.deleted;
    }

    public void setDeleted(int deleted) {
        this.deleted = deleted;
    }

    @Column(name="notify")
    public Integer getNotify() {
        return this.notify;
    }

    public void setNotify(Integer notify) {
        this.notify = notify;
    }
    @Column(name="last_notified", length=29)
    public Date getLastNotified() {
        return this.lastNotified;
    }

    public void setLastNotified(Date lastNotified) {
        this.lastNotified = lastNotified;
    }

    @Column(name="notification_step")
    public Integer getNotificationStep() {
        return this.notificationStep;
    }

    public void setNotificationStep(Integer notificationStep) {
        this.notificationStep = notificationStep;
    }

    @Column(name="due_date_unit_id")
    public Integer getDueDateUnitId() {
        return this.dueDateUnitId;
    }

    public void setDueDateUnitId(Integer dueDateUnitId) {
        this.dueDateUnitId = dueDateUnitId;
    }

    @Column(name="due_date_value")
    public Integer getDueDateValue() {
        return this.dueDateValue;
    }

    public void setDueDateValue(Integer dueDateValue) {
        this.dueDateValue = dueDateValue;
    }

    @Column(name="df_fm")
    public Integer getDfFm() {
        return this.dfFm;
    }

    public void setDfFm(Integer dfFm) {
        this.dfFm = dfFm;
    }

    @Column(name="anticipate_periods")
    public Integer getAnticipatePeriods() {
        return this.anticipatePeriods;
    }

    public void setAnticipatePeriods(Integer anticipatePeriods) {
        this.anticipatePeriods = anticipatePeriods;
    }

    @Column(name="own_invoice")
    public Integer getOwnInvoice() {
        return this.ownInvoice;
    }

    public void setOwnInvoice(Integer ownInvoice) {
        this.ownInvoice = ownInvoice;
    }

    @Column(name="notes", length=200)
    public String getNotes() {
        return this.notes;
    }

    public void setNotes(String notes) {
        // make sure this is fits in the DB
        if (notes == null || notes.length() <= 200) {
            this.notes = notes;
        } else {
            this.notes = notes.substring(0, 200);
            logger.warn("Trimming notes to 200 lenght: from {} to {}", notes, this.notes);
        }
    }

    @Column(name="notes_in_invoice")
    public Integer getNotesInInvoice() {
        return this.notesInInvoice;
    }

    public void setNotesInInvoice(Integer notesInInvoice) {
        this.notesInInvoice = notesInInvoice;
    }

    /*
     * There might potentially hundreds of process records, but they are not read by the app.
     * They are only taken for display, and then all are needed
     */
    @ElementCollection
    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="purchaseOrder")
    @OrderBy (
            clause = "id desc"
            )
    public Set<OrderProcessDTO> getOrderProcesses() {
        return this.orderProcesses;
    }

    public void setOrderProcesses(Set<OrderProcessDTO> orderProcesses) {
        this.orderProcesses = orderProcesses;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="purchaseOrder")
    @OrderBy(clause="id")
    public List<OrderLineDTO> getLines() {
        return this.lines;
    }

    public void setLines(List<OrderLineDTO> orderLineDTOs) {
        this.lines = orderLineDTOs;
    }

    @Column(name="reseller_order", updatable = false)
    public Integer getResellerOrder() {
        return resellerOrder;
    }

    public void setResellerOrder(Integer resellerOrder) {
        this.resellerOrder = resellerOrder;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="purchaseOrder")
    @OrderBy(clause="id")
    public List<DiscountLineDTO> getDiscountLines() {
        return discountLines;
    }

    public void setDiscountLines(List<DiscountLineDTO> discountLines) {
        this.discountLines = discountLines;
    }

    public boolean hasDiscountLines() {
        return getDiscountLines() != null && !getDiscountLines().isEmpty();
    }

    @Version
    @Column(name="OPTLOCK")
    public Integer getVersionNum() {
        return versionNum;
    }
    public void setVersionNum(Integer versionNum) {
        this.versionNum = versionNum;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_order_id")
    public OrderDTO getParentOrder() {
        return parentOrder;
    }

    public void setParentOrder(OrderDTO parentOrder) {
        this.parentOrder = parentOrder;
    }

    @OneToMany(cascade = {}, fetch = FetchType.LAZY, mappedBy = "parentOrder")
    public Set<OrderDTO> getChildOrders() {
        return childOrders;
    }

    public void setChildOrders(Set<OrderDTO> childOrders) {
        this.childOrders = childOrders;
    }

    @Override
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "order_meta_field_map",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
            )
    @SortComparator(value = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Column(name = "prorate_flag", nullable = false)
    public Boolean getProrateFlag() {
        return prorateFlag;
    }

    public void setProrateFlag(Boolean prorateFlag) {
        this.prorateFlag = prorateFlag;
    }

    @Column(name = "prorate_adjustment_flag", nullable = false)
    public Boolean getProrateAdjustmentFlag() {
        return prorateAdjustmentFlag;
    }

    public void setProrateAdjustmentFlag(Boolean prorateAdjustmentFlag) {
        this.prorateAdjustmentFlag = prorateAdjustmentFlag;
    }

    @Column(name = "is_mediated", nullable = false)
    public Boolean getIsMediated() {
        return isMediated;
    }

    public void setIsMediated(Boolean isMediated) {
        this.isMediated = isMediated;
    }

    @Column(name = "upgrade_order_id", nullable = true)
    public Integer getUpgradeOrderId() {
        return upgradeOrderId;
    }

    public void setUpgradeOrderId(Integer upgradeOrderId) {
        this.upgradeOrderId = upgradeOrderId;
    }

    @Column(name = "renew_order_id", nullable = true)
    public Integer getRenewOrderId() {
        return renewOrderId;
    }

    public void setRenewOrderId(Integer renewOrderId) {
        this.renewOrderId = renewOrderId;
    }

    @Column(name = "parent_upgrade_order_id", nullable = true)
    public Integer getParentUpgradeOrderId() {
        return parentUpgradeOrderId;
    }

    public void setParentUpgradeOrderId(Integer parentUpgradeOrderId) {
        this.parentUpgradeOrderId = parentUpgradeOrderId;
    }

    @Column(name = "auto_renew")
    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    @Column(name = "renew_notification")
    public Integer getRenewNotification() {
        return renewNotification;
    }

    public void setRenewNotification(Integer renew_notification) {
        this.renewNotification = renew_notification;
    }

    @Transient
    public boolean getProrateFlagValue() {
        return null != prorateFlag && prorateFlag.booleanValue();
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[] { EntityType.ORDER };
    }

    /*
     * Conveniant methods to ease migration from entity beans
     */
    @Transient
    public Integer getBillingTypeId() {
        return getOrderBillingType() == null ? null : getOrderBillingType().getId();
    }

    @Transient
    public boolean isPostPaid() {
        return getBillingTypeId().compareTo(Constants.ORDER_BILLING_POST_PAID) == 0;
    }
    
    @Transient
    public boolean isPrePaid() {
        return getBillingTypeId().compareTo(Constants.ORDER_BILLING_PRE_PAID) == 0;
    }

    @Transient
    public Integer getStatusId() {
        return getOrderStatus() == null ? null : getOrderStatus().getId();
    }
    public void setStatusId(Integer statusId) {
        if (statusId == null) {
            setOrderStatus(null);
            return;
        }
        OrderStatusDTO dto = new OrderStatusDTO();
        dto.setId(statusId);
        setOrderStatus(dto);
    }

    @Transient
    public Integer getCurrencyId() {
        return getCurrency().getId();
    }
    public void setCurrencyId(Integer currencyId) {
        if (currencyId == null) {
            setCurrency(null);
        } else {
            CurrencyDTO currency = new CurrencyDTO(currencyId);
            setCurrency(currency);
        }
    }

    @Transient
    public UserDTO getUser() {
        return getBaseUserByUserId();
    }

    @Transient
    public BigDecimal getTotal() {
        total = new BigDecimal("0").setScale(Constants.BIGDECIMAL_SCALE, Constants.BIGDECIMAL_ROUND);
        if(getLines() != null){
            for(OrderLineDTO line: getLines()) {
                if(line.getDeleted() == 0) {
                    total = total.add(line.getAmount());
                }
            }
        }

        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Transient
    // all the periods, but excluding those from process reviews
    public Collection<OrderProcessDTO> getPeriods() {
        return nonReviewPeriods;
    }

    @Transient
    public Collection<InvoiceDTO> getInvoices() {
        return invoices;

    }

    @Transient
    public String getPeriodStr() {
        return periodStr;
    }
    public void setPeriodStr(String str) {
        periodStr = str;
    }

    @Transient
    public String getBillingTypeStr() {
        return billingTypeStr;
    }
    public void setBillingTypeStr(String str) {
        this.billingTypeStr = str;
    }

    @Transient
    public String getStatusStr() {
        return statusStr;
    }

    @Transient
    public String getTimeUnitStr() {
        return timeUnitStr;
    }

    @Transient
    public String getCurrencyName() {
        return currencyName;
    }

    @Transient
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void addExtraFields(Integer languageId) {
        invoices = new ArrayList<InvoiceDTO>();
        billingProcesses = new ArrayList<BillingProcessDTO>();
        nonReviewPeriods = new ArrayList<OrderProcessDTO>();

        for (OrderProcessDTO process: getOrderProcesses()) {
            if (process.getIsReview() == 1) {
                continue;
            }
            nonReviewPeriods.add(process);

            try {
                InvoiceBL invoiceBl = new InvoiceBL(process.getInvoice().getId());
                invoices.add(invoiceBl.getDTO());
            } catch (Exception e) {
                throw new SessionInternalError(e);
            }

            billingProcesses.add(process.getBillingProcess());
        }

        periodStr = getOrderPeriod().getDescription(languageId);
        billingTypeStr = getOrderBillingType().getDescription(languageId);
        statusStr = getOrderStatus().getDescription(languageId);
        timeUnitStr = Util.getPeriodUnitStr(
                getDueDateUnitId(), languageId);

        currencySymbol = getCurrency().getSymbol();
        currencyName = getCurrency().getDescription(languageId);

        for (OrderLineDTO line : getLines()) {
            line.addExtraFields(languageId);
        }
    }

    @Transient
    public Integer getPeriodId() {
        return getOrderPeriod().getId();
    }

    @Transient
    public Integer getUserId() {
        return (getBaseUserByUserId() == null) ? null : getBaseUserByUserId().getId();
    }

    @Transient
    public Integer getCreatedBy() {
        return (getBaseUserByCreatedBy() == null) ? null : getBaseUserByCreatedBy().getId();
    }

    @Transient
    public OrderLineDTO getLine(Integer itemId) {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().getId() == itemId) {
                return line;
            }
        }

        return null;
    }

    @Transient
    public OrderLineDTO getOldLine(Integer itemId) {
        for (OrderLineDTO line : lines) {
            // Added line.getId() != 0 check as part of creatOrder api fix.
            if (line.getId() != 0 && line.getDeleted() == 0 && line.getItem() != null && line.getItem().getId() == itemId) {
                return line;
            }
        }

        return null;
    }

    @Transient
    public int getLineCountByItemId(Integer itemId) {
        return getLinesByItemId(itemId).size();
    }

    @Transient
    public List<OrderLineDTO> getLinesByItemId(Integer itemId) {
        List<OrderLineDTO> result = new ArrayList<>();
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().getId() == itemId) {
                result.add(line);
            }
        }
        return result;
    }

    @Transient
    public OrderLineDTO getLineByCreateDateTime(Date createDateTime) {
        OrderLineDTO line = null;
        for(OrderLineDTO orderLine: getLines()) {
            if(orderLine.getCreateDatetime().equals(createDateTime)) {
                line = orderLine;
                break;
            }
        }
        return line;
    }

    @Transient
    public OrderLineDTO getLine(Integer itemId, String callIdentifier) {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0 && line.getItem() != null && line.getItem().getId() == itemId) {
                String phoneNumber = line.getCallIdentifier();
                if (null != phoneNumber && callIdentifier.equals(phoneNumber)) {
                    return line;
                }
            }
        }

        return null;
    }

    @Transient
    public OrderLineDTO getLineById(Integer lineId) {
        for (OrderLineDTO line : lines) {
            if (line.getDeleted() == 0 && line.getId() == lineId) {
                return line;
            }
        }

        return null;
    }

    @Transient
    public void removeLine(Integer itemId) {
        OrderLineDTO line = getLine(itemId);
        if (line != null) {
            lines.remove(line);
        }
    }

    @Transient
    public void removeLineById(Integer lineId) {
        OrderLineDTO line = getLineById(lineId);
        if (line != null) {
            lines.remove(line);
        }
    }

    @Transient
    public boolean isEmpty() {
        return lines.isEmpty();
    }

    @Transient
    public int getNumberOfLines() {
        int count = 0;
        for (OrderLineDTO line: getLines()) {
            if (line.getDeleted() == 0) {
                count++;
            }
        }
        return count;
    }

    @Transient
    public boolean hasLinePresent(Integer lineId) {
        if(lines.isEmpty()) {
            return false;
        }

        return lines.stream()
                .filter(line -> line.getId() == lineId)
                .findAny()
                .isPresent();
    }

    @Override
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<UserCodeOrderLinkDTO> getUserCodeLinks() {
        return userCodeLinks;
    }

    public void setUserCodeLinks(Set<UserCodeOrderLinkDTO> userCodes) {
        this.userCodeLinks = userCodes;
    }

    @Override
    public void addUserCodeLink(UserCodeOrderLinkDTO dto) {
        dto.setOrder(this);
        userCodeLinks.add(dto);
    }

    @Transient
    public List<PricingField> getPricingFields() {
        return this.pricingFields;
    }

    public void setPricingFields(List<PricingField> fields) {
        this.pricingFields = fields;
    }

    // default values
    @Transient
    public void setDefaults(Integer entityId) {
        if (getCreateDate() == null) {
            setCreateDate(Calendar.getInstance().getTime());
            setDeleted(0);
        }
        if (getOrderStatus() == null) {
            OrderStatusDAS orderStatusDAS = new OrderStatusDAS();
            setOrderStatus(orderStatusDAS.find(orderStatusDAS.getDefaultOrderStatusId(OrderStatusFlag.INVOICE, entityId)));
        }
        for (OrderLineDTO line : lines) {
            line.setDefaults();
        }
    }

    /**
     * Makes sure that all the proxies are loaded, so no session is needed to
     * use the pojo
     */
    public void touch() {
        // touch entity with possible cycle dependencies only once
        if (isTouched) {
            return;
        }
        isTouched = true;

        getActiveSince();
        if (getBaseUserByUserId() != null) {
            getBaseUserByUserId().getCreateDatetime();
        }
        if (getBaseUserByCreatedBy() != null) {
            getBaseUserByCreatedBy().getCreateDatetime();
        }
        for (OrderLineDTO line: getLines()) {
            line.touch();
        }
        for (DiscountLineDTO discountLine: getDiscountLines()) {
            discountLine.getDiscount();
        }
        for (InvoiceDTO invoice: getInvoices()) {
            invoice.getCreateDatetime();
        }
        for (OrderProcessDTO process: getOrderProcesses()) {
            process.getPeriodStart();
        }
        for (MetaFieldValue metaField : this.getMetaFields()) {
            metaField.touch();
        }
        if (getOrderBillingType() != null) {
            getOrderBillingType().getId();
        }
        if (getOrderPeriod() != null) {
            getOrderPeriod().getId();
        }
        if (getOrderStatus() != null) {
            getOrderStatus().getId();
        }
        if (getParentOrder() != null) {
            getParentOrder().touch();
        }
        for (OrderDTO childOrder : getChildOrders()) {
            childOrder.touch();
        }

        for(UserCodeLinkDTO userCodeLink : getUserCodeLinks()) {
            userCodeLink.touch();
        }
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer("Order = " +
                "id=" + id + "," +
                "baseUserByUserId=" + ((baseUserByUserId == null) ? null : baseUserByUserId.getId()) + "," +
                "baseUserByCreatedBy=" + ((baseUserByCreatedBy== null) ? null : baseUserByCreatedBy.getId()) + "," +
                "currencyDTO=" + currencyDTO + "," +
                "orderStatusDTO=" + ((orderStatusDTO == null) ? null : orderStatusDTO.getId()) + "," +
                "orderPeriodDTO=" + ((orderPeriod == null) ? null : orderPeriod.getId()) + "," +
                "orderBillingTypeDTO=" + ((orderBillingType == null) ? null : orderBillingType.getId()) + "," +
                "primaryOrderDTO=" + ((primaryOrderDTO == null) ? null : primaryOrderDTO.getId()) + "," +
                "activeSince=" + activeSince + "," +
                "activeUntil=" + activeUntil + "," +
                "createDate=" + createDate + "," +
                "nextBillableDay=" + nextBillableDay + "," +
                "deleted=" + deleted + "," +
                "notify=" + notify + "," +
                "lastNotified=" + lastNotified + "," +
                "notificationStep=" + notificationStep + "," +
                "dueDateUnitId=" + dueDateUnitId + "," +
                "dueDateValue=" + dueDateValue + "," +
                "dfFm=" + dfFm + "," +
                "anticipatePeriods=" + anticipatePeriods + "," +
                "ownInvoice=" + ownInvoice + "," +
                "notes=" + notes + "," +
                "notesInInvoice=" + notesInInvoice + "," +
                "versionNum=" + versionNum +
                " freeUsageQuantity=" +  freeUsageQuantity +
                " prorateFlag=" +  prorateFlag +
                " isMediated=" +  isMediated +
                " lines:[");

        for (OrderLineDTO line: getLines()) {
            str.append(line.getId() + "-");
        }
        str.append(']');
        return str.toString();
    }

    @Transient
    public Date getPricingDate() {
        Date billingDate = getActiveSince();
        if (billingDate == null) {
            billingDate = getCreateDate();
        }
        return billingDate;
    }

    @Column(name = "cancellation_fee_type")
    public String getCancellationFeeType() {
        return cancellationFeeType;
    }

    public void setCancellationFeeType(String cancellationFeeType) {
        this.cancellationFeeType = cancellationFeeType;
    }

    @Column(name = "cancellation_fee")
    public Integer getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(Integer cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    @Column(name = "cancellation_fee_percentage")
    public Integer getCancellationFeePercentage() {
        return cancellationFeePercentage;
    }

    public void setCancellationFeePercentage(Integer cancellationFeePercentage) {
        this.cancellationFeePercentage = cancellationFeePercentage;
    }

    @Column(name = "cancellation_maximum_fee")
    public Integer getCancellationMaximumFee(){
        return cancellationMaximumFee;
    }

    public void setCancellationMaximumFee(Integer cancellationMaximumFee){
        this.cancellationMaximumFee = cancellationMaximumFee;
    }

    @Column(name = "cancellation_minimum_period")
    public Integer getCancellationMinimumPeriod() {
        return cancellationMinimumPeriod;
    }

    public void setCancellationMinimumPeriod(Integer cancellationMinimumPeriod) {
        this.cancellationMinimumPeriod = cancellationMinimumPeriod;
    }

    @Override
    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getUser().getCompany().getId())
        .append("-usr-")
        .append(getUser().getId())
        .append("-")
        .append(id);

        return key.toString();
    }

    @Override
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = javax.persistence.CascadeType.ALL)
    @SortComparator(value = ProvisioningCommandDTO.ProvisioningCommandComparator.class)
    public List<OrderProvisioningCommandDTO> getProvisioningCommands() {
        return provisioningCommands;
    }

    public void setProvisioningCommands(List<OrderProvisioningCommandDTO> provisioningCommands) {
        this.provisioningCommands = provisioningCommands;
    }

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

    @Column(name="cycle_start", length=29)
    public Date getCycleStarts() {
        return cycleStarts;
    }

    public void setCycleStarts(Date cycleStarts) {
        this.cycleStarts = cycleStarts;
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

    @Column(name="free_usage_quantity", precision=17, scale=17)
    public BigDecimal getFreeUsageQuantity() {
        return this.freeUsageQuantity;
    }

    public void setFreeUsageQuantity(BigDecimal freeUsageQuantity) {
        this.freeUsageQuantity = freeUsageQuantity;
    }

    @Transient
    public BigDecimal getFreeUsagePoolsTotalQuantity() {
        BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
        for (OrderLineDTO orderLine : this.getLines()) {
            if (orderLine.getDeleted() == 0) {
                freeUsagePoolQuantity = freeUsagePoolQuantity.add(orderLine.getFreeUsagePoolQuantity());
            }
        }
        return freeUsagePoolQuantity;
    }

    @Transient
    public BigDecimal getFreeUsagePoolsTotalQuantity(Integer freeUsagePoolId) {
        BigDecimal freeUsagePoolQuantity = BigDecimal.ZERO;
        for (OrderLineDTO orderLine : this.getLines()) {
            if (orderLine.getDeleted() == 0) {
                freeUsagePoolQuantity = freeUsagePoolQuantity.add(orderLine.getFreeUsagePoolQuantity(freeUsagePoolId));
            }
        }
        return freeUsagePoolQuantity;
    }

    @Transient
    public Set<OrderLineUsagePoolDTO> getFreeUsagePools() {
        Set<OrderLineUsagePoolDTO> freeUsagePools = new HashSet<>();
        for (OrderLineDTO line : getLines()) {
            if (line.getDeleted() == 0) {
                freeUsagePools.addAll(line.getOrderLineUsagePools());
            }
        }
        return freeUsagePools;
    }

    @Transient
    public boolean hasFreeUsagePools() {
        return !getFreeUsagePools().isEmpty();
    }

    @Transient
    public boolean hasPlanWithFreeUsagePool() {
        return getLines().stream()
                .filter(line -> line.getItem().isPlan() && line.getDeleted() == 0)
                .flatMap(line -> line.getItem().getPlans().stream())
                .anyMatch(plan -> !plan.getUsagePools().isEmpty());
    }

    @Transient
    public Map<Integer, BigDecimal> getFreeUsagePoolsMap() {
        Map<Integer, BigDecimal> freeUsagePoolsMap = new HashMap<>();
        for (OrderLineUsagePoolDTO olUsagePool : getFreeUsagePools()) {
            if (!freeUsagePoolsMap.containsKey(olUsagePool.getCustomerUsagePool().getId())) {
                freeUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), olUsagePool.getQuantity());
            } else {
                BigDecimal existingQuantityOnMap = freeUsagePoolsMap.get(olUsagePool.getCustomerUsagePool().getId());
                freeUsagePoolsMap.put(olUsagePool.getCustomerUsagePool().getId(), existingQuantityOnMap.add(olUsagePool.getQuantity()));
            }
        }
        return freeUsagePoolsMap;
    }

    @Transient
    public Integer getPlanItemId() {
        return planItemId;
    }

    public void setPlanItemId(Integer planItemId) {
        this.planItemId = planItemId;
    }

    @Transient
    public PlanDTO getPlanWithUsagePool() {
        return getLines().stream()
                .filter(line -> line.getItem().isPlan() && line.getDeleted() == 0)
                .flatMap(line -> line.getItem().getPlans().stream())
                .filter(plan -> !plan.getUsagePools().isEmpty())
                .findFirst()
                .orElse(null);
    }

    @Transient
    public Date calcNextBillableDayFromChanges () {
        Date nextBillableDate = this.getNextBillableDay();
        for (OrderLineDTO line : this.getLines()) {
            for (OrderChangeDTO change : line.getOrderChanges()) {
                if (nextBillableDate == null || (change.getNextBillableDate() != null &&
                        nextBillableDate.after(change.getNextBillableDate()))) {
                    nextBillableDate = change.getNextBillableDate();
                }
            }
        }
        return nextBillableDate;
    }

    @Transient
    public PeriodUnit valueOfPeriodUnit () {
        int periodUnitId = this.getOrderPeriod().getPeriodUnit().getId();
        int dayOfMonth = this.getUser().getCustomer().getMainSubscription().getNextInvoiceDayOfPeriod();

        return PeriodUnit.valueOfPeriodUnit(dayOfMonth, periodUnitId);
    }

    @Transient
    public BigDecimal getAjustmentOrderTotalUsedQuantity() {
        BigDecimal ajustmentOrderUsedTotalQuantity = BigDecimal.ZERO;
        for (OrderLineDTO orderLine : this.getLines()) {
            if (orderLine.getDeleted() == 0) {
                ajustmentOrderUsedTotalQuantity = ajustmentOrderUsedTotalQuantity.add(orderLine.getQuantity());
            }
        }
        return ajustmentOrderUsedTotalQuantity;
    }

    @Transient
    public BigDecimal getTotalQuantityofOrderLineHasUsagePool(Integer freeUsagePoolId) {
        BigDecimal lineTotalQuantity = BigDecimal.ZERO;
        CustomerUsagePoolDTO customerUsagePool = new CustomerUsagePoolDAS().find(freeUsagePoolId);
        for (OrderLineDTO orderLine : this.getLines()) {

            if (orderLine.getDeleted() == 0 && null != customerUsagePool && customerUsagePool.getAllItems().contains(orderLine.getItem())) {
                lineTotalQuantity = lineTotalQuantity.add(orderLine.getQuantity());
            }
        }
        return lineTotalQuantity;
    }

    @Transient
    public BigDecimal getTotalOrderLineQuantity() {
        BigDecimal result = BigDecimal.ZERO;
        for(OrderLineDTO orderLine : this.getLines()) {
            result = result.add(orderLine.getQuantity());
        }
        return result;
    }

    @Transient
    public PlanDTO getPlanFromOrder() {
        for(OrderLineDTO orderLine : getLines()) {
        	// JBSPC-804 -Put a check for Deleted Order to fetch Plans from current order & not from Deleted Orders.This is to handle Plan Swap scenario.
            if(orderLine.getDeleted() == 0 && orderLine.hasItem()) {
                ItemDTO item = orderLine.getItem();
                if(item.hasPlans()) {
                    return item.firstPlan();
                }
            }
        }
        return null;
    }

    @Override
    @Transient
    public String[] getFieldNames() {
        return getOrderExportableWrapper().getFieldNames();
    }

    @Override
    @Transient
    public Object[][] getFieldValues() {
        return getOrderExportableWrapper().getFieldValues();
    }

    @Transient
    public OrderExportableWrapper getOrderExportableWrapper() {
        if(null==orderExportableWrapper) {
            orderExportableWrapper = new OrderExportableWrapper(this.getId());
        }
        return orderExportableWrapper;
    }


    @Transient
    public List<OrderLineDTO> getTaxQuoteLines() {
        return this.getLines().stream()
                .filter(orderLine -> orderLine.getTypeId().equals(Constants.ORDER_LINE_TYPE_TAX_QUOTE))
                .collect(Collectors.toList());
    }

    @Transient
    public OrderLineDTO getTaxLine(Integer lineID) {
        return this.getTaxQuoteLines().stream()
                .filter(line -> line.getDeleted()==0 &&
                line.getId()==lineID)
                .findFirst()
                .orElse(null);
    }

    @Transient
    public boolean isDiscountOrder() {
        return lines.stream().anyMatch(line -> line.getDeleted() == 0 &&
                line.isDiscount());
    }

    @Override
    @Transient
    public void setMetaField(Integer entitId, Integer groupId, String name, Object value) throws IllegalArgumentException {
        MetaFieldHelper.setMetaField(entitId, groupId, this, name, value);
    }

    @Transient
    public void addMetaField(MetaFieldValue<?> value) {
        getMetaFields().add(value);
    }

    @Transient
    public boolean isAssetPresent(Integer assetId) {
        for(OrderLineDTO line : getLines()) {
            if(line.getAssetIds().contains(assetId)) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public boolean containsAssets() {
        for(OrderLineDTO line : getLines()) {
            if(CollectionUtils.isNotEmpty(line.getAssetIds())) {
                return true;
            }
        }
        return false;
    }

    @Transient
    public List<String> getAssetIdentifiers() {
        List<String> assets = new ArrayList<>();
        for(OrderLineDTO line : this.lines) {
            for(AssetDTO asset : line.getAssets()) {
                assets.add(asset.getIdentifier());
            }
        }
        return assets;
    }

    @Transient
    public List<Integer> getAssetIds() {
        List<Integer> assets = new ArrayList<>();
        for(OrderLineDTO line : this.lines) {
            for(AssetDTO asset : line.getAssets()) {
                assets.add(asset.getId());
            }
        }
        return assets;
    }

    @Transient
    public List<AssetDTO> getAssets() {
        List<AssetDTO> assets = new ArrayList<>();
        for(OrderLineDTO line : this.lines) {
            assets.addAll(line.getAssets());
        }
        return assets;
    }

    @Transient
    public boolean isCreditOrder() {
        Optional<BigDecimal> optTotalAmount = getLines().stream()
                       .map(OrderLineDTO :: getAmount)
                       .reduce((amount1, amount2)-> amount1.add(amount2));
        return optTotalAmount.isPresent() ? optTotalAmount.get().compareTo(BigDecimal.ZERO) < 0 : false;
    }

    @Transient
    public List<AssetAssignmentDTO> getAssetAssignments() {
        List<AssetAssignmentDTO> assetAssignments = new ArrayList<>();
        for(OrderLineDTO line : this.lines) {
            assetAssignments.addAll(line.getAssetAssignments());
        }
        return assetAssignments;
    }

    @Transient
    public boolean isPlanOrder() {
        for(OrderLineDTO line : this.lines) {
            if(null != line.getItem() && line.getItem().isPlan()) {
                return true;
            }
        }
        return false;
    }
}
