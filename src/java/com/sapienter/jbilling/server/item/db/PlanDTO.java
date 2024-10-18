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

package com.sapienter.jbilling.server.item.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.audit.Auditable;
import com.sapienter.jbilling.server.item.PlanWS;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;
import com.sapienter.jbilling.server.order.db.OrderPeriodDTO;
import com.sapienter.jbilling.server.pricing.db.PriceModelDTO;
import com.sapienter.jbilling.server.timezone.TimezoneHelper;
import com.sapienter.jbilling.server.usagePool.db.UsagePoolDTO;
import com.sapienter.jbilling.server.util.csv.Exportable;

/**
 * @author Brian Cowdery
 * @since 26-08-2010
 */
@Entity
@Table(name = "plan")
@TableGenerator(
        name = "plan_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "plan",
        allocationSize = 1
)
@NamedQueries({
        @NamedQuery(name  = "PlanDTO.findByPlanItem",
                    query = "SELECT plan FROM PlanDTO plan WHERE plan.item.id = :plan_item_id"),

        @NamedQuery(name  = "CustomerDTO.findCustomersByPlan",
                    query = "SELECT user.customer"
                            + " FROM OrderLineDTO line "
                            + " INNER JOIN line.item.plans AS plan "
                            + " INNER JOIN line.purchaseOrder.baseUserByUserId AS user"
                            + " WHERE plan.id = :plan_id"
                            + " AND line.deleted = 0 "
                            + " AND line.purchaseOrder.orderPeriod.id != 1 " // Constants.ORDER_PERIOD_ONCE
                            + " AND line.purchaseOrder.orderStatus.orderStatusFlag = 0" //OrderStatusFlag.INVOICE
                            + " AND line.purchaseOrder.deleted = 0"),

        @NamedQuery(name  = "PlanDTO.isSubscribed",
                    query = "SELECT line"
                            + " FROM OrderLineDTO line "
                            + " INNER JOIN line.item.plans AS plan "
                            + " INNER JOIN line.purchaseOrder.baseUserByUserId AS user "
                            + " WHERE plan.id = :plan_id "
                            + " AND user.id = :user_id "
                            + " AND ( line.startDate <= :pricingDate OR line.startDate = null ) "
                            + " AND ( line.endDate > :pricingDate OR line.endDate = null ) "
                            + " AND   line.purchaseOrder.orderPeriod.id != 1 " // Constants.ORDER_PERIOD_ONCE
                            + " AND   line.purchaseOrder.deleted = 0 AND line.deleted = 0 "
                            //+ " and ( line.purchaseOrder.deletedDate > :pricingDate or line.purchaseOrder.deletedDate = null )"
                            + " AND   line.purchaseOrder.activeSince  <= :pricingDate "
                            + " AND ( line.purchaseOrder.activeUntil > :pricingDate OR line.purchaseOrder.activeUntil = null)"),

        @NamedQuery(name  = "PlanDTO.findByAffectedItem",
                    query = "SELECT plan "
                            + " FROM PlanDTO plan "
                            + " INNER JOIN plan.planItems planItems "
                            + " WHERE planItems.item.id = :affected_item_id"),

        @NamedQuery(name  = "PlanDTO.findAllByEntity",
                    query = "SELECT plan "
                            + " FROM PlanDTO plan "
                            + " INNER JOIN plan.item AS it"
                            + " INNER JOIN it.entities AS child"
                            + " WHERE child.id= :entity_id"),
                            //+ " where plan.item.entity.id = :entity_id"),

        @NamedQuery(name  = "PlanDTO.findAllActiveByEntity",
                    query = "SELECT distinct plan"
                            + " FROM PlanDTO plan"
                            + " INNER JOIN plan.item AS it"
                            + " LEFT OUTER JOIN it.entities AS child"
                            + " WHERE ((child.id IN (:entityIds)) OR (it.entity.id = :entityId AND it.global = true)) AND it.deleted = 0"),

        @NamedQuery(name  = "PlanDTO.findAllActiveAvailable",
                    query = "SELECT distinct plan"
                            + " FROM PlanDTO plan"
                            + " INNER JOIN plan.item AS it"
                            + " LEFT OUTER JOIN it.entities AS child"
                            + " WHERE ((child.id in (:entityIds)) OR (it.entity.id = :entityId AND it.global = true)) AND it.deleted = 0"
                            + " AND (it.activeSince is null OR it.activeSince <= :date ) AND  (it.activeUntil is null OR it.activeUntil >= :date ) "),

        @NamedQuery(name  = "PlanDTO.findByItemId",
                    query = "SELECT plan FROM PlanDTO plan WHERE plan.item.id = :item_id"),

        @NamedQuery(name  = "PlanDTO.isSubscribedFinished",
                    query = "SELECT line.id"
                            + " FROM OrderLineDTO line "
                            + " INNER JOIN line.item.plans AS plan "
                            + " INNER JOIN line.purchaseOrder.baseUserByUserId AS user "
                            + " WHERE plan.id = :plan_id "
                            + " AND user.id = :user_id "
                            +"  AND line.deleted = 0 "
                            + " AND line.purchaseOrder.orderPeriod.id != 1 " // Constants.ORDER_PERIOD_ONCE
                            + " AND line.purchaseOrder.orderStatus.orderStatusFlag = 1" //+OrderStatusFlag.FINISHED
                            + " AND line.purchaseOrder.deleted = 0"),

        @NamedQuery(name  = "PlanDTO.findUserByFreeTrialPlan",
                    query = "SELECT user.id"
                            + " FROM OrderLineDTO line"
                            + " INNER JOIN line.item.plans AS plan"
                            + " INNER JOIN line.purchaseOrder.baseUserByUserId AS user"
                            + " WHERE plan.freeTrial = true"
                            + " AND line.deleted = 0"
                            + " AND line.purchaseOrder.activeUntil < :expiry_date"
                            + " AND line.purchaseOrder.orderPeriod.id != 1" // Constants.ORDER_PERIOD_ONCE
                            + " AND line.purchaseOrder.orderStatus.orderStatusFlag = 0" //OrderStatusFlag.INVOICE
                            + " AND line.purchaseOrder.deleted = 0")
})
// todo: cache config
public class PlanDTO extends CustomizedEntity implements Serializable, Exportable, Auditable {

