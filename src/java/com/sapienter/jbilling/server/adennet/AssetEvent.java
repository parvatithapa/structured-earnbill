/*
 * SARATHI SOFTECH PVT. LTD. CONFIDENTIAL
 * _____________________
 *
 * [2024] Sarathi Softech Pvt. Ltd.
 * All Rights Reserved.
 *
 * NOTICE:  All information contained herein is and remains
 * the property of Sarathi Softech.
 * The intellectual and technical concepts contained
 * herein are proprietary to Sarathi Softech
 * and are protected by IP copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden.
 */

package com.sapienter.jbilling.server.adennet;

import com.sapienter.jbilling.server.system.event.Event;

public class AssetEvent implements Event {

    private Integer entityId;
    private Integer userId;
    private String identifier;
    private Integer planId;
    private UserAction userAction;

    public AssetEvent(Integer entityId, Integer userId, String identifier, Integer planId, UserAction userAction) {
        this.entityId = entityId;
        this.userId = userId;
        this.identifier = identifier;
        this.planId = planId;
        this.userAction = userAction;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    @Override
    public String getName() {
        return "AssetEvent-" + getEntityId();
    }

    public Integer getUserId() {
        return userId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Integer getPlanId() {
        return planId;
    }

    public UserAction getUserAction() {
        return userAction;
    }

}
