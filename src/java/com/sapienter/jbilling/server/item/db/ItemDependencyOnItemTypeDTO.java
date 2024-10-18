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
 * Class represents a dependency of an ItemDTO on an ItemTypeDTO.
 *
 */
@Entity
@DiscriminatorValue("item_type")
public class ItemDependencyOnItemTypeDTO extends ItemDependencyDTO {

    private ItemTypeDTO dependent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependent_item_type_id")
    public ItemTypeDTO getDependent() {
        return dependent;
    }

    public void setDependent(ItemTypeDTO dependent) {
        this.dependent = dependent;
    }

    @Override
    public void setDependentObject(Object dependent) {
        setDependent((ItemTypeDTO) dependent);
    }

    @Override
    @Transient
    public Integer getDependentObjectId() {
        return dependent.getId();
    }

    @Override
    @Transient
    public ItemDependencyType getType() {
        return ItemDependencyType.ITEM_TYPE;
    }

    @Transient
    @Override
    public String getDependentDescription() {
        return dependent.getDescription();
    }
}


