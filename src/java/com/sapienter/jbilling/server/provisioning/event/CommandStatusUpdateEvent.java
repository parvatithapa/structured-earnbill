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

package com.sapienter.jbilling.server.provisioning.event;

import com.sapienter.jbilling.server.provisioning.db.ProvisioningCommandDTO;
import com.sapienter.jbilling.server.system.event.Event;

public class CommandStatusUpdateEvent implements Event {

    private final Integer  entityId;
    private final ProvisioningCommandDTO provisioningCommand;

    public CommandStatusUpdateEvent(Integer entityId, ProvisioningCommandDTO provisioningCommand) {
        this.entityId = entityId;
        this.provisioningCommand = provisioningCommand;
    }

    @Override
    public String getName() {
        return "Command Status Update Event - entity " + entityId;
    }

    @Override
    public Integer getEntityId() {
        return entityId;
    }

    public ProvisioningCommandDTO getProvisioningCommand() {
        return provisioningCommand;
    }

    public String toString() {
        return getName() + " - entity " + entityId;
    }
}
