/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2014] Enterprise jBilling Software Ltd.
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

import java.util.HashSet;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TableGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Sort;
import org.hibernate.annotations.SortType;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.ItemDTO;
import com.sapienter.jbilling.server.metafields.EntityType;
import com.sapienter.jbilling.server.metafields.MetaFieldHelper;
import com.sapienter.jbilling.server.metafields.db.CustomizedEntity;
import com.sapienter.jbilling.server.metafields.db.MetaFieldValue;

/**
 * @author: Alexander Aksenov
 * @since: 21.03.14
 */
@Entity
@TableGenerator(
        name = "order_change_plan_item_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "order_change_plan_item",
        allocationSize = 100
)
@Table(name = "order_change_plan_item")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class OrderChangePlanItemDTO extends CustomizedEntity implements java.io.Serializable {

    private Integer id;
    private int optLock;
    private String description;
    private OrderChangeDTO orderChange;
    private ItemDTO item;
    private Set<AssetDTO> assets = new HashSet<AssetDTO>();
    private boolean isTouched = false;

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "order_change_plan_item_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Version
    @Column(name = "optlock")
    public int getOptLock() {
        return optLock;
    }

    public void setOptLock(int optLock) {
        this.optLock = optLock;
    }

    @Column(name = "description", length = 1000)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_change_id", nullable = false)
    public OrderChangeDTO getOrderChange() {
        return orderChange;
    }

    public void setOrderChange(OrderChangeDTO orderChange) {
        this.orderChange = orderChange;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    public ItemDTO getItem() {
        return item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }

    @ManyToMany(fetch = FetchType.LAZY)
    @Cascade({org.hibernate.annotations.CascadeType.DETACH})
    @JoinTable(name = "order_change_plan_item_asset_map",
            joinColumns = {@JoinColumn(name = "order_change_plan_item_id", updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "asset_id", updatable = false)}
    )
    public Set<AssetDTO> getAssets() {
        return assets;
    }

    public void setAssets(Set<AssetDTO> assets) {
        this.assets = assets;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @JoinTable(
            name = "order_change_plan_item_meta_field_map",
            joinColumns = @JoinColumn(name = "order_change_plan_item_id"),
            inverseJoinColumns = @JoinColumn(name = "meta_field_value_id")
    )
    @Sort(type = SortType.COMPARATOR, comparator = MetaFieldHelper.MetaFieldValuesOrderComparator.class)
    @Override
    public List<MetaFieldValue> getMetaFields() {
        return getMetaFieldsList();
    }

    @Override
    @Transient
    public EntityType[] getCustomizedEntityType() {
        return new EntityType[]{EntityType.ORDER_CHANGE};
    }

    public void touch() {
        // touch entity with possible cycle dependencies only once
        if (isTouched) return;
        isTouched = true;
        getDescription();
        for (AssetDTO asset : assets) {
            asset.getIdentifier();
        }
        for (MetaFieldValue metaFieldValue : getMetaFields()) {
            metaFieldValue.getValue();
        }
    }

    @Override
    public String toString() {
        return "OrderChangePlanItemDTO{" +
                "id=" + id +
                ", item=" + (item != null ? item.getId() : "null") +
                ", orderChange=" + (orderChange != null ? orderChange.getId() : "null") +
                ", description='" + description + '\'' +
                ", assets=" + assets +
                '}';
    }
}
