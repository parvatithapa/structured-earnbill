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

import com.sapienter.jbilling.server.item.ItemDependencyType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Class represents a dependency of an ItemDTO on other products.
 * It specifies a minimum and maximum quantity of the dependent product which must be in the same order hierarchy.
 *
 */
@Entity
@TableGenerator(
        name = "item_dependency_GEN",
        table = "jbilling_seqs",
        pkColumnName = "name",
        valueColumnName = "next_id",
        pkColumnValue = "item_dependency",
        allocationSize = 100
)

@NamedQueries({
        @NamedQuery(name = "ItemDependencyOnItemDTO.countForDependItem",
                query = "select count(a.id) from ItemDependencyOnItemDTO a where a.dependent.id = :item_id ")
})

@Table(name = "item_dependency")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
public abstract class ItemDependencyDTO implements Serializable {

    private int id;
    private ItemDTO item;
    private Integer minimum;
    private Integer maximum;

    @Id @GeneratedValue(strategy = GenerationType.TABLE, generator = "item_dependency_GEN")
    @Column(name = "id", unique = true, nullable = false)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    public ItemDTO getItem() {
        return item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }

    @Column (name = "min")
    public Integer getMinimum() {
        return minimum;
    }

    public void setMinimum(Integer minimum) {
        this.minimum = minimum;
    }

    @Column (name = "max")
    public Integer getMaximum() {
        return maximum;
    }

    public void setMaximum(Integer maximum) {
        this.maximum = maximum;
    }

    public abstract void setDependentObject(Object dependent);

    @Transient
    public abstract Object getDependent();

    @Transient
    public abstract Integer getDependentObjectId();

    @Transient
    public abstract String getDependentDescription();

    @Transient
    public abstract ItemDependencyType getType();
}


