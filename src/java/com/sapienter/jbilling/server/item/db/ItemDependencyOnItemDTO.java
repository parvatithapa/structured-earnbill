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

import javax.persistence.*;

/**
 * Class represents a dependency of an ItemDTO on another ItemDTO.
 *
 */
@Entity
@DiscriminatorValue("item")
public class ItemDependencyOnItemDTO extends ItemDependencyDTO {

    private ItemDTO dependent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_item_id")
    public ItemDTO getDependent() {
        return dependent;
    }

    public void setDependent(ItemDTO dependent) {
        this.dependent = dependent;
    }

    @Override
    public void setDependentObject(Object dependent) {
        setDependent((ItemDTO) dependent);
    }

    @Transient
    @Override
    public Integer getDependentObjectId() {
        return dependent.getId();
    }

    @Transient
    @Override
    public String getDependentDescription() {
        return dependent.getDescription();
    }

    @Override
    @Transient
    public ItemDependencyType getType() {
        return ItemDependencyType.ITEM;
    }
}


