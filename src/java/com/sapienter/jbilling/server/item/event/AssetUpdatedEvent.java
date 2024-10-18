/*
 * JBILLING CONFIDENTIAL
 * _____________________
 *
 * [2003] - [2013] Enterprise jBilling Software Ltd.
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
package com.sapienter.jbilling.server.item.event;

import java.util.Map;

import com.sapienter.jbilling.server.item.db.AssetDTO;
import com.sapienter.jbilling.server.item.db.AssetStatusDTO;
import com.sapienter.jbilling.server.user.db.UserDTO;

/**
 * Event gets fired when an asset gets updated.
 *
 * @author Gerhard
 * @since 15/04/13
 */
public class AssetUpdatedEvent extends AbstractAssetEvent {

    private Map<String, Object> oldMetaFieldValues;

    /** Old status. May be null the same or different from the current status */
    private AssetStatusDTO oldStatus;

    public AssetUpdatedEvent(Integer entityId, AssetDTO asset, AssetStatusDTO oldStatus, UserDTO assignedTo, Integer user, Map<String, Object> oldMetaFieldValues) {
        super(entityId, asset, assignedTo, user);
        this.oldStatus = oldStatus;
        this.oldMetaFieldValues = oldMetaFieldValues;
    }

    public AssetUpdatedEvent(Integer entityId, AssetDTO asset, AssetStatusDTO oldStatus, UserDTO assignedTo, Integer user) {
        this(entityId, asset, oldStatus, assignedTo, user, null);
    }


    public AssetStatusDTO getOldStatus() {
        return oldStatus;
    }


    public AssetStatusDTO getNewStatus() {
        return getAsset().getAssetStatus();
    }

    public Map<String, Object> getOldMetaFieldValues() { return oldMetaFieldValues; }

    /**
     * Check if there is a difference the oldStatus and the current status of the asset
     * @return
     */
    public boolean hasStatusChanged() {
        return oldStatus == null || (oldStatus.getId() != getNewStatus().getId());
    }

    @Override
    public String getName() {
        return "Asset Updated event";
    }

    @Override
    public String toString() {
        return "AssetUpdatedEvent{" +
                "entityId=" + getEntityId() +
                ", name=" + getName() +
                ", asset=" + getAsset() +
                ", assignedTo=" + (getAssignedTo() != null ? getAssignedTo().getId() : null) +
                ", user=" + getUser() +
                ", oldStatus=" + oldStatus +
                '}';
    }
}
