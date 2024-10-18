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
package com.sapienter.jbilling.server.provisioning.db;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.db.OrderDAS;
import com.sapienter.jbilling.server.order.db.OrderDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.apache.log4j.Logger;
import org.hibernate.annotations.*;

import java.util.Map;
import java.util.UUID;

@Entity
@DiscriminatorValue("order")
public class OrderProvisioningCommandDTO extends ProvisioningCommandDTO implements IOrderProvisioningCommandDTO {

    private OrderDTO order;

    public OrderProvisioningCommandDTO() {
    }

    public OrderProvisioningCommandDTO(OrderDTO orderDTO) {
        this.order = orderDTO;

        setEntity(getOrder().getUser().getCompany());
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "order_provisioning_command_map",
            joinColumns = @JoinColumn(name = "provisioning_command_id"),
            inverseJoinColumns = @JoinColumn(name = "order_id")
    )
    public OrderDTO getOrder() {
        return this.order;
    }

    public void setOrder(OrderDTO orderDTO) {
        this.order = orderDTO;
    }

    @Transient
    @Override
    public ProvisioningCommandType getCommandType() {
        return ProvisioningCommandType.ORDER;
    }

    @Transient
    @Override
    public void postCommand(MapMessage message, String eventType) throws JMSException {
        FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderProvisioningCommandDTO.class));
        EventLogger eLogger = EventLogger.getInstance();

        UUID uid = UUID.randomUUID();

        message.setStringProperty("id", uid.toString());
        LOG.debug("set message property id=" + uid.toString());
        message.setIntProperty("commandId", getId());
        LOG.debug("set message commandId=" + getId());
        message.setStringProperty("command", getName());
        LOG.debug("set message command=" + getName());
        message.setIntProperty("entityId", getEntity().getId());
        LOG.debug("set message property entityId=" + getEntity().getId());
        message.setIntProperty("order_id", getOrder().getId());
        LOG.debug("set message property order_id=" + getOrder().getId());

        for (Map.Entry<String, String> param : getCommandParameters().entrySet())
        {
            message.setStringProperty(param.getKey(), param.getValue());
            LOG.debug("set Message property : (" + param.getKey() + ","
                    + param.getValue() + ")");
        }

        if (order != null) {
            order.setProvisioningStatusId(Constants.PROVISIONING_STATUS_PENDING_ACTIVE);
        }

        LOG.debug("Sending message for command '" + getName() + "'");
    }
}