    private Integer id;
    private ItemDTO item; // plan subscription item
    private OrderPeriodDTO period;
    private String description;
    private int editable = 0;
    private List<PlanItemDTO> planItems = new ArrayList<>();
    private List<MetaFieldValue> metaFields = new LinkedList<>();
    private Set<UsagePoolDTO> usagePools = new HashSet<>(0);
    
    private boolean freeTrial;

    public PlanDTO() {
    }

    public PlanDTO(PlanWS ws, ItemDTO item, OrderPeriodDTO period, List<PlanItemDTO> planItems, Set<UsagePoolDTO> usagePools) {
        this.id = ws.getId();
        this.item = item;
        this.period = period;
        this.description = ws.getDescription();
        this.freeTrial = ws.isFreeTrial();
        this.editable = ws.getEditable();
        this.planItems = planItems;
        this.usagePools = usagePools;
    }

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "plan_GEN")
    @Column(name = "id", nullable = false, unique = true)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Item holding this plan. When the customer subscribes to this item the
     * plan prices will be added for the customer.
     *
     * @return plan subscription item
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    public ItemDTO getItem() {
        return item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }

    @Transient
    public Integer getItemId() {
        return getItem().getId();
    }

    /**
     * Returns the plan subscription item.
     * Syntax sugar, alias for {@link #getItem()}
     * @return plan subscription item
     */
    @Transient
    public ItemDTO getPlanSubscriptionItem() {
        return getItem();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    public OrderPeriodDTO getPeriod() {
        return period;
    }

    public void setPeriod(OrderPeriodDTO period) {
        this.period = period;
    }

    @Column(name = "description", nullable = true, length = 255)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	@Column(name = "editable", nullable = false)
    public int getEditable() {
        return editable;
    }

    @Column(name = "is_free_trial", nullable = false)
    public boolean isFreeTrial() {
        return freeTrial;
    }

    public void setFreeTrial(boolean freeTrial) {
        this.freeTrial = freeTrial;
    }

    public void setEditable(int editable) {
        this.editable = editable;
    }

    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "plan")
    public List<PlanItemDTO> getPlanItems() {
        return planItems;
    }

    public void setPlanItems(List<PlanItemDTO> planItems) {
        for (PlanItemDTO planItem : planItems)
            planItem.setPlan(this);

        this.planItems = planItems;
    }

    public void addPlanItem(PlanItemDTO planItem) {
        planItem.setPlan(this);
        this.planItems.add(planItem);
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "plan_meta_field_map",
            joinColumns = @JoinColumn(name = "plan_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )

    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    public List<MetaFieldValue> getMetaFields() {
        return metaFields;
    }

    @Transient
    public void setMetaFields(List<MetaFieldValue> fields) {
        this.metaFields = fields;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "plan_usage_pool_map",
            joinColumns = {@JoinColumn(name = "plan_id", updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "usage_pool_id", updatable = false)}
    )
    public Set<UsagePoolDTO> getUsagePools() {
        return usagePools;
    }

    public void setUsagePools(Set<UsagePoolDTO> usagePools) {
        this.usagePools = usagePools;
    }

    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[]{EntityType.PLAN};
    }

    public PlanItemDTO findPlanItem(Integer itemId) {
        for (PlanItemDTO planItem : getPlanItems()) {
            if (itemId.equals(planItem.getItem().getId())) {
                return planItem;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanDTO planDTO = (PlanDTO) o;

        if (description != null ? !description.equals(planDTO.description) : planDTO.description != null) return false;
        if (editable != planDTO.editable) return false;
        if (freeTrial != planDTO.freeTrial) return false;
        if (id != null ? !id.equals(planDTO.id) : planDTO.id != null) return false;
        if (item != null ? !item.equals(planDTO.item) : planDTO.item != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + editable;
        return result;
    }

    @Override
    public String toString() {
        return "PlanDTO{"
                + "id=" + id
                + ", item=" + item
                + ", description='" + description + '\''
                + ", editable=" + editable
                + ", freeTrial=" + freeTrial
                + ", planItems=" + planItems
                + '}';
    }

    public String getAuditKey(Serializable id) {
        StringBuilder key = new StringBuilder();
        key.append(getItem().getEntity().getId())
                .append("-")
                .append(id);

        return key.toString();
    }

    @Transient
    public String[] getFieldNames() {
        return new String[]{
                "id",
                "plan number",
                "description",
                "period",
                "currency",
                "rate",
                "item id",
                "free trial",
                //Plan Items
                "plan item product code",
                "plan item product description",
                "plan item precedence",
                "bundled quantity",
                "bundled period",
                "add to customer",
                "pricing strategy",
                "rate",
                "currency",
                "attributes"
        };
    }

    @Transient
    public Object[][] getFieldValues() {

        List<Object[]> values = new ArrayList<>();
        Integer languageId = getItem().getEntity().getLanguage().getId();
        // Now prices for company exist for different companies, 
        // have to tell for which company you want to get price for
        // current setting 'null', gives global price
        Integer entityId = this.period.getCompany().getId();
        PriceModelDTO currentPrice = item.getPrice(TimezoneHelper.companyCurrentDate(entityId), entityId);
        // main plan row
        values.add(
                new Object[]{
                        id,
                        (item != null ? item.getInternalNumber() : null),
                        item.getDescription(languageId),
                        (period != null ? period.getDescription(languageId) : null),
                        (currentPrice != null ? currentPrice.getCurrency().getDescription(languageId) : null),
                        (currentPrice != null ? currentPrice.getRate() : null),
                        (item != null ? item.getId() : null),
                        freeTrial
                }
        );

        // indented row for each invoice line  planItem.getModels().get(new Date()).getRate(),
        for (PlanItemDTO planItem : planItems) {
            PriceModelDTO priceModel = planItem.getPrice(TimezoneHelper.companyCurrentDate(entityId));

            ItemDTO itemDTO = planItem.getItem();

            PlanItemBundleDTO bundle = planItem.getBundle();

            String addToCustomer = null;
            if (bundle != null) {
                if (bundle.getTargetCustomer().name().equals("SELF")) {
                    addToCustomer = "Plan Subscriber";
                } else {
                    addToCustomer = "Billable Parent";
                }
            }

            values.add(
                    new Object[]{
                            // padding for the main invoice columns
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            // planItems
                            (itemDTO != null ? itemDTO.getInternalNumber() : null),
                            (itemDTO != null ? planItem.getItem().getDescription(languageId) : null),
                            (itemDTO != null ? planItem.getPrecedence() : null),
                            (bundle != null ? bundle.getQuantity() : null),
                            (bundle != null ? bundle.getPeriod().getDescription(languageId) : null),
                            addToCustomer,
                            (priceModel != null ? priceModel.getType().name() : null),
                            (priceModel != null ? priceModel.getRate() : null),
                            (priceModel != null ? priceModel.getCurrency().getDescription(languageId) : null),
                            (priceModel != null ? priceModel.getAttributes() : null)
                    }
            );
        }

        return values.toArray(new Object[values.size()][]);
    }

    @Transient
    public boolean doesPlanHaveItem(Integer itemId) {
        return getPlanItems().stream().anyMatch(planItem -> planItem.getItem().getId() == itemId);
    }
}

