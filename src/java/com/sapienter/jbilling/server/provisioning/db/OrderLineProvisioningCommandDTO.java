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

import com.sapienter.jbilling.common.FormatLogger;
import com.sapienter.jbilling.server.order.OrderBL;
import com.sapienter.jbilling.server.order.db.OrderLineDTO;
import com.sapienter.jbilling.server.order.db.OrderChangeDTO;
import com.sapienter.jbilling.server.provisioning.ProvisioningCommandType;
import com.sapienter.jbilling.server.provisioning.task.ProvisioningCommandsRulesTask;
import com.sapienter.jbilling.server.util.Constants;
import com.sapienter.jbilling.server.util.audit.EventLogger;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.UUID;

@Entity
@DiscriminatorValue("order_line")
public class OrderLineProvisioningCommandDTO extends ProvisioningCommandDTO implements IOrderLineProvisioningCommandDTO {

    private OrderChangeDTO orderChange;

    public OrderLineProvisioningCommandDTO() {}

    public OrderLineProvisioningCommandDTO(OrderChangeDTO orderChangeDTO) {
        this.orderChange = orderChangeDTO;

        setEntity(getOrderChange().getUser().getCompany());
    }

    @Transient
    public OrderLineDTO getOrderLine() {
        return getOrderChange().getOrderLine();
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(
            name = "order_line_provisioning_command_map",
            joinColumns = @JoinColumn(name = "provisioning_command_id"),
            inverseJoinColumns = @JoinColumn(name = "order_change_id")
    )
    public OrderChangeDTO getOrderChange() {
        return orderChange;
    }

    public void setOrderChange(OrderChangeDTO orderChange) {
        this.orderChange = orderChange;
    }

    @Transient
    @Override
    public ProvisioningCommandType getCommandType() {
        return ProvisioningCommandType.ORDER_LINE;
    }

    @Transient
    @Override
    public void postCommand(MapMessage message, String eventType) throws JMSException {
        FormatLogger LOG = new FormatLogger(Logger.getLogger(OrderLineProvisioningCommandDTO.class));

        UUID uid = UUID.randomUUID();

        message.setStringProperty("id", uid.toString());
        LOG.debug("set message property id=" + uid.toString());
        message.setIntProperty("commandId", getId());
        LOG.debug("set message commandId=" + getId());
        message.setStringProperty("command", getName());
        LOG.debug("set message command=" + getName());
        message.setIntProperty("entityId", getEntity().getId());
        LOG.debug("set message property entityId=" + getEntity().getId());
        message.setIntProperty("order_change_id", getOrderChange().getId());
        LOG.debug("set message property order_change_id=" + getOrderChange().getId());

        for (Map.Entry<String, String> param : getCommandParameters().entrySet())
        {
            message.setStringProperty(param.getKey(), param.getValue());
            LOG.debug("set Message property : (" + param.getKey() + ","
                    + param.getValue() + ")");
        }

        if (getOrderLine() != null) {
            message.setIntProperty("order_line_id", getOrderLine().getId());
            LOG.debug("set message property order_line_id=" + getOrderLine().getId());

            getOrderLine().setProvisioningStatusId(Constants.PROVISIONING_STATUS_PENDING_ACTIVE);

        }

        LOG.debug("Sending message for command '" + getName() + "'");
    }
